---
id: force-update
title: 강제 업데이트 구현 계획
status: draft
type: work-order
created: 2026-11-24
updated: 2026-11-24
platforms: android
owner: android
related_adr:
related_spec: force-update
related_code:
  - SplashViewModel.kt#SplashViewModel
  - SplashRoute.kt#SplashRoute
  - YGModalPopup.kt#YGModalPopup
archived_reason:
tags: [plan, parfait, splash, force-update]
---

# 강제 업데이트 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** 스플래시에서 서버 버전 체크가 `FORCE`면 업데이트 모달로 진행을 막고, [종료]는 앱 종료·[업데이트]는 스토어 이동을 한다.

**Architecture:** data가 `GET api/v1/app-versions/check`를 불러 `updateType` 문자열을 도메인 enum으로 옮기고, `CheckForceUpdateUseCase`가 `Required`/`NotRequired`와 fail-open을 한 곳에서 판정한다. `SplashViewModel`이 부트스트랩·버전 체크(3초 상한)·로띠 셋을 기다려 모달 상태를 켜거나 기존대로 이동한다. 모달은 `core:ui`의 `YGModalPopup` 래퍼다.

**Tech Stack:** Kotlin, Retrofit + kotlinx.serialization, Hilt, Jetpack Compose, coroutines, JUnit(kotlin.test) + MockK + Turbine.

**Spec:** [2026-11-24-force-update-design.md](../specs/2026-11-24-force-update-design.md) — 실행자는 스펙과 이 계획을 함께 읽는다.

## Global Constraints

- 브랜치 `feature/force-update`(develop 기준, 이미 생성). **push·PR은 사용자 요청 전까지 하지 않는다.**
- 커밋 메시지: `type: 한국어 설명`(예: `feat: 강제 업데이트 도메인 판정 추가`). **`Co-Authored-By` 줄을 넣지 않는다.** 서브에이전트 커밋도 같다.
- Kotlin 주석·KDoc은 [docs/code-conventions.md](../../code-conventions.md)를 따른다 — 코드가 이미 말하는 것은 쓰지 않는다.
- feature impl의 UI가 아닌 타입(데이터 클래스·enum·상수)은 `impl/model` 패키지, 한 파일에 한 타입.
- 서버 계약: `GET /api/v1/app-versions/check?platform=ANDROID&version=x.y.z`, 인증 없음, 응답 `ApiResponse<{updateType, minimumVersion, latestVersion, storeUrl?}>`.
- `updateType`은 **`String`으로 역직렬화**한다. 모르는 값은 `UNKNOWN`.
- 버전 체크 실패(네트워크·400·404·500)와 3초 초과는 **전부 통과(`NotRequired`)**.
- 앱 버전은 `com.teamyg.parfait.core.util.android.APP_VERSION_NAME`.
- 모달 문구: 제목 `업데이트가 필요해요`, 본문 `서비스를 계속 이용하려면\n최신 버전으로 업데이트해 주세요`, 버튼 `종료`(secondary)·`업데이트`(primary).
- 스토어 열기 실패 문구: `스토어를 열 수 없어요. Play 스토어에서 파르페를 업데이트해 주세요`.
- 각 Task 끝에 `./gradlew ktlintCheck`가 통과해야 한다.

## Review Focus

1. **서버가 `updateType`에 새 값을 보낼 때** — 역직렬화가 깨지지 않고 통과해야 한다. → Task 2 `checkVersion_unknownUpdateType_mapsToUnknown`.
2. **버전 체크가 응답 없이 매달릴 때** — 3초 뒤 기존 목적지로 이동해야 한다. → Task 4 `versionCheckTimesOut_navigatesAsNotRequired`.
3. **서버 `storeUrl`이 Play가 아닌 주소일 때** — 무시하고 `market://`로 가야 한다. → Task 3 `storeLinkCandidates_nonPlayHttps_isIgnored`.
4. **버전 체크가 로띠보다 늦게 올 때** — 응답 전엔 이동·모달 모두 없어야 한다. → Task 4 `versionCheckPending_afterBootstrapAndAnimation_staysOnSplash`.
5. **`FORCE`에서 로띠가 끝나기 전** — 모달이 로띠를 끊고 먼저 뜨면 안 된다. → Task 4 `forceUpdate_beforeAnimationFinishes_hidesModal`.

