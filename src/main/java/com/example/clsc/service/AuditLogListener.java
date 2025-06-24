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
import org.springframework.web.servlet.View;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.clsc.utils.CalculateJsonDiffs.findDiffs;

@Service
@SuppressWarnings("unchecked")
public class AuditLogListener {

    private final AuditLogRepo auditLogRepo;
    private final AuditLogRepo missingLogRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final View error;

    @Autowired
    public AuditLogListener(AuditLogRepo auditLogRepo, AuditLogRepo missingLogRepo, View error) {
        this.auditLogRepo = auditLogRepo;
        this.missingLogRepo = missingLogRepo;
        this.error = error;
    }

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditEvent message) {
        try {
            System.out.println("Received message: " + message);

            AuditLog auditLog = buildBaseLog(message);

            /*
            finding previous log for data validation ;
             */
            Optional<AuditLog> previousLogOpt = auditLogRepo.findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
                    message.getEntityName(), message.getEntityId());

            if (previousLogOpt.isPresent()) {
                AuditLog previousLog = previousLogOpt.get();


                // conversion of logs in specific types:
                String previousLogRawDataAfter = previousLog.getRawDataAfter();
                String currentLogRawDataBefore = message.getRawDataBefore() != null
                        ? objectMapper.writeValueAsString(message.getRawDataBefore())
                        : null;
                // extraction of timestamps
                Object currentDataBefore = message.getRawDataBefore();
                LocalDateTime prevUpdatedAt = extractUpdatedAt(objectMapper.readValue(previousLogRawDataAfter, new TypeReference<>() {}));
                LocalDateTime currUpdatedAt = extractUpdatedAt(message.getRawDataBefore());
                System.out.println("prevUpdatedAt: " + prevUpdatedAt);
                System.out.println("currUpdatedAt: " + currUpdatedAt);

                if (prevUpdatedAt != null && currUpdatedAt != null) {
                    // Normal general diff case

                    if (prevUpdatedAt.isEqual(currUpdatedAt) || prevUpdatedAt.isAfter(currUpdatedAt)) {
                        auditLog.setRawDataBefore(currentLogRawDataBefore);
                        auditLog.setFieldChanges(generateFieldChanges( previousLogRawDataAfter, (Map<String, Object>) message.getRawDataAfter()));
                    }

                    else if(prevUpdatedAt.isBefore(currUpdatedAt) && currentDataBefore != null) {
                        // Missing log detected
                        AuditLog missingLog = createMissingAuditLog(message, previousLog);
                        missingLogRepo.save(missingLog);
                        // since the last log got changed then rawdata before also got changed to the following thing

                        auditLog.setRawDataBefore(missingLog.getRawDataAfter());
                        auditLog.setFieldChanges(generateFieldChanges(missingLog.getRawDataAfter(), (Map<String, Object>) message.getRawDataAfter()));
                    }
                }

            }
            auditLogRepo.save(auditLog);
        }

        catch (Exception e) {
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


    // Class for creating a log
    private AuditLog buildBaseLog(AuditEvent message) throws Exception {
        AuditLog log = new AuditLog();
        log.setEntityName(message.getEntityName());
        log.setEntityId(message.getEntityId());
        log.setAction(ActionType.valueOf(message.getAction()));
        log.setChangedAt(message.getTimestamp());
        log.setChangedBy(message.getChangedBy());
        log.setRequestId(message.getRequestId());
        log.setRawDataAfter(objectMapper.writeValueAsString(message.getRawDataAfter()));
        log.setRawDataBefore(objectMapper.writeValueAsString(message.getRawDataBefore()));
        return log;
    }

    private String generateFieldChanges(String oldJson, Map<String, Object> newData) throws Exception {
        Map<String, Object> oldData = objectMapper.readValue(oldJson, new TypeReference<>() {});
        Map<String, Object[]> diffs = findDiffs(newData, oldData);
        return objectMapper.writeValueAsString(diffs);
    }

    /**
     * Extracts the "updatedAt" value from a JSON-like Object (Map) and returns it as LocalDateTime.
     *
     * @param jsonData Object that should be a Map<String, Object>
     * @return LocalDateTime if "updatedAt" exists and is parsable, otherwise null
     */
    public static LocalDateTime extractUpdatedAt(Object jsonData) {
        if (jsonData instanceof Map) {
            Map<?, ?> dataMap = (Map<?, ?>) jsonData;
            Object updatedAtValue = dataMap.get("updatedAt");

            if (updatedAtValue instanceof String) {
                try {
                    return LocalDateTime.parse((String) updatedAtValue);
                } catch (DateTimeParseException e) {
                    System.err.println("Failed to parse updatedAt: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private LocalDateTime parseDate(Object value) {
        if (value instanceof String str) {
            return LocalDateTime.parse(str);
        }
        return null;
    }


    private AuditLog  createMissingAuditLog(AuditEvent message, AuditLog previousLog) throws Exception {
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


