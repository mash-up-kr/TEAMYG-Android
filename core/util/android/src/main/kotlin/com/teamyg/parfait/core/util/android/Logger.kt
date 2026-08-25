package com.teamyg.parfait.core.util.android

import com.teamyg.parfait.core.util.jvm.analytics.Logger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

internal val coreUtilAndroidLogger: Logger by lazy {
    Loggers.create(tag = "CoreUtilAndroid")
}
