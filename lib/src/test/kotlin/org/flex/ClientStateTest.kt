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
 * Unit tests for [ClientState] and [SessionStats].
 */
class ClientStateTest {

    @Test
    fun `ClientState enum should have all expected states`() {
        val states = ClientState.entries.toSet()

        assertTrue(states.contains(ClientState.DISCONNECTED))
        assertTrue(states.contains(ClientState.CONNECTING))
        assertTrue(states.contains(ClientState.CONNECTED))
        assertTrue(states.contains(ClientState.RUNNING))
        assertTrue(states.contains(ClientState.STOPPING))
        assertEquals(5, states.size)
    }

    @Test
    fun `SessionStats should have zero defaults`() {
        val stats = SessionStats()

        assertEquals(0, stats.messagesReceived)
        assertEquals(0, stats.messagesSent)
        assertEquals(0, stats.trainOperations)
        assertEquals(0, stats.evaluateOperations)
        assertEquals(0, stats.weightsReceived)
        assertEquals(0, stats.weightsSent)
        assertEquals(0, stats.healthChecks)
        assertEquals(0, stats.errors)
        assertEquals(0, stats.connectionAttempts)
        assertNull(stats.sessionStartTime)
        assertNull(stats.lastActivityTime)
    }

    @Test
    fun `SessionStats incrementMessagesReceived should update count and time`() {
        val stats = SessionStats()
        val updated = stats.incrementMessagesReceived()

        assertEquals(1, updated.messagesReceived)
        assertNotNull(updated.lastActivityTime)
    }

    @Test
    fun `SessionStats incrementMessagesSent should update count and time`() {
        val stats = SessionStats()
        val updated = stats.incrementMessagesSent()

        assertEquals(1, updated.messagesSent)
        assertNotNull(updated.lastActivityTime)
    }

    @Test
    fun `SessionStats incrementTrain should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementTrain()

        assertEquals(1, updated.trainOperations)
        assertNotNull(updated.lastActivityTime)
    }

    @Test
    fun `SessionStats incrementEvaluate should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementEvaluate()

        assertEquals(1, updated.evaluateOperations)
        assertNotNull(updated.lastActivityTime)
    }

    @Test
    fun `SessionStats incrementWeightsReceived should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementWeightsReceived()

        assertEquals(1, updated.weightsReceived)
    }

    @Test
    fun `SessionStats incrementWeightsSent should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementWeightsSent()

        assertEquals(1, updated.weightsSent)
    }

    @Test
    fun `SessionStats incrementHealthChecks should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementHealthChecks()

        assertEquals(1, updated.healthChecks)
    }

    @Test
    fun `SessionStats incrementErrors should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementErrors()

        assertEquals(1, updated.errors)
    }

    @Test
    fun `SessionStats incrementConnectionAttempts should update count`() {
        val stats = SessionStats()
        val updated = stats.incrementConnectionAttempts()

        assertEquals(1, updated.connectionAttempts)
    }

    @Test
    fun `SessionStats startSession should set session start time`() {
        val stats = SessionStats()
        val started = stats.startSession()

        assertNotNull(started.sessionStartTime)
        assertNotNull(started.lastActivityTime)
    }

    @Test
    fun `SessionStats sessionDurationMs should return null when not started`() {
        val stats = SessionStats()

        assertNull(stats.sessionDurationMs())
    }

    @Test
    fun `SessionStats sessionDurationMs should return positive value when started`() {
        val stats = SessionStats().startSession()

        // Small delay to ensure some time passes
        Thread.sleep(10)

        val duration = stats.sessionDurationMs()
        assertNotNull(duration)
        assertTrue(duration >= 0)
    }

    @Test
    fun `SessionStats should chain updates correctly`() {
        val stats = SessionStats()
            .startSession()
            .incrementConnectionAttempts()
            .incrementMessagesReceived()
            .incrementMessagesSent()
            .incrementTrain()
            .incrementEvaluate()
            .incrementWeightsReceived()
            .incrementWeightsSent()
            .incrementHealthChecks()
            .incrementErrors()

        assertEquals(1, stats.connectionAttempts)
        assertEquals(1, stats.messagesReceived)
        assertEquals(1, stats.messagesSent)
        assertEquals(1, stats.trainOperations)
        assertEquals(1, stats.evaluateOperations)
        assertEquals(1, stats.weightsReceived)
        assertEquals(1, stats.weightsSent)
        assertEquals(1, stats.healthChecks)
        assertEquals(1, stats.errors)
        assertNotNull(stats.sessionStartTime)
    }

    @Test
    fun `SessionStats data class should implement equals correctly`() {
        val stats1 = SessionStats(messagesReceived = 5, messagesSent = 3)
        val stats2 = SessionStats(messagesReceived = 5, messagesSent = 3)
        val stats3 = SessionStats(messagesReceived = 5, messagesSent = 4)

        assertEquals(stats1, stats2)
        assertTrue(stats1 != stats3)
    }
}

