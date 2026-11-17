# 공지 읽음 기록의 계정별 저장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 공지 읽음 id를 계정별 별칭 키로 저장해, 같은 계정 재로그인은 기록을 유지하고 다른 계정은 0부터 시작하며 Preferences 파일에서 `memberId`를 유추할 수 없게 한다.

**Architecture:** `data/security`에 Keystore HMAC 별칭 생성기(`AccountKeyAlias`)를 두고, `NoticeReadLocalDataSource`가 `MemberId`를 받아 `notice_read_<alias>` 키로 읽고 쓴다. `NoticeRepositoryImpl`은 `UserInfoLocalDataSource.myAccount`에 `flatMapLatest`로 묶는다. 도메인의 `readNoticeIds`/`markRead(id)` 시그니처는 그대로고, 탈퇴만 `clearReadNotices()`로 그 계정 기록을 지운다.

**Tech Stack:** Kotlin, DataStore Preferences, AndroidKeyStore(HMAC-SHA256), Hilt, mockk, kotlin.test, JUnit4 계측 테스트(API 28 에뮬레이터).

**Spec:** [`docs/superpowers/specs/2026-11-16-notice-read-per-account-design.md`](../specs/2026-11-16-notice-read-per-account-design.md) (공지 서버 연동 스펙 [`2026-11-11-notice-server-integration-design.md`](../specs/archive/2026-11-11-notice-server-integration-design.md)의 읽음 저장 결정을 뒤집는다)

## Global Constraints

