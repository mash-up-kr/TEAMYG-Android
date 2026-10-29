---
id: text-topping-api
title: 텍스트 토핑 서버 연동 — 배치·C-001 렌더링·C-305 재편집
status: done
type: work-order
created: 2026-10-26
updated: 2026-10-27
platforms: android
owner:
related_adr: ADR-0029
related_spec: text-topping-api, text-topping
related_code:
  - CanvasTextInputViewModel.kt#handleOnClickStack
  - CanvasToppingArrangeViewModel.kt#handleOnClickConfirm
  - RecreateTextToppingUseCase.kt
  - TextEditSessionHolder.kt
archived_reason: 구현 완료(10 Task 수행, develop 머지 PR #645 `ed88a1547`). Task 7·9의 에뮬레이터 수동 확인은 미수행.
tags: [plan, parfait]
---

# 텍스트 토핑 서버 연동 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** C-106 [캔버스에 쌓기]가 텍스트를 서버에 저장하고, C-001이 캔버스 `texts`를 그리며, C-305에서 내 텍스트를 이동·핀치·삭제·재편집한다.

**Architecture:** 도메인은 `CanvasVO.texts`를 별도 목록으로 두고 텍스트 저장소·유스케이스를 새로 만든다. 일괄 PATCH(`textItems`)는 기존 `ToppingRepository.updateAll`이 함께 싣는다. 피처는 텍스트 렌더러 하나(`TextToppingContent`)를 C-106·C-001·C-305가 공유하고, C-305↔C-106 편집 왕복은 Hilt 싱글턴 홀더로 넘긴다.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, kotlinx.serialization, Retrofit, JUnit + MockK + Turbine.

**Spec:** [`docs/superpowers/specs/2026-10-26-text-topping-api.md`](../../specs/archive/2026-10-26-text-topping-api.md) — 화면 동작 정본은 [`2026-10-16-text-topping.md`](../../specs/archive/2026-10-16-text-topping.md).

## Global Constraints

- 코드 주석·KDoc은 [`docs/code-conventions.md`](../../../code-conventions.md)를 따른다 — 코드가 말하는 것은 쓰지 않고, 다른 컴포넌트의 현재 상태를 단정하지 않는다.
- 커밋 메시지: `type: 한국어 설명` 형식. **Co-Authored-By 줄을 넣지 않는다.**
- 피처의 비-UI 타입(data class·enum·홀더·상수)은 `impl/model` 또는 `impl/util`에 파일당 하나. 새 컴포넌트에는 `@YGPreview` 프리뷰를 둔다.
- 저장 단위: `positionX`·`positionY` = 텍스트 박스 **중심**의 Canvas-Area 대비 비율, `fontSize` = `designFontSize`(디자인 너비 335 기준, ≤24), `scale` = 배치 핀치 배율, `rotation` = 도, `color` = `TextToppingColor.contentColor`의 `#RRGGBB` 대문자, `isFill`·`isStroke` = `TextToppingType.hasFill`·`hasBorder`.
- 읽기: 모르는 색 → `TextToppingColor.Black`. `isFill=false` → `DEFAULT`, `isFill && !isStroke` → `FILL`, `isFill && isStroke` → `FILL_STROKE`.
- 렌더 크기: 캔버스 너비 `W`에서 글자 크기 `fontSize × W / 335`, 래핑 폭 `W × fontSize / 24`, 그 위에 `scale`을 `graphicsLayer`로 곱한다. `fontScale = 1`. 정렬은 저장하지 않고 읽을 때 가운데.
- 겹침 순서: 이미지·텍스트를 `positionZ` 오름차순, 같으면 이미지가 아래, 그다음 id 오름차순. 새 토핑 z = 둘을 합친 최댓값 + 1.
- 텍스트 POST는 자동 재시도하지 않는다(멱등 아님).
- 이미지 id와 텍스트 id는 서로 다른 테이블이라 값이 겹칠 수 있다 — 하나의 Long 집합·키로 섞지 않는다.

## Review Focus

- 텍스트만 있는 캔버스: C-001이 일괄 등장 빗장(`rememberBatchRevealState`)에서 영원히 숨지 않고 바로 보여야 한다 → Task 7 테스트.
- 같은 숫자 id를 가진 이미지와 텍스트: 탭·포커스·Spotlight·dirty가 서로를 건드리지 않아야 한다 → Task 7·8 테스트.
- C-305 내용 수정 저장 중 POST 성공·옛 DELETE 실패: 다시 확인을 눌러도 POST가 두 번 나가지 않아야 한다 → Task 9 테스트.
- C-106 [캔버스에 쌓기] 연타: POST는 한 번만 나가야 한다 → Task 6 테스트.
- 서버가 `#ff6b6b`처럼 소문자·다른 클라이언트 형식으로 준 색: 대소문자 무시로 매칭하고, `rgb(…)` 같은 형식은 Black으로 그려야 한다 → Task 4 테스트.

---

### Task 1: 캔버스 응답의 `texts`를 도메인으로

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasTextToppingVO.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/canvas/CanvasVO.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfait/mapper/VOMapper.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfait/remote/ParfaitRemoteDataSourceImplTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class CanvasTextToppingVO(
      val parfaitTextId: ParfaitTextId,
      val text: String,
      val transform: ToppingTransform,
      val style: TextToppingStyle,
      val placedBy: ToppingPlacerVO,
      val isMine: Boolean,
      val createdAt: LocalDateTime,
  )
  // CanvasVO 에 추가 (기본값 없음 — 생성처는 전부 고친다)
  val texts: List<CanvasTextToppingVO>
  ```

- [x] **Step 1: 실패하는 테스트** — `ParfaitRemoteDataSourceImplTest`에 오늘 조회 응답 JSON에 `texts` 한 건(`parfaitTextId` 7, `color` `"#FA91A2"`, `fontSize` 18.0, `scale` 1.5, `isFill` true, `isStroke` false, `placedBy.ownerType` `"ME"`, `createdAt` `"2026-10-26T10:00:00.123"`)을 넣고 `texts.single()`의 필드가 그대로이며 `isMine == true`인지, `texts`가 `null`이면 `emptyList()`인지 단언한다. `ownerType` `"OTHER"`면 `isMine == false`.
- [x] **Step 2: 실행해 실패 확인** — `./gradlew :data:testDebugUnitTest --tests "*ParfaitRemoteDataSourceImplTest*"` → 컴파일 실패(`texts` 없음).
- [x] **Step 3: 구현** — `CanvasTextToppingVO` 생성, `CanvasVO.texts` 추가(KDoc의 "null 을 빈 목록으로 접는다" 문장에 texts 포함), `toCanvasVO`에서 `texts.orEmpty().map { it.toCanvasTextToppingVO() }`. `isMine`은 이미지와 같은 `OWNER_TYPE_ME` 비교. `CanvasVO(...)`를 만드는 모든 곳(테스트 픽스처 포함, `grep -rn "CanvasVO(" --include='*.kt'`)에 `texts = emptyList()`를 넣는다.
- [x] **Step 4: 통과 확인** — 같은 명령 PASS. 이어 `./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest` 컴파일·통과.
- [x] **Step 5: 커밋** — `feat: 캔버스 응답의 텍스트 토핑을 도메인으로 올린다`

### Task 2: 텍스트 저장소·유스케이스

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/TextToppingRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/PlaceTextToppingUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/UpdateTextToppingUseCase.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/DeleteTextToppingUseCase.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/topping/TextToppingRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/TextToppingRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `ParfaitTextRemoteDataSource`(기존 `placeText`·`updateText`·`deleteText`)
- Produces:
  ```kotlin
  interface TextToppingRepository {
      suspend fun place(groupId: GroupId, parfaitId: ParfaitId, text: String, transform: ToppingTransform, style: TextToppingStyle): Result<PlacedTextToppingVO>
      suspend fun update(groupId: GroupId, parfaitId: ParfaitId, update: TextToppingUpdate): Result<UpdatedTextToppingVO>
      suspend fun delete(groupId: GroupId, parfaitId: ParfaitId, parfaitTextId: ParfaitTextId): Result<Unit>
  }
  class PlaceTextToppingUseCase  { suspend operator fun invoke(groupId, parfaitId, text, transform, style): Result<PlacedTextToppingVO> }
  class UpdateTextToppingUseCase { suspend operator fun invoke(groupId, parfaitId, update: TextToppingUpdate): Result<UpdatedTextToppingVO> }
  class DeleteTextToppingUseCase { suspend operator fun invoke(groupId, parfaitId, parfaitTextId: ParfaitTextId): Result<Unit> }
  ```

- [x] **Step 1: 실패하는 테스트** — `ToppingRepositoryImplTest` 모양을 따라 세 메서드 각각: 성공은 데이터소스 결과를 그대로, `ApiException`·`IOException` 실패는 `AppError`로 매핑(`mapErrorToAppError`)되는지 단언.
- [x] **Step 2: 실패 확인** — `./gradlew :data:testDebugUnitTest --tests "*TextToppingRepositoryImplTest*"` → 컴파일 실패.
- [x] **Step 3: 구현** — 위임 + `mapErrorToAppError()`. `place` KDoc에 "멱등이 아니다 — 부를 때마다 새 행과 푸시"(근거 `api/parfait-text.md`). `RepositoryModule`에 `@Binds`. 유스케이스 셋은 위임만.
- [x] **Step 4: 통과 확인** — 같은 명령 PASS. 바인딩 테스트가 있으면(`ToppingRepositoryBindingTest`) 같은 모양으로 텍스트 바인딩도 하나 추가.
- [x] **Step 5: 커밋** — `feat: 텍스트 토핑 저장소와 유스케이스를 추가한다`

### Task 3: 일괄 PATCH에 텍스트 변형 싣기

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/TextToppingTransformUpdate.kt`
- Modify: `domain/.../repository/topping/ToppingRepository.kt#updateAll`, `domain/.../usecase/topping/UpdateToppingsUseCase.kt`
- Modify: `data/.../repository/topping/ToppingRepositoryImpl.kt`, `data/.../source/topping/remote/ParfaitImageRemoteDataSource.kt`·`Impl.kt#updateToppings`, `data/.../source/topping/mapper/VOMapper.kt#toUpdateRequest`
- Test: `ToppingRepositoryImplTest.kt`, `ParfaitImageRemoteDataSourceImplTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class TextToppingTransformUpdate(
      val parfaitTextId: ParfaitTextId,
      val positionX: Double? = null, val positionY: Double? = null, val positionZ: Int? = null,
      val scale: Double? = null, val rotation: Double? = null,
  )
  // ToppingRepository / UpdateToppingsUseCase / ParfaitImageRemoteDataSource.updateToppings
  suspend fun updateAll(groupId: GroupId, parfaitId: ParfaitId, updates: List<ToppingTransformUpdate>, textUpdates: List<TextToppingTransformUpdate> = emptyList()): Result<List<UpdatedToppingVO>>
  ```
  반환은 이미지 결과만이다 — 호출처는 저장 뒤 캔버스를 다시 읽는다.

- [x] **Step 1: 실패하는 테스트** — (a) 데이터소스: 텍스트 업데이트 하나를 넘기면 요청 `textItems`에 `parfaitTextId`·축 값이 실리고 `items`는 빈 목록. (b) 저장소: `updates`·`textUpdates`가 **둘 다** 비었을 때만 요청 없이 `success(emptyList())`, 텍스트만 있으면 요청이 나간다.
- [x] **Step 2: 실패 확인** — `./gradlew :data:testDebugUnitTest --tests "*ToppingRepositoryImplTest*" --tests "*ParfaitImageRemoteDataSourceImplTest*"` → 컴파일 실패.
- [x] **Step 3: 구현** — `toUpdateRequest`가 두 목록을 받아 `UpdateParfaitImagesRequest(items, textItems)`를 만든다. 빈 요청 가드 주석의 조건을 "둘 다 비면"으로.
- [x] **Step 4: 통과 확인** — 같은 명령 + `./gradlew :feature:groups:canvas:impl:testDebugUnitTest` PASS(기존 호출처는 기본값으로 컴파일된다).
- [x] **Step 5: 커밋** — `feat: 토핑 일괄 수정에 텍스트 변형을 싣는다`

### Task 4: 텍스트 서버 값 변환과 겹침 순서

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/TextToppingWireMapper.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/model/CanvasToppingKey.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/CanvasLayerOrder.kt`
- Modify: `viewmodel/CanvasMainViewModel.kt#nextPositionZ`
- Test: `feature/groups/canvas/impl/src/test/kotlin/.../util/TextToppingWireMapperTest.kt`, `.../util/CanvasLayerOrderTest.kt`, `CanvasMainViewModelTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  internal fun TextToppingColor.toWireColor(): String                 // "#FA91A2"
  internal fun textToppingColorOf(wire: String): TextToppingColor     // 대소문자 무시, 모르면 Black
  internal fun TextToppingType.toFillStroke(): Pair<Boolean, Boolean> // (isFill, isStroke)
  internal fun textToppingTypeOf(isFill: Boolean, isStroke: Boolean): TextToppingType
  internal fun textToppingStyleOf(color: TextToppingColor, type: TextToppingType, designFontSize: Float): TextToppingStyle
  /** 박스 좌상단 Dp 오프셋 → 중심 비율. scale·rotation 은 그대로 */
  internal fun toTextToppingTransform(offsetX: Dp, offsetY: Dp, boxSize: DpSize, canvasSize: DpSize, scale: Float, rotationDegrees: Float, positionZ: Int): ToppingTransform

  sealed interface CanvasToppingKey {
      data class Image(val id: Long) : CanvasToppingKey
      data class Text(val id: Long) : CanvasToppingKey
  }
  /** Global Constraints 의 겹침 순서. 결과 원소는 Either 대신 sealed 로 */
  internal sealed interface CanvasLayerItem { val key: CanvasToppingKey; val positionZ: Int
      data class Image(val topping: CanvasToppingVO) : CanvasLayerItem
      data class Text(val text: CanvasTextToppingVO) : CanvasLayerItem }
  internal fun canvasLayerOrder(images: List<CanvasToppingVO>, texts: List<CanvasTextToppingVO>): List<CanvasLayerItem>
  ```

- [x] **Step 1: 실패하는 테스트**
  - `TextToppingColor.entries.forEach { assertEquals(it, textToppingColorOf(it.toWireColor())) }`; `textToppingColorOf("#fa91a2") == Pink`; `textToppingColorOf("rgb(1,2,3)") == Black`; `TextToppingColor.White.toWireColor() == "#FFFFFF"`.
  - 타입 왕복 3종 + `textToppingTypeOf(false, true) == DEFAULT`.
  - `toTextToppingTransform(offsetX=100.dp, offsetY=50.dp, boxSize=DpSize(40.dp,20.dp), canvasSize=DpSize(200.dp,400.dp), scale=1.5f, rotationDegrees=30f, positionZ=4)` → `positionX 0.6`, `positionY 0.15`, `scale 1.5`, `rotation 30.0`, `positionZ 4`.
  - `canvasLayerOrder`: 이미지 z=2(id 5)·텍스트 z=1(id 9)·텍스트 z=2(id 5) → 순서 `Text(9), Image(5), Text(5)`.
  - `CanvasMainViewModelTest`: 이미지 최대 z 3, 텍스트 최대 z 7인 캔버스에서 토핑 흐름 시작 시 초안 `nextPositionZ == 8`. 텍스트만 z 2면 3.
- [x] **Step 2: 실패 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*TextToppingWireMapperTest*" --tests "*CanvasLayerOrderTest*" --tests "*CanvasMainViewModelTest*"` → 컴파일 실패.
- [x] **Step 3: 구현** — 색 hex는 `Color.toArgb()`에서 RGB 6자리를 대문자로. `nextPositionZ`는 `toppings`와 `texts`의 z를 합친 최댓값.
- [x] **Step 4: 통과 확인** — 같은 명령 PASS.
- [x] **Step 5: 커밋** — `feat: 텍스트 토핑 서버 값 변환과 겹침 순서를 정한다`

### Task 5: 공용 텍스트 렌더러 추출

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/.../component/TextToppingContent.kt`
- Modify: `screen/CanvasTextInputScreen.kt` (배치 모드 렌더를 이 컴포저블로)

**Interfaces:**
- Produces:
  ```kotlin
  /** 저장값으로 텍스트 하나를 그린다. 크기 규칙은 Global Constraints. 위치·scale·rotation 은 호출부 몫 */
  @Composable internal fun TextToppingContent(
      text: String, color: TextToppingColor, type: TextToppingType, align: TextAlign,
      designFontSize: Float, canvasWidth: Dp, modifier: Modifier = Modifier,
  )
  ```
  `fontScale = 1`, `detectTextToppingScript`로 글꼴·자간, `TextToppingBackground`로 배경·테두리, 래핑 폭 = `canvasWidth × designFontSize / TEXT_TOPPING_FONT_SIZE_SP`, `measureTextBoxUnbounded` 유지.

- [x] **Step 1: 추출** — C-106 배치 모드가 지금 그리는 텍스트(편집 모드의 `BasicTextField`가 아닌 쪽)를 `TextToppingContent`로 옮긴다. C-106 동작은 바뀌지 않아야 한다. 같은 파일에 `@YGPreview` 프리뷰(타입 3종 × 짧은/두 줄 텍스트).
- [x] **Step 2: 빌드·회귀 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest :feature:groups:canvas:impl:compileDebugKotlin` PASS. 래핑 폭 공식이 C-106의 `fitTextToppingToHeight` 결과(`fieldMaxWidthDp`)와 같은 값이 되는지 `CanvasTextInputScreen`의 계산과 대조해 리뷰어가 확인할 수 있게 커밋 메시지 본문에 근거 한 줄.
- [x] **Step 3: 커밋** — `refactor: 텍스트 토핑 렌더러를 공용 컴포저블로 뽑는다`

### Task 6: C-106 [캔버스에 쌓기] 저장

**Files:**
- Modify: `viewmodel/CanvasTextInputViewModel.kt`, `route/CanvasTextInputRoute.kt`, `screen/CanvasTextInputScreen.kt`(로딩 중 입력 차단)
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml` (필요 시 — 아래 문구 재사용 우선)
- Test: `CanvasTextInputViewModelTest.kt`

**Interfaces:**
- Consumes: `PlaceTextToppingUseCase`(Task 2), `toTextToppingTransform`·`textToppingStyleOf`(Task 4), 초안 `ToppingDraft.parfaitId`·`nextPositionZ`, `RefreshTodayParfaitDetailUseCase`, `AppError.isPermanentPlaceFailure()`
- Produces: `CanvasTextInputUiState.isLoading: Boolean`; `CanvasTextInputEffect.ShowPlaceFailed(permanent: Boolean)`

- [x] **Step 1: 실패하는 테스트**
  - 내용 "hi", 캔버스·박스 측정 뒤 `OnClickStack` → `placeTextTopping(GroupId(1), ParfaitId(2), "hi", transform(positionZ = 1), style)` 한 번, 그 뒤 `refreshTodayParfaitDetail`·`clearToppingDraft`·`NavigateBack`.
  - `OnClickStack` 두 번 연속(첫 호출이 suspend 중) → place 호출 1회.
  - place가 `AppError.Server(code = PARFAIT_ALREADY_CLOSED)` 실패 → `ShowPlaceFailed(permanent = true)`, `isLoading == false`, 내용 유지, `clearToppingDraft` 미호출, 재시도 없음(place 1회).
  - 네트워크 실패 → `ShowPlaceFailed(permanent = false)`.
  - 분석 이벤트 `CANVAS_TEXT_STACK_TAPPED`는 결과와 무관하게 탭마다 1회.
- [x] **Step 2: 실패 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasTextInputViewModelTest*"` → FAIL.
- [x] **Step 3: 구현** — 초안 구독에서 `parfaitId`·`nextPositionZ`도 들고 있는다. `launch(key = …)` 로 중복 실행 차단(사진 배치의 `CONFIRM_KEY` 패턴). 성공 시 갱신을 기다린 뒤 나간다(라우트가 나가며 scope가 취소되는 문제는 `CanvasToppingArrangeViewModel#handleOnClickConfirm` 주석과 같다). 토스트 문구는 `canvas_topping_place_failed`·`canvas_topping_place_failed_permanently` 재사용. 로딩 중엔 쌓기·제스처를 막는다. 스텁 주석(OQ-P-450) 제거.
- [x] **Step 4: 통과 확인** — 같은 명령 PASS.
- [x] **Step 5: 커밋** — `feat: 텍스트 토핑 쌓기를 서버에 저장한다`

### Task 7: C-001에 텍스트 그리기·탭

**Files:**
- Modify: `component/CanvasToppingLayer.kt`, `screen/CanvasMainScreen.kt`, `component/CanvasVideoRecordLayer.kt`, `screen/CanvasImageSaveScreen.kt`(텍스트 전달), `screen/CanvasTextInputScreen.kt`·`screen/CanvasToppingPlaceScreen.kt`(배경으로 기존 텍스트도 보이게)
- Modify: `viewmodel/CanvasMainViewModel.kt`, `route/CanvasMainRoute.kt`
- Modify: `feature/groups/canvas/api/.../NavKeyCanvasToppingArrange.kt`, `navigation/EntryBuilder.kt`, `route/CanvasToppingArrangeRoute.kt`(인자 전달만)
- Test: `CanvasMainViewModelTest.kt`, `.../util/ToppingHitTestTest.kt`(텍스트 판정)

**Interfaces:**
- Consumes: `canvasLayerOrder`·`CanvasToppingKey`(Task 4), `TextToppingContent`(Task 5)
- Produces:
  ```kotlin
  // CanvasToppingLayer: texts 를 받고, Spotlight·탭 키를 CanvasToppingKey 로
  internal fun CanvasToppingLayer(toppings: List<CanvasToppingVO>, texts: List<CanvasTextToppingVO>, spotlightedKey: CanvasToppingKey?, onClickTopping: (CanvasToppingVO) -> Unit, onClickText: (CanvasTextToppingVO) -> Unit, ...)
  // CanvasMainUiState.spotlightedToppingId → spotlightedKey: CanvasToppingKey?
  CanvasMainIntent.OnClickText(val text: CanvasTextToppingVO)
  CanvasMainEffect.NavigateToToppingArrange(groupId, parfaitId, toppingId: Long, isText: Boolean)
  data class NavKeyCanvasToppingArrange(val groupId: Long, val parfaitId: Long, val initialToppingId: Long, val initialIsText: Boolean = false)
  ```
  텍스트 판정은 `ToppingHitTarget`을 외곽선 없이(`outline = null`) 회전된 박스 크기로 만든다 — 박스 크기는 렌더 때 측정한 값 × `scale`.

- [x] **Step 1: 실패하는 테스트**
  - 내 텍스트 `OnClickText` (오늘 캔버스) → `NavigateToToppingArrange(isText = true, toppingId = text id)`; 과거 캔버스면 효과 없음.
  - 남의 텍스트 → `spotlightedKey == CanvasToppingKey.Text(id)` + `ShowSpotlightToast(nickname, …, elapsed)`.
  - 같은 숫자 id 5의 이미지가 Spotlight인 상태에서 다음 캔버스 방출에 이미지 5가 사라지고 텍스트 5만 남으면 Spotlight가 풀린다(키 종류로 생존 판정).
  - Spotlight 중 텍스트 탭은 무시(이미지 가드와 같다).
  - `ToppingHitTestTest`: 회전 0·90도 텍스트 사각형 안/밖 판정.
- [x] **Step 2: 실패 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasMainViewModelTest*" --tests "*ToppingHitTestTest*"` → FAIL.
- [x] **Step 3: 구현** — 레이어는 `canvasLayerOrder` 순으로 이미지/텍스트를 그린다. 텍스트는 이미지 로딩과 무관하게 `settled`(일괄 등장 빗장에 `true`로 참여). 텍스트만 있으면 바로 보인다. 타임랩스 `toppingPopProgresses`는 합친 순서 기준 인덱스로 바꾼다. 판정 오버레이는 이미지·텍스트 대상을 합친 목록으로 `toppingTapInput`에 넣되 `keyOf`는 `CanvasToppingKey`. Spotlight 딤 위로 올리는 대상도 키로 고른다.
- [x] **Step 4: 통과 확인** — 같은 명령 + 모듈 전체 `testDebugUnitTest` PASS.
- [ ] **Step 5: 수동 확인** — 에뮬레이터에서 C-106으로 텍스트를 쌓고 C-001에 같은 자리·크기로 보이는지, 남의 텍스트 탭 토스트, 이미지 저장 결과물에 텍스트가 찍히는지. 못 하면 PR 본문에 미수행으로 적는다. [^manual]
- [x] **Step 6: 커밋** — `feat: 캔버스 메인에 텍스트 토핑을 그리고 탭을 받는다`

### Task 8: C-305에 텍스트 편입(이동·핀치·삭제·저장)

**Files:**
- Create: `model/EditableText.kt`
- Modify: `viewmodel/CanvasToppingArrangeViewModel.kt`, `screen/CanvasToppingArrangeScreen.kt`, `route/CanvasToppingArrangeRoute.kt`, `component/EditableToppingLayer.kt`(텍스트 그리기·판정·포커스 장식)
- Test: `CanvasToppingArrangeViewModelTest.kt`

**Interfaces:**
- Consumes: `CanvasToppingKey`·`textToppingColorOf`·`textToppingTypeOf`(Task 4), `TextToppingContent`(Task 5), `UpdateToppingsUseCase(…, textUpdates)`(Task 3), `DeleteTextToppingUseCase`(Task 2), NavKey `initialIsText`(Task 7)
- Produces:
  ```kotlin
  data class EditableText(
      val parfaitTextId: Long, val isMine: Boolean, val text: String,
      val color: TextToppingColor, val type: TextToppingType, val designFontSize: Float,
      val positionX: Float, val positionY: Float, val positionZ: Int,
      val scale: Float = 1f, val rotationDegrees: Float = 0f,
  )
  // CanvasToppingArrangeUiState
  val texts: List<EditableText>
  val focusedKey: CanvasToppingKey?          // focusedToppingId 를 대체
  val dirtyTextIds: Set<Long>; val pendingDeleteTextIds: Set<Long>; val deletedTextIds: Set<Long>
  CanvasToppingArrangeIntent.OnClickText(val text: EditableText)
  ```
  이미지 쪽 세 집합은 그대로 두고 텍스트 쪽을 따로 둔다(id 충돌). `hasUnsavedChanges`는 양쪽을 본다.

- [x] **Step 1: 실패하는 테스트**
  - `initialIsText = true, initialToppingId = 5`로 열면 `focusedKey == Text(5)`; 같은 id 5의 이미지가 있어도 이미지는 포커스 아님.
  - 포커스된 내 텍스트에 `OnToppingTransform` → 텍스트만 이동·dirty, 이미지 5는 그대로.
  - 남의 텍스트 탭 → `ShowOthersToppingNotEditable`.
  - 텍스트 포커스 중 `OnToggleBorderPanel` → 패널 열리지 않음.
  - 삭제 버튼 → `pendingDeleteTextIds`에 들어가고 목록에서 숨는다. 다음 캔버스 방출에도 되살아나지 않는다(병합).
  - 확인 → 이미지·텍스트 변형이 `updateToppings(…, updates, textUpdates)` **한 번**에 실리고, 그 뒤 `deleteTextTopping` 호출, 성공 시 갱신·`NavigateBack`. 일괄 PATCH 실패 시 삭제를 보내지 않고 `ShowError(TOPPING_SAVE_UNKNOWN)`.
- [x] **Step 2: 실패 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingArrangeViewModelTest*"` → FAIL.
- [x] **Step 3: 구현** — `CanvasTextToppingVO`→`EditableText` 매퍼는 `util/EditableToppingMapper.kt`에. 그리기·판정은 이미지·텍스트를 z 순서로 섞는다(Task 4 순서 규칙과 같게). 텍스트 판정은 Task 7과 같은 사각형 방식.
- [x] **Step 4: 통과 확인** — 같은 명령 PASS(기존 테스트의 `focusedToppingId` 단언은 `focusedKey`로 고친다).
- [x] **Step 5: 커밋** — `feat: 토핑 정리 화면에서 텍스트를 옮기고 지운다`

### Task 9: C-305 → C-106 재편집 왕복

**Files:**
- Create: `util/TextEditSessionHolder.kt`, `model/TextEditSession.kt`, `model/TextEditResult.kt`
- Modify: `feature/groups/canvas/api/.../NavKeyCanvasTextInput.kt`, `navigation/EntryBuilder.kt`, `route/CanvasMainRoute.kt`, `app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`(컴파일만)
- Modify: `viewmodel/CanvasTextInputViewModel.kt`, `route/CanvasTextInputRoute.kt`, `viewmodel/CanvasToppingArrangeViewModel.kt`, `route/CanvasToppingArrangeRoute.kt`
- Test: `CanvasTextInputViewModelTest.kt`, `CanvasToppingArrangeViewModelTest.kt`

**Interfaces:**
- Consumes: `EditableText`(Task 8), `PlaceTextToppingUseCase`·`UpdateTextToppingUseCase`·`DeleteTextToppingUseCase`(Task 2)
- Produces:
  ```kotlin
  @Serializable data class NavKeyCanvasTextInput(val isEditingExisting: Boolean = false) : NavKey
  data class TextEditSession(val original: EditableText)
  sealed interface TextEditResult {
      data class Edited(val text: String, val color: TextToppingColor, val type: TextToppingType, val designFontSize: Float,
                        val positionX: Float, val positionY: Float, val scale: Float, val rotationDegrees: Float) : TextEditResult
      data object Deleted : TextEditResult
  }
  @Singleton class TextEditSessionHolder @Inject constructor() {
      fun start(session: TextEditSession); fun session(): TextEditSession?
      val result: StateFlow<Pair<Long, TextEditResult>?>   // parfaitTextId 와 결과
      fun finish(result: TextEditResult); fun consume(); fun clear()
  }
  CanvasToppingArrangeEffect.NavigateToTextEdit
  // CanvasToppingArrangeUiState 에 추가: 내용이 바뀌어 재배치가 필요한 텍스트
  val recreateTextIds: Set<Long>
  ```

- [x] **Step 1: 실패하는 테스트**
  - C-305: 포커스된 내 텍스트를 다시 탭 → 홀더 `start(original)` + `NavigateToTextEdit`. 포커스 안 된 텍스트 첫 탭은 포커스만.
  - C-305: 홀더에 `Edited(text="new", …)` 결과 → 로컬 텍스트 갱신, `recreateTextIds`에 id, `consume()` 호출. `text`가 같고 색만 다르면 `recreateTextIds`엔 없고 dirty.
  - C-305: `Deleted` 결과 → `pendingDeleteTextIds`.
  - C-305 확인 순서: 일괄 PATCH → 스타일만 바뀐 텍스트 `updateTextTopping(TextToppingUpdate(color, fontSize, isFill, isStroke))` → 재배치 텍스트 `placeTextTopping(new text, 현재 transform(positionZ 유지), style)` 성공 후 옛 id `deleteTextTopping` → 삭제 대기 DELETE. 재배치 텍스트는 일괄 PATCH의 `textUpdates`에서 뺀다.
  - **POST 성공·옛 DELETE 실패** → `ShowError`, `recreateTextIds`에서 빠지고 옛 id가 `pendingDeleteTextIds`로 이동. 다시 확인 → `placeTextTopping` 추가 호출 없음, `deleteTextTopping(옛 id)`만 다시.
  - C-106 편집 모드(`isEditingExisting = true`): 홀더 세션 값으로 `content`·`selectedColor`·`textType`·`designFontSize`·`scale`·`rotationDegrees`가 채워지고, 캔버스·박스 측정 뒤 오프셋이 중심 비율에서 복원된다. `OnClickStack` → 서버 호출 없이 `finish(Edited(…))` + `NavigateBack`. 빈 내용으로 확인 → `finish(Deleted)`. 헤더 X 확인 → `finish` 없이 나간다. 초안을 비우지 않는다.
- [x] **Step 2: 실패 확인** — `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasTextInputViewModelTest*" --tests "*CanvasToppingArrangeViewModelTest*"` → FAIL.
- [x] **Step 3: 구현** — `NavKeyCanvasTextInput`이 object에서 data class로 바뀌므로 `CanvasMainRoute`는 `NavKeyCanvasTextInput()`, 앱 매핑은 `is` 그대로. 엔트리는 `isEditingExisting`을 라우트로 넘기고, 뷰모델은 `SavedStateHandle`이나 assisted 인자로 받는다(기존 `CanvasToppingArrangeViewModel`의 assisted 패턴). 편집 모드는 C-106 오늘 캔버스 배경 구독에 `groupId`가 필요하다 — 세션에 `groupId`·`parfaitId`를 함께 싣는다(`TextEditSession`에 필드 추가 허용). C-305는 `holder.result`를 `init`에서 구독한다.
- [x] **Step 4: 통과 확인** — 같은 명령 + `./gradlew :app:compileDebugKotlin` PASS.
- [ ] **Step 5: 수동 확인** — 에뮬레이터: C-001 내 텍스트 탭 → C-305 → 재탭 → C-106에서 글자 수정 → 쌓기 → C-305 → 쌓기 → C-001에 새 내용. 못 하면 PR 본문에 미수행으로. [^manual]
- [x] **Step 6: 커밋** — `feat: 쌓인 텍스트를 다시 편집해 저장한다`

### Task 10: 문서 갱신

**Files:**
- Modify: `docs/status.md`(텍스트 토핑 절 — 덮어쓰기), `docs/synthesis/open-questions.md`, `docs/api/parfait-text.md`(「Android 매핑」·frontmatter `android_status`), `docs/api/parfait.md`(Android 매핑의 `texts` 줄), `docs/superpowers/specs/2026-10-16-text-topping.md`(범위의 "미구현" → 구현됨, 정렬은 미저장), `docs/superpowers/specs/2026-10-26-text-topping-api.md`(`status: implemented`는 머지 뒤라 `in-progress`), `docs/log.md`(한 줄)

- [x] **Step 1: open-questions** — OQ-P-450 해소. OQ-P-439 ①·③ 해소 메모, ②는 미해결 유지. OQ-P-441·443·444에 "앱 측 잠정 규칙"(스펙의 값) 메모. 새 OQ: "텍스트 저장 단위(335 기준 `fontSize`, 중심 비율 좌표)를 iOS와 합의하지 않았다" — `<!-- oq-next -->` 번호를 쓰고 증가시킨다.
- [x] **Step 2: 나머지 문서** — 코드로 복원 가능한 사실은 적지 않는다(`CLAUDE.md` 「무엇을 적나」). `status.md`는 현재 상태만.
- [x] **Step 3: 확인** — `python3 docs/script/check_links.py docs` exit 0.
- [x] **Step 4: 커밋** — `docs: 텍스트 토핑 서버 연동 상태를 반영한다`

[^manual]: 에뮬레이터·실기기 확인은 수행하지 않았다 — [status.md 「텍스트 토핑」](../../../status.md#텍스트-토핑-c-106-텍스트-입력--c-001-렌더--c-305-재편집)의 미확인 항목으로 이월한다.
