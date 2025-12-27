/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

/**
 * Represents the connection state of a [FlexClient].
 *
 * The client transitions through these states during its lifecycle:
 * ```
 * DISCONNECTED -> CONNECTING -> CONNECTED -> RUNNING
 *                     |              |          |
 *                     v              v          v
 *                DISCONNECTED   DISCONNECTED  DISCONNECTED
 * ```
 *
 * @see FlexClient
 * @see FlexClientListener
 */
enum class ClientState {
    /**
     * The client is not connected to the server.
     *
     * This is the initial state and the state after disconnection
     * (whether graceful or due to an error).
     */
    DISCONNECTED,

    /**
     * The client is attempting to establish a connection to the server.
     *
     * This includes DNS resolution, TCP handshake, and gRPC stream setup.
     */
    CONNECTING,

    /**
     * The client has established a connection and completed the handshake.
     *
     * The client is ready to receive instructions from the server.
     */
    CONNECTED,

    /**
     * The client is actively processing server instructions.
     *
     * This indicates the client is in the main message processing loop,
     * responding to train, evaluate, or weight transfer requests.
     */
    RUNNING,

    /**
     * The client is in the process of gracefully shutting down.
     *
     * The client will complete any in-progress operations before
     * transitioning to [DISCONNECTED].
     */
    STOPPING
}

/**
 * Statistics about the client's session.
 *
 * @property messagesReceived Total number of messages received from server.
 * @property messagesSent Total number of messages sent to server.
 * @property trainOperations Number of training operations completed.
 * @property evaluateOperations Number of evaluation operations completed.
 * @property weightsReceived Number of times weights were received from server.
 * @property weightsSent Number of times weights were sent to server.
 * @property healthChecks Number of health checks responded to.
 * @property errors Number of errors encountered.
 * @property connectionAttempts Number of connection attempts made.
 * @property sessionStartTime Timestamp when the session started (epoch millis), or null if not started.
 * @property lastActivityTime Timestamp of last activity (epoch millis), or null if no activity.
 */
data class SessionStats(
    val messagesReceived: Long = 0,
    val messagesSent: Long = 0,
    val trainOperations: Long = 0,
    val evaluateOperations: Long = 0,
    val weightsReceived: Long = 0,
    val weightsSent: Long = 0,
    val healthChecks: Long = 0,
    val errors: Long = 0,
    val connectionAttempts: Int = 0,
    val sessionStartTime: Long? = null,
    val lastActivityTime: Long? = null
) {
    /**
     * Returns the session duration in milliseconds, or null if session hasn't started.
     */
    fun sessionDurationMs(): Long? {
        return sessionStartTime?.let { start ->
            (lastActivityTime ?: System.currentTimeMillis()) - start
        }
    }

    internal fun incrementMessagesReceived() = copy(
        messagesReceived = messagesReceived + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementMessagesSent() = copy(
        messagesSent = messagesSent + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementTrain() = copy(
        trainOperations = trainOperations + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementEvaluate() = copy(
        evaluateOperations = evaluateOperations + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementWeightsReceived() = copy(
        weightsReceived = weightsReceived + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementWeightsSent() = copy(
        weightsSent = weightsSent + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementHealthChecks() = copy(
        healthChecks = healthChecks + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementErrors() = copy(
        errors = errors + 1,
        lastActivityTime = System.currentTimeMillis()
    )

    internal fun incrementConnectionAttempts() = copy(
        connectionAttempts = connectionAttempts + 1
    )

    internal fun startSession() = copy(
        sessionStartTime = System.currentTimeMillis(),
        lastActivityTime = System.currentTimeMillis()
    )
}

