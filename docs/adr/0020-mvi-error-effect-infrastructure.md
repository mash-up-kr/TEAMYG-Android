---
id: ADR-0020
title: MVI 공통 에러·이펙트 인프라 (Channel 이펙트 · AppError · launch 가드)
status: accepted
date: 2026-08-13
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0005, ADR-0009, ADR-0016, ADR-0017
related_spec: mvi-error-infrastructure, a002-kakao-login-api, ygscaffold-v2-common-loading-error
related_architecture: state-management, data-layer
platforms: android
tags: [adr, parfait]
---
# ADR-0020: MVI 공통 에러·이펙트 인프라 (Channel 이펙트 · AppError · launch 가드)

> ⚠️ **번복 (2026-08-14, 구현 중)** — 아래 **결정 ④(`error` 채널을 `E` 와 분리)가 뒤집혔다.**
> 나머지 결정(① Channel 이펙트 · ② `AppError` · ③ `launch` 가드 · ⑤ 로딩은 State 소유)은
> 그대로 유효하다. 무엇이 왜 바뀌었는지는 아래 [번복: 공용 error 채널 철회](#번복-공용-error-채널-철회).
>
> ✅ **develop 머지 완료**(2026-08-15, PR #241 `80895eb1`) — 번복분까지 포함한 최종 형태가 코드에 있다.

## 맥락

[[0005-custom-mvi-baseviewmodel]]이 정한 `BaseViewModel`은 `updateState`·`postSideEffect` 둘뿐이다.
화면 19개가 이를 상속하고 있으나 그동안 UseCase가 전부 mock이라 **실패 경로가 한 번도 지나가지
않았다.** A-002 카카오 로그인이 앱 최초의 실서버 호출을 붙이면서 다섯 가지 공백이 동시에 드러났다.

1. **이펙트 유실** — `MutableSharedFlow`가 replay 0·버퍼 0이라 `emit`이 구독자를 기다리며 suspend한다.
   구독자가 없는 순간 발행한 이펙트는 전달 시점을 보장받지 못한다. `init`에서 조회하고 실패를
   이펙트로 알리는 화면이 앞으로의 기본형인데, 그 구간이 정확히 구독 전이다.
2. **에러 타입이 `:data`에 갇힘** — `ApiException`이 `:data` 소유라 `:domain`·feature가 볼 수 없다.
   실패 원인으로 분기하려면 레이어 의존을 거꾸로 뚫어야 한다.
3. **코루틴 예외 가드 부재** — 각 ViewModel이 `viewModelScope.launch`를 직접 쓴다. 매퍼 버그 같은
   예상 못 한 예외가 나면 처리 지점이 없다.
4. **중복 실행 방어 부재** — 버튼 연타가 그대로 API 중복 호출이 된다.
5. **공통 실패 표현 경로 부재** — 화면마다 `SideEffect`에 `ShowError`를 따로 선언해야 한다.

## 결정

`BaseViewModel`을 **하위호환을 유지한 채** 확장하고, 도메인 에러 타입을 신설한다.
외부 MVI 프레임워크는 [[0005-custom-mvi-baseviewmodel]]에 이어 다시 기각한다.

**① 이펙트 전달을 `Channel(BUFFERED) + receiveAsFlow()`로 교체**

구독자가 없어도 버퍼에 남고 붙는 즉시 전달된다. `postSideEffect`는 `trySend`라 suspend도 코루틴
기동도 필요 없어져 시그니처가 그대로다 — **19개 ViewModel과 21개 수집 지점 모두 무수정**이다.

단일 소비자 제약이 생긴다. 현재 `effect` 수집은 화면당 정확히 하나(Route)이고 ViewModel을 자식
컴포저블로 내려주는 곳은 없다. 규약을 문서로만 두지 않고 `onStart`/`onCompletion`에서 동시
구독자 수를 세어 2 이상이면 error 로그를 남긴다.

**② `:domain`에 sealed `AppError` 신설**

```
sealed class AppError(message, cause) : Exception(message, cause)
  ├─ Network(cause)                                   재시도가 의미 있는 유일한 갈래
  ├─ Server(code, statusCode, serverMessage)          서버 에러 envelope
  └─ Unexpected(cause)                                그 외 전부
```

`Exception` 하위로 두어 기존 `Result<T>` 관용구를 그대로 쓴다. 변환은 **Repository 경계**에서
일어난다 — DataSource는 지금처럼 `ApiException`을 실어 보내고, Repository 구현이 `AppError`로
바꿔 도메인에 넘긴다. `CancellationException`은 변환하지 않고 재던진다.

**③ `launch(key, onError, block)` 헬퍼**

같은 `key`의 job이 살아 있으면 새 job을 만들지 않고 `null`을 반환한다(중복 방어). 블록이 던진
예외는 `AppError.Unexpected`로 감싸 `onError` 또는 `postError`로 흘리고, `CancellationException`은
재던진다. `Result.failure`는 값이므로 잡지 않는다.

**④ `error: Flow<AppError>` 채널을 `E`와 분리** — 🔁 **철회됨**(아래 번복 절)

화면 `SideEffect` 타입마다 `ShowError`를 중복 선언하지 않는다. `core:ui`가 수집 컴포저블
`CollectAppError`를 제공하고, 기본 동작은 **로그 + TODO**다(에러 UX 디자인 미확정).

**⑤ 로딩 상태는 각 State 소유**

`isLoading`을 인터페이스로 강제하지 않는다. 필드명만 규약으로 통일한다.

## 대안

- **`SharedFlow`에 `replay` 부여** — 버퍼 문제는 풀리지만 **1회성 이벤트를 재발화시킨다.**
  내비게이션 이펙트가 replay 캐시에 남아, 화면 재진입·Activity 재생성으로 새 collector가 붙는
  순간 사용자가 아무 조작도 하지 않았는데 다시 발화한다. `resetReplayCache()`는 소비 시점을
  ViewModel이 알 수 없고 부분 소비도 불가능하다.
  **→ 기각:** state는 "마지막 값이 진실", 이펙트는 "한 번 일어난 일"로 성질이 반대다.
- **`SharedFlow(replay = 0, extraBufferCapacity = N)`** — `emit` suspend는 사라진다. 그러나
  `extraBufferCapacity`는 *느린 기존 구독자*를 위한 버퍼라 **구독자가 0명이면 값은 그대로 버려진다.**
  **→ 기각:** 공백 1의 핵심(구독 전 발행)을 못 막는다.
- **`UiState`에 `isLoading` 강제(마커 인터페이스 또는 제네릭 바운드)** — 누락 불가라는 장점.
  **→ 기각:** 제네릭 바운드는 19개 화면을 즉시 깨뜨려 "점진 마이그레이션" 결정과 충돌하고,
  옵트인 마커로 타협하면 자동 토글이 불가능해 이득이 얇다. 화면마다 로딩 UI가 달라 단일 필드로
  통일되지도 않는다.
- **Orbit MVI 도입** — `intent {}`/`reduce {}` 컨테이너, 이펙트·상태 순서 보장, 테스트 지원.
  **→ 기각:** [[0005-custom-mvi-baseviewmodel]]의 자체 구현 결정을 뒤집는 비용이 크다.
  `processIntent(intent)` 진입 규약이 바뀌어 19개 화면을 전부 재작성해야 한다.
- **Orbit 스타일 자체 재구현(container·reduce DSL)** — 라이브러리 없이 같은 모양.
  **→ 기각:** 만드는 양이 채택안의 3~4배이고 결국 MVI 라이브러리 하나를 자체 유지보수하게 된다.
  현재 공백 다섯을 메우는 데 컨테이너 추상화가 필요하지 않다.
- **`ApiException`을 `:domain`으로 이동** — 매핑 계층이 없어져 간단하다.
  **→ 기각:** Retrofit `HttpException`·`IOException`을 도메인이 알게 되어 [[0001-layered-multi-module]]이
  깨진다. 화면이 구분해도 할 일이 같은 갈래(`EmptyBody`·`Http`)까지 도메인에 새어 나온다.

## 영향

**긍정**
- 이펙트 유실·재발화가 둘 다 닫힌다. 19개 화면이 호출부 수정 없이 수혜를 받는다.
- 실패 원인 분기가 레이어를 지키면서 가능해진다.
- 연타 방어·예외 가드가 화면마다의 관행이 아니라 베이스의 계약이 된다.
- ~~에러 UX가 정해지면 `CollectAppError` 한 곳만 고치면 전 화면에 적용된다.~~
  🔁 철회됨 — 아래 번복 절. 이 이득은 실현되지 않았고, 대가만 지불했다.

**트레이드오프**
- `Channel`은 단일 소비자다. 진짜 멀티캐스트가 필요하면 `effect`를 재활용하지 말고 해당
  ViewModel이 별도 `SharedFlow`를 노출한다. 두 성질을 한 채널에 겹치면 재발화 문제가 돌아온다.
- 버퍼 초과분(64개)은 조용히 버려진다.
- 로딩 필드 선언이 화면마다의 손일로 남는다.

**위험·방어**
- 이펙트 2중 수집은 어느 primitive로도 조용히 오동작한다. 동시 구독자 카운트 로그로 드러낸다.
- 새 API를 쓰지 않는 기존 화면과 쓰는 새 화면이 공존한다. 점진 마이그레이션은 각 화면의
  API 결선 라운드에 묶어 진행한다.

## 번복: 공용 error 채널 철회

**2026-08-14, 구현 라운드 중.** 위 결정 ④를 되돌린다. `error`·`postError`·`CollectAppError`를
삭제하고 실패도 화면의 이펙트 어휘(`E`)로 옮긴다.

### 무엇이 틀렸나

결정 ④가 내세운 이유("화면마다 `ShowError`를 중복 선언하지 않는다")는 **진짜 이유가 아니었다.**
진짜 이유는 타입 시스템 제약이다 — `BaseViewModel`은 feature의 `E`를 생성할 수 없어서, `launch`
가드가 잡은 예외를 담을 곳이 없었고 자기가 아는 타입의 채널을 하나 더 팠다. 설계 미덕이 아니라
우회였고, ADR이 우회를 미덕으로 적었다.

### 무엇을 사지 못했나

구현이 끝난 시점에 세어 보니 `postError` 호출처는 `launch` 가드 **한 곳**, `CollectAppError`
사용처는 `LoginRoute` **한 곳**이었고, 기본 동작은 로그였다. **채널 하나를 통째로 유지하면서
하는 일이 로그 한 줄**이었다.

### 무엇을 지불했나

- 이 ADR이 딛고 선 [[0005-custom-mvi-baseviewmodel]]의 **3분할 계약이 4분할이 됐다.**
  [[state-management]]는 셋이라고 적은 채였다 — 문서와 코드가 갈렸다.
- **두 채널 사이에 순서 보장이 없다.** "에러를 알리고 뒤로 간다" 같은 흐름을 쓸 수 없다.
  같은 채널이면 공짜로 되는 것이었다.
- **실패 경로가 아예 없는 화면까지 빈 채널을 하나씩 달았다.** 화면 다수가 그렇다.

### 대신 무엇을

`launch(onError = …)`가 이미 있었다. 새 API가 필요 없다.

```
// 실패를 표현하는 화면 — 자기 sealed 에 케이스를 두고 그리로 옮긴다
launch(key = …, onError = { postSideEffect(XxxSideEffect.ShowError(it)) }) { … }

// 표현하지 않는 화면 — 아무것도 안 한다. 가드가 로그만 남긴다
launch(key = …) { … }
```

`onError`를 넘기지 않았을 때의 결과(로그)는 철회 전 `CollectAppError` 기본 동작과 **같다.**
잃은 기능이 없다.

### 남는 것

에러 UX가 "전 화면 공통 토스트 하나"로 정해지면 화면마다 케이스 추가가 필요하다 — 그게 이
번복의 유일한 비용이다. 다만 화면별 표현이 실제로 갈릴 가능성이 크고(토스트/인라인/다이얼로그),
세션 만료처럼 **진짜 앱 전역**인 관심사는 애초에 VM 채널이 아니라 앱 스코프 버스 소관이다 —
위 트레이드오프 절이 "진짜 멀티캐스트가 필요하면 별도 `SharedFlow`"라고 적어 둔 그 자리다.

> 📌 **그 비용이 실제로 청구됐고, 예상대로 갈렸다(2026-08-16, PR #267)** —
> [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)이 **"알리고 끝나는
> 실패는 공통 토스트"**로 확정하고 `YGScaffoldV2`가 그 자리를 제공한다. 다만 **공통은 자리뿐이고
> 케이스는 화면마다 추가한다** — 이 절이 예상한 그대로다. 계약이 `String`이라 문구는 화면 소유이고,
> A-002가 `LoginError` + `LoginSideEffect.ShowError`로 첫 사례를 만들었다. **재시도 동선이 필요한
> 차단성 실패는 여전히 화면 소관**이라 표현은 지금도 갈려 있다(OQ-P-167).
> `launch(onError = …)`가 "이 자리가 통로"로 실제 소비된 첫 사례이기도 하다 — A-002가 `onError`와
> `Result.failure` 두 경로를 모두 토스트로 잇는다.

### 배운 것

ADR이 **결정의 진짜 제약을 적지 않고 사후 정당화를 적으면** 다음 사람이 그 정당화를 검증할 수
없다. ④의 근거가 "베이스가 `E`를 만들 수 없다"라고 쓰여 있었다면 "그럼 `E`를 만들 수 있게
하거나, 만들 필요가 없게 하면 되지 않나"라는 질문이 설계 시점에 나왔을 것이다.
