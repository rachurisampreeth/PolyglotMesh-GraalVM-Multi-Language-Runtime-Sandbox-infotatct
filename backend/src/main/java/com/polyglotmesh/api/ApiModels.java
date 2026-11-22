package com.polyglotmesh.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ApiModels {
    private ApiModels() {}

    public record ExecuteRequest(
        String mode,
        String language,
        String code,
        String pythonCode,
        String javascriptCode,
        Map<String, Object> input
    ) {}

    public record ExecutionStep(String name, String language, long elapsedMs, String status) {}

    public record ExecuteResponse(
        String id, String status, String mode, String language,
        String output, String result, String error,
        long elapsedMs, long heapDeltaKb,
        List<ExecutionStep> steps, Instant timestamp
    ) {}

    public record SnippetRequest(
        @NotBlank @Size(max = 80) String title,
        @NotBlank String mode,
        String language,
        String code,
        String pythonCode,
        String javascriptCode
    ) {}

    public record Template(String id, String title, String subtitle, String mode,
                           String language, String code, String pythonCode,
                           String javascriptCode, Map<String, Object> input) {}
}
