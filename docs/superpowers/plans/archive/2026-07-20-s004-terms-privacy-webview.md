---
id: s004-terms-privacy-webview
title: S-004 약관/개인정보 화면 분리 + Notion 웹뷰 Implementation Plan
status: done
type: work-order
created: 2026-07-20
updated: 2026-07-22
platforms: android
owner:
related_adr: ADR-0010
related_spec: s004-terms-privacy-webview
related_code: EntryBuilder, TermsRoute, PrivacyPolicyRoute, ServiceTermsScreen, PrivacyPolicyScreen, ServiceTermsViewModel, PrivacyPolicyViewModel, NotionWebView, YGTopBarDetail, NavKeyServiceTerms, NavKeyPrivacyPolicy
archived_reason: 구현 완료 — develop 머지 #161(최종 위치 :feature:common:terms, 모듈 분리 plan과 동일 PR).
tags: [plan, parfait, feature, setting, webview, s004]
---

# S-004 약관/개인정보 화면 분리 + Notion 웹뷰 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** `EntryBuilder`에서 하나로 통일돼 있던 `NavKeyServiceTerms`·`NavKeyPrivacyPolicy`를 `TermsRoute`(서비스 이용약관)·`PrivacyPolicyRoute`(개인정보 처리 방침) 두 갈래로 분리하고, 각 화면을 `YGTopBarDetail`(고정 title) + Notion 공개페이지 WebView(로딩/에러 폴백)로 구현한다.

**Architecture:** 기존 feature 모듈 관례(`route/`·`screen/`·`viewmodel/`·`component/`) + MVI `BaseViewModel<S,I,E>`(`core:ui`). Route가 `hiltViewModel()`로 VM 주입·`state.collectAsStateWithLifecycle()` 수집 후 stateless Screen에 `url`·`onClickBack` 전달. back은 MVI 경유(Screen 콜백→`processIntent(ClickBack)`→VM→`postSideEffect(NavigateBack)`→Route `LaunchedEffect`→`navigator.onBack()`). 본문은 `NotionWebView`(로컬 컴포넌트) — `AndroidView`로 `WebView` 래핑, 로딩/에러는 컴포넌트 로컬 `remember`(WebViewClient 콜백 구동). Notion URL은 각 VM `State`의 기본값(placeholder) — 추후 UseCase 주입 지점.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt(`hiltViewModel`), navigation3(`NavKey`/`entry`), Android `WebView`(프레임워크), 자체 디자인시스템([ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)).

**Spec:** [specs/2026-07-20-s004-terms-privacy-webview.md](../../specs/archive/2026-07-20-s004-terms-privacy-webview.md)

**작업 repo:** TJYG-Android, 신규 브랜치 `feature/s004-terms-privacy-webview`(사용자 확인 후 생성). 로컬 경로는 `wiki/personal-private/project-paths.md`.

## Global Constraints

