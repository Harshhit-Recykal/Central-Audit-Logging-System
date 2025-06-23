//
//package com.example.clsc.service;
//
//import com.example.clsc.constants.ConfigConstants;
//import com.example.clsc.dto.AuditEvent;
//import com.example.clsc.entity.AuditLog;
//import com.example.clsc.enums.ActionType;
//import com.example.clsc.repository.AuditLogRepo;
//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import static com.example.clsc.utils.CalculateJsonDiffs.findDiffs;
//
//@Service
//@SuppressWarnings("unchecked")
//public class AuditLogListener {
//
//    private final AuditLogRepo auditLogRepo;
//
//    private final ObjectMapper objectMapper;
//
//    @Autowired
//    public AuditLogListener(AuditLogRepo auditLogRepo) {
//        this.auditLogRepo = auditLogRepo;
//        this.objectMapper = new ObjectMapper();
//    }
//
//    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
//    public void receiveLog(AuditEvent message) {
//        System.out.println("Received Log: " + message);
//
//        AuditLog auditLog = new AuditLog();
//        auditLog.setEntityName(message.getEntityName());
//        auditLog.setEntityId(message.getEntityId());
//        auditLog.setAction(ActionType.valueOf(message.getAction()));
//        auditLog.setChangedAt(message.getTimestamp());
//        auditLog.setChangedBy(message.getChangedBy());
//        auditLog.setRequestId(message.getRequestId());
//        //  auditLog.setRawDataAfter(message.getRawDataAfter().toString());
//
//        try {
//            String rawAfterJson = message.getRawDataAfter() != null
//                    ? objectMapper.writeValueAsString(message.getRawDataAfter())
//                    : null;
//            auditLog.setRawDataAfter(rawAfterJson);
//
//            // Load previous version and calculate diffs
//            if (rawAfterJson != null) {
//                Optional<AuditLog> previousLogOpt = auditLogRepo
//                        .findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
//                                message.getEntityName(), message.getEntityId()
//                        );
//
//                if (previousLogOpt.isPresent()) {
//                    AuditLog previousLog = previousLogOpt.get();
//                    auditLog.setRawDataBefore(previousLog.getRawDataAfter());
//                    Map<String, Object> currentData = (Map<String, Object>) message.getRawDataAfter();
//                    Map<String, Object> previousData = objectMapper.readValue(
//                            previousLog.getRawDataAfter(), new TypeReference<>() {
//                            }
//                    );
//                    Map<String, Object[]> fieldChanges = findDiffs(currentData, previousData);
//
//                    auditLog.setFieldChanges(objectMapper.writeValueAsString(fieldChanges));
//                }
//            }
//        } catch (Exception e) {
//            auditLog.setRawDataBefore("ERROR_PROCESSING_DATA: " + e.getMessage());
//            auditLog.setRawDataAfter("ERROR_PROCESSING_DATA: " + e.getMessage());
//        }
//        auditLogRepo.save(auditLog);
//    }
//}

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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import static com.example.clsc.utils.CalculateJsonDiffs.findDiffs;

@Service
@SuppressWarnings("unchecked")
public class AuditLogListener {
    private final AuditLogRepo auditLogRepo;
    private final ObjectMapper objectMapper;
    @Autowired
    public AuditLogListener(AuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
        this.objectMapper = new ObjectMapper();
    }
    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditEvent message) {
        System.out.println("Received Log: " + message);
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(message.getEntityName());
        auditLog.setEntityId(message.getEntityId());
        auditLog.setAction(ActionType.valueOf(message.getAction()));
        auditLog.setChangedAt(message.getTimestamp());
        auditLog.setChangedBy(message.getChangedBy());
        auditLog.setRequestId(message.getRequestId());

        try {
            String rawAfterJson = objectMapper.writeValueAsString(message.getRawDataAfter());
            auditLog.setRawDataAfter(rawAfterJson);

            Optional<AuditLog> previousLogOpt = auditLogRepo
                    .findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
                            message.getEntityName(), message.getEntityId());

            if (previousLogOpt.isPresent()) {
                AuditLog previousLog = previousLogOpt.get();
                auditLog.setRawDataBefore(previousLog.getRawDataAfter());

                LocalDateTime previousUpdatedAtAfter = extractUpdatedAt(previousLog.getRawDataAfter());
                LocalDateTime currentUpdatedAtBefore = extractUpdatedAt(objectMapper.writeValueAsString(message.getRawDataBefore()));

                if (previousUpdatedAtAfter!= null && currentUpdatedAtBefore != null) {
                    if (previousUpdatedAtAfter == currentUpdatedAtBefore) {
//                 Normal diff case
                auditLog.setRawDataBefore(previousLog.getRawDataAfter());

                Map<String, Object> currentData = (Map<String, Object>) message.getRawDataAfter();
                Map<String, Object> previousData = objectMapper.readValue(
                        previousLog.getRawDataAfter(), new TypeReference<>() {
                        });
                Map<String, Object[]> fieldChanges = findDiffs(currentData, previousData);

                auditLog.setFieldChanges(objectMapper.writeValueAsString(fieldChanges));

                    }
            else {
                        // in every case misslog is there
                        AuditLog missingLog = createMissingAuditLog(message, previousLog);
                        auditLogRepo.save(missingLog);
                    }
                }
            }

        } catch (Exception e) {
            String error = "ERROR_PROCESSING_DATA: " + e.getMessage();
            auditLog.setRawDataBefore(error);
            auditLog.setRawDataAfter(error);
        }

        auditLogRepo.save(auditLog);
    }

    private LocalDateTime extractUpdatedAt(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object updatedAt = map.get("updatedAt");
            return (updatedAt instanceof LocalDateTime) ? (LocalDateTime) updatedAt : null;
        } catch (Exception e) {
            return null;
        }
    }
    private AuditLog createMissingAuditLog(AuditEvent message, AuditLog previousLog) throws Exception {
        AuditLog missingLog = new AuditLog();
        missingLog.setEntityName(previousLog.getEntityName());
        missingLog.setEntityId(previousLog.getEntityId());
        missingLog.setAction(ActionType.UPDATE);
        missingLog.setChangedAt(previousLog.getChangedAt());
        missingLog.setChangedBy(previousLog.getChangedBy());
        missingLog.setRequestId(UUID.randomUUID().toString());
        missingLog.setRawDataBefore(previousLog.getRawDataAfter());
        String rawBeforeJson = objectMapper.writeValueAsString(message.getRawDataBefore());
        missingLog.setRawDataAfter(rawBeforeJson);
        Map<String, Object> currentData = (Map<String, Object>) message.getRawDataBefore();
        Map<String, Object> previousData = objectMapper.readValue(
                previousLog.getRawDataAfter(), new TypeReference<>() {});
        Map<String, Object[]> fieldChanges = findDiffs(currentData, previousData);
        missingLog.setFieldChanges(objectMapper.writeValueAsString(fieldChanges));
        return missingLog;
    }
}
