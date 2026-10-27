# 텍스트 토핑 글꼴·자간 구간 분기 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 텍스트 토핑의 글꼴·자간을 텍스트 전체가 아니라 글자 구간마다 `SpanStyle`로 고른다.

**Architecture:** 폰트 cmap 에서 생성한 한자 커버리지 테이블(Task 1)을 구간 분할 함수 `textToppingScriptRuns`(Task 2)가
쓰고, 입력 필드(`OutputTransformation`)·캔버스 렌더·크기 맞춤 측정(`AnnotatedString`)이 그 결과 하나를 함께 쓴다(Task 3).

**Tech Stack:** Kotlin, Jetpack Compose foundation 1.12 (`TextFieldBuffer.addStyle`), kotlin.test, Python 3 + fonttools(생성 스크립트 전용)

**Spec:** [`docs/superpowers/specs/2026-10-27-text-topping-script-span-design.md`](../specs/2026-10-27-text-topping-script-span-design.md)

## Global Constraints

- minSdk 26. API 29+ 전용 API(`Typeface.CustomFallbackBuilder` 등)를 쓰지 않는다
- 자간 값은 지금 그대로: `DEFAULT`(SUIT) -0.011f, `JAPANESE` -0.011f, `CHINESE` 0f
- Gradle 의존성을 추가하지 않는다. fonttools 는 생성 스크립트만 쓴다
- 생성물 `TextToppingChineseCharacterCoverageData.kt`는 손으로 고치지 않는다
- feature impl 의 비 UI 타입은 `impl/model/`, 한 파일 한 타입
- Kotlin 주석·KDoc 은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다
- 커밋 메시지는 `type: 한국어 설명`, `Co-Authored-By` 줄을 넣지 않는다. 푸시하지 않는다
- 모듈 경로: `:feature:groups:canvas:impl`. 소스 루트 `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/` 를 아래에서 `impl/` 로 줄여 쓴다. 테스트 루트는 `impl-test/` (`.../src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/`)

## Review Focus

1. 한글 IME 조합 중 연타 — `OutputTransformation`이 붙어도 조합 밑줄·커서가 어긋나지 않아야 한다. 자동화할 수 없어(adb 는 ASCII 만 보낸다) Task 3 Step 9 사람 확인
2. 같은 텍스트의 편집 중(C-106)과 배치 후(C-001) 줄바꿈이 같아야 한다 — 세 경로가 `textToppingAnnotatedString`/`textToppingScriptOutputTransformation` 둘만 쓰는지 Task 3 리뷰에서 본다. 눈 확인은 Task 3 Step 9
3. 이모지 ZWJ 시퀀스·변형 선택자 같은 다중 코드포인트 그래핌이 구간 경계에서 쪼개지지 않아야 한다 — Task 2 `emojiZwjSequence_staysInOneRun`
4. 보조 평면 한자(서로게이트 쌍)의 구간 인덱스가 UTF-16 기준이어야 `addStyle` 범위가 맞는다 — Task 2 `supplementaryChineseCharacter_indicesAreUtf16`
5. 폰트 교체 후 테이블을 재생성하지 않으면 판정이 조용히 틀어진다 — Task 1 `coverageData_matchesBundledFontHashes`

---

### Task 1: 한자 커버리지 테이블과 생성 스크립트

**Files:**
- Create: `tools/text-topping-chinese-character-coverage/generate.py`
- Create: `tools/text-topping-chinese-character-coverage/README.md`
- Create: `impl/model/TextToppingChineseCharacterCoverage.kt`
- Create (생성물): `impl/model/TextToppingChineseCharacterCoverageData.kt`
- Test: `impl-test/model/TextToppingChineseCharacterCoverageTest.kt`

**Interfaces:**
- Produces:
  - `internal class TextToppingChineseCharacterCoverage(bmpPairs: String, supplementaryPairs: IntArray)` — `operator fun contains(codePoint: Int): Boolean`
  - `internal val LINE_SEED_TW_CHINESE_CHARACTERS: TextToppingChineseCharacterCoverage`, `internal val LINE_SEED_JP_CHINESE_CHARACTERS: TextToppingChineseCharacterCoverage`
  - `internal const val LINE_SEED_TW_SHA256: String`, `internal const val LINE_SEED_JP_SHA256: String` (소문자 hex)

- [ ] **Step 1: 실패하는 테스트 작성** — `TextToppingChineseCharacterCoverageTest`

