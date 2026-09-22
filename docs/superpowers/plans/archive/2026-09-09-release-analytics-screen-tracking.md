---
id: release-analytics-screen-tracking
title: 화면 진입 계측과 기기·앱 사용자 속성 구현 계획
status: done
type: work-order
created: 2026-09-09
updated: 2026-09-09
platforms: android
owner: Parfait Android
related_adr: ADR-0031
related_spec: release-analytics-screen-tracking
related_code:
  - MainRoute.kt#MainRoute
  - MainActivity.kt#MainActivity
  - BaseApplication.kt#BaseApplication
  - Navigator.kt#Navigator
  - app/build.gradle.kts
archived_reason: develop 머지 완료(2026-09-09, PR #478 `c56ed15eb`) — 대응 스펙도 `specs/archive/`로 이동
tags: [plan, parfait, analytics]
---

# 화면 진입 계측과 기기·앱 사용자 속성 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

> ✅ **완료·develop 머지(2026-09-09, PR #478 `c56ed15eb`).** 머지본이 스펙과 어긋나는 자리는
> 없다. 다만 **이 계획 문서 자체가 두 자리에서 낡았다** — 아래 Architecture 절이 트래커를
> `@Singleton`이라고 적었는데 실제 스코프는 `@ActivityRetainedScoped`이고(`@Singleton`이면
> `Navigator`보다 오래 살아 그 실행의 A-001이 통째로 빠진다), 판정 기준도 "백스택 최상단"이
> 아니라 **크기와 최상단의 짝**이다. 정본은
> [스펙](../../specs/archive/2026-09-09-release-analytics-screen-tracking.md) 「중복 억제와 Activity 재생성」.
> ⚠️ **체크박스는 실행 세션이 남기지 않아 대부분 미체크로 남아 있다**(41개 중 3개만 체크).
> 진행의 정본은 `git log`이고, 이 문서의 체크 상태는 근거가 아니다.

**Goal:** 사용자가 어느 화면에 들어오는지를 기획의 화면 ID(`C-001`·`G-001` 등)로 Firebase
Analytics에 보내고, 기기·앱 정보 7종을 사용자 속성으로 함께 싣는다.

**Architecture:** 분석 코드는 전부 `:app`의 `com.teamyg.parfait.analytics` 패키지에 있다.
`NavKey`를 화면 ID로 바꾸는 매핑은 순수 함수 하나(`toAnalyticsScreenOrNull`)이고, 전송 여부
판정은 `@Singleton ScreenViewTracker` 하나가 든다. `MainRoute`가 백스택 최상단을 보고 그
트래커를 부른다. feature·core 모듈은 바뀌지 않는다.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation3, Hilt, Firebase Analytics(BOM 34.18.0),
JUnit4 + `kotlin.test`, MockK.

**Spec:** [`parfait/specs/2026-09-09-release-analytics-screen-tracking.md`](../../specs/archive/2026-09-09-release-analytics-screen-tracking.md)

## Global Constraints

- **작업 대상 저장소는 `TJYG-Android`다.** 이 계획서가 있는 위키 저장소가 아니다.
- **브랜치는 이미 있는 `feature/release-time-ga`를 쓴다.** 새 브랜치를 만들지 않고 워크트리도
  만들지 않는다. 이 브랜치의 분기점은 `origin/develop` 팁 `acbc4b457`이다.
- **Task 마다 커밋한다.** 사용자가 요청했다. 커밋 메시지는 이 저장소 관례를 따라
  `feat: <한국어 현재형 서술>` 형태로 쓰고, 마지막 줄에
  `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>` 를 넣는다.
- **`git push` 와 PR 생성은 하지 않는다.** 리모트로 나가는 작업은 사용자 확인이 먼저다.
- **feature·core 모듈을 고치지 않는다.** 새 파일은 전부 `:app` 안에 만든다.
- **기존 파일을 전문으로 덮어쓰지 않는다.** 지정한 자리에만 추가·치환한다.
- **Robolectric·계측 테스트 소스셋을 새로 들이지 않는다.** JVM 유닛 테스트로 덮이지 않는
  부분은 실기기 확인으로 넘긴다.
- **파라미터 이름에 `firebase_screen`을 쓰지 않는다.** `firebase_`는 GA4 예약 접두사라
  수동 전송분이 버려질 수 있다. 반드시 `FirebaseAnalytics.Param.SCREEN_NAME`·`SCREEN_CLASS`
  상수를 쓴다.
- **화면 클래스명을 리플렉션으로 얻지 않는다.** release는 R8 난독화가 켜져 있어
  `this::class.simpleName`이 뭉개진다. 화면 ID와 클래스명 모두 코드에 박은 문자열 상수다.
- **주석 규약**(`parfait/CLAUDE.md`):
  - 코드가 이미 말하는 것은 쓰지 않는다.
  - `@return`·`@param`은 타입·이름이 말하지 못할 때만 쓴다.
  - 다른 컴포넌트의 현재 상태를 단정하지 않는다. 낡는다. 단정 대신 근거 문서를 가리킨다.
- 각 Task 끝에서 `./gradlew :app:ktlintCheck`를 돌려 포맷을 확인한다.

---

## 파일 구성

**새로 만드는 파일** (모두 `app/src/main/java/com/teamyg/parfait/analytics/` 아래)

| 파일 | 책임 |
|---|---|
| `AnalyticsLogger.kt` | 분석 도구로 나가는 인터페이스와 `AnalyticsScreen` 값 타입 |
| `NavKeyAnalyticsScreen.kt` | `NavKey` → 화면 ID 매핑 전체 |
| `ScreenViewTracker.kt` | 중복 판정과 전송 |
| `DeviceInfo.kt` | 기기·앱 값과 조립 함수 |
| `AnalyticsUserProperty.kt` | 사용자 속성 이름 상수 |
| `FirebaseAnalyticsLogger.kt` | `FirebaseAnalytics`에 위임하는 유일한 구현체 |
| `di/AnalyticsModule.kt` | Hilt 바인딩 |

**새로 만드는 테스트** (모두 `app/src/test/java/com/teamyg/parfait/analytics/` 아래)

`NavKeyAnalyticsScreenTest.kt`, `ScreenViewTrackerTest.kt`, `DeviceInfoTest.kt`

**고치는 파일**

| 파일 | 무엇을 |
|---|---|
| `app/build.gradle.kts` | `buildTypes`를 열어 `ANALYTICS_IS_DEBUG` 필드 추가 |
| `app/src/main/AndroidManifest.xml` | 자동 화면 보고 끄기 `meta-data` |
| `BaseApplication.kt` | 수집 활성화와 사용자 속성 7종 설정 |
| `MainActivity.kt` | `ScreenViewTracker` 주입과 전달 |
| `MainRoute.kt` | 백스택 관찰 `LaunchedEffect` 추가 |

---

## Task 1: 화면 ID 매핑

**Files:**
- Create: `app/src/main/java/com/teamyg/parfait/analytics/AnalyticsLogger.kt`
- Create: `app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`
- Test: `app/src/test/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreenTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `data class AnalyticsScreen(val screenId: String, val screenClass: String)`
  - `interface AnalyticsLogger { fun setCollectionEnabled(enabled: Boolean); fun setUserProperty(name: String, value: String); fun logScreenView(screen: AnalyticsScreen) }`
  - `fun NavKey.toAnalyticsScreenOrNull(): AnalyticsScreen?`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreenTest.kt`

```kotlin
package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.NavKeyCameraSystem
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.common.terms.api.NavKeyWebView
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSelect
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupCreate
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupInviteCode
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupNickName
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting
import com.teamyg.parfait.feature.intro.api.NavKeySplash
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
import com.teamyg.parfait.feature.login.api.NavKeyLogin
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavKeyAnalyticsScreenTest {
    @Test
    fun toAnalyticsScreenOrNull_fixedKeys_returnMappedScreenId() {
        // Given 인자로 갈리지 않는 화면들
        val expected = mapOf<NavKey, String>(
            NavKeySplash to "A-001",
            NavKeyLogin to "A-002",
            NavKeyTermAgree(registrationToken = "token") to "A-003",
            NavKeyGroupInviteCode to "A-004",
            NavKeyGroupNickName(inviteCode = "CODE", groupName = "그룹", nickName = "닉") to "A-004-naming",
            NavKeyGroupCreate(nickName = "닉") to "A-005",
            NavKeyGroupList to "G-001",
            NavKeyCanvasMain(groupId = 1L) to "C-001",
            NavKeyCameraSystem to "C-101-system",
            NavKeySystemGalleryPicker to "C-102-system",
            NavKeySegmentation(sourceImageUri = "uri") to "C-103",
            NavKeySegmentationConfirm(
                sourceImageUri = "uri",
                subjectImagePath = "subject",
                trimmedSubjectImagePath = "trimmed",
            ) to "C-103-select",
            NavKeyCanvasToppingPlace to "C-106",
            NavKeyCanvasImageSave(imagePath = "path", date = "2026-09-09") to "C-001-image-save",
            NavKeyCanvasEdit(imageUri = "uri") to "C-001-edit",
            NavKeyCanvasImageSelect to "C-001-image-select",
            NavKeyCanvasMove(imageUri = "uri") to "C-001-move",
            NavKeyAppSetting to "S-001",
            NavKeyAccountInfo to "S-002",
            NavKeyGroupSetting(groupId = 1L) to "S-101",
            NavKeyWebView(title = "약관", url = "https://example.com") to "S-004",
        )

        // When, Then 각 키가 그 화면 ID 로 간다
        expected.forEach { (navKey, screenId) ->
            assertEquals(screenId, navKey.toAnalyticsScreenOrNull()?.screenId, "$navKey")
        }
    }

    @Test
    fun toAnalyticsScreenOrNull_anyMappedKey_carriesNavKeyNameAsScreenClass() {
        // Given, When 매핑된 키
        val screen = NavKeyCanvasMain(groupId = 1L).toAnalyticsScreenOrNull()

        // Then 클래스명은 리플렉션이 아니라 상수라 R8 난독화에도 살아남는다
        assertEquals("NavKeyCanvasMain", screen?.screenClass)
    }

    @Test
    fun toAnalyticsScreenOrNull_cameraCustom_splitsByReturnResultOnly() {
        // Given, When 토핑 생성 진입과 편집 모드 진입
        val topping = NavKeyCameraCustom(returnResultOnly = false)
        val edit = NavKeyCameraCustom(returnResultOnly = true)

        // Then
        assertEquals("C-101", topping.toAnalyticsScreenOrNull()?.screenId)
        assertEquals("C-302", edit.toAnalyticsScreenOrNull()?.screenId)
    }

    @Test
    fun toAnalyticsScreenOrNull_customGalleryPicker_splitsByReturnResultOnly() {
        // Given, When
        val topping = NavKeyCustomGalleryPicker(
            recentImagePick = RecentImagePick.CUTOUT,
            returnResultOnly = false,
        )
        val edit = NavKeyCustomGalleryPicker(
            recentImagePick = RecentImagePick.SOURCE,
            returnResultOnly = true,
        )

        // Then
        assertEquals("C-102", topping.toAnalyticsScreenOrNull()?.screenId)
        assertEquals("C-303", edit.toAnalyticsScreenOrNull()?.screenId)
    }

    @Test
    fun toAnalyticsScreenOrNull_pictureConfirm_splitsBySourceAndReturnResultOnly() {
        // Given, When 두 인자를 함께 봐 넷으로 갈린다
        val expected = mapOf(
            NavKeyPictureConfirm("uri", PictureConfirmSource.CAMERA, false) to "C-101-confirm",
            NavKeyPictureConfirm("uri", PictureConfirmSource.CAMERA, true) to "C-302-confirm",
            NavKeyPictureConfirm("uri", PictureConfirmSource.GALLERY, false) to "C-102-confirm",
            NavKeyPictureConfirm("uri", PictureConfirmSource.GALLERY, true) to "C-303-confirm",
        )

        // Then
        expected.forEach { (navKey, screenId) ->
            assertEquals(screenId, navKey.toAnalyticsScreenOrNull()?.screenId, "$navKey")
        }
    }

    @Test
    fun toAnalyticsScreenOrNull_toppingEdit_mergesBorderOnlyIntoOneId() {
        // Given, When borderOnly 진입은 두 경로에서 오는데 키만으로는 갈리지 않는다
        val area = NavKeyToppingEdit(sourceImageUri = "src", segmentationImageUri = "seg")
        val border = NavKeyToppingEdit(
            sourceImageUri = "src",
            segmentationImageUri = "seg",
            borderOnly = true,
        )

        // Then 둘 중 하나로 몰지 않고 합친 값을 쓴다
        assertEquals("C-104", area.toAnalyticsScreenOrNull()?.screenId)
        assertEquals("C-105/C-306", border.toAnalyticsScreenOrNull()?.screenId)
    }

    @Test
    fun toAnalyticsScreenOrNull_canvasBGEdit_splitsByInitialToppingId() {
        // Given, When 편집 모드 진입과 특정 토핑을 탭한 진입
        val mode = NavKeyCanvasBGEdit(groupId = 1L, parfaitId = 2L)
        val topping = NavKeyCanvasBGEdit(groupId = 1L, parfaitId = 2L, initialToppingId = 3L)

        // Then
        assertEquals("C-301", mode.toAnalyticsScreenOrNull()?.screenId)
        assertEquals("C-305", topping.toAnalyticsScreenOrNull()?.screenId)
    }

    @Test
    fun toAnalyticsScreenOrNull_unmappedKey_returnsNull() {
        // Given 매핑에 없는 키(새 화면을 만들고 매핑을 잊은 상황)
        val unmapped = object : NavKey {}

        // When, Then 조용히 아무 ID 나 돌려주지 않는다
        assertNull(unmapped.toAnalyticsScreenOrNull())
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "com.teamyg.parfait.analytics.NavKeyAnalyticsScreenTest"`
Expected: 컴파일 실패. `AnalyticsScreen`·`toAnalyticsScreenOrNull` 미해결.

- [ ] **Step 3: 계약을 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/AnalyticsLogger.kt`

```kotlin
package com.teamyg.parfait.analytics

/**
 * 분석 도구로 나가는 유일한 창구.
 *
 * [setCollectionEnabled] 가 여기 있는 이유는 수집을 켜는 자리(`BaseApplication`)가
 * `FirebaseAnalytics` 를 직접 잡지 않게 하기 위해서다.
 */
interface AnalyticsLogger {
    fun setCollectionEnabled(enabled: Boolean)

    fun setUserProperty(
        name: String,
        value: String,
    )

    fun logScreenView(screen: AnalyticsScreen)
}

/**
 * @property screenClass `NavKey` 이름. 리플렉션이 아니라 상수다 —
 *   release 는 R8 난독화가 켜져 있어 `simpleName` 이 뭉개진다
 */
data class AnalyticsScreen(
    val screenId: String,
    val screenClass: String,
)
```

- [ ] **Step 4: 매핑을 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt`

⚠️ **arm 사이 빈 줄은 취향이 아니다.** 멀티라인 arm 이 하나라도 있으면 `.editorconfig` 의
`ktlint_standard_blank-line-between-when-conditions` 가 **모든** arm 사이를 요구한다
(선례: `core/ui` 의 `NameValidResultUiText.kt`). 아래 형태 그대로 쓴다.

```kotlin
package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.NavKeyCameraSystem
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.common.terms.api.NavKeyWebView
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSelect
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupCreate
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupInviteCode
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupNickName
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting
import com.teamyg.parfait.feature.intro.api.NavKeySplash
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
import com.teamyg.parfait.feature.login.api.NavKeyLogin
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit

/**
 * 화면 ID 체계와 각 값의 근거는 `parfait/specs/2026-09-09-release-analytics-screen-tracking.md`
 * 매핑표에 있다.
 *
 * `NavKey` 는 sealed 가 아니라 `when` 이 빠짐없음을 강제하지 못한다. 대응이 없으면 `null` 이고,
 * 부르는 쪽이 그것을 경고로 남긴다.
 */
fun NavKey.toAnalyticsScreenOrNull(): AnalyticsScreen? = when (this) {
    is NavKeySplash -> AnalyticsScreen("A-001", "NavKeySplash")

    is NavKeyLogin -> AnalyticsScreen("A-002", "NavKeyLogin")

    is NavKeyTermAgree -> AnalyticsScreen("A-003", "NavKeyTermAgree")

    is NavKeyGroupInviteCode -> AnalyticsScreen("A-004", "NavKeyGroupInviteCode")

    is NavKeyGroupNickName -> AnalyticsScreen("A-004-naming", "NavKeyGroupNickName")

    is NavKeyGroupCreate -> AnalyticsScreen("A-005", "NavKeyGroupCreate")

    is NavKeyGroupList -> AnalyticsScreen("G-001", "NavKeyGroupList")

    is NavKeyCanvasMain -> AnalyticsScreen("C-001", "NavKeyCanvasMain")

    is NavKeyCameraCustom -> AnalyticsScreen(
        screenId = if (returnResultOnly) "C-302" else "C-101",
        screenClass = "NavKeyCameraCustom",
    )

    is NavKeyCameraSystem -> AnalyticsScreen("C-101-system", "NavKeyCameraSystem")

    is NavKeyPictureConfirm -> AnalyticsScreen(
        screenId = when (source) {
            PictureConfirmSource.CAMERA -> if (returnResultOnly) "C-302-confirm" else "C-101-confirm"
            PictureConfirmSource.GALLERY -> if (returnResultOnly) "C-303-confirm" else "C-102-confirm"
        },
        screenClass = "NavKeyPictureConfirm",
    )

    is NavKeyCustomGalleryPicker -> AnalyticsScreen(
        screenId = if (returnResultOnly) "C-303" else "C-102",
        screenClass = "NavKeyCustomGalleryPicker",
    )

    is NavKeySystemGalleryPicker -> AnalyticsScreen("C-102-system", "NavKeySystemGalleryPicker")

    is NavKeySegmentation -> AnalyticsScreen("C-103", "NavKeySegmentation")

    is NavKeySegmentationConfirm -> AnalyticsScreen("C-103-select", "NavKeySegmentationConfirm")

    // 두 경로(최근 알맹이 재사용·편집 모드 테두리)가 같은 키로 와 키만으로는 갈리지 않는다
    is NavKeyToppingEdit -> AnalyticsScreen(
        screenId = if (borderOnly) "C-105/C-306" else "C-104",
        screenClass = "NavKeyToppingEdit",
    )

    is NavKeyCanvasToppingPlace -> AnalyticsScreen("C-106", "NavKeyCanvasToppingPlace")

    is NavKeyCanvasBGEdit -> AnalyticsScreen(
        screenId = if (initialToppingId == null) "C-301" else "C-305",
        screenClass = "NavKeyCanvasBGEdit",
    )

    is NavKeyCanvasImageSave -> AnalyticsScreen("C-001-image-save", "NavKeyCanvasImageSave")

    is NavKeyCanvasEdit -> AnalyticsScreen("C-001-edit", "NavKeyCanvasEdit")

    is NavKeyCanvasImageSelect -> AnalyticsScreen("C-001-image-select", "NavKeyCanvasImageSelect")

    is NavKeyCanvasMove -> AnalyticsScreen("C-001-move", "NavKeyCanvasMove")

    is NavKeyAppSetting -> AnalyticsScreen("S-001", "NavKeyAppSetting")

    is NavKeyAccountInfo -> AnalyticsScreen("S-002", "NavKeyAccountInfo")

    is NavKeyGroupSetting -> AnalyticsScreen("S-101", "NavKeyGroupSetting")

    is NavKeyWebView -> AnalyticsScreen("S-004", "NavKeyWebView")

    else -> null
}
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "com.teamyg.parfait.analytics.NavKeyAnalyticsScreenTest"`
Expected: PASS (테스트 8건)

- [ ] **Step 6: 포맷을 확인한다**

Run: `./gradlew :app:ktlintCheck`
Expected: 통과. 실패하면 `./gradlew :app:ktlintFormat` 후 다시 확인한다. 실패하면 `./gradlew :app:ktlintFormat` 후 다시 확인한다.

- [ ] **Step 7: 커밋한다**

```bash
git add app/src/main/java/com/teamyg/parfait/analytics/AnalyticsLogger.kt app/src/main/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreen.kt app/src/test/java/com/teamyg/parfait/analytics/NavKeyAnalyticsScreenTest.kt
git commit -m "feat: NavKey 를 화면 ID 로 바꾸는 매핑을 둔다" \
  -m "Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 전송 판정 트래커

**Files:**
- Create: `app/src/main/java/com/teamyg/parfait/analytics/ScreenViewTracker.kt`
- Test: `app/src/test/java/com/teamyg/parfait/analytics/ScreenViewTrackerTest.kt`

**Interfaces:**
- Consumes: `AnalyticsLogger`, `AnalyticsScreen`, `NavKey.toAnalyticsScreenOrNull()` (Task 1)
- Produces: `class ScreenViewTracker @Inject constructor(analyticsLogger: AnalyticsLogger) { fun track(backStackSize: Int, top: NavKey?) }`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/teamyg/parfait/analytics/ScreenViewTrackerTest.kt`

```kotlin
package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeAnalyticsLogger : AnalyticsLogger {
    val screenViews = mutableListOf<AnalyticsScreen>()

    override fun setCollectionEnabled(enabled: Boolean) = Unit

    override fun setUserProperty(
        name: String,
        value: String,
    ) = Unit

    override fun logScreenView(screen: AnalyticsScreen) {
        screenViews += screen
    }
}

class ScreenViewTrackerTest {
    private val logger = FakeAnalyticsLogger()
    private val tracker = ScreenViewTracker(logger)

    @Test
    fun track_sameSizeAndKeyTwice_logsOnce() {
        // Given, When 컴포지션이 죽었다 살아나 같은 상태를 다시 방출한 경우
        tracker.track(backStackSize = 1, top = NavKeyGroupList)
        tracker.track(backStackSize = 1, top = NavKeyGroupList)

        // Then 이동이 없었으므로 한 번만 나간다
        assertEquals(listOf("G-001"), logger.screenViews.map { it.screenId })
    }

    @Test
    fun track_sameKeyPushedOnTopOfItself_logsAgain() {
        // Given, When 그룹 목록에서 그룹 목록 딥링크를 탭해 같은 값이 겹쳐 쌓인 경우.
        // Navigator.goTo 는 조건 없이 add 한다
        tracker.track(backStackSize = 1, top = NavKeyGroupList)
        tracker.track(backStackSize = 2, top = NavKeyGroupList)

        // Then 화면은 실제로 바뀌었으므로 두 번 나간다
        assertEquals(listOf("G-001", "G-001"), logger.screenViews.map { it.screenId })
    }

    @Test
    fun track_backToPreviousScreen_logsThatScreenAgain() {
        // Given, When 목록 → 캔버스 → 뒤로 가기
        tracker.track(backStackSize = 1, top = NavKeyGroupList)
        tracker.track(backStackSize = 2, top = NavKeyCanvasMain(groupId = 1L))
        tracker.track(backStackSize = 1, top = NavKeyGroupList)

        // Then 복귀도 조회로 센다
        assertEquals(
            listOf("G-001", "C-001", "G-001"),
            logger.screenViews.map { it.screenId },
        )
    }

    @Test
    fun track_unmappedKey_logsNothing() {
        // Given 매핑에 없는 키
        val unmapped = object : NavKey {}

        // When
        tracker.track(backStackSize = 1, top = unmapped)

        // Then 아무 ID 나 보내지 않는다
        assertEquals(emptyList(), logger.screenViews)
    }

    @Test
    fun track_nullTop_logsNothing() {
        // Given, When 백스택이 빈 순간
        tracker.track(backStackSize = 0, top = null)

        // Then
        assertEquals(emptyList(), logger.screenViews)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "com.teamyg.parfait.analytics.ScreenViewTrackerTest"`
Expected: 컴파일 실패. `ScreenViewTracker` 미해결.

- [ ] **Step 3: 트래커를 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/ScreenViewTracker.kt`

```kotlin
package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.util.jvm.analytics.Loggers
import javax.inject.Inject
import javax.inject.Singleton

private val logger = Loggers.create("ScreenViewTracker")

/**
 * 화면 진입을 보낼지 판정한다.
 *
 * 마지막 전송 값을 컴포지션 밖에 들기 위해 `@Singleton` 이다. 수집기가 사는
 * `LaunchedEffect` 는 Activity 가 재생성되면 다시 시작하고, 그때 이동이 없었는데도
 * 같은 상태가 한 번 더 방출된다.
 *
 * 판정에 백스택 크기가 함께 들어가는 이유는 같은 값의 키가 겹쳐 쌓일 수 있어서다
 * (`Navigator#goTo` 는 조건 없이 더한다). 근거는
 * `parfait/adr/0031-analytics-central-screen-mapping.md`.
 */
@Singleton
class ScreenViewTracker @Inject constructor(
    private val analyticsLogger: AnalyticsLogger,
) {
    private var lastTracked: Pair<Int, NavKey?>? = null

    fun track(
        backStackSize: Int,
        top: NavKey?,
    ) {
        val current = backStackSize to top
        if (current == lastTracked) return
        lastTracked = current

        if (top == null) return

        val screen = top.toAnalyticsScreenOrNull()
        if (screen == null) {
            logger.w { "화면 ID 매핑이 없다: ${top.javaClass.name}" }
            return
        }

        analyticsLogger.logScreenView(screen)
    }
}
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "com.teamyg.parfait.analytics.ScreenViewTrackerTest"`
Expected: PASS (테스트 5건)

- [ ] **Step 5: 포맷을 확인한다**

Run: `./gradlew :app:ktlintCheck`
Expected: 통과. 실패하면 `./gradlew :app:ktlintFormat` 후 다시 확인한다.

- [ ] **Step 6: 커밋한다**

```bash
git add app/src/main/java/com/teamyg/parfait/analytics/ScreenViewTracker.kt app/src/test/java/com/teamyg/parfait/analytics/ScreenViewTrackerTest.kt
git commit -m "feat: 화면 진입 전송 판정을 ScreenViewTracker 한 곳에 둔다" \
  -m "Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 기기·앱 사용자 속성 값

**Files:**
- Create: `app/src/main/java/com/teamyg/parfait/analytics/AnalyticsUserProperty.kt`
- Create: `app/src/main/java/com/teamyg/parfait/analytics/DeviceInfo.kt`
- Test: `app/src/test/java/com/teamyg/parfait/analytics/DeviceInfoTest.kt`

**Interfaces:**
- Consumes: 없음
- Produces:
  - `object AnalyticsUserProperty { const val IS_DEBUG; OS_TYPE; OS_VER; APP_VER; APP_VER_CODE; DEVICE; APP_ID }`
  - `data class DeviceInfo(osType, osVer, appVer, appVerCode, device, appId: String)`
  - `fun buildDeviceInfo(osVer: String, appVer: String, appVerCode: Int, manufacturer: String, model: String, appId: String): DeviceInfo`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`app/src/test/java/com/teamyg/parfait/analytics/DeviceInfoTest.kt`

```kotlin
package com.teamyg.parfait.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceInfoTest {
    @Test
    fun buildDeviceInfo_givenBuildValues_assemblesEachProperty() {
        // Given, When
        val info = buildDeviceInfo(
            osVer = "14",
            appVer = "1.1.1",
            appVerCode = 8,
            manufacturer = "samsung",
            model = "SM-S928N",
            appId = "com.teamyg.parfait",
        )

        // Then 코드는 문자열로 싣는다 — 사용자 속성 값은 String 만 받는다
        assertEquals("Android", info.osType)
        assertEquals("14", info.osVer)
        assertEquals("1.1.1", info.appVer)
        assertEquals("8", info.appVerCode)
        assertEquals("samsung SM-S928N", info.device)
        assertEquals("com.teamyg.parfait", info.appId)
    }

    @Test
    fun buildDeviceInfo_longManufacturerAndModel_truncatesDeviceToLimit() {
        // Given 제조사와 모델을 이으면 사용자 속성 값 상한을 넘는 기기
        val info = buildDeviceInfo(
            osVer = "14",
            appVer = "1.1.1",
            appVerCode = 8,
            manufacturer = "VeryLongManufacturerName",
            model = "VeryLongModelIdentifier-2026",
            appId = "com.teamyg.parfait",
        )

        // Then GA4 가 조용히 자르기 전에 우리가 자른다
        assertEquals(USER_PROPERTY_VALUE_MAX_LENGTH, info.device.length)
        assertEquals("VeryLongManufacturerName VeryLongMod", info.device)
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "com.teamyg.parfait.analytics.DeviceInfoTest"`
Expected: 컴파일 실패. `buildDeviceInfo`·`USER_PROPERTY_VALUE_MAX_LENGTH` 미해결.

- [ ] **Step 3: 속성 이름 상수를 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/AnalyticsUserProperty.kt`

```kotlin
package com.teamyg.parfait.analytics

object AnalyticsUserProperty {
    const val IS_DEBUG = "IS_DEBUG"
    const val OS_TYPE = "OS_TYPE"
    const val OS_VER = "OS_VER"
    const val APP_VER = "APP_VER"
    const val APP_VER_CODE = "APP_VER_CODE"
    const val DEVICE = "DEVICE"
    const val APP_ID = "APP_ID"
}
```

- [ ] **Step 4: 값 조립을 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/DeviceInfo.kt`

```kotlin
package com.teamyg.parfait.analytics

import android.os.Build
import com.teamyg.parfait.BuildConfig

/** GA4 사용자 속성 값의 상한 */
const val USER_PROPERTY_VALUE_MAX_LENGTH = 36

private const val OS_TYPE_ANDROID = "Android"

data class DeviceInfo(
    val osType: String,
    val osVer: String,
    val appVer: String,
    val appVerCode: String,
    val device: String,
    val appId: String,
)

/** `Build`·`BuildConfig` 를 읽지 않아 JVM 유닛으로 덮인다 */
fun buildDeviceInfo(
    osVer: String,
    appVer: String,
    appVerCode: Int,
    manufacturer: String,
    model: String,
    appId: String,
): DeviceInfo = DeviceInfo(
    osType = OS_TYPE_ANDROID,
    osVer = osVer,
    appVer = appVer,
    appVerCode = appVerCode.toString(),
    device = "$manufacturer $model".take(USER_PROPERTY_VALUE_MAX_LENGTH),
    appId = appId,
)

fun currentDeviceInfo(): DeviceInfo = buildDeviceInfo(
    osVer = Build.VERSION.RELEASE,
    appVer = BuildConfig.VERSION_NAME,
    appVerCode = BuildConfig.VERSION_CODE,
    manufacturer = Build.MANUFACTURER,
    model = Build.MODEL,
    appId = BuildConfig.APPLICATION_ID,
)
```

- [ ] **Step 5: 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:testDebugUnitTest --tests "com.teamyg.parfait.analytics.DeviceInfoTest"`
Expected: PASS (테스트 2건)

- [ ] **Step 6: 포맷을 확인한다**

Run: `./gradlew :app:ktlintCheck`
Expected: 통과. 실패하면 `./gradlew :app:ktlintFormat` 후 다시 확인한다.

- [ ] **Step 7: 커밋한다**

```bash
git add app/src/main/java/com/teamyg/parfait/analytics/AnalyticsUserProperty.kt app/src/main/java/com/teamyg/parfait/analytics/DeviceInfo.kt app/src/test/java/com/teamyg/parfait/analytics/DeviceInfoTest.kt
git commit -m "feat: GA 사용자 속성으로 실을 기기·앱 값을 조립한다" \
  -m "Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: Firebase 구현체와 빌드 설정

**Files:**
- Create: `app/src/main/java/com/teamyg/parfait/analytics/FirebaseAnalyticsLogger.kt`
- Create: `app/src/main/java/com/teamyg/parfait/analytics/di/AnalyticsModule.kt`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `AnalyticsLogger`, `AnalyticsScreen` (Task 1)
- Produces: `AnalyticsLogger` Hilt 바인딩, `BuildConfig.ANALYTICS_IS_DEBUG`

> ⚠️ **이 Task 에는 유닛 테스트가 없다.** `logEvent` 가 받는 `Bundle` 은 프레임워크 타입이라
> Robolectric 없이 JVM 유닛에서 내용을 볼 수 없고, Robolectric 을 새로 들이지 않기로 했다.
> 검증은 빌드 통과와 Task 6 의 실기기 확인이다.

- [ ] **Step 1: 구현체를 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/FirebaseAnalyticsLogger.kt`

```kotlin
package com.teamyg.parfait.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsLogger {
    override fun setCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun setUserProperty(
        name: String,
        value: String,
    ) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    // 파라미터 이름을 문자열로 쓰지 않는다. firebase_ 는 GA4 예약 접두사라 그 이름으로 실어
    // 보낸 값은 버려질 수 있다.
    override fun logScreenView(screen: AnalyticsScreen) {
        val params = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screen.screenId)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screen.screenClass)
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
    }
}
```

- [ ] **Step 2: Hilt 바인딩을 만든다**

`app/src/main/java/com/teamyg/parfait/analytics/di/AnalyticsModule.kt`

모듈을 둘로 가르는 이유는 이 저장소가 `@Binds` 를 `interface` 에, `@Provides` 를 `object` 에
나눠 두기 때문이다(`push/di/DeviceTokenModule`·`data/di/ClockModule`). 한 파일 안에 둘 다
두되 형태는 그 관례를 따른다. 이 설명을 코드 주석으로 남기지 않는다 — 다른 파일의 현재
상태를 단정하는 문장이라 낡는다.

```kotlin
package com.teamyg.parfait.analytics.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.teamyg.parfait.analytics.AnalyticsLogger
import com.teamyg.parfait.analytics.FirebaseAnalyticsLogger
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface AnalyticsModule {
    @Binds
    @Singleton
    fun bindAnalyticsLogger(firebaseAnalyticsLogger: FirebaseAnalyticsLogger): AnalyticsLogger
}

