package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import com.teamyg.parfait.core.util.jvm.extension.ElapsedTimeBucket
import com.teamyg.parfait.feature.groups.canvas.impl.R

/**
 * Spotlight 토스트에 쓰는 "3분 전"·"오래전" 같은 완성된 구절. [ElapsedTimeBucket] 갈래별 문구는
 * 여기서만 정한다.
 *
 * 토스트 이펙트는 `LaunchedEffect`(Composable 이 아닌 코루틴 스코프) 안에서 처리되므로
 * `stringResource` 대신 [Context.getString] 을 쓴다.
 */
internal fun ElapsedTimeBucket.toSpotlightTimeLabel(context: Context): String = when (this) {
    ElapsedTimeBucket.JustNow -> context.getString(R.string.canvas_spotlight_time_just_now)
    is ElapsedTimeBucket.Minutes -> context.getString(R.string.canvas_spotlight_time_minutes, value)
    is ElapsedTimeBucket.Hours -> context.getString(R.string.canvas_spotlight_time_hours, value)
    is ElapsedTimeBucket.Days -> context.getString(R.string.canvas_spotlight_time_days, value)
    ElapsedTimeBucket.LongAgo -> context.getString(R.string.canvas_spotlight_time_long_ago)
}
