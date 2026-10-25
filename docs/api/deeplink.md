---
id: deeplink
title: 딥링크 웹 표면(App Links 검증 파일·초대 링크 폴백·루트 랜딩)
server_module: http/api/deeplink, http/api/page/root, core/deeplink
server_commit: 8b4a94b
verified: 2026-10-25
android_status: done
related_spec:
related_adr:
tags: [api, parfait, server-contract, deeplink]
---

# 딥링크 웹 표면 API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

앱이 호출하는 API가 아니라 **브라우저·OS·스토어가 읽는 웹 표면**이다. `/api` 밖에 있고 전부
`ApiResponse` envelope를 쓰지 않는다(HTML 또는 스펙이 정한 JSON 그대로). 그래도 Android App Links의
검증과 앱 미설치 유저의 초대코드 복원이 이 표면에 기대므로 계약으로 적는다.
앱 쪽 흐름은 [navigation-flow](../architecture/navigation-flow.md) 「App Links 이동」.

**호스트는 루트 도메인 `parfait-app.store`다**(`landing.domain`). API 호스트와 다르다. 앞단 Caddy가 이
호스트에서 `/` `/link` `/.well-known/*` `/assets/*` `/icon-512.png`만 애플리케이션으로 넘기고 나머지는
Caddy가 직접 404를 준다(`deploy/caddy/Caddyfile`의 `@public` 매처).

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/.well-known/assetlinks.json` | 불필요 | 없음 | JSON 배열(Digital Asset Links) | 구현됨[^assetlinks] |
| GET | `/.well-known/apple-app-site-association` | 불필요 | 없음 | JSON 객체(AASA) | 해당 없음[^ios] |
| GET | `/link` | 불필요 | 쿼리스트링 임의 · `User-Agent` 헤더 | `text/html` 폴백 페이지 | 구현됨[^link] |
| GET | `/` | 불필요 | 없음 | `text/html` 랜딩 페이지 | 해당 없음[^root] |

정적 리소스 `/assets/**`(OG 이미지·SUIT 폰트·랜딩 번들)도 인증 없이 열린다. 다섯 경로 모두
`SecurityConfig.WHITELIST_PATHS`에 있다.

[^assetlinks]: 앱 매니페스트의 `intent-filter android:autoVerify="true"`가 이 파일로 검증된다.
    `package_name`이 앱 `applicationId`(`com.teamyg.parfait`)와 같다.
[^ios]: iOS Universal Links 전용이다. Android는 읽지 않는다.
[^link]: 앱이 이 페이지를 부르지는 않는다. 앱이 설치돼 있으면 OS가 이 URL을 앱으로 넘기고
    (`Intent.toAppLinkDeepLinkOrNull`), 미설치면 이 페이지가 Play 스토어로 보내며 실은 referrer를 첫 실행 때
    `AppLinkReferrerRepositoryImpl`이 읽는다. 둘 다 아래 계약과 맞다.
[^root]: 랜딩 페이지다. 앱 매니페스트가 루트를 일부러 받지 않는다(navigation-flow 「App Links 이동」).

## 엔드포인트 상세

### GET /.well-known/assetlinks.json

- **인증**: 불필요
- **성공**: HTTP 200 · `Content-Type: application/json` · envelope 없음. OpenAPI에서 숨겨져 있다(`@Hidden`).
- **응답 본문**: 원소 하나짜리 배열.

| JSON 경로 | 값 | 비고 |
|---|---|---|
| `[0].relation` | `["delegate_permission/common.handle_all_urls"]` | 고정 |
| `[0].target.namespace` | `"android_app"` | 고정 |
| `[0].target.package_name` | 설정 `deeplink.android.package-name` | 현재 `com.teamyg.parfait` |
| `[0].target.sha256_cert_fingerprints` | 설정 `deeplink.android.sha256-cert-fingerprints`를 쉼표로 나눈 배열 | 현재 2개 — 설정 주석상 debug 키스토어 지문과 Play 앱 서명 키 지문 |

  키가 snake_case여야 해서 `DeepLinkWellKnownController`가 data class 대신 `Map`을 그대로 돌려준다.
  ⚠️ **지문이 비어 있어도 200이다.** 설정이 빈 문자열이면 빈 배열이 나가고 검증만 조용히 실패한다.
  ⚠️ **debug 빌드가 검증을 통과하는 것은 그 지문이 특정 debug 키스토어 하나와 같을 때뿐이다.** 개발자마다
  로컬 debug 키스토어가 다르면 그 기기의 debug 빌드는 검증되지 않고 링크가 브라우저 선택지로 떨어진다.
  등록된 지문이 실제 키와 같은지는 서버 코드만으로 확인할 수 없다 → [미결](#미결).

### GET /.well-known/apple-app-site-association

- **인증**: 불필요
- **성공**: HTTP 200 · `application/json` · envelope 없음 · `@Hidden`.
- **응답 본문**: `applinks.apps`는 빈 배열이고, `applinks.details`는 `deeplink.ios.team-id`·`bundle-id`가
  둘 다 있을 때만 원소 하나(`appID` = `<team-id>.<bundle-id>`, `paths` = `["/link"]`)다. 둘 중 하나가 비면
  `details`가 빈 배열이다.

  **iOS가 앱으로 여는 경로는 `/link` 하나다.** 하위 경로(`/link/ABC123`)는 열지 않는다. Android 매니페스트는
  `/link`와 `/link/` 둘을 받으므로 끝 슬래시만 두 플랫폼이 다르다.

### GET /link

초대 링크 `https://parfait-app.store/link?inviteCode={code}`의 웹 폴백이다. 앱이 설치돼 있으면 OS가
이 요청 전에 앱을 연다. 이 페이지는 앱이 없거나 App Links가 적용되지 않는 환경에서만 보인다.

- **인증**: 불필요
- **성공**: 항상 HTTP 200 · `text/html; charset=UTF-8`. 서버 단 리다이렉트(3xx)가 아니다 — 로딩 화면 HTML을
  주고 인라인 스크립트가 `window.location.replace(redirect_url)`로 넘긴다. 같은 URL이 「직접 이동하기」 버튼의
  `href`에도 들어간다(목적지가 없으면 `#`).
- **요청**: 쿼리스트링을 해석하지 않고 **원문 그대로**(`HttpServletRequest.queryString`) 쓴다. `inviteCode`
  키를 서버가 검사하지 않는다.
- **목적지 판정**(`GetDownloadStoreLinkService`): 위에서부터 먼저 맞는 것.

| 조건 | 목적지 |
|---|---|
| `User-Agent`에 `KAKAOTALK`(대소문자 무시) | `kakaotalk://web/openExternal?url=<인코딩한 https://parfait-app.store/link?원본쿼리>` — 카카오톡 인앱 브라우저에서 외부 브라우저로 같은 링크를 다시 연다. 플랫폼과 무관하다 |
| `User-Agent`에 `android` | `https://play.google.com/store/apps/details?id=com.teamyg.parfait&referrer=<원본쿼리를 URL 인코딩>`. 쿼리가 없으면 `&referrer=` 없이 나간다 |
| `User-Agent`에 `iphone`·`ipad`·`ipod` | `https://apps.apple.com/app/id<deeplink.ios.app-store-id>`. id가 비면 목적지 없음 |
| 그 밖(데스크톱·UA 없음) | 목적지 없음 — 로딩 화면만 남는다 |

  **referrer 형식이 앱의 가정과 맞는다.** `?inviteCode=ABC123`이면 `referrer=inviteCode%3DABC123`이 되고,
  Play가 설치 후 넘기는 referrer 문자열은 `inviteCode=ABC123`이다. 앱은 그것을 `&`·`=`로 나눠 `inviteCode`
  값을 꺼낸다. 원본 쿼리를 통째로 싣기 때문에 다른 파라미터(`utm_*` 등)도 함께 실린다 — 앱 파서는 그것을
  견딘다.

  ⚠️ **`/link/`(끝 슬래시)는 폴백을 받지 못한다.** Caddy `@public` 매처와 Spring 매핑이 둘 다 `/link`
  정확 일치라 Caddy가 404를 준다. 앱 매니페스트는 `/link/`도 받으므로, 앱이 있으면 열리고 없으면 404다.
  앱이 공유하는 문자열(`group_invite_message`)은 `/link`라 지금은 드러나지 않는다.

- **OG 메타**: `og:title`은 `Parfait`이고 `og:image`는 `https://parfait-app.store/assets/og_image.png`
  (1200×600)다. 메신저 공유 미리보기가 이 값을 읽는다. 초대코드별로 달라지지 않는다.
- **에러**: 없다. UA가 없거나 쿼리가 비어도 200이다.

### GET /

- **인증**: 불필요
- **성공**: 요청 호스트(`serverName`)가 `landing.domain`과 같을 때만 HTTP 200 · `text/html`(`web/landing` 빌드
  산출물, 기동 때 한 번 읽는다). 다른 호스트로 들어오면 **본문 없는 404**다 — API 호스트의 `/`는 404다.
- 랜딩 페이지는 Android 일반 브라우저면 렌더 전에 `intent://details?id=com.teamyg.parfait#Intent;scheme=market;…`로
  Play 스토어를 연다. 이 경로에는 referrer가 없다 — 초대코드를 실어 나르는 것은 `/link`뿐이다.

## Android 매핑

| 계약 | Android 심볼 |
|---|---|
| `assetlinks.json`의 `package_name` | `app/build.gradle.kts` `applicationId` · `AndroidManifest.xml` `intent-filter android:autoVerify="true"`(`host="parfait-app.store"`, `path="/link"`·`"/link/"`) |
| `/link` 경로·`inviteCode` 쿼리 | `AppLinkDeepLinkIntent.kt` `APP_LINK_PATHS` · `Intent.toAppLinkDeepLinkOrNull` · `AppLinkDeepLink.OpenGroupInvite.INVITE_CODE_QUERY_KEY` |
| Play referrer `inviteCode%3D…` | `AppLinkReferrerRepositoryImpl#consumeDeepLinkOnce`(`referrerQueryParameter`) |
| 초대 링크 문자열 | `core/ui` `strings.xml` `group_invite_message` |

호스트·경로가 세 곳에 따로 적혀 있는 사정은 [open-questions](../synthesis/open-questions.md) OQ-P-436.

## 미결

- `sha256_cert_fingerprints`에 등록된 두 지문이 실제 Play 앱 서명 키·팀 debug 키스토어와 같은지 서버 코드로는
  확인할 수 없다. 지문이 틀려도 응답은 200이다 → [open-questions](../synthesis/open-questions.md) OQ-P-449
