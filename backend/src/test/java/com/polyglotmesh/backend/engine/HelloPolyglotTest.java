package com.polyglotmesh.backend.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HelloPolyglotTest {

    private final HelloPolyglot hello = new HelloPolyglot();

    @Test
    void pythonPrintsHello() {
        assertEquals("Hello", hello.runPython("print('Hello')").trim());
    }

    @Test
    void javascriptPrintsHello() {
        assertEquals("Hello", hello.runJs("console.log('Hello')").trim());
    }
}