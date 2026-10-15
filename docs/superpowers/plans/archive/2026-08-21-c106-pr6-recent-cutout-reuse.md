---
id: c106-pr6-recent-cutout-reuse
title: C-106 결선 PR6 — 누끼 알맹이 재사용 (최근 목록 종류 축·확인 화면 직행)
status: done
type: work-order
created: 2026-08-21
updated: 2026-08-21
archived_reason: 구현 완료·develop 머지(2026-08-22, PR #334 19cb5299). 브랜치 feature/#270-recent-cutout-reuse 에 커밋 10개(67ee0d6a..656cbf2e), 신규 테스트 24건, 30파일 964/145. 실행이 계획과 갈린 자리 셋은 plans/README 행과 스펙 「구현이 이 절과 갈린 자리」 절에 있다.
platforms: android
owner: Parfait 팀
related_adr: ADR-0025, ADR-0026
related_spec: c106-topping-place-api, c103-segmentation-topping-edit
related_code:
  - RecentImageLocalDataSourceImpl.kt#decode
  - FileRecentImageLocalDataSourceImpl.kt#readBytes
  - RecentImageRepositoryImpl.kt#storeRecentImageInInternalStorage
  - AddRecentImageUseCase.kt#invoke
  - GetRecentCacheImagesUseCase.kt#clearOutsideDayWindow
  - CanvasToppingPlaceViewModel.kt#handleOnClickConfirm
  - CustomGalleryPickerViewModel.kt#CustomGalleryPickerState
  - GalleryImageGridComponent.kt#GalleryImageGridComponent
  - NavKeySegmentationConfirm.kt#NavKeySegmentationConfirm
  - SegmentationConfirmViewModel.kt#observeDraft
  - ToppingDraftRepository.kt#record
tags: [plan, parfait, topping, gallery, segmentation, c-106]
---

# C-106 결선 PR6 — 누끼 알맹이 재사용 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 배치에 성공한 **테두리 없는 트리밍 알맹이**를 갤러리 "최근"에 남기고, 그것을 고르면 카메라·세그멘테이션을 건너뛰어 **누끼 확인 화면(C-103)으로 직행**하게 한다.

**Architecture:** 최근 이미지 계층은 지금 `List<String>` 하나로 원본 사진만 다루고, 그 계층 전체가 **테스트가 한 건도 없다.** 그래서 이 라운드는 종류 축을 얹기 전에 **잡을 자리를 먼저 만든다** — 파일 datasource가 `android.net.Uri`를 안으로 삼켜 `RecentImageRepositoryImpl`을 순수 JVM 테스트 대상으로 만들고(Task 2), 그 위에 종류 축을 관통시킨다(Task 3). 목록 스키마 확장은 **2단 폴백 디코드**가 지켜 준다 — 신 스키마가 실패하면 구 `List<String>`으로 한 번 더 시도하므로 기존 목록이 통째로 날아가지 않는다. 화면 쪽에서 새로 만드는 것은 하나뿐이다: 재사용 진입은 세그멘테이션을 타지 않아 **초안에 알맹이를 적는 주체가 없으므로**, 확인 화면이 스스로 `record`를 먼저 마친 뒤 구독을 연다.

**Tech Stack:** Kotlin · Hilt · Jetpack Compose · Coil3 · DataStore Preferences · kotlinx.serialization · kotlinx-coroutines-test · MockK · Turbine · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-20-c106-topping-place-api.md`](../../specs/archive/2026-08-20-c106-topping-place-api.md) — 「누끼 알맹이 재사용 (PR6)」 절, PR 분할 표 **6번 행**

> **베이스는 PR5 브랜치의 현재 팁이다.** `feature/#270-topping-place-wiring`(**미머지**, 검수 시점
> 팁 `67ee0d6a` — 그 브랜치가 계획서보다 나중에도 자랄 수 있으므로 **작업 직전에 팁을 다시 확인한다**).
> 그 아래로 PR4 `feature/#270-topping-border-contract` → PR3 `feature/#270-topping-draft-ssot`가
> 깔려 있고, PR1·PR2는 develop에 머지됐다(`da03c9b0`).
> 새 브랜치 `feature/#270-recent-cutout-reuse`를 PR5 팁 위에 만든다.
> 🔁 이 문서의 스택 해시는 **2026-08-22 리베이스 뒤 기준**이다 — 스택 넷을 develop `ef55a58c` 위로
> 다시 쌓았고 기록은 [PR3 계획서](2026-08-20-c106-pr3-topping-draft-ssot.md)에 있다.

