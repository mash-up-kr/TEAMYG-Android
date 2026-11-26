# 혼자 공간 (Solo Space) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버가 닫힌 뒤에도 쓰는, 기기에 하나뿐인 로컬 캔버스(혼자 공간)를 만든다.

**Architecture:** 그룹 캔버스의 화면 composable·UiState는 함께 쓰고, ViewModel·use case·저장소는 혼자 공간 전용으로 따로 둔다. 혼자 공간 저장소는 `filesDir/solo_space/`의 JSON + 이미지 파일이고 remote 의존이 없다. 토핑 흐름(카메라·갤러리·누끼)은 `ToppingDraftTarget`으로 공간을 나르고, 캔버스 복귀는 표식 인터페이스 + `Navigator.popUpTo`의 `isInstance` 비교로 한다.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt(assisted ViewModel), Navigation3 `NavKey`, kotlinx.serialization(`@LocalJson`), coroutines/Flow, JUnit4 + kotlin.test + MockK + Turbine.

**Spec:** [`docs/superpowers/specs/2026-11-26-solo-space-design.md`](../specs/2026-11-26-solo-space-design.md) — 실행자는 계획과 스펙을 함께 읽는다.

## Global Constraints

- 주석·KDoc은 [`docs/code-conventions.md`](../../code-conventions.md)를 따른다. 서브에이전트 브리프에 이 문서를 링크한다.
- 커밋 메시지: `type: 한국어 설명`. **Co-Authored-By 줄을 넣지 않는다.** 원격 push·PR은 하지 않는다.
- 혼자 공간 ViewModel(`Solo*ViewModel`)은 `AnalyticsLogger`와, 서버를 부르는 저장소에 닿는 use case를 의존하지 않는다. 허용: `domain/usecase/solo/*`, 토핑 초안 use case, 기기 로컬 use case(최근 이미지, 갤러리·영상 저장, 이미지 디코드·캐시 복사).
- 그룹 캔버스 동작과 화면 모습은 바뀌지 않는다. 각 Task 끝에 해당 모듈의 기존 테스트가 전부 통과해야 한다.
- feature impl의 비 UI 타입(데이터 클래스, enum, 상태)은 `impl/model`에 파일 하나당 타입 하나. 새 컴포넌트에는 프리뷰를 둔다.
- 스펙이 고정한 문구(그대로 쓴다):
  - 제목 "내 공간", 메뉴 "비우기" / "삭제하기", 버튼 "그만두기"
  - 비우기 팝업: "캔버스를 비울까요?" / "올린 사진·글·배경이 모두 지워지고\n되돌릴 수 없어요"
  - 삭제 팝업: "내 공간을 삭제할까요?" / "캔버스와 올린 사진이 모두 사라지고\n되돌릴 수 없어요"
  - 실패 토스트: "캔버스를 비우지 못했어요, 잠시 후 다시 시도해 주세요", "내 공간을 삭제하지 못했어요, 잠시 후 다시 시도해 주세요"
- 체리 탭 영역: 그려진 이미지 경계 대비 너비 0.24, 높이 0.28, 중심 (0.5, 0.19). 5번째 탭에 이동, 횟수 0으로.
- 테스트 명령: domain은 JVM이라 `./gradlew :domain:test --tests '<클래스>'`, 그 밖의 모듈은 `./gradlew :<모듈>:testDebugUnitTest --tests '<클래스>'`.
- 문서를 옮기거나 링크를 바꾼 뒤 `python3 docs/script/check_links.py docs`가 깨진 링크 0건.

## Review Focus

1. **혼자 공간에서 시작한 토핑 흐름을 취소·완료했을 때 그룹 캔버스로 튀는 것** — 백스택에 그룹 캔버스와 혼자 공간이 함께 있으면(설정 진입) 가장 위의 혼자 공간으로 돌아가야 한다. Task 1 테스트가 섞인 스택을 고정한다.
2. **업데이트 직후 남아 있던 옛 초안** — `isSolo` 없는 JSON은 그룹 초안으로 읽혀 기존 흐름이 이어져야 한다. Task 2 테스트가 고정한다.
3. **앱이 저장 중 죽거나 저장이 실패한 뒤** — 기존 캔버스가 그대로 남아야 한다(빈 캔버스로 덮이면 안 된다). Task 4의 롤백·손상 파일 테스트가 고정한다.
4. **비우기·삭제 버튼 연타** — `reset()`·`delete()`는 한 번만, 삭제 성공 시 화면은 한 번만 닫힌다. Task 5 테스트가 고정한다.
5. **로그인 화면 체리 영역이 로그인 버튼·페이저 스와이프를 먹는 것** — 탭 영역은 체리 위에만 있고 스와이프는 그대로 넘어가야 한다. Task 10의 실기기 확인 단계가 확인한다.

