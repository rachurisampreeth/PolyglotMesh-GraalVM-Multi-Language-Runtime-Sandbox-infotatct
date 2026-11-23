package com.polyglotmesh.comparison;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;
import com.polyglotmesh.api.ApiModels.ExecuteResponse;
import com.polyglotmesh.service.PolyglotExecutionService;

import org.springframework.stereotype.Component;

@Component
public class GraalVmExecutionAdapter implements ExecutionAdapter {

    private final PolyglotExecutionService executionService;

    public GraalVmExecutionAdapter(
            PolyglotExecutionService executionService) {

        this.executionService = executionService;
    }

    @Override
    public String name() {
        return "graalvm";
    }

    @Override
    public String measurementType() {
        return "in-process-execution";
    }

    @Override
    public String note() {
        return "Java wall-clock time for existing GraalVM execution; includes runtime setup.";
    }

    @Override
    public void invoke(ExecuteRequest request) {

        ExecuteResponse response = executionService.execute(request);

        if (!"success".equalsIgnoreCase(response.status())) {

            throw new IllegalArgumentException(
                "GraalVM execution failed: " + response.error()
            );
        }
    }
}