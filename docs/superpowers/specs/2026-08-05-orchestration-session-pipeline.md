---
id: orchestration-session-pipeline
title: 요구사항 → 구현 오케스트레이션 파이프라인 (Orca orchestration + git worktree)
status: in-progress
category: tooling-spec
platforms: android
verified: 2026-08-05
related_code: .claude/skills/start-orchestration-session/SKILL.md, .claude/skills/start-default-session/SKILL.md
related_adr:
related_spec: vendor-android-kotlin-skills
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, tooling, orchestration]
---

# Spec: 요구사항 → 구현 오케스트레이션 파이프라인

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 목표

TJYG-Android 기능 요구사항 하나를 사람이 던지면, Orca orchestration과 git worktree로
분석·설계·계획·TDD 구현·리뷰까지 다수 에이전트가 나눠 수행하고, 최종 변경만 사람이 있는
브랜치로 돌려주는 파이프라인을 만든다. 진입점은 새 스킬 `start-orchestration-session`.

기존 `start-session`(온보딩)은 `start-default-session`으로 이름을 바꿔 두 진입점을 구분한다.
이름만 바꾸고 내용은 그대로 둔다.

이 파이프라인이 동시에 만족해야 하는 것 네 가지:

- **자율성** — 요구사항을 넣으면 리뷰·재수정까지 알아서 돈다. 사람 게이트는 최소.
- **병렬 속도** — 구현을 Gradle 모듈 단위로 쪼개 worktree에서 동시에 진행한다.
- **품질** — TDD RED/GREEN과 문서 리뷰·코드 리뷰를 게이트로 강제한다.
- **가시성** — 단계 전환이 Orca 워크스페이스 카드에 남아 스크롤백을 안 읽어도 진행이 보인다.

## 범위

- 포함: 파이프라인 토폴로지, 워커별 계약, 게이트·에스컬레이션 정책, 모델 분배 기준,
  산출물 반환 방식, 스킬 파일 2종(`start-orchestration-session` 신설, `start-default-session` 개명).
- 제외: TJYG-Android 코드 변경 자체(이 파이프라인이 나중에 만들어낼 결과물),
  파이프라인 실행 자동화 스크립트(스킬 문서로만 규정하고 코디네이터가 CLI를 직접 호출),
  Gradle 빌드 튜닝(동시 실행 문제가 실제로 관측된 뒤에 대응).

## 전제 (확인 완료)

| 항목 | 확인 결과 |
|---|---|
| Orca 런타임 | 1.4.164, `orchestration.contract.v1` 보유 |
| orchestration 기능 | 활성(바인딩된 Run이 없다는 응답까지 정상 도달) |
| TJYG-Android | Orca repo로 등록됨. 현재 worktree는 `develop` 하나 |
| 설치된 에이전트 CLI | `claude`만. codex·gemini·opencode·droid·grok·cursor 없음 |
| `start-session` 참조처 | 자기 자신 2줄 + 아카이브 스펙 1건뿐 — 개명이 안전 |
| TJYG-Android·team-yg 시작 정책 | 둘 다 `setupAgentStartupPolicy: start-immediately`, setup 스크립트 비어 있음 → **모델 지정 2단계 경로 사용 가능** |

에이전트 CLI가 하나뿐이라 **리뷰어를 다른 벤더로 두는 독립성 확보는 불가능**하다.
Claude 모델 티어 안에서만 나눈다.

## 토폴로지

Run 하나가 요구사항 하나에 대응한다. 코디네이터는 TJYG-Android의 `feature/xxxxx-master`
worktree에서 도는 세션이고, 사람과 대화하는 유일한 지점이다.

```
feature/xxxxx-master   ← 사람 ↔ 코디네이터 (Run 바인딩, 게이트, 중계, 최종 보고)
   │
   ├─ (worktree 없음) W1 analyst        요구분석 + 설계 → parfait/specs/
   ├─ (worktree 없음) W2 spec-reviewer  스펙 독립 검수
   ├─ (worktree 없음) W3 planner        파일선택 + 모듈분할 + 테스트명세 → parfait/plans/
   ├─ (worktree 없음) W4 plan-reviewer  계획 독립 검수
   │
   ├─ wt-domain     테스트작성 → RED → 구현 → GREEN
   ├─ wt-data       〃  (deps: domain)
   ├─ wt-feature    〃  (deps: domain)
   │
   └─ wt-integrate  브랜치 merge + 전체 테스트 + 코드리뷰 + 재수정
          └─ 최종 diff를 master 작업 트리에 apply (커밋하지 않음) → 사람 보고
```