---

### Task 1: 도메인 — 강제 업데이트 판정

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/appversion/AppUpdateType.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/appversion/AppVersionInfo.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/appversion/AppUpdateCheck.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/repository/appversion/AppVersionRepository.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/usecase/appversion/CheckForceUpdateUseCase.kt`
- Test: `domain/src/test/java/com/teamyg/parfait/domain/usecase/appversion/CheckForceUpdateUseCaseTest.kt`

**Interfaces:**
- Produces:
  - `enum class AppUpdateType { FORCE, OPTIONAL, NONE, UNKNOWN }`
  - `data class AppVersionInfo(val updateType: AppUpdateType, val storeUrl: String?)`
  - `sealed interface AppUpdateCheck { data class Required(val storeUrl: String?) : AppUpdateCheck; data object NotRequired : AppUpdateCheck }`
  - `interface AppVersionRepository { suspend fun checkVersion(version: String): Result<AppVersionInfo> }`
  - `class CheckForceUpdateUseCase @Inject constructor(repository: AppVersionRepository) { suspend operator fun invoke(version: String): AppUpdateCheck }`

- [ ] **Step 1: 실패하는 테스트 작성** — `CheckForceUpdateUseCaseTest`, `repository: AppVersionRepository = mockk()`.

```kotlin
@Test fun force_returnsRequiredWithStoreUrl()        // FORCE, "market://details?id=x" → Required("market://details?id=x")
@Test fun force_nullStoreUrl_returnsRequiredWithNull() // FORCE, null → Required(null)
@Test fun optional_returnsNotRequired()               // OPTIONAL → NotRequired
@Test fun none_returnsNotRequired()                   // NONE → NotRequired
@Test fun unknown_returnsNotRequired()                // UNKNOWN → NotRequired
@Test fun failure_returnsNotRequired()                // Result.failure(AppError.Network(IOException())) → NotRequired
@Test fun passesVersionToRepository()                 // invoke("1.4.0") → coVerify(exactly = 1) { repository.checkVersion("1.4.0") }
```

Given/When/Then 주석은 기존 `GetNoticeItemsUseCaseTest` 형식을 따른다. `AppError.Network` 생성자는 `domain/model/error`에서 확인한다.

- [ ] **Step 2: 실패 확인** — `./gradlew :domain:test --tests "*CheckForceUpdateUseCaseTest"` → 컴파일 실패(`Unresolved reference`).

- [ ] **Step 3: 모델·리포지토리 인터페이스·유스케이스 구현** — 위 Interfaces 그대로. 유스케이스는 `checkVersion(version).getOrNull()`이 `FORCE`일 때만 `Required(storeUrl)`, 그 외 전부 `NotRequired`. `AppUpdateType`에 `OPTIONAL`을 남기는 이유(선택 업데이트 UI 대비)는 KDoc 한 줄.

- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS 7건.

- [ ] **Step 5: 커밋** — `feat: 강제 업데이트 도메인 판정 추가`

---

### Task 2: data — 버전 체크 API 배선

**Files:**
- Create: `data/src/main/java/com/teamyg/parfait/data/service/AppVersionService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/appversion/AppVersionCheckResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/appversion/mapper/VOMapper.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/appversion/remote/AppVersionRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/appversion/remote/AppVersionRemoteDataSourceImpl.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/repository/appversion/AppVersionRepositoryImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`, `RemoteDataSourceModule.kt`, `RepositoryModule.kt` — 기존 Notice 바인딩 옆에 하나씩
- Test: `data/src/test/java/com/teamyg/parfait/data/source/appversion/remote/AppVersionRemoteDataSourceImplTest.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/repository/appversion/AppVersionRepositoryImplTest.kt`

**Interfaces:**
- Consumes: Task 1의 `AppVersionInfo`, `AppUpdateType`, `AppVersionRepository`.
- Produces:
  - `interface AppVersionService { @GET("api/v1/app-versions/check") suspend fun checkVersion(@Query("platform") platform: String, @Query("version") version: String): ApiResponse<AppVersionCheckResponse> }`
  - `@Serializable data class AppVersionCheckResponse(updateType: String, minimumVersion: String, latestVersion: String, storeUrl: String? = null)` — 필드마다 `@SerialName`
  - `internal fun AppVersionCheckResponse.toAppVersionInfo(): AppVersionInfo`
  - `interface AppVersionRemoteDataSource { suspend fun checkVersion(version: String): Result<AppVersionInfo> }`
  - `class AppVersionRepositoryImpl @Inject constructor(remote: AppVersionRemoteDataSource) : AppVersionRepository`

- [ ] **Step 1: 실패하는 테스트 작성** — 매퍼는 단독 테스트하지 않고 데이터 소스 테스트로 잠근다(기존 규약). 구성은 `NoticeRemoteDataSourceImplTest`와 같다(`ApiCaller(json = Json { ignoreUnknownKeys = true })`).

`AppVersionRemoteDataSourceImplTest`:
```kotlin
@Test fun checkVersion_force_mapsToForceWithStoreUrl()   // "FORCE", storeUrl "market://details?id=x" → AppVersionInfo(FORCE, "market://details?id=x")
@Test fun checkVersion_optionalAndNone_mapToEnum()        // "OPTIONAL" → OPTIONAL, "NONE" → NONE
@Test fun checkVersion_unknownUpdateType_mapsToUnknown()  // "SOFT" → UNKNOWN
@Test fun checkVersion_always_requestsAndroidWithVersion() // checkVersion("1.4.0") → coVerify(exactly = 1) { service.checkVersion("ANDROID", "1.4.0") }
@Test fun checkVersion_businessFailure_returnsFailure()    // success=false, code "INVALID_VERSION_FORMAT" → assertIs<ApiException.Business>
@Test fun checkVersion_http404_returnsFailure()            // throws HttpException(Response.error<Unit>(404, "".toResponseBody(null))) → isFailure
@Test fun checkVersion_http500_returnsFailure()            // 500 같은 방식 → isFailure
@Test fun checkVersion_ioException_returnsFailure()        // throws IOException() → assertIs<ApiException.Network>
```

`AppVersionRepositoryImplTest`:
```kotlin
@Test fun checkVersion_success_passesInfoThrough()    // remote 성공 그대로
@Test fun checkVersion_failure_mapsToAppError()       // remote Result.failure(ApiException.Network(IOException())) → assertIs<AppError>(exceptionOrNull())
```

- [ ] **Step 2: 실패 확인** — `./gradlew :data:testDebugUnitTest --tests "*AppVersion*"` → 컴파일 실패.

- [ ] **Step 3: 구현** — `NoticeService`·`NoticeRemoteDataSourceImpl`·`NoticeRepositoryImpl`과 같은 형태. `platform`은 `private const val PLATFORM = "ANDROID"`. 리포지토리는 `.mapErrorToAppError()`만, 캐시 없음. 매퍼의 문자열 → enum은 `AppUpdateType.entries.firstOrNull { it.name == updateType } ?: UNKNOWN`처럼 `UNKNOWN` 문자열 자체가 와도 `UNKNOWN`이 되게 한다. DTO KDoc에 "`updateType`을 enum이 아닌 문자열로 받는 이유" 한 줄.

- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS 10건. `./gradlew :app:assembleDebug`로 Hilt 그래프 확인.

- [ ] **Step 5: 커밋** — `feat: 앱 버전 체크 API 연동`

---

### Task 3: 강제 업데이트 모달과 스토어 링크 후보

**Files:**
- Create: `core/ui/src/main/java/com/teamyg/parfait/core/ui/component/modal/YGModalForceUpdate.kt`
- Modify: `core/ui/src/main/res/values/strings.xml` — `yg_modal_force_update_*` 4개(제목·본문·종료·업데이트)
- Create: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/model/StoreLinkCandidates.kt`
- Test: `feature/intro/impl/src/test/java/com/teamyg/parfait/feature/intro/impl/model/StoreLinkCandidatesTest.kt`

