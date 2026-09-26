# 설정 Danger Zone 확인 팝업 구현 계획

> ✅ **완료 · develop 머지(2026-08-13, PR #225).** Task 1~5 전량 실행 후 브랜치
> `feature/group-and-app-setting-pop-up`이 머지됐다. 체크박스는 실행 기록을 이 블록에 모으는
> 관례대로 미체크로 둔다. **산출물 계약의 정본은
> [스펙](../../specs/archive/2026-08-09-setting-danger-zone-popups.md)이다.**
>
> 머지 시점 재대조 결과 **계획서가 틀린 곳 0건 · 드리프트 0건** — 계획의 코드블록이 그대로 as-built다.
> 계획 이후 추가된 것은 2026-08-10 독립 리뷰가 넣은 **확인 핸들러 3종의 멱등 가드**와 교차 방어
> 테스트뿐이다(커밋 `6965d901`). 유닛 테스트 as-built: `AppSettingViewModelTest` 5개,
> `GroupSettingViewModelTest` 16개 → 24개.
>
> 계획서의 "커밋하지 마라" 제약은 실행 시점에 뒤집혔다(SDD가 Task별 `BASE..HEAD` diff로 리뷰
> 패키지를 만들어 커밋 없이는 리뷰 게이트가 못 돈다 — 사용자가 Task별 로컬 커밋을 승인). 육안 확인
> 8항목은 기기 잠금으로 미수행이고 그중 코드 대조로 판정 가능한 것만 닫혔다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 앱 설정의 "서비스 탈퇴하기"와 그룹 설정의 "그룹 나가기"·"그룹 신고하기"를 눌렀을 때 피그마가 정의한 확인 모달이 뜨고, 취소하면 닫히도록 배선한다.

**Architecture:** 신규 디자인시스템 컴포넌트를 만들지 않는다 — 피그마 3개 팝업은 모두 이미 존재하는 `YGModalPopup`(`core:designsystem` `component/modal/`)의 인스턴스다. 팝업 표시 여부는 각 화면 ViewModel의 `UiState`가 소유하고(확인 핸들러가 곧 API를 부를 자리이므로), 화면은 그 상태를 읽어 조건부로 `YGModalPopup`을 호출만 한다. 확인 버튼은 이번 범위에서 팝업을 닫고 TODO 로그만 남긴다.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, 자체 MVI(`BaseViewModel`/`UiState`/`UiIntent`), JUnit4 + kotlin-test + kotlinx-coroutines-test(`MainDispatcherRule`), Gradle 컨벤션 플러그인.

**설계 정본:** [`parfait/specs/2026-08-09-setting-danger-zone-popups.md`](../../specs/archive/2026-08-09-setting-danger-zone-popups.md)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 저장소가 아니다. 모든 경로는 `TJYG-Android` 루트 기준이다. 현재 브랜치는 `feature/group-and-app-setting-pop-up`.
- **`git commit`을 하지 마라.** 이 저장소는 기본이 미커밋이다. 각 태스크는 파일 변경 + 테스트 통과까지만 하고 멈춘다. 커밋은 사람이 판단한다. (계획서 관례상 커밋 스텝이 있어야 할 자리에도 넣지 않았다 — 의도적이다.)
- **디자인시스템(`core/designsystem/`)을 수정하지 마라.** `YGModalPopup`·`YGButtonType`·`ic_warning_round` 전부 그대로 쓴다.
- **파괴적 액션은 왼쪽 `secondaryText`, 취소("그만두기")는 오른쪽 `primaryText`다.** `YGModalPopup`은 confirm/cancel 의미를 규정하지 않고 좌/우 버튼 타입만 노출한다. 뒤집으면 취소하려던 사용자가 탈퇴한다.
- **문구는 하드코딩하지 말고 모듈 `strings.xml`에 넣는다.** 줄바꿈은 `\n`. 마침표 유무는 아래 표 그대로 — "그룹 나가기" 본문만 마침표가 없다.
- **ktlint 규칙**: 들여쓰기 4칸, 최대 줄 길이 120, `ktlint_code_style = android_studio`. 테스트 함수명에 백틱 한글을 쓰지 않는다(minSdk 26이라 기기에서 깨진다) — 기존 `GroupSettingViewModelTest`의 `메서드명_상황_기대결과` 영문 스타일을 따른다.
- **테스트 주석은 한국어 Given/When/Then**. 기존 `GroupSettingViewModelTest` 스타일을 그대로 따른다.
- **팝업은 `@Preview`에 렌더되지 않는다** — Compose `Dialog`가 별도 window이기 때문. 프리뷰 파라미터에 팝업 상태를 추가하지 마라. 프리뷰 함수는 새 콜백 인자를 채워 **컴파일만 통과**시키면 된다.

### 팝업 문구 정본 (피그마)

| 팝업 | 제목 | 본문 | 좌(파괴적) | 우(취소) |
|---|---|---|---|---|
| 서비스 탈퇴 | `파르페에서 탈퇴하시겠어요?` | `지금까지 올린 사진은 익명으로 표시되며,\n삭제되지 않아요.` | `탈퇴하기` | `그만두기` |
| 그룹 나가기 | `그룹에서 나갈까요?` | `그룹에서 나가도\n그룹에 올렸던 사진은 지워지지 않아요` | `나가기` | `그만두기` |
| 그룹 신고 | `그룹을 신고할까요?` | `신고 후에는 그룹에서 자동으로 나가지며,\n그룹은 운영 정책에 따라 처리 돼요.` | `신고하기` | `그만두기` |

---

## 파일 구성

**Task 1 — 앱 설정 ViewModel**
- Modify: `feature/app/setting/impl/build.gradle.kts` — 유닛 테스트 컨벤션 플러그인 추가
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModel.kt` — 팝업 상태 필드 + Intent 2종 + 핸들러
- Create: `feature/app/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModelTest.kt`

**Task 2 — 앱 설정 UI**
- Modify: `feature/app/setting/impl/src/main/res/values/strings.xml` — 문자열 4건
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/screen/AppSettingScreen.kt` — 콜백 2개 + 팝업 호출
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/route/AppSettingRoute.kt` — 콜백 → Intent 배선

**Task 3 — 그룹 설정 ViewModel**
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModel.kt` — `GroupSettingDialog` 열거형 + 상태 필드 + Intent 3종 + 핸들러
- Modify: `feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModelTest.kt` — 테스트 추가

**Task 4 — 그룹 설정 UI**
- Modify: `feature/groups/setting/impl/src/main/res/values/strings.xml` — 문자열 7건
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/screen/GroupSettingScreen.kt` — 콜백 3개 + 팝업 분기
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/route/GroupSettingRoute.kt` — 콜백 → Intent 배선

**Task 5 — 통합 검증**
- 수정 없음. 빌드·린트·육안 확인.

**손대지 않는 파일:** `core/designsystem/**`, `app-preview/**`, 두 feature의 `EntryBuilder.kt`·`NavigationModule.kt`·`:api` 모듈.

---

### Task 1: 앱 설정 ViewModel — 탈퇴 팝업 상태

**Files:**
- Modify: `feature/app/setting/impl/build.gradle.kts`
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModel.kt`
- Test: `feature/app/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModelTest.kt` (신규)

**Interfaces:**
- Consumes: `BaseViewModel`의 `state: StateFlow<S>`, `updateState { copy(...) }`, `processIntent(intent)`, `viewModelLogger` — 전부 `com.teamyg.parfait.core.ui` 소속이고 이미 이 파일이 쓰고 있다. `MainDispatcherRule`은 `com.teamyg.parfait.core.testing`.
- Produces: `AppSettingState.isWithdrawDialogVisible: Boolean`(기본 `false`), `AppSettingIntent.ClickWithdraw`(열기), `AppSettingIntent.ConfirmWithdraw`(닫기 + TODO), `AppSettingIntent.DismissWithdrawDialog`(닫기). Task 2가 이 이름들을 그대로 쓴다.

- [ ] **Step 1: 유닛 테스트 컨벤션 플러그인 추가**

이 모듈에는 아직 테스트 소스셋이 없다. `feature/groups/setting/impl/build.gradle.kts`가 쓰는 것과 같은 플러그인을 붙인다. 이 플러그인이 `:core:testing`(`MainDispatcherRule`)·kotlin-test·coroutines-test·Turbine 의존성을 함께 넣어주므로 `dependencies` 블록은 건드리지 않는다.

`feature/app/setting/impl/build.gradle.kts` 전체를 아래로 만든다:

```kotlin
plugins {
    alias(libs.plugins.parfait.module.feature.impl)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.feature.app.setting.impl"
}

dependencies {
    implementation(projects.feature.app.setting.api)
    implementation(projects.feature.common.terms.api)
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`feature/app/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/app/setting/impl/viewmodel/AppSettingViewModelTest.kt` 신규 생성:

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel() = AppSettingViewModel()

    @Test
    fun clickWithdraw_showsWithdrawDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팝업이 떠 있지 않은 초기 화면
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.isWithdrawDialogVisible)

        // When 서비스 탈퇴하기를 누름
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)

        // Then 탈퇴 확인 팝업이 뜬다
        assertTrue(viewModel.state.value.isWithdrawDialogVisible)
    }

    @Test
    fun confirmWithdraw_hidesWithdrawDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 탈퇴 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)

        // When 팝업의 탈퇴하기를 누름
        viewModel.processIntent(AppSettingIntent.ConfirmWithdraw)

        // Then 팝업이 닫힌다
        assertFalse(viewModel.state.value.isWithdrawDialogVisible)
    }

    @Test
    fun dismissWithdrawDialog_hidesWithdrawDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 탈퇴 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)

        // When 그만두기 또는 바깥 탭으로 닫기를 요청
        viewModel.processIntent(AppSettingIntent.DismissWithdrawDialog)

        // Then 팝업이 닫힌다
        assertFalse(viewModel.state.value.isWithdrawDialogVisible)
    }

    @Test
    fun clickWithdraw_doesNotChangeProfileState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초기 화면의 프로필 값
        val viewModel = viewModel()
        val before = viewModel.state.value

        // When 팝업을 열었다 닫음
        viewModel.processIntent(AppSettingIntent.ClickWithdraw)
        viewModel.processIntent(AppSettingIntent.DismissWithdrawDialog)

        // Then 팝업 외 상태는 그대로다
        val after = viewModel.state.value
        assertEquals(before.nickname, after.nickname)
        assertEquals(before.loginProvider, after.loginProvider)
        assertEquals(before.version, after.version)
        assertFalse(after.isWithdrawDialogVisible)
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인**

