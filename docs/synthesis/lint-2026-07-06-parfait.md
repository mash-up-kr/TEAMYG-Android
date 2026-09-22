---
tags: [lint, maintenance, parfait]
updated: 2026-07-06
---

# Lint 보고서 2026-07-06 — parfait 내용 정합성

[[lint-2026-07-06]]의 후속. parfait 서브트리(ADR 9 + architecture 4)의 **문서 주장 vs 실제 TJYG-Android 코드** 대조. 코드 3개 클러스터를 병렬 검증.

- 코드: `TJYG-Android` (private submodule `project-paths.md`의 절대경로)
- 방법: 각 문서의 검증가능한 주장 추출 → grep/파일 확인으로 코드 대조.

## 링크 · 상태표 · 규율 ✅
- **wikilink/mdlink 무결성**: 깨진 링크 0건.
- **ADR 상태표**: `adr/README.md` 인덱스(9건 accepted + 날짜)와 각 ADR 헤더 상태·날짜 전부 일치.
- **라인번호·변동수치 금지 규율**: 위반 0건. 전 문서 파일명+심볼명 기반 서술. `module-structure.md`는 수치 대신 측정용 grep 안내(모범).

## 민감 데이터 ⚠️
- (없음) — 실명·키·경로 노출 없음. 코드 절대경로는 private submodule에만 존재.

## 코드 대조 발견 🟠 (중간 — 수정 권고)

### 1. `architecture/module-structure.md` — `feature/app/setting` 모듈 누락 (stale)
- 실제: `settings.gradle.kts`에 `:feature:app:setting:api/impl` 등록, `NavKeyAppSetting`·`AppSettingRoute` 등 구현 존재.
- 문서: 레이어별 모듈 표가 `feature/{login,segmentation,camera,gallery,intro}` + `feature/groups/*`만 열거, `feature/app/*` 계층 누락.
- 코드에 있는데 문서에 없음 → 트리 어긋남.

### 2. `adr/0002-feature-api-impl-split.md` — `:api` 의존 과소 기술
- 문서: ":api는 Android library + serialization만 적용".
- 실제: `ModuleFeatureApiConventionPlugin`이 `libs.bundles.navigation`(navigation3-runtime/ui, lifecycle-nav-viewmodel)도 의존 추가. NavKey가 navigation3 타입 참조 → 필요.
- "serialization만"은 navigation 번들 누락.

### 3. `adr/0007-compose-material3-design-tokens.md` — 토큰 심볼명 불일치
- 문서: `ColorSystem`(Primary/Secondary/Tertiary), `TypographySystem`.
- 실제: 심볼명은 `YGSemanticColors`(Primary/Secondary/Tertiary 보유), `YGTypography`. 개념은 맞으나 **문서가 명명한 클래스명이 코드에 없음** → 심볼 대조 실패.

## 코드 대조 발견 (낮음)

### 4. `adr/0007` — 토큰 단수/복수 혼동
- 문서: `SizeToken/ShapeToken/GapToken/PaddingToken`(단수).
- 실제: 단수형 `SizeToken`은 `@JvmInline value class`(개별 값), 토큰 모음 object는 복수형 `SizeTokens/ShapeTokens/GapTokens/PaddingTokens`. Shape/Gap/Padding은 단수 클래스 없음.

### 5. `adr/0001`·`0002` — feature 평면 묘사 vs 중첩 구조
- feature를 `feature/*` 평면으로 묘사하나 실제 `feature/app/setting`, `feature/groups/*`처럼 2~3단계 중첩(발견 1과 동일 근거). ADR 본문은 개념 설명이라 경미.

### 6. `adr/0002` — `:impl`의 `:api` 의존 서술 오해 소지
- ":impl은 :api와 core·domain에 의존"이 컨벤션 플러그인 차원처럼 읽힘. 실제 `ModuleFeatureImplConventionPlugin`은 core+domain만, 자기 `:api`는 각 impl `build.gradle.kts`에서 개별 선언(`projects.feature.<x>.api`).

### 7. `adr/0006`·`architecture/navigation-flow.md`·`state-management.md` — entry builder 시그니처 축약
- 문서: `featureLoginEntryBuilder()`(무인자).
- 실제: `EntryProviderScope<NavKey>.featureLoginEntryBuilder(navigator: Navigator)`. 축약 표기로 미세 불일치.

## 참고 (문서 오류 아님)
- **코드 측 잔재**: `feature/splash/` 폴더 존재하나 `settings.gradle.kts` 미등록·소스 없음. 스플래시는 `feature:intro:api`의 `NavKeySplash`로 구현. (문서엔 splash 언급 없어 문서 오류 아님. 코드 정리 대상.)
- **캐시 축출 이원**: `0008`/`data-layer`가 시간 윈도우 축출만 서술하나, 실제 개수 제한(`MAX_SIZE`, `takeLast`)+시간 윈도우(`clearOutsideDayWindow`) 두 경로. 모순은 아니나 문서에 개수 경로 미명시.
- **코드 스멜**: `LoginSideEffect.NavigateToNext`가 `data object` 아닌 일반 `class`(매번 새 인스턴스). 문서·동작 문제 없음.

## 검증되어 일치 (대표)
BaseViewModel<S,I,E>·MVI 3인터페이스·`@HiltViewModel` 8개·Navigator(@ActivityRetainedScoped·백스택 가드)·`@IntoSet` 멀티바인딩·Navigation3 `1.2.0-alpha04`·NavEntry 데코레이터 3종·DataStore(Preferences)·Room 미채택·UseCase `operator invoke` 8개·컨벤션 플러그인 11종·플러그인 접두사 `com.teamyg.parfait.plugin.*`·`YGMaterialTheme`(dynamic color) — 전부 코드와 부합.

## 조치 요약
| 발견 | 심각도 | 권고 |
|---|---|---|
| module-structure feature/app/setting 누락 | 🟠 | 표에 `feature/app/*` 추가 |
| ADR-0002 :api serialization만 | 🟠 | navigation 번들 의존 명시 |
| ADR-0007 토큰 심볼명 | 🟠 | `YGSemanticColors`/`YGTypography`로 정정 |
| 4~7 (낮음) | 🟡 | 정정 시 함께 반영 |
| 링크·상태표·규율·민감데이터 | ✅ | 문제 없음 |

## 조치 완료 (2026-07-06)
- ✅ **1**: `module-structure.md` 레이어 표에 `feature/app/setting/{api,impl}` 행 추가.
- ✅ **2**: `ADR-0002` :api 서술에 `libs.bundles.navigation`(navigation3) 의존 명시.
- ✅ **3**: `ADR-0007` 토큰 심볼명 `ColorSystem`/`TypographySystem` → `YGSemanticColors`/`YGTypography`로 정정.
- ✅ **4**(낮음, 같은 라인이라 함께): 토큰 단수/복수 정정 — 모음 object 복수형 `SizeTokens` 등, 개별 값은 `SizeToken` value class. `module-structure.md` 토큰 예시도 `ColorSystem` → `YGSemanticColors`·`SizeTokens`로 정정.
- 미조치(정보성): 5·6·7(경미한 서술 축약), feature/splash 잔재 폴더(코드 측), 캐시 축출 이원 경로 미명시.
