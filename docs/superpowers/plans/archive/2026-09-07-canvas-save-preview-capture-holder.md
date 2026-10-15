# 캔버스 저장 미리보기 캡처 전달 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> ✅ **완료·develop 머지(2026-09-07, PR #463 `2285d09da`).** 머지본이 이 계획과 **세 자리에서 다르고,
> 셋 다 계획보다 나은 쪽으로 갈렸다.** 아래 본문은 실행 당시의 기록이라 되쓰지 않고 여기에 정정만 싣는다.
> ① **홀더를 비우는 자리가 생겼다.** 아래 Global Constraints의 "앱 코드에 홀더를 비우는 호출을 넣지
> 않는다"와 Architecture 절의 "다음 `put`이 덮을 때만 빈다"는 **뒤집혔다** — 코드리뷰가 마지막 캡처
> 한 장이 다음 캡처까지 살아 있는 것을 지적했고, 미리보기의 `onDispose`에서 자기 `NavKey`가
> `Navigator.backStack`에 남아 있는지 보아 **없을 때만** 비우는 조건 하나가 들어갔다. 비파괴라는
> 성질은 그대로다(잠깐 내려간 화면은 키가 남아 있어 비우지 않는다). 정본은
> [스펙 「홀더 수명」](../../specs/archive/2026-09-07-canvas-save-preview-capture-holder.md).
> ② **압축 실패 분기에 유닛이 붙었다.** Task 2의 "파일 IO라 JVM 유닛으로 감싸기 어렵다"는 판단이
> 틀렸다 — `Context`를 `mockk`로 세우고 `cacheDir`에 JUnit `TemporaryFolder`를 물리면 실제 파일 IO가
> 도는 채로 `compress`만 스텁할 수 있다. `CanvasCaptureCacheTest` 2건이 신설됐고 `check`를 지우는
> 뮤테이션으로 실패 케이스가 실제로 깨지는 것을 확인했다.
> ③ 그래서 산출 파일이 **신규 2 · 수정 4**가 아니라 **신규 3 · 수정 4**다(Task 4 Step 6의 셈).
> 유닛은 계획의 4건이 아니라 **6건**이다.
>
> 반영하지 않은 리뷰 지적 하나: 홀더에 비트맵이 쌓여 OOM이 난다는 우려는 성립하지 않는다 —
> `put`이 필드를 덮어쓰므로 언제나 최대 한 장이고 누적 경로가 없다.

**Goal:** 저장 미리보기에 진입했을 때 캡처 이미지가 곧바로 보이게 한다. 캡처 비트맵을 홀더로 건네 정상 경로에서 디스크 왕복과 이미지 디코딩을 없앤다.

**Architecture:** `CanvasCaptureHolder`(전역 `object`, `put`/`peek` 2함수) 하나를 둔다. 캔버스 메인이 캡처 직후 `put`하고, 미리보기 Route가 `remember { peek() }`로 읽어 `Image(bitmap)`으로 직접 그린다. 홀더는 다음 `put`이 덮을 때만 비므로 미리보기가 다시 컴포즈돼도 그림이 남는다. 홀더가 비어 있으면 지금의 `AsyncImage(경로)` 분기로 떨어진다. 곁들여 `writeToCanvasCaptureCache`가 `compress`의 반환값을 확인하게 한다. 내비게이션 계약과 백스택은 바뀌지 않는다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3, Coil 3, mockk, kotlin.test

