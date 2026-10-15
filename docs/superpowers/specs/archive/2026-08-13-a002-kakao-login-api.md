---
id: a002-kakao-login-api
title: A-002 카카오 로그인 API 결선 (SDK idToken·nonce → POST /auth/kakao → 신규/기존 분기)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-15
related_code: LoginRoute, LoginScreen, LoginViewModel, KakaoLoginHelper, KakaoLoginResult, KakaoLoginVO, KakaoLoginResponse, AuthRepository, AuthRepositoryImpl, AuthRemoteDataSource, LoginWithKakaoUseCase, NonceGenerator, SecureRandomNonceGenerator, ServerErrorCode, TokenStore, EncryptedTokenStore, NavKeyLogin, NavKeyTermAgree, NavKeyGroupList, TermAgreeRoute, RepositoryModule, runSuspendCatching
related_adr: ADR-0005, ADR-0009, ADR-0017, ADR-0019, ADR-0020
related_spec: mvi-error-infrastructure, a002-login-onboarding, data-api-service-layer, network-envelope-token-storage, intro-term-agree
related_architecture: navigation-flow, data-layer, state-management, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, login, a002, auth]
---

# Spec: A-002 카카오 로그인 API 결선

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> 이 스펙은 [mvi-error-infrastructure](2026-08-13-mvi-error-infrastructure.md) 위에 올라간다.
> 그쪽이 먼저 들어와야 한다.

