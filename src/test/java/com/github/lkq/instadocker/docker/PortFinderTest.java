package com.github.lkq.instadocker.docker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PortFinderTest {

    @Test
    void canFindPort() {
        assertTrue(PortFinder.find(10) > 0, "can't find valid port");
    }
}