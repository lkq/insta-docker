package com.github.lkq.instadocker.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;

public class DockerClientFactory {
    public static DockerClient defaultClient() {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        return DockerClientBuilder.getInstance(configBuilder.build()).build();
    }
}
