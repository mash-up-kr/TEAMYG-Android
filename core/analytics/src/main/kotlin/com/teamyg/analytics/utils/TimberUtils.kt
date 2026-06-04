package com.teamyg.analytics.utils

import timber.log.Timber

object TimberUtils {
    internal var isDebug: Boolean = false

    fun setPlant(isDebug: Boolean) {
        if (isDebug) {
            Timber.plant(Timber.DebugTree())
            this.isDebug = true
        }
    }
}
