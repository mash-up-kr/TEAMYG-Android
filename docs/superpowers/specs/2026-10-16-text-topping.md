---
id: text-topping
title: 텍스트 토핑 — 캔버스 텍스트 업로드·수정·삭제 (Text Topping)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-10-26
related_code:
  # as-built — 신규 생성 플로우(단일 화면)
  - AnalyticsScreenType.kt#CANVAS_TEXT_INPUT
  - NavKeyCanvasTextInput.kt
  - CanvasTextInputScreen.kt
  - CanvasTextInputViewModel.kt
  - CanvasTextInputRoute.kt
  - CanvasTextInputToolbar.kt
  - TextColorChipRow.kt
  - TextToppingBackground.kt
  - TextToppingType.kt
  - TextToppingAlign.kt#nextTextToppingAlign
  - TextToppingColor.kt
  - TextToppingInput.kt
  - TextToppingLimits.kt#TEXT_TOPPING_DESIGN_CANVAS_WIDTH
  - TextToppingLayoutModifiers.kt#fitTextToppingToHeight
  - TextEditDragInput.kt#textEditTapInput
  - GraphemeClusterExtension.kt#graphemeClusterCount
  - GraphemeClusterExtension.kt#truncateToGraphemeClusters
  - TextToppingScript.kt#detectTextToppingScript
  - CanvasMainScreen.kt#addAction
  # 그대로 재사용한 사진 토핑 제스처·배경·포커스
  - ToppingTransformInput.kt#toppingTransformInput
  - ToppingHitTestInput.kt#toppingTapInput
  - ToppingPanelFocus.kt#panelFocusCenter
  - ToppingPanelFocus.kt#animatePanelFocusFraction
  - ToppingArrangeCanvasSurface.kt
  - ToppingHitTarget.kt
  # 후속 작업이 붙을 자리(아직 미구현 — 아래 본문 참고)
  - CanvasToppingArrangeViewModel.kt#mergeToppings
  - CanvasToppingVO.kt
  - ToppingTransform.kt
  - DayWindow.kt#DAY_BOUNDARY_HOUR
  - CanvasStatus.kt
  - ServerErrorCode.kt#Parfait
  - ToppingPlaceFailure.kt#isPermanentPlaceFailure
  - DeleteToppingUseCase.kt
  - ParfaitTextRemoteDataSource.kt
  - TextToppingStyle.kt
related_adr: ADR-0025, ADR-0026
related_spec: c106-topping-place-api, c106-topping-place, ygtextfield, user-events-design
related_architecture: state-management, module-structure, data-layer
supersedes:
superseded_by:
tags: [spec, parfait, canvas, topping, c-106, text]
---

# Spec: 텍스트 토핑

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

캔버스에 텍스트를 올리고(업로드), 수정·삭제하는 기능을 추가한다. 기존 "사진 토핑" 위에 **텍스트라는
두 번째 토핑 종류**를 얹는 것이지, 별개 시스템을 새로 만드는 것이 아니다 — 위치·레이어 순서·작성자·
권한·마감 처리는 사진 토핑과 동일한 체계를 그대로 쓴다. 색 세트·색 매핑 규칙 자체는 이 스펙의
범위가 아니고 별도 정의([`텍스트 타입 - 컬러 매핑 규칙.png`] 기준 자리만 잡아 둔다).

## 화면 식별자 — 신규 생성은 C-106 한 화면으로 입력·배치를 다 한다 (중요)

> **설계 변경(2026-10-16, as-built)**: 초안은 "키보드를 내리는 순간 C-106과 별개의 새 화면
> (C-106-Arrange)으로 전환"하는 2화면 분리였으나, **인스타그램 스토리처럼 화면 전환 없이 한
> 화면 안에서 편집/배치 모드만 가르는 단일 화면으로 확정**했다. 화면이 두 모드 사이에서
> 네비게이션으로 튀지 않아 텍스트 자리가 매끄럽게 이어지고, 두 ViewModel 사이로 입력 중인
> 내용을 넘길 필요도 없어진다. 별도 NavKey/ViewModel(`C-106-Arrange`, `CANVAS_TEXT_ARRANGE`)은
> 만들지 않았다.

**사진 토핑과 텍스트 토핑의 화면 구조가 다르다.** 사진 토핑은 C-106(`CanvasToppingPlaceViewModel`)
하나가 입력부터 이동·핀치·확인(쌓기)까지 전부 처리하고, C-305(`CANVAS_TOPPING_ARRANGE`)는 오직
"C-001에서 **이미 쌓인** 토핑을 다시 탭했을 때"만 쓰이는 재편집 전용 화면이다. 텍스트 토핑의
**신규 생성**도 같은 단일-화면 원칙을 따르되, 한 화면 안에서 키보드 노출 여부에 대응하는
**편집 모드 ↔ 배치 모드**를 로컬 플래그(`isEditing`) 하나로 가른다.

