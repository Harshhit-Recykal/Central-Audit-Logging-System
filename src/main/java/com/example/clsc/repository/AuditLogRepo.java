package com.example.clsc.repository;

import com.example.clsc.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, String entityId);
}
