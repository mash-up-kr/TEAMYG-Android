---
id: unit-test-infrastructure
title: 유닛 테스트 기반 구조 (Unit Test Infrastructure)
status: implemented
category: build-spec
platforms: android
verified: 2026-08-09
related_code:
  - build-logic/convention/src/main/kotlin/BaseConventionPlugin.kt
  - build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/AndroidConfig.kt#setConfigAndroidLibrary
  - build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/KotlinJvmConfig.kt#setConfigKotlinJvm
  - build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt#setConfigTestUnit
  - build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt#setConfigTestAndroid
  - core/testing/build.gradle.kts
  - core/testing/src/main/kotlin/com/teamyg/parfait/core/testing/MainDispatcherRule.kt#MainDispatcherRule
  - .github/workflows/test.yml
  - .github/workflows/ktlint.yml
  - .github/actions/setup-android-build/action.yml
  - .github/actions/restore-app-secrets/action.yml
  - gradle/libs.versions.toml
  - domain/src/main/java/com/teamyg/parfait/domain/usecase/CheckNameValidUseCase.kt#CheckNameValidUseCase
  - domain/src/main/java/com/teamyg/parfait/domain/model/DayWindow.kt#DayWindow
  - core/util/jvm/src/main/kotlin/com/teamyg/parfait/core/util/jvm/extension/CharExtension.kt#isKorean
  - data/src/main/java/com/teamyg/parfait/data/source/policy/mapper/VOMapper.kt#toPolicyVO
  - data/src/main/java/com/teamyg/parfait/data/network/AuthInterceptor.kt#AuthInterceptor
  - data/src/main/java/com/teamyg/parfait/data/network/ApiCaller.kt#ApiCaller
related_adr: ADR-0003
related_spec:
related_architecture: module-structure
supersedes:
superseded_by:
tags: [spec, parfait, testing, build-logic]
---

# Spec: 유닛 테스트 기반 구조

> GitHub 이슈: `mash-up-kr/TJYG-Android#215` — "Unit Test가 가능한 구조를 설계"

## 목표

현재 저장소에는 테스트 소스셋과 테스트 의존성이 하나도 없다. 30개가 넘는 모듈 중
어디에도 `src/test/`가 존재하지 않고, 버전 카탈로그에 JUnit·코루틴 테스트·Fake 관련
아티팩트가 전혀 선언돼 있지 않다. 이 스펙은 테스트를 쓸 수 있는 최소 기반을 세우고,
로직이 실제로 있는 모듈 4개에 배선한 뒤, 팀이 따라 쓸 수 있는 시범 테스트와 규약을 남긴다.

컨벤션 플러그인은 unit·계측·Compose UI 3종을 모두 만들되, 실제 적용은 unit을 4개 모듈에,
계측·Compose는 검증용 스모크 모듈 1개씩에만 한다.

## 범위

**포함**

- 버전 카탈로그에 테스트 의존성·번들 추가
- 컨벤션 플러그인 3종 신설 (`parfait-test-unit` / `parfait-test-android` / `parfait-test-compose`)
- `:core:testing` 모듈 신설 (공용 Rule·Fake·픽스처)
- `domain` / `data` / `core:util:jvm` / `core:util:android` 4개 모듈에 unit 배선
- 시범 유닛 테스트 (순수 로직 · 매퍼 · DataSource · HTTP 계층)
- 스모크 계측 테스트 2건 (`core:util:android`, `core:designsystem`)
- `DayWindow.current()`에 `Clock` 주입 (테스트 가능성 확보를 위한 최소 프로덕션 변경)
- GitHub Actions 테스트 워크플로 + 실패 리포트·결과 업로드

**제외**

- Robolectric — 안 넣는다. Android 프레임워크 의존 코드는 이번 unit 테스트 대상이 아니다.
- `feature:*` 모듈 — ViewModel·화면이 데이터 미결선 상태라 테스트 대상으로 적합하지 않다.
  플러그인이 준비돼 있으므로 준비되는 시점에 붙인다.
