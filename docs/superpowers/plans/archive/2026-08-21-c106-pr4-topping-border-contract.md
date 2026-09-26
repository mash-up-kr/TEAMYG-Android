---
id: c106-pr4-topping-border-contract
title: C-106 결선 PR4 — 테두리 계약 전환 (굽기 중단·초안 채우기·공유 스탬프 렌더러)
status: done
type: work-order
created: 2026-08-21
updated: 2026-08-21
archived_reason: 구현 완료·develop 머지(2026-08-22, PR #334 19cb5299). 브랜치 feature/#270-topping-border-contract 에 커밋 9개, 신규 테스트 16건.
platforms: android
owner: Parfait 팀
related_adr: ADR-0025, ADR-0026
related_spec: c106-topping-place-api, c103-segmentation-topping-edit, segmentation-pipeline-hardening
related_code:
  - SegmentationViewModel.kt#init
  - ToppingEditViewModel.kt#completeEdit
  - ToppingEditMask.kt#withBorders
  - ToppingEditMask.kt#trimTransparentBounds
  - NavKeyToppingEdit.kt#ToppingEditResult
  - SegmentationConfirmRoute.kt#SegmentationConfirmRoute
  - SegmentationConfirmScreen.kt#SegmentationConfirmScreen
  - CanvasToppingLayer.kt#ToppingImage
  - CanvasToppingLayer.kt#ToppingOutline
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceUiState
  - CanvasToppingPlaceScreen.kt#CanvasToppingPlaceScreen
  - NavKeyCanvasToppingPlace.kt#NavKeyCanvasToppingPlace
  - YGCanvas.kt#CanvasArea
  - CanvasConst.kt#CANVAS_ASPECT_RATIO
  - ToppingDraftRepository.kt#record
  - CanvasBGEditViewModel.kt#handleOnToppingEditResult
tags: [plan, parfait, topping, border, canvas, segmentation]
---

# C-106 결선 PR4 — 테두리 계약 전환 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 토핑 테두리를 이미지 픽셀에 굽는 것을 그만두고, 테두리 없는 알맹이와 색·굵기 값을 초안에 담아 **누끼 확인 화면과 배치 화면이 캔버스와 같은 8방향 스탬프로** 그리게 한다. 함께 `NavKeyCanvasToppingPlace`의 인자를 걷고 캔버스 종횡비 상수를 하나로 모은다.

**Architecture:** 세 화면(누끼 확인·배치·캔버스)이 같은 그림을 그리도록 `CanvasToppingLayer` 안에 있던 8방향 스탬프 컴포저블을 `:core:designsystem`으로 올린다([ADR-0025](../../../adr/0025-topping-border-as-server-field.md)). 초안에 쓰는 시점은 **사건 둘**이다 — 세그멘테이션 성공과 편집 완료. 화면이 열릴 때가 아니라 사건이 났을 때 적으므로, 프로세스가 죽었다 살아나도 이미 적힌 편집 결과가 진입 인자로 덮이지 않는다([ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md)이 DataStore를 고른 이유가 그 복원이다). 편집 화면은 종전대로 `TOPPING_EDIT_RESULT_KEY`로 결과를 돌려주고, 배치 화면은 초안만 읽는다. **이 PR은 서버를 부르지 않는다** — 업로드·배치 호출과 좌표 변환은 PR5다. 사용자에게 보이는 변화는 **테두리 렌더 방식**이고, 그래서 이 라운드에 시각 회귀 위험이 몰린다.

**Tech Stack:** Kotlin · Hilt · Jetpack Compose · Coil3 · DataStore Preferences · kotlinx-coroutines-test · MockK · Turbine · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-20-c106-topping-place-api.md`](../../specs/archive/2026-08-20-c106-topping-place-api.md) — PR 분할 표 **4번 행**, 「토핑 초안 SSOT」·「좌표 변환」의 종횡비 절

> **베이스는 PR3 브랜치다.** `feature/#270-topping-draft-ssot`(팁 `90eb22a1`, **PR 올라가 있고 미머지**).
> PR1·PR2는 develop에 머지됐다(`da03c9b0`).
> 🔁 이 문서의 스택 해시는 **2026-08-22 리베이스 뒤 기준**이다 — 스택 넷을 develop `ef55a58c` 위로
> 다시 쌓았고 기록은 [PR3 계획서](2026-08-20-c106-pr3-topping-draft-ssot.md)에 있다.

> ✅ **2026-08-22 develop 머지**(PR #334 `19cb5299`) — 스택 넷이 머지 커밋 하나로 함께 들어왔다.
> 머지 트리가 PR6 브랜치 팁 `656cbf2e`와 같아 충돌 해소 편집이 0건이고, 위 as-built 수치는
> 재측정 없이 그대로 develop 사실이다.

## 사용자에게 보이는 변화 (예고)

1. 누끼 확인 화면에 **테두리가 보이기 시작한다.** 지금은 구운 PNG를 띄우므로 결과적으로 보였지만,
   이제는 화면이 dp 굵기로 그린다. **편집 화면에서 본 굵기보다 가늘어 보인다** — 편집은 원본 픽셀
   좌표계로, 이후 화면은 화면 dp로 그리기 때문이다(OQ-P-245).
2. 배치 화면·캔버스의 테두리는 굵기 거동이 바뀐다 — **토핑을 키워도 테두리는 굵어지지 않는다.**
3. ⚠️ **C-301 배경 편집의 테두리 재편집(`borderOnly`)이 이 라운드 동안 무반응이 된다.** 그 화면은
   `borderLayers`를 읽어 그리지 않고 넘겨받은 이미지 파일만 띄우는데, 그 파일에서 테두리가 사라진다.
   색을 골라도 화면이 변하지 않는다. **의도적으로 C-301 라운드로 미룬다**(아래 결정 셋 2번).

## 스펙과 갈린 결정 셋 (실행 전에 읽는다)

1. **종횡비 상수를 domain이 아니라 디자인시스템으로 모은다.** 스펙은 "`YGCanvas`가 `domain`의 상수를
   쓰도록 통일한다"고 적었지만, [module-structure](../../../architecture/module-structure.md)는 그
   `domain` 상수(`CANVAS_ASPECT_RATIO`) 자체를 **"표시 규격이 domain에 들어온 사례"**로 미결에 올려
   두었다(2026-08-15). 스펙대로 하면 `:core:designsystem → :domain` 간선이 새로 생기면서 그 미결이
   굳는다. 반대로 하면 새 간선이 0개다 — 그 상수를 쓰는 곳은 `feature/groups/canvas/impl`의 화면
   둘뿐이고 그 모듈은 이미 디자인시스템을 본다. **Task 3이 그 방향이다.**
2. **C-301 배경 편집 화면은 테두리를 그리지 않은 채 둔다.** 그 화면은 원래 테두리를 스스로 그린 적이
   없고(구운 파일을 그대로 띄웠다) 서버 결선 전 목업이라, 이 라운드는 필드 이름만 맞추고 표시는
   **C-301 라운드로 미룬다.** 대가는 위 「보이는 변화」 3번이고, 실기기 확인 항목과 open-questions에
   그 사실을 남긴다(Task 8).
3. **확인 버튼 비활성 *표현*은 PR5로 미루되, 이 라운드가 사용자를 막다른 곳에 두지 않는다.**
   `YGFloatingBarEdit`의 확인 버튼(`YGCircleButton`)에 비활성 상태가 없고 디자인 근거도 없다. 그래서
   **초안이 비었을 때 확인을 누르면 알리고 캔버스로 되감는다**(Task 7). 조용히 아무 일도 안 하는
   방어는 두지 않는다 — 종전에는 최소한 되감기라도 됐다.

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**이고 이 문서가 사는 저장소가 아니다. 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다(Task 8만 이 문서 저장소에서 한다).
- **베이스 브랜치는 `feature/#270-topping-draft-ssot`**(PR3, 팁 `90eb22a1`, 미머지)다. 그 위에 새 브랜치 `feature/#270-topping-border-contract`를 만들어 작업한다.
- **커밋은 태스크마다 한다.** `git push`·`gh pr create`·`gh pr merge`는 **하지 않는다** — 사용자 확인이 필요한 작업이다.
- ⚠️ **새 ViewModel 테스트는 `runTest(mainDispatcherRule.dispatcher)`로 연다.** `MainDispatcherRule`의 KDoc이 이유를 못 박아 두었다 — 인자 없이 부르면 스케줄러가 갈려 `advanceUntilIdle()`이 Main 큐를 못 비울 수 있다.
- ⚠️ **ktlint가 미사용 import를 실패로 잡는다**(`ktlint_standard_no-unused-imports`). 그래서 이 계획은 태스크마다 **추가할 import와 지울 import를 명시한다.** 그 목록을 건너뛰면 `ktlintCheck`에서 멈춘다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 선언에 멀티라인을 강제한다**(`.editorconfig`). 코드 블록을 한 줄로 줄이지 말 것. 최대 줄 길이는 120이다.
- **주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 근거는 문서를 가리킨다. 함정과 의도는 쓴다.
  - 아키텍처 결정 설명을 코드에 복사하지 않는다. 포인터 한 줄만 둔다.
- **초안이 담는 이미지 경로는 파일 시스템 절대경로**다. `file://` uri가 아니다. uri가 필요한 화면이 그 자리에서 바꾼다.
- ⚠️ **`TOPPING_EDIT_RESULT_KEY`를 걷지 않는다.** 소비자가 `SegmentationConfirmRoute`와 `CanvasBGEditRoute` 둘이라 걷으면 배경 편집이 컴파일은 통과한 채 조용히 죽는다([ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md)).
- ⚠️ **`NavKeySegmentationConfirm`의 경로 셋도 그대로 둔다.** 그것은 화면을 여는 인자이고 초안은 흐름의 결과물이다. 값이 겹치는 구간에서는 **초안이 정본**이다.
- ⚠️ **초안 쓰기는 화면 진입이 아니라 사건에 건다.** 화면이 열릴 때마다 적으면 프로세스 사망 복원에서 **이미 적힌 편집 결과를 진입 인자로 덮어쓴다** — ADR-0026이 영속을 고른 이유를 스스로 깨는 경로다.
- **실패 알림은 시스템 `Toast`가 아니라 `YGScaffoldV2`의 `toastPolicy`로 띄운다.** 선례는 `AppSettingRoute`·`TermAgreeRoute`다. (같은 모듈의 `ToppingEditRoute`가 시스템 토스트를 쓰지만 그것은 이 라운드가 손대는 자리가 아니다.)
- **서버를 부르지 않는다.** `AddToppingUseCase`·`ImageUploadRepository`·좌표 변환·로딩 오버레이·되감기 정책은 전부 PR5다.
- 매퍼 단독 테스트(`XxxVOMapperTest`)는 만들지 않는다.
- 검증 명령(태스크마다 해당하는 것을 전부 통과해야 한다):
  ```bash
  ./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest \
    :feature:segmentation:impl:testDebugUnitTest ktlintCheck
  ```
  마지막 태스크에서만 `./gradlew :app:assembleDebug`까지 돌린다.
- ⚠️ **`:core:designsystem`에는 호스트 단위 테스트 소스셋이 없다.** Task 2·3의 디자인시스템 변경은 Preview와 상위 태스크의 상태 테스트로 잠근다. **그 둘만 예외이고** 나머지는 테스트를 먼저 쓴다.
- ⚠️ **비트맵 파이프라인은 JVM 단위 테스트로 못 잡는다** — 저장소에 Robolectric이 없고 `android.graphics.Canvas`가 실제로 돈다. Task 4의 감지선은 컴파일과 실기기 확인이다.

## 파일 구성

| 자리 | 역할 | 태스크 |
|---|---|---|
| `domain/repository/topping/ToppingDraftRepository.kt` | 흐름 결과를 초안에 적는 `record` 추가 | 1 |
| `data/repository/topping/ToppingDraftRepositoryImpl.kt` | 캔버스 식별값은 두고 이미지·테두리만 덮어쓰는 병합 | 1 |
| `core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt` | **신설.** 누끼 알맹이 + 8방향 테두리 스탬프. 세 화면 공유 | 2 |
| `feature/groups/canvas/impl/component/CanvasToppingLayer.kt` | 자기 스탬프를 걷고 공유 컴포저블을 쓴다 | 2 |
| `core/designsystem/component/ygcanvas/YGCanvas.kt` | private 종횡비 상수를 public으로 올린다 | 3 |
| `domain/model/CanvasConst.kt` | **삭제.** 표시 규격이라 디자인시스템이 소유한다 | 3 |
| `feature/segmentation/api/NavKeyToppingEdit.kt` | `ToppingEditResult`가 구운 이미지 대신 알맹이를 나른다 | 4 |
| `feature/segmentation/impl/viewmodel/ToppingEditViewModel.kt` | 굽기 중단. `originPxPerDp` 배선 제거 | 4 |
| `feature/segmentation/impl/screen/ToppingEditScreen.kt` | 굽기 전용이던 `originPxPerDp` 계산·콜백 제거 | 4 |
| `feature/segmentation/impl/editor/ToppingEditMask.kt` | 소비자가 사라진 `withBorders` 삭제 | 4 |
| `feature/segmentation/impl/viewmodel/SegmentationViewModel.kt` | 세그멘테이션 성공 시 초안에 알맹이·마스크를 적는다 | 5 |
| `feature/segmentation/impl/viewmodel/SegmentationConfirmViewModel.kt` | **신설.** 초안을 읽고, 편집 결과를 초안에 옮겨 적는다 | 6 |
| `feature/segmentation/impl/route/SegmentationConfirmRoute.kt` | `rememberSaveable` 셋 제거, 토스트 자리 결선 | 6 |
| `feature/segmentation/impl/screen/SegmentationConfirmScreen.kt` | 공유 컴포저블로 테두리까지 그린다 | 6 |
| `feature/groups/canvas/api/NavKeyCanvasToppingPlace.kt` | 인자 없는 `data object`로 바뀐다 | 7 |
| `feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt` | 초안을 구독해 이미지·테두리를 상태로 싣는다 | 7 |
| `feature/groups/canvas/impl/screen/CanvasToppingPlaceScreen.kt` | 공유 컴포저블로 그리고, 그림이 뜬 뒤에만 실측을 올린다 | 7 |

---

## Task 1: 초안에 흐름 결과를 적는 길을 낸다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt`

**Interfaces:**
- Consumes: PR3가 만든 `ToppingDraftLocalDataSource.draft`/`save`
- Produces: `suspend fun ToppingDraftRepository.record(subjectImagePath: String, cutoutImagePath: String, borderColorArgb: Int?, borderWidthDp: Float?): Boolean` — Task 5·6이 부른다. 흐름이 열려 있지 않으면 `false`.

- [ ] **Step 1: 실패하는 테스트 셋을 쓴다**

`ToppingDraftRepositoryImplTest`에 아래 셋을 더한다. 기존 헬퍼(`stubEmptyStore`·`givenStoredDraft`·
`draft`)와 companion을 그대로 쓴다. **추가할 import**: `kotlin.test.assertFalse`, `kotlin.test.assertTrue`.

```kotlin
    @Test
    fun record_keepsCanvasIdentity_andFillsImages() = runTest {
        // Given 흐름 진입만 마친 초안(이미지·테두리가 비어 있다)
        givenStoredDraft(draft(subjectImagePath = null, cutoutImagePath = null))
        val saved = slot<ToppingDraft>()
        coEvery { toppingDraftLocalDataSource.save(capture(saved)) } returns Unit

        // When 세그멘테이션 결과를 적는다
        val recorded = repository().record(
            subjectImagePath = "/cache/segmentation/subject.png",
            cutoutImagePath = "/cache/segmentation/cutout.png",
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 진입 때 못 박은 캔버스 식별값은 건드리지 않는다 — 그것이 이 배치의 전제다
        assertTrue(recorded)
        assertEquals(
            ToppingDraft(
                groupId = GROUP_ID,
                parfaitId = PARFAIT_ID,
                nextPositionZ = 4,
                subjectImagePath = "/cache/segmentation/subject.png",
                cutoutImagePath = "/cache/segmentation/cutout.png",
            ),
            saved.captured,
        )
    }

    @Test
    fun record_withoutBorder_dropsThePreviousOne() = runTest {
        // Given 테두리까지 적혀 있던 초안
        givenStoredDraft(
            draft(
                subjectImagePath = "/cache/segmentation/old.png",
                cutoutImagePath = "/cache/segmentation/old-cutout.png",
            ).copy(borderColorArgb = 0xFFFF0000.toInt(), borderWidthDp = 10f),
        )
        val saved = slot<ToppingDraft>()
        coEvery { toppingDraftLocalDataSource.save(capture(saved)) } returns Unit

        // When 테두리를 벗긴 편집 결과를 적는다
        repository().record(
            subjectImagePath = "/cache/segmentation/new.png",
            cutoutImagePath = "/cache/segmentation/new-cutout.png",
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 지난 테두리가 살아남지 않는다 — 병합하면 방금 벗긴 테두리가 배치까지 따라간다
        assertNull(saved.captured.borderColorArgb)
        assertNull(saved.captured.borderWidthDp)
    }

    @Test
    fun record_withNoOpenFlow_writesNothing() = runTest {
        // Given 흐름 밖이다(진입이 초안을 쓰지 못했거나 이미 비워졌다)
        givenStoredDraft(null)

        // When 결과를 적으려 한다
        val recorded = repository().record(
            subjectImagePath = "/cache/segmentation/subject.png",
            cutoutImagePath = "/cache/segmentation/cutout.png",
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 캔버스 식별값 없는 초안을 지어내지 않는다 — 그걸 만들면 배치까지 가서야 올릴 데가
        // 없다는 것을 알게 된다
        assertFalse(recorded)
        coVerify(exactly = 0) { toppingDraftLocalDataSource.save(any()) }
    }
```

- [ ] **Step 2: 테스트가 깨지는 것을 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ToppingDraftRepositoryImplTest*"`
Expected: 컴파일 실패 — `Unresolved reference: record`

- [ ] **Step 3: 계약을 더한다**

`ToppingDraftRepository`에 더한다.

```kotlin
    /**
     * 흐름이 만들어 낸 알맹이·테두리를 초안에 적는다. 캔버스 식별값은 진입 때 못 박은 것을 그대로 둔다.
     *
     * 테두리는 넘어온 값으로 매번 덮어쓴다 — 알맹이가 바뀌면 그 전 테두리는 설 자리가 없다.
     *
     * @return 흐름이 열려 있지 않으면 `false`. 없는 초안을 지어내지 않는다
     */
    suspend fun record(
        subjectImagePath: String,
        cutoutImagePath: String,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
    ): Boolean
```

- [ ] **Step 4: 구현한다**

`ToppingDraftRepositoryImpl`에 더한다. **추가할 import**: `kotlinx.coroutines.flow.first`.

```kotlin
    // 정규화된 [draft] 가 아니라 원문을 읽는다 — 여기서 필요한 것은 캔버스 식별값뿐이고,
    // 정규화는 파일 존재 확인 IO 를 태운다
    override suspend fun record(
        subjectImagePath: String,
        cutoutImagePath: String,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
    ): Boolean {
        val current = toppingDraftLocalDataSource.draft.first() ?: return false

        toppingDraftLocalDataSource.save(
            current.copy(
                subjectImagePath = subjectImagePath,
                cutoutImagePath = cutoutImagePath,
                borderColorArgb = borderColorArgb,
                borderWidthDp = borderWidthDp,
            ),
        )
        return true
    }
```

- [ ] **Step 5: 테스트를 통과시킨다**

Run: `./gradlew :domain:test :data:testDebugUnitTest ktlintCheck`
Expected: PASS

- [ ] **Step 6: 커밋한다**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt \
  data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt \
  data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt
git commit -m "feat(topping): 흐름 결과를 토핑 초안에 적는 경로를 연다"
```

---

## Task 2: 8방향 스탬프를 디자인시스템으로 올린다

**Files:**
- Create: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt`

**Interfaces:**
- Produces: `@Composable fun YGToppingCutoutImage(painter: Painter, borderColor: Color?, borderWidth: Dp, modifier: Modifier = Modifier)` — Task 6·7이 부른다. `borderColor`가 `null`이면 테두리를 안 그린다.

> ⚠️ **이름 충돌 주의.** 디자인시스템에 이미 `YGToppingImage`가 있다(`component/ygtoppinggroup/`, G-001 파르페 토핑). 다른 물건이므로 `ygtoppingcutout` 패키지에 `YGToppingCutoutImage`로 둔다.

- [ ] **Step 1: 공유 컴포저블을 만든다**

`:core:designsystem`에는 호스트 테스트 소스셋이 없어 이 태스크만 테스트를 먼저 쓰지 않는다.

```kotlin
package com.teamyg.parfait.core.designsystem.component.ygtoppingcutout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import kotlin.math.cos
import kotlin.math.sin

/** 누끼 외곽선을 찍는 방향 수. 8 방향이면 대각까지 메워져 이음매가 보이지 않는다 */
private const val OUTLINE_STAMP_COUNT = 8

private const val FULL_TURN_DEGREES = 360.0

/**
 * 누끼 이미지와 그 실루엣을 따르는 테두리를 함께 그린다. 사각 테두리를 두르면 잘라 낸 배경이 다시
 * 드러나므로, 같은 그림을 테두리 색으로 물들여 여덟 방향으로 밀어 찍고 그 위에 원본을 얹는다.
 *
 * 테두리를 그리는 화면이 셋이라 여기서 한 벌만 둔다(`adr/0025-topping-border-as-server-field.md`).
 *
 * ⚠️ **그림이 아직 뜨지 않은 [painter] 로 찍으면 플레이스홀더 실루엣이 테두리로 보인다.**
 * 준비되기 전에는 호출부가 [borderColor] 에 `null` 을 넘긴다.
 *
 * @param borderWidth 화면 기준 dp 다 — 토핑을 키워도 굵기는 그대로다
 */
@Composable
fun YGToppingCutoutImage(
    painter: Painter,
    borderColor: Color?,
    borderWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (borderColor != null) {
            ToppingOutline(
                painter = painter,
                color = borderColor,
                width = borderWidth,
            )
        }

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ToppingOutline(
    painter: Painter,
    color: Color,
    width: Dp,
) {
    val widthPx = with(LocalDensity.current) { width.toPx() }

    repeat(OUTLINE_STAMP_COUNT) { index ->
        val radians = Math.toRadians(FULL_TURN_DEGREES / OUTLINE_STAMP_COUNT * index)

        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(color),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = (cos(radians) * widthPx).toFloat()
                    translationY = (sin(radians) * widthPx).toFloat()
                },
        )
    }
}

@YGPreview
@Composable
private fun YGToppingCutoutImagePreview() = PreviewBox {
    YGToppingCutoutImage(
        painter = painterResource(R.drawable.ic_plus),
        borderColor = YGAtomicColors.Cherry.Cherry200,
        borderWidth = 6.dp,
        modifier = Modifier.size(120.dp),
    )
}
```

- [ ] **Step 2: 캔버스 레이어가 그 컴포저블을 쓰게 한다**

`CanvasToppingLayer.kt`에서 private `ToppingOutline`과 상수 둘(`OUTLINE_STAMP_COUNT`·
`FULL_TURN_DEGREES`)을 지우고 `ToppingImage`를 아래로 바꾼다.

**지울 import**: `androidx.compose.foundation.Image`, `androidx.compose.ui.graphics.ColorFilter`,
`androidx.compose.ui.platform.LocalDensity`, `kotlin.math.cos`, `kotlin.math.sin`.
**남길 import**: `androidx.compose.ui.graphics.graphicsLayer` — `CanvasTopping`의 회전이 계속 쓴다.
**추가할 import**: `com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage`.

```kotlin
@Composable
private fun ToppingImage(
    imageUrl: String,
    border: ToppingBorder,
) {
    val painter = rememberAsyncImagePainter(
        model = imageUrl,
        contentScale = ContentScale.Fit,
    )
    val painterState by painter.state.collectAsState()
    val solidBorder = border as? ToppingBorder.Solid

    YGToppingCutoutImage(
        painter = painter,
        // 색을 못 읽으면 테두리를 걸러 낸다 — 임의의 색을 골라 칠하는 것보다 안 그리는 편이 덜 틀리다.
        // 로딩·실패 상태에서 찍으면 플레이스홀더 실루엣이 테두리로 보인다
        borderColor = solidBorder?.color?.toColorOrNull()
            ?.takeIf { painterState is AsyncImagePainter.State.Success },
        borderWidth = (solidBorder?.width?.toFloat() ?: 0f).dp,
        modifier = Modifier.fillMaxSize(),
    )
}
```

- [ ] **Step 3: 회귀가 없는지 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck`
Expected: PASS(기존 테스트 그대로 통과, 신규 테스트 없음)

Android Studio에서 `YGToppingCutoutImagePreview`를 렌더해 테두리가 실루엣을 따라 그려지는지 눈으로 본다.
이 태스크의 유일한 감지선이다.

- [ ] **Step 4: 커밋한다**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtoppingcutout/YGToppingCutoutImage.kt \
  feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt
git commit -m "refactor(designsystem): 토핑 테두리 스탬프를 세 화면이 나눠 쓰게 올린다"
```

---

## Task 3: 캔버스 종횡비 상수를 하나로 모은다

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcanvas/YGCanvas.kt:44`
- Delete: `domain/src/main/java/com/teamyg/parfait/domain/model/CanvasConst.kt`
- Modify: `feature/groups/canvas/impl/.../screen/CanvasToppingPlaceScreen.kt:39,97,145,159`
- Modify: `feature/groups/canvas/impl/.../screen/CanvasBGEditScreen.kt:55,112`

**Interfaces:**
- Produces: `const val com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO`

> 값이 하나만 남으므로 "둘이 어긋나면 깨지는 단언"은 필요 없어진다 — 컴파일이 그 자리를 대신한다.
> 스펙 테스트 표의 「종횡비 상수」 행도 Task 8에서 그렇게 고친다.

- [ ] **Step 1: 디자인시스템 상수를 공개한다**

`YGCanvas.kt:44`를 바꾼다.

```kotlin
/**
 * Canvas-Area 종횡비. 캔버스를 그리는 화면과 그 위 좌표를 계산하는 화면이 **같은 값**을 써야
 * 저장된 배치가 맞는 자리에 얹힌다 — 값이 갈리면 모든 토핑의 세로 위치가 조금씩 밀리고
 * 컴파일은 깨지지 않는다.
 */
const val CANVAS_AREA_ASPECT_RATIO = 9f / 16f
```

- [ ] **Step 2: 화면 둘의 참조를 옮긴다**

두 화면에서 **지울 import**: `com.teamyg.parfait.domain.model.CANVAS_ASPECT_RATIO`.
**추가할 import**: `com.teamyg.parfait.core.designsystem.component.ygcanvas.CANVAS_AREA_ASPECT_RATIO`.
`aspectRatio(CANVAS_ASPECT_RATIO)` 네 자리를 `aspectRatio(CANVAS_AREA_ASPECT_RATIO)`로 바꾼다.

- [ ] **Step 3: domain 상수를 지운다**

```bash
git rm domain/src/main/java/com/teamyg/parfait/domain/model/CanvasConst.kt
```

- [ ] **Step 4: 남은 참조가 없는지 확인한다**

Run: `grep -rn "CANVAS_ASPECT_RATIO" --include "*.kt" . | grep -v "/build/"`
Expected: 0건

Run: `./gradlew :domain:test :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck`
Expected: PASS

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "refactor(canvas): 캔버스 종횡비 상수를 디자인시스템 하나로 모은다"
```

---

## Task 4: 편집 화면이 테두리를 굽지 않는다

**Files:**
- Modify: `feature/segmentation/api/src/main/java/.../NavKeyToppingEdit.kt:24-37`
- Modify: `feature/segmentation/impl/src/main/java/.../viewmodel/ToppingEditViewModel.kt`(82·142-143·226-228·266-319줄)
- Modify: `feature/segmentation/impl/src/main/java/.../screen/ToppingEditScreen.kt`(102·120·145·194-201·669줄)
- Modify: `feature/segmentation/impl/src/main/java/.../route/ToppingEditRoute.kt`(76-78줄)
- Modify: `feature/segmentation/impl/src/main/java/.../editor/ToppingEditMask.kt:54-79`
- Modify: `feature/segmentation/impl/src/main/java/.../route/SegmentationConfirmRoute.kt:53`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/.../viewmodel/CanvasBGEditViewModel.kt:40-41,342-351`

**Interfaces:**
- Produces: `ToppingEditResult(subjectImagePath: String, cutoutImagePath: String, borderLayers: List<ToppingBorderLayer>)` — Task 6이 초안에 옮겨 적는다. `subjectImagePath`는 **테두리 없는 알맹이의 트리밍본**이다.

> ⚠️ **이 커밋 하나만으로는 세 화면(누끼 확인·배치·C-301)에서 테두리가 전부 사라진다.** Task 6·7과
> 한 덩어리이므로 이 지점만 보고 회귀로 판단하지 말 것. 되돌리려면 라운드를 통째로 되돌린다.
>
> ⚠️ **자동 테스트가 없다.** `completeEdit`이 `android.graphics.Canvas`를 실제로 돌리고 저장소에
> Robolectric이 없다. 그래서 **결과 타입을 먼저 바꿔 소비처를 컴파일 에러로 드러내는** 순서를 지킨다.

- [ ] **Step 1: 결과 타입의 계약을 바꾼다**

```kotlin
/**
 * 편집 결과.
 *
 * 테두리는 픽셀에 굽지 않고 값으로 나른다(`adr/0025-topping-border-as-server-field.md`).
 * 그리는 것은 결과를 받는 쪽이다.
 *
 * @param subjectImagePath 테두리를 두르지 않은 알맹이. 투명 여백을 걷어 실제 토핑 크기다
 * @param cutoutImagePath 다시 편집할 때의 시작 마스크. 원본 좌표계를 지켜야 해 여백을 걷지 않는다
 */
data class ToppingEditResult(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val borderLayers: List<ToppingBorderLayer>,
)
```

- [ ] **Step 2: 컴파일을 돌려 소비처를 드러낸다**

Run: `./gradlew :feature:segmentation:impl:compileDebugKotlin`
Expected: FAIL — `ToppingEditViewModel`과 `SegmentationConfirmRoute`가 없는 이름을 부른다.

- [ ] **Step 3: 굽기를 걷어낸다**

`ToppingEditViewModel.completeEdit`의 본문을 아래로 바꾼다.
**지울 import**: `com.teamyg.parfait.feature.segmentation.impl.editor.withBorders`.

```kotlin
        viewModelScope.launch {
            updateState { copy(isSaving = true) }

            val cutout = withContext(Dispatchers.Default) {
                buildCutoutBitmap(
                    originBitmap = originBitmap,
                    segmentationBitmap = segmentationBitmap,
                    strokes = current.strokes,
                )
            }
            // cutout 은 재편집 좌표계를 지키려고 원본 크기를 유지해야 하고, 보여 주고 올릴 알맹이는
            // 투명 여백 없이 실제 토핑 크기여야 한다. 여백이 붙은 채로 올라가면 배치 좌표가 어긋난다
            val trimmedCutout = withContext(Dispatchers.Default) { cutout.trimTransparentBounds() }

            // 화면 사이에서는 비트맵 대신 경로를 주고받으므로 여기서 파일로 떨군다.
            // 저장 전용으로 만든 비트맵이라 화면이 잡고 있지 않고, 원본 해상도라 수십 MB 에
            // 이르기도 해서 파일로 떨구는 즉시 메모리를 돌려준다
            val (cutoutPath, subjectPath) = try {
                val savedCutoutPath = saveEditedImageUseCase(cutout.toAndroidBitmap()).getOrNull()
                val savedSubjectPath = saveEditedImageUseCase(trimmedCutout.toAndroidBitmap()).getOrNull()
                savedCutoutPath to savedSubjectPath
            } finally {
                if (trimmedCutout !== cutout) trimmedCutout.recycle()
                cutout.recycle()
            }
            updateState { copy(isSaving = false) }

            if (cutoutPath == null || subjectPath == null) {
                postSideEffect(ToppingEditEffect.SaveFailed)
                return@launch
            }

            postSideEffect(
                ToppingEditEffect.EditCompleted(
                    ToppingEditResult(
                        subjectImagePath = subjectPath,
                        cutoutImagePath = cutoutPath,
                        borderLayers = current.borderLayers,
                    ),
                ),
            )
        }
```

- [ ] **Step 4: 굽기 전용이던 `originPxPerDp` 배선을 함께 걷는다**

`originPxPerDp`는 **dp 굵기를 원본 픽셀 좌표계로 환산해 굽기 위해서만** 있었고 소비자가
`withBorders` 하나였다. 굽기가 사라지면 값이 아무 데도 안 쓰이는데 컴파일러는 그것을 잡지 않는다
(`allWarningsAsErrors`가 없다). 서버로 보내는 굵기는 dp 그대로라 PR5도 이 값을 쓰지 않는다.

지울 자리 다섯:

- `ToppingEditViewModel.kt:82` — `ToppingEditState.originPxPerDp` 프로퍼티와 그 KDoc
- 같은 파일 `:142-143` — `ChangeOriginPxPerDp` 인텐트
- 같은 파일 `:226-228` — `processIntent`의 그 arm
- `ToppingEditScreen.kt:102,120,145` — `onChangeOriginPxPerDp` 파라미터와 전달, `:194-201`의
  `scale`·`originPxPerDp` 계산과 `LaunchedEffect`, `:669`의 프리뷰 인자
- `ToppingEditRoute.kt:76-78` — `onChangeOriginPxPerDp` 람다

`ToppingEditScreen.kt`에서 `BitmapViewMapping`·`IntSize`·`density` 등이 그 블록에서만 쓰였다면
해당 import도 함께 지운다(`ktlintCheck`가 잡는다).

- [ ] **Step 5: 소비자가 사라진 굽기 함수를 지운다**

`ToppingEditMask.kt`에서 `Bitmap.withBorders`(54~79줄)를 지운다. **`toOutlineDistanceField`·
`buildBorderBitmap`은 남긴다** — 편집 화면의 실시간 미리보기(`ToppingBorderEditScreen`)가 계속 쓴다.
`ToppingBorderLayer` import가 그 함수에서만 쓰였다면 함께 지운다.

Run: `grep -rn "withBorders\|originPxPerDp" --include "*.kt" . | grep -v "/build/"`
Expected: 0건

- [ ] **Step 6: 남은 소비처 둘의 이름을 맞춘다**

`SegmentationConfirmRoute.kt:53`을 `subjectImagePath = result.subjectImagePath`로 바꾼다
(이 Route는 Task 6에서 통째로 다시 쓴다 — 여기서는 컴파일만 통과시킨다).

`CanvasBGEditViewModel.handleOnToppingEditResult`:

```kotlin
    private fun handleOnToppingEditResult(intent: CanvasBGEditIntent.OnToppingEditResult) {
        applyToppingTransform(intent.toppingId) { topping ->
            topping.copy(
                // 다시 편집을 열 때 이 사진에서 시작해야 방금 지운/되살린 영역이 유지된다
                segmentationImageUri = File(intent.result.cutoutImagePath).toUri().toString(),
                borderLayers = intent.result.borderLayers,
                editedImagePath = intent.result.subjectImagePath,
            )
        }
    }
```

`CanvasBGEditViewModel.kt:40-41`의 `CanvasToppingItem.editedImagePath` KDoc도 실제와 맞춘다.

```kotlin
    /**
     * 편집을 거쳐 새로 만든 알맹이 경로. 있으면 [imageResId] 대신 이걸 그린다.
     *
     * ⚠️ 이 파일에 테두리는 없다. 테두리는 [borderLayers] 에 값으로 있고 이 화면은 아직 그리지
     * 않는다 — 그래서 테두리 재편집이 이 화면에서 무반응이다(`synthesis/open-questions.md`).
     */
    val editedImagePath: String? = null,
```

- [ ] **Step 7: 컴파일과 기존 테스트를 통과시킨다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck`
Expected: PASS

- [ ] **Step 8: 커밋한다**

```bash
git add -A
git commit -m "feat(topping): 테두리를 픽셀에 굽지 않고 값으로 넘긴다"
```

---

## Task 5: 세그멘테이션 성공이 초안에 알맹이를 적는다

**Files:**
- Modify: `feature/segmentation/impl/src/main/java/.../viewmodel/SegmentationViewModel.kt`
- Test: `feature/segmentation/impl/src/test/java/.../viewmodel/SegmentationViewModelTest.kt`

**Interfaces:**
- Consumes: `ToppingDraftRepository.record`(Task 1)
- Produces: 없음(초안에 값이 적힌다는 사실만 Task 6·7이 기댄다)

> **왜 확인 화면이 아니라 여기인가:** 스펙 「토핑 초안 SSOT」 표가 "세그멘테이션 완료"를 쓰는 시점으로
> 적었고, 그 편이 **사건에 걸리는 쓰기**다. 화면 진입에 걸면 프로세스 사망 복원 때 진입 인자로
> 편집 결과를 덮어쓴다 — ADR-0026이 DataStore를 고른 이유를 스스로 깨는 경로다.
> 재세그멘테이션(뒤로 가서 다시 시도)도 성공할 때마다 새 경로로 덮이므로 자연히 맞는다.

- [ ] **Step 1: 실패하는 테스트 둘을 쓴다**

`SegmentationViewModelTest`에 더한다. **추가할 import**:
`com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository`, `io.mockk.coVerify`(이미 있다면 생략).
기존 `viewModel()` 헬퍼에 새 인자를 더하고, 클래스에 `private val toppingDraftRepository:
ToppingDraftRepository = mockk(relaxed = true)`를 둔다.

```kotlin
    @Test
    fun init_segmentationSucceeds_recordsTheDraft() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세그멘테이션이 성공한다
        coEvery { segmentImage(bitmapWrapper) } returns Result.success(success)

        // When 화면이 돈다
        viewModel()
        advanceUntilIdle()

        // Then 알맹이와 재편집 마스크가 초안에 적힌다. 이 시점엔 두른 테두리가 없다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(
                subjectImagePath = TRIMMED_SUBJECT_PATH,
                cutoutImagePath = SUBJECT_PATH,
                borderColorArgb = null,
                borderWidthDp = null,
            )
        }
    }

    @Test
    fun init_segmentationFails_recordsNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 세그멘테이션이 실패한다
        coEvery { segmentImage(bitmapWrapper) } returns Result.failure(IllegalStateException())

        // When 화면이 돈다
        viewModel()
        advanceUntilIdle()

        // Then 초안에 아무것도 적지 않는다 — 다음 화면으로 갈 수 없는 결과다
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
    }
```

> 기존 테스트가 쓰는 스텁 이름(`segmentImage`·`bitmapWrapper`·`success`·`SUBJECT_PATH`·
> `TRIMMED_SUBJECT_PATH`)을 그대로 쓴다. 기존 케이스는 손대지 않는다.

- [ ] **Step 2: 테스트가 깨지는 것을 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationViewModelTest*"`
Expected: 컴파일 실패 — 생성자에 `toppingDraftRepository`가 없다.

- [ ] **Step 3: 구현한다**

`SegmentationViewModel` 생성자에 `private val toppingDraftRepository: ToppingDraftRepository`를 더하고,
`onSuccess` 블록의 상태 갱신 뒤에 아래를 넣는다. 이웃(`clearSegmentationCacheUseCase`·
`addRecentImageUseCase`)과 같은 `runSuspendCatching` 관용구를 쓴다 — 초안 쓰기가 실패해도 이 화면은
진행되고, 못 적었다는 사실은 다음 화면이 알린다(Task 6).

```kotlin
                    // 흐름의 결과물은 초안이 나른다(`adr/0026-topping-draft-datastore-ssot.md`).
                    // 미리보기·배치에 쓸 것은 여백을 걷은 판이고, 재편집 마스크는 좌표계를 지킨 판이다
                    runSuspendCatching {
                        toppingDraftRepository.record(
                            subjectImagePath = result.trimmedSubjectImagePath,
                            cutoutImagePath = result.subjectImagePath,
                            borderColorArgb = null,
                            borderWidthDp = null,
                        )
                    }
```

- [ ] **Step 4: 테스트를 통과시킨다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest ktlintCheck`
Expected: PASS(신규 2건 포함)

- [ ] **Step 5: 커밋한다**

```bash
git add -A
git commit -m "feat(segmentation): 세그멘테이션 결과를 토핑 초안에 적는다"
```

---

## Task 6: 누끼 확인 화면이 초안을 읽고 테두리를 그린다

**Files:**
- Create: `feature/segmentation/impl/src/main/java/.../viewmodel/SegmentationConfirmViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/.../route/SegmentationConfirmRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/.../screen/SegmentationConfirmScreen.kt`
- Modify: `feature/segmentation/impl/src/main/res/values/strings.xml`
- Test: `feature/segmentation/impl/src/test/java/.../viewmodel/SegmentationConfirmViewModelTest.kt`

**Interfaces:**
- Consumes: `ToppingDraftRepository.draft`·`record`(Task 1) · `ToppingEditResult`(Task 4) · `YGToppingCutoutImage`(Task 2)
- Produces: `SegmentationConfirmViewModel` — 화면 밖으로 나가는 계약은 없다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

새 파일 `SegmentationConfirmViewModelTest.kt`.

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.core.testing.MainDispatcherRule
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SUBJECT_PATH = "/cache/segmentation/subject_trimmed.png"
private const val CUTOUT_PATH = "/cache/segmentation/subject.png"

class SegmentationConfirmViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toppingDraftRepository: ToppingDraftRepository = mockk()

    private fun givenDraft(draft: ToppingDraft?) {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        coEvery { toppingDraftRepository.record(any(), any(), any(), any()) } returns true
    }

    private fun draft(
        subjectImagePath: String? = SUBJECT_PATH,
        borderColorArgb: Int? = null,
        borderWidthDp: Float? = null,
    ) = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 3,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = CUTOUT_PATH,
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )

    private fun viewModel() = SegmentationConfirmViewModel(
        subjectImagePath = SUBJECT_PATH,
        cutoutImagePath = CUTOUT_PATH,
        toppingDraftRepository = toppingDraftRepository,
    )

    @Test
    fun state_followsTheDraft_notTheEntryArguments() = runTest(mainDispatcherRule.dispatcher) {
        // Given 편집을 거쳐 테두리까지 적힌 초안
        givenDraft(draft(borderColorArgb = 0xFF00FF00.toInt(), borderWidthDp = 4f))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 겹치는 구간에서는 초안이 정본이다 — 편집을 거치면 진입 인자가 낡는다
        val state = viewModel.state.value
        assertEquals(0xFF00FF00.toInt(), state.borderColorArgb)
        assertEquals(4f, state.borderWidthDp)
        assertTrue(state.isDraftReady)
    }

    @Test
    fun onEnter_writesNothing() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 이미 채워져 있다
        givenDraft(draft())

        // When 화면이 열린다
        viewModel()
        advanceUntilIdle()

        // Then 화면이 열렸다는 이유로 초안에 쓰지 않는다 — 프로세스 사망 복원에서 진입 인자가
        // 편집 결과를 덮어쓰는 경로가 그렇게 생긴다
        coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
    }

    @Test
    fun onEditResult_recordsBorderValues() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 열려 있다
        givenDraft(draft())
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 편집을 마치고 돌아온다
        viewModel.processIntent(
            SegmentationConfirmIntent.OnEditResult(
                ToppingEditResult(
                    subjectImagePath = "/cache/segmentation/edited.png",
                    cutoutImagePath = "/cache/segmentation/edited-cutout.png",
                    borderLayers = listOf(ToppingBorderLayer(colorArgb = 0xFFFF0000.toInt(), widthDp = 8f)),
                ),
            ),
        )
        advanceUntilIdle()

        // Then 테두리는 굽지 않고 값으로 적힌다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(
                subjectImagePath = "/cache/segmentation/edited.png",
                cutoutImagePath = "/cache/segmentation/edited-cutout.png",
                borderColorArgb = 0xFFFF0000.toInt(),
                borderWidthDp = 8f,
            )
        }
    }

    @Test
    fun draft_carriesTheBorder_backIntoTheEditor() = runTest(mainDispatcherRule.dispatcher) {
        // Given 한 번 두른 테두리가 초안에 있다
        givenDraft(draft(borderColorArgb = 0xFF0000FF.toInt(), borderWidthDp = 6f))

        // When 화면이 열린다
        val viewModel = viewModel()
        advanceUntilIdle()

        // Then 다시 편집을 열 때 벗겨진 채로 열리지 않는다
        assertEquals(
            listOf(ToppingBorderLayer(colorArgb = 0xFF0000FF.toInt(), widthDp = 6f)),
            viewModel.state.value.borderLayers,
        )
    }

    @Test
    fun draft_withoutSubject_blocksNext_andTellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 가리키던 캐시 파일이 사라졌거나 흐름이 열려 있지 않다
        givenDraft(draft(subjectImagePath = null))

        // When 화면이 열린다
        val viewModel = viewModel()

        // Then 알리고 다음으로 못 가게 막는다 — 여기서 안 막으면 배치 화면까지 가서야 올릴 데가
        // 없다는 것을 알게 된다
        viewModel.effect.test {
            advanceUntilIdle()
            assertEquals(SegmentationConfirmEffect.DraftMissing, awaitItem())
        }
        assertFalse(viewModel.state.value.isDraftReady)
    }
}
```

- [ ] **Step 2: 테스트가 깨지는 것을 확인한다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationConfirmViewModelTest*"`
Expected: 컴파일 실패 — `SegmentationConfirmViewModel`이 없다.

- [ ] **Step 3: ViewModel을 만든다**

```kotlin
package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * @param subjectImagePath 초안이 아직 흐르기 전 첫 프레임에만 쓰는 초기값이다. 정본은 초안이다
 */
data class SegmentationConfirmState(
    val subjectImagePath: String,
    val cutoutImagePath: String,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    val borderLayers: List<ToppingBorderLayer> = emptyList(),
    val isDraftReady: Boolean = false,
) : UiState

sealed interface SegmentationConfirmIntent : UiIntent {
    data class OnEditResult(val result: ToppingEditResult) : SegmentationConfirmIntent
}

sealed interface SegmentationConfirmEffect : UiSideEffect {
    data object DraftMissing : SegmentationConfirmEffect

    data object DraftWriteFailed : SegmentationConfirmEffect
}

@HiltViewModel(assistedFactory = SegmentationConfirmViewModel.Factory::class)
class SegmentationConfirmViewModel
@AssistedInject constructor(
    @Assisted("subjectImagePath") subjectImagePath: String,
    @Assisted("cutoutImagePath") cutoutImagePath: String,
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<SegmentationConfirmState, SegmentationConfirmIntent, SegmentationConfirmEffect>(
    initialState = SegmentationConfirmState(
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
    ),
) {
    // 초안이 비어 있다는 말은 한 번만 한다. 흐름이 여러 번 방출돼도 토스트가 쌓이면 안 된다
    private var hasReportedMissingDraft = false

    init {
        observeDraft()
    }

    override fun processIntent(intent: SegmentationConfirmIntent) {
        when (intent) {
            is SegmentationConfirmIntent.OnEditResult -> record(intent.result)
        }
    }

    private fun observeDraft() {
        launch {
            toppingDraftRepository.draft.collect { draft ->
                val subjectImagePath = draft?.subjectImagePath
                if (subjectImagePath == null) {
                    reportMissingDraft()
                    return@collect
                }

                val border = draft.borderColorArgb?.let { argb ->
                    ToppingBorderLayer(colorArgb = argb, widthDp = draft.borderWidthDp ?: 0f)
                }
                updateState {
                    copy(
                        subjectImagePath = subjectImagePath,
                        cutoutImagePath = draft.cutoutImagePath ?: cutoutImagePath,
                        borderColorArgb = draft.borderColorArgb,
                        borderWidthDp = draft.borderWidthDp,
                        // 겹칠 수 없어 언제나 0개 아니면 1개다(`adr/0025-topping-border-as-server-field.md`)
                        borderLayers = listOfNotNull(border),
                        isDraftReady = true,
                    )
                }
            }
        }
    }

    private fun reportMissingDraft() {
        if (hasReportedMissingDraft) return

        hasReportedMissingDraft = true
        postSideEffect(SegmentationConfirmEffect.DraftMissing)
    }

    private fun record(result: ToppingEditResult) {
        launch(onError = { postSideEffect(SegmentationConfirmEffect.DraftWriteFailed) }) {
            val border = result.borderLayers.lastOrNull()
            val recorded = toppingDraftRepository.record(
                subjectImagePath = result.subjectImagePath,
                cutoutImagePath = result.cutoutImagePath,
                borderColorArgb = border?.colorArgb,
                borderWidthDp = border?.widthDp,
            )
            if (!recorded) postSideEffect(SegmentationConfirmEffect.DraftWriteFailed)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("subjectImagePath") subjectImagePath: String,
            @Assisted("cutoutImagePath") cutoutImagePath: String,
        ): SegmentationConfirmViewModel
    }
}
```

> `copy(cutoutImagePath = draft.cutoutImagePath ?: cutoutImagePath)`의 오른쪽 `cutoutImagePath`는
> **상태의 현재 값**이다(`updateState`의 리시버가 상태다). 초기 상태가 진입 인자로 채워져 있으므로
> 결과가 같고, 생성자 값을 프로퍼티로 들고 있을 필요는 없다.

- [ ] **Step 4: 테스트를 통과시킨다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest --tests "*SegmentationConfirmViewModelTest*"`
Expected: PASS(5건)

- [ ] **Step 5: 화면이 테두리를 그리게 한다**

`SegmentationConfirmScreen`의 시그니처를 바꾼다.

```kotlin
@Composable
internal fun SegmentationConfirmScreen(
    subjectImagePath: String,
    borderColorArgb: Int?,
    borderWidthDp: Float?,
    isNextEnabled: Boolean,
    onClickBack: () -> Unit,
    onClickClose: () -> Unit,
    onClickEditPhoto: () -> Unit,
    onClickNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
```

기존 `Image(painter = rememberAsyncImagePainter(...), …)` 하나를 아래로 바꾼다.

```kotlin
            val painter = rememberAsyncImagePainter(
                model = subjectImagePath,
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()

            YGToppingCutoutImage(
                painter = painter,
                borderColor = borderColorArgb
                    ?.takeIf { painterState is AsyncImagePainter.State.Success }
                    ?.let { argb -> Color(argb) },
                borderWidth = (borderWidthDp ?: 0f).dp,
                modifier = Modifier.fillMaxSize(),
            )
```

다음 버튼의 `isEnabled = true`를 `isEnabled = isNextEnabled`로 바꾸고, Preview 호출부에
`borderColorArgb = null`·`borderWidthDp = null`·`isNextEnabled = true`를 더한다.

**추가할 import**: `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.getValue`,
`androidx.compose.ui.graphics.Color`, `androidx.compose.ui.unit.dp`, `coil3.compose.AsyncImagePainter`,
`com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage`.
**지울 import**: `androidx.compose.foundation.Image`.

- [ ] **Step 6: Route를 다시 쓴다**

```kotlin
@Composable
internal fun SegmentationConfirmRoute(
    navigator: Navigator,
    key: NavKeySegmentationConfirm,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SegmentationConfirmViewModel, SegmentationConfirmViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                // 미리보기·배치에 넘길 값이라 투명 여백을 걷어낸 판으로 연다.
                // 재편집 마스크는 원본 좌표계를 지켜야 해 걷지 않은 판이다
                subjectImagePath = key.trimmedSubjectImagePath,
                cutoutImagePath = key.subjectImagePath,
            )
        },
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toastPolicy = rememberYGToastPolicy()

    ResultEffect<ToppingEditResult>(resultKey = TOPPING_EDIT_RESULT_KEY) { result ->
        viewModel.processIntent(SegmentationConfirmIntent.OnEditResult(result))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                SegmentationConfirmEffect.DraftMissing,
                SegmentationConfirmEffect.DraftWriteFailed,
                -> context.getString(R.string.segmentation_confirm_draft_unavailable)
            }
            toastPolicy.showError(message)
        }
    }

    YGScaffoldV2(toastPolicy = toastPolicy) { innerPadding ->
        SegmentationConfirmScreen(
            subjectImagePath = uiState.subjectImagePath,
            borderColorArgb = uiState.borderColorArgb,
            borderWidthDp = uiState.borderWidthDp,
            isNextEnabled = uiState.isDraftReady,
            onClickBack = { navigator.onBack() },
            // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
            onClickClose = { navigator.popUpTo<NavKeyCanvasMain>() },
            onClickEditPhoto = {
                navigator.goTo(
                    NavKeyToppingEdit(
                        sourceImageUri = key.sourceImageUri,
                        // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                        segmentationImageUri = File(uiState.cutoutImagePath).toUri().toString(),
                        borderLayers = uiState.borderLayers,
                    ),
                )
            },
            onClickNext = {
                navigator.goTo(NavKeyCanvasToppingPlace(imageUri = File(uiState.subjectImagePath).toUri().toString()))
            },
            modifier = modifier.padding(innerPadding),
        )
    }
}
```

> `onClickNext`의 `NavKeyCanvasToppingPlace(imageUri = …)`는 Task 7에서 인자 없는 형태로 바뀐다.
> 여기서는 그 NavKey가 아직 인자를 요구하므로 컴파일이 통과하는 중간 형태다.

**지울 것**: 파일 상단의 `BorderLayersSaver` 선언 전체와 import
`androidx.compose.runtime.mutableStateOf`·`androidx.compose.runtime.saveable.listSaver`·
`androidx.compose.runtime.saveable.rememberSaveable`·`androidx.compose.runtime.setValue`·
`com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer`.
**추가할 import**: `androidx.compose.runtime.LaunchedEffect`, `androidx.compose.ui.platform.LocalContext`,
`androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel`,
`androidx.lifecycle.compose.collectAsStateWithLifecycle`,
`com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy`,
`com.teamyg.parfait.feature.segmentation.impl.R`,
`com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationConfirmEffect`·`…Intent`·`…ViewModel`.
(선례는 `AppSettingRoute`의 토스트 결선과 `ToppingEditRoute`의 Route 골격이다.)

- [ ] **Step 7: 문구를 더한다**

`feature/segmentation/impl/src/main/res/values/strings.xml`:

```xml
    <string name="segmentation_confirm_draft_unavailable">토핑을 이어서 만들 수 없어요. 캔버스에서 다시 시작해 주세요.</string>
