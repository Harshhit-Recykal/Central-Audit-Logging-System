package com.example.clsc.service;

import com.example.clsc.entity.AuditLog;
import com.example.clsc.repository.AuditLogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
}
