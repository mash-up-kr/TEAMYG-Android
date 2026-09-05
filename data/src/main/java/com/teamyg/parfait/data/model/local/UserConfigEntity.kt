package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.member.UserConfigVO
import kotlinx.serialization.Serializable

/**
 * 모든 항목에 기본값을 둔다 — 항목이 늘어난 뒤에도 **이전 버전이 저장해 둔 JSON 이 그대로
 * 읽혀야** 한다. 기본값이 없으면 없는 필드에서 역직렬화가 터지고, 손상분 폐기 규칙에 걸려
 * 이미 끝낸 튜토리얼까지 통째로 초기화된다.
 */
@Serializable
internal data class UserConfigEntity(
    /**
     * [TutorialKind] 가 아니라 이름 문자열로 담는다. enum 으로 두면 **최신 버전이 저장한 값을
     * 구버전이 읽다가** 모르는 항목에서 터지고, 그 폐기가 설정 전체를 날린다. 문자열이면 모르는
     * 항목만 조용히 버리고 나머지는 지킨다.
     */
    val seenTutorials: Set<String> = emptySet(),
)

internal fun UserConfigVO.toEntity(): UserConfigEntity = UserConfigEntity(
    seenTutorials = seenTutorials.mapTo(mutableSetOf(), TutorialKind::name),
)

internal fun UserConfigEntity.toVO(): UserConfigVO = UserConfigVO(
    seenTutorials = seenTutorials.mapNotNullTo(mutableSetOf()) { name ->
        TutorialKind.entries.firstOrNull { it.name == name }
    },
)
