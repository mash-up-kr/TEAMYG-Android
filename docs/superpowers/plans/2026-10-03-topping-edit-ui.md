---
id: topping-edit-ui
title: 누끼 편집(C-104) 화면 개편과 감지 실패 토스트 (계획 2/2)
status: draft
type: work-order
created: 2026-10-03
updated: 2026-10-03
platforms: android
owner:
related_adr: ADR-0010
related_spec: topping-edit-entry-flow
related_code:
  - feature/segmentation/impl/.../screen/ToppingEditScreen.kt#ToppingEditScreen
  - feature/segmentation/impl/.../route/ToppingEditRoute.kt#ToppingEditRoute
  - feature/segmentation/impl/.../viewmodel/ToppingEditViewModel.kt#loadImages
  - feature/segmentation/api/.../NavKeyToppingEdit.kt#NavKeyToppingEdit
archived_reason:
tags: [plan, parfait]
---

# 누끼 편집 화면 개편과 감지 실패 토스트 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** C-104를 Figma `C-104`·`C-104-Toast` 시안으로 바꾸고, 감지 실패로 들어오면 헤더 아래에 안내 토스트를 한 번 띄운다.

**Architecture:** 계획 1(`2026-10-03-topping-edit-entry-navigation.md`) 브랜치 위에 쌓는다. 계획 1이 만든 이펙트·의도(`GoToPlace`·`ClickClose`·`QuitToCanvas`·`DraftUnavailable`)를 그대로 쓰고 화면 레이아웃과 메시지 표시만 바꾼다. 감지 실패 여부는 `NavKeyToppingEdit`에 실어 VM이 `SavedStateHandle`로 한 번만 보낸다.

**Tech Stack:** Kotlin, Jetpack Compose, 디자인시스템(`YGFloatingBar`·`YGButton`·`YGToastHost`), Hilt AssistedInject, JUnit + MockK + Turbine

**Spec:** `docs/superpowers/specs/2026-10-03-topping-edit-entry-flow-design.md`

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다.
- 커밋 메시지: `type: 한국어 설명`. `Co-Authored-By` 줄을 넣지 않는다.
- 색·간격·타이포는 디자인시스템 토큰(`YGTheme.layout`·`YGTheme.typography`·`YGAtomicColors`)으로만 쓴다. Figma 값에 맞는 토큰이 없으면 가장 가까운 토큰.
- Figma: `C-104` `node-id=5461-6875`, `C-104-Toast` `node-id=5477-8932` (파일 키 `QPoxqbNMNktsi8ktua3gMN`).
- 매 Task 끝에 `./gradlew ktlintCheck`가 통과해야 한다.
- 문서에 라인번호·hex 색·변동 수치를 적지 않는다.

## Review Focus

- 감지 실패 진입 → C-106 갔다가 뒤로 → 토스트가 다시 뜨지 않는다(Task 3 — 같은 VM이라 `loadImages`가 다시 돌지 않음. VM 테스트로 재생성 경우를 고정).
- 디코드 실패로 들어온 감지 실패 → 안내 토스트 없이 `LoadFailed`만(Task 3 `init_detectionFailedButLoadFails_onlyReportsLoadFailed`).
- 저장 실패 토스트가 저장 딤에 가려지지 않는다(Task 2 — 호스트를 딤 아래 층에 두고, `finishSaving`이 딤을 내린 뒤 이펙트를 보내는 순서를 기존 테스트 `assertFalse(isSaving)`로 확인).
- 이미지 로딩 중 「다음」 탭 → 아무 일 없음(Task 1 — 버튼 `isEnabled = !state.isLoading`).
- 긴 감지 실패 문구가 좁은 폭에서 잘리지 않는다(Task 3 미리보기 — 작은 폭 `@YGPreview`로 확인).

---

### Task 1: C-104 레이아웃 교체

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/ToppingEditScreen.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: 계획 1의 `ToppingEditIntent.ClickClose`, `ClickDone`
- Produces: `ToppingEditScreen(state, onChangeMode, onChangeBrushWidth, onAddStroke, onClickUndoArea, onClickRedoArea, onClickNext, onClickBack, onClickClose, modifier, toast: @Composable () -> Unit = {})` — `onClickDone`은 `onClickNext`로 이름을 바꾼다. `toast` 슬롯은 Task 2가 채운다.
- 문자열: `topping_edit_title` = 「대상 영역 선택」, `topping_edit_next` = 「다음」. `topping_edit_area_only_title` 삭제.

