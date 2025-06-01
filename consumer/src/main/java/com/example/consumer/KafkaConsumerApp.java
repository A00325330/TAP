package com.example.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class KafkaConsumerApp {
    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumerApp.class, args);
    }
}

@Service
class KafkaService {
    @KafkaListener(topics = "test", groupId = "my-group")
    public void listen(String message) {
        System.out.println("Received: " + message);
    }
}
