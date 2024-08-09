package com.splunk.example;
import io.opentelemetry.api.OpenTelemetry;
import static spark.Spark.*;

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
    private final Random rand = new Random();
    private final OpenTelemetry otel;

    HttpServer(OpenTelemetry otel) {
        this.otel = otel;
    }

    void run(){
        port(PORT);

        get("/", (request, response) -> {
            perRequestTweak();
            return "good hello.";
        });
        System.out.println("HTTP server running on port " + PORT);
    }

    /**
     * Helps us to maintain an error rate and response time distribution
     */
    void perRequestTweak(){
        if(rand.nextInt(100) < 15){
            throw new RuntimeException("KABOOM!");
        }
        int latency = rand.nextInt(5) * 100;
        try {
            TimeUnit.MILLISECONDS.sleep(latency);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
