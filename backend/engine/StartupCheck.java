package com.polyglotmesh.backend.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupCheck.class);

    @Override
    public void run(ApplicationArguments args) {
        HelloPolyglot hello = new HelloPolyglot();
        log.info("Python says: {}", hello.runPython("print('Hello')").trim());
        log.info("JS says: {}", hello.runJs("console.log('Hello')").trim());
    }
}