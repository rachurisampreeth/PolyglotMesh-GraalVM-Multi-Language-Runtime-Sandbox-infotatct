package com.polyglotmesh.backend.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PolyglotServiceTest {

    private final PolyglotService service = new PolyglotService();

    @Test
    void pythonOutputIsCaptured() {
        ExecutionResult r = service.execute("python", "print(2 + 3)");
        assertEquals("OK", r.status());
        assertEquals("5", r.output().trim());
        assertNull(r.error());
        assertTrue(r.durationMs() >= 0);
    }

    @Test
    void javascriptOutputIsCaptured() {
        ExecutionResult r = service.execute("js", "console.log(2 * 3)");
        assertEquals("OK", r.status());
        assertEquals("6", r.output().trim());
    }

    @Test
    void pythonRuntimeErrorGivesErrorStatus() {
        ExecutionResult r = service.execute("python", "print(1 / 0)");
        assertEquals("ERROR", r.status());
        assertNotNull(r.error());
    }

    @Test
    void javascriptSyntaxErrorGivesErrorStatus() {
        ExecutionResult r = service.execute("js", "console.log(");
        assertEquals("ERROR", r.status());
        assertNotNull(r.error());
    }

    @Test
    void unknownLanguageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.execute("ruby", "puts 1"));
    }

    @Test
    void emptyCodeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.execute("python", "  "));
    }
}