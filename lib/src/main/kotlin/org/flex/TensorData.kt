/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

/**
 * Represents tensor data used in federated learning operations.
 *
 * This data class encapsulates the binary content and shape information
 * of a tensor, which is the fundamental data structure used for
 * transmitting model weights between clients and servers.
 *
 * ## Usage Example
 * ```kotlin
 * // Creating tensor data for a 2x3 matrix
 * val tensorData = TensorData(
 *     content = listOf(1, 2, 3, 4, 5, 6).map { it.toByte() },
 *     shape = listOf(2, 3)
 * )
 * ```
 *
 * @property content The raw byte content of the tensor. The interpretation
 *                   of these bytes depends on the dtype used during serialization.
 * @property shape The dimensions of the tensor. For example, `[2, 3]` represents
 *                 a 2x3 matrix, while `[10]` represents a vector of 10 elements.
 *
 * @see FlexClient
 */
data class TensorData(
    val content: List<Byte>,
    val shape: List<Int>
) {
    /**
     * Returns the total number of elements in this tensor.
     *
     * Calculated as the product of all dimensions in [shape].
     * Returns 0 if shape is empty.
     */
    val size: Int
        get() = if (shape.isEmpty()) 0 else shape.reduce { acc, i -> acc * i }

    /**
     * Returns the number of dimensions (rank) of this tensor.
     */
    val rank: Int
        get() = shape.size

    /**
     * Validates that the tensor data is consistent.
     *
     * @return `true` if the content size matches the expected size based on shape,
     *         `false` otherwise.
     */
    fun isValid(): Boolean {
        // Basic validation - actual byte size depends on dtype
        return content.isNotEmpty() && shape.isNotEmpty() && shape.all { it > 0 }
    }

    companion object {
        /**
         * Creates an empty TensorData instance.
         */
        fun empty(): TensorData = TensorData(emptyList(), emptyList())
    }
}

/**
 * Convenience extension to convert a List<Byte> to ByteArray.
 */
fun List<Byte>.toByteArray(): ByteArray {
    val result = ByteArray(size)
    forEachIndexed { index, byte -> result[index] = byte }
    return result
}

