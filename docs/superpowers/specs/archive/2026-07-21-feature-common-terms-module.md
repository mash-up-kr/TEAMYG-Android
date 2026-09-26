---
id: feature-common-terms-module
title: 약관/개인정보 화면 :feature:common:terms 모듈 분리 (feature/common shared layer)
status: implemented
category: architecture-spec
platforms: android
verified: 2026-08-18
related_code:
  - settings.gradle.kts
  - NavKeyWebView
  - WebViewRoute
  - WebViewScreen
  - NotionWebView
  - EntryBuilder#featureCommonTermsEntryBuilder
  - AppSettingRoute
  - AppSettingViewModel
related_adr: ADR-0015
related_spec: s004-terms-privacy-webview
related_architecture: module-structure
supersedes:
superseded_by:
tags: [spec, parfait, module, terms, common, s004, a003]
---

# 약관/개인정보 화면 `:feature:common:terms` 모듈 분리

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> 🔁 **as-built 정정 (2026-08-18, develop 머지 #296) — 모듈이 예상한 재사용이 실제로 일어났고, 그 대가로
> 내용물이 줄었다.** 이 분리의 명분("약관 동의 화면에서도 같은 항목을 세부 정보 뷰로 재사용")이 코드가
> 됐다 — 온보딩 약관 동의 화면이 `feature/common/terms/api`만 물고 같은 목적지를 연다. 다만 옮겨 온
> **NavKey 2 + Route/Screen/ViewModel 2세트가 `NavKeyWebView` + `WebViewRoute`/`WebViewScreen` 한 벌로
> 합쳐졌고 ViewModel은 사라졌다**(제목·주소가 인자라 상태가 없다). 모듈 경계·`NotionWebView` 위치·
> `featureCommonTermsEntryBuilder` 배선은 설계 그대로다 →
> [s004 스펙 as-built](2026-07-20-s004-terms-privacy-webview.md).

## 목표

S-004 약관/개인정보 화면(구현: [s004-terms-privacy-webview](2026-07-20-s004-terms-privacy-webview.md))을 `:feature:app:setting`에서 신규
**`:feature:common:terms`** 모듈로 분리한다. **A-003 서비스 이용약관** 화면에서도 동일 항목을
세부 정보 뷰로 재사용해야 하므로, 특정 feature 도메인에 묶이지 않는 공유 위치가 필요하다.

## 범위

- **포함**: NavKey 2종 + Route/Screen/ViewModel 2세트 + `NotionWebView` + EntryBuilder를 신규
  `:feature:common:terms:{api,impl}`로 이동. setting 모듈에서 제거·재배선. ADR·architecture 갱신.
- **제외**:
  - A-003 화면 자체 구현(별도 작업 — 이번엔 재사용 **기반**만 마련).
  - `NotionWebView`의 core 승격(사용처 terms뿐 → common:terms:impl 유지, YAGNI).
  - 동작·UI 변경(순수 이동. 로딩/에러/clipToBounds/onRelease/update 가드 등 기존 동작 그대로).

## 설계

### 1. 신규 모듈

**`:feature:common:terms:api`** — 플러그인 `parfait.module.feature.api`, namespace `com.teamyg.parfait.feature.common.terms.api`
- `NavKeyServiceTerms`, `NavKeyPrivacyPolicy` (setting:api에서 이동, 내용 무변경)

**`:feature:common:terms:impl`** — 플러그인 `parfait.module.feature.impl`, namespace `com.teamyg.parfait.feature.common.terms.impl`, `implementation(projects.feature.common.terms.api)`
- `ServiceTermsRoute`·`PrivacyPolicyRoute`·`ServiceTermsScreen`·`PrivacyPolicyScreen`·`ServiceTermsViewModel`·`PrivacyPolicyViewModel`·`NotionWebView` (setting:impl에서 이동, package 선언만 변경)
- `navigation/EntryBuilder.kt` — `featureCommonTermsEntryBuilder(navigator)`: `entry<NavKeyServiceTerms>` → `ServiceTermsRoute`, `entry<NavKeyPrivacyPolicy>` → `PrivacyPolicyRoute` (setting EntryBuilder에서 두 entry 이관)
- `navigation/NavigationModule.kt` — `@IntoSet @Provides`로 entry builder 공급(setting NavigationModule 선례 복제)
- `res/values/strings.xml` — 신규: 화면 title 2(`terms_service_title`·`terms_privacy_title`) + webview 에러/재시도 2(`terms_webview_error_message`·`terms_webview_retry`)

### 2. 등록·의존

- `settings.gradle.kts`: 신규 `include(":feature:common:terms:api", ":feature:common:terms:impl")` 블록.
- `app/build.gradle.kts`: `implementation(projects.feature.common.terms.api)` + `.impl` 추가. impl의 `@IntoSet` entry가 `MainRoute`의 `Set<EntryProviderScope<NavKey>.(Navigator)->Unit>` 주입에 자동 합류 → 수동 배선 없음.

### 3. setting 모듈 정리

- **setting:api**: `NavKeyServiceTerms.kt`·`NavKeyPrivacyPolicy.kt` 삭제.
- **setting:impl**:
  - 이동 7파일 삭제(`ServiceTermsRoute`·`PrivacyPolicyRoute`·`ServiceTermsScreen`·`PrivacyPolicyScreen`·`ServiceTermsViewModel`·`PrivacyPolicyViewModel`·`NotionWebView`).
  - `build.gradle.kts`: `implementation(projects.feature.common.terms.api)` 추가(AppSetting `goTo`용).
  - `EntryBuilder.kt`: `entry<NavKeyServiceTerms>`·`entry<NavKeyPrivacyPolicy>` 및 관련 import 제거(`NavKeyAppSetting`·`NavKeyAccountInfo` entry만 잔존).
  - `AppSettingViewModel`(SideEffect `NavigateToServiceTerms`/`NavigateToPrivacyPolicy` 유지)·`AppSettingRoute`(`goTo(NavKeyServiceTerms)`/`goTo(NavKeyPrivacyPolicy)` 유지): NavKey import를 `feature.common.terms.api`로 변경.
  - `res/values/strings.xml`: `setting_webview_error_message`·`setting_webview_retry` 삭제(common으로 이관). `setting_item_service_terms`·`setting_item_privacy_policy`는 **AppSetting 리스트 라벨로 유지**.

### 4. 의존 방향 (ADR-0002 준수)

```
app ─▶ feature/common/terms/{api,impl}
feature/app/setting/impl ─▶ feature/common/terms/api   (NavKey goTo)
feature/{A-003 소속}/impl ─▶ feature/common/terms/api   (추후, 동일 패턴)
```
feature→feature 이동은 상대 `:api`(NavKey)만 참조. impl끼리 직접 의존 없음.

## 표시·제어 규칙

동작 무변경. 화면 진입 경로(설정 리스트 탭 → `goTo`)·로딩/에러 폴백·뒤로가기 전부 이동 전과 동일.

## 파일 구성

- **신규 모듈 파일**: `feature/common/terms/api/build.gradle.kts`, `feature/common/terms/impl/build.gradle.kts`, 이동된 kt 9개(NavKey 2 + route/screen/vm/webview 7) + `navigation/EntryBuilder.kt`·`NavigationModule.kt` + `res/values/strings.xml`.
- **수정**: `settings.gradle.kts`, `app/build.gradle.kts`, setting:impl `build.gradle.kts`·`EntryBuilder.kt`·`AppSettingRoute.kt`·`AppSettingViewModel.kt`·`strings.xml`.
- **삭제**: setting:api NavKey 2, setting:impl 이동 7파일.

## 주의 / 열린 질문

- **문자열 중복**: "서비스 이용약관"이 setting(리스트 라벨 `setting_item_service_terms`) + common(화면 title `terms_service_title`) 2모듈에 중복. 모듈 독립성 우선(의도적 허용).
- **파일 이동**: `git mv`로 히스토리 보존, package 선언 `feature.common.terms.*`로 변경.
- **A-003 구현**은 후속 작업. 이 spec은 재사용 기반(모듈)까지만.
- Notion URL(각 VM `State.url` placeholder)은 이동 시 그대로 유지 — 값 정책 변경 없음.
