---
id: orchestration-session-pipeline
title: 요구사항 → 구현 오케스트레이션 파이프라인 스킬 구현 계획
status: draft
type: work-order
created: 2026-08-05
updated: 2026-08-05
platforms: android
owner: harness
related_adr:
related_spec: orchestration-session-pipeline
related_code: .claude/skills/start-orchestration-session/SKILL.md, .claude/skills/start-default-session/SKILL.md
archived_reason:
tags: [plan, parfait, tooling, orchestration]
---

# 오케스트레이션 세션 파이프라인 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:subagent-driven-development(권장) 또는 superpowers:executing-plans로 task 단위 구현. 단계는 체크박스(`- [ ]` → `- [x]`)로 추적.

> **실행 완료(2026-08-05)** — Task 1~6이 전부 끝났고, 각 Task의 검증 단계와 커밋 단계까지
> 수행됐다. 아래 체크박스는 그 실행 결과다. 이후 전면 코드 리뷰의 지적을 반영한 수정은
> 이 계획의 Task가 아니라 별도 수정 웨이브로 처리했다.
>
> 따라서 **각 Step에 인용된 마크다운·bash 블록은 실행 시점의 원문**이고, 그 뒤 수정 웨이브에서
> 달라진 부분이 있다(반려 루프의 재-디스패치 전환, G1·G2의 대화형 전환, 문서 워커의 모델 지정
> 2단계 경로, `--timeout-ms` 값). **현재 정본은 언제나 `.claude/skills/start-orchestration-session/SKILL.md`
> 와 스펙이다.** 이 계획서를 그대로 재실행하지 말 것.

**Goal:** 요구사항 하나를 Orca orchestration + git worktree로 분석·설계·계획·TDD 구현·리뷰까지 다수 에이전트가 나눠 수행하는 파이프라인을 스킬 문서로 규정하고, 진입점 `start-orchestration-session`을 만든다.

**Architecture:** 산출물은 마크다운 스킬 문서 2개와 라우팅 한 줄이다. 실행 로직은 코드가 아니라 스킬 문서에 적힌 절차이고, 그 절차를 코디네이터 세션이 `orca orchestration` CLI로 직접 수행한다. 기존 `start-session`은 `start-default-session`으로 개명해 두 진입점을 구분한다.

**Tech Stack:** Markdown(SKILL.md frontmatter + 본문), `orca` CLI 1.4.164(`orchestration.contract.v1`), git worktree, Gradle(TJYG-Android).

## Global Constraints

- 대상 저장소는 team-yg-pesonal-agent(이 repo)다. TJYG-Android 코드는 이 계획에서 **한 줄도 건드리지 않는다**.
- 브랜치는 `feat/orchestration-session-skill`. `main` 직접 커밋 금지.
- **`git push`와 PR 생성(`gh pr create`)은 실행 전 사용자 확인 필수.** `git commit`은 로컬이라
  확인 없이 한다(실행 중 사용자가 완화, `CLAUDE.md`의 "Git 워크플로 (필수)" 절 참조).
- 스킬 디렉토리명과 `SKILL.md` frontmatter의 `name` 값은 **반드시 일치**해야 한다. 불일치 시 스킬이 로드되지 않는다.
- 스킬 문서 본문은 한국어. 스킬 안에 인용하는 CLI 명령·플래그·frontmatter 키는 원문 그대로.
- 검증에 쓰는 `orca` 명령은 **읽기 전용만**(`--help`, `status`, `repo list`, `worktree list`). `run-create`·`task-create`·`worker-start`처럼 상태를 만드는 명령은 이 계획에서 실행하지 않는다.
- 확정된 사실(2026-08-05 실측): Orca 1.4.164 / `orchestration.contract.v1` 보유 / 설치된 에이전트 CLI는 `claude` 하나 / TJYG-Android·team-yg 모두 `setupAgentStartupPolicy: start-immediately`.
- 스펙 정본: `parfait/specs/2026-08-05-orchestration-session-pipeline.md`. 계획과 스펙이 어긋나면 스펙이 정답이고, 스펙을 고쳐야 하면 사용자에게 먼저 알린다.

---

### Task 1: `start-session` → `start-default-session` 개명

**Files:**
- Rename: `.claude/skills/start-session/` → `.claude/skills/start-default-session/`
- Modify: `.claude/skills/start-default-session/SKILL.md` (frontmatter `name`·`description`, 본문 제목)

**Interfaces:**
- Consumes: 없음(첫 Task)
- Produces: 디렉토리 경로 `.claude/skills/start-default-session/`, 스킬 이름 `start-default-session`. Task 6의 CLAUDE.md 라우팅 문구가 이 이름을 참조한다.

- [x] **Step 1: 현재 참조처를 다시 확인**

```bash
grep -rn "start-session" --include="*.md" --include="*.json" . | grep -v "^./parfait/specs/archive/"
```

기대 출력: `.claude/skills/start-session/SKILL.md`의 2줄(`name:`, `description:`)과 본문 제목 1줄.
`parfait/specs/archive/2026-07-22-vendor-android-kotlin-skills.md`의 2건은 아카이브된 역사 기록이므로 **고치지 않는다**.
그 밖의 파일이 나오면 멈추고 사용자에게 보고한다.

> **재실행 시 주의** — 개명이 끝난 지금 이 grep을 다시 돌리면 이 계획서와
> `parfait/specs/2026-08-05-orchestration-session-pipeline.md`가 매치된다. 개명 작업 자체를
> 서술하는 역사적 문장(`start-session` → `start-default-session`)이라 **정상 매치이고 결함이
> 아니다.** 실제 결함은 `.claude/skills/` 아래에 `start-session`이 남아 있는 경우뿐이다.

- [x] **Step 2: git mv로 디렉토리 개명**

```bash
git mv .claude/skills/start-session .claude/skills/start-default-session
```

- [x] **Step 3: frontmatter와 본문 제목 수정**

`.claude/skills/start-default-session/SKILL.md`의 앞부분을 아래로 바꾼다. 내용(단계·주의)은 손대지 않는다.

```markdown
---
name: start-default-session
description: 이 repo에서 세션 시작 시 기본 온보딩. 사용자가 "/start-default-session", "세션 시작", "온보딩", "먼저 읽어", "claude.md·위키·개인정보 읽어"라고 하거나 새 세션에서 작업 방향을 잡기 전에 사용. repo 3축 구조·작업 라우팅·로컬 경로를 로드한다. 오케스트레이션 파이프라인을 돌릴 때는 start-orchestration-session을 쓴다.
---

# start-default-session — 세션 온보딩
```

- [x] **Step 4: 개명이 완전한지 검증**

```bash
test -f .claude/skills/start-default-session/SKILL.md && echo "OK: file exists"
test -d .claude/skills/start-session && echo "FAIL: old dir remains" || echo "OK: old dir gone"
head -3 .claude/skills/start-default-session/SKILL.md | grep -q "^name: start-default-session" && echo "OK: name matches dir"
grep -rn "start-session" --include="*.md" .claude/ | grep -v "start-default-session" | grep -v "start-orchestration-session"
```

