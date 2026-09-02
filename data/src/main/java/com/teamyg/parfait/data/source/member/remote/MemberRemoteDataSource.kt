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
     * 소비 측은 ApiException.Business.statusCode 로 두 상황을 구분할 수 있다 — 401/404 는
     * Retrofit 의 HttpException 경로를 타고, ApiCaller.toApiException 이 HTTP status 를
     * statusCode 에 채워 넣는다.
     */
    suspend fun getMyAccount(): Result<MyAccountVO>

    /**
     * 전역 닉네임을 바꾼다. 이미 참여한 그룹의 그룹 닉네임은 바뀌지 않는다 — 별도 컬럼이고
     * 서버가 이 API 에서 건드리지 않는다(`api/member.md`).
     */
    suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>

    /**
     * 회원 탈퇴. 되돌릴 수 없다.
     *
     * 서버가 회원 행을 지우고 참여 중인 모든 그룹 멤버십을 탈퇴 처리하며(그룹 닉네임이
     * "(알수없음)"으로 바뀐다) 커밋 후 refresh token 을 정리한다. 다만 그 회원이 올린
     * 토핑은 캔버스에 남는다(`api/member.md`).
     *
     * 성공 응답에 envelope 가 없다(204) — 서버 전체에서 logout 과 이 API 둘뿐이다.
     */
    suspend fun withdraw(): Result<Unit>
}
