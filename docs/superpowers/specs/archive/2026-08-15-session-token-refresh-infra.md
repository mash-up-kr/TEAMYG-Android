---
id: session-token-refresh-infra
title: 세션 인프라 — 401 자동 재발급 · 강제 로그아웃 이벤트 · 로그아웃 결선 (Token Refresh Infra)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-16
related_code: TokenAuthenticator, SessionEventBus, SessionEventSource, SessionEvent, UnauthenticatedClient, SessionModule, LogoutUseCase, AuthInterceptor, TokenStore, EncryptedTokenStore, AuthRemoteDataSource, AuthRepositoryImpl, AuthService#postAuthReissue, AuthService#postAuthLogout, NetworkModule, ServiceModule, Navigator#replaceAll, MainRoute, AppSettingViewModel, YGActionItem
related_adr: ADR-0021, ADR-0019, ADR-0017, ADR-0020
related_spec: network-envelope-token-storage, a002-kakao-login-api
related_architecture: data-layer
supersedes:
superseded_by:
tags: [spec, parfait, auth, network, session]
---

# Spec: 세션 인프라 — 401 자동 재발급 · 강제 로그아웃 · 로그아웃 결선

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ✅ **develop 머지(2026-08-15, PR #260 `9cfbd117`)** — 설계와 코드가 대체로 일치한다.
> **as-built 차이 3건**을 아래 본문에 반영했다.
> ① 재발급 전용 표면의 한정자 이름이 **`@AuthClient` → `@UnauthenticatedClient`**다(`data/model/qualifier/`).
> 이름이 사용처("재발급용")가 아니라 표면의 성질("자격증명을 붙이지 않는다")을 가리키도록 바꾼 것이고,
> `OkHttpClient`·`Retrofit`·`AuthService` 세 provider가 모두 이 한정자를 단다 — 그중 `AuthService`는
> `NetworkModule`이 아니라 **`ServiceModule`**이 만든다(서비스 생성은 그 모듈 소관이라는 기존 규약).
> ② `SessionEventBus.postForcedLogout()`이 `trySend` 실패를 **로그로 남긴다** — `CONFLATED`인 지금은
> 실패할 수 없지만 채널 종류가 바뀌면 조용한 유실이 되므로 그 회귀가 드러나게 남겼다.
> ③ `SessionEventSource` ↔ `SessionEventBus` 바인딩은 신설 **`SessionModule`**이 `@Provides`로 준다.
> 📌 **이름과 자리가 바뀌었다(2026-09-05, PR #450)** — 인터페이스 `SessionEventSource` → **`SessionEventBus`**
> (`:domain` `event/`), 구현 `SessionEventBus` → **`SessionEventBusImpl`**(`:data` `event/`)이다. 이 문서
> 본문은 **당시 이름을 그대로 둔다**(아카이브 규율). 결정·배선은 변하지 않았고 바뀐 것은 이름과 패키지뿐이다
> → [ADR-0021](../../../adr/0021-token-refresh-forced-logout.md) 정정 · [data-layer](../../../architecture/data-layer.md) 「이벤트 버스 개명」.
> **서술 오류 1건 정정**: 아래 "범위 → 제외"가 회원 탈퇴를 "서버에 엔드포인트 계약이 없다"고 적었으나
> 2026-08-15 서버 delta로 `DELETE /api/v1/users/me`가 생겼고 앱 표면(`MemberService`·DataSource)도
> PR #250으로 들어와 있다([api/member.md](../../../api/member.md)). 제외 사유는 계약 부재가 아니라
> **이 라운드 범위 밖**이다 — `AppSettingViewModel.handleConfirmWithdraw`는 여전히 stub이다.

## 목표

access token이 만료되면 화면이 알아채지 못한 채 재발급되고 원요청이 이어지게 한다. 재발급까지
실패하면 앱 전체가 한 번에 로그인 화면으로 빠진다. 사용자가 직접 누르는 로그아웃도 같은
정리 경로를 쓴다.

지금은 `reissue`가 `AuthRemoteDataSource`까지만 있고 호출부가 없다. `AuthInterceptor`는 헤더를
붙이기만 하므로 access token이 만료되는 순간 모든 인증 API가 401로 깨진다. 로그아웃은
`AppSettingViewModel`에 stub으로 남아 있다.

## 범위

- **포함**
  - `TokenAuthenticator`(OkHttp `Authenticator`) — 401 가로채 재발급 후 원요청 재시도
  - 동시 401 직렬화 · 선점 확인 · 재시도 루프 가드
  - 강제 로그아웃 이벤트(`SessionEvent.ForcedLogout`)와 앱 루트 단일 구독
  - 로그아웃 API 결선(`AuthRepository.logout()` → `LogoutUseCase` → `AppSettingViewModel`)
- **제외**
  - **userInfo SSoT**(`GET /api/v1/users/me` → DataStore) — 후속 스펙. 이 스펙은 토큰만 정리한다
  - **자동로그인 라우팅**(스플래시 분기) — 후속 스펙. 이 인프라 위에 얹는다
  - **회원 탈퇴** — 이 라운드 범위 밖(계약·앱 표면은 이미 있다 → 위 as-built 블록)
  - 401 외 상태코드의 자동 재시도

## API / 인터페이스

```kotlin
// data/network/TokenAuthenticator.kt
class TokenAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @UnauthenticatedClient private val authService: AuthService,
    private val apiCaller: ApiCaller,
    private val sessionEventBus: SessionEventBus,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request?
}
```

**재발급은 전용 `OkHttpClient`로 나간다**(`@UnauthenticatedClient` 한정자). 그 클라이언트는 자기
`Dispatcher`를 갖고, 인증기도 `AuthInterceptor`도 달지 않는다. `AuthInterceptor`를 빼는 이유는
재발급이 자격증명을 헤더가 아니라 **본문**(`ReissueRequest.refreshToken`)으로 보내기 때문이다.

이유가 둘이고 둘 다 데드락이다.

- **디스패처 고갈** — `authenticate()`는 그 호출이 아직 `Dispatcher` 슬롯을 점유한 채
  `runBlocking`으로 블록된 상태에서 실행된다. 재발급이 같은 클라이언트를 타면 같은 디스패처·같은
  호스트로 enqueue되는데, OkHttp 기본 `maxRequestsPerHost`는 5다. **같은 호스트 요청 5건이 동시에
  401을 맞으면** 전부 슬롯을 점유한 채 블록되고 재발급은 큐에서 영원히 promote되지 않는다.
  `callTimeout`이 없어 풀리지도 않는다 — 앱의 모든 네트워크가 프로세스 재시작까지 정지한다.
  ⚠️ `okHttpClient.newBuilder()`로 파생하면 **부모의 `Dispatcher` 인스턴스를 물려받아** 이
  교착이 살아남는다. 반드시 독립 생성한다.
- **재진입** — 재발급 요청 자신이 401을 받으면 인증기가 자기 자신을 다시 타고 `Mutex`에 걸린다.
  아래 0단계 `@NoAuth` 가드가 막지만, 클라이언트 분리로 구조적으로도 불가능해진다.

부수 효과로 `Retrofit`↔`OkHttpClient`↔`Authenticator` Dagger 순환이 사라져 `Provider` 지연
주입이 필요 없다.

```kotlin
// domain/model/session/SessionEvent.kt
sealed interface SessionEvent {
    /** refresh token 이 서버에 거절당해 세션을 더 유지할 수 없다 */
    data object ForcedLogout : SessionEvent
}

// domain/repository/session/SessionEventSource.kt
interface SessionEventSource {
    val events: Flow<SessionEvent>
}
```

feature 모듈은 `:data`를 보지 않으므로(ADR-0001) 인터페이스는 `:domain`에 두고 구현체
`SessionEventBus`(`data/session`, `@Singleton`)가 발행과 구독을 겸한다.

**`Channel(CONFLATED)` + `receiveAsFlow()`를 쓴다.** `SharedFlow`가 아닌 이유는 ADR-0020이
이펙트에서 정리한 것과 같다 — 구독자가 없는 순간 발행해도 버퍼에 남아야 하고, 이미 소비한
이벤트가 재구독으로 다시 오면 안 된다. 소비자가 앱 루트 하나뿐이라는 것도 `Channel`의 단일
소비자 성질과 맞는다. `CONFLATED`인 이유는 401이 여러 건 터져 `ForcedLogout`이 연달아
발행돼도 이동은 한 번이어야 하기 때문이다.

```kotlin
// domain/repository/auth/AuthRepository.kt — 추가
suspend fun logout(): Result<Unit>

// domain/usecase/auth/LogoutUseCase.kt
class LogoutUseCase @Inject constructor(private val authRepository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
```

## 동작 / 상태

### 401 재발급 순서

`authenticate()`는 아래 순서로 판단한다. 각 단계는 앞 단계가 통과했을 때만 실행된다.

0. **`@NoAuth` 가드** — 요청이 인증 불필요 엔드포인트로 나간 것이면 즉시 `null`. `AuthInterceptor`와
   같은 Retrofit `Invocation` 태그 판정을 쓴다. 클라이언트 분리(위)로 재진입은 이미 구조적으로
   막혔지만, 이 가드는 그 위의 방어층으로 남긴다
1. **루프 가드** — `response.priorResponse` 체인에서 **401인 것만** 세어 2 이상이면 `null`. 서버가
   새 토큰에도 401을 주는 경우 무한 재시도를 끊는다.
   ⚠️ 체인 전체를 세면 안 된다 — 리다이렉트 같은 follow-up이 섞여 있어, LB가 301을 한 번만
   끼워도 첫 401이 이미 2회차로 보이고 **재발급을 한 번도 시도하지 못한 채** 모든 인증 API가
   영구 실패한다
2. **`Mutex` 획득** — 재발급 호출을 직렬화한다. 401이 동시에 여러 건 나도 재발급은 하나씩
3. **선점 확인** — 실패한 요청이 들고 갔던 `Authorization` 헤더 값과 지금 `TokenStore`의 access
   token을 비교한다. 다르면 앞선 요청이 이미 갱신을 끝낸 것이므로 **재발급을 건너뛰고** 새
   토큰으로 요청만 다시 만든다
4. **재발급** — `AuthService.postAuthReissue(refreshToken)`. 성공하면 `TokenStore.save()` 후 새
   access token을 단 요청을 반환한다

`Authenticator`는 동기 API라 `runBlocking`을 쓴다. 기존 `TokenStoreTokenProvider`가 이미 같은
방식이라 새로 들이는 관례가 아니다.

선점 확인이 없으면 `Mutex`만으로는 부족하다 — 직렬화될 뿐 대기하던 요청들이 차례로 각자
재발급을 쏜다.

### 실패 갈래

| 상황 | 판단 | 토큰 | 이벤트 | 반환 |
|---|---|---|---|---|
| 재발급 **401** (status 만으로 충분) | refresh token이 죽었다 | `clear()` | `ForcedLogout` | `null` |
| 재발급 실패 + 본문 `code`가 거절 코드 (`INVALID_TOKEN`·`EXPIRED_TOKEN`·`FORBIDDEN_REFRESH_TOKEN`) | 같음 | `clear()` | `ForcedLogout` | `null` |
| 재발급 **403**이나 본문 `code`가 없음 | 프록시·WAF가 낸 것이다 | **유지** | 없음 | `null` |
| 재발급 네트워크 실패·타임아웃 | 세션은 살아있을 수 있다 | **유지** | 없음 | `null` |
| 재발급 5xx | 서버 장애 | **유지** | 없음 | `null` |
| refresh token 부재 | 로그인한 적 없다 | 그대로(지울 것 없음) | 없음 | `null` |

**status 코드만으로 세션을 끝내는 것은 401뿐이다.** 서버 계약상 재발급이 403을 내지 않는다
(403 `FORBIDDEN_REFRESH_TOKEN`은 logout 엔드포인트 소속이다). 반면 WAF·사내 프록시·CDN은 HTML
본문과 함께 403을 낸다 — 그것을 세션 종료로 보면 **로그인 상태를 유지했어야 할 사용자가 조용히
로그아웃된다.** 그래서 403은 본문 `code`가 거절 코드일 때만 인정한다.

네트워크 실패를 강제 로그아웃으로 보지 않는 이유 — 연결이 끊긴 것과 자격증명이 죽은 것은 다른
사건이다. 지하철·엘리베이터에서 앱을 켠 것만으로 재로그인을 요구하게 된다. `null`을 반환하면
원요청이 401로 화면에 도달하고, 화면은 기존 `AppError` 경로로 실패를 표시한다(ADR-0020).

refresh token 부재를 따로 가르는 이유 — 로그인 전 화면이 인증 API를 때리면 이 경로로 들어온다.
여기서 `ForcedLogout`을 쏘면 로그인 화면에서 로그인 화면으로 튕긴다.

### 강제 로그아웃 소비

`SessionEventSource.events`는 **앱 루트 한 곳에서만** 수집한다(NavHost 상위). 화면마다 구독하면
같은 이벤트로 여러 번 이동한다.

```
ForcedLogout 수신 → navigator.replaceAll(NavKeyLogin)
```

`replaceAll`은 이 라운드에서 `Navigator`에 신설했다. 원래는 `clearBackStack()` + `goTo()` 두 줄을
쓰는 자리였는데, 호출부 5곳이 **전부 그 쌍**이고 단독 사용이 없었다. 따로 노출돼 있으면 그 사이에
백스택이 빈 상태가 생기고 채우는 것은 규약일 뿐이라, 하나로 묶어 빈 상태를 만들 수 있는 API 자체를
없앴다(`clearBackStack()`은 제거). 빈 백스택은 `Navigator.onBack()`이 이미 방어하고 있는 크래시 원인이다.

### 사용자 로그아웃

```
ClickLogout → LogoutUseCase → AuthRemoteDataSource.logout(refreshToken) → TokenStore.clear() → NavigateToLogin
```

`AuthRemoteDataSource.logout(refreshToken)`은 **이미 구현돼 있다**(`AuthService.postAuthLogout` +
`ApiCaller.safeApiCallNoContent`). 없는 것은 그 위의 `AuthRepository.logout()`과 호출부다.

**서버 호출이 실패해도 로컬 토큰은 지운다.** 사용자가 로그아웃을 눌렀으면 이 기기에서는 나가는
것이 기대 동작이고, 서버 세션 정리 실패는 로그로만 남긴다. 연타는 `launch(key)` 가드로 막는다.

`POST /api/v1/auth/logout`은 인증 도메인에서 유일하게 화이트리스트 밖이라 **access token과
refresh token이 둘 다 필요하다**(`api/auth.md`). 즉 access token이 만료된 상태의 로그아웃은
`TokenAuthenticator`를 한 번 타고 나가는데, **그대로 두면 반드시 실패한다.**

refresh token이 **요청 본문**에 실려 나가기 때문이다. 401 → 재발급이 일어나면 서버가 refresh
token을 **회전시키고 구 토큰을 폐기한다**(`api/auth.md`). 인증기는 `Authorization` 헤더만 갈아
끼우고 본문은 그대로 재전송하므로, 재시도는 이미 죽은 refresh token을 들고 간다. 결과는
**로컬만 정리되고 새로 발급된 서버 세션은 refresh token 수명(2주)까지 살아남는 것** — 사용자는
로그아웃했다고 믿는데 세션이 유효하다.

그래서 `logout()`은 호출이 실패하면 `TokenStore`에서 refresh token을 **다시 읽고**, 값이 바뀌었으면
(= 인증기가 비행 중에 회전시켰으면) **정확히 1회** 재전송한다. 그 뒤 로컬을 정리하고 성공을
반환하는 계약은 그대로다.

## 표시·제어 규칙

- 재발급 성공 경로에서 화면은 아무것도 보지 못한다. 로딩·에러 표시가 생기지 않는다
- `ForcedLogout` 이동은 백스택을 비운다 — 뒤로가기로 인증이 필요한 화면에 돌아갈 수 없다
- 로그아웃 버튼은 요청 중 비활성 — 다만 **클릭만 막히고 색은 바뀌지 않는다.** `YGActionItem`에
  비활성 색이 디자인시스템에 정의돼 있지 않아 컴포넌트가 임의로 정하지 않았다
  ([ygactionitem 스펙](2026-07-12-ygactionitem.md)의 as-built 노트 참고). 사용자는 클릭이
  안 먹는 이유를 알 수 없는 상태이고, 비활성 색이 확정되면 채운다

## 파일 구성

| 파일 | 역할 |
|---|---|
| `data/network/TokenAuthenticator.kt` | 401 가로채 재발급·재시도. 신규 |
| `data/session/SessionEventBus.kt` | `SessionEventSource` 구현. 발행+구독, `@Singleton`. 신규 |
| `data/di/SessionModule.kt` | `SessionEventBus` → `SessionEventSource` 바인딩. 신규 |
| `domain/model/session/SessionEvent.kt` | `ForcedLogout`. 신규 |
| `domain/repository/session/SessionEventSource.kt` | 구독 인터페이스. 신규 |
| `domain/usecase/auth/LogoutUseCase.kt` | 신규 |
| `data/di/NetworkModule.kt` | 메인 `OkHttpClient`에 `authenticator(...)` 결합 + **자격증명 없는 클라이언트·Retrofit**(독립 `Dispatcher`) + 두 클라이언트가 공유하는 `loggingInterceptor()` 추출 |
| `data/di/ServiceModule.kt` | `@UnauthenticatedClient AuthService` provider 추가(같은 인터페이스, 다른 Retrofit) |
| `data/model/qualifier/UnauthenticatedClient.kt` | 자격증명을 붙이지 않는 표면 한정자. 신규 |
| `domain/model/error/ServerErrorCode.kt` | `Auth`에 `INVALID_TOKEN`·`EXPIRED_TOKEN`·`FORBIDDEN_REFRESH_TOKEN` 추가 |
| `data/repository/auth/AuthRepositoryImpl.kt` | `logout()` 추가 |
| `domain/repository/auth/AuthRepository.kt` | `logout()` 추가 |
| `feature/app/setting/.../AppSettingViewModel.kt` | 로그아웃 stub 제거 + `NavigateToLogin` + `isLoggingOut` |
| `feature/app/setting/.../AppSettingScreen.kt` | 요청 중 로그아웃 항목 비활성 |
| `feature/app/setting/impl/build.gradle.kts` | `feature.login.api` 의존 추가(`NavKeyLogin`) |
| `feature/app/setting/.../AppSettingRoute.kt` | `NavigateToLogin` → `navigator.replaceAll(NavKeyLogin)` |
| `core/designsystem/.../ygactionitem/YGActionItem.kt` | `enabled` 파라미터 추가(클릭 차단만, 색 불변) |
| `core/navigation/.../Navigator.kt` | `replaceAll(destination)` 추가·`clearBackStack()` 제거 |
| `app/.../MainRoute.kt`·`MainActivity.kt` | `SessionEventSource` 주입·단일 구독 |

## 테스트

**`TokenAuthenticator`** — MockWebServer (`data/src/test/`)

- 401 → 재발급 200 → 원요청 재시도 성공, 새 토큰이 헤더에 실림
- 401 → 재발급 401 → `TokenStore.clear()` + `ForcedLogout` 1건 + 원요청 실패
- 401 → 재발급 네트워크 실패 → **토큰 유지**, 이벤트 0건
- 앞선 401이 갱신을 끝낸 뒤 뒤따라온 401 → **재발급 없이** 새 토큰으로 재시도(선점 확인).
  실제 동시 실행 대신 순차로 검증한다 — 스레드를 띄우면 결과가 스케줄러에 좌우돼 회귀 감지선이
  흐려진다. 막으려는 것은 "대기하다 깨어난 요청이 또 재발급하는 것"이고 그건 순차로 재현된다
- 서버가 계속 401 → 재시도 2회에서 중단(루프 가드)
- **401 아닌 선행 응답(301 등)이 있어도 재발급은 시도된다** — 루프 가드가 401만 세는지 고정
- 재발급 403 + HTML 본문 → **토큰 유지**, 이벤트 0건. 403 + 거절 `code` → 세션 종료
- envelope 실패(HTTP 200 + `success:false`, `statusCode` null) → 세션 종료. `code` 분기를 격리한다
- `@NoAuth` 엔드포인트가 401 → 재발급 시도 0건(재진입 가드). 요청은 Retrofit `Invocation` 태그를
  실제로 갖고 있어야 한다 — 태그 없는 raw 요청으로 만들면 가드가 타지 않아 테스트가 의미를 잃는다
- refresh token 부재 → 이벤트 0건, `clear()` 없음

**`NetworkModule`** — 재발급 전용 클라이언트의 구조적 성질(인증기 미부착, 메인과 다른 `Dispatcher`
인스턴스). 데드락 자체를 재현하는 테스트는 만들지 않는다 — 회귀 시 실패가 아니라 무한 대기로
나타나 CI가 걸린다.

**`AuthRepositoryImpl.logout()`** — 기존 `AuthRepositoryImplTest`에 추가

- 서버 성공 → 로컬 clear
- 서버 실패 → **로컬 clear 수행**, 실패는 전파하지 않고 로그만
- **회전 경로** — 1차 실패 후 저장소의 refresh token이 바뀌어 있으면 새 값으로 1회 재전송
- 회전하지 않았으면 재전송하지 않는다

**`AppSettingViewModel`**

- 로그아웃 클릭 → `LogoutUseCase` 1회 + `NavigateToLogin`
- 연타 → 1회만
- 요청 중 `isLoggingOut`이 서 있고, 끝나면 `finally`로 내려간다

앱 루트의 이벤트 구독은 Compose 테스트 비용 대비 이득이 적어 수동 확인으로 둔다. 대신 발행
측(`TokenAuthenticator`)을 위처럼 조인다.

**as-built(2026-08-15 머지)** — 위 목록이 그대로 파일 넷으로 들어왔다: `TokenAuthenticatorTest`
(MockWebServer, 12케이스 — envelope 실패·403 HTML·403 거절 코드·리다이렉트 선행 응답·`@NoAuth`
포함) · `NetworkModuleTest`(3케이스, `@Provides` 함수를 직접 호출해 **다른 `Dispatcher` 인스턴스**·
인증기 미부착·`AuthInterceptor` 미부착을 단언) · `SessionEventBusTest`(2) · `AuthRepositoryImplTest`
로그아웃 5케이스(회전 재전송·회전 없으면 재전송 안 함 포함) · `AppSettingViewModelTest` 로그아웃
4케이스(`isLoggingOut` 상승·`finally` 하강·연타 1회).
**계획에 없던 테스트 파일이 하나 늘었다** — `core:navigation`의 `NavigatorTest`(3케이스)가 신설돼
`replaceAll`이 목적지 하나만 남기는지·백스택이 비지 않는지·직후 `onBack`이 no-op인지를 잠근다.
`clearBackStack()`을 지운 근거(빈 백스택을 만들 수 있는 API 자체를 없앤다)를 코드가 아니라
테스트가 들고 있는 셈이다.

## 주의 / 열린 질문

- **`runBlocking`이 OkHttp 디스패처 스레드를 잡는다.** `Authenticator` 계약이 동기라 피할 수
  없다. 클라이언트 분리로 재발급이 남의 슬롯을 굶기지는 않게 됐지만, 오프라인에서 401 N건이
  각자 최대 15초(read timeout)씩 직렬로 재발급을 시도하는 지연은 남는다. **재발급 실패에 쿨다운이
  없다** — 실패 결과를 짧게 공유해 같은 웨이브의 대기자들이 재시도하지 않게 하는 것이 후속 과제다
- **재발급 성공 후 원요청이 다시 401이면** 루프 가드에 걸려 실패한다. **세션은 유지한다**(결정됨) —
  서버가 access token은 인정하는데 리소스 권한이 없는 경우와 구분되지 않으므로, 판정 불가일 때
  세션을 버리지 않는 쪽을 택했다
- **`ForcedLogout` 수집 지점이 앱 루트 한 곳이라는 것은 규약일 뿐 기계 검사가 없다.**
  `BaseViewModel.effect`가 구독자 수를 세어 로그를 남기는 것과 같은 방어를 둘지 미정
- **`TokenAuthenticator`가 한정된(`@UnauthenticatedClient`) `AuthService`를 받는다는 사실에 그물이
  없다.** 생성자에서 한정자만 지우면 모든 테스트가 통과하면서 디스패처 데드락이 되살아난다
  (`NetworkModuleTest`는 클라이언트 쪽 성질만 잠근다)
- **Activity 재생성 중 `ForcedLogout` 유실 창.** `Channel(CONFLATED)`에 `onUndeliveredElement`가
  없어, 값을 꺼낸 직후 수집 코루틴이 취소되면 이벤트가 조용히 사라진다. 토큰은 이미 지워진
  뒤라 이후 401은 "refresh token 부재" 경로로 조용히 끝나고 두 번째 이벤트가 오지 않는다
- 회원 탈퇴는 별건이다. **계약도 앱 표면도 이미 있는데**(`DELETE /api/v1/users/me`,
  `MemberRemoteDataSource#withdraw`) `AppSettingViewModel.handleConfirmWithdraw`는 stub 그대로다 —
  로그아웃이 결선된 지금 같은 화면에 결선된 항목과 안 된 항목이 나란히 있다
