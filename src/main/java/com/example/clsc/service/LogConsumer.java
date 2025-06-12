package com.example.clsc.service;

import com.example.clsc.constants.ConfigConstants;
import com.example.clsc.dto.AuditEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(AuditEvent message) {
        System.out.println("Received Log: " + message.toString());
    }
}