```

- [ ] **Step 8: 전부 통과시킨다**

Run: `./gradlew :feature:segmentation:impl:testDebugUnitTest ktlintCheck`
Expected: PASS

- [ ] **Step 9: 커밋한다**

```bash
git add -A
git commit -m "feat(segmentation): 누끼 확인 화면이 초안을 읽고 테두리를 그린다"
```

---

## Task 7: 배치 화면이 초안을 읽는다

**Files:**
- Modify: `feature/groups/canvas/api/src/main/kotlin/.../NavKeyCanvasToppingPlace.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/.../viewmodel/CanvasToppingPlaceViewModel.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/.../route/CanvasToppingPlaceRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/.../screen/CanvasToppingPlaceScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/.../navigation/EntryBuilder.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Modify: `feature/segmentation/impl/src/main/java/.../route/SegmentationConfirmRoute.kt`(`onClickNext`)
- Test: `feature/groups/canvas/impl/src/test/kotlin/.../viewmodel/CanvasToppingPlaceViewModelTest.kt`

**Interfaces:**
- Consumes: `ToppingDraftRepository.draft`(PR3) · `YGToppingCutoutImage`(Task 2)
- Produces: `NavKeyCanvasToppingPlace`(인자 없는 `data object`) · `CanvasToppingPlaceUiState.toppingImagePath: String?`·`borderColorArgb: Int?`·`borderWidthDp: Float?` — PR5가 이 상태에서 좌표를 계산해 올린다.