문서 단계(W1~W4)는 TJYG-Android 코드를 건드리지 않고 team-yg repo의 `parfait/` 아래에
절대경로로 쓴다. 그래서 worktree가 필요 없고, master worktree에
`terminal create --worktree current`로 터미널만 띄운 뒤 그 handle에 task를 붙인다
(모델 티어 지정이 필요하므로 — 아래 "실행 메커니즘"). Gradle 비용 0, 파일 충돌 0.

구현 단계만 자식 worktree를 만든다.

## 단계 매핑

사용자가 제시한 9단계를 워커 단위로 재배치한 결과와 그 근거:

| 원 단계 | 배치 | 근거 |
|---|---|---|
| 1 요구분석 + 2 설계 | W1 하나 | 분석 결과를 설계로 넘길 때 컨텍스트 유실이 가장 크다. 한 워커가 `superpowers:brainstorming`을 로드해 스펙까지 산출 |
| 3 파일 선택 + 4 테스트 명세 | W3 하나 | 파일 집합을 정한 주체가 테스트 명세도 쓴다. 분리하면 "이 파일에 왜 이 테스트인가"의 연결이 끊긴다 |
| 5 구현 + 6 RED 실행 | 모듈 워커에 통합 | RED를 구현 **앞으로** 옮긴다. 원안은 구현 뒤에 RED가 있어 신호 역할을 못 한다 |
| 7 리뷰 | wt-integrate | 통합 diff 전체를 봐야 인터페이스 불일치를 잡는다. 모듈별 리뷰로는 안 보인다 |
| 8 재수정 | wt-integrate | 리뷰어가 findings만 반환하고 통합 워커가 고친다 |
| 9 최종 반환 | 코디네이터 | 아래 "최종 산출" 참조 |

새로 추가된 단계는 문서 리뷰 두 개(W2, W4)다.

## 문서 리뷰 게이트

```
W1 analyst → spec 파일
   ↓
W2 spec-reviewer   ── findings ──→ W1 터미널 재-디스패치 → 수정 → 재리뷰 (최대 2회)
   ↓ pass
[G1: 코디네이터가 사람에게 직접 질의]
   ↓
W3 planner → plan 파일
   ↓
W4 plan-reviewer   ── findings ──→ W3 터미널 재-디스패치 → 수정 → 재리뷰 (최대 2회)
   ↓ pass
[G2: 코디네이터가 사람에게 직접 질의]
   ↓
구현 단계
```

**반려 전달은 mail이 아니라 재-디스패치다.** 워커는 `worker_done`을 보낸 뒤 턴을 끝내고
idle 상태로 대기하며 `orchestration check`를 더 이상 돌리지 않고, 그 `worker_done`이
task와 dispatch를 자동으로 `completed`로 만든다. 따라서 완료된 워커에게
`send --to dispatch:<id>`로 보낸 반려 지시는 읽히지 않고, 코디네이터는 오지 않을
`worker_done`을 기다리며 교착한다. 반려 라운드마다 **새 task를 만들어
`terminal wait --terminal <원 워커 터미널 handle> --for tui-idle --timeout-ms 60000` →
`dispatch --task <new> --to <원 워커 터미널 handle> --inject`로 같은 터미널을 다시 깨운다.**
`dispatch --inject` 앞에 idle 대기를 두는 이유는, 터미널이 아직 idle이 아닌 상태에서
주입하면 프롬프트가 유실될 수 있기 때문이다. 터미널이 같으므로 작성자의 컨텍스트는 살아
있고, 라운드마다 새 task가 생겨 반려 이력이 감사 흔적으로 남는다. 완료된 task를
`task-update`로 되살리지 않는다.

`send --to dispatch:<id>`는 **아직 돌고 있는(worker_done 전) 워커에게 주는 중간 안내**로만
쓴다. 이 구분이 이 파이프라인이 정상 경로에서 멈추지 않기 위한 전제다.

이 규칙의 따름정리로, 코디네이터는 각 워커의 **터미널 handle을 `worker_done` 이후에도
보관**해야 한다. handle을 잃으면 그 워커를 다시 깨울 주소가 없다.

리뷰어를 별도 워커로 두는 이유는 자기 문서를 자기가 리뷰하면 통과시키기 때문이다.
리뷰어에게는 **원본 요구사항과 산출 문서만** 준다. 작성 과정의 대화 컨텍스트는 주지 않는다.
그래야 "문서만 읽고 이해되는가"가 실제로 검증된다.

