---
id: fixed-font-scale-text
title: 시스템 글자 크기와 무관한 텍스트
status: in-progress
type: work-order
created: 2026-11-03
updated: 2026-11-03
platforms: android
owner: android
related_adr:
related_spec: fixed-font-scale-text
related_code:
  - FixedFontScale.kt#toFixedFontScale
  - YGText.kt#YGText
  - YGBasicText.kt#YGBasicText
  - YGBasicTextField.kt#YGBasicTextField
archived_reason:
tags: [plan, parfait]
---

# 시스템 글자 크기와 무관한 텍스트 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** OS `fontScale`과 무관하게 앱의 모든 Compose 텍스트를 피그마 수치(dp)로 그린다.

**Architecture:** `core:designsystem`의 `component/ygtext/`에 공유 변환 함수(`toFixedFontScale`)와 원본 오버로드별 래퍼를 둔다. 래퍼는 원본 파라미터를 그대로 미러링하고, 안에서 스타일을 변환한 뒤 원본을 호출한다. 그다음 저장소 전체 호출부를 래퍼로 교체하고, 캔버스의 로컬 `LocalDensity` 덮어쓰기를 걷어낸다.

**Tech Stack:** Kotlin, Jetpack Compose. 실측 해석 버전은 `material3` 1.4.0, `foundation`·`ui-text` 1.12.0(BOM `2026.08.00`). 테스트는 JUnit4 + `kotlin.test`(JVM), Compose UI test(계측).

**Spec:** [`docs/superpowers/specs/2026-11-03-fixed-font-scale-text-design.md`](../specs/2026-11-03-fixed-font-scale-text-design.md)

## Global Constraints

- 정책: `fontScale`을 **완전히 무시**한다. 상한(clamp)은 두지 않는다. 정책 코드는 `FixedFontScale.kt` 안에만 둔다.
- 변환식: `sp` 값을 피그마 dp로 보고 `with(density) { value.dp.toSp() }`. `em`·`Unspecified`는 그대로 둔다.
- 변환은 멱등이 아니다. **래퍼에는 원본(피그마 수치) 스타일을 넘긴다.** 호출부가 직접 변환하는 것은 `TextMeasurer`·`android.graphics.Paint` 같은 컴포지션 밖 측정·그리기뿐이다.
- 래퍼 파라미터는 원본과 **같은 이름·순서·기본값**으로 전부 미러링한다(`@Deprecated` 오버로드는 제외).
- 자동 강제(ktlint/Lint 커스텀 룰, CI 스크립트)는 만들지 않는다. 문서 규약만 둔다.
- 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md) 규약을 따른다. 서브에이전트 브리프에 이 링크를 넣는다.
- 커밋 메시지: `type: 한국어 설명`. `Co-Authored-By` 줄을 붙이지 않는다.
- 원격 push·PR 생성 금지(사용자가 따로 요청할 때만).
- 각 task 끝에서 `./gradlew ktlintCheck`가 통과해야 한다. import 순서는 `./gradlew ktlintFormat`으로 맞춘다.

## Review Focus

1. **이중 변환** — 이미 변환한 스타일을 래퍼에 다시 넘기면 1.15에서 14sp가 약 10.6sp로 줄어든다. 측정과 렌더링이 스타일 변수를 같이 쓰는 캔버스가 가장 위험하다. → Task 6에서 변환 전후 변수를 나누고 실기기 비교로 확인한다.
2. **`fontScale` 1.0 기기 무변화** — 교체 전후 화면이 같아야 한다. → Task 1 `toFixedFontScale_fontScaleOne_isIdentity`.
3. **비선형 구간(`fontScale` 2.0)** — Android 14 비선형 변환에서도 결과 px이 피그마 dp px과 같아야 한다. → Task 1 `toFixedFontScale_nonLinearScale_rendersDesignDp`.
4. **AnnotatedString span의 sp** — `YGToast`처럼 span에 실린 `fontSize`가 변환되지 않으면 토스트 글자만 커진다. → Task 1 span 테스트, Task 2 `ygTextAnnotated_spanFontSize_isFixed`.
5. **새 window(`Dialog`/`Popup`)** — `YGModalPopup`, `YGAlert` 안의 텍스트도 고정돼야 한다. 변환이 그 자리의 `LocalDensity`를 쓰므로 맞아야 하지만 자동 테스트는 없다. → Task 7 실기기 확인 목록에 넣는다.

