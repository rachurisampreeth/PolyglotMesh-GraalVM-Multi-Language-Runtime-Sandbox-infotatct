package com.polyglotmesh.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="execution_logs")
public class ExecutionLog {
    @Id
    private String id;
    private Instant timestamp;
    private String status;
    private String mode;
    private String language;
    private long elapsedMs;
    @Column(length=512)
    private String summary;

    public ExecutionLog() {}
    public ExecutionLog(String id, Instant timestamp, String status, String mode, String language,
                        long elapsedMs, String summary) {
        this.id=id; this.timestamp=timestamp; this.status=status; this.mode=mode;
        this.language=language; this.elapsedMs=elapsedMs; this.summary=summary;
    }
    public String getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public String getMode() { return mode; }
    public String getLanguage() { return language; }
    public long getElapsedMs() { return elapsedMs; }
    public String getSummary() { return summary; }
}
