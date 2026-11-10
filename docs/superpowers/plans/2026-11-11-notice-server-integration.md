# 공지사항 서버 연동 · 읽음 상태 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 `GET /api/v1/notices`를 앱에 연결하고, 설정 화면 공지사항 행이 "게시 14일 미만이면서 안 읽은 공지가 있는가"를 보여주게 한다.

**Architecture:** 공지 목록은 인메모리 SSoT(`NoticeLocalDataSource`), 읽음 id는 DataStore(`NoticeReadLocalDataSource`)에 둔다. NEW는 UseCase가 두 흐름과 오늘(KST)을 결합해 파생한다. 설정 화면이 서버 갱신을 일으키고, 목록은 SSoT를 구독하며(비어 있으면 1회 갱신), 상세는 NavKey로 공지 한 건을 받아 서버를 부르지 않는다. 그룹 SSoT([ADR-0023](../../adr/0023-group-in-memory-ssot.md))와 같은 패턴이다.

**Tech Stack:** Kotlin, Hilt, Retrofit + kotlinx.serialization, DataStore Preferences, `kotlinx.datetime`·`kotlin.time.Clock`, MockK + Turbine + `MainDispatcherRule`.

**Spec:** [`docs/superpowers/specs/2026-11-11-notice-server-integration-design.md`](../specs/2026-11-11-notice-server-integration-design.md) · 서버 계약 [`docs/api/notice.md`](../../api/notice.md) · 코드 주석 규약 [`docs/code-conventions.md`](../../code-conventions.md)

## Global Constraints

- 브랜치 `feature/#671-notice_api`(베이스는 최신 `develop`). 응답·문서·테스트 주석은 한국어, 식별자·커밋 메시지는 영어.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. 커밋은 Task 단위. **push·PR은 사용자가 시킬 때만.** 커밋·푸시·PR은 `JeonK1` 계정으로 한다.
- `.agents/` `.codex/` `AGENTS.md` `l1.png`는 우리 것이 아니다 — 스테이징 금지.
- 프로젝트 규칙: `.claude/rules/one-type-per-file.md`, `no-dto-serialization-test.md`(DTO 직렬화 테스트 금지), `repository-package.md`, `no-local-functions.md`. 주석은 "왜"만. Preview는 `private`.
- 요청 `platform`은 `ANDROID`. NEW = **`오늘(KST) - publishedDate < 14일` 이고 `id ∉ 읽은 id`**. KST는 `TimeZone.of("Asia/Seoul")`, 시각은 주입한 `kotlin.time.Clock`(`ClockModule`이 `Clock.System`을 제공)으로 얻는다.
- 읽음 id는 **기기 단위**라 로그아웃·세션 만료에도 지우지 않는다. 공지 캐시는 두 경로(`LogoutUseCase`, `TokenAuthenticator`)에서 모두 비운다.
- 상세 화면은 서버·UseCase 조회를 하지 않는다. `NavKeyNoticeDetail(noticeId: Long, title: String, date: String, content: String)`, `date`는 `2026.11.17` 형식.
- 설정 화면의 알림·공지 행은 #701로 주석 처리돼 있다. **주석은 풀지 않는다**(되돌림은 #701 revert로만). 주석 안의 `isNew` 줄만 새 값으로 고친다.
- 컴파일 순서: Task 1~4는 `:domain`·`:data`만, Task 5~6은 feature만 고친다. Task 1에서 `NoticeVO`가 바뀌므로 **Task 5가 끝나기 전에는 `:feature:app:notice:impl`·`:app`이 컴파일되지 않는다.** 각 Task는 자기 모듈의 테스트로 검증하고 전체 컴파일은 Task 7에서 본다.

## Review Focus