---

### Task 1: 공유 변환 함수

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/FixedFontScale.kt`
- Create: `core/designsystem/src/test/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/FixedFontScaleTest.kt`
- Modify: `core/designsystem/build.gradle.kts` — `alias(libs.plugins.parfait.test.unit)` 추가

**Interfaces:**
- Produces (모두 public):
  - `fun TextUnit.toFixedFontScale(density: Density): TextUnit`
  - `fun TextStyle.toFixedFontScale(density: Density): TextStyle` — `fontSize`·`lineHeight`·`letterSpacing`
  - `fun AnnotatedString.toFixedFontScale(density: Density): AnnotatedString` — 모든 `SpanStyle`의 `fontSize`·`letterSpacing`, 모든 `ParagraphStyle`의 `lineHeight`
  - `@Composable @ReadOnlyComposable fun TextStyle.toFixedFontScale(): TextStyle` — `LocalDensity.current` 사용

- [ ] **Step 1: 실패하는 테스트 작성**

`FixedFontScaleTest`. 기준 density는 `Density(density = 2.75f, fontScale = s)`. 불변식은 "변환한 sp를 px로 바꾸면 원래 수치를 dp로 바꾼 px과 같다"이다. 비교는 `assertEquals(expected, actual, absoluteTolerance = 0.01f)`.

- `toFixedFontScale_fontScaleOne_isIdentity` — s=1.0. `14.sp` → `14.sp`
- `toFixedFontScale_fontScale115_rendersDesignDp` — s=1.15. `with(d) { 14.sp.toFixedFontScale(d).toPx() } == with(d) { 14.dp.toPx() }`
- `toFixedFontScale_nonLinearScale_rendersDesignDp` — s=2.0. 위와 같은 단언
- `toFixedFontScale_emAndUnspecified_unchanged` — `1.4.em`, `TextUnit.Unspecified`는 그대로
- `textStyle_toFixedFontScale_convertsSpFields` — `TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 1.sp)`, s=1.15. 세 필드 모두 px이 dp px과 같음. `fontWeight` 등 나머지 필드는 보존
- `annotatedString_toFixedFontScale_convertsSpans` — `buildAnnotatedString { withStyle(SpanStyle(fontSize = 12.sp)) { append("a") }; withStyle(ParagraphStyle(lineHeight = 18.sp)) { append("b") } }`, s=1.15. span `fontSize`, paragraph `lineHeight`가 변환되고 텍스트와 범위(start/end)는 보존

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:designsystem:testDebugUnitTest --tests '*FixedFontScaleTest'`
Expected: FAIL. 컴파일 에러 `Unresolved reference: toFixedFontScale`.

- [ ] **Step 3: `FixedFontScale.kt` 구현**

`TextUnit.toFixedFontScale`만 정책을 안다. `isSp`이면 `with(density) { value.dp.toSp() }`, 아니면 `this`. 나머지 함수는 모두 이 함수에 위임한다.
- `AnnotatedString`은 `AnnotatedString.Builder`로 다시 만든다. `getStringAnnotations`·`getLinkAnnotations` 등 비스타일 annotation도 보존한다. `spanStyles`/`paragraphStyles`만 바꾸고 `AnnotatedString(text, spanStyles, paragraphStyles)` 생성자를 쓰면 annotation이 사라지니 쓰지 않는다.
- KDoc 한 줄: 멱등이 아니며 래퍼에는 원본 스타일을 넘긴다는 경고.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:designsystem:testDebugUnitTest --tests '*FixedFontScaleTest'`
Expected: PASS(6 tests).
- 비선형 테스트가 JVM에서 `Method ... not mocked` 등 Android 런타임 문제로 실패하면, 그 테스트 하나만 `src/androidTest/.../ygtext/FixedFontScaleNonLinearTest.kt`로 옮긴다. 계측 테스트는 `:core:designsystem:connectedDebugAndroidTest`로 돌린다. 나머지는 JVM에 남긴다.

- [ ] **Step 5: 커밋**

```bash
git add core/designsystem/build.gradle.kts core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/FixedFontScale.kt core/designsystem/src/test
git commit -m "feat: 시스템 글자 크기를 무시하는 텍스트 단위 변환 함수 추가"
```

---

### Task 2: `YGText`·`YGBasicText`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGText.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGBasicText.kt`
- Create: `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGTextTest.kt`

