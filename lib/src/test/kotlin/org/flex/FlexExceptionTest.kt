/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [FlexException] and its subclasses.
 */
class FlexExceptionTest {

    @Test
    fun `ConnectionException should contain message and cause`() {
        val cause = java.io.IOException("Network error")
        val exception = ConnectionException(
            message = "Connection failed",
            cause = cause,
            isRecoverable = true
        )

        assertEquals("Connection failed", exception.message)
        assertEquals(cause, exception.cause)
        assertTrue(exception.isRecoverable)
    }

    @Test
    fun `ConnectionException should default to recoverable`() {
        val exception = ConnectionException(message = "Connection failed")

        assertTrue(exception.isRecoverable)
        assertNull(exception.cause)
    }

    @Test
    fun `ConnectionException can be non-recoverable`() {
        val exception = ConnectionException(
            message = "Auth failed",
            isRecoverable = false
        )

        assertTrue(!exception.isRecoverable)
    }

    @Test
    fun `ServerException should contain message and reason`() {
        val exception = ServerException(
            message = "Server error occurred",
            reason = "INTERNAL_ERROR"
        )

        assertEquals("Server error occurred", exception.message)
        assertEquals("INTERNAL_ERROR", exception.reason)
    }

    @Test
    fun `OperationException should contain operation name`() {
        val cause = RuntimeException("Training failed")
        val exception = OperationException(
            message = "Training operation failed",
            operation = "train",
            cause = cause
        )

        assertEquals("Training operation failed", exception.message)
        assertEquals("train", exception.operation)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun `ProtocolException should contain message`() {
        val exception = ProtocolException(
            message = "Unexpected message type"
        )

        assertEquals("Unexpected message type", exception.message)
        assertNull(exception.cause)
    }

    @Test
    fun `CancellationException should indicate graceful cancellation`() {
        val exception = CancellationException(
            message = "User cancelled",
            wasGraceful = true
        )

        assertEquals("User cancelled", exception.message)
        assertTrue(exception.wasGraceful)
    }

    @Test
    fun `CancellationException should indicate forced cancellation`() {
        val exception = CancellationException(
            message = "Timeout exceeded",
            wasGraceful = false
        )

        assertEquals("Timeout exceeded", exception.message)
        assertTrue(!exception.wasGraceful)
    }

    @Test
    fun `CancellationException should default to graceful`() {
        val exception = CancellationException(message = "Cancelled")

        assertTrue(exception.wasGraceful)
    }

    @Test
    fun `FlexException hierarchy should work with when expression`() {
        val exceptions = listOf<FlexException>(
            ConnectionException("Connection failed"),
            ServerException("Server error", "ERROR"),
            OperationException("Op failed", "train"),
            ProtocolException("Protocol error"),
            CancellationException("Cancelled")
        )

        val types = exceptions.map { exception ->
            when (exception) {
                is ConnectionException -> "connection"
                is ServerException -> "server"
                is OperationException -> "operation"
                is ProtocolException -> "protocol"
                is CancellationException -> "cancellation"
            }
        }

        assertEquals(listOf("connection", "server", "operation", "protocol", "cancellation"), types)
    }

    @Test
    fun `FlexException should be throwable and catchable`() {
        var caught: FlexException? = null

        try {
            throw ConnectionException("Test error")
        } catch (e: FlexException) {
            caught = e
        }

        assertNotNull(caught)
        assertTrue(caught is ConnectionException)
    }

    @Test
    fun `FlexException should preserve stack trace`() {
        val exception = ConnectionException("Test")

        assertNotNull(exception.stackTrace)
        assertTrue(exception.stackTrace.isNotEmpty())
    }
}

