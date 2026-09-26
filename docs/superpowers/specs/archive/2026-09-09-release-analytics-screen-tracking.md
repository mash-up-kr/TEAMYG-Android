---
id: release-analytics-screen-tracking
title: 화면 진입 계측과 기기·앱 사용자 속성 (Firebase Analytics)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-21
related_code:
  - MainRoute.kt#MainRoute
  - MainActivity.kt#MainActivity
  - BaseApplication.kt#BaseApplication
  - Navigator.kt#Navigator
  - app/build.gradle.kts
related_adr: ADR-0031
related_spec:
related_architecture: navigation-flow
supersedes:
superseded_by:
tags: [spec, parfait, analytics]
---

# Spec: 화면 진입 계측과 기기·앱 사용자 속성

## 목표

사용자가 어느 화면에 얼마나 들어오는지를 Firebase Analytics로 집계한다. 화면을 부르는
이름은 기획이 이미 쓰고 있는 화면 ID(`C-001`·`G-001` 등, 위키 `화면-ID-체계`)를 그대로
쓴다. 기기·앱 정보 7종을 사용자 속성으로 함께 실어 OS 버전이나 앱 버전으로 지표를 가를 수
있게 한다.

`firebase-analytics` 의존성은 이미 `app/build.gradle.kts`에 들어와 있고 Firebase 도입 자체는
ADR-0013이 Analytics 를 포함해 결정했으나, 부르는 코드가 없어 지금까지 이벤트가 한 건도
나가지 않았다. 이 스펙이 그 첫 소비처를 만든다.

## 범위

- 포함
  - 화면 진입 이벤트(`screen_view`) 전송. `NavKey` 전체가 대상이다.
  - 기기·앱 사용자 속성 7종 설정.
  - release 에서만 수집하는 게이트와, 그 게이트를 빌드 시점에 덮어쓰는 수단.
  - `NavKey`를 화면 ID로 바꾸는 매핑과 그 유닛 테스트.
- 제외
  - 버튼 탭·전환 같은 **액션 이벤트**. 계층을 세우는 첫 라운드라 화면 축 하나만 다룬다.
    다음 라운드에서 같은 `AnalyticsLogger` 위에 얹는다.
  - 사용자 식별자(`setUserId`) 전송. 계정 id를 분석 도구로 내보내는 판단이 따로 필요하다.
  - debug 빌드를 별도 Firebase 앱으로 분리하는 일(`applicationIdSuffix` 신설).
  - 도달할 수 없는 화면 5종의 코드 삭제. 매핑에는 넣되 정리는 이 브랜치에서 하지 않는다.
  - `Navigator`에 중복 push 를 막는 변경. 계측이 내비게이션 동작을 바꾸지 않는다.

## API / 인터페이스

```kotlin
// 분석 도구에 값을 넘기는 유일한 창구. Firebase 를 직접 부르는 자리는 구현체 하나뿐이다.
interface AnalyticsLogger {
    fun setCollectionEnabled(enabled: Boolean)
    fun setUserProperty(name: String, value: String)
    fun logScreenView(screen: AnalyticsScreen)
}

// 매핑 결과. 두 값 모두 코드에 박힌 고정 문자열이다.
data class AnalyticsScreen(
    val screenId: String,
    val screenClass: String,
)

// 매핑 전체가 이 함수 하나에 있다. 대응이 없으면 null 이다.
fun NavKey.toAnalyticsScreenOrNull(): AnalyticsScreen?

// 중복 판정과 전송을 맡는다. 컴포지션이 죽어도 살아남아야 해서 @ActivityRetainedScoped 다.
class ScreenViewTracker(logger: AnalyticsLogger) {
    fun track(backStackSize: Int, top: NavKey?)
}

// 사용자 속성으로 실을 기기·앱 값. Build 를 직접 읽지 않고 주입받아 조립한다.
data class DeviceInfo(
    val osType: String,
    val osVer: String,
    val appVer: String,
    val appVerCode: String,
    val device: String,
    val appId: String,
)
```