@Module
@InstallIn(SingletonComponent::class)
object FirebaseAnalyticsModule {
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context,
    ): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
}
```

- [ ] **Step 3: `ANALYTICS_IS_DEBUG` 를 더한다**

먼저 `app/build.gradle.kts` 최상단, `localProperties` 선언 바로 아래에 프로퍼티를 읽는 줄을
넣는다. `localProperties` 와 같은 자리에 두어 이 스크립트가 밖에서 받는 값을 한곳에서 읽게
한다.

```kotlin
// 기본값은 빌드 타입을 따르되 -Panalytics.isDebug 로 덮어쓴다. debug 빌드로도 운영과 같은
// 조건을 만들어 GA4 에서 확인하기 위한 것이다.
val analyticsIsDebugOverride: String? = providers.gradleProperty("analytics.isDebug").orNull
```

이어서 `android { }` 블록 안, `buildFeatures { }` 바로 앞에 아래를 넣는다. release·debug
정의 자체는 `build-logic`의 `AndroidConfig.kt#setConfigAndroidApplication`에 있으므로
여기서는 그 타입을 다시 열어 필드만 더한다.

```kotlin
    buildTypes {
        release {
            buildConfigField(
                "boolean",
                "ANALYTICS_IS_DEBUG",
                analyticsIsDebugOverride ?: "false",
            )
        }
        debug {
            buildConfigField(
                "boolean",
                "ANALYTICS_IS_DEBUG",
                analyticsIsDebugOverride ?: "true",
            )
        }
    }
```

