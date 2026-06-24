package com.teamyg.parfait.domain.model

import co.touchlab.kermit.Logger

internal val useCaseLogger: Logger by lazy {
    Logger.withTag("UseCase")
}
