package com.gregor.datadog.interceptors;

import java.util.Collection;

import datadog.trace.api.interceptor.MutableSpan;

class UnsetErrorInterceptor extends AbstractTraceInterceptor {

    public UnsetErrorInterceptor(int priority) {
        super(priority);
    }
    @Override
    public String getPropertyPrefix() {
        return "dd.error.unset.pattern.";
    }

    @Override
    public Collection<? extends MutableSpan> onTraceComplete(
            Collection<? extends MutableSpan> trace) {
        for (final MutableSpan span : trace) {
            if (span.isError() && spanMatchesPatterns(span)) {
                span.setError(false);
                span.setTag("error.unset", "true");
            }
        }

        return trace;
    }
}