**Interfaces:**
- Consumes: Task 1의 `TextStyle/TextUnit/AnnotatedString.toFixedFontScale(density)`
- Produces:
  - `@Composable fun YGText(text: String, …)` — `material3.Text`(String, `autoSize` 있는 비-deprecated 오버로드) 파라미터 전부
  - `@Composable fun YGText(text: AnnotatedString, …)` — `material3.Text`(AnnotatedString, `inlineContent` 있는 비-deprecated 오버로드) 파라미터 전부
  - `@Composable fun YGBasicText(text: AnnotatedString, …)` — `foundation.text.BasicText`(AnnotatedString, `autoSize` 있는 비-deprecated 오버로드) 파라미터 전부

- [ ] **Step 1: 실패하는 계측 테스트 작성**

`YGTextTest`. `createComposeRule()`을 쓰고, `CompositionLocalProvider(LocalDensity provides Density(density = 기기 density, fontScale = 1.15f))` 안에서 그린다. `onTextLayout`으로 `TextLayoutResult`를 받는다. 단언 기준은 `with(density) { 14.dp.toPx() }`이다.

- `ygText_styleFontSize_isFixed` — `YGText("a", style = TextStyle(fontSize = 14.sp))`. `layoutInput.style.fontSize`의 px이 기준과 같다(허용 오차 0.01)
- `ygText_fontSizeParam_isFixed` — `YGText("a", fontSize = 14.sp)`. 같은 단언
- `ygTextAnnotated_spanFontSize_isFixed` — span `fontSize = 14.sp`. `layoutInput.text.spanStyles.first().item.fontSize`의 px이 기준과 같다
- `ygBasicText_styleFontSize_isFixed` — `YGBasicText(AnnotatedString("a"), style = TextStyle(fontSize = 14.sp))`. 같은 단언

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:designsystem:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.core.designsystem.component.ygtext.YGTextTest`
Expected: FAIL. 컴파일 에러 `Unresolved reference: YGText`.

- [ ] **Step 3: 래퍼 구현**

- 각 래퍼는 `val density = LocalDensity.current`로 변환한 뒤 원본을 호출한다.
- 변환 대상: `style`, `fontSize`, `letterSpacing`, `lineHeight` 파라미터, 그리고 AnnotatedString 오버로드의 `text`.
- `autoSize`는 그대로 넘긴다. KDoc 한 줄로 "변환하지 않는다"고 적는다.
- `style` 기본값은 원본과 같다(`YGText`는 `LocalTextStyle.current`, `YGBasicText`는 `TextStyle.Default`).
- `text`·`style` 변환은 `remember(text, style, density)`로 감싼다.

- [ ] **Step 4: 통과 확인**

Step 2와 같은 명령. Expected: PASS(4 tests).

- [ ] **Step 5: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGText.kt core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGBasicText.kt core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGTextTest.kt
git commit -m "feat: 글자 크기 고정 래퍼 YGText·YGBasicText 추가"
```

---

