---
id: topping-edit-entry-navigation
title: 누끼 편집(C-104) 진입 흐름 단축 — 네비게이션과 코드 제거 (계획 1/2)
status: done
type: work-order
created: 2026-10-03
updated: 2026-10-03
platforms: android
owner:
related_adr: ADR-0006, ADR-0026
related_spec: topping-edit-entry-flow
related_code:
  - feature/segmentation/impl/.../viewmodel/ToppingEditViewModel.kt#ToppingEditViewModel
  - feature/segmentation/impl/.../route/ToppingEditRoute.kt#ToppingEditRoute
  - feature/segmentation/impl/.../viewmodel/SegmentationViewModel.kt#selectCandidate
  - feature/segmentation/impl/.../route/SegmentationRoute.kt#SegmentationRoute
  - feature/gallery/impl/.../viewmodel/CustomGalleryPickerViewModel.kt#handleOnClickCutoutImage
  - feature/gallery/impl/.../route/CustomGalleryPickerRoute.kt#CustomGalleryPickerRoute
archived_reason: 구현 완료(2026-10-03, 5 Task 수행, 브랜치 `feature/#566-topping-edit-ui`, develop 머지 전). 화면 모양은 계획 2가 바꿨다.
tags: [plan, parfait]
---

# 누끼 편집 진입 흐름 — 네비게이션과 코드 제거 Implementation Plan

> **Archived (2026-10-03)** — 구현 완료(2026-10-03, 5 Task 수행, 브랜치 `feature/#566-topping-edit-ui`, develop 머지 전). 화면 모양은 계획 2가 바꿨다. 본문은 작성 시점의 계획이고 식별자·문구는 그때 이름이다.
> 현재 상태는 [status.md](../../../status.md)를 본다.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 누끼 결과를 고르면 언제나 C-104로 들어가고 C-104 「다음」이 C-106으로 가게 하며, 도달할 수 없게 된 확인 화면·튜토리얼·결과 왕복 코드를 지운다.

**Architecture:** 이동 경로만 바꾼다. C-104 화면 모양은 지금 그대로다 — 지금 있는 `YGFloatingBarEdit`의 닫기가 그만두기 팝업을, 확인이 「다음」을 맡는다. 화면 개편·감지 실패 토스트는 계획 2(`2026-10-03-topping-edit-ui.md`)가 이 브랜치 위에 쌓는다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3(`Navigator`), 자체 MVI(`BaseViewModel`), Hilt AssistedInject, JUnit + MockK + Turbine

**Spec:** `docs/superpowers/specs/archive/2026-10-03-topping-edit-entry-flow-design.md`

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../../code-conventions.md)를 따른다.
- 커밋 메시지: `type: 한국어 설명`. `Co-Authored-By` 줄을 넣지 않는다.
- 브랜치: `feature/#566-topping-edit-ui-spec` 위에 새 브랜치를 따서 쌓는다(스택 PR).
- 이 계획에서 C-104의 메시지는 지금처럼 `android.widget.Toast`다. 화면 내 토스트는 계획 2 몫이다.
- 매 Task 끝에 `./gradlew ktlintCheck`가 통과해야 한다.
- 문서에 라인번호·hex 색·변동 수치를 적지 않는다.

## Review Focus

- C-104 「다음」 연타·저장 중 탭 → C-106이 한 번만 쌓인다(Task 1의 Route 가드, VM `isSaving` 가드).
- C-106에서 뒤로 와서 다시 「다음」 → 다시 기록하고 다시 간다(Task 1 `clickDone_afterReturningFromPlace_completesAgain`).
- 초안 흐름이 닫힌 채 C-104 「다음」 → 재시도 문구가 아니라 "캔버스에서 다시 시작" 문구(Task 1 `clickDone_recordReturnsFalse_showsDraftUnavailable`).
- 후보 두 번 탭 → C-104가 한 번만 쌓인다(Task 2 기존 `clickCandidate_tappedTwice_persistsOnlyOnce` + Route 가드).
- 갤러리 누끼 연타 → 맞춤 한 번, 이동 한 번(Task 3 `onClickCutoutImage_tappedTwice_ensuresOnce`).
- 중간 커밋의 동작 공백 — Task 1~3 사이에는 확인 화면이 남아 있어 「사진 편집」 → C-104 → 「다음」이 결과를
  돌려주지 않고 C-106으로 간다. 빌드·테스트는 통과한다. 이 계획의 PR은 Task 4까지 끝나기 전에 머지하지 않는다.