**Spec:** [`parfait/specs/archive/2026-09-07-canvas-save-preview-capture-holder.md`](../../specs/archive/2026-09-07-canvas-save-preview-capture-holder.md)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**(remote `mash-up-kr/TEAMYG-Android`)이고 브랜치는 `bugfix/#462-canvas-image-preview`다. 이 계획 문서가 있는 저장소가 아니다. **Task 5만 문서 저장소(`team-yg-pesonal-agent`) 작업이다.**
- **커밋하지 않는다.** 사용자가 요청하지 않았다. 각 Task는 변경을 남긴 채로 끝내고 리뷰를 받는다.
- **`feature/groups/canvas/api` 모듈은 건드리지 않는다.** `NavKeyCanvasImageSave`·`CanvasImageSaveResult`·`CANVAS_IMAGE_SAVE_RESULT_KEY` 셋 다 그대로다.
- **저장 확정 경로(`ResultEffect<CanvasImageSaveResult>`)를 건드리지 않는다.** 캔버스 메인은 지금처럼 `readCanvasCaptureCache`로 파일에서 읽는다.
- **`readCanvasCaptureCache`를 건드리지 않는다.** 이번에 고치는 것은 `writeToCanvasCaptureCache` 하나다.
- **앱 코드에 홀더를 비우는 호출을 넣지 않는다.** `put`이 덮어쓰는 것이 유일한 해제이고, 그것이 이 설계의 핵심이다(스펙 「홀더 수명」). 테스트가 전역 상태를 되돌리는 `clear`는 예외이고 그 함수를 앱 코드에서 부르지 않는다.
- **import 정렬을 맞추려 애쓰지 않는다.** `.editorconfig`가 `ktlint_standard_import-ordering = disabled`라 순서를 강제하지 않고, 대상 파일 일부는 애초에 정렬돼 있지 않다. 같은 패키지의 다른 import 곁에 넣고 기존 줄을 재배치하지 않는다.
- **주석·KDoc 규약**(`parfait/CLAUDE.md`, 이 저장소 밖에서 일하면 자동으로 닿지 않으므로 여기 싣는다):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 단다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 아키텍처 결정의 설명은 문서에 두고 코드에는 포인터 한 줄만 남긴다.
  - 주석 분량은 그 코드의 중요함이 아니라 **어려움**에 비례해야 한다. 6줄짜리 자명한 코드에 12줄 KDoc을 달지 않는다.
  - **의도와 함정은 쓴다.** 특히 "이걸 밟으면 깨진다"는 남긴다.
- **모든 gradle 명령은 `TJYG-Android` 루트에서 실행한다.**

---

### Task 1: 캡처 홀더

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasCaptureHolder.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasCaptureHolderTest.kt`

**Interfaces:**
- Consumes: 없다. 이 Task는 독립적이다.
- Produces: `internal object CanvasCaptureHolder`의 `fun put(bitmap: Bitmap)`(반환 없음)과 `fun peek(): Bitmap?`. Task 3이 `peek`을, Task 4가 `put`을 부른다. `Bitmap`은 `android.graphics.Bitmap`이다.

이 모듈은 이미 `parfait.test.unit` 플러그인을 쓰고 있어 `build.gradle.kts`를 고칠 필요가 없다. `mockk`와 `kotlin.test`는 그 플러그인이 붙이는 `libs.bundles.test.unit`에 들어 있다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasCaptureHolderTest.kt`

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.graphics.Bitmap
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class CanvasCaptureHolderTest {
    // 홀더가 object 라 상태가 테스트 클래스 경계도 넘는다. 앞뒤로 다 비운다
    @BeforeTest
    fun emptyBefore() = CanvasCaptureHolder.clear()

    @AfterTest
    fun emptyAfter() = CanvasCaptureHolder.clear()

    @Test
    fun peek_afterPut_givesSameBitmap() {
        val bitmap = mockk<Bitmap>()

        CanvasCaptureHolder.put(bitmap)

        assertSame(bitmap, CanvasCaptureHolder.peek())
    }

    @Test
    fun peek_calledTwice_stillGivesSameBitmap() {
        // Given 미리보기가 다시 컴포즈되면 같은 자리를 한 번 더 읽는다
        val bitmap = mockk<Bitmap>()
        CanvasCaptureHolder.put(bitmap)

        CanvasCaptureHolder.peek()

        assertSame(bitmap, CanvasCaptureHolder.peek())
    }

    @Test
    fun put_overExistingCapture_keepsOnlyTheNewOne() {
        CanvasCaptureHolder.put(mockk<Bitmap>())
        val newer = mockk<Bitmap>()

        CanvasCaptureHolder.put(newer)

        assertSame(newer, CanvasCaptureHolder.peek())
    }

    @Test
    fun peek_whenEmpty_givesNull() {
        assertNull(CanvasCaptureHolder.peek())
    }
}
```

- [ ] **Step 2: 실패를 확인한다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasCaptureHolderTest*"
```