**Interfaces:**
- Produces:
  - `@Composable fun YGModalForceUpdate(onQuit: () -> Unit, onUpdate: () -> Unit)`
  - `internal fun storeLinkCandidates(storeUrl: String?, packageName: String): List<String>` — 시도 순서대로

- [ ] **Step 1: 실패하는 테스트 작성** — `StoreLinkCandidatesTest`, `packageName = "com.teamyg.parfait"`.

```kotlin
val market = "market://details?id=com.teamyg.parfait"
val web = "https://play.google.com/store/apps/details?id=com.teamyg.parfait"

@Test fun storeLinkCandidates_nullStoreUrl_returnsMarketThenWeb()     // [market, web]
@Test fun storeLinkCandidates_marketStoreUrl_comesFirst()              // "market://details?id=other" → ["market://details?id=other", market, web]
@Test fun storeLinkCandidates_playHttpsStoreUrl_comesFirst()           // "https://play.google.com/store/apps/details?id=other" → [그 값, market, web]
@Test fun storeLinkCandidates_nonPlayHttps_isIgnored()                 // "https://example.com/app.apk" → [market, web]
@Test fun storeLinkCandidates_httpPlayHost_isIgnored()                 // "http://play.google.com/..." → [market, web]
@Test fun storeLinkCandidates_malformed_isIgnored()                    // "not a url", "" → [market, web]
@Test fun storeLinkCandidates_storeUrlEqualsDefault_notDuplicated()    // storeUrl == market → [market, web]
```