---

### Task 1: 캔버스 복귀를 표식 인터페이스로

**Files:**
- Modify: `core/navigation/src/main/java/com/teamyg/parfait/core/navigation/Navigator.kt` (`popUpTo(type)`)
- Create: `feature/groups/canvas/api/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/api/CanvasHomeNavKey.kt`, `CanvasBGEditNavKey.kt`
- Modify: `NavKeyCanvasMain.kt`, `NavKeyCanvasBGEdit.kt`(같은 api 패키지) — 표식 구현
- Modify(호출부 9곳): `ToppingEditRoute.kt`, `SegmentationRoute.kt`, `PictureConfirmRoute.kt`(2곳), `CustomCameraRoute.kt`, `CustomGalleryPickerRoute.kt`, `CanvasToppingArrangeRoute.kt`, `CanvasToppingPlaceRoute.kt`(2곳)
- Test: `core/navigation/src/test/java/com/teamyg/parfait/core/navigation/NavigatorTest.kt`

**Interfaces:**
- Produces: `interface CanvasHomeNavKey : NavKey`, `interface CanvasBGEditNavKey : NavKey`. 이후 Task의 `NavKeySoloCanvas`·`NavKeySoloBGEdit`가 각각 구현한다.

- [ ] **Step 1: 실패하는 테스트** — `NavigatorTest`에 테스트 전용 표식 인터페이스와 그것을 구현하는 키 둘(`HomeA`, `HomeB`)을 두고:
  - `popUpTo_markerInterface_findsImplementingKey`: 스택 `[Root, HomeA, X, Y]`에서 `popUpTo<Marker>()` → `true`, 스택 `[Root, HomeA]`.
  - `popUpTo_markerInterface_picksTopmostOfMixedStack`: 스택 `[Root, HomeA, X, HomeB, Y]` → `[Root, HomeA, X, HomeB]`.
  - `popUpTo_markerInterface_absent_changesNothing`: 표식 키가 없으면 `false`, 스택 그대로.
- [ ] **Step 2: 실패 확인** — `./gradlew :core:navigation:testDebugUnitTest --tests 'com.teamyg.parfait.core.navigation.NavigatorTest'` → 새 테스트 FAIL.
- [ ] **Step 3: 구현** — `popUpTo(type)`의 `it::class == type`을 `type.isInstance(it)`로 바꾸고 KDoc의 "타입으로 찾는다" 설명을 표식 인터페이스까지 포함하게 고친다. 표식 인터페이스 둘을 만들고 그룹 키 둘이 구현하게 한다. 호출부 9곳을 `popUpTo<CanvasHomeNavKey>()` / `popUpTo<CanvasBGEditNavKey>()`로 바꾼다.
- [ ] **Step 4: 통과 확인** — 같은 명령 PASS. `./gradlew :feature:segmentation:impl:testDebugUnitTest :feature:camera:impl:testDebugUnitTest :feature:gallery:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest` PASS(테스트 소스셋이 없는 모듈은 `compileDebugKotlin`).
- [ ] **Step 5: 커밋** — `refactor: 캔버스 복귀를 표식 인터페이스로 찾기`