기대: 컴파일 실패. `Unresolved reference: CanvasCaptureHolder`.

- [ ] **Step 3: 홀더를 만든다**

`feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasCaptureHolder.kt`

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.graphics.Bitmap

/**
 * 캡처한 캔버스를 저장 미리보기로 건네는 자리. 왜 파일이 아닌지는
 * `parfait/specs/2026-09-07-canvas-save-preview-capture-holder.md` 에 있다.
 *
 * ⚠️ 읽으면서 비우지 않는다. 미리보기는 화면에서 사라지지 않고도 다시 컴포즈되고(Activity
 * 재생성·백스택 하강 후 복귀), 그때 비어 있으면 보고 있던 그림을 잃는다.
 */
internal object CanvasCaptureHolder {
    @Volatile
    private var captured: Bitmap? = null

    /** 이전 캡처를 밀어낸다 — 홀더가 비는 유일한 자리다. */
    fun put(bitmap: Bitmap) {
        captured = bitmap
    }

    fun peek(): Bitmap? = captured

    /** 테스트가 전역 상태를 되돌리는 수단. 앱 코드에서 부르는 곳은 두지 않는다. */
    fun clear() {
        captured = null
    }
}
```

`clear`를 두는 형태는 같은 디렉토리의 `ToppingAlphaMaskCache.clearToppingAlphaMasks`와 같다 — 그쪽 KDoc도 "메모리 압박이나 테스트에서 캐시를 비우는 수단"이라 적고 호출부를 두지 않았다.

- [ ] **Step 4: 테스트 통과를 확인한다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasCaptureHolderTest*"
```

기대: PASS, 4건.

같은 모듈의 `CanvasMainViewModelTest`가 이미 `mockk<Bitmap>()`을 쓰고 있으므로 목킹 자체가 막힐 이유는 없다.

- [ ] **Step 5: ktlint를 통과시킨다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:ktlintCheck
```

기대: PASS. 실패하면 `./gradlew :feature:groups:canvas:impl:ktlintFormat`으로 고치고 다시 확인한다.

- [ ] **Step 6: 커밋하지 않는다**

`git status`로 두 파일이 미추적 상태인지만 확인한다.

---

### Task 2: 압축 실패를 성공으로 넘기지 않는다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasCaptureCache.kt`

**Interfaces:**
- Consumes: 없다. Task 1과 독립이라 순서를 바꿔도 된다.
- Produces: 없다. `writeToCanvasCaptureCache`의 시그니처(`Bitmap.(Context) -> Result<File>`)가 그대로다. 달라지는 것은 실패로 치는 조건뿐이다.

**왜 이 Task가 홀더와 같은 라운드에 있는가.** `Bitmap.compress`는 `Boolean`을 돌려주는데 지금 그 값이 버려지고 `runCatching`은 예외만 잡는다. 압축이 중간에 실패하면 잘린 파일이 남았는데 `Result`는 성공이다. 지금은 미리보기가 그 파일을 읽다 빈 프레임이 되어 사용자가 **확정 전에** 이상을 알아채는데, 홀더가 들어가면 미리보기가 멀쩡한 메모리 비트맵을 보여 주므로 그 실패가 확정 이후로 밀린다. 홀더만 넣고 이것을 빼면 "보고 확정한 그림과 갤러리에 남는 그림이 같아야 한다"는 계약이 약해진다.

이 함수는 실제 파일 IO와 `Context`를 요구해 JVM 유닛으로 감싸기 어렵다. 유닛 테스트를 붙이지 않고 컴파일과 기존 수동 확인에 맡긴다.

- [ ] **Step 1: 반환값을 확인한다**

`CanvasCaptureCache.kt`에서 쓰기 함수를 찾는다. 현재 모습:

```kotlin
internal fun Bitmap.writeToCanvasCaptureCache(context: Context): Result<File> = runCatching {
    val directory = File(context.cacheDir, CAPTURE_DIR_NAME).apply { mkdirs() }

    File(directory, CAPTURE_FILE_NAME).also { file ->
        file.outputStream().use { output -> compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output) }
    }
}
```

