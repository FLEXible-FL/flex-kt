/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [ClientConfig].
 */
class ClientConfigTest {

    @Test
    fun `simple factory should create config with default values`() {
        val config = ClientConfig.simple("http://localhost:8080")

        assertEquals("http://localhost:8080", config.baseUrl)
        assertEquals(ClientConfig.DEFAULT_CONNECTION_TIMEOUT_MS, config.connectionTimeoutMs)
        assertEquals(ClientConfig.DEFAULT_READ_TIMEOUT_MS, config.readTimeoutMs)
        assertEquals(ClientConfig.DEFAULT_WRITE_TIMEOUT_MS, config.writeTimeoutMs)
        assertEquals(ClientConfig.DEFAULT_MAX_RETRIES, config.maxRetries)
        assertEquals(ClientConfig.DEFAULT_RETRY_DELAY_MS, config.retryDelayMs)
        assertTrue(config.useExponentialBackoff)
        assertTrue(config.enableHealthCheck)
    }

    @Test
    fun `builder should create config with custom values`() {
        val config = ClientConfig.Builder()
            .baseUrl("http://server:9090")
            .connectionTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .maxRetries(5)
            .retryDelayMs(2000)
            .useExponentialBackoff(false)
            .maxRetryDelayMs(60000)
            .enableHealthCheck(false)
            .build()

        assertEquals("http://server:9090", config.baseUrl)
        assertEquals(10000L, config.connectionTimeoutMs)
        assertEquals(20000L, config.readTimeoutMs)
        assertEquals(15000L, config.writeTimeoutMs)
        assertEquals(5, config.maxRetries)
        assertEquals(2000L, config.retryDelayMs)
        assertFalse(config.useExponentialBackoff)
        assertEquals(60000L, config.maxRetryDelayMs)
        assertFalse(config.enableHealthCheck)
    }

    @Test
    fun `config should throw on blank baseUrl`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig.simple("")
        }

        assertThrows<IllegalArgumentException> {
            ClientConfig.simple("   ")
        }
    }

    @Test
    fun `config should throw on invalid connectionTimeoutMs`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig(baseUrl = "http://localhost", connectionTimeoutMs = 0)
        }

        assertThrows<IllegalArgumentException> {
            ClientConfig(baseUrl = "http://localhost", connectionTimeoutMs = -1)
        }
    }

    @Test
    fun `config should throw on invalid readTimeoutMs`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig(baseUrl = "http://localhost", readTimeoutMs = 0)
        }
    }

    @Test
    fun `config should throw on invalid writeTimeoutMs`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig(baseUrl = "http://localhost", writeTimeoutMs = -100)
        }
    }

    @Test
    fun `config should throw on negative maxRetries`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig(baseUrl = "http://localhost", maxRetries = -1)
        }
    }

    @Test
    fun `config should allow zero maxRetries`() {
        val config = ClientConfig(baseUrl = "http://localhost", maxRetries = 0)
        assertEquals(0, config.maxRetries)
    }

    @Test
    fun `config should throw on invalid retryDelayMs`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig(baseUrl = "http://localhost", retryDelayMs = 0)
        }
    }

    @Test
    fun `config should throw when maxRetryDelayMs less than retryDelayMs`() {
        assertThrows<IllegalArgumentException> {
            ClientConfig(
                baseUrl = "http://localhost",
                retryDelayMs = 5000,
                maxRetryDelayMs = 2000
            )
        }
    }

    @Test
    fun `calculateRetryDelay should return fixed delay when exponential backoff disabled`() {
        val config = ClientConfig(
            baseUrl = "http://localhost",
            retryDelayMs = 1000,
            useExponentialBackoff = false
        )

        assertEquals(1000L, config.calculateRetryDelay(0))
        assertEquals(1000L, config.calculateRetryDelay(1))
        assertEquals(1000L, config.calculateRetryDelay(5))
        assertEquals(1000L, config.calculateRetryDelay(10))
    }

    @Test
    fun `calculateRetryDelay should use exponential backoff when enabled`() {
        val config = ClientConfig(
            baseUrl = "http://localhost",
            retryDelayMs = 1000,
            useExponentialBackoff = true,
            maxRetryDelayMs = 30000
        )

        assertEquals(1000L, config.calculateRetryDelay(0))   // 1000 * 2^0 = 1000
        assertEquals(2000L, config.calculateRetryDelay(1))   // 1000 * 2^1 = 2000
        assertEquals(4000L, config.calculateRetryDelay(2))   // 1000 * 2^2 = 4000
        assertEquals(8000L, config.calculateRetryDelay(3))   // 1000 * 2^3 = 8000
        assertEquals(16000L, config.calculateRetryDelay(4))  // 1000 * 2^4 = 16000
        assertEquals(30000L, config.calculateRetryDelay(5))  // 1000 * 2^5 = 32000, but capped at 30000
        assertEquals(30000L, config.calculateRetryDelay(10)) // Should always cap at maxRetryDelayMs
    }

    @Test
    fun `data class should implement equals correctly`() {
        val config1 = ClientConfig.simple("http://localhost:8080")
        val config2 = ClientConfig.simple("http://localhost:8080")
        val config3 = ClientConfig.simple("http://localhost:9090")

        assertEquals(config1, config2)
        assertFalse(config1 == config3)
    }

    @Test
    fun `copy should work correctly`() {
        val original = ClientConfig.simple("http://localhost:8080")
        val copied = original.copy(maxRetries = 10)

        assertEquals("http://localhost:8080", copied.baseUrl)
        assertEquals(10, copied.maxRetries)
        assertEquals(ClientConfig.DEFAULT_CONNECTION_TIMEOUT_MS, copied.connectionTimeoutMs)
    }
}

