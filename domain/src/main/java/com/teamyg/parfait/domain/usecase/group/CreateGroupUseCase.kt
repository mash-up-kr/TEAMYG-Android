package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

/**
 * `POST /api/parfait-groups` 로 그룹을 만든다.
 *
 * envelope 가 성공이어도 본문이 쓸 수 없는 값일 수 있어 `groupId > 0` 을 성공 조건으로
 * 못 박는다 — 화면마다 다시 검사하면 한 곳만 빠져도 0 인 ID 로 다음 요청이 나간다.
 */
class CreateGroupUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO> {
        val createResult = parfaitGroupRepository.createGroup(
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = memberLimit,
        )
        val createdGroup = createResult.getOrElse { return Result.failure(it) }

        if (createdGroup.groupId.value <= INVALID_GROUP_ID_THRESHOLD) {
            // 네트워크·비즈니스 실패가 아니라 서버 계약 위반이라 Unexpected 로 둔다
            return Result.failure(
                AppError.Unexpected(
                    IllegalStateException("그룹 생성 응답의 groupId 가 유효하지 않다: ${createdGroup.groupId.value}"),
                ),
            )
        }

        return Result.success(createdGroup)
    }

    private companion object {
        const val INVALID_GROUP_ID_THRESHOLD = 0L
    }
}
