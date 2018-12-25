package com.github.lkq.instadocker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.lkq.instadocker.util.Assert;
import com.github.lkq.instadocker.docker.entity.PortBinding;
import com.github.lkq.instadocker.docker.entity.VolumeBinding;
import com.github.lkq.instadocker.util.InstaUtils;
import org.slf4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class DockerContainer {

    private static final Logger logger = getLogger(DockerContainer.class);

    private final DockerClient dockerClient;

    private final String imageId;
    private final String containerName;

    private String hostName;
    private String network;

    private final List<VolumeBinding> volumeBindings = new ArrayList<>();
    private final List<PortBinding> portBindings = new ArrayList<>();
    private final List<String> environmentVariables = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();

    private final ContainerLogger containerLogger;

    /**
     * the docker container id, available after start
     */
    private String containerId;

    /**
     * class for manipulating docker containers.
     *
     * @param dockerClient  the docker-java api client
     * @param imageId       the docker image id
     * @param containerName the docker container name to use
     * @param logger        the logger for outputting docker console logs, will use the DockerContainer.logger if not provided
     */
    public DockerContainer(DockerClient dockerClient, String imageId, String containerName, Logger logger) {
        Objects.requireNonNull(dockerClient);
        Assert.requiresNotBlank(imageId, "imageId is required");
        Assert.requiresNotBlank(containerName, "containerName is required");
        this.dockerClient = dockerClient;
        this.imageId = imageId;
        this.containerName = containerName;
        this.containerLogger = new ContainerLogger(containerName, logger == null ? DockerContainer.logger : logger);
    }

    public DockerContainer hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public DockerContainer network(String network) {
        this.network = network;
        return this;
    }

    public DockerContainer volumeBindings(List<VolumeBinding> volumeBindings) {
        this.volumeBindings.addAll(volumeBindings);
        return this;
    }

    public DockerContainer volumeBindings(String... containerAndHostPaths) {
        Assert.requiresEvenNumber(containerAndHostPaths.length, "container and host path pair doesn't match");
        for (int i = 0; i < containerAndHostPaths.length / 2; i++) {
            volumeBinding(containerAndHostPaths[i], containerAndHostPaths[i + 1]);
        }
        return this;
    }

    public DockerContainer volumeBinding(String containerPath, String hostPath) {
        volumeBindings.add(new VolumeBinding(containerPath, hostPath));
        return this;
    }

    public DockerContainer portBinding(String protocol, int containerPort, int hostPort) {
        portBindings.add(new PortBinding(protocol, containerPort, hostPort));
        return this;
    }

    public DockerContainer portBindings(List<PortBinding> portBindings) {
        this.portBindings.addAll(portBindings);
        return this;
    }

    public DockerContainer environmentVariables(List<String> environmentVariables) {
        this.environmentVariables.addAll(environmentVariables);
        return this;
    }

    public DockerContainer commands(List<String> commands) {
        this.commands.addAll(commands);
        return this;
    }

    public boolean ensureRunning() {
        if (isRunning()) {
            logger.debug("container already running, containerName={}", containerName);
            return true;
        }
        if (exists()) {
            logger.debug("trying to start container, containerName={}", containerName);
            dockerClient.startContainerCmd(containerName).exec();
            if (isRunning()) {
                this.containerLogger.attach(dockerClient);
                logger.info("container started, containerName={}", containerName);
                return true;
            } else {
                logger.debug("container fails to start, containerName={}", containerName);
                return false;
            }
        } else {
            logger.error("fail to start container, container does not exist, please create the container first, containerName={}", containerName);
            return false;
        }
    }

    public boolean isRunning() {
        InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerName).exec();
        Boolean state = inspectResponse.getState().getRunning();
        return state == null ? false : state;
    }

    public boolean ensureStopped(int timeoutInSeconds) {
        dockerClient.stopContainerCmd(containerId).withTimeout(timeoutInSeconds).exec();
        if (isRunning()) {
            logger.info("unable to stop container, still running after stop, containerName={}", containerName);
            return false;
        } else {
            logger.info("container stopped, containerName={}", containerName);
            return true;
        }
    }

    /**
     * ensure the docker container exists
     * @return true if the container exists or being created
     */
    public boolean ensureExists() {
        if (!exists()) {
            return createOrReplace();
        }
        return exists();
    }

    /**
     * create the docker container
     *
     * @return true only if the container was actually created by this method invocation
     */
    public boolean createOrReplace() {
        logger.info("replacing container, imageId={}, containerName={}", imageId, containerName);
        if (exists()) {
            if (ensureNotExists()) {
                logger.info("removed old container, containerName={}", containerName);
            } else {
                logger.error("unable to replace container, container exists but failed to remove, containerName={}", containerName);
                return false;
            }
        }
        CreateContainerCmd cmd = dockerClient.createContainerCmd(imageId);
        cmd.withName(containerName);

        if (hostName != null) {
            cmd.withHostName(hostName);
        }
        if (network != null) {
            cmd.withNetworkMode(network);
        }

        List<Bind> binds = volumeBindings.stream().map(volumeBinding -> {
            String containerPath = volumeBinding.containerPath();
            String hostPath = volumeBinding.hostPath();
            logger.info("binding volume: container={}, host={}", containerPath, hostPath);
            return new Bind(hostPath, new Volume(containerPath));
        }).collect(Collectors.toList());
        cmd.withBinds(binds);

        if (portBindings.size() > 0) {
            List<ExposedPort> exposedPorts = new ArrayList<>();
            Ports ports = new Ports();
            for (PortBinding portBinding : portBindings) {
                logger.info("binding port: protocol={}, container={}, host={}",
                        portBinding.protocol(), portBinding.containerPort(), portBinding.hostPort());
                ExposedPort exposedPort = portBinding.toExposedPort();
                exposedPorts.add(exposedPort);

                ports.bind(exposedPort, Ports.Binding.bindPort(portBinding.hostPort()));
            }
            cmd.withExposedPorts(exposedPorts).withPortBindings(ports);
        }
        if (environmentVariables.size() > 0) {
            cmd.withEnv(environmentVariables);
        }
        if (commands.size() > 0) {
            cmd.withCmd(commands);
        }

        CreateContainerResponse createResponse = cmd.exec();
        containerId = createResponse.getId();
        if (exists()) {
            logger.info("container created, containerName={}, containerId={}", containerName, containerId);
            return true;
        } else {
            logger.info("container not created, not exists after created, containerName={}", containerName);
            return false;
        }
    }

    /**
     * check if the container already exists in local
     *
     * @return true if the container exists, false if the container does not exists
     */
    public boolean exists() {
        try {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerName).exec();
            logger.debug("check container existence: inspect result={}", inspectResponse);
            return InstaUtils.isNotBlank(inspectResponse.getId());
        } catch (NotFoundException e) {
            logger.debug("check container existence: container not found, containerName=" + containerName, e);
            return false;
        }
    }

    /**
     * ensure the container does not exists, if it's already exist, remove it
     *
     * @return true if the container does not exist or removed successfully
     */
    public boolean ensureNotExists() {
        if (exists()) {
            dockerClient.removeContainerCmd(containerName).withForce(true).exec();
            if (exists()) {
                logger.warn("failed to remove container, container still exists after remove, containerName={}", containerName);
                return false;
            } else {
                logger.info("container removed, containerName={}", containerName);
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "\"dockerClient\":" + dockerClient +
                ", \"imageId\":\"" + imageId + "\"" +
                ", \"containerName\":\"" + containerName + "\"" +
                ", \"hostName\":\"" + hostName + "\"" +
                ", \"network\":\"" + network + "\"" +
                ", \"volumeBindings\":" + volumeBindings +
                ", \"portBindings\":" + portBindings +
                ", \"environmentVariables\":" + environmentVariables +
                ", \"commands\":" + commands +
                ", \"containerLogger\":" + containerLogger +
                ", \"containerId\":\"" + containerId + "\"" +
                '}';
    }
}