- 브랜치 `feature/notice-read-per-account`, 워크트리 `/Users/zemic.jeon/EtcProject/TEAMYG-Android-wt-714`. 커밋은 `JeonK1`로, 메시지 끝에 `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.
- 코드·식별자·커밋 메시지·PR 제목은 영어, 주석·KDoc·문서는 한국어. 주석은 "왜"만 적고 코드가 이미 말하는 것은 적지 않는다 — [`docs/code-conventions.md`](../../code-conventions.md) 준수.
- 별칭 scope 문자열은 `"notice-read"`, Keystore 키 별칭은 `parfait_alias_key`, 별칭 길이는 HMAC 결과 hex의 앞 16자, 저장 키는 `notice_read_$alias`.
- `domain`의 `readNoticeIds`, `markRead(id)` 시그니처는 바꾸지 않는다. UseCase·ViewModel·화면은 건드리지 않는다.
- `data` 모듈 main에는 로깅 도구가 없다. 스펙이 "로그를 남긴다"고 적은 자리는 **조용히 건너뛰고** 왜 그런지만 주석으로 남긴다(Task 4에서 스펙 문구도 맞춘다).
- 옛 키 `notice_read_ids`는 미배포라 마이그레이션도 삭제도 하지 않는다.
- 각 태스크 끝에 `./gradlew :data:ktlintCheck :domain:ktlintCheck`가 통과해야 한다.
- 워크트리에서 첫 빌드 전에 `app/debug.keystore`가 없으면 메인 레포(`/Users/zemic.jeon/EtcProject/TEAMYG-Android/app/debug.keystore`)에서 복사한다(gitignored). 브랜치를 바꾼 뒤 Hilt 증분 빌드 오류가 나면 해당 모듈을 `clean`한다.

## Review Focus

- **계정 전환 중 흐름이 낡은 계정을 계속 읽는다**: `myAccount`가 A→B로 바뀌어도 이전 계정의 `readNoticeIds` 구독이 남으면 B 화면에 A의 NEW 상태가 보인다. → Task 2의 `readNoticeIds_accountSwitch_readsNewAccountKey`.
- **같은 계정으로 재로그인(null을 거침)하면 기록이 사라진다**: A → `null` → A 순서에서 A의 집합이 다시 보여야 한다. → Task 2의 `readNoticeIds_signOutThenSameAccount_restoresRecords`.
- **`myAccount == null`일 때 `markRead`가 어딘가에 쓴다**: 잘못된 계정이나 기본 키에 귀속되면 안 된다. → Task 2의 `markRead_withoutAccount_writesNothing`.
- **Keystore가 실패하면 화면이 죽는다**: 별칭 생성 예외가 읽기 흐름이나 `markRead` 호출자에게 번지면 공지 화면이 크래시한다. → Task 2의 `readNoticeIds_aliasFails_isEmptySet`, `markRead_aliasFails_doesNotThrow`.
- **탈퇴에서 삭제 순서가 뒤집히거나 삭제 실패가 로그아웃을 막는다**: `logout()`이 `myAccount`를 비운 뒤에는 지울 키를 알 수 없고, 삭제 실패로 로그아웃이 안 되면 탈퇴한 계정이 로그인 상태로 남는다. → Task 3의 `invoke_serverAccepts_clearsReadNoticesBeforeLogout`, `invoke_clearReadNoticesFails_stillLogsOut`.

---

## File Structure

| 파일 | 역할 |
|---|---|
| `data/build.gradle.kts` | 계측 테스트 플러그인(`parfait.test.android`) 추가 |
| `data/src/main/java/com/teamyg/parfait/data/security/AccountKeyAlias.kt` | 신규. `(scope, memberId)` → 결정적 별칭 |
| `data/src/androidTest/java/com/teamyg/parfait/data/security/AccountKeyAliasTest.kt` | 신규. 실제 Keystore로 결정성·구분·비노출 검증 |
| `data/src/main/java/com/teamyg/parfait/data/source/notice/local/NoticeReadLocalDataSource.kt`, `...Impl.kt` | 계정 인자·`clear` 추가, 별칭 키 사용 |
| `data/src/main/java/com/teamyg/parfait/data/repository/notice/NoticeRepositoryImpl.kt` | `myAccount`에 묶인 읽기·쓰기·삭제 |
| `domain/src/main/java/com/teamyg/parfait/domain/repository/notice/NoticeRepository.kt` | `clearReadNotices()` 추가 |
| `domain/src/main/java/com/teamyg/parfait/domain/usecase/member/WithdrawUseCase.kt`, `.../auth/LogoutUseCase.kt` | 탈퇴만 읽음 기록 삭제 |
| 각 대응 `*Test.kt` | 위 변경의 단위 테스트 |
| `docs/...` | Task 4에서 갱신 |

---

### Task 1: AccountKeyAlias (Keystore HMAC 별칭)

**Files:**
- Modify: `data/build.gradle.kts` (`alias(libs.plugins.parfait.test.android)` 추가)
- Create: `data/src/main/java/com/teamyg/parfait/data/security/AccountKeyAlias.kt`
- Test: `data/src/androidTest/java/com/teamyg/parfait/data/security/AccountKeyAliasTest.kt`

**Interfaces:**
- Produces: `class AccountKeyAlias @Inject constructor()`, `@Singleton`, `fun alias(scope: String, memberId: MemberId): String` — 길이 16의 소문자 hex. `MemberId`는 `com.teamyg.parfait.domain.model.id.MemberId`.

- [ ] **Step 1: 계측 테스트를 쓴다.** `AccountKeyAliasTest`(JUnit4, `androidx.test.ext.junit.runners.AndroidJUnit4`)에서 `AccountKeyAlias()`를 직접 만들어 쓴다.
  - `alias_sameInput_isStable`: `alias("notice-read", MemberId(1))`를 두 번 호출한 결과가 같다.
  - `alias_differentMemberId_differs`: `MemberId(1)`과 `MemberId(2)` 결과가 다르다.
  - `alias_differentScope_differs`: scope `"notice-read"`와 `"other"` 결과가 다르다.
  - `alias_hidesRawId_andHas16LowerHexChars`: 결과가 `Regex("[0-9a-f]{16}")`에 맞고, `MemberId(123456)`의 결과에 `"123456"`이 들어 있지 않다.

- [ ] **Step 2: 실패를 확인한다.** API 28 에뮬레이터를 켜고 `./gradlew :data:connectedDebugAndroidTest --tests "*AccountKeyAliasTest"` → 컴파일 실패(`AccountKeyAlias` 없음).

- [ ] **Step 3: 구현한다.** `AccountKeyAlias.kt`에 위 시그니처로 작성한다. `CryptoManager`와 같은 형태(`@Singleton`, `@Inject constructor()`, `getOrCreateKey()` + `private companion object`)를 따른다. 키는 `KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")`에 `KeyGenParameterSpec.Builder("parfait_alias_key", KeyProperties.PURPOSE_SIGN)`로 만들고, `Mac.getInstance("HmacSHA256")`으로 `"$scope:${memberId.value}"`를 서명해 hex 앞 16자를 돌려준다. KDoc에는 단순 해시를 쓰지 않는 이유(작은 정수라 대입으로 역산됨)와 백업 복원 시 키가 사라져 별칭이 달라지는 것이 의도라는 점만 적는다.

- [ ] **Step 4: 통과를 확인한다.** Step 2와 같은 명령 → 4개 PASS.

- [ ] **Step 5: 커밋한다.**

```bash
git add data/build.gradle.kts data/src/main/java/com/teamyg/parfait/data/security/AccountKeyAlias.kt data/src/androidTest
git commit -m "feat: add Keystore HMAC alias generator for account-scoped keys"
```

