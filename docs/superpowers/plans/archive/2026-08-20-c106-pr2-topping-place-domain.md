---
id: c106-pr2-topping-place-domain
title: C-106 결선 PR2 — 토핑 배치 계층 (ToppingRepository·AddToppingUseCase)
status: done
type: work-order
created: 2026-08-20
updated: 2026-08-20
archived_reason: 구현 완료·develop 머지(PR #322 da03c9b0, 2026-08-20). 브랜치 feature/#270-topping-place-domain(PR1 커밋 포함).
platforms: android
owner: Parfait 팀
related_adr: ADR-0025, ADR-0026
related_spec: c106-topping-place-api
related_code:
  - ParfaitImageRemoteDataSource.kt#placeTopping
  - ParfaitImageRemoteDataSourceImpl.kt#placeTopping
  - PlacedToppingVO.kt
  - ToppingTransform.kt
  - ToppingBorder.kt
  - ImageType.kt
  - ImageUploadRepository.kt#upload
  - AppErrorMapper.kt#mapErrorToAppError
  - ParfaitRepositoryImpl.kt
  - PolicyRepositoryImplTest.kt
  - RepositoryModule.kt
tags: [plan, parfait, topping, canvas, domain]
---

# C-106 결선 PR2 — 토핑 배치 계층 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 확정된 `ImageId`를 캔버스 좌표에 배치하는 `ToppingRepository`를 만들고, PR1이 만든 업로드와 이 배치를 한 순서로 묶는 `AddToppingUseCase`를 만든다.

**Architecture:** DataSource(`ParfaitImageRemoteDataSource#placeTopping`)는 이미 있다. 이 PR이 더하는 것은 **그 위 두 층**이다 — `ApiException`을 `AppError`로 바꿔 도메인이 `:data`를 보지 않게 하는 Repository 한 겹, 그리고 업로드 → 배치 순서를 도메인 규칙으로 고정하는 UseCase 한 겹. **이 PR에도 소비자가 없다**(화면 결선은 PR5). 그래서 리뷰가 "계약 매핑" 한 가지에만 집중한다.

**Tech Stack:** Kotlin · Hilt · kotlinx-coroutines-test · MockK · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-20-c106-topping-place-api.md`](../../specs/archive/2026-08-20-c106-topping-place-api.md) — PR 분할 표 **2번 행**

> ✅ **실행 완료·미머지(2026-08-20)** — subagent-driven-development로 2 태스크, 태스크 리뷰 두 번 다
> **지적 0건 통과**(fix 라운드 0회). 브랜치 `feature/#270-topping-place-domain`(PR1 브랜치 위),
> 커밋 3개. 신규 테스트 **11건**(계획 예상과 같음), 검증 명령
> `:domain:test :data:testDebugUnitTest ktlintCheck :app:assembleDebug` 전부 통과. **머지·push 안 했다.**
>
> 최종 브랜치 리뷰(opus, base는 PR1 팁)는 **머지 가능 · Critical 0 · Important 0 · Minor 3**을 냈고,
> 셋을 fix 웨이브 한 번으로 닫았다. 그래서 계획이 예상한 커밋 2개가 아니라 3개다.
>
> 🔁 **2026-08-20 저녁, PR1 이 develop 위로 리베이스되면서 이 브랜치도 따라 옮겼다** —
> `git rebase --onto feature/#270-image-upload-transport 0ea4d9e9`, 충돌 0건.
> 커밋 3개는 `11c38113..4b780d17`(새 PR1 팁 `496d55f1` 위)이 됐다.
>
> **계획 텍스트와 갈린 것 셋:**
> - `ToppingRepositoryImpl`에서 `.mapErrorToAppError()`의 줄바꿈 위치가 계획 코드 블록과 다르다 —
>   ktlint `standard:chain-method-continuation`이 닫는 괄호와 같은 줄로 합치도록 강제한다.
> - `ToppingRepositoryBindingTest`가 `ToppingRepository` 하나가 아니라 **`ImageUploadRepository`까지
>   두 바인딩을 함께 단언한다.** `AddToppingUseCase`가 둘 다 주입받는데 감지선이 한쪽에만 있으면
>   PR1이 심은 바인딩이 사라져도 PR5 빌드까지 잠든다는 최종 리뷰 지적을 받았다.
> - 같은 테스트의 주석에서 "소비자가 0이라 이 단언이 유일한 선이다"를 걷어냈다 — 다음 PR이 소비자를
>   붙이면 거짓이 되는 상태 단정이다. 근거는 스펙 포인터로 옮겼다.
>
> **이월한 것 1건:** `AddToppingUseCase` 클래스 KDoc의 두 문장이 "포인터 한 줄" 지향보다 길다.
> 최종 리뷰가 "뒷문장은 코드가 말하지 않는 계약이고 PR5 재시도 정책이 여기 걸려 있다"고 판정해
> 그대로 뒀다.
>
> **계획 defect 0건** — 실행 전 서브에이전트 검수 두 번(코드 사실 대조·스펙 범위 대조)이 Important
> 4건을 미리 닫아 둔 것이 실제로 방어됐다. 그중 둘이 이 라운드의 감지선을 만들었다: Task 2의 인자
> 전달 테스트와 `@Binds` 리플렉션 테스트다. 검수 없이 실행했다면 둘 다 없는 채로 통과했을 것이다.

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**이고 이 문서가 사는 저장소가 아니다. 로컬 절대경로는 `wiki/personal-private/project-paths.md`에 있다.
- **베이스 브랜치는 `feature/#270-image-upload-transport`**(PR1, 팁 `0ea4d9e9`, **미머지**)다. 그 위에 새 브랜치 `feature/#270-topping-place-domain`을 만들어 작업한다. PR1의 커밋 6개는 그대로 둔다.
- **커밋은 태스크마다 한다.** `git push`·`gh pr create`·`gh pr merge`는 **하지 않는다** — 사용자 확인이 필요한 작업이다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 선언에 멀티라인을 강제한다**(`.editorconfig`의 `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than = 2`). 이 계획의 코드 블록은 이미 그 형태로 적혀 있으니 **한 줄로 줄이지 말 것.**
- **주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 근거는 문서를 가리킨다. 함정과 의도는 쓴다.
  - 아키텍처 결정 설명을 코드에 복사하지 않는다. 포인터 한 줄만 둔다.
- **`filePath`는 파일 시스템 절대경로**다. `file://` uri가 아니다.
- **좌표 변환은 이 PR이 하지 않는다.** `ToppingTransform`은 이미 정규화된 서버 좌표로 들어온다고 보고 그대로 넘긴다. 변환은 PR5가 `CanvasToppingPlaceViewModel`에서 한다.
- 매퍼 단독 테스트(`XxxVOMapperTest`)는 만들지 않는다. 변환 판단은 Repository·UseCase 테스트 케이스로 잠근다.
- ⚠️ **`:app:assembleDebug`는 이 PR의 DI 안전망이 아니다.** 저장소에 `dagger.fullBindingGraphValidation` 설정이 없고 Dagger는 기본값에서 **엔트리포인트로부터 도달 가능한 바인딩만** 검증한다. 이 PR에도 소비자가 0이라 `@Binds`를 빠뜨려도 통과한다. `assembleDebug`가 보는 것은 KSP·Hilt 코드 생성이 깨지지 않는지까지다.
- 검증 명령(태스크마다 전부 통과해야 한다):
  ```bash
  ./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
  ```
  마지막 태스크에서만 `./gradlew :app:assembleDebug`까지 돌린다. `:domain`은 JVM 모듈이라 `:domain:test`이고 `:data`는 안드로이드 모듈이라 `:data:testDebugUnitTest`다 — 헷갈리면 태스크가 통째로 안 돈다.

## 파일 구성

| 파일 | 책임 |
|---|---|
| `domain/repository/topping/ToppingRepository.kt` (신규) | 배치 도메인 계약 |
| `data/repository/topping/ToppingRepositoryImpl.kt` (신규) | DataSource 위임 + `AppError` 변환 |
| `data/di/RepositoryModule.kt` (수정) | 위 구현 바인딩 |
| `domain/usecase/topping/AddToppingUseCase.kt` (신규) | 업로드 → 배치 조율 |

---

### Task 1: 배치 Repository

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingRepository.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImplTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/di/ToppingRepositoryBindingTest.kt`

**Interfaces:**
- Consumes: `ParfaitImageRemoteDataSource.placeTopping(groupId: GroupId, parfaitId: ParfaitId, imageId: ImageId, transform: ToppingTransform, border: ToppingBorder): Result<PlacedToppingVO>` (기존)
- Produces: `ToppingRepository.place(groupId: GroupId, parfaitId: ParfaitId, imageId: ImageId, transform: ToppingTransform, border: ToppingBorder): Result<PlacedToppingVO>`

- [ ] **Step 0: 브랜치를 만든다**

```bash
git checkout feature/#270-image-upload-transport
git checkout -b feature/#270-topping-place-domain
```

- [ ] **Step 1: 실패 테스트 작성**

`data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImplTest.kt`:

```kotlin
package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ToppingRepositoryImplTest {
    private val parfaitImageRemoteDataSource: ParfaitImageRemoteDataSource = mockk()
    private val repository = ToppingRepositoryImpl(parfaitImageRemoteDataSource)

    private val transform = ToppingTransform(
        positionX = 0.5,
        positionY = 0.25,
        positionZ = 3,
        scale = 1.2,
        rotation = 15.0,
    )

    private val border = ToppingBorder.Solid(color = "#FFFF6B6B", width = 4.0)

    private val placed = PlacedToppingVO(
        parfaitImageId = ParfaitImageId(42L),
        imageId = IMAGE_ID,
        imageUrl = "https://cdn.example.com/nukki.png",
        transform = transform,
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(10L),
            nickname = GroupNickname("연경이"),
        ),
    )

    private suspend fun place() = repository.place(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        imageId = IMAGE_ID,
        transform = transform,
        border = border,
    )

    @Test
    fun place_dataSourceSucceeds_returnsSameValue() = runTest {
        // Given 원격 데이터소스가 배치 결과를 준다
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        } returns Result.success(placed)

        // When 배치한다
        val result = place()

        // Then 값을 가공 없이 그대로 전달한다
        assertEquals(placed, result.getOrThrow())
    }

    @Test
    fun place_onceCalled_forwardsEveryArgumentVerbatim() = runTest {
        // Given 원격 데이터소스가 배치 결과를 준다
        val sentTransform = slot<ToppingTransform>()
        val sentBorder = slot<ToppingBorder>()
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(
                groupId = GROUP_ID,
                parfaitId = PARFAIT_ID,
                imageId = IMAGE_ID,
                transform = capture(sentTransform),
                border = capture(sentBorder),
            )
        } returns Result.success(placed)

        // When 배치한다
        place()

        // Then 좌표와 테두리가 손대지 않은 채 그대로 나간다 — 이 층은 에러 변환만 한다.
        // 테두리를 흘리면 서버는 200 을 주고 캔버스에서 테두리만 조용히 사라진다
        coVerify(exactly = 1) {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        }
        assertEquals(transform, sentTransform.captured)
        assertEquals(border, sentBorder.captured)
    }

    @Test
    fun place_dataSourceFailsWithBusiness_convertsToAppErrorServer() = runTest {
        // Given 마감된 파르페에 올리려 한다
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        } returns Result.failure(
            ApiException.Business(
                code = "PARFAIT_ALREADY_CLOSED",
                serverMessage = "이미 마감된 파르페입니다",
                statusCode = 409,
                errorDetail = null,
            ),
        )

        // When 배치한다
        val result = place()

        // Then 코드와 상태 코드가 함께 살아 있다 — 화면이 둘을 같이 봐야 되감기를 판정한다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_ALREADY_CLOSED", error.code)
        assertEquals(409, error.statusCode)
    }

    @Test
    fun place_dataSourceFailsWithNetwork_convertsToAppErrorNetwork() = runTest {
        // Given 연결이 끊긴다
        coEvery {
            parfaitImageRemoteDataSource.placeTopping(any(), any(), any(), any(), any())
        } returns Result.failure(ApiException.Network(cause = IOException("connection reset")))

        // When 배치한다
        val result = place()

        // Then ApiException 이 도메인까지 새지 않는다
        assertIs<AppError.Network>(result.exceptionOrNull())
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
        val IMAGE_ID = ImageId(3L)
    }
}
```

- [ ] **Step 1-b: 바인딩 감지선 테스트를 작성한다**

Global Constraints가 적었듯 이 PR은 소비자가 0이라 **`@Binds`를 빠뜨려도 `:app:assembleDebug`가 통과한다.** 그러면 누락은 PR5에서야 Hilt 빌드 실패로 드러난다. PR1이 `UploadOkHttpClientTest`로 만든 것과 같은 성격의 감지선을 여기서도 하나 둔다.

`data/src/test/java/com/teamyg/parfait/data/di/ToppingRepositoryBindingTest.kt`:

```kotlin
package com.teamyg.parfait.data.di

