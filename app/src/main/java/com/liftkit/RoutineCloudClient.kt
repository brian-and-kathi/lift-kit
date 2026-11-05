package com.liftkit

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object RoutineCloudClient {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    data class Version(
        val id: String,
        val name: String,
        val updatedAt: String
    )

    data class Record(
        val id: String,
        val name: String,
        val updatedAt: String,
        val days: List<ExportRoutineDay>,
        val restDefaults: ExerciseTimers.RestDefaults? = null
    )

    private data class FetchResponse(
        val routine: Record?
    )

    fun fetch(context: Context): Result<Record?> {
        return request(context, "/routines", allowNotFound = true) { body ->
            if (body.isBlank()) return@request null
            parseFetch(body)
        }
    }

    fun save(context: Context, days: List<ExportRoutineDay>): Result<Record> {
        // Fetch first so we can merge unknown fields and preserve restDefaults / extras.
        val existingBody = rawGet(context, "/routines").getOrNull()
        val payload = buildMergedPayload(existingBody, days, RestPrefs.defaults)
        return request(context, "/routines", "PUT", payload) { body ->
            parseRecord(body)
        }
    }

    private fun parseFetch(body: String): Record? {
        val root = JsonParser.parseString(body).asJsonObject
        val routineEl = root.get("routine") ?: return null
        if (routineEl.isJsonNull) return null
        return parseRecordJson(routineEl.asJsonObject)
    }

    private fun parseRecord(body: String): Record {
        val el = JsonParser.parseString(body)
        val obj = if (el.isJsonObject && el.asJsonObject.has("days")) {
            el.asJsonObject
        } else if (el.isJsonObject && el.asJsonObject.has("routine")) {
            el.asJsonObject.getAsJsonObject("routine")
        } else {
            el.asJsonObject
        }
        return parseRecordJson(obj)
    }

    private fun parseRecordJson(obj: JsonObject): Record {
        val gson = RoutineExport.gson()
        val daysType = object : TypeToken<List<ExportRoutineDay>>() {}.type
        val days: List<ExportRoutineDay> = gson.fromJson(obj.get("days"), daysType)
        val restDefaults = obj.getAsJsonObject("restDefaults")?.let {
            ExerciseTimers.RestDefaults(
                easy = it.get("easy")?.asInt ?: ExerciseTimers.HARDCODED_EASY,
                good = it.get("good")?.asInt ?: ExerciseTimers.HARDCODED_GOOD,
                hard = it.get("hard")?.asInt ?: ExerciseTimers.HARDCODED_HARD
            )
        }
        return Record(
            id = obj.get("id")?.asString ?: "",
            name = obj.get("name")?.asString ?: "Routine",
            updatedAt = obj.get("updatedAt")?.asString ?: "",
            days = days,
            restDefaults = restDefaults
        )
    }

    /**
     * Merge local exercise fields into the server JSON so unknown keys are never dropped.
     */
    internal fun buildMergedPayload(
        existingBody: String?,
        localDays: List<ExportRoutineDay>,
        defaults: ExerciseTimers.RestDefaults
    ): String {
        val root = JsonObject()
        val daysArray = JsonArray()

        val existingDays: JsonArray? = try {
            if (existingBody.isNullOrBlank()) null
            else {
                val parsed = JsonParser.parseString(existingBody).asJsonObject
                val routine = parsed.getAsJsonObject("routine") ?: parsed
                routine.getAsJsonArray("days")
            }
        } catch (_: Exception) {
            null
        }

        localDays.forEachIndexed { dayIndex, localDay ->
            val dayObj = JsonObject()
            dayObj.addProperty("dayOfWeek", localDay.dayOfWeek)
            val exercises = JsonArray()
            val existingDay = existingDays?.getOrNull(dayIndex)?.takeIf { it.isJsonObject }?.asJsonObject
            val existingExercises = existingDay?.getAsJsonArray("exercises")
            localDay.exercises.forEachIndexed { exIndex, localEx ->
                val base = existingExercises?.getOrNull(exIndex)
                    ?.takeIf { it.isJsonObject }?.asJsonObject?.deepCopy() ?: JsonObject()
                base.addProperty("title", localEx.title)
                base.addProperty("weight", localEx.weight)
                base.addProperty("reps", localEx.reps)
                base.addProperty("sets", localEx.sets)
                base.addProperty("notes", localEx.notes)
                base.addProperty("easyGoodOrHard", localEx.easyGoodOrHard)
                val pace = localEx.restPace.ifBlank {
                    ExerciseTimers.paceFromRating(localEx.easyGoodOrHard)
                }
                base.addProperty("restPace", pace)
                if (localEx.restSec != null) {
                    base.addProperty("restSec", localEx.restSec)
                } else {
                    base.remove("restSec")
                }
                exercises.add(base)
            }
            dayObj.add("exercises", exercises)
            // Preserve unknown day-level keys.
            existingDay?.entrySet()?.forEach { (key, value) ->
                if (key != "dayOfWeek" && key != "exercises" && !dayObj.has(key)) {
                    dayObj.add(key, value)
                }
            }
            daysArray.add(dayObj)
        }
        root.add("days", daysArray)
        val rest = JsonObject()
        rest.addProperty("easy", defaults.easy)
        rest.addProperty("good", defaults.good)
        rest.addProperty("hard", defaults.hard)
        root.add("restDefaults", rest)

        // Preserve unknown top-level keys from the previous routine.
        try {
            if (!existingBody.isNullOrBlank()) {
                val parsed = JsonParser.parseString(existingBody).asJsonObject
                val routine = parsed.getAsJsonObject("routine") ?: parsed
                routine.entrySet().forEach { (key, value) ->
                    if (key !in setOf("id", "name", "updatedAt", "days", "restDefaults", "routine") && !root.has(key)) {
                        root.add(key, value)
                    }
                }
            }
        } catch (_: Exception) {
        }
        return root.toString()
    }

    private fun JsonArray.getOrNull(index: Int) =
        if (index in 0 until size()) get(index) else null

    private fun rawGet(context: Context, path: String): Result<String> {
        val base = WorkerRemote.getUrl(context)
            ?: return Result.failure(IllegalStateException("No worker configured"))
        val builder = Request.Builder().url("$base$path").get()
        val token = WorkerRemote.getToken(context)
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        return try {
            http.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Result.failure(IllegalStateException(errorMessage(body, response.code)))
                } else Result.success(body)
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun <T> request(
        context: Context,
        path: String,
        method: String = "GET",
        jsonBody: String? = null,
        allowNotFound: Boolean = false,
        parse: (String) -> T
    ): Result<T> {
        val base = WorkerRemote.getUrl(context)
            ?: return Result.failure(IllegalStateException("No worker configured"))
        val builder = Request.Builder().url("$base$path")
        if (jsonBody != null) {
            builder.method(method, jsonBody.toRequestBody(jsonType))
            builder.header("Content-Type", "application/json")
        } else {
            builder.get()
        }
        val token = WorkerRemote.getToken(context)
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return try {
            http.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (allowNotFound && response.code == 404) {
                    Result.success(parse(""))
                } else if (!response.isSuccessful) {
                    val message = errorMessage(body, response.code)
                    Result.failure(IllegalStateException(message))
                } else {
                    Result.success(parse(body))
                }
            }
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        return try {
            val err = RoutineExport.gson().fromJson(body, ErrorBody::class.java)
            err.error?.takeIf { it.isNotBlank() } ?: "Worker returned $code"
        } catch (_: Exception) {
            "Worker returned $code"
        }
    }

    private data class ErrorBody(val error: String?)
}
