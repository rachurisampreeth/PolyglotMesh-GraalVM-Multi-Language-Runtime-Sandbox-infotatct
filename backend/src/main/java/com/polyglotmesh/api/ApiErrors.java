package com.polyglotmesh.api;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> badRequest(IllegalArgumentException ex) { return Map.of("error", ex.getMessage()); }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String,String> notFound(NoSuchElementException ex) { return Map.of("error", ex.getMessage()); }

    @ExceptionHandler(RejectedExecutionException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String,String> tooBusy(RejectedExecutionException ex) { return Map.of("error", "Execution queue full. Try again shortly."); }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> validation(WebExchangeBindException ex) { return Map.of("error", "Invalid snippet fields."); }
}
