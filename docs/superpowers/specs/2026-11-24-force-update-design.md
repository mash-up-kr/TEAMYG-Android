---
id: force-update
title: 강제 업데이트 (Force Update)
status: implemented
category: feature
platforms: android
verified: 2026-11-24
related_code:
  - SplashViewModel.kt#SplashViewModel
  - SplashRoute.kt#SplashRoute
  - YGModalPopup.kt#YGModalPopup
  - AppInfo.kt#APP_VERSION_NAME
  - BootstrapSessionUseCase.kt#BootstrapSessionUseCase
  - AppVersionService.kt#AppVersionService
  - AppVersionRemoteDataSourceImpl.kt#AppVersionRemoteDataSourceImpl
  - AppVersionRepositoryImpl.kt#AppVersionRepositoryImpl
  - CheckForceUpdateUseCase.kt#CheckForceUpdateUseCase
  - StoreLinkCandidates.kt#storeLinkCandidates
  - YGModalForceUpdate.kt#YGModalForceUpdate
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, splash, force-update]
---

# Spec: 강제 업데이트

## 목적

서버가 정한 최소 버전보다 낮은 앱은 스플래시에서 막고 스토어로 보낸다. 기능정의서 v7 `A-001`의
"강제 업데이트 대상 → 스토어 이동, 딥링크 폐기"를 구현한다. 2026-10-20 회의에서 후속 우선순위 2위로
잡힌 항목이다.

해결할 문제(가정 — 위키에 근거 문서 없음): 서버 계약이 바뀌거나 치명적 버그가 있는 구버전이 남으면
그 사용자는 깨진 화면을 계속 본다. 지금은 구버전 사용자를 최신 버전으로 옮길 수단이 스토어 자동
업데이트뿐이다.

## 범위

### 목표

- 스플래시에서 서버 버전 체크 API를 부르고, 응답이 `FORCE`면 업데이트 모달로 진행을 막는다.
- 모달의 [종료]는 앱을 끝내고, [업데이트]는 스토어를 연다.

### 비목표

- **선택 업데이트(`OPTIONAL`) UI.** 시나리오가 정해지지 않았다. `OPTIONAL`은 `NONE`과 똑같이 통과한다.
- 공용 `YGModalPopup`의 시안 불일치 수정(아래 "시안 대조").
- 스플래시 밖(포그라운드 복귀 등)에서의 재검사.

### 실패로 보는 것

- 서버 장애·미배포·느린 네트워크 때문에 사용자가 앱에 못 들어가거나 스플래시에 오래 갇히는 것.
  그래서 **체크가 실패하거나 상한 시간을 넘기면 통과한다(fail-open).**
- 기능정의서 v7 `A-001`의 "네트워크 오류 시 재시도 안내"와 다르다 — 버전 체크에 한해 재시도 안내 대신
  통과를 택한 것은 사용자 결정이다. 부트스트랩의 오류 처리는 이 스펙이 바꾸지 않는다.

## 제약 · 의존성

- **서버 미배포.** 서버 구현은 `TEAMYG-SERVER` `feat/#225`에 있고 아직 배포 전이다. 앱이 먼저 나가면
  엔드포인트가 404를 준다 — fail-open이라 사용자는 영향이 없다.
- **운영 데이터.** 플랫폼 행이 없으면 서버가 `APP_VERSION_NOT_FOUND`(500)를 주고, fail-open이라 기능이
  아무 일도 하지 않는다. **배포 시 서버팀이 ANDROID 행(`minimum_version`·`latest_version`·`store_url`)을
  DB에 넣기로 했다.**
- 서버 계약(`feat/#225` 기준):
  - `GET /api/v1/app-versions/check?platform=ANDROID&version=x.y.z`, **인증 없음**(화이트리스트).
  - 응답 `ApiResponse<AppVersionCheckResponse>` — `updateType`(`FORCE`·`OPTIONAL`·`NONE`),
    `minimumVersion`, `latestVersion`, `storeUrl`(**nullable**).
  - 에러: 형식 오류 `INVALID_VERSION_FORMAT`(400), 플랫폼 행 없음 `APP_VERSION_NOT_FOUND`(**500**).
- 앱 버전은 `APP_VERSION_NAME`(`core/util/android`)에서 읽는다. 지금 `versionNameSuffix`가 없어
  `"1.4.0"` 형식 그대로다.

## 미결

| 항목 | 결정 주체 | 이번 구현을 막는가 |
|---|---|---|
| `minimumVersion`을 언제 누가 올리는가 | 미정 | 아니오 — 값은 서버 DB에 있다 |
| 선택 업데이트(`OPTIONAL`) UI 시나리오 | 미정(기획·디자인) | 아니오 — 이번엔 통과 처리 |

추적: [open-questions.md](../../synthesis/open-questions.md) `OQ-P-464`.

## 접근

