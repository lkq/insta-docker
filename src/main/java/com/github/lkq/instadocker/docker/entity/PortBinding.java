package com.github.lkq.instadocker.docker.entity;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.lkq.instadocker.exception.InstaDockerException;

public class PortBinding {
    private String protocol;
    private int containerPort;
    private int hostPort;

    public PortBinding(String protocol, int containerPort, int hostPort) {
        this.containerPort = containerPort;
        this.hostPort = hostPort;
        this.protocol = protocol;
    }

    public PortBinding(int port) {
        this(InternetProtocol.TCP.name(), port, port);
    }

    public ExposedPort toExposedPort() {
        switch (protocol) {
            case "UDP":
                return ExposedPort.udp(containerPort);
            case "TCP":
                return ExposedPort.tcp(containerPort);
            default:
                throw new InstaDockerException("unsupported protocol");
        }
    }

    public String protocol() {
        return protocol;
    }

    public int containerPort() {
        return containerPort;
    }

    public int hostPort() {
        return hostPort;
    }

    @Override
    public String toString() {
        return "{" +
                "\"protocol\":\"" + protocol + "\"" +
                ", \"containerPort\":" + containerPort +
                ", \"hostPort\":" + hostPort +
                '}';
    }
}
