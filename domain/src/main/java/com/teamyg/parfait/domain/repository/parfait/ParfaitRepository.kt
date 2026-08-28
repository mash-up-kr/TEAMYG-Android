package com.teamyg.parfait.domain.repository.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.canvas.PastCanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import kotlinx.coroutines.flow.Flow
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
     * 오늘 캔버스 구독. 아직 한 번도 못 받았으면 `null` 이다.
     *
     * 값을 얻는 길은 이것 하나뿐이다 — 갱신 함수가 값을 돌려주면 캐시가 곧 두 번째 출처가 된다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     */
    fun todayCanvas(groupId: GroupId): Flow<CanvasVO?>

    /**
     * 구독이 시작된 뒤 오늘 캔버스 갱신이 한 번은 끝났는지. **성공과 실패를 가리지 않는다.**
     *
     * [todayCanvas] 만으로는 첫 조회가 아직 오는 중인지 실패해서 영영 오지 않는지 구분할 수
     * 없다. 실패를 화면에 알리지 않기로 한 구조라 이 신호가 없으면 초기 로딩이 걷히지 않는다
     * (`adr/0029-canvas-today-ssot-polling.md`).
     */
    fun isTodayCanvasSettled(groupId: GroupId): Flow<Boolean>

    /**
     * 오늘 캔버스 캐시를 갱신한다. 호출부가 응답을 기다려야 하는 자리(예: 되감기 직전)에서
     * 쓴다 — 기다리지 않아도 되는 자리는 [requestTodayCanvasRefresh] 를 쓴다.
     *
     * [getCanvasDetail] 과 같은 엔드포인트를 부를 수 있지만 캐시에 싣는다는 점이 다르다 —
     * 지난 날 조회가 오늘 캔버스를 덮지 않도록 표면을 갈라 둔다.
     *
     * ⚠️ [parfaitId] 는 의도 표시일 뿐 실제로 조회할 대상은 아니다 — 실제 대상(오늘 날짜인지,
     * 캐시된 캔버스인지)은 `:data` 의 폴러가 캐시 상태로 정한다.
     */
    suspend fun refreshTodayCanvasDetail(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit>

    /** 세션 종료 정리. `:domain` 이 `:data` 를 볼 수 없어 저장소 표면으로 낸다 */
    fun clearTodayCanvas()

    /**
     * [refreshTodayCanvasDetail] 의 비동기 표면. 즉시 반환하므로 호출부의 되감기를 늦추지
     * 않는다 — 갱신 자체는 폴러의 스코프에서 끝까지 간다(`adr/0029-canvas-today-ssot-polling.md`).
     */
    fun requestTodayCanvasRefresh(groupId: GroupId)

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
     * 특정 캔버스 상세. 부르는 엔드포인트는 [refreshTodayCanvasDetail] 과 같을 수 있지만
     * 부작용이 없다(캐시에 싣지 않는다).
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