> ⚠️ **Step 3~6은 한 덩어리다.** 상태·ViewModel만 고치고 테스트를 돌리면 `testDebugUnitTest`가 main
> 소스셋을 함께 컴파일하다가 Route·Screen·EntryBuilder에서 멈춘다. **여섯 단계를 다 끝낸 뒤에** 돌린다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`CanvasToppingPlaceViewModelTest`의 `viewModel()` 헬퍼를 바꾸고 케이스 셋을 더한다. 기존 케이스
(`rotatedViewModel` 경유분)는 손대지 않는다.

**추가할 import**: `com.teamyg.parfait.core.testing.MainDispatcherRule`,
`com.teamyg.parfait.domain.model.id.GroupId`·`ParfaitId`,
`com.teamyg.parfait.domain.model.topping.ToppingDraft`,
`com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository`, `io.mockk.every`·`mockk`,
`kotlinx.coroutines.flow.flowOf`, `kotlinx.coroutines.test.advanceUntilIdle`·`runTest`,
`app.cash.turbine.test`, `org.junit.Rule`.

```kotlin
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val toppingDraftRepository: ToppingDraftRepository = mockk()

    private fun draft(
        subjectImagePath: String? = "/cache/segmentation/subject.png",
        borderColorArgb: Int? = null,
        borderWidthDp: Float? = null,
    ) = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 3,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = "/cache/segmentation/cutout.png",
        borderColorArgb = borderColorArgb,
        borderWidthDp = borderWidthDp,
    )

    private fun viewModel(draft: ToppingDraft? = draft()): CanvasToppingPlaceViewModel {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        return CanvasToppingPlaceViewModel(toppingDraftRepository = toppingDraftRepository)
    }

    @Test
    fun draft_fillsTheToppingImageAndBorder() = runTest(mainDispatcherRule.dispatcher) {
        // Given 테두리까지 적힌 초안
        val viewModel = viewModel(draft(borderColorArgb = 0xFFFF0000.toInt(), borderWidthDp = 8f))

        // When 화면이 초안을 읽는다
        advanceUntilIdle()

        // Then 올릴 알맹이와 그릴 테두리가 상태에 실린다 — NavKey 인자로 나르지 않는다
        val state = viewModel.state.value
        assertEquals("/cache/segmentation/subject.png", state.toppingImagePath)
        assertEquals(0xFFFF0000.toInt(), state.borderColorArgb)
        assertEquals(8f, state.borderWidthDp)
    }

    @Test
    fun onClickConfirm_withoutASubjectImage_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안이 가리키던 캐시 파일이 이미 사라졌다
        val viewModel = viewModel(draft(subjectImagePath = null))
        advanceUntilIdle()

        // When 확인을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 조용히 아무 일도 안 하지 않는다 — 올릴 것이 없다고 알린다
            assertEquals(CanvasToppingPlaceEffect.DraftMissing, awaitItem())
        }
    }

    @Test
    fun onClickConfirm_withASubjectImage_confirmsThePlacement() = runTest(mainDispatcherRule.dispatcher) {
        // Given 올릴 알맹이가 있다
        val viewModel = viewModel()
        advanceUntilIdle()

        // When 확인을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 배치를 확정한다(서버에 올리는 것은 다음 라운드다)
            val effect = awaitItem()
            assertTrue(effect is CanvasToppingPlaceEffect.ToppingPlaced)
            assertEquals("/cache/segmentation/subject.png", effect.imagePath)
        }
    }
```

