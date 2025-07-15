package com.example.clsc.controllers;

import com.example.clsc.dto.ApiResponse;
import com.example.clsc.dto.AuditLogDto;
import com.example.clsc.service.AuditLogService;
import com.example.clsc.service.criteria.AuditLogCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAllAuditLogs() {
        List<AuditLogDto> logs = auditLogService.getAllLogs();
        return ResponseEntity.ok().body(new ApiResponse<>(true, "Audit logs fetched successfully", logs));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<AuditLogDto>>> getAuditLogs(@ModelAttribute AuditLogCriteria criteria,
                 @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        List<AuditLogDto> filteredLogs = auditLogService.getLogsByFilter(criteria, pageable);
        return ResponseEntity.ok().body(new ApiResponse<>(true, "Filtered Audit logs fetched successfully", filteredLogs));
    }
}
