package com.teamyg.parfait.data.source.parfait.remote

import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.datetime.LocalDate

interface ParfaitRemoteDataSource {
    suspend fun getYears(groupId: GroupId): Result<List<Int>>

    /**
     * 오늘의 캔버스를 상태·멤버·배경·배치 토핑까지 한 번에 읽는다.
     *
     * ⚠️ 조회인데 서버가 캔버스를 만든다 — 오늘 날짜 파르페가 없으면 생성해 저장한다
     * (`api/parfait.md`). 화면이 반복 호출하면 빈 캔버스가 양산되므로 호출 지점을 아껴야 한다.
     *
     * 오늘 날짜가 이미 마감돼 있으면 그것을 그대로 돌려준다 — status 가 ACTIVE 가 아닐 수 있고,
     * 서버는 마감된 캔버스의 편집도 막지 않으므로 잠그는 것은 화면 책임이다.
     */
    suspend fun getTodayCanvas(groupId: GroupId): Result<CanvasVO>

    /**
     * 과거 캔버스 목록. 범위를 생략하면 서버 기본값(오늘 - 30일 ~ 오늘)이다.
     *
     * 페이지네이션도 범위 상한도 없다 — 넓게 주면 그만큼 전량이 내려온다.
     * from 이 to 보다 늦으면 400 INVALID_DATE_RANGE 다.
     */
    suspend fun getPastCanvases(
        groupId: GroupId,
        from: LocalDate? = null,
        to: LocalDate? = null,
    ): Result<List<PastCanvasVO>>

    /**
     * 특정 캔버스 상세. 결과 형태가 [getTodayCanvas] 와 같다 — 서버가 같은 응답을 쓴다.
     *
     * 조회만 한다(캔버스를 만들지 않는다). 다만 상태로 거르지 않아 오늘의 ACTIVE 캔버스도
     * 이 경로로 얻을 수 있다 — 같은 캔버스를 두 경로로 얻을 수 있고 한쪽만 부작용이 있다.
     *
     * 파르페가 없거나 다른 그룹 소속이면 PARFAIT_NOT_FOUND 다.
     */
    suspend fun getCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<CanvasVO>

    /**
     * 캔버스 배경을 단색 또는 이미지로 바꾸고 저장된 배경을 돌려받는다.
     *
     * 이미지로 바꾸면 앱은 imageId 만 알고 URL 은 모른다 — 그래서 이 반환값이 그릴 값이다.
     * 서버가 앱이 모르는 type 을 돌려주면 조회와 같은 규칙으로 null 이 된다(저장은 됐지만
     * 그릴 수 없다).
     *
     * ⚠️ 서버가 캔버스 상태를 보지 않아 마감된 캔버스의 배경도 바뀐다 — 막는 것은 화면 책임이다.
     * ⚠️ 배경 이미지는 참조 카운트를 올리지 않는다 — 같은 이미지의 토핑을 지우면 배경이 깨진다
     * (`api/parfait.md`).
     */
    suspend fun changeCanvasBackground(
        groupId: GroupId,
        parfaitId: ParfaitId,
        background: CanvasBackgroundEdit,
    ): Result<CanvasBackground?>
}
