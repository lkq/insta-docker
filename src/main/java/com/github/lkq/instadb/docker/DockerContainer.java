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

    /**
     * create the docker container
     *
     * @param force if set to true and the container already exist, will remove the old container and create a new one
     * @return true only if the container was actually created by this method
     */
    public boolean create(boolean force) {
        boolean exists = exists();
        if (exists && !force) {
            return false;
        }
        if (exists) {
            if (!remove(true)) {
                String message = "container already exists but failed to remove, containerName=" + containerName;
                logger.error(message);
                throw new DockerClientException(message);
            }
        }
        try {
            CreateContainerCmd cmd = dockerClient.createContainerCmd(imageId);
            cmd.withName(containerName);

            List<Bind> binds = new ArrayList<>();
            for (String key : volumnBindings.keySet()) {
                binds.add(new Bind(volumnBindings.get(key), new Volume(key)));
            }
            cmd.withBinds(binds);

            CreateContainerResponse createResponse = cmd.exec();
            containerId = createResponse.getId();
            return true;
        } catch (Exception e) {
            String message = "failed to create container, imageId=" + imageId
                    + ", containerName=" + containerName + ", reason=" + e.getMessage();
            logger.error(message);
            throw new DockerClientException(message, e);
        }
    }

    public DockerContainer bindVolumn(String containerPath, String hostPath) {
        volumnBindings.put(containerPath, hostPath);
        return this;
    }

    public DockerContainer network(String networkMode) {
        this.networkMode = networkMode;
        return this;
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
                    String message = "container still exists after remove, containerName=" + containerName;
                    logger.warn(message);
                    throw new DockerClientException(message);
                }
                logger.info("container removed, containerName={}", containerName);
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
