---
id: module-structure
title: 모듈 구조
category: architecture
status: living
platforms: android
verified: 2026-09-09
related_spec: a005-group-create, g001-group-list, c101-camera-picture-confirm, unit-test-infrastructure, c301-canvas-background-edit, c201-canvas-calendar, session-token-refresh-infra, c301-topping-edit-tab
related_adr: ADR-0001, ADR-0002, ADR-0003, ADR-0011, ADR-0015, ADR-0016, ADR-0025
related_architecture:
related_code:
  - settings.gradle.kts
  - build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/TestConfig.kt#setConfigTestUnit
tags: [architecture, parfait]
---
# 모듈 구조

전체 모듈의 목적·주요 의존·레이어 그룹. 결정 근거는 [[0001-layered-multi-module]]·[[0002-feature-api-impl-split]]·[[0003-convention-plugins-version-catalog]].

> 모듈 등록 SoT = `settings.gradle.kts`. 버전·의존 SoT = `gradle/libs.versions.toml`.
> 근거는 파일명+심볼명으로만. 라인번호·모듈 개수 등 수치는 적지 않는다(→ [`../adr/README.md`](../adr/README.md)).

## 의존 방향 (단방향)

```
app / app-preview
  └─ feature/*/impl ── (이동 대상) ──▶ 다른 feature/*/api
       └─ core/{ui, navigation, designsystem, util}
            └─ domain (순수 Kotlin)
            └─ data ──▶ domain
```

## 레이어별 모듈

