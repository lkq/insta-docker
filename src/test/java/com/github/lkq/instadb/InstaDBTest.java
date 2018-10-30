package com.github.lkq.instadb;

import com.github.dockerjava.api.model.InternetProtocol;
import com.github.lkq.instadb.docker.DockerClientFactory;
import com.github.lkq.instadb.docker.PortFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

class InstaDBTest {
    private InstaDB subject;
    private final Logger dockerLogger = LoggerFactory.getLogger("docker-container-logger");

    @BeforeEach
    void setUp() {
    }

    @Tag("integration")
    @Test
    void canStartPGContainer() throws InterruptedException {
        subject = InstaDB.postgresql("instadb-pg-test")
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();

        subject.container().bindPort(5432, PortFinder.find(), InternetProtocol.TCP);

        subject.start(60);

    }

    @Tag("integration")
    @Test
    void canStartMySQLContainer() {
        subject = InstaDB.mysql("instadb-mysql-test")
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();
        subject.container().environmentVariables(Arrays.asList("MYSQL_ROOT_PASSWORD=123"));

        subject.container().bindPort(3306, PortFinder.find(), InternetProtocol.TCP);
        subject.container().bindPort(33060, PortFinder.find(), InternetProtocol.TCP);

        subject.start(60);
    }
}