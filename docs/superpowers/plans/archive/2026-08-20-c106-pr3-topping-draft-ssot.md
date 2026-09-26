---
id: c106-pr3-topping-draft-ssot
title: C-106 결선 PR3 — 토핑 초안 SSOT + C-001 정비 (ToppingDraft·YGScaffoldV2·추가 버튼 가드)
status: done
type: work-order
created: 2026-08-20
updated: 2026-08-20
archived_reason: 구현 완료·develop 머지(2026-08-22, PR #334 19cb5299). 브랜치 feature/#270-topping-draft-ssot 에 커밋 9개.
platforms: android
owner: Parfait 팀
related_adr: ADR-0026, ADR-0025
related_spec: c106-topping-place-api, ygscaffold-v2-common-loading-error, screen-resume-refetch
related_code:
  - CanvasMainViewModel.kt#loadTodayCanvas
  - CanvasMainViewModel.kt#handleOnClickCamera
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasMainScreen.kt#CanvasMainScreen
  - EntryBuilder.kt#featureCanvasEntryBuilder
  - YGScaffold.kt#YGScaffold
  - YGScaffoldV2.kt#YGScaffoldV2
  - YGCanvasMenuAction.kt#YGCanvasMenuAction
  - YGCanvasMenu.kt#YGCanvasMenu
  - YGStrokeButton.kt#YGStrokeButton
  - RecentImageLocalDataSourceImpl.kt
  - UserInfoLocalDataSourceImpl.kt
  - UserInfoEntity.kt
  - FakePreferencesDataStore.kt
  - DataStoreModule.kt#provideParfaitPreferencesDataStore
  - LocalDataSourceModule.kt
  - RepositoryModule.kt
  - BaseViewModel.kt#launch
tags: [plan, parfait, topping, canvas, datastore, state]
---

# C-106 결선 PR3 — 토핑 초안 SSOT + C-001 정비 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 토핑 만들기 흐름의 상태를 담을 DataStore 초안 한 벌을 만들고, `CanvasMain`이 흐름에 들어설 때 그 초안을 캔버스 식별값으로 새로 쓰게 한다. 함께 C-001을 `YGScaffoldV2`로 옮겨 오늘 캔버스 조회 실패를 화면에 드러내고, 올릴 캔버스가 없을 때 토핑 추가 버튼을 비활성한다.

**Architecture:** 초안은 `:data`의 기존 `DataStore<Preferences>` 위에 JSON 한 줄로 산다([ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md)). 계층은 셋이다 — 직렬화·영속만 아는 `ToppingDraftLocalDataSource`, 흐름 규칙(진입 시 덮어쓰기·사라진 캐시 경로 정규화)을 아는 `ToppingDraftRepository`, 그리고 그 규칙을 부르는 `CanvasMainViewModel`. **이 PR에서 초안을 읽는 화면은 아직 없다** — 이미지·테두리를 채우는 것은 PR4, 읽어서 올리는 것은 PR5다. 사용자에게 보이는 변화는 셋이다: **토핑 추가 버튼 가드 · 오늘 캔버스 조회 실패 토스트 · 초안 쓰기 실패 토스트.** 스펙 PR 표는 앞의 둘만 적었고 셋째는 이 계획이 더한 방어다(아래 Task 5, 스펙 「토핑 초안 SSOT」에도 같은 문장을 실어 두었다).

**Tech Stack:** Kotlin · Hilt · DataStore Preferences · kotlinx.serialization · Jetpack Compose · kotlinx-coroutines-test · MockK · Turbine · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-20-c106-topping-place-api.md`](../../specs/archive/2026-08-20-c106-topping-place-api.md) — PR 분할 표 **3번 행**, 「토핑 초안 SSOT」·「표시·제어 규칙」·「C-001 오늘 캔버스 조회 실패 표현」 절

> ✅ **실행 완료(2026-08-20) · develop 머지(2026-08-22, PR #334 `19cb5299` — 스택 넷이 함께 들어왔다)**
> — subagent-driven-development로 5 태스크. 브랜치
> `feature/#270-topping-draft-ssot`(PR2 브랜치 `feature/#270-topping-place-domain` 위), 커밋 **10개**
> (`6938e8a7`..`90eb22a1`, 아래 리베이스 뒤 기준). 신규 테스트 **20건**(계획 예상 18건 + 최종 리뷰가 요구한 2건),
> 검증 명령 `:domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest
> ktlintCheck :app:assembleDebug` 전부 통과. **머지·push 안 했다.**
>
> 태스크 리뷰는 Task 2에서 fix 라운드 1회(정규화 테스트가 safe-call 단언이라 초안 전체가 `null`이
> 되는 회귀를 못 잡던 것), 나머지 넷은 지적 0건 통과.
>
> **최종 브랜치 리뷰(opus)가 잡은 진짜 결함 하나** — 지난 날짜를 보는 동안 파르페 하루 경계를 넘기면
> `syncToday`의 else 분기가 `todayCanvas`를 어제 것인 채로 남기고, `handleClickGoToToday`가 재조회를
> 하지 않아 **어제 `parfaitId`가 초안에 못 박히는** 경로가 있었다. PR5에서 촬영·누끼·편집을 다 마친
> 뒤 409를 맞는 시나리오이고, 버튼 가드가 막으려던 바로 그 실패다. 이 PR 이전부터 있던 표시 결함인데
> 초안이 그 값을 "못 박히는 값"으로 승격시켜 무거워졌다. 두 자리를 고치고 테스트 둘로 잠갔다
> (`6456070a`). 함께 닫은 것 셋: 성공 경로의 내비게이션 이펙트 미단언, DataSource 의 dedupe·decode
> 순서, 같은 결정 설명 3중복.
>
> **계획 텍스트와 갈린 것 둘:**
> - **이 문서의 코드 블록에 적힌 KDoc·주석은 최종 형태가 아니다.** 실행 뒤 주석이 코드의 어려움에
>   비해 과하다는 지적을 받아 6파일에서 23줄을 걷었다(`59e20635`). 걷은 것은 코드가 이미 말하는 것,
>   두 계층에 겹쳐 적힌 dedupe 설명, 다른 파일의 현재 상태를 단정하던 주석이다. **코드가 정본이다.**
> - `ToppingDraftLocalDataSourceImpl.draft` 가 계획과 달리 원문 단계에서 `distinctUntilChanged` 한 뒤
>   decode 하고, Repository 쪽 중복 dedupe 는 지웠다. 이웃 `EncryptedPreferences.observe` 가
>   같은 이유로 같은 순서를 쓴다.
>
> 🔁 **스택 셋을 develop `9b112e86` 위로 다시 쌓았다(2026-08-20 저녁)** — PR1·PR2는 충돌 0건이었고
> **PR3만 develop 의 Spotlight 라운드(PR #298)와 부딪혔다.** 그 라운드가 C-001 에 작성자 토스트·
> `LifecycleStartEffect`·토핑 탭 콜백을 더하면서 `CanvasMainRoute`·`CanvasMainViewModel` 을 같이 고쳤기
> 때문이고, 커밋 셋에 걸쳐 해소했다. 양쪽 기능은 전부 살렸다.
>
> **그 과정에서 토스트 자리 하나를 다시 정했다**(계획에 없던 결정, 커밋 `90eb22a1`). Spotlight 토스트가
> `YGCanvas` 의 `overlayContent`에 — 즉 **캔버스 프레임 상단**에 — 이미 못 박혀 있어서, 실패 토스트를
> `YGScaffoldV2` 의 공통 자리로 보내면 같은 화면에서 토스트 자리가 둘로 갈리고 [[toast]] 공통 정책의
> 스택(나중 것이 위로)이 큐마다 따로 논다. 그래서 **정책 하나를 화면 쪽 호스트에 넘기고 스캐폴드에는
> 넘기지 않는다.** 스캐폴드는 이 화면에서 로딩 오버레이 자리로만 남는다(PR5 가 쓴다).
> ygscaffold-v2 스펙이 정한 "공통 실패 자리는 스캐폴드"와 갈리는 첫 사례라
> [open-questions OQ-P-167](../../../synthesis/open-questions.md)에 근거를 남겼다.
>
> **아직 안 한 것**: 아래 「완료 조건」의 실기기 확인 5항목. 비활성 버튼 표현과 토스트 노출은
> `:core:designsystem`에 호스트 테스트 소스셋이 없어 사람 눈이 유일한 감지선이고, 리베이스로 토스트가
> 뜨는 자리가 바뀌었으므로 **조회 실패 토스트가 캔버스 프레임 상단에 뜨는지**도 함께 본다.
>
> 🔁 **스택 넷을 develop `ef55a58c` 위로 다시 쌓았다(2026-08-22, PR3~PR6 공통 기록)** —
> `git rebase --update-refs`로 네 브랜치를 한 번에 옮겼고 커밋 43개가 그대로 재적용됐다(유실·중복 0건).
> 새 팁은 **PR3 `90eb22a1` · PR4 `17a2c09f` · PR5 `67ee0d6a` · PR6 `656cbf2e`**이고 force-push 해
> 리모트도 같다. PR 베이스 구조(#327→develop, #331→#327, #333→#331, #334→#333)는 그대로다.
> **이 라운드가 스택의 해시를 전부 다시 썼으므로 PR3~PR6 문서·`plans/README`·스펙이 인용하던 옛
> 해시는 새 것으로 갱신했다.** 옛 팁은 로컬 `refs/backup/prestack/<브랜치>`에 남겨 뒀다.
>
> **충돌은 한 자리뿐이다** — `SegmentationViewModel.kt`. develop의 #311이 세그멘테이션 실패 표현을
> 상태(`SegmentationState.isError`)에서 1회성 이펙트(`SegmentationEffect.ShowError`)로 옮겼는데 이
> 스택은 같은 자리에 초안 기록(`toppingDraftRepository.record`)을 넣었다. 둘 다 살렸다 — 기록 블록은
> 스택 것 그대로 두고 실패 경로만 develop의 이펙트를 따른다. 스택이 `isError`를 읽거나 쓰던 자리는
> 남지 않았고, **스택 쪽 동작 서술은 바뀌지 않는다**(초안 기록은 `subjectBounds == null`이면 여전히
> 건너뛴다). 스택 문서가 #311·#325가 지운 심볼을 인용하던 자리도 없다.
>
> 검증: 전 모듈 `testDebugUnitTest` 통과. 리베이스 전후 diff가 develop delta(#311·#325)와 정확히
> 같아 스택 콘텐츠 유실이 없음을 확인했다. **실기기 확인은 여전히 0회다.**

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**이고 이 문서가 사는 저장소가 아니다. 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다.
- **베이스 브랜치는 `feature/#270-topping-place-domain`**(PR2, 팁 `4b780d17`, **미머지**)이다. 그 위에 새 브랜치 `feature/#270-topping-draft-ssot`를 만들어 작업한다. PR1·PR2의 커밋은 그대로 둔다.
- **커밋은 태스크마다 한다.** `git push`·`gh pr create`·`gh pr merge`는 **하지 않는다** — 사용자 확인이 필요한 작업이다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 선언에 멀티라인을 강제한다**(`.editorconfig`의 `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`). 이 계획의 코드 블록은 이미 그 형태로 적혀 있으니 **한 줄로 줄이지 말 것.** 최대 줄 길이는 120이다.
- **주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 근거는 문서를 가리킨다. 함정과 의도는 쓴다.
  - 아키텍처 결정 설명을 코드에 복사하지 않는다. 포인터 한 줄만 둔다.
- **초안이 담는 이미지 경로는 파일 시스템 절대경로**다. `file://` uri가 아니다.
- **이 PR은 초안에 이미지·테두리를 채우지 않는다.** 모델에 자리는 만들되 쓰는 곳은 PR4다. `SegmentationConfirmRoute`·`ToppingEditRoute`·`NavKey` 세 가지는 **한 줄도 건드리지 않는다.**
- **`TOPPING_EDIT_RESULT_KEY`를 걷지 않는다.** 소비자가 `SegmentationConfirmRoute`와 `CanvasBGEditRoute` 둘이라 걷으면 배경 편집 쪽이 조용히 죽는다([ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md)). 이 PR의 범위 밖이지만, 초안을 만들었다는 이유로 손대고 싶어지는 자리라 미리 못 박는다.
- 매퍼 단독 테스트(`XxxEntityMapperTest`)는 만들지 않는다. 변환 판단은 DataSource 테스트 케이스로 잠근다.
- 검증 명령(태스크마다 해당하는 것을 전부 통과해야 한다):
  ```bash
  ./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
  ```
  마지막 태스크에서만 `./gradlew :app:assembleDebug`까지 돌린다. `:domain`은 JVM 모듈이라 `:domain:test`이고 `:data`·`:feature:*`는 안드로이드 모듈이라 `testDebugUnitTest`다 — 헷갈리면 태스크가 통째로 안 돈다.
- ⚠️ **`:core:designsystem`에는 호스트 단위 테스트 소스셋이 없다**(`src/`에 `main`과 `androidTest`뿐이다). Task 3의 디자인시스템 변경은 기기 없이 돌릴 자동 테스트가 없어 Preview와 상위 태스크의 상태 테스트로 잠근다. 그 태스크에서만 예외이고 다른 태스크는 전부 테스트를 먼저 쓴다.

## 파일 구성

| 파일 | 책임 |
|---|---|
| `domain/model/topping/ToppingDraft.kt` (신규) | 흐름 상태 한 벌의 도메인 형태 |
| `data/model/local/ToppingDraftEntity.kt` (신규) | 위의 저장 형태 + 변환 |
| `data/source/toppingdraft/local/ToppingDraftLocalDataSource.kt` (신규) | 초안 저장 계약 |
| `data/source/toppingdraft/local/ToppingDraftLocalDataSourceImpl.kt` (신규) | DataStore 한 키에 JSON 으로 저장·관찰·삭제 |
| `data/di/LocalDataSourceModule.kt` (수정) | 위 구현 바인딩 |
| `domain/repository/topping/ToppingDraftRepository.kt` (신규) | 흐름 규칙 계약(진입 덮어쓰기·비우기·읽기) |
| `data/repository/topping/ToppingDraftRepositoryImpl.kt` (신규) | 위 구현 + 사라진 캐시 경로 정규화 |
| `data/di/RepositoryModule.kt` (수정) | 위 구현 바인딩 |
| `core/designsystem/component/ygcanvasmenu/YGCanvasMenuAction.kt` (수정) | 액션에 활성 여부를 싣는다 |
| `core/designsystem/component/ygcanvasmenu/YGCanvasMenu.kt` (수정) | 그 값을 `YGStrokeButton` 에 전달 |
| `feature/groups/canvas/impl/navigation/EntryBuilder.kt` (수정) | C-001 엔트리에서 스캐폴드를 걷는다 |
| `feature/groups/canvas/impl/route/CanvasMainRoute.kt` (수정) | `YGScaffoldV2` 소유 + 토스트 정책 |
| `feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt` (수정) | 조회 실패 알림 · 흐름 진입 시 초안 쓰기 · 추가 버튼 활성 판정 |
| `feature/groups/canvas/impl/screen/CanvasMainScreen.kt` (수정) | 추가 버튼 활성 전달 |
| `feature/groups/canvas/impl/res/values/strings.xml` (수정) | 실패 문구 둘 |

---

### Task 1: 초안의 도메인 형태와 저장소

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/model/local/ToppingDraftEntity.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `DataStore<Preferences>`(기존 `DataStoreModule#provideParfaitPreferencesDataStore`) · `@LocalJson Json`(기존 `JsonModule`)
- Produces:
  - `ToppingDraft(groupId: GroupId, parfaitId: ParfaitId, nextPositionZ: Int, subjectImagePath: String? = null, cutoutImagePath: String? = null, borderColorArgb: Int? = null, borderWidthDp: Float? = null)`
  - `ToppingDraftLocalDataSource.draft: Flow<ToppingDraft?>` · `suspend fun save(draft: ToppingDraft)` · `suspend fun clear()`

- [ ] **Step 0: 브랜치를 만든다**

```bash
git checkout feature/#270-topping-place-domain
git checkout -b feature/#270-topping-draft-ssot
```

- [ ] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.toppingdraft.local

import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToppingDraftLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    private val dataSource = ToppingDraftLocalDataSourceImpl(
        dataStore = dataStore,
        json = Json { ignoreUnknownKeys = true },
    )

    private val filledDraft = ToppingDraft(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(2L),
        nextPositionZ = 4,
        subjectImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/subject.png",
        cutoutImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/cutout.png",
        borderColorArgb = 0xFFFF6B6B.toInt(),
        borderWidthDp = 4f,
    )

    @Test
    fun save_thenRead_roundTripsEveryField() = runTest {
        // Given 이미지와 테두리까지 다 채워진 초안

        // When 저장하고 다시 읽는다
        dataSource.save(filledDraft)

        // Then 필드가 하나도 뒤바뀌지 않는다 — 값 클래스 둘과 널 넷을 거쳐 오므로 매퍼가
        // 뒤집혀도 컴파일러가 막지 못한다
        assertEquals(filledDraft, dataSource.draft.first())
    }

    @Test
    fun save_overExistingDraft_replacesItWhole() = runTest {
        // Given 이미지까지 채워진 지난 흐름의 초안이 남아 있다
        dataSource.save(filledDraft)

        // When 캔버스 식별값만 든 새 초안을 얹는다
        val fresh = ToppingDraft(
            groupId = GroupId(9L),
            parfaitId = ParfaitId(8L),
            nextPositionZ = 1,
        )
        dataSource.save(fresh)

        // Then 지난 흐름의 이미지가 따라붙지 않는다 — 병합이 아니라 통째로 덮어쓴다
        assertEquals(fresh, dataSource.draft.first())
    }

    @Test
    fun clear_afterSave_readsNull() = runTest {
        // Given 저장된 초안
        dataSource.save(filledDraft)

        // When 비운다
        dataSource.clear()

        // Then 흐름 밖과 같은 상태가 된다
        assertNull(dataSource.draft.first())
    }

    @Test
    fun draft_nothingSaved_isNull() = runTest {
        // Given, When 아무것도 저장하지 않았다

        // Then 빈 값이 아니라 null 이다 — 없는 초안을 기본값으로 지어내면 읽는 쪽이
        // 있지도 않은 캔버스에 올리려 든다
        assertNull(dataSource.draft.first())
    }

    @Test
    fun draft_storedFormatIsUnreadable_isNull() = runTest {
        // Given 앱 판올림 전 형태로 저장돼 지금은 못 읽는 값
        dataStore.putRaw(ToppingDraftLocalDataSourceImpl.TOPPING_DRAFT_KEY_NAME, "{\"groupId\":")

        // Then 터지지 않고 초안이 없는 것으로 본다 — 흐름은 진입에서 다시 열린다
        assertNull(dataSource.draft.first())
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSourceImplTest"
```

Expected: 컴파일 실패 — `Unresolved reference: ToppingDraftLocalDataSourceImpl`(`ToppingDraft` 도 아직 없다)

- [ ] **Step 3: 도메인 모델을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt`:

```kotlin
package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId

/**
 * 토핑 만들기 흐름의 상태 한 벌. 흐름당 하나만 존재한다
 * (`adr/0026-topping-draft-datastore-ssot.md`).
 *
 * 이미지와 테두리가 비어 있는 초안은 흐름에 막 들어선 상태다 — 진입은 캔버스 식별값 셋만
 * 알고, 나머지는 흐름의 뒤 단계가 채운다.
 *
 * @param nextPositionZ 흐름에 들어설 때 못 박은 값이라 그 사이 남이 올린 토핑과 겹칠 수 있다
 *   (`specs/archive/2026-08-20-c106-topping-place-api.md` 결정 표).
 * @param subjectImagePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
 * @param cutoutImagePath 재편집 시작 마스크. 좌표계를 지켜야 해 트리밍하지 않는다.
 * @param borderColorArgb null 이면 테두리가 없다.
 */
data class ToppingDraft(
    val groupId: GroupId,
    val parfaitId: ParfaitId,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
)
```

- [ ] **Step 4: 저장 형태와 변환을 만든다**

`data/src/main/java/com/teamyg/parfait/data/model/local/ToppingDraftEntity.kt`:

```kotlin
package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.serialization.Serializable

/** 초안의 저장 형태. 값 클래스를 품고 있어 domain 이 직렬화를 알게 하지 않는다(`adr/0001-layered-multi-module.md`) */
@Serializable
internal data class ToppingDraftEntity(
    val groupId: Long,
    val parfaitId: Long,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
)

internal fun ToppingDraft.toEntity(): ToppingDraftEntity = ToppingDraftEntity(
    groupId = groupId.value,
    parfaitId = parfaitId.value,
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
)

internal fun ToppingDraftEntity.toVO(): ToppingDraft = ToppingDraft(
    groupId = GroupId(groupId),
    parfaitId = ParfaitId(parfaitId),
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
)
```

- [ ] **Step 5: 저장 계약과 구현을 만든다**

`data/src/main/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSource.kt`:

```kotlin
package com.teamyg.parfait.data.source.toppingdraft.local

import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow

interface ToppingDraftLocalDataSource {
    /** 흐름 밖이면 null 이다 */
    val draft: Flow<ToppingDraft?>

    /** 병합하지 않고 통째로 덮어쓴다 */
    suspend fun save(draft: ToppingDraft)

    suspend fun clear()
}
```

`data/src/main/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImpl.kt`:

```kotlin
package com.teamyg.parfait.data.source.toppingdraft.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.model.local.ToppingDraftEntity
import com.teamyg.parfait.data.model.local.toEntity
import com.teamyg.parfait.data.model.local.toVO
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToppingDraftLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    @LocalJson private val json: Json,
) : ToppingDraftLocalDataSource {
    override val draft: Flow<ToppingDraft?> = dataStore.data
        .map { prefs -> decode(prefs[TOPPING_DRAFT_KEY]) }

    override suspend fun save(draft: ToppingDraft) {
        dataStore.edit { prefs ->
            prefs[TOPPING_DRAFT_KEY] = json.encodeToString(draft.toEntity())
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(TOPPING_DRAFT_KEY) }
    }

    /**
     * 못 읽는 값은 초안이 없는 것으로 본다 — 흐름은 진입에서 새로 열리므로 되살릴 이유가 없다.
     * 손상분을 지우지 않는 것도 같은 이유다(다음 진입이 덮어쓴다).
     */
    private fun decode(raw: String?): ToppingDraft? {
        if (raw.isNullOrBlank()) {
            return null
        }

        return runCatching { json.decodeFromString<ToppingDraftEntity>(raw).toVO() }.getOrNull()
    }

    internal companion object {
        const val TOPPING_DRAFT_KEY_NAME = "topping_draft"
        val TOPPING_DRAFT_KEY = stringPreferencesKey(TOPPING_DRAFT_KEY_NAME)
    }
}
```

- [ ] **Step 6: DI 바인딩을 더한다**

`data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt`의 import 블록은 알파벳 순으로 유지돼 있다(ktlint의 `import-ordering`은 꺼져 있어 빌드가 깨지지는 않는다). 자리를 지켜 둘을 더한다 — `data.source.token.local.TokenStore` 다음이다:

```kotlin
import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSourceImpl
```

인터페이스 본문 끝(`bindGroupLocalDataSource` 아래)에 바인딩을 더한다:

```kotlin
    @Binds
    @Singleton
    fun bindToppingDraftLocalDataSource(
        toppingDraftLocalDataSourceImpl: ToppingDraftLocalDataSourceImpl,
    ): ToppingDraftLocalDataSource
```

- [ ] **Step 7: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
```

Expected: PASS

- [ ] **Step 8: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingDraft.kt \
        data/src/main/java/com/teamyg/parfait/data/model/local/ToppingDraftEntity.kt \
        data/src/main/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSource.kt \
        data/src/main/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/LocalDataSourceModule.kt \
        data/src/test/java/com/teamyg/parfait/data/source/toppingdraft/local/ToppingDraftLocalDataSourceImplTest.kt
git commit -m "feat(topping): 토핑 초안을 DataStore 한 키에 저장한다

흐름당 하나만 두고 저장은 병합이 아니라 통째로 덮어쓴다.
못 읽는 값은 초안이 없는 것으로 본다 - 흐름은 진입에서 새로 열린다."
```

---

### Task 2: 흐름 규칙을 아는 ToppingDraftRepository

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `ToppingDraftLocalDataSource.draft: Flow<ToppingDraft?>` · `save(draft: ToppingDraft)` · `clear()` (Task 1)
- Produces:
  - `ToppingDraftRepository.draft: Flow<ToppingDraft?>`
  - `suspend fun start(groupId: GroupId, parfaitId: ParfaitId, nextPositionZ: Int)`
  - `suspend fun clear()`

> **바인딩 감지 테스트를 두지 않는 이유**: PR1·PR2와 달리 이 PR에는 소비자가 생긴다 — Task 5가 `CanvasMainViewModel`에 주입한다. 그래서 `@Binds`를 빠뜨리면 `:app:assembleDebug`가 Hilt 그래프에서 잡는다.
>
> **소비자 없는 `clear()` 를 계약에 올리는 것은 PR2 선례의 예외다.** PR2는 "쓰지 않는 것을 올리면 계약이 바뀌어도 아무도 고치지 않는다"며 `place` 만 올렸다. 여기서 예외를 두는 근거는 ADR-0026 「위험·방어」가 **"덮어쓰기와 비우기를 단위 테스트로 고정한다. 이 결정의 안전성이 전부 그 두 규칙에 걸려 있다"**고 요구한 것이다. 부르는 곳은 PR5다.

- [ ] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToppingDraftRepositoryImplTest {
    private val toppingDraftLocalDataSource: ToppingDraftLocalDataSource = mockk(relaxUnitFun = true)

    /**
     * `draft` 는 구현의 생성자 초기화식이 곧바로 읽으므로 저장소를 만들기 전에 답이 있어야 한다.
     * `relaxUnitFun` 은 Unit 을 돌려주는 함수만 채워 주고 이 프로퍼티는 채우지 않는다.
     */
    @Before
    fun stubEmptyStore() {
        givenStoredDraft(null)
    }

    private fun repository() = ToppingDraftRepositoryImpl(toppingDraftLocalDataSource)

    private fun givenStoredDraft(draft: ToppingDraft?) {
        every { toppingDraftLocalDataSource.draft } returns flowOf(draft)
    }

    private fun draft(
        subjectImagePath: String?,
        cutoutImagePath: String?,
    ) = ToppingDraft(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        nextPositionZ = 4,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
    )

    @Test
    fun start_writesAFreshDraft_withNoImageOrBorder() = runTest {
        // Given 흐름에 들어선다
        val saved = slot<ToppingDraft>()
        coEvery { toppingDraftLocalDataSource.save(capture(saved)) } returns Unit

        // When 캔버스 식별값으로 흐름을 연다
        repository().start(groupId = GROUP_ID, parfaitId = PARFAIT_ID, nextPositionZ = 4)

        // Then 이미지와 테두리는 비어 있다 — 진입 시 덮어쓰는 이 규칙 하나가 지난 흐름의
        // 이미지가 따라붙는 것을 막는다
        assertEquals(
            ToppingDraft(groupId = GROUP_ID, parfaitId = PARFAIT_ID, nextPositionZ = 4),
            saved.captured,
        )
    }

    @Test
    fun draft_pathsPointToMissingFiles_areBlankedOut() = runTest {
        // Given 저장된 초안이 이미 지워진 캐시 파일을 가리킨다
        givenStoredDraft(
            draft(
                subjectImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/gone.png",
                cutoutImagePath = "/data/user/0/com.teamyg.parfait/cache/segmentation/gone-cutout.png",
            ),
        )

        // When 초안을 읽는다
        val read = repository().draft.first()

        // Then 경로를 흘리지 않는다 — 세그멘테이션 진입이 그 디렉토리를 통째로 비우므로
        // 그대로 두면 읽는 쪽이 있지도 않은 파일을 올리려 든다
        assertNull(read?.subjectImagePath)
        assertNull(read?.cutoutImagePath)
    }

    @Test
    fun draft_pathsPointToRealFiles_areKept() = runTest {
        // Given 파일이 아직 살아 있는 초안
        val subject = File.createTempFile("subject", ".png").apply { deleteOnExit() }
        val cutout = File.createTempFile("cutout", ".png").apply { deleteOnExit() }
        givenStoredDraft(
            draft(subjectImagePath = subject.absolutePath, cutoutImagePath = cutout.absolutePath),
        )

        // When 초안을 읽는다
        val read = repository().draft.first()

        // Then 살아 있는 경로까지 지우지 않는다
        assertEquals(subject.absolutePath, read?.subjectImagePath)
        assertEquals(cutout.absolutePath, read?.cutoutImagePath)
    }

    @Test
    fun draft_nothingStored_isNull() = runTest {
        // Given 흐름 밖이다
        givenStoredDraft(null)

        // Then 빈 초안을 지어내지 않는다
        assertNull(repository().draft.first())
    }

    @Test
    fun clear_delegatesToTheStore() = runTest {
        // Given, When 초안을 비운다
        repository().clear()

        // Then 저장소에서 지운다 — 지우지 않으면 다음 흐름의 진입 전까지 낡은 초안이 읽힌다
        coVerify(exactly = 1) { toppingDraftLocalDataSource.clear() }
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.repository.topping.ToppingDraftRepositoryImplTest"
```

Expected: 컴파일 실패 — `Unresolved reference: ToppingDraftRepositoryImpl`

- [ ] **Step 3: 도메인 계약을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt`:

```kotlin
package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow

interface ToppingDraftRepository {
    /** 흐름 밖이면 null 이다. 가리키던 캐시 파일이 사라진 경로는 비운 채로 흐른다 */
    val draft: Flow<ToppingDraft?>

    /**
     * 흐름을 연다. 이전 초안은 남기지 않고 통째로 덮어쓴다 — 낡은 초안이 다음 흐름에
     * 따라붙는 문제를 닫는 규칙이라 별도 만료·정리 경로를 두지 않는다
     * (`adr/0026-topping-draft-datastore-ssot.md`).
     */
    suspend fun start(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    )

    suspend fun clear()
}
```

- [ ] **Step 4: 구현을 만든다**

`data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt`:

```kotlin
package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class ToppingDraftRepositoryImpl @Inject constructor(
    private val toppingDraftLocalDataSource: ToppingDraftLocalDataSource,
) : ToppingDraftRepository {
    // 초안 하나가 앉은 DataStore 는 토큰·계정·최근 이미지도 함께 쓰는 파일이라, 남의 쓰기마다
    // 이 흐름이 다시 방출된다. 파일 확인이 수집자 디스패처에서 도는 것을 막고 같은 값의
    // 재방출을 걸러 낸다
    override val draft: Flow<ToppingDraft?> = toppingDraftLocalDataSource.draft
        .distinctUntilChanged()
        .map { draft -> draft?.withExistingFilesOnly() }
        .flowOn(Dispatchers.IO)

    override suspend fun start(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    ) = toppingDraftLocalDataSource.save(
        ToppingDraft(
            groupId = groupId,
            parfaitId = parfaitId,
            nextPositionZ = nextPositionZ,
        ),
    )

    override suspend fun clear() = toppingDraftLocalDataSource.clear()

    /**
     * 초안은 영속되지만 그것이 가리키는 것은 `cacheDir` 하위 파일이다. 세그멘테이션 진입이
     * 그 디렉토리를 비우고 OS 도 저장 공간이 모자라면 회수하므로, 사라진 경로는 처음부터
     * 없었던 것처럼 읽힌다(`specs/archive/2026-08-20-c106-topping-place-api.md` 초안 SSOT 절).
     */
    private fun ToppingDraft.withExistingFilesOnly(): ToppingDraft = copy(
        subjectImagePath = subjectImagePath?.takeIf { path -> File(path).isFile },
        cutoutImagePath = cutoutImagePath?.takeIf { path -> File(path).isFile },
    )
}
```

> **"빈 초안과 같이 취급한다"를 어디까지로 읽는가.** 스펙과 ADR-0026의 문구를 글자대로 읽으면 초안 전체를 `null` 로 봐야 하지만, 그러면 흐름 진입 때 못 박은 캔버스 식별값까지 잃어 ADR이 지키려던 "진입 캔버스가 못 박힌다"가 깨진다. 그래서 **비우는 것은 이미지 경로 둘뿐**이고 식별값과 테두리는 남긴다. 스펙 「토핑 초안 SSOT」의 문구도 이 해석으로 정정해 둔다 — 다음 라운드가 같은 자리에서 다시 흔들리지 않게 한다.

- [ ] **Step 5: DI 바인딩을 더한다**

`RepositoryModule.kt`의 import 블록 자리를 지켜 둘을 더한다. 알파벳 순으로 `ToppingDraft…` 가 `Topping…` 보다 앞이므로 **각각 `ToppingRepositoryImpl` 앞과 `ToppingRepository` 앞**이다:

```kotlin
import com.teamyg.parfait.data.repository.topping.ToppingDraftRepositoryImpl
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
```

인터페이스 본문 끝(`bindToppingRepository` 아래)에 바인딩을 더한다. **한 줄로 적는다** — 단일 라인이 114자라 120자 안에 들어가고, ktlint의 `function-signature` 는 파라미터가 하나면 그때 멀티라인을 허용하지 않는다(같은 파일의 `bindParfaitGroupRepository` 가 같은 길이로 한 줄이다):

```kotlin
    @Binds
    @Singleton
    fun bindToppingDraftRepository(toppingDraftRepositoryImpl: ToppingDraftRepositoryImpl): ToppingDraftRepository
```

- [ ] **Step 6: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
```

Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt
git commit -m "feat(topping): 초안 흐름 규칙을 ToppingDraftRepository 로 올린다

진입은 통째로 덮어쓰고, 사라진 캐시 경로는 비운 채로 읽힌다.
경로를 그대로 흘리면 읽는 쪽이 있지도 않은 파일을 올리려 든다."
```

---

### Task 3: 캔버스 메뉴 액션에 활성 여부를 싣는다

**Files:**
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcanvasmenu/YGCanvasMenuAction.kt`
- Modify: `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcanvasmenu/YGCanvasMenu.kt`

**Interfaces:**
- Consumes: `YGStrokeButton(text, onClick, modifier, iconResource, isSelected, isEnabled, borderWidth, interactionSource)` (기존)
- Produces: `YGCanvasMenuAction(text: String, iconResource: Int?, onClick: () -> Unit, isEnabled: Boolean = true)`

> ⚠️ **이 태스크만 자동 테스트가 없다.** `:core:designsystem`에는 호스트 단위 테스트 소스셋이 없고 기존 컴포넌트 테스트는 전부 `androidTest`(기기 필요)다. 새 소스셋을 이 PR에서 여는 것은 범위를 크게 넘는다. 대신 **판정 로직은 Task 5의 `CanvasMainViewModelTest`가 잠그고**, 이 태스크가 더하는 것은 그 판정을 화면까지 나르는 통로와 그 통로를 눈으로 볼 Preview 하나다. `YGStrokeButton`의 비활성 색·터치 차단은 이미 그 컴포넌트가 갖고 있어 여기서 새로 만드는 표현이 없다.

- [ ] **Step 1: 액션에 필드를 더한다**

`YGCanvasMenuAction.kt`:

```kotlin
@Immutable
data class YGCanvasMenuAction(
    val text: String,
    @DrawableRes val iconResource: Int?,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
)
```

- [ ] **Step 2: 두 버튼에 그 값을 전달한다**

`YGCanvasMenu.kt`의 `Row` 안 두 `YGStrokeButton` 호출에 각각 한 줄씩 더한다:

```kotlin
            YGStrokeButton(
                text = addAction.text,
                onClick = addAction.onClick,
                iconResource = addAction.iconResource,
                isEnabled = addAction.isEnabled,
                modifier = Modifier.weight(1f),
            )
            YGStrokeButton(
                text = editAction.text,
                onClick = editAction.onClick,
                iconResource = editAction.iconResource,
                isEnabled = editAction.isEnabled,
                modifier = Modifier.weight(1f),
            )
```

- [ ] **Step 3: 비활성 상태를 볼 Preview 를 더한다**

`YGCanvasMenu.kt`의 `YGCanvasMenuPreview` 안 `Column` 끝에 셋째 호출을 더한다:

```kotlin
        YGCanvasMenu(
            addAction = YGCanvasMenuAction(
                text = "토핑 추가",
                iconResource = R.drawable.ic_plus,
                onClick = {},
                isEnabled = false,
            ),
            editAction = YGCanvasMenuAction(
                text = "캔버스 편집",
                iconResource = R.drawable.ic_caret_right,
                onClick = {},
            ),
        )
```

- [ ] **Step 4: 컴파일과 포맷을 확인한다**

```bash
./gradlew :core:designsystem:assembleDebug ktlintCheck
```

Expected: PASS. 기존 호출부는 전부 이름 있는 인자를 쓰고 새 필드에 기본값이 있어 한 곳도 고칠 것이 없다.

- [ ] **Step 5: 커밋**

```bash
git add core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcanvasmenu/YGCanvasMenuAction.kt \
        core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygcanvasmenu/YGCanvasMenu.kt
git commit -m "feat(designsystem): 캔버스 메뉴 액션에 활성 여부를 싣는다

YGStrokeButton 이 이미 갖고 있던 비활성 표현으로 가는 통로만 연다."
```

---

### Task 4: C-001을 YGScaffoldV2로 옮기고 오늘 캔버스 조회 실패를 알린다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/navigation/EntryBuilder.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: `YGScaffoldV2(modifier, containerColor, contentWindowInsets, isLoading, toastPolicy, content)` · `rememberYGToastPolicy()` · `YGToastPolicy.showError(message)` (전부 기존)
- Produces: `CanvasMainEffect.ShowTodayCanvasError`(data object)

- [ ] **Step 1: 실패 테스트 작성**

`CanvasMainViewModelTest.kt`의 import 블록에 셋을 더한다:

```kotlin
import app.cash.turbine.test
import com.teamyg.parfait.domain.model.error.AppError
import kotlin.test.assertIs
```

그리고 클래스 안 아무 테스트 뒤에 둘을 더한다:

```kotlin
    @Test
    fun enter_todayCanvasFailsWithNothingOnScreen_tellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 한 번도 못 받은 화면
        coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = viewModel()

        viewModel.effect.test {
            // When 화면이 앞에 서면서 나간 조회가 실패한다
            viewModel.processIntent(CanvasMainIntent.Enter)
            advanceUntilIdle()

            // Then 빈 캔버스와 조회 실패가 구분되지 않으므로 따로 알린다. 알리지 않으면
            // 토핑 추가 버튼이 왜 안 눌리는지까지 보이지 않는다
            assertIs<CanvasMainEffect.ShowTodayCanvasError>(awaitItem())
        }
    }

    @Test
    fun enter_todayCanvasFailsAfterOneIsShown_staysQuiet() = runTest(mainDispatcherRule.dispatcher) {
        // Given 이미 오늘 캔버스를 그린 화면
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 돌아오면서 저절로 나간 재조회가 실패한다
            coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
            viewModel.processIntent(CanvasMainIntent.Enter)
            advanceUntilIdle()

            // Then 화면이 앞에 설 때마다 재조회하므로 매번 알리면 방해가 된다. 보여 줄
            // 캔버스가 남아 있으면 조용히 넘어간다
            expectNoEvents()
        }
        assertTrue(viewModel.state.value.todayCanvas != null)
    }
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModelTest"
```

Expected: 컴파일 실패 — `Unresolved reference: ShowTodayCanvasError`

- [ ] **Step 3: 이펙트를 더하고 실패를 알린다**

> ⚠️ **이 단계와 아래 Step 5(Route 교체)는 한 덩어리다.** 이펙트 arm 을 더하는 순간
> `CanvasMainRoute` 의 `when (effect)` 가 비망라 `when` 문이 되어 **컴파일이 깨진다**(Kotlin 은
> sealed 를 주제로 한 `when` 문에 모든 arm 을 요구한다). 그래서 이 태스크는 Step 3~6을 다 마친
> 뒤에야 테스트가 돈다. 중간에 검증을 시도하지 말 것.

`CanvasMainViewModel.kt`의 `CanvasMainEffect` 끝에 하나를 더한다:

```kotlin
    /**
     * 오늘 캔버스를 못 받았고 보여 줄 것도 없을 때만 온다. 화면이 앞에 설 때마다 재조회하므로
     * 매번 알리면 방해가 된다(`specs/archive/2026-08-20-c106-topping-place-api.md` C-001 절).
     */
    data object ShowTodayCanvasError : CanvasMainEffect
```

`loadTodayCanvas`의 `onFailure`를 고친다:

```kotlin
                }.onFailure { throwable ->
                    viewModelLogger.e(throwable) { "오늘 캔버스를 불러오지 못했다 - groupId: ${groupId.value}" }
                    if (state.value.todayCanvas == null) {
                        postSideEffect(CanvasMainEffect.ShowTodayCanvasError)
                    }
                }
```

- [ ] **Step 4: 실패 문구를 더한다**

`feature/groups/canvas/impl/src/main/res/values/strings.xml`의 `canvas_main_member_overflow_count` 아래에 한 줄을 더한다:

```xml
    <string name="canvas_main_today_canvas_error">오늘의 캔버스를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</string>
```

- [ ] **Step 5: Route가 스캐폴드를 소유하고 새 이펙트를 받게 한다**

`CanvasMainRoute.kt`를 아래로 바꾼다. 바뀌는 것은 **토스트 정책을 만들어 `YGScaffoldV2`에 넘기는 것과, `modifier`가 스캐폴드로 가고 화면이 인셋 패딩을 받는 것** 둘이다:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainIntent
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting

@Composable
internal fun CanvasMainRoute(
    groupId: Long,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasMainViewModel = hiltViewModel(
        creationCallback = { factory: CanvasMainViewModel.Factory ->
            factory.create(groupIdValue = groupId)
        },
    ),
) {
    val canvasState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val todayCanvasErrorMessage = stringResource(R.string.canvas_main_today_canvas_error)

    // 백스택 아래에 깔린 엔트리는 컴포지션에서 빠지므로 다시 앞에 설 때 한 번 더 돈다.
    // 매번 다시 묻는 이유는 CanvasMainIntent.Enter 에 있다
    LifecycleResumeEffect(viewModel) {
        viewModel.processIntent(CanvasMainIntent.Enter)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CanvasMainEffect.NavigateToCamera -> navigator.goTo(
                    destination = NavKeyCameraCustom(),
                )

                is CanvasMainEffect.NavigateToCanvas -> navigator.goTo(
                    destination = NavKeyCustomGalleryPicker(),
                )

                is CanvasMainEffect.NavigateToCanvasBGEdit -> navigator.goTo(
                    destination = NavKeyCanvasBGEdit,
                )

                is CanvasMainEffect.NavigateToGroupSetting -> navigator.goTo(
                    destination = NavKeyGroupSetting(groupId = effect.groupId.value),
                )

                is CanvasMainEffect.ShowTodayCanvasError -> toastPolicy.showError(todayCanvasErrorMessage)
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        CanvasMainScreen(
            canvasState = canvasState,
            onClickBack = { navigator.onBack() },
            onClickDateSelect = { viewModel.processIntent(CanvasMainIntent.OnClickDateSelect) },
            onClickMenu = { viewModel.processIntent(CanvasMainIntent.OnClickGroupSetting) },
            onClickCamera = { viewModel.processIntent(CanvasMainIntent.OnClickCamera()) },
            onClickGallery = { viewModel.processIntent(CanvasMainIntent.OnClickCanvas()) },
            onClickEditCanvasBG = { viewModel.processIntent(CanvasMainIntent.OnClickCanvasEdit()) },
            onClickSaveToGallery = { viewModel.processIntent(CanvasMainIntent.OnClickSaveToGallery) },
            onClickGoToToday = { viewModel.processIntent(CanvasMainIntent.OnClickGoToToday) },
            onDismissCalendar = { viewModel.processIntent(CanvasMainIntent.DismissCalendar) },
            onSelectYear = { viewModel.processIntent(CanvasMainIntent.SelectYear(it)) },
            onSelectMonth = { viewModel.processIntent(CanvasMainIntent.SelectMonth(it)) },
            onClickDate = { viewModel.processIntent(CanvasMainIntent.ClickDate(it)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
```

- [ ] **Step 6: EntryBuilder에서 구판 스캐폴드를 걷는다**

`EntryBuilder.kt`의 `NavKeyCanvasMain` 엔트리 **하나만** 바꾼다. 나머지 엔트리는 아직 구판을 쓰므로 `YGScaffold` import 는 남는다:

```kotlin
    entry<NavKeyCanvasMain> { navKey ->
        CanvasMainRoute(
            groupId = navKey.groupId,
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
```

`padding` import 는 나머지 엔트리 넷(`NavKeyCanvasBGEdit`·`NavKeyCanvasEdit`·`NavKeyCanvasImageSelect`·`NavKeyCanvasMove`)이 계속 쓰므로 지우지 않는다. `YGScaffold` import 도 같은 이유로 남는다.

- [ ] **Step 7: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
```

Expected: PASS. Step 3~6을 다 마친 지금이 이 태스크에서 컴파일이 성립하는 첫 지점이다.

- [ ] **Step 8: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/navigation/EntryBuilder.kt \
        feature/groups/canvas/impl/src/main/res/values/strings.xml \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt
git commit -m "feat(canvas): C-001 을 YGScaffoldV2 로 옮기고 조회 실패를 알린다

지금까지 오늘 캔버스 조회 실패는 로그만 남아 사용자가 빈 캔버스와 구분하지 못했다.
매번 알리지는 않는다 - 화면이 앞에 설 때마다 재조회하므로, 보여 줄 캔버스가
없을 때만 토스트를 띄운다."
```

---

### Task 5: 흐름 진입 시 초안을 쓰고 토핑 추가를 가드한다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMainScreen.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt`

**Interfaces:**
- Consumes: `ToppingDraftRepository.start(groupId: GroupId, parfaitId: ParfaitId, nextPositionZ: Int)` (Task 2) · `YGCanvasMenuAction(..., isEnabled)` (Task 3)
- Produces:
  - `CanvasMainUiState.isToppingAddEnabled: Boolean`
  - `CanvasMainEffect.ShowToppingFlowStartError`(data object)

- [ ] **Step 1: 실패 테스트 작성**

`CanvasMainViewModelTest.kt`의 import 블록에 아래를 더한다. `coVerify`·`assertTrue`·`GroupMemberId`·`GroupNickname`·`ParfaitId` 는 이미 있으므로 다시 넣지 않는다(중복 import 는 컴파일 오류다):

```kotlin
import com.teamyg.parfait.domain.model.canvas.CanvasToppingVO
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.datetime.LocalDateTime
import java.io.IOException
import kotlin.test.assertFalse
```

Task 4가 이미 더한 `app.cash.turbine.test`·`AppError`·`assertIs` 도 여기서 그대로 쓴다.

`viewModel()` 헬퍼와 mock 을 고친다:

```kotlin
    private val toppingDraftRepository: ToppingDraftRepository = mockk(relaxUnitFun = true)
```

```kotlin
    private fun viewModel() = CanvasMainViewModel(
        groupIdValue = GROUP_ID,
        getParfaitHistoriesUseCase = getParfaitHistories,
        getParfaitYearsUseCase = getParfaitYears,
        getTodayParfaitUseCase = getTodayParfait,
        getParfaitDetailUseCase = getParfaitDetail,
        getMyGroupsFlowUseCase = getMyGroupsFlow,
        refreshMyGroupsUseCase = refreshMyGroups,
        toppingDraftRepository = toppingDraftRepository,
    )
```

그리고 테스트 다섯을 더한다:

```kotlin
    @Test
    fun clickCamera_opensTheFlowWithTheCanvasItEnteredFrom() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 그린 화면
        val viewModel = enteredViewModel()

        // When 카메라로 떠난다
        viewModel.processIntent(CanvasMainIntent.OnClickCamera())
        advanceUntilIdle()

        // Then 진입 시점의 캔버스가 초안에 못 박힌다 — 도중에 하루 경계를 넘어도 다른 캔버스로
        // 조용히 옮겨 가지 않는다
        coVerify(exactly = 1) {
            toppingDraftRepository.start(
                groupId = GroupId(GROUP_ID),
                parfaitId = ParfaitId(TODAY_PARFAIT_ID),
                nextPositionZ = any(),
            )
        }
    }

    @Test
    fun clickGallery_opensTheFlowToo() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 그린 화면
        val viewModel = enteredViewModel()

        // When 갤러리로 떠난다
        viewModel.processIntent(CanvasMainIntent.OnClickCanvas())
        advanceUntilIdle()

        // Then 카메라와 같은 흐름이라 초안도 같이 열린다
        coVerify(exactly = 1) { toppingDraftRepository.start(any(), any(), any()) }
    }

    @Test
    fun clickCamera_stacksTheNewToppingOnTop() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑이 z 3 과 7 로 놓여 있는 오늘 캔버스
        coEvery { getTodayParfait(any()) } returns Result.success(
            canvas(TODAY_PARFAIT_ID, today).copy(toppings = listOf(topping(3), topping(7))),
        )
        val viewModel = enteredViewModel()

        // When 카메라로 떠난다
        viewModel.processIntent(CanvasMainIntent.OnClickCamera())
        advanceUntilIdle()

        // Then 맨 위 z 보다 하나 크다. 목록 크기로 세면 지워진 토핑이 있는 캔버스에서 겹친다
        coVerify(exactly = 1) {
            toppingDraftRepository.start(any(), any(), nextPositionZ = 8)
        }
    }

    @Test
    fun clickCamera_draftWriteFails_staysOnTheCanvasAndTellsTheUser() = runTest(mainDispatcherRule.dispatcher) {
        // Given 초안을 쓸 수 없는 상태
        coEvery { toppingDraftRepository.start(any(), any(), any()) } throws IOException("no space")
        val viewModel = enteredViewModel()

        viewModel.effect.test {
            // When 카메라로 떠나려 한다
            viewModel.processIntent(CanvasMainIntent.OnClickCamera())
            advanceUntilIdle()

            // Then 화면을 옮기지 않는다 — 초안 없이 흐름에 들어가면 촬영·누끼·편집을 다 마친
            // 뒤에야 올릴 데가 없다는 것을 알게 된다
            assertIs<CanvasMainEffect.ShowToppingFlowStartError>(awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun clickCamera_withoutTodayCanvas_doesNotOpenTheFlow() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 못 받아 버튼이 잠긴 화면
        coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
        val viewModel = enteredViewModel()

        // When 그래도 의도가 들어온다(가드가 뚫렸거나 화면 밖에서 왔다)
        viewModel.processIntent(CanvasMainIntent.OnClickCamera())
        advanceUntilIdle()

        // Then 캔버스 식별값 없이 초안을 열지 않는다 — 그 초안으로는 올릴 데를 정할 수 없다
        coVerify(exactly = 0) { toppingDraftRepository.start(any(), any(), any()) }
    }

    @Test
    fun toppingAdd_isEnabledOnlyWhenTodayCanvasIsInHand() = runTest(mainDispatcherRule.dispatcher) {
        // Given 오늘 캔버스를 못 받은 화면
        coEvery { getTodayParfait(any()) } returns Result.failure(AppError.Network(cause = null))
        val failed = enteredViewModel()

        // Then 올릴 데가 없으므로 잠근다
        assertFalse(failed.state.value.isToppingAddEnabled)

        // Given, When 캔버스를 받은 화면
        coEvery { getTodayParfait(any()) } returns Result.success(canvas(TODAY_PARFAIT_ID, today))
        val loaded = enteredViewModel()

        // Then 열어 준다
        assertTrue(loaded.state.value.isToppingAddEnabled)
    }
```

이 파일의 픽스처 헬퍼(`member`·`canvas`)는 **클래스 맨 아래 `private companion object` 안**에 산다. 토핑 픽스처도 그 옆, `canvas(...)` 다음에 더한다:

```kotlin
        fun topping(positionZ: Int) = CanvasToppingVO(
            parfaitImageId = ParfaitImageId(positionZ.toLong()),
            imageId = ImageId(positionZ.toLong()),
            imageUrl = "https://cdn.example.com/topping-$positionZ.png",
            transform = ToppingTransform(
                positionX = 0.5,
                positionY = 0.5,
                positionZ = positionZ,
                scale = 1.0,
                rotation = 0.0,
            ),
            border = ToppingBorder.None,
            placedBy = ToppingPlacerVO(
                groupMemberId = GroupMemberId(1L),
                nickname = GroupNickname("연경이"),
            ),
            createdAt = LocalDateTime(2026, 8, 20, 12, 0),
        )
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModelTest"
```

Expected: 컴파일 실패 — `viewModel()` 헬퍼가 이름 있는 인자로 넘기므로 먼저 `Cannot find a parameter with this name: toppingDraftRepository` 가 나오고, 이어서 `Unresolved reference: isToppingAddEnabled` 가 나온다

- [ ] **Step 3: 상태에 활성 판정을 더한다**

`CanvasMainViewModel.kt`의 `CanvasMainUiState` 안, `isViewingToday` 아래에 더한다:

```kotlin
    /**
     * 캔버스를 아직 못 받았으면 parfaitId 도 nextPositionZ 도 없다. 열어 두면 촬영·누끼·편집을
     * 다 마친 뒤에야 올릴 데가 없다는 것을 알게 된다
     * (`adr/0026-topping-draft-datastore-ssot.md`).
     */
    val isToppingAddEnabled: Boolean
        get() = isViewingToday && todayCanvas != null
```

> `isViewingToday` 항은 지금 이 값을 읽는 유일한 자리(`CanvasMainScreen` 의 `addAction`)가 이미 그 분기 안이라 중복이다. 그래도 남기는 이유는 판정의 정본이 스펙 「표시·제어 규칙」이고 거기가 두 조건을 함께 적었기 때문이다 — 화면 쪽 분기가 바뀌어도 판정이 따라 흔들리지 않는다.

- [ ] **Step 4: 흐름 진입에서 초안을 쓴다**

`CanvasMainEffect` 끝에 하나를 더한다:

```kotlin
    data object ShowToppingFlowStartError : CanvasMainEffect
```

생성자에 의존을 더한다(`refreshMyGroupsUseCase` 다음 줄):

```kotlin
    private val toppingDraftRepository: ToppingDraftRepository,
```

import 하나를 더한다. 초안 타입을 직접 짓지 않으므로 `ToppingDraft` 는 넣지 않는다 — 안 쓰는 import 는 ktlint 의 `no-unused-imports` 가 잡는다:

```kotlin
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
```

`handleOnClickCamera`·`handleOnClickCanvas` 를 바꾸고 그 아래에 흐름 열기를 더한다:

```kotlin
    private fun handleOnClickCamera() {
        startToppingFlow(effect = CanvasMainEffect.NavigateToCamera())
    }

    private fun handleOnClickCanvas() {
        startToppingFlow(effect = CanvasMainEffect.NavigateToCanvas())
    }

    /**
     * 흐름에 들어서는 순간 초안을 새로 쓴다. 그 뒤에야 화면을 옮긴다 — 초안 없이 들어가면
     * 촬영·누끼·편집을 다 마친 뒤에야 올릴 데가 없다는 것을 알게 된다.
     */
    private fun startToppingFlow(effect: CanvasMainEffect) {
        val canvas = state.value.todayCanvas ?: return

        launch(
            key = START_TOPPING_FLOW_KEY,
            onError = { error ->
                viewModelLogger.e { "토핑 초안을 쓰지 못했다 - $error" }
                postSideEffect(CanvasMainEffect.ShowToppingFlowStartError)
            },
        ) {
            toppingDraftRepository.start(
                groupId = groupId,
                parfaitId = canvas.parfaitId,
                nextPositionZ = canvas.nextPositionZ(),
            )
            postSideEffect(effect)
        }
    }
```

z 산출을 클래스 안 private 확장으로 더한다 — `movedToNearestMonth` 가 이미 그 자리에 같은 형태로 있다:

```kotlin
    /** 새 토핑은 언제나 맨 위다. 목록 크기로 세면 지워진 토핑이 있는 캔버스에서 z 가 겹친다 */
    private fun CanvasVO.nextPositionZ(): Int = (toppings.maxOfOrNull { it.transform.positionZ } ?: 0) + 1
```

`companion object` 에 키를 더한다:

```kotlin
        const val START_TOPPING_FLOW_KEY = "startToppingFlow"
```

- [ ] **Step 5: 화면과 Route를 잇는다**

`CanvasMainScreen.kt`의 `addAction` 중 **오늘을 보고 있을 때의 분기**에 한 줄을 더한다:

```kotlin
            addAction = if (canvasState.isViewingToday) {
                YGCanvasMenuAction(
                    text = stringResource(R.string.canvas_main_topping_add),
                    iconResource = DesignSystemR.drawable.ic_plus,
                    onClick = openMenu,
                    isEnabled = canvasState.isToppingAddEnabled,
                )
            } else {
```

> ⚠️ **이 한 줄에는 자동 감지선이 없다.** Task 5의 테스트가 잠그는 것은 `isToppingAddEnabled` 의 값뿐이라, `isEnabled = …` 를 빠뜨려도 이 PR의 테스트 18건이 전부 초록이다. 상태에서 화면까지의 배선을 확인하는 유일한 수단은 아래 「완료 조건」의 **실기기 확인 1번**이다. 눈으로 반드시 밟는다.

`strings.xml` 에 문구 하나를 더한다:

```xml
    <string name="canvas_main_topping_flow_start_error">토핑을 시작하지 못했어요. 잠시 후 다시 시도해 주세요.</string>
```

`CanvasMainRoute.kt` 에 문구를 읽어 두고(다른 `stringResource` 옆) `when` 에 arm 하나를 더한다:

```kotlin
    val toppingFlowStartErrorMessage = stringResource(R.string.canvas_main_topping_flow_start_error)
```

```kotlin
                is CanvasMainEffect.ShowToppingFlowStartError ->
                    toastPolicy.showError(toppingFlowStartErrorMessage)
```

- [ ] **Step 6: 전체 검증**

```bash
./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck :app:assembleDebug
```

Expected: 전부 PASS. `:app:assembleDebug` 가 Task 2의 `@Binds` 누락까지 함께 잡는다 — 이 태스크가 `ToppingDraftRepository` 의 첫 소비자다.

- [ ] **Step 7: 커밋**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModel.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasMainScreen.kt \
        feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasMainRoute.kt \
        feature/groups/canvas/impl/src/main/res/values/strings.xml \
        feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasMainViewModelTest.kt
git commit -m "feat(canvas): 흐름 진입에서 토핑 초안을 쓰고 추가 버튼을 가드한다

진입 시점의 캔버스를 초안에 못 박는다 - 도중에 하루 경계를 넘어도 다른 캔버스로
옮겨 가지 않는다. 캔버스를 아직 못 받았으면 버튼을 잠근다."
```

---

## 완료 조건

- `./gradlew :domain:test :data:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck :app:assembleDebug` 전부 통과
- 신규 테스트 **18건**(Task 1: 5 · Task 2: 5 · Task 4: 2 · Task 5: 6)
- 커밋 5개, push·PR 없음
- 기존 테스트 중 `CanvasMainViewModelTest` 의 생성자 헬퍼 한 곳만 고쳐진다. 나머지 기존 테스트는 손대지 않는다

**실기기 확인 항목**(이 라운드가 처음으로 사용자에게 보이는 변화를 낸다):

1. 비행기 모드로 C-001에 처음 들어가면 **토스트가 한 번** 뜨고 토핑 추가 버튼이 회색으로 잠긴다.
2. 통신을 켜고 화면을 다시 앞에 세우면 캔버스가 그려지고 버튼이 풀린다.
3. 캔버스를 받은 뒤 다시 비행기 모드로 오가며 재진입해도 **토스트가 반복되지 않는다**(이미 그린 캔버스가 남아 있다).
4. 지난 날짜를 보는 동안에는 지금까지처럼 저장·오늘로 가기 버튼이 그대로 나온다.
5. 토핑 추가 → 카메라·갤러리로 들어갔다가 뒤로 돌아와도 화면 동작이 이전과 같다(초안은 아직 읽는 곳이 없어 눈에 보이는 차이가 없다).

## 이 PR에서 하지 않는 것

- **초안에 이미지·테두리를 채우는 것**(PR4) — 모델에 자리만 만든다. `SegmentationConfirmRoute` 의 `rememberSaveable` 셋을 걷는 것도 PR4다
- **초안을 읽어 배치하는 것**(PR5) — 좌표 변환·로딩 오버레이·되감기 판정·성공 시 초안 비우기가 전부 그 라운드다. `ToppingDraftRepository.clear()` 는 계약만 서 있고 부르는 곳이 아직 없다
- **테두리 계약 전환·굽기 중단·종횡비 상수 통일**(PR4)
- **C-106 확인 버튼 가드**(PR5) — 스펙 「표시·제어 규칙」 표는 두 행이고 이 PR이 여는 것은 **첫째 행(C-001 토핑 추가 버튼)뿐**이다. 둘째 행의 판정 근거(토핑 이미지 painter 가 `Success` 인지)는 초안을 읽어 그리는 화면이 선 뒤에야 성립한다
- **`NavKeyCanvasToppingPlace` 인자 제거**(PR4) — 초안이 정본이 되는 것은 그 라운드부터다
- **`TOPPING_EDIT_RESULT_KEY` 정리** — 걷지 않는다. 소비자가 둘이라 걷으면 `CanvasBGEditRoute` 가 컴파일은 통과한 채 조용히 죽는다
- **나머지 캔버스 화면의 `YGScaffoldV2` 이관** — `EntryBuilder` 의 다른 엔트리는 구판을 그대로 쓴다. 이관은 화면별 API 결선 라운드에 묶어 점진 진행한다는 것이 [ygscaffold-v2 스펙](../../specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)의 결정이다
- **C-001 로딩 오버레이** — `YGScaffoldV2` 의 `isLoading` 은 넘기지 않는다. 오늘 캔버스 조회는 화면을 막을 만큼 긴 작업이 아니고, 이 라운드가 여는 것은 실패 표현이다
- **PR5 선행 미결 둘**(OQ-P-109 발급 응답 본문 로깅 · OQ-P-246 업로드가 코루틴 취소를 안 따라감) — 스펙 PR 표 5번 행이 정본이다