- [ ] **Step 1: 레이아웃을 바꾼다**

```
Column
├─ YGFloatingBarBackTitleClose(title = topping_edit_title, onBackClick = onClickBack, onCloseClick = onClickClose)
├─ Box(weight 1f, fillMaxWidth — 좌우 여백 없음)
│   ├─ Box(fillMaxSize, 좌우 padding7) { ToppingEditCanvas 또는 로딩 }
│   └─ Box(TopCenter, fillMaxWidth) { toast() }   — 헤더 바로 아래, 화면 폭
├─ 편집 영역 Column(위 gap5, 좌우 padding7, 세로 spacedBy gap3)
│   ├─ Row(라벨 「브러시 크기」 weight 1f, 오른쪽 끝 ToppingEditHistoryActions)  — 간격 gap1
│   ├─ YGSlider
│   └─ Row(YGEditButton 지우기·채우기 weight 1f 씩, 사이 간격 토큰)
└─ YGButton(text = topping_edit_next, buttonType = YGButtonType.Large, isEnabled = !state.isLoading, onClick = onClickNext, 좌우 padding7)
```

- `YGFloatingBarEdit`과 상단 `ToppingEditHistoryActions` 줄을 지운다.
- 토스트가 화면 폭이어야 해서 좌우 여백은 바깥 Box가 아니라 캔버스 Box에만 준다.
- 되돌리기 버튼이 Figma에서 편집 영역 위 경계에 걸친 정도는 미리보기로 대조하고, 라벨 줄 높이가 버튼 높이만큼 늘어나지 않게 맞춘다(필요하면 Row `verticalAlignment = Bottom`).
- 저장 딤(`ToppingEditSavingOverlay`)은 지금처럼 맨 위.

- [ ] **Step 2: Route 연결**

`onClickNext` → `ClickDone`, `onClickBack` → `navigator.onBack()`, `onClickClose` → `ClickClose`.

- [ ] **Step 3: 미리보기 대조**

`PreviewToppingEditScreen`(기본·저장 중)을 Figma 스크린샷과 나란히 본다. 헤더 제목·버튼 줄·「다음」 자리가 맞으면 통과.

- [ ] **Step 4: 빌드·테스트**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add feature/segmentation
git commit -m "feat: 누끼 편집 화면을 대상 영역 선택 시안으로 바꾼다"
```

---

### Task 2: 화면 내 토스트 호스트

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt`
- Modify: `docs/architecture/design-system.md` (`YGFloatingBarEdit` 사용처에서 C-104 제거, 헤더 아래 토스트 호스트를 쓰는 화면에 C-104 추가)

**Interfaces:**
- Consumes: Task 1의 `toast` 슬롯
- Produces: Route 안 `val toastPolicy = rememberYGToastPolicy()`. Task 3이 같은 정책으로 `YGToastType.Edit`을 띄운다.

- [ ] **Step 1: 메시지를 옮긴다**

- `toast = { YGToastHost(policy = toastPolicy, modifier = Modifier.fillMaxWidth()) }`
- `SaveFailed`·`SubjectTooSmall`·`DraftUnavailable` → `toastPolicy.showError(context.getString(...))` (`YGToastType.Fail`)
- `LoadFailed`는 `android.widget.Toast` 그대로 — 띄우자마자 화면을 닫아 화면 내 호스트로는 보이지 않는다(주석으로 남긴다).
- `YGScaffoldV2`에는 `toastPolicy`를 넘기지 않는다. 스캐폴드 자리는 헤더를 덮는다.

- [ ] **Step 2: 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS. 에뮬레이터에서 남은 영역을 다 지우고 「다음」 → 헤더 아래 빨간 문구 토스트.

- [ ] **Step 3: Commit**

```bash
git add feature/segmentation docs/architecture/design-system.md
git commit -m "feat: 누끼 편집의 오류 토스트를 헤더 아래에 띄운다"
```

---

### Task 3: 감지 실패 안내 토스트

**Files:**
- Modify: `feature/segmentation/api/src/main/java/com/teamyg/parfait/feature/segmentation/api/NavKeyToppingEdit.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/ToppingEditRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/ToppingEditViewModelTest.kt`

