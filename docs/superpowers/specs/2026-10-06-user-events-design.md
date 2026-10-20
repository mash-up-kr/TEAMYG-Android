---
id: user-events
title: 사용자 이벤트 설계 (User Events)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-10-20
related_code:
  - AnalyticsEventType.kt#AnalyticsEventType
  - AnalyticsLogger.kt#AnalyticsLogger
  - FirebaseAnalyticsLogger.kt#logEvent
  - AppLinkOpenedEvent.kt#toOpenedEvent
related_adr: ADR-0031, ADR-0013
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, analytics]
---

# 사용자 이벤트 설계

## 목적

사용자 행동을 파악하고, 추후 개선점을 찾기 위한 이벤트를 수집한다.
이 문서는 **액션 이벤트**만 다룬다. 화면 이벤트(`screen_view`)는 별도로 정의한다.

## 결정 사항

| 항목 | 결정 |
|------|------|
| 수집 도구 | Firebase Analytics |
| 이름 규칙 | `snake_case`, `<기능>_<화면>_<동작>` |
| 모듈 위치 | `core:analytics`(계약과 Firebase 구현체를 한 모듈에 둔다 — ADR-0031) |

## 네이밍 규칙

- 이벤트명: `<기능>_<화면>_<동작>`, 동작은 과거형 (`_tapped`, `_copied`, `_changed`)
- 앞부분으로 묶어서 비교한다: `canvas_` → 캔버스 전체, `canvas_topping_` → 토핑 편집 화면만
- `<기능>`·`<화면>` 은 아래 표에 정한 값만 쓴다 (`topping` / `toppings` 처럼 갈리면 묶이지 않음)
- 화면 정보는 파라미터로 보내지 않는다 → `screen_view` 를 찍으면 이후 이벤트에 현재 화면이 자동으로 붙음
- 값에 PII(이름·이메일·전화번호·닉네임 원문) 금지

### 기능 · 화면 단위

| 기능 | 화면 | 범위 |
|------|------|------|
| `app` | `push` | 푸시 알림 |
| `app` | `account` | 앱 사이드메뉴 > 계정정보 |
| `canvas` | `camera` | 촬영·사진 선택·대상 감지 (C101 · C102 · C104) |
| `canvas` | `edit` | 캔버스 편집 — `C-106`(최초 배치) / `C-305`(재편집)[^oq430] |
| `canvas` | `topping` | 토핑 편집 |
| `canvas` | `calendar` | 달력 (SY001) |
| `canvas` | `save` | 저장 미리보기 |
| `group` | `list` | 그룹 목록 |
| `group` | `menu` | 그룹 사이드메뉴 |

### Firebase 제약 ⚠️

- 이벤트명·파라미터명 40자 이하, 영문/숫자/`_`, 영문으로 시작
- `firebase_`, `google_`, `ga_` 접두사 사용 금지
- 파라미터 값은 String(100자 이하)·Int·Double만 → **Bool 은 `"true"`/`"false"` 문자열로 보낸다**
- 파라미터를 리포트에서 보려면 맞춤 측정기준 등록 필요 (등록 이전 데이터는 소급 안 됨)
- 이벤트당 파라미터 25개, 앱 전체 고유 이벤트 500개까지

## 공통

- 앱 버전·OS 버전·기기: Firebase 자동 수집 → 따로 보내지 않음

## 이벤트 목록

Firebase 이벤트는 `name` + `parameters` 로 구성된다.

- parameters 가 비어 있으면 `parameters: nil` 로 보낸다
  - Android: `FirebaseAnalyticsLogger.logEvent`가 모든 이벤트에 `event_id`(`AnalyticsEventType.eventId`)를 붙여 파라미터가 비지 않는다.
- 값 표기: `"a"` / `"b없"` = 그중 하나, 모두 String

### app

