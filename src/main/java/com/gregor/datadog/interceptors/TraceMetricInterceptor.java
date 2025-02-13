package com.gregor.datadog.interceptors;

import datadog.trace.api.interceptor.MutableSpan;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TraceMetricInterceptor extends AbstractTraceInterceptor {
    private final String[] tags;
    private final StatsDClient statsd;
    public TraceMetricInterceptor(int priority) {
        super(priority);

        statsd = new NonBlockingStatsDClientBuilder()
                .prefix("custom")
                .build();

        if (getConfig("dd.trace.metric.tags") != null) {
            tags = getConfig("dd.trace.metric.tags").split(",");
        } else {
            tags = new String[] {};
        }

    }

    @Override
    public String getPropertyPrefix() {
        return "dd.trace.metric.pattern.";
    }

    @Override
    public Collection<? extends MutableSpan> onTraceComplete(Collection<? extends MutableSpan> trace) {
        for (final MutableSpan span : trace) {
            if (spanMatchesPatterns(span)) {
                List<String> strings = new ArrayList<>();
                strings.add("resource_name:" + span.getResourceName().toString());
                for (String t : tags) {
                    if (span.getTag(t) != null) {
                        strings.add(t + ":" + span.getTag(t).toString());
                    }
                }

                String[] metricTags = strings.toArray(new String[]{});
                statsd.distribution(getMetricName(span), span.getDurationNano() * 1000000000, metricTags);
                statsd.incrementCounter(getMetricName(span, "hits"), metricTags);
                if (span.isError()) {
                    statsd.incrementCounter(getMetricName(span, "errors"), metricTags);
                }
            }
        }

        return trace;
    }

    private static String getMetricName(MutableSpan span, String suffix) {
        return "trace." + span.getOperationName() + "." + suffix;
    }
    private static String getMetricName(MutableSpan span) {
        return "trace." + span.getOperationName();
    }
}
