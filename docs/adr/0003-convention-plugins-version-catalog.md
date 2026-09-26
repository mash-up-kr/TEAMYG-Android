---
id: ADR-0003
title: build-logic 컨벤션 플러그인 + 버전 카탈로그
status: accepted
date: 2026-05-14
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0001, ADR-0004
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0003: build-logic 컨벤션 플러그인 + 버전 카탈로그

## 맥락
다중 모듈([[0001-layered-multi-module]])이면 모듈마다 `build.gradle.kts`에 compileSdk·Java 버전·플러그인·공통 의존을 반복 선언하게 된다. 복붙은 곧 드리프트(모듈마다 설정이 미묘하게 다름)를 낳는다.

## 결정
`build-logic/convention`에 커스텀 Gradle 컨벤션 플러그인을 두고, 각 모듈은 `alias(libs.plugins.*)`로 해당 플러그인만 적용한다. 플러그인 ID 접두사는 `com.teamyg.parfait.plugin.*`.

주요 플러그인:
- `AndroidApplicationConventionPlugin`, `AndroidLibraryConventionPlugin` — compileSdk/minSdk/targetSdk·Java 17 공통.
  🔁 **as-built(2026-08-26, PR #372)** — **릴리즈 빌드 타입이 축소까지 간다.** `setConfigAndroidApplication`의
  `release`에 **`isShrinkResources = true`**와 `proguardFiles`가 붙었다. 그전까지 켜져 있던 것은
  `isMinifyEnabled`뿐이라 **코드만 줄고 리소스는 그대로 실렸다** — 이 스위치가 그 절반을 채운다.
  같은 블록이 `debug`를 **명시적으로 끈다**(`isMinifyEnabled = false`·`isShrinkResources = false`).
  ⚠️ debug에도 `proguardFiles`가 함께 들어갔는데 minify가 꺼져 있어 **아무 일도 안 한다** — 켜져
  있다고 오해하기 쉬운 자리다 → [open-questions](../synthesis/open-questions.md) OQ-P-308 ③.
  ⚠️ 이 플러그인은 `app`과 `app-preview` 둘에 걸리므로 **축소도 둘 다에 걸린다.** 축소의 실패는
  조립이 아니라 실행에서 드러나는데 릴리즈 산출물을 설치해 본 기록은 0건이다(OQ-P-308 ①).
- `AndroidApplicationSigningConventionPlugin` — 서명 키를 `local.properties`에서 로드(`PropertySettingManager`).
  🔁 **as-built(2026-08-16, PR #264)** — `setSigningConfig`이 `signingConfigs`를 만들기만 하던 것에서
  **release 빌드 타입에 release 설정을 결선**하는 데까지 갔다(`buildTypes.getByName("release")`).
  그전까지 release 산출물은 등록만 된 키를 쓰지 않았다. debug 타입은 종전대로 기본 결선을 쓴다.
  🔁 **as-built(2026-08-25, PR #354)** — **키가 없을 때의 거동이 바뀌었다.** `loadReleaseKey`·
  `loadDebugKey`가 `Key?`를 반환해 없으면 `null`이고, 예전처럼 `./error.jks`라는 **없는 경로로
  채우지 않는다** — 가짜 경로를 채우면 AGP가 그것을 그대로 믿어 "어떤 프로퍼티가 비었는가"를
  아무도 말해 주지 않았다. 대신 `failWhenStoreFileMissing`이 `validateSigningRelease`·
  `validateSigningDebug`에 `doFirst`를 얹어 **서명이 실제로 필요한 순간에만** 비어 있는 프로퍼티
  이름과 함께 실패시킨다. 설정 단계에서 터뜨리지 않는 근거는 **CI가 키를 주입하지 않는다**는 것이다
  (`.github/actions/restore-app-secrets`는 카카오 키와 `google-services.json`만 복원한다) — 설정
  단계에서 막으면 ktlint·테스트조차 못 돈다. 두 프로퍼티 키 상수(`YG_RELEASE_STORE_FILE`·
  `YG_DEBUG_STORE_FILE`)가 그 메시지를 만들려고 `internal`에서 공개로 올라갔고, `findProperty` →
  `local.properties` 순회와 `Properties` 로딩이 릴리즈·디버그 공용 함수 하나로 합쳐졌다
  (`loadBaseUrl`도 같은 `localProperties()`를 쓴다). ⚠️ **이 안내가 실제로 발화하는지 확인되지
  않았다** — 태스크 **이름 일치**에 걸려 있어 그 이름의 태스크가 없으면 조용히 아무 일도 하지 않는다
  → [open-questions](../synthesis/open-questions.md) OQ-P-305.
- `JetpackComposeConventionPlugin` — Compose 활성 + Material3 + Coil.
- `DaggerHiltCoreConventionPlugin` / `DaggerHiltComposeConventionPlugin` — Hilt + KSP([[0004-hilt-ksp-di]]).
- `ModuleDataConventionPlugin`, `ModuleDomainConventionPlugin`, `ModuleFeatureApiConventionPlugin`, `ModuleFeatureImplConventionPlugin` — 레이어별 표준 의존·플러그인 묶음.

버전·의존 SoT = `gradle/libs.versions.toml`. `TYPESAFE_PROJECT_ACCESSORS` 활성으로 `projects.feature.login.api` 형태의 타입 안전 모듈 참조 사용.

🔁 **as-built(2026-08-26, PR #372) — 카탈로그에 `androidx-appcompat`이 생긴 이유는 화면이 아니라 lint다.**
`app`이 매니페스트에서 카카오 `AuthCodeHandlerActivity`를 직접 선언하는데(딥링크 스킴을 붙이려고),
그 액티비티의 상위 타입이 `AppCompatActivity`라 **컴파일 클래스패스에 `appcompat`이 없으면 lint
`Instantiatable`이 상속 체인을 못 풀어 릴리즈 빌드가 깨진다.** 런타임 의존은 카카오 SDK가 이미
끌고 오고 있었고(그 액티비티의 테마 `TransparentCompat`도 SDK AAR이 매니페스트 병합으로 넣는다),
이번에 더한 것은 **버전을 우리가 못 박는 컴파일 의존**이다. 즉 화면 코드가 AppCompat을 쓰기
시작한 것이 아니다 — 이 선언을 "AppCompat 도입"으로 읽으면 안 된다.

🔁 **as-built(2026-08-26, PR #374) — 앱 버전이 `appVersionCode` 1 → 3, `appVersionName` 0.0.1 → 0.0.3.**
⚠️ **2는 어느 브랜치에도 없다**(→ [open-questions](../synthesis/open-questions.md) OQ-P-310) —
이 값은 손으로 고치고, 저장소가 가진 유일한 배포 표식은 경량 태그 `0.0.3` 하나다.
📌 **그 뒤로도 카탈로그에서 손으로 올라간다**(2026-09-11, PR #490 — `appVersionCode` 11,
`appVersionName` 1.1.3. 그 앞은 2026-09-10 PR #488 의 10·1.1.3, 다시 그 앞은 PR #483 의 9·1.1.2).
프리뷰 쪽 두 값은 그대로다.
버전만 바꾸는 커밋이 따로 서는 관행이 자리 잡았고, **그 커밋이 곧 릴리즈 브랜치의 팁이다** —
`release/version-1.1.2-9` 와 `release/version-1.1.1-8` 은 각각 그 시점의 `develop` HEAD 그
커밋이라 두 계보가 최신 구간에서는 갈라지지 않는다. `release/version-1.1.3-10`·`-11` 도 각각 #488·#490
머지 커밋이라 둘 다 `develop` 커밋이다.
⚠️ **#490 은 이름을 두고 코드만 올린 첫 커밋이다** — 그 사이에 #489(권한 요청 수정)가 머지돼 `1.1.3` 이
**코드 10(수정 전)·11(수정 후) 두 트리**를 가리키고, 경량 태그 `1.1.3` 은 11 쪽만 가리킨다.
⚠️ **그래도 표식 문제는 남는다**(OQ-P-310) —
다음 올림을 강제하는 것이 없고, 태그는 여전히 경량이라 누가 언제 붙였는지 남지 않는다.

## 대안
- **모듈별 수기 build.gradle.kts** — 진입 장벽 낮음. 그러나 드리프트·중복.
  **→ 기각:** 모듈 수 증가 시 유지 불가.
- **buildSrc** — 컨벤션 공유 가능하나 변경 시 전체 캐시 무효화.
  **→ 기각:** `build-logic` composite build가 빌드 캐시 친화적.

## 영향

**긍정**
- 새 모듈 = 알맞은 컨벤션 플러그인 한 줄. 설정 일관성 보장.
- 버전 상향(Kotlin·AGP·Compose BOM 등)이 카탈로그 한 곳에서.

**트레이드오프**
- 컨벤션 플러그인 자체를 이해해야 신규 참여자가 빌드 흐름을 파악 가능.

**위험·방어**
- 레이어 규칙(누가 무엇에 의존)을 플러그인이 코드로 강제 → 문서 드리프트 방지.