### Task 3: `YGBasicTextField`

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGBasicTextField.kt`
- Create: `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGBasicTextFieldTest.kt`

**Interfaces:**
- Consumes: Task 1의 `TextStyle.toFixedFontScale(density)`
- Produces: `foundation` 1.12.0 `BasicTextField`의 비-deprecated 오버로드 3개를 미러링한 `YGBasicTextField`
  - `(state: TextFieldState, …, decorator: TextFieldDecorator?, scrollState: ScrollState)`
  - `(value: String, onValueChange: (String) -> Unit, …, decorationBox)`
  - `(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, …, decorationBox)`

- [ ] **Step 1: 실패하는 계측 테스트 작성**

Task 2와 같은 `fontScale = 1.15f` 환경.
- `ygBasicTextField_state_textStyleIsFixed` — `onTextLayout = { getResult -> captured = getResult() }`
- `ygBasicTextField_string_textStyleIsFixed`
- `ygBasicTextField_textFieldValue_textStyleIsFixed`

셋 다 `textStyle = TextStyle(fontSize = 14.sp)`로 그리고, `layoutInput.style.fontSize`의 px이 `14.dp.toPx()`와 같은지 단언한다. 입력 값·`TextFieldValue`의 span은 변환하지 않는다(사용자 입력이므로).

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:designsystem:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.teamyg.parfait.core.designsystem.component.ygtext.YGBasicTextFieldTest`
Expected: FAIL. `Unresolved reference: YGBasicTextField`.

- [ ] **Step 3: 구현**

`textStyle`만 `remember(textStyle, density)`로 변환해 원본에 넘긴다. `scrollState` 기본값 `rememberScrollState()`, String/TextFieldValue 오버로드의 `maxLines = if (singleLine) 1 else Int.MAX_VALUE`처럼 원본 기본값을 그대로 옮긴다.

- [ ] **Step 4: 통과 확인**

Step 2와 같은 명령. Expected: PASS(3 tests).

