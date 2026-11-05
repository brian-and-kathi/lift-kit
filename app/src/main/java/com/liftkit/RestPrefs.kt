package com.liftkit

import android.content.Context
import android.content.SharedPreferences

object RestPrefs {
    private const val PREFS = "lift_kit_rest"
    private const val LEGACY_PREFS = "gym_buddy_rest"
    private const val KEY_EASY = "rest_easy"
    private const val KEY_GOOD = "rest_good"
    private const val KEY_HARD = "rest_hard"

    @Volatile
    var defaults: ExerciseTimers.RestDefaults = ExerciseTimers.RestDefaults()
        private set

    fun load(context: Context) {
        val p = prefs(context)
        defaults = ExerciseTimers.RestDefaults(
            easy = p.getInt(KEY_EASY, ExerciseTimers.HARDCODED_EASY),
            good = p.getInt(KEY_GOOD, ExerciseTimers.HARDCODED_GOOD),
            hard = p.getInt(KEY_HARD, ExerciseTimers.HARDCODED_HARD)
        )
    }

    fun save(context: Context, value: ExerciseTimers.RestDefaults) {
        defaults = value
        prefs(context).edit()
            .putInt(KEY_EASY, value.easy)
            .putInt(KEY_GOOD, value.good)
            .putInt(KEY_HARD, value.hard)
            .apply()
    }

    private fun prefs(context: Context): SharedPreferences {
        val current = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (current.contains(KEY_EASY) || current.contains(KEY_GOOD) || current.contains(KEY_HARD)) {
            return current
        }
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        if (!legacy.contains(KEY_EASY) && !legacy.contains(KEY_GOOD) && !legacy.contains(KEY_HARD)) {
            return current
        }
        current.edit()
            .putInt(KEY_EASY, legacy.getInt(KEY_EASY, ExerciseTimers.HARDCODED_EASY))
            .putInt(KEY_GOOD, legacy.getInt(KEY_GOOD, ExerciseTimers.HARDCODED_GOOD))
            .putInt(KEY_HARD, legacy.getInt(KEY_HARD, ExerciseTimers.HARDCODED_HARD))
            .apply()
        return current
    }
}
