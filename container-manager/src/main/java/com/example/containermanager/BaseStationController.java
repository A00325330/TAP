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
import java.util.List;

@RestController
public class BaseStationController {

    private final DockerClient dockerClient;
    private final String baseStationImage;
    private final String dockerNetwork = "tap_network";  // No need to use value from config for now
    private final String kafkaBroker;
    private final String mysqlHost;
    private final String mysqlPort;
    private final String mysqlDb;
    private final String mysqlUser;
    private final String mysqlPassword;
    private final BaseStationRepository bsr;

    public BaseStationController(
            @Value("${base.station.image}") String baseStationImage,
            @Value("${kafka.broker}") String kafkaBroker,
            @Value("${mysql.host}") String mysqlHost,
            @Value("${mysql.port}") String mysqlPort,
            @Value("${mysql.db}") String mysqlDb,
            @Value("${mysql.user}") String mysqlUser,
            @Value("${mysql.password}") String mysqlPassword,
            BaseStationRepository bsr
    ) {
        this.baseStationImage = baseStationImage;
        this.kafkaBroker = kafkaBroker;
        this.mysqlHost = mysqlHost;
        this.mysqlPort = mysqlPort;
        this.mysqlDb = mysqlDb;
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;
        this.bsr = bsr;

        // Configure the Docker client to use the Unix socket
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")  // Use the Unix socket
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

        // Log the Docker host directly
        System.out.println("Docker client is configured with host: " + config.getDockerHost());
    }

    @PostMapping("/create-base-station")
    public String createBaseStation(@RequestBody BaseStationRequest request) {
        try {
            // Ensure the network exists
            ensureNetworkExists(dockerNetwork);

            ExposedPort tcp8080 = ExposedPort.tcp(8080);
            PortBinding portBinding = new PortBinding(null, tcp8080);

            CreateContainerResponse container = dockerClient.createContainerCmd(baseStationImage)
                    .withName("base-station-" + request.getNodeId())
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
                            "NODE_ID=" + request.getNodeId(),
                            "NETWORK_ID=" + request.getNetworkId(),
                            "NETWORK_NAME=" + request.getNetworkName(),
                            "STREAMING_ENABLED=" + request.isStreamingEnabled()
                    )
                    .exec();

            dockerClient.startContainerCmd(container.getId()).exec();

            // Verify network attachment
            boolean isInNetwork = isContainerInNetwork(container.getId(), dockerNetwork);
            if (!isInNetwork) {
                throw new RuntimeException("Container failed to join network: " + dockerNetwork);
            }

            BaseStation baseStation = new BaseStation();
            baseStation.setNodeId(request.getNodeId());
            baseStation.setNetworkId(request.getNetworkId());
            baseStation.setNetworkName(request.getNetworkName());
            baseStation.setStreamingEnabled(request.isStreamingEnabled());
            bsr.save(baseStation);

            return "Base Station " + request.getNodeId() + " created and started successfully on network " + dockerNetwork + "!";
        } catch (Exception e) {
            return "Error creating Base Station: " + e.getMessage();
        }
    }

    private void ensureNetworkExists(String networkName) {
        try {
            // List all networks and filter by name
            System.out.println("Checking if network exists: " + networkName);
            List<com.github.dockerjava.api.model.Network> networks = dockerClient.listNetworksCmd()
                    .withNameFilter(networkName)
                    .exec();

            // Log network list for debugging
            System.out.println("Networks found: " + networks.size());
            for (com.github.dockerjava.api.model.Network network : networks) {
                System.out.println("Network: " + network.getName() + " (ID: " + network.getId() + ")");
            }

            // If the network does not exist, create it
            if (networks.isEmpty()) {
                System.out.println("Network does not exist. Creating new network: " + networkName);
                dockerClient.createNetworkCmd()
                        .withName(networkName)
                        .withDriver("bridge")
                        .exec();
                System.out.println("Network created: " + networkName);
            } else {
                System.out.println("Network already exists: " + networkName);
            }
        } catch (Exception e) {
            System.err.println("Error during network existence check or creation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create/check network: " + networkName, e);
        }
    }

    private boolean isContainerInNetwork(String containerId, String networkName) {
        try {
            System.out.println("Inspecting network: " + networkName);

            // Get network details
            com.github.dockerjava.api.model.Network network = dockerClient.inspectNetworkCmd()
                    .withNetworkId(networkName)
                    .exec();

            // Log network containers for debugging
            System.out.println("Network " + networkName + " contains the following containers:");
            network.getContainers().forEach((key, value) -> {
                System.out.println("  - Container ID: " + key + " (Name: " + value.getName() + ")");
            });

            // Check if the specified container is in the network
            boolean isInNetwork = network.getContainers().containsKey(containerId);
            if (isInNetwork) {
                System.out.println("Container " + containerId + " is attached to network " + networkName);
            } else {
                System.out.println("Container " + containerId + " is NOT attached to network " + networkName);
            }

            return isInNetwork;
        } catch (Exception e) {
            System.err.println("Error inspecting network " + networkName + " for container " + containerId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