- 공지가 0건이어도(빈 목록) 크래시 없이 `isNew` 없는 빈 목록이 나가고 설정 칩이 꺼진다 → Task 1 `GetNoticeItemsUseCaseTest`·`HasNewNoticeUseCaseTest`.
- 읽은 id 집합에 목록에 없는 id(삭제된 공지 등)가 섞여 있어도 무시된다 → Task 1.
- 서버 갱신이 실패하면 기존 캐시를 덮어쓰지 않고 유지한다 → Task 4 `NoticeRepositoryImplTest`.
- 로그아웃과 세션 만료 모두 공지 캐시를 비워, 다른 계정으로 재로그인해도 이전 공지가 남지 않는다 → Task 1 `LogoutUseCaseTest`, Task 4 `TokenAuthenticatorTest`.
- `markRead`가 실패(DataStore IO)해도 상세 화면은 그대로 열려 있다 → Task 5 `NoticeDetailViewModelTest`.

---

### Task 1: domain — 모델·Repository 인터페이스·UseCase

**Files:**
- Modify: `domain/src/main/java/com/teamyg/parfait/domain/model/notice/NoticeVO.kt`
- Create: `domain/.../model/notice/NoticeItemVO.kt`
- Modify: `domain/.../repository/notice/NoticeRepository.kt`
- Create: `domain/.../usecase/notice/{GetNoticeItemsUseCase,HasNewNoticeUseCase,RefreshNoticesUseCase,MarkNoticeReadUseCase}.kt`
- Delete: `domain/.../usecase/notice/{GetNoticesUseCase,GetNoticeUseCase}.kt`
- Modify: `domain/.../usecase/auth/LogoutUseCase.kt`, `domain/src/test/.../usecase/auth/LogoutUseCaseTest.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/notice/{GetNoticeItemsUseCaseTest,HasNewNoticeUseCaseTest}.kt`

**Interfaces:**
- Produces:
  - `data class NoticeVO(val id: Long, val title: String, val publishedDate: LocalDate, val content: String)` — `kotlinx.datetime.LocalDate`. `date`·`isNew` 필드는 없앤다.
  - `data class NoticeItemVO(val notice: NoticeVO, val isNew: Boolean)`
  - `interface NoticeRepository { val notices: Flow<List<NoticeVO>?>; val readNoticeIds: Flow<Set<Long>>; suspend fun refreshNotices(): Result<Unit>; suspend fun markRead(id: Long); fun clearNotices() }`
  - `GetNoticeItemsUseCase @Inject constructor(noticeRepository: NoticeRepository, clock: Clock)` · `operator fun invoke(): Flow<List<NoticeItemVO>?>` — `kotlin.time.Clock`.
  - `HasNewNoticeUseCase @Inject constructor(getNoticeItems: GetNoticeItemsUseCase)` · `operator fun invoke(): Flow<Boolean>`
  - `RefreshNoticesUseCase` · `suspend operator fun invoke(): Result<Unit>`, `MarkNoticeReadUseCase` · `suspend operator fun invoke(id: Long)`

- [ ] **Step 1: 실패하는 테스트를 쓴다** — `GetNoticeItemsUseCaseTest`(고정 `Clock` = 2026-11-17 KST, Repository는 MockK로 두 Flow를 대체). 테스트 이름과 단언:
  - `invoke_publishedThirteenDaysAgo_isNew` — `2026-11-04` 게시, 안 읽음 → `isNew == true`
  - `invoke_publishedFourteenDaysAgo_isNotNew` — `2026-11-03` 게시 → `isNew == false`
  - `invoke_readNotice_isNotNew` — 13일 전 게시지만 `readNoticeIds = setOf(id)` → `false`
  - `invoke_cacheNotLoaded_emitsNull` — `notices`가 `null` → 방출 값 `null`
  - `invoke_emptyList_emitsEmptyList` — 빈 목록 → 빈 리스트(`null` 아님)
  - `invoke_readIdsNotInList_areIgnored` — 목록에 없는 id만 읽음 집합에 있음 → 목록 항목은 영향 없음
  - `invoke_serverOrderIsKept` — 입력 순서 `[3, 2, 1]` → 출력 순서 동일
  - `invoke_markedReadLater_flipsToNotNew` — `readNoticeIds`가 `emptySet → setOf(id)`로 바뀌면 같은 항목의 `isNew`가 `true → false`(Turbine)
  `HasNewNoticeUseCaseTest`: `noNewNotice_emitsFalse`(전부 읽음), `someNewNotice_emitsTrue`, `cacheNotLoaded_emitsFalse`, `emptyList_emitsFalse`. `LogoutUseCaseTest`에 `invoke_clearsNoticeCache`(`noticeRepository.clearNotices()` 1회 호출) 추가, 기존 생성자 호출에 `noticeRepository` 인자를 더한다.

