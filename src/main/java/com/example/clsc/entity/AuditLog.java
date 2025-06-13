package com.example.clsc.entity;

import com.example.clsc.enums.ActionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "entity_name")
    private String entityName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action")
    private ActionType action;

    @Column(name = "changed_by")
    private String changedBy;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    @Column(name = "request_id")
    private String requestId;

    @Lob
    @Column(name = "field_changes")
    private String fieldChanges;

    @Lob
    @Column(name = "raw_data_before")
    private String rawDataBefore;

    @Lob
    @Column(name = "raw_data_after", columnDefinition = "TEXT")
    private String rawDataAfter;

}
