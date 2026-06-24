package com.teamyg.parfait.data.utils

import co.touchlab.kermit.Logger

internal val repoLogger: Logger by lazy {
    Logger.withTag("Repository")
}

internal val sourceLogger: Logger by lazy {
    Logger.withTag("Source")
}
