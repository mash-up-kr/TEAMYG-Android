---
id: text-topping-api
title: 텍스트 토핑 서버 연동 — 배치·C-001 렌더링·C-305 재편집 (Text Topping API Wiring)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-10-26
related_code:
  - ParfaitTextRemoteDataSource.kt
  - TextToppingVOMapper.kt
  - PlacedTextToppingVO.kt
  - TextToppingStyle.kt
  - TextToppingUpdate.kt
  - CanvasVO.kt
  - CanvasToppingVO.kt
  - ToppingRepository.kt#updateAll
  - CanvasTextInputViewModel.kt#handleOnClickStack
  - CanvasMainViewModel.kt#handleOnClickTopping
  - CanvasMainViewModel.kt#nextPositionZ
  - CanvasToppingLayer.kt#CanvasToppingLayer
  - CanvasToppingArrangeViewModel.kt#mergeToppings
  - CanvasToppingArrangeViewModel.kt#handleOnClickConfirm
  - NavKeyCanvasTextInput.kt
  - NavKeyCanvasToppingArrange.kt
  - TextToppingColor.kt
  - TextToppingType.kt
  - TextToppingLimits.kt#TEXT_TOPPING_DESIGN_CANVAS_WIDTH
related_adr: ADR-0029
related_spec: text-topping, c106-topping-place-api
related_architecture: data-layer, state-management
supersedes:
superseded_by:
tags: [spec, parfait, canvas, topping, text, api]
---

# Spec: 텍스트 토핑 서버 연동

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.
> 화면 동작의 정본은 [텍스트 토핑 스펙](2026-10-16-text-topping.md)이다. 이 문서는 그 스펙의
> "미구현" 절(저장·C-001 렌더링·C-305 재편집·삭제)을 서버 계약([api/parfait-text.md](../../api/parfait-text.md))에
> 붙이는 방법만 다룬다.

## 목표

C-106 [캔버스에 쌓기]가 텍스트를 실제로 서버에 저장하고(OQ-P-450), 캔버스 응답의 `texts`를
C-001이 그리며(OQ-P-439 ①·③), 내 텍스트를 C-305에서 이동·크기·회전·내용 수정·삭제할 수 있게 한다.
텍스트는 사진 토핑의 두 번째 종류다 — 위치 좌표계·겹침 축·권한·dirty-set 병합·409 마감 처리는
사진 체계를 그대로 쓴다.

## 범위

- **포함**
  - 도메인: 캔버스 VO에 `texts`, 텍스트 저장소·유스케이스, 일괄 PATCH의 `textItems`
  - C-106 신규 배치(POST) — 로딩·실패 처리 포함
  - C-001(오늘·과거 캔버스 공통)·이미지 저장·타임랩스 녹화에서 텍스트 렌더링
  - C-001 탭: 내 텍스트 → C-305, 남의 텍스트 → Spotlight(사진과 같은 토스트)
  - C-305: 텍스트 포커스·이동·핀치·삭제, 재탭 → C-106 편집 모드, [캔버스에 쌓기]로 일괄 반영
  - 새 토핑의 `positionZ`를 이미지·텍스트 합친 최댓값 + 1로
- **제외**
  - 정렬 저장 — 서버 필드가 없다(OQ-P-451). 사용자가 서버팀에 제보한다. 그때까지 정렬 UI는 그대로 두고
    저장·렌더링은 가운데 정렬이다
  - `lineBreakPositions`(줄바꿈 고정), iOS와 저장 단위 합의 — 이 문서의 단위를 Android 안으로 기록하고
    OQ로 넘긴다
  - 달력 빈 날 판정에 텍스트 포함 여부(OQ-P-439 ②) — 기획 판단 대기
  - 푸시 `TEXT` type(OQ-P-445), 알림 문구
  - POST 멱등 보장(OQ-P-444) — 앱은 자동 재시도를 하지 않는 것으로만 대응한다

