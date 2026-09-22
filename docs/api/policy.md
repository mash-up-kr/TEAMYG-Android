---
id: policy
title: 약관(현재 유효 약관 목록 조회)
server_module: http/auth
server_commit: aa9cc9b
verified: 2026-09-04
android_status: done
related_spec: intro-term-agree
related_adr: ADR-0017
tags: [api, parfait, server-contract, policy]
---

# 약관(현재 유효 약관 목록 조회) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

`[Feat/#64] 약관 목록 조회 API 구현 (#65)`(`69654bc`)로 신설됐다. 회원가입(약관 동의) 화면과 설정
화면이 소비한다. **`POST /api/v1/auth/signup`의 `agreements[].termsId` 출처가 이 엔드포인트다**
([auth.md](auth.md) signup 절).

⚠️ **서버 위치는 `http/auth`인데 경로는 `/api/v1/policies`다.** `PolicyController`가 `http/auth/controller`
패키지에 있고 OpenAPI 태그도 `Auth`지만, URL 세그먼트가 `auth` 하위가 아니라 최상위 `policies`라
이 체계의 파일명 규약(도메인 = URL 세그먼트, [README.md](README.md))에 따라 별도 문서로 둔다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/v1/policies` | 불필요(화이트리스트) | 없음 | `PolicyResponse` | 구현됨 |

## 엔드포인트 상세

### GET /api/v1/policies

- **인증**: 불필요 — `SecurityConfig.WHITELIST_PATHS`에 `/api/v1/policies`가 개별 등록돼 있다.
  auth 도메인의 개별 3경로와 같은 방식이며 와일드카드가 아니다.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`). `@ResponseStatus`가 없고
  컨트롤러가 `ApiResponse<PolicyResponse>`를 그대로 반환한다.
