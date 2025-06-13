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

import java.util.concurrent.CountDownLatch;

@Service
public class LogConsumer {

     @Autowired
     private final AuditLogRepo auditLogRepo;

    public LogConsumer(AuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditEvent message) {
        System.out.println("Received Log: " + message);
        AuditLog auditLog = new AuditLog();
        auditLog.setAction(ActionType.valueOf(message.getAction()));
        auditLog.setRequestId(message.getEntityId());
        auditLog.setChangedAt(message.getTimestamp());
        auditLog.setRawDataAfter(message.getRawDataAfter().toString());
        auditLog.setRawDataBefore(message.getRawDataBefore().toString());

        auditLogRepo.save(auditLog);
    }
}
