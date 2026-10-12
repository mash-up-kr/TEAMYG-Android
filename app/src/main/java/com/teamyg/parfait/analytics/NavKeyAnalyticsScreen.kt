package com.teamyg.parfait.analytics

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.analytics.AnalyticsScreen
import com.teamyg.parfait.core.analytics.AnalyticsScreenType
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
    is NavKeySplash -> AnalyticsScreen(AnalyticsScreenType.SPLASH)

    is NavKeyLogin -> AnalyticsScreen(AnalyticsScreenType.LOGIN)

    is NavKeyTermAgree -> AnalyticsScreen(AnalyticsScreenType.TERM_AGREE)

    is NavKeyGroupInviteCode -> AnalyticsScreen(AnalyticsScreenType.GROUP_INVITE_CODE)

    is NavKeyGroupNickName -> AnalyticsScreen(AnalyticsScreenType.GROUP_NICK_NAME)

    is NavKeyGroupCreate -> AnalyticsScreen(AnalyticsScreenType.GROUP_CREATE)

    is NavKeyGroupList -> AnalyticsScreen(AnalyticsScreenType.GROUP_LIST)

    is NavKeyCanvasMain -> AnalyticsScreen(AnalyticsScreenType.CANVAS_MAIN)

    is NavKeyCameraCustom -> AnalyticsScreen(
        if (returnResultOnly) {
            AnalyticsScreenType.CAMERA_CUSTOM_RETURN_RESULT
        } else {
            AnalyticsScreenType.CAMERA_CUSTOM
        },
    )

    is NavKeyCameraSystem -> AnalyticsScreen(AnalyticsScreenType.CAMERA_SYSTEM)

    is NavKeyPictureConfirm -> AnalyticsScreen(
        when (source) {
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
        },
    )

    is NavKeyCustomGalleryPicker -> AnalyticsScreen(
        if (returnResultOnly) {
            AnalyticsScreenType.GALLERY_CUSTOM_RETURN_RESULT
        } else {
            AnalyticsScreenType.GALLERY_CUSTOM
        },
    )

    is NavKeySystemGalleryPicker -> AnalyticsScreen(AnalyticsScreenType.GALLERY_SYSTEM)

    is NavKeySegmentation -> AnalyticsScreen(AnalyticsScreenType.SEGMENTATION)

    is NavKeyToppingEdit -> AnalyticsScreen(AnalyticsScreenType.TOPPING_EDIT)

    is NavKeyCanvasToppingPlace -> AnalyticsScreen(AnalyticsScreenType.CANVAS_TOPPING_PLACE)

    is NavKeyCanvasBGEdit -> AnalyticsScreen(AnalyticsScreenType.CANVAS_BG_EDIT)

    is NavKeyCanvasToppingArrange -> AnalyticsScreen(AnalyticsScreenType.CANVAS_TOPPING_ARRANGE)

    is NavKeyCanvasImageSave -> AnalyticsScreen(AnalyticsScreenType.CANVAS_IMAGE_SAVE)

    is NavKeyAppSetting -> AnalyticsScreen(AnalyticsScreenType.APP_SETTING)

    is NavKeyAccountInfo -> AnalyticsScreen(AnalyticsScreenType.ACCOUNT_INFO)

    is NavKeyGroupSetting -> AnalyticsScreen(AnalyticsScreenType.GROUP_SETTING)

    is NavKeyWebView -> AnalyticsScreen(AnalyticsScreenType.WEB_VIEW)

    else -> null
}
