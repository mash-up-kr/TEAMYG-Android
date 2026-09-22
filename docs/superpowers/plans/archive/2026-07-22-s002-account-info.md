---
id: s002-account-info
title: S-002 계정 정보 화면 (AccountInfo) 구현 계획
status: done
type: work-order
created: 2026-07-22
updated: 2026-08-04
platforms: android
owner: TJYG-Android
related_adr:
  - ADR-0016
related_spec:
  - s002-account-info
  - clearfocusontap-modifier
  - app-setting-s001
related_code:
  - AccountInfoRoute.kt#AccountInfoRoute
  - AccountInfoScreen.kt#AccountInfoScreen
  - AccountInfoViewModel.kt#AccountInfoViewModel
  - ClearFocusOnTap.kt#clearFocusOnTap
  - feature/app/setting/impl/res/values/strings.xml
archived_reason: PR #192로 develop 머지 완료(2026-08-04) — Task 1은 #179 선행 머지로 skip, Task 2~5 산출물 반영. 체크박스는 미체크로 남았고 as-built 4건은 스펙이 정본
tags: [plan, parfait, setting, account, nickname, s002]
---

# S-002 계정 정보 화면 (AccountInfo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** S-002 계정 정보 화면 구현 — 닉네임 조회·수정(실시간 유효성) + 로그아웃/탈퇴 진입점.

**Architecture:** 기존 `feature/app/setting/impl`의 AppSetting MVI 패턴을 미러(Route가 ViewModel 수집·SideEffect 처리 → Screen은 순수 UI). 닉네임 유효성은 공유 `domain` `CheckNameValidUseCase`를 재사용하되 빈 값 규칙(`CheckEmpty`)을 선두에 추가. 저장 API·로그아웃/탈퇴는 미연동(Intent만 정의, VM 핸들러 stub).

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Navigation3, core:designsystem(YG* 컴포넌트), core:ui(BaseViewModel).

> **⚠️ As-built 애드덤 (2026-07-23, [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md))** — 아래 Task 1·2·4는 "에러 문자열을 UseCase가 소유"를 전제로 작성됐으나, 구현 중 i18n 리팩터로 변경됨:
> - `NicknameResult`를 **sealed interface**로 전환(`Success`/`Error.{Empty,SpaceAtEdge,DuplicatedSpace,InvalidCharacter}`), 문자열 제거. UseCase는 `Error` 변형 반환.
> - 에러 표시 문자열은 **`core:ui`** `text/NickNameResultUiText.kt#toStringResource`(@Composable) + `core:ui` `strings.xml`로 이동(`core:ui`에 `:domain` 의존 추가). setting·groups 공용.
> - UI State는 `nickNameError: NicknameResult.Error?` 보유(문자열 아님), Screen이 `toStringResource()`로 매핑.
> - S-102(GroupNickName VM/Screen) 동반 리팩터. 확정 API는 스펙 [2026-07-22-s002-account-info.md](../../specs/archive/2026-07-22-s002-account-info.md) 참조.

