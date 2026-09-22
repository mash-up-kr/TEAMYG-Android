---
id: vendor-android-kotlin-skills
title: Android/Kotlin/Compose 스킬 벤더링 + 탐색·업데이트 체계
status: implemented
category: tooling-spec
platforms: android
verified: 2026-07-22
related_code:
  - parfait/script/search.py
  - parfait/script/vendor.py
  - .claude/skills/skill-finder/SKILL.md
  - .claude/skills/update-injected-skills/SKILL.md
  - .claude/skills-vendor/baseline.json
  - .claude/skills-vendor/CATALOG.md
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, tooling, skills, vendoring]
---

# Spec: Android/Kotlin/Compose 스킬 벤더링 + 탐색·업데이트 체계

> **범위**: 이 repo(스킬·위키 repo)에서 **TJYG-Android 구현 작업을 지휘**하고 코드는 TJYG repo에 반영된다.
> 여기 벤더링하는 스킬(android/kotlin/compose)은 그 **워크플로 A(TJYG 코드 구현)에 직접 복무**한다 →
> parfait(구현 문서 허브)에 두는 게 자연스럽다. 단 산출물이 앱 소스가 아니라 **하니스/툴링**이라
> `category: tooling-spec`(통상 ui/behavior-spec과 구분). specs/plans 단일 홈 = parfait 규약.
>
> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.

- **작업 repo**: team-yg-pesonal-agent(스킬·위키 repo, TJYG-Android 작업 지휘 허브). 스킬 설치 대상도 이 repo `.claude/skills/`.

## 목표

외부 4개 repo의 스킬을 이 repo `.claude/skills/`에 **전량 벤더링**해 스킬 허브로 삼고,
spec/plan 작성 시 관련 스킬이 **적재적소에 로드**되게 배선하며, 벤더한 스킬을 **업데이트 가능**하게 한다.

### 소스 repo (전부 Apache-2.0)

| repo | 구조 | 스킬 수 |
|------|------|--------|
| `android/skills` | 카테고리 dir(raw) | 20 |
| `Kotlin/kotlin-agent-skills` | `.claude-plugin` 마켓플레이스 + `skills/` | 6 |
| `skydoves/compose-performance-skills` | 카테고리 dir(raw) | 26 |
| `chrisbanes/skills` | `.claude-plugin` 마켓플레이스 + `skills/` | 22 |

합계 **73개**. leaf 디렉토리명 충돌 0(검증), 기존 프로젝트 스킬(ingest·lint·query·start-session·sync-tjyg-develop-baseline)과도 무충돌.

## 배경 — Claude Code 스킬 메커니즘 (설계 전제)

공식 문서 확인 결과:
- CC는 세션 시작 시 **모든 스킬의 name+description(≤1536자)을 컨텍스트에 자동 주입**한다. "인덱스"가 항상 in-context → 별도 검색 CLI가 탐색 latency를 줄이지 않음.
- **프로젝트 스킬 이름 = 디렉토리명**(frontmatter `name` 아님). `.claude/skills/<name>/SKILL.md` 1단계 배치가 안전.
- 이름 충돌 우선순위: **enterprise > personal > project**.
- 마켓플레이스(`extraKnownMarketplaces`+`enabledPlugins`)로 선언해도 외부 소스 플러그인은 **팀원이 각자 `claude plugin install` 실행해야** 로드됨(자동 아님) → 팀 공유엔 부적합.
- submodule 내부 SKILL.md 발견은 공식 미명시. **symlink는 따라감**. → 자체 복사(벤더)가 가장 확정적.

즉 tao-agent-os의 `workflow_search.py`식 검색은 "문서가 자동 주입 안 되는" 시스템용이라 CC엔 그대로 이식할 필요 없음(의존 모듈 3개 + wikimap 인프라도 필요). 대신 **경량 자체 검색 스킬**로 정밀도·확장성만 취한다.

## 아키텍처 결정: C안 (네이티브 73 + 경량 search)

- 73개를 **네이티브 스킬로 설치**(Skill 툴 호출·context:fork·allowed-tools 등 CC 기능 유지).
- description 자동주입에 따른 컨텍스트 증가는 수용(전량 도입 선택의 대가). 검색으로 **선택 정밀도**를 보완.
- 반려: A(search 없음 — 규모 확장 시 선택 흐림), B(문서+search만 — 네이티브 기능 상실·구현 부담 최대).

## 구성 요소

### A. 벤더링 (설치)

- 각 스킬 dir **전체**(SKILL.md + 부속파일: scripts/references/assets 등)를 `.claude/skills/<원본 leaf 이름>/`로 복사. 원본 이름 유지(충돌 0, upstream 명령어명·내부 상호참조 보존).
- 카테고리 계층(예: `jetpack-compose/theming/styles`)은 평면화 → 스킬명 `styles`(leaf). 매핑은 MANIFEST가 기록.
- 각 소스 repo LICENSE를 `.claude/skills-vendor/licenses/<repo>.LICENSE`로 보존(Apache-2.0 출처표기 충족).

