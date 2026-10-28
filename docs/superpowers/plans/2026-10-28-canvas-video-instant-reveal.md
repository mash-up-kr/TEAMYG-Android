---
id: canvas-video-instant-reveal
title: 캔버스 타임랩스 영상 — 토핑 즉시 등장
status: in-progress
type: work-order
created: 2026-10-28
updated: 2026-10-28
platforms: android
owner: Parfait 팀
related_adr: 0035
related_spec: canvas-video-instant-reveal-design
related_code: ToppingVideoTimeline.kt#visibleToppingCount, VideoTimelineOptions, CanvasImageSaveRoute, CanvasVideoRecordLayer, CanvasToppingLayer, PlacedTextTopping
archived_reason:
tags: [plan, parfait, canvas, video]
---

# 캔버스 타임랩스 영상 — 토핑 즉시 등장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 영상 저장본에서 토핑·텍스트가 팝인 연출 없이 원래 크기·불투명도로 시작 프레임에 바로 나타나게 하고, 의미를 잃은 진행도(0~1) 모델을 "보이는 개수" 정수 하나로 바꾼다.

**Architecture:** `toppingPopProgresses(): List<Float>` 를 `visibleToppingCount(): Int` 로 바꾼다. 녹화 Route가 프레임마다 그 개수를 세워 `CanvasVideoRecordLayer` → `CanvasToppingLayer` 로 내리고, 레이어는 `index < visibleToppingCount` 가 아닌 토핑을 `alpha = 0f` 로 숨긴다(컴포지션은 유지). 크기·알파 연출 함수와 상수는 지운다.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4(`org.junit.Assert`).

**Spec:** [`docs/superpowers/specs/2026-10-28-canvas-video-instant-reveal-design.md`](../specs/2026-10-28-canvas-video-instant-reveal-design.md)

## Global Constraints

- 영상 수치는 그대로다: 2초, 30fps(`FRAME_COUNT` 60), 등장 구간 1.5초(45프레임), 1080×1920.
- 순서는 `canvasLayerOrder` 가 만든 겹침 순서(이미지·텍스트 합친 `positionZ` 오름차순) 그대로다.
- 토핑 `order`(0부터)는 `order * 45 <= frameIndex * toppingCount` 일 때 보인다 — 정수 비교. iOS(`CanvasImageExporter.toppingVisibilities`, `time >= 1.5 * order / count`)와 모든 프레임에서 같은 결과여야 한다.
- 아직 안 보이는 토핑은 **컴포지션에서 빼지 않는다** — `graphicsLayer` 의 `alpha = 0f` 로만 숨긴다. 빼면 이미지 로드 상태 집계와 텍스트 박스 측정이 끊긴다.
- 이미지 저장, 캔버스 메인의 `revealTogether` 연출은 건드리지 않는다.
- 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다: 코드가 이미 말하는 것은 쓰지 않는다 · `@return`·`@param`은 타입·이름이 말하지 못할 때만 · 다른 컴포넌트의 현재 상태를 단정하지 않는다.
- 커밋 메시지는 `type: 한국어 설명`, `Co-Authored-By` 줄을 넣지 않는다. 원격 push 금지.

## Review Focus

1. **iOS와 경계 불일치** — 같은 캔버스를 두 플랫폼에서 저장했을 때 토핑이 다른 프레임에 나타나면 안 된다. → Task 1 `visibleToppingCount_everyFrame_matchesIosBoundary`.
2. **마지막 프레임이 이미지 저장물과 다름** — 토핑이 아주 많아도(45개 초과) 1.5초 시점과 마지막 프레임에는 전부 보여야 한다. → Task 1 `visibleToppingCount_fromAppearWindowEnd_showsAll`.
3. **프레임이 거꾸로 감** — 한 번 나타난 토핑이 다음 프레임에 사라지면 깜빡인다. → Task 1 `visibleToppingCount_acrossFrames_neverDecreases`.
4. **녹화가 아닌 자리의 회귀** — `visibleToppingCount = null`(캔버스 메인·정리 화면 등)에서 모든 토핑이 보이고 기존 텍스트 `alpha` 인자가 그대로 먹어야 한다. 자동 테스트 없음 — Task 1 Step 6에서 기존 화면을 확인한다.
5. **보이는 토핑의 테두리 띠 잘림** — 보이는 토핑에는 `alpha` 를 걸지 않아야 한다(오프스크린 합성으로 띠가 잘린다). Task 1 Step 3 지시로 막는다.