```kotlin
@Test fun tw_containsCommonTraditionalChineseCharacter() { assertTrue('們'.code in LINE_SEED_TW_CHINESE_CHARACTERS); assertTrue('這'.code in LINE_SEED_TW_CHINESE_CHARACTERS) }
@Test fun jpOnlyShinjitai_absentFromTw() {
    listOf('学', '会').forEach { assertFalse(it.code in LINE_SEED_TW_CHINESE_CHARACTERS); assertTrue(it.code in LINE_SEED_JP_CHINESE_CHARACTERS) }
}
@Test fun simplifiedChineseCharacter_absentFromBoth() {
    listOf('们', '这').forEach { assertFalse(it.code in LINE_SEED_TW_CHINESE_CHARACTERS); assertFalse(it.code in LINE_SEED_JP_CHINESE_CHARACTERS) }
}
@Test fun supplementaryPlane_jpHasExtB() { assertTrue(0x21A1A in LINE_SEED_JP_CHINESE_CHARACTERS); assertFalse(0x21A1A in LINE_SEED_TW_CHINESE_CHARACTERS) }
@Test fun nonChineseCharacter_absentFromBoth() { listOf('A', '가', 'ア').forEach { assertFalse(it.code in LINE_SEED_TW_CHINESE_CHARACTERS); assertFalse(it.code in LINE_SEED_JP_CHINESE_CHARACTERS) } }
@Test fun coverageData_matchesBundledFontHashes() {
    // 단위 테스트 작업 디렉터리는 모듈 루트다
    assertEquals(LINE_SEED_TW_SHA256, sha256("src/main/res/font/line_seed_tw_regular.ttf"), REGENERATE_MESSAGE)
    assertEquals(LINE_SEED_JP_SHA256, sha256("src/main/res/font/line_seed_jp_regular.ttf"), REGENERATE_MESSAGE)
}
```

`REGENERATE_MESSAGE` = `"폰트가 바뀌었다 — python3 tools/text-topping-chinese-character-coverage/generate.py 로 테이블을 다시 만든다"`. `sha256(path)`는 `MessageDigest`로 소문자 hex 를 내는 테스트 전용 private 함수.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*TextToppingChineseCharacterCoverageTest'`
Expected: 컴파일 실패 (`LINE_SEED_TW_CHINESE_CHARACTERS` 미정의)

- [ ] **Step 3: `TextToppingChineseCharacterCoverage` 구현** (`impl/model/TextToppingChineseCharacterCoverage.kt`)

`bmpPairs`는 정렬된 `[start, end]` 문자 쌍을 이어 붙인 문자열(`bmpPairs[2i].code..bmpPairs[2i+1].code`), `supplementaryPairs`는 같은 구조의 정수 배열이다. `contains`는 `codePoint <= 0xFFFF`로 둘 중 하나를 골라 쌍 단위 이진 탐색. KDoc 에 문자열 인코딩 이유 한 줄 — `intArrayOf`로는 JVM 메서드 64KB 한도를 넘는다.

- [ ] **Step 4: 생성 스크립트 작성** (`tools/text-topping-chinese-character-coverage/generate.py`)

- 저장소 루트에서 실행. 입력 `feature/groups/canvas/impl/src/main/res/font/line_seed_{tw,jp}_regular.ttf`, 출력 `impl/model/TextToppingChineseCharacterCoverageData.kt`
- 한자 범위: U+3400–4DBF, U+4E00–9FFF, U+F900–FAFF, U+20000–3FFFF. `TTFont(path).getBestCmap()` 키 중 이 범위를 연속 구간으로 묶는다
- BMP 구간은 `"\uXXXX\uXXXX..."` 문자열 상수(`private const val ..._BMP`, 120자 줄 단위 `+` 연결), 보조 평면은 `intArrayOf(0x..., ...)`
- 파일 머리 주석: `// 생성물 — 직접 고치지 않는다. python3 tools/text-topping-chinese-character-coverage/generate.py`
- 각 폰트 SHA-256 을 `LINE_SEED_{TW,JP}_SHA256` 으로 함께 쓴다
- fonttools 가 없으면 설치 방법(`python3 -m venv .venv && .venv/bin/pip install fonttools`)을 출력하고 exit 1

README.md 는 위 실행 방법과 "폰트를 바꾸면 다시 돌린다, 해시 테스트가 잡는다" 두 가지만.

