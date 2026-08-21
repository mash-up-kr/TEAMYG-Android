package com.teamyg.parfait.core.util.jvm.extension

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private const val LONG_AGO_DAYS = 7

/**
 * 두 시점의 차이를 "얼마나 지났는지" 갈래로 나눈다. 문구는 호출부가 붙인다
 */
sealed interface ElapsedTimeBucket {
    /** 1분 미만. 몇 초인지는 보여주지 않아 값이 없다 */
    data object JustNow : ElapsedTimeBucket

    data class Minutes(val value: Int) : ElapsedTimeBucket

    data class Hours(val value: Int) : ElapsedTimeBucket

    data class Days(val value: Int) : ElapsedTimeBucket

    /** [LONG_AGO_DAYS]일 이상 지났다. 정확한 날짜 대신 뭉뚱그려 보여준다 */
    data object LongAgo : ElapsedTimeBucket
}

/**
 * 기기 시계가 서버보다 뒤처져 미래 시각이 들어오면 [ElapsedTimeBucket.JustNow] 로 본다 —
 * 음수 경과를 그대로 보여주는 것보다 낫다.
 */
fun Instant.toElapsedTimeBucket(now: Instant): ElapsedTimeBucket {
    val elapsed = (now - this).coerceAtLeast(Duration.ZERO)

    return when {
        elapsed < 1.minutes -> ElapsedTimeBucket.JustNow
        elapsed < 1.hours -> ElapsedTimeBucket.Minutes(elapsed.inWholeMinutes.toInt())
        elapsed < 1.days -> ElapsedTimeBucket.Hours(elapsed.inWholeHours.toInt())
        elapsed < LONG_AGO_DAYS.days -> ElapsedTimeBucket.Days(elapsed.inWholeDays.toInt())
        else -> ElapsedTimeBucket.LongAgo
    }
}
