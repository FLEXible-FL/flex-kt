/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

import com.squareup.wire.GrpcClient
import com.squareup.wire.GrpcStreamingCall
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okio.ByteString.Companion.toByteString
import org.flex.flexible.ClientMessage
import org.flex.flexible.FlexibleClient
import org.flex.flexible.ServerMessage
import org.flex.flexible.Tensor
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException as KotlinCancellationException

/**
 * Abstract base class for federated learning clients.
 *
 * This class provides a robust, coroutine-based implementation for communicating
 * with a federated learning server. It handles connection management, retry logic,
 * error handling, and graceful cancellation.
 *
 * ## Implementation
 *
 * To create a federated learning client, extend this class and implement the
 * abstract methods:
 *
 * ```kotlin
 * class MyFLClient(config: ClientConfig) : FlexClient(config) {
 *
 *     override suspend fun train(): Map<String, Float> {
 *         // Perform local training
 *         return mapOf("loss" to 0.5f, "accuracy" to 0.95f)
 *     }
 *
 *     override suspend fun evaluate(): Map<String, Float> {
 *         // Evaluate the model
 *         return mapOf("accuracy" to 0.92f)
 *     }
 *
 *     override suspend fun getWeights(): Map<String, TensorData> {
 *         // Return current model weights
 *         return mapOf("layer1" to TensorData(...))
 *     }
 *
 *     override suspend fun setWeights(weights: List<TensorData>) {
 *         // Update model with new weights
 *     }
 * }
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * val config = ClientConfig.Builder()
 *     .baseUrl("http://server:8080")
 *     .maxRetries(5)
 *     .build()
 *
 * val client = MyFLClient(config)
 *
 * // Run in a coroutine scope
 * scope.launch {
 *     try {
 *         client.run()
 *     } catch (e: FlexException) {
 *         println("Client error: ${e.message}")
 *     }
 * }
 *
 * // To stop gracefully
 * client.stop()
 * ```
 *
 * ## Android Integration
 *
 * This client is designed to be Android-compatible. Use it with appropriate
 * coroutine scopes:
 *
 * ```kotlin
 * // In a ViewModel
 * class FLViewModel : ViewModel() {
 *     private val client = MyFLClient(config)
 *
 *     fun startTraining() {
 *         viewModelScope.launch(Dispatchers.IO) {
 *             client.run()
 *         }
 *     }
 *
 *     override fun onCleared() {
 *         client.stop()
 *     }
 * }
 * ```
 *
 * @param config Configuration for the client including server URL and timeouts.
 * @param listener Optional listener for lifecycle events.
 *
 * @see ClientConfig
 * @see FlexClientListener
 * @see TensorData
 */
