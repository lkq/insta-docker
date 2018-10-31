package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
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

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DockerContainerTest {

    public static final String CONTAINER_NAME = "instadb-container-test";
    public static final String IMAGE_NAME = "busybox:latest";
    private static Logger dockerLogger = LoggerFactory.getLogger("docker-container-logger");
    private static DockerClient dockerClient;

    private DockerContainer subject;

    @BeforeAll
    static void setUp() {
        // get the logger specific configured for redirecting docker container logs
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
        DockerImage image = new DockerImage(dockerClient, IMAGE_NAME);
        image.ensureExists(60);
    }

    @Tag("integration")
    @Test
    void testContainerLifecycle() {
        subject = new DockerContainer(dockerClient, IMAGE_NAME, CONTAINER_NAME, dockerLogger);

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

        assertTrue(subject.ensureNotExists(), "failed to clear up container after test");
    }

    @Tag("integration")
    @Test
    void canStartContainerWithPortBindingsAndCmd() {
        int containerPort = 65432;
        int hostPort = 65431;
        subject = new DockerContainer(dockerClient, IMAGE_NAME, CONTAINER_NAME, dockerLogger)
                .commands(Arrays.asList("/bin/sleep", "3"))
                .bindPort(containerPort, hostPort, InternetProtocol.TCP);

        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
        assertTrue(subject.createAndReplace(), "failed to create and replace container");
        assertTrue(subject.run(), "failed to start container");

        // it have potential race condition, if the container stopped too soon,
        // the inspect command response will not containers the port bindings
        InspectContainerResponse container = dockerClient.inspectContainerCmd(CONTAINER_NAME).exec();
        Map<ExposedPort, Ports.Binding[]> bindings = container.getNetworkSettings().getPorts().getBindings();
        PortBinding portBinding = new PortBinding(containerPort, hostPort, InternetProtocol.TCP);
        Ports.Binding[] binding = bindings.get(portBinding.getExposedPort());
        assertEquals(1, binding.length);
        assertEquals(String.valueOf(hostPort), binding[0].getHostPortSpec());

        String[] cmd = container.getConfig().getCmd();
        assertArrayEquals(new String[]{"/bin/sleep", "3"}, cmd);

        assertTrue(subject.ensureNotExists(), "failed to clear up container after test");
    }

    @Tag("integration")
    @Test
    void canStartContainerWithVolumeBindings() {
        String containerPath = "/test_volume";
        String hostPath = ClassLoader.getSystemResource(".").getPath();
        subject = new DockerContainer(dockerClient, IMAGE_NAME, CONTAINER_NAME, dockerLogger)
                .commands(Arrays.asList("/bin/sleep", "3"))
                .bindVolume(containerPath, hostPath);

        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
        assertTrue(subject.createAndReplace(), "failed to create and replace container");
        assertTrue(subject.run(), "failed to start container");

        // it have potential race condition, if the container stopped too soon,
        // the inspect command response will not containers the volume bindings
        InspectContainerResponse container = dockerClient.inspectContainerCmd(CONTAINER_NAME).exec();
        Bind[] binds = container.getHostConfig().getBinds();
        assertTrue(binds.length > 0, "volume binds is empty");
        assertEquals("/test_volume", binds[0].getVolume().getPath());
        assertEquals("/Users/kingson/Sandbox/github/insta-db/target/test-classes/", binds[0].getPath());

        assertTrue(subject.ensureNotExists(), "failed to clear up container after test");

    }
}