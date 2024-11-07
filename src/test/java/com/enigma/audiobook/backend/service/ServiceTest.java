package com.enigma.audiobook.backend.service;

import org.junit.Test;

public class ServiceTest {

    @Test
    public void serviceFirstTest() {
        System.out.println("Success");
    }

    @Test
    public void serviceSecondTest() {
        throw new RuntimeException("Testing runtime exception");
    }
}
