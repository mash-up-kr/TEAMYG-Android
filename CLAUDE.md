# CLAUDE.md

## 언어 (Language)

- 사용자 응답: **한국어**로 작성
- 이 레포에 생성하는 문서 (`CONTEXT.md`, ADR, 이슈 본문, PRD, 문서 내 코멘트 등): **한국어**로 작성
- 스킬 파일(`SKILL.md` 등)은 **영어** 유지 — 사용자가 영어 스킬 정의를 읽는 데 문제없음
- 코드, 식별자, 파일명, 커밋 메시지, PR 제목: **영어** 유지 (이 레포의 표준 컨벤션)
- 스킬이 정한 고정 구조(헤딩, 프론트매터 키)는 영어로 두고, **내용**만 한국어로 작성

## 문서 (`docs/`)

`docs/`는 이 앱의 **구현 문서**다. 무엇을 어떻게 만들었고 왜 그렇게 했는지를 남긴다.
제품 스펙·기획 자체는 `wiki/`가 적는다 — 의존 방향은 구현 → 위키 단방향이다.

| 경로 | 내용 |
|---|---|
| `docs/index.md` | 진입 허브. 라우팅은 여기서 본다 |
| `docs/superpowers/specs/` | 설계 스펙. 구현 완료분은 `archive/` |
| `docs/superpowers/plans/` | 구현 계획. 구현 완료분은 `archive/` |
| `docs/adr/` | 아키텍처 결정 기록 |
| `docs/architecture/` | 레이어별 구조 문서 |
| `docs/api/` | TEAMYG-SERVER 계약. 서버가 정본이고 Android 매핑은 각 문서의 절로 붙는다 |
| `docs/synthesis/` | 미결 항목·린트 로그 |
| `docs/doc-baseline.md` | 문서를 어느 `develop` 커밋 기준으로 마지막 검증했는지 적는 단일 출처 |
| `docs/script/` | `check_links` `search`. 전부 저장소 루트에서 실행 |

**Kotlin 코드 주석·KDoc 규약은 [`docs/code-conventions.md`](docs/code-conventions.md)에
있다.** 구현·리뷰를 서브에이전트에 디스패치할 때 그 문서를 브리프에 링크하고,
`writing-plans`로 계획을 쓸 때는 Global Constraints에도 넣는다.

문서를 옮기거나 `archive/`로 내린 뒤에는
`python3 docs/script/check_links.py docs`로 상대 링크를 전수 확인한다(깨지면 exit 1).

코드 작업(설계 → 계획 → 구현 → 리뷰)은 superpowers 체인으로 돈다. 그 산출물인 설계 스펙과
구현 계획은 `docs/superpowers/`에 남는다.

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