---

### Task 1: 진행도를 보이는 개수로 교체하고 팝인 연출 제거

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/VideoTimelineOptions.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingVideoTimeline.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasImageSaveRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasVideoRecordLayer.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/PlacedTextTopping.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingVideoTimelineTest.kt`

**Interfaces:**
- Produces: `internal fun visibleToppingCount(toppingCount: Int, frameIndex: Int): Int` (`ToppingVideoTimeline.kt`), `VideoTimelineOptions.TOPPING_APPEAR_WINDOW_FRAMES: Int = 45`

- [x] **Step 0: 스파이크 변경을 되돌린다**

작업 트리에 커밋되지 않은 스파이크 수정(`POP_OVERSHOOT = 0f`, `POP_START_SCALE = 1f`, 단계식 `toppingPopAlpha`)이 있으면 버린다. 이 Task가 같은 자리를 새로 쓴다.

Run: `git checkout -- feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/VideoTimelineOptions.kt feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingVideoTimeline.kt && git status --short`
Expected: 출력 없음

- [x] **Step 1: 테스트를 새 계약으로 바꾼다**

`ToppingVideoTimelineTest` 에서 `toppingPopProgresses_*`, `toppingPopScale_*`, `toppingPopAlpha_*` 를 지우고 `canvasVideoFrameCount_always_isTwoSeconds` 는 남긴다. 아래를 추가한다.

```kotlin
@Test
fun visibleToppingCount_noToppings_isZero() {
    assertEquals(0, visibleToppingCount(toppingCount = 0, frameIndex = 0))
    assertEquals(0, visibleToppingCount(toppingCount = 0, frameIndex = VideoTimelineOptions.FRAME_COUNT - 1))
}

@Test
fun visibleToppingCount_firstFrame_showsFirstToppingWithoutIntro() {
    assertEquals(1, visibleToppingCount(toppingCount = 3, frameIndex = 0))
}

@Test
fun visibleToppingCount_threeToppings_appearsOnStartFrame() {
    // 시작 시각 0초·0.5초·1초 = 0·15·30 프레임. 시작 프레임에 바로 보인다
    assertEquals(1, visibleToppingCount(toppingCount = 3, frameIndex = 14))
    assertEquals(2, visibleToppingCount(toppingCount = 3, frameIndex = 15))
    assertEquals(2, visibleToppingCount(toppingCount = 3, frameIndex = 29))
    assertEquals(3, visibleToppingCount(toppingCount = 3, frameIndex = 30))
}

@Test
fun visibleToppingCount_fromAppearWindowEnd_showsAll() {
    for (count in 1..200) {
        assertEquals(count, visibleToppingCount(count, VideoTimelineOptions.TOPPING_APPEAR_WINDOW_FRAMES))
        assertEquals(count, visibleToppingCount(count, VideoTimelineOptions.FRAME_COUNT - 1))
    }
}

@Test
fun visibleToppingCount_acrossFrames_neverDecreases() {
    for (count in 1..200) {
        val counts = (0 until VideoTimelineOptions.FRAME_COUNT).map { visibleToppingCount(count, it) }
        assertEquals(counts.sorted(), counts)
    }
}