abstract class FlexClient(
    protected val config: ClientConfig,
    protected val listener: FlexClientListener = NoOpFlexClientListener
) {
    // State management
    private val _state = MutableStateFlow(ClientState.DISCONNECTED)

    /**
     * The current connection state of the client.
     *
     * Observe this flow to react to state changes:
     * ```kotlin
     * client.state.collect { state ->
     *     updateUI(state)
     * }
     * ```
     */
    val state: StateFlow<ClientState> = _state.asStateFlow()

    private val _stats = AtomicReference(SessionStats())

    /**
     * Current session statistics.
     */
    val stats: SessionStats
        get() = _stats.get()

    // Cancellation support
    private var sessionJob: Job? = null
    private val stopRequested = MutableStateFlow(false)

    // OkHttp client (reusable)
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE))
            .connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    // ==================== Abstract Methods ====================

    /**
     * Performs local model training.
     *
     * This method is called when the server requests a training round.
     * Implement your training logic here and return the resulting metrics.
     *
     * This is a suspending function, so you can use coroutines for
     * parallel or async operations within your training logic.
     *
     * @return A map of metric names to their values (e.g., "loss" to 0.5f).
     * @throws Exception if training fails. The exception will be caught and
     *                   reported to the listener.
     */
    protected abstract suspend fun train(): Map<String, Float>

    /**
     * Evaluates the current model.
     *
     * This method is called when the server requests model evaluation.
     * Implement your evaluation logic here and return the metrics.
     *
     * @return A map of metric names to their values (e.g., "accuracy" to 0.95f).
     * @throws Exception if evaluation fails.
     */
    protected abstract suspend fun evaluate(): Map<String, Float>

    /**
     * Returns the current model weights.
     *
     * This method is called when the server requests the client's model weights.
     * The weights are serialized and sent to the server for aggregation.
     *
     * @return A map of weight names to their tensor data.
     */
    protected abstract suspend fun getWeights(): Map<String, TensorData>

    /**
     * Updates the model with new weights.
     *
     * This method is called when the server sends aggregated weights.
     * Apply these weights to your local model.
     *
     * @param weights The list of tensor weights received from the server.
     */
    protected abstract suspend fun setWeights(weights: List<TensorData>)

    // ==================== Public API ====================

    /**
     * Starts the federated learning client.
     *
     * This method connects to the server and enters the main message loop,
     * processing server instructions until stopped or an unrecoverable error occurs.
     *
     * This is a suspending function that should be called from a coroutine scope.
     * It will suspend until the session ends.
     *
     * ## Retry Behavior
     *
     * If connection fails, the client will retry according to the configuration:
     * - Up to [ClientConfig.maxRetries] attempts
     * - With exponential backoff (if enabled)
     * - Starting with [ClientConfig.retryDelayMs] delay
     *
     * ## Cancellation
     *
     * The session can be cancelled by:
     * - Calling [stop] from another coroutine
     * - Cancelling the parent coroutine scope
     *
     * @throws ConnectionException if connection fails after all retries.
     * @throws ServerException if the server sends an error.
     * @throws CancellationException if the session is cancelled.
     * @throws OperationException if a client operation fails.
     */
    suspend fun run() {
        if (_state.value != ClientState.DISCONNECTED) {
            throw IllegalStateException("Client is already running or connecting")
        }

        stopRequested.value = false
        _stats.set(SessionStats().startSession())

        coroutineScope {
            sessionJob = coroutineContext[Job]

            try {
                runWithRetry()
            } finally {
                sessionJob = null
                transitionTo(ClientState.DISCONNECTED)
            }
        }
    }

    /**
     * Requests graceful shutdown of the client.
     *
     * This method signals the client to stop processing after completing
     * the current operation. It does not block; use [awaitStop] if you
     * need to wait for shutdown to complete.
     *
     * Safe to call from any thread or coroutine.
     */
    fun stop() {
        stopRequested.value = true
        transitionTo(ClientState.STOPPING)
    }

    /**
     * Waits for the client to fully stop.
     *
     * Call this after [stop] if you need to ensure the client has
     * completely shut down before proceeding.
     *
     * @param timeoutMs Maximum time to wait in milliseconds. 0 means wait indefinitely.
     * @return `true` if the client stopped within the timeout, `false` otherwise.
     */
    suspend fun awaitStop(timeoutMs: Long = 0): Boolean {
        return if (timeoutMs > 0) {
            withTimeoutOrNull(timeoutMs) {
                sessionJob?.join()
                true
            } ?: false
        } else {
            sessionJob?.join()
            true
        }
    }

    /**
     * Forcefully cancels the client session.
     *
     * Unlike [stop], this immediately cancels all operations without
     * waiting for them to complete gracefully.
     *
     * @param reason Optional reason for the cancellation.
     */
    fun cancel(reason: String = "Client cancelled") {
        sessionJob?.cancel(KotlinCancellationException(reason))
        transitionTo(ClientState.DISCONNECTED)
    }

    // ==================== Internal Implementation ====================

    private suspend fun runWithRetry() {
        var lastException: FlexException? = null
        var attempt = 0

        while (attempt <= config.maxRetries && !stopRequested.value) {
            attempt++
            updateStats { it.incrementConnectionAttempts() }
            listener.onConnectionAttempt(attempt, config.maxRetries + 1)

            try {
                transitionTo(ClientState.CONNECTING)
                connectAndRun()

                // If we reach here, session ended gracefully
                listener.onDisconnected(wasGraceful = true, cause = null)
                return

            } catch (@Suppress("UNUSED_VARIABLE") e: KotlinCancellationException) {
                // Coroutine was cancelled - don't retry
                val flexException = CancellationException("Session cancelled", wasGraceful = true)
                listener.onDisconnected(wasGraceful = true, cause = flexException)
                throw flexException

            } catch (e: IOException) {
                lastException = ConnectionException(
                    message = "Connection failed: ${e.message}",
                    cause = e,
                    isRecoverable = true
                )
                handleRetryableError(lastException, attempt)

            } catch (e: FlexException) {
                lastException = e
                when (e) {
                    is ConnectionException -> {
                        if (e.isRecoverable) {
                            handleRetryableError(e, attempt)
                        } else {
                            throw e
                        }
                    }
                    else -> throw e
                }
            } catch (e: Exception) {
                // Unexpected exception - wrap and rethrow
                lastException = OperationException(
                    message = "Unexpected error: ${e.message}",
                    operation = "run",
                    cause = e
                )
                throw lastException
            }
        }

        // All retries exhausted
        lastException?.let {
            listener.onDisconnected(wasGraceful = false, cause = it)
            throw it
        }
    }

    private suspend fun handleRetryableError(exception: FlexException, attempt: Int) {
        updateStats { it.incrementErrors() }
        listener.onError(exception)

        if (attempt <= config.maxRetries && !stopRequested.value) {
            val delayMs = config.calculateRetryDelay(attempt - 1)
            delay(delayMs)
        }
    }

    private suspend fun connectAndRun() {
        val grpcClient = GrpcClient.Builder()
            .client(okHttpClient)
            .baseUrl(config.baseUrl)
            .build()

        val flexibleClient = grpcClient.create(FlexibleClient::class)
        val streamingCall: GrpcStreamingCall<ClientMessage, ServerMessage> = flexibleClient.Send()

        coroutineScope {
            val (sendChannel, receiveChannel) = streamingCall.executeIn(this)

            try {
                // Send handshake
                sendChannel.send(ClientMessage(handshake_res = ClientMessage.HandshakeRes(status = 200)))
                updateStats { it.incrementMessagesSent() }

                transitionTo(ClientState.CONNECTED)
                listener.onConnected()

                // Enter main message loop
                transitionTo(ClientState.RUNNING)
                processMessages(sendChannel, receiveChannel)

            } finally {
                // Clean up
                try {
                    sendChannel.close()
                } catch (_: Exception) {
                    // Ignore close errors
                }
            }
        }
    }

    private suspend fun processMessages(
        sendChannel: SendChannel<ClientMessage>,
        receiveChannel: ReceiveChannel<ServerMessage>
    ) {
        for (message in receiveChannel) {
            // Check for stop request
            if (stopRequested.value) {
                break
            }

            updateStats { it.incrementMessagesReceived() }

            // Handle server error
            message.error?.let { error ->
                throw ServerException(
                    message = "Server error: ${error.reason}",
                    reason = error.reason
                )
            }

            // Process message types
            when {
                message.health_ins != null -> handleHealthCheck(sendChannel)
                message.eval_ins != null -> handleEvaluate(sendChannel)
                message.get_weights_ins != null -> handleGetWeights(sendChannel)
                message.train_ins != null -> handleTrain(sendChannel)
                message.send_weights_ins != null -> handleSendWeights(sendChannel, message.send_weights_ins)
            }
        }
    }

    private suspend fun handleHealthCheck(sendChannel: SendChannel<ClientMessage>) {
        if (config.enableHealthCheck) {
            sendChannel.send(ClientMessage(health_ins = ClientMessage.HealthPing(status = 200)))
            updateStats { it.incrementHealthChecks().incrementMessagesSent() }
        }
    }

    private suspend fun handleEvaluate(sendChannel: SendChannel<ClientMessage>) {
        listener.onEvaluateStarted()
        val startTime = System.currentTimeMillis()

        try {
            val metrics = evaluate()
            val duration = System.currentTimeMillis() - startTime

            sendChannel.send(ClientMessage(eval_res = ClientMessage.EvalRes(metrics = metrics)))
            updateStats { it.incrementEvaluate().incrementMessagesSent() }

            listener.onEvaluateCompleted(metrics, duration)
        } catch (e: Exception) {
            val exception = OperationException(
                message = "Evaluation failed: ${e.message}",
                operation = "evaluate",
                cause = e
            )
            updateStats { it.incrementErrors() }
            listener.onError(exception)
            throw exception
        }
    }

    private suspend fun handleGetWeights(sendChannel: SendChannel<ClientMessage>) {
        try {
            val weightsMap = getWeights()
            val tensors = weightsMap.values.map { tensorData ->
                Tensor(
                    shape = tensorData.shape,
                    data_ = tensorData.content.toByteArray().toByteString(),
                    dtype = "float32"
                )
            }

            sendChannel.send(ClientMessage(get_weights_res = ClientMessage.GetWeightsRes(weights = tensors)))
            updateStats { it.incrementWeightsSent().incrementMessagesSent() }

            listener.onWeightsSent(tensors.size)
        } catch (e: Exception) {
            val exception = OperationException(
                message = "Get weights failed: ${e.message}",
                operation = "getWeights",
                cause = e
            )
            updateStats { it.incrementErrors() }
            listener.onError(exception)
            throw exception
        }
    }

    private suspend fun handleTrain(sendChannel: SendChannel<ClientMessage>) {
        listener.onTrainStarted()
        val startTime = System.currentTimeMillis()

        try {
            val metrics = train()
            val duration = System.currentTimeMillis() - startTime

            sendChannel.send(ClientMessage(train_res = ClientMessage.TrainRes(metrics = metrics)))
            updateStats { it.incrementTrain().incrementMessagesSent() }

            listener.onTrainCompleted(metrics, duration)
        } catch (e: Exception) {
            val exception = OperationException(
                message = "Training failed: ${e.message}",
                operation = "train",
                cause = e
            )
            updateStats { it.incrementErrors() }
            listener.onError(exception)
            throw exception
        }
    }

    private suspend fun handleSendWeights(
        sendChannel: SendChannel<ClientMessage>,
        instruction: ServerMessage.SendWeightsIns
    ) {
        try {
            val weights = instruction.weights.map { tensor ->
                TensorData(
                    content = tensor.data_.toByteArray().toList(),
                    shape = tensor.shape
                )
            }

            setWeights(weights)

            sendChannel.send(ClientMessage(send_weights_res = ClientMessage.SendWeightsRes(status = 200)))
            updateStats { it.incrementWeightsReceived().incrementMessagesSent() }

            listener.onWeightsReceived(weights.size)
        } catch (e: Exception) {
            val exception = OperationException(
                message = "Set weights failed: ${e.message}",
                operation = "setWeights",
                cause = e
            )
            updateStats { it.incrementErrors() }
            listener.onError(exception)
            throw exception
        }
    }

    private fun transitionTo(newState: ClientState) {
        val oldState = _state.value
        if (oldState != newState) {
            _state.value = newState
            listener.onStateChanged(oldState, newState)
        }
    }

    private fun updateStats(update: (SessionStats) -> SessionStats) {
        _stats.set(update(_stats.get()))
        listener.onStatsUpdated(_stats.get())
    }
}

