package com.polyglotmesh.api;

import com.polyglotmesh.comparison.ComparisonModels.*;
import com.polyglotmesh.comparison.ComparisonService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(
            ComparisonService comparisonService) {

        this.comparisonService = comparisonService;
    }

    @PostMapping("/compare-metrics")
    public Mono<CompareResponse> compare(
            @Valid @RequestBody CompareRequest request) {

        return Mono.fromCallable(
                () -> comparisonService.compare(request)
        ).subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/mock-rest/execute")
    public MockRestResponse mockExecute(
            @RequestBody MockRestRequest request) {

        return new MockRestResponse(
            "success",
            request.input() == null
                ? Map.of()
                : request.input(),
            "local-http-echo"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(
            IllegalArgumentException ex) {

        return Map.of(
            "error",
            ex.getMessage() == null
                ? "Invalid request"
                : ex.getMessage()
        );
    }
}