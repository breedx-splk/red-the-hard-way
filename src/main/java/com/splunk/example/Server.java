package com.splunk.example;
import io.opentelemetry.api.OpenTelemetry;
import static spark.Spark.*;

import java.util.Random;

public class Server {

    private final Random rand = new Random();
    private final OpenTelemetry otel;

    public Server(OpenTelemetry otel) {
        this.otel = otel;
    }

    void run(){
        get("/hello", (request, response) -> {
            return "Hello World!";
        });
    }

}
