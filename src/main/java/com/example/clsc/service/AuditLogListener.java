package com.example.clsc.service;

import com.example.clsc.constants.ConfigConstants;
import com.example.clsc.dto.AuditLogDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class AuditLogListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogListener.class);
    private final AuditLogProcessor auditLogProcessor;

    public AuditLogListener(AuditLogProcessor auditLogProcessor) {
        this.auditLogProcessor = auditLogProcessor;
    }

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditLogDto message) {
        logger.info("Received message from RabbitMQ with requestId: {}", message.getRequestId());
        auditLogProcessor.processAuditEvent(message);
    }
}
