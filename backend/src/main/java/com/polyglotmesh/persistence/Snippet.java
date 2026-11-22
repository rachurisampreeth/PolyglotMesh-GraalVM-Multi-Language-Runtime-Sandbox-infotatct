package com.polyglotmesh.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="snippets")
public class Snippet {
    @Id
    private String id;
    @Column(nullable=false, length=80)
    private String title;
    @Column(nullable=false, length=20)
    private String mode;
    @Column(length=20)
    private String language;
    @Lob
    @Column(columnDefinition="CLOB")
    private String code;
    @Lob
    @Column(columnDefinition="CLOB")
    private String pythonCode;
    @Lob
    @Column(columnDefinition="CLOB")
    private String javascriptCode;
    private Instant updatedAt;

    public Snippet() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPythonCode() { return pythonCode; }
    public void setPythonCode(String pythonCode) { this.pythonCode = pythonCode; }
    public String getJavascriptCode() { return javascriptCode; }
    public void setJavascriptCode(String javascriptCode) { this.javascriptCode = javascriptCode; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