- [ ] **Step 4: 자동 화면 보고를 끈다**

`app/src/main/AndroidManifest.xml`의 `<application>` 안, `<activity>` 선언 앞에 넣는다.

```xml
        <!--
            단일 Activity 라 자동 수집은 화면과 무관하게 MainActivity 이름으로 찍힌다.
            그 이벤트가 같은 보고서에 섞이면 화면별 수치를 흐린다.
        -->
        <meta-data
            android:name="google_analytics_automatic_screen_reporting_enabled"
            android:value="false" />
```

- [ ] **Step 5: 빌드가 통과하는지 확인한다**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 덮어쓰기가 먹는지 확인한다**

Run: `./gradlew :app:assembleDebug -Panalytics.isDebug=false`
Expected: BUILD SUCCESSFUL. 이어서 생성된 `BuildConfig`에 값이 반영됐는지 본다.

Run: `grep -r "ANALYTICS_IS_DEBUG" app/build/generated/source/buildConfig/debug/`
Expected: `public static final boolean ANALYTICS_IS_DEBUG = false;`

- [ ] **Step 7: 포맷을 확인한다**

Run: `./gradlew :app:ktlintCheck`
Expected: 통과. 실패하면 `./gradlew :app:ktlintFormat` 후 다시 확인한다.

