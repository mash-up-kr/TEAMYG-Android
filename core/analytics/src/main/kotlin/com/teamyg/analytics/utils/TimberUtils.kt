package com.teamyg.analytics.utils

import android.util.Log
import timber.log.Timber

object TimberUtils {
    internal var minPriority: Int = Log.VERBOSE

    fun setPlant(isDebug: Boolean) {
        when (isDebug) {
            true -> {
                Timber.plant(Timber.DebugTree())
                minPriority = Log.VERBOSE
            }

            false -> {
                // TODO Release Plant
                minPriority = Log.ERROR
            }
        }
    }
}
