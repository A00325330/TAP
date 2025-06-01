package com.example.containermanager;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
public class BaseStationController {

    private final DockerClient dockerClient;

    private final String baseStationImage;
    private final String dockerNetwork;
    private final String kafkaBroker;
    private final String mysqlHost;
    private final String mysqlPort;
    private final String mysqlDb;
    private final String mysqlUser;
    private final String mysqlPassword;

    // Constructor injection ensures these values are set before dockerClient is created
    public BaseStationController(
        @Value("${base.station.image}") String baseStationImage,
        @Value("${docker.network}") String dockerNetwork,
        @Value("${kafka.broker}") String kafkaBroker,
        @Value("${mysql.host}") String mysqlHost,
        @Value("${mysql.port}") String mysqlPort,
        @Value("${mysql.db}") String mysqlDb,
        @Value("${mysql.user}") String mysqlUser,
        @Value("${mysql.password}") String mysqlPassword
    ) {
        this.baseStationImage = baseStationImage;
        this.dockerNetwork = dockerNetwork;
        this.kafkaBroker = kafkaBroker;
        this.mysqlHost = mysqlHost;
        this.mysqlPort = mysqlPort;
        this.mysqlDb = mysqlDb;
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        this.dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerHttpClient(httpClient)
                .build();
    }

@PostMapping("/create-base-station")
public String createBaseStation(@RequestBody BaseStationRequest request) {
    try {
        ExposedPort tcp8080 = ExposedPort.tcp(8080);
        PortBinding portBinding = new PortBinding(null, tcp8080);

        CreateContainerResponse container = dockerClient.createContainerCmd(baseStationImage)
                .withName("base-station-" + request.getNodeId()) // or combine with networkId if you want unique names
                .withExposedPorts(tcp8080)
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withPortBindings(portBinding)
                                .withNetworkMode(dockerNetwork)
                )
                .withEnv(
                        "KAFKA_BROKER=" + kafkaBroker,
                        "SPRING_DATASOURCE_URL=jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDb + "?useSSL=false",
                        "SPRING_DATASOURCE_USERNAME=" + mysqlUser,
                        "SPRING_DATASOURCE_PASSWORD=" + mysqlPassword,
                        // Pass the new variables as environment variables to the container:
                        "NODE_ID=" + request.getNodeId(),
                        "NETWORK_ID=" + request.getNetworkId(),
                        "NETWORK_NAME=" + request.getNetworkName(),
                        "STREAMING_ENABLED=" + request.isStreamingEnabled()
                )
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return "Base Station " + request.getNodeId() + " created and started successfully!";
    } catch (Exception e) {
        return "Error creating Base Station: " + e.getMessage();
    }
}
}
