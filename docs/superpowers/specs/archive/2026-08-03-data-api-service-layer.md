---
id: data-api-service-layer
title: :data API Service·remote DataSource 레이어 (14 엔드포인트)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-06
related_code: AuthService, PolicyService, ParfaitGroupService, ParfaitService, ApiCaller, NoAuth, ServiceModule, RemoteDataSourceModule
related_adr: ADR-0017, ADR-0019, ADR-0016
related_spec: 2026-08-02-network-envelope-token-storage
related_architecture: data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, data, network, api]
---

# :data API Service·remote DataSource 레이어

서버 계약([api/README.md](../../../api/README.md), 기준선 `69654bc`)의 **14개 엔드포인트 전량**을
`:data`의 Retrofit Service와 remote DataSource로 구현하고, 대응 domain VO를 만든다.
네트워크 기반 구조는 이미 있다([ADR-0017](../../../adr/0017-remote-network-datasource.md),
[2026-08-02-network-envelope-token-storage](2026-08-02-network-envelope-token-storage.md)) —
이 스펙은 그 위에 **실제 API 표면**을 올린다.

## 범위

**포함** — Service 4개 · request/response DTO · remote DataSource 4개 · VO mapper · domain VO/value class ·
DI 등록 · 구조 예시용 `Temp*` 제거.

**제외** — Repository 구현과 `domain/repository` 인터페이스, UseCase, 화면 결선, 401 자동 재발급,
카카오 SDK 연동, 토큰 저장 흐름 변경(`TokenStore`·`TokenProvider` 무수정).

## 계층과 배치

`Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`.

```
data/service/
├── AuthService.kt · PolicyService.kt · ParfaitGroupService.kt · ParfaitService.kt
└── model/
    ├── request/{auth,group}/
    └── response/{auth,policy,group,parfait}/   + ApiResponse.kt(기존, 무수정)

data/source/{auth,policy,group,parfait}/
├── remote/XxxRemoteDataSource.kt + XxxRemoteDataSourceImpl.kt
└── mapper/VOMapper.kt

domain/model/{auth,policy,group,id}/     (parfait 연도는 List<Int>라 VO 없음)
```

Service 4개는 계약 문서와 1:1이다 — `auth.md`·`policy.md`·`parfait-group.md`·`parfait.md`.
분할 기준은 서버 패키지가 아니라 **URL 세그먼트**다. `PolicyController`는 서버에서 `http/auth`
패키지에 있고 OpenAPI 태그도 `Auth`지만 경로가 `/api/v1/policies`라 `AuthService`에 합치지 않는다
(계약 문서가 `policy.md`를 따로 둔 것과 같은 근거).

`domain/model/`은 현재 평면 10개다. 9개 이상이 더 붙으면 도메인 구분이 사라져 하위 패키지로 나눈다.
DI는 ADR-0017대로 **역할 파일에 추가만** 한다(`ServiceModule`·`RemoteDataSourceModule`, 하위 패키지 없음).

## Service 함수 이름 규칙

**`<method><경로 세그먼트 PascalCase>`**, 경로 변수는 `By<파라미터명>`, `/api`·`/v1` 접두사는 생략.

전송 계층은 계약을 그대로 비추고, 의미 부여는 DataSource가 한다. DataSource를 읽을 때는 Retrofit
애노테이션이 보이지 않으므로 **이름이 경로를 말해주는 값**이 거기서 생긴다.

