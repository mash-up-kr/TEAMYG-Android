---
id: parfait-text
title: 텍스트 토핑 배치(배치 확정·수정·삭제)
server_module: http/parfaittext
server_commit: 8b4a94b
verified: 2026-10-25
android_status: partial
related_spec:
related_adr:
tags: [api, parfait, server-contract, parfait-text]
---

# 텍스트 토핑 배치(배치 확정·수정·삭제) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

캔버스(파르페)에 **텍스트를 토핑으로 놓는** 도메인이다. 이미지 토핑([parfait-image.md](parfait-image.md))과
같은 좌표계와 같은 권한 모델을 쓰지만, 업로드 단계가 없다. JSON 요청 하나로 배치가 끝난다.

**텍스트 토핑에 닿는 경로는 넷이다.** 이 도메인의 셋(배치 · 단건 수정 · 삭제)과, 위치·크기·각도를
이미지와 함께 고치는 **이미지 도메인의 일괄 PATCH**(`textItems`)다 → [parfait-image.md](parfait-image.md)
"일괄 수정". 배치된 텍스트를 되읽는 경로는 캔버스 조회 응답의 `texts` 배열이다 → [parfait.md](parfait.md).

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| POST | `/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts` | 필요 | `PlaceParfaitTextRequest` | `TodayParfaitTextResponse` | 구현됨 |
| PATCH | `/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}` | 필요 | `UpdateParfaitTextRequest` | `UpdateParfaitTextResponse` | 구현됨 |
| DELETE | `/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}` | 필요 | 없음 | `null`(data 없음) | 구현됨 |

셋 다 `SecurityConfig.WHITELIST_PATHS`에 없어 **access token이 필요하다**. `groupId`·`parfaitId`·
`parfaitTextId`는 경로 변수이고 memberId는 토큰에서 나온다. 클래스 레벨 매핑
(`/api/v1/groups/{groupId}/parfaits/{parfaitId}/texts`)이 컨트롤러 셋
(`PlaceParfaitTextController`·`UpdateParfaitTextController`·`DeleteParfaitTextController`)에 각각 반복돼 있다.
Swagger 태그는 `ParfaitText`다.

⚠️ **세 엔드포인트 모두 요청 DTO에 Bean Validation이 없고 컨트롤러에 `@Valid`도 없다.** 검증은 전부
도메인(`ParfaitText`)이 한다. 그래서 OpenAPI 스키마의 `required` 배열이 비어 있다
([conventions.md](conventions.md) `required` 절). 비널 필드를 빼면 Jackson 역직렬화에서 400
`INVALID_REQUEST`가 난다(`PlaceParfaitTextControllerTest` "필수 필드가 빠지면 400으로 응답한다").

## 검증 규칙

`ParfaitText.validate`가 배치와 수정에서 같은 규칙을 돌린다. 수정은 **병합 뒤 값**을 검증한다.

| 대상 | 규칙 | 위반 코드 |
|---|---|---|
| `text` | `isBlank()`가 아니고 길이 2000 이하 | 400 `INVALID_TEXT` |
| `color` | 비어 있지 않고 길이 20 이하 | 400 `INVALID_TEXT_STYLE` |
| `positionX` · `positionY` · `rotation` | 유한한 값(NaN·무한대 아님) | 400 `INVALID_TEXT_STYLE` |
| `scale` · `fontSize` | 유한하고 0보다 큼 | 400 `INVALID_TEXT_STYLE` |
| `positionZ` | **검증 없음** | — |

