---
id: <kebab-case-id>
title: <사람이 읽는 제목> (<English/SDK term>)
status: draft                # draft | in-progress | implemented | superseded
category: ui-spec            # ui-spec | behavior-spec | ...
platforms: android           # 이 repo는 TJYG-Android(Kotlin/Compose) 전용
verified: YYYY-MM-DD          # 코드와 대조 확인한 날짜
related_code:                 # 파일명#심볼 (라인번호·hex·변동수치 금지 — parfait 규칙)
related_adr:                  # ADR-NNNN (없으면 비움)
related_spec:                 # 관련 스펙 id
related_architecture:         # architecture 문서명
supersedes:                   # 이 스펙이 대체하는 기존 스펙 id (없으면 비움)
superseded_by:                # 이 스펙을 대체한 새 스펙 id (없으면 비움)
tags: [spec, parfait]
---

# Spec: [기능/컴포넌트명]

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표
무엇을 만드는가, 왜(한두 문장).

## 범위
- 포함: …
- 제외: … (명시적으로 안 만드는 것)

## API / 인터페이스
```kotlin
// 시그니처
```
- 파라미터별 의미·기본값.

## 동작 / 상태
- 상태 목록과 각 상태의 조건(런타임 vs prop).
- 상태 → 토큰(색·타이포·모양·간격) 매핑 표. 심볼명으로.

## 표시·제어 규칙
- 조건부 노출, 입력 제어 등.

## 파일 구성
- 만들 파일과 각 역할.

## 주의 / 열린 질문
- 과도기·미확정 사항 → [open-questions](../../../wiki/open-questions.md) 연동.

<!--
이 템플릿 사용법 (작성 후 이 주석 블록은 삭제):
- 복사 후 `docs/superpowers/specs/<YYYY-MM-DD-kebab-topic>.md`로 저장, frontmatter 채우고
  `docs/superpowers/specs/README.md` 인덱스에 한 줄 추가.
- `category: ui-spec`(단일 위젯)이면 다중 UI 상호작용 섹션은 생략 가능.
- 사양 변경 시: 값 수정 + `verified` 갱신 + `related_code` 심볼명 재확인(라인번호 안 씀).
- 결정 번복(대체) 시: 기존 문서 `status: superseded` + `superseded_by` 지정,
  새 문서 `supersedes` 지정(원본 내용은 보존). 구현 완료 스펙은 `status: implemented` + `archive/`로 이동.
-->
