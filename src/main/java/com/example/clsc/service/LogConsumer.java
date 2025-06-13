package com.example.clsc.service;
import com.example.clsc.constants.ConfigConstants;
import com.example.clsc.dto.AuditEvent;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.repository.AuditLogRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    private final AuditLogRepo auditLogRepo;

    private final ObjectMapper objectMapper;

    @Autowired
    public LogConsumer(AuditLogRepo auditLogRepo) {
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

        try {
            String rawJson = objectMapper.writeValueAsString(message.getRawDataAfter());
            auditLog.setRawDataAfter(rawJson);
        } catch (Exception e) {
            auditLog.setRawDataAfter("ERROR_CONVERTING_RAW_DATA");
        }

        auditLogRepo.save(auditLog);
    }
}