### Task 2: 토핑 초안이 공간을 싣기

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraftTarget.kt`
- Modify: `domain/.../model/topping/ToppingDraft.kt`, `domain/.../repository/topping/ToppingDraftRepository.kt`(`start`), `domain/.../usecase/topping/StartToppingDraftUseCase.kt`
- Modify: `data/.../model/entity/ToppingDraftEntity.kt`, `data/.../model/mapper/entity/ToppingDraftEntityMapper.kt`, `data/.../repository/topping/ToppingDraftRepositoryImpl.kt`, `data/.../source/topping/local/ToppingDraftLocalDataSource(Impl).kt`
- Modify: `CanvasMainViewModel.kt`(초안 시작), `CanvasToppingPlaceViewModel.kt`, `CanvasTextInputViewModel.kt`(`draft.groupId`·`parfaitId` 읽는 곳)
- Test: `data/src/test/java/com/teamyg/parfait/data/model/mapper/entity/ToppingDraftEntityMapperTest.kt`(없으면 생성), 기존 domain·data·canvas 테스트의 초안 생성부

**Interfaces:**
- Produces:
  ```kotlin
  sealed interface ToppingDraftTarget {
      data class Group(val groupId: GroupId, val parfaitId: ParfaitId) : ToppingDraftTarget
      data object Solo : ToppingDraftTarget
  }
  // ToppingDraft: groupId·parfaitId 를 지우고 val target: ToppingDraftTarget
  // StartToppingDraftUseCase.invoke(target: ToppingDraftTarget, nextPositionZ: Int)
  ```
- `ToppingDraftEntity`: `groupId: Long? = null`, `parfaitId: Long? = null`, `isSolo: Boolean = false`. 매퍼: `isSolo == true` → `Solo`, 아니면 둘 다 있을 때 `Group`. 그룹인데 id가 비면 초안 없음(`null`)으로 읽는다.

- [ ] **Step 1: 실패하는 테스트** — 매퍼 테스트:
  - `legacyJsonWithoutIsSolo_decodesAsGroup`: `{"groupId":1,"parfaitId":100,"nextPositionZ":3}` → `target == Group(GroupId(1), ParfaitId(100))`, `nextPositionZ == 3`.
  - `soloDraft_roundTrips`: `Solo` 초안 → 엔티티 → JSON → 엔티티 → 초안이 원본과 같다.
  - `groupDraft_roundTrips`: 같은 왕복을 `Group`으로.
- [ ] **Step 2: 실패 확인** — `./gradlew :data:testDebugUnitTest --tests '*ToppingDraftEntityMapperTest'` → 컴파일 실패/FAIL.
- [ ] **Step 3: 구현** — 위 Interfaces대로 바꾼다. 그룹 ViewModel 셋은 `draft.target as? ToppingDraftTarget.Group`으로 꺼내 쓰고, `Group`이 아니면 배치·텍스트 저장을 하지 않고 기존 "캔버스로 돌아가기" Effect를 낸다. 애널리틱스 `group_id`와 `refreshTodayParfaitDetail` 인자는 `Group`에서 꺼낸다.
- [ ] **Step 4: 통과 확인** — `./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest :feature:gallery:impl:testDebugUnitTest` PASS.
- [ ] **Step 5: 커밋** — `refactor: 토핑 초안이 그룹·혼자 공간 대상을 싣도록 변경`

### Task 3: 혼자 공간 도메인

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/solo/SoloCanvasVO.kt`, `SoloBackgroundEdit.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/solo/SoloSpaceRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/solo/` — `GetSoloCanvasFlowUseCase`, `ChangeSoloBackgroundUseCase`, `PlaceSoloToppingUseCase`, `UpdateSoloTransformsUseCase`, `UpdateSoloToppingBorderUseCase`, `DeleteSoloToppingUseCase`, `PlaceSoloTextUseCase`, `ResetSoloSpaceUseCase`, `DeleteSoloSpaceUseCase`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/solo/ChangeSoloBackgroundUseCaseTest.kt`

**Interfaces:**
- Produces: 스펙 「도메인 › 모델」「저장소」의 `SoloCanvasVO`, `SoloBackgroundEdit`, `SoloSpaceRepository` 시그니처 그대로(`updateText` 없음). use case는 같은 이름의 저장소 메서드에 위임하는 `operator fun invoke`. 예외:
  - `ChangeSoloBackgroundUseCase`: `suspend fun color(hex: String): Result<Unit>`, `suspend fun image(uri: String): Result<Unit>`. `image`는 `ImageFileRepository.copyToCache(uri)`로 캐시 사본 경로를 얻어 `SoloBackgroundEdit.Image(filePath)`로 넘긴다.
  - `PlaceSoloToppingUseCase.invoke(filePath: String, transform: ToppingTransform, border: ToppingBorder): Result<Unit>`.

- [ ] **Step 1: 실패하는 테스트** — `ChangeSoloBackgroundUseCaseTest`:
  - `image_copiesToCacheThenChangesWithCachedPath`: `copyToCache("content://x")` → `"/cache/a.jpg"`이면 저장소가 `SoloBackgroundEdit.Image("/cache/a.jpg")`로 1회 불린다.
  - `image_copyFails_doesNotTouchRepository`: `copyToCache` 실패면 그 실패를 돌려주고 저장소 호출 0회.
- [ ] **Step 2: 실패 확인** — `./gradlew :domain:test --tests '*ChangeSoloBackgroundUseCaseTest'` FAIL.
- [ ] **Step 3: 구현** — 모델·인터페이스·use case 작성.
- [ ] **Step 4: 통과 확인** — 같은 명령 PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 도메인 계약 추가`