Run: `./gradlew :feature:app:setting:impl:testDebugUnitTest`
Expected: 컴파일 실패. `Unresolved reference: isWithdrawDialogVisible`, `Unresolved reference: ConfirmWithdraw`, `Unresolved reference: DismissWithdrawDialog`.

- [ ] **Step 4: 상태 필드 추가**

`AppSettingViewModel.kt`의 `AppSettingState`에 필드 하나를 더한다. KDoc의 기존 `@property` 줄들은 그대로 두고 새 줄만 덧붙인다:

```kotlin
/**
 * @property nickname TODO 프로필 API 연동 전 placeholder 데이터
 * @property loginProvider
 * @property version TODO BuildConfig.VERSION_NAME 주입으로 교체
 * @property isWithdrawDialogVisible 서비스 탈퇴 확인 팝업 노출 여부
 */
data class AppSettingState(
    val nickname: String = "아니야나그런데기니야",
    val loginProvider: String = "Kakao",
    val version: String = "1.0v",
    val isWithdrawDialogVisible: Boolean = false,
) : UiState
```

- [ ] **Step 5: Intent 2종 추가**

`AppSettingIntent`의 마지막 항목(`ClickWithdraw`) 뒤에 두 개를 붙인다. 기존 항목은 손대지 않는다:

```kotlin
sealed interface AppSettingIntent : UiIntent {
    data object ClickBack : AppSettingIntent

    data object ClickAccount : AppSettingIntent

    data object ClickServiceTerms : AppSettingIntent

    data object ClickPrivacyPolicy : AppSettingIntent

    data object ClickLogout : AppSettingIntent

    data object ClickWithdraw : AppSettingIntent

    data object ConfirmWithdraw : AppSettingIntent

    data object DismissWithdrawDialog : AppSettingIntent
}
```

- [ ] **Step 6: processIntent 분기와 핸들러 구현**

`processIntent`의 `when`에 두 분기를 더한다:

```kotlin
    override fun processIntent(intent: AppSettingIntent) {
        when (intent) {
            AppSettingIntent.ClickBack -> handleClickBack()
            AppSettingIntent.ClickAccount -> handleClickAccount()
            AppSettingIntent.ClickServiceTerms -> handleClickServiceTerms()
            AppSettingIntent.ClickPrivacyPolicy -> handleClickPrivacyPolicy()
            AppSettingIntent.ClickLogout -> handleClickLogout()
            AppSettingIntent.ClickWithdraw -> handleClickWithdraw()
            AppSettingIntent.ConfirmWithdraw -> handleConfirmWithdraw()
            AppSettingIntent.DismissWithdrawDialog -> handleDismissWithdrawDialog()
        }
    }
```

기존 `handleClickWithdraw`(로그만 찍던 stub)를 **팝업 열기로 교체**하고, TODO 주석은 확인 핸들러로 옮긴다:

```kotlin
    private fun handleClickWithdraw() {
        updateState { copy(isWithdrawDialogVisible = true) }
    }

    private fun handleConfirmWithdraw() {
        updateState { copy(isWithdrawDialogVisible = false) }
        // TODO 회원 탈퇴 API 연동 전 stub
        viewModelLogger.i { "AppSettingViewModel::handleConfirmWithdraw (stub)" }
    }

    private fun handleDismissWithdrawDialog() {
        updateState { copy(isWithdrawDialogVisible = false) }
    }
```

`handleClickLogout`은 건드리지 않는다 — 로그아웃 확인 팝업은 이번 범위가 아니다.

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew :feature:app:setting:impl:testDebugUnitTest`
Expected: PASS (4 tests)

---

### Task 2: 앱 설정 화면 — 탈퇴 팝업 노출

**Files:**
- Modify: `feature/app/setting/impl/src/main/res/values/strings.xml`
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/screen/AppSettingScreen.kt`
- Modify: `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/route/AppSettingRoute.kt`

**Interfaces:**
- Consumes: Task 1의 `AppSettingState.isWithdrawDialogVisible`, `AppSettingIntent.ConfirmWithdraw`, `AppSettingIntent.DismissWithdrawDialog`. 그리고 `com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup(title, body, iconRes, secondaryText, onSecondaryClick, primaryText, onPrimaryClick, onDismissRequest, modifier, isEnabledButton, iconTint, properties)` — 앞의 8개가 필수 인자이고 나머지는 기본값이 있다.
- Produces: `AppSettingScreen`의 새 파라미터 `onConfirmWithdraw: () -> Unit`, `onDismissWithdrawDialog: () -> Unit`.

- [ ] **Step 1: 문자열 리소스 추가**

`feature/app/setting/impl/src/main/res/values/strings.xml`의 `setting_withdraw` 줄 **바로 아래**에 4줄을 넣는다:

```xml
    <string name="setting_withdraw_dialog_title">파르페에서 탈퇴하시겠어요?</string>
    <string name="setting_withdraw_dialog_body">지금까지 올린 사진은 익명으로 표시되며,\n삭제되지 않아요.</string>
    <string name="setting_withdraw_dialog_confirm">탈퇴하기</string>
    <string name="setting_dialog_cancel">그만두기</string>
```

- [ ] **Step 2: 화면 시그니처에 콜백 2개 추가**

`AppSettingScreen.kt`의 함수 파라미터에서 `onClickWithdraw` 뒤에 두 개를 붙인다:

```kotlin
@Composable
internal fun AppSettingScreen(
    state: AppSettingState,
    onClickBack: () -> Unit,
    onClickAccount: () -> Unit,
    onClickTerms: () -> Unit,
    onClickPrivacy: () -> Unit,
    onClickLogout: () -> Unit,
    onClickWithdraw: () -> Unit,
    onConfirmWithdraw: () -> Unit,
    onDismissWithdrawDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

- [ ] **Step 3: 팝업 호출 추가**

같은 파일에서 `YGScreen { ... }` 블록 안, 기존 `OnBack { onClickBack() }` **바로 위**에 넣는다. `YGScreen`의 content는 `Surface` 한 겹 안이라 형제 컴포저블을 여러 개 두는 것이 이미 이 파일의 패턴이고, `Dialog`는 자체 window로 나가므로 레이아웃에 영향을 주지 않는다:

```kotlin
        if (state.isWithdrawDialogVisible) {
            YGModalPopup(
                title = stringResource(R.string.setting_withdraw_dialog_title),
                body = stringResource(R.string.setting_withdraw_dialog_body),
                iconRes = DesignSystemR.drawable.ic_warning_round,
                secondaryText = stringResource(R.string.setting_withdraw_dialog_confirm),
                onSecondaryClick = onConfirmWithdraw,
                primaryText = stringResource(R.string.setting_dialog_cancel),
                onPrimaryClick = onDismissWithdrawDialog,
                onDismissRequest = onDismissWithdrawDialog,
            )
        }

        OnBack { onClickBack() }