- [ ] **Step 2: 실패 확인** — Run: `./gradlew :domain:test --tests '*notice*' --tests '*LogoutUseCaseTest*'` / Expected: 컴파일 실패(타입 미정의).

- [ ] **Step 3: 구현** — 위 Interfaces 시그니처 그대로. `GetNoticeItemsUseCase`는 `combine(notices, readNoticeIds)`로 결합하고, 오늘은 방출할 때마다 `clock.now().toLocalDateTime(KST).date`로 얻으며 `publishedDate.daysUntil(today) < NEW_DAYS(14)`로 판정한다(`null`은 그대로 전파). `HasNewNoticeUseCase`는 `map { it?.any(NoticeItemVO::isNew) == true }.distinctUntilChanged()`. `LogoutUseCase`는 `noticeRepository.clearNotices()`를 `clearTodayCanvas()` 바로 뒤, `runSuspendCatching` 앞에 두고 KDoc의 "던지지 않는 인메모리 정리" 설명에 맞춘다. 옛 UseCase 두 개와 `NoticeVO.date`·`isNew`는 삭제한다.

- [ ] **Step 4: 통과 확인** — Run: `./gradlew :domain:test` / Expected: PASS.

- [ ] **Step 5: 커밋** — `git add domain && git commit -m "Feat : add notice items use case with 14-day new rule"` (본문에 NEW 규칙과 `isNew`를 캐시에 두지 않는 이유 한 줄).

---

### Task 2: data — 서버 호출 (Service · DTO · RemoteDataSource · 매퍼)

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/NoticeService.kt`, `.../service/model/response/notice/NoticeResponse.kt`
- Create: `.../source/notice/mapper/VOMapper.kt`, `.../source/notice/remote/{NoticeRemoteDataSource,NoticeRemoteDataSourceImpl}.kt`
- Modify: `.../di/ServiceModule.kt`, `.../di/RemoteDataSourceModule.kt`
- Test: `data/src/test/.../source/notice/mapper/VOMapperTest.kt`, `.../source/notice/remote/NoticeRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `NoticeVO` (Task 1).
- Produces: `interface NoticeRemoteDataSource { suspend fun getNotices(): Result<List<NoticeVO>> }`, `NoticeService.getNotices(@Query("platform") platform: String): ApiResponse<List<NoticeResponse>>`(JWT 필요 — `@NoAuth` 없음), `NoticeResponse(id: Long, type: String, title: String, content: String, publishedDate: String)`(`@SerialName` 전 필드 명시).

- [ ] **Step 1: 실패하는 테스트** — `VOMapperTest.toNoticeVO_parsesIsoDateToLocalDate`(`"2026-11-12"` → `LocalDate(2026, 11, 12)`, 나머지 필드 보존), `NoticeRemoteDataSourceImplTest`: `getNotices_success_returnsVoListInServerOrder`(id 2→1 순서 유지), `getNotices_always_requestsAndroidPlatformOnce`(`coVerify(exactly = 1) { service.getNotices("ANDROID") }`), `getNotices_businessFailure_returnsBusinessException`, `getNotices_ioException_returnsNetworkException`. 기존 `PolicyRemoteDataSourceImplTest`와 같은 형태(`ApiCaller(Json { ignoreUnknownKeys = true })`).
- [ ] **Step 2: 실패 확인** — Run: `./gradlew :data:testDebugUnitTest --tests '*source.notice*'` / Expected: 컴파일 실패.
- [ ] **Step 3: 구현** — 위 시그니처 그대로. `NoticeRemoteDataSourceImpl`은 `apiCaller.safeApiCall(block = { noticeService.getNotices(PLATFORM) }, transform = { list -> list.map { it.toNoticeVO() } })`, `PLATFORM = "ANDROID"`. 두 DI 모듈에 `@Provides`/`@Binds` 추가. `type`은 화면이 구분하지 않아 VO에 싣지 않는다는 이유를 DTO KDoc에 적는다. Task 1이 `NoticeRepository`를 바꿔 Mock `NoticeRepositoryImpl`이 컴파일되지 않으므로, 이 Step에서 그 본문을 새 인터페이스를 만족하는 `TODO()` 스텁(생성자는 Task 4 형태 그대로 `@Inject constructor()`)으로 줄이고 옛 `NoticeRepositoryImplTest.kt`를 삭제한다. 둘 다 Task 4에서 실제 구현·새 테스트로 대체된다.
- [ ] **Step 4: 통과 확인** — Run: `./gradlew :data:testDebugUnitTest --tests '*source.notice*'` / Expected: PASS.
- [ ] **Step 5: 커밋** — `Feat : add notice list remote data source`

