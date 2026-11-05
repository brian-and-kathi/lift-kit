package com.liftkit

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** Private copy of the About log/stats page so the phone can draw it without waiting on the network. */
object DashboardCache {
    private const val MARKER = "id=\"weekStrip\""
    private val refreshing = AtomicBoolean(false)
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun pageUrl(profileUrl: String): String = "${profileUrl.trimEnd('/')}/?app=1&view=totals"

    fun read(context: Context, profileUrl: String): String? {
        val file = file(context, profileUrl)
        if (!file.isFile || file.length() == 0L) return null
        return try {
            synchronized(this) { file.readText() }
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        context.applicationContext.cacheDir.listFiles()
            ?.filter { it.name.startsWith("dashboard-") }
            ?.forEach { it.delete() }
    }

    /** Fetch ahead of the About screen. A refresh already in progress is left to finish. */
    fun prefetch(context: Context) {
        val app = context.applicationContext
        val url = WorkerRemote.getUrl(app) ?: return
        if (!refreshing.compareAndSet(false, true)) return
        thread(name = "dashboard-cache") {
            try {
                refresh(app, url)
            } finally {
                refreshing.set(false)
            }
        }
    }

    fun refresh(context: Context, profileUrl: String): String? {
        val token = WorkerRemote.getToken(context)
        val request = Request.Builder().url(pageUrl(profileUrl)).apply {
            if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
        }.build()
        val html = try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.string()
            }
        } catch (_: Exception) {
            return null
        } ?: return null
        if (!html.contains(MARKER)) return null
        write(file(context, profileUrl), html)
        return html
    }

    private fun file(context: Context, profileUrl: String): File {
        val slug = profileUrl.trimEnd('/').substringAfterLast('/')
            .filter { it.isLetterOrDigit() || it == '-' }
            .ifBlank { "profile" }
        return File(context.applicationContext.cacheDir, "dashboard-$slug.html")
    }

    private fun write(file: File, html: String) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        synchronized(this) {
            tmp.writeText(html)
            if (file.exists() && !file.delete()) {
                file.writeText(html)
                tmp.delete()
                return
            }
            if (!tmp.renameTo(file)) {
                file.writeText(html)
                tmp.delete()
            }
        }
    }
}
