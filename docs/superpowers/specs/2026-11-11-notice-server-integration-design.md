---
id: notice-server-integration
title: 공지사항 서버 연동 · 읽음 상태 (Notice Server Integration)
status: in-progress
category: data-flow
platforms: android
verified: 2026-11-11
related_code:
  - NoticeRepositoryImpl.kt#NoticeRepositoryImpl
  - GroupLocalDataSourceImpl.kt#GroupLocalDataSourceImpl
  - AppSettingViewModel.kt#AppSettingViewModel
  - LogoutUseCase.kt#LogoutUseCase
related_adr:
  - ../../adr/0023-group-in-memory-ssot.md
related_spec:
  - 2026-11-07-notice-design.md
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, notice, data-flow]
---

# Spec: 공지사항 서버 연동 · 읽음 상태

> 이슈 #671. 상태·날짜는 위 frontmatter가 단일 출처. Mock 단계의 화면 설계는 [2026-11-07-notice-design.md](2026-11-07-notice-design.md)가 정본이고, 이 문서는 그 위에 서버 연동과 읽음 상태를 얹는다.

## 목표

서버 공지 API(`GET /api/v1/notices`, [api/notice.md](../../api/notice.md))를 앱에 연결하고, 설정 화면의 공지사항 행이 "아직 안 본 새 공지가 있는가"를 보여주게 한다.

- **설정 화면 NEW**: 게시 14일 미만이면서 이 앱에서 상세를 연 적 없는 공지가 하나라도 있으면 설정 화면 공지사항 행에 `YGChipNew`를 띄운다. 지금의 `isNew = true // TODO`를 대체한다.
- **API 호출 지점**: 서버 호출은 설정 화면 진입이 일으키고, 목록·상세는 그 결과를 이어 쓴다. 상세는 서버를 부르지 않는다.

## 범위

- 포함: 공지 인메모리 SSoT, 읽음 id 영속, NEW 판정, 설정 화면 칩, 목록 화면의 SSoT 구독, 상세 화면의 NavKey 입력화, 로그아웃 시 캐시 정리, Mock 제거.
- 제외: 푸시·딥링크로 공지 상세에 직접 진입(id만으로 여는 경로), 페이지네이션, 계정별 읽음 분리, 공지 본문 갱신 반영(열려 있는 상세), 분석 이벤트 변경(이미 [log 스펙](2026-10-06-user-events-design.md)에 있는 이벤트는 그대로).

## 서버 계약 요약

`GET /api/v1/notices?platform=ANDROID`(JWT)는 `id`·`type`·`title`·`content`·`publishedDate`(`yyyy-MM-dd`)를 최신순으로 준다. **상세 엔드포인트와 읽음 필드가 없다** — 그래서 상세는 목록 값으로 그리고, NEW는 앱이 판정한다.

## 설계

### 데이터 흐름

```
설정 화면 진입 ── refreshNotices() ──▶ 서버 ──▶ NoticeLocalDataSource(인메모리)
                                                      │ notices: StateFlow
            NoticeReadLocalDataSource(DataStore) ──┐  ▼
              readNoticeIds: Flow<Set<Long>>       └▶ GetNoticeItemsUseCase ─▶ 설정 칩 · 목록 행 칩
상세 화면 진입 ── markRead(id) ──▶ NoticeReadLocalDataSource
```

그룹 목록과 같은 패턴이다([ADR-0023](../../adr/0023-group-in-memory-ssot.md)) — 캐시를 읽는 Flow 하나와 서버 재조회 `refresh…()` 하나로 나눈다.

### data

- `NoticeLocalDataSource`(인메모리, `@Singleton`): `notices: StateFlow<List<NoticeVO>?>`, `saveNotices(list)`, `clear()`. `null`은 아직 못 받음, 빈 목록은 공지 0건으로 구분한다. 디스크에 남기지 않는다.
- `NoticeReadLocalDataSource`(DataStore): 읽은 공지 id 집합. `readNoticeIds: Flow<Set<Long>>`, `suspend markRead(id: Long)`. **기기 단위**라 로그아웃해도 지우지 않는다.
- `NoticeRemoteDataSource`·`NoticeService`·`NoticeResponse`: [api/notice.md](../../api/notice.md)의 계약 그대로. 요청 `platform`은 `ANDROID`.
- `NoticeRepositoryImpl`: Mock을 걷어낸다. `notices: Flow<List<NoticeVO>?>`(캐시), `suspend refreshNotices(): Result<Unit>`(서버 호출 후 `saveNotices`), `readNoticeIds: Flow<Set<Long>>`, `suspend markRead(id)`, `clearNotices()`. `getNotices()`·`getNotice(id)`는 없앤다.

### domain

- `NoticeVO(id, title, publishedDate: LocalDate, content)`. 서버 사실만 담고 `isNew`를 두지 않는다 — NEW는 읽음 상태와 오늘 날짜에 따라 바뀌는 파생 값이라 캐시에 박으면 낡는다.
- `NoticeItemVO(notice: NoticeVO, isNew: Boolean)`: 화면이 그리는 단위.
- `GetNoticeItemsUseCase`: `notices`와 `readNoticeIds`를 결합해 `Flow<List<NoticeItemVO>?>`를 낸다(`null` 전파). **NEW = `오늘(KST) - publishedDate < 14일` 이고 `id ∉ readNoticeIds`**. 오늘은 주입한 `Clock`과 `Asia/Seoul`로 얻어 테스트에서 고정할 수 있다.
- `HasNewNoticeUseCase`: 위 결과에 `isNew`가 하나라도 있는지 `Flow<Boolean>`으로 낸다.
- `RefreshNoticesUseCase`, `MarkNoticeReadUseCase`: Repository를 감싸는 얇은 UseCase.
- `LogoutUseCase`: 던지지 않는 인메모리 정리 구간에 `clearNotices()`를 더한다(`clearGroups()` 옆). `TokenAuthenticator`의 세션 종료 분기에도 `noticeLocalDataSource.clear()`를 같은 자리에 더한다 — 두 경로가 같은 캐시를 비워야 로그아웃과 세션 만료의 결과가 같다.
- 삭제: `GetNoticeUseCase`, `GetNoticesUseCase`.

