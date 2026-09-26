# 유닛 테스트 기반 구조 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 테스트 소스셋·의존성이 하나도 없는 TJYG-Android에 유닛 테스트 기반을 세우고, 로직 보유 4개 모듈에 배선한 뒤 팀이 따라 쓸 시범 테스트를 남긴다.

**Architecture:** 기존 build-logic 관례대로 컨벤션 플러그인 3종(unit·android·compose)을 만든다 — 플러그인 클래스는 `BaseConventionPlugin`을 상속한 얇은 껍데기이고 실제 설정은 `buildlogic/TestConfig.kt`의 `setConfigTestXxx()` 함수에 둔다. 공용 테스트 유틸은 `:core:testing` JVM 모듈에 모아 `testImplementation`·`androidTestImplementation` 양쪽에서 소비한다. unit은 `domain`·`data`·`core:util:jvm`·`core:util:android` 4개 모듈에, 계측·Compose는 배선 검증용 스모크 1개씩에만 적용한다.

**Tech Stack:** JUnit4 · kotlin-test · kotlinx-coroutines-test · Turbine · MockK · MockWebServer(mockwebserver3) · androidx.test · Compose ui-test-junit4 · Gradle 컨벤션 플러그인 · GitHub Actions

**설계 스펙:** [`parfait/specs/archive/2026-08-06-unit-test-infrastructure.md`](../../specs/archive/2026-08-06-unit-test-infrastructure.md) (스킬·위키 repo 경로. 코드 작업은 TJYG-Android repo에서 한다)

**GitHub 이슈:** `mash-up-kr/TJYG-Android#215`

> **완료 — PR #219 `develop` 머지(2026-08-09).** Task 1~9 전량 반영(Task 10은 아래 보류 블록 참고).
> 커밋 17개. 계획 본문의 코드 블록은 작성 시점 스냅샷이고, 산출물 계약의 정본은
> [스펙](../../specs/archive/2026-08-06-unit-test-infrastructure.md)이다. 본문 Task 체크박스는
> 실행 기록을 상단·말미 블록에 모으는 관례대로 미체크로 둔다.

## Global Constraints

- **작업 대상 repo는 `TJYG-Android`다.** 브랜치는 `feature/#215-test-environment`, 베이스는 `develop`.
- **커밋한다.** 각 Task 끝에서 커밋한다. push·PR 생성은 사람 확인 후.
- 이 저장소(스킬·위키 repo)에는 코드를 만들지 않는다. 스펙·계획 문서만 여기 있다.
- JDK 17, AGP 9.2.1, Kotlin 2.4.0, `compileSdk` 37, `minSdk` 26.
- **Robolectric을 넣지 않는다.** Android 프레임워크 의존 코드는 이번 유닛 테스트 대상이 아니다.
- **`testOptions.unitTests.isReturnDefaultValues`를 건드리지 않는다** (기본값 false 유지). 켜면 `android.jar` 스텁이 예외 대신 기본값을 반환해 Android 의존 코드가 잘못 통과한다.
- **테스트 메서드 이름은 영문 `메서드_상태_기대`.** 백틱 메서드명 금지 — 백틱은 기기 API 30+ 전용인데 `minSdk`가 26이라 계측 테스트가 구형 기기에서 깨진다.
- 테스트 본문 구조는 `// Given` / `// When` / `// Then` 주석 블록으로 드러낸다.
- 코루틴 테스트는 `runTest`. `runBlocking`·`runBlockingTest` 금지.
- 상태를 제공하는 대역은 Fake. MockK는 `coVerify`로 상호작용을 검증하거나 Retrofit Service처럼 Fake 작성 비용이 과한 인터페이스를 대신할 때만 쓴다.
- 새 파일의 소스셋 디렉토리는 각 모듈의 `main`을 따른다 — `domain`·`data`는 `src/test/java/`, `core:util:*`·`core:designsystem`은 `src/test/kotlin/`·`src/androidTest/kotlin/`.
- 버전 카탈로그 alias의 Gradle 접근자는 케밥이 점으로 바뀐다 — `test-unit` 번들은 `libs.bundles.test.unit`, `parfait-test-unit` 플러그인은 `libs.plugins.parfait.test.unit`.

---

### Task 1: 버전 카탈로그 + `parfait-test-unit` 플러그인 + `core:util:jvm` 첫 테스트

이 Task 하나로 "테스트가 실행되는 배선"이 처음으로 성립한다. 카탈로그·플러그인·모듈 적용·실제 테스트를 한 묶음으로 두는 이유는, 셋 중 하나만 있어도 검증할 수 없기 때문이다.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Create: `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`
- Create: `build-logic/convention/src/main/kotlin/TestUnitConventionPlugin.kt`
- Modify: `build-logic/convention/build.gradle.kts`
- Modify: `core/util/jvm/build.gradle.kts`
- Test: `core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/extension/CharExtensionTest.kt`
- Test: `core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/model/DateFormatTest.kt`

**Interfaces:**
- Consumes: 없음 (첫 Task)
- Produces:
  - Gradle 플러그인 id `com.teamyg.parfait.plugin.test.unit`, 카탈로그 alias `libs.plugins.parfait.test.unit`
  - `com.teamyg.parfait.buildlogic.setConfigTestUnit(): Unit` — `Project` 확장, `internal`
  - 카탈로그 번들 `libs.bundles.test.unit`, `libs.bundles.test.android`
  - 카탈로그 라이브러리 `libs.androidx.compose.ui.test.junit4`, `libs.androidx.compose.ui.test.manifest`

- [ ] **Step 1: 브랜치 생성**

```bash
cd <TJYG-Android 경로>
git checkout develop
git pull
git checkout -b feature/#215-test-environment
```

- [ ] **Step 2: 버전 카탈로그에 테스트 의존성 추가**

`gradle/libs.versions.toml`의 `[versions]` 블록 끝(`firebase-crashlytics` 다음)에 추가:

```toml
# Test
junit4 = "4.13.2"
androidxTest = "1.7.0"
androidxTestExtJunit = "1.3.0"
turbine = "1.2.1"
mockk = "1.14.11"
```

`[libraries]` 블록 끝에 추가:

```toml
#Test - unit
junit4 = { group = "junit", name = "junit", version.ref = "junit4" }
kotlin-test = { group = "org.jetbrains.kotlin", name = "kotlin-test", version.ref = "kotlin" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
okhttp-mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver3", version.ref = "okhttp" }

#Test - android
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTest" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTest" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTest" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }

#Test - compose
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
```

`[bundles]` 블록 끝에 추가:

```toml
test-unit = [
    "junit4",
    "kotlin-test",
    "kotlinx-coroutines-test",
    "turbine",
    "mockk",
    "okhttp-mockwebserver",
]

test-android = [
    "junit4",
    "androidx-test-core",
    "androidx-test-runner",
    "androidx-test-rules",
    "androidx-test-ext-junit",
]
```

`[plugins]`의 `#Custom Plugin` 목록 끝에 추가:

```toml
parfait-test-unit = { id = "com.teamyg.parfait.plugin.test.unit" }
parfait-test-android = { id = "com.teamyg.parfait.plugin.test.android" }
parfait-test-compose = { id = "com.teamyg.parfait.plugin.test.compose" }
```

Compose 테스트 아티팩트에 버전이 없는 건 의도한 것이다 — `androidx-compose-bom`이 버전을 결정한다.

- [ ] **Step 3: `TestConfig.kt` 생성**

`build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`:

```kotlin
package com.teamyg.parfait.buildlogic

import com.teamyg.parfait.buildlogic.utils.extensions.libs
import com.teamyg.parfait.buildlogic.utils.extensions.testImplementation
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

internal fun Project.setConfigTestUnit() {
    dependencies {
        testImplementation(libs.bundles.test.unit)
    }
}
```

`testOptions`는 Android 확장에만 존재해서 JVM 모듈(`domain`·`core:util:jvm`)에서는 건드릴 수 없다. 이번 unit 설정에서 실제로 필요한 `testOptions` 항목이 없으므로(`isReturnDefaultValues`는 기본값 유지) 분기 자체를 두지 않는다.

`:core:testing` 의존은 여기 넣지 않는다 — 그 모듈이 Task 2에서 생기기 때문이다. 지금 넣으면 존재하지 않는 프로젝트를 참조해 빌드가 깨진다. import는 실제로 쓰는 것만 넣는다. ktlint의 `no-unused-imports`가 미사용 import를 에러로 잡는다.

- [ ] **Step 4: `TestUnitConventionPlugin.kt` 생성**

`build-logic/convention/src/main/kotlin/TestUnitConventionPlugin.kt`:

```kotlin
import com.teamyg.parfait.buildlogic.setConfigTestUnit

class TestUnitConventionPlugin : BaseConventionPlugin({
    setConfigTestUnit()
})
```

- [ ] **Step 5: 플러그인 등록**

`build-logic/convention/build.gradle.kts`의 `gradlePlugin { plugins { ... } }` 블록에서 `module.feature.api` 등록 다음에 추가:

```kotlin
        pluginRegister(
            pluginName = "test.unit",
            className = "TestUnit",
        )
```

- [ ] **Step 6: 테스트 파일 두 개 작성 (배선 전 — 실패 확인용)**

`core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/extension/CharExtensionTest.kt`:

```kotlin
package com.teamyg.parfait.core.util.jvm.extension

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharExtensionTest {
    @Test
    fun isKorean_completeSyllable_returnsTrue() {
        // Given 완성형 한글 음절
        val char = '가'

        // When isKorean 호출
        val result = char.isKorean()

        // Then 한글로 판정한다
        assertTrue(result)
    }

    @Test
    fun isKorean_lastCompleteSyllable_returnsTrue() {
        assertTrue('힣'.isKorean())
    }

    @Test
    fun isKorean_standaloneConsonant_returnsTrue() {
        // Given 단독 자음(ㄱ~ㆎ 구간)
        assertTrue('ㄱ'.isKorean())
    }

    @Test
    fun isKorean_standaloneVowel_returnsTrue() {
        // Given 단독 모음(ㅏ~ㅣ 구간)
        assertTrue('ㅏ'.isKorean())
    }

    @Test
    fun isKorean_latinLetter_returnsFalse() {
        assertFalse('a'.isKorean())
        assertFalse('Z'.isKorean())
    }

    @Test
    fun isKorean_digit_returnsFalse() {
        assertFalse('0'.isKorean())
    }

    @Test
    fun isKorean_whitespace_returnsFalse() {
        assertFalse(' '.isKorean())
    }

    @Test
    fun isKorean_symbol_returnsFalse() {
        assertFalse('!'.isKorean())
        assertFalse('_'.isKorean())
    }
}
```

