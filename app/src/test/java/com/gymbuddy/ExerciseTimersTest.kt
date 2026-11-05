package com.gymbuddy

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTimersTest {
    @Test
    fun prefersExerciseRestSec() {
        val resolved = ExerciseTimers.resolve(
            rating = "hard",
            restSec = 90,
            defaults = ExerciseTimers.RestDefaults(20, 40, 60)
        )
        assertEquals(90, resolved.seconds)
    }

    @Test
    fun usesDefaultsBeforeHardcoded() {
        val resolved = ExerciseTimers.resolve(
            rating = "good",
            restSec = null,
            defaults = ExerciseTimers.RestDefaults(20, 45, 60)
        )
        assertEquals(45, resolved.seconds)
    }

    @Test
    fun fallsBackToHardcoded() {
        val resolved = ExerciseTimers.resolve("easy", null, null)
        assertEquals(ExerciseTimers.HARDCODED_EASY, resolved.seconds)
    }

    @Test
    fun formatsMss() {
        assertEquals("1:30", ExerciseTimers.formatMss(90))
        assertEquals("0:05", ExerciseTimers.formatMss(5))
    }
}
