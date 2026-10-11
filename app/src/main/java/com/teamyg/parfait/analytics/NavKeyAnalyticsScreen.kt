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
    is NavKeySplash -> AnalyticsScreen(AnalyticsScreenId.SPLASH, AnalyticsScreenClass.SPLASH)

    is NavKeyLogin -> AnalyticsScreen(AnalyticsScreenId.LOGIN, AnalyticsScreenClass.LOGIN)

    is NavKeyTermAgree -> AnalyticsScreen(AnalyticsScreenId.TERM_AGREE, AnalyticsScreenClass.TERM_AGREE)

    is NavKeyGroupInviteCode -> AnalyticsScreen(AnalyticsScreenId.GROUP_INVITE_CODE, AnalyticsScreenClass.GROUP_INVITE_CODE)

    is NavKeyGroupNickName -> AnalyticsScreen(AnalyticsScreenId.GROUP_NICK_NAME, AnalyticsScreenClass.GROUP_NICK_NAME)

    is NavKeyGroupCreate -> AnalyticsScreen(AnalyticsScreenId.GROUP_CREATE, AnalyticsScreenClass.GROUP_CREATE)

    is NavKeyGroupList -> AnalyticsScreen(AnalyticsScreenId.GROUP_LIST, AnalyticsScreenClass.GROUP_LIST)

    is NavKeyCanvasMain -> AnalyticsScreen(AnalyticsScreenId.CANVAS_MAIN, AnalyticsScreenClass.CANVAS_MAIN)

    is NavKeyCameraCustom -> AnalyticsScreen(
        screenId = if (returnResultOnly) AnalyticsScreenId.CAMERA_CUSTOM_RETURN_RESULT else AnalyticsScreenId.CAMERA_CUSTOM,
        screenClass = AnalyticsScreenClass.CAMERA_CUSTOM,
    )

    is NavKeyCameraSystem -> AnalyticsScreen(AnalyticsScreenId.CAMERA_SYSTEM, AnalyticsScreenClass.CAMERA_SYSTEM)

    is NavKeyPictureConfirm -> AnalyticsScreen(
        screenId = when (source) {
            PictureConfirmSource.CAMERA ->
                if (returnResultOnly) AnalyticsScreenId.PICTURE_CONFIRM_CAMERA_RETURN_RESULT else AnalyticsScreenId.PICTURE_CONFIRM_CAMERA
            PictureConfirmSource.GALLERY ->
                if (returnResultOnly) AnalyticsScreenId.PICTURE_CONFIRM_GALLERY_RETURN_RESULT else AnalyticsScreenId.PICTURE_CONFIRM_GALLERY
        },
        screenClass = AnalyticsScreenClass.PICTURE_CONFIRM,
    )

    is NavKeyCustomGalleryPicker -> AnalyticsScreen(
        screenId = if (returnResultOnly) AnalyticsScreenId.GALLERY_CUSTOM_RETURN_RESULT else AnalyticsScreenId.GALLERY_CUSTOM,
        screenClass = AnalyticsScreenClass.GALLERY_CUSTOM,
    )

    is NavKeySystemGalleryPicker -> AnalyticsScreen(AnalyticsScreenId.GALLERY_SYSTEM, AnalyticsScreenClass.GALLERY_SYSTEM)

    is NavKeySegmentation -> AnalyticsScreen(AnalyticsScreenId.SEGMENTATION, AnalyticsScreenClass.SEGMENTATION)

    is NavKeyToppingEdit -> AnalyticsScreen(AnalyticsScreenId.TOPPING_EDIT, AnalyticsScreenClass.TOPPING_EDIT)

    is NavKeyCanvasToppingPlace -> AnalyticsScreen(AnalyticsScreenId.CANVAS_TOPPING_PLACE, AnalyticsScreenClass.CANVAS_TOPPING_PLACE)

    is NavKeyCanvasBGEdit -> AnalyticsScreen(AnalyticsScreenId.CANVAS_BG_EDIT, AnalyticsScreenClass.CANVAS_BG_EDIT)

    is NavKeyCanvasToppingArrange -> AnalyticsScreen(AnalyticsScreenId.CANVAS_TOPPING_ARRANGE, AnalyticsScreenClass.CANVAS_TOPPING_ARRANGE)

    is NavKeyCanvasImageSave -> AnalyticsScreen(AnalyticsScreenId.CANVAS_IMAGE_SAVE, AnalyticsScreenClass.CANVAS_IMAGE_SAVE)

    is NavKeyAppSetting -> AnalyticsScreen(AnalyticsScreenId.APP_SETTING, AnalyticsScreenClass.APP_SETTING)

    is NavKeyAccountInfo -> AnalyticsScreen(AnalyticsScreenId.ACCOUNT_INFO, AnalyticsScreenClass.ACCOUNT_INFO)

    is NavKeyGroupSetting -> AnalyticsScreen(AnalyticsScreenId.GROUP_SETTING, AnalyticsScreenClass.GROUP_SETTING)

    is NavKeyWebView -> AnalyticsScreen(AnalyticsScreenId.WEB_VIEW, AnalyticsScreenClass.WEB_VIEW)

    else -> null
}
