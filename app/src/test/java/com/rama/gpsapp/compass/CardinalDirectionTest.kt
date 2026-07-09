package com.rama.gpsapp.compass

import org.junit.Assert.assertEquals
import org.junit.Test

class CardinalDirectionTest {

    @Test
    fun `cardinal labels for primary directions`() {
        assertEquals("N", CardinalDirection.fromDegrees(0f))
        assertEquals("E", CardinalDirection.fromDegrees(90f))
        assertEquals("S", CardinalDirection.fromDegrees(180f))
        assertEquals("W", CardinalDirection.fromDegrees(270f))
    }

    @Test
    fun `cardinal labels for intercardinal directions`() {
        assertEquals("NE", CardinalDirection.fromDegrees(45f))
        assertEquals("SE", CardinalDirection.fromDegrees(135f))
        assertEquals("SW", CardinalDirection.fromDegrees(225f))
        assertEquals("NW", CardinalDirection.fromDegrees(315f))
    }
}
