package com.teamyg.parfait.feature.common.terms.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 주소 하나를 웹뷰로 여는 화면. 무엇을 여는지는 부르는 쪽이 이 두 값으로 정한다 — 열 대상마다
 * 목적지를 나누지 않는다.
 *
 * 지금 오는 곳은 약관(이용약관·개인정보 처리방침)뿐이고, 두 값은 `GET /api/v1/policies` 가 준
 * 것을 그대로 싣는다. 이 화면은 스스로 조회하지 않는다 — 이미 목록을 들고 있는 화면만 여기로
 * 오기 때문이다.
 *
 * @property title 상단바에 그대로 걸린다
 * @property url 열 주소. ⚠️ 약관의 경우 서버가 URL 전용 컬럼이 아니라 본문 컬럼을 그대로 매핑해
 *   주므로 링크가 아닐 수 있다(`http/policy.http` 참고). 그때는 웹뷰가 로드에 실패해 재시도
 *   화면을 띄운다
 */
@Serializable
data class NavKeyWebView(
    val title: String,
    val url: String,
) : NavKey