- [ ] **Step 8: 커밋한다**

```bash
git add app/src/main/java/com/teamyg/parfait/analytics/FirebaseAnalyticsLogger.kt app/src/main/java/com/teamyg/parfait/analytics/di/AnalyticsModule.kt app/build.gradle.kts app/src/main/AndroidManifest.xml
git commit -m "feat: Firebase Analytics 구현체와 빌드 설정을 붙인다" \
  -m "Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: 배선

**Files:**
- Modify: `app/src/main/java/com/teamyg/parfait/BaseApplication.kt`
- Modify: `app/src/main/java/com/teamyg/parfait/MainActivity.kt`
- Modify: `app/src/main/java/com/teamyg/parfait/MainRoute.kt`

**Interfaces:**
- Consumes: `AnalyticsLogger`·`AnalyticsUserProperty`·`currentDeviceInfo()` (Task 1·3), `ScreenViewTracker` (Task 2), `BuildConfig.ANALYTICS_IS_DEBUG` (Task 4)
- Produces: 없음. 이 Task 가 마지막 소비처다.

- [ ] **Step 1: 수집을 켜고 사용자 속성을 설정한다**

`BaseApplication.kt`에 필드 주입과 호출을 더한다. `@HiltAndroidApp` 이라 필드 주입이 된다.

import 에 다음을 더한다.

```kotlin
import com.teamyg.parfait.analytics.AnalyticsLogger
import com.teamyg.parfait.analytics.AnalyticsUserProperty
import com.teamyg.parfait.analytics.currentDeviceInfo
import javax.inject.Inject
```

클래스 본문 맨 앞에 필드를 더한다.

```kotlin
    @Inject
    lateinit var analyticsLogger: AnalyticsLogger
