/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

/**
 * Base exception class for all Flex client errors.
 *
 * This sealed class hierarchy provides structured error handling
 * for federated learning operations.
 *
 * @property message A human-readable description of the error.
 * @property cause The underlying cause of this exception, if any.
 */
sealed class FlexException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a connection to the federated learning server fails.
 *
 * This can occur during initial connection establishment or when
 * the connection is lost during operation.
 *
 * @property message Description of the connection failure.
 * @property cause The underlying network or IO exception.
 * @property isRecoverable Indicates whether the connection failure might be
 *                         recoverable through retry.
 */
class ConnectionException(
    override val message: String,
    override val cause: Throwable? = null,
    val isRecoverable: Boolean = true
) : FlexException(message, cause)

/**
 * Exception thrown when the server sends an error response.
 *
 * @property message Description of the server error.
 * @property reason The error reason provided by the server.
 */
class ServerException(
    override val message: String,
    val reason: String
) : FlexException(message)

/**
 * Exception thrown when a client operation (train, evaluate, etc.) fails.
 *
 * @property message Description of the operation failure.
 * @property operation The name of the operation that failed.
 * @property cause The underlying cause of the failure.
 */
class OperationException(
    override val message: String,
    val operation: String,
    override val cause: Throwable? = null
) : FlexException(message, cause)

/**
 * Exception thrown when a protocol violation occurs.
 *
 * This typically indicates a bug or version mismatch between
 * the client and server.
 *
 * @property message Description of the protocol violation.
 */
class ProtocolException(
    override val message: String
) : FlexException(message)

/**
 * Exception thrown when the client session is cancelled.
 *
 * @property message Description of the cancellation.
 * @property wasGraceful Indicates whether the cancellation was graceful
 *                       (initiated by the client) or forced.
 */
class CancellationException(
    override val message: String,
    val wasGraceful: Boolean = true
) : FlexException(message)