`screenClass`를 `this::class.simpleName`으로 얻지 않는 이유는 release 빌드에 R8 난독화가
켜져 있어 클래스 이름이 뭉개지기 때문이다. 그러면 운영 집계에서만 값이 깨져 개발 중에는
드러나지 않는다. 매핑 함수가 두 값을 모두 문자열 상수로 돌려주면 이 위험이 사라진다.

`setCollectionEnabled`가 인터페이스에 있는 이유는 `BaseApplication`이 수집을 켜야 하는데,
그 자리에서 `FirebaseAnalytics`를 직접 잡으면 "Firebase 를 부르는 자리는 구현체 하나뿐"이라는
규칙이 첫 파일에서 깨지기 때문이다.

## 동작 / 상태

### 화면 진입 이벤트

GA4 표준 이벤트 `screen_view`(`FirebaseAnalytics.Event.SCREEN_VIEW`)를 쓴다. 커스텀 이름을
쓰면 GA4 기본 화면 보고서를 쓸 수 없다.

| 파라미터 상수 | 실제 키 | 값 |
|---|---|---|
| `FirebaseAnalytics.Param.SCREEN_NAME` | `screen_name` | 화면 ID (`C-001` 등) |
| `FirebaseAnalytics.Param.SCREEN_CLASS` | `screen_class` | `NavKey` 이름 문자열 (`NavKeyCanvasMain` 등) |

⚠️ **`firebase_screen`·`firebase_screen_class`를 직접 쓰지 않는다.** `firebase_`는 GA4
예약 접두사(`firebase_`·`google_`·`ga_`)이고, 그 이름은 자동 수집이 내부에서 만드는 것이지
`logEvent`로 넣는 이름이 아니다. 예약 접두사로 실어 보낸 파라미터는 버려질 수 있다.
반드시 SDK 상수를 쓴다.

### 화면 전환 판정

**최상단 키만 보면 안 된다.** `Navigator.goTo`는 조건 없이 `_backStack.add` 하므로 같은 값의
키가 겹쳐 쌓일 수 있다. 실제로 `MainRoute`의 푸시 딥링크 처리가 `goTo`를 쓰고, 두 목적지가
모두 이 상황에 걸린다.

- `NavKeyGroupList`는 `data object`다. 그룹 목록에 있는 상태로 GroupList 푸시를 탭하면 같은
  값이 하나 더 쌓인다.
- `NavKeyCanvasMain`은 welcome 두 필드가 기본값 `null`이다. 그룹 목록에서 탭해 들어온
  평범한 진입은 두 필드가 `null`이라, 같은 그룹의 AddTopping 푸시를 탭하면 동등한 인스턴스가
  하나 더 쌓인다.

두 경우 모두 `NavEntry`가 새로 생기고 전환 애니메이션이 도는 **실제 화면 전환**인데 최상단
값은 바뀌지 않는다. 그래서 판정 기준을 **백스택 크기와 최상단 키의 짝**으로 둔다.

```kotlin
snapshotFlow { navigator.backStack.size to navigator.backStack.lastOrNull() }
```

이 짝이면 위 두 경우가 크기 변화로 잡히고, `goToAndPopCurrent`(크기 그대로·최상단 교체)와
`popUpTo`·`goToSingleClearTop`·`replaceAll`(크기와 최상단이 함께 변함)도 모두 잡힌다.

### 중복 억제와 Activity 재생성

중복 판정은 `Flow` 연산자가 아니라 `ScreenViewTracker`가 들고 있는 마지막 전송 값과 견주어
한다. 이유는 Activity 재생성이다. `MainRoute`의 수집기는 `LaunchedEffect(Unit)` 안에 있고
`snapshotFlow`는 구독하는 순간 현재 값을 한 번 방출한다. 컴포지션이 죽었다 살아나면 이동이
없었는데도 이벤트가 한 번 더 나간다.

회전은 매니페스트의 `screenOrientation="portrait"`(ADR-0027)로 막혀 있지만 회전 없이 재생성되는
경로가 남아 있다. 다크모드 토글, 폰트·언어 설정 변경, 폴더블·멀티윈도우 리사이즈가 그렇고,
이 목록은 `MainActivity.consumePushDeepLink`의 주석이 같은 이유로 이미 열거해 둔 것이다.

