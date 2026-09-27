---
name: sync-server-api-baseline
description: Use when the user asks to check or sync docs/api against TEAMYG-SERVER — "/sync-server-api-baseline", "서버 API 문서 점검", "서버 계약 갱신", "TEAMYG-SERVER delta 감사", "서버 API 바뀐 거 문서에 반영해줘", or when docs/api/server-baseline.md lags behind the server's origin/main.
---

# sync-server-api-baseline

## Overview

Audit only the server commits that landed on TEAMYG-SERVER `main` after the recorded baseline, fix contract drift in `docs/api/`, then move the baseline forward.

**Authority:** `docs/api/server-baseline.md` owns the baseline and the procedure (「점검 절차」). Read the whole procedure every run — its examples of contract changes hidden outside controllers are the core of the audit. Format rules live in `docs/api/README.md` 「규약」 and `docs/api/template.md`. If this skill and those files disagree, the files win.

## Setup

`<S>` is the local clone of `mash-up-kr/TEAMYG-SERVER`. Look for `../TEAMYG-SERVER` next to this repo. If it is missing, ask the user for the path. Never write the absolute path into any file in this repo.

The server repo is **read-only**. Run `fetch`, `log`, `show`, `diff` only. No checkout, branch, commit, or edit.

## Commands (run from this repo's root)

| Step | Command |
|---|---|
| Baseline | read 「현재 기준선」 in `docs/api/server-baseline.md` |
| Refresh | `git -C <S> fetch origin main` |
| Delta | `git -C <S> log --oneline <baseline>..origin/main` |
| Per commit | `git -C <S> show --stat --format='%h %s' <hash>` (merge commit: `git -C <S> diff --stat <hash>^1 <hash>`) |
| Server file | `git -C <S> show origin/main:<path>` |
| Android symbol | `git grep -n <symbol> origin/develop -- data/ domain/` |
| Links | `python3 docs/script/check_links.py docs` |

Track `main`, not `develop`. Never add `--merges`: feature PRs arrive as squash commits. Never read the server working tree: the local checkout sits on another branch.

If the delta is empty, update only the baseline fields and the history row, then report.

## Surfaces to update

When the delta changes the contract, update every surface below that it touches:

| Change | Surface |
|---|---|
| Endpoint, DTO field, error code, status, auth | domain doc tables and detail sections in `docs/api/<domain>.md` |
| Any audited domain doc | frontmatter `server_commit` + `verified` |
| Endpoint added or removed | `docs/api/README.md` 「도메인 계약」 row: count and endpoint list |
| New domain | new doc from `docs/api/template.md` + new row in the README table |
| Global contract (`ApiResponse`, `SecurityConfig`, `GlobalExceptionHandler`) | `docs/api/conventions.md` |
| Matching doc in `docs/api/spec/` | rerun its `## 코드 대조` section; never edit the team spec text |
| Android symbol exists and now disagrees | Android column → `⚠️불일치` + entry in `docs/synthesis/open-questions.md` |
| Unverifiable fact | `## 미결` in the domain doc + entry in `docs/synthesis/open-questions.md` |

Out of scope: Android-side changes (`android_status`, 「Android 매핑」) belong to the `sync-develop-doc-baseline` skill. No edits to TJYG-Android code.

## Finish

1. Replace the 「현재 기준선」 fields in `server-baseline.md`: commit, summary, 검증일 + 회차 (previous + 1).
2. Add one row to 「기준선 이력」: delta size, endpoint total, new domains/error codes, Android column changes, OQ added/closed.
3. Run `check_links.py`. It must exit 0.
4. Report to the user: delta table (commit → contract effect), docs changed, OQ added/closed.
5. Stop. Do not commit. Commit only when the user asks.