---

### Task 1: C-104 「다음」 → C-106, 그만두기 팝업

**Files:**
- Modify: `feature/segmentation/api/src/main/java/com/teamyg/parfait/feature/segmentation/api/NavKeyToppingEdit.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt` (`completion` 인자만 걷는다)
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModelTest.kt`

**Interfaces:**
- Produces:
  - `NavKeyToppingEdit(sourceImageUri: String, segmentationImageUri: String)` — `completion` 삭제, `ToppingEditCompletion` enum 삭제. `ToppingEditResult`·`TOPPING_EDIT_RESULT_KEY`는 확인 화면이 아직 쓰므로 Task 4까지 남긴다.
  - `ToppingEditIntent.ClickClose`, `ConfirmQuit`, `DismissQuit` (data object)
  - `ToppingEditState.showQuitDialog: Boolean = false`
  - `ToppingEditEffect.GoToPlace`, `DraftUnavailable`, `QuitToCanvas` (data object). `EditCompleted`·`GoToConfirm` 삭제
  - 문자열 `topping_edit_draft_unavailable` = 「토핑을 이어서 만들 수 없어요. 캔버스에서 다시 시작해 주세요.」

- [ ] **Step 1: 테스트를 새 계약으로 고친다**

`createViewModel`에서 `completion` 인자를 지운다. 지운다: `confirm` 필드, `result` 픽스처와 `ToppingEditResult` import,
`clickDone_returnResult_completesWithoutRecording`. 대체한다:
- `clickDone_recordAndConfirm_recordsDraftThenGoesToConfirmWithSwappedPaths` → `clickDone_recordsDraftThenGoesToPlace`
- `clickDone_recordReturnsFalse_showsSaveFailed` → `clickDone_recordReturnsFalse_showsDraftUnavailable`
- `clickDone_afterReturningFromConfirm_completesAgain` → `clickDone_afterReturningFromPlace_completesAgain`
- `clickDone_tappedAgainWhileRecording_isIgnored`는 이름을 두고 기대값만 `GoToPlace`로

새 계약과 추가 테스트:

```kotlin
@Test
fun clickDone_recordsDraftThenGoesToPlace() = runTest {
    val viewModel = createViewModel()
    advanceUntilIdle()
    viewModel.effect.test {
        viewModel.processIntent(ToppingEditIntent.ClickDone)
        assertEquals(ToppingEditEffect.GoToPlace, awaitItem())
    }
    coVerify(exactly = 1) {
        recordToppingDraft(subjectImagePath = TRIMMED_PATH, cutoutImagePath = CUTOUT_PATH, sourceLongSide = SourceLongSide(CUTOUT_SIDE))
    }
    assertFalse(viewModel.state.value.isSaving)
}

@Test
fun clickDone_recordReturnsFalse_showsDraftUnavailable()  // coEvery record → false, 기대 DraftUnavailable
@Test
fun clickDone_afterReturningFromPlace_completesAgain()     // 기대 GoToPlace 두 번
// 기존 clickDone_recordThrows_showsSaveFailed 는 그대로 (기대 SaveFailed)

@Test
fun clickClose_showsQuitDialog()          // ClickClose 후 state.showQuitDialog == true
@Test
fun dismissQuit_hidesQuitDialog()         // ClickClose → DismissQuit 후 false, 이펙트 없음
@Test
fun confirmQuit_hidesDialogAndQuitsToCanvas()  // ClickClose → ConfirmQuit: showQuitDialog == false, awaitItem() == QuitToCanvas
@Test
fun confirmQuit_twice_quitsOnce()         // ConfirmQuit 두 번 → QuitToCanvas 한 번, expectNoEvents()
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*ToppingEditViewModelTest"`
Expected: 컴파일 실패(`GoToPlace`·`ClickClose` 없음)

- [ ] **Step 3: VM 구현**

- 생성자에서 `completion`을 지운다. `finishSaving` KDoc의 "확인 화면" 서술을 "토핑 배치(C-106)"로 고친다. `completionEffect`는 기록 결과로 `GoToPlace` / `DraftUnavailable`을 고른다. 기록 예외는 지금처럼 `launch(onError)`가 `SaveFailed`로 받는다.
- 그만두기는 `SegmentationViewModel`과 같은 모양 — `private var isQuitConfirmed`로 `ConfirmQuit`을 한 번만 처리하고, 팝업을 내린 뒤 `QuitToCanvas`.
- `NavKeyToppingEdit`·`ToppingEditCompletion` KDoc에서 `completion` 서술을 걷는다.