> ✅ **develop 머지 완료 · 실기기 미검증** (2026-08-15, PR #241 `80895eb1`). 본문은 as-built 다.
> **실기기 9항목이 남아 있다** — 아래 "실기기 검증", [OQ-P-146](../../../synthesis/open-questions.md).

> 📌 **후속(2026-08-16, develop 머지 PR #267 `955c4636` — 이관 커밋 `dbbed12e`)** — 이 스펙이
> "로그 + TODO(에러 UX 미정)"로 남긴 **실패 8케이스가 사용자에게 보이게 됐다.** 화면이
> [`YGScaffoldV2`](2026-08-16-ygscaffold-v2-common-loading-error.md)로 이관되면서 스캐폴드를
> `EntryBuilder`에서 `LoginRoute` 안으로 내렸고, 신설 `LoginError`(NETWORK · INVALID_ID_TOKEN ·
> KAKAO_UNAVAILABLE · UNKNOWN) + `LoginSideEffect.ShowError`로 실패 토스트를 띄운다. 로그 분기는
> 그대로 8갈래이고 사용자 문구만 4갈래로 묶었다(502·503·SDK 실패는 "잠시 후 다시"로 같다).
> 함께 `launch(onError = …)`가 붙어, UseCase가 예외를 던지는 경로도 `Result.failure`와 같이 알린다.

## 목표

A-002 화면과 카카오 SDK는 이미 있고 `:data`의 인증 API 표면도 있으나, **둘을 잇는 것이 아무것도
없다.** 카카오 SDK 토큰은 `LoginState`에만 담기고 서버 호출·토큰 저장·신규/기존 분기가 전부 0건이다
([a002-login-onboarding](2026-08-11-a002-login-onboarding.md) "범위 → 제외").
이 라운드가 Repository·UseCase를 신설해 앱 최초의 실서버 호출을 붙인다.

## 범위

- **포함**
  - 카카오 SDK에서 **`idToken` + `nonce`** 취득(현재는 카카오 access token을 받고 있어 쓸 수 없다).
  - `AuthRepository`·`LoginWithKakaoUseCase` 신설, `POST /api/v1/auth/kakao` 호출.
  - `isNewUser` 분기 — 기존 회원은 세션 저장 후 G-001, 신규는 `registrationToken`을 들고 약관 화면.
  - `KakaoLoginResponse`의 `@SerialName` 정정.
  - 실패 케이스 전수 열거 + 로그 + TODO.
- **제외**(이번 라운드에서 안 함)
  - `POST /auth/signup` 호출과 약관 목록 조회(`GET /policies`) — 다음 라운드. 약관 화면은
    `registrationToken`을 **받아 들고만 있고 쓰지 않는다.**
  - 스플래시 자동 로그인(`reissue`)·로그아웃 — 세션 전 주기는 별도 라운드.
  - 애플 로그인 — Android 미사용 결정(2026-08-11) → [api/auth.md](../../../api/auth.md).
  - 에러 UX(토스트·재시도) — 디자인 미확정, 로그 + TODO만.

## API / 인터페이스

### 카카오 SDK 계층

```kotlin
// domain/model/KakaoLoginResult.kt  (변경)
/** 카카오 **SDK** 로그인 결과. 서버 응답은 KakaoLoginVO — 다른 것이다 */
sealed interface KakaoLoginResult {
    data class Success(val idToken: String, val nonce: String) : KakaoLoginResult
    data class Cancel(val throwable: Throwable?) : KakaoLoginResult
    data class Failure(val throwable: Throwable?) : KakaoLoginResult
}
```

- `Success(token: String)`(카카오 access token)에서 바뀐다. 서버가 요구하는 것은 ID 토큰이다.
- **`nonce`를 `Success`에 함께 싣는 이유** — 서버가 ID 토큰의 `nonce` 클레임과 요청 본문 `nonce`를
  대조해 재생 공격을 검증한다([api/auth.md](../../../api/auth.md)). 두 값이 반드시 같아야 하므로 생성
  지점과 SDK 호출 지점을 붙여 두고 결과에 실어 보낸다. 화면이 따로 만들어 넘기면 두 자리가
  조용히 갈릴 수 있다.
- `KakaoLoginVO`(서버 응답)와 이름이 닮았으므로 **양쪽 KDoc에 상호 참조**를 넣는다 —
  [data-api-service-layer](2026-08-03-data-api-service-layer.md)가 예고했으나 누락된 항목.

```kotlin
// domain/util/NonceGenerator.kt  (신설, 인터페이스)
fun interface NonceGenerator { fun generate(): String }

// data/util/SecureRandomNonceGenerator.kt  (신설)
// SecureRandom 32바이트 → URL-safe Base64(패딩 없음)
```

인터페이스로 분리하는 이유는 테스트에서 nonce를 고정하기 위해서다.

`KakaoLoginHelper` 변경:

- `loginWithKakaoTalk(context, nonce = …)` / `loginWithKakaoAccount(context, nonce = …)` — SDK가
  인자를 지원한다(`v2-user` 현재 버전).
- 콜백에서 `token.idToken`을 읽는다. **nullable이다** — 콘솔에서 OpenID Connect가 꺼져 있으면
  null이다. null이면 `Failure(IllegalStateException)`로 떨어뜨리고 로그에 원인을 적는다.
- 카카오톡 로그인 실패 시 계정 로그인으로 폴백하는 기존 분기는 유지하되 **`Cancel`은 폴백하지
  않는다**(현재도 그렇다 — 의도적 취소를 다시 묻지 않는다).

### 데이터 계층

```kotlin
// domain/repository/auth/AuthRepository.kt  (신설)
interface AuthRepository {
    suspend fun loginWithKakao(idToken: String, nonce: String): Result<KakaoLoginVO>
    suspend fun saveSession(session: AuthSessionVO)
}

// data/repository/auth/AuthRepositoryImpl.kt  (신설)
//   AuthRemoteDataSource + TokenStore 주입
//   DataSource 의 Result(실패=ApiException) → Result(실패=AppError) 변환

// domain/usecase/auth/LoginWithKakaoUseCase.kt  (신설)
//   invoke(idToken, nonce): Result<KakaoLoginVO>
//     기존 회원이면 saveSession. 저장 실패는 runSuspendCatching 으로 잡아
//     Result.failure(AppError.Unexpected) 로 되돌린다
```

⚠️ **`Result.onSuccess` 안에서 저장하면 안 된다.** `onSuccess`는 inline 이라 예외를 잡지 않아
`saveSession`이 던지면 **선언된 실패 채널을 우회해 그대로 throw** 된다 — 호출부가 `Result`만
검사하면 영영 못 본다. 실제로 도달 가능한 경로다(`EncryptedTokenStore.save`는 KeyStore·DataStore
IO 실패를 전파한다). `runSuspendCatching`으로 감싸 실패를 `Result`로 되돌린다.

**세션 저장을 UseCase에 두는 이유** — "기존 회원이면 토큰을 저장한다"는 도메인 규칙이지 화면
사정이 아니다. 화면에 두면 로그인 진입점이 늘 때마다 잊을 수 있고, 저장 전에 내비게이션이 나가면
다음 화면의 첫 API 호출이 토큰 없이 나간다.

**DI** — `RepositoryModule`에 `@Binds` 1줄, `NonceGenerator` 바인딩 1줄. 새 모듈 파일을 만들지
않는다([data-layer](../../../architecture/data-layer.md) "DI 모듈"). `NonceGenerator`도 `RepositoryModule`에
들어간다(`@Binds`는 `interface` 모듈에만 되므로 `object`인 `SingletonInjectModule`은 못 받는다).

**DTO 정정** — `KakaoLoginResponse`의 `@SerialName("newUser")`를 `@SerialName("isNewUser")`로 고친다.
현재 값이면 응답 키를 찾지 못해 `MissingFieldException`이 나고 **로그인이 통째로 실패한다**
→ [api/auth.md](../../../api/auth.md) "판별자 키는 `isNewUser`다", [open-questions](../../../synthesis/open-questions.md).

### 화면 계층

```kotlin
// feature/intro/api/NavKeyTermAgree.kt  (변경)
@Serializable
data class NavKeyTermAgree(val registrationToken: String) : NavKey   // data object → data class

// feature/login/impl  LoginViewModel
data class LoginState(val isLoading: Boolean = false) : UiState      // token: String? 삭제

sealed interface LoginIntent : UiIntent {
    data object LoginWithKakao : LoginIntent
    data class LoginWithKakaoSuccess(val idToken: String, val nonce: String) : LoginIntent
    data class LoginWithKakaoFailure(val throwable: Throwable?) : LoginIntent
    data object LoginWithKakaoCancel : LoginIntent
}

sealed interface LoginSideEffect : UiSideEffect {
    data object RequestLoginWithKakao : LoginSideEffect
    data class NavigateToTermAgree(val registrationToken: String) : LoginSideEffect
    data object NavigateToGroupList : LoginSideEffect
}
```

- `LoginState.token` 삭제 — 카카오 토큰을 화면 상태에 들고 있을 이유가 사라졌다(서버가 원하는
  것은 idToken이고 그것도 즉시 소비된다).
- `NavigateToNext`는 목적지가 하나였을 때의 이름이라 분기가 생긴 지금 오도한다. 두 목적지로
  가른다. 기존 선언이 `data object`가 아니라 `class`여서 호출마다 새 인스턴스를 만들던 것도
  여기서 정리된다.
- `featureTermAgreeEntryBuilder`가 `entry<NavKeyTermAgree> { navKey -> … }`로 값을 꺼내
  `TermAgreeRoute`에 넘긴다. **이번 라운드에서 약관 화면은 값을 받기만 한다** —
  `// TODO(signup 라운드): 이 토큰으로 POST /auth/signup`. ViewModel 주입(assisted `Factory`,
  `GroupCreateViewModel`과 동일 패턴)은 다음 라운드 몫.
- `feature/login/impl`이 `feature/groups/list/api`를 의존에 추가한다.

## 동작 / 상태

### 분기

| Intent | 처리 |
|---|---|
| `LoginWithKakao` | `state.isLoading`이면 **무시**. 아니면 `isLoading = true` + `RequestLoginWithKakao` |
| `LoginWithKakaoSuccess` | `launch(key = 카카오로그인)`으로 `LoginWithKakaoUseCase` 호출 |
| `LoginWithKakaoCancel` | `isLoading = false`, `d` 로그만(의도적 행위 — 에러 아님) |
| `LoginWithKakaoFailure` | `isLoading = false`, `e` 로그 + TODO |

UseCase 성공 시:

- `KakaoLoginVO.ExistingMember` — UseCase가 이미 세션을 저장했다 → `NavigateToGroupList` →
  Route가 `clearBackStack()` 후 `goTo(NavKeyGroupList)`.
- `KakaoLoginVO.NewUser` — `NavigateToTermAgree(registrationToken)` → Route가 `goTo`.
  **백스택을 지우지 않는다** — 약관에서 뒤로가기가 로그인으로 돌아와야 한다.

어느 경로든 종료 시 `isLoading = false`로 되돌린다. **`finally`에 둔다** — 마지막 줄에 두면
블록이 던지거나 취소될 때 도달하지 못하고 버튼이 영구 비활성으로 남는다. 백스택 크기가 1이라
시스템 뒤로가기도 no-op이라서 앱을 죽이는 것 외에 탈출구가 없다.

SDK 단계는 `launch` 블록 밖(Route의 `LaunchedEffect`)에서 도므로 `finally`가 닫아주지 못한다.
회전 등 설정 변경으로 컴포지션이 죽으면 continuation이 취소되고 SDK 콜백이 돌아올 곳이 없다 —
Route가 `catch (CancellationException)`로 취소 인텐트를 보낸 뒤 **재던진다.**

**중복 방어가 두 겹인 이유** — `launch(key)`는 서버 호출을 막지만 카카오 SDK 다이얼로그는 그
앞에서 뜬다. `LoginWithKakao` 진입에 `isLoading` 가드를 한 번 더 두지 않으면 연타에 로그인 창이
두 번 뜬다.

### 실패 케이스 전수 (전부 로그 + TODO)

서버 에러 코드 문자열은 화면이 리터럴로 들지 않는다. `:domain`의 **`ServerErrorCode.Auth`**
(`domain/model/error/ServerErrorCode.kt`)가 소유한다 — 코드는 화면 사정이 아니라 서버 계약이고
`signup`·`reissue`·`logout` 라운드가 같은 값을 다시 쓴다. 도메인별로 감싸는 이유는 코드 문자열이
도메인 간 유일하지 않기 때문이다(`MEMBER_NOT_FOUND`가 인증에서 401, 그룹·이미지·회원에서 404).
**앱이 실제로 분기에 쓰는 코드만 둔다** — 서버 enum 14종을 미리 옮겨 적으면 안 쓰는 상수가
계약 변경 때 방치돼 거짓말이 된다.

> 📌 **as-built(머지 직전 확장)** — 같은 PR의 마지막 커밋이 `ParfaitGroup` 8종(서버
> `ParfaitGroupApiErrorCode` 대응)과 `Common` 1종(`INVALID_REQUEST`)을 더했다. 그룹 Repository
> 경계를 먼저 심는 커밋([data-layer](../../../architecture/data-layer.md) "Repository 경계")이
> 함께 넣은 것이고 **이 스펙 범위 밖**이다. `Auth` 3종만 실제 분기에 쓰이고 나머지 9종은
> 아직 소비처가 없다 → [open-questions](../../../synthesis/open-questions.md).

| 케이스 | 판정 타입 | 로그 |
|---|---|---|
| 사용자 취소 | `KakaoLoginResult.Cancel` | `d` (에러 아님) |
| SDK 실패 | `KakaoLoginResult.Failure` | `e` + TODO |
| `idToken`이 null | `Failure(IllegalStateException)` | `e` + TODO — **콘솔 OIDC 설정 확인** |
| 401 `INVALID_ID_TOKEN` | `AppError.Server` | `e` + TODO |
| 502 `KAKAO_JWKS_FETCH_FAILED` | `AppError.Server` | `e` + TODO |
| 503 `KAKAO_SERVER_UNAVAILABLE` | `AppError.Server` | `e` + TODO |
| 네트워크 단절 | `AppError.Network` | `e` + TODO (재시도 안내 자리) |
| 매퍼 `requireNotNull` 실패 등 | `AppError.Unexpected` | `e` + TODO |

에러 코드 근거는 [api/auth.md](../../../api/auth.md) "POST /api/v1/auth/kakao".
`when`으로 전부 열거해 둔다 — UX가 정해지면 로그 자리를 문구로 바꾸면 되고 분기를 다시 발굴할
필요가 없다.

## 파일 구성

```
domain/
  model/KakaoLoginResult.kt            변경 — Success(idToken, nonce) + KDoc 상호 참조
  model/auth/KakaoLoginVO.kt           변경 — KDoc 상호 참조만
  util/NonceGenerator.kt               신설
  repository/auth/AuthRepository.kt    신설
  usecase/auth/LoginWithKakaoUseCase.kt 신설
  model/error/ServerErrorCode.kt       신설 — 서버 에러 코드 문자열(도메인별 중첩 object)
data/
  util/SecureRandomNonceGenerator.kt   신설
  repository/auth/AuthRepositoryImpl.kt 신설
  service/model/response/auth/KakaoLoginResponse.kt  변경 — @SerialName 정정
  di/RepositoryModule.kt               @Binds 2줄(AuthRepository·NonceGenerator)
feature/login/impl/
  util/KakaoLoginHelper.kt             변경 — nonce 전달·idToken 취득
  viewmodel/LoginViewModel.kt          변경 — 분기·로딩·실패 8케이스 로그
  route/LoginRoute.kt                  변경 — 두 목적지·SDK 단계 취소 처리
  build.gradle.kts                     groups.list.api 의존 추가
feature/intro/
  api/NavKeyTermAgree.kt               변경 — data class(registrationToken)
  impl/EntryBuilder.kt                 변경 — navKey 인자 전달
  impl/termagree/TermAgreeRoute.kt     변경 — 파라미터 수신(TODO)
```

## 테스트

- `LoginViewModel` — 신규 유저면 `NavigateToTermAgree(토큰)`, 기존 회원이면 `NavigateToGroupList`,
  각 `AppError`별 `isLoading` 복귀, 로딩 중 `LoginWithKakao` 재진입 시 `RequestLoginWithKakao`
  **미발행**.
- `LoginWithKakaoUseCase` — `ExistingMember`면 `saveSession` 호출, `NewUser`면 **미호출**.
- `AuthRepositoryImpl` — `ApiException` → `AppError` 매핑, `CancellationException` 재던짐.
- `SecureRandomNonceGenerator` — 길이·문자셋·연속 호출 비중복.
- 매퍼 단독 테스트는 만들지 않는다 — 판단이 든 변환은 DataSource 테스트 케이스로 확인한다.

## 실기기 검증

이 라운드가 앱 최초의 실서버 호출이다. 오래 이월된 미결이 여기서 닫힌다:

- **`isNewUser` 실제 응답 키 확인** — 지금 근거는 서버 코드·컨트롤러 테스트·팀 명세 3축뿐이고
  OpenAPI 스키마만 반대다. 실물 응답으로 확정한다 → [api/auth.md](../../../api/auth.md).
- **토큰 저장 왕복** — 저장 → 앱 종료 → 재시작 → 읽기, DataStore 파일에 평문 없음 확인
  → [ADR-0019](../../../adr/0019-encrypted-token-storage.md).
- **`TokenStoreTokenProvider`의 `runBlocking` 실지연 관측** — 지금까지 이 경로가 런타임에 돈 적이 없다.
- **`:data` 표면의 미검증 결함** — 컴파일·lint·Hilt 어디에도 안 걸리는 종류의 오류(`@SerialName`
  키 오타 등)가 여기서 처음 드러난다 → [open-questions](../../../synthesis/open-questions.md).
- **카카오 로그인 창이 떠 있는 동안 화면 회전** — 로딩이 풀리는가(위 취소 처리 확인).
  유닛 테스트로 못 덮는다(컴포지션 파괴가 필요해 계측 테스트 영역).

⚠️ **디버그 빌드는 `HttpLoggingInterceptor.Level.BODY`라 logcat에 ID 토큰·nonce·발급 토큰이
전부 찍힌다.** 그 로그를 PR·이슈에 붙이지 않는다.

## 주의 / 열린 질문

- ⚠️ **개발 서버 평문 HTTP** — `app/src/main/AndroidManifest.xml`에 `usesCleartextTraffic="true"`를
  넣어 뚫었다. **main 매니페스트라 릴리즈 빌드까지 따라간다.** 서버가 HTTPS로 올라가면 지우고,
  그 전에 릴리즈가 나가야 하면 debug 한정으로 좁힌다 → [open-questions](../../../synthesis/open-questions.md).
- **콘솔 OpenID Connect 활성화가 선행 조건**이다. 꺼져 있으면 `idToken`이 null이라 로그인이 성립하지
  않는다. 활성 상태는 확인됐다(2026-08-13).
- **약관 화면이 토큰을 받아 쓰지 않는 과도기**가 이 라운드의 산출물이다. signup 라운드까지
  `registrationToken`은 전달만 되고 소비되지 않는다. 그래서 **신규 가입자는 약관에서 "다음"을
  누르면 세션 없이 그룹 목록에 도달하고 첫 인증 호출이 401이 난다** — 의도된 과도기이지 회귀가
  아니다.
- `TERM_CONTENT_LIST` 하드코딩과 `GET /policies` 결선은 이 스펙 범위 밖으로 남는다
  → [open-questions](../../../synthesis/open-questions.md).
