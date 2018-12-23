package com.github.lkq.instadocker.docker.entity;

public class VolumeBinding {
    private String containerPath;
    private String hostPath;

    public VolumeBinding(String containerPath, String hostPath) {
        this.containerPath = containerPath;
        this.hostPath = hostPath;
    }

    public String containerPath() {
        return containerPath;
    }

    public String hostPath() {
        return hostPath;
    }

    @Override
    public String toString() {
        return "{" +
                "\"containerPath\":\"" + containerPath + "\"" +
                ", \"hostPath\":\"" + hostPath + "\"" +
                '}';
    }
}
