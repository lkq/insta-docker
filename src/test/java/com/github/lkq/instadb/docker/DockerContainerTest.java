package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
        subject = new DockerContainer(dockerClient, "hello-world", "hello-world-test");

        subject.remove(true);
        assertFalse(subject.exists(), "container should not exist");
        assertFalse(subject.remove(false), "should return false when trying to remove non-exists container");
        assertTrue(subject.create(true), "should return true when creating container");
        assertTrue(subject.run(), "should be able to run container");
        assertTrue(subject.exists(), "should return true after container created");
        assertFalse(subject.create(false), "should return false when container exists without using force create");
        assertTrue(subject.create(true), "should return true when container exists and using force create");
        assertTrue(subject.remove(false), "should return true when remove container");

    }
}