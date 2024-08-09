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

What if you just want to do it the hard way? Can we do it?

## Let's try

Ok, first, it's not that hard. But be warned, the approach described here
is probably not the best solution for most users or most applciations. By doing
something like this, you'll be missing out on a LOT of features and a pile of telemetry
from a fully instrumented application. You probably shouldn't do it this way.

### Collector

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

The otel sdk instance is then passed to the `HttpServer`.

## HttpServer

The `HttpServer` 
([link](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java)) 
is a very simple single-endpoint http server written using 
[SparkJava](https://github.com/perwendel/spark).

The first thing we'll do is to build our Application scaffold and initialize
our OpenTelemetry SDK.

Next, we need to build a little test server, which we've implemented in the
`HttpServer` class 
([link](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java)).


