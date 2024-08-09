package com.splunk.example;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

import java.time.Duration;

public class Application {

    public static void main(String[] args) {
        // Use HTTP not gRPC for export
        System.setProperty("otel.exporter.otlp.protocol", "http/protobuf");
        // Set metric reader to 10s short duration (1m is default)
        System.setProperty("otel.metric.export.interval", "10000");
        // Set deployment.environment on the resource
        System.setProperty("otel.resource.attributes", "deployment.environment=red-metrics");
        // Set the service name
        System.setProperty("otel.service.name", "red-metrics");

        // Initialize OpenTelemetry with our quick-interval metric reader
        OpenTelemetry otel = AutoConfiguredOpenTelemetrySdk.builder()
                .build()
                .getOpenTelemetrySdk();
        HttpServer.create(otel).run();
    }
}
