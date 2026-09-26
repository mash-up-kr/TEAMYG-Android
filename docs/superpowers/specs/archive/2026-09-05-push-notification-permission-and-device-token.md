---
id: push-notification-permission-and-device-token
title: 알림 권한 안내와 기기 토큰 등록 (POST_NOTIFICATIONS · FCM device token)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-05
related_code:
  - NotificationPermissionGate.kt#NotificationPermissionGate
  - NotificationPermissionManager.kt#NotificationPermissionManager
  - RegisterCurrentDeviceTokenUseCase.kt#RegisterCurrentDeviceTokenUseCase
  - RegisterDeviceTokenUseCase.kt#RegisterDeviceTokenUseCase
  - DeviceTokenProvider.kt#DeviceTokenProvider
  - FirebaseDeviceTokenProvider.kt#FirebaseDeviceTokenProvider
  - ParfaitFirebaseMessagingService.kt#ParfaitFirebaseMessagingService
  - BootstrapSessionUseCase.kt#BootstrapSessionUseCase
  - LoginWithKakaoUseCase.kt#LoginWithKakaoUseCase
  - SignUpUseCase.kt#SignUpUseCase
related_adr: ADR-0004, ADR-0013, ADR-0021
related_spec:
related_architecture: data-layer, module-structure, navigation-flow, state-management
supersedes:
superseded_by:
tags: [spec, parfait, notification, permission]
---

# Spec: 알림 권한 안내와 기기 토큰 등록

## 목표

서버가 토핑 등록 푸시를 보낼 수 있도록 기기 FCM 토큰을 등록하고, 사용자에게 알림 권한을
적절한 시점에 묻는다. **두 축은 서로 독립이다** — 이 스펙의 핵심 결정이 그것이다.

## 범위

- 포함: 기기 토큰 등록 시점, 알림 권한 요청 시점과 거부 처리, API 33 미만 플랫폼 정책.
- 제외: 푸시 수신·채널·딥링크(ADR-0013 소관), 알림 설정 화면, 알림 종류별 on/off 토글,
  토큰 삭제 요청(서버에 엔드포인트가 없다 — `api/notification.md`).

## 결정 1 — 등록과 권한은 별개 축이다

FCM 토큰은 **알림 권한과 무관하게** SDK 가 설치 시점에 발급한다. 따라서 등록을 권한 허용에
매달면 안 된다.

⚠️ **이 브랜치 초판이 그렇게 묶었고 리뷰가 되돌렸다**(커밋 `7e984099` → `a99a899e`).
**출시된 적은 없다** — develop 의 `onNewToken` 은 등록을 아예 부르지 않았고
(`api/notification.md` 가 그 표면을 "호출부 0"으로 기록한다) 초판도 머지 전에 걷혔다.
아래는 그 초판을 그대로 뒀다면 놓쳤을 사용자군이다.

- 로그아웃 후 재로그인 — 서버는 토큰 매핑을 지웠는데 기기 토큰은 그대로라 `onNewToken` 이
  불리지 않고, 권한은 이미 허용이라 안내 게이트가 건너뛴다.
- 기기 교체·재설치 — `onNewToken` 은 로그인 전이라 401 로 유실되고, 이미 그룹이 있으면
  그룹 생성·참여 흐름 자체를 타지 않는다.
- 설정에서 나중에 알림을 켠 사용자.

**권한을 거부한 사용자의 토큰도 등록한다.** 서버는 권한 상태를 모르므로 그 기기에도 발송이
나가고 OS 가 조용히 버린다. 대신 사용자가 나중에 OS 설정에서 알림을 켜면 앱이 아무것도 하지
않아도 즉시 동작한다. 권한 변경을 감지해 재등록하는 장치를 만들지 않기 위한 선택이다.

> ⚠️ 그래서 `notification_outbox` 의 **발송 성공은 실제 도달과 다르다.** 지표를 볼 때 주의한다.

## 결정 2 — 등록 시점은 세션 축 셋

