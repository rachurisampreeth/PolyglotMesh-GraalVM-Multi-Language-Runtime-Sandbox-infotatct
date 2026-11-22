package com.polyglotmesh.api;

import com.polyglotmesh.api.ApiModels.*;
import com.polyglotmesh.persistence.*;
import com.polyglotmesh.service.PolyglotExecutionService;
import com.polyglotmesh.service.TemplateCatalog;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class WorkspaceController {
    private final PolyglotExecutionService executionService;
    private final TemplateCatalog templates;
    private final SnippetRepository snippets;
    private final ExecutionLogRepository logs;

    public WorkspaceController(PolyglotExecutionService executionService, TemplateCatalog templates,
                               SnippetRepository snippets, ExecutionLogRepository logs) {
        this.executionService=executionService; this.templates=templates; this.snippets=snippets; this.logs=logs;
    }

    @GetMapping("/health")
    public Map<String,Object> health() {
        return Map.of("status", "UP", "service", "PolyglotMesh", "runtime", "GraalVM Polyglot",
            "languages", List.of("python", "javascript"), "timestamp", Instant.now().toString(),
            "security", "restricted-local-demo");
    }

    @GetMapping("/templates")
    public List<Template> getTemplates() { return templates.getTemplates(); }

    @PostMapping("/execute")
    public Mono<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        return Mono.fromCallable(() -> {
            ExecuteResponse result = executionService.execute(request);
            String summary = result.error().isBlank() ? result.result() : result.error();
            logs.save(new ExecutionLog(result.id(), result.timestamp(), result.status(),
                result.mode(), result.language(), result.elapsedMs(),
                summary.length() > 500 ? summary.substring(0,500) : summary));
            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/history")
    public Mono<List<ExecutionLog>> history(@RequestParam(defaultValue="20") int limit) {
        return Mono.fromCallable(() -> logs.findAllByOrderByTimestampDesc(
            PageRequest.of(0, Math.min(100, Math.max(1,limit)))))
            .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/metrics")
    public Mono<Map<String,Object>> metrics() {
        return Mono.fromCallable(() -> {
            List<ExecutionLog> recent = logs.findAllByOrderByTimestampDesc(PageRequest.of(0,40));
            long total = logs.count();
            long successes = logs.countByStatus("success");
            double average = recent.stream().mapToLong(ExecutionLog::getElapsedMs).average().orElse(0);
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("total", total);
            result.put("successes", successes);
            result.put("failures", total - successes);
            result.put("successRate", total == 0 ? 0 : Math.round(1000.0 * successes / total) / 10.0);
            result.put("averageMs", Math.round(average));
            result.put("recent", recent.stream().limit(12).toList());
            return result;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/snippets")
    public Mono<List<Snippet>> snippets() {
        return Mono.fromCallable(snippets::findAllByOrderByUpdatedAtDesc)
            .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/snippets")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Snippet> createSnippet(@Valid @RequestBody SnippetRequest req) {
        return Mono.fromCallable(() -> {
            Snippet snippet = new Snippet();
            snippet.setId(UUID.randomUUID().toString());
            apply(req, snippet);
            return snippets.save(snippet);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/snippets/{id}")
    public Mono<Snippet> updateSnippet(@PathVariable String id, @Valid @RequestBody SnippetRequest req) {
        return Mono.fromCallable(() -> {
            Snippet snippet = snippets.findById(id).orElseThrow(() ->
                new NoSuchElementException("Snippet not found: " + id));
            apply(req, snippet);
            return snippets.save(snippet);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/snippets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteSnippet(@PathVariable String id) {
        return Mono.fromRunnable(() -> {
            if (!snippets.existsById(id)) throw new NoSuchElementException("Snippet not found: " + id);
            snippets.deleteById(id);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private void apply(SnippetRequest req, Snippet snippet) {
        if (!Set.of("single", "pipeline").contains(req.mode()))
            throw new IllegalArgumentException("Invalid snippet mode");
        if ("single".equals(req.mode()) && !Set.of("python", "javascript").contains(req.language()))
            throw new IllegalArgumentException("Invalid snippet language");
        String code = req.code()==null ? "" : req.code();
        String python = req.pythonCode()==null ? "" : req.pythonCode();
        String javascript = req.javascriptCode()==null ? "" : req.javascriptCode();
        if (code.length() > 16000 || python.length() > 16000 || javascript.length() > 16000)
            throw new IllegalArgumentException("Snippet is too large");
        snippet.setTitle(req.title()); snippet.setMode(req.mode()); snippet.setLanguage(req.language());
        snippet.setCode(code); snippet.setPythonCode(python);
        snippet.setJavascriptCode(javascript); snippet.setUpdatedAt(Instant.now());
    }
}