```

`onCreate` 의 `createPushNotificationChannel()` 호출 뒤에 한 줄을 더한다.

```kotlin
        setUpAnalytics()
```

`createPushNotificationChannel()` 함수 아래에 다음을 더한다.

```kotlin
    // debug 와 release 가 같은 GA4 속성으로 들어간다(applicationIdSuffix 가 없다).
    // 둘을 가르는 것은 IS_DEBUG 뿐이다 — adr/0031-analytics-central-screen-mapping.md.
    private fun setUpAnalytics() {
        analyticsLogger.setCollectionEnabled(true)

        val deviceInfo = currentDeviceInfo()
        with(analyticsLogger) {
            setUserProperty(AnalyticsUserProperty.IS_DEBUG, BuildConfig.ANALYTICS_IS_DEBUG.toString())
            setUserProperty(AnalyticsUserProperty.OS_TYPE, deviceInfo.osType)
            setUserProperty(AnalyticsUserProperty.OS_VER, deviceInfo.osVer)
            setUserProperty(AnalyticsUserProperty.APP_VER, deviceInfo.appVer)
            setUserProperty(AnalyticsUserProperty.APP_VER_CODE, deviceInfo.appVerCode)
            setUserProperty(AnalyticsUserProperty.DEVICE, deviceInfo.device)
            setUserProperty(AnalyticsUserProperty.APP_ID, deviceInfo.appId)
        }
    }
