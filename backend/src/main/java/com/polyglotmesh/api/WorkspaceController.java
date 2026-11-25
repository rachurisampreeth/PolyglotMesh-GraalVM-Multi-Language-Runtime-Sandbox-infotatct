package com.polyglotmesh.api;

import com.polyglotmesh.api.Models.*;
import com.polyglotmesh.data.MongoMock;
import com.polyglotmesh.model.ExecutionLog;
import com.polyglotmesh.model.Snippet;
import com.polyglotmesh.repository.ExecutionLogRepository;
import com.polyglotmesh.repository.ComparisonLogRepository;
import com.polyglotmesh.runtime.PolyglotRunner;
import com.polyglotmesh.service.SnippetService;
import com.polyglotmesh.service.ComparisonService;
import com.polyglotmesh.service.PricingMath;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import java.time.Instant;
import java.util.*;

@RestController
public class WorkspaceController {
    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);
    private final PolyglotRunner runner;
    private final ExecutionLogRepository executionLogs;
    private final ComparisonLogRepository comparisons;
    private final SnippetService snippets;
    private final MongoMock db;
    private final ComparisonService comparisonService;
    private final int mockDelayMs;

    public WorkspaceController(PolyglotRunner runner, ExecutionLogRepository executionLogs,
            ComparisonLogRepository comparisons, SnippetService snippets, MongoMock db,
            ComparisonService comparisonService, @org.springframework.beans.factory.annotation.Value("${mesh.mock-delay-ms:40}") int mockDelayMs) {
        this.runner = runner; this.executionLogs = executionLogs; this.comparisons = comparisons;
        this.snippets = snippets; this.db = db; this.comparisonService = comparisonService;
        this.mockDelayMs = mockDelayMs;
    }
    private <T> Mono<T> blocking(java.util.concurrent.Callable<T> callback) {
        return Mono.fromCallable(callback).subscribeOn(Schedulers.boundedElastic());
    }
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "PolyglotMesh", "languages", List.of("python", "javascript"),
            "security", "local-educational-demo", "timestamp", Instant.now().toString());
    }
    @PostMapping("/api/execute")
    public Mono<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        return blocking(() -> {
            ExecuteResponse result = runner.execute(request);
            executionLogs.save(new ExecutionLog(result.id(), result.mode(), result.language(), result.status(),
                result.elapsedMs(), result.timestamp()));
            log.info("execution id={} mode={} status={} elapsedMs={}", result.id(), result.mode(), result.status(), result.elapsedMs());
            return result;
        });
    }
    @PostMapping("/api/audit")
    public Mono<ExecuteResponse> audit() {
        return blocking(() -> {
            ExecuteResponse result = runner.execute(new ExecuteRequest("audit", "python", "", null, null, Map.of()));
            executionLogs.save(new ExecutionLog(result.id(), result.mode(), result.language(), result.status(),
                result.elapsedMs(), result.timestamp()));
            return result;
        });
    }
    @GetMapping("/api/templates")
    public List<Map<String, Object>> templates() {
        return List.of(
            Map.of("id", "hello-python", "title", "Python Hello", "language", "python", "code", "print('Hello from GraalPy')"),
            Map.of("id", "hello-js", "title", "JS Hello", "language", "javascript", "code", "print('Hello from GraalJS');"),
            Map.of("id", "math-python", "title", "Python Math", "language", "python", "code", "result = sum([1,2,3,4])"),
            Map.of("id", "mongo-python", "title", "MongoMock Find", "language", "python", "code", "items = db.find('products')\nresult = len(items)"),
            Map.of("id", "timeout-python", "title", "Timeout", "language", "python", "code", "while True:\n    pass"));
    }
    @GetMapping("/api/history")
    public Mono<?> history(@RequestParam(defaultValue = "20") int limit) {
        return blocking(() -> executionLogs.findTop100ByOrderByCreatedAtDesc().stream()
            .limit(Math.max(1, Math.min(100, limit))).toList());
    }
    @GetMapping("/api/metrics")
    public Mono<?> metrics() {
        return blocking(() -> {
            var recent = executionLogs.findTop100ByOrderByCreatedAtDesc();
            long successes = recent.stream().filter(x -> "OK".equals(x.status)).count();
            return Map.of("sampleSize", recent.size(), "successes", successes,
                "failures", recent.size() - successes,
                "successRate", recent.isEmpty() ? 0.0 : successes * 100.0 / recent.size(),
                "averageMs", recent.stream().mapToLong(x -> x.elapsedMs).average().orElse(0),
                "recent", recent.stream().limit(12).toList());
        });
    }
    @GetMapping("/api/metrics/history")
    public Mono<?> comparisonHistory() { return blocking(comparisons::findTop100ByOrderByCreatedAtDesc); }
    @PostMapping("/api/compare-metrics")
    public Mono<ComparisonResponse> compare(@Valid @RequestBody ComparisonRequest request) {
        return blocking(() -> comparisonService.compare(request));
    }
    @GetMapping("/api/snippets")
    public Mono<?> snippets() { return blocking(snippets::list); }
    @PostMapping("/api/snippets")
    public Mono<ResponseEntity<Snippet>> create(@Valid @RequestBody SnippetRequest request) {
        return blocking(() -> ResponseEntity.status(HttpStatus.CREATED).body(snippets.create(request)));
    }
    @PutMapping("/api/snippets/{id}")
    public Mono<?> update(@PathVariable String id, @Valid @RequestBody SnippetRequest request) {
        return blocking(() -> snippets.update(id, request));
    }
    @DeleteMapping("/api/snippets/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable String id) {
        return blocking(() -> { snippets.delete(id); return ResponseEntity.noContent().build(); });
    }
    @GetMapping("/api/data/collections")
    public List<String> collections() { return db.collections(); }
    @GetMapping("/api/data/{collection}")
    public List<Map<String, Object>> collection(@PathVariable String collection) {
        try { return db.find(collection); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }
    @PostMapping("/api/data/query")
    public List<Map<String, Object>> query(@RequestBody DataQuery q) {
        try { return db.find(q.collection(), q.filter()); }
        catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
    }
    @PostMapping("/mock/price")
    public Mono<ComparisonService.PricingResult> mockPrice(@Valid @RequestBody PricingRequest request) {
        return blocking(() -> {
            try { Thread.sleep(mockDelayMs); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Interrupted"); }
            Map<String, Object> product;
            try { product = db.product(request.productId()); }
            catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage()); }
            double price = ((Number) product.get("price")).doubleValue();
            return new ComparisonService.PricingResult(request.productId(), price, request.quantity(),
                request.discountPercent(), PricingMath.calculate(price, request.quantity(), request.discountPercent()),
                "mock-loopback-java-priced");
        });
    }
}