기대: 앞 세 줄이 전부 `OK`, 마지막 grep은 **출력 없음**.
(마지막 grep은 `.claude/`로 범위를 좁혀 두었으므로 계획서·스펙의 역사적 서술은 걸리지 않는다.
범위를 repo 전체로 넓혀 돌리면 Step 1의 주의대로 계획서·스펙이 매치되는데 그건 정상이다.)

- [x] **Step 5: 커밋 (로컬 커밋이라 건별 확인 없이 진행 — 사용자가 Task 1~6 커밋을 일괄 사전 승인함)**

```bash
git add .claude/skills/start-default-session
git commit -m "refactor(skills): start-session을 start-default-session으로 개명"
```

---

### Task 2: `start-orchestration-session` 골격 — 전제 확인과 Run 부트스트랩

**Files:**
- Create: `.claude/skills/start-orchestration-session/SKILL.md`

**Interfaces:**
- Consumes: Task 1이 확정한 이름 `start-default-session`(스킬 설명에서 구분 대상으로 언급)
- Produces: `SKILL.md`의 frontmatter와 `## 0. 전제 확인` · `## 1. 요구사항 수집` · `## 2. Run 생성` 세 절. Task 3~5가 이 파일에 절을 이어 붙인다. 파이프라인 용어(`W1 analyst`, `W2 spec-reviewer`, `W3 planner`, `W4 plan-reviewer`, `wt-domain`/`wt-data`/`wt-feature`, `wt-integrate`, 게이트 `G1`/`G2`/`G3`, 프로필 `균형`/`품질`/`비용`)를 여기서 정의하고 이후 Task는 같은 표기를 쓴다.

- [x] **Step 1: 파일 생성 — frontmatter와 도입부**

```markdown
---
name: start-orchestration-session
description: 요구사항 하나를 Orca orchestration + git worktree로 분석·설계·계획·TDD 구현·리뷰까지 다수 에이전트에 나눠 수행하는 파이프라인 진입점. 사용자가 "/start-orchestration-session", "오케스트레이션 시작", "이 요구사항 파이프라인으로 돌려줘", "에이전트 나눠서 구현해줘"라고 할 때 사용. 코디네이터는 feature/xxxxx-master worktree에서 사람과 소통하고 나머지 단계는 워커에게 위임한다.
---

# start-orchestration-session — 요구사항 오케스트레이션 파이프라인

요구사항 하나를 받아 분석 → 설계 → 스펙 리뷰 → 계획 → 계획 리뷰 → TDD 구현(모듈 병렬)
→ 통합 → 코드 리뷰 → 최종 반환까지 자동으로 돈다.

설계 정본은 [`parfait/specs/2026-08-05-orchestration-session-pipeline.md`](../specs/2026-08-05-orchestration-session-pipeline.md).
이 문서와 스펙이 어긋나면 스펙이 정답이다.

## 이 스킬을 쓰지 않는 경우

- 단순 온보딩만 필요하다 → `start-default-session`
- 위키(`wiki/`·`raw/`) 작업 → `ingest`·`query`·`lint`
- 파일 한두 개 고치는 수준의 변경 → 파이프라인 비용이 이득보다 크다. 그냥 한다.

## 용어

| 표기 | 뜻 |
|---|---|
| 코디네이터 | `feature/xxxxx-master` worktree에서 도는 이 세션. 사람과 소통하는 유일한 지점 |
| W1 analyst | 요구분석 + 설계. `parfait/specs/`에 스펙 작성 |
| W2 spec-reviewer | 스펙 독립 검수 |
| W3 planner | 파일 선택 + 모듈 분할 + 테스트 명세. `parfait/plans/`에 계획 작성 |
| W4 plan-reviewer | 계획 독립 검수 |
| wt-domain / wt-data / wt-feature | Gradle 모듈별 구현 자식 worktree |
| wt-integrate | 병합·전체 테스트·코드 리뷰 자식 worktree |
| G1 / G2 / G3 | 사람 게이트 — 스펙 승인 / 계획 승인 / 최종 보고 |
| 프로필 | 모델 배치 3종 — `균형`(기본) / `품질` / `비용` |
```

- [x] **Step 2: `## 0. 전제 확인` 절 추가**

```markdown
## 0. 전제 확인

아래를 먼저 실행하고, 하나라도 실패하면 파이프라인을 시작하지 않고 사용자에게 보고한다.

```bash
orca status --json
orca repo list --json
orca worktree list --json
```

확인 항목:

| 항목 | 판정 |
|---|---|
| 런타임 | `result.runtime.state == "ready"` |
| orchestration | `result.runtime.capabilities`에 `orchestration.contract.v1` 포함 |
| TJYG-Android 등록 | `repo list`에 `TJYG-Android` 존재 |
| master worktree | `worktree list`에 `feature/…-master` 브랜치의 TJYG-Android worktree 존재 |
| 시작 정책 | TJYG-Android `hookSettings.setupAgentStartupPolicy` 값을 기록해 둔다 |

`feature/…-master` worktree가 없으면 만들고 시작한다.

```bash
orca worktree create --name <xxxxx>-master --repo name:TJYG-Android --base-branch develop --json
```

시작 정책이 `wait-for-setup`이면 모델 지정 2단계 경로(§4)를 쓸 수 없다.
그때는 모든 워커를 `--agent claude`(기본 모델)로 띄우고, 그 사실을 사용자에게 알린다.
`start-immediately`(2026-08-05 기준 현재 값)면 그대로 진행한다.
```

- [x] **Step 3: `## 1. 요구사항 수집` 절 추가**

```markdown
## 1. 요구사항 수집

사용자에게 요구사항을 받고, 아래 세 가지를 확정한다. 답이 없으면 기본값을 쓰고 그 사실을 알린다.

| 항목 | 기본값 | 의미 |
|---|---|---|
| 프로필 | `균형` | 모델 배치. 아래 표 |
| 진행 방식 | 게이트 유지 | `자동진행`이면 G1·G2를 건너뛰고 G3만 받는다 |
| master 브랜치 | 현재 worktree의 브랜치 | 결과를 돌려줄 대상 |

### 프로필

| 워커 | 균형(기본) | 품질 | 비용 |
|---|---|---|---|
| W1 analyst | Opus | Opus | Opus |
| W2 spec-reviewer | Opus | Opus | Opus |
| W3 planner | Opus | Opus | Sonnet |
| W4 plan-reviewer | Opus | Opus | Sonnet |
| 모듈 구현 워커 | 계획서의 `model:` | Opus | Sonnet |
| integrator | Sonnet | Opus | Sonnet |
| code-reviewer | Opus | Opus | Opus |

`품질`은 아키텍처를 건드리는 큰 변경, `비용`은 화면 하나 추가 수준의 정형 작업에 쓴다.
어느 프로필이든 **리뷰어는 작성자와 같은 티어 이상**이라는 규칙이 깨지지 않는다.

모델 배치의 근거 세 가지(스펙 §모델 분배):

1. 실패 비용은 상류일수록 크다 — 스펙이 틀리면 하류 전부가 폐기된다.
2. 판단 자유도가 낮으면 티어를 낮춘다 — RED/GREEN 게이트가 기계적으로 검증하는 자리는 안전하다.
3. 리뷰어는 작성자와 같은 티어 이상 — 아래 티어가 위 티어 산출물을 반려하기 어렵다.
   **기준 2와 충돌하면 기준 3이 이긴다.** 검증 장치가 다른 에이전트일 때 그 티어를 낮추면
   검증 자체가 약해진다. `비용` 프로필에서 W3·W4가 함께 Sonnet인 것은 규칙 위반이 아니다.

요구사항 원문은 **그대로 보존**한다. 요약해서 워커에 넘기지 않는다.
W1과 W2가 같은 원문을 보고 각자 판단해야 리뷰가 의미를 갖는다.
```

