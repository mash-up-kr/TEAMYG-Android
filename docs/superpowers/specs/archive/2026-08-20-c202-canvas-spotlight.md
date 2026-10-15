---
id: c202-canvas-spotlight
title: C-202 캔버스 토핑 Spotlight — 타인 토핑 탭 강조·Dim·작성자 토스트 (Canvas Spotlight)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-28
related_code:
  - CanvasToppingLayer.kt#CanvasToppingLayer
  - CanvasToppingLayer.kt#CanvasTopping
  - CanvasMainViewModel.kt#CanvasMainUiState.spotlightedToppingId
  - CanvasMainViewModel.kt#handleOnClickTopping
  - CanvasToppingVO.kt#CanvasToppingVO.isMine
  - CanvasMainViewModel.kt#resetSpotlight
  - CanvasMainViewModel.kt#CanvasMainEffect.ShowSpotlightToast
  - CanvasMainViewModel.kt#GroupMemberChip
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasMainScreen.kt#CanvasMainScreen
  - SpotlightTimeLabel.kt#toSpotlightTimeLabel
  - SpotlightToastColor.kt#toSpotlightToastNameColor
  - InstantExtension.kt#ElapsedTimeBucket
  - InstantExtension.kt#toElapsedTimeBucket
  - YGCanvas.kt#YGCanvas
  - YGToast.kt#YGToastType.Record
related_adr: ADR-0005, ADR-0010, ADR-0014
related_spec: c001-canvas-today-detail, c201-canvas-calendar-server, ygtoast, designsystem-canvas-components, c106-topping-place-api
related_architecture: design-system, state-management
supersedes:
superseded_by:
tags: [spec, parfait, canvas, topping, c-202, ui]
---

# Spec: C-202 캔버스 토핑 Spotlight

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> **사후 기록(post-hoc)** — 선작성 스펙 없이 develop 머지(PR #298, 브랜치 `feature/spotlight-topping`,
> 2026-08-20 `9b112e86`). as-built 역기록이고 **코드가 SoT**다.

## 목표

C-001 캔버스에 놓인 토핑을 탭하면 그 하나만 남기고 나머지를 어둡게 덮은 뒤, 누가 언제 쌓았는지를
토스트로 한 번 알린다. 위키 [[C-202-토핑-편집자-확인-규칙-v0.1]]([[토핑-spotlight]])이 정의한
Default ↔ Spotlighted 두 상태를 코드로 옮긴 첫 라운드다.

## 범위

- **포함**: 토핑 탭 수신 · Spotlight 상태 필드 · Dim 레이어 · 그리기 순서 재배치 ·
  작성자 토스트 1회 노출(닉네임·닉네임 색·상대 시각) · 백그라운드 복귀 시 해제 ·
  `YGCanvas`의 토스트 자리(`overlayContent` 슬롯) 신설 · `YGToastType.Record`의 닉네임 색 파라미터화 ·
  경과 시간 갈래 유틸(`ElapsedTimeBucket`).
- **제외**: **본인 토핑 탭 → C-305 편집 진입**(목적지가 아직 없다) · 탈퇴·이탈 사용자 토스트 표기 ·
  Pull-to-Refresh 시 해제(캔버스 화면에 당겨 새로고침이 없다) · **테스트**(이 라운드는 신규 유닛 0건) ·
  실기기 확인.

## 상태 기계

상태는 `CanvasMainUiState.spotlightedToppingId: ParfaitImageId?` 하나이고 `null`이 Default다.
파생 `spotlightedTopping`은 그 id로 현재 목록을 되짚어, **강조 중이던 토핑이 재조회로 사라지면
자동으로 `null`**이 된다.

| 전이 | 계기 | 코드 |
|---|---|---|
| Default → Spotlighted | 토핑 탭 | `handleOnClickTopping` |
| Spotlighted → Default | Dim 영역 탭 | `OnClickSpotlightDim` → `resetSpotlight` |
| Spotlighted → Default | 앱 백그라운드 복귀 | `OnAppReturnedFromBackground` → `resetSpotlight` |
| Spotlighted → Spotlighted | **불가** | `handleOnClickTopping`이 `spotlightedToppingId != null`이면 즉시 반환 |