- [ ] **Step 5: 생성물 만들기**

Run: `python3 -m venv "${TMPDIR:-/tmp}/chinese-character-venv" && "${TMPDIR:-/tmp}/chinese-character-venv/bin/pip" install -q fonttools && "${TMPDIR:-/tmp}/chinese-character-venv/bin/python" tools/text-topping-chinese-character-coverage/generate.py`
Expected: `TextToppingChineseCharacterCoverageData.kt` 생성. 구간 수 TW 3706, JP 4167 (스크립트가 출력)

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*TextToppingChineseCharacterCoverageTest'`
Expected: PASS 6건

- [ ] **Step 7: 커밋**

```bash
git add tools/text-topping-chinese-character-coverage feature/groups/canvas/impl/src
git commit -m "feat: Line Seed TW·JP 한자 커버리지 테이블과 생성 스크립트를 추가한다"
```

---

### Task 2: 구간 분할 함수

**Files:**
- Create: `impl/model/TextToppingScriptRun.kt`
- Modify: `impl/model/TextToppingScript.kt`
- Test: `impl-test/model/TextToppingScriptRunsTest.kt`

**Interfaces:**
- Consumes: `LINE_SEED_TW_CHINESE_CHARACTERS`, `LINE_SEED_JP_CHINESE_CHARACTERS`, `TextToppingChineseCharacterCoverage.contains` (Task 1)
- Produces:
  - `data class TextToppingScriptRun(val script: TextToppingScript, val start: Int, val end: Int)` — UTF-16 인덱스, `end` 제외
  - `fun textToppingScriptRuns(text: CharSequence): List<TextToppingScriptRun>` (`TextToppingScript.kt`)
  - `TextToppingScript` 멤버 `val spanStyle: SpanStyle` = `SpanStyle(fontFamily = fontFamily, letterSpacing = letterSpacingEm.em)`
- `detectTextToppingScript`는 이 태스크에서 **지우지 않는다** (호출부가 Task 3 에서 바뀐다)

- [ ] **Step 1: 실패하는 테스트 작성** — `TextToppingScriptRunsTest`

헬퍼 `private fun runsOf(text: String) = textToppingScriptRuns(text).map { it.script to text.substring(it.start, it.end) }`.
`J = JAPANESE`, `C = CHINESE`, `D = DEFAULT` 로 줄여 적는다.

| 테스트 | 입력 | 기대 |
|---|---|---|
| `japaneseSpecExample_allJapanese` | `最高デザイナー ソフィ` | `[J 전체]` |
| `chineseSpecExample_allChinese` | `頂級設計師徐輝` | `[C 전체]` |
| `koreanEnglishDigits_allDefault` | `파르페 Parfait 2026` | `[D 전체]` |
| `kanaPresent_chineseCharacterTriesJapaneseFirst` | `学校へ行く` | `[J 전체]` |
| `noKana_chineseCharacterTriesChineseFirst` | `學校 学校` | `[C "學校 ", J "学", C "校"]` |
| `hangulWithChineseCharacter_chineseCharacterTriesChineseFirst` | `오늘 漢字 공부` | `[D "오늘 ", C "漢字 ", D "공부"]` |
| `hanMissingFromBoth_keepsFirstPriority` | `这` / `这は` | `[C "这"]` / `[J "这は"]` |
| `kanaWithHangul_hangulIsDefault` | `パフェ 파르페` | `[J "パフェ ", D "파르페"]` |
| `digitsInsideJapanese_areDefault` | `パフェ2個` | `[J "パフェ", D "2", J "個"]` |
| `leadingNeutral_followsFirstStrongRun` | `「最高」` / `  パフェ` | `[C 전체]` / `[J 전체]` |
| `allNeutral_isDefault` | `!? 🎉` | `[D 전체]` |
| `emojiZwjSequence_staysInOneRun` | `パフェ👨‍👩‍👧` | `[J 전체]` |
| `supplementaryChineseCharacter_indicesAreUtf16` | `"𡨚パ"` (U+21A1A + パ) | `textToppingScriptRuns` 결과가 `[TextToppingScriptRun(J, 0, 3)]` |
| `empty_noRuns` | `""` | `[]` |

추가로 `runs_areContiguousAndMerged`: 위 입력 전부에 대해 첫 `start == 0`, 마지막 `end == length`, 각 `start == 앞 end`, 이웃 구간의 `script`가 다르다.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*TextToppingScriptRunsTest'`
Expected: 컴파일 실패 (`textToppingScriptRuns` 미정의)