- [ ] **Step 4: Route 구현**

- `GoToPlace` → `if (navigator.backStack.lastOrNull() == key) navigator.goTo(NavKeyCanvasToppingPlace)`. 지금 `GoToConfirm`의 가드 주석을 옮기되 "확인 화면"을 "토핑 배치(C-106)"로 고쳐 쓴다(`docs/code-conventions.md` 수명 기준).
- `DraftUnavailable` → `android.widget.Toast`로 `topping_edit_draft_unavailable`.
- `QuitToCanvas` → `navigator.popUpTo<NavKeyCanvasMain>()`.
- `ToppingEditScreen`의 `onClickBack`(지금 플로팅 바 닫기)에 `ClickClose`를 연결한다. 헤더 뒤로 버튼은 계획 2에서 생긴다. 지금은 시스템 뒤로가 `onBack()`이다.
- `state.showQuitDialog`면 `YGModalQuitEdit(onConfirmQuit = ConfirmQuit, onDismiss = DismissQuit)`를 스캐폴드 밖에 띄운다.
- `SegmentationRoute`의 `NavKeyToppingEdit(...)` 호출에서 `completion` 인자를 지운다.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add feature/segmentation
git commit -m "feat: 누끼 편집의 다음이 토핑 배치로 가고 닫기가 그만두기를 묻는다"
```

---

### Task 2: 후보 선택 → C-104

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1의 `NavKeyToppingEdit(sourceImageUri, segmentationImageUri)`
- Produces: `SegmentationEffect.GoToEdit(val segmentationImagePath: String?)` — 값이 있으면 후보 저장본(원본 크기, `PersistSubjectUseCase` 결과의 `subjectImagePath`), `null`이면 감지 실패. `SegmentationEffect.GoToConfirm` 삭제. 생성자에서 `RecordToppingDraftUseCase` 삭제.

- [ ] **Step 1: 테스트를 새 계약으로 고친다**

- `recordToppingDraft` 필드와 그것을 거는 stub·검증을 전부 지운다 — 생성 코드 외에도
  `clickCandidate_succeeds_releasesTheLoadingOverlay`, `clickCandidate_persisting_showsTheLoadingOverlay`,
  `clickCandidate_tappedTwice_persistsOnlyOnce`, `clickCandidate_tapsTheSecondOfTwo_persistsTheTappedCandidate`,
  `clickCandidate_tappedAgainAfterCompletion_persistsAgain`이 stub을 건다.
- `init_segmentationSucceeds_persistsNothingYet`의 `recordToppingDraft` 미호출 검증은 지우고 `persistSubject`
  미호출 검증만 남긴다.
- 지운다: `clickCandidate_succeeds_recordsTheDraftBeforeNavigating`, `selectCandidate_recordsSourceLongSideFromResult`, `clickCandidate_draftIsNotOpen_doesNotNavigate`.
- 더한다:

```kotlin
@Test
fun clickCandidate_succeeds_goesToEditWithFullSizeSubject() = runTest {
    // persistSubject 성공 결과의 subjectImagePath = SUBJECT_PATH, trimmedSubjectImagePath = TRIMMED_PATH
    // 기대: awaitItem() == SegmentationEffect.GoToEdit(segmentationImagePath = SUBJECT_PATH)
}
```

- 바꾼다: `init_decodeFails_goesToEdit`, `init_segmentationReturnsFailure_goesToEdit`, `init_segmentationThrows_goesToEdit`, `init_noSubjectDetected_goesToEdit`, `noSubjectWhileQuitDialogOpen_isHeldUntilDismissed`의 기대값 → `SegmentationEffect.GoToEdit(segmentationImagePath = null)`.
- 나머지 `clickCandidate_*` 테스트의 기대 이펙트 `GoToConfirm(...)` → `GoToEdit(SUBJECT_PATH)`.

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest"`
Expected: 컴파일 실패

- [ ] **Step 3: VM 구현**

`selectCandidate`는 저장 → `releaseLoading()` → `GoToEdit(result.subjectImagePath)`. 쓰지 않게 된 import(`RecordToppingDraftUseCase`, 남는다면 `runSuspendCatching`)는 지운다 — ktlint가 잡는다. 저장 실패는 지금처럼 `ShowError`. KDoc의 "저장 → 초안 기록 → 이동" 순서 근거(c103 다중 선택 스펙 링크)를 이 스펙(`2026-10-03-topping-edit-entry-flow-design.md`)으로 바꾼다. `Outcome.Edit` 전달은 `GoToEdit(null)`.