- **테스트 코드 없음** — 이 프로젝트는 테스트 선례가 없다. 테스트 파일을 만들지 않는다. 각 task 검증 = `:feature:app:setting:impl:compileDebugKotlin` + `:feature:app:setting:impl:ktlintCheck`.
- **커밋은 CLAUDE.md 규율** — `git commit`/`push`/PR 전 사용자 확인. 각 task의 commit 스텝은 사용자 승인 후 실행(자동 커밋 금지).
- **모듈 경로 접두**: impl = `feature/app/setting/impl/src/main/kotlin/com/teamyg/parfait/feature/app/setting/impl/`. NavKey 2종(`NavKeyServiceTerms`·`NavKeyPrivacyPolicy`)은 `:api`에 **이미 존재** — 손대지 않는다.
- **디자인시스템/색**: 값은 `YGTheme.*` 토큰으로, 색은 `YGAtomicColors.*` 직접 참조(#158 이후 public). 탑바는 기존 `YGTopBarDetail(title, onIconClick, modifier)` 재사용.
- **정적 UI 라벨**은 `res/values/strings.xml` + `stringResource(R.string.*)`. R = `com.teamyg.parfait.feature.app.setting.impl.R`. 화면 title은 S-001에서 만든 기존 문자열 재사용(`setting_item_service_terms`·`setting_item_privacy_policy`). 동적 데이터(url)만 State 소유(placeholder).
- **gradle 변경 없음**: `WebView`는 Android 프레임워크(새 라이브러리 0개). `INTERNET` 권한은 `app/src/main/AndroidManifest.xml`에 이미 존재. `build.gradle.kts` 손대지 않는다.
- **Notion URL placeholder**: 실제 공개 URL 미확정. 각 VM `State.url` 기본값에 placeholder 문자열 + TODO 주석. 확정 시 기본값 교체(또는 UseCase 주입).

---

### Task 1: NotionWebView 컴포넌트 + 에러 문자열

> **실행 후 정합(2026-07-20)**: 아래 Step 2 원본 코드블록에서 4건 변경됨(실기기 검증 + 코드리뷰 반영). 최신 설계는 [spec §3](../../specs/archive/2026-07-20-s004-terms-privacy-webview.md) 참조.
> - 컨테이너 `Box(modifier.clipToBounds())` — 로딩 중 네이티브 WebView가 탑바 위로 overdraw 방지(실기기 재현).
> - `onRelease = { it.destroy() }` — `AndroidView`가 `destroy()` 미호출 → 렌더러·DOM 스토리지 누수 방지.
> - `loadUrl`을 factory에서 제거하고 `update = { if (it.tag != url) { it.tag = url; it.loadUrl(url) } }`로 이동 — `update` 매 recomposition 실행 → 무가드 `loadUrl`은 무한 리로드. tag로 url 변경 시만 로드.
> - `LocalInspectionMode` 분기 + `NotionWebViewPreviewPlaceholder` — WebView는 `@Preview` 렌더 불가라 프리뷰용 대체 렌더.

**Files:**
- Create: `feature/app/setting/impl/.../component/NotionWebView.kt`
- Modify: `feature/app/setting/impl/src/main/res/values/strings.xml` (문자열 2개 추가)

**Interfaces:**
- Consumes: `YGTheme`·`YGAtomicColors`, `stringResource(R.string.setting_webview_error_message)`·`R.string.setting_webview_retry`.
- Produces: `NotionWebView(url: String, modifier: Modifier = Modifier)`. Screen(Task 4·5) 사용.

- [ ] **Step 1: strings.xml에 에러 문자열 2개 추가**

기존 `<resources>` 블록 안(마지막 `</resources>` 직전)에 아래 2줄 추가:

```xml
    <string name="setting_webview_error_message">페이지를 불러오지 못했어요</string>
    <string name="setting_webview_retry">다시 시도</string>
```

- [ ] **Step 2: NotionWebView.kt 작성**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.component

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.app.setting.impl.R

@Composable
internal fun NotionWebView(
    url: String,
    modifier: Modifier = Modifier,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Box(modifier) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                            error = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                error = true
                                loading = false
                            }
                        }
                    }
                    webViewRef = this
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        if (error) {
            NotionWebViewError(
                onRetry = {
                    error = false
                    loading = true
                    webViewRef?.reload()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun NotionWebViewError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.setting_webview_error_message),
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray500,
        )
        Text(
            text = stringResource(R.string.setting_webview_retry),
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Cherry.Cherry600,
            modifier = Modifier.clickable(onClick = onRetry),
        )
    }
}
```

> 주의: `url`은 factory에서 1회 로드한다. 현재 `State.url`은 첫 컴포지션에 동기적으로 존재하는 placeholder 상수라 문제없다. 추후 URL이 비동기 주입되면 `AndroidView(update = { if (it.url != url) it.loadUrl(url) })` 가드를 추가한다(지금은 YAGNI).
> 토큰명(`gap.gap4`·`body.b02R`·`body.b01R`·`Gray.Gray500`·`Cherry.Cherry600`)은 실제 심볼과 대조 후 없으면 근접 토큰으로 교체.

- [ ] **Step 3: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): add NotionWebView component with loading/error fallback"
```

---

### Task 2: ServiceTermsViewModel (State/Intent/SideEffect + VM)

**Files:**
- Create: `feature/app/setting/impl/.../viewmodel/ServiceTermsViewModel.kt` — State·Intent·SideEffect·ViewModel 한 파일(`AppSettingViewModel` 선례).

