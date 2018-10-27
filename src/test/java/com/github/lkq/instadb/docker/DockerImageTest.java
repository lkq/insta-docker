package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
    void willReturnTrueIfImageExists() {
        subject = new DockerImage(dockerClient, "not-exists-image:1.0");
        assertFalse(subject.exists(), "expected image not exist");
    }
}