`ScreenViewTracker`를 `@ActivityRetainedScoped`로 두면 마지막 전송 값이 컴포지션 밖에 남아,
재생성 직후의 재방출이 같은 짝이므로 걸러진다.

**`@Singleton`으로 두면 안 된다.** 그러면 트래커가 `Navigator`(`@ActivityRetainedScoped`)보다
오래 산다. Activity 가 실제로 끝난 뒤 프로세스가 살아 있는 채로 다시 들어오면 `Navigator`가
새로 서면서 언제나 `(1, NavKeySplash)`를 내는데, 그 짝이 낡은 마지막 전송 값과 같아 **그
실행의 A-001 이 통째로 빠진다.** 두 객체의 수명을 맞추면 그 손실이 사라지고, 구성 변경
재생성을 막는 성질은 그대로 남는다 — 그때는 `Navigator`도 같은 인스턴스다.

나머지 규칙은 다음과 같다.

- 뒤로 가기로 이전 화면에 **돌아올 때는 보낸다.** 크기와 최상단이 함께 바뀐다. 조회수
  기준으로 이쪽이 맞다.
- 앱을 백그라운드로 보냈다 돌아오는 것은 **보내지 않는다.** 스택이 그대로다. 세션 경계는
  Firebase 가 자체로 잡는다.
- 매핑이 `null`이면 전송하지 않고 경고 로그를 한 줄 남긴다(`Loggers.create(...)`로 만든
  로거의 `w { }`). 새 화면을 만들며 매핑을 잊었을 때 개발 중에 드러나게 하려는 것이다.

수집기는 `MainRoute`의 기존 세션·딥링크 `LaunchedEffect` 옆에 나란히 둔다. 화면마다 따로
수집하지 않는 이유는 앞의 둘과 같다. 여러 화면이 구독하면 한 전환이 여러 번 찍힌다.

### 사용자 속성

`BaseApplication.onCreate`에서 수집을 켜고 속성 7종을 설정한다. 이름은 대문자 스네이크
케이스다.

| 속성 | 값의 출처 |
|---|---|
| `IS_DEBUG` | `BuildConfig.ANALYTICS_IS_DEBUG` |
| `OS_TYPE` | 고정 문자열 `Android` |
| `OS_VER` | `Build.VERSION.RELEASE` |
| `APP_VER` | `BuildConfig.VERSION_NAME` |
| `APP_VER_CODE` | `BuildConfig.VERSION_CODE` |
| `DEVICE` | `Build.MANUFACTURER`와 `Build.MODEL`을 이어 붙인 뒤 36자로 자른 값 |
| `APP_ID` | `BuildConfig.APPLICATION_ID` |

`DEVICE`를 자르는 이유는 GA4 사용자 속성 **값**의 상한이 36자이기 때문이다. 안 자르면 GA4가
조용히 자른다. 우리가 먼저 자르고 그 규칙을 테스트로 고정한다.

`DeviceInfo`가 나르는 것은 이 중 여섯이다. `IS_DEBUG`는 빌드 설정에서 따로 오므로 그 자료형에
넣지 않는다.

Firebase 는 앱 버전·OS 버전·기기 모델을 이미 자동 측정 차원으로 갖고 있어 이 넷은
중복이다. 그래도 명시 속성을 두는 이유는 자동 차원의 이름과 형식을 우리가 정할 수 없어
세그먼트 조건으로 쓰기 번거롭기 때문이다. 프로젝트당 사용자 속성 한도는 25개이고 7개를
쓴다.

### 수집 활성화와 IS_DEBUG

debug 와 release 가 **같은 Firebase 앱·같은 GA4 속성을 쓴다.** `applicationId`가
`com.teamyg.parfait` 하나이고 `applicationIdSuffix`가 저장소 어디에도 없기 때문이다. 개발
트래픽이 운영 지표와 같은 통에 섞이므로 **debug 는 수집 자체를 끈다.**

```kotlin
analyticsLogger.setCollectionEnabled(!BuildConfig.ANALYTICS_IS_DEBUG)
```

