package com.teamyg.parfait.core.analytics

/**
 * [AnalyticsScreen][com.teamyg.parfait.core.analytics.AnalyticsScreen]을 만드는
 * `screenId`/`screenClass` 상수.
 *
 * @property screenClass `NavKey` 이름. 리플렉션이 아니라 상수다 —
 *   release 는 R8 난독화가 켜져 있어 `simpleName` 이 뭉개진다
 */
enum class AnalyticsScreenType(
    val screenId: String,
    val screenClass: String,
) {
    SPLASH(screenId = "A-001", screenClass = "NavKeySplash"),
    LOGIN(screenId = "A-002", screenClass = "NavKeyLogin"),
    TERM_AGREE(screenId = "A-003", screenClass = "NavKeyTermAgree"),
    GROUP_INVITE_CODE(screenId = "A-004", screenClass = "NavKeyGroupInviteCode"),
    GROUP_NICK_NAME(screenId = "A-004-naming", screenClass = "NavKeyGroupNickName"),
    GROUP_CREATE(screenId = "A-005", screenClass = "NavKeyGroupCreate"),
    GROUP_LIST(screenId = "G-001", screenClass = "NavKeyGroupList"),
    CANVAS_MAIN(screenId = "C-001", screenClass = "NavKeyCanvasMain"),
    CAMERA_CUSTOM(screenId = "C-101", screenClass = "NavKeyCameraCustom"),
    CAMERA_CUSTOM_RETURN_RESULT(screenId = "C-302", screenClass = "NavKeyCameraCustom"),
    CAMERA_SYSTEM(screenId = "C-101-system", screenClass = "NavKeyCameraSystem"),
    PICTURE_CONFIRM_CAMERA(screenId = "C-101-confirm", screenClass = "NavKeyPictureConfirm"),
    PICTURE_CONFIRM_CAMERA_RETURN_RESULT(screenId = "C-302-confirm", screenClass = "NavKeyPictureConfirm"),
    PICTURE_CONFIRM_GALLERY(screenId = "C-102-confirm", screenClass = "NavKeyPictureConfirm"),
    PICTURE_CONFIRM_GALLERY_RETURN_RESULT(screenId = "C-303-confirm", screenClass = "NavKeyPictureConfirm"),
    GALLERY_CUSTOM(screenId = "C-102", screenClass = "NavKeyCustomGalleryPicker"),
    GALLERY_CUSTOM_RETURN_RESULT(screenId = "C-303", screenClass = "NavKeyCustomGalleryPicker"),
    GALLERY_SYSTEM(screenId = "C-102-system", screenClass = "NavKeySystemGalleryPicker"),
    SEGMENTATION(screenId = "C-103", screenClass = "NavKeySegmentation"),
    TOPPING_EDIT(screenId = "C-104", screenClass = "NavKeyToppingEdit"),
    CANVAS_TOPPING_PLACE(screenId = "C-106", screenClass = "NavKeyCanvasToppingPlace"),
    CANVAS_TEXT_INPUT(screenId = "C-106", screenClass = "NavKeyCanvasTextInput"),
    CANVAS_BG_EDIT(screenId = "C-301", screenClass = "NavKeyCanvasBGEdit"),
    CANVAS_TOPPING_ARRANGE(screenId = "C-305", screenClass = "NavKeyCanvasToppingArrange"),
    CANVAS_IMAGE_SAVE(screenId = "C-001-image-save", screenClass = "NavKeyCanvasImageSave"),
    APP_SETTING(screenId = "S-001", screenClass = "NavKeyAppSetting"),
    ACCOUNT_INFO(screenId = "S-002", screenClass = "NavKeyAccountInfo"),
    GROUP_SETTING(screenId = "S-101", screenClass = "NavKeyGroupSetting"),
    WEB_VIEW(screenId = "S-004", screenClass = "NavKeyWebView"),
}
