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
 * 성공/실패 판정을 여기서 한 번 더 하는 이유: HTTP 와 envelope 가 성공이어도 본문이
 * 쓸 수 없는 값일 수 있다. 이후 화면들이 [CreatedGroupVO.groupId] 로 API 를 부르므로
 * **`groupId > 0` 을 이 UseCase 의 성공 조건**으로 못 박는다 — 화면마다 다시 검사하면
 * 한 곳이라도 빠졌을 때 0 인 ID 로 다음 요청이 나간다.
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
            // 서버 계약 위반이지 네트워크/비즈니스 실패가 아니므로 Unexpected 로 둔다
            return Result.failure(
                AppError.Unexpected(
                    IllegalStateException("그룹 생성 응답의 groupId 가 유효하지 않다: ${createdGroup.groupId.value}"),
                ),
            )
        }

        return Result.success(createdGroup)
    }

    private companion object {
        /** 이 값 이하의 `groupId` 는 서버가 값을 못 채운 것으로 본다 */
        const val INVALID_GROUP_ID_THRESHOLD = 0L
    }
}
