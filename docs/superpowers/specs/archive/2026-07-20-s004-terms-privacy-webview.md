---
id: s004-terms-privacy-webview
title: S-004 약관/개인정보 처리 방침 화면 분리 + Notion 웹뷰 (ServiceTerms / PrivacyPolicy WebView)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-18
related_code:
  - EntryBuilder.kt#featureCommonTermsEntryBuilder
  - NavKeyWebView.kt#NavKeyWebView
  - WebViewRoute.kt#WebViewRoute
  - WebViewScreen.kt#WebViewScreen
  - NotionWebView.kt#NotionWebView
  - YGTopBar.kt#YGTopBarDetail
  - YGScaffoldV2.kt#YGScaffoldV2
  - AppSettingViewModel.kt#AppSettingViewModel
  - TermAgreeViewModel.kt#TermAgreeViewModel
related_adr:
related_spec: app-setting-s001
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, setting, webview, s004]
---

# S-004 약관 / 개인정보 처리 방침 화면 분리 + Notion 웹뷰

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> **구현 완료(2026-07-22, develop 머지 #161)**: 이 설계는 `feature/app/setting/impl`를 전제로 작성됐으나,
> 실제 코드는 곧바로 **`:feature:common:terms:{api,impl}`** 신규 모듈에 안착했다(모듈 분리 설계
> [feature-common-terms-module](2026-07-21-feature-common-terms-module.md), [ADR-0015](../../../adr/0015-feature-common-shared-layer.md)).
> 따라서 아래 본문의 "대상 모듈 = setting/impl"·`EntryBuilder#featureAppSettingEntryBuilder` 등 경로는
> 최종 위치가 `common:terms:impl`(`featureCommonTermsEntryBuilder`)로 대체됨. 화면 구조·MVI·NotionWebView 설계 자체는 코드와 일치.
>
> 🔁 **as-built 정정 (2026-08-18, develop 머지 #296) — 화면 2벌이 1벌이 됐고 ViewModel은 사라졌다.**
> 이 설계의 뼈대(목표 1·2, MVI 세트 2벌, `State`가 url을 소유하고 UseCase 주입을 기다리는 자리)는
> **더 이상 코드가 아니다.** 아래가 develop 실물이다.
>
> - **목적지**: `NavKeyServiceTerms`·`NavKeyPrivacyPolicy` 두 `data object` **삭제** →
>   `NavKeyWebView(title, url)` 하나. 무엇을 여는지는 부르는 쪽이 정한다.
> - **삭제**: `ServiceTermsRoute`·`ServiceTermsScreen`·`ServiceTermsViewModel`·`PrivacyPolicyViewModel`.
>   `PrivacyPolicyScreen` → **`WebViewScreen`**(제목이 `stringResource`가 아니라 파라미터), `Route`는
>   **`WebViewRoute`** 신규. `terms_service_title`·`terms_privacy_title` 문자열도 삭제됐다 —
>   제목이 서버 값이라 화면이 가질 것이 없다.
> - **ViewModel이 없다**: 이 화면의 상태는 목적지에 실려 온 `title`·`url`이 전부고 부를 API도 없다.
>   뒤로가기만 남는데 그것을 상태로 감쌀 이유가 없어 `Route`가 `navigator::onBack`을 그대로 넘긴다.
>   설계 3번("`url`만 `State` 필드 → ViewModel이 소유")이 뒤집힌 자리다.
> - **컨테이너**: 엔트리의 머티리얼 `Scaffold`(V1·V2 어느 쪽도 아니던 규약 이탈) → Route의
>   **`YGScaffoldV2`**. 엔트리는 Route를 부르기만 한다.
> - **호출자 둘**: S-001 앱 설정(`AppSettingViewModel`이 `GetPoliciesUseCase`로 받아 둔 목록에서
>   종류로 골라 `title`·`url`을 싣는다)과 온보딩 약관 동의(`TermAgreeViewModel`, 줄의 화살표).
>   출처 인자는 두지 않았다 — 그려야 할 것이 갈리지 않는다.
> - **`NotionWebView`는 그대로**다(로딩·에러·재시도·`clipToBounds`·`tag` 가드·`onRelease` 전부 유지).
>   이름만 옛 전제를 이고 있다 — 이제 여는 주소가 노션이라는 보장이 없다.
> - 남은 것: 서버가 `url` 자리에 약관 **전문**을 내려줄 수 있어(계약이 URL 전용 컬럼을 두지 않았다,
>   [api/policy.md](../../../api/policy.md)) 그때는 웹뷰가 로드에 실패해 재시도 화면이 뜬다. 목적지가
>   임의 `url`을 받는 범용 화면이 된 것에 출처 검증도 없다 → [open-questions](../../../synthesis/open-questions.md).

- **화면 ID**: S-004 (서비스 이용약관 / 개인정보 처리 방침)
- **대상 모듈**: `feature/app/setting/impl`
- **Figma**: 서비스 이용약관 `220-2220`, 개인정보 처리 방침 `220-2225` (파르페 v0.1, fileKey `QPoxqbNMNktsi8ktua3gMN`)

## 배경 / 문제

`EntryBuilder.kt`에서 `NavKeyServiceTerms`·`NavKeyPrivacyPolicy` 두 NavKey가 **동일한 `TermsRoute`**
하나를 가리킨다. `TermsRoute`는 TODO stub. 두 화면은 탑바 타이틀과 본문 콘텐츠만 다르고 구조는 같다.

두 화면 모두:
- **탑바**: 디자인시스템 기존 컴포넌트 `YGTopBarDetail`(back caret + title) 형태와 일치.
- **본문**: 정적 텍스트가 아니라 **Notion 공개 페이지를 WebView로** 렌더.

## 목표

1. 라우팅을 `TermsRoute` / `PrivacyPolicyRoute` 두 갈래로 분리.
2. 각 화면은 `YGTopBarDetail` + Notion WebView 구성.
3. Notion URL은 ViewModel `State` 기본값(placeholder)으로 두어, 추후 UseCase 주입 지점 확보.
4. WebView 로딩/에러 폴백 처리.

## 비목표 (YAGNI)

- WebView 내부 히스토리 back 네비게이션 (단일 Notion 페이지 → route pop만).
- 새 재사용 core 모듈 (WebView는 `setting/impl` 로컬 컴포넌트, 현재 사용처 1곳).
- BuildConfig / string resource 기반 URL 주입 (지금은 오버킬).
- 테스트 코드 (현재 프로젝트에 테스트 인프라 미적용 → 스코프 제외).

## 설계

### 1. 라우팅 분리 — `navigation/EntryBuilder.kt`

`entry<NavKeyServiceTerms>` → **`ServiceTermsRoute`**(기존 `TermsRoute` 리네임), `entry<NavKeyPrivacyPolicy>` → **`PrivacyPolicyRoute`**.
(현재 둘 다 `TermsRoute` 가리키는 것을 분리.) NavKey 2개는 이미 `api` 모듈에 존재 — 수정 없음.

### 2. 화면 2벌 — MVI 세트 (기존 `AppSetting*` 패턴 복제)

두 화면 구조 동일, `title`·`url`만 다름. `BaseViewModel<State, Intent, SideEffect>`(`core.ui`) 준수.

| 파일 | 내용 |
|---|---|
| `viewmodel/ServiceTermsViewModel.kt` | `ServiceTermsState(url: String = <placeholder>)` + `Intent.ClickBack` + `SideEffect.NavigateBack`. `@HiltViewModel @Inject constructor()`. `url` 기본값 placeholder = **추후 UseCase 주입 지점** |
| `viewmodel/PrivacyPolicyViewModel.kt` | 동일 구조, placeholder url만 다름 |
| `route/ServiceTermsRoute.kt` | 기존 `TermsRoute` 리네임 + 구현: `hiltViewModel()` + `state`/`effect` collect, `NavigateBack` → `navigator.onBack()`, `ServiceTermsScreen` 렌더 |
| `route/PrivacyPolicyRoute.kt` | 신규, 동일 골격 |
| `screen/ServiceTermsScreen.kt` | stateless. `Column { YGTopBarDetail(title="서비스 이용약관", onIconClick=onClickBack); NotionWebView(url, Modifier.weight(1f)) }` |
| `screen/PrivacyPolicyScreen.kt` | 동일, `title="개인정보 처리 방침"` |

- `title`은 화면 고정값 → Screen 내 상수(또는 string resource). `State`에 넣지 않음.
- `url`만 `State` 필드 → ViewModel이 소유(주입 대상).
- VM 2개 분리(통합 VM 아님): route·NavKey별 1 VM 관례 유지.

### 3. Notion WebView 컴포넌트 — `component/NotionWebView.kt` (로컬)

```
NotionWebView(url: String, modifier: Modifier)
  if (LocalInspectionMode.current) { PreviewPlaceholder(url); return }   // 프리뷰 대응
  Box(modifier.clipToBounds()) {
    AndroidView(
      factory = { WebView(it).apply {
          settings.javaScriptEnabled = true      // notion 렌더에 필요
          settings.domStorageEnabled = true
          webViewClient = <상태 콜백 클라이언트>   // loadUrl 은 factory에서 안 함
      }},
      update   = { if (it.tag != url) { it.tag = url; it.loadUrl(url) } },  // url 변경 시만
      onRelease = { it.destroy() },              // 컴포지션 이탈 시 리소스 해제
    )
    if (loading) CircularProgressIndicator(center)
    if (error)   에러 메시지 + 재시도 버튼
  }
```

- **⚠️ 컨테이너 `Box`에 `Modifier.clipToBounds()` 필수.** 없으면 네이티브 WebView가 초기 로드 프레임 동안 자기 layout bounds를 넘어 상위(탑바 영역)까지 overdraw → 로딩 중 탑바가 안 보이다 `onPageFinished` recomposition 때 나타남(실기기 재현·검증). clip으로 weight 영역 밖 draw 차단.
- **⚠️ WebView 리소스 해제**: `AndroidView`는 뷰 제거만 하고 `WebView.destroy()`를 안 부른다. JS/DOM storage 켜진 상태라 렌더러·DOM 스토리지가 누수됨 → `onRelease = { it.destroy() }`로 명시 해제.
- **⚠️ url 로드는 `update`에서 tag 가드로.** `update`는 매 recomposition마다 실행되므로 무조건 `loadUrl` 하면 `onPageStarted`→`loading` 갱신→recompose→재로드 **무한 루프**가 된다. 마지막 로드 url을 `webView.tag`에 저장해 실제 변경 시에만 `loadUrl`. redirect로 `webView.url`이 바뀌어도 tag는 요청 url이라 안전. factory에서는 `loadUrl` 하지 않음(중복 로드 방지).
- **프리뷰**: WebView(`AndroidView`)는 `@Preview`에서 렌더 안 됨 → `LocalInspectionMode.current`면 `NotionWebViewPreviewPlaceholder`(Gray100 배경 + 라벨/url)로 대체, early return. Screen 프리뷰(`@YGPreview`)가 정상 렌더됨.
- **로딩/에러 상태는 컴포넌트 로컬 `remember`.** WebViewClient 콜백:
  `onPageStarted` → loading=true·error=false, `onPageFinished` → loading=false,
  `onReceivedError`(main frame) → error=true·loading=false.
- ViewModel `State`에 로딩/에러를 넣지 않음 → VM은 url 데이터만 소유(주입 의도 유지).
- 재시도 = error=false 후 `reload()`.

### 4. 뒤로가기

탑바 back → `Intent.ClickBack` → `SideEffect.NavigateBack` → `navigator.onBack()`.
WebView 내부 back 미구현(비목표).

### 5. 의존성 / 권한

- 새 라이브러리 **0개** — WebView는 Android 프레임워크.
- `INTERNET` 권한 이미 `app/src/main/AndroidManifest.xml`에 존재.

## 변경 파일 요약

**신규**: `PrivacyPolicyRoute.kt`, `ServiceTermsScreen.kt`, `PrivacyPolicyScreen.kt`,
`ServiceTermsViewModel.kt`, `PrivacyPolicyViewModel.kt`, `component/NotionWebView.kt`
**수정**: `navigation/EntryBuilder.kt`, `route/TermsRoute.kt` → `route/ServiceTermsRoute.kt` 리네임+구현

## 열린 질문

- ~~실제 Notion 공개 URL 2개 — 현재 placeholder. 확정 시 각 ViewModel `State` 기본값 교체(또는 UseCase 주입).~~
  ✅ **해소(2026-08-18, #296)** — 주소는 `GET /api/v1/policies` 응답 값이고 목적지 인자로 실려 온다.
  ViewModel 자체가 사라져 "기본값 교체" 자리도 없다. 다만 그 값이 링크라는 보장이 계약에 없다(위 as-built).
