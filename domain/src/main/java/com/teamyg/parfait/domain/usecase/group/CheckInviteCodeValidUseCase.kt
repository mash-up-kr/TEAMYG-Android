package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.InviteCodeResult
import kotlinx.coroutines.delay
import javax.inject.Inject

class CheckInviteCodeValidUseCase
@Inject
constructor() {
    suspend operator fun invoke(): InviteCodeResult {
        // Todo : 검증 및 에러처리도 추후 추가 예정
        delay(100)
        return InviteCodeResult(
            isSuccess = true,
            errorMessage = null,
            // Todo : 초대코드가 가리키는 그룹명도 서버에서 받아오도록 변경 필요, 지금은 mock 값입니다
            groupName = "모카의 파르페",
        )
    }
}
