package com.github.lkq.instadb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InstaDBTest {
    private InstaDB subject;

    @BeforeEach
    void setUp() {
        subject = new InstaDB();
    }

    @Tag("integration")
    @Test
    void canStartPGContainer() {
        subject.start();
        assertTrue(false, "to be implemented");
    }
}