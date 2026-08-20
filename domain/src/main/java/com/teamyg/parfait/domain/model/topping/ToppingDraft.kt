package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId

/**
 * 토핑 만들기 흐름의 상태 한 벌. 흐름당 하나만 존재하고, 이미지와 테두리가 빈 초안은 흐름에
 * 막 들어선 상태다(`adr/0026-topping-draft-datastore-ssot.md`).
 *
 * @param subjectImagePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
 * @param cutoutImagePath 재편집 시작 마스크. 좌표계를 지켜야 해 트리밍하지 않는다.
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
