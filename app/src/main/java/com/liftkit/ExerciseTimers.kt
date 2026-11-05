package com.liftkit

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

    fun normalizeRating(raw: String?): String = when (raw?.lowercase()) {
        "trivial" -> "trivial"
        "easy" -> "easy"
        "hard" -> "hard"
        "unachievable" -> "unachievable"
        "solid", "good" -> "solid"
        else -> "solid"
    }

    /** Old logs stored effort only. That effort used to pick the rest length. */
    fun paceFromRating(rating: String): String = when (rating.lowercase()) {
        "trivial", "easy" -> "short"
        "hard", "unachievable" -> "long"
        else -> "medium"
    }

    fun normalizePace(raw: String?, rating: String): String = when (raw?.lowercase()) {
        "short", "medium", "long" -> raw.lowercase()
        else -> paceFromRating(rating)
    }

    /** Rest length is independent of effort. A per-exercise second override still wins. */
    fun resolve(
        restPace: String,
        restSec: Int? = null,
        defaults: RestDefaults? = null,
        ratingForLegacyPace: String = "solid"
    ): ResolvedRest {
        if (restSec != null && restSec >= 0) {
            val sec = restSec.coerceIn(0, MAX_REST_SEC)
            return ResolvedRest(sec, "Custom · ${formatMss(sec)}")
        }
        val pace = normalizePace(restPace, ratingForLegacyPace)
        val fromDefaults = when (pace) {
            "short" -> defaults?.easy
            "long" -> defaults?.hard
            else -> defaults?.good
        }
        val sec = (fromDefaults ?: hardcodedSeconds(pace)).coerceIn(0, MAX_REST_SEC)
        val label = when (pace) {
            "short" -> "Short"
            "long" -> "Long"
            else -> "Medium"
        }
        return ResolvedRest(sec, "$label · ${formatMss(sec)}")
    }

    fun hardcodedSeconds(pace: String): Int = when (pace) {
        "short" -> HARDCODED_EASY
        "long" -> HARDCODED_HARD
        else -> HARDCODED_GOOD
    }

    fun formatMss(totalSeconds: Int): String {
        val sec = totalSeconds.coerceAtLeast(0)
        val m = sec / 60
        val s = sec % 60
        return "$m:${s.toString().padStart(2, '0')}"
    }
}
