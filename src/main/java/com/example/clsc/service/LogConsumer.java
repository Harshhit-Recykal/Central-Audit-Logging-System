package com.example.clsc.service;

import com.example.clsc.config.RabbitMQConfig;
import com.example.clsc.constants.ConfigConstants;
import com.example.clsc.dto.AuditEvent;
import com.example.clsc.entity.AuditLog;
import com.example.clsc.enums.ActionType;
import com.example.clsc.repository.AuditLogRepo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.log.LogMessage;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    @Autowired
    private AuditLogRepo auditLogRepository;

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditEvent message) {
        System.out.println("Received Log: " + message);
        // Convert DTO to entity
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(ActionType.valueOf(message.getAction()));
//        auditLog.setServiceName(message.getServiceName());
//        auditLog.setPerformedBy(message.getPerformedBy());
//        auditLog.setTimestamp(message.getTimestamp()); // assuming this is a LocalDateTime

        auditLogRepository.save(auditLog);
    }
}
