# LLM 위키 이식 설계 — 단일 도메인 평탄 구조

- 작성일: 2026-09-17
- 대상 브랜치: `feature/ai/llm-wiki`
- 원본: `huihun-private-wiki` (`porgy`) — LLM이 운영하는 멀티 도메인 마크다운 위키

## 1. 목적

`huihun-private-wiki`가 운영 중인 위키 체계를 이 저장소로 옮긴다. 원본은 여러 도메인을
전제한 구조이고, 이 저장소에서 위키가 다룰 대상은 하나뿐이다 — parfait의 제품 스펙과
기획. 따라서 도메인 차원을 제거한 평탄 구조로 옮긴다.

옮기는 것은 **구조**다. 원본의 parfait 페이지 42건은 옮기지 않는다. 그 콘텐츠는
`huihun-private-wiki`에 남고, 이 저장소의 위키는 빈 스캐폴드로 시작해 여기서 새로
ingest한다.

## 2. 확정된 결정

| 항목 | 결정 | 근거 |
|---|---|---|
| 콘텐츠 이관 | 하지 않는다. 구조만 | 원본 위키가 계속 정본. 여기서 새로 ingest |
| 도메인 레이어 | 제거(평탄화) | 이 저장소에서 위키 대상은 영구히 하나 |
| 스크립트 범위 | 핵심 4개 + `wikilib` + `graph_signals` | `sync_issues.py` 제외 |
| 위키 주제 | 제품 스펙·기획 | 코드 구현 세부는 범위 밖 |
| `raw/` 위치 | `wiki/raw/` | Gradle 멀티모듈 루트를 어지럽히지 않는다 |
| 팀 레포 통합 수준 | 파일만 추가, CI 미변경 | 기존 Gradle CI와 분리, 팀원 작업에 영향 0 |
| graphify | 사용. 산출물은 `wiki/graphify-out/` | 콘텐츠를 `wiki/pages/`로 한 겹 내려 재귀 회피 |
| Obsidian | 제외 | 스크립트 의존 없음. 공용 레포에 개인 상태 파일 불필요 |
| `pages/purpose.md` | 원본 `parfait-purpose.md` 그대로 | 이미 검증된 헌장이고 대상이 같다 |

### 2.1 `sync_issues.py`를 제외하는 이유

원본에서 미결 항목은 GitHub 이슈로 단방향 투영된다. 이 저장소는 팀 공용
(`mash-up-kr/TEAMYG-Android`)이라 스크립트가 팀 이슈 트래커에 이슈를 자동 생성한다.
위키 운영이 팀 합의 없이 팀 작업 공간을 건드리게 되므로 뺀다.

딸려서 빠지는 것:

- `lint.py`의 `check_issue_field` 검사 — `issue: #숫자` 형식을 지키는 유일한 이유가
  동기화 스크립트의 이슈 중복 생성 방지였다. 소비자가 없으면 규칙도 없다.
- `conventions.md` §3의 선택 필드 `issue`와 관련 서술 전체
- `references/lint-rules.md`의 `issue` 규칙 행
- `test_sync_issues.py`, `test_issue_templates.py`

### 2.2 Obsidian을 제외하는 이유

원본은 저장소 루트를 Obsidian vault로 열고 `[[파일명]]` 링크를 사람이 따라간다.
이 저장소에서는 그러지 않는다.

Obsidian은 어떤 스크립트의 의존이 아니다. 빠지는 것은 `.obsidian/` 디렉토리,
`.gitignore`의 obsidian 항목, `wikilib.SKIP_DIRS`의 `.obsidian` 하나다.

**남는 것**: `[[이름]]` 문법과 `파일명중복`·`경로링크`·`고아` 세 검사. 이 셋은
Obsidian 때문이 아니라 `route.py`의 seed 확장이 링크를 결정적으로 resolve해야
동작하기 때문에 필요하다. 다만 원본 `conventions.md`와 `references/lint-rules.md`가
이 셋의 근거로 "Obsidian은 이름이 겹치면 링크를 아무 파일로나 해석한다"를 쓰고 있다.
그 문구를 그대로 두면 이 저장소에 없는 도구를 근거로 든 규칙이 남는다. 근거를
"링크 resolve가 결정적이어야 `route.py` seed 확장이 동작한다"로 교체한다.

