---
id: s105-notification-setting
title: S-105 알림 설정 화면 (Notification Setting)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-11-08
related_code:
  - NotificationSettingVO.kt#NotificationSettingVO
  - NotificationSettingRepository.kt#NotificationSettingRepository
  - NotificationSettingRepositoryImpl.kt#NotificationSettingRepositoryImpl
  - GetNotificationSettingUseCase.kt#GetNotificationSettingUseCase
  - UpdateCanvasNotificationUseCase.kt#UpdateCanvasNotificationUseCase
  - UpdateActivityNotificationUseCase.kt#UpdateActivityNotificationUseCase
  - NotificationSettingViewModel.kt#NotificationSettingViewModel
  - NotificationSettingScreen.kt#NotificationSettingScreen
  - NotificationSettingRoute.kt#NotificationSettingRoute
  - NavKeyNotificationSetting.kt#NavKeyNotificationSetting
  - NotificationPermissionManager.kt#areNotificationsEnabled
  - Context.kt#buildAppNotificationSettingsIntent
  - ListItemToggle.kt#ListItemToggle
  - NotificationSwitch.kt#NotificationSwitch
  - NotificationInfoCard.kt#NotificationInfoCard
  - AppSettingScreen.kt#AppSettingScreen
  - AppSettingViewModel.kt#handleClickNotification
related_adr:
related_spec: user-events-design
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, app-setting, notification]
---

# Spec: S-105 알림 설정 화면

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

> 이 문서는 **구현을 먼저 끝낸 뒤 사후 작성한 as-built 스펙**이다 — 브레인스토밍의 탐색·대안
> 비교 단계 없이, 이미 커밋된 결정을 기록만 한다.

## 목표

앱 설정(S-001)에서 진입하는 알림 설정 화면. 캔버스 알림·활동 알림 두 종류를 토글로
켜고 끌 수 있게 한다.

## 범위

- 포함: 토글 두 개(캔버스/활동) 조회·저장, 기기 알림 꺼짐 안내 및 기기 설정 이동,
  S-001에서의 진입 항목, 관련 분석 이벤트 배선.
- 제외: 서버 연동(아래 「주의 / 열린 질문」 참조) — 지금은 토글 값을 서버가 아니라
  메모리에만 둔다. 알림 권한(`POST_NOTIFICATIONS`) 요청 자체는 범위 밖(그룹
  생성·참여 직후 `NotificationPermissionGate`가 이미 담당 — [push-notification-permission-and-device-token](2026-09-05-push-notification-permission-and-device-token.md)).

## API / 인터페이스

```kotlin
data class NotificationSettingVO(
    val isCanvasNotificationEnabled: Boolean,
    val isActivityNotificationEnabled: Boolean,
)

interface NotificationSettingRepository {
    suspend fun getNotificationSetting(): Result<NotificationSettingVO>
    suspend fun updateCanvasNotification(enabled: Boolean): Result<Unit>
    suspend fun updateActivityNotification(enabled: Boolean): Result<Unit>
}
```

- `GetNotificationSettingUseCase` / `UpdateCanvasNotificationUseCase` /
  `UpdateActivityNotificationUseCase` — 각자 리포지토리 메서드 하나를 그대로 감싼다.
- `NotificationSettingRepositoryImpl`은 서버 대신 `MutableStateFlow`로 상태를 들고
  있는 **임시 구현**이다. 초기값은 캔버스 `true`, 활동 `false`(mock).

## 동작 / 상태

`NotificationSettingState`:

| 필드 | 의미 |
|---|---|
| `isCanvasNotificationChecked` / `isActivityNotificationChecked` | 토글 체크 상태 |
| `isSettingLoaded` | 서버(현재는 mock) 설정을 한 번이라도 받아왔는지 |
| `isDeviceNotificationEnabled` | 기기 알림 on/off. `null`은 아직 안 읽음 |
| `isRefreshing` | 당겨서 새로고침(`Refresh` 인텐트)으로 설정을 다시 받는 중인지 |
| `isDeviceNotificationOff` (계산) | `isDeviceNotificationEnabled == false` |
| `isToggleEnabled` (계산) | `isSettingLoaded && isDeviceNotificationEnabled == true` — 설정을 못 받았거나 기기 알림이 꺼져 있으면 토글 자체를 조작할 수 없다 |

