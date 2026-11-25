package com.polyglotmesh.repository;
import com.polyglotmesh.model.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, String> {
    List<ExecutionLog> findTop100ByOrderByCreatedAtDesc();
}
