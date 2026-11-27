package com.teamyg.parfait.data.model.entity

import kotlinx.serialization.Serializable

/** 초안의 저장 형태. 값 클래스를 품고 있어 domain 이 직렬화를 알게 하지 않는다(`docs/adr/0001-layered-multi-module.md`) */
// isSolo 가 없는 옛 JSON 은 그룹 초안으로 읽힌다
@Serializable
internal data class ToppingDraftEntity(
    val groupId: Long? = null,
    val parfaitId: Long? = null,
    val isSolo: Boolean = false,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val sourceLongSide: Int? = null,
)