- [ ] **Step 2: 테스트가 깨지는 것을 확인한다**

Run: `./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingPlaceViewModelTest*"`
Expected: 컴파일 실패 — 생성자가 `imageUri`를 요구하고 `toppingImagePath`·`DraftMissing`이 없다.

- [ ] **Step 3: 상태와 ViewModel을 고친다**

`CanvasToppingPlaceUiState`에서 `toppingImageUri`를 걷고 셋을 더한다.

```kotlin
data class CanvasToppingPlaceUiState(
    /** 올릴 알맹이의 파일 시스템 절대경로. 초안이 비어 있으면 `null` 이다 */
    val toppingImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
    // TODO: 캔버스의 실제 배경(색/이미지) 로드 API 연동 필요 - 지금은 기본 배경색만 보여준다
    val backgroundColor: Color = YGAtomicColors.Gray.White,
    …
) : UiState
```

이펙트에 `DraftMissing`을 더하고 `ToppingPlaced`의 첫 인자 이름을 `imagePath`로 바꾼다(이제 uri가
아니라 절대경로다).

```kotlin
sealed interface CanvasToppingPlaceEffect : UiSideEffect {
    data object NavigateBack : CanvasToppingPlaceEffect

    /** 올릴 알맹이가 없다. 초안이 가리키던 캐시 파일은 먼저 사라질 수 있다 */
    data object DraftMissing : CanvasToppingPlaceEffect

    /** 사용자가 정한 위치·크기·각도로 토핑 배치를 확정했다. */
    data class ToppingPlaced(
        val imagePath: String,
        val offsetX: Dp,
        val offsetY: Dp,
        val scale: Float,
        val rotationDegrees: Float,
    ) : CanvasToppingPlaceEffect
}
```

