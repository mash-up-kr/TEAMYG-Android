package com.teamyg.parfait.core.ui

import com.teamyg.parfait.core.util.jvm.analytics.Logger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

val vmLogger: Logger by lazy {
    Loggers.create("ViewModel")
}

val screenLogger: Logger by lazy {
    Loggers.create("Screen")
}
