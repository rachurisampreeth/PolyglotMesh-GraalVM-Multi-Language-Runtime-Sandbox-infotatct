package com.polyglotmesh.comparison;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;
import com.polyglotmesh.comparison.ComparisonModels.CompareRequest;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonServiceTest {

    @Test
    void aggregatesAdapterSamples() {

        AtomicInteger calls = new AtomicInteger();

        ExecutionAdapter fake = new ExecutionAdapter() {

            public String name() {
                return "fake";
            }

            public String measurementType() {
                return "unit-test";
            }

            public String note() {
                return "test only";
            }

            public void invoke(ExecuteRequest request) {
                calls.incrementAndGet();
            }
        };

        var service = new ComparisonService(List.of(fake));

        var execution = new ExecuteRequest(
            "single",
            "javascript",
            "result = 1",
            null,
            null,
            Map.of()
        );

        var result = service.compare(
            new CompareRequest(execution, 3)
        );

        assertEquals(3, calls.get());

        assertEquals(1, result.metrics().size());

        assertEquals(3, result.metrics().get(0).samples());

        assertEquals(3, result.metrics().get(0).sampleMs().size());

        assertTrue(
            result.metrics().get(0).averageMs() >= 0
        );
    }

    @Test
    void rejectsInvalidIterations() {

        var service = new ComparisonService(List.of());

        var execution = new ExecuteRequest(
            "single",
            "javascript",
            "result = 1",
            null,
            null,
            Map.of()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.compare(
                new CompareRequest(execution, 0)
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.compare(
                new CompareRequest(execution, 11)
            )
        );
    }
}