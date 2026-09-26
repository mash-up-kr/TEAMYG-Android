---
id: c201-canvas-calendar
title: C-201 캔버스 캘린더 (연·월 드롭다운 + 날짜 그리드 + 업로드 인디케이터)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-17
related_code: CustomCalendar, CalendarDropdown, CalendarDayUiModel, CanvasMainViewModel, CanvasMainUiState, CanvasMainIntent, GetParfaitHistoriesUseCase, GetParfaitYearsUseCase, GetParfaitDetailUseCase, PastCanvasVO, parfaitToday, YGListDate, YGStrokeButton, YGCanvas, verticalScrollbar, toFirstDayOfMonth, DateTextFormat
related_adr: ADR-0002, ADR-0005, ADR-0009
related_spec: c001-canvas-main, c001-canvas-today-detail, c201-canvas-calendar-server, designsystem-canvas-components, designsystem-bar-listdate-components
related_architecture: design-system, module-structure, state-management
supersedes:
superseded_by:
tags: [spec, parfait, canvas, calendar, c201]
---

# Spec: C-201 캔버스 캘린더

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> 📌 **심볼 리네임(2026-08-17, #278)** — 아래 본문의 `CanvasImageAdd*`는 **당시 이름**이다. 현재 코드는 **`CanvasMain*`**(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`, `strings.xml` 키 `canvas_main_*`). 이름만 바뀌고 시그니처·동작은 불변이라 본문은 기록대로 둔다.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #259(`feature/#207-canvas-calendar`)가 develop에
> 머지됐다(2026-08-16). C-001 캔버스 메인의 날짜 버튼이 처음으로 무언가를 열고, 그 자리를 채우는
> 캘린더가 **화면 로컬 컴포넌트**로 들어왔다. 아래는 머지 코드를 역기록한 것이며 설계 대조가 아니라
> **규약(parfait)·정책(위키 [[캘린더-컴포넌트]]) 대조**로 드리프트를 표기한다.
> 화면 전체 맥락은 [c001-canvas-main 스펙](2026-08-12-c001-canvas-main.md).

## 목표

C-001에서 날짜 버튼을 눌러 그룹의 과거 파르페를 날짜로 훑는다. 파르페가 있는 연·월만 고를 수
있고, 이미지가 실제로 올라간 날에만 점이 찍힌다.

## 범위

- **포함**
  - `CustomCalendar`(`feature/groups/canvas/impl` `component/`) — 머리글(월·연 드롭다운) + 요일 행 +
    날짜 그리드. `YGCanvas`의 `calendarContent` 슬롯을 처음으로 채운다.
  - `CalendarDropdown` — 연/월 선택 팝업(`Popup`), 항목은 `YGStrokeButton` 재사용.
  - `GetParfaitHistoriesUseCase`·`GetParfaitYearsUseCase`(`domain/usecase/parfait/`) — **둘 다 mock**.
  - `ParfaitHistory`(`domain/model/parfait/`) — 목록·달력이 쓰는 최소 정보 + `isEmpty` 파생.
  - UiState 확장 — `today`·`selectedDate`·`isCalendarVisible`·`displayedMonth`·`selectableYears`·
    `parfaitHistories`·`uploadedDates` + 계산 프로퍼티 `selectableMonths`.
  - 공용 유틸 3종 — `Modifier.verticalScrollbar`(`core:util:android`),
    `LocalDate.toFirstDayOfMonth()`·`DateTextFormat.monthFormat`/`fullMonthFormat`(`core:util:jvm`).
  - `YGStrokeButton.borderWidth` 파라미터(`Dp.Hairline`이면 테두리를 안 그린다).
- **제외**(이번 라운드에서 안 함)
  - ~~**서버 연동**~~ — 두 UseCase가 고정 mock을 만들었다(`Todo : 서버 연동 시 groupId를 받아…`).
    #268 이후로도 그대로였고 → **#279에서 걷혔다**(드리프트 1).
  - ~~**날짜 선택의 결과**~~ — 고른 날짜가 캔버스 내용·상단 날짜 라벨을 바꾸지 않는다 → **#268에서 결선됐다**(드리프트 2).
  - C-201을 별도 화면(목적지)으로 만드는 것 — 캔버스 위 오버레이 슬롯이다.

## 동작 / 구조

### 열고 닫기

- 날짜 버튼(`YGCanvasDateSelectButton`)이 `OnClickDateSelect`를 쏘고 ViewModel이
  `isCalendarVisible`을 **토글**한다 — 달력이 열린 동안에도 같은 버튼이 달력 위에 다시 그려지므로
  한 번 더 누르면 닫힌다.
- `YGCanvas`의 Dim이 켜지는 조건은 `isMenuExpanded || isCalendarVisible`이고, `onDimClick`이
  메뉴 닫기와 `DismissCalendar`를 함께 부른다 — 토핑 추가 메뉴와 캘린더가 **같은 Dim을 공유**한다.

### 그리드 구성

- 앞은 이전 달, 뒤는 다음 달 날짜로 채워 **항상 7의 배수**를 만든다. 빈 칸이 아니라 실제 날짜를
  넣어야 칸마다 오늘·선택·업로드 여부를 스스로 판단할 수 있다.
- 요일 머리글은 **일요일 시작**이라 ISO 요일 번호(월=1, 일=7)를 `% 7`로 접어 선두 공백 수를 얻는다.
  머리글 문자열은 화면 `strings.xml`의 `string-array`이고 순서가 곧 배열 의미다.
- 셀은 `YGListDate`(디자인시스템)이고 `isEnabled = 이번 달 && 오늘 이하`, `isUploaded = 업로드 집합
  포함`이다. "Disabled면 인디케이터 항상 False"라는 정책 예외는 컴포넌트가 이미 강제한다.

### 연·월 선택

- 머리글의 월·연 텍스트와 캐럿은 **`interactionSource`를 공유**해 어느 쪽을 눌러도 같이 눌린 색이
  된다. 드롭다운은 `Popup`으로 띄운다 — 같은 레이아웃에 넣으면 열릴 때마다 본문이 아래로 밀린다.
- **연 선택은 재조회를 부른다**(`loadParfaitHistories(year, moveToNearestMonth = true)`).
  월 선택은 상태만 바꾼다 — 한 해치를 한 번에 받아 두기 때문이다.
- 해를 옮길 때 보고 있던 달을 그 해로 그대로 옮긴 자리를 기준으로 삼아 **가장 가까운 선택 가능한
  달**로 붙는다(거리 동률이면 더 최근 달). 그래서 같은 달이 있으면 저절로 유지된다.
- `selectableMonths`는 그 해 기록의 달 + **이번 달**(기록이 없어도)이다. 파르페를 아직 안 만든
  달이어도 오늘로 돌아올 수 있어야 한다. 같은 이유로 `GetParfaitYearsUseCase`가 올해를 채워 넣는다.

### 테두리·스크롤바

- 달력 컨테이너는 `Modifier.border`가 아니라 **좌·우·하단만 직접 그린다**(`drawBehind`). 위쪽은 바로
  위 날짜 버튼이 같은 테두리를 이미 두르고 있어 맞닿는 자리에 선이 두 겹으로 깔린다.
- 드롭다운은 항목마다 테두리를 두르지 않고 **팝업이 한 번만** 그린다(항목은 `borderWidth =
  Dp.Hairline`). 겹쳐 지우는 방식은 겹침이 빠지는 순간 두께가 두 배가 된다.
- Compose에 `ScrollState`용 스크롤바가 없어 `Modifier.verticalScrollbar`를 만들었다.
  `verticalScroll`보다 **앞**에 둬야 스크롤이 반영되지 않은 뷰포트 좌표에 그려 막대가 제자리에 선다.

## 데이터

| 심볼 | 하는 일 | 상태 |
|---|---|---|
| `GetParfaitHistoriesUseCase(year)` | 그 해 파르페를 최신순으로 | ~~**mock**~~ — 고정 지연 + 달마다 정해진 6일, 이미지 수는 `dayOfYear % 9`, `parfaitId`는 epoch day → **#279에서 서버 결선** |
| `GetParfaitYearsUseCase()` | 파르페가 있는 연도 목록 | ~~**mock**~~ — 올해부터 3년 → **#279에서 서버 결선**(올해 채우기만 남았다) |
| `ParfaitHistory` | `parfaitId`·`date`·`thumbnailUrl`·`imageCount` + `isEmpty` | 서버 응답 형태를 따랐다 → **#279에서 삭제**, 계약 VO `PastCanvasVO`로 대체 |

두 UseCase의 KDoc이 대응 서버 엔드포인트를 적는다(`GET /api/v1/groups/{groupId}/parfaits?from=&to=` ·
`.../parfaits/year`). 계약·앱 표면(`ParfaitService`·`ParfaitRemoteDataSource`)은 **이미 있는데**
UseCase가 그것을 쓰지 않고 자기 안에서 mock을 만든다 → [api/parfait.md](../../../api/parfait.md).

> 📌 **막고 있던 것 둘 중 하나는 사라졌다(2026-08-17, PR #268)** — `ParfaitRepository`가 생겼고
> `NavKeyCanvasImageAdd`가 `groupId`를 들고 다니므로, "화면이 그룹 식별자를 안 갖고 있어 UseCase 인자에서
> 뺐다"는 근거는 더 이상 성립하지 않는다. **그런데 이 둘은 여전히 mock이다** — 같은 ViewModel 안에서
> 캔버스 조회는 Repository를 타고 달력 조회는 안 탄다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-183.
>
> ✅ **같은 날 결선됐다(2026-08-17, PR #279)** — 두 UseCase가 `ParfaitRepository`를 주입받고 mock 생성
> 로직이 전부 사라졌다. `getYears`가 Repository에 올라오며 다섯 갈래 중 넷이 열렸고, `ParfaitHistory`는
> 삭제돼 달력이 계약 VO `PastCanvasVO`를 그대로 쓴다. 층이 갈렸던 상태는 닫혔다(OQ-P-183)
> → [c201-canvas-calendar-server 스펙](2026-08-17-c201-canvas-calendar-server.md).

빈 파르페(캔버스를 열어만 보고 이미지를 안 올린 날)는 `isEmpty`로 걸러 `uploadedDates`에서 뺀다 —
안 그러면 화면을 열어 본 날마다 점이 찍힌다.

## 드리프트 / 잔존

1. ~~**mock UseCase가 다시 들어왔다**~~ — 같은 형태(고정 지연 + 성공만 반환)가 2026-08-15 그룹 결선
   라운드에서 전부 걷혔는데(OQ-P-134 해소) 하루 만에 `domain`에 둘이 새로 생겼고, **mock 데이터 생성
   로직이 `domain` UseCase 본문에 있다**는 점에서 더 나아갔다(달력 좌표를 만드는 상수 셋과 말일 계산이
   프로덕션 코드에 살았다). → ✅ **해소(2026-08-17, PR #279)**. 둘 다 Repository를 타고 상수 셋은
   사라졌다(연 범위를 얻는 말일 계산 수법만 남았다) → OQ-P-183 ·
   [c201-canvas-calendar-server 스펙](2026-08-17-c201-canvas-calendar-server.md).
2. ~~**고른 날짜가 아무것도 바꾸지 않는다**~~ → ✅ **해소(2026-08-17, PR #268)**. `ClickDate`가 달력을
   닫고, 이전 날 그림을 즉시 비운 뒤 `GetCanvasByDateUseCase`로 그날 캔버스를 채운다. 상단 날짜 라벨도
   `selectedDate` 파생이라 고른 날을 따라간다. 조회는 **목록→상세 2단**이라 훑는 것만으로 캔버스가
   생기지 않고, 응답 경합은 반영 직전 `selectedDate` 재확인으로 막는다
   → [c001-canvas-today-detail 스펙](2026-08-17-c001-canvas-today-detail.md).
3. **하루 경계가 03시가 아니다** — ~~`today`를 `Clock.System.todayIn(currentSystemDefault())`로 만든다.~~
   → **시간대만 정정(2026-08-17, PR #268)**: `parfaitToday()`가 KST로 센다. **경계는 여전히 00:00**이라
   C-001의 날짜 라벨과 같은 문제이고(OQ-P-127) **미래 날짜 잠금과 오늘 강조까지** 같은 값에 걸린다.
   00:00~02:59에는 캔버스의 실제 날짜가 아직 어제인데 달력은 오늘을 다음 날로 표시하고 그 날을 이미
   선택 가능하게 연다.
4. ~~**`today`가 두 번 계산된다**~~ → ✅ **해소(2026-08-17, PR #268)**. `loadCanvasImageAddInfo()`가
   날짜를 만들지 않게 되어 `today`는 UiState 기본값 `parfaitToday()` 한 자리에서만 나온다.
5. **State가 계산 프로퍼티를 든다** — `selectableMonths`가 UiState 안에 있다.
   [state-management](../../../architecture/state-management.md)는 "표시 규칙에 따른 분기는 화면 private
   헬퍼가 갖는다, State가 계산 프로퍼티로 들 이유가 없다"고 적는다. 다만 이 값은 ViewModel의
   `movedToNearestMonth`도 읽어서 화면 전용이 아니다 → [open-questions](../../../synthesis/open-questions.md).
6. **그리기 확장 소유가 또 갈렸다** — 스크롤바는 `core:util:android` `extension/`,
   달력 컨테이너의 3변 테두리(`Modifier.sideBorder`)는 **feature 파일 안 private**, 점선 테두리·컷
   도형은 `core:designsystem`이다. 같은 층위가 세 곳에 흩어져 있다(design-system "과도기" 절).
7. **드롭다운 치수가 리터럴이다** — 폭·최대 높이·스크롤바 기본값이 `dp` 리터럴이고 토큰 스케일 밖이다.
8. ~~**달력 안 빈 자리를 누르면 달력이 닫혔다**~~ → ✅ **해소(2026-08-20, PR #319)**. 달력은 Dim 위에
   겹쳐 있을 뿐이라 항목 사이 여백을 누르면 뒤의 Dim이 그 탭을 받아 `onDimClick`이 돌았다.
   `YGCanvas`의 달력 `Column`이 `pointerInput`으로 탭을 소비해 막는다 — **막는 자리가 슬롯을 채우는
   화면이 아니라 컴포넌트 쪽**이라, `calendarContent`에 무엇을 넣든 같은 규칙이 적용된다.

## 정책 대조

| 위키 정책([[캘린더-컴포넌트]]) | 코드 | 판정 |
|---|---|---|
| Button-Date 4상태(Default/Selected/Today/Disabled) | `YGListDate(isSelected, isToday, isEnabled)` | 일치 |
| Selected와 Today가 겹치면 Selected | `YGDateButton` 상태 분기 | 일치 |
| Disabled = 캔버스를 볼 수 없는 **미래 날짜** | `isCurrentMonth && date <= today` | **확대** — 앞뒤 달 날짜도 Disabled로 잠근다(정책엔 없는 조건). **#279에서 기록 없는 과거 날짜까지 확대됐다** → [c201-canvas-calendar-server 스펙](2026-08-17-c201-canvas-calendar-server.md) |
| Chip-Indicator = 그 날 토핑 1개 이상 | `imageCount > 0`인 날만 `uploadedDates`(#279부터 `toppingCount`) | 일치 |
| Disabled면 인디케이터 항상 False | `YGListDate` 내부 `isEnabled && isUploaded` | 일치(컴포넌트가 강제) |
| 하루 경계 03:00 KST([[캔버스-마감-스케줄]]) | `todayIn(currentSystemDefault())` | **불일치**(드리프트 3) |
| 연·월 선택 UI·주 시작 요일·드롭다운 | 정책 소스 없음 | 대조 대상 부재 — 코드가 먼저 확정(일요일 시작, 영문 월 표기) |

월 표기가 영문 약어(`Aug`)·드롭다운은 영문 전체(`August`)인데 요일 머리글은 한글이다 — 표기 언어가
한 화면 안에서 갈린다(C-001 상단 날짜 영문 고정과 같은 자리, OQ-P-082).

## 파일 구성

```
feature/groups/canvas/impl/
  component/CustomCalendar.kt      머리글·요일·그리드 + sideBorder + 프리뷰 3케이스
  component/CalendarDropdown.kt    Popup 내용(YGStrokeButton 목록 + 스크롤바)
  viewmodel/CanvasImageAddViewModel.kt  달력 상태·연 재조회·최근접 달 이동
  screen/CanvasImageAddScreen.kt   calendarContent 슬롯 연결·Dim 공유
  route/CanvasImageAddRoute.kt     인텐트 4종 배선
  res/values/strings.xml           요일 string-array 추가
domain/
  model/parfait/ParfaitHistory.kt              신규
  usecase/parfait/GetParfaitHistoriesUseCase.kt  신규(mock)
  usecase/parfait/GetParfaitYearsUseCase.kt      신규(mock)
core/util/android/extension/Modifier.kt        verticalScrollbar 신설
core/util/jvm/extension/LocalDateExtension.kt  toFirstDayOfMonth 신설
core/util/jvm/model/DateTextFormat.kt          monthFormat·fullMonthFormat 추가
core/designsystem/component/ygstrokebutton/YGStrokeButton.kt  borderWidth 파라미터
```
