---
id: ADR-0031
title: 분석 계층을 :app 에 가두고 화면 ID 매핑을 한 자리에 둔다
status: accepted
date: 2026-09-09
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0013, ADR-0014
related_spec: release-analytics-screen-tracking
related_architecture: navigation-flow
platforms: android
tags: [adr, parfait, analytics]
---

# ADR-0031: 분석 계층을 :app 에 가두고 화면 ID 매핑을 한 자리에 둔다

## 맥락

Firebase 도입은 ADR-0013 이 FCM·Crashlytics 와 함께 Analytics 까지 묶어 이미 결정했고
의존성도 들어와 있으나, 부르는 코드가 없어 이벤트가 한 건도 나가지 않았다. 화면 진입을
집계하려면 화면마다 이름이 있어야 한다. 기획은 `C-001`·`G-001` 같은 화면 ID 체계를 쓰지만
코드에는 그 ID 가 주석 몇 군데에만 있고, 화면의 정체는 feature `:api` 에 흩어진 `NavKey`
26개가 들고 있다. 둘을 잇는 자리를 어디에 둘지가 이 결정이다.

겸해 debug 와 release 가 같은 `applicationId` 를 써서 같은 GA4 속성으로 들어간다는 사실이
수집 정책을 강제한다.

## 결정

**분석 코드를 `:app` 안에 가두고, `NavKey`를 화면 ID 로 바꾸는 매핑을 순수 함수 한 자리에
모은다.**

- `AnalyticsLogger` 인터페이스와 Firebase 구현체, 매핑 함수, 전송 판정 모두 `:app` 의
  `com.teamyg.parfait.analytics` 패키지에 둔다. feature·core 모듈은 바뀌지 않는다.
  `:app` 은 이미 모든 feature `:api` 에 의존하므로 새 의존이 생기지 않는다.
- 화면 진입은 `MainRoute` 한 곳에서만 관찰한다. 이 지점은 세션 사건·푸시 딥링크가 이미 같은
  이유로 모여 있는 자리다. 여러 곳이 구독하면 한 전환이 여러 번 찍힌다.
- **전환 판정은 최상단 키가 아니라 백스택 크기와 최상단 키의 짝으로 한다.**
  `Navigator.goTo` 가 조건 없이 `add` 하므로 같은 값의 키가 겹쳐 쌓일 수 있고, 그때도 화면은
  실제로 바뀐다.
- 매핑 함수는 화면 ID 와 화면 클래스명을 **둘 다 문자열 상수로** 돌려준다. 리플렉션으로
  클래스 이름을 얻으면 release 의 R8 난독화가 값을 뭉갠다.
- **release 에서만 수집한다.** debug 와 release 가 같은 GA4 속성을 쓰므로 개발 트래픽이
  운영 지표에 섞인다. 게이트는 `BuildConfig.DEBUG` 가 아니라 새 `ANALYTICS_IS_DEBUG` 필드이고
  Gradle 프로퍼티로 덮어쓸 수 있어, debug 빌드로도 전송을 확인할 수 있다.

## 대안

- **대안 A — `NavKey` 가 자기 화면 ID 를 든다.** `core:navigation` 에 인터페이스를 두고
  `NavKey` 26개가 각자 구현하면 화면과 ID 가 한 자리에 붙어, 새 화면을 만들 때 빠뜨리기
  어렵다. 그러나 순수한 내비게이션 계약에 분석 관심사가 섞이고, api 모듈 26개를 모두
  고쳐야 한다.
  **→ 기각:** 화면 ID 는 Android 집계 도구의 사정이지 화면의 정체가 아니다. iOS 와 공유할
  일이 없는 관심사를 화면 계약에 심으면, 이벤트 축이 늘 때마다 그 인터페이스가 부푼다.
- **대안 B — Route 마다 직접 호출한다.** 각 Route 의 `LaunchedEffect` 에서 스스로 보낸다.
  화면이 자기 이벤트를 갖는 모양이라 읽기는 쉽다. 그러나 26곳에 흩어지고 뒤로 가기 복귀
  처리가 화면마다 제각각이 된다.
  **→ 기각:** 세션 사건과 푸시 딥링크를 `MainRoute` 한 곳에 모은 근거가 그대로 적용된다.