import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class ToppingRepositoryBindingTest {
    @Test
    fun repositoryModule_bindsToppingRepository() {
        // Given·When 모듈이 선언한 바인딩의 반환 타입을 본다
        val boundTypes = RepositoryModule::class.java.methods.map { method -> method.returnType }

        // Then 배치 Repository 가 그중에 있다. 소비자가 0 이라 Dagger 가 이 그래프를 검증하지
        // 않으므로 이 단언이 누락을 잡는 유일한 선이다
        assertTrue(ToppingRepository::class.java in boundTypes)
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "com.teamyg.parfait.data.repository.topping.ToppingRepositoryImplTest" --tests "com.teamyg.parfait.data.di.ToppingRepositoryBindingTest"
```

Expected: 컴파일 실패 — `Unresolved reference: ToppingRepositoryImpl`(`ToppingRepository` 도 아직 없어 바인딩 테스트도 같은 이유로 깨진다)

- [ ] **Step 3: 도메인 계약을 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingRepository.kt`:

```kotlin
package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform

interface ToppingRepository {
    /**
     * 업로드가 확정된 이미지를 파르페 위 좌표에 배치한다.
     *
     * 이미 배치된 imageId 로 다시 부를 때의 upsert 와 소유자 이전은 `api/parfait-image.md` 참고.
     *
     * @param transform 화면 좌표가 아니라 정규화된 서버 좌표다.
     */
    suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO>
}
```

- [ ] **Step 4: 구현을 만든다**

`data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImpl.kt`:

```kotlin
package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.error.mapErrorToAppError
import com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

/**
 * 위임만 하는 것처럼 보여도 [mapErrorToAppError] 때문에 이 층이 필요하다 — 여기서
 * `ApiException` 을 `AppError` 로 바꿔야 domain·feature 가 `:data` 를 보지 않는다.
 */
class ToppingRepositoryImpl @Inject constructor(
    private val parfaitImageRemoteDataSource: ParfaitImageRemoteDataSource,
) : ToppingRepository {
    override suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO> = parfaitImageRemoteDataSource
        .placeTopping(
            groupId = groupId,
            parfaitId = parfaitId,
            imageId = imageId,
            transform = transform,
            border = border,
        )
        .mapErrorToAppError()
}
```

> 클래스 KDoc 한 줄은 `ParfaitRepositoryImpl`·`PolicyRepositoryImpl`에 이미 있는 문장과 같다. 위임만 하는 층이 왜 필요한지를 파일마다 되묻게 되는 자리라 저장소가 그렇게 유지해 온 것이고, 그 설명의 정본은 `AppErrorMapper`의 KDoc이다. **줄이거나 새로 쓰지 말고 그대로 맞춘다.**

- [ ] **Step 5: DI 바인딩을 더한다**

`RepositoryModule.kt`의 import 블록은 알파벳 순으로 유지돼 있다(ktlint의 `import-ordering`은 꺼져 있어 빌드가 깨지지는 않는다). 자리를 지켜 둘을 더한다 — `data.repository.policy.PolicyRepositoryImpl` 다음에 하나, `domain.repository.policy.PolicyRepository` 다음에 하나다:

```kotlin
import com.teamyg.parfait.data.repository.topping.ToppingRepositoryImpl
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
```

인터페이스 본문 끝(`bindImageUploadRepository` 아래)에 바인딩을 더한다:

```kotlin
    @Binds
    @Singleton
    fun bindToppingRepository(toppingRepositoryImpl: ToppingRepositoryImpl): ToppingRepository
```

- [ ] **Step 6: 테스트를 돌려 통과를 확인한다**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck
```

Expected: PASS

- [ ] **Step 7: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/repository/topping/ToppingRepository.kt \
        data/src/main/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImpl.kt \
        data/src/main/java/com/teamyg/parfait/data/di/RepositoryModule.kt \
        data/src/test/java/com/teamyg/parfait/data/repository/topping/ToppingRepositoryImplTest.kt \
        data/src/test/java/com/teamyg/parfait/data/di/ToppingRepositoryBindingTest.kt
git commit -m "feat(topping): 배치 ToppingRepository 를 추가한다

DataSource 에 배치·수정·삭제·테두리 넷이 있지만 이 라운드가 쓰는 place 만 올린다.
쓰지 않는 것을 올리면 계약이 바뀌어도 아무도 고치지 않는다.
좌표와 테두리는 손대지 않고 그대로 넘긴다 — 이 층은 에러 변환만 한다."
```

---

### Task 2: 업로드와 배치를 묶는 AddToppingUseCase

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCaseTest.kt`

**Interfaces:**
- Consumes:
  - `ImageUploadRepository.upload(filePath: String, imageType: ImageType): Result<ImageId>` (PR1)
  - `ToppingRepository.place(groupId: GroupId, parfaitId: ParfaitId, imageId: ImageId, transform: ToppingTransform, border: ToppingBorder): Result<PlacedToppingVO>` (Task 1)
- Produces: `AddToppingUseCase.invoke(groupId: GroupId, parfaitId: ParfaitId, filePath: String, transform: ToppingTransform, border: ToppingBorder): Result<PlacedToppingVO>`

- [ ] **Step 1: 실패 테스트 작성**

`domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCaseTest.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AddToppingUseCaseTest {
    private val imageUploadRepository: ImageUploadRepository = mockk()
    private val toppingRepository: ToppingRepository = mockk()
    private val addTopping = AddToppingUseCase(
        imageUploadRepository = imageUploadRepository,
        toppingRepository = toppingRepository,
    )

    private val transform = ToppingTransform(
        positionX = 0.5,
        positionY = 0.25,
        positionZ = 3,
        scale = 1.2,
        rotation = 15.0,
    )

    private val border = ToppingBorder.Solid(color = "#FFFF6B6B", width = 4.0)

    private val placed = PlacedToppingVO(
        parfaitImageId = ParfaitImageId(42L),
        imageId = CONFIRMED_IMAGE_ID,
        imageUrl = "https://cdn.example.com/nukki.png",
        transform = transform,
        placedBy = ToppingPlacerVO(
            groupMemberId = GroupMemberId(10L),
            nickname = GroupNickname("연경이"),
        ),
    )

    private fun givenBothStepsSucceed() {
        coEvery { imageUploadRepository.upload(any(), any()) } returns Result.success(CONFIRMED_IMAGE_ID)
        coEvery {
            toppingRepository.place(any(), any(), any(), any(), any())
        } returns Result.success(placed)
    }

    // 프로퍼티 addTopping 과 이름을 나눠 둔다. 겹치면 호출부가 어느 쪽을 부르는지 읽기 어렵다
    private suspend fun addToppingWithFixtures() = addTopping(
        groupId = GROUP_ID,
        parfaitId = PARFAIT_ID,
        filePath = FILE_PATH,
        transform = transform,
        border = border,
    )

    @Test
    fun invoke_bothStepsSucceed_returnsPlacedTopping() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()

        // When 토핑을 추가한다
        val result = addToppingWithFixtures()

        // Then 배치 결과가 그대로 나온다
        assertEquals(placed, result.getOrThrow())
    }

    @Test
    fun invoke_bothStepsSucceed_placesTheConfirmedImageId() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()
        val placedImageId = slot<ImageId>()
        coEvery {
            toppingRepository.place(any(), any(), capture(placedImageId), any(), any())
        } returns Result.success(placed)

        // When 토핑을 추가한다
        addToppingWithFixtures()

        // Then 업로드가 돌려준 id 로 배치한다 — 두 단계를 잇는 유일한 값이다
        assertEquals(CONFIRMED_IMAGE_ID, placedImageId.captured)
    }

    @Test
    fun invoke_bothStepsSucceed_forwardsEveryArgumentVerbatim() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()
        val sentFilePath = slot<String>()
        val sentTransform = slot<ToppingTransform>()
        val sentBorder = slot<ToppingBorder>()
        coEvery {
            imageUploadRepository.upload(capture(sentFilePath), any())
        } returns Result.success(CONFIRMED_IMAGE_ID)
        coEvery {
            toppingRepository.place(
                groupId = GROUP_ID,
                parfaitId = PARFAIT_ID,
                imageId = any(),
                transform = capture(sentTransform),
                border = capture(sentBorder),
            )
        } returns Result.success(placed)

        // When 토핑을 추가한다
        addToppingWithFixtures()

        // Then 캔버스 식별값·경로·좌표·테두리가 손대지 않은 채 그대로 나간다. 이 층이 하는 일은
        // 순서를 정하는 것뿐인데, 값을 지어내면 서버는 200 을 주고 엉뚱한 자리에 토핑이 앉는다
        assertEquals(FILE_PATH, sentFilePath.captured)
        assertEquals(transform, sentTransform.captured)
        assertEquals(border, sentBorder.captured)
    }

    @Test
    fun invoke_bothStepsSucceed_uploadsAsNukki() = runTest {
        // Given 업로드와 배치가 모두 성공한다
        givenBothStepsSucceed()
        val uploadedType = slot<ImageType>()
        coEvery {
            imageUploadRepository.upload(any(), capture(uploadedType))
        } returns Result.success(CONFIRMED_IMAGE_ID)

        // When 토핑을 추가한다
        addToppingWithFixtures()

        // Then 용도는 호출부가 고르지 않는다. BACKGROUND 로 올라가면 객체가 엉뚱한 S3 접두사에
        // 앉는데 배치는 그것을 검사하지 않아 아무 실패도 드러나지 않는다
        assertEquals(ImageType.NUKKI, uploadedType.captured)
    }

    @Test
    fun invoke_uploadFails_doesNotPlace() = runTest {
        // Given 업로드가 실패한다
        coEvery { imageUploadRepository.upload(any(), any()) } returns Result.failure(
            AppError.Network(cause = null),
        )

        // When 토핑을 추가한다
        val result = addToppingWithFixtures()

        // Then 배치를 부르지 않는다 — 올라가지 않은 이미지의 id 가 없다
        assertIs<AppError.Network>(result.exceptionOrNull())
        coVerify(exactly = 0) { toppingRepository.place(any(), any(), any(), any(), any()) }
    }

    @Test
    fun invoke_placeFails_propagatesErrorUnchanged() = runTest {
        // Given 업로드는 되고 마감된 파르페라 배치가 거절된다
        givenBothStepsSucceed()
        coEvery {
            toppingRepository.place(any(), any(), any(), any(), any())
        } returns Result.failure(
            AppError.Server(
                code = "PARFAIT_ALREADY_CLOSED",
                statusCode = 409,
                serverMessage = "이미 마감된 파르페입니다",
            ),
        )

        // When 토핑을 추가한다
        val result = addToppingWithFixtures()

        // Then 코드가 살아서 올라온다. 화면이 이 코드로 되감기를 판정한다
        val error = assertIs<AppError.Server>(result.exceptionOrNull())
        assertEquals("PARFAIT_ALREADY_CLOSED", error.code)
    }

    private companion object {
        val GROUP_ID = GroupId(1L)
        val PARFAIT_ID = ParfaitId(2L)
        val CONFIRMED_IMAGE_ID = ImageId(99L)
        const val FILE_PATH = "/data/user/0/com.teamyg.parfait/cache/segmentation/subject.png"
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

```bash
./gradlew :domain:test --tests "com.teamyg.parfait.domain.usecase.topping.AddToppingUseCaseTest"
```

Expected: 컴파일 실패 — `Unresolved reference: AddToppingUseCase`

- [ ] **Step 3: UseCase를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt`:

```kotlin
package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

/**
 * 누끼 이미지를 올리고 그 결과를 캔버스에 배치한다.
 *
 * 두 단계의 순서는 서버 계약이 정한 도메인 규칙이지 화면 관심사가 아니라 여기서 조율한다
 * (`specs/archive/2026-08-20-c106-topping-place-api.md`). 배치까지 마치지 못해도 앞 단계를 되돌리지
 * 않는 것이 같은 스펙의 결정이다.
 */
class AddToppingUseCase @Inject constructor(
    private val imageUploadRepository: ImageUploadRepository,
    private val toppingRepository: ToppingRepository,
) {
    /**
     * @param filePath 파일 시스템 절대경로다. `file://` uri 가 아니다.
     * @param transform 화면 좌표가 아니라 정규화된 서버 좌표다.
     */
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        filePath: String,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO> {
        // 용도를 파라미터로 열지 않는다. 잘못 고르면 객체가 엉뚱한 S3 접두사에 앉는데
        // 배치는 그것을 검사하지 않아 아무 실패도 드러나지 않는다(api/image.md 키 규칙)
        val imageId = imageUploadRepository
            .upload(filePath = filePath, imageType = ImageType.NUKKI)
            .getOrElse { throwable -> return Result.failure(throwable) }

        return toppingRepository.place(
            groupId = groupId,
            parfaitId = parfaitId,
            imageId = imageId,
            transform = transform,
            border = border,
        )
    }
}
```

- [ ] **Step 4: 전체 검증**

```bash
./gradlew :domain:test :data:testDebugUnitTest ktlintCheck :app:assembleDebug
```

Expected: 전부 PASS

- [ ] **Step 5: 커밋**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt \
        domain/src/test/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCaseTest.kt
git commit -m "feat(topping): 업로드와 배치를 묶는 AddToppingUseCase 를 추가한다

업로드가 실패하면 배치를 부르지 않는다 - 올라가지 않은 이미지의 id 가 없다.
ImageType 은 파라미터가 아니라 여기서 정한다. 잘못 고르면 객체가 엉뚱한 S3
접두사에 앉는데 배치가 그것을 검사하지 않아 아무 실패도 드러나지 않는다."
```

---

## 완료 조건

- `./gradlew :domain:test :data:testDebugUnitTest ktlintCheck :app:assembleDebug` 전부 통과
- 신규 테스트 **11건**(Task 1: 5 · Task 2: 6)
- 커밋 2개, push·PR 없음
- 기존 파일 변경은 `RepositoryModule` 하나뿐이고 **기존 동작은 한 줄도 바뀌지 않는다**
- **사용자에게 보이는 변화 0** — 소비자가 없다. 실기기로 밟을 화면이 없어 수동 확인 항목도 없다

## 이 PR에서 하지 않는 것

- 화면 결선·좌표 변환·로딩·토스트·되감기(PR5) · 초안 SSOT(PR3) · 테두리 계약 전환(PR4)
- **토핑 수정·삭제·테두리 재편집** — DataSource에 넷 다 있으나 소비 화면이 C-301 라운드다. 쓰지 않는 것을 Repository로 올리면 계약이 바뀌어도 아무도 고치지 않는다
- **배경 이미지 업로드**(C-301) — 같은 `ImageUploadRepository`를 쓰지만 결선은 그 라운드 몫이다
- **`PARFAIT_ALREADY_CLOSED`·`GROUP_NOT_JOINED`·`PARFAIT_NOT_FOUND` 세 코드의 되감기 판정** — 이 층은 `AppError.Server`로 바꿔 올리기만 한다. 어느 코드에서 화면을 되감을지는 PR5가 정한다
- **PR5 선행 미결 둘**(OQ-P-109 발급 응답 본문 로깅 · OQ-P-246 업로드가 코루틴 취소를 안 따라감) — 이 PR도 소비자가 0이라 아직 아무것도 새지 않는다. 스펙 PR 표 5번 행이 정본이다
- `positionZ` 산출(흐름 진입 시 최대 z + 1) — 초안이 그 값을 들고 있어야 하므로 PR3·PR5 몫이다