---

### Task 3: data — 로컬 저장 (SSoT · 읽음)

**Files:**
- Create: `.../source/notice/local/{NoticeLocalDataSource,NoticeLocalDataSourceImpl,NoticeReadLocalDataSource,NoticeReadLocalDataSourceImpl}.kt`
- Modify: `.../di/LocalDataSourceModule.kt`
- Test: `data/src/test/.../source/notice/local/{NoticeLocalDataSourceImplTest,NoticeReadLocalDataSourceImplTest}.kt`

**Interfaces:**
- Produces: `interface NoticeLocalDataSource { val notices: StateFlow<List<NoticeVO>?>; fun saveNotices(notices: List<NoticeVO>); fun clear() }`(`@Singleton` 인메모리, `GroupLocalDataSourceImpl`과 같은 형태, `null`은 미수신·빈 목록은 0건), `interface NoticeReadLocalDataSource { val readNoticeIds: Flow<Set<Long>>; suspend fun markRead(id: Long) }`(DataStore, `stringSetPreferencesKey("notice_read_ids")`, `PastCanvasAlertLocalDataSourceImpl`처럼 `DataStore<Preferences>` 주입).

- [ ] **Step 1: 실패하는 테스트** — `NoticeLocalDataSourceImplTest`: `initially_isNull`, `saveNotices_emitsList`, `saveEmptyList_isEmptyNotNull`, `clear_returnsToNull`. `NoticeReadLocalDataSourceImplTest`(`FakePreferencesDataStore` 사용): `initially_isEmptySet`, `markRead_addsId`, `markRead_sameIdTwice_isIdempotent`(집합 크기 1), `markRead_accumulatesDifferentIds`, `readNoticeIds_ignoresNonNumericStoredValues`(`putRaw`로 `"x"` 심고 집합에서 제외).
- [ ] **Step 2: 실패 확인** — Run: `./gradlew :data:testDebugUnitTest --tests '*notice.local*'` / Expected: 컴파일 실패.
- [ ] **Step 3: 구현** — `readNoticeIds`는 `dataStore.data.map { prefs -> prefs[KEY].orEmpty().mapNotNull(String::toLongOrNull).toSet() }.distinctUntilChanged()`, `markRead`는 `dataStore.edit`로 집합에 `id.toString()` 추가. `LocalDataSourceModule`에 `@Binds @Singleton` 두 개. 읽음 저장소는 로그아웃에서 지우지 않는다는 이유를 KDoc 한 줄로 남긴다.
- [ ] **Step 4: 통과 확인** — Run: `./gradlew :data:testDebugUnitTest --tests '*notice.local*'` / Expected: PASS.
- [ ] **Step 5: 커밋** — `Feat : add notice in-memory cache and read-id storage`

---

### Task 4: data — Repository 구현 · 세션 만료 정리

**Files:**
- Modify: `data/.../repository/notice/NoticeRepositoryImpl.kt`(Mock 전체 교체), `.../network/TokenAuthenticator.kt`
- Modify: `data/src/test/.../repository/notice/NoticeRepositoryImplTest.kt`(전체 교체), `.../network/TokenAuthenticatorTest.kt`

**Interfaces:**
- Consumes: Task 1의 `NoticeRepository`, Task 2·3의 데이터소스.
- Produces: `NoticeRepositoryImpl @Inject constructor(noticeRemoteDataSource, noticeLocalDataSource, noticeReadLocalDataSource)`. `TokenAuthenticator`에 생성자 인자 `noticeLocalDataSource: NoticeLocalDataSource`(`groupLocalDataSource` 바로 뒤)를 더한다.

