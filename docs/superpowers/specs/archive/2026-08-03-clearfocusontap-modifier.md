---
id: clearfocusontap-modifier
title: 배경 탭 포커스 해제 Modifier (clearFocusOnTap)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-04
related_code:
  - ClearFocusOnTap.kt#clearFocusOnTap
  - YGScreen.kt#YGScreen
  - AccountInfoScreen.kt#AccountInfoScreen
  - YGClickable.kt#clickableYGNoRipple
related_adr: ADR-0010
related_spec: designsystem-ygscreen-scaffold, s002-account-info, clickableyg-throttle
related_architecture: design-system, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, util, focus, accessibility]
---

# Spec: 배경 탭 포커스 해제 Modifier (`clearFocusOnTap`)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> ✅ **2026-08-04 develop 머지(PR #192, 브랜치 `feature/#86-app-setting-account-info-screen`)** — `ClearFocusOnTap.kt`는
> 아래 API와 **문자 그대로 일치**(KDoc 한 줄만 추가). 다만 머지 시점 대조에서 **전제 하나가 틀렸다**:
> `YGScreen`의 포커스 결선은 **develop에 도달한 적이 없다.** 도입(`feat: YGScreen focus clear 도입`)과
> 철회(`refactor: focus clear 코드 리뷰 반영`)가 둘 다 이 브랜치 안에서 일어나 같은 PR로 함께 들어왔다.
> 그래서 이 PR의 변경 파일에 `YGScreen.kt`는 **없고**(순수 `Surface` 래퍼 상태가 baseline부터 유지), 아래
> [파일 구성](#파일-구성)의 "결선 제거" 항목은 브랜치 내부 작업이지 develop 델타가 아니다.
> 반대로 결선을 위해 만든 `clickableYGNoRipple` + `clickableYGThrottle`의 `indications: List<Indication>?`
> nullable 일반화는 **철회 후에도 되돌리지 않아 이 PR로 develop에 신설됐다** — 즉 develop 기준으로
> 사용처 0인 공개 API가 새로 생겼다(→ [open-questions](../../../synthesis/open-questions.md) [2026-08-03]).

- **대상 모듈**: `core:util:android` — `focus/`(신규 패키지)
- **첫 실사용처**: `feature/app/setting/impl` `AccountInfoScreen`
- **대체 대상**: `YGScreen`에 결선돼 있던 `clickableYGNoRipple { focusManager.clearFocus() }`
  (2026-07-23 도입 → 2026-08-03 철회, **둘 다 브랜치 내부** — [YGScreen 스펙](2026-07-20-designsystem-ygscreen-scaffold.md))

## 목표

텍스트 입력이 있는 화면에서 **빈 영역을 탭하면 포커스·IME를 닫는다.** 단
① 접근성 트리를 오염시키지 않고, ② 필요한 화면만 opt-in으로 붙인다.

## 범위

- **포함**: `Modifier.clearFocusOnTap()` 확장 1개(`@Composable`), `focus/` 패키지 신설.
- **제외**:
  - `YGScreen`·`YGScaffold` 등 공용 컨테이너에 기본 탑재 — 명시적 opt-in만 지원(아래 [설계 결정](#설계-결정)).
  - 포커스 이동 방향 제어(`FocusDirection`)·특정 포커스 대상 지정 — `clearFocus()` 전체 해제만.
  - IME 직접 제어(`SoftwareKeyboardController`) — 포커스 해제에 따라오는 기본 동작에 위임.
  - 기존 `clickableYGNoRipple` 제거 — 사용처 0이 됐으나 존치 여부는 미결(아래 [주의](#주의--열린-질문)).

## API / 인터페이스

```kotlin
package com.teamyg.parfait.core.util.android.focus

@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current

    return pointerInput(focusManager) {
        detectTapGestures(onTap = { focusManager.clearFocus() })
    }
}
```

- 파라미터 없음. 동작이 하나뿐이라 `enabled` 플래그도 두지 않는다(필요 없으면 안 붙이면 됨).
- `pointerInput` 키는 `focusManager`. `LocalFocusManager`는 owner 수명 동안 동일 인스턴스라
  `Unit` 키와 실질 동일하지만, 캡처 값을 키로 두는 쪽이 정확하다.

## 동작 / 상태

- **탭 소비 순서**: Compose는 포인터 이벤트를 자식부터 dispatch한다. `YGTextFormField` 등 자식이
  탭을 소비하면 부모의 `detectTapGestures`는 발화하지 않는다 → 필드 탭으로 포커스가 풀리지 않는다.
  자식이 소비하지 않은 빈 영역 탭만 `clearFocus()`에 도달한다.
- **런타임 상태 없음.** `LocalFocusManager` 조회 외 보유 상태 없음.

## 설계 결정

### 왜 `Modifier.clickable`이 아닌가 (접근성)

`clickable`은 `role`을 `null`로 줘도 semantics에 **`onClick` action을 무조건 추가하고 focus target을 만든다.**
화면 최외곽에 붙이면 배경 전체가

- TalkBack에는 **단일 인터랙티브 요소**로 노출되고(`onClickLabel`도 없어 라벨 없이 "두 번 탭하여 활성화"만 읽힘),
- 키보드/D-pad 탐색에는 **포커스 스톱**으로 낀다.

`pointerInput`은 semantics를 전혀 남기지 않아 이 두 문제가 없다. 포커스 해제는 **보조 제스처**이지
사용자에게 알릴 액션이 아니므로 semantics를 남기지 않는 쪽이 의미상으로도 맞다.

### 왜 공용 컨테이너가 아니라 opt-in인가 (적용 범위)

`YGScreen`에 상시 탑재했을 때의 실제 분포:

| 화면 | 텍스트 입력 | `YGScreen` 사용 | 결과 |
|---|---|---|---|
| `AccountInfoScreen` | ✅ | ✅ | 정상 |
| `AppSettingScreen` | ❌ | ✅ | 접근성 비용만 지불 |
| `GroupNickNameScreen` | ✅ | ❌ | 필요한데 미적용 |
| `GroupCreateScreen` | ✅ | ❌ | 필요한데 미적용 |
| `InviteCodeInputFieldElement` | ✅ | ❌ | 필요한데 미적용 |

공용 컨테이너 소속이면 **필요 없는 화면이 비용을 지고 필요한 화면은 못 받는** 배치가 된다.
컨테이너 선택(`YGScreen` vs `YGScaffold` vs `Box`)과 입력 존재 여부는 직교하는 축이라,
컨테이너에 묶으면 항상 어긋난다. → 화면이 자기 필요를 선언하는 opt-in Modifier로 분리.

### 왜 `core:util:android`인가

`core:designsystem`이 아닌 이유: 시각 표현·토큰이 전혀 없는 순수 동작 유틸이다.
기존 `clickable/`(`clickableYG*`·리플)과 같은 계층이므로 형제 패키지 `focus/`로 둔다.
`ModuleFeatureImplConventionPlugin`이 모든 feature `:impl`에 `core:util:android`를 제공하므로
호출부 build.gradle 변경은 불필요하다.

## 파일 구성

- `core/util/android/.../focus/ClearFocusOnTap.kt` — 확장 1개(신규 패키지).
- ~~`core/designsystem/.../screen/YGScreen.kt` — 결선 제거~~ — **브랜치 내부 되돌리기라 develop 델타 없음**(위 2026-08-04 주석).
- `core/util/android/.../clickable/YGClickable.kt` — `clickableYGNoRipple` + `clickableYGThrottle(indications: List<Indication>?)`.
  결선 철회 후에도 남아 **develop에 사용처 0으로 신설**됐다(의도한 산출물이 아니라 잔여물).
- `feature/app/setting/impl/.../screen/AccountInfoScreen.kt` — `YGScreen(modifier = modifier.clearFocusOnTap())`.

## 검증

프로젝트에 테스트 인프라 없음 → 유닛/UI 테스트 미작성. 다음으로 검증:

- **컴파일**: `:core:util:android`·`:core:designsystem`·`:feature:app:setting:impl` `compileDebugKotlin` 통과.
- **ktlint**: `ktlintCheck` 통과.
- **수동**: S-002에서 ① 필드 탭 → 포커스 유지·IME 유지, ② 빈 영역 탭 → 포커스 해제·IME 닫힘,
  ③ TalkBack 켜고 화면 배경이 인터랙티브 요소로 읽히지 않는지.
- **미검증**: TalkBack 실기기 확인은 아직 안 함(접근성 회귀 제거가 이 변경의 주 목적이므로 확인 필요).

## 주의 / 열린 질문

- **`clickableYGNoRipple` 사용처 0** — `YGScreen` 결선을 위해 신설된 API인데 이 스펙으로 유일 사용처가 사라졌다.
  `clickableYG`/`DimRipple`/`ScaleRipple`/`MergeRipple` 4종과 세트를 이루는 공용 API라 존치했으나,
  YAGNI 관점에서 제거 대상일 수 있다(제거 시 `clickableYGThrottle`의 `indications: List<Indication>?`
  nullable 일반화도 함께 되돌려야 함) → [open-questions](../../../synthesis/open-questions.md) [2026-08-03].
- **미적용 입력 화면 3종** — `GroupNickNameScreen`·`GroupCreateScreen`·`InviteCodeInputFieldElement`는
  여전히 배경 탭 포커스 해제가 없다. `YGScreen`을 쓰지 않아 이전에도 없었으므로 회귀는 아니지만,
  UX 일관성을 맞추려면 각 화면에 이 Modifier를 붙이는 후속이 필요하다(이번 범위 밖).
- **`YGScaffold` 화면** — 같은 방식으로 붙일 수 있으나 선례 없음. `YGScreen`↔`YGScaffold` 통합 논의와
  함께 정리 → [open-questions](../../../synthesis/open-questions.md) [2026-07-20].
