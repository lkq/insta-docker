package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.lkq.instadb.Strings;
import org.slf4j.Logger;

import java.util.Objects;

public class ContainerLogger extends LogContainerResultCallback {

    private Logger logger;
    private String containerName;

    public ContainerLogger(Logger logger, String containerName) {
        Objects.requireNonNull(logger, "logger is required");
        Strings.requiresNotBlank(containerName, "containerName is required");
        this.logger = logger;
        this.containerName = containerName;
    }

    @Override
    public void onNext(Frame frame) {
        if (frame.getPayload().length > 0) {
            logger.info("[{}] - {}", containerName, new String(frame.getPayload(), 0, frame.getPayload().length - 1).trim());
        }
    }

    public void attach(DockerClient dockerClient) {
        logger.info("redirecting logs from container: {}", containerName);
        new Thread(() -> {
            try {
                dockerClient.logContainerCmd(this.containerName)
                        .withStdErr(true)
                        .withStdOut(true)
                        .withFollowStream(true)
                        .withTailAll()
                        .exec(this);
            } catch (Exception e) {
                logger.error("failed to redirect container log");
            }
        }).start();
    }
}
