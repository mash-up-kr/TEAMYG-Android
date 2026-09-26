---
id: intro-term-agree
title: 온보딩 약관 동의 화면 (TermAgree)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-26
related_code:
  - NavKeyTermAgree
  - TermAgreeRoute.kt#TermAgreeRoute
  - TermAgreeScreen.kt#TermAgreeScreen
  - TermAgreeViewModel.kt#TermAgreeViewModel
  - TermAgreeError.kt#TermAgreeError
  - TermAgreeViewModelTest
  - GetPoliciesUseCase.kt#GetPoliciesUseCase
  - SignUpUseCase.kt#SignUpUseCase
  - SignUpException.kt#SignUpException
  - PolicyRepository.kt#PolicyRepository
  - PolicyRepositoryImpl.kt#PolicyRepositoryImpl
  - AuthRepository.kt#signUp
  - EntryBuilder.kt#featureTermAgreeEntryBuilder
  - NavKeyWebView.kt#NavKeyWebView
  - feature/intro/impl/res/values/strings.xml
related_adr: ADR-0005, ADR-0006, ADR-0016, ADR-0017, ADR-0019, ADR-0020
related_spec: s004-terms-privacy-webview, a002-kakao-login-api, mvi-error-infrastructure, ygscaffold-v2-common-loading-error
related_architecture: state-management, navigation-flow, data-layer, design-system
supersedes:
superseded_by:
tags: [spec, parfait, intro, terms, onboarding]
---

