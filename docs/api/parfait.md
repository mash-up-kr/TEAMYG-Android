---
id: parfait
title: 파르페(캔버스) 조회·배경·회전
server_module: http/parfait
server_commit: d76b27a
verified: 2026-09-10
android_status: partial
related_spec: 2026-08-15-parfait-canvas-topping-member-api-service-layer, 2026-08-16-canvas-detail-background-api-service-layer, c201-canvas-calendar, c201-canvas-calendar-server
related_adr: ADR-0017
tags: [api, parfait, server-contract, canvas]
---

# 파르페(캔버스) 조회·배경·회전 API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

`feat: 오늘의 파르페(캔버스) 조회 API 구현`(PR #92)·`feat: 과거 파르페(캔버스) 목록 조회 API 구현`(PR #94)·
`[Feat/#85] 캔버스 상태 자동 전환 배치(새벽 3시) 구현 (#97)`으로 **1 → 3 엔드포인트 + 테스트 전용 1**이 됐고,
`feat: 이전 파르페(캔버스) 상세 조회 API 구현`(PR #96)·`feat: 파르페(캔버스) 배경 변경 API 구현`(PR #103)이
**3 → 5**로 올렸다. `parfait` 테이블의 `status`·`background_type`·`background_value` 세 컬럼
(`V11__add_status_and_background_to_parfait.sql`)은 이 라운드에 **쓰기 경로를 얻었다** — 배경 필드가
"읽기만 있고 채우는 코드가 없는" 상태가 닫혔다.

⚠️ **2026-08-18 — 엔드포인트는 안 늘고 "하루"의 정의가 바뀌었다**(`[Fix] 그룹/캔버스 API에 Nametag-Chip
노출&배치 작업 시간 수정`). `ParfaitDay`(`core/parfait/domain`)가 생겨 **파르페의 하루가 자정이 아니라
03:00에 넘어간다** — 자정~03시 사이에는 전날이 아직 진행 중이다. 오늘 조회·그룹 생성·회전 가드가 전부
이 기준을 쓴다. **앱은 여전히 자정 기준**이라 이 구간에서 계약과 어긋난다 → 아래
[하루 경계](#하루-경계) · [Android 매핑](#android-매핑).

✅ **2026-08-19 — 서버 안에서 갈려 있던 기준이 합쳐졌다.** 과거 목록의 `to` 기본값만 `LocalDate.now()`
(자정)로 남아 있던 것이 `ParfaitDay.current()`로 바뀌어, **이 도메인의 "오늘"이 한 값이 됐다.** 같은
delta가 `groupMembers`에도 `nameTagChip`을 실어 캔버스 상단 멤버 칩이 계약 안으로 들어왔다.

✅ **2026-08-20 — 마감된 캔버스의 쓰기가 서버에서 막혔다**(`fix: 마감된 파르페에 대한 편집 요청 거부`,
PR #109). 배경 변경과 토핑 네 엔드포인트([parfait-image.md](parfait-image.md))가 대상 파르페의
`status`를 읽고 `ACTIVE`가 아니면 **409 `PARFAIT_ALREADY_CLOSED`**로 거부한다. 엔드포인트·요청·응답
형태는 그대로이고 **실패 경로만 늘었다** — 그전에는 03시 회전 직후 마감된 캔버스에 쓰기가 200으로
성공하고도 뒤이은 `today` 조회가 새 캔버스를 줘서 편집이 사라진 것처럼 보였다. 이로써
`PARFAIT_ALREADY_CLOSED`가 **공개 경로를 처음 얻었다**(아래 [도메인 에러 코드 전수](#도메인-에러-코드-전수)).

✅ **2026-08-26 — 서버가 "이 토핑이 내 것인가"에 답하기 시작했다**(`[Feat/#114] 오늘/상세 캔버스 응답
placedBy에 ownerType(ME/OTHER) 필드 추가`, PR #115). 오늘·상세 두 캔버스 응답의 `images[].placedBy`에
`ownerType`이 붙는다. 서버가 요청자의 계정 id와 배치자의 계정 id를 직접 견주므로 **앱이 두 식별자의
축을 맞출 일이 없어졌다** — 읽기만 하면 된다. 엔드포인트·요청 형태는 그대로이고 **응답 필드 하나가
늘었다** → [Android 매핑](#android-매핑).

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/v1/groups/{groupId}/parfaits/year` | 필요 | path `groupId` Long | `ParfaitYearsResponse` | 결선됨 |
| GET | `/api/v1/groups/{groupId}/parfaits/today` | 필요 | path `groupId` Long | `GetTodayParfaitResponse` | 결선됨[^dayboundary] |
| GET | `/api/v1/groups/{groupId}/parfaits` | 필요 | query `from`·`to`(선택) | `PastParfaitsResponse` | 결선됨 |
| GET | `/api/v1/groups/{groupId}/parfaits/{parfaitId}` | 필요 | path `groupId`·`parfaitId` Long | `GetTodayParfaitResponse`(**재사용**) | 결선됨 |
| PATCH | `/api/v1/groups/{groupId}/parfaits/{parfaitId}/background` | 필요 | `ChangeParfaitBackgroundRequest` | `ChangeParfaitBackgroundResponse` | 결선됨 |
| POST | `/api/v1/test/parfait-canvas/rotate` | **불필요(화이트리스트)** | 없음 | `RotateParfaitCanvasesResponse` | 해당 없음[^test] |

[^test]: **테스트 전용 엔드포인트다.** 서버 컨트롤러(`ParfaitCanvasRotationTestController`)와
`SecurityConfig.WHITELIST_PATHS` 양쪽에 "프로덕션 오픈 전 함께 제거할 것"이라는 TODO가 달려 있다.
앱이 붙일 대상이 아니다 → [미결](#미결).

[^dayboundary]: ✅ **해소됨(2026-08-20, PR #308 develop 머지).** 2026-08-18~19 동안은 서버가 "오늘"을
    `ParfaitDay`(03시 경계)로 세는데 앱 `parfaitToday()`(`domain/model/ParfaitDay.kt`)가 **KST 자정
    경계**여서, 00:00~03:00 KST에 서버가 전날 캔버스를 주면 `GetTodayParfaitUseCase`가 그것을 어긋난
    응답으로 보고 **한 번 더 부른 뒤 같은 값을 그대로 썼다**(부작용 있는 GET이 두 배). 앱이 경계를
    03시로 옮겨 어긋남이 사라졌고, 경계 값은 `DayWindow.DAY_BOUNDARY_HOUR` 하나만 쓴다 —
    **다만 시간대는 공유하지 않는다**(`parfaitToday()`는 고정 KST, `DayWindow.current()`는 기기 시간대).
    → 아래 [하루 경계](#하루-경계) · [conventions.md](conventions.md) "Android 불일치"(이제 0건) ·
    [open-questions](../synthesis/open-questions.md) [2026-08-18].

경로 주의: 그룹을 `groups`로 부르는 유일한 경로다(다른 그룹 API는 `parfait-groups`) —
[conventions.md](conventions.md)의 URL 규약 절 참고.

컨트롤러 주의: 조회 넷은 `ParfaitController` 하나에 모였지만 **배경 변경만 별도 컨트롤러**
(`ChangeParfaitBackgroundController`)다. 같은 `http/parfait` 패키지이고 OpenAPI 태그도 `Parfait`로
같으므로 소비 측에서는 한 도메인으로 본다. 상세 조회의 `@GetMapping("/{parfaitId}")`가 `year`·`today`
같은 고정 세그먼트와 한 컨트롤러 안에서 겹치지만, Spring이 고정 세그먼트를 먼저 매칭하므로
`/parfaits/year`가 `parfaitId = "year"`로 새지 않는다(`year`는 Long 변환도 안 된다).

## 엔드포인트 상세

### GET /api/v1/groups/{groupId}/parfaits/year

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `groupId` | Long | 필수(path) | |

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `years` | List<Int> | 아니오 | 해당 그룹에 파르페(캔버스)가 존재하는 연도 목록 |

  **관측 사실**: 경로 세그먼트는 단수 `year`인데 응답 필드는 복수 `years`(목록)다 — 세그먼트 이름과 응답
  형태가 어긋난다. 서버 코드에 의도를 밝히는 주석·문서는 없다.

  용도 메모: C-201 캘린더가 연도 선택지를 그릴 때 쓸 값이다. `ParfaitService.getYears`는 정렬을 직접
  수행하지 않고 `ParfaitQueryPort.findDistinctYearsByGroupId`가 반환한 순서를 그대로 내려준다 —
  다만 그 구현(`ParfaitRepository.findDistinctYearsByParfaitGroupId`)의 JPQL이 `ORDER BY YEAR(...) ASC`를
  들고 있어 실제로는 **오름차순**이다. 보장 주체가 서비스가 아니라 쿼리라는 뜻이다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 403 | `GROUP_NOT_JOINED` | 참여하지 않은 그룹입니다 |

  **그룹 미참여 시 동작(권한 검사 유무 확인)**: 권한 검사가 **있다**. `ParfaitService.getYears`가 연도
  조회보다 먼저 `ParfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(groupId, memberId)`로 그룹
  멤버십을 확인하고, 없으면 `ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)`를 던진다.
  `GlobalExceptionHandler.handleParfaitGroupException`이 `ParfaitGroupApiErrorCode.from(error)`로
  변환해 403으로 응답한다 — parfait 도메인 전용 enum이 아니라 [parfait-group.md](parfait-group.md)의
  `ParfaitGroupApiErrorCode`를 그대로 재사용한 것(`from(error) = valueOf(error.name)`,
  [conventions.md](conventions.md) 참고). 근거: `ParfaitServiceTest`("그룹에 참여하지 않은 회원은 조회를
  거부한다")가 직접 검증.

  **주의**: `getYears`는 그룹 존재 여부를 별도로 확인하지 않는다. `existsByGroupIdAndMemberId`가 그룹
  존재·멤버십을 한 번에 판정하므로, 존재하지 않는 `groupId`를 넘겨도 `GROUP_NOT_FOUND`가 아니라
  `GROUP_NOT_JOINED`가 나온다 — `GROUP_NOT_FOUND`를 `GROUP_NOT_JOINED`보다 먼저 별도로 던지는
  parfait-group 도메인의 상세·닉네임변경·탈퇴·신고 4개 엔드포인트와 다른 동작이다.
  **세 조회 엔드포인트가 전부 이 방식이다.**

  🔁 **2026-09-10 — 이 검사의 기준이 좁아졌다**(`fix: 그룹 탈퇴 후 재참여가 불가능하던 문제 해결`).
  `existsByGroupIdAndMemberId`의 구현이 `existsByParfaitGroupIdAndMemberId`에서
  `existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull`로 바뀌어, **탈퇴한 회원은 이 검사를 쓰는 다섯 경로**
  (연도 리스트·오늘·과거·상세 조회와 배경 변경)**에서 403 `GROUP_NOT_JOINED`를 받는다.** 직전까지는 탈퇴한
  멤버십 행이 남아 있는 것만으로 `true`가 나와 **나간 그룹의 캔버스를 계속 읽고 배경까지 바꿀 수 있었다.**
  엔드포인트·DTO·에러 코드가 하나도 안 바뀌었는데 **권한 경계가 이동한 delta**다. 그 결과 아래 404
  `GROUP_NOT_FOUND`가 닿는 조건도 "멤버십 행이 남은 상태"에서 **"활동 중인 멤버십이 남은 상태"**로 좁아졌다.
  근거: `ParfaitGroupAdapter`·`ParfaitGroupMemberRepositoryQueryTest`("탈퇴한 멤버는
  `existsByParfaitGroupIdAndMemberIdAndLeftAtIsNull`이 false를 반환한다").

### GET /api/v1/groups/{groupId}/parfaits/today

C-001 캔버스 메인이 그릴 **오늘의 캔버스 전체**를 한 번에 내려준다 — 상태·멤버 목록·배경·배치된 토핑 전량이
한 응답에 들어 있다. 배치 목록을 따로 얻는 API는 여전히 없고, **이 엔드포인트가 그 자리를 대신한다.**

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드**: 경로 변수 `groupId`뿐(쿼리·바디 없음)

  ⚠️ **읽기인데 쓰기가 일어난다.** `GetTodayParfaitService`가 `EnsureActiveCanvasUseCase.ensure(groupId, 오늘)`을
  호출하고, 그 구현(`ParfaitService.ensure`)은 해당 날짜 파르페가 없으면 **`Parfait.createToday`로 새로 만들어
  저장한다**(`@Transactional`, `readOnly`가 아니다). GET 한 번이 행을 만든다는 뜻이다 — 캘린더·연도 목록에도
  그날이 즉시 나타난다 → [미결](#미결).

  ⚠️ **`ensure`는 상태가 아니라 날짜로 찾는다.** `findByGroupIdAndDate`라서, 오늘 날짜 캔버스가 이미
  `CLOSED`·`EMPTY`면 **그것을 그대로 돌려준다**(새로 만들지 않는다). 응답 `status`가 `ACTIVE`가 아닐 수 있고,
  그때는 토핑을 더 올릴 수 없는 캔버스를 "오늘"로 받는다 — **2026-08-20부터는 그 "올릴 수 없다"를 서버가
  실제로 강제한다**(쓰기 다섯 경로가 409 `PARFAIT_ALREADY_CLOSED`). 그전까지는 쓰기가 200으로 성공하고
  다음 `today` 조회에서 편집이 사라진 것처럼 보였다. **2026-08-18에 이 상태로 가는 지름길이 생겼다** —
  테스트 전용 회전이 오늘 캔버스를 강제로 마감하고 내일 날짜 캔버스를 만들므로, 그 뒤 `today`를 부르면
  방금 마감된 캔버스가 온다.

  ⚠️ **"오늘"이 자정이 아니라 03시 기준이다**(2026-08-18) — `ParfaitDay.current()` →
  아래 [하루 경계](#하루-경계).

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaitId` | Long | 아니오 | 토핑 배치([parfait-image.md](parfait-image.md))가 쓰는 키 |
| `groupName` | String | 아니오 | **2026-09-07 신설.** 그 캔버스가 속한 그룹의 이름 — 아래 참고 |
| `date` | LocalDate | 아니오 | 캔버스 날짜 |
| `status` | String(enum) | 아니오 | `ACTIVE` · `CLOSED` · `EMPTY` |
| `lastClosedDate` | LocalDate? | 예 | 그 그룹의 **마지막 `CLOSED` 캔버스 날짜**. 아래 참고 |
| `groupMembers` | List<객체> | 아니오 | 원소: `id`(Long, **groupMemberId**) · `nickname`(String) · `nameTagChip`(String(enum), **2026-08-19 신설**) |
| `background` | 객체? | 예 | `type`(`COLOR`·`IMAGE`) · `value`(String) |
| `images` | List<객체>? | **예** | 배치된 토핑. **0건이면 빈 배열이 아니라 `null`** |

  토핑 원소(`TodayParfaitImageResponse`) 필드: `parfaitImageId` · `imageId` · `imageUrl` ·
  `positionX`/`positionY`(Double) · `positionZ`(Int) · `scale`/`rotation`(Double) ·
  `borderType`(`NONE`·`SOLID`) · `borderColor`(String?) · `borderWidth`(Double?) ·
  `placedBy`(`groupMemberId`·`nickname`·`nameTagChip`·`ownerType`) · `createdAt`(LocalDateTime).

  ✅ **`groupName`이 응답에 들어왔다**(2026-09-07 신설). `GetTodayParfaitResult`·`GetTodayParfaitResponse`
  양쪽에 **비널 문자열**로 붙었고, 두 서비스(`GetTodayParfaitService`·`GetParfaitDetailService`)가
  `ParfaitGroupQueryPort.findById`로 그룹을 다시 조회해 `group.name.value`를 채운다. 커밋 메시지가
  이유를 적는다 — **그룹 id만 쥔 채(예: 푸시 알림) 캔버스로 바로 들어오면 상단에 그릴 그룹명을 얻을
  길이 없었다.** 즉 이 필드는 [notification.md](notification.md)의 딥링크 진입을 받치는 자리다.
  ⚠️ **그 조회가 실패 경로를 하나 더 만들었다** — 아래 에러 코드 표의 404 `GROUP_NOT_FOUND`.
  ✅ **앱이 하루 만에 이 필드를 읽기 시작했다**(2026-09-08, PR #469 develop 머지) —
  `GetTodayParfaitResponse`·`CanvasVO`에 `groupName`이 서고 `VOMapper`의 `toCanvasVO`가
  `GroupName`으로 감싸 나른다. 오늘·상세 두 조회가 같은 매퍼를 타므로 두 경로 모두 값을 얻는다.
  다만 **그룹명의 정본은 그룹 목록 캐시로 두었다** — 아래 [Android 매핑](#android-매핑).
  ⚠️ **404 `GROUP_NOT_FOUND`는 여전히 앱이 구별하지 않는다**(앱 코드에 그 상수가 없다)
  → [open-questions](../synthesis/open-questions.md) OQ-P-383 ④.

  ✅ **`nameTagChip`이 두 목록 모두에 있다**(`placedBy` 2026-08-18 · `groupMembers` 2026-08-19). 값 집합·배정
  규칙은 [parfait-group.md](parfait-group.md) "Nametag-Chip 배정 규칙"이 정본이고, JSON에는 enum 이름
  문자열(`"TYPE6"`)로 실린다. **비널**이며(2026-08-19에 도메인 타입에서 널이 빠졌다) **탈퇴한 멤버가 남긴
  토핑에는 `DEFAULT`가 온다**(`placedBy` 조회에 탈퇴 필터가 없다 — 아래 참고). `groupMembers`는 탈퇴자를
  거르므로 그쪽에는 `DEFAULT`가 오지 않는다. 즉 **캔버스 상단 멤버 칩도 이제 계약으로 색을 정할 수 있다.**
  🔁 **JSON 키가 `nametagChip`에서 `nameTagChip`으로 바뀌었다**(2026-08-19) — 서버 코어 프로퍼티명은
  그대로이고 HTTP 응답 DTO 경계에서만 바뀌었다.
  근거: `ParfaitControllerTest`가 `images[0].placedBy.nameTagChip`(`"TYPE6"`)을 `jsonPath`로 단언한다.

  ✅ **`ownerType`이 소유 판정을 계약 안으로 들여왔다**(2026-08-26 신설). 값은 `ME`·`OTHER` 둘뿐이고
  (`OwnerType`, 서버 `core/parfait/domain`) **비널**이다. `GetTodayParfaitService`·`GetParfaitDetailService`의
  `buildImages`가 **배치자의 계정 id와 요청자의 계정 id를 견주어** 채운다 — 응답에 함께 실리는
  `placedBy.groupMemberId`는 그룹 멤버십 행 id라 **축이 다르므로**, 소비 측이 그 값으로 소유를 판정하면
  안 된다. 서버가 판정을 끝내 주는 덕분에 앱은 자기 식별자를 따로 구해 올 필요가 없다.
  ⚠️ **요청자마다 값이 다르다** — 같은 캔버스라도 보는 사람에 따라 다른 JSON이므로 이 응답을
  사용자 사이에 공유하거나 캐시해 돌려 쓰면 안 된다.
  ⚠️ **탈퇴 필터가 없는 것은 그대로다** — 탈퇴한 멤버가 남긴 토핑에도 `ownerType`이 실리고, 그 값은
  요청자가 그 사람이 아닌 한 `OTHER`다.
  근거: `GetTodayParfaitServiceTest`·`GetParfaitDetailServiceTest`가 각각 본인·타인 두 갈래를 단언하고,
  `ParfaitControllerTest`가 응답 JSON에 이 필드가 실리는 것을 확인한다.

  ⚠️ **`lastClosedDate`는 `EMPTY`를 세지 않는다.** `ParfaitAdapter.findLastClosedDateByGroupId`가
  `status = CLOSED` 행만 최신순으로 하나 집는다. 토핑 0건으로 마감된 날은 `EMPTY`라 여기 잡히지 않으므로,
  이 값은 "마지막으로 마감된 날"이 아니라 **"마지막으로 토핑이 있던 날"**이다.

  ⚠️ **0건 표현이 `null`이다.** `buildImages`가 빈 목록에서 `null`을 반환한다. `background`도 `type`·`value`
  둘 중 하나라도 없으면 통째로 `null`이다. envelope는 `default-property-inclusion: always`라 키 자체는
  실려 오므로([conventions.md](conventions.md)), 소비 측은 **키 존재가 아니라 값이 `null`인지**로 갈라야 한다.

  ✅ **배경을 설정하는 API가 생겼다**(2026-08-16, PR #103) — 아래
  [PATCH .../background](#patch-apiv1groupsgroupidparfaitsparfaitidbackground). 이전 판본이 "쓰기 경로가
  서버 어디에도 없어 항상 `null`"이라고 적던 자리다. **C-301 배경 편집 화면**(develop, PR #231)이 고른
  배경을 버리던 이유 중 서버 절반이 닫혔고, **앱 표면도 다음 날 붙었다**(PR #266) — 남은 것은
  Repository·UseCase·화면이다 → [open-questions](../synthesis/open-questions.md).

  **`groupMembers`는 탈퇴하지 않은 멤버만**이다(`findAllByGroupIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc` —
  참여 순). ⚠️ **그런데 `placedBy` 조회에는 그 필터가 없다**(`findAllByIdIn`). 탈퇴한 멤버가 남긴 토핑은
  그대로 보이고 그 `placedBy.nickname`은 `GroupNickname.unknown()`이 넣은 **`(알수없음)`**이다
  (탈퇴 시 닉네임이 이 값으로 대체된다 — [parfait-group.md](parfait-group.md)·[member.md](member.md)).

  즉 `images[].placedBy.groupMemberId`가 `groupMembers`에 없을 수 있다 → [미결](#미결).

  🔁 **2026-09-10 — 그 탈퇴는 되돌려질 수 있다.** 재참여가 **기존 멤버십 행을 재활성화**하므로
  (`ParfaitGroupMember.rejoin`, [parfait-group.md](parfait-group.md)) 같은 `groupMemberId`가 다시 활동
  중으로 돌아온다. 그러면 **탈퇴 중에 `(알수없음)`·`DEFAULT`로 보였던 그 사람의 과거 토핑이 재참여 후
  새 닉네임·새 칩으로 다시 표시된다**(닉네임·칩은 재참여 때 새로 정해지므로 탈퇴 전 값으로 돌아가는 것도
  아니다). `groupMembers`에서 `groupMemberId`로 찾아 색을 정하는 소비 측 매칭도 함께 되살아난다 —
  **`placedBy`의 표시 이름이 한번 `(알수없음)`이 되면 영구히 굳는다고 전제하면 안 된다.**

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 403 | `GROUP_NOT_JOINED` | 그 그룹의 멤버가 아님(`ParfaitGroupApiErrorCode`) |
| 404 | `GROUP_NOT_FOUND` | **2026-09-07 신설.** 그룹 조회가 널(`ParfaitGroupApiErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  ⚠️ **404 `GROUP_NOT_FOUND`는 순서상 거의 닿지 않는다.** 멤버십 검사(`isMember`)가 그룹 조회보다
  먼저라, 그룹이 사라진 경우는 대개 403 `GROUP_NOT_JOINED`로 먼저 걸린다. **멤버십 행은 남았는데 그룹
  행만 없는 상태에서만** 이 코드가 나온다. 그래도 소비 측은 이 두 조회에서 404가 올 수 있다는 것을
  전제해야 한다 — 직전 판본까지 이 경로의 404는 상세 조회의 `PARFAIT_NOT_FOUND`뿐이었다.

  근거: `ParfaitControllerTest`가 성공·빈 캔버스(`background`·`images` 널)·403 세 케이스를 직접 검증한다.
  **`groupName`은 같은 테스트가 응답 JSON에 실리는 것을 확인하고**, `GetTodayParfaitServiceTest`가
  결과 객체에 담기는 것을 단언한다.

### GET /api/v1/groups/{groupId}/parfaits

과거 캔버스 목록. C-201 캘린더·목록이 쓸 요약 형태다.

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `from` | LocalDate | 선택(query) | 생략 시 `to - 30일` |
| `to` | LocalDate | 선택(query) | 생략 시 **서버 기준 오늘**(2026-08-19부터 `ParfaitDay.current()` — 03시 경계) |

  **기본 범위는 30일이다**(`GetPastParfaitsService.DEFAULT_RANGE_DAYS`). `to`만 주면 그 날부터 30일 전까지,
  둘 다 생략하면 오늘부터 30일 전까지다. 상한이 없어 **범위를 크게 주면 그만큼 다 내려온다** — 페이지네이션이
  없다.

  `from > to`면 400 `INVALID_DATE_RANGE`다. 기본값이 적용된 **뒤에** 비교하므로, `from`만 미래로 주면
  (`to`는 오늘) 이 에러가 난다.

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaits` | List<객체> | 아니오 | 0건이면 **빈 배열**(`today`와 반대다) |

  원소 필드: `parfaitId`(Long) · `date`(LocalDate) · `status`(String(enum), **2026-08-31 신설**) ·
  `thumbnailUrl`(String?) · `imageCount`(Int).

  ✅ **2026-08-31 — 목록 원소가 `status`를 싣는다**(`feat: 캔버스 리스트 조회 응답에 status 추가`).
  값 집합은 `today`·상세와 같은 `ACTIVE`·`CLOSED`·`EMPTY`이고 널이 아니다. 경량 프로젝션
  `ParfaitSummary`가 상태를 안 읽던 것이 원인이었고, `ParfaitSummary` → `PastParfaitResult` →
  `PastParfaitResponse` 세 층에 같은 필드를 관통시켰다. 엔드포인트 수·요청·에러는 그대로다.
  근거: `ParfaitControllerTest`가 `$.data.parfaits[0].status`를 `"CLOSED"`로 단언하고,
  `ParfaitAdapterTest`가 프로젝션이 상태를 채우는 것을 잠근다.

  ⚠️ **`imageCount == 0`과 `status == EMPTY`는 같은 뜻이 아니다.** `EMPTY`는 **토핑 0건으로 마감된
  날**이고, 진행 중인 `ACTIVE` 캔버스도 아직 아무도 안 올렸으면 `imageCount`가 0이다. 두 값을
  한 조건으로 접으면 오늘 캔버스가 마감된 날로 보인다 → [Android 매핑](#android-매핑).

  ⚠️ **`thumbnailUrl`은 항상 `null`이다.** `GetPastParfaitsService`가 `thumbnailUrl = null`을 리터럴로 넣는다 —
  필드만 있고 채우는 코드가 없다. 앱이 목록 썸네일을 그리려면 다른 값을 써야 한다 → [미결](#미결).

  **정렬은 날짜 내림차순**(`findAllByParfaitGroupIdAndParfaitDateBetweenOrderByParfaitDateDesc`).
  **상태로 거르지 않는다** — 오늘의 `ACTIVE` 캔버스도 범위에 들면 그대로 포함된다.
  `imageCount`는 `ParfaitImageRepository.countAllByParfaitIdIn` 집계이고, 배치가 0건인 파르페는 집계 결과에
  키가 없어 `0`으로 채워진다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_DATE_RANGE` | `from`이 `to`보다 늦음(`ParfaitErrorCode`) |
| 400 | `INVALID_REQUEST` | `from`·`to`가 `LocalDate`로 파싱되지 않음(`CommonErrorCode`) |
| 403 | `GROUP_NOT_JOINED` | 그 그룹의 멤버가 아님(`ParfaitGroupApiErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  근거: `ParfaitControllerTest`가 성공·기본값 위임·400·403 네 케이스를 직접 검증한다.

### GET /api/v1/groups/{groupId}/parfaits/{parfaitId}

과거 목록에서 항목을 눌러 들어가는 **캔버스 상세**다. 응답 타입이 `today`와 **같은 클래스**
(`GetTodayParfaitResult`·`GetTodayParfaitResponse`)라 필드 구성이 완전히 동일하다 — 소비 측은 DTO를
하나만 만들면 된다.

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드**: 경로 변수 `groupId`·`parfaitId`(쿼리·바디 없음)
- **응답 필드**: `today`와 동일 → [위 표](#get-apiv1groupsgroupidparfaitstoday) 참고.
  **2026-09-07에 붙은 `groupName`도 같다** — 이 경로도 `ParfaitGroupQueryPort.findById`로 그룹을 다시
  조회해 채우고, 그 조회가 널이면 404 `GROUP_NOT_FOUND`다.
  `lastClosedDate`·`groupMembers`·`background`·`images`의 널 규칙(0건이 빈 배열이 아니라 `null`)도 같다.
  `placedBy.ownerType`(2026-08-26 신설)도 같은 규칙으로 채워진다 — **요청자 기준**이라 과거 캔버스를
  봐도 보는 사람에 따라 값이 갈린다.

  **`today`와 다른 점은 셋이다.**
  ① **부작용이 없다** — `EnsureActiveCanvasUseCase`를 부르지 않고 `@Transactional(readOnly = true)`다.
  없는 날짜를 조회해도 행이 생기지 않는다.
  ② **날짜가 아니라 id로 찾는다**(`ParfaitRepository.findByIdAndParfaitGroupId`). 그룹이 다르면 조회되지
  않으므로 **남의 그룹 캔버스를 id로 훔쳐볼 수 없다**(응답은 404 `PARFAIT_NOT_FOUND`, 403이 아니다).
  ③ **상태로 거르지 않는다** — `ACTIVE`인 오늘 캔버스도 이 경로로 조회된다. "이전 파르페 상세"라는
  이름이지만 계약상 과거 전용이 아니다.

  `lastClosedDate`는 **조회 대상 파르페 기준이 아니라 그룹 기준**이다(`findLastClosedDateByGroupId`) —
  과거 캔버스를 봐도 그 값은 그룹의 최신 마감일이라 **조회 대상 날짜보다 뒤일 수 있다.**

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 404 | `PARFAIT_NOT_FOUND` | 파르페가 없거나 **그 그룹 소속이 아님**(`ParfaitErrorCode`) |
| 404 | `GROUP_NOT_FOUND` | **2026-09-07 신설.** 그룹 조회가 널(`ParfaitGroupApiErrorCode`) |
| 403 | `GROUP_NOT_JOINED` | 그 그룹의 멤버가 아님(`ParfaitGroupApiErrorCode`) |
| 400 | `INVALID_REQUEST` | `parfaitId`가 Long으로 파싱되지 않음(`CommonErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  멤버십 검사가 파르페 조회보다 **먼저**다 — 남의 그룹이면 파르페 존재 여부와 무관하게 403이다.
  **2026-09-07부터 그 사이에 그룹 조회가 하나 낀다**(멤버십 → 그룹 → 파르페). 같은 404라도
  `GROUP_NOT_FOUND`가 `PARFAIT_NOT_FOUND`보다 먼저 나오므로, 소비 측이 404를 `PARFAIT_NOT_FOUND` 하나로
  읽고 분기하면 안 된다.
  근거: `ParfaitControllerTest`가 성공·404·403 세 케이스를 직접 검증한다.

### PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/background

C-301 배경 편집이 고른 값을 **서버에 저장**하는 경로다. 단색(HEX) 또는 업로드 완료된 이미지 둘 중 하나로
바꾼다.

- **인증**: 필요
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드**(`ChangeParfaitBackgroundRequest`)

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `type` | String(enum) | 필수 | `COLOR` · `IMAGE` |
| `value` | String? | `type = COLOR`일 때 필수 | HEX 문자열. `#` + 6자리(대소문자 무관) |
| `imageId` | Long? | `type = IMAGE`일 때 필수 | [image.md](image.md)의 업로드 확인을 마친 이미지 |

  ⚠️ **Bean Validation 애노테이션이 하나도 없다.** 필수 여부는 서비스(`resolveValue`)와 도메인
  (`Parfait.changeBackground`)이 판정하므로 **OpenAPI 스키마 `required`에는 `type`조차 나오지 않는다**
  ([conventions.md](conventions.md) "OpenAPI가 모르는 것"). `type`은 Kotlin 비널이라 누락하면 검증
  에러가 아니라 역직렬화 실패 → 400 `INVALID_REQUEST`다.

  **타입별로 다른 필드가 필수인데 표현 수단이 널 허용 두 개뿐이다.** `type = COLOR` + `imageId`만,
  또는 `type = IMAGE` + `value`만 보내면 400 `INVALID_BACKGROUND`다. 반대로 **둘 다 채워 보내면 오류가
  아니라 `type`에 해당하는 쪽만 쓰이고 나머지는 조용히 버려진다.**

  **HEX 검증은 도메인이 한다** — `Parfait.changeBackground`의 `HEX_COLOR_PATTERN`이 `#` + 6자리만
  통과시킨다. 3자리 축약형·8자리 알파 포함·`#` 없는 형태는 전부 400 `INVALID_BACKGROUND`다.

- **응답 필드**(`ChangeParfaitBackgroundResponse`)

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `background` | 객체 | 아니오 | `type`(`COLOR`·`IMAGE`) · `value`(String) |

  **중첩 `BackgroundResponse`는 조회 응답의 것을 그대로 재사용**한다(`GetTodayParfaitResponse.kt`에
  선언된 같은 클래스). 조회에서는 `background`가 널 허용인데 **여기서는 비널**이다 — 방금 설정한 값을
  돌려주기 때문이다.

  ⚠️ **`type = IMAGE`로 저장되는 `value`는 `imageId`가 아니라 이미지 URL이다**(`ImageMeta.url`). 요청은
  id로 받고 응답·조회는 URL로 내려온다. 앱이 "지금 배경이 어느 이미지인지"를 id로 되짚을 방법이
  계약에 없다 → [미결](#미결).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_BACKGROUND` | 타입에 필요한 값 누락, 또는 HEX 형식 위반(`ParfaitErrorCode`) |
| 404 | `PARFAIT_NOT_FOUND` | 파르페가 없거나 그 그룹 소속이 아님(`ParfaitErrorCode`) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(`ParfaitErrorCode`, **2026-08-20 신설 경로**) |
| 404 | `IMAGE_NOT_FOUND` | `imageId`에 해당하는 이미지 메타 없음(`ImageErrorCode`) |
| 409 | `BACKGROUND_IMAGE_NOT_CONFIRMED` | 이미지가 `COMPLETED`가 아님(`ParfaitErrorCode`) |
| 403 | `GROUP_NOT_JOINED` | 그 그룹의 멤버가 아님(`ParfaitGroupApiErrorCode`) |
| 400 | `INVALID_REQUEST` | 바디 역직렬화 실패(`type` 누락·모르는 enum 값 등, `CommonErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  **한 엔드포인트가 세 enum의 코드를 섞어 낸다**(`ParfaitErrorCode`·`ImageErrorCode`·
  `ParfaitGroupApiErrorCode`). 이미지 미확인은 `ImageErrorCode`가 아니라 **parfait 쪽 enum**
  (`BACKGROUND_IMAGE_NOT_CONFIRMED`)이라는 점이 특히 갈린다 — 소비 측은 도메인별로 분기하면 놓친다.
  근거: `ChangeParfaitBackgroundControllerTest`가 성공 2·400·404 2·409·403 일곱 케이스를 직접 검증한다.
  **409 `PARFAIT_ALREADY_CLOSED`만 컨트롤러가 아니라 서비스 테스트가 잠근다**
  (`ChangeParfaitBackgroundServiceTest`의 "이미 마감된 파르페면 PARFAIT_ALREADY_CLOSED를 던진다").

  **검사 순서**: 그룹 멤버십 → 파르페 존재 → **파르페 상태** → 배경 값 해석(`resolveValue`).
  앞이 걸리면 뒤는 실행되지 않는다.

  ✅ **마감된 캔버스의 배경은 이제 바꿀 수 없다**(2026-08-20). `ChangeParfaitBackgroundService`가 파르페를
  찾은 **직후** `status != ACTIVE`를 검사해 409로 끊는다 — 값 해석보다 앞이라 `CLOSED` 캔버스에 잘못된
  HEX를 함께 보내도 `INVALID_BACKGROUND`가 아니라 `PARFAIT_ALREADY_CLOSED`가 온다. 토핑 네 엔드포인트도
  같은 라운드에서 같은 가드를 얻었다([parfait-image.md](parfait-image.md)) — 직전 판본이 "마감 후 편집을
  막는 서버 가드는 어디에도 없다"고 적던 자리다.

  ⚠️ **다만 권한이 상태보다 앞이다.** 멤버십 검사가 첫 줄이라 **마감된 캔버스라도 그 그룹의 멤버가
  아니면 409가 아니라 403 `GROUP_NOT_JOINED`**가 온다. 토핑 세 편집 경로가 소유권 뒤에 마감을 검사하는
  것과 같은 모양이고([parfait-image.md](parfait-image.md)), 소비 측이 마감을 유일한 실패로 두고 분기하면
  이 경우를 놓친다.

  ⚠️ **배경 이미지는 `image_meta.reference_count`를 올리지 않는다.** 증감 경로는 토핑 배치(+1)·토핑
  삭제(−1)뿐이고([parfait-image.md](parfait-image.md)) 이 API는 `ImageMetaQueryPort`로 **읽기만** 한다.
  같은 이미지를 토핑으로도 올렸다가 그 토핑을 지우면 카운트가 0이 되어 **S3 객체가 삭제되고 배경이
  깨진다** → [미결](#미결).

### POST /api/v1/test/parfait-canvas/rotate (테스트 전용)

⚠️ **인증 없이 전체 그룹의 캔버스를 즉시 마감·재생성한다.** 화이트리스트에 올라 있어 토큰 없이 호출되고,
대상이 특정 그룹이 아니라 **모든 그룹**이다. 서버 코드의 TODO가 프로덕션 오픈 전 제거를 예고한다
→ [미결](#미결).

- **인증**: 불필요(`SecurityConfig.WHITELIST_PATHS`에 `/api/v1/test/parfait-canvas/rotate` 등재)
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**: 없음
- **응답 필드**: `closedCount` · `emptyCount` · `createdCount` · `failedCount`(전부 Int)

  `ParfaitService.forceRotateAll`이 `ParfaitGroupQueryPort.findAllIds()`로 전 그룹을 돌며 그룹당
  `ParfaitCanvasRotator.forceRotateOne`을 재시도 정책(`canvasRotationRetryTemplate` — 3회·고정 백오프)으로
  실행한다. 그룹 하나가 3회 모두 실패하면 `failedCount`만 올리고 **다음 그룹으로 넘어간다**(전체가 멈추지 않음).

  ⚠️ **2026-08-18 — 정식 배치와 다른 경로를 탄다.** 그전에는 배치와 같은 `rotateAll`을 불렀는데, 같은
  라운드가 `rotateOne`에 "오늘 또는 미래 날짜는 마감하지 않는다" 가드를 넣으면서 이 엔드포인트의
  원래 목적(지금 당장 강제 마감)이 막혔다. 그래서 **가드를 건너뛰는 `ForceRotateParfaitCanvasesUseCase`가
  따로 생겼고 컨트롤러가 그것만 주입받는다.** 결과적으로 이 엔드포인트는 **오늘 캔버스를 마감하고
  내일 날짜 캔버스를 만든다** — 그 뒤 `today` 조회는 방금 마감된 오늘 캔버스를 돌려준다(내일 캔버스는
  날짜로 찾지 않으므로 잡히지 않는다). 정식 배치는 `rotateOne` 가드가 그 미래 캔버스를 다시 건드리지
  않으므로 **한 번의 강제 회전이 영구 드리프트가 되지는 않는다**(서버 주석이 이 근거를 적는다).

## 하루 경계

2026-08-18 delta가 도입한 `ParfaitDay`(`core/parfait/domain`)가 **"오늘"의 단일 정의**다.

- **경계는 03:00**(`ROLLOVER_TIME`)이고 회전 배치의 실행 시각과 같은 값이다. `now`가 03시 전이면
  `current()`는 **전날 날짜**를 돌려준다. 위키 [[캔버스-마감-스케줄]]의 "매일 03시 마감"을 조회 쪽에도
  적용한 것이다.
- **타임존은 서버 로컬 시간**이다 — `LocalDateTime.now()`를 그대로 쓰고 `ZoneId`를 명시하지 않는다.
  서버·DB가 `Asia/Seoul`로 맞춰져 있어([parfait-group.md](parfait-group.md) 타임존 절) 실질은 KST다.
- **쓰는 곳은 넷**이다 — 오늘 조회(`GetTodayParfaitService`), **그룹 생성 시 최초 캔버스**
  (`ParfaitGroupService.create`의 `ensure`), 회전 가드(`ParfaitCanvasRotator.rotateOne`),
  **과거 목록의 `to` 기본값**(`GetPastParfaitsService`, 2026-08-19에 합류).
  그전까지 앞의 둘은 `LocalDate.now()`(자정 기준)를 썼고, 그래서 **자정~03시에 앱을 켜거나 그룹을
  만들면 아직 안 끝난 전날 대신 당일 날짜 캔버스가 생기고 뒤이은 배치가 그것을 또 하루 밀었다.**
- **계약에 드러나는 결과**: 00:00~03:00 사이 `today` 응답의 `date`는 **캘린더상 어제**다. `status`는
  그 시각에도 `ACTIVE`이므로 **그 캔버스에 계속 토핑을 올리는 것이 정상 동작**이다.
- ✅ **2026-08-19 — 서버 안의 두 기준이 하나가 됐다.** 직전 판본이 "과거 목록만 자정 기준"이라고 적던
  자리다(`fix: 이전 파르페 목록 조회가 자정이 아닌 새벽 3시 기준으로 오늘을 판단하도록 통일`). 이제
  00:00~03:00에도 목록의 기본 상한과 활성 캔버스 날짜가 같다. **남은 어긋남은 앱 쪽 하나**다.

## 캔버스 회전(마감·재생성) 규칙

앱이 부르는 API는 아니지만 **`today` 응답의 `status`·`lastClosedDate`가 이 규칙의 산물**이라 계약의 일부다.

- **시각**: 매일 **03:00 Asia/Seoul**(`ParfaitCanvasRotationScheduler`의 `@Scheduled(cron)`).
  Spring Batch job(`ParfaitCanvasRotationJobConfig`·`ParfaitCanvasRotationTasklet`)으로 돈다
  (`V12__add_spring_batch_schema.sql`가 배치 메타 테이블을 깐다). 위키 [[캔버스-마감-스케줄]]의 03시와 같다.
- **그룹당 1회전**: `findActiveByGroupId`로 `ACTIVE` 캔버스를 찾고, 없으면 아무것도 하지 않는다.
- **마감 분기**: 토핑이 하나라도 있으면 `close()` → `CLOSED`, 0건이면 `markEmpty()` → `EMPTY`.
  **`EMPTY`도 마감 상태다** — "비어 있음"이 아니라 "빈 채로 끝남"이다.
- **다음 캔버스**: 마감한 날 **+1일** 날짜로 새 `ACTIVE`를 만든다. 그 날짜 캔버스가 이미 있으면 생략한다.
- **불변식**: "마감 후 생성" 순서가 **그룹당 `ACTIVE` 최대 1개**를 지키는 유일한 근거다(DB 제약이 없다).
  `findActiveByGroupId`가 이 전제에 기대고 있고 서버 코드 주석이 순서 변경을 금지한다.
- **가드**(2026-08-18 강화): **오늘 또는 미래 날짜 캔버스는 마감하지 않는다** — 여기서 "오늘"은
  [`ParfaitDay`](#하루-경계) 기준이다. 이전 판본은 `isAfter(LocalDate.now())`라 **오늘 날짜는
  걸러내지 못했고**, 자정~03시 사이 `ensure`가 만든 오늘 캔버스를 03시 배치가 마감해 다음 날짜 캔버스를
  또 만들었다. 지금은 `!isBefore(ParfaitDay.current(now))`다.
- **가드를 건너뛰는 경로가 하나 있다** — 테스트 전용 회전이 쓰는 `forceRotateOne`이다(위
  [테스트 전용 절](#post-apiv1testparfait-canvasrotate-테스트-전용)). 정식 배치는 쓰지 않는다.

## 도메인 에러 코드 전수

`ParfaitErrorCode`(`core/parfait/exception`) 5종 전부. 2026-08-16 delta가 **2 → 5**로 늘렸고,
2026-08-20 delta는 종수를 늘리지 않고 **귀속만 바꿨다**.

| HTTP | code | message | 귀속 |
|---|---|---|---|
| 400 | `INVALID_DATE_RANGE` | 조회 시작일이 종료일보다 늦을 수 없습니다 | 과거 목록 조회 |
| 404 | `PARFAIT_NOT_FOUND` | 존재하지 않는 파르페입니다 | 상세 조회 · 배경 변경 |
| 409 | `PARFAIT_ALREADY_CLOSED` | 이미 마감된 파르페입니다 | 배경 변경 · **토핑 네 엔드포인트**([parfait-image.md](parfait-image.md)) — 아래 |
| 400 | `INVALID_BACKGROUND` | 배경 정보가 올바르지 않습니다 | 배경 변경 |
| 409 | `BACKGROUND_IMAGE_NOT_CONFIRMED` | 업로드가 확인되지 않은 이미지입니다 | 배경 변경 |

  🔁 **`PARFAIT_ALREADY_CLOSED`가 공개 경로를 얻었다**(2026-08-20). 직전 판본까지 이 코드를 던지는 곳은
  `Parfait.close`·`markEmpty`뿐이었고 그 둘을 부르는 회전 로직은 `ACTIVE`만 골라 오므로 **앱이 받을 경로가
  없었다**(경합 시에도 응답이 아니라 `failedCount`로 집계됐다). 지금은 **쓰기 다섯 경로가 직접 던진다** —
  배경 변경과 토핑 배치·수정·테두리·삭제다. 그 결과 **다른 도메인이 이 enum의 값을 내는 자리가 생겼다**:
  토핑 네 엔드포인트는 자기 enum(`ParfaitImageErrorCode`)이 아니라 이 코드로 마감을 알린다.

  ⚠️ **다섯 경로 전부 권한 검사가 마감 검사보다 앞이다.** 마감된 캔버스라도 남의 토핑이거나 그 그룹의
  멤버가 아니면 **409가 아니라 403**이 먼저 온다(수정·테두리·삭제는 `PARFAIT_IMAGE_NOT_OWNED`,
  배치·배경 변경은 `GROUP_NOT_JOINED`). 소비 측이 "마감된 캔버스면 409"로 읽고 분기하면 그 경우가 빠진다.

이 도메인은 자기 enum 밖의 코드도 던진다 — `ParfaitGroupApiErrorCode.GROUP_NOT_JOINED`(403)와
**`GROUP_NOT_FOUND`(404, 2026-09-07 신설 — 오늘·상세 두 조회)**, 그리고 배경 변경의
`ImageErrorCode.IMAGE_NOT_FOUND`(404). 소비 측은 이 도메인 enum만 보고 분기하면 안 된다.

## Android 매핑

**앱 표면이 다섯 전부를 덮는다**(2026-08-16, PR #266 develop 머지). 서버 delta가 벌린 상세 조회·배경
변경 둘이 **같은 날 표면을 얻어** 공백이 하루 만에 닫혔다 — 이 도메인은 테스트 전용 회전을 뺀 전량에
Service·DataSource 함수가 있다.

✅ **소비처가 생겼다**(2026-08-17, PR #268). `ParfaitRepository`/`ParfaitRepositoryImpl`(위임 +
`mapErrorToAppError`, `RepositoryModule` `@Binds` 1줄) → UseCase 둘 → **C-001 캔버스 메인**까지 이어졌다.
**다섯 중 셋만 열었다** — 오늘 조회·과거 목록·상세다. 연도 조회와 배경 변경은 소비자가 생길 때
인터페이스에 올린다(쓰지 않는 갈래를 미리 열면 어떤 실패를 어떻게 다룰지 정하지 않은 채 계약이 굳는다).
`android_status`는 **`partial` 그대로**다.

✅ **같은 날 넷이 됐다**(2026-08-17, PR #279). C-201 캘린더가 mock을 버리면서 **연도 조회**가 그
방침대로 소비자와 함께 올라왔다(`ParfaitRepository#getYears`). 남은 하나는 **배경 변경**이고,
`android_status`는 여전히 `partial`이다 — 표면·계약이 아니라 **소비처가 다섯 중 넷**이라는 뜻이다.

✅ **2026-08-20(PR #308·#310 develop 머지) — 소비처 수는 그대로 넷이고 `android_status`도 `partial`이다.**
이 라운드가 바꾼 것은 갈래 수가 아니라 **오늘 조회 응답을 얼마나 읽는가**다(멤버 칩 결선 + 하루 경계
정정). 배경 변경은 여전히 소비처 0건이라 Repository 인터페이스에 없다.

✅ **`android_status`가 `done`이 됐다**(2026-08-22, PR #329 develop 머지). 마지막 하나였던 **배경 변경**이
C-301 확인 버튼과 함께 올라와 다섯 갈래 전부(테스트 전용 회전 제외)가 화면까지 이어졌다. 경로는
`ParfaitRepository#changeCanvasBackground` → `ChangeCanvasBackgroundUseCase` → `CanvasBGEditViewModel`이다.
이 도메인은 **표면이 먼저 들어오고 소비처가 엿새에 걸쳐 붙은** 형태로 닫혔다(#266 표면 → #268·#279 조회
넷 → #329 쓰기 하나).

**반환값을 버리지 않기로 한 것이 이 결선의 핵심 결정이다.** 이미지 배경은 **쓸 때 `imageId`·읽을 때
URL**이라 앱이 방금 저장한 배경의 주소를 아는 길이 이 응답뿐이다. 앱이 모르는 `type`이 오면 조회와
같은 규칙으로 `null`이고, 그 값의 뜻은 "미설정"이 아니라 **"저장은 됐는데 그릴 수 없다"**인데
(OQ-P-193) **화면은 이것을 실패로 다루지 않는다** — 저장이 끝났으므로 막을 이유가 없고 고른 값으로
그린다. 다만 지금 이 응답은 **실제로 쓰이지 않는다**: 확인 이펙트를 받은 Route가 값을 버리고 돌아간
캔버스 메인이 다시 조회한다. 즉 널 폴백은 **아직 아무 화면 결과도 바꾸지 않는 방어**다.

⚠️ **409 `PARFAIT_ALREADY_CLOSED`를 이 경로가 구별하지 않는다.** 마감된 캔버스에 배경을 보내면 서버가
거절하는데(위 [도메인 에러 코드 전수](#도메인-에러-코드-전수)) 화면 사유 enum은 `NETWORK`·
`UNSUPPORTED_IMAGE`·`UNKNOWN` 셋뿐이라 **"잠시 후 다시 시도해 주세요"로 접힌다**. 같은 상수를 C-106
배치는 되감기 판정에 쓰므로 **한 코드에 두 처분이 생겼다**
→ [open-questions](../synthesis/open-questions.md) OQ-P-261.

⚠️ **배경 이미지가 참조 카운트를 올리지 않는 문제에 첫 실사례가 생겼다** — 이 경로가 업로드하는
이미지는 `ImageType.BACKGROUND`이고, 서버는 배경 설정 시 `reference_count`를 올리지 않는다(OQ-P-190).
지금까지는 소비처가 0이라 드러나지 않던 상태였다.

✅ **거짓이 된 주석 일곱 곳이 같은 날 정리됐다**(2026-08-20, PR #318 develop 머지). "서버가 캔버스
상태를 보지 않아 마감된 캔버스도 편집된다 · 막는 것은 화면 책임"이라는 서술이
`ParfaitService`·`ParfaitRemoteDataSource`(둘)·`ParfaitRepository`·`CanvasStatus`·`CanvasMainViewModel`·
`CanvasMainScreen` 일곱 자리에 흩어져 있었고, 전부 **409를 사실로 적는 문장으로 바뀌었다**.
지우지 않고 고친 것은 [parfait/CLAUDE.md](../code-conventions.md) "기준 2와 3이 겹칠 때는 남긴다"를 따른
것이다 — `CanvasStatus`·`CanvasMainScreen`처럼 오해를 미리 막는 성격이 섞인 자리이고, 새 문장은
단정 대신 근거 문서(`api/parfait.md`)를 가리킨다. **화면 방어는 그대로 남는다** — 지난 캔버스의
편집 진입을 치우는 것은 실패를 보여 주기 전에 길을 없애는 일이라 서버 가드와 목적이 다르다.
같은 PR이 **`ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED`를 신설했다** — 신설 당시는 소비처가
아직 0건인데 상수를 먼저 둔 예외였고, 그 근거(처분이 이미 정해졌다)를 상수 KDoc이 함께 적었다.
✅ **그 예외 사유는 2026-08-22 develop 머지(PR #334)로 소멸했다** — `feature/groups/canvas/impl/util/ToppingPlaceFailure.kt`의 `isPermanentPlaceFailure()`가
이 상수를 배치(POST) 실패의 되감기 판정에 실제로 쓴다. 나머지 넷(위치·테두리 수정·삭제·배경 변경)의
경로는 여전히 소비처 0건이라, 이 상수는 배치 경로 하나로만 소비된다. 상수 KDoc의 결정·함정 서술은
남지만 더는 예외가 아니라 보통의 소비되는 상수다.
⚠️ 그 KDoc이 경고하는 함정이 하나 있다: 다섯 경로 전부 **권한 검사가 마감 검사보다 앞이라**
마감된 캔버스라도 남의 토핑·비멤버면 409가 아니라 403이 먼저 온다.
이 부류(다른 컴포넌트의 현재 상태를 단정한 주석)를 문서 감사 말고 잡을 수단이 없다는 것은 그대로다
→ [open-questions](../synthesis/open-questions.md) [2026-08-20] OQ-P-244 ③.

**계약의 두 성질이 소비 방식을 갈랐다.**
① `today`는 **부작용이 있다**(행 생성). 그럼에도 쓰는 이유는 토핑을 얹으려면 `parfaitId`가 있어야 하고,
**부작용 없는 두 경로는 없는 날을 만들어 주지 않기** 때문이다. 호출 시점은 처음엔 진입 1회였고
(`launch(key)` 가드), **#297부터는 화면이 앞에 설 때마다**다 — 다른 멤버가 올린 토핑으로도 캔버스가
바뀌기 때문이고, 재진입 호출은 첫 진입에서 이미 만들어진 행을 받을 뿐이라 캔버스가 늘지는 않는다.
가드는 동시 중복 호출만 막는 역할로 남았고, 지난 날을 보는 중이면 부르지 않는다.
② 달력에서 날짜를 고를 때는 반대로 **부작용 없는 경로**를 탄다 — 훑는 것만으로 빈 캔버스가 쌓이면
안 된다. "같은 캔버스를 두 경로로 얻는데 한쪽만 부작용이 있다"는 [미결](#미결)이 **앱에서 용도 분리로
굳었다.** 처음에는 하루 범위 목록→상세 2단(`GetCanvasByDateUseCase`)이었고, **#279부터는 달력이 그 해
목록을 이미 캐시로 들고 있어 거기서 `parfaitId`를 꺼내 상세만 부른다**(`GetParfaitDetailUseCase`).
날짜로 캔버스를 찾는 엔드포인트가 없다는 계약 사실은 그대로이고, 그 조회를 화면 캐시가 대신한다.
같은 이유로 **오늘로 되돌아갈 때도 조회를 안 한다** — 받아 둔 오늘 캔버스를 상태에서 갈아 끼운다
(다시 부르면 부작용 있는 `today`가 돈다).

⚠️ **계약이 말하지 않는 것을 앱이 정했다.** 배치 값의 단위·기준이 계약에 없어 렌더가 규칙을 만들었다 —
`positionX`·`positionY`는 **Canvas-Area 대비 0~1 정규화 중심점**, `scale` 1.0은 **긴 변이 그 너비의 40%**
(위키 C-106 초기 크기), `borderWidth` 1.0은 **화면 기준 1dp**(토핑 배율과 무관), `borderColor`는
`#RRGGBB` 6자리(알파 8자리도 읽는다). 서버가 같은 값을 다른 뜻으로 쓰거나 iOS가 달리 해석해도
**계약으로는 드러나지 않는다** → [open-questions](../synthesis/open-questions.md).

⚠️ **`today` 응답의 `date`를 앱이 검증한다.** 자정을 걸친 요청이 어제 캔버스를 받을 수 있어, 오늘을
**응답 뒤에** 읽어 비교하고 어긋나면 딱 한 번 다시 부른다. 그 "오늘"은 기기 시간대가 아니라
**KST**(`PARFAIT_TIME_ZONE`)다 — 캔버스 행이 KST 날짜를 키로 저장되기 때문이고, 기기 시간대로 세면
해외 기기에서 재시도가 하루 한 번이 아니라 **로드마다** 돈다.

✅ **그 어긋남은 닫혔다(2026-08-20, PR #308 develop 머지).** 2026-08-18~19 동안은 앱 `parfaitToday()`가
**KST 자정** 경계, 서버가 **03시** 경계여서([하루 경계](#하루-경계)) 00:00~03:00 KST에 서버가 전날
캔버스를 주는 정상 응답을 앱이 "자정을 걸친 요청"으로 보고 **매번 한 번 더 불렀다** — 부작용 있는
GET이 두 배로 돌고, 표시도 어긋났다(`CanvasMainUiState.today`는 캘린더 오늘 D인데 그 아래 캔버스는
D−1). **정책상 옳은 쪽이 서버였으므로 앱을 옮겼다**(위키 [[캔버스-마감-스케줄]]의 03시). 고친 것은
`parfaitToday()` 한 함수이고 **재시도 조건·달력 활성 조건·`syncToday()`가 그 값을 읽으므로 저절로
따라왔다** → [conventions.md](conventions.md) "Android 불일치"(이제 0건) ·
[open-questions](../synthesis/open-questions.md) [2026-08-18].

✅ **칩 필드 하나는 결선됐고 하나는 여전히 아무도 안 읽는다**(2026-08-20, PR #308·#310 develop 머지).
캔버스 상단 멤버 칩은 `groupMembers[].nameTagChip`을 `CanvasMemberVO.nametagChip`으로 올려 그린다 —
7종 팔레트 인덱스 순환(`NAMETAG_CHIP_PALETTE`)이 사라졌고, 같은 사람이 S-101과 C-001에서 같은 색이다
(서버가 같은 행에서 두 값을 준다). ⚠️ **토핑 작성자 칩(`placedBy.nameTagChip`)은 DTO까지만이다** —
`PlacedByResponse`가 필드를 두지만 `ToppingPlacerVO`에는 안 올렸다. 읽는 화면이 0건인 상태로 도메인
모양을 굳히지 않으려는 판단이고, **C-202 Spotlight는 이 필드가 아니라 `groupMembers` 조인을 쓰므로**
그 화면이 붙어도 여기가 자동으로 필요해지지는 않는다.
📌 **그 Spotlight가 실제로 붙었고 예측대로 조인을 썼다**(2026-08-20, PR #298 develop 머지) — 토스트의
닉네임 색을 `groupMembers`에서 같은 `groupMemberId`로 찾아 정하고, 못 찾으면 `Default`다. **탈퇴
멤버에서도 서버가 `placedBy.nameTagChip = DEFAULT`를 주므로 두 경로의 결과가 우연히 같다** — 그래서
이 필드를 안 읽는 것이 지금은 증상을 만들지 않는다. 서버가 두 목록의 배정 규칙을 갈라 놓는 순간
조용히 틀린 색이 된다 → [open-questions](../synthesis/open-questions.md) OQ-P-251.
닉네임 쪽은 반대로 **서버 문자열이 그대로 화면 문장이 된다** — 탈퇴 멤버 토핑은
`(알수없음)님이 …에 쌓았어요`로 뜬다. 응답 필드를 안 읽는 것뿐이라 `⚠️불일치`는
아니다(앱 JSON은 `ignoreUnknownKeys = true`) → [open-questions](../synthesis/open-questions.md) [2026-08-18].

✅ **본인 판정의 재료가 서버에서 왔다**(2026-08-26 서버 delta). 그전까지 앱에는 "내 토핑"을 가려낼
근거가 없었다 — `CanvasMainViewModel#handleOnClickTopping`의 `CanvasToppingVO.isMine()`이 **상수
`false`**라 C-202의 본인 갈래가 통째로 비어 있었고(OQ-P-250), C-301 편집 탭은 계정 id와 그룹 멤버십
행 id를 견주는 **축이 다른 비교**로 게이트를 열고 닫았다. `placedBy.ownerType`이 그 둘을 한꺼번에
없앤다 — 판정을 서버가 끝내 주므로 앱이 자기 식별자를 구해 올 경로 자체가 필요 없다.
✅ **앱이 같은 날 그 값을 읽기 시작했다**(2026-08-26, PR #376 develop 머지). `PlacedByResponse`에
`ownerType: String?`이 붙고 `VOMapper`가 `ownerType == "ME"`를 **`CanvasToppingVO.isMine`**(비널
`Boolean`)으로 접는다. 즉 **문자열은 `:data` 경계를 못 넘는다** — 도메인이 받는 것은 판정 결과
하나이고, `OTHER`·모르는 값·필드 부재가 **한 갈래로 합쳐진다**(아래 미지 값 폴백).
소비 자리가 둘 다 갈렸다 — `CanvasMainViewModel`의 상수 `false` 확장 함수가 사라지고
`topping.isMine`을 그대로 보며, `CanvasBGEditViewModel`은 `GetMyAccountFlowUseCase` 의존과
진입 시 `first()` 대기를 **통째로 버렸다**(축이 다른 비교가 그 자리에 있었다).
✅ **본인 토핑 탭이 목적지를 얻었다**(2026-08-27, PR #400 develop 머지). `handleOnClickMyTopping`이
`NavKeyCanvasBGEdit`에 **탭한 토핑 id**를 실어 보내고, 편집 화면이 토핑 탭을 편 채 그 토핑을 선택한
상태로 열린다. ⚠️ **새 C-305 화면이 생긴 것이 아니다** — 정책이 말하는 편집 진입을 **기존 C-301
편집 화면의 토핑 탭**이 받는다. 그래서 갈 곳이 있는 것은 **오늘 캔버스뿐이고**, 지난 캔버스를 보는
중이면 `isViewingToday` 가드에 걸려 탭이 여전히 아무 일도 안 한다
→ [open-questions](../synthesis/open-questions.md) OQ-P-250.

✅ **키 어긋남도 develop에서 닫혔다** — 2026-08-19 서버 delta가 응답 키를 `nameTagChip` 계열로 바꾼 뒤
그 필드를 옛 키로 읽던 브랜치가 잠시 있었으나(기본값이 있어 예외 없이 **조용히 `null`**이 되는 부류),
PR #310이 세 DTO의 키를 맞추고 반납 값 이름도 `RELEASED` → `DEFAULT`로 따라간 상태로 머지됐다.
**재발 방지 수단은 여전히 없다** — 이 부류를 잡은 것은 두 번 다 계약 문서 감사였고, 앱 테스트는 자기
DTO를 자기가 만들어 넣어 `@SerialName` 문자열을 검증하지 않는다
→ [server-delta 스펙](../superpowers/specs/archive/2026-08-19-server-delta-nametag-chip-keys.md) ·
[open-questions](../synthesis/open-questions.md) [2026-08-19].

⚠️ **과거 목록은 이제 연 단위로 부른다**(2026-08-17, PR #279) — 1월 1일 ~ 12월 31일을 한 번에 받아
화면이 연도별로 캐시한다. 근거는 계약이다 — **페이지네이션도 범위 상한도 없어**(→ [미결](#미결))
최대 366건이 한 응답으로 오고, 달력은 월을 오갈 때마다 다시 부를 이유가 없다.
**정렬은 앱이 한다** — 계약이 순서를 약속하지 않아 UseCase가 날짜 내림차순으로 세운다.
직전 구현(범위를 하루로 좁혀 부르고도 응답 날짜를 다시 보던 것 — 경계 처리가 서버 몫이라 하루가 더
딸려 오면 옆날을 고른 날로 착각한다)은 그 UseCase와 함께 사라졌다.

⚠️ **배경 변경은 이 도메인 첫 쓰기 경로**이고, 그래서 **첫 요청 DTO**(`ChangeParfaitBackgroundRequest`)와
**쓰기 전용 도메인 모델**(`CanvasBackgroundEdit`)이 함께 들어왔다. 읽기 모델 `CanvasBackground`를
재사용하지 않은 이유는 계약이 비대칭이기 때문이다 — 이미지 배경은 **쓸 때 `imageId`, 읽을 때 URL**이라
읽은 값을 그대로 되돌려 보낼 수 없다. 동시에 이 sealed가 **조건부 필수를 컴파일 시점으로 끌어올린다**
(평면 DTO를 도메인에 노출하면 잘못된 조합을 400 `INVALID_BACKGROUND`로만 알게 된다).

⚠️ **표면을 건너뛴 소비자가 생겼다**(2026-08-16, PR #259) — C-201 캘린더의
`GetParfaitHistoriesUseCase`·`GetParfaitYearsUseCase`가 KDoc으로 `GET .../parfaits?from=&to=`와
`GET .../parfaits/year`를 각각 가리키면서 **`ParfaitRemoteDataSource`를 호출하지 않고 UseCase 본문에서
mock을 만든다**. 즉 이 도메인은 "표면은 있는데 소비처가 없다"가 아니라 **소비처가 표면을 우회한
상태**다. 계약 대조 관점에서 두 가지가 미검증으로 남는다 — 응답 매핑(`ParfaitHistory`가 서버 응답의
어느 필드에 대응하는지 코드에 없다)과 `groupId` 전달(화면 `NavKeyCanvasMain`가 `data object`라
그룹 식별자를 들고 있지 않아 UseCase 인자에서 아예 뺐다) → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md) ·
[open-questions](../synthesis/open-questions.md).
📌 **막고 있던 이유는 사라졌는데 상태는 그대로다**(2026-08-17, PR #268) — Repository가 생겼고
`NavKeyCanvasMain`가 `groupId`를 들고 다니지만 **두 UseCase는 여전히 mock**이다. 이제 같은
ViewModel 안에서 **캔버스 조회는 계약을 타고 달력 조회는 안 탄다.**
✅ **해소(2026-08-17, PR #279)** — 두 UseCase가 `ParfaitRepository`를 주입받아 **표면 우회가 사라졌다.**
미검증으로 남았던 둘도 닫혔다: 응답 매핑은 `ParfaitHistory`를 **삭제**하고 계약 VO `PastCanvasVO`를
그대로 쓰는 것으로(그래서 "달력이 점을 찍는 기준"이 응답 필드 `imageCount` → VO `toppingCount`가
됐다), `groupId`는 NavKey 인자를 타고 두 UseCase 시그니처에 들어왔다
→ [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md).

| 엔드포인트 | Service 함수 | DataSource 함수 |
|---|---|---|
| GET `/api/v1/groups/{groupId}/parfaits/year` | `ParfaitService#getGroupsByGroupIdParfaitsYear` | `ParfaitRemoteDataSource#getYears` |
| GET `.../parfaits/today` | `ParfaitService#getGroupsByGroupIdParfaitsToday` | `ParfaitRemoteDataSource#getTodayCanvas(groupId)` |
| GET `.../parfaits` | `ParfaitService#getGroupsByGroupIdParfaits` | `ParfaitRemoteDataSource#getPastCanvases(groupId, from, to)` |
| GET `.../parfaits/{parfaitId}` | `ParfaitService#getGroupsByGroupIdParfaitsByParfaitId` | `ParfaitRemoteDataSource#getCanvasDetail(groupId, parfaitId)` |
| PATCH `.../parfaits/{parfaitId}/background` | `ParfaitService#patchGroupsByGroupIdParfaitsByParfaitIdBackground` | `ParfaitRemoteDataSource#changeCanvasBackground(groupId, parfaitId, background)` |
| POST `/api/v1/test/parfait-canvas/rotate` | — (해당 없음) | — (해당 없음) |

**신규 둘의 앱 쪽 비용은 비대칭이었다.** 상세 조회는 응답이 `GetTodayParfaitResponse` 재사용이라
DTO·VO·매퍼를 그대로 썼다 — Service 함수 하나와 DataSource 함수 하나로 끝났다. 배경 변경은 요청·응답
DTO가 함께 새로 생겼다. **DI 등록 줄은 한 줄도 늘지 않았다**(Service·DataSource가 이미 바인딩돼 있고
함수만 늘었다 — PR #250에 이어 두 번째).

- **응답 DTO**: `ParfaitYearsResponse`(`years: List<Int>`) · `GetTodayParfaitResponse`(중첩
  `GroupMemberResponse`·`BackgroundResponse`·`TodayParfaitImageResponse`·`PlacedByResponse`) ·
  `PastParfaitsResponse`(중첩 `PastParfaitResponse`) · `ChangeParfaitBackgroundResponse`(중첩
  `BackgroundResponse`를 **조회 응답 파일의 것 그대로 재사용** — 서버도 같은 클래스를 쓴다) — 전부
  `data/service/model/response/parfait/`.
- **요청 DTO**: `ChangeParfaitBackgroundRequest`(`data/service/model/request/parfait/`) — **이 도메인 첫
  요청 DTO**다(그전엔 전부 GET). 서버의 거울이라 `type`·`value`·`imageId` 평면·널 허용 그대로 두고,
  잘못된 조합을 막는 일은 domain `CanvasBackgroundEdit`가 한다(DTO에 sealed를 넣지 않는 규약).
  ⚠️ **중첩 응답은 상위 응답 파일 안에 함께 둔다** — `:data`의 "선언당 파일 하나" 규약의 명시적 예외이고
  근거는 "서버가 한 파일에 담은 것을 앱도 한 파일에 담아야 계약 문서와 눈으로 대조된다"이다
  ([data-layer](../architecture/data-layer.md)). `PlacedByResponse`라는 이름이
  `response/parfait`·`response/parfaitimage` **두 패키지에 각각 존재**하는 것도 같은 이유였다(서버가 그랬다).
  🔁 **2026-08-19에 그 근거가 사라졌다** — 서버가 토핑 배치 쪽을 `PlaceParfaitImagePlacedByResponse`로
  개명해 이름 충돌을 없앴다(springdoc이 두 스키마를 같은 것으로 취급해 `nameTagChip` 추가가 스웨거에
  안 보이던 문제 때문이다, [parfait-image.md](parfait-image.md)). 앱은 두 이름을 그대로 두고 있어
  **더는 서버의 거울이 아니다** → [open-questions](../synthesis/open-questions.md) [2026-08-19].
- **domain VO**: `domain/model/canvas/`에 일곱(`CanvasVO`·`PastCanvasVO`·`CanvasStatus`·
  `CanvasBackground`·`CanvasBackgroundEdit`·`CanvasMemberVO`·`CanvasToppingVO`). 이름은 제품 언어라 서버
  `parfait`가 `Canvas`, 응답 필드 `imageCount`가 `toppingCount`다 — 다만 **id 타입은 서버 언어 유지**
  (`ParfaitId`). 연도 조회만 VO가 없다(응답이 `years` 한 필드라 `transform = { it.years }`).
  ⚠️ **`TodayCanvasVO`는 `CanvasVO`로 개명됐다**(2026-08-16, PR #266) — 상세 조회가 같은 응답 클래스를
  쓰면서 한 타입이 오늘과 특정 날짜 양쪽을 담게 됐기 때문이다. 그 대가로 **"이 조회가 캔버스 행을
  만든다"는 경고의 소유가 타입에서 함수로 옮겨졌다** — 오늘 조회만 만들고 상세 조회는 만들지 않으므로
  VO KDoc이 그 성질을 대표할 수 없다 → [open-questions](../synthesis/open-questions.md).
- **Mapper**: `source/parfait/mapper/VOMapper.kt`(이 도메인 첫 매퍼 — 연도 조회뿐이던 시절엔 없었다).
  계약의 "널이 세 가지를 뜻한다"를 여기서 가른다 — `images` `null`은 **빈 목록으로 접고**,
  `background`(미설정)·`lastClosedDate`(마감 이력 없음)의 `null`은 **그대로 둔다**.
  요청 방향 변환(`CanvasBackgroundEdit.toRequest()`)도 같은 파일에 있다 — **조건부 필수를 여기서 편다**
  (색이면 `value`만, 이미지면 `imageId`만 채워 서버가 값을 버릴 경로를 만들지 않는다).
- **배경 변경 반환은 `CanvasBackground?`다.** 응답 `background`는 비널인데 널 허용으로 받는 이유는
  **미지 `type`을 `null`로 접는 규칙을 조회와 통일**했기 때문이다(뜻은 "저장은 됐는데 그릴 수 없다").
  echo를 버리지 않는 이유는 **이미지 배경일 때 앱이 URL을 모르기 때문**이다 — 요청은 id로 보내고 응답에
  저장된 URL이 실려 오며, 그 값이 화면이 그릴 값이다.
- **미지 값 폴백 세 갈래**: `status`가 모르는 값이면 `CanvasStatus.UNKNOWN`(값 자체가 상태라 버릴 수 없다),
  `background.type`이 모르는 값이면 **`null`로 접는다**(미지와 미설정은 화면 처리가 같다).
  토핑 테두리도 `SOLID`인데 색·두께가 비면 `ToppingBorder.None`으로 떨어뜨린다 — 서버가 저장 시점에
  막지만 이미 저장된 행이 있을 수 있어서다. **두께가 있어도 그대로 쓰지는 않는다**(2026-09-08, PR #464)
  — `ToppingBorder.solidClamped` 가 `WIDTH_RANGE_DP`(2.0..30.0)로 가둔다. 서버가 범위를 검증하지 않는
  자리를 앱이 임시로 메운 것이다([parfait-image](parfait-image.md) 「Android 매핑」).
  세 번째가 `placedBy.ownerType`이다(2026-08-26, PR #376) — `"ME"`가 아닌 **모든 값**이 `isMine = false`다.
  방향을 그쪽으로 고른 근거는 매퍼 KDoc이 적어 두었다: 여는 쪽으로 틀리면 **남의 토핑을 만지게 되고**
  접는 쪽으로 틀리면 내 토핑을 못 만질 뿐이다. 계약상 이 필드는 비널이므로 `null` 갈래는 서버가
  안 주는 경우가 아니라 **구 버전 앱이 새 서버를 만나는 반대 방향**을 위한 자리다.
- **범위 파라미터는 `null` 그대로 보낸다** — `from`·`to`가 `null`이면 Retrofit이 쿼리를 URL에서 빼므로
  서버 기본값(오늘 − 30일 ~ 오늘)이 산다. 문자열 변환(`LocalDate.toString()`)은 DataSource가 한다.

설계 근거는 [specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer](../superpowers/specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md)와
신규 둘의 사후 스펙 [specs/archive/2026-08-16-canvas-detail-background-api-service-layer](../superpowers/specs/archive/2026-08-16-canvas-detail-background-api-service-layer.md).
DataSource 테스트는 29 케이스이고, 배경 변경 요청 바디의 **조건부 필수 두 갈래를 `coVerify` 인자 비교로**
잠근다(매퍼 단독 테스트를 만들지 않는 규약 그대로).

**`today` 조회가 C-001 캔버스 결선의 선행이었다.** "배치 목록 조회 API가 없어 캔버스를 다시 그릴 수 없다"던
자리가 이 엔드포인트로 닫혔고([parfait-image.md](parfait-image.md) 참고) **2026-08-17 화면까지 이어졌다.**
⚠️ **부작용 있는 GET을 억제하는 코드 수단은 여전히 없다** — 경고가 Service·DataSource·Repository KDoc에만
있고, 실제 억제는 화면의 `launch(key = LOAD_TODAY_CANVAS_KEY)` **하나**에 걸려 있다(다른 소비자가 생기면
같은 규율을 스스로 지켜야 한다) → [open-questions](../synthesis/open-questions.md).

✅ **쓰기도 배치(POST) 하나는 붙었다**(2026-08-22 develop 머지, PR #334) — `CanvasToppingPlaceViewModel` → `AddToppingUseCase` → `ToppingRepository.place`가
화면에서 새 토핑을 얹는다. **좌표 수정(위치 PATCH)·테두리 PATCH·DELETE는 여전히 소비처 0건**이라
화면은 그 셋에 대해서는 여전히 **서버가 가진 배치를 그리기만 하고 고치거나 지우지는 못한다**
([parfait-image.md](parfait-image.md)).

✅ **`today`·상세 두 갈래를 부르는 주체가 화면에서 저장소 층으로 내려갔다**(2026-08-31 develop 머지, PR #404).
엔드포인트·요청·응답은 하나도 안 바뀌었고 `android_status`도 `done` 그대로다. 바뀐 것은 **누가 언제 부르는가**다 —
세 화면이 각자 부르던 자리를 `:data`의 `CanvasPoller` 하나가 가져가고, 화면은 `ParfaitRepository.todayCanvas(groupId)`
`Flow`를 구독만 한다. 주기 갱신은 **부작용 없는 상세 조회**를 쓰고 `today`는 **캐시가 비었거나 실린 날짜가
오늘이 아닐 때**, 즉 캔버스를 만들 필요가 있을 때만 부른다. 위가 "억제가 화면의 `launch(key)` 하나에 걸려
있다"고 적던 상태는 끝났다 — **억제 지점이 폴러의 그룹별 진행 중 가드 한 곳으로 모였고** 다른 소비자가
따로 규율을 지킬 필요가 없다. 다만 **경계 티커가 발화하면 그 그룹을 보고 있는 클라이언트가 각자 한 번씩
`today`를 태우므로** 서버 쪽 중복 생성 방지 여부는 여전히 확인되지 않았다(OQ-P-323).

✅ **2026-08-31 서버 delta가 낸 공백을 같은 날 닫았고 이튿날 develop에 들어왔다**(PR #428 `e870fb87`) — 과거 목록 원소의 `status`를 앱 DTO
`PastParfaitResponse`와 `PastCanvasVO`가 받는다. 매핑은 `today`·상세가 이미 쓰던
`toCanvasStatus()` 재사용이라 미지 값 폴백(`CanvasStatus.UNKNOWN`)도 그대로다.

⚠️ **달력 점 기준은 옮기지 않았다.** `CanvasMainViewModel.uploadedDates`는 계속
`PastCanvasVO.isEmpty`(토핑 개수)를 쓴다 — 위키 [[C-201-캘린더-정책-v0.1]]이 인디케이터를
"토핑 1개 이상 = True, 0개 = False"로 규정하고 그 판정이 정본과 일치한다. 서버 `EMPTY`는
"0건으로 마감된 날"이라 뜻이 좁아, 옮기면 진행 중인 오늘의 빈 캔버스에 점이 찍힌다. 두 값이
같지 않다는 것을 `PastCanvasVO.isEmpty` KDoc이 담는다. **읽는 화면은 아직 0건이다**
→ [open-questions](../synthesis/open-questions.md) OQ-P-333.

✅ **`groupName`을 하루 만에 읽기 시작했다**(2026-09-08, PR #469 develop 머지). `GetTodayParfaitResponse`
(`:data`)·`CanvasVO`(`:domain`)에 자리가 서고 `toCanvasVO`가 `GroupName`으로 감싼다. 오늘·상세 두 조회가
같은 매퍼를 타므로 값은 두 경로 모두에 실린다.

**정본을 캔버스로 옮기지 않은 것이 이 결선의 결정이다.** C-001 상단 바의 그룹명은 여전히 그룹 목록
캐시가 정본이고(`loadCanvasMainInfo`, [ADR-0023](../adr/0023-group-in-memory-ssot.md)), 캔버스가 준
이름은 **그 캐시가 아직 비어 있을 때만** 상태를 채운다(`groupName.ifEmpty { … }`). 캔버스 SSoT
([ADR-0029](../adr/0029-canvas-today-ssot-polling.md))가 그룹의 값을 자기 캐시의 정본으로 삼으면 같은
값의 출처가 둘이 되기 때문이다. 그래서 **서버가 없애 주려던 왕복(목록 조회)은 실제로는 남아 있고**,
이 필드가 실제로 값을 내는 자리는 **목록 캐시가 빈 진입** — 푸시 딥링크·프로세스 재시작 복귀다.

**같은 라운드가 이름을 지우던 자리도 고쳤다.** 목록 캐시에 그 그룹이 없을 때 `orEmpty()`로 접어
빈 문자열을 쓰던 것이 `return@collect`로 바뀌었다 — 그러지 않으면 캔버스가 채운 이름이 목록 방출
때마다 지워져, 부트스트랩이 곧바로 무효가 된다. ⚠️ 뒤집으면 **목록에서 사라진 그룹의 옛 이름이
화면에 남는다**(탈퇴·삭제 직후). 그 경로는 화면을 떠나는 흐름이라 지금은 드러나지 않는다.

## 미결

- 과거 목록 원소의 `status`를 VO까지 받았으나 읽는 화면이 0건이다(달력 점은 개수 축을 그대로 쓴다)
  → [open-questions](../synthesis/open-questions.md) OQ-P-333
- **경로 세그먼트 `year`(단수) vs 응답 필드 `years`(복수) 불일치.** 서버 코드로는 의도된 설계인지 실수인지
  확인할 수 없다 — 근거 자료(PR 설명·이슈) 조사는 이번 범위 밖. → [open-questions](../synthesis/open-questions.md)
- `GET .../today`가 조회인데 캔버스 행을 만든다(부작용 있는 GET), 그리고 오늘 날짜가 이미 마감돼 있으면
  마감된 캔버스를 "오늘"로 돌려준다 → [open-questions](../synthesis/open-questions.md)
- 과거 목록의 `thumbnailUrl`이 항상 `null`이고 페이지네이션·범위 상한이 없다
  → [open-questions](../synthesis/open-questions.md)
- 테스트 전용 회전 엔드포인트가 인증 없이 전 그룹 캔버스를 마감한다(프로덕션 제거 TODO)
  → [open-questions](../synthesis/open-questions.md)
- `images[].placedBy`가 탈퇴 멤버를 걸러내지 않아 `groupMembers`에 없는 `groupMemberId`와 `(알수없음)`
  닉네임이 섞인다 — **2026-08-20부터 이 값을 실제로 읽는 화면이 생겼다**(C-202 Spotlight 토스트)
  → [open-questions](../synthesis/open-questions.md)
- 배경 이미지가 `reference_count`를 올리지 않아 같은 이미지의 토핑을 지우면 배경이 깨질 수 있다
  → [open-questions](../synthesis/open-questions.md)
- 배경 이미지를 요청은 `imageId`로 받고 응답·조회는 URL로만 내려줘 앱이 현재 배경의 이미지 id를 되짚을
  수단이 없다 → [open-questions](../synthesis/open-questions.md)
- 상세 조회가 상태를 거르지 않아 "이전 파르페 상세"라는 이름과 달리 오늘의 `ACTIVE` 캔버스도 조회된다.
  같은 캔버스를 `today`와 상세 두 경로로 얻을 수 있고 **한쪽만 부작용이 있다**
  → [open-questions](../synthesis/open-questions.md)
- **테스트 전용 회전이 오늘 캔버스를 강제로 마감한다**(2026-08-18) — 호출 뒤 `today`가 `CLOSED` 캔버스를
  돌려주는 상태를 아무나 만들 수 있다 → [open-questions](../synthesis/open-questions.md)

✅ **2026-08-19 해소 2건** — ① 하루 경계가 서버 안에서 갈려 있던 것(과거 목록 `to` 기본값만 자정)이
`ParfaitDay.current()`로 통일됐다. ② `nameTagChip`이 `placedBy`에만 있고 `groupMembers`에는 없어
캔버스 상단 멤버 칩을 계약으로 정할 수 없던 것이 닫혔다. **남은 것은 앱이 03시 경계와 칩 필드를
따라오는 일**이었고 그것도 2026-08-20에 닫혔다 → [open-questions](../synthesis/open-questions.md).

✅ **2026-08-20 해소 1건** — 배경 변경이 마감 상태를 보지 않던 것을 서버가 409 `PARFAIT_ALREADY_CLOSED`로
막았다(OQ-P-189). 앱이 "막는 것은 화면 책임"이라고 적어 둔 자리 일곱 곳이 그 순간 거짓이 됐고,
**같은 날 PR #318이 그 일곱을 전부 고쳤다**(경로가 아니라 서술의 문제라 `⚠️불일치`였던 적은 없다)
→ [Android 매핑](#android-매핑) · [open-questions](../synthesis/open-questions.md).

✅ **2026-08-26 해소 1건** — 앱이 "내 토핑"을 가려낼 재료가 계약에 없던 것을 서버가
`placedBy.ownerType`으로 닫았다(OQ-P-250 ①). 예고했던 남은 둘 중 **앱이 그 값을 읽는 일은 같은 날
닫혔고**(PR #376), **목적지는 하루 뒤에 생겼다**(2026-08-27 PR #400 — 새 화면이 아니라 C-301 편집
화면의 토핑 탭이 받는다) → [Android 매핑](#android-매핑) ·
[open-questions](../synthesis/open-questions.md).
