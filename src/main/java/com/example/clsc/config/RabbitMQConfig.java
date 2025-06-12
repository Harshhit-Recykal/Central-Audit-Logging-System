package com.example.clsc.config;

import com.example.clsc.constants.ConfigConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.amqp.core.Queue; // ✅ Correct

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue queue() {
        return new Queue(ConfigConstants.QUEUE_NAME );
    }
}
