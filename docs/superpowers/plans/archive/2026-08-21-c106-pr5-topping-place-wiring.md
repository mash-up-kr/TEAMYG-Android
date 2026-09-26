---
id: c106-pr5-topping-place-wiring
title: C-106 결선 PR5 — 배치 결선 (좌표 변환·업로드 호출·로딩·되감기)
status: done
type: work-order
created: 2026-08-21
updated: 2026-08-21
archived_reason: 구현 완료·develop 머지(2026-08-22, PR #334 19cb5299). 브랜치 feature/#270-topping-place-wiring 에 커밋 10개(17a2c09f..c443846b), 신규 테스트 29건 + 삭제 1건 — 최종 브랜치 리뷰 지적 8건과 주석 정리를 포함한 as-built 수치. 아래 "as-built 정정" 절 참고.
platforms: android
owner: Parfait 팀
related_adr: ADR-0025, ADR-0026
related_spec: c106-topping-place-api, image-api-service-layer, parfait-canvas-topping-member-api-service-layer, ygscaffold-v2-common-loading-error
related_code:
  - CanvasToppingPlaceViewModel.kt#handleOnClickConfirm
  - CanvasToppingPlaceRoute.kt#CanvasToppingPlaceRoute
  - CanvasToppingPlaceScreen.kt#CanvasToppingPlaceScreen
  - AddToppingUseCase.kt#invoke
  - ImageUploadRepositoryImpl.kt#upload
  - PresignedUploadDataSourceImpl.kt#put
  - NetworkModule.kt#loggingInterceptor
  - ImageService.kt#postImages
  - ServerErrorCode.kt#Parfait
  - ServerErrorCode.kt#ParfaitGroup
  - ServerErrorCode.kt#ParfaitImage
  - String.kt#toColorOrNull
  - CanvasToppingLayer.kt#TOPPING_BASE_LONG_SIDE_RATIO
  - CanvasToppingLayer.kt#CanvasTopping
  - YGCanvas.kt#CANVAS_AREA_ASPECT_RATIO
  - ToppingDraftRepository.kt#clear
tags: [plan, parfait, topping, canvas, api, c-106]
---

# C-106 결선 PR5 — 배치 결선 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** C-106 확인 버튼을 누르면 토핑이 **실제로 서버에 올라가게 한다.** 화면 좌표를 서버 좌표로 바꾸고, `AddToppingUseCase`를 불러 업로드·배치를 마치고, 로딩 오버레이·실패 토스트·영구 실패 되감기를 붙이고, 성공하면 초안을 비운 뒤 캔버스로 되감는다.

**Architecture:** 배치 확정의 4단계(발급 → S3 PUT → confirm → 배치)는 이미 PR1·PR2가 `ImageUploadRepository`와 `AddToppingUseCase`로 쌓아 두었고 이 라운드가 **처음으로 그것을 부른다.** 그래서 계층을 새로 만드는 일보다 **잠들어 있던 결함을 깨우는 일**이 이 라운드의 절반이다 — 발급 응답 본문 로깅(OQ-P-109)과 업로드 취소 미전파(OQ-P-246)가 소비자가 생기는 순간 살아난다. 화면 쪽은 `CanvasToppingPlaceViewModel`이 캔버스 실측과 토핑 원본 크기를 둘 다 아는 유일한 자리라 좌표 변환이 거기서 일어나고, 계산 자체는 순수 함수로 뽑아 읽기 쪽 식으로 되돌리는 **왕복 테스트**가 잠근다. 실패 분기는 `AppError.Server.code` 하나로 판정한다(아래 결정 1번).

**Tech Stack:** Kotlin · Hilt · Jetpack Compose · Coil3 · OkHttp5 · Retrofit · DataStore Preferences · kotlinx-coroutines-test · MockK · Turbine · MockWebServer · kotlin.test

**Spec:** [`parfait/specs/archive/2026-08-20-c106-topping-place-api.md`](../../specs/archive/2026-08-20-c106-topping-place-api.md) — PR 분할 표 **5번 행**, 「좌표 변환」·「실패 처리」·「표시·제어 규칙」 절

> **베이스는 PR4 브랜치다.** `feature/#270-topping-border-contract`(팁 `17a2c09f`, **미머지**).
> 그 아래로 PR3 `feature/#270-topping-draft-ssot`가 깔려 있고, PR1·PR2는 develop에 머지됐다(`da03c9b0`).
> 새 브랜치 `feature/#270-topping-place-wiring`을 PR4 팁 위에 만든다.
> 🔁 이 문서의 스택 해시는 **2026-08-22 리베이스 뒤 기준**이다 — 스택 넷을 develop `ef55a58c` 위로
> 다시 쌓았고 기록은 [PR3 계획서](2026-08-20-c106-pr3-topping-draft-ssot.md)에 있다.

> ✅ **2026-08-22 develop 머지**(PR #334 `19cb5299`) — 스택 넷이 머지 커밋 하나로 함께 들어왔다.
> 머지 트리가 PR6 브랜치 팁 `656cbf2e`와 같아 충돌 해소 편집이 0건이고, 위 as-built 수치는
> 재측정 없이 그대로 develop 사실이다.

## 사용자에게 보이는 변화 (예고)

1. **토핑이 실제로 서버에 올라간다.** 확인을 누르면 로딩 오버레이가 덮이고, 끝나면 캔버스로
   돌아가면서 방금 만든 토핑이 목록에 함께 내려온다(`CanvasMainViewModel#handleEnter`가 이미
   `loadTodayCanvas()`를 부른다).
2. **확인 버튼이 그림이 뜨기 전에는 아무 일도 하지 않는다.** 비활성 *표현*은 여전히 없다
   (아래 결정 3번) — 누를 수는 있지만 확정이 나가지 않는다.
3. **실패하면 토스트가 뜬다.** 세 가지 영구 실패(마감·그룹 미참여·파르페 없음)는 알린 뒤
   캔버스로 되감고, 나머지는 화면에 남아 다시 시도할 수 있다.
4. **첫 실기기 확인 라운드다.** PR1·PR2는 소비자가 0이라 밟을 화면이 없었고, PR3·PR4의 실기기
   확인 13항목(PR3 5 + PR4 8)이 아직 안 됐다. 그것들이 이 브랜치와 함께 나간다.

## 스펙과 갈린 결정 셋 (실행 전에 읽는다)

1. **되감기 판정은 `code` 단독으로 한다. `statusCode`는 보지 않는다.** 스펙의 「실패 처리」 절은
   "`code`와 `statusCode`를 **함께** 본다"고 적었고, 그것이 판정 불가가 되는 갈래를 OQ-P-247이
   열어 두었다(서버가 HTTP 200에 실패 봉투를 실으면 `ApiCaller#runCatchingApi`가
   `statusCode = null`을 채운다). **OQ-P-247 ③을 먼저 확인한 결과 계약 스냅샷에 그 사례가 없다** —
   [`parfait/api/parfait-image.md`](../../../api/parfait-image.md)의 네 엔드포인트 실패 표가 전부
   403/404/409이고 성공만 200이다. 그래도 `statusCode`를 조건에 넣지 않는 이유는 따로 있다:
   **되감기 대상 세 코드에는 status로 갈려야 하는 동명 코드가 없다.** `GROUP_NOT_JOINED`는
   `ParfaitGroupApiErrorCode`에만 있고, `PARFAIT_ALREADY_CLOSED`는 `ParfaitErrorCode`에만 있으며,
   `PARFAIT_NOT_FOUND`는 두 enum이 공유하지만 **둘 다 404이고 뜻도 "이 파르페에는 못 쓴다"로
   같아** 갈릴 이유가 없다. `statusCode`를 조건에 넣으면 null 갈래에서 사용자가 영원히 실패하는
   재시도만 반복하게 된다. → **OQ-P-247을 ①안으로 닫는다.**
2. **취소로 생기는 고아 이미지는 감수한다(OQ-P-248).** 업로드가 `COMPLETED`까지 간 뒤 배치 전에
   취소되면 서버에 이미지만 남는다. 스펙의 재시도 결정이 이미 고아 S3 객체를 감수하기로 했으므로
   같은 처분이다. ①(취소가 예외로 올라오는 것)은 `BaseViewModel.launch`가
   `CancellationException`을 재던지고 그 시점엔 화면이 이미 사라진 뒤라 **화면이 할 일이 없다.**
   ③(z 겹침)은 서버가 유일성을 요구하지 않아 거부되지 않는다. **코드 변경 없이 문서로 닫고**
   `AddToppingUseCase` KDoc에 함정 한 줄만 남긴다(Task 7).
3. **확인 버튼 비활성 *표현*은 이 라운드에서도 만들지 않는다.** `YGFloatingBarEdit`의 확인
   버튼에 `enabled` 파라미터가 없고 비활성 디자인 근거도 없다(PR4가 같은 이유로 미뤘다).
   대신 **판정을 ViewModel이 갖고 조건이 안 맞으면 확정을 흘려보내지 않는다.** 판정 근거는
   스펙이 정한 대로 **painter 상태**다 — 화면이 `AsyncImagePainter.State.Success`를
   `OnToppingImageReadyChanged`로 올려보낸다. 이미 있는 `toppingBaseSize != null`에 기대지
   않는 이유: PR4가 실측 방출을 `isToppingImageLoaded`로 막아 두어 지금은 결과적으로 같지만,
   그 가드는 화면 쪽 `LaunchedEffect` 한 줄이라 **누가 걷어도 컴파일도 테스트도 안 깨진다.**
   판정 근거를 ViewModel 자기 어휘로 두면 그 결합이 끊긴다.

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`**이고 이 문서가 사는 저장소가 아니다. 로컬 절대경로는
  `wiki/personal-private/project-paths.md`에 있다(Task 7만 이 문서 저장소에서 한다).
- **베이스 브랜치는 `feature/#270-topping-border-contract`**(PR4, 팁 `17a2c09f`, 미머지)다.
  그 위에 새 브랜치 `feature/#270-topping-place-wiring`을 만들어 작업한다.
- **워크트리를 만들지 않는다.** 본 체크아웃에서 브랜치로 작업한다.
- **커밋은 태스크마다 한다.** `git push`·`gh pr create`·`gh pr merge`는 **하지 않는다** —
  사용자 확인이 필요한 작업이다.
- ⚠️ **새 ViewModel 테스트는 `runTest(mainDispatcherRule.dispatcher)`로 연다.**
  `MainDispatcherRule`의 KDoc이 이유를 못 박아 두었다 — 인자 없이 부르면 스케줄러가 갈려
  `advanceUntilIdle()`이 Main 큐를 못 비운다.
- ⚠️ **ktlint가 미사용 import를 실패로 잡는다**(`ktlint_standard_no-unused-imports`). 그래서 이
  계획은 태스크마다 **추가할 import와 지울 import를 명시한다.** 건너뛰면 `ktlintCheck`에서 멈춘다.
- ⚠️ **ktlint가 파라미터 2개 이상인 함수 선언에 멀티라인을 강제한다**(`.editorconfig`). 코드
  블록을 한 줄로 줄이지 말 것. 최대 줄 길이는 120이다.
- **주석·KDoc 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - **다른 컴포넌트의 현재 상태를 단정하지 않는다**(낡는다). 근거는 문서를 가리킨다.
    함정과 의도는 쓴다.
  - 아키텍처 결정 설명을 코드에 복사하지 않는다. 포인터 한 줄만 둔다.
- **초안이 담는 이미지 경로는 파일 시스템 절대경로**다. `file://` uri가 아니다.
  `ImageUploadRepository.upload`도 절대경로를 받는다.
- **테두리 색의 직렬화 형식은 `#RRGGBB` 6자리다**(계획 당시엔 8자리로 적었으나, PR5 브랜치
  리뷰에서 8자리가 ARGB·RGBA 두 관례로 갈려 iOS·CSS와 어긋나는 것이 드러나 6자리로 정정됐다).
  형식이 어긋나면 읽기 쪽 `String#toColorOrNull`이 `null`을 내고 캔버스가 **테두리를 그냥 안
  그린다** — 서버는 200을 주고 어디에도 로그가 남지 않는 무증상 실패다.
- **실패 알림은 시스템 `Toast`가 아니라 `YGScaffoldV2`의 `toastPolicy`로 띄운다.**
- ⚠️ **`ImageType`을 화면이 정하지 않는다.** `AddToppingUseCase`가 `NUKKI`로 못 박아 두었다.
  파라미터로 열면 객체가 무증상으로 엉뚱한 S3 접두사에 앉는다.
- 매퍼 단독 테스트(`XxxVOMapperTest`)는 만들지 않는다.
- 검증 명령(태스크마다 해당하는 것을 전부 통과해야 한다):
  ```bash
  ./gradlew :domain:test :data:testDebugUnitTest \
    :core:util:android:testDebugUnitTest \
    :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
  ```
  마지막 코드 태스크(Task 6)에서만 `./gradlew :app:assembleDebug`까지 돌린다.

---

## 파일 구성

| 자리 | 역할 | 태스크 |
|---|---|---|
| `domain/model/error/ServerErrorCode.kt` | `ParfaitGroup.GROUP_NOT_JOINED` 추가 + `object ParfaitImage` 신설(`PARFAIT_NOT_FOUND`) | 1 |
| `feature/groups/canvas/impl/util/ToppingPlaceFailure.kt` | **신규** — 영구 실패 판정 순수 함수 | 1 |
| `feature/groups/canvas/impl/util/ToppingPlacement.kt` | **신규** — 화면 좌표 → 서버 좌표 변환 순수 함수 | 2 |
| `feature/groups/canvas/impl/component/CanvasToppingLayer.kt` | 읽기 쪽 `size` → `requiredSize`. clamp가 역함수를 깨고 있었다 | 2 |
| `core/util/android/extension/String.kt` | `Int.toArgbHexString()` 추가(`toColorOrNull`의 역함수) | 2 |
| `data/network/NoBodyLog.kt` | **신규** — 응답 본문을 로그에 남기지 않는 엔드포인트 표시 | 4 |
| `data/network/SelectiveLoggingInterceptor.kt` | **신규** — 표시된 엔드포인트만 본문 로깅을 낮춘다 | 4 |
| `data/source/image/remote/PresignedUploadDataSourceImpl.kt` | 코루틴 취소를 OkHttp `Call.cancel()`로 잇는다 | 3 |
| `data/di/NetworkModule.kt` | 로깅 인터셉터를 선택형으로 교체 | 4 |
| `data/service/ImageService.kt` | 발급 엔드포인트에 `@NoBodyLog` | 4 |
| `feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt` | 확정 결선·로딩·실패 분기·초안 비우기 | 5 |
| `feature/groups/canvas/impl/screen/CanvasToppingPlaceScreen.kt` | painter 준비 상태를 올려보낸다(**Preview 호출부 포함**) | 6 |
| `feature/groups/canvas/impl/route/CanvasToppingPlaceRoute.kt` | 로딩 오버레이·토스트·되감기 | 6 |
| `feature/groups/canvas/impl/res/values/strings.xml` | 실패 문구 둘 | 6 |
| `domain/usecase/topping/AddToppingUseCase.kt` | 취소 함정 KDoc 한 줄(OQ-P-248) | 7 |
| (문서 저장소) `parfait/**` | 스펙·open-questions·README 반영 + PR6 등록 | 7 |

---

## Task 1: 영구 실패 판정

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/error/ServerErrorCode.kt`
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlaceFailure.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlaceFailureTest.kt`

**Interfaces:**
- Consumes: `AppError`(`domain/model/error/AppError.kt`) — `Server(code: String, statusCode: Int?, serverMessage: String)`·`Network`·`Unexpected` 세 갈래.
- Produces: `internal fun AppError.isPermanentPlaceFailure(): Boolean` — Task 5가 실패 분기에서 부른다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlaceFailureTest.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.error.AppError
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingPlaceFailureTest {
    private fun server(
        code: String,
        statusCode: Int?,
    ) = AppError.Server(
        code = code,
        statusCode = statusCode,
        serverMessage = "서버 메시지",
    )

    @Test
    fun isPermanentPlaceFailure_closedParfait_isPermanent() {
        // Given 03시 회전이 캔버스를 닫은 사이 화면이 열려 있었다
        val error = server(code = "PARFAIT_ALREADY_CLOSED", statusCode = 409)

        // Then 재시도해도 같은 결과라 되감아야 한다
        assertTrue(error.isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_notJoinedOrMissingParfait_isPermanent() {
        // 배치 POST 는 그룹 참여 → 파르페 존재 → 파르페 상태 순으로 검사한다.
        // 마감만 되감으면 앞의 둘에서 사용자가 실패만 반복한다
        assertTrue(server(code = "GROUP_NOT_JOINED", statusCode = 403).isPermanentPlaceFailure())
        assertTrue(server(code = "PARFAIT_NOT_FOUND", statusCode = 404).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_nullStatusCode_stillPermanent() {
        // 서버가 200 에 실패 봉투를 실으면 ApiCaller 가 statusCode 를 null 로 채운다.
        // 그 갈래에서 판정 불가로 두면 사용자가 영원히 실패하는 재시도만 반복한다
        assertTrue(server(code = "PARFAIT_ALREADY_CLOSED", statusCode = null).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_otherServerCode_isNotPermanent() {
        assertFalse(server(code = "IMAGE_NOT_FOUND", statusCode = 404).isPermanentPlaceFailure())
        assertFalse(server(code = "INVALID_REQUEST", statusCode = 400).isPermanentPlaceFailure())
    }

    @Test
    fun isPermanentPlaceFailure_networkAndUnexpected_areNotPermanent() {
        // 연결 실패는 재시도가 의미 있는 유일한 갈래다
        assertFalse(AppError.Network(IOException("connection reset")).isPermanentPlaceFailure())
        assertFalse(AppError.Unexpected(IllegalStateException("매핑 실패")).isPermanentPlaceFailure())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingPlaceFailureTest*"
```

Expected: 컴파일 실패 — `isPermanentPlaceFailure` 가 없고 `ServerErrorCode` 에 `GROUP_NOT_JOINED`·`PARFAIT_NOT_FOUND` 도 없다.

- [ ] **Step 3: `ServerErrorCode`에 상수 둘을 더한다**

`domain/src/main/java/com/teamyg/parfait/domain/model/error/ServerErrorCode.kt` —
`object ParfaitGroup` 안에 추가한다(기존 상수들 아래):

```kotlin
        /** 403 — 그 그룹의 멤버가 아니다. 토핑 배치 POST 가 마감 검사보다 **먼저** 이 검사를 한다 */
        const val GROUP_NOT_JOINED = "GROUP_NOT_JOINED"
```

`PARFAIT_NOT_FOUND`는 **`object Parfait`에 넣지 않는다.** 이 파일의 분류 원칙은
"서버 enum 하나에 object 하나"이고 `object Parfait`은 서버 `ParfaitErrorCode` 대응인데,
**배치 POST가 내는 `PARFAIT_NOT_FOUND`는 `ParfaitImageErrorCode` 것이다**
([api/parfait-image.md](../../../api/parfait-image.md) 실패 표). 그래서 `object ParfaitImage`를
새로 만든다(`object Parfait` 아래):

```kotlin
    /** 서버 `ParfaitImageErrorCode` 에 대응한다 */
    object ParfaitImage {
        /**
         * 404 — `parfaitId` 가 그 그룹의 파르페가 아니다.
         *
         * ⚠️ "존재하지 않음"과 "남의 그룹 것"을 구분하지 않는다 — 서버가
         * `findByIdAndGroupId` 하나로 판정한다. 같은 문자열을 `ParfaitErrorCode` 도 갖지만
         * (캔버스 상세 조회·배경 변경) 둘 다 404 라 와이어에서 구분되지 않는다 — 그래서 이
         * 문자열을 보는 판정은 `code` 단독으로 한다. 검사 순서는 `api/parfait-image.md`.
         */
        const val PARFAIT_NOT_FOUND = "PARFAIT_NOT_FOUND"
    }
```

> C-301이 위치·테두리·삭제 PATCH를 붙일 때 같은 enum의 `PARFAIT_IMAGE_NOT_FOUND`·
> `PARFAIT_IMAGE_NOT_OWNED`가 이 object로 들어온다.

- [ ] **Step 4: 판정 함수를 만든다**

`feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlaceFailure.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode

/**
 * 다시 눌러도 영원히 같은 실패인가. 참이면 화면에 잡아 두지 않고 캔버스로 되감는다.
 *
 * `statusCode` 를 보지 않는 것이 결정이다 — 세 코드 모두 status 로 갈려야 하는 동명 코드가
 * 없고, 서버가 200 에 실패 봉투를 실으면 그 값이 `null` 로 와서 조건에 넣는 순간 판정이
 * 사라진다(`specs/archive/2026-08-20-c106-topping-place-api.md` 실패 처리 절).
 */
internal fun AppError.isPermanentPlaceFailure(): Boolean = this is AppError.Server &&
    code in PERMANENT_PLACE_FAILURE_CODES

private val PERMANENT_PLACE_FAILURE_CODES = setOf(
    ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED,
    ServerErrorCode.ParfaitImage.PARFAIT_NOT_FOUND,
    ServerErrorCode.ParfaitGroup.GROUP_NOT_JOINED,
)
```

> **판정 범위는 배치 POST의 실패 표뿐이다.** 업로드 3단계(발급·전송·확인)의 실패와
> `ImageUploadRepositoryImpl`의 로컬 프리플라이트 실패(파일 부재·지원 밖 확장자)는
> **전량 잔류**로 둔다 — 재시도가 발급부터 다시 타므로 새 `imageId`가 생겨 대부분 의미가
> 있고, 파일이 사라진 경우는 `ToppingDraftRepository`가 그 경로를 정규화해 다음 방출에서
> `DraftMissing`으로 넘어간다.

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
./gradlew :domain:test :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
```

Expected: 전부 PASS. 신규 5건.

- [ ] **Step 6: 커밋한다**

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/model/error/ServerErrorCode.kt \
  feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlaceFailure.kt \
  feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlaceFailureTest.kt
git commit -m "feat(topping): 배치 영구 실패 세 코드를 판정한다"
```

---

## Task 2: 좌표 변환과 테두리 색 직렬화

**Files:**
- Create: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlacement.kt`
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt:110`(읽기 쪽 clamp 제거)
- Modify: `core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/String.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlacementTest.kt`
- Test: `core/util/android/src/test/kotlin/com/teamyg/parfait/core/util/android/extension/StringTest.kt`(기존 파일에 케이스 추가)

**Interfaces:**
- Consumes: `TOPPING_BASE_LONG_SIDE_RATIO`(`feature/.../impl/component/CanvasToppingLayer.kt`, `internal const val = 0.4f`) · `ToppingTransform`(`domain/model/topping/`) · `String#toColorOrNull`(`core/util/android/extension/String.kt`).
- Produces:
  - `internal fun toToppingTransform(offsetX: Dp, offsetY: Dp, scale: Float, rotationDegrees: Float, canvasSize: DpSize, toppingBaseSize: DpSize, positionZ: Int): ToppingTransform`
  - `fun Int.toRgbHexString(): String` — `#RRGGBB` 6자리 대문자(계획 당시 `toArgbHexString`·8자리였으나 PR5 브랜치 리뷰에서 정정됐다). Task 5가 `ToppingBorder.Solid(color = …)`에 넣는다.

> **왜 왕복 테스트인가.** 쓰기 쪽 `scale`과 읽기 쪽 `scale`은 **기준이 다른 다른 수다.**
> 읽기 쪽 `CanvasToppingLayer#CanvasTopping`은 한 변이
> `canvasWidth × TOPPING_BASE_LONG_SIDE_RATIO × scale`인 정사각 박스에 `ContentScale.Fit`으로
> 담아 긴 변을 꽉 채운다. 배치 화면의 긴 변은 `max(baseWidth, baseHeight) × scale`이다.
> 둘을 같게 놓은 것이 아래 식이고, 어긋나면 "토핑이 조금씩 밀린다"라 원인 추적이 어렵다.
>
> ⚠️ **그 역함수가 지금은 오버플로 구간에서 성립하지 않는다.** `CanvasToppingLayer.kt:110`이
> 그 정사각 박스를 `Modifier.size(side)`로 주는데 `size`는 부모 constraints에 clamp된다.
> 반면 배치 화면은 같은 자리를 `requiredSize(sizeAfterScale)`로 주고 그 이유를 주석에 적어
> 두었으며(`CanvasToppingPlaceScreen.kt:136-139`),
> `CanvasToppingPlaceViewModel#maxScaleToOverflowCanvas`는 긴 변이 캔버스 긴 변의 1.5배까지
> 가도록 **일부러** 허용한다. 그래서 캔버스를 넘긴 토핑은 캔버스에서 잘리는 것이 아니라
> **조용히 작아진다** — `CanvasTopping`의 KDoc이 "넘친 픽셀은 clip이 잘라 낸다"고 적은 것도
> 함께 거짓이다. **Step 3이 읽기 쪽을 고쳐 역함수를 참으로 만든다.**

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlacementTest.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.groups.canvas.impl.component.TOPPING_BASE_LONG_SIDE_RATIO
import kotlin.test.Test
import kotlin.test.assertEquals

private const val DELTA = 1e-4

private val CANVAS = DpSize(width = 360.dp, height = 640.dp)

class ToppingPlacementTest {
    /**
     * 읽기 쪽 `CanvasToppingLayer#CanvasTopping` 의 식을 그대로 옮긴 역함수.
     * 정사각 박스의 한 변을 내고, 그 안에서 원본 비율을 유지한 긴 변이 곧 화면 긴 변이다.
     */
    private fun readBackLongSideDp(
        scale: Double,
        canvasWidth: Float,
    ): Double = canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO * scale

    @Test
    fun toToppingTransform_centered_mapsToHalfHalf() {
        // Given 캔버스 정중앙에 놓인 토핑
        val baseSize = DpSize(width = 100.dp, height = 50.dp)
        val transform = toToppingTransform(
            offsetX = (CANVAS.width - baseSize.width) / 2,
            offsetY = (CANVAS.height - baseSize.height) / 2,
            scale = 1f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 7,
        )

        // Then 정규화 좌표는 중심 기준 0.5·0.5 다
        assertEquals(0.5, transform.positionX, DELTA)
        assertEquals(0.5, transform.positionY, DELTA)
        assertEquals(7, transform.positionZ)
        assertEquals(0.0, transform.rotation, DELTA)
    }

    @Test
    fun toToppingTransform_topLeftCorner_mapsToHalfSizeFractions() {
        // Given 좌상단에 딱 붙인 토핑을 2배로 키웠다.
        // 중심은 **배율 적용 전** 크기의 절반만큼 안쪽이다 — 화면이 그렇게 계산한다
        // (CanvasToppingPlaceScreen 의 center = offsetX + baseSize.width / 2).
        // scale 을 곱해 중심을 구하는 구현은 여기서 갈린다
        val baseSize = DpSize(width = 100.dp, height = 50.dp)
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 2f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        // 캔버스가 가로/세로로 달라 x·y 를 뒤바꾼 구현도 여기서 걸린다
        assertEquals(50.0 / 360.0, transform.positionX, DELTA)
        assertEquals(25.0 / 640.0, transform.positionY, DELTA)
    }

    @Test
    fun toToppingTransform_overflowingTopping_roundTripsToo() {
        // Given 캔버스 폭을 넘도록 키운 토핑 — maxScaleToOverflowCanvas 가 허용하는 구간이다
        val baseSize = DpSize(width = 200.dp, height = 80.dp)
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 4f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        // 화면 긴 변 800dp 는 캔버스 폭 360dp 를 넘는다. 읽기 쪽 박스가 clamp 되면
        // 여기가 아니라 실제 화면에서만 갈리므로, 이 단언과 Step 3 의 수정이 한 쌍이다
        assertEquals(
            200.0 * 4,
            readBackLongSideDp(scale = transform.scale, canvasWidth = CANVAS.width.value),
            DELTA,
        )
    }

    @Test
    fun toToppingTransform_scale_roundTripsThroughReadSideFormula() {
        // Given 배율 1.75 로 키운 가로로 긴 토핑
        val baseSize = DpSize(width = 200.dp, height = 80.dp)
        val screenLongSideDp = 200.0 * 1.75

        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 1.75f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        // Then 읽기 쪽 식으로 되돌리면 화면에서 보던 긴 변이 그대로 나온다
        assertEquals(
            screenLongSideDp,
            readBackLongSideDp(scale = transform.scale, canvasWidth = CANVAS.width.value),
            DELTA,
        )
    }

    @Test
    fun toToppingTransform_portraitTopping_roundTripsOnTheLongSideToo() {
        // 세로로 긴 토핑에서도 기준은 긴 변이다 — 짧은 변으로 나누면 여기서 갈린다
        val baseSize = DpSize(width = 60.dp, height = 240.dp)
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 0.5f,
            rotationDegrees = 0f,
            canvasSize = CANVAS,
            toppingBaseSize = baseSize,
            positionZ = 1,
        )

        assertEquals(
            240.0 * 0.5,
            readBackLongSideDp(scale = transform.scale, canvasWidth = CANVAS.width.value),
            DELTA,
        )
    }

    @Test
    fun toToppingTransform_rotation_passesThroughUnchanged() {
        val transform = toToppingTransform(
            offsetX = 0.dp,
            offsetY = 0.dp,
            scale = 1f,
            rotationDegrees = -37.5f,
            canvasSize = CANVAS,
            toppingBaseSize = DpSize(width = 100.dp, height = 100.dp),
            positionZ = 1,
        )

        assertEquals(-37.5, transform.rotation, DELTA)
    }
}
```

`core/util/android/src/test/kotlin/com/teamyg/parfait/core/util/android/extension/StringTest.kt`
클래스 안에 케이스 셋을 더한다:

```kotlin
    @Test
    fun toArgbHexString_opaqueColor_writesEightDigitsWithHash() {
        // Given 초안이 담는 ARGB Int
        val argb = Color(0xFFFF6B00).toArgb()

        // Then 서버에 보내는 형식은 8자리다 — 6자리로 보내면 알파가 사라진다
        assertEquals("#FFFF6B00", argb.toArgbHexString())
    }

    @Test
    fun toArgbHexString_thenToColorOrNull_roundTrips() {
        // 이 왕복이 깨지면 캔버스가 테두리를 조용히 안 그린다 — 서버는 200 을 준다
        val original = Color(0x80123456)
        val restored = original.toArgb().toArgbHexString().toColorOrNull()

        assertEquals(original, restored)
    }

    @Test
    fun toArgbHexString_transparentBlack_keepsLeadingZeros() {
        // 앞자리 0 이 잘리면 길이가 8 이 아니게 되고 읽기 쪽이 null 을 낸다
        assertEquals("#00000000", 0.toArgbHexString())
    }
```

`StringTest.kt` 상단 import에 더한다(`Color`는 이미 있다):

```kotlin
import androidx.compose.ui.graphics.toArgb
```

> `toArgb()`는 `ToppingEditViewModel`이 이미 쓰는 것과 같은 함수다. **프로덕션 경로에는 이
> 변환이 필요 없다** — 초안의 `borderColorArgb`가 이미 `Int`라 테스트에서만 쓴다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*ToppingPlacementTest*" \
  :core:util:android:testDebugUnitTest --tests "*StringTest*"
```

Expected: 컴파일 실패 — `toToppingTransform`·`toArgbHexString` 이 없다.

- [ ] **Step 3: 읽기 쪽 clamp를 걷어 역함수를 참으로 만든다**

`feature/groups/canvas/impl/.../component/CanvasToppingLayer.kt` — `CanvasTopping`의
`.size(side)`를 바꾼다:

```kotlin
            // 캔버스를 넘긴 토핑은 잘려야지 작아지면 안 된다. size 는 부모 constraints 로
            // clamp 돼 정사각 박스가 캔버스 크기로 줄고, 그 안에서 Fit 이 다시 축소한다
            ).requiredSize(side)
```

import를 바꾼다:

```kotlin
// 지운다
import androidx.compose.foundation.layout.size
// 더한다
import androidx.compose.foundation.layout.requiredSize
```

> `size`가 파일의 다른 자리에서도 쓰이면 그 import는 남긴다. **`grep -n "\.size(" `로 확인한 뒤
> 판단한다.**
>
> ⚠️ **이것은 캔버스 렌더가 실제로 바뀌는 수정이다.** JVM 테스트로는 안 잡히므로
> **실기기 확인 9번**이 감지선이다. `CanvasTopping`의 KDoc이 이미 "넘친 픽셀은 clip이
> 잘라 낸다"고 적어 두었으니 문구는 고칠 것이 없다 — 코드가 그 문장에 맞춰지는 것이다.

- [ ] **Step 4: 좌표 변환 함수를 만든다**

`feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlacement.kt`:

```kotlin
package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.feature.groups.canvas.impl.component.TOPPING_BASE_LONG_SIDE_RATIO

/**
 * 배치 화면의 화면 좌표를 서버가 저장하는 정규화 좌표로 바꾼다.
 *
 * ⚠️ **쓰기 쪽 `scale` 과 읽기 쪽 `scale` 은 기준이 다른 다른 수다.** 읽기 쪽
 * `CanvasToppingLayer#CanvasTopping` 은 한 변이 `canvasWidth × TOPPING_BASE_LONG_SIDE_RATIO ×
 * scale` 인 정사각 박스에 `ContentScale.Fit` 으로 담아 긴 변을 꽉 채운다. 그 긴 변이 배치
 * 화면의 `max(baseWidth, baseHeight) × scale` 과 같아지도록 역산한 것이 아래 식이다.
 * 그대로 보내면 캔버스에서 크기가 달라진다.
 *
 * 위치는 좌상단이 아니라 **중심** 기준이다.
 */
internal fun toToppingTransform(
    offsetX: Dp,
    offsetY: Dp,
    scale: Float,
    rotationDegrees: Float,
    canvasSize: DpSize,
    toppingBaseSize: DpSize,
    positionZ: Int,
): ToppingTransform {
    val canvasWidth = canvasSize.width.value
    val canvasHeight = canvasSize.height.value
    val longerBaseSide = maxOf(toppingBaseSize.width.value, toppingBaseSize.height.value)

    return ToppingTransform(
        positionX = ((offsetX.value + toppingBaseSize.width.value / 2) / canvasWidth).toDouble(),
        positionY = ((offsetY.value + toppingBaseSize.height.value / 2) / canvasHeight).toDouble(),
        positionZ = positionZ,
        scale = (longerBaseSide * scale / (canvasWidth * TOPPING_BASE_LONG_SIDE_RATIO)).toDouble(),
        rotation = rotationDegrees.toDouble(),
    )
}
```

- [ ] **Step 5: 테두리 색 직렬화를 만든다**

`core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/String.kt`
맨 아래에 더한다(`toColorOrNull`과 **같은 파일에 둔다** — 둘은 서로의 역함수라 떨어뜨리면
한쪽만 고쳐도 아무것도 안 깨진다):

```kotlin
/**
 * ARGB 정수를 서버에 보내는 색 문자열로 쓴다. [toColorOrNull] 의 역함수다.
 *
 * 8자리로 쓰는 것이 C-106 의 결정이다 — 6자리로 줄이면 알파가 사라지고, 형식이 어긋나면
 * 읽기 쪽이 `null` 을 내 캔버스가 테두리를 그냥 안 그린다(서버는 200 을 준다).
 */
fun Int.toArgbHexString(): String = "#%08X".format(this)
```

- [ ] **Step 6: 테스트가 통과하는지 확인한다**

```bash
./gradlew :core:util:android:testDebugUnitTest :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
```

Expected: 전부 PASS. 신규 9건(좌표 6 + 색 3).

- [ ] **Step 7: 커밋한다**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlacement.kt \
  feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/component/CanvasToppingLayer.kt \
  feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/util/ToppingPlacementTest.kt \
  core/util/android/src/main/kotlin/com/teamyg/parfait/core/util/android/extension/String.kt \
  core/util/android/src/test/kotlin/com/teamyg/parfait/core/util/android/extension/StringTest.kt
git commit -m "feat(topping): 화면 좌표와 테두리 색을 서버 형식으로 바꾼다"
```

---

## Task 3: 업로드가 코루틴 취소를 따라가게 한다 (OQ-P-246)

**Files:**
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImplTest.kt`(기존 파일에 케이스 추가)

**Interfaces:**
- Consumes: `PresignedUploadDataSource#put(uploadUrl: String, contentType: String, file: File): Result<Unit>` — **시그니처는 바뀌지 않는다.**
- Produces: 같은 시그니처. 달라지는 것은 **호출 코루틴이 취소되면 진행 중인 HTTP 호출도 끊긴다**는 것뿐이다.

> **왜 지금인가.** PR1이 미룬 이유는 전송 메서드의 모양을 바꾸는 변경이라 깨끗한 브랜치를
> 늦게 흔들 위험이 컸다는 것이다. 이 라운드는 로딩 오버레이와 `popUpTo` 되감기가 있어
> `viewModelScope` 취소가 흔하다 — 지금 고치지 않으면 화면을 떠난 뒤에도 업로드가
> `callTimeout`(120초)까지 계속 돈다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`PresignedUploadDataSourceImplTest.kt`의 `setUp`은 지금 클라이언트를 그 자리에서 만들어 버려
(`PresignedUploadDataSourceImpl(NetworkModule.provideUploadOkHttpClient())`) 테스트가 그것을
들여다볼 수 없다. **필드로 끌어올린다** — `setUp`을 아래로 바꾸고 `client` 필드를 더한다:

```kotlin
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var dataSource: PresignedUploadDataSource
    private lateinit var file: File

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = NetworkModule.provideUploadOkHttpClient()
        dataSource = PresignedUploadDataSourceImpl(client)
        file = File.createTempFile("topping", ".png")
        file.writeBytes(ByteArray(FILE_SIZE) { index -> index.toByte() })
    }
```

그리고 클래스 안에 케이스를 더한다:

```kotlin
    @Test
    fun put_whenCallerIsCancelled_cancelsTheHttpCall() = runTest {
        // Given 응답을 오래 붙잡고 놓지 않는 서버
        server.enqueue(
            MockResponse
                .Builder()
                .code(200)
                .headersDelay(HANG_SECONDS, TimeUnit.SECONDS)
                .build(),
        )

        // When 전송이 시작된 뒤 호출자가 취소된다
        val job = launch(Dispatchers.IO) {
            dataSource.put(
                uploadUrl = server.url("/upload").toString(),
                contentType = "image/png",
                file = file,
            )
        }
        // 요청이 실제로 나간 뒤에 취소해야 "취소가 호출을 끊는지"를 보는 테스트가 된다
        server.takeRequest()
        // ⚠️ join 하지 않는다. 블로킹 execute() 를 쓰는 구현에서는 join 이 응답 도착까지
        // 기다려 버려, 그 뒤에 세면 호출이 이미 걷힌 뒤라 구·신 구현이 똑같이 통과한다
        job.cancel()

        // Then 열린 호출이 사라진다. 취소가 안 이어지면 HANG_SECONDS 내내 1 로 남는다
        assertTrue(awaitNoRunningCalls(), "취소 후에도 열린 업로드 호출이 남았다")
    }

    /**
     * OkHttp 는 `cancel()` 직후가 아니라 전송 스레드가 취소를 알아챈 뒤에 호출을 걷는다.
     * 그래서 즉시 단언하면 경합이 되고, 대신 짧은 상한 안에 0 이 되는지를 본다.
     */
    private fun awaitNoRunningCalls(): Boolean {
        val deadline = System.nanoTime() + CANCEL_TIMEOUT_MILLIS * NANOS_PER_MILLI
        while (System.nanoTime() < deadline) {
            if (client.dispatcher.runningCallsCount() == 0) return true
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }
```

`FILE_SIZE`는 파일 하단이 아니라 **클래스 안 `private companion object`** 에 있다.
그 안, `FILE_SIZE` 아래에 더한다:

```kotlin
        const val HANG_SECONDS = 10L
        const val CANCEL_TIMEOUT_MILLIS = 2_000L
        const val POLL_INTERVAL_MILLIS = 10L
        const val NANOS_PER_MILLI = 1_000_000L
```

파일 상단 import에 더한다(기존 목록에 없는 것만):

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
```

> ⚠️ **이 테스트는 `runTest`의 가상 시간을 쓰지 않는다.** 실제 소켓이 오가는 자리라
> `headersDelay`도 폴링도 실시간이다. `advanceUntilIdle()`을 부르지 않고,
> `launch`에 `Dispatchers.IO`를 명시해 테스트 스케줄러가 이 코루틴을 붙잡지 않게 한다.
>
> ⚠️ `FILE_SIZE`는 기존 `private companion object` 에 이미 있는 상수다.
> **다시 선언하지 않는다.**

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*PresignedUploadDataSourceImplTest*"
```

Expected: FAIL — `awaitNoRunningCalls()`가 2초 상한 안에 0을 못 보고 `false`를 낸다.
현행 `execute()`는 블로킹이라 `Call.cancel()`이 취소에 이어져 있지 않고, 열린 호출이
`HANG_SECONDS`(10초) 내내 남는다. **이 실패까지 약 2초가 걸린다.**

- [ ] **Step 3: 취소를 잇는다**

`PresignedUploadDataSourceImpl.kt` 전문을 아래로 바꾼다:

```kotlin
package com.teamyg.parfait.data.source.image.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.model.exception.PresignedUploadException
import com.teamyg.parfait.data.model.qualifier.UploadClient
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume

class PresignedUploadDataSourceImpl @Inject constructor(
    @UploadClient private val okHttpClient: OkHttpClient,
) : PresignedUploadDataSource {
    /**
     * `execute()` 가 아니라 `enqueue` 를 쓰는 것이 취소 전파의 전부다 — 블로킹 호출은
     * 코루틴이 취소돼도 스스로 멈추지 않아 `callTimeout` 까지 돈다. 자체 디스패처 위에서
     * 돌므로 `withContext(Dispatchers.IO)` 도 필요 없다.
     */
    override suspend fun put(
        uploadUrl: String,
        contentType: String,
        file: File,
    ): Result<Unit> {
        val call = try {
            // asRequestBody 는 파일을 스트리밍으로 읽는다. 바이트를 미리 배열에 담으면 원본
            // 해상도 이미지가 통째로 힙에 올라간다
            val request = Request
                .Builder()
                .url(uploadUrl)
                .put(file.asRequestBody(contentType.toMediaType()))
                .build()

            okHttpClient.newCall(request)
        } catch (e: Exception) {
            // uploadUrl·contentType 은 서버가 준 값이라 Request 조립 단계에서 예외가 날 수 있다.
            // 여기서 안 잡으면 Result 를 돌려주기로 한 계약이 깨진 채 호출부까지 올라간다.
            // 이 블록에는 suspend 호출이 없어 CancellationException 재던지기가 필요 없다 —
            // 한 줄이라도 들어오면 그때 가드를 붙인다
            return Result.failure(ApiException.Unknown(e))
        }

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(
                object : Callback {
                    override fun onResponse(
                        call: Call,
                        response: Response,
                    ) {
                        response.use {
                            val result = if (it.isSuccessful) {
                                Result.success(Unit)
                            } else {
                                Result.failure(ApiException.Unknown(PresignedUploadException(it.code)))
                            }
                            if (continuation.isActive) continuation.resume(result)
                        }
                    }

                    // 취소로 끊긴 호출도 IOException("Canceled") 로 여기 들어온다.
                    // 이미 취소된 continuation 은 resume 을 버리므로 굳이 부르지 않는다
                    override fun onFailure(
                        call: Call,
                        e: IOException,
                    ) {
                        if (continuation.isActive) continuation.resume(Result.failure(ApiException.Network(e)))
                    }
                },
            )
        }
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest ktlintCheck
```

Expected: 전부 PASS. **기존 케이스가 하나도 깨지지 않아야 한다** — `Authorization` 부재와
`Content-Type` 일치 단언이 이 라운드의 핵심 감지선이다.

- [ ] **Step 5: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImpl.kt \
  data/src/test/java/com/teamyg/parfait/data/source/image/remote/PresignedUploadDataSourceImplTest.kt
git commit -m "fix(image): 업로드가 코루틴 취소를 따라가게 한다"
```

---

## Task 4: 발급 응답 본문을 로그에 남기지 않는다 (OQ-P-109)

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/network/NoBodyLog.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/network/SelectiveLoggingInterceptor.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/network/SelectiveLoggingInterceptorTest.kt`

**Interfaces:**
- Consumes: `retrofit2.Invocation` 태그 판정 — `AuthInterceptor#intercept`가 `@NoAuth`에 쓰는 것과 **같은 기법**이다.
- Produces: `@NoBodyLog` 애노테이션 · `SelectiveLoggingInterceptor(full: Interceptor, redacted: Interceptor)`.

> **무엇이 새는가.** `POST /api/v1/images`의 **응답 본문**에 presigned `uploadUrl`이 실려 온다.
> presigned URL은 서명(`X-Amz-Signature`)과 자격 정보를 **쿼리 스트링**에 싣는 방식이라
> **URL 자체가 유효한 업로드 자격증명**이다. `redactHeader`는 헤더만 가려 본문을 못 가린다.
> PR1이 업로드 클라이언트에서 로깅을 통째로 걷은 것과 같은 사유이고, 그때 남은 절반이 이쪽이다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/network/SelectiveLoggingInterceptorTest.kt`:

```kotlin
package com.teamyg.parfait.data.network

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SECRET_BODY = """{"uploadUrl":"https://s3.example.com/o?X-Amz-Signature=deadbeef"}"""

/**
 * 반환 타입이 `ResponseBody` 라 Retrofit 내장 컨버터로 읽힌다 — 이 테스트가 보는 것은 로깅
 * 선택이지 직렬화가 아니라서 컨버터 팩토리를 달지 않는다.
 */
private interface FakeService {
    @GET("plain")
    suspend fun plain(): ResponseBody

    @NoBodyLog
    @GET("secret")
    suspend fun secret(): ResponseBody
}

class SelectiveLoggingInterceptorTest {
    private lateinit var server: MockWebServer
    private val logs = mutableListOf<String>()

    private fun service(): FakeService {
        val full = HttpLoggingInterceptor { message -> logs += message }
            .apply { level = HttpLoggingInterceptor.Level.BODY }
        val redacted = HttpLoggingInterceptor { message -> logs += message }
            .apply { level = HttpLoggingInterceptor.Level.HEADERS }

        val client = OkHttpClient
            .Builder()
            .addInterceptor(SelectiveLoggingInterceptor(full = full, redacted = redacted))
            .build()

        return Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .build()
            .create(FakeService::class.java)
    }

    @BeforeTest
    fun setUp() {
        server = MockWebServer()
        server.start()
        logs.clear()
    }

    @AfterTest
    fun tearDown() {
        server.close()
    }

    @Test
    fun intercept_annotatedEndpoint_doesNotLogTheResponseBody() = runTest {
        // Given 응답 본문에 presigned URL 이 실려 오는 엔드포인트
        server.enqueue(MockResponse.Builder().code(200).body(SECRET_BODY).build())

        service().secret().close()

        // Then 그 URL 이 로그 어디에도 남지 않는다 — URL 자체가 업로드 자격증명이다
        assertFalse(logs.any { it.contains("X-Amz-Signature") })
    }

    @Test
    fun intercept_plainEndpoint_stillLogsTheResponseBody() = runTest {
        // 표시 없는 엔드포인트의 본문 로깅은 그대로여야 한다 — 전체를 낮추는 수정이 아니다
        server.enqueue(MockResponse.Builder().code(200).body(SECRET_BODY).build())

        service().plain().close()

        assertTrue(logs.any { it.contains("X-Amz-Signature") })
    }
}
```

> ⚠️ **두 테스트가 `HttpLoggingInterceptor` 를 `Level.BODY` 로 직접 세운다** — 프로덕션은
> `BuildConfig.DEBUG` 로 갈리지만 이 테스트가 보는 것은 **선택 로직**이지 그 게이트가 아니다.
> 게이트까지 테스트에 끌어들이면 릴리스 빌드에서 두 케이스가 다 통과해 감지선이 사라진다.

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest --tests "*SelectiveLoggingInterceptorTest*"
```

Expected: 컴파일 실패 — `NoBodyLog`·`SelectiveLoggingInterceptor` 가 없다.

- [ ] **Step 3: 애노테이션을 만든다**

`data/src/main/java/com/teamyg/parfait/data/network/NoBodyLog.kt`:

```kotlin
package com.teamyg.parfait.data.network

/**
 * 이 엔드포인트의 **본문**은 로그에 남기지 않는다.
 *
 * 응답에 그 자체로 자격증명인 값이 실려 오는 자리에 붙인다 — presigned URL 은 서명을 쿼리
 * 스트링에 싣는 방식이라 URL 한 줄이 곧 업로드 권한이고, `redactHeader` 로는 못 가린다.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoBodyLog
```

- [ ] **Step 4: 선택형 로깅 인터셉터를 만든다**

`data/src/main/java/com/teamyg/parfait/data/network/SelectiveLoggingInterceptor.kt`:

```kotlin
package com.teamyg.parfait.data.network

import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Invocation

/**
 * [NoBodyLog] 가 붙은 엔드포인트만 [redacted] 로, 나머지는 [full] 로 로깅한다.
 *
 * 인스턴스를 둘 두는 이유: `HttpLoggingInterceptor.level` 은 가변 필드라 요청마다 바꾸면
 * 동시 요청끼리 서로의 레벨을 덮어쓴다.
 */
class SelectiveLoggingInterceptor(
    private val full: Interceptor,
    private val redacted: Interceptor,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val suppressBody = chain
            .request()
            .tag(Invocation::class.java)
            ?.method()
            ?.isAnnotationPresent(NoBodyLog::class.java) == true

        return if (suppressBody) redacted.intercept(chain) else full.intercept(chain)
    }
}
```

- [ ] **Step 5: 발급 엔드포인트에 표시를 붙인다**

`data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt`:

```kotlin
    @NoBodyLog
    @POST("api/v1/images")
    suspend fun postImages(@Body request: IssueImageUploadUrlRequest): ApiResponse<IssueImageUploadUrlResponse>
```

import를 더한다:

```kotlin
import com.teamyg.parfait.data.network.NoBodyLog
```

인터페이스 KDoc 끝에 한 줄을 더한다:

```
 * 발급 응답 본문에는 presigned uploadUrl 이 실려 온다 — URL 자체가 자격증명이라 본문 로깅을 막는다.
```

- [ ] **Step 6: `NetworkModule`을 갈아 끼운다**

`data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt` —
`loggingInterceptor()` 하나를 아래 셋으로 바꾼다:

```kotlin
    /** 두 클라이언트가 같은 로깅·`Authorization` 마스킹 처리를 받는다 */
    private fun loggingInterceptor(): Interceptor = SelectiveLoggingInterceptor(
        full = httpLoggingInterceptor(HttpLoggingInterceptor.Level.BODY),
        // 본문만 뺀다. BASIC 은 헤더까지 통째로 버려 실패 원인을 좁힐 단서가 사라진다
        redacted = httpLoggingInterceptor(HttpLoggingInterceptor.Level.HEADERS),
    )

    private fun httpLoggingInterceptor(debugLevel: HttpLoggingInterceptor.Level): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) debugLevel else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
```

import를 더한다:

```kotlin
import com.teamyg.parfait.data.network.SelectiveLoggingInterceptor
import okhttp3.Interceptor
```

**호출부(`provideOkHttpClient`·`provideUnauthenticatedOkHttpClient`의 `.addInterceptor(loggingInterceptor())`)는
그대로 둔다** — 반환 타입만 `Interceptor`로 넓어졌다.

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

```bash
./gradlew :data:testDebugUnitTest ktlintCheck
```

Expected: 전부 PASS. 신규 2건.

- [ ] **Step 8: 커밋한다**

```bash
git add data/src/main/java/com/teamyg/parfait/data/network/NoBodyLog.kt \
  data/src/main/java/com/teamyg/parfait/data/network/SelectiveLoggingInterceptor.kt \
  data/src/main/java/com/teamyg/parfait/data/di/NetworkModule.kt \
  data/src/main/java/com/teamyg/parfait/data/service/ImageService.kt \
  data/src/test/java/com/teamyg/parfait/data/network/SelectiveLoggingInterceptorTest.kt
git commit -m "fix(network): 발급 응답 본문에 실려 오는 presigned URL 을 로그에서 뺀다"
```

---

## Task 5: `CanvasToppingPlaceViewModel` 결선

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt`
- Test: `feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModelTest.kt`(기존 파일 확장)

**Interfaces:**
- Consumes:
  - `AddToppingUseCase#invoke(groupId, parfaitId, filePath, transform, border): Result<PlacedToppingVO>`(Task 없음 — PR2가 만들었다)
  - `ToppingDraftRepository#draft: Flow<ToppingDraft?>` · `#clear()`
  - Task 1의 `AppError.isPermanentPlaceFailure()`
  - Task 2의 `toToppingTransform(...)` · `Int.toArgbHexString()`
- Produces:
  - `CanvasToppingPlaceUiState`에 `groupId: GroupId?`·`parfaitId: ParfaitId?`·`nextPositionZ: Int?`·`isToppingImageReady: Boolean`·`isLoading: Boolean` 추가
  - `CanvasToppingPlaceIntent.OnToppingImageReadyChanged(isReady: Boolean)` 추가
  - `CanvasToppingPlaceEffect`: `ToppingPlaced` **삭제**, `PlaceSucceeded`·`PlaceFailed`·`PlaceFailedPermanently` 추가
  — Task 6이 전부 소비한다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**

⚠️ **먼저 기존 케이스 하나를 지운다.** `onClickConfirm_withASubjectImage_confirmsThePlacement`가
`CanvasToppingPlaceEffect.ToppingPlaced`와 그 프로퍼티(`imagePath`·`offsetX` 등)를 단언하는데
Step 3이 그 이펙트를 없앤다. 남겨 두면 `Unresolved reference: ToppingPlaced`로 **모듈 테스트가
통째로 컴파일되지 않는다.** 그 자리를 아래 `onClickConfirm_success_clearsDraftAndNavigatesBack`이
대신한다.

그다음 `viewModel(...)` 헬퍼를 바꾸고 케이스를 더한다.
**헬퍼 교체**(기존 `viewModel` 함수를 아래로 대체):

```kotlin
    private val addToppingUseCase: AddToppingUseCase = mockk()

    private fun viewModel(draft: ToppingDraft? = draft()): CanvasToppingPlaceViewModel {
        every { toppingDraftRepository.draft } returns flowOf(draft)
        return CanvasToppingPlaceViewModel(
            toppingDraftRepository = toppingDraftRepository,
            addToppingUseCase = addToppingUseCase,
        )
    }

    /** 확정이 나갈 수 있는 최소 조건을 갖춘 ViewModel — 실측 둘 + painter 준비 */
    private fun readyViewModel(draft: ToppingDraft? = draft()): CanvasToppingPlaceViewModel =
        viewModel(draft).apply {
            processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(DpSize(360.dp, 640.dp)))
            processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(DpSize(100.dp, 50.dp)))
            processIntent(CanvasToppingPlaceIntent.OnToppingImageReadyChanged(isReady = true))
        }
```

**케이스 추가**:

```kotlin
    @Test
    fun onClickConfirm_whenImageNotReady_doesNotCallTheUseCase() = runTest(mainDispatcherRule.dispatcher) {
        // Given 실측은 끝났지만 그림은 아직 뜨지 않았다
        val viewModel = viewModel().apply {
            processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(DpSize(360.dp, 640.dp)))
            processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(DpSize(100.dp, 50.dp)))
        }
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // Then 폴백 크기로 계산된 배율이 서버에 올라가면 안 된다
        coVerify(exactly = 0) { addToppingUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_success_clearsDraftAndNavigatesBack() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            assertEquals(CanvasToppingPlaceEffect.PlaceSucceeded, awaitItem())
        }
        // 성공한 흐름의 초안이 남으면 다음 진입까지 낡은 알맹이를 들고 있다
        coVerify(exactly = 1) { toppingDraftRepository.clear() }
    }

    @Test
    fun onClickConfirm_sendsDraftIdentityAndBorderAsServerFormat() = runTest(mainDispatcherRule.dispatcher) {
        val groupIdSlot = slot<GroupId>()
        val parfaitIdSlot = slot<ParfaitId>()
        val transformSlot = slot<ToppingTransform>()
        val borderSlot = slot<ToppingBorder>()
        coEvery {
            addToppingUseCase(
                groupId = capture(groupIdSlot),
                parfaitId = capture(parfaitIdSlot),
                filePath = any(),
                transform = capture(transformSlot),
                border = capture(borderSlot),
            )
        } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit

        val viewModel = readyViewModel(
            draft(borderColorArgb = Color(0xFFFF6B00).toArgb(), borderWidthDp = 4f),
        )
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // 캔버스 식별값은 흐름 진입 때 못 박은 초안 것이다 — 화면이 다시 고르지 않는다
        assertEquals(GroupId(1L), groupIdSlot.captured)
        assertEquals(ParfaitId(2L), parfaitIdSlot.captured)
        assertEquals(3, transformSlot.captured.positionZ)
        // 형식이 어긋나면 캔버스가 테두리를 조용히 안 그린다
        assertEquals(ToppingBorder.Solid(color = "#FFFF6B00", width = 4.0), borderSlot.captured)
    }

    @Test
    fun onClickConfirm_withoutBorderColor_sendsNone() = runTest(mainDispatcherRule.dispatcher) {
        val borderSlot = slot<ToppingBorder>()
        coEvery {
            addToppingUseCase(any(), any(), any(), any(), border = capture(borderSlot))
        } returns Result.success(mockk())
        coEvery { toppingDraftRepository.clear() } returns Unit

        val viewModel = readyViewModel(draft(borderColorArgb = null, borderWidthDp = null))
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // 색이나 두께가 빠진 SOLID 는 서버가 400 INVALID_BORDER 로 거절한다
        assertEquals(ToppingBorder.None, borderSlot.captured)
    }

    @Test
    fun onClickConfirm_permanentFailure_rewindsAndKeepsDraftUncleaned() = runTest(mainDispatcherRule.dispatcher) {
        // 스펙의 되감기 표는 세 코드를 든다. 하나만 넣으면 집합이 좁아진 회귀를 못 잡는다
        listOf("PARFAIT_ALREADY_CLOSED", "GROUP_NOT_JOINED", "PARFAIT_NOT_FOUND").forEach { code ->
            coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.failure(
                AppError.Server(code = code, statusCode = null, serverMessage = "서버 메시지"),
            )
            val viewModel = readyViewModel()
            advanceUntilIdle()

            viewModel.effect.test {
                viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
                advanceUntilIdle()

                assertEquals(CanvasToppingPlaceEffect.PlaceFailedPermanently, awaitItem(), code)
            }
        }
        // 실패한 흐름의 초안은 남아야 한다 — 비우면 막 만든 토핑을 통째로 잃는다
        coVerify(exactly = 0) { toppingDraftRepository.clear() }
    }

    @Test
    fun onClickConfirm_transientFailure_staysOnScreen() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } returns Result.failure(
            AppError.Network(IOException("connection reset")),
        )
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.effect.test {
            viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
            advanceUntilIdle()

            // 재시도가 의미 있는 갈래라 화면에 남는다
            assertEquals(CanvasToppingPlaceEffect.PlaceFailed, awaitItem())
        }
    }

    @Test
    fun onClickConfirm_whileLoading_doesNotStartASecondUpload() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } coAnswers {
            delay(1_000)
            Result.success(mockk())
        }
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceUntilIdle()

        // 연타로 두 번 올라가면 고아 이미지와 겹친 토핑이 함께 생긴다
        coVerify(exactly = 1) { addToppingUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun onClickConfirm_setsLoadingWhileInFlight() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { addToppingUseCase(any(), any(), any(), any(), any()) } coAnswers {
            delay(1_000)
            Result.success(mockk())
        }
        coEvery { toppingDraftRepository.clear() } returns Unit
        val viewModel = readyViewModel()
        advanceUntilIdle()

        viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm)
        advanceTimeBy(500)
        assertTrue(viewModel.state.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }
```

파일 상단 import에 더한다:

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.usecase.topping.AddToppingUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import java.io.IOException
import kotlin.test.assertFalse
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest --tests "*CanvasToppingPlaceViewModelTest*"
```

Expected: 컴파일 실패 — 생성자에 `addToppingUseCase` 가 없고
`OnToppingImageReadyChanged`·`PlaceSucceeded`·`PlaceFailed`·`PlaceFailedPermanently` 도 없다.

- [ ] **Step 3: 상태·인텐트·이펙트를 넓힌다**

`CanvasToppingPlaceViewModel.kt` — `CanvasToppingPlaceUiState`에 필드를 더한다
(`isDraftLoaded` 아래):

```kotlin
    /** 흐름 진입 때 초안에 못 박힌 캔버스다. 화면이 다시 고르지 않는다 */
    val groupId: GroupId? = null,
    val parfaitId: ParfaitId? = null,
    val nextPositionZ: Int? = null,
    /** 확정 판정의 근거. 그림이 뜨기 전 실측은 폴백 크기라 그대로 올리면 배율이 틀어진다 */
    val isToppingImageReady: Boolean = false,
    val isLoading: Boolean = false,
```

`CanvasToppingPlaceIntent`에 더한다:

```kotlin
    /** 토핑 이미지 painter 가 실제 그림을 들었는지 화면이 알려준다 */
    data class OnToppingImageReadyChanged(
        val isReady: Boolean,
    ) : CanvasToppingPlaceIntent
```

`CanvasToppingPlaceEffect`에서 `ToppingPlaced`를 **지우고** 셋을 더한다:

```kotlin
    /** 초안을 비웠다. 캔버스로 되감으면 새 토핑이 오늘 조회에 함께 내려온다 */
    data object PlaceSucceeded : CanvasToppingPlaceEffect

    /** 다시 눌러 볼 값이 있는 실패. 화면에 남는다 */
    data object PlaceFailed : CanvasToppingPlaceEffect

    /** 다시 눌러도 같은 실패. 알리고 되감는다 */
    data object PlaceFailedPermanently : CanvasToppingPlaceEffect
```

- [ ] **Step 4: 결선한다**

생성자를 바꾼다:

```kotlin
@HiltViewModel
class CanvasToppingPlaceViewModel
@Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
    private val addToppingUseCase: AddToppingUseCase,
) : BaseViewModel<CanvasToppingPlaceUiState, CanvasToppingPlaceIntent, CanvasToppingPlaceEffect>(
    initialState = CanvasToppingPlaceUiState(),
) {
```

`observeDraft`의 `updateState`에 식별값 셋을 더한다:

```kotlin
                updateState {
                    copy(
                        toppingImagePath = draft?.subjectImagePath,
                        borderColorArgb = draft?.borderColorArgb,
                        borderWidthDp = draft?.borderWidthDp,
                        groupId = draft?.groupId,
                        parfaitId = draft?.parfaitId,
                        nextPositionZ = draft?.nextPositionZ,
                        isDraftLoaded = true,
                    )
                }
```

`processIntent`의 `when`에 갈래를 더한다:

```kotlin
            is CanvasToppingPlaceIntent.OnToppingImageReadyChanged -> {
                updateState { copy(isToppingImageReady = intent.isReady) }
            }
```

`handleOnClickConfirm`을 통째로 바꾼다:

```kotlin
    /**
     * 4단계(발급 → 전송 → 확인 → 배치)를 한 덩어리로 본다. 단계별 진행률을 표시하지 않는 것이
     * 스펙의 결정이고, 실패하면 발급부터 전부 다시 탄다.
     *
     * ⚠️ 확정이 도는 동안 화면을 떠나면 `viewModelScope` 취소가 업로드를 끊는다. 확인까지
     * 간 뒤 배치 전에 끊기면 **서버에 고아 이미지가 남는다** — 되돌리지 않기로 한 자리다
     * (`specs/archive/2026-08-20-c106-topping-place-api.md`).
     */
    private fun handleOnClickConfirm() {
        val current = state.value
        if (!current.isDraftLoaded) return

        val imagePath = current.toppingImagePath
        val groupId = current.groupId
        val parfaitId = current.parfaitId
        val positionZ = current.nextPositionZ
        if (imagePath == null || groupId == null || parfaitId == null || positionZ == null) {
            postSideEffect(effect = CanvasToppingPlaceEffect.DraftMissing)
            return
        }

        // 그림이 아직 없으면 실측이 폴백 크기다. 그것으로 계산한 배율이 서버에 굳는다
        val canvasSize = current.canvasSize
        val baseSize = current.toppingBaseSize
        if (!current.isToppingImageReady || canvasSize == null || baseSize == null) return

        val transform = toToppingTransform(
            offsetX = current.offsetX,
            offsetY = current.offsetY,
            scale = current.scale,
            rotationDegrees = current.rotationDegrees,
            canvasSize = canvasSize,
            toppingBaseSize = baseSize,
            positionZ = positionZ,
        )
        val border = toToppingBorder(current.borderColorArgb, current.borderWidthDp)

        launch(key = CONFIRM_JOB_KEY, onError = { postSideEffect(CanvasToppingPlaceEffect.PlaceFailed) }) {
            updateState { copy(isLoading = true) }

            addToppingUseCase(
                groupId = groupId,
                parfaitId = parfaitId,
                filePath = imagePath,
                transform = transform,
                border = border,
            ).onSuccess {
                // 되감기를 먼저 알린다 — clear() 가 초안을 비우면 구독이 알맹이를 null 로
                // 되돌려, 오버레이가 내려간 화면에 빈 캔버스가 잠깐 조작 가능한 상태로 남는다
                postSideEffect(effect = CanvasToppingPlaceEffect.PlaceSucceeded)
                toppingDraftRepository.clear()
            }.onFailure { throwable ->
                val error = throwable as? AppError ?: AppError.Unexpected(throwable)
                postSideEffect(
                    effect = if (error.isPermanentPlaceFailure()) {
                        CanvasToppingPlaceEffect.PlaceFailedPermanently
                    } else {
                        CanvasToppingPlaceEffect.PlaceFailed
                    },
                )
                // 성공 경로에서는 되돌리지 않는다. 화면이 사라지므로 되돌릴 대상이 없고,
                // 내리는 순간 위 주석의 빈 화면이 드러난다
                updateState { copy(isLoading = false) }
            }
        }
    }

    /** 색이나 두께가 빠진 `SOLID` 는 서버가 400 으로 거절한다 — 둘 다 있을 때만 만든다 */
    private fun toToppingBorder(
        colorArgb: Int?,
        widthDp: Float?,
    ): ToppingBorder = if (colorArgb != null && widthDp != null) {
        ToppingBorder.Solid(color = colorArgb.toArgbHexString(), width = widthDp.toDouble())
    } else {
        ToppingBorder.None
    }
```

파일 하단(클래스 밖 상수 자리)에 더한다:

```kotlin
/** 확정 작업의 중복 실행 키. 연타로 두 번 올라가면 고아 이미지와 겹친 토핑이 함께 생긴다 */
private const val CONFIRM_JOB_KEY = "canvas-topping-place-confirm"
```

import를 더한다:

```kotlin
import com.teamyg.parfait.core.util.android.extension.toArgbHexString
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.usecase.topping.AddToppingUseCase
import com.teamyg.parfait.feature.groups.canvas.impl.util.isPermanentPlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.util.toToppingTransform
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

```bash
./gradlew :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck
```

Expected: 전부 PASS. 신규 8건, 삭제 1건.
**`Unresolved reference: ToppingPlaced`가 나오면 Step 1의 삭제 지시를 빠뜨린 것이다.**

- [ ] **Step 6: 커밋한다**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModel.kt \
  feature/groups/canvas/impl/src/test/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/viewmodel/CanvasToppingPlaceViewModelTest.kt
git commit -m "feat(topping): 배치 확정이 서버로 나가게 결선한다"
```

---

## Task 6: 화면과 Route 결선

**Files:**
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasToppingPlaceScreen.kt`
  (컴포저블 선언 + **같은 파일의 `PreviewCanvasToppingPlaceScreen` 호출부**)
- Modify: `feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasToppingPlaceRoute.kt`
- Modify: `feature/groups/canvas/impl/src/main/res/values/strings.xml`

> ⚠️ **`CanvasToppingPlaceScreen` 호출부는 둘이다** — Route 하나와 같은 파일의 `@YGPreview`
> 함수 하나. 파라미터를 더하면서 Preview를 빠뜨리면 main 소스셋이 컴파일되지 않아
> Step 4의 검증이 아예 실행되지 않는다.

**Interfaces:**
- Consumes: Task 5의 `CanvasToppingPlaceIntent.OnToppingImageReadyChanged` ·
  `CanvasToppingPlaceEffect.PlaceSucceeded`·`PlaceFailed`·`PlaceFailedPermanently` ·
  `CanvasToppingPlaceUiState.isLoading`
- Produces: 없음(이 라운드의 마지막 코드 태스크다).

> **테스트가 없는 태스크다.** 이 모듈에 Compose UI 테스트 소스셋이 없고, 여기서 하는 일은
> 이펙트 → 내비게이션·토스트 배선과 painter 상태 전달뿐이라 JVM 테스트로 잡히지 않는다.
> **감지선은 아래 실기기 확인 항목이다.**

- [ ] **Step 1: 화면이 painter 준비 상태를 올려보내게 한다**

`CanvasToppingPlaceScreen.kt` — 함수 시그니처에 파라미터를 더한다
(`onToppingBaseSizeMeasured` 아래):

```kotlin
    onToppingImageReadyChanged: (Boolean) -> Unit,
```

기존 `LaunchedEffect(baseSize, isToppingImageLoaded)` **바로 위**에 더한다:

```kotlin
            // 확정 판정의 근거를 ViewModel 자기 어휘로 올린다 — 실측 방출 가드에 기대면
            // 그 가드를 걷는 순간 확인 버튼이 폴백 크기로 확정을 내보낸다
            LaunchedEffect(isToppingImageLoaded) {
                onToppingImageReadyChanged(isToppingImageLoaded)
            }
```

같은 파일의 `PreviewCanvasToppingPlaceScreen`(`onToppingBaseSizeMeasured = {},` 아래)에도 더한다:

```kotlin
        onToppingImageReadyChanged = {},
```

- [ ] **Step 2: 문구 둘을 더한다**

`feature/groups/canvas/impl/src/main/res/values/strings.xml` —
`canvas_topping_place_draft_unavailable` 아래에 더한다:

```xml
    <string name="canvas_topping_place_failed">토핑을 올리지 못했어요. 잠시 후 다시 시도해 주세요.</string>
    <string name="canvas_topping_place_failed_permanently">이 캔버스에는 더 올릴 수 없어요. 캔버스에서 다시 시작해 주세요.</string>
```

- [ ] **Step 3: Route를 결선한다**

`CanvasToppingPlaceRoute.kt` — 이펙트 `when`에서 `is CanvasToppingPlaceEffect.ToppingPlaced` 갈래를
**지우고** 셋으로 바꾼다:

```kotlin
                // 캔버스를 새로 쌓지 않고 원래 자리로 되감는다. 새로 쌓으면 방금 끝난 토핑 만들기
                // 화면들이 그 밑에 남고, 다음 흐름이 진입하며 비우는 세그멘테이션 캐시가 그 화면들이
                // 가리키던 PNG 를 지운다(뒤로 가면 빈 이미지만 남는다)
                CanvasToppingPlaceEffect.PlaceSucceeded -> navigator.popUpTo<NavKeyCanvasMain>()

                CanvasToppingPlaceEffect.PlaceFailed -> {
                    toastPolicy.showError(context.getString(R.string.canvas_topping_place_failed))
                }

                // 다시 눌러도 같은 실패라 잡아 두지 않는다. 되감아도 막 만든 토핑은 초안에 남는다
                CanvasToppingPlaceEffect.PlaceFailedPermanently -> {
                    toastPolicy.showError(context.getString(R.string.canvas_topping_place_failed_permanently))
                    navigator.popUpTo<NavKeyCanvasMain>()
                }
```

> ⚠️ **영구 실패에서 토스트가 잔상으로 끝난다.** PR4가 같은 자리에서 겪은 것과 같은 문제다 —
> `popUpTo`가 이 Route에 매달린 `toastPolicy`를 같은 프레임에 폐기한다. **그래도 되감는 것이
> 이 갈래의 결정이다**(재시도가 영원히 실패하는 자리라 잡아 두면 사용자가 할 수 있는 일이
> 없다). 되돌아간 캔버스가 오늘 조회를 다시 하므로 사용자는 상태 변화를 본다.
> 문구가 실제로 보이는지는 **실기기 확인 4번**이 판정하고, 안 보이면 캔버스 쪽 토스트 호스트로
> 옮기는 것이 후속이다(OQ-P-167과 같은 축).

`YGScaffoldV2`에 로딩을 넘긴다:

```kotlin
    YGScaffoldV2(
        modifier = modifier,
        isLoading = uiState.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
```

`CanvasToppingPlaceScreen` 호출에 새 파라미터를 넘긴다:

```kotlin
            onToppingImageReadyChanged = { isReady ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingImageReadyChanged(isReady))
            },
```

- [ ] **Step 4: 빌드와 테스트를 전부 돌린다**

```bash
./gradlew :domain:test :data:testDebugUnitTest \
  :core:util:android:testDebugUnitTest \
  :feature:groups:canvas:impl:testDebugUnitTest ktlintCheck :app:assembleDebug
```

Expected: 전부 PASS. `:app:assembleDebug`가 Hilt 그래프 게이트다 —
`AddToppingUseCase`가 처음으로 엔트리포인트에서 도달 가능해지므로, PR1·PR2가 심은
`@Binds`·한정자가 여기서 처음 실제로 검증된다.

- [ ] **Step 5: 커밋한다**

```bash
git add feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/screen/CanvasToppingPlaceScreen.kt \
  feature/groups/canvas/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/canvas/impl/route/CanvasToppingPlaceRoute.kt \
  feature/groups/canvas/impl/src/main/res/values/strings.xml
git commit -m "feat(topping): 배치 화면에 로딩·실패 알림·되감기를 붙인다"
```

---

## Task 7: 문서 반영과 PR6 등록

> **이 태스크만 문서 저장소(`team-yg-pesonal-agent`)에서 한다.** 브랜치를 따로 만들고,
> `git push`·`gh pr create`는 **하지 않는다**.

**Files:**
- Modify(코드 저장소): `domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt`
- Modify: `parfait/specs/archive/2026-08-20-c106-topping-place-api.md`
- Modify: `parfait/specs/README.md`
- Modify: `parfait/plans/README.md`
- Modify: `parfait/synthesis/open-questions.md`
- Modify: `parfait/api/parfait-image.md` · `parfait/api/image.md`(소비처 셈)
- Move: `parfait/plans/2026-08-21-c106-pr5-topping-place-wiring.md` → `parfait/plans/archive/`

- [ ] **Step 1: `AddToppingUseCase`에 취소 함정을 한 줄 남긴다 (OQ-P-248)**

`invoke`의 KDoc에 더한다:

```kotlin
     * ⚠️ 업로드가 확정된 뒤 배치 전에 취소되면 서버에 **이미지만 남는다.** 되돌리지 않는 것이
     * 결정이고(재시도가 이미 고아 S3 객체를 감수한다), `mapErrorToAppError` 가
     * `CancellationException` 을 재던지므로 그 취소는 실패 `Result` 가 아니라 예외로 올라온다.
```

커밋:

```bash
git add domain/src/main/java/com/teamyg/parfait/domain/usecase/topping/AddToppingUseCase.kt
git commit -m "docs(topping): 취소가 남기는 고아 이미지를 KDoc 에 남긴다"
```

- [ ] **Step 2: 스펙의 PR 분할 표를 갱신하고 PR6 행을 만든다**

`parfait/specs/archive/2026-08-20-c106-topping-place-api.md` — 5번 행 「사용자에게 보이는 변화」 칸을
실행 결과로 갱신하고, 표 아래에 6번 행을 더한다:

| # | 브랜치 성격 | 내용 | 사용자에게 보이는 변화 |
|---|---|---|---|
| 6 | 누끼 알맹이 재사용 | 배치 성공 시 **테두리 없는 알맹이**를 최근 이미지에 저장 · 최근 목록에 종류 축 신설 · 알맹이를 고르면 누끼 확인 화면으로 직행 | 갤러리 "최근"에서 이미 만든 누끼를 다시 쓸 수 있다 |

그리고 「범위」의 **제외** 목록에 항목을 더한다:

```
  - **누끼 알맹이의 최근 이미지 재사용** — PR6로 분리했다. 결정 셋(배치 성공 시 저장 · 대상은
    테두리 없는 알맹이 · 최근에서 고르면 누끼 확인 화면 직행)은 PR 분할 표 6번 행에 있고,
    선행 결함 넷은 OQ-P-255에 있다.
```

「실패 처리」 절의 "`code`와 `statusCode`를 **함께** 본다"를 아래로 고친다:

```
분기는 `AppError.Server`의 `code` 하나로 한다. `statusCode`를 조건에 넣지 않는 것이 결정이다 —
되감기 대상 세 코드에는 status로 갈려야 하는 동명 코드가 없고, 서버가 HTTP 200에 실패 봉투를
실으면 `ApiCaller`가 그 값을 `null`로 채워 조건에 넣는 순간 판정이 사라진다(OQ-P-247 해소).
```

- [ ] **Step 3: open-questions를 갱신한다**

`parfait/synthesis/open-questions.md`:

- **OQ-P-109** — 상태를 `해소됨(PR5)`으로 바꾸고 해소 메모를 더한다:
  `@NoBodyLog` + `SelectiveLoggingInterceptor`로 발급 엔드포인트만 `Level.HEADERS`로 낮췄다.
  전체 레벨을 낮추지 않은 이유는 본문 로깅이 다른 엔드포인트에서는 값이 크기 때문이고,
  `BASIC`이 아니라 `HEADERS`인 이유는 새는 값이 **응답 본문에만** 있어 헤더까지 버릴
  이유가 없기 때문이다.
- **OQ-P-246** — 상태를 `해소됨(PR5)`으로 바꾼다. `execute()` → `enqueue` +
  `suspendCancellableCoroutine`·`invokeOnCancellation { call.cancel() }`.
  `onFailure`가 취소를 실패 `Result`로 둔갑시키지 않도록 `continuation.isActive` 가드를 뒀다.
- **OQ-P-247** — 상태를 `해소됨(PR5, ①안)`으로 바꾼다. ③을 먼저 확인한 결과
  `parfait/api/`의 실패 표에 200 + `success=false` 사례가 없고, 그와 별개로 세 코드에
  status로 갈려야 하는 동명 코드가 없어 `code` 단독 판정으로 닫았다.
- **OQ-P-248** — 상태를 `해소됨(PR5, 감수)`으로 바꾼다. ① 취소는 예외로 올라오는 것이 정상
  계약이고 그 시점엔 화면이 없다. ② 고아 이미지는 재시도 결정과 같은 처분으로 감수한다.
  ③ z 겹침은 서버가 유일성을 요구하지 않아 거부되지 않는다. 코드 변경 없이 KDoc 한 줄로 닫았다.
- **OQ-P-245 본문 확장** — 지금은 "편집 화면 굵기와 캔버스 굵기의 어긋남"(배율 축)만 적혀
  있다. **기기 폭 축을 덧붙인다**: 토핑 크기는 캔버스 폭 대비 비율로 정규화되는데
  `borderWidth`만 절대 dp라, 폭이 다른 기기에서 상대 굵기가 달라진다.
- **OQ-P-255 신설** — 제목: `누끼 알맹이를 최근 이미지로 재사용하려면 선행 결함 넷을 먼저 닫아야 한다`.
  본문에 아래 넷을 근거 파일과 함께 적는다:
  1. `FileRecentImageLocalDataSourceImpl#readBytes`가 `contentResolver.openInputStream(uri)`
     전용이라 스킴 없는 절대경로를 못 읽는다. `AddRecentImageUseCase`가 `runSuspendCatching`으로
     감싸므로 **아무 일도 안 일어난 채 성공처럼 지나간다.**
  2. 같은 파일이 확장자를 `.jpg`로 하드코딩한다(`FILE_EXTENSION`). 알맹이는 투명 PNG라 이름이
     거짓이 되고, `ImageUploadRepositoryImpl#contentTypeOf`의 확장자 판정과 부딪힌다.
  3. 최근 목록이 `List<String>`이라 종류 축이 없다. 스키마를 넓히면
     `RecentImageLocalDataSourceImpl#decode`의 `runCatching { … }.getOrDefault(emptyList())`가
     구 스키마 디코드 실패를 삼켜 **기존 목록이 통째로 날아가고 파일은 고아로 남는다**
     (`GetRecentCacheImagesUseCase#clearOutsideDayWindow`가 목록 기준이라 못 지운다).
  4. `NavKeySegmentationConfirm`이 인자 셋을 요구하고 그 화면의 `onClickEditPhoto`가
     `sourceImageUri`·`cutoutImagePath`를 둘 다 쓴다 — 알맹이만 복원하면 **"사진 편집" 버튼이
     죽는다.** 셋을 다 저장하면 내부 저장소 사용량이 3배가 되면서 `MAX_SIZE = 9`와 부딪힌다.

- [ ] **Step 4: 계약 문서의 "소비처 0건" 서술을 갱신한다**

`parfait/api/parfait-image.md`와 `parfait/api/image.md`가 여러 곳에서 **"화면 소비처는
0건"**이라고 적는다. PR5가 그것을 거짓으로 만든다. 아래를 고친다:

- `parfait-image.md` — 배치(POST)의 소비처가 `CanvasToppingPlaceViewModel`임을 적고,
  "남은 것은 화면 결선(스펙의 PR5)"이라는 문장을 걷는다. 나머지 셋(위치·테두리 PATCH·DELETE)은
  **여전히 0건**이므로 그 구분을 분명히 한다.
- `parfait-image.md` — **"소비처가 0건인데 상수를 먼저 둔 것은 명시적 예외"**라는 서술의
  사유가 소멸했다. PR5가 `ServerErrorCode`의 세 코드를 실제로 소비한다.
- `image.md` — 업로드 2 엔드포인트의 소비처가 `ImageUploadRepositoryImpl`을 거쳐
  `AddToppingUseCase`에 닿았음을 적는다.

> 이 저장소는 `sync-teamyg-server-api` 워크플로로 이 문서들을 감사한다. 여기서 안 고치면
> 다음 라운드가 "소비처 0"을 전제로 판단한다.

- [ ] **Step 5: 두 README를 갱신한다**

- `parfait/plans/README.md` — 이 계획의 행을 추가하고 **실행 결과**를 적는다
  (형식은 PR4 행을 따른다: 태스크 수, 브랜치명, 커밋 범위, 신규 테스트 수,
  계획과 갈린 결정, 미확인으로 남긴 것).
- `parfait/specs/README.md` — `c106-topping-place-api` 행의 상태를 갱신한다
  (PR5까지 구현 완료·미머지, PR6 분리 사실).

- [ ] **Step 6: 계획서를 아카이브로 옮긴다**

```bash
git mv parfait/plans/2026-08-21-c106-pr5-topping-place-wiring.md parfait/plans/archive/
```

frontmatter의 `status`를 `done`으로, `archived_reason`을 실행 결과 한 줄로 채운다.

- [ ] **Step 7: 커밋한다**

```bash
git add parfait/
git commit -m "docs: PR5 배치 결선의 결정을 문서에 반영한다"
```

---

## 실기기 확인 (이 라운드 8항목 + 이월 13항목)

> ⚠️ **이 라운드는 JVM 테스트로 잡히지 않는 것이 많다.** 실제 S3 전송·로딩 오버레이·되감기·
> 토스트는 전부 기기에서만 드러난다. `YG_BASE_URL`이 필요하다.

1. 토핑을 배치하고 확인 → **로딩 오버레이가 뜨고 터치가 먹히지 않는다.**
2. 성공 후 캔버스로 돌아오면 **방금 만든 토핑이 목록에 있다.** 위치·크기·회전이 배치 화면에서
   본 것과 같다(모서리에 걸친 것은 캔버스 쪽이 더 잘린다 — 스펙이 적어 둔 알려진 차이다).
3. **테두리가 캔버스에서도 보인다.** 색이 배치 화면과 같다(`#RRGGBB` 왕복이 깨지면 여기서
   테두리만 조용히 사라진다).
4. 영구 실패(다른 기기로 그룹에서 나간 뒤 확인) → **토스트가 보이는가**, 그리고 캔버스로
   되감기는가. **토스트가 잔상으로 끝나면 Task 6 Step 3의 경고대로 후속이 필요하다.**
5. 비행기 모드로 확인 → 토스트가 뜨고 **화면에 남는다.** 다시 켜고 확인하면 성공한다.
6. 확인을 **연타**해도 토핑이 하나만 올라간다.
7. 로딩 중 **뒤로 가기** → 업로드가 끊긴다(Logcat에 `callTimeout`까지 남는 요청이 없다).
8. 큰 원본 사진(12MP)으로 전체 흐름 → **OOM 없이** 업로드가 끝난다(OQ-P-228이 열려 있는 축이라
   증상이 나오면 그 항목에 실측을 남긴다).
9. **토핑을 캔버스 밖으로 넘칠 만큼 키워서** 배치 → 캔버스에서 **같은 크기로 잘려** 보인다.
   Task 2 Step 3의 `requiredSize` 전환이 감지선 없이 들어가는 유일한 자리다. 수정 전에는
   토핑이 잘리는 대신 통째로 작아졌으므로, **차이가 눈에 보여야 한다.**

**이월 13항목** — PR3의 5항목과 PR4의 8항목이 아직 안 됐다. 세 브랜치가 한 스택이라
이 라운드에서 함께 확인하고, 각 계획서의 해당 절에 결과를 적는다.

---

## 이 라운드가 하지 않는 것

- **누끼 알맹이의 최근 이미지 재사용** — PR6. 결정 셋은 스펙 PR 분할 표 6번 행, 선행 결함
  넷은 OQ-P-255.
- **토핑 수정·삭제·테두리 재편집 API** — C-301 라운드.
- **배경 이미지 업로드** — C-301.
- **배치 화면의 실제 배경·기존 토핑 미리보기** — OQ-P-240을 연 채로 둔다.
- **고아 `PENDING` 이미지·S3 객체 정리** — 서버에 경로가 없고, 이 라운드가 그 발생률을 처음으로
  실제화한다.
- **확인 버튼 비활성 표현** — 디자인 근거가 없다. 판정만 ViewModel이 갖는다.
- **presigned URL 만료 판정** — 만료는 실패 후 전량 재시도로만 풀린다.
- **원본 다운샘플** — OQ-P-228 잔존.
- **초안 구독이 끊긴 뒤의 재구독** — `observeDraft`가 실패하면 `isDraftLoaded`가 `false`로 굳고
  확인이 아무 반응 없이 흘러간다. 첫 `DraftMissing` 토스트를 놓치면 되돌릴 길이 없다. PR4에서
  넘어온 기존 동작이라 이 라운드가 손대지 않는다.
- **테두리 굵기가 캔버스 폭을 안 따라간다** — 토핑 크기는 캔버스 폭 대비 비율로 정규화되는데
  `borderWidth`만 절대 dp라, 폭이 다른 기기에서 상대 굵기가 달라진다. OQ-P-245가 열어 둔 것은
  배율 축뿐이고 이 기기 폭 축은 Task 7이 그 항목에 덧붙인다.
- **`ImageUploadRepositoryImpl#upload`의 `file.isFile`이 호출자 스레드에서 도는 것** — Task 3이
  업로드 사슬의 마지막 IO 홉을 걷으면서 더 도드라지지만 이 라운드가 만든 문제가 아니다.

---

## as-built 정정 (2026-08-21, 최종 브랜치 리뷰 + 주석 정리 이후)

이 계획서 본문(Task 1~7)은 **실행 전 문서라 고치지 않는다** — 계획과 실행이 갈린 곳은 실행
결과 문서([plans/README.md](../README.md) PR5 행)에 적는 것이 이 저장소의 관례다. 이 절은 그
관례에 따라 계획 본문과 갈린 지점만 모은다. 원문은 그대로 두고 정정만 여기 적는다.

계획 실행 직후(Task 1~7 통과 시점)의 수치는 커밋 7개 `17a2c09f..56a1c0c4`, 신규 테스트 25건 +
삭제 1건, 20파일 886삽입/63삭제였다. **그 뒤 최종 브랜치 리뷰(opus)가 지적 8건을 냈고**, fix
웨이브 한 번(`80ba8805`)과 별도의 주석·KDoc 정리 커밋(`c443846b`)이 더 붙어 아래로 갈렸다.

| 원 서술(Task 1~7 통과 시점) | as-built 정정 |
|---|---|
| 영구 실패 세 코드(`PARFAIT_ALREADY_CLOSED`·`GROUP_NOT_JOINED`·`PARFAIT_NOT_FOUND`)에서 캔버스로 되감는다 | **되감지 않는다 — 알린 뒤 화면에 남는다.** 최종 리뷰가 Critical로 잡았다: `CanvasToppingPlaceRoute`의 `toastPolicy`가 `rememberYGToastPolicy()`로 Route 컴포지션에 매달려 있어, `popUpTo`가 Route를 접는 같은 프레임에 안내(토스트)까지 함께 폐기된다. `DraftMissing`을 안 되감는 이유로 이미 주석에 적어 둔 함정을 바로 아래 영구 실패 갈래가 반복하고 있었다. 처방은 `DraftMissing`과 같은 처분(알린 뒤 화면에 남긴다, 닫기 버튼이 이미 있어 막다른 곳이 아니다) — 진짜 처방(안내를 캔버스 쪽 토스트 호스트로 보내기)은 OQ-P-167 소관이라 이 라운드 밖으로 미뤘다. **성공 경로의 `popUpTo`는 그대로다.** |
| 영구 실패 코드 셋(`PARFAIT_ALREADY_CLOSED`·`GROUP_NOT_JOINED`·`PARFAIT_NOT_FOUND`) | **다섯**으로 늘었다 — 최종 리뷰가 배치 POST 실패 표의 400 둘(`Common.INVALID_REQUEST`·`ParfaitImage.INVALID_BORDER`)이 빠졌다고 지적했다. 재시도가 발급부터 4단계를 전부 다시 태워도 결과가 항상 같은 400이라, 확인을 누를 때마다 참조되지 않는 이미지만 쌓인다. |
| 커밋 7개 `17a2c09f..56a1c0c4` | **커밋 10개** `17a2c09f..c443846b` — 위 fix 웨이브(`80ba8805`, 최종 리뷰 지적 8건)와 주석·KDoc 정리(`c443846b`, 규약을 어기거나 낡은 자리 여덟)가 더 붙었다. |
| 신규 테스트 25건 + 삭제 1건, 20파일 886삽입/63삭제 | **신규 테스트 29건**(계획 25건 + 최종 리뷰가 심은 4건) + 삭제 1건, **23파일 961삽입/64삭제**. 신규 테스트 파일이 셋(`ToppingPlaceFailureTest`·`ToppingPlacementTest`·`SelectiveLoggingInterceptorTest`)에서 **넷**(`ImageServiceTest` 추가)으로, 기존 파일 확장이 셋에서 **넷**(`NetworkModuleTest` 추가)으로 늘었다. |
| `ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED` KDoc — "토핑 추가 흐름이 이 코드를 받으면 화면 이동은 그대로 진행하고 실패만 알린다" | 되감기 철회로 이 문장이 거짓이 됐다 — 주석 정리 커밋(`c443846b`)에서 "화면 이동에 대한 단정"만 걷었다. "되돌리지 않는다"(업로드 이미지를 롤백하지 않는다)는 여전히 참이라 남겼다. 소비처가 화면을 어떻게 다루는지는 이 상수가 알 바가 아니다. |

**실기기 확인은 여전히 안 했다** — 위 정정은 전부 코드 검토·자동 테스트로 확인했고, 실기기
확인 항목(이 라운드 9항목 + PR3·PR4 이월 13항목)은 원 서술 그대로 미수행이다.

문서 반영: [c106-topping-place-api 스펙](../../specs/archive/2026-08-20-c106-topping-place-api.md) 실패
처리 절 · [parfait-image.md](../../../api/parfait-image.md) · [plans/README.md](../README.md) PR5
행 · [specs/README.md](../../specs/README.md) ⑤ 문단 · [open-questions](../../../synthesis/open-questions.md)
OQ-P-167(캔버스 토스트 호스트 필요 사례 둘째)·**OQ-P-256**(신설 — `Int.toRgbHexString()`의
`require()` 불투명 가드가 `toToppingBorder` 호출부에서 `launch`의 `try` 밖에 있어, 팔레트에
반투명 색이 들어오면 크래시하는 파킹된 부작용).
