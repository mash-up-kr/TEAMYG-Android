---
id: c201-canvas-calendar-server
title: C-201 캘린더 서버 결선 (mock 제거 + 연도별 캐시 + 지난 캔버스 열람)
status: implemented
category: feature-spec
platforms: android
verified: 2026-09-01
related_code: GetParfaitHistoriesUseCase, GetParfaitYearsUseCase, GetParfaitDetailUseCase, ParfaitRepository, ParfaitRepositoryImpl, PastCanvasVO, CanvasMainViewModel, CanvasMainUiState, CanvasMainIntent, CanvasMainRoute, CanvasMainScreen, CustomCalendar, YGCanvasMenuAction, parfaitToday
related_adr: ADR-0009, ADR-0017, ADR-0020
related_spec: c201-canvas-calendar, c001-canvas-today-detail, c001-canvas-main, parfait-canvas-topping-member-api-service-layer, screen-resume-refetch, c001-canvas-gallery-save
related_architecture: data-layer, state-management, design-system
supersedes:
superseded_by:
tags: [spec, parfait, canvas, calendar, c201, api-consumer]
---

# Spec: C-201 캘린더 서버 결선

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> 📌 **심볼 리네임(2026-08-17, #278)** — 아래 본문의 `CanvasImageAdd*`는 **당시 이름**이다. 현재 코드는 **`CanvasMain*`**(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`, `strings.xml` 키 `canvas_main_*`). 이름만 바뀌고 시그니처·동작은 불변이라 본문은 기록대로 둔다.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #279(`feature/canvas-calendar-api`)가 develop에
> 머지됐다(2026-08-17). 캘린더가 처음 들어온 라운드의 기록은
> [c201-canvas-calendar 스펙](2026-08-16-c201-canvas-calendar.md)이고, 이 문서는 그때 남긴
> "둘 다 mock"이 걷힌 라운드를 역기록한다. 화면 전체 맥락은
> [c001-canvas-today-detail 스펙](2026-08-17-c001-canvas-today-detail.md).

## 목표

달력이 서버 기록으로 그려지고, 고른 지난 날의 캔버스가 실제로 열린다. 하루 만에 두 번 지적됐던
"같은 ViewModel 안에서 캔버스 조회는 계약을 타고 달력 조회는 mock을 만든다"를 닫는다(OQ-P-183).

## 범위

- **포함**
  - `ParfaitRepository.getYears(groupId)` — 다섯 갈래 중 **넷째**가 열렸다(`ParfaitRepositoryImpl`은
    DataSource 위임 + `mapErrorToAppError` 한 줄).
  - `GetParfaitHistoriesUseCase(groupId, year)` — mock 생성 로직 전부 삭제, `getPastCanvases`에
    **연 범위**(1월 1일 ~ 12월 31일)를 넘기고 날짜 내림차순으로 정렬해 돌려준다.
  - `GetParfaitYearsUseCase(groupId)` — mock 삭제, `getYears` 위임 + 올해 채워 넣기(`withYear`)만 남았다.
  - `GetParfaitDetailUseCase(groupId, parfaitId)` 신규 — `GetCanvasByDateUseCase` **삭제**를 대신한다.
  - `ParfaitHistory` **삭제** — 달력이 `PastCanvasVO`(계약 응답 VO)를 그대로 쓴다. `isEmpty` 파생은
    그쪽으로 옮겼고 기준 필드는 `imageCount` → `toppingCount`.
  - UiState 재편 — `todayCanvas`/`viewedCanvas` 두 갈래 + `parfaitHistoriesByYear` 연도별 캐시.
  - 지난 캔버스의 메뉴 액션 2종 교체 — 토핑 추가·캔버스 편집 → **갤러리에 저장**·**오늘의 파르페 가기**.
  - 달력 셀 활성 조건 변경 — `isCurrentMonth && (오늘 || 기록 있는 날)`.
- **제외**(이번 라운드에서 안 함)
  - **갤러리 저장의 실제 동작** — 버튼과 인텐트만 있고 핸들러는 로그 한 줄이다(드리프트 1).
    ✅ PR #324가 채웠다 → [c001-canvas-gallery-save 스펙](2026-08-23-c001-canvas-gallery-save.md).
  - 배경 변경(`PATCH .../background`) — 다섯 갈래 중 마지막 하나는 여전히 Repository에 없다.
  - 조회 실패의 사용자 표현 — 셋 다 로그만 남기는 것이 그대로다.

## 동작 / 구조

### 오늘과 보고 있는 날을 상태에서 가른다

`canvasBackground`·`toppings`를 직접 들던 자리가 **캔버스 두 개**로 바뀌었다.

| 필드 | 뜻 | 쓰임 |
|---|---|---|
| `todayCanvas` | `/parfaits/today`로 받아 둔 오늘 캔버스(#297부터 화면이 앞에 설 때마다 갱신) | 오늘로 돌아올 때 **재조회 없이** 되돌리는 원본 |
| `viewedCanvas` | 지금 화면에 그려지는 캔버스 | `canvasBackground`·`toppings`·`isCanvasEmpty` 파생의 유일한 출처 |

가른 이유는 **토핑 추가·배경 편집이 언제나 오늘 것을 대상으로 해야 하는데** 서버가 마감된 캔버스의
편집을 막지 않기 때문이다(OQ-P-189). 하나로 두면 지난 날을 보다가 그 캔버스를 고치게 된다.

부수 효과로 **오늘로 돌아가는 길에 조회가 없다** — `/parfaits/today`는 없는 날을 만들어 저장하므로
다시 부르는 것이 안전하지 않고, 받아 둔 `todayCanvas`로 갈아 끼우는 것이 유일한 경로다.

응답 경합은 두 자리에서 같은 방식으로 막는다 — 오늘 조회는 반영 직전 `isViewingToday`를, 상세 조회는
`selectedDate == date`를 다시 본다. 늦게 온 응답이 그 사이 옮겨 간 화면을 덮지 않는다.

### 날짜 선택이 목록→상세 2단에서 캐시→상세로 바뀌었다

`GetCanvasByDateUseCase`는 고른 날짜로 **목록을 한 번 더 불러** `parfaitId`를 찾고 상세를 받았다.
지금은 달력이 이미 그 해 목록을 들고 있으므로 **캐시에서 `parfaitId`를 꺼내** 상세만 부른다.
날짜로 캔버스를 찾는 엔드포인트가 없다는 사실은 그대로이고, 그 조회를 화면이 대신한다.

목록에 없는 날은 **아무 일도 하지 않는다** — 달력이 기록 있는 날만 열어 주므로 여기까지 올 수 없고,
와도 빈 캔버스를 보여 주는 것보다 그냥 두는 편이 낫다는 판단이다.

새 캔버스가 오기 전에 이전 날 그림을 **비우지 않는다**(직전 라운드는 비웠다). 근거가 뒤집혔다 —
달력이 기록 있는 날만 열어 주게 되면서 "잠깐 비어 보이는 것"이 항상 거짓말이 됐기 때문이다.
그 대신 다른 문제가 생겼다(드리프트 2).

### 연도별 캐시

`parfaitHistoriesByYear: Map<Int, List<PastCanvasVO>>`. 한 번 받은 해는 다시 부르지 않는다 —
달력은 연·월을 오가며 같은 해로 몇 번이고 돌아온다.

리스트 하나에 이어 붙이지 않은 이유는 **"받아 봤는데 비어 있는 해"와 "아직 안 받은 해"를 구분**해야
해서다. 그 둘이 같아지면 빈 해를 볼 때마다 서버를 다시 부른다. 연 선택도 캐시를 먼저 보고,
있으면 서버를 거치지 않고 `movedToNearestMonth(year)`로 바로 옮긴다.

한 해치를 한 번에 받는 근거는 계약이다 — 목록 API에 페이지네이션도 범위 상한도 없어 최대 366건이
한 응답으로 온다(`api/parfait.md` 미결).

### 지난 캔버스의 메뉴가 다른 두 가지를 준다

`YGCanvasMenuAction` 두 슬롯이 `isViewingToday`로 갈린다.

| 보고 있는 날 | 위 액션 | 아래 액션 |
|---|---|---|
| 오늘 | 토핑 추가(`ic_plus`) | 캔버스 편집(`ic_caret_right`) |
| 지난 날 | ~~갤러리에 저장(`ic_gallery`)~~ → **없음(`null`)** | 오늘의 파르페 가기(`ic_caret_right`) |

> 🔁 **지난 날의 위 액션이 비었다(2026-09-01, PR #413·#414)** — 갤러리 저장이 하단 메뉴에서
> **날짜바 오른쪽 아이콘**(`ic_save`)으로 옮겨 가면서 이 자리에 넣을 것이 없어졌다. `addAction`이
> `null`이면 `YGCanvasMenu`가 "오늘의 파르페 가기" 하나를 전폭으로 그린다. 저장은 이제 날짜로 갈리지
> 않고 **캔버스에 저장할 내용이 있는지**로 갈린다(`isCanvasSaveVisible`) → [c001-canvas-gallery-save
> 스펙](2026-08-23-c001-canvas-gallery-save.md).

**지난 캔버스를 고치지 못하게 하는 방법이 "길을 치우는 것"**이다 — 서버가 마감 캔버스의 편집을 막지
않으므로 화면이 진입점을 없앤다. OQ-P-189 ②("서버가 막을지 앱이 막을지")에 앱 쪽 첫 답이 나왔다.

"오늘의 파르페 가기"는 `selectedDate`·`displayedMonth`·`viewedCanvas`를 함께 되돌리고, 올해 기록이
아직 캐시에 없으면 그때 한 번 받는다.

### 달력이 기록 없는 날을 잠근다

`isEnabled = day.isCurrentMonth && (day.date == today || day.date in uploadedDates)`.
직전 라운드의 `day.date <= today`가 바뀐 것이다. 이유는 선택의 출력이 생겼기 때문이다 — 누를 수는
있는데 열 캔버스가 없는 날이 남으면 그 탭은 아무 일도 하지 않는다.

오늘만 예외다. 오늘은 기록이 없어도 `todayCanvas`로 돌아올 수 있어야 한다(`selectableMonths`가
이번 달을 항상 넣는 것과 같은 이유).

## 데이터

| 심볼 | 하는 일 | 상태 |
|---|---|---|
| `ParfaitRepository#getYears` | 파르페가 있는 연도 | 신규 — DataSource 위임 + `mapErrorToAppError` |
| `GetParfaitYearsUseCase(groupId)` | 연도 목록 + 올해 채우기 | **서버** |
| `GetParfaitHistoriesUseCase(groupId, year)` | 그 해 캔버스 목록, 날짜 내림차순 | **서버** — 연 범위 계산·정렬이 UseCase 몫 |
| `GetParfaitDetailUseCase(groupId, parfaitId)` | 특정 캔버스 상세 | **서버** — 순수 위임 |
| `PastCanvasVO.isEmpty` | `toppingCount == 0` | `ParfaitHistory`에서 이관 |

정렬을 UseCase가 하는 근거는 **계약이 순서를 약속하지 않는다**는 것이다. 연 범위는 다음 해 1월 1일에서
하루를 빼 얻어 윤년·말일을 따로 다루지 않는다(직전 라운드의 mock 말일 계산과 같은 수법이 남았다).

## 테스트

유닛 434 → **436건**. `GetCanvasByDateUseCaseTest` 6건이 삭제되고 `GetParfaitHistoriesUseCaseTest`
4건·`GetParfaitYearsUseCaseTest` 4건이 생겼다. 잠근 것은 연 범위 요청(윤년 포함)·내림차순 정렬·
실패 전파·올해 채우기다. **`GetParfaitDetailUseCase`에는 테스트가 없다** — 순수 위임이라 잠글 판단이
없다(매퍼 단독 테스트를 만들지 않는 규약과 같은 근거).

계측 테스트 변경 0건. ⚠️ **실기기·실서버 확인 없음.**

## 드리프트 / 잔존

1. ~~**갤러리에 저장이 아무 일도 하지 않는다**~~ — ✅ **해소(PR #324, 2026-08-23)**. 핸들러가
   `RequestCanvasCapture` 이펙트로 바뀌어 화면이 `GraphicsLayer`로 캡처하고 `MediaStore`에 쓴다.
   설계·잔존은 [c001-canvas-gallery-save 스펙](2026-08-23-c001-canvas-gallery-save.md)이 갖는다
   → [open-questions](../../../synthesis/open-questions.md) OQ-P-211.
   저장되는 그림은 **배경+토핑뿐**이라 이 라운드가 만든 캔버스 프레임(테두리·컷 도형·날짜 라벨)은
   들어가지 않는다.
2. **날짜를 빠르게 두 번 고르면 머리말과 그림이 어긋난 채 남는다** — 상세 조회에 `launch(key)`
   가드가 붙었는데 이 가드는 **앞선 조회를 살리고 새 것을 버린다**. 새 요청이 버려진 뒤 앞선 응답이
   와도 `selectedDate`가 이미 달라 반영되지 않고, 이번 라운드부터 이전 날 그림을 비우지도 않는다.
   결과는 **B 날짜 머리말 + A 날짜 토핑**이 다음 조작까지 유지되는 상태다. 직전 라운드가 가드를
   일부러 걸지 않았던 이유("날짜 선택은 마지막에 고른 것이 이겨야 한다")가 뒤집혔는데 근거는
   코드에 없다 → [open-questions](../../../synthesis/open-questions.md).
3. **연도별 캐시에 무효화 경로가 없다** — 화면이 사는 동안 한 번 받은 해는 다시 받지 않는다. 오늘
   캔버스에 토핑을 얹어도 달력 점이 그대로다. 지금은 얹는 경로 자체가 없어 드러나지 않는다(OQ-P-209).
   > 🔁 **부분 해소(2026-08-17, PR #297)** — **올해만** 재진입(`Enter`)마다 다시 받는다. 다른 멤버가
   > 오늘 토핑을 올리면 오늘 칸 점이 생기는데 캐시가 그것을 스스로 알 수 없고, 바뀔 수 있는 해는
   > 올해뿐이라는 근거다. 지난 해는 여전히 무효화되지 않고, `loadParfaitHistories`의 KDoc은 아직
   > "연 단위로 한 번만 받는다"라고 적혀 있어 **주석과 동작이 어긋난다**
   > → [screen-resume-refetch 스펙](2026-08-17-screen-resume-refetch.md).
4. **`parfaitHistories` 파생이 캐시가 가른 둘을 다시 뭉갠다** — `parfaitHistoriesByYear[year].orEmpty()`가
   "아직 안 받은 해"와 "기록 없는 해"를 똑같이 빈 목록으로 준다. 무효화·재시도가 이 파생을 근거로
   판단하면 캐시를 나눈 이유가 사라진다.
5. **조회 실패가 여전히 안 보인다** — 오늘·연도·목록·상세 넷 다 로그만 남긴다. 달력은 점이 안 찍힐
   뿐이라는 근거가 목록·연도에는 남지만, **상세 실패는 이제 "고른 날이 안 열린다"**여서 성격이 다르다
   (OQ-P-204와 같은 자리).
6. **하루 경계 03:00 미적용이 그대로다** — `parfaitToday()`가 KST 자정으로 센다. 이번 라운드가 그
   값에 **달력 셀 활성 조건**까지 새로 걸었다(OQ-P-127).
7. **그룹명이 mock인 것도 그대로다** — `loadCanvasImageAddInfo()`가 문자열을 그대로 든다.

## 정책 대조

| 위키 정책([[캘린더-컴포넌트]]) | 코드 | 판정 |
|---|---|---|
| Disabled = 캔버스를 볼 수 없는 **미래 날짜** | `isCurrentMonth && (오늘 \|\| 기록 있는 날)` | **확대 심화** — 기록 없는 **과거** 날짜까지 잠근다 |
| Chip-Indicator = 그 날 토핑 1개 이상 | `toppingCount > 0`인 날만 `uploadedDates` | 일치(기준 필드가 계약 값으로 바뀌었다) |
| 하루 경계 03:00 KST([[캔버스-마감-스케줄]]) | `parfaitToday()` = KST 자정 | **불일치**(드리프트 6) |
| 지난 캔버스의 열람·저장 동작 | 갤러리 저장·오늘로 가기 2종 | 정책 소스 없음 — 코드가 먼저 확정 |

## 파일 구성

```
domain/
  repository/parfait/ParfaitRepository.kt            getYears 추가
  usecase/parfait/GetParfaitHistoriesUseCase.kt      mock 제거·Repository 주입
  usecase/parfait/GetParfaitYearsUseCase.kt          mock 제거·Repository 주입
  usecase/parfait/GetParfaitDetailUseCase.kt         신규
  usecase/parfait/GetCanvasByDateUseCase.kt          삭제
  model/canvas/PastCanvasVO.kt                       isEmpty 파생 이관
  model/parfait/ParfaitHistory.kt                    삭제(패키지 소멸)
data/
  repository/parfait/ParfaitRepositoryImpl.kt        getYears 위임
feature/groups/canvas/impl/
  viewmodel/CanvasImageAddViewModel.kt               캔버스 두 갈래·연도 캐시·액션 2종
  screen/CanvasImageAddScreen.kt                     isViewingToday 분기·프리뷰 2케이스
  route/CanvasImageAddRoute.kt                       인텐트 2종 배선
  component/CustomCalendar.kt                        isEnabled 조건
  res/values/strings.xml                             문구 2종
```