`core/util/jvm/src/test/kotlin/com/teamyg/parfait/core/util/jvm/model/DateFormatTest.kt`:

```kotlin
package com.teamyg.parfait.core.util.jvm.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {
    private val date = LocalDate(2026, 8, 6)

    @Test
    fun fullMonthWithDay_augustSixth_returnsAugust06() {
        // Given 2026-08-06

        // When 전체 월 이름 + 일 포맷 적용
        val formatted = DateFormat.FullMonthWithDay.format(date)

        // Then "August 06" — day() 는 kotlinx-datetime 기본값이 Padding.ZERO 라 0이 붙는다
        assertEquals("August 06", formatted)
    }

    @Test
    fun abbreviatedDayOfWeek_thursday_returnsThu() {
        // Given 2026-08-06 은 목요일

        // When 요일 약어 포맷 적용
        val formatted = DateFormat.AbbreviatedDayOfWeek.format(date)

        // Then "Thu"
        assertEquals("Thu", formatted)
    }

    @Test
    fun monthDayFormat_augustSixth_hasNoDayPadding() {
        // Given 2026-08-06

        // When 축약 월 + 패딩 없는 일 포맷 적용
        val formatted = DateTextFormat.monthDayFormat.format(date)

        // Then "Aug 6" — 앞에 0이 붙지 않는다
        assertEquals("Aug 6", formatted)
    }
}
```

- [ ] **Step 7: 배선 없이 실행해 실패 확인 (RED)**

Run: `./gradlew :core:util:jvm:test`
Expected: FAIL. 테스트 의존성이 없어 `kotlin.test` import를 해석하지 못하고 컴파일 에러가 난다 (`Unresolved reference: test`).

기존 프로덕션 코드에 대한 특성화 테스트라 논리적 RED는 성립하지 않는다. 여기서 RED가 확인하는 건 "배선이 없으면 테스트가 못 돈다"는 사실이고, 다음 스텝의 GREEN이 배선의 효과를 증명한다.

- [ ] **Step 8: `core:util:jvm`에 플러그인 적용**

`core/util/jvm/build.gradle.kts`의 `plugins` 블록에 한 줄 추가:

```kotlin
plugins {
    alias(libs.plugins.parfait.kotlin.jvm)
    alias(libs.plugins.parfait.test.unit)
}
```

- [ ] **Step 9: 실행해 통과 확인 (GREEN)**

Run: `./gradlew :core:util:jvm:test`
Expected: PASS. `Tests run: 11, Failures: 0`

- [ ] **Step 10: 커밋**

```bash
git add gradle/libs.versions.toml \
        build-logic/convention/build.gradle.kts \
        build-logic/convention/src/main/kotlin/TestUnitConventionPlugin.kt \
        build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt \
        core/util/jvm
git commit -m "test: 유닛 테스트 컨벤션 플러그인 추가 및 core:util:jvm 배선

버전 카탈로그에 테스트 의존성·번들을 추가하고 parfait-test-unit 컨벤션
플러그인을 만들어 core:util:jvm에 적용했다. isKorean 경계와 날짜 포맷
특성화 테스트로 배선을 검증한다."
```

---

### Task 2: `:core:testing` 모듈 + `MainDispatcherRule` + `domain` 배선

**Files:**
- Create: `core/testing/build.gradle.kts`
- Create: `core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/MainDispatcherRule.kt`
- Modify: `settings.gradle.kts`
- Modify: `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`
- Modify: `domain/build.gradle.kts`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/CheckNameValidUseCaseTest.kt`

**Interfaces:**
- Consumes: `libs.plugins.parfait.test.unit`, `setConfigTestUnit()` (Task 1)
- Produces:
  - Gradle 모듈 `:core:testing` (`parfait-kotlin-jvm` 기반, `java-library`)
  - `com.teamyg.parfait.core.testing.MainDispatcherRule` — 생성자 `MainDispatcherRule(dispatcher: TestDispatcher = StandardTestDispatcher())`, 공개 프로퍼티 `val dispatcher: TestDispatcher`

- [ ] **Step 1: `:core:testing` 모듈 생성**

`core/testing/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.parfait.kotlin.jvm)
}

