package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerContainerTest {

    private DockerContainer subject;
    private DockerClient dockerClient;

    @BeforeEach
    void setUp() {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
    }

    @Tag("integration")
    @Test
    void testContainerLifecycle() {
        // get the logger specific configured for redirecting docker container logs
        Logger logger = LoggerFactory.getLogger("docker-container-logger");
        subject = new DockerContainer(dockerClient, "hello-world", "hello-world-test", logger);

        DockerImage image = new DockerImage(dockerClient, "hello-world:latest");
        image.ensureExists(30);

        subject.ensureNotExists();
        assertFalse(subject.exists(), "container should not exist");
        assertTrue(subject.ensureNotExists(), "container should not exist");
        assertTrue(subject.createAndReplace(), "should able to create container");
        assertTrue(subject.exists(), "should return true after container created");
        assertTrue(subject.run(), "should be able to run container");
        assertTrue(subject.isRunning(), "container should be running");
        assertTrue(subject.stop(), "should be able to stop container");
        assertTrue(subject.createAndReplace(), "should be able to replace container");
        assertTrue(subject.ensureNotExists(), "should be able to remove container");

    }
}