- **요청 필드**: 없음. 쿼리 파라미터·경로 변수·바디 모두 없다(`getPolicies()`가 인자를 받지 않는다).

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `policies` | List<`PolicyItemResponse`> | 아니오 | 아래 원소 구조. **빈 배열이 정상 응답이다** — 아래 참고 |
| `policies[].termsId` | Long | 아니오 | signup 요청의 `agreements[].termsId`에 그대로 넣는 값 |
| `policies[].type` | String | 아니오 | `TosType` enum 이름 문자열. **`TERMS_OF_SERVICE` · `PRIVACY_POLICY` 2종**(`it.type.name`으로 직렬화) |
| `policies[].title` | String | 아니오 | 약관 제목(`Tos.title` 컬럼) |
| `policies[].url` | String | 아니오 | 약관 전문 링크. **별도 컬럼이 아니라 `Tos.content` 재사용** — 아래 참고 |
| `policies[].required` | Boolean | 아니오 | 필수 동의 여부(`Tos.required`, DB 기본값 true). `is` 접두사가 없어 키 변환이 없다 |

  **정렬은 서버가 고정한다.** `PolicyQueryService`가 포트 반환 순서에 의존하지 않고
  `TERMS_OF_SERVICE` → `PRIVACY_POLICY` 순으로 직접 재구성한다(`listOfNotNull`). 별도 정렬 컬럼이
  없고 약관 종류가 둘뿐이라는 전제다 — **종류가 늘면 이 서비스를 고쳐야 하고, 그때까지 앱은 배열
  순서를 화면 순서로 그대로 써도 된다.**

  **"현재 유효한 약관"의 정의**: `TosRepository.findCurrentTerms`가 `type`별로 `published_at` 내림차순
  (동률 시 `id` 내림차순) 1건씩 뽑는다. 즉 **타입당 최대 1건**이라 응답 배열 길이는 0~2다.
  signup의 `TERMS_NOT_FOUND` 판정도 **같은 포트**(`TosQueryPort.findCurrentTerms`)를 쓴다 — 이 API가
  내려준 `termsId`는 같은 시점의 signup에서 유효하다.

  ⚠️ **`policies`가 빈 배열이어도 200이다.** DB에 약관 행이 없으면 `[]`가 나가고 에러가 아니다
  (`PolicyControllerTest`가 이 케이스를 직접 검증한다). 앱이 배열 길이를 검사하지 않고 필수 약관을
  꺼내면 빈 화면으로 진행되고, 그대로 signup을 호출하면 `REQUIRED_TERMS_NOT_AGREED` 400을 받는다.

  ⚠️ **`url`은 `Tos.content` 컬럼을 그대로 매핑한 값이다.** `TosAdapter`가 `url = it.content`로 채운다
  (URL 전용 컬럼을 추가하지 않았다). `Tos.content`는 `@Lob` `LONGTEXT` 컬럼이라 **약관 전문이 들어갈
  수도 있는 자리**다 — 운영 데이터에 전문이 저장돼 있으면 `url` 필드로 전문이 내려온다. 이 필드가
  항상 URL이라는 보장은 스키마에 없고 데이터 투입 규약에만 있다 → [미결](#미결).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| — | — | 이 엔드포인트 전용 에러 코드 없음 |

  `PolicyController`·`PolicyQueryService`·`TosAdapter` 어디에도 예외를 던지는 분기가 없다. 조회 결과가
  비어도 200이다(위 참고). 남는 경로는 전역뿐이다 — `CommonErrorCode.INTERNAL_SERVER_ERROR`(500,
  `GlobalExceptionHandler`의 `Exception` 핸들러). 화이트리스트 경로라 401도 나지 않는다.

  단 `JwtAuthFilter`는 `shouldNotFilter` 오버라이드가 없어 화이트리스트 경로에서도 실행된다 —
  **`Authorization` 헤더에 만료·위조 토큰을 붙이면 401이 난다**(reissue와 같은 함정,
  [auth.md](auth.md) reissue 절). 이 API는 토큰이 필요 없으니 헤더를 붙이지 않는 것이 안전하다.

## Android 매핑

`:data`·`:domain`에 API 표면이 구현됐다([spec](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md)) —
**2026-08-06 PR #197로 develop 머지 완료**다. 이 표면이 딛고 선 공용 인프라(`ApiCaller` 4진입점·
`ApiResponse` envelope·`@NoAuth`·`TokenStoreTokenProvider`)는 PR #190으로 먼저 들어왔고, 아래
Service·DataSource·DTO·VO가 이번에 그 위에 올라갔다.
**✅ 2026-08-15(PR #242) — 화면까지 결선됐다.** `PolicyRepository`/`PolicyRepositoryImpl`(신설)과
`GetPoliciesUseCase`가 붙고 온보딩 약관 동의 화면(`feature/intro/impl` `TermAgreeViewModel`)이 진입 시
이 API를 호출한다. 코틀린 리터럴이던 `TERM_CONTENT_LIST`(+`TermContent`)는 **삭제**됐고 제목·필수 여부·
랜딩 URL이 전부 응답 값이다 → [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md).
응답의 `termsId`는 그대로 `POST /api/v1/auth/signup`의 `agreements[].termsId`로 나간다([auth.md](auth.md)).
**✅ 2026-08-18(PR #296) — 소비처가 둘이 됐고 `url`이 실제로 열린다.** S-001 앱 설정(`AppSettingViewModel`)이
진입 시 같은 UseCase로 목록을 받아 두고, 약관 줄을 누르면 `title`·`url`을 `NavKeyWebView`에 실어 웹뷰를
연다(온보딩 약관 화면의 화살표도 같은 목적지로 간다). **이 응답 두 필드가 목적지 인자가 된 것**이라
NavKey 통합의 근거이기도 하다 → [navigation-flow](../architecture/navigation-flow.md).

앱 동작 메모(코드 대조):

- **정렬은 서버 순서 그대로** 화면 순서다(앱이 재정렬하지 않는다 — 위 "정렬은 서버가 고정한다"에 의존).
- **빈 배열을 실패로 보지 않는다** — 화면은 조회 성공으로 처리하고 목록이 비면 확인 버튼이 비활성이라
  signup까지 가지 않는다. 다만 **조회 실패와 화면 표현이 다르다**(실패는 재시도 문구, 빈 배열은 그냥 빈 목록).
- **설정 화면은 목록을 "여는 재료"로만 쓴다**(#296) — 약관 두 줄은 `strings.xml`로 고정돼 있어 조회가
  실패해도 줄이 사라지지 않고, 대신 **눌러도 아무 일이 없다**(로그만 남는다). 찾는 종류가 목록에 없을 때
  재조회하지 않는 것은 의도다 — 같은 요청이 같은 응답을 줄 것이므로 탭마다 요청만 는다. 사용자에게는
  실패와 무반응이 구분되지 않는다 → [open-questions](../synthesis/open-questions.md).
- **미동의 약관도 함께 보낸다** — `SignUpUseCase`가 화면에 노출한 목록 전체를 `agreed` 플래그와 묶는다.
- 실패는 Repository 경계에서 `AppError`로 바뀌어 올라온다(`mapErrorToAppError`).

| 엔드포인트 | Service 함수 | DataSource 함수 |
|---|---|---|
| GET `/api/v1/policies` | `PolicyService#getPolicies` | `PolicyRemoteDataSource#getPolicies` |

- **응답 DTO**: `PolicyResponse`(`policies: List<PolicyItemResponse>`)·`PolicyItemResponse` — 선언당
  파일 하나(파일명은 선언명과 동일)로 `data/service/model/response/policy/PolicyResponse.kt`·
  `PolicyItemResponse.kt`. 요청 DTO 없음(파라미터 없는 GET).
- **VO**: `PolicyVO`·`PolicyType`(enum, 알 수 없는 값은 `UNKNOWN`으로 떨어진다) — 각각
  `domain/model/policy/PolicyVO.kt`·`PolicyType.kt`. `termsId`는 `domain/model/id/TermsId.kt`의
  `TermsId` value class로 감싼다.
- **Mapper**: `data/source/policy/mapper/VOMapper.kt`(`PolicyResponse#toPolicyVOList`·
  `PolicyItemResponse#toPolicyVO`).

## 미결

- `policies[].url`이 `Tos.content`(LONGTEXT) 컬럼 재사용이라 값이 URL인지 약관 전문인지 스키마로는
  보장되지 않는다. 운영 데이터 투입 규약 확인 필요 → [open-questions](../synthesis/open-questions.md)
  ⚠️ **2026-08-18(PR #296)부터 이 값이 실제로 웹뷰에 들어간다** — 링크가 아니면 로드에 실패해 재시도
  화면이 뜬다(`NavKeyWebView` KDoc이 이 함정을 적어 둔다). 계약 공백이 화면 실패로 드러나는 자리가 생겼다.
