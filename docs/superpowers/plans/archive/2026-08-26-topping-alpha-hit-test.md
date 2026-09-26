---
id: topping-alpha-hit-test
title: 토핑 누끼 모양 터치 판정 Implementation Plan
status: done
archived_reason: PR #388·#390·#389 로 develop 머지(2026-08-27) — 스택 PR 다섯 갈래가 세 머지로 들어왔다
type: work-order
created: 2026-08-26
updated: 2026-08-27
platforms: android
owner: Parfait 팀
related_adr: ADR-0025
related_spec: topping-alpha-hit-test
related_code:
  - ToppingAlphaMask.kt#ToppingAlphaMask
  - ToppingHitTarget.kt#ToppingHitTarget
  - ToppingHitTarget.kt#pickToppingHit
  - ToppingAlphaMaskCache.kt#loadToppingAlphaMask
  - ToppingAlphaMaskCache.kt#rememberToppingAlphaMasks
  - ToppingClickThrottle.kt#ToppingClickThrottle
  - ToppingHitTestInput.kt#toppingTapInput
  - ToppingHitTestInput.kt#toppingDragInput
  - ToppingCorners.kt#ToppingCorners
  - ToppingGeometry.kt#toppingImageSize
  - CanvasToppingLayer.kt#CanvasToppingLayer
  - CanvasToppingLayer.kt#ToppingHitEntry
  - CanvasBGEditScreen.kt#BGEditHitEntry
  - CanvasBGEditScreen.kt#CanvasToppingImage
  - CanvasToppingPlaceScreen.kt
  - YGToppingCutoutImage.kt#TOPPING_OUTLINE_STAMP_COUNT
related_architecture: design-system
tags: [plan, parfait, canvas, topping]
---

# 토핑 누끼 모양 터치 판정 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 캔버스 토핑의 터치 판정을 사각형에서 실제로 보이는 실루엣으로 바꾸고, 겹친 토핑의 투명한 자리를 통과해 아래 토핑이 잡히게 한다.

**Architecture:** 토핑마다 걸린 `clickable`을 걷어내고 `CanvasToppingLayer`가 포인터 입력 하나로 판정을 맡는다. 판정은 저해상도 알파 마스크를 점으로 찍는 순수 함수이고, 테두리는 렌더링이 밀어 찍는 방향과 대칭으로 되민 점을 함께 읽어 처리한다. 마스크는 표시용 이미지와 별개로 한 번 더 디코딩해 비트셋으로 접고 동기화된 LRU에 담는다.

**Tech Stack:** Kotlin, Jetpack Compose (BOM 2026.08.00), Coil 3.5.0, kotlin.test (JVM 유닛)

**Spec:** [`parfait/specs/archive/2026-08-26-topping-alpha-hit-test.md`](../../specs/archive/2026-08-26-topping-alpha-hit-test.md)

## Global Constraints

- **작업 저장소는 `TJYG-Android`다.** 이 계획 문서가 있는 저장소가 아니다.
- **PR 1 의 베이스만 `develop` 이다.** PR 2~5 는 앞 브랜치 위에 쌓는다. 각 태스크의 브랜치 생성
  명령은 **앞 PR 브랜치를 체크아웃한 상태에서** 실행한다.
- **커밋만 한다. `git push`와 PR 생성은 하지 않는다.** 사용자 확인이 필요한 작업이다.
- **주석 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다. 낡는다.
- **테스트는 `kotlin.test`를 쓴다.** 기존 `ToppingGeometryTest`와 같은 스타일이다 — `Given·When`, `Then` 주석과 `assertEquals(expected, actual, DELTA)`.
- **`core/util/android`는 손대지 않는다.** `Modifier.dragBy`도, 스로틀 게이트도 그대로 둔다.
- **렌더링 결과는 배경 편집의 테두리를 빼면 지금과 같아야 한다.** 토핑의 위치·크기가 달라지면 회귀다.
- 마스크 해상도와 알파 임계값은 상수 하나씩으로 두어 나중에 조정할 수 있게 한다.
- **새 의존성을 추가하지 않는다.** 버전 카탈로그와 gradle 파일을 건드리지 않는다.
- 한 줄은 120자를 넘기지 않는다(`.editorconfig`, ktlint `android_studio` 스타일).
- **모디파이어를 걷어낼 때마다 미사용 import 를 함께 지운다.** ktlint 의 `no-unused-imports` 가 켜져 있다.
- **스택 PR 에는 CI 가 돌지 않는다.** ktlint·테스트 워크플로가 베이스 `develop` 으로 필터돼 있어,
  베이스가 feature 브랜치인 PR 2~5 는 검사가 실행되지 않는다. 각 태스크에서 로컬로 돌린다.

## 스택 PR 구성

작업 분량이 커서 다섯 갈래로 나눈다. **뒤 브랜치는 앞 브랜치 위에 쌓는다.** 앞이 머지되면 뒤는 베이스만 `develop`으로 바꾸면 된다.

| PR | 브랜치 | 베이스 | 태스크 | 성격 |
|---|---|---|---|---|
| 1 | `feature/bgedit-topping-border` | `develop` | 1 | 배경 편집 테두리 렌더링. 판정과 무관해 단독으로 의미가 있다 |
| 2 | `feature/topping-hit-test-core` | PR 1 | 2·3·4 | 마스크 자료구조와 판정 순수 함수. 화면 변경 없음, 전부 JVM 유닛으로 덮인다 |
| 3 | `feature/topping-mask-loading` | PR 2 | 5·6 | 마스크 로딩·캐시와 대상별 스로틀. 아직 화면에 붙지 않는다 |
| 4 | `feature/canvas-main-alpha-hit` | PR 3 | 7·8 | 캔버스 메인 판정 전환 |
| 5 | `feature/bgedit-alpha-hit` | PR 4 | 9·10 | 배경 편집 판정·드래그 전환 |

PR 2와 3의 산출물은 그 PR 안에서 화면에 쓰이지 않는다. 스택 PR에서 흔한 형태이고, 유닛 테스트가 그 자리의 검증을 대신한다. 리뷰어에게 "PR 4에서 배선된다"를 PR 설명에 적는다.

## 실행 뒤 달라진 것

이 계획대로 실행한 뒤, 리뷰와 사용자 요청으로 아래를 바꿨다. 계획 본문의 코드 블록은 실행 당시의
것이므로 지금 코드와 다르다 — **현재 형태는 코드가 정본이다.**

| 무엇 | 왜 |
|---|---|
| `ToppingHitTest.kt` → `ToppingHitTarget.kt` | ktlint 파일명 규칙(단일 최상위 타입명과 파일명 일치) |
| `ToppingHitEntry.painter`·`BGEditHitEntry.painter` 타입을 `AsyncImagePainter`로 | `Painter`로 좁히면 `state`를 잃어 테두리 조건을 볼 수 없다. 그대로 갔으면 컴파일이 깨졌다 |
| `toppingAlphaMaskOf`·임계값·`BITS_PER_WORD`를 `ToppingAlphaMask`의 companion으로 | 생성자가 `internal`이라 팩토리가 유일한 공개 생성 경로인데 그 관계가 파일 배치로만 암시됐다. 임계값은 `ALPHA_THRESHOLD`로 줄어 접두사 중복이 사라졌다 |
| `ToppingCorners`를 `impl.model`로 분리 | 프로젝트의 기존 `model` 패키지 관례를 따른다 |
| 드래그에서 `positionChange()`를 읽고 나서 소비하도록 순서 교정 | **계획의 코드가 소비를 먼저 해 이동량이 항상 0이었다.** 최종 리뷰가 잡은 회귀 |
| 탭 판정을 up 좌표에서 **down 좌표**로 | `detectTapGestures`의 `onTap` 인자는 뗀 지점이다. 미끄러져 떼면 미스 분기가 오발했다 |
| 겹침 순서 선택을 `pickToppingHit` 순수 함수로 추출 | 기능 목표 절반인 겹침 통과에 유닛 테스트가 닿지 않았다 |
| 마스크 접기를 기본 디스패처로, 메모리 캐시 끄기, 배경 편집은 내 토핑만 요청 | 메인 스레드 점유와 불필요한 디코딩 |
| `ToppingClickThrottle`의 기본 창 상수를 companion으로 | 클래스의 기본 인자값이라 밖에서는 의미가 없다 |
| `containsPoint`·`isOpaqueAtLocal`을 `ToppingHitTarget` 멤버로, `FULL_TURN_DEGREES`도 companion으로 | 프로젝트가 소유한 타입의 본질적 행위라 확장으로 둘 이유가 없다(`kotlin-functions` 규약) |
| 마스크 칸 번호를 `floor`로 내림 | `Float.toInt()`는 0 쪽으로 버려서 그림 왼쪽·위쪽 밖의 음수 좌표가 0번 칸으로 접혔다. **스펙이 정한 "범위 밖은 불투명 아님"이 그만큼 무력해졌다** — 테두리가 있는 토핑의 왼쪽·위쪽에서만 실루엣 밖이 눌리는 비대칭 결함 |
| 같은 모델의 동시 로드를 진행 중인 것에 합류 | 캐시 조회와 쓰기만 잠금 안이라 두 화면이 같은 토핑을 동시에 요청하면 각자 디코딩·주사했다. 합류자가 먼저 시작한 컴포지션과 함께 죽지 않도록 로드는 전용 스코프에서 돌리고 애플리케이션 컨텍스트를 넘긴다 |
| 스포트라이트 딤에 클릭 시맨틱스 부여 | 딤의 `clickable`을 걷어내면서 그것이 붙여 주던 클릭 시맨틱스도 사라져, C-202의 "강조 토핑 밖 탭 → Default 복귀"가 터치 사용자에게만 남았다. 판정 레이어는 `pointerInput` 뿐이라 대신할 시맨틱스가 없다 |
| 토핑 시맨틱스를 판정 활성 여부로 조건화 | 토핑 배치 화면은 판정을 끄고 클릭 핸들러도 비어 있는데 같은 컴포저블을 써서 버튼으로 안내됐다 |
| 배경 편집 `rememberToppingPainter`의 반환 타입을 `AsyncImagePainter`로 | 테두리 조건 때문에 `painter.state`가 필요해지면서 호출부가 모델 결정 로직을 따로 풀어 썼다. 타입만 좁히면 한 곳으로 다시 모인다. PR 5에서 `drawnModel` 프로퍼티로 대체됐다 |

