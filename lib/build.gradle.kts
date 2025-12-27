/*
 * Flex-KT: A Kotlin library for federated learning clients.
 *
 * This library provides a robust, coroutine-based client implementation
 * for federated learning systems, designed to be Android-compatible.
 */

plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    id("com.squareup.wire") version "5.4.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

wire {
    sourcePath {
        srcDir("src/main/proto")
        include("org/flex/tensor.proto")
        include("org/flex/transport.proto")
    }
    kotlin {
        rpcCallStyle = "suspending"
    }
}

val coroutinesVersion = "1.7.3"

dependencies {
    // Kotlin Coroutines - Android compatible
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

    // Use the Kotlin Test integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockk for mocking in tests
    testImplementation("io.mockk:mockk:1.13.8")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(libs.commons.math3)

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(libs.guava)

    // Wire gRPC client
    implementation("com.squareup.wire:wire-runtime:5.4.0")
    implementation("com.squareup.wire:wire-grpc-client:5.4.0")
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