- [x] **Step 4: `## 2. Run 생성` 절 추가**

```markdown
## 2. Run 생성

```bash
orca orchestration run-create --objective "<요구사항 한 줄 요약>" --json
```

Run은 이 요구사항 하나에 대응하는 네임스페이스이자 코디네이터 우편함이다.
Run이 워커를 스케줄하지는 않는다 — 배치와 동시성은 코디네이터가 정한다.

Run을 만든 뒤 Orca 워크스페이스 카드에 시작을 남긴다.

```bash
orca worktree set --worktree current \
  --comment "오케스트레이션 시작: <요약> (프로필=<균형|품질|비용>)" \
  --workspace-status in-progress --json
```

이후 각 단계 전환마다 `--comment`를 갱신하고, 코드 리뷰 단계에서 `--workspace-status in-review`,
최종 보고에서 `completed`로 옮긴다. 이것은 **장식층**이다 —
진행의 정본은 `orca orchestration task-list --json`과 각 워커의 `worker_done`이다.
```

- [x] **Step 5: 골격 검증**

```bash
head -3 .claude/skills/start-orchestration-session/SKILL.md | grep -q "^name: start-orchestration-session" && echo "OK: name matches dir"
grep -c "^## " .claude/skills/start-orchestration-session/SKILL.md
grep -n "TBD\|FIXME" .claude/skills/start-orchestration-session/SKILL.md
```

기대: 첫 줄 `OK`, `## ` 절 개수 5(이 스킬을 쓰지 않는 경우 / 용어 / 0. 전제 확인 / 1. 요구사항 수집 / 2. Run 생성), 마지막 grep은 **출력 없음**.

- [x] **Step 6: 문서에 적은 orca 명령이 실재하는지 검증**

```bash
orca orchestration run-create --help >/dev/null 2>&1 && echo "OK run-create"
orca worktree create --help 2>&1 | grep -q -- "--base-branch" && echo "OK worktree create --base-branch"
orca worktree set --help 2>&1 | grep -q -- "--workspace-status" && echo "OK worktree set --workspace-status"
```

세 줄 모두 `OK`가 나와야 한다. 하나라도 실패하면 그 명령을 문서에서 빼고,
`orca skills get orchestration`으로 현재 문법을 다시 확인한 뒤 대체 명령을 적는다.

- [x] **Step 7: 커밋 (로컬 커밋이라 건별 확인 없이 진행 — 사용자가 Task 1~6 커밋을 일괄 사전 승인함)**

```bash
git add .claude/skills/start-orchestration-session/SKILL.md
git commit -m "feat(skills): start-orchestration-session 골격 — 전제 확인·요구사항 수집·Run 생성"
```

---

### Task 3: 문서 단계 — W1~W4와 게이트 G1·G2

**Files:**
- Modify: `.claude/skills/start-orchestration-session/SKILL.md` (`## 3. 문서 단계` 절 추가)

**Interfaces:**
- Consumes: Task 2의 용어표(W1~W4, G1·G2, 프로필)와 §2의 Run
- Produces: `## 3. 문서 단계` 절 — 워커 4종의 task spec 템플릿, 반려 루프 규칙, 게이트 처리. Task 4가 여기서 확정한 계획 문서 형식(각 task의 `model:` 필드와 담당 파일 화이트리스트)을 소비한다.

- [x] **Step 1: 문서 워커의 작업 디렉토리 제약을 실측**

문서 워커는 TJYG-Android worktree에서 돌면서 team-yg repo의 `parfait/` 아래에 쓴다.
cwd 밖 절대경로 쓰기가 막히는지 먼저 확인한다.

```bash
# 경로는 wiki/personal-private/project-paths.md에서 읽어 채운다(public repo라 직접 적지 않는다)
TJYG="<TJYG-Android 절대경로>"
TEAMYG="<team-yg-pesonal-agent 절대경로>"

cd "$TJYG"
printf 'probe\n' > "$TEAMYG/.orch-write-probe"
test -f "$TEAMYG/.orch-write-probe" && echo "OK: cross-repo write allowed"
rm "$TEAMYG/.orch-write-probe"
```

`OK`가 나오면 Step 2의 표를 그대로 쓴다.
막히면 문서 워커 4종의 `--worktree`를 team-yg repo worktree로 바꾸고(§3 표의 "실행 위치" 열),
그 경우 워커가 TJYG-Android 코드를 읽을 때는 절대경로로 읽는다는 문장을 함께 적는다.

- [x] **Step 2: `## 3. 문서 단계` 절 — 워커 배치표 추가**

```markdown
## 3. 문서 단계 (W1 → W2 → G1 → W3 → W4 → G2)

문서 단계는 TJYG-Android 코드를 건드리지 않는다. 산출물은 team-yg repo의 `parfait/` 아래에
절대경로로 쓴다. 그래서 worktree를 새로 파지 않고 터미널만 띄운다.

| 워커 | 실행 위치 | 모델(균형 프로필) | 산출물 |
|---|---|---|---|
| W1 analyst | `--worktree current` | Opus 5 | `parfait/specs/YYYY-MM-DD-<topic>.md` |
| W2 spec-reviewer | `--worktree current` | Opus 5 | findings(파일 없음) |
| W3 planner | `--worktree current` | Opus 5 | `parfait/plans/YYYY-MM-DD-<topic>.md` |
| W4 plan-reviewer | `--worktree current` | Opus 5 | findings(파일 없음) |

네 워커는 순차 실행한다. 병렬 이득이 없고, 뒤 워커가 앞 산출물을 입력으로 받는다.
```

- [x] **Step 3: W1 task spec 템플릿 추가**

```markdown
### W1 analyst

```bash
orca orchestration task-create --spec "$(cat <<'SPEC'
[역할] 요구사항 분석 + 설계. 산출물은 설계 스펙 문서 하나다.

[읽어라]
- 코드 대상: <TJYG-Android 절대경로 — wiki/personal-private/project-paths.md 참조. 코디네이터가
  §0에서 이 문서를 읽어 실제 경로로 채워 넣은 뒤 워커에게 전달한다>