- ⚠️ **`color`는 형식을 보지 않는다.** `#RRGGBB`가 아니어도 20자 이하면 저장된다. 다른 클라이언트가
  넣은 임의 문자열이 캔버스 조회로 내려올 수 있으니, 읽는 쪽이 파싱 실패를 처리해야 한다 → [미결](#미결).
- ⚠️ **`text` 길이는 Kotlin `String.length` 기준이다.** UTF-16 코드 유닛을 세므로 이모지 하나가 2로
  셀 수 있다. 앱이 글자 수 제한을 보여 줄 때 이 기준을 맞춰야 2000 근처에서 400을 피한다
  → [미결](#미결).
- **`rotation`에 범위가 없다.** 360을 넘는 값도 그대로 저장된다. `positionX`·`positionY`도 캔버스 범위를
  보지 않는다.
- **줄바꿈은 보존된다**(서버 문서 `docs/parfait-text-api.md`). 공백만 있는 텍스트는 400이다.
- **이미지 토핑과 검증 수준이 다르다.** 이미지의 `scale`·좌표는 검증이 하나도 없어서, 같은 값을 넣어도
  이미지는 저장되고 텍스트는 400이 난다.

## 엔드포인트 상세

### POST /api/v1/groups/{groupId}/parfaits/{parfaitId}/texts

- **인증**: 필요.
- **성공**: HTTP **201** · envelope `code` = `"CREATED"`(`@ResponseStatus(HttpStatus.CREATED)` +
  `ApiResponse.created`). 이미지 배치 POST와 같다.
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `text` | String | 필수(non-null 타입) | 공백만으로는 안 되고 최대 2000자. **배치 뒤에는 바꿀 수 없다** |
| `positionX` · `positionY` | Double | 필수(non-null 타입) | 이미지 토핑과 같은 좌표계 |
| `positionZ` | Int | 필수(non-null 타입) | 겹침 순서. **이미지와 텍스트가 한 축을 공유한다** |
| `scale` | Double | 필수(non-null 타입) | `fontSize`에 곱하는 배율. 양수 |
| `rotation` | Double | 필수(non-null 타입) | |
| `color` | String | 필수(non-null 타입) | 최대 20자. 형식 검증 없음 |
| `fontSize` | Double | 필수(non-null 타입) | 확대 전 기준 폰트 크기. 양수 |
| `isFill` | Boolean | 선택(기본값 `false`) | 텍스트 내부 채움 여부 |
| `isStroke` | Boolean | 선택(기본값 `false`) | 외곽선 표시 여부 |

  **표시 크기는 `fontSize × scale`이다.** 서버는 두 값을 따로 저장하고 따로 돌려주며 합치지 않는다.
  예: `fontSize` 16, `scale` 1.5면 표시 크기 24.

  **`isFill`·`isStroke`는 서로 독립이다.** 둘 다 `true`, 둘 다 `false`도 허용된다. JSON 키는
  `isFill`·`isStroke` 그대로다 — DTO가 `@JsonProperty`로 `is` 접두사를 고정한다(Kotlin `is` 프로퍼티는
  Jackson 기본 규칙에서 `fill`로 바뀔 수 있다).

  ⚠️ **같은 내용을 여러 번 보내면 행이 여러 개 생긴다.** 이미지 배치 POST의
  `(parfaitId, imageId)` upsert와 달리 텍스트 POST는 항상 새 배치다. 앱이 응답을 잃고 재시도하면 같은
  텍스트가 두 번 놓인다. 이 POST는 멱등이 아니다.

- **응답 필드** — 캔버스 조회의 `texts[]` 원소와 **같은 DTO**(`TodayParfaitTextResponse`)다.

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaitTextId` | Long | 아니오 | 텍스트 배치 행 id. 이후 PATCH·DELETE·일괄 PATCH가 쓰는 키 |
| `text` | String | 아니오 | 저장된 내용 |
| `positionX` · `positionY` | Double | 아니오 | |
| `positionZ` | Int | 아니오 | |
| `scale` · `rotation` | Double | 아니오 | |
| `color` | String | 아니오 | |
| `fontSize` | Double | 아니오 | |
| `isFill` · `isStroke` | Boolean | 아니오 | |
| `placedBy` | 객체 | 아니오 | `groupMemberId`(Long) · `nickname`(String) · `nameTagChip`(String(enum)) · `ownerType`(String(enum)) |
| `createdAt` | String(LocalDateTime) | 아니오 | 오프셋 없는 서버 로컬 시각. 배치 응답은 `LocalDateTime.now()` 값이라 소수 초가 붙을 수 있다 |

  **`placedBy`가 캔버스 조회와 같은 `PlacedByResponse`다.** 그래서 `ownerType`이 있고, 배치 응답에서는
  항상 `ME`다. 이미지 배치 응답의 `PlaceParfaitImagePlacedByResponse`에는 `ownerType`이 없다 — 두
  토핑의 배치 응답 모양이 다르다. `nickname`은 그룹 닉네임(`groupMember.groupNickname.value`)이다.

  **부수효과: 그룹의 다른 구성원에게 푸시 알림이 나간다**(`TextPlacedNotifier`). 수신자는 나간 사람과
  배치자 본인을 뺀 구성원이다. 위 비멱등성 때문에 **POST마다 알림이 나간다.** 문구·`data` 키는
  [notification.md](notification.md).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · 비널 필드 누락 · 타입 불일치(`CommonErrorCode`) |
| 403 | `GROUP_NOT_JOINED` | 그 그룹의 멤버가 아님(`ParfaitGroupApiErrorCode`) |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(**`ParfaitErrorCode`**) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(`ParfaitErrorCode`) |
| 400 | `INVALID_TEXT` | 텍스트가 공백뿐이거나 2000자 초과(`ParfaitTextErrorCode`) |
| 400 | `INVALID_TEXT_STYLE` | 색·좌표·각도·배율·폰트 크기 검증 실패(`ParfaitTextErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  **검사 순서**: 그룹 참여 → 파르페 존재 → 파르페 상태 → 도메인 검증. 도메인 검증이 맨 뒤라 마감된
  캔버스에 잘못된 텍스트를 보내면 400이 아니라 409가 온다.

  `PARFAIT_NOT_FOUND`는 이미지 도메인과 달리 `ParfaitErrorCode` 값이다. 와이어 문자열·status가 같아
  구분되지 않는다([parfait-image.md](parfait-image.md) "도메인 에러 코드 전수").

### PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}

- **인증**: 필요.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드** — 전부 널 허용이고, **생략하거나 `null`이면 기존 값을 유지한다**(부분 수정).

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `positionX` · `positionY` | Double? | 선택 | |
| `positionZ` | Int? | 선택 | |
| `scale` · `rotation` | Double? | 선택 | |
| `color` | String? | 선택 | |
| `fontSize` | Double? | 선택 | |
| `isFill` · `isStroke` | Boolean? | 선택 | `false`를 보내면 실제로 `false`가 된다 |

  ⚠️ **`text`는 요청에 없다.** 내용은 배치 뒤 바꿀 수 없다. 바디에 `text`를 넣어도 400이 아니라
  **조용히 무시**된다(`ParfaitTextMutationControllerTest` "수정 요청의 text 필드는 무시되어…").
  내용을 고치려면 지우고 다시 배치해야 하고, 그러면 `parfaitTextId`·`createdAt`이 바뀌고 알림도 다시 나간다.

  `ParfaitText.update`가 `color ?: this.color` 꼴로 병합한 뒤 [검증 규칙](#검증-규칙)을 다시 돌린다.
  빈 바디 `{}`도 유효하고 `updatedAt`과 캔버스 version만 오른다.

- **응답 필드** (`UpdateParfaitTextResponse`)

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaitTextId` | Long | 아니오 | |
| `text` | String | 아니오 | 저장된 기존 내용 |
| `positionX` · `positionY` | Double | 아니오 | 병합 후 값 |
| `positionZ` | Int | 아니오 | |
| `scale` · `rotation` | Double | 아니오 | |
| `color` | String | 아니오 | |
| `fontSize` | Double | 아니오 | |
| `isFill` · `isStroke` | Boolean | 아니오 | |

  POST 응답에 있던 `placedBy`·`createdAt`이 없다. 이미지 일괄 PATCH 응답의 `texts[]` 원소도 이 DTO다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · 필드 타입 불일치 · 경로 변수가 Long이 아님(`CommonErrorCode`) |
| 404 | `PARFAIT_TEXT_NOT_FOUND` | `parfaitTextId` 부재이거나 그 배치의 `parfaitId`가 경로와 다름(`ParfaitTextErrorCode`) |
| 403 | `PARFAIT_TEXT_NOT_OWNED` | 본인이 배치한 텍스트가 아님 · **그룹 미참여도 같은 코드**(`ParfaitTextErrorCode`) |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitErrorCode`) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(`ParfaitErrorCode`) |
| 400 | `INVALID_TEXT_STYLE` | 병합 결과가 검증 실패(`ParfaitTextErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  **검사 순서**: 배치 존재 → 소유권 → 파르페 존재 → 파르페 상태 → 도메인 검증. 이미지 단건 PATCH와
  같은 순서다. 그래서 마감된 캔버스에서 남의 텍스트를 고치려 하면 409가 아니라 403이 먼저 온다.
  `INVALID_TEXT`는 이 경로에서 나오지 않는다 — 검증하는 `text`가 이미 저장된 값이어서다.

### DELETE /api/v1/groups/{groupId}/parfaits/{parfaitId}/texts/{parfaitTextId}

- **인증**: 필요.
- **성공**: HTTP **200** · envelope `code` = `"OK"` · `data` = `null`. 이미지 토핑 DELETE와 같은 표현이다
  (204 아님).
- **요청 필드**: 없음(경로 변수 3개뿐)
- **응답 필드**: 없음(`data`가 `null`)

  부수효과는 행 삭제와 캔버스 version 증가뿐이다. 이미지 삭제와 달리 참조 카운트·S3 객체가 없다.
  **삭제는 멱등이 아니다** — 같은 `parfaitTextId`를 두 번 지우면 두 번째는 404 `PARFAIT_TEXT_NOT_FOUND`다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 경로 변수가 Long이 아님(`CommonErrorCode`) |
| 404 | `PARFAIT_TEXT_NOT_FOUND` | `parfaitTextId` 부재이거나 그 배치의 `parfaitId`가 경로와 다름 |
| 403 | `PARFAIT_TEXT_NOT_OWNED` | 본인이 배치한 텍스트가 아님 · **그룹 미참여도 같은 코드** |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitErrorCode`) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(`ParfaitErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  검사 순서는 PATCH와 같다(배치 존재 → 소유권 → 파르페 존재 → 파르페 상태). 마감 검사가 삭제 실행보다
  앞이라 마감된 캔버스의 텍스트는 지워지지 않는다.

## 캔버스 version과 동시성

세 서비스(`PlaceParfaitTextService`·`UpdateParfaitTextService`·`DeleteParfaitTextService`)가 모든 검증을
마친 뒤, 행 쓰기 바로 앞에서 `ParfaitVersionService.bump(parfaitId)`를 부른다. 이미지 토핑 다섯
엔드포인트와 같은 규칙이다 → [parfait-image.md](parfait-image.md) "캔버스 version 증가".

- 성공한 쓰기는 v2 오늘 조회의 `ETag`를 바꾼다. 4xx로 끝난 요청은 바꾸지 않는다.
- 값이 안 바뀐 PATCH(`{}`)도 성공이면 version이 오른다.
- `bump`가 부모 `parfait` 행에 배타 락을 먼저 잡으므로, 같은 캔버스에 대한 이미지·텍스트 쓰기가
  이 지점에서 직렬화된다.

## 저장소 제약

`V21__create_parfait_text.sql`이 테이블을 만들고 `V22__add_fill_and_stroke_to_parfait_text.sql`이 두
Boolean 컬럼을 붙였다. 앞서 적은 계약과 맞물리는 것만 적는다.

- `text VARCHAR(2000)` · `color VARCHAR(20)` · 나머지 전부 `NOT NULL`. 도메인 검증 상한과 같다.
- `is_fill`·`is_stroke`는 `NOT NULL DEFAULT b'0'`. 기존 행은 `false`로 채워졌다.
- 인덱스는 `idx_parfait_text_parfait_id` 하나다.
- ⚠️ **FK가 없다.** `parfait_id`·`placed_by_group_member_id`가 `parfait`·`parfait_group_member`를
  참조하지 않는다. `parfait_image`는 같은 두 컬럼에 FK가 있다(`fk_parfait_image_parfait`·`fk_parfait_image_placed_by_group_member`). 계약상 차이는 지금 없다. 쓰기 경로가
  전부 서비스에서 존재를 먼저 확인하기 때문이다. 다만 서비스 주석이 말하는 "FK 공유 락 → 승격 데드락"은
  이 테이블에는 해당하지 않는다. 텍스트 경로의 선행 `bump`는 이미지 경로와 락 순서를 맞추는 의미만 있다.
- 캔버스 조회는 `findAllByParfaitIdOrderByPositionZAscIdAsc`로 텍스트를 읽는다. **텍스트끼리는
  `positionZ` → `id` 순서**다. 이미지와 섞인 전체 겹침 순서는 응답이 정해 주지 않는다. 두 배열을
  합쳐 `positionZ`로 정렬하는 것은 소비 측 몫이고, 이미지와 텍스트의 `positionZ`가 같을 때 어느 쪽이
  위인지는 계약에 없다 → [미결](#미결).

## 캔버스 조회에서 되읽기

오늘 조회(v1·v2)와 상세 조회 응답에 `texts`가 붙는다. 원소는 이 문서의 POST 응답과 같은
`TodayParfaitTextResponse`다. 텍스트가 없으면 `images`와 같은 규칙으로 `texts: null`이다(서버 문서
기준). 응답 서술의 정본은 [parfait.md](parfait.md)다.

## 도메인 에러 코드 전수

`ParfaitTextErrorCode`(`core/parfaittext/exception`) 4종 전부.

| HTTP | code | message | 귀속 |
|---|---|---|---|
| 404 | `PARFAIT_TEXT_NOT_FOUND` | 존재하지 않는 텍스트 배치입니다 | PATCH · DELETE · 이미지 일괄 PATCH(`textItems`) |
| 403 | `PARFAIT_TEXT_NOT_OWNED` | 본인이 배치한 텍스트가 아닙니다 | PATCH · DELETE · 이미지 일괄 PATCH(`textItems`) |
| 400 | `INVALID_TEXT` | 텍스트는 공백이 아닌 1~2000자여야 합니다 | POST |
| 400 | `INVALID_TEXT_STYLE` | 색상, 위치, 각도, 확대 배율 또는 폰트 크기가 올바르지 않습니다 | POST · PATCH · 이미지 일괄 PATCH(`textItems`) |

이 도메인은 자기 enum 밖의 코드도 던진다. `ParfaitErrorCode.PARFAIT_NOT_FOUND`(404) ·
`ParfaitErrorCode.PARFAIT_ALREADY_CLOSED`(409) · `ParfaitGroupApiErrorCode.GROUP_NOT_JOINED`(403, POST만)다.

⚠️ **그룹 미참여 코드가 경로마다 다르다.** POST는 `GROUP_NOT_JOINED`, PATCH·DELETE는
`PARFAIT_TEXT_NOT_OWNED`, 이미지 일괄 PATCH는 텍스트만 보내도 `PARFAIT_IMAGE_NOT_OWNED`다. 이미지 토핑의
비대칭을 그대로 따른다.

⚠️ **소유권 기준은 멤버십 행 id(`groupMemberId`)다.** `findByGroupIdAndMemberId`(= `leftAt IS NULL`)로
찾은 멤버십 id를 `placed_by_group_member_id`와 비교한다. 재참여가 기존 멤버십 행을 되살리므로, 나갔다
다시 들어온 회원은 자기 텍스트의 소유권을 되찾는다. 이미지 토핑과 같다
→ [parfait-image.md](parfait-image.md) "도메인 에러 코드 전수".

## 명세 델타

서버 저장소의 팀 문서 `docs/parfait-text-api.md`와 코드가 대체로 일치한다. 어긋나거나 문서에만 있는 것:

- 문서는 DDL이 "파르페·배치자 ID"를 포함한다고만 적는다. 코드상 **FK는 없다**(위 [저장소 제약](#저장소-제약)).
- 문서의 "텍스트가 없으면 `texts: null`"은 이 라운드에서 응답 쪽 코드로 확인하지 않았다. [parfait.md](parfait.md)
  담당 범위다.

## Android 매핑

표면만 있고 소비처가 없다(Repository·UseCase·화면 0건).

| 서버 | Android |
|---|---|
| 3 엔드포인트 | `ParfaitTextService` → `ParfaitTextRemoteDataSource`(`placeText`·`updateText`·`deleteText`) |
| `TodayParfaitTextResponse` | 같은 이름 DTO(`response/parfait`). 배치 POST 응답과 캔버스 `texts` 원소가 공유한다 |
| 도메인 모델 | `ParfaitTextId` · `PlacedTextToppingVO` · `UpdatedTextToppingVO` · `TextToppingStyle` · `TextToppingUpdate` |

- `PlacedTextToppingVO.placedBy`는 이미지 배치와 같은 `ToppingPlacerVO`로 접는다 — `ownerType`·`nameTagChip`·
  `createdAt`은 DTO에만 있고 VO로 올리지 않았다(읽는 화면이 생길 때 올린다).
- `TextToppingUpdate`에 `text`가 없다. 서버가 내용 수정을 받지 않기 때문이다.
- **일괄 PATCH의 `textItems`·`texts`는 DTO만 있다**(`UpdateParfaitImagesRequest.textItems` 기본 `[]`,
  `UpdateParfaitImagesResponse.texts` 기본 `[]`). `ParfaitImageRemoteDataSource.updateToppings`는 아직
  이미지 항목만 보낸다. `encodeDefaults = true`라 매 요청에 `"textItems": []`가 실린다 — 서버 기본값과 같은 뜻이다.
- **캔버스 `GetTodayParfaitResponse.texts`도 DTO만 있다.** 캔버스 VO 매핑은 아직 없다(OQ-P-439).

## 미결

- `color`에 형식 검증이 없다. 앱·iOS·서버가 어떤 색 문자열 형식(`#RRGGBB`, 알파 포함 여부 등)을 쓸지
  합의가 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-441
- `text` 2000자 상한을 Kotlin `String.length`(UTF-16 코드 유닛)로 센다. 앱의 글자 수 표시 기준과
  맞출지 정해야 한다. 이 라운드에서 운영 DB의 `parfait_text` 문자셋은 확인하지 못했다 —
  `utf8mb4`가 아니면 이모지 INSERT가 500일 수 있다 → [open-questions](../synthesis/open-questions.md) OQ-P-442
- 이미지와 텍스트가 `positionZ` 한 축을 공유하는데, 값이 같을 때의 겹침 순서와 `positionZ` 배정 규칙
  (누가 다음 z를 정하는지)이 계약에 없다 → [open-questions](../synthesis/open-questions.md) OQ-P-443
- POST가 멱등이 아니다. 응답을 잃은 재시도가 같은 텍스트를 두 번 놓고 알림도 두 번 보낸다. 앱이
  재시도를 어떻게 다룰지 정해야 한다 → [open-questions](../synthesis/open-questions.md) OQ-P-444
- POST에 `isFill`·`isStroke`를 `null`로 **명시**해 보낼 때의 동작(기본값 적용인지 400인지)을 이 라운드에서
  확인하지 못했다. 컨트롤러 테스트는 키 생략만 잠근다.
