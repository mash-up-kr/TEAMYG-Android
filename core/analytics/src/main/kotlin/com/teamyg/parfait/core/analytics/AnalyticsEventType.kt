package com.teamyg.parfait.core.analytics

/** [AnalyticsEvent.type] 상수. 값은 [사용자 이벤트 설계](../../../../../../../../../../docs/superpowers/specs/2026-10-06-user-events-design.md) 정본을 따른다 */
enum class AnalyticsEventType(
    val eventId: String,
    val eventName: String,
) {
    APP_PUSH_OPENED(eventId = "N-001", eventName = "app_push_opened"),

    /**
     * 외부 링크(App Links, 설치 후 Install Referrer 복원)로 앱이 열렸을 때. 파라미터 스키마는
     * [사용자 이벤트 설계](../../../../../../../../../../docs/superpowers/specs/2026-10-06-user-events-design.md)가 정본이다.
     */
    APP_LINK_OPENED(eventId = "AppLink", eventName = "AppLink"),
    APP_ACCOUNT_NICKNAME_CHANGED(eventId = "S-002-01", eventName = "app_account_nickname_changed"),

    CANVAS_CAMERA_RETAKE_TAPPED(eventId = "C-101-01", eventName = "canvas_camera_retake_tapped"),
    CANVAS_CAMERA_PHOTO_SELECTED(eventId = "C-102-01", eventName = "canvas_camera_photo_selected"),
    CANVAS_CAMERA_DETECTION_COMPLETED(eventId = "C-104-01", eventName = "canvas_camera_detection_completed"),
    CANVAS_EDIT_STACK_TAPPED(eventId = "C-106-01", eventName = "canvas_edit_stack_tapped"),
    CANVAS_TOPPING_EDITOR_CHECKED(eventId = "C-001-01", eventName = "canvas_topping_editor_checked"),
    CANVAS_CALENDAR_DATE_TAPPED(eventId = "C-001-02", eventName = "canvas_calendar_date_tapped"),
    CANVAS_CALENDAR_PAST_PARFAIT_TAPPED(eventId = "C-001-03", eventName = "canvas_calendar_past_parfait_tapped"),
    CANVAS_SAVE_GALLERY_SAVED(eventId = "C-001-04", eventName = "canvas_save_gallery_saved"),
    CANVAS_TOPPING_CLOSED_UNCHANGED(eventId = "C-305-01", eventName = "canvas_topping_closed_unchanged"),
    CANVAS_EDIT_BORDER_TAPPED(eventId = "C-305-02", eventName = "canvas_edit_border_tapped"),

    GROUP_LIST_CREATED(eventId = "G-001-01", eventName = "group_list_created"),
    GROUP_LIST_JOINED(eventId = "G-001-02", eventName = "group_list_joined"),
    GROUP_LIST_REFRESHED(eventId = "G-001-03", eventName = "group_list_refreshed"),
    GROUP_LIST_INVITE_CODE_PASTED(eventId = "A-004-01", eventName = "group_list_invite_code_pasted"),
    GROUP_MENU_INVITE_CODE_COPIED(eventId = "S-101-01", eventName = "group_menu_invite_code_copied"),
    GROUP_MENU_LEFT(eventId = "S-101-02", eventName = "group_menu_left"),
    GROUP_MENU_NICKNAME_CHANGED(eventId = "S-101-03", eventName = "group_menu_nickname_changed"),
}
