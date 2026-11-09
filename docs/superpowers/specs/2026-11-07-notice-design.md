---
id: notice
title: 공지사항 (Notice)
status: in-progress
category: ui-spec
platforms: android
verified: 2026-11-07
related_code:
  - AppSettingScreen.kt#AppSettingScreen
  - ListItemMenuSub.kt#ListItemMenuSub
  - NotificationSettingRepositoryImpl.kt#NotificationSettingRepositoryImpl
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, setting, notice]
---

# Spec: 공지사항

> 이슈 #671. 상태·날짜는 위 frontmatter가 단일 출처.

## 목표

앱 사이드메뉴(설정) 화면의 '알림 설정' 아래에 '공지사항' 항목을 두고, 누르면 공지 목록 → 공지 상세로 들어간다.
서버 API가 아직 없어 UseCase까지만 만들고 Repository는 Mock으로 둔다. 디자인은 러프하게 만들고 상세 스펙은 나중에 받는다.

## 범위

- 포함: 설정 화면 '공지사항' 항목 + `YGChipNew`, 공지 목록 화면, 공지 상세 화면, domain 모델·UseCase, Mock Repository, 새 모듈 `feature/app/notice`.
- 제외: 서버 연동, 읽음 처리·NEW 판정 규칙, 페이지네이션, 로그 이벤트, 상세 디자인 스펙(추후 전달).

## 설계

### 디자인 시스템

- `YGChipNew`(`core/designsystem`) — `Text("NEW")` 하나. 배경 `YGAtomicColors.Cherry.Cherry100`, 패딩 세로 `padding1`·가로 `padding2`, 글자 `c01M`·`Cherry700`. 너비·높이는 고정하지 않고 글자를 따른다.
- `ListItemMenuSub`(`feature/app/notice/impl` `component/`) — 제목(최대 2줄·말줄임)·선택 부가 텍스트·`isNew`일 때 제목 오른쪽 4dp 뒤의 `YGChipNew`·caret(`YGIconButton` 44dp) 행. 가로 여백은 두지 않아 놓이는 화면이 정한다. 공지 목록만 쓴다.

### domain

- `NoticeVO(id: Long, title: String, date: String, content: String, isNew: Boolean)`. `date`는 표시 형식(`2026.11.17`) 그대로 둔다 — Mock 단계에서 날짜 타입 변환이 가치를 만들지 않는다.
- `NoticeRepository`: `getNotices(): Result<List<NoticeVO>>`, `getNotice(id: Long): Result<NoticeVO>`.
- `GetNoticesUseCase`, `GetNoticeUseCase` — Repository를 감싸는 얇은 UseCase(알림 설정 UseCase와 같은 형태).

### data

- `NoticeRepositoryImpl`(`@Singleton`) — 이미지의 공지 5건을 메모리에 들고 있는 Mock. `getNotice`는 없는 id에 `Result.failure`. KDoc에 서버 연동 전 임시 구현임을 적는다. `RepositoryModule`에 `@Binds`.

### feature/app/notice (신규 모듈 api + impl)

- api: `NavKeyNoticeList`(data object), `NavKeyNoticeDetail(noticeId: Long)`.
- impl: 목록 Route·Screen·ViewModel, 상세 Route·Screen·ViewModel, `EntryBuilder`, `NavigationModule`. `settings.gradle.kts`와 `app/build.gradle.kts`에 등록.
- 목록: `YGTopBarDetail`("공지사항") + 행 목록. 행은 제목·날짜 두 줄과 caret이고 `isNew`면 제목 옆에 `YGChipNew`. 행 탭은 `navigator.goTo(NavKeyNoticeDetail(id))`.
- 상세: 제목·날짜·본문. 본문은 테두리 있는 박스. 상세는 `noticeId`로 `GetNoticeUseCase`를 불러온다.
- 두 ViewModel 모두 `BaseViewModel<State, Intent, SideEffect>` MVI. 로딩 실패는 `YGToastPolicy.showError`(문구는 임시 "공지를 불러오지 못했어요, 나중에 다시 시도해 주세요").

### feature/app/setting

- `AppSettingScreen`: '알림 설정' 아래 `YGListItem(text = 공지사항, trailingIcon, isNew = true)` — `YGListItem`의 제목 오른쪽 4dp 뒤에 `YGChipNew`가 붙는다. 설정 화면의 NEW는 일단 항상 표시한다.
- `AppSettingViewModel`: `ClickNotice` intent → `NavigateToNotice` effect → `navigator.goTo(NavKeyNoticeList)`. 알림 설정과 같은 구조.
- `setting:impl`이 `feature.app.notice.api`에 의존한다.

## 테스트

- `NoticeListViewModelTest`·`NoticeDetailViewModelTest`: 로딩 성공·실패, 항목 탭 이동, 뒤로가기.
- `NoticeRepositoryImplTest`: 목록 반환, 없는 id는 실패.
- `AppSettingViewModelTest`: `ClickNotice` → `NavigateToNotice`.

## 주의 / 열린 질문

- NEW 판정 규칙(서버 필드인지, 앱의 읽음 기록인지)은 API 확정 때 정한다. 지금 Mock은 최신 1건만 `isNew`.
- 상세 디자인은 추후 전달 예정이라 이번 화면 스타일은 임시다.
- 공지 화면 ID(`S-xxx`)와 분석 이벤트는 정해지지 않아 이번에 다루지 않는다.
