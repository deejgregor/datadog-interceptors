package com.gregor.datadog.interceptors;

import java.util.Collection;

import datadog.trace.api.interceptor.MutableSpan;

class SpanSetInterceptor extends AbstractTraceInterceptor {
    private final String resourceNameTag;
    private final String operationNameTag;
    private final String spanTypeTag;

    public SpanSetInterceptor(int priority) {
        super(priority);

        resourceNameTag = getConfig("dd.span.set.resource_name.tag");
        operationNameTag = getConfig("dd.span.set.operation_name.tag");
        spanTypeTag = getConfig("dd.span.set.span_type.tag");
    }
    @Override
    public String getPropertyPrefix() {
        return "dd.span.set.pattern.";
    }

    @Override
    public Collection<? extends MutableSpan> onTraceComplete(
            Collection<? extends MutableSpan> trace) {
        for (final MutableSpan span : trace) {
            if (spanMatchesPatterns(span)) {
                if (resourceNameTag != null) {
                    final Object value = span.getTag(resourceNameTag);
                    if (value != null) {
                        span.setResourceName(value.toString());
                    }
                }
                if (operationNameTag != null) {
                    final Object value = span.getTag(operationNameTag);
                    if (value != null) {
                        span.setOperationName(value.toString());
                    }
                }
                if (spanTypeTag != null) {
                    final Object value = span.getTag(spanTypeTag);
                    if (value != null) {
                        span.setSpanType(value.toString());
                    }
                }
            }
        }

        return trace;
    }
}