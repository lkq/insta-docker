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
        subject.remove(true);
        assertFalse(subject.exists(), "image should not exists");
        assertTrue(subject.pull(30), "failed to pull image");
        assertTrue(subject.pull(30), "failed to pull image");
        assertTrue(subject.exists(), "image should be exists");
        assertTrue(subject.remove(false), "failed to remove image");
        assertFalse(subject.remove(true), "return true when trying to remove non-exists image");
        assertFalse(subject.exists(), "image should not exists");

    }
}