package com.polyglotmesh.repository;
import com.polyglotmesh.model.ComparisonLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ComparisonLogRepository extends JpaRepository<ComparisonLog, String> {
    List<ComparisonLog> findTop100ByOrderByCreatedAtDesc();
}