# Spec: 온보딩 약관 동의 화면 (TermAgree)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> **사후 기록(post-hoc)**: 타 작업자 구현이 선작성 스펙 없이 develop 머지(#153, 2026-07-22)됨.
> 파르페 완성도 유지를 위해 as-built로 역기록. 코드가 SoT.
>
> **as-built 갱신(2026-07-26, #166)**: 화면 정적 문자열이 하드코딩 → `strings.xml` + `stringResource`로 이동. 문구 자체는 불변.
>
> **as-built 갱신(2026-08-09, #220)**: TODO 3종 중 **다음 화면 네비게이션이 결선**됨 —
> `NavigateToNext` stub → `navigator.clearBackStack()` + `goTo(NavKeyGroupList)`. 동시에 이 화면이
> 로그인의 다음 목적지가 돼(`LoginRoute`가 `NavKeyGroupHome` 대신 `NavKeyTermAgree`로) **도달 불가
> 상태가 해소**됐다. 저장·랜딩 URL TODO는 그대로.
>
> ⚠️ **as-built 갱신(2026-08-15, #242 develop 머지)**: **약관이 서버에서 오고, 동의가 회원가입으로
> 나간다.** 화면 상수 `TERM_CONTENT_LIST`(+`TermContent`)가 통째로 삭제되고 `GET /api/v1/policies`
> 응답(`PolicyVO`)이 그 자리를 채웠으며, "확인"이 `POST /api/v1/auth/signup`을 호출한 뒤 세션까지
> 저장한다. 즉 **신규 가입자가 세션 없이 그룹 목록에 도달하던 과도기가 닫혔다**. 랜딩 URL도 이제
> 서버가 주므로(`PolicyVO.url`) 리터럴 TODO가 사라졌고, 남은 stub은 Route의 `NavigateToUrl` 소비뿐이다.
> 조회 실패 자리와 가입 실패 표현은 임시다(아래 "실패 표현" 절).
>
> ✅ **as-built 갱신(2026-08-18, #296 develop 머지)**: **마지막 stub이던 상세 랜딩이 열렸다.**
> `ClickTermLandingUrl(landingUrl)` → **`ClickTermDetail(policy)`**, `NavigateToUrl(landingUrl)` →
> **`NavigateToPolicyDetail(title, url)`**로 바뀌고 Route의 `/* navigate to url */` 주석이
> `goTo(NavKeyWebView(title, url))`가 됐다. 주소만 넘기던 것을 **제목까지** 넘기는 이유는 여는 화면이
> 상단바에 걸 것을 스스로 조회하지 않기 때문이다 — 목적지는 설정 화면(S-001)과 공유한다
> ([s004 스펙 as-built](2026-07-20-s004-terms-privacy-webview.md)). 화면 콜백도
> `onClickTermLandingUrl(String)` → `onClickTermDetail(PolicyVO)`로 넓어져 `url`을 뽑는 자리가
> ViewModel로 내려갔다. `feature/intro/impl` → `feature/common/terms/api` 의존이 이때 생겼다.
> 조회 실패·가입 실패 표현은 여전히 임시다.
>
> ✅ **as-built 갱신(2026-08-20, #315 develop 머지)**: **실패 표현 둘이 임시에서 결정으로 바뀌었다.**
> 가입 실패는 `TermAgreeError` 2갈래(`NETWORK`·`UNKNOWN`) + `ShowError` 이펙트 + 공통 토스트로 나가고
> (A-002 `LoginError`·S-101 `GroupSettingError`와 같은 형태), 조회 실패는 **공용 에러화면으로 가지
> 않기로 정해져** `TODO(공통 에러화면)` 둘이 근거 문장으로 바뀌었다 — 재시도라는 갈 곳이 화면 안에
> 있어 흘려보내는 실패가 아니기 때문이다. 즉 같은 화면의 두 실패가 **처분이 갈리는 기준**을 보여 준다:
> 사용자가 그 자리에서 할 수 있는 일이 있으면 화면에 남기고, 없으면 토스트로 알린다.
> 컨테이너도 함께 옮겼다 — 엔트리의 `YGScaffold`가 걷히고 Route가 `YGScaffoldV2`를 소유하며
> `isLoading`에 **조회와 가입을 함께** 넘긴다(`state.isLoading || state.isSigningUp`).
> 확인 버튼이 `isSigningUp`을 안 보므로 응답을 기다리는 동안 눌리는 것을 그 오버레이가 막는다.

- **대상 모듈**: `feature/intro/impl`(`termagree/`) + `feature/intro/api`(NavKey) + `domain`(UseCase·Repository·예외)
  + `data`(`PolicyRepositoryImpl`·`AuthRepositoryImpl#signUp`). `feature/groups/list/api` 의존(#220, 다음 목적지).
- **흐름 위치**: 온보딩 intro 플로우의 약관 동의 단계. 진입은 `NavKeyLogin`, 다음은 `NavKeyGroupList`
  ([navigation-flow](../../../architecture/navigation-flow.md) "앱 진입 체인").

## 목표

앱 진입 온보딩에서 서비스 이용 약관·개인정보 처리방침에 동의받는 화면. 필수 약관 전건 체크 시에만
다음 진행을 허용한다.

## 범위

- 포함: 약관 리스트(필수 마킹) 렌더·개별/전체 토글·필수 충족 시 확인 버튼 활성·항목별 상세 랜딩 진입 콜백·뒤로가기.
  **#242 추가**: 진입 시 약관 목록 서버 조회·조회 실패 표시와 재시도·확인 시 회원가입 요청·가입 중 재진입 가드·세션 저장 후 이동.
- 제외(구현 TODO 상태):
  - ~~**동의 결과 저장 로직**~~ — ✅ **해소(#242)**. `ClickNextButton` → `SignUpUseCase` → `POST /auth/signup` → 세션 저장 → `NavigateToNext`.
  - ~~**랜딩 URL 실값**~~ — ✅ **해소(#242 값 + #296 화면)**. 값은 서버가 주고(`PolicyVO.url`), 탭하면 `NavKeyWebView(title, url)`로 공용 웹뷰가 열린다.
  - ~~**조회 실패 화면**~~ — ✅ **해소(#315)**. 공용 에러화면으로 바꾸지 **않기로** 정해졌다(목록 자리 유지, 아래 "실패 표현" 절).
  - ~~**가입 실패 표현**~~ — ✅ **해소(#315)**. `TermAgreeError` 2갈래 + 공통 토스트.
- 결선 완료(#220): **다음 화면 네비게이션** — `NavigateToNext`가 `clearBackStack()` 후 `NavKeyGroupList`로 `goTo`.

## API / 인터페이스

```kotlin
// api 모듈 — 인자 있는 NavKey(#241, 로그인이 신규 회원으로 판정하며 받은 가입 토큰)
@Serializable data class NavKeyTermAgree(val registrationToken: String) : NavKey

// domain (#242 신설)
interface PolicyRepository { suspend fun getPolicies(): Result<List<PolicyVO>> }
class GetPoliciesUseCase @Inject constructor(private val policyRepository: PolicyRepository) {
    suspend operator fun invoke(): Result<List<PolicyVO>>          // 서버 순서 그대로
}
class SignUpUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        registrationToken: RegistrationToken,
        policies: List<PolicyVO>,          // 화면에 노출한 전체
        agreedTermsIds: Set<TermsId>,
    ): Result<AuthSessionVO>                // 성공 시 세션 저장까지 마친다
}
sealed class SignUpException : Exception { class RequiredPolicyNotAgreed(val termsIds: List<TermsId>) }

// impl — MVI (core.ui BaseViewModel<State, Intent, SideEffect>)
data class TermAgreeState(
    val policies: List<PolicyVO> = emptyList(),
    val agreedTermsIds: Set<TermsId> = emptySet(),
    val isLoading: Boolean = true,
    val isLoadFailed: Boolean = false,      // 목록 자리에 남긴다(#315 확정, 에러화면 대체 안 함)
    val isSigningUp: Boolean = false,       // 오버레이만 켠다 — 확인 버튼은 이 값을 안 본다
) : UiState {
    val isAllSelected: Boolean          // 목록이 비지 않고 전 항목 동의
    val isAvailable: Boolean            // 목록이 비지 않고 필수 전건 동의(확인 버튼 활성 조건)
    fun isAgreed(policy: PolicyVO): Boolean
}

sealed interface TermAgreeIntent {
    data class ClickTermAgree(val termsId: TermsId, val newSelected: Boolean)  // 개별 토글(🔁 index → termsId)
    data class ClickTermDetail(val policy: PolicyVO)                     // 상세 진입(🔁 #296, 구 ClickTermLandingUrl(String))
    data class ClickAgreeAllTerm(val newSelected: Boolean)               // 전체 토글
    data object ClickNextButton; data object ClickBackButton
    data object ClickRetryLoad                                           // #242 신설
}
sealed interface TermAgreeSideEffect {
    data class NavigateToPolicyDetail(val title: String, val url: String)  // 🔁 #296, 구 NavigateToUrl(String)
    data object NavigateToBack; data object NavigateToNext
    data class ShowError(val error: TermAgreeError)                        // #315 신설 — 문구가 아니라 사유
}

// #315 신설 — 가입 실패 사유. 문구가 갈리는 지점에서만 나눈다
enum class TermAgreeError { NETWORK, UNKNOWN }
@Composable internal fun TermAgreeError.toStringResource(): String

// ViewModel — 가입 토큰을 NavKey 인자로 받으므로 Assisted 주입(#242)
@HiltViewModel(assistedFactory = TermAgreeViewModel.Factory::class)
class TermAgreeViewModel @AssistedInject constructor(
    @Assisted registrationTokenValue: String,
    private val getPolicies: GetPoliciesUseCase,
    private val signUp: SignUpUseCase,
) : BaseViewModel<…>
```

## 동작 / 상태

- **약관 조회**(#242): `init`에서 `loadPolicies()`. 로딩 플래그는 `launch` **밖**에서 켠다 — 재시도 클릭이
  중복 실행 가드(`launch(key = …)`)에 막혀도 화면은 "조회 중"이어야 하고 실제로도 조회가 돌고 있기 때문이다.
  해제는 `finally`라 예외·취소 어느 경로로 빠져도 스피너가 남지 않는다.
  성공 시 목록을 반영하며 **사라진 약관의 동의 상태는 버린다**(`agreedTermsIds`를 새 목록과 교집합).
- **개별 토글**(`ClickTermAgree`): `agreedTermsIds`에서 해당 `TermsId`를 더하거나 뺀다(🔁 #242 — 이전엔
  index 기반 `List<Boolean>`이었다. 목록이 서버에서 오므로 순서가 아니라 ID가 식별자다).
- **전체 토글**(`ClickAgreeAllTerm`): 전 항목 ID 집합 또는 빈 집합으로 교체.
- **확인**(`ClickNextButton`, #242): `isAvailable`이 아니면 요청하지 않고 로그만 남긴다(화면 가드 재확인).
  통과하면 `SignUpUseCase`에 **노출한 약관 전체 + 동의한 ID 집합**을 넘긴다 — 서버가 미동의 약관도
  `agreed = false`로 함께 받기 때문이다. 성공하면 `NavigateToNext`.
  중복 요청은 `launch(key = KEY_SIGN_UP)` 가드가 막고 `isSigningUp`은 버튼 잠금 표시로만 쓴다(두 곳에서
  막으면 어긋났을 때 원인을 못 찾는다는 코드 주석).
- **도메인 재검증**: `SignUpUseCase`가 필수 미동의를 `SignUpException.RequiredPolicyNotAgreed`로 되돌린다 —
  화면 가드가 뚫려도 잘못된 요청이 나가지 않는다.
- **세션 저장 주체가 UseCase다**: 가입 응답(`AuthSessionVO`)을 `authRepository.saveSession`으로 저장한 뒤에야
  성공을 반환한다(`LoginWithKakaoUseCase`와 같은 이유 — 저장 전에 이동하면 다음 화면 첫 요청이 토큰 없이 나간다).
  저장은 `runSuspendCatching`으로 감싸 실패를 `AppError.Unexpected`로 되돌린다(취소는 재던짐).
- **확인 버튼 활성**: `state.isAvailable` → `YGButton(isEnabled = ...)`. 목록이 비면(로딩·조회 실패) 항상 비활성.

| 요소 | 토큰/기본값(심볼) |
|------|-------------------|
| 상단 | `YGTopBarBack` |
| 제목 | `typography.title.t01B` / `Gray.Gray900` |
| 모두동의 박스 | 배경 `Gray.Gray100` **각짐**(🔁 #353, 2026-08-25 — `shapes.radius.small` + `clip`을 걷었다), 체크 tint 선택 `Gray.Black` / 비선택 `Gray.Gray200` |
| 항목 라벨 | 선택 `Gray.Gray800` / 비선택 `Gray.Gray500`, `body.b02R`, 필수 접두 `R.string.prefix_required`("(필수)") |
| 상세 진입 | `ic_caret_right`(tint `Gray.Gray500`) 탭 → `onClickTermLandingUrl` |
| 확인 버튼 | `YGButton` `YGButtonType.Large` |

> 🔁 **as-built 정정(2026-08-25, PR #353)** — 모두동의 행의 모서리가 각짐이 됐다. `background(color, shape)`의
> `shape` 인자와 뒤따르던 `clip`이 함께 빠져 **둥근 모서리를 만들던 두 자리가 한 번에 사라졌다.**
> 이 화면만의 변경이고 `YGTheme.shapes.radius.small` 자체는 그대로다. 저장소의 다른 각짐 사례
> (`YGButtonType.radius` 삭제·`ProfileCard`의 `RectangleShape`)와 같은 방향이지만, 그 방향을 규약으로
> 적어 둔 자리는 여전히 없다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-049.

### 실패 표현 (#242 → 🔁 #315 확정)

**두 실패의 처분이 갈린 기준은 "사용자가 그 자리에서 할 수 있는 일이 있는가"다.**

- **조회 실패**(화면에 남긴다): `isLoadFailed`가 서면 목록 자리에 "약관을 불러오지 못했어요" +
  "다시 시도"(탭 → `ClickRetryLoad`)를 띄운다. #315가 이 자리를 **공용 에러화면으로 바꾸지 않기로**
  정했다 — 재시도 버튼이 화면 안에 있어 흘려보내면 갈 곳이 사라진다. `TODO(공통 에러화면)` 둘
  (State 필드 KDoc·Screen)이 그 근거 문장으로 바뀌었다.
- **가입 실패**(토스트로 알린다): `handleSignUpFailure`가 갈래 넷을 **`TermAgreeError` 둘로 접어**
  `ShowError`를 쏘고 Route가 공통 토스트로 띄운다. `AppError.Network`만 `NETWORK`이고
  `SignUpException.RequiredPolicyNotAgreed`·`AppError.Server`·그 외(세션 저장 실패 포함)는 전부
  `UNKNOWN`이다 — **사용자가 할 수 있는 일이 "잠시 후 다시"로 같아서**이고, 갈래 구분은 로그가 남긴다
  (`RequiredPolicyNotAgreed`는 화면 가드가 뚫린 것이라 로그 레벨이 결함이다).
  재시도 동선을 따로 주지 않는 것도 같은 이유다 — 화면이 그대로 남아 확인 버튼이 그 자리에 있다.
- 문구는 Route가 `TermAgreeError.entries.associateWith { it.toStringResource() }`로 **컴포지션에서
  미리 뽑아 둔다**(이펙트 수집은 코루틴이라 `stringResource`를 부를 수 없다) —
  [state-management](../../../architecture/state-management.md) "서버 실패 갈래는 feature 로컬 enum".
- **로딩 오버레이는 조회와 가입을 함께 덮는다**(`isLoading || isSigningUp`). 확인 버튼 활성 조건
  `isAvailable`은 `isSigningUp`을 안 보므로 응답을 기다리는 동안에도 눌리고, 중복 요청은
  `launch(key = KEY_SIGN_UP)` 가드가 막는다 — 즉 오버레이가 없으면 사용자에게는 아무 반응이 없다.
  ⚠️ `requestSignUp` KDoc은 이 플래그를 "버튼을 잠그는 표시"라고 적는데 그 버튼이 없다
  → [open-questions](../../../synthesis/open-questions.md) [2026-08-20].


## 표시·제어 규칙

- 개별 라벨 영역 탭 = 토글, caret 탭 = 상세 랜딩(두 클릭 영역 분리).
- 필수 미충족 시 확인 버튼 비활성.
- **목록 항목 key는 `termsId`**(#242) — 서버 순서를 그대로 쓰되 재조회로 순서가 바뀌어도 항목이 뒤섞이지 않는다.
- **정적 UI 라벨은 `feature/intro/impl` `res/values/strings.xml` + `stringResource(R.string.*)`**(제목·"모두 동의하기"·"(필수)"·확인 버튼 + #242의 조회 실패·다시 시도 2건). S-001/S-004 플랜이 세운 관용구와 동일.
  **약관 항목 title은 이제 서버 값**이라 코틀린 리터럴이 사라졌다(#242) — [open-questions](../../../synthesis/open-questions.md) [2026-07-26] ①이 이 화면에서 닫혔다.
- 목록이 비어 있으면(로딩·조회 실패) 상단 여백만 남지 않도록 항목 앞 `Spacer`를 넣지 않는다(#242).

## 파일 구성

- `api/NavKeyTermAgree.kt` — 인자 있는 목적지 키(`registrationToken`).
- `impl/termagree/TermAgreeScreen.kt` — stateless UI(`LazyColumn`) + 조회 실패 자리 + `PreviewParameterProvider` 4상태.
- `impl/termagree/TermAgreeRoute.kt` — Assisted 팩토리로 VM 생성(#242) + state/effect collect, back→`navigator.onBack()`, next→`clearBackStack()`+`goTo(NavKeyGroupList)`(#220), url은 stub. **#315부터 `YGScaffoldV2`를 소유**하고 토스트 정책·실패 문구 맵을 든다.
- `impl/termagree/TermAgreeViewModel.kt` — MVI State/Intent/SideEffect + `processIntent` + 조회·가입 job 키 2종.
- `impl/termagree/TermAgreeError.kt` — 가입 실패 사유 enum + `@Composable toStringResource()`(#315 신설).
- ~~`impl/termagree/model/TermContent.kt`~~ — **#242에서 삭제**(서버 `PolicyVO`가 대체).
- `impl/res/values/strings.xml` — 화면 정적 라벨(제목·모두동의·(필수)·확인, #166 / 조회 실패·다시 시도, #242 / 가입 실패 2종, #315).
- `impl/EntryBuilder.kt#featureTermAgreeEntryBuilder` — 🔁 **#315부터 Route를 부르기만 한다**(`entry<NavKeyTermAgree> { TermAgreeRoute(…, modifier = Modifier.fillMaxSize()) }`). 구 형태는 엔트리가 `YGScaffold`로 감싸던 것이고, 스캐폴드는 이제 Route가 소유한다 → [design-system](../../../architecture/design-system.md) "화면 컨테이너".
- `domain/usecase/policy/GetPoliciesUseCase.kt`·`domain/usecase/auth/SignUpUseCase.kt`·`domain/exception/SignUpException.kt`·
  `domain/repository/policy/PolicyRepository.kt` · `data/repository/policy/PolicyRepositoryImpl.kt`(#242 신설).
  `AuthRepository`에 `signUp`이 추가되고 `RepositoryModule`이 `PolicyRepository` 바인딩을 얻었다.
- 테스트(#242): `TermAgreeViewModelTest`(조회·토글·가입 경로) · `SignUpUseCaseTest` · `GetPoliciesUseCaseTest` ·
  `PolicyRepositoryImplTest` · `AuthRepositoryImplTest`(signUp 경로 추가). `feature/intro/impl`에 `parfait.test.unit` 적용.

## 주의 / 열린 질문

- ~~**동의 저장 미구현**~~ — ✅ **해소(#242)**. 가입 요청 + 세션 저장까지 이 화면이 책임진다.
- ~~**랜딩 URL은 값만 왔다**~~ — ✅ **해소(#296)**. caret 탭이 `NavKeyWebView(title, url)`로
  [s004-terms-privacy-webview](2026-07-20-s004-terms-privacy-webview.md)의 `NotionWebView` 화면을 연다
  (후보로 적어 둔 재사용이 그대로 실현됐다).
  ⚠️ 서버 계약상 이 필드는 URL 전용 컬럼이 아니라 약관 본문 컬럼(`Tos.content`) 재사용이라
  **전문이 내려올 수도 있다** → [api/policy.md](../../../api/policy.md) 미결.
- **빈 목록이어도 200이다** — 서버가 약관 0건을 정상 응답으로 내려주며(계약 문서 명시), 그 경우 이 화면은
  실패 표시 없이 **빈 목록 + 비활성 확인 버튼**으로 멈춘다. 조회 실패와 구분되는 상태이나 화면 표현은 같지 않다.
- ~~**가입 실패가 로그뿐**(#242)~~ — ✅ **해소(#315)**. 공통 토스트로 나간다. 남은 것은 `UNKNOWN`이
  이 화면에서도 "알 수 없는 오류" 문구를 다시 만든다는 것이다(화면 수만큼 복제되는 자리)
  → [open-questions](../../../synthesis/open-questions.md) [2026-08-15] OQ-P-167 ②.
- ~~**조회 실패 자리가 임시**(#242)~~ — ✅ **결정됨(#315)**: 목록 자리에 남긴다. G-001이 같은 성격의
  실패를 전용 화면(`GroupListErrorScreen`)으로 그리는 것은 그대로라 **저장소에 형태가 둘인 것은 변함없고**,
  달라진 것은 그것이 임시가 아니라 선택이라는 점이다(재시도 동선의 유무가 갈랐다).
- "모두 동의하기" 클릭 영역이 `clickable`(스로틀 `clickableYG` 미사용) — 캘린더 셀 등과 동일한 스로틀 규약 이탈 패턴(연타 방어 부재).
