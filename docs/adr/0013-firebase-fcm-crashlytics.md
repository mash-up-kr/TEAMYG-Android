---
id: ADR-0013
title: Firebase 도입 — FCM 푸시 + Crashlytics + Analytics
status: accepted
date: 2026-07-18
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0014, ADR-0021
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0013: Firebase 도입 — FCM 푸시 + Crashlytics + Analytics

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락
캔버스 마감·초대 등 이벤트를 사용자에게 푸시로 알릴 필요가 있고([[캔버스-마감-스케줄]] 등 기획 유스케이스), 출시 후 크래시·사용 지표를 수집할 수단이 없었다. 푸시·크래시 리포팅·기초 애널리틱스의 제공자를 정해야 했다.

## 결정
**Firebase**를 도입해 **Cloud Messaging(FCM) 푸시 + Crashlytics 크래시 리포팅 + Analytics**를 앱에 통합한다(PR #139 `feature/firebase-setup`).

- 의존은 **`app` 모듈에 집중**. `app/build.gradle.kts`에서 `firebase-bom`(BoM) + `firebase-analytics`·`firebase-messaging`·`firebase-crashlytics`를 붙이고, Gradle 플러그인 `com.google.gms.google-services`·`com.google.firebase.crashlytics`를 적용(루트 `build.gradle.kts`는 `apply false`로 선언만). 버전은 `gradle/libs.versions.toml`(`firebase-bom`·`firebase`·`firebase-crashlytics` + 별칭 `google-firebase`·`google-firebase-crashlytics`).
- **FCM 수신**: `app`의 `fcm/YGFirebaseMessagingService`(`FirebaseMessagingService` 상속). `onMessageReceived`에서 알림 권한 확인 후 `NotificationCompat`로 `CHANNEL_ID = "fcm_default_channel"` 채널에 표시. 토큰 로그는 [[0014-logging-abstraction-kermit|Logger]] 사용(`fcmLogger`).
- **토큰 서버 전송은 후속**: `onNewToken`은 현재 `TODO("서버에 FCM 토큰 전송")` — 원격 연동이 준비되면 구현(원격 네트워킹 자체가 후속 과제, [[data-layer]]).

> 🔁 **FCM 축이 철회됐다 (2026-08-22, PR #325 `refactor/#316-remove-fcm-and-notification-permission` develop 머지).**
> 위 결정 셋 중 **Cloud Messaging만 걷혔고 Crashlytics·Analytics는 그대로**다. 사라진 것은
> `app`의 `fcm/YGFirebaseMessagingService`·`Logger.kt`(`fcmLogger`·`tokenLogger`)·`MainActivity`의
> 토큰 조회와 알림 채널 생성·권한 요청, 매니페스트의 서비스 등록과 `POST_NOTIFICATIONS`,
> 그리고 `firebase-messaging` 의존(`app/build.gradle.kts`·`gradle/libs.versions.toml`)이다.
> `firebase-bom`·`firebase-analytics`·`firebase-crashlytics`와 두 Gradle 플러그인은 남는다.
>
> **철회 근거는 "쓰이지 않았다"** 하나다 — `onNewToken`이 서버에 닿은 적이 없어 토큰은 로그로만
> 갔고(위 "토큰 서버 전송은 후속"이 그대로 남아 있었다), 권한과 채널은 오직 그 서비스가 알림을
> 띄우려고 존재했다. 그 상태로 두면 **결선된 적 없는 기능 때문에 첫 실행마다 알림 권한을 묻는다**는
> 것이 걷어낸 이유다. 즉 "푸시는 그룹 협업 앱에서 사실상 필수"라는 위 [대안](#대안) 절의 판단이
> 뒤집힌 것이 아니라, **결선 없는 껍데기를 출시 경로에서 뺀 것**이다. 푸시를 실제로 붙일 때
> 되살릴 결정이므로 이 ADR을 폐기하지 않고 여기 정정으로 남긴다.
>
> 되살릴 때 다시 정해야 하는 것: 토큰 라이프사이클(등록·갱신·로그아웃 시 폐기)과 알림 권한을
> **언제 묻는가**다. 지금 걷어낸 형태는 `MainActivity.onCreate`에서 무조건 묻는 것이었고,
> 그 시점은 사용자가 알림이 무엇에 쓰이는지 알기 전이다.

> 🔁 **Crashlytics 축이 네이티브까지 넓어졌다 (2026-08-26, PR #372 develop 머지).**
> `app/build.gradle.kts`에 **`firebase-crashlytics-ndk`**가 붙었다. 근거는 AAR로 들어오는
> CameraX·DataStore의 네이티브 라이브러리가 **시그널로 죽으면 JVM 예외가 남지 않아** 기본
> 수집기의 그물에 안 걸린다는 것이다. 즉 이 추가가 겨냥하는 것은 우리 코드가 아니라
> **의존 라이브러리의 `.so`**다.
> ⚠️ **`nativeSymbolUploadEnabled`는 켜지 않았다** — 자체 네이티브 빌드가 없어 올릴 언스트립 심볼이
> 없고, 서드파티 `.so`는 심볼 없이 **주소만** 남는다. 그래서 이 수집기가 실제로 얼마나 좁혀 주는지,
> 그리고 리포트가 콘솔에 도착하는지는 **아무도 확인하지 않았다**(릴리즈를 설치해 본 적이 없다)
> → [open-questions](../synthesis/open-questions.md) OQ-P-309.
> 자체 네이티브 코드가 생기면 그때 이 스위치를 다시 본다.

> 🔁 **FCM 축이 되살아났다 (2026-09-05, PR #446 `feature/push-notification-deeplink` · #447
> `feature/push-fcm-service` develop 머지).** 위 철회 정정(2026-08-22)을 지우지 않는다 — 두 결정이
> 모두 이력이고, 되살린 근거가 걷어낸 근거와 정확히 짝을 이룬다. **철회 근거는 "보낼 서버가
> 없었다"였고, 서버가 실제로 보내기 시작하면서 그 전제가 사라졌다**
> ([api/notification.md](../api/notification.md) "서버가 보내는 푸시").
>
> 돌아온 것과 **이름·자리가 달라진 것**:
>
> | 걷어낸 것(2026-08-22) | 되살아난 것(2026-09-05) |
> |---|---|
> | `app` `fcm/YGFirebaseMessagingService` | `app` `push/ParfaitFirebaseMessagingService` |
> | 채널 id `fcm_default_channel`(앱이 정한 값) | 채널 id `parfait_default` — **서버가 못 박아 보내는 값을 앱이 따랐다**(`PUSH_NOTIFICATION_CHANNEL_ID`, OQ-P-352 ①) |
> | 채널 생성이 `MainActivity.onCreate` | `BaseApplication.onCreate`(`createPushNotificationChannel`, `IMPORTANCE_HIGH` · 표시 이름·설명은 `strings.xml`) |
> | `fcmLogger`·`tokenLogger` | `Loggers.create("Push")` 하나([[0014-logging-abstraction-kermit]]) |
> | 매니페스트 `POST_NOTIFICATIONS` **선언 + 런타임 요청** | **선언만 돌아왔다** — 요청하는 코드는 develop에 없다 → OQ-P-358 |
> | `onNewToken`이 `TODO("서버에 FCM 토큰 전송")` | `onNewToken`이 로그만 남긴다 — **등록 표면(`NotificationRemoteDataSource`, PR #437)이 이미 있는데 부르지 않는다**(OQ-P-341 ②) |
>
> **함께 들어온 것은 딥링크 축**이다(#446). 이것은 철회 전에 없던 새 결정이라 위 표에 짝이 없다 —
> `:domain`의 `PushDeepLink`(sealed) · `PushNotificationType` · `PushDeepLinkEventBus`와 `:data`의
> `PushDeepLinkEventBusImpl`(`Channel(CONFLATED)`), `app`의 `PushDeepLinkParser`·`toPushDeepLinkOrNull`이다.
> **세션 종료 이동의 구조를 그대로 재사용한다**(도메인 인터페이스 + `:data` 채널 구현 + 앱 루트 단일
> 수집, [ADR-0021](0021-token-refresh-forced-logout.md)) — 그래서 별도 ADR을 만들지 않고 여기 적는다.
> 다른 점은 하나다: 발행자가 `app`의 `MainActivity`라 **`post`를 도메인 인터페이스에 함께 뒀다**
> (세션 쪽은 구독구만 도메인에 있고 발행은 `:data` 구현이 갖는다).
> 배선은 [navigation-flow](../architecture/navigation-flow.md) "푸시 딥링크 이동", 계약 대조는
> [api/notification.md](../api/notification.md) Android 매핑에 있다.
>
> **되살릴 때 다시 정하라고 남긴 둘 중 하나만 답해졌다.** 토큰 라이프사이클은 여전히 미정이고
> (`onNewToken`이 등록을 안 부른다), **알림 권한을 언제 묻는가는 답이 아니라 공백으로 돌아왔다** —
> 걷어낸 형태(`MainActivity.onCreate`에서 무조건)를 되풀이하지 않은 대신 **묻는 코드 자체가 없어**
> Android 13 이상에서는 사용자가 OS 설정으로 직접 켜기 전까지 알림이 표시되지 않는다.
> 둘 다 미머지 브랜치 `feature/push-notification-permission`가 건드리는 자리다(OQ-P-341 ②③④).

> ✅ **남은 둘이 2026-09-05 PR #450 `feature/push-notification-permission` 으로 답해졌다.**
> 위 표의 마지막 두 행이 닫힌다.
>
> - **토큰 라이프사이클** — `onNewToken` 이 이제 `DeviceTokenRegistrar.register()` 를 부르고,
>   등록 시점이 그 하나가 아니라 **세션 축 넷**이다(로그인·가입·앱 진입의 성공 분기 + `onNewToken`).
>   **권한과 독립**이라는 것이 이 결정의 핵심이다 — FCM 토큰은 알림 권한과 무관하게 SDK 가 설치
>   시점에 발급하므로, 등록을 권한에 매달면 재로그인·기기교체·재설치 사용자가 등록 경로에 닿지
>   못한다. **권한을 거부한 사용자의 토큰도 등록한다**(서버가 권한 상태를 모르니 발송이 나가고 OS 가
>   버리지만, 사용자가 나중에 설정에서 켜면 앱이 아무것도 안 해도 동작한다).
> - **알림 권한을 언제 묻는가** — **A-004·A-005 완료 직후, 캔버스 진입 전**이다
>   (`NotificationPermissionGate`). 걷어낸 형태("첫 실행마다 무조건")로 돌아가지 않았고, 이미
>   허용돼 있으면 안내 없이 통과한다. 영구 거부면 앱 설정으로 보낸다.
> - 함께 **API 33 미만 정책**이 생겼다 — `POST_NOTIFICATIONS` 가 그 아래 플랫폼에 정의돼 있지 않아
>   `checkSelfPermission` 이 항상 거부를 답하는데 알림은 기본으로 켜져 있다. 판정을
>   `NotificationPermissionManager` 로 빼고 **허용으로 본다.** 되살아난
>   `ParfaitFirebaseMessagingService.showNotification` 이 그 함정을 그대로 밟고 있었으므로
>   **그 기기군의 포그라운드 알림을 전부 버리던 것**이 이 라운드에 함께 고쳐졌다.
>
> ⚠️ **Firebase 경계가 `:app` Hilt 모듈을 낳았다** — 토큰을 읽는 `FirebaseDeviceTokenProvider` 가
> SDK 를 쓰므로 `:app` 에 있어야 하고, 그 바인딩 `DeviceTokenModule` 도 함께 `:app` 에 생겼다.
> [ADR-0004](0004-hilt-ksp-di.md) 의 "DI 모듈은 `:data` `di/` 평면 배치" 규칙의 **첫 예외**다.
>
> ⚠️ **쓰고 있는 FCM API 셋이 전부 deprecated 다** — `getToken()`·`deleteToken()`·`onNewToken()` 이고
> 대체는 FID 기반이다. **서버가 선행 조건이라 지금은 옮기지 않는다**(Admin SDK 9.9.0 에는 `setFid` 가
> 없다) → OQ-P-362. 근거·전환 조건의 정본은
> [스펙](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md) 결정 6.

## 대안
- **푸시 미도입(로컬 알림만)** — 외부 SDK·GMS 의존 회피. 그러나 서버발 이벤트(마감·초대) 푸시 불가로 핵심 UX 결손.
  **→ 기각:** 그룹 협업 앱에서 푸시는 사실상 필수.
- **크래시/애널리틱스 별도 제공자(Sentry 등) 조합** — 도구별 최적. 그러나 SDK·대시보드 이원화, FCM은 어차피 Firebase 필요.
  **→ 기각:** 이미 세그멘테이션에서 GMS 의존([[0012-mlkit-subject-segmentation]]) → Firebase 단일 스택이 운영 단순.

## 영향

**긍정**
- 푸시·크래시·기초 지표를 단일 콘솔에서 운영. BoM으로 버전 정합 자동.
- Firebase 의존이 `app` 모듈에만 있어 `core/data/domain`은 SDK에 비노출(경계 유지).

**트레이드오프**
- **GMS(Play services) 의존** — GMS 없는 기기에서 푸시·일부 기능 제약(세그멘테이션과 동일 제약).
- `google-services.json`(Firebase 프로젝트 설정 파일) 필요 — 빌드·CI에 비밀 관리 부담. **public repo 커밋 금지** 대상.
- FCM 토큰 라이프사이클(서버 전송·갱신) 미완 → 실제 타겟 푸시는 원격 연동 이후.

**위험·방어**
- 알림 표시 전 `NotificationManagerCompat.areNotificationsEnabled()` 확인, `onMessageReceived`에 `@RequiresPermission(POST_NOTIFICATIONS)` 명시.
- 토큰 서버 전송 미구현은 코드 `TODO` + 본 ADR에 명시 → [open-questions](../synthesis/open-questions.md)에서 추적.

> 🔁 **위 트레이드오프·방어 중 FCM에 걸린 것은 2026-08-22 부로 대상이 사라졌다**(PR #325, 위
> [결정](#결정) 절의 철회 정정). 알림 표시 방어 둘은 코드째 없어졌고 토큰 라이프사이클 미완도
> 추적할 대상이 없다(OQ-P-012 해소). **남는 것은 GMS 의존과 `google-services.json`**이다 —
> 전자는 세그멘테이션([[0012-mlkit-subject-segmentation]])이 같은 의존을 이미 지고 있어 FCM이
> 빠져도 그대로이고, 후자는 Analytics·Crashlytics가 계속 요구한다.

> 🔁 **방어 둘이 2026-09-05에 형태를 바꿔 돌아왔다**(위 되살림 정정). 지금 코드가 하는 것은
> `showNotification` 진입부의 `ContextCompat.checkSelfPermission(POST_NOTIFICATIONS)` 확인
> 하나이고, **`areNotificationsEnabled()` 확인도 `@RequiresPermission` 표기도 없다.**
> 권한이 없으면 조용히 `return` 한다 — 실패가 로그에도 안 남는다. 토큰 라이프사이클 미완은
> 대상이 다시 생겼으므로 OQ-P-341이 이어서 추적한다(OQ-P-012 해소는 그대로 이력이다).