**Interfaces:**
- Consumes: `BaseViewModel`·`UiState`·`UiIntent`·`UiSideEffect`·`viewModelLogger`(`com.teamyg.parfait.core.ui`).
- Produces: `ServiceTermsState(url: String)`, `ServiceTermsIntent.ClickBack`, `ServiceTermsSideEffect.NavigateBack`, `ServiceTermsViewModel`(with `val state: StateFlow<ServiceTermsState>`, `val effect`). Route(Task 6)·Screen(Task 4) 사용.

- [ ] **Step 1: ServiceTermsViewModel 작성 (한 파일)**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property url TODO 실제 Notion 공개 URL 미확정 — placeholder. 추후 UseCase 주입으로 교체.
 */
data class ServiceTermsState(
    val url: String = "https://parfait.notion.site/service-terms-placeholder",
) : UiState

sealed interface ServiceTermsIntent : UiIntent {
    data object ClickBack : ServiceTermsIntent
}

sealed interface ServiceTermsSideEffect : UiSideEffect {
    data object NavigateBack : ServiceTermsSideEffect
}

@HiltViewModel
class ServiceTermsViewModel
@Inject
constructor() : BaseViewModel<ServiceTermsState, ServiceTermsIntent, ServiceTermsSideEffect>(
    initialState = ServiceTermsState(),
) {
    init {
        viewModelLogger.i { "ServiceTermsViewModel::init" }
    }

    override fun processIntent(intent: ServiceTermsIntent) {
        when (intent) {
            ServiceTermsIntent.ClickBack -> handleClickBack()
        }
    }

    private fun handleClickBack() {
        postSideEffect(ServiceTermsSideEffect.NavigateBack)
    }
}
```

- [ ] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): add ServiceTermsViewModel with placeholder url state"
```

---

### Task 3: PrivacyPolicyViewModel (State/Intent/SideEffect + VM)

**Files:**
- Create: `feature/app/setting/impl/.../viewmodel/PrivacyPolicyViewModel.kt` — State·Intent·SideEffect·ViewModel 한 파일.

**Interfaces:**
- Consumes: `BaseViewModel`·`UiState`·`UiIntent`·`UiSideEffect`·`viewModelLogger`(`com.teamyg.parfait.core.ui`).
- Produces: `PrivacyPolicyState(url: String)`, `PrivacyPolicyIntent.ClickBack`, `PrivacyPolicySideEffect.NavigateBack`, `PrivacyPolicyViewModel`. Route(Task 6)·Screen(Task 5) 사용.

- [ ] **Step 1: PrivacyPolicyViewModel 작성 (한 파일)**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property url TODO 실제 Notion 공개 URL 미확정 — placeholder. 추후 UseCase 주입으로 교체.
 */
data class PrivacyPolicyState(
    val url: String = "https://parfait.notion.site/privacy-policy-placeholder",
) : UiState

sealed interface PrivacyPolicyIntent : UiIntent {
    data object ClickBack : PrivacyPolicyIntent
}

sealed interface PrivacyPolicySideEffect : UiSideEffect {
    data object NavigateBack : PrivacyPolicySideEffect
}

@HiltViewModel
class PrivacyPolicyViewModel
@Inject
constructor() : BaseViewModel<PrivacyPolicyState, PrivacyPolicyIntent, PrivacyPolicySideEffect>(
    initialState = PrivacyPolicyState(),
) {
    init {
        viewModelLogger.i { "PrivacyPolicyViewModel::init" }
    }

    override fun processIntent(intent: PrivacyPolicyIntent) {
        when (intent) {
            PrivacyPolicyIntent.ClickBack -> handleClickBack()
        }
    }

    private fun handleClickBack() {
        postSideEffect(PrivacyPolicySideEffect.NavigateBack)
    }
}
```

- [ ] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): add PrivacyPolicyViewModel with placeholder url state"
```

---

### Task 4: ServiceTermsScreen (stateless UI)

**Files:**
- Create: `feature/app/setting/impl/.../screen/ServiceTermsScreen.kt`

**Interfaces:**
- Consumes: `YGTopBarDetail`(디자인시스템), `NotionWebView`(Task 1), `stringResource(R.string.setting_item_service_terms)`(S-001 기존 문자열).
- Produces: `ServiceTermsScreen(url: String, onClickBack: () -> Unit, modifier: Modifier = Modifier)`. Route(Task 6) 사용.

