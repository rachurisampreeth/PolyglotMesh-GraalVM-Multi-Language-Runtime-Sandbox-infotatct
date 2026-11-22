package com.polyglotmesh;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;
import com.polyglotmesh.service.PolyglotExecutionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Integration tests: require GraalPy/GraalJS Maven dependencies to be installed. */
class PolyglotExecutionServiceTest {
    private static PolyglotExecutionService service;

    @BeforeAll
    static void setUp() { service = new PolyglotExecutionService(60000, 16000, 32768); }
    @AfterAll
    static void tearDown() { service.shutdown(); }

    @Test
    void executesJavaScriptInJVM() {
        var response = service.execute(new ExecuteRequest("single", "javascript",
            "result = 6 * 7; print('JS works');", "", "", Map.of()));
        assertEquals("success", response.status(), response.error());
        assertEquals("42", response.result());
        assertTrue(response.output().contains("JS works"));
    }

    @Test
    void executesPythonInJVM() {
        var response = service.execute(new ExecuteRequest("single", "python",
            "result = 5 + 8\nprint('Python works')", "", "", Map.of()));
        assertEquals("success", response.status(), response.error());
        assertEquals("13", response.result());
        assertTrue(response.output().contains("Python works"));
    }

    @Test
    void bridgesJavaToPythonToJavaScript() {
        var response = service.execute(new ExecuteRequest("pipeline", "python", "",
            "result = input_data['price'] * input_data['quantity']\nprint('Subtotal:', result)",
            "result = 'approved:' + python_result; print('JS:', result);",
            Map.of("price", 35, "quantity", 3)));
        assertEquals("success", response.status(), response.error());
        assertEquals("approved:105", response.result());
        assertTrue(response.output().contains("Subtotal:"));
        assertEquals(3, response.steps().size());
    }

    @Test
    void deniesHostClassLookup() {
        var response = service.execute(new ExecuteRequest("single", "javascript",
            "result = Java.type('java.lang.System').getProperty('user.home');", "", "", Map.of()));
        assertEquals("error", response.status(), "Host class lookup should not be available");
        assertFalse(response.error().isBlank());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
            () -> service.execute(new ExecuteRequest("single", "python", "", "", "", Map.of())));
    }
}
