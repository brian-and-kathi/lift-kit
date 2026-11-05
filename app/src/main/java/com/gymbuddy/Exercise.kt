package com.gymbuddy

import java.io.Serializable

data class Exercise(
    var title: String,
    var weight: Int,
    var reps: Int,
    var sets: Int,
    var notes: String = "",
    var completedSets: Int = 0,
    var isTimerActive: Boolean = false,
    var remainingSeconds: Int = 0,
    var timerEndTime: Long = 0,
    var rating: String = "good", // "easy", "good", "hard"
    var timerDurationSeconds: Int = 0,
    /** Optional per-exercise rest override in seconds. Null = use defaults. */
    var restSec: Int? = null,
    /** Label for where the active rest came from (e.g. "Hard · 1:00"). */
    var restSourceLabel: String = ""
) : Serializable {
    fun cooldownSeconds(defaults: ExerciseTimers.RestDefaults? = RestPrefs.defaults): Int =
        ExerciseTimers.resolve(rating, restSec, defaults).seconds

    fun resolvedRest(defaults: ExerciseTimers.RestDefaults? = RestPrefs.defaults): ExerciseTimers.ResolvedRest =
        ExerciseTimers.resolve(rating, restSec, defaults)
}