### Task 4: 혼자 공간 로컬 저장소

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/model/entity/SoloSpaceRecordEntity.kt`, `data/.../model/mapper/entity/SoloSpaceRecordEntityMapper.kt`
- Create: `data/.../source/solo/local/SoloSpaceLocalDataSource.kt`, `SoloSpaceLocalDataSourceImpl.kt`(`@Singleton`)
- Create: `data/.../repository/solo/SoloSpaceRepositoryImpl.kt`
- Modify: `data/.../di/LocalDataSourceModule.kt`, `data/.../di/RepositoryModule.kt`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/xml/backup_rules.xml`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/solo/SoloSpaceRepositoryImplTest.kt`

**Interfaces:**
- Consumes: Task 3의 `SoloSpaceRepository`, `SoloCanvasVO`, `SoloBackgroundEdit`.
- `SoloSpaceLocalDataSource`는 파일 I/O만 맡는다(루트 디렉터리, 기록 읽기·원자적 쓰기, 이미지 복사·삭제). 루트 `File`을 생성 인자로 받을 수 있게 해 테스트가 임시 디렉터리를 넘긴다. 실제 루트는 `context.filesDir/solo_space`.
- `SoloSpaceRepositoryImpl` 생성 인자: 데이터소스, `kotlin.time.Clock`, `CoroutineDispatcher`(기본 `Dispatchers.IO`). 메모리 SSoT(`MutableStateFlow`), `Mutex`, 쓰기 순서, 읽기 실패 규칙, `nextId`, 파일명 `<UUID>.png`/`<UUID>.jpg`는 스펙 「데이터 › 구현」 그대로.
- 로컬 I/O 실패는 `AppError.Unexpected`로 감싼다(네트워크 계열 금지).
- 백업: `data_extraction_rules.xml`의 `<cloud-backup>`에 `<exclude domain="file" path="solo_space/"/>`, `<device-transfer>`에는 넣지 않는다. `backup_rules.xml`에 같은 exclude.

- [ ] **Step 1: 실패하는 테스트** — 임시 디렉터리(`kotlin.io.path.createTempDirectory`)와 고정 `Clock`, `UnconfinedTestDispatcher`:
  - `noFile_emitsEmptyCanvas`
  - `placeTopping_copiesImageAndEmitsTopping`: 결과 토핑 `isMine == true`, `imageUrl`이 `file://`로 시작하고 그 파일이 존재
  - `changeBackgroundImage_deletesOldImageAfterSave`
  - `deleteTopping_deletesImageFile`
  - `saveFails_rollsBackMemoryAndDeletesNewCopy`: 데이터소스 쓰기를 실패시키면 `Result.isFailure`, `canvas` 마지막 값은 이전 그대로, 새 사본 파일 없음
  - `reset_emptiesCanvasAndImagesKeepsNextId`: 비운 뒤 새 토핑 id가 비우기 전 최대 id보다 크다
  - `delete_thenResubscribe_emitsEmptyCanvas`
  - `corruptedFile_emitsFailureAndKeepsFile`: `canvas.json`에 `"{broken"` → `canvas`가 실패 방출, 파일 내용 그대로
  - `corruptedFile_resetRecovers`
  - `concurrentWrites_allPersisted`: `placeText` 20개를 동시에 → 다시 읽으면 텍스트 20개, id 중복 없음
