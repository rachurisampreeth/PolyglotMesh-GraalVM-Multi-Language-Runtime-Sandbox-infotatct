package com.polyglotmesh.comparison;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;

import com.polyglotmesh.comparison.ComparisonModels.MockRestRequest;
import com.polyglotmesh.comparison.ComparisonModels.MockRestResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
public class MockRestExecutionAdapter implements ExecutionAdapter {

    private final WebClient client;

    public MockRestExecutionAdapter(
            WebClient.Builder builder,
            @Value("${server.port:7071}") int port) {

        // Loopback only; not an external microservice.
        this.client = builder
                .baseUrl("http://127.0.0.1:" + port)
                .build();
    }

    @Override
    public String name() {
        return "mock-rest";
    }

    @Override
    public String measurementType() {
        return "loopback-http-echo";
    }

    @Override
    public String note() {

        return "Real HTTP round trip to a local echo endpoint; "
                + "does NOT execute guest code or model a remote service.";
    }

    @Override
    public void invoke(ExecuteRequest request) {

        MockRestResponse response = client
                .post()
                .uri("/api/mock-rest/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(
                    new MockRestRequest(
                        request.input() == null
                            ? Map.of()
                            : request.input()
                    )
                )
                .retrieve()
                .bodyToMono(MockRestResponse.class)
                .block(Duration.ofSeconds(10));

        if (response == null ||
                !"success".equals(response.status())) {

            throw new IllegalStateException(
                "Mock REST endpoint did not return success"
            );
        }
    }
}