- Hilt 테스트(`@HiltAndroidTest`·`@TestInstallIn`) — 계측·통합 테스트용이고 이번 범위 밖이다.
  유닛 테스트는 생성자 주입으로 직접 인스턴스화한다.
- 에뮬레이터 실행 — 스모크 계측 테스트는 `assembleDebugAndroidTest` 컴파일까지만 검증한다.
- 커버리지 측정(JaCoCo·Kover) · 스크린샷 테스트 · Gradle Managed Devices

## 라이브러리 스택

| 아티팩트 | 소스셋 | 용도 |
|---|---|---|
| `junit:junit` | test / androidTest | JUnit4 |
| `kotlin-test` | test | 어서션 (Kotlin 버전에 정렬) |
| `kotlinx-coroutines-test` | test | `runTest`·`StandardTestDispatcher`·가상 시간 |
| `app.cash.turbine:turbine` | test | Flow 방출 검증 |
| `io.mockk:mockk` | test | 상호작용 검증 · Retrofit Service 대역 |
| `com.squareup.okhttp3:mockwebserver3` | test | HTTP 계층 검증 |
| `androidx.test.ext:junit` | androidTest | `AndroidJUnit4` 러너 |
| `androidx.test:runner`·`androidx.test:rules` | androidTest | 계측 러너·룰 |
| `androidx.compose.ui:ui-test-junit4` | androidTest | Compose UI 테스트 |
| `androidx.compose.ui:ui-test-manifest` | **debug** | `ComponentActivity` 매니페스트 병합 |

`androidx.test.ext:junit`은 unit 소스셋에 넣지 않는다. Robolectric을 쓰지 않으므로
`AndroidJUnit4` 러너가 필요 없고, 순수 JVM 테스트는 러너 지정 없이 기본 JUnit4로 돈다.

MockWebServer는 OkHttp 5 계열에서 아티팩트가 둘로 갈리는데(레거시 `okhttp3:mockwebserver`와
신규 `okhttp3:mockwebserver3`), 실물 확인 결과 **둘 다 5.4.0이 배포돼 있다.** OkHttp 버전과
함께 움직이도록 `version.ref = "okhttp"`로 걸고 신규 패키지(`mockwebserver3`)를 쓴다.

번들 2개로 묶는다.

- `test-unit` — junit4, kotlin-test, kotlin-test-junit, kotlinx-coroutines-test, turbine, mockk, mockwebserver
- `test-android` — junit4, androidx.test:core, androidx.test:runner, androidx.test:rules, androidx.test.ext:junit

`test-compose` 번들은 만들지 않는다. Compose UI 테스트 의존은 BOM platform과 짝지어 걸어야 하고
`ui-test-manifest`는 소스셋이 달라서(`debugImplementation`), 번들로 묶으면 그 구분이 사라진다.
`setConfigTestCompose()`가 개별 좌표로 직접 선언한다.

