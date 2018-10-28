package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
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

    private Map<String, String> volumnBindings = new TreeMap<>();
    private String networkMode;

    public DockerContainer(DockerClient dockerClient, String imageId, String containerName) {
        Objects.requireNonNull(dockerClient);
        Strings.requiresNotBlank(imageId, "imageId is required");
        Strings.requiresNotBlank(containerName, "containerName is required");
        this.dockerClient = dockerClient;
        this.imageId = imageId;
        this.containerName = containerName;
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
        if (!exists()) {
            String message = "container not exist, imageId=" + imageId + ", containerName=" + containerName;
            logger.error(message);
            throw new DockerClientException(message);
        } else {
            try {
                dockerClient.startContainerCmd(containerName).exec();
                InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerName).exec();
                Boolean state = inspectResponse.getState().getRunning();
                boolean isRunning = state == null ? false : state;
                if (isRunning) {
                    attachLogging();
                }
                return isRunning;
            } catch (Exception e) {
                String message = "failed to start container, containerName=" + containerName + ", reason=" + e.getMessage();
                logger.warn(message);
                throw new DockerClientException(message, e);
            }
        }
    }

    public void attachLogging() {
        new Thread(() -> {
            try {
                logger.info("attaching logs from container: {}", containerName);
                dockerClient.logContainerCmd(containerName)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(new ContainerLogger());
            } catch (Exception e) {
                logger.error("failed to redirect container log");
            }
        }).start();
    }

    /**
     * create the docker container
     *
     * @param force if set to true and the container already exist, will remove the old container and create a new one
     * @return true only if the container was actually created by this method
     */
    public boolean create(boolean force) {
        logger.info("creating container, imageId={}, containerName={}, force={}", imageId, containerName, force);
        boolean exists = exists();
        if (exists) {
            if (force) {
                if (remove(true)) {
                    logger.info("container exists and removed, containerName={}", containerName);
                } else {
                    String message = "container not created: container already exists but failed to remove, containerName=" + containerName;
                    logger.error(message);
                    throw new DockerClientException(message);
                }
            } else {
                logger.info("container not created: container already exist, imageId={}, containerName={}, force={}", imageId, containerName, force);
                return false;
            }
        }
        try {
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
            logger.info("container created, containerId={}", containerId);
            return true;
        } catch (Exception e) {
            String message = "failed to create container, imageId=" + imageId
                    + ", containerName=" + containerName + ", reason=" + e.getMessage();
            logger.error(message);
            throw new DockerClientException(message, e);
        }
    }

    /**
     * check if the container already exists in local
     *
     * @return true if the container exists, false if the container does not exists
     * @throws DockerClientException if error happens
     */
    public boolean exists() {
        try {
            InspectContainerResponse inspectResponse = dockerClient.inspectContainerCmd(containerName).exec();
            return Strings.isNotBlank(inspectResponse.getId());
        } catch (NotFoundException e) {
            logger.debug("container not found, containerName={}, reason={}", containerName, e.getMessage());
            return false;
        } catch (Exception e) {
            String message = "failed to check container existence, containerName=" + containerName + ", reason=" + e.getMessage();
            logger.error(message);
            throw new DockerClientException(message, e);
        }
    }

    /**
     * remove the container
     *
     * @param force force remove
     * @return true if the container was actually been removed, false if container not exists
     * @throws DockerClientException if error happens
     */
    public boolean remove(boolean force) {
        try {
            if (exists()) {
                dockerClient.removeContainerCmd(containerName).withForce(force).exec();
                if (exists()) {
                    String message = "container still exists after remove, containerName=" + containerName + ", force=" + force;
                    logger.warn(message);
                    throw new DockerClientException(message);
                }
                logger.info("container removed, imageId={}, containerName={}", imageId, containerName);
                return true;
            }
        } catch (DockerClientException e) {
            throw e;
        } catch (Exception e) {
            String message = "failed to remove container, containerName=" + containerName + ", reason=" + e.getMessage();
            logger.error(message, e);
            throw new DockerClientException(message, e);
        }
        return false;
    }
}
