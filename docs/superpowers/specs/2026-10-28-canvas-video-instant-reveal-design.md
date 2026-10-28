---
status: draft
related_code:
  - feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingVideoTimeline.kt
  - feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/VideoTimelineOptions.kt
  - feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasImageSaveRoute.kt
  - feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasVideoRecordLayer.kt
  - feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt
  - feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/PlacedTextTopping.kt
---

# 캔버스 타임랩스 영상 — 토핑 즉시 등장

## 의도

영상으로 저장할 때 토핑이 튀어 오르는 팝인(0.6배 → easeOutBack 넘침 → 1배, 0.25초 페이드인)이
과하다는 사용자 의견이 왔다("애니메이션 보잉보잉한거 없이 바로 나타나는게 더 이쁜거 같다").
스파이크로 넘침 제거 → 페이드 제거 → 확대 제거 순으로 걷어 내 보고 **연출 없이 한 장씩 바로
나타나는 판**으로 컨펌을 받았다.

## 목표 / 비목표

- 목표: 토핑과 텍스트가 원래 크기·불투명도로 **시작 프레임에 바로** 나타난다. 차례로 쌓이는 순서와
  타이밍(0~1.5초에 고르게 시작, 1.5~2초 완성본)은 그대로다.
- 목표: 연출을 걷어 낸 뒤 의미가 없어진 진행도(0~1) 모델을 코드에서 없앤다.
- 비목표: 영상 길이·해상도·fps·등장 구간 변경. 이미지 저장, 캔버스 메인 화면의 등장 연출(`revealTogether`).
- 반목표: 녹화 레이어에서 아직 등장하지 않은 토핑을 **컴포지션에서 빼는 것**. 이미지 로드 상태 집계와
  텍스트 박스 측정이 끊겨 첫 프레임 대기가 무너진다.

## 접근

| 안 | 내용 | 판단 |
|---|---|---|
| A. 최소 삭제 | 진행도 Float 경로를 두고 크기·알파 함수만 지운다 | 결과는 같지만 쓰이지 않는 0~1 값과 0.25초 개념이 경로 전체에 남는다 |
| **B. 보이는 개수** | 진행도 목록을 "이 프레임에 보이는 토핑 수" 정수 하나로 바꾼다 | **채택.** 이름과 동작이 맞고, 연속값이 들어올 자리가 없어 연출이 다시 붙을 여지가 줄어든다 |
| C. 토핑별 Boolean 목록 | B와 같되 목록으로 나른다 | 순서가 고정이라 개수 하나로 충분하다 |

정확성은 A와 B가 같다 — 아래 경계 계산과 fallback은 어느 안이든 똑같이 지켜야 한다. B를 고른 이유는
유지보수다.

## 결정된 것

- `toppingPopProgresses(): List<Float>` 를 `visibleToppingCount(toppingCount, frameIndex): Int` 로 바꾼다.
  토핑 `order` 는 `order * 등장구간프레임 <= frameIndex * toppingCount` 일 때 보인다(등장 구간 1.5초 × 30fps
  = 45프레임). **정수 비교**라 float 경계 오차가 없고, 시작 프레임에 바로 보인다 — 스파이크의 1프레임
  지연이 사라진다.
- 경로의 `toppingPopProgresses: List<Float>?` / `popProgress: Float` 를 `visibleToppingCount: Int?` /
  `visible: Boolean` 으로 바꾼다. `null` 이면 전부 보인다(녹화가 아닌 모든 자리).
- 숨김은 `graphicsLayer` 의 `alpha = 0f` 로 한다. 컴포지션은 유지한다(반목표 참고).
- **fallback**: 레이어 수가 `toppingCount` 와 어긋나도 마지막 프레임에는 전부 보여야 한다. 지금의
  `getOrNull(index) ?: 1f` 와 같은 성질을 지킨다 — 개수를 넘는 인덱스가 끝까지 숨지 않게 한다.
- 지운다: `TOPPING_POP_DURATION_SECONDS` · `POP_START_SCALE` · `POP_OVERSHOOT` · `toppingPopScale` ·
  `toppingPopAlpha`, 그리고 그것들을 검사하던 테스트.
- 문서: `docs/status.md` 영상 서술의 "팝인", OQ-P-429 ③(팝인 중 알파 < 1로 테두리 띠 잘림 — 원인이 사라짐)
  닫기, `docs/log.md` 한 줄. 위키에는 타임랩스 페이지가 없어 변경 없음.

## 빌더에게 남긴 것

- fallback을 구현하는 방식(레이어 목록 길이 기준 보정이든, 인덱스 클램프든).
- `CanvasTopping` 의 `graphicsLayer` 에서 테두리 띠 잘림을 피하려 진행 중에만 걸던 분기 정리.