**Interfaces:**
- Produces:
  - `NavKeyToppingEdit.isDetectionFailed: Boolean = false` — KDoc: 「자동 감지가 대상을 못 찾아 원본 그대로 들어왔다. 첫 진입에 안내 토스트를 띄운다」
  - `ToppingEditEffect.ShowDetectionFailed` (data object)
  - VM 생성자: `@Assisted isDetectionFailed: Boolean`, `savedStateHandle: SavedStateHandle` (Hilt가 주입. `SegmentationConfirmViewModel`이 쓰던 방식)
  - 문자열 `topping_edit_detection_failed` = 「대상 감지에 실패했어요, 영역을 직접 선택해 주세요」

- [ ] **Step 1: 실패하는 테스트**

`createViewModel`에 `isDetectionFailed: Boolean = false`, `savedStateHandle: SavedStateHandle = SavedStateHandle()`를 더한다.

```kotlin
@Test
fun init_detectionFailed_showsGuideOnceAfterLoading()
    // isDetectionFailed = true → effect.test { awaitItem() == ShowDetectionFailed; expectNoEvents() }
@Test
fun init_detectionFailedRecreatedWithSameSavedState_doesNotShowAgain()
    // 같은 SavedStateHandle 로 두 번 생성 → 두 번째 VM 은 expectNoEvents()
@Test
fun init_detectionFailedButLoadFails_onlyReportsLoadFailed()
    // decodeImage → failure, isDetectionFailed = true → awaitItem() == LoadFailed; expectNoEvents()
@Test
fun init_notDetectionFailed_showsNothing()
    // isDetectionFailed = false → expectNoEvents()
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*ToppingEditViewModelTest"`
Expected: 컴파일 실패

- [ ] **Step 3: 구현**

- VM: `loadImages` 성공 분기에서 `isDetectionFailed && savedStateHandle[KEY_DETECTION_FAILED_SHOWN] != true`면 표시를 남기고 `ShowDetectionFailed`. 성공 분기인 이유를 주석으로 — 디코드 실패면 곧 닫히는 화면에 "직접 선택해 주세요"를 띄우지 않는다.
- Factory에 `isDetectionFailed` 추가, Route가 `key.isDetectionFailed`를 넘긴다.
- Route: `ShowDetectionFailed` → `toastPolicy.show(YGToastType.Edit(getString(topping_edit_detection_failed)))`.
- `SegmentationRoute`의 감지 실패 갈래(`segmentationImagePath == null`)에 `isDetectionFailed = true`.

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest && ./gradlew ktlintCheck`
Expected: PASS

- [ ] **Step 5: 미리보기**

토스트를 띄운 상태의 미리보기를 하나 더한다(작은 폭 포함). 토스트는 Route의 정책으로만 뜨므로 미리보기는 `toast = { YGToast(type = YGToastType.Edit(...)) }`로 정적 슬롯을 넘긴다. Figma `C-104-Toast`와 대조 — 헤더 바로 아래, 화면 폭, 노란 글자.

- [ ] **Step 6: Commit**

```bash
git add feature/segmentation
git commit -m "feat: 대상 감지에 실패해 들어온 누끼 편집에 안내 토스트를 띄운다"
```

---

### Task 4: 문서·스펙 마감

**Files:**
- Modify: `docs/status.md` — 「누끼 추출」에 C-104 화면 구성(헤더·「다음」·감지 실패 토스트·화면 내 토스트 층) 반영
- Modify: `docs/superpowers/specs/2026-10-03-topping-edit-entry-flow-design.md` — `status: implemented`, `verified` 갱신 후 `archive/`로 옮기고 `specs/README.md` 아카이브 표로 행을 옮긴다
- Move: 계획 1·2를 `docs/superpowers/plans/archive/`로 — 각각 `status: done`, `archived_reason`, 상단 Archived 배너(`plans/template.md` 사용법). `plans/README.md`는 활성 표에서 아카이브 표로 행을 옮긴다
- Modify: `docs/log.md` 한 줄

- [ ] **Step 1: 고친다**

- [ ] **Step 2: 링크 확인**

Run: `python3 docs/script/check_links.py docs`
Expected: 깨진 링크 0건

- [ ] **Step 3: Commit**

```bash
git add docs
git commit -m "docs: 누끼 편집 화면 개편을 현재 상태 문서에 반영하고 스펙·계획을 내린다"
```