- [ ] **Step 2: 실패 확인** — `./gradlew :data:testDebugUnitTest --tests '*SoloSpaceRepositoryImplTest'` FAIL.
- [ ] **Step 3: 구현** — 위 Interfaces와 스펙대로. DI 바인딩과 백업 XML을 함께 넣는다.
- [ ] **Step 4: 통과 확인** — 같은 명령 PASS, `./gradlew :data:testDebugUnitTest :app:assembleDebug` PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 로컬 저장소 추가`

### Task 5: 혼자 공간 캔버스 화면

**Files:**
- Create: `feature/groups/canvas/api/.../NavKeySoloCanvas.kt` — `@Serializable data object NavKeySoloCanvas : CanvasHomeNavKey`
- Create: `feature/groups/canvas/impl/.../solo/SoloCanvasViewModel.kt`, `solo/SoloCanvasRoute.kt`, `solo/SoloCanvasVOMapper.kt`
- Create: `feature/groups/canvas/impl/.../model/SoloDialog.kt`, `component/SoloSpaceMenu.kt`(+프리뷰)
- Modify: `screen/CanvasMainScreen.kt`(그룹 전용 요소 숨김), `navigation/EntryBuilder.kt`, `res/values/strings.xml`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/solo/SoloCanvasViewModelTest.kt`

**Interfaces:**
- Consumes: Task 3 use case(`GetSoloCanvasFlowUseCase`, `ResetSoloSpaceUseCase`, `DeleteSoloSpaceUseCase`), Task 2 `StartToppingDraftUseCase(target = ToppingDraftTarget.Solo, ...)`, 기존 `SaveCanvasToGalleryUseCase`·`SaveCanvasVideoToGalleryUseCase`·`ObserveParfaitDayBoundaryUseCase`.
- Produces: `NavKeySoloCanvas`. 이 Task는 메뉴·카메라·갤러리·이미지 저장·영상 저장까지 연결한다. 배경·텍스트·재편집으로 가는 이동은 Task 6·8·9가 각자 키를 만들며 연결하고, 그 전까지 해당 콜백은 아무것도 하지 않는다.
- `SoloDialog { Menu, ResetConfirm, DeleteConfirm }`(enum). ViewModel 상태는 `CanvasMainUiState`에 `soloDialog: SoloDialog?`, `isSoloActionRunning: Boolean`을 더하거나 별도 필드로 Route에 넘긴다 — 구현자 선택, 단 그룹 화면 동작 불변.
- `SoloCanvasVO.toCanvasVO(today: LocalDate, title: String): CanvasVO` — 스펙 「혼자 공간 캔버스」의 고정값(`ParfaitId(0)`, `ACTIVE`, `lastClosedDate = null`, `members` 빈 목록).
- `CanvasMainScreen`: 캘린더 아이콘·멤버 칩을 숨기고 날짜를 비버튼 글자로 그리는 경로를 연다(nullable `onClickDateSelect` 또는 UiState 플래그). 그룹 호출부는 기존 값 그대로.

- [ ] **Step 1: 실패하는 테스트** — 가짜 use case(MockK)와 `MainDispatcherRule`, Turbine:
  - `init_emitsCanvasWithSoloTitleAndNoMembers`
  - `menuClick_opensMenu_thenResetConfirm_callsResetOnce`: 확인을 3번 연타해도 `reset` 1회
  - `resetFailure_emitsResetFailedToast`
  - `deleteSuccess_emitsCloseEffectOnce`
  - `deleteFailure_emitsDeleteFailedToast`
  - `menuClickWhileActionRunning_doesNotOpenMenu`
  - `canvasLoadFailure_stopsLoadingAndEmitsError`
  - `cameraClick_startsSoloDraft`: `StartToppingDraftUseCase`가 `ToppingDraftTarget.Solo`로 불린다
