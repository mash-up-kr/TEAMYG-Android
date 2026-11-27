package com.teamyg.parfait.domain.repository.solo

import kotlinx.datetime.LocalDate

/**
 * 혼자 공간 빈 캔버스 안내 연출을 어느 날짜까지 재생했는지 기억한다.
 *
 * 계정과 무관하게 기기에 하나다 — 혼자 공간은 로그인 없이 들어온다. 비우기·삭제와도 무관하게 남는다.
 */
interface SoloIntroRepository {
    suspend fun playedDate(): LocalDate?

    suspend fun markPlayed(date: LocalDate)
}