- [ ] **Step 1: 실패하는 테스트** — `NoticeRepositoryImplTest`: `refreshNotices_success_savesToCache`(`saveNotices` 호출·`Result.success`), `refreshNotices_failure_keepsExistingCache`(`saveNotices` 미호출, 실패 전파), `refreshNotices_failure_mapsToAppError`, `notices_exposesLocalCacheFlow`, `readNoticeIds_exposesReadStorageFlow`, `markRead_delegatesToReadStorage`, `clearNotices_clearsCacheButNotReadIds`(`noticeLocalDataSource.clear()` 1회, 읽음 저장소 호출 없음). `TokenAuthenticatorTest`: 기존 세션 종료 테스트(`verify(exactly = 1) { groupLocalDataSource.clear() }` 근처)에 `verify(exactly = 1) { noticeLocalDataSource.clear() }` 추가, 생성자에 `noticeLocalDataSource = mockk(relaxed = true)` 추가.
- [ ] **Step 2: 실패 확인** — Run: `./gradlew :data:testDebugUnitTest --tests '*NoticeRepositoryImplTest*' --tests '*TokenAuthenticatorTest*'` / Expected: FAIL.
- [ ] **Step 3: 구현** — `refreshNotices() = remote.getNotices().onSuccess(local::saveNotices).map { }.mapErrorToAppError()`(그룹 Repository와 같은 형태), 나머지는 위임. Task 2의 임시 스텁을 제거한다. `TokenAuthenticator`는 `groupLocalDataSource.clear()` 바로 뒤 "던지지 않는 인메모리 캐시" 구간에 `noticeLocalDataSource.clear()`를 둔다. `RepositoryModule`의 기존 `bindNoticeRepository`는 그대로 쓴다.
- [ ] **Step 4: 통과 확인** — Run: `./gradlew :data:testDebugUnitTest` / Expected: PASS.
- [ ] **Step 5: 커밋** — `Feat : connect notice repository to server and clear cache on session end`

---

### Task 5: feature/app/notice — 목록 구독 · 상세 NavKey

**Files:**
- Modify: `feature/app/notice/api/.../NavKeyNoticeDetail.kt`
- Modify: `feature/app/notice/impl/.../viewmodel/{NoticeListViewModel,NoticeListState,NoticeListSideEffect,NoticeDetailViewModel,NoticeDetailState,NoticeDetailSideEffect}.kt`, `.../route/{NoticeListRoute,NoticeDetailRoute}.kt`, `.../screen/{NoticeListScreen,NoticeDetailScreen}.kt`
- Create: `.../util/NoticeDateText.kt`
- Modify: `app/src/test/.../analytics/NavKeyAnalyticsScreenTest.kt`
- Test: `.../viewmodel/{NoticeListViewModelTest,NoticeDetailViewModelTest}.kt`, `.../util/NoticeDateTextTest.kt`

**Interfaces:**
- Consumes: `GetNoticeItemsUseCase`, `RefreshNoticesUseCase`, `MarkNoticeReadUseCase`, `NoticeItemVO`(Task 1).
- Produces: `NavKeyNoticeDetail(noticeId: Long, title: String, date: String, content: String)`, `internal fun LocalDate.toNoticeDateText(): String`(`2026.11.17`, 월·일 두 자리 0 패딩), `NoticeListState(notices: List<NoticeItemVO> = emptyList())`, `NoticeListSideEffect.NavigateToDetail(notice: NoticeVO)`, `NoticeDetailState(title: String, date: String, content: String)`(`NoticeDetailSideEffect`는 `NavigateBack`만 남긴다), `NoticeDetailViewModel.Factory.create(key: NavKeyNoticeDetail)`.

