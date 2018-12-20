package com.github.lkq.instadocker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.lkq.instadocker.Values;
import org.slf4j.Logger;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

public class ContainerLogger extends LogContainerResultCallback {
    private static final Logger logger = getLogger(ContainerLogger.class);

    private Logger redirectLogger;
    private String containerName;

    public ContainerLogger(Logger redirectLogger, String containerName) {
        Objects.requireNonNull(redirectLogger, "logger is required");
        Values.requiresNotBlank(containerName, "containerName is required");
        this.redirectLogger = redirectLogger;
        this.containerName = containerName;
    }

    @Override
    public void onNext(Frame frame) {
        if (frame.getPayload().length > 0) {
            redirectLogger.info("[{}] - {}", containerName, new String(frame.getPayload(), 0, frame.getPayload().length - 1).trim());
        }
    }

    public void attach(DockerClient dockerClient) {
        logger.info("redirecting logs from container, containerName={}, logger={}", containerName, redirectLogger.getName());
        new Thread(() -> {
            try {
                dockerClient.logContainerCmd(this.containerName)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(this);
            } catch (Exception e) {
                redirectLogger.error("failed to redirect container log");
            }
        }).start();
    }
}
