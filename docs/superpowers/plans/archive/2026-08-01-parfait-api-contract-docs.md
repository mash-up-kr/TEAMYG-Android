---
id: parfait-api-contract-docs
title: parfait/api — 서버 API 계약 문서 체계 구축
status: done
type: work-order
created: 2026-08-01
updated: 2026-08-02
platforms: android
owner:
related_adr: ADR-0017
related_spec: parfait-api-contract-docs
related_code: parfait/api/README.md, parfait/api/conventions.md, parfait/api/server-baseline.md, parfait/api/template.md, parfait/api/auth.md, parfait/api/parfait-group.md, parfait/api/parfait.md, .claude/skills/sync-teamyg-server-api/SKILL.md
archived_reason: Task 1~6 전량 완료 — parfait/api/ 7파일 서버 main 6f5bffc와 1:1 대조 완료, index.md 라우팅·open-questions 8건 등록(2026-08-02 최종 리뷰에서 스펙 "열린 질문" 이월 2건 보완)·private submodule 경로 편집(커밋 대기)·링크/민감정보 검사 통과. TJYG-Android·TEAMYG-SERVER 코드 변경 0건.
tags: [plan, parfait, api, doc-infra, server-contract]
---

# parfait/api — 서버 API 계약 문서 체계 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 서버(`mash-up-kr/TEAMYG-SERVER`) API 계약을 `parfait/api/`에 스냅샷으로 두고, Android 적용 상태를 함께 추적하며, 서버 변경 시 delta만 재감사하는 체계를 만든다.

**Architecture:** `parfait/api/`는 서버 코드의 **미러**다(정본은 서버 코드). 전역 계약은 `conventions.md`, 도메인별 계약은 URL 세그먼트를 딴 파일(`auth.md`·`parfait-group.md`·`parfait.md`), 재감사 기준점은 `server-baseline.md`(서버 `main` 커밋), 반복 절차는 스킬 `sync-teamyg-server-api`가 들고 있다. 문서 작업이라 코드 변경·테스트가 없고, 검증은 **서버 소스 1:1 재대조 + 링크 resolve**다.

**Tech Stack:** Markdown, git(서버 저장소 read-only 조회), Claude Code 스킬(`.claude/skills/`)

## Global Constraints

- **TJYG-Android 코드는 한 줄도 고치지 않는다.** 불일치는 문서화·`open-questions.md` 등록까지만.
- **서버 코드도 고치지 않는다.** 서버 저장소는 read-only 조회 대상.
- **추적 브랜치는 서버 `main`.** 모든 대조는 `git -C <S> show origin/main:<path>`로 한다 — 로컬 워킹트리는 `develop`이고 뒤처져 있어 믿을 수 없다.
- **기준 커밋**: `origin/main` = `6f5bffc` (`[Feat/#45] 토큰 재발급(refresh) / 로그아웃 API 구현 (#63)`).
  ⚠️ **실행 중 서버가 전진해 `6b05b8c` → `6f5bffc`로 기준선을 올렸다**(2026-08-02, 사용자 승인).
  #63이 함께 가져온 것: **패키지 전면 재편**(`http/api/auth`→`http/auth`, `http/api/parfait`→`http/parfait`,
  `http/api/health`→`http/global/health`, `http/parfaitgroup/*.kt`→`http/parfaitgroup/{controller,dto,exception}/`) ·
  **신규 엔드포인트 2**(reissue·logout) · **`AuthErrorCode` 11→12종**(`FORBIDDEN_REFRESH_TOKEN` 403) ·
  **화이트리스트 실질 변경**(`/api/v1/auth/**` → `kakao`·`signup`·`reissue` 개별 3경로 + `/favicon.ico`
  → **`logout`은 인증 필요**). **그룹 API 계약은 불변**(`ApiResponse.Companion.ok` 명시화뿐인 기계적 변경).
- 파르페 규율: **라인번호·변동 수치·색 hex 금지**. 근거는 파일명 + 심볼명.
- **public repo**: 서버 로컬 절대경로를 `parfait/`에 쓰지 않는다. 문서에는 `<TEAMYG-SERVER>` 플레이스홀더, 실경로는 private submodule.
- **commit·push·PR은 사용자 확인 후.** `main` 직접 커밋 금지. 작업 브랜치 `docs/parfait-api-contract-docs`(생성 완료).
- 서버 저장소 경로는 `wiki/personal-private/project-paths.md` 참조(이하 `<S>`).

---

## File Structure

| 파일 | 책임 |
|---|---|
| `parfait/api/README.md` | 도메인 문서 인덱스 + 이 디렉토리의 규약(파일명·frontmatter·Android 열 값) + 갱신 절차 요약 |
| `parfait/api/conventions.md` | 도메인 무관 전역 계약: envelope·성공 코드·에러코드 체계·인증·URL 규약·Android 불일치 |
| `parfait/api/server-baseline.md` | 서버 `main` 커밋 기준선(SoT) + delta 감사 절차 + 이력 표 |
| `parfait/api/template.md` | 도메인 계약 문서 템플릿(frontmatter + 4개 절) |
| `parfait/api/auth.md` | 인증 도메인(4 엔드포인트) |
| `parfait/api/parfait-group.md` | 그룹 도메인(8 엔드포인트) |
| `parfait/api/parfait.md` | 파르페 조회 도메인(1 엔드포인트) |
| `.claude/skills/sync-teamyg-server-api/SKILL.md` | 서버 delta 재감사 반복 워크플로 |

---

### Task 1: `parfait/api/` 골격 — template·conventions·server-baseline·README

**Files:**
- Create: `parfait/api/template.md`
- Create: `parfait/api/conventions.md`
- Create: `parfait/api/server-baseline.md`
- Create: `parfait/api/README.md`

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: `template.md`의 frontmatter 필드 집합(`id`·`title`·`server_module`·`server_commit`·`verified`·`android_status`·`related_spec`·`related_adr`·`tags`)과 본문 4개 절 구조 — Task 2·3·4가 이 형식을 그대로 따른다. `conventions.md`의 `## 응답 envelope`·`## 에러 코드 체계`·`## 인증`·`## Android 불일치` 앵커 — 도메인 문서가 상대링크로 참조한다.

- [ ] **Step 1: `parfait/api/template.md` 작성**

```markdown
---
id: <파일명(확장자 제외)>
title: <도메인 한 줄 이름>
server_module: <서버 소스 위치, 예: http/parfaitgroup>
server_commit: <대조한 서버 main 커밋 short hash>
verified: YYYY-MM-DD
android_status: none        # none | partial | done
related_spec:
related_adr:
tags: [api, parfait, server-contract, <도메인>]
---

# <도메인 이름> API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| <GET/POST/…> | <경로> | <필요/불필요> | <요청 타입 또는 `없음`> | <응답 타입> | <미구현 / 구현됨 / ⚠️불일치> |

## 엔드포인트 상세

### <메서드> <경로>

- **인증**: <필요/불필요>
- **성공**: HTTP <코드> · envelope `code` = `"<OK/CREATED>"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|

