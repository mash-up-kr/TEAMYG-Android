package com.teamyg.parfait.domain.usecase.splash

import com.teamyg.parfait.domain.model.useCaseLogger
import kotlinx.coroutines.delay
import javax.inject.Inject

class SplashInitialUseCase
@Inject
constructor() {
    init {
        useCaseLogger.i { "SplashInitialUseCase::init" }
    }

    suspend operator fun invoke() {
        delay(1000L)
    }
}