`api/notification.md` 가 서버 `DeviceTokenAdapter.save` 를 근거로 "앱 시작·`onNewToken`·
권한 허용마다 재호출"을 전제한다. 서버는 `token` 을 유일 키로 upsert 하므로 반복 호출이
안전하고, 그 반복이 곧 지난 실패를 메우는 수단이다.

| 시점 | 자리 | 비고 |
|---|---|---|
| 로그인 성공 | `LoginWithKakaoUseCase` | 기존 회원 분기 안, `refreshMyAccount` 뒤 |
| 가입 성공 | `SignUpUseCase` | `refreshMyAccount` 뒤 |
| 앱 진입 | `BootstrapSessionUseCase` | `refreshMyAccount` 성공 분기에서만 |
| 토큰 회전 | `ParfaitFirebaseMessagingService.onNewToken` | 토큰이 바뀔 때 |

**네 자리 모두 `DeviceTokenRegistrar.register()` 하나를 부른다.** `onNewToken` 도 전달받은
값을 쓰지 않고 이 진입점을 탄다 — 등록구가 지금 값을 다시 읽으므로 결과가 같고, 같은 뮤텍스를
타야 세션 축과 겹치지 않는다.

`register()` 는 **`suspend` 가 아니다.** 걸어만 두고 곧장 돌아간다. 부르는 자리가 로그인·가입·
앱 진입이라 사용자가 스피너를 보고 있고, 등록 결과로 그 화면이 달라질 것도 없다. 초판은 세
UseCase 가 등록을 직렬로 await 해서 **로그인 스피너와 스플래시가 FCM `getToken()` + 등록 POST
두 왕복을 그대로 떠안았다.** 최초 설치 직후에는 `getToken()` 이 FCM 등록 왕복까지 포함한다.

실제 실행은 `:data` 의 `DeviceTokenRegistrarImpl` 이 `@ApplicationScope` 에서 한다. 호출자
스코프에 매달면 곧바로 갈아 끼워지는 화면과 함께 등록이 취소된다. 계약이 `:domain` 에 있고
구현이 `:data` 에 있으므로 **`ApplicationScope` 를 `:domain` 으로 올릴 필요가 없다.**

**재시도는 최대 3회, 3초·6초 백오프다.** 무한히 끌지 않는 이유는 실패가 이어지는 상황이 대개
이 스코프보다 오래 가고, 서버가 반복 호출을 전제로 upsert 를 설계했기 때문이다. 나머지는 다음
세션 트리거가 메운다. WorkManager 도, "등록됨" 영속 플래그도 들이지 않는다.

⚠️ **`Mutex` 로 겹침을 막는다.** `api/notification.md` 등록 절이 "같은 신규 토큰으로 두 요청이
동시에 들어가면 두 번째가 유니크 제약 위반으로 500" 을 경고한다. 진행 중이면 두 번째 호출은
대기하지 않고 그냥 돌아간다 — 기다려 봐야 같은 토큰을 한 번 더 올릴 뿐이다.

세션이 없는 경로에서는 부르지 않는다 — 이 엔드포인트는 인증이 필요하다(화이트리스트 밖).
신규 회원 로그인 분기, 필수 약관 미동의, 저장된 토큰이 없는 부트스트랩이 그렇다.

로그인·가입 두 자리는 `refreshMyAccount` 를 이미 삼키기 형태로 부르고 있어 그 결을 따랐다.
`BootstrapSessionUseCase` 는 다르다 — 그쪽의 `refreshMyAccount` 실패는 라우팅을 `ToLogin` 으로
바꾸고 인증 거절이면 `logout()` 까지 부른다. 등록은 그 성공 분기에서만 건다.

부수 효과 — 서버는 등록 시점 access token 클레임에서 `sessionId` 를 꺼내고 `(memberId,
sessionId)` 로 로그아웃 시 토큰을 지운다. 세션마다 재등록하므로 옛 세션이 남지 않는다.

## 결정 3 — API 33 미만은 허용으로 본다

`POST_NOTIFICATIONS` 는 API 33 에 생긴 권한이고 `minSdk` 는 26 이다. 그 아래 플랫폼에는
권한이 정의돼 있지 않아 `checkSelfPermission` 이 **항상 거부로 답하는데** 정작 알림은 OS
설정에서 기본으로 켜져 있다. 버전으로 먼저 가르지 않으면 판정이 사실과 정반대가 된다.

