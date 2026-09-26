---
id: canvas-adaptive-polling
title: 캔버스 폴링 적응형 주기 (Adaptive canvas poll interval)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-10
related_code:
  - CanvasPoller
  - CanvasPollInterval
  - CanvasLocalDataSource#cachedTodayCanvas
  - CanvasVO
  - ParfaitRepositoryImpl#requestTodayCanvasRefresh
  - RequestTodayParfaitRefreshUseCase
  - ParfaitFirebaseMessagingService#onMessageReceived
  - PushDeepLinkParser
  - PushDeepLink.AddTopping
related_adr: ADR-0029
related_spec: canvas-today-ssot-polling
related_architecture:
  - data-layer.md
supersedes:
superseded_by:
tags: [spec, parfait, canvas, polling, push, network]
---

# Spec: 캔버스 폴링 적응형 주기

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

오늘 캔버스 폴링의 고정 주기를 변화 여부에 반응하는 적응형 주기로 바꾼다. 아무도 토핑을 올리지
않는 동안에는 조회를 성기게 하고, 변화가 감지되면 곧바로 촘촘한 주기로 되돌린다.

## 문제

`CanvasPoller`의 주기는 고정 5초이고, 그 값에 붙은 주석이 근거가 없음을 스스로 밝힌다.

```kotlin
/** 실측 전 값이다(OQ-P-320) */
private val CANVAS_POLL_INTERVAL: Duration = 5.seconds
```

캔버스 상세 응답은 실측 6126바이트다. 5초 주기는 화면을 열어 둔 1분마다 약 72KB를 쓴다. 그런데
그 조회의 대부분은 **직전과 완전히 같은 값**을 받는다. 여럿이 함께 꾸미는 화면이라고 해도 토핑이
5초마다 올라오지는 않기 때문이다.

서버에는 조건부 요청 수단이 없다. 응답 헤더가 `cache-control: no-cache, no-store`이고 `ETag`도
`Last-Modified`도 없어서 304로 접을 길이 없다. 서버를 고칠 여유가 없는 상황이므로 앱에서
줄인다.

## 범위

- 포함
  - `:data`에 주기 계산 전담 클래스 `CanvasPollInterval` 신설.
  - `CanvasPoller`가 고정 주기 대신 그 클래스에 묻고, 조회 결과의 변화 여부를 되먹인다.
  - `ParfaitFirebaseMessagingService`가 토핑 푸시를 받으면 갱신을 요청하는 갈래 추가.
  - `CanvasPollInterval` 단위 테스트 신설, `CanvasPollerTest`에 주기 관련 케이스 추가.
  - `feature/groups/canvas/impl`에 남은 「폴링은 5초마다」 단정 주석 정리. 이 변경으로 한꺼번에
    거짓이 된다.
  - ADR-0029 개정. 그 결정문은 주기 **값**을 정한 적이 없고 「주기 폴링」이라고만 썼으므로,
    적응형 주기 항목을 새로 넣고 되돌리는 계기를 열거표로 붙인다. 「위험·방어」에 남은
    `5초 주기마다` 수치도 함께 정정한다.