직접 전환을 막은 것은 정책이 "다른 토핑을 고르려면 Default를 경유한다"고 정해 둔 그대로다.
백그라운드 복귀 감지는 Route의 `LifecycleStartEffect`이고, 같은 이유로 `ON_START` 구독 관용구가
이 화면에서 `LifecycleResumeEffect`(재진입 재조회)와 나란히 서게 됐다.

`resetSpotlight`를 별도 함수로 뽑아 둔 것은 Pull-to-Refresh가 붙을 자리를 비워 둔 것이다 —
정책은 "먼저 Default로 되돌린 뒤 새로고침"을 규정하는데 캔버스에는 아직 당겨서 새로고침이 없다.

## 렌더링 — 순서가 곧 우선순위다

`CanvasToppingLayer`가 한 `BoxWithConstraints` 안에서 네 덩어리를 순서대로 놓는다.

1. 강조 대상이 **아닌** 토핑 전부(목록 순서 = `positionZ` 오름차순 유지)
2. Dim 레이어 — `matchParentSize` · `Transparency.Black50`
3. 강조 토핑 하나
4. 판정 오버레이 — `matchParentSize` · `toppingTapInput`(🔁 2026-08-27, 아래)

결과 z는 정책이 적은 **Spotlight 토핑 > Dim > 나머지 토핑 > 배경**과 같다. 배경은 이 레이어 밖
`YGCanvas`가 그리므로 Dim이 캔버스 배경까지 덮지는 않는다 — 정책의 "선택 토핑 제외 전체 영역"을
**토핑 레이어 범위로 읽은 것**이고, 그림상 캔버스 배경은 어두워지지 않는다.