@Test
fun visibleToppingCount_everyFrame_matchesIosBoundary() {
    // iOS CanvasImageExporter.toppingVisibilities 의 식을 그대로 옮긴 것
    for (count in 1..200) {
        for (frame in 0 until VideoTimelineOptions.FRAME_COUNT) {
            val time = frame.toDouble() / VideoTimelineOptions.FRAMES_PER_SECOND
            val ios = (0 until count).count { order -> time >= 1.5 * order / count }
            assertEquals("count=$count frame=$frame", ios, visibleToppingCount(count, frame))
        }
    }
}
```

`assertTrue` 를 더 쓰지 않으면 import를 지운다.

- [x] **Step 2: 실패를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*ToppingVideoTimelineTest'`
Expected: 컴파일 실패 — `Unresolved reference: visibleToppingCount`

- [x] **Step 3: 구현한다**

`VideoTimelineOptions`:
- 지운다: `TOPPING_POP_DURATION_SECONDS`, `POP_START_SCALE`, `POP_OVERSHOOT`.
- 추가한다: `const val TOPPING_APPEAR_WINDOW_FRAMES: Int = (TOPPING_APPEAR_WINDOW_SECONDS * FRAMES_PER_SECOND).toInt()` — 정수 비교가 이 값을 쓴다.
- `TOPPING_APPEAR_WINDOW_SECONDS` KDoc의 "등장을 **시작**한다"를 "나타난다" 취지로 고친다(이제 시작과 완료가 같다).

`ToppingVideoTimeline.kt`: `toppingPopProgresses` · `toppingPopScale` · `toppingPopAlpha` 를 지우고 `visibleToppingCount(toppingCount: Int, frameIndex: Int): Int` 를 둔다. 식:

```kotlin
if (toppingCount == 0) return 0
return minOf(toppingCount, frameIndex * toppingCount / VideoTimelineOptions.TOPPING_APPEAR_WINDOW_FRAMES + 1)
```

KDoc에는 float 대신 정수로 비교하는 이유(경계 프레임에서 반올림 오차가 등장을 한 프레임 흔들지 않게)와 iOS와 같은 경계여야 한다는 것만 적는다.

`CanvasImageSaveRoute`:
- `recordPopProgresses` 를 `var recordVisibleToppingCount by remember { mutableIntStateOf(0) }` 로 바꾼다.
- `advanceFrame` 에서 `visibleToppingCount(toppingCount = videoSource.toppings.size + videoSource.texts.size, frameIndex = frameIndex)` 를 넣는다.
- `CanvasVideoRecordLayer(visibleToppingCount = recordVisibleToppingCount, ...)` 로 넘긴다.

`CanvasVideoRecordLayer`: `toppingPopProgresses: List<Float>` → `visibleToppingCount: Int`. `CanvasToppingLayer` 에 그대로 넘긴다.

`CanvasToppingLayer`:
- 공개 인자 `toppingPopProgresses: List<Float>? = null` → `visibleToppingCount: Int? = null`. KDoc: `null` 이면 전부 보인다, 순서는 [canvasLayerOrder].
- 레이어 루프의 `popProgress = toppingPopProgresses?.getOrNull(index) ?: 1f` → `visible = visibleToppingCount == null || index < visibleToppingCount`. 그 위 "길이가 어긋난 프레임에서는 다 보이는 쪽으로 접는다" 주석은 지운다 — 개수는 이 레이어가 받는 것과 같은 `toppings`·`texts` 로 세므로 어긋날 길이가 없다. 대신 "개수는 이 레이어에 넘긴 `toppings`·`texts` 합으로 세야 한다"는 계약을 `visibleToppingCount` KDoc에 둔다.
- `CanvasLayer` · `CanvasTopping` · `CanvasText` 의 `popProgress: Float` → `visible: Boolean`(`CanvasTopping`·`CanvasText` 는 기본값 `true`).
- `CanvasTopping` 의 `graphicsLayer`: 크기 변경을 지우고 `if (!visible) alpha = 0f` 만 남긴다. **보이는 토핑에는 alpha를 걸지 않는다** — 기존 주석(테두리 띠가 alpha < 1에서 잘린다)의 취지를 이 분기에 맞게 남긴다.
- `CanvasText` 는 `PlacedTextTopping(alpha = if (visible) 1f else 0f, ...)` 로 넘긴다.