## Approaches

**도메인에서 텍스트를 어디에 둘까.**

1. **`CanvasVO.texts` 별도 목록 (채택)** — 서버 응답 모양 그대로. 기존 `toppings` 소비처(타임랩스·
   이미지 저장·외곽선·Spotlight)를 건드리지 않고 텍스트가 필요한 곳만 합친다. 합치는 규칙(겹침 순서)을
   한 함수로 모아야 하는 비용이 있다.
2. `CanvasToppingVO`를 sealed(Image/Text)로 — 그리기 순서가 자연스럽지만 `imageUrl`·`border`·
   `parfaitImageId`를 읽는 소비처 전부가 분기를 떠안는다. 변경 범위가 C-001 밖으로 번진다.

**C-305 재편집에서 내용 수정을 어떻게 받을까.** 서버는 배치 뒤 `text`를 바꾸지 못한다.

1. **C-305가 쌓기 때 "새로 배치 → 옛 것 삭제" (채택)** — 스펙의 "저장은 쌓기 때만"을 지킨다.
   배치가 실패하면 옛 텍스트가 남아 내용이 사라지지 않는다. 대가: id가 바뀌고 알림이 다시 나간다.
2. C-106 편집에서 즉시 서버 반영 — 단순하지만 C-305 헤더 X의 "전부 취소"를 지킬 수 없다.
3. 삭제 후 재배치 — 배치 실패 시 텍스트를 잃는다.

**C-305 → C-106 편집 결과를 어떻게 돌려받을까.** NavKey는 결과를 돌려주지 않는다. 기존
`CanvasVideoSourceHolder` 패턴대로 피처 내부 홀더(`TextEditSessionHolder`)에 편집 대상을 넣고,
C-106이 결과를 써 두면 C-305가 화면 복귀 때 읽는다(Hilt 싱글턴 상태 홀더 — 내 판단).

## 저장 단위 (내 판단 — OQ로 iOS 합의 대기)

| 서버 필드 | 값 | 근거 |
|---|---|---|
| `positionX`·`positionY` | 텍스트 박스 **중심**의 Canvas-Area 대비 비율(0~1) | 사진 `toToppingTransform`과 같은 좌표계 |
| `positionZ` | 신규: 캔버스 이미지·텍스트 합친 `positionZ` 최댓값 + 1. 재배치: 옛 값 유지 | OQ-P-443 동률 규칙 미정 |
| `fontSize` | `designFontSize` — 디자인 캔버스 너비 335 기준 글자 크기(자동 크기 결과, ≤24) | 기기 너비와 무관한 값 |
| `scale` | 배치 모드 핀치 배율(`CanvasTextInputUiState.scale`) | 표시 크기 = `fontSize × scale` (서버 계약) |
| `rotation` | 도(degree) | 사진과 같다 |
| `color` | `TextToppingColor.contentColor`의 `#RRGGBB` 대문자 | OQ-P-441 |
| `isFill`·`isStroke` | `TextToppingType.hasFill`·`hasBorder` | 타입 3종 ↔ 두 Boolean |

**읽기 규칙.**

- 색: `#RRGGBB`(대소문자 무시)로 `TextToppingColor`를 찾는다. 못 찾으면 `Black`으로 그린다(내 판단 —
  안 그리는 것보다 낫고, 기본 배경이 흰색이다).
- 타입: `isFill=false` → `DEFAULT`, `isFill && !isStroke` → `FILL`, 둘 다 `true` → `FILL_STROKE`.
  `isFill=false, isStroke=true`는 앱이 만들지 않는 조합이라 `DEFAULT`로 접는다.
- 크기: 실제 캔버스 너비 `W`에서 글자 크기 `fontSize × W / 335`, 래핑 폭 `W × fontSize / 24`.
  이것은 C-106 자동 크기가 글자 크기와 래핑 폭을 같은 비율로 줄이는 규칙(`fitTextToppingToHeight`)을 거꾸로 푼 것이다.
  그 위에 `scale`을 `graphicsLayer`로 곱한다. 시스템 글꼴 배율은 무시한다(`fontScale = 1`).