🔁 **클릭이 붙는 자리가 바뀌었다 (2026-08-27, PR #389).** 이 스펙이 적은 형태는 토핑마다
`clickableYGNoRipple`이 붙고 Dim에도 하나 붙는 것이었다. [토핑 알파 판정](2026-08-26-topping-alpha-hit-test.md)이
그 클릭을 전부 걷어내고 **전면을 덮는 판정 오버레이 하나**로 옮겼다 — 누끼 실루엣으로 판정하려면
겹친 토핑을 한자리에서 훑어야 하기 때문이다. 이 스펙의 **동작은 그대로다**: 강조 토핑이 히트면 그
토핑의 클릭이고(뷰모델이 Spotlighted에서 온 탭을 무시하는 것도 그대로), 미스면 Dim 아래를 보지 않고
곧바로 해제다. 달라진 것 둘 — ① **강조된 토핑의 투명한 여백**을 누르면 이제 해제된다(전에는 토핑
박스가 받아 아무 일도 없었다), ② Dim이 클릭을 잃으면서 스크린리더용 해제 액션이 **Dim 자체의
시맨틱스**로 남았다.

## 토스트

`ShowSpotlightToast(nickname, nicknameColor, elapsed)` 이펙트를 Spotlight 진입과 **같은 자리에서**
1회 쏜다. 상태가 아니라 이펙트라 회전·재구성으로 다시 뜨지 않는다.

**자리**는 `YGCanvas`에 새로 뚫은 `overlayContent` 슬롯이다. 이 슬롯은 캔버스 본체 `Box`의 형제라
Dim·확장 메뉴·달력보다 위에 그려지고, 상단 여백은 캔버스 위쪽 여백에 맞추되 **폭은 캔버스 폭이
아니라 화면 폭**이다. 화면(`CanvasMainScreen`)이 그 슬롯에 `YGToastHost`를 꽂고 Route가 만든
`YGToastPolicy`를 넘겨받는다 — 이펙트를 처리하는 곳이 Route라 정책 홀더도 Route가 쥔다.

이 결정이 C-001의 다른 토스트 자리까지 정했다 — 오늘 캔버스 조회 실패 토스트도 `YGScaffoldV2`
최상단이 아니라 이 호스트로 간다(OQ-P-167, C-106 결선 PR3).

**문구**는 `{닉네임}님이 {상대시각}에 쌓았어요`다. 정책 문안(`{닉네임} 님이 {상대시간} 전에
쌓았어요`)과 달리 "전"을 시각 쪽 구절이 품는다 — `오래전`처럼 "전"이 붙지 않는 갈래가 있어서다.
`YGToastType.Record.time`의 KDoc이 "조사까지 포함한 완성된 구절"이라고 그 계약을 적는다.

**상대 시각**은 `core:util:jvm`의 `ElapsedTimeBucket`이 갈래로 나누고 문구는 화면이 붙인다
(`SpotlightTimeLabel`, `strings.xml`).

| 갈래 | 조건 | 문구 |
|---|---|---|
| `JustNow` | 1분 미만 | 방금 전 |
| `Minutes` | 1시간 미만 | N분 전 |
| `Hours` | 1일 미만 | N시간 전 |
| `Days` | `LONG_AGO_DAYS` 미만 | N일 전 |
| `LongAgo` | 그 이상 | 오래전 |

기준 시각은 `topping.createdAt.toInstant(PARFAIT_TIME_ZONE)`이다 — 서버가 오프셋 없는 KST 벽시계를
주므로 기기 시간대로 재면 해외 기기에서 어긋난다. 기기 시계가 서버보다 뒤처져 **미래 시각이
들어오면 `JustNow`로 접는다**(음수 경과를 그대로 보여 주지 않는다).

**닉네임 색**은 작성자의 Nametag-Chip 타입에서 나온다(`toSpotlightToastNameColor`). 칩 자체의
글자색과는 다른 토스트 전용 매핑이고 12타입이 6색으로 접힌다. `Default`·`NametagChipPlus`는
그레이 계열과 같은 `White`다. 이 변환 때문에 `YGToastType.Record`가 `userNameColor`를 받게 됐고,
디자인시스템이 들고 있던 `Pudding500` 고정이 호출자 몫으로 내려왔다.

**작성자 칩을 어디서 얻는가가 이 라운드의 임시 결정이다.** 서버는 `placedBy.nameTagChip`을 이미
주지만 앱 DTO에서 멈춰 있어(OQ-P-224 잔여) 도메인까지 오지 않는다. 그래서 화면이 이미 들고 있는
`memberChips`에서 **같은 `groupMemberId`를 찾아 대신 쓴다** — 그 조인을 위해 `GroupMemberChip`에
`groupMemberId`가 붙었다. 목록에 없는 사람(탈퇴·이탈)은 `Default`로 떨어지는데, **서버도 그 경우
`placedBy.nameTagChip`을 `DEFAULT`로 준다**(`groupMembers`는 탈퇴자를 거르고 `placedBy`는 안 거른다,
[api/parfait.md](../../../api/parfait.md)). 즉 지금은 두 경로의 결과가 **우연히 같다** — 조인이 임시라는
사실이 화면에 드러나지 않는다.

## 파일 구성

| 자리 | 역할 |
|---|---|
| `core/util/jvm/.../extension/InstantExtension.kt` | `ElapsedTimeBucket` + `toElapsedTimeBucket` |
| `core/designsystem/.../ygcanvas/YGCanvas.kt` | `overlayContent` 슬롯 |
| `core/designsystem/.../ygtoast/YGToast.kt` | `Record.userNameColor` · 문구 조립 변경 |
| `feature/.../canvas/impl/component/CanvasToppingLayer.kt` | Dim·그리기 순서·탭 수신(2026-08-27부터 판정 오버레이) |
| `feature/.../canvas/impl/util/SpotlightToastColor.kt` | 칩 타입 → 토스트 닉네임 색 |
| `feature/.../canvas/impl/util/SpotlightTimeLabel.kt` | 갈래 → 문구(`Context.getString`) |
| `feature/.../canvas/impl/viewmodel/CanvasMainViewModel.kt` | 상태·전이·이펙트 |

`SpotlightTimeLabel`이 `stringResource` 대신 `Context.getString`을 쓰는 이유는 이펙트 처리가
`LaunchedEffect` 코루틴 안이라 컴포저블 문맥이 없어서다.

## 정책 대조 (위키 [[C-202-토핑-편집자-확인-규칙-v0.1]])

| 정책 | 구현 |
|---|---|
| 타인 토핑 탭 → Spotlight + 작성자 Toast 1회 | 일치 |
| Dim = Transparency/Black-50 | 일치 |
| z: Spotlight 토핑 > Dim > 나머지 토핑 > 배경 | 일치(배경은 Dim 밖이라 어두워지지 않는다) |
| Spotlighted → Spotlighted 직접 전환 불가 | 일치 |
| Dim 포함 강조 토핑 밖 탭 → Default | 일치 |
| 앱 백그라운드 → 복귀 시 Default | 일치 |
| **본인 토핑 탭 → C-305 편집 진입** | **일치(오늘 캔버스 한정)**(2026-08-27, PR #400) — 판정은 서버 `ownerType`(2026-08-26, PR #376), 목적지는 **C-301 편집 화면의 토핑 탭**이다(새 C-305 화면이 아니다). ⚠️ 지난 캔버스를 보는 중이면 `isViewingToday` 가드에 걸려 **여전히 무반응**이다 |
| **탈퇴·이탈 사용자는 Toast에 "알 수 없음"** | **부분** — 서버가 탈퇴 멤버의 `placedBy.nickname`을 `(알수없음)`으로 주므로 뜻은 맞지만, 문장이 정책 예시(`알 수 없는 사용자가 …`)와 달리 `(알수없음)님이 …`가 된다 |
| Pull-to-Refresh 시 먼저 Default 복귀 | 해당 없음(캔버스에 당겨 새로고침 없음) |
| Toast 노출·소멸은 [[toast]] 공통 규칙 | 호스트는 공통(`YGToastHost`)이나 **자리가 화면 상단이 아니라 캔버스 프레임 상단**이다 |

상대 시각의 갈래 경계(1분·1시간·1일·7일)와 `오래전` 문구는 정책에 근거가 없다 — 코드가 정했다.

## 드리프트 / 열린 질문

- ⚠️ **본인 토핑 판정 경로가 없다** — 서버 응답이 "내 `groupMemberId`"를 알려 주지 않아
  `isMine()`이 항상 `false`다. C-305 진입도 `TODO`로 남는다 → OQ-P-250.
  ✅ **판정은 닫혔다**(2026-08-26, PR #376 — 아래 [as-built 재정정](#as-built-재정정-2026-08-26-pr-376-develop-머지)).
  ✅ **진입도 닫혔다**(2026-08-27, PR #400 — 아래 [as-built 재정정](#as-built-재정정-2026-08-27-pr-400-develop-머지)).
  **오늘 캔버스에서만 열린다**는 조건이 새로 붙었다.
- ⚠️ **작성자 칩이 서버 값이 아니라 화면 목록 조인이다** — `placedBy.nameTagChip`이 도메인까지 오면
  그 값으로 바꾼다는 `TODO`가 붙어 있다(OQ-P-224 잔여). 지금은 탈퇴 멤버에서도 두 경로의 결과가
  같아 **틀린 색이 보이지는 않는다** → OQ-P-251.
- 탈퇴 멤버 문장이 `(알수없음)님이 …`가 된다 — 서버 문자열을 그대로 쓰는 결과이고 정책 예시 문안과
  형태가 다르다 → OQ-P-251 ②.
- ⚠️ **이 라운드는 신규 유닛 테스트가 0건이다** — 판단이 몰린 순수 함수 둘
  (`toElapsedTimeBucket`·`toSpotlightToastNameColor`)이 덮이지 않았다 → OQ-P-252.
- 상대 시각 갈래 경계와 `오래전` 문구에 정책 근거가 없다 → OQ-P-251 ③.
- 토스트 폭이 캔버스가 아니라 화면 기준이라, 캔버스 좌우 여백 위까지 토스트가 걸친다.
- 실기기 확인 없음.

## as-built 재정정 (2026-08-26, PR #376 develop 머지)

> **비어 있던 본인 갈래가 반쯤 찼다** — 판정이 붙었고 목적지는 아직 없다.
> 브랜치 `feature/#373-sync-backend-api-260825`, 머지 `c55e10bc`, 커밋 둘.

- **상수 `false`가 사라졌다.** `CanvasToppingVO.isMine()`(항상 `false`를 돌려주던 private 확장 함수)이
  통째로 지워지고, `handleOnClickTopping`이 도메인 모델의 프로퍼티 `topping.isMine`을 그대로 읽는다.
  값의 출처는 서버다 — 캔버스 응답 `images[].placedBy.ownerType`(`ME`·`OTHER`)을 `:data` 매퍼가
  `"ME"`인지 여부로 접어 넣는다(계약은 [api/parfait.md](../../../api/parfait.md)).
- **그래서 "본인 토핑도 Spotlight로 들어간다"가 멎었다.** 정책이 Spotlight 대상에서 빼라고 한 갈래가
  실제로 빠졌고, 자기 닉네임이 적힌 토스트가 자기에게 뜨던 것도 함께 사라졌다.
- ⚠️ **그런데 간 곳이 없다.** 본인 갈래는 `TODO: C-305 토핑 편집 화면으로 이동` 한 줄 뒤에 그냥
  `return`한다. 즉 **본인 토핑 탭은 무반응**이다 — 정책이 정한 "편집으로 보낸다"는 여전히 미구현이고,
  이제는 판정이 없어서가 아니라 **목적지가 없어서**다 → OQ-P-250 ③.
  ✅ **하루 뒤에 닫혔다**(2026-08-27, PR #400 — 아래 절).
- 위 KDoc도 그 사실에 맞춰 고쳐졌다 — "경로가 없어 `isMine`이 항상 false다"라는 문장이 빠지고
  "진입점이 아직 없어 아무 동작도 하지 않는다"만 남았다.

### 유닛 테스트

`CanvasMainViewModelTest`에 둘이 붙었다 — 남의 토핑 탭이 Spotlight와 작성자 토스트로 이어지는지,
본인 토핑 탭이 **Default에 머무는지**(`spotlightedToppingId`가 `null`). 후자가 이번 변경의 회귀
감지선이다. 이 클래스는 27건이 됐다.

⚠️ **이 스펙이 적어 둔 신규 유닛 0건(OQ-P-252)은 그대로다** — 붙은 둘은 클릭 갈래를 덮고, 판단이
몰린 순수 함수 둘(`toElapsedTimeBucket`·`toSpotlightToastNameColor`)은 여전히 안 덮였다.

## as-built 재정정 (2026-08-27, PR #400 develop 머지)

> **본인 갈래가 마침내 어딘가로 간다** — 다만 목적지는 새 화면이 아니라 이미 있던 화면이다.
> 브랜치 `feat/canvas-touch-mine`, 머지 `a22583a3`, 커밋 하나.

- **`TODO`가 `handleOnClickMyTopping`으로 바뀌었다.** `handleOnClickTopping`의 본인 갈래가
  `CanvasMainEffect.NavigateToCanvasBGEdit`를 쏘고, 그 이펙트에 **탭한 토핑의 `parfaitImageId`**가
  실린다(`toppingId: ParfaitImageId? = null` — 편집 버튼으로 들어오는 기존 경로는 이 값을 안 채운다).
  Route가 그것을 `NavKeyCanvasBGEdit.initialToppingId`로 넘긴다.
- ⚠️ **C-305라는 새 화면이 생긴 것이 아니다.** 정책([[C-202-토핑-편집자-확인-규칙-v0.1]])이 말하는
  편집 진입을 **C-301 편집 화면의 토핑 탭**이 받는다. `CanvasBGEditViewModel`의 `withCanvas`가 첫
  조회 결과에 `selectedTab = TOPPING`과 `selectedToppingId = initialToppingId`를 얹어, 화면이 그
  토핑을 선택한 상태로 열린다. 위키 화면 ID 체계에서 C-305가 별도 화면인 것과 구현이 갈린 자리다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-250.
- ⚠️ **오늘 캔버스에서만 열린다.** `handleOnClickMyTopping`이 `isViewingToday`와 `todayCanvas`를
  둘 다 확인하고, 하나라도 어긋나면 조용히 반환한다 — 지난 캔버스를 보는 중에는 본인 토핑 탭이
  **종전대로 무반응**이다. 편집 대상이 언제나 오늘 캔버스라는 `NavKeyCanvasBGEdit`의 기존 계약을
  따른 결과이고, 정책은 그 조건을 적지 않는다.
- 남의 토핑 갈래(Spotlight + 작성자 토스트)는 한 줄도 안 바뀌었다.

### 유닛 테스트

`CanvasMainViewModelTest`에서 "본인 토핑 탭이 Default에 머무는지"를 보던 회귀 감지선이
`clickTopping_placedByMe_navigatesToCanvasBGEditInsteadOfSpotlighting`으로 바뀌어 **이동 이펙트와
실린 토핑 id를 함께 단언한다.** 새 조건인 지난 날 무반응은
`clickTopping_placedByMe_whileViewingAPastDate_doesNothing`이 `expectNoEvents()`로 잠갔고, 그
테스트의 주석이 가드의 이유도 적는다 — 편집 화면은 넘겨받은 `parfaitId`와 무관하게 오늘 캔버스를
다시 조회하므로 지난 날에서 열면 엉뚱한 캔버스가 열린다. `CanvasBGEditViewModelTest`에는
`init_withInitialToppingId_opensToppingTabWithThatToppingSelected`가 붙었다. 저장소 유닛은
926 → 931건이 됐다(이 PR과 PR #369·#398을 합친 수).

## as-built 재정정 (2026-08-30, PR #405 develop 머지)

> **작성자 토스트가 겹치지 않고 교체된다** — 발행 자리는 그대로이고 정책 홀더가 갈래를 하나 얻었다.
> 브랜치 `feature/#391-canvas-topping-spotlight`, 이슈 #391.

토핑을 하나 탭해 Spotlight를 켜고 Dim을 눌러 끈 뒤 **자동 소멸 시간 안에** 다른 토핑을 탭하면 앞
토스트가 아직 살아 있다. `YGToastHost`는 토스트를 오프셋 없이 `Box` 안에 그리므로 두 작성자 안내가
**같은 자리에 포개져** 둘 다 못 읽는다.

- **`YGToastPolicy.show`가 `replaceTag`를 받는다.** 같은 태그가 떠 있으면 걷어내고 새것만 남긴다.
  기본값이 `null`이라 태그를 안 주는 호출부는 종전대로 쌓인다 →
  [ygtoast 스펙](2026-07-23-ygtoast.md)의 같은 날짜 재정정.
- **태그를 주는 곳은 `ShowSpotlightToast` 하나다.** 상수는 `CanvasMainRoute`의 private
  `SPOTLIGHT_TOAST_TAG`이고, 같은 화면의 갤러리 저장·오늘 캔버스 조회 실패 토스트는 태그 없이
  발행해 위키 [[Toast-공통-정책]]의 스택 규칙을 그대로 따른다.
- **`Spotlighted`에서 다른 토핑으로의 직접 전환은 계속 막는다**(위 본문의 C-202 규칙 그대로).
  이 수정이 다루는 것은 **Dim을 눌러 `Default`를 거쳐 다시 탭하는 동선** 하나다.

**계측 테스트가 붙었다** — `YGToastHostTest` 신설(계측 3건, 계측 총량 14 → 17건, 파일 5 → 6).
같은 태그면 뒤엣것만 남고, 태그가 다르거나 없으면 쌓이는 것을 잠근다. ⚠️ **CI는 계측을 컴파일만
한다**(OQ-P-102 ②) — 이 감지선은 사람이 기기에서 돌릴 때만 운다.

⚠️ 태그 문자열은 정책이 아니라 화면이 갖는다. 다른 화면·다른 발행자가 같은 문자열을 고르면 서로의
토스트를 지우는데 그것을 막는 장치가 없다 → OQ-P-329.