**스펙 반려 사유**

- 원 요구사항 중 스펙에 담기지 않은 항목
- placeholder·TBD·"추후 결정"
- 내부 모순(아키텍처 서술 vs 기능 서술)
- 두 가지로 읽히는 요구사항
- `parfait/adr/`·`parfait/architecture/`의 기존 결정과 상충

`wiki/`의 정책 문서와 상충하는 경우는 반려가 아니라 **사람 에스컬레이션**이다.
기획 자체의 미결일 수 있고, 그건 에이전트가 판정할 수 없다.

**계획 반려 사유**

- 스펙 항목 중 계획에 잡히지 않은 것
- 모듈 분할에서 **파일 집합이 겹침** — merge 충돌 예고이므로 반려
- 테스트 명세가 실행 가능한 형태가 아님(검증 대상과 기대값이 특정되지 않음)
- 의존 순서가 실제 Gradle 모듈 의존과 어긋남
- task 하나의 크기가 과도함

**반려 루프 상한은 2회.** 3회째는 `escalation`으로 사람을 부른다. 리뷰어와 작성자가
합의하지 못하는 상태이고, 자동으로 풀리지 않는다.

## 구현 워커 계약

모듈당 워커 하나. task spec에 다음을 그대로 담는다.

```
입력
  - plan 문서 절대경로
  - 담당 Gradle 모듈
  - 담당 파일 집합(화이트리스트)
  - 테스트 명세

순서
  1. 테스트 작성
  2. ./gradlew :<module>:test  → RED 확인, 실패 로그 캡처
     ※ 이 시점에 GREEN이면 테스트가 잘못된 것이므로 다시 작성
  3. 구현
  4. ./gradlew :<module>:test  → GREEN 확인, 로그 캡처
  5. 커밋 (repo 관례에 맞춰 test: / feat: 분리)

금지
  - 담당 파일 집합 밖 수정. 필요하면 escalation.

보고
  worker_done --outcome succeeded --files-modified <...>
  body에 RED 로그와 GREEN 로그를 모두 포함.
  둘 중 하나라도 없으면 코디네이터가 반려하고 재실행을 요구한다.
```

RED 로그를 강제하는 이유는, 그것이 없으면 워커가 구현을 먼저 하고 통과하는 테스트를
나중에 붙이는 것을 막을 수단이 없기 때문이다. 로그 두 개가 TDD를 지켰다는 유일하게
검증 가능한 흔적이다.

**의존 관계**는 `task-create --deps`로 표현한다. 기본형은 domain → (data, feature).
인터페이스가 domain에 있으므로 domain이 끝나야 나머지 둘이 병렬로 시작한다.
요구사항이 domain을 건드리지 않으면 세 모듈이 모두 동시에 출발한다.

**Gradle 동시 실행** — worktree 3개는 `build/` 3벌과 최대 3개의 데몬을 뜻한다.
`~/.gradle` 캐시는 공유되지만 동시 쓰기 잠금 경합이 생길 수 있다. 첫 실행에서 락 대기나
비정상적인 지연이 관측되면 그때 모듈 워커 수를 줄인다. 사전 튜닝은 하지 않는다.

## 통합 · 코드 리뷰

```
wt-integrate (new-child)
  ├─ integrator : wt-domain / wt-data / wt-feature 브랜치 merge
  │               → ./gradlew test 전체 GREEN 확인
  ├─ reviewer   : 같은 worktree에 별도 터미널, 통합 diff 리뷰
  │               findings → integrator 터미널 재-디스패치 → 수정 → 재리뷰 (최대 2회)
  └─ pass
```

리뷰어는 파일 수정 권한이 없다. findings만 반환한다. 리뷰와 수정을 같은 워커가 하면
자기 수정을 자기가 승인하는 구조가 된다.

findings 전달도 문서 리뷰와 같다 — integrator는 `worker_done` 이후 idle이고 dispatch는
완료 상태이므로 mail이 아니라 **새 task + `dispatch --inject`로 integrator 터미널을 다시
깨운다.** 재리뷰도 마찬가지로 reviewer 터미널을 재-디스패치한다.

## 최종 산출 (A안)

```
wt-integrate:  git diff feature/xxxxx-master...HEAD > <scratchpad>/final.patch
master:        git apply final.patch        ← 언스테이지 상태, 커밋하지 않음
```