dependencies {
    api(projects.domain)

    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
```

`api`로 노출하는 이유는 이 모듈을 `testImplementation`으로 당기는 쪽에서 `MainDispatcherRule`의 타입(`TestRule`·`TestDispatcher`)과 `domain` 모델을 그대로 써야 하기 때문이다.

- [ ] **Step 2: `settings.gradle.kts`에 모듈 등록**

`include(":core:designsystem", ...)` 목록에 `":core:testing",`을 추가한다:

```kotlin
include(
    ":core:designsystem",
    ":core:ui",
    ":core:util:android",
    ":core:util:jvm",
    ":core:navigation",
    ":core:testing",
)
```

- [ ] **Step 3: `MainDispatcherRule` 작성**

`core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/MainDispatcherRule.kt`:

```kotlin
package com.teamyg.parfait.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * `Dispatchers.Main` 을 테스트 디스패처로 바꾼다.
 *
 * [dispatcher] 를 **공개**하는 이유: `runTest` 를 인자 없이 부르면 자기
 * `TestCoroutineScheduler` 를 새로 만들어서 `advanceUntilIdle()` 이 Main 쪽 큐를
 * 비우지 못한다. 호출부는 `runTest(mainDispatcherRule.dispatcher)` 로 명시 전달해
 * 스케줄러를 하나로 묶어야 한다.
 *
 * 기본값이 [StandardTestDispatcher] 인 이유: `UnconfinedTestDispatcher` 는 즉시
 * 디스패치라 실행 순서 버그를 감춘다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

- [ ] **Step 4: `TestConfig.kt`에 `:core:testing` 의존 추가**

이제 모듈이 존재하므로 `setConfigTestUnit()`에 한 줄을 더한다:

```kotlin
internal fun Project.setConfigTestUnit() {
    dependencies {
        testImplementation(libs.bundles.test.unit)
        testImplementation(project(":core:testing"))
    }
}
```

import에 `project` 확장을 추가한다:

```kotlin
import com.teamyg.parfait.buildlogic.utils.extensions.project
```

`:core:testing` 자신에게는 이 플러그인을 적용하지 않는다 — 자기 자신을 의존하게 된다.

**공용 Fake는 이번에 만들지 않는다.** 스펙은 `:core:testing`의 구성 요소로 공유 Fake와 도메인 픽스처를 들었지만, 이번 범위의 테스트 대상 중 Fake가 필요한 곳이 없다 — `CheckNameValidUseCase`는 의존이 없고, `PolicyRemoteDataSourceImpl`은 Retrofit Service를 MockK로 대신하는 편이 싸다. 쓰이지 않는 Fake를 미리 만들면 첫 사용자가 자기 요구에 맞춰 다시 고쳐야 한다. 모듈과 규약만 자리를 잡아 두고, 실제 Fake는 필요한 테스트가 생기는 시점에 추가한다.

- [ ] **Step 5: `domain`에 플러그인 적용**

`domain/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.parfait.module.domain)
    alias(libs.plugins.parfait.test.unit)
}
```

- [ ] **Step 6: 실패하는 테스트 작성**

`domain/src/test/java/com/teamyg/parfait/domain/usecase/CheckNameValidUseCaseTest.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase

import com.teamyg.parfait.domain.model.NameValidResult
import kotlin.test.Test
import kotlin.test.assertEquals

class CheckNameValidUseCaseTest {
    private val checkNameValid = CheckNameValidUseCase()

    @Test
    fun invoke_plainKoreanName_returnsSuccess() {
        // Given 공백 없는 한글 이름
        val name = "파르페"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 통과
        assertEquals(NameValidResult.Success, result)
    }

    @Test
    fun invoke_nameWithSingleInnerSpace_returnsSuccess() {
        // Given 가운데 공백 한 칸은 허용된다
        assertEquals(NameValidResult.Success, checkNameValid("우리 그룹"))
    }

    @Test
    fun invoke_alphanumericName_returnsSuccess() {
        assertEquals(NameValidResult.Success, checkNameValid("Team1"))
    }

    @Test
    fun invoke_leadingSpace_returnsSpaceAtEdge() {
        // Given 앞에 공백
        val name = " 파르페"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 가장자리 공백 오류
        assertEquals(NameValidResult.Error.SpaceAtEdge, result)
    }

    @Test
    fun invoke_trailingSpace_returnsSpaceAtEdge() {
        assertEquals(NameValidResult.Error.SpaceAtEdge, checkNameValid("파르페 "))
    }

    @Test
    fun invoke_consecutiveSpaces_returnsDuplicatedSpace() {
        // Given 연속 공백
        val name = "우리  그룹"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 연속 공백 오류
        assertEquals(NameValidResult.Error.DuplicatedSpace, result)
    }

    @Test
    fun invoke_emojiIncluded_returnsInvalidCharacter() {
        // Given 허용 문자 집합 밖의 문자
        val name = "파르페🍨"

        // When 유효성 검사
        val result = checkNameValid(name)

        // Then 문자 오류
        assertEquals(NameValidResult.Error.InvalidCharacter, result)
    }

    @Test
    fun invoke_symbolIncluded_returnsInvalidCharacter() {
        assertEquals(NameValidResult.Error.InvalidCharacter, checkNameValid("파르페!"))
    }

    @Test
    fun invoke_emptyString_returnsEmptyString() {
        // Given 빈 문자열

        // When 유효성 검사
        val result = checkNameValid("")

        // Then 빈 문자열 오류
        assertEquals(NameValidResult.Error.EmptyString, result)
    }

    @Test
    fun invoke_singleSpaceOnly_returnsSpaceAtEdge() {
        // Given 공백 한 칸만 입력
        // 검사 순서상 가장자리 공백이 빈 문자열보다 먼저 걸린다
        val result = checkNameValid(" ")

        // Then EmptyString 이 아니라 SpaceAtEdge
        assertEquals(NameValidResult.Error.SpaceAtEdge, result)
    }
}
```

마지막 케이스는 검사 순서를 고정하는 회귀 방어다. `NameValidation` enum의 선언 순서가 바뀌면 이 테스트가 깨진다.

- [ ] **Step 7: 실행해 실패 확인 (RED)**

Run: `./gradlew :domain:test --tests "*CheckNameValidUseCaseTest"`
Expected: 이 시점에 `:core:testing`이 이미 있으므로 컴파일은 통과하고 **테스트는 전부 PASS**한다. 기존 구현에 대한 특성화 테스트라 논리적 RED가 없다.

RED를 실제로 확인하려면 `invoke_singleSpaceOnly_returnsSpaceAtEdge`의 기대값을 `NameValidResult.Error.EmptyString`으로 바꿔 실행한다.
Expected: FAIL with `expected:<EmptyString> but was:<SpaceAtEdge>` — 이걸로 테스트가 실제로 구현을 호출하고 있음이 증명된다. 확인 후 기대값을 `SpaceAtEdge`로 되돌린다.

- [ ] **Step 8: 전체 실행해 통과 확인 (GREEN)**

Run: `./gradlew :core:util:jvm:test :domain:test`
Expected: PASS. 두 모듈 모두 통과.

- [ ] **Step 9: 커밋**

```bash
git add settings.gradle.kts core/testing domain \
        build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt
git commit -m "test: core:testing 모듈 신설 및 domain 유닛 테스트 배선

공용 테스트 유틸을 담을 :core:testing JVM 모듈을 만들고 MainDispatcherRule 을
추가했다. 룰이 dispatcher 를 공개하는 이유는 runTest 가 자기 스케줄러를 새로
만들어 advanceUntilIdle 이 Main 큐를 비우지 못하는 문제를 피하기 위해서다.

domain 에 플러그인을 적용하고 CheckNameValidUseCase 의 검사 규칙 4종과
적용 순서를 고정하는 테스트를 추가했다."
```

---

### Task 3: `DayWindow`에 `Clock` 주입 + 03시 경계 테스트

이 Task는 진짜 TDD다 — 현재 `DayWindow.current()`는 `Clock.System.now()`를 직접 호출해서 시각을 고정할 수 없고, 따라서 새벽 3시 경계 로직을 그 시각이 아니면 검증할 방법이 없다.

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/DayWindow.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/model/DayWindowTest.kt`

**Interfaces:**
- Consumes: `domain` 테스트 배선 (Task 2)
- Produces: `DayWindow.Companion.current(timeZone: TimeZone = TimeZone.currentSystemDefault(), clock: Clock = Clock.System): DayWindow`

- [ ] **Step 1: 실패하는 테스트 작성**

`domain/src/test/java/com/teamyg/parfait/domain/model/DayWindowTest.kt`:

```kotlin
package com.teamyg.parfait.domain.model

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class DayWindowTest {
    private val seoul = TimeZone.of("Asia/Seoul")

    private fun fixedClock(iso: String): Clock = object : Clock {
        override fun now(): Instant = Instant.parse(iso)
    }

    @Test
    fun current_justAfterBoundary_anchorsToSameDay() {
        // Given 한국 시간 8월 6일 03:00 정각 (UTC 로는 8월 5일 18:00)
        val clock = fixedClock("2026-08-05T18:00:00Z")

        // When 현재 윈도우 계산
        val window = DayWindow.current(timeZone = seoul, clock = clock)

        // Then 윈도우 시작은 8월 6일 03:00 이다
        assertEquals(
            Instant.parse("2026-08-05T18:00:00Z").toEpochMilliseconds(),
            window.startMs,
        )
    }

    @Test
    fun current_justBeforeBoundary_anchorsToPreviousDay() {
        // Given 한국 시간 8월 6일 02:59:59 (UTC 로는 8월 5일 17:59:59)
        val clock = fixedClock("2026-08-05T17:59:59Z")

        // When 현재 윈도우 계산
        val window = DayWindow.current(timeZone = seoul, clock = clock)

        // Then 윈도우 시작은 하루 전인 8월 5일 03:00 이다
        assertEquals(
            Instant.parse("2026-08-04T18:00:00Z").toEpochMilliseconds(),
            window.startMs,
        )
    }

    @Test
    fun current_anyMoment_windowSpans24Hours() {
        // Given 임의 시각
        val clock = fixedClock("2026-08-05T23:30:00Z")

        // When 현재 윈도우 계산
        val window = DayWindow.current(timeZone = seoul, clock = clock)

        // Then 길이는 정확히 24시간이다
        assertEquals(24L * 60 * 60 * 1000, window.endMs - window.startMs)
    }

    @Test
    fun contains_startBoundary_returnsTrue() {
        // Given 시작 3000, 끝 5000 인 윈도우
        val window = DayWindow(startMs = 3_000, endMs = 5_000)

        // When 시작 시각 자신을 검사

        // Then 포함한다 (닫힌 하한)
        assertTrue(3_000L in window)
    }

    @Test
    fun contains_endBoundary_returnsFalse() {
        // Given 시작 3000, 끝 5000 인 윈도우
        val window = DayWindow(startMs = 3_000, endMs = 5_000)

        // When 끝 시각 자신을 검사

        // Then 포함하지 않는다 (열린 상한)
        assertFalse(5_000L in window)
    }

    @Test
    fun contains_beforeStart_returnsFalse() {
        val window = DayWindow(startMs = 3_000, endMs = 5_000)
        assertFalse(2_999L in window)
    }

    @Test
    fun contains_insideRange_returnsTrue() {
        val window = DayWindow(startMs = 3_000, endMs = 5_000)
        assertTrue(4_999L in window)
    }
}
```

- [ ] **Step 2: 실행해 실패 확인 (RED)**

Run: `./gradlew :domain:test --tests "*DayWindowTest"`
Expected: FAIL — 컴파일 에러. `current()`에 `clock` 파라미터가 없어서 `No parameter with name 'clock' found`가 난다.

- [ ] **Step 3: `DayWindow.current()`에 `clock` 파라미터 추가**

`domain/src/main/java/com/teamyg/parfait/domain/model/DayWindow.kt`의 `current` 함수를 아래로 바꾼다. 시그니처 두 줄과 `Clock.System.now()` → `clock.now()` 한 곳만 변경된다:

```kotlin
        fun current(
            timeZone: TimeZone = TimeZone.currentSystemDefault(),
            clock: Clock = Clock.System,
        ): DayWindow {
            val now: LocalDateTime = clock.now().toLocalDateTime(timeZone)
            val anchorDate: LocalDate = when (now.time >= LocalTime(DAY_BOUNDARY_HOUR, 0)) {
                true -> now.date
                false -> now.date.minus(1, DateTimeUnit.DAY)
            }

            val startInstant: Instant = anchorDate
                .atStartOfDayIn(timeZone)
                .plus(DAY_BOUNDARY_HOUR.hours)
            val endInstant: Instant = startInstant.plus(24.hours)

            return DayWindow(
                startMs = startInstant.toEpochMilliseconds(),
                endMs = endInstant.toEpochMilliseconds(),
            )
        }
```

`import kotlin.time.Clock`은 이미 파일에 있다. 기본값이 있어 기존 호출부는 변경할 필요가 없다.

- [ ] **Step 4: 실행해 통과 확인 (GREEN)**

Run: `./gradlew :domain:test`
Expected: PASS. `DayWindowTest` 7건 + `CheckNameValidUseCaseTest` 10건 모두 통과.

- [ ] **Step 5: 기존 호출부가 안 깨졌는지 확인**

Run: `./gradlew :domain:compileKotlin :data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 커밋**

```bash
git add domain
git commit -m "test: DayWindow 에 Clock 을 주입하고 03시 경계 테스트 추가

current() 가 Clock.System.now() 를 직접 불러 시각을 고정할 수 없었다.
기본값이 있는 clock 파라미터를 추가해 기존 호출부는 그대로 두고 경계
직전·직후와 contains 의 반열림 구간을 검증한다."
```

---

### Task 4: `data` 모듈 배선 + `VOMapper` 테스트

**Files:**
- Modify: `data/build.gradle.kts`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/policy/mapper/VOMapperTest.kt`

**Interfaces:**
- Consumes: `libs.plugins.parfait.test.unit` (Task 1)
- Produces: `data` 모듈의 `src/test/java` 소스셋 (Task 5·6이 여기에 파일을 추가한다)

- [ ] **Step 1: `data`에 플러그인 적용**

`data/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.parfait.module.data)
    alias(libs.plugins.parfait.test.unit)
}

