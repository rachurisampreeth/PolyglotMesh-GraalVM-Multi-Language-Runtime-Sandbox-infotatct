package com.polyglotmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comparison_logs")
public class ComparisonLog {
    @Id public String id;
    public Instant createdAt;
    public double graalAverageMs;
    public double restAverageMs;
    public int iterations;
    public boolean equalResults;
    protected ComparisonLog() {}
    public ComparisonLog(String id, Instant createdAt, double graalAverageMs,
                         double restAverageMs, int iterations, boolean equalResults) {
        this.id = id; this.createdAt = createdAt; this.graalAverageMs = graalAverageMs;
        this.restAverageMs = restAverageMs; this.iterations = iterations;
        this.equalResults = equalResults;
    }
}
