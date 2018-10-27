package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.lkq.instadb.Strings;
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
        Strings.requiresNotBlank(imageId, "imageId is required");
        this.dockerClient = dockerClient;
        this.imageId = imageId;
    }

    /**
     * check if the image already exists in local
     * @return true if the image exists, false if the image does not exists
     * @throws DockerClientException if error happens
     */
    public boolean exists() {
        try {
            InspectImageResponse inspectResponse = dockerClient.inspectImageCmd(imageId).exec();
            if (logger.isDebugEnabled()) {
                logger.debug("inspecting image, result={}", inspectResponse.toString());
            }
            return Strings.isNotBlank(inspectResponse.getId());
        } catch (NotFoundException e) {
            logger.info("image not found, imageId={}, reason={}", imageId, e.getMessage());
            return false;
        } catch (Exception e) {
            String message = "failed to check image existence, imageId=" + imageId + ", reason=" + e.getMessage();
            logger.warn(message);
            throw new DockerClientException(message, e);
        }
    }

    /**
     * pull the image from docker hub
     * @param timeoutInSeconds pull timeout
     * @return true if the image was actually been pulled, false if image pull timeout
     * @throws DockerClientException if error happens
     */
    public boolean pull(int timeoutInSeconds) {
        try {
            return dockerClient.pullImageCmd(imageId).exec(new PullImageResultCallback())
                    .awaitCompletion(timeoutInSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            String message = "pull image interrupted, timeout=" + timeoutInSeconds;
            logger.warn(message);
            throw new DockerClientException(message, e);
        } catch (Exception e) {
            String message = "failed to pull image, imageId=" + imageId;
            logger.warn(message, e);
            throw new DockerClientException(message, e);
        }
    }

    /**
     * remove the image
     * @param force force to remove the image
     * @return true if the image was actually been removed, false if image not exists
     * @throws DockerClientException if error happens
     */
    public boolean remove(boolean force) {
        try {
            if (exists()) {
                dockerClient.removeImageCmd(imageId).withForce(force).exec();
                if (exists()) {
                    String message = "image still exists after remove, imageId=" + imageId;
                    logger.warn(message);
                    throw new DockerClientException(message);
                }
                logger.info("image removed, imageId={}", imageId);
                return true;
            }
        } catch (DockerClientException e) {
            throw e;
        } catch (Exception e) {
            String message = "failed to remove image, imageId=" + imageId + ", reason=" + e.getMessage();
            logger.warn(message, e);
            throw new DockerClientException(message, e);
        }
        return false;
    }
}
