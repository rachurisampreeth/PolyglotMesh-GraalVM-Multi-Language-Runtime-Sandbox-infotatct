package com.polyglotmesh.comparison;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public final class ComparisonModels {

    private ComparisonModels() {}

    public record CompareRequest(

        @NotNull
        @Valid
        ExecuteRequest execution,

        @Min(1)
        @Max(10)
        int iterations

    ) {}

    public record AdapterMetric(

        String adapter,

        String measurementType,

        int samples,

        double averageMs,

        double minMs,

        double maxMs,

        List<Double> sampleMs,

        String note

    ) {}

    public record CompareResponse(

        List<AdapterMetric> metrics,

        String disclaimer

    ) {}

    public record MockRestRequest(

        Map<String, Object> input

    ) {}

    public record MockRestResponse(

        String status,

        Map<String, Object> echoedInput,

        String implementation

    ) {}
}