- [ ] **Step 1: 실패하는 테스트**
  - `NoticeDateTextTest`: `toNoticeDateText_padsMonthAndDay`(`LocalDate(2026, 1, 5)` → `"2026.01.05"`), `toNoticeDateText_twoDigitMonthAndDay`(`2026.11.17`).
  - `NoticeListViewModelTest`(기존 테스트를 `GetNoticeItemsUseCase` Flow 기준으로 바꾼다): `init_cacheEmpty_refreshesOnce`(Flow가 `null`이면 `refreshNotices` 1회), `init_cacheLoaded_doesNotRefresh`, `init_refreshFails_showsLoadError`, `init_emptyList_showsNothingWithoutError`, `clickNotice_navigatesWithThatNotice`(`NavigateToDetail(notice)`가 눌린 항목의 `NoticeVO`), `clickNotice_logsTappedEventWithRowIsNew`(`notice_id`, `is_new` = 행의 `isNew`), `clickNotice_unknownId_navigatesNowhereAndLogsNothing`, `clickBack_navigatesBack`.
  - `NoticeDetailViewModelTest`: `init_buildsStateFromKey`(title·date·content 그대로), `init_marksNoticeRead`(`markNoticeRead(noticeId)` 1회), `init_markReadThrows_screenStaysUsable`(예외를 던져도 상태 유지·크래시 없음), `clickBack_navigatesBack`.
  - `NavKeyAnalyticsScreenTest`: `NavKeyNoticeDetail(noticeId = 1L, title = "t", date = "2026.11.17", content = "c") to "S-106-detail"`.
- [ ] **Step 2: 실패 확인** — Run: `./gradlew :feature:app:notice:impl:testDebugUnitTest :app:testDebugUnitTest --tests '*NavKeyAnalyticsScreenTest*'` / Expected: 컴파일 실패.
- [ ] **Step 3: 구현**
  - `NoticeListViewModel`: 생성자 `(getNoticeItems, refreshNotices, analyticsLogger)`. `init`에서 `getNoticeItems()`를 collect해 `null`이면 `refreshNotices()`(실패 시 `ShowLoadError`·로그), 아니면 `notices` 갱신. 행 탭은 `state.value.notices`에서 id로 찾고(없으면 아무것도 안 함), 있으면 기존 분석 이벤트(`APP_NOTICE_ITEM_TAPPED`, `notice_id`·`is_new`)를 남긴 뒤 `NavigateToDetail(item.notice)`.
  - `NoticeListRoute`: `NavigateToDetail`에서 `NavKeyNoticeDetail(noticeId = id, title = title, date = publishedDate.toNoticeDateText(), content = content)`로 이동. `NoticeListScreen`: 행의 날짜는 `item.notice.publishedDate.toNoticeDateText()`, 칩은 `item.isNew`. Preview 데이터도 새 모델로 바꾼다.
  - `NoticeDetailViewModel`: `@Assisted key: NavKeyNoticeDetail`, 생성자 `(key, markNoticeRead)`. 초기 상태를 key로 만들고, `init`에서 `runSuspendCatching { markNoticeRead(key.noticeId) }`(LogoutUseCase가 쓰는 같은 헬퍼)로 호출해 실패는 로그만 남긴다. `NoticeDetailRoute`는 `factory.create(key)`, 로딩 에러 토스트 코드와 문자열 사용처를 제거한다(`notice_load_error`는 목록이 계속 쓴다). `NoticeDetailScreen`은 non-null 상태로 그리고 null 분기를 없앤다.
- [ ] **Step 4: 통과 확인** — Run: `./gradlew :feature:app:notice:impl:testDebugUnitTest :app:testDebugUnitTest` / Expected: PASS. (`:app:compileDebugKotlin`은 Task 6 후 Task 7에서 확인.)
- [ ] **Step 5: 커밋** — `Feat : read notice list from cache and pass detail through nav key`

---

### Task 6: feature/app/setting — 설정 화면 NEW 칩

**Files:**
- Modify: `feature/app/setting/impl/.../viewmodel/AppSettingViewModel.kt`(상태·생성자), `.../screen/AppSettingScreen.kt`
- Test: `feature/app/setting/impl/src/test/.../viewmodel/AppSettingViewModelTest.kt`

