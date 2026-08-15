package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.feature.groups.list.impl.R
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * 문구가 아니라 갈래만 고르는 이유: 단위마다 문자열 리소스가 다른데 ViewModel 은
 * `Context` 를 들지 않는다. 문구는 [toStringResource] 가 화면에서 붙인다.
 */
internal sealed interface GroupTimestamp {
    /** 아직 아무도 토핑을 올리지 않아 잴 기준 시각이 없다 — 조회 실패와는 다른 상태다 */
    data object NoImage : GroupTimestamp

    data object JustNow : GroupTimestamp

    data class Minutes(val value: Int) : GroupTimestamp

    data class Hours(val value: Int) : GroupTimestamp

    data class Days(val value: Int) : GroupTimestamp
}

/**
 * 서버는 오프셋 없는 `LocalDateTime` 을 주므로 [timeZone] 을 붙여 시점으로 만든다.
 * 기기 시계가 서버보다 뒤처져 미래 시각이 들어오면 [GroupTimestamp.JustNow] 로 본다 —
 * 음수 경과를 그대로 보여주는 것보다 낫다.
 */
internal fun LocalDateTime?.toGroupTimestamp(
    now: Instant,
    timeZone: TimeZone,
): GroupTimestamp {
    val uploadedAt = this?.toInstant(timeZone) ?: return GroupTimestamp.NoImage
    val elapsed = now - uploadedAt

    return when {
        elapsed < 1.minutes -> GroupTimestamp.JustNow
        elapsed < 1.hours -> GroupTimestamp.Minutes(elapsed.inWholeMinutes.toInt())
        elapsed < 1.days -> GroupTimestamp.Hours(elapsed.inWholeHours.toInt())
        else -> GroupTimestamp.Days(elapsed.inWholeDays.toInt())
    }
}

@Composable
internal fun GroupTimestamp.toStringResource(): String = when (this) {
    GroupTimestamp.NoImage -> stringResource(R.string.group_list_timestamp_no_image)
    GroupTimestamp.JustNow -> stringResource(R.string.group_list_timestamp_just_now)
    is GroupTimestamp.Minutes -> stringResource(R.string.group_list_timestamp_minutes, value)
    is GroupTimestamp.Hours -> stringResource(R.string.group_list_timestamp_hours, value)
    is GroupTimestamp.Days -> stringResource(R.string.group_list_timestamp_days, value)
}
