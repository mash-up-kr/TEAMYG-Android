---
id: force-update
title: 강제 업데이트 (Force Update)
status: draft
category: feature
platforms: android
verified: 2026-11-24
related_code:
  - SplashViewModel.kt#SplashViewModel
  - SplashRoute.kt#SplashRoute
  - YGModalPopup.kt#YGModalPopup
  - AppInfo.kt#APP_VERSION_NAME
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

## 범위

### 목표

- 스플래시에서 서버 버전 체크 API를 부르고, 응답이 `FORCE`면 업데이트 모달로 진행을 막는다.
- 모달의 [종료]는 앱을 끝내고, [업데이트]는 스토어를 연다.

### 비목표

- **선택 업데이트(`OPTIONAL`) UI.** 시나리오가 정해지지 않았다. `OPTIONAL`은 `NONE`과 똑같이 통과한다.
- 공용 `YGModalPopup`의 시안 불일치 수정(아래 "시안 대조").
- 스플래시 밖(포그라운드 복귀 등)에서의 재검사.

### 실패로 보는 것

- 서버 장애·미배포 때문에 사용자가 앱에 못 들어가는 것. 그래서 **체크가 실패하면 통과한다(fail-open).**

## 제약

- **서버 미배포.** 서버 구현은 `TEAMYG-SERVER` `feat/#225`에 있고 아직 배포 전이다. 앱이 먼저 나가면
  엔드포인트가 404를 준다 — fail-open이라 사용자는 영향이 없다.
- 서버 계약(`feat/#225` 기준):
  - `GET /api/v1/app-versions/check?platform=ANDROID&version=x.y.z`, **인증 없음**(화이트리스트).
  - 응답 `ApiResponse<AppVersionCheckResponse>` — `updateType`(`FORCE`·`OPTIONAL`·`NONE`),
    `minimumVersion`, `latestVersion`, `storeUrl`(**nullable**).
  - 에러: 형식 오류 `INVALID_VERSION_FORMAT`(400), 플랫폼 행 없음 `APP_VERSION_NOT_FOUND`(**500**).
- 앱 버전은 `APP_VERSION_NAME`(`core/util/android`)에서 읽는다. 지금 `versionNameSuffix`가 없어
  `"1.4.0"` 형식 그대로다.

## 접근

| 방식 | 내용 | 판단 |
|---|---|---|
| **스플래시 안의 다이얼로그** | `SplashViewModel`이 판정하고 `SplashRoute`가 모달을 띄운다 | **채택.** 시안이 스플래시 로고 위에 모달을 겹친 화면이고, 스플래시에 머무르는 것만으로 딥링크 폐기가 성립한다 |
| 별도 화면(`NavKey`) | 판정 뒤 업데이트 전용 화면으로 `replaceAll` | 시안의 겹침을 재현하려면 스플래시를 다시 그려야 하고, 딥링크 대기 조건(`!= NavKeySplash`)이 풀려 폐기 처리를 따로 넣어야 한다 |
| Remote Config / In-App Updates | 서버 없이 판정 | 서버 API가 이미 만들어졌고 iOS와 정책을 한 곳에서 관리하기로 했다 |

## 설계

### 데이터 · 도메인

- `data`
  - `AppVersionService` — `@GET("api/v1/app-versions/check")`, 쿼리 `platform`·`version`.
  - `AppVersionCheckResponse` — `updateType`을 **`String`으로 받는다.** 서버가 값을 늘려도 역직렬화가
    깨지지 않게 하고, 모르는 값은 매핑에서 `FORCE`가 아닌 것으로 본다.
  - `AppVersionRemoteDataSource(Impl)` — `ApiCaller.safeApiCall`, `platform = "ANDROID"` 상수
    (`NoticeRemoteDataSourceImpl`과 같은 형태).
  - `AppVersionRepositoryImpl` — `Result`를 그대로 넘기고 `mapErrorToAppError()`. 캐시 없음.
