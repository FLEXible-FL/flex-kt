/*
 * Flex-KT: A Kotlin library for federated learning clients.
 *
 * This library provides a robust, coroutine-based client implementation
 * for federated learning systems, designed to be Android-compatible.
 */

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)

    id("com.squareup.wire") version "5.4.0"

    // JaCoCo for code coverage
    jacoco

    `maven-publish`
}

android {
    namespace = "org.flex"
    compileSdk = 35

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
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
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")

    // Use the Kotlin Test integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the JUnit 5 integration.
    testImplementation(libs.junit.jupiter.engine)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Mockk for mocking in tests
    testImplementation("io.mockk:mockk:1.13.8")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api(libs.commons.math)

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation(libs.guava)

    // Wire gRPC client
    implementation("com.squareup.wire:wire-runtime:5.4.0")
    implementation("com.squareup.wire:wire-grpc-client:5.4.0")
}

// Configure JaCoCo code coverage
jacoco {
    toolVersion = "0.8.10"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("testDebugUnitTest"))

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug")
    val mainSrc = "src/main/kotlin"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(debugTree)
    executionData.setFrom(fileTree(layout.buildDirectory).include("/jacoco/*.exec"))
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "org.flex"
            artifactId = "flexible"
            version = "0.0.1"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        mavenLocal()
    }
}