| 그룹 | 모듈 | 목적 | 적용 컨벤션 플러그인 |
|------|------|------|----------------------|
| 진입 | `app`, `app-preview` | 앱 진입점(`BaseApplication`, `MainActivity`, `MainRoute`), 전체 조립 + **푸시 수신·딥링크 파싱 `push/`**(#446·#447 — `ParfaitFirebaseMessagingService`·`PushDeepLinkParser`·`Intent.toPushDeepLinkOrNull`. Firebase SDK 의존을 `app`에만 두는 [[0013-firebase-fcm-crashlytics]] 경계를 지킨다) + **기기 토큰 조회 `push/FirebaseDeviceTokenProvider`와 `push/di/DeviceTokenModule`**(#450 — 같은 경계 때문에 `:data`로 못 내려, **`:app` 최초의 Hilt 모듈**이 됐다. [[0004-hilt-ksp-di]]의 `:data` `di/` 평면 배치 규칙의 첫 예외) + **화면 진입 계측 `analytics/`**(#478 — `AnalyticsLogger`·`FirebaseAnalyticsLogger`·`AnalyticsScreen`·`NavKeyAnalyticsScreen`·`ScreenViewTracker`·`DeviceInfo`·`analytics/di/AnalyticsModule`. 같은 Firebase 경계 때문에 여기 산다. **매핑이 `:app`에 있는 것은 필연이다** — `NavKey` 전부를 한 `when`으로 보려면 모든 feature `:api`를 봐야 하는데 그 조건을 만족하는 모듈이 `:app` 하나다. ⚠️ `core:util:jvm`의 `…util.jvm.analytics`는 이름과 달리 **로깅** 패키지이고([[0014-logging-abstraction-kermit]]) 이쪽이 진짜 분석이다) | `AndroidApplication*`, 서명 (+ **`parfait.test.unit`** #447) |
| core | `core:ui` | MVI 베이스(`BaseViewModel`, `MviContract`), 공유 전환 스코프, 여러 feature 공용 레이아웃(`VerticalGridLayout`)·공용 문자열 리소스(유효성 에러 문구) + **도메인 결과 → 표시 문자열 매핑**(`text/NameValidResultUiText.kt`, [[0016-domain-result-presentation-string-mapping]]) + **드러내기 상태 `reveal/`**(#440, 아래 참고) + **`outline/`**(PR #464 develop 머지 — `loadToppingOutline`·`rememberToppingOutlines`·`clearToppingOutlines`. 옛 `ToppingAlphaMaskCache`(`feature/groups/canvas/impl`)가 이 자리로 옮겨 오며 거리판을 내도록 확장됐다 — LRU 64칸·in-flight 합류·Coil 디코딩 한 번을 그대로 물려받는다). `:domain` 의존(#223, 2026-08-13) | android-library + compose (+ `parfait.test.android`·`parfait.test.compose` #440) |
| core | `core:designsystem` | 테마(`YGMaterialTheme`)·토큰(`YGSemanticColors`, `SizeTokens` 등) | android-library + compose |
| core | `core:navigation` | `Navigator`, NavKey 레지스트리, 엔트리 등록 | android-library |
| core | `core:util:android` | Android 전용 유틸(`decodeUriToBitmap`, `AndroidBitmap`) — `decodeUriToBitmap`은 API 28 미만 갈래에서 EXIF 회전을 픽셀에 적용한다(`extension/ExifOrientation.kt#exifOrientationToDegrees` + 비공개 `rotatedToUpright`, #349. `androidx.exifinterface` 의존은 이 모듈만 쓴다). 모듈 로거 `Logger.kt#coreUtilAndroidLogger`([[0014-logging-abstraction-kermit]]) + Compose clickable 유틸(`clickable/`: `clickableYG`·`clickableYGNoRipple`·`ygDimRipple`·`ygScaleRipple`, 테마 비의존 — #284로 프로덕션 클릭 전량이 이 패키지를 탄다) + 포커스 유틸(`focus/`: `Modifier.clearFocusOnTap`) + Compose·플랫폼 확장(`extension/`: `Modifier.navigationBarsAndImePadding`·`Modifier.drawTooltipCornerTop`·`AnnotatedString.Builder.withStyle`·`List<Offset>.toPath`/`toAndroidPath`·`ClipDescription.isSensitive`·`Modifier.verticalScrollbar`(#259)·`Modifier.centeredAt`·`Modifier.dragBy`(#264. `centeredAt` 은 **부모보다 큰 자식도 중심이 맞는다** — 좌상단을 직접 계산해 `offset` 으로 넘기면 부모 제약으로 잘린 겉크기 안에 내용이 가운데 정렬되어 넘친 양의 절반만큼 밀린다. 이 계약을 계측 테스트 `ModifierCenteredAtTest` 가 두 방식을 나란히 재서 고정한다, #465)·`String.toColorOrNull`(#268 — 서버가 주는 `#RRGGBB` 색 문자열은 캔버스 전용이 아니라 어느 화면에나 온다)·**`File.readExifDegrees`**(#473 — `extension/File.kt`. `ExifInterface` 판독을 감싸 `exifOrientationToDegrees` 로 각도를 낸다. **못 읽으면 경고 로그를 남기고 0** — 태그가 깨진 것과 파일을 못 여는 것은 다른 사건이라 호출부의 디코드까지 막지 않는다. `decodeUriToBitmap` 의 `Uri` 판과 규약이 같고 받는 타입만 다르다)) + 권한 판정(`permission/`: `GalleryPermissionManager`·`GalleryWritePermissionManager`·**`NotificationPermissionManager`**(#450 — `POST_NOTIFICATIONS`가 API 33 신설이라 그 아래를 **허용으로 본다.** `sdkInt`를 인자로 받고 기본값만 `Build.VERSION.SDK_INT`인 것은 Robolectric 없이 이 갈림을 JVM 테스트로 덮기 위해서다)) + 앱 메타(`AppInfo.kt#APP_VERSION_NAME`, #295 — 이 모듈만 `buildFeatures.buildConfig`를 켜고 `:app`과 **같은 버전 카탈로그 항목**을 `buildConfigField`로 다시 심는다. `versionName`은 애플리케이션 모듈 속성이라 라이브러리 `BuildConfig`에 없기 때문이고, `:app`이 `versionNameSuffix`·플레이버로 갈아 끼우면 따라가지 않는다 → [open-questions](../synthesis/open-questions.md)) + **`outline/`**(PR #464 develop 머지 — `Bitmap.toToppingOutline`이 알파 채널에서 거리판을 만들고, `ToppingOutline.toBorderAlphaBitmap`·`toBorderArgbBitmap`이 그 거리판을 색 안 태운 `ALPHA_8` 띠 / 색까지 태운 `ARGB_8888` 띠로 굽는다. 아래 `core:util:jvm` 픽셀 연산 문단이 예고한 승격이 이 패키지로 실현된다). `core:util:jvm` 의존 | android-library + compose (+ **`parfait.test.compose`** #465 — 계측 소스셋에 Compose 테스트 하니스가 붙었다) |
| core | `core:util:jvm` | 순수 Kotlin 유틸·로깅·플랫폼 무관 추상(`BitmapWrapper`) + 공용 날짜 포맷(`model/DateFormat`·`model/DateTextFormat`, `kotlinx-datetime`)·날짜 확장(`extension/LocalDateExtension`, #259) + 픽셀 연산 확장(`extension/`: `Int.argbAlpha`·`Int.fadeArgb`·`Int.mixArgb`·`IntArray.sumArgbAlpha`·`FloatArray.fillWithSquaredDistance`. 알파 총합은 #359 리뷰가 `data`에서 올렸다 — ARGB 알파를 꺼내 더하는 연산이라 세그멘테이션에 묶일 이유가 없다) + **`outline/`**(PR #464 develop 머지 — `ToppingOutline` 거리판 보유·이중선형 보간·불투명 판정. 값 타입 `ToppingBorderBand`·`ToppingBorderTarget` 과 담기 규격 `ToppingOutlineSpec` 은 `model/`에 있다. Android 타입 0건이라 순수 JVM 유닛으로 덮는다) | kotlin-jvm |
| core | `core:testing` | **테스트 전용** 공용 유틸(`MainDispatcherRule`). 테스트 소스셋만 소비하므로 위 의존 방향 그래프에 없다 | kotlin-jvm |
| domain | `domain` | UseCase, Repository 인터페이스, 도메인 모델 + **이벤트 통로 `event/`**(#450 — `SessionEventBus`·`PushDeepLinkEventBus`가 `repository/session/`·`repository/push/`에서 옮겨 왔다. 둘 다 Repository가 아니다) + **`notification/`**(#450 — `DeviceTokenProvider`·`DeviceTokenRegistrar`, 역시 `repository/` 밖이다) | `ModuleDomain`(kotlin-jvm) |
| data | `data` | Repository 구현, DataSource, DI 모듈 + **`event/`**(#450 — `SessionEventBusImpl`·`PushDeepLinkEventBusImpl`이 `session/`·`push/`에서 옮겨 왔고 구현 이름이 `~Impl`로 통일됐다) + **`notification/DeviceTokenRegistrarImpl`**(앱 스코프 실행·뮤텍스·재시도) | `ModuleData` |
| feature | `feature/{login,segmentation,camera,gallery,intro}/{api,impl}` | 화면·VM(impl) / NavKey 계약(api) | `ModuleFeatureApi` / `ModuleFeatureImpl` |
| feature | `feature/groups/{canvas,enter,list,setting}/{api,impl}` | 그룹 관련 화면 묶음 | 동일 |
| feature | `feature/app/setting/{api,impl}` | 앱 설정 화면(`NavKeyAppSetting`, `AppSettingRoute`) | 동일 |
| feature | `feature/common/terms/{api,impl}` | 웹뷰 화면(`NavKeyWebView(title, url)`, `WebViewRoute`/`WebViewScreen`, `NotionWebView`) — 여러 feature 공유([[0015-feature-common-shared-layer]]). **#296에서 약관 종류별 NavKey·Route·ViewModel 2벌이 하나로 합쳐졌다**(무엇을 여는지는 부르는 쪽이 인자로 정한다) | 동일 |

> **픽셀 연산이 `core:util:jvm`으로 올라온 이유(2026-08-14, PR #221)** — 토핑 테두리를 거리장으로 그리려면
> 픽셀 배열을 직접 훑어야 해서 색 하나마다 Compose `Color` 변환을 태울 수 없다. ARGB 정수를 그대로 만지는
> 연산(`fadeArgb`·`mixArgb`)과 2-pass 제곱거리 변환(`fillWithSquaredDistance`)은 Android 타입이 없어
> jvm 쪽에 두고 유닛 테스트로 덮었다. **화면에 쓸 색은 여전히 Compose `Color`가 맞다** — 여기 있는 것은
> 픽셀 루프 전용이다(파일 상단 주석이 그 경계를 적어 둔다).
> 🔁 **예고가 실현됐다(PR #464 develop 머지, [ADR-0030](../adr/0030-topping-outline-distance-field.md))** —
> 그때는 편집 화면 하나만 거리장으로 그렸는데, 이 연산 위에 `ToppingOutline`(`core:util:jvm`)·
> `Bitmap.toToppingOutline`(`core:util:android`)이 얹히며 네 화면 전부가 같은 거리장으로 그리고
> 판정한다. `feature/segmentation/impl`의 `internal` 계산기가 코어로 승격된 것이 바로 이 라운드다.

> **`feature/groups/home/{api,impl}` 삭제(2026-08-09, PR #220)** — `NavKeyGroupHome`·`GroupHomeRoute`는
> `ResultEventBus` 왕복을 시연하던 임시 화면이었고, 로그인 다음 목적지가 온보딩 체인으로 바뀌면서
> 모듈 2개가 `settings.gradle.kts`·`app`·`core:navigation`에서 함께 빠졌다. 그룹 개별 화면은 별도
> 목적지 없이 G-001 목록에서 캔버스(C-001)로 직접 가는 구조라 대체 모듈을 만들지 않았다.

## 규칙
- feature 간 이동은 상대 **`:api`(NavKey)만** 참조. `:impl`끼리 직접 의존 금지([[0002-feature-api-impl-split]]).
- `domain`은 Android 의존 금지(순수 Kotlin 유지). Android 타입이 도메인에 필요하면 `core:util:jvm`의 플랫폼 무관 추상으로 감싼다 — 비트맵은 `BitmapWrapper`([[0011-cross-module-bitmap-abstraction]]).
- 새 모듈 = 알맞은 컨벤션 플러그인 적용 + `settings.gradle.kts` 등록(같은 커밋).
- **`core:testing`은 프로덕션 코드에서 참조 금지.** 배선은 `setConfigTestUnit()`이 `testImplementation`으로
  넣어주고, 계측 소스셋에는 붙지 않는다. 이 모듈은 junit4·coroutines-test를 `api`로 재노출하지
  않으므로 `bundles.test-unit`과 짝일 때만 성립한다([spec](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md)).
  **`parfait.test.unit`은 화면 결선 라운드마다 그 feature `impl`에 붙는다**(2026-08-15 기준 인트로·로그인·
  그룹 목록·그룹 참여·그룹 설정·앱 설정). 적용 목록의 SoT는 각 `build.gradle.kts`다.
  > 📌 **진입 모듈에 처음 붙었다(2026-09-05, PR #447)** — 그전까지 이 플러그인은 core·`data`·`domain`과
  > feature `impl`에만 있었다. `app`에 붙으면서
  > `app/src/test`가 생겼다(`PushDeepLinkParserTest`). 진입 모듈이 유닛 테스트 소스셋을 가진 것은
  > 처음이고, 그것을 가능하게 한 것은 **파싱을 `Intent`에서 떼어 둔 것**이다 — `PushDeepLinkParser`가
  > 원시 문자열만 받으므로 Android 프레임워크 타입 없이 JVM에서 돌아간다. `Intent` 쪽 얇은 확장
  > (`toPushDeepLinkOrNull`)은 테스트가 없다.
  > 📌 **버그 수정 라운드에서도 붙었다(2026-09-11, PR #489)** — `feature/camera/impl`은 화면 결선
  > (#182, 2026-08-01) 뒤 여섯 주 동안 테스트 소스셋 없이 있다가, 권한 요청 수정과 함께 이 플러그인과
  > `CustomCameraViewModelTest`를 얻었다. 붙는 계기가 결선 라운드만은 아니므로, 위 문장의 "화면 결선
  > 라운드마다"는 관행이 시작된 자리로만 읽는다.
- ⚠️ **표시 규격이 `domain`에 들어온 사례**(2026-08-15, PR #231) — `domain` `model/CanvasConst.kt`의
  `CANVAS_ASPECT_RATIO`는 캔버스 화면 비율이라 도메인 규칙이 아니라 표시 규격이고, 같은 값이
  `core:designsystem` `YGCanvas`의 private `CANVAS_AREA_ASPECT_RATIO`로 이미 있다. Android 의존은
  없어 위 "순수 Kotlin 유지" 규칙은 어기지 않지만 **레이어 소유가 갈렸다**
  → [open-questions](../synthesis/open-questions.md) [2026-08-15].
  > ✅ **소유를 디자인시스템으로 모았다**(2026-08-22 develop 머지, PR #334) — `domain`의
  > `CanvasConst.kt`를 지우고 `YGCanvas`의 상수를 public으로 올려
  > 정본을 하나로 뒀다. 반대 방향으로 모으면 `core:designsystem` → `:domain` 간선이 새로 생기는데,
  > 캔버스 비율은 도메인 규칙이 아니라 **표시 규격**이라 소유가 이쪽이다. 상수가 하나뿐이라
  > 갈라짐을 막을 단언도 필요 없어졌다 — 컴파일이 보증한다.
  > 화면 규격 상수 **전반**의 소유 규칙은 여전히 정해지지 않았다(OQ-P-177 ③).
  > 📌 **같은 기준이 판정 코드에도 적용됐다**(2026-08-27 develop 머지, PR #388) — 토핑 알파 판정
  > (`ToppingAlphaMask`·`ToppingHitTarget`·`pickToppingHit`)은 Android 의존이 없어 `domain`으로
  > 올릴 수 있었지만 **표시 규격이라 feature에 남겼다.** 가르는 기준이 Android 의존 유무가 아니라
  > 도메인 규칙이냐 표시 규격이냐라는 것이 위 두 사례에서 이미 정해졌기 때문이다. 더해서 테두리
  > 방향 수는 그리는 쪽이 정본을 갖는 것이 설계 결정이라, `domain`으로 올리면서 그 값을 파라미터로
  > 빼면 판정 모양과 외형이 갈라질 자리가 열린다 — 지금 컴파일이 보증하는 것을 사람 규율로 바꾸는
  > 셈이다. 근거 전문은 [구현 계획서](../superpowers/plans/archive/2026-08-26-topping-alpha-hit-test.md)
  > "검토했으나 하지 않기로 한 것"에 있다.
- **feature `impl`이 다른 feature `:api`에 의존하는 경우가 하나 늘었다**(2026-08-15, PR #260) —
  `feature/app/setting/impl` → `feature/login/api`(강제·사용자 로그아웃 뒤 `NavKeyLogin`으로 간다).
  규약대로 `:api`만 본다.
  > 📌 **둘 더 늘었다(2026-08-17, PR #285·#287)** — `feature/groups/canvas/impl` →
  > `feature/groups/setting/api`(C-001 상단 메뉴가 S-101을 연다)와 `feature/groups/setting/impl` →
  > `feature/groups/list/api`(그룹 나가기·신고 성공 후 목록으로 백스택 교체). 둘 다 `:api`만 본다.
- **`core:util:jvm`의 `Char.isKorean()`은 삭제됐다**(2026-08-15, PR #243) — 이름 유효성 검사가 서버 정규식과
  같은 문자 집합(`가-힣A-Za-z0-9` + 스페이스)을 직접 쓰게 되며 유일한 사용처가 사라졌다.
- **여러 feature가 공유하는 화면**은 특정 도메인 feature 밑이 아니라 `feature/common/*`에 둔다([[0015-feature-common-shared-layer]]). 단, **2개 이상 소비처가 확정된 경우에만**(단일 소비면 소유 feature 유지).
- **feature `impl` 안의 화면 아닌 헬퍼는 `util` 패키지**(단수)에 둔다 — 도메인 값을 디자인시스템 타입으로
  옮기는 변환, 기하 계산, 플랫폼 헬퍼 따위다. 컴포저블 파일 바닥에 두면 화면 파일이 상태·인텐트·문구
  상수에 더해 변환까지 이고, 자기 테스트 파일을 가진 `internal` 함수가 화면 파일에 사는 모양이 된다.
  적용 모듈은 `camera`·`login`·`groups/canvas`·`groups/list`·`groups/setting` 다섯이고 SoT는 코드다.
  **컴포저블만 읽는 레이아웃 상수는 옮기지 않는다** — 그건 화면 소관이다.
  `core:designsystem`만 `utils`(복수)를 쓰는데 그쪽이 예외다(2026-08-18 기준 이름을 맞추지 않았다).
  > 📌 **화면 둘이 공유하는 컴포저블은 같은 모듈 `component/`에 둔다**(2026-08-19, PR #290) —
  > `groups/canvas/impl`의 C-301 편집 탭과 C-106 배치 화면이 토핑 표시 조각 셋
  > (`ToppingHandleComponents.kt`의 `rememberToppingBaseSize`·`ToppingSelectionStroke`·
  > `ToppingDragHandleButton`)을 나눠 쓰면서, 앞 화면의 private 컴포저블이 그리로 올라갔다.
  > **디자인시스템으로 올리지는 않았다** — 소비처가 한 모듈 안 두 화면이라
  > `feature/common/*` 승격 기준("2개 이상 소비처")과 같은 판단을 모듈 안에서 한 셈이다.
  > 컴포저블이 아닌 기하 계산은 종전대로 `util/ToppingGeometry.kt`에 남는다.
  > 📌 **같은 모듈에 `model/`이 생겼다**(2026-08-27, PR #388) — `ToppingGeometry.kt`가 들고 있던
  > `ToppingCorners`가 `canvas/impl/model/`로 나왔다. 자료구조는 `model/`, 계산은 `util/`,
  > 컴포저블은 `component/`라는 이 저장소의 기존 갈래를 feature 안에서도 따른 것이다.
  > 판정 입력 모디파이어(`component/ToppingHitTestInput.kt`)는 컴포저블이라 `component/`에 있고,
  > 같은 모듈 화면 둘(캔버스 메인·배경 편집)이 나눠 쓴다.
  > 📌 **소비처가 모듈 경계를 넘으면 `:core:designsystem`으로 올린다**(2026-08-22 develop 머지,
  > PR #334) — 토핑 테두리를 그리는 8방향 스탬프가
  > `component/ygtoppingcutout/YGToppingCutoutImage`로 올라갔다. 나눠 쓰는 화면이 누끼 확인
  > (`:feature:segmentation:impl`)과 배치·캔버스(`:feature:groups:canvas:impl`) 셋이라 **모듈 둘에
  > 걸친다** — 앞 항목처럼 한 모듈 `component/`에 두면 다른 모듈이 볼 길이 없고, `feature/common/*`은
  > 화면을 올리는 자리이지 컴포저블 조각을 올리는 자리가 아니다. **가르는 기준은 소비처 수가 아니라
  > 소비처가 몇 모듈에 걸치는가**다. 올린 이유 자체는 [ADR-0025](../adr/0025-topping-border-as-server-field.md)가
  > 쥔다 — 세 화면이 갈라진 그림을 그릴 여지를 구조로 없애려는 것이다.
- **표시 문자열은 `strings.xml` + `stringResource`**(코틀린 리터럴 금지). 화면 전용 정적 라벨은 그 화면의 `feature/*/impl` `res/values/strings.xml`
  (같은 모듈의 여러 화면이 한 파일 공용), **여러 feature가 공유하는 문구**(유효성 에러 등)는 `core:ui` `res/values/strings.xml`([[0016-domain-result-presentation-string-mapping]]).
  `domain`은 표시 문자열을 보유하지 않는다. 미착수 화면에 잔존한 리터럴은 [open-questions](../synthesis/open-questions.md) [2026-07-26]에서 추적.
- **`core:ui` → `:domain` 의존**(#223 develop 머지, 2026-08-13) — 표시 매핑 확장(`NameValidResult.Error.toStringResource`)의 리시버가 도메인 타입이라 필요하다. 방향은 허용(ui → domain)이나 **`implementation`이라 public API 시그니처에 domain 타입이 노출되면서 의존은 숨어 있다** — 소비 feature가 컨벤션 플러그인으로 `:domain`을 직접 갖고 있어 지금은 컴파일된다. 저장소에 `api(...)` 선언이 0건이고 컨벤션 플러그인에 `api` 확장 함수 자체가 없어 승격은 팀 결정 대상 → [open-questions](../synthesis/open-questions.md) [2026-08-13].
- **`core:ui` `reveal/` — 두 화면이 공유하는 드러내기 상태**(#440 develop 머지, 2026-09-04) —
  원격 이미지를 기다리는 동안 무엇을 가리고 언제 낼지를 정하는 순수 상태다. 인터페이스
  `RevealState`(+`AllRevealed`) 하나에 구현이 둘 — `BatchRevealState`(전부 결말날 때까지 가렸다 한 번에,
  C-001 캔버스 토핑)와 `StaggeredRevealState`(간격을 두고 하나씩, G-001 그룹 목록) — 그리고 그리기 쪽
  `Modifier.revealed`다. **`core:designsystem`이 아니라 여기인 이유**는 이것이 컴포넌트가 아니라
  화면 상태이고, 소비처가 `:feature` 둘에 걸치기 때문이다(같은 기준으로 `YGToppingCutoutImage`는
  디자인시스템에 갔다 — 그쪽은 그리는 물건이다). `revealed`는 알파만 0으로 두고 **측정·배치는 살려
  둔다** — 감춘 자리도 배치가 살아 있어야 이미지 요청이 이어진다. 시맨틱은 `hideFromAccessibility`가
  아니라 `clearAndSetSemantics`로 지운다(앞의 것은 그 노드 하나만 감추고 자식 문구는 트리에 남는다).
  같은 라운드에 이 모듈이 **계측 소스셋을 처음 갖게 됐다**(`parfait.test.android`·`parfait.test.compose`).
- **도메인 enum → 디자인시스템 타입 변환은 feature impl의 `util` 패키지에 둔다**(2026-08-19, PR #308·#310
  develop 머지) — 지금 셋이다: `setting/impl/util/ColorChipType.kt`(12종 1:1 + `DEFAULT` → 중립) ·
  `canvas/impl/util/ColorChipType.kt`(같은 규칙, 앞의 것과 글자까지 같다) ·
  `list/impl/util/GrouptagChipType.kt`(12종을 6종으로 접고 `DEFAULT`는 별도 arm).
  셋 다 `NametagChipType`의 모든 상수를 덮는 exhaustive `when`이라 **폴백 분기가 없다** — 그래서
  "모르는 값"을 처리하는 자리는 여기가 아니라 `:data` 매퍼 하나다([ADR-0024](../adr/0024-nametag-chip-unknown-fold.md)). **공용화하지 않은 이유는
  자리가 없어서가 아니다** — 변환의 입력은 `:domain`, 출력은 `:core:designsystem`이고 `core:ui`가 이미
  `:domain`을 보므로 간선 하나면 된다(`:core:designsystem`은 `core:ui`를 모르니 순환도 없다). 막는 것은
  **바로 위 항목의 `implementation`/`api` 가시성 미결**이다. 같은 형태의 매핑을 두 번째로 올리면서 그
  결정을 조용히 굳힐 수 없어 복제를 택했다.
  ⚠️ **컴파일러는 앱이 그 enum에 상수를 더할 때의 arm 누락만 잡는다.** 서버에 새 타입이 생기면 매퍼가
  모르는 문자열을 `NametagChipType.DEFAULT`로 접어 컴파일이 안 깨지고(2026-08-19까지는 `null`로 접었고
  결과는 같았다), 셋 중 하나에서 색만 바꾸는 것도 못 잡는다. **색을 고칠 때는 셋을 함께 본다.**
- **`data` 쪽 같은 축의 중복은 걷었다**(2026-08-19 리뷰 지적, PR #310 develop 머지) — `String? → NametagChipType`이
  group·parfait 매퍼에 각각 `private`으로 있던 것을 `source/common/mapper/NametagChipTypeMapper.kt`
  `internal` 하나로 모았다. **바로 위 셋과 결론이 갈리는 이유는 가시성 미결이 여기엔 안 걸려서다** —
  입출력이 `:data`·`:domain` 안에서 닫히므로 새 모듈 간선이 0개고, 따라서 `implementation`/`api`
  결정을 조용히 굳힐 위험도 없다. 막는 것이 없으면 복제를 유지할 이유도 없다.
  `source/*`가 데이터소스 도메인별로만 쪼개져 있어 `source/common/mapper`가 새 슬롯이다 —
  **여러 데이터소스가 공유하는 wire→도메인 변환만** 여기 둔다(플랫폼 헬퍼는 `data/utils` 소관).
  📌 **두 번째 입주자는 `ToppingBorderMapper.kt`다**(2026-09-11, PR #496 develop 머지) — parfait·parfaitimage
  매퍼의 `private` 테두리 변환 사본 둘을 `internal fun toToppingBorder` 하나로 모았고, group 매퍼가 그룹 목록
  테두리를 읽으면서 세 번째 소비처가 됐다.

## 현재 수치가 필요하면 코드에서 측정
```bash
# 모듈 목록
grep -E '^\s*include' settings.gradle.kts
# feature 목록
find feature -maxdepth 2 -name build.gradle.kts
```
