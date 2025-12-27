/*
 * Copyright (c) 2024 Flex-KT Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.flex

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [TensorData].
 */
class TensorDataTest {

    @Test
    fun `TensorData should correctly calculate size for 1D tensor`() {
        val tensor = TensorData(
            content = List(10) { it.toByte() },
            shape = listOf(10)
        )

        assertEquals(10, tensor.size)
        assertEquals(1, tensor.rank)
    }

    @Test
    fun `TensorData should correctly calculate size for 2D tensor`() {
        val tensor = TensorData(
            content = List(6) { it.toByte() },
            shape = listOf(2, 3)
        )

        assertEquals(6, tensor.size)
        assertEquals(2, tensor.rank)
    }

    @Test
    fun `TensorData should correctly calculate size for 3D tensor`() {
        val tensor = TensorData(
            content = List(24) { it.toByte() },
            shape = listOf(2, 3, 4)
        )

        assertEquals(24, tensor.size)
        assertEquals(3, tensor.rank)
    }

    @Test
    fun `TensorData should return 0 size for empty shape`() {
        val tensor = TensorData(
            content = emptyList(),
            shape = emptyList()
        )

        assertEquals(0, tensor.size)
        assertEquals(0, tensor.rank)
    }

    @Test
    fun `TensorData isValid should return true for valid tensor`() {
        val tensor = TensorData(
            content = List(6) { it.toByte() },
            shape = listOf(2, 3)
        )

        assertTrue(tensor.isValid())
    }

    @Test
    fun `TensorData isValid should return false for empty content`() {
        val tensor = TensorData(
            content = emptyList(),
            shape = listOf(2, 3)
        )

        assertFalse(tensor.isValid())
    }

    @Test
    fun `TensorData isValid should return false for empty shape`() {
        val tensor = TensorData(
            content = List(6) { it.toByte() },
            shape = emptyList()
        )

        assertFalse(tensor.isValid())
    }

    @Test
    fun `TensorData isValid should return false for shape with zero dimension`() {
        val tensor = TensorData(
            content = List(6) { it.toByte() },
            shape = listOf(2, 0, 3)
        )

        assertFalse(tensor.isValid())
    }

    @Test
    fun `TensorData isValid should return false for shape with negative dimension`() {
        val tensor = TensorData(
            content = List(6) { it.toByte() },
            shape = listOf(2, -1, 3)
        )

        assertFalse(tensor.isValid())
    }

    @Test
    fun `TensorData empty should create empty tensor`() {
        val tensor = TensorData.empty()

        assertTrue(tensor.content.isEmpty())
        assertTrue(tensor.shape.isEmpty())
        assertEquals(0, tensor.size)
        assertEquals(0, tensor.rank)
    }

    @Test
    fun `TensorData data class should implement equals correctly`() {
        val tensor1 = TensorData(
            content = listOf<Byte>(1, 2, 3),
            shape = listOf(3)
        )
        val tensor2 = TensorData(
            content = listOf<Byte>(1, 2, 3),
            shape = listOf(3)
        )
        val tensor3 = TensorData(
            content = listOf<Byte>(1, 2, 4),
            shape = listOf(3)
        )

        assertEquals(tensor1, tensor2)
        assertFalse(tensor1 == tensor3)
    }

    @Test
    fun `TensorData copy should work correctly`() {
        val original = TensorData(
            content = listOf<Byte>(1, 2, 3),
            shape = listOf(3)
        )
        val copied = original.copy(shape = listOf(1, 3))

        assertEquals(listOf<Byte>(1, 2, 3), copied.content)
        assertEquals(listOf(1, 3), copied.shape)
    }
}