| 방식 | 내용 | 판단 |
|---|---|---|
| **스플래시 안의 다이얼로그** | `SplashViewModel`이 판정하고 `SplashRoute`가 모달을 띄운다 | **채택.** 시안이 스플래시 로고 위에 모달을 겹친 화면이고, 스플래시에 머무르는 것만으로 딥링크 폐기가 성립한다 |
| 별도 화면(`NavKey`) | 판정 뒤 업데이트 전용 화면으로 `replaceAll` | 시안의 겹침을 재현하려면 스플래시를 다시 그려야 하고, 딥링크 대기 조건(`!= NavKeySplash`)이 풀려 폐기 처리를 따로 넣어야 한다 |
| Remote Config / In-App Updates | 서버 없이 판정 | 서버 API가 이미 만들어졌다 |

## 설계

### 데이터 · 도메인

판정은 **유스케이스 한 곳**에서 한다. data 계층은 문자열을 도메인 enum으로 옮기기만 한다.

- `data`
  - `AppVersionService` — `@GET("api/v1/app-versions/check")`, 쿼리 `platform`·`version`.
  - `AppVersionCheckResponse` — `updateType`을 **`String`으로 받는다.** 서버가 값을 늘려도 역직렬화가
    깨지지 않게 한다.
  - 매퍼 — 문자열 → `AppUpdateType`. 모르는 값은 `UNKNOWN`.
  - `AppVersionRemoteDataSource(Impl)` — `ApiCaller.safeApiCall`, `platform = "ANDROID"` 상수
    (`NoticeRemoteDataSourceImpl`과 같은 형태).
  - `AppVersionRepositoryImpl` — `Result`를 그대로 넘기고 `mapErrorToAppError()`. 캐시 없음.
- `domain`
  - `AppUpdateType { FORCE, OPTIONAL, NONE, UNKNOWN }` — `OPTIONAL`을 남겨 두는 이유는 선택 업데이트
    UI가 생길 때 data 계층을 다시 건드리지 않기 위해서다.
  - `AppVersionInfo(updateType: AppUpdateType, storeUrl: String?)` — `minimumVersion`·`latestVersion`은
    앱이 쓰지 않으므로 올리지 않는다.
  - `AppVersionRepository.checkVersion(version: String): Result<AppVersionInfo>`.
  - `CheckForceUpdateUseCase(version: String): AppUpdateCheck`
    - `sealed interface AppUpdateCheck { data class Required(val storeUrl: String?); data object NotRequired }`
    - `FORCE` → `Required(storeUrl)`
    - `OPTIONAL`·`NONE`·`UNKNOWN` → `NotRequired`
    - **`Result` 실패(네트워크·400·404·500 전부) → `NotRequired`**
  - 버전 문자열은 유스케이스가 인자로 받는다(도메인은 Android 의존 없음). 호출부가 `APP_VERSION_NAME`을 넘긴다.

### 스플래시

- `SplashState`에 판정 결과(`AppUpdateCheck?`, `null` = 아직 모름)를 더한다. 모달 노출은 이 값이
  `Required`이고 로띠가 끝났는지로 정한다.
- `Init`에서 부트스트랩과 버전 체크를 **동시에** 시작한다(별도 `launch` key).
- 버전 체크는 **`withTimeoutOrNull(3초)`** 로 감싼다. 상한을 넘기면 `NotRequired`다. 부트스트랩에는
  상한을 걸지 않는다(기존 동작 유지).
- 진행 조건: 부트스트랩 목적지, 버전 판정, 로띠 종료 **셋 다** 갖춰졌을 때.
  - `Required` → 이동 이펙트를 내지 않고 모달을 띄운다(이펙트가 아니라 상태 — 재구성·회전에도 남아야 한다).
  - `NotRequired` → 기존대로 `NavigateToLogin` / `NavigateToGroupList`.
- **부트스트랩의 부수효과는 강제 업데이트 중에도 허용한다.** 성공 시 푸시 토큰 등록
  (`registerCurrentDeviceToken`), 인증 거절 시 `logout()`이 그대로 일어난다. 업데이트 뒤에도 필요한
  동작이라 막지 않는다. 막힌 기기가 받는 푸시는 딥링크가 소비되지 않으므로 화면을 열지 않는다.
- 인텐트 `UpdateClick` → 이펙트 `OpenStore(storeUrl)`, `QuitClick` → 이펙트 `QuitApp`.
- `SplashRoute`가 이펙트를 처리한다.
  - `QuitApp` → `Activity.finishAndRemoveTask()`.
  - `OpenStore` → 아래 순서로 **처리할 앱이 있는 첫 후보**를 연다.
    1. `storeUrl` — **scheme이 `market`이거나 `https`일 때만.** Play가 아닌 `https`는 공지·멀티 링크 같은
       안내 페이지로 보내는 경우로, 외부 브라우저로 연다. `http`·`intent` 등 다른 스킴과 형식 오류는 무시한다.
       APK 직접 배포 링크는 넣지 않는다(Play 정책 — 사용자 확인: 직접 APK 배포 없음).
    2. `market://details?id=<packageName>`
    3. `https://play.google.com/store/apps/details?id=<packageName>`
    4. 모두 실패 → 토스트 "스토어를 열 수 없어요. Play 스토어에서 파르페를 업데이트해 주세요", 모달 유지.
  - 후보 목록을 만드는 로직은 순수 함수로 둔다(테스트 대상).
  - 스토어에서 업데이트 없이 돌아오면 모달이 그대로 남는다(상태가 유지되므로).