판정은 `core:util:android` 의 `NotificationPermissionManager` 가 소유한다
(`GalleryPermissionManager` 와 같은 자리·같은 형태). `sdkInt` 를 인자로 받아 기본값을
`Build.VERSION.SDK_INT` 로 두었다 — 이 저장소에 Robolectric 이 없어 그래야 갈림을 JVM
테스트로 덮을 수 있다.

같은 판정을 쓰는 자리가 둘이다. 안내 게이트와 `ParfaitFirebaseMessagingService.showNotification`
(포그라운드 알림)이다. 후자는 이 축이 붙기 전부터 같은 결함을 갖고 있었다.

## 결정 4 — 안내 게이트

정책상 A-004(그룹 참여)·A-005(그룹 생성) 완료 직후 캔버스 진입 전에 보여준다. 이미 허용돼
있으면 안내 없이 통과한다.

- 「알림 받기」 → 권한 요청. 「나중에」·바깥 탭·back → 그대로 통과.
- **영구 거부면 앱 설정으로 보낸다.** Android 13+ 는 두 번 거부되면 시스템 다이얼로그를
  띄우지 않고 즉시 거부 콜백을 준다 — 그대로 닫으면 "눌러도 아무 일 없는 버튼"이 된다.
- 별도 안내 모달을 두지 않는다. 기존 문구 하나로 끝낸다.

⚠️ **`shouldShowRequestPermissionRationale` 을 두 번 읽어 비교한다.** 이 값은 첫 요청 전에도,
두 번 거부된 뒤에도 `false` 라 **한 번만 읽으면 두 상태를 구분하지 못한다.** 요청 직전 값이
`true` 였다가 콜백에서 `false` 로 떨어진 경우만 이번 요청에서 거부가 확정된 것으로 본다.
한 번만 읽으면 다이얼로그를 바깥 탭으로 닫은 첫 사용자까지 설정으로 보낸다 — 바깥 탭 닫기는
시스템이 거부로 집계하지 않아 `false` 가 그대로 남기 때문이다.

⚠️ **그래서 남는 사각지대가 있다** — 이전 세션에서 이미 두 번 거부해 둔 사용자는 요청 직전
값이 `false` 라 설정으로 보내지 못하고 버튼이 무반응으로 남는다. 닫으려면 "한 번은 요청했다"를
로컬에 영속해야 한다 → 미결.

설정 이동이 실패해도(`ActivityNotFoundException`) 흐름은 잇는다 — `onFinished` 는 반드시 부른다.

**노출 횟수를 제한하지 않는다.** 거부·「나중에」 선택을 영속하지 않으므로 허용 전까지
그룹 생성·참여 흐름마다 다시 뜬다. 최초 1회로 줄이려면 별도 플래그가 필요하다 → 미결.

게이트는 목적지를 들고 대기하는 구조라 두 Route 가 `pendingNavigation` 을
`rememberSaveable` 로 든다. `remember` 로 두면 구성 변경으로 Activity 가 다시 설 때 값이
유실되고, 이펙트가 `Channel` 이라 다시 오지 않아 **서버에서는 처리가 끝났는데 사용자만
이전 화면에 갇힌다.** 프로세스 사망은 이 구조가 막지 못한다 — `Navigator` 백스택이
`@ActivityRetainedScoped` 의 순수 `mutableStateListOf` 라 스플래시로 초기화된다.

## 결정 5 — 이벤트 버스를 `event` 패키지로 모은다

이 브랜치의 두 번째 축이다. 알림과 직접 관계는 없으나 같은 브랜치에 실려 있다.

세션 종료와 푸시 딥링크는 같은 구조(도메인 인터페이스 + `:data` `Channel` 구현 + 앱 루트
단일 수집, [ADR-0021](../../../adr/0021-token-refresh-forced-logout.md))인데 패키지가
`repository/session`·`repository/push`·`data/session`·`data/push` 넷으로 흩어져 있었다.
`domain/event`·`data/event` 로 모은다. 둘 다 Repository 가 아니므로 `repository/` 아래
있을 이유가 없었다.

