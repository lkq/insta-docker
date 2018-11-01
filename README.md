# Insta DB

a wrapper over docker-java api to startup a local db docker container for testing

[![Build Status](https://travis-ci.org/lkq/insta-db.svg?branch=master)](https://travis-ci.org/lkq/insta-db)


#### Dependency

    <dependency>
        <groupId>com.github.lkq</groupId>
        <artifactId>insta-db</artifactId>
        <version>0.1.3</version>
    </dependency>


#### Usage

Setup

    instaDB = InstaDB.postgresql("instadb-pg-container")
            .dockerClient(DockerClientFactory.defaultClient())
            .dockerLogger(dockerLogger)
            .init();

    int hostPort = PortFinder.find();
    instaDB.container().bindPort(5432, hostPort, InternetProtocol.TCP);
    instaDB.container().environmentVariables(Arrays.asList("POSTGRES_PASSWORD=password01"));

    instaDB.start(60);

Connect

    connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + hostPort + "/", "postgres", "password01");
