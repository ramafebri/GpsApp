package com.rama.gpsapp.pdr

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class PdrTrackerTest {
    @Test
    fun `step heading north advances Y and leaves X unchanged`() {
        val tracker = PdrTracker()

        val position = tracker.onStep(headingRadians = 0f, stepLengthMeters = 1.0f)

        assertEquals(0f, position.x, EPSILON)
        assertEquals(1f, position.y, EPSILON)
        assertEquals(1, position.stepCount)
    }

    @Test
    fun `step heading east advances X and leaves Y unchanged`() {
        val tracker = PdrTracker()

        val position = tracker.onStep(headingRadians = (PI / 2).toFloat(), stepLengthMeters = 1.0f)

        assertEquals(1f, position.x, EPSILON)
        assertEquals(0f, position.y, EPSILON)
    }

    @Test
    fun `successive steps accumulate position and step count`() {
        val tracker = PdrTracker()

        tracker.onStep(headingRadians = 0f, stepLengthMeters = 0.75f)
        val second = tracker.onStep(headingRadians = 0f, stepLengthMeters = 0.75f)

        assertEquals(0f, second.x, EPSILON)
        assertEquals(1.5f, second.y, EPSILON)
        assertEquals(2, second.stepCount)
    }

    @Test
    fun `reset zeroes position and step count`() {
        val tracker = PdrTracker()
        tracker.onStep(headingRadians = 0.4f, stepLengthMeters = 0.8f)

        tracker.reset()
        val position = tracker.onStep(headingRadians = 0f, stepLengthMeters = 1.0f)

        assertEquals(0f, position.x, EPSILON)
        assertEquals(1f, position.y, EPSILON)
        assertEquals(1, position.stepCount)
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
