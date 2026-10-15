---
id: canvas-detail-background-api-service-layer
title: ":data 캔버스 상세 조회·배경 변경 API Service·remote DataSource 레이어 (2 엔드포인트)"
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-16
related_code: ParfaitService, ParfaitRemoteDataSource, ParfaitRemoteDataSourceImpl, ChangeParfaitBackgroundRequest, ChangeParfaitBackgroundResponse, CanvasBackgroundEdit, CanvasBackground, CanvasVO, ApiCaller
related_adr: ADR-0017
related_spec: 2026-08-15-parfait-canvas-topping-member-api-service-layer, 2026-08-15-c301-canvas-background-edit, 2026-08-06-unit-test-infrastructure
related_architecture: data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, data, network, api, canvas, background]
---

# :data 캔버스 상세 조회·배경 변경 API Service·remote DataSource 레이어

> **사후 스펙(as-built)·머지(PR #266, 2026-08-16)** — 브랜치 `feature/#265-sync-backend-api-260816`,
> 단일 커밋 `277f9f24`. 선작성 스펙 없이 머지된 라운드라 이 문서는 develop 코드를 읽어 쓴 것이다.
> 앞선 API 표면 라운드(PR #230·#250)에는 스펙·플랜 한 쌍이 있었으므로 **표면 라운드가 스펙 없이 간 첫
> 사례**다. 다만 형태는 선례를 그대로 따랐다(Service 함수 → wire DTO → 매퍼 → DataSource, DI 무증가).

2026-08-16 서버 delta(`22717fe`)가 파르페 도메인에 엔드포인트 둘을 더했고, 이 라운드가 그 둘의
`:data` 표면을 붙였다 — **Repository·UseCase·화면은 범위 밖**이다.

| 엔드포인트 | 성격 |
|---|---|
| `GET /api/v1/groups/{groupId}/parfaits/{parfaitId}` | 캔버스 상세. 응답이 오늘 조회와 **같은 클래스** |
| `PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/background` | 배경 변경. **이 도메인 첫 쓰기 경로** |

계약 정본은 [api/parfait.md](../../../api/parfait.md).

## 무엇을 만들었나

| 파일 | 역할 |
|---|---|
| `data/service/ParfaitService.kt` | `getGroupsByGroupIdParfaitsByParfaitId` · `patchGroupsByGroupIdParfaitsByParfaitIdBackground` 2함수 추가 |
| `data/service/model/request/parfait/ChangeParfaitBackgroundRequest.kt` | **이 도메인 첫 요청 DTO**(`type`·`value`·`imageId` 평면) |
| `data/service/model/response/parfait/ChangeParfaitBackgroundResponse.kt` | `background: BackgroundResponse`(조회 응답의 중첩 클래스 재사용) |
| `data/source/parfait/mapper/VOMapper.kt` | `CanvasBackgroundEdit.toRequest()` · `ChangeParfaitBackgroundResponse.toCanvasBackground()` 추가, `toTodayCanvasVO` → `toCanvasVO` 개명 |
| `data/source/parfait/remote/ParfaitRemoteDataSource(+Impl).kt` | `getCanvasDetail` · `changeCanvasBackground` 2함수 추가 |
| `domain/model/canvas/CanvasBackgroundEdit.kt` | **쓰기 전용** sealed(`Color(hex)` · `Image(imageId)`) 신설 |
| `domain/model/canvas/CanvasVO.kt` | `TodayCanvasVO`에서 **개명**(파일 이름째 이동) |
| `domain/model/canvas/CanvasBackground.kt` | KDoc만 갱신(쓰기는 `CanvasBackgroundEdit`, "쓰는 API가 없다" 서술 제거) |
| `data/src/test/.../ParfaitRemoteDataSourceImplTest.kt` | 15 → **25 케이스**(+10) |

**DI 등록 줄은 한 줄도 늘지 않았다** — `ParfaitService`·`ParfaitRemoteDataSource`가 이미 바인딩돼 있고
함수만 늘었다. PR #250에 이어 **두 번째 사례**다.

## 결정

### ① 읽기 모델을 재사용하지 않고 쓰기 전용 `CanvasBackgroundEdit`를 세운다

서버 계약이 **비대칭**이다 — 이미지 배경은 **쓸 때 `imageId`, 읽을 때 URL**이다. 읽기 모델
`CanvasBackground.Image`는 URL을 들고 있어 그대로 요청으로 되돌릴 수 없다. 그래서 `:domain`에 짝이지만
같은 타입이 아닌 `CanvasBackgroundEdit`를 둔다.

동시에 이 sealed가 **조건부 필수를 컴파일 시점으로 끌어올린다**. 요청 wire 형태는 `{ type, value, imageId }`
평면이고 `type`에 따라 한쪽만 필수인데, 서버에 Bean Validation이 없어 그 제약이 OpenAPI 스키마에도
드러나지 않는다. 평면 DTO를 그대로 도메인에 노출하면 잘못된 조합을 **런타임 400 `INVALID_BACKGROUND`로만**
알게 된다. `Color(hex)` / `Image(imageId)` 둘로 가르면 그 조합 자체를 만들 수 없다.

이는 "필드 사이에 의존이 있으면 domain을 좁게 잡고 펴는 일은 매퍼가 한다"는 기존 규약
([data-layer](../../../architecture/data-layer.md))의 세 번째 적용이다(선례: `ToppingTransform.toPlaceRequest`,
`ToppingBorder.toUpdateBorderRequest`).

### ② wire DTO는 서버의 거울로 둔다

`ChangeParfaitBackgroundRequest`는 평면·널 허용 그대로다. **DTO에 sealed·value class·enum을 넣지 않는다**는
규약이 그대로 적용된다 — 계약 문서와 눈으로 대조돼야 하기 때문이다. 좁히는 쪽은 domain, 펴는 쪽은 매퍼.

### ③ `TodayCanvasVO` → `CanvasVO` 개명

상세 조회 응답이 오늘 조회와 **같은 서버 클래스**(`GetTodayParfaitResponse`)라 DTO·매퍼·VO를 그대로 쓴다.
그러면 같은 타입이 "오늘"과 "특정 날짜" 양쪽을 담게 되므로 이름에서 날짜를 뺐다. 매퍼도
`toTodayCanvasVO` → `toCanvasVO`로 따라 옮겼다.

⚠️ **부작용 경고의 소유가 바뀌었다.** "이 조회는 서버에 캔버스 행을 만든다"는 경고가 VO KDoc에 있었는데,
이제 그 성질은 **타입이 아니라 함수의 것**이다(오늘 조회만 만들고 상세 조회는 안 만든다). VO KDoc은
"오늘의 캔버스 조회만"으로 한정되고 실질 경고는 `ParfaitService`·`ParfaitRemoteDataSource`의 함수 KDoc이
진다 → [open-questions](../../../synthesis/open-questions.md).

### ④ 배경 변경 반환은 `CanvasBackground?`다

응답 `background`는 비널인데 DataSource 반환은 널 허용이다. **미지 `type`을 `null`로 접는 규칙을 조회와
통일**한 결과다(`toCanvasBackground()`를 그대로 재사용). 뜻은 "저장은 됐는데 그릴 수 없다"이다.

echo를 버리지 않는 이유는 **이미지 배경일 때 앱이 URL을 모르기 때문**이다 — 요청은 `imageId`로 보내고
응답에 저장된 URL이 실려 온다. 그 값이 화면이 그릴 값이다.

### ⑤ 마감 가드는 붙이지 않는다

서버가 캔버스 상태를 보지 않아 `CLOSED`·`EMPTY` 캔버스의 배경도 바뀐다. 이 라운드는 그 사실을
**Service·DataSource KDoc의 ⚠️로만** 남기고 코드 수단을 두지 않았다 — 표면 라운드의 범위가 아니고,
막는 것은 화면 책임이라는 판단이다(토핑 네 엔드포인트도 같은 상태다).

## 계약이 던지는 함정

1. **같은 캔버스를 두 경로로 얻을 수 있고 한쪽만 부작용이 있다.** 상세 조회는 상태로 거르지 않아 오늘의
   `ACTIVE` 캔버스도 온다. 오늘 조회는 없으면 행을 만들고, 상세 조회는 만들지 않는다. 호출 선택 규칙이
   코드에 없다.
2. **404가 두 가지를 뜻한다.** 파르페가 없는 것과 다른 그룹 소속인 것이 똑같이 `PARFAIT_NOT_FOUND`다
   (403이 아니다). 앱은 구분할 수 없다.
3. **한 엔드포인트가 세 enum의 코드를 낸다.** `ParfaitErrorCode`(`INVALID_BACKGROUND`·`PARFAIT_NOT_FOUND`·
   `BACKGROUND_IMAGE_NOT_CONFIRMED`) · `ImageErrorCode`(`IMAGE_NOT_FOUND`) ·
   `ParfaitGroupApiErrorCode`(`GROUP_NOT_JOINED`). **이미지 미확인이 image 쪽이 아니라 parfait 쪽 코드**다 —
   도메인별로 분기하면 놓친다.
4. **둘 다 채워 보내도 오류가 아니다.** 서버가 `type`에 해당하는 쪽만 쓰고 나머지를 조용히 버린다.
   매퍼가 어느 쪽도 둘을 함께 채우지 않으므로 이 경로는 앱에서 발생하지 않는다.
5. **HEX는 `#` + 6자리만 통과한다.** 3자리 축약·8자리 알파는 400이다. 앱은 이 검증을 하지 않는다 —
   팔레트가 상수라면 문제되지 않지만, C-301의 팔레트 색 일부가 코드 hex 리터럴이라는 점과 함께 본다.
6. **배경 이미지는 참조 카운트를 올리지 않는다.** 같은 이미지를 토핑으로도 올렸다가 그 토핑을 지우면
   S3 객체가 지워지고 배경이 깨진다. 앱이 막을 수단은 계약에 없다.

## 테스트

`ParfaitRemoteDataSourceImplTest` 15 → **25 케이스**. 신규 10건은 매퍼 단독 파일 없이 DataSource 테스트로
잠근다(규약 그대로, [unit-test-infrastructure](2026-08-06-unit-test-infrastructure.md) 11번).

- 상세 조회 3 — 오늘 조회와 **같은 형태로 매핑됨**(전 계층 채워짐) · `PARFAIT_NOT_FOUND` · `GROUP_NOT_JOINED`
- 배경 변경 7 — 색 편집이 `value`만 채우고 `imageId`를 비움(`coVerify`로 요청 바디 단언) · 이미지 편집이
  그 반대 · 이미지 편집이 **서버 echo URL을 돌려줌** · 미지 type echo가 `null`로 접힘 ·
  `INVALID_BACKGROUND` · `IMAGE_NOT_FOUND`(도메인 밖 코드) · `BACKGROUND_IMAGE_NOT_CONFIRMED`

`ParfaitService`는 `mockk`이고 `ApiCaller`는 실물이다(기존 파일의 방식 그대로) — 요청 바디 검증이
`coVerify`의 인자 비교로 이뤄져 **조건부 필수 두 갈래가 실제로 잠긴다**.

## 범위 밖

- **Repository·UseCase·화면.** 이 도메인은 여전히 소비처 0건이고, C-301 배경 편집은 고른 값을 계속 버린다
  → [open-questions](../../../synthesis/open-questions.md).
- **`http/parfait.http` 보강.** 두 요청이 없어 실서버 확인 수단이 스웨거뿐이다(OQ-P-108 다섯 번째 왕복이
  **표면 쪽만 닫힌 첫 사례**).
- **실서버 호출.** 이번에도 0건이다.

## 연관

- 계약: [api/parfait.md](../../../api/parfait.md) · [api/conventions.md](../../../api/conventions.md)
- 선행 표면: [2026-08-15-parfait-canvas-topping-member-api-service-layer](2026-08-15-parfait-canvas-topping-member-api-service-layer.md)
- 소비 예정 화면: [2026-08-15-c301-canvas-background-edit](2026-08-15-c301-canvas-background-edit.md) ·
  [2026-08-12-c001-canvas-main](2026-08-12-c001-canvas-main.md)
- 구조: [data-layer](../../../architecture/data-layer.md) · [ADR-0017](../../../adr/0017-remote-network-datasource.md)
