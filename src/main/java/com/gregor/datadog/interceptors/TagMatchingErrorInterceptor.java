package com.gregor.datadog.interceptors;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import datadog.trace.api.interceptor.MutableSpan;

class TagMatchingErrorInterceptor extends AbstractTraceInterceptor {
    // The key is the *new* tag we are going to create by copying the value from the *existing* tag in the value
    private Map<String, String> tagMapping = new HashMap<String, String>();

    public TagMatchingErrorInterceptor(int priority) {
        super(priority);

        String mappingList = getConfig("dd.error.tagmatching.mapping");
        if (mappingList != null) {
            for (String mapping : mappingList.split(",")) {
                String[] split = mappingList.split("=");
                if (split.length != 2) {
                    throw new IllegalArgumentException("mapping '" + mapping + "' must contain exactly one '='");
                }
                tagMapping.put(split[0], split[1]);
            }
        }
    }
    @Override
    public String getPropertyPrefix() {
        return "dd.error.tagmatching.pattern.";
    }

    @Override
    public Collection<? extends MutableSpan> onTraceComplete(
            Collection<? extends MutableSpan> trace) {
        for (final MutableSpan span : trace) {
            // We don't care if it's an error already, always run in case there's tag mapping to do
            if (spanMatchesPatterns(span)) {
                span.setError(true);
                span.setTag("error.tagmatching", "true");
                for (Entry<String, String> mapping : tagMapping.entrySet()) {
                    Object tagValue = span.getTag(mapping.getValue());
                    if (tagValue != null) {
                        span.setTag(mapping.getKey(), tagValue.toString());
                    }
                }
            }
        }

        return trace;
    }
}