- 규약: <team-yg>/CLAUDE.md, <team-yg>/parfait/index.md, <team-yg>/parfait/specs/README.md
- 형식: <team-yg>/parfait/specs/template.md
- 기존 결정: <team-yg>/parfait/adr/, <team-yg>/parfait/architecture/
- 정책 SoT: <team-yg>/wiki/index.md에서 관련 페이지만

[해라]
1. superpowers:brainstorming 스킬을 로드한다.
2. 주제와 관련된 벤더 스킬을 먼저 찾는다:
   python3 <team-yg>/parfait/script/search.py "<주제>"
   상위 후보 중 관련 스킬을 네이티브 Skill로 로드한 뒤 설계를 확정한다.
3. 설계 스펙을 <team-yg>/parfait/specs/YYYY-MM-DD-<kebab-topic>.md에 쓴다.
   형식은 template.md, frontmatter 필수. 라인번호·hex·변동수치는 적지 않는다.
4. parfait/specs/README.md 인덱스에 한 줄 등록한다.

[금지]
- TJYG-Android 코드 수정. 이 단계는 읽기만 한다.
- placeholder("TBD", "추후 결정", 빈 섹션). 결정할 수 없으면 열린 질문 절에 근거와 함께 적는다.
- wiki/ 파일 수정.

[요구사항 원문]
<사용자 원문 그대로>

[보고]
worker_done --outcome succeeded --files-modified "<스펙 경로>,<README 경로>"
body에 스펙 절대경로와 핵심 설계 결정 3~5줄 요약.
SPEC
)" --json
```

기동:

```bash
orca orchestration worker-start --task <task_id> --worktree current --agent claude --json
```
```

- [x] **Step 4: W2 task spec 템플릿과 반려 루프 추가**

```markdown
### W2 spec-reviewer

리뷰어에게는 **요구사항 원문과 스펙 파일만** 준다. W1의 대화 컨텍스트는 주지 않는다.
그래야 "문서만 읽고 이해되는가"가 실제로 검증된다.

```bash
orca orchestration task-create --spec "$(cat <<'SPEC'
[역할] 설계 스펙 독립 검수. 너는 이 스펙을 쓰지 않았고, 작성 과정도 모른다.

[입력]
- 요구사항 원문: 아래
- 검수 대상: <스펙 절대경로>
- 대조 대상: <team-yg>/parfait/adr/, <team-yg>/parfait/architecture/, <team-yg>/wiki/

[반려 사유 — 하나라도 해당하면 반려]
1. 요구사항 원문의 항목 중 스펙에 담기지 않은 것
2. placeholder·TBD·"추후 결정"
3. 내부 모순 (아키텍처 서술 vs 기능 서술)
4. 두 가지로 읽히는 요구사항
5. parfait/adr/ 또는 parfait/architecture/의 기존 결정과 상충

[에스컬레이션 — 반려하지 말고 escalation]
- wiki/ 정책과 스펙이 상충. 기획 자체의 미결일 수 있어 에이전트가 판정하지 않는다.

[금지]
- 파일 수정. 너는 findings만 반환한다.
- 스타일 지적(문장 다듬기, 표 정렬). 위 5개 사유에만 집중한다.

[요구사항 원문]
<사용자 원문 그대로>

[보고]
통과: worker_done --outcome succeeded, body 첫 줄에 "PASS"
반려: worker_done --outcome succeeded, body 첫 줄에 "REJECT", 이어서 사유별로
      "사유번호 / 스펙의 어느 절 / 무엇이 빠졌거나 어긋나는지"를 한 건씩.
SPEC
)" --json
```

**반려 루프**: `REJECT`를 받으면 findings를 W1에게 회신한다.

```bash
orca orchestration send --to dispatch:<W1_dispatch_id> --subject "스펙 리뷰 반려" --body "<findings 원문>" --json
```

W1이 수정해 `worker_done`을 다시 보내면 W2를 같은 방식으로 다시 띄운다.
**상한 2회.** 3회째 반려면 escalation으로 사람을 부른다 —
리뷰어와 작성자가 합의하지 못하는 상태이고 자동으로 풀리지 않는다.
```

- [x] **Step 5: G1 게이트 처리 추가**

```markdown
### G1 — 스펙 승인

`자동진행` 모드면 건너뛴다. 아니면:

```bash
orca orchestration gate-create --task <spec_task_id> \
  --question "스펙 검토 요청: <스펙 절대경로>. 구현 계획 단계로 넘어갈까?" \
  --options '["진행","수정 요청","중단"]' --json
orca orchestration check --wait --types decision_gate,question --timeout-ms 900000 --json
```

- `진행` → W3
- `수정 요청` → 사용자 지적을 W1에게 회신하고 다시 W2 검수부터
- `중단` → Run을 남긴 채 종료하고, 재개 방법을 사용자에게 알린다

타임아웃은 실패가 아니다. 사람이 아직 안 봤을 뿐이므로 계속 기다린다.
```

- [x] **Step 6: W3·W4·G2 추가**

```markdown
### W3 planner

```bash
orca orchestration task-create --spec "$(cat <<'SPEC'
[역할] 구현 계획 작성. 파일 선택 + 모듈 분할 + 테스트 명세 + 모델 지정.

[읽어라]
- 확정 스펙: <스펙 절대경로>
- 코드 대상: <TJYG-Android 절대경로 — wiki/personal-private/project-paths.md 참조. 코디네이터가
  §0에서 이 문서를 읽어 실제 경로로 채워 넣은 뒤 워커에게 전달한다>
- 형식: <team-yg>/parfait/plans/template.md, 규약: parfait/plans/README.md

[해라]
1. superpowers:writing-plans 스킬을 로드한다.
2. 주제 관련 벤더 스킬을 먼저 찾는다:
   python3 <team-yg>/parfait/script/search.py "<주제>"
3. 변경이 필요한 파일을 전부 나열하고 **Gradle 모듈 경계로 묶는다.**
   모듈 간 파일 집합이 겹치면 안 된다. 겹치면 분할을 다시 한다.
4. 모듈별로 아래를 확정한다:
   - 담당 파일 화이트리스트(절대경로)
   - 테스트 명세: 무엇을 검증하고 기대값이 무엇인지. 실행 가능한 형태로.
   - Gradle 테스트 태스크(예: :domain:test)
   - model: opus | sonnet — 근거 한 줄과 함께
     · opus — Compose UI·recomposition, 코루틴 동시성, 기존 코드 대수술, 파일 6개 이상
     · sonnet — 시그니처까지 특정된 신규 파일, DTO·매퍼·Repository 같은 정형 작업
   - 의존 관계(기본형: domain → data, domain → feature)
5. 계획을 <team-yg>/parfait/plans/YYYY-MM-DD-<kebab-topic>.md에 쓰고
   parfait/plans/README.md 인덱스에 한 줄 등록한다.

[판단]
- 파일이 한 모듈 안에서 많이 겹쳐 병렬 이득이 없으면 **단일 워커로 결정**해도 된다.
  그 판단과 근거를 계획서에 적는다.

