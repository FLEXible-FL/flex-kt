# Flex-KT

A Kotlin library for federated learning clients, designed to be Android-compatible.

## Features

- **Coroutine-based**: Full support for Kotlin coroutines, making it easy to integrate with Android's lifecycle
- **Robust error handling**: Automatic retry with exponential backoff for connection failures
- **Graceful cancellation**: Stop operations cleanly without leaving resources dangling
- **Observable state**: StateFlow-based state management for reactive UI updates
- **Comprehensive logging**: Listener interface for monitoring all client activities
- **Well-documented**: KDoc documentation for all public APIs

## Quick Start

### 1. Create a Client Implementation

```kotlin
class MyFLClient(config: ClientConfig) : FlexClient(config) {

    override suspend fun train(): Map<String, Float> {
        // Implement your training logic here
        // This runs when the server requests training
        return mapOf(
            "loss" to 0.5f,
            "accuracy" to 0.95f
        )
    }

    override suspend fun evaluate(): Map<String, Float> {
        // Implement your evaluation logic here
        return mapOf("accuracy" to 0.92f)
    }

    override suspend fun getWeights(): Map<String, TensorData> {
        // Return your model weights
        return mapOf(
            "layer1" to TensorData(
                content = /* your weight bytes */,
                shape = listOf(10, 5)
            )
        )
    }

    override suspend fun setWeights(weights: List<TensorData>) {
        // Apply the received weights to your model
        weights.forEach { tensor ->
            // Process each tensor
        }
    }
}
```

### 2. Configure and Run

```kotlin
// Create configuration
val config = ClientConfig.Builder()
    .baseUrl("http://your-server:8080")
    .maxRetries(5)
    .retryDelayMs(1000)
    .useExponentialBackoff(true)
    .connectionTimeout(30, TimeUnit.SECONDS)
    .build()

// Or use simple configuration with defaults
val simpleConfig = ClientConfig.simple("http://your-server:8080")

// Create client
val client = MyFLClient(config)

// Run in a coroutine scope
scope.launch(Dispatchers.IO) {
    try {
        client.run()
    } catch (e: FlexException) {
        when (e) {
            is ConnectionException -> println("Connection failed: ${e.message}")
            is ServerException -> println("Server error: ${e.reason}")
            is OperationException -> println("Operation '${e.operation}' failed")
            is CancellationException -> println("Cancelled: ${e.message}")
            is ProtocolException -> println("Protocol error: ${e.message}")
        }
    }
}

// To stop gracefully
client.stop()

// Wait for complete shutdown
client.awaitStop(timeoutMs = 5000)
```

## Android Integration

### ViewModel Example

```kotlin
class FederatedLearningViewModel : ViewModel() {

    private val config = ClientConfig.Builder()
        .baseUrl("http://server:8080")
        .maxRetries(3)
        .build()

    private val client = MyFLClient(config, listener = object : FlexClientListener {
        override fun onStateChanged(oldState: ClientState, newState: ClientState) {
            _uiState.value = _uiState.value.copy(connectionState = newState)
        }

        override fun onTrainCompleted(metrics: Map<String, Float>, durationMs: Long) {
            _uiState.value = _uiState.value.copy(
                lastTrainingMetrics = metrics,
                lastTrainingDuration = durationMs
            )
        }

        override fun onError(exception: FlexException) {
            _uiState.value = _uiState.value.copy(error = exception.message)
        }
    })

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Observe client state directly
    val connectionState: StateFlow<ClientState> = client.state

    fun startTraining() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.run()
            } catch (e: FlexException) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun stopTraining() {
        client.stop()
    }

    override fun onCleared() {
        client.cancel("ViewModel cleared")
        super.onCleared()
    }
}
```

## Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `baseUrl` | (required) | Server URL |
| `connectionTimeoutMs` | 30,000 | Connection timeout |
| `readTimeoutMs` | 60,000 | Read timeout |
| `writeTimeoutMs` | 60,000 | Write timeout |
| `maxRetries` | 3 | Max retry attempts |
| `retryDelayMs` | 1,000 | Initial retry delay |
| `useExponentialBackoff` | true | Use exponential backoff |
| `maxRetryDelayMs` | 30,000 | Max delay between retries |
| `enableHealthCheck` | true | Respond to health checks |

## Exception Hierarchy

```
FlexException (sealed)
├── ConnectionException   # Network/connection errors (may be recoverable)
├── ServerException       # Server sent an error response
├── OperationException    # Client operation (train/eval) failed
├── ProtocolException     # Protocol violation
└── CancellationException # Session was cancelled
```

## Client States

```
DISCONNECTED ──► CONNECTING ──► CONNECTED ──► RUNNING
      ▲              │              │            │
      │              │              │            │
      └──────────────┴──────────────┴────────────┘
                        (on error or stop)
                        
STOPPING ──► DISCONNECTED
```

## License

Apache-2.0

