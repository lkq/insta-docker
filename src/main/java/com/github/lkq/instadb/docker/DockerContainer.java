package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.lkq.instadb.Values;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class DockerContainer {

    private static final Logger logger = getLogger(DockerContainer.class);

    private final Map<String, String> volumeBindings = new TreeMap<>();
    private final List<PortBinding> portBindings = new ArrayList<>();
    private final List<String> environmentVariables = new ArrayList<>();

    private final DockerClient dockerClient;
    private final String imageId;
    private final String containerName;
    private final ContainerLogger containerLogger;
    private String containerId;
    private String networkMode;

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
        Values.requiresNotBlank(imageId, "imageId is required");
        Values.requiresNotBlank(containerName, "containerName is required");
        this.dockerClient = dockerClient;
        this.imageId = imageId;
        this.containerName = containerName;
        this.containerLogger = new ContainerLogger(logger == null ? DockerContainer.logger : logger, containerName);
    }

    public DockerContainer bindVolume(String containerPath, String hostPath) {
        volumeBindings.put(containerPath, hostPath);
        return this;
    }

    public DockerContainer bindPort(int containerPort, int hostPort, InternetProtocol protocol) {
        portBindings.add(new PortBinding(containerPort, hostPort, protocol));
        return this;
    }

    public DockerContainer environmentVariables(List<String> environmentVariables) {
        this.environmentVariables.addAll(environmentVariables);
        return this;
    }

    public DockerContainer network(String networkMode) {
        this.networkMode = networkMode;
        return this;
    }

    public boolean run() {
        if (isRunning()) {
            logger.debug("container already running, containerName={}", containerName);
            return true;
        }
        if (exists()) {
            logger.debug("trying to run container, containerName={}", containerName);
            dockerClient.startContainerCmd(containerName).exec();
            if (isRunning()) {
                this.containerLogger.attach(dockerClient);
                logger.info("container start running, containerName={}", containerName);
                return true;
            } else {
                logger.debug("container fail to run, containerName={}", containerName);
                return false;
            }
        } else {
            logger.error("fail to run container, container not exist, please create the container first, containerName={}", containerName);
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
     * create the docker container
     *
     * @return true only if the container was actually created by this method
     */
    public boolean createAndReplace() {
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

        if (volumeBindings.size() > 0) {
            List<Bind> binds = new ArrayList<>();
            for (String containerPath : volumeBindings.keySet()) {
                String hostPath = volumeBindings.get(containerPath);
                logger.info("binding volume: container={}, host={}", containerPath, hostPath);
                binds.add(new Bind(hostPath, new Volume(containerPath)));
            }
            cmd.withBinds(binds);
        }
        if (portBindings.size() > 0) {
            List<ExposedPort> exposedPorts = new ArrayList<>();
            Ports bindings = new Ports();
            for (PortBinding portBinding : portBindings) {
                logger.info("binding port: container={}, host={}, protocol={}",
                        portBinding.containerPort(), portBinding.hostPort(), portBinding.protocol());
                ExposedPort exposedPort = portBinding.getExposedPort();
                exposedPorts.add(exposedPort);

                bindings.bind(exposedPort, portBinding.getPortBinding());
            }
            cmd.withExposedPorts(exposedPorts).withPortBindings(bindings);
        }
        if (environmentVariables.size() > 0) {
            cmd.withEnv(environmentVariables);
        }

        CreateContainerResponse createResponse = cmd.exec();
        containerId = createResponse.getId();
        if (exists()) {
            logger.info("container created, containerName={}, containerId={}", containerName, containerId);
            return true;
        } else {
            logger.info("container not created, not exists after create, containerName={}", containerName);
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
            return Values.isNotBlank(inspectResponse.getId());
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
                "\"imageId\":\"" + imageId + "\"" +
                ", \"containerName\":\"" + containerName + "\"" +
                ", \"containerId\":\"" + containerId + "\"" +
                ", \"volumeBindings\":" + volumeBindings +
                ", \"networkMode\":\"" + networkMode + "\"" +
                '}';
    }
}