| name | parameters | 시점 |
|------|------------|------|
| `app_push_opened` | `type`: `"TOPPING"` / `"REMIND_AM"` / `"REMIND_PM"` | 푸시 알림으로 앱 진입 |
| `AppLink` | `destination`: `"group_invite"` | 외부 링크(App Links, 설치 후 Install Referrer 복원)로 앱 진입. ⚠️ snake_case 규칙의 예외 — iOS 와 같은 로그로 합쳐 보려고 iOS 이름·값을 그대로 쓴다 |
| `app_account_nickname_changed` | — | 계정정보에서 닉네임 변경 완료 |

### canvas

| name | parameters | 시점 |
|------|------------|------|
| `canvas_camera_retake_tapped` | — | C101 다시 찍기 탭 |
| `canvas_camera_photo_selected` | `source`: `"recent"` / `"today"` | C102 최근 사진 / 오늘 사진 탭 |
| `canvas_camera_detection_completed` | `success`: `"true"` / `"false"` | C104 대상 감지 끝남 |
| `canvas_edit_border_tapped` | — | C-305 테두리 패널이 닫힌 상태에서 열릴 때만(닫는 토글은 세지 않음) |
| `canvas_edit_stack_tapped` | `group_id`: 그룹 ID, `is_border_on`: `"true"` / `"false"` | `C-106`(최초 배치)[^oq430] 캔버스 쌓기 탭 |
| `canvas_topping_closed_unchanged` | `action`: `"done"` / `"close"` | `C-305`(재편집)[^oq430] 토핑 편집에서 수정 없이 완료 / X. ⚠️ Android는 `CanvasToppingArrangeViewModel`의 무변경 닫기(`close`)와 `handleOnClickConfirm` 무변경 확정(`done`) 두 곳에서만 찍는다 — 그만두기 팝업 확인·시스템 뒤로가기는 찍지 않는다 |
| `canvas_topping_editor_checked` | `group_id`: 그룹 ID, `is_mine`: `"true"` / `"false"` | 토핑 편집자 확인 (내 토핑 / 다른 사람 토핑) |
| `canvas_calendar_date_tapped` | `group_id`: 그룹 ID | 달력 날짜 탭 |
| `canvas_calendar_past_parfait_tapped` | `group_id`: 그룹 ID | SY001 지난 날짜의 완성된 파르페 보러가기 탭 |
| `canvas_save_gallery_saved` | `format`: `"image"` / `"video"` | 갤러리 저장 완료. ⚠️ `"video"`는 아직 보내지 않는다 → [OQ-P-437](../../synthesis/open-questions.md#2026-10-20-캔버스-영상-저장이-canvas_save_gallery_saved를-남기지-않는다) |

### group

| name | parameters | 시점 |
|------|------------|------|
| `group_list_created` | — | 그룹 만들기 완료 |
| `group_list_joined` | — | 그룹 들어가기 완료 |
| `group_list_invite_code_pasted` | — | 초대코드 붙여넣기 |
| `group_list_refreshed` | — | 새로고침 |
| `group_menu_invite_code_copied` | — | 초대코드 복사 |
| `group_menu_left` | — | 그룹 나가기 완료 |
| `group_menu_nickname_changed` | — | 그룹 닉네임 변경 완료 (변경 빈도는 사용자별 이벤트 수로 확인) |

### 맞춤 측정기준 등록 대상

리포트에서 값별로 나눠 보려면 Firebase 콘솔에서 **이벤트 범위 맞춤 측정기준**으로 등록한다. 배포 전에 등록해야 첫날 데이터부터 보인다.

`type` · `destination` · `source` · `success` · `group_id` · `is_border_on` · `action` · `is_mine` · `format`

[^oq430]: 정본 문서의 "C105"가 재편집(`C-305`)까지 포함할 의도였는지 확인이 남았다 — [OQ-P-430](../../synthesis/open-questions.md#2026-10-07-canvas_edit_-화면-분류가-정본-문서와-코드에서-다르다) ①.