android {
    namespace = "com.teamyg.parfait.data"
}
```

- [ ] **Step 2: 실패하는 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/policy/mapper/VOMapperTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.policy.mapper

import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.id.TermsId
import com.teamyg.parfait.domain.model.policy.PolicyType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VOMapperTest {
    private fun itemResponse(
        termsId: Long = 1L,
        type: String = "TERMS_OF_SERVICE",
        title: String = "이용약관",
        url: String = "https://example.com/terms",
        required: Boolean = true,
    ) = PolicyItemResponse(
        termsId = termsId,
        type = type,
        title = title,
        url = url,
        required = required,
    )

    @Test
    fun toPolicyVO_termsOfServiceType_mapsAllFields() {
        // Given 서버가 준 약관 항목
        val response = itemResponse(termsId = 7L, required = false)

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then 모든 필드가 그대로 옮겨진다
        assertEquals(TermsId(7L), vo.termsId)
        assertEquals(PolicyType.TERMS_OF_SERVICE, vo.type)
        assertEquals("이용약관", vo.title)
        assertEquals("https://example.com/terms", vo.url)
        assertEquals(false, vo.required)
    }

    @Test
    fun toPolicyVO_privacyPolicyType_mapsToPrivacyPolicy() {
        // Given 개인정보 처리방침 타입
        val response = itemResponse(type = "PRIVACY_POLICY")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then 대응 enum 으로 매핑된다
        assertEquals(PolicyType.PRIVACY_POLICY, vo.type)
    }

    @Test
    fun toPolicyVO_unknownType_mapsToUnknown() {
        // Given 클라이언트가 모르는 타입 문자열
        val response = itemResponse(type = "MARKETING_CONSENT")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then UNKNOWN 으로 떨어진다 — 예외를 던지지 않는다
        assertEquals(PolicyType.UNKNOWN, vo.type)
    }

    @Test
    fun toPolicyVO_lowercaseType_mapsToUnknown() {
        // Given 대소문자가 다른 타입 (매핑은 정확히 일치할 때만 성립한다)
        val response = itemResponse(type = "terms_of_service")

        // When VO 로 변환
        val vo = response.toPolicyVO()

        // Then UNKNOWN
        assertEquals(PolicyType.UNKNOWN, vo.type)
    }

    @Test
    fun toPolicyVOList_multipleItems_preservesOrder() {
        // Given 두 건이 담긴 응답
        val response = PolicyResponse(
            policies = listOf(
                itemResponse(termsId = 1L, type = "TERMS_OF_SERVICE"),
                itemResponse(termsId = 2L, type = "PRIVACY_POLICY"),
            ),
        )

        // When 리스트로 변환
        val vos = response.toPolicyVOList()

        // Then 순서와 개수가 유지된다
        assertEquals(2, vos.size)
        assertEquals(TermsId(1L), vos[0].termsId)
        assertEquals(TermsId(2L), vos[1].termsId)
    }

    @Test
    fun toPolicyVOList_emptyPolicies_returnsEmptyList() {
        // Given 빈 응답
        val response = PolicyResponse(policies = emptyList())

        // When 리스트로 변환
        val vos = response.toPolicyVOList()

        // Then 빈 리스트
        assertTrue(vos.isEmpty())
    }
}
```

`toPolicyVO`·`toPolicyVOList`는 `internal`이라 같은 모듈의 `src/test`에서 접근 가능하다.

- [ ] **Step 3: 실행해 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*VOMapperTest"`
Expected: PASS, 6건.

RED를 확인하려면 `toPolicyVO_unknownType_mapsToUnknown`의 기대값을 `PolicyType.TERMS_OF_SERVICE`로 바꿔 실행한다.
Expected: FAIL with `expected:<TERMS_OF_SERVICE> but was:<UNKNOWN>`. 확인 후 되돌린다.

- [ ] **Step 4: 커밋**

```bash
git add data
git commit -m "test: data 모듈 유닛 테스트 배선 및 정책 매퍼 테스트 추가

PolicyResponse 에서 PolicyVO 로의 변환과, 모르는 타입 문자열이 예외 대신
UNKNOWN 으로 떨어지는 동작을 고정한다."
```

---

### Task 5: `PolicyRemoteDataSourceImpl` 테스트 (MockK로 Service 대역)

**Files:**
- Test: `data/src/test/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `data` 테스트 배선 (Task 4)
- Produces: 없음 (테스트만 추가)

- [ ] **Step 1: 실패하는 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.policy.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.PolicyService
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyItemResponse
import com.teamyg.parfait.data.service.model.response.policy.PolicyResponse
import com.teamyg.parfait.domain.model.policy.PolicyType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PolicyRemoteDataSourceImplTest {
    private val policyService: PolicyService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = PolicyRemoteDataSourceImpl(
        policyService = policyService,
        apiCaller = apiCaller,
    )

    private fun successResponse(vararg types: String) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = PolicyResponse(
            policies = types.mapIndexed { index, type ->
                PolicyItemResponse(
                    termsId = index.toLong(),
                    type = type,
                    title = "약관 $index",
                    url = "https://example.com/$index",
                    required = true,
                )
            },
        ),
    )

    @Test
    fun getPolicies_serviceReturnsSuccess_returnsMappedVoList() = runTest {
        // Given 서비스가 약관 두 건을 성공 응답으로 준다
        coEvery { policyService.getPolicies() } returns
            successResponse("TERMS_OF_SERVICE", "PRIVACY_POLICY")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then VO 리스트로 매핑된 성공 결과
        val policies = result.getOrThrow()
        assertEquals(2, policies.size)
        assertEquals(PolicyType.TERMS_OF_SERVICE, policies[0].type)
        assertEquals(PolicyType.PRIVACY_POLICY, policies[1].type)
    }

    @Test
    fun getPolicies_onceCalled_delegatesToServiceExactlyOnce() = runTest {
        // Given 성공 응답
        coEvery { policyService.getPolicies() } returns successResponse("TERMS_OF_SERVICE")

        // When 정책 조회
        dataSource.getPolicies()

        // Then 서비스를 정확히 한 번만 호출한다 (중복 호출 회귀 방어)
        coVerify(exactly = 1) { policyService.getPolicies() }
    }

    @Test
    fun getPolicies_businessFailure_returnsBusinessException() = runTest {
        // Given 서버가 success=false 로 응답
        coEvery { policyService.getPolicies() } returns ApiResponse(
            success = false,
            code = "POLICY_NOT_FOUND",
            message = "약관을 찾을 수 없습니다",
            data = null,
        )

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("POLICY_NOT_FOUND", error.code)
        assertEquals("약관을 찾을 수 없습니다", error.serverMessage)
    }

    @Test
    fun getPolicies_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { policyService.getPolicies() } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }

    @Test
    fun getPolicies_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { policyService.getPolicies() } throws IOException("connection reset")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun getPolicies_unexpectedException_returnsUnknownException() = runTest {
        // Given 예상 못 한 예외
        coEvery { policyService.getPolicies() } throws IllegalStateException("boom")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then Unknown 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Unknown>(result.exceptionOrNull())
    }
}
```

`PolicyService`를 Fake가 아니라 MockK로 대신하는 이유는 Retrofit 인터페이스라 각 테스트가 서로 다른 예외·응답을 던지게 해야 하고, Fake로 그 분기를 다 만들면 오히려 배보다 배꼽이 커지기 때문이다. `coVerify`가 있는 두 번째 케이스는 진짜 상호작용 검증이다.

- [ ] **Step 2: 실행해 통과 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*PolicyRemoteDataSourceImplTest"`
Expected: PASS, 6건.

- [ ] **Step 3: 커밋**

```bash
git add data
git commit -m "test: PolicyRemoteDataSourceImpl 의 성공·실패 경로 테스트 추가

Retrofit Service 를 MockK 로 대신해 성공 매핑, 비즈니스 실패, 빈 본문,
네트워크 예외, 예상 못 한 예외까지 ApiCaller 를 통과한 결과를 고정한다."
```

---

### Task 6: `AuthInterceptor` MockWebServer 테스트 + `ApiCaller` 단위 테스트

**Files:**
- Test: `data/src/test/java/com/teamyg/parfait/data/network/AuthInterceptorTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/network/ApiCallerTest.kt`

**Interfaces:**
- Consumes: `data` 테스트 배선 (Task 4)
- Produces: 없음 (테스트만 추가)

- [ ] **Step 1: `AuthInterceptor` 테스트 작성**

`AuthInterceptor`는 요청의 Retrofit `Invocation` 태그를 읽어 `@NoAuth` 여부를 판정한다. 그 태그는 Retrofit이 실제로 호출을 만들 때만 붙기 때문에, OkHttp `Request`를 직접 만들어 검증할 수 없다. Retrofit + MockWebServer를 실제로 세워야 한다.

`data/src/test/java/com/teamyg/parfait/data/network/AuthInterceptorTest.kt`:

```kotlin
package com.teamyg.parfait.data.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 테스트 전용 API. 프로덕션 Service 를 쓰지 않는 이유는 인증이 필요한
 * 엔드포인트와 [NoAuth] 엔드포인트를 한 인터페이스에서 나란히 두고 비교해야 하기
 * 때문이다. 반환 타입이 [ResponseBody] 인 건 Retrofit 내장 컨버터만으로 동작해
 * 직렬화 설정을 끌어들이지 않기 위해서다.
 */
private interface TestApi {
    @GET("authed")
    suspend fun authed(): ResponseBody

    @NoAuth
    @GET("open")
    suspend fun open(): ResponseBody
}

class AuthInterceptorTest {
    private lateinit var server: MockWebServer

    private fun createApi(token: String?): TestApi {
        val tokenProvider = object : TokenProvider {
            override fun getToken(): String? = token
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()

        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .build()
            .create(TestApi::class.java)
    }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun intercept_tokenPresentAndEndpointRequiresAuth_addsBearerHeader() = runTest {
        // Given 토큰이 있고 인증이 필요한 엔드포인트
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        val api = createApi(token = "abc123")

        // When 호출
        api.authed()

        // Then Authorization 헤더가 붙는다
        val recorded = server.takeRequest()
        assertEquals("Bearer abc123", recorded.headers["Authorization"])
    }

    @Test
    fun intercept_endpointAnnotatedNoAuth_omitsHeader() = runTest {
        // Given 토큰이 있어도 @NoAuth 가 붙은 엔드포인트
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        val api = createApi(token = "abc123")

        // When 호출
        api.open()

        // Then Authorization 헤더를 붙이지 않는다
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun intercept_tokenAbsent_omitsHeader() = runTest {
        // Given 토큰이 없다 (미로그인)
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        val api = createApi(token = null)

        // When 인증이 필요한 엔드포인트 호출
        api.authed()

        // Then 헤더 없이 나간다 — 빈 Bearer 를 보내지 않는다
        val recorded = server.takeRequest()
        assertNull(recorded.headers["Authorization"])
    }

    @Test
    fun intercept_anyRequest_preservesPathAndMethod() = runTest {
        // Given 임의 호출
        server.enqueue(MockResponse.Builder().code(200).body("{}").build())
        val api = createApi(token = "abc123")

        // When 호출
        api.authed()

        // Then 경로·메서드를 바꾸지 않는다
        val recorded = server.takeRequest()
        assertEquals("/authed", recorded.url.encodedPath)
        assertEquals("GET", recorded.method)
    }
}
```

- [ ] **Step 2: 실행해 확인**

Run: `./gradlew :data:testDebugUnitTest --tests "*AuthInterceptorTest"`
Expected: PASS, 4건.