| 상태 | 화면 | 코드 현황 |
|---|---|---|
| 캔버스 메인 | C-001 | `CANVAS_MAIN` — 있음 |
| **텍스트 입력 ↔ 배치**(한 화면, 키보드 ↑ = 편집 / 키보드 ↓ = 배치) | **C-106** | `CANVAS_TEXT_INPUT`(`NavKeyCanvasTextInput`) — **신규, 있음**. `CanvasTextInputScreen`/`ViewModel`/`Route`. 편집 모드(가운데 포커스·전체 딤·입력 툴바)와 배치 모드(저장 자리·딤 해제·팬/핀치·쌓기 바)를 `isEditing` 로 가른다 |
| 이미 쌓인 토핑의 재편집 배치 상태 | C-305 | `CANVAS_TOPPING_ARRANGE` — 있음(사진 전용으로 이미 동작). 텍스트 재편집(E-2)도 이 화면을 **확장**해서 타는 안이 합리적이나 **아직 미구현** — 아래 "파일 구성"·"범위" 참고 |

`CANVAS_TEXT_INPUT` 은 사진 C-106(`CANVAS_TOPPING_PLACE`)과 같은 `screenId = "C-106"` 을 공유하되
`screenClass` 로 갈린다(`AnalyticsScreenType.kt`). 제스처 처리(이동 pan, 핀치 zoom/rotation)와
배치 중 키보드-포커스 애니메이션은 사진 토핑의
`ToppingTransformInput.kt#toppingTransformInput` / `ToppingHitTestInput.kt#toppingTapInput` /
`ToppingPanelFocus.kt#panelFocusCenter`·`#animatePanelFocusFraction` **컴포넌트를 그대로 재사용**한다.

