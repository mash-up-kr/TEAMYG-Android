package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.util.jvm.useranalytics.AnalyticsScreen
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
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingArrange
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
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit

/**
 * 각 값의 근거는 `parfait/specs/2026-09-09-release-analytics-screen-tracking.md` 매핑표에 있다.
 *
 * `NavKey` 는 sealed 가 아니라 `when` 이 빠짐없음을 강제하지 못한다. 빠뜨린 화면의 처리는
 * [ScreenViewTracker] 에 있다.
 */
fun NavKey.toAnalyticsScreenOrNull(): AnalyticsScreen? = when (this) {
    is NavKeySplash -> AnalyticsScreenType.SPLASH.toAnalyticsScreen()

    is NavKeyLogin -> AnalyticsScreenType.LOGIN.toAnalyticsScreen()

    is NavKeyTermAgree -> AnalyticsScreenType.TERM_AGREE.toAnalyticsScreen()

    is NavKeyGroupInviteCode -> AnalyticsScreenType.GROUP_INVITE_CODE.toAnalyticsScreen()

    is NavKeyGroupNickName -> AnalyticsScreenType.GROUP_NICK_NAME.toAnalyticsScreen()

    is NavKeyGroupCreate -> AnalyticsScreenType.GROUP_CREATE.toAnalyticsScreen()

    is NavKeyGroupList -> AnalyticsScreenType.GROUP_LIST.toAnalyticsScreen()

    is NavKeyCanvasMain -> AnalyticsScreenType.CANVAS_MAIN.toAnalyticsScreen()

    is NavKeyCameraCustom -> if (returnResultOnly) {
        AnalyticsScreenType.CAMERA_CUSTOM_RETURN_RESULT
    } else {
        AnalyticsScreenType.CAMERA_CUSTOM
    }.toAnalyticsScreen()

    is NavKeyCameraSystem -> AnalyticsScreenType.CAMERA_SYSTEM.toAnalyticsScreen()

    is NavKeyPictureConfirm -> when (source) {
        PictureConfirmSource.CAMERA ->
            if (returnResultOnly) {
                AnalyticsScreenType.PICTURE_CONFIRM_CAMERA_RETURN_RESULT
            } else {
                AnalyticsScreenType.PICTURE_CONFIRM_CAMERA
            }

        PictureConfirmSource.GALLERY ->
            if (returnResultOnly) {
                AnalyticsScreenType.PICTURE_CONFIRM_GALLERY_RETURN_RESULT
            } else {
                AnalyticsScreenType.PICTURE_CONFIRM_GALLERY
            }
    }.toAnalyticsScreen()

    is NavKeyCustomGalleryPicker -> if (returnResultOnly) {
        AnalyticsScreenType.GALLERY_CUSTOM_RETURN_RESULT
    } else {
        AnalyticsScreenType.GALLERY_CUSTOM
    }.toAnalyticsScreen()

    is NavKeySystemGalleryPicker -> AnalyticsScreenType.GALLERY_SYSTEM.toAnalyticsScreen()

    is NavKeySegmentation -> AnalyticsScreenType.SEGMENTATION.toAnalyticsScreen()

    is NavKeyToppingEdit -> AnalyticsScreenType.TOPPING_EDIT.toAnalyticsScreen()

    is NavKeyCanvasToppingPlace -> AnalyticsScreenType.CANVAS_TOPPING_PLACE.toAnalyticsScreen()

    is NavKeyCanvasBGEdit -> AnalyticsScreenType.CANVAS_BG_EDIT.toAnalyticsScreen()

    is NavKeyCanvasToppingArrange -> AnalyticsScreenType.CANVAS_TOPPING_ARRANGE.toAnalyticsScreen()

    is NavKeyCanvasImageSave -> AnalyticsScreenType.CANVAS_IMAGE_SAVE.toAnalyticsScreen()

    is NavKeyAppSetting -> AnalyticsScreenType.APP_SETTING.toAnalyticsScreen()

    is NavKeyAccountInfo -> AnalyticsScreenType.ACCOUNT_INFO.toAnalyticsScreen()

    is NavKeyGroupSetting -> AnalyticsScreenType.GROUP_SETTING.toAnalyticsScreen()

    is NavKeyWebView -> AnalyticsScreenType.WEB_VIEW.toAnalyticsScreen()

    else -> null
}

private fun AnalyticsScreenType.toAnalyticsScreen() = AnalyticsScreen(screenId, screenClass)
