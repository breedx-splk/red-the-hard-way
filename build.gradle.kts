plugins {
    application
}

repositories {
    mavenCentral()
}

val otelVersion = "1.41.0"

application {
    mainClass.set("com.splunk.example.Application")
    applicationDefaultJvmArgs = listOf(
        "-Dotel.service.name=red-the-hard-way"
    )
}

dependencies {
    implementation("com.sparkjava:spark-core:2.9.4")
    implementation("io.opentelemetry:opentelemetry-api:${otelVersion}")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:${otelVersion}")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:${otelVersion}")
}