package com.teamyg.parfait.domain.repository.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

interface ParfaitRepository {
    /**
     * 파르페가 하나라도 있는 연도. 하나도 없으면 빈 목록이다.
     *
     * 목록 조회에서 뽑을 수 없어 따로 있는 엔드포인트다 — 그쪽은 범위를 받으므로 어느 해까지
     * 거슬러 물어봐야 하는지 미리 알 수 없다.
     */
    suspend fun getYears(groupId: GroupId): Result<List<Int>>

    /**
     * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
     * (`api/parfait.md`). 화면이 반복 호출하면 빈 캔버스가 양산되므로 호출 지점을 아껴야 한다.
     *
     * 오늘 날짜가 이미 마감돼 있으면 그것을 그대로 돌려준다 — status 가 ACTIVE 가 아닐 수 있다.
     * 그 캔버스에 쓰기를 보내면 409 PARFAIT_ALREADY_CLOSED 로 돌아온다(`api/parfait.md`).
     */
    suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO>

    /**
     * 범위 안의 캔버스 목록. 상태로 거르지 않아 오늘의 ACTIVE 캔버스도 함께 온다.
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
     * 특정 캔버스 상세. [getTodayCanvas] 와 결과 형태가 같지만 부작용이 없다.
     *
     * 파르페가 없거나 다른 그룹 소속이면 PARFAIT_NOT_FOUND 다.
     */
    suspend fun getCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<CanvasVO>

    /**
     * 반환값이 필요한 이유: 이미지 배경은 보낼 때 imageId, 받을 때 URL 이라 앱은 방금 저장한
     * 배경의 주소를 이 응답으로만 알 수 있다. 앱이 모르는 type 이 오면 조회와 같은 규칙으로
     * null 이다 — 저장은 됐지만 그릴 수 없다는 뜻이다.
     *
     * ⚠️ 서버가 캔버스 상태를 보지 않아 마감된 캔버스의 배경도 바뀐다 — 막는 것은 화면 책임이다.
     */
    suspend fun changeCanvasBackground(
        groupId: GroupId,
        parfaitId: ParfaitId,
        background: CanvasBackgroundEdit,
    ): Result<CanvasBackground?>
}
