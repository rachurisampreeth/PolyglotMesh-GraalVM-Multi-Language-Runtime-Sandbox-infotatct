package com.polyglotmesh.service;

import com.polyglotmesh.api.Models.*;
import com.polyglotmesh.data.MongoMock;
import com.polyglotmesh.model.ComparisonLog;
import com.polyglotmesh.repository.ComparisonLogRepository;
import com.polyglotmesh.runtime.PolyglotRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class ComparisonService {
    private final PolyglotRunner runner;
    private final MongoMock data;
    private final WebClient client;
    private final ComparisonLogRepository repository;
    public ComparisonService(PolyglotRunner runner, MongoMock data,
                             ComparisonLogRepository repository,
                             @org.springframework.beans.factory.annotation.Value("${mesh.mock-base-url:http://localhost:7071}") String baseUrl) {
        this.runner = runner; this.data = data; this.repository = repository;
        this.client = WebClient.builder().baseUrl(baseUrl).build();
    }
    public ComparisonResponse compare(ComparisonRequest request) {
        if (request == null || request.pricing() == null || request.iterations() < 1 || request.iterations() > 20)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide pricing and iterations 1–20");
        PricingRequest p = request.pricing();
        if (p.quantity() < 1 || p.quantity() > 10000 || p.discountPercent() < 0 || p.discountPercent() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pricing inputs");
        Map<String, Object> product;
        try { product = data.product(p.productId()); }
        catch (IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage()); }
        double unitPrice = ((Number) product.get("price")).doubleValue();
        List<Double> graalTimes = new ArrayList<>();
        List<Double> restTimes = new ArrayList<>();
        // Exclude one deliberate warm-up iteration per adapter from reported timings.
        runner.priceInPython(unitPrice, p.quantity(), p.discountPercent());
        client.post().uri("/mock/price").bodyValue(p).retrieve()
            .bodyToMono(PricingResult.class).block(Duration.ofSeconds(10));
        double graalValue = 0, restValue = 0;
        for (int i = 0; i < request.iterations(); i++) {
            long gStart = System.nanoTime();
            graalValue = runner.priceInPython(unitPrice, p.quantity(), p.discountPercent());
            graalTimes.add((System.nanoTime() - gStart) / 1_000_000.0);
            long rStart = System.nanoTime();
            PricingResult response = client.post().uri("/mock/price").bodyValue(p)
                .retrieve().bodyToMono(PricingResult.class).block(Duration.ofSeconds(10));
            if (response == null) throw new IllegalStateException("REST response was empty");
            restValue = response.total();
            restTimes.add((System.nanoTime() - rStart) / 1_000_000.0);
        }
        boolean equal = Math.abs(graalValue - restValue) <= 0.011;
        String id = UUID.randomUUID().toString();
        Instant at = Instant.now();
        TimingStats graal = stats("graalvm-python", "in-process-python", graalTimes);
        TimingStats rest = stats("mock-rest", "loopback-http-java", restTimes);
        repository.save(new ComparisonLog(id, at, graal.averageMs(), rest.averageMs(), request.iterations(), equal));
        return new ComparisonResponse(id, Map.of("graalvmPrice", graalValue, "restPrice", restValue), equal,
            List.of(graal, rest),
            "Equivalent pricing formula; different implementations, request overhead and configured HTTP delay make this illustrative, not a scientific benchmark. No causal speed claim.", at);
    }
    private TimingStats stats(String name, String kind, List<Double> samples) {
        var s = samples.stream().mapToDouble(Double::doubleValue).summaryStatistics();
        return new TimingStats(name, kind, s.getAverage(), s.getMin(), s.getMax(), List.copyOf(samples));
    }
    public record PricingResult(String productId, double unitPrice, int quantity,
                                double discountPercent, double total, String implementation) {}
}
