package com.liftkit

import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseTimersTest {
    @Test
    fun prefersExerciseRestSec() {
        val resolved = ExerciseTimers.resolve(
            restPace = "long",
            restSec = 90,
            defaults = ExerciseTimers.RestDefaults(20, 40, 60),
            ratingForLegacyPace = "trivial"
        )
        assertEquals(90, resolved.seconds)
    }

    @Test
    fun usesDefaultsBeforeHardcoded() {
        val resolved = ExerciseTimers.resolve(
            restPace = "medium",
            restSec = null,
            defaults = ExerciseTimers.RestDefaults(20, 45, 60)
        )
        assertEquals(45, resolved.seconds)
    }

    @Test
    fun fallsBackToHardcoded() {
        val resolved = ExerciseTimers.resolve("short", null, null)
        assertEquals(ExerciseTimers.HARDCODED_EASY, resolved.seconds)
    }

    @Test
    fun effortDoesNotChangeAChosenPace() {
        val resolved = ExerciseTimers.resolve("short", null, null, "unachievable")
        assertEquals(ExerciseTimers.HARDCODED_EASY, resolved.seconds)
        assertEquals("Short · 0:19", resolved.sourceLabel)
    }

    @Test
    fun blankPaceKeepsTheOldRatingLength() {
        val resolved = ExerciseTimers.resolve(
            restPace = "",
            restSec = null,
            defaults = ExerciseTimers.RestDefaults(20, 45, 60),
            ratingForLegacyPace = "hard"
        )
        assertEquals(60, resolved.seconds)
    }

    @Test
    fun formatsMss() {
        assertEquals("1:30", ExerciseTimers.formatMss(90))
        assertEquals("0:05", ExerciseTimers.formatMss(5))
    }
}
