package com.polyglotmesh.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, String> {
    List<ExecutionLog> findAllByOrderByTimestampDesc(Pageable pageable);
    long countByStatus(String status);
}
