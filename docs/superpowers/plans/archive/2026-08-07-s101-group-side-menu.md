---
id: s101-group-side-menu
title: S-101 그룹 사이드 메뉴 화면 구현 계획
status: done
spec: ../../specs/archive/2026-08-07-s101-group-side-menu.md
branch: feature/#211-S-101-group-side-menu
issue: mash-up-kr/TEAMYG-Android#211
created: 2026-08-07
tags: [plan, parfait, feature, groups, setting]
---

# S-101 그룹 사이드 메뉴 화면 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ✅ **develop 머지 완료(2026-08-13, PR #223).** 아래 실행 기록은 그대로 유효하고, 머지 시점
> 재대조에서 확정한 as-built는 [스펙](../../specs/archive/2026-08-07-s101-group-side-menu.md) 상단
> 머지 블록에 있다. 계획서 Task 1의 grep 전제 오류(`app-preview` 사용처 누락)는 실제 코드에서
> 구현자 정정본 그대로 머지됐다.

> ✅ **실행 완료(2026-08-09, 미머지).** Task 1~7 전량 + 추가 라운드. 체크박스는 실행 기록을 이 블록에
> 모으는 관례대로 미체크로 둔다. **산출물 계약의 정본은 [스펙](../../specs/archive/2026-08-07-s101-group-side-menu.md)이다**
> — 아래 본문은 실행 당시 지시서이고 이후 뒤집힌 결정이 있다(특히 Task 6의 하단 여백 처리).
>
> **실행 방식**: SDD(Task별 구현자 + 리뷰) 7 Task, 이후 사용자 요청으로 추가 라운드 5회.
> `develop` 위 rebase 후 커밋 20개. 유닛 테스트 16개·프리뷰 13종, `:app:assembleDebug` 통과.
>
> **계획이 틀렸던 곳 3건** — 계획서를 그대로 따랐다면 결함이 남았을 자리다.
> 1. **Task 1 Step 1의 grep 전제가 틀렸다.** "`ygcolorchip` 밖 사용처 없음"으로 적었으나 `app-preview`가
>    `NametagChip12`를 쓰고 있었다(컨트롤러의 사전 조사가 `head -30`에 잘려 놓쳤다). 구현자가 멈추고
>    보고해 잡혔고, 재번호 시 **같은 색을 계속 가리키도록** 사용처를 함께 옮겼다.
> 2. **Task 2 브리프의 호출부 목록이 부정확했다.** `InviteCodeInputFieldElement`는 `YGTextFormField`가
>    아니라 raw `BasicTextField`를 쓴다. 구현자가 정정했고 리뷰가 확인했다.
> 3. **Task 6의 "편집 중 하단 여백" 처리는 두 번 뒤집혔다.** 계획엔 없던 것을 리뷰 지적으로 넣었다가
>    (버튼 높이 실측 + `isEditing` 분기) Figma 근거로 걷어냈고, 실기기에서 마지막 블록이 버튼 뒤에
>    갇히는 것이 확인돼 **버튼을 오버레이에서 빼내 스크롤 영역의 형제로** 두는 구조로 정리했다.
>    교훈: 레이아웃 겹침은 여백으로 보정하지 말고 겹침 자체를 없애라.
>
> **유닛 테스트가 못 잡은 것** — 실기기에서만 드러난 결함 2건: 인셋 이중 계산으로 확인 버튼이
> 내비게이션 바 높이만큼 떠오름, 편집 중 마지막 블록이 버튼 뒤에 갇힘. 둘 다 최종 리뷰가 M1/Important로
> 예측했으나 기기 없이는 확정할 수 없어 이월했던 항목이다.
>
> **계획 범위 밖으로 추가된 것**(사용자 요청): 초대 문구 클립보드 복사 + `복사됨` 2초 복귀 ·
> `YGTextFieldImpl` 최소 높이 48 · `GroupMemberUiModel` `model` 패키지 분리 · State 3필드 도메인 VO 치환 ·
> **ADR-0016 원안 수렴**(유효성 표시 매핑을 `core:ui`로, 4개 화면 동시) · 컴포넌트 프리뷰 7종.

**Goal:** 그룹 사이드 메뉴 화면 S-101(닉네임 조회·인라인 편집 / 그룹원 목록 / 초대 코드 / Danger Zone)을 Mock 데이터로 구현한다.

**Architecture:** 단일 라우트 `GroupSettingRoute` + stateless `GroupSettingScreen` + MVI `GroupSettingViewModel`(`BaseViewModel` 상속). Figma `S-102`는 별도 화면이 아니라 이 화면의 `isEditing` 상태이므로 라우트를 분리하지 않고, 편집 중에만 하단에 `확인` 버튼 영역을 `imePadding()`으로 띄운다. 닉네임 확정 경로는 **키보드 엔터와 `확인` 버튼 둘**이고, 두 경로 모두 같은 Intent로 모인다.

