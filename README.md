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
   from the environment and sends it in the `X-SF-TOKEN` http header.
   You won't need to edit this file.
3. `collector.sh` - script that runs docker, passes the config and environment.
   You won't need to edit this file.
5. You'll need an ingest token ([docs here](https://docs.splunk.com/observability/en/admin/authentication/authentication-tokens/org-tokens.html)).

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
(line [70](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L70)).
This is done by temporarily storing the start time of the request and then computing the duration
when the request is completed. This is accomplished by wrapping the basic route with a timed route. 
Observe that the raw measured duration is passed to the histogram, and that no quantization is 
performed in user code (this is a built-in feature of the histogram). In keeping with the 
[spec](https://github.com/open-telemetry/semantic-conventions/blob/231528e8fd564dd3d0b07ac3de65fe117442d930/docs/http/http-metrics.md#http-server), 
the `durationHistogram` is created with a set of predetermined bucket boundaries.

It should be noted that when doing manual instrumentation like this, care must be taken to ensure that 
_all code paths have a recording_. In this case, this is accomplished by doing calling `record()` in a `finally` 
block, thus ensuring that successes and failures are both tracked.

## The Metrics

You might be surpised to find only 2 instruments when we are trying to get 3 RED metrics (RED has 3 letters!). 
This is due to how errors are accounted for within the `requestCounter`. In accordance with the otel 
[HTTP server semantic conventions](https://github.com/open-telemetry/semantic-conventions/blob/231528e8fd564dd3d0b07ac3de65fe117442d930/docs/http/http-metrics.md#http-server), 
we set the `error.type` attribute when an error is encountered (line [82](https://github.com/breedx-splk/red-the-hard-way/blob/main/src/main/java/com/splunk/example/HttpServer.java#L82)). When this attribute is present on some
recorded data point, it creates one or more `metric dimensions`. In other words, we are able to use a metric
with a single name to keep track of both requests, and errors.

The metrics then break down like this:

* (R)equests - `http.server.request.count` aggregated (summed) across all dimensions.
* (E)rrors - `http.server.request.count` aggregated (summed) only where `error.type` is present.
* (D)uration - `http.server.request.duration` brought in as quantized (bucketed) time ranges in a histogram.

The following shows screens from Splunk Observability Cloud, but similar results can be obtained from
Prometheus and other open source tools.

Using the dashboard builder, let's look at our `http.server.request.count` metric and filter/limit it to 
just our service, where `service.name=red-metrics`. Because we want to see requests as a _rate_, we choose
the "Rate" rollup. Immediately, we see two timeseries:

<img width="1025" alt="image" src="https://github.com/user-attachments/assets/a0dcb145-3f83-4ded-b52d-879f3219d6dd">

The upper line represents the successful requests, and the lower line represents the errors.
If we click on a datapoint to peek at the data table, we can clearly see that the time ranges are differentiated
by the `error.type` dimension:

<img width="1015" alt="image" src="https://github.com/user-attachments/assets/2dd20e0e-9fd6-46c7-9799-3f3b20f53e27">

For total requests as a rate, we can simply apply the `F(x)` "Sum" aggregation without a grouping. Once we are
happy with the results, we can save the chart to our RED metrics dashboard.

Lastly, we can look at Duration (or service latency). We once again use the chart builder, and use our histogram
metric name `http.server.request.duration` with a rollup of "mean" to get the average latency:

<img width="487" alt="image" src="https://github.com/user-attachments/assets/df0466b4-ae7c-446e-ba46-bb51d5de9223">

Unfortunately, just computing the average duration causes us to lose some subtle details tucked within in our data, 
and we could also be skewed or otherwise impacted by outliers. A common approach then is to use _percentiles_ instead
of a singular mean (average). We do this by creating a separate timeseries for each percentile we care about, 
using the "Percentile" rollup. For this exercise, we choose to look at P50, P90, P95, and P99:

<img width="1005" alt="image" src="https://github.com/user-attachments/assets/fdf5c93b-b9b0-475b-942d-6badeb45e676">

# Summary

pros/cons
api code sprinkled among user/business code. but not vendor specific, otel apis ftw.
imagine an enterprise app with hundreds of endpoints/routes
build your own o11y framework I suppose


