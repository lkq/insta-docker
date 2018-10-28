package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerImageTest {

    private DockerImage subject;
    private static DockerClient dockerClient;

    @BeforeAll
    static void setUp() {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
    }

    @Tag("integration")
    @Test
    void testImageLifecycle() {
        subject = new DockerImage(dockerClient, "hello-world");
        subject.ensureNotExists();
        assertFalse(subject.exists(), "image should not exists");
        assertTrue(subject.ensureExists(60), "image should exists");
        assertTrue(subject.ensureExists(30), "image should exists");
        assertTrue(subject.exists(), "image should exists");
        assertTrue(subject.ensureNotExists(), "image should not exists");
        assertFalse(subject.exists(), "image should not exists");

    }
}