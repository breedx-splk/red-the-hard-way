package com.splunk.example;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

import java.time.Duration;

public class Application {

    public static void main(String[] args) {
        PeriodicMetricReader metricReader = PeriodicMetricReader
                .builder(OtlpHttpMetricExporter.getDefault())
                // Set interval to 10 seconds for quick demo harvest
                .setInterval(Duration.ofSeconds(10))
                .build();
        OpenTelemetry otel = AutoConfiguredOpenTelemetrySdk.builder()
                .addMetricReaderCustomizer((reader, config) -> metricReader)
                .build()
                .getOpenTelemetrySdk();
        new HttpServer(otel).run();
    }
}
