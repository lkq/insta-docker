package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerContainerTest {

    private static Logger dockerLogger = LoggerFactory.getLogger("docker-container-logger");
    private static DockerClient dockerClient;

    private DockerContainer subject;

    @BeforeAll
    static void setUp() {
        // get the logger specific configured for redirecting docker container logs
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
        DockerImage image = new DockerImage(dockerClient, "hello-world:latest");
        image.ensureExists(60);
    }

    @Tag("integration")
    @Test
    void testContainerLifecycle() {
        subject = new DockerContainer(dockerClient, "hello-world", "hello-world-test", dockerLogger);

        subject.ensureNotExists();
        assertFalse(subject.exists(), "container should not exist");
        assertTrue(subject.ensureNotExists(), "failed to ensure container not exist");
        assertTrue(subject.createAndReplace(), "failed to create and replace container");
        assertTrue(subject.exists(), "should return true after container created");
        assertTrue(subject.run(), "should be able to run container");
        assertTrue(subject.run(), "should return true if container already running");
        assertTrue(subject.isRunning(), "container is not running");
        assertTrue(subject.ensureStopped(30), "failed to ensure container is stopped");
        assertFalse(subject.isRunning(), "container is not running");
        assertTrue(subject.createAndReplace(), "failed to create and replace container");
        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
    }

    @Tag("integration")
    @Test
    void canStartContainerWithPortBindings() throws InterruptedException {
        String containerName = "hello-world-test";
        int testPort = 65432;
        subject = new DockerContainer(dockerClient, "hello-world", containerName, dockerLogger)
                .bindPort(testPort, testPort, InternetProtocol.TCP);

        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
        assertTrue(subject.createAndReplace(), "failed to create and replace container");
        assertTrue(subject.run(), "failed to start container");

        // it have potential race condition, if the container stopped too soon,
        // the inspect command response will not containers the port bindings
        InspectContainerResponse container = dockerClient.inspectContainerCmd(containerName).exec();
        Map<ExposedPort, Ports.Binding[]> bindings = container.getNetworkSettings().getPorts().getBindings();
        PortBinding portBinding = new PortBinding(testPort, testPort, InternetProtocol.TCP);
        Ports.Binding[] binding = bindings.get(portBinding.getExposedPort());
        assertEquals(1, binding.length);
        assertEquals(String.valueOf(testPort), binding[0].getHostPortSpec());
    }

    @Test
    void canStartContainerWithVolumeBindings() {

    }
}