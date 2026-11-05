package com.liftkit

import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseJsonTest {
    @Test
    fun oldLogWithoutRestLabelCanBeCopied() {
        val json = """[{"title":"Bench","weight":135,"reps":8,"sets":3,"notes":"","completedSets":1,"rating":"good"}]"""
        val type = object : TypeToken<List<Exercise>>() {}.type
        val loaded: List<Exercise> = Converters.exerciseGson.fromJson(json, type)
        val planned = loaded.map { it.copy(completedSets = 0) }
        assertEquals(0, planned[0].completedSets)
        assertEquals("", planned[0].restSourceLabel)
        assertEquals(1, loaded[0].completedSets)
        assertEquals("solid", loaded[0].rating)
        assertEquals("medium", loaded[0].restPace)
    }

    @Test
    fun storedPaceSurvivesAndOldHardRestStaysLong() {
        val json = """[{"title":"Row","weight":95,"reps":10,"sets":3,"notes":"","rating":"hard","restPace":"short"}]"""
        val type = object : TypeToken<List<Exercise>>() {}.type
        val loaded: List<Exercise> = Converters.exerciseGson.fromJson(json, type)
        assertEquals("hard", loaded[0].rating)
        assertEquals("short", loaded[0].restPace)
        assertEquals(ExerciseTimers.HARDCODED_EASY, loaded[0].cooldownSeconds(null))
    }
}
