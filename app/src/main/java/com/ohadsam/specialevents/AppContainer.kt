package com.ohadsam.specialevents

import android.content.Context
import android.util.Log
import com.ohadsam.specialevents.log.AppLogger
import com.ohadsam.specialevents.log.LogLevel
import java.io.File

/** Manual DI root: one place that builds the app's singletons (no Hilt). Grows by batch. */
class AppContainer(context: Context) {
    val logger = AppLogger(
        file = File(context.filesDir, "logs/app.log"),
        echo = { e ->
            val priority = when (e.level) {
                LogLevel.DEBUG -> Log.DEBUG
                LogLevel.INFO -> Log.INFO
                LogLevel.WARN -> Log.WARN
                LogLevel.ERROR -> Log.ERROR
            }
            Log.println(priority, e.tag, e.message)
        },
    )
}