- [ ] **Step 3: `TextToppingScriptRun` 작성, `TextToppingScript`에 `spanStyle` 멤버 추가**

- [ ] **Step 4: `textToppingScriptRuns` 구현** (`TextToppingScript.kt`)

코드포인트 단위로 판정하고 같은 스크립트가 이어지면 합친다.

1. 먼저 텍스트 전체에 가나(U+3040–30FF)가 있는지 본다 → 한자 순서 = 있으면 `[JAPANESE to LINE_SEED_JP_CHINESE_CHARACTERS, CHINESE to LINE_SEED_TW_CHINESE_CHARACTERS]`, 없으면 반대
2. 글자별 판정
   - 가나 → `JAPANESE`
   - 한자(Task 1 과 같은 네 범위) → 순서대로 `codePoint in coverage`인 첫 스크립트, 없으면 순서의 첫 스크립트
   - `Character.isLetter` 또는 `Character.isDigit` → `DEFAULT`
   - 그 밖(공백·문장부호·이모지·ZWJ·변형 선택자) → 중립. 앞 구간 스크립트를 따른다
3. 맨 앞 중립 문자들은 뒤에 오는 첫 비중립 글자의 스크립트를 따른다. 끝까지 중립뿐이면 `DEFAULT`

KDoc 은 "왜 가나 유무로 한자 순서를 정하나"(상용 한자는 두 폰트에 다 있어 먼저 오는 폰트가 상대 언어 한자를 가져간다) 한 줄과 "왜 가나를 명시적으로 JP 로 보내나"(TW 에도 가나 글리프가 있다) 한 줄. enum 의 기존 KDoc 은 Task 3 에서 고친다.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*TextToppingScriptRunsTest'`
Expected: PASS 15건

- [ ] **Step 6: 커밋**

```bash
git add feature/groups/canvas/impl/src
git commit -m "feat: 텍스트 토핑 글꼴 구간 분할 함수를 추가한다"
```

---

### Task 3: 입력 필드·캔버스 렌더·측정에 적용

**Files:**
- Create: `impl/util/TextToppingScriptSpans.kt`
- Modify: `impl/component/TextToppingContent.kt` (`textToppingBaseStyle`, `TextToppingContent`)
- Modify: `impl/component/TextToppingLayoutModifiers.kt` (`fitTextToppingToHeight`)
- Modify: `impl/screen/CanvasTextInputScreen.kt`
- Modify: `impl/model/TextToppingScript.kt` (`detectTextToppingScript` 삭제, enum KDoc)
- Modify: `docs/status.md` (「텍스트 토핑」 절)
- Test: `impl-test/util/TextToppingScriptSpansTest.kt`

**Interfaces:**
- Consumes: `textToppingScriptRuns`, `TextToppingScript.spanStyle` (Task 2)
- Produces:
  - `internal fun textToppingAnnotatedString(text: String): AnnotatedString`
  - `internal val textToppingScriptOutputTransformation: OutputTransformation`
  - `internal fun textToppingBaseStyle(): TextStyle` (파라미터 제거)
  - `fitTextToppingToHeight(..., text: AnnotatedString, ...)` (나머지 파라미터 그대로)

- [ ] **Step 1: 실패하는 테스트 작성** — `TextToppingScriptSpansTest`

```kotlin
@Test fun annotatedString_onePerRunSpanAndTextUnchanged() {
    val result = textToppingAnnotatedString("パフェ 파르페")
    assertEquals("パフェ 파르페", result.text)
    assertEquals(
        listOf(
            AnnotatedString.Range(TextToppingScript.JAPANESE.spanStyle, 0, 4),
            AnnotatedString.Range(TextToppingScript.DEFAULT.spanStyle, 4, 7),
        ),
        result.spanStyles,
    )
}
@Test fun annotatedString_empty_noSpans() { assertTrue(textToppingAnnotatedString("").spanStyles.isEmpty()) }
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*TextToppingScriptSpansTest'`
Expected: 컴파일 실패

- [ ] **Step 3: `TextToppingScriptSpans.kt` 구현**