`mockwebserver3` 5.4.0의 API 이름이 위와 다르면(예: `takeRequest()` 반환 타입의 프로퍼티명, `server.close()` vs `server.shutdown()`) 컴파일 에러 메시지가 정확한 이름을 알려준다. 그에 맞춰 고치되 검증 내용은 바꾸지 않는다.

- [ ] **Step 3: `ApiCaller` 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/network/ApiCallerTest.kt`:

```kotlin
package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.service.model.response.ApiResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ApiCallerTest {
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })

    private fun success(data: String?) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = data,
    )

    @Test
    fun safeApiCall_successWithData_returnsData() = runTest {
        // Given 데이터가 담긴 성공 응답
        // When safeApiCall
        val result = apiCaller.safeApiCall { success("payload") }

        // Then 데이터를 그대로 돌려준다
        assertEquals("payload", result.getOrThrow())
    }

    @Test
    fun safeApiCall_successWithTransform_appliesTransform() = runTest {
        // Given 데이터가 담긴 성공 응답
        // When 변환 함수와 함께 호출
        val result = apiCaller.safeApiCall(
            block = { success("payload") },
            transform = { it.length },
        )

        // Then 변환 결과를 돌려준다
        assertEquals(7, result.getOrThrow())
    }

    @Test
    fun safeApiCall_successWithNullData_returnsEmptyBody() = runTest {
        // Given success=true 인데 data 가 null
        // When safeApiCall
        val result = apiCaller.safeApiCall { success(null) }

        // Then EmptyBody 로 실패한다
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }

    @Test
    fun safeApiCall_businessFailure_returnsBusinessWithNullStatusCode() = runTest {
        // Given success=false 인 응답
        val response = ApiResponse(
            success = false,
            code = "INVALID_NAME",
            message = "이름이 올바르지 않습니다",
            data = null,
            errorDetail = mapOf("field" to "name"),
        )

        // When safeApiCall
        val result = apiCaller.safeApiCall { response }

        // Then Business 예외. HTTP 계층을 거치지 않았으므로 statusCode 는 null 이다
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_NAME", error.code)
        assertEquals(null, error.statusCode)
        assertEquals(mapOf("field" to "name"), error.errorDetail)
    }

    @Test
    fun safeApiCall_ioException_returnsNetwork() = runTest {
        // Given 네트워크 예외
        // When safeApiCall
        val result = apiCaller.safeApiCall<String> { throw IOException("timeout") }

        // Then Network 로 감싼다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun safeApiCallWithoutData_success_returnsUnit() = runTest {
        // Given 본문 없는 성공 응답
        val response = ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = Unit,
        )

        // When safeApiCallWithoutData
        val result = apiCaller.safeApiCallWithoutData { response }

        // Then data 가 없어도 성공이다
        assertTrue(result.isSuccess)
        assertEquals(Unit, result.getOrThrow())
    }

    @Test
    fun safeApiCallNoContent_blockSucceeds_returnsSuccess() = runTest {
        // Given 응답 본문 자체가 없는 호출
        // When safeApiCallNoContent
        val result = apiCaller.safeApiCallNoContent { }

        // Then 성공
        assertTrue(result.isSuccess)
    }

    @Test
    fun safeApiCallNoContent_blockThrowsIo_returnsNetwork() = runTest {
        // Given 블록이 IO 예외를 던진다
        // When safeApiCallNoContent
        val result = apiCaller.safeApiCallNoContent { throw IOException("reset") }

        // Then Network 로 감싼다
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
}
```

- [ ] **Step 4: 실행해 통과 확인**

Run: `./gradlew :data:testDebugUnitTest`
Expected: PASS. `VOMapperTest` 6건 + `PolicyRemoteDataSourceImplTest` 6건 + `AuthInterceptorTest` 4건 + `ApiCallerTest` 8건.

- [ ] **Step 5: 커밋**

```bash
git add data
git commit -m "test: AuthInterceptor 와 ApiCaller 테스트 추가

AuthInterceptor 는 Retrofit Invocation 태그로 @NoAuth 를 판정해서 Request 를
직접 만들어서는 검증할 수 없다. MockWebServer 위에 Retrofit 을 세워 토큰 부착,
@NoAuth 생략, 토큰 부재 세 경로를 확인한다.

ApiCaller 는 성공·변환·빈 본문·비즈니스 실패·네트워크 예외의 Result 매핑을
고정한다."
```

---

### Task 7: `parfait-test-android` + `core:util:android` 배선 + 계측 스모크

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`
- Create: `build-logic/convention/src/main/kotlin/TestAndroidConventionPlugin.kt`
- Modify: `build-logic/convention/build.gradle.kts`
- Modify: `core/util/android/build.gradle.kts`
- Test: `core/util/android/src/androidTest/kotlin/com/teamyg/parfait/core/util/android/extension/ContextExtensionTest.kt`

**Interfaces:**
- Consumes: `setConfigTestUnit()` (Task 1), `libs.bundles.test.android` (Task 1)
- Produces:
  - Gradle 플러그인 id `com.teamyg.parfait.plugin.test.android`, alias `libs.plugins.parfait.test.android`
  - `com.teamyg.parfait.buildlogic.setConfigTestAndroid(): Unit` — `Project` 확장, `internal`

- [ ] **Step 1: `setConfigTestAndroid()` 추가**

`build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`의 `setConfigTestUnit()` 아래에 추가:

```kotlin
internal fun Project.setConfigTestAndroid() {
    dependencies {
        androidTestImplementation(libs.bundles.test.android)
        androidTestImplementation(project(":core:testing"))
    }

    val applicationExtension: ApplicationExtension? =
        extensions.findByType(ApplicationExtension::class)
    val libraryExtension: LibraryExtension? =
        extensions.findByType(LibraryExtension::class)

    when {
        applicationExtension != null -> applicationExtension.configureInstrumentationTest()
        libraryExtension != null -> libraryExtension.configureInstrumentationTest()
        else -> error("must be applied com.android.application or com.android.library")
    }
}

private fun ApplicationExtension.configureInstrumentationTest() {
    defaultConfig.testInstrumentationRunner = ANDROID_JUNIT_RUNNER
    testOptions.animationsDisabled = true
}

private fun LibraryExtension.configureInstrumentationTest() {
    defaultConfig.testInstrumentationRunner = ANDROID_JUNIT_RUNNER
    testOptions.animationsDisabled = true
}

private const val ANDROID_JUNIT_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
```

파일 상단 import에 네 개를 추가한다 (Task 1에서는 쓰지 않아 넣지 않았던 것들이다):

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.utils.extensions.androidTestImplementation
import org.gradle.kotlin.dsl.findByType
```

`testInstrumentationRunner`는 `setConfigAndroidLibrary()`·`setConfigAndroidApplication()`에도 이미 있다. 중복 지정이지만 값이 같아 충돌하지 않고, 이 플러그인만 붙여도 계측 테스트가 도는 자족성이 생긴다.

- [ ] **Step 2: `TestAndroidConventionPlugin.kt` 생성**

`build-logic/convention/src/main/kotlin/TestAndroidConventionPlugin.kt`:

```kotlin
import com.teamyg.parfait.buildlogic.setConfigTestAndroid

class TestAndroidConventionPlugin : BaseConventionPlugin({
    setConfigTestAndroid()
})
```

- [ ] **Step 3: 플러그인 등록**

`build-logic/convention/build.gradle.kts`의 `test.unit` 등록 다음에 추가:

```kotlin
        pluginRegister(
            pluginName = "test.android",
            className = "TestAndroid",
        )
```

- [ ] **Step 4: `core:util:android`에 플러그인 두 개 적용**

`core/util/android/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.parfait.test.unit)
    alias(libs.plugins.parfait.test.android)
}

android {
    namespace = "com.teamyg.parfait.core.util.android"
}

