package com.github.lkq.instadb.docker;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;

public class PortBinding {
    private int containerPort;
    private int hostPort;
    private InternetProtocol protocol;

    public PortBinding(int containerPort, int hostPort, InternetProtocol protocol) {
        this.containerPort = containerPort;
        this.hostPort = hostPort;
        this.protocol = protocol;
    }

    public PortBinding(int port, InternetProtocol protocol) {
        this(port, port, protocol);
    }

    public PortBinding(int port) {
        this(port, port, InternetProtocol.TCP);
    }

    public ExposedPort getExposedPort() {
        switch (protocol) {
            case UDP:
                return ExposedPort.udp(containerPort);
            default:
                return ExposedPort.tcp(containerPort);
        }
    }

    public Ports.Binding getPortBinding() {
        return Ports.Binding.bindPort(hostPort);
    }

    public int containerPort() {
        return containerPort;
    }

    public int hostPort() {
        return hostPort;
    }

    public InternetProtocol protocol() {
        return protocol;
    }

    @Override
    public String toString() {
        return "{" +
                "\"containerPort\":" + containerPort +
                ", \"hostPort\":" + hostPort +
                ", \"protocol\":" + protocol +
                '}';
    }
}
