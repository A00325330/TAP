package com.example.containermanager;

import org.springframework.web.bind.annotation.*;
import java.io.*;

@RestController
public class BaseStationController {

    @PostMapping("/create-base-station")
    public String createBaseStation(@RequestBody BaseStationRequest request) {
        try {
            // Extract parameters from the request body
            String nodeId = request.getNodeId();
            String networkId = request.getNetworkId();
            String networkName = request.getNetworkName();
            String streamingEnabled = Boolean.toString(request.isStreamingEnabled());

            // Placeholder values for other environment variables
            String kafkaBroker = "kafka.broker.url";
            String mysqlHost = "mysql.host";
            String mysqlPort = "3306";
            String mysqlDb = "mydb";
            String mysqlUser = "root";
            String mysqlPassword = "password";
            String baseStationImage = "nginx";

            // Prepare the Docker command
            String command = String.format(
                    "docker run -d --name base-station-%s --network tap_network -p 8080:8080 " +
                            "-e KAFKA_BROKER=%s " +
                            "-e SPRING_DATASOURCE_URL=jdbc:mysql://%s:%s/%s?useSSL=false " +
                            "-e SPRING_DATASOURCE_USERNAME=%s " +
                            "-e SPRING_DATASOURCE_PASSWORD=%s " +
                            "-e NODE_ID=%s " +
                            "-e NETWORK_ID=%s " +
                            "-e NETWORK_NAME=%s " +
                            "-e STREAMING_ENABLED=%s " +
                            "%s",
                    nodeId, kafkaBroker, mysqlHost, mysqlPort, mysqlDb, mysqlUser, mysqlPassword,
                    nodeId, networkId, networkName, streamingEnabled, baseStationImage
            );

            // Ensure Docker is in the PATH, but handling spaces in the path for Windows
            String dockerPath = "C:\\Program Files\\Docker\\Docker\\Resources\\bin\\docker.exe";

            // Construct full command for Windows, accounting for spaces
            String fullCommand = String.format(
                    "\"%s\" run -d --name base-station-%s --network tap_network -p 8080:8080 " +
                            "-e KAFKA_BROKER=%s " +
                            "-e SPRING_DATASOURCE_URL=jdbc:mysql://%s:%s/%s?useSSL=false " +
                            "-e SPRING_DATASOURCE_USERNAME=%s " +
                            "-e SPRING_DATASOURCE_PASSWORD=%s " +
                            "-e NODE_ID=%s " +
                            "-e NETWORK_ID=%s " +
                            "-e NETWORK_NAME=%s " +
                            "-e STREAMING_ENABLED=%s " +
                            "%s",
                    dockerPath, nodeId, kafkaBroker, mysqlHost, mysqlPort, mysqlDb, mysqlUser, mysqlPassword,
                    nodeId, networkId, networkName, streamingEnabled, baseStationImage
            );

            // Execute command via Runtime
            Process process = Runtime.getRuntime().exec(fullCommand);

            // Wait for the process to complete and check the exit code
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return "Base Station " + nodeId + " created and started successfully!";
            } else {
                return "Error: Docker container failed to start.";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "Error creating Base Station: " + e.getMessage();
        }
    }
}