---

### Task 2: 계정별 읽음 저장소와 Repository

**Files:**
- Modify: `data/.../source/notice/local/NoticeReadLocalDataSource.kt`, `NoticeReadLocalDataSourceImpl.kt`
- Modify: `data/.../repository/notice/NoticeRepositoryImpl.kt`
- Modify: `domain/.../repository/notice/NoticeRepository.kt`
- Test: `data/src/test/.../source/notice/local/NoticeReadLocalDataSourceImplTest.kt`, `data/src/test/.../repository/notice/NoticeRepositoryImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 `AccountKeyAlias.alias(scope, memberId)`, `UserInfoLocalDataSource.myAccount: Flow<MyAccountVO?>`.
- Produces:
  - `NoticeReadLocalDataSource`: `fun readNoticeIds(memberId: MemberId): Flow<Set<Long>>`, `suspend fun markRead(memberId: MemberId, id: Long)`, `suspend fun clear(memberId: MemberId)`
  - `NoticeReadLocalDataSourceImpl(dataStore: DataStore<Preferences>, accountKeyAlias: AccountKeyAlias)`
  - `NoticeRepository.clearReadNotices()`: `suspend fun`
  - `NoticeRepositoryImpl(noticeRemoteDataSource, noticeLocalDataSource, noticeReadLocalDataSource, userInfoLocalDataSource: UserInfoLocalDataSource)`

- [ ] **Step 1: 데이터 소스 테스트를 바꾼다.** `AccountKeyAlias`는 `mockk()`로 두고 `MemberId(1)`→`"aaaa"`, `MemberId(2)`→`"bbbb"`를 돌려주게 스텁한다. 기존 테스트는 `memberId` 인자를 붙여 유지하고 아래를 추가한다(키 이름 하드코딩 테스트는 `"notice_read_aaaa"`로).
  - `readNoticeIds_otherAccount_isIsolated`: 계정 1에 `markRead(1L)`하면 계정 2의 `readNoticeIds`는 빈 집합.
  - `clear_removesOnlyThatAccount`: 계정 1·2 모두 기록 후 `clear(MemberId(1))`하면 1은 빈 집합, 2는 유지.
  - `markRead_storesUnderAliasKey_withoutRawId`: `markRead(MemberId(123456), 1L)` 뒤 저장소의 모든 키 이름에 `"123456"`이 들어 있지 않고 `"notice_read_"`로 시작한다(별칭은 스텁값).
  - `readNoticeIds_aliasFails_isEmptySet`: `alias`가 예외를 던지면 `readNoticeIds`가 빈 집합을 낸다.
  - `markRead_aliasFails_doesNotThrow`: `alias`가 예외를 던져도 `markRead`/`clear`가 예외 없이 끝난다.

- [ ] **Step 2: Repository 테스트를 바꾼다.** 생성자에 `userInfo: UserInfoLocalDataSource = mockk()`를 추가하고, 계정은 `MutableStateFlow<MyAccountVO?>`로 스텁한다(기존 방식대로 Repository는 getter로 만든다). 읽음 저장소는 `readLocal.readNoticeIds(MemberId(1))`/`(MemberId(2))`를 각각 `flowOf`로 스텁한다.
  - 기존 `readNoticeIds_exposesReadStorageFlow` → `readNoticeIds_withAccount_exposesThatAccountsIds`.
  - `readNoticeIds_withoutAccount_isEmptySet`: 계정이 `null`이면 빈 집합.
  - `readNoticeIds_accountSwitch_readsNewAccountKey`: 계정 1 → 2로 바꾸면 방출이 `setOf(1L)` → `setOf(9L)` 순서. 계정 `MutableStateFlow`의 값을 바꿔 가며 `take(2).toList()`로 모은다.
  - `readNoticeIds_signOutThenSameAccount_restoresRecords`: 1 → `null` → 1이면 `setOf(1L)`, `emptySet()`, `setOf(1L)` 순서.
  - `markRead_withAccount_delegatesWithMemberId`: 계정 1일 때 `readLocal.markRead(MemberId(1), 3L)`을 한 번 호출.
  - `markRead_withoutAccount_writesNothing`: 계정이 `null`이면 `readLocal.markRead`를 부르지 않는다.
  - `clearReadNotices_clearsCurrentAccountOnly`: 계정 1이면 `readLocal.clear(MemberId(1))`만 호출. 계정이 `null`이면 호출하지 않는다.
  - 기존 `clearNotices_clearsCacheButNotReadIds`는 `readLocal.clear(any())`를 부르지 않는다는 검증으로 바꾼다(주석의 "기기 단위"는 "로그아웃은 읽음 기록을 지우지 않는다"로).

- [ ] **Step 3: 실패를 확인한다.** `./gradlew :data:testDebugUnitTest --tests "*NoticeReadLocalDataSourceImplTest" --tests "*NoticeRepositoryImplTest"` → 컴파일 실패.

- [ ] **Step 4: 구현한다.**
  - `NoticeReadLocalDataSource`: 위 시그니처로 바꾸고 KDoc의 "기기 단위" 서술을 계정 단위·별칭 키로 고친다.
  - `NoticeReadLocalDataSourceImpl`: `private fun keyOrNull(memberId): Preferences.Key<Set<String>>?`가 `runCatching { accountKeyAlias.alias("notice-read", memberId) }`로 별칭을 만들고 실패하면 `null`을 돌려준다. 읽기는 키가 `null`이면 `flowOf(emptySet())`, 쓰기·`clear`는 `null`이면 아무것도 하지 않는다. 기존의 숫자 아닌 값 건너뛰기와 `distinctUntilChanged()`는 유지한다. `keyOrNull`에는 왜 예외를 삼키는지(읽음 기록은 편의 기능이라 Keystore 실패가 화면을 막으면 안 된다)만 주석으로 남긴다.
  - `NoticeRepository`: `suspend fun clearReadNotices()`를 추가하고 `readNoticeIds`/`clearNotices` KDoc의 "기기 단위"·"로그아웃해도 유지"를 계정 단위 서술로 바꾼다.
  - `NoticeRepositoryImpl`: 생성자에 `userInfoLocalDataSource`를 추가한다. `readNoticeIds`는 `myAccount.map { it?.memberId }.distinctUntilChanged().flatMapLatest { id -> id?.let(noticeReadLocalDataSource::readNoticeIds) ?: flowOf(emptySet()) }`(`@OptIn(ExperimentalCoroutinesApi::class)`). `markRead`/`clearReadNotices`는 `userInfoLocalDataSource.myAccount.first()?.memberId`가 `null`이면 그대로 끝낸다. Hilt는 `@Inject` 생성자라 모듈 변경이 없다.

- [ ] **Step 5: 통과를 확인한다.** Step 3과 같은 명령 → PASS. 이어서 `./gradlew :data:testDebugUnitTest :domain:test` 전체와 `./gradlew :data:ktlintCheck :domain:ktlintCheck`.

- [ ] **Step 6: 커밋한다.**

```bash
git add data domain
git commit -m "feat: store notice read ids per account behind an alias key"
```

---

### Task 3: 탈퇴 시 해당 계정 읽음 기록 삭제

**Files:**
- Modify: `domain/.../usecase/member/WithdrawUseCase.kt`, `domain/.../usecase/auth/LogoutUseCase.kt`(KDoc만)
- Test: `domain/src/test/.../usecase/member/WithdrawUseCaseTest.kt`, `domain/src/test/.../usecase/auth/LogoutUseCaseTest.kt`

**Interfaces:**
- Consumes: Task 2의 `NoticeRepository.clearReadNotices()`.
- Produces: `WithdrawUseCase(memberRepository, noticeRepository, logout)` — 생성자에 `NoticeRepository`가 `memberRepository` 다음 인자로 들어간다(Hilt 주입이라 호출부 변경 없음).

- [ ] **Step 1: 테스트를 쓴다.** `WithdrawUseCaseTest`의 `WithdrawUseCase(...)` 생성에 이미 있는 `noticeRepository` mock을 넘기고 아래를 추가한다.
  - `invoke_serverAccepts_clearsReadNoticesBeforeLogout`: `coVerifyOrder { noticeRepository.clearReadNotices(); authRepository.logout() }`.
  - `invoke_clearReadNoticesFails_stillLogsOut`: `coEvery { noticeRepository.clearReadNotices() } throws IllegalStateException()`여도 결과가 성공이고 `authRepository.logout()`·`memberRepository.clearMyAccount()`가 한 번씩 불린다.
  - 기존 `invoke_serverRejects_leavesTheDeviceSignedIn`에 `coVerify(exactly = 0) { noticeRepository.clearReadNotices() }`를 더한다.
  - `LogoutUseCaseTest`에 `invoke_doesNotClearReadNotices`: 로그아웃은 `noticeRepository.clearReadNotices()`를 부르지 않는다(그 파일의 기존 설정을 그대로 쓴다).

- [ ] **Step 2: 실패를 확인한다.** `./gradlew :domain:test --tests "*WithdrawUseCaseTest" --tests "*LogoutUseCaseTest"` → `WithdrawUseCase` 생성자 불일치로 컴파일 실패.

- [ ] **Step 3: 구현한다.** `WithdrawUseCase`에 `noticeRepository: NoticeRepository`를 받고, 서버 탈퇴 성공 분기에서 `logout()` 앞에 `runSuspendCatching { noticeRepository.clearReadNotices() }`를 호출한다(`com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching`, 이미 `LogoutUseCase`가 쓰는 것). KDoc에는 두 가지만 덧붙인다: 읽음 기록은 `logout()`이 `myAccount`를 비우기 전에 지워야 한다는 순서 이유, 그리고 로그아웃은 같은 계정의 재로그인을 위해 읽음 기록을 일부러 지우지 않는다는 것. `LogoutUseCase` KDoc의 "무엇을 지우는가가 한 곳" 문단에는 읽음 기록이 의도적 예외라는 한 문장을 단다.

- [ ] **Step 4: 통과를 확인한다.** Step 2와 같은 명령 → PASS, 이어서 `./gradlew :domain:test :domain:ktlintCheck`.

- [ ] **Step 5: 커밋한다.**

```bash
git add domain
git commit -m "feat: clear the account's notice read ids on withdrawal"
```

---

### Task 4: 문서 갱신

**Files:**
- Modify: `docs/superpowers/specs/2026-11-11-notice-server-integration-design.md`, `docs/superpowers/specs/2026-11-16-notice-read-per-account-design.md`, `docs/status.md`, `docs/log.md`, `docs/synthesis/open-questions.md`, `docs/superpowers/specs/README.md`

- [ ] **Step 1: 공지 서버 연동 스펙을 고친다.** "제외"의 "계정별 읽음 분리"를 빼고, `NoticeReadLocalDataSource` 설명("기기 단위라 로그아웃해도 지우지 않는다")을 계정 단위·별칭 키·탈퇴 시 삭제로, 결정 문장 "읽음은 기기 단위로 유지한다"를 이번 결정(계정별 별칭, 이유: 로그아웃 때 지우면 재로그인에 NEW가 다시 뜨고 안 지우면 다른 계정이 물려받음)으로 바꾼다. 테스트 절의 `NoticeReadLocalDataSourceImpl` 설명도 계정 인자에 맞춘다. 이 스펙의 frontmatter `verified`를 갱신한다.

- [ ] **Step 2: 새 스펙을 맞춘다.** 동작 절의 Keystore 오류 문구와 `myAccount == null` 쓰기 문구에서 "로그를 남긴다"를 "조용히 건너뛴다"로 바꾼다(`data`에 로깅 도구가 없다). `status`를 `in-progress`로 바꾸고 README 인덱스 행의 상태도 맞춘다.

- [ ] **Step 3: 현재 상태 문서를 갱신한다.** `docs/status.md` 공지 절 NEW 규칙의 "기기 단위"와 "읽은 id는 지우지 않는다" 두 문장을 새 동작(계정별 별칭 키 `notice_read_<alias>`, 로그아웃·세션 종료는 유지, 탈퇴만 삭제)으로 바꾼다. `docs/log.md` 맨 위에 `## [2026-11-16] change | 공지 읽음 기록을 계정별 별칭 키로 저장` 항목을 기존 항목 형식으로 추가한다. `docs/synthesis/open-questions.md`의 OQ-P-397에 "공지 읽음 기록이 `AccountKeyAlias`(scope 분리)로 같은 문제를 풀었다. 이 항목도 같은 방식으로 해소할 수 있다"는 한 문단을 덧붙인다.

- [ ] **Step 4: 링크를 검사한다.** `python3 docs/script/check_links.py docs` → 깨진 링크 0건.

- [ ] **Step 5: 커밋한다.**

```bash
git add docs
git commit -m "docs: record per-account notice read state"
```

---

## Final Verification

- [ ] `./gradlew :data:testDebugUnitTest :domain:test :data:ktlintCheck :domain:ktlintCheck` 통과.
- [ ] `./gradlew :data:connectedDebugAndroidTest --tests "*AccountKeyAliasTest"`(API 28 에뮬레이터) 통과.
- [ ] 앱 모듈이 Hilt 그래프까지 컴파일되는지 `./gradlew :app:compileDebugKotlin` 확인.
- [ ] 푸시한 뒤 PR을 JeonK1 계정으로 연다(`gh auth status`로 계정을 게시 직전에 다시 확인). 본문에 이슈 #714를 연결하고 `PastCanvasAlert`(OQ-P-397)는 별도 이슈로 남긴다는 점을 적는다.
