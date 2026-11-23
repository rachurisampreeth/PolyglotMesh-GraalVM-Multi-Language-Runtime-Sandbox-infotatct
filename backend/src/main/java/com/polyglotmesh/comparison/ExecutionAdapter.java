package com.polyglotmesh.comparison;

import com.polyglotmesh.api.ApiModels.ExecuteRequest;

/**
 * A single invocation to be timed by ComparisonService.
 */
public interface ExecutionAdapter {

    String name();

    String measurementType();

    String note();

    void invoke(ExecuteRequest request);
}