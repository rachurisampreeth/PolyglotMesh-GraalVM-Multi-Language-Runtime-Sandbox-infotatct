package com.polyglotmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "execution_logs")
public class ExecutionLog {
    @Id public String id;
    public String mode;
    public String language;
    public String status;
    public long elapsedMs;
    public Instant createdAt;
    protected ExecutionLog() {}
    public ExecutionLog(String id, String mode, String language, String status, long elapsedMs, Instant createdAt) {
        this.id = id; this.mode = mode; this.language = language;
        this.status = status; this.elapsedMs = elapsedMs; this.createdAt = createdAt;
    }
}