- [ ] **Step 5: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGBasicTextField.kt core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/component/ygtext/YGBasicTextFieldTest.kt
git commit -m "feat: 글자 크기 고정 래퍼 YGBasicTextField 추가"
```

---

### Task 4: core 모듈과 app-preview 교체

**Files (Modify):**
- `core/designsystem/src/main/kotlin/.../component/` 아래 `material3.Text`·`BasicTextField`를 import하는 28개 파일
  - `YGTextFieldImpl`(BasicTextField String 오버로드 + placeholder `Text`)과 `YGNametagChip` 포함
- `core/ui/src/main/java/com/teamyg/parfait/core/ui/VerticalGridLayout.kt`
- `app-preview/src/main/kotlin/com/teamyg/parfait/preview/screen/MainScreen.kt`, `.../component/YGTopBarPreviewScreen.kt`

대상 목록은 다음 명령으로 뽑는다(저장소 루트):
`grep -rlE '^import androidx\.compose\.(material3\.Text|foundation\.text\.(BasicText|BasicTextField))$' --include='*.kt' core app-preview | grep -v /build/`

**Interfaces:**
- Consumes: `YGText`, `YGBasicTextField`(패키지 `com.teamyg.parfait.core.designsystem.component.ygtext`), `TextStyle.toFixedFontScale(density)`

- [ ] **Step 1: 기계적 교체**

파일마다 다음을 한다.
- import 줄 `androidx.compose.material3.Text` → `com.teamyg.parfait.core.designsystem.component.ygtext.YGText`. `BasicTextField`도 같은 방식으로 바꾼다.
- 호출 `Text(` → `YGText(`. 식별자 경계가 앞에 있을 때만 바꾼다(`BasicText(`, `TextButton(` 등은 건드리지 않는다).
- `ygtext` 패키지 안 파일(`YGDate`, `YGLabel`)은 import 줄을 지우기만 한다(같은 패키지).

- [ ] **Step 2: `YGNametagChip` Paint 보정**

`inkPaint`의 `textSize`를 `with(density) { textStyle.toFixedFontScale(density).fontSize.toPx() }`로 바꾼다. 렌더링(`YGText`)에는 원본 `textStyle`을 그대로 넘긴다.

- [ ] **Step 3: 남은 원본 사용이 없는지 확인**

Run: `grep -rnE '^import androidx\.compose\.(material3\.Text|foundation\.text\.(BasicText|BasicTextField))$' --include='*.kt' core app-preview | grep -v /build/ | grep -v '/ygtext/YG'`
Expected: 출력 없음. `ygtext/` 래퍼 파일 자신은 예외다.

- [ ] **Step 4: 빌드·포맷·기존 테스트**

Run: `./gradlew ktlintFormat ktlintCheck :core:designsystem:assembleDebug :core:ui:assembleDebug :app-preview:assembleDebug :core:designsystem:testDebugUnitTest :core:ui:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 커밋**

```bash
git add core app-preview
git commit -m "refactor: core 모듈 텍스트를 글자 크기 고정 래퍼로 교체"
```

---

### Task 5: feature 모듈 교체 (캔버스 제외)

**Files (Modify):** `feature/` 아래 대상 파일 중 `feature/groups/canvas/` 밖의 것.
- 카메라 `CameraZoomIndicatorComponent`, `ZoomLevelRow`(직접 `fontSize = 13.sp` 전달 — 래퍼가 변환하므로 값은 그대로 둔다)
- `InviteCodeInputField`(BasicTextField TextFieldValue 오버로드), `GroupListTooltip`(AnnotatedString) 포함

대상 목록: `grep -rlE '^import androidx\.compose\.(material3\.Text|foundation\.text\.(BasicText|BasicTextField))$' --include='*.kt' feature | grep -v /build/ | grep -v feature/groups/canvas/`

**Interfaces:**
- Consumes: `YGText`, `YGBasicTextField`

- [ ] **Step 1: 기계적 교체** — Task 4 Step 1과 같은 규칙

- [ ] **Step 2: 남은 원본 사용 확인**

Run: Task 4 Step 3 명령에서 경로를 `feature`로 바꾸고 `| grep -v feature/groups/canvas/`를 붙여 실행한다.
Expected: 출력 없음.

- [ ] **Step 3: 빌드·포맷·테스트**

Run: `./gradlew ktlintFormat ktlintCheck assembleDebug test`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: 커밋**

```bash
git add feature
git commit -m "refactor: feature 모듈 텍스트를 글자 크기 고정 래퍼로 교체"
```

---

### Task 6: 캔버스 통일

**Files (Modify):**
- `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/TextToppingContent.kt`
- `.../canvas/impl/screen/CanvasTextInputScreen.kt`
- `.../canvas/impl/component/TextToppingLayoutModifiers.kt`
- `.../canvas/impl/component/TextToppingBackground.kt`(KDoc만)
- 캔버스의 나머지 교체 대상: `CanvasLoadOverlay`, `CanvasTextInputToolbar`, `CustomCalendar`, `ToppingBorderPanel`, `CanvasImageSaveScreen`

**Interfaces:**
- Consumes: `YGText`, `YGBasicText`, `YGBasicTextField`, `TextStyle.toFixedFontScale(density)`

- [ ] **Step 1: 교체 전 기준 화면 캡처**

기기 `font_scale` 1.15(`adb shell settings get system font_scale`로 확인)에서 현재 HEAD를 설치하고 스크린샷을 찍는다.
- C-106 텍스트 입력: 짧은 문구, 세로 예산을 넘는 긴 문구(자동 축소 발동) 각각 편집 중 + 배치 상태
- C-001 캔버스에 쌓인 텍스트 토핑
- 이미지 저장 결과 PNG

모두 스크래치 디렉터리에 저장한다.

- [ ] **Step 2: `LocalDensity` 덮어쓰기 제거와 측정 경로 변환**

- `TextToppingContent`, `CanvasTextInputScreen`의 `fixedFontScaleDensity`와 `CompositionLocalProvider`를 제거한다. 그 안의 "시스템 폰트 크기와 무관" 주석은 래퍼 쪽으로 의도를 옮긴다(호출부 주석 한 줄).
- `fitTextToppingToHeight`의 두 `measure` 호출에 넘기는 스타일을 `baseStyle.withTextToppingSizing(x).toFixedFontScale(density)`로 바꾼다.
- `CanvasTextInputScreen`의 `hugWidthDp` 측정을 `textMeasurer.measure(text = hugMeasureText, style = hugRenderStyle.toFixedFontScale(density))`로 바꾼다.
  - `hugRenderStyle` 자체는 원본으로 둔다. 렌더링 쪽 `YGBasicTextField(textStyle = …)`와 placeholder `YGText(style = …)`에는 원본 체인을 그대로 넘긴다.
- `TextToppingContent`의 `BasicText` → `YGBasicText`, `CanvasTextInputScreen`의 `BasicTextField(state = …)` → `YGBasicTextField`, `Text` → `YGText`.
- `TextToppingBackground` KDoc에서 "호출부가 `fontScale = 1`로 고정해 두므로" 전제를 "[fontSizeSp]는 피그마 수치라 그대로 dp로 쓴다"로 고친다.
- 나머지 캔버스 파일은 Task 4 Step 1 규칙으로 교체한다.

- [ ] **Step 3: 남은 원본 사용과 덮어쓰기 확인**

Run: `grep -rnE 'fontScale = 1f|^import androidx\.compose\.(material3\.Text|foundation\.text\.(BasicText|BasicTextField))$' feature/groups/canvas --include='*.kt' | grep -v /build/`
Expected: 출력 없음.

- [ ] **Step 4: 빌드·테스트**

Run: `./gradlew ktlintFormat ktlintCheck :feature:groups:canvas:impl:assembleDebug :feature:groups:canvas:impl:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: 실기기 비교**

Step 1과 같은 화면·문구로 다시 찍고, 크기·줄바꿈·배경 박스가 같은지 나란히 비교한다. 이미지 저장 PNG는 픽셀 diff까지 본다.
- 다르면 이중 변환이나 변환 누락이다. 측정 경로와 렌더 경로에 넘긴 스타일부터 확인한다.

- [ ] **Step 6: 커밋**

```bash
git add feature/groups/canvas
git commit -m "refactor: 캔버스 텍스트 토핑의 글자 크기 고정을 래퍼 방식으로 통일"
```

---

### Task 7: 문서와 최종 확인

**Files:**
- Modify: `docs/code-conventions.md` — "Compose 텍스트" 절 추가
- Modify: `docs/superpowers/specs/2026-11-03-fixed-font-scale-text-design.md` — `status: in-progress`, `related_code`에 `FixedFontScale.kt#toFixedFontScale` 추가
- Modify: `docs/superpowers/plans/README.md` — 이 계획 행의 상태 갱신
- Modify: `docs/status.md` — 디자인시스템 타이포 항목에 현재 상태 한 줄(형식은 파일의 기존 행을 따른다)

- [ ] **Step 1: `code-conventions.md` 절 작성**

스펙 「문서 규약」 절의 세 규칙을 옮긴다.
- 텍스트는 `YGText`·`YGBasicText`·`YGBasicTextField`를 기본으로 쓴다.
- 원본은 OS 글자 크기를 의도적으로 따라야 할 때만 쓰고, 그 자리에 이유를 주석 한 줄로 남긴다.
- 컴포지션 밖 측정·그리기에는 `toFixedFontScale(density)`를 적용한다.

여기에 멱등이 아니라는 경고 한 줄을 더한다.

- [ ] **Step 2: 저장소 전체 잔여 확인**

Run: `grep -rnE '^import androidx\.compose\.(material3\.Text|foundation\.text\.(BasicText|BasicTextField))$' --include='*.kt' . | grep -v /build/ | grep -v '/ygtext/YG'`
Expected: 출력 없음.

- [ ] **Step 3: 전체 빌드·테스트·링크**

Run: `./gradlew ktlintCheck assembleDebug test assembleAllDebugAndroidTest && ./gradlew :core:designsystem:connectedDebugAndroidTest && python3 docs/script/check_links.py docs`
Expected: BUILD SUCCESSFUL, `깨진 링크 0건`.

- [ ] **Step 4: 실기기 확인(`font_scale` 1.15)**

다음 화면을 찍어 피그마와 나란히 놓고 글자 크기가 같은지 확인한다.
- 그룹 상세(S-101)
- `YGModalPopup` 다이얼로그(그룹 나가기)
- `YGToast`(span 텍스트)
- 닉네임 입력 `YGTextField`

결과는 사용자에게 스크린샷 경로로 보고한다.

- [ ] **Step 5: 커밋**

```bash
git add docs
git commit -m "docs: 텍스트 글자 크기 고정 규약과 상태 문서 갱신"
```
