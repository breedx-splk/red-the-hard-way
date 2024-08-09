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