**Tech Stack:** Kotlin / Jetpack Compose (BOM 2026.06.00) / Navigation3 + 자체 `Navigator` / Hilt / 자체 MVI(`BaseViewModel`) / 자체 디자인시스템(`core:designsystem`)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 위키 repo가 아니다. 로컬 절대경로는 `wiki/personal-private/project-paths.md` 참고.
- **브랜치 `feature/#211-S-101-group-side-menu`에서 작업한다.** 이 브랜치는 `develop`이 아니라 **`feature/#215-test-environment`(PR #219, `develop` 대상 OPEN) 위에 올라가 있다** — 유닛 테스트 기반(`parfait.test.unit` 컨벤션 플러그인 · `:core:testing` · coroutines-test/Turbine/MockK)을 쓰기 위해서다. 따라서 이 작업의 PR은 #219가 머지된 뒤에 내거나 base를 `feature/#215-test-environment`로 잡아야 한다.
- **커밋하지 않는다.** TJYG-Android는 기본이 미커밋이며, 사용자가 따로 지시할 때만 커밋한다. 각 Task의 마지막 단계는 커밋이 아니라 **검증 명령 통과**다.
- **ViewModel은 TDD로 만든다**(Task 3). 테스트를 먼저 쓰고 실패를 확인한 뒤 구현한다. 그 외(디자인시스템 상수·Compose UI)는 테스트하지 않는다 — 계측 테스트가 feature 모듈에 배선돼 있지 않고, 값 대비 비용이 크다. **화면 콜백 순서(확정 → 포커스 해제)는 유닛 테스트로 안 잡히므로 Task 7의 실기기 확인이 유일한 그물이다.**
- **테스트 컨벤션**(기존 테스트와 맞춘다): `kotlin.test`의 `Test`·`assertEquals`·`assertNull`·`assertTrue`를 쓰고, 메서드명은 `subject_condition_expectation` 영문 스네이크(백틱 금지 — minSdk 26이라 백틱 메서드명은 기기 API 30+ 전용), 본문은 `// Given` / `// When` / `// Then` 주석으로 나눈다. `runTest`는 **반드시 `runTest(mainDispatcherRule.dispatcher)`** 로 호출한다(인자 없이 부르면 스케줄러가 둘로 갈려 Main 큐가 안 비워진다).
- **신규 디자인시스템 컴포넌트·에셋·색 토큰을 만들지 않는다.** 기존 컴포넌트 수정은 두 건뿐이다 — `YGColorChipType` 드리프트 정정(Task 1)과 `YGTextFormField` IME 파라미터 확장(Task 2). 둘 다 기존 호출부를 깨지 않는다.
- **하드코딩 금지 항목**: 색은 `YGAtomicColors.*`, 간격·패딩은 `YGTheme.layout.*`, 타이포는 `YGTheme.typography.*`를 쓴다. `dp` 리터럴은 프리뷰 코드에서만 허용한다.
- **정적 UI 라벨은 전부 `strings.xml`**. 닉네임·그룹명·초대 코드처럼 데이터인 값은 State가 소유한다.
- 닉네임 길이 상한은 `GroupCreateConfig.NICKNAME_MAX_LENGTH`(= 15) 상수를 쓴다. 숫자 리터럴 금지.
- 유효성 에러 문구는 `core:ui`의 기존 리소스를 쓴다: `error_duplicated_space` · `error_invalid_character` · `error_space_at_edge_nickname` · `error_empty_space_nickname`. 새로 만들지 않는다.
- **긴 문자열 프리뷰를 반드시 포함한다** — 그룹명 10자, 닉네임 15자. 짧은 샘플만 두다 폭 측정 결함을 놓친 전례가 있다(`bar-listdate` 라운드).

**패키지 루트:** `com.teamyg.parfait.feature.groups.setting.impl`
**모듈 경로:** `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/`
(이하 `IMPL/`로 줄여 쓴다.)

---

## File Structure

| 파일 | 책임 |
|---|---|
| `core/designsystem/.../ygcolorchip/YGColorChipType.kt` | (수정) 컬러칩 12종 + Plus로 Figma와 정렬 |
| `core/designsystem/.../ygcolorchip/YGNametagChipPreviewData.kt` | (수정) 위 변경에 맞춰 프리뷰 목록 조정 |
| `core/designsystem/.../textfield/YGTextFieldImpl.kt` | (수정) `keyboardOptions`·`keyboardActions`를 `BasicTextField`로 전달 |
| `core/designsystem/.../textfield/YGTextFormField.kt` | (수정) 위 두 파라미터를 공개 시그니처로 노출 |
| `feature/groups/setting/impl/build.gradle.kts` | (수정) `parfait.test.unit` 플러그인 적용 |
| `IMPL/viewmodel/GroupSettingViewModel.kt` | State·Intent·SideEffect·VM + Mock 데이터. 편집 상태 전이와 실시간 유효성 검사를 소유 |
| `feature/groups/setting/impl/src/test/kotlin/.../viewmodel/GroupSettingViewModelTest.kt` | 위 VM의 상태 전이·유효성 매핑·확정 가드·SideEffect 검증 |
| `IMPL/res/values/strings.xml` | 정적 라벨 |
| `IMPL/component/GroupNicknameField.kt` | 라벨 + 닉네임 입력 필드(포커스 변화·엔터 확정 콜백 포함) |
| `IMPL/component/GroupMemberList.kt` | 라벨(인원수 포함) + 그룹원 칩 목록 |
| `IMPL/screen/GroupSettingScreen.kt` | stateless 화면 조립 + 편집 모드 버튼 영역 + 프리뷰 |
| `IMPL/route/GroupSettingRoute.kt` | ViewModel·navigator·클립보드 배선 |

Task 1과 Task 2는 디자인시스템 단독이라 서로 독립이다. Task 3 → 4 → 5 → 6 → 7은 순차 의존이고, Task 4는 Task 2에 의존한다.

---

### Task 1: `YGColorChipType`을 Figma와 정렬

Figma 컴포넌트셋 `144:5415`는 타입 `1~12` + `+` = 13종인데 코드는 `NametagChip1~13` + `NametagChipPlus` = 14종이다. 원인은 (a) `NametagChip11`이 `NametagChip3`과 색 3개가 전부 같은 중복이라 이후 항목이 한 칸씩 밀린 것, (b) `NametagChip9`의 텍스트 색이 테두리 색과 같게 들어간 드리프트다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcolorchip/YGColorChipType.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcolorchip/YGNametagChipPreviewData.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `YGColorChipType.NametagChip1` ~ `NametagChip12`(12종) + `NametagChipPlus`. `NametagChip13`은 **사라진다**. Task 3이 이 12종을 목록으로 참조한다.

- [ ] **Step 1: 변경 전 사용처를 확인해 파급이 없음을 증명**

Run:
```bash
grep -rn "NametagChip1[123]" --include=*.kt . | grep -v "component/ygcolorchip/"
```
Expected: **출력 없음**. (`ygcolorchip` 패키지 밖에서 11·12·13번을 쓰는 코드가 없다는 뜻. 출력이 있으면 멈추고 보고할 것 — 이 계획의 전제가 깨진 것이다.)

- [ ] **Step 2: `NametagChip9`의 텍스트 색을 정정**

`YGColorChipType.kt`에서 아래 블록을 찾아

```kotlin
    data object NametagChip9 : YGColorChipType {
        override val fillColor = YGAtomicColors.Melon.Melon500
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Cherry.Cherry50
    }
```

아래로 바꾼다(Figma 9번: 채움 Melon500 / 테두리 Cherry50 / 글자 Pudding500).

```kotlin
    data object NametagChip9 : YGColorChipType {
        override val fillColor = YGAtomicColors.Melon.Melon500
        override val strokeColor = YGAtomicColors.Cherry.Cherry50
        override val textColor = YGAtomicColors.Pudding.Pudding500
    }
```

- [ ] **Step 3: 중복 11번을 삭제하고 12·13번을 11·12번으로 재번호**

`YGColorChipType.kt`에서 `NametagChip11`·`NametagChip12`·`NametagChip13` 세 블록을

```kotlin
    data object NametagChip11 : YGColorChipType {
        override val fillColor = YGAtomicColors.Cherry.Cherry400
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Melon.Melon500
    }

    data object NametagChip12 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Melon.Melon500
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }

    data object NametagChip13 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }
```

아래 두 블록으로 교체한다(중복이던 구 11번 삭제, 구 12→11, 구 13→12).

```kotlin
    data object NametagChip11 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Melon.Melon500
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }

    data object NametagChip12 : YGColorChipType {
        override val fillColor = YGAtomicColors.Pudding.Pudding500
        override val strokeColor = YGAtomicColors.Cherry.Cherry100
        override val textColor = YGAtomicColors.Cherry.Cherry300
    }
```

`NametagChipPlus`는 그대로 둔다(주석 `// 5명이상 시 + 컬러칩 타입` 포함).

- [ ] **Step 4: 프리뷰 목록에서 13번 항목 제거**

`YGNametagChipPreviewData.kt`의 `values` 시퀀스에서 아래 항목을 **삭제**한다.

```kotlin
        YGChipPreviewData(
            name = "NametagChip13",
            colorChipType = YGColorChipType.NametagChip13,
        ),
```

나머지 `NametagChip1` ~ `NametagChip12` 12개 항목과 `NametagChipPlus` 항목은 그대로 둔다. 결과적으로 시퀀스는 13개가 된다.

- [ ] **Step 5: 컴파일·lint 검증**

Run:
```bash
./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintCheck
```
Expected: BUILD SUCCESSFUL. (`NametagChip13` 참조가 어딘가 남아 있으면 여기서 `Unresolved reference`로 잡힌다.)

- [ ] **Step 6: 프리뷰 육안 확인**

Android Studio에서 `YGNametagChip.kt`의 `YGNametagChipPreview`를 연다.
확인할 것: 칩이 **13종**(1~12 + Plus)만 보이고, `NametagChip9`(민트 배경)의 글자가 **노랑(Pudding500)** 이며 배경에 묻히지 않는다. `NametagChip3`과 `NametagChip11`이 더 이상 같은 색이 아니다.

---

### Task 2: `YGTextFormField`에 IME 액션 전달 경로 추가

닉네임 확정 트리거가 `확인` 버튼과 **키보드 엔터** 둘인데, 현재 `YGTextFormField`·`YGTextFieldImpl`은 `BasicTextField`의 `keyboardOptions`·`keyboardActions`를 노출하지 않아 엔터를 잡을 방법이 없다. 두 파라미터를 기본값과 함께 뚫는다.

기본값이 `KeyboardOptions.Default`·`KeyboardActions.Default`라 **기존 호출부 4곳**(`AccountInfoScreen`·`GroupCreateScreen`·`GroupNickNameScreen`·`InviteCodeInputFieldElement` 계열)은 한 줄도 바뀌지 않는다.

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFieldImpl.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/textfield/YGTextFormField.kt`

**Interfaces:**
- Consumes: 없음
- Produces: `YGTextFormField(value, onValueChange, modifier, placeholder, enabled, isError, maxLength, errorDescription, colors, keyboardOptions, keyboardActions)` — 뒤 두 파라미터가 신규. Task 4의 `GroupNicknameField`가 쓴다.

- [ ] **Step 1: `YGTextFieldImpl`에 파라미터 추가**

`YGTextFieldImpl.kt`의 함수 시그니처에서 `colors: YGTextFieldColors,` 다음에 두 줄을 추가한다.

```kotlin
internal fun YGTextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    maxLength: Int?,
    colors: YGTextFieldColors,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
```

그리고 내부 `BasicTextField(...)` 호출에서 `singleLine = true,` 바로 앞에 두 줄을 전달한다.

```kotlin
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
```

import 2개를 추가한다.

```kotlin
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
```

- [ ] **Step 2: `YGTextFormField`에 파라미터 추가·전달**

`YGTextFormField.kt`의 시그니처에서 `colors: YGTextFormFieldColors = YGTextFormFieldDefaults.colors(),` 다음에 두 줄을 추가한다.

```kotlin
    colors: YGTextFormFieldColors = YGTextFormFieldDefaults.colors(),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
```

내부 `YGTextFieldImpl(...)` 호출에서 `colors = colors.textFieldColors,` 다음에 두 줄을 전달한다.

```kotlin
            colors = colors.textFieldColors,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
```

import 2개를 추가한다.

```kotlin
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
```

- [ ] **Step 3: 컴파일·lint 검증 — 기존 호출부가 안 깨졌는지 포함**

Run:
```bash
./gradlew :core:designsystem:compileDebugKotlin :core:designsystem:ktlintCheck :app:assembleDebug
```
Expected: BUILD SUCCESSFUL. `:app:assembleDebug`가 `YGTextFormField`를 쓰는 feature 모듈 전부를 컴파일하므로, 기본값 덕에 기존 호출부가 무영향이라는 주장이 여기서 검증된다.

- [ ] **Step 4: 기존 프리뷰 육안 확인**

`YGTextFormField.kt`의 `YGTextFormFieldPreview`를 연다.
확인할 것: 4종 프리뷰의 렌더가 **변경 전과 동일**하다(파라미터만 늘었고 기본값이 이전 동작이므로 시각적 변화가 있으면 안 된다).

---

### Task 3: `GroupSettingViewModel` — 테스트 먼저, 그다음 구현

**Files:**
- Create: `IMPL/viewmodel/GroupSettingViewModel.kt`

**Interfaces:**
- Consumes: Task 1의 `YGColorChipType.NametagChip1` ~ `NametagChip12`. 기존 `CheckNameValidUseCase`(`operator fun invoke(name: String): NameValidResult`), `BaseViewModel<S, I, E>`(`state: StateFlow<S>` · `effect: SharedFlow<E>` · `updateState(S.() -> S)` · `postSideEffect(E)`).
- Produces:
  - `GroupMemberUiModel(nickname: String, colorChipType: YGColorChipType, isMe: Boolean)`
  - `GroupSettingUiState` — 프로퍼티 `groupName`·`myNickname`·`nicknameInput`·`isEditing`·`errorMessageResId`·`members`·`inviteCode`·`remainingCount`·`isCodeCopied`, 파생값 `isConfirmEnabled: Boolean`
  - `GroupSettingIntent` — `ClickBack`·`InputNickname(nickname)`·`ChangeNicknameFocus(isFocused)`·`ConfirmNickname`·`ClickCopyInviteCode`·`ClickLeaveGroup`·`ClickReportGroup`
  - `GroupSettingSideEffect` — `NavigateBack`·`CopyInviteCode(inviteCode)`
  - `GroupSettingViewModel`

- [ ] **Step 1: 모듈에 유닛 테스트 배선**

`feature/groups/setting/impl/build.gradle.kts`의 `plugins` 블록에 한 줄을 추가한다.

```kotlin
plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}
```

`parfait.test.unit`이 `libs.bundles.test.unit`(junit4·kotlin-test·coroutines-test·Turbine·MockK·MockWebServer)과 `:core:testing`을 `testImplementation`으로 붙이고, release 변형 유닛 테스트를 꺼 중복 실행을 막는다. **`dependencies` 블록은 손대지 않는다.**

- [ ] **Step 2: State·Intent·SideEffect 선언**

Create `IMPL/viewmodel/GroupSettingViewModel.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.teamyg.parfait.core.ui.R as CoreR

