# CLAUDE.md

## 언어 (Language)

- 사용자 응답: **한국어**로 작성
- 이 레포에 생성하는 문서 (`CONTEXT.md`, ADR, 이슈 본문, PRD, 문서 내 코멘트 등): **한국어**로 작성
- 스킬 파일(`SKILL.md` 등)은 **영어** 유지 — 사용자가 영어 스킬 정의를 읽는 데 문제없음
- 코드, 식별자, 파일명, 커밋 메시지, PR 제목: **영어** 유지 (이 레포의 표준 컨벤션)
- 스킬이 정한 고정 구조(헤딩, 프론트매터 키)는 영어로 두고, **내용**만 한국어로 작성

## Agent skills

### Issue tracker

Issues live in this repo's GitHub Issues, accessed via the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Five canonical triage roles using their default label strings. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` and `docs/adr/` at the repo root. See `docs/agents/domain.md`.
