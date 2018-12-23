package com.github.lkq.instadocker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.lkq.instadocker.Assert;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class DockerImage {
    private static final Logger logger = getLogger(DockerImage.class);

    private DockerClient dockerClient;

    private String imageId;

    public DockerImage(DockerClient dockerClient, String imageId) {
        Objects.requireNonNull(dockerClient, "dockerClient is required");
        Assert.requiresNotBlank(imageId, "imageId is required");
        this.dockerClient = dockerClient;
        this.imageId = imageId;
    }

    /**
     * check if the image already exists in local
     *
     * @return true if the image exists, false if the image does not exists
     * @throws DockerClientException if error happens
     */
    public boolean exists() {
        try {
            InspectImageResponse inspectResponse = dockerClient.inspectImageCmd(imageId).exec();
            logger.debug("check image existence: inspect result={}", inspectResponse);
            return Assert.isNotBlank(inspectResponse.getId());
        } catch (NotFoundException e) {
            logger.debug("check image existence: image not found, imageId=" + imageId, e);
            return false;
        }
    }

    /**
     * ensure the image exists, if it's not already exists, pull the image from docker hub
     *
     * @param timeoutInSeconds pull timeout
     * @return true if the image exists or pulled successfully
     */
    public boolean ensureExists(int timeoutInSeconds) {
        try {
            if (!exists()) {
                boolean pulled = dockerClient.pullImageCmd(imageId).exec(new PullImageResultCallback())
                        .awaitCompletion(timeoutInSeconds, TimeUnit.SECONDS);
                if (pulled) {
                    logger.info("pulled image, imageId={}", imageId);
                }
                return pulled;
            } else {
                return true;
            }
        } catch (InterruptedException e) {
            logger.warn("failed to pull image, imageId=" + imageId, e);
            return false;
        }
    }

    /**
     * ensure the image does not exists, if it's already exist, remove it
     *
     * @return true if the image does not exist or removed successfully
     */
    public boolean ensureNotExists() {
        if (exists()) {
            dockerClient.removeImageCmd(imageId).withForce(true).exec();
            if (exists()) {
                logger.warn("failed to remove image: image still exists after remove, imageId=" + imageId);
                return false;
            } else {
                logger.info("image removed, imageId={}", imageId);
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "\"imageId\":\"" + imageId + "\"" +
                '}';
    }
}
