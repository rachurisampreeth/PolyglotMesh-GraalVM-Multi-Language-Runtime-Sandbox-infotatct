package com.polyglotmesh.comparison;

import com.polyglotmesh.comparison.ComparisonModels.*;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ComparisonService {

    private final List<ExecutionAdapter> adapters;

    public ComparisonService(List<ExecutionAdapter> adapters) {

        this.adapters = List.copyOf(adapters);
    }

    public CompareResponse compare(CompareRequest request) {

        if (request == null ||
                request.execution() == null ||
                request.iterations() < 1 ||
                request.iterations() > 10) {

            throw new IllegalArgumentException(
                "execution is required; iterations must be 1..10"
            );
        }

        List<AdapterMetric> metrics = new ArrayList<>();

        for (ExecutionAdapter adapter : adapters) {

            List<Double> samples = new ArrayList<>();

            for (int i = 0; i < request.iterations(); i++) {

                long start = System.nanoTime();

                adapter.invoke(request.execution());

                double elapsed =
                    (System.nanoTime() - start) / 1_000_000.0;

                samples.add(elapsed);
            }

            double min = samples.stream()
                    .mapToDouble(Double::doubleValue)
                    .min()
                    .orElse(0);

            double max = samples.stream()
                    .mapToDouble(Double::doubleValue)
                    .max()
                    .orElse(0);

            double avg = samples.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            metrics.add(
                new AdapterMetric(
                    adapter.name(),
                    adapter.measurementType(),
                    samples.size(),
                    avg,
                    min,
                    max,
                    List.copyOf(samples),
                    adapter.note()
                )
            );
        }

        return new CompareResponse(
            List.copyOf(metrics),

            "Illustrative comparison only: GraalVM executes guest code, "
            + "while mock REST only echoes input. "
            + "The workloads differ, so these numbers cannot establish "
            + "a performance advantage. "
            + "JVM memory deltas are not compared because "
            + "they are process-wide and noisy."
        );
    }
}