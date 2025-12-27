/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

import java.util.concurrent.TimeUnit

/**
 * Configuration for the Flex federated learning client.
 *
 * This class provides all configurable parameters for the client's
 * network behavior, retry logic, and timeouts. Use [Builder] to
 * create instances with custom configuration.
 *
 * ## Usage Example
 * ```kotlin
 * val config = ClientConfig.Builder()
 *     .baseUrl("http://localhost:8080")
 *     .connectionTimeout(30, TimeUnit.SECONDS)
 *     .maxRetries(5)
 *     .retryDelayMs(2000)
 *     .build()
 * ```
 *
 * ## Default Values
 * - Connection timeout: 30 seconds
 * - Read timeout: 60 seconds
 * - Write timeout: 60 seconds
 * - Max retries: 3
 * - Retry delay: 1000ms
 * - Exponential backoff: enabled
 * - Max retry delay: 30 seconds
 *
 * @property baseUrl The base URL of the federated learning server.
 * @property connectionTimeoutMs Connection timeout in milliseconds.
 * @property readTimeoutMs Read timeout in milliseconds.
 * @property writeTimeoutMs Write timeout in milliseconds.
 * @property maxRetries Maximum number of retry attempts for recoverable errors.
 * @property retryDelayMs Initial delay between retry attempts in milliseconds.
 * @property useExponentialBackoff Whether to use exponential backoff for retries.
 * @property maxRetryDelayMs Maximum delay between retries when using exponential backoff.
 * @property enableHealthCheck Whether to respond to server health checks.
 *
 * @see FlexClient
 */