### feature/app/setting

- `AppSettingViewModel`: `init`에서 `RefreshNoticesUseCase()`를 한 번 호출하고(실패는 로그만 남기고 칩 없이 둔다), `HasNewNoticeUseCase()`를 구독해 `AppSettingState.hasNewNotice`로 낸다. `AppSettingScreen`의 공지사항 행은 `isNew = state.hasNewNotice`.

### feature/app/notice

- `NavKeyNoticeDetail(noticeId: Long, title: String, date: String, content: String)`. 공지 한 건이라 백스택에 실려도 작고, 프로세스가 복원돼도 내용이 그대로라 상세에는 폴백이 필요 없다. `date`는 표시 형식(`2026.11.17`)이다. 상세가 그리지 않는 `isNew`는 싣지 않는다.
- `NoticeListViewModel`: `GetNoticeItemsUseCase()`를 구독한다. 값이 `null`(캐시 없음)이면 `RefreshNoticesUseCase()`를 한 번 호출한다 — 프로세스 복원 직후처럼 설정 화면을 거치지 않고 목록이 열린 경우를 위한 폴백이다. 실패하면 기존대로 에러 토스트. 행을 누르면 그 공지의 필드로 `NavKeyNoticeDetail`을 만들어 이동한다. 탭 분석 이벤트(`notice_id`·`is_new`)는 행의 `isNew`를 그대로 쓴다.
- `NoticeDetailViewModel`: Assisted 인자를 NavKey 값으로 바꾸고, 서버·UseCase 조회와 로딩·에러 상태를 없앤다. `init`에서 `MarkNoticeReadUseCase(noticeId)`를 호출한다.
- 목록 행의 날짜 표시: `NoticeVO.publishedDate`를 화면이 `yyyy.MM.dd`로 포맷한다.

## 오류 처리

- 설정 화면의 갱신 실패: 사용자에게 알리지 않는다(부가 정보라 칩만 없다). 로그만 남긴다.
- 목록의 갱신 실패: 에러 토스트. 캐시가 이미 있으면 갱신 실패와 무관하게 그 값을 보여준다.
- `markRead` 실패(DataStore IO): 상세 화면을 막지 않는다. 다음에 칩이 한 번 더 보일 뿐이다.

## 테스트

- `NoticeLocalDataSourceImpl`: 저장·`null` 구분·`clear`.
- `NoticeReadLocalDataSourceImpl`: `FakePreferencesDataStore`로 읽음 추가·중복·재조회.
- `NoticeRemoteDataSourceImpl`·`NoticeRepositoryImpl`: 정상·비즈니스 실패·네트워크 실패, 캐시 저장 여부, 요청 `platform`.
- `GetNoticeItemsUseCase`: 14일 경계(13일·14일), 읽은 공지 제외, `null` 전파. `Clock` 고정.
- `HasNewNoticeUseCase`: 새 공지 없음/있음/읽은 뒤 사라짐.
- `AppSettingViewModelTest`: 진입 시 갱신 호출, `hasNewNotice` 반영.
- `NoticeListViewModelTest`: 캐시 없음 → 갱신, 캐시 있음 → 호출 없음, 탭 → NavKey 필드·분석 이벤트.
- `NoticeDetailViewModelTest`: NavKey 값으로 상태 구성, 진입 시 `markRead`.
- `LogoutUseCase`·`TokenAuthenticator`: 공지 캐시 정리 호출.
- `NavKeyAnalyticsScreenTest`: `NavKeyNoticeDetail` 생성부 수정.

## 결정과 이유

- **SSoT를 인메모리로, 읽음만 영속으로 나눴다.** 공지는 서버가 정본이라 영속할 이유가 없고(그룹과 같은 판단), 읽음은 서버가 모르는 앱 로컬 사실이라 영속이 필요하다.
- **`isNew`를 캐시에 두지 않고 UseCase에서 파생한다.** 읽음 처리와 날짜 경과로 값이 바뀌는데 캐시에 넣으면 갱신 지점이 늘어난다.
- **상세만 NavKey로 받는다.** 목록 전체를 NavKey로 넘기면 용량·낡은 값 문제가 생기지만 공지 한 건은 작고, 상세에서 폴백 경로를 통째로 없앤다. 대가는 id만으로 상세를 열 수 없다는 것이다.
- **읽음은 기기 단위로 유지한다.** 공지는 계정과 무관한 전체 공지라 계정별 분리의 이득이 작다.

## 미결

- NEW 기준 14일은 이 요구에서 정해졌다. 기획 문서의 읽음 정책이 따로 확정되면 `GetNoticeItemsUseCase`의 상수 한 곳만 바꾼다.
- 푸시로 공지 상세에 바로 들어가는 진입이 생기면 id 전용 진입점이 필요하다(범위 제외 항목).