### 2.3 graphify와 `wiki/pages/` 한 겹

graphify 산출물 디렉토리는 스캔 루트와 분리돼야 한다. 산출물을 스캔 루트 안에 두면
산출물이 자기를 스캔한다. 원본에도 그 사고 흔적이 남아 있다 —
`wiki/domains/graphify-out/cache/stat-index.json`이 스캔 루트 안에 잘못 쓰였다.

산출물을 `wiki/` 안에 두기로 했으므로 콘텐츠를 `wiki/pages/`로 한 겹 내린다.
원본의 `wiki/domains/`가 하던 스캔 루트 역할을 `wiki/pages/`가 그대로 받는다.

**대가**: `wiki/graphify-out/GRAPH_REPORT.md`가 위키 마크다운 수집 범위 안으로
들어온다. 막지 않으면 `파일명중복`·`고아` 검사에 자기 산출물이 걸린다.
`wikilib.UNCOLLECTED_DIRS`에 `wiki/graphify-out`을 추가하는 것이 그 마개다.
원본은 `graphify-out/`이 `wiki/` 밖이라 이 문제가 없었다.

### 2.4 graphify 실행에 관한 사실

`graphify --help` 기준으로 정리한다.

- `graphify update <path>` — LLM 불필요. **코드 전용**.
- `graphify extract <path> --code-only` — 로컬 AST, API 키 불필요. **코드 전용**.
- `graphify extract <path>` 일반 — semantic LLM 단계를 거친다. 백엔드는
  `gemini|kimi|claude|openai|deepseek|ollama`. `ollama`이거나 `OPENAI_BASE_URL`을
  로컬 서버(llama.cpp / vLLM / LM Studio)로 돌리면 **API 키 없이 로컬에서 돈다**.
- `graphify label` / `cluster-only` — `--backend=claude-cli` 가능(키 불필요). 단
  커뮤니티 이름 붙이기지 추출이 아니다.

이 위키는 마크다운이라 코드 전용 경로는 해당 없다. semantic 추출이 필수이고, 그
백엔드가 로컬이면 키는 필요 없다.

**운영 규칙**: PR을 올리기 전 로컬에서 graphify를 1회 수동 실행하고
`wiki/graphify-out/` 산출물까지 함께 커밋한다. 백엔드는 실행하는 사람이 환경에 맞춰
고른다 — `wiki/CLAUDE.md`에 특정 백엔드를 박지 않는다.

`graph.json`이 아예 없으면 `graph_signals`는 경고를 내지 않고 조용히 통과한다.
백엔드가 없는 환경에서도 `lint.py`는 정상 동작한다.

## 3. 디렉토리 배치

```
wiki/
  CLAUDE.md                운영 진입점               (수집 제외)
  conventions.md           데이터 계약               (수집 제외)
  routing.json             의도 → 문서 라우팅
  log.md                   작업 로그, append-only
  open-questions.md        전역 미결 단일 추적처
  routing-misses.md        라우팅 실패 기록          (수집 제외)
  references/              의도별 긴 절차            (수집 제외)
  templates/               페이지 템플릿             (수집 제외)
  script/                  스크립트 + tests/         (수집 제외)
  raw/                     미통합 원본
  graphify-out/            그래프 산출물             (수집 제외, 스캔 루트 밖)
  pages/                   ← graphify 스캔 루트
    purpose.md             목표·핵심 질문·범위
    overview.md            논지
    index.md               페이지 카탈로그
    sources/               src-*.md
    concepts/
    entities/
    queries/
    synthesis/             분석 페이지
```

원본 대비 달라지는 점과 근거:

1. **`pages/` 한 겹** — §2.3 참조. 도메인 이름 접두사(`parfait-`)는 사라진다.
2. **허브 파일과 도메인 구조 파일이 한 벌로 합쳐진다.** 원본은 `wiki/purpose.md`(전역)와
   `wiki/domains/parfait/parfait-purpose.md`(도메인) 두 벌이었다. 도메인이 없으니
   `pages/purpose.md` 하나다. `overview`, `index`도 같다.