- [ ] **Step 2: 실패 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests '*SoloCanvasViewModelTest'` FAIL.
- [ ] **Step 3: 구현** — ViewModel·Route·매퍼·메뉴(그룹 목록 "그룹 추가하기" 드롭다운 모양, 딤·뒤로가기는 메뉴만 닫음)·`YGModalPopup` 확인 팝업(`ic_warning_round`)·문자열·엔트리 등록. 날짜는 `ObserveParfaitDayBoundaryUseCase`로 갱신한다.
- [ ] **Step 4: 통과 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest` PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 캔버스 화면 추가`

### Task 6: 혼자 공간 배경 편집

**Files:**
- Create: `feature/groups/canvas/api/.../NavKeySoloBGEdit.kt` — `data object NavKeySoloBGEdit : CanvasBGEditNavKey`
- Create: `feature/groups/canvas/impl/.../solo/SoloBGEditViewModel.kt`, `solo/SoloBGEditRoute.kt`
- Modify: `CanvasBGEditViewModel.kt`(공용 순수 계산 추출 시), `navigation/EntryBuilder.kt`, `solo/SoloCanvasRoute.kt`(배경 버튼 연결)
- Test: `.../solo/SoloBGEditViewModelTest.kt`

**Interfaces:**
- Consumes: `GetSoloCanvasFlowUseCase`, `ChangeSoloBackgroundUseCase.color/image`. 화면은 `CanvasBGEditScreen`·`CanvasBGEditUiState` 재사용.

- [ ] **Step 1: 실패하는 테스트** — `CanvasBGEditViewModelTest`의 시나리오를 혼자 공간 판으로: 색 선택 후 확인 → `color(hex)` 1회 후 닫기 Effect; 사진 선택 후 확인 → `image(uri)` 1회; 실패 → 오류 토스트이고 문구가 네트워크 계열이 아님; 변경 없이 닫기 → use case 0회.
- [ ] **Step 2: 실패 확인** — `--tests '*SoloBGEditViewModelTest'` FAIL.
- [ ] **Step 3: 구현** — 그룹 ViewModel과 겹치는 순수 계산은 `impl/util`로 빼서 둘이 함께 부른다(그룹 동작 불변).
- [ ] **Step 4: 통과 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest` PASS(그룹 `CanvasBGEditViewModelTest` 포함).
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 배경 편집 추가`

### Task 7: 혼자 공간 토핑 배치와 배치 화면 고르기

**Files:**
- Create: `feature/groups/canvas/api/.../NavKeySoloToppingPlace.kt`(`data object`)
- Create: `feature/groups/canvas/impl/.../solo/SoloToppingPlaceViewModel.kt`, `solo/SoloToppingPlaceRoute.kt`
- Modify: `CanvasToppingPlaceViewModel.kt`(공용 순수 계산 추출: 초기 배치, 다음 z, 변형 스냅 처리), `navigation/EntryBuilder.kt`
- Modify: `feature/segmentation/impl/.../ToppingEditViewModel.kt`·`route/ToppingEditRoute.kt`, `feature/gallery/impl/.../CustomGalleryPickerViewModel.kt`·`route/CustomGalleryPickerRoute.kt` — 초안 `target`으로 배치 키 고르기
- Test: `.../solo/SoloToppingPlaceViewModelTest.kt`, 추출한 util의 테스트, `ToppingEditViewModelTest`·`CustomGalleryPickerViewModelTest`(있으면 추가, 없으면 생성)

**Interfaces:**
- Consumes: 초안 use case(`GetToppingDraftFlowUseCase`, `ClearToppingDraftUseCase`), `GetSoloCanvasFlowUseCase`, `PlaceSoloToppingUseCase`, `AddRecentImageUseCase`. 화면은 `CanvasToppingPlaceScreen`·`CanvasToppingPlaceUiState` 재사용.
- 배치 키 고르기: `Group` → `NavKeyCanvasToppingPlace`, `Solo` → `NavKeySoloToppingPlace`. ViewModel Effect에 실을지 Route가 고를지는 구현자 선택.

- [ ] **Step 1: 실패하는 테스트**
  - `SoloToppingPlaceViewModelTest`: `Solo` 초안 → 확인 시 `PlaceSoloToppingUseCase`가 초안의 cutout 경로·스펙 초기 배치 변형으로 1회, 성공 시 초안 정리와 캔버스 복귀 Effect; `Group` 초안이면 배치 0회·복귀 Effect; 실패 시 비네트워크 오류 Effect.
  - 누끼 편집·갤러리: `Solo` 초안이면 혼자 공간 배치로, `Group` 초안이면 기존 배치로 가는 Effect.
- [ ] **Step 2: 실패 확인** — 해당 모듈 `testDebugUnitTest --tests` FAIL.
- [ ] **Step 3: 구현** — 순수 계산 추출 후 그룹 ViewModel이 같은 함수를 부르게 한다.
- [ ] **Step 4: 통과 확인** — `:feature:groups:canvas:impl`, `:feature:segmentation:impl`, `:feature:gallery:impl`의 `testDebugUnitTest` PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 토핑 배치 추가`