### 모달

- `core:ui`에 `YGModalForceUpdate(onQuit, onUpdate)` — `YGModalPopup` 래퍼, `YGModalQuit`과 같은 형태.
  - 아이콘 `ic_warning_round`, 제목 "업데이트가 필요해요",
    본문 "서비스를 계속 이용하려면\n최신 버전으로 업데이트해 주세요".
  - secondary **[종료]**, primary [업데이트]. 시안의 "취소"를 "종료"로 바꾼 것은 의도다(사용자 결정).
  - `onDismissRequest = {}` + `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`.
  - 접근성: `Dialog` 기본 동작대로 창이 열리면 포커스가 모달로 옮겨 가고 제목 → 본문 → [종료] →
    [업데이트] 순으로 읽는다. 뒤의 스플래시는 탐색되지 않는다. 뒤로가기가 막혀 있어 나가는 길은 [종료]다.
  - 문자열은 `core:ui` 리소스, 프리뷰 포함.

### 딥링크

추가 코드 없음. 푸시 딥링크(`MainRoute.kt`)와 앱 링크(`awaitAppLinkReady`) 모두 백스택 맨 위가
`NavKeySplash`가 아니게 될 때까지 기다린다. 강제 업데이트 중엔 스플래시를 떠나지 않으니 소비되지 않고,
종료와 함께 사라진다 — 정책의 "딥링크 폐기"와 같다.

## 시안 대조

Figma `A-001-Update`(node `7315:17518`)와 `YGModalPopup`을 대조했다. 아이콘·타이포·색·패딩·세로 간격은
일치한다. 아래 셋은 **공용 컴포넌트의 기존 차이**라 이번 범위에서 고치지 않는다(고치면 모든 모달이 바뀐다).

| 항목 | 시안 | 코드 |
|---|---|---|
| 버튼 사이 간격 | `gap-4` 12dp | `gap3` 8dp |
| 팝업 모서리 | 직각 | `radius.medium1` |
| 딤 | black 25% | `Dialog` 기본 |

## 테스트

### 단위

- `CheckForceUpdateUseCase`: `FORCE`→`Required`(storeUrl 전달), `OPTIONAL`·`NONE`·`UNKNOWN`→`NotRequired`,
  실패→`NotRequired`, 넘긴 버전 문자열이 리포지토리로 그대로 가는지.
- 매퍼: `"FORCE"`·`"OPTIONAL"`·`"NONE"` → 대응 값, 모르는 문자열 → `UNKNOWN`.
- `AppVersionRemoteDataSourceImpl`·`AppVersionRepositoryImpl`: 쿼리 `platform=ANDROID`·`version`,
  경로, 400/404/500 응답이 `Result` 실패가 되는지(선례 `NoticeRemoteDataSourceImplTest`·`NoticeRepositoryImplTest`).
- 스토어 후보 함수: `storeUrl` null / `market://` / `https://play.google.com` / 다른 https host / `intent`·`http` / 형식 오류.
- `SplashViewModel`
  - 세 신호의 도착 순서와 무관하게 셋 다 와야 진행.
  - `Required` → 이동 이펙트 없음 + 모달 상태. `NotRequired` → 기존 이동 케이스(회귀, 기존
    `SplashViewModelTest` 유지).
  - 버전 체크가 3초를 넘기면 `NotRequired`로 진행.
  - `APP_VERSION_NAME`이 유스케이스 인자로 넘어가는지.
  - `UpdateClick` → `OpenStore(storeUrl)`, `QuitClick` → `QuitApp`.

### 수동

`FORCE` 응답은 `feat/#225` 로컬 서버(ANDROID 행의 `minimum_version`을 현재보다 높게)로 재현한다.
서버 배포 뒤에는 같은 확인을 실서버에서 `minimum_version`을 잠시 올려 한 번 더 한다.

- 서버 미배포(404)·응답 지연(3초 초과)에서 정상 진입.
- `FORCE`에서 모달 노출, 뒤로가기·바깥 탭에 닫히지 않음, 회전 후 유지.
- [종료] → 앱 종료·최근 앱 목록에서 제거. [업데이트] → 스토어, 돌아오면 모달 유지.
- `storeUrl` null일 때 `market://`로 이동. Play 스토어 없는 기기(에뮬레이터)에서 웹 폴백.
- `FORCE` 상태에서 푸시 딥링크·앱 링크로 진입해도 해당 화면으로 가지 않음.

## 문서

- `docs/api/app-version.md` 신설 — 서버 계약 미러 + Android 매핑(fail-open, 3초 상한, `updateType` 문자열 수신).
- `docs/status.md` 갱신.

## 구현자 재량

- 파일 배치는 기존 `domain/model/<영역>`·`data/source/<영역>` 규칙을 따른다.
- 스토어 인텐트 처리 코드의 위치(`SplashRoute` 내부 또는 `core:util:android` 확장 함수).
- 3초 상한의 상수 위치.
