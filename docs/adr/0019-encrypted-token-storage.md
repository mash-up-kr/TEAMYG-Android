---
id: ADR-0019
title: 인증 토큰 암호화 저장 — Android Keystore AES/GCM + Preferences DataStore
status: accepted
date: 2026-08-02
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0008, ADR-0017
related_spec: network-envelope-token-storage
related_architecture: data-layer
platforms: android
tags: [adr, parfait, security, network, data, auth]
---

# ADR-0019: 인증 토큰 암호화 저장

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.
>
> **코드 머지 확인(2026-08-04, PR #190)** — 아래 결정이 그대로 develop에 들어왔다
> (`CryptoManager`·`TokenStore`·`EncryptedTokenStore`·`TokenStoreTokenProvider`, `EmptyTokenProvider` 삭제).
> `EncryptedTokenStore.read()`의 `runCatching` 범위는 설계보다 넓다 — 복호화뿐 아니라 **DataStore
> 읽기까지** 감싸고 복구 경로의 `clear()`도 `runCatching`으로 감싼다(아래 "키 유실 시 정책" 참고).
> **검증 상태는 변하지 않았다**: `TokenStore.save()` 호출부가 develop 기준 0건이라 실기기 왕복도
> 키 유실 경로도 확인하지 못했다.

## 맥락

[ADR-0017](0017-remote-network-datasource.md)이 만든 `TokenProvider` 추상화는 stub
구현(`EmptyTokenProvider`)이 항상 `null`을 반환해, 인증이 필요한 API를 전혀 호출할 수 없었다.
실제 토큰 소스를 붙이려면 토큰을 어딘가에 저장해야 하는데, refresh token은 만료까지
**2주**(`jwt.refresh-token-expiration-seconds`, [api/spec/auth-kakao-login.md](../api/spec/auth-kakao-login.md))
가는 장기 자격증명이라 평문 저장이 부적절하다.

## 결정

**Android Keystore에 AES/GCM 키를 만들어 토큰 문자열을 암호화하고, `IV + 암호문`을 Base64 인코딩한
문자열 하나로 Preferences DataStore에 저장한다.**

- **`CryptoManager`**(`data/security`) — Keystore 키 생성·조회와 암복호화만 담당, 저장 매체를 모른다.
  - 키 별칭 1개(`AndroidKeyStore`에 상주), `KeyGenParameterSpec`으로 `PURPOSE_ENCRYPT`/`PURPOSE_DECRYPT`
    + `BLOCK_MODE_GCM` + `ENCRYPTION_PADDING_NONE` 지정.
  - `encrypt(plainText)`가 매 호출마다 새 GCM IV를 뽑는다(`cipher.iv`, Cipher가 자동 생성) — GCM은
    동일 키로 IV를 재사용하면 안전성이 깨지기 때문이다. 복호화에 IV가 필요하므로 `IV + 암호문`을
    이어붙여 Base64 문자열 하나로 반환한다(`decrypt`는 앞 12바이트를 IV로 분리해 되돌림).
  - `encrypt`/`decrypt`는 실패를 삼키지 않고 그대로 던진다 — 예외 처리는 호출자(`EncryptedTokenStore`)
    책임으로 분리했다.
- **`TokenStore`**(인터페이스) + **`EncryptedTokenStore`**(`data/source/token/local`) —
  `getAccessToken()`·`getRefreshToken()`·`save(accessToken, refreshToken)`·`clear()`, 전부 suspend.
  `CryptoManager`로 암복호화한 문자열을 [ADR-0008](0008-datastore-local-persistence.md)의
  기존 `DataStore<Preferences>`(`DataStoreModule` 제공, 새 인스턴스 아님)에 넣고 뺀다.
  > 📌 **as-built(PR #263, 2026-08-16)** — 암복호화·폐기를 `EncryptedTokenStore`가 직접 하지 않고
  > **`EncryptedPreferences`**(`data/datastore/`)에 위임한다. 같은 저장 형태(값이 아니라 암호문)를
  > 쓰는 저장소가 둘이 되며(계정 정보, [ADR-0022](0022-user-info-local-ssot.md)) 세 가지 — 쓰기의
  > 암호화, 읽기의 복호화·역직렬화, **못 읽는 저장분 폐기** — 가 그대로 복제될 자리였고, 특히
  > 마지막은 빠뜨려도 평소엔 드러나지 않는다. 저장소가 정하는 것은 **무엇을 지울지**와
  > **어떻게 해석할지**뿐이다. `save`는 두 토큰을 **한 `edit` 블록**에서 써 반쪽만 저장된 상태가
  > 보이지 않게 한다.
- **`TokenProvider`** — ADR-0017의 동기 인터페이스를 그대로 유지하고 구현만 교체한다
  (`EmptyTokenProvider` → `TokenStoreTokenProvider`, `EmptyTokenProvider`는 삭제). `AuthInterceptor`는
  시그니처 변경 없음. `TokenStoreTokenProvider.getToken()`은 `runBlocking { tokenStore.getAccessToken() }`으로
  suspend 경계를 넘는다 — OkHttp `Interceptor.intercept`가 동기 API라 suspend를 직접 호출할 수 없고,
  이 코드는 OkHttp dispatcher 스레드에서 실행되므로 메인 스레드를 막지 않는다. 대안이던
  "메모리 캐시(StateFlow) + 동기 읽기"는 앱 시작 직후 캐시가 비어 있는 창에서 첫 요청이 토큰 없이
  나가는 타이밍 문제를 새로 만들어 채택하지 않았다.

## 키 유실 시 정책

기기 복원·잠금 화면 자격증명 변경 등으로 Keystore 키가 무효화되면 `CryptoManager.decrypt`가 예외를
던진다. **`EncryptedTokenStore`는 이 예외를 밖으로 전파하지 않는다** — 내부 `read()`가 복호화
실패를 잡아 `clear()`를 호출하고 `null`을 반환한다. 앱은 "토큰 없음" 상태가 되어 자연스럽게
재로그인 경로로 간다.

> **as-built(PR #190)** — 머지된 `read()`의 가드는 복호화만이 아니라 **DataStore 읽기까지** 포함하고,
> 복구 경로의 `clear()`도 다시 `runCatching`으로 감싼다. 즉 키 무효화뿐 아니라 저장소 I/O 실패도
> 같은 "토큰 삭제 후 null" 경로로 떨어지고, 삭제 자체가 실패해도 `null` 반환은 보장된다. 넓힌 만큼
> **일시적 I/O 실패를 영구 토큰 삭제로 처리**하게 되는데, 재로그인으로 복구 가능하므로 수용한다.
>
> 🔁 **정정(PR #263, 2026-08-16) — 그 수용이 되돌려졌다.** 폐기 판단이 `EncryptedPreferences`
> (`data/datastore/`)로 옮겨가며 **저장소 읽기 실패와 해석 실패가 갈렸다**: 값을 손에 넣고도
> 복호화·역직렬화하지 못했을 때만 폐기하고, 읽기 자체가 실패하면(디스크 IO) **아무것도 지우지 않고
> `null`만 돌려준다**. 근거는 값이 손상됐다는 근거가 없다는 것 — 일시적 실패로 지우면 다음 시도에
> 살아날 세션까지 잃는다. `EncryptedTokenStore`는 자기 몫 하나만 남겼다: **폐기 범위가 한 짝 전체**
> (`clear()`)라는 것. 두 토큰이 같은 키로 암호화돼 하나를 못 읽으면 다른 하나도 못 읽기 때문이고,
> 프록시가 이 범위를 임의로 정하지 않도록 호출부가 `onDecodeFailure`로 넘긴다(계정 정보는 자기 키
> 하나만 지운다 → [ADR-0022](0022-user-info-local-ssot.md)).

예외를 전파하면 `TokenProvider.getToken()` → `AuthInterceptor.intercept`에서 터져 **모든 네트워크
요청이 죽는다** — 사용자가 앱을 지우기 전까지 복구할 수 없는 상태가 된다.

> ⚠️ **as-built** — `read()`의 `runCatching` 범위가 복호화 호출보다 넓다. **DataStore 읽기까지 함께**
> 감싸므로 키 무효화뿐 아니라 저장소 I/O 실패도 같은 복구 경로(`clear()` + `null`)로 떨어진다.
> 복구 경로의 `clear()`도 다시 `runCatching`으로 감싸 **삭제가 실패해도 `null` 반환은 보장**된다
> (삭제 실패 시 다음 읽기에서 같은 경로를 한 번 더 시도한다). 결과적으로 "복호화 실패만 잡는다"보다
> 넓은 계약이다 — 토큰 유실을 감수하고 앱이 죽지 않는 쪽을 택한 것으로, 위 결정의 의도와 같은 방향이다.

## 대안

- **대안 A — Tink** — 구글이 유지보수하는 암호 라이브러리라 키 관리·논스 처리 등 직접 구현 실수를
  피할 수 있다. 그러나 의존성·APK 크기가 늘고, 이 저장소의 관례상 새 의존은 버전 카탈로그·컨벤션
  플러그인 작업을 동반한다([ADR-0003](0003-convention-plugins-version-catalog.md)).
  **→ 기각:** 현재 필요한 건 문자열 두 개의 대칭 암호화뿐이라 Keystore 직접 사용으로 충분하다.
- **대안 B — `EncryptedSharedPreferences`** — 토큰 암호화 저장에 가장 흔히 쓰이는 선택지.
  그러나 `androidx.security-crypto` 1.1.0이 alpha 상태로 오래 정체돼 있고, `SharedPreferences` 기반이라
  이 저장소가 로컬 영속화 표준으로 정한 DataStore 관용([ADR-0008](0008-datastore-local-persistence.md))과
  어긋난다.
  **→ 기각:** DataStore 일관성을 유지하고, Keystore를 `CryptoManager`로 직접 감싸는 쪽을 택한다.

## 영향

**긍정**

- 장기 자격증명(refresh token)이 평문으로 기기에 남지 않는다.
- `TokenProvider`가 실제 토큰을 반환해 인증이 필요한 API 호출이 가능해진다([ADR-0017](0017-remote-network-datasource.md) 갱신).
- 저장 매체(DataStore)를 바꿔도 `CryptoManager`(암복호화)는 영향받지 않는다 — 책임 분리.

**트레이드오프**

- Tink 대신 Keystore API를 직접 다루므로 암호화 로직(IV 관리·GCM 태그 크기 등)을 팀이 직접 들고
  있어야 한다.
- 키 유실 경로(`EncryptedTokenStore.read()`의 `clear()` 분기)가 테스트 인프라 부재로 미검증인 채로
  남는다.

**위험·방어**

- 코드베이스에 `test`/`androidTest` 디렉토리가 없고(무테스트 관례) Android Keystore는 JVM 유닛
  테스트에서 동작하지 않아, 이 라운드의 검증은 `:data:compileDebugKotlin`·`ktlintCheck`·
  `:app:assembleDebug`(Hilt 그래프 전체 해소: `CryptoManager`→`EncryptedTokenStore`→`TokenStore`→
  `TokenStoreTokenProvider`→`TokenProvider`→`AuthInterceptor` 체인)로 한정했다. **실기기 암복호화
  왕복 검증(저장 → 앱 완전 종료 → 재시작 → 읽기)은 사람이 수행하면 된다고 봤으나 수행 불가였다** —
  `TokenStore.save()` 호출부가 코드베이스에 0건이라 저장을 트리거할 방법이 없다(auth 도메인
  Service·RemoteDataSource·Repository가 이 라운드 범위 밖). 로그인 연동 라운드로 이월한다.
- 키 유실 재현(기기 복원·잠금 자격증명 변경)과 위 실기기 왕복 검증은 이번 라운드에서 검증하지
  않았다 → [open-questions](../synthesis/open-questions.md)로 추적.
