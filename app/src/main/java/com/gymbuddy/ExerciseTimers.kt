package com.gymbuddy

object ExerciseTimers {
    const val HARDCODED_EASY = 19
    const val HARDCODED_GOOD = 39
    const val HARDCODED_HARD = 59
    const val MAX_REST_SEC = 300

    data class RestDefaults(
        val easy: Int = HARDCODED_EASY,
        val good: Int = HARDCODED_GOOD,
        val hard: Int = HARDCODED_HARD
    )

    data class ResolvedRest(
        val seconds: Int,
        val sourceLabel: String
    )

    /** Resolve: exercise.restSec → restDefaults[rating] → hardcoded. */
    fun resolve(
        rating: String,
        restSec: Int? = null,
        defaults: RestDefaults? = null
    ): ResolvedRest {
        val key = rating.lowercase()
        if (restSec != null && restSec >= 0) {
            val sec = restSec.coerceIn(0, MAX_REST_SEC)
            return ResolvedRest(sec, "Override · ${formatMss(sec)}")
        }
        val fromDefaults = when (key) {
            "easy" -> defaults?.easy
            "hard" -> defaults?.hard
            else -> defaults?.good
        }
        if (fromDefaults != null && fromDefaults >= 0) {
            val sec = fromDefaults.coerceIn(0, MAX_REST_SEC)
            val label = key.replaceFirstChar { it.uppercase() }
            return ResolvedRest(sec, "$label · ${formatMss(sec)}")
        }
        val sec = cooldownSeconds(key)
        val label = when (key) {
            "easy" -> "Easy"
            "hard" -> "Hard"
            else -> "Good"
        }
        return ResolvedRest(sec, "$label · ${formatMss(sec)}")
    }

    fun cooldownSeconds(rating: String): Int = when (rating.lowercase()) {
        "easy" -> HARDCODED_EASY
        "good" -> HARDCODED_GOOD
        "hard" -> HARDCODED_HARD
        else -> HARDCODED_HARD
    }

    fun formatMss(totalSeconds: Int): String {
        val sec = totalSeconds.coerceAtLeast(0)
        val m = sec / 60
        val s = sec % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }
}
