/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

/**
 * Listener interface for [FlexClient] lifecycle events.
 *
 * Implement this interface to receive callbacks about client state changes,
 * errors, and session events. This is useful for logging, UI updates,
 * or implementing custom retry/recovery logic.
 *
 * ## Usage Example
 * ```kotlin
 * class MyClientListener : FlexClientListener {
 *     override fun onStateChanged(oldState: ClientState, newState: ClientState) {
 *         println("Client state: $oldState -> $newState")
 *     }
 *
 *     override fun onError(exception: FlexException) {
 *         println("Error: ${exception.message}")
 *     }
 *
 *     // ... implement other methods
 * }
 *
 * val client = object : FlexClient(config, listener = MyClientListener()) {
 *     // ... implement abstract methods
 * }
 * ```
 *
 * All callback methods have default no-op implementations, so you only need
 * to override the ones you're interested in.
 *
 * **Note:** Callbacks are invoked from the coroutine context of the client,
 * so avoid blocking operations in callback implementations.
 *
 * @see FlexClient
 * @see ClientState
 */
interface FlexClientListener {

    /**
     * Called when the client's connection state changes.
     *
     * @param oldState The previous state.
     * @param newState The new state.
     */
    fun onStateChanged(oldState: ClientState, newState: ClientState) {}

    /**
     * Called when an error occurs during client operation.
     *
     * This is called for both recoverable and non-recoverable errors.
     * Check the exception type and properties to determine the appropriate action.
     *
     * @param exception The exception that occurred.
     */
    fun onError(exception: FlexException) {}

    /**
     * Called when a connection attempt starts.
     *
     * @param attempt The current attempt number (1-indexed).
     * @param maxAttempts The maximum number of attempts that will be made.
     */
    fun onConnectionAttempt(attempt: Int, maxAttempts: Int) {}

    /**
     * Called when the client successfully connects to the server.
     */
    fun onConnected() {}

    /**
     * Called when the client disconnects from the server.
     *
     * @param wasGraceful `true` if the disconnection was initiated by the client,
     *                    `false` if it was due to an error or server disconnection.
     * @param cause The exception that caused the disconnection, if any.
     */
    fun onDisconnected(wasGraceful: Boolean, cause: FlexException?) {}

    /**
     * Called before a training operation starts.
     */
    fun onTrainStarted() {}

    /**
     * Called after a training operation completes.
     *
     * @param metrics The training metrics returned by the operation.
     * @param durationMs The duration of the training operation in milliseconds.
     */
    fun onTrainCompleted(metrics: Map<String, Float>, durationMs: Long) {}

    /**
     * Called before an evaluation operation starts.
     */
    fun onEvaluateStarted() {}

    /**
     * Called after an evaluation operation completes.
     *
     * @param metrics The evaluation metrics returned by the operation.
     * @param durationMs The duration of the evaluation operation in milliseconds.
     */
    fun onEvaluateCompleted(metrics: Map<String, Float>, durationMs: Long) {}

    /**
     * Called when weights are received from the server.
     *
     * @param weightsCount The number of tensor weights received.
     */
    fun onWeightsReceived(weightsCount: Int) {}

    /**
     * Called when weights are sent to the server.
     *
     * @param weightsCount The number of tensor weights sent.
     */
    fun onWeightsSent(weightsCount: Int) {}

    /**
     * Called when the session statistics are updated.
     *
     * @param stats The current session statistics.
     */
    fun onStatsUpdated(stats: SessionStats) {}
}

/**
 * A [FlexClientListener] implementation that does nothing.
 *
 * Use this when you don't need to handle any lifecycle events.
 */
object NoOpFlexClientListener : FlexClientListener

