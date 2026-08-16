package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 계정 정보 SSoT 스트림을 돌려준다. 값이 없으면(로그아웃·최초 진입) `null` 을 방출한다.
 *
 * 이름이 `Observe…` 가 아닌 이유: 호출 자체는 **구독하지 않고 [Flow] 만 넘긴다**.
 * 구독 시점은 수집하는 쪽이 정한다. 같은 이유로 Flow 를 돌려주는 선례
 * ([com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase]) 도 `Get…` 이다.
 * 일회성 서버 갱신은 [RefreshMyAccountUseCase] 쪽이라 이름에 `Flow` 를 남겨 구분한다.
 */
class GetMyAccountFlowUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
) {
    operator fun invoke(): Flow<MyAccountVO?> = memberRepository.myAccount
}
