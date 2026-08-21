package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode

/**
 * 다시 눌러도 영원히 같은 실패인가. 참이면 화면에 잡아 두지 않고 캔버스로 되감는다.
 *
 * `statusCode` 를 보지 않는 것이 결정이다 — 세 코드 모두 status 로 갈려야 하는 동명 코드가
 * 없고, 서버가 200 에 실패 봉투를 실으면 그 값이 `null` 로 와서 조건에 넣는 순간 판정이
 * 사라진다(`specs/2026-08-20-c106-topping-place-api.md` 실패 처리 절).
 */
internal fun AppError.isPermanentPlaceFailure(): Boolean = this is AppError.Server &&
    code in PERMANENT_PLACE_FAILURE_CODES

private val PERMANENT_PLACE_FAILURE_CODES = setOf(
    ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED,
    ServerErrorCode.ParfaitImage.PARFAIT_NOT_FOUND,
    ServerErrorCode.ParfaitGroup.GROUP_NOT_JOINED,
)
