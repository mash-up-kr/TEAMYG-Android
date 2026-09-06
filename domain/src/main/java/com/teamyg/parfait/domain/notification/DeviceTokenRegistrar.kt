package com.teamyg.parfait.domain.notification

/**
 * 등록을 **걸어만 두고 곧장 돌아온다.** 부르는 자리가 로그인·가입·앱 진입이라 사용자가
 * 스피너를 보고 있고, 등록 결과로 그 화면이 달라질 것도 없다.
 */
interface DeviceTokenRegistrar {
    fun register()
}
