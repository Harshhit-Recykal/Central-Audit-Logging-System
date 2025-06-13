package com.example.clsc.controllers;

import com.example.clsc.dto.ApiResponse;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/logs")
public class LogController {

    @Autowired

    private AuditLogService auditLogService;

    @GetMapping("/all")
    public ApiResponse<List<AuditLog>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllLogs();
        return new ApiResponse<>(true, "Audit logs fetched successfully", logs);
    }
}