생성자에서 Assisted를 걷고 초안을 구독한다. `Factory`와 `@HiltViewModel(assistedFactory = …)`도 지운다.
**추가할 import**: `com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository`,
`javax.inject.Inject`. **지울 import**: `dagger.assisted.Assisted`·`AssistedFactory`·`AssistedInject`.

```kotlin
@HiltViewModel
class CanvasToppingPlaceViewModel
@Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, CanvasToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(),
) {
    init {
        observeDraft()
    }

    private fun observeDraft() {
        launch {
            toppingDraftRepository.draft.collect { draft ->
                updateState {
                    copy(
                        toppingImagePath = draft?.subjectImagePath,
                        borderColorArgb = draft?.borderColorArgb,
                        borderWidthDp = draft?.borderWidthDp,
                    )
                }
            }
        }
    }
```

`handleOnClickConfirm`을 바꾼다.

```kotlin
    private fun handleOnClickConfirm() {
        val current = state.value
        val imagePath = current.toppingImagePath
        if (imagePath == null) {
            postSideEffect(effect = CanvasToppingPlaceEffect.DraftMissing)
            return
        }

        // TODO: 캔버스에 토핑을 배치하는 저장 API 연동 필요 - 지금은 결과만 이펙트로 흘려보낸다
        postSideEffect(
            effect = CanvasToppingPlaceEffect.ToppingPlaced(
                imagePath = imagePath,
                offsetX = current.offsetX,
                offsetY = current.offsetY,
                scale = current.scale,
                rotationDegrees = current.rotationDegrees,
            ),
        )
    }
```

