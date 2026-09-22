---
id: parfait-group
title: 파르페 그룹
server_module: http/parfaitgroup
server_commit: 82e6edc
verified: 2026-09-11
android_status: done
related_spec: s101-group-setting-api
related_adr: ADR-0017
tags: [api, parfait, server-contract, group]
---

# 파르페 그룹 API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

base path `/api/parfait-groups`(버전 프리픽스 없음 — [conventions.md](conventions.md)의 URL 규약 관측 참고).
**8개 엔드포인트 전부 인증 필요.** 미인증 401 `UNAUTHORIZED`는 전역 공통이라 아래 표·상세에서 반복하지 않는다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/parfait-groups` | 필요 | 없음 | `List<MyParfaitGroupResponse>` | ⚠️불일치[^uploadedat][^todayurl] |
| GET | `/api/parfait-groups/{groupId}` | 필요 | path `groupId` Long | `MyParfaitGroupDetailResponse` | 구현됨·결선됨 |
| GET | `/api/parfait-groups/join-preview` | 필요 | query `inviteCode` String | `PreviewParfaitGroupJoinResponse` | 구현됨·결선됨 |
| POST | `/api/parfait-groups/join` | 필요 | `JoinParfaitGroupRequest` | `JoinParfaitGroupResponse` | 구현됨·결선됨 |
| POST | `/api/parfait-groups` | 필요 | `CreateParfaitGroupRequest` | `CreateParfaitGroupResponse` | 구현됨·결선됨 |
| PATCH | `/api/parfait-groups/{groupId}/nickname` | 필요 | `ChangeMyParfaitGroupNicknameRequest` | `ChangeMyParfaitGroupNicknameResponse` | 구현됨·결선됨 |
| DELETE | `/api/parfait-groups/{groupId}/members/me` | 필요 | path `groupId` Long | `LeaveParfaitGroupResponse` | 구현됨·결선됨 |
| POST | `/api/parfait-groups/{groupId}/reports` | 필요 | `ReportParfaitGroupRequest` | `ReportParfaitGroupResponse` | 구현됨·결선됨 |

[^uploadedat]: ✅ **해소됨(2026-08-20, PR #310 develop 머지).** 2026-08-15부터 매퍼가
    `recentImageUploadedAt`을 `kotlin.time.Instant::parse`(오프셋 필수)로 읽어, 오프셋 없는 로컬
    날짜시각을 주는 이 응답에서 던졌다. 2026-08-19 서버 `COALESCE` 비널화가 **마지막 우회로를
    없애면서**(그전에는 토핑 0건 그룹만 `null`이라 앱 매퍼의 `?.let`이 파싱을 건너뛰었다) 그룹이
    하나라도 있으면 G-001 목록 조회가 통째로 실패하는 상태가 됐고, 그 라운드가 매퍼를
    `LocalDateTime::parse` → `toInstant(PARFAIT_TIME_ZONE)`로 고쳤다. **VO 타입과 화면 계산은 안 바뀌었다** —
    바뀐 것은 벽시계에 어느 시간대를 붙이는가뿐이고, 근거는 서버 DB 커넥션 세 환경이
    `serverTimezone=Asia/Seoul`이라는 계약 사실이다. 앱 DTO는 여전히 널 허용이라 서버가 비널로 좁힌 것에
    앱이 물려 있지 않다 → [conventions.md](conventions.md) "Android 불일치"(이제 0건) ·
    [open-questions](../synthesis/open-questions.md) [2026-08-15].

[^todayurl]: ⚠️ **2026-08-31 서버 delta로 새로 벌어졌다.** `recentImageUrl`이 **오늘 캔버스의 토핑**으로
    좁혀졌는데, 앱은 그 필드가 `null`인 것을 여전히 "토핑이 하나도 없는 그룹"으로 읽는다
    (`MyParfaitGroupVO.recentImageUploadedAt` KDoc · `feature/groups/list/impl/util/ToppingImage.kt`의
    `toToppingImage`). 그래서 **어제까지 토핑이 있었고 오늘 캔버스만 빈 그룹**이 G-001에서 템플릿
    그래픽으로 그려지는데, 같은 줄의 경과 시간은 어제 토핑 시각을 가리킨다 — 두 표시가 서로를 반박한다.
    앱 쪽 타입도 JSON 키도 안 바뀌었고 서버가 같은 필드에 담는 **뜻**만 바뀐 부류라 역직렬화·매퍼는
    초록으로 지나간다 → [conventions.md](conventions.md) "Android 불일치" ·
    [open-questions](../synthesis/open-questions.md) OQ-P-336.

요청 DTO(`ParfaitGroupRequest.kt`)에는 Bean Validation 애노테이션이 없다 — auth 도메인과 달리 `@NotBlank`/`@Valid`가
없다. 필드는 Kotlin non-null 타입이라 요청 바디에 없거나 `null`이면 Jackson이 파싱 단계에서 거부한다(결과적으로
`CommonErrorCode.INVALID_REQUEST` 400). 실제 값 검증(길이·패턴·범위)은 컨트롤러 도달 후 도메인 값 객체
(`GroupName`·`GroupNickname`·`GroupMemberLimit`)가 담당하며, 위반 시 `ParfaitGroupApiErrorCode`로 나간다(아래
엔드포인트별 에러 코드 참고).

## 엔드포인트 상세

### GET /api/parfait-groups

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.Companion.ok`)
- **요청 필드**: 없음
- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |
| `groupName` | String | 아니오 | |
| `recentImageUrl` | String? | 예 | **오늘 캔버스**(`ParfaitDay.current()` — 03시 경계)에 토핑이 없으면 `null`. 어제 이전 토핑은 이 필드에 안 잡힌다(2026-08-31 변경) |
| `recentImageBorderType` | String(enum)? | 예 | **2026-09-11 신설.** `recentImageUrl`과 같은 토핑의 테두리 종류(`NONE`·`SOLID`). `recentImageUrl`이 `null`이면 `null`, 값이 있으면 널이 아니다 → 아래 |
| `recentImageBorderColor` | String? | 예 | **2026-09-11 신설.** 같은 토핑의 테두리 색. `borderType=SOLID`일 때만 값이 보장된다 |
| `recentImageBorderWidth` | Double? | 예 | **2026-09-11 신설.** 같은 토핑의 테두리 두께. `borderType=SOLID`일 때만 값이 보장되고, 범위는 서버가 검사하지 않는다 |
| `recentImageUploadedAt` | LocalDateTime | **아니오**(2026-08-19 변경) | 아래 직렬화 포맷 참고. **날짜와 무관하게 마지막 토핑 시각**이고, 토핑이 한 건도 없을 때만 **그룹 생성 시각**으로 대체된다(2026-08-31 변경) |
| `lastPlacedByNameTagChip` | String(enum) | **아니오**(2026-08-19 변경) | 마지막 토핑을 올린 사람의 Nametag-Chip 타입. 토핑이 없으면 **그룹 생성자의 칩** → 아래 [Nametag-Chip 배정 규칙](#nametag-chip-배정-규칙) |

  🔁 **2026-08-19 — 뒤의 두 필드가 널을 버렸다**(`[Fix] 그룹 Nametag-Chip 정합성 및 재가입·알림 버그 수정`).
  네이티브 쿼리(`findMyGroupSummaries`)의 두 서브쿼리에 `COALESCE`가 붙어, 토핑이 0건인 그룹은
  `recentImageUploadedAt`이 `parfait_group.created_at`으로, `lastPlacedByNameTagChip`이 **가장 먼저 참여한
  멤버(=생성자)의 칩**으로 대체된다. Kotlin 타입(`MyParfaitGroupSummary`·`MyParfaitGroupResult`·
  `MyParfaitGroupResponse`)과 프로젝션이 전부 비널로 좁혀졌다.
  ⚠️ **그래서 `recentImageUploadedAt`이 두 가지를 뜻한다** — "마지막 토핑 시각"이거나 "그룹 생성 시각"이고,
  ~~소비 측이 둘을 가르는 수단은 `recentImageUrl`이 `null`인지뿐이다~~(그 수단은 2026-08-31에 깨졌다 —
  바로 아래) → [미결](#미결).

  🔁 **2026-08-31 — 네 서브쿼리의 조인과 정렬이 바뀌었다**(`[Fix/#117] 오늘 토핑 없는 그룹의 "N시간 전"이
  그룹 생성 시각 기준으로 표시됨`). `findMyGroupSummaries`의 네 서브쿼리가 `parfait_image`를 **INNER
  JOIN**으로 바꿔 토핑이 없는 캔버스를 아예 제외하고, `ORDER BY`에서 `parfait_date`를 빼 `created_at`
  기준으로만 마지막 토핑을 고른다. 겸해서 `recentImageUrl` 서브쿼리에만 `parfait_date = :today` 조건이
  붙었고, 어댑터(`ParfaitGroupAdapter.findAllByMemberId`)가 `ParfaitDay.current()`를 넘긴다. 바뀐 계약
  사실은 셋이다.

  - `recentImageUrl`은 이제 **오늘 캔버스의 토핑**만 가리킨다. 어제 토핑이 있어도 오늘 캔버스가 비면 `null`이다.
  - `recentImageUploadedAt`은 **정말 마지막 토핑 시각**이 됐다. 그전에는 새벽 3시 회전 배치가 만든 빈 오늘
    캔버스가 `LEFT JOIN`으로 NULL 행을 만들고 그 행이 `parfait_date` 내림차순으로 맨 앞에 와, `COALESCE`가
    **그룹 생성 시각**까지 새어 나왔다 — 오늘 토핑이 아직 없는 그룹은 매일 아침 그룹 생성순으로 재정렬됐다.
  - 그래서 **`recentImageUrl`이 `null`인 것은 더 이상 "토핑 0건"을 뜻하지 않는다.** 응답에서 그 둘을 가르는
    수단이 사라졌다 → [미결](#미결).

  **목록 정렬(계약)**: 마지막 토핑 `created_at`(없으면 `parfait_group.created_at`)의 내림차순이고, 동률이면
  `parfait_group.id` 내림차순이다. 즉 마지막 토핑이 최근인 그룹이 위로 오고, 토핑이 한 건도 없는 그룹은 그룹
  생성 시각으로 줄을 선다. 앱은 응답 순서를 그대로 그린다.

  🔁 **2026-09-11 — 최신 사진의 테두리 세 필드가 붙었다**(`fix: 그룹 목록 응답에 최신 사진 테두리 정보 추가`,
  PR #139). `recentImageBorderType`·`recentImageBorderColor`·`recentImageBorderWidth`가 프로젝션
  (`MyParfaitGroupSummaryProjection`)부터 `MyParfaitGroupSummary`·`MyParfaitGroupResult`·
  `MyParfaitGroupResponse`까지 관통했다. 커밋 메시지가 밝힌 동기는 "클라이언트가 목록 썸네일에 테두리를
  그릴 수 없었다"는 것이다. 계약 사실은 넷이다.

  - **`recentImageUrl`과 같은 토핑을 가리킨다.** `findMyGroupSummaries`에 서브쿼리 셋이 새로 붙었는데, 셋 다
    `recentImageUrl` 서브쿼리와 조건(`parfait_date = :today`)도 정렬(`created_at` → `id` 내림차순)도 같다.
    그래서 오늘 캔버스가 비면 넷이 함께 `null`이고, **어제 토핑의 테두리는 이 응답에 없다**.
  - **`recentImageBorderType`은 `recentImageUrl`이 있으면 널이 아니다.** 컬럼 `parfait_image.border_type`이
    `NOT NULL`(V8)이라 행이 잡히면 값이 있다. 어댑터(`ParfaitGroupAdapter`)가 문자열을 `BorderType::valueOf`로
    되돌리고 Jackson이 다시 이름 문자열로 내보내므로, 와이어 값은 토핑 응답과 같은 `NONE`·`SOLID`다.
  - **색·두께는 `SOLID`일 때만 믿는다.** 배치(`ParfaitImage.place`)·재배치(`reposition`)·테두리 수정
    (`updateBorder`)이 모두 `validateBorder`를 거쳐 `SOLID`면 둘 다 있음을 보장한다. `NONE`이면 검사하지 않고
    보낸 값을 그대로 저장하므로 두 필드에 값이 남아 있을 수 있다([parfait-image.md](parfait-image.md) 배치 절).
    **소비 측은 `recentImageBorderType`을 먼저 봐야 한다.**
  - **두께 범위는 여전히 서버가 검사하지 않는다** — 캔버스 조회 응답의 `borderWidth`와 같은 사정이다
    ([conventions.md](conventions.md) "Android 불일치"의 `borderWidth` 행).

  근거: `ParfaitGroupControllerTest`가 테두리 있는 케이스(`"SOLID"`·`"#FFD54F"`·`6.0`)와 오늘 토핑이 없는
  케이스(세 필드 모두 `null`)를 `jsonPath`로 단언하고, `ParfaitGroupAdapterTest`가 프로젝션 문자열 →
  `BorderType` 변환을, `ParfaitGroupServiceTest`가 결과 매핑을 단언한다.

  🔁 **2026-08-19 — JSON 키가 `lastPlacedByNametagChip` → `lastPlacedByNameTagChip`으로 바뀌었다**
  (`fix: placedBy 스키마 이름 충돌 해소 및 nameTagChip 필드명을 스펙에 맞게 통일`). 서버 코어·도메인·영속성
  계층의 내부 프로퍼티명은 `nametagChip` 그대로이고 **HTTP 응답 DTO 경계에서만** 바뀌었다 — 계약 문서가
  보는 것은 후자다.

  **`lastPlacedByNameTagChip`의 출처와 함정**: `recentImageUrl`·`recentImageUploadedAt`과 같은 네이티브
  쿼리의 **별개 상관 서브쿼리**다. 정렬 기준(`created_at` → `id` 내림차순)이 `recentImageUploadedAt`과
  같아 그 시각과는 같은 토핑을 가리킨다. 🔁 **2026-08-31부터 `recentImageUrl`과는 갈린다** — 그쪽에만
  `parfait_date = :today`가 붙어, 오늘 캔버스가 빈 그룹은 **이미지가 `null`인데 칩은 어제 토퍼의 것**이다.
  조인 대상은 **배치 당시가 아니라 지금의 `parfait_group_member` 행**이라, 그 사람이 이미 탈퇴했으면
  `DEFAULT`가 내려온다 — 근거는 `placer` 조인만 `LEFT JOIN`으로 남고 `left_at` 필터가 없다는 쿼리 구조다
  (그 이유를 적어 두었던 쿼리 주석은 2026-08-31 커밋이 지웠다).
  근거: `ParfaitGroupControllerTest`가 토핑 있는 케이스(`"TYPE7"`)와 토핑 0건 케이스(`recentImageUrl`은
  `null`인데 시각·칩은 값이 있음, `"TYPE3"`)를 각각 `jsonPath`로 단언한다.

  응답은 `List<MyParfaitGroupResponse>`. nullable 필드도 값이 없다고 생략되지 않고 `null`로 내려온다
  (`jackson.default-property-inclusion: always`, `bootstrap/application.yaml`) — 이 응답에서 널이 될 수
  있는 필드는 `recentImageUrl`과 2026-09-11에 붙은 테두리 세 필드다. `recentImageUrl`이 `null`이면 넷이 함께
  `null`이고, 값이 있으면 `recentImageBorderType`도 값이 있으며 색·두께만 `NONE`일 때 널일 수 있다.

  **`recentImageUploadedAt`의 출처**: 애플리케이션 코드가 만든 값이 아니라, `persistence`
  모듈의 `ParfaitGroupMemberRepository.findMyGroupSummaries`(네이티브 쿼리, `MyParfaitGroupSummaryProjection`)가
  `parfait_image.created_at`을, 그것이 없으면 `parfait_group.created_at`을 프로젝션한 값이다. 여기서
  `parfait_image.created_at`은 **날짜 제한 없이** 그 그룹의 마지막 토핑이다(2026-08-31 변경).

  **직렬화 포맷(확인됨, 단 아래 한계 참고)**: `ParfaitGroupControllerTest`가
  `LocalDateTime.of(2026, 8, 1, 12, 0)` → 응답 문자열 `"2026-08-01T12:00:00"`을 `jsonPath`로 직접 검증한다.
  ISO-8601 로컬 날짜시간(`yyyy-MM-ddTHH:mm:ss`, 타임존 오프셋 없음) — 별도 `@JsonFormat`이나 커스텀
  `ObjectMapper` 빈이 없어 Jackson 3(`tools.jackson.module:jackson-module-kotlin`, Spring Boot 4.0.6) 기본
  직렬화로 보인다. **한계**: 이 포맷은 Jackson 설정을 직접 읽어 확정한 게 아니라 컨트롤러 테스트의 기대값에서
  역추론한 것이다 — 실제 직렬화기 동작 자체를 확인한 근거는 아니다.

  **타임존**: JSON에 오프셋은 실리지 않지만, `created_at` 컬럼 값은 MySQL 서버가 세션 타임존으로 저장·반환한
  값이고, 커넥션 문자열(`bootstrap/application-{dev,local,prod}.yaml`의 `spring.datasource.url`)이
  `serverTimezone=Asia/Seoul`을 세 환경 전부에서 지정한다. `hibernate.jdbc.time_zone: Asia/Seoul`
  (`bootstrap/application.yaml`)도 JDBC 드라이버가 `LocalDateTime`을 이 타임존으로 주고받도록 맞춘다.
  즉 `created_at`은 Asia/Seoul 벽시계 기준 값이라는 뜻이다. Android가 이 문자열을 UTC로 오인하면 시각이
  어긋난다.

- **에러 코드**: 없음(도메인 고유 에러 없음)

### GET /api/parfait-groups/{groupId}

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `groupId` | Long | 필수(path) | |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |
| `groupName` | String | 아니오 | **2026-08-18 신설**(`ParfaitGroup.name`) |
| `groupNickname` | String | 아니오 | 인증 회원 본인의 그룹 닉네임 |
| `inviteCode` | String | 아니오 | |
| `memberLimit` | Int | 아니오 | **2026-08-18 신설.** 그룹 정원(`GroupMemberLimit`, 1~12) |
| `members` | List<`ParfaitGroupMemberResponse`> | 아니오 | 원소: `memberId` Long · `groupNickname` String · `nameTagChip` String(enum)(**2026-08-18 신설**, 2026-08-19에 키가 `nametagChip`에서 바뀌고 비널이 됐다) |

  **`members`는 탈퇴하지 않은 멤버만**이다(`findAllByParfaitGroupIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc` —
  참여 순). 따라서 원소의 `nameTagChip`에는 `DEFAULT`가 오지 않고 `TYPE1`~`TYPE12` 중 하나이며,
  **같은 응답 안에서 값이 겹치지 않는다** → 아래 [Nametag-Chip 배정 규칙](#nametag-chip-배정-규칙).
  근거: `ParfaitGroupControllerTest`가 `groupName`·`memberLimit`·`members[0].nameTagChip`(`"TYPE3"`)을
  `jsonPath`로 단언한다.

  ✅ **2026-08-18 — 앱이 메우던 계약 공백 둘이 서버에서 닫혔다.** 그룹명(상세 응답에 없어
  `GetGroupDetailUseCase`가 목록을 한 번 더 읽어 붙이던 값)과 정원(생성 응답에만 있어 "N명 남음"이
  mock 1이던 값)이 이 응답에 실린다 → [Android 매핑](#android-매핑).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 404 | `GROUP_NOT_FOUND` | 존재하지 않는 그룹입니다 |
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹입니다 |

  근거: `ParfaitGroupService.get`이 `findGroupById`(→ `GROUP_NOT_FOUND`)·`findMembership`(→ `GROUP_NOT_JOINED`)을
  순서대로 호출한다.

### GET /api/parfait-groups/join-preview

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `inviteCode` | String | 필수(query, `@RequestParam` 기본값이 required) | 6자 영숫자(ASCII). 아래 초대코드 형식 참고 |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupName` | String | 아니오 | |

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 404 | `INVALID_INVITE_CODE` | 유효하지 않은 초대코드입니다 |
| 409 | `GROUP_ALREADY_JOINED` | 이미 참여한 그룹입니다 |
| 409 | `GROUP_MEMBER_LIMIT_REACHED` | 그룹의 최대 인원이 모두 참여했습니다 |
| 404 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |
| 400 | `INVALID_GROUP_NICKNAME` | 그룹 닉네임이 올바르지 않습니다(아래 참고) |

  근거: `ParfaitGroupService.preview`가 `findGroup`(→ `INVALID_INVITE_CODE`) 후 `validateJoin`을 호출한다.
  `validateJoin`은 `ParfaitGroup.validateJoin`(→ `GROUP_ALREADY_JOINED`·`GROUP_MEMBER_LIMIT_REACHED`)과
  `requireMemberNickname`(→ `MEMBER_NOT_FOUND`), `GroupNickname.of`(→ `INVALID_GROUP_NICKNAME`)를
  순서대로 실행한다 — `join`과 완전히 같은 private 함수를
  공유한다. `INVALID_INVITE_CODE`·`GROUP_ALREADY_JOINED`·`GROUP_MEMBER_LIMIT_REACHED`는
  `ParfaitGroupServiceTest`가 preview 경로로 직접 검증(`ParfaitGroupControllerTest`도 `GROUP_MEMBER_LIMIT_REACHED`
  409 응답을 확인). `MEMBER_NOT_FOUND`·`INVALID_GROUP_NICKNAME`은 preview 전용 테스트는 없지만 `validateJoin`
  공유 코드 경로로 확인했다.

  `INVALID_GROUP_NICKNAME`은 요청 바디가 아니라 **회원의 전역 닉네임**에 `GroupNickname.of`를 적용한 결과다
  (`requireMemberNickname`이 반환한 값을 그대로 검증) — 아래 [미결](#미결) 참고.

  🔁 **2026-09-10 — `GROUP_ALREADY_JOINED`의 조건이 "탈퇴하지 않은 멤버십"으로 좁아졌다**
  (`fix: 그룹 탈퇴 후 재참여가 불가능하던 문제 해결`). `alreadyJoined` 판정을 떠받치는 구현이
  `existsByParfaitGroupIdAndMemberId`에서 `existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull`로 바뀌어
  **탈퇴 이력만 있는 회원은 미리보기와 참여 양쪽을 다시 통과한다.** 직전까지는 탈퇴한 행이 남아 있는 것만으로
  409가 나와서 **재참여 자체가 불가능했다.** 정원 계산(`countByGroupId`)은 이미 `leftAt IS NULL` 기준이었으므로
  바뀐 것이 없다. 근거: `ParfaitGroupMemberRepositoryQueryTest`("탈퇴한 멤버는
  `existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull`이 false를 반환한다")·`ParfaitGroupAdapterTest`.

  🔁 **2026-08-15 — 그룹 내 닉네임 중복 검사가 사라졌다**(`fix: 그룹 내 닉네임 중복 검사 제거`).
  참여·닉네임 변경 양쪽에서 `existsByGroupIdAndNickname` 호출과 그 결과로 던지던
  `GROUP_NICKNAME_ALREADY_USED`가 제거됐고(포트·어댑터·리포지토리 메서드·에러 코드까지 함께 삭제),
  **같은 그룹 안에서 닉네임이 겹쳐도 허용**된다. 서버 설명은 "정책상 허용해야 한다"이나 대응 정책 문서는
  아직 없다 → [미결](#미결).

  > ⚠️ **2026-08-24 — 이 서술이 운영에서는 거짓이었다.** 코드에서 검사가 사라진 뒤에도 운영 DB에는
  > 유니크 인덱스 `uk_parfait_group_member_group_nickname`이 남아 있었다. V2가 만들고 V6이 지우는
  > 인덱스인데 **V6이 운영에 적용되지 않았고**, 검사를 걷어낸 코드가 그 위로 INSERT 하다
  > **처리되지 않은 500**을 냈다. 겹치는 닉네임으로 참여하거나 닉네임을 바꾸면 409가 아니라 500이었다는
  > 뜻이다. `V16`이 인덱스를 지워 코드와 운영을 맞췄다 — 다만 그 반영에는 사람이 실행하는 baseline
  > 절차가 선행한다([conventions.md 스키마 소유권](conventions.md#스키마-소유권--코드가-정본이어도-운영-응답은-다를-수-있다)).

### POST /api/parfait-groups/join

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `inviteCode` | String | 필수(non-null 타입) | 6자 영숫자(ASCII). 아래 초대코드 형식 참고 |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |
| `groupName` | String | 아니오 | |

- **에러 코드**: join-preview와 동일 5종(`INVALID_INVITE_CODE`·`GROUP_ALREADY_JOINED`·
  `GROUP_MEMBER_LIMIT_REACHED`·`MEMBER_NOT_FOUND`·`INVALID_GROUP_NICKNAME`) —
  `ParfaitGroupService.join`이 같은 `findGroup`·`validateJoin`을 호출한 뒤 멤버십을 저장한다. 코드 표는 위
  join-preview 절 참고(중복 서술 생략). 2026-08-15 이전에 있던 `GROUP_NICKNAME_ALREADY_USED`는
  중복 검사와 함께 삭제됐다(위 join-preview 절 참고).

- **재참여 동작(2026-09-10 신설)**: 탈퇴 이력이 있는 회원이 같은 그룹에 다시 참여하면 **새 멤버십 행을
  만들지 않고 기존 행을 재활성화한다**(`findAnyByGroupIdAndMemberId`로 탈퇴 여부와 무관하게 찾은 뒤
  `ParfaitGroupMember.rejoin`). 그 전이는 `leftAt`을 널로 되돌리고 `joinedAt`을 **재참여 시각으로 갱신**하며,
  `groupNickname`을 `validateJoin`이 돌려준 **회원의 전역 닉네임**으로 덮고, `nametagChip`을
  `assignNametagChip`으로 **다시 뽑는다.** 즉 탈퇴 전에 바꿔 둔 그룹 닉네임과 칩 타입은 **복원되지 않는다**
  (정책 문서에 재참여 규정이 없다 → [미결](#미결)). 반면 **멤버십 id(`groupMemberId`)는 그대로 유지된다** —
  이것이 그 사람의 과거 토핑 귀속을 되살린다([parfait.md](parfait.md)·[parfait-image.md](parfait-image.md)).
  `(parfait_group_id, member_id)`에 걸린 유니크 제약 때문에 INSERT로는 재참여가 성립하지 않았던 것이
  이 경로의 근거다. 근거: `ParfaitGroupServiceTest`("한 번 나갔던 멤버가 다시 참여하면 기존 멤버십 row를
  재활성화한다" — 저장된 행의 id 유지·`leftAt` 널·닉네임 덮어쓰기를 단언한다)·
  `RejoinAfterLeaveIntegrationTest`(MySQL 컨테이너로 참여 → 탈퇴 → 재참여를 실제로 돌린다).

#### 초대코드 형식

`InviteCode`(`core/parfaitgroup/domain/InviteCode.kt`) 값 객체가 판정한다.

- **길이 6**(`InviteCode.LENGTH`, 2026-08-15에 8에서 줄었다 — `refactor: 그룹 참여 코드 자릿수 8자에서 6자로 변경`,
  DB 컬럼도 마이그레이션 V13으로 같이 줄었다).
- 문자는 **ASCII 영숫자만**(`isLetterOrDigit` + `code > 127` 배제) — 한글·특수문자·공백 불가.
- 입력은 **대문자로 정규화**해 조회한다(`InviteCode.of`가 `uppercase()`) → 소문자 입력도 통한다.
- 위반은 값 객체 단계에서 404 `INVALID_INVITE_CODE`다(형식 오류와 "없는 코드"가 **같은 코드로 나간다**).
- 서버 생성 코드는 `InviteCodeGenerator`가 `SecureRandom`으로 뽑고 **알파벳에서 `I`·`O`·`0`·`1`을 뺐다**
  (혼동 방지). 즉 서버가 만드는 코드에는 그 넷이 없지만 **검증은 그것들도 받는다**.

### POST /api/parfait-groups

- **인증**: 필요
- **성공**: HTTP **201** · envelope `code` = `"CREATED"`(`@ResponseStatus(HttpStatus.CREATED)` +
  `ApiResponse.Companion.created`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `groupName` | String | 필수(non-null 타입) | 1~10자, 아래 정책 대조 참고 |
| `groupNickname` | String | 필수(non-null 타입) | 1~15자, 아래 정책 대조 참고 |
| `memberLimit` | Int | 필수(non-null 타입) | 1~12 |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |
| `groupName` | String | 아니오 | |
| `inviteCode` | String | 아니오 | 서버가 자동 생성 |
| `memberLimit` | Int | 아니오 | |
| `recentImageUrl` | String? | 예 | **2026-08-19 신설.** 갓 만든 그룹이라 **항상 `null`**(서비스가 리터럴로 넣는다) |
| `recentImageUploadedAt` | LocalDateTime | 아니오 | **2026-08-19 신설.** 방금 저장한 그룹의 `updatedAt` |
| `lastPlacedByNameTagChip` | String(enum) | 아니오 | **2026-08-19 신설.** 생성자에게 방금 배정된 칩 |

  🔁 **2026-08-19 — 생성 응답이 목록 응답의 세 필드를 얻었다.** 목록 카드를 그리는 데 필요한 값을
  생성 직후에도 갖게 하려는 변경이고, 그래서 `ParfaitGroupService.create`가 칩을 먼저 뽑아
  (`creatorNametagChip`) 멤버십 저장과 응답 양쪽에 쓴다.
  ⚠️ **같은 필드의 출처가 두 엔드포인트에서 다르다** — 생성은 `savedGroup.updatedAt`, 목록은
  `parfait_group.created_at`이다. 생성 직후에는 두 값이 같지만 그룹 행이 갱신되면 갈린다
  → [미결](#미결).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_GROUP_NAME` | 그룹명이 올바르지 않습니다 |
| 400 | `INVALID_GROUP_NICKNAME` | 그룹 닉네임이 올바르지 않습니다 |
| 400 | `INVALID_GROUP_MEMBER_LIMIT` | 그룹 최대 인원은 1명 이상 12명 이하여야 합니다 |
| 404 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |

  근거: `ParfaitGroupService.create`가 `GroupNickname.of(groupNickname)`(→ `INVALID_GROUP_NICKNAME`,
  요청 필드 검증) → `requireMember`(→ `MEMBER_NOT_FOUND`) → `ParfaitGroup.create` 내부의
  `GroupName.of(name)`(→ `INVALID_GROUP_NAME`)·`GroupMemberLimit.of(memberLimit)`(→
  `INVALID_GROUP_MEMBER_LIMIT`)를 순서대로 실행한다. 이 4종은 `ParfaitGroupServiceTest`의 성공 케이스로만
  간접 확인되고 실패(에러) 케이스 전용 테스트는 없다 — 코드 직독으로 확정, 테스트 근거는 없음.

### PATCH /api/parfait-groups/{groupId}/nickname

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `groupId` | Long | 필수(path) | |
| `groupNickname` | String | 필수(non-null 타입) | 1~15자, 아래 정책 대조 참고 |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |
| `groupNickname` | String | 아니오 | 변경된 닉네임(요청과 동일하면 저장 생략, 응답은 항상 최종 값) |

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 404 | `GROUP_NOT_FOUND` | 존재하지 않는 그룹입니다 |
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹입니다 |
| 400 | `INVALID_GROUP_NICKNAME` | 그룹 닉네임이 올바르지 않습니다 |

  근거: `ParfaitGroupService.change`가 `findGroupByIdForUpdate`(→ `GROUP_NOT_FOUND`) → `findMembership`(→
  `GROUP_NOT_JOINED`) → `membership.changeNickname`(내부 `GroupNickname.of`, → `INVALID_GROUP_NICKNAME`)
  순서로 실행하고, 값이 바뀐 경우에만 저장한다.

  🔁 **2026-08-15 — `GROUP_NICKNAME_ALREADY_USED`(409)가 사라졌다.** 같은 그룹의 다른 멤버와 같은 닉네임으로
  바꿔도 성공한다(위 join-preview 절 참고). 앱 분기·상수·문구는 PR #250에서 걷혔다 →
  [Android 매핑](#android-매핑).

### DELETE /api/parfait-groups/{groupId}/members/me

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `groupId` | Long | 필수(path) | |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 404 | `GROUP_NOT_FOUND` | 존재하지 않는 그룹입니다 |
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹입니다 |

  근거: `ParfaitGroupService.leave`가 `findGroupByIdForUpdate`(→ `GROUP_NOT_FOUND`) →
  `findMembership`(→ `GROUP_NOT_JOINED`) → `ParfaitGroupMemberLeavePort.leave` 순서로 실행한다. 탈퇴 시
  `groupNickname`은 `GroupNickname.unknown()`("(알수없음)")으로 대체되고 `leftAt`이 기록된다(`ParfaitGroupMember.leave`).
  **2026-08-18부터 `nameTagChip`도 같은 전이에서 반납된다**(반납 값의 이름은 2026-08-19에 `RELEASED`에서
  `DEFAULT`로 바뀌었다) → 아래 [Nametag-Chip 배정 규칙](#nametag-chip-배정-규칙).

  **2026-08-15 — 같은 전이를 부르는 두 번째 경로가 생겼다.** 회원 탈퇴(`DELETE /api/v1/users/me`,
  [member.md](member.md))가 그 회원의 **모든 그룹 멤버십에 같은 `leave()`를 적용**한다. 즉 이 그룹 API를
  거치지 않고도 멤버가 목록에서 사라질 수 있다.

  **2026-09-10 — 이 전이는 더 이상 최종 상태가 아니다.** 같은 회원이 같은 그룹에 다시 참여하면 이 행이
  `rejoin`으로 되살아난다(위 [참여 절](#post-apiparfait-groupsjoin)의 재참여 동작). 탈퇴가 비운 값 가운데
  **닉네임과 칩은 새 값으로 채워지고 멤버십 id는 유지된다.**

### POST /api/parfait-groups/{groupId}/reports

- **인증**: 필요
- **성공**: HTTP **201** · envelope `code` = `"CREATED"`(`@ResponseStatus(HttpStatus.CREATED)` +
  `ApiResponse.Companion.created`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `groupId` | Long | 필수(path) | |
| `reason` | String | 필수(non-null 타입) | 공백만 있는 값은 `INVALID_GROUP_REPORT_REASON`(`reason.isBlank()`) |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `groupId` | Long | 아니오 | |
| `reportId` | Long | 아니오 | |

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_GROUP_REPORT_REASON` | 신고 사유를 입력해 주세요 |
| 404 | `GROUP_NOT_FOUND` | 존재하지 않는 그룹입니다 |
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹입니다 |

  흐름 메모: `ParfaitGroupService.report`는 `ParfaitGroupReport.create`(reason 검증, → `INVALID_GROUP_REPORT_REASON`,
  다른 검사보다 먼저 실행) → `findGroupByIdForUpdate`(→ `GROUP_NOT_FOUND`) → `findMembership`(→
  `GROUP_NOT_JOINED`) → 신고 저장 → **같은 트랜잭션에서 신고자 멤버십을 탈퇴 처리**한다(`leave()`). 즉 신고
  성공은 항상 탈퇴를 동반한다. 근거: `ParfaitGroupServiceTest`("그룹 신고는 신고를 저장한 뒤 같은 트랜잭션에서
  탈퇴 이력을 남긴다"·"비어 있는 신고 사유는 신고나 탈퇴 없이 거부한다").

## Nametag-Chip 배정 규칙

2026-08-18 delta(`[Fix] 그룹/캔버스 API에 Nametag-Chip 노출&배치 작업 시간 수정`)로 **칩 타입의 부여
주체가 서버가 됐다.** 그전까지 응답에 타입 필드가 없어 앱이 목록 인덱스로 돌려 쓰던 자리다
([open-questions](../synthesis/open-questions.md) OQ-P-140·OQ-P-210).

- **값 집합**: `NameTagChipType`(`core/parfaitgroup/domain`) — `TYPE1`~`TYPE12` + `DEFAULT`.
  JSON에는 **enum 이름 문자열 그대로** 실린다(`"TYPE7"`). 위키 [[nametag-chip]]의 12종과 개수가 같고,
  `DEFAULT`는 그 12종 밖의 **반납 표식**이다(정책 문서에 대응 항목이 없다 → [미결](#미결)).
  🔁 **2026-08-19에 이 값의 이름이 `RELEASED`에서 `DEFAULT`로 바뀌었다**(마이그레이션 V15가 기존 행을
  갱신하고 컬럼을 `NOT NULL DEFAULT 'DEFAULT'`로 좁혔다). **`TYPE1`~`TYPE12`와 달리 유일성 제약이 없어
  같은 그룹 안에서 여러 명이 동시에 가질 수 있다**(서버 enum 주석이 이 성질을 명시한다).
- **널이 될 수 없다**(2026-08-19) — 도메인 `ParfaitGroupMember.nametagChip`·응답 DTO·JPA 컬럼이 전부
  비널로 좁혀졌다. "가입 시 항상 배정되고 탈퇴 시 항상 `DEFAULT`로 채워진다"는 불변식을 타입으로 옮긴
  것이다. 소비 측은 이 필드에서 `null`을 볼 일이 없다.
- **부여 시점**: 그룹 **생성**과 **참여** 두 경로 모두 `ParfaitGroupService.assignNametagChip`이
  `NameTagChipType.assignRandom`으로 뽑는다. 후보는 `ASSIGNABLE`(= 전체 − `DEFAULT`)에서 **그 그룹의
  탈퇴하지 않은 멤버가 이미 쓰는 값을 뺀 나머지**이고 그중 무작위다.
- **유일성 범위**: **그룹 안 · 활동 중인 멤버 사이에서만** 겹치지 않는다. 그룹이 다르면 같은 회원이 다른
  타입을 받고, 같은 타입이 다른 그룹에서 다른 사람에게 갈 수 있다. **계정 공통이 아니다** — 닉네임 초기값이
  계정 공통인 것과 반대다(위키 [[닉네임-자동-생성]]).
- **반납**: `ParfaitGroupMember.leave()`가 `DEFAULT`로 바꾼다. 그룹 탈퇴와 **회원 탈퇴**
  (`DELETE /api/v1/users/me`, [member.md](member.md)) 양쪽이 같은 전이를 부른다. 반납된 값은 후보에서
  빠지므로 **뒤에 들어오는 사람이 그 타입을 다시 받을 수 있다.**
- **응답에 실리는 자리는 넷**이다 — 그룹 상세 `members[].nameTagChip` · 그룹 목록·생성
  `lastPlacedByNameTagChip` · 캔버스 조회 `groupMembers[].nameTagChip`·`images[].placedBy.nameTagChip`
  ([parfait.md](parfait.md)) · 토핑 배치 응답 `placedBy.nameTagChip`([parfait-image.md](parfait-image.md)).
  뒤의 둘은 2026-08-19에 더해졌다.
- **닉네임 변경은 칩을 바꾸지 않는다**(`changeNickname`이 값을 그대로 넘긴다).
- **재배정 API는 없다.** 한번 받은 타입을 바꾸는 엔드포인트가 없다. **내부 재추첨 경로는 2026-09-10에 하나
  생겼다** — 재참여가 `rejoin`으로 기존 행을 되살릴 때 `assignNametagChip`을 다시 부른다. 직전까지는 재참여
  자체가 막혀 있어서 "그룹을 나갔다 다시 들어오면 새로 뽑힌다"는 서술이 실제로는 닿지 않는 경로였다.
- **기존 행**: 마이그레이션 `V14__add_nametag_chip_to_parfait_group_member.sql`이 활동 중인 멤버에게
  그룹별 무작위 순번으로 `TYPE1`~`TYPE12`를, 이미 탈퇴한 행에는 반납 값을 채웠고,
  `V15__rename_nametag_chip_released_to_default.sql`이 그 값을 `DEFAULT`로 갱신하며 컬럼을
  `NOT NULL DEFAULT 'DEFAULT'`로 바꿨다.
- **정원과의 결속**: 배정 가능 타입 12종과 `GroupMemberLimit.MAX`(12)가 같아야 마지막 멤버까지 배정된다
  (서버 코드가 주석으로 이 전제를 적는다). 후보가 비면 `check()`가 `IllegalStateException`을 던지므로
  도메인 에러 코드가 아니라 **500**이다 — 참여는 정원 검사(`GROUP_MEMBER_LIMIT_REACHED`)를 먼저 통과하므로
  정상 경로에서는 닿지 않는다.

근거: `NameTagChipTypeTest`·`ParfaitGroupMemberTest`·`ParfaitGroupServiceTest`·`ParfaitGroupAdapterTest`.

## 도메인 에러 코드 전수 — `ParfaitGroupApiErrorCode`(10종)

`ParfaitGroupApiErrorCode`는 core `ParfaitGroupError`와 이름이 1:1이다(`from(error) = valueOf(error.name)`,
[conventions.md](conventions.md)). 8개 엔드포인트 서비스 코드(`ParfaitGroupService`)와 도메인 값 객체
(`ParfaitGroup`·`ParfaitGroupMember`·`ParfaitGroupReport`·`GroupName`·`GroupNickname`·`GroupMemberLimit`)를 직독해
10종 전부의 귀속처를 확인했다 — "귀속 미대조"로 남길 항목은 없다.
**2026-08-15에 `GROUP_NICKNAME_ALREADY_USED`가 두 enum에서 함께 삭제돼 11 → 10이 됐다.**

| code | HTTP | 의미 | 귀속 |
|---|---|---|---|
| `INVALID_INVITE_CODE` | 404 | 유효하지 않은 초대코드입니다 | join-preview · join(형식 위반·없는 코드 공용) |
| `GROUP_ALREADY_JOINED` | 409 | 이미 참여한 그룹입니다 | join-preview · join |
| `GROUP_MEMBER_LIMIT_REACHED` | 409 | 그룹의 최대 인원이 모두 참여했습니다 | join-preview · join |
| `INVALID_GROUP_NAME` | 400 | 그룹명이 올바르지 않습니다 | 생성 |
| `INVALID_GROUP_NICKNAME` | 400 | 그룹 닉네임이 올바르지 않습니다 | 생성 · 닉네임 변경 · join-preview · join(전역 닉네임 경유, 근거는 각 절 참고) |
| `INVALID_GROUP_MEMBER_LIMIT` | 400 | 그룹 최대 인원은 1명 이상 12명 이하여야 합니다 | 생성 |
| `MEMBER_NOT_FOUND` | 404 | 존재하지 않는 회원입니다 | 생성(`requireMember`) · join-preview · join(`requireMemberNickname`) |
| `GROUP_NOT_FOUND` | 404 | 존재하지 않는 그룹입니다 | 상세 · 닉네임 변경 · 탈퇴 · 신고 |
| `GROUP_NOT_JOINED` | 403 | 참여하지 않은 그룹입니다 | 상세 · 닉네임 변경 · 탈퇴 · 신고 |
| `INVALID_GROUP_REPORT_REASON` | 400 | 신고 사유를 입력해 주세요 | 신고 |

⚠️ **`MEMBER_NOT_FOUND`는 코드 문자열이 유일하지 않다.** `AuthErrorCode`에도 같은 문자열이 존재하지만
값은 **401**로 다르다([auth.md](auth.md) "도메인 에러 코드 전수", [conventions.md](conventions.md)
"코드 문자열은 enum 간 유일하지 않다" 참고) — 소비 측은 이 문서의 **404**와 혼동하지 않도록 HTTP status를
함께 봐야 한다.

## 정책 대조 메모

- **`memberLimit` 1~12**(`GroupMemberLimit.MIN`·`MAX`, `core/parfaitgroup/domain/GroupMemberLimit.kt`)는
  위키 정책 "최대 12명"과 일치하고, Android `GroupCreateConfig` 상한과도 같다.
- **`groupName` 1~10자**(`GroupName.MAX_LENGTH`, `GroupName.kt`)는 위키 정책 "그룹명 1~10자"와 일치한다.
- **`groupNickname` 1~15자**(`GroupNickname.MAX_LENGTH`, `GroupNickname.kt`)는 위키 정책 "닉네임 1~15자"와
  일치한다.
- 두 값 객체(`GroupName`·`GroupNickname`)가 공유하는 문자 규칙: 정규식
  `^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$` — 완성형 한글·**자모 단독**·영문·숫자를 허용하고
  단어 사이 단일 스페이스만 허용한다(선행·후행 공백, 연속 공백, 그 외 특수문자 불가).
  🔁 **자모 범위는 두 번에 나눠 들어왔다.** 2026-08-15(`e4ff23f`, `[Fix] 닉네임 자모 허용 및 그룹 내 중복 검사
  제거`)에는 `GroupNickname`·`GlobalNickname`만 바뀌었고(사유는 iOS 클라이언트가 통과시키는 `ㅋㅋ`류가 서버에서만
  400이 되던 것), `GroupName`은 **2026-09-11**(`21d8bd1`, PR #137 `fix: 그룹명 검증에 한글 자음/모음 단독 입력
  허용`)에야 같은 범위를 얻었다(사유는 `ㅎㅇ` 같은 그룹명이 400 `INVALID_GROUP_NAME`으로 거부되던 것).
  ⚠️ **이 문서는 2026-08-15부터 그룹명도 자모를 받는다고 적어 왔는데, 그 서술은 틀렸다.** 그 기간에 앱
  `CheckNameValidUseCase`(A-005 그룹명·닉네임 공용)는 이미 자모를 통과시켜, 자모가 든 그룹명은 앱을 지나
  서버에서만 400이 됐다. 이제는 서버 값 객체 셋(`GroupName`·`GroupNickname`·`GlobalNickname`)과 앱이 같은
  집합이다. 위키 [[이름-입력-규칙]]은 "한글"의 범위를 정하지 않아 여전히 대조 근거가 없다 → [미결](#미결).
- **초대코드 자릿수 6**은 위키에 대응 정책 문서가 없다(앱 A-004도 코드로만 6을 확정했다) → [미결](#미결).
- **그룹 내 닉네임 중복 허용**(2026-08-15 서버 변경)도 위키 정책에 근거 항목이 없다 → [미결](#미결).
- `INVALID_GROUP_MEMBER_LIMIT`의 메시지("1명 이상 12명 이하")가 곧 규칙 본문이라 http 계층 어디에도 별도
  문서화가 없어도 서버 코드만으로 확정된다 — 브리프가 우려한 "규칙 본문이 http 계층에 없다"는 core 값
  객체(`GroupName`·`GroupNickname`·`GroupMemberLimit`)에서 전부 찾았다.

## Android 매핑

`:data`·`:domain`에 API 표면이 구현됐다([spec](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md)) —
**2026-08-06 PR #197로 develop 머지 완료**다. 이 표면이 딛고 선 공용 인프라(`ApiCaller` 4진입점·
`ApiResponse` envelope·`@NoAuth`·`TokenStoreTokenProvider`)는 PR #190으로 먼저 들어왔고, 아래
Service·DataSource·DTO·VO가 이번에 그 위에 올라갔다.
**2026-08-15 — Repository 경계가 먼저 들어왔다**(PR #241 `80895eb1`). `ParfaitGroupRepository`/
`ParfaitGroupRepositoryImpl`이 DataSource 8개 중 **5개**를 도메인에 올린다 —
`getMyGroups`·`previewJoin`·`joinGroup`·`createGroup`·`changeMyNickname`. 그룹 상세·탈퇴·신고는
**화면이 요구할 때까지 인터페이스에 올리지 않는다.** 화면 브랜치 셋(#233·#239·#240)이 각자 같은
4파일을 만들고 있어 충돌을 먼저 막은 것이고, `ServerErrorCode.ParfaitGroup` 8종도 같은 커밋이다.

**✅ 2026-08-15 — 다섯 함수 전부 화면까지 결선됐다**(PR #243·#244·#248). 선반영이던 Repository 경계가
같은 날 UseCase·ViewModel을 얻었다.

**✅ 2026-08-17 — 남은 셋도 올라왔고, 이 도메인이 `android_status: done`이 됐다**(PR #285·#287).
S-101 그룹 설정이 화면에서 요구하자 `getGroupDetail`·`leaveGroup`·`reportGroup`이 인터페이스에
올라왔다 — **DataSource 8함수 전량이 Repository를 얻었고 8 엔드포인트 전부 호출부가 있다**
([spec](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md)). "화면이 요구할 때 올린다"는 방침이
끝까지 지켜진 도메인이다.

| Repository 함수 | 반환 | 대응 엔드포인트 | UseCase → 화면 |
|---|---|---|---|
| `myGroups` / `refreshMyGroups()`(#307) | `Flow<List<MyParfaitGroupVO>?>` / `Result<Unit>` | GET `/api/parfait-groups` | `GetMyGroupsFlowUseCase`(구독) · `RefreshMyGroupsUseCase`(갱신) → G-001(**재진입마다**·당김 재조회, #297) · C-001이 그룹명을 같은 캐시에서 읽는다 |
| `previewJoin(inviteCode)` | `Result<GroupName>` | GET `/api/parfait-groups/join-preview` | `GetGroupJoinPreviewUseCase` → A-004(확인 버튼, 통과하면 S-102로 이동) |
| `joinGroup(inviteCode)` | `Result<JoinedGroupVO>` | POST `/api/parfait-groups/join` | `JoinGroupUseCase` → **S-102(모달 확인)** — #261에서 A-004에서 이관 |
| `createGroup(groupName, groupNickname, memberLimit)` | `Result<CreatedGroupVO>` | POST `/api/parfait-groups` | `CreateGroupUseCase` → A-005(모달 확인) |
| `changeMyNickname(groupId, groupNickname)` | `Result<GroupNicknameVO>` | PATCH `/api/parfait-groups/{groupId}/nickname` | `ChangeGroupNicknameUseCase` → S-102 · **S-101(#285)** |
| `groupDetail(groupId)` / `refreshGroupDetail(groupId)`(#285, #307) | `Flow<ParfaitGroupDetailVO?>` / `Result<Unit>` | GET `/api/parfait-groups/{groupId}` | `GetGroupDetailUseCase`(구독) · `RefreshGroupDetailUseCase`(갱신) → S-101 |
| `leaveGroup(groupId)`(#287) | `Result<GroupId>` | DELETE `/api/parfait-groups/{groupId}/members/me` | `LeaveGroupUseCase` → S-101(나가기 확인 팝업) |
| `reportGroup(groupId, reason)`(#287) | `Result<ReportedGroupVO>` | POST `/api/parfait-groups/{groupId}/reports` | `ReportGroupUseCase` → S-101(신고 확인 팝업) |
| `clearGroups()`(#307) | `Unit`(non-suspend) | — | `LogoutUseCase`(탈퇴가 위임하는 자리 포함) · `TokenAuthenticator` 강제 로그아웃 |

> ✅ **2026-08-20 — 읽기 두 갈래가 `Flow` 구독으로 바뀌었다**(PR #307 develop 머지). 엔드포인트·에러
> 코드는 그대로이고 **같은 응답을 어디에 두는가**만 달라졌다 — 화면이 조회 결과를 자기 State에 넣지
> 않고 `:data`의 인메모리 캐시를 구독한다. 갱신 함수가 `Result<Unit>`이라 **값을 얻는 두 번째 경로가
> 애초에 없다**([ADR-0023](../adr/0023-group-in-memory-ssot.md)). 명령 다섯(참여·생성·닉네임 변경·
> 나가기·신고)은 성공 시 캐시에 반영되므로 화면이 따로 재조회하지 않는다 — 단 닉네임 변경은 응답에
> `memberId`가 없어 **캐시의 "내" 항목을 짚지 못해 상세를 한 번 더 부른다**(계약 쪽 개선 여지).

앱 동작 메모(코드 대조):

- **에러 코드 7종이 실제 분기에 쓰인다** — `INVALID_INVITE_CODE`·`GROUP_ALREADY_JOINED`·
  `GROUP_MEMBER_LIMIT_REACHED`(🔁 #261 — **A-004 미리보기와 S-102 참여 양쪽**이 같은 문구로 매핑한다.
  미리보기를 통과한 뒤 상태가 바뀐 경우가 S-102 쪽이다) ·
  `INVALID_GROUP_NAME`·`INVALID_GROUP_MEMBER_LIMIT`·`MEMBER_NOT_FOUND`(A-005는 로그만) ·
  `INVALID_GROUP_NICKNAME`(🔁 #261 — S-102 분기가 사라져 **A-005 그룹 생성에서만** 쓰인다.
  S-102의 닉네임 PATCH 실패는 화면에 표시되지 않고 로그만 남는다).
  ✅ **`GROUP_NICKNAME_ALREADY_USED` 死코드는 걷혔다**(2026-08-15, PR #250) — 상수·
  `GroupNickNameError.ALREADY_USED`·문구·매핑 분기가 함께 제거됐고, 그 코드를 검증하던 두 테스트는
  `INVALID_GROUP_NICKNAME`(400)으로 바뀌어 살았다. **남은 것은 정책 쪽이다** — 같은 그룹에 같은 표시
  이름이 여럿일 때의 구분 수단이 없다 → [open-questions](../synthesis/open-questions.md) [2026-08-15].
- ✅ **닉네임 허용 문자가 다시 맞았다**(2026-08-15, PR #250) — `CheckNameValidUseCase`에
  `'ㄱ'..'ㅎ'`·`'ㅏ'..'ㅣ'`가 더해져 서버 정규식과 같은 집합이다. 앱이 서버보다 **좁아도 안 된다**는
  기준이 KDoc에 명시됐다(좁으면 서버가 받는 이름을 앱이 먼저 막는다). 정책 문서에는 여전히 자모 항목이
  없다 → [open-questions](../synthesis/open-questions.md) [2026-08-15].
  🔁 **그룹명 쪽은 2026-09-11에야 맞았다** — 같은 UseCase가 A-005 그룹명에도 쓰이는데 서버 `GroupName`은
  그날까지 완성형만 받아, 그 사이에는 **앱이 서버보다 넓었다**(위 [정책 대조 메모](#정책-대조-메모)).
  앱 코드는 바뀌지 않았고 서버가 따라와 닫혔다 → OQ-P-171.
- ✅ **초대코드 자릿수가 맞아떨어졌다** — 앱 `InviteCode.LENGTH`·A-004 입력 칸은 처음부터 6이었고 서버가
  이번에 8 → 6으로 내려왔다. 그전까지는 **앱이 보낸 코드가 서버 형식 검증을 통과할 수 없었다**(문서에
  서버 자릿수가 적혀 있지 않아 드러나지 않던 불일치다). 다만 앱은 대문자 정규화를 하지 않는다 —
  서버가 `uppercase()` 하므로 동작은 같다.
- **생성 응답을 한 번 더 검사한다** — `CreateGroupUseCase`가 `groupId > 0`이 아니면 계약 위반으로 보고 실패로 되돌린다.
- **참여 → 닉네임이 두 요청이다** — ✅ **이탈 문제는 해소됐다**(2026-08-16, PR #261): 둘 다 S-102 확인 모달
  뒤에서 연달아 나가므로 중간 이탈로 "닉네임 없는 참여"가 남지 않는다. 다만 원자적이지는 않다 —
  **POST join이 성공하고 PATCH만 실패하면 참여는 유지되고 닉네임은 서버 초기값**이며 화면에 표시가 없다
  → [open-questions](../synthesis/open-questions.md) [2026-08-15].
- ~~**A-005가 보내는 `groupNickname`이 아직 mock**이다~~ → ✅ **닫혔다**(2026-08-20, PR #312) —
  G-001이 `GetMyAccountFlowUseCase`를 구독해 **전역 닉네임**을 넘긴다. 그 값이 그룹 내 닉네임의
  초기값으로 서버에 저장되는 것은 위키 [[S-102-그룹-닉네임-생성-정책-v0.1]]의 "계정 공통 1개 값
  재사용"과 방향이 같다 → [open-questions](../synthesis/open-questions.md) OQ-P-197.
  ✅ **참여 갈래도 같아졌다**(2026-09-07, PR #461) — 그때 닫힌 것은 **생성 갈래(A-005)뿐**이었고,
  참여 갈래는 S-102 입력칸이 빈 채로 서서 사용자가 손으로 친 이름이 `PATCH nickname`으로 나갔다.
  이제 A-004가 같은 구독으로 앱 닉네임을 실어 보내 두 갈래가 같은 초기값에서 출발한다. **서버로
  나가는 값의 형태는 그대로다** — 참여는 여전히 `POST join` 뒤 `PATCH nickname` 두 요청이고, 이
  변경은 그 요청에 실릴 문자열의 출발점만 바꾼다. 앱 닉네임을 구하지 못하면 빈 초기값으로 서고
  두 갈래의 답이 갈린다 → [open-questions](../synthesis/open-questions.md) OQ-P-377.
- ~~⚠️ `recentImageUploadedAt` 파싱이 이 문서의 직렬화 포맷과 어긋난다~~ → ✅ **닫혔다**(2026-08-20,
  PR #310) — 매퍼가 `LocalDateTime::parse` 뒤 `toInstant(PARFAIT_TIME_ZONE)`로 KST를 부여한다.
  앱이 서버 포맷 변경을 기다리지 않고 읽는 쪽을 고쳤고, 근거는 서버 DB 커넥션 세 환경이
  `serverTimezone=Asia/Seoul`이라는 계약 사실이다 → [open-questions](../synthesis/open-questions.md) OQ-P-165.
- ⚠️ **`recentImageUploadedAt`이 이제 "그룹 생성 시각"일 수도 있다** — `GroupListScreen`이 이 값으로
  경과 시간을 그리므로, 토핑이 0건인 그룹도 **활동이 있었던 것처럼 보인다.**
  🔁 **2026-08-31 — 앱이 쓰던 판별법이 무효가 됐다.** 그전에는 `recentImageUrl`이 `null`인지를 함께 보면
  됐지만, 그 필드는 이제 **오늘 캔버스에 토핑이 있는지**를 뜻한다. `MyParfaitGroupVO.recentImageUploadedAt`
  KDoc과 `ToppingImage.kt`의 `toToppingImage`가 아직 옛 뜻으로 읽는다. 사정거리는 좁아졌다 — 시각이
  그룹 생성 시각으로 새는 그룹은 이제 **토핑이 한 건도 없는 그룹뿐**이다
  → [open-questions](../synthesis/open-questions.md) OQ-P-235 · OQ-P-336.
- ✅ **상세 조회 한 화면에 요청이 둘이던 이유가 서버에서 사라졌다**(2026-08-18 서버 delta) — 상세 응답에
  **그룹명이 없어** `GetGroupDetailUseCase`가 `getMyGroups()`를 한 번 더 읽어 붙이던 자리다(그룹 SSoT
  라운드에서 HTTP 호출이 먼저 사라져 인메모리 캐시 `combine`이 됐다). **앱 쪽도 닫혔다(2026-08-20,
  PR #308 develop 머지)** — `combine`과 `GroupDetailVO`가 함께 삭제되고 두 `TODO(서버 응답 확장 대기)`도
  걷혔다. 지금 `GetGroupDetailUseCase`는 상세 캐시 하나를 구독한다
  → [open-questions](../synthesis/open-questions.md) [2026-08-17].
- ✅ **`memberLimit` 공백도 닫혔다**(2026-08-18 서버 delta, 앱은 2026-08-20 PR #308 develop 머지) —
  정원이 **그룹 생성 응답에만** 있어 "N명 남음"이 mock 1로 남아 있던 자리다. 상세 응답이 `memberLimit`을
  실으면서 `GroupSettingViewModel`이 TODO에 적어 둔 식(`memberLimit - members.size`, 음수 클램프)으로
  바뀌었다 → [open-questions](../synthesis/open-questions.md) [2026-08-13].
- ✅ **칩 타입이 인덱스 순환을 완전히 대체했다**(2026-08-20, PR #308·#310 develop 머지). 그전까지는
  `GroupSettingViewModel`이 `NAMETAG_CHIP_TYPES[index % 12]`로, `GroupListScreen`이
  `YGGrouptagChipType.entries`를 순환으로 썼다. 지금은 S-101이 `members[].nameTagChip`,
  G-001이 `lastPlacedByNameTagChip`을 읽고 각 모듈 `util/`의 변환이 색으로 옮긴다 —
  **"멤버가 빠지면 남은 사람 색이 밀린다"는 성질이 사라졌다.** 값이 없거나 앱이 모르는 문자열은
  `NametagChipType.DEFAULT`로 접혀 중립 색이 된다([ADR-0024](../adr/0024-nametag-chip-unknown-fold.md))
  → [open-questions](../synthesis/open-questions.md) [2026-08-18].
- ✅ **JSON 키 어긋남도 develop에서 닫혔다.** 2026-08-19 서버 delta가 키를 `nameTagChip` 계열로 바꿀 때
  그 필드를 옛 키(`lastPlacedByNametagChip`·`nametagChip`, 둘 다 `String? = null`)로 읽던 코드가 잠시
  있었다 — 기본값이 있어 역직렬화는 안 깨지고 **전부 `null`로 떨어지는**(칩이 조용히 폴백 색으로
  그려지는) 부류였다. PR #310이 세 DTO의 키를 맞추고 `RELEASED`를 `DEFAULT`로 바꾼 상태로 머지됐고,
  develop DTO는 이제 `MyParfaitGroupResponse.lastPlacedByNameTagChip` ·
  `ParfaitGroupMemberResponse.nameTagChip` ·
  `CreateParfaitGroupResponse.lastPlacedByNameTagChip`이다
  → [server-delta 스펙](../superpowers/specs/archive/2026-08-19-server-delta-nametag-chip-keys.md) ·
  [open-questions](../synthesis/open-questions.md) [2026-08-19].
- ⚠️ **신고 사유가 하드코딩 상수 하나**다(`GROUP_REPORT_REASON`) — 사유 선택 UI가 없는데 서버는
  사유를 필수로 받으므로(빈 값이면 400 `INVALID_GROUP_REPORT_REASON`) 화면이 대신 채운다. 결과적으로
  **모든 신고가 같은 문자열로 저장된다** → [open-questions](../synthesis/open-questions.md) [2026-08-17].
- **신고 성공은 탈퇴를 동반한다는 서버 동작을 앱이 그대로 받는다** — 나가기와 신고가 같은 함수
  (`submitDialogAction`)로 모이고 성공하면 둘 다 `replaceAll(NavKeyGroupList)`로 그룹 목록에 간다.
  나간 뒤에는 그 그룹의 상세·닉네임 변경·신고가 전부 403 `GROUP_NOT_JOINED`라 백스택을 남기지 않는다.
- **에러 코드 분기는 늘지 않았다** — S-101은 `INVALID_GROUP_NICKNAME`만 문구를 갖고
  `GROUP_NOT_FOUND`(404)·`GROUP_NOT_JOINED`(403)는 `UNKNOWN`으로 접힌다. 즉 **이미 나간 그룹을 다시
  여는 상황과 일시 장애가 같은 문구**다 → [open-questions](../synthesis/open-questions.md) [2026-08-17].

| 엔드포인트 | Service 함수 | DataSource 함수 |
|---|---|---|
| GET `/api/parfait-groups` | `ParfaitGroupService#getParfaitGroups` | `ParfaitGroupRemoteDataSource#getMyGroups` |
| GET `/api/parfait-groups/{groupId}` | `ParfaitGroupService#getParfaitGroupsByGroupId` | `ParfaitGroupRemoteDataSource#getGroupDetail` |
| GET `/api/parfait-groups/join-preview` | `ParfaitGroupService#getParfaitGroupsJoinPreview` | `ParfaitGroupRemoteDataSource#previewJoin` |
| POST `/api/parfait-groups/join` | `ParfaitGroupService#postParfaitGroupsJoin` | `ParfaitGroupRemoteDataSource#joinGroup` |
| POST `/api/parfait-groups` | `ParfaitGroupService#postParfaitGroups` | `ParfaitGroupRemoteDataSource#createGroup` |
| PATCH `/api/parfait-groups/{groupId}/nickname` | `ParfaitGroupService#patchParfaitGroupsByGroupIdNickname` | `ParfaitGroupRemoteDataSource#changeMyNickname` |
| DELETE `/api/parfait-groups/{groupId}/members/me` | `ParfaitGroupService#deleteParfaitGroupsByGroupIdMembersMe` | `ParfaitGroupRemoteDataSource#leaveGroup` |
| POST `/api/parfait-groups/{groupId}/reports` | `ParfaitGroupService#postParfaitGroupsByGroupIdReports` | `ParfaitGroupRemoteDataSource#reportGroup` |

- **요청 DTO**: `JoinParfaitGroupRequest`·`CreateParfaitGroupRequest`·`ChangeMyParfaitGroupNicknameRequest`·
  `ReportParfaitGroupRequest` — `data/service/model/request/group/` 패키지, 선언당 파일 하나(파일명은
  선언명과 동일). 이 도메인은 타입이 많아 이하 DTO/VO 절도 개별 파일명 대신 패키지+규약으로 적는다.
- **응답 DTO**: `MyParfaitGroupResponse`·`MyParfaitGroupDetailResponse`·`ParfaitGroupMemberResponse`·
  `PreviewParfaitGroupJoinResponse`·`JoinParfaitGroupResponse`·`CreateParfaitGroupResponse`·
  `ChangeMyParfaitGroupNicknameResponse`·`LeaveParfaitGroupResponse`·`ReportParfaitGroupResponse` —
  `data/service/model/response/group/` 패키지, 선언당 파일 하나(9개).
- **VO/value class**: `MyParfaitGroupVO`·`ParfaitGroupDetailVO`·`ParfaitGroupMemberVO`·`JoinedGroupVO`·
  `CreatedGroupVO`·`GroupNicknameVO`·`ReportedGroupVO`·`InviteCode`·`GroupName`·`GroupNickname` —
  `domain/model/group/` 패키지, 선언당 파일 하나(10개. 예전엔 `ParfaitGroupVO.kt`·`GroupValues.kt` 두
  파일에 묶여 있었으나 지금은 각 선언이 동명 파일로 분리돼 있다). join-preview·탈퇴는 응답이 필드
  하나뿐이라 래퍼 VO 없이 `GroupName`·`GroupId`를 그대로 반환한다.
  📌 **2026-08-17(PR #285)에 `GroupDetailVO`가 더해져 11개다** — 이 하나만 **서버 응답에 1:1로
  대응하지 않는다.** 상세(`ParfaitGroupDetailVO`)에 목록에서 가져온 `groupName`을 붙인 조합 결과이고,
  그래서 `data/source/group/mapper/VOMapper.kt`가 아니라 `GetGroupDetailUseCase`가 만든다.
- **Mapper**: `data/source/group/mapper/VOMapper.kt`(응답별 `toMyParfaitGroupVO`·
  `toParfaitGroupDetailVO`·`toParfaitGroupMemberVO`·`toJoinedGroupVO`·`toCreatedGroupVO`·
  `toGroupNicknameVO`·`toReportedGroupVO`·`toGroupName`·`toGroupId`). `recentImageUploadedAt`은
  이 mapper가 변환한다. 🔁 **2026-08-15(PR #248)에 `kotlinx.datetime.LocalDateTime.parse()` →
  `kotlin.time.Instant::parse`로 바뀌었고**(VO 타입도 `Instant?`), "오프셋(`Z`)째로 읽는다"는 주석이 붙었다 —
  Asia/Seoul 벽시계 전제를 없애려는 변경이다. **그런데 이 문서의 직렬화 포맷 절이 근거로 삼는 서버
  컨트롤러 테스트의 기대값은 오프셋 없는 `2026-08-01T12:00:00`이라, 그 문자열은 `Instant.parse`가 받지
  못한다** → [open-questions](../synthesis/open-questions.md) [2026-08-15].

## 미결

✅ **회원 전역 닉네임과 그룹 닉네임 규칙 대조 — 2026-08-11 해소.** 두 값 객체가 **문자 그대로 같은
규칙**이다: `core/member/domain/GlobalNickname`과 `core/parfaitgroup/domain/GroupNickname` 모두
`MAX_LENGTH = 15`, 패턴 `^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$`(2026-08-15 자모 추가도
**두 객체 동시**였다), 길이 검사 `1..MAX_LENGTH`.
다른 것은 위반 시 던지는 코드(`INVALID_NICKNAME` vs `INVALID_GROUP_NICKNAME`)와 `GroupNickname.unknown()`
센티널의 존재뿐이다. 따라서 join-preview·join이 회원의 전역 닉네임에 `GroupNickname.of`를 적용해도
**정상 경로에서는 통과한다** — 우려했던 "본인 입력과 무관한 `INVALID_GROUP_NICKNAME`"은 발생하지 않는다.

> ⚠️ 예외 하나. `GroupNickname.unknown()`이 만드는 `(알수없음)`은 괄호를 포함해 **자기 패턴을 통과하지
> 못하는 값**이다.
>
> 🔁 **2026-08-15 — 그 경로가 실제로 생겼고 서버가 특례로 막았다.** 회원 탈퇴가 멤버십을 `leave()`로
> 바꾸면서 `(알수없음)` 행이 DB에 남고, 그 행을 도메인으로 재구성할 때 `GroupNickname.of`가 걸려 터졌다
> (`fix: 탈퇴 멤버 닉네임 재구성 시 GroupNickname 검증 실패 수정`). 지금 `of`는 **입력이 정확히
> `(알수없음)`이면 검증을 건너뛰고 통과시킨다.** 그런데 `of`는 **사용자 입력에도 그대로 쓰인다** —
> 그룹 생성(`ParfaitGroupService.create`)과 닉네임 변경(`ParfaitGroupMember.changeNickname`) 양쪽이다.
> 즉 사용자가 `(알수없음)`을 입력하면 괄호 금지 규칙을 우회해 **탈퇴자와 같은 표시 이름**을 가질 수 있다
> → [open-questions](../synthesis/open-questions.md).

전역 닉네임을 바꾸는 API는 [member.md](member.md)에 있다.

2026-08-15 서버 delta로 새로 열린 것 셋:

- 같은 그룹 안 닉네임 중복이 허용됐는데 **정책 문서에 근거가 없다** — 서버 커밋 메시지의 "정책상 허용"만
  근거다. 앱 S-102는 아직 중복 에러 문구를 갖고 있다 → [open-questions](../synthesis/open-questions.md)
- 닉네임 허용 문자에 자모가 들어왔는데 위키 [[이름-입력-규칙]]은 "한글"의 범위를 정하지 않는다.
  앱은 완성형만 통과시켜 **서버보다 좁다** → [open-questions](../synthesis/open-questions.md)
- 초대코드 자릿수 6이 서버·앱 코드 양쪽에만 있고 정책 문서에 없다 →
  [open-questions](../synthesis/open-questions.md)

2026-08-18 서버 delta로 새로 열린 것:

- **Nametag-Chip 배정 규칙이 서버 코드에만 있다.** 위키 [[nametag-chip]]은 "타입은 유저별 고정"이라고만
  적고 부여 주체·유일성 범위를 정하지 않았는데, 서버가 **그룹별 무작위·활동 멤버 사이 유일**로 구현했다
  (계정 공통이 아니다). `DEFAULT`라는 13번째 값도 정책에 없다 →
  [open-questions](../synthesis/open-questions.md)
- **`DEFAULT`를 받는 소비 측 처리가 정해지지 않았다.** 그룹 상세 `members`에는 안 오지만
  **목록·생성의 `lastPlacedByNameTagChip`에는 온다**(마지막 토퍼가 탈퇴한 경우). 앱 `YGColorChipType`은
  12종 + `NametagChipPlus` + `Default`뿐이고 그 `Default`의 색 구분·대비도 미결이다
  → [open-questions](../synthesis/open-questions.md)
  ✅ **앱 쪽 처리는 정해졌다(2026-08-20, PR #310 develop 머지)** —
  `DEFAULT`는 중립 색(`YGColorChipType.Default`·`YGGrouptagChipType.DEFAULT`)으로 그리고,
  **앱이 모르는 문자열과 값 없음도 같은 값으로 접는다**([ADR-0024](../adr/0024-nametag-chip-unknown-fold.md)).
  그 대가로 "서버가 늘린 새 타입"과 "반납된 자리"가 앱에서 구분되지 않는다.
  **남은 미결은 디자인 몫**이다 — 그 중립 색의 구분·대비가 정해지지 않았고, 갈라지는 순간
  ADR-0024의 재검토 트리거가 걸린다.

2026-08-19 서버 delta로 새로 열린 것 둘:

- **`recentImageUploadedAt`이 두 뜻을 겸한다** — 토핑이 없으면 그룹 생성 시각으로 대체돼, "마지막 활동"
  표시가 활동 없는 그룹에도 나온다 → [open-questions](../synthesis/open-questions.md)
- **같은 필드가 목록과 생성에서 다른 컬럼에서 나온다**(`parfait_group.created_at` vs `updatedAt`)
  → [open-questions](../synthesis/open-questions.md)

2026-08-31 서버 delta로 새로 열린 것:

- **`recentImageUrl`이 오늘 캔버스로 좁혀지면서 "토핑 0건" 판별 수단이 사라졌다** — 앱 두 곳
  (`MyParfaitGroupVO` KDoc · `toToppingImage`)이 아직 옛 뜻으로 읽어, 어제까지 활동한 그룹이 G-001에서
  템플릿 그래픽으로 그려진다. 위키 [[토핑]]의 대체 그래픽 정책은 템플릿을 "첫 토핑 등록 전까지"로 적어
  **정책과도 갈린다** → [open-questions](../synthesis/open-questions.md) OQ-P-336

2026-09-10 서버 delta로 새로 열린 것:

- **재참여 규정이 정책 문서에 없다.** 서버가 재참여를 열면서 **그룹 닉네임 초기화·칩 재추첨·멤버십 id 유지**
  라는 세 가지를 한꺼번에 정했는데, 위키 [[그룹]]·[[nametag-chip]]·[[닉네임-자동-생성]] 어디에도 "나갔다
  다시 들어온 사람"에 대한 조항이 없다. 특히 **멤버십 id가 유지되므로 탈퇴 중 `(알수없음)`·`DEFAULT`로
  보였던 그 사람의 과거 토핑이 재참여 후 새 닉네임·새 칩으로 되살아난다** — 그것이 의도인지 확인되지
  않았다 → [open-questions](../synthesis/open-questions.md) OQ-P-398

2026-09-11 서버 delta로 새로 열린 것:

- **목록 썸네일 테두리를 앱이 읽고 그린다.** 서버가 테두리 세 필드를 주기 시작해
  OQ-P-316 ①("서버가 목록 응답에 테두리 필드를 줄지")이 서버 쪽에서 닫혔다. 서버 delta 당일에는 앱에 대응
  필드가 없었고, `ignoreUnknownKeys = true`라 역직렬화가 안 깨져 `⚠️불일치`로 세지 않았다
  → [open-questions](../synthesis/open-questions.md) OQ-P-316
  ✅ **데이터 계층 수용이 develop에 들어왔다**(2026-09-11, PR #496 `1b21725ba`, 머지 트리 = 브랜치 팁).
  `MyParfaitGroupResponse`가 세 키를 읽고 `MyParfaitGroupVO.recentImageBorder`(`ToppingBorder`, 비널)로 접는다.
  접는 규칙은 캔버스·토핑 매퍼와 같은 공용 함수(`data/source/common/mapper/ToppingBorderMapper.kt`의
  `toToppingBorder`)라 **두께도 `solidClamped`로 가둔다** — [conventions.md](conventions.md) "Android 불일치"의
  `borderWidth` 행이 이 응답까지 넓어졌다. `ParfaitGroupRemoteDataSourceImplTest`가 네 케이스(`SOLID` ·
  `NONE`인데 색·두께가 남음 · `SOLID`인데 두께 없음 · 이미지 없음)를 잠근다.
  ✅ **렌더도 develop에 들어왔다**(2026-09-16, PR #497 `a1fc2377f`, 머지 트리 = 브랜치 팁). `YGToppingImage.Remote`가
  `border`를 싣고 `YGToppingGroup`이 `YGToppingCutoutImage`로 그린다. **앱이 이 응답을 그릴 때 보는 조건은
  둘**이다 — `recentImageBorderType`이 `SOLID`이고, 색 문자열이 파싱되는 것. 어느 하나라도 아니면 테두리 없이
  그린다(`feature/groups/list/impl/util/ToppingImage.kt`). 두께는 이미 데이터 계층이 `solidClamped`로 가둬 두어
  feature가 다시 가두지 않는다 → [g001-group-list-topping-border 스펙](../superpowers/specs/archive/2026-09-11-g001-group-list-topping-border.md)
- **테두리도 오늘 캔버스에 묶여 OQ-P-336의 사정을 그대로 물려받는다** — 어제까지 토핑이 있던 그룹은 이미지와
  테두리가 함께 비어 템플릿 그래픽으로 그려진다. 템플릿·조회 실패 그래픽에 테두리를 두를지는 여전히 정책이
  비어 있다 → [open-questions](../synthesis/open-questions.md) OQ-P-316 ③ · OQ-P-336
