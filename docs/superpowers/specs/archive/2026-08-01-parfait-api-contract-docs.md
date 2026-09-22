---
id: parfait-api-contract-docs
title: parfait/api — 서버 API 계약 문서 체계
status: implemented
category: doc-infra
platforms: android
verified: 2026-08-02
related_code: parfait/api/README.md, parfait/api/conventions.md, parfait/api/server-baseline.md, parfait/api/template.md, parfait/api/auth.md, parfait/api/parfait-group.md, parfait/api/parfait.md, .claude/skills/sync-teamyg-server-api/SKILL.md
related_adr: ADR-0017
related_spec: data-network-setup
related_architecture: data-layer
supersedes:
superseded_by:
tags: [spec, parfait, api, doc-infra, server-contract]
---

# parfait/api — 서버 API 계약 문서 체계

## 배경

TJYG-Android의 원격 네트워크 기초 구조는 [ADR-0017](../../../adr/0017-remote-network-datasource.md)로 확정돼 develop에 머지됐지만(#174),
실제 API 연동은 시작되지 않았다. `TempService`·`TempResponse`·`TempRemoteDataSource`가 복제용 예시로 남아 있고
`ApiResponse.SUCCESS_CODE`는 `TODO` 상태다.

한편 서버(`mash-up-kr/TEAMYG-SERVER`, Spring Boot + 헥사고날)는 이미 인증·그룹·파르페 조회 API를 구현했다.
Android 구현자가 매번 서버 저장소를 훑어 컨트롤러·DTO·에러코드를 재구성하는 대신,
**서버 계약을 parfait 안에 스냅샷으로 두고 Android 적용 상태를 함께 추적**한다.

`parfait/api/`는 서버 계약의 **미러**이지 정본이 아니다. 정본은 서버 코드이며,
불일치 시 서버 코드가 이긴다(파르페 SoT 우선순위 "코드 > wiki > CLAUDE.md"와 동형).

## 목표

- Android 구현자가 `parfait/api/`만 읽고 Service·Response·DataSource를 작성할 수 있다.
- 서버가 제공하는 것과 Android가 소비하는 것의 **간극이 표 하나로 보인다**.
- 서버 변경 시 **delta만** 재감사한다(전체 재작성 금지).

## 범위

**포함**
- `parfait/api/` 디렉토리 신설: `README.md`·`conventions.md`·`server-baseline.md`·`template.md`
- 도메인 계약 문서 3건 초기 작성: `auth.md`(2) · `parfait-group.md`(8) · `parfait.md`(1)
- 갱신 스킬 `.claude/skills/sync-teamyg-server-api/SKILL.md` 신설
- `parfait/index.md` 라우팅 등록
- `wiki/personal-private/project-paths.md`에 서버 로컬 경로 추가(private submodule)
- 발견된 계약 불일치를 `parfait/synthesis/open-questions.md`에 등록

**제외(이번 범위 아님)**
- **TJYG-Android 코드 수정 일절 없음** — `ApiResponse` 필드 추가·`isSuccess` 판정 수정·실제 Service 구현은
  이 스펙 완료 후 별도로 판단한다(사용자 지시).
- 서버 코드 수정
- `HealthController`(`GET /health`) 전용 도메인 문서 — 운영용이고 Android가 호출하지 않는다.
  단 화이트리스트 관측 사실은 `conventions.md`에 기록한다(아래).
- OpenAPI JSON 자동 생성·파싱 스크립트 — 근거 소스는 **서버 코드 직독**으로 확정했다.

## 디렉토리 구조

```
parfait/api/
  README.md          인덱스 테이블 + 규약 + 갱신 절차 요약
  conventions.md     전역 계약(envelope·성공/에러 코드·인증·URL 규약)
  server-baseline.md 서버 main 커밋 기준선(SoT) + delta 감사 절차
  template.md        도메인 계약 문서 템플릿
  auth.md            인증 도메인 계약
  parfait-group.md   그룹 도메인 계약
  parfait.md         파르페(캔버스) 조회 도메인 계약
```

**파일명에 날짜 접두사를 붙이지 않는다.** `specs/`·`plans/`는 1회성 산출물이라 `YYYY-MM-DD-` 접두사와
`archive/` 이동을 쓰지만, API 계약은 `architecture/`와 같은 **살아있는 문서**다. 서버가 바뀌면 같은
파일을 갱신하고, 판본은 frontmatter `server_commit`·`verified`가 기록한다.

도메인 파일명은 **서버 URL 세그먼트**를 따른다(`/api/parfait-groups` → `parfait-group.md`).
서버 패키지명(`http/api/auth`)이 아니라 URL을 택한 이유: Android 소비자는 패키지가 아니라 경로로 API를 찾는다.

## 도메인 계약 문서 형식

### Frontmatter

| 필드 | 내용 |
|---|---|
| `id` | 파일명(확장자 제외) |
| `title` | 도메인 한 줄 이름 |
| `server_module` | 서버 소스 위치(예: `http/parfaitgroup`) — 재감사 시 볼 곳 |
| `server_commit` | 이 문서를 대조한 서버 커밋 해시 |
| `verified` | 대조일 `YYYY-MM-DD` |
| `android_status` | `none` / `partial` / `done` — 도메인 전체 롤업 |
| `related_spec`·`related_adr` | parfait 문서 연결 |
| `tags` | `[api, parfait, server-contract, <도메인>]` |

### 본문 절

1. **엔드포인트 표** — 메서드 / 경로 / 인증 / 요청 / 응답 / **Android** 열.
   Android 열 값은 3종: `미구현` · `구현됨` · `⚠️불일치`(사유 각주).
2. **엔드포인트 상세** — 엔드포인트마다 요청 필드·응답 필드·전용 에러코드(HTTP status + code + 의미).
3. **Android 매핑** — 대응 심볼명(`XService#method` · `XResponse` · `XRemoteDataSource`).
   미구현이면 "없음"으로 명시한다(빈 절을 남기지 않는다).
4. **미결** — [`../../synthesis/open-questions.md`](../../../synthesis/open-questions.md) 링크.

파르페 공통 규율을 그대로 적용한다: **라인번호·변동 수치·색 hex 금지**, 근거는 파일명 + 심볼명.

## conventions.md 내용 (2026-08-01 서버 코드 대조 확정)

⚠️ **2026-08-02 기준선 전진(`6f5bffc`) 반영** — 아래 절은 초안 시점(`6b05b8c`) 스냅샷이라 **당시 전제**를
그대로 남긴다(조용히 덮어쓰지 않는다). 실제 `conventions.md`는 `AuthErrorCode` **12종**(`FORBIDDEN_REFRESH_TOKEN`
403 신설), 화이트리스트는 `/api/v1/auth/**` 와일드카드 대신 `/api/v1/auth/kakao`·`signup`·`reissue` 개별
3경로(+`/favicon.ico`, `logout`은 제외돼 인증 대상)로 갱신됐다 — 최신 값은 [conventions.md](../../../api/conventions.md),
이력은 [server-baseline.md](../../../api/server-baseline.md) "기준선 이력" 표.

- **응답 envelope** `parfait.common.response.ApiResponse<T>`:
  `success: Boolean` · `code: String` · `message: String` · `data: T?` · `errorDetail: Map<String, String>?`
- **성공 코드 2종**: `ok()` → `"OK"`, `created()` → `"CREATED"`. 즉 성공 판정은 단일 상수 비교로 불가능하다.
- **에러 코드 체계**: `BaseErrorCode` 인터페이스(`status`·`code`·`message`)를 도메인별 enum이 구현한다 —
  공통 `CommonErrorCode`(`INVALID_REQUEST`·`INTERNAL_SERVER_ERROR`), 그룹 `ParfaitGroupApiErrorCode`(11종),
  인증 `AuthErrorCode`(11종). 에러 응답은 `ApiResponse.error(errorCode, errorDetail)`로 생성된다.
- **`errorDetail`은 현재 항상 `null`**: `GlobalExceptionHandler`의 네 핸들러가 모두 `errorDetail` 인자 없이
  `ApiResponse.error(errorCode)`를 호출한다. 검증 실패(`MethodArgumentNotValidException`)도 필드별 상세 없이
  `CommonErrorCode.INVALID_REQUEST` 하나로 뭉개진다. **필드가 계약에는 있으나 값은 채워지지 않는다**를 명시한다.
- **인증**: JWT Bearer. `JwtAuthFilter`가 검증하고 `Authentication.name`이 **memberId(Long 문자열)**다.
  화이트리스트(무인증): `/actuator/health` · `/swagger-ui**` · `/v3/api-docs/**` · `/api/v1/auth/**`.
  그 외 **전 요청 인증 필수**. 관측 사실 1건: `HealthController`가 매핑한 `GET /health`는
  화이트리스트의 `/actuator/health`와 **경로가 달라 인증 대상**이다.
- **URL 규약 혼재 (3형태)**: `/api/v1/auth/**` · `/api/v1/groups/{groupId}/parfaits/**` · `/api/parfait-groups`.
  버전 프리픽스 유무가 갈리고, **그룹을 가리키는 경로가 `groups`와 `parfait-groups` 둘**이다.
  서버 규약 문서가 없으므로 **관측 사실로 기록**하고 미결로 등록한다.
- **OpenAPI**: 서버는 springdoc을 켜두었다(`OpenApiConfig`, `/v3/api-docs`·`/swagger-ui`).
  본 체계는 이를 근거로 쓰지 않지만(서버 실행 필요), 대조 보조 수단으로 존재를 기록한다.

### Android 불일치 (문서화만 — 코드 수정은 이번 범위 밖)

| # | 불일치 | 영향 |
|---|---|---|
| 1 | Android `ApiResponse`에 `success`·`errorDetail` 필드 없음 | `errorDetail`(필드별 검증 메시지) 소비 불가 |
| 2 | Android `isSuccess = code == "SUCCESS"` (`SUCCESS_CODE` 상수가 `TODO`) — 서버는 `"OK"`/`"CREATED"` | 현 상태로 **모든 호출이 `ApiException.Business` 실패 판정** |
| 3 | Android `TokenProvider` 구현이 `EmptyTokenProvider`(항상 null) | 화이트리스트 밖 전 API가 401 |

세 건 모두 `open-questions.md`에 등록하고, 해당 엔드포인트 표의 Android 열은 `미구현`으로 둔다
(`⚠️불일치`는 Android에 대응 심볼이 **있는데** 계약과 어긋날 때 쓴다).

## server-baseline.md 형식

[`../../doc-baseline.md`](../../../doc-baseline.md)와 동형이되 **별도 파일**이다 — 대상 저장소가 다르고
(`TEAMYG-SERVER` vs `TJYG-Android`) 갱신 주기도 독립적으로 돈다.

구성: **현재 기준선**(repo·브랜치·커밋·요약·검증일) + **점검 절차** + **기준선 이력 표**.

**추적 브랜치는 `main`이다.** 근거: `origin/HEAD -> origin/main`(기본 브랜치)이고 기능 PR이 main으로
머지된다(#57 signup, #62 파르페 연도 조회). `develop`은 main을 주기적으로 끌어오는 쪽이라 **뒤처진다** —
실제로 2026-08-01 시점에 develop은 위 두 API를 갖고 있지 않았다. 앱이 바라볼 서버는 main에서 나온다.
(TJYG-Android는 `develop`을 추적한다 — 두 저장소의 통합 브랜치 이름이 다르다는 것을 문서에 명시한다.)

초기 기준선은 **`origin/main` = `6b05b8c`**(`[Feat/#61] 그룹별 캘린더 연도 리스트 조회 API 구현 (#62)`).
⚠️ 로컬 워킹트리는 `develop`에 있고 main보다 뒤처져 있다 —
**모든 대조는 `git show origin/main:<path>`로 하고 워킹트리를 믿지 않는다.**

로컬 절대경로는 개인정보라 문서에는 `<TEAMYG-SERVER>` 플레이스홀더를 쓰고,
실경로는 `wiki/personal-private/project-paths.md`(private submodule)에 추가한다.

## 갱신 스킬 sync-teamyg-server-api

`.claude/skills/sync-teamyg-server-api/SKILL.md`. `sync-tjyg-develop-baseline`과 동형 구조:

- **frontmatter** `name`·`description`(트리거 문구: "서버 API 문서 점검"·"TEAMYG-SERVER delta 감사" 등)
- **핵심 규율**: 전체 재감사 금지(기준선 이후 delta만) / 절대경로는 private submodule 참조 /
  commit·push·PR은 사용자 확인 후
- **단계**: ① `server-baseline.md`에서 기준선 확보 → ② `git -C <S> fetch origin main` +
  `log --oneline <기준선>..origin/main` → ③ 변경된 컨트롤러·DTO·에러코드를
  `parfait/api/*.md`와 대조 → ④ 드리프트 수정 + 신규 도메인이면 `template.md`로 문서 신규 작성 +
  `README.md` 인덱스 등록 → ⑤ 기준선 갱신(현재 블록 + 이력 표 1줄) → ⑥ 보고 → 커밋(승인 후)

Android 쪽 변경(구현이 진행돼 `android_status`가 바뀌는 경우)은 이 스킬이 아니라
`sync-tjyg-develop-baseline`이 잡는다. 두 스킬의 경계: **서버 delta → 계약 절 갱신 /
Android delta → `android_status`·Android 매핑 절 갱신**. 이 경계를 두 스킬 문서에 각각 명시한다.

## 초기 작성 대상

⚠️ **2026-08-02 기준선 전진(`6f5bffc`) 반영** — 아래 표도 초안 시점(`6b05b8c`) 스냅샷이라 **당시 전제**를
그대로 남긴다(조용히 덮어쓰지 않는다). 실제 문서는 auth **4** 엔드포인트(`kakao`·`signup`·`reissue`·`logout`,
`server_module: http/auth`) · parfait-group **8**(계약 불변, `server_module: http/parfaitgroup`) · parfait **1**
(`server_module: http/parfait`)이다 — 최신 값·경로는 [api/README.md](../../../api/README.md).

| 도메인 | 서버 위치 | 엔드포인트 |
|---|---|---|
| `auth.md` | `http/api/auth` | `POST /api/v1/auth/kakao`(카카오 로그인 — 신규/기존 분기 응답) · `POST /api/v1/auth/signup`(회원가입 완료 — 약관 동의 목록) |
| `parfait-group.md` | `http/parfaitgroup` | `GET /api/parfait-groups` · `GET /{groupId}` · `GET /join-preview` · `POST /join` · `POST /` · `PATCH /{groupId}/nickname` · `DELETE /{groupId}/members/me` · `POST /{groupId}/reports` |
| `parfait.md` | `http/api/parfait` | `GET /api/v1/groups/{groupId}/parfaits/year`(그룹 캘린더 연도 리스트) |

세 도메인 모두 Android 대응 심볼이 0건이므로 전 엔드포인트 Android 열은 `미구현`이다.
요청·응답 필드는 서버 DTO(`*Request`·`*Response`)를 직독해 채운다.

## 연동 지점

1. **`parfait/index.md`** — "무엇을 찾는가" 표에 `서버 API 계약·엔드포인트 → api/README.md` 행 추가,
   "문서 지도"에 `api/` 항목 추가.
2. **`parfait/synthesis/open-questions.md`** — 계약 불일치 3건 + URL 버전 규약 혼재 1건 등록.
3. **`wiki/personal-private/project-paths.md`** — 서버 repo 절대경로 행 추가(private submodule PR 필요).
4. **위키 무관** — 서버 계약은 구현 사실이지 정책이 아니므로 `wiki/`는 건드리지 않는다.

## 검증

문서 작업이라 자동 테스트가 없다. 대신:

- 각 도메인 문서의 엔드포인트·필드·에러코드를 **서버 소스와 1:1 재대조**한다(작성 후 1회).
- 상대 링크가 실제로 resolve되는지 확인한다(`parfait/api/` → `../adr/`·`../synthesis/`).
- `parfait/index.md`에서 `api/README.md`까지 라우팅이 이어지는지 확인한다.
- public repo에 서버 로컬 절대경로·토큰·시크릿이 새지 않았는지 확인한다.

## 열린 질문

- 서버 URL 규약 3형태(`/api/v1/auth` · `/api/v1/groups/{id}/parfaits` · `/api/parfait-groups`)가 의도된 것인지
  미확정. 특히 **그룹 경로가 `groups`와 `parfait-groups`로 갈린다** — 서버팀 확인 필요.
- `errorDetail`이 계약에만 있고 값이 채워지지 않는 상태가 의도인지 미확정 — Android가 필드별 검증 메시지를
  못 받으므로, 폼 검증 UI가 필요해지면 서버팀과 정해야 한다.
- `GET /health`가 인증 대상인 것이 의도인지 미확정(화이트리스트는 `/actuator/health`만 허용).
- Android 코드 수정(불일치 3건) 착수 여부는 이 스펙 완료 후 사용자가 결정한다.