```

- [ ] **Step 2: 트래커를 화면까지 나른다**

`MainActivity.kt`의 import 에 다음을 더한다.

```kotlin
import com.teamyg.parfait.analytics.ScreenViewTracker
```

다른 `@Inject` 필드 옆에 더한다.

```kotlin
    @Inject
    lateinit var screenViewTracker: ScreenViewTracker
```

`setContent` 안 `MainRoute(...)` 호출의 `hasActiveSession = hasActiveSession,` 다음 줄에
인자를 더한다.

```kotlin
                    screenViewTracker = screenViewTracker,
```

- [ ] **Step 3: 백스택을 관찰한다**

`MainRoute.kt`의 import 에 다음을 더한다.

```kotlin
import com.teamyg.parfait.analytics.ScreenViewTracker
```

`MainRoute` 시그니처의 `hasActiveSession: HasActiveSessionUseCase,` 다음 줄에 파라미터를
더한다.

```kotlin
    screenViewTracker: ScreenViewTracker,
```

딥링크 `LaunchedEffect` 블록 뒤, `SharedTransitionLayout` 앞에 수집기를 더한다.

```kotlin
    // 화면 진입도 앞의 둘과 같은 이유로 여기 한 곳에서만 본다 — 화면마다 보면 한 전환이 여러 번 찍힌다.
    // 크기를 함께 보는 이유와 중복 판정이 트래커에 있는 이유는
    // adr/0031-analytics-central-screen-mapping.md 에 있다.
    LaunchedEffect(navigator, screenViewTracker) {
        snapshotFlow { navigator.backStack.size to navigator.backStack.lastOrNull() }
            .collect { (size, top) -> screenViewTracker.track(backStackSize = size, top = top) }
    }
