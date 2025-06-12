package com.example.clsc;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class ClscApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClscApplication.class, args);
	}
}