[금지]
- TJYG-Android 코드 수정.
- placeholder. "적절히 처리", "테스트 추가" 같은 서술.

[보고]
worker_done --outcome succeeded --files-modified "<계획 경로>,<README 경로>"
body에 모듈 분할 결과(모듈 / 파일 수 / model / 의존)를 표로.
SPEC
)" --json
```

### W4 plan-reviewer

W2와 같은 구조다. 리뷰어에게는 **스펙 파일과 계획 파일만** 준다.

[반려 사유]
1. 스펙 항목 중 계획에 잡히지 않은 것
2. **모듈 간 파일 집합 겹침** — merge 충돌 예고이므로 반려
3. 테스트 명세가 실행 가능한 형태가 아님(검증 대상·기대값 미특정)
4. 의존 순서가 실제 Gradle 모듈 의존과 어긋남
5. task 하나의 크기가 과도함
6. `model:` 필드 누락 또는 근거 없음

균형 프로필에서 모델은 Opus 5다. 검사 항목이 명시적 체크리스트라 낮출 만해 보이지만,
작성자 W3가 Opus라 "리뷰어는 작성자와 같은 티어 이상" 규칙이 이긴다.
반려 루프와 상한 2회는 W2와 동일하다.

### G2 — 계획 승인

G1과 같은 방식. `자동진행`이면 건너뛴다.
```

- [x] **Step 7: 절 구성과 명령 실재 검증**

```bash
grep -n "^## 3\. 문서 단계" .claude/skills/start-orchestration-session/SKILL.md
grep -c "^### " .claude/skills/start-orchestration-session/SKILL.md
grep -n "TBD\|FIXME" .claude/skills/start-orchestration-session/SKILL.md
orca orchestration task-create --help >/dev/null 2>&1 && echo "OK task-create"
orca orchestration gate-create --help >/dev/null 2>&1 && echo "OK gate-create"
orca orchestration send --help 2>&1 | grep -q -- "--to" && echo "OK send --to"
orca orchestration check --help 2>&1 | grep -q -- "--wait" && echo "OK check --wait"
```

기대: `OK` 4줄.

`### ` 개수는 **7**이다 — 이 Task가 넣는 6개(W1 / W2 / G1 / W3 / W4 / G2)에 Task 2가 이미 넣은
`### 프로필`이 더해진다. 새로 붙인 절만 떼어 세면 6이다.

placeholder grep은 **2건 매치가 정상**이다. W1·W3의 task spec 템플릿에 있는 금지 문구
(`placeholder("TBD", "추후 결정", 빈 섹션)`)가 걸리는 것이고, 실제 placeholder가 아니다.
매치된 줄이 그 두 건인지 눈으로 확인하고, 다른 줄이 걸리면 그것이 진짜 결함이다.

- [x] **Step 8: 커밋 (로컬 커밋이라 건별 확인 없이 진행 — 사용자가 Task 1~6 커밋을 일괄 사전 승인함)**

```bash
git add .claude/skills/start-orchestration-session/SKILL.md
git commit -m "feat(skills): 문서 단계(W1~W4)와 게이트 G1·G2 추가"
```

---

### Task 4: 구현 단계 — 모듈 worktree와 TDD 계약

**Files:**
- Modify: `.claude/skills/start-orchestration-session/SKILL.md` (`## 4. 구현 단계` 절 추가)

**Interfaces:**
- Consumes: Task 3의 계획 문서 형식(모듈별 담당 파일 화이트리스트, 테스트 명세, `model:` 필드, 의존 관계)
- Produces: `## 4. 구현 단계` 절 — 자식 worktree 생성 명령, 모델 지정 2단계 경로, 구현 워커 task spec 템플릿, RED/GREEN 증거 검사 규칙. Task 5가 여기서 만든 모듈 브랜치들을 병합한다.

- [x] **Step 1: `## 4. 구현 단계` 도입부와 의존 표현 추가**

```markdown
## 4. 구현 단계

계획서가 나눈 모듈마다 자식 worktree를 하나씩 만들고 워커를 붙인다.

의존 관계는 `task-create --deps`로 표현한다. 기본형은 domain → (data, feature)다.
인터페이스가 domain에 있으므로 domain이 끝나야 나머지 둘이 병렬로 시작한다.
계획서가 domain 변경 없음이라고 판단했으면 세 모듈이 모두 동시에 출발한다.

```bash
DOMAIN_TASK=$(orca orchestration task-create --spec "<domain task spec>" --json | jq -r '.result.task.id')
orca orchestration task-create --spec "<data task spec>"    --deps "[\"$DOMAIN_TASK\"]" --json
orca orchestration task-create --spec "<feature task spec>" --deps "[\"$DOMAIN_TASK\"]" --json
```

⚠️ **`task-create --json`의 응답 스키마는 Orca 가이드에 문서화돼 있지 않다.** 위 `jq` 경로는
가정이므로 첫 실행에서 실제 출력으로 확인한다. 경로가 틀리면 `DOMAIN_TASK`가 빈 문자열이 되고
`--deps '[""]'`가 들어가 **의존이 조용히 깨진다**(에러 없이 순서만 무너진다).

```bash
orca orchestration task-create --spec "<domain task spec>" --json    # 출력 전체를 눈으로 본다
orca orchestration task-list --brief --json                          # id 필드 위치 확인
```

id를 뽑은 뒤 `[ -n "$DOMAIN_TASK" ]`로 비어 있지 않은지 확인하고 나머지 task를 만든다.
```

- [x] **Step 2: 워커 기동 — 모델 지정 2단계 경로 추가**

```markdown
### 워커 기동

`worker-start --agent claude`는 **모델을 지정하지 못한다.** 계획서가 정한 모델을 쓰려면
worktree와 터미널을 먼저 만들고 그 터미널에 task를 붙인다.

```bash
# 1) 자식 worktree 생성
orca worktree create --name wt-<module> --repo name:TJYG-Android \
  --base-branch <feature/xxxxx-master> --json

# 2) 모델을 지정해 에이전트 터미널 생성
orca terminal create --worktree name:wt-<module> \
  --command "claude --model <opus|sonnet>" --json

# 3) 그 터미널에 task를 붙인다 (orchestration 계보 유지)
orca orchestration worker-start --task <task_id> --terminal <handle> --json
```

`--terminal`로 붙여도 task·dispatch 계보, `worker_done` 권한, 주입 프리앰블은 그대로다.

이 경로는 repo의 `wait-for-setup` 정책을 강제하지 못한다. §0에서 기록해 둔 값이
`start-immediately`면 잃는 것이 없다. `wait-for-setup`이면 모델 지정을 포기하고
아래 한 줄로 간다.

```bash
orca orchestration worker-start --task <task_id> --worktree new-child --name wt-<module> --agent claude --setup run --json
```

세 모듈이 동시에 돌면 Gradle 데몬이 최대 3개, `build/`가 3벌이 된다. `~/.gradle`은 공유라
동시 쓰기 잠금 경합이 생길 수 있다. 락 대기나 비정상적인 지연이 보이면 그때 모듈 워커를
줄인다. 미리 튜닝하지 않는다.
```