- [ ] **Step 2: 실패 확인** — `./gradlew :feature:intro:impl:testDebugUnitTest --tests "*StoreLinkCandidatesTest"` → 컴파일 실패.

- [ ] **Step 3: `storeLinkCandidates` 구현** — `java.net.URI`로 파싱(JVM 테스트라 `android.net.Uri` 금지), 파싱 예외는 무시. 허용은 scheme `market`, 또는 scheme `https` + host `play.google.com`. 결과는 `distinct()`.

- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS 7건.

- [ ] **Step 5: `YGModalForceUpdate` 구현** — `YGModalQuit`과 같은 형태로 `YGModalPopup` 호출: `iconRes = DesignSystemR.drawable.ic_warning_round`, `secondaryText = 종료`/`onSecondaryClick = onQuit`, `primaryText = 업데이트`/`onPrimaryClick = onUpdate`, `onDismissRequest = {}`, `properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`. `@YGPreview` 프리뷰 1개. 문자열은 Global Constraints 값 그대로.

- [ ] **Step 6: 빌드 확인** — `./gradlew :core:ui:compileDebugKotlin ktlintCheck` → BUILD SUCCESSFUL.

- [ ] **Step 7: 커밋** — `feat: 강제 업데이트 모달과 스토어 링크 후보 추가`

---

### Task 4: 스플래시 판정

**Files:**
- Modify: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/splash/SplashViewModel.kt`
- Create: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/model/VersionCheckTimeout.kt`
- Modify: `feature/intro/impl/src/test/java/com/teamyg/parfait/feature/intro/impl/splash/SplashViewModelTest.kt`