수집을 여닫는 것이 `ANALYTICS_IS_DEBUG` 하나이므로, 그 값을 덮어쓰면 debug 빌드로도 전송을
확인할 수 있다. 거르는 일을 GA4 보고서 쪽에 맡기지 않는 이유는, 세그먼트를 거는 것을 잊은
지표가 조용히 오염되기 때문이다.

⚠️ **그래서 `IS_DEBUG` 사용자 속성은 도착한 데이터에서 언제나 `false`다.** 수집이 켜진
빌드는 정의상 그 값이 `false` 인 빌드뿐이다. 이 속성은 지금 지표를 가르지 않고, 나중에 수집
정책이 바뀔 때를 위한 자리로만 남는다.

`ANALYTICS_IS_DEBUG`는 `BuildConfig.DEBUG`가 아니라 별도 필드다. Gradle 프로퍼티
`-Panalytics.isDebug=false`로 덮어쓸 수 있게 하기 위해서다. debug 빌드로도 운영과 같은
조건을 만들어 검증하며, 코드를 고칠 필요가 없다. 프로퍼티를 주지 않으면 빌드 타입 기본값을
따르고(release `false` / debug `true`), 값이 `true`가 아닌 것은 전부 `false`로 읽는다 —
값 없이 `-Panalytics.isDebug`만 주면 빈 문자열이 `boolean` 자리에 꽂혀 빌드가 깨지기 때문이다.

⚠️ **이 필드는 한 줄로 끝나지 않는다.** `app/build.gradle.kts`에는 `buildTypes` 블록이 없고,
release·debug 정의는 `build-logic`의 `AndroidConfig.kt#setConfigAndroidApplication`에 있다.
앱 스크립트에서 `buildTypes`를 다시 열어 타입별 `buildConfigField`를 넣고, Gradle 프로퍼티가
있으면 그 값이 이기게 한다.

## 표시·제어 규칙

### 화면 ID 매핑표

`NavKey` 26개 전부를 대응시킨다. 인자를 보고 갈리는 키가 다섯이고, 그중
`NavKeyPictureConfirm`은 두 인자를 함께 봐 넷으로 갈린다.

| NavKey | 조건 | 화면 ID |
|---|---|---|
| `NavKeySplash` | | `A-001` |
| `NavKeyLogin` | | `A-002` |
| `NavKeyTermAgree` | | `A-003` |
| `NavKeyGroupInviteCode` | | `A-004` |
| `NavKeyGroupNickName` | | `A-004-naming` |
| `NavKeyGroupCreate` | | `A-005` |
| `NavKeyGroupList` | | `G-001` |
| `NavKeyCanvasMain` | | `C-001` |
| `NavKeyCameraCustom` | `returnResultOnly=false` | `C-101` |
| `NavKeyCameraCustom` | `returnResultOnly=true` | `C-302` |
| `NavKeyCameraSystem` | | `C-101-system` |
| `NavKeyPictureConfirm` | `source=CAMERA`, `returnResultOnly=false` | `C-101-confirm` |
| `NavKeyPictureConfirm` | `source=CAMERA`, `returnResultOnly=true` | `C-302-confirm` |
| `NavKeyPictureConfirm` | `source=GALLERY`, `returnResultOnly=false` | `C-102-confirm` |
| `NavKeyPictureConfirm` | `source=GALLERY`, `returnResultOnly=true` | `C-303-confirm` |
| `NavKeyCustomGalleryPicker` | `returnResultOnly=false` | `C-102` |
| `NavKeyCustomGalleryPicker` | `returnResultOnly=true` | `C-303` |
| `NavKeySystemGalleryPicker` | | `C-102-system` |
| `NavKeySegmentation` | | `C-103` |
| `NavKeySegmentationConfirm` | | `C-103-select` |
| `NavKeyToppingEdit` | `borderOnly=false` | `C-104` |
| `NavKeyToppingEdit` | `borderOnly=true` | `C-105/C-306` |
| `NavKeyCanvasToppingPlace` | | `C-106` |
| `NavKeyCanvasBGEdit` | `initialToppingId=null` | `C-301` |
| `NavKeyCanvasBGEdit` | `initialToppingId≠null` | `C-305` |
| `NavKeyCanvasImageSave` | | `C-001-image-save` |
| `NavKeyCanvasEdit` | | `C-001-edit` |
| `NavKeyCanvasImageSelect` | | `C-001-image-select` |
| `NavKeyCanvasMove` | | `C-001-move` |
| `NavKeyAppSetting` | | `S-001` |
| `NavKeyAccountInfo` | | `S-002` |
| `NavKeyGroupSetting` | | `S-101` |
| `NavKeyWebView` | | `S-004` |

