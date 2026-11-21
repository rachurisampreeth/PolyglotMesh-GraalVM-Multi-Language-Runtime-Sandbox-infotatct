package com.polyglotmesh.backend.engine;

/**
 * Result of running one script. Field names match the API contract.
 * status is one of: OK, ERROR, TIMEOUT, SANDBOX_VIOLATION.
 */
public record ExecutionResult(
        String status,
        String output,
        String error,
        String stackTrace,
        double durationMs) {
}