data class ClientConfig(
    val baseUrl: String,
    val connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS,
    val readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS,
    val writeTimeoutMs: Long = DEFAULT_WRITE_TIMEOUT_MS,
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS,
    val useExponentialBackoff: Boolean = true,
    val maxRetryDelayMs: Long = DEFAULT_MAX_RETRY_DELAY_MS,
    val enableHealthCheck: Boolean = true
) {
    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(connectionTimeoutMs > 0) { "connectionTimeoutMs must be positive" }
        require(readTimeoutMs > 0) { "readTimeoutMs must be positive" }
        require(writeTimeoutMs > 0) { "writeTimeoutMs must be positive" }
        require(maxRetries >= 0) { "maxRetries must be non-negative" }
        require(retryDelayMs > 0) { "retryDelayMs must be positive" }
        require(maxRetryDelayMs >= retryDelayMs) { "maxRetryDelayMs must be >= retryDelayMs" }
    }

    /**
     * Calculates the delay for a given retry attempt.
     *
     * @param attempt The current retry attempt (0-indexed).
     * @return The delay in milliseconds before the next retry.
     */
    fun calculateRetryDelay(attempt: Int): Long {
        return if (useExponentialBackoff) {
            minOf(retryDelayMs * (1L shl attempt), maxRetryDelayMs)
        } else {
            retryDelayMs
        }
    }

    /**
     * Builder for creating [ClientConfig] instances.
     *
     * Provides a fluent API for configuring client parameters.
     */
    class Builder {
        private var baseUrl: String = ""
        private var connectionTimeoutMs: Long = DEFAULT_CONNECTION_TIMEOUT_MS
        private var readTimeoutMs: Long = DEFAULT_READ_TIMEOUT_MS
        private var writeTimeoutMs: Long = DEFAULT_WRITE_TIMEOUT_MS
        private var maxRetries: Int = DEFAULT_MAX_RETRIES
        private var retryDelayMs: Long = DEFAULT_RETRY_DELAY_MS
        private var useExponentialBackoff: Boolean = true
        private var maxRetryDelayMs: Long = DEFAULT_MAX_RETRY_DELAY_MS
        private var enableHealthCheck: Boolean = true

        /**
         * Sets the base URL of the federated learning server.
         *
         * @param url The server URL (e.g., "http://localhost:8080").
         */
        fun baseUrl(url: String) = apply { this.baseUrl = url }

        /**
         * Sets the connection timeout.
         *
         * @param timeout The timeout value.
         * @param unit The time unit for the timeout.
         */
        fun connectionTimeout(timeout: Long, unit: TimeUnit) = apply {
            this.connectionTimeoutMs = unit.toMillis(timeout)
        }

        /**
         * Sets the read timeout.
         *
         * @param timeout The timeout value.
         * @param unit The time unit for the timeout.
         */
        fun readTimeout(timeout: Long, unit: TimeUnit) = apply {
            this.readTimeoutMs = unit.toMillis(timeout)
        }

        /**
         * Sets the write timeout.
         *
         * @param timeout The timeout value.
         * @param unit The time unit for the timeout.
         */
        fun writeTimeout(timeout: Long, unit: TimeUnit) = apply {
            this.writeTimeoutMs = unit.toMillis(timeout)
        }

        /**
         * Sets the maximum number of retry attempts.
         *
         * @param maxRetries Maximum retries (0 means no retries).
         */
        fun maxRetries(maxRetries: Int) = apply {
            this.maxRetries = maxRetries
        }

        /**
         * Sets the initial delay between retries.
         *
         * @param delayMs Delay in milliseconds.
         */
        fun retryDelayMs(delayMs: Long) = apply {
            this.retryDelayMs = delayMs
        }

        /**
         * Enables or disables exponential backoff for retries.
         *
         * @param enabled Whether to use exponential backoff.
         */
        fun useExponentialBackoff(enabled: Boolean) = apply {
            this.useExponentialBackoff = enabled
        }

        /**
         * Sets the maximum delay between retries.
         *
         * Only applicable when exponential backoff is enabled.
         *
         * @param maxDelayMs Maximum delay in milliseconds.
         */
        fun maxRetryDelayMs(maxDelayMs: Long) = apply {
            this.maxRetryDelayMs = maxDelayMs
        }

        /**
         * Enables or disables responding to health checks.
         *
         * @param enabled Whether to respond to health checks.
         */
        fun enableHealthCheck(enabled: Boolean) = apply {
            this.enableHealthCheck = enabled
        }

        /**
         * Builds the [ClientConfig] with the configured values.
         *
         * @throws IllegalArgumentException if required parameters are missing or invalid.
         */
        fun build(): ClientConfig = ClientConfig(
            baseUrl = baseUrl,
            connectionTimeoutMs = connectionTimeoutMs,
            readTimeoutMs = readTimeoutMs,
            writeTimeoutMs = writeTimeoutMs,
            maxRetries = maxRetries,
            retryDelayMs = retryDelayMs,
            useExponentialBackoff = useExponentialBackoff,
            maxRetryDelayMs = maxRetryDelayMs,
            enableHealthCheck = enableHealthCheck
        )
    }

    companion object {
        /** Default connection timeout: 30 seconds */
        const val DEFAULT_CONNECTION_TIMEOUT_MS: Long = 30_000L

        /** Default read timeout: 60 seconds */
        const val DEFAULT_READ_TIMEOUT_MS: Long = 60_000L

        /** Default write timeout: 60 seconds */
        const val DEFAULT_WRITE_TIMEOUT_MS: Long = 60_000L

        /** Default maximum retries: 3 */
        const val DEFAULT_MAX_RETRIES: Int = 3

        /** Default retry delay: 1 second */
        const val DEFAULT_RETRY_DELAY_MS: Long = 1_000L

        /** Default maximum retry delay: 30 seconds */
        const val DEFAULT_MAX_RETRY_DELAY_MS: Long = 30_000L

        /**
         * Creates a simple configuration with just the server URL.
         *
         * Uses all default values for timeouts and retry behavior.
         *
         * @param baseUrl The server URL.
         */
        fun simple(baseUrl: String): ClientConfig = ClientConfig(baseUrl = baseUrl)
    }
}

