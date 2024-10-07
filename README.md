# About

This is a set of [TraceInterceptors](https://docs.datadoghq.com/tracing/trace_collection/custom_instrumentation/java/dd-api/#extending-tracers)
for Datadog's [Java APM tracer](https://docs.datadoghq.com/tracing/trace_collection/automatic_instrumentation/dd_libraries/java/?tab=wget).

This group of interceptors is intended to help manage errors in automatically instrumented services where it is
non-trivial to change the application code. They are intended to help in these use cases:

1. Unset the error status on spans that aren't considered errors from the application's perspective. Useful when automatic instrumentation marks a span as an error in situations where it isn't treated as an error from an application perspective. Example: a downstream service returns an HTTP 500 code for something that isn't considered an error.
2. Set the error status on spans where automatic instrumentation didn't detect an error. This is helpful when an automatically instrumented service receives an HTTP 200 response, but the HTTP body contains more detailed status information indicating an error, such as GraphQL errors. If the existence of the error is available in tags (including onces created by [Dynamic Instrumentation](https://docs.datadoghq.com/dynamic_instrumentation/)),  then this feature can be used to set the error status.
3. Promote an error status from a deeper span to the Service Entry Span (aka LocalRootSpan). This helps to make errors visible on the Service page which usually shows the Service Entry Span by default and also [Error Tracking](https://docs.datadoghq.com/tracing/error_tracking/#use-span-tags-to-track-error-spans), which only looks at the Service Entry Span.

Currently, there are three interceptors that help for the above three use cases:

* UnsetErrorInterceptor - unset the error status on matching spans
* SetErrorInterceptor - set the error status on matching spans, optionally remapping arbitrary tags
* PromoteErrorInterceptor - promote an error status to the Service Entry Span, optionally also promoting error tags (to enable Error Tracking).

Each is configured by regex patterns that are used to match against span tag values.

# THIS IS NOT PRODUCTION READY

This is an initial release, with no unit tests, and is not hardened against configuration mistakes.
This is intended as a proof-of-concept do direct future work.

# Installation

1. Download the `datadog-interceptors.jar` file.
2. Copy `datadog-interceptors.jar` to your system.
3. Add `-javaagent:<directory-with-jar-file>/datadog-interceptors.jar` to your command line *after* `-javaagent:<path-to-dd-directory>/dd-java-agent.jar`.
4. Configure using the options mentioned below.

Note: if you get the exception below, it is most likely that you haven't loaded `dd-java-agent.jar` *before* `datadog-interceptors.jar` on your command-line.
Double-check your ordering make sure that `datadog-interceptors.jar` comes *later*.

```
Exception in thread "main" java.lang.NoClassDefFoundError: datadog/trace/api/interceptor/TraceInterceptor

```

# Configuration

Configuration is controlled through system properties and/or environment variables, with a preference given to system
properties.
The naming scheme follows the Datadog naming scheme, where environment variable names are uppercased system property
names with periods replaced with underscores.

## Tag value matching

Currently, only a hard-coded list of tags is available for matching. This is for simplicity in the initial release,
but is expected to be extensible in the future. These are the tags (and other span data elements) that can be matched on today:

* `error.type`
* `error.message`
* `error.stack`
* `operation_name`
* `resource_name`
* `type`

The system properties for configuring the tag matching patterns for the three interceptors start with these prefixes:

* UnsetErrorInterceptor - `dd.error.unset.pattern.`
* SetErrorInterceptor - `dd.error.set.pattern.`
* PromoteErrorInterceptor - `dd.error.promote.pattern.`

The configured value is used as a Java regular expression
[Pattern](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html) and is matched against the tag value
using [Matcher.matches](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html#matches--).
Note that the **entire** tag value must match the expression, so build your pattern accordingly.

### Regular expression tips

1. The regular expression pattern must match the *entire* tag value. Use `.*?` at the beginning and/or end as needed. Note: for performance, it's important to use `.*?` instead of `.*` if it is used multiple times ([details on why](https://www.google.com/search?q=regex+backtracing+performance)).
2. Make sure to quote the regular expression so special characters won't be interpreted by the shell, etc..
3. If the tag value might have embedded newlines (such as a stack trace), use `(?s)` at the beginning of the expression so that `.*?` will match newlines.
4. If you want to match on the character `.`, that is a special character in regular expressions, so make sure to escape it, like `\.`, e.g.: `servlet\.request`.

## Other configuration

* `dd.error.promote.error.tags.enable` - set this to `true` to promote the standard error tags to the Service Entry Span. This is to help enable Error Tracking.
* `dd.error.set.mapping` - this is a comman-separated key=value list of tags to remap when the error status is set to true. This is useful for mapping custom tags to standard error tags to help enable Error Tracking).


## Configuration examples
To unset the error status when the `error.stack` contains `404 Not Found`:

```
dd.error.unset.pattern.error.stack='(?s).*?404 Not Found.*?'

```

To promote errors from `jax-ws.request` spans to the service entry span and also propagate standard error tags:

```
dd.error.promote.pattern.operation_name='jax-ws\.request'
dd.error.promote.error.tags.enable=true
```

To set the error status when we see a specific method called. Also remap custom tags to standard error tags:

```
dd.error.set.pattern.resource_name='MyErrorHandlingClass.somethingIsBroken'
dd.error.set.mapping=error.message=custom_error_tag,error.stack=custom_error_details,error.type=custom_error_type
```
