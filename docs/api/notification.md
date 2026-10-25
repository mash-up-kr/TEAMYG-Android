---
id: notification
title: 알림(기기 FCM 토큰 등록 · 푸시 발송)
server_module: http/notification
server_commit: 8b4a94b
verified: 2026-10-25
android_status: partial
related_spec:
related_adr: ADR-0013, ADR-0017
tags: [api, parfait, server-contract, notification]
---

# 알림(기기 FCM 토큰 등록 · 푸시 발송) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

이 도메인은 **HTTP 왕복 하나(기기 토큰 등록)와 서버→앱 단방향 푸시**로 이루어진다. 아래
[서버가 보내는 푸시](#서버가-보내는-푸시--그룹-이벤트-알림outbox)부터가 **HTTP 응답 스키마가 아닌 계약**이라
엔드포인트를 세는 방식으로는 이 계약의 변화가 보이지 않는다.

푸시는 **5종**이다. `data.type` 문자열 기준이다.

| `data.type` | 트리거 | 수신자 | 경로 | `data.route` |
|---|---|---|---|---|
| `TOPPING` | 이미지 토핑 **신규** 배치(`PlaceParfaitImageService.place`) | 같은 그룹의 남은 구성원 − 작성자 | Outbox · 단건 | `canvas` |
| `BACKGROUND` | 캔버스 배경 변경(`ChangeParfaitBackgroundService.change`) | 같은 그룹의 남은 구성원 − 변경자 | Outbox · 단건 | `canvas` |
| `TEXT` | 텍스트 토핑 배치(`PlaceParfaitTextService.place`) | 같은 그룹의 남은 구성원 − 작성자 | Outbox · 단건 | `canvas` |
| `REMIND_AM` | 매일 10:00 KST 배치 | 활성 그룹이 있고 토큰이 있는 이용자 전원 | 배치 · 멀티캐스트 | `group` |
| `REMIND_PM` | 매일 20:00 KST 배치 | 같음 | 배치 · 멀티캐스트 | `group` |

**캔버스 마감·그룹 초대·텍스트 수정/삭제·토핑 재배치·토핑 일괄 수정은 발송 코드가 없다.**

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| POST | `/api/v1/notifications/devices` | **필요**(화이트리스트 밖) | `RegisterDeviceTokenRequest` | 없음(204, envelope 없음) | **구현됨·결선됨**(PR #450 — 세션 축 넷에서 부른다: 로그인·가입·앱 진입의 성공 분기 + `onNewToken`) |

**삭제 엔드포인트는 없다.** 커밋 제목이 "저장/삭제 API"라고 적지만 HTTP 표면은 등록 하나뿐이고,
삭제는 다른 도메인·발송 경로의 부수 효과로만 일어난다
(아래 [기기 토큰이 지워지는 네 경로](#기기-토큰이-지워지는-네-경로)).

## 엔드포인트 상세

### POST /api/v1/notifications/devices

- **인증**: **필요**. `SecurityConfig` 화이트리스트가 신설 때도 이번 delta에서도 바뀌지 않았으므로 전 요청
  인증 대상이다([conventions.md](conventions.md) "인증").
- **성공**: HTTP **204** · **envelope 없음**. `DeviceTokenController.register`는 반환 타입이 없는(Unit)
  함수이고 `@ResponseStatus(HttpStatus.NO_CONTENT)`만 붙는다 — `ApiResponse.ok`/`created` 호출이 없으므로
  **응답 본문 자체가 비어 있다**. 로그아웃·탈퇴에 이어 **envelope를 쓰지 않는 세 번째 성공 응답**이다.
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `token` | String | 필수(`@NotBlank`) | FCM SDK가 발급한 기기 등록 토큰 |
| `platform` | String(enum) | 필수 | `DevicePlatform` — `IOS` · `ANDROID` 두 값 |

  `platform`에는 `@NotNull`이 붙어 있지 않지만 Kotlin 비널 프로퍼티라 **필드를 빼면 역직렬화 단계에서
  거절된다**(400). 열거형에 없는 값(예: `WINDOWS`)도 같은 400이다 — 근거는 `DeviceTokenControllerTest`의
  두 케이스다.

- **응답 필드**: 없음(204, 본문 없음 — 위 참고)

  **회원과 세션은 요청이 아니라 토큰에서 정한다.** `memberId`는 `Authentication.name`, `sessionId`는
  `Authentication.credentials`에서 꺼낸다 — 요청 바디로 남의 기기를 지목할 경로가 없다.
  `credentials`에 세션이 실리는 것은 신설 delta가 access token에 `sessionId` 클레임을 더했기 때문이다
  ([conventions.md](conventions.md) "인증" · [auth.md](auth.md)).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | `token`이 공백이거나 `platform`이 허용 값 밖 |
| 401 | `UNAUTHORIZED` | 인증이 필요합니다 |
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰입니다 |
| 401 | `EXPIRED_TOKEN` | 만료된 토큰입니다 |
| 401 | `MEMBER_NOT_FOUND` | 존재하지 않는 회원입니다 |

  **도메인 전용 에러 코드가 없다.** 두 delta 어느 쪽도 `*ErrorCode` enum을 만들지 않았다 — 400은 전역
  `CommonErrorCode.INVALID_REQUEST`(`GlobalExceptionHandler`의 bad-request 핸들러), 401 4종은 전역 인증
  (`AuthErrorCode`)이다. 즉 **실패 표현이 전부 전역 계약이다.** 발송 실패도 마찬가지로 **HTTP 에러가
  아니다** — 아무에게도 응답으로 나가지 않고 서버 로그와 `notification_outbox.last_error`에만 남는다.

## 등록은 upsert다 — `token`이 유일 키

`RegisterDeviceTokenService.register`는 `findByToken`으로 같은 토큰의 행을 먼저 찾고, 있으면
`DeviceToken.reassign`으로 **소유자(`memberId`)·세션(`sessionId`)·플랫폼·갱신 시각을 덮어쓴다.**
없으면 `DeviceToken.register`로 새로 만든다.

**이 설계가 계약에서 뜻하는 바**는 기기 하나가 항상 회원 하나에만 매달린다는 것이다 — 기기를 양도하거나
같은 기기에서 계정을 바꿔 로그인하면 **이전 매핑이 별도 정리 호출 없이 자동으로 끊긴다.** 클라이언트는
"이전 계정의 토큰을 지워 달라"고 따로 부를 필요가 없고, 그런 엔드포인트도 없다.

⚠️ **같은 신규 토큰으로 두 요청이 동시에 들어오면 두 번째가 유니크 제약 위반으로 500이 날 수 있다.**
`DeviceTokenAdapter.save`의 주석이 이 경합을 인정하고, **재시도가 update 경로로 수렴한다**는 것을 근거로
둔다(앱 시작·`onNewToken`·권한 허용마다 재호출되는 엔드포인트라는 전제다). 즉 **이 엔드포인트는 반복
호출을 전제로 설계됐고, 클라이언트가 실패를 삼켜도 다음 호출이 메운다.**

✅ **등록이 이제 실제 효력을 갖는다.** 지금까지 이 표면은 "보관만 하는 자리"였는데, `NotificationOutboxDispatcher`가
발송 직전에 `DeviceTokenQueryPort.findByMemberId`로 **수신자의 토큰을 전부 꺼내 각각 보낸다.**
등록하지 않은 회원은 알림을 받지 못하고, 그 사실이 예외가 아니라 **정상 취소**로 처리된다(아래).

## 서버가 보내는 푸시 — 그룹 이벤트 알림(Outbox)

`TOPPING`·`BACKGROUND`·`TEXT` 셋은 **같은 파이프라인**을 탄다. 생산자(`ToppingPlacedNotifier`·
`BackgroundChangedNotifier`·`TextPlacedNotifier`)가 도메인 트랜잭션 안에서 수신자당 `notification_outbox`
한 행을 쌓고, `NotificationOutboxDispatcher`가 발송 시점에 문구를 렌더해 `FcmNotificationSender`로
**수신자 토큰마다 단건 발송**한다. 문구와 `data`는 `NotificationMessageFactory`(`toppingPlaced`·
`backgroundChanged`·`textPlaced`)가 한 곳에서 조립한다.

리마인드는 이 파이프라인을 쓰지 않는다 — 아래 [데일리 리마인드](#서버가-보내는-푸시--데일리-리마인드p-02p-03).

### 언제, 누구에게 보내는가

| 종류 | 생산 지점 | 생산 조건 |
|---|---|---|
| `TOPPING` | `PlaceParfaitImageService.place` | **신규 배치(`existing == null`)일 때만**. 이미 배치된 이미지의 재배치는 알림이 없다 |
| `BACKGROUND` | `ChangeParfaitBackgroundService.change` | 배경 변경이 저장될 때마다(`COLOR`·`IMAGE` 둘 다). **같은 값으로 바꿔도, 연달아 바꿔도 매번 나간다** — 스로틀이 없다 |
| `TEXT` | `PlaceParfaitTextService.place` | 텍스트 배치마다. 텍스트 수정·삭제는 알림이 없다 |

수신자 규칙은 셋이 같다(정책 E-01).

- 그룹 구성원 전체(`ParfaitGroupMemberQueryPort.findAllByGroupId`)에서
- **나간 사람(`leftAt != null`)을 빼고**
- **행위자 본인(`actorMemberId`)을 뺀다** — 자기 행동으로는 알림을 받지 않는다.

남은 사람이 없으면 아무 행도 쌓지 않는다. 행을 쌓은 뒤 각 생산자가 깨우기 이벤트
(`ToppingPlacedEvent`·`BackgroundChangedEvent`·`TextPlacedEvent`)를 발행하고, 대응 리스너가
**커밋 직후(`AFTER_COMMIT`)** `OutboxPollingWorker.wakeUp()`을 부른다. **도메인 트랜잭션이 롤백되면 행도
알림도 없다.**

### 어떤 페이로드가 가는가

FCM `Message`에 **`notification` 블록과 `data` 블록이 함께** 실린다.

| 자리 | `TOPPING` | `BACKGROUND` | `TEXT` |
|---|---|---|---|
| `notification.title` | `{그룹명} 파르페에 체리 하나 톡!` | 같음 | 같음 |
| `notification.body` | `{닉네임}님이 새 사진을 쌓았어요` | `{닉네임}님이 배경을 바꿨어요` | `{닉네임}: {미리보기}` |
| `notification.body`(행위자가 그룹을 나감·탈퇴) | `누군가 새 사진을 쌓았어요` | `누군가 배경을 바꿨어요` | `누군가: {미리보기}` |
| 닉네임 자르기 | 8자 초과 시 앞 8자 + `...` | 같음 | **자르지 않는다** |
| TTL | 6시간 | 6시간 | 6시간 |

| `data` 키 | 값 | 비고 |
|---|---|---|
| `type` | `TOPPING` · `BACKGROUND` · `TEXT` | 고정 문자열 |
| `route` | `canvas` | 셋 다 같다 |
| `groupId` | 그룹 식별자 | **문자열로 직렬화된 숫자**(FCM `data`는 String만 받는다) |
| `date` | 캔버스 날짜 | `LocalDate.toString()` — `yyyy-MM-dd` |

**`data` 키 구성은 셋이 완전히 같고 `type`만 다르다.** 어느 토핑·텍스트인지 가리키는 식별자는 없다.

⚠️ **닉네임 자르기가 종류마다 다르다.** `TOPPING`·`BACKGROUND`는 `NICKNAME_DISPLAY_MAX = 8`로 자르지만
(`ellipsize`), `textPlaced`는 닉네임을 그대로 넣는다. 닉네임 상한이 15자라([parfait-group.md](parfait-group.md))
텍스트 알림만 닉네임이 길게 나온다. 그룹명은 어느 쪽도 자르지 않는다 → [미결](#미결).

**텍스트 미리보기 규칙**(`NotificationMessageFactory.generateTextPreview`)

1. 줄바꿈(`\r`·`\n` 연속)을 공백 하나로, 연속 공백을 하나로 바꾼다.
2. 앞뒤 공백을 지운다.
3. 결과가 비면 **보내지 않는다** — 디스패처가 `CANCELLED_EMPTY_PREVIEW`로 취소한다(정책 E-22).
4. **grapheme cluster 기준** 11자 이하면 그대로, 12자 이상이면 앞 10자 + `…`(U+2026 한 글자). 이모지를
   중간에서 자르지 않는다. 닉네임의 `...`(마침표 셋)과 **말줄임 기호가 다르다.**

**그룹명과 닉네임은 발송 시점에 다시 조회해 렌더한다.** outbox `payload`는 `NotificationPayload`
(sealed interface — `groupId`·`parfaitId`·`parfaitDate`·`actorMemberId`)의 구현이고, 큐에 쌓인 뒤 이름이
바뀌면 바뀐 이름으로 나간다.

⚠️ **텍스트 본문만은 쌓는 시점의 스냅숏이다.** `TextPlacedPayload`는 식별자 넷에 더해 **원문 `text`**를
outbox에 담는다. 발송 전에 텍스트가 수정·삭제돼도 디스패처가 그것을 다시 보지 않으므로, **이미 지워진
텍스트의 원문이 알림으로 나갈 수 있다**(재시도 경로까지 포함하면 최대 수 시간 뒤) → [미결](#미결).

**봉투(플랫폼별 옵션)는 `FcmNotificationSender`가 상수로 붙인다.** 다섯 종류 모두 같다.

| 플랫폼 | 값 |
|---|---|
| 공통 TTL | 종류별(`NotificationMessageFactory`가 `PushMessage.ttl`로 지정) — Outbox 셋은 6시간, 리마인드는 1시간 |
| Android | priority `HIGH` · **채널 id `parfait_default`** · TTL은 밀리초로 환산 |
| APNs | 헤더 `apns-priority: 10` · `apns-expiration`은 **절대 epoch 초** · sound `default` |

⚠️ **채널 id `parfait_default`는 앱이 만들어야 하는 계약이다.** Android 8 이상에서 같은 id의 알림 채널이
앱에 없으면 알림이 표시되지 않는다 — 발송 결과가 `SENT`로 찍혀도 그렇다.

⚠️ **`notification` 블록이 실려 있으므로 앱이 백그라운드일 때는 시스템이 알림을 직접 표시한다.**
앱 코드가 도는 것은 포그라운드일 때뿐이고, 백그라운드 표시 문구를 앱이 가공할 수 없다.

### 발송 직전에 다시 검사한다

`NotificationOutboxDispatcher`가 **보내기 직전에 조건을 다시 확인**한다. 아래 중 하나면 **보내지 않고
종료 처리**한다(실패가 아니라 취소 — 행은 `SENT` + `last_error`에 사유).

| 조건 | 근거 심볼 | 결과 |
|---|---|---|
| 그룹이 사라졌다 | `ParfaitGroupQueryPort.findById`가 널 | `CANCELLED_GROUP_DELETED` |
| 수신자가 그룹을 나갔다 | 구성원 조회가 널이거나 `leftAt != null` | `CANCELLED_RECEIVER_LEFT` |
| 수신자에게 등록된 기기 토큰이 없다 | `findByMemberId`가 빈 목록 | `NO_DEVICE_TOKEN` |
| (`TEXT`만) 미리보기가 빈 문자열 | `generateTextPreview`가 널 | `CANCELLED_EMPTY_PREVIEW` |

**행위자가 그룹을 나간 경우는 취소가 아니라 문구 치환이다**(정책 E-05, 위 표의 `누군가`). **캔버스·토핑·
텍스트 자체의 존재는 검사하지 않는다** — 그룹이 살아 있으면 대상이 지워져도 나간다.

**디스패처가 모르는 `eventType`은 그 행만 `FAILED`(`UNKNOWN_EVENT_TYPE: …`)로 닫는 분기가 있다.** 다만
그 앞 단계인 `NotificationOutboxAdapter.toDomain`이 payload 역직렬화에서 같은 경우 `error(...)`를 던지므로,
실제로는 그 분기에 닿기 전에 **배치 선점(`claimBatch`) 전체가 예외로 끝난다** — 서버 내부 결함이고 앱
계약에는 영향이 없다.

### 몇 번, 얼마나 늦게 오는가

- **회원의 기기 토큰 전부에 보낸다.** 하나라도 성공하면 그 행은 발송 완료로 본다.
- **최소 한 번(at-least-once) 보장이고, 중복 수신이 가능하다.** 생산 쪽은 `dedup_key`로 멱등하지만
  (`NotificationOutboxAdapter.saveAll`이 이미 큐에 있는 키를 먼저 걸러 낸다), 발송 자체는 재시도되므로
  **앱은 같은 알림을 두 번 받아도 견뎌야 한다.**

| 종류 | `aggregate_type` / `event_type` | `dedup_key` |
|---|---|---|
| `TOPPING` | `TOPPING` / `TOPPING_PLACED` | `topping-placed:{toppingId}:{수신자}` |
| `BACKGROUND` | `PARFAIT` / `BACKGROUND_CHANGED` | `background-changed:{parfaitId}:{변경시각 yyyyMMddHHmmssSSS}:{수신자}` |
| `TEXT` | `TEXT` / `TEXT_PLACED` | `text-placed:{textId}:{수신자}` |

  `BACKGROUND`의 키에 변경 시각(`saved.updatedAt`, 밀리초)이 들어가므로 **변경 한 번마다 별개의 알림**이다.
  같은 밀리초 안의 두 변경만 하나로 접힌다.
- **지연은 폴링 주기에 걸린다.** `OutboxPollingWorker`가 기본 2초(`notification.outbox.poll-interval-ms`)
  간격으로 돌고, 위 리스너가 커밋 직후 워커를 즉시 깨우므로 정상 경로는 거의 즉시다. **깨우는 신호가
  유실돼도 다음 폴링이 잡는다.**
- **재시도는 1분 → 5분 → 15분 → 1시간 → 6시간, 최대 5회**(`OutboxBackoff`). 소진하면 그 행은 실패로
  확정되고 **사용자에게도 클라이언트에게도 알리지 않는다** — 서버 로그와 `last_error` 컬럼에만 남는다.
- 재시도 대상은 FCM이 `UNAVAILABLE`·`INTERNAL`·`QUOTA_EXCEEDED`를 주거나 **에러 코드를 아예 주지
  않을 때**(타임아웃·네트워크 오류)다. `FcmNotificationSender`가 코드 미상을 재시도 쪽으로 분류한다.

⚠️ **TTL 6시간이 재시도 일정보다 짧다.** 마지막 두 단계(1시간·6시간)까지 밀린 알림은 FCM 쪽 만료와
겹칠 수 있다. 서버 코드가 이 상호작용을 조정하지 않는다 → [미결](#미결).

## 서버가 보내는 푸시 — 데일리 리마인드(P-02·P-03)

`[Feat/#128]`이 붙인 **두 번째 계약 축**이다. 이벤트가 아니라 **시각**이 트리거이고, 대상이 그룹이 아니라
**서비스 전체 이용자**다.

### 언제, 누구에게 보내는가

| 항목 | 값 |
|---|---|
| 오전(P-02) | 매일 **10:00 KST**(`notification.reminder.morning-cron` 기본 `0 0 10 * * *`, zone `Asia/Seoul`) |
| 저녁(P-03) | 매일 **20:00 KST**(`notification.reminder.evening-cron` 기본 `0 0 20 * * *`) |
| 대상 | `left_at IS NULL`인 그룹 멤버십이 **1개 이상**이고 **기기 토큰을 가진** 이용자의 토큰 전부 |
| 제외 | 그룹이 하나도 없는 이용자 · 토큰이 없는 이용자 |

대상 조회는 `ReminderTargetQueryPort.findActiveGroupMemberDeviceTokens`이고, 구현
(`DeviceTokenRepository.findActiveGroupMemberTokens`)이 **`device_token`을 기준으로 `parfait_group_member`를
`EXISTS` 세미조인**한다. 그래서 **한 사람이 그룹 N개에 속해도 토큰은 한 번만 나온다** — 사용자당 알림 1건이다.

⚠️ **"알림을 끄겠다"는 의사를 보는 자리가 없다.** 서버는 토큰 보유 여부만 본다. `ReminderTargetQueryPort`
KDoc은 "권한 거부 이용자는 토큰이 없어 결과에 미포함(정책 E-02)"이라고 적지만, **앱은 권한과 무관하게
토큰을 등록한다** — PR #450이 등록 호출을 권한 허용 직후에서 세션 축으로 옮긴 이유가 바로
"토큰이 권한과 무관하게 발급되기 때문"이다(아래 [Android 매핑](#기기-토큰-등록-결선-2026-09-05-pr-450)).
즉 **권한을 거부한 기기의 토큰도 대상에 든다** → [open-questions](../synthesis/open-questions.md) OQ-P-384.

### 어떤 페이로드가 가는가

Outbox 알림과 마찬가지로 `notification` 블록과 `data` 블록이 함께 실린다. 봉투(Android 채널 id
`parfait_default` · priority `HIGH` · APNs 헤더)는 **Outbox 알림과 같은 헬퍼가 붙이므로 동일**하고,
**TTL만 다르다.**

| 자리 | `REMIND_AM`(오전) | `REMIND_PM`(저녁) |
|---|---|---|
| `notification.title` | `새벽 3시에 오늘의 새 캔버스가 열렸어요` | `새벽 3시에 오늘의 캔버스가 마감돼요` |
| `notification.body` | `오늘의 파르페를 먼저 쌓아볼까요?` | `오늘의 파르페를 완성하러 가볼까요?` |
| `data.type` | `REMIND_AM` | `REMIND_PM` |
| `data.route` | `group` | `group` |
| TTL | **1시간** | **1시간** |

⚠️ **`data`에 `groupId`가 없다.** 이용자 단위 알림이라 특정 그룹을 가리키지 않고, 목적지는
`route=group`(그룹 목록)이다. **Outbox 알림 셋의 `route=canvas` + `groupId`·`date`와 키 구성이 다르므로**, 소비 측은
`groupId`·`date`가 항상 있다고 전제하면 안 된다.

✅ **앱이 이 두 값을 이미 안다.** `PushNotificationType`에 `REMIND_AM`·`REMIND_PM`이,
`PushNotificationRouteType`에 `GROUP("group")`이 있고 `PushDeepLinkParser`가 `route=group`을
`PushDeepLink.GroupList`로 접는다 — **문자열이 양쪽에서 같다.** 앱이 먼저 구현하고 서버가 나중에 따라온
드문 자리이고(OQ-P-361), 그 근거로 적힌 "FCM 페이로드 스펙 v1 §3.1"은 서버 `ReminderType` KDoc에도
그대로 인용돼 있다 — **문서 실물은 여전히 어느 저장소에도 없다.**

### 어떻게 보내는가 — Outbox 알림과 다른 점

| 항목 | `TOPPING`·`BACKGROUND`·`TEXT` | 데일리 리마인드 |
|---|---|---|
| 큐 | `notification_outbox` 경유 | **안 쓴다** — 배치가 직접 발송 |
| 발송 | 수신자별 단건(`send`) | **토큰 500개씩 멀티캐스트**(`sendMulticast`) |
| 재시도 | 1분→5분→15분→1시간→6시간, 최대 5회 | **없다** — 그 회차로 끝 |
| 중복 | at-least-once, 중복 수신 가능 | 잡이 하루 한 번만 도는 한 1회 |
| 실패 기록 | `notification_outbox` 행(`last_error`) | **서버 로그뿐** |

Outbox를 안 쓰는 근거를 커밋 메시지가 적는다 — 묶일 사용자 트랜잭션이 없고, 배치 잡 자체가 재시작
모델을 가지며, **리마인드는 시의성 알림이라 다음 날 재발송이 무의미하다.**

- **청킹**: `DailyReminderSender.CHUNK_SIZE = 500`(FCM `sendEachForMulticast` 상한), 청크 **사이**에만
  `CHUNK_DELAY_MS = 200`ms 지연.
- **개별 실패는 예외가 아니다** — `MulticastResult`에 담겨 돌아오고, 요청 전체 실패(인증·쿼터 등)만
  `NotificationSendException`이다.
- **죽은 토큰은 잡 끝에 일괄 삭제**한다(아래 [토큰이 지워지는 네 경로](#기기-토큰이-지워지는-네-경로)).
- **잡 중복 실행 방지는 Spring Batch가 한다** — 스케줄러가 `JobParameters`에 `runDate` + `reminderType`을
  실어 오전·저녁이 서로 다른 `JobInstance`가 되고, 같은 날 각각 1회만 돈다.

⚠️ **P-03에는 하드컷이 있다.** `DailyReminderSender`가 **배치 시작 시각**이 `EVENING_CUTOFF = 21:00`
이상이면 발송을 **통째로 건너뛴다**(정책 E-09 "21:00 이후 도착 금지"). 그런데 판정이 시작 시각 한 번뿐이라
**청킹이 길어져 21:00을 넘겨 나가는 뒤쪽 청크는 막지 못한다**
→ [open-questions](../synthesis/open-questions.md) OQ-P-385.

### FCM 에러 코드 분류가 한곳으로 모였다

`FcmErrorCodes`(`core/notification/domain`)가 두 집합을 든다 — 디스패처와 리마인드 배치가 같은 판정을 쓴다.

| 집합 | 값 | 처리 |
|---|---|---|
| `DEAD_TOKEN` | `UNREGISTERED` · `INVALID_ARGUMENT` · `SENDER_ID_MISMATCH` | 토큰 즉시 삭제(정책 E-10/E-12) |
| `RETRYABLE` | `UNAVAILABLE` · `INTERNAL` · `QUOTA_EXCEEDED` | 재시도(Outbox 경로만 — 리마인드는 재시도가 없다) |

**값 자체는 직전 판본과 같다** — `NotificationOutboxDispatcher.DEAD_TOKEN_CODES`와
`FcmNotificationSender`에 흩어져 있던 것을 옮겨 모았을 뿐이다. `core`가 firebase-admin에 의존하지 않아
**문자열로 들고 있으므로 오타를 컴파일러가 못 잡는다**고 KDoc이 적는다.

## 기기 토큰이 지워지는 네 경로

| 경로 | 지우는 범위 | 근거 |
|---|---|---|
| `POST /api/v1/auth/logout` | 그 **로그인 세션**이 등록한 행(`memberId` + `sessionId`) | `LogoutService`가 `TokenDeletePort.delete` 바로 뒤에서 `DeviceTokenDeletePort.delete` 호출 |
| `DELETE /api/v1/users/me` | 그 **회원의 전 행**(`memberId`) | `MemberService.withdraw`의 `afterCommit`에서 `deleteAllByMemberId` 호출 |
| **푸시 발송 실패** | **죽은 토큰 한 행**(`token`) | `NotificationOutboxDispatcher`가 `FcmErrorCodes.DEAD_TOKEN`을 받으면 `DeviceTokenDeletePort.deleteByToken` 호출 |
| **리마인드 배치(신설)** | **그 회차에서 죽은 토큰 전부**(`token IN (...)`) | `DailyReminderSender`가 `MulticastResult.deadTokens()`를 모아 잡 끝에 `deleteByTokenIn`으로 일괄 삭제(`DELETE_CHUNK = 5000`씩) |

앞의 둘은 범위가 다른 것이 의도다 — 로그아웃은 한 세션만 끝내지만 탈퇴는 그 회원의 모든 세션을 정리한다.
**뒤의 둘은 클라이언트 요청과 무관하게 서버가 스스로 걷는 경로**다: 앱을 지웠거나 토큰이 회전돼 무효가
된 기기를 발송 시도가 발견해 회수한다. 없는 토큰을 지워도 예외가 아니다(`deleteByToken`).
**넷째는 셋째와 판정 기준이 같고**(`FcmErrorCodes.DEAD_TOKEN`) **범위만 다르다** — 리마인드는 전 이용자를
한 회차에 훑으므로 **죽은 토큰이 가장 빨리 걷히는 경로**다. 빈 목록이면 쿼리를 아예 보내지 않는다.

⚠️ **세션이 없는 행은 로그아웃이 지우지 못한다.** `sessionId`는 널을 허용하고
(`session_id` 컬럼이 nullable, 클레임 과도기 때문이다), 삭제 조건은 `memberId` **와** `sessionId`를
함께 본다. 클레임 도입 전에 발급된 access token으로 등록하면 `sessionId`가 널인 행이 생기고, **그 행은
로그아웃으로 지워지지 않는다** — 탈퇴이거나 같은 토큰의 재등록(upsert), 또는 위 발송 실패 회수만 그것을
걷는다 → [미결](#미결).

⚠️ **탈퇴 시 정리는 실패해도 탈퇴를 막지 않는다.** `afterCommit` 블록에서 refresh token 정리와 **별개의**
`runCatching`으로 감싸므로, 한쪽이 실패해도 나머지 정리와 탈퇴 자체는 진행되고 로그 경고만 남는다
([member.md](member.md)).

## 저장 스키마

계약 서술에 걸리는 것만 옮긴다(스키마 전체 미러가 아니다).

### `device_token`(Flyway `V17`)

- `token`이 **유니크**(`uk_device_token_token`) — 위 upsert의 근거다.
- `member_id`가 `member(id)` **외래 키** — 회원이 사라지면 남을 수 없다.
- `session_id`는 **널 허용** — 위 경고의 근거다.
- `(member_id, session_id)` 인덱스 — 로그아웃 삭제 조건과 같은 축이다.

### `notification_outbox`(Flyway `V18`, 신설)

발송 의도를 도메인 쓰기(이미지 토핑 배치·배경 변경·텍스트 배치)와 **같은 트랜잭션**에 남기는 큐 테이블이다(Transactional Outbox).

- **수신자당 한 행**이고 `dedup_key`가 **유니크**(`uk_notification_outbox_dedup`) — 생산자 멱등의 근거다.
- **외래 키가 없다.** 큐로만 쓰고 참조 무결성은 걸지 않는다 — 그룹·회원이 지워져도 행은 남고, 발송 직전
  재검증이 그것을 취소로 처리한다.
- `payload`가 `LONGTEXT`에 **JSON 문자열**로 들어간다(`NotificationOutboxAdapter`가 Jackson으로 직렬화).
  타입 정보는 JSON에 없고 **`event_type` 컬럼으로 역직렬화 대상을 고른다**(`TOPPING_PLACED` → `ToppingPlacedPayload`,
  `BACKGROUND_CHANGED` → `BackgroundChangedPayload`, `TEXT_PLACED` → `TextPlacedPayload`). `TEXT_PLACED` 행에는
  사용자가 쓴 **텍스트 원문**이 그대로 남는다(보관 기간 아래).
- `status`는 `PENDING`·`SENT`·`FAILED` 세 값이고, 컬럼이 가변 길이 문자열이라 **값을 늘려도 마이그레이션이
  필요 없다** — `OutboxStatus` 주석이 스로틀 정책에서 값을 더할 것이라고 적는다.
- `(status, scheduled_at)` 인덱스로 발송 대상을 고른다. 선점은 `FOR UPDATE SKIP LOCKED`다.
- **종료 상태 행은 영구 보관하지 않는다** — `OutboxRetentionSweeper`가 매일 04:00(Asia/Seoul)에
  `notification.outbox.retention-days` 지난 `SENT`·`FAILED` 행을 지운다. `application.yaml` 값은 **2일**이다
  (`OutboxRetentionSweeper`의 `@Value` 기본값은 7 — 설정이 빠진 프로필에서만 7일이 된다).

⚠️ **운영 DB에 이 테이블이 실제로 생겼는지는 이 회차에서 확인하지 않았다** — 마이그레이션 파일의 존재만
확인했다([conventions.md](conventions.md) "스키마 소유권"이 코드와 운영 스키마가 갈렸던 전례를 적는다).

## 이 계약이 앱에 요구하는 것

서버 코드가 정한 것이라 **앱이 맞추지 않으면 알림이 동작하지 않는 항목**만 모은다.

| 항목 | 요구 | 근거 | 앱(2026-09-05, PR #446·#447) |
|---|---|---|---|
| 알림 채널 | id `parfait_default` 채널을 앱이 생성 | `FcmNotificationSender`의 `ANDROID_CHANNEL_ID` | ✅ `BaseApplication.createPushNotificationChannel` + `PUSH_NOTIFICATION_CHANNEL_ID` — **같은 문자열이다** |
| `data` 파싱 | **알림 종류마다 키 구성이 다르다** — Outbox 셋(`TOPPING`·`BACKGROUND`·`TEXT`)은 `type`·`route`·`groupId`·`date` 넷, 리마인드는 `type`·`route` 둘. 값은 전부 문자열 | `NotificationMessageFactory` | ⚠️ **Outbox 셋은 셋만 읽는다** — `PushDeepLinkIntent.kt`의 extras 키가 `type`·`route`·`groupId`이고 `date`는 어디서도 안 읽힌다. 리마인드는 두 키가 그대로 맞는다 |
| 알림 종류 | `type` 값 **다섯 개** — `TOPPING` · `BACKGROUND` · `TEXT` · `REMIND_AM` · `REMIND_PM` | `NotificationMessageFactory`·`ReminderType` | ⚠️불일치 — `PushNotificationType`에 `BACKGROUND`·`TEXT`가 **없다**. 라우팅은 `route=canvas`만 보므로 이동은 맞게 되지만, `PushDeepLink.AddTopping`이 `type`을 `TOPPING`으로 **하드코딩**해 `MainActivity.consumePushDeepLink`의 `APP_PUSH_OPENED` 분석 이벤트가 배경·텍스트 알림도 `type=TOPPING`으로 찍는다. 포그라운드 캔버스 갱신 요청(`toppingGroupIdOrNull`)도 같은 이유로 셋 모두에서 걸린다 → OQ-P-445 |
| 리마인드 딥링크 | `route=group`(그룹 목록). `groupId`가 **없다** | `NotificationMessageFactory.dailyReminder` | ✅ `PushNotificationRouteType.GROUP("group")` → `PushDeepLink.GroupList`, `groupId` 없이 접는다 |
| 딥링크 | `route=canvas` + `groupId` + `date`로 캔버스에 도달(Outbox 셋 공통) | 같은 곳 | ⚠️ `groupId`로만 간다 — `PushDeepLink.AddTopping` KDoc이 **"알림이 가리키던 날짜가 아니라 항상 그 그룹의 최신 캔버스"**라고 못 박는다 → OQ-P-359 |
| 중복 내성 | 같은 알림을 두 번 받아도 부작용이 없어야 함 | at-least-once 보장 | ⚠️ 이동은 견딘다(`Channel(CONFLATED)`) 그러나 **표시는 안 견딘다** — 알림 id가 `message.messageId?.hashCode()`라 재시도로 온 같은 알림이 **알림 두 개**로 쌓인다 → OQ-P-359 |
| 토큰 등록 | 앱 시작·`onNewToken`·권한 허용마다 재호출(실패는 다음 호출이 메움) | `DeviceTokenAdapter.save` 주석 | ✅ **부른다**(PR #450) — 자리는 **세션 축 넷**이다(로그인·가입·앱 진입의 성공 분기 + `onNewToken`). 권한 허용 직후는 **빼고** 세션 쪽으로 옮겼다 — 토큰이 권한과 무관하게 발급되기 때문이다. 실패는 3회 재시도(3초·6초) 뒤 다음 트리거에 맡긴다 |
| (앱 쪽 전제) | Android 13+에서 `POST_NOTIFICATIONS` 런타임 허용 | Android 플랫폼 | ✅ **묻는다**(PR #450) — A-004·A-005 완료 직후 `NotificationPermissionGate`가 안내하고, 영구 거부면 앱 설정으로 보낸다. API 33 미만은 `NotificationPermissionManager`가 **허용으로 본다**(그 아래에서 `checkSelfPermission`이 항상 거부를 답하던 것이 포그라운드 알림을 통째로 버리고 있었다) |

## Android 매핑

✅ **`:data` 표면이 있다(2026-09-03, PR #437 `7019a550`)** — 서버가 표면을 먼저 연 도메인에 앱이 하루
만에 따라붙었다.

| 계약 | Android |
|---|---|
| `POST /api/v1/notifications/devices` | `NotificationService.postNotificationsDevices` — `@NoAuth`를 붙이지 않아 access token이 실린다 |
| 요청 `token`·`platform` | `RegisterDeviceTokenRequest`(`@SerialName` 둘). `platform`은 `NotificationRemoteDataSourceImpl`이 `"ANDROID"` 상수로 채운다 — 호출자가 고를 수 없다 |
| 성공 204·본문 없음 | `ApiCaller.safeApiCallNoContent` — 반환 타입에 envelope를 두지 않는다. `Result<Unit>`으로 나온다 |
| 등록 해제 엔드포인트 없음 | 서비스에도 없다. 로그아웃·탈퇴가 서버에서 대신 지운다는 사실을 KDoc이 가리킨다 |
| **푸시 수신·채널·딥링크** | ~~**대응 심볼 0건**~~ → **셋 다 생겼다**(2026-09-05, PR #446·#447) — 아래 [푸시 수신·딥링크](#푸시-수신딥링크-2026-09-05-pr-446447) |

도메인 모델은 `domain.model.notification.DeviceToken`(`@JvmInline value class`) 하나다.
`upsert`라 반복 호출해도 된다는 계약은 `NotificationRemoteDataSourceImpl`의 KDoc이 받아 적었다.

✅ **손으로 확인할 자리가 생겼다(2026-09-04, PR #451 `2b1dce3a`)** — 앱 코드는 한 줄도 안 바뀌었고
저장소 루트 `http/`에 요청 모음 둘이 들어왔다.

| 파일 | 나가는 곳 | 확인 대상 |
|---|---|---|
| `http/notifications.http` | 우리 서버 | 등록 성공 204·본문 없음, upsert 재호출, 400 4종·401 |
| `http/fcm-test.http` | **FCM v1 API**(서버를 거치지 않는다) | 위 [서버가 보내는 푸시](#서버가-보내는-푸시--그룹-이벤트-알림outbox)를 재현해 앱 수신을 확인 |

**요청 모음이 앱 코드보다 앞서 나간 첫 도메인이다** — 등록 엔드포인트는 앱 표면이 있어도 부를 수단이
없는데(FCM 토큰 취득 심볼 0건), 이 파일은 토큰을 손으로 넣으므로 **앱 없이도 지금 돌릴 수 있다.**
`fcm-test.http`는 반대로 **받을 쪽이 없어 지금은 절반만 돌아간다** — 발송은 200이지만 채널이 없어
기기에 아무것도 뜨지 않는다.

⚠️ **`fcm-test.http`는 이 문서의 페이로드 표를 상수로 복제한다.** 문구·`data` 키·채널 id·TTL·APNs
헤더가 그 파일에 그대로 적혀 있고, 서버가 값을 바꿀 때 함께 고치는 절차가 없다 — 엔드포인트 커버와
달리 **이 복제는 세는 축이 없다** → [open-questions](../synthesis/open-questions.md) OQ-P-354.
**이미 어긋났다** — 그 파일의 토핑 본문은 `…새 토핑을 쌓았어요`인데 서버는 `…새 사진을 쌓았어요`를 보내고,
`BACKGROUND`·`TEXT` 요청은 없다.

⚠️ **둘 다 실행 기록은 0건이다.** `fcm_access_token`(유효기간 1시간)과 Firebase 서비스 계정 키가
있어야 돌릴 수 있고, 이 회차에 그것을 확보해 쏴 본 적이 없다.

⚠️ **표면뿐이고 결선은 0이다.** 리포지토리도 UseCase도 화면도 없고, 무엇보다 **넣을 토큰을 얻을 수단이
없다** — `FirebaseMessaging`·FCM 토큰 취득 심볼이 develop 전체에 여전히 0건이고 `firebase-messaging`
의존도 없다. 즉 `registerDeviceToken`을 부르는 코드는 하나도 없고, 지금 상태로는 **부를 수도 없다.**

> 🔁 **뒤 문장의 전제가 2026-09-05에 깨졌다** — `firebase-messaging` 의존과 `onNewToken`이 돌아와
> **토큰을 얻을 수단이 생겼다.** 그런데 `registerDeviceToken` 호출부는 **여전히 0건**이다. 즉 판정이
> "부를 수 없다"에서 **"부를 수 있는데 안 부른다"**로 바뀌었다(OQ-P-341 ②). 앞 문단의 `:data` 표면
> 서술 자체는 그대로 옳다.

🔁 **이 공백은 "아직 안 만든 것"이 아니라 "만들었다가 걷어낸 것"이다.** 앱은 FCM 수신 서비스와
토큰 조회·알림 권한 요청을 갖고 있었고, **2026-08-22 PR #325가 그것을 걷어냈다**
([ADR-0013](../adr/0013-firebase-fcm-crashlytics.md)의 철회 정정 — `firebase-messaging` 의존까지 빠졌고
Crashlytics·Analytics만 남았다). **철회 근거가 정확히 이 엔드포인트의 부재였다** — `onNewToken`이
`TODO("서버에 FCM 토큰 전송")`인 채여서 토큰이 로그로만 갔고, 결선된 적 없는 기능 때문에 첫 실행마다
알림 권한을 묻는 상태였다.

⚠️ **이번 서버 delta로 그 공백의 성격이 또 한 번 바뀌었다.** 직전 회차까지는 "앱이 안 보내면 아무 일도
안 일어난다"였는데, 이제는 **서버가 실제로 보내고 있고 받을 앱이 없다.** 등록된 토큰이 0건이라
`NO_DEVICE_TOKEN`으로 전부 취소되므로 **동작 장애는 아니지만**, 앱이 토큰을 등록하는 순간부터는 위
[이 계약이 앱에 요구하는 것](#이-계약이-앱에-요구하는-것)이 곧바로 구속력을 갖는다 — 특히 채널 id를
맞추지 못하면 **보내는 쪽은 성공인데 사용자는 못 보는 상태**가 된다 → OQ-P-341·OQ-P-352.

### 푸시 수신·딥링크 (2026-09-05, PR #446·#447)

✅ **받을 쪽이 생겼다.** `firebase-messaging` 의존과 `app` `push/ParfaitFirebaseMessagingService`가
돌아왔고, 딥링크 축이 새로 붙었다([ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정 ·
[navigation-flow](../architecture/navigation-flow.md) "푸시 딥링크 이동").

| 계약 | Android |
|---|---|
| 채널 id `parfait_default` | `PUSH_NOTIFICATION_CHANNEL_ID` 상수 — 채널은 `BaseApplication.createPushNotificationChannel`이 앱 시작에 만든다(`IMPORTANCE_HIGH`, 표시 이름·설명은 `strings.xml`의 `notification_channel_default_*`) |
| `notification` 블록이 늘 실린다 | `onMessageReceived`가 `message.notification`이 없으면 **곧장 return** 한다 — `data`-only 페이로드는 처리하지 않는다(KDoc이 그 전제를 적는다) |
| 백그라운드는 시스템이 표시 | 앱이 직접 만드는 알림도 `data`의 키를 그대로 `Intent` extras에 실어 **두 경로의 extras 모양을 맞춘다** |
| `data` 키 `type`·`route`·`groupId` | `PushDeepLinkIntent.kt`의 `EXTRA_TYPE`·`EXTRA_ROUTE`·`EXTRA_GROUP_ID` → `PushDeepLinkParser.parse`. **`route`·`type` 문자열은 PR #456으로 `PushNotificationRouteType`·`PushNotificationType` 두 enum의 `key`가 물고**, `groupId`를 양수로 읽는 규칙은 `PushDeepLink.AddTopping.parse`에 있다 |
| `data` 키 `date` | **읽는 코드가 없다** → OQ-P-359 |
| `route=canvas` | `PushDeepLink.AddTopping(groupId)` → `NavKeyCanvasMain(groupId)` — **날짜 인자가 없어 최신 캔버스로 간다** |
| `route=group` | `PushDeepLink.GroupList(type)` → `NavKeyGroupList`. 서버는 리마인드 둘(`REMIND_AM`·`REMIND_PM`)에서 이 값을 보낸다 → OQ-P-361 |
| 두 `route` 다 로그인 필요 | 이동 전에 `HasActiveSessionUseCase`가 세션을 확인하고 없으면 **딥링크를 버린다** — 로그인을 마쳐도 이어가지 않는다(PR #456) |
| `type` 값 3종 | ⚠️불일치 — 서버는 5종이다(`BACKGROUND`·`TEXT` 추가, 위 [이 계약이 앱에 요구하는 것](#이-계약이-앱에-요구하는-것) · OQ-P-445). `PushNotificationType`(`TOPPING`·`REMIND_AM`·`REMIND_PM`). **라우팅에 쓰지 않는다** — 목적지는 `route`가 정하고 `type`은 탭 분석 용도로만 실린다. 모르는 값은 `null`이라 파싱이 실패하지 않는다 |

**전달 통로는 세션 종료 이동과 같은 모양이다** — `:domain`의 `PushDeepLinkEventBus`(발행·구독 겸용
인터페이스)와 `:data`의 `PushDeepLinkEventBusImpl`(`Channel(CONFLATED)` + `receiveAsFlow()`)이고,
**수집은 앱 루트 `MainRoute` 한 곳**이다. 발행은 `MainActivity`가 하며 `onCreate`와 `onNewIntent`
(`launchMode="singleTop"`) 양쪽을 덮는다. **같은 딥링크가 두 번 발행되는 경로가 둘**이라 막는 수단도
둘이다 — 구성 변경으로 `onCreate`가 다시 도는 것은 `setIntent(Intent())`가, 알림 인텐트가 태스크의
base intent로 남아 되살릴 때 다시 오는 것은 `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY` 판정이 막는다.
자세한 것은 [navigation-flow](../architecture/navigation-flow.md) "푸시 딥링크 이동"에 있다.

📌 **토핑 알림이 이동 말고 하나를 더 한다**(2026-09-10, PR #482 `efa771503`) — `onMessageReceived`가
알림을 띄운 뒤 같은 `data` 를 다시 읽어, `type=TOPPING`·`route=canvas` 이면
`RequestTodayParfaitRefreshUseCase` 로 **오늘 캔버스 갱신을 요청한다**(폴링 주기를 기다리지 않는다).
판정은 `Map<String, String>.toppingGroupIdOrNull()` 하나이고 리마인드(`route=group`)는 파서가 이미
갈라 준다. 계약 쪽 요구는 **늘지 않았다** — 읽는 키는 종전과 같은 `type`·`route`·`groupId` 셋이고
`date` 는 여전히 안 읽는다(OQ-P-359). ⚠️ **포그라운드에서만 돈다** — `notification` 블록이 실린
페이로드를 백그라운드·종료 상태에서 받으면 시스템이 알림을 직접 띄우고 이 콜백을 거치지 않는다
→ [canvas-adaptive-polling 스펙](../superpowers/specs/archive/2026-09-10-canvas-adaptive-polling.md).

🔁 **여기 있던 두 경고를 걷었다 — 둘 다 낡았다.** ① 권한을 묻는 자리는 PR #450이 붙였다
(`NotificationPermissionGate`, A-004·A-005 완료 직후 — OQ-P-358은 그때 해소됐다). ② 콜드 스타트에서
딥링크와 스플래시 백스택 리셋이 겹치는 구간은 **닫혔다** — 수집이 스플래시 이탈을 기다린 뒤 소비하고,
그 자리에서 세션도 함께 본다(OQ-P-360 ①③ 해소, ② 잔존 — 2026-09-06 PR #456으로 develop에 들어왔다).

### 기기 토큰 등록 결선 (2026-09-05, PR #450)

✅ **표면이 실제로 불린다.** 설계 정본은
[push-notification-permission-and-device-token 스펙](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md),
계층 배치는 [data-layer](../architecture/data-layer.md) 「기기 토큰 등록」.

| 계약 | Android |
|---|---|
| 반복 호출이 안전한 upsert(`token`이 유일 키) | 부르는 자리가 **넷**이다 — `LoginWithKakaoUseCase`·`SignUpUseCase`(성공 분기) · `BootstrapSessionUseCase`(`refreshMyAccount` 성공 분기만) · `ParfaitFirebaseMessagingService.onNewToken`. **그 반복이 곧 실패 복구 수단**이라 "등록됨" 영속 플래그도 WorkManager도 두지 않았다 |
| 같은 신규 토큰의 동시 요청은 유니크 제약 위반으로 500 | `NotificationRepositoryImpl`이 `Mutex`로 막는다. 진행 중이면 두 번째 호출은 **대기하지 않고 그냥 돌아간다** |
| 인증 필요(화이트리스트 밖) | 세션이 없는 경로에서는 부르지 않는다 — 신규 회원 로그인 분기, 필수 약관 미동의, 저장된 토큰이 없는 부트스트랩 |
| `platform` 은 앱이 고정 | 종전대로 `NotificationRemoteDataSourceImpl`이 `"ANDROID"` 상수로 채운다 |
| 로그아웃이 `(memberId, sessionId)` 로 매핑을 지운다 | 세션마다 재등록하므로 옛 세션 행이 남지 않는다 — 재로그인이 곧 재등록이다 |

**`onNewToken` 도 전달받은 값을 쓰지 않고 같은 진입점을 탄다** — 등록구가 지금 값을 다시 읽으므로
결과가 같고, 같은 뮤텍스를 타야 세션 축과 겹치지 않는다. 토큰 계약은 이 라운드에 **비널로 좁혀졌다**
(`DeviceTokenProvider.currentToken(): DeviceToken`) — `getToken()` 이 미발급을 값이 아니라 `Task`
실패로 주므로 `null` 분기가 도달하지 않는 경로였다.

⚠️ **실서버·실기기 확인은 여전히 0회다.** `http/notifications.http` 도 이 회차에 돌리지 않았고,
등록이 실제로 204를 받는지, 발송이 `NO_DEVICE_TOKEN` 을 벗어나는지 확인된 바 없다.

⚠️ **쓰고 있는 FCM API 셋이 deprecated 다**(`getToken()`·`deleteToken()`·`onNewToken()`). 대체는 FID
기반이고 **서버 Admin SDK 가 9.10.0 이상이어야 `setFid` 가 있다** — 앱만 옮기면 모든 발송이 실패하므로
등록 토큰 축에 남는다 → [open-questions](../synthesis/open-questions.md) OQ-P-362.

## 미결

- ~~2026-08-22에 걷어낸 FCM 축을 되살릴지~~ → **되살렸다**(2026-09-05, PR #446·#447 — [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md)
  되살림 정정). ~~남은 것은 **등록 호출 시점**이다~~ → ✅ **답해졌다**(PR #450, 세션 축 넷) — OQ-P-341 해소
- ~~앱이 `POST_NOTIFICATIONS` 런타임 허용을 **묻지 않아** Android 13+에서는 표시 단계에서 막힌다~~
  → ✅ **묻는다**(PR #450, A-004·A-005 완료 직후) — OQ-P-358 해소. 다만 **노출 횟수 정책이 없어**
  허용 전까지 그룹 생성·참여 흐름마다 다시 뜬다 → [open-questions](../synthesis/open-questions.md) OQ-P-370
- 앱이 `date`를 버리고 항상 최신 캔버스로 열며, 중복 수신이 **알림 두 개**로 쌓인다 — 위
  [이 계약이 앱에 요구하는 것](#이-계약이-앱에-요구하는-것)의 ⚠️ 셋
  → [open-questions](../synthesis/open-questions.md) OQ-P-359
- ~~앱이 서버에 없는 알림 둘(P-02·P-03 리마인드, `route=group`)을 먼저 구현했고~~ → ✅ **서버가 따라붙었고
  `type`·`route` 문자열이 양쪽에서 같다**(2026-09-04). 다만 근거로 인용되는 **"FCM 페이로드 스펙 v1"은
  여전히 어느 저장소에도 없다** — 이제 서버 `ReminderType` KDoc까지 그것을 인용한다
  → [open-questions](../synthesis/open-questions.md) OQ-P-361
- 세션 없는(`session_id` 널) 행을 로그아웃이 못 지우는 구간을 서버가 닫을지, 앱이 재등록으로 덮을지
  → [open-questions](../synthesis/open-questions.md) OQ-P-342
- ~~알림 종류가 토핑 등록 1종뿐이고(커밋 제목의 "3종"과 어긋난다)~~ → ✅ **3종이 됐다**(2026-09-04,
  `[Feat/#128]` — 토핑 등록 · `REMIND_AM` · `REMIND_PM`) — OQ-P-343의 그 갈래는 해소. **남은 것은
  수신 설정이다** — 알림을 끄겠다는 의사를 담는 자리가 서버·앱 어디에도 없다
  → [open-questions](../synthesis/open-questions.md) OQ-P-343
- 리마인드 대상 산출이 **알림 권한을 보지 않는다** — 서버 KDoc의 "권한 거부 이용자는 토큰이 없다"는
  전제가 앱 동작(권한과 무관한 세션 축 등록)과 어긋난다
  → [open-questions](../synthesis/open-questions.md) OQ-P-384
- P-03의 21:00 하드컷이 **배치 시작 시각만** 보므로 뒤쪽 청크의 도착 시각은 보장되지 않는다
  → [open-questions](../synthesis/open-questions.md) OQ-P-385
- 리마인드는 Outbox를 안 써서 **실패 기록이 서버 로그뿐**이다 — 몇 명에게 갔는지 사후에 물을 자리가 없다
- 문구·`data` 스키마·채널 id·딥링크 목적지가 **서버 코드에만 있고 정책 근거가 없다** — 위키 정책 소스에
  알림 항목 자체가 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-351
- 앱이 채널 `parfait_default`를 만들지 않으면 발송이 성공해도 표시되지 않는데, 그 합의를 확인할 수단이
  양쪽 어디에도 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-352
  (2026-09-04 갱신 — `http/fcm-test.http`가 **불일치를 재현하는 요청**을 갖췄다. 다만 앱에 수신부가
  없어 아직 돌려서 확인할 수 없고, 상수 자체를 대조하는 자동 검사는 여전히 양쪽에 없다)
- `fcm-test.http`가 서버 발송 페이로드를 앱 저장소에 복제해 두 번째 정본처럼 보인다
  → [open-questions](../synthesis/open-questions.md) OQ-P-354
- TTL 6시간과 최대 6시간짜리 재시도 백오프가 겹쳐, 늦게 재시도된 알림이 만료와 경합한다
  → [open-questions](../synthesis/open-questions.md) OQ-P-353
- 앱 `PushNotificationType`에 `BACKGROUND`·`TEXT`가 없고 `PushDeepLink.AddTopping`이 `type`을 `TOPPING`으로
  고정해, 배경·텍스트 알림 탭이 분석 이벤트에서 토핑 알림으로 집계된다 → OQ-P-445
- `TEXT` 알림이 outbox에 **텍스트 원문 스냅숏**을 담아, 발송 전에 수정·삭제된 텍스트도 원문 그대로 나가고
  원문이 보관 기간(2일) 동안 `notification_outbox`에 남는다 → OQ-P-446
- `TEXT` 알림만 닉네임을 8자로 자르지 않는다 — 의도인지 누락인지 근거가 없다 → OQ-P-447
- `BACKGROUND` 알림에 스로틀이 없어 배경을 연달아 바꾸면 변경 횟수만큼 푸시가 나간다. 배경·텍스트 알림(P-04 포함)의
  정책 근거가 위키에 없다 → OQ-P-448
