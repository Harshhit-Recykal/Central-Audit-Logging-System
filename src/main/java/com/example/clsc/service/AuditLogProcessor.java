package com.example.clsc.service;

import com.example.clsc.dto.AuditLogDto;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.repository.AuditLogRepo;
import com.example.clsc.utils.CalculateJsonDiffs;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuditLogProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogProcessor.class);
    private final AuditLogRepo auditLogRepo;
    private final ObjectMapper objectMapper;

    public AuditLogProcessor(AuditLogRepo auditLogRepo, ObjectMapper objectMapper) {
        this.auditLogRepo = auditLogRepo;
        this.objectMapper = objectMapper;
    }

    public void processAuditEvent(AuditLogDto message) {
        try {
            AuditLog auditLog = buildBaseLog(message);

            Optional<AuditLog> previousLogOpt = auditLogRepo.findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
                    message.getEntityName(), message.getEntityId());

            if (previousLogOpt.isPresent()) {
                AuditLog previousLog = previousLogOpt.get();
                handleUpdateScenario(auditLog, message, previousLog);
            } else {
                auditLog.setRawDataBefore("NO_PREVIOUS_LOG");
            }

            auditLogRepo.save(auditLog);

        } catch (Exception e) {
            logger.error("Error processing audit log for requestId {}: {}", message.getRequestId(), e.getMessage(), e);
            auditLogRepo.save(buildErrorLog(message, e));
        }
    }

    private void handleUpdateScenario(AuditLog auditLog, AuditLogDto message, AuditLog previousLog) throws Exception {
        String previousRawAfter = previousLog.getRawDataAfter();
        String currentRawBefore = message.getRawDataBefore() != null
                ? objectMapper.writeValueAsString(message.getRawDataBefore())
                : null;

        LocalDateTime prevUpdatedAt = extractUpdatedAt(previousRawAfter);
        LocalDateTime currUpdatedAt = extractUpdatedAt(currentRawBefore);

        int comparison = compareTimestamps(prevUpdatedAt, currUpdatedAt);

        if (comparison >= 0) {
            auditLog.setRawDataBefore(previousRawAfter);
            auditLog.setFieldChanges(generateFieldChanges(previousRawAfter, message.getRawDataAfter()));
        } else {
            AuditLog missingLog = createMissingAuditLog(message, previousLog);
            auditLogRepo.save(missingLog);
            auditLog.setRawDataBefore(missingLog.getRawDataAfter());
            auditLog.setFieldChanges(generateFieldChanges(missingLog.getRawDataAfter(), message.getRawDataAfter()));
        }
    }

    private AuditLog buildBaseLog(AuditLogDto message) throws Exception {
        AuditLog log = buildBaseFields(message);
        log.setRawDataAfter(objectMapper.writeValueAsString(message.getRawDataAfter()));
        return log;
    }

    private AuditLog buildErrorLog(AuditLogDto message, Exception e) {
        AuditLog errorLog = buildBaseFields(message);
        String error = "ERROR_PROCESSING_DATA: " + e.getMessage();
        errorLog.setRawDataBefore(error);
        errorLog.setRawDataAfter(error);
        return errorLog;
    }

    private AuditLog buildBaseFields(AuditLogDto message) {
        AuditLog log = new AuditLog();
        log.setEntityName(message.getEntityName());
        log.setEntityId(message.getEntityId());
        log.setAction(ActionType.valueOf(message.getAction()));
        log.setChangedAt(message.getTimestamp());
        log.setChangedBy(message.getChangedBy());
        log.setRequestId(message.getRequestId());
        return log;
    }

    private String generateFieldChanges(String oldJson, Object newData) throws Exception {
        if (oldJson == null || newData == null) return null;
        Map<String, Object> oldMap = objectMapper.readValue(oldJson, new TypeReference<>() {});
        Map<String, Object> newMap = objectMapper.convertValue(newData, new TypeReference<>() {});
        Map<String, Object[]> diffs = CalculateJsonDiffs.findDiffs(newMap, oldMap);
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
            }
        } catch (DateTimeParseException e) {
            logger.warn("Date parsing failed: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to extract updatedAt: {}", e.getMessage());
        }
        return null;
    }

    private AuditLog createMissingAuditLog(AuditLogDto message, AuditLog previousLog) throws Exception {
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
        missing.setFieldChanges(generateFieldChanges(previousLog.getRawDataAfter(), message.getRawDataBefore()));
        return missing;
    }

    private int compareTimestamps(LocalDateTime a, LocalDateTime b) {
        if (a == null || b == null) return -2;
        return a.compareTo(b);
    }
}
