package com.teamyg.parfait.feature.login.impl.model

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.feature.login.impl.R

class OnboardingPagesPreviewParameterProvider : PreviewParameterProvider<List<OnboardingPage>> {
    override val values: Sequence<List<OnboardingPage>>
        get() = sequenceOf(
            listOf(
                OnboardingPage(
                    title = "매일 다르게 채우는 하루",
                    description = "매일 새로운 캔버스에 하루하루 다르게 기록해요",
                    painterResourceId = R.drawable.image_onboarding_1,
                ),
                OnboardingPage(
                    title = "평범한 일상이 토핑으로",
                    description = "오늘 찍은 사진을 친구들과 함께 캔버스에 붙여요",
                    painterResourceId = R.drawable.image_onboarding_2,
                ),
                OnboardingPage(
                    title = "완성된 하나의 파르페",
                    description = "서로의 하루가 겹겹이 쌓여,\n하나의 캔버스로 완성돼요",
                    painterResourceId = null,
                ),
            ),
        )
}
