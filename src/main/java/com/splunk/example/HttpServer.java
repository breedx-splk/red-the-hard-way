package com.splunk.example;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import spark.Route;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static spark.Spark.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A simple http server that has a few characteristics:
 *
 * * It attempts to maintain a 15% error rate
 * * It attempts to keep a pretty even distribution .
 */
class HttpServer {

    static final int PORT = 1974;

    // These are defined here: https://opentelemetry.io/docs/specs/semconv/http/http-metrics/#metric-httpserverrequestduration
    static final List<Double> DURATION_BUCKETS = List.of(
            0.005, 0.01, 0.025, 0.05, 0.075, 0.1,
            0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0
    );
    static final Attributes GENERIC_ERROR_ATTRS = Attributes.of(stringKey("error.type"), "500");
    private static final double NS_PER_S = 1000000000.0;
    private final Random rand = new Random();
    private final LongCounter requestCounter;
    private final DoubleHistogram durationHistogram;

    HttpServer(LongCounter requestCounter, DoubleHistogram durationHistogram) {
        this.requestCounter = requestCounter;
        this.durationHistogram = durationHistogram;
    }

    static HttpServer create(OpenTelemetry otel) {
        Meter meter = otel.getMeter("red-metrics");
        LongCounter requestCounter = meter
                .counterBuilder("http.server.request.count") // not in semconv!
                .build();
        DoubleHistogram durationHistogram = meter
                .histogramBuilder("http.server.request.duration") // is in semconv
                .setExplicitBucketBoundariesAdvice(DURATION_BUCKETS)
                .build();
        return new HttpServer(requestCounter, durationHistogram);
    }

    void run(){
        port(PORT);

        get("/", getTimedRoute(getRoute()));
        System.out.println("HTTP server running on port " + PORT);
    }

    private Route getTimedRoute(Route route){
        return (request, response) -> {
            Instant start = Instant.now();
            try {
                return route.handle(request, response);
            } finally {
                double duration = Duration.between(start, Instant.now()).toNanos() / NS_PER_S;
                durationHistogram.record(duration);
            }
        };
    }

    private Route getRoute() {
        return (request, response) -> {
            try {
                perRequestTweak();
                requestCounter.add(1);
                return "good hello.";
            } catch (Exception e) {
                requestCounter.add(1, GENERIC_ERROR_ATTRS);
                throw e;
            }
        };
    }

    /**
     * Helps us to maintain an error rate and response time distribution.
     */
    void perRequestTweak(){
        // 15% chance of throwing an exception
        if(rand.nextInt(100) < 15) {
            throw new RuntimeException("KABOOM!");
        }
        // Our latency will be one of 0, 100, 200, 300, or 400ms.
        int latency = rand.nextInt(5) * 100;
        try {
            TimeUnit.MILLISECONDS.sleep(latency);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
