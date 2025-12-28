/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [FlexClient].
 *
 * These tests focus on the client's state management and behavior
 * without requiring a real server connection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlexClientTest {

    /**
     * Test implementation of FlexClient for unit testing.
     */
    private class TestFlexClient(
        config: ClientConfig,
        listener: FlexClientListener = NoOpFlexClientListener
    ) : FlexClient(config, listener) {

        var trainCallCount = 0
        var evaluateCallCount = 0
        var getWeightsCallCount = 0
        var setWeightsCallCount = 0
        var lastReceivedWeights: List<TensorData>? = null

        var trainResult: Map<String, Float> = mapOf("loss" to 0.1f)
        var evaluateResult: Map<String, Float> = mapOf("accuracy" to 0.95f)
        var weightsResult: Map<String, TensorData> = emptyMap()

        var shouldThrowOnTrain = false
        var shouldThrowOnEvaluate = false

        public override suspend fun train(): Map<String, Float> {
            trainCallCount++
            if (shouldThrowOnTrain) {
                throw RuntimeException("Training failed")
            }
            return trainResult
        }

        public override suspend fun evaluate(): Map<String, Float> {
            evaluateCallCount++
            if (shouldThrowOnEvaluate) {
                throw RuntimeException("Evaluation failed")
            }
            return evaluateResult
        }

        public override suspend fun getWeights(): Map<String, TensorData> {
            getWeightsCallCount++
            return weightsResult
        }

        public override suspend fun setWeights(weights: List<TensorData>) {
            setWeightsCallCount++
            lastReceivedWeights = weights
        }
    }

    /**
     * Test implementation of FlexClientListener for capturing events.
     */
    private class TestListener : FlexClientListener {
        val stateChanges = mutableListOf<Pair<ClientState, ClientState>>()
        val errors = mutableListOf<FlexException>()
        val connectionAttempts = mutableListOf<Pair<Int, Int>>()
        var connectedCount = 0
        var disconnectedCount = 0
        var trainStartedCount = 0
        var trainCompletedCount = 0
        var evaluateStartedCount = 0
        var evaluateCompletedCount = 0

        override fun onStateChanged(oldState: ClientState, newState: ClientState) {
            stateChanges.add(oldState to newState)
        }

        override fun onError(exception: FlexException) {
            errors.add(exception)
        }

        override fun onConnectionAttempt(attempt: Int, maxAttempts: Int) {
            connectionAttempts.add(attempt to maxAttempts)
        }

        override fun onConnected() {
            connectedCount++
        }

        override fun onDisconnected(wasGraceful: Boolean, cause: FlexException?) {
            disconnectedCount++
        }

        override fun onTrainStarted() {
            trainStartedCount++
        }

        override fun onTrainCompleted(metrics: Map<String, Float>, durationMs: Long) {
            trainCompletedCount++
        }

        override fun onEvaluateStarted() {
            evaluateStartedCount++
        }

        override fun onEvaluateCompleted(metrics: Map<String, Float>, durationMs: Long) {
            evaluateCompletedCount++
        }
    }

    @Test
    fun `client should start in DISCONNECTED state`() {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)

        assertEquals(ClientState.DISCONNECTED, client.state.value)
    }

    @Test
    fun `client should have zero initial stats`() {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)

        val stats = client.stats
        assertEquals(0, stats.messagesReceived)
        assertEquals(0, stats.messagesSent)
        assertEquals(0, stats.trainOperations)
        assertEquals(0, stats.evaluateOperations)
    }

    @Test
    fun `stop should set state to STOPPING`() {
        val config = ClientConfig.simple("http://localhost:8080")
        val listener = TestListener()
        val client = TestFlexClient(config, listener)

        client.stop()

        assertEquals(ClientState.STOPPING, client.state.value)
        assertTrue(listener.stateChanges.any { it.second == ClientState.STOPPING })
    }

    @Test
    fun `client should use custom config values`() {
        val config = ClientConfig.Builder()
            .baseUrl("http://custom:9090")
            .maxRetries(5)
            .retryDelayMs(2000)
            .build()

        // Just verify the config itself is created correctly
        assertEquals("http://custom:9090", config.baseUrl)
        assertEquals(5, config.maxRetries)
        assertEquals(2000L, config.retryDelayMs)

        // Verify client can be created with this config
        val client = TestFlexClient(config)
        assertEquals(ClientState.DISCONNECTED, client.state.value)
    }

    @Test
    fun `NoOpFlexClientListener should not throw on any callback`() {
        val listener = NoOpFlexClientListener

        // None of these should throw
        listener.onStateChanged(ClientState.DISCONNECTED, ClientState.CONNECTING)
        listener.onError(ConnectionException("Test"))
        listener.onConnectionAttempt(1, 3)
        listener.onConnected()
        listener.onDisconnected(true, null)
        listener.onTrainStarted()
        listener.onTrainCompleted(emptyMap(), 100)
        listener.onEvaluateStarted()
        listener.onEvaluateCompleted(emptyMap(), 100)
        listener.onWeightsReceived(5)
        listener.onWeightsSent(5)
        listener.onStatsUpdated(SessionStats())
    }

    @Test
    fun `TestFlexClient train should be callable`() = runTest {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)
        client.trainResult = mapOf("loss" to 0.5f, "accuracy" to 0.9f)

        val result = client.train()

        assertEquals(1, client.trainCallCount)
        assertEquals(0.5f, result["loss"])
        assertEquals(0.9f, result["accuracy"])
    }

    @Test
    fun `TestFlexClient evaluate should be callable`() = runTest {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)
        client.evaluateResult = mapOf("accuracy" to 0.95f)

        val result = client.evaluate()

        assertEquals(1, client.evaluateCallCount)
        assertEquals(0.95f, result["accuracy"])
    }

    @Test
    fun `TestFlexClient getWeights should be callable`() = runTest {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)
        val tensor = TensorData(listOf(1, 2, 3), listOf(3))
        client.weightsResult = mapOf("layer1" to tensor)

        val result = client.getWeights()

        assertEquals(1, client.getWeightsCallCount)
        assertEquals(tensor, result["layer1"])
    }

    @Test
    fun `TestFlexClient setWeights should receive weights`() = runTest {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)
        val weights = listOf(
            TensorData(listOf(1, 2, 3), listOf(3)),
            TensorData(listOf(4, 5, 6), listOf(3))
        )

        client.setWeights(weights)

        assertEquals(1, client.setWeightsCallCount)
        assertEquals(weights, client.lastReceivedWeights)
    }

    @Test
    fun `listener should have default implementations`() {
        // Create anonymous listener that only overrides one method
        val listener = object : FlexClientListener {
            var stateChangeCount = 0

            override fun onStateChanged(oldState: ClientState, newState: ClientState) {
                stateChangeCount++
            }
        }

        // These should all work with default no-op implementations
        listener.onError(ConnectionException("test"))
        listener.onConnected()
        listener.onDisconnected(true, null)

        assertEquals(0, listener.stateChangeCount)
    }

    @Test
    fun `cancel should set state to DISCONNECTED`() {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)

        client.cancel("Test cancellation")

        assertEquals(ClientState.DISCONNECTED, client.state.value)
    }

    @Test
    fun `run should throw if already running`() = runTest {
        val config = ClientConfig.simple("http://localhost:8080")
        val client = TestFlexClient(config)
        
        // Use a background job to simulate running
        client.stop() // Transition to STOPPING to simulate not DISCONNECTED
        
        kotlin.test.assertFailsWith<IllegalStateException> {
            client.run()
        }
    }

    @Test
    fun `run should retry on connection failure`() = runTest {
        val config = ClientConfig.Builder()
            .baseUrl("http://localhost:1") // Likely to fail quickly
            .maxRetries(1)
            .retryDelayMs(1)
            .build()
        val listener = TestListener()
        val client = TestFlexClient(config, listener)

        try {
            client.run()
        } catch (e: ConnectionException) {
            // Expected
        }

        assertEquals(0, client.trainCallCount)
        assertTrue(listener.connectionAttempts.isNotEmpty())
        assertTrue(listener.errors.any { it is ConnectionException })
        assertEquals(ClientState.DISCONNECTED, client.state.value)
    }

    @Test
    fun `stop should be effective during connection`() = runTest {
        val config = ClientConfig.Builder()
            .baseUrl("http://localhost:1")
            .maxRetries(10)
            .retryDelayMs(100)
            .build()
        val client = TestFlexClient(config)
        
        val job = launch {
            try {
                client.run()
            } catch (e: Exception) {
                // Ignore
            }
        }
        
        delay(50)
        client.stop()
        job.join()
        
        assertEquals(ClientState.DISCONNECTED, client.state.value)
    }
}

