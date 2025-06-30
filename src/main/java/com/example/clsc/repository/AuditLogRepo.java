package com.example.clsc.repository;

import com.example.clsc.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepo extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, String entityId);

    @Procedure(name = "filter_audit_log")
    List<AuditLog> filterAuditLogs(
            @Param("p_entity_name")  String entityName,
            @Param("p_entity_id") String entityId,
            @Param("p_action") String action,
            @Param("p_changed_by") String changedBy,
            @Param("p_changed_at") LocalDateTime changedAt,
            @Param("p_page_number") Integer pageNumber,
            @Param("p_page_size") Integer pageSize
    );
}
