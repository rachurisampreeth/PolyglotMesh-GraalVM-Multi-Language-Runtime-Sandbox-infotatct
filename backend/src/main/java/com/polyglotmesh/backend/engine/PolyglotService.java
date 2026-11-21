package com.polyglotmesh.backend.engine;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.springframework.stereotype.Service;

/**
 * Day 3: runs Python or JavaScript inside the JVM and captures what the script prints.
 * Sandbox limits (Day 4+) and a shared Engine (Day 7) come later.
 */
@Service
public class PolyglotService {

    private static final Set<String> SUPPORTED = Set.of("python", "js");

    public ExecutionResult execute(String language, String code) {
        if (language == null || !SUPPORTED.contains(language)) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Code must not be empty");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long start = System.nanoTime();

        try (Context context = Context.newBuilder(language)
                .out(out)
                .err(out)
                .build()) {
            context.eval(language, code);
            return result("OK", out, null, null, start);
        } catch (PolyglotException e) {
            return result("ERROR", out, e.getMessage(), stackTraceOf(e), start);
        }
    }

    private ExecutionResult result(String status, ByteArrayOutputStream out,
                                   String error, String stackTrace, long startNanos) {
        double ms = (System.nanoTime() - startNanos) / 1_000_000.0;
        return new ExecutionResult(status, out.toString(StandardCharsets.UTF_8), error, stackTrace, ms);
    }

    private String stackTraceOf(PolyglotException e) {
        StringBuilder sb = new StringBuilder();
        for (PolyglotException.StackFrame frame : e.getPolyglotStackTrace()) {
            sb.append(frame).append('\n');
        }
        return sb.toString().trim();
    }
}