| Service 함수 | HTTP | 경로 |
|---|---|---|
| `AuthService.postAuthKakao` | POST | `/api/v1/auth/kakao` |
| `AuthService.postAuthSignup` | POST | `/api/v1/auth/signup` |
| `AuthService.postAuthReissue` | POST | `/api/v1/auth/reissue` |
| `AuthService.postAuthLogout` | POST | `/api/v1/auth/logout` |
| `PolicyService.getPolicies` | GET | `/api/v1/policies` |
| `ParfaitGroupService.getParfaitGroups` | GET | `/api/parfait-groups` |
| `ParfaitGroupService.getParfaitGroupsByGroupId` | GET | `/api/parfait-groups/{groupId}` |
| `ParfaitGroupService.getParfaitGroupsJoinPreview` | GET | `/api/parfait-groups/join-preview` |
| `ParfaitGroupService.postParfaitGroupsJoin` | POST | `/api/parfait-groups/join` |
| `ParfaitGroupService.postParfaitGroups` | POST | `/api/parfait-groups` |
| `ParfaitGroupService.patchParfaitGroupsByGroupIdNickname` | PATCH | `/api/parfait-groups/{groupId}/nickname` |
| `ParfaitGroupService.deleteParfaitGroupsByGroupIdMembersMe` | DELETE | `/api/parfait-groups/{groupId}/members/me` |
| `ParfaitGroupService.postParfaitGroupsByGroupIdReports` | POST | `/api/parfait-groups/{groupId}/reports` |
| `ParfaitService.getGroupsByGroupIdParfaitsYear` | GET | `/api/v1/groups/{groupId}/parfaits/year` |

규칙에서 파생되는 두 가지를 못박아 둔다.

- **`Signup`이지 `SignUp`이 아니다.** 경로 세그먼트가 `signup` 한 단어다. 규칙이 경로를 그대로 옮기는
  것이므로 여기에 캐멀을 끼우면 규칙이 흔들린다.
- **`parfait-groups`를 `Groups`로 줄이지 않는다.** 서버가 `/api/parfait-groups`와
  `/api/v1/groups/{groupId}/parfaits/year` 두 경로를 실제로 쓴다. 줄이면 그룹 목록과 연도 조회가 같은
  `Groups`를 말하면서 서로 다른 base path를 쳐서, 이 규칙이 없애려던 모호함이 되돌아온다.

`deleteParfaitGroupsByGroupIdMembersMe`가 37자다. 규칙을 지킨 대가로 받아들인다.

base URL은 호스트까지만 두고(`BuildConfig.BASE_URL`) 각 메서드가 전체 상대경로를 갖는다.
도메인마다 버전 프리픽스 유무가 다르기 때문이다.

## 계약이 던지는 함정

기계적 매핑으로 풀리지 않는 여섯 지점. 각각 근거와 조치를 함께 적는다.

### 1. `newUser` 키 변환 — 조용히 틀리는 유일한 건

> 🔁 **2026-08-11 정정 — 이 절의 전제가 틀렸다.** 실제 JSON 키는 **`isNewUser`**다(서버가
> `jackson-module-kotlin`을 쓰므로 `is` 접두사가 남는다). 아래 서술은 당시 계약 문서를 그대로 따른
> as-built 기록으로 남기고, **정본은** [../../api/auth.md](../../../api/auth.md) "판별자 키"·
> [../../api/conventions.md](../../../api/conventions.md) "직렬화 규약"이다. 이 절대로 구현된
> `@SerialName("newUser")`는 **수정 대상**이다 → [open-questions](../../../synthesis/open-questions.md).

서버 `KakaoLoginResponse`는 Kotlin `val isNewUser: Boolean`인데 Jackson이 `is` 접두사를 떼어
**JSON 키는 `newUser`**로 나간다([conventions.md](../../../api/conventions.md) "직렬화 규약").

DTO 프로퍼티는 `isNewUser`로 두고 `@SerialName("newUser")`를 붙인다. **붙이지 않으면 예외가 나지
않는다** — `JsonModule.provideRemoteJson`이 `coerceInputValues = true`라 없는 키가 기본값으로 떨어지고,
신규 유저가 기존 회원으로 분기해 존재하지 않는 `accessToken`을 꺼낸다. 이 프로젝트의 `Json` 설정이
실패를 삼키는 쪽이라 다른 필드보다 위험하다.

### 2. 로그아웃 204 — envelope 자체가 없다

`AuthService.postAuthLogout`의 반환 타입은 `Unit`이다(`ApiResponse<Unit>`이 아니다).
`ApiCaller.safeApiCallNoContent`로 감싼다. `safeApiCallWithoutData`는 envelope 파싱을 전제하므로
본문 없는 응답에서 깨진다 — 진입점이 3개인 이유가 이것이다(ADR-0017 as-built).

