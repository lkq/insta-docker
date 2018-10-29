package com.github.lkq.instadb;

import com.github.lkq.instadb.docker.DockerClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.slf4j.LoggerFactory.getLogger;

class InstaDBTest {
    private static final Logger logger = getLogger(InstaDBTest.class);
    private InstaDB subject;

    @BeforeEach
    void setUp() {
        Logger dockerLogger = LoggerFactory.getLogger("docker-container-logger");
        subject = InstaDB.postgresql("instadb-pg-test", 0)
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();
    }

    @Tag("integration")
    @Test
    void canStartPGContainer() throws InterruptedException {
        subject.start(60);
        Thread.sleep(10000);
    }
}