이렇게 바꾼다.

```kotlin
internal fun Bitmap.writeToCanvasCaptureCache(context: Context): Result<File> = runCatching {
    val directory = File(context.cacheDir, CAPTURE_DIR_NAME).apply { mkdirs() }

    File(directory, CAPTURE_FILE_NAME).also { file ->
        file.outputStream().use { output ->
            // compress 는 던지지 않고 false 를 준다 — 안 보면 잘린 파일이 성공으로 나간다
            check(compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) {
                "캡처한 캔버스를 굽지 못했다: ${file.absolutePath}"
            }
        }
    }
}
```

`check`가 던지는 `IllegalStateException`은 감싸고 있는 `runCatching`이 잡아 `Result.failure`가 된다. 호출부(`CanvasMainRoute`)의 `onFailure`가 이미 `canvas_main_capture_failure` 토스트를 띄우므로 새 분기를 만들 필요가 없다.

- [ ] **Step 2: 컴파일과 유닛 테스트를 확인한다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:testDebugUnitTest
```

기대: 둘 다 PASS.

- [ ] **Step 3: ktlint를 통과시킨다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:ktlintCheck
```

기대: PASS.

- [ ] **Step 4: 커밋하지 않는다**

---

### Task 3: 미리보기가 비트맵을 받아 그린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasImageSaveScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasImageSaveRoute.kt`

**Interfaces:**
- Consumes: Task 1의 `CanvasCaptureHolder.peek(): Bitmap?`.
- Produces: `CanvasImageSaveScreen`의 새 시그니처 — 첫 인자가 `bitmap: ImageBitmap?`, 둘째가 `fallbackImagePath: String`(기존 `imagePath`의 개명), 나머지 `date: LocalDate` · `onClickClose: () -> Unit` · `onClickSave: () -> Unit` · `modifier: Modifier`는 그대로다. Task 4는 이 시그니처를 부르지 않는다.

이 Task가 끝난 시점의 앱은 **동작이 지금과 같다.** 아직 아무도 `put`하지 않아 `peek()`이 늘 `null`을 주고, 미리보기는 폴백 경로(`AsyncImage`)로 그린다. Task 4가 정상 경로를 켠다. 그래서 이 시점의 `bitmap != null` 분기는 실행되지 않는 코드이고, 그것을 실제로 그려 보는 것은 Task 4 Step 5의 수동 확인이 처음이다.

두 파일을 한 Task로 묶는 이유는 컴파일 단위이기 때문이다. Screen의 시그니처를 바꾸면 유일한 호출부인 Route도 같이 고쳐야 빌드가 된다.

UI 변경이라 이 Task에는 실패하는 테스트를 먼저 쓰는 단계가 없다. 이 모듈에는 계측 테스트 소스셋이 없고, 스펙이 그 신설을 범위 밖으로 확정했다.

- [ ] **Step 1: Screen의 KDoc과 시그니처를 바꾼다**

`CanvasImageSaveScreen.kt`에서 KDoc과 함수 머리를 찾는다. 현재 모습:

```kotlin
/**
 * 캔버스를 갤러리에 넣기 전, 무엇이 저장될지 그대로 보여 주는 화면.
 *
 * 저장 자체는 하지 않는다 — 확정을 호출부에 알리기만 하고, 갤러리에 넣는 일과 결과를 알리는
 * 일은 캔버스 메인이 맡는다.
 *
 * @param imagePath 캔버스 메인이 캡처해 캐시에 구운 PNG 의 경로
 */
@Composable
internal fun CanvasImageSaveScreen(
    imagePath: String,
    date: LocalDate,
```

이렇게 바꾼다. `@param`을 하나만 남기는 것은 `bitmap`은 이름과 타입이 다 말하는 반면 `fallbackImagePath`는 **언제 쓰이는지**가 이름만으로 안 보이기 때문이다.

