---
name: sync-develop-doc-baseline
description: Use when the user asks to check or sync docs against develop in TEAMYG-Android — "/sync-develop-doc-baseline", "develop 기준 문서 점검", "develop 문서 점검", "doc-baseline 맞춰줘", "baseline sync 확인", "develop delta 감사", or when docs/doc-baseline.md lags behind origin/develop.
---

# sync-develop-doc-baseline

## Overview

Audit only the merges that landed on `develop` after the recorded doc baseline, fix doc drift, then move the baseline forward.

**Authority:** `docs/doc-baseline.md` owns both the baseline and the procedure (「점검 절차」 + 「판정 규칙」). Read the whole file every run. This skill adds only the repo-specific commands and the output contract. If this skill and that file disagree, the file wins.

## Scope

The audit reconciles existing docs with merged code. It never creates a new spec or plan.
- A merge with a pre-written spec/plan: compare the spec decisions line by line against the merged code.
- A merge without one: check `status.md`, `architecture/*`, `adr/`, `api/` Android sections, and `synthesis/open-questions.md` only.
- A spec or plan that needs writing: report it to the user as a follow-up (superpowers brainstorming → writing-plans). Do not write it during the audit.

`docs/api/` contract sections and their `verified` field belong to `docs/api/server-baseline.md`, which is a separate manual process. This audit edits only the Android mapping parts.

## Commands (run from repo root)

| Step | Command |
|---|---|
| Baseline | read 「현재 기준선」 in `docs/doc-baseline.md`; cross-check the latest `audit` line in `docs/log.md` (`grep "^## \[" docs/log.md \| tail -5`) |
| Refresh | `git fetch origin develop` |
| Delta | `git log --oneline --merges <baseline>..origin/develop` |
| Per merge | `git diff --stat <merge>^1 <merge>` and `git diff --dirstat=files,5 <merge>^1 <merge>` |
| Merged code | `git show origin/develop:<path>` |
| Unmerged symbol | `git ls-tree -r --name-only origin/develop \| grep <symbol>` |
| Remote branch | `git ls-remote --heads origin <branch>` |
| Unit tests | `git grep -h -e '@Test' origin/develop -- ':(glob)**/src/test/**' \| grep -c '^[[:space:]]*@Test'` |
| Instrumented tests | same, with `':(glob)**/src/androidTest/**'` |
| Links | `python3 docs/script/check_links.py docs` (after any move to `archive/`) |

Use `<merge>^1 <merge>` for stats. `git show --stat` on a merge commit prints nothing useful.

If the delta is empty, update only the baseline fields and the `log.md` audit line, then report.

## Triage

Classify every merge before reading code:

| Touches | Action |
|---|---|
| `.github/`, color values only | no doc edit; baseline only |
| `wiki/` only | out of scope (`wiki/` is not `docs/`) |
| `docs/` only | check links and status table vs frontmatter; no code comparison |
| feature/core/data code | full comparison against the docs listed in Scope |

## Finish

1. Replace **every** field in 「현재 기준선」: commit + PR title, 검증일 + 회차 (previous + 1), 테스트 수, 미머지 추적 항목.
2. Append one `audit` entry to `docs/log.md` in the format given in that file's header quote. Take `+/-` and file count from `git diff --shortstat <baseline> origin/develop`.
3. Run `check_links.py`. It must exit 0.
4. Report to the user: delta table (merge → action), drift fixed, OQ added/closed, follow-ups (missing specs, unverified-on-device items).
5. Stop. Do not commit. Commit only when the user asks.
