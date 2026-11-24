---
id: app-version
title: 앱 버전 체크 (강제 업데이트 판정)
server_module: http/appversion
server_commit: 40e2a33
verified: 2026-11-24
android_status: done
related_spec: force-update
related_adr:
tags: [api, parfait, server-contract, app-version]
---

# 앱 버전 체크 API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).
> ⚠️ **`main`이 아니라 미머지 브랜치 `feat/#225`(`40e2a33`) 기준이다.** 서버는 아직 배포 전이고,
> [server-baseline.md](server-baseline.md)의 기준선에도 들어 있지 않다. 머지되면 다음 점검 라운드에서 `main` 기준으로 다시 대조한다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/v1/app-versions/check?platform=&version=` | 불필요(화이트리스트) | 쿼리 `platform`·`version` | `AppVersionCheckResponse` | 구현됨(실서버 미확인) |

## 엔드포인트 상세

### GET /api/v1/app-versions/check

- **인증**: 서버 `SecurityConfig` 화이트리스트에 있어 토큰이 필요 없다.
- **성공**: HTTP 200 · envelope `code` = `"OK"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|
| `platform` | String | 예 | `IOS` · `ANDROID`. 앱은 `ANDROID`를 보낸다 |
| `version` | String | 예 | `^\d+\.\d+\.\d+$`. 선행 0은 허용(`01.4.0` = 1.4.0). 접미사(`-debug` 등)는 400 |

- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `updateType` | String | 아니오 | `FORCE` · `OPTIONAL` · `NONE` |
| `minimumVersion` | String | 아니오 | |
| `latestVersion` | String | 아니오 | |
| `storeUrl` | String | **예** | DB `store_url` 컬럼이 nullable이다 |

- **판정**: 세그먼트(major·minor·patch)를 숫자로 비교한다. `현재 < minimumVersion`이면 `FORCE`, 아니고 `현재 < latestVersion`이면 `OPTIONAL`, 아니면 `NONE`. 문자열 비교가 아니라 `1.10.0 > 1.9.0`이다.
- **데이터**: 테이블 `app_version`(`V28`)은 `platform` 유니크, `minimum_version`·`latest_version` NOT NULL. 행은 코드가 만들지 않고 운영이 넣는다(배포 시 서버팀이 ANDROID 행 삽입).
- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|
| 400 | `INVALID_VERSION_FORMAT` | `version`이 `x.y.z`가 아님 |
| 500 | `APP_VERSION_NOT_FOUND` | 그 플랫폼 행이 DB에 없음. 서버 오류로 나가 앱이 400대와 구별할 수 없다 |

## Android 매핑

앵커: `AppVersionService`, `AppVersionRemoteDataSourceImpl`, `AppVersionRepositoryImpl`, `CheckForceUpdateUseCase`, `SplashViewModel`, `storeLinkCandidates`, `YGModalForceUpdate`, `VERSION_CHECK_TIMEOUT`

- **fail-open.** 404(서버 미배포)·500(`APP_VERSION_NOT_FOUND` 포함)·네트워크 오류·3초 초과 전부 "강제 아님"으로 통과한다. 재시도 안내는 없다. 판정은 `CheckForceUpdateUseCase` 한 곳이고, 3초 상한은 `SplashViewModel` 쪽(`VERSION_CHECK_TIMEOUT`)이다. 이 결합 때문에 서버가 죽어도 앱은 못 막고, 반대로 서버 DB 행이 빠지면 기능이 조용히 꺼진다.
- **`updateType`은 문자열로 받는다.** enum으로 바로 역직렬화하면 서버가 값을 추가하는 순간 파싱이 터져 fail-open으로 묻힌다. 모르는 값은 `UNKNOWN`으로 두고 강제로 취급하지 않는다.
- **`OPTIONAL`은 `NONE`과 같다.** 선택 업데이트 UI가 없어 통과시키고, 도메인 `AppUpdateType`에는 값만 있다 (OQ-P-464 ②).
- **`storeUrl`은 허용 출처만 쓴다.** `market` 스킴이거나 `https` + 호스트 `play.google.com`일 때만 맨 앞 후보가 된다. 아니면 버리고 `market://details?id=<패키지>` → `https://play.google.com/store/apps/details?id=<패키지>` 순서로 연다(`storeLinkCandidates`). 서버 값이 null이어도 같다.
- **`version`은 `APP_VERSION_NAME`을 그대로 보낸다.** `versionNameSuffix`를 붙이면 서버가 400을 주고 fail-open으로 영영 판정되지 않는다.

## 미결

- `minimum_version`을 언제 누가 올리는지, `OPTIONAL` UI 시나리오 → [open-questions](../synthesis/open-questions.md) OQ-P-464