`NavKeyPictureConfirm`을 넷으로 가르는 근거는 `NavKeyCameraCustom`·`NavKeyCustomGalleryPicker`를
가른 것과 같다. 그 두 화면에서 시작한 편집 모드 흐름(C-302·C-303)의 확인 화면이
`returnResultOnly=true`로 들어오는데, 앞 화면은 갈라 놓고 확인 화면만 합치면 두 흐름의
이탈률을 견줄 수 없다. 값은 `CustomCameraRoute`와 `CustomGalleryPickerRoute`가 그대로
전파한다.

`C-105/C-306`은 한 화면 ID 안에 두 번호를 담은 값이다. `borderOnly=true` 진입이
`SegmentationConfirmRoute`(최근 알맹이 재사용, C-105)와 `CanvasBGEditRoute`(편집 모드의
토핑 테두리 편집, C-306) 두 곳에서 오는데 키만 봐서는 갈리지 않는다. 둘 중 하나로 몰아
집계를 왜곡하는 대신 합친 값을 쓰기로 확정했다.

`A-004-naming`은 Figma 표기를 따른 **확정값**이다. 기능정의서 표에는 행이 없다.

**임시 ID 여덟**은 기능정의서에도 Figma 에도 대응이 없어 접두사 체계만 따라 붙인 것이다.
`C-101-system`·`C-102-system`·`C-302-confirm`·`C-303-confirm`·`C-001-image-save`·
`C-001-edit`·`C-001-image-select`·`C-001-move`가 여기 든다.

### 자동 화면 보고 끄기

매니페스트에 `google_analytics_automatic_screen_reporting_enabled`를 `false`로 넣는다.
단일 Activity 구조라 자동 수집은 `MainActivity` 이름으로 세션당 몇 번 찍히는 데 그치는데,
그 이벤트가 우리 화면 이벤트와 같은 보고서에 섞여 화면별 수치를 흐린다.

## 파일 구성

새 코드는 전부 `:app`의 `com.teamyg.parfait.analytics` 패키지에 둔다. feature·core 모듈은
바뀌지 않는다. `:app`은 이미 모든 feature `:api`에 의존하므로 새 의존이 생기지 않는다.

| 파일 | 역할 |
|---|---|
| `analytics/AnalyticsLogger.kt` | 인터페이스와 `AnalyticsScreen` |
| `analytics/FirebaseAnalyticsLogger.kt` | `FirebaseAnalytics`에 위임하는 유일한 구현체 |
| `analytics/AnalyticsUserProperty.kt` | 속성 이름 상수 |
| `analytics/DeviceInfo.kt` | 기기·앱 값과 조립 함수 |
| `analytics/NavKeyAnalyticsScreen.kt` | `toAnalyticsScreenOrNull()` 매핑 전체 |
| `analytics/ScreenViewTracker.kt` | 중복 판정과 전송 |
| `analytics/di/AnalyticsModule.kt` | Hilt 바인딩 |

Hilt 모듈을 `di` 하위 패키지에 두는 것은 `push/di/DeviceTokenModule`과 `:data`의 `data/di/`
관례를 따른 것이다.

기존 파일은 넷을 고친다. `BaseApplication`이 수집 활성화와 사용자 속성을 설정하고,
`MainActivity`가 `ScreenViewTracker`를 주입받아 `MainRoute`에 넘기며, `MainRoute`가 수집기를
하나 더 달고, `app/build.gradle.kts`가 `buildTypes`를 열어 `ANALYTICS_IS_DEBUG`를 더한다.
매니페스트에 자동 화면 보고 끄기 항목이 들어간다.

