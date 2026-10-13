package com.teamyg.parfait.data.installer.deeplink

/**
 * Install Referrer 조회 결과. 값이 없다는 확정 응답과 일시 실패를 가르는 이유는, 앞의 둘만
 * "확인했다"로 기록해야 일시 실패가 복원 기회를 영구히 없애지 않기 때문이다.
 */
sealed interface ReferrerLookup {
    /** 스토어가 실어 보낸 raw referrer 문자열(예: `inviteCode=ABC123`). */
    data class Found(val referrer: String) : ReferrerLookup

    /** 이 기기에서는 다시 물어도 값이 없다(지원하지 않는 Play 스토어 등). */
    data object NotAvailable : ReferrerLookup

    /** Play 서비스 연결·조회가 일시적으로 실패했다. 다음 실행에서 다시 물을 수 있다. */
    data object TransientFailure : ReferrerLookup
}
