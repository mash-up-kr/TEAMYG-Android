---
id: ADR-0021
title: 401 자동 재발급 — OkHttp Authenticator + 강제 로그아웃 이벤트
status: accepted
date: 2026-08-15
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0019, ADR-0017, ADR-0020, ADR-0001
related_spec: session-token-refresh-infra
related_architecture: data-layer
platforms: android
tags: [adr, parfait, auth, network, session]
---

# ADR-0021: 401 자동 재발급 — OkHttp Authenticator + 강제 로그아웃 이벤트

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

> ✅ **develop 머지(2026-08-15, PR #260 `9cfbd117`)** — 결정이 코드가 됐다. 아래 서술의 `@AuthClient`는
> as-built에서 **`@UnauthenticatedClient`**다(사용처가 아니라 표면의 성질을 가리키도록 개명, 세
> provider가 이 한정자를 단다). 나머지는 본문 그대로이고, 방어 네 겹·실패 두 부류·`CONFLATED` 채널·
> 앱 루트 단일 수집이 전부 테스트로 잠겼다
> ([스펙](../superpowers/specs/archive/2026-08-15-session-token-refresh-infra.md) "테스트" 절).
> **같은 라운드가 `Navigator`도 바꿨다** — `clearBackStack()`을 제거하고 `replaceAll(destination)`으로
> 합쳐 "비우고 안 쌓은" 중간 상태를 API에서 지웠다(강제 로그아웃 이동이 첫 소비처).

## 맥락

[ADR-0019](0019-encrypted-token-storage.md)가 토큰을 저장하고 [ADR-0017](0017-remote-network-datasource.md)의
`AuthInterceptor`가 access token을 헤더에 붙인다. 그러나 **만료를 다루는 주체가 없다.**
`AuthRemoteDataSource.reissue()`는 구현돼 있으나 호출부가 0건이고, access token이 만료되면 모든
인증 API가 401로 깨진 채 각 화면이 알아서 실패를 표시한다.

여기에 두 가지가 겹친다. 재발급은 **여러 요청이 동시에 401을 맞는 상황**을 전제해야 하고,
재발급마저 실패하면 **앱 전체가 반응해야 한다** — 화면 하나가 결정할 수 있는 일이 아니다.

## 결정

**OkHttp `Authenticator`가 401을 가로채 재발급 후 원요청을 재시도하고, 재발급이 서버에 거절당하면
`:domain`에 둔 단일 이벤트 스트림으로 강제 로그아웃을 알린다.**

- **`TokenAuthenticator`**(`data/network`) — `authenticate()`가 `@NoAuth` 가드 → 루프 가드 →
  `Mutex` → 선점 확인 → 재발급 순으로 판단한다. `Authenticator` 계약이 동기라 `runBlocking`을
  쓴다(`TokenStoreTokenProvider` 선례와 동일).
- **재발급은 전용 `OkHttpClient`로 나간다**(`@UnauthenticatedClient`, 독립 `Dispatcher`, 인증기·`AuthInterceptor`
  없음). 같은 클라이언트를 쓰면 **디스패처가 고갈돼 앱 전체가 정지한다** — `authenticate()`는 자기
  호출이 슬롯을 점유한 채 블록된 상태로 실행되는데, 재발급이 같은 디스패처·같은 호스트로 enqueue
  되고 기본 `maxRequestsPerHost`는 5다. 동시 401이 5건이면 재발급이 영원히 promote되지 않고
  `callTimeout`도 없어 풀리지 않는다. `newBuilder()` 파생은 부모의 `Dispatcher`를 물려받아 무효다.
  부수 효과로 `Retrofit`↔`OkHttpClient`↔`Authenticator` Dagger 순환이 사라져 `Provider` 지연 주입이
  필요 없어졌다.
- **방어는 네 겹이고 하나라도 빠지면 뚫린다.** `@NoAuth` 가드가 재발급 요청 자신의 재진입을 막고,
  `Mutex`가 직렬화하며, **실패한 요청이 들고 갔던 `Authorization` 값과 현재 저장 토큰을 비교**하는
  선점 확인이 대기하다 깨어난 요청의 중복 재발급을 막고, `priorResponse` 가드가 무한 재시도를
  끊는다. `Mutex`만으로는 부족하다 — 직렬화될 뿐 대기자들이 차례로 각자 재발급을 쏜다.
  루프 가드는 **401인 선행 응답만** 센다. 체인 전체를 세면 리다이렉트 한 번에 첫 401이 2회차로
  보여 재발급을 아예 시도하지 못한다.
- **실패를 두 부류로 가른다.** 서버가 refresh token을 거절한 경우만 세션을 버리고, 네트워크
  실패·5xx는 **토큰을 유지한 채** `null`을 반환해 원요청 401이 화면에 도달하게 한다. refresh
  token이 아예 없는 경우도 조용히 `null`이다. **status만으로 세션을 끝내는 것은 401뿐이다** —
  재발급은 계약상 403을 내지 않는 반면 WAF·프록시는 HTML과 함께 403을 내므로, 403은 본문 `code`가
  거절 코드일 때만 인정한다. 그러지 않으면 로그인 상태를 유지했어야 할 사용자가 조용히 로그아웃된다.
- **`SessionEvent.ForcedLogout`은 `:domain`에 둔다.** feature 모듈은 `:data`를 보지 않으므로
  (ADR-0001) 인터페이스가 `:domain`에 있고 구현(`@Singleton`, `Channel(CONFLATED)` +
  `receiveAsFlow()`)이 `:data`에서 발행과 구독을 겸한다.
  > 📌 **이름과 자리가 바뀌었다(2026-09-05, PR #450)** — 인터페이스 `SessionEventSource` → **`SessionEventBus`**
  > (`:domain` `event/`), 구현 `SessionEventBus` → **`SessionEventBusImpl`**(`:data` `event/`)이다.
  > 푸시 딥링크 통로와 규칙을 맞춘 것이고(**인터페이스가 `~EventBus`, 구현이 `~Impl`**), `Source`가 이
  > 저장소에서 DataSource 계열 이름이라 이벤트 구독구에 붙으면 오독을 부른다는 것이 근거다.
  > **결정 자체는 그대로다** — 채널 선택·단일 수집·비대칭(구독만 도메인에 내놓는다) 모두 변함없고
  > Hilt 그래프도 같다.
  `SharedFlow`가 아닌 이유는 ADR-0020이 이펙트에서 정리한 것과 같다 — 구독 전 발행이 버퍼에
  남아야 하고 소비한 이벤트가 재구독으로 다시 오면 안 된다. `CONFLATED`는 401이 여러 건 터져도
  이동을 한 번으로 접는다. 수집은 **앱 루트 한 곳** — 화면마다 구독하면 한 이벤트로 여러 번 이동한다.
- **사용자 로그아웃은 서버 실패와 무관하게 로컬을 정리한다.** 눌렀으면 이 기기에서는 나가는 것이
  기대 동작이고, 서버 세션 정리 실패는 로그로만 남긴다.

## 대안

- **재발급을 부트스트랩 한 곳에서만 명시적으로 호출** — 스플래시에서 401이면 재발급하고 실패하면
  로그인으로. 호출 지점이 하나뿐이라 동시성 문제가 없고 테스트가 쉽다. 그러나 앱을 쓰는 도중
  만료되는 경우를 전혀 다루지 못한다 — access token 수명이 짧을수록 화면들이 401을 직접 맞는
  빈도가 올라가고, 결국 화면마다 "재로그인해 주세요"를 붙이게 된다.
  **→ 기각:** 만료는 앱 수명 전체에 걸쳐 일어나는 사건이고, 그것을 다루는 자리는 네트워크 계층이다.
- **`Interceptor`에서 401을 처리** — `Authenticator`보다 익숙하고 요청/응답을 자유롭게 다룬다.
  그러나 OkHttp는 인증 재시도를 위해 `Authenticator`를 따로 두고 있고, 재시도 횟수 추적
  (`priorResponse`)과 `Route` 컨텍스트를 그쪽에만 준다. `Interceptor`로 하면 재시도 루프 방어를
  직접 만들어야 한다.
  **→ 기각:** 플랫폼이 이미 제공하는 자리를 두고 재구현할 이유가 없다.
- **재발급 실패를 `AppError`로만 흘리고 전역 이벤트를 두지 않음** — 새 개념을 안 만들고 ADR-0020의
  기존 실패 경로를 그대로 쓴다. 그러나 "이 화면의 요청이 실패했다"와 "세션이 끝났다"는 다른
  사건이다. 후자를 전자로 표현하면 화면마다 로그인 이동을 복제하게 되고, 여러 화면이 동시에
  실패하면 이동이 중복된다.
  **→ 기각:** 전역 사건은 전역 통로가 필요하다. 대신 통로를 하나로 좁히고 수집 지점을 앱 루트로 못박는다.
- **네트워크 실패도 강제 로그아웃** — 분기가 하나로 줄어 구현·테스트가 단순하다. 그러나 일시적
  단절만으로 2주짜리 refresh token을 버린다 — 지하철에서 앱을 켠 것이 로그아웃 사유가 된다.
  **→ 기각:** 연결 실패와 자격증명 만료는 다른 사건이다.

## 영향

**긍정**

- 만료가 화면에 보이지 않는다 — 재발급 성공 경로에서 화면은 로딩도 에러도 겪지 않는다
- 재발급 정책이 한 파일에 모인다. 화면·Repository가 401을 알 필요가 없다
- 세션 종료 반응이 한 곳 — 로그인 이동 로직이 앱 루트에만 존재한다

**트레이드오프**

- `runBlocking`이 OkHttp 디스패처 스레드를 점유한다. `Authenticator` 계약이 동기라 피할 수 없고,
  재발급이 타임아웃까지 늘어지면 그 스레드가 묶인다. 클라이언트 분리로 남의 슬롯을 굶기지는
  않게 됐지만, **재발급 실패에 쿨다운이 없어** 오프라인에서 401 N건이 각자 최대 15초씩 직렬로
  재시도하는 지연은 남는다
- 클라이언트가 둘이 됐다 — 소켓·`ConnectionPool`이 하나 더 뜨고, 네트워크 설정을 바꿀 때 두 곳을
  맞춰야 한다
- 수집 지점이 하나여야 한다는 것이 규약일 뿐 기계 검사가 없다

**위험·방어**

- 방어 네 겹(재진입 가드·직렬화·선점 확인·루프 가드)을 각각 MockWebServer 테스트로 고정한다.
  특히 **앞선 401이 갱신을 끝낸 뒤 뒤따라온 401에 재발급 0회**가 선점 확인의 회귀 감지선이다
- 네트워크 실패 시 **토큰이 남아 있는지**를 테스트가 직접 단언한다 — 이 분기가 무너지면 오프라인
  진입이 곧 로그아웃이 된다
- 403 + HTML 본문이 세션을 끝내지 않는지 단언한다 — 프록시가 낸 403에 로그아웃되는 것을 막는다
- 루프 가드가 401만 세는지는 **401 아닌 선행 응답이 있어도 재발급이 시도되는지**로 고정한다
- refresh token 부재 경로는 이벤트 0건으로 단언한다 — 로그인 화면 자기순환 방어
- **디스패처 데드락 자체는 테스트로 재현하지 않는다.** 회귀가 실패가 아니라 무한 대기로 나타나
  CI가 걸린다. 대신 전용 클라이언트의 구조적 성질(인증기 미부착, 별도 `Dispatcher` 인스턴스)을
  고정한다. 남은 구멍: **`TokenAuthenticator`가 한정된 `AuthService`를 받는다는 사실에는 그물이
  없다** — 생성자에서 `@UnauthenticatedClient`만 지우면 모든 테스트가 통과하면서 데드락이 되살아난다