`PlacedTextTopping`: `popProgress` 인자를 지운다. `graphicsLayer` 는 `scaleX = scale`, `scaleY = scale`, `this.alpha = alpha` 로 돌아간다. `toppingPopScale`·`toppingPopAlpha` import를 지운다.

- [x] **Step 4: 테스트 통과를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*ToppingVideoTimelineTest'`
Expected: BUILD SUCCESSFUL, 7 tests passed

- [x] **Step 5: 남은 참조·린트를 확인한다**

Run: `grep -rn "toppingPop\|popProgress\|POP_START_SCALE\|POP_OVERSHOOT\|TOPPING_POP_DURATION" feature/ ; ./gradlew :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:ktlintCheck`
Expected: grep 출력 없음, BUILD SUCCESSFUL

- [ ] **Step 6: 기존 화면 회귀를 확인한다**

에뮬레이터·기기가 있으면 앱을 띄워 캔버스 메인에서 토핑·텍스트가 그대로 보이는지, 저장 미리보기에서 「영상으로 저장」을 눌러 갤러리 mp4에서 토핑이 연출 없이 하나씩 나타나는지 본다. 기기가 없으면 수행하지 않았다고 보고한다(OQ-P-429 ①에 이미 남아 있다).

- [x] **Step 7: 커밋한다**

```bash
git add feature/groups/canvas/impl
git commit -m "feat: 캔버스 영상에서 토핑이 연출 없이 바로 나타나게 한다"
```

---

### Task 2: 문서를 현재 상태로 맞춘다

**Files:**
- Modify: `docs/status.md`
- Modify: `docs/synthesis/open-questions.md`
- Modify: `docs/log.md`
- Modify: `docs/superpowers/specs/README.md`
- Modify: `docs/superpowers/plans/README.md`
- Modify: `docs/superpowers/specs/2026-10-28-canvas-video-instant-reveal-design.md`

**Interfaces:**
- Consumes: Task 1의 `visibleToppingCount`(앵커 이름)

- [x] **Step 1: `docs/status.md`**

「캔버스 이미지 저장」의 영상 줄: "차례로 팝인하는" → 연출 없이 차례로 나타난다는 서술, 앵커 `toppingPopProgresses` → `visibleToppingCount`. OQ-P-429 요약 줄의 "·팝인 중 띠 잘림"을 지운다.

- [x] **Step 2: `docs/synthesis/open-questions.md` OQ-P-429**

③(팝인 중 알파 < 1로 테두리 띠 잘림)을 지우고 ④⑤⑥을 ③④⑤로 당긴다. 해소 메모의 "①~③" → "①②", "③이 거슬리면 …" 문장을 지운다. 해소 메모의 "④⑤는" → "③④는". 다른 문서가 OQ-P-429 ①만 가리키는 것을 `grep -rn "OQ-P-429" docs` 로 확인한다(①은 번호가 그대로라 고칠 곳이 없어야 한다).

- [x] **Step 3: `docs/log.md` 에 한 줄 덧붙인다**

```markdown
## [2026-10-28] change | 캔버스 영상 토핑 즉시 등장
팝인(0.6배 확대·easeOutBack·페이드) 제거, `toppingPopProgresses` → `visibleToppingCount`(iOS `toppingVisibilities` 와 같은 경계) · OQ-P-429 ③ 소멸
```

- [x] **Step 4: 스펙·계획 README와 스펙 상태**

`docs/superpowers/specs/README.md` 활성 표 맨 위, `docs/superpowers/plans/README.md` 활성 목록에 이 스펙·계획을 한 줄씩 추가한다(형식은 각 표의 기존 행을 따른다). 스펙 frontmatter `status` 를 `in-progress` 로 바꾼다. archive 이동은 develop 머지 뒤라 하지 않는다.

- [x] **Step 5: 링크를 확인한다**

Run: `python3 docs/script/check_links.py docs`
Expected: exit 0

- [x] **Step 6: 커밋한다**

```bash
git add docs
git commit -m "docs: 캔버스 영상 즉시 등장을 상태·미결·기록에 반영한다"
```
