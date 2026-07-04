package com.orbitflux.android

import android.content.Context
import android.util.Log

object CrashLogger {
    private const val PREFS_NAME = "fluxcore_ops"
    private const val KEY_LAST_CRASH = "last_crash"

    fun readAndClear(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_LAST_CRASH, null)
        if (!value.isNullOrBlank()) {
            prefs.edit().remove(KEY_LAST_CRASH).apply()
        }
        return value
    }

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dump = buildString {
                    append("thread=")
                    append(thread.name)
                    append('\n')
                    append(Log.getStackTraceString(throwable))
                }
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_CRASH, dump)
                    .apply()
            } catch (_: Throwable) {
                // Intentionally ignored to avoid masking original crash.
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }
}
