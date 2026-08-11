package com.teamyg.parfait.data.source.member.remote

import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO

interface MemberRemoteDataSource {
    /**
     * 토큰이 가리키는 회원의 계정 정보를 읽는다.
     *
     * MEMBER_NOT_FOUND 가 401(전역 인증 필터)과 404(서비스) 둘 다로 올 수 있다.
     * code 문자열만으로 분기하면 두 상황이 뭉개지므로, 이 계층은 번역하지 않고
     * ApiException.Business 로 그대로 흘린다.
     */
    suspend fun getMyAccount(): Result<MyAccountVO>

    /**
     * 전역 닉네임을 바꾼다. 이미 참여한 그룹의 그룹 닉네임은 바뀌지 않는다 — 별도 컬럼이고
     * 서버가 이 API 에서 건드리지 않는다(`api/member.md`).
     */
    suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>
}