- [x] **Step 3: 구현 워커 task spec 템플릿 추가**

```markdown
### 구현 워커 task spec

```bash
orca orchestration task-create --spec "$(cat <<'SPEC'
[역할] <module> 모듈 TDD 구현.

[입력]
- 계획 문서: <계획 절대경로>
- 담당 모듈: <module>
- 담당 파일 화이트리스트:
    <파일 절대경로 목록>
- 테스트 명세:
    <검증 대상과 기대값>
- 테스트 태스크: ./gradlew :<module>:test

[순서 — 이 순서를 지켜야 한다]
1. 테스트를 먼저 쓴다.
2. ./gradlew :<module>:test 를 실행한다. RED여야 한다. 실패 출력을 그대로 보관한다.
   여기서 GREEN이면 테스트가 잘못된 것이다. 다시 쓴다.
3. 구현한다.
4. ./gradlew :<module>:test 를 다시 실행한다. GREEN이어야 한다. 출력을 그대로 보관한다.
5. 커밋한다. repo 관례를 따라 test: 와 feat: 를 나눈다.

[금지]
- 담당 파일 화이트리스트 밖 수정. 필요하면 escalation을 보낸다.
- 구현을 먼저 하고 테스트를 나중에 붙이는 것.
- RED 확인 없이 3단계로 건너뛰는 것.

[보고]
worker_done --outcome succeeded --files-modified "<수정 파일 목록>"
body에 다음 둘을 **모두** 넣는다:
  - RED 로그: 2단계 실행 출력 중 실패를 보여주는 부분
  - GREEN 로그: 4단계 실행 출력 중 통과를 보여주는 부분
실패했으면 --outcome failed 를 쓴다. 산문으로만 실패를 적지 않는다.
SPEC
)" --json
```
```

- [x] **Step 4: RED/GREEN 증거 검사 규칙 추가**

```markdown
### 증거 검사

`worker_done`을 받으면 코디네이터가 body를 확인한다.

| 확인 | 통과 조건 |
|---|---|
| RED 로그 | 2단계 실행 결과에 실패가 보인다 |
| GREEN 로그 | 4단계 실행 결과에 통과가 보인다 |
| 파일 범위 | `--files-modified`가 화이트리스트를 벗어나지 않는다 |

하나라도 없으면 **반려**한다.

```bash
orca orchestration send --to dispatch:<dispatch_id> --subject "증거 누락" \
  --body "RED 로그와 GREEN 로그를 모두 첨부해 다시 보고하라. 없으면 이 task는 완료로 인정하지 않는다." --json
```

이 검사를 강제하는 이유는, 로그 두 개가 TDD를 지켰다는 **유일하게 검증 가능한 흔적**이기
때문이다. 없으면 구현을 먼저 하고 통과하는 테스트를 나중에 붙인 것과 구분할 수 없다.

GREEN에 도달하지 못했으면 같은 dispatch에 회신해 1회 재시도시킨다.
재시도는 **한 티어 올린 모델**로 한다 — 같은 모델에 같은 프롬프트를 다시 넣으면 같은
결과가 나온다. 그래도 실패하면 escalation.
```

- [x] **Step 5: 대기 루프 추가**

```markdown
### 대기

```bash
orca orchestration check --wait --types worker_done,escalation,question --timeout-ms 900000 --json
orca orchestration check --ack <delivery_id> --wait --types worker_done,escalation,question --timeout-ms 900000 --json
```

Delivery 안의 메시지를 **전부 처리한 뒤** ack한다.
타임아웃이나 `{count:0}`은 실패가 아니라 체크포인트다. 구현 task는 보통 오래 걸린다.
워커를 죽이거나 재시작하지 않는다. 하트비트와 터미널 활동은 살아 있다는 뜻이지 끝났다는
뜻이 아니다.
```

- [x] **Step 6: 검증**

```bash
grep -n "^## 4\. 구현 단계" .claude/skills/start-orchestration-session/SKILL.md
grep -q "RED 로그" .claude/skills/start-orchestration-session/SKILL.md && echo "OK: RED evidence rule present"
grep -q "한 티어 올린" .claude/skills/start-orchestration-session/SKILL.md && echo "OK: escalation tier rule present"
orca terminal create --help 2>&1 | grep -q -- "--command" && echo "OK terminal create --command"
orca orchestration worker-start --help 2>&1 | grep -q -- "--terminal" && echo "OK worker-start --terminal"
orca orchestration task-create --help 2>&1 | grep -q -- "--deps" && echo "OK task-create --deps"
```

기대: `OK` 5줄.

- [x] **Step 7: 커밋 (로컬 커밋이라 건별 확인 없이 진행 — 사용자가 Task 1~6 커밋을 일괄 사전 승인함)**

```bash
git add .claude/skills/start-orchestration-session/SKILL.md
git commit -m "feat(skills): 구현 단계 — 모듈 worktree·모델 지정 경로·RED/GREEN 증거 계약"
```

---

### Task 5: 통합·코드 리뷰·최종 산출·에스컬레이션

**Files:**
- Modify: `.claude/skills/start-orchestration-session/SKILL.md` (`## 5. 통합과 코드 리뷰`, `## 6. 최종 산출`, `## 7. 에스컬레이션` 절 추가)

**Interfaces:**
- Consumes: Task 4가 만든 모듈 브랜치들(`wt-<module>`)과 그 커밋
- Produces: 파이프라인 종료 절차. 이 Task 이후 스킬 문서는 기능적으로 완결된다.

- [x] **Step 1: `## 5. 통합과 코드 리뷰` 절 추가**

```markdown
## 5. 통합과 코드 리뷰

모듈 워커가 전부 `worker_done`을 보내고 증거 검사를 통과하면 통합 worktree를 만든다.

```bash
orca worktree create --name wt-integrate --repo name:TJYG-Android \
  --base-branch <feature/xxxxx-master> --json
orca terminal create --worktree name:wt-integrate --command "claude --model sonnet" --json
orca orchestration worker-start --task <integrate_task_id> --terminal <handle> --json
```

### integrator task spec

```bash
orca orchestration task-create --spec "$(cat <<'SPEC'
[역할] 모듈 브랜치 병합 + 전체 테스트.

[해라]
1. 아래 브랜치를 순서대로 merge 한다: <wt-domain 브랜치>, <wt-data 브랜치>, <wt-feature 브랜치>
2. 충돌이 나면 **직접 해결하지 말고 escalation을 보낸다.**
   모듈 경계로 나눴는데 충돌했다는 것은 분할이 잘못됐다는 신호이고,
   여기서 임의로 봉합하면 그 신호가 사라진다.
3. ./gradlew test 로 전체 테스트를 돌린다. GREEN이어야 한다.
4. 커밋한다.

