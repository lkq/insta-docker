package com.github.lkq.instadb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstaDBTest {
    private InstaDB subject;

    @BeforeEach
    void setUp() {
        subject = new InstaDB();
    }

    @Test
    void canStartPGContainer() {
        subject.start();
    }
}