```kotlin
/**
 * 캔버스를 갤러리에 넣기 전, 무엇이 저장될지 그대로 보여 주는 화면.
 *
 * 저장 자체는 하지 않는다 — 확정을 호출부에 알리기만 하고, 갤러리에 넣는 일과 결과를 알리는
 * 일은 캔버스 메인이 맡는다.
 *
 * @param fallbackImagePath [bitmap] 이 없을 때만 읽는 캐시 PNG 의 경로
 */
@Composable
internal fun CanvasImageSaveScreen(
    bitmap: ImageBitmap?,
    fallbackImagePath: String,
    date: LocalDate,
```

- [ ] **Step 2: 이미지 분기를 넣는다**

같은 파일에서 미리보기 프레임 `Box`의 내용을 찾는다. 현재 모습:

```kotlin
            ) {
                AsyncImage(
                    model = ImageRequest
                        .Builder(LocalContext.current)
                        .data(imagePath)
                        // 캡처 파일명이 고정이라(CanvasCaptureCache) 경로만으로는 캐시 키가
                        // 안 갈린다 — 다시 저장한 캔버스를 열어도 이전 캡처가 뜰 수 있다
                        .addLastModifiedToFileCacheKey(true)
                        .build(),
                    contentDescription = stringResource(R.string.canvas_image_save_preview_content_description),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
```

이렇게 바꾼다. 프레임 `Box` 자체(폭 198.dp · `aspectRatio` · `border`)는 그대로 두고 안쪽만 가른다.

```kotlin
            ) {
                // 낭독이 어느 갈래로 그렸는지에 따라 달라지면 안 된다
                val previewDescription =
                    stringResource(R.string.canvas_image_save_preview_content_description)

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = previewDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = ImageRequest
                            .Builder(LocalContext.current)
                            .data(fallbackImagePath)
                            // 캡처 파일명이 고정이라(CanvasCaptureCache) 경로만으로는 캐시 키가
                            // 안 갈린다 — 다시 저장한 캔버스를 열어도 이전 캡처가 뜰 수 있다
                            .addLastModifiedToFileCacheKey(true)
                            .build(),
                        contentDescription = previewDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
```

import 두 줄을 추가한다. 기존 import 줄은 재배치하지 않는다.

```kotlin
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
```

- [ ] **Step 3: `@Preview` 호출을 고친다**

같은 파일 맨 아래 `PreviewCanvasImageSaveScreen`의 호출을 바꾼다. 현재 모습:

```kotlin
    CanvasImageSaveScreen(
        imagePath = "",
        date = date,
```

이렇게 바꾼다. 빈 경로를 주던 기존 프리뷰와 렌더 결과가 같다.

```kotlin
    CanvasImageSaveScreen(
        bitmap = null,
        fallbackImagePath = "",
        date = date,
```

- [ ] **Step 4: Route가 홀더를 읽어 넘기게 한다**

`CanvasImageSaveRoute.kt`에서 `resultEventBus`를 잡는 줄 아래에 비트맵을 읽는 줄을 넣는다. 현재 모습:

```kotlin
    val resultEventBus = LocalResultEventBus.current

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        CanvasImageSaveScreen(
            imagePath = navKey.imagePath,
            date = LocalDate.parse(navKey.date),
```

이렇게 바꾼다.

```kotlin
    val resultEventBus = LocalResultEventBus.current

    // 이 화면은 사라지지 않고도 다시 컴포즈된다(Activity 재생성·백스택 하강 후 복귀). 홀더를
    // 읽으면서 비우면 그때마다 보고 있던 그림을 잃는다
    val capturedBitmap = remember { CanvasCaptureHolder.peek()?.asImageBitmap() }

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        CanvasImageSaveScreen(
            bitmap = capturedBitmap,
            fallbackImagePath = navKey.imagePath,
            date = LocalDate.parse(navKey.date),
```

import 세 줄을 추가한다.

```kotlin
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
```

- [ ] **Step 5: 컴파일과 유닛 테스트를 확인한다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:testDebugUnitTest
```

기대: 둘 다 PASS. `CanvasImageSaveScreen`을 부르는 곳은 `CanvasImageSaveRoute`와 이 파일의 `@Preview` 둘뿐이므로 다른 호출부가 깨졌다는 오류가 나오면 안 된다.

- [ ] **Step 6: ktlint를 통과시킨다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:ktlintCheck
```