### 검토했으나 하지 않기로 한 것

**판정 코드를 `domain` 모듈로 올리는 안.** 의존을 걷어내면 기술적으로는 가능하다 —
캐시 자료구조만 떼고, 연타 방어의 시계 기본값을 없애고, 테두리 방향 수를 파라미터로 빼면 된다.
그럼에도 하지 않는다.

- `architecture/module-structure.md`가 같은 사례를 이미 되돌렸다(PR #231 → #334). 기준은 Android
  의존 유무가 아니라 **도메인 규칙이냐 표시 규격이냐**이고, 알파 마스크 판정은 표시 규격이다.
- 테두리 방향 수는 **그리는 쪽이 정본을 갖는 것이 설계 결정**이다. 파라미터로 빼면 판정 모양과
  외형이 갈라질 자리가 열리고, 지금 컴파일이 보증하는 것을 사람 규율로 바꾼다.
- 얻는 것이 없다. `domain`은 `kotlin.jvm` 모듈이라 iOS와 공유되지 않고, 소비자는 캔버스 impl
  하나뿐이며, JVM 유닛 테스트는 이미 feature에서 돈다.

`util` 패키지에 성격이 다른 것들이 섞여 있다는 문제는 남는다. 나눈다면 모듈이 아니라 **feature 안에서**
순수 판정 / 정책 / Compose·Coil 계층으로 가르는 편이 같은 효과에 위험이 없다.

---

## File Structure

**새로 만드는 파일** (전부 `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/` 아래)

| 파일 | 책임 |
|---|---|
| `util/ToppingAlphaMask.kt` | 불투명 픽셀만 접은 비트셋과 좌표 조회. 순수 코틀린 |
| `util/ToppingHitTarget.kt` | 판정 대상 자료구조와 점 판정. 순수 코틀린 (ktlint 파일명 규칙에 맞춰 `ToppingHitTest.kt` 대신 이 이름) |
| `util/ToppingAlphaMaskCache.kt` | Coil로 마스크를 디코딩하고 LRU에 담는다. Android·Coil |
| `util/ToppingClickThrottle.kt` | 대상별 연타 방어 |
| `component/ToppingHitTestInput.kt` | 탭·드래그 포인터 입력 모디파이어 |
| `model/ToppingCorners.kt` | 회전이 반영된 네 꼭짓점. 실행 뒤 `ToppingGeometry.kt`에서 분리했다 |

**고치는 파일**

| 파일 | 무엇을 |
|---|---|
| `component/CanvasToppingLayer.kt` | 판정 주인이 된다. `clickable` 제거, painter를 레이어가 만든다 |
| `screen/CanvasBGEditScreen.kt` | 테두리 렌더링, 딤 클릭 제거, 판정·드래그 레이어로 이동 |
| `util/ToppingGeometry.kt` | 그림 사각형 계산 함수 추가 |
| `core/designsystem/.../ygtoppingcutout/YGToppingCutoutImage.kt` | 방향 수 상수를 `TOPPING_OUTLINE_STAMP_COUNT` 로 개명하며 공개 |
| `screen/CanvasToppingPlaceScreen.kt` | 판정을 달지 않도록 `hitTestEnabled = false` 를 넘긴다 |
| `res/values/strings.xml` | 토핑 접근성 문구 |

**테스트 파일** (`feature/groups/canvas/impl/src/test/kotlin/.../util/`)

- `ToppingAlphaMaskTest.kt`
- `ToppingHitTestTest.kt`
- `ToppingClickThrottleTest.kt`
- `ToppingGeometryTest.kt` (기존 파일에 추가)

---

# PR 1 — 배경 편집 테두리 렌더링

브랜치: `feature/bgedit-topping-border` (베이스 `develop`)

### Task 1: 배경 편집이 토핑 테두리를 그린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt` (`CanvasToppingImage`)

**Interfaces:**
- Consumes: `CanvasToppingItem.borderLayers: List<ToppingBorderLayer>` (이미 존재), `YGToppingCutoutImage(painter, borderColor, borderWidth, modifier)`
- Produces: 없음. 렌더링만 바뀐다

**배경:** `CanvasToppingImage`는 지금 맨 `Image`로 그려 테두리가 나오지 않는다. 캔버스 메인은 `YGToppingCutoutImage`를 쓴다. 두 화면이 같은 그림을 그려야 뒤 PR의 판정 규칙 하나로 덮인다.

`ToppingBorderLayer`는 `colorArgb: Int`·`widthDp: Float`를 갖고, `toBorderLayers()`가 색 파싱에 성공한 `Solid`만 원소로 만든다. 그래서 원소가 있으면 색은 읽혔다는 뜻이다.

⚠️ **조건이 하나 더 있다.** `YGToppingCutoutImage` 의 KDoc 이 "그림이 아직 뜨지 않은 painter 로 찍으면
플레이스홀더 실루엣이 테두리로 보인다. 준비되기 전에는 호출부가 `borderColor` 에 `null` 을 넘긴다"를
계약으로 적어 두었고, 캔버스 메인과 토핑 배치가 그것을 지킨다. 색 파싱만으로는 부족하고
**painter 가 성공 상태인지도 함께 봐야 한다.**

- [ ] **Step 1: 브랜치를 만든다**

```bash
cd <TJYG-Android>
git checkout develop && git pull
git checkout -b feature/bgedit-topping-border
```

- [ ] **Step 2: `CanvasToppingImage`의 그리는 부분을 바꾼다**

`CanvasToppingImage` 안의 `Image(...)` 호출을 아래로 교체한다. 바깥 `Box`의 모디파이어 체인은 건드리지 않는다.

```kotlin
val painterState by painter.state.collectAsState()
val border = topping.borderLayers.firstOrNull()

YGToppingCutoutImage(
    painter = painter,
    // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
    borderColor = border
        ?.let { Color(it.colorArgb) }
        ?.takeIf { painterState is AsyncImagePainter.State.Success },
    borderWidth = (border?.widthDp ?: 0f).dp,
    modifier = Modifier.fillMaxSize(),
)
```

import 를 추가한다. `androidx.compose.ui.graphics.Color` 는 이 파일에 **이미 있으므로** 넣지 않는다.

```kotlin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import coil3.compose.AsyncImagePainter
import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage
```

이미 있는 import 는 다시 넣지 않는다. 쓰지 않게 된 `androidx.compose.foundation.Image` 는 파일 안
다른 곳에서 쓰이는지 확인하고, 안 쓰이면 지운다 — ktlint 의 `no-unused-imports` 가 켜져 있다.

- [ ] **Step 3: 컴파일을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: ktlint 를 돌린다**

Run: `./gradlew :feature:groups:canvas:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 실기기로 눈으로 확인한다**

프리뷰로는 확인할 수 없다 — `PreviewCanvasBGEditScreen` 은 기본 상태를 넘기는데 기본 탭이 배경 탭이고
`toppings` 가 비어 있어 토핑 렌더 분기에 아예 들어가지 않는다.

실기기에서 테두리가 있는 토핑을 올린 캔버스로 배경 편집에 들어가, 실루엣을 따르는 테두리가 보이고
로딩 중에 사각 플레이스홀더 테두리가 번쩍이지 않는지 본다.

- [ ] **Step 6: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt
git commit -m "feat: 배경 편집 캔버스에서 토핑 테두리를 그린다"
```

---

# PR 2 — 판정 핵심 (순수 함수)

브랜치: `feature/topping-hit-test-core` (베이스 PR 1)

### Task 2: 알파 마스크 자료구조

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMask.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMaskTest.kt`

**Interfaces:**
- Produces:
  - `class ToppingAlphaMask(val width: Int, val height: Int, bits: LongArray)`
  - `ToppingAlphaMask.isOpaqueAt(x: Int, y: Int): Boolean` (멤버)
  - `ToppingAlphaMask.hasAnyOpaque: Boolean` (멤버)
  - `fun toppingAlphaMaskOf(width: Int, height: Int, alphaAt: (Int, Int) -> Int): ToppingAlphaMask`
  - `const val TOPPING_MASK_ALPHA_THRESHOLD: Int`

- [ ] **Step 1: 브랜치를 만든다**

```bash
git checkout -b feature/topping-hit-test-core
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`ToppingAlphaMaskTest.kt` 를 만든다.

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingAlphaMaskTest {
    @Test
    fun isOpaqueAt_alphaAboveThreshold_isTrue() {
        // Given 가운데 한 픽셀만 완전 불투명한 3x3 마스크
        val mask = toppingAlphaMaskOf(width = 3, height = 3) { x, y ->
            if (x == 1 && y == 1) 255 else 0
        }

        // Then
        assertTrue(mask.isOpaqueAt(1, 1))
        assertFalse(mask.isOpaqueAt(0, 0))
    }

    @Test
    fun isOpaqueAt_alphaBelowThreshold_isFalse() {
        // Given 임계값 바로 아래로만 채운 마스크 — 다운스케일 잡티를 걸러 내는 자리다
        val mask = toppingAlphaMaskOf(width = 2, height = 2) { _, _ ->
            TOPPING_MASK_ALPHA_THRESHOLD - 1
        }

        // Then
        assertFalse(mask.isOpaqueAt(0, 0))
        assertFalse(mask.hasAnyOpaque)
    }

    @Test
    fun isOpaqueAt_outOfBounds_isFalseNotThrow() {
        // Given 테두리 되밀기 점은 정의상 마스크 밖으로 나간다
        val mask = toppingAlphaMaskOf(width = 2, height = 2) { _, _ -> 255 }

        // Then 예외가 아니라 "불투명 아님"이다
        assertFalse(mask.isOpaqueAt(-1, 0))
        assertFalse(mask.isOpaqueAt(0, -1))
        assertFalse(mask.isOpaqueAt(2, 0))
        assertFalse(mask.isOpaqueAt(0, 2))
    }

    @Test
    fun hasAnyOpaque_allTransparent_isFalse() {
        // Given 불투명 픽셀이 하나도 없는 마스크 — 이런 마스크는 부재로 취급해야 한다
        val mask = toppingAlphaMaskOf(width = 8, height = 8) { _, _ -> 0 }

        // Then
        assertFalse(mask.hasAnyOpaque)
    }

    @Test
    fun bitset_packsMoreThan64Pixels() {
        // Given 64픽셀을 넘겨 LongArray 가 여러 칸이 되는 크기
        val mask = toppingAlphaMaskOf(width = 10, height = 10) { x, y ->
            if (x == 9 && y == 9) 255 else 0
        }

        // Then 마지막 픽셀이 두 번째 Long 칸에 들어가도 제대로 읽힌다
        assertTrue(mask.isOpaqueAt(9, 9))
        assertFalse(mask.isOpaqueAt(8, 9))
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingAlphaMaskTest*"`
Expected: 컴파일 실패 — `toppingAlphaMaskOf` 를 찾을 수 없다

- [ ] **Step 4: 최소 구현을 쓴다**

`ToppingAlphaMask.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 다운스케일은 블록 평균이라, 0보다 크기만 하면 불투명으로 치면 원본 블록에 픽셀 하나만 있어도
 * 마스크가 채워져 실루엣이 한 픽셀만큼 부푼다. 절반을 기준으로 삼아 부풀림과 깎임을 상쇄한다.
 */
const val TOPPING_MASK_ALPHA_THRESHOLD = 128

private const val BITS_PER_WORD = 64

/** 토핑 누끼에서 불투명한 자리만 남긴 저해상도 마스크. 판정에만 쓰고 그리지 않는다. */
class ToppingAlphaMask internal constructor(
    val width: Int,
    val height: Int,
    private val bits: LongArray,
) {
    /** 마스크 밖 좌표는 예외가 아니라 투명으로 답한다 — 테두리 되밀기 점이 정의상 밖으로 나간다. */
    fun isOpaqueAt(
        x: Int,
        y: Int,
    ): Boolean {
        if (x < 0 || y < 0 || x >= width || y >= height) return false
        val index = y * width + x
        return bits[index / BITS_PER_WORD] and (1L shl (index % BITS_PER_WORD)) != 0L
    }

    val hasAnyOpaque: Boolean
        get() = bits.any { it != 0L }
}

fun toppingAlphaMaskOf(
    width: Int,
    height: Int,
    alphaAt: (x: Int, y: Int) -> Int,
): ToppingAlphaMask {
    val bits = LongArray((width * height + BITS_PER_WORD - 1) / BITS_PER_WORD)

    for (y in 0 until height) {
        for (x in 0 until width) {
            if (alphaAt(x, y) >= TOPPING_MASK_ALPHA_THRESHOLD) {
                val index = y * width + x
                bits[index / BITS_PER_WORD] = bits[index / BITS_PER_WORD] or (1L shl (index % BITS_PER_WORD))
            }
        }
    }

    return ToppingAlphaMask(width = width, height = height, bits = bits)
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingAlphaMaskTest*"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMask.kt feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMaskTest.kt
git commit -m "feat: 토핑 알파 마스크 자료구조를 추가한다"
```

---

### Task 3: 그림 사각형 계산

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingGeometry.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingGeometryTest.kt` (기존 파일에 추가)

**Interfaces:**
- Consumes: `toppingLongSide(canvasWidth, scale)` (기존)
- Produces: `fun toppingImageSize(longSide: Dp, aspectRatio: Float): DpSize`

**배경:** 배경 편집의 `rememberToppingSize` 가 지금 이 계산을 컴포저블 안에서 한다. 판정도 같은 사각형을 알아야 하므로 순수 함수로 꺼낸다. 배경 편집은 뒤 태스크에서 이 함수를 쓰도록 바꾼다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`ToppingGeometryTest.kt` 의 클래스 안에 추가한다.

```kotlin
    @Test
    fun toppingImageSize_landscape_longSideIsWidth() {
        // Given 가로가 긴 원본 (2:1)
        val size = toppingImageSize(longSide = 100.dp, aspectRatio = 2f)

        // Then 긴 변이 가로에 붙고 세로만 비율대로 줄어든다
        assertEquals(100f, size.width.value, DELTA)
        assertEquals(50f, size.height.value, DELTA)
    }

    @Test
    fun toppingImageSize_portrait_longSideIsHeight() {
        // Given 세로가 긴 원본 (1:4)
        val size = toppingImageSize(longSide = 100.dp, aspectRatio = 0.25f)

        // Then
        assertEquals(25f, size.width.value, DELTA)
        assertEquals(100f, size.height.value, DELTA)
    }

    @Test
    fun toppingImageSize_square_bothSidesEqual() {
        val size = toppingImageSize(longSide = 60.dp, aspectRatio = 1f)

        assertEquals(60f, size.width.value, DELTA)
        assertEquals(60f, size.height.value, DELTA)
    }

    @Test
    fun toppingImageSize_nonPositiveRatio_fallsBackToSquare() {
        // Given 아직 원본 비율을 모르는 상태 — 정사각으로 두는 것이 현행 동작이다
        val size = toppingImageSize(longSide = 40.dp, aspectRatio = 0f)

        assertEquals(40f, size.width.value, DELTA)
        assertEquals(40f, size.height.value, DELTA)
    }
```

파일 상단 import 에 아래를 추가한다.

```kotlin
import androidx.compose.ui.unit.dp
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingGeometryTest*"`
Expected: 컴파일 실패 — `toppingImageSize` 를 찾을 수 없다

- [ ] **Step 3: 구현을 쓴다**

`ToppingGeometry.kt` 의 `toppingCenter` 아래에 추가한다.

```kotlin
/**
 * 긴 변이 [longSide]가 되도록 원본 비율([aspectRatio] = 가로÷세로)을 편 크기.
 *
 * 비율을 아직 모르면(0 이하) 정사각으로 둔다 — 그림이 뜨기 전에 크기를 지어내면 뜬 뒤에 튄다.
 */
fun toppingImageSize(
    longSide: Dp,
    aspectRatio: Float,
): DpSize = when {
    aspectRatio <= 0f -> DpSize(longSide, longSide)
    aspectRatio >= 1f -> DpSize(longSide, longSide / aspectRatio)
    else -> DpSize(longSide * aspectRatio, longSide)
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingGeometryTest*"`
Expected: PASS (기존 테스트 포함 전부)

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingGeometry.kt feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingGeometryTest.kt
git commit -m "feat: 토핑 그림 사각형 계산을 순수 함수로 꺼낸다"
```

---

### Task 4: 점 판정

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingHitTest.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingHitTestTest.kt`

**Interfaces:**
- Consumes: `ToppingAlphaMask.isOpaqueAt`, `hasAnyOpaque`, `TOPPING_OUTLINE_STAMP_COUNT`
- Produces:
  - `data class ToppingHitTarget(centerXPx, centerYPx, imageWidthPx, imageHeightPx, rotationDegrees, borderWidthPx, mask)`
  - `fun ToppingHitTarget.containsPoint(xPx: Float, yPx: Float): Boolean`

**배경:** 판정 모양을 렌더링과 맞추려면 테두리를 찍는 방향 수를 그리는 쪽에서 읽어야 한다. `OUTLINE_STAMP_COUNT` 는 지금 `private` 이라 공개 이름으로 바꾼다.

- [ ] **Step 1: 디자인시스템의 방향 수를 공개한다**

`YGToppingCutoutImage.kt` 에서 아래처럼 바꾸고, 같은 파일 안의 사용처(`repeat(OUTLINE_STAMP_COUNT)`, `FULL_TURN_DEGREES / OUTLINE_STAMP_COUNT`)도 새 이름으로 고친다.

```kotlin
/**
 * 누끼 외곽선을 찍는 방향 수. 8 방향이면 대각까지 메워져 이음매가 보이지 않는다.
 *
 * 터치 판정이 같은 방향으로 되민 점을 읽어 판정 모양을 외형과 맞추므로, 이 값이 정본이다.
 */
const val TOPPING_OUTLINE_STAMP_COUNT = 8
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`ToppingHitTestTest.kt` 를 만든다.

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 왼쪽 절반만 불투명한 4x4 마스크. 좌우 비대칭이라 좌표 부호 실수가 드러난다. */
private fun leftHalfMask(): ToppingAlphaMask =
    toppingAlphaMaskOf(width = 4, height = 4) { x, _ -> if (x < 2) 255 else 0 }

private fun target(
    rotationDegrees: Float = 0f,
    borderWidthPx: Float = 0f,
    mask: ToppingAlphaMask? = leftHalfMask(),
): ToppingHitTarget = ToppingHitTarget(
    centerXPx = 100f,
    centerYPx = 100f,
    imageWidthPx = 40f,
    imageHeightPx = 40f,
    rotationDegrees = rotationDegrees,
    borderWidthPx = borderWidthPx,
    mask = mask,
)

class ToppingHitTestTest {
    @Test
    fun containsPoint_opaqueSide_isHit() {
        // Given·When 왼쪽 절반(불투명)의 한가운데
        // Then
        assertTrue(target().containsPoint(90f, 100f))
    }

    @Test
    fun containsPoint_transparentSide_isMiss() {
        // Given·When 오른쪽 절반(투명)의 한가운데 — 그림 사각형 안이지만 안 눌려야 한다
        // Then
        assertFalse(target().containsPoint(110f, 100f))
    }

    @Test
    fun containsPoint_outsideImageRect_isMiss() {
        // Given·When 그림 사각형 밖
        // Then
        assertFalse(target().containsPoint(100f, 130f))
    }

    @Test
    fun containsPoint_rotated180_opaqueSideMovesToRight() {
        // Given 180도 돌리면 불투명한 왼쪽 절반이 오른쪽으로 온다
        val rotated = target(rotationDegrees = 180f)

        // Then
        assertTrue(rotated.containsPoint(110f, 100f))
        assertFalse(rotated.containsPoint(90f, 100f))
    }

    @Test
    fun containsPoint_withBorder_extendsBeyondSilhouette() {
        // Given 테두리 8px 인 토핑. 투명한 오른쪽이지만 불투명 경계에서 8px 안쪽이다
        val bordered = target(borderWidthPx = 8f)

        // Then 테두리가 있으면 히트, 없으면 미스다
        assertTrue(bordered.containsPoint(104f, 100f))
        assertFalse(target(borderWidthPx = 0f).containsPoint(104f, 100f))
    }

    @Test
    fun containsPoint_withBorder_farTransparentStillMiss() {
        // Given 테두리 두께보다 훨씬 멀리 떨어진 투명한 자리
        val bordered = target(borderWidthPx = 4f)

        // Then
        assertFalse(bordered.containsPoint(118f, 100f))
    }

    @Test
    fun containsPoint_nullMask_fallsBackToRectangle() {
        // Given 마스크가 아직 없는 토핑
        val noMask = target(mask = null)

        // Then 그림 사각형 안이면 투명한 자리여도 히트다 — 현행 판정과 같다
        assertTrue(noMask.containsPoint(110f, 100f))
        assertFalse(noMask.containsPoint(100f, 130f))
    }

    @Test
    fun containsPoint_emptyMask_fallsBackToRectangle() {
        // Given 불투명 픽셀이 하나도 없는 마스크 — 부재로 봐야 영영 안 눌리는 일이 없다
        val empty = target(mask = toppingAlphaMaskOf(4, 4) { _, _ -> 0 })

        // Then
        assertTrue(empty.containsPoint(110f, 100f))
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingHitTestTest*"`
Expected: 컴파일 실패 — `ToppingHitTarget` 을 찾을 수 없다

- [ ] **Step 4: 구현을 쓴다**

`ToppingHitTest.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.TOPPING_OUTLINE_STAMP_COUNT
import kotlin.math.cos
import kotlin.math.sin

private const val FULL_TURN_DEGREES = 360.0

/**
 * 한 토핑의 판정 대상. 좌표는 모두 레이어 기준 픽셀이다.
 *
 * @param borderWidthPx 테두리 색이 실제로 정해졌을 때만 0 보다 크다. 그리지 않은 테두리만큼
 *   판정이 넓어지면 안 된다.
 * @param mask 아직 없거나 불투명 픽셀이 하나도 없으면 사각형 판정으로 떨어진다.
 */
data class ToppingHitTarget(
    val centerXPx: Float,
    val centerYPx: Float,
    val imageWidthPx: Float,
    val imageHeightPx: Float,
    val rotationDegrees: Float,
    val borderWidthPx: Float,
    val mask: ToppingAlphaMask?,
)

fun ToppingHitTarget.containsPoint(
    xPx: Float,
    yPx: Float,
): Boolean {
    val radians = Math.toRadians(-rotationDegrees.toDouble())
    val cosT = cos(radians).toFloat()
    val sinT = sin(radians).toFloat()

    val dx = xPx - centerXPx
    val dy = yPx - centerYPx
    val localX = dx * cosT - dy * sinT
    val localY = dx * sinT + dy * cosT

    val halfWidth = imageWidthPx / 2f + borderWidthPx
    val halfHeight = imageHeightPx / 2f + borderWidthPx
    if (localX < -halfWidth || localX > halfWidth) return false
    if (localY < -halfHeight || localY > halfHeight) return false

    // 마스크가 없거나 비어 있으면 사각형 판정이다 — 여기까지 왔으면 사각형 안이다
    if (mask?.hasAnyOpaque != true) return true

    if (isOpaqueAtLocal(localX, localY)) return true
    if (borderWidthPx <= 0f) return false

    // 테두리는 원본을 여덟 방향으로 밀어 찍은 것이라, 같은 방향으로 되민 점의 원본 알파를 본다
    return (0 until TOPPING_OUTLINE_STAMP_COUNT).any { index ->
        val stampRadians = Math.toRadians(FULL_TURN_DEGREES / TOPPING_OUTLINE_STAMP_COUNT * index)
        val offsetX = (cos(stampRadians) * borderWidthPx).toFloat()
        val offsetY = (sin(stampRadians) * borderWidthPx).toFloat()
        isOpaqueAtLocal(localX - offsetX, localY - offsetY)
    }
}

/** 그림 사각형 안의 좌표를 마스크 격자로 옮겨 읽는다. 격자 밖은 [ToppingAlphaMask]가 투명으로 답한다. */
private fun ToppingHitTarget.isOpaqueAtLocal(
    localX: Float,
    localY: Float,
): Boolean {
    val usableMask = mask ?: return false
    val maskX = ((localX + imageWidthPx / 2f) * usableMask.width / imageWidthPx).toInt()
    val maskY = ((localY + imageHeightPx / 2f) * usableMask.height / imageHeightPx).toInt()
    return usableMask.isOpaqueAt(maskX, maskY)
}
```

⚠️ `localX + imageWidthPx / 2f` 가 음수이면 `toInt()` 가 0 쪽으로 잘라 `-0.5` 도 `0` 이 된다.
그 자리는 마스크 격자 밖이므로 왼쪽·위 가장자리 한 줄이 살짝 넓게 잡힌다. [정확도 한계] 안이라
그대로 둔다.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingHitTestTest*"`
Expected: PASS

- [ ] **Step 6: 디자인시스템 컴파일을 확인한다**

Run: `./gradlew :core:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingHitTest.kt feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingHitTestTest.kt core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt
git commit -m "feat: 토핑 알파 점 판정을 추가한다"
```

---

# PR 3 — 마스크 로딩과 연타 방어

브랜치: `feature/topping-mask-loading` (베이스 PR 2)

### Task 5: 마스크 로딩과 캐시

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMaskCache.kt`

**Interfaces:**
- Consumes: `toppingAlphaMaskOf`, `ToppingAlphaMask`
- Produces:
  - `suspend fun loadToppingAlphaMask(context: Context, model: String): ToppingAlphaMask?`
  - `fun clearToppingAlphaMasks()`
  - `@Composable fun rememberToppingAlphaMasks(models: List<String>): Map<String, ToppingAlphaMask>`
    — **빈 목록을 넘기면 아무것도 로드하지 않는다.** 배치 화면이 이 성질로 로딩을 끈다.

**배경:** 표시용 이미지는 하드웨어 비트맵이라 픽셀을 읽을 수 없다. 마스크 전용으로 한 번 더, 소프트웨어 비트맵으로 작게 받는다. 원본은 디스크 캐시에서 재사용되므로 네트워크 요청은 늘지 않는다.

유닛 테스트를 붙이지 않는다. Coil 과 Android `Bitmap` 에 붙어 있어 JVM 유닛으로 의미 있게 덮이지 않고, 판정 로직은 Task 2·4 가 이미 덮었다.

- [ ] **Step 1: 브랜치를 만든다**

```bash
git checkout -b feature/topping-mask-loading
```

- [ ] **Step 2: 구현을 쓴다**

`ToppingAlphaMaskCache.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap

/** 마스크 한 장의 긴 변. 올리면 판정이 외형에 가까워지고 디코딩·메모리가 는다. */
private const val MASK_LONG_SIDE = 256

/** 캔버스 하나에 올라가는 토핑 수를 넉넉히 덮는 상한. */
private const val MASK_CACHE_ENTRIES = 64

private const val LOAD_FACTOR = 0.75f

/**
 * 접근 순서 갱신이 곧 쓰기라 잠금 없이 건드리면 상태가 깨진다 — 디코딩은 백그라운드에서 끝나고
 * 읽기는 메인 스레드다. 모든 접근을 [maskCache] 자신에 대해 동기화한다.
 */
private val maskCache = object : LinkedHashMap<String, ToppingAlphaMask>(
    MASK_CACHE_ENTRIES,
    LOAD_FACTOR,
    true,
) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ToppingAlphaMask>): Boolean =
        size > MASK_CACHE_ENTRIES
}

/**
 * [model] 은 그 화면이 **실제로 그리는** 대상이어야 한다. 배경 편집은 편집본 로컬 경로를 그리는데,
 * 그 파일은 투명 여백이 잘려 있어 원본과 비율이 다르다.
 */
suspend fun loadToppingAlphaMask(
    context: Context,
    model: String,
): ToppingAlphaMask? {
    synchronized(maskCache) { maskCache[model] }?.let { return it }

    val request = ImageRequest.Builder(context)
        .data(model)
        .size(MASK_LONG_SIDE)
        .allowHardware(false)
        .build()

    val image = (context.imageLoader.execute(request) as? SuccessResult)?.image ?: return null
    val bitmap = image.toBitmap()

    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val mask = toppingAlphaMaskOf(width = bitmap.width, height = bitmap.height) { x, y ->
        pixels[y * bitmap.width + x] ushr 24
    }

    synchronized(maskCache) { maskCache.put(model, mask) }
    return mask
}

/** 메모리 압박이나 테스트에서 캐시를 비우는 수단. 지금은 호출부가 없다. */
fun clearToppingAlphaMasks() {
    synchronized(maskCache) { maskCache.clear() }
}

/** [models] 가 비면 아무것도 로드하지 않는다 — 판정을 쓰지 않는 화면이 로딩을 끄는 방법이다. */
@Composable
fun rememberToppingAlphaMasks(models: List<String>): Map<String, ToppingAlphaMask> {
    val context = LocalContext.current
    val loaded = remember { mutableStateMapOf<String, ToppingAlphaMask>() }

    LaunchedEffect(models) {
        models.distinct()
            .filterNot { loaded.containsKey(it) }
            .forEach { model -> loadToppingAlphaMask(context, model)?.let { loaded[model] = it } }
    }

    return loaded
}
```

- [ ] **Step 3: 컴파일을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

새 의존성이 필요 없다. 캐시는 표준 라이브러리의 `LinkedHashMap` 접근 순서 모드로 만들고 잠금을
직접 건다. gradle 파일과 버전 카탈로그를 건드리지 않는다.

- [ ] **Step 4: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingAlphaMaskCache.kt
git commit -m "feat: 토핑 알파 마스크 로딩과 캐시를 추가한다"
```

---

### Task 6: 대상별 연타 방어

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingClickThrottle.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingClickThrottleTest.kt`

**Interfaces:**
- Produces:
  - `class ToppingClickThrottle(windowMillis: Long = 300L, now: () -> Long)`
  - `fun ToppingClickThrottle.tryPass(key: Any): Boolean`

**배경:** 지금 300ms 스로틀은 clickable 모디파이어마다 하나씩이라 토핑마다·딤마다 따로다. 레이어에 게이트 하나만 두면 토핑을 눌러 스포트라이트를 켠 뒤 곧바로 바깥을 눌렀을 때 해제가 씹힌다. 대상이 바뀌면 즉시 통과시켜야 한다.

시각을 주입받아 JVM 유닛으로 덮는다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingClickThrottleTest {
    @Test
    fun tryPass_sameKeyWithinWindow_isBlocked() {
        // Given 시각을 손으로 미는 게이트
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }

        // When 같은 대상을 창 안에서 두 번 누른다
        assertTrue(throttle.tryPass("a"))
        now = 100L

        // Then 두 번째는 막힌다
        assertFalse(throttle.tryPass("a"))
    }

    @Test
    fun tryPass_sameKeyAfterWindow_passes() {
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }

        assertTrue(throttle.tryPass("a"))
        now = 300L

        assertTrue(throttle.tryPass("a"))
    }

    @Test
    fun tryPass_differentKeyWithinWindow_passes() {
        // Given 토핑을 눌러 스포트라이트를 켠 직후
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }
        assertTrue(throttle.tryPass("topping"))
        now = 50L

        // When 곧바로 바깥을 누른다
        // Then 대상이 다르므로 즉시 통과한다 — 해제가 씹히면 안 된다
        assertTrue(throttle.tryPass("dim"))
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingClickThrottleTest*"`
Expected: 컴파일 실패 — `ToppingClickThrottle` 을 찾을 수 없다

- [ ] **Step 3: 구현을 쓴다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.os.SystemClock

/** `clickableYGNoRipple` 과 같은 창 길이. 판정을 레이어로 옮기면서 이 방어도 함께 옮겨 온다. */
private const val DEFAULT_WINDOW_MILLIS = 300L

/**
 * 대상이 바뀌면 즉시 통과시키고, 같은 대상을 다시 누를 때만 창을 적용한다.
 *
 * 대상 개념 없이 게이트 하나로 막으면 토핑을 누른 직후의 바깥 탭까지 씹힌다.
 */
class ToppingClickThrottle(
    private val windowMillis: Long = DEFAULT_WINDOW_MILLIS,
    private val now: () -> Long = SystemClock::elapsedRealtime,
) {
    private var lastKey: Any? = null
    private var lastAt = 0L

    fun tryPass(key: Any): Boolean {
        val at = now()
        if (key == lastKey && at - lastAt < windowMillis) return false

        lastKey = key
        lastAt = at
        return true
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingClickThrottleTest*"`
Expected: PASS

`SystemClock` 은 JVM 유닛에서 스텁이라 **호출하면** 예외를 던진다. 기본 인자 표현식은 그 분기를 실행할
때만 평가되고 테스트는 시각을 전부 주입하므로, 클래스 로딩과 테스트 실행 모두 문제가 없다.

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingClickThrottle.kt feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingClickThrottleTest.kt
git commit -m "feat: 대상별 토핑 연타 방어를 추가한다"
```

---

# PR 4 — 캔버스 메인 판정 전환

브랜치: `feature/canvas-main-alpha-hit` (베이스 PR 3)

### Task 7: 레이어가 판정 대상을 만든다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`

**Interfaces:**
- Consumes: `ToppingHitTarget`, `rememberToppingAlphaMasks`, `toppingImageSize`, `toppingCenter`, `toppingLongSide`
- Produces: `internal data class ToppingHitEntry(val topping: CanvasToppingVO, val painter: AsyncImagePainter, val target: ToppingHitTarget)`
  — 타입이 `Painter` 가 아니라 **`AsyncImagePainter`** 다. `Painter` 에는 `state` 가 없어 테두리 조건을
  볼 수 없다.

**배경:** 판정에 필요한 비율은 painter 의 고유 크기에서 나오는데, 지금은 그 값이 자식 컴포저블 안에 갇혀 있다. painter 를 레이어에서 만들어 자식에게 넘기면 비율도 얻고 이미지도 한 번만 로드한다.

`loadMasks` 를 파라미터로 두는 이유는 배치 화면 때문이다. 그 화면은 클릭을 받지 않으므로 쓰지도 않을 마스크를 디코딩하면 안 된다.

- [ ] **Step 1: 브랜치를 만든다**

앞 PR 브랜치(`feature/topping-mask-loading`)에 있는 상태에서 실행한다.

```bash
git checkout -b feature/canvas-main-alpha-hit
```

- [ ] **Step 2: 판정 대상을 만드는 컴포저블을 추가한다**

`CanvasToppingLayer.kt` 안에 더한다.

```kotlin
internal data class ToppingHitEntry(
    val topping: CanvasToppingVO,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val target: ToppingHitTarget,
)

/**
 * 그리기와 판정이 같은 painter 를 본다. 각각 만들면 비율이 서로 다른 시점의 값이 될 수 있다.
 *
 * @param loadMasks 클릭을 받지 않는 화면은 꺼서 쓰지도 않을 디코딩을 막는다
 */
@Composable
private fun rememberToppingHitEntries(
    toppings: List<CanvasToppingVO>,
    canvasWidth: Dp,
    canvasHeight: Dp,
    loadMasks: Boolean,
): List<ToppingHitEntry> {
    val masks = rememberToppingAlphaMasks(
        if (loadMasks) toppings.map { it.imageUrl } else emptyList(),
    )
    val density = LocalDensity.current

    return toppings.map { topping ->
        key(topping.parfaitImageId.value) {
            val painter = rememberAsyncImagePainter(
                model = topping.imageUrl,
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()
            val intrinsicSize = painter.intrinsicSize

            val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
                intrinsicSize.width / intrinsicSize.height
            } else {
                0f
            }

            val longSide = toppingLongSide(canvasWidth, topping.transform.scale.toFloat())
            val imageSize = toppingImageSize(longSide = longSide, aspectRatio = aspectRatio)
            val center = toppingCenter(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                positionX = topping.transform.positionX.toFloat(),
                positionY = topping.transform.positionY.toFloat(),
            )

            // 테두리는 색을 못 읽거나 그림이 안 떴으면 그려지지 않는다 — 판정도 같은 조건이어야 한다
            val drawnBorderWidth = (topping.border as? ToppingBorder.Solid)
                ?.takeIf { it.color.toColorOrNull() != null }
                ?.takeIf { painterState is AsyncImagePainter.State.Success }
                ?.width
                ?.toFloat()
                ?: 0f

            ToppingHitEntry(
                topping = topping,
                painter = painter,
                target = with(density) {
                    ToppingHitTarget(
                        centerXPx = center.x.toPx(),
                        centerYPx = center.y.toPx(),
                        imageWidthPx = imageSize.width.toPx(),
                        imageHeightPx = imageSize.height.toPx(),
                        rotationDegrees = topping.transform.rotation.toFloat(),
                        borderWidthPx = drawnBorderWidth.dp.toPx(),
                        mask = masks[topping.imageUrl],
                    )
                },
            )
        }
    }
}
```

import 를 더한다. `toColorOrNull` 은 이 파일에 **이미 있으므로** 넣지 않는다.

```kotlin
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.platform.LocalDensity
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberToppingAlphaMasks
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingImageSize
```

- [ ] **Step 3: 컴파일을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

이 단계에서는 `rememberToppingHitEntries` 가 아직 호출되지 않아 **미사용 경고가 난다. 정상이다** —
Task 8 이 배선한다. `allWarningsAsErrors` 설정이 없어 빌드는 통과한다.

- [ ] **Step 4: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt
git commit -m "refactor: 토핑 판정 대상을 레이어에서 만든다"
```

---

### Task 8: 캔버스 메인이 알파 판정으로 탭을 받는다

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/ToppingHitTestInput.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasToppingPlaceScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `ToppingHitEntry`, `ToppingHitTarget`, `containsPoint`, `ToppingClickThrottle`
- Produces:
  - `@Composable fun <T> Modifier.toppingTapInput(entries: () -> List<Pair<T, ToppingHitTarget>>, keyOf: (T) -> Any, onHit: (T) -> Unit, onMiss: () -> Unit): Modifier`
  - `CanvasToppingLayer(..., hitTestEnabled: Boolean = true)`

**배경:** 미스일 때 이벤트를 아래로 흘려보내는 것은 Compose 에서 성립하지 않는다. 레이어가 캔버스 영역의 포인터를 독점하고, 미스 동작을 스스로 정의한다.

⚠️ **포인터 입력 핸들러는 참조로 비교된다.** `SuspendPointerInputElement.equals` 의 마지막 줄이
`pointerInputEventHandler === other.pointerInputEventHandler` 다. 무언가를 캡처하는 람다는 리컴포지션마다
새 인스턴스가 되므로, 그대로 넘기면 **진행 중인 제스처가 매번 리셋된다.** 핸들러를 `remember` 로
안정화하고, 그 안에서 읽는 값은 `rememberUpdatedState` 로 최신화한다.

- [ ] **Step 1: 접근성 문구를 문자열 리소스에 넣는다**

`feature/groups/canvas/impl/src/main/res/values/strings.xml` 의 `<resources>` 안에 더한다.

```xml
<!-- TODO(접근성): 토핑을 무엇으로 읽어 줄지 정해지지 않았다(스펙 미결) -->
<string name="canvas_topping_content_description">토핑</string>
```

- [ ] **Step 2: 탭 입력 모디파이어를 만든다**

`ToppingHitTestInput.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputEventHandler
import androidx.compose.ui.input.pointer.pointerInput
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingClickThrottle
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.containsPoint

private const val MISS_KEY = "miss"

/**
 * 겹친 것부터 훑어 처음 맞는 것을 고른다. 아무것도 맞지 않으면 [onMiss] 다 —
 * 이벤트를 아래로 흘려보내지는 않는다. 레이어가 캔버스 영역의 포인터를 독점한다.
 *
 * 핸들러를 [remember] 로 붙잡는 이유는 포인터 입력이 핸들러를 참조로 비교하기 때문이다.
 * 매번 새 람다를 넘기면 진행 중인 제스처가 리셋된다.
 *
 * @param entries 겹침 순서가 **아래에서 위**인 목록. 그리는 순서 그대로 넘기면 된다.
 * @param keyOf 연타 방어가 "같은 대상"을 가리는 기준
 */
@Composable
internal fun <T> Modifier.toppingTapInput(
    entries: () -> List<Pair<T, ToppingHitTarget>>,
    keyOf: (T) -> Any,
    onHit: (T) -> Unit,
    onMiss: () -> Unit,
): Modifier {
    val latestEntries by rememberUpdatedState(entries)
    val latestKeyOf by rememberUpdatedState(keyOf)
    val latestOnHit by rememberUpdatedState(onHit)
    val latestOnMiss by rememberUpdatedState(onMiss)
    val throttle = remember { ToppingClickThrottle() }

    val handler = remember {
        PointerInputEventHandler {
            detectTapGestures { offset ->
                val hit = latestEntries()
                    .asReversed()
                    .firstOrNull { (_, target) -> target.containsPoint(offset.x, offset.y) }

                if (throttle.tryPass(hit?.let { latestKeyOf(it.first) } ?: MISS_KEY)) {
                    hit?.let { latestOnHit(it.first) } ?: latestOnMiss()
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
```

- [ ] **Step 3: 레이어를 다시 배선한다**

`CanvasToppingLayer` 본문을 아래로 바꾼다.

```kotlin
@Composable
internal fun CanvasToppingLayer(
    toppings: List<CanvasToppingVO>,
    spotlightedToppingId: ParfaitImageId?,
    onClickTopping: (CanvasToppingVO) -> Unit,
    onClickSpotlightDim: () -> Unit,
    modifier: Modifier = Modifier,
    hitTestEnabled: Boolean = true,
) {
    BoxWithConstraints(modifier = modifier) {
        val entries = rememberToppingHitEntries(
            toppings = toppings,
            canvasWidth = maxWidth,
            canvasHeight = maxHeight,
            loadMasks = hitTestEnabled,
        )
        val spotlighted = entries.firstOrNull { it.topping.parfaitImageId == spotlightedToppingId }

        entries.forEach { entry ->
            if (entry.topping.parfaitImageId != spotlightedToppingId) {
                CanvasTopping(
                    entry = entry,
                    canvasWidth = maxWidth,
                    canvasHeight = maxHeight,
                    onClick = { onClickTopping(entry.topping) },
                )
            }
        }

        if (spotlighted != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(YGAtomicColors.Transparency.Black50),
            )

            CanvasTopping(
                entry = spotlighted,
                canvasWidth = maxWidth,
                canvasHeight = maxHeight,
                onClick = { onClickTopping(spotlighted.topping) },
            )
        }

        if (hitTestEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .toppingTapInput(
                        // 강조된 토핑이 있으면 그것만 본다. 딤이 전면을 덮어 나머지는 닿지 않는다
                        entries = {
                            (spotlighted?.let(::listOf) ?: entries).map { it.topping to it.target }
                        },
                        keyOf = { it.parfaitImageId },
                        onHit = onClickTopping,
                        onMiss = { if (spotlighted != null) onClickSpotlightDim() },
                    ),
            )
        }
    }
}
```

- [ ] **Step 4: 토핑 컴포저블에서 클릭을 떼고 시맨틱스를 남긴다**

`clickableYGNoRipple` 을 떼면 접근성 트리에서 토핑이 사라진다. 병합·역할·설명에 더해
**클릭 액션까지** 넣어야 접근성 서비스로 토핑을 누를 수 있다.

```kotlin
@Composable
private fun CanvasTopping(
    entry: ToppingHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClick: () -> Unit,
) {
    val transform = entry.topping.transform
    val side = toppingLongSide(canvasWidth = canvasWidth, scale = transform.scale.toFloat())
    val description = stringResource(R.string.canvas_topping_content_description)

    Box(
        modifier = Modifier
            .centeredAt(
                toppingCenter(
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    positionX = transform.positionX.toFloat(),
                    positionY = transform.positionY.toFloat(),
                ),
            )
            // size 는 부모 constraints 로 clamp 돼 토핑이 잘리는 대신 작아진다 — requiredSize 를 쓴다
            .requiredSize(side)
            .graphicsLayer { rotationZ = transform.rotation.toFloat() }
            // 판정은 레이어가 하지만, 접근성 서비스에는 토핑이 개별 버튼으로 보여야 한다
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick {
                    onClick()
                    true
                }
            },
    ) {
        ToppingImage(
            painter = entry.painter,
            border = entry.topping.border,
        )
    }
}
```

`ToppingImage` 는 painter 를 받도록 바꾸고 스스로 만들던 부분을 지운다.

```kotlin
@Composable
private fun ToppingImage(
    painter: AsyncImagePainter,
    border: ToppingBorder,
) {
    val painterState by painter.state.collectAsState()
    val solidBorder = border as? ToppingBorder.Solid

    YGToppingCutoutImage(
        painter = painter,
        // 색을 못 읽으면 테두리를 걸러 낸다 — 임의의 색을 골라 칠하는 것보다 안 그리는 편이 덜 틀리다.
        // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
        borderColor = solidBorder
            ?.color
            ?.toColorOrNull()
            ?.takeIf { painterState is AsyncImagePainter.State.Success },
        borderWidth = (solidBorder?.width?.toFloat() ?: 0f).dp,
        modifier = Modifier.fillMaxSize(),
    )
}
```

import 를 더한다.

```kotlin
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.feature.groups.canvas.impl.R
```

**미사용이 된 import 를 지운다**: `com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple`,
`coil3.compose.rememberAsyncImagePainter`(레이어 쪽으로 옮겨 갔으면 남는지 확인),
`androidx.compose.ui.layout.ContentScale`(같음).

- [ ] **Step 5: 배치 화면이 판정을 달지 않게 한다**

`CanvasToppingPlaceScreen.kt` 의 `CanvasToppingLayer` 호출에 `hitTestEnabled = false` 를 넘긴다.
클릭을 받지 않는 화면이라 마스크 디코딩과 이벤트 싱크가 생기면 안 된다.

```kotlin
CanvasToppingLayer(
    toppings = uiState.existingToppings,
    spotlightedToppingId = null,
    onClickTopping = {},
    onClickSpotlightDim = {},
    hitTestEnabled = false,
    modifier = Modifier.fillMaxSize(),
)
```

- [ ] **Step 6: 컴파일·테스트·ktlint 를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:ktlintCheck`
Expected: 전부 BUILD SUCCESSFUL

- [ ] **Step 7: 실기기로 확인한다**

캔버스 메인에서 아래를 확인한다.

1. 토핑의 그림 부분을 누르면 스포트라이트가 뜬다.
2. 토핑의 **투명한 여백**을 누르면 아무 일도 없다(스포트라이트가 아닌 상태).
3. 겹친 두 토핑에서 위 토핑의 투명한 자리를 누르면 아래 토핑이 잡힌다.
4. 테두리가 있는 토핑은 테두리 위를 눌러도 잡힌다.
5. 스포트라이트 중 바깥을 누르면 해제되고, 토핑을 누른 직후 바로 눌러도 해제가 씹히지 않는다.
6. 메뉴나 달력을 열면 지금처럼 캔버스 탭이 그 딤으로 간다.
7. 토핑 배치 화면에서 배치 중인 토핑의 드래그·크기조절·회전이 그대로 동작한다.

- [ ] **Step 8: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/ feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasToppingPlaceScreen.kt feature/groups/canvas/impl/src/main/res/values/strings.xml
git commit -m "feat: 캔버스 메인 토핑을 누끼 모양으로 판정한다"
```

---

# PR 5 — 배경 편집 판정 전환

브랜치: `feature/bgedit-alpha-hit` (베이스 PR 4)

### Task 9: 배경 편집이 판정 대상을 만들고 딤 클릭을 버린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: `ToppingHitTarget`, `rememberToppingAlphaMasks`, `toppingImageSize`
- Produces: `private data class BGEditHitEntry(val topping: CanvasToppingItem, val painter: AsyncImagePainter, val target: ToppingHitTarget)`
  — 타입이 `Painter` 가 아니라 **`AsyncImagePainter`** 다. 기존 `rememberToppingPainter` 는 반환 타입이
  `Painter` 라 `state` 를 잃는다. 그 함수는 이 태스크에서 지운다.

**배경:** 딤에 클릭이 걸린 채로 두면 딤이 먼저 down 을 소비해 **레이어 판정이 한 번도 실행되지 않는다.**
"남의 토핑 영역 탭 = 선택 해제"는 딤이 아니라 레이어의 미스 분기가 만든다.

배경 편집이 그리는 대상은 `editedImagePath ?: imageUrl` 이다. 그 로컬 파일은 투명 여백이 잘려 있어
원본과 비율이 다르므로, 마스크도 같은 대상으로 받아야 한다.

⚠️ **선택 스트로크와 코너 버튼도 같은 크기를 봐야 한다.** 지금은 `ToppingCornerButtons` 가
`rememberToppingPainter`·`rememberToppingSize` 를 따로 불러 크기를 다시 계산한다. 그대로 두면 그림과
스트로크가 서로 다른 시점의 고유 크기를 보게 되고, 두 함수를 지우면 컴파일이 깨진다.

- [ ] **Step 1: 브랜치를 만든다**

앞 PR 브랜치(`feature/canvas-main-alpha-hit`)에 있는 상태에서 실행한다.

```bash
git checkout -b feature/bgedit-alpha-hit
```

- [ ] **Step 2: 판정 대상을 만드는 컴포저블을 추가한다**

`CanvasBGEditScreen.kt` 에 더한다.

```kotlin
private data class BGEditHitEntry(
    val topping: CanvasToppingItem,
    // Painter 로 좁히면 state 를 잃어 테두리 조건을 볼 수 없다
    val painter: AsyncImagePainter,
    val target: ToppingHitTarget,
)

/** 배경 편집이 그리는 대상. 편집본이 있으면 그쪽이고, 그 파일은 투명 여백이 잘려 있다. */
private val CanvasToppingItem.drawnModel: String
    get() = editedImagePath ?: imageUrl

@Composable
private fun rememberBGEditHitEntries(
    toppings: List<CanvasToppingItem>,
    canvasWidth: Dp,
    canvasHeight: Dp,
): List<BGEditHitEntry> {
    val masks = rememberToppingAlphaMasks(toppings.map { it.drawnModel })
    val density = LocalDensity.current

    return toppings.map { topping ->
        key(topping.parfaitImageId) {
            val painter = rememberAsyncImagePainter(model = topping.drawnModel)
            val painterState by painter.state.collectAsState()
            val intrinsicSize = painter.intrinsicSize

            val aspectRatio = if (intrinsicSize.isSpecified && intrinsicSize.height > 0f) {
                intrinsicSize.width / intrinsicSize.height
            } else {
                0f
            }

            val imageSize = toppingImageSize(
                longSide = toppingLongSide(canvasWidth, topping.scale),
                aspectRatio = aspectRatio,
            )
            val center = toppingCenter(
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                positionX = topping.positionX,
                positionY = topping.positionY,
            )

            // 캔버스 메인과 같은 조건이다 — 그림이 뜨기 전에는 테두리를 그리지도, 판정에 넣지도 않는다
            val drawnBorderWidth = topping.borderLayers
                .firstOrNull()
                ?.takeIf { painterState is AsyncImagePainter.State.Success }
                ?.widthDp
                ?: 0f

            BGEditHitEntry(
                topping = topping,
                painter = painter,
                target = with(density) {
                    ToppingHitTarget(
                        centerXPx = center.x.toPx(),
                        centerYPx = center.y.toPx(),
                        imageWidthPx = imageSize.width.toPx(),
                        imageHeightPx = imageSize.height.toPx(),
                        rotationDegrees = topping.rotationDegrees,
                        borderWidthPx = drawnBorderWidth.dp.toPx(),
                        mask = masks[topping.drawnModel],
                    )
                },
            )
        }
    }
}
```

import 를 더한다. `isSpecified`·`LocalDensity`·`DpSize`·`Offset`·`Color`·`AsyncImagePainter` 는 이 파일에
**이미 있으므로** 넣지 않는다(`AsyncImagePainter` 는 Task 1 이 넣었다). `Painter` 는 `rememberToppingPainter`
를 지우면 미사용이 될 수 있으니 확인하고 정리한다.

```kotlin
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.util.ToppingHitTarget
import com.teamyg.parfait.feature.groups.canvas.impl.util.rememberToppingAlphaMasks
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingImageSize
```

⚠️ `toppingTapInput`·`toppingDragInput`·`containsPoint` 는 **여기서 import 하지 않는다.** 이 태스크에서는
아직 쓰지 않아 ktlint 의 `no-unused-imports` 에 걸린다. Task 10 이 함께 넣는다.

- [ ] **Step 3: 캔버스 안에서 판정 대상을 만들어 둔다**

캔버스를 그리는 `BoxWithConstraints` 안, `canvasHeightPx` 를 선언하는 줄 **바로 다음**에 넣는다.

```kotlin
val entries = rememberBGEditHitEntries(
    toppings = uiState.toppings,
    canvasWidth = canvasWidth,
    canvasHeight = canvasHeight,
)
val myEntries = entries.filter { it.topping.isMine }
val selectedEntry = myEntries.firstOrNull { it.topping.parfaitImageId == uiState.selectedToppingId }
```

판정 대상은 **전체 토핑**으로 만든다. 남의 토핑도 그리려면 painter 가 필요하기 때문이다.

- [ ] **Step 4: 두 반복문이 `entries` 를 돌게 한다**

지금 구조는 `남의 토핑 반복문 → 딤 → 내 토핑 반복문 → 코너 버튼` 순서다. **필터를 유지한 채로**
도는 대상만 바꾼다. 필터를 빼면 모든 토핑이 딤 위아래로 두 번 그려진다.

```kotlin
// 남의 토핑
entries.filterNot { it.topping.isMine }.forEach { entry ->
    CanvasToppingImage(
        entry = entry,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        onClick = onClickDeselectTopping,
    )
}

// (딤)

// 내 토핑
myEntries.forEach { entry ->
    CanvasToppingImage(
        entry = entry,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        onClick = { onClickTopping(entry.topping) },
    )
}
```

`onDrag` 인자는 사라진다. 포인터 판정은 Task 10 이 다는 입력 레이어가 맡고, `onClick` 은
**접근성 시맨틱스 액션 전용**이다 — 화면과 같은 동작을 접근성 서비스에도 열어 둔다.

- [ ] **Step 5: `CanvasToppingImage` 를 바꾼다**

`rememberToppingPainter` 와 `rememberToppingSize` 를 지우고, 크기는 판정 대상이 이미 계산한 값을 쓴다.

```kotlin
@Composable
private fun CanvasToppingImage(
    entry: BGEditHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val size = with(density) {
        DpSize(entry.target.imageWidthPx.toDp(), entry.target.imageHeightPx.toDp())
    }
    val painterState by entry.painter.state.collectAsState()
    val border = entry.topping.borderLayers.firstOrNull()
    val description = stringResource(R.string.canvas_topping_content_description)

    Box(
        modifier = modifier
            .centeredAt(
                toppingCenter(
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    positionX = entry.topping.positionX,
                    positionY = entry.topping.positionY,
                ),
            ).requiredSize(size)
            .graphicsLayer(rotationZ = entry.topping.rotationDegrees)
            // 판정은 입력 레이어가 하지만, 접근성 서비스에는 토핑이 개별 버튼으로 보여야 한다
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = description
                onClick {
                    onClick()
                    true
                }
            },
    ) {
        YGToppingCutoutImage(
            painter = entry.painter,
            borderColor = border
                ?.let { Color(it.colorArgb) }
                ?.takeIf { painterState is AsyncImagePainter.State.Success },
            borderWidth = (border?.widthDp ?: 0f).dp,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

- [ ] **Step 6: 코너 버튼도 같은 크기를 보게 한다**

`ToppingCornerButtons` 가 painter 와 크기를 스스로 만들던 부분을 지우고 `BGEditHitEntry` 를 받는다.
스트로크·버튼 좌표 계산(`computeToppingStrokeCorners`·`computeToppingButtonPoints`·
`ToppingSelectionStroke`)은 그대로 두고, `sizeAfterScale` 만 판정 대상에서 가져온다.

```kotlin
@Composable
private fun ToppingCornerButtons(
    entry: BGEditHitEntry,
    canvasWidth: Dp,
    canvasHeight: Dp,
    // 나머지 파라미터(콜백 등)는 지금 것을 그대로 둔다
) {
    val density = LocalDensity.current
    val sizeAfterScale = with(density) {
        DpSize(entry.target.imageWidthPx.toDp(), entry.target.imageHeightPx.toDp())
    }
    // 이 아래는 기존 코드 그대로다 — center·corners·buttonPoints 계산과 배치
}
```

호출부도 바꾼다.

```kotlin
selectedEntry?.let { entry ->
    ToppingCornerButtons(
        entry = entry,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        // 나머지 인자는 그대로
    )
}
```

- [ ] **Step 7: 딤에서 클릭을 걷어낸다**

캔버스 안 딤 `Box` 에서 `clickableYGNoRipple(...)` 과 그 `interactionSource` 를 지운다. 배경만 남는다.

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(YGAtomicColors.Transparency.Black25),
)
```

- [ ] **Step 8: 미사용 import 를 지우고 확인한다**

`dragBy`·`MutableInteractionSource`·`remember`·`clickableYGNoRipple` 이 파일 안 다른 곳에서 쓰이는지
확인하고, 안 쓰이면 지운다.

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:ktlintCheck`
Expected: BUILD SUCCESSFUL

이 단계에서는 아직 탭·드래그 입력이 없어 **배경 편집에서 토핑을 선택할 수 없다. 정상이다** —
Task 10 이 배선한다. 이 상태로 커밋하되, 실기기 확인은 Task 10 뒤에 한다.

- [ ] **Step 9: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt
git commit -m "refactor: 배경 편집 토핑 판정 대상을 캔버스에서 만든다"
```

---

### Task 10: 배경 편집이 알파 판정으로 탭과 드래그를 받는다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/ToppingHitTestInput.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasBGEditScreen.kt`

**Interfaces:**
- Consumes: `BGEditHitEntry`, `toppingTapInput`, `containsPoint`
- Produces: `@Composable fun Modifier.toppingDragInput(targetAt: () -> ToppingHitTarget?, onDrag: (Offset) -> Unit): Modifier`

**배경 — 세 가지가 서로 얽혀 있다.**

1. **탭과 드래그는 같은 노드의 모디파이어 체인에 나란히 단다.** 형제 `Box` 둘로 나누면 안 된다.
   전면 크기 포인터 노드는 좌표만 들어오면 히트로 잡히고 형제 히트 테스트가 첫 히트에서 끊기므로,
   뒤에 놓인 쪽이 앞엣것을 통째로 가린다. 같은 체인에 달린 포인터 노드는 **둘 다** 이벤트를 받는다.
2. **판정 좌표는 터치 다운 지점이다.** 기본형 드래그 감지가 주는 시작 좌표는 슬롭을 넘긴 뒤의
   좌표라, 여유분 없는 실루엣 판정에 넣으면 가장자리 드래그가 어긋난다.
3. **그렇다고 슬롭을 버리면 안 된다.** 지금 `dragBy` 는 슬롭을 적용하는데, 그것 없이 첫 이동부터
   소비하면 선택된 토핑 위의 탭이 미세 이동만으로 이동으로 처리된다. down 좌표로 판정한 뒤
   슬롭을 기다린다.

- [ ] **Step 1: 드래그 입력 모디파이어를 추가한다**

`ToppingHitTestInput.kt` 에 더한다.

```kotlin
/**
 * 터치 다운 지점이 [targetAt] 의 실루엣 안일 때만 드래그를 소비한다.
 *
 * 판정은 down 좌표로 하고 이동은 슬롭을 넘긴 뒤부터 친다 — 슬롭을 버리면 탭이 미세 이동만으로
 * 이동으로 처리된다.
 */
@Composable
internal fun Modifier.toppingDragInput(
    targetAt: () -> ToppingHitTarget?,
    onDrag: (Offset) -> Unit,
): Modifier {
    val latestTargetAt by rememberUpdatedState(targetAt)
    val latestOnDrag by rememberUpdatedState(onDrag)

    val handler = remember {
        PointerInputEventHandler {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val target = latestTargetAt() ?: return@awaitEachGesture
                if (!target.containsPoint(down.position.x, down.position.y)) {
                    return@awaitEachGesture
                }

                val afterSlop = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                    change.consume()
                } ?: return@awaitEachGesture

                drag(afterSlop.id) { change ->
                    change.consume()
                    latestOnDrag(change.positionChange())
                }
            }
        }
    }

    return this.pointerInput(Unit, handler)
}
```

import 를 더한다.

```kotlin
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChange
```

- [ ] **Step 2: 배경 편집 캔버스에 입력 레이어를 단다**

**넣는 자리가 중요하다** — 내 토핑 반복문 **뒤**, `ToppingCornerButtons` **앞**이다. 코너 버튼보다
뒤에 넣으면 삭제·편집 버튼과 크기조절·회전 핸들이 전부 죽는다.

`Box` 는 **하나**다. 탭과 드래그를 모디파이어 체인에 나란히 잇는다.

```kotlin
Box(
    modifier = Modifier
        .matchParentSize()
        .toppingTapInput(
            entries = { myEntries.map { it.topping to it.target } },
            keyOf = { it.parfaitImageId },
            onHit = onClickTopping,
            onMiss = onClickDeselectTopping,
        ).toppingDragInput(
            targetAt = { selectedEntry?.target },
            onDrag = { amount ->
                onToppingMoveDrag(amount.x / canvasWidthPx, amount.y / canvasHeightPx)
            },
        ),
)
```

- [ ] **Step 3: 컴파일·테스트·ktlint 를 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:ktlintCheck`
Expected: 전부 BUILD SUCCESSFUL

- [ ] **Step 4: 실기기로 확인한다**

배경 편집에서 아래를 확인한다.

1. 내 토핑의 그림 부분을 누르면 선택된다.
2. 내 토핑의 투명한 여백을 누르면 선택이 해제된다.
3. 겹친 내 토핑 둘에서 위 토핑의 투명한 자리를 누르면 아래 토핑이 잡힌다.
4. 선택된 토핑의 그림 부분에서 드래그를 시작하면 움직이고, 투명한 여백에서 시작하면 움직이지 않는다.
5. 드래그 중에 토핑이 중간에 멈추지 않는다.
6. 선택된 토핑을 살짝 누르기만 하면 이동하지 않는다(슬롭 유지).
7. 코너 버튼(삭제·편집)과 드래그 핸들(크기조절·회전)이 지금처럼 동작한다.
8. 선택 스트로크와 코너 버튼이 그림 가장자리에 맞게 붙는다.
9. 남의 토핑 영역을 누르면 선택이 해제된다.
10. 다른 토핑을 연달아 눌러도 선택이 바로 바뀐다(연타 방어가 대상별이다).
11. 토핑에 테두리가 보이고, 로딩 중에 사각 플레이스홀더 테두리가 번쩍이지 않는다.

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/
git commit -m "feat: 배경 편집 토핑을 누끼 모양으로 판정한다"
```

---

## 마무리

다섯 브랜치가 모두 커밋되면 사용자에게 확인을 받아 아래를 순서대로 진행한다. **확인 없이 push 하거나 PR 을 만들지 않는다.**

1. `feature/bgedit-topping-border` → `develop`
2. `feature/topping-hit-test-core` → PR 1 브랜치
3. `feature/topping-mask-loading` → PR 2 브랜치
4. `feature/canvas-main-alpha-hit` → PR 3 브랜치
5. `feature/bgedit-alpha-hit` → PR 4 브랜치

앞 PR 이 머지되면 뒤 PR 의 베이스를 `develop` 으로 바꾼다. **그 시점에야 CI 가 처음 돈다** —
워크플로가 베이스 `develop` 으로 필터돼 있어 스택 상태에서는 검사가 실행되지 않는다. 베이스를
바꾼 뒤 ktlint·유닛 테스트 결과를 반드시 확인한다.

## 스펙 미결 중 이 계획이 남기는 것

- 마스크 해상도 256px 과 알파 임계값 0.5 는 측정하지 않았다. 실기기 확인 뒤 상수만 고치면 된다.
- 토핑을 읽어 줄 콘텐츠 설명 문구가 정해지지 않아 임시 문자열과 `TODO` 를 남긴다.
- 테두리가 생기면서 코너 스트로크·버튼이 테두리 바깥이 아니라 그림 가장자리에 붙는다. 디자인 확인이 필요하다.
- 배경 편집에서 테두리 스탬프가 토핑 박스 밖으로 나가 잘리는지 실기기에서 봐야 한다.
