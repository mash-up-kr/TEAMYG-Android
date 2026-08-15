package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.parfait.ParfaitHistory
import kotlinx.coroutines.delay
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import javax.inject.Inject
import kotlin.time.Clock

private const val MOCK_LOAD_DURATION = 300L

/** 달마다 이 날짜들에 파르페가 있는 셈 친다 */
private val MOCK_DAYS_OF_MONTH = listOf(3, 8, 14, 15, 21, 27)

/** 이미지 수를 0~8 로 흩뜨리는 값. 0 이 나온 날은 빈 파르페가 된다 */
private const val MOCK_IMAGE_COUNT_MODULO = 9

private const val MONTHS_IN_YEAR = 12

/**
 * 한 해 동안 그룹이 남긴 파르페 목록을 최신순으로 준다.
 *
 * 서버의 `GET /api/v1/groups/{groupId}/parfaits?from=&to=` 한 번 호출에 대응한다.
 * 연 단위로 끊는 이유는 달력이 연·월을 오가며 그려지기 때문이다 — 월이 바뀔 때마다
 * 다시 부르지 않아도 되고, 연도 선택지도 이 결과에서 그대로 뽑을 수 있다.
 */
class GetParfaitHistoriesUseCase
@Inject
constructor() {
    /**
     * Todo : 서버 연동 시 `groupId` 를 받아 실제 API 를 호출하도록 바꾼다.
     *   지금 화면(`NavKeyCanvasImageAdd`)이 groupId 를 들고 있지 않아 인자에서 뺐다.
     */
    suspend operator fun invoke(year: Int): Result<List<ParfaitHistory>> {
        delay(MOCK_LOAD_DURATION)
        return Result.success(mockHistoriesOf(year))
    }

    /**
     * 오늘까지만 만든다. 아직 오지 않은 날에 기록이 있으면 달력이 미래에 점을 찍는다.
     */
    private fun mockHistoriesOf(year: Int): List<ParfaitHistory> {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        return (MONTHS_IN_YEAR downTo 1)
            .flatMap { month ->
                val lastDay = lastDayOfMonth(year, month)

                MOCK_DAYS_OF_MONTH
                    .filter { day -> day <= lastDay }
                    .map { day -> LocalDate(year, month, day) }
                    .filter { date -> date <= today }
                    .sortedDescending()
                    .map { date -> mockHistoryOf(date) }
            }
    }

    private fun mockHistoryOf(date: LocalDate): ParfaitHistory = ParfaitHistory(
        parfaitId = ParfaitId(date.toEpochDays()),
        date = date,
        // Todo : 서버가 thumbnailUrl 을 아직 항상 null 로 내려 준다
        thumbnailUrl = null,
        // LocalDate.month 에는 이 버전에서 쓸 수 있는 숫자 프로퍼티가 없어 dayOfYear 로 흩뜨린다
        imageCount = date.dayOfYear % MOCK_IMAGE_COUNT_MODULO,
    )

    /** 다음 달 1일에서 하루를 빼 말일을 얻는다 — 윤년을 따로 다루지 않아도 된다 */
    private fun lastDayOfMonth(
        year: Int,
        month: Int,
    ): Int = LocalDate(year, month, 1)
        .plus(DatePeriod(months = 1))
        .minus(DatePeriod(days = 1))
        .day
}
