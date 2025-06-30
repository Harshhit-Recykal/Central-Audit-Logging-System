package com.example.clsc.service;

import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.repository.AuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepo auditLogRepo;

    @Autowired
    public AuditLogService(AuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepo.findAll();
    }
            /**
              * Finds and filters audit logs based on the provided criteria.
              *
              * @param entityName Optional: The name of the entity (e.g., "Product").
              * @param entityId Optional: The ID of the entity record.
              * @param action Optional: The type of action performed (e.g., CREATE, UPDATE).
              * @param changedBy Optional: The user who performed the action.
             * @param changedAt Optional: The time at which the log changedAt or got created
              * @param pageNumber The page number for pagination (1-based).
              * @param pageSize The number of records per page.
              * @return A list of AuditLog entities matching the criteria.
              */
            @Transactional
                   public List<AuditLog> findLogs(
              String entityName,
              String entityId,
              ActionType action,
              String changedBy,
              LocalDateTime changedAt,
              Integer pageNumber,
              Integer pageSize) {

                 // conversion of enum type to String
                 String actionName = (action != null) ? action.name() : null;

                 // Call the repository method. Spring Data JPA handles the rest.
                 return auditLogRepo.filterAuditLogs(
                                  entityName,
                                 entityId,
                                 actionName,
                                 changedBy,
                                 changedAt,
                                 pageNumber,
                                 pageSize
                         );
            }

}
