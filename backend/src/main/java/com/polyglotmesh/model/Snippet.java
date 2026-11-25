package com.polyglotmesh.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "snippets")
public class Snippet {
    @Id public String id;
    @Column(nullable = false, length = 80) public String title;
    @Column(nullable = false, length = 20) public String language;
    @Column(nullable = false, length = 16000) public String code;
    @Column(nullable = false) public long version;
    @Column(nullable = false) public Instant updatedAt;
    protected Snippet() {}
    public Snippet(String title, String language, String code) {
        this.id = java.util.UUID.randomUUID().toString();
        this.title = title; this.language = language; this.code = code;
        this.version = 1; this.updatedAt = Instant.now();
    }
}