data class GroupMemberUiModel(
    val nickname: String,
    val colorChipType: YGColorChipType,
    val isMe: Boolean = false,
)

data class GroupSettingUiState(
    val groupName: String = MOCK_GROUP_NAME,
    val myNickname: String = MOCK_MY_NICKNAME,
    val nicknameInput: String = MOCK_MY_NICKNAME,
    val isEditing: Boolean = false,
    val errorMessageResId: Int? = null,
    val members: List<GroupMemberUiModel> = MOCK_MEMBERS,
    val inviteCode: String = MOCK_INVITE_CODE,
    val remainingCount: Int = MOCK_REMAINING_COUNT,
    val isCodeCopied: Boolean = false,
) : UiState {
    val isConfirmEnabled: Boolean
        get() = errorMessageResId == null && nicknameInput != myNickname
}

sealed interface GroupSettingIntent : UiIntent {
    data object ClickBack : GroupSettingIntent

    data class InputNickname(val nickname: String) : GroupSettingIntent

    data class ChangeNicknameFocus(val isFocused: Boolean) : GroupSettingIntent

    data object ConfirmNickname : GroupSettingIntent

    data object ClickCopyInviteCode : GroupSettingIntent

    data object ClickLeaveGroup : GroupSettingIntent

    data object ClickReportGroup : GroupSettingIntent
}

sealed interface GroupSettingSideEffect : UiSideEffect {
    data object NavigateBack : GroupSettingSideEffect

    data class CopyInviteCode(val inviteCode: String) : GroupSettingSideEffect
}
```

`ConfirmNickname`이 `Click*` 접두를 안 쓰는 이유: 확정 경로가 버튼 클릭과 키보드 엔터 **둘**이라 클릭 전용 이름이면 거짓말이 된다.

- [ ] **Step 3: 같은 파일 하단에 Mock 데이터 추가**

```kotlin
// TODO: 그룹 상세 조회 API 연동 시 아래 Mock 전량 교체
//  (GET /api/parfait-groups/{groupId} — groupName·memberLimit는 계약에 없어 별도 확보 필요)
private const val MOCK_GROUP_NAME = "그룹이름"
private const val MOCK_MY_NICKNAME = "잠탈전용닉네임2"
private const val MOCK_INVITE_CODE = "WDIDCJ"
private const val MOCK_REMAINING_COUNT = 1

