package com.polyglotmesh.backend.engine;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;

/**
 * Day 2: the first polyglot class. Runs a Python and a JavaScript snippet
 * inside the same JVM using GraalVM contexts and returns what they printed.
 */
public class HelloPolyglot {

    public String runPython(String code) {
        return run("python", code);
    }

    public String runJs(String code) {
        return run("js", code);
    }

    private String run(String language, String code) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Context context = Context.newBuilder(language)
                .out(out)
                .err(out)
                .build()) {
            context.eval(language, code);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}