package com.gymbuddy

import android.content.Context

object RestPrefs {
    private const val PREFS = "gym_buddy_rest"
    private const val KEY_EASY = "rest_easy"
    private const val KEY_GOOD = "rest_good"
    private const val KEY_HARD = "rest_hard"

    @Volatile
    var defaults: ExerciseTimers.RestDefaults = ExerciseTimers.RestDefaults()
        private set

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        defaults = ExerciseTimers.RestDefaults(
            easy = p.getInt(KEY_EASY, ExerciseTimers.HARDCODED_EASY),
            good = p.getInt(KEY_GOOD, ExerciseTimers.HARDCODED_GOOD),
            hard = p.getInt(KEY_HARD, ExerciseTimers.HARDCODED_HARD)
        )
    }

    fun save(context: Context, value: ExerciseTimers.RestDefaults) {
        defaults = value
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_EASY, value.easy)
            .putInt(KEY_GOOD, value.good)
            .putInt(KEY_HARD, value.hard)
            .apply()
    }
}