> ✅ **2026-08-22 develop 머지**(PR #334 `19cb5299`) — 스택 넷이 머지 커밋 하나로 함께 들어왔다.
> 머지 트리가 PR6 브랜치 팁 `656cbf2e`와 같아 충돌 해소 편집이 0건이고, 위 as-built 수치는
> 재측정 없이 그대로 develop 사실이다.

> 🔁 **이 계획의 결정 하나가 뒤집혔다(2026-08-31, 이슈 #424 — 브랜치 `feature/#424-topping-border`, 미머지)**
> — 아래 「사용자에게 보이는 변화」 3번과 「설계 결정」 1번의 **"재사용 항목에서 사진 편집이 잠긴다"**가
> 더는 사실이 아니다. 잠그는 대신 `NavKeyToppingEdit(borderOnly = true)`로 **테두리 편집만** 연다.
> 저장 대상을 알맹이 1장으로 좁힌 근거(용량 3배 vs `MAX_SIZE = 9`)와 영역 편집이 불가능하다는 사실은
> 그대로다 — 되살릴 원본이 없다는 조건에서 무엇을 열어 줄지가 바뀌었을 뿐이다.

## 사용자에게 보이는 변화 (예고)

1. **토핑을 한 번 올리면 그 알맹이가 갤러리 "최근 업로드"에 남는다.** 다음에 그것을 고르면
   누끼를 다시 따지 않고 곧장 확인 화면으로 간다.
2. **알맹이 셀은 잘리지 않는다.** 원본 사진 셀은 지금처럼 `Crop`이고 알맹이만 `Fit`이다.
   종류를 알리는 뱃지·배경 구분은 두지 않는다(시안 없음, OQ-P-257).
3. **재사용 항목에서는 "사진 편집"이 비활성이다.** 원본과 재편집 마스크를 저장하지 않기 때문이다.
4. **배경 선택(C-301) 경로의 갤러리에는 알맹이가 보이지 않는다.**

## 설계 결정 (실행 전에 읽는다)

1. **저장 대상은 알맹이 1장뿐이다.** 원본·마스크까지 두면 내부 저장소 사용량이 3배가 되면서
   상한 `MAX_SIZE = 9`와 정면으로 부딪힌다. 그 대가로 재사용 항목에서 "사진 편집"이 잠긴다 —
   다시 편집하려면 갤러리의 원본에서 새로 시작한다.
2. **종류 축은 목록 하나를 넓혀서 만든다.** 목록을 둘로 가르면 상한 9가 목록마다 따로 걸려
   최대 18장이 되고 시간순 병합이 이중으로 생긴다.
3. **`android.net.Uri`를 파일 datasource 안으로 삼킨다.** 지금 `RecentImageRepositoryImpl`이
   `toUri()`·`Uri.toString()`을 직접 불러 **JVM 단위 테스트로 잡을 수 없다.** 이 라운드가
   그 계층의 동작을 바꾸므로 잡을 자리를 함께 만든다. 동작 변화는 없다(Task 2).
4. **저장 실패는 삼키고 로그만 남긴다.** 배치는 이미 성공했다. 재사용 편의 하나 때문에 성공한
   흐름을 실패로 보이게 하지 않는다.
5. **재사용 진입 판정은 `cutoutImagePath == null`이다.** 정상 흐름은 세그멘테이션·편집이 초안을
   채운 뒤 이 화면에 오므로 재편집 마스크를 언제나 들고 온다. 재사용만 그것이 없다.

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**이고 이 문서가 사는 저장소가 아니다. 로컬 절대경로는
  `wiki/personal-private/project-paths.md`에 있다(Task 7만 이 문서 저장소에서 한다).
- **베이스 브랜치는 `feature/#270-topping-place-wiring`의 현재 팁**(PR5, 미머지)이다. 검수 시점 팁은
  `67ee0d6a`이고, 브랜치할 때 `git log -1`로 다시 확인한다.
  그 위에 새 브랜치 `feature/#270-recent-cutout-reuse`를 만들어 작업한다.
- **워크트리를 만들지 않는다.** 본 체크아웃에서 브랜치로 작업한다.
- **커밋은 태스크마다 한다.** `git push`·`gh pr create`·`gh pr merge`는 **하지 않는다** —
  사용자 확인이 필요한 작업이다.
- ⚠️ **새 ViewModel 테스트는 `runTest(mainDispatcherRule.dispatcher)`로 연다.**
  `MainDispatcherRule`의 KDoc이 이유를 못 박아 두었다 — 인자 없이 부르면 스케줄러가 갈려
  `advanceUntilIdle()`이 Main 큐를 못 비운다.
- ⚠️ **ktlint가 미사용 import를 실패로 잡는다**(`ktlint_standard_no-unused-imports`). 이 계획은
  태스크마다 추가·삭제할 import를 명시한다. 건너뛰면 `ktlintCheck`에서 멈춘다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 선언에 멀티라인을 강제한다**(`.editorconfig`). 코드
  블록을 한 줄로 줄이지 말 것. 최대 줄 길이는 120이다.
- ⚠️ **컴포저블 시그니처를 바꾸면 `@YGPreview` 호출부도 함께 고친다.** PR5에서 이것을 빠뜨려
  컴파일이 깨질 뻔했다.
- **주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 근거는 문서를 가리킨다.
    함정과 의도는 쓴다.
  - 아키텍처 결정 설명을 코드에 복사하지 않는다. 포인터 한 줄만 둔다.
- **초안이 담는 이미지 경로는 파일 시스템 절대경로**다. `file://` uri가 아니다.
- **`domain`은 직렬화를 모른다.** 저장 형태는 `data/model/local/`의 `@Serializable` 엔티티가 맡고
  도메인 모델과 매퍼로 잇는다(`ToppingDraftEntity`가 그 본보기다).
- 매퍼 단독 테스트(`XxxVOMapperTest`)는 만들지 않는다. 판단이 든 변환은 DataSource·Repository
  테스트의 케이스로 넣는다.

## 파일 구성

| 자리 | 역할 | Task |
|---|---|---|
| `domain/model/image/RecentImage.kt` | 최근 이미지 한 항목(uri·절대경로·종류)과 `RecentImageKind` | 1 |
| `data/model/local/RecentImageEntity.kt` | 저장 형태(uri·종류)와 매퍼 | 1 |
| `data/source/image/local/RecentImageLocalDataSource(.kt/Impl.kt)` | 목록 스키마 확장·2단 폴백 디코드 | 1 |
| `data/source/file/local/FileRecentImageLocalDataSource(.kt/Impl.kt)` | 절대경로 읽기·확장자 인자화·`Uri` 삼키기 | 2 |
| `data/repository/image/RecentImageRepositoryImpl.kt` | 안드로이드 타입을 안 만지는 조율자 | 2·3 |
| `domain/repository/image/RecentImageRepository.kt` | 위 계약 | 2·3 |
| `domain/usecase/image/AddRecentImageUseCase.kt` | 종류를 받아 저장 | 3 |
| `domain/usecase/image/GetRecentCacheImagesUseCase.kt` | 종류를 실은 목록·데이 윈도우 정리 | 3 |
| `feature/groups/canvas/impl/.../CanvasToppingPlaceViewModel.kt` | 배치 성공 시 알맹이 저장(초안 비우기 전) | 4 |
| `feature/gallery/impl/.../CustomGalleryPickerViewModel.kt` | 진입 경로별 알맹이 노출·클릭 분기 | 5 |
| `feature/gallery/impl/.../GalleryImageGridComponent.kt` | 알맹이 셀 `Fit` | 5 |
| `feature/gallery/impl/build.gradle.kts` | 단위 테스트 플러그인 | 5 |
| `feature/segmentation/api/NavKeySegmentationConfirm.kt` | 원본·마스크 인자 완화 | 6 |
| `feature/segmentation/impl/.../SegmentationConfirmViewModel.kt` | 재사용 진입 시 `record` 선행 | 6 |
| `feature/segmentation/impl/.../SegmentationConfirmScreen.kt` | 편집 버튼 비활성 | 6 |

---

## Task 1: 최근 목록에 종류 축을 얹고 구 스키마를 폴백으로 살린다

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/image/RecentImage.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/model/local/RecentImageEntity.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/image/local/RecentImageLocalDataSource.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/image/local/RecentImageLocalDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt` (컴파일 유지용 최소 수정)
- Test: `data/src/test/java/com/teamyg/parfait/data/source/image/local/RecentImageLocalDataSourceImplTest.kt` (신규)

**Interfaces:**
- Consumes: `FakePreferencesDataStore`(`data/src/test/.../datastore/`), `@LocalJson Json`
- Produces:
  - `domain`: `data class RecentImage(val uri: String, val filePath: String, val kind: RecentImageKind)`,
    `enum class RecentImageKind { SOURCE, CUTOUT }`
  - `data`: `RecentImageEntity(val uri: String, val kind: RecentImageKindEntity)`,
    `RecentImageKindEntity { SOURCE, CUTOUT }`
  - `RecentImageLocalDataSource`: `values: Flow<List<RecentImageEntity>>` ·
    `encodeValue(List<RecentImageEntity>): String` · `decodeValue(String?): List<RecentImageEntity>` ·
    `edit(suspend (RecentImageEditor) -> Unit)` · `remove(uris: List<String>)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/source/image/local/RecentImageLocalDataSourceImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.model.local.RecentImageEntity
import com.teamyg.parfait.data.model.local.RecentImageKindEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class RecentImageLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    // 프로덕션 @LocalJson 과 같은 설정이다(`data/di/JsonModule.kt`). coerceInputValues 를 빼면
    // 모르는 종류값을 흡수하는 동작이 테스트에서 재현되지 않는다
    private val dataSource = RecentImageLocalDataSourceImpl(
        dataStore = dataStore,
        json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        },
    )

    private val entities = listOf(
        RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE),
        RecentImageEntity(uri = "content://recent/b", kind = RecentImageKindEntity.CUTOUT),
    )

    @Test
    fun encodeThenDecode_roundTripsKind() {
        // Given 종류가 섞인 목록

        // When 저장 형태로 바꿨다가 되돌린다
        val decoded = dataSource.decodeValue(dataSource.encodeValue(entities))

        // Then 종류가 뒤바뀌지 않는다
        assertEquals(entities, decoded)
    }

    @Test
    fun decodeValue_legacyStringList_survivesAsSourceKind() {
        // Given 종류 축이 없던 시절의 값이 남아 있다
        val legacy = """["content://recent/a","content://recent/b"]"""

        // When 새 스키마로 읽는다
        val decoded = dataSource.decodeValue(legacy)

        // Then 목록이 통째로 날아가지 않고 원본 사진으로 올라온다 — 여기서 비우면 파일은
        // 남는데 목록에서 사라져 `clearOutsideDayWindow` 가 영영 못 지운다
        assertEquals(
            listOf(
                RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE),
                RecentImageEntity(uri = "content://recent/b", kind = RecentImageKindEntity.SOURCE),
            ),
            decoded,
        )
    }

    @Test
    fun decodeValue_unknownKind_keepsEntryAsSource() {
        // Given 이 판본이 모르는 종류값이 섞여 있다(뒷 판본에서 만든 값이거나 손상된 값)
        val unknown = """[{"uri":"content://recent/a","kind":"STICKER"}]"""

        // When 읽는다
        val decoded = dataSource.decodeValue(unknown)

        // Then 항목 하나 때문에 목록 전체가 비워지지 않는다 — 기본값이 있어야
        // coerceInputValues 가 흡수한다
        assertEquals(
            listOf(RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE)),
            decoded,
        )
    }

    @Test
    fun decodeValue_brokenPayload_readsEmpty() {
        // Given 어느 스키마로도 읽히지 않는 값
        val broken = "{not json at all"

        // When 읽는다
        val decoded = dataSource.decodeValue(broken)

        // Then 빈 목록이다 — 두 번째 시도까지 실패했을 때만 여기로 온다
        assertEquals(emptyList(), decoded)
    }

    @Test
    fun values_afterEditWithLegacyPayload_emitsMigratedList() = runTest {
        // Given 구 스키마 값이 저장돼 있다
        dataSource.edit { editor -> editor.set("""["content://recent/a"]""") }

        // When 흐름을 읽는다
        val emitted = dataSource.values.first()

        // Then 폴백이 흐름 쪽에도 걸린다 — decodeValue 만 고치고 values 를 놓치면
        // 화면에서만 목록이 비어 보인다
        assertEquals(
            listOf(RecentImageEntity(uri = "content://recent/a", kind = RecentImageKindEntity.SOURCE)),
            emitted,
        )
    }

    @Test
    fun remove_dropsMatchingUrisOnly() = runTest {
        // Given 두 항목이 저장돼 있다
        dataSource.edit { editor -> editor.set(dataSource.encodeValue(entities)) }

        // When 하나만 지운다
        dataSource.remove(listOf("content://recent/a"))

        // Then 나머지 하나가 종류를 유지한 채 남는다
        assertEquals(listOf(entities[1]), dataSource.values.first())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*RecentImageLocalDataSourceImplTest*"
```

Expected: 컴파일 실패 — `RecentImageEntity`·`RecentImageKindEntity`가 없다.

- [ ] **Step 3: 도메인 모델을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/image/RecentImage.kt`:

```kotlin
package com.teamyg.parfait.domain.model.image

/**
 * 갤러리 "최근"에 남는 항목 한 개.
 *
 * @param uri 화면이 그릴 때 쓰는 FileProvider uri
 * @param filePath 같은 파일의 절대경로. 토핑 초안이 요구하는 형태다
 */
data class RecentImage(
    val uri: String,
    val filePath: String,
    val kind: RecentImageKind,
)

enum class RecentImageKind {
    /** 사용자가 고른 원본 사진 */
    SOURCE,

    /** 배치까지 마친 테두리 없는 트리밍 알맹이 */
    CUTOUT,
}
```

- [ ] **Step 4: 저장 형태와 매퍼를 만든다**

`data/src/main/java/com/teamyg/parfait/data/model/local/RecentImageEntity.kt`:

```kotlin
package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.image.RecentImageKind
import kotlinx.serialization.Serializable

/** 최근 이미지의 저장 형태. 절대경로는 저장하지 않는다 — uri 로부터 매번 되짚는다 */
@Serializable
data class RecentImageEntity(
    val uri: String,
    /** 기본값이 있어야 모르는 종류값 하나가 목록 전체를 못 날린다(`coerceInputValues`) */
    val kind: RecentImageKindEntity = RecentImageKindEntity.SOURCE,
)

@Serializable
enum class RecentImageKindEntity {
    SOURCE,
    CUTOUT,
}

fun RecentImageKindEntity.toVO(): RecentImageKind = when (this) {
    RecentImageKindEntity.SOURCE -> RecentImageKind.SOURCE
    RecentImageKindEntity.CUTOUT -> RecentImageKind.CUTOUT
}

fun RecentImageKind.toEntity(): RecentImageKindEntity = when (this) {
    RecentImageKind.SOURCE -> RecentImageKindEntity.SOURCE
    RecentImageKind.CUTOUT -> RecentImageKindEntity.CUTOUT
}
```

**`kind`의 기본값은 장식이 아니다.** 프로덕션 `@LocalJson`은 `coerceInputValues = true`인데, 이
옵션은 **기본값이 있는 프로퍼티에만** 작동한다. 기본값이 없으면 모르는 종류값 하나가 예외를 내고,
그 예외가 2단 폴백의 두 번째 시도(`List<String>`)까지 실패시켜 **목록이 통째로 비워진다** — 폴백을
만든 이유와 똑같은 사고가 다른 문으로 들어온다. `encodeDefaults = true`라 저장 형태는 달라지지 않는다.

**`ToppingDraftEntity`와 달리 `internal`을 붙이지 않는다.** 그쪽 datasource는 도메인 모델만 내보내지만
여기 datasource 계약은 엔티티를 그대로 노출한다(절대경로를 붙이는 일이 Repository 몫이라 그렇다).
`internal`을 붙이면 "public 함수가 internal 타입을 노출한다"로 깨진다.

- [ ] **Step 5: DataSource 계약을 넓힌다**

`RecentImageLocalDataSource.kt` 전문:

```kotlin
package com.teamyg.parfait.data.source.image.local

import com.teamyg.parfait.data.datastore.RecentImageEditor
import com.teamyg.parfait.data.model.local.RecentImageEntity
import kotlinx.coroutines.flow.Flow

interface RecentImageLocalDataSource {
    val values: Flow<List<RecentImageEntity>>

    fun encodeValue(value: List<RecentImageEntity>): String

    fun decodeValue(raw: String?): List<RecentImageEntity>

    suspend fun edit(transform: suspend (RecentImageEditor) -> Unit)

    suspend fun remove(uris: List<String>)
}
```

- [ ] **Step 6: 2단 폴백 디코드를 구현한다**

`RecentImageLocalDataSourceImpl.kt`에서 `values`·`encodeValue`·`decodeValue`·`remove`·`decode`를
아래로 갈아탄다(`edit`과 `companion object`는 그대로 둔다).

추가할 import: `com.teamyg.parfait.data.model.local.RecentImageEntity`,
`com.teamyg.parfait.data.model.local.RecentImageKindEntity`.

```kotlin
    override val values: Flow<List<RecentImageEntity>> = dataStore.data
        .map { prefs -> decode(prefs[RECENT_IMAGE_URIS_KEY]) }

    override fun encodeValue(value: List<RecentImageEntity>): String = json.encodeToString(value)

    override fun decodeValue(raw: String?): List<RecentImageEntity> = decode(raw)

    override suspend fun remove(uris: List<String>) {
        if (uris.isEmpty()) {
            return
        }

        dataStore.edit { prefs ->
            val current: List<RecentImageEntity> = decode(prefs[RECENT_IMAGE_URIS_KEY])
            val updated: List<RecentImageEntity> = current.filterNot { it.uri in uris }

            prefs[RECENT_IMAGE_URIS_KEY] = json.encodeToString(updated)
        }
    }

    /**
     * 종류 축이 없던 시절의 값도 읽는다. 폴백 없이 빈 목록으로 떨어뜨리면 목록만 사라지고
     * 파일은 남아, 데이 윈도우 정리가 목록을 기준으로 도는 탓에 영영 고아가 된다.
     */
    private fun decode(raw: String?): List<RecentImageEntity> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        return runCatching { json.decodeFromString<List<RecentImageEntity>>(raw) }
            .recoverCatching {
                json
                    .decodeFromString<List<String>>(raw)
                    .map { uri -> RecentImageEntity(uri = uri, kind = RecentImageKindEntity.SOURCE) }
            }.getOrDefault(emptyList())
    }
```

- [ ] **Step 7: Repository를 컴파일만 되게 맞춘다**

`RecentImageRepositoryImpl`에서 목록을 다루는 세 자리를 엔티티 기준으로 바꾼다. **종류 축을
바깥으로 내보내는 것은 Task 3이 한다** — 여기서는 지금의 `Flow<List<String>>` 계약을 유지한다.

추가할 import: `com.teamyg.parfait.data.model.local.RecentImageEntity`,
`com.teamyg.parfait.data.model.local.RecentImageKindEntity`.

```kotlin
    override val recentCacheImages: Flow<List<String>> = recentImageLocalDataSource.values
        .map { entities -> entities.map { it.uri } }

    override suspend fun addAndGetEvictedCacheFileName(value: String): List<String> {
        var evicted: List<String> = emptyList()

        recentImageLocalDataSource.edit { prefs ->
            val current: List<RecentImageEntity> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<RecentImageEntity> = (
                current.filterNot { it.uri == value } +
                    listOf(RecentImageEntity(uri = value, kind = RecentImageKindEntity.SOURCE))
                ).takeLast(MAX_SIZE)

            evicted = current.filterNot { it.uri in updated.map(RecentImageEntity::uri) }.map(RecentImageEntity::uri)
            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }

        return evicted
    }

    override suspend fun removeCacheFileName(values: List<String>) {
        if (values.isEmpty()) {
            return
        }

        recentImageLocalDataSource.edit { prefs ->
            val current: List<RecentImageEntity> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<RecentImageEntity> = current.filterNot { it.uri in values }

            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }
    }
```

추가할 import: `kotlinx.coroutines.flow.map`.

- [ ] **Step 8: 테스트가 통과하는 것을 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*RecentImageLocalDataSourceImplTest*" ktlintCheck
```

Expected: PASS.

- [ ] **Step 9: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/image/RecentImage.kt \
  data/src/main/java/com/teamyg/parfait/data/model/local/RecentImageEntity.kt \
  data/src/main/java/com/teamyg/parfait/data/source/image/local/ \
  data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt \
  data/src/test/java/com/teamyg/parfait/data/source/image/local/
git commit -m "feat: 최근 이미지 목록에 종류 축을 얹고 구 스키마를 폴백으로 읽는다"
```

---

## Task 2: 파일 계층이 `Uri`를 삼키고 절대경로·확장자를 받는다

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/file/local/FileRecentImageLocalDataSource.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/file/local/FileRecentImageLocalDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt` (신규)

**Interfaces:**
- Consumes: Task 1의 `RecentImageEntity`·`RecentImageKindEntity`
- Produces: `FileRecentImageLocalDataSource`:
  `mkdirs(): Boolean` · `readBytes(sourceUri: String): ByteArray` ·
  `readFileBytes(filePath: String): ByteArray` ·
  `getTargetFile(bytes: ByteArray, extension: String): File` ·
  `getTargetFileFromUri(uri: String): File?` · `getUriStringForFile(target: File): String`
  (`getTargetFile(name: String)`은 이 태스크가 지운다)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.data.source.file.local.FileRecentImageLocalDataSource
import com.teamyg.parfait.data.source.image.local.RecentImageLocalDataSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentImageRepositoryImplTest {
    private val dir: File = File(System.getProperty("java.io.tmpdir"), "recent-image-test").apply {
        deleteRecursively()
        mkdirs()
    }

    private val localDataSource: RecentImageLocalDataSource = mockk(relaxed = true)
    private val fileDataSource: FileRecentImageLocalDataSource = mockk(relaxed = true)

    /**
     * 프로퍼티가 아니라 함수다. 구현의 `recentCacheImages` 가 **생성자 초기화식**에서
     * `localDataSource.values` 를 곧바로 읽으므로, 저장소를 먼저 만들어 두면 relaxed mock 이 준
     * 빈 흐름을 붙들어 뒤늦은 `every` 가 닿지 않는다(`ToppingDraftRepositoryImplTest` 가 같은
     * 함정을 문서로 박아 두었다).
     */
    private fun repository() = RecentImageRepositoryImpl(
        recentImageLocalDataSource = localDataSource,
        fileRecentImageLocalDataSource = fileDataSource,
    )

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun store_withContentUri_readsThroughContentResolverPath() = runTest {
        // Given 갤러리가 준 content uri
        val bytes = byteArrayOf(1, 2, 3)
        val target = File(dir, "abc.jpg")
        every { fileDataSource.readBytes("content://media/1") } returns bytes
        every { fileDataSource.getTargetFile(bytes, "jpg") } returns target
        every { fileDataSource.getUriStringForFile(target) } returns "content://recent/abc.jpg"

        // When 원본 사진으로 저장한다
        val stored = repository().storeRecentImageInInternalStorage(
            source = "content://media/1",
            kind = com.teamyg.parfait.domain.model.image.RecentImageKind.SOURCE,
        )

        // Then uri 읽기 경로를 탄다
        verify { fileDataSource.readBytes("content://media/1") }
        assertEquals("content://recent/abc.jpg", stored)
        assertTrue(target.exists())
    }

    @Test
    fun store_withAbsolutePath_readsThroughFilePathAndKeepsPngExtension() = runTest {
        // Given 초안이 들고 있는 절대경로. content resolver 로는 열리지 않는다
        val bytes = byteArrayOf(9, 9)
        val target = File(dir, "def.png")
        every { fileDataSource.readFileBytes("/data/cache/segmentation/subject.png") } returns bytes
        every { fileDataSource.getTargetFile(bytes, "png") } returns target
        every { fileDataSource.getUriStringForFile(target) } returns "content://recent/def.png"

        // When 알맹이로 저장한다
        val stored = repository().storeRecentImageInInternalStorage(
            source = "/data/cache/segmentation/subject.png",
            kind = com.teamyg.parfait.domain.model.image.RecentImageKind.CUTOUT,
        )

        // Then 파일 읽기 경로를 타고 확장자가 png 다 — jpg 로 굳으면 투명 PNG 가
        // image/jpeg 로 올라가 알파가 사라진다
        verify { fileDataSource.readFileBytes("/data/cache/segmentation/subject.png") }
        verify { fileDataSource.getTargetFile(bytes, "png") }
        assertEquals("content://recent/def.png", stored)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*RecentImageRepositoryImplTest*"
```

Expected: 컴파일 실패 — `readFileBytes`·`getTargetFile(bytes, extension)`·`getUriStringForFile`가 없고
`storeRecentImageInInternalStorage`가 인자 하나만 받는다.

- [ ] **Step 3: 파일 datasource 계약을 바꾼다**

`FileRecentImageLocalDataSource.kt` 전문:

```kotlin
package com.teamyg.parfait.data.source.file.local

import java.io.File

/** 안드로이드 uri 를 밖으로 내보내지 않는다 — 위 계층이 JVM 테스트로 잡히게 하려는 경계다 */
interface FileRecentImageLocalDataSource {
    fun mkdirs(): Boolean

    fun readBytes(sourceUri: String): ByteArray

    fun readFileBytes(filePath: String): ByteArray

    fun getTargetFile(
        bytes: ByteArray,
        extension: String,
    ): File

    /** uri 가 가리키는 파일 이름을 못 읽으면 `null` */
    fun getTargetFileFromUri(uri: String): File?

    fun getUriStringForFile(target: File): String
}
```

지울 import: `android.net.Uri`.

- [ ] **Step 4: 구현을 맞춘다**

`FileRecentImageLocalDataSourceImpl.kt`에서 `readBytes` 아래를 아래로 갈아탄다.

```kotlin
    override fun readFileBytes(filePath: String): ByteArray = File(filePath).readBytes()

    override fun getTargetFile(
        bytes: ByteArray,
        extension: String,
    ): File = File(
        dir,
        bytes.sha256() + "." + extension,
    )

    override fun getTargetFileFromUri(uri: String): File? = uri
        .toUri()
        .lastPathSegment
        ?.let { name -> File(dir, name) }

    override fun getUriStringForFile(target: File): String = FileProvider
        .getUriForFile(
            context,
            authority,
            target,
        ).toString()
```

`private fun fileName`과 `FILE_EXTENSION` 상수는 지운다. 지울 import: `android.net.Uri`.
`getTargetFile(name: String)` 오버로드는 **인터페이스와 구현 양쪽에서 지운다** — 유일한 호출부였던
`RecentImageRepositoryImpl`의 두 자리를 Step 5가 `getTargetFileFromUri`로 갈아 치우므로 남기면
호출부 없는 죽은 계약이 된다. `androidx.core.net.toUri`는 `readBytes`가 계속 쓰므로 **지우지 않는다**.

- [ ] **Step 5: Repository에서 안드로이드 타입을 걷는다**

`RecentImageRepositoryImpl.kt`의 아래 세 함수를 갈아탄다.

```kotlin
    override suspend fun storeRecentImageInInternalStorage(
        source: String,
        kind: RecentImageKind,
    ): String = withContext(Dispatchers.IO) {
        fileRecentImageLocalDataSource.mkdirs()

        val bytes: ByteArray = when (kind) {
            // 갤러리·카메라가 주는 것은 uri 이고, 초안이 주는 것은 스킴 없는 절대경로다
            RecentImageKind.SOURCE -> fileRecentImageLocalDataSource.readBytes(source)
            RecentImageKind.CUTOUT -> fileRecentImageLocalDataSource.readFileBytes(source)
        }
        val target: File = fileRecentImageLocalDataSource.getTargetFile(bytes, kind.fileExtension())

        if (target.exists().not()) {
            target
                .outputStream()
                .use { output -> output.write(bytes) }
        }

        target.setLastModified(Clock.System.now().toEpochMilliseconds())

        return@withContext fileRecentImageLocalDataSource.getUriStringForFile(target)
    }

    override suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean = withContext(Dispatchers.IO) {
        val file: File = fileRecentImageLocalDataSource.getTargetFileFromUri(sourceUri) ?: return@withContext false

        return@withContext file.delete()
    }

    override suspend fun getLastModifiedCacheFile(sourceUri: String): Long? = withContext(Dispatchers.IO) {
        val file: File = fileRecentImageLocalDataSource.getTargetFileFromUri(sourceUri) ?: return@withContext null

        return@withContext when (file.exists()) {
            true -> file.lastModified()
            false -> null
        }
    }

    /** 알맹이는 투명 PNG 다. 이름이 거짓이면 업로드가 content type 을 잘못 정한다 */
    private fun RecentImageKind.fileExtension(): String = when (this) {
        RecentImageKind.SOURCE -> "jpg"
        RecentImageKind.CUTOUT -> "png"
    }
```

추가할 import: `com.teamyg.parfait.domain.model.image.RecentImageKind`.
지울 import: `android.net.Uri`, `androidx.core.net.toUri`.

`RecentImageRepository`의 선언도 함께 바꾼다:

```kotlin
    suspend fun storeRecentImageInInternalStorage(
        source: String,
        kind: RecentImageKind,
    ): String
```

추가할 import: `com.teamyg.parfait.domain.model.image.RecentImageKind`.

- [ ] **Step 6: 호출부를 맞춘다**

`AddRecentImageUseCase#invoke`가 이 함수를 부른다. Task 3이 종류를 인자로 받게 만들 때까지는
`RecentImageKind.SOURCE`를 넘겨 기존 동작을 유지한다.

```kotlin
        val stableUri: String? = runSuspendCatching {
            recentImageRepository.storeRecentImageInInternalStorage(
                source = uri,
                kind = RecentImageKind.SOURCE,
            )
        }.getOrNull()
```

추가할 import: `com.teamyg.parfait.domain.model.image.RecentImageKind`.

- [ ] **Step 7: 테스트가 통과하는 것을 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*RecentImageRepositoryImplTest*" :domain:test ktlintCheck
```

Expected: PASS.

- [ ] **Step 8: 커밋**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/file/local/ \
  data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt \
  domain/src/main/java/com/teamyg/parfait/domain/repository/image/RecentImageRepository.kt \
  domain/src/main/java/com/teamyg/parfait/domain/usecase/image/AddRecentImageUseCase.kt \
  data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt
git commit -m "refactor: 최근 이미지 파일 계층이 uri 를 삼키고 절대경로·확장자를 받는다"
```

---

## Task 3: 종류 축을 Repository와 UseCase 밖으로 관통시킨다

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/image/RecentImageRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/AddRecentImageUseCase.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/usecase/image/GetRecentCacheImagesUseCase.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/viewmodel/CustomGalleryPickerViewModel.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/screen/CustomGalleryPickerScreen.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/component/GalleryImageGridComponent.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt` (확장)

**Interfaces:**
- Consumes: Task 1의 `RecentImage`·`RecentImageKind`, Task 2의 파일 datasource 계약
- Produces:
  - `RecentImageRepository.recentCacheImages: Flow<List<RecentImage>>`
  - `RecentImageRepository.addAndGetEvictedCacheFileName(uri: String, kind: RecentImageKind): List<String>`
  - `AddRecentImageUseCase.invoke(source: String, kind: RecentImageKind = RecentImageKind.SOURCE)`
  - `GetRecentCacheImagesUseCase.invoke(): Flow<List<RecentImage>>`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`RecentImageRepositoryImplTest`에 아래 둘을 더한다. 추가할 import:
`com.teamyg.parfait.data.model.local.RecentImageEntity`,
`com.teamyg.parfait.data.model.local.RecentImageKindEntity`,
`com.teamyg.parfait.domain.model.image.RecentImage`,
`com.teamyg.parfait.domain.model.image.RecentImageKind`,
`kotlinx.coroutines.flow.first`, `kotlinx.coroutines.flow.flowOf`.
(`io.mockk.coEvery`는 쓰지 않는다 — 두 테스트가 `every`만 쓴다.)

> 두 테스트 모두 `every { localDataSource.values } returns …`를 **`repository()` 호출보다 먼저**
> 둔다. 순서가 뒤집히면 생성자 초기화식이 relaxed mock의 빈 흐름을 붙들어 단언이 무조건 실패한다.

```kotlin
    @Test
    fun recentCacheImages_attachesAbsolutePathToEachEntry() = runTest {
        // Given 종류가 섞인 저장 목록
        every { localDataSource.values } returns flowOf(
            listOf(
                RecentImageEntity(uri = "content://recent/a.jpg", kind = RecentImageKindEntity.SOURCE),
                RecentImageEntity(uri = "content://recent/b.png", kind = RecentImageKindEntity.CUTOUT),
            ),
        )
        every { fileDataSource.getTargetFileFromUri("content://recent/a.jpg") } returns File(dir, "a.jpg")
        every { fileDataSource.getTargetFileFromUri("content://recent/b.png") } returns File(dir, "b.png")

        // When 목록을 읽는다
        val images: List<RecentImage> = repository().recentCacheImages.first()

        // Then 절대경로가 함께 온다 — 확인 화면과 초안이 요구하는 형태가 uri 가 아니라 경로다
        assertEquals(
            listOf(
                RecentImage(
                    uri = "content://recent/a.jpg",
                    filePath = File(dir, "a.jpg").absolutePath,
                    kind = RecentImageKind.SOURCE,
                ),
                RecentImage(
                    uri = "content://recent/b.png",
                    filePath = File(dir, "b.png").absolutePath,
                    kind = RecentImageKind.CUTOUT,
                ),
            ),
            images,
        )
    }

    @Test
    fun recentCacheImages_dropsEntryWhoseFileNameIsUnreadable() = runTest {
        // Given 파일 이름을 못 읽는 값이 섞여 있다
        every { localDataSource.values } returns flowOf(
            listOf(RecentImageEntity(uri = "broken", kind = RecentImageKindEntity.SOURCE)),
        )
        every { fileDataSource.getTargetFileFromUri("broken") } returns null

        // When 목록을 읽는다
        val images: List<RecentImage> = repository().recentCacheImages.first()

        // Then 경로 없는 항목을 지어내지 않고 뺀다
        assertEquals(emptyList(), images)
    }
```

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*RecentImageRepositoryImplTest*"
```

Expected: 컴파일 실패 — `recentCacheImages`가 아직 `Flow<List<String>>`이다.

- [ ] **Step 3: Repository 계약과 구현을 바꾼다**

`RecentImageRepository.kt` 전문:

```kotlin
package com.teamyg.parfait.domain.repository.image

import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import kotlinx.coroutines.flow.Flow

interface RecentImageRepository {
    val recentCacheImages: Flow<List<RecentImage>>

    suspend fun addAndGetEvictedCacheFileName(
        uri: String,
        kind: RecentImageKind,
    ): List<String>

    suspend fun removeCacheFileName(values: List<String>)

    suspend fun storeRecentImageInInternalStorage(
        source: String,
        kind: RecentImageKind,
    ): String

    suspend fun deleteRecentImageInInternalStorage(sourceUri: String): Boolean

    suspend fun getLastModifiedCacheFile(sourceUri: String): Long?
}
```

`RecentImageRepositoryImpl`의 두 자리를 갈아탄다.

```kotlin
    override val recentCacheImages: Flow<List<RecentImage>> = recentImageLocalDataSource.values
        .map { entities ->
            entities.mapNotNull { entity ->
                val file: File = fileRecentImageLocalDataSource.getTargetFileFromUri(entity.uri)
                    ?: return@mapNotNull null

                RecentImage(
                    uri = entity.uri,
                    filePath = file.absolutePath,
                    kind = entity.kind.toVO(),
                )
            }
        }

    override suspend fun addAndGetEvictedCacheFileName(
        uri: String,
        kind: RecentImageKind,
    ): List<String> {
        var evicted: List<String> = emptyList()

        recentImageLocalDataSource.edit { prefs ->
            val current: List<RecentImageEntity> = recentImageLocalDataSource.decodeValue(prefs.get())
            val updated: List<RecentImageEntity> = (
                current.filterNot { it.uri == uri } +
                    listOf(RecentImageEntity(uri = uri, kind = kind.toEntity()))
                ).takeLast(MAX_SIZE)
            val keptUris: List<String> = updated.map(RecentImageEntity::uri)

            evicted = current.filterNot { it.uri in keptUris }.map(RecentImageEntity::uri)
            prefs.set(recentImageLocalDataSource.encodeValue(updated))
        }

        return evicted
    }
```

추가할 import: `com.teamyg.parfait.data.model.local.toEntity`,
`com.teamyg.parfait.data.model.local.toVO`,
`com.teamyg.parfait.domain.model.image.RecentImage`.
지울 import: `com.teamyg.parfait.data.model.local.RecentImageKindEntity`(더는 안 쓴다).

> `mapNotNull`이 경로를 못 만든 항목을 목록에서 뺀다. 그러면 그 항목은
> `clearOutsideDayWindow`의 시야에서도 사라져 **저장소에 영영 남는다**(지금은 uri를 그대로
> 흘려서 `getLastModifiedCacheFile`이 `null → 0L`을 주고 윈도우 밖으로 판정돼 걷혔다). 저장되는
> 값이 언제나 FileProvider content uri라 발생 조건이 거의 없어 **감수한다.** 실제로 관측되면
> 정리 기준을 목록이 아니라 datasource의 uri로 바꾼다.

- [ ] **Step 4: UseCase 둘을 맞춘다**

`AddRecentImageUseCase#invoke`:

```kotlin
    suspend operator fun invoke(
        source: String,
        kind: RecentImageKind = RecentImageKind.SOURCE,
    ) {
        val stableUri: String? = runSuspendCatching {
            recentImageRepository.storeRecentImageInInternalStorage(
                source = source,
                kind = kind,
            )
        }.getOrNull()

        if (stableUri == null) {
            useCaseLogger.d { "AddRecentImageUseCase - stableUri is null" }
            return
        }

        val evicted: List<String> = recentImageRepository.addAndGetEvictedCacheFileName(
            uri = stableUri,
            kind = kind,
        )

        useCaseLogger.d { "AddRecentImageUseCase - evicted.size: ${evicted.size}" }

        evicted.forEach {
            recentImageRepository.deleteRecentImageInInternalStorage(it)
        }
    }
```

`GetRecentCacheImagesUseCase`는 반환 타입과 정리 기준만 바꾼다.

```kotlin
    operator fun invoke(): Flow<List<RecentImage>> = recentImageRepository.recentCacheImages
        .onStart { clearOutsideDayWindow() }
        .distinctUntilChanged()
```

`clearOutsideDayWindow` 안에서 목록 타입이 바뀌므로 아래처럼 uri를 꺼내 쓴다.

```kotlin
        val current: List<RecentImage> = recentImageRepository.recentCacheImages
            .first()
            .also { current ->
                useCaseLogger.d { "clearOutsideDayWindow - current.size: ${current.size}" }
            }

        val outdated: List<String> = current
            .filterNot { image ->
                val lastModified = recentImageRepository.getLastModifiedCacheFile(image.uri) ?: 0L

                lastModified in window
            }.map(RecentImage::uri)
            .also { outdated ->
                useCaseLogger.d { "clearOutsideDayWindow - outdated.size: ${outdated.size}" }
            }
```

추가할 import는 파일마다 다르다. `GetRecentCacheImagesUseCase`에는
`com.teamyg.parfait.domain.model.image.RecentImage`만, `AddRecentImageUseCase`에는
`com.teamyg.parfait.domain.model.image.RecentImageKind`만 넣는다. 서로 교차해 넣으면 미사용
import로 ktlint가 멈춘다.

- [ ] **Step 5: 갤러리 소비자를 컴파일만 되게 맞춘다**

노출 분기와 셀 표시는 Task 5가 한다. 여기서는 타입만 따라간다.

화면 계층은 Route → `CustomGalleryPickerScreen` → 같은 파일의 private `GalleryContent` →
`GalleryImageGridComponent` **넷**이다. `CustomGalleryPickerScreen`은 `recentImages`를 직접 받지
않고 `state`를 받으므로 시그니처가 안 바뀌고, 실제로 고칠 자리는 `GalleryContent`다.

- `CustomGalleryPickerState.recentImages: List<RecentImage>`
- `GalleryContent(… recentImages: List<RecentImage> …)`
- `GalleryImageGridComponent(recentImages: List<RecentImage>, …)` — `items` 블록의 키는
  `key = { "recent-${it.uri}" }`, 셀 호출은 `uri = image.uri`

`GalleryImageGridComponent`의 `@YGPreview` 호출부(파일 끝 `recentImages = listOf("test1", "test3")`)를
`RecentImage(uri = …, filePath = …, kind = RecentImageKind.SOURCE)` 형태로 고친다.

추가할 import: `com.teamyg.parfait.domain.model.image.RecentImage`(세 파일), 프리뷰가 있는 파일에
`com.teamyg.parfait.domain.model.image.RecentImageKind`.

- [ ] **Step 6: 테스트와 빌드가 통과하는 것을 확인한다**

```bash
./gradlew :data:testDebugUnitTest :domain:test :feature:segmentation:impl:testDebugUnitTest \
  ktlintCheck :app:assembleDebug
```

Expected: PASS. `assembleDebug`는 main 소스만 컴파일하므로 **segmentation 테스트를 반드시 함께
돌린다** — `SegmentationViewModelTest`가 `addRecentImage(SOURCE_URI)`를 네 자리에서 `coVerify`·
`coEvery`로 잡고 있고, 기본 인자가 붙은 호출을 MockK가 어떻게 기록하는지는 돌려 봐야 안다.
깨지면 그 네 자리를 `addRecentImage(SOURCE_URI, RecentImageKind.SOURCE)`로 명시한다.

- [ ] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/ \
  data/src/main/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImpl.kt \
  data/src/test/java/com/teamyg/parfait/data/repository/image/RecentImageRepositoryImplTest.kt \
  feature/gallery/impl/src/main/
git commit -m "feat: 최근 이미지 목록이 종류와 절대경로를 함께 실어 나른다"
```

---

## Task 4: 배치에 성공하면 알맹이를 최근 목록에 남긴다

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/.../CanvasToppingPlaceViewModelTest.kt` (확장)

**Interfaces:**
- Consumes: `AddRecentImageUseCase.invoke(source, kind)`, `ToppingDraftRepository#clear`
- Produces: 없음(화면 안에서 닫힌다)

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`CanvasToppingPlaceViewModelTest`에 더한다. 기존 픽스처(`draft()`·`readyViewModel()`)를 그대로 쓰고,
클래스 프로퍼티로 `private val addRecentImageUseCase: AddRecentImageUseCase = mockk(relaxed = true)`를
선언한다.

⚠️ **`CanvasToppingPlaceViewModel(`를 부르는 자리가 셋이다.** 생성자에 파라미터를 더하면 셋 다
깨지므로 전부 `addRecentImageUseCase = addRecentImageUseCase`를 더한다 — `viewModel()` 헬퍼 하나와,
자기 안에서 `draft` 스텁을 따로 세우느라 헬퍼를 안 쓰는 테스트 둘
(`onClickConfirm_beforeDraftEmits_sendsNoEffect`·`draft_throws_tellsTheUser_insteadOfDyingSilently`).

```kotlin
    @Test
    fun onClickConfirm_afterSuccess_savesCutoutBeforeClearingDraft() = runTest(mainDispatcherRule.dispatcher) {
        // Given 배치가 성공하는 상태
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()

        // When 확인을 누른다
        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 알맹이를 남긴 뒤에 초안을 비운다 — 순서가 뒤집히면 경로를 잃어 아무것도 안 남는다
        coVerifyOrder {
            addRecentImageUseCase(
                source = "/cache/segmentation/subject.png",
                kind = RecentImageKind.CUTOUT,
            )
            toppingDraftRepository.clear()
        }
    }

    @Test
    fun onClickConfirm_whenRecentImageSaveThrows_stillReportsSuccess() = runTest(mainDispatcherRule.dispatcher) {
        // Given 최근 목록 저장이 던진다
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit
        coEvery { addRecentImageUseCase(any(), any()) } throws IllegalStateException("disk full")
        val viewModel = readyViewModel()

        // When 확인을 누른다
        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // Then 배치는 이미 성공했다. 재사용 편의 하나로 성공한 흐름을 실패로 보이게 하지 않는다
            assertEquals(CanvasToppingPlaceEffect.PlaceSucceeded, awaitItem())
        }
    }
```

추가할 import: `com.teamyg.parfait.domain.model.image.RecentImageKind`,
`com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase`, `io.mockk.coVerifyOrder`.

> `addToppingUseCase`는 `Result<PlacedToppingVO>`를 돌려준다. 기존 성공 케이스도 `Result.success(mockk())`를
> 쓴다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingPlaceViewModelTest*"
```

Expected: 컴파일 실패 — ViewModel이 `AddRecentImageUseCase`를 받지 않는다.

- [ ] **Step 3: 구현한다**

생성자에 `private val addRecentImageUseCase: AddRecentImageUseCase,`를 더하고,
`handleOnClickConfirm`의 `.onSuccess { }` 안에서 **`PlaceSucceeded`를 쏘기 전에** 저장을 넣는다.
경로는 그 함수가 확정 직전에 이미 지역 변수 `imagePath`로 잡아 둔 값을 그대로 쓴다 — `clear()` 뒤에
초안을 다시 읽으면 없다.

```kotlin
                }.onSuccess {
                    // 알림보다 먼저 남긴다 — PlaceSucceeded 를 받은 Route 가 popUpTo 로 이 화면을
                    // 걷어 내면 viewModelScope 가 취소되고, 그 뒤 코드는 실행되다 말고 끊긴다
                    runSuspendCatching {
                        addRecentImageUseCase(source = imagePath, kind = RecentImageKind.CUTOUT)
                    }.onFailure { throwable ->
                        viewModelLogger.d { "recent cutout save failed - $throwable" }
                    }

                    // 되감기를 먼저 알린다 — clear() 가 초안을 비우면 구독이 알맹이를 null 로
                    // 되돌려, 오버레이가 내려간 화면에 빈 캔버스가 잠깐 조작 가능한 상태로 남는다
                    postSideEffect(effect = CanvasToppingPlaceEffect.PlaceSucceeded)
                    toppingDraftRepository.clear()
                }