### Task 8: 혼자 공간 텍스트 입력

**Files:**
- Create: `feature/groups/canvas/api/.../NavKeySoloTextInput.kt`(`data object`)
- Create: `feature/groups/canvas/impl/.../solo/SoloTextInputViewModel.kt`, `solo/SoloTextInputRoute.kt`
- Modify: `CanvasTextInputViewModel.kt`(공용 순수 계산 추출 시), `navigation/EntryBuilder.kt`, `solo/SoloCanvasRoute.kt`(텍스트 버튼 → 초안 시작 후 `NavKeySoloTextInput`)
- Test: `.../solo/SoloTextInputViewModelTest.kt`

**Interfaces:**
- Consumes: 초안 use case, `GetSoloCanvasFlowUseCase`, `PlaceSoloTextUseCase`. 화면은 `CanvasTextInputScreen` 재사용.

- [ ] **Step 1: 실패하는 테스트** — 내용 입력 후 확인 → `PlaceSoloTextUseCase` 1회(초안의 `nextPositionZ` 반영), 성공 시 복귀; 빈 내용이면 0회; `Group` 초안이면 0회·복귀; 실패 시 비네트워크 오류.
- [ ] **Step 2: 실패 확인** — `--tests '*SoloTextInputViewModelTest'` FAIL.
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `:feature:groups:canvas:impl:testDebugUnitTest` PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 텍스트 입력 추가`

### Task 9: 혼자 공간 토핑 재편집

**Files:**
- Create: `feature/groups/canvas/api/.../NavKeySoloToppingArrange.kt` — `data class NavKeySoloToppingArrange(val initialToppingId: Long, val initialIsText: Boolean) : NavKey`
- Create: `feature/groups/canvas/impl/.../solo/SoloToppingArrangeViewModel.kt`, `solo/SoloToppingArrangeRoute.kt`
- Modify: `CanvasToppingArrangeViewModel.kt`(공용 순수 계산 추출 시), `navigation/EntryBuilder.kt`, `solo/SoloCanvasRoute.kt`(토핑·텍스트 탭 연결)
- Test: `.../solo/SoloToppingArrangeViewModelTest.kt`

**Interfaces:**
- Consumes: `GetSoloCanvasFlowUseCase`, `UpdateSoloTransformsUseCase`, `UpdateSoloToppingBorderUseCase`, `DeleteSoloToppingUseCase`, `DeleteSoloTextUseCase`. 화면은 `CanvasToppingArrangeScreen` 재사용.

- [ ] **Step 1: 실패하는 테스트** — 이동 후 확인 → 변경된 토핑·텍스트만 담아 `UpdateSoloTransformsUseCase` 1회; 변경 없음 → 0회; 테두리 변경 → `UpdateSoloToppingBorderUseCase`; 토핑 삭제 → `DeleteSoloToppingUseCase`, 텍스트 삭제 → `DeleteSoloTextUseCase`; 실패 → 비네트워크 오류.
- [ ] **Step 2: 실패 확인** — `--tests '*SoloToppingArrangeViewModelTest'` FAIL.
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — `:feature:groups:canvas:impl:testDebugUnitTest` PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 토핑 재편집 추가`

### Task 10: 진입점 — 로그인 체리, 설정 프로필 카드

**Files:**
- Create: `feature/login/impl/.../model/HiddenTapCounter.kt`(또는 같은 역할의 작은 순수 클래스), `component/CherryTapArea.kt`
- Modify: `feature/login/impl/.../component/OnboardingPager.kt`, `route/LoginRoute.kt`, `feature/login/impl/build.gradle.kts`(canvas api 의존)
- Modify: `feature/app/setting/impl/.../component/ProfileCard.kt`, `screen/AppSettingScreen.kt`, `route/AppSettingRoute.kt`, `feature/app/setting/impl/build.gradle.kts`
- Test: `feature/login/impl/src/test/.../model/HiddenTapCounterTest.kt`

**Interfaces:**
- Produces: `class HiddenTapCounter(private val required: Int = 5) { fun tap(): Boolean }` — `required`번째 호출에 `true`를 돌려주고 0으로 되돌린다. 두 화면이 함께 쓸 수 있게 `core:ui`에 둘지는 구현자 선택(둘 다 쓰면 `core:ui`).