- **대안 C — 인터페이스를 `core` 로 올린다.** 로깅은 ADR-0014 가 `Logger` 를
  `core:util:jvm` 에 뒀다. 같은 모양으로 맞추면 나중에 feature 모듈이 직접 이벤트를 보낼 때
  옮길 일이 없다.
  **→ 기각:** 로깅은 처음부터 data·domain·feature 가 모두 부르는 것이라 코어가 맞았다.
  분석은 지금 소비처가 `MainRoute` 와 `BaseApplication` 둘뿐이고 둘 다 `:app` 에 있다.
  쓰는 곳이 없는 모듈로 먼저 올리면 배치의 근거가 사라진다. 액션 이벤트가 feature 로 내려가는
  라운드에서 그때 올린다.
- **대안 D — debug 를 별도 Firebase 앱으로 분리한다.** `applicationIdSuffix` 를 달고
  콘솔에 앱을 하나 더 등록하면 개발 트래픽이 운영 속성에 아예 닿지 않는다. 가장 깔끔하다.
  **→ 기각:** `google-services.json`·카카오 키·서명·FCM 토큰 등록까지 연쇄로 바뀐다.
  화면 계측 한 축을 넣는 라운드가 감당할 범위가 아니고 되돌리기도 어렵다.

## 영향

**긍정**

- 매핑 전체가 한 파일에 있어 빠진 화면을 눈으로 셀 수 있고, 순수 함수라 `NavKey` 26개를
  넣어 기대 ID 와 대조하는 유닛 테스트가 그대로 성립한다.
- 전송 판정을 `ScreenViewTracker` 한 곳에 모아 `snapshotFlow` 없이 JVM 테스트로 덮인다.
  그 객체가 컴포지션 밖에 살아 Activity 재생성이 만드는 헛 이벤트도 함께 막는다.
  스코프는 `Navigator` 와 같은 `@ActivityRetainedScoped` 다 — 더 넓게 두면 트래커가
  `Navigator` 보다 오래 살아, 새로 선 `Navigator` 의 첫 화면을 낡은 값과 같다고 보고 버린다.
- 분석 도구를 바꾸더라도 구현체 하나만 갈면 된다.

**트레이드오프**

- 새 화면을 추가하며 매핑을 잊으면 컴파일러가 잡지 못한다. `NavKey` 가 sealed 가 아니어서
  `when` 이 빠짐없음을 강제할 수 없다.
- 화면 클래스명을 문자열 상수로 두므로 `NavKey` 이름을 바꿀 때 매핑도 함께 고쳐야 한다.
  둘이 어긋나도 컴파일은 통과한다.
- 기본 상태의 debug 빌드에서는 계측이 아무 일도 하지 않는다. 전송을 눈으로 보려면
  `-Panalytics.isDebug=false` 로 다시 빌드해야 한다.
- `IS_DEBUG` 사용자 속성이 도착한 데이터에서 언제나 `false` 다 — 수집이 켜진 빌드는 정의상
  그 값이 `false` 인 빌드뿐이다. 지금은 지표를 가르지 않는 자리다.
- 전환 판정이 `Navigator` 의 현재 동작(`goTo` 가 무조건 쌓는다)에 기대고 있다. 그 동작이
  바뀌면 판정 기준도 함께 봐야 한다.

**위험·방어**

- 매핑에 대응이 없으면 전송하지 않고 경고 로그로 남겨 개발 중에 드러나게 한다.
- 매핑 함수 유닛 테스트가 `NavKey` 26개와 인자로 갈리는 다섯의 모든 조합을 대조하고,
  `ScreenViewTracker` 테스트가 중복 push·뒤로 가기 복귀·재생성 재방출을 각각 본다.
- ✅ 실기기에서 이벤트 도착을 확인했다(2026-09-09). 화면 전환이 실제로 `screen_view` 가 된다.
- ⚠️ 뒤로 가기 복귀·Activity 재생성·사용자 속성 7종은 실기기로 아직 안 봤다.
