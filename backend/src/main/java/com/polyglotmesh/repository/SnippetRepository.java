package com.polyglotmesh.repository;
import com.polyglotmesh.model.Snippet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SnippetRepository extends JpaRepository<Snippet, String> {
    List<Snippet> findAllByOrderByUpdatedAtDesc();
}