**Interfaces:**
- Consumes: Task 1 `CheckForceUpdateUseCase`, `AppUpdateCheck`.
- Produces:
  - `internal val VERSION_CHECK_TIMEOUT: Duration = 3.seconds`
  - `SplashState(destination: SessionBootstrap? = null, isAnimationFinished: Boolean = false, updateCheck: AppUpdateCheck? = null)` + 파생 `val isForceUpdateVisible: Boolean` (= 세 값이 다 있고 `updateCheck is Required`)
  - `SplashIntent.UpdateClick`, `SplashIntent.QuitClick` (data object)
  - `SplashSideEffect.OpenStore(val storeUrl: String?)`, `SplashSideEffect.QuitApp`
  - 생성자 `SplashViewModel(bootstrapSession: BootstrapSessionUseCase, checkForceUpdate: CheckForceUpdateUseCase)`

- [ ] **Step 1: 실패하는 테스트 작성** — 기존 테스트의 `viewModel()`을 두 유스케이스로 바꾸고, 기존 케이스에는 `coEvery { checkForceUpdate(any()) } returns AppUpdateCheck.NotRequired`를 깔아 **기존 6건을 회귀로 유지**한다. 추가:

```kotlin
@Test fun forceUpdate_afterAllSignals_showsModalWithoutNavigation()
    // Required("market://x") + ToGroupList + AnimationFinished → expectNoEvents(), state.isForceUpdateVisible == true
@Test fun forceUpdate_beforeAnimationFinishes_hidesModal()
    // Required + 부트스트랩 완료, 로띠 미종료 → isForceUpdateVisible == false
@Test fun versionCheckPending_afterBootstrapAndAnimation_staysOnSplash()
    // checkForceUpdate가 CompletableDeferred로 대기 → AnimationFinished 후 runCurrent → expectNoEvents()
    // 이어서 NotRequired 완료 → NavigateToGroupList
@Test fun versionCheckTimesOut_navigatesAsNotRequired()
    // checkForceUpdate coAnswers { awaitCancellation() } → AnimationFinished → advanceTimeBy(3_001) → NavigateToGroupList
@Test fun versionCheck_receivesAppVersionName()
    // coVerify(exactly = 1) { checkForceUpdate(APP_VERSION_NAME) }
@Test fun updateClick_emitsOpenStoreWithStoreUrl()
    // Required("market://x") 상태에서 UpdateClick → OpenStore("market://x")
@Test fun quitClick_emitsQuitApp()
    // QuitClick → QuitApp
```

- [ ] **Step 2: 실패 확인** — `./gradlew :feature:intro:impl:testDebugUnitTest --tests "*SplashViewModelTest"` → 컴파일 실패.

- [ ] **Step 3: 구현** — `handleInit`에서 기존 `KEY_BOOTSTRAP` 작업 옆에 `KEY_VERSION_CHECK` 작업을 따로 `launch`: `withTimeoutOrNull(VERSION_CHECK_TIMEOUT) { checkForceUpdate(APP_VERSION_NAME) } ?: AppUpdateCheck.NotRequired`를 상태에 넣고 `navigateIfReady()`. `navigateIfReady`는 `updateCheck`까지 셋이 다 있을 때만 판단하고, `Required`면 이펙트 없이 끝낸다(모달은 `isForceUpdateVisible`이 그린다). `UpdateClick`은 현재 `updateCheck`가 `Required`일 때만 `OpenStore(storeUrl)`을 낸다. 클래스 KDoc의 "두 가지"를 "세 가지"로 고치고, 부트스트랩 부수효과를 강제 업데이트 중에도 막지 않는다는 점을 `handleInit` KDoc에 한 줄.

- [ ] **Step 4: 통과 확인** — 같은 명령 → 기존 6건 + 신규 7건 PASS.

- [ ] **Step 5: 커밋** — `feat: 스플래시에서 강제 업데이트 판정`

---

### Task 5: 스플래시 화면 연결

