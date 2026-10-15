---
id: c001-canvas-today-detail
title: C-001 캔버스 오늘·날짜별 조회 결선 (ParfaitRepository + 토핑 렌더 + G-001 진입)
status: implemented
category: feature-spec
platforms: android
verified: 2026-09-04
related_code: ParfaitRepository, ParfaitRepositoryImpl, GetTodayParfaitUseCase, GetParfaitDetailUseCase, parfaitToday, PARFAIT_TIME_ZONE, CanvasToppingLayer, CanvasMainViewModel, CanvasMainUiState, CanvasMainRoute, CanvasMainScreen, NavKeyCanvasMain, GroupListViewModel, GroupListScreen, GroupListRoute, String.toColorOrNull, CanvasVO, CanvasToppingVO, ToppingTransform, ToppingBorder, YGCanvasBackground
related_adr: ADR-0009, ADR-0017, ADR-0020
related_spec: c001-canvas-main, c201-canvas-calendar, c201-canvas-calendar-server, c301-topping-edit-tab, parfait-canvas-topping-member-api-service-layer, canvas-detail-background-api-service-layer, g001-group-list, screen-resume-refetch
related_architecture: data-layer, navigation-flow, module-structure, design-system
supersedes:
superseded_by:
tags: [spec, parfait, canvas, c001, api-consumer]
---

