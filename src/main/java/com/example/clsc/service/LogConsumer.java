package com.example.clsc.service;

import com.example.clsc.config.RabbitMQConfig;
import com.example.clsc.constants.ConfigConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.core.log.LogMessage;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

    @RabbitListener(queues = ConfigConstants.QUEUE_NAME)
    public void receiveLog(LogMessage message) {
        System.out.println("Received Log: " + message);
    }
}