> **⚠️ 2차 애드덤 (2026-07-29, PR #179 develop 머지)** — **Task 1은 이미 완료된 것으로 간주하고 건너뛴다.**
> A-005 그룹 생성 작업이 같은 domain 리팩터를 먼저 머지했다. 단 형태가 위 1차 애드덤과 다르다:
> `NicknameResult` → **`NameValidResult`**, `Error.Empty` → **`Error.EmptyString`**(enum 마지막 — 결과 동일),
> UseCase 패키지 `domain.usecase`(`.group` 제거)·인자명 `name`, 길이 상한은 `domain` **`GroupCreateConfig.NICKNAME_MAX_LENGTH`**
> (Global Constraints의 `NICKNAME_MAX_LENGTH = 15` 지역 상수 항목은 무효 — 새로 정의하지 말 것),
> 에러 문자열은 `core:ui` `strings.xml`에 존재하나 **`toStringResource()` 확장은 없다** → 매핑은 ViewModel `when`이 `@StringRes` ID 산출.
> Task 2·4는 `nickNameErrorResId: Int?` 기준으로 읽는다. 재작성 시 `GroupNickNameViewModel`을 미러하고,
> 매핑 위치 수렴 여부는 [open-questions](../../../synthesis/open-questions.md) [2026-07-29] 결정 후 반영한다.

> **⚠️ 3차 애드덤 (2026-08-03, 구현 완료 후 develop rebase + Figma 대조)** — **이 계획은 사실상 완료됐다.**
> 아래 체크박스는 미체크로 남아 있으나 Task 1~5는 브랜치 `feature/#86-app-setting-account-info-screen`에
> 구현·rebase 완료 상태다(Task 6 실기기 확인만 미수행). 다만 **as-built가 계획과 4곳 다르다** —
> 확정 계약은 스펙 [2026-07-22-s002-account-info.md](../../specs/archive/2026-07-22-s002-account-info.md)가 정본이고, 이 계획을 그대로 재실행하지 말 것:
>
> 1. **Danger Zone 삭제** — Figma `S-002`(node `220:2192`)에 Danger Zone이 없다. 로그아웃/탈퇴는
>    `S-001`(node `220:2176`) 소속 → Intent(`ClickLogout`/`ClickWithdraw`)·VM stub 핸들러·문자열 2종을
>    **전부 `AppSettingViewModel`/`AppSettingScreen`으로 이관**했다([app-setting-s001 스펙](../../specs/archive/2026-07-19-app-setting-s001.md)).
>    문자열 키도 `account_info_*` → `setting_logout`/`setting_withdraw`로 개명.
> 2. **State 필드명** — `nickName`/`nickNameErrorResId` → **`nickname`/`errorMessageResId`**(`GroupNickNameViewModel` as-built 미러).
> 3. **상단바** — `YGTopBarBack` → **`YGTopBarDetail(title = "계정 정보")`**(Figma Top Bar에 타이틀 존재).
>    섹션 라벨도 인라인 `Text` → **`YGLabel`**.
> 4. **포커스 해제** — `YGScreen`에 상시 결선돼 있던 배경 탭 포커스 해제가 접근성 사유로 철회되고,
>    opt-in **`Modifier.clearFocusOnTap()`**(신규, [스펙](../../specs/archive/2026-08-03-clearfocusontap-modifier.md))으로 대체됐다.
>    `AccountInfoScreen`은 `YGScreen(modifier = modifier.clearFocusOnTap())`으로 명시 적용한다.
>
> 1차 애드덤의 `NicknameResult`·`toStringResource` 경로는 **rebase에서 폐기**됐다(develop `NameValidResult`와 충돌).
> 최종은 2차 애드덤 형태 + 위 4건이다.
>
> ✅ **2026-08-04 develop 머지(PR #192)** — 체크박스는 미체크로 남기고 계획을 종료(`status: done`)한다.
> Task 6 Step 2(실기기 동작 확인) 기록은 없다. 산출물 계약은 [스펙](../../specs/archive/2026-07-22-s002-account-info.md)이 정본.

## Global Constraints

- 코드 대상 repo: `TJYG-Android`(remote `mash-up-kr/TJYG-Android`). 브랜치 `feature/#86-app-setting-account-info-screen`(이미 체크아웃됨).
- 닉네임 최대 길이 15자(`NICKNAME_MAX_LENGTH = 15`), `YGTextFormField(maxLength = 15)`로 하드 차단.
- 에러 메시지는 `CheckNameValidUseCase`가 반환하는 하드코딩 문자열을 그대로 사용(별도 string 리소스 없음, S-102 동일 패턴).
- 테스트 코드 미작성(repo 테스트 인프라 없음) → 검증은 컴파일 + ktlint + `@YGPreview`.
- `:domain`·`:core:designsystem`·`:core:ui`·`:core:navigation`은 feature.impl 컨벤션 플러그인이 자동 포함 → build.gradle 변경 불필요.
- MVI 계약(`UiState`/`UiIntent`/`UiSideEffect`, `BaseViewModel`, `viewModelLogger`, `updateState`/`postSideEffect`)은 `com.teamyg.parfait.core.ui`.
- 커밋 메시지는 한국어 `feat:`/`refactor:` 컨벤션. 커밋·푸시·PR은 사용자 확인 후.

---

### Task 1: 공유 UseCase에 빈 값 규칙 추가 (`CheckEmpty`)

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/group/CheckNameValidUseCase.kt`

**Interfaces:**
- Consumes: 없음(기존 `NameValidation` enum 확장).
- Produces: `CheckNameValidUseCase.invoke(nickName: String): NicknameResult` — 시그니처 무변경. 빈 문자열 입력 시 `NicknameResult(isSuccess=false, errorMessage="닉네임은 비워둘 수 없어요")` 반환(가장 먼저).

- [ ] **Step 1: `NameValidation` enum 선두에 `CheckEmpty` 추가**

`private enum class NameValidation`의 첫 엔트리로 삽입(enum 순서 = 검사 순서, `entries.forEach`가 순차 검사·첫 실패 반환):

```kotlin
private enum class NameValidation(
    val isValid: (String) -> Boolean,
    val errorMessage: String,
) {
    CheckEmpty(
        isValid = { nickName ->
            nickName.isNotEmpty()
        },
        errorMessage = "닉네임은 비워둘 수 없어요",
    ),

    CheckSpaceStartOrEnd(
        isValid = { nickName ->
            nickName.startsWith(" ").not() && nickName.endsWith(" ").not()
        },
        errorMessage = "닉네임의 처음과 끝에는 공백을 사용할 수 없어요",
    ),

    CheckDuplicatedSpace(
        isValid = { nickName ->
            nickName.indexOf("  ") == -1
        },
        errorMessage = "공백은 글자 사이에 1칸만 사용할 수 있어요",
    ),

    CheckValidCharacter(
        isValid = { nickName ->
            nickName.all { it.isWhitespace() || it.isDigit() || it.isLetter() || it.isKorean() }
        },
        errorMessage = "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요",
    ),
}
```

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :domain:compileDebugKotlin` (또는 `:domain:compileKotlin` — JVM 모듈이면)
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: ktlint 확인**

Run: `./gradlew :domain:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/usecase/group/CheckNameValidUseCase.kt
git commit -m "feat: CheckNameValidUseCase 빈 값 검사(CheckEmpty) 규칙 추가"
```

---

### Task 2: AccountInfoViewModel (State/Intent/SideEffect + MVI)

**Files:**
- Create: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AccountInfoViewModel.kt`

**Interfaces:**
- Consumes: `CheckNameValidUseCase`(Task 1) — `invoke(String): NicknameResult`. `com.teamyg.parfait.core.ui.{BaseViewModel, UiState, UiIntent, UiSideEffect, viewModelLogger}`.
- Produces:
  - `AccountInfoUiState(nickName: String, errorMessage: String?)` — Screen(Task 4)이 소비.
  - `AccountInfoIntent`: `InputWord(nickName: String)`, `ClickBack`, `ClickLogout`, `ClickWithdraw`.
  - `AccountInfoSideEffect`: `NavigateBack`.
  - `AccountInfoViewModel.processIntent(intent)`, `state`, `effect` — Route(Task 5)가 소비.

- [ ] **Step 1: ViewModel 파일 작성**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.usecase.group.CheckNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property nickName TODO 프로필 API 연동 전 placeholder 데이터
 * @property errorMessage null=정상, non-null=유효성 위반 메시지
 */
data class AccountInfoUiState(
    val nickName: String = "대충지은랜덤닉네임",
    val errorMessage: String? = null,
) : UiState

sealed interface AccountInfoIntent : UiIntent {
    data class InputWord(val nickName: String) : AccountInfoIntent

    data object ClickBack : AccountInfoIntent

    data object ClickLogout : AccountInfoIntent

    data object ClickWithdraw : AccountInfoIntent
}

sealed interface AccountInfoSideEffect : UiSideEffect {
    data object NavigateBack : AccountInfoSideEffect
}

@HiltViewModel
class AccountInfoViewModel
@Inject
constructor(
    private val checkNameValid: CheckNameValidUseCase,
) : BaseViewModel<AccountInfoUiState, AccountInfoIntent, AccountInfoSideEffect>(
    initialState = AccountInfoUiState(),
) {
    init {
        viewModelLogger.i { "AccountInfoViewModel::init" }
    }

    override fun processIntent(intent: AccountInfoIntent) {
        when (intent) {
            is AccountInfoIntent.InputWord -> handleInputWord(intent.nickName)
            AccountInfoIntent.ClickBack -> handleClickBack()
            AccountInfoIntent.ClickLogout -> handleClickLogout()
            AccountInfoIntent.ClickWithdraw -> handleClickWithdraw()
        }
    }

    private fun handleInputWord(nickName: String) {
        val result = checkNameValid(nickName)
        updateState {
            copy(
                nickName = nickName,
                errorMessage = result.errorMessage,
            )
        }
    }

    private fun handleClickBack() {
        postSideEffect(AccountInfoSideEffect.NavigateBack)
    }

    private fun handleClickLogout() {
        // TODO auth 로그아웃 연동 전 stub
        viewModelLogger.i { "AccountInfoViewModel::handleClickLogout (stub)" }
    }

    private fun handleClickWithdraw() {
        // TODO 회원 탈퇴 API 연동 전 stub
        viewModelLogger.i { "AccountInfoViewModel::handleClickWithdraw (stub)" }
    }
}
```

> 주의: `handleInputWord`는 값 갱신과 동시에 유효성 결과를 반영(실시간 검증). `result.isSuccess`면 `result.errorMessage`가 `null`이므로 별도 분기 불필요.

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`BaseViewModel`의 `state`/`effect`/`updateState`/`postSideEffect` 시그니처는 기존 `AppSettingViewModel`과 동일 — 불일치 시 `AppSettingViewModel.kt` 참조.)

- [ ] **Step 3: ktlint 확인**

Run: `./gradlew :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AccountInfoViewModel.kt
git commit -m "feat: AccountInfoViewModel MVI(닉네임 실시간 검증·로그아웃/탈퇴 stub)"
```

---

### Task 3: strings.xml 문자열 추가

**Files:**
- Modify: `feature/app/setting/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Produces: `R.string.account_info_title`, `R.string.account_info_nickname_label`, `R.string.account_info_logout`, `R.string.account_info_withdraw` — Screen(Task 4)이 `stringResource`로 소비.

- [ ] **Step 1: `<resources>`에 문자열 4개 추가**

기존 항목 아래에 삽입:

```xml
    <string name="account_info_title">계정 정보</string>
    <string name="account_info_nickname_label">닉네임</string>
    <string name="account_info_logout">로그아웃</string>
    <string name="account_info_withdraw">서비스 탈퇴하기</string>
```

- [ ] **Step 2: ktlint 확인(리소스 포함 빌드로 병행 검증은 Task 4에서)**

Run: `./gradlew :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add feature/app/setting/impl/src/main/res/values/strings.xml
git commit -m "feat: 계정 정보 화면 문자열 리소스 추가"
```

---

### Task 4: AccountInfoScreen (stateless UI + 프리뷰)

**Files:**
- Create: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/screen/AccountInfoScreen.kt`

**Interfaces:**
- Consumes: `AccountInfoUiState`(Task 2). DS: `YGScreen`(+ `OnBack`), `YGTopBarDetail`(title 파라미터 有), `YGTextFormField`, `YGTextFormFieldDefaults`, `YGDangerZone`, `YGActionItem`, `YGTheme`, `YGAtomicColors`. `R.string.*`(Task 3). `com.teamyg.parfait.core.designsystem.utils.preview.{PreviewBox, YGPreview}`.
- Produces: `AccountInfoScreen(uiState, onValueChanged, onClickBack, onClickLogout, onClickWithdraw, modifier)` — Route(Task 5)가 소비.

- [ ] **Step 1: Screen 파일 작성**

레이아웃은 기본 프레임(220-2192): TopBar → "닉네임" 라벨 → `YGTextFormField` → `YGDangerZone`. 패딩·간격은 `AppSettingScreen`과 동일 토큰 사용.

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormField
import com.teamyg.parfait.core.designsystem.component.textfield.YGTextFormFieldDefaults
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygdangerzone.YGDangerZone
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R

private const val NICKNAME_MAX_LENGTH = 15

@Composable
internal fun AccountInfoScreen(
    uiState: AccountInfoUiState,
    onValueChanged: (nickName: String) -> Unit,
    onClickBack: () -> Unit,
    onClickLogout: () -> Unit,
    onClickWithdraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.account_info_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap8),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues = PaddingValues(
                            top = YGTheme.layout.padding.padding8,
                            start = YGTheme.layout.padding.padding7,
                            end = YGTheme.layout.padding.padding7,
                        ),
                    ),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.account_info_nickname_label),
                        style = YGTheme.typography.body.b02R,
                        color = YGAtomicColors.Gray.Gray400,
                    )
                    YGTextFormField(
                        value = uiState.nickName,
                        onValueChange = onValueChanged,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.errorMessage != null,
                        maxLength = NICKNAME_MAX_LENGTH,
                        errorDescription = uiState.errorMessage,
                        colors = YGTextFormFieldDefaults.colors(),
                    )
                }

                YGDangerZone(
                    topZone = {
                        YGActionItem(
                            text = stringResource(R.string.account_info_logout),
                            onClick = onClickLogout,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    bottomZone = {
                        YGActionItem(
                            text = stringResource(R.string.account_info_withdraw),
                            onClick = onClickWithdraw,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        OnBack { onClickBack() }
    }
}

private class AccountInfoPreviewParameterProvider :
    PreviewParameterProvider<AccountInfoUiState> {
    override val values: Sequence<AccountInfoUiState>
        get() = sequenceOf(
            AccountInfoUiState(nickName = "대충지은랜덤닉네임"),
            AccountInfoUiState(nickName = "닉네임바꾸"),
            AccountInfoUiState(
                nickName = "",
                errorMessage = "닉네임은 비워둘 수 없어요",
            ),
            AccountInfoUiState(
                nickName = " 가",
                errorMessage = "닉네임의 처음과 끝에는 공백을 사용할 수 없어요",
            ),
        )
}

@YGPreview
@Composable
private fun AccountInfoScreenPreview(
    @PreviewParameter(AccountInfoPreviewParameterProvider::class) uiState: AccountInfoUiState,
) = PreviewBox {
    AccountInfoScreen(
        uiState = uiState,
        onValueChanged = {},
        onClickBack = {},
        onClickLogout = {},
        onClickWithdraw = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

> 주의:
> - `YGScreen`의 `OnBack`은 리시버 스코프(`YGScreenScope`)에서 제공 — `AppSettingScreen.kt`와 동일하게 블록 안에서 호출.
> - 타이틀 "계정 정보"는 `YGTopBarDetail(title=...)`가 렌더(S-102 `GroupNickNameScreen`과 동일 컴포넌트). `YGTopBarBack`은 뒤로 아이콘만 있어 부적합.
> - `YGTextFormField`에 `placeholder` 미지정(초기값이 placeholder 닉네임이라 빈 상태 진입 없음). 빈 값은 클리어 시에만 발생 → 에러 노출.

- [ ] **Step 2: 컴파일 확인(리소스 병합 포함)**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. (`R.string.account_info_*` 미해결 시 Task 3 병합 확인.)

- [ ] **Step 3: ktlint 확인**

Run: `./gradlew :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Android Studio 프리뷰 렌더 확인**

`AccountInfoScreen.kt`의 `@YGPreview` 4변형(정상 2 + 에러 2) 렌더 확인: 카운터(N/15)·에러 테두리·에러 메시지·DangerZone 점선.

- [ ] **Step 5: 커밋**

```bash
git add feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/screen/AccountInfoScreen.kt
git commit -m "feat: AccountInfoScreen UI(닉네임 폼·DangerZone·프리뷰)"
```

---

### Task 5: AccountInfoRoute 배선 (stub 본문 채움)

**Files:**
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/route/AccountInfoRoute.kt`

**Interfaces:**
- Consumes: `AccountInfoViewModel`·`AccountInfoIntent`·`AccountInfoSideEffect`(Task 2), `AccountInfoScreen`(Task 4). `Navigator.onBack()`. `hiltViewModel`, `collectAsStateWithLifecycle`, `LaunchedEffect`.
- Produces: `AccountInfoRoute(navigator, modifier)` — `EntryBuilder`가 이미 호출(무변경).

- [ ] **Step 1: Route 본문 작성**

기존 stub 전체를 교체(`AppSettingRoute.kt` 패턴 미러):

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.AccountInfoScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.AccountInfoViewModel

@Composable
internal fun AccountInfoRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: AccountInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                AccountInfoSideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    AccountInfoScreen(
        uiState = state,
        onValueChanged = { viewModel.processIntent(AccountInfoIntent.InputWord(it)) },
        onClickBack = { viewModel.processIntent(AccountInfoIntent.ClickBack) },
        onClickLogout = { viewModel.processIntent(AccountInfoIntent.ClickLogout) },
        onClickWithdraw = { viewModel.processIntent(AccountInfoIntent.ClickWithdraw) },
        modifier = modifier,
    )
}
```

> 주의: `import`·시그니처(`hiltViewModel`·`collectAsStateWithLifecycle`·`effect.collect`)는 `AppSettingRoute.kt`와 정확히 동일 — 불일치 시 그 파일 참조.

- [ ] **Step 2: 컴파일 확인**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: ktlint 확인**

Run: `./gradlew :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/route/AccountInfoRoute.kt
git commit -m "feat: AccountInfoRoute 배선(VM 수집·뒤로가기 이펙트)"
```

---

### Task 6: 통합 빌드 + 실기기/에뮬 동작 확인

**Files:** 없음(검증 전용).

- [ ] **Step 1: 모듈 전체 빌드**

Run: `./gradlew :feature:app:setting:impl:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 앱 실행 → S-001 앱 설정 → "계정 정보" 진입**

확인:
- 닉네임 폼에 placeholder 닉네임 + 카운터 표시.
- 입력 시 실시간 검증: 클리어(빈 값)→"닉네임은 비워둘 수 없어요", 앞/뒤 공백·연속 공백·이모지/특수문자 각 메시지, 16번째 글자 입력 차단(카운터 15/15 고정).
- 포커스 시 cherry 테두리, 에러 시 red 테두리 + 하단 메시지.
- 로그아웃/서비스 탈퇴하기 탭 → 무동작(로그만, stub).
- 뒤로가기 → S-001 복귀.

- [ ] **Step 3: (동작 확인 완료 후) 스펙 상태 갱신 — 별도(이 repo)**

`parfait/specs/2026-07-22-s002-account-info.md` frontmatter `status: draft → implemented`, `verified: <날짜>` 갱신 + `archive/` 이동, README 인덱스 갱신. `related_code`의 심볼이 실제와 일치하는지 대조. (이 커밋은 spec repo `specs/s002-account-info` 브랜치.)

---

## Self-Review

**Spec coverage:**
- 닉네임 폼·실시간 검증 → Task 2(VM)·Task 4(Screen). ✅
- `CheckEmpty` 규칙 추가 → Task 1. ✅
- maxLength=15 하드 차단 → Task 4(`NICKNAME_MAX_LENGTH`). ✅
- 로그아웃/탈퇴 Intent + VM stub → Task 2. ✅
- YGDangerZone/YGActionItem UI → Task 4. ✅
- Route 배선·뒤로가기 → Task 5. ✅
- strings 4개(에러 제외) → Task 3. ✅
- 저장 API 미연동(placeholder) → Task 2 State 초기값 + 주석. ✅
- 검증(테스트 없음, 컴파일+ktlint+프리뷰) → 각 Task Step + Task 6. ✅

**Placeholder scan:** 코드 내 `TODO`는 stub 핸들러 2곳(의도된 스코프 제외)·State placeholder 주석뿐. 계획 지시에 미완성 placeholder 없음. ✅

**Type consistency:** `AccountInfoUiState`/`AccountInfoIntent`(`InputWord`/`ClickBack`/`ClickLogout`/`ClickWithdraw`)/`AccountInfoSideEffect.NavigateBack`/`onValueChanged`/`AccountInfoScreen` 시그니처가 Task 2·4·5 전반에서 일치. `CheckNameValidUseCase.invoke` 반환 `NicknameResult.errorMessage` 사용 일관. ✅

**DS 시그니처 확인 완료:**
- `YGTopBarDetail(title, onIconClick, modifier)` — 타이틀 렌더용, 사용 확정.
- `YGTextFormField(value, onValueChange, isError, maxLength, errorDescription, colors)`·`YGDangerZone(topZone, bottomZone)`·`YGActionItem(text, onClick)`·`YGScreenScope.OnBack` — 전부 실재 확인.