## Android 매핑

<대응 심볼명(`XService#method`·`XResponse`·`XRemoteDataSource`) 또는 "없음">

## 미결

- <항목> → [open-questions](../../synthesis/open-questions.md)
```

- [ ] **Step 2: `parfait/api/conventions.md` 작성**

아래 내용은 2026-08-01에 `origin/main` `6b05b8c`를 직독해 확정한 것이다. 그대로 싣는다.

```markdown
---
id: conventions
title: 서버 API 전역 계약
server_module: common/response, common/error, http/global
server_commit: 6b05b8c
verified: 2026-08-01
tags: [api, parfait, server-contract, conventions]
---

# 서버 API 전역 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다.
> 도메인별 계약은 [README.md](README.md)의 인덱스 참고.

## 응답 envelope

모든 응답은 `parfait.common.response.ApiResponse<T>`로 감싼다.

| 필드 | 타입 | 비고 |
|---|---|---|
| `success` | Boolean | 성공 여부 |
| `code` | String | 성공 시 `"OK"`/`"CREATED"`, 실패 시 에러 코드 |
| `message` | String | 사람이 읽는 메시지 |
| `data` | T? | 성공 payload, 실패 시 `null` |
| `errorDetail` | Map<String, String>? | **현재 항상 `null`** — 아래 참고 |

생성 지점은 세 개다 — `ApiResponse.ok(data)`(`code`=`"OK"`) · `ApiResponse.created(data)`(`code`=`"CREATED"`) ·
`ApiResponse.error(errorCode, errorDetail)`.

**성공 코드가 2종**이라는 점이 중요하다. 클라이언트가 성공을 단일 상수 비교로 판정하면 `CREATED` 응답을
실패로 분류한다.

### `errorDetail`은 계약에만 있고 채워지지 않는다

`GlobalExceptionHandler`의 네 핸들러(`BusinessException`·`ParfaitGroupException`·bad-request 4종·`Exception`)가
모두 `errorDetail` 인자 없이 `ApiResponse.error(errorCode)`를 호출한다. 검증 실패
(`MethodArgumentNotValidException`)도 필드별 상세 없이 `CommonErrorCode.INVALID_REQUEST` 하나로 뭉개진다.

## 에러 코드 체계

`parfait.common.error.BaseErrorCode` 인터페이스(`status: Int`·`code: String`·`message: String`)를
도메인별 enum이 구현한다.

| enum | 위치 | 종수 |
|---|---|---|
| `CommonErrorCode` | `common/error` | 2 |
| `AuthErrorCode` | `core/auth/exception` | 11 |
| `ParfaitGroupApiErrorCode` | `http/parfaitgroup` | 11 |

### `CommonErrorCode`

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 요청 형식이 올바르지 않습니다 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 내부 오류가 발생했습니다 |

`ParfaitGroupApiErrorCode`는 core 계층 `ParfaitGroupError`와 **이름이 1:1**이다(`from(error) = valueOf(error.name)`).

## 인증

JWT Bearer. `JwtAuthFilter`가 검증하고 인증 주체의 이름(`Authentication.name`)이 **memberId(Long 문자열)**다.
컨트롤러는 `Authentication.memberId(): Long = name.toLong()` 확장으로 꺼낸다.

`SecurityConfig`는 세션을 쓰지 않고(STATELESS), 아래 화이트리스트 외 **전 요청 인증 필수**다.

- `/actuator/health`
- `/swagger-ui.html` · `/swagger-ui/**`
- `/v3/api-docs/**`
- `/api/v1/auth/**`

인증 실패는 `AuthErrorCode.UNAUTHORIZED`(401)로 나간다.

**관측 사실**: `HealthController`가 매핑한 `GET /health`는 화이트리스트의 `/actuator/health`와 경로가 달라
**인증 대상**이다.

## URL 규약

현재 3형태가 공존한다.

| 형태 | 예 |
|---|---|
| `/api/v1/<도메인>` | `/api/v1/auth/kakao` · `/api/v1/auth/signup` |
| `/api/v1/groups/{groupId}/<하위>` | `/api/v1/groups/{groupId}/parfaits/year` |
| `/api/<도메인>` (버전 없음) | `/api/parfait-groups` |

버전 프리픽스 유무가 갈리고, **그룹을 가리키는 경로가 `groups`와 `parfait-groups` 둘**이다.
서버에 URL 규약 문서가 없어 관측 사실로만 적는다 → [open-questions](../../synthesis/open-questions.md).

## OpenAPI

서버는 springdoc을 켜 두었다(`OpenApiConfig`, title `Parfait API`, version `v1`) — `/v3/api-docs`·`/swagger-ui`.
이 문서 체계는 **서버 코드 직독**을 근거로 삼고 OpenAPI JSON을 파싱하지 않는다(서버 실행이 필요하고
에러코드 열거·검증 로직이 스키마에 안 잡힌다). 대조 보조 수단으로만 존재를 기록한다.

## Android 불일치

TJYG-Android `:data`의 원격 네트워크 구조([ADR-0017](../../adr/0017-remote-network-datasource.md))와 위 계약의 간극.
**세 건 모두 코드 미수정 상태**다.

| # | 불일치 | 영향 |
|---|---|---|
| 1 | Android `ApiResponse`에 `success`·`errorDetail` 필드 없음(`code`/`message`/`data`만) | 서버가 보내는 두 필드를 소비하지 못한다 |
| 2 | Android `ApiResponse.isSuccess`가 `code == "SUCCESS"` 단일 비교(`SUCCESS_CODE` 상수가 `TODO`) — 서버는 `"OK"`/`"CREATED"` | **현 상태로 모든 호출이 `ApiException.Business` 실패 판정** |
| 3 | Android `TokenProvider` 구현이 `EmptyTokenProvider`(항상 null 반환) | 화이트리스트 밖 전 API가 401 |

세 건은 [open-questions](../../synthesis/open-questions.md)에 등록돼 있다.
```

- [ ] **Step 3: `parfait/api/server-baseline.md` 작성**

```markdown
# 서버 API 문서 검증 기준선 (Server Baseline)

> `parfait/api/` 계약 문서를 **어느 서버 커밋 기준으로 마지막 검증했는지** 기록하는 단일 출처(SoT).
> "서버 API 문서 점검"을 요청받으면 아래 기준선부터 현재 `origin/main`까지의 **delta만** 감사하고,
> 끝나면 기준선을 갱신한다.

## 현재 기준선
- **repo**: `TEAMYG-SERVER` (`mash-up-kr/TEAMYG-SERVER`) **`main`**
- **커밋**: `6b05b8c`
- **요약**: `[Feat/#61] 그룹별 캘린더 연도 리스트 조회 API 구현 (#62)`
- **검증일**: 2026-08-01 (1회차 — 체계 신설)

### 왜 `main`인가
서버 저장소의 기본 브랜치가 `main`이고(`origin/HEAD -> origin/main`) 기능 PR이 main으로 머지된다.
`develop`은 main을 주기적으로 끌어오는 쪽이라 **뒤처진다** — 체계 신설 시점에 develop은
signup·파르페 연도 조회 두 API를 갖고 있지 않았다. 앱이 바라볼 서버는 main에서 나온다.

