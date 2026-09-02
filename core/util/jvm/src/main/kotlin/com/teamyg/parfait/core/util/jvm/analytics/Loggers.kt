package com.teamyg.parfait.core.util.jvm.analytics

import co.touchlab.kermit.Logger as KermitLogger

object Loggers {
    fun create(tag: String = ""): Logger = KermitLoggerImpl(
        delegate = KermitLogger.withTag(tag),
    )
}
