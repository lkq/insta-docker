# Insta Docker

a wrapper over docker-java api to startup a local db docker container for testing

[![Build Status](https://travis-ci.org/lkq/insta-docker.svg?branch=master)](https://travis-ci.org/lkq/insta-docker)


#### Dependency

    <dependency>
        <groupId>com.github.lkq</groupId>
        <artifactId>insta-docker</artifactId>
        <version>0.1.4</version>
    </dependency>


#### Usage

Setup

    instaDocker = new InstaDocker("postgres:latest", "insta-pg-container")
            .dockerClient(DockerClientFactory.defaultClient())
            .dockerLogger(dockerLogger)
            .init();

    int hostPort = PortFinder.find();
    instaDocker.container().bindPort(5432, hostPort, InternetProtocol.TCP)
                            .environmentVariables(Arrays.asList("POSTGRES_PASSWORD=password01"));

    instaDocker.start(true, 60);

Connect

    connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + hostPort + "/", "postgres", "password01");
