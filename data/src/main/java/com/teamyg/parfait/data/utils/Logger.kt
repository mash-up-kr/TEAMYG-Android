package com.teamyg.parfait.data.utils

import com.teamyg.parfait.core.util.jvm.analytics.Logger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

internal val repositoryLogger: Logger by lazy {
    Loggers.create("Repository")
}

internal val sourceLogger: Logger by lazy {
    Loggers.create("Source")
}