- `domain`
  - `AppVersionRepository.checkVersion(version: String): Result<AppVersionCheck>`.
  - `AppVersionCheck(isForceUpdate: Boolean, storeUrl: String?)` 수준의 모델. `minimumVersion`·
    `latestVersion`은 앱이 쓰지 않으므로 도메인으로 올리지 않는다.
  - `CheckForceUpdateUseCase` → `ForceUpdate` 판정 결과(예: `sealed interface AppUpdateCheck {
    Required(storeUrl: String?); NotRequired }`).
    - `FORCE` → `Required`
    - `OPTIONAL`·`NONE`·모르는 값 → `NotRequired`
    - **`Result` 실패(네트워크·4xx·5xx·404 전부) → `NotRequired`**
  - 버전 문자열은 유스케이스가 인자로 받는다(도메인은 Android 의존 없음). 호출부가 `APP_VERSION_NAME`을 넘긴다.

### 스플래시

- `SplashState`에 판정 결과(`AppUpdateCheck?`, `null` = 아직 모름)와 모달 노출 여부를 더한다.
- `Init`에서 부트스트랩과 버전 체크를 **동시에** 시작한다(별도 `launch` key). 부트스트랩은 강제
  업데이트여도 그대로 돈다 — 순서를 직렬로 묶으면 스플래시가 길어진다.
- 진행 조건: 부트스트랩 목적지, 버전 판정, 로띠 종료 **셋 다** 갖춰졌을 때.
  - `Required` → 이동 이펙트를 내지 않고 모달을 상태로 켠다(이펙트가 아니라 상태 — 재구성·회전에도 남아야 한다).
  - `NotRequired` → 기존대로 `NavigateToLogin` / `NavigateToGroupList`.
- 인텐트 `ForceUpdateClick` → 이펙트 `OpenStore(storeUrl)`, `QuitClick` → 이펙트 `QuitApp`.
- `SplashRoute`가 이펙트를 처리한다.
  - `QuitApp` → `Activity.finishAndRemoveTask()`.
  - `OpenStore` → `storeUrl`이 있으면 그 URL, 없으면 `market://details?id=<packageName>`,
    그 인텐트를 받을 앱이 없으면 `https://play.google.com/store/apps/details?id=<packageName>`.
  - 스토어에서 업데이트 없이 돌아오면 모달이 그대로 남는다(상태가 유지되므로).

### 모달

- `core:ui`에 `YGModalForceUpdate(onQuit, onUpdate)` — `YGModalPopup` 래퍼, `YGModalQuit`과 같은 형태.
  - 아이콘 `ic_warning_round`, 제목 "업데이트가 필요해요",
    본문 "서비스를 계속 이용하려면\n최신 버전으로 업데이트해 주세요".
  - secondary **[종료]**, primary [업데이트]. 시안의 "취소"를 "종료"로 바꾼 것은 의도다(사용자 결정).
  - `onDismissRequest = {}` + `DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)`.
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

- `CheckForceUpdateUseCase`: `FORCE`→`Required`(storeUrl 전달), `OPTIONAL`·`NONE`→`NotRequired`,
  실패→`NotRequired`.
- 매퍼: 모르는 `updateType` 문자열 → 강제 아님.
- `SplashViewModel`: 세 신호의 도착 순서와 무관하게 셋 다 와야 진행, `Required`면 이동 이펙트 없음 +
  모달 상태, 버튼 인텐트 → 해당 이펙트.
- 수동: 서버 미배포(404) 상태에서 정상 진입 확인.

## 문서

- `docs/api/app-version.md` 신설 — 서버 계약 미러 + Android 매핑(fail-open, `updateType` 문자열 수신).
- `docs/status.md` 갱신.

## 구현자 재량

- 도메인 모델·유스케이스의 정확한 이름과 파일 배치(기존 `domain/model/<영역>` 규칙을 따른다).
- 스토어 인텐트 처리 코드의 위치(`SplashRoute` 내부 또는 `core:util:android` 확장 함수).
- 버전 체크용 별도 타임아웃은 두지 않는다 — 기존 OkHttp 설정을 따른다.
