---
id: screenshot-comparison-ci
title: PR 스크린샷 비교 리포트 CI 구현 계획
status: done
type: work-order
created: 2026-11-04
updated: 2026-11-08
platforms: android
owner: android
related_adr: ADR-0037
related_spec: screenshot-comparison-ci
related_code:
  - build-logic/convention/build.gradle.kts
  - build-logic/convention/src/main/kotlin/ModuleFeatureImplConventionPlugin.kt
  - core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/utils/preview/YGPreview.kt
  - .github/PULL_REQUEST_TEMPLATE.md
archived_reason: 구현 완료(PR #684, 프리뷰 이름 붙이기와 캐시 시딩 스텝은 #686). 미체크 4개 — Task 1 Step 3a는 스펙의 `UI_SNAPSHOT_PREVIEW_GROUP`(`phone` 변형만 찍기)이 대체해 6변형을 유지했다. Task 3 Step 4 시간 게이트는 조건부이고 발화하지 않았다. Task 9 Step 2 검증 PR은 따로 열지 않고 실제 PR의 신규·변경 섹션 코멘트로 같은 경로를 확인했다. Task 9 Step 3 prune `workflow_dispatch` dry-run 기록은 없고 schedule 실행만 있다. Task 3 Step 2는 기준(프리뷰 함수만 고침)과 달리 프로덕션 코드 `GroupListContent`(`clock` 인자)·`VerticalGridLayout`(`Random(seed = 0)`)을 고쳤고, 제외는 `YGToppingCutoutImage` 프리뷰 하나다(WebView는 제외하지 않았다). File Structure 표 밖에 `stage.py`·`UiSnapshotPreviewGroup.kt`·`NamedPreviewParameterProvider.kt`·`PreviewParameterNames.kt`·`gradle-cache-seed.yml` 시딩 스텝이 더 생겼다.
---

# PR 스크린샷 비교 리포트 CI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** UI 관련 경로를 바꾼 PR에 base 대비 바뀐·신규·삭제 `@Preview` 렌더를 이미지 표 코멘트로 단다.

**Architecture:** Roborazzi가 `@Preview`를 스캔해 Robolectric 테스트를 생성한다. PR 워크플로 한 잡이 `base.sha`에서 `recordRoborazziDebug`, PR merge ref에서
`compareRoborazziDebug`를 돌리고 결과와 메타데이터를 아티팩트로 올린다. 쓰기 권한은 `workflow_run`으로 도는 코멘트 워크플로에만 있고, 이미지는
orphan 브랜치 `ui-snapshots/pr-<N>`에 올려 커밋 SHA URL로 임베드한다. 일 1회 워크플로가 닫힌 지 30일 지난 PR의 이미지 브랜치를 지운다.

**Tech Stack:** Roborazzi 1.77.0, Robolectric 4.17, ComposablePreviewScanner 0.9.3, GitHub Actions, Python 3 표준 라이브러리(`unittest`), `gh` CLI.

**Spec:** [`docs/superpowers/specs/2026-11-04-screenshot-comparison-ci-design.md`](../../specs/archive/2026-11-04-screenshot-comparison-ci-design.md)

## Global Constraints

- 참고한 외부 프로젝트의 이름·URL·PR 번호·파일명을 코드·주석·문서·커밋·PR 어디에도 쓰지 않는다.
- 이름은 이 레포 기준으로 새로 짓는다. 그대로 쓰는 것은 라이브러리·GitHub이 정한 이름뿐이다(`recordRoborazziDebug`, `compareRoborazziDebug`, `build/outputs/roborazzi`, `refs/pull/N/merge`).
- 이 계획이 정한 이름(아래 Interfaces)을 바꾸지 않는다.
- 비교 워크플로에는 시크릿을 넘기지 않는다. 권한은 `contents: read`만.
- 비교 잡은 diff가 있어도, 일부 모듈 렌더가 실패해도 성공으로 끝난다. 필수 체크로 등록하지 않는다.
- 트리거 경로: `feature/**` `core/designsystem/**` `core/ui/**` `core/testing/android/**` `gradle/libs.versions.toml` `build-logic/**` `gradle/wrapper/**` `build.gradle.kts` `settings.gradle.kts` `gradle.properties` `.github/workflows/ui-diff.yml` `.github/scripts/ui_diff/**`.
- 바뀐 화면이 0건이면 코멘트를 새로 만들지 않는다. 이미 있으면 "바뀐 화면 없음"으로 갱신한다. 비교 못 함 중 `merge-conflict`·`failed`는 코멘트를 남긴다. `no-base-setup`은 기존 코멘트 갱신에만 쓴다.
- 이미지 브랜치 보존 기간: PR이 닫힌 뒤 **30일**.
- Kotlin 주석·KDoc은 [`docs/code-conventions.md`](../../../code-conventions.md)를 따른다 — 코드가 말하는 것은 쓰지 않는다, `@return`·`@param`은 이름이 못 말할 때만, 다른 컴포넌트의 현재 상태를 단정하지 않는다.
- 커밋 메시지는 `type: 한국어 설명`, Co-Authored-By 줄을 넣지 않는다.
- 원격 push·PR 생성은 사용자가 명시적으로 요청할 때만 한다.

## Review Focus

1. **UI와 무관한 PR에서 diff가 뜬다** — 같은 코드를 두 번 렌더하면 diff 0건이어야 한다. Task 3이 전 모듈 record → compare로 확인한다.
2. **평소 `./gradlew test`(필수 체크 `test.yml`)가 생성된 프리뷰 테스트까지 돌아 느려진다** — Roborazzi 태스크가 아닐 때는 생성 클래스를 제외한다. Task 1 Step 4가 확인한다.
3. **프리뷰가 0개인 feature 모듈**(`feature:segmentation:impl`)에서 record가 실패해 `failed` 오보가 난다 — Task 2 Step 3이 확인한다.
4. **늦게 도착한 오래된 실행이 최신 코멘트를 덮어쓴다** — head SHA stale 검사. Task 4 `test_stale_head_is_rejected`, Task 6 Step 2.
5. **아티팩트 경로·PR 번호를 통한 주입**(경로 이스케이프, 개행, pathspec magic, 숫자 아닌 PR 번호) — Task 4 `test_unsafe_paths_are_dropped` · `test_meta_validation`.

---

## File Structure

| 경로 | 책임 |
|---|---|
| `gradle/libs.versions.toml` | roborazzi·robolectric·composable-preview-scanner 버전과 라이브러리·플러그인, `parfait-ui-snapshot` 플러그인 별칭 |
| `build.gradle.kts` (루트) | roborazzi 플러그인 `apply false` |
| `build-logic/convention/build.gradle.kts` | roborazzi gradle plugin `compileOnly`, `ui.snapshot` 등록 |
| `build-logic/convention/src/main/kotlin/UiSnapshotConventionPlugin.kt` | 플러그인 진입점 |
| `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/UiSnapshotConfig.kt` | `setConfigUiSnapshot()` — Roborazzi 생성 설정, 테스트 옵션, 의존성, 평소 `test`에서 생성 테스트 제외 |
| `core/testing/android/` (신규 모듈 `:core:testing:android`) | `UiSnapshotPreviewTester` — 생성 테스트에 끼워 넣는 tester |
| `core/designsystem/.../utils/preview/UiSnapshotExcluded.kt` | 비교에서 빼는 프리뷰 표시 어노테이션 |
| `.github/scripts/ui_diff/report.py` | 결과 분류·경로 검증·메타 검증·코멘트 렌더 |
| `.github/scripts/ui_diff/prune.py` | 지울 이미지 브랜치 선정 |
| `.github/scripts/ui_diff/test_report.py`, `test_prune.py` | 위 두 스크립트 단위 테스트 |
| `.github/workflows/ui-diff.yml` | 비교(`pull_request`) |
| `.github/workflows/ui-diff-comment.yml` | 코멘트(`workflow_run`) |
| `.github/workflows/ui-snapshots-prune.yml` | 이미지 브랜치 정리(`schedule`) |

---

### Task 1: 스크린샷 convention plugin과 tester 모듈 (`core:designsystem`에 먼저 적용)

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`, `build-logic/convention/build.gradle.kts`, `core/designsystem/build.gradle.kts`
- Create: `build-logic/convention/src/main/kotlin/UiSnapshotConventionPlugin.kt`, `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/UiSnapshotConfig.kt`
- Create: `core/testing/android/build.gradle.kts`, `core/testing/android/src/main/kotlin/com/teamyg/parfait/core/testing/android/UiSnapshotPreviewTester.kt`
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/utils/preview/UiSnapshotExcluded.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/utils/preview/YGPreview.kt`

**Interfaces:**
- Produces:
  - 플러그인 id `com.teamyg.parfait.plugin.ui.snapshot`, 카탈로그 별칭 `libs.plugins.parfait.ui.snapshot`
  - `internal fun Project.setConfigUiSnapshot()` (`UiSnapshotConfig.kt`)
  - `class UiSnapshotPreviewTester : ComposePreviewTester<AndroidPreviewJUnit4TestParameter>` — FQN `com.teamyg.parfait.core.testing.android.UiSnapshotPreviewTester`
  - `annotation class UiSnapshotExcluded` — FQN `com.teamyg.parfait.core.designsystem.utils.preview.UiSnapshotExcluded`
  - 렌더 결과: 모듈별 `build/outputs/roborazzi/*.png`, 캡처별 결과 JSON(경로·스키마는 Step 6에서 기록)

- [x] **Step 1: 카탈로그·플러그인 등록**

  `[versions]` `roborazzi = "1.77.0"` `robolectric = "4.17"` `composablePreviewScanner = "0.9.3"`(0.10.0은 `ComposablePreview.invoke(Composer, int)` 시그니처가 바뀌어 Roborazzi 1.77.0의 scanner-support가 `NoSuchMethodError`로 죽는다 — Roborazzi 1.77.0은 0.9.1 기준으로 빌드됐다).
  라이브러리: `roborazzi-gradle-plugin`(`io.github.takahirom.roborazzi:roborazzi-gradle-plugin`), `roborazzi-compose-preview-scanner-support`,
  `composable-preview-scanner-android`(`io.github.sergio-sastre.ComposablePreviewScanner:android`), `robolectric`.
  플러그인: `roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }`, `parfait-ui-snapshot`.
  루트 `build.gradle.kts`에 `alias(libs.plugins.roborazzi) apply false`. build-logic에 `compileOnly(libs.roborazzi.gradle.plugin)`와
  `pluginRegister(pluginName = "ui.snapshot", className = "UiSnapshot")`.

- [x] **Step 2: `setConfigUiSnapshot()` 구현**

  - roborazzi 플러그인 적용
  - `generateComposePreviewRobolectricTests`:
    - `enable = true`
    - `packages = listOf(<android namespace>)` — `LibraryExtension.namespace`에서 읽는다(이 레포의 namespace가 패키지 루트)
    - `includePrivatePreviews = true`
    - `testerQualifiedClassName = UiSnapshotPreviewTester FQN`
    - `useScanOptionParametersInTester = true`
    - `annotationFilter = AnnotationFilter.Exclude(UiSnapshotExcluded FQN)`
    - `robolectricConfig = mapOf("sdk" to "[36]")` — compileSdk 37은 Robolectric 4.17 미지원, targetSdk 36에 맞춘다
  - `LibraryExtension.testOptions.unitTests`: `isIncludeAndroidResources = true`, 모든 테스트에 `robolectric.pixelCopyRenderMode=hardware`
  - `testImplementation`: `:core:testing:android`, scanner-support, composable-preview-scanner-android, robolectric, junit4 — Roborazzi 플러그인이 모듈 직접 선언을 검사하므로 전이 의존으로 두지 않는다
  - **평소 테스트 제외**: 시작 태스크 이름에 `Roborazzi`가 없으면 모든 `Test` 태스크에서 `com.github.takahirom.roborazzi.RoborazziPreviewParameterizedTests*`를
    `excludeTestsMatching`하고 `isFailOnNoMatchingTests = false`

- [x] **Step 3: `:core:testing:android` 모듈과 tester, 제외 어노테이션**

  모듈은 `parfait.android.library`, namespace `com.teamyg.parfait.core.testing.android`, 의존성 scanner-support·robolectric·`lottie-compose`(`compileOnly` 아님 — `LottieTask`를 런타임에 바꾼다).
  `TestConfig.kt`의 `setConfigTestAndroid` 주석이 "계측용 공유 자산이 생기면 `:core:testing:android`"라고 예고한 이름이다 — 이 모듈은 계측이 아니라 Robolectric 호스트 테스트용이므로 그 주석을 사실에 맞게 고친다.
  `UiSnapshotPreviewTester`는 `AndroidComposePreviewTester`에 위임하고 `test()` 직전에 `LottieTask.EXECUTOR`를 호출 스레드 즉시 실행 executor로 바꾼다
  (로티 파싱이 Compose idle 감지 밖이라 빈 화면이 찍힌다 — KDoc 한 줄).
  `UiSnapshotExcluded`: `@Retention(BINARY)` `@Target(FUNCTION, ANNOTATION_CLASS)`, KDoc에 "두 번 렌더해 픽셀이 같지 않은 프리뷰(네트워크·WebView·시간 의존)에만 붙인다".
  `core/designsystem/build.gradle.kts`에 `alias(libs.plugins.parfait.ui.snapshot)`.

- [ ] **Step 3a: `@YGPreview`를 `phone` 하나로 줄인다**

  `name = "phone"`, `device = "spec:width=411dp,height=891dp,navigation=buttons"`, `uiMode = AndroidUiModes.UI_MODE_NIGHT_NO` 하나만 활성으로 두고
  `phone-night` `foldable` `foldable-night` `foldable-fold` `foldable-fold-night`는 **지우지 않고 주석 처리**한다(사용자 결정 — 다크 모드·폴더블
  대응 때 되살린다). 주석 블록 위에 한 줄: 스크린샷 비교 렌더 시간 때문에 끈 것이고 다크 모드가 생기면 night를 되살린다는 취지 + 스펙 포인터.
  이유: 사용처 106곳 × 6변형을 PR마다 두 번 렌더하는 시간, `YGDarkColorScheme`이 `YGLightColorScheme`과 같아 night 변형은 같은 그림이다.
  IDE 프리뷰에서도 폴더블 변형이 사라진다.

- [x] **Step 4: 평소 테스트에 생성 테스트가 안 섞이는지 확인**

  Run: `./gradlew :core:designsystem:testDebugUnitTest && ls core/designsystem/build/test-results/testDebugUnitTest/ | grep -c RoborazziPreview`
  Expected: 빌드 성공, 출력 `0`

- [x] **Step 5: 기록 확인**

  Run: `./gradlew :core:designsystem:recordRoborazziDebug && ls core/designsystem/build/outputs/roborazzi/*.png | wc -l`
  Expected: 성공. PNG 수 ≥ 8. `@YGPreview` 프리뷰는 `phone` 하나씩만 찍힌다

- [x] **Step 6: 비교 결과 JSON 위치·스키마 기록**

  Run: `./gradlew :core:designsystem:compareRoborazziDebug && find core/designsystem/build -path '*roborazzi*' -name '*.json' | head`
  캡처 하나당 결과 파일 경로와 필드(기대: `type` ∈ `changed`/`added`/`unchanged`, 골든·실제·비교 이미지 경로)를 이 계획 Task 4 상단 "결과 JSON 형식" 줄에 적는다.
  base 기록 없이 compare했을 때 `added`로 나오는지도 같이 확인해 적는다.

- [x] **Step 7: ktlint, 커밋**

  Run: `./gradlew :core:designsystem:ktlintCheck :core:testing:android:ktlintCheck`
  ```bash
  git add gradle/libs.versions.toml build.gradle.kts settings.gradle.kts build-logic core/testing/android core/designsystem docs/superpowers/plans/2026-11-04-screenshot-comparison-ci.md
  git commit -m "build: 프리뷰 스크린샷 기록용 convention plugin과 tester 모듈 추가"
  ```

### Task 2: 프리뷰가 있는 모든 모듈에 적용

**Files:**
- Modify: `build-logic/convention/src/main/kotlin/ModuleFeatureImplConventionPlugin.kt`, `core/ui/build.gradle.kts`

**Interfaces:**
- Consumes: `libs.plugins.parfait.ui.snapshot` (Task 1)

- [x] **Step 1: `ModuleFeatureImplConventionPlugin`이 `parfait.ui.snapshot`을 함께 적용** — 새 feature 모듈이 자동으로 리포트 대상이 되게 한다.
- [x] **Step 2: `core/ui/build.gradle.kts`에 `alias(libs.plugins.parfait.ui.snapshot)`**
- [x] **Step 3: 프리뷰 0개 모듈 확인**

  Run: `./gradlew :feature:segmentation:impl:recordRoborazziDebug`
  Expected: 성공. 실패하면 `setConfigUiSnapshot()`에서 빈 파라미터 목록을 성공으로 처리하도록 고친다(모듈별 예외 목록을 두지 않는다).

- [x] **Step 4: 전 모듈 기록**

  Run: `./gradlew recordRoborazziDebug --continue 2>&1 | tee /tmp/ui-record.log; grep -E "testDebugUnitTest FAILED" /tmp/ui-record.log`
  Expected: 실패 줄이 나오면 그 모듈·프리뷰를 Task 3 대상 목록에 적는다(여기서 고치지 않는다). 전체 소요 시간을 적는다.

- [x] **Step 5: 커밋** — `build: 프리뷰가 있는 feature·core:ui 모듈에 스크린샷 기록 적용`

### Task 3: 렌더 결정성

**Files:**
- Modify(후보): `feature/intro/impl/.../termagree/TermAgreeScreen.kt`, `feature/groups/canvas/impl/.../screen/CanvasToppingPlaceScreen.kt`, `.../screen/CanvasImageSaveScreen.kt`, `feature/groups/list/impl/.../route/GroupListScreen.kt`, `feature/common/terms/impl/.../screen/WebViewScreen.kt`, Task 2 Step 4에서 실패한 파일

**Interfaces:**
- Consumes: `UiSnapshotExcluded`, `UiSnapshotPreviewTester` (Task 1)

- [x] **Step 1: 같은 커밋 두 번 렌더 비교**

  Run: `./gradlew recordRoborazziDebug --continue && ./gradlew compareRoborazziDebug --continue && find . -path '*/build/outputs/roborazzi/*_compare.png' -not -path './build-logic/*'`
  Expected(수정 전): 흔들리는 프리뷰의 `_compare.png`가 나온다. 목록을 적는다.

- [x] **Step 2: 고치거나 제외한다** — 기준: Coil 원격 이미지는 `YGCustomTheme`이 이미 `LocalAsyncImagePreviewHandler`를 공급하므로, 그 테마 밖에서 그리는 프리뷰만 테마로 감싼다. Lottie는 Task 1 tester로 이미 동기다. WebView·시간 의존·네트워크 필수는 `@UiSnapshotExcluded`. 프로덕션 코드 경로는 바꾸지 않는다(프리뷰 함수만).
- [x] **Step 3: Step 1 재실행** — Expected: `_compare.png` 0건, `testDebugUnitTest FAILED` 0건.
- [ ] **Step 4: 시간 게이트** — Step 1의 record 소요 시간이 **15분**(로컬)을 넘으면 여기서 멈추고 사용자에게 보고한다. 프리뷰를 더 줄이는 것은 사용자 결정이다.
- [x] **Step 5: 커밋** — `fix: 스크린샷 비교에서 흔들리는 프리뷰 고정·제외`

### Task 4: 리포트 스크립트

결과 JSON 형식: 모듈별 `build/test-results/roborazzi/debug/results/<timestamp>_<캡처 이름>.json` 캡처당 1개, 같은 디렉터리 위 `results-summary.json`에 `{"summary": {"total", "recorded", "added", "changed", "unchanged"}, "results": [...]}`(각 원소는 캡처별 JSON과 같은 객체). 캡처별 필드: `type` ∈ `unchanged`/`changed`/`added`(record 때는 `recorded`), `golden_file_path`(항상), `actual_file_path`·`compare_file_path`(`changed`·`added`일 때만), `diff_percentage`(`changed`일 때만, 0.0~1.0), `timestamp`, `context_data`. 경로는 전부 절대 경로이고 `build/outputs/roborazzi/<캡처 이름>{,_actual,_compare}.png`를 가리킨다. 기준 PNG 없이 compare하면 전부 `added`이고 `golden_file_path`는 존재하지 않는 파일을 가리킨다. compare는 `changed`가 있어도 태스크가 성공(exit 0)한다.

**Files:**
- Create: `.github/scripts/ui_diff/report.py`, `.github/scripts/ui_diff/test_report.py`

**Interfaces:**
- Produces (CLI, 워크플로가 호출):
  - `python3 .github/scripts/ui_diff/report.py collect --results-root DIR --base-list FILE --meta FILE --out-entries FILE --out-files FILE --github-output FILE`
    — `--out-files`: 이미지 브랜치에 커밋할 상대 경로, NUL 구분. `--github-output`에 `total=<n>` 기록
  - `python3 .github/scripts/ui_diff/report.py render --entries FILE --meta FILE --repo OWNER/NAME --image-sha SHA --out FILE`
  - `python3 .github/scripts/ui_diff/report.py check-head --meta FILE --current-head SHA` — 다르면 exit 3
- Produces (모듈 내부): `validate_meta(raw: dict) -> Meta`, `is_safe_path(rel: PurePosixPath) -> bool`,
  `classify(results: list[CaptureResult], base_names: set[str], failed_modules: set[str]) -> Report`,
  `render_comment(report: Report, meta: Meta, repo: str, image_sha: str, max_rows: int = 40) -> str`
- 메타 파일 `meta.json`: `{"pr": int, "head_sha": str(40 hex), "base_sha": str(40 hex), "status": "ok"|"no-base-setup"|"merge-conflict"|"failed", "failed_modules": [":a:b", ...]}`

- [x] **Step 1: 실패하는 테스트 작성** (`unittest`, 픽스처는 `tempfile`로 만든다)

  - `test_meta_validation` — `pr="12; rm"` · `head_sha` 39자 · 모르는 `status` · `failed_modules`에 `":a\n:b"` → 각각 `ValueError`
  - `test_unsafe_paths_are_dropped` — `../x/build/outputs/roborazzi/a.png`, `build/outputs/roborazzi` 밖, 이름에 `\n`, 심볼릭 링크, `:(glob)*` 로 시작하는 이름 → `is_safe_path` False
  - `test_classify_changed_added_removed` — 결과 `{A: changed, B: added, C: unchanged}`, base `{A, C, D}` → changed `[A]`, added `[B]`, removed `[D]`
  - `test_failed_module_rows_excluded` — `:feature:x` 실패, 그 모듈의 added·removed 항목 → 표에서 빠지고 `report.failed_modules == [":feature:x"]`
  - `test_render_uses_image_sha_urls` — 이미지 URL이 `https://github.com/o/r/blob/<sha>/<percent-encoded path>?raw=true`, 한글 경로는 퍼센트 인코딩, 브랜치 이름이 URL에 없음
  - `test_render_header_has_marker_and_shas` — 첫 줄 `<!-- parfait-ui-diff-report -->`, 본문에 `base` `head` 7자 SHA
  - `test_render_row_cap` — 변경 45건, `max_rows=40` → 행 40개 + "45개 중 40개" 안내와 `tree/ui-snapshots/pr-<N>` 링크
  - `test_render_zero` — 0건 → 본문에 `바뀐 화면 없음`, `collect`의 `total=0`
  - `test_render_status_messages` — `no-base-setup` → `base에 스크린샷 구성이 없어 비교하지 못했습니다`, `merge-conflict` → `충돌로 merge ref가 없어 비교하지 못했습니다`, `failed` → `렌더가 실패해 비교하지 못했습니다`
  - `test_stale_head_is_rejected` — `check-head`가 다른 SHA에 exit 3, 같은 SHA에 exit 0
  - `test_rows_sorted` — 입력 순서를 섞어도 출력 행 순서 동일

- [x] **Step 2: 실패 확인** — Run: `python3 -m unittest discover -s .github/scripts/ui_diff -p 'test_*.py'` → FAIL(`ModuleNotFoundError`)
- [x] **Step 3: 구현** — 표준 라이브러리만. 표 열: `모듈 | 프리뷰 | Before / Diff / After`(변경), `모듈 | 프리뷰 | After`(신규), 삭제는 이름 목록. 프리뷰 이름은 namespace 접두사 `com.teamyg.parfait.`를 떼고 28자마다 `<br>`. 실패 모듈은 표 위 한 줄.
- [x] **Step 4: 통과 확인** — 같은 명령 → OK
- [x] **Step 5: 커밋** — `ci: 스크린샷 비교 리포트 스크립트 추가`

### Task 5: 비교 워크플로 `ui-diff.yml`

**Files:**
- Create: `.github/workflows/ui-diff.yml`

**Interfaces:**
- Consumes: Task 1 Gradle 태스크, `.github/actions/setup-android-build`
- Produces: 워크플로 `name: UI diff`, 아티팩트 `ui-diff-meta`(`meta.json`, `base-list.txt`), `ui-diff-results`(모듈별 `build/outputs/roborazzi/**`, 결과 JSON) — 보존 3일

- [x] **Step 1: 워크플로 작성**

  - `on.pull_request.paths` = Global Constraints 목록. `permissions: contents: read`.
  - `concurrency: { group: ui-diff-${{ github.event.pull_request.number }}, cancel-in-progress: true }`
  - 순서:
    1. merge ref 존재 확인(`git ls-remote origin refs/pull/<N>/merge`). 없으면 `status=merge-conflict`로 메타만 올리고 끝낸다.
    2. `base.sha` checkout(`persist-credentials: false`).
    3. `local.properties`에 `KAKAO_NATIVE_APP_KEY=dummy`. 실제 시크릿은 쓰지 않는다.
    4. `./gradlew help --task recordRoborazziDebug`가 태스크를 못 찾아 실패하면(`not found in`) `status=no-base-setup`, 다른 이유로 실패하면 `status=failed`.
    5. `./gradlew recordRoborazziDebug --continue | tee base.log`.
    6. 기록 PNG를 `$RUNNER_TEMP/base`로 모듈 상대 경로를 유지해 옮긴다. 이름 목록을 `base-list.txt`에 쓴다.
    7. merge ref checkout.
    8. 6의 PNG를 각 모듈 `build/outputs/roborazzi`로 복원한다.
    9. `./gradlew compareRoborazziDebug --continue | tee head.log`.
    10. 두 로그의 `> Task :<path>:testDebugUnitTest FAILED`에서 모듈 경로를 뽑아 `failed_modules`에 넣는다. 모든 모듈이 실패하면 `status=failed`.
  - 메타 업로드는 `if: always()`로 둔다.
  - 잡의 최종 결론은 diff·모듈 실패와 무관하게 success다(Gradle 스텝 `continue-on-error: true`).
  - `timeout-minutes`는 Task 3 Step 1 실측의 2배 + 10분.
- [x] **Step 2: 로컬 검증** — `local.properties`를 치우고 Step 1의 더미 한 줄만 둔 상태에서 `./gradlew :core:designsystem:recordRoborazziDebug` 성공(구성 단계가 서명·`google-services.json` 없이 통과하는지). Run: `actionlint .github/workflows/ui-diff.yml`(없으면 `brew install actionlint`) → 오류 0.
- [x] **Step 3: 커밋** — `ci: PR 스크린샷 비교 워크플로 추가`

### Task 6: 코멘트 워크플로 `ui-diff-comment.yml`

**Files:**
- Create: `.github/workflows/ui-diff-comment.yml`

**Interfaces:**
- Consumes: `UI diff` 아티팩트(Task 5), `report.py collect|render|check-head`(Task 4)

- [x] **Step 1: 워크플로 작성**

  - `on.workflow_run: { workflows: [UI diff], types: [completed] }`
  - `if: github.event.workflow_run.event == 'pull_request' && github.event.workflow_run.conclusion == 'success'`
  - `permissions: { actions: read, contents: write, pull-requests: write }`
  - `concurrency: { group: ui-diff-comment-${{ github.event.workflow_run.head_repository.full_name }}-${{ github.event.workflow_run.head_branch }}, cancel-in-progress: true }`
  - default 브랜치 checkout(스크립트용). PR 코드는 checkout하지 않는다.
  - 아티팩트 다운로드: `gh run download <run_id> -n ui-diff-meta`. status가 `ok`일 때만 `-n ui-diff-results`.
  - 순서:
    1. `collect`
    2. `check-head`(현재 head는 `gh api repos/<repo>/pulls/<N> --jq .head.sha`)
    3. `total > 0`이면 orphan `ui-snapshots/pr-<N>`에 `--out-files` 목록만 `git --literal-pathspecs add --pathspec-from-file=… --pathspec-file-nul`로 커밋하고 force push. 커밋 SHA를 기록한다.
    4. `render`
    5. `check-head` 재실행
    6. 마커 `<!-- parfait-ui-diff-report -->`로 기존 봇 코멘트를 찾는다(`gh api .../issues/<N>/comments --paginate`).
    7. `total == 0 && status == ok && 기존 없음`이면 끝낸다. 그 밖에는 생성하거나 PATCH한다(`--input` 파일로 본문 전달).
- [x] **Step 2: stale 경로 검토** — `check-head` exit 3이면 스텝이 아무것도 쓰지 않고 성공으로 끝나는지(`exit 0` 처리) 워크플로 코드에서 확인. `actionlint` 오류 0.
- [x] **Step 3: 커밋** — `ci: 스크린샷 비교 결과 PR 코멘트 워크플로 추가`

### Task 7: 이미지 브랜치 정리

**Files:**
- Create: `.github/scripts/ui_diff/prune.py`, `.github/scripts/ui_diff/test_prune.py`, `.github/workflows/ui-snapshots-prune.yml`

**Interfaces:**
- Produces: `select_expired(branches: list[str], prs: dict[int, PrState], now: datetime, keep_days: int = 30) -> list[str]`, CLI `prune.py --repo OWNER/NAME [--dry-run]`

- [x] **Step 1: 실패하는 테스트** — `test_select_expired`:
  - `ui-snapshots/pr-1` 닫힘 31일 전 → 삭제
  - `pr-2` 닫힘 29일 전 → 유지
  - `pr-3` 열림 → 유지
  - `pr-4` PR 조회 실패 → 유지
  - `ui-snapshots/foo`·`feature/x` → 대상 아님
- [x] **Step 2: 실패 확인** → **Step 3: 구현** (`gh api`로 브랜치·PR 조회, 삭제는 refs API `gh api -X DELETE repos/<repo>/git/refs/heads/<branch>`) → **Step 4: 통과 확인** (`python3 -m unittest discover -s .github/scripts/ui_diff -p 'test_*.py'`)
- [x] **Step 5: 워크플로** — `schedule: cron "0 18 * * *"`(KST 03:00) + `workflow_dispatch`, `permissions: contents: write, pull-requests: read`. `actionlint` 오류 0.
- [x] **Step 6: 커밋** — `ci: 닫힌 지 30일 지난 스크린샷 이미지 브랜치 정리`

### Task 8: PR 템플릿·문서

**Files:**
- Modify: `.github/PULL_REQUEST_TEMPLATE.md`, `docs/superpowers/specs/2026-11-04-screenshot-comparison-ci-design.md`(status), `docs/superpowers/specs/README.md`, `docs/superpowers/plans/README.md`, `docs/log.md`
- Create: `docs/adr/<다음 빈 번호>-pr-screenshot-diff-in-job-base-render.md`

- [x] **Step 1: 템플릿** — 체크 항목 `UI 변경 시 스크린샷 첨부` → `UI 변경 시 봇의 스크린샷 비교 코멘트 확인 (동작·애니메이션·프리뷰 없는 화면은 직접 첨부)`. Screenshots 섹션 안내 주석도 같은 취지로.
- [x] **Step 2: ADR** — 번호는 `ls docs/adr` 기준 다음 빈 번호(#669 머지 여부에 따라 0036 또는 0037). 내용: B 채택, A(develop 골든 아티팩트)·C(골든 커밋) 기각 이유, 30일 보존, 0건 무코멘트. 템플릿은 `docs/adr/template.md`.
- [x] **Step 3: 인덱스·로그** — 스펙 status `in-progress`, 두 README 한 줄, `docs/log.md` 한 줄.
- [x] **Step 4: 링크 확인** — Run: `python3 docs/script/check_links.py docs` → exit 0
- [x] **Step 5: 커밋** — `docs: 스크린샷 비교 CI ADR과 PR 템플릿 안내 갱신`

### Task 9: 종단 검증 (사용자 승인 필요)

`workflow_run`·`schedule`은 기본 브랜치(`develop`)의 워크플로 파일로만 돈다. 그래서 코멘트·정리는 이 브랜치가 develop에 머지된 뒤에야 실제로 확인된다.

- [x] **Step 1:** 이 브랜치 PR을 연다(사용자 요청 시). 이 PR에서 `UI diff`는 base에 구성이 없으므로 `no-base-setup` 메타로 끝나는지 Actions 로그로 확인.
- [ ] **Step 2:** 머지 후 검증용 PR(머지 금지)을 연다. `YGButton` 모서리를 바꿔 diff를 만든다. 확인할 것:
  - 코멘트의 변경 표와 이미지가 보인다.
  - 신규 프리뷰 1개를 추가하면 신규 섹션에 뜬다.
  - 연속 push 2회 후 코멘트의 head SHA가 최신이다.
- [ ] **Step 3:** 검증 PR을 닫고 `ui-snapshots-prune`을 `workflow_dispatch --dry-run`으로 돌린다. 30일이 안 지났으므로 삭제 대상 0건이어야 한다.
