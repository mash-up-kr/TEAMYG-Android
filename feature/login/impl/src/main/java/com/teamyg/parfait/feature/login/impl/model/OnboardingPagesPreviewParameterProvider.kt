package com.teamyg.parfait.feature.login.impl.model

import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

class OnboardingPagesPreviewParameterProvider : NamedPreviewParameterProvider<List<OnboardingPage>>() {
    override val entries = listOf(
        "default" to listOf(
            OnboardingPage(
                description = "매일 새로운 캔버스에 하루하루 다르게 기록해요",
                painterResourceId = DesignSystemR.drawable.image_onboarding_1,
            ),
            OnboardingPage(
                description = "친구들과 함께 남기고 싶은 순간을 캔버스에 붙여요",
                painterResourceId = DesignSystemR.drawable.image_onboarding_2,
            ),
            OnboardingPage(
                description = "서로의 하루가 겹겹이 쌓여,\n하나의 캔버스로 완성돼요",
                painterResourceId = DesignSystemR.drawable.image_onboarding_1,
            ),
        ),
    )
}