### 3. `@NoAuth` 4곳

`postAuthKakao`·`postAuthSignup`·`postAuthReissue`·`getPolicies`.
`postAuthLogout`에는 **붙이지 않는다** — 서버 `SecurityConfig.WHITELIST_PATHS`에서 유일하게 제외돼
access token이 필요한 엔드포인트다. 그룹·파르페 9종도 전부 인증 대상이라 붙이지 않는다.

`@NoAuth`가 R8 release 빌드에서 유지되는지는 ADR-0017이 남긴 열린 질문이었고, 이 스펙이 사용처를
4곳으로 늘려 위험 표면이 커진다는 우려도 그대로였다 — 실제로 **부정적으로 확인되고 고쳐졌다**
(as-built 정정, 아래 [As-built 이탈](#as-built-이탈) 참고).

### 4. 성공 코드 2종 — 추가 작업 없음

`postAuthSignup`·`postParfaitGroups`·`postParfaitGroupsByGroupIdReports`가 201 `CREATED`, 나머지는
200 `OK`다. `ApiCaller`가 `code` 문자열이 아니라 **`success` 필드**로 판정하므로 DTO·DataSource는
201을 특별 취급하지 않는다. 명시적으로 적어 두는 이유는, 나중에 성공 판정을 코드 문자열로 되돌리면
201 응답 3건이 전부 실패로 분류되기 때문이다.

### 5. `MEMBER_NOT_FOUND` 중복 — 이번 범위에서는 번역하지 않는다

같은 코드 문자열이 `AuthErrorCode`에서 **401**, `ParfaitGroupApiErrorCode`에서 **404**다.
`ApiException.Business`가 이미 `statusCode`를 들고 있으므로 소비 측이 code + status로 판정할 수 있다.

이번 라운드는 실패를 VO로 번역하지 않고 `Result.failure(ApiException)`를 그대로 올린다. 표시 문자열
매핑은 [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md)대로 `core:ui` 몫이고
화면을 붙일 때 결정한다.

### 6. `recentImageUploadedAt` — UTC로 읽으면 9시간 어긋난다

서버가 `"2026-08-01T12:00:00"`(오프셋 없음)을 준다. 실제 기준은 **Asia/Seoul 벽시계**다 —
DB 커넥션과 Hibernate가 세 환경 모두 그 타임존으로 맞춰져 있다([parfait-group.md](../../../api/parfait-group.md)).

DTO는 `String?`으로 받고 mapper가 `kotlinx.datetime.LocalDateTime.parse()`로 변환해 VO는
`LocalDateTime?`을 갖는다. 프로젝트가 이미 kotlinx-datetime을 쓴다(`DayWindow`·`DateFormat`).
mapper에 타임존 근거를 주석으로 남긴다.

파싱 실패는 삼키지 않는다 — `ApiCaller#safeApiCall(block, transform)`이 `block`과 `transform`을
같은 가드 안에서 실행해 `ApiException.Unknown`으로 잡는다(as-built 정정, 아래
[As-built 이탈](#as-built-이탈) 참고).

## domain 타입

### value class

서로 바꿔 넣으면 안 되는 값은 타입으로 가른다. 필드 하나짜리 `data class` 래퍼를 만들지 않는다.

```kotlin
// domain/model/id/
@JvmInline value class GroupId(val value: Long)
@JvmInline value class MemberId(val value: Long)
@JvmInline value class TermsId(val value: Long)
@JvmInline value class ReportId(val value: Long)

// domain/model/auth/
@JvmInline value class AccessToken(val value: String)
@JvmInline value class RefreshToken(val value: String)
@JvmInline value class RegistrationToken(val value: String)

// domain/model/group/
@JvmInline value class InviteCode(val value: String)
@JvmInline value class GroupName(val value: String)
@JvmInline value class GroupNickname(val value: String)
```

**토큰 3종이 가장 값을 한다.** 셋 다 `String`이고 절대 섞이면 안 되는데, 이 프로젝트에는 실제로
"reissue에 access token을 붙여 재발급이 막힌" 사고가 기록돼 있다(`http/README.md`). 컴파일러가 막을
수 있는 종류의 버그다.

**경계는 domain 계층까지다.** `service/` DTO·Retrofit 시그니처·`TokenStore`는 raw 타입을 유지하고
mapper가 감싸고 벗긴다. 이유 둘 — ① `@Serializable` DTO에 value class를 쓰면 인코딩 형태가 객체가
아니라 값으로 바뀌어 서버 계약을 흔든다, ② Retrofit `@Path`/`@Query` 파라미터의 value class는 Kotlin
이름 맹글링과 리플렉션이 얽히는 미검증 영역이라 계약 경계에 둘 이유가 없다.

`idToken`·`nonce`는 raw `String`이다. 카카오 SDK가 만드는 값이라 서버 발급 토큰 3종과 성격이 다르다.
`memberLimit`(Int)·`reason`(String)도 raw다.

`expiresIn`은 `kotlin.time.Duration`으로 받는다(이미 value class다). 서버가 **초 단위 Long**을 주므로
mapper가 `.seconds`로 변환한다 — 단위가 타입에 실려 소비 측이 밀리초로 오해할 여지가 없어진다.

박싱 하나를 명시해 둔다: `Result<GroupId>`처럼 제네릭 인자로 쓰면 value class는 박싱된다.
네트워크 호출당 한 번이라 무시할 수준이지만 "제로 코스트"라고 적지는 않는다.

### VO

접미사는 기존 `TempVO` 관례대로 `*VO`.

```kotlin
// auth
sealed interface KakaoLoginVO {
    data class ExistingMember(val session: AuthSessionVO) : KakaoLoginVO
    data class NewUser(val registrationToken: RegistrationToken) : KakaoLoginVO
}
data class AuthSessionVO(accessToken: AccessToken, refreshToken: RefreshToken, expiresIn: Duration)
data class TermsAgreement(termsId: TermsId, agreed: Boolean)

// policy
data class PolicyVO(termsId: TermsId, type: PolicyType, title: String, url: String, required: Boolean)
enum class PolicyType { TERMS_OF_SERVICE, PRIVACY_POLICY, UNKNOWN }

// group
data class MyParfaitGroupVO(groupId, groupName, recentImageUrl: String?, recentImageUploadedAt: LocalDateTime?)
data class ParfaitGroupDetailVO(groupId, groupNickname, inviteCode, members: List<ParfaitGroupMemberVO>)
data class ParfaitGroupMemberVO(memberId: MemberId, groupNickname: GroupNickname)
data class JoinedGroupVO(groupId: GroupId, groupName: GroupName)
data class CreatedGroupVO(groupId, groupName, inviteCode, memberLimit: Int)
data class GroupNicknameVO(groupId: GroupId, groupNickname: GroupNickname)
data class ReportedGroupVO(groupId: GroupId, reportId: ReportId)
```

**`KakaoLoginVO`는 sealed다.** 서버가 판별자 하나로 두 묶음을 갈라 보내고 4필드가 전부 nullable이다.
mapper가 `newUser`를 읽어 분기하므로 nullable 불확실성이 `:data` 경계에서 끝나고, 화면은 `when`으로
받는다. 서버가 약속을 깨면 `EmptyBody`가 아니라 매핑 지점에서 `ApiCaller#safeApiCall(block, transform)`의
가드에 잡힌다(as-built 정정, 아래 [As-built 이탈](#as-built-이탈) 참고).

`signup`·`reissue` 응답이 같은 3필드라 `AuthSessionVO` 하나를 셋이 공유한다.

⚠️ **`domain/model/KakaoLoginResult`가 이미 있다** — 카카오 **SDK** 로그인 결과(`Success`/`Cancel`/`Failure`)지
서버 응답이 아니다. 이름이 닮아 헷갈리므로 양쪽 KDoc에 서로를 가리키는 한 줄을 남긴다. 기존 쪽 개명은
feature 모듈을 건드리는 일이라 범위 밖이다.

`PolicyType`에 `UNKNOWN`을 두는 이유 — 서버가 약관 종류를 늘려도 앱이 파싱에서 죽지 않는다.
목록은 `List<PolicyVO>`이고 **빈 리스트가 정상값**이다(서버가 200에 빈 배열을 준다, 에러가 아니다).

**VO를 만들지 않는 3곳** — join-preview는 `groupName` 하나, 탈퇴는 `groupId` 하나, 연도 조회는 `years`
하나다. 각각 `GroupName`·`GroupId`·`List<Int>`를 그대로 반환한다. 필드 하나짜리 래퍼는 이름만 있고
내용이 없다. 서버가 필드를 늘리면 그때 VO로 승격하는 편이 싸다.

연도는 `List<Int>`를 유지한다 — `Year` value class는 리스트 원소 전부가 박싱되는데 얻는 구분이 약하다.

## DataSource 시그니처

```kotlin
AuthRemoteDataSource
  loginWithKakao(idToken: String, nonce: String): Result<KakaoLoginVO>
  signup(registrationToken: RegistrationToken, agreements: List<TermsAgreement>): Result<AuthSessionVO>
  reissue(refreshToken: RefreshToken): Result<AuthSessionVO>
  logout(refreshToken: RefreshToken): Result<Unit>

PolicyRemoteDataSource
  getPolicies(): Result<List<PolicyVO>>

ParfaitGroupRemoteDataSource
  getMyGroups(): Result<List<MyParfaitGroupVO>>
  getGroupDetail(groupId: GroupId): Result<ParfaitGroupDetailVO>
  previewJoin(inviteCode: InviteCode): Result<GroupName>
  joinGroup(inviteCode: InviteCode): Result<JoinedGroupVO>
  createGroup(groupName: GroupName, groupNickname: GroupNickname, memberLimit: Int): Result<CreatedGroupVO>
  changeMyNickname(groupId: GroupId, groupNickname: GroupNickname): Result<GroupNicknameVO>
  leaveGroup(groupId: GroupId): Result<GroupId>
  reportGroup(groupId: GroupId, reason: String): Result<ReportedGroupVO>

ParfaitRemoteDataSource
  getYears(groupId: GroupId): Result<List<Int>>
```

Service와 달리 **의미 기반 이름**이다. 인자에도 value class가 들어가 호출부가 순서를 뒤바꿔 넣을 수 없다.

## DI

`ServiceModule`에 `provideAuthService`·`providePolicyService`·`provideParfaitGroupService`·
`provideParfaitService` 4개, `RemoteDataSourceModule`에 대응 `@Binds` 4개를 **추가**한다.
역할 파일 분할은 하지 않는다(ADR-0017).

## Temp 스텁 제거

`TempService`·`TempRequest`·`TempResponse`·`TempRemoteDataSource`(+`Impl`)·`source/temp/mapper/VOMapper`·
`TempVO` 6파일을 삭제하고 `ServiceModule`·`RemoteDataSourceModule`의 바인딩 2줄을 지운다.

파일 주석이 "실제 API 확정 시 삭제"이고 지금이 그 시점이다. 소비자는 0건이다
(`core:designsystem`의 `YGToppingTemplate`은 이름만 비슷할 뿐 무관하다).

## 검증

**테스트를 만들지 않는다** — repo 전체에 `test`·`androidTest` 소스셋이 0개이고, 이번 라운드에
테스트 인프라를 세우지 않기로 했다. 검증 수단은 둘이다.

1. **컴파일** — `:data`·`:domain` 빌드 + Hilt 그래프 해석(`assembleDebug`).
2. **`http/` 요청 파일** — 실제 서버 응답과 DTO 필드명·타입을 눈으로 대조.

⚠️ **런타임 검증 없이 들어간다.** 앱에서는 아직 서버를 호출할 수 없다 — 개발 서버가 평문 HTTP인데
`usesCleartextTraffic`도 `networkSecurityConfig`도 없고, `local.properties`에 `YG_BASE_URL`이 비어 있다
([conventions.md](../../../api/conventions.md) "서버는 평문 HTTP로 서비스된다"). 즉 `@SerialName("newUser")`
누락처럼 **조용히 틀리는 종류의 결함은 이 라운드에서 발견되지 않는다.**

## As-built 이탈

> **2026-08-06 develop 머지(PR #197) 시점 재대조** — Service 함수명 14/14, `@NoAuth` 4곳,
> DTO 전 프로퍼티 `@SerialName`, `logout`만 `safeApiCallNoContent`, VO 미생성 3곳, `Temp*` 6파일 삭제,
> DI 8바인딩까지 설계와 일치했다. 아래 1~3은 브랜치 리뷰 시점의 정정이고, **4~6은 머지 시점
> 재대조에서 새로 확인된 미이행**이다.

최종 전체 브랜치 리뷰(Critical 0 · Important 2 · Minor 3, fix 웨이브 1회로 전량 해소)가 잡은 항목 중
이 스펙의 서술을 직접 정정해야 하는 두 가지.

1. **파르페 연도 응답 DTO 파일명이 `ParfaitResponses.kt`가 아니라 `ParfaitYearsResponse.kt`다.**
   [계층과 배치](#계층과-배치) 절이 domain별 응답 파일을 복수형(`AuthResponses.kt`·
   `PolicyResponses.kt`·`ParfaitGroupResponses.kt`)으로 전제했으나, 파르페 도메인은 응답 DTO가
   `ParfaitYearsResponse` 단 하나뿐이다. ktlint `standard:filename` 규칙이 **단일 top-level 선언을 담은
   파일은 그 선언과 같은 이름이어야 한다**고 강제해서 복수형 파일명이 실패한다 — 나머지 도메인은
   파일 하나에 선언이 여럿이라 복수형이 통과한다. 리뷰어가 실제로 파일명을 복수형으로 되돌려
   ktlint가 실패하는 것을 재현해 확인했다.

2. **매퍼 실패가 `ApiCaller`에 잡힌다는 서술은 스펙 작성 시점엔 틀렸다.** 원안은 `.safeApiCall { }.map { }`
   형태를 전제했는데, `kotlin.Result.map`은 성공 케이스에서만 `transform`을 실행하고 그 `transform`이
   던지는 예외를 그대로 rethrow한다 — `safeApiCall`의 try/catch 가드는 이미 반환한 뒤이므로 매핑 예외가
   가드 밖에서 터진다. 최종 전체 리뷰가 이를 확인했다. **지금은 스펙 서술대로 동작한다** — 단
   `ApiCaller`에 `safeApiCall(block, transform)` 오버로드가 새로 생겨 `block`과 `transform`을 같은
   `runCatchingApi` 가드 안에서 실행하기 때문이다. 13개 매핑 호출부(auth 3·policy 1·group 8·parfait 1)
   전부가 `.safeApiCall { }.map { }`에서 이 오버로드로 전환됐다. [계약이 던지는 함정](#계약이-던지는-함정)
   6번과 [domain 타입](#domain-타입) 절의 관련 서술을 이 진입점 기준으로 정정했다.

3. **DTO·VO를 도메인별로 한 파일에 묶은 배치는 철회됐다 — 1파일 1선언으로 쪼갰다.**
   [계층과 배치](#계층과-배치) 절이 `AuthRequests.kt`·`AuthResponses.kt`·`PolicyResponses.kt`·
   `ParfaitGroupRequests.kt`·`ParfaitGroupResponses.kt`와 domain의 `Ids.kt`·`AuthTokens.kt`·
   `GroupValues.kt`·`ParfaitGroupVO.kt`·`PolicyVO.kt`처럼 도메인 단위 묶음을 전제했으나,
   **repo 관례는 이미 1파일 1클래스였다**(선행 `TempRequest.kt`·`TempResponse.kt` 각 1선언).
   근거 없이 벗어난 쪽이 이 스펙이다. 10개 묶음 파일을 39개로 분할했고 패키지는 그대로라
   소비 측 import는 한 줄도 바뀌지 않았다. 파일명은 선언명과 일치시킨다.

   묶음이 실제로 문제를 하나 만들었다 — 위 1번의 ktlint `standard:filename`은 **단일 선언 파일에만**
   발동하므로, 선언이 여럿인 묶음 파일은 규칙을 피해 가고 파르페 도메인만 개명을 강제당했다.
   1선언 1파일로 통일하면 그 비대칭이 사라진다.

4. **mapper의 타임존 근거 주석이 없다.** [계약이 던지는 함정](#계약이-던지는-함정) 6번이
   "mapper에 타임존 근거를 주석으로 남긴다"고 정했으나, `source/group/mapper/VOMapper.kt`의
   `recentImageUploadedAt` 변환에는 주석이 없다. 오프셋 없는 문자열이라 다음에 읽는 사람이 UTC로
   오인하면 9시간 어긋난다 — 코드만 봐서는 Asia/Seoul 전제를 알 길이 없다
   → [open-questions](../../../synthesis/open-questions.md).

5. **`KakaoLoginVO`↔`KakaoLoginResult` 상호 참조 KDoc이 없다.** [VO](#vo) 절이 "양쪽 KDoc에 서로를
   가리키는 한 줄을 남긴다"고 정했으나 두 파일 모두 KDoc이 없다. 이름이 닮은 두 타입(서버 응답 vs
   카카오 SDK 결과)이 `domain/model/auth/`와 `domain/model/` 루트에 나란히 있는 상태
   → [open-questions](../../../synthesis/open-questions.md).

6. **`domain/model/`이 하위 패키지와 루트 평면으로 갈렸다.** [domain 타입](#domain-타입) 절은 "평면
   10개에 9개 이상이 더 붙으면 하위 패키지로 나눈다"를 전제했는데, 실제로는 **이번 라운드 신규
   선언만** `auth/`·`group/`·`id/`·`policy/`로 들어가고 기존 8선언(`DayWindow`·`GalleryImageGroup`·
   `GroupCreateConfig`·`InviteCodeResult`·`KakaoLoginResult`·`Logger`·`NameValidResult`·
   `SegmentationResult`)은 루트에 남았다. 다음 모델을 어디에 둘지 규약이 서지 않았다
   → [open-questions](../../../synthesis/open-questions.md).

## 미결

- ~~`@NoAuth` 어노테이션의 R8 release 유지 여부 미검증~~ **해소됨(부정적으로) — 발견 즉시 수정됨.**
  `:data`는 Android 라이브러리 모듈이라 `proguardFiles`(`data/proguard-rules.pro`)는 앱의 R8 실행에
  반영되지 않고, `consumerProguardFiles`도 컨벤션 플러그인이 설정하지 않아 keep 규칙이 앱에 전달될
  경로가 없었다 — release 빌드였다면 `@NoAuth` 4곳 전부에서 어노테이션이 제거돼 화이트리스트
  엔드포인트에 `Authorization` 헤더가 붙어 토큰 재발급이 막혔을 것이다. 규칙을
  `data/consumer-rules.pro`로 옮기고 `setConfigAndroidLibrary`가 `consumerProguardFiles`를 등록하도록
  고쳤다. **2026-08-06 PR #197로 develop 머지 확인** — develop의 라이브러리 모듈 전부가 이미
  `consumer-rules.pro`를 갖고 있어 등록이 다른 모듈을 깨지 않는다. release 빌드 실행으로 확인한
  것은 아니다 → [open-questions](../../../synthesis/open-questions.md).
- 이 레이어 전체가 런타임 미검증 상태로 들어간다(평문 HTTP 차단·`YG_BASE_URL` 부재)
  → [open-questions](../../../synthesis/open-questions.md)
- `PolicyVO.url`이 링크인지 약관 전문인지 서버 데이터 규약 미확정([policy.md](../../../api/policy.md) 미결)
  — 값이 전문이면 화면에서 WebView로 열 수 없다
- 실패를 domain 타입으로 번역하지 않고 `ApiException`을 그대로 올린다 — 표시 문자열 매핑 위치는
  ADR-0016 as-built 논쟁과 함께 화면 결선 라운드에서 결정