이름 규칙도 푸시 쪽으로 통일한다 — **인터페이스가 `~EventBus`, 구현이 `~Impl`.** 전에는
세션만 인터페이스가 `SessionEventSource`, 구현이 `SessionEventBus` 라 두 축의 규칙이
반대였다. `Source` 는 이 저장소에서 `LocalDataSource`·`RemoteDataSource` 계열 이름이라
이벤트 구독구에 붙으면 오독을 부른다.

⚠️ **계약의 비대칭은 그대로 남는다** — `SessionEventBus` 는 구독만 내놓고 발행
(`postForcedLogout`)은 `:data` 구현에만 있다. `PushDeepLinkEventBus` 가 `post` 를 계약에
함께 두는 것과 다르다. 발행 자리가 `TokenAuthenticator` 하나뿐이라 밖으로 낼 이유가 없어서다.
`architecture/data-layer.md` 가 이 차이를 의도된 설계로 적어 두었다.
✅ **머지 회차에 갱신했다**(2026-09-05, PR #450 develop 머지) — [data-layer](../../../architecture/data-layer.md)
「이벤트 버스 개명」·[module-structure](../../../architecture/module-structure.md) domain/data 패키지 목록·
[ADR-0021](../../../adr/0021-token-refresh-forced-logout.md) 정정.

## 결정 6 — 등록 토큰 축에 남는다 (FID 전환은 서버와 함께)

`firebase-messaging` 25.1.0 이 **`FirebaseMessaging.getToken()`·`deleteToken()` 과
`FirebaseMessagingService.onNewToken()` 을 전부 deprecated 로 돌렸다.** 이 브랜치가 쓰는
API 가 그 셋이다(25.1.2 소스로 확인).

대체는 **FID(Firebase Installation ID) 기반 등록**이고, 모양이 다르다.

| | 지금(등록 토큰) | 대체(FID) |
|---|---|---|
| 값 얻기 | `getToken(): Task<String>` — 값을 돌려준다 | `register(): Task<Void>` — **값을 안 돌려준다** |
| 값 수신 | `onNewToken(token)` | `onRegistered(installationId)` |
| 서버 발송 | Admin SDK `Message.setToken(...)` | Send API 의 `fid` 필드 |

⚠️ **두 API 는 매니페스트 플래그로 갈리고 서로 배타적이다.**
`firebase_messaging_installation_id_enabled` 가 없으면 `register()` 가
`IllegalStateException` 을 던지고, **있으면 반대로 `getToken()` 이 던진다.** 앱이 절반만
옮길 수 없다.

**그래서 지금은 옮기지 않는다.** 근거 셋이다.

1. **서버가 먼저 움직여야 하고, 순서를 뒤집을 수 없다.** `TEAMYG-SERVER` 의
   `FcmNotificationSender` 는 `Message.builder().setToken(token)` 으로 보낸다(Admin SDK
   **9.9.0**). 그 판의 `Message.Builder` 에는 **`setFid` 가 없다**(jar 로 확인). `setFid` 는
   **9.10.0** 부터 있다. 즉 앱만 옮기면 FID 가 `setToken` 자리에 들어가고 **모든 발송이
   실패한다.** 순서는 ① 서버가 9.10.0 으로 올리고 `setFid` 로 바꾼다 ② 그 뒤 앱이 플래그를
   켠다 뿐이다 — 앱 플래그를 켜는 순간 `getToken()` 이 던지므로 되돌아갈 중간 상태가 없다.
2. **급하지 않다.** Firebase 문서가 두 방식을 "fully co-supported" 로 적고 제거 일정을 두지
   않았다. 기존 등록 토큰은 계속 동작한다.
3. **이 브랜치의 범위를 넘는다.** 다만 **구조가 통째로 바뀌지는 않는다** —
   `FirebaseInstallations.getId()` 로 FID 를 당겨올 수 있어 지금의 pull 모델이 그대로
   성립한다. `DeviceTokenRegistrar` 와 세션 트리거 넷은 남고, 바뀌는 것은
   `FirebaseDeviceTokenProvider` 의 구현과 `onNewToken` → `onRegistered` 콜백,
   그리고 매니페스트 플래그다.

**전환을 다시 볼 조건**: ① 서버가 Admin SDK 9.10.0 이상으로 올라가고 `setFid` 로 바뀌거나,
② Firebase 가 등록 토큰 제거 일정을 발표하거나, ③ deprecated API 가 실제로 동작을 멈출 때.
①이 선행 조건이고 나머지 둘은 시급성만 바꾼다.

부수적으로 이번에 **토큰 계약을 비널로 좁혔다.** `getToken()` 은 `Task<String>` 이고 미발급
상태를 `Task` **실패**로 주므로, `currentToken(): DeviceToken?` 의 `null` 분기는 도달하지
않는 경로였다(그것을 검증하던 테스트도 함께 걷었다). 지금은 미발급이 예외로 오고 등록구의
재시도와 다음 세션 트리거가 메운다.

## 파일 구성

| 파일 | 역할 |
|---|---|
| `core/util/android/permission/NotificationPermissionManager.kt` | 버전 갈림 판정과 `shouldShowRationale` 노출 |
| `feature/groups/enter/impl/component/NotificationPermissionGate.kt` | 안내 모달과 권한 요청 |
| `domain/notification/DeviceTokenProvider.kt` | 현재 토큰 읽기 계약(Repository 가 아니라 `repository/` 밖) |
| `domain/notification/DeviceTokenRegistrar.kt` | 등록을 걸기만 하는 계약(`suspend` 아님) |
| `data/notification/DeviceTokenRegistrarImpl.kt` | 앱 스코프 실행·뮤텍스·재시도 |
| `app/push/FirebaseDeviceTokenProvider.kt` | 위 구현(Firebase 의존이 `:app` 에 갇혀 있다) |
| `app/push/di/DeviceTokenModule.kt` | 위 바인딩 — `:app` 최초의 Hilt 모듈 |
| `domain/usecase/notification/` | 값 있는 등록 / 값 없는 등록 두 유스케이스 |
| `data/repository/notification/NotificationRepositoryImpl.kt` | 원격 호출과 에러 매핑 |

## 주의 / 열린 질문

- ✅ **develop 에 들어왔다**(2026-09-05, PR #450 `489b14cc`). 대조 결과 **설계와 코드가 어긋난 자리는
  없다** — 등록 시점 넷·`suspend` 아닌 `register()`·앱 스코프·뮤텍스·3회 재시도(3초·6초)·API 33 미만
  허용·두 번 읽기 비교·`rememberSaveable` 대기가 모두 스펙대로다. 위 결정 2 표의 비고만 한 칸
  정정했다(등록을 거는 자리가 `saveSession` 직후가 아니라 `refreshMyAccount` 뒤다).
  [ADR-0013](../../../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정에 등록·권한 결선을 더했고,
  [ADR-0004](../../../adr/0004-hilt-ksp-di.md) 에 `:app` Hilt 모듈 예외를 적었다.

- **게이트 자체에 테스트가 없다.** 이 모듈에 `androidTest` 소스셋이 없어 Compose 테스트
  하니스 신설이 필요하다. JVM 으로 덮인 것은 **API 33 미만에서 `hasPermission` 이 `Context`
  를 묻지 않고 `true` 를 준다**는 것과 두 `NavigateToNextSaver` 왕복뿐이다. 33 이상 분기와
  `shouldShowRationale` 두 번 읽기 비교는 덮이지 않았다.
- **안내 노출 횟수 정책이 미정이다**(위 결정 4).
- **게이트 배선이 두 Route 에 복제돼 있다.** 타입이 달라 공용화하려면 제네릭이나 공통
  인터페이스가 필요하고, 지금은 이득이 얇아 두었다.
- `DeviceTokenModule` 이 `:app` 에 놓여 ADR-0004 의 "DI 모듈은 `:data` `di/` 평면 배치"
  규칙과 어긋난다. ADR-0013 의 Firebase 경계 때문에 불가피하다 → ADR-0004 정정 필요.
