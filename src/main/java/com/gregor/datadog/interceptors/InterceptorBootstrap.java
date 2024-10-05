package com.gregor.datadog.interceptors;

import datadog.trace.api.*;

@SuppressWarnings("unused")
public final class InterceptorBootstrap {
    @SuppressWarnings("unused")
    public static void premain(String agentArgs) {
        GlobalTracer.get().addTraceInterceptor(new UnsetErrorInterceptor(100));
        GlobalTracer.get().addTraceInterceptor(new TagMatchingErrorInterceptor(101));

        // Promote comes last, so it can act on any adjustments to the error status made earlier
        GlobalTracer.get().addTraceInterceptor(new PromoteErrorInterceptor(102));
    }
}