3. **전역 추적 파일 두 개를 `wiki/` 바로 아래로 올린다.** 원본 위치(`wiki/synthesis/`)를
   유지하면 `wiki/synthesis/`와 `wiki/pages/synthesis/`가 이름만 같고 성격이 다른 두
   디렉토리가 된다.
4. **파일명 유일성 검사 범위를 `wiki/`로 한정한다.** 이 저장소에는 `README.md`가
   3곳(`/`, `http/`, `tools/build-cache-bench/`)에 있어 원본 검사를 그대로 옮기면
   `파일명중복` 위반이 즉시 뜬다. `[[이름]]` 링크는 위키 안에서만 쓰이고 안드로이드
   쪽 마크다운은 링크 대상이 아니다. `SKIP_DIRS`에 예외를 계속 쌓는 것보다 경계를
   긋는 것이 맞다.
5. **`raw/`에 도메인 하위 디렉토리가 없어진다.** `raw/<도메인>/X.md` →
   `wiki/raw/X.md`.

## 4. 스크립트 변경

가져올 것: `route.py` `lint.py` `check_status.py` `ingest_cache.py` `wikilib.py`
`graph_signals.py` + `conftest.py` + 테스트 14개.
제외: `sync_issues.py` `test_sync_issues.py` `test_issue_templates.py`
`test_lint_issue_field.py` (원본 테스트 17개 중 3개 제외).

### 4.1 `wikilib.py`

도메인 차원의 뿌리다. 여기를 먼저 못 박는다.

- `Page.domain` 프로퍼티 삭제
- `all_markdown` 범위를 `repo_root` → `repo_root / "wiki"`
- `SKIP_DIRS`에서 `.obsidian` 제거
- `UNCOLLECTED_DIRS`에 `wiki/graphify-out` 추가
- `UNCOLLECTED_FILES`에서 저장소 루트 항목(`CLAUDE.md`, `AGENTS.md`, `README.md`,
  `script/README.md`) 제거 — 범위가 `wiki/`라 닿지 않는다. 남는 것은
  `wiki/CLAUDE.md`, `wiki/conventions.md`, `wiki/routing-misses.md`

### 4.2 `lint.py`

상수 `STRUCTURE_SUFFIXES = ("-purpose", "-index", "-overview")` →
`STRUCTURE_FILES = {"purpose.md", "index.md", "overview.md"}`.

| 검사 | 변경 |
|---|---|
| `check_unique_names` | 범위가 `wiki/`로 좁아짐 (`all_markdown` 경유) |
| `check_links` | 없음 |
| `check_orphans` | `_orphan_exempt`가 `<도메인>-purpose` 접미사 판정 → `pages/{purpose,index,overview}.md` 고정 이름 판정 |
| `check_layout` | 전면 재작성. `wiki/domains/<d>/` 배치 규칙 → `wiki/pages/` 배치 규칙 |
| `check_frontmatter` | `_needs_frontmatter`의 구조 파일 판정만 |
| `check_provenance` | `by_domain` 딕셔너리 제거, sources 단일 집합으로 |
| `check_raw_sync` | `wiki/raw/X.md` ↔ `wiki/pages/sources/src-X.md` 짝 |
| `check_manifest` | 경로 상수만 |
| `check_sensitive` | raw 경로만 |
| `check_action_enum` | `wiki/open-questions.md` 경로만 |
| `check_issue_field` | **삭제** (§2.1) |
| `graph_signals` | §4.6 |

`CONTENT_DIRS = {"sources", "concepts", "entities", "queries", "synthesis"}`는 그대로다.

### 4.3 `route.py`

- `--domain` 인자와 `Route.domain` 필드 삭제, `lookup()`의 도메인 분기 삭제
- `validate_routing`의 `domains.*` 버킷 검증 삭제
- 종료 코드 4 설명과 seed 오류 메시지 예시를 `wiki/pages/concepts/<페이지>.md`로

`routing.json`:

- `domains` 블록 삭제
- 의도 8개 → 7개. `add-domain` 제거
- 각 의도의 `required`/`reference` 경로를 새 배치로 갱신
- `research`의 reference를 `wiki/graphify-out/GRAPH_REPORT.md`로

