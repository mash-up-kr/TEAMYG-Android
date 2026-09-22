---
id: parfait-image
title: 토핑 배치(배치 확정·위치/크기/각도 수정·일괄 수정·테두리 수정·삭제)
server_module: http/parfaitimage
server_commit: d76b27a
verified: 2026-09-10
android_status: done
related_spec: 2026-08-15-parfait-canvas-topping-member-api-service-layer
related_adr: ADR-0017
tags: [api, parfait, server-contract, parfait-image]
---

# 토핑 배치(배치 확정·위치/크기/각도 수정·일괄 수정·테두리 수정·삭제) API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

`feat: 토핑 배치 확정 API 구현`(PR #75)과 `feat: 토핑 위치/크기/각도 수정 API 구현`(PR #81)으로 신설됐고,
`feat: 토핑 테두리 두께/색깔 수정 API 구현`(PR #83)·`feat: 토핑 삭제 API 구현`(PR #88)으로 **2 → 4**가 됐으며,
`feat: 토핑 여러 개를 한 번에 수정하는 배치 API 추가`(PR #119)가 **4 → 5**로 올렸다.
[image.md](image.md)로 올린 이미지를 **캔버스(파르페) 위 좌표에 놓는** 단계다 — 업로드와 배치가 서로 다른
도메인으로 갈려 있다.

**두 단계가 이어진다.** ① `POST /api/v1/images` + S3 PUT + confirm으로 이미지를 `COMPLETED`로 만들고
([image.md](image.md)) ② 그 `imageId`를 이 API에 넘겨 파르페에 배치한다. `COMPLETED`가 아니면 409다.

**배치된 토핑을 되읽는 경로가 생겼다** — 이 도메인이 아니라 [parfait.md](parfait.md)의
`GET .../parfaits/today`가 `images` 배열로 내려준다. 이 문서의 오래된 "다시 그릴 수 없다"는 그것으로 닫혔다.

✅ **2026-08-20 — 네 엔드포인트가 전부 캔버스 마감 상태를 본다**(`fix: 마감된 파르페에 대한 편집 요청
거부`, PR #109). 대상 파르페의 `status`가 `ACTIVE`가 아니면 **409 `PARFAIT_ALREADY_CLOSED`**
(`ParfaitErrorCode` — 이 도메인 enum이 아니다)로 거부한다. 요청·응답 형태는 그대로이고 **실패 경로만
늘었다.** 배치를 뺀 셋은 그 김에 **파르페 존재·그룹 소속 검사까지 처음 갖게 됐다**(404
`PARFAIT_NOT_FOUND`) — 그전에는 경로의 `parfaitId`를 배치 행과 대조만 하고 `groupId`는 멤버십 확인에만
썼다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| POST | `/api/v1/groups/{groupId}/parfaits/{parfaitId}/images` | 필요 | `PlaceParfaitImageRequest` | `PlaceParfaitImageResponse` | 구현됨 |
| PATCH | `.../images/{parfaitImageId}` | 필요 | `UpdateParfaitImageRequest` | `UpdateParfaitImageResponse` | **표면 없음**(2026-08-31 걷어냄) |
| PATCH | `/api/v1/groups/{groupId}/parfaits/{parfaitId}/images` | 필요 | `UpdateParfaitImagesRequest` | `UpdateParfaitImagesResponse` | 구현됨(2026-08-31 신설) |
| PATCH | `.../images/{parfaitImageId}/border` | 필요 | `UpdateParfaitImageBorderRequest` | `UpdateParfaitImageBorderResponse` | 구현됨 |
| DELETE | `.../images/{parfaitImageId}` | 필요 | 없음 | `null`(data 없음) | 구현됨 |

다섯 다 `SecurityConfig.WHITELIST_PATHS`에 없어 **access token이 필요하다**. `groupId`·`parfaitId`는
경로 변수이고 memberId는 토큰에서 나온다. 클래스 레벨 매핑
(`/api/v1/groups/{groupId}/parfaits/{parfaitId}/images`)이 **컨트롤러 5개에 각각 반복**돼 있다 —
`PlaceParfaitImageController`·`UpdateParfaitImageController`·`UpdateParfaitImagesController`·
`UpdateParfaitImageBorderController`·`DeleteParfaitImageController`가 한 경로를 나눠 갖는다.

⚠️ **컬렉션 경로 하나에 POST와 PATCH가 서로 다른 컨트롤러로 걸려 있다** — `PlaceParfaitImageController`가
`@PostMapping`, `UpdateParfaitImagesController`가 **경로 인자 없는 `@PatchMapping`**을 갖는다. 즉 같은
URL이 메서드로 갈려 배치 확정과 일괄 수정을 나눠 맡는다.

⚠️ **`images`라는 세그먼트가 두 도메인에 있다.** 최상위 `/api/v1/images`는 업로드([image.md](image.md)),
그룹 하위 `.../parfaits/{parfaitId}/images`는 배치다. 두 경로의 `imageId`와 `parfaitImageId`는
**다른 키**다 — 전자는 `image_meta` 행, 후자는 배치 행을 가리킨다.

## 엔드포인트 상세

### POST /api/v1/groups/{groupId}/parfaits/{parfaitId}/images

- **인증**: 필요.
- **성공**: HTTP **201** · envelope `code` = `"CREATED"`(`@ResponseStatus(HttpStatus.CREATED)` +
  `ApiResponse.created`). 리소스 생성 POST에 `ApiResponse.ok`를 쓴 [image.md](image.md)와 다르다 —
  같은 서버 안에서 생성 POST의 성공 코드가 두 갈래다.
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `imageId` | Long | 필수(non-null 타입) | `POST /api/v1/images`가 내려준 `imageId`. **`COMPLETED` 상태여야 한다** |
| `positionX` | Double | 필수(non-null 타입) | |
| `positionY` | Double | 필수(non-null 타입) | |
| `positionZ` | Int | 필수(non-null 타입) | 겹침 순서 |
| `scale` | Double | 필수(non-null 타입) | |
| `rotation` | Double | 필수(non-null 타입) | |
| `borderType` | String(enum) | 필수(non-null 타입) | `NONE` · `SOLID` |
| `borderColor` | String? | 선택 | `borderType=SOLID`면 **필수**(아래) |
| `borderWidth` | Double? | 선택 | `borderType=SOLID`면 **필수**(아래) |

  ⚠️ **검증 애노테이션이 하나도 없다.** `PlaceParfaitImageRequest`에 `@NotNull`·`@Positive` 류가 없고
  컨트롤러도 `@Valid`를 붙이지 않는다. 결과는 둘이다 — ① **OpenAPI 스키마의 `required` 배열이 비어 있다**
  (springdoc이 Bean Validation에서만 `required`를 유도한다, [conventions.md](conventions.md) 참고). 스키마에서
  클라이언트를 생성하면 전 필드가 nullable로 떨어지는데 실제로는 비우면 400이다. ② **좌표·배율·각도의
  범위 검증이 서버에 없다** — 음수 `scale`, 캔버스 밖 좌표, 360을 넘는 `rotation`이 그대로 저장된다
  → [미결](#미결).

  ⚠️ **2026-08-24 — 이 엔드포인트가 운영에서 500이었다.** 요청·응답 형태와는 무관하고 원인은 운영 DB
  스키마다. Flyway가 꺼져 있던 기간에 `ddl-auto: update`가 스키마를 대신 관리해 제약·기본값을 바꾸는
  마이그레이션이 반영되지 않았고, 그 상태에서 `parfait_image` INSERT가 실패했다(`#110` 커밋 본문이
  증상만 적고 어느 제약이 걸렸는지는 밝히지 않는다). `V16`이 그 차이를 메운다 — 이 도메인에 걸린 것은
  `parfait_image`의 `placed_by_group_member_id` FK 복원이다. 계약 자체는 한 글자도 안 바뀌었다
  → [conventions.md 스키마 소유권](conventions.md#스키마-소유권--코드가-정본이어도-운영-응답은-다를-수-있다).

  ⚠️ **`borderType`이 enum 밖 값이면 Jackson 역직렬화가 먼저 깨져** `HttpMessageNotReadableException`을
  타고 `INVALID_REQUEST`(400)가 된다. 도메인 코드가 아니라 공통 코드다([image.md](image.md)의
  `imageType`과 같은 구조).

  **`SOLID`면 색·두께가 함께 와야 한다.** `ParfaitImage.validateBorder`가 `borderType == SOLID`이고
  `borderColor`나 `borderWidth`가 `null`이면 `INVALID_BORDER`(400)를 던진다. `NONE`이면 둘 다 무시된다
  (값을 보내도 그대로 저장될 뿐 검증하지 않는다).

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaitImageId` | Long | 아니오 | 배치 행의 id. 이후 PATCH가 쓰는 키 |
| `imageId` | Long | 아니오 | 요청에 넣은 `image_meta` id 그대로 |
| `imageUrl` | String | 아니오 | `ImageMeta.url` — 배치 시점에 복사해 배치 행에 들고 있는다 |
| `positionX` · `positionY` | Double | 아니오 | 저장된 값 |
| `positionZ` | Int | 아니오 | |
| `scale` · `rotation` | Double | 아니오 | |
| `placedBy` | 객체 | 아니오 | `groupMemberId`(Long) · `nickname`(String) · `nameTagChip`(String(enum), **2026-08-19 신설**) |

  🔁 **2026-08-19 — `placedBy`가 칩을 싣고, 그 중첩 타입이 개명됐다**(`fix: placedBy 스키마 이름 충돌 해소
  및 nameTagChip 필드명을 스펙에 맞게 통일`). 이 응답의 `PlacedByResult`/`PlacedByResponse`가 캔버스 조회
  응답([parfait.md](parfait.md))의 동명 클래스와 겹쳐 **springdoc이 두 스키마를 같은 것으로 취급했고**,
  그 탓에 캔버스 쪽에 추가한 칩 필드가 스웨거에 안 나왔다. 토핑 배치 쪽을
  `PlaceParfaitImagePlacedByResult`/`PlaceParfaitImagePlacedByResponse`로 개명해 충돌을 없앴다.
  값은 **이미 조회한 `groupMember`에서 바로 꺼내므로 추가 쿼리가 없고**, 방금 배치한 사람의 칩을
  재조회 없이 쓸 수 있다. 값 집합·배정 규칙은 [parfait-group.md](parfait-group.md) "Nametag-Chip 배정 규칙".
  근거: `PlaceParfaitImageControllerTest`가 `placedBy.nameTagChip`(`"TYPE6"`)을 `jsonPath`로 단언한다.

  ⚠️ **응답 DTO 이름이 계층마다 갈렸다** — 서버 HTTP DTO는 `PlaceParfaitImagePlacedByResponse`인데
  앱 `data/service/model/response/parfaitimage/`는 아직 `PlacedByResponse`다. "wire DTO는 서버의 거울"이라는
  규약과 어긋난 자리다 → [Android 매핑](#android-매핑).

  ⚠️ **요청에 보낸 `borderType`·`borderColor`·`borderWidth`가 응답에 없다.** 저장은 되지만
  `PlaceParfaitImageResponse`에 필드가 없어 되돌려 받지 못한다. 다만 **되읽을 자리는 생겼다** —
  [parfait.md](parfait.md)의 `GET .../parfaits/today`가 `images[]`에 테두리 3필드를 실어 준다.
  배치 직후 응답으로는 못 받고 캔버스를 다시 조회해야 안다는 뜻이다.

  `placedBy.nickname`은 **그룹 닉네임**(`groupMember.groupNickname.value`)이지 전역 닉네임이 아니다
  ([member.md](member.md) 참고).

  **부수효과: `image_meta.reference_count`가 1 오른다** — 단, **새 배치일 때만**이다
  (`PlaceParfaitImageService`가 `existing == null`인 경우에만 `imageMeta.incrementReferenceCount()`를 저장).
  같은 `(parfaitId, imageId)` 재-POST(아래 upsert)는 카운트를 올리지 않는다. 이 값이 0이 되는 순간 S3 객체가
  지워진다(아래 DELETE 절) — [image.md](image.md)에 "증감 경로가 없다"고 적혀 있던 자리가 이 라운드로 채워졌다.

  **부수효과 2(2026-09-04 신설): 그룹의 다른 구성원에게 푸시 알림이 나간다.** 같은
  `existing == null` 분기가 `ToppingPlacedNotifier.notify`를 부른다 — **새 배치만이고 재-POST(이동)는
  알림을 만들지 않는다.** 수신자는 그룹 구성원에서 **나간 사람과 배치자 본인을 뺀** 나머지이고, 실제
  발송은 이 요청이 아니라 커밋 뒤 워커가 한다. **응답이 발송 결과를 담지 않는다** — 알림이 안 가도
  이 API는 201이다 → [notification.md](notification.md) "서버가 보내는 푸시".

  ⚠️ **알림 큐 적재가 이 트랜잭션 안에서 일어난다.** `NotificationOutboxAdapter.saveAll`이 이미 큐잉된
  `dedup_key`를 미리 걸러 정상 경로에서는 제약 위반이 나지 않지만, **같은 키로 동시 저장이 겹치는 드문
  경합에서는 나중 트랜잭션이 롤백된다** — 즉 **알림 적재 실패가 토핑 배치 자체를 실패시킬 수 있다.**
  서버 커밋 메시지가 이것을 at-least-once의 대가로 명시하고 상위 재시도를 전제한다. 앱이 배치 실패를
  삼키면 사용자에게는 "토핑이 안 올라갔다"로 보인다.

  ⚠️ **같은 `(parfaitId, imageId)`로 다시 POST하면 새 행이 생기지 않고 기존 배치가 이동한다.**
  `PlaceParfaitImageService`가 `findByParfaitIdAndImageMetaId`로 기존 배치를 찾아 있으면
  `ParfaitImage.reposition`을 호출한다. 이때 **`placedByGroupMemberId`가 호출자로 다시 쓰인다** — 즉
  **남이 배치한 토핑도 같은 `imageId`만 알면 옮기고 소유자까지 가져올 수 있다.** POST에는 소유자 검사가
  없고 PATCH에만 있다(아래) → [미결](#미결).

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · `borderType` enum 밖 값 · 비널 필드 누락(`CommonErrorCode`) |
| 400 | `INVALID_BORDER` | `SOLID`인데 색·두께 없음(`ParfaitImageErrorCode`) |
| 403 | `GROUP_NOT_JOINED` | 그 그룹의 멤버가 아님(`ParfaitGroupApiErrorCode`) |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitImageErrorCode`) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(**`ParfaitErrorCode`**, 2026-08-20 신설) |
| 404 | `IMAGE_NOT_FOUND` | `imageId` 부재(`ImageErrorCode`) |
| 409 | `IMAGE_NOT_CONFIRMED` | 이미지가 `COMPLETED`가 아님(`ParfaitImageErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  **검사 순서**가 코드에 그대로 있다 — 그룹 참여 → 파르페 존재 → **파르페 상태** → 이미지 존재 →
  이미지 상태 → 테두리 검증. 앞이 걸리면 뒤는 실행되지 않는다.
  근거: `PlaceParfaitImageControllerTest`가 성공 201 포함 6케이스를 검증하고, 마감 거부는
  `PlaceParfaitImageServiceTest`("이미 마감된 파르페면 PARFAIT_ALREADY_CLOSED를 던진다")가 잠근다.

  ⚠️ **`PARFAIT_NOT_FOUND`는 "존재하지 않음"과 "다른 그룹 것"을 구분하지 않는다.**
  `findByIdAndGroupId(parfaitId, groupId)` 하나로 판정하므로, 남의 그룹 파르페를 지목해도 404다.

  ✅ **파르페 상태를 본다**(2026-08-20). 존재 확인이 `existsByIdAndGroupId`(불리언)에서
  `findByIdAndGroupId`(엔티티)로 바뀌면서 **같은 조회로 `status`까지 읽게 됐고**, `ACTIVE`가 아니면
  409다. 쓰임이 사라진 `existsByIdAndGroupId`는 포트·어댑터·리포지토리에서 함께 걷혔다.
  직전 판본이 "마감된 캔버스에도 토핑을 올릴 수 있다 · 막는 것은 앱 책임"이라고 적던 자리다.

### PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}

- **인증**: 필요.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드** — 전부 널 허용이고, **`null`이면 기존 값을 유지한다**(부분 수정).

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `positionX` | Double? | 선택 | |
| `positionY` | Double? | 선택 | |
| `positionZ` | Int? | 선택 | |
| `scale` | Double? | 선택 | |
| `rotation` | Double? | 선택 | |

  `ParfaitImage.update`가 `positionX ?: this.positionX` 꼴로 병합한다. **빈 바디 `{}`도 유효하고**
  `updatedAt`만 올라간다(에러가 아니다).

  **이 PATCH는 테두리를 다루지 않는다.** 요청에 `borderType`·`borderColor`·`borderWidth`가 없고
  `ParfaitImage.update`도 기존 값을 그대로 복사한다. 테두리는 **형제 엔드포인트 `.../border`**가 맡는다
  (아래) — 2026-08-15 delta 이전에는 그 경로가 없어 같은 `imageId` 재-POST가 유일한 우회였다.

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaitImageId` | Long | 아니오 | |
| `positionX` · `positionY` | Double | 아니오 | 병합 후 값 |
| `positionZ` | Int | 아니오 | |
| `scale` · `rotation` | Double | 아니오 | |

  POST 응답에 있던 `imageId`·`imageUrl`·`placedBy`가 없다 — **같은 리소스인데 두 응답의 필드 집합이 다르다.**

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · 경로 변수가 Long이 아님(`CommonErrorCode`) |
| 404 | `PARFAIT_IMAGE_NOT_FOUND` | `parfaitImageId` 부재이거나 그 배치의 `parfaitId`가 경로와 다름(`ParfaitImageErrorCode`) |
| 403 | `PARFAIT_IMAGE_NOT_OWNED` | 본인이 배치한 토핑이 아님(`ParfaitImageErrorCode`) |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitImageErrorCode`, **2026-08-20 신설**) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(**`ParfaitErrorCode`**, 2026-08-20 신설) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  **검사 순서**: 배치 존재 → 소유권 → **파르페 존재 → 파르페 상태**. 표의 순서가 아니라 이 순서다 —
  뒤의 둘이 2026-08-20에 붙었고 **소유권 검사보다 뒤**라, 남의 토핑을 마감된 캔버스에서 고치려 하면
  409가 아니라 403이 온다.

  ⚠️ **그룹 미참여도 `PARFAIT_IMAGE_NOT_OWNED`(403)로 나간다.** `UpdateParfaitImageService`는
  `findByGroupIdAndMemberId`가 `null`이든, 찾은 멤버가 배치자가 아니든 **같은 코드**를 던진다 —
  POST가 미참여를 `GROUP_NOT_JOINED`로 구분하는 것과 다르다. 앱이 "그룹에서 나갔다"와 "남의 토핑이다"를
  구분해 안내하려면 코드만으로는 안 된다.
  근거: `UpdateParfaitImageControllerTest`가 성공 200 + 404 + 403 세 케이스를 검증하고, 신설 둘은
  `UpdateParfaitImageServiceTest`가 잠근다.

  **POST와 PATCH의 권한 모델이 비대칭이다** — PATCH는 배치자 본인만, POST는 그룹 멤버 누구나
  남의 배치를 덮어쓸 수 있다(위 upsert 참고).

### PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/images (일괄 수정)

`feat: 토핑 여러 개를 한 번에 수정하는 배치 API 추가`(PR #119)로 신설됐다. 단건 위치 PATCH를
개수만큼 반복 호출하던 것을 **요청 하나로 접는다** — 커밋 본문이 밝힌 동기가 그것이고, 앱이 실제로
`async` + `awaitAll`로 단건을 병렬 호출하고 있다([Android 매핑](#android-매핑)).

- **인증**: 필요.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `items` | List<객체> | 필수(non-null 타입) | 수정할 배치 목록 |

  항목(`UpdateParfaitImageItemRequest`) 필드는 **`parfaitImageId`(Long, 비널) + 단건 PATCH의 다섯 필드**다.

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `parfaitImageId` | Long | 필수(non-null 타입) | 수정 대상 배치 행 |
| `positionX` | Double? | 선택 | `null`이면 기존 값 유지 |
| `positionY` | Double? | 선택 | |
| `positionZ` | Int? | 선택 | |
| `scale` | Double? | 선택 | |
| `rotation` | Double? | 선택 | |

  **병합 규칙이 단건 PATCH와 같다** — 항목마다 `ParfaitImage.update`를 그대로 태우므로 `null` 필드는
  기존 값을 유지한다. 테두리는 여기서도 다루지 않는다.

  ⚠️ **`items`가 빈 배열이면 검증을 하나도 돌지 않고 200 + `images: []`다.** `UpdateParfaitImagesService`가
  `items.isEmpty()`면 즉시 `emptyList()`를 반환해 **그룹 소속·파르페 존재·마감 검사보다 앞에서 끊는다** —
  그룹 밖 사람이 빈 요청을 보내도 403이 아니라 200이다.
  근거: `UpdateParfaitImagesServiceTest` "항목이 비어있으면 조회·저장 없이 빈 목록을 반환한다".

  ⚠️ **검증 애노테이션도 `@Valid`도 없고 `items` 개수 상한도 없다.** 이 도메인의 다른 요청 DTO와 같은
  상태이고(OpenAPI `required` 공백), 여기서는 **한 요청이 수정하는 행 수에 서버 상한이 없다**는 뜻이 더해진다
  → [미결](#미결).

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `images` | List<객체> | 아니오 | 갱신된 배치 목록. 0건이면 **빈 배열** |

  원소는 **단건 PATCH 응답 DTO를 그대로 재사용한다**(`UpdateParfaitImageResponse`) —
  `parfaitImageId` · `positionX`/`positionY`(Double) · `positionZ`(Int) · `scale`/`rotation`(Double).
  단건과 마찬가지로 `imageId`·`imageUrl`·`placedBy`·테두리 3필드가 없다.
  **순서는 `saveAll` 반환 순서**이고 요청 항목 순서와 같다고 계약이 보장하지 않는다 — 소비 측은
  `parfaitImageId`로 맞춰야 한다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · `items` 누락 · 항목의 `parfaitImageId` 누락(`CommonErrorCode`) |
| 403 | `PARFAIT_IMAGE_NOT_OWNED` | **그룹 미참여** · 항목 중 하나라도 본인이 배치한 토핑이 아님(`ParfaitImageErrorCode`) |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitImageErrorCode`) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(**`ParfaitErrorCode`**) |
| 404 | `PARFAIT_IMAGE_NOT_FOUND` | 항목 중 하나라도 부재이거나 그 배치의 `parfaitId`가 경로와 다름(`ParfaitImageErrorCode`) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  **검사 순서**: 빈 항목 → **그룹 소속 → 파르페 존재 → 파르페 상태** → (항목마다) 배치 존재 → 소유권.

  ⚠️ **단건 PATCH와 검사 순서가 뒤집혀 있다.** 단건은 배치 존재 → 소유권 → 파르페 존재 → 파르페 상태라
  소유권이 마감보다 **앞**이고, 일괄은 마감이 소유권보다 **앞**이다. 결과가 갈린다 — **마감된 캔버스에서
  남의 토핑을 고치려 하면 단건은 403 `PARFAIT_IMAGE_NOT_OWNED`, 일괄은 409 `PARFAIT_ALREADY_CLOSED`다.**
  같은 일을 하는 두 엔드포인트가 같은 상황에 다른 코드를 낸다.

  ⚠️ **그룹 미참여도 `PARFAIT_IMAGE_NOT_OWNED`(403)다** — 단건·테두리·삭제와 같은 처리이고 POST만
  `GROUP_NOT_JOINED`로 구분한다.

  ⚠️ **부분 성공이 없다.** `@Transactional` 하나로 묶여 있어 항목 하나가 404·403을 내면 앞서 병합된
  항목들도 **전부 롤백**된다. 어느 항목이 걸렸는지는 응답에 없다 — 코드만 오고 `parfaitImageId`는 안 온다
  → [미결](#미결).

  ⚠️ **같은 `parfaitImageId`를 중복해 보내도 막지 않는다.** `findAllByIds` 결과를 id로 색인해 항목마다
  꺼내 쓰므로 중복 항목은 같은 행을 두 번 병합하고 `saveAll`에도 두 번 실려, 응답 `images`에 같은
  `parfaitImageId`가 두 번 나온다.
  근거: `UpdateParfaitImagesControllerTest`가 성공 200(2건) · 404 · 403 세 케이스를,
  `UpdateParfaitImagesServiceTest`가 빈 항목·그룹 미참여·파르페 부재·마감·항목별 부재·항목별 소유권·
  경로 `parfaitId` 불일치까지 여덟 케이스를 잠근다.

### PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}/border

`feat: 토핑 테두리 두께/색깔 수정 API 구현`(PR #83)으로 신설됐다. 위치 PATCH와 **다른 경로·다른 컨트롤러**다
(`UpdateParfaitImageBorderController`).

- **인증**: 필요.
- **성공**: HTTP 200 · envelope `code` = `"OK"`(`ApiResponse.ok`, `@ResponseStatus` 없음)
- **요청 필드** — 위치 PATCH와 달리 **부분 수정이 아니다.** 보낸 값으로 세 필드가 통째로 덮인다.

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `borderType` | String(enum) | 필수(non-null 타입) | `NONE` · `SOLID` |
| `borderColor` | String? | 선택 | `borderType=SOLID`면 **필수** |
| `borderWidth` | Double? | 선택 | `borderType=SOLID`면 **필수** |

  `ParfaitImage.updateBorder`가 저장 전에 `validateBorder`를 다시 돌린다 — POST와 **같은 규칙, 같은 코드**
  (`SOLID`인데 색·두께 중 하나라도 `null`이면 400 `INVALID_BORDER`). `NONE`으로 바꾸면서 색·두께를 보내면
  검증 없이 그대로 저장된다.

  ⚠️ **검증 애노테이션도 `@Valid`도 없다** — 요청 DTO에 Bean Validation이 하나도 없고 컨트롤러가 `@Valid`를
  붙이지 않는다. POST와 같은 결과다: OpenAPI 스키마 `required`가 비고, `borderWidth` 음수·과대값을 막는 것이
  서버에 없다([conventions.md](conventions.md) `required` 절).

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `parfaitImageId` | Long | 아니오 | |
| `borderType` | String | 아니오 | 저장된 enum 이름 문자열(`BorderType.name`) |
| `borderColor` | String? | 예 | 저장된 값 |
| `borderWidth` | Double? | 예 | 저장된 값 |

  **이 도메인에서 테두리를 되돌려주는 유일한 응답이다** — POST·위치 PATCH 둘 다 테두리 필드가 없다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 바디 형식 오류 · `borderType` enum 밖 값·누락(`CommonErrorCode`) |
| 400 | `INVALID_BORDER` | `SOLID`인데 색·두께 없음(`ParfaitImageErrorCode`) |
| 404 | `PARFAIT_IMAGE_NOT_FOUND` | `parfaitImageId` 부재이거나 그 배치의 `parfaitId`가 경로와 다름 |
| 403 | `PARFAIT_IMAGE_NOT_OWNED` | 본인이 배치한 토핑이 아님 · **그룹 미참여도 같은 코드** |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitImageErrorCode`, **2026-08-20 신설**) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(**`ParfaitErrorCode`**, 2026-08-20 신설) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  검사 순서와 코드 선택이 위치 PATCH와 문자 그대로 같다(`UpdateParfaitImageBorderService`가
  `UpdateParfaitImageService`와 같은 네 검사를 같은 순서로 한다) — 그룹 미참여와 "남의 토핑"이 한 코드로
  뭉개지는 것도, 마감 검사가 소유권 뒤인 것도 동일하다. 근거: `UpdateParfaitImageBorderControllerTest`와
  신설 둘을 덮는 `UpdateParfaitImageBorderServiceTest`.

### DELETE /api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}

`feat: 토핑 삭제 API 구현`(PR #88)으로 신설됐다.

- **인증**: 필요.
- **성공**: HTTP **200** · envelope `code` = `"OK"` · `data` = `null`.
  ⚠️ **204가 아니다.** `@ResponseStatus`가 없고 컨트롤러가 `ApiResponse.ok(null)`을 반환한다 — 같은 delta에
  들어온 회원 탈퇴(`DELETE /api/v1/users/me`)는 **204에 본문 자체가 없다**([member.md](member.md)).
  **같은 서버의 두 DELETE가 성공 표현을 달리한다.**
- **요청 필드**: 없음(경로 변수 3개뿐)
- **응답 필드**: 없음(`data`가 `null`)

  **부수효과가 셋이다** — 이 API의 본체다.
  ① 배치 행 삭제(`ParfaitImageDeletePort.deleteById`).
  ② `image_meta.reference_count` 1 감소(`ImageMeta.decrementReferenceCount`, 하한 0).
  ③ **감소 결과가 0이면 S3 객체를 지운다**(`ImageDeletePort.delete(url)` → `ImageDeleteAdapter`가
  `imageUrl`에서 버킷·리전 접두사를 잘라 키를 얻고 `DeleteObjectRequest`를 보낸다).
  즉 **같은 이미지를 다른 파르페가 참조 중이면 S3 원본은 남는다.**

  ⚠️ **`image_meta` 행 자체는 남는다** — 카운트가 0이어도 메타 행은 삭제되지 않고 `COMPLETED` 상태로
  존속한다. S3 객체만 사라지므로 그 `imageUrl`은 **404를 뱉는 주소**가 되고, 그 `imageId`로 다시 배치하면
  깨진 이미지가 걸린다(배치 시 검사하는 것은 상태가 `COMPLETED`인지뿐이다) → [미결](#미결).

  ⚠️ **S3 삭제가 트랜잭션 안에서 일어난다.** `@Transactional` 메서드 본문에서 외부 호출을 하므로,
  삭제 성공 후 커밋이 실패하면 **DB에는 배치가 남고 S3 객체만 없어진다.** 회원 탈퇴가 같은 문제를
  `afterCommit` 동기화로 피한 것과 다르다([member.md](member.md)).

  `image_meta`가 없으면 `requireNotNull`이 터져 500이다 — FK로 항상 존재한다는 전제를 코드가 주석으로 밝힌다.

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_REQUEST` | 경로 변수가 Long이 아님(`CommonErrorCode`) |
| 404 | `PARFAIT_IMAGE_NOT_FOUND` | `parfaitImageId` 부재이거나 그 배치의 `parfaitId`가 경로와 다름 |
| 403 | `PARFAIT_IMAGE_NOT_OWNED` | 본인이 배치한 토핑이 아님 · **그룹 미참여도 같은 코드** |
| 404 | `PARFAIT_NOT_FOUND` | `parfaitId`가 그 그룹의 파르페가 아님(`ParfaitImageErrorCode`, **2026-08-20 신설**) |
| 409 | `PARFAIT_ALREADY_CLOSED` | 파르페 `status`가 `ACTIVE`가 아님(**`ParfaitErrorCode`**, 2026-08-20 신설) |
| 401 | `UNAUTHORIZED` 외 | 전역 인증(`AuthErrorCode`) |

  근거: `DeleteParfaitImageControllerTest`가 성공(200·`data` 널)·404·403 세 케이스를 검증하고, 신설 둘은
  `DeleteParfaitImageServiceTest`가 잠근다. **삭제는 멱등이 아니다** — 같은 `parfaitImageId`를 두 번
  지우면 두 번째는 404다. 검사 순서는 위치·테두리 PATCH와 같아 **마감 검사가 삭제 실행보다 앞**이므로
  마감된 캔버스에서는 부수효과(참조 카운트 감소·S3 삭제)가 시작되지 않는다.

## 도메인 에러 코드 전수

`ParfaitImageErrorCode`(`core/parfaitimage/exception`) 5종 전부. 귀속이 확인되지 않은 코드는 없다.

| HTTP | code | message | 귀속 |
|---|---|---|---|
| 400 | `INVALID_BORDER` | SOLID 테두리는 색상과 두께가 필요합니다 | POST · 테두리 PATCH |
| 404 | `PARFAIT_NOT_FOUND` | 존재하지 않는 파르페입니다 | **다섯 엔드포인트 전부**(2026-08-20에 POST 단독에서 넓어졌고, 2026-08-31 신설된 일괄 PATCH도 같다) |
| 404 | `PARFAIT_IMAGE_NOT_FOUND` | 존재하지 않는 배치입니다 | 위치 PATCH · **일괄 PATCH** · 테두리 PATCH · DELETE |
| 403 | `PARFAIT_IMAGE_NOT_OWNED` | 본인이 배치한 토핑이 아닙니다 | 위치 PATCH · **일괄 PATCH** · 테두리 PATCH · DELETE |
| 409 | `IMAGE_NOT_CONFIRMED` | 업로드가 확인되지 않은 이미지입니다 | POST |

이 도메인은 자기 enum 밖의 코드도 던진다 — `ImageErrorCode.IMAGE_NOT_FOUND`(404),
`ParfaitGroupApiErrorCode.GROUP_NOT_JOINED`(403), 그리고 **2026-08-20부터
`ParfaitErrorCode.PARFAIT_ALREADY_CLOSED`(409)가 다섯 엔드포인트 전부에서** 나간다
([parfait.md](parfait.md) "도메인 에러 코드 전수"). 소비 측은 이 도메인 enum만 보고 분기하면 안 된다.
`GROUP_NOT_JOINED`를 내는 것은 **POST 하나뿐**이다 — 나머지 넷은 그룹 미참여도
`PARFAIT_IMAGE_NOT_OWNED`로 접는다.

⚠️ **소유권의 기준은 회원 id가 아니라 멤버십 행 id(`groupMemberId`)다.** 네 엔드포인트가
`findByGroupIdAndMemberId`(= `leftAt IS NULL`)로 찾은 멤버십의 id를 `parfait_image.placed_by_group_member_id`와
비교한다. **2026-09-10 서버 delta로 이 기준에 되돌림이 생겼다** — 재참여가 새 행을 만들지 않고 기존 행을
`rejoin`으로 재활성화하므로([parfait-group.md](parfait-group.md)) **탈퇴했던 회원이 같은 그룹에 다시 들어오면
탈퇴 전에 올린 토핑의 소유권을 그대로 되찾는다.** 마감 검사가 소유권 뒤라 실제로 고칠 수 있는 것은
**아직 `ACTIVE`인 캔버스의 토핑**(같은 날 나갔다 들어온 경우)뿐이다. 근거: `DeleteParfaitImageService`·
`UpdateParfaitImageService`의 `groupMember.id != parfaitImage.placedByGroupMemberId` 분기.

⚠️ **마감 거부의 자리가 엔드포인트마다 다르다.** 넷(POST · 위치 PATCH · 테두리 PATCH · DELETE)은
마감 검사가 권한 검사 **뒤**라, 마감된 캔버스라도 남의 토핑이면 `PARFAIT_IMAGE_NOT_OWNED`, 그룹 멤버가
아니면(배치) `GROUP_NOT_JOINED`가 **먼저** 온다. **2026-08-31 신설된 일괄 PATCH만 반대다** — 그룹 소속과
마감을 요청당 한 번씩 앞에서 보고 항목별 소유권을 뒤에 보므로 **같은 상황에 409가 먼저** 온다.
"마감된 캔버스면 409"로 읽고 분기하면 앞의 넷에서 빠지고, "남의 토핑이면 403"으로 읽으면 일괄에서
빠진다 — 각 엔드포인트 절의 검사 순서를 볼 것.

⚠️ **같은 문자열 `PARFAIT_NOT_FOUND`를 두 enum이 갖는다** — 이 도메인 것과
`ParfaitErrorCode` 것이고, HTTP status(404)와 message가 같아 **와이어에서는 구분되지 않는다.**
한 요청 안에서도 갈린다: 토핑 네 엔드포인트는 이 도메인 값을, 캔버스 상세 조회·배경 변경은
`ParfaitErrorCode` 값을 던진다. 소비 측이 `code` 문자열로 분기하는 한 차이가 없어 실질 영향은 없지만,
[conventions.md](conventions.md) "코드 문자열은 enum 간 유일하지 않다"의 사례가 하나 늘었다.

## Android 매핑

**계약이 다섯이 되면서 표면이 갈렸고, 갈린 자리가 한 번 옮겨 앉았다** — 지금 표면·소비처를 갖춘 것은
배치(POST, 2026-08-21 PR5) · 삭제(DELETE, 2026-08-23 PR #335) · **일괄 PATCH**(2026-09-01 PR #428) ·
**테두리 PATCH**(2026-08-27 PR #369) 넷이고, **위치/크기/각도 단건 PATCH가 표면 0건**이다. 단건은
2026-08-23 PR #336으로 표면·소비처를 얻었다가 그 소비처가 일괄로 옮겨 타면서 함께 걷혔다. 마지막 하나는 `ToppingRepository.updateBorder` →
`UpdateToppingBorderUseCase`를 거쳐 C-301 편집 탭의 확인 버튼에 걸렸다(표면은 2026-08-12 PR #230 두 건
+ **2026-08-15 PR #250 두 건**).

⚠️ **앱의 겹 목록을 서버의 한 겹으로 접는 자리는 `CanvasBGEditViewModel.toToppingBorder`다** —
`borderLayers`의 **마지막 겹**만 `ToppingBorder.Solid`로 보내고, 비면 `None`을 보낸다. 되읽는 방향
(`toBorderLayers`)은 반대로 한 겹짜리 목록으로 편다. 같은 화면이 **그릴 때는 첫 겹**을 쓰고 있어
겹이 둘 이상이면 보이는 테두리와 저장되는 테두리가 갈린다
→ [open-questions](../synthesis/open-questions.md) OQ-P-324.

⚠️ **변형과 테두리가 확인 버튼 안에서 독립적으로 판정되고 독립적으로 나간다** —
`updateDirtyToppings`가 dirty 토핑을 `hasTransformChange`·`hasBorderChange` 둘로 갈라, 변형이 바뀐
토핑은 `saveTransforms`가 **일괄 PATCH 1회**로 한꺼번에 보내고 테두리가 바뀐 토핑은 `saveBorder`가
토핑마다 보낸다. 테두리만 고친 토핑에는 테두리 PATCH 하나만 나간다. 계약이 두 엔드포인트로 갈라져
있는 것을 앱이 축 단위로 미러링한 결과다. **응답 `UpdatedToppingBorderVO`는 여전히 아무도 읽지 않는다**(실패만 로그로 접는다).

⚠️ **읽는 방향에 앱 클램프가 하나 생겼다**(2026-09-08, PR #464) — 응답을 도메인으로 옮기는 두
`VOMapper`(`data/source/parfait/`·`data/source/parfaitimage/`)가 `ToppingBorder.Solid` 를 직접 만들지
않고 `ToppingBorder.solidClamped` 를 지나, 서버가 준 `borderWidth` 를 `WIDTH_RANGE_DP`(2.0..30.0)로
가둔다. **서버가 범위를 검증하지 않는 자리를 앱이 임시로 메운 것**이라, 서버나 정책이 범위를 정하면
걷을 코드다(위 「미결」·[open-questions](../synthesis/open-questions.md) OQ-P-381). 두
`RemoteDataSource` 테스트가 범위 밖 값이 잘리는 것을 각각 한 건씩 잠근다.
📌 **2026-09-11(PR #496)부터 두 매퍼가 사본 대신 공용 함수를 부른다** —
`data/source/common/mapper/ToppingBorderMapper.kt`의 `toToppingBorder`이고, 그룹 목록 매퍼
(`data/source/group/`)가 같은 함수의 세 번째 호출부다([parfait-group.md](parfait-group.md) 「미결」).

| 계약 | Android 심볼 |
|---|---|
| `POST .../parfaits/{parfaitId}/images` | `ParfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages` → `ParfaitImageRemoteDataSource.placeTopping(groupId, parfaitId, imageId, transform, border)` |
| `PATCH .../images/{parfaitImageId}` | **없음**(2026-08-31 걷어냄 — 소비처가 없어졌다) |
| `PATCH .../parfaits/{parfaitId}/images`(일괄) | `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImages` → `ParfaitImageRemoteDataSource.updateToppings(groupId, parfaitId, updates)` |
| `PATCH .../images/{parfaitImageId}/border` | `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageIdBorder` → `ParfaitImageRemoteDataSource.updateToppingBorder(groupId, parfaitId, parfaitImageId, border)` |
| `DELETE .../images/{parfaitImageId}` | `ParfaitImageService.deleteGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId` → `ParfaitImageRemoteDataSource.deleteTopping(groupId, parfaitId, parfaitImageId)` |

**테두리 수정은 nullable 파라미터가 아니라 `ToppingBorder` 하나를 받는다.** 서버가 세 필드를 통째로
덮기 때문이다(위치 수정의 부분 병합과 다르다). sealed를 그대로 받으므로 `SOLID`인데 색·두께가 빠지는
조합을 만들 수 없고, **400 `INVALID_BORDER`는 앱에서 도달 불가**다. 응답 쪽에는 반대 방향 폴백이 있다 —
`SOLID`인데 색·두께가 비면 `ToppingBorder.None`으로 떨어뜨린다(이미 저장된 행 대비).

**두 DELETE가 `ApiCaller` 진입점을 달리 쓴다.** 토핑 삭제는 200 + `data: null`이라
`safeApiCallWithoutData`, 회원 탈퇴는 204·본문 없음이라 `safeApiCallNoContent`다
([member.md](member.md)). 死코드로 지적돼 있던 `safeApiCallWithoutData`가 **첫 프로덕션 소비처**를
얻었다. 설계 근거는
[specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer](../superpowers/specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md).

**이름이 계층마다 갈린다.** `data`는 서버 언어(`ParfaitImageService`·`PlaceParfaitImageRequest`),
`domain`은 제품 언어(`PlacedToppingVO`·`ToppingTransform`·`ToppingBorder`·`UpdatedToppingVO`) —
제품 어디에도 "parfait image"라는 말이 없고 위키·기획은 전부 "토핑"이다. `source/parfaitimage/mapper/VOMapper.kt`가
그 번역 지점이다. 설계 근거는
[specs/archive/2026-08-11-member-parfait-image-api-service-layer](../superpowers/specs/archive/2026-08-11-member-parfait-image-api-service-layer.md).

계약의 얽힌 제약 하나를 타입이 강제한다 — **`borderType = SOLID`면 색·두께가 필수**(아니면 400
`INVALID_BORDER`)라는 규칙을 `sealed interface ToppingBorder { None | Solid(color, width) }`로 모델링해
그 실패를 표현 불가능한 상태로 만들었다. 매퍼가 평면 3필드(`borderType`·`borderColor`·`borderWidth`)로 편다.
`ToppingTransform`도 `Double` 넷이 연속이라 순서 뒤바뀜을 막으려고 묶었다. **wire DTO는 계약 문서와 눈으로
대조돼야 해서 평면을 유지**한다.

**POST와 PATCH의 비대칭은 서버 계약의 비대칭이다** — POST는 `ToppingTransform` 통째를 받고 PATCH는
nullable 5파라미터다. PATCH 요청 DTO의 5필드가 전부 `= null` 기본값인데 `@RemoteJson`이
`encodeDefaults = true`라 **안 바꾸는 필드도 `"positionX": null`로 실려 나간다.** 서버 `ParfaitImage.update`가
`?:` 병합이라 키 부재와 동치이므로 동작은 정확하다.

✅ **2026-08-19 서버 delta로 어긋났던 두 자리는 2026-08-20에 맞춰졌다**(PR #310 develop 머지).
① POST 응답 `placedBy.nameTagChip`을 앱 DTO가 **거울로 받되 VO(`PlacedToppingVO`)에는 안 올렸다** —
읽는 화면이 0건인 상태로 도메인 모양을 굳히지 않는 규약이고, 안 읽는 것뿐이라 `⚠️불일치`도 아니다
(`ignoreUnknownKeys = true`). ② 중첩 클래스를 서버 개명에 맞춰 `PlaceParfaitImagePlacedByResponse`로
바꿨다 — **거울 규약을 복원한 것**이고, 이름이 길어진 대가로 같은 이름 두 개를 두 패키지에 두던
모양이 사라졌다(캔버스 응답 쪽은 `PlacedByResponse` 그대로). 두 DTO를 "통일해야 하나" 오해하지
않도록 양쪽 KDoc에 서로를 가리키는 한 줄이 붙어 있다
→ [open-questions](../synthesis/open-questions.md) [2026-08-19].

**POST·위치 PATCH 응답 VO에는 여전히 테두리 필드가 없다** — 두 응답이 테두리를 돌려주지 않아서다.
대신 **되읽을 두 자리가 실제로 생겼다**: 테두리 PATCH 응답(`UpdatedToppingBorderVO` —
`domain/model/topping/`, 앱이 테두리를 되받는 첫 자리)과 `parfaits/today`의 `images[]`
(`CanvasToppingVO`). `CanvasToppingVO`를 `PlacedToppingVO`와 합치지 않은 것도 같은 이유다 —
POST 응답에 없는 값을 지어내거나 nullable로 "모른다"와 "없다"를 뭉개게 된다.

✅ **늘어난 실패 경로를 앱이 같은 날 받았다**(2026-08-20, PR #318 develop 머지). 네 엔드포인트가 모두
409 `PARFAIT_ALREADY_CLOSED`를 낼 수 있게 되자 앱이 **`ServerErrorCode.Parfait`** 를 신설해 그 코드를
들었다. 소비처가 0건인데 상수를 먼저 둔 것은 "쓰지 않는 상수는 계약이 바뀌어도 아무도 고치지 않아
거짓말이 된다"는 그 파일의 규약에 대한 **명시적 예외**였고, 근거는 **처분이 이미 정해졌다**는 것이다
(→ [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md): 이 코드는 다른 넷과
함께 토스트 후 화면에 남는다). 상수 KDoc이 결정과 함정을 함께 적어 소비처가 붙을 때
같은 판단을 다시 하지 않게 한다 — 특히 **네 경로 전부 소유권 검사가 마감 검사보다 앞이라** 남의
토핑을 마감된 캔버스에서 고치려 하면 409가 아니라 403 `PARFAIT_IMAGE_NOT_OWNED`가 먼저 온다.
같은 PR이 `CanvasStatus` KDoc의 "서버가 그것을 강제하지 않는다"도 뒤집었다
→ [parfait.md](parfait.md) Android 매핑 · [open-questions](../synthesis/open-questions.md) [2026-08-20].

✅ **위 예외 사유가 소멸했다**(2026-08-21 브랜치 작업 → 2026-08-22 develop 머지, PR #334) — `CanvasToppingPlaceViewModel`이 영구 실패 판정에
쓰는 코드에 소비처가 생겼다. 처음 셋(`PARFAIT_ALREADY_CLOSED`·`GROUP_NOT_JOINED`·`PARFAIT_NOT_FOUND`)에
최종 브랜치 리뷰가 400 둘(`Common.INVALID_REQUEST`·`ParfaitImage.INVALID_BORDER`)을 더해 **다섯**이
됐다 — 재시도가 발급부터 4단계를 다시 태워도 결과는 항상 같은 400이라, 확인을 누를 때마다
참조되지 않는 이미지만 쌓여서다. 다섯은 **서로 다른 object에 흩어져 있다**
(`ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED`·`ServerErrorCode.ParfaitGroup.GROUP_NOT_JOINED`·
`ServerErrorCode.ParfaitImage.{PARFAIT_NOT_FOUND, INVALID_BORDER}`·`ServerErrorCode.Common.INVALID_REQUEST`)
— 위에서 경고한 "같은 문자열을 두 enum이 갖는다"와 같은 이유로 코드 쪽도 도메인 경계를 지켜 나눠
둔다. PR5가 `PARFAIT_NOT_FOUND`를 담을 **`object ParfaitImage`를 새로 만들었다**(그전엔 없었다).
"쓰지 않는 상수를 먼저 둔 명시적 예외"는 이제 필요 없다 — 상수 KDoc의 결정·함정 서술은 남지만,
더는 예외가 아니라 보통의 소비되는 상수다. 다만 `PARFAIT_ALREADY_CLOSED` KDoc의 "화면 이동은
그대로 진행하고"는 낡았다 — 최종 리뷰가 되감기 자체를 걷어 이제 알린 뒤 화면에 남는다(아래 인접
절·[스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) 참고).

**화면 소비처는 배치(POST) 하나뿐이다.** 나머지 셋(위치 PATCH · 테두리 PATCH · DELETE)은 여전히
화면 로컬 상태로만 동작한다(소비 화면은 C-301 라운드). 다만 **"다시 그릴 수 없다"는 사유도, 앱 표면
공백도 사라졌다** — `GET .../parfaits/today`가 배치 전량을 내려주고([parfait.md](parfait.md)) 네
엔드포인트 전부 DataSource까지 와 있다.
> 🔁 **삭제가 그 셋에서 빠졌다**(2026-08-23, PR #335) — 아래 항목 참고.
> 🔁 **위치 PATCH도 빠졌다**(2026-08-23, PR #336) — 남은 것은 테두리 PATCH 하나다.

✅ **배치 하나는 Repository·UseCase까지 올라왔다**(2026-08-20 develop 머지, PR #322) —
`ToppingRepository.place`가 DataSource의 넷 중 배치만 열고, `AddToppingUseCase`가 업로드
([image.md](image.md))와 배치를 **이 순서로** 조율한다. 나머지 셋(위치·테두리 수정·삭제)을 안 올린
것은 소비 화면이 C-301 라운드라서다 — 쓰지 않는 갈래를 미리 열면 계약이 바뀌어도 아무도 고치지
않는다. Repository 층은 **에러 변환만 하고 좌표·테두리를 손대지 않는다**(테두리를 흘리면 서버는
200을 주고 캔버스에서 테두리만 조용히 사라진다).

✅ **화면 결선이 끝났다**(2026-08-22 develop 머지, PR #334) —
`CanvasToppingPlaceViewModel`이 `AddToppingUseCase`를 불러 좌표 변환·업로드·배치를 조율하고, 로딩
오버레이·실패 토스트·영구 실패 판정(다섯 코드)까지 붙었다. 영구 실패도 **되감지 않고 알린 뒤
화면에 남는다**(최종 브랜치 리뷰로 뒤집힌 결정, 성공은 여전히 되감는다). **배치(POST)의 화면
소비처는 이것 하나다.** `android_status`는 `partial` 그대로다 — 나머지 셋(위치·테두리·삭제)의
소비 화면이 C-301 라운드다. 실기기 확인은 아직 없다
→ [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) ·
[open-questions](../synthesis/open-questions.md).

✅ **삭제가 화면까지 이어졌다**(2026-08-23 develop 머지, PR #335) — `ToppingRepository.delete` ·
`DeleteToppingUseCase`가 신설되고 C-301 편집 탭의 삭제 확인 모달이 그것을 부른다. **앱이 서버의
데이터를 지우는 첫 경로**이고, `safeApiCallWithoutData`(200 + `data: null`)가 이 라운드에 화면 쪽
소비자까지 갖게 됐다. 성공해야 화면 목록에서 뺀다.
✅ **정정 — 실패는 토스트로 닿는다.** `failToDeleteTopping`이 `CanvasBGEditError.TOPPING_DELETE_UNKNOWN`
토스트를 내고 로딩만 내린다 — 이 절 초판이 "실패가 화면에 닿지 않는다"고 적은 것은 틀렸다.
**dirty 집합과는 무관하다** — 삭제는 dirty 축을 안 쓴다(그 축이 붙잡는 것은 이동·크기·각도·테두리뿐이다),
그래서 위치 PATCH가 실패 id를 `dirtyToppingIds`에 남겨 재시도하는 것과는 처분이 다르다
→ [open-questions](../synthesis/open-questions.md) OQ-P-270.
`android_status`는 여전히 `partial`이다 — 위치·테두리 PATCH의 소비 화면이 없다.

✅ **위치 PATCH도 화면까지 이어졌다**(2026-08-23 develop 머지, PR #336) — `ToppingRepository.update` ·
`UpdateToppingUseCase`가 신설되고 C-301 편집 탭의 **확인 버튼**이 그것을 부른다. 소비되지 않은
엔드포인트는 이제 **테두리 PATCH 하나**다. 설계에서 계약과 맞물리는 자리는 셋이다.

- **바뀐 토핑만 보낸다.** ViewModel이 조회 응답 스냅샷(`serverToppings`)을 따로 들고 확인 시점에
  대조해, 위치·배율·각도 중 하나라도 달라진 토핑만 요청한다. 안 건드린 토핑은 요청이 0건이다.
- **`positionZ`를 안 보낸다.** 이 PATCH가 부분 병합(`null`이면 유지)이라 겹침 순서는 서버 값이
  그대로 남는다. 앱에는 z 조작 경로 자체가 없다.
- **토핑들끼리는 병렬, 배경보다는 앞.** `async` + `awaitAll`로 동시에 나가고 전부 끝난 뒤에야 배경
  변경([parfait.md](parfait.md))이 이어진다. 둘을 얽으면 한쪽만 실패한 경우를 갈라 다뤄야 해서다.

✅ **정정 — 실패는 화면에 닿는다.** `CanvasBGEditViewModel.handleOnClickConfirm`이 실패한 토핑
id를 `dirtyToppingIds`에 남겨 다음 확인이 그것만 재시도하고, `CanvasBGEditError.TOPPING_SAVE_UNKNOWN`
토스트를 내며 화면을 닫지 않는다 — 이 절 초판이 "실패가 화면에 닿지 않고 확인은 그대로 성공한다"고
적은 것은 틀렸다 → [open-questions](../synthesis/open-questions.md) OQ-P-275.
✅ **정정 — 둘 다 토스트를 낸다, 다만 완전히 같지는 않다.** 배경 실패는 `failToSave`가
`toCanvasBGEditError`로 원인별 코드(`NETWORK`·`UNSUPPORTED_IMAGE`·`BACKGROUND_SAVE_UNKNOWN`)를
가른다. 토핑 변형 실패는 원인을 안 가리고 항상 `TOPPING_SAVE_UNKNOWN` 하나로 접힌다. **같은
확인에서 배경과 토핑이 함께 실패하면 배경 쪽 토스트만 뜬다** — `handleOnClickConfirm`의 `when`이
`savedBackground == null`을 토핑 실패 분기(`failedToppingIds.isNotEmpty()`)보다 먼저 매칭해서다.
다만 `dirtyToppingIds`는 그 분기 이전에 이미 갱신돼 있어, 토스트만 안 뜰 뿐 다음 확인의 재시도
대상에서는 안 빠진다 → [open-questions](../synthesis/open-questions.md) OQ-P-261.
⚠️ **범위 검증 없는 두 축이 그대로 요청 값이 된다** — 아래 [미결](#미결)의 `scale`·`rotation`
서버 검증 부재가 이 라운드부터 실제로 닿는다. 앱 쪽 상한도 없다(OQ-P-271).
`android_status`는 여전히 `partial`이다 — 테두리 PATCH의 소비 화면이 없다.

✅ **일괄로 옮겨 탔다**(2026-08-31 브랜치 작업 → **2026-09-01 develop 머지, PR #428 `e870fb87`**) — 확인 버튼이 변형(위치·배율·각도)을 일괄 PATCH 한 번으로 접는다. 위 PR #336 항목이
적은 `async` + `awaitAll` 단건 병렬 호출은 사라졌고, `ToppingRepository.update`·`UpdateToppingUseCase`도
함께 걷혔다(각각 `updateAll`·`UpdateToppingsUseCase`로 교체). **부분 성공이 사라진 대가는 그대로
받았다** — 서버가 항목 하나만 걸려도 전부 롤백하고 응답이 어느 항목인지 안 알려주므로, 실패하면
**변형을 보낸 토핑 전부**가 `dirtyToppingIds`에 남아 다음 확인에서 다시 나간다(재시도 입도가 토핑
단위에서 요청 단위로 거칠어졌다). **테두리는 이 일괄 계약에 필드가 없어 여전히 토핑마다 나간다** —
확인 한 번이 변형 일괄 1회 + 테두리 병렬 N회로 갈린다. `positionZ`는 여전히 안 보낸다
→ [open-questions](../synthesis/open-questions.md) OQ-P-334.

`http/parfait-image.http`가 **다섯 중 넷**을 덮는다(2026-08-15 시점 전량, 2026-08-31 delta로 다시 벌어졌다) —
일괄 PATCH 요청이 없다. **선행이 넷**이 됐다 —
`auth.http` → `parfait-group.http` → `images.http`(발급·PUT·confirm) → `parfait.http`(오늘의 캔버스
조회가 `parfait_id`를 채운다). `parfaitId` 리터럴을 손으로 바꾸던 단계는 사라졌다.

## 미결

- 좌표·`scale`·`rotation`·`borderWidth`에 서버 검증이 없다 — 범위를 서버가 강제할지, 앱 책임으로 둘지
  → [open-questions](../synthesis/open-questions.md)
  > ⚠️ **`borderWidth` 는 Android 가 매핑에서 임시로 가둔다**(2026-09-08, PR #464 develop
  > 머지) — `ToppingBorder.solidClamped`
  > 가 받은 값을 `WIDTH_RANGE_DP`(2.0..30.0)에 넣는다. 상한을 내리기 전에 50 으로 저장된 행이
  > 이미 있어 슬라이더만 좁히면 그 행이 계속 굵게 그려지기 때문이고, **서버나 정책이 범위를
  > 정하면 걷을 자리다** → 같은 문서 OQ-P-381.
  > ⚠️ **앱 쪽 상한도 하나 사라졌다**(2026-08-23, PR #335) — C-301 편집 탭의 `TOPPING_MAX_SCALE`이
  > 삭제돼 배율을 막는 자리가 양쪽 어디에도 없다 → 같은 문서 OQ-P-271.
  > ⚠️ **그 값이 하루 만에 요청 값이 됐다**(2026-08-23, PR #336) — 확인 버튼이 `scale`·`rotation`을
  > 그대로 위치 PATCH에 싣는다. 무한히 커진 배율과 누적된 각도가 검증 없이 저장된다.
- 같은 `imageId` 재-POST가 남의 배치를 옮기고 소유자를 가져간다(POST에 소유자 검사 없음)
  → [open-questions](../synthesis/open-questions.md)
- 삭제가 S3 객체를 지우면서 `image_meta` 행은 `COMPLETED`로 남긴다 — 그 `imageId`로 다시 배치하면 깨진
  이미지가 걸린다 → [open-questions](../synthesis/open-questions.md)
- 삭제의 S3 호출이 트랜잭션 안에 있어 커밋 실패 시 DB와 S3가 갈린다
  → [open-questions](../synthesis/open-questions.md)
- 일괄 PATCH에 `items` 개수 상한이 없고, 실패해도 **어느 항목이 걸렸는지 응답에 없다**(코드만 오고
  `parfaitImageId`는 안 온다) → [open-questions](../synthesis/open-questions.md) OQ-P-334
- 같은 수정을 단건과 일괄이 **다른 검사 순서**로 처리해 마감된 캔버스의 남의 토핑에 서로 다른 코드를
  낸다(403 vs 409) → [open-questions](../synthesis/open-questions.md) OQ-P-334

✅ **2026-08-15 해소** — ① 배치 **삭제** 엔드포인트 신설, ② 배치 후 **테두리 변경 경로** 신설,
③ 배치 **목록 조회** 부재는 [parfait.md](parfait.md) `GET .../parfaits/today`가 대신 닫았다.

✅ **2026-08-20 해소** — "네 엔드포인트 어디도 `parfait.status`를 보지 않아 마감된 캔버스도 편집된다"가
서버 가드로 닫혔다(409 `PARFAIT_ALREADY_CLOSED`). **서버가 막을지 앱 책임으로 둘지**라는 물음에
서버가 답했고, "앱이 그 코드를 어떻게 보여줄지"도 같은 날 정해졌다(토스트 후 캔버스로 되감기, 상수
신설까지 PR #318). **이 처분은 2026-08-21 PR5 최종 리뷰로 뒤집혔다** — 되감기가 안내(토스트)를
같은 프레임에 폐기하는 것이 드러나 알린 뒤 화면에 남는 것으로 바뀌었다(위 [Android
매핑](#android-매핑) 절 참고) → [Android 매핑](#android-매핑) · [open-questions](../synthesis/open-questions.md).