[보고]
worker_done --outcome succeeded --files-modified "<병합 결과 변경 파일>"
body에 전체 테스트 출력의 결과 부분과 병합한 브랜치 목록.
SPEC
)" --json
```

### code-reviewer

같은 worktree에 **별도 터미널**로 띄운다. 리뷰어는 파일을 고치지 않는다.

```bash
orca terminal create --worktree name:wt-integrate --command "claude --model opus" --json
orca orchestration worker-start --task <review_task_id> --terminal <handle> --json
```

```bash
orca orchestration task-create --spec "$(cat <<'SPEC'
[역할] 통합 diff 코드 리뷰. 너는 이 코드를 쓰지 않았다.

[입력]
- 스펙: <스펙 절대경로>
- 계획: <계획 절대경로>
- 대상: git diff <feature/xxxxx-master>...HEAD

[보는 것]
- 스펙 요구 중 구현되지 않은 것
- 모듈 간 인터페이스 불일치 (모듈별 리뷰로는 안 보이는 것 — 여기가 핵심)
- 테스트가 실제로 명세한 동작을 검증하는가 (통과만 하는 빈 테스트가 아닌가)
- 계획의 담당 파일 화이트리스트를 벗어난 변경
- repo 관례 이탈 (parfait/architecture/ 참조)

[금지]
- 파일 수정. findings만 반환한다.
- 스타일 지적. ktlint가 잡는 것은 적지 않는다.

[보고]
worker_done --outcome succeeded
body 첫 줄에 "PASS" 또는 "FINDINGS", FINDINGS면 건별로
"심각도(Critical|Important|Minor) / 파일#심볼 / 무엇이 문제인지 / 어떻게 고칠지".
SPEC
)" --json
```

`FINDINGS`를 받으면 integrator에게 회신해 수정시킨다.

```bash
orca orchestration send --to dispatch:<integrator_dispatch_id> --subject "코드 리뷰 findings" --body "<findings 원문>" --json
```

수정 후 다시 리뷰. **상한 2회.** 3회째면 escalation.
```

- [x] **Step 2: `## 6. 최종 산출` 절 추가**

```markdown
## 6. 최종 산출

통합 worktree에서 diff를 뽑아 master 작업 트리에 적용한다. **커밋하지 않는다.**

```bash
# wt-integrate에서
git diff <feature/xxxxx-master>...HEAD > <scratchpad>/final.patch

# master worktree에서
git apply <scratchpad>/final.patch
git status --short
```

`git apply`가 실패하면 master 브랜치가 그 사이 앞으로 나갔다는 뜻이다.
덮어쓰지 말고 사용자에게 보고한다.

### G3 — 최종 보고

`자동진행` 모드에서도 **이 게이트는 건너뛰지 않는다.** 보고 항목:

- 변경 파일 목록 (`git status --short` 출력)
- 전체 테스트 결과
- 코드 리뷰 findings와 처리 내역 (해소 / 이월 / 반박)
- 스펙·계획 문서 경로
- 자식 브랜치 이름들

`push`와 PR 생성은 사용자 승인 후에만 한다.

이 방식으로 만들어지는 PR 히스토리는 사람이 만든 커밋 하나다.
자식 worktree의 커밋들은 PR에 올라가지 않고 로컬 브랜치에만 남는다.

### 정리

**자식 worktree는 보고 후에도 유지한다.** 사용자가 중간 산출물과 커밋 히스토리를 볼 수
있어야 하고, 재수정 요청이 오면 거기서 이어간다.
사용자가 명시적으로 정리를 요청할 때만 archive한다.

### 커밋 정책

기본 규칙은 "TJYG-Android는 구현이 끝나도 커밋하지 않는다"이다.
이 파이프라인에 한해 **자식 worktree 브랜치의 커밋은 병합 수단으로 허용**한다.
master 브랜치는 커밋하지 않고, push와 PR은 사용자 승인을 받는다.
```

- [x] **Step 3: `## 7. 에스컬레이션` 절 추가**

```markdown
## 7. 에스컬레이션

| 상황 | 동작 |
|---|---|
| `worker-start` 실패 | receipt의 `stage`·`effects`·`residualResources`를 읽고 판단. `--retry-of <dispatch_id>`로 1회만 재시도. 자동 무한 재시도 금지 |
| RED 또는 GREEN 로그 누락 | 반려하고 같은 dispatch에 재실행 요구 |
| GREEN 미달성 | 한 티어 올린 모델로 1회 재시도 → 실패 시 사람 호출 |
| 문서 리뷰 반려 3회째 | 사람 호출 |
| 코드 리뷰 findings 3회째 | 사람 호출 |
| merge 충돌 | 즉시 사람 호출. 분할이 잘못됐다는 신호이므로 계획 단계로 되돌아간다 |
| 같은 task 3연속 실패 | Orca가 circuit-break하여 task failed. 사람에게 보고하고 중단 |
| `wiki/` 정책과 스펙 상충 | 사람 호출. 기획 미결일 수 있어 에이전트가 판정하지 않는다 |
| `git apply` 실패 | 사람 호출. 덮어쓰지 않는다 |

## 진행 상황 확인

```bash
orca orchestration task-list --brief --json
orca orchestration dispatch-show --task <task_id> --json
orca orchestration worker-read --dispatch <dispatch_id> --limit 50 --json
```
```

- [x] **Step 4: 검증**

```bash
grep -c "^## " .claude/skills/start-orchestration-session/SKILL.md
grep -q "커밋하지 않는다" .claude/skills/start-orchestration-session/SKILL.md && echo "OK: no-commit rule present"
grep -q "직접 해결하지 말고 escalation" .claude/skills/start-orchestration-session/SKILL.md && echo "OK: conflict escalation present"
grep -n "TBD\|FIXME" .claude/skills/start-orchestration-session/SKILL.md
orca orchestration worker-read --help >/dev/null 2>&1 && echo "OK worker-read"
orca orchestration dispatch-show --help >/dev/null 2>&1 && echo "OK dispatch-show"
```

기대: `## ` 절 개수 **11**(이 스킬을 쓰지 않는 경우 / 용어 / 0. 전제 확인 / 1. 요구사항 수집 /
2. Run 생성 / 3. 문서 단계 / 4. 구현 단계 / 5. 통합과 코드 리뷰 / 6. 최종 산출 /
7. 에스컬레이션 / 진행 상황 확인), `OK` 4줄.
절 개수가 다르면 빠진 절을 찾아 보충한다.

placeholder grep은 Task 3이 남긴 금지 문구 2건이 계속 걸린다(실제 placeholder 아님).
그 2건 외의 매치만 결함으로 본다.

- [x] **Step 5: 커밋 (로컬 커밋이라 건별 확인 없이 진행 — 사용자가 Task 1~6 커밋을 일괄 사전 승인함)**

```bash
git add .claude/skills/start-orchestration-session/SKILL.md
git commit -m "feat(skills): 통합·코드 리뷰·최종 산출·에스컬레이션 절 추가"
```

---

### Task 6: CLAUDE.md 라우팅과 문서 정합

**Files:**
- Modify: `CLAUDE.md` (작업 유형별 워크플로 라우팅 절)
- Modify: `parfait/specs/2026-08-05-orchestration-session-pipeline.md` (frontmatter `status`)
- Modify: `parfait/plans/README.md` (활성 계획 인덱스)

