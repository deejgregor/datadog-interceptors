package com.gregor.datadog.interceptors;

import datadog.trace.api.interceptor.MutableSpan;

import java.util.Collection;

public class PromoteErrorInterceptor extends AbstractTraceInterceptor {
    // This list is chosen to match what is needed for Error Tracking
    // https://docs.datadoghq.com/tracing/error_tracking/#use-span-tags-to-track-error-spans
    private final static String[] PROMOTE_ERROR_TAGS = {
            "error.stack",
            "error.message",
            "error.type"
    };
    private boolean promoteErrorTagsEnable = false;

    public PromoteErrorInterceptor(int priority) {
        super(priority);
        if ("true".equals(getConfig("dd.error.promote.error.tags.enable"))) {
            promoteErrorTagsEnable = true;
        }
    }
    @Override
    public String getPropertyPrefix() {
        return "dd.error.promote.pattern.";
    }

    @Override
    public Collection<? extends MutableSpan> onTraceComplete(
            Collection<? extends MutableSpan> trace) {
        for (final MutableSpan span : trace) {
            if (span.isError() && spanMatchesPatterns(span)) {
                span.getLocalRootSpan().setError(true);
                span.getLocalRootSpan().setTag("error.promoted.from.operation", span.getOperationName().toString());
                span.getLocalRootSpan().setTag("error.promoted.from.resource_name", span.getResourceName().toString());
                if (promoteErrorTagsEnable) {
                    for (String errorTag : PROMOTE_ERROR_TAGS) {
                        final Object value = span.getTag(errorTag);
                        if (value != null) {
                            span.getLocalRootSpan().setTag(errorTag, value.toString());
                        }
                    }
                }
            }
        }

        return trace;
    }
}
