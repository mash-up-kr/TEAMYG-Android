package com.teamyg.parfait.domain.repository.deeplink

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink

/**
 * 스토어 설치 후 최초 실행 시, 설치 전 클릭했던 링크의 목적지를 복원한다(Android 전용 —
 * Play Install Referrer 는 iOS 에 대응 API가 없다).
 */
interface AppLinkReferrerRepository {
    /**
     * 이 기기에서 한 번도 확인한 적 없으면 Install Referrer 를 조회해 목적지로 바꾼다.
     * 이미 확인했으면(또는 딥링크로 볼 수 없는 값이면) `null` — 앱을 열 때마다 같은 화면으로
     * 되돌리지 않기 위해 딱 한 번만 값을 낸다.
     */
    suspend fun consumeDeepLinkOnce(): AppLinkDeepLink?
}
