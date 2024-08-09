package com.splunk.example;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

public class Application {

    public static void main(String[] args) {
        OpenTelemetry otel = AutoConfiguredOpenTelemetrySdk.builder()
                .build().getOpenTelemetrySdk();
        new Server(otel).run();
    }
}
