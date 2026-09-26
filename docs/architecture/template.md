---
id: <kebab-case-id>            # 예: module-structure
title: <사람이 읽는 제목>
category: architecture
status: living                 # living | superseded | deprecated
platforms: android             # 이 repo는 TJYG-Android(Kotlin/Compose) 전용
verified: YYYY-MM-DD           # 코드와 대조 확인한 날짜
related_spec:
related_adr:
related_architecture:
related_code:                  # 파일명#심볼 (라인번호·hex·변동수치 금지 — parfait 규칙)
tags: [architecture, parfait]
---

# [문서 제목]

> 상시 갱신되는 **구현 가이드**("어떻게/어디"). 결정 근거(why)는 `../adr/`, 구현 직전 설계(what)는 `../specs/`.
> 근거는 파일명 + 심볼명으로만. 라인번호·모듈 개수 등 변동 수치는 적지 않는다(→ [`../adr/README.md`](../adr/README.md)).

## [섹션]
…

<!--
사용법 (작성 후 이 주석 삭제):
- `parfait/architecture/<id>.md`로 저장, frontmatter 채우고 `parfait/architecture/README.md` 인덱스에 한 줄 추가.
- `status: living` = 상시 갱신 문서. 코드 대조 시 `verified` 갱신.
- 대체/폐기 시 `status: superseded | deprecated`.
-->