// TODO: 컬러칩 타입 부여 주체가 미정이라 목록 인덱스로 순환 배정한다. 서버가 타입을 주면 교체.
private val NAMETAG_CHIP_TYPES: List<YGColorChipType> = listOf(
    YGColorChipType.NametagChip1,
    YGColorChipType.NametagChip2,
    YGColorChipType.NametagChip3,
    YGColorChipType.NametagChip4,
    YGColorChipType.NametagChip5,
    YGColorChipType.NametagChip6,
    YGColorChipType.NametagChip7,
    YGColorChipType.NametagChip8,
    YGColorChipType.NametagChip9,
    YGColorChipType.NametagChip10,
    YGColorChipType.NametagChip11,
    YGColorChipType.NametagChip12,
)

private val MOCK_MEMBER_NICKNAMES = listOf(
    MOCK_MY_NICKNAME,
    "아니야나그런데기니야기니라니까",
    "체리마루",
    "멜론소다먹고싶다",
    "푸딩왕자",
    "딸기시럽듬뿍",
    "오레오조각",
    "노랑젤리",
    "파랑젤리",
    "키위한조각",
    "생크림가득",
)

private val MOCK_MEMBERS: List<GroupMemberUiModel> = MOCK_MEMBER_NICKNAMES.mapIndexed { index, nickname ->
    GroupMemberUiModel(
        nickname = nickname,
        colorChipType = NAMETAG_CHIP_TYPES[index % NAMETAG_CHIP_TYPES.size],
        isMe = index == 0,
    )
}
```

Mock이 파일 하단에 있어도 `data class` 기본값은 인스턴스 생성 시점에 평가되므로 초기화 순서 문제가 없다.

- [ ] **Step 4: 실패하는 테스트 작성**

Create `feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModelTest.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.teamyg.parfait.core.ui.R as CoreR

class GroupSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = GroupSettingViewModel(CheckNameValidUseCase())

    @Test
    fun inputNickname_validName_updatesInputAndClearsError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 유효한 닉네임을 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname("새닉네임"))

        // Then 입력값이 반영되고 에러가 없다
        assertEquals("새닉네임", viewModel.state.value.nicknameInput)
        assertNull(viewModel.state.value.errorMessageResId)
    }

    @Test
    fun inputNickname_invalidCharacter_setsInvalidCharacterError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 허용되지 않는 문자를 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname("닉네임!"))

        // Then 문자 규칙 위반 에러가 붙는다
        assertEquals(
            CoreR.string.error_invalid_character,
            viewModel.state.value.errorMessageResId,
        )
    }

    @Test
    fun inputNickname_leadingSpace_setsSpaceAtEdgeError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 앞에 공백이 있는 닉네임을 입력
        viewModel.processIntent(GroupSettingIntent.InputNickname(" 닉네임"))

        // Then 가장자리 공백 에러가 붙는다
        assertEquals(
            CoreR.string.error_space_at_edge_nickname,
            viewModel.state.value.errorMessageResId,
        )
    }

    @Test
    fun inputNickname_emptyName_setsEmptyStringError() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 전부 지움
        viewModel.processIntent(GroupSettingIntent.InputNickname(""))

        // Then 빈 값 에러가 붙고 확인이 비활성이다
        assertEquals(
            CoreR.string.error_empty_space_nickname,
            viewModel.state.value.errorMessageResId,
        )
        assertFalse(viewModel.state.value.isConfirmEnabled)
    }

    @Test
    fun isConfirmEnabled_nicknameUnchanged_isFalse() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname

        // When 고쳤다가 원래 값으로 되돌림
        viewModel.processIntent(GroupSettingIntent.InputNickname("잠깐바꿈"))
        viewModel.processIntent(GroupSettingIntent.InputNickname(original))

        // Then 바뀐 것이 없으므로 확인은 비활성
        assertFalse(viewModel.state.value.isConfirmEnabled)
    }

    @Test
    fun changeNicknameFocus_focused_entersEditing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()

        // When 입력 필드가 포커스를 얻음
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))

        // Then 편집 모드로 들어간다
        assertTrue(viewModel.state.value.isEditing)
    }

    @Test
    fun changeNicknameFocus_unfocused_cancelsEditingAndRestoresInput() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중 입력을 고친 상태
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("고치던값"))

        // When 포커스를 잃음
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = false))

        // Then 편집이 취소되고 입력값이 원래대로 돌아간다
        assertFalse(viewModel.state.value.isEditing)
        assertEquals(original, viewModel.state.value.nicknameInput)
        assertNull(viewModel.state.value.errorMessageResId)
    }

    @Test
    fun confirmNickname_validChange_commitsAndSyncsMemberList() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중 유효한 새 닉네임을 입력한 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("확정될닉네임"))

        // When 확정
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)

        // Then 내 닉네임이 바뀌고 편집이 끝나며 그룹원 목록의 내 항목도 따라 바뀐다
        val state = viewModel.state.value
        assertEquals("확정될닉네임", state.myNickname)
        assertFalse(state.isEditing)
        assertEquals("확정될닉네임", state.members.first { it.isMe }.nickname)
    }

    @Test
    fun confirmNickname_invalidNickname_keepsPreviousNickname() = runTest(mainDispatcherRule.dispatcher) {
        // Given 유효성을 통과하지 못한 입력
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("잘못된 닉네임!"))

        // When 확정을 시도(키보드 엔터 포함 같은 경로)
        viewModel.processIntent(GroupSettingIntent.ConfirmNickname)

        // Then 아무 것도 확정되지 않고 편집 상태가 유지된다
        assertEquals(original, viewModel.state.value.myNickname)
        assertTrue(viewModel.state.value.isEditing)
    }

    @Test
    fun clickBack_whileEditing_cancelsEditingWithoutNavigating() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중인 상태
        val viewModel = viewModel()
        val original = viewModel.state.value.myNickname
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("고치던값"))

        // When 뒤로가기
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ClickBack)

            // Then 화면을 닫지 않고 편집만 취소한다
            expectNoEvents()
        }
        assertFalse(viewModel.state.value.isEditing)
        assertEquals(original, viewModel.state.value.nicknameInput)
    }

    @Test
    fun clickBack_notEditing_emitsNavigateBack() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집 중이 아닌 상태
        val viewModel = viewModel()

        // When 뒤로가기
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ClickBack)

            // Then 화면을 닫는 SideEffect가 나간다
            assertEquals(GroupSettingSideEffect.NavigateBack, awaitItem())
        }
    }

    @Test
    fun clickCopyInviteCode_marksCopiedAndEmitsCopyEffect() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 상태의 화면
        val viewModel = viewModel()
        val inviteCode = viewModel.state.value.inviteCode

        // When 복사 버튼
        viewModel.effect.test {
            viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)

            // Then 클립보드 SideEffect가 코드와 함께 나가고 카드 문구가 복사됨으로 바뀐다
            assertEquals(GroupSettingSideEffect.CopyInviteCode(inviteCode), awaitItem())
        }
        assertTrue(viewModel.state.value.isCodeCopied)
    }
}
```

- [ ] **Step 5: 테스트가 실패하는지 확인**

Run:
```bash
./gradlew :feature:groups:setting:impl:testDebugUnitTest
```
Expected: **FAIL** — `Unresolved reference: GroupSettingViewModel`(아직 클래스가 없다). 컴파일이 통과해 버리면 이미 구현이 있다는 뜻이므로 멈추고 확인한다.

- [ ] **Step 6: ViewModel 구현**

`GroupSettingSideEffect` 선언 다음(= Mock 선언 앞)에 붙인다.

```kotlin
@HiltViewModel
class GroupSettingViewModel
@Inject
constructor(
    private val checkNameValid: CheckNameValidUseCase,
) : BaseViewModel<GroupSettingUiState, GroupSettingIntent, GroupSettingSideEffect>(
    initialState = GroupSettingUiState(),
) {
    init {
        viewModelLogger.i { "GroupSettingViewModel::init" }
    }

    override fun processIntent(intent: GroupSettingIntent) {
        when (intent) {
            GroupSettingIntent.ClickBack -> handleClickBack()
            is GroupSettingIntent.InputNickname -> handleInputNickname(intent.nickname)
            is GroupSettingIntent.ChangeNicknameFocus -> handleChangeNicknameFocus(intent.isFocused)
            GroupSettingIntent.ConfirmNickname -> handleConfirmNickname()
            GroupSettingIntent.ClickCopyInviteCode -> handleClickCopyInviteCode()
            GroupSettingIntent.ClickLeaveGroup -> handleClickLeaveGroup()
            GroupSettingIntent.ClickReportGroup -> handleClickReportGroup()
        }
    }

    private fun handleClickBack() {
        if (state.value.isEditing) {
            cancelEditing()
        } else {
            postSideEffect(GroupSettingSideEffect.NavigateBack)
        }
    }

    private fun handleInputNickname(nickname: String) {
        val errorMessageResId = when (checkNameValid(nickname)) {
            NameValidResult.Success -> null
            NameValidResult.Error.DuplicatedSpace -> CoreR.string.error_duplicated_space
            NameValidResult.Error.InvalidCharacter -> CoreR.string.error_invalid_character
            NameValidResult.Error.SpaceAtEdge -> CoreR.string.error_space_at_edge_nickname
            NameValidResult.Error.EmptyString -> CoreR.string.error_empty_space_nickname
        }

        updateState {
            copy(
                nicknameInput = nickname,
                errorMessageResId = errorMessageResId,
            )
        }
    }

    private fun handleChangeNicknameFocus(isFocused: Boolean) {
        if (isFocused) {
            updateState { copy(isEditing = true) }
        } else {
            cancelEditing()
        }
    }

    private fun handleConfirmNickname() {
        if (!state.value.isConfirmEnabled) return

        // TODO: 닉네임 변경 API 연동 (PATCH /api/parfait-groups/{groupId}/nickname)
        updateState {
            copy(
                myNickname = nicknameInput,
                members = members.map { member ->
                    if (member.isMe) member.copy(nickname = nicknameInput) else member
                },
                isEditing = false,
                errorMessageResId = null,
            )
        }
    }

    private fun handleClickCopyInviteCode() {
        updateState { copy(isCodeCopied = true) }
        postSideEffect(GroupSettingSideEffect.CopyInviteCode(state.value.inviteCode))
    }

    private fun handleClickLeaveGroup() {
        // TODO: 그룹 나가기 확인 모달 + DELETE /api/parfait-groups/{groupId}/members/me
        viewModelLogger.i { "GroupSettingViewModel::handleClickLeaveGroup" }
    }

    private fun handleClickReportGroup() {
        // TODO: 그룹 신고 확인 모달 + POST /api/parfait-groups/{groupId}/reports
        viewModelLogger.i { "GroupSettingViewModel::handleClickReportGroup" }
    }

    private fun cancelEditing() {
        updateState {
            copy(
                nicknameInput = myNickname,
                errorMessageResId = null,
                isEditing = false,
            )
        }
    }
}
```

동작 근거 3가지를 코드 읽는 사람이 헷갈리지 않게 여기 적어둔다:
- **확정 경로는 `ConfirmNickname` 하나**이고 버튼·엔터가 거기로 모인다. 그 외(배경 탭·뒤로가기·포커스 상실)는 전부 `cancelEditing()`이라 입력값이 `myNickname`으로 되돌아간다.
- `handleConfirmNickname`은 `isConfirmEnabled`가 아니면 **아무 것도 하지 않는다.** 유효성 미통과 상태에서 엔터를 눌러도 확정되지 않는다("활성화된 확인 버튼을 눌렀을 때"와 같은 조건).
- `isConfirmEnabled`가 `nicknameInput != myNickname`을 포함하는 이유: 값이 그대로인데 확인이 활성인 것은 무의미하다. 빈 문자열은 `CheckNameValidUseCase`가 `EmptyString`으로 잡아 별도 조건이 필요 없다.

- [ ] **Step 7: 테스트가 통과하는지 확인**

Run:
```bash
./gradlew :feature:groups:setting:impl:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, 12개 테스트 전부 통과.