코디네이터가 사람에게 보고하는 항목:

- 변경 파일 목록
- 전체 테스트 결과
- 코드 리뷰 findings와 그 처리 내역
- spec·plan 문서 경로
- 자식 브랜치 이름들

`push`와 PR 생성은 종전대로 사람 승인 후에만 한다.

이 방식으로 만들어지는 PR 히스토리는 사람이 만든 커밋 하나다. 자식 worktree의 커밋들은
PR에 올라가지 않고 로컬 브랜치에만 남는다. 파이프라인의 중간 이력을 PR에 남기지 않는다는
선택이고, 그 대가로 리뷰어가 보는 히스토리가 단순해진다.

**자식 worktree는 보고 후에도 유지한다.** 사람이 중간 산출물과 커밋 히스토리를 확인할 수
있어야 하고, 재수정 요청이 오면 거기서 이어가기 때문이다. 사람이 명시적으로 정리를
요청할 때 archive한다.

**커밋 정책 예외** — 기존 규칙은 "TJYG-Android는 구현이 끝나도 커밋하지 않는다"이다.
이 파이프라인에 한해 **자식 worktree 브랜치의 커밋은 병합 수단으로 허용**한다.
master 브랜치는 커밋하지 않고, push와 PR은 종전대로 사람 승인을 받는다.

## 모델 분배

고정 표보다 아래 세 기준이 정본이다.

1. **실패 비용은 상류일수록 크다.** 스펙이 틀리면 계획·구현·리뷰가 전부 폐기된다.
   구현 하나가 틀리면 그 모듈만 다시 돈다. 상류에 높은 티어를 둔다.
2. **판단 자유도가 낮으면 티어를 낮춘다.** 계획서가 파일과 테스트 명세까지 특정했다면
   구현은 실행에 가깝고, RED/GREEN 게이트가 기계적으로 검증한다. 검증 장치가 있는
   자리는 낮춰도 안전하다.
3. **리뷰어는 작성자와 같은 티어 이상.** 아래 티어가 위 티어의 산출물을 반려하기 어렵다.
   벤더를 바꿀 수 없으므로 티어만이라도 낮추지 않는다.
   **기준 2와 충돌하면 기준 3이 이긴다** — 검증 장치가 사람이 아니라 다른 에이전트일 때,
   그 에이전트의 티어를 낮추면 검증 자체가 약해진다.

**기본 배치(균형 프로필)**

| 워커 | 모델 | 근거 |
|---|---|---|
| 코디네이터 | Opus 5 | 게이트 판단·에스컬레이션 해석 |
| W1 analyst | Opus 5 | 기준 1의 최상류 |
| W2 spec-reviewer | Opus 5 | 기준 3 |
| W3 planner | Opus 5 | 분할 실패가 곧 merge 충돌이고 파이프라인 되감기 |
| W4 plan-reviewer | Opus 5 | 기준 3. 검사 항목이 명시적 체크리스트라 기준 2로는 낮출 만하지만, W3가 Opus라 기준 3이 이긴다 |
| 모듈 구현 워커 | planner가 모듈별로 지정 | 아래 |
| integrator | Sonnet 5 | 기계적. 충돌 시 판단하지 않고 escalation |
| code-reviewer | Opus 5 | 여기서 놓치면 사람에게 그대로 간다 |

**모듈 구현 워커의 모델은 planner가 정한다.** 계획서의 각 task에 `model:` 필드와 근거
한 줄을 쓴다. 판단 규칙:

- Opus — Compose UI·recomposition, 코루틴 동시성, 기존 코드 대수술, 파일 6개 이상
- Sonnet — 계획서가 시그니처까지 특정한 신규 파일, DTO·매퍼·Repository 구현 같은 정형 작업

**에스컬레이션 규칙** — 워커 실패로 재시도할 때는 **한 티어 올려서** 재시도한다.
같은 모델에 같은 프롬프트를 다시 넣으면 같은 결과가 나온다.
이미 최상위 티어(Opus 5)라 올릴 곳이 없으면 — `품질` 프로필은 항상 이 경우다 —
모델 대신 **프롬프트를 좁혀서** 재시도한다(실패 지점만 남기거나 실패 로그를 spec에 넣는다).

**프로필 3종** (요구사항 투입 시 선택, 기본은 균형)