- 글꼴·자간: 저장된 `text`로 `detectTextToppingScript`를 다시 돌린다.

**C-106·C-001·C-305가 같은 렌더러를 쓴다.** 텍스트 하나를 그리는 컴포저블(`TextToppingContent` 가칭)을
뽑아 세 화면이 공유한다. 쌓기 전후로 크기·줄바꿈이 튀면 안 된다.

## 동작

### 도메인·데이터

- `CanvasTextToppingVO` 신설: `parfaitTextId`·`text`·`transform`·`style`·`placedBy`·`isMine`·`createdAt`.
  `isMine`은 사진처럼 `placedBy.ownerType == ME`. `CanvasVO.texts`(서버 `null` → 빈 목록).
- `TextToppingRepository`: `place`·`update`·`delete` — `ParfaitTextRemoteDataSource` 위임.
- 일괄 PATCH: `ToppingRepository.updateAll`이 이미지·텍스트 변형을 함께 받아 한 요청에 싣는다
  (`textItems`). 부분 성공이 없는 서버 계약 그대로다.
- 유스케이스: `PlaceTextToppingUseCase`·`UpdateTextToppingUseCase`(스타일·변형 단건 PATCH)·
  `DeleteTextToppingUseCase`. 기존 `UpdateToppingsUseCase`는 텍스트 변형을 함께 받도록 넓힌다.

### C-106 신규 배치

- [캔버스에 쌓기] → `isLoading = true`로 버튼·제스처를 막고 POST 한 번. **자동 재시도 없음**(OQ-P-444).
- 성공 → 오늘 캔버스 강제 갱신(`RefreshTodayParfaitDetailUseCase`) → 초안 비움 → C-001.
- 실패 → 로딩 해제, 내용 유지, 실패 토스트. 409 `PARFAIT_ALREADY_CLOSED`는 사진 배치와 같은
  영구 실패 처리(`isPermanentPlaceFailure`)를 따른다.
- 분석 이벤트 `canvas_text_stack_tapped`는 그대로 탭 시점에 남긴다.

### C-001 렌더링

- 그리는 순서: 이미지·텍스트를 `positionZ` 오름차순으로 합친다. 같으면 이미지가 아래, 그다음 id 오름차순
  (내 판단 — OQ-P-443).
- 텍스트는 이미지 로딩을 기다리는 일괄 등장(`rememberBatchRevealState`)에 같이 묶여 함께 나타난다.
- 판정: 회전된 텍스트 박스 사각형(배경 여백 포함). 사진의 외곽선 판정은 쓰지 않는다.
- 탭: 내 텍스트(오늘 캔버스) → C-305, 텍스트에 포커스. 남의 텍스트 → Spotlight + "XX님이 N분 전에
  쌓았어요" 토스트(사진과 같은 경로). Spotlight 대상 id는 이미지·텍스트를 가르는 키로 넓힌다.
- 이미지 저장·타임랩스 녹화도 같은 레이어를 쓰므로 텍스트가 함께 찍힌다. 타임랩스 팝인 순서도 합친 순서를 따른다.

### C-305 재편집

- 목록에 내 텍스트·남의 텍스트가 섞인다. 남의 텍스트 탭 → `ShowOthersToppingNotEditable`(사진과 같다).
- 포커스 키는 이미지·텍스트를 가른다(`NavKeyCanvasToppingArrange`의 초기 포커스도 마찬가지).
- 텍스트 포커스 중에는 테두리 패널을 열 수 없다.
- 이동·핀치 → dirty. 모서리 X → 삭제 대기.
- **포커스된 내 텍스트를 다시 탭** → 홀더에 편집 대상을 싣고 C-106 편집 모드로 간다. C-106은 내용·색·
  타입·`designFontSize`·자리·크기·회전을 그 값으로 채운다. 편집 모드 [캔버스에 쌓기]는 서버를 부르지
  않고 결과를 홀더에 써 두고 돌아온다. 빈 내용으로 끝내면 결과는 "삭제"다. 헤더 X는 결과 없이 돌아온다.
