# RED metrics (the hard way)

A guide to generating RED metrics without auto-instrumentation.

## Background

With just a few keystrokes, users can add a java instrumentation agent to their
JVM runtime and get metrics, traces, and logs from their application.
Generally, traces alone contain enough information to create (R)ate, (E)rror, and (D)uration 
or RED metrics. Observability tools, like Splunk Observability Cloud, will 
show RED metrics based on tracing data for a service.

But what if you're overambitious and like doing extra work? What if you are obsessive
and crave total control and freedom over how your RED metrics get created?

What if you just want to do it the hard way? Can we do it with just manual instrumentation?

## Let's try

Ok, first, it's not that hard. But be warned, the approach described here
is probably not the best solution for most users or most applciations. By doing
something like this, you'll be missing out on a LOT of features and a pile of telemetry
from a fully instrumented application. You probably shouldn't do it this way.

## Collector

First, let's get our collector set up. We're going to run the official OpenTelemetry 
collector contrib image, so you'll need docker. There are three pieces to this:

1. `collector.yaml` - this is the config for the collector. It's configured to receive
   OTLP and to export metrics to Splunk us0 realm. It sources an ingest token
   from the environment and sends it in the `X-SF-TOKEN` http header. That's it.
2. `collector.sh` - script that runs docker, passes the config and environment.
3. You'll need an ingest token ([docs here](https://docs.splunk.com/observability/en/admin/authentication/authentication-tokens/org-tokens.html)).

With these 3 things in hand, it's simple to start the collector:

```
SPLUNK_ACCESS_TOKEN=xxxredactedxxx ./collector.sh
```

## The Application

The `Application` class is just the entrypoint that initializes our OpenTelemetry
SDK and starts the HTTP server. The `Application`'s main method configures
4 things before launching the http server:

1. `otel.exporter.otlp.protocol` - used to set the export protocol to `http/protobuf`, because the default is gRPC.
2. `otel.metric.export.interval` - used to set the metric interval to 10s instead of the default of 1m.
3. `otel.resource.attributes` - used here to set the `deployment.environment` resource attribute to `red-metrics`.
4. `otel.service.name` - used to set the service name to `red-metrics`.

These configuration items can also be set more programmatically, but we find it convenient to 
just set the system properties, because we are using SDK auto-configuration. These could have also been
passed to the JVM via the commandline.

The otel sdk instance is then passed to the `HttpServer`.

## HttpServer

The `HttpServer` 
([link](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java)) 
is a very simple single-endpoint http server written using 
[SparkJava](https://github.com/perwendel/spark). 
The root endpoint ("`/`") is written to have both a controlled error rate and a set of ranomly determined,
fixed durations. See the `perRequestTweak()` 
([line 91](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L91))
for details on how this is implemented.

The `HttpServer` is created with two instruments:

1. `LongCounter requestCounter` - A counter that is incremented with every request
2. `DoubleHistogram durationHistogram` - A histogram that tracks the quantized duration of every request.

The `requestCounter` is incremented every time a request has been handled 
(lines [79](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L79) and 
[82](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L82)).

The duration of each request is recorded in the `durationHistogram` 
(line [70](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L79)).
This is done by temporarily storing the start time of the request and then computing the duration
when the request is completed. This is accomplished by wrapping the basic route with a timed route. It should
be noted that when doing manual instrumentation like this, care must be taken to ensure that 
_all code paths have a recording_. In this case, this is accomplished by doing calling `record()` in a `finally` 
block, thus ensuring that successes and failures are both tracked.

## The Metrics

You might be surpised to find only 2 instruments when we are trying to get 3 RED metrics (RED has 3 letters!). 
This is due to how errors are accounted for within the `requestCounter`. In accordance with the otel 
[HTTP server semantic conventions](https://github.com/open-telemetry/semantic-conventions/blob/231528e8fd564dd3d0b07ac3de65fe117442d930/docs/http/http-metrics.md#http-server), 
we set the `error.type` attribute when an error is encountered (line [82](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L82)). When this attribute is present on some
recorded data point, it creates one or more `metric dimensions`.


The following shows screens from Splunk Observability cloud, but similar results can be obtained from
Prometheus and other open source tools.

# Summary

pros/cons
api code sprinkled among user/business code. but not vendor specific, otel apis ftw.
imagine an enterprise app with hundreds of endpoints/routes
build your own o11y framework I suppose


