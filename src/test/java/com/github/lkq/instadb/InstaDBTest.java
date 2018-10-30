package com.github.lkq.instadb;

import com.github.dockerjava.api.model.InternetProtocol;
import com.github.lkq.instadb.docker.DockerClientFactory;
import com.github.lkq.instadb.docker.PortFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.*;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

class InstaDBTest {

    private static final Logger logger = getLogger(InstaDBTest.class);

    private InstaDB subject;
    private final Logger dockerLogger = getLogger("docker-container-logger");

    @BeforeEach
    void setUp() {
    }

    @Tag("integration")
    @Test
    void canStartPGContainer() throws SQLException, InterruptedException {
        subject = InstaDB.postgresql("instadb-pg-test")
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();
        try {

            int hostPort = PortFinder.find();
            subject.container().bindPort(5432, hostPort, InternetProtocol.TCP);
            subject.container().environmentVariables(Arrays.asList("POSTGRES_PASSWORD=" + hostPort));

            subject.start(60);

            Connection connection = null;
            int retryCount = 5;
            while (connection == null && retryCount-- > 0) {
                try {
                    connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + hostPort + "/", "postgres", String.valueOf(hostPort));
                } catch (SQLException e) {
                    logger.warn("failed to connect to postgres, retrying {}", retryCount);
                    Thread.sleep(3000);
                }
            }
            assertNotNull(connection, "failed to create connection");
            PreparedStatement statement = connection.prepareStatement("select character_value from information_schema.sql_implementation_info where implementation_info_name = 'DBMS NAME';");
            ResultSet resultSet = statement.executeQuery();
            assertTrue(resultSet.next(), "failed to query dbms name");
            String dbmsName = resultSet.getString(1);
            assertEquals("PostgreSQL", dbmsName);
        } finally {
            subject.container().ensureStopped(60);
            subject.container().ensureNotExists();
        }
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