```

`snapshotFlow` 는 값이 이전과 다를 때만 방출하므로 `distinctUntilChanged()` 를 더하지
않는다. 컴포지션을 넘어가는 중복은 트래커가 맡는다.

- [ ] **Step 4: 빌드와 전체 유닛 테스트가 통과하는지 확인한다**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL. 기존 테스트가 하나도 깨지지 않는다.

- [ ] **Step 5: release 빌드가 통과하는지 확인한다**

R8 축소가 켜져 있어 Hilt·Firebase 배선이 여기서만 깨질 수 있다.

⚠️ **서명 키가 없으면 이 태스크는 건너뛰어지지 않고 실패한다.** `build-logic` 의
`AndroidConfig.kt#failWhenStoreFileMissing` 이 `validateSigningRelease` 에 `doFirst` 로
오류를 걸어 두었고 `assembleRelease` 가 그 태스크를 경유한다. `local.properties` 에
`YG_RELEASE_STORE_FILE` 이 있고 그 파일이 실재하는 환경에서만 이 단계를 돌린다. 키가 없어
실패하면 그것은 이 변경의 결함이 아니므로 그대로 보고하고 넘어간다. 같은 이유로
`:app:assembleDebug` 도 `YG_DEBUG_STORE_FILE` 이 없는 환경에서는 실패한다.

