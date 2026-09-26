---
id: mvi-error-infrastructure
title: MVI 공통 에러·이펙트 인프라 (core:ui BaseViewModel 확장 · AppError)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-15
related_code: BaseViewModel, MviContract, AppError, AppErrorMapper, ApiException, ApiCaller, viewModelLogger, runSuspendCatching
related_adr: ADR-0005, ADR-0009, ADR-0016, ADR-0017, ADR-0020
related_spec: a002-kakao-login-api, data-api-service-layer, unit-test-infrastructure
related_architecture: state-management, data-layer
supersedes:
superseded_by:
tags: [spec, parfait, mvi, error]
---

# Spec: MVI 공통 에러·이펙트 인프라

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ✅ **develop 머지 완료** (2026-08-15, PR #241 `80895eb1`). 본문은 as-built 다 —
> **설계에서 뒤집힌 결정이 하나 있다**(공용 `error` 채널 철회,
> [ADR-0020 번복 절](../../../adr/0020-mvi-error-effect-infrastructure.md#번복-공용-error-채널-철회)).


## 목표

`BaseViewModel`이 실패 경로를 다룰 수 없는 상태에서 앱 최초의 실서버 호출([a002-kakao-login-api](2026-08-13-a002-kakao-login-api.md))이
붙는다. 그 전에 **에러 타입·이펙트 전달·예외 가드·중복 방어·공통 실패 표현**을 베이스에 넣는다.
결정 근거와 기각 대안은 [ADR-0020](../../../adr/0020-mvi-error-effect-infrastructure.md).

## 범위

- **포함**
  - `:domain`에 sealed `AppError` 신설, `:data`에 `ApiException → AppError` 매핑.
  - `BaseViewModel` 이펙트 전달을 `Channel(BUFFERED)`로 교체 + `launch(key, onError, block)` 추가.
  - `core:ui` 단위 테스트 소스셋 신설.
  - 🔁 **설계 시점에 있었던 `error`·`postError`·`CollectAppError`는 구현 중 철회됐다** —
    [ADR-0020 번복 절](../../../adr/0020-mvi-error-effect-infrastructure.md#번복-공용-error-채널-철회).
- **제외**
  - 기존 19개 ViewModel의 새 API 이관 — **하위호환을 유지하고 각 화면의 API 결선 라운드에 묶는다.**
    `Channel` 전환만은 호출부 수정 없이 전 화면에 즉시 적용된다.
  - 에러 UX(토스트 문구·재시도 버튼) — 디자인 미확정. 경로만 열어 두고 로그 + TODO.
  - 로딩 상태의 인터페이스 강제 — 각 `UiState`가 `isLoading`을 소유하는 규약만 둔다.

## API / 인터페이스

### AppError (`:domain`)

```kotlin
sealed class AppError(message: String?, cause: Throwable?) : Exception(message, cause) {
    /** 연결 실패·타임아웃. 재시도가 의미 있는 유일한 갈래 */
    data class Network(override val cause: Throwable?) : AppError(cause?.message, cause)

    /** 서버가 에러 envelope 를 준 경우. code 로 도메인 분기 */
    data class Server(
        val code: String,
        val statusCode: Int?,
        val serverMessage: String,
    ) : AppError(serverMessage, null)

    /** envelope 밖 HTTP 실패·빈 본문·파싱/매핑 실패 등 그 외 전부 */
    data class Unexpected(override val cause: Throwable?) : AppError(cause?.message, cause)
}
```

- **`Exception` 하위인 이유** — `Result.failure`가 `Throwable`을 요구한다. 기존 `Result<T>` 관용구
  (DataSource·UseCase 전반)를 그대로 쓰기 위한 제약이다.
- **갈래가 셋인 이유** — 화면이 실제로 다르게 굴 수 있는 경우가 셋뿐이다(재시도 권유 / 서버가
  말해준 이유 / 우리 잘못). `ApiException` 5종을 그대로 올리면 화면이 구분해도 할 일이 같은
  `EmptyBody`·`Http`가 도메인까지 새어 나온다.
- **`code`가 enum이 아니라 String인 이유** — 서버가 코드를 추가할 때마다 앱이 깨지지 않아야 한다.
  서버 에러 코드 문자열은 도메인 간 유일하지 않아(`MEMBER_NOT_FOUND`가 401·404 양쪽에 존재)
  `statusCode`를 함께 들고 간다 → [api/conventions.md](../../../api/conventions.md).

### AppError 매핑 (`:data`)

```kotlin
internal fun Throwable.toAppError(): AppError
internal fun <T> Result<T>.mapErrorToAppError(): Result<T>
```

| ApiException | AppError |
|---|---|
| `Business` | `Server(code, statusCode, serverMessage)` |
| `Network` | `Network` |
| `Http` | `Unexpected` |
| `EmptyBody` | `Unexpected` |
| `Unknown` | `Unexpected` |

`CancellationException`은 변환하지 않고 **재던진다.** 취소를 에러로 오분류하면 화면을 벗어날
때마다 에러가 발행된다.

변환 지점은 **Repository 경계**다. DataSource는 지금처럼 `ApiException`을 싣고, Repository 구현이
바꿔 도메인에 넘긴다. 이로써 feature 모듈이 `:data`를 보지 않는다.

### BaseViewModel (`core:ui`)

```kotlin
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiSideEffect>(
    initialState: S,
) : ViewModel() {
    val state: StateFlow<S>
    val effect: Flow<E>              // Channel(BUFFERED).receiveAsFlow()

    abstract fun processIntent(intent: I)

    protected fun updateState(reducer: S.() -> S)
    protected fun postSideEffect(effect: E)        // trySend — suspend 아님

    @MainThread
    protected fun launch(
        key: Any? = null,
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit,
    ): Job?
}
```

- `postSideEffect`·`updateState`는 **시그니처가 그대로**다. 기존 19개 ViewModel 무수정.
- **공용 에러 스트림은 두지 않는다.** 실패도 1회성 효과라 이펙트와 성질이 같은데, 스트림을 나누면
  둘 사이 순서 보장이 사라지고 실패 경로가 아예 없는 화면까지 빈 채널을 하나씩 달게 된다.
  실패를 어떤 동작으로 옮길지는 화면의 어휘(`E`)가 정한다 — 필요한 화면이 `onError`에서
  자기 `SideEffect`를 발행한다.
- `launch` 계약
  - `key != null`이고 같은 key의 job이 살아 있으면 **새 job을 만들지 않고 `null` 반환**.
  - 완료·취소 시 `invokeOnCompletion`으로 내부 맵에서 제거.
  - 블록이 던지면 `AppError.Unexpected`로 감싸 `onError`로 넘긴다. **`onError`가 없으면 로그만**
    남는다 — 실패 표현이 필요 없는 화면이 다수다.
  - `block()` 호출은 `coroutineScope { }`로 감싼다. 안 그러면 블록 안에서 띄운 **자식 코루틴의
    실패가 이 가드에 잡히지 않고** 부모 취소로 둔갑해 원래 예외가 `viewModelScope`로 새어 앱이 죽는다.
  - `CancellationException`은 재던져 구조적 취소를 보존.
  - **`Result.failure`는 잡지 않는다** — 값이지 예외가 아니므로 호출부가 명시적으로 처리한다.
    `launch`의 가드는 매퍼 버그·NPE 같은 *예상 못 한* 예외용이다.
- 내부 job 맵의 스레드 안전 불변식은 **`launch`가 항상 메인 스레드에서 호출된다는 것**이다
  (`viewModelScope`가 `Main.immediate`라서가 아니다 — 그건 `block`이 도는 디스패처일 뿐,
  `runningJobs[key] = job` 줄 자체는 코루틴 밖에서 돈다). `@MainThread`로 표시한다.
- 완료 핸들러는 `if (runningJobs[key] === job)`로 **자기 것일 때만** 지운다. 무조건 지우면
  이전 job 완료와 같은 key 재등록 사이에서 새 엔트리를 지워 중복 방어가 뚫린다.

### 실패를 화면 어휘로 옮기는 법

```kotlin
// 실패를 표현하는 화면 — 자기 sealed 에 케이스를 두고 그리로 옮긴다
launch(key = …, onError = { postSideEffect(XxxSideEffect.ShowError(it)) }) { … }

// 표현하지 않는 화면 — 아무것도 안 한다. 가드가 로그만 남긴다
launch(key = …) { … }
```

## 동작 / 상태

### 이펙트 전달

| 상황 | 동작 |
|---|---|
| 구독자 없음 → 이후 구독 | 버퍼에 보관했다가 **전달** |
| 구독 종료 후 재구독 | 이미 소비한 이펙트 **재발화 없음** |
| 버퍼(64) 초과 | 초과분 **드롭** |
| 동시 구독자 2 이상 | 이펙트가 한쪽에만 감 + **error 로그** |

동시 구독자 수는 `onStart`/`onCompletion`에서 카운트한다. 어느 primitive를 써도 2중 수집은
오동작하므로(SharedFlow는 내비게이션 2회 실행), 조용히 넘기지 않고 드러내는 것이 목적이다.

### 중복 방어

`launch(key)`가 서버 호출을 막는다. 사용자 조작 자체(외부 SDK 다이얼로그 등)가 `launch` 이전에
일어나는 화면은 **State의 `isLoading` 가드를 한 겹 더** 둔다 → [a002 스펙](2026-08-13-a002-kakao-login-api.md).

## 표시·제어 규칙

- 로딩 필드명은 `isLoading`으로 통일한다(인터페이스 강제 없음).
- 표시 문자열·리소스 ID는 여전히 State에 담지 않는다([ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md)).
  `AppError`는 도메인 타입이며 문구 매핑은 화면 소관이다.

## 파일 구성

```
domain/model/error/AppError.kt              신설
data/model/error/AppErrorMapper.kt          신설 — toAppError · mapErrorToAppError
data/repository/**/                         Repository 구현이 경계에서 변환
core/ui/BaseViewModel.kt                    Channel 전환 + launch(key, onError, block)
core/ui/build.gradle.kts                    parfait.test.unit 플러그인 추가
core/ui/src/test/                           신설
```

## 테스트

`BaseViewModel`

- 구독자 없이 `postSideEffect` → 이후 수집 시 **받는다**.
- 수집 종료 후 재구독 → **재발화 없음**(replay 회귀 방지).
- 같은 key로 `launch` 2회 → 두 번째는 `null` 반환, 블록 미실행.
- 다른 key면 둘 다 실행.
- job 완료 후 같은 key 재호출 → 실행됨(맵 정리 확인).
- 블록이 `IllegalStateException` → `onError`가 `AppError.Unexpected`를 받는다.
- 블록이 띄운 **자식 코루틴**이 던져도 `onError`가 받는다(`coroutineScope` 회귀 방지).
- `onError`를 안 넘기고 블록이 던지면 → 로그만, **이펙트 스트림은 오염되지 않는다**.
- 블록이 `CancellationException` → `onError`에 **도달하지 않는다**, 취소 전파.

`toAppError` — `ApiException` 5종 전부 + `CancellationException` 재던짐.

Turbine·MockK는 이미 갖춰져 있다 → [unit-test-infrastructure](2026-08-06-unit-test-infrastructure.md).

## 주의 / 열린 질문

- **`Channel`은 단일 소비자다.** 현재 `effect` 수집 지점은 화면당 정확히 하나이고 ViewModel을 자식
  컴포저블로 내려주는 곳은 없음을 확인했다. 진짜 멀티캐스트가 필요해지면 `effect`를 재활용하지
  말고 해당 ViewModel이 별도 `SharedFlow`를 노출한다.
- **점진 마이그레이션이 과도기를 만든다.** 새 API를 쓰는 화면과 안 쓰는 화면이 공존한다. 이관은
  각 화면의 API 결선 라운드에 묶는다 → [open-questions](../../../synthesis/open-questions.md).
- **에러 UX 부재**가 이 스펙의 의도된 공백이다. 디자인 확정 전까지 실패는 로그로만 남는다.
  전 화면 공통 토스트로 정해지면 화면마다 `SideEffect` 케이스 + `onError` 추가가 필요하다 —
  그것이 공용 채널 철회의 유일한 비용이다.
- **세션 만료처럼 진짜 앱 전역인 실패**는 VM 이펙트가 아니라 앱 스코프 버스 소관이다. 아직 없다
  → [open-questions](../../../synthesis/open-questions.md).
