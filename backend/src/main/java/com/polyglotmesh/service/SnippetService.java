package com.polyglotmesh.service;

import com.polyglotmesh.api.Models.SnippetRequest;
import com.polyglotmesh.model.Snippet;
import com.polyglotmesh.repository.SnippetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
public class SnippetService {
    private final SnippetRepository repo;
    public SnippetService(SnippetRepository repo) { this.repo = repo; }
    public List<Snippet> list() { return repo.findAllByOrderByUpdatedAtDesc(); }
    private void validate(SnippetRequest request) {
        if (request == null || request.title() == null || request.title().isBlank() || request.title().length() > 80 ||
            request.code() == null || request.code().isBlank() || request.code().length() > 16000 ||
            request.language() == null || !List.of("python", "javascript", "java").contains(request.language()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid snippet title, language or code");
    }
    public Snippet create(SnippetRequest request) {
        validate(request);
        return repo.save(new Snippet(request.title(), request.language(), request.code()));
    }
    public Snippet update(String id, SnippetRequest request) {
        validate(request);
        Snippet saved = repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found"));
        if (request.expectedVersion() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "expectedVersion is required");
        if (request.expectedVersion() != saved.version) throw new ResponseStatusException(HttpStatus.CONFLICT, "Snippet version conflict");
        saved.title = request.title(); saved.language = request.language(); saved.code = request.code();
        saved.version++; saved.updatedAt = Instant.now();
        return repo.save(saved);
    }
    public void delete(String id) {
        if (!repo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Snippet not found");
        repo.deleteById(id);
    }
}
