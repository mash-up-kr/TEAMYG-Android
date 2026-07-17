package com.teamyg.parfait

import com.teamyg.parfait.core.util.jvm.analytics.Logger
import com.teamyg.parfait.core.util.jvm.analytics.Loggers

val fcmLogger: Logger by lazy {
    Loggers.create("FCM")
}
val tokenLogger: Logger by lazy {
    Loggers.create("token")
}