| 프로필 | 내용 | 용도 |
|---|---|---|
| 균형(기본) | 위 표 | 보통 |
| 품질 | 전부 Opus 5 | 아키텍처를 건드리는 큰 변경 |
| 비용 | W1 analyst·W2 spec-reviewer·code-reviewer만 Opus 5, 나머지(W3·W4 포함) Sonnet 5 | 화면 하나 추가 수준의 정형 작업 |

## 실행 메커니즘

Run 생성과 task 등록:

```bash
orca orchestration run-create --objective "<요구사항 요약>" --json
orca orchestration task-create --spec "<task spec>" [--deps '<json_array>'] --json
```

**`worker-start --agent claude`는 모델을 지정하지 못한다.** 모델 티어를 지정해야 하는 워커는
두 단계로 나눈다. 아래 "모델 분배"의 프로필 표가 **문서 워커(W1~W4)에도 티어를 지정**하므로,
문서 워커도 이 2단계 경로를 쓴다. 문서 워커는 worktree를 새로 파지 않고 현재 worktree에
터미널만 추가한다는 점만 다르다.

```bash
# 문서 워커 — 현재 worktree에 모델 지정 터미널만 추가
orca terminal create --worktree current --command "claude --model opus" --json
orca orchestration worker-start --task <task_id> --terminal <handle> --json

# 모듈 워커 — 자식 worktree를 먼저 만들고 그 안에 터미널
orca worktree create --name wt-<module> --repo name:TJYG-Android --base-branch <feature/xxxxx-master> --json
orca terminal create --worktree name:wt-<module> --command "claude --model sonnet" --json
orca orchestration worker-start --task <task_id> --terminal <handle> --json
```

`terminal create --json` 응답의 handle 필드 경로는 미검증이다. 첫 실행에서 출력 전체를 보고
경로를 특정한다(가이드가 `worktree create --agent`에 대해 안내하는 `agentTerminalHandle` /
`startupTerminal.handle`이 첫 추측이지만 `terminal create`에 같은지는 확인 전이다).

`--agent claude` 한 줄 경로는 **모델 지정을 포기하는 대체 경로**로만 남는다(아래 `wait-for-setup` 참조).
자식 worktree에 이 경로를 쓸 때는 `--repo name:TJYG-Android --base-branch <feature/xxxxx-master>`를
반드시 함께 준다. 빼면 repo 기본 base(`develop`)에서 갈라져 최종 `git diff`가 어긋난다.

`--terminal`로 붙여도 orchestration 계보(task·dispatch·`worker_done` 권한·주입 프리앰블)는
그대로 유지된다.

대가가 하나 있다. 이 경로는 repo의 `wait-for-setup` 시작 정책을 강제하지 못한다.
TJYG-Android는 `setupAgentStartupPolicy: start-immediately`이고 setup 스크립트도 비어 있어
이 경로를 써도 잃는 것이 없다(2026-08-05 확인). 정책이 나중에 `wait-for-setup`으로 바뀌면
모델 지정을 포기하고 `--agent claude`(기본 모델)로 간다.

코디네이터 대기 루프:

```bash
orca orchestration check --wait --types worker_done,escalation,question --timeout-ms 540000 --json
orca orchestration check --ack <delivery_id> --wait --types worker_done,escalation,question --timeout-ms 540000 --json
```

`--wait` 타임아웃은 코디네이터 하네스의 Bash 도구 타임아웃(최대 600000ms)보다 짧게 잡는다.
길게 주면 orca가 아니라 하네스가 먼저 명령을 끊는다.

타임아웃이나 `{count:0}`은 실패가 아니라 체크포인트다. 워커를 죽이지 않고 계속 기다린다.

## 게이트와 에스컬레이션

사람이 개입하는 지점:

| 게이트 | 시점 | 생략 가능 |
|---|---|---|
| G1 | 스펙 리뷰 통과 후 | 자동진행 모드에서 생략 |
| G2 | 계획 리뷰 통과 후 | 자동진행 모드에서 생략 |
| G3 | 최종 보고 | 생략 불가 |

**세 지점 모두 코디네이터가 사용자에게 대화로 직접 묻고 답을 기다린다.** Orca gate 객체를
만들지 않는다 — 코디네이터는 이미 사람과 대화 중인 세션이므로 중간에 게이트 객체를 둘 이유가
없고, `gate-create`는 코디네이터가 관리하는 task DAG 결정용이지 사람 승인 채널이 아니다.
게이트 객체를 쓰면 gate id 회수·`gate-resolve`·사람이 답을 넣을 경로가 모두 필요한데,
그 경로가 이 파이프라인에는 존재하지 않는다.

