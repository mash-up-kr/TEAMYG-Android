package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId

/**
 * 토핑 만들기 흐름의 상태 한 벌. 흐름당 하나만 존재한다
 * (`adr/0026-topping-draft-datastore-ssot.md`).
 *
 * 이미지와 테두리가 비어 있는 초안은 흐름에 막 들어선 상태다 — 진입은 캔버스 식별값 셋만
 * 알고, 나머지는 흐름의 뒤 단계가 채운다.
 *
 * @param nextPositionZ 흐름에 들어설 때 못 박은 값이라 그 사이 남이 올린 토핑과 겹칠 수 있다
 *   (`specs/2026-08-20-c106-topping-place-api.md` 결정 표).
 * @param subjectImagePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
 * @param cutoutImagePath 재편집 시작 마스크. 좌표계를 지켜야 해 트리밍하지 않는다.
 * @param borderColorArgb null 이면 테두리가 없다.
 */
data class ToppingDraft(
    val groupId: GroupId,
    val parfaitId: ParfaitId,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
)
