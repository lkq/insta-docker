package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.lkq.instadb.InstaUtils;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class DockerImage {
    private static final Logger logger = getLogger(DockerImage.class);

    private DockerClient dockerClient;

    private String imageId;

    public DockerImage(DockerClient dockerClient, String imageId) {
        this.dockerClient = dockerClient;
        this.imageId = imageId;
    }

    public boolean exists() {
        try {
            InspectImageResponse inspectResponse = dockerClient.inspectImageCmd(imageId).exec();
            if (logger.isDebugEnabled()) {
                logger.debug(inspectResponse.toString());
            }
            return InstaUtils.isNotEmpty(inspectResponse.getId());
        } catch (Exception e) {
            logger.warn("failed to check image existence, imageId={}, cause={}", imageId, e.getMessage());
        }
        return false;
    }
}
