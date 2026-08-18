package com.teamyg.parfait.core.util.android

/**
 * 앱의 버전 이름. `"1.0.0"` 처럼 온다 — 접미사·접두사는 화면이 붙인다.
 *
 * `versionName` 은 애플리케이션 모듈의 속성이라 라이브러리 `BuildConfig` 에는 없다. 그래서
 * `:app` 이 쓰는 것과 같은 카탈로그 항목(`appVersionName`)을 이 모듈 빌드에도 심고 여기서 읽는다.
 * 두 곳에 각각 박히지만 출처가 하나라 어긋나지 않는다.
 *
 * ⚠️ `:app` 이 `versionNameSuffix` 나 플레이버로 버전을 갈아 끼우면 이 값은 따라가지 않는다.
 * 그런 빌드가 생기면 `PackageManager` 로 설치된 패키지를 읽는 쪽으로 옮겨야 한다.
 */
const val APP_VERSION_NAME: String = BuildConfig.APP_VERSION_NAME