- [ ] **Step 4: NavKey 인자를 걷고 Route·EntryBuilder를 맞춘다**

```kotlin
package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 배치할 토핑은 초안이 나른다(`adr/0026-topping-draft-datastore-ssot.md`) */
@Serializable
data object NavKeyCanvasToppingPlace : NavKey
```

`EntryBuilder.kt`:

```kotlin
    entry<NavKeyCanvasToppingPlace> {
        CanvasToppingPlaceRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
```

`CanvasToppingPlaceRoute`에서 `key` 파라미터와 `creationCallback`을 걷고 토스트를 결선한다.
**지울 import**: `com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace`(더 이상 안 쓴다).
**추가할 import**: `androidx.compose.ui.platform.LocalContext`,
`com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy`,
`com.teamyg.parfait.feature.groups.canvas.impl.R`.

```kotlin
@Composable
internal fun CanvasToppingPlaceRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasToppingPlaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toastPolicy = rememberYGToastPolicy()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CanvasToppingPlaceEffect.NavigateBack -> navigator.onBack()

                // 올릴 것이 없는 화면에 사용자를 남기지 않는다. 되돌릴 편집 결과도 이미 없다
                CanvasToppingPlaceEffect.DraftMissing -> {
                    toastPolicy.showError(context.getString(R.string.canvas_topping_place_draft_unavailable))
                    navigator.popUpTo<NavKeyCanvasMain>()
                }

                is CanvasToppingPlaceEffect.ToppingPlaced -> {
                    // TODO: 배치 결과(effect)를 캔버스 상태에 반영/서버에 저장하는 연동 필요
                    // 캔버스를 새로 쌓지 않고 원래 자리로 되감는다. 새로 쌓으면 방금 끝난 토핑 만들기
                    // 화면들이 그 밑에 남고, 다음 흐름이 진입하며 비우는 세그멘테이션 캐시가 그 화면들이
                    // 가리키던 PNG 를 지운다(뒤로 가면 빈 이미지만 남는다)
                    navigator.popUpTo<NavKeyCanvasMain>()
                }
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
```

