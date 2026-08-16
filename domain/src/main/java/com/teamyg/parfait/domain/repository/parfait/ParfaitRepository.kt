package com.teamyg.parfait.domain.repository.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

interface ParfaitRepository {
    /**
     * 범위 안의 캔버스 목록. 상태로 거르지 않아 오늘의 ACTIVE 캔버스도 함께 온다 —
     * 오늘 캔버스가 이미 있는지 확인하는 데 이 경로를 쓸 수 있다.
     *
     * 범위를 생략하면 서버 기본값(오늘 - 30일 ~ 오늘)이다. from 이 to 보다 늦으면
     * 400 INVALID_DATE_RANGE 다.
     */
    suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<List<PastCanvasVO>>

    /**
     * 특정 캔버스 상세. 오늘 조회(`/parfaits/today`)와 결과 형태가 같지만 부작용이 없다 —
     * 그쪽은 캔버스가 없으면 만들어 저장한다.
     *
     * 파르페가 없거나 다른 그룹 소속이면 PARFAIT_NOT_FOUND 다.
     */
    suspend fun getCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<CanvasVO>
}