기대: PASS.

- [ ] **Step 7: 커밋하지 않는다**

`git diff --stat`으로 두 파일만 바뀌었는지 확인하고 끝낸다.

---

### Task 4: 캔버스 메인이 캡처를 홀더에 담는다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`

**Interfaces:**
- Consumes: Task 1의 `CanvasCaptureHolder.put(bitmap: Bitmap)`. Task 3이 만든 읽는 쪽(`peek`)이 이 값을 받는다.
- Produces: 없다. 이 Task가 정상 경로를 켜고 흐름이 닫힌다.

**Task 3 없이 이것만 채택하면 안 된다.** 아무도 `peek`하지 않는 홀더에 캡처마다 전체 해상도 비트맵이 들어가고, 다음 캡처가 덮을 때까지 아무 쓸모 없이 상주한다. 반대 순서(Task 3만 채택)는 이득이 0일 뿐 해가 없다.

- [ ] **Step 1: 캡처를 홀더에 담는다**

`CanvasMainRoute.kt`에서 `CanvasMainEffect.RequestCanvasCaptureForPreview` 분기를 찾는다. 현재 모습:

```kotlin
                is CanvasMainEffect.RequestCanvasCaptureForPreview -> {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    val selectedDate = viewModel.state.value.selectedDate

                    withContext(Dispatchers.IO) { bitmap.writeToCanvasCaptureCache(context) }
                        .onSuccess { file ->
                            navigator.goTo(
                                destination = NavKeyCanvasImageSave(
                                    imagePath = file.absolutePath,
                                    date = selectedDate.toString(),
                                ),
                            )
                        }.onFailure { toastPolicy.showError(captureFailureMessage) }
                }
```

`goTo` 앞에 한 줄을 넣는다. 나머지는 한 글자도 바꾸지 않는다 — 파일 쓰기가 성공해야 진입하는 순서도, 실패 토스트도 그대로다.

```kotlin
                is CanvasMainEffect.RequestCanvasCaptureForPreview -> {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    val selectedDate = viewModel.state.value.selectedDate

                    withContext(Dispatchers.IO) { bitmap.writeToCanvasCaptureCache(context) }
                        .onSuccess { file ->
                            // 미리보기가 파일을 다시 열지 않도록 방금 캡처한 그림을 그대로 건넨다.
                            // ⚠️ 구운 파일은 그래도 필요하다 — 아래 ResultEffect 가 저장할 때 읽는다
                            CanvasCaptureHolder.put(bitmap)
                            navigator.goTo(
                                destination = NavKeyCanvasImageSave(
                                    imagePath = file.absolutePath,
                                    date = selectedDate.toString(),
                                ),
                            )
                        }.onFailure { toastPolicy.showError(captureFailureMessage) }
                }
```

import 한 줄을 추가한다. 이 파일에는 같은 패키지의 `writeToCanvasCaptureCache`·`readCanvasCaptureCache` import가 이미 있으니 그 곁에 둔다.

```kotlin
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
```