`Unresolved reference: CheckNameValidUseCase` 또는 `viewModelLogger`가 나면 모듈 의존성 문제다 — `feature/groups/setting/impl/build.gradle.kts`를 열어 `feature/app/setting/impl/build.gradle.kts`와 비교한다(S-001 모듈은 별도 선언 없이 `domain`·`core:ui`를 쓴다. 즉 컨벤션 플러그인 `parfait.module.feature.impl`이 준다. 그래도 안 되면 보고할 것).

`Module with the Main dispatcher had failed to initialize` 계열이 나면 `MainDispatcherRule`이 안 붙은 것이다 — `@get:Rule` 어노테이션과 `runTest(mainDispatcherRule.dispatcher)` 인자를 확인한다.

- [ ] **Step 8: 테스트가 구현을 실제로 호출하는지 증명**

이 저장소의 테스트 대부분이 기존 코드를 뒤따라 쓰는 특성화 테스트라, 통과만으로는 "테스트가 진짜 구현을 보고 있다"는 것이 증명되지 않는다. **기대값을 일부러 뒤집어 FAIL을 확인한 뒤 되돌린다.**

`confirmNickname_validChange_commitsAndSyncsMemberList`의 단언 한 줄을 임시로 바꾼다.

```kotlin
        assertEquals("확정되면안되는값", state.myNickname)
```

Run:
```bash
./gradlew :feature:groups:setting:impl:testDebugUnitTest --tests "*GroupSettingViewModelTest.confirmNickname_validChange_commitsAndSyncsMemberList"
```
Expected: **FAIL**(expected `확정되면안되는값`, actual `확정될닉네임`). 확인했으면 원래 단언으로 되돌리고 다시 돌려 통과를 확인한다.

- [ ] **Step 9: lint 검증**