- [ ] **Step 4: Route 구현**

```kotlin
is SegmentationEffect.GoToEdit -> {
    val path = effect.segmentationImagePath
    if (path == null) {
        navigator.goToAndPopCurrent(NavKeyToppingEdit(key.sourceImageUri, key.sourceImageUri))
    } else if (navigator.backStack.lastOrNull() == key) {
        navigator.goTo(NavKeyToppingEdit(key.sourceImageUri, File(path).toUri().toString()))
    }
}
```

주석: 감지 실패는 분석 화면을 걷어 편집에서 뒤로가 분석으로 돌아오지 않게, 후보는 쌓아서 뒤로가 선택 UI로 돌아오게. 가드는 이펙트 뒤 이동 전의 탭이 두 번 쌓는 것을 막는다.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add feature/segmentation
git commit -m "feat: 누끼 후보를 고르면 확인 화면 없이 누끼 편집으로 간다"
```

---

### Task 3: 갤러리 최근 누끼 → C-106

**Files:**
- Modify: `feature/gallery/impl/build.gradle.kts`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/viewmodel/CustomGalleryPickerViewModel.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/route/CustomGalleryPickerRoute.kt`
- Modify: `feature/gallery/impl/src/main/res/values/strings.xml`
- Test: `feature/gallery/impl/src/test/java/com/teamyg/parfait/feature/gallery/impl/viewmodel/CustomGalleryPickerViewModelTest.kt`

**Interfaces:**
- Produces:
  - `CustomGalleryPickerEffect.NavigateToToppingPlace` (data object), `ShowDraftUnavailable` (data object). `NavigateToSegmentationConfirm` 삭제
  - 생성자에 `ensureDraftSubjectRecorded: EnsureDraftSubjectRecordedUseCase` 추가
  - 문자열 `gallery_cutout_draft_unavailable` = 「토핑을 이어서 만들 수 없어요. 캔버스에서 다시 시작해 주세요.」

- [ ] **Step 1: 테스트**

`createViewModel`에 `ensureDraftSubjectRecorded` mock을 넘긴다(기본 `coEvery { ensureDraftSubjectRecorded(any()) } returns true`). `onClickCutoutImage_navigatesToSegmentationConfirmWithFilePath`를 아래로 바꾼다.

```kotlin
@Test
fun onClickCutoutImage_draftAligned_navigatesToToppingPlace()
    // OnClickCutoutImage(cutout) → awaitItem() == NavigateToToppingPlace
    // coVerify(exactly = 1) { ensureDraftSubjectRecorded(cutout.filePath) } — uri 가 아니라 절대경로다
@Test
fun onClickCutoutImage_draftNotAligned_showsDraftUnavailable()   // returns false → ShowDraftUnavailable
@Test
fun onClickCutoutImage_ensureThrows_showsDraftUnavailable()      // throws IOException → ShowDraftUnavailable
@Test
fun onClickCutoutImage_tappedTwice_ensuresOnce()
    // ensure 가 CompletableDeferred 로 멈춘 동안 두 번 탭 → 풀면 coVerify(exactly = 1), 이펙트 한 번
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:gallery:impl:testDebugUnitTest --tests "*CustomGalleryPickerViewModelTest"`
Expected: 컴파일 실패

- [ ] **Step 3: VM 구현**

`handleOnClickCutoutImage`: `launch(key = ENSURE_CUTOUT_DRAFT_KEY, onError = { postSideEffect(ShowDraftUnavailable) })` 안에서 맞추고 결과로 두 이펙트 중 하나. 주석은 이력("확인 화면에서 옮겼다")이 아니라 이유를 적는다 — C-106은 초안을 읽기만 하므로 들어가기 전에 이 알맹이를 가리키게 해 둬야 한다.

- [ ] **Step 4: Route·의존 구현**

