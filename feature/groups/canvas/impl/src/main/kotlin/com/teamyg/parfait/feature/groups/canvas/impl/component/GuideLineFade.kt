package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * 위치·각도 가이드선이 공유하는 페이드 스펙
 * [CenterGuideLines] 와 [AngleGuideLines] 가 같은 값으로 뜨고 사라지도록 한 곳에 둔다.
 */
private const val GUIDE_FADE_IN_MS = 100
private const val GUIDE_FADE_OUT_MS = 150

internal fun guideLineFadeIn(): EnterTransition =
    fadeIn(tween(durationMillis = GUIDE_FADE_IN_MS, easing = LinearOutSlowInEasing))

internal fun guideLineFadeOut(): ExitTransition =
    fadeOut(tween(durationMillis = GUIDE_FADE_OUT_MS, easing = LinearOutSlowInEasing))
