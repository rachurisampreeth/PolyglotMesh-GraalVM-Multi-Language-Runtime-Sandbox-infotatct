package com.polyglotmesh.runtime;

import com.polyglotmesh.api.Models.*;
import com.polyglotmesh.data.GuestValues;
import com.polyglotmesh.data.MongoMock;
import com.polyglotmesh.service.PricingMath;
import jakarta.annotation.PreDestroy;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Educational sandbox: use OS/container isolation before exposing to untrusted public users. */
@Service
public class PolyglotRunner {
    private static final java.util.function.Predicate<Source> COUNT_ALL = source -> true;
    private final Engine engine;
    private final ThreadPoolExecutor pool;
    private final MongoMock db;
    private final int timeoutMs, outputLimit, codeLimit;
    private final long statementLimit;

    public PolyglotRunner(MongoMock db,
            @org.springframework.beans.factory.annotation.Value("${mesh.timeout-ms:10000}") int timeoutMs,
            @org.springframework.beans.factory.annotation.Value("${mesh.statement-limit:100000}") long statementLimit,
            @org.springframework.beans.factory.annotation.Value("${mesh.output-limit:32768}") int outputLimit,
            @org.springframework.beans.factory.annotation.Value("${mesh.code-limit:16000}") int codeLimit,
            @org.springframework.beans.factory.annotation.Value("${mesh.max-workers:4}") int maxWorkers,
            @org.springframework.beans.factory.annotation.Value("${mesh.max-queue:16}") int maxQueue) {
        this.db = db;
        this.timeoutMs = timeoutMs;
        this.statementLimit = statementLimit;
        this.outputLimit = outputLimit;
        this.codeLimit = codeLimit;
        this.engine = Engine.newBuilder("python", "js").build();
        this.pool = new ThreadPoolExecutor(maxWorkers, maxWorkers, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(maxQueue), r -> {
                Thread t = new Thread(r, "polyglot-guest");
                t.setDaemon(true);
                return t;
            }, new ThreadPoolExecutor.AbortPolicy());
    }

