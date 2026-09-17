# CLAUDE.md

## 언어 (Language)

- 사용자 응답: **한국어**로 작성
- 이 레포에 생성하는 문서 (`CONTEXT.md`, ADR, 이슈 본문, PRD, 문서 내 코멘트 등): **한국어**로 작성
- 스킬 파일(`SKILL.md` 등)은 **영어** 유지 — 사용자가 영어 스킬 정의를 읽는 데 문제없음
- 코드, 식별자, 파일명, 커밋 메시지, PR 제목: **영어** 유지 (이 레포의 표준 컨벤션)
- 스킬이 정한 고정 구조(헤딩, 프론트매터 키)는 영어로 두고, **내용**만 한국어로 작성

## 위키 (`wiki/`)

`wiki/`는 parfait의 **제품 스펙·기획**을 추적하는 마크다운 위키다. 무엇을 만들기로
했고 무엇을 만들지 않기로 했는지, 그 판단이 언제 왜 뒤집혔는지를 남긴다. 코드 구현
세부는 범위 밖이다 — 그건 소스가 이미 담고 있다.

위키 관련 작업을 시작하기 전에 `wiki/CLAUDE.md`를 먼저 읽는다. 규칙은 여기 없다.
`wiki/CLAUDE.md`(운영 진입점)와 `wiki/conventions.md`(데이터 계약)가 정본이고,
어긋나면 그쪽이 옳다.

모든 위키 작업은 라우팅으로 시작한다. 요청을 의도 하나로 분류한 뒤 아래를 돌리면
이번 작업에 읽어야 할 문서를 돌려준다.

    python3 wiki/script/route.py --intent <의도> [--seed <저장소상대경로> ...] [--json]

의도는 7개다 — `ingest` `query` `research` `delete-source` `lint` `schema-change`,
그리고 어디에도 확신이 없을 때 쓰는 `unclear`(진행하지 않고 사용자에게 확인한다).

| 경로 | 내용 |
|---|---|
| `wiki/pages/` | 위키 콘텐츠. `sources` `concepts` `entities` `queries` `synthesis` |
| `wiki/raw/` | 아직 통합되지 않은 원본 |
| `wiki/open-questions.md` | 미결 항목의 단일 추적 장소 |
| `wiki/script/` | `route` `lint` `check_status` `ingest_cache` 등. 전부 저장소 루트에서 실행 |

작업을 마치면 `python3 wiki/script/lint.py`로 위반 0건을 확인한다.

이 위키는 Gradle 빌드·CI와 완전히 분리돼 있다. 안드로이드 작업 중에는 건드릴 일이
없고, `wiki/` 밖으로 쓰는 스크립트도 없다.
