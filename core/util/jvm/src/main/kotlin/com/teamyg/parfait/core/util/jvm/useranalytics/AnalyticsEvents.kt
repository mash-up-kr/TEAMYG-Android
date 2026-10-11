package com.teamyg.parfait.core.util.jvm.useranalytics

/**
 * [AnalyticsEvent.eventId] 상수. [AnalyticsEventName]과 같은 이름을 쓴다 —
 * `AnalyticsEventId.X`/`AnalyticsEventName.X`가 한 이벤트의 두 반쪽이라는 걸
 * 이름만 보고 짝지을 수 있게.
 */
object AnalyticsEventId {
    const val APP_PUSH_OPENED = "N-001"
    const val APP_ACCOUNT_NICKNAME_CHANGED = "S-002-01"

    const val CANVAS_CAMERA_RETAKE_TAPPED = "C-101-01"
    const val CANVAS_CAMERA_PHOTO_SELECTED = "C-102-01"
    const val CANVAS_CAMERA_DETECTION_COMPLETED = "C-104-01"
    const val CANVAS_EDIT_STACK_TAPPED = "C-106-01"
    const val CANVAS_TOPPING_EDITOR_CHECKED = "C-001-01"
    const val CANVAS_CALENDAR_DATE_TAPPED = "C-001-02"
    const val CANVAS_CALENDAR_PAST_PARFAIT_TAPPED = "C-001-03"
    const val CANVAS_SAVE_GALLERY_SAVED = "C-001-04"
    const val CANVAS_TOPPING_CLOSED_UNCHANGED = "C-305-01"
    const val CANVAS_EDIT_BORDER_TAPPED = "C-305-02"

    const val GROUP_LIST_CREATED = "G-001-01"
    const val GROUP_LIST_JOINED = "G-001-02"
    const val GROUP_LIST_REFRESHED = "G-001-03"
    const val GROUP_LIST_INVITE_CODE_PASTED = "A-004-01"
    const val GROUP_MENU_INVITE_CODE_COPIED = "S-101-01"
    const val GROUP_MENU_LEFT = "S-101-02"
    const val GROUP_MENU_NICKNAME_CHANGED = "S-101-03"
}

/** [AnalyticsEvent.eventName] 상수. 값은 [사용자 이벤트 설계](../../../../../../../../docs/superpowers/specs/2026-10-06-user-events-design.md) 정본을 따른다 */
object AnalyticsEventName {
    const val APP_PUSH_OPENED = "app_push_opened"
    const val APP_ACCOUNT_NICKNAME_CHANGED = "app_account_nickname_changed"

    const val CANVAS_CAMERA_RETAKE_TAPPED = "canvas_camera_retake_tapped"
    const val CANVAS_CAMERA_PHOTO_SELECTED = "canvas_camera_photo_selected"
    const val CANVAS_CAMERA_DETECTION_COMPLETED = "canvas_camera_detection_completed"
    const val CANVAS_EDIT_STACK_TAPPED = "canvas_edit_stack_tapped"
    const val CANVAS_TOPPING_EDITOR_CHECKED = "canvas_topping_editor_checked"
    const val CANVAS_CALENDAR_DATE_TAPPED = "canvas_calendar_date_tapped"
    const val CANVAS_CALENDAR_PAST_PARFAIT_TAPPED = "canvas_calendar_past_parfait_tapped"
    const val CANVAS_SAVE_GALLERY_SAVED = "canvas_save_gallery_saved"
    const val CANVAS_TOPPING_CLOSED_UNCHANGED = "canvas_topping_closed_unchanged"
    const val CANVAS_EDIT_BORDER_TAPPED = "canvas_edit_border_tapped"

    const val GROUP_LIST_CREATED = "group_list_created"
    const val GROUP_LIST_JOINED = "group_list_joined"
    const val GROUP_LIST_REFRESHED = "group_list_refreshed"
    const val GROUP_LIST_INVITE_CODE_PASTED = "group_list_invite_code_pasted"
    const val GROUP_MENU_INVITE_CODE_COPIED = "group_menu_invite_code_copied"
    const val GROUP_MENU_LEFT = "group_menu_left"
    const val GROUP_MENU_NICKNAME_CHANGED = "group_menu_nickname_changed"
}