```

⚠️ **이 순서가 이 태스크의 핵심이다.** `PlaceSucceeded`를 받은 `CanvasToppingPlaceRoute`가
`popUpTo<NavKeyCanvasMain>()`을 부르고, 그 `popUpTo`는 백스택 리스트를 **즉시** 잘라 다음
리컴포지션에서 NavEntry가 dispose되며 `viewModelScope`가 취소된다. 저장을 그 뒤에 두면 파일 읽기·
sha256·쓰기·DataStore edit·축출 삭제가 **한 프레임 안에** 끝나야만 살아남고, `runSuspendCatching`은
`CancellationException`을 재던지므로 `clear()`까지 함께 건너뛴다. 저장이 앞에 있으면 그 시간 동안
로딩 오버레이가 유지될 뿐이다. 계획의 테스트는 내비게이션을 태우지 않으므로 **순서를 뒤집어도
초록불이 뜬다** — 테스트가 아니라 이 문단이 근거다.

추가할 import: `com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching`,
`com.teamyg.parfait.domain.model.image.RecentImageKind`,
`com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase`.
`viewModelLogger`가 아직 import되어 있지 않으면 `com.teamyg.parfait.core.ui.viewModelLogger`도 더한다.

- [ ] **Step 4: 테스트가 통과하는 것을 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
```

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
git add feature/groups/canvas/impl/src/
git commit -m "feat: 배치에 성공하면 테두리 없는 알맹이를 최근 목록에 남긴다"
```

---

## Task 5: 갤러리가 진입 경로에 따라 알맹이를 싣고 잘리지 않게 그린다

**Files:**
- Modify: `feature/gallery/impl/build.gradle.kts`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/viewmodel/CustomGalleryPickerViewModel.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/route/CustomGalleryPickerRoute.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/screen/CustomGalleryPickerScreen.kt`
- Modify: `feature/gallery/impl/src/main/java/com/teamyg/parfait/feature/gallery/impl/component/GalleryImageGridComponent.kt`
- Test: `feature/gallery/impl/src/test/java/com/teamyg/parfait/feature/gallery/impl/viewmodel/CustomGalleryPickerViewModelTest.kt` (신규)