- `textToppingAnnotatedString`: `buildAnnotatedString { append(text); runs.forEach { addStyle(it.script.spanStyle, it.start, it.end) } }`
- `textToppingScriptOutputTransformation`: `OutputTransformation { textToppingScriptRuns(asCharSequence()).forEach { addStyle(it.script.spanStyle, it.start, it.end) } }`. 실험 API opt-in 이 필요하면 이 선언에만 `@OptIn(ExperimentalFoundationApi::class)`

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*TextToppingScriptSpansTest'`
Expected: PASS 2건

- [ ] **Step 5: 세 경로에 적용하고 `detectTextToppingScript` 삭제**

- `textToppingBaseStyle()`: `b01R.copy(fontFamily = DEFAULT.fontFamily, letterSpacing = DEFAULT.letterSpacingEm.em)` — 구간이 전부 덮고, 베이스는 placeholder 가 쓴다
- `TextToppingContent`: `BasicText(text = remember(text) { textToppingAnnotatedString(text) }, style = textToppingBaseStyle()...)`
- `fitTextToppingToHeight`: `text: String` → `text: AnnotatedString`. 본문의 `measure` 호출은 그대로 컴파일된다
- `CanvasTextInputScreen`
  - `textScript`·`detectTextToppingScript` 줄 삭제, `autoSizeBaseStyle = textToppingBaseStyle()`
  - `hugMeasureText`를 `remember(currentText, placeholderText) { textToppingAnnotatedString(currentText.ifEmpty { placeholderText }) }`로 (`currentText = textFieldState.text.toString()`). fit 과 hug 폭 측정이 이 값을 쓴다
  - `BasicTextField`에 `outputTransformation = textToppingScriptOutputTransformation`
  - placeholder `Text`는 그대로 `autoSizeBaseStyle` 기반
- `TextToppingScript.kt`: `detectTextToppingScript`와 `HANGUL_RANGE` 삭제. enum KDoc 을 "글꼴·자간을 글자 구간마다 고른다 — 분할 규칙은 `textToppingScriptRuns`"로 바꾸고, "Compose 편집 필드는 스타일 하나만 먹는다" 문장은 지운다(이제 사실이 아니다)

- [ ] **Step 6: 모듈 테스트·빌드**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :app:assembleDebug`
Expected: BUILD SUCCESSFUL, 실패 0. `grep -rn detectTextToppingScript feature/` 결과 없음

- [ ] **Step 7: `docs/status.md` 갱신**

「텍스트 토핑」 절에서
- "⚠️ Line Seed JP·TW 전체 글리프를 … 한 글꼴로만 그려진다" 줄을 "⚠️ Line Seed JP·TW 전체 글리프를 서브셋 없이 번들해 APK가 커졌다. 글꼴·자간은 글자 구간마다 고르고, 한자는 가나가 있으면 JP → TW, 없으면 TW → JP 순으로 글리프가 있는 첫 폰트를 쓴다 — 두 폰트에 다 없는 한자(간체 일부)는 시스템 폰트로 그려지고, 한 단어 안에서 JP·TW 가 섞일 수 있다(`学校`)"로 바꾼다
- 앵커에 `textToppingScriptRuns`, `TextToppingChineseCharacterCoverage`, `tools/text-topping-chinese-character-coverage/` 추가
- 설계 줄에 이 스펙 링크 추가

Run: `python3 docs/script/check_links.py docs`
Expected: exit 0

- [ ] **Step 8: 커밋**

```bash
git add feature/groups/canvas/impl/src docs/status.md
git commit -m "feat: 텍스트 토핑 입력·렌더·측정에 글꼴 구간 스타일을 적용한다"
```

- [ ] **Step 9: 실기기 확인 (사람)** — `./gradlew :app:installDebug` 뒤 사용자가 직접

1. C-106 에 `最高デザイナー ソフィ`, `頂級設計師徐輝`, `パフェ 파르페`, `오늘 漢字 공부`를 각각 입력 → 가나·한자는 Line Seed, 한글은 SUIT 로 보인다
2. 키보드를 내렸을 때(배치 모드)와 [캔버스에 쌓기] 뒤 C-001 에서 줄바꿈 위치가 편집 중과 같다
3. 한글을 빠르게 연타 — 조합 밑줄·커서가 어긋나지 않고 글자가 밀리지 않는다
4. `パフェ頂級` 처럼 JP(-1.1%)·TW(0%) 구간이 맞닿는 곳의 간격이 어색하지 않다

결과를 사용자에게 받아 문제가 있으면 그 항목만 고친다.