Run:
```bash
./gradlew :feature:groups:setting:impl:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

---

### Task 4: 문자열 리소스 + 화면 로컬 컴포넌트 2종

**Files:**
- Create: `IMPL/res/values/strings.xml`
- Create: `IMPL/component/GroupNicknameField.kt`
- Create: `IMPL/component/GroupMemberList.kt`

**Interfaces:**
- Consumes: Task 3의 `GroupMemberUiModel`, Task 2가 확장한 `YGTextFormField`. 기존 DS `YGLabel(text, modifier)` · `YGUserChip(colorChipType, userFirstName, chip, userName, userStyle, modifier)` · `YGNametagChipStyle.Style40` · `YGUserNameStyle.StyleBold`/`StyleMedium`.
- Produces:
  - `GroupNicknameField(nickname: String, errorMessageResId: Int?, onNicknameChange: (String) -> Unit, onFocusChange: (Boolean) -> Unit, onConfirmNickname: () -> Unit, modifier: Modifier)`
  - `GroupMemberList(members: List<GroupMemberUiModel>, modifier: Modifier)`
  - `R.string.group_setting_*` 문자열 11종

- [ ] **Step 1: 문자열 리소스 생성**

Create `IMPL/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="group_setting_nickname_label">그룹 속 내 닉네임</string>
    <string name="group_setting_member_label">그룹원 (%1$d)</string>
    <string name="group_setting_member_name_me">%1$s (나)</string>
    <string name="group_setting_invite_label">그룹 초대 코드</string>
    <string name="group_setting_invite_remaining">%1$d명 남음</string>
    <string name="group_setting_invite_copied">복사됨</string>
    <string name="group_setting_invite_full">최대 인원 도달</string>
    <string name="group_setting_copy">복사</string>
    <string name="group_setting_leave">그룹 나가기</string>
    <string name="group_setting_report">그룹 신고하기</string>
    <string name="group_setting_confirm">확인</string>
</resources>
```

- [ ] **Step 2: `GroupNicknameField` 생성**

Create `IMPL/component/GroupNicknameField.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.setting.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.domain.model.GroupCreateConfig
import com.teamyg.parfait.feature.groups.setting.impl.R

@Composable
internal fun GroupNicknameField(
    nickname: String,
    errorMessageResId: Int?,
    onNicknameChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onConfirmNickname: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
    ) {
        YGLabel(text = stringResource(R.string.group_setting_nickname_label))

        YGTextFormField(
            value = nickname,
            onValueChange = onNicknameChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> onFocusChange(focusState.hasFocus) },
            isError = errorMessageResId != null,
            maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH,
            errorDescription = errorMessageResId?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirmNickname() }),
        )
    }
}
```

두 가지 근거:
- `hasFocus`를 쓰는 이유: `onFocusChanged`를 `YGTextFormField`(내부가 `Column`)에 걸면 그 노드 자신은 포커스를 받지 않는다. `isFocused`는 자기 자신만, `hasFocus`는 자손 포함이라 실제 텍스트 필드의 포커스를 잡으려면 `hasFocus`여야 한다.
- `ImeAction.Done`을 쓰는 이유: 입력이 한 필드로 끝나 다음으로 이동할 곳이 없다. 키보드 우측 하단이 완료(체크) 모양이 되고 그것을 누르면 `onDone`이 온다.

- [ ] **Step 3: `GroupMemberList` 생성**

Create `IMPL/component/GroupMemberList.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.setting.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGNametagChipStyle
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGUserChip
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGUserNameStyle
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.groups.setting.impl.R
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupMemberUiModel

@Composable
internal fun GroupMemberList(
    members: List<GroupMemberUiModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
    ) {
        YGLabel(text = stringResource(R.string.group_setting_member_label, members.size))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = YGTheme.layout.gap.gap3),
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        ) {
            members.forEach { member ->
                YGUserChip(
                    colorChipType = member.colorChipType,
                    userFirstName = member.nickname.take(1),
                    chip = YGNametagChipStyle.Style40,
                    userName = if (member.isMe) {
                        stringResource(R.string.group_setting_member_name_me, member.nickname)
                    } else {
                        member.nickname
                    },
                    userStyle = if (member.isMe) {
                        YGUserNameStyle.StyleBold
                    } else {
                        YGUserNameStyle.StyleMedium
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
```

`take(1)`을 쓰는 이유: `first()`는 빈 문자열에서 예외를 던진다. 닉네임이 비는 경우는 없어야 하지만 Mock/서버 어느 쪽이든 방어값이 공짜다.

- [ ] **Step 4: 컴파일·lint 검증**

Run:
```bash
./gradlew :feature:groups:setting:impl:compileDebugKotlin :feature:groups:setting:impl:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

---

### Task 5: `GroupSettingScreen` — 조회 상태 조립

**Files:**
- Create: `IMPL/screen/GroupSettingScreen.kt`

**Interfaces:**
- Consumes: Task 3의 `GroupSettingUiState`, Task 4의 `GroupNicknameField`·`GroupMemberList`·문자열. 기존 DS `YGScreen { }`(리시버 `YGScreenScope`, `OnBack(enabled, handler)` 제공) · `YGTopBarDetail(title, onIconClick, modifier)` · `YGInviteCard(label, inviteCode, subText, status, copyButtonText, onCopyClick, modifier, endIconResource)` · `YGInviteCardStatus.Active`/`Invalid` · `YGDangerZone(topZone, bottomZone, modifier)` · `YGActionItem(text, onClick, modifier, iconResource)` · `clearFocusOnTap()`.
- Produces: `GroupSettingScreen(state, onClickBack, onNicknameChange, onNicknameFocusChange, onConfirmNickname, onClickCopyInviteCode, onClickLeaveGroup, onClickReportGroup, modifier)` — Task 6이 편집 버튼 영역을 이 파일에 덧붙이고, Task 7이 호출한다.

- [ ] **Step 1: 화면 파일 생성**

Create `IMPL/screen/GroupSettingScreen.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.setting.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCard
import com.teamyg.parfait.core.designsystem.component.card.YGInviteCardStatus
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.util.android.focus.clearFocusOnTap
import com.teamyg.parfait.feature.groups.setting.impl.R
import com.teamyg.parfait.feature.groups.setting.impl.component.GroupMemberList
import com.teamyg.parfait.feature.groups.setting.impl.component.GroupNicknameField
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingUiState

@Composable
internal fun GroupSettingScreen(
    state: GroupSettingUiState,
    onClickBack: () -> Unit,
    onNicknameChange: (String) -> Unit,
    onNicknameFocusChange: (Boolean) -> Unit,
    onConfirmNickname: () -> Unit,
    onClickCopyInviteCode: () -> Unit,
    onClickLeaveGroup: () -> Unit,
    onClickReportGroup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val confirmAndDismissKeyboard = {
        onConfirmNickname()
        focusManager.clearFocus()
    }

    YGScreen(modifier = modifier.clearFocusOnTap()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                YGTopBarDetail(
                    title = state.groupName,
                    onIconClick = onClickBack,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap8),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = YGTheme.layout.padding.padding7,
                            end = YGTheme.layout.padding.padding7,
                            bottom = YGTheme.layout.padding.padding8,
                        ),
                ) {
                    GroupNicknameField(
                        nickname = state.nicknameInput,
                        errorMessageResId = state.errorMessageResId,
                        onNicknameChange = onNicknameChange,
                        onFocusChange = onNicknameFocusChange,
                        onConfirmNickname = confirmAndDismissKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    GroupMemberList(
                        members = state.members,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    YGInviteCard(
                        label = stringResource(R.string.group_setting_invite_label),
                        inviteCode = state.inviteCode,
                        subText = inviteCardSubText(state),
                        status = inviteCardStatus(state),
                        copyButtonText = stringResource(R.string.group_setting_copy),
                        onCopyClick = onClickCopyInviteCode,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    YGDangerZone(
                        topZone = {
                            YGActionItem(
                                text = stringResource(R.string.group_setting_leave),
                                onClick = onClickLeaveGroup,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        bottomZone = {
                            YGActionItem(
                                text = stringResource(R.string.group_setting_report),
                                onClick = onClickReportGroup,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        OnBack { onClickBack() }
    }
}

@Composable
private fun inviteCardSubText(state: GroupSettingUiState): String = when {
    state.remainingCount <= 0 -> stringResource(R.string.group_setting_invite_full)
    state.isCodeCopied -> stringResource(R.string.group_setting_invite_copied)
    else -> stringResource(R.string.group_setting_invite_remaining, state.remainingCount)
}

private fun inviteCardStatus(state: GroupSettingUiState): YGInviteCardStatus =
    if (state.remainingCount > 0) YGInviteCardStatus.Active else YGInviteCardStatus.Invalid
```

⚠️ **`confirmAndDismissKeyboard`의 호출 순서를 뒤집지 말 것.** `onConfirmNickname()`이 먼저다. 반대로 하면 `clearFocus()` → 포커스 상실 → ViewModel이 `cancelEditing()`으로 입력값을 되돌림 → 뒤이은 확정이 `isConfirmEnabled == false`라 **아무 일도 안 일어난다**. 지금 순서에서는 확정이 먼저 반영되고, 뒤따르는 포커스 상실의 `cancelEditing()`은 `nicknameInput`이 이미 `myNickname`과 같아 무해하다.

Contents 상단 패딩이 없는 것은 의도다. Figma에서 Status Bar(48) + Top Bar(60) = 108이고 Contents `top`도 108이라 **여백 0**이다.

- [ ] **Step 2: 프리뷰 추가**

같은 파일 하단에 붙인다.

```kotlin
private class GroupSettingPreviewParameterProvider :
    PreviewParameterProvider<GroupSettingUiState> {
    override val values: Sequence<GroupSettingUiState>
        get() = sequenceOf(
            GroupSettingUiState(),
            GroupSettingUiState(isCodeCopied = true),
            GroupSettingUiState(remainingCount = 0),
            GroupSettingUiState(
                groupName = "열글자를꽉채운그룹명",
                myNickname = "열다섯글자를꽉꽉채운닉네임야호",
                nicknameInput = "열다섯글자를꽉꽉채운닉네임야호",
            ),
        )
}

@YGPreview
@Composable
private fun GroupSettingScreenPreview(
    @PreviewParameter(GroupSettingPreviewParameterProvider::class)
    state: GroupSettingUiState,
) = PreviewBox {
    GroupSettingScreen(
        state = state,
        onClickBack = {},
        onNicknameChange = {},
        onNicknameFocusChange = {},
        onConfirmNickname = {},
        onClickCopyInviteCode = {},
        onClickLeaveGroup = {},
        onClickReportGroup = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

프리뷰용 import를 파일 상단에 추가한다.

```kotlin
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
```

- [ ] **Step 3: 컴파일·lint 검증**

Run:
```bash
./gradlew :feature:groups:setting:impl:compileDebugKotlin :feature:groups:setting:impl:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 프리뷰 육안 확인**

`GroupSettingScreenPreview` 4종을 연다. 확인할 것:
1. 위에서부터 상단바(그룹이름 + 뒤로) → 닉네임 라벨/필드 → `그룹원 (11)` + 칩 11개 → 초대 코드 카드 → 점선 Danger Zone 순서.
2. 두 번째 프리뷰에서 카드 우측이 `복사됨`, 세 번째에서 카드가 회색(`Invalid`) + `최대 인원 도달`.
3. **네 번째(긴 문자열) 프리뷰에서 상단바 제목이 잘리거나 2줄로 감기지 않는지**, 닉네임 15자가 필드 안에 카운터(`15/15`)와 겹치지 않는지.
4. 목록이 화면을 넘치면 스크롤된다(프리뷰에선 잘려 보이는 게 정상).

3번에서 문제가 보이면 멈추고 보고한다 — `YGTopBarDetail`은 `Text`에 `weight`가 걸려 있으나 `maxLines` 지정이 없어 긴 제목이 2줄로 감길 수 있다. 그 경우는 화면이 아니라 디자인시스템 쪽 문제이므로 임의로 고치지 말 것.

---

### Task 6: 편집 모드 — 하단 `확인` 버튼 영역

**Files:**
- Modify: `IMPL/screen/GroupSettingScreen.kt`

**Interfaces:**
- Consumes: Task 5의 `GroupSettingScreen` 본문, Task 3의 `GroupSettingUiState.isEditing`·`isConfirmEnabled`. 기존 DS `YGButton(text, buttonType, isEnabled, onClick, modifier, startIconResource, endIconResource, interactionSource)` · `YGButtonType.Large` · `YGAtomicColors.Gray.White`.
- Produces: 편집 상태에서만 보이는 하단 버튼 영역. 시그니처 변화 없음.

- [ ] **Step 1: 버튼 영역을 `Box` 하단에 추가**

`GroupSettingScreen`의 바깥 `Box` 안, 스크롤 `Column`을 담은 `Column` **다음**(= `Box`의 마지막 자식)에 아래를 넣는다.

```kotlin
            if (state.isEditing) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .imePadding()
                        .background(YGAtomicColors.Gray.White)
                        .padding(YGTheme.layout.padding.padding7),
                ) {
                    YGButton(
                        text = stringResource(R.string.group_setting_confirm),
                        buttonType = YGButtonType.Large,
                        isEnabled = state.isConfirmEnabled,
                        onClick = confirmAndDismissKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
```

버튼과 키보드 엔터가 **같은 `confirmAndDismissKeyboard`를 공유한다** — 확정 경로가 하나뿐이라는 것이 코드에도 드러나야 한다.

modifier 순서가 중요하다: `imePadding()`을 `background()` **앞**에 둬야 흰 배경이 키보드 위에서 끝난다. 뒤에 두면 배경이 키보드 뒤까지 칠해진다.

`state.isEditing`은 텍스트 필드 포커스와 같이 움직이고, 포커스가 있으면 키보드가 올라와 있다. 즉 "키보드가 올라와 있을 때만 노출"이 이 조건으로 성립한다. IME 인셋 높이를 직접 읽어 판정하지 않는 이유는 키보드 애니메이션 중에 버튼이 깜빡이기 때문이다.

- [ ] **Step 2: import 추가**

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Alignment
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
```

- [ ] **Step 3: 편집 상태 프리뷰 2종 추가**

`GroupSettingPreviewParameterProvider`의 `values` 시퀀스 끝에 두 항목을 더한다.

```kotlin
            GroupSettingUiState(
                isEditing = true,
                nicknameInput = "바꾼닉네임",
            ),
            GroupSettingUiState(
                isEditing = true,
                nicknameInput = " 잘못된닉네임",
                errorMessageResId = CoreR.string.error_space_at_edge_nickname,
            ),
```

import를 추가한다.

```kotlin
import com.teamyg.parfait.core.ui.R as CoreR
```

- [ ] **Step 4: 컴파일·lint 검증**

Run:
```bash
./gradlew :feature:groups:setting:impl:compileDebugKotlin :feature:groups:setting:impl:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 프리뷰 육안 확인**

새 프리뷰 2종에서:
1. 하단에 흰 배경 + `확인` 버튼이 보인다.
2. 다섯 번째(`바꾼닉네임`)는 버튼이 **활성**(검정 배경), 여섯 번째(공백 시작)는 **비활성**(회색) + 필드 아래 빨간 문구가 보이고 필드 테두리가 빨갛다.
3. 앞의 네 프리뷰에는 버튼 영역이 **없다**.

---

### Task 7: `GroupSettingRoute` 배선 + 클립보드 + 통합 확인

**Files:**
- Modify: `IMPL/route/GroupSettingRoute.kt`

**Interfaces:**
- Consumes: Task 3의 `GroupSettingViewModel`·`GroupSettingIntent`·`GroupSettingSideEffect`, Task 5·6의 `GroupSettingScreen`. 기존 `Navigator`(`goTo(NavKey)` · `onBack()`), 기존 `featureGroupSettingEntryBuilder`.
- Produces: 완성된 S-101 화면.

- [ ] **Step 1: Route 본문 구현**

`IMPL/route/GroupSettingRoute.kt` 전체를 아래로 교체한다(현재 내용은 `// TODO impl` 한 줄이다).

```kotlin
package com.teamyg.parfait.feature.groups.setting.impl.route

import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.setting.impl.screen.GroupSettingScreen
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingIntent
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingSideEffect
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingViewModel

private const val CLIP_LABEL_INVITE_CODE = "invite_code"

@Composable
internal fun GroupSettingRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupSettingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupSettingSideEffect.NavigateBack -> navigator.onBack()
                is GroupSettingSideEffect.CopyInviteCode -> clipboard.setClipEntry(
                    ClipEntry(
                        ClipData.newPlainText(CLIP_LABEL_INVITE_CODE, effect.inviteCode),
                    ),
                )
            }
        }
    }

    GroupSettingScreen(
        state = state,
        onClickBack = { viewModel.processIntent(GroupSettingIntent.ClickBack) },
        onNicknameChange = { viewModel.processIntent(GroupSettingIntent.InputNickname(it)) },
        onNicknameFocusChange = {
            viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(it))
        },
        onConfirmNickname = { viewModel.processIntent(GroupSettingIntent.ConfirmNickname) },
        onClickCopyInviteCode = {
            viewModel.processIntent(GroupSettingIntent.ClickCopyInviteCode)
        },
        onClickLeaveGroup = { viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup) },
        onClickReportGroup = { viewModel.processIntent(GroupSettingIntent.ClickReportGroup) },
        modifier = modifier,
    )
}
```

클립보드 API 근거: Compose BOM 2026.06.00(= `compose.ui` 1.11.x)에서 `LocalClipboardManager`는 deprecated이고 `LocalClipboard`가 현행이다. `Clipboard.setClipEntry(ClipEntry?)`는 `suspend` 함수인데, `LaunchedEffect`의 `collect` 블록 자체가 코루틴이라 별도 `rememberCoroutineScope` 없이 그대로 호출된다. `ClipEntry`는 Android에서 `ClipData`를 감싸는 생성자를 갖는다(2026-08-07 아티팩트 직접 확인).

- [ ] **Step 2: `EntryBuilder`가 손댈 것이 없음을 확인**

Read `IMPL/navigation/EntryBuilder.kt`.
확인할 것: `YGScaffold { innerPadding -> GroupSettingRoute(...) }` 구조가 그대로이고, `YGScaffold`의 `containerColor` 기본값이 이미 `YGAtomicColors.Gray.White`라 **수정이 필요 없다**.
구조가 다르면(예: `Scaffold` 직접 사용) 그때만 `YGScaffold`로 맞춘다.

- [ ] **Step 3: 모듈 컴파일·lint 검증**

Run:
```bash
./gradlew :feature:groups:setting:impl:compileDebugKotlin :feature:groups:setting:impl:ktlintCheck
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 앱 전체 빌드 + 유닛 테스트 재실행**

Run:
```bash
./gradlew :app:assembleDebug :feature:groups:setting:impl:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL. (`GroupSettingViewModel`의 `CheckNameValidUseCase` 주입이 여기서 검증된다. `local.properties`에 카카오 키가 없으면 실패할 수 있는데 그건 이 계획과 무관한 환경 문제다 — 그 경우 기존 값으로 채우고 다시 돌린다.)

- [ ] **Step 5: 실기기/에뮬레이터 동작 확인**

`NavKeyGroupSetting`으로 `goTo` 하는 호출자가 아직 없다. 확인은 둘 중 하나로 한다.
- (권장) Android Studio 프리뷰의 인터랙티브 모드로 `GroupSettingScreenPreview` 실행.
- 또는 앱 시작 목적지를 임시로 `NavKeyGroupSetting`으로 바꿔 실행하고 **확인 후 반드시 되돌린다**.

확인 항목:
1. 닉네임 필드를 탭하면 키보드가 오르고 하단 `확인` 버튼이 **키보드 바로 위**에 붙어 함께 움직인다. 키보드가 내려가면 버튼도 사라진다.
2. 한 글자 지우면 버튼이 활성, 원래 값으로 되돌리면 다시 비활성.
3. 특수문자(`!`)를 넣으면 즉시 빨간 테두리 + 에러 문구 + 버튼 비활성.
4. 16번째 글자가 입력되지 않는다(카운터 `15/15`에서 멈춤).
5. **`확인` 버튼**을 누르면 키보드/버튼이 사라지고, 그룹원 목록 첫 줄의 내 닉네임이 새 값으로 바뀐다.
6. **키보드 우측 하단 완료(엔터)** 를 눌러도 5번과 똑같이 확정된다.
7. 유효성 미통과 상태에서 완료(엔터)를 누르면 **아무 일도 안 일어난다**(확정 안 됨, 키보드 유지).
8. 편집 중 배경을 탭하거나 뒤로가기를 하면 입력값이 원래 닉네임으로 되돌아온다(화면은 유지).
9. 편집 중이 아닐 때 뒤로가기를 하면 화면이 닫힌다.
10. `복사` 버튼을 누르면 카드 우측이 `복사됨`으로 바뀌고, 다른 앱(메모 등)에 붙여넣으면 `WDIDCJ`가 나온다.
11. 그룹 나가기·신고하기를 눌러도 아무 일도 안 일어난다(Logcat에 `handleClickLeaveGroup`·`handleClickReportGroup` 로그만). **이것이 의도한 stub 동작이다.**

- [ ] **Step 6: 결과 보고**

Task 1~7에서 검증한 명령 결과와 위 11개 확인 항목의 통과 여부를 보고한다. **커밋하지 않는다.**

---

## 검증 요약

| 단계 | 명령 |
|---|---|
| ViewModel 유닛 테스트 | `./gradlew :feature:groups:setting:impl:testDebugUnitTest` (12개) |
| 모듈 컴파일 | `./gradlew :feature:groups:setting:impl:compileDebugKotlin` |
| 디자인시스템 컴파일 | `./gradlew :core:designsystem:compileDebugKotlin` |
| 포맷 검사 | `./gradlew :feature:groups:setting:impl:ktlintCheck :core:designsystem:ktlintCheck` |
| Hilt 그래프 · 기존 호출부 회귀 | `./gradlew :app:assembleDebug` |
| 육안 | `@YGPreview` 6종 + 실기기 11개 항목 |

유닛 테스트가 잡는 것: 유효성 매핑 · 확정 가드 · 편집 취소/복원 · SideEffect 방출.
**유닛 테스트가 못 잡는 것**(육안이 유일한 그물): 확정과 포커스 해제의 호출 순서 · `imePadding()` 동작 · 클립보드 실제 복사 · 긴 문자열 레이아웃.

## 이 계획이 다루지 않는 것

- 그룹 상세 조회·닉네임 변경·탈퇴·신고 **API 연동** 전량(Repository·UseCase 신설 없음)
- 그룹 나가기·신고 **확인 모달**(Figma 미제공)
- 상단바 `List-Member` 멤버 겹침 칩(Figma `opacity 0`)
- 이 화면으로 들어오는 **진입 경로 결선**(G-001 또는 C-001에서 `goTo`)
- `복사됨` 문구의 자동 복귀(디자인 미기재 — 화면 이탈 전까지 유지가 이번 결정)