**Interfaces:**
- Consumes: Task 3의 `GetRecentCacheImagesUseCase.invoke(): Flow<List<RecentImage>>`
- Produces:
  - `CustomGalleryPickerViewModel.Factory.create(returnResultOnly: Boolean)`
  - `CustomGalleryPickerIntent.OnClickImage(image: RecentImage?)`는 만들지 않는다 —
    `OnClickImage(uri: String, kind: RecentImageKind)`로 넓힌다.
  - `CustomGalleryPickerEffect.NavigateToSegmentationConfirm(cutoutFilePath: String)`

- [ ] **Step 1: 모듈에 단위 테스트를 켠다**

`feature/gallery/impl/build.gradle.kts`의 `plugins` 블록에 한 줄을 더한다.

```kotlin
    alias(libs.plugins.parfait.test.unit)
```

- [ ] **Step 2: 실패하는 테스트를 쓴다**

`feature/gallery/impl/src/test/java/com/teamyg/parfait/feature/gallery/impl/viewmodel/CustomGalleryPickerViewModelTest.kt`:

```kotlin
package com.teamyg.parfait.feature.gallery.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.domain.usecase.gallery.LoadFilterYGGalleryImageGroupsUseCase
import com.teamyg.parfait.domain.usecase.image.GetRecentCacheImagesUseCase
import com.teamyg.parfait.core.testing.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomGalleryPickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getRecentCacheImages: GetRecentCacheImagesUseCase = mockk()
    private val loadGroups: LoadFilterYGGalleryImageGroupsUseCase = mockk(relaxed = true)

    private val source = RecentImage(
        uri = "content://recent/a.jpg",
        filePath = "/data/files/recent_images/a.jpg",
        kind = RecentImageKind.SOURCE,
    )
    private val cutout = RecentImage(
        uri = "content://recent/b.png",
        filePath = "/data/files/recent_images/b.png",
        kind = RecentImageKind.CUTOUT,
    )

    private fun createViewModel(returnResultOnly: Boolean): CustomGalleryPickerViewModel {
        every { getRecentCacheImages() } returns flowOf(listOf(source, cutout))

        return CustomGalleryPickerViewModel(
            returnResultOnly = returnResultOnly,
            getRecentCacheImagesUseCase = getRecentCacheImages,
            loadFilterYGGalleryImageGroupsUseCase = loadGroups,
        )
    }

    @Test
    fun recentImages_whenReturnResultOnly_hidesCutout() = runTest(mainDispatcherRule.dispatcher) {
        // Given 결과만 돌려주는 진입(배경 선택)
        val viewModel = createViewModel(returnResultOnly = true)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 알맹이는 안 보인다 — 배경으로 투명 알맹이가 골라지면 안 된다
        assertEquals(listOf(source), viewModel.state.value.recentImages)
    }

    @Test
    fun recentImages_whenToppingFlow_showsBoth() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(returnResultOnly = false)

        // When 목록이 흘러온다
        advanceUntilIdle()

        // Then 종류를 가리지 않는다
        assertEquals(listOf(source, cutout), viewModel.state.value.recentImages)
    }

    @Test
    fun onClickImage_withCutout_navigatesToSegmentationConfirmWithFilePath() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 토핑 만들기 진입
            val viewModel = createViewModel(returnResultOnly = false)
            advanceUntilIdle()

            // When 알맹이를 누른다
            viewModel.effect.test {
                viewModel.processIntent(
                    CustomGalleryPickerIntent.OnClickImage(uri = cutout.uri, kind = RecentImageKind.CUTOUT),
                )

                // Then 확인 화면으로 가고, 넘기는 것은 uri 가 아니라 절대경로다 — 초안 계약이 경로다
                assertEquals(
                    CustomGalleryPickerEffect.NavigateToSegmentationConfirm(cutout.filePath),
                    awaitItem(),
                )
            }
        }

    @Test
    fun onClickImage_withSource_navigatesToPictureConfirm() = runTest(mainDispatcherRule.dispatcher) {
        // Given 토핑 만들기 진입
        val viewModel = createViewModel(returnResultOnly = false)
        advanceUntilIdle()

        // When 원본 사진을 누른다
        viewModel.effect.test {
            viewModel.processIntent(
                CustomGalleryPickerIntent.OnClickImage(uri = source.uri, kind = RecentImageKind.SOURCE),
            )

            // Then 지금까지의 경로 그대로다
            assertEquals(CustomGalleryPickerEffect.NavigateToConfirm(source.uri), awaitItem())
        }
    }
}
```

