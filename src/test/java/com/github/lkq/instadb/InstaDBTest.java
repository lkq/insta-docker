package com.github.lkq.instadb;

import com.github.lkq.instadb.docker.DockerClientFactory;
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
        subject = InstaDB.postgresql("instadb-pg-test", 0)
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();

        subject.start(60);

    }

    @Tag("integration")
    @Test
    void canStartMySQLContainer() {
        subject = InstaDB.mysql("instadb-mysql-test", 0)
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();
        subject.container().environmentVariables(Arrays.asList("MYSQL_ROOT_PASSWORD=123"));

        subject.start(60);
    }
}