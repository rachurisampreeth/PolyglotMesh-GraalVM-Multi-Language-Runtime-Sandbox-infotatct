package com.polyglotmesh;

import com.polyglotmesh.api.Models.ExecuteRequest;
import com.polyglotmesh.data.MongoMock;
import com.polyglotmesh.runtime.PolyglotRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PolyglotRunnerTest {
  PolyglotRunner runner = new PolyglotRunner(new MongoMock(), 30000, 100000, 1024, 16000, 2, 4);
    private ExecuteRequest single(String lang, String code) {
        return new ExecuteRequest("single", lang, code, null, null, Map.of());
    }
    @Test void pythonPrint() {
        var out = runner.execute(single("python", "print('Hello')\nresult = 2 + 3"));
        assertEquals("OK", out.status()); assertTrue(out.output().contains("Hello"));
        assertEquals(5.0, Double.parseDouble(String.valueOf(out.result())), 0.01);
    }
    @Test void javascriptPrint() {
        var out = runner.execute(single("javascript", "print('Hi JS'); result = 3 * 7;"));
        assertEquals("OK", out.status()); assertEquals(21.0, Double.parseDouble(String.valueOf(out.result())), 0.01);
    }
    @Test void auditWritesBackSameMap() {
        var out = runner.execute(new ExecuteRequest("audit", "python", "", null, null, Map.of()));
        assertEquals("OK", out.status());
        Map<?, ?> result = (Map<?, ?>) out.result();
        assertEquals(1500.0, Double.parseDouble(String.valueOf(result.get("javaReadback"))), 0.01);
    }
    @Test void outputSizeIsCapped() {
        var out = runner.execute(single("python", "print('x' * 5000)"));
        assertEquals("OK", out.status());
        assertTrue(out.output().length() < 1200);
        assertTrue(out.output().contains("[output truncated]"));
    }
    @Test void filesystemIsBlocked() {
        var out = runner.execute(single("python", "open('private.txt', 'w').write('no')"));
        assertNotEquals("OK", out.status());
    }
    @Test void infiniteLoopStops() {
        var out = runner.execute(single("javascript", "while (true) {}"));
        assertTrue(out.status().equals("TIMEOUT") || out.status().equals("SANDBOX_VIOLATION"));
    }
    @Test void normalCodeAfterInfiniteLoopStillWorks() {
        runner.execute(single("javascript", "while (true) {}"));
        assertEquals("OK", runner.execute(single("javascript", "result = 4;")).status());
    }
    @Test void pipelineDoesJavaPythonJavaScript() {
        var out = runner.execute(new ExecuteRequest("pipeline", null, null, null, null, Map.of("productId", "p2", "quantity", 2, "discountPercent", 10)));
        assertEquals("OK", out.status()); assertEquals(2, out.steps().size());
    }
}
