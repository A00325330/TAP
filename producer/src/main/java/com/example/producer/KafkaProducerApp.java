package com.example.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Random;

@SpringBootApplication
@RestController
@EnableScheduling
public class KafkaProducerApp {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Random random = new Random();
    private final AppConfig appConfig;

    public KafkaProducerApp(KafkaTemplate<String, String> kafkaTemplate, AppConfig appConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.appConfig = appConfig;
    }

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerApp.class, args);
    }

    @PostMapping("/send")
    public String send(@RequestParam String msg) {
        kafkaTemplate.send("test", msg);
        return "Sent: " + msg;
    }

    // Scheduled method to send every 20 seconds
    @Scheduled(fixedRate = 20000)
    public void sendRandomMessage() {
        if (!appConfig.isStreamingEnabled()) {
            System.out.println("Streaming disabled, skipping scheduled send.");
            return;
        }

        int nodeId = appConfig.getNodeId();
        int networkId = appConfig.getNetworkId();
        String networkName = appConfig.getNetworkName();

        int numCalls = random.nextInt(60) + 1; // 1 to 60

        String msg = String.format(
            "{\"node_id\": %d, \"network_id\": %d, \"network_name\": \"%s\", \"num_calls\": %d}",
            nodeId, networkId, networkName, numCalls
        );

        kafkaTemplate.send("test", msg);
        System.out.println("Auto sent: " + msg);
    }
}