⚠️ TJYG-Android는 `develop`을 추적한다([doc-baseline.md](../../doc-baseline.md)).
**두 저장소의 통합 브랜치 이름이 다르다** — 혼동하지 말 것.

## 점검 절차 (다음 요청 시)
로컬 경로는 개인정보라 `wiki/personal-private/project-paths.md` 참고(아래 `<S>`).

1. **최신화**: `git -C <S> fetch origin main`
2. **신규 커밋 나열**: `git -C <S> log --oneline <기준선>..origin/main`
   - 기능 PR이 squash로 들어와 merge 커밋이 아닐 수 있다 → **`--merges` 필터를 쓰지 않는다.**
   - 변경 파일: `git -C <S> show --stat <hash>`
3. **계약 대조**: 컨트롤러·`*Request`/`*Response` DTO·`*ErrorCode` enum·`SecurityConfig`·
   `ApiResponse`·`GlobalExceptionHandler` 변경이 `parfait/api/*.md`와 어긋나는지 검사.
   - 파일 조회는 항상 `git -C <S> show origin/main:<path>` — **워킹트리를 믿지 않는다**(로컬은 `develop`).
   - 신규 도메인이면 [template.md](template.md)로 문서 신설 + [README.md](README.md) 인덱스 등록.
4. **기준선 갱신**: 위 "현재 기준선"을 새 `origin/main` HEAD로 교체하고 아래 이력에 한 줄 추가.

## 기준선 이력
| 검증일 | main 커밋 | 요약 | 비고 |
|--------|-----------|------|------|
| 2026-08-01 | `6b05b8c` | `[Feat/#61] 그룹별 캘린더 연도 리스트 조회 API (#62)` | 체계 신설. 도메인 3건(auth 2·parfait-group 8·parfait 1) 전량 초기 작성. Android 대응 심볼 0건 → 전 엔드포인트 `미구현`. 불일치 3건·URL 규약 혼재 open-questions 등록 |
```

- [ ] **Step 4: `parfait/api/README.md` 작성**

도메인 3건 행은 Task 2~4에서 채운다. 이 Step에서는 표 자체와 규약을 세운다.

```markdown
# API 계약 문서

서버(`mash-up-kr/TEAMYG-SERVER`)가 제공하는 API 계약의 **스냅샷**과 TJYG-Android의 **적용 상태**를 함께 둡니다.

> **정본은 서버 코드**입니다. 이 디렉토리는 미러이고, 어긋나면 서버가 옳습니다
> (파르페 SoT 우선순위 "코드 > wiki > CLAUDE.md"와 동형).
>
> 추적 브랜치는 서버 **`main`** — 기준 커밋과 갱신 절차는 [server-baseline.md](server-baseline.md).

## 전역 계약
- [conventions.md](conventions.md) — 응답 envelope·성공/에러 코드 체계·인증·URL 규약·**Android 불일치 3건**

## 도메인 계약
| 문서 | 서버 위치 | 엔드포인트 | Android |
|---|---|---|---|

## 규약
- **파일명에 날짜 접두사를 붙이지 않습니다.** `specs/`·`plans/`와 달리 API 계약은 `architecture/`와 같은
  **살아있는 문서**입니다 — 서버가 바뀌면 같은 파일을 갱신하고, 판본은 frontmatter `server_commit`·`verified`가 기록합니다.
- 도메인 파일명은 **서버 URL 세그먼트** 기준입니다(`/api/parfait-groups` → `parfait-group.md`).
  소비자는 서버 패키지가 아니라 경로로 API를 찾기 때문입니다.
- 형식 권위 출처는 [template.md](template.md). 새 도메인 문서는 위 인덱스 표에 한 줄 등록합니다.
- 엔드포인트 표의 **Android 열**은 세 값입니다.
  - `미구현` — 대응 심볼이 없다
  - `구현됨` — 대응 심볼이 있고 계약과 일치한다
  - `⚠️불일치` — 대응 심볼이 **있는데** 계약과 어긋난다(사유 각주 필수)
- 파르페 공통 규율: **라인번호·변동 수치·색 hex 금지**. 근거는 파일명 + 심볼명.

## 갱신
- **서버가 바뀌었을 때** → 스킬 `sync-teamyg-server-api`(계약 절 갱신 + 기준선 갱신)
- **Android가 바뀌었을 때** → 스킬 `sync-tjyg-develop-baseline`(`android_status`·Android 매핑 절 갱신)
```

- [ ] **Step 5: 링크 resolve 검증**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
for f in parfait/api/*.md; do
  grep -oE '\]\(([^)]+\.md)[^)]*\)' "$f" | sed -E 's/^\]\(//; s/[)#].*$//' | while read -r l; do
    [ -e "parfait/api/$l" ] || echo "BROKEN: $f -> $l"
  done
done
```
Expected: 출력 없음. (`../adr/0017-…`·`../synthesis/open-questions.md`·`../doc-baseline.md`가 실재하므로 전부 resolve된다.)

- [ ] **Step 6: 커밋 — 사용자 확인 후**

CLAUDE.md 규율상 **먼저 사용자에게 묻고 승인받는다.** 승인 시:
```bash
git add parfait/api/template.md parfait/api/conventions.md parfait/api/server-baseline.md parfait/api/README.md
git commit -m "docs(parfait): parfait/api 골격 — 전역 계약·기준선·템플릿"
```

---

### Task 2: `auth.md` — 인증 도메인 (2 엔드포인트)

**Files:**
- Create: `parfait/api/auth.md`
- Modify: `parfait/api/README.md` (도메인 인덱스 표에 1행)
- Modify: `parfait/api/conventions.md` (기준선 전진 반영 — 아래 추가 범위)
- Modify: `parfait/api/server-baseline.md` (기준선 전진 반영 — 아래 추가 범위)

**Interfaces:**
- Consumes: Task 1의 `template.md` 형식, `conventions.md`의 에러 체계·인증 절
- Produces: 없음(도메인 문서는 서로 참조하지 않는다)

**추가 범위 — 기준선 전진(`6b05b8c` → `6f5bffc`) 반영**

Task 1이 `6b05b8c` 기준으로 쓴 두 파일을 새 기준선에 맞춘다. 같은 서버 조사를 이미 하는 Task라 여기서 함께 처리한다.

