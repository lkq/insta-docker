package com.github.lkq.instadocker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.lkq.instadocker.docker.entity.PortBinding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DockerContainerTest {

    public static final String CONTAINER_NAME = "instadocker-container-test";
    public static final String BUSY_BOX = "busybox:latest";
    private static Logger dockerLogger = LoggerFactory.getLogger(DockerContainerTest.class);
    private static DockerClient dockerClient;
    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private DockerContainer subject;

    @BeforeAll
    static void setUp() {
        // get the logger specific configured for redirecting docker container logs
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        dockerClient = DockerClientBuilder.getInstance(configBuilder.build()).build();
        DockerImage image = new DockerImage(dockerClient, BUSY_BOX);
        image.ensureExists(60);
    }

    @Tag("integration")
    @Test
    void testContainerLifecycle() {
        subject = new DockerContainer(dockerClient, BUSY_BOX, CONTAINER_NAME, dockerLogger);

        subject.ensureNotExists();
        assertFalse(subject.exists(), "container should not exist");
        assertTrue(subject.ensureNotExists(), "failed to ensure container not exist");
        assertTrue(subject.ensureExists(), "container should exists");
        assertTrue(subject.createOrReplace(), "failed to create and replace container");
        assertTrue(subject.exists(), "should return true after container created");
        assertTrue(subject.ensureRunning(), "should be able to run container");
        assertTrue(subject.ensureRunning(), "should return true if container already running");
        assertTrue(subject.isRunning(), "container is not running");
        assertTrue(subject.ensureStopped(30), "failed to ensure container is stopped");
        assertFalse(subject.isRunning(), "container is not running");
        assertTrue(subject.createOrReplace(), "failed to create and replace container");

        assertTrue(subject.ensureNotExists(), "failed to clear up container after test");
    }

    @Tag("integration")
    @Test
    void canStartContainer() {
        subject = new DockerContainer(dockerClient, BUSY_BOX, CONTAINER_NAME, dockerLogger)
                .environmentVariables(Arrays.asList("VAR1=value1", "VAR2=value2"))
                .commands(Arrays.asList("/bin/sleep", "3"))
                .hostName("hostName")
                .network("host");

        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
        assertTrue(subject.createOrReplace(), "failed to create and replace container");
        assertTrue(subject.ensureExists(), "container not exists");
        assertTrue(subject.ensureRunning(), "failed to start container");

        // it have potential race condition, if the container stopped too soon,
        // the inspect command response will not containers the volume bindings
        InspectContainerResponse container = dockerClient.inspectContainerCmd(CONTAINER_NAME).exec();

        dockerLogger.info(gson.toJson(container));

        Assertions.assertArrayEquals(new String[]{"/bin/sleep", "3"}, container.getConfig().getCmd());
        Assertions.assertNotNull(container.getNetworkSettings().getNetworks().get("host"), "network is not host");
        Assertions.assertEquals("VAR1=value1", container.getConfig().getEnv()[0]);
        Assertions.assertEquals("VAR2=value2", container.getConfig().getEnv()[1]);
        Assertions.assertEquals("hostName", container.getConfig().getHostName());

        assertTrue(subject.ensureNotExists(), "failed to clear up container after test");

    }

    @Tag("integration")
    @Test
    void canStartContainerWithPortBindingsAndCmd() {
        int containerPort = 65432;
        int hostPort = 65431;
        subject = new DockerContainer(dockerClient, BUSY_BOX, CONTAINER_NAME, dockerLogger)
                .commands(Arrays.asList("/bin/sleep", "3"))
                .portBinding(InternetProtocol.TCP.name(), containerPort, hostPort);

        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
        assertTrue(subject.createOrReplace(), "failed to create and replace container");
        assertTrue(subject.ensureExists(), "container not exists");
        assertTrue(subject.ensureRunning(), "failed to start container");

        // it have potential race condition, if the container stopped too soon,
        // the inspect command response will not containers the port bindings
        InspectContainerResponse container = dockerClient.inspectContainerCmd(CONTAINER_NAME).exec();
        Map<ExposedPort, Ports.Binding[]> bindings = container.getNetworkSettings().getPorts().getBindings();
        PortBinding portBinding = new PortBinding(InternetProtocol.TCP.name(), containerPort, hostPort);
        Ports.Binding[] binding = bindings.get(portBinding.toExposedPort());
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
        subject = new DockerContainer(dockerClient, BUSY_BOX, CONTAINER_NAME, dockerLogger)
                .commands(Arrays.asList("/bin/sleep", "3"))
                .volumeBinding(containerPath, hostPath);

        assertTrue(subject.ensureNotExists(), "failed to ensure container not exists");
        assertTrue(subject.createOrReplace(), "failed to create and replace container");
        assertTrue(subject.ensureExists(), "container not exists");
        assertTrue(subject.ensureRunning(), "failed to start container");

        // it have potential race condition, if the container stopped too soon,
        // the inspect command response will not containers the volume bindings
        InspectContainerResponse container = dockerClient.inspectContainerCmd(CONTAINER_NAME).exec();
        Bind[] binds = container.getHostConfig().getBinds();
        assertTrue(binds.length > 0, "volume binds is empty");
        assertEquals("/test_volume", binds[0].getVolume().getPath());
        assertEquals(hostPath, binds[0].getPath());

        assertTrue(subject.ensureNotExists(), "failed to clear up container after test");

    }
}