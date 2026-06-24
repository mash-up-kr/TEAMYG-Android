package com.teamyg.parfait.core.ui

import co.touchlab.kermit.Logger

val vmLogger: Logger by lazy {
    Logger.withTag("ViewModel")
}

val screenLogger: Logger by lazy {
    Logger.withTag("Screen")
}