    public ExecuteResponse execute(ExecuteRequest request) {
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing request");
        String mode = request.mode() == null ? "single" : request.mode().trim().toLowerCase(Locale.ROOT);
        long start = System.nanoTime();
        List<ExecutionStep> steps = new ArrayList<>();
        Evaluation evaluation;
        String language = request.language() == null ? "python" : request.language().trim().toLowerCase(Locale.ROOT);
        if (!List.of("single", "audit", "pipeline").contains(mode))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode must be single, audit or pipeline");
        if (mode.equals("single")) {
            if (!List.of("python", "javascript", "js").contains(language))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only Python and JavaScript execute (Java tab is editor-only)");
            requireCode(request.code());
            evaluation = run(language.equals("python") ? "python" : "js", request.code(), request.input(), false, null);
            steps.add(new ExecutionStep(language, evaluation.status(), evaluation.result(), evaluation.elapsedMs()));
        } else if (mode.equals("audit")) {
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("price", 1000);
            evaluation = run("python", "audit['price'] = audit['price'] + 500\nresult = audit['price']",
                Map.of(), false, audit);
            // Read back the identical Java Map instance without JSON conversion.
            if ("OK".equals(evaluation.status())) {
                Map<String, Object> answer = Map.of("original", 1000, "javaReadback", audit.get("price"),
                    "sameJavaMap", true, "serializedBetweenLanguages", false);
                evaluation = evaluation.withResult(answer);
            }
            steps.add(new ExecutionStep("python", evaluation.status(), evaluation.result(), evaluation.elapsedMs()));
        } else {
            Map<String, Object> input = request.input() == null ? Map.of() : request.input();
            String productId = String.valueOf(input.getOrDefault("productId", "p2"));
            Map<String, Object> product;
            try { product = db.product(productId); }
            catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
            int quantity = requiredInt(input.getOrDefault("quantity", 2));
            double discount = requiredDouble(input.getOrDefault("discountPercent", 10));
            if (quantity < 1 || quantity > 10000 || discount < 0 || discount > 100)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity or discountPercent");
            double basePrice = ((Number) product.get("price")).doubleValue();
            // Java pulls data, Python performs pricing, Java validates numeric result, JS validates stock.
            String python = request.pythonCode() == null || request.pythonCode().isBlank()
                ? "result = round(" + basePrice + " * " + quantity + " * (1 - " + discount + " / 100), 2)"
                : request.pythonCode();
            requireCode(python);
            Evaluation py = run("python", python, Map.of("basePrice", basePrice, "quantity", quantity,
                "discountPercent", discount), false, null);
            steps.add(new ExecutionStep("python", py.status(), py.result(), py.elapsedMs()));
            if (!"OK".equals(py.status())) evaluation = py;
            else {
                double expected = PricingMath.calculate(basePrice, quantity, discount);
                // Validate that the guest output matches the Java business formula before continuing.
                double fromPython;
                try { fromPython = Double.parseDouble(String.valueOf(py.result())); }
                catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Python must assign numeric 'result'"); }
                if (Math.abs(expected - fromPython) > 0.011) {
                    evaluation = new Evaluation("ERROR", py.output(), null, "Python price failed Java validation", List.of(), py.elapsedMs());
                } else {
                    String javascript = request.javascriptCode() == null || request.javascriptCode().isBlank()
                        ? "result = (" + quantity + " <= " + product.get("stock") + ") && (" + fromPython + " >= 0);"
                        : request.javascriptCode();
                    requireCode(javascript);
                    Evaluation js = run("js", javascript, Map.of("stock", product.get("stock"), "quantity", quantity,
                        "finalPrice", fromPython), false, null);
                    steps.add(new ExecutionStep("javascript", js.status(), js.result(), js.elapsedMs()));
                    evaluation = "OK".equals(js.status())
                        ? new Evaluation("OK", py.output() + js.output(),
                            Map.of("product", product, "price", fromPython, "javascriptValid", js.result()), "", List.of(),
                            py.elapsedMs() + js.elapsedMs())
                        : js;
                }
            }
            language = "python+javascript";
        }
        return new ExecuteResponse(UUID.randomUUID().toString(), evaluation.status(), mode, language,
            evaluation.output(), evaluation.result(), evaluation.error(), evaluation.stack(), List.copyOf(steps),
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), Instant.now());
    }

    public double priceInPython(double price, int quantity, double discount) {
        String code = "result = round(" + price + " * " + quantity + " * (1 - " + discount + " / 100), 2)";
        Evaluation outcome = run("python", code, Map.of(), false, null);
        if (!"OK".equals(outcome.status())) throw new IllegalStateException("Python pricing: " + outcome.error());
        return Double.parseDouble(String.valueOf(outcome.result()));
    }

    private void requireCode(String code) {
        if (code == null || code.isBlank() || code.length() > codeLimit)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code must have 1–" + codeLimit + " characters");
    }
    private static int requiredInt(Object obj) {
        try { return Integer.parseInt(String.valueOf(obj)); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity"); }
    }
    private static double requiredDouble(Object obj) {
        try { return Double.parseDouble(String.valueOf(obj)); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid discount"); }
    }

    private Evaluation run(String language, String code, Map<String, Object> input,
                           boolean ignored, Map<String, Object> auditMap) {
        long start = System.nanoTime();
        AtomicReference<Context> running = new AtomicReference<>();
        CappedStream stdout = new CappedStream(outputLimit);
        CappedStream stderr = new CappedStream(outputLimit);
        Future<Evaluation> job;
        try {
            job = pool.submit(() -> {
                try {
                    ResourceLimits limits = ResourceLimits.newBuilder()
                        .statementLimit(statementLimit, COUNT_ALL).build();
                    try (Context context = Context.newBuilder(language)
                        .engine(engine)
                        .allowHostAccess(HostAccess.NONE)
                        .allowHostClassLookup(s -> false)
                        .allowIO(IOAccess.NONE)
                        .allowCreateThread(false)
                        .allowNativeAccess(false)
                        .allowCreateProcess(false)
                        .allowEnvironmentAccess(EnvironmentAccess.NONE)
                        .allowPolyglotAccess(PolyglotAccess.NONE)
                        .resourceLimits(limits)
                        .out(stdout)
                        .err(stderr)
                        .build()) {
                        running.set(context);
                        Value bindings = context.getBindings(language);
                        bindings.putMember("input", GuestValues.wrap(input == null ? Map.of() : input));
                        bindings.putMember("db", db.guestProxy());
                        if (auditMap != null) bindings.putMember("audit", org.graalvm.polyglot.proxy.ProxyHashMap.from((Map) auditMap));
                        Value evaluated = context.eval(language, code);

Value result = bindings.hasMember("result")
        ? bindings.getMember("result")
        : evaluated;

// Convert guest values before closing the context.
if (auditMap != null) {
    auditMap.replaceAll((key, value) ->
        value instanceof Value guestValue
            ? unwrap(guestValue)
            : value
    );
}

Object javaResult = unwrap(result);

return new Evaluation(
    "OK",
    stdout.text(),
    javaResult,
    "",
    List.of(),
    elapsed(start)
);
                    }
                } catch (PolyglotException ex) {
                    String message = safeMessage(ex.getMessage());
                    String status = ex.isCancelled() || ex.isResourceExhausted() ? "TIMEOUT" :
                        looksLikePermissionFailure(message) ? "SANDBOX_VIOLATION" : "ERROR";
                    List<String> stack = new ArrayList<>();
                    for (PolyglotException.StackFrame f : ex.getPolyglotStackTrace()) {
                        if (f.isGuestFrame() && stack.size() < 12) stack.add(safeMessage(f.toString()));
                    }
                    if (stack.isEmpty()) stack.add(message);
                    return new Evaluation(status, stdout.text(), null, message, List.copyOf(stack), elapsed(start));
                } catch (Exception ex) {
                    return new Evaluation("ERROR", stdout.text(), null, safeMessage(ex.getMessage()), List.of("Execution failed"), elapsed(start));
                } finally { running.set(null); }
            });
        } catch (RejectedExecutionException ex) {
            return new Evaluation("ERROR", "", null, "Too many concurrent executions; retry shortly", List.of(), elapsed(start));
        }
        try {
            return job.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            Context active = running.get();
            if (active != null) try { active.close(true); } catch (Exception ignoredCloseError) { }
            job.cancel(true);
            return new Evaluation("TIMEOUT", stdout.text(), null,
                "Execution exceeded " + timeoutMs + " ms", List.of("Guest execution stopped"), elapsed(start));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            job.cancel(true);
            return new Evaluation("ERROR", stdout.text(), null, "Request interrupted", List.of(), elapsed(start));
        } catch (ExecutionException ex) {
            return new Evaluation("ERROR", stdout.text(), null, safeMessage(ex.getMessage()), List.of(), elapsed(start));
        }
    }

    private static Object unwrap(Value value) {
        if (value == null || value.isNull()) return null;
        if (value.isBoolean()) return value.asBoolean();
        if (value.isString()) return value.asString();
        if (value.isNumber()) return value.fitsInLong() ? (Object) value.asLong() : value.asDouble();
        return safeMessage(value.toString());
    }
    private static long elapsed(long start) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start); }
    private static boolean looksLikePermissionFailure(String message) {
        String m = message.toLowerCase(Locale.ROOT);
        return m.contains("not allowed") || m.contains("access is not") || m.contains("permission") ||
            m.contains("not permitted") || m.contains("not supported") || m.contains("file access") ||
            m.contains("host access") || m.contains("polyglot access") || m.contains("denied");
    }
    private static String safeMessage(String message) {
        if (message == null) return "Guest execution failed";
        String normalized = message.replaceAll("[\\r\\n]+", " ");
        return normalized.substring(0, Math.min(500, normalized.length()));
    }
    private record Evaluation(String status, String output, Object result, String error,
                              List<String> stack, long elapsedMs) {
        Evaluation withResult(Object replacement) { return new Evaluation(status, output, replacement, error, stack, elapsedMs); }
    }
    private static final class CappedStream extends OutputStream {
        private final int limit;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean truncated;
        CappedStream(int limit) { this.limit = Math.max(128, limit); }
        @Override public synchronized void write(int b) {
            if (buffer.size() < limit) buffer.write(b);
            else truncated = true;
        }
        @Override public synchronized void write(byte[] b, int off, int len) {
            int room = Math.max(0, limit - buffer.size());
            buffer.write(b, off, Math.min(room, len));
            if (len > room) truncated = true;
        }
        synchronized String text() {
            return buffer.toString(StandardCharsets.UTF_8) + (truncated ? "\n[output truncated]" : "");
        }
    }
    @PreDestroy public void shutdown() {
        pool.shutdownNow();
        engine.close();
    }
}
