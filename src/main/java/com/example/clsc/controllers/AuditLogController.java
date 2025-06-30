package com.example.clsc.controllers;

import com.example.clsc.dto.ApiResponse;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("api/logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Autowired
    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllLogs();
        return ResponseEntity.ok().body(new ApiResponse<>(true, "Audit logs fetched successfully", logs));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AuditLog>> findLogs(@RequestParam(required = false) String entityName,
                                                   @RequestParam(required = false) String entityId,
                                                   @RequestParam(required = false) ActionType action,
                                                   @RequestParam(required = false)String changedBy,
                                                   @RequestParam(required = false) LocalDateTime changedAt,
                                                   @RequestParam(defaultValue = "1") Integer pageNumber,
                                                   @RequestParam(defaultValue = "20") Integer pageSize) {
       List<AuditLog> logs =  auditLogService.findLogs(entityName, entityId, action,changedBy,changedAt, pageNumber, pageSize);
       return ResponseEntity.ok().body(logs);
    }
}