- [ ] **Step 1: ServiceTermsScreen 작성**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.NotionWebView

@Composable
internal fun ServiceTermsScreen(
    url: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        YGTopBarDetail(
            title = stringResource(R.string.setting_item_service_terms),
            onIconClick = onClickBack,
            modifier = Modifier.fillMaxWidth(),
        )
        NotionWebView(
            url = url,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}
```

> Screen 프리뷰 없음 — `WebView`(AndroidView)는 IDE 프리뷰에서 렌더 안 됨. 검증은 compile+ktlint + Task 7 에뮬 실행.

- [ ] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): add ServiceTermsScreen (topbar + notion webview)"
```

---

### Task 5: PrivacyPolicyScreen (stateless UI)

**Files:**
- Create: `feature/app/setting/impl/.../screen/PrivacyPolicyScreen.kt`

**Interfaces:**
- Consumes: `YGTopBarDetail`(디자인시스템), `NotionWebView`(Task 1), `stringResource(R.string.setting_item_privacy_policy)`(S-001 기존 문자열).
- Produces: `PrivacyPolicyScreen(url: String, onClickBack: () -> Unit, modifier: Modifier = Modifier)`. Route(Task 6) 사용.

- [ ] **Step 1: PrivacyPolicyScreen 작성**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.NotionWebView

@Composable
internal fun PrivacyPolicyScreen(
    url: String,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        YGTopBarDetail(
            title = stringResource(R.string.setting_item_privacy_policy),
            onIconClick = onClickBack,
            modifier = Modifier.fillMaxWidth(),
        )
        NotionWebView(
            url = url,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}
```

- [ ] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): add PrivacyPolicyScreen (topbar + notion webview)"
```

---

### Task 6: Route 배선 — ServiceTermsRoute 구현 + PrivacyPolicyRoute 신규

> **실행 후 정합(2026-07-20)**: 이름 대칭을 위해 `TermsRoute` → **`ServiceTermsRoute`**로 리네임(파일 `route/ServiceTermsRoute.kt`, 함수 `ServiceTermsRoute`, EntryBuilder 참조 포함). 아래 Step 1 코드블록의 `TermsRoute`는 `ServiceTermsRoute`로 읽는다.

**Files:**
- Modify: `feature/app/setting/impl/.../route/TermsRoute.kt` (기존 `// TODO impl` stub 교체 → ServiceTerms 배선)
- Create: `feature/app/setting/impl/.../route/PrivacyPolicyRoute.kt`

**Interfaces:**
- Consumes: `ServiceTermsViewModel`·`ServiceTermsIntent`·`ServiceTermsSideEffect`(Task 2), `PrivacyPolicyViewModel`·`PrivacyPolicyIntent`·`PrivacyPolicySideEffect`(Task 3), `ServiceTermsScreen`(Task 4), `PrivacyPolicyScreen`(Task 5), `Navigator`.
- Produces: 배선된 `TermsRoute(navigator, modifier)`, `PrivacyPolicyRoute(navigator, modifier)`. EntryBuilder(Task 7)가 사용.

- [ ] **Step 1: TermsRoute 교체 (stub → ServiceTerms 배선)**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.ServiceTermsScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.ServiceTermsIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.ServiceTermsSideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.ServiceTermsViewModel

@Composable
internal fun TermsRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: ServiceTermsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                ServiceTermsSideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    ServiceTermsScreen(
        url = state.url,
        onClickBack = { viewModel.processIntent(ServiceTermsIntent.ClickBack) },
        modifier = modifier,
    )
}
```

- [ ] **Step 2: PrivacyPolicyRoute 작성 (신규)**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.PrivacyPolicyScreen
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.PrivacyPolicyIntent
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.PrivacyPolicySideEffect
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.PrivacyPolicyViewModel

@Composable
internal fun PrivacyPolicyRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: PrivacyPolicyViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PrivacyPolicySideEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    PrivacyPolicyScreen(
        url = state.url,
        onClickBack = { viewModel.processIntent(PrivacyPolicyIntent.ClickBack) },
        modifier = modifier,
    )
}
```

- [ ] **Step 3: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): wire TermsRoute and add PrivacyPolicyRoute"
```

---

### Task 7: EntryBuilder — PrivacyPolicy entry를 PrivacyPolicyRoute로 분리

**Files:**
- Modify: `feature/app/setting/impl/.../navigation/EntryBuilder.kt`

**Interfaces:**
- Consumes: `TermsRoute`(Task 6, ServiceTerms 배선), `PrivacyPolicyRoute`(Task 6), 기존 `AppSettingRoute`·`AccountInfoRoute`.
- Produces: `entry<NavKeyServiceTerms>` → `TermsRoute`, `entry<NavKeyPrivacyPolicy>` → `PrivacyPolicyRoute` 분리 완료.

- [ ] **Step 1: EntryBuilder 교체 (`PrivacyPolicyRoute` import 추가 + Privacy entry 교체)**

```kotlin
package com.teamyg.parfait.feature.app.setting.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.app.setting.api.NavKeyPrivacyPolicy
import com.teamyg.parfait.feature.app.setting.api.NavKeyServiceTerms
import com.teamyg.parfait.feature.app.setting.impl.route.AccountInfoRoute
import com.teamyg.parfait.feature.app.setting.impl.route.AppSettingRoute
import com.teamyg.parfait.feature.app.setting.impl.route.PrivacyPolicyRoute
import com.teamyg.parfait.feature.app.setting.impl.route.TermsRoute

