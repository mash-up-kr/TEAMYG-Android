package com.teamyg.parfait.core.util.jvm.model

/**
 * 설치된 앱의 버전 이름 — `:app` 의 `BuildConfig.VERSION_NAME` 그대로다.
 *
 * `String` 을 그대로 주입하면 다른 문자열 바인딩과 구별되지 않아 감싼다. `value class` 가 아닌
 * 이유는 Dagger 가 인라인된 시그니처를 `String` 으로 보아 그 구별이 무의미해지기 때문이다.
 *
 * @property value `"1.0.0"` 처럼 온다. 접미사·접두사는 화면이 붙인다
 */
data class AppVersionName(val value: String)
