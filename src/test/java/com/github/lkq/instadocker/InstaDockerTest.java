package com.github.lkq.instadocker;

import com.github.dockerjava.api.model.InternetProtocol;
import com.github.lkq.instadocker.docker.DockerClientFactory;
import com.github.lkq.instadocker.docker.PortFinder;
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

class InstaDockerTest {

    private static final Logger logger = getLogger(InstaDockerTest.class);

    private InstaDocker subject;
    private final Logger dockerLogger = getLogger("docker-container-logger");

    @BeforeEach
    void setUp() {
    }

    @Tag("integration")
    @Test
    void canStartPGContainer() throws SQLException, InterruptedException {
        subject = new InstaDocker("postgres:latest", "insta-docker-pg-test")
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();
        try {

            int hostPort = PortFinder.find(10);
            subject.container().portBinding(InternetProtocol.TCP.name(), 5432, hostPort);
            subject.container().environmentVariables(Arrays.asList("POSTGRES_PASSWORD=" + hostPort));

            subject.start(true, 60);

            ResultSet resultSet = executeQuery("jdbc:postgresql://localhost:" + hostPort + "/", "postgres", String.valueOf(hostPort),
                    "select character_value from information_schema.sql_implementation_info where implementation_info_name = 'DBMS NAME';");
            assertTrue(resultSet.next(), "failed to query dbms name");
            String dbmsName = resultSet.getString(1);
            assertEquals("PostgreSQL", dbmsName);
        } finally {
            logger.info("clearing test container");
            subject.container().ensureStopped(60);
            subject.container().ensureNotExists();
        }
    }

    @Tag("integration")
    @Test
    void canStartMySQLContainer() throws SQLException, InterruptedException {
        subject = new InstaDocker("mysql:latest", "insta-docker-mysql-test")
                .dockerClient(DockerClientFactory.defaultClient())
                .dockerLogger(dockerLogger)
                .init();
        try {
            int hostPort = PortFinder.find(10);
            subject.container().environmentVariables(Arrays.asList("MYSQL_ROOT_PASSWORD=" + hostPort));

            subject.container().portBinding(InternetProtocol.TCP.name(), 3306, hostPort);
            subject.container().portBinding(InternetProtocol.TCP.name(), 33060, PortFinder.find(10));

            subject.start(true, 60);

            ResultSet resultSet = executeQuery("jdbc:mysql://localhost:" + hostPort + "/", "root", String.valueOf(hostPort),
                    "select TRANSACTIONS from information_schema.ENGINES where engine = 'InnoDB';");
            assertTrue(resultSet.next(), "failed to query mysql");
            String dbmsName = resultSet.getString(1);
            assertEquals("YES", dbmsName);
        } finally {
            logger.info("clearing test container");
            subject.container().ensureStopped(60);
            subject.container().ensureNotExists();
        }
    }

    private ResultSet executeQuery(String url, String user, String password, String queryStatement) throws InterruptedException, SQLException {
        Connection connection = null;
        int retryCount = 20;
        while (connection == null && retryCount-- > 0) {
            try {
                connection = DriverManager.getConnection(url, user, password);
            } catch (SQLException e) {
                logger.warn("failed to connect to db, retrying " + retryCount, e);
                Thread.sleep(5000);
            }
        }
        assertNotNull(connection, "failed to create to db");
        PreparedStatement statement = connection.prepareStatement(queryStatement);
        return statement.executeQuery();
    }
}