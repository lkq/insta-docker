package com.github.lkq.instadocker;

import com.github.dockerjava.api.DockerClient;
import com.github.lkq.instadocker.docker.DockerClientFactory;
import com.github.lkq.instadocker.docker.DockerContainer;
import com.github.lkq.instadocker.docker.DockerImage;
import com.github.lkq.instadocker.util.Assert;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class InstaDocker {
    private static final Logger logger = getLogger(InstaDocker.class);

    private boolean initialized = false;

    private DockerClient dockerClient;

    private String imageName;
    private String containerName;

    private DockerImage dockerImage;
    private Logger dockerLogger;
    private DockerContainer dockerContainer;

    public InstaDocker(String imageName, String containerName) {
        Assert.requiresNotBlank(imageName, "image name is required");
        Assert.requiresNotBlank(containerName, "container name is required");
        this.imageName = imageName;
        this.containerName = containerName;
    }

    public InstaDocker init() {
        Assert.requiresTrue(!initialized, "instance already initialized");
        if (dockerClient == null) {
            dockerClient = DockerClientFactory.defaultClient();
        }
        if (dockerLogger == null) {
            dockerLogger = logger;
        }
        dockerImage = new DockerImage(dockerClient, imageName);
        dockerContainer = new DockerContainer(dockerClient, imageName, containerName, dockerLogger);
        initialized = true;
        return this;
    }

    public InstaDocker dockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        return this;
    }

    public InstaDocker dockerLogger(Logger dockerLogger) {
        this.dockerLogger = dockerLogger;
        return this;
    }

    public DockerContainer container() {
        return dockerContainer;
    }

    public void start(boolean cleanStart, int timeoutInSeconds) {
        Assert.requiresTrue(initialized, "instance haven't been initialized, forget to call init()?");

        if (!dockerImage.ensureExists(timeoutInSeconds)) {
            throw new IllegalStateException("failed to pull image: " + dockerImage);
        }
        if (cleanStart) {
            if (!dockerContainer.createOrReplace()) {
                throw new IllegalStateException("failed to create or replace container: " + dockerContainer);
            }
        } else {
            if (!dockerContainer.ensureExists()) {
                throw new IllegalStateException("failed to create container: " + dockerContainer);
            }
        }
        if (!dockerContainer.ensureRunning()) {
            throw new IllegalStateException("failed to run container: " + dockerContainer);
        }
    }
}