`feature/groups/canvas/impl/src/main/res/values/strings.xml`:

```xml
    <string name="canvas_topping_place_draft_unavailable">올릴 토핑을 찾지 못했어요. 캔버스에서 다시 시작해 주세요.</string>
```

`SegmentationConfirmRoute`의 `onClickNext`도 바꾼다.

```kotlin
            onClickNext = { navigator.goTo(NavKeyCanvasToppingPlace) },
```

이 변경으로 그 파일의 `java.io.File`·`androidx.core.net.toUri` import가 아직 `onClickEditPhoto`에서
쓰이는지 확인하고, 안 쓰이면 지운다.

- [ ] **Step 5: 화면이 초안 값으로 그리게 한다**

`CanvasToppingPlaceScreen`의 painter 자리를 바꾼다.

```kotlin
            val toppingImagePath = uiState.toppingImagePath
            val painter = rememberAsyncImagePainter(
                // 이 화면은 지금까지 file 스킴 uri 로 그려 왔다. 초안이 담는 것은 절대경로다
                model = remember(toppingImagePath) {
                    toppingImagePath?.let { path -> File(path).toUri().toString() }
                },
                contentScale = ContentScale.Fit,
            )
            val painterState by painter.state.collectAsState()
            val isToppingImageLoaded = painterState is AsyncImagePainter.State.Success
            val baseSize = rememberToppingBaseSize(painter)

            // 그림이 뜨기 전 실측은 고정 폴백 크기다. 그것을 올려보내면 폴백 기준으로 계산된 배율이
            // 배치에 굳는다 — 초안을 읽어 오는 동안 그 창이 생긴다
            LaunchedEffect(baseSize, isToppingImageLoaded) {
                if (isToppingImageLoaded) onToppingBaseSizeMeasured(baseSize)
            }
```