# Spec: C-001 캔버스 오늘·날짜별 조회 결선

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> 📌 **심볼 리네임(2026-08-17, #278)** — 아래 본문의 `CanvasImageAdd*`는 **당시 이름**이다. 현재 코드는 **`CanvasMain*`**(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`, `strings.xml` 키 `canvas_main_*`). 이름만 바뀌고 시그니처·동작은 불변이라 본문은 기록대로 둔다.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #268(`feature/canvas-today-parfait-detail`)이
> develop에 머지됐다(2026-08-17). 화면 맥락은 [c001-canvas-main 스펙](2026-08-12-c001-canvas-main.md),
> 달력 맥락은 [c201 스펙](2026-08-16-c201-canvas-calendar.md).

## 목표

캔버스 메인이 **서버가 가진 캔버스**를 그린다 — 오늘 것으로 열고, 달력에서 고른 날의 것으로
갈아 끼운다. 그리고 G-001에서 그 화면으로 **들어갈 수 있게** 한다.

## 범위

- **포함**
  - `ParfaitRepository`(domain) + `ParfaitRepositoryImpl`(data) 신설 — 이 도메인 첫 Repository.
    RemoteDataSource가 가진 다섯 갈래 중 **셋만**(오늘·목록·상세) 인터페이스에 올린다.
  - UseCase 둘 — `GetTodayParfaitUseCase`(오늘, 자정 경계 재시도 1회) ·
    `GetCanvasByDateUseCase`(고른 날, 목록→상세 2단, 없으면 `null`).
  - `PARFAIT_TIME_ZONE`·`parfaitToday()`(`domain/model/ParfaitDay.kt`) — 오늘을 **서버 시간대(KST)** 로 센다.
  - `CanvasToppingLayer`(feature 로컬 `component/`) — 저장된 배치대로 토핑을 얹고 누끼 실루엣 테두리를 그린다.
  - `NavKeyCanvasImageAdd`가 `data object` → **`data class(groupId: Long)`**, ViewModel은 `@AssistedInject`.
  - G-001 토핑 클릭 결선 — `ClickTopping(groupId)` → `NavigateToCanvas(groupId)` →
    `goTo(NavKeyCanvasImageAdd(groupId))`. 클릭은 카드 `modifier`에 `clickableYGScaleRipple`로 붙는다.
  - `String.toColorOrNull()`을 `core:util:android` `extension/`으로 신설(화면 전용이 아니라 공용).
  - 화면 실데이터 3종 — 멤버 칩·배경·토핑. `isEmpty`가 상수에서 토핑 목록 파생으로 바뀐다.
- **제외**(이번 라운드에서 안 함)
  - **토핑을 새로 얹는 경로** — 배치 확정(POST)·좌표 수정은 그대로 없다. 이 라운드는 **읽기 전용**이다.
  - 캘린더의 `GetParfaitHistoriesUseCase`·`GetParfaitYearsUseCase` — **여전히 mock**이다
    (표면을 우회하는 소비자 그대로) → [open-questions](../../../synthesis/open-questions.md) OQ-P-183 ①.
  - 그룹명 — 캔버스 응답에 없어 `loadCanvasImageAddInfo()`의 TODO 하드코딩이 남았다.
  - 배경 변경(PATCH) 소비 — C-301이 고른 배경은 여전히 서버로 가지 않는다.
  - 조회 실패의 사용자 표현 — 오늘·날짜별 둘 다 로그만 남긴다.

## 동작 / 구조

### 층

```
CanvasImageAddViewModel ─┬─ GetTodayParfaitUseCase ──┐
                         └─ GetCanvasByDateUseCase ──┴─ ParfaitRepository ─ ParfaitRepositoryImpl
                                                                            └ ParfaitRemoteDataSource
```

`ParfaitRepositoryImpl`이 하는 일은 위임 + `mapErrorToAppError()`뿐이다(다른 원격 Repository와 같다).
**부분 노출**은 `ParfaitGroupRepository`의 방침 그대로다 — 연도 조회·배경 변경은 소비자가 생길 때 올린다.
DI는 `RepositoryModule`에 `@Binds` 한 줄.

### 오늘 조회 — 부작용 있는 GET을 한 번만 부른다

`GET /parfaits/today`는 **조회인데 그날 캔버스가 없으면 만들어 저장한다**([api/parfait.md](../../../api/parfait.md)).
그래서 진입 시 **한 번만** 부르고 `launch(key = LOAD_TODAY_CANVAS_KEY)`로 중복 호출을 막는다.

> 🔁 **뒤집힘(2026-08-17, PR #297)** — 이제 **화면이 앞에 설 때마다** 부른다(`Enter` 인텐트 +
> `LifecycleResumeEffect`). 근거는 캔버스가 다른 멤버의 토핑으로도 바뀐다는 것이고, 재진입 호출은
> 첫 진입에서 이미 만들어진 것을 받을 뿐이라 캔버스가 늘지 않는다. 대신 `init`에서는 더 이상 부르지
> 않아 **ViewModel이 만들어지는 것만으로는 캔버스가 생기지 않는다.** `LOAD_TODAY_CANVAS_KEY`는 동시
> 중복 호출만 막는 역할로 남았고, 지난 날을 보는 중이면 재진입해도 부르지 않는다
> → [screen-resume-refetch 스펙](2026-08-17-screen-resume-refetch.md).
>
> 📌 **첫 조회에만 화면을 덮는다(2026-08-30, PR #407 develop 머지)** — 그때까지 이 조회는 도는 동안
> 아무 표시가 없어 빈 캔버스를 보게 됐다. `CanvasMainUiState.isInitialLoading`이 생겨
> `YGScaffoldV2(isLoading = …)`로 넘어간다. 켜는 조건은 **`todayCanvas == null`**, 즉 아직 한 번도
> 받지 못한 조회다 — 바로 위 재정정대로 재진입마다 조회가 나가므로 조건이 없으면 다른 화면에서
> 돌아올 때마다 이미 그려진 캔버스 위로 덮개가 번쩍인다. 켜고 내리는 것을 둘 다 `launch` 블록
> **안**에서 하는 이유는 `LOAD_TODAY_CANVAS_KEY` 가드에 막히면 블록이 아예 안 돌아 밖에서 켠 것을
> 내려 줄 `finally`가 따라오지 않기 때문이다. 연도 목록·달력 기록 조회는 실패해도 노출하지 않는
> 값이라 덮개 대상이 아니다. 판정이 그룹 목록 쪽과 따로 적혀 있는 것은 OQ-P-330.

그럼에도 `today`를 쓰는 이유는 **토핑을 올리려면 `parfaitId`가 있어야 하기 때문**이다 — 목록·상세는
부작용이 없지만 없는 날을 만들어 주지 않는다. 초기 구현은 목록→상세로 우회했다가 이 이유로 되돌렸다.

**자정 경계 재시도 1회** — 요청이 도는 사이 날이 바뀌면 어제 캔버스가 온다. 그래서 오늘을
**응답을 받은 뒤에** 읽어 비교하고, 어긋나면 딱 한 번 다시 부른다. 두 번째도 어긋나면 기기와 서버의
시계가 어긋난 것이라 더 불러도 같은 답이 온다.

> 📌 **자정 처리 자리가 하나 늘었다(2026-08-17, PR #297)** — 위 재시도는 **요청 중** 날이 바뀐 경우고,
> 화면을 열어 둔 채 날이 바뀐 경우는 재진입 때 `syncToday()`가 `UiState.today`를 다시 센다(오늘을
> 보고 있었으면 두 캔버스를 비우고 새 날로 옮긴다). 서로 다른 상황을 덮지만 기준 시각은 둘 다
> KST 자정이라 03:00 경계 미적용은 그대로다.

### 오늘은 서버 시간대로 센다

캔버스 행은 **KST 날짜를 키로** 저장되고 오늘 조회도 서버가 그 날짜로 찾는다. 기기 시간대로 오늘을
세면 해외 기기에서 **매번** 날짜가 어긋나 위 재시도가 하루 한 번이 아니라 로드마다 돌고, 달력은
지금 보고 있는 날을 미래로 보고 잠근다. `PARFAIT_TIME_ZONE`(`Asia/Seoul`) + `parfaitToday()`가
그 계산의 단일 자리다.

⚠️ **경계 시각은 여전히 00:00이다.** 위키 [[캔버스-마감-스케줄]]의 03:00 경계는 적용되지 않았다
(`domain`의 `DayWindow`는 C-102 갤러리만 쓴다) → [open-questions](../../../synthesis/open-questions.md) OQ-P-127.

### 날짜별 조회 — 훑는 것만으로 캔버스를 만들지 않는다

> 📌 **이 UseCase는 하루 뒤 삭제됐다(2026-08-17, PR #279)** — 달력이 mock을 버리고 그 해 목록을
> 캐시로 들게 되면서 목록 조회를 화면이 이미 갖게 됐고, `GetCanvasByDateUseCase`는 캐시에서
> `parfaitId`를 꺼내 상세만 부르는 `GetParfaitDetailUseCase`로 대체됐다. 아래 2단 설계와 셋으로
> 갈리는 결과는 그 시점까지의 기록이다 —
> [c201-canvas-calendar-server 스펙](2026-08-17-c201-canvas-calendar-server.md).

`GetCanvasByDateUseCase`는 **목록(하루 범위) → 상세** 2단이다. 오늘을 골라도 `today`로 가지 않는다 —
달력을 훑는 것만으로 빈 캔버스가 쌓이면 안 되기 때문이다. 셋으로 갈린다.

| 상황 | 결과 |
|---|---|
| 그날 파르페 없음 | `Result.success(null)` — 실패가 아니다 |
| 목록 실패 | 그대로 실패 |
| 상세 실패 | 그대로 실패 — `null`로 접으면 "없음"과 구분되지 않는다 |

범위를 하루로 좁혀 부르지만 **응답 날짜를 다시 본다** — 경계 처리가 서버 몫이라 하루가 더 딸려 오면
옆날 캔버스를 고른 날로 착각한다(테스트로 잠갔다).

### 날짜 선택의 결과

`ClickDate`는 이제 셋을 한다 — 달력을 닫고, **이전 날 그림을 즉시 비우고**(머리말은 새 날짜인데
그림이 이전 날인 상태가 눈에 보이는 것보다 잠깐 비는 편이 덜 틀리다), 그날 캔버스를 불러 채운다.
같은 날을 다시 고르면 닫기만 한다.

**응답 경합은 중복 가드가 아니라 날짜 확인으로 막는다** — `launch`의 key 가드는 앞선 조회를 살리고
새 것을 버리는데, 날짜 선택은 **마지막에 고른 것이 이겨야** 한다. 그래서 가드를 걸지 않고, 응답을
반영하기 전에 `selectedDate`가 아직 그 날인지 확인한다.

상단 날짜 라벨(`canvasDate`·`canvasDay`)도 `selectedDate` 파생으로 바뀌어 고른 날을 따라간다.

### 토핑 렌더 (`CanvasToppingLayer`)

서버가 좌표의 **단위를 말하지 않아** 앱이 정했다.

| 값 | 앱이 정한 뜻 | 근거 |
|---|---|---|
| `positionX`·`positionY` | Canvas-Area 대비 **0~1 정규화 중심점** | 절대 px면 기기마다 캔버스 폭이 달라 같은 배치가 다른 자리에 뜬다 |
| `scale` = 1.0 | 긴 변이 **Canvas-Area 너비의 40%** | 위키 [[C-106-토핑-배치-정책-v0.1]]의 초기 크기 규칙 |
| `borderWidth` = 1.0 | **화면 기준 1dp**(토핑을 키워도 굵기 불변) | 계약에 단위 없음 — PR 리뷰에서 확정 |
| `borderColor` | `#RRGGBB` 6자리(8자리도 읽는다) | 계약 타입이 String이라 형식 변경이 컴파일로 안 드러난다 |

- 정사각 박스 + `ContentScale.Fit`이라 **원본 크기를 몰라도** 긴 변이 40%가 되고 짧은 변이 비율대로 준다.
  남는 여백은 투명이라 배치 중심이 흔들리지 않는다.
- 캔버스 밖으로 나간 배치는 **되돌리지 않는다** — Canvas-Area의 clip이 잘라 낸다(위키 이탈 허용 규칙).
- `positionZ` 오름차순으로 그린다(ViewModel이 정렬해 State에 담고, 컴포넌트는 받은 순서대로 얹는다).
- 레이어 크기는 **호출자가 준다**(`fillMaxSize` 전제 아님) — 배치 계산이 받은 상자에 대한 비율이라
  상자를 컴포넌트가 정하면 안 된다.

**테두리는 누끼 실루엣을 따라간다.** 사각 테두리를 두르면 잘라 낸 배경이 다시 드러나므로, 같은 그림을
테두리 색으로 물들여 **여덟 방향으로 밀어 찍고** 그 위에 원본을 얹는다. 그래서 `SOLID` 토핑 하나가
**이미지 9장**을 그린다. 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보이므로
`AsyncImagePainter.State.Success`일 때만 찍는다.

**색을 못 읽으면 각각 다르게 떨어진다** — 테두리는 **안 그리고**, 배경은 **기본 배경**으로 간다.
둘 다 "임의의 색을 골라 칠하는 것보다 덜 틀리다"는 같은 근거다.

> 🔁 **as-built 정정(2026-08-25, PR #351)** — 배경 쪽 폴백의 **자리와 값이 둘 다 바뀌었다.**
> 화면이 들고 있던 `DEFAULT_CANVAS_BACKGROUND`(`Solid(Gray100)`)가 사라지고, `toYGCanvasBackground()`가
> 미설정·미지 type·색 파싱 실패 셋을 전부 **`null`로** 넘긴다. 폴백 그림을 그리는 주체는 이제
> `YGCanvas`이고 그림도 `Gray100`이 아니라 **흰 바탕**이다 — 이 스펙이 "기본 배경"이라 적은 것은
> 그때의 `Solid(Gray100)`이었다. 근거 문장("덜 틀리다")은 그대로 성립한다.
> 함께 **`isEmpty`의 효력이 배경에 종속됐다** — 빈 안내판은 `isEmpty && background == null`일 때만
> 뜬다. 배경을 고른 캔버스는 토핑이 0개여도 안내 없이 그 배경만 보인다. 그 조건은 정책 소스가
> 없다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-304.

> 🔁 **as-built 갱신(2026-09-04, PR #440 develop 머지) — 다 모이기 전에는 아무것도 안 낸다.**
> 종전에는 토핑이 각자 도착하는 대로 하나씩 떴다. 이제 `CanvasToppingLayer`가 `core:ui`의
> `rememberBatchRevealState`로 **한 묶음이 전부 결말날 때까지 가렸다가 한 번에 낸다**(성공과 실패를
> 모두 결말로 센다). 가리는 수단은 `Modifier.revealed`(알파 0 + 시맨틱 제거)라 **측정·배치는 살아
> 있고**, 그래야 감춘 자리의 이미지 요청이 이어진다. 판정 오버레이는 드러난 뒤에만 단다 —
> 보이지 않는 토핑이 눌리면 안 된다.
> - **빈 목록은 완료가 아니다.** 조회가 오기 전에는 기다릴 대상이 없는데 그 순간을 완료로 세면
>   빗장이 먼저 풀려 뒤늦게 온 것들이 하나씩 뜬다. 한 번 풀린 뒤에는 다시 가리지 않는다(항목 하나가
>   늘 때마다 화면 전체가 사라졌다 나타나면 더 거슬린다).
> - **날짜를 바꿔도 이 레이어는 컴포지션에 남으므로** 빗장을 다시 걸 열쇠가 필요하다 —
>   `revealResetKey`는 **고른 날짜가 아니라 실제로 그리는 캔버스**(`displayedCanvas`)다. 지난 날을
>   기다리는 동안에는 직전 캔버스가 그대로 걸려 있기 때문이다.
> - **배치 화면(C-106)은 이 게이트를 끈다**(`revealTogether = false`) — 거기서는 이 레이어가 배경으로만
>   깔리므로, 켜면 배치 중인 토핑 옆에서 로딩이 돈다.
> - 화면 전체의 결말은 `CanvasLoadState`(`Loading`/`Loaded`/`Failed`)로 접힌다 — **한 장만 실패해도
>   전체가 실패**이고 실패가 있으면 나머지를 기다리지 않는다. 빈 목록은 실패가 아니다. 접기는 두 겹이다
>   (토핑들 → 하나, 그 결과 + 배경 → 하나). ⚠️ 이 규칙의 근거는 코드 주석("기획 판단")과 커밋
>   메시지뿐이다 → OQ-P-355.
> - 실패하면 `YGScaffoldV2`의 `loadingOverlay` 슬롯에 **다시 시도 버튼이 있는 덮개**가 들어가고,
>   버튼은 `retryKey`를 올린다 — `rememberReloadableImageRequest`가 그 값으로 캐시를 건너뛰므로 같은
>   url이라도 실제로 다시 받아 온다. ⚠️ **알파 마스크는 그 재시도에 딸려 오지 않는다** → OQ-P-356.
> - 덮개는 화면 전체를 덮는다 — 캔버스 영역만 덮으면 그 밖의 날짜 선택과 메뉴가 그대로 눌린다.

### 진입

`NavKeyCanvasImageAdd`가 인자를 얻으면서 C-001의 **도달 불가가 닫혔다**. 인자가 `groupId`인 이유는
캔버스가 그룹 하나에 매여 있어서다 — 첫 그룹으로 고정하면 두 번째 그룹의 캔버스에 들어갈 방법이 없다.
그래서 인텐트·사이드이펙트도 `GroupId`를 싣는다(`GroupListViewModelTest`가 "누른 그룹이 실려 간다"를 잠근다).

`YGToppingGroup`은 `onClick`을 갖지 않으므로(디자인시스템 규약) 호출부가 `modifier`로 클릭을 붙인다 —
`clickableYGScaleRipple`이라 **그룹 목록 토핑에 첫 클릭 경로**가 생겼다.

### 멤버 칩

mock 7명을 버리고 캔버스 응답의 `groupMembers`를 쓴다. **서버가 칩 색을 주지 않아** 목록 순서로
팔레트(7종)를 돌려 쓴다 — 순서가 고정이라 같은 그룹을 다시 열면 같은 사람에게 같은 색이 간다.
칩 글자는 닉네임 **첫 글자**다(`take(1)`).

⚠️ 위키 [[nametag-chip]]의 "타입은 유저별 고정" 규칙은 여전히 미구현이다 — S-101 그룹원 목록이
같은 형태의 인덱스 순환 mock을 쓰는 것과 같은 자리다.

## 드리프트 / 잔존

1. **좌표·크기·테두리 단위가 전부 앱이 정한 것이다** — 서버 계약(`api/parfait.md`)은 `positionX`가
   무엇에 대한 비율인지, `scale` 1.0이 얼마인지, `borderWidth`의 단위가 무엇인지 적지 않는다.
   iOS가 같은 캔버스를 다르게 그려도 계약으로는 잡히지 않는다
   → [open-questions](../../../synthesis/open-questions.md).
2. **읽기만 있고 쓰기가 없다** — 40% 규칙이 **서버에서 받은 토핑에만** 적용된다. 새로 얹는 경로가
   없어 초기 위치(정중앙)·최소 터치 방어(48px)는 여전히 코드에 없다(OQ-P-200).
   같은 화면의 C-301 편집 탭은 아직 `loadMockToppings()`를 고친다(OQ-P-199) — **한 앱 안에서
   캔버스 토핑의 출처가 둘**이다.
3. **조회 실패가 사용자에게 안 보인다** — 오늘·날짜별 둘 다 로그만 남긴다. 근거는 "배경과 토핑이
   안 그려질 뿐 토핑을 올리는 것은 그대로 할 수 있다"인데, 그 "올리는 것"이 아직 없어 실제로는
   **빈 캔버스와 조회 실패가 구분되지 않는다**. 같은 라운드에 `YGScaffoldV2`가 공통 실패 표현을
   열었는데(#267) 이 화면은 이관 대상 3화면에 없다(OQ-P-167·OQ-P-204).
4. **그룹명이 mock이다** — 캔버스 응답에 그룹명이 없어 `loadCanvasImageAddInfo()`가 문자열을 그대로
   들고 있다. 실데이터가 붙은 멤버 칩과 한 상단 바 안에서 갈린다.
5. **누끼 테두리가 이미지 9장이다** — 토핑 수 × 9의 `AsyncImagePainter` 그리기가 매 프레임 나간다.
   측정한 값은 없다(실기기 확인 없음).
6. **경계 시각 03:00 미적용** — 시간대만 KST로 맞췄다(위 "오늘은 서버 시간대로 센다").
7. **`selectedDate` 기본값이 오늘이라 "아무것도 안 골랐다"와 구분되지 않는다** — 같은 날 재선택을
   "닫기만 한다"로 처리해 지금은 드러나지 않는다(OQ-P-184 ③).

## 정책 대조

| 위키 정책 | 코드 | 판정 |
|---|---|---|
| C-106 초기 크기 = 긴 쪽이 캔버스 가로 40% | `TOPPING_BASE_LONG_SIDE_RATIO` + `ContentScale.Fit` | 일치(단, **렌더에만** — 새 배치 경로 없음) |
| C-106 초기 위치 = 정중앙 | 해당 코드 없음 | 대조 대상 부재(드리프트 2) |
| C-106 최소 터치 방어 짧은 쪽 48px | 해당 코드 없음 | 대조 대상 부재(드리프트 2) |
| C-106 Off-canvas 이탈 허용 + Clipping Mask | 되돌림 없음 + Canvas-Area clip | 일치 |
| 내부 요소는 캔버스 크기에 비례 스케일([[캔버스-반응형-레이아웃]]) | 위치·크기 전부 Canvas-Area 비율 | 일치(직전 라운드까지 "대조 대상 없음"이던 자리) |
| 하루 경계 03:00 KST([[캔버스-마감-스케줄]]) | `parfaitToday()` = KST 00:00 | **부분 일치** — 시간대만 맞음(드리프트 6) |
| [[nametag-chip]] 타입은 유저별 고정 | 목록 인덱스 순환 | **불일치**(mock) |
| 토핑 테두리 표현 | 정책 소스 없음 | 대조 대상 부재 — 코드가 먼저 확정(8방향 스탬프) |

## 테스트

유닛 417 → **434건**. 신규 3파일 16건 + `GroupListViewModelTest` 1건.

| 파일 | 잠근 것 |
|---|---|
| `GetTodayParfaitUseCaseTest` | 재시도가 **정확히 1회**(두 번째도 어제면 그것을 쓴다), 첫 실패는 재시도 없이 전파, **재시도 실패를 어제 캔버스로 눙치지 않는다** |
| `GetCanvasByDateUseCaseTest` | 없는 날 = `success(null)`(상세 호출 안 함), 옆날만 섞여 오면 `null`, 범위를 그날 하루로 좁힘, 목록·상세 실패 각각 전파 |
| `StringTest` | `#` 유무·6자리·8자리·길이 이상·비16진 |
| `GroupListViewModelTest` | 두 번째 그룹을 눌렀을 때 **그 그룹**이 이펙트에 실린다 |

⚠️ **실기기 확인 없음.** 실서버 요청 검증도 그대로 0건이다.

## 파일 구성

```
domain/
  model/ParfaitDay.kt                              신설 — PARFAIT_TIME_ZONE·parfaitToday()
  repository/parfait/ParfaitRepository.kt          신설 — 오늘·목록·상세 셋만
  usecase/parfait/GetTodayParfaitUseCase.kt        신설
  usecase/parfait/GetCanvasByDateUseCase.kt        신설
data/
  repository/parfait/ParfaitRepositoryImpl.kt      신설 — 위임 + mapErrorToAppError
  di/RepositoryModule.kt                           @Binds 1줄
core/util/android/
  extension/String.kt                              신설 — toColorOrNull()
feature/groups/canvas/
  api/NavKeyCanvasImageAdd.kt                      data object → data class(groupId)
  impl/component/CanvasToppingLayer.kt             신설 — 배치·크기·누끼 테두리
  impl/viewmodel/CanvasImageAddViewModel.kt        캔버스 조회 2종·멤버 칩·날짜 전환
  impl/screen/CanvasImageAddScreen.kt              배경 매핑·토핑 슬롯·isEmpty 파생
  impl/route/CanvasImageAddRoute.kt                Assisted 생성
  impl/navigation/EntryBuilder.kt                  navKey.groupId 전달
feature/groups/list/impl/
  route/GroupListViewModel.kt                      ClickTopping·NavigateToCanvas에 GroupId
  route/GroupListScreen.kt                         카드 modifier에 clickableYGScaleRipple
  route/GroupListRoute.kt                          goTo(NavKeyCanvasImageAdd(groupId))
  build.gradle.kts                                 canvas:api 의존 추가
```