- `conventions.md`
  - frontmatter `server_commit` → `6f5bffc`, `verified` → 작업일
  - **인증 절 화이트리스트**: `/api/v1/auth/**` 한 줄을 `/api/v1/auth/kakao` · `/api/v1/auth/signup` ·
    `/api/v1/auth/reissue` 개별 3항목으로 교체하고 `/favicon.ico` 추가. **`/api/v1/auth/logout`은
    화이트리스트에 없어 인증 대상**임을 한 줄로 명시한다.
  - `AuthErrorCode` 종수 11 → **12**
  - `GET /health` 관측 문장의 근거 위치를 `http/global/health`로 정정(#63이 `http/api/health`에서 옮겼다)
- `server-baseline.md`
  - "현재 기준선" 커밋 → `6f5bffc`, 요약 → `[Feat/#45] 토큰 재발급(refresh) / 로그아웃 API 구현 (#63)`
  - 이력 표의 기존 `6b05b8c` 행은 **지우지 않는다**. 그 행 비고 끝에
    "체계 신설 도중 서버가 전진해 같은 라운드에서 `6f5bffc`로 올림"을 덧붙이고, `6f5bffc` 행을 새로 추가한다
    (비고: 패키지 재편 · 신규 엔드포인트 2 · `AuthErrorCode` 12종 · 화이트리스트 개별 3경로 축소 ·
    그룹 계약 불변).

**서버 소스** (`git -C <S> show origin/main:<path>`) — 경로는 `6f5bffc` 기준(#63이 `http/api/auth`에서 옮겼다):
- `http/src/main/kotlin/parfait/http/auth/controller/` — `KakaoLoginController.kt` · `SignupController.kt` · `ReissueController.kt` · `LogoutController.kt`
- `http/src/main/kotlin/parfait/http/auth/dto/` — `KakaoLoginRequest.kt` · `KakaoLoginResponse.kt` · `SignupRequest.kt` · `SignupResponse.kt` · `ReissueRequest.kt` · `ReissueResponse.kt` · `LogoutRequest.kt`
- `core/src/main/kotlin/parfait/core/auth/exception/AuthErrorCode.kt` (**12종**)
- `core/src/main/kotlin/parfait/core/auth/service/` — `KakaoLoginService.kt` · `SignupService.kt` · `ReissueService.kt` · `LogoutService.kt` (에러코드 귀속 조사용)

- [ ] **Step 1: 엔드포인트별 에러 코드 귀속 확정**

`AuthErrorCode` 11종 중 어느 것이 어느 엔드포인트에서 나오는지는 컨트롤러에 없다. core 계층을 읽어 확정한다.

Run:
```bash
git -C <S> ls-tree -r --name-only origin/main | grep -E "core/.*auth/"
```
그다음 `KakaoLoginUseCase`·`SignupUseCase` 구현체를 `git -C <S> show origin/main:<path>`로 읽어
`AuthErrorCode.<이름>`이 던져지는 지점을 수집한다.

**확정할 수 없는 코드는 도메인 공통 표에만 싣고 "엔드포인트 귀속 미대조"로 표기한다** — 추측해서 배치하지 않는다.

- [ ] **Step 2: `parfait/api/auth.md` 작성**

frontmatter:
```yaml
---
id: auth
title: 인증(카카오 로그인·회원가입)
server_module: http/api/auth
server_commit: 6b05b8c
verified: 2026-08-01
android_status: none
related_spec:
related_adr: ADR-0017
tags: [api, parfait, server-contract, auth]
---
```

엔드포인트 표(확정 데이터):

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/kakao` | 불필요(화이트리스트) | `KakaoLoginRequest` | `KakaoLoginResponse` | 미구현 |
| POST | `/api/v1/auth/signup` | 불필요(화이트리스트) | `SignupRequest` | `SignupResponse` | 미구현 |
| POST | `/api/v1/auth/reissue` | 불필요(화이트리스트) | `ReissueRequest` | `ReissueResponse` | 미구현 |
| POST | `/api/v1/auth/logout` | **필요** | `LogoutRequest` | 없음(204) | 미구현 |

⚠️ **`logout`만 화이트리스트에 없다.** #63이 `/api/v1/auth/**` 와일드카드를 `kakao`·`signup`·`reissue`
개별 3경로로 좁혔다. 이 비대칭을 표와 상세 절 양쪽에 명시한다.

`POST /api/v1/auth/reissue` · `POST /api/v1/auth/logout` 상세는 서버 DTO·컨트롤러를 직독해 채운다.
확인된 것: `ReissueResponse`는 `accessToken` String · `refreshToken` String · `expiresIn` Long,
`LogoutController`는 `@ResponseStatus(HttpStatus.NO_CONTENT)`다 — **204는 응답 본문이 없어
envelope 자체가 오지 않을 수 있다.** 컨트롤러 반환 타입을 확인해 "envelope 있음/없음"을 명확히 적어라.
Android 소비 시 `safeApiCallWithoutData`조차 파싱할 본문이 없을 수 있으므로 이 구분이 실제로 중요하다.

`POST /api/v1/auth/kakao` 상세:
- 성공: HTTP 200 · `code` = `"OK"` (`ApiResponse.ok`)
- 요청 필드: `idToken` String 필수(`@NotBlank`) / `nonce` String 필수(`@NotBlank`)
- 응답 필드: `isNewUser` Boolean · `accessToken` String? · `refreshToken` String? · `expiresIn` Long? · `registrationToken` String?
- **응답이 분기한다**: 기존 회원(`KakaoLoginResult.ExistingMember`) → `isNewUser=false` + `accessToken`·`refreshToken`·`expiresIn`,
  `registrationToken`은 `null`. 신규(`KakaoLoginResult.NewUser`) → `isNewUser=true` + `registrationToken`,
  나머지 셋은 `null`. **`isNewUser`가 어느 필드 묶음이 채워졌는지를 결정하는 판별자**임을 문서에 명시한다.

`POST /api/v1/auth/signup` 상세:
- 성공: HTTP **201** · `code` = `"CREATED"` (`ResponseEntity.status(CREATED)` + `ApiResponse.created`)
- 요청 필드: `registrationToken` String 필수(`@NotBlank`) / `agreements` List<`TermsAgreementRequest`>(`@Valid`),
  원소는 `termsId` Long · `agreed` Boolean
- 응답 필드: `accessToken` String · `refreshToken` String · `expiresIn` Long (**셋 다 널 아님** — 로그인 응답과 다르다)
- 흐름 메모: 카카오 로그인이 신규로 판정해 내려준 `registrationToken`을 이 API에 넘겨 가입을 끝낸다.

에러 코드 절에는 `AuthErrorCode` **12종** 표를 싣는다(HTTP·code·의미):
`UNAUTHORIZED`401 · `INVALID_TOKEN`401 · `EXPIRED_TOKEN`401 · `MEMBER_NOT_FOUND`401 · `INVALID_ID_TOKEN`401 ·
`KAKAO_JWKS_FETCH_FAILED`502 · `KAKAO_SERVER_UNAVAILABLE`503 · `ALREADY_REGISTERED`409 ·
`DUPLICATE_TERMS_ID`400 · `TERMS_NOT_FOUND`400 · `REQUIRED_TERMS_NOT_AGREED`400 ·
`FORBIDDEN_REFRESH_TOKEN`403(#63 신설).
Step 1에서 확정한 귀속만 엔드포인트별 표로 내리고, 나머지는 도메인 공통 표에 남긴다.

`## Android 매핑` 절: `없음` — `:data`에 인증 관련 Service·Response·DataSource가 없다.

`## 미결` 절: 토큰 저장소·갱신(refresh) 엔드포인트 부재 관측, `expiresIn` 단위(초/밀리초) 미확정.

- [ ] **Step 3: `expiresIn` 단위 확인**

Run: `git -C <S> grep -n "expiresIn" origin/main -- core http | head -20`
확인되면 문서에 단위를 적고, 코드로 확정 안 되면 `## 미결`에 남긴다(추측 금지).

- [ ] **Step 4: 서버 소스 1:1 재대조**

위 서버 소스 6개 파일을 `git -C <S> show origin/main:<path>`로 다시 열어 **필드명·타입·널 허용·HTTP 코드·경로**를
문서와 한 줄씩 대조한다. 어긋나면 문서를 고친다.

- [ ] **Step 5: `README.md` 인덱스 등록**

도메인 표에 한 행 추가:
```markdown
| [auth.md](auth.md) | `http/api/auth` | 2 (카카오 로그인 · 회원가입 완료) | 미구현 |
```

- [ ] **Step 6: 커밋 — 사용자 확인 후**

```bash
git add parfait/api/auth.md parfait/api/README.md
git commit -m "docs(parfait): api/auth 계약 — 카카오 로그인·회원가입"
```

---

### Task 3: `parfait-group.md` — 그룹 도메인 (8 엔드포인트)

**Files:**
- Create: `parfait/api/parfait-group.md`
- Modify: `parfait/api/README.md` (도메인 인덱스 표에 1행)

**Interfaces:**
- Consumes: Task 1의 `template.md` 형식, `conventions.md`
- Produces: 없음

**서버 소스** (`git -C <S> show origin/main:<path>`) — 경로는 `6f5bffc` 기준(#63이 하위 패키지로 나눴다):
- `http/src/main/kotlin/parfait/http/parfaitgroup/controller/ParfaitGroupController.kt`
- `http/src/main/kotlin/parfait/http/parfaitgroup/dto/ParfaitGroupRequest.kt`
- `http/src/main/kotlin/parfait/http/parfaitgroup/dto/ParfaitGroupResponse.kt`
- `http/src/main/kotlin/parfait/http/parfaitgroup/exception/ParfaitGroupApiErrorCode.kt`
- `core/src/main/kotlin/parfait/core/parfaitgroup/application/service/ParfaitGroupService.kt` ·
  `core/src/main/kotlin/parfait/core/parfaitgroup/domain/` (`ParfaitGroupError`·`GroupName`·`GroupNickname`·`GroupMemberLimit`) — 에러코드 귀속·유효성 규칙 조사용

> **#63은 이 도메인의 계약을 바꾸지 않았다** — 패키지 이동과 `ApiResponse.Companion.ok` 명시화뿐이다.
> 아래 엔드포인트·필드 데이터는 그대로 유효하되, 재대조는 위 새 경로로 한다.

- [ ] **Step 1: 엔드포인트별 에러 코드 귀속 확정**

`ParfaitGroupApiErrorCode`는 core `ParfaitGroupError`와 이름이 1:1이다(`valueOf(error.name)`).
core 서비스를 읽어 UseCase별로 어떤 `ParfaitGroupError`를 던지는지 수집한다.

Run:
```bash
git -C <S> ls-tree -r --name-only origin/main | grep "core/.*parfaitgroup"
```
각 UseCase 구현체를 `git -C <S> show origin/main:<path>`로 읽는다.
확정 못 한 코드는 도메인 공통 표에만 두고 "엔드포인트 귀속 미대조"로 표기한다.

- [ ] **Step 2: `parfait/api/parfait-group.md` 작성**

frontmatter:
```yaml
---
id: parfait-group
title: 파르페 그룹
server_module: http/parfaitgroup
server_commit: 6b05b8c
verified: 2026-08-01
android_status: none
related_spec:
related_adr: ADR-0017
tags: [api, parfait, server-contract, group]
---
```

엔드포인트 표(확정 데이터 — base path `/api/parfait-groups`, **전부 인증 필요**, Android 전부 `미구현`):

| 메서드 | 경로 | 성공 | 요청 | 응답 |
|---|---|---|---|---|
| GET | `/api/parfait-groups` | 200 `OK` | 없음 | `List<MyParfaitGroupResponse>` |
| GET | `/api/parfait-groups/{groupId}` | 200 `OK` | path `groupId` Long | `MyParfaitGroupDetailResponse` |
| GET | `/api/parfait-groups/join-preview` | 200 `OK` | query `inviteCode` String | `PreviewParfaitGroupJoinResponse` |
| POST | `/api/parfait-groups/join` | 200 `OK` | `JoinParfaitGroupRequest` | `JoinParfaitGroupResponse` |
| POST | `/api/parfait-groups` | **201 `CREATED`** | `CreateParfaitGroupRequest` | `CreateParfaitGroupResponse` |
| PATCH | `/api/parfait-groups/{groupId}/nickname` | 200 `OK` | `ChangeMyParfaitGroupNicknameRequest` | `ChangeMyParfaitGroupNicknameResponse` |
| DELETE | `/api/parfait-groups/{groupId}/members/me` | 200 `OK` | path `groupId` Long | `LeaveParfaitGroupResponse` |
| POST | `/api/parfait-groups/{groupId}/reports` | **201 `CREATED`** | `ReportParfaitGroupRequest` | `ReportParfaitGroupResponse` |

요청 타입 필드:
- `JoinParfaitGroupRequest`: `inviteCode` String
- `CreateParfaitGroupRequest`: `groupName` String · `groupNickname` String · `memberLimit` Int
- `ChangeMyParfaitGroupNicknameRequest`: `groupNickname` String
- `ReportParfaitGroupRequest`: `reason` String

응답 타입 필드:
- `MyParfaitGroupResponse`: `groupId` Long · `groupName` String · `recentImageUrl` String? · `recentImageUploadedAt` LocalDateTime?
- `MyParfaitGroupDetailResponse`: `groupId` Long · `groupNickname` String · `inviteCode` String · `members` List<`ParfaitGroupMemberResponse`>
- `ParfaitGroupMemberResponse`: `memberId` Long · `groupNickname` String
- `PreviewParfaitGroupJoinResponse`: `groupName` String
- `JoinParfaitGroupResponse`: `groupId` Long · `groupName` String
- `CreateParfaitGroupResponse`: `groupId` Long · `groupName` String · `inviteCode` String · `memberLimit` Int
- `ChangeMyParfaitGroupNicknameResponse`: `groupId` Long · `groupNickname` String
- `LeaveParfaitGroupResponse`: `groupId` Long
- `ReportParfaitGroupResponse`: `groupId` Long · `reportId` Long

에러 코드 표(`ParfaitGroupApiErrorCode` 11종):
`INVALID_INVITE_CODE`404 · `GROUP_ALREADY_JOINED`409 · `GROUP_MEMBER_LIMIT_REACHED`409 ·
`GROUP_NICKNAME_ALREADY_USED`409 · `INVALID_GROUP_NAME`400 · `INVALID_GROUP_NICKNAME`400 ·
`INVALID_GROUP_MEMBER_LIMIT`400(1~12) · `MEMBER_NOT_FOUND`404 · `GROUP_NOT_FOUND`404 ·
`GROUP_NOT_JOINED`403 · `INVALID_GROUP_REPORT_REASON`400.

**정책 대조 메모(문서에 포함)**: `memberLimit` 1~12는 위키 정책 "최대 12명"과 일치하고,
Android `GroupCreateConfig` 상한과도 같다. 그룹명·닉네임 유효성은 서버가 `INVALID_GROUP_NAME`·
`INVALID_GROUP_NICKNAME`으로 거절하지만 **규칙 본문(허용 문자·길이)은 http 계층에 없다** — core 대조 결과를 적고,
확인 안 되면 미결로 남긴다. 위키 정책은 그룹명 1~10자·닉네임 1~15자다.
링크는 걸지 않는다(위키 → 구현 단방향 규율. 구현 문서에서 위키 참조는 허용이나 여기선 **값만** 적어도 충분하다).

`## Android 매핑` 절: `없음`.

`## 미결` 절: 그룹명/닉네임 유효성 규칙 본문 위치, `recentImageUploadedAt`의 `LocalDateTime` 직렬화 포맷·타임존.

- [ ] **Step 3: `LocalDateTime` 직렬화 포맷 확인**

`MyParfaitGroupResponse.recentImageUploadedAt`이 `java.time.LocalDateTime`이라 Android가
파싱 포맷을 알아야 한다.

Run:
```bash
git -C <S> grep -nE "JavaTimeModule|WRITE_DATES|jackson|ObjectMapper" origin/main -- http bootstrap common | head -20
git -C <S> show origin/main:bootstrap/src/main/resources/application.yaml | grep -iA5 "jackson"
```
확정되면 포맷을 문서에 적고, 안 되면 `## 미결`에 남긴다(**추측 금지** — Android 파싱 실패의 직접 원인이 된다).

- [ ] **Step 4: 서버 소스 1:1 재대조**

컨트롤러의 매핑 애노테이션(경로·`@ResponseStatus`)과 DTO 필드를 문서와 한 줄씩 대조한다.
특히 **201을 쓰는 두 엔드포인트**(`POST /`·`POST /{groupId}/reports`)를 확인한다.

- [ ] **Step 5: `README.md` 인덱스 등록**

```markdown
| [parfait-group.md](parfait-group.md) | `http/parfaitgroup` | 8 (목록 · 상세 · 참여 미리보기 · 참여 · 생성 · 닉네임 변경 · 탈퇴 · 신고) | 미구현 |
```

- [ ] **Step 6: 커밋 — 사용자 확인 후**

```bash
git add parfait/api/parfait-group.md parfait/api/README.md
git commit -m "docs(parfait): api/parfait-group 계약 — 그룹 8 엔드포인트"
```

---

### Task 4: `parfait.md` — 파르페 조회 도메인 (1 엔드포인트)

**Files:**
- Create: `parfait/api/parfait.md`
- Modify: `parfait/api/README.md` (도메인 인덱스 표에 1행)

**Interfaces:**
- Consumes: Task 1의 `template.md` 형식, `conventions.md`
- Produces: 없음

**서버 소스** — 경로는 `6f5bffc` 기준(#63이 `http/api/parfait`에서 옮겼다):
- `http/src/main/kotlin/parfait/http/parfait/controller/ParfaitController.kt`
- `http/src/main/kotlin/parfait/http/parfait/dto/ParfaitYearsResponse.kt`

⚠️ 이 도메인은 **`main`에만 있다**(`develop`에는 없다). 반드시 `git -C <S> show origin/main:<path>`로 읽는다.
경로가 실제와 다르면 `git -C <S> ls-tree -r --name-only origin/main | grep parfait/` 로 실경로를 먼저 확인한다.

- [ ] **Step 1: `parfait/api/parfait.md` 작성**

frontmatter:
```yaml
---
id: parfait
title: 파르페(캔버스) 조회
server_module: http/api/parfait
server_commit: 6b05b8c
verified: 2026-08-01
android_status: none
related_spec:
related_adr: ADR-0017
tags: [api, parfait, server-contract, canvas]
---
```

엔드포인트 표:

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/v1/groups/{groupId}/parfaits/year` | 필요 | path `groupId` Long | `ParfaitYearsResponse` | 미구현 |

상세:
- 성공: HTTP 200 · `code` = `"OK"`
- 응답 필드: `years` List<Int> — 해당 그룹에 파르페(캔버스)가 존재하는 연도 목록
- 용도 메모: C-201 캘린더가 연도 선택지를 그릴 때 쓸 값이다. **단수 경로 세그먼트가 `year`인데 응답은
  목록(`years`)**이라는 관측 사실을 적는다.
- 경로 주의: 그룹을 `groups`로 부르는 유일한 경로다(다른 그룹 API는 `parfait-groups`) → `conventions.md` URL 절 참조.

`## Android 매핑` 절: `없음`.

`## 미결` 절: 경로 단수/복수 불일치, 그룹 미참여 시 에러 코드 미대조.

- [ ] **Step 2: 에러 코드 확인**

Run:
```bash
git -C <S> ls-tree -r --name-only origin/main | grep "core/.*parfait/" | grep -v parfaitgroup
```
`GetParfaitYearsUseCase` 구현(`ParfaitService`)을 읽어 던지는 에러가 있는지 본다.
전용 `ErrorCode` enum이 없으면 "전용 에러 코드 없음 — 인증 실패 시 `AuthErrorCode.UNAUTHORIZED`,
그 외 `CommonErrorCode.INTERNAL_SERVER_ERROR`"로 적는다.

- [ ] **Step 3: 서버 소스 1:1 재대조 + `README.md` 인덱스 등록**

```markdown
| [parfait.md](parfait.md) | `http/api/parfait` | 1 (그룹 캘린더 연도 리스트) | 미구현 |
```

- [ ] **Step 4: 커밋 — 사용자 확인 후**

```bash
git add parfait/api/parfait.md parfait/api/README.md
git commit -m "docs(parfait): api/parfait 계약 — 그룹 캘린더 연도 리스트"
```

---

### Task 5: 갱신 스킬 `sync-teamyg-server-api` + 기존 스킬 경계 명시

**Files:**
- Create: `.claude/skills/sync-teamyg-server-api/SKILL.md`
- Modify: `.claude/skills/sync-tjyg-develop-baseline/SKILL.md` (경계 문구 1블록 추가)

**Interfaces:**
- Consumes: Task 1의 `server-baseline.md`(기준선 SoT), Task 2~4의 도메인 문서
- Produces: 없음

- [ ] **Step 1: `.claude/skills/sync-teamyg-server-api/SKILL.md` 작성**

```markdown
---
name: sync-teamyg-server-api
description: TEAMYG-SERVER main 기준 parfait/api 계약 문서 점검(반복 워크플로). 사용자가 "/sync-teamyg-server-api", "서버 API 문서 점검", "서버 계약 갱신", "TEAMYG-SERVER delta 감사", "서버 API 바뀐 거 문서에 반영해줘"라고 할 때 사용. 기준선 이후 신규 커밋 delta만 감사해 parfait/api 계약 드리프트를 제거한다.
---

# sync-teamyg-server-api — 서버 main 기준 API 계약 문서 점검

TEAMYG-SERVER `main`에 새로 들어온 것과 `parfait/api/` 계약 문서의 드리프트를 제거한다.
**기준선(서버 main 커밋 해시)의 단일 출처는 `parfait/api/server-baseline.md`** — 절차 권위도 그 파일.
이 스킬은 실행 순서만 요약한다.

## 핵심 규율
- **전체 재감사 금지** — 기준선 이후 **신규 커밋 delta만** 본다.
- **브랜치는 `main`.** 서버 기본 브랜치가 main이고 기능 PR이 거기로 머지된다. `develop`은 뒤처진다.
  (TJYG-Android는 `develop` 추적 — 헷갈리지 말 것.)
- **워킹트리를 믿지 않는다.** 파일 조회는 항상 `git -C <S> show origin/main:<path>`.
- 로컬 절대경로는 개인정보 → `wiki/personal-private/project-paths.md`의 `TEAMYG-SERVER` 경로(아래 `<S>`).
- **서버 저장소는 read-only.** 커밋·브랜치·수정 일절 금지(fetch만 한다).
- 커밋/push/PR은 **CLAUDE.md 규율** — 사용자 확인 후. main 직접 금지, 브랜치→PR→머지.

## 단계

1. **기준선 확인** — `parfait/api/server-baseline.md` 읽어 현재 기준 커밋 확보.
2. **최신화 + delta 나열**:
   - `git -C <S> fetch origin main`
   - `git -C <S> log --oneline <기준선>..origin/main`
     — **`--merges` 필터를 쓰지 않는다.** 기능 PR이 squash로 들어와 merge 커밋이 아닐 수 있다.
   - 각 커밋: `git -C <S> show --stat <hash>`로 변경 파일 파악
   - delta 0건이면 기준선 해시만 갱신하고 종료 보고.
3. **계약 ↔ 문서 대조** — 아래가 바뀌었는지 본다:
   - 컨트롤러(`*Controller.kt`): 경로·메서드·`@ResponseStatus`·인증 사용 여부
   - DTO(`*Request.kt`·`*Response.kt`): 필드명·타입·널 허용·검증 애노테이션
   - 에러코드 enum(`*ErrorCode.kt`): 추가·삭제·status 변경
   - `SecurityConfig`(화이트리스트) · `ApiResponse`(envelope) · `GlobalExceptionHandler`(errorDetail 채움 여부)
   - **신규 도메인**이면 `parfait/api/template.md`로 문서 신설 + `README.md` 인덱스 등록.
   - 전역 계약이 바뀌었으면 `conventions.md` 갱신.
4. **드리프트 수정** — 해당 도메인 문서의 표·상세 절을 고치고 frontmatter `server_commit`·`verified` 갱신.
   Android 대응 심볼이 있는데 계약과 어긋나면 Android 열을 `⚠️불일치`로 바꾸고
   `parfait/synthesis/open-questions.md`에 `### [YYYY-MM-DD] 주제`로 등록한다.
5. **기준선 갱신** — `server-baseline.md` "현재 기준선"을 새 `origin/main` HEAD로 교체 + 이력 표에 1줄.
6. **보고 → 커밋**(사용자 확인 후) — delta 요약·드리프트 건수·조치 목록 보고.

## 경계 — 이 스킬이 하지 않는 것
- **Android 쪽 변화**(구현이 진행돼 `android_status`·Android 매핑 절이 바뀌는 경우)는
  `sync-tjyg-develop-baseline`이 잡는다. 나눔은 이렇다:
  - **서버 delta → 계약 절**(엔드포인트·필드·에러코드) = 이 스킬
  - **Android delta → `android_status`·Android 매핑 절** = `sync-tjyg-develop-baseline`
- TJYG-Android 코드 수정은 이 스킬의 일이 아니다.

## 주의
- 파르페 규율: 라인번호·변동수치·색 hex는 문서에 안 적는다. 근거는 파일명 + 심볼명.
- 확인 못 한 것을 추측해 적지 않는다 — `## 미결`에 남기고 `open-questions.md`에 등록한다.
```

- [ ] **Step 2: `sync-tjyg-develop-baseline/SKILL.md`에 경계 문구 추가**

`## 주의` 절 바로 앞에 아래 블록을 넣는다.

```markdown
## 경계 — 서버 API 계약과의 분담
`parfait/api/`의 **계약 절**(엔드포인트·요청/응답 필드·에러코드)은 서버가 정본이라
`sync-teamyg-server-api`가 갱신한다. 이 스킬은 그 문서의 **Android 쪽**만 본다 —
develop delta에 원격 연동 코드(Service·Response·RemoteDataSource·`ApiResponse`·`TokenProvider`)가
있으면 해당 도메인 문서의 `android_status`와 `## Android 매핑` 절, 엔드포인트 표의 Android 열
(`미구현`/`구현됨`/`⚠️불일치`)을 갱신한다.
```

- [ ] **Step 3: 스킬 로드 확인**

Run:
```bash
ls -1 .claude/skills/sync-teamyg-server-api/SKILL.md
head -4 .claude/skills/sync-teamyg-server-api/SKILL.md
```
Expected: 파일 존재 + frontmatter `name`·`description` 정상. (스킬 목록 반영은 세션 재시작 시점이라 이 Step에서는 파일 존재·형식만 확인한다.)

- [ ] **Step 4: 커밋 — 사용자 확인 후**

```bash
git add .claude/skills/sync-teamyg-server-api/SKILL.md .claude/skills/sync-tjyg-develop-baseline/SKILL.md
git commit -m "feat(skill): sync-teamyg-server-api 신설 + 기존 baseline 스킬 경계 명시"
```

---

### Task 6: 연동 — index 라우팅 · open-questions 등록 · private 경로 · 최종 검증

**Files:**
- Modify: `parfait/index.md` (라우팅 표 1행 + 문서 지도 1항목)
- Modify: `parfait/synthesis/open-questions.md` (신규 항목)
- Modify: `wiki/personal-private/project-paths.md` (**private submodule** — 별도 브랜치·PR)

**Interfaces:**
- Consumes: Task 1~5 산출물 전량
- Produces: 없음(최종 Task)

- [ ] **Step 1: `parfait/index.md` 라우팅 등록**

"무엇을 찾는가 → 어디를 보라" 표에 아래 행을 추가한다(원격 네트워크 행 바로 아래).

```markdown
| 서버 API 계약·엔드포인트·요청/응답 필드 | [api/README.md](api/README.md) + [api/conventions.md](api/conventions.md) |
```

"문서 지도"에 아래 항목을 추가한다(`architecture/` 다음).

```markdown
- **[`api/`](api/README.md)** — 서버(`mash-up-kr/TEAMYG-SERVER`) API 계약 스냅샷 + Android 적용 상태.
  정본은 서버 코드이고 이 디렉토리는 미러다. 추적 브랜치는 서버 **`main`**(TJYG-Android의 `develop`과 다름).
  기준선·갱신 절차는 [api/server-baseline.md](api/server-baseline.md), 반복 워크플로는 스킬 `sync-teamyg-server-api`.
```

"지금 상태 (1줄)"의 네트워크 문장 끝에 아래를 덧붙인다.

```markdown
서버 계약은 `api/`에 스냅샷돼 있고(도메인 3건), Android 대응 심볼은 아직 0건이다.
```

- [ ] **Step 2: `parfait/synthesis/open-questions.md`에 항목 등록**

파일의 기존 항목 형식(`### [YYYY-MM-DD] 주제`)을 그대로 따라 아래 4건을 추가한다.

1. `### [2026-08-01] 서버 응답 envelope와 Android ApiResponse 불일치` — 서버 `success`·`errorDetail` 필드를
   Android가 갖고 있지 않음. 근거: 서버 `parfait.common.response.ApiResponse`, Android
   `data/service/model/response/ApiResponse.kt`. 상태: 미해결(코드 수정 미착수).
2. `### [2026-08-01] Android 성공 코드 판정이 서버와 어긋남` — Android `isSuccess`가 `"SUCCESS"` 단일 비교인데
   서버는 `"OK"`/`"CREATED"` 2종 → 현 상태로 전 호출 실패 판정. 근거: `ApiResponse.SUCCESS_CODE`(TODO 상수),
   서버 `ApiResponse.ok`/`created`. 상태: 미해결.
3. `### [2026-08-01] TokenProvider 실구현 부재` — `EmptyTokenProvider`가 항상 null이라 화이트리스트
   (`/actuator/health`·`/swagger-ui**`·`/v3/api-docs/**`·`/api/v1/auth/**`) 밖 전 API가 401.
   근거: Android `EmptyTokenProvider`, 서버 `SecurityConfig`. 상태: 미해결.
4. `### [2026-08-01] 서버 URL 규약 3형태 혼재` — `/api/v1/auth/**` · `/api/v1/groups/{groupId}/parfaits/**` ·
   `/api/parfait-groups`. 버전 프리픽스 유무가 갈리고 그룹 경로가 `groups`/`parfait-groups` 둘.
   근거: 서버 `KakaoLoginController`·`SignupController`·`ParfaitController`·`ParfaitGroupController`.
   상태: 미해결(서버팀 확인 필요).

Task 2~4에서 미결로 남긴 항목(`expiresIn` 단위·`LocalDateTime` 포맷·그룹명/닉네임 규칙 위치·
`GET /health` 인증 대상·`errorDetail` 미채움·파르페 경로 단수/복수)도 확정되지 않았으면 함께 등록한다.

- [ ] **Step 3: private submodule에 서버 경로 추가**

`wiki/personal-private/`는 private repo(`team-yg-pesonal-agent-privacy-data`)의 submodule이다.
CLAUDE.md "Public repo 주의" 절차를 따른다 — **부모와 같은 이름의 브랜치**에서 작업.

```bash
cd wiki/personal-private
git checkout -b docs/parfait-api-contract-docs
```

`project-paths.md`의 표에 한 행 추가:
```markdown
| 서버 프로젝트 TEAMYG-SERVER | `<TEAMYG-SERVER 실경로 — private submodule에만 기록>` |
```
그리고 본문 끝의 안내 문장 아래에 한 줄 추가:
```markdown
서버 API 계약 문서(`parfait/api/`) 작업 시 조회 대상은 위 `TEAMYG-SERVER` 경로이며, **read-only**다.
```

commit + push + PR + 머지는 **사용자 확인 후**. 머지 뒤 로컬을 `main`으로 갱신하고 부모 repo에서
`git add wiki/personal-private`로 gitlink를 갱신한다.

- [ ] **Step 4: 전체 링크 resolve 검증**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
for f in parfait/api/*.md parfait/index.md; do
  d=$(dirname "$f")
  grep -oE '\]\(([^)]+\.md)[^)]*\)' "$f" | sed -E 's/^\]\(//; s/[)#].*$//' | while read -r l; do
    case "$l" in http*) continue;; esac
    [ -e "$d/$l" ] || echo "BROKEN: $f -> $l"
  done
done
```
Expected: 출력 없음.

- [ ] **Step 5: 민감정보 누출 검사**

Run:
```bash
cd "$(git rev-parse --show-toplevel)"
grep -rn "/Users/" parfait/ .claude/skills/sync-teamyg-server-api/ || echo "OK: 절대경로 없음"
grep -rniE "(secret|password|api[_-]?key|bearer [A-Za-z0-9])" parfait/api/ || echo "OK: 시크릿 패턴 없음"
```
Expected: 두 줄 모두 `OK: …`. 절대경로가 잡히면 `<TEAMYG-SERVER>` 플레이스홀더로 바꾼다.

- [ ] **Step 6: 스펙·계획 상태 갱신**

- `parfait/specs/2026-08-01-parfait-api-contract-docs.md` → `status: implemented`, `verified: <완료일>`,
  `specs/archive/`로 이동. 이동 시 **상대링크 `../` → `../../` 보정**.
- `parfait/plans/2026-08-01-parfait-api-contract-docs.md` → `status: done` + `archived_reason` 기입,
  `plans/archive/`로 이동(같은 링크 보정).
- `parfait/specs/README.md`·`parfait/plans/README.md`의 활성 행을 아카이브 표로 옮긴다.

- [ ] **Step 7: 커밋 — 사용자 확인 후**

```bash
git add parfait/index.md parfait/synthesis/open-questions.md parfait/specs parfait/plans wiki/personal-private
git commit -m "docs(parfait): api 계약 체계 연동 — index 라우팅·open-questions·스펙/계획 아카이브"
```

그다음 PR 생성도 **사용자 확인 후** 진행한다.

---

## 완료 기준

- `parfait/api/` 7파일이 존재하고 도메인 3건(엔드포인트 합계 **13** — auth 4 · group 8 · parfait 1)이
  서버 `main` `6f5bffc`와 1:1 대조를 마쳤다.
- `parfait/index.md`에서 `api/README.md`까지 라우팅이 이어진다.
- 스킬 `sync-teamyg-server-api`가 존재하고 `sync-tjyg-develop-baseline`과의 경계가 양쪽 문서에 적혀 있다.
- 링크 검사·민감정보 검사가 모두 통과한다.
- **TJYG-Android·TEAMYG-SERVER 저장소에 변경이 0건**이다.
- 불일치 3건 + URL 규약 1건이 `open-questions.md`에 등록돼 있다.

## 이 계획이 하지 않는 것

- Android 코드 수정(불일치 3건 해소) — 사용자가 문서 완료 후 별도 판단한다.
- 서버 실행·OpenAPI JSON 수집.
- `HealthController` 전용 도메인 문서(운영용, Android 미사용).