- [ ] **Step 2: 컴파일과 유닛 테스트를 확인한다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:compileDebugKotlin :feature:groups:canvas:impl:testDebugUnitTest
```

기대: 둘 다 PASS.

- [ ] **Step 3: ktlint를 통과시킨다**

실행:

```bash
./gradlew :feature:groups:canvas:impl:ktlintCheck
```

기대: PASS.

- [ ] **Step 4: 앱을 설치한다**

실행:

```bash
adb devices
./gradlew :app:installDebug
```

기대: 기기가 하나 이상 나오고 `BUILD SUCCESSFUL`.

- [ ] **Step 5: 기기에서 다섯 갈래를 눈으로 확인한다**

이 모듈에 계측 테스트 소스셋이 없어(스펙이 신설을 범위 밖으로 확정했다) 여기까지가 검증이다. 그룹 하나에 들어가 캔버스를 연 뒤 확인한다. 저장 아이콘은 날짜 버튼 오른쪽에 있고, 캔버스에 배경도 토핑도 없으면 아이콘 자체가 나오지 않는다.

1. **오늘 캔버스** — 저장 아이콘을 누른다. 미리보기가 뜨는 순간 이미 이미지가 보여야 한다. 빈 프레임이 잠깐이라도 보이면 실패다.
2. **지난 캔버스** — 날짜를 바꿔 내용이 있는 지난 날짜를 고르고 저장 아이콘을 누른다. 이미지가 곧바로 보이고, 아래 날짜 라벨이 그 날짜여야 한다.
3. **확정** — 미리보기에서 저장을 누른다. 캔버스로 돌아오고 저장 성공 토스트의 날짜가 방금 저장한 캔버스의 날짜여야 한다. 갤러리 앱에서 실제 파일도 확인한다.
4. **취소 후 재진입** — 미리보기를 닫고 다른 날짜의 캔버스에서 다시 저장한다. 앞서 본 캡처가 아니라 새 캡처가 보여야 한다.
5. **다크모드 토글** — 미리보기가 떠 있는 상태에서 상단바를 내려 다크모드를 토글한다. **그림이 그대로 남아야 한다.** 사라졌다가 페이드로 돌아오면 홀더가 비파괴로 동작하지 않는다는 뜻이다. 이 갈래가 Task 1의 설계 결정을 실제로 검증하는 유일한 자리다.

결과를 갈래별로 적어 보고한다. 실패한 갈래가 있으면 고치기 전에 무엇이 어떻게 보였는지 먼저 남긴다.

여력이 있으면 저장 아이콘 연타도 한 번 해 본다. 스로틀이 없어 미리보기가 두 번 열릴 수 있는데, 이번 변경과 독립인 기존 결함이라 재현되면 고치지 말고 관찰 내용만 보고한다.

- [ ] **Step 6: 커밋하지 않는다**

`git status`로 이번 라운드가 남긴 파일이 신규 2개(`CanvasCaptureHolder.kt`·`CanvasCaptureHolderTest.kt`)와 수정 4개(`CanvasCaptureCache.kt`·`CanvasImageSaveScreen.kt`·`CanvasImageSaveRoute.kt`·`CanvasMainRoute.kt`)인지 확인하고 끝낸다.

---

### Task 5: 문서를 코드에 맞춘다

**Files (문서 저장소 `team-yg-pesonal-agent`):**
- Modify: `parfait/architecture/navigation-flow.md`
- Modify: `parfait/synthesis/open-questions.md`

**Interfaces:**
- Consumes: Task 4까지의 최종 코드.
- Produces: 없다.

**Interfaces 밖의 주의.** 이 Task만 **다른 저장소**에서 일한다. `TJYG-Android`가 아니라 이 계획 문서가 있는 저장소다. gradle 명령은 여기서 돌지 않는다.

- [ ] **Step 1: `navigation-flow.md`의 저장 왕복 절을 갱신한다**

「캔버스 저장 미리보기 왕복 (2026-09-05, PR #445)」 절을 찾는다. 그 안의 도식과 불릿이 `goTo` 뒤 미리보기가 `AsyncImage`로 경로를 읽는 것을 정본으로 그리고 있다. 다음 셋을 반영한다.

- 도식의 `goTo(NavKeyCanvasImageSave(imagePath, date))` 앞에 `CanvasCaptureHolder.put(bitmap)`을 넣고, 미리보기 쪽에 `peek()` → `Image(bitmap)` / 없으면 `AsyncImage` 갈래를 그린다.
- "캐시 파일명이 고정이다" 불릿에 **정상 경로는 더 이상 그 파일을 그리지 않는다**는 것과, 그래서 `addLastModifiedToFileCacheKey`가 이제 방어 분기에서만 의미를 갖는다는 것을 덧붙인다. 파일 자체는 저장 확정이 읽으므로 여전히 필요하다.
- **백스택이 저장되지 않는다**는 사실을 새 불릿으로 적는다. `Navigator`가 `@ActivityRetainedScoped`이고 `MainRoute`가 `rememberNavBackStack`을 쓰지 않으므로, 프로세스가 죽으면 백스택이 `NavKeySplash` 하나로 리셋된다. 이 사실은 저장 미리보기만의 것이 아니라 이 문서 전체의 전제라 여기 두는 것이 맞다.

- [ ] **Step 2: OQ-P-364를 정정한다**

`open-questions.md`에서 `OQ-P-364`를 찾는다. 항목 ①의 "프로세스 사망 뒤 복원이나 OS의 캐시 정리를 지나면 경로만 살아 돌아온다"가 사실이 아니다. 백스택이 저장되지 않아 **그 화면 자체가 복원되지 않는다.** 해당 문장을 정정하고, 정정 근거로 `Navigator`의 스코프와 `MainRoute`가 백스택을 저장하지 않는다는 사실을 적는다.

- [ ] **Step 3: OQ-P-365 ②에 홀더를 추가한다**

같은 파일에서 `OQ-P-365`를 찾는다. 항목 ②(고정 이름이라 연달아 캡처하면 앞선 미리보기가 다른 그림을 본다)에 **홀더도 같은 성질을 갖는다**는 것을 덧붙인다. 미리보기를 열어 둔 채 푸시 딥링크로 다른 그룹 캔버스에 가서 저장하면 파일과 홀더가 같은 자리에서 덮이고, 아래로 내려갔던 미리보기로 돌아오면 다른 그룹의 캡처를 그 날짜 라벨과 함께 보게 된다.

- [ ] **Step 4: 저장 아이콘 연타를 새 미결로 올린다**

같은 파일 끝에 새 항목을 추가한다. `handleClickSaveToGallery`가 스로틀 없이 `postSideEffect`하고 `BaseViewModel`의 `_effect`가 `Channel(BUFFERED)`이라, 저장 아이콘을 연타하면 두 번째 이펙트가 버퍼에 남았다가 미리보기에서 돌아온 뒤 전달되어 곧바로 다시 미리보기로 들어간다. 이번 변경과 독립인 기존 결함이다. ID는 파일에서 쓰이지 않은 다음 번호를 쓰고, 기존 항목들의 형식(출처·항목·상태·해소 메모)을 따른다.

- [ ] **Step 5: 커밋하지 않는다**

이 저장소도 같은 규칙을 따른다. 사용자가 요청하면 그때 브랜치와 커밋을 만든다.

---

## 검증 요약

| 항목 | 수단 | Task |
|---|---|---|
| 홀더 계약 4건(읽어도 안 비운다 포함) | JVM 유닛(`CanvasCaptureHolderTest`) | 1 |
| 압축 실패가 실패로 나간다 | 컴파일만(파일 IO라 유닛으로 못 감쌌다) | 2 |
| 시그니처 변경이 호출부를 깨지 않음 | `compileDebugKotlin` | 3 |
| 정상 경로에서 이미지가 곧바로 보임 | 수동(오늘·지난 캔버스) | 4 |
| 날짜 라벨이 캡처 시점 값 | 수동(지난 캔버스) | 4 |
| 저장 확정이 여전히 동작 | 수동(갤러리 확인) | 4 |
| 재진입 시 새 캡처 | 수동(취소 후 다른 날짜) | 4 |
| **비파괴 홀더가 실제로 그림을 지킴** | 수동(다크모드 토글) | 4 |
| 문서가 코드와 맞음 | 사람이 읽고 판단 | 5 |

**자동 검증이 덮지 못하는 범위를 분명히 해 둔다.** 유닛 4건은 홀더 계약만 본다. 누군가 `CanvasMainRoute`의 `put`이나 `CanvasImageSaveRoute`의 `peek`을 지워도 네 건은 전부 통과하고, 앱은 크래시 없이 예전의 느린 경로로 조용히 돌아간다. **이 기능의 유일한 관측 가능한 증상이 "느리다"인데 그것을 잡는 자동 검증은 없다.** 계측 테스트 소스셋을 신설하지 않기로 한 결과이고(`parfait.test.compose` 컨벤션 플러그인과 `core/designsystem`의 `YGCanvasTest` 선례가 있으므로 불가능해서가 아니라 이번에 안 하는 것이다), 위 표의 수동 다섯 갈래가 그 자리를 대신한다.
