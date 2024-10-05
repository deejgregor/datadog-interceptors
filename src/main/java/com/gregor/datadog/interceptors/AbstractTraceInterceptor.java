package com.gregor.datadog.interceptors;

import datadog.trace.api.interceptor.MutableSpan;
import datadog.trace.api.interceptor.TraceInterceptor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class AbstractTraceInterceptor implements TraceInterceptor {
    public static final String[] TAGS_EVALUATED = {
            "error.type",
            "error.message",
            "error.stack",
            "operation_name",
            "resource_name",
            "type"
    };
    private final int priority;

    /** Key/value map of span tags and regex patterns */
    private final Map<String, Pattern> patterns = new HashMap<>();

    public AbstractTraceInterceptor(int priority) {
        this.priority = priority;

        configurePatterns();
    }

    protected boolean spanMatchesPatterns(MutableSpan span) {
        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            final Object tagValue;
            switch (entry.getKey()) {
                case "operation_name":
                    tagValue = span.getOperationName();
                    break;
                case "resource_name":
                    tagValue = span.getResourceName();
                    break;
                case "type":
                    tagValue = span.getSpanType();
                    break;
                default:
                    tagValue = span.getTag(entry.getKey());
                    break;
            }

            if (tagValue != null && entry.getValue().matcher(tagValue.toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    protected void configurePatterns() {
        for (String tag : TAGS_EVALUATED) {
            String pattern = getConfig(getPropertyPrefix() + tag);

            if (pattern != null) {
                patterns.put(tag, Pattern.compile(pattern));
            }
        }
    }

    protected static String getConfig(String property) {
        String value = System.getProperty(property);
        if (value != null) {
            return value;
        }

        String envName = property.toUpperCase(Locale.US).replace('.', '_');
        return System.getenv(envName);
    }

    public abstract String getPropertyPrefix();

    @Override
    public int priority() {
        return priority;
    }

}