Run: `./gradlew :app:assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 포맷을 확인한다**

Run: `./gradlew :app:ktlintCheck`
Expected: 통과. 실패하면 `./gradlew :app:ktlintFormat` 후 다시 확인한다.

- [ ] **Step 7: 커밋한다**

```bash
git add app/src/main/java/com/teamyg/parfait/BaseApplication.kt app/src/main/java/com/teamyg/parfait/MainActivity.kt app/src/main/java/com/teamyg/parfait/MainRoute.kt
git commit -m "feat: 화면 진입 계측을 앱에 배선한다" \
  -m "Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: 실기기 확인과 문서 갱신

**Files:**
- Modify: `parfait/specs/2026-09-09-release-analytics-screen-tracking.md` (위키 저장소)
- Modify: `parfait/adr/0031-analytics-central-screen-mapping.md` (위키 저장소)

**Interfaces:**
- Consumes: Task 5 까지의 결과물
- Produces: 없음

> 이 Task 는 사람이 기기에서 하는 확인이다. 자동화하지 않는다. 확인 항목마다 결과를 적고,
> 어긋난 것이 있으면 고친 뒤 다시 본다.

> 🔁 **수집 정책이 구현 중 뒤집혀 이 절차가 바뀌었다.** 이제 debug 는 수집 자체를 끄므로
> 평범한 debug 빌드로는 DebugView 에 아무것도 안 뜬다. 반드시 게이트를 덮어써서 설치한다.

- [x] **Step 1: DebugView 를 켠다** — 완료(2026-09-09)

```bash
./gradlew :app:installDebug -Panalytics.isDebug=false
adb shell setprop debug.firebase.analytics.app com.teamyg.parfait
```

앱을 실행하고 Firebase 콘솔의 DebugView 에 기기가 잡히는지 본다.

- [x] **Step 2: 화면 진입 이벤트를 확인한다** — 완료(2026-09-09), 화면 전환이 실제로 도착한다

앱을 스플래시부터 그룹 목록·캔버스·카메라·확인 화면까지 진행하며 DebugView 에서 다음을
확인한다.

1. `screen_view` 이벤트가 화면마다 하나씩 온다.
2. `screen_name` 파라미터에 화면 ID(`A-001`·`G-001`·`C-001`·`C-101`·`C-101-confirm`)가
   들어 있다. **비어 있으면 파라미터 이름이 틀린 것이다.**
3. `screen_class` 파라미터에 `NavKey` 이름이 들어 있다.
4. `MainActivity` 이름의 자동 `screen_view` 가 오지 않는다.

- [ ] **Step 3: 중복과 복귀를 확인한다**

1. 캔버스에서 뒤로 가 그룹 목록으로 돌아오면 `G-001` 이 다시 온다.
2. 다크모드를 토글해 Activity 가 재생성돼도 `screen_view` 가 더 오지 않는다.
3. 앱을 백그라운드로 보냈다 돌아와도 `screen_view` 가 더 오지 않는다.

- [ ] **Step 4: 사용자 속성을 확인한다**

DebugView 의 사용자 속성에서 7종이 모두 보이고 값이 비어 있지 않은지 본다.

⚠️ `IS_DEBUG` 는 **언제나 `false`** 다. 수집이 켜진 빌드는 정의상 그 값이 `false` 인
빌드뿐이라, 게이트를 덮어써서 설치한 이 빌드도 `false` 로 온다.

- [x] **Step 5: 덮어쓰기를 확인한다** — 완료(2026-09-09)

Step 1 이 이미 덮어쓴 빌드를 설치했고, 이벤트가 도착한 것 자체가 게이트가 열렸다는 증거다.
덮어쓰기가 안 먹었다면 수집이 꺼져 DebugView 에 아무것도 안 왔을 것이다.

- [ ] **Step 6: 확인을 끄고 문서를 갱신한다**

Run: `adb shell setprop debug.firebase.analytics.app .none.`

위키 저장소에서 다음을 고친다.

1. 스펙의 `status` 를 `implemented` 로 올리고 `verified` 를 확인한 날짜로 바꾼다.
2. 스펙 주의 절의 "실기기에서 확인한 적이 0회다" 항목을 확인 결과로 바꾼다.
3. ADR 의 `status` 를 `accepted` 로 올리고 위험 절의 같은 항목을 바꾼다.
4. `parfait/specs/README.md`·`parfait/adr/README.md`·`parfait/plans/README.md` 의 해당
   행에 결과를 한 줄 더한다.
5. 어긋난 것이 있었다면 무엇이 왜 틀렸는지 스펙에 남긴다. 이것이 이 Task 의 진짜 산출물이다.

---

## 주의

- **Task 1 과 Task 2 사이에 컴파일이 깨지지 않는다.** 각 Task 는 독립적으로 빌드된다.
- **Task 5 전까지 앱 동작은 하나도 바뀌지 않는다.** Task 1~4 는 아무도 부르지 않는 코드를
  쌓는다. 그래서 Task 5 를 빼고 머지하면 계측이 조용히 없는 상태가 된다.
- **`:app` 유닛 테스트가 feature `:api` 클래스를 본다.** `:app` 이 그 모듈들을
  `implementation` 으로 갖고 있고 `testImplementation` 이 그것을 이어받는다.
- **매핑 누락은 컴파일러가 잡지 못한다.** 새 화면을 만들면 `NavKeyAnalyticsScreen.kt` 에
  줄을 더하고 `NavKeyAnalyticsScreenTest` 에 기대값을 더해야 한다.