각 지점에서 코디네이터가 제시하는 것: **산출물 경로 + 리뷰 결과 요약 + 선택지(진행 / 수정
요청 / 중단)**. 요구사항을 투입할 때 "자동진행"이라고 지정하면 G1·G2를 건너뛰고 G3만 받는다.

`check --wait --types worker_done,escalation,question`(워커 lifecycle mail)은 그대로 쓴다.
빠지는 것은 게이트 기제뿐이다.

에스컬레이션이 발생하는 조건:

| 상황 | 동작 |
|---|---|
| `worker-start` 실패 | receipt의 `stage`·`effects` 확인 후 `--retry-of`로 1회 재시도. 무한 재시도 금지 |
| RED 로그 누락 | 반려 — 새 task + `dispatch --inject`로 그 워커 터미널 재-디스패치 |
| 담당 파일 화이트리스트 초과 변경 | escalation. 모듈 분할이 틀렸다는 신호이므로 워커에게 되돌리게 하지 않는다 |
| GREEN 미달성 | 한 티어 올린 모델로 재-디스패치 1회(이미 최상위면 프롬프트를 좁혀 재시도) → 실패 시 escalation |
| 문서 리뷰 반려 3회째 | escalation |
| merge 충돌 해결 불가 | escalation. 분할이 잘못됐다는 신호이므로 계획 단계로 되돌아간다 |
| 같은 task 3연속 실패 | Orca가 circuit-break하여 task failed → 사람 보고 후 중단 |
| `wiki/` 정책과 스펙이 상충 | escalation. 기획 미결일 수 있어 에이전트가 판정하지 않는다 |

## 가시성

코디네이터가 단계 전환마다 Orca 워크스페이스 카드를 갱신한다(worktree comment와
cardStatus). 사람은 언제든 다음으로 현황을 볼 수 있다.

```bash
orca orchestration task-list --brief --json
```

## 파일 구성

| 파일 | 역할 |
|---|---|
| `.claude/skills/start-orchestration-session/SKILL.md` | 신설. 이 파이프라인의 진입점 |
| `.claude/skills/start-default-session/SKILL.md` | 기존 `start-session` 디렉토리 개명 + frontmatter `name` 갱신. 내용 변경 없음 |
| `CLAUDE.md` | 작업 유형별 라우팅에 이 파이프라인 진입점 한 줄 추가 |
| `parfait/specs/README.md` | 이 스펙 인덱스 등록 |

`start-orchestration-session` 스킬이 담을 것:

- 전제 확인 절차(Orca 런타임, orchestration 활성, TJYG-Android worktree 존재)
- Run 생성과 요구사항 수집(프로필 선택, 자동진행 여부)
- 단계별 워커 기동 명령과 task spec 템플릿
- 게이트·에스컬레이션 처리
- 최종 산출과 보고 형식

## 주의 / 열린 질문

- **에이전트 CLI가 `claude` 하나뿐**이라 리뷰어의 벤더 독립성을 확보할 수 없다.
  같은 모델 계열은 맹점을 공유하므로, 리뷰가 놓치는 유형이 체계적으로 존재할 수 있다.
- **Gradle 동시 실행 비용이 미측정 상태**다. worktree 3개에서 데몬과 캐시 경합이
  어떻게 나타나는지는 첫 실행에서 관측한 뒤 판단한다.
- **모듈 분할이 항상 성립하지는 않는다.** 한 모듈 안에서 파일이 많이 겹치는 요구사항은
  병렬화 이득이 없고, 이때 planner가 단일 워커로 결정할 수 있어야 한다.
- **문서 산출물이 team-yg repo에, 코드가 TJYG-Android repo에** 나뉘어 있어 두 repo의
  커밋·PR 흐름이 별개다. 스펙·계획 문서의 커밋은 이 파이프라인 밖에서 사람이 승인한다.
- **문서 워커의 작업 디렉토리는 TJYG-Android worktree인데 쓰기 대상은 team-yg repo**다.
  cwd 밖 절대경로 쓰기가 권한 프롬프트에 걸리면 워커가 사람 개입 없이 진행하지 못한다.
  스킬 §0 전제 확인에 cross-repo 쓰기 probe를 넣어 파이프라인 시작 전에 판정하고,
  막히면 **문서 워커만 team-yg worktree에서 띄우는** 대체 경로로 간다(구현 워커는 영향 없음).
  실제 환경에서 아직 관측되지 않았으므로 열린 항목으로 남긴다.