> `MainDispatcherRule`은 `core/testing` 모듈에 있다. `parfait.test.unit` 플러그인이 그 의존을
> 함께 붙이는지 확인하고, 안 붙으면 `testImplementation(projects.core.testing)`을 더한다.

- [ ] **Step 3: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew :feature:gallery:impl:testDebugUnitTest
```

Expected: 컴파일 실패 — ViewModel이 `returnResultOnly`를 받지 않고 `OnClickImage`가 종류를 모른다.

- [ ] **Step 4: ViewModel을 고친다**

상태·인텐트·이펙트를 아래로 바꾼다.

```kotlin
@Immutable
data class CustomGalleryPickerState(
    val isLoading: Boolean = false,
    val access: GalleryPermissionManager.GalleryAccessLevel = GalleryPermissionManager.GalleryAccessLevel.INITIAL,
    val groups: List<GalleryImageGroup> = emptyList(),
    val recentImages: List<RecentImage> = emptyList(),
) : UiState {
    val isEmpty: Boolean
        get() = groups.all { it.images.isEmpty() } && recentImages.isEmpty()
}
```

`CustomGalleryPickerEffect`에 더한다.

```kotlin
    data class NavigateToSegmentationConfirm(
        val cutoutFilePath: String,
    ) : CustomGalleryPickerEffect()
