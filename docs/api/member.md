---
id: member
title: 회원(내 계정 조회·전역 닉네임 변경·탈퇴)
server_module: http/member
server_commit: aa9cc9b
verified: 2026-09-04
android_status: done
related_spec: 2026-08-15-parfait-canvas-topping-member-api-service-layer, 2026-08-15-user-info-ssot
related_adr: ADR-0017, ADR-0022
tags: [api, parfait, server-contract, member]
---

# 회원(내 계정 조회·전역 닉네임 변경·탈퇴) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

`[Feat/#66] 전역 닉네임 변경 API (#77)`·`[Feat/#67] 내 계정 정보 조회 API (#84)`로 신설됐고
`feat: 회원 탈퇴(DELETE /api/v1/users/me) API 구현 (#90)`으로 **2 → 3**이 됐다.
셋이 `MemberController` 하나에 있고 클래스 레벨 매핑이 `/api/v1/users/me`다 —
**URL 세그먼트는 `users`인데 서버 패키지·도메인 이름은 `member`다.**

**"전역 닉네임"은 계정 1개당 1개**이고, 그룹 안에서 쓰는 닉네임([parfait-group.md](parfait-group.md)의
`groupNickname`)과 다른 값이다. 계정 생성 시 서버가 `RandomNicknameGenerator.generate()`로 자동
부여하고([auth.md](auth.md) `signup`), 이 API가 그 값을 바꾼다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/v1/users/me` | 필요 | 없음 | `MyAccountResponse` | 구현됨·결선됨 |
| PATCH | `/api/v1/users/me/nickname` | 필요 | `ChangeGlobalNicknameRequest` | `ChangeGlobalNicknameResponse` | 구현됨·결선됨 |
| DELETE | `/api/v1/users/me` | 필요 | 없음 | **본문 없음(204)** | 구현됨·결선됨 |

셋 다 `SecurityConfig.WHITELIST_PATHS`에 없어 **access token이 필요하다**. memberId는 요청이 아니라
토큰에서 나온다(`Authentication.memberId(): Long = name.toLong()`) — 남의 계정을 지정할 경로가 없다.

## 엔드포인트 상세

### GET /api/v1/users/me

- **인증**: 필요.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드**: 없음(경로 변수·쿼리·바디 전부 없음)
- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `memberId` | Long | 아니오 | 토큰에서 꺼낸 값을 그대로 되돌려준다 |
| `provider` | String | 아니오 | `LoginProvider` 이름 문자열. **core enum은 `KAKAO`·`APPLE` 2종** — 아래 참고 |
| `nickname` | String | 아니오 | 전역 닉네임(`Member.globalNickname`) |

  ⚠️ **영속 계층 `LoginProvider`에는 `GOOGLE`이 있는데 core enum에는 없다.** `MemberAdapter.toCoreProvider`가
  `GOOGLE`에서 `error(...)`로 `IllegalStateException`을 던지고, `GlobalExceptionHandler`의 `Exception`
  핸들러가 이를 **500 `INTERNAL_SERVER_ERROR`**로 바꾼다. 구글 로그인 회원 행이 생기는 순간 이 조회가
  깨진다는 뜻이다 — 현재 구글 로그인 경로 자체가 없어 도달하지 않는다 → [미결](#미결).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `UNAUTHORIZED` | 인증 헤더 없음·`Bearer` 아님(`AuthErrorCode`, 전역) |
| 401 | `INVALID_TOKEN` · `EXPIRED_TOKEN` | access token 검증 실패(`AuthErrorCode`, 전역 `JwtAuthFilter`) |
| 401 | `MEMBER_NOT_FOUND` | 토큰의 `memberId` 회원 부재(`AuthErrorCode`, 전역 `JwtAuthFilter`) |
| 404 | `MEMBER_NOT_FOUND` | 조회 대상 회원 부재(`MemberErrorCode`, `MemberService.getMyAccount`) |

  ⚠️ **같은 엔드포인트에서 `MEMBER_NOT_FOUND`가 401과 404 둘 다로 나갈 수 있다.** 앞의 것은
  `JwtAuthFilter.authenticate`가 `MemberQueryPort.existsById`로 컨트롤러 도달 **전에** 던지고, 뒤의 것은
  `MemberService`가 `findAccountById`가 `null`일 때 던진다. **실무상 401이 항상 먼저 걸리므로 404 분기는
  두 검사 사이에 회원이 사라진 경우에만 도달한다** — 2026-08-15 탈퇴 API가 생기면서 그 "사라짐"이 실제로
  가능해졌다(다른 세션이 탈퇴를 마친 직후). 소비 측이 `code` 문자열로만
  분기하면 두 상황이 한 브랜치로 뭉개진다 → [conventions.md](conventions.md) "코드 문자열은 enum 간
  유일하지 않다". 근거: `MemberControllerTest`가 404 케이스를 직접 검증하고, 401 케이스는
  `SecurityConfigIntegrationTest`가 전역으로 검증한다.

### PATCH /api/v1/users/me/nickname

- **인증**: 필요.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `nickname` | String | 필수(`@NotBlank`) | 바꿀 전역 닉네임. 규칙은 아래 |

  **유효성은 `GlobalNickname.of`가 판정한다** — 1~15자, 패턴
  `^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$`
  (완성형 한글·**자모 단독**·영문·숫자, 단어 사이 한 칸 공백 허용, 앞뒤·연속 공백 불가).
  위반은 400 `INVALID_NICKNAME`이다.

  🔁 **2026-08-15 — 자모 범위가 추가됐다**(`fix: 그룹/전역 닉네임 자음 모음 단독 입력 허용`). `ㅋㅋ`·`ㅠㅠ`가
  이제 통과한다. 사유는 iOS 클라이언트가 통과시키는 값이 서버에서만 400으로 튕기던 것이다. **앱은 반대로
  완성형만 허용하도록 좁혀 둔 상태**(`CheckNameValidUseCase`)라 지금은 앱이 서버보다 좁다
  → [미결](#미결).

  📌 **그룹 닉네임 규칙과 문자 그대로 같다.** `core/parfaitgroup/domain/GroupNickname`도 `MAX_LENGTH = 15`에
  같은 정규식을 쓰고 자모 추가도 같은 커밋에서 함께 이뤄졌다 — 다른 것은 던지는 에러 코드
  (`INVALID_GROUP_NICKNAME`)와 `GroupNickname.unknown()` 센티널 값의 존재뿐이다. 이 대조가
  [parfait-group.md](parfait-group.md)에 오래 걸려 있던 미결을 닫는다.

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `nickname` | String | 아니오 | 저장된 값. 요청 값을 `GlobalNickname.of`에 통과시킨 결과이므로 요청과 같다 |

  **저장 범위**: `MemberService.change`가 `@Transactional`로 `Member.globalNickname` 한 컬럼을 갱신한다.
  ⚠️ **이미 참여한 그룹의 `groupNickname`은 바뀌지 않는다** — 그룹 닉네임은 별도 컬럼이고 이 API가
  건드리지 않는다(그룹 닉네임 변경은 [parfait-group.md](parfait-group.md)의 전용 엔드포인트다).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | `nickname` 필드 부재·빈 문자열(`@NotBlank` 위반 → `CommonErrorCode`) |
| 400 | `INVALID_NICKNAME` | 길이·문자 패턴 위반(`MemberErrorCode`, `GlobalNickname.of`) |
| 404 | `MEMBER_NOT_FOUND` | 갱신 대상 회원 부재(`MemberErrorCode`, `MemberAdapter.updateGlobalNickname`) |
| 401 | `UNAUTHORIZED` · `INVALID_TOKEN` · `EXPIRED_TOKEN` · `MEMBER_NOT_FOUND` | 전역 인증(`AuthErrorCode`) |

  **빈 문자열과 형식 위반이 다른 코드로 갈린다.** `""`는 `@NotBlank`에 걸려 `INVALID_REQUEST`,
  `"연속  공백"`은 애노테이션을 통과한 뒤 `GlobalNickname.of`에서 `INVALID_NICKNAME`이 된다.
  근거: `MemberControllerTest`가 네 케이스(정상·빈 문자열·필드 부재·형식 위반)를 직접 검증한다.

### DELETE /api/v1/users/me

`feat: 회원 탈퇴(DELETE /api/v1/users/me) API 구현 (#90)`으로 신설됐다.

- **인증**: 필요.
- **성공**: HTTP **204 No Content** · **본문 없음**(`@ResponseStatus(HttpStatus.NO_CONTENT)`, 반환 타입 `Unit`).

  ⚠️ **envelope를 쓰지 않는 성공 응답 셋 중 하나다** — 로그아웃([auth.md](auth.md))과 기기 토큰
  등록([notification.md](notification.md), 2026-09-02 신설)이 나머지 둘이다.
  🔁 **신설 당시에는 이 응답이 유일한 예외였다** — 그 뒤 같은 모양이 하나 늘어 "유일하다"는 서술은
  더 이상 맞지 않는다. 나머지 전 엔드포인트는 `ApiResponse<T>`를 감싸
  `success`·`code`·`message`·`data`를 준다([conventions.md](conventions.md)).
  같은 delta의 토핑 삭제(`DELETE .../images/{parfaitImageId}`)조차 200 + `data: null`인데
  ([parfait-image.md](parfait-image.md)) 이쪽은 본문이 통째로 없다. **envelope를 무조건 파싱하는 클라이언트는
  이 응답에서 깨진다** → [미결](#미결).

- **요청 필드**: 없음(경로 변수·쿼리·바디 전부 없음)
- **응답 필드**: 없음

  **처리 순서**(`MemberService.withdraw`, `@Transactional`):
  ① 회원이 없으면 **아무것도 하지 않고 그대로 성공**(`existsById`가 false면 조기 반환) —
  즉 **멱등이고 404를 내지 않는다**. ② 회원 행 삭제. ③ 참여 중인 그룹 멤버십 전부를 `leave()` 처리.
  ④ **커밋 이후**(`TransactionSynchronization.afterCommit`) refresh token 전량 삭제
  (`TokenDeletePort.deleteAllByMemberId` — Redis). ④가 실패하면 로그 경고만 남기고 탈퇴는 유지된다.
  ⑤ **같은 `afterCommit` 블록에서 기기(FCM) 토큰 전량 삭제**(`DeviceTokenDeletePort.deleteAllByMemberId`
  — DB, 2026-09-02 신설). **④와 별개의 `runCatching`으로 감싸므로 한쪽이 실패해도 나머지 정리와 탈퇴
  자체는 진행된다.** 로그아웃이 **세션 단위**로만 지우는 것과 달리 탈퇴는 **회원 단위**로 전부 지운다
  — 탈퇴는 그 회원의 모든 세션을 정리하기 때문이다([notification.md](notification.md)).
  요청·응답·상태 코드는 이 delta에서 바뀌지 않았다.

  **④를 `afterCommit`에 둔 이유가 코드 주석에 있다** — 메서드 본문에서 지우면 이후 커밋이 실패했을 때
  DB는 롤백됐는데 Redis만 비는 상태가 되기 때문이다. 같은 delta의 토핑 삭제는 이 방어 없이 트랜잭션
  안에서 S3를 지운다([parfait-image.md](parfait-image.md)) — **한 저장소 안에서 판단이 갈렸다.**

  ⚠️ **회원 행은 하드 삭제다.** `MemberAdapter.deleteById`가 `providerUserId`를 `withdrawn_<memberId>`로
  덮어 저장한 뒤 `delete`한다. 회원 이력은 남지 않는다.

  🔁 **2026-08-19 — 그 rename이 실제로는 DB에 안 닿고 있었다**(`fix: 회원 탈퇴 후 재가입 시 유니크 제약
  위반으로 500 발생하는 문제 수정`). 같은 트랜잭션에서 rename과 delete가 한 번에 flush되면 Hibernate가
  **곧 삭제될 엔티티라는 이유로 그 UPDATE를 dirty-check에서 건너뛴다.** `provider_user_id`가 원래 값
  그대로 남아, **같은 계정으로 다시 가입하면 유니크 제약 위반으로 500**이 났다. 지금은 rename 직후
  `memberRepository.flush()`로 삭제보다 먼저 반영한다. **계약(요청·응답·상태 코드)은 안 바뀌었고
  바뀐 것은 재가입 성공 여부**다 — 카카오·애플 로그인의 신규 가입 경로([auth.md](auth.md))가 이 수정의
  수혜자다. 근거: `SignupAfterWithdrawIntegrationTest`가 단일 트랜잭션 안에서 재현·검증한다.

  ⚠️ **탈퇴해도 올려 둔 토핑은 남는다.** 그룹 멤버십은 `leftAt`이 찍히고 `groupNickname`이
  `GroupNickname.unknown()`(`(알수없음)`)으로 바뀌지만, 그 멤버가 배치한 `parfait_image` 행은 그대로다.
  `GET .../parfaits/today`의 `placedBy.nickname`이 `(알수없음)`으로 내려오고, 그 `groupMemberId`는
  `groupMembers` 목록에 없다([parfait.md](parfait.md)) → [미결](#미결).
  `GroupNickname.of`가 이 센티널 값만 검증을 건너뛰도록 특례가 붙은 것이 그 때문이다
  (`fix: 탈퇴 멤버 닉네임 재구성 시 GroupNickname 검증 실패 수정`).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 401 | `UNAUTHORIZED` · `INVALID_TOKEN` · `EXPIRED_TOKEN` · `MEMBER_NOT_FOUND` | 전역 인증(`AuthErrorCode`) |

  **도메인 에러가 없다.** 회원 부재도 성공(204)이라 404 경로가 없다. 근거: `MemberControllerTest`
  ("탈퇴는 인증 회원 id를 그대로 유스케이스에 전달하고 204를 반환한다").

  ⚠️ **애플 연동 해제(revoke)는 하지 않는다.** 같은 delta의
  `refactor: 애플 로그인 authorizationCode 교환 로직 제거 (#89)`가 애플 refresh token 보관을 통째로
  걷어냈고([auth.md](auth.md)), 탈퇴 로직에도 애플 API 호출이 없다. 애플 계정으로 가입한 회원이 탈퇴해도
  **애플 쪽 연동은 끊기지 않는다** → [미결](#미결).

## 도메인 에러 코드 전수

`MemberErrorCode`(`core/member/exception`) 2종 전부.

| HTTP | code | message |
|---|---|---|
| 400 | `INVALID_NICKNAME` | 닉네임 형식이 올바르지 않습니다 |
| 404 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |

⚠️ **`MEMBER_NOT_FOUND`를 가진 네 번째 enum이다.** `AuthErrorCode`(401) · `ParfaitGroupApiErrorCode`(404) ·
`ImageErrorCode`(404) · `MemberErrorCode`(404) → [conventions.md](conventions.md).

## Android 매핑

**세 엔드포인트 전부 표면이 있고 소비처도 전부 있다** — 표면은 2026-08-12 PR #230 두 건 + 2026-08-15
PR #250(탈퇴), 소비처는 2026-08-16 PR #263(조회·닉네임 변경) + 2026-08-19 PR #306(탈퇴)이다.

| 계약 | Android 심볼 |
|---|---|
| `GET /api/v1/users/me` | `MemberService.getUsersMe` → `MemberRemoteDataSource.getMyAccount()` |
| `PATCH /api/v1/users/me/nickname` | `MemberService.patchUsersMeNickname` → `MemberRemoteDataSource.changeGlobalNickname(nickname)` |
| `DELETE /api/v1/users/me` | `MemberService.deleteUsersMe` → `MemberRemoteDataSource.withdraw()` |

**204 문제는 새 진입점 없이 풀렸다.** `deleteUsersMe`는 `ApiResponse`가 아니라 `Unit`을 반환하고
DataSource가 `ApiCaller.safeApiCallNoContent`로 호출한다 — `logout`에 이어 **두 번째 소비처**다.
같은 delta의 토핑 삭제(200 + `data: null`)가 `safeApiCallWithoutData`로 갈린 것과 짝이다
([parfait-image.md](parfait-image.md)). 설계 근거는
[specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer](../superpowers/specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md).

✅ **탈퇴에도 소비처가 생겼다(2026-08-19, PR #306).** S-001 앱 설정의 탈퇴 확인 팝업이
`WithdrawUseCase`를 부른다. **이 UseCase의 판단은 순서다** — 서버가 받아 준 뒤에야 기기를 정리하고,
거절당하면 아무것도 지우지 않는다. 로그아웃과 반대인데(그쪽은 서버 호출이 실패해도 로컬을 지운다),
탈퇴는 서버가 거절하면 계정이 살아 있어 로컬만 지우면 사용자가 탈퇴했다고 믿는 상태가 남기 때문이다.
지우는 일 자체는 `LogoutUseCase`에 위임한다 — "무엇을 지우는가"가 거기 한 곳이다.

회원이 없어도 204라 **멱등이고 도메인 에러가 없어서**, 앱은 "이미 탈퇴됨"을 성공과 구분하지 않는다
(구분할 필요가 아직 서지 않았다 → [open-questions](../synthesis/open-questions.md) OQ-P-162 ③).
실패는 401 계열과 네트워크·서버 장애뿐이라 화면이 문구 하나로 받는다.

⚠️ **성공 뒤에 죽은 토큰으로 요청이 한 번 더 나간다** — 위임받은 `LogoutUseCase`가 서버 로그아웃을
부르는데 그 계정은 방금 지워졌다. 서버는 401 `MEMBER_NOT_FOUND`를 주고, 그 401이 `TokenAuthenticator`의
재발급을 깨우며, refresh token은 탈퇴가 이미 지웠으므로 재발급도 거절돼 `ForcedLogout`까지 발행된다
→ [open-questions](../synthesis/open-questions.md) OQ-P-242.

wire DTO는 `service/model/{request,response}/member/`, 변환은 `source/member/mapper/VOMapper.kt`,
domain은 `domain/model/member/`(`MyAccountVO`·`GlobalNickname`·`LoginProvider`)다. 설계 근거는
[specs/archive/2026-08-11-member-parfait-image-api-service-layer](../superpowers/specs/archive/2026-08-11-member-parfait-image-api-service-layer.md).

세 가지가 이 도메인 특유의 결정이다.

- **`GlobalNickname`을 `GroupNickname`과 합치지 않았다.** 서버 유효성 규칙은 문자 그대로 같지만
  저장 위치와 에러 코드(`INVALID_NICKNAME` vs `INVALID_GROUP_NICKNAME`)가 다르고, 합치면 전역 닉네임을
  그룹 API에 넘기는 실수가 컴파일을 통과한다.
- **`LoginProvider`에 `UNKNOWN` 폴백을 뒀다.** 매퍼가 `enumValueOf`가 아니라 `when` 분기라, 영속
  `GOOGLE`이 core enum에 들어오더라도 앱이 크래시하지 않는다(그 회원 조회가 서버에서 500이 나는 것은
  별개 문제다 → 아래 "미결").
- **닉네임 변경 반환은 `Result<GlobalNickname>`이고 VO가 없다** — 응답 필드가 하나뿐이라 감쌀 것이 없다.

✅ **소비처가 붙었다(2026-08-16, PR #263).** 두 엔드포인트가 **로컬 SSoT를 사이에 두고** 소비된다 —
화면은 조회 API를 직접 부르지 않고 `MemberRepository.myAccount: Flow<MyAccountVO?>`를 구독하며,
서버 조회는 **로그인·가입 직후 / 앱 진입(스플래시 부트스트랩) / 닉네임 변경 성공** 세 시점뿐이다
([ADR-0022](../adr/0022-user-info-local-ssot.md) ·
[스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md)).

| 계약 | 앱 쪽 경로 |
|---|---|
| `GET /api/v1/users/me` | `MemberRepositoryImpl.refreshMyAccount()` → 암호화 로컬 저장 → `GetMyAccountFlowUseCase` 구독(S-001·S-002·**G-001**(#312 — 그리지 않고 A-005로 넘길 인자로만 쓴다)). 부트스트랩(`BootstrapSessionUseCase`)이 **세션 검증도 이 호출로 겸한다** |
| `PATCH /api/v1/users/me/nickname` | S-002 확인 → `ChangeGlobalNicknameUseCase` → 응답 값으로 로컬 갱신(낙관적 갱신 없음). 로컬이 비어 있으면 `GET`으로 폴백해 SSoT를 채운다 |

**이 도메인의 에러 코드 둘이 앱에서 서로 다른 두 소비자에게 다르게 읽힌다.**
`INVALID_NICKNAME`·`MEMBER_NOT_FOUND`가 `:domain`의 `ServerErrorCode.Member`로 들어왔고,
S-002는 둘을 표시용 갈래(`GlobalNicknameError.INVALID`·`ACCOUNT_GONE`)로 바꿔 **표시만** 하는 반면
`BootstrapSessionUseCase`는 `MEMBER_NOT_FOUND`를 **세션 사망**(401과 동급)으로 보고 토큰·계정 정보를
지운다. 같은 코드에 처분이 갈리는 것은 의도다 — 화면이 세션을 파괴하는 경로를 새로 열지 않고, 죽은
세션은 다음 앱 진입의 부트스트랩이 걷어낸다.

✅ **탈퇴가 셋째 소비처다(2026-08-19, PR #306).** 앞의 둘과 달리 **로컬 SSoT를 거치지 않는다** —
읽어서 화면에 그릴 값이 없고 지우는 일만 남기 때문이다.

| 계약 | 앱 쪽 경로 |
|---|---|
| `DELETE /api/v1/users/me` | S-001 탈퇴 확인 → `WithdrawUseCase` → 성공 시 `LogoutUseCase`(토큰·계정 정보 정리) → `replaceAll(NavKeyLogin)` / 실패 시 화면을 떠나지 않고 토스트 |

요청 중에는 `YGScaffoldV2` 로딩 오버레이가 화면을 덮고(`AppSettingState.isLoading`이
`isLoggingOut`·`isWithdrawing`의 OR), `launch(key)` 가드가 **되돌릴 수 없는 요청이 두 번 나가는 것**을
막는다. 확인 팝업은 요청 전에 닫는다 — S-101 나가기·신고가 먼저 확정한 형태를 그대로 따른다.

`http/users.http`가 세 요청을 덮는다(선행은 `auth.http`만). ⚠️ **마지막 요청이 탈퇴라 파일을 위에서부터
통째로 돌리면 계정이 지워진다** — `http/README.md`가 이 경고를 담는다.

## 미결

- 영속 `LoginProvider.GOOGLE`이 core enum에 없어 해당 회원 조회가 500이 된다 — 구글 로그인을 뺄지
  core enum에 넣을지 → [open-questions](../synthesis/open-questions.md)
- 전역 닉네임을 바꿔도 기존 그룹의 `groupNickname`이 그대로인 것이 의도인지(앱은 두 값을 다른 화면에
  보여줘야 한다) → [open-questions](../synthesis/open-questions.md)
- 탈퇴 응답만 envelope 없는 204다 — 서버가 맞출지, 클라이언트가 예외 분기를 둘지
  → [open-questions](../synthesis/open-questions.md)
- 탈퇴 회원이 남긴 토핑의 `placedBy`가 `(알수없음)`으로 캔버스에 계속 보인다 — 표시 정책이 없다
  → [open-questions](../synthesis/open-questions.md)
- 애플 계정 탈퇴 시 애플 연동 해제(revoke)를 하지 않는다 → [open-questions](../synthesis/open-questions.md)
- ✅ **자모 단독 허용 불일치는 해소됐다**(2026-08-15, PR #250) — `CheckNameValidUseCase`의 허용 문자에
  `'ㄱ'..'ㅎ'`·`'ㅏ'..'ㅣ'`가 더해져 앱과 서버 집합이 다시 같다. **다만 정책 근거는 여전히 서버 커밋
  메시지뿐이고** 위키 [[이름-입력-규칙]]은 "한글"의 범위를 정하지 않는다
  → [open-questions](../synthesis/open-questions.md)
