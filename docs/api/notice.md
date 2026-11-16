---
id: notice
title: 공지사항 (목록 조회)
server_module: http/notice
server_commit: 6689a0c
verified: 2026-11-11
android_status: done
related_spec: notice-server-integration
related_adr:
tags: [api, parfait, server-contract, notice]
---

# 공지사항 API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).
> 서버 PR `[Feat/#221] 공지사항 조회 API 및 어드민 등록 API 추가`(#222)로 신설됐다.

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| GET | `/api/v1/notices?platform=` | 필요(JWT) | 쿼리 `platform` | List<`NoticeResponse`> | 구현됨 |

`POST /admin/notices`·`PATCH /admin/notices/{id}`는 운영자가 Swagger로 쓰는 어드민 API(`X-Notice-Token`)라 앱이 부르지 않는다.

## 엔드포인트 상세

### GET /api/v1/notices

- **요청**: `platform`은 `IOS`·`ANDROID`·`COMMON` 중 하나이고, 앱은 `ANDROID`를 보낸다.
- **조회 범위**: `platform IN (요청 플랫폼, COMMON)` 이고 게시일 ≤ 오늘(KST). **게시일 내림차순, 같은 날은 id 내림차순**이라 앱은 배열 순서를 화면 순서로 그대로 쓴다.
- **응답 필드**

| JSON 키 | 타입 | 널 허용 | 비고 |
|---|---|---|---|
| `id` | Long | 아니오 | |
| `type` | String | 아니오 | `UPDATE` · `GUIDE`. 앱은 구분하지 않는다 |
| `title` | String | 아니오 | 서버가 저장 시점에 만든다(`파르페 v{버전} 업데이트 안내` 등) |
| `content` | String | 아니오 | 본문 전체 |
| `publishedDate` | String | 아니오 | `yyyy-MM-dd`(KST 게시일) |

## Android 매핑 · gotcha

- **상세 엔드포인트가 없다.** 목록이 본문까지 담는다. 상세는 목록 값을 `NavKeyNoticeDetail`로 받아 그린다.
- **읽음 상태 필드가 없다.** "새 공지"는 앱이 게시 14일 미만(KST)이고 이 기기에서 열지 않은 공지로 판정한다([설계](../superpowers/specs/2026-11-11-notice-server-integration-design.md)).
- 업데이트 공지에도 `COMMON`이 허용된다(서버 PR 리뷰 노트).
