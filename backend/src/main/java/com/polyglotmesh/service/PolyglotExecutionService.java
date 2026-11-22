package com.polyglotmesh.service;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;
import com.polyglotmesh.api.ApiModels.ExecuteResponse;
import com.polyglotmesh.api.ApiModels.ExecutionStep;
import jakarta.annotation.PreDestroy;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.graalvm.polyglot.proxy.ProxyHashMap;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Local demonstration runner. Host file, socket, class lookup and thread permissions
 * are denied, but this is NOT a hardened multi-tenant / internet-facing sandbox.
 */
@Service
public class PolyglotExecutionService {
    private final int timeoutMs;
    private final int maxCodeChars;
    private final int maxOutputBytes;
    private final Engine engine = Engine.create();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(
        2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8),
        task -> {
            Thread thread = new Thread(task, "polyglotmesh-guest");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.AbortPolicy()
    );

    public PolyglotExecutionService(
        @org.springframework.beans.factory.annotation.Value("${mesh.execution.timeout-ms:10000}") int timeoutMs,
        @org.springframework.beans.factory.annotation.Value("${mesh.execution.max-code-chars:16000}") int maxCodeChars,
        @org.springframework.beans.factory.annotation.Value("${mesh.execution.max-output-bytes:32768}") int maxOutputBytes
    ) {
        this.timeoutMs = timeoutMs;
        this.maxCodeChars = maxCodeChars;
        this.maxOutputBytes = maxOutputBytes;
    }

