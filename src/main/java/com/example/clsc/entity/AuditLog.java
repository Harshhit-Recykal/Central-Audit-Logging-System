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

@NamedStoredProcedureQuery(
        name = "filter_audit_log",
        procedureName = "filter_audit_log",
        resultClasses = {AuditLog.class},
        parameters = {
                @StoredProcedureParameter(mode =
                        ParameterMode.IN, name = "p_entity_name", type =
                        String.class),
                @StoredProcedureParameter(mode =
                        ParameterMode.IN, name = "p_entity_id", type =
                        String.class),
                @StoredProcedureParameter(mode =
                        ParameterMode.IN, name = "p_action", type =
                        String.class),
                @StoredProcedureParameter(mode =
                ParameterMode.IN, name = "p_changed_by", type =
                String.class),
                @StoredProcedureParameter(mode =
                        ParameterMode.IN, name = "p_changed_at", type =
                        LocalDateTime.class),
                @StoredProcedureParameter(mode =
                        ParameterMode.IN, name = "p_page_number", type =
                        Integer.class),
                     @StoredProcedureParameter(mode =
                ParameterMode.IN, name = "p_page_size", type =
                Integer.class)
        }
)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name")
    private String entityName;

    @Column(name = "entity_id")
    private String entityId;

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
    @Column(name = "field_changes", columnDefinition = "TEXT")
    private String fieldChanges;

    @Lob
    @Column(name = "raw_data_before", columnDefinition = "TEXT")
    private String rawDataBefore;

    @Lob
    @Column(name = "raw_data_after", columnDefinition = "TEXT")
    private String rawDataAfter;

}