### 테스트

전송 판정이 `ScreenViewTracker`에 모여 있어 전부 JVM 유닛 테스트로 덮인다.
Turbine·MockK·`kotlinx-coroutines-test`는 이미 유닛 테스트 번들에 있고 `:app`에
`parfait.test.unit` 플러그인이 적용돼 있다.

- 매핑 함수 — `NavKey` 26개와 갈리는 다섯의 모든 인자 조합을 넣어 기대 ID·클래스명과
  대조한다. 매핑되지 않은 키가 `null`을 내는 것도 본다.
- `ScreenViewTracker` — 가짜 `AnalyticsLogger`를 끼우고 다음을 본다. 같은 짝이 연달아 오면
  한 번만 보낸다. **크기만 달라져도 보낸다**(중복 push). 뒤로 가기 복귀(A→B→A)에서 A 가 두
  번 나온다. 매핑이 없는 키는 보내지 않는다.
- `DeviceInfo` 조립 — 주입한 값으로 여섯 필드가 규격대로 만들어지는지 본다.

## 주의 / 열린 질문

- ✅ **실기기에서 이벤트 도착을 확인했다**(2026-09-09). 수집이 release 한정이 된 뒤이므로
  `-Panalytics.isDebug=false` 로 빌드해 확인한 것이고, 그 사실 자체가 게이트 덮어쓰기가
  동작한다는 근거이기도 하다.
- ⚠️ **확인은 전송까지다.** 뒤로 가기 복귀가 다시 찍히는지, 다크모드 토글로 Activity 가
  재생성돼도 헛 이벤트가 안 나가는지, 사용자 속성 7종이 모두 보이는지는 아직 눈으로 보지
  않았다. 앞의 둘은 유닛 테스트가 덮고 있으나 실기기 확인은 별개다.
- ⚠️ **임시 ID 여덟은 기획이 정식 번호를 주면 이름이 바뀐다.** 그 시점에 과거 집계가
  끊긴다. 감수하기로 확정했다.
- ⚠️ 도달할 수 없는 화면 다섯(`NavKeyCanvasEdit`·`NavKeyCanvasImageSelect`·
  `NavKeyCanvasMove`·`NavKeyCameraSystem`·`NavKeySystemGalleryPicker`)에도 ID 를 줬다.
  이벤트가 나가지 않아 지표에 0건으로 남는다.
  > 📌 **다섯이 둘로 줄었다(2026-09-20, PR #514)** — 캔버스 쪽 셋이 화면째 삭제되면서
  > `toAnalyticsScreenOrNull()`의 `C-001-edit`·`C-001-image-select`·`C-001-move` arm 과
  > `NavKeyAnalyticsScreenTest` 의 기대값도 함께 빠졌다(OQ-P-239). 위 매핑 표의 그 세 행은
  > 더 이상 코드에 없다. **`NavKeyCameraSystem`·`NavKeySystemGalleryPicker` 둘은 그대로**라
  > 0건으로 남는 ID 도 둘이다.
- ⚠️ 매핑 누락은 컴파일러가 잡지 못한다. `NavKey`는 sealed 가 아니어서 `when`이 빠짐없음을
  강제하지 못하고, 새 화면을 추가하며 매핑을 잊으면 런타임 경고 한 줄로만 드러난다.
- ⚠️ 패키지 이름이 겹쳐 읽는 사람이 헷갈릴 수 있다. `core:util:jvm`의
  `com.teamyg.parfait.core.util.jvm.analytics`는 이름과 달리 **로깅** 패키지이고(ADR-0014),
  새로 만드는 `com.teamyg.parfait.analytics`가 진짜 분석이다.
- `Navigator.goToSingleClearTop`은 저장소 전체에서 호출부가 0건이다. 같은 값 키가 겹쳐 쌓이는
  문제를 내비게이션 쪽에서 없앨 수단이 이미 있다는 뜻이지만, 이 스펙은 계측이 내비게이션
  동작을 바꾸지 않는다는 원칙을 지켜 손대지 않는다.