dependencies {
    implementation(projects.core.util.jvm)

    implementation(libs.androidx.core.ktx)
}
```

이 모듈에는 유닛 테스트 대상이 없다 — 내용물이 Compose `Modifier` 확장·`Context`/`Bitmap` 확장·권한 매니저라 전부 Android 프레임워크나 Compose 런타임을 탄다. `parfait-test-unit`은 소스셋과 의존성만 준비해 두고 실제 테스트는 0개로 시작한다.

- [ ] **Step 5: 계측 스모크 테스트 작성**

`core/util/android/src/androidTest/kotlin/com/teamyg/parfait/core/util/android/extension/ContextExtensionTest.kt`:

```kotlin
package com.teamyg.parfait.core.util.android.extension

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class ContextExtensionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun buildAppSettingsIntent_anyContext_targetsApplicationDetailsSettings() {
        // Given 애플리케이션 컨텍스트

        // When 앱 설정 인텐트 생성
        val intent = context.buildAppSettingsIntent()

        // Then 앱 상세 설정 화면을 가리킨다
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
    }

    @Test
    fun buildAppSettingsIntent_anyContext_encodesOwnPackageName() {
        // Given 애플리케이션 컨텍스트

        // When 앱 설정 인텐트 생성
        val intent = context.buildAppSettingsIntent()

        // Then data 에 자기 패키지명이 담긴다
        assertEquals("package", intent.data?.scheme)
        assertEquals(context.packageName, intent.data?.schemeSpecificPart)
    }

    @Test
    fun buildAppSettingsIntent_anyContext_setsNewTaskFlag() {
        // Given 애플리케이션 컨텍스트

        // When 앱 설정 인텐트 생성
        val intent = context.buildAppSettingsIntent()

        // Then NEW_TASK 플래그가 설정된다
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
```

계측 소스셋에서는 `kotlin.test` 대신 `org.junit.Assert`를 쓴다. `kotlin-test`는 `test-unit` 번들에만 있고 `androidTestImplementation`에는 없다.

- [ ] **Step 6: 컴파일 검증**

Run: `./gradlew :core:util:android:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL

에뮬레이터를 띄우지 않으므로 실행은 하지 않는다. 이 스텝이 검증하는 건 의존성 좌표·러너 설정·소스셋 배선이 맞다는 것이다.

- [ ] **Step 7: 유닛 테스트 태스크도 도는지 확인**

Run: `./gradlew :core:util:android:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. 테스트 0건이라 `NO-SOURCE`로 표시될 수 있다 — 정상이다.

- [ ] **Step 8: 커밋**

```bash
git add build-logic core/util/android
git commit -m "test: 계측 테스트 컨벤션 플러그인 추가 및 core:util:android 배선

parfait-test-android 를 만들어 androidx.test 의존성과 러너, animationsDisabled
를 건다. core:util:android 에 적용하고 Context.buildAppSettingsIntent 계측
테스트로 배선을 검증한다. 이 모듈은 내용물이 전부 Android·Compose 의존이라
유닛 테스트는 0건으로 시작한다."
```

---

### Task 8: `parfait-test-compose` + `core:designsystem` Compose 스모크

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`
- Create: `build-logic/convention/src/main/kotlin/TestComposeConventionPlugin.kt`
- Modify: `build-logic/convention/build.gradle.kts`
- Modify: `core/designsystem/build.gradle.kts`
- Test: `core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/theme/YGThemeSmokeTest.kt`

**Interfaces:**
- Consumes: `setConfigTestAndroid()` (Task 7), `libs.androidx.compose.ui.test.junit4`·`libs.androidx.compose.ui.test.manifest` (Task 1)
- Produces: Gradle 플러그인 id `com.teamyg.parfait.plugin.test.compose`, alias `libs.plugins.parfait.test.compose`

- [ ] **Step 1: `setConfigTestCompose()` 추가**

`TestConfig.kt` 끝에 추가:

```kotlin
internal fun Project.setConfigTestCompose() {
    dependencies {
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)

        debugImplementation(libs.androidx.compose.ui.test.manifest)
    }
}
```

import에 `debugImplementation`을 추가한다:

```kotlin
import com.teamyg.parfait.buildlogic.utils.extensions.debugImplementation
```

`ui-test-manifest`가 `debugImplementation`인 건 선택이 아니다. 이 아티팩트는 `<activity android:name="androidx.activity.ComponentActivity">` 항목을 병합 매니페스트에 넣는 역할인데, `androidTestImplementation`에 걸면 병합이 안 돼 `createComposeRule()`이 `ActivityNotFoundException`으로 죽는다. `TestManifestGradleConfiguration` lint가 이 실수를 잡는다.

- [ ] **Step 2: `TestComposeConventionPlugin.kt` 생성**

`build-logic/convention/src/main/kotlin/TestComposeConventionPlugin.kt`:

```kotlin
import com.teamyg.parfait.buildlogic.setConfigTestCompose

class TestComposeConventionPlugin : BaseConventionPlugin({
    setConfigTestCompose()
})
```

이 플러그인은 `parfait-test-android`를 자동으로 적용하지 않는다. 어느 의존성이 어느 플러그인에서 왔는지 추적 가능하도록 연쇄 적용을 피한 설계다. 계측 러너가 필요하면 모듈에서 두 플러그인을 함께 적용한다.

- [ ] **Step 3: 플러그인 등록**

`build-logic/convention/build.gradle.kts`의 `test.android` 등록 다음에 추가:

```kotlin
        pluginRegister(
            pluginName = "test.compose",
            className = "TestCompose",
        )
```

- [ ] **Step 4: `core:designsystem`에 플러그인 적용**

`core/designsystem/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.parfait.android.library)
    alias(libs.plugins.parfait.jetpack.compose)
    alias(libs.plugins.parfait.test.android)
    alias(libs.plugins.parfait.test.compose)
}

android {
    namespace = "com.teamyg.parfait.core.designsystem"
}

dependencies {
    implementation(projects.core.util.android)
    implementation(projects.core.util.jvm)
}
```

- [ ] **Step 5: Compose 스모크 테스트 작성**

`core/designsystem/src/androidTest/kotlin/com/teamyg/parfait/core/designsystem/theme/YGThemeSmokeTest.kt`:

```kotlin
package com.teamyg.parfait.core.designsystem.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * Compose 테스트 배선이 살아 있는지 확인하는 스모크 테스트.
 *
 * `createComposeRule()` 이 `androidx.activity.ComponentActivity` 를 띄우므로
 * `ui-test-manifest` 가 `debugImplementation` 에 제대로 걸려 있지 않으면 여기서
 * `ActivityNotFoundException` 이 난다.
 */
@MediumTest
class YGThemeSmokeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun ygCustomTheme_lightTheme_providesColorSchemeToContent() {
        // Given 라이트 테마로 감싼 컨텐츠
        var capturedBackground: Any? = null

        composeTestRule.setContent {
            YGCustomTheme(darkTheme = false) {
                capturedBackground = YGTheme.colorScheme
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(SMOKE_TAG),
                )
            }
        }

        // When 컴포지션 완료

        // Then CompositionLocal 이 채워지고 컨텐츠가 그려진다
        composeTestRule.onNodeWithTag(SMOKE_TAG).assertIsDisplayed()
        assertNotNull(capturedBackground)
    }

    @Test
    fun composeTestRule_clickOnTaggedNode_updatesState() {
        // Given 클릭할 때마다 증가하는 상태
        composeTestRule.setContent {
            var count by remember { mutableStateOf(0) }

            YGCustomTheme(darkTheme = false) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("$COUNTER_TAG$count"),
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(SMOKE_TAG)
                        .clickable { count += 1 },
                )
            }
        }

        // When 노드를 한 번 클릭
        composeTestRule.onNodeWithTag(SMOKE_TAG).performClick()

        // Then 재컴포지션이 일어나 태그가 바뀐다
        composeTestRule.onNodeWithTag("${COUNTER_TAG}1").assertIsDisplayed()
    }

    private companion object {
        const val SMOKE_TAG = "smoke"
        const val COUNTER_TAG = "counter-"
    }
}
```

`clickable`은 `androidx.compose.foundation`에 있고 `core:designsystem`이 Compose BOM으로 이미 당기고 있다. 이 두 번째 테스트가 확인하는 건 클릭 액션 주입과 재컴포지션 대기(`ComposeTestRule`의 자동 idle 동기화)가 실제로 동작한다는 것이다 — 첫 번째 테스트의 정적 렌더링만으로는 안 드러난다.

- [ ] **Step 6: 컴파일 검증**

Run: `./gradlew :core:designsystem:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: lint로 매니페스트 배선 확인**

Run: `./gradlew :core:designsystem:lintDebug`
Expected: `TestManifestGradleConfiguration` 경고가 **없어야** 한다. 나오면 `ui-test-manifest`가 잘못된 configuration에 걸린 것이다.

- [ ] **Step 8: 커밋**

```bash
git add build-logic core/designsystem
git commit -m "test: Compose UI 테스트 컨벤션 플러그인 추가 및 designsystem 스모크

parfait-test-compose 를 만들어 ui-test-junit4 를 androidTest 에,
ui-test-manifest 를 debugImplementation 에 건다. 후자를 androidTest 에 걸면
매니페스트 병합이 안 돼 createComposeRule 이 ActivityNotFoundException 으로
죽는다.

core:designsystem 에 적용하고 YGCustomTheme 의 CompositionLocal 제공과
재컴포지션을 확인하는 스모크 테스트를 추가했다."
```

---

### Task 9: CI 워크플로

**Files:**
- Create: `.github/workflows/test.yml`
- Modify: `.github/workflows/ktlint.yml`

**Interfaces:**
- Consumes: Task 1~8의 테스트 태스크
- Produces: 없음 (CI 산출물)

- [ ] **Step 1: `ktlint.yml`에서 테스트 스텝 제거**

`.github/workflows/ktlint.yml`에서 아래 두 줄을 삭제한다:

```yaml
      # Gradle test 수행
      - name: Test with Gradle
        run: ./gradlew --info test
```

지금까지는 테스트가 0개라 아무것도 안 하고 통과했다. 이 계획을 구현하면 실제로 테스트가 돌기 시작하므로, 새 워크플로와 중복 실행된다. `ktlint.yml`은 포매팅 검사만 담당하게 한다.

- [ ] **Step 2: `test.yml` 생성**

`.github/workflows/test.yml`:

```yaml
name: test

on:
  pull_request:
    branches: [ develop ]

jobs:
  unit-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      # JDK 17버전으로 설치
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      # Gradlew Cache를 통한 속도 향상
      - name: Cache Gradle packages
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      # Gradlew 실행을 위한 권한 부여
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      # build 를 위한 local.properties 생성
      - name: Create local.properties
        run: |
          echo "KAKAO_NATIVE_APP_KEY=${{ secrets.KAKAO_NATIVE_APP_KEY }}" >> local.properties

      # google-services.json 생성
      - name: Create google-services.json
        run: echo '${{ secrets.GOOGLE_SERVICES_JSON }}' > app/google-services.json

      # 유닛 테스트 수행 (JVM 모듈은 test, Android 모듈은 testDebugUnitTest)
      - name: Run unit tests
        run: |
          ./gradlew \
            :domain:test \
            :core:util:jvm:test \
            :data:testDebugUnitTest \
            :core:util:android:testDebugUnitTest

      # 실패해도 결과를 남긴다
      - name: Publish test report
        uses: dorny/test-reporter@v2
        if: always()
        with:
          name: Unit Test Results
          path: '**/build/test-results/**/TEST-*.xml'
          reporter: java-junit
          fail-on-error: false

      - name: Upload test reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-reports
          path: '**/build/reports/tests/**'
          retention-days: 7

      # 캐시 지우기
      - name: Cleanup Gradle Cache
        run: |
          rm -f ~/.gradle/caches/modules-2/modules-2.lock
          rm -f ~/.gradle/caches/modules-2/gc.properties
```

루트에서 `./gradlew test`를 돌리지 않는 이유는 두 가지다 — 전 모듈이 대상이 되어 이번 범위를 넘고, Android 모듈의 `test`는 debug·release 유닛 테스트를 둘 다 실행해 시간이 두 배로 든다.

`dorny/test-reporter`는 PR에 체크 결과를 쓰기 위해 `checks: write` 권한이 필요하다. 조직 설정에서 기본 `GITHUB_TOKEN` 권한이 read-only면 워크플로 상단에 아래를 추가한다:

```yaml
permissions:
  contents: read
  checks: write
  pull-requests: write
```

- [ ] **Step 3: 전체 테스트 로컬 실행으로 최종 확인**

Run:

```bash
./gradlew :domain:test :core:util:jvm:test \
          :data:testDebugUnitTest :core:util:android:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL. `domain` 17건, `core:util:jvm` 11건, `data` 24건, `core:util:android` 0건.

- [ ] **Step 4: 계측 테스트 컴파일 최종 확인**

Run: `./gradlew :core:util:android:assembleDebugAndroidTest :core:designsystem:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: ktlint 통과 확인**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

`function-naming` 규칙이 `메서드_상태_기대` 형태의 언더스코어를 거부하면 여기서 실패한다. 그 경우 루트 `build.gradle.kts`의 ktlint 설정에서 테스트 소스셋에 한해 해당 규칙을 비활성화한다:

```kotlin
ktlint {
    filter {
        exclude { it.file.path.contains("/src/test/") || it.file.path.contains("/src/androidTest/") }
    }
}
```

전체 제외가 과하다고 판단되면 `.editorconfig`에 아래를 넣어 규칙만 끈다:

```
[**/src/{test,androidTest}/**/*.kt]
ktlint_standard_function-naming = disabled
```

`.editorconfig` 방식을 먼저 시도한다 — 포매팅 검사 자체는 유지하면서 이름 규칙만 완화하기 때문이다.

- [ ] **Step 6: 커밋**

```bash
git add .github/workflows
git commit -m "ci: 유닛 테스트 워크플로 추가

PR 마다 4개 모듈의 유닛 테스트를 실행하고, 실패 여부와 무관하게 JUnit XML 을
PR 체크로 게시하고 HTML 리포트를 아티팩트로 올린다.

ktlint.yml 의 기존 test 스텝은 제거했다. 지금까지는 테스트가 0개라 무의미하게
통과했지만 이제는 중복 실행이 된다."
```

- [ ] **Step 7: 사람에게 push·PR 확인 요청**

push와 PR 생성은 사람 확인 후에 한다. 아래를 보고하고 대기한다.

- 브랜치명 `feature/#215-test-environment`, 커밋 9개
- 전체 테스트 통과 결과
- ktlint 통과 여부와, `.editorconfig` 완화를 적용했다면 그 사실

---

### Task 10: `domain` UseCase 테스트 (로직 보유 4건) + Fake 3종 + `Clock` DI 바인딩 — **보류(미실행)**

> **이 Task는 구현했다가 되돌렸다(2026-08-06).** 커밋 `a2b67ca7`·`35146ea0`으로 전부 구현하고
> 리뷰까지 통과했지만(테스트 58 → 72건), Fake 2종을 `:core:testing`에 넣고 나니 그 구조의 대가가
> 드러났다 — **Repository가 늘 때마다 Fake가 한 모듈에 쌓이고**, 그 모듈은 `setConfigTestUnit()`을 통해
> 모든 대상 모듈의 테스트 classpath에 걸려 있어 `data`용 Fake 하나를 고쳐도 `core:util:jvm` 테스트까지
> 재컴파일된다. 소유권도 어긋난다(`domain` repository의 Fake를 `core:testing`이 소유).
>
> 그래서 브랜치를 `c6d7a57c`(Task 9 완료 시점)로 되돌렸다. 폐기한 커밋은 reflog에 남아 있다.
> **Fake 배치 방식을 정한 뒤** 이 Task를 다시 실행한다 — 선택지는 스펙의 미결 항목 참고.
> 아래 본문은 그때 재사용할 수 있도록 그대로 둔다. 단 Fake의 위치는 재검토 결과에 따라 바뀐다.

Task 1~9로 기반이 섰고 `domain`에는 `CheckNameValidUseCase`·`DayWindow`만 테스트돼 있다. 남은
UseCase 10개 중 **실제 분기·변환이 있는 4개만** 다룬다. `DecodeImageUseCase`·`SegmentImageUseCase`·
`CreateCameraCacheUriUseCase(file)`는 한 줄 위임이라 테스트가 구현을 되읊는 데 그치고,
`CheckInviteCodeValidUseCase`·`SplashInitialUseCase`는 `// Todo`가 붙은 임시 구현이라 지금 못 박으면
안 된다. 의도적으로 제외한다.

이 Task에서 `:core:testing`이 처음으로 Fake를 갖게 되며, Task 1~9의 최종 수정 웨이브에서 지웠던
`api(projects.domain)`도 근거가 생겨 되살아난다(그때는 쓰는 곳이 없어서 지운 것이다).

**Files:**
- Modify: `core/testing/build.gradle.kts`
- Create: `core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/fake/FakeRecentImageRepository.kt`
- Create: `core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/fake/FakeGalleryRepository.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/GetRecentCacheImagesUseCase.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/SingletonInjectModule.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/image/AddRecentImageUseCaseTest.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/image/GetRecentCacheImagesUseCaseTest.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/gallery/GalleryImageGroupsUseCaseTest.kt`

**Interfaces:**
- Consumes: `libs.plugins.parfait.test.unit`, `:core:testing`, `libs.bundles.test.unit`(Turbine 포함)
- Produces:
  - `FakeRecentImageRepository` — `RecentImageRepository` 구현. 인메모리 상태 + 테스트 전용 헬퍼
    (`seed(vararg uri: String)`, `setLastModified(uri, millis)`, `setEvicted(List<String>)`,
    `failStore()`, 관찰용 `deletedUris: List<String>`, `removedFromMetadata: List<String>`)
  - `FakeGalleryRepository` — `GalleryRepository` 구현. `seedAll(...)`·`seedFiltered(...)`
  - `GetRecentCacheImagesUseCase(recentImageRepository, clock)` — 생성자에 `Clock` 추가

- [ ] **Step 1: `:core:testing`에 `domain` 의존 복구**

`core/testing/build.gradle.kts`의 `dependencies`에 한 줄 추가한다. 파일 상단 주석은 유지한다.

```kotlin
dependencies {
    api(projects.domain)

    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
}
```

이번엔 Fake가 `domain`의 repository 인터페이스와 모델을 구현하므로 `api`가 정당하다.

- [ ] **Step 2: `FakeRecentImageRepository` 작성**

`core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/fake/FakeRecentImageRepository.kt`:

```kotlin
package com.teamyg.parfait.core.testing.fake

import com.teamyg.parfait.domain.repository.image.RecentImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [RecentImageRepository] 의 인메모리 대역.
 *
 * 프로덕션 인터페이스에 없는 seed/관찰 헬퍼를 노출한다 — 테스트 소스셋에서만 소비되므로
 * 프로덕션 계약은 오염되지 않는다.
 */
class FakeRecentImageRepository : RecentImageRepository {
    private val cacheImages = MutableStateFlow<List<String>>(emptyList())
    private val lastModified = mutableMapOf<String, Long>()

    /** 다음 [addAndGetEvictedCacheFileName] 호출이 반환할 목록. */
    var evictedOnAdd: List<String> = emptyList()

    /** true 면 [storeRecentImageInInternalStorage] 가 예외를 던진다. */
    var storeFails: Boolean = false

    /** [storeRecentImageInInternalStorage] 가 돌려줄 안정 URI 접두사. */
    var storedUriPrefix: String = "stored://"

    val deletedUris = mutableListOf<String>()
    val removedFromMetadata = mutableListOf<List<String>>()
    val addedUris = mutableListOf<String>()

    override val recentCacheImages: Flow<List<String>> = cacheImages.asStateFlow()

    override suspend fun addAndGetEvictedCacheFileName(value: String): List<String> {
        addedUris += value
        cacheImages.value = cacheImages.value + value
        return evictedOnAdd
    }

    override suspend fun removeCacheFileName(values: List<String>) {
        removedFromMetadata += values
        cacheImages.value = cacheImages.value - values.toSet()
    }

    override suspend fun storeRecentImageInInternalStorage(sourceUri: String): String {
        if (storeFails) {
            throw IllegalStateException("store failed: $sourceUri")
        }
        return "$storedUriPrefix$sourceUri"
    }

    override suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean {
        deletedUris += sourceUri
        return true
    }

    override suspend fun getLastModifiedCacheFile(sourceUri: String): Long? = lastModified[sourceUri]

    fun seed(vararg uris: String) {
        cacheImages.value = uris.toList()
    }

    fun setLastModified(uri: String, millis: Long) {
        lastModified[uri] = millis
    }
}
```

- [ ] **Step 3: `FakeGalleryRepository` 작성**

`core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/fake/FakeGalleryRepository.kt`:

```kotlin
package com.teamyg.parfait.core.testing.fake

import com.teamyg.parfait.domain.repository.gallery.GalleryRepository
import kotlinx.datetime.LocalDate

/** [GalleryRepository] 의 인메모리 대역. */
class FakeGalleryRepository : GalleryRepository {
    private var all = LinkedHashMap<LocalDate, MutableList<String>>()
    private var filtered = LinkedHashMap<LocalDate, MutableList<String>>()

    override suspend fun loadAllGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>> = all

    override suspend fun loadFilterYGGalleryImages(): LinkedHashMap<LocalDate, MutableList<String>> = filtered

    fun seedAll(vararg entries: Pair<LocalDate, List<String>>) {
        all = entries.toLinkedHashMap()
    }

    fun seedFiltered(vararg entries: Pair<LocalDate, List<String>>) {
        filtered = entries.toLinkedHashMap()
    }

    private fun Array<out Pair<LocalDate, List<String>>>.toLinkedHashMap() =
        LinkedHashMap<LocalDate, MutableList<String>>().also { map ->
            forEach { (date, uris) -> map[date] = uris.toMutableList() }
        }
}
```

- [ ] **Step 4: 실패하는 테스트 3개 작성 (RED)**

세 파일 모두 작성한다. `GetRecentCacheImagesUseCaseTest`는 아직 존재하지 않는 `clock` 파라미터를
쓰므로 **컴파일 에러가 나야 한다** — 이 Task의 진짜 RED다.

`AddRecentImageUseCaseTest.kt`는 저장 실패 시 조기 반환(evict·삭제가 일어나지 않음)과 성공 시
evict된 항목이 전부 삭제되는 경로를 덮는다. `GalleryImageGroupsUseCaseTest.kt`는 두 로더가 맵의
순서를 보존하고 `toList()`로 방어 복사하는지(원본 `MutableList`를 나중에 바꿔도 결과가 안 변하는지)
확인한다. `GetRecentCacheImagesUseCaseTest.kt`는 고정 `Clock`으로 윈도우를 못 박고 Turbine으로
방출을 검증한다 — 윈도우 밖 항목이 메타데이터·파일 양쪽에서 지워지고, 윈도우 안 항목은 남으며,
지울 게 없으면 `removeCacheFileName`이 호출되지 않아야 한다.

각 테스트의 정확한 본문은 구현자가 이 Task의 규약(GWT·`runTest`·Fake 우선)에 맞춰 작성한다.

- [ ] **Step 5: 실행해 실패 확인 (RED)**

Run: `./gradlew :domain:test`
Expected: FAIL — `GetRecentCacheImagesUseCaseTest`가 `No parameter with name 'clock' found`로 컴파일 실패.

- [ ] **Step 6: `GetRecentCacheImagesUseCase`에 `Clock` 주입**

생성자에 `private val clock: Clock`을 추가하고, `clearOutsideDayWindow()`의
`DayWindow.current()`를 `DayWindow.current(clock = clock)`으로 바꾼다. **기본값을 주지 않는다** —
Hilt는 생성자 기본값을 쓰지 않으므로 기본값은 테스트에만 도움이 되고 프로덕션에서는 바인딩 누락을
숨긴다. `import kotlin.time.Clock`을 추가한다.

- [ ] **Step 7: Hilt 그래프에 `Clock` 바인딩**

`data/src/main/java/com/teamyg/parfait/data/di/SingletonInjectModule.kt`에 추가:

```kotlin
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.System
```

`import kotlin.time.Clock`을 추가한다. 시각에 의존하는 후속 코드도 같은 결을 따르게 된다.

- [ ] **Step 8: 실행해 통과 확인 (GREEN)**

Run: `./gradlew :domain:test`
Expected: PASS.

- [ ] **Step 9: Hilt 그래프가 실제로 조립되는지 확인**

Run: `./gradlew :app:kspDebugKotlin`
Expected: BUILD SUCCESSFUL. `Clock` 바인딩이 빠졌거나 중복이면 여기서 KSP가 잡는다.
유닛 테스트만으로는 DI 그래프 오류가 드러나지 않으므로 이 스텝을 생략하지 않는다.

- [ ] **Step 10: 전체 검증 + 커밋**

Run: `./gradlew test`, `./gradlew ktlintCheck`
Expected: 모두 BUILD SUCCESSFUL.

```bash
git add core/testing domain data
git commit -m "test: domain UseCase 4건 테스트 추가 및 Clock 을 DI 그래프에 바인딩

로직·분기가 실재하는 UseCase 만 다룬다. 한 줄 위임(Decode·Segment)과
// Todo 가 붙은 임시 구현(CheckInviteCodeValid·SplashInitial)은 제외했다.

GetRecentCacheImagesUseCase 가 DayWindow.current() 를 직접 불러 시각을
고정할 수 없었다. Hilt 는 생성자 기본값을 쓰지 않으므로 Clock 을 그래프에
바인딩하고 주입받는다.

:core:testing 에 Repository Fake 2종을 신설했다. 이 모듈이 domain 을
다시 의존하는 근거가 여기서 생긴다."
```

---

## 실행 중 드러난 계획 오류 (2026-08-06 기록)

이 계획은 아래 지점에서 틀렸다. 구현 중 발견해 고쳤고, 같은 실수를 반복하지 않도록 남긴다.

1. **`DateFormat.FullMonthWithDay` 기대값** — `day()`는 kotlinx-datetime 기본값이 `Padding.ZERO`라
   `"August 6"`이 아니라 `"August 06"`이다. 본문은 정정했다.
2. **`kotlin-test-junit` 누락** — Android 모듈의 유닛 테스트 classpath는 Kotlin Gradle 플러그인의
   `kotlin-test` → `kotlin-test-junit` 변형 치환을 받지 못한다. 순수 JVM 모듈만 자동으로 해결된다.
   카탈로그 alias와 `test-unit` 번들에 추가해야 `kotlin.test.Test`가 해석된다.
3. **순서 고정 테스트의 근거가 사실과 달랐다** — `CheckNameValidUseCase`에 입력 `" "`를 주면
   `CheckSpaceStartOrEnd`만 실패하므로 enum 선언 순서를 바꿔도 결과가 같다. 순서를 실제로
   고정하려면 `"  "`(공백 두 칸)처럼 두 규칙이 동시에 실패하는 입력이 필요하다.
4. **빈 GWT 블록** — 계획의 테스트 코드 곳곳에 `// Given`·`// When` 아래 문장이 없는 형태가 있다.
   Task 2·6·7·최종 리뷰에서 반복 지적됐다. 규약은 (a) 주석 아래 문장이 없으면 안 되고
   (b) 파일 안에서 한 스타일이며, 설정·실행·단언이 각각 한 줄인 테스트는 주석 없이 **완전히 비운다**.
5. **CI 모듈 목록 하드코딩** — 4개 모듈을 명시하면 이후 다른 모듈에 테스트가 생겨도 CI가 안 돌고
   초록불이 뜬다. 루트 `./gradlew test`로 바꾸고, Android 모듈의 release 유닛 테스트 중복은
   `beforeVariants`에서 `HostTestBuilder.UNIT_TEST_TYPE`을 끄는 방식으로 없앴다.
6. **`:core:testing`의 `api(projects.domain)`** — `MainDispatcherRule`은 `domain`을 쓰지 않는다.
   이 의존은 `core:util:jvm`을 자기 테스트 classpath에 되돌려 놓고 `core:designsystem` 계측 APK에
   `domain`을 통째로 끌어들였다. 삭제했고, 도메인 픽스처를 실제로 넣을 때 되살린다.
7. **`createComposeRule()` v1** — deprecated 경고가 난다. v2(`...junit4.v2.createComposeRule`)로 옮겼다.
   v2의 `StandardTestDispatcher`가 `MainDispatcherRule` 기본값과도 일치한다.
8. **Compose 스모크의 겹친 노드** — 카운터 `Box`와 클릭 `Box`를 부모 레이아웃 없이 같은 자리에 두면
   앞의 것이 완전히 가려진다. `assertIsDisplayed()`는 가림을 보지 않아 우연히 통과한다. `Column`으로 감쌌다.

## PR 리뷰 반영 (2026-08-09)

PR #219에 달린 리뷰 3건을 반영했다. 위 Task 본문의 코드 블록은 당시 상태이므로, 최종 형태는
[스펙](../../specs/archive/2026-08-06-unit-test-infrastructure.md)이 정본이다.

1. **CI 셋업 스텝 공통화** (Task 9). `test.yml`과 `ktlint.yml`이 셋업 6스텝을 글자 그대로
   공유한다는 지적. `.github/actions/setup-android-build`·`restore-app-secrets` composite action
   2개로 뽑았다. 옮기면서 3스텝이 사라졌다 — `gradle/actions/setup-gradle@v4`가 캐시를 직접
   관리하며 lock 파일을 제외하므로 수동 `actions/cache`와 `Cleanup Gradle Cache`가 불필요하고,
   `chmod +x gradlew`는 `gradlew`가 이미 `100755`라 처음부터 무의미했다.
   `test.yml` 82줄 → 55줄, `ktlint.yml` 53줄 → 25줄.
2. **`:core:testing`의 `api` 제거** (Task 2). `implementation`으로 충분하지 않냐는 질문.
   ABI만 보면 `api`가 정석이지만(`MainDispatcherRule`이 `TestWatcher`를 상속하고
   `TestDispatcher`를 노출) `bundles.test-unit`이 두 라이브러리를 소비자에 이미 넣어 재노출이
   중복이었다. `implementation`으로 내리는 전제로 `setConfigTestAndroid()`에서
   `androidTestImplementation(project(":core:testing"))`을 **제거**했다. 그 선을 남기면
   `test-android` 번들에 coroutines-test가 없어 계측 테스트에서 `TestDispatcher`가 깨진다.
   검증은 `domain`에 임시 테스트를 넣어 `@get:Rule` + `runTest(rule.dispatcher)`가 통과함을 보고 지웠다.
3. **VO 매퍼 테스트 축약** (Task 4). 단순 매핑에 테스트가 필요하냐는 질문. 절반 맞다 —
   `toPolicyType()`의 `else -> UNKNOWN` fallback은 크래시를 가르는 결정이라 남기고, 순수 필드
   복사와 `List.map` 순서 검증은 줄였다. 6건 → 4건. `VOMapperTest` → `PolicyVOMapperTest`로
   개명하고(VO가 늘 때 한 파일에 몰리는 것을 막는다) 규약 11·12를 스펙에 신설했다.
   테스트가 실제로 버그를 잡는지 뮤테이션 2건으로 확인했다 — `enumValueOf`로 교체하면 2건 실패,
   `title`·`url` 뒤바꾸면 1건 실패.

## 검증 요약

구현 완료 시점에 아래가 모두 성립해야 한다.

- [x] `./gradlew test` 통과 (모듈 목록 하드코딩은 위 "계획 오류" 5번에서 걷어냈다)
- [x] `./gradlew :core:util:android:assembleDebugAndroidTest :core:designsystem:assembleDebugAndroidTest` 통과
- [x] `./gradlew :core:designsystem:lintDebug`에 `TestManifestGradleConfiguration` 경고 없음
- [x] `./gradlew ktlintCheck` 통과
- [x] `grep -rn "runBlocking" --include="*.kt" */src/test` 결과 없음
- [x] `grep -rn "isReturnDefaultValues" build-logic` 결과 없음
- [x] 백틱 테스트 메서드명 없음 — `grep -rn 'fun \`' --include="*.kt" */src/test */src/androidTest` 결과 없음
- [x] `feature/#215-test-environment` 브랜치에 커밋 — 계획은 9개를 예상했고 리뷰 대응까지 포함해 최종 17개로 머지됐다

위 8항목은 `develop` 머지본(PR #219)에서 확인했다(2026-08-09).
