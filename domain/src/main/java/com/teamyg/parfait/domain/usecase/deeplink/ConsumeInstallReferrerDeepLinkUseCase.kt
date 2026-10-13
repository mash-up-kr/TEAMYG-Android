package com.teamyg.parfait.domain.usecase.deeplink

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import com.teamyg.parfait.domain.repository.deeplink.AppLinkReferrerRepository
import javax.inject.Inject

class ConsumeInstallReferrerDeepLinkUseCase @Inject constructor(
    private val appLinkReferrerRepository: AppLinkReferrerRepository,
) {
    suspend operator fun invoke(): AppLinkDeepLink? = appLinkReferrerRepository.consumeDeepLinkOnce()
}