### B. 적재적소 라우팅 (keyflow "명시적 경로+조건"의 CC판)

- **CLAUDE.md 워크플로 A**(TJYG-Android 코드 구현: brainstorming→writing-plans→TDD)에 규칙 추가:
  > spec(brainstorming)·plan(writing-plans) 작성 단계에서, 주제(예: recomposition·stability·navigation·coroutines·testing·gradle)에 대해 **`skill-finder`로 먼저 검색**해 매칭 스킬을 로드한 뒤 설계/계획을 확정한다. (확정적 조건 — 생략 금지.)
- `.claude/skills-vendor/CATALOG.md` — 73개를 주제별 그룹핑한 브라우즈 보조(사람·모델용 목차). 검색의 1차 수단은 skill-finder, CATALOG는 개요.

### C. `skill-finder` (검색 스킬)

- 위치: SKILL.md는 `.claude/skills/skill-finder/`, 로직은 `parfait/script/search.py`(스크립트 규약).
- 동작: 형제 `.claude/skills/*/SKILL.md`의 frontmatter(name·description)+제목을 스캔해 자연어 쿼리로 **키워드/BM25 랭킹** 후 상위 N개를 `이름 — description (score)` 형태로 반환. 자체 완결(외부 인덱스·wikimap 불필요, 표준 라이브러리만).
- 용도: spec/plan 단계에서 모델이 `skill-finder "<주제>"` 호출 → 랭킹 결과 중 관련 스킬을 네이티브 `Skill`로 호출.
- **문서화**: 이 용법을 CLAUDE.md 워크플로 A(위 B)와 skill-finder SKILL.md에 명시 → 다음 세션 spec/plan 작성 때 재사용되게 한다.

### D. `update-injected-skills` (baseline+diff 업데이트 스킬)

기존 `sync-tjyg-develop-baseline`(doc-baseline SoT + delta)과 **동형**.
- **baseline SoT**: `.claude/skills-vendor/baseline.json` — 소스 repo별 `sha·branch·url`. 사람용 `baseline.md`는 렌더 산출물.
- 위치: SKILL.md는 `.claude/skills/update-injected-skills/`, 로직은 `parfait/script/vendor.py`(스크립트 규약).
- **동작**:
  1. 각 소스 repo `git ls-remote`로 현재 HEAD SHA 취득.
  2. baseline SHA와 동일하면 skip. 다르면 `baseline_sha..HEAD` 범위에서 **변경된 SKILL 디렉토리만** 판별(추가/수정/삭제).
  3. 변경분만 재복사(수정·추가) / 삭제분 제거. **전량 재복사 안 함**(delta only).
  4. MANIFEST·CATALOG 갱신 필요분 반영, 변경 요약 보고.
  5. baseline SHA·날짜 갱신 + 이력 1줄 추가.
- 초기 벤더링(요소 A)도 이 스크립트의 "전량 모드"로 수행 → 로직 단일화.

## 레이아웃

```
parfait/script/              # 파이썬 툴링 홈(규약: parfait/script/README.md)
  vendor.py + test_vendor.py # 벤더/업데이트 엔진
  search.py + test_search.py # 검색 랭킹
  _script-template.py · SKILL.template.md   # 템플릿
.claude/skills/
  <73 vendored>/SKILL.md (+부속파일)
  skill-finder/SKILL.md            # search.py(parfait/script) 호출
  update-injected-skills/SKILL.md  # vendor.py(parfait/script) 호출
  (기존) ingest/ lint/ query/ start-session/ sync-tjyg-develop-baseline/
.claude/skills-vendor/       # 비-스킬 지원 디렉토리(SKILL.md 없음 → CC 무시)
  sources.json               # 입력: 4 repo
  baseline.json / manifest.json   # 머신 SoT
  baseline.md / MANIFEST.md / CATALOG.md   # 사람용 렌더 산출물
  licenses/<repo>.LICENSE    # repo별 라이선스 보존
  .cache/                    # blobless clone (gitignore)
```

## 비목표 (YAGNI)

- tao-agent-os `workflow_search.py`/wikimap/doc-graph 이식(중복·인프라 과다).
- 마켓플레이스 방식 설치(팀원 수동 install 필요).
- 컨텍스트 절감(네이티브 설치 선택 → description 주입 불가피, 수용).
- 73개 큐레이션(전량 벤더링 확정 — 타 프로젝트 재사용 목적).
- 스킬 내용 자체 수정(순수 벤더 — upstream 원본 보존, 로컬 편집 금지).

## 열린 질문

- `skill-finder` 랭킹: 순수 키워드 vs 경량 BM25 — 73개 규모엔 키워드 가중(제목>description>본문)로 충분할 수 있음. 구현 시 확정.
- CATALOG 주제 그룹 경계(성능/recomposition/stability 중복 스킬을 어느 그룹에) — 초안 후 조정.
- 평면화로 생기는 제네릭 이름(`styles`·`adaptive`·`shepherd`·`implement-issue` 등)이 향후 personal 스킬과 그림자 충돌 가능(personal>project) — 낮음, MANIFEST로 추적.