- `NavigateToToppingPlace` → `if (navigator.backStack.lastOrNull() is NavKeyCustomGalleryPicker) navigator.goTo(NavKeyCanvasToppingPlace)`. Route는 키를 받지 않고 풀린 값만 받으므로 타입으로 맨 위를 본다.
- `ShowDraftUnavailable` → `toastPolicy.showError(...)`.
- `build.gradle.kts`: `projects.feature.groups.canvas.api` 추가, `projects.feature.segmentation.api` 제거.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :feature:gallery:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add feature/gallery
git commit -m "feat: 갤러리 최근 누끼를 고르면 초안을 맞추고 토핑 배치로 간다"
```

---

### Task 4: 확인 화면·튜토리얼·결과 왕복 제거

**Files:**
- Delete: `feature/segmentation/api/.../NavKeySegmentationConfirm.kt`
- Delete: `feature/segmentation/impl/.../route/SegmentationConfirmRoute.kt`, `screen/SegmentationConfirmScreen.kt`, `viewmodel/SegmentationConfirmViewModel.kt`
- Delete: `feature/segmentation/impl/src/test/.../viewmodel/SegmentationConfirmViewModelTest.kt`
- Delete: `feature/segmentation/impl/src/main/res/drawable*/img_segmentation_tutorial.png`
- Modify: `feature/segmentation/impl/.../navigation/EntryBuilder.kt` (확인 화면 entry 제거)
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml` (`segmentation_confirm_*` 전부 제거)
- Modify: `feature/segmentation/api/.../NavKeyToppingEdit.kt` (`ToppingEditResult`·`TOPPING_EDIT_RESULT_KEY` 제거)
- Create: `feature/segmentation/impl/.../model/ToppingEditResult.kt` — 기존 `ToppingEditDraft.kt`의 `recordEditResult`와 합쳐도 된다
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/member/TutorialKind.kt`
- Modify: `app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`, `app/src/test/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreenTest.kt`

**Interfaces:**
- Produces: `internal data class ToppingEditResult(subjectImagePath: String, cutoutImagePath: String, sourceLongSide: SourceLongSide)` in impl. `recordEditResult`는 `sourceLongSide`를 그대로 넘긴다.

- [ ] **Step 1: 지운다**

위 Delete 목록과 각 사용처를 지운다. `TutorialKind.SEGMENTATION`을 지우고 KDoc의 "세 화면"·누끼 튜토리얼 서술을 남은 둘(캔버스·업로드)에 맞게 고친다. 분석 매핑의 `NavKeySegmentationConfirm` 줄과 테스트의 그 줄을 지운다.

- [ ] **Step 2: `ToppingEditResult`를 impl로 옮긴다**

KDoc의 "domain을 의존하지 않아 `Int`로 나른다" 근거를 걷는다. 두 경로 KDoc(알맹이 vs 재편집 마스크)은 남긴다.

- [ ] **Step 3: 남은 참조가 없는지 확인**

Run: `grep -rn "SegmentationConfirm\|TOPPING_EDIT_RESULT_KEY\|ToppingEditCompletion\|TutorialKind.SEGMENTATION\|img_segmentation_tutorial" --include='*.kt' --include='*.xml' --include='*.kts' . | grep -v /build/`
Expected: 출력 없음

- [ ] **Step 4: 전체 테스트**

Run: `./gradlew test && ./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add -A feature app domain
git commit -m "refactor: 누끼 확인 화면과 그 튜토리얼, 편집 결과 왕복을 걷는다"
```

---

### Task 5: 문서

**Files:**
- Modify: `docs/status.md` — 「누끼 추출」·「토핑 생성·배치」·튜토리얼 절과 확인 화면을 전제한 ⚠️ 항목을 새 흐름으로 덮어쓴다
- Modify: `docs/architecture/navigation-flow.md` — 토핑 흐름
- Modify: `docs/architecture/state-management.md` — 저장 → 초안 기록 → 이동 순서 계약을 "C-104 「다음」 한 곳이 기록한다"로
- Modify: `docs/synthesis/open-questions.md` — `ResultEffect` 항목의 실사용 근거가 사라졌다
- Modify: `docs/superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md` — 「선택 시점에 일어나는 일의 순서」 절 머리에 이 스펙이 대체한다는 한 줄
- Modify: `docs/superpowers/plans/README.md`, `docs/log.md`(한 줄)

`docs/architecture/design-system.md`의 `YGFloatingBarEdit` 사용처는 계획 2에서 바뀌므로 여기서 건드리지 않는다.

- [ ] **Step 1: 고친다** — 루트 `CLAUDE.md` 「무엇을 적나」 기준. 변경 서술이 아니라 현재 상태로 적는다.

- [ ] **Step 2: 링크 확인**

Run: `python3 docs/script/check_links.py docs`
Expected: 깨진 링크 0건

- [ ] **Step 3: Commit**

```bash
git add docs
git commit -m "docs: 누끼 편집 진입 흐름 단축을 현재 상태 문서에 반영한다"
```