    public ExecuteResponse execute(ExecuteRequest request) {
        if (request == null) throw new IllegalArgumentException("Missing request body.");
        String mode = "pipeline".equalsIgnoreCase(request.mode()) ? "pipeline" : "single";
        String language = normalizeLanguage(request.language());
        validate(request, mode, language);
        String id = UUID.randomUUID().toString();
        AtomicReference<Context> runningContext = new AtomicReference<>();
        long started = System.nanoTime();
        long beforeMemory = usedHeap();

        Future<Outcome> future = workers.submit(() -> evaluate(request, mode, language, runningContext));
        Outcome outcome;
        try {
            outcome = future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            // Close from the request thread to cancel guest execution at a GraalVM safepoint.
            // A stubborn guest may not terminate immediately; restrict this app to localhost.
            Context context = runningContext.get();
            if (context != null) {
                try { context.close(true); } catch (Exception ignored) { }
            }
            future.cancel(true);
            outcome = new Outcome("timeout", "", "", "Execution exceeded " + timeoutMs + " ms.", List.of());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            outcome = new Outcome("error", "", "", "Execution interrupted.", List.of());
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            outcome = new Outcome("error", "", "", safeMessage(cause), List.of());
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        long heapDeltaKb = Math.max(0L, (usedHeap() - beforeMemory) / 1024);
        return new ExecuteResponse(id, outcome.status(), mode,
            mode.equals("pipeline") ? "python â†’ javascript" : language,
            outcome.output(), outcome.result(), outcome.error(), elapsedMs,
            heapDeltaKb, outcome.steps(), Instant.now());
    }

    private Outcome evaluate(ExecuteRequest request, String mode, String language,
                             AtomicReference<Context> runningContext) {
        LimitedOutput output = new LimitedOutput(maxOutputBytes);
        List<ExecutionStep> steps = new ArrayList<>();
        String result = "";
        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Cancelled before start");
            }
            String[] enabled = mode.equals("pipeline")
                ? new String[]{"python", "js"}
                : new String[]{language.equals("javascript") ? "js" : "python"};
            // Restrict every host capability we can without requiring a custom image.
            Context context = Context.newBuilder(enabled)
                .engine(engine)
                .allowHostAccess(HostAccess.NONE)
                .allowHostClassLookup(name -> false)
                .allowIO(IOAccess.NONE)
                .allowCreateThread(false)
                .allowNativeAccess(false)
                .allowCreateProcess(false)
                .out(output)
                .err(output)
                .build();
            runningContext.set(context);
            try (context) {
                Map<String, Object> input = new LinkedHashMap<>(request.input() == null ? Map.of() : request.input());
                if (mode.equals("pipeline")) {
                    long javaStart = System.nanoTime();
                    // A guest hash proxy exposes Java Map entries to Python as dict keys,
                    // without exposing Java host methods or classes.
                    Map<Object, Object> pythonInput = new LinkedHashMap<>();
                    pythonInput.putAll(input);
                    context.getBindings("python").putMember("input_data", ProxyHashMap.from(pythonInput));
                    steps.add(new ExecutionStep("Java input binding", "java", millisSince(javaStart), "success"));
                    long pythonStart = System.nanoTime();
                    context.eval("python", request.pythonCode());
                    steps.add(new ExecutionStep("GraalPy calculation", "python", millisSince(pythonStart), "success"));
                    Value pyResult = context.getBindings("python").getMember("result");
                    String pyValue = stringify(pyResult);
                    context.getBindings("js").putMember("python_result", pyValue);
                    context.getBindings("js").putMember("input_data", ProxyObject.fromMap(input));
                    long jsStart = System.nanoTime();
                    context.eval("js", request.javascriptCode());
                    steps.add(new ExecutionStep("GraalJS validation", "javascript", millisSince(jsStart), "success"));
                    result = stringify(context.getBindings("js").getMember("result"));
                } else {
                    String graalLanguage = language.equals("javascript") ? "js" : "python";
                    if (graalLanguage.equals("python")) {
                        Map<Object, Object> pythonInput = new LinkedHashMap<>();
                        pythonInput.putAll(input);
                        context.getBindings("python").putMember("input_data", ProxyHashMap.from(pythonInput));
                    } else {
                        context.getBindings("js").putMember("input_data", ProxyObject.fromMap(input));
                    }
                    long stageStart = System.nanoTime();
                    context.eval(graalLanguage, request.code());
                    steps.add(new ExecutionStep("Run " + language, language, millisSince(stageStart), "success"));
                    result = stringify(context.getBindings(graalLanguage).getMember("result"));
                }
            } finally {
                runningContext.compareAndSet(context, null);
            }
            return new Outcome("success", output.content(), truncate(result, 12000), "", steps);
        } catch (PolyglotException ex) {
            String message = ex.isCancelled() ? "Execution was cancelled (time limit)." : safeMessage(ex);
            return new Outcome("error", output.content(), "", message, steps);
        } catch (Exception ex) {
            return new Outcome("error", output.content(), "", safeMessage(ex), steps);
        }
    }

    private void validate(ExecuteRequest request, String mode, String language) {
        if (!"single".equalsIgnoreCase(request.mode()) && !"pipeline".equalsIgnoreCase(request.mode()))
            throw new IllegalArgumentException("mode must be single or pipeline.");
        if (mode.equals("single") && !Set.of("python", "javascript").contains(language))
            throw new IllegalArgumentException("language must be python or javascript.");
        if (mode.equals("single")) validateCode(request.code(), "code");
        else {
            validateCode(request.pythonCode(), "pythonCode");
            validateCode(request.javascriptCode(), "javascriptCode");
        }
        if (request.input() != null) {
            if (request.input().size() > 24) throw new IllegalArgumentException("At most 24 input keys.");
            for (var entry : request.input().entrySet()) {
                if (entry.getKey() == null || entry.getKey().length() > 80)
                    throw new IllegalArgumentException("Invalid input key.");
                Object value = entry.getValue();
                if (value != null && !(value instanceof String || value instanceof Number || value instanceof Boolean))
                    throw new IllegalArgumentException("Input values must be simple strings, numbers, booleans or null.");
                if (value instanceof String s && s.length() > 1000)
                    throw new IllegalArgumentException("Input string exceeds 1000 characters.");
            }
        }
    }

    private void validateCode(String code, String field) {
        if (code == null || code.isBlank()) throw new IllegalArgumentException(field + " cannot be blank.");
        if (code.length() > maxCodeChars) throw new IllegalArgumentException(field + " is too long.");
    }
    private String normalizeLanguage(String language) { return language == null ? "" : language.toLowerCase(Locale.ROOT); }
    private String stringify(Value value) {
        if (value == null || value.isNull()) return "(no result variable; use result = ...)";
        try { return value.isString() ? value.asString() : value.toString(); }
        catch (Exception ex) { return "(result unavailable)"; }
    }
    private long usedHeap() { Runtime rt=Runtime.getRuntime(); return rt.totalMemory()-rt.freeMemory(); }
    private long millisSince(long since) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-since); }
    private String safeMessage(Throwable ex) { return truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), 1600); }
    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max) + "â€¦"; }
    @PreDestroy
    public void shutdown() { workers.shutdownNow(); engine.close(); }

    private record Outcome(String status, String output, String result, String error, List<ExecutionStep> steps) {}
    private static final class LimitedOutput extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final int max;
        private boolean truncated;
        private LimitedOutput(int max) { this.max = max; }
        @Override public synchronized void write(int b) {
            if (buffer.size() < max) buffer.write(b); else truncated = true;
        }
        @Override public synchronized void write(byte[] b, int off, int len) {
            int space = Math.max(0, max-buffer.size());
            buffer.write(b, off, Math.min(len, space));
            if (len > space) truncated = true;
        }
        private synchronized String content() {
            return buffer.toString(java.nio.charset.StandardCharsets.UTF_8)
                + (truncated ? "\n[Output truncated at configured size]" : "");
        }
    }
}