fun EntryProviderScope<NavKey>.featureAppSettingEntryBuilder(navigator: Navigator) {
    entry<NavKeyAppSetting> {
        Scaffold(containerColor = YGAtomicColors.Gray.White) { innerPadding ->
            AppSettingRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyAccountInfo> {
        Scaffold { innerPadding ->
            AccountInfoRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyServiceTerms> {
        Scaffold { innerPadding ->
            TermsRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyPrivacyPolicy> {
        Scaffold { innerPadding ->
            PrivacyPolicyRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
```

- [ ] **Step 2: 컴파일 + ktlint**

Run: `./gradlew :feature:app:setting:impl:compileDebugKotlin :feature:app:setting:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: 에뮬 실행 수동 확인**

앱 실행 → 설정(S-001) → **서비스 이용약관** 탭 → 탑바 `< 서비스 이용약관` + Notion 웹뷰(로딩 인디케이터 → 콘텐츠, 네트워크 끊으면 에러+다시시도) → back 복귀. **개인정보 처리 방침** 탭 → 탑바 `< 개인정보 처리 방침` + 웹뷰 → back 복귀. 크래시 없음.

- [ ] **Step 4: commit (사용자 확인 후)**

```bash
git add feature/app/setting/impl
git commit -m "feat(setting): split terms/privacy nav entries into separate routes"
```

---

## Self-Review 메모

- **스펙 커버리지**: 라우팅 분리(Task 7)·ServiceTerms VM/Screen/Route(Task 2·4·6)·PrivacyPolicy VM/Screen/Route(Task 3·5·6)·NotionWebView 로딩/에러(Task 1)·title 고정 재사용(Task 4·5)·url placeholder State(Task 2·3)·뒤로가기 MVI(Task 6)·의존성/권한 무변경(Global Constraints) — 전 항목 매핑. ✅
- **placeholder 없음**: 전 파일 실제 코드. URL만 의도된 placeholder(TODO 주석 + Global Constraints 명시). ✅
- **타입 일관**: `ServiceTermsState(url)`↔`TermsRoute(state.url)`↔`ServiceTermsScreen(url,onClickBack)`, `PrivacyPolicyState(url)`↔`PrivacyPolicyRoute`↔`PrivacyPolicyScreen`, `NotionWebView(url, modifier)` 시그니처 Task1↔4↔5 일치. `ServiceTermsSideEffect.NavigateBack`/`PrivacyPolicySideEffect.NavigateBack` Route와 일치. `YGTopBarDetail(title,onIconClick,modifier)` 실제 시그니처와 일치. ✅
- **테스트 스텝 없음**(프로젝트 규율). 검증 = compile+ktlint + Task 7 에뮬 수동. ✅
- **gradle 변경 없음**(WebView 프레임워크, INTERNET 권한 존재, NavKey는 api 기존). ✅
