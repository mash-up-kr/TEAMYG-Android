package com.teamyg.parfait.domain.model

import com.teamyg.parfait.core.util.jvm.analytics.Logger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

internal val useCaseLogger: Logger by lazy {
    Loggers.create("UseCase")
}
