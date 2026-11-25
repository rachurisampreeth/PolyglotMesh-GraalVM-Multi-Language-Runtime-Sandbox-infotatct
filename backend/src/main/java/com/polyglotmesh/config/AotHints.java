package com.polyglotmesh.config;

import com.polyglotmesh.api.Models;
import com.polyglotmesh.model.Snippet;
import com.polyglotmesh.model.ExecutionLog;
import com.polyglotmesh.model.ComparisonLog;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(AotHints.Hints.class)
public class AotHints {
    public static class Hints implements RuntimeHintsRegistrar {
        @Override public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            for (Class<?> type : new Class<?>[]{Snippet.class, ExecutionLog.class, ComparisonLog.class,
                Models.ExecuteRequest.class, Models.ExecuteResponse.class, Models.ExecutionStep.class,
                Models.SnippetRequest.class, Models.PricingRequest.class, Models.ComparisonRequest.class,
                Models.ComparisonResponse.class, Models.TimingStats.class, Models.DataQuery.class}) {
                hints.reflection().registerType(type, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.DECLARED_FIELDS);
            }
        }
    }
}
