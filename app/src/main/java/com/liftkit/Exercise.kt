package com.liftkit

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
    var rating: String = "solid",
    var timerDurationSeconds: Int = 0,
    /** Optional per-exercise rest override in seconds. Null = use the rest length. */
    var restSec: Int? = null,
    /** Label for where the active rest came from (e.g. "Long · 0:59"). */
    var restSourceLabel: String = "",
    /** short, medium, or long. Blank means derive once from the old effort rating. */
    var restPace: String = ""
) : Serializable {
    fun effectivePace(): String = ExerciseTimers.normalizePace(restPace, rating)

    fun cooldownSeconds(defaults: ExerciseTimers.RestDefaults? = RestPrefs.defaults): Int =
        resolvedRest(defaults).seconds

    fun resolvedRest(defaults: ExerciseTimers.RestDefaults? = RestPrefs.defaults): ExerciseTimers.ResolvedRest =
        ExerciseTimers.resolve(restPace, restSec, defaults, rating)
}
