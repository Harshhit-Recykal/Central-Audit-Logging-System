package com.example.clsc.service;

import com.example.clsc.constants.ConfigConstants;
import com.example.clsc.dto.AuditEvent;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.repository.AuditLogRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.clsc.utils.CalculateJsonDiffs.findDiffs;

@Service
@SuppressWarnings("unchecked")
public class AuditLogListener {

    private final AuditLogRepo auditLogRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AuditLogListener(AuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditEvent message) {
        try {
            System.out.println("Received message: " + message);

            AuditLog auditLog = buildBaseLog(message);

            Optional<AuditLog> previousLogOpt = auditLogRepo.findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
                    message.getEntityName(), message.getEntityId());

            if (previousLogOpt.isPresent()) {
                AuditLog previousLog = previousLogOpt.get();

                String previousRawAfter = previousLog.getRawDataAfter();
                String currentRawBefore = message.getRawDataBefore() != null
                        ? objectMapper.writeValueAsString(message.getRawDataBefore())
                        : null;

                LocalDateTime prevUpdatedAt = extractUpdatedAt(previousRawAfter);
                LocalDateTime currUpdatedAt = extractUpdatedAt(currentRawBefore);

                System.out.println("prevUpdatedAt: " + prevUpdatedAt);
                System.out.println("currUpdatedAt: " + currUpdatedAt);

                if (prevUpdatedAt != null && currUpdatedAt != null) {
                    if (prevUpdatedAt.isEqual(currUpdatedAt) || prevUpdatedAt.isAfter(currUpdatedAt)) {
                        // Normal update case
                        auditLog.setRawDataBefore(previousRawAfter);
                        auditLog.setFieldChanges(generateFieldChanges(previousRawAfter, (Map<String, Object>) message.getRawDataAfter()));
                    } else {
                        // Missing log detected
                        AuditLog missingLog = createMissingAuditLog(message, previousLog);
                        auditLogRepo.save(missingLog);

                        auditLog.setRawDataBefore(currentRawBefore);
                        auditLog.setFieldChanges(generateFieldChanges(previousRawAfter, (Map<String, Object>) message.getRawDataAfter()));
                    }
                } else {
                    // Fallback: still attempt to save rawDataBefore if available
                    if (currentRawBefore != null) {
                        auditLog.setRawDataBefore(currentRawBefore);
                        auditLog.setFieldChanges(generateFieldChanges(currentRawBefore, (Map<String, Object>) message.getRawDataAfter()));
                    }
                }
            }

            if (auditLog.getRawDataBefore() == null) {
                auditLog.setRawDataBefore("NO_PREVIOUS_LOG_OR_TIMESTAMP_MISSING");
            }

            auditLogRepo.save(auditLog);

        } catch (Exception e) {
            String error = "ERROR_PROCESSING_DATA: " + e.getMessage();
            AuditLog errorLog = new AuditLog();
            errorLog.setEntityName(message.getEntityName());
            errorLog.setEntityId(message.getEntityId());
            errorLog.setAction(ActionType.valueOf(message.getAction()));
            errorLog.setChangedAt(message.getTimestamp());
            errorLog.setChangedBy(message.getChangedBy());
            errorLog.setRequestId(message.getRequestId());
            errorLog.setRawDataBefore(error);
            errorLog.setRawDataAfter(error);
            auditLogRepo.save(errorLog);
        }
    }

    private AuditLog buildBaseLog(AuditEvent message) throws Exception {
        AuditLog log = new AuditLog();
        log.setEntityName(message.getEntityName());
        log.setEntityId(message.getEntityId());
        log.setAction(ActionType.valueOf(message.getAction()));
        log.setChangedAt(message.getTimestamp());
        log.setChangedBy(message.getChangedBy());
        log.setRequestId(message.getRequestId());
        log.setRawDataAfter(objectMapper.writeValueAsString(message.getRawDataAfter()));
        return log;
    }

    private String generateFieldChanges(String oldJson, Map<String, Object> newData) throws Exception {
        Map<String, Object> oldData = objectMapper.readValue(oldJson, new TypeReference<>() {});
        Map<String, Object[]> diffs = findDiffs(newData, oldData);
        return objectMapper.writeValueAsString(diffs);
    }

    private LocalDateTime extractUpdatedAt(String json) {
        try {
            if (json == null) return null;
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object updatedAt = map.get("updatedAt");

            if (updatedAt instanceof LocalDateTime) {
                return (LocalDateTime) updatedAt;
            } else if (updatedAt instanceof String) {
                return LocalDateTime.parse((String) updatedAt);
            } else {
                return null;
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            System.err.println("Failed to parse updatedAt: " + e.getMessage());
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private AuditLog createMissingAuditLog(AuditEvent message, AuditLog previousLog) throws Exception {
        AuditLog missing = new AuditLog();
        missing.setEntityName(previousLog.getEntityName());
        missing.setEntityId(previousLog.getEntityId());
        missing.setAction(ActionType.UPDATE);
        missing.setChangedAt(previousLog.getChangedAt());
        missing.setChangedBy(previousLog.getChangedBy());
        missing.setRequestId(UUID.randomUUID().toString());
        missing.setRawDataBefore(previousLog.getRawDataAfter());

        String rawAfter = objectMapper.writeValueAsString(message.getRawDataBefore());
        missing.setRawDataAfter(rawAfter);
        missing.setFieldChanges(generateFieldChanges(previousLog.getRawDataAfter(), (Map<String, Object>) message.getRawDataBefore()));
        return missing;
    }
}
