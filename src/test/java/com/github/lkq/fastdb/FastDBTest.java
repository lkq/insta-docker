package com.github.lkq.fastdb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FastDBTest {
    private FastDB subject;

    @BeforeEach
    void setUp() {
        subject = new FastDB();
    }

    @Test
    void canStartPGContainer() {
        subject.start();
        assertTrue(false, "to be implemented");
    }
}