```

import 한 줄을 추가한다 (`import` 블록은 알파벳순이므로 `component.etc.YGListItem` 위, `component.card`가 없으니 `component.etc`보다 앞에 온다 — `modal`은 `m`이라 `etc` 다음, `ygactionitem` 앞):

```kotlin
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
```

`DesignSystemR`(`com.teamyg.parfait.core.designsystem.R as DesignSystemR`)와 `stringResource`는 이 파일에 이미 import돼 있다.

- [ ] **Step 4: 프리뷰 컴파일 복구**

같은 파일 하단 `AppSettingScreenPreview`에 새 인자 2개를 채운다. 팝업은 프리뷰에 렌더되지 않으므로 상태는 기본값(`AppSettingState()`) 그대로 둔다:

```kotlin
@YGPreview
@Composable
private fun AppSettingScreenPreview() = PreviewBox {
    AppSettingScreen(
        state = AppSettingState(),
        onClickBack = {},
        onClickAccount = {},
        onClickTerms = {},
        onClickPrivacy = {},
        onClickLogout = {},
        onClickWithdraw = {},
        onConfirmWithdraw = {},
        onDismissWithdrawDialog = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

- [ ] **Step 5: Route에서 Intent 배선**

`AppSettingRoute.kt`의 `AppSettingScreen(...)` 호출에 두 줄을 더한다:

```kotlin
    AppSettingScreen(
        state = state,
        onClickBack = { viewModel.processIntent(AppSettingIntent.ClickBack) },
        onClickAccount = { viewModel.processIntent(AppSettingIntent.ClickAccount) },
        onClickTerms = { viewModel.processIntent(AppSettingIntent.ClickServiceTerms) },
        onClickPrivacy = { viewModel.processIntent(AppSettingIntent.ClickPrivacyPolicy) },
        onClickLogout = { viewModel.processIntent(AppSettingIntent.ClickLogout) },
        onClickWithdraw = { viewModel.processIntent(AppSettingIntent.ClickWithdraw) },
        onConfirmWithdraw = { viewModel.processIntent(AppSettingIntent.ConfirmWithdraw) },
        onDismissWithdrawDialog = {
            viewModel.processIntent(AppSettingIntent.DismissWithdrawDialog)
        },
        modifier = modifier,
    )
```

- [ ] **Step 6: 컴파일과 린트 확인**

Run: `./gradlew :feature:app:setting:impl:assembleDebug :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 3: 그룹 설정 ViewModel — 나가기·신고 팝업 상태

**Files:**
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModel.kt`
- Test: `feature/groups/setting/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/setting/impl/viewmodel/GroupSettingViewModelTest.kt` (기존 파일에 추가)

**Interfaces:**
- Consumes: 기존 `GroupSettingUiState`(`isEditing`·`nicknameInput`·`myNickname` 등), `GroupSettingIntent.ClickLeaveGroup`·`ClickReportGroup`, `updateState`, `viewModelLogger`. 테스트 헬퍼 `private fun viewModel() = GroupSettingViewModel(CheckNameValidUseCase())`가 기존 테스트 파일에 이미 있다.
- Produces: `enum class GroupSettingDialog { Leave, Report }`, `GroupSettingUiState.visibleDialog: GroupSettingDialog?`(기본 `null`), `GroupSettingIntent.ConfirmLeaveGroup`·`ConfirmReportGroup`·`DismissDialog`. Task 4가 이 이름들을 그대로 쓴다.

- [ ] **Step 1: 실패하는 테스트 작성**

기존 `GroupSettingViewModelTest.kt`의 **마지막 `@Test` 뒤, 클래스 닫는 중괄호 앞**에 아래 6개를 추가한다. import는 파일 상단에 이미 `assertEquals`·`assertFalse`·`assertNull`·`assertTrue`가 다 있으므로 추가할 것이 없다:

```kotlin
    @Test
    fun clickLeaveGroup_showsLeaveDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팝업이 떠 있지 않은 초기 화면
        val viewModel = viewModel()
        assertNull(viewModel.state.value.visibleDialog)

        // When 그룹 나가기를 누름
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)

        // Then 나가기 확인 팝업이 뜬다
        assertEquals(GroupSettingDialog.Leave, viewModel.state.value.visibleDialog)
    }

    @Test
    fun clickReportGroup_showsReportDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 팝업이 떠 있지 않은 초기 화면
        val viewModel = viewModel()

        // When 그룹 신고하기를 누름
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // Then 신고 확인 팝업이 뜬다
        assertEquals(GroupSettingDialog.Report, viewModel.state.value.visibleDialog)
    }

    @Test
    fun confirmLeaveGroup_hidesDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 나가기 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)

        // When 팝업의 나가기를 누름
        viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup)

        // Then 팝업이 닫힌다
        assertNull(viewModel.state.value.visibleDialog)
    }

    @Test
    fun confirmReportGroup_hidesDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 신고 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 팝업의 신고하기를 누름
        viewModel.processIntent(GroupSettingIntent.ConfirmReportGroup)

        // Then 팝업이 닫힌다
        assertNull(viewModel.state.value.visibleDialog)
    }

    @Test
    fun dismissDialog_hidesDialog() = runTest(mainDispatcherRule.dispatcher) {
        // Given 신고 확인 팝업이 떠 있는 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ClickReportGroup)

        // When 그만두기 또는 바깥 탭으로 닫기를 요청
        viewModel.processIntent(GroupSettingIntent.DismissDialog)

        // Then 팝업이 닫힌다
        assertNull(viewModel.state.value.visibleDialog)
    }

    @Test
    fun dialogIntents_whileEditing_keepEditingState() = runTest(mainDispatcherRule.dispatcher) {
        // Given 닉네임을 편집 중인 상태
        val viewModel = viewModel()
        viewModel.processIntent(GroupSettingIntent.ChangeNicknameFocus(isFocused = true))
        viewModel.processIntent(GroupSettingIntent.InputNickname("고치던닉네임"))

        // When 나가기 팝업을 열었다 닫음
        viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup)
        viewModel.processIntent(GroupSettingIntent.DismissDialog)

        // Then 편집 상태와 입력값이 그대로 남는다
        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("고치던닉네임", state.nicknameInput)
        assertNull(state.visibleDialog)
    }
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest`
Expected: 컴파일 실패. `Unresolved reference: GroupSettingDialog`, `Unresolved reference: visibleDialog`, `Unresolved reference: ConfirmLeaveGroup`.

- [ ] **Step 3: 열거형과 상태 필드 추가**

`GroupSettingViewModel.kt`에서 `GroupSettingUiState` 선언 **바로 위**에 열거형을 넣는다:

```kotlin
/** 그룹 설정 화면이 한 번에 하나만 띄울 수 있는 확인 팝업. */
enum class GroupSettingDialog {
    Leave,
    Report,
}
```

이어서 `GroupSettingUiState`에 필드를 더한다. `isConfirmEnabled` 파생 프로퍼티는 그대로 둔다:

```kotlin
data class GroupSettingUiState(
    val groupName: GroupName = GroupName(MOCK_GROUP_NAME),
    val myNickname: GroupNickname = GroupNickname(MOCK_MY_NICKNAME),
    val nicknameInput: String = MOCK_MY_NICKNAME,
    val isEditing: Boolean = false,
    val nicknameError: NameValidResult.Error? = null,
    val members: List<GroupMemberUiModel> = MOCK_MEMBERS,
    val inviteCode: InviteCode = InviteCode(MOCK_INVITE_CODE),
    val remainingCount: Int = MOCK_REMAINING_COUNT,
    val isCodeCopied: Boolean = false,
    val visibleDialog: GroupSettingDialog? = null,
) : UiState {
    val isConfirmEnabled: Boolean
        get() = nicknameError == null && nicknameInput != myNickname.value
}
```

- [ ] **Step 4: Intent 3종 추가**

`GroupSettingIntent`의 마지막 항목(`ClickReportGroup`) 뒤에 붙인다:

```kotlin
    data object ClickLeaveGroup : GroupSettingIntent

    data object ClickReportGroup : GroupSettingIntent

    data object ConfirmLeaveGroup : GroupSettingIntent

    data object ConfirmReportGroup : GroupSettingIntent

    data object DismissDialog : GroupSettingIntent
```

- [ ] **Step 5: processIntent 분기와 핸들러 구현**

`when`에 세 분기를 더한다:

```kotlin
            GroupSettingIntent.ClickLeaveGroup -> handleClickLeaveGroup()
            GroupSettingIntent.ClickReportGroup -> handleClickReportGroup()
            GroupSettingIntent.ConfirmLeaveGroup -> handleConfirmLeaveGroup()
            GroupSettingIntent.ConfirmReportGroup -> handleConfirmReportGroup()
            GroupSettingIntent.DismissDialog -> handleDismissDialog()
```

기존 `handleClickLeaveGroup`·`handleClickReportGroup`(로그만 찍던 stub)을 팝업 열기로 교체하고, 두 함수에 있던 TODO 주석을 각각의 확인 핸들러로 옮긴다:

```kotlin
    private fun handleClickLeaveGroup() {
        updateState { copy(visibleDialog = GroupSettingDialog.Leave) }
    }

    private fun handleClickReportGroup() {
        updateState { copy(visibleDialog = GroupSettingDialog.Report) }
    }

    private fun handleConfirmLeaveGroup() {
        updateState { copy(visibleDialog = null) }
        // TODO: 그룹 나가기 API 연동 (DELETE /api/parfait-groups/{groupId}/members/me)
        viewModelLogger.i { "GroupSettingViewModel::handleConfirmLeaveGroup" }
    }

    private fun handleConfirmReportGroup() {
        updateState { copy(visibleDialog = null) }
        // TODO: 그룹 신고 API 연동 (POST /api/parfait-groups/{groupId}/reports)
        viewModelLogger.i { "GroupSettingViewModel::handleConfirmReportGroup" }
    }

    private fun handleDismissDialog() {
        updateState { copy(visibleDialog = null) }
    }
```

- [ ] **Step 6: 테스트 통과 확인**

Run: `./gradlew :feature:groups:setting:impl:testDebugUnitTest`
Expected: PASS. 기존 테스트를 포함해 전부 통과해야 한다 — 팝업 상태 추가가 닉네임 편집 로직을 건드리지 않았음을 기존 테스트가 확인해 준다.

---

### Task 4: 그룹 설정 화면 — 나가기·신고 팝업 노출

**Files:**
- Modify: `feature/groups/setting/impl/src/main/res/values/strings.xml`
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/screen/GroupSettingScreen.kt`
- Modify: `feature/groups/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/setting/impl/route/GroupSettingRoute.kt`

**Interfaces:**
- Consumes: Task 3의 `GroupSettingDialog`, `GroupSettingUiState.visibleDialog`, `GroupSettingIntent.ConfirmLeaveGroup`·`ConfirmReportGroup`·`DismissDialog`. 그리고 `YGModalPopup`(Task 2와 같은 시그니처).
- Produces: `GroupSettingScreen`의 새 파라미터 `onConfirmLeaveGroup: () -> Unit`, `onConfirmReportGroup: () -> Unit`, `onDismissDialog: () -> Unit`.

- [ ] **Step 1: 문자열 리소스 추가**

`feature/groups/setting/impl/src/main/res/values/strings.xml`의 `group_setting_report` 줄 **바로 아래**에 7줄을 넣는다:

```xml
    <string name="group_setting_leave_dialog_title">그룹에서 나갈까요?</string>
    <string name="group_setting_leave_dialog_body">그룹에서 나가도\n그룹에 올렸던 사진은 지워지지 않아요</string>
    <string name="group_setting_leave_dialog_confirm">나가기</string>
    <string name="group_setting_report_dialog_title">그룹을 신고할까요?</string>
    <string name="group_setting_report_dialog_body">신고 후에는 그룹에서 자동으로 나가지며,\n그룹은 운영 정책에 따라 처리 돼요.</string>
    <string name="group_setting_report_dialog_confirm">신고하기</string>
    <string name="group_setting_dialog_cancel">그만두기</string>
```

"그룹 나가기" 본문에는 마침표가 없고 "그룹 신고" 본문에는 있다. 피그마 그대로다 — 통일하지 마라.

- [ ] **Step 2: 화면 시그니처에 콜백 3개 추가**

`GroupSettingScreen.kt`의 파라미터에서 `onClickReportGroup` 뒤에 세 개를 붙인다:

```kotlin
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
    onConfirmLeaveGroup: () -> Unit,
    onConfirmReportGroup: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

- [ ] **Step 3: 팝업 분기 추가**

같은 파일에서 `YGScreen { ... }` 블록 안, 기존 `OnBack { handleBack() }` **바로 위**에 넣는다. `when`이 열거형을 전수 처리하므로 `else` 분기를 두지 않는다 — 팝업이 늘어나면 컴파일러가 여기를 짚어 준다:

```kotlin
        when (state.visibleDialog) {
            GroupSettingDialog.Leave -> YGModalPopup(
                title = stringResource(R.string.group_setting_leave_dialog_title),
                body = stringResource(R.string.group_setting_leave_dialog_body),
                iconRes = DesignSystemR.drawable.ic_warning_round,
                secondaryText = stringResource(R.string.group_setting_leave_dialog_confirm),
                onSecondaryClick = onConfirmLeaveGroup,
                primaryText = stringResource(R.string.group_setting_dialog_cancel),
                onPrimaryClick = onDismissDialog,
                onDismissRequest = onDismissDialog,
            )

            GroupSettingDialog.Report -> YGModalPopup(
                title = stringResource(R.string.group_setting_report_dialog_title),
                body = stringResource(R.string.group_setting_report_dialog_body),
                iconRes = DesignSystemR.drawable.ic_warning_round,
                secondaryText = stringResource(R.string.group_setting_report_dialog_confirm),
                onSecondaryClick = onConfirmReportGroup,
                primaryText = stringResource(R.string.group_setting_dialog_cancel),
                onPrimaryClick = onDismissDialog,
                onDismissRequest = onDismissDialog,
            )

            null -> Unit
        }

        OnBack { handleBack() }
```

import 두 줄을 추가한다:

```kotlin
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.feature.groups.setting.impl.viewmodel.GroupSettingDialog
```

그리고 `DesignSystemR` 별칭 import를 파일 import 블록 **맨 끝**에 추가한다 (`AppSettingScreen.kt`가 쓰는 것과 같은 형태다):

```kotlin
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
```

- [ ] **Step 4: 프리뷰 컴파일 복구**

같은 파일 하단 `GroupSettingScreenPreview`에 새 인자 3개를 채운다. `GroupSettingPreviewParameterProvider`는 **건드리지 않는다** — 팝업 상태를 넣어도 프리뷰에 안 뜬다:

```kotlin
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
        onConfirmLeaveGroup = {},
        onConfirmReportGroup = {},
        onDismissDialog = {},
        modifier = Modifier.fillMaxSize(),
    )
}
```

- [ ] **Step 5: Route에서 Intent 배선**

`GroupSettingRoute.kt`의 `GroupSettingScreen(...)` 호출에서 `onClickReportGroup` 뒤에 세 줄을 더한다:

```kotlin
        onClickLeaveGroup = { viewModel.processIntent(GroupSettingIntent.ClickLeaveGroup) },
        onClickReportGroup = { viewModel.processIntent(GroupSettingIntent.ClickReportGroup) },
        onConfirmLeaveGroup = { viewModel.processIntent(GroupSettingIntent.ConfirmLeaveGroup) },
        onConfirmReportGroup = {
            viewModel.processIntent(GroupSettingIntent.ConfirmReportGroup)
        },
        onDismissDialog = { viewModel.processIntent(GroupSettingIntent.DismissDialog) },
        modifier = modifier,
```

- [ ] **Step 6: 컴파일과 린트 확인**

Run: `./gradlew :feature:groups:setting:impl:assembleDebug :feature:groups:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

---

### Task 5: 통합 검증

**Files:** 없음(코드 변경 없음). 앞선 태스크에서 회귀가 나오면 그 태스크로 돌아가 고친다.

**Interfaces:**
- Consumes: Task 1~4의 결과 전부.
- Produces: 없음. 검증 결과 보고만.

- [ ] **Step 1: 전체 유닛 테스트**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL. 새로 추가된 `AppSettingViewModelTest` 4건 + `GroupSettingViewModelTest` 확장분 6건이 기존 테스트와 함께 통과한다.

- [ ] **Step 2: 전체 린트**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 앱 빌드**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 육안 확인 체크리스트 보고**

테스트가 잡지 못하는 것들이다. 에뮬레이터/기기에서 확인하고, 확인할 수 없는 환경이면 "미확인"으로 명시해 보고한다. 임의로 통과 처리하지 마라.

- 앱 설정 → "서비스 탈퇴하기" → 팝업 제목 `파르페에서 탈퇴하시겠어요?`, 본문 2줄.
- 그룹 설정 → "그룹 나가기" → `그룹에서 나갈까요?`, "그룹 신고하기" → `그룹을 신고할까요?`.
- **세 팝업 모두 왼쪽이 파괴적 액션(회색 테두리), 오른쪽이 `그만두기`(검정 배경)**. 뒤바뀌어 있으면 즉시 실패로 보고.
- 오른쪽 `그만두기`·팝업 바깥 영역 탭·시스템 뒤로가기 → 팝업이 닫히고 화면이 그대로.
- 왼쪽 파괴적 버튼 → 팝업만 닫히고 아무 일도 일어나지 않음(기능 미구현이 의도).
- 그룹 설정에서 닉네임 편집 중 팝업을 열었다 닫았을 때 입력값·키보드 상태가 유지되는지.
- 본문 줄바꿈이 피그마와 같은 위치에서 끊기는지. 팝업 폭이 플랫폼 기본값이라 다를 수 있는데, **다르면 고치지 말고 보고**하라 — 디자인시스템 쪽 미결 사항이다.

---

## 참고

- 설계 근거·열린 질문: [`parfait/specs/2026-08-09-setting-danger-zone-popups.md`](../../specs/archive/2026-08-09-setting-danger-zone-popups.md)
- 팝업 컴포넌트 스펙: [`parfait/specs/archive/2026-07-15-ygmodalpopup.md`](../../specs/archive/2026-07-15-ygmodalpopup.md)
- 그룹 설정 화면 스펙: [`parfait/specs/2026-08-07-s101-group-side-menu.md`](../../specs/archive/2026-08-07-s101-group-side-menu.md)
- MVI 규약: [ADR-0005](../../../adr/0005-custom-mvi-baseviewmodel.md), [state-management](../../../architecture/state-management.md)
