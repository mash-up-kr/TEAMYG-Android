package com.teamyg.parfait.core.util.jvm.analytics

import co.touchlab.kermit.Logger
import co.touchlab.kermit.platformLogWriter

object LoggerInitializer {
    fun setupDebug() {
        Logger.setLogWriters(platformLogWriter())
    }
}