- [ ] **Step 1: 실패하는 테스트** — `tap()` 4번 `false`, 5번째 `true`, 다시 4번 `false`, 10번째 `true`.
- [ ] **Step 2: 실패 확인** — 해당 모듈 `testDebugUnitTest --tests '*HiddenTapCounterTest'` FAIL.
- [ ] **Step 3: 구현** — 마지막 온보딩 페이지(`image_onboarding_3`) 위에 Global Constraints의 비율로 탭 영역(`pointerInput` 탭 감지만, 드래그 소비 안 함). 5번째 → `navigator.goTo(NavKeySoloCanvas)`. `ProfileCard`에 탭 콜백 → 같은 이동.
- [ ] **Step 4: 통과 확인** — 테스트 PASS. 실기기/에뮬레이터: 로그인 화면 마지막 페이지에서 체리 5번 → 혼자 공간, 체리 위에서 시작한 스와이프로 페이지가 넘어가는지, 로그인 버튼이 그대로 눌리는지 확인.
- [ ] **Step 5: 커밋** — `feat: 로그인·설정에 혼자 공간 숨은 진입점 추가`

### Task 11: 애널리틱스 제외와 구조 보장 테스트

**Files:**
- Modify: `app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`(혼자 공간 키 다섯 → `null`)
- Test: `app/src/test/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreenTest.kt`
- Create: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/solo/SoloViewModelDependencyTest.kt`

- [ ] **Step 1: 실패하는 테스트**
  - `NavKeyAnalyticsScreenTest`: `NavKeySoloCanvas`, `NavKeySoloBGEdit`, `NavKeySoloToppingPlace`, `NavKeySoloTextInput`, `NavKeySoloToppingArrange(1, false)` 모두 `toAnalyticsScreenOrNull() == null`.
  - `SoloViewModelDependencyTest`: 혼자 공간 ViewModel 다섯의 `@Inject`/`@AssistedInject` 생성자 파라미터 타입이 허용 목록(패키지 `domain.usecase.solo`, 명시한 초안·로컬 use case 클래스, `SavedStateHandle`, 원시·assisted 인자) 안에 있는지 리플렉션으로 확인. `AnalyticsLogger`가 있으면 실패.
- [ ] **Step 2: 실패 확인** — `./gradlew :app:testDebugUnitTest --tests '*NavKeyAnalyticsScreenTest'` FAIL. 구조 테스트는 이미 지켜졌으면 바로 PASS할 수 있다 — 허용 목록에서 한 항목을 일부러 빼서 FAIL하는지 확인한 뒤 되돌린다.
- [ ] **Step 3: 구현** — 매핑에 다섯 키를 `null`로.
- [ ] **Step 4: 통과 확인** — 두 테스트 PASS.
- [ ] **Step 5: 커밋** — `feat: 혼자 공간 화면 조회 제외와 의존 검사 추가`

### Task 12: 문서

**Files:**
- Create: `docs/adr/0038-solo-space-separate-viewmodels.md`(`docs/adr/template.md` 형식) — 접근 A·B·C와 B를 고른 이유(스펙 「접근」 요약)
- Modify: `docs/adr/README.md`, `docs/architecture/navigation-flow.md`(혼자 공간 진입·표식 인터페이스 복귀), `docs/architecture/data-layer.md`(혼자 공간 로컬 저장소·백업 규칙), `docs/status.md`, `docs/log.md`, `docs/superpowers/specs/2026-11-26-solo-space-design.md`(`status: in-progress`, `related_adr: …, ADR-0038`), `docs/superpowers/specs/README.md`, `docs/superpowers/plans/README.md`

- [ ] **Step 1: 작성** — 루트 `CLAUDE.md` 「무엇을 적나」 기준(코드로 복원할 수 없는 것만).
- [ ] **Step 2: 확인** — `python3 docs/script/check_links.py docs` → 깨진 링크 0건.
- [ ] **Step 3: 전체 확인** — `./gradlew testDebugUnitTest :domain:test :app:assembleDebug` PASS.
- [ ] **Step 4: 실기기 확인** — 스펙 「테스트 › 실기기 확인」 체크리스트를 에뮬레이터에서 돈다. 못 돈 항목은 사용자 보고에 그대로 적는다.
- [ ] **Step 5: 커밋** — `docs: 혼자 공간 구현 문서 반영`
