---
id: <slug>                     # 파일명 topic (날짜 접두사 제외)
title: <사람이 읽는 제목>
status: draft                  # draft | in-progress | done | abandoned | superseded
type: work-order               # work-order | handoff
created: YYYY-MM-DD
updated: YYYY-MM-DD
platforms: android             # 이 repo는 TJYG-Android(Kotlin/Compose) 전용
owner:                         # 담당 팀/역할 (실명·개인정보 금지 — public repo)
related_adr:
related_spec:
related_code:                  # 파일명#심볼 (라인번호·hex·변동수치 금지 — parfait 규칙)
archived_reason:               # done/abandoned 시 사유 (활성 계획은 비움)
tags: [plan, parfait]
---

# [기능/컴포넌트] Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]`)로 추적.

**Goal:** …

**Architecture:** …

## Tasks
- [ ] …

<!--
사용법 (작성 후 이 주석 삭제):
- `parfait/plans/YYYY-MM-DD-<slug>.md`로 저장, frontmatter 채우고 `parfait/plans/README.md` 활성 카탈로그에 한 줄 추가.
- 플랫폼별 핸드오프(type: handoff)는 필요 시 `plans/aos/` 등 하위에 둔다(이 repo는 Android 전용).
- 작업 완료: `status: done` + `archived_reason` + 상단 Archived 배너 후 `archive/`로 이동, `README.md` 아카이브에 한 줄 기록.
- 미채택/폐기: `status: abandoned` + `archived_reason` + `archive/`로 이동(맥락 보존).
- 결정 번복: `status: superseded` + 새 문서가 관련 참조.
-->
