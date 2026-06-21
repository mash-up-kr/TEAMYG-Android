package com.teamyg.parfait.domain.usecase.splash

import kotlinx.coroutines.delay
import javax.inject.Inject

class SplashInitialUseCase
@Inject
constructor() {
    suspend operator fun invoke() {
        delay(1000L)
    }
}
