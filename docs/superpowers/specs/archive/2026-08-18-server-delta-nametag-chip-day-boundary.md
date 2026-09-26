---
id: server-delta-nametag-chip-day-boundary
title: 서버 delta 08df1bf 반영 — Nametag-Chip 서버 배정·그룹 상세 확장·하루 경계 03시
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-20
related_code: NametagChipType, MyParfaitGroupDetailResponse, ParfaitGroupMemberResponse, MyParfaitGroupResponse, PlacedByResponse, ParfaitGroupDetailVO, ParfaitGroupMemberVO, MyParfaitGroupVO, GroupDetailVO, GetGroupDetailUseCase, ParfaitGroupRepository, GroupSettingViewModel, GroupListScreen, YGColorChipType, YGGrouptagChipType, ParfaitDay, parfaitToday, GetTodayParfaitUseCase, GetParfaitYearsUseCase, CanvasMainViewModel
related_adr: ADR-0023, ADR-0017
related_spec: group-ssot, s101-group-setting-api, c001-canvas-today-detail, c201-canvas-calendar-server
related_architecture: data-layer, design-system, state-management
supersedes:
superseded_by:
tags: [spec, parfait, group, canvas, server-contract, design-system]
---

# Spec: 서버 delta 08df1bf 반영

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ✅ **develop 머지 완료(2026-08-20, PR #308 `feature/#300-sync-backend-api-250818` → develop `412991ea`)** —
> 선행 PR #307이 먼저 들어간 직후 같은 순서로 머지됐고 머지 커밋에 충돌 해소 편집이 0건이다.
> 유닛 테스트는 511 → 532건. **as-built 차이 ①의 전제가 이 머지로 사라졌다** — `YGColorChipType.Default`는
> 이제 develop에 있으므로 PR #298이 같은 타입을 다시 더할 자리가 없다(그 PR이 열려 있다면 그 블록은
> 충돌로 드러난다).
>
> ⚙️ **구현 완료 시점 기록(2026-08-18, 브랜치 `feature/#300-sync-backend-api-250818` = PR #308, `refactor/#294-group-data-using-ssot`(PR #307) 위 커밋 20개)** — 계획 8 Task가
> 전부 들어왔다. **뒤집힌 결정 1건**(아래 결정 5 참고: 호출부 무변경을 테스트 이음매 금지로까지 읽은 것이
> 과했다), as-built 차이 2건, park 2건.
>
> **as-built 차이 ①** — `YGColorChipType.Default`를 이 브랜치에서 자체 신설했다. 열린 PR #298이 같은 타입을
> 추가하지만 develop에 없어 여기서 쓸 수 없다. 이름·토큰·KDoc·위치를 #298과 글자까지 맞춰 머지 충돌이 한
> 블록을 지우는 것으로 끝나게 뒀다.
> **as-built 차이 ②** — 스펙이 "호출부는 안 바뀐다"고 적으며 감사한 목록이 **`parfaitToday()`를 이미 부르는
> 곳만** 셌다. 불러야 했는데 안 부르던 두 곳(`GroupListViewModel.updateToday()`·`CustomCameraScreen`의
> 날짜 라벨)이 기기 자정으로 오늘을 세고 있어, 이 브랜치가 00:00~03:00에 **목록 헤더 D / 캔버스 D−1**이라는
> 회귀를 만들었다. 최종 리뷰가 잡았고 둘 다 `parfaitToday()`로 바꿨다.
>
> **park 2건** — 상세 조회가 빈 캐시로 실패할 때 `remainingCount = 0`이 "정원이 찼어요"로 읽히는 것
> (OQ-P-225, 실패 표현은 이 스펙 범위 밖) · 캔버스 상단 멤버 칩이 여전히 인덱스 순환인 것
> (OQ-P-224 ①, 서버가 `groupMembers`에 칩을 안 준다).
>
> 실기기·실서버 확인은 하지 않았다 — 이 저장소의 모든 계약 라운드와 같이 코드 대조까지다.

서버 `main`이 `22717fe` → `08df1bf`로 오면서 **엔드포인트는 안 늘고 응답 필드 넷과 "오늘"의 정의가
바뀌었다**([api/server-baseline.md](../../../api/server-baseline.md) 9회차). 이 스펙은 그 delta를 앱에 반영한다.

작업 브랜치는 **`feature/#300-sync-backend-api-250818`**(PR #308)이고 그룹 SSoT 브랜치
**`refactor/#294-group-data-using-ssot`**(PR #307) 위에 얹혀 있다. 그룹 상세·목록을 인메모리 SSoT로 옮긴
[group-ssot](2026-08-17-group-ssot.md) 배선이 이 delta가 닫는 자리와 같은 파일이라, 그 위에 얹는 편이
`combine` 제거·TODO 삭제까지 한 번에 끝난다.

> 🔁 **작업 당시 이 둘은 한 브랜치 `feature/#294-group-ssot`(PR #299)였다.** 그 PR은 닫혔고 SSoT와
> 계약 반영이 PR #307 → #308 스택으로 갈렸다. 2026-08-20에 #294가 develop `c36cad49` 위로
> 리베이스되면서 이 브랜치도 그 위로 다시 얹혔다(고유 커밋 20개는 그대로).

## 서버가 바꾼 것

| 계약 | 신설 | 뜻 |
|---|---|---|
| 그룹 상세 | `groupName` · `memberLimit` | 앱이 메우던 공백 둘 |
| 그룹 상세 | `members[].nametagChip` | 멤버별 칩 타입 |
| 그룹 목록 | `lastPlacedByNametagChip` | 마지막 토퍼의 칩 타입 |
| 캔버스 | `placedBy.nametagChip` | 토핑 작성자의 칩 타입 |
| 오늘의 정의 | `ParfaitDay.current()` | 하루가 자정이 아니라 **03시**에 넘어간다 |

칩 배정 규칙은 서버 소관이 됐다 — 참여·생성 시 **그 그룹의 활동 멤버가 안 쓰는 값 중 무작위**,
탈퇴 시 `RELEASED` 반납, 재배정 경로 없음. 유일성은 **그룹 안에서만** 성립해 계정 공통이 아니다
([api/parfait-group.md](../../../api/parfait-group.md) "Nametag-Chip 배정 규칙").

## 결정

### 1. 칩 타입은 `:domain`에 중립 enum으로 받는다

`:domain`은 순수 JVM(`:core:util:jvm`만 의존)이라 `YGColorChipType`을 모르고, `:core:designsystem`은
서버를 모른다. 둘을 잇는 자리는 feature다.

```
NametagChipType(:domain)  ←  서버 문자열(:data 매퍼)
        ↓ feature impl 확장 함수
YGColorChipType / YGGrouptagChipType(:core:designsystem)
```

- 값은 `TYPE1`~`TYPE12` + `RELEASED`.
- **`RELEASED`를 남긴다.** 화면 표현은 지금 `Default`로 같지만 "나간 사람"과 "값이 없다"는 뜻이 다르다.
  계약이 갈라 주는 것을 매퍼가 뭉개면 되돌릴 수 없다.
- **미지 문자열은 `null`로 접는다.** `CanvasBackground.type` 폴백과 같은 규약이고, `CanvasStatus.UNKNOWN`
  쪽 관용구는 쓰지 않는다 — 상태는 값 자체가 정보지만 칩은 그리지 못하면 그만이다.

### 2. 값이 없으면 `Default`로 그린다

디자인시스템에 `Default` 변형이 생겼다(Figma Nametag-Chip `144:5415` · Grouptag-Chip `3733:9410`).
`null`·`RELEASED`·미지 값 셋을 전부 여기로 접는다 — 폴백을 앱이 지어내지 않는다.

| 컴포넌트 | 신설 | 토큰 |
|---|---|---|
| `YGColorChipType.Default` | 필요 | fill `Gray.White` · stroke `Gray.Gray100` · text `Gray.Gray300` |
| `YGGrouptagChipType.DEFAULT` | 필요 | timestamp `Gray.Gray300` |

Figma의 Nametag-Chip `Default`는 글자가 닉네임 첫 글자가 아니라 **`-`**다. `YGNametagChip`은 글자를
파라미터로 받으므로 컴포저블은 안 바뀌고 호출부가 넘긴다(`NametagChipPlus`의 `+7`과 같은 방식).

**이 라운드에서 `-`를 넘기는 호출부는 없다.** S-101 멤버 목록은 서버가 탈퇴자를 빼고 주므로 `Default`가
계약상 나오지 않고, 나온다면 그건 계약 위반이라 닉네임 첫 글자가 오히려 단서다. `-`가 뜻을 갖는 자리는
"알 수 없는 사용자"를 그리는 C-202 작성자 표시이고 그것은 PR #298 소관이다 — 타입만 세워 두고 글자
규칙은 그 화면이 정한다.

> ⚠️ **`YGColorChipType.Default`는 열린 PR #298(스포트라이트 토핑)도 추가한다.** 이름·토큰·위치가 같아
> 머지 충돌은 한 블록이고 해소가 자명하다. 이 스펙은 #298이 develop에 없는 상태를 전제로 자체 신설하되
> **정의를 글자까지 맞춘다.** 먼저 머지되는 쪽이 남고 뒤가 그것을 쓴다.

### 3. 캔버스 `placedBy`는 DTO까지만 받는다

응답 DTO는 서버의 거울이라 필드를 받는다. **도메인 VO는 건드리지 않는다.**

`ToppingPlacerVO`를 토핑 배치 확정 응답(`PlacedToppingVO`)과 공유하는데 **서버는 그쪽에 칩을 안 준다.**
지금 올리려면 타입을 가르거나 nullable로 "없다/모른다"를 뭉개야 하는데, 그 결정을 소비자 없이 굳히는
값이 없다 — 지금 `placedBy`를 읽는 화면이 0건이다. C-202 Spotlight(PR #298)가 소비처가 되지만 그쪽은
칩을 `placedBy`가 아니라 `groupMembers`에서 `GroupMemberId`로 조인해 찾는다.

> 서버 `groupMembers`에는 칩이 없어 **C-001 상단 멤버 칩은 이 라운드로 안 닫힌다.** 서버 요청 대상이다
> ([open-questions](../../../synthesis/open-questions.md) OQ-P-224 ①).

### 4. `GroupDetailVO`를 삭제한다

`GroupDetailVO`의 KDoc이 스스로 존재 이유를 "서버 응답 하나에 대응하지 않는다 — 그룹명이 상세에 없어
목록에서 따로 집어 붙인다"로 적는데, **그 전제가 사라진다.** 남기면 이름만 다른 쌍둥이가 둘 남는다.

- `ParfaitGroupDetailVO`에 `groupName`·`memberLimit`을 얹고 `GroupDetailVO`는 지운다.
- `GetGroupDetailUseCase`의 `combine`이 사라져 `repository.groupDetail(groupId)` 통과가 된다.
  **껍데기가 되지만 남긴다** — feature가 Repository를 직접 보지 않는 계층 규약이 그대로다.
- `myNickname`이라는 이름이 사라지는 대신 `ParfaitGroupDetailVO.groupNickname` KDoc에 "인증 회원 본인의
  그룹 닉네임"을 박는다(서버 계약이 그 이름을 쓴다).
- `ParfaitGroupRepository.refreshGroupDetail`의 `TODO(서버 응답 확장 대기)`를 걷는다.

### 5. 하루 경계를 03시로 옮긴다

`parfaitToday()`가 KST 자정 기준이라 **00:00~03:00에 계약과 어긋난다** — 서버가 준 정상 응답(D−1 날짜의
`ACTIVE` 캔버스)을 `GetTodayParfaitUseCase`가 "자정을 걸친 요청"으로 오인해 **한 번 더 부르고**
(부작용 있는 GET이 두 배), 화면은 캘린더 오늘(D) 아래 D−1 캔버스를 그린다. C-201 달력의 오늘 강조·
미래 잠금도 같은 값을 쓴다.

- `domain/model/ParfaitDay.kt`의 `parfaitToday()`에 03시 롤오버를 넣는다.
- **상수는 새로 만들지 않고 `DayWindow.DAY_BOUNDARY_HOUR`를 쓴다.** 같은 `:domain`에 이미 03시 경계가
  있고(갤러리 하루 윈도우가 쓴다) 두 번 적으면 서버가 배치 시각을 바꿀 때 한쪽만 고쳐진다.
- KDoc에 **"서버 `ParfaitDay`의 거울 — 서버가 배치 시각을 바꾸면 같이 바꾼다"**를 박는다. 지금 계약에
  그 값을 내려주는 필드가 없어 앱이 복제하는 것이고, 복제라는 사실이 코드에 남아야 한다.
- `GetTodayParfaitUseCase`·`GetParfaitYearsUseCase`·`CanvasMainViewModel`은 **코드가 안 바뀐다** —
  전부 `parfaitToday()`를 통과하므로 재시도 조건·달력 기준이 저절로 맞아진다.

  🔁 **정정(as-built)** — 이 문장을 "테스트 이음매도 열지 않는다"로 읽은 것이 과했다. `GetTodayParfaitUseCase`가
  `parfaitToday()`를 기본 시계로 부르는 한 **이 브랜치의 핵심 동작(03시 이전에 재조회가 안 나간다)을 어떤
  테스트도 잠그지 못한다** — 테스트가 검증 대상과 같은 함수로 기대값을 만들어 경계를 자정으로 되돌려도
  전부 통과한다. `invoke`에 `clock: Clock = Clock.System`을 더했다(`parfaitToday`·`DayWindow.current`가
  이미 쓰는 이 저장소의 관용구, Hilt 바인딩 불필요). **운영 호출부는 그대로**이므로 위 문장의 취지는 살아 있다.

  🔁 **정정(as-built)** — 위 목록이 감사 범위였는데 그 범위가 틀렸다. `parfaitToday()`를 **부르지 않던**
  두 곳(`GroupListViewModel.updateToday()`·`CustomCameraScreen`)이 기기 자정으로 오늘을 세고 있었고,
  브랜치 이전에는 KST 기기에서 우연히 값이 같았다. 둘 다 `parfaitToday()`로 바꿨다.

> 서버 안에서도 두 기준이 공존한다 — 과거 목록의 `to` 기본값만 자정이다. 앱은 항상 범위를 명시해
> 부르므로 지금은 안 물린다.

## 화면 반영

| 자리 | 지금 | 바꿈 |
|---|---|---|
| S-101 멤버 칩 | `NAMETAG_CHIP_TYPES[index % 12]` | `member.nametagChip` → 12종 1:1, 없으면 `Default` |
| S-101 "N명 남음" | `MOCK_REMAINING_COUNT` | `memberLimit - members.size` |
| G-001 그룹 칩 | `CHIP_TYPES[index % 6]` | `lastPlacedByNametagChip` → 짝 묶음, 없으면 `DEFAULT` |

`YGGrouptagChipType` 6종은 Nametag 12종을 둘씩 묶은 타입이라(`TYPE_1_2`…`TYPE_11_12`) 매핑이 결정적이다.
**`TYPE1`~`TYPE12`만 그 짝으로 보내고 `RELEASED`는 `DEFAULT`로 간다** — `ordinal` 산술 하나로 12종과
`RELEASED`를 같이 다루면 `RELEASED`가 범위를 넘으므로, 분기를 먼저 두고 산술은 그 안에서 한다.
S-101(12→12)과 G-001(12→6)은 규칙이 달라 **공용 변환을 만들지 않는다** — 각 feature impl에 확장 함수로 둔다.
자리는 그 모듈의 **`util` 패키지**다(`setting/impl/util/ColorChipType.kt`·`list/impl/util/GrouptagChipType.kt`) —
화면 파일 바닥이 아니라. 같은 라운드에 G-001의 `toToppingImage`와 이미 빠져 있던 `GroupTimestamp.kt`도
그 자리로 모았고, 규칙은 [module-structure](../../../architecture/module-structure.md)에 올렸다.

`remainingCount`가 실데이터가 되면 **음수가 날 수 있다**(정원을 줄이는 경로는 없지만 캐시와 서버가
어긋난 순간). `coerceAtLeast(0)`으로 접는다.

## 범위 밖

- **C-001 상단 멤버 칩** — 서버 `groupMembers`에 필드가 없다(위 3).
- **`YGColorChipType` 팔레트 7종 문제** — 캔버스가 12종 중 7종만 도는 근거 없음(OQ-P-210 ②). 상단 칩이
  계약을 타게 될 때 같이 정한다.
- **C-202 Spotlight 토스트** — PR #298 소관.
- **`http/` 요청 모음** — 엔드포인트가 안 늘어 커버는 25/27 그대로다. 신규 필드는 응답이라 요청 파일이
  바뀌지 않는다.
- **실기기·실서버 확인** — 이 저장소의 모든 계약 라운드와 같이 코드 대조까지다.

## 테스트

TDD로 간다. 매퍼 단독 테스트는 만들지 않는다(규약) — 판단이 든 변환은 DataSource 테스트 케이스로.

| 대상 | 잠글 것 |
|---|---|
| `ParfaitDayTest`(신설) | 02:59 → 전날 · 03:00 → 당일 · 00:00 → 전날 · 23:59 → 당일 |
| `GetTodayParfaitUseCaseTest` | 00:00~03:00 응답(D−1)을 **재호출 없이** 받는다(고정 시계 주입) · 이틀 전 캔버스는 그때도 재조회한다 |
| `GetGroupDetailUseCaseTest` | `myGroups` 없이 상세만으로 `groupName`이 나온다 |
| 그룹 DataSource 테스트 | 신규 필드 4종 · `RELEASED` · `null` · 미지 문자열 → `null` |
| `GroupSettingViewModelTest` | 칩이 서버 값을 따른다 · `remainingCount` 계산 · 음수 클램프 |
| `GroupListViewModelTest` 또는 화면 단위 | 칩 매핑 · 폴백 |

그룹 도메인에는 DataSource 테스트가 없어 `ParfaitGroupRemoteDataSourceImplTest`를 신설한다(다른 도메인은
전부 갖고 있다). `MyParfaitGroupVOMapperTest`는 규약 예외로 남아 있는 파일이라 **늘리지도 손대지도 않는다** —
신규 DTO 필드가 널 허용이라 그 파일은 그대로 컴파일된다.

## 열린 질문

- **칩 배정 규칙이 정책 문서에 없다** — 위키 [[nametag-chip]]은 "타입은 유저별 고정"이라고만 적고
  부여 주체·유일성 범위를 정하지 않았는데 서버가 그룹 단위로 구현했다. `RELEASED`도 정책 밖이다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-223.
- **`groupMembers`에 칩이 없다** → OQ-P-224 ①.
- **03시 값을 앱이 복제한다** — 계약에 그 값이 없어 서버가 배치 시각을 바꾸면 조용히 갈린다
  → OQ-P-222 ②.