- 제외
  - 서버 변경. `ETag`·`If-None-Match`·경량 변경 확인 엔드포인트는 전부 이번 범위 밖이다.
  - 갱신 실패에 대한 백오프. 실패는 주기를 건드리지 않는다(아래 [주기 정책](#주기-정책)).
  - 사용자 입력(터치·스크롤)을 리셋 계기로 삼는 것. 상한이 20초라 이득이 작고, 입력 감지
    배선이 화면마다 늘어난다.
  - 그룹 목록·그룹 상세 등 캔버스 밖의 조회. 폴링을 쓰지 않는다.
  - `data`-only FCM 페이로드 처리. 지금 스펙에 그런 payload가 없다.

## 설계

### 주기 정책

주기는 10초에서 시작해 변화 없는 조회마다 한 칸씩 올라가고 20초에서 멈춘다.

| 단계 | 대기 |
|---|---|
| 0 | 10초 |
| 1 | 15초 |
| 2 | 20초 |

상한을 20초로 둔 이유는 **상한이 곧 최악의 체감 지연**이기 때문이다. 푸시가 그 지연을 메우도록
설계했지만 푸시는 보장 경로가 아니다. 알림 권한을 거부한 기기에는 오지 않고 FCM 자체도 지연될 수
있다. 20초는 분당 약 18KB로 절약을 거의 그대로 얻으면서, 푸시가 오지 않았을 때의 최악의 지연을
20초로 묶는다.

한 칸씩 올리는 이유는 배수 증가(10 → 20)가 상한 20에서 단계 하나뿐이라 "조금 성기게"라는 중간
상태를 갖지 못하기 때문이다. 세 단계면 약 25초 만에 상한에 닿는다.

### `CanvasPollInterval`

주기 계산만 아는 클래스를 따로 둔다. 시간·코루틴·네트워크를 모른다.

| 표면 | 시그니처 | 뜻 |
|---|---|---|
| 현재 주기 | `fun current(groupId: GroupId): Duration` | 다음 대기 시간 |
| 변화 있음 | `fun onChanged(groupId: GroupId)` | 단계를 0으로 |
| 변화 없음 | `fun onUnchanged(groupId: GroupId)` | 단계를 한 칸 올린다(상한에서 멈춤) |
| 리셋 | `fun onReset(groupId: GroupId)` | 단계를 0으로 |
| 정리 | `fun forget(groupId: GroupId)` | 그 그룹의 단계를 지운다 |
| 전체 정리 | `fun forgetAll()` | 모든 그룹의 단계를 지운다 |

`onChanged`와 `onReset`은 하는 일이 같지만 부르는 자리의 뜻이 달라 나눈다. 앞은 조회 결과가
말해 준 것이고, 뒤는 바깥 사건(진입·쓰기·푸시)이 명령한 것이다. 호출부를 읽을 때 어느 쪽인지
드러나야 리셋 계기 열거표와 코드가 대조된다. `onReset`은 `acquire`와 `refreshNow` 둘에서 불린다.

상태는 그룹별 단계 인덱스 하나뿐이다. `CanvasPoller`가 이미 `synchronized(lock)`으로 자기 맵
셋을 지키고 있으므로, 이 클래스도 같은 규칙 아래에서만 불린다. 스스로 락을 들지 않는다 —
락을 둘로 나누면 "주기를 읽는 것"과 "타이머를 다시 세우는 것" 사이가 갈라진다.

**이 클래스를 폴러 안에 인라인하지 않는 이유**는 `CanvasPoller`가 이미 참조 계수·세대·진행 중
표시라는 동시성 규칙 셋을 한 락 위에서 굴리고 있어서다. 램프를 그 안에 섞으면 네 번째 규칙이
되고, 램프만 따로 테스트할 방법이 사라진다. 순수 클래스로 떼면 램프 전 구간이 값만으로 고정된다.

### 변화 판정

`CanvasPoller#refresh`는 이미 조회 직전에 `local.cachedTodayCanvas(groupId)`로 이전 값을 읽고
있다(오늘 조회를 쓸지 상세 조회를 쓸지 가르는 데 쓴다). 그 값과 새 응답을 그대로 비교한다.

`CanvasVO`는 `parfaitId`·`groupName`·`date`·`status`·`lastClosedDate`·`members`·`background`·
`toppings`만 가진 data class다. 요청 시각처럼 매번 달라지는 필드가 없어서 구조적 동등성이 곧
"서버가 준 캔버스가 달라졌는가"다. 별도 비교 함수를 만들지 않는다.

판정은 캐시에 싣는 것과 같은 `synchronized` 블록 안에서, 세대가 유효할 때만, 그리고 **그 그룹에
구독자가 있을 때만** 한다. 버려진 세대의 응답이 주기를 움직이면 안 되고, 화면을 보지 않는 동안
푸시로 나간 갱신이 단계를 올려 두면 다음 진입의 첫 주기가 10초가 아니게 된다.

이전 값이 `null`인 첫 조회는 `onChanged`로 친다. 화면이 막 열려 캔버스를 처음 받은 순간은 가장
촘촘해야 할 때다.

### 리셋 계기

ADR-0029가 "갱신이 나가는 시점 자체는 여전히 열거한다"를 규약으로 세웠다. 주기를 되돌리는
계기도 같은 형식으로 열거한다.

| 계기 | 자리 | 동작 |
|---|---|---|
| 화면 진입(첫 구독) | `CanvasPoller#acquire` | 즉시 갱신 + `onReset` — 단계 0에서 시작 |
| 조회 결과가 캐시와 다름 | `CanvasPoller#refresh` | `onChanged` — 다음 회차부터 10초 |
| 조회 결과가 캐시와 같음 | `CanvasPoller#refresh` | `onUnchanged` — 한 칸 올린다 |
| 쓰기 성공 후 강제 갱신 | `CanvasPoller#refreshNow` | 즉시 갱신 + `onReset` |
| 토핑 푸시 수신 | `RequestTodayParfaitRefreshUseCase` | 즉시 갱신 + `onReset`(같은 경로) |
| 마지막 구독 해제 | `CanvasPoller#release` | `forget` — 다시 들어오면 10초부터 |
| 갱신 실패 | `CanvasPoller#refresh` | **주기를 건드리지 않는다** |
| 세션 종료 | `CanvasPoller#stopAll` | `forgetAll` — 구독자 없는 그룹의 단계까지 지운다 |

**실패를 램프에서 뺀 이유**: 실패를 "변화 없음"으로 세면 서버가 흔들리는 동안 주기가 20초로
늘어져, 회복이 가장 필요한 순간에 가장 늦게 회복한다. 실패 전용 백오프가 필요해지면 램프와 다른
축으로 따로 설계한다.

`refreshNow`는 지금도 갱신 뒤에 `restartPollTimerLocked`로 타이머를 다시 세운다. 여기에
`onReset` 한 줄을 더한다. 배경 저장·토핑 추가·토핑 삭제가 모두 이 경로를 지나므로 쓰기 계기는
따로 배선하지 않는다.

주기는 대기 직전에 `current`로 매번 다시 묻는다. 도는 중에 단계가 바뀌어도 다음 회차부터
반영되고, 이미 시작된 대기를 중간에 끊지 않는다. 대기를 끊어야 하는 자리(진입·쓰기·푸시)는
전부 `restartPollTimerLocked`를 이미 부르고 있다.

### 푸시 경로

`ParfaitFirebaseMessagingService#onMessageReceived`는 지금 `message.notification`이 없으면
곧장 return하고 `data`는 보지 않는다. 알림 표시는 그대로 두고 그 뒤에 갈래를 하나 더한다.

```
PushDeepLinkParser.parse(route, groupId, type)
  → PushDeepLink.AddTopping(groupId)  → RequestTodayParfaitRefreshUseCase(GroupId(groupId))
  → 그 밖                              → 아무 일도 하지 않음
```

도메인·저장소 표면을 새로 만들지 않는다. `RequestTodayParfaitRefreshUseCase`가 이미 있고
`ParfaitRepositoryImpl#requestTodayCanvasRefresh`가 `CanvasPoller#refreshNowAsync`로 이어진다.
푸시가 원하는 것("즉시 한 번 받고 주기를 되돌린다")과 그 경로가 하는 일이 정확히 같다.

리마인드 푸시(`REMIND_AM`·`REMIND_PM`)는 `route`가 `group`이라 파서가 `PushDeepLink.GroupList`로
가른다. 타입을 따로 거르는 코드가 필요 없다. 이 갈림이 없으면 하루 두 번 아무 이유 없이 주기가
리셋된다. `route` 값은 `PushNotificationRouteType`의 키 그대로 **소문자**다(`canvas`·`group`).

`PushDeepLinkEventBus`는 쓰지 않는다. 그것은 `Channel` 기반 단일 소비자이고 **알림을 탭했을 때의
이동**을 나르는 축이라, 도착 신호로 재사용하면 푸시가 올 때마다 화면이 옮겨 간다.

**포그라운드에서만 동작한다.** `notification` 블록이 있는 payload를 앱이 백그라운드·종료
상태에서 받으면 시스템이 알림을 직접 띄우고 이 콜백을 거치지 않는다. 다만 폴러의 수명이 화면
구독에 매여 있어(ADR-0029) 그때는 폴링도 돌지 않으므로 잃는 것이 없다.

캔버스를 보고 있지 않은데 푸시가 오면 `refreshNowAsync`가 갱신 한 번만 내보낸다. 그 안의
`synchronized` 블록이 구독자가 없을 때 타이머를 다시 세우지 않으므로 폴링이 살아나지 않는다.
기존 가드가 그대로 막는다.

## 오류 처리

지금 동작을 그대로 유지한다.

- 갱신 실패는 `refreshFailures`로 흘러 첫 조회를 기다리는 화면이 덮개를 내리는 계기가 된다.
  이 스펙은 그 축을 건드리지 않는다.
- 푸시가 실린 `groupId`가 숫자가 아니거나 0 이하이면 `PushDeepLink.AddTopping.parse`가 `null`을
  낸다. 그 경우 갱신을 요청하지 않는다.
- 푸시로 나간 갱신이 실패해도 알림은 이미 표시된 뒤다. 사용자에게 따로 알리지 않는다 — 다음
  주기 조회가 같은 값을 다시 받으러 간다.

## 테스트

`CanvasPollInterval`은 순수 클래스라 JVM 단위 테스트로 전 구간을 덮는다.

- 초기값이 10초다.
- `onUnchanged`를 거듭하면 10 → 15 → 20으로 올라가고 20에서 더 올라가지 않는다.
- `onChanged`·`onReset`이 어느 단계에서든 10초로 되돌린다.
- 그룹이 다르면 단계가 섞이지 않는다.
- `forget` 뒤에는 다시 10초에서 시작한다.

`CanvasPollerTest`는 이미 `runTest` 가상 시간 위에서 돈다. 세 건을 더한다.

- 같은 응답이 이어지면 조회 간격이 10 → 15 → 20으로 늘어난다.
- 응답이 달라지면 다음 간격이 10초로 돌아온다.
- 갱신이 실패해도 간격이 늘지 않는다.

`RemoteMessage`를 계측 없이 만들 수 없다는 제약은 그대로다. 그래서 토핑만 고르는 판정을
`Map<String, String>.toppingGroupIdOrNull()`로 서비스 밖에 꺼내 순수 `Map` 테스트로 잠갔고
(`PushDeepLinkIntentTest`에 6건), 서비스에는 그 결과를 받아 UseCase를 부르는 줄만 남겼다.

## 후속

서버에 `ETag`와 `If-None-Match`가 생기면 이 스펙의 **변화 판정을 걷는다.** 304가 곧 "변화 없음"
이므로 클라이언트가 값을 비교할 이유가 사라지고, 본문 전송도 함께 사라져 성긴 주기의 이득보다
크다. 그때 `CanvasPollInterval`은 남고 `refresh`의 비교만 응답 코드 판정으로 바뀐다.

## 미결

- 상한 20초와 단계 폭(10/15/20)은 실측이 아니라 응답 크기와 체감 지연으로 정한 값이다.
  실사용 데이터가 쌓이면 다시 본다(OQ-P-320의 후속).
- 푸시 도달률을 모른다. 알림 권한 거부 비율이 높으면 상한을 더 낮춰야 할 수 있다.

## as-built (2026-09-10, PR #482 `efa771503`)

설계대로 들어왔다. `CanvasPollInterval`의 표면 여섯과 단계 셋(10/15/20), 리셋 계기 여덟 자리,
푸시가 `RequestTodayParfaitRefreshUseCase`를 그대로 부르는 배선이 위 표와 같다. 실패가 램프를
건드리지 않는 성질도 그대로다. 구현하며 갈린 것은 셋이다.

- **`acquire`의 락이 하나로 합쳐졌다**(`4a167e8a1`). `onReset` + `restartPollTimerLocked`를
  구독자 판정과 같은 `synchronized` 블록 안에 넣는다 — `refreshNow`가 이미 같은 이유로 그렇게
  하고 있었고, 진입 쪽만 갈라 두면 그 사이에 마지막 `release`가 끼어들어 구독자가 없는데도 폴
  잡이 살아난다. 스펙은 `refreshNow`에 대해서만 이 규칙을 적었다.
- **`CanvasPoller`가 `CanvasPollInterval`을 기본 인자로 받는다**(`= CanvasPollInterval()`).
  주입은 Hilt가 하고 기본값은 테스트가 폴러만 세울 때 쓴다. `clock`이 이미 같은 모양이다.
- **토핑 판정이 서비스 밖으로 나왔다**(`45145e1a0`). `RemoteMessage`를 계측 없이 만들 수 없어
  `Map<String, String>.toppingGroupIdOrNull()`을 `PushDeepLinkIntent.kt`에 두고 순수 `Map`
  테스트로 잠갔다(`PushDeepLinkIntentTest` 8건). 서비스에는 그 결과로 UseCase를 부르는 줄만
  남는다. 위 「테스트」 절은 이 결과를 이미 반영해 적혀 있다.

⚠️ **같은 PR이 폴링과 무관한 화면 수정 다섯을 함께 실었다** — 로딩 덮개·인디케이터 최소 노출,
캔버스 첫 페인트 뒤 덮개 억제, 정원 1 그룹의 알림 권한 건너뛰기, 그룹 추가 메뉴의 딤·칩 반응.
그 다섯의 as-built 는 [canvas-feedback-fixes](2026-09-10-canvas-feedback-fixes.md)에 있다.