### 4.4 `check_status.py`

- `_key(domain, name)` → `nfc(name)` 단독. `collect_sources` 키가 `도메인/stem` → `stem`
- `check_index_projection` 대상이 `wiki/domains/<d>/<d>-index.md` → `wiki/pages/index.md`
- "판본 체인은 도메인 내부로 한정한다" 서술과 관련 오류 메시지 삭제

### 4.5 `ingest_cache.py`

- `MANIFEST_REL` → `wiki/raw/.manifest.json`
- `repo_root_ok`를 `wiki/` 기준으로

### 4.6 `graph_signals.py`

- `SCAN_ROOT = "wiki/domains"` → `"wiki/pages"`
- 산출물 경로 3개(`graph.json`, `manifest.json`, `.graphify_root`)를
  `repo_root / "wiki" / "graphify-out" / ...`로

## 5. 문서·규약 변경

### 5.1 `wiki/conventions.md`

| 절 | 변경 |
|---|---|
| §1 근거 우선순위 | 허브 → 도메인 index 2단계가 `wiki/pages/index.md` 한 단계로 |
| §2 판본 상태 | "판본 관계는 같은 도메인 안에서만 성립한다" 삭제. 투영 대상이 `wiki/pages/index.md` |
| §3 미결 항목 | `issue` 필드 문단 전체 삭제. 경로 `wiki/open-questions.md` |
| §4 파일명·링크 | Obsidian 근거 교체(§2.2). 예외 목록을 `wiki/CLAUDE.md`·`wiki/conventions.md`·`wiki/routing-misses.md` 셋으로 축소 |
| 디렉토리 배치 | `wiki/domains/<도메인>/` → `wiki/pages/` |

### 5.2 `wiki/CLAUDE.md`

- 라우팅 절차에서 도메인 판정 단계 삭제
- 의도 7개로 갱신
- "작업 후" 절을 다음으로 교체:

  > `python3 wiki/script/lint.py`로 위반 0건을 확인한다. PR을 올리기 전 로컬에서
  > graphify를 1회 수동 실행하고 `wiki/graphify-out/` 산출물까지 함께 커밋한다.
  > 백엔드는 환경에 맞춰 고른다.

- graphify 조회 명령의 경로를 새 배치로 갱신

### 5.3 `wiki/references/`

5개 중 `add-domain.md` 삭제, 4개 유지.

- `lint-rules.md` — `issue` 규칙 행 삭제, Obsidian 근거 교체, `SCAN_ROOT` 값을
  `wiki/pages`로, 경로 갱신
- `ingest-checklist.md` — 도메인 배치 단계 제거
- `cascade-delete.md`, `research.md` — 경로 갱신

### 5.4 `wiki/templates/`

9개 중 `domain-purpose.md`·`domain-index.md`·`domain-overview.md` 3개 삭제.
6개 유지: `concept` `entity` `query` `source` `synthesis-analysis` `synthesis-lint`.

구조 파일 3개는 위키 수명 동안 한 번만 만든다. 템플릿이 값을 못 한다.

### 5.5 콘텐츠 골격

- `wiki/pages/purpose.md` — 원본 `parfait-purpose.md`를 그대로. "parfait 도메인" →
  "이 위키"로 용어를 고친다. 지역 규약 3개(화면은 ID로 지칭, 빈 칸은 미정, 답 없이
  사라진 서술을 해소로 읽지 않기)를 포함한다.

  **링크 처리**: 원본은 `[[parfait-screen-id-scheme]]`과 `[[open-questions]]` 두
  위키링크를 건다. `[[open-questions]]`는 `wiki/open-questions.md`가 수집 대상이라
  resolve된다. `[[parfait-screen-id-scheme]]`은 콘텐츠 페이지라 빈 스캐폴드에
  존재하지 않는다 — 그대로 옮기면 `깨진링크` 위반이 떠서 §7의 "위반 0건"과 충돌한다.
  해당 문장에서 링크를 풀어 평문으로 적고, 그 페이지가 실제로 생기는 첫 ingest 때
  링크로 되돌린다
