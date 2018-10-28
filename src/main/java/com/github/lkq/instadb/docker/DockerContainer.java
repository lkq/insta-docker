package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.lkq.instadb.Strings;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

public class DockerContainer {

    private static final Logger logger = getLogger(DockerContainer.class);

    private DockerClient dockerClient;
    private String imageId;
    private String containerName;
    private String containerId;
    private ContainerLogger containerLogger;

    private Map<String, String> volumnBindings = new TreeMap<>();
    private String networkMode;

    /**
     * class for manipulate docker containers.
     *
     * @param dockerClient  the docker-java api client
     * @param imageId       the docker image id
     * @param containerName the docker container name to use
     * @param logger        the logger for outputting docker console logs, will use the DockerContainer.logger if not provided
     */
    public DockerContainer(DockerClient dockerClient, String imageId, String containerName, Logger logger) {
        Objects.requireNonNull(dockerClient);
        Strings.requiresNotBlank(imageId, "imageId is required");
        Strings.requiresNotBlank(containerName, "containerName is required");
        this.dockerClient = dockerClient;
        this.imageId = imageId;
        this.containerName = containerName;
        this.containerLogger = new ContainerLogger(logger == null ? DockerContainer.logger : logger, containerName);
    }

    public DockerContainer bindVolumn(String containerPath, String hostPath) {
        volumnBindings.put(containerPath, hostPath);
        return this;
    }

    public DockerContainer network(String networkMode) {
        this.networkMode = networkMode;
        return this;
    }

    public boolean run() {
        if (isRunning()) {
            logger.info("container already running, containerName={}", containerName);
            return true;
        }
        if (exists()) {
            logger.info("trying to run container, containerName={}", containerName);
            dockerClient.startContainerCmd(containerName).exec();
            if (isRunning()) {
                this.containerLogger.attach(dockerClient);
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

    public boolean ensureStopped() {
        return true;
    }

    /**
     * create the docker container
     *
     * @return true only if the container was actually created by this method
     */
    public boolean createAndReplace() {
        logger.info("creating container, imageId={}, containerName={}", imageId, containerName);
        if (exists()) {
            if (ensureNotExists()) {
                logger.info("creating container, removed old container, containerName={}", containerName);
            } else {
                logger.error("container not created, already exists but failed to remove, containerName={}", containerName);
                return false;
            }
        }
        CreateContainerCmd cmd = dockerClient.createContainerCmd(imageId);
        cmd.withName(containerName);

        List<Bind> binds = new ArrayList<>();
        for (String key : volumnBindings.keySet()) {
            binds.add(new Bind(volumnBindings.get(key), new Volume(key)));
        }
        if (binds.size() > 0) {
            cmd.withBinds(binds);
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
            return Strings.isNotBlank(inspectResponse.getId());
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
}
