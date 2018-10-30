package com.github.lkq.instadb;

import com.github.dockerjava.api.DockerClient;
import com.github.lkq.instadb.docker.DockerClientFactory;
import com.github.lkq.instadb.docker.DockerContainer;
import com.github.lkq.instadb.docker.DockerImage;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class InstaDB {
    private static final Logger logger = getLogger(InstaDB.class);

    private boolean initialized = false;

    private DockerClient dockerClient;

    private String imageName;
    private String containerName;

    private DockerImage dockerImage;
    private Logger dockerLogger;
    private DockerContainer dockerContainer;

    public InstaDB(String imageName, String containerName) {
        Values.requiresNotBlank(imageName, "image name is required");
        Values.requiresNotBlank(containerName, "container name is required");
        this.imageName = imageName;
        this.containerName = containerName;
    }

    public static InstaDB postgresql(String name) {
        return new InstaDB("postgres:latest", name);
    }

    public static InstaDB mysql(String name) {
        return new InstaDB("mysql:latest", name);
    }

    public InstaDB init() {
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

    public InstaDB dockerClient(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        return this;
    }

    public InstaDB dockerLogger(Logger dockerLogger) {
        this.dockerLogger = dockerLogger;
        return this;
    }

    public DockerContainer container() {
        return dockerContainer;
    }

    public void start(int timeoutInSeconds) {
        Values.requiresTrue(initialized, "InstaDB not initialized, forget to call init()?");

        if (!dockerImage.ensureExists(timeoutInSeconds)) {
            throw new IllegalStateException("failed to ensure docker image exists: " + dockerImage);
        }
        if (!dockerContainer.createAndReplace()) {
            throw new IllegalStateException("failed to create and replace container: " + dockerContainer);
        }
        if (!dockerContainer.run()) {
            throw new IllegalStateException("failed to run container: " + dockerContainer);
        }
    }
}
