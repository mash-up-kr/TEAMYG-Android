package com.teamyg.parfait.domain.usecase.deeplink

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import com.teamyg.parfait.domain.repository.deeplink.AppLinkReferrerRepository
import com.teamyg.parfait.domain.usecase.session.HasActiveSessionUseCase
import javax.inject.Inject

/**
 * 세션이 이미 있는 기기는 조회하지 않고 "확인함"만 남긴다 — Install Referrer 는 설치 후 약
 * 90일 남아 있어서, 이 기능이 들어간 버전으로 업데이트한 기존 유저가 한 번 초대코드 화면으로
 * 끌려갈 수 있기 때문이다.
 */
class ConsumeInstallReferrerDeepLinkUseCase @Inject constructor(
    private val appLinkReferrerRepository: AppLinkReferrerRepository,
    private val hasActiveSession: HasActiveSessionUseCase,
) {
    suspend operator fun invoke(): AppLinkDeepLink? = if (hasActiveSession()) {
        appLinkReferrerRepository.skipLookup()
        null
    } else {
        appLinkReferrerRepository.consumeDeepLinkOnce()
    }
}