사진 토핑 쪽 화면 분류 자체의 기존 혼선(정본 문서의 "C105" 하나 vs 코드의 C-106/C-305 분리)은
별개 미결 [OQ-P-430](../../synthesis/open-questions.md#2026-10-07-canvas_edit-화면-분류가-정본-문서와-코드에서-다르고-canvas_edit_border_tapped는-아직-못-심었다)로
이미 올라가 있다 — 참고만 하고 이 스펙이 새로 다루는 문제와는 구분한다.

## 범위

이 스펙은 기능 전체의 설계를 담는다. 서버 연동·재편집은 [text-topping-api](2026-10-26-text-topping-api.md)가 정본이다. 아래 "구현됨 / 미구현"으로 현재 상태를 명시한다.

- **구현됨 (as-built)**
  - "텍스트 입력" 메뉴 진입 → C-106 단일 화면(편집 모드 ↔ 배치 모드, `isEditing`)
  - 캔버스 중앙 생성 + 키보드 ↑, 빈 상태 플레이스홀더("텍스트를 입력해 주세요")
  - 그래핌 클러스터 기준 100자 제한·초과 자르기·카운터 빨간색, 빈 내용 판정
  - 타입 3종(기본/배경/배경+테두리) · 정렬 3종 순환 · 8색 팔레트 + Black/White 교차 · 색 팝오버
  - **자동 크기**(디자인 24부터 Text-Area 높이에 맞게 글자 크기·래핑 폭을 같은 비율로 연속 축소, 하한
    없음, 키보드 높이 변화 시 재계산). 키보드를 내린 순간의 값을 `designFontSize`(디자인 단위)로 들고
    배치에선 다시 계산하지 않는다
  - **캔버스 너비 기준 크기**: 글자 크기·래핑 폭·여백을 디자인 너비 335 기준 값 × (실제 캔버스 너비 / 335)
    로 그린다. 래핑 폭은 캔버스 너비(글자만 기준). 시스템 글꼴 배율은 무시한다(`fontScale = 1`)
  - 네모배경 여백·테두리 비율(1/3·1/6·1/24, 0.5dp clamp, 직각) — 자동 크기에 연동
  - **글꼴·자간 언어 분기**: 한/영 SUIT·자간 -1.1%, 일 Line Seed JP·-1.1%, 중 Line Seed TW·0%
    (내용의 주된 스크립트로 통째 적용), 행간 140%
  - 배치 상태 이동·핀치(크기·회전), 텍스트 재탭 편집 복귀
  - **서버 연동 구현됨** — [캔버스에 쌓기] 저장, C-001 렌더, C-305 재편집·이동·삭제.
    정본은 [text-topping-api](2026-10-26-text-topping-api.md)
  - **키보드 ↑ 3분기 제스처**: 탭=커서 / 롱프레스=시스템 선택 / 슬롭(touchSlop×2.5) 이상 끌기=키보드
    내리며 그 자리에서 이어 이동(`PointerEventPass.Initial` 선점 + 롱프레스 레이스). 두 번째 손가락이
    닿으면 키보드를 내리며 크기·회전까지 이어 간다. 빈 내용은 끌기·핀치 불가
  - **키보드 내리기 4경로 모두**(✓·빈 곳 탭·텍스트 끌기·Android 뒤로) — 끌기 외 경로는 **빈 내용이면
    생성 취소·C-001 복귀**(플레이스홀더만 있는 채로 배치에 남지 않음), 내용 있으면 배치 모드로
  - 편집·배치 모드 판정 영역 **최소 44×44**(글자가 작아도, 시각 크기는 그대로). 편집 중 박스 둘레
    44×44 안의 탭은 박스 탭으로 보고 편집을 유지한다
  - 박스 밖 탭 순차 닫힘(팝오버 → 키보드), 닫는 탭이 아래 요소로 전파되지 않음. ✓ 는 팝오버가 열려
    있어도 한 번에 편집을 끝내고, 어느 경로로 편집이 끝나도 팝오버를 닫는다
  - 헤더 X 확인 팝업(`YGModalQuitAddText` — 그만두기 / 계속 추가)
- **미구현 (설계만, 후속 PR/선행 조건 대기)**
  - 동시편집 병합 · 03:00 마감 — 사진 토핑의 기존 체계를 그대로 재사용한다
  - 정렬 저장 — 서버 계약에 필드가 없어 저장하지 않고 가운데로 그린다(OQ-P-451)
  - Android 뒤로가기의 "팝오버 먼저, 그다음 키보드" 순차 — 소프트 키보드가 떠 있으면 시스템이
    백을 먼저 먹어 키보드부터 내려가는 플랫폼 제약. 팝오버는 편집 모드에만 보이므로 키보드와 함께
    닫힌다(탭-아웃은 스펙대로 순차 동작). 자세한 근거는 "주의" 참고
- **제외 (이 기능 범위 밖)**
  - 색 세트·색 매핑 값 정의 자체(별도 문서) — 이 스펙은 "색 ID를 저장한다"까지만 다룬다
  - 비속어 필터, 개별 텍스트 신고, 접근성 기준
  - iOS 구현, 크로스플랫폼 줄바꿈 엔진 통일(아래 열린 질문에 완화책만 기록)
  - 서버 API 계약 자체 — 정본은 서버이고 미러는 [api/parfait-text.md](../../api/parfait-text.md)다

## 진입 · 플로우

```
C-001 하단 [추가 +] 탭
  → 2Depth 3단 펼침 [카메라로 촬영 / 갤러리에서 선택 / 텍스트 입력]
    → [텍스트 입력] 탭 → C-106 편집 모드 (캔버스 중앙에 생성, 키보드 ↑)
      → 내용 없이(공백만 포함) 완료 → 생성 취소, C-001로 복귀 (변경 없어 팝업 없음)
      → 100자 도달 → 추가 입력 무시 + 카운터 빨간색
      → 완료(✓) / 빈 곳 탭 → 같은 화면의 배치 모드로 전환 (키보드 ↓, 포커스 + 상단 쌓기 바)
        → 한 손가락 이동, 두 손가락 핀치로 크기·회전
        → 텍스트 재탭 → 편집 모드로 복귀 (키보드 ↑, 내용 유지)
        → [캔버스에 쌓기] → 서버 저장 뒤 C-001 반영
```

편집·배치는 C-106 한 화면에서 처리한다 — 위 "화면 식별자" 참고.

수정 플로우:

```
C-001에서 내 텍스트 탭 → 배치 상태(C-305, 포커스 + 모서리 X)
  → 한 번 더 탭 → C-106 편집 (키보드 ↑)
    → 이동·크기만 바꿨으면 바로 [캔버스에 쌓기] 가능 (C-106을 거치지 않아도 됨)
    → 모서리 X → 삭제 표시 → [캔버스에 쌓기] 때 반영 (사진 삭제와 동일)
    → 헤더 X → 변경 있으면 확인 팝업 (전부 취소, 지운 텍스트도 복구)
남의 텍스트 탭 → 토스트 "XX님이 N분 전에 쌓았어요"만 뜨고 진입하지 않음
```

**"텍스트 입력" 메뉴 항목은 구현됐다.** `CanvasMainScreen.kt#addAction`의 펼침 목록에 세 번째
`YGCanvasMenuItem`("텍스트 입력")이 추가돼 `CanvasMainIntent.OnClickTextInput` → `startToppingFlow`
→ `NavKeyCanvasTextInput` 로 간다. `startToppingFlow` 는 사진과 공유하는 초안(groupId·parfaitId·
nextPositionZ)만 쓰고, 텍스트 화면은 그 중 groupId 만 읽어 오늘 캔버스를 구독한다(배경·기존 토핑).

## 텍스트 스타일

| 항목 | 규칙 |
|---|---|
| 한글/영어/숫자 | SUIT Regular(400), 행간 140%, 자간 -1.1%(24px 기준 -0.264px) |
| 일본어 | Line Seed JP Regular(400), 행간 140%, 자간 -1.1% |
| 중국어 | Line Seed TW Regular(400), 행간 140%, 자간 0% |
| 공통 | 굵기·기울임·그림자·투명도 없음 |
| 자동 크기 | 최대 24px에서 시작. 텍스트 높이가 Text-Area 높이를 넘으면 **모든 줄이 들어갈 때까지 연속으로 축소, 하한 없음**. 줄이 줄면 24px까지만 다시 커짐. 핀치 배율과는 별개 레이어로 적용 |
| 타입 3종 | 기본 / 배경(네모배경) / 배경+테두리. `[타입]` 탭마다 기본→배경→배경+테두리 순환(새 텍스트는 기본에서 시작, 수정 시 현재 타입 유지). 글자 자체 테두리는 없음 |
| 정렬 3종 | `[정렬]` 탭마다 가운데→왼쪽→오른쪽 순환. 기본값: 기본 타입·가운데 정렬 |
| 색 | `[색]` 버튼 → 툴바 위 컬러칩 팝오버. 같은 버튼 재탭 시 닫힘, 열려 있어도 타이핑 가능, Text-Area 아래 48px를 잠깐 가림(텍스트 위치는 불변), 타입·정렬 조작으로는 닫히지 않음 |

**⚠️ 자동 크기 모델이 이번 라운드에서 바뀌었다.** 이전 논의(이 대화 초반)는 "24→20→16 3단계
이산 축소"를 전제했으나, 최신 요구사항 문서는 **인스타그램 스토리 방식의 연속 축소, 하한 없음**으로
확정됐다. 아래 "자동 크기 계산" 절은 이 최신 모델 기준이다.

**색상 팔레트 8색의 실제 hex 값이 확정됐다** (디자인 토큰명·값, 2026-10-16 사용자 확인):

| 색 | Stroke/Text(진한 톤) | Fill 배경(옅은 톤) |
|---|---|---|
| Black | `Color/Base/Black` `#0E0E0E` | White와 교차 사용(아래 참고) |
| White | `Color/Base/White` `#FAFAFA` | Black과 교차 사용(아래 참고) |
| Pink | `Function/Text/Stroke/Pink` `#FA91A2` | `Function/Text/Fill/Pink` `#FFF9FA` |
| Orange | `Function/Text/Stroke/Orange` `#FFBB42` | `Function/Text/Fill/Orange` `#FFFBF3` |
| Yellow | `Function/Text/Stroke/Yellow` `#FFD21F` | `Function/Text/Fill/Yellow` `#FEFEF6` |
| Green | `Function/Text/Stroke/Green` `#64DB88` | `Function/Text/Fill/Green` `#F4FCF7` |
| Blue | `Function/Text/Stroke/Blue` `#6DBBF2` | `Function/Text/Fill/Blue` `#F6FBFE` |
| Violet | `Function/Text/Stroke/Violet` `#CEA8FC` | `Function/Text/Fill/Violet` `#FAF4FF` |

규칙: 같은 색 안에서 **진한 톤 = 테두리(Fill+Stroke 타입)·텍스트(Default 타입) 색**, **옅은 톤 = 배경(Fill·Fill+Stroke 타입의 네모배경) 색**. Black/White 둘만 예외로, 자기 색의 옅은 버전을 쓰지 않고
서로 **교차**한다 — White 텍스트의 Fill 배경은 Black, Black 텍스트의 Fill 배경은 White
(이전에 본 목업 스크린샷의 "White-Fill = 검정 배경 + 흰 텍스트"와 일치).
Pink·Orange·Yellow·Green·Blue·Violet 값은 `TextToppingColor`(canvas impl `model/`)에 둔다 — 텍스트
토핑 전용이라 디자인시스템에 두지 않는다(Black/White 는 `YGAtomicColors.Gray` 교차 사용).

## 네모배경 (배경 · 배경+테두리 타입)

| 항목 | 규칙 |
|---|---|
| 글자 가로 최대 | 캔버스 너비(디자인 335 기준, 글자만, 배경 여백 불포함) |
| 좌우 여백 | 글자 크기 × 1/3 (24px → 8px, 20px → 약 6.7px) |
| 상하 여백 | 글자 크기 × 1/6 (24px → 4px, 20px → 약 3.3px) |
| 모서리 반경 | 0 (직각) |
| 배경 테두리(배경+테두리 타입만) | 굵기 = 글자 크기 × 1/24 (24px → 1px), **박스 바깥쪽**으로 |
| 핀치 배율 | 박스 전체(글자+여백+테두리)에 같이 적용 — 비율 유지 |
| 사용자 직접 조절 | 없음 (여백·테두리는 전부 자동) |
| 캔버스 밖으로 넘침 | 가능, 넘친 부분은 잘림(사진 토핑과 동일 — Clip Contents) |

**결정: 여백·테두리 두께의 소수점은 반올림하지 않고 그대로 그린다.** Android(Skia)·iOS(CoreGraphics)
모두 서브픽셀 두께를 안티앨리어싱으로 자연스럽게 렌더링하므로 정수 스냅이 불필요하다. 다만 핀치로
축소했을 때 테두리가 0.5px 미만으로 내려가면 흐려 보이는 문제가 있어, **렌더링 시 최소 두께
바닥값(0.5px)을 clamp로 둔다.**

## 입력

- 시스템 키보드, 엔터 = 줄바꿈(완료 아님)
- **100자까지. 줄 수·높이로는 입력을 막지 않는다** — 자동 크기 축소가 하한 없이 흡수하므로 글자 수
  제한 하나로 충분하다.
- 100자 도달 시 추가 입력 무시 + 카운터 빨간색. 붙여넣기로 100자 초과 시 앞 100자만 남김(그래핌
  클러스터 경계에서 자름 — 조합 이모지가 중간에서 깨지지 않게)
- 글자 수는 **화면에 보이는 글자 단위**(그래핌 클러스터)로 센다 — 🙌🏽, 👨‍👩‍👧 같은 조합 이모지도
  1자, 줄바꿈도 1자. `GraphemeClusterExtension`(직접 구현 — `BreakIterator`/ICU 미사용, ZWJ·스킨톤·
  키캡(Me)·국기(지역 표시자 쌍)·태그 시퀀스·CRLF 접합을 코드포인트 규칙으로 직접 처리)으로 센다
- 빈 내용 판정: 앞뒤 공백 제거 후 공백·줄바꿈만 남으면 빈 내용
- 지원 언어: 한/영/일/중/숫자/이모지. 비속어 필터·신고·접근성은 범위 밖

닉네임 입력 등에 쓰는 `YGTextFieldImpl.kt`의 `newValue.length <= maxLength`(UTF-16 코드 유닛 비교)는
조합 이모지를 여러 자로 세어 재사용하지 않고, 텍스트 토핑은 위 그래핌 카운터를 쓴다.

## 키보드 · 터치

### Text-Area와 텍스트 박스

- Text-Area는 **레이아웃 틀일 뿐 터치를 받지 않는다.** 실제 글자 크기에 맞춰 늘고 주는 텍스트 박스만
  터치 대상(hug content). 최소 터치 영역 44×44 — Android 머터리얼 기준(48×48dp)이 더 넉넉해 여유 있음.
  글자가 아주 작아져도 `TouchDelegate`나 min width/height로 터치 영역만 확장(시각 크기는 그대로)
- Text-Area 높이 = 캔버스 높이 − 키보드 높이 − 툴바 높이(52px, 디자인 기준 347px). `WindowInsets.ime`로
  실제 키보드 높이(추천어 줄·서드파티 키보드 포함)를 받아 가변 처리 — 기기별 추측 불필요

### 키보드 ↑ (입력·편집 중) 제스처 3분기 — **결정된 구현 방식**

| 입력 | 결과 |
|---|---|
| 탭 | 커서 이동(시스템 동작 그대로) |
| 길게 누르기 | 시스템 선택(복사/붙여넣기) |
| 끌기(일정 거리 이상) | 키보드 ↓ + 지금 보이는 위치에서 그대로 이어서 이동. 놓은 곳이 새 위치 |
| 두 손가락 핀치 | 키보드 ↓ + 크기·회전이 그대로 이어짐 → C-305 |

Compose `BasicTextField` 기준으로 **`PointerEventPass.Initial`에서 먼저 이벤트를 가로채 소비하는
방식**으로 구현한다(View 기반 EditText보다 깨끗하게 된다 — 내부 제스처가 전부 Compose
`pointerInput`이라 Main pass보다 먼저 보고 `consume()`하면 충돌이 안 남):

1. `awaitFirstDown` 직후 이 제스처 시작 시점의 키보드 표시 여부를 한 번만 캡처(`rememberUpdatedState`로
   읽되, `pointerInput`의 재시작 키로는 쓰지 않는다 — 드래그 중 키보드를 내리는 행위 자체가 키를
   바꿔 코루틴을 취소시키는 사고를 피하기 위함)
2. `withTimeoutOrNull(longPressTimeoutMillis)`로 "슬롭(`touchSlop × 2.5` 정도) 초과"와 "롱프레스 시간
   초과"를 레이스. 슬롭을 먼저 넘으면 그 이벤트만 `consume()`하고 드래그 확정, 타임아웃이 먼저
   끝나면 아무것도 소비하지 않고 반환 → 필드의 Main pass 제스처(롱프레스 선택)에 그대로 양도
3. 두 번째 손가락이 닿으면 슬롭과 무관하게 바로 확정한다. 확정 후 `onDragStart()`(배치 좌표를 심고
   `focusManager.clearFocus()` + `keyboardController?.hide()`), 이후 이벤트를 계속 `consume()`하며
   `onDrag(pan, zoom, rotation)` 호출 — 한 손가락이면 이동만, 두 손가락이면 이동·확대·회전을 함께 넘긴다

끌기 판정 거리는 `ViewConfiguration.touchSlop × 2.5` 정도를 기본값으로 하되 실기기 튜닝 대상.

### 박스 밖 탭 · 뒤로가기

- 박스 밖 탭: 열린 것부터 하나씩 닫힘(팝오버 → 키보드). **닫는 탭은 닫기만 하고 이동·토스트는
  일어나지 않는다** — 이벤트를 소비해 하위 요소(남의 사진 토스트 등)로 전파되지 않게 함
- 키보드 내리기 4가지(✓ / 박스 밖 탭 / 텍스트 끌기 / Android 뒤로가기) 전부 **편집 내용 유지**
- Android 뒤로가기: 1차는 팝오버/키보드 닫기, 2차는 나가기(변경 있으면 확인 팝업) — `OnBackPressedCallback`/
  Compose `BackHandler`로 표준 구현

### 배치 상태(신규 생성은 C-106의 배치 모드, 재편집은 C-305, 둘 다 키보드 ↓)

- 포커스 + 한 손가락 이동·두 손가락 핀치(크기·회전). **제스처 컴포넌트는** 사진 토핑의
  `ToppingTransformInput.kt#toppingTransformInput` / `ToppingHitTestInput.kt#toppingTapInput`을
  그대로 재사용한다. 신규 생성은 **C-106 한 화면 안의 배치 모드**(`isEditing = false`)로 처리하고,
  이미 쌓인 토핑 재편집은 C-305(`CanvasToppingArrangeViewModel` 확장, **미구현**)로 간다
- 텍스트를 다시 탭하면 편집 모드로 돌아가며 키보드 ↑. 편집 중엔 키보드가 떠 있는 동안 텍스트가
  Text-Area 가운데로 임시 이동, 배치로 돌아가면 원래 자리로 복귀(저장은 원래 자리 기준) —
  `ToppingPanelFocus.kt#panelFocusCenter`/`#animatePanelFocusFraction` 패턴과 동일한 축을 `isEditing`
  으로 보간한다
- 배치 모드의 탭 판정은 텍스트 렌더링 Box와 **형제**인 전면 오버레이 Box가 독점한다 —
  `BasicTextField` 를 제스처 Box의 자식으로 두면 readOnly 여도 내부 탭 제스처가 포인터를 먼저
  소비해 재탭(`onHit`)이 안 뜬다(`CanvasToppingPlaceScreen.kt` 와 같은 배치)

## 자동 크기 계산 (연속 축소, 하한 없음)

- `fitTextToppingToHeight`: 최대 크기(디자인 24 × 캔버스 비율)·래핑 폭(캔버스 너비)으로 한 번 재고,
  Text-Area 예산을 넘으면 넘친 비율만큼 글자 크기와 래핑 폭을 **같은 비율**로 줄인 뒤 한 번 더 재서
  보정한다. 래핑 폭을 같이 줄이므로 줄바꿈 위치는 그대로다. 측정은 Compose `TextMeasurer`
- 입력·Text-Area 예산(`WindowInsets.ime`로 구한 키보드 높이 포함)·캔버스 크기가 바뀔 때마다 다시 잰다
- 편집 중 결과를 디자인 단위(`designFontSize`)로 ViewModel 에 올린다. 배치는 이 값으로 그리고 다시
  재지 않는다 — 키보드를 내리는 순간 크기·줄바꿈이 바뀌지 않는다
- 줄바꿈 100번 같은 극단 케이스는 폰트 크기가 한없이 작아지는 문제가 생길 수 있어, 실기기에서
  SUIT로 렌더링 한계를 확인하는 QA 항목으로 남긴다(아래 열린 질문)

## 플랫폼 간 렌더링 차이 — 저장값 그대로 렌더링

같은 텍스트·같은 박스 폭이라도 iOS(CoreText)와 Android(Skia/ICU) 줄바꿈 엔진이 달라 자동 크기가
달라질 수 있고, 기기별 키보드 높이 차이로 Text-Area 높이 자체도 달라진다. **안드로이드 개발로
해소할 수 없는 구조적 차이다.**

- **결정: 작성자 기기에서 계산한 크기·배율을 저장하고, 보는 쪽은 재계산하지 않고 저장값 그대로
  렌더링한다.**
- 작성자가 **다른 기기**에서 다시 수정하면 그 기기 기준으로 자동 크기를 다시 계산해 저장(수정 =
  그 기기가 새 정본이 됨)
- ⚠️ **박스 폭·폰트 크기만 저장해서는 줄바꿈 위치까지 고정되지 않는다.** 보는 쪽 OS가 같은 폭 안에서
  자체 줄바꿈 엔진으로 다시 줄을 나누면 줄바꿈 위치 자체가 달라질 수 있다. 완전히 동일하게 보장하려면
  **작성 시점에 계산된 줄바꿈 위치를 저장값에 포함해(`lineBreakPositions` 또는 줄 단위로 쪼갠 문자열),
  읽는 쪽은 그 줄바꿈을 그대로 쓰고 재래핑하지 않아야 한다.** 이 필드가 데이터 모델에 들어가는지는
  서버 스키마 협의 대상(열린 질문).

## 빈 상태

- 공백·줄바꿈만 남아도 빈 내용
- 새 텍스트를 빈 채로 키보드 내림 → C-106이 닫히고 C-001로 복귀(변경 없어 팝업 없음)
- 수정 중 빈 채로 키보드 내림 → 해당 텍스트가 삭제로 표시(쌓기 때 반영)
- 빈 내용일 때는 끌기·핀치 불가

## 저장 · 삭제 · 동시편집 · 03:00 마감 — 사진 토핑 체계 재사용

- 키보드를 내려도 화면에만 반영. 실제 저장은 **[캔버스에 쌓기]** 때만. 삭제도 쌓기 때 반영
- 헤더 X → 변경 있으면 확인 팝업 1회 → 전부 취소(지운 텍스트도 복구)
- 텍스트 객체로 저장(이미지로 합성하지 않음) → 올린 뒤에도 수정 가능
- 수정·삭제는 작성자 본인만. 사진 토핑과 동일하게
  `CanvasToppingArrangeViewModel`의 `!topping.isMine` 분기(`ShowOthersToppingNotEditable`
  사이드이펙트)를 텍스트에도 그대로 적용
- 레이어 순서: 마지막에 쌓은 게 맨 위. 다른 사람의 텍스트·사진 위에도 쌓을 수 있음. 개수 제한 없음

**편집 중 동시편집 병합은 사진 토핑과 완전히 같은 패턴으로 간다.** 사진 토핑은 락이 아니라
**적응형 폴링(`CanvasPoller`, 10초 시작 → 변화 없으면 15/20초로 증가, 변화 있으면 10초 복귀)
+ dirty-set 병합**이다. `CanvasToppingArrangeViewModel#mergeToppings`가 `dirtyToppingIds`/
`pendingDeleteToppingIds`/`deletedToppingIds` 세 집합으로 서버 응답과 교집합하며, dirty한 항목은
로컬값을 유지하고 삭제는 툼스톤으로 가린다. **텍스트 토핑 id를 같은 dirty-set에 포함시키는 것으로
끝난다** — 새 동기화 메커니즘 불필요.

**작성·편집 중 03:00 마감도 사진 업로드와 동일하게 처리된다.** `DayWindow.DAY_BOUNDARY_HOUR = 3`이
하루 경계를 정의하고, 서버가 배치로 캔버스를 `CLOSED`/`EMPTY`(`CanvasStatus.kt`)로 전환하면 이미
지난 캔버스에 쓰기를 시도할 때 **서버가 409 `PARFAIT_ALREADY_CLOSED`로 거부**한다
(`ServerErrorCode.kt#Parfait`, 처리는 `ToppingPlaceFailure.kt#isPermanentPlaceFailure`). 텍스트 쓰기
요청에도 같은 409 처리 경로를 그대로 붙인다.

## 데이터 모델 (Android 로컬 모델 초안)

```kotlin
// 화면이 들고 있어야 하는 필드의 초안. 서버 요청(`PlaceParfaitTextRequest`)에는 정렬·줄바꿈 위치가 없다(OQ-P-451)
data class EditableTextTopping(
    val id: ToppingId,
    val content: String,              // 그래핌 클러스터 기준 최대 100자
    val textType: TextToppingType,    // DEFAULT / FILL / FILL_STROKE
    val colorId: String,              // YGColorChipType 팔레트 참조
    val align: TextAlign3,            // CENTER / LEFT / RIGHT
    val autoFontSizePx: Float,        // 작성 기기가 계산한 값, 그대로 저장(소수점 유지)
    val lineBreakPositions: List<Int>?, // 크로스플랫폼 줄바꿈 고정용 (열린 질문 — 서버 협의 필요)
    val pinchScale: Float,
    val rotationDegrees: Float,
    val positionX: Float,             // 원래 자리 기준, 캔버스 폭 대비 비율 (사진 토핑과 동일 좌표계)
    val positionY: Float,
    val positionZ: Int,                // 레이어 순서, 사진 토핑과 같은 z 공간 공유
    val authorId: GroupMemberId,
    val isMine: Boolean,
)
```

`positionX/Y/Z` 좌표계는 `ToppingTransform.kt`·`EditableTopping.kt`와 **동일한 비율 좌표계**를
공유해야 한다 — 사진과 텍스트가 같은 z-순서 공간에서 겹칠 수 있어야 하므로([c106-topping-place-api](archive/2026-08-20-c106-topping-place-api.md)의
좌표 변환 공식 참고).

## 파일 구성 (as-built)

**구현됨 (신규 생성 플로우, 단일 화면)**

| 자리 | 역할 |
|---|---|
| `feature/groups/canvas/impl/.../screen/CanvasMainScreen.kt` | `addAction` 펼침 목록에 "텍스트 입력" `YGCanvasMenuItem` 추가(별도 `YGCanvasMenuAction` 안 만듦) |
| `feature/groups/canvas/api/.../NavKeyCanvasTextInput.kt` (신규) | 파라미터 없는 `data object` NavKey. 내용은 초안이 아니라 화면 로컬 상태 |
| `feature/groups/canvas/impl/.../route/CanvasTextInputRoute.kt` (신규) | 인셋을 Scaffold가 미리 안 먹게 `contentWindowInsets = 0`, effect→네비게이션 배선 |
| `feature/groups/canvas/impl/.../screen/CanvasTextInputScreen.kt` (신규, **C-106**) | 편집/배치 모드를 `isEditing`(필드 포커스로 구동) 로 가르는 단일 화면. 정적 캔버스 9:16 프레임·딤·툴바·헤더, 캔버스 너비 기준 크기·자동 크기 적용·키보드 위 중심 계산·빈 내용 취소 판정 |
| `feature/groups/canvas/impl/.../viewmodel/CanvasTextInputViewModel.kt` (신규) | 그래핌 카운팅·타입/정렬/색 순환·배치 transform·초기 중앙 배치. 캔버스 구독(배경·기존 토핑) |
| `feature/groups/canvas/impl/.../component/CanvasTextInputToolbar.kt` (신규) | 색 칩·타입 프리뷰 버튼(`TypePreviewButton`)·정렬·글자수 상태바(52dp hug) |
| `feature/groups/canvas/impl/.../component/TextColorChipRow.kt` (신규) | 8색 가로 팝오버 |
| `feature/groups/canvas/impl/.../component/TextToppingBackground.kt` (신규) | 네모배경·테두리(글자 크기 비율, 단일 Box) |
| `feature/groups/canvas/impl/.../component/TextEditDragInput.kt` (신규) | 키보드 ↑ 제스처(`PointerEventPass.Initial` 선점, 슬롭 vs 롱프레스 레이스) — 끌기·핀치면 키보드 내리며 이동·확대·회전. `textEditTapInput` 은 박스 둘레 최소 터치 탭과 바깥 탭을 가른다 |
| `feature/groups/canvas/impl/.../component/TextToppingLayoutModifiers.kt` (신규) | 행간, 자동 크기(`fitTextToppingToHeight`), 편집↔배치 세로 배치, 캔버스 제약 없는 박스 측정 |
| `feature/groups/canvas/impl/.../model/TextToppingType.kt`·`TextToppingAlign.kt`·`TextToppingColor.kt`·`TextToppingLimits.kt`·`TextToppingScript.kt`·`TextEditModeState.kt`·`TextEditVerticalAnchor.kt` (신규) | 타입/정렬 순환, 8색 팔레트(`contentColor`/`backgroundColor`, 텍스트 토핑 전용이라 디자인시스템이 아니라 여기 둔다), 상수(max 100자·min scale·max font size·디자인 캔버스 너비), 언어별 글꼴·자간 분기(JP/TW `FontFamily` 도 여기 로컬 — 캔버스 밖에서 안 써 디자인시스템에 안 둔다), 편집 모드·세로 예산 상태 |
| `feature/groups/canvas/impl/.../util/TextToppingInput.kt` (신규) | 입력 변환(100자 초과분 자르기, 한글 줄바꿈 중복 보정) |
| `core/ui/.../component/modal/YGModalQuit.kt` | `YGModalQuitAddText`(텍스트 추가 그만두기 모달) |
| `feature/groups/canvas/impl/src/main/res/font/line_seed_{jp,tw}_regular.ttf` + `licenses/LINESeed*` (신규) | Line Seed JP/TW Regular 번들(공식 배포본·라이선스 포함) |
| `core/util/jvm/.../extension/GraphemeClusterExtension.kt` (신규) | 그래핌 클러스터 카운터(ZWJ·스킨톤 직접 접합), 경계 자르기 |
| `core/designsystem/.../component/ygfloatingbar/YGFloatingBar.kt` | `YGFloatingBarTitleConfirm` 가운데 정렬 보정(편집 헤더는 Title-Confirm, 배치 헤더는 Title 사용) |
| `core/designsystem/src/main/res/drawable/ic_align_{left,center,right}.xml` | 정렬 아이콘 |
| `core/analytics/.../AnalyticsScreenType.kt`, `app/.../NavKeyAnalyticsScreen.kt` | `CANVAS_TEXT_INPUT`(C-106) enum·매핑 |

**미구현 (후속 작업이 붙을 자리)**

| 자리 | 역할 |
|---|---|
| (없음) | C-305 재편집·저장소·유스케이스·`texts` VO 매핑은 [text-topping-api](2026-10-26-text-topping-api.md)에서 구현됐다 |

## 주의 / 열린 질문

- **Android 뒤로가기의 "팝오버 먼저" 순차는 플랫폼상 불가.** 소프트 키보드가 떠 있으면 시스템이
  백을 먼저 소비해 키보드를 내리므로(앱 `BackHandler` 로도 못 가로챔), 백 1회로 "팝오버만 닫고
  키보드 유지"가 안 된다. 팝오버는 편집 모드에서만 보여서 키보드가 내려가면 함께 사라진다. 순차
  닫힘이 필요한 **박스 밖 탭**은 스펙대로 팝오버→키보드로 동작한다. (현재 구현: 키보드 ↑ 백은
  `WindowInsets.ime` 관측으로 '포커스 살아있는 dismiss'를 집어 종료 처리.)
- **서버 계약이 화면 상태를 다 담지 못한다.** 배치 요청(`PlaceParfaitTextRequest`)은 `text`·좌표·`scale`·
  `rotation`·`color`·`fontSize`·`isFill`·`isStroke`뿐이다. 타입 3종은 `isFill`·`isStroke` 조합으로 실을 수
  있지만 **정렬 3종과 `lineBreakPositions`를 실을 필드가 없다**(OQ-P-451). 그 밖의 계약 미결 — `color` 형식
  (OQ-P-441), 글자 수 계수 기준(서버 2000자 UTF-16 vs 앱 100 그래핌, OQ-P-442), 이미지와 공유하는
  `positionZ` 배정(OQ-P-443), 멱등이 아닌 POST의 재시도(OQ-P-444)는 [api/parfait-text.md](../../api/parfait-text.md) 「미결」.
- **Line Seed JP/TW 폰트를 번들했다(공식 배포본).** `res/font/line_seed_{jp,tw}_regular.ttf`, 라이선스는
  `feature/groups/canvas/impl/licenses/`. 남는 고려: ⓐ APK 가 ~10.5MB 커짐(특히 TW 번체 전체 글리프 6.9MB) —
  필요하면 서브셋팅으로 축소, ⓑ 언어 판별은 **글자별이 아니라 내용의 주된 스크립트**로 통째 적용이라
  한 토핑에 일+중이 섞이면 하나로만 그려짐(Compose 편집 필드 제약), ⓒ 중국어는 **번체(TW)** 폰트.
- **그래핌 클러스터 카운팅은 `BreakIterator` 가 아니라 직접 구현**(`GraphemeClusterExtension`, ZWJ·
  스킨톤·키캡·국기·태그 시퀀스·CRLF 접합을 직접 처리)이라 ICU 버전 편차에 덜 민감하다. 그래도 최신 이모지 시퀀스는 실기기
  매트릭스로 확인 권장.
- **크로스플랫폼 줄바꿈 완전 동기화는 이 스펙만으로 닫히지 않는다.** 저장값 재사용 원칙은 정했지만
  서버 계약에 `lineBreakPositions` 필드가 없어 위 계약 간극(OQ-P-451)과 묶인다.
- 화면 식별자 C-305 통일 건은 사진 토핑 쪽 [OQ-P-430](../../synthesis/open-questions.md#2026-10-07-canvas_edit-화면-분류가-정본-문서와-코드에서-다르고-canvas_edit_border_tapped는-아직-못-심었다)에
  이미 올라가 있다 — 이 스펙에서 새 미결 항목을 만들지 않고 거기 묶는다.
- 줄바꿈 100번 같은 극단 입력에서의 렌더링·성능 한계는 구현 후 실기기 QA 항목으로 남긴다(수치
  기준 없음).