- C-305가 돌아온 결과를 로컬 목록에 반영하고 dirty(내용이 바뀌었으면 "재배치 필요"로 표시)로 둔다.
- C-305 [캔버스에 쌓기] 순서:
  1. 변형 변경(이미지 + 텍스트) → 일괄 PATCH 한 번
  2. 사진 테두리 변경 → 기존 단건 PATCH
  3. 텍스트 스타일만 바뀜(색·타입·`fontSize`) → `PATCH /texts/{id}`
  4. 텍스트 내용 바뀜 → 새 값으로 POST, 성공하면 옛 id DELETE
  5. 삭제 대기(이미지·텍스트) → 저장이 전부 성공한 뒤 DELETE
  - 실패하면 기존과 같이 로딩만 풀고 토스트, 화면에 남는다. 4의 POST는 성공했는데 옛 DELETE가 실패하면
    옛 id를 삭제 대기로 옮겨 다음 확인 때 다시 지운다 — 같은 텍스트를 두 번 POST하지 않기 위해서다.
- 병합: 기존 `mergeToppings`의 세 집합(dirty·삭제 대기·툼스톤)을 텍스트 id에도 같은 규칙으로 쓴다.
  이미지 id와 텍스트 id는 다른 테이블이라 겹칠 수 있다 — 집합 키는 종류를 포함해야 한다.

## 파일 구성 (예정)

| 층 | 파일 | 역할 |
|---|---|---|
| domain | `model/canvas/CanvasTextToppingVO.kt`, `CanvasVO.texts` | 캔버스 위 텍스트 |
| domain | `repository/topping/TextToppingRepository.kt` | 배치·수정·삭제 |
| domain | `usecase/topping/{Place,Update,Delete}TextToppingUseCase.kt` | 화면 진입점 |
| domain | `ToppingRepository.updateAll`·`UpdateToppingsUseCase` | 텍스트 변형 동승 |
| data | `repository/topping/TextToppingRepositoryImpl.kt`, `RepositoryModule` 바인딩 | 위임 |
| data | `source/parfait/mapper/VOMapper.kt` | `texts` → VO |
| data | `ParfaitImageRemoteDataSource.updateToppings` | `textItems` 채움 |
| feature | `component/TextToppingContent.kt` | 세 화면 공용 렌더러 |
| feature | `util/TextToppingWireMapper.kt`, `model/TextToppingType.of` | 색·크기 ↔ 서버 값. 서버 `isFill`·`isStroke` → 타입은 `TextToppingType.of` |
| feature | `util/TextEditSessionHolder.kt` | C-305 ↔ C-106 편집 왕복 |
| feature | `CanvasToppingLayer`·`CanvasMainViewModel`·`CanvasToppingArrangeViewModel`·`CanvasTextInputViewModel` | 연동 |
| api | `NavKeyCanvasTextInput`(편집 여부), `NavKeyCanvasToppingArrange`(초기 포커스 종류) | 진입 인자 |

## 주의 / 열린 질문

- 정렬은 저장되지 않는다. 쌓은 뒤 C-001·다른 기기는 가운데 정렬로 그린다(OQ-P-451, 서버 제보 중).
- 저장 단위(`fontSize` = 335 기준 디자인 크기, 중심 비율 좌표)는 iOS와 합의되지 않았다 — 새 OQ로 올린다.
- 내용 수정은 재배치라 다른 구성원에게 알림이 다시 간다(서버 계약 한계).
- 줄바꿈은 기기마다 다시 계산된다(텍스트 토핑 스펙 「플랫폼 간 렌더링 차이」).