이미지를 그리던 안쪽 `Image(...)` 하나를 아래로 바꾼다(감싸는 `Box`의 `requiredSize`·`dragBy`·
`graphicsLayer`는 그대로 둔다).

```kotlin
                    YGToppingCutoutImage(
                        painter = painter,
                        // 그림이 뜨기 전에 찍으면 플레이스홀더 실루엣이 테두리로 보인다
                        borderColor = uiState.borderColorArgb
                            ?.takeIf { isToppingImageLoaded }
                            ?.let { argb -> Color(argb) },
                        borderWidth = (uiState.borderWidthDp ?: 0f).dp,
                        modifier = Modifier.fillMaxSize(),
                    )
```

Preview 호출부의 `CanvasToppingPlaceUiState(toppingImageUri = "")`를 `CanvasToppingPlaceUiState()`로 바꾼다.

**추가할 import**: `androidx.compose.runtime.remember`, `androidx.compose.runtime.collectAsState`,
`androidx.compose.ui.graphics.Color`, `coil3.compose.AsyncImagePainter`, `androidx.core.net.toUri`,
`java.io.File`, `com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.YGToppingCutoutImage`.
**지울 import**: `androidx.compose.foundation.Image`.

- [ ] **Step 6: 전부 통과시킨다**

Run:
```bash
./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest \
  :feature:segmentation:impl:testDebugUnitTest ktlintCheck :app:assembleDebug
```
Expected: PASS

- [ ] **Step 7: 커밋한다**

```bash
git add -A
git commit -m "feat(canvas): 배치 화면이 초안을 읽고 같은 스탬프로 그린다"
```

---

## Task 8: 갈린 결정을 문서에 되싣는다

> ⚠️ **이 태스크만 작업 저장소가 다르다.** `TJYG-Android`가 아니라 이 계획이 사는
> `team-yg-pesonal-agent`에서 한다. 코드 변경은 0건이다.

**Files:**
- Modify: `parfait/specs/archive/2026-08-20-c106-topping-place-api.md`
- Modify: `parfait/architecture/module-structure.md`
- Modify: `parfait/architecture/design-system.md`
- Modify: `parfait/adr/0025-topping-border-as-server-field.md`
- Modify: `parfait/synthesis/open-questions.md`
- Modify: `parfait/plans/README.md`

- [ ] **Step 1: 스펙을 실제와 맞춘다**

- 「좌표 변환」 절의 **"`YGCanvas`가 `domain`의 상수를 쓰도록 통일한다"**를 **"`domain`의
  `CANVAS_ASPECT_RATIO`를 지우고 `core:designsystem`의 `CANVAS_AREA_ASPECT_RATIO` 하나로 통일한다"**로
  바꾸고 근거 한 줄을 단다.
- 테스트 표의 「종횡비 상수」 행을 **"상수가 하나뿐이라 컴파일이 보증한다(단언 없음)"**로 바꾼다.
- 「파일 구성」 표 두 행을 고친다 — `YGCanvas.kt` 행("private 종횡비 상수를 `domain` 것으로 교체")과
  8방향 스탬프 행(`feature/…/component/`가 아니라 `core:designsystem`이 소유한다).
- 「토핑 초안 SSOT」 표의 "세그멘테이션 완료" 행에, 쓰는 주체가 `SegmentationViewModel`이고
  **화면 진입이 아니라 사건에 건다**는 사실과 그 이유(프로세스 사망 복원)를 한 줄 단다.

- [ ] **Step 2: 아키텍처 문서 둘을 고친다**

- `module-structure.md`의 ⚠️ **표시 규격이 `domain`에 들어온 사례**(2026-08-15) 항목에 해소 메모를 단다.
- 같은 문서의 📌 **화면 둘이 공유하는 컴포저블은 같은 모듈 `component/`에 둔다**(2026-08-19) 항목에,
  **모듈 둘·화면 셋이 나눠 쓰면 `:core:designsystem`으로 올린다**는 이번 사례를 잇는다.
- `design-system.md`의 컴포넌트 인벤토리 표에 `YGToppingCutoutImage`(`component/ygtoppingcutout/`)를
  올리고, 기존 `YGToppingImage`(G-001 파르페 토핑)와 **다른 물건**임을 한 줄로 적는다.

- [ ] **Step 3: ADR-0025를 as-built로 맞춘다**

- 「트레이드오프」의 **"캐시 PNG가 흐름당 한 장 더 는다"**를 정정한다 — 굽기 전에도 편집은 파일 둘
  (`cutout`·트리밍본)을 저장했으므로 장수는 늘지 않는다.
- 렌더러의 소유가 `:core:designsystem`이라는 사실을 「결정」에 반영한다.
- `status`를 그대로 `proposed`로 둘지 `accepted`로 올릴지 판단하고, 올린다면 근거를 남긴다.

- [ ] **Step 4: open-questions를 갱신한다**

- **해소**: 종횡비 상수 이중 소유(2026-08-15).
- **신규**: C-301 배경 편집이 테두리를 그리지 않아 **테두리 재편집이 무반응**이다. 값은
  `CanvasToppingItem.borderLayers`에 있고 공유 컴포저블도 이미 있으므로, C-301 라운드가 붙이면 된다.
- **📌 메모**: OQ-P-245(편집 화면 굵기와 캔버스 굵기의 어긋남)가 **PR4에서 사용자 화면에 실현됐다**.

- [ ] **Step 5: 계획을 인덱스에 올린다**

`parfait/plans/README.md`의 표에서 이 계획의 줄을 실행 결과에 맞게 갱신한다.

- [ ] **Step 6: 커밋한다**

```bash
git add parfait/
git commit -m "docs: PR4 테두리 계약 전환의 결정을 문서에 반영한다"
```

---

## 완료 조건

자동 검증:

```bash
./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest \
  :feature:segmentation:impl:testDebugUnitTest ktlintCheck :app:assembleDebug
```

신규 테스트 **13건**(Task 1에서 3, Task 5에서 2, Task 6에서 5, Task 7에서 3).

**실기기 확인**(이 라운드의 진짜 감지선이다 — 시각 회귀가 여기 몰린다):

1. 촬영 → 누끼 → **편집에서 테두리를 두르고** 확인 화면으로 돌아온다. 확인 화면에 테두리가 보인다.
   **편집 화면에서 본 것보다 가늘어 보이는 것은 의도된 변화다**(편집은 원본 픽셀, 이후는 화면 dp).
2. 편집 → 확인 → 배치 → 캔버스로 이어지는 동안 **색이 같고 굵기가 화면 dp로 고정**된다. 토핑이
   화면마다 다른 크기로 보이므로 **상대적인 굵기는 달라 보인다** — 그 어긋남은 OQ-P-245의 판정
   대상이지 이 라운드의 회귀가 아니다.
3. 테두리를 **투명 칩으로 벗기고** 확인 화면으로 돌아온다. 테두리가 사라진다.
4. 편집을 **두 번** 한다. 두 번째 편집 화면이 첫 번째 테두리를 두른 채 열린다.
5. 확인 화면에서 편집을 마친 뒤 **앱을 강제 종료했다가 다시 연다.** 편집 결과와 테두리가 살아 있다
   (초안 쓰기를 화면 진입이 아니라 사건에 건 이유가 이것이다).
6. 이미 놓인 토핑이 있는 캔버스를 연다. 서버가 준 테두리가 종전과 같이 보인다(Task 2 회귀 확인).
7. 배치 화면에서 토핑을 키운다. **테두리 굵기는 그대로다** — 의도된 변화다.
8. 캔버스 편집(C-301)에서 토핑을 골라 테두리를 다시 편집한다. **아무 변화가 없다** — 위
   「보이는 변화」 3번의 의도된 상태이고, 회귀가 아니다.
9. **PR3이 남긴 미확인 5항목을 함께 본다**(토핑 추가 버튼 비활성 표현, 오늘 캔버스 조회 실패 토스트가
   **캔버스 프레임 상단**에 뜨는지 등). 두 라운드가 한 브랜치로 나가므로 여기서 함께 잡지 않으면
   양쪽 감지선이 빈 채로 리모트에 나간다.

**하지 않은 것**(PR5 몫): 좌표 변환 · `AddToppingUseCase` 호출 · 로딩 오버레이 · 실패 코드별 되감기 ·
성공 시 초안 비우기 · **확인 버튼 비활성 표현과 painter 상태를 근거로 삼는 가드** ·
선행 미결 둘(OQ-P-109·OQ-P-246).

> 참고 — 검수에서 제기된 "`trimmedSubjectImagePath`가 트리밍 안 된 원본일 수 있다"는 경로는
> **이 흐름에 도달하지 않는다.** `SegmentationViewModel`이 `subjectBounds == null`이면 오류로 끊어
> 확인 화면으로 넘어가지 않는다(`ImageSegmentationRepositoryImpl`의 `(trimmedFile ?: file)` 폴백은
> 그 지점에서 소비되지 않는다).