```

`CustomGalleryPickerIntent.OnClickImage`를 넓힌다.

```kotlin
    data class OnClickImage(
        val uri: String,
        val kind: RecentImageKind,
    ) : CustomGalleryPickerIntent()
```

생성자를 assisted로 바꾸고(`SegmentationViewModel`이 본보기다) 두 함수를 고친다.

```kotlin
@HiltViewModel(assistedFactory = CustomGalleryPickerViewModel.Factory::class)
class CustomGalleryPickerViewModel
@AssistedInject constructor(
    @Assisted private val returnResultOnly: Boolean,
    private val getRecentCacheImagesUseCase: GetRecentCacheImagesUseCase,
    private val loadFilterYGGalleryImageGroupsUseCase: LoadFilterYGGalleryImageGroupsUseCase,
) : BaseViewModel<CustomGalleryPickerState, CustomGalleryPickerIntent, CustomGalleryPickerEffect>(
    initialState = CustomGalleryPickerState(),
) {
```

```kotlin
    private fun handleOnClickImage(intent: CustomGalleryPickerIntent.OnClickImage) {
        when (intent.kind) {
            RecentImageKind.SOURCE -> postSideEffect(CustomGalleryPickerEffect.NavigateToConfirm(intent.uri))
            // 이미 누끼가 끝난 알맹이라 카메라·세그멘테이션을 건너뛴다
            RecentImageKind.CUTOUT -> {
                val filePath = state.value.recentImages
                    .firstOrNull { it.uri == intent.uri }
                    ?.filePath
                    ?: return

                postSideEffect(CustomGalleryPickerEffect.NavigateToSegmentationConfirm(filePath))
            }
        }
    }

    private suspend fun collectRecentCacheImages() = getRecentCacheImagesUseCase().collect { images ->
        // 배경 선택처럼 결과만 돌려주는 진입에는 알맹이를 싣지 않는다
        val visible = when (returnResultOnly) {
            true -> images.filter { it.kind == RecentImageKind.SOURCE }
            false -> images
        }

        updateState { copy(recentImages = visible) }
    }
```

파일 끝에 팩토리를 더한다.

```kotlin
    @AssistedFactory
    interface Factory {
        fun create(returnResultOnly: Boolean): CustomGalleryPickerViewModel
    }
```

추가할 import: `com.teamyg.parfait.domain.model.image.RecentImage`,
`com.teamyg.parfait.domain.model.image.RecentImageKind`, `dagger.assisted.Assisted`,
`dagger.assisted.AssistedFactory`, `dagger.assisted.AssistedInject`.
지울 import: `javax.inject.Inject`(다른 데서 안 쓰면).

- [ ] **Step 5: Route를 맞춘다**

`CustomGalleryPickerRoute`의 기본 인자 `viewModel: CustomGalleryPickerViewModel = hiltViewModel()`을
지우고 본문에서 만든다.

```kotlin
    val viewModel = hiltViewModel<CustomGalleryPickerViewModel, CustomGalleryPickerViewModel.Factory>(
        creationCallback = { factory -> factory.create(returnResultOnly) },
    )
```

이펙트 `when`에 갈래를 더한다.

```kotlin
                is CustomGalleryPickerEffect.NavigateToSegmentationConfirm -> {
                    navigator.goTo(
                        NavKeySegmentationConfirm(
                            sourceImageUri = null,
                            subjectImagePath = null,
                            trimmedSubjectImagePath = effect.cutoutFilePath,
                        ),
                    )
                }
```

같은 파일에서 화면으로 내려보내는 콜백도 인자 둘을 받게 고친다.

```kotlin
            onClickImage = { uri, kind ->
                viewModel.processIntent(
                    CustomGalleryPickerIntent.OnClickImage(uri = uri, kind = kind),
                )
            },
```

추가할 import: `com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm`,
`com.teamyg.parfait.domain.model.image.RecentImageKind`.
`feature/gallery/impl/build.gradle.kts`의 `dependencies`에
`implementation(projects.feature.segmentation.api)`를 더한다(feature impl이 다른 feature의 api를
쓰는 것은 이 저장소의 기존 관례이고 순환도 생기지 않는다).

> `NavKeySegmentationConfirm`의 인자 완화는 **Task 6이 한다.** 이 단계까지는 컴파일이 깨지므로
> Task 5와 Task 6은 **연달아 끝낸 뒤 함께 검증**한다. 중간 커밋은 Step 8에서 한 번만 한다.

- [ ] **Step 6: 셀을 종류에 따라 그린다**

`GalleryImageGridComponent`의 최근 항목 블록과 셀을 고친다.

```kotlin
            items(
                items = recentImages,
                key = { "recent-${it.uri}" },
            ) { image ->
                GalleryImageCell(
                    uri = image.uri,
                    // 알맹이는 투명 여백을 걷어낸 객체라 잘라 채우면 잘린다
                    contentScale = when (image.kind) {
                        RecentImageKind.SOURCE -> ContentScale.Crop
                        RecentImageKind.CUTOUT -> ContentScale.Fit
                    },
                    onClickImage = { onClickImage(image.uri, image.kind) },
                )
            }
```

`GalleryImageCell` 시그니처를 바꾼다.

```kotlin
@Composable
private fun GalleryImageCell(
    uri: String,
    onClickImage: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(
        modifier = modifier
            .padding(bottom = YGTheme.layout.padding.padding5)
            .aspectRatio(1f)
            .clickableYGNoRipple { onClickImage() },
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
```

날짜 그룹 쪽 호출부는 `onClickImage = { onClickImage(uri, RecentImageKind.SOURCE) }`로 맞춘다.

콜백 타입 `(String, RecentImageKind) -> Unit`을 **네 자리 전부**에 관통시킨다 —
`GalleryImageGridComponent` · `CustomGalleryPickerScreen.kt`의 private `GalleryContent` ·
`CustomGalleryPickerScreen` 자신 · Route(위 Step 5). 프리뷰 호출부 둘
(`GalleryImageGridComponent.kt`·`CustomGalleryPickerScreen.kt`의 `onClickImage = {}`)은
`{ _, _ -> }`로 고친다 — 인자 0개 람다는 인자 둘짜리 타입에 대입되지 않는다.

- [ ] **Step 7: Task 6을 마친 뒤 검증한다**

이 시점에는 `NavKeySegmentationConfirm`이 아직 인자 셋을 요구해 컴파일이 깨져 있다.
**Task 6의 Step 3까지 진행한 뒤** 아래를 돌린다.

```bash
./gradlew :feature:gallery:impl:testDebugUnitTest ktlintCheck
```

Expected: PASS.

- [ ] **Step 8: 커밋**

```bash
git add feature/gallery/impl/
git commit -m "feat: 갤러리가 토핑 만들기 진입에서만 알맹이를 싣고 잘리지 않게 그린다"
```

---

## Task 6: 재사용 진입이 확인 화면으로 직행한다

**Files:**
- Modify: `feature/segmentation/api/src/main/java/com/teamyg/parfait/feature/segmentation/api/NavKeySegmentationConfirm.kt`
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingDraftRepository.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImpl.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModel.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationConfirmRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/route/SegmentationRoute.kt`
- Modify: `feature/segmentation/impl/src/main/java/com/teamyg/parfait/feature/segmentation/impl/screen/SegmentationConfirmScreen.kt`
- Test: `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModelTest.kt` (**기존 파일 확장** — 테스트 7건이 이미 있다. 덮어쓰지 말 것)
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingDraftRepositoryImplTest.kt` (확장)

**Interfaces:**
- Consumes: Task 5의 `NavigateToSegmentationConfirm(cutoutFilePath)`
- Produces:
  - `NavKeySegmentationConfirm(sourceImageUri: String?, subjectImagePath: String?, trimmedSubjectImagePath: String)`
  - `ToppingDraftRepository.record(subjectImagePath: String, cutoutImagePath: String?, borderColorArgb: Int?, borderWidthDp: Float?): Boolean`
  - `SegmentationConfirmViewModel.Factory.create(subjectImagePath: String, cutoutImagePath: String?, sourceImageUri: String?)`
  - `SegmentationConfirmState.sourceImageUri: String?` · `SegmentationConfirmState.isEditPhotoEnabled: Boolean`
  - `SegmentationConfirmScreen(… isEditPhotoEnabled: Boolean …)`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

기존 파일 `feature/segmentation/impl/src/test/java/com/teamyg/parfait/feature/segmentation/impl/viewmodel/SegmentationConfirmViewModelTest.kt`에
**테스트 2건을 더한다.** 그 파일에는 이미 7건이 있고 그중 `onEnter_writesNothing`이 이 라운드가
파는 예외의 반대편을 지키고 있다 — 통째로 덮어쓰면 안 된다.

기존 픽스처(`SUBJECT_PATH`·`CUTOUT_PATH`·`toppingDraftRepository`·`givenDraft`·`draft`·`viewModel`)를
그대로 쓰되, 재사용 진입은 인자가 다르므로 헬퍼 하나와 상수 하나를 더한다.

```kotlin
private const val REUSED_PATH = "/data/files/recent_images/b.png"

    private fun reuseViewModel() = SegmentationConfirmViewModel(
        subjectImagePath = REUSED_PATH,
        cutoutImagePath = null,
        toppingDraftRepository = toppingDraftRepository,
    )

    @Test
    fun reuseEntry_withEmptyDraft_recordsBeforeObserving() = runTest(mainDispatcherRule.dispatcher) {
        // Given 캔버스가 흐름은 열었지만 알맹이는 아직 없는 초안 — 최근 목록에서 고른 진입이다
        givenDraft(draft(subjectImagePath = null))

        // When 화면이 열린다
        reuseViewModel()
        advanceUntilIdle()

        // Then 구독보다 먼저 적는다. 뒤집으면 첫 방출의 null 이 DraftMissing 토스트를 쏴
        // 사용자가 없는 실패를 듣는다
        coVerify(exactly = 1) {
            toppingDraftRepository.record(REUSED_PATH, null, null, null)
        }
    }

    @Test
    fun reuseEntry_whenDraftAlreadyHasSubject_doesNotRecordAgain() =
        runTest(mainDispatcherRule.dispatcher) {
            // Given 이미 이 알맹이가 적힌 초안 — 프로세스 사망 복원으로 돌아온 자리다
            givenDraft(draft(subjectImagePath = REUSED_PATH, borderColorArgb = 0xFF00FF00.toInt()))

            // When 화면이 다시 열린다
            reuseViewModel()
            advanceUntilIdle()

            // Then 다시 적지 않는다 — record 는 테두리까지 통째로 덮어쓰므로 여기서 다시 적으면
            // 사용자가 방금 두른 테두리가 사라진다
            coVerify(exactly = 0) { toppingDraftRepository.record(any(), any(), any(), any()) }
        }
```

`draft()` 헬퍼에 `borderWidthDp` 인자를 넘기지 않아도 되도록 기본값이 이미 있다. 위 둘째 테스트는
테두리가 실제로 살아남는지까지 보려면 `assertEquals(0xFF00FF00.toInt(), viewModel.state.value.borderColorArgb)`를
덧붙인다.

`ToppingDraftRepositoryImplTest`에는 한 건을 더한다.

```kotlin
    @Test
    fun record_withNullCutoutPath_keepsDraftWritable() = runTest {
        // Given 흐름이 열려 있다
        givenStoredDraft(draft(subjectImagePath = null, cutoutImagePath = null))
        val repository = repository()

        // When 재편집 마스크 없이 알맹이만 적는다
        val recorded = repository.record(
            subjectImagePath = "/data/files/recent_images/b.png",
            cutoutImagePath = null,
            borderColorArgb = null,
            borderWidthDp = null,
        )

        // Then 적힌다 — 최근 목록에서 되살린 알맹이에는 마스크가 없다
        assertTrue(recorded)

        val saved = slot<ToppingDraft>()
        coVerify { toppingDraftLocalDataSource.save(capture(saved)) }
        assertEquals("/data/files/recent_images/b.png", saved.captured.subjectImagePath)
        assertNull(saved.captured.cutoutImagePath)
    }
```

> `draft` 흐름은 스텁이라 `save` 뒤에도 옛 값을 준다. 그래서 저장된 값은 `slot` 으로 잡는다 —
> 기존 테스트가 쓰는 방식과 같다.

- [ ] **Step 2: 테스트가 실패하는 것을 확인한다**

```bash
./gradlew :feature:segmentation:impl:testDebugUnitTest :data:testDebugUnitTest
```

Expected: 컴파일 실패 — `cutoutImagePath`가 non-null이다.

- [ ] **Step 3: NavKey와 초안 계약을 넓힌다**

`NavKeySegmentationConfirm.kt` 전문:

```kotlin
package com.teamyg.parfait.feature.segmentation.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 분리된 객체 이미지를 확인하는 화면.
 *
 * 최근 목록에서 되살린 알맹이로 들어오면 앞의 둘이 없다. 그때는 "사진 편집"이 잠긴다.
 *
 * @param sourceImageUri 원본 이미지. 여기서 수동 편집으로 넘어갈 때 지운 영역을 되살릴 재료로 쓴다
 * @param subjectImagePath 배경이 제거된 객체 이미지의 파일 경로
 * @param trimmedSubjectImagePath 위 이미지에서 투명한 여백을 걷어내 객체 크기만 남긴 파일 경로.
 */
@Serializable
data class NavKeySegmentationConfirm(
    val sourceImageUri: String?,
    val subjectImagePath: String?,
    val trimmedSubjectImagePath: String,
) : NavKey
```

`ToppingDraftRepository#record`의 `cutoutImagePath`를 `String?`로 바꾸고 `Impl`의 시그니처도
같이 바꾼다(본문은 그대로 `current.copy(...)`가 받는다).

- [ ] **Step 4: ViewModel이 재사용 진입에서 초안을 먼저 적게 한다**

`SegmentationConfirmViewModel`을 아래 뼈대로 바꾼다.

```kotlin
@HiltViewModel(assistedFactory = SegmentationConfirmViewModel.Factory::class)
class SegmentationConfirmViewModel
@AssistedInject constructor(
    @Assisted("subjectImagePath") subjectImagePath: String,
    @Assisted("cutoutImagePath") private val cutoutImagePath: String?,
    private val toppingDraftRepository: ToppingDraftRepository,
) : BaseViewModel<SegmentationConfirmState, SegmentationConfirmIntent, SegmentationConfirmEffect>(
    initialState = SegmentationConfirmState(
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
    ),
) {
    private var hasReportedMissingDraft = false

    init {
        launch(onError = { reportMissingDraft() }) {
            // 최근 목록에서 되살린 알맹이는 세그멘테이션을 타지 않아 초안을 적어 준 데가 없다.
            // 구독보다 먼저 적어야 첫 방출의 null 이 없는 실패를 알리지 않는다.
            // 초안이 이미 이 알맹이를 가리키면 건드리지 않는다 — record 는 테두리까지 통째로
            // 덮어쓰므로, 프로세스 사망 복원으로 화면이 다시 만들어질 때 두른 테두리가 사라진다
            val isReuseEntry = cutoutImagePath == null

            if (isReuseEntry && toppingDraftRepository.draft.first()?.subjectImagePath != subjectImagePath) {
                val recorded = toppingDraftRepository.record(
                    subjectImagePath = subjectImagePath,
                    cutoutImagePath = null,
                    borderColorArgb = null,
                    borderWidthDp = null,
                )
                if (!recorded) {
                    reportMissingDraft()
                    return@launch
                }
            }

            collectDraft()
        }
    }
```

`observeDraft()`의 `collect { … }` 본문을 `private suspend fun collectDraft()`로 옮기고
`init`의 옛 `observeDraft()` 호출은 지운다. 상태 갱신의 `cutoutImagePath` 자리는
`draft.cutoutImagePath ?: cutoutImagePath`를 그대로 둔다(둘 다 nullable이 된다).
추가할 import: `kotlinx.coroutines.flow.first`.

**판정 기준이 "초안이 비어 있는가"가 아니라 "초안이 이 알맹이를 가리키는가"인 이유**: 갤러리는
`goTo`로 쌓여 백스택에 남으므로, 알맹이 A를 고른 뒤 뒤로 가 B를 고르는 경로가 실재한다. "비어
있는가"로 판정하면 그때 초안에 A가 남아 있어 B를 적지 않고, 구독이 초안을 정본으로 삼아 **A가 배치된다.**
경로 비교로 두면 복원(경로가 같다)은 여전히 건너뛰고 새 알맹이는 적힌다.

이 판정이 읽는 `draft` 흐름은 `withExistingFilesOnly`로 걸러진다 — 초안이 가리키던 파일이 이미
지워졌으면 빈 초안으로 보이므로 재사용 진입이 다시 적는다. 그게 맞는 동작이다.

`SegmentationConfirmState.cutoutImagePath`를 `String?`로 바꾸고 **편집 가능 여부도 상태로 올린다** —
Route의 지역 변수로 두면 단위 테스트로 잡히지 않는데, 스펙 테스트 표가 "편집 버튼이 잠긴다"를
검증 항목으로 든다.

```kotlin
data class SegmentationConfirmState(
    val subjectImagePath: String,
    val cutoutImagePath: String?,
    val sourceImageUri: String?,
    …
) : UiState {
    /** 원본과 재편집 마스크가 둘 다 있어야 편집 화면이 지운 영역을 되살릴 수 있다 */
    val isEditPhotoEnabled: Boolean
        get() = sourceImageUri != null && cutoutImagePath != null
}
```

`sourceImageUri`는 assisted 인자로 하나 더 받아 초기 상태에 넣는다(Route가 `key.sourceImageUri`를
넘긴다). 팩토리 선언도 맞춘다.

```kotlin
    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("subjectImagePath") subjectImagePath: String,
            @Assisted("cutoutImagePath") cutoutImagePath: String?,
            @Assisted("sourceImageUri") sourceImageUri: String?,
        ): SegmentationConfirmViewModel
    }
```

Step 1의 두 테스트도 `reuseViewModel()` 헬퍼에 `sourceImageUri = null`을 함께 넘긴다. 기존 7건이
쓰는 `viewModel()` 헬퍼에는 `sourceImageUri = "content://media/1"` 같은 값을 넣어 편집 버튼이
열린 채로 두고, "재사용 진입에서는 `isEditPhotoEnabled`가 `false`"를 단언하는 테스트 한 건을 더한다.

- [ ] **Step 5: Route와 화면을 맞춘다**

`SegmentationConfirmRoute`:

```kotlin
    val viewModel = hiltViewModel<SegmentationConfirmViewModel, SegmentationConfirmViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                // 두 인자의 이름이 서로 반대 의미라 뒤바꾸기 쉽다(`ToppingEditResult` KDoc)
                subjectImagePath = key.trimmedSubjectImagePath,
                cutoutImagePath = key.subjectImagePath,
                sourceImageUri = key.sourceImageUri,
            )
        },
    )
```

편집 버튼 호출을 바꾼다. 판정은 상태가 갖고 있으므로 Route는 값을 읽기만 한다.

```kotlin
    val sourceImageUri = uiState.sourceImageUri
    val cutoutImagePath = uiState.cutoutImagePath
```

```kotlin
            isEditPhotoEnabled = uiState.isEditPhotoEnabled,
            onClickEditPhoto = {
                if (sourceImageUri == null || cutoutImagePath == null) return@SegmentationConfirmScreen

                navigator.goTo(
                    NavKeyToppingEdit(
                        sourceImageUri = sourceImageUri,
                        // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                        segmentationImageUri = File(cutoutImagePath).toUri().toString(),
                        borderLayers = uiState.borderLayers,
                    ),
                )
            },
```

`SegmentationConfirmScreen`에 `isEditPhotoEnabled: Boolean` 파라미터를 더하고 편집 버튼의
`isEnabled = true`를 `isEnabled = isEditPhotoEnabled`로 바꾼다. `@YGPreview` 호출부에
`isEditPhotoEnabled = true`를 더한다.

`SegmentationRoute`의 `NavKeySegmentationConfirm(...)` 호출은 인자 이름이 그대로라 바뀌지 않는다.

- [ ] **Step 6: 테스트와 빌드가 통과하는 것을 확인한다**

Task 5의 Step 7과 함께 돌린다.

```bash
./gradlew :domain:test :data:testDebugUnitTest :feature:segmentation:impl:testDebugUnitTest \
  :feature:gallery:impl:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest \
  ktlintCheck :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 7: 커밋**

```bash
git add feature/segmentation/ domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ \
  data/src/main/java/com/teamyg/parfait/data/repository/topping/ \
  data/src/test/java/com/teamyg/parfait/data/repository/topping/
git commit -m "feat: 최근 알맹이를 고르면 누끼 확인 화면으로 직행한다"
```

---

## Task 7: 문서에 as-built를 반영한다

> **이 태스크만 문서 저장소(`team-yg-pesonal-agent`)에서 한다.** 브랜치는
> `docs/c106-pr6-recent-cutout-reuse`이고 코드 저장소를 건드리지 않는다.

**Files:**
- Modify: `parfait/specs/archive/2026-08-20-c106-topping-place-api.md`
- Modify: `parfait/plans/README.md`
- Modify: `parfait/synthesis/open-questions.md`
- Move: 이 계획 파일을 `parfait/plans/archive/`로

- [ ] **Step 1: 스펙의 진행 상황을 갱신한다**

`## PR 분할 (스택)` 표 6번 행의 "사용자에게 보이는 변화" 칸에 실행 결과를 적는다 — 브랜치명·베이스
커밋·커밋 수·신규 테스트 수·머지 여부. 스펙 본문 「누끼 알맹이 재사용 (PR6)」 절에서 **구현이 계획과
갈린 자리**가 있으면 그 자리를 고친다(계획이 아니라 실제 코드가 정답이다).

- [ ] **Step 2: `plans/README.md`에 행을 더한다**

기존 PR5 행과 같은 형식으로 PR6 행을 만든다 — 상태(완료·미머지 여부), 태스크 수, 신규 테스트 수,
베이스 브랜치, 스펙 링크, 실행 중 갈린 결정.

- [ ] **Step 3: open-questions를 정리한다**

- OQ-P-255의 상태 문구에서 "구현은 PR6 라운드가 한다"를 실제 결과로 바꾼다.
- OQ-P-257(알맹이 셀 디자인)은 **열어 둔다.** 이 라운드는 `Fit`만 적용했다.
- 구현 중 새로 드러난 미결이 있으면 `<!-- oq-next: -->` 번호를 따라 신설하고 그 주석을 올린다.

- [ ] **Step 4: 계획 파일을 archive로 옮기고 frontmatter를 닫는다**

```bash
git mv parfait/plans/2026-08-21-c106-pr6-recent-cutout-reuse.md parfait/plans/archive/
```

frontmatter의 `status`를 `done`으로 바꾸고 `archived_reason`을 한 줄 적는다.

- [ ] **Step 5: 커밋**

```bash
git add parfait/
git commit -m "docs: C-106 PR6 누끼 알맹이 재사용 — as-built 반영"
```

---

## 실기기 확인 (이 브랜치가 지고 나가는 것)

PR3 5항목 + PR4 8항목 + PR5 9항목이 아직 미수행이고, 이 라운드가 아래를 더한다.

1. 토핑을 하나 올린 뒤 갤러리를 열면 "최근 업로드" 맨 앞에 **테두리 없는 알맹이**가 있다.
2. 그 셀이 잘리지 않는다(원본 사진 셀은 지금처럼 꽉 찬다).
3. 알맹이를 누르면 누끼 확인 화면이 곧바로 뜨고, **테두리가 없는 상태**로 시작한다.
4. 그 화면의 "사진 편집"이 비활성이고, "다음"은 눌린다.
5. 재사용 알맹이로 배치를 끝내면 캔버스에 올라간다(같은 알맹이가 최근 목록에 중복되지 않는다).
6. 앱을 지웠다 깔지 않고 **기존 최근 목록이 있는 상태로 업데이트**했을 때 그 목록이 그대로 보인다
   (2단 폴백이 실제로 걸리는지 보는 유일한 자리다).
7. 배경 선택(C-301) 경로로 갤러리를 열면 알맹이가 **안 보인다**.
8. 03시를 넘긴 뒤 다시 열면 알맹이도 원본과 같이 목록에서 사라진다.