**Interfaces:**
- Consumes: `RefreshNoticesUseCase`, `HasNewNoticeUseCase`(Task 1).
- Produces: `AppSettingState.hasNewNotice: Boolean = false`(KDoc: 안 본 새 공지가 하나라도 있는지), `AppSettingViewModel` 생성자에 `refreshNotices: RefreshNoticesUseCase`, `hasNewNotice: HasNewNoticeUseCase` 추가.

- [ ] **Step 1: 실패하는 테스트** — `init_refreshesNoticesOnce`(`refreshNotices` 1회), `init_refreshFails_keepsChipOffAndDoesNotCrash`(`Result.failure` → `hasNewNotice == false`), `hasNewNotice_followsUseCaseFlow`(`flowOf(false) → true → false` 반영, Turbine). 기존 `viewModel()` 헬퍼에 두 UseCase를 기본 스텁(`refreshNotices` = `Result.success(Unit)`, `hasNewNotice` = `flowOf(false)`)으로 추가한다.
- [ ] **Step 2: 실패 확인** — Run: `./gradlew :feature:app:setting:impl:testDebugUnitTest` / Expected: 컴파일 실패.
- [ ] **Step 3: 구현** — `init`에서 `launch(key = KEY_REFRESH_NOTICES)`로 갱신(실패는 `viewModelLogger.e`만) 하고, 별도 `launch`로 `hasNewNotice()`를 collect해 `updateState { copy(hasNewNotice = it) }`. `AppSettingScreen`의 **주석 처리된** 공지 행에서 `// isNew = true, // TODO: ...` 줄을 `//                        isNew = state.hasNewNotice,`로 바꾼다(주석은 풀지 않는다).
- [ ] **Step 4: 통과 확인** — Run: `./gradlew :feature:app:setting:impl:testDebugUnitTest` / Expected: PASS.
- [ ] **Step 5: 커밋** — `Feat : drive setting notice chip from unread recent notices`

---

### Task 7: 문서 · 최종 검증

**Files:**
- Modify: `docs/api/notice.md`(`android_status: done`, 표의 Android 열 `구현됨`), `docs/api/README.md`(행의 `미구현` → `구현됨`), `docs/status.md`(공지사항 절을 새 동작으로 교체 — Mock 서술 삭제, 설정 칩·목록 구독·상세 NavKey·NEW 14일 규칙), `docs/architecture/module-structure.md`(공지 모듈 설명의 Mock 서술), `docs/log.md`(append), `docs/superpowers/plans/README.md`(이 계획 행)

- [ ] **Step 1: 문서를 갱신한다** — `docs/log.md`에는 `## [2026-11-11] change | 공지사항 서버 연동·읽음 상태` 한 항목을 **맨 아래에 추가**만 한다(append-only). `status.md`는 현재 상태만 적고 변경 서술·PR 번호는 적지 않는다.
- [ ] **Step 2: 전체 검증** — Run: `python3 docs/script/check_links.py docs` / Expected: 깨진 링크 0건. Run: `./gradlew ktlintCheck :domain:test :data:testDebugUnitTest :feature:app:notice:impl:testDebugUnitTest :feature:app:setting:impl:testDebugUnitTest :app:testDebugUnitTest :app:compileDebugKotlin` / Expected: 전부 통과.
- [ ] **Step 3: 에뮬레이터 확인(로컬 한정)** — 설정 화면의 알림·공지 행 주석을 **로컬에서만** 풀고 `:app:installDebug`로 설치, 사용자가 카카오 로그인한 뒤 확인한다: (a) 설정의 공지사항 행에 NEW가 뜬다 (b) 목록이 서버 공지를 최신순으로 보여주고 각 행 NEW가 같은 규칙을 따른다 (c) 행을 눌러 상세에 제목·날짜·본문이 나오고 **네트워크 호출이 추가로 나가지 않는다** (d) 뒤로 돌아오면 그 행의 NEW가 사라지고, 모두 읽으면 설정의 NEW도 꺼진다 (e) 앱 재실행 후에도 읽음이 유지된다. 확인 후 `git checkout -- feature/app/setting/impl/.../AppSettingScreen.kt`로 주석 해제를 반드시 되돌리고 `git status`로 확인한다.
- [ ] **Step 4: 커밋** — `docs: reflect notice server integration in status and api contract`. push·PR은 사용자 요청 시에만.
