package com.example.clsc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String entityName;
    private String entityId;
    private String action;
    private LocalDateTime timestamp;
    private String changedBy;
    private Map<String, List<Object>> fieldChanges;
    private Object rawDataBefore;
    private Object rawDataAfter;
}