`test-unit`이 `:core:testing`의 의존까지 책임진다. 그 모듈이 junit4와 coroutines-test를 `api`로
재노출하지 않기 때문이다(아래 [`:core:testing` 모듈](#coretesting-모듈) 참고). 번들에서 둘 중
하나를 빼면 `MainDispatcherRule`을 쓰는 테스트가 컴파일되지 않는다.

## 컨벤션 플러그인

기존 build-logic 구조를 따른다 — 플러그인 클래스는 `BaseConventionPlugin`을 상속한 얇은
껍데기이고, 실제 설정은 `com.teamyg.parfait.buildlogic.TestConfig`의 함수에 둔다.
`AndroidApplicationConventionPlugin` → `setConfigAndroidApplication()` 관계와 동일하다.

### `parfait-test-unit` → `setConfigTestUnit()`

- `testImplementation(bundles.test-unit)`
- `testImplementation(projects.core.testing)`
- Android 확장이 있을 때만 `testOptions` 설정 (아래 분기 참고)

`testOptions`는 Android 확장(`LibraryExtension`·`ApplicationExtension`)에만 존재한다.
`domain`·`core:util:jvm`은 `parfait-kotlin-jvm` 기반이라 확장이 없으므로, 의존성은 항상
붙이고 `testOptions` 설정은 확장 존재 여부로 분기한다.

`testOptions.unitTests.isReturnDefaultValues`는 **기본값(false)을 유지한다.** Robolectric을
쓰지 않는 구성에서 이걸 켜면 `android.jar` 스텁 메서드가 예외 대신 기본값을 조용히 반환해
Android 의존 코드가 잘못 통과한다. 예외로 실패하는 편이 "이건 유닛 테스트 대상이 아니다"라는
정확한 신호다.

### `parfait-test-android` → `setConfigTestAndroid()`

- `androidTestImplementation(bundles.test-android)`
- `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
- `testOptions.animationsDisabled = true` — 계측 테스트 flake의 주원인 차단

`:core:testing`은 붙이지 않는다. 그 모듈의 유일한 자산인 `MainDispatcherRule`은
`Dispatchers.Main`을 테스트 디스패처로 바꾸는 JVM 유닛 테스트용이고, 계측 테스트에는 실제
Main looper가 있어 쓸 일이 없다. `test-android` 번들에 coroutines-test도 없어서 붙여봐야
`TestDispatcher`를 찾지 못한다. 계측용 공유 자산이 생기면 그때 `:core:testing:android`를 만든다.

### `parfait-test-compose` → `setConfigTestCompose()`

- `androidTestImplementation(bundles.test-compose)` (Compose BOM platform 포함)
- `debugImplementation(androidx.compose.ui:ui-test-manifest)`

`ui-test-manifest`는 반드시 `debugImplementation`이다. 이 아티팩트는
`<activity android:name="androidx.activity.ComponentActivity">` 항목을 병합 매니페스트에
넣는 역할인데, `androidTestImplementation`에 걸면 병합이 안 돼 `createComposeRule()`이
`ActivityNotFoundException`으로 죽는다. `TestManifestGradleConfiguration` lint가 이걸 잡는다.

### 적용 대상

| 플러그인 | 적용 모듈 |
|---|---|
| `parfait-test-unit` | (스펙 시점) `domain`, `data`, `core:util:jvm`, `core:util:android` |
| `parfait-test-android` | `core:util:android`, `core:designsystem` (둘 다 스모크) |
| `parfait-test-compose` | `core:designsystem` (스모크) |

> 📌 **`parfait-test-unit`은 이후 라운드에서 feature·core 모듈로 퍼졌다** — `core:ui`·`core:testing`과
> 화면 결선 라운드마다 그 feature `impl`(로그인·설정·그룹 설정·그룹 참여·인트로·그룹 목록)이 차례로 붙였다.
> **결선이 아닌 라운드에서 붙은 사례도 생겼다** — `feature/segmentation/impl`은 크래시 정리·스캐폴드
> 이관 라운드(2026-08-20, PR #309)에 붙어 첫 `src/test` 소스셋을 얻었다.
> **현재 적용 목록은 코드가 SoT**다(`grep -rl "parfait.test.unit" --include=build.gradle.kts .`) — 여기 표는
> 스펙 시점 기준으로 남긴다. 아래 두 플러그인은 여전히 스모크 적용 그대로다.

`core:designsystem`은 `parfait-test-android`와 `parfait-test-compose`를 **함께** 적용한다.
`parfait-test-compose`는 Compose 테스트 의존만 추가하고 계측 러너·`testOptions`는 여전히
`parfait-test-android` 소관이라, 하나만 붙이면 계측 테스트가 돌지 않는다.

계측·Compose 플러그인을 아무 모듈에도 적용하지 않으면 build-logic 컴파일만 통과할 뿐
의존성 좌표 오타·매니페스트 누락 같은 실제 배선 오류가 드러나지 않는다. 스모크 적용 1개씩이
그 검증 공백을 메운다.

## `:core:testing` 모듈

`parfait-kotlin-jvm` 기반 순수 Kotlin 모듈이다. 소비 대상 4개 중 `domain`·`core:util:jvm`은
JVM 모듈이고 `data`·`core:util:android`는 Android 라이브러리인데, Android 모듈이 JVM
라이브러리를 소비하는 건 가능하지만 그 반대는 불가능하다. 따라서 JVM으로 만든다.
`ApplicationProvider` 같은 Android 전용 유틸이 필요해지면 그때 `:core:testing-android`를 분리한다.

`domain` 의존은 넣지 않는다. 공유 Fake를 이번 범위에서 빼기로 하면서(아래 미결 항목 참고)
repository 인터페이스를 참조할 일이 없어졌다. Fake가 실제로 필요해지는 시점에 되살린다.

**구성**

- `MainDispatcherRule` — `TestWatcher` 기반, `val dispatcher: TestDispatcher`를 **공개**한다.

### junit4·coroutines-test는 `implementation`이다

`MainDispatcherRule`은 `TestWatcher`를 상속하고 `TestDispatcher`를 public property로 노출한다.
그래서 이 룰을 쓰는 쪽도 두 라이브러리가 있어야 컴파일된다. ABI만 보면 `api`가 정석이지만,
`setConfigTestUnit()`이 `bundles.test-unit`을 늘 함께 넣어 소비자가 두 라이브러리를 직접
갖추므로 재노출이 아무것도 더하지 않는다. 그 중복을 없애려고 `implementation`으로 내렸다.

대가는 이 모듈이 유닛 테스트 배선과 짝일 때만 성립한다는 점이다. `bundles.test-unit`에서
junit4나 kotlinx-coroutines-test를 빼면 컴파일 에러가 `:core:testing`이 아니라 소비자 쪽에서
난다. 그래서 번들 선언 자리에도 경고 주석을 남겼다.

### `MainDispatcherRule`이 dispatcher를 공개해야 하는 이유

androidx 정본은 dispatcher를 private으로 받는 `TestRule` 형태인데, 그대로 쓰면 스케줄러가
둘로 갈린다. `runTest`는 인자 없이 호출하면 자기 `TestCoroutineScheduler`를 새로 만들기
때문에, `advanceUntilIdle()`이 룰 쪽 Main 디스패처의 큐를 비우지 못한다. 결과는 "상태가
아직 Loading"으로 실패하는 테스트다.

`TestWatcher` 변형으로 dispatcher를 노출하고 호출부에서 `runTest(mainRule.dispatcher)`로
명시 전달해 스케줄러를 하나로 묶는다. 기본값은 `StandardTestDispatcher`다 —
`UnconfinedTestDispatcher`는 즉시 디스패치라 순서 버그를 감춘다.

## 테스트 규약

1. **상태를 제공하는 대역은 Fake.** `every { }`로 데이터만 먹이는 mock은 Fake로 대체한다.
2. **MockK는 상호작용 검증에만.** `coVerify`가 있는 테스트에서만 등장한다.
   검증 없는 mock이 보이면 Fake로 바꾼다.
3. **코루틴은 `runTest`.** `runBlocking`·`runBlockingTest` 금지. `StandardTestDispatcher`가
   기본이므로 상태 단언 전에 `advanceUntilIdle()`(또는 `runCurrent()`)을 호출한다.
4. `viewModelScope`·`Dispatchers.Main`을 타면 `MainDispatcherRule` 필수, 본문은
   `runTest(mainRule.dispatcher)`.
5. **Flow 검증은 Turbine.** 핫 플로우를 `TestScope`에서 그냥 `collect`하면 테스트가
   타임아웃까지 멈춘다. `flow.test { }` 또는 `backgroundScope`를 쓴다.
6. `advanceUntilIdle`·`advanceTimeBy`·`runCurrent`를 쓰는 파일에는
   `@file:OptIn(ExperimentalCoroutinesApi::class)`를 붙인다.
7. **테스트 이름은 영문 `메서드_상태_기대`.** 예: `invoke_nameStartsWithSpace_returnsSpaceAtEdge`.
   백틱 메서드명은 기기 API 30+에서만 지원되는데 이 프로젝트 `minSdk`는 26이라 계측
   테스트가 구형 기기에서 깨진다. unit·계측 규약을 하나로 통일한다.
8. 본문 구조는 Given-When-Then 주석 블록으로 드러낸다.
9. **시각·난수는 주입한다.** `Clock.System.now()`·`Random()`을 직접 호출하는 코드는 테스트
   불가능하다. 파라미터로 받고 기본값을 두는 방식으로 해결한다.
10. 소스셋 디렉토리는 각 모듈의 `main`을 따른다 — `domain`·`data`는 `src/test/java/`,
    `core:util:*`·`core:designsystem`은 `src/test/kotlin`·`src/androidTest/kotlin`.
11. **매퍼는 단독 테스트하지 않는다.** 판단이 든 변환(문자열→enum 매핑, nullable 처리, 기본값
    주입, 단위 변환, 같은 타입 필드의 배선)은 **그 매퍼를 통과시키는 DataSource 테스트의
    케이스**로 잠근다. `XxxVOMapperTest` 같은 파일을 새로 만들지 않는다.
    > 🔁 **2026-08-11 개정.** 이전 규약은 "결정이 있는 곳만 테스트한다"였고 "같은 타입 필드가
    > 둘 이상이면 배선을 한 번 확인한다"는 예외를 열어 뒀다. **그 예외가 매퍼 테스트를 계속
    > 만들어 내는 통로였고, 리뷰에서 반복 지적됐다** — 매퍼는 DataSource가 유일한 호출자라
    > `PolicyRemoteDataSourceImplTest`가 이미 같은 변환을 통과시키고 있었다(성공 경로 케이스가
    > `type` 문자열→`PolicyType` 매핑과 리스트 순서를 단언한다). 두 파일이 같은 것을 두 번
    > 검증하는 대신 **검증 지점을 DataSource 하나로 모은다.**
    >
    > 잃는 것을 명시한다 — 매퍼 테스트에만 있던 케이스(모르는 enum 값의 폴백, 대소문자 민감성)는
    > **DataSource 테스트에 케이스로 옮겨 담는다.** 규약이 "검증을 줄이자"가 아니라 "한 곳에서
    > 하자"이므로, 옮기지 않고 지우면 규약 위반이다.
12. **테스트 클래스명은 대상 심볼을 그대로 딴다.** `PolicyRemoteDataSourceImplTest`처럼
    클래스명 + `Test`. 한 파일에 여러 대상을 몰지 않는다.

## 프로덕션 코드 변경

`DayWindow.current()`가 `Clock.System.now()`를 직접 호출해 시각을 고정할 수 없다.
`timeZone`은 이미 파라미터인데 시각은 아니라서, 새벽 3시 경계 로직을 실제 그 시각에만
검증할 수 있는 상태다.

```kotlin
fun current(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    clock: Clock = Clock.System,
): DayWindow
```

기본값이 있어 기존 호출부는 변경되지 않는다. 이 스펙에서 유일한 프로덕션 코드 변경이다.

## 시범 테스트

| 모듈 | 대상 | 검증 내용 |
|---|---|---|
| `core:util:jvm` | `CharExtension#isKorean` | 한글·영문·숫자·기호·공백 경계 (🔁 **2026-08-15 PR #243에서 확장·테스트 모두 삭제** — 이름 유효성이 서버 문자 집합을 직접 쓰게 되며 사용처가 사라졌다) |
| `core:util:jvm` | `DateFormat` · `DateTextFormat` | 포맷 변환 |
| `domain` | `CheckNameValidUseCase` | 검증 규칙 4종 각각 + 규칙 적용 우선순위 |
| `domain` | `DayWindow` | 03시 경계 직전·직후, `contains`의 반열림 구간 |
| ~~`data`~~ | ~~`VOMapper#toPolicyVO`~~ | 🔁 **2026-08-11 규약 11 개정으로 폐기** — 검증은 아래 DataSource 행으로 흡수한다 |
| `data` | `PolicyRemoteDataSourceImpl` | Service를 MockK로 대역, 성공·실패 경로 매핑 + **매퍼 판단 케이스**(미지의 type·대소문자 불일치가 `UNKNOWN`으로) |
| `data` | `AuthInterceptor` + `ApiCaller` | MockWebServer로 토큰 헤더 부착 · `@NoAuth` 분기 · 에러 변환 |
| `core:util:android` | (unit 없음) | 내용물이 Compose Modifier·Context/Bitmap 확장이라 대상 없음 |
| `core:util:android` | 계측 스모크 1건 | `assembleDebugAndroidTest` 컴파일 검증 |
| `core:designsystem` | Compose 스모크 1건 | `createComposeRule` 동작 + `ui-test-manifest` 배선 검증 |

`data`의 MockWebServer 테스트는 엄밀히는 유닛이 아니라 컴포넌트 성격이지만, 실제 기기가
필요 없으므로 `src/test/`에 둔다.

## CI

기존 `ktlint.yml`에 이미 `./gradlew --info test` 스텝이 있다. 지금은 테스트가 0개라
아무것도 안 하고 통과하지만, 이 스펙을 구현하면 실제로 테스트를 돌리기 시작한다.
새 워크플로를 추가하면 같은 테스트가 두 번 실행되므로 **`ktlint.yml`에서 그 스텝을 제거하고**
테스트 실행을 `test.yml`로 옮긴다. `ktlint.yml`은 포매팅 검사만 담당한다.

### 셋업 스텝은 composite action으로 뽑는다

`test.yml`과 `ktlint.yml`이 셋업 스텝을 글자 그대로 공유한다. 두 워크플로에 같은 30줄을 두는
대신 `.github/actions/` 아래 composite action 2개로 뽑는다. 조직 내 `Team-MINO-Android`가 쓰는
구조와 같다.

| action | 내용 |
|---|---|
| `setup-android-build` | JDK 17(temurin) + `gradle/actions/setup-gradle@v4` |
| `restore-app-secrets` | `local.properties`의 `KAKAO_NATIVE_APP_KEY` + `app/google-services.json` |

`reusable workflow`(`workflow_call`)가 아니라 composite action인 이유는 공통부가 job 전체가
아니라 step 묶음이라는 점이다. 가운데 본체(`./gradlew test` 대 `ktlintCheck`)가 다르고
`test.yml`은 뒤에 리포트 게시·아티팩트 업로드가 더 붙는다.

composite action 안에서는 `secrets` context가 동작하지 않는다. 시크릿은 `inputs`로 받아
`env:`에 담고 `printf`로 파일에 쓴다. 호출부에서 `${{ secrets.* }}`를 넘긴다.

`gradle/actions/setup-gradle@v4`가 Gradle User Home 캐시를 직접 관리하면서 저장 전에
`modules-2.lock`·`gc.properties` 같은 휘발성 파일을 제외한다. 그래서 수동 `actions/cache`
설정과 두 워크플로에 있던 `Cleanup Gradle Cache` 스텝이 둘 다 사라진다. `chmod +x gradlew`도
빠진다. 저장소의 `gradlew`가 이미 `100755`로 커밋돼 있어 처음부터 무의미한 스텝이었다.

### 실행 태스크

루트에서 `./gradlew test`를 돌린다. 테스트가 없는 모듈은 `NO-SOURCE`로 즉시 스킵되고,
Android 라이브러리의 debug·release 중복 실행은 `setConfigTestUnit()`이 release 변형의
host-test 컴포넌트를 끄는 방식으로 막는다(모듈을 일일이 나열하지 않아도 된다).

계측 테스트는 에뮬레이터 없이 컴파일만 검증한다. 배선·매니페스트 회귀를 잡는 그물이다.

```
./gradlew test
./gradlew :core:util:android:assembleDebugAndroidTest :core:designsystem:assembleDebugAndroidTest
```

실패 여부와 무관하게(`if: always()`) 결과를 남긴다.

- `build/test-results/**/*.xml` — JUnit XML, PR에 요약으로 게시
- `build/reports/tests/**` — HTML 리포트, 아티팩트 업로드

## 파일 구성

**신규**

- `build-logic/convention/src/main/kotlin/TestUnitConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/TestAndroidConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/TestComposeConventionPlugin.kt`
- `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt`
- `core/testing/build.gradle.kts` + `MainDispatcherRule`
- 각 대상 모듈의 `src/test/` · 스모크 모듈의 `src/androidTest/`
- `.github/workflows/test.yml`
- `.github/actions/setup-android-build/action.yml`
- `.github/actions/restore-app-secrets/action.yml`

**수정**

- `.github/workflows/ktlint.yml` — `./gradlew --info test` 스텝 제거(중복 실행 방지),
  셋업 스텝을 composite action 호출로 교체
- `gradle/libs.versions.toml` — 버전·라이브러리·번들·플러그인 id 추가
- `settings.gradle.kts` — `:core:testing` include
- 대상 6개 모듈의 `build.gradle.kts` — 플러그인 alias 추가
- `domain/.../DayWindow.kt` — `clock` 파라미터 추가

## as-built (PR #219 develop 머지, 2026-08-09)

`develop` 실물과 대조했다. 이 스펙이 지시한 산출물은 전량 들어왔고 **드리프트 1건**만 나와
위 "적용 대상" 표에 반영했다 — `parfait-test-android`가 `core:util:android`뿐 아니라
`core:designsystem`에도 적용된다. 스펙 본문이 이미 "`parfait-test-compose`는
`parfait-test-android`를 대체하지 않는다"고 적고 있어 논리적으로 필연인데 표에만 빠져 있었다.

일치 확인 항목:

- 컨벤션 플러그인 3종 + `TestConfig.kt`의 `setConfigTestXxx()` 3함수
- `:core:testing` 모듈 — `MainDispatcherRule` 단일 파일, junit4·coroutines-test `implementation`,
  `domain` 의존 없음, `setConfigTestAndroid()`에 미배선
- 번들 2종(`test-unit`·`test-compose` 없음), `DayWindow.current(timeZone, clock)` 파라미터
- 시범 테스트 대상 전량(순수 로직 · 매퍼 · DataSource · HTTP 계층) + 계측 스모크 2건
  (`YGThemeSmokeTest`·`ContextExtensionTest`)
  > ⚠️ **2026-08-15 — 규약이 한 번 깨졌다(PR #248)**: `MyParfaitGroupVOMapperTest`가 신설돼 develop에
  > `*VOMapperTest`가 다시 생겼다(업로드 시각 파싱 3케이스). 대응 `ParfaitGroupRemoteDataSourceImplTest`가
  > 이미 있으므로 케이스를 그쪽으로 옮기는 것이 규약이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
  >
  > 🔁 **2026-08-11 — 매퍼 항목은 규약 개정으로 폐기됐다.** `PolicyVOMapperTest`는 develop에
  > 있으나 **삭제 대상**이고, 그 케이스는 `PolicyRemoteDataSourceImplTest`로 옮긴다. 삭제는
  > 다음 API 서비스 레이어 구현 PR(member·parfait-image)에 함께 묶기로 했다 — 규약 변경과 정리가
  > 한 PR에 보여야 리뷰어가 맥락을 안다. 미머지 브랜치 `feature/sync-backend-api-260810`의
  > `ImageVOMapperTest`도 같은 대상이다.
- CI — `test.yml`(composite action 2개 + 루트 `./gradlew test` + 계측 컴파일 + 리포트·아티팩트),
  `ktlint.yml`(job `lint`, 기존 `--info test` 스텝 제거됨)

규약 검증은 `runBlocking`·`isReturnDefaultValues`·백틱 메서드명 grep 3건 모두 0건이다.
유닛 테스트는 `./gradlew test` 통과하고 계측 2건은 설계대로 컴파일까지만 검증된다.

## 주의 / 열린 질문

- **`mockwebserver3` API 표면.** 좌표는 확정했지만(`okhttp3:mockwebserver3` 5.4.0) 5.x에서
  `MockResponse` 생성 방식과 `takeRequest()` 반환 타입의 프로퍼티명이 4.x와 다르다.
  구현 시 컴파일 에러가 정확한 이름을 알려주므로 그에 맞춘다.
- **ktlint와 테스트 함수명.** 영문 언더스코어 규약을 택했으므로 백틱 면제 여부는 쟁점이
  아니지만, `function-naming` 규칙이 언더스코어를 허용하는지는 첫 빌드에서 확인한다.
  걸리면 해당 규칙을 테스트 소스셋에 한해 완화한다.
- **스모크 계측 테스트는 실행되지 않는다.** 컴파일만 검증하므로 런타임 오류는 이후 실제로
  기기·에뮬레이터를 붙이는 시점까지 드러나지 않는다. CI에 기기를 붙일 때 재검증이 필요하다.
- **`core:util:android`에 unit 테스트가 0개다.** 플러그인만 적용된 상태로 시작하며,
  Android 비의존 로직이 이 모듈에 추가되는 시점에 채워진다.
- **Repository Fake를 어디에 둘지 미결.** 이 스펙은 공용 Fake를 `:core:testing`에 두기로 했는데,
  실제로 Fake 2종을 만들어보니(2026-08-06, 이후 되돌림) 대가가 드러났다 — Repository가 늘 때마다
  한 모듈에 쌓이고, `:core:testing`은 `setConfigTestUnit()`을 통해 **모든 대상 모듈의 테스트
  classpath**에 걸려 있어 한 Fake를 고치면 무관한 모듈의 테스트까지 재컴파일된다. `domain`
  repository의 Fake를 `core:testing`이 소유하는 것도 어긋난다. 선택지 셋:
  (1) 모듈별 `src/testFixtures` — 소유권 명확·재컴파일 범위 최소, 대신 소스셋이 모듈마다 늘고
  AGP `testFixtures` 활성화 필요 / (2) `:core:testing`은 모듈 비종속 유틸(`MainDispatcherRule` 등)만
  두고 도메인 Fake는 `testFixtures`로 / (3) 각 모듈 `src/test`에 두고 공유하지 않음 — 소비자가
  하나뿐인 Fake가 많다면 중복이 오히려 싸다. **첫 Fake가 실제로 필요해지는 시점에 정한다.**
  현재 상태(`:core:testing`에 `MainDispatcherRule`만)는 어느 쪽도 강제하지 않는다.
- **`MainDispatcherRule`은 현재 사용처가 0이다.** 이 룰은 `viewModelScope`·`Dispatchers.Main`을
  타는 테스트용인데 이번 범위(`domain`·`data`·`core:util:*`)에는 ViewModel이 없다. 배선까지는
  확인했다(2026-08-09, `api` 제거 검증차 `domain`에 임시 테스트를 넣어 `@get:Rule` +
  `runTest(rule.dispatcher)`가 컴파일·통과함을 보고 지웠다). 하지만 `Dispatchers.setMain`
  적용·복원과 스케줄러 공유가 실제로 무엇을 막아주는지는 여전히 증명되지 않았다.
  **첫 ViewModel 테스트를 쓰는 사람이 여기서 막힐 수 있다** — 특히 `runTest`를 인자 없이 부르면
  스케줄러가 갈려 `advanceUntilIdle()`이 Main 큐를 비우지 못한다. 그때 룰 자체를 검증하는
  테스트를 함께 추가할 것.
- **계측 테스트에서 코루틴을 다루려면 `test-android` 번들에 `kotlinx-coroutines-test`를 넣어야
  한다.** 현재 그 번들엔 없고 `:core:testing`도 계측 소스셋에 붙지 않으므로, `androidTest`에서
  `runTest`를 부르는 순간 컴파일이 깨진다. 지금 미리 넣지 않은 이유는 계측 테스트 2건
  (`YGThemeSmokeTest`·`ContextExtensionTest`)이 코루틴을 쓰지 않아서다. 필요해지는 시점에 추가한다.
- **ADR 동반 여부 판단 필요.** 테스트 스택 선택(JUnit4 · Fake 우선 · MockK는 상호작용 검증
  한정)과 `:core:testing` 모듈 도입은 장기간 유지되는 구조 결정에 해당한다. parfait 규율상
  새 아키텍처 결정에는 ADR을 동반하므로, 구현 PR에서 ADR 신설 여부를 결정한다.