**Files:**
- Modify: `feature/intro/impl/src/main/java/com/teamyg/parfait/feature/intro/impl/splash/SplashRoute.kt`
- Modify: `feature/intro/impl/src/main/res/values/strings.xml` — `splash_store_open_failed`

**Interfaces:**
- Consumes: Task 3 `YGModalForceUpdate`, `storeLinkCandidates`; Task 4 `SplashState.isForceUpdateVisible`, `SplashIntent.UpdateClick/QuitClick`, `SplashSideEffect.OpenStore/QuitApp`.

- [ ] **Step 1: 구현** — `state`를 `collectAsStateWithLifecycle()`로 받아 `isForceUpdateVisible`이면 `YGModalForceUpdate(onQuit = { QuitClick }, onUpdate = { UpdateClick })`. 이펙트 처리:
  - `QuitApp` → `LocalActivity.current?.finishAndRemoveTask()`(`CustomCameraRoute`와 같은 `androidx.activity.compose.LocalActivity`).
  - `OpenStore` → `storeLinkCandidates(effect.storeUrl, context.packageName)`을 순서대로 `Intent(ACTION_VIEW, uri)`로 `startActivity`, `ActivityNotFoundException`이면 다음 후보. 전부 실패하면 `android.widget.Toast.makeText(context, splash_store_open_failed, LENGTH_LONG)`. **`YGToastHost`를 쓰지 않는 이유**(모달 `Dialog` 창 아래에 그려져 딤에 가린다)를 주석 한 줄로 남긴다.

- [ ] **Step 2: 빌드 확인** — `./gradlew :feature:intro:impl:testDebugUnitTest :app:assembleDebug ktlintCheck` → BUILD SUCCESSFUL.

- [ ] **Step 3: 커밋** — `feat: 강제 업데이트 모달과 스토어 이동 연결`

---

### Task 6: 문서

**Files:**
- Create: `docs/api/app-version.md` — `docs/api/template.md` 형식. 서버 계약(`feat/#225` 기준, 미배포 명시) + Android 매핑 절: fail-open(404·500·네트워크·3초 초과), `updateType` 문자열 수신과 `UNKNOWN`, `storeUrl` 허용 출처, `OPTIONAL` 미사용.
- Modify: `docs/api/README.md` — 도메인 목록에 한 줄
- Modify: `docs/status.md` — 강제 업데이트 행(서버 미배포라 실서버 미확인)
- Modify: `docs/superpowers/specs/2026-11-24-force-update-design.md` — frontmatter `status: implemented`, `related_code`에 새 파일
- Modify: `docs/superpowers/plans/README.md` — 활성 카탈로그에 이 계획 한 줄

- [ ] **Step 1: 작성**
- [ ] **Step 2: 링크 확인** — `python3 docs/script/check_links.py docs` → 깨진 링크 0건.
- [ ] **Step 3: 커밋** — `docs: 강제 업데이트 API 계약과 상태 문서 추가`

---

### Task 7: 수동 확인 (사용자 수행)

자동화하지 않는다. 실행자는 아래 목록을 사용자에게 넘기고 결과를 `docs/log.md`에 한 줄 남긴다.

`FORCE`는 `TEAMYG-SERVER` `feat/#225`를 로컬로 띄우고 `app_version`에 ANDROID 행(`minimum_version`을 현재 `APP_VERSION_NAME`보다 높게)을 넣어 재현한다.

- [ ] 서버 미배포(404)·응답 지연(3초 초과)에서 정상 진입
- [ ] `FORCE`에서 모달 노출, 뒤로가기·바깥 탭에 닫히지 않음, 회전 후 유지
- [ ] [종료] → 앱 종료·최근 앱 목록에서 제거
- [ ] [업데이트] → 스토어, 돌아오면 모달 유지
- [ ] `store_url` NULL → `market://`. Play 스토어 없는 에뮬레이터 → 웹 폴백
- [ ] `FORCE` 상태에서 푸시 딥링크·앱 링크로 진입해도 해당 화면으로 가지 않음
