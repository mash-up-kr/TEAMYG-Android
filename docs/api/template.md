---
id: <파일명(확장자 제외)>
title: <도메인 한 줄 이름>
server_module: <서버 소스 위치, 예: http/parfaitgroup>
server_commit: <대조한 서버 main 커밋 short hash>
verified: YYYY-MM-DD
android_status: none        # none | partial | done
related_spec:
related_adr:
tags: [api, parfait, server-contract, <도메인>]
---

# <도메인 이름> API 계약

> 정본은 서버 코드(`mash-up-kr/TEAMYG-SERVER` `main`). 이 문서는 미러다 — 어긋나면 서버가 옳다.
> 전역 계약(envelope·에러 체계·인증)은 [conventions.md](conventions.md).

## 엔드포인트

| 메서드 | 경로 | 인증 | 요청 | 응답 | Android |
|---|---|---|---|---|---|
| <GET/POST/…> | <경로> | <필요/불필요> | <요청 타입 또는 `없음`> | <응답 타입> | <미구현 / 구현됨 / ⚠️불일치> |

## 엔드포인트 상세

### <메서드> <경로>

- **인증**: <필요/불필요>
- **성공**: HTTP <코드> · envelope `code` = `"<OK/CREATED>"`
- **요청 필드**

| 필드 | 타입 | 필수 | 비고 |
|---|---|---|---|

- **응답 필드**

| 필드 | 타입 | 널 허용 | 비고 |
|---|---|---|---|

- **에러 코드**

| HTTP | code | 의미 |
|---|---|---|

## Android 매핑

<대응 심볼명(`XService#method`·`XResponse`·`XRemoteDataSource`) 또는 "없음">

## 미결

- <항목> → [open-questions](../synthesis/open-questions.md)
