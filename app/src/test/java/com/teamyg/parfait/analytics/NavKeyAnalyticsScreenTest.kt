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