- `wiki/pages/overview.md`, `wiki/pages/index.md` — 빈 카탈로그 골격
- `wiki/open-questions.md`, `wiki/routing-misses.md` — 빈 골격
- `wiki/log.md` — 첫 항목이 이 이식 작업

### 5.6 신설 파일

- `.claudeignore` — graphify가 파일을 쓸 때마다 프롬프트 캐시가 무효화된다.
  `wiki/graphify-out/graph.json`, `graph.html`, `cache/` 차단.
  `GRAPH_REPORT.md`는 일부러 열어둔다 — `routing.json`의 `research` 의도가 이 파일을
  reference로 내보내므로 막으면 라우팅이 열라고 건넨 문서를 열 수 없다
- `.gitignore` 추가 — `__pycache__/`, `*.pyc`,
  `wiki/graphify-out/cost.json`, `wiki/graphify-out/cache/last_query_stamp`

## 6. 작업 순서

아래에서 위로 의존한다. 각 단계는 테스트를 먼저 고치고 스크립트를 고친다 — 경로 치환이
빠진 테스트는 여전히 통과해버린다(`wiki/domains/a/...` fixture를 만들면 새 `load_pages`가
그냥 0건을 돌려준다). 실패를 먼저 봐야 한다.

1. 디렉토리 골격, `.gitignore` 추가, `.claudeignore` 신설
2. `wikilib.py` + `test_wikilib.py`
3. `lint.py` + 테스트 7개 (`test_lint_cli`, `test_lint_frontmatter`, `test_lint_layout`,
   `test_lint_links`, `test_lint_names`, `test_lint_raw`, `test_lint_sensitive`).
   `test_lint_issue_field`는 가져오지 않는다
4. `check_status.py` + `test_check_status.py`
5. `ingest_cache.py` + `test_ingest_cache.py`
6. `route.py` + `routing.json` + `test_route_lookup`, `test_route_expand`,
   `test_routing_json`
7. `graph_signals.py` + `test_graph_signals.py`
8. 문서 — `conventions.md`, `wiki/CLAUDE.md`, `references/` 4개, `templates/` 6개
9. 콘텐츠 골격 — §5.5
10. 전체 검증

## 7. 검증

```
python3 -m pytest wiki/script/tests/ -q       # 전부 통과
python3 wiki/script/lint.py                    # 종료 코드 0, 위반 0건
python3 wiki/script/check_status.py            # 종료 코드 0
python3 wiki/script/route.py --intent ingest   # 0
python3 wiki/script/route.py --intent query    # 0
python3 wiki/script/route.py --intent research # 0
python3 wiki/script/route.py --intent delete-source  # 0
python3 wiki/script/route.py --intent lint     # 0
python3 wiki/script/route.py --intent schema-change  # 0
python3 wiki/script/route.py --intent unclear  # 3 (정지)
python3 wiki/script/route.py --intent add-domain     # 4 (삭제된 의도)
cd /tmp && python3 <repo>/wiki/script/lint.py  # 2 (루트 아님)
```

빈 스캐폴드라 `lint.py`의 콘텐츠 검사 대상은 0건이다. `graph.json`이 없으므로 그래프
신호 4종(`그래프stale`·`그래프고립`·`희소커뮤니티`·`브리지`)은 아예 뜨지 않는다 —
경고 0건이 정상 상태다.

이 검증은 "검사가 도는지"만 증명하고 "콘텐츠가 맞는지"는 증명하지 않는다. 후자는 첫
원본이 ingest돼야 확인된다.

## 8. 범위 밖

- `sync_issues.py`와 GitHub 이슈 투영
- CI 워크플로우 변경
- 실제 콘텐츠 ingest — 이 작업의 산출물은 빈 스캐폴드다
- graphify 첫 추출 — 백엔드가 있는 환경에서 별도로 수행

## 9. 미확정

- graphify 백엔드를 무엇으로 고정할지. 현재 머신에 `ollama`가 없고 관련 API 키
  환경변수도 설정돼 있지 않다. `wiki/CLAUDE.md`는 백엔드를 박지 않고 선택지만
  적는다. 실제 첫 추출 시점에 정한다.
