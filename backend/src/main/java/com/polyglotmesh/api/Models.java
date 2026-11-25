package com.polyglotmesh.api;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class Models {
    private Models() {}
    public record ExecuteRequest(String mode, String language, String code, String pythonCode,
                                 String javascriptCode, Map<String, Object> input) {}
    public record ExecutionStep(String language, String status, Object result, long elapsedMs) {}
    public record ExecuteResponse(String id, String status, String mode, String language, String output,
                                  Object result, String error, List<String> stack, List<ExecutionStep> steps,
                                  long elapsedMs, Instant timestamp) {}
    public record SnippetRequest(@NotBlank @Size(max = 80) String title, @NotBlank String language,
                                 @NotBlank String code, Long expectedVersion) {}
    public record PricingRequest(@NotBlank String productId, @Min(1) @Max(10000) int quantity,
                                 @DecimalMin("0.0") @DecimalMax("100.0") double discountPercent) {}
    public record ComparisonRequest(@NotNull @Valid PricingRequest pricing, @Min(1) @Max(20) int iterations) {}
    public record TimingStats(String adapter, String measurementType, double averageMs,
                              double minMs, double maxMs, List<Double> sampleMs) {}
    public record ComparisonResponse(String id, Object result, boolean equalResults,
                                     List<TimingStats> metrics, String caveat, Instant timestamp) {}
    public record DataQuery(String collection, Map<String, Object> filter) {}
}