기기 알림 on/off는 `NotificationPermissionManager.areNotificationsEnabled(context)`로
읽는다. 화면이 복귀할 때마다(`LifecycleResumeEffect`) 다시 읽어 `UpdateDeviceNotificationEnabled`
인텐트로 반영하고, 당겨서 새로고침 때도 다시 읽는다.

당겨서 새로고침(`PullToRefreshBox`, 디자인 미정이라 Material3 기본 인디케이터 — TODO)은 기기 알림
값과 서버 설정을 함께 다시 읽는다. 저장 대기 중인 스위치가 하나라도 있으면 받아 온 값을 반영하지
않는다. 조회 실패는 `ShowLoadError` 토스트다.

토글 저장은 **optimistic update**다 — 누르면 화면에 바로 반영하고, 저장 요청은
스위치별로 `ToggleSync`(mutex + pending 카운터)로 직렬화한다. 실패하면 **뒤에 대기
중인 요청이 없을 때만** 마지막 confirmed 값으로 되돌리고 저장 실패 토스트를 띄운다
(대기 중인 요청이 있으면 그 결과가 최종값을 정하므로 되돌리지 않는다 — 사용자가
방금 누른 값을 덮어쓰지 않으려는 의도).

## 표시·제어 규칙

- 기기 알림이 꺼져 있으면(`isDeviceNotificationOff`) 토글 목록 위에 안내 카드
  (`NotificationInfoCard`)를 보여주고 "설정으로 이동" 버튼을 둔다. 버튼은
  `ACTION_APP_NOTIFICATION_SETTINGS`로 앱 알림 설정을 열고, 그 화면을 못 여는 기기면
  앱 정보 화면(`ACTION_APPLICATION_DETAILS_SETTINGS`)으로 폴백한다.
- 토글 자체는 `isToggleEnabled`가 `false`면 비활성(`ListItemToggle enabled = false`).
- S-001(`AppSettingScreen`)에 "알림 설정" 항목이 추가돼 탭하면
  `app_setting_notification_tapped` 이벤트를 찍고 `NavKeyNotificationSetting`으로
  이동한다.

## 파일 구성

- `domain/model/notification/NotificationSettingVO.kt`
- `domain/repository/notification/NotificationSettingRepository.kt`
- `domain/usecase/notification/{Get,UpdateCanvas,UpdateActivity}NotificationUseCase.kt` (각 1파일)
- `data/repository/notification/NotificationSettingRepositoryImpl.kt`
- `core/util/android/permission/NotificationPermissionManager.kt#areNotificationsEnabled`
- `core/util/android/extension/Context.kt#buildAppNotificationSettingsIntent`
- `feature/app/setting/api/NavKeyNotificationSetting.kt`
- `feature/app/setting/impl/viewmodel/NotificationSettingViewModel.kt`
- `feature/app/setting/impl/screen/NotificationSettingScreen.kt` + `component/{ListItemToggle,NotificationSwitch,NotificationInfoCard}.kt`
- `feature/app/setting/impl/route/NotificationSettingRoute.kt`
- `feature/app/setting/impl/navigation/EntryBuilder.kt`에 `entry<NavKeyNotificationSetting>` 등록

분석 이벤트(`app_setting_notification_tapped` · `app_notification_setting_changed` ·
`app_notification_device_settings_tapped`)는 [user-events-design](../2026-10-06-user-events-design.md)이
정본이라 이 문서에서 값을 반복하지 않는다.

## 주의 / 열린 질문

- **서버 계약이 없다.** `NotificationSettingRepositoryImpl`이 KDoc으로 스스로 "서버
  연동 전 임시 구현, 서버 API가 정해지면 원격 데이터소스 호출로 교체"라고 밝히는
  stub이다 — 설정이 앱 재시작마다 mock 초기값(캔버스 on·활동 off)으로 돌아간다.
  서버 쪽 계약이 생기면 [api/notification.md](../../../api/notification.md)에 이 화면의
  설정 저장 절을 추가하고 이 리포지토리를 실제 호출로 교체해야 한다.
- 화면·ViewModel 계측 테스트는 없다 — ViewModel은 `NotificationSettingViewModelTest`·
  `NotificationSettingViewModelInitTest` 유닛, 인텐트 빌더만 `ContextExtensionTest` androidTest다.
- 서버 계약·토글과 푸시 종류의 대응은 OQ-P-343 ③이 추적한다.
