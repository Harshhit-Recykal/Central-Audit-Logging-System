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
//import java.sql.Timestamp;
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
//        auditLog.setRawDataAfter(message.getRawDataAfter().toString());
//        auditLog.setRawDataBefore(message.getRawDataBefore().toString());
//
//        try {
//            String rawAfterJson = message.getRawDataAfter() != null
//                    ? objectMapper.writeValueAsString(message.getRawDataAfter())
//                    : null;
//            auditLog.setRawDataAfter(rawAfterJson);
//
//            if (rawAfterJson != null) {
//                Optional<AuditLog> previousLogOpt = auditLogRepo
//                        .findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
//                                message.getEntityName(), message.getEntityId()
//                        );
//            // Load previous version and calculate diffs
//
//            if (previousLogOpt.isPresent()) {
//                AuditLog previousLog = previousLogOpt.get();
//
//                Timestamp previous = previousLog.getRawDataAfter().get("createdAt");
//                Timestamp current =  objectMapper.writeValueAsTimestamp(message.getRawDataBefore().get("timestamp"));
//
//                if( previous == current  || previous.after(current)){
//                    auditLog.setRawDataBefore(previousLog.getRawDataAfter());
//                    Map<String ,Object> currentData = (Map<String ,Object>)message.getRawDataAfter();
//                    Map<String, Object> previousData = objectMapper.readValue(
//                            previousLog.getRawDataAfter(), new TypeReference<>() {}
//                    );
//                    Map<String, Object[]> fieldChanges =  findDiffs( currentData , previousData);
//                    auditLog.setFieldChanges(objectMapper.writeValueAsString(fieldChanges));
//                }
//                else{
//                    // logic for handling the missing log
//                    AuditEvent missingLog = new AuditEvent();
//                    missingLog.setEntityName(previousLog.getEntityName());
//                    missingLog.setEntityId(previousLog.getEntityId());
//                    missingLog.setAction("UPDATE");
//                    missingLog.setTimestamp(message.getRawDataBefore().get("updatedAt"));
//                    missingLog.setChangedBy(previousLog.getChangedBy());
//                    missingLog.setRequestId(previousLog.getRequestId()); /// we need to set it to random
//                    missingLog.setRawDataBefore(previousLog.getRawDataAfter());
//                    missingLog.setRawDataAfter(message.getRawDataBefore());
//                    Map<String ,Object> currentData = (Map<String ,Object>)message.getRawDataBefore();
//                    Map<String, Object> previousData = objectMapper.readValue(
//                            previousLog.getRawDataAfter(), new TypeReference<>() {}
//                    );
//                    Map<String, Object[]> fieldChanges =  findDiffs( currentData , previousData);
//                    auditLog.setFieldChanges(objectMapper.writeValueAsString(fieldChanges));
//                    auditLogRepo.save(missingLog);
//
//                    Map<String ,Object> currentVal = (Map<String ,Object>)message.getRawDataAfter();
//                    Map<String, Object> previousVal = (Map<String, Object>)message.getRawDataAfter();
//                    Map<String, Object[]> fieldChange =  findDiffs( currentVal , previousVal);
//                    auditLog.setFieldChanges(objectMapper.writeValueAsString(fieldChange));
//
//                }
//            }
//        }
//    }
//    catch (Exception e) {
//        auditLog.setRawDataBefore("ERROR_PROCESSING_DATA: " + e.getMessage());
//        auditLog.setRawDataAfter("ERROR_PROCESSING_DATA: " + e.getMessage());
//    }
//    auditLogRepo.save(auditLog);
//    }
//}
package com.example.clsc.service;

import com.example.clsc.constants.ConfigConstants;
import com.example.clsc.dto.AuditEvent;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.repository.AuditLogRepo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
        //  auditLog.setRawDataAfter(message.getRawDataAfter().toString());

        try {
            String rawAfterJson = message.getRawDataAfter() != null
                    ? objectMapper.writeValueAsString(message.getRawDataAfter())
                    : null;
            auditLog.setRawDataAfter(rawAfterJson);

            // Load previous version and calculate diffs
            if (rawAfterJson != null) {
                Optional<AuditLog> previousLogOpt = auditLogRepo
                        .findTopByEntityNameAndEntityIdOrderByChangedAtDesc(
                                message.getEntityName(), message.getEntityId()
                        );

                if (previousLogOpt.isPresent()) {
                    AuditLog previousLog = previousLogOpt.get();
                    auditLog.setRawDataBefore(previousLog.getRawDataAfter());
                    Map<String ,Object> currentData = (Map<String ,Object>)message.getRawDataAfter();
                    Map<String, Object> previousData = objectMapper.readValue(
                            previousLog.getRawDataAfter(), new TypeReference<>() {}
                    );
                    Map<String, Object[]> fieldChanges =  findDiffs( currentData , previousData);

                    auditLog.setFieldChanges(objectMapper.writeValueAsString(fieldChanges));
                }
            }
        }
        catch (Exception e) {
            auditLog.setRawDataBefore("ERROR_PROCESSING_DATA: " + e.getMessage());
            auditLog.setRawDataAfter("ERROR_PROCESSING_DATA: " + e.getMessage());
        }
        auditLogRepo.save(auditLog);
    }
}