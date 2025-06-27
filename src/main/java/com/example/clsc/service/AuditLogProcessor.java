
package com.example.clsc.service;

import com.example.clsc.dto.AuditEvent;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.clsc.utils.DateTimeUtils.*;

@Service
public class AuditLogProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogProcessor.class);
    private final AuditLogRepo auditLogRepo;
    private final ObjectMapper objectMapper;

    public AuditLogProcessor(AuditLogRepo auditLogRepo, ObjectMapper objectMapper) {
        this.auditLogRepo = auditLogRepo;
        this.objectMapper = objectMapper;
    }

    public void processAuditEvent(AuditEvent message) {
        try {
            AuditLog auditLog = buildBaseLog(message);

            Optional<AuditLog> previousLogOpt = auditLogRepo.findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
                    message.getEntityName(), message.getEntityId());

            if (previousLogOpt.isPresent()) {
                AuditLog previousLog = previousLogOpt.get();
                handleUpdateScenario(auditLog, message, previousLog);
            } else {
                auditLog.setRawDataBefore(null);
            }

            auditLogRepo.save(auditLog);

        } catch (Exception e) {
            logger.error("Error processing audit log for requestId {}: {}", message.getRequestId(), e.getMessage(), e);
            auditLogRepo.save(buildErrorLog(message, e));
        }
    }

    private void handleUpdateScenario(AuditLog auditLog, AuditEvent message, AuditLog previousLog) throws Exception {
        String previousRawAfter = previousLog.getRawDataAfter();
        String currentRawBefore = message.getRawDataBefore() != null
                ? objectMapper.writeValueAsString(message.getRawDataBefore())
                : null;

        LocalDateTime prevUpdatedAt = extractTimeStampFromJson(previousRawAfter, "updatedAt");
        LocalDateTime prevCreatedAt = extractTimeStampFromJson(previousRawAfter, "createdAt");
        LocalDateTime currUpdatedAt = extractTimeStampFromJson(currentRawBefore, "updatedAt");
        LocalDateTime currCreatedAt = extractTimeStampFromJson(currentRawBefore, "createdAt");

        if (areTimestampsEqual(currCreatedAt, prevCreatedAt)) {
            if ((currUpdatedAt == null && prevUpdatedAt == null) || compareTimestamps(prevUpdatedAt, currUpdatedAt) >= 0) {
                auditLog.setRawDataBefore(previousRawAfter);
                auditLog.setFieldChanges(generateFieldChanges(previousRawAfter, message.getRawDataAfter()));
            } else {
                AuditLog missingLog = createMissingAuditLog(message, previousLog);
                auditLogRepo.save(missingLog);
                auditLog.setRawDataBefore(missingLog.getRawDataAfter());
                auditLog.setFieldChanges(generateFieldChanges(missingLog.getRawDataAfter(), message.getRawDataAfter()));
            }
        } else {
            logger.warn("Inconsistent createdAt timestamps: prevCreatedAt={}, currCreatedAt={}", prevCreatedAt, currCreatedAt);
        }
    }

    private AuditLog buildBaseLog(AuditEvent message) throws Exception {
        AuditLog log = buildBaseFields(message);
        String rawBefore = objectMapper.writeValueAsString(message.getRawDataBefore());
        String rawAfter = objectMapper.writeValueAsString(message.getRawDataAfter());

        log.setRawDataBefore(rawBefore);
        log.setRawDataAfter(rawAfter);

        if (extractTimeStampFromJson(rawBefore, "updatedAt") == null) {
            log.setFieldChanges(generateFieldChanges(null, message.getRawDataAfter()));
        }

        return log;
    }

    private AuditLog buildErrorLog(AuditEvent message, Exception e) {
        AuditLog errorLog = buildBaseFields(message);
        String error = "ERROR_PROCESSING_DATA: " + e.getMessage();
        errorLog.setRawDataBefore(error);
        errorLog.setRawDataAfter(error);
        return errorLog;
    }

    private AuditLog buildBaseFields(AuditEvent message) {
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
        missing.setFieldChanges(generateFieldChanges(previousLog.getRawDataAfter(), message.getRawDataBefore()));
        return missing;
    }

}
