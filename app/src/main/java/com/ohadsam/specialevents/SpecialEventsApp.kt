package com.ohadsam.specialevents

import android.app.Application
import com.ohadsam.specialevents.log.AppLogger

class SpecialEventsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        val log = container.logger
        log.purgeOlderThanDays(AppLogger.DEFAULT_RETENTION_DAYS)
        log.i("App", "start v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            log.e("Crash", "uncaught on ${t.name}", e)
            log.flush()
            previous?.uncaughtException(t, e)
        }
    }
}
