package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.member.UserConfigVO
import kotlinx.serialization.Serializable

/**
 * 모든 항목에 기본값을 둔다. 없으면 이전 버전이 저장한 JSON 에서 역직렬화가 터지고, 손상분
 * 폐기 규칙에 걸려 이미 끝낸 튜토리얼까지 초기화된다.
 */
@Serializable
internal data class UserConfigEntity(
    /**
     * [TutorialKind] 가 아니라 이름 문자열로 담는다. enum 이면 구버전이 최신 버전의 값을 읽다가
     * 모르는 항목에서 터져 설정 전체가 폐기된다. 문자열이면 모르는 항목만 버린다.
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
