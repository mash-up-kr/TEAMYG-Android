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
 * 각 값의 근거는 `parfait/specs/2026-09-09-release-analytics-screen-tracking.md` 매핑표에 있다.
 *
 * `NavKey` 는 sealed 가 아니라 `when` 이 빠짐없음을 강제하지 못한다. 빠뜨린 화면의 처리는
 * [ScreenViewTracker] 에 있다.
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