**Interfaces:**
- Consumes: Task 1의 `start-default-session`, Task 2~5의 `start-orchestration-session`
- Produces: 없음(마지막 Task)

- [x] **Step 1: CLAUDE.md 라우팅에 진입점 추가**

`## 작업 유형별 워크플로 라우팅 (필수)` 절의 도입부 아래, `### A.` 앞에 다음을 넣는다.

```markdown
> **세션 진입점 2종** — 온보딩만 필요하면 `start-default-session`.
> 요구사항 하나를 다수 에이전트가 나눠 처리하는 파이프라인(분석·설계·스펙리뷰·계획·계획리뷰·
> TDD 구현 병렬·통합·코드리뷰)을 돌릴 때는 `start-orchestration-session`.
> 파이프라인 설계 정본은 [`parfait/specs/2026-08-05-orchestration-session-pipeline.md`](parfait/specs/2026-08-05-orchestration-session-pipeline.md).
```

- [x] **Step 2: 스펙 status를 in-progress로 갱신**

`parfait/specs/2026-08-05-orchestration-session-pipeline.md`의 frontmatter에서
`status: draft`를 `status: in-progress`로 바꾼다. 파이프라인을 실제로 한 번 돌려
동작을 확인한 뒤에야 `implemented`가 된다.

- [x] **Step 3: plans README 활성 인덱스에 등록**

`parfait/plans/README.md`의 활성 카탈로그 테이블에서 `_(없음 — 진행 중인 계획 없음)_` 줄을
아래로 교체한다.

```markdown
| [2026-08-05-orchestration-session-pipeline.md](2026-08-05-orchestration-session-pipeline.md) | 오케스트레이션 파이프라인 스킬 구현(6 Task, **TJYG-Android 코드 변경 0**): `start-session` → `start-default-session` 개명 → `start-orchestration-session` 골격(전제 확인·요구사항 수집·Run 생성) → 문서 단계 W1~W4 + 게이트 G1·G2 → 구현 단계(모듈 worktree·모델 지정 2단계 경로·RED/GREEN 증거 계약) → 통합·코드리뷰·최종 산출·에스컬레이션 → CLAUDE.md 라우팅. 산출물은 마크다운 스킬 문서 2개뿐이라 자동 테스트가 없고, 검증은 **frontmatter `name`↔디렉토리명 일치 · placeholder grep · 문서에 적은 orca 명령·플래그가 `--help`에 실재하는지 대조**로 한다. 스펙: [specs](../specs/2026-08-05-orchestration-session-pipeline.md) |
```

- [x] **Step 4: 전체 정합 검증**

```bash
# 스킬 2종의 name과 디렉토리명 일치
for d in start-default-session start-orchestration-session; do
  n=$(grep -m1 "^name: " .claude/skills/$d/SKILL.md | sed 's/^name: //')
  [ "$n" = "$d" ] && echo "OK: $d" || echo "FAIL: $d has name=$n"
done

# 낡은 참조 잔존 확인 (아카이브 스펙 제외)
grep -rn "start-session" --include="*.md" . | grep -v "start-default-session" | grep -v "start-orchestration-session" | grep -v "parfait/specs/archive/"

# 링크 대상 실재
test -f parfait/specs/2026-08-05-orchestration-session-pipeline.md && echo "OK: spec exists"
test -f parfait/plans/2026-08-05-orchestration-session-pipeline.md && echo "OK: plan exists"

# CLAUDE.md 라우팅 반영
grep -q "start-orchestration-session" CLAUDE.md && echo "OK: CLAUDE.md routing"

# placeholder 잔존
grep -rn "TBD\|FIXME" .claude/skills/start-orchestration-session/SKILL.md parfait/specs/2026-08-05-orchestration-session-pipeline.md
```

기대: `OK` 5줄, 낡은 참조 grep은 **출력 없음**.
(단, 이 계획서와 스펙에는 개명 작업을 서술하는 `start-session` 문장이 남아 있다.
Task 1 Step 1의 주의대로 **정상 매치**이며, 실제 결함은 `.claude/skills/` 아래 잔존뿐이다.)
마지막 placeholder grep은 Task 3의 금지 문구 2건이 걸리는 것이 정상이고, 그 외 매치가 결함이다.

- [x] **Step 5: 민감정보 점검**

스킬 문서에 절대경로가 들어간다. 이 repo는 public이므로 확인한다.

```bash
grep -rn "/Users/" .claude/skills/start-orchestration-session/SKILL.md
```

출력이 있으면 그 줄들을 `<TJYG-Android 절대경로 — wiki/personal-private/project-paths.md 참조>`
형태의 플레이스홀더로 바꾸고, 스킬이 실행 시점에 `wiki/personal-private/project-paths.md`를
읽어 경로를 얻도록 §0 전제 확인에 한 줄 추가한다.

```markdown
로컬 절대경로는 `wiki/personal-private/project-paths.md`에서 읽는다.
이 문서에 직접 적지 않는다(public repo).
```

- [x] **Step 6: 커밋 (로컬 커밋이라 건별 확인 없이 진행 — 사용자가 Task 1~6 커밋을 일괄 사전 승인함)**

```bash
git add CLAUDE.md parfait/specs/2026-08-05-orchestration-session-pipeline.md parfait/plans/
git commit -m "docs: 오케스트레이션 파이프라인 라우팅·스펙 상태·계획 인덱스 반영"
```

- [x] **Step 7: 사용자에게 다음 단계 보고**

보고 내용:
- 만들어진 스킬 2종과 각 역할
- 파이프라인을 실제로 한 번 돌려봐야 `status: implemented`가 된다는 것
- 첫 실행 때 관측할 것 두 가지: worktree 3개 동시 Gradle 데몬 경합, 문서 워커의 cross-repo 쓰기
- PR 생성 여부 확인

---

## 검증 전략

이 계획의 산출물은 마크다운 문서라 단위 테스트가 없다. 대신 **기계적으로 판정 가능한 검사**를
Task마다 둔다.

| 검사 | 방법 | 왜 |
|---|---|---|
| 스킬 로드 가능성 | frontmatter `name` ↔ 디렉토리명 일치 | 불일치하면 스킬이 로드되지 않는다 |
| placeholder 부재 | `grep "TBD\|TODO\|FIXME"` | 계획 실패의 대표 징후 |
| CLI 명령 실재 | 문서에 적은 각 `orca` 명령을 `--help`로 확인 | Orca 릴리스 간 문법이 바뀐다. 문서에 죽은 명령을 남기면 파이프라인이 첫 실행에서 멈춘다 |
| 낡은 참조 부재 | `grep "start-session"` (아카이브 제외) | 개명 누락 탐지 |
| 민감정보 | `grep "/Users/"` | public repo |

**하지 않는 검증**: `run-create`·`task-create`·`worker-start` 실제 실행. 상태를 만드는 명령이고,
파이프라인 첫 실행은 실제 요구사항으로 하는 것이 맞다.
