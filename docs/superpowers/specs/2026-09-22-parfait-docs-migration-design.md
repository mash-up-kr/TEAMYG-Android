# parfait 구현 문서 이관 설계 — android·api·script를 코드 레포로

- 작성일: 2026-09-22
- 대상 브랜치: `feature/#525-ai-docs-set-up`
- 원본: `team-yg-pesonal-agent` (`parfait/`) — AI 스킬·위키 레포

## 1. 목적

`parfait/android`·`parfait/api`·`parfait/script`를 이 저장소의 `docs/` 아래로 **복사**한다.
구현 문서가 구현 코드와 같은 저장소에 있게 만드는 것이 목적이다.

지금은 스펙·계획·ADR이 다른 저장소에 있어서 두 가지 비용을 낳는다.

1. 코드를 고치는 사람과 문서를 고치는 사람이 저장소를 오간다.
2. `parfait/android/CLAUDE.md`(KDoc 규약)가 **이 저장소에서 일하는 서브에이전트에게 닿지
   않는다.** 원문이 그 사실을 경고하고 있고, 우회책으로 디스패치 프롬프트의 전역 제약과
   계획의 Global Constraints에 요지를 수동으로 실어 날라 왔다.

2번은 이관만으로 사라진다. 그것이 이 작업의 주된 실익이다.

## 2. 확정된 결정

| 항목 | 결정 | 근거 |
|---|---|---|
| 이관 방식 | **복사**. 원본 저장소는 불변 | 원본 저장소는 사용자의 개인 작업장이고 이번 작업 범위 밖 |
| 원본 처리 | 양쪽 유지(미러). 삭제·포인터 추가 없음 | 위와 같음 |
| specs·plans 위치 | `docs/superpowers/{specs,plans}` | superpowers 기본 경로. 원본 저장소가 걸어 둔 override가 필요 없어진다 |
| adr·architecture·synthesis | `docs/` 바로 아래 | 플랫폼 축이 하나뿐이라 `android/` 한 겹이 의미 없다 |
| api | `docs/api/` | 이 저장소 기준으로는 Android가 소비하는 계약 문서다 |
| script | `docs/script/` | `wiki/script/`와 같은 배치. `REPO_ROOT = parents[2]`가 그대로 맞는다 |
| KDoc 규약 | `docs/code-conventions.md`. 루트 `CLAUDE.md`는 **링크만** | 루트 비대화 방지 |
| `vendor.py` | 제외 | 벤더 스킬이 이 저장소에 없다 |
| `oq_sync.py` | 제외 | 아래 2.1 |
| `search.py` | 이관 + **수집기 개작** | 아래 2.2 |
| `index.md` | pm 절을 걷어낸 사본 | `parfait/pm/`은 옮기지 않는다 |
| 위키 | 파일 이동 없음 | 아래 2.3 |

### 2.1 `oq_sync.py`를 제외하는 이유

`docs/superpowers/specs/2026-09-17-wiki-port-design.md` §2.1이 같은 이유로 `sync_issues.py`를
이미 제외했다.

> 이 저장소는 팀 공용(`mash-up-kr/TEAMYG-Android`)이라 스크립트가 팀 이슈 트래커에 이슈를
> 자동 생성한다. 위키 운영이 팀 합의 없이 팀 작업 공간을 건드리게 되므로 뺀다.

`oq_sync.py`는 같은 종류다 — `gh issue` 생성, 라벨 `oq:wiki`·`oq:parfait`를 만든다.
옮기면 이미 내린 결정을 뒤집는 것이므로 옮기지 않는다. open-questions의 이슈 투영은
원본 저장소에서만 돈다.

딸려서 빠지는 것: `test_oq_sync.py`, `test_vendor.py`.

### 2.2 `search.py`는 이동이 아니라 개작이다

현재 수집기는 `.claude/skills/*/SKILL.md`만 훑고 스킬 이름을 **디렉토리 이름**에서 뽑는다.
이 저장소의 `.claude/`에는 `settings.local.json` 하나뿐이고, 대상 문서는 평면 파일이라
그대로 옮기면 결과가 0건이다.

살리는 이유는 graphify가 닿지 않는 영역이 있기 때문이다. graphify의 스캔 루트는
`wiki/pages/`이고(2026-09-17 결정문 §2.3), `docs/` 아래 문서는 그 바깥이다. 스펙·계획·ADR·
아키텍처를 자연어로 찾을 수단이 이것 말고 없다.

대상 문서의 frontmatter가 `SKILL.md`보다 풍부하다.

```yaml
id: login-debug-mode
title: 로그인 화면 디버그 모드 (Debug Mode)
category: behavior-spec          # adr은 status/type, architecture는 category
related_spec: ...
related_adr: ...
related_code: LoginRoute, LoginViewModel, ...
tags: [plan, parfait, login, debug]
```

`tokenize`·`score`는 그대로 두고 **수집기만 교체**한다.

- 대상: `docs/superpowers/specs`, `docs/superpowers/plans`, `docs/adr`, `docs/architecture`
- `archive/` 포함. 완료분 181건이 선례 검색의 본체다. 결과 줄에 archived 표시를 붙인다
- 문서 키: 디렉토리 이름 대신 frontmatter `id`, 없으면 파일명 stem
- 점수 필드: `title`·`tags`·`related_code`를 기존 `name`·`desc`·`headings` 자리에 대응시킨다

`test_search.py`가 이미 있으므로 TDD 게이트가 선다.

### 2.3 위키를 옮기지 않는 이유

두 저장소의 `raw/`는 **내용이 동일하다**. 44개 파일 전부 SHA-256 일치를 확인했고, 차이는
이 저장소에만 있는 `.gitkeep`·`.manifest.json` 둘뿐이다(파일명의 유니코드 정규화 차이는
내용 차이가 아니다).

2026-09-17 결정문이 "콘텐츠 이관은 하지 않는다. 구조만"을 이미 정했고, 이 저장소의 위키는
같은 `raw/`를 독립적으로 재ingest해 왔다. 현재 sources 40건이 들어와 있다.

차이는 concepts 층에만 있다.

| | 원본 저장소 | 이 저장소 |
|---|---|---|
| 개수 | 22 | 9 |
| 이름 | 한글(`누끼-따기`, `공통-로딩`) | 영문(`canvas`, `topping-placement-pipeline`) |
| 입도 | 잘게 | 묶어서 |

따라서 위키 쪽 할 일은 파일 이동이 아니라 **같은 `raw/`에서 아직 뽑지 않은 개념을 추가
ingest**하는 것이다. 기존 `ingest` 워크플로가 그대로 처리한다. **이 설계의 범위 밖**이고
별도 작업으로 다룬다.

## 3. 목적지 구조

```
docs/
  index.md                  ← parfait/index.md (pm 절 제거)
  code-conventions.md       ← parfait/android/CLAUDE.md
  doc-baseline.md           ← parfait/android/doc-baseline.md
  superpowers/
    specs/                  ← parfait/android/specs      (3 + README + template + archive 105)
    plans/                  ← parfait/android/plans      (3 + README + template + archive 76)
  adr/                      ← parfait/android/adr        (32 + README + template)
  architecture/             ← parfait/android/architecture (6 + README + template)
  synthesis/                ← parfait/android/synthesis  (open-questions + lint 2)
  api/                      ← parfait/api                (11 + template + spec/ 4 + README)
  script/                   ← 아래 3.2
```

### 3.2 `docs/script/`가 받는 것

옮긴다: `check_links.py` · `search.py` · `test_check_links.py` · `test_search.py` ·
`README.md` · `_script-template.py` · `.gitignore`(`__pycache__` 무시).

옮기지 않는다: `oq_sync.py` · `vendor.py` · `test_oq_sync.py` · `test_vendor.py`(§2.1),
`SKILL.template.md`(스킬 작성 템플릿이고 이 저장소에는 벤더 스킬이 없다).

`README.md`는 빠진 스크립트 절을 지운 사본이다.

`docs/superpowers/`에는 이미 `2026-09-17-wiki-port`와 이 문서가 있다. 이관분은 그 옆에 앉는다.

### 3.1 `code-conventions.md`로 이름을 바꾸는 이유

원본은 `CLAUDE.md`라서 그 디렉토리 파일을 열면 자동 로드됐다. 같은 이름으로 `docs/` 아래
두면 **문서 작업을 할 때마다 Kotlin 주석 규약이 로드된다** — 범위가 틀린다.

이 저장소에서는 자동 로드가 필요 없다. 루트 `CLAUDE.md`가 한 줄 링크를 걸면 구현
서브에이전트가 그 경로를 읽는다. 원문이 경고하던 "서브에이전트에게 닿지 않는다" 문제가
해결되는 지점이므로, 그 경고 절은 이관 시 삭제한다.

## 4. 링크 재작성

`android/specs|plans` 참조만 79곳이고, `adr`·`architecture`·`synthesis`는 경로 깊이가
한 겹 줄어 상대 링크가 전부 어긋난다.

| 원본 | 목적지 | 깊이 |
|---|---|---|
| `parfait/android/specs/` | `docs/superpowers/specs/` | 3 → 3 (유지) |
| `parfait/android/plans/` | `docs/superpowers/plans/` | 3 → 3 (유지) |
| `parfait/android/adr/` | `docs/adr/` | 3 → 2 (감소) |
| `parfait/android/architecture/` | `docs/architecture/` | 3 → 2 (감소) |
| `parfait/android/synthesis/` | `docs/synthesis/` | 3 → 2 (감소) |
| `parfait/api/` | `docs/api/` | 2 → 2 (유지) |
| `parfait/api/spec/` | `docs/api/spec/` | 3 → 3 (유지) |
| `parfait/index.md` | `docs/index.md` | 1 → 1 (유지) |

깊이가 유지되는 쌍도 **상대 링크의 중간 경로가 바뀐다**. 예를 들어 `adr/0031`에서 스펙을
가리키던 `../specs/X.md`는 `../superpowers/specs/X.md`가 되고, `api/auth.md`의
`../android/specs/X.md`는 `../superpowers/specs/X.md`가 된다.

**게이트**: `python3 docs/script/check_links.py docs`가 위반 0건(exit 0)이어야 한다.

### 4.1 원본에 이미 깨져 있는 링크 11건

두 문서가 다른 디렉토리에서 작성된 뒤 옮겨지면서 상대 깊이가 따라가지 않았다.

- `plans/2026-08-05-orchestration-session-pipeline.md` — 스펙을 `parfait/specs/`로 가리킨다(1건)
- `plans/archive/2026-08-01-parfait-api-contract-docs.md` — API 계약 문서를 같은 디렉토리 형제나 `api/` 하위로 가리킨다(10건). 이 문서가 `parfait/api/`에서 작성된 흔적이다

이관이 만든 문제가 아니지만, 게이트가 exit 0을 내야 하므로 **사본에서 고친다.** 원본 저장소는
읽기 전용이라 그쪽은 깨진 채로 남는다. 목적지 기준 경로가 원본 기준 정답과 우연히 같다 —
`docs/superpowers/plans/archive/`에서 `docs/api/`까지도 `../../../api/`다.

이것이 사본이 원본과 바이트 단위로 같지 않게 되는 첫 지점이다. §5의 드리프트가 0에서
시작하지 않는다는 뜻이고, 의도한 것이다.

### 4.2 `parfait/index.md`가 가리키는 이관 범위 밖 경로

허브 문서 하나가 이관되지 않는 디렉토리를 여섯 번 가리킨다. 전수 스캔으로 확인한 전부다.

| 링크 | 대상 | 처리 |
|---|---|---|
| `pm/README.md` 2건 | `parfait/pm/` | 링크 해제. 이관 범위 밖 |
| `blog/README.md` 2건 | `parfait/blog/` | 링크 해제. 이관 범위 밖 |
| `android/` 디렉토리 링크 1건 | `parfait/android` 디렉토리 | 링크 해제. 목적지에서 `superpowers`·`adr`·`architecture`로 흩어져 대응하는 한 경로가 없다 |
| `script/README.md` 1건 | `parfait/script/` | **매핑.** `docs/script/README.md`가 실제로 있다 |

디렉토리 링크는 접두사 해제로 처리할 수 없다 — `parfait/android`를 접두사로 넣으면 그
하위 235개 문서의 링크가 전부 해제된다. 그래서 정확일치 목록(`UNLINK_EXACT`)을 따로 둔다.

이 다섯 건의 해제는 §2.3의 위키 개념 7건과 성격이 다르다. 위키 쪽은 나중에 ingest하면
되살릴 링크이고, 이쪽은 **되살릴 계획이 없는 의도된 제거**다. 남은 텍스트 줄은 §3의
`index.md` 정리가 걷어낸다. 따라서 미결로 등록하지 않는다.

### 4.3 쌍 목록은 원본에서 만든다

`docs/superpowers/`에는 이관분이 아닌 문서가 이미 4개 있다(2026-09-17 위키 이식 스펙·계획과
이 이관의 스펙·계획). 복사 뒤 **목적지**를 훑어 재작성 대상을 모으면 그 4개까지 쓸려 들어가,
존재하지 않는 원본 경로와 짝지어진다.

그래서 쌍 목록은 **원본 트리를 열거해** 만든다. 목적지에 무엇이 이미 있든 영향을 받지 않고,
파일 수도 정확히 255가 된다.

이것이 조용한 파손이 될 수 있었던 이유는 `specs`와 `plans`가 이관 전후로 깊이가 같다는
점이다 — 그 4개 문서의 상대 링크가 우연히 재작성 전후로 동일해서 내용이 바뀌지 않았다.
깊이가 달라지는 디렉토리였다면 무관한 문서의 링크가 조용히 망가졌을 것이다.

## 5. 미러의 대가

원본 저장소를 고치지 않으므로 같은 문서가 두 곳에 산다. 한쪽을 고치면 다른 쪽이 낡는다.

이 설계는 그 드리프트를 **막지 않는다.** 자동 동기화를 넣으면 원본 저장소를 건드려야 하고,
그것이 범위 밖이기 때문이다. 대신 드리프트를 **볼 수 있게** 둔다 — 이관 완료 시점의
커밋 SHA를 `docs/index.md` 머리에 적어, 나중에 원본과 비교할 기준점을 남긴다.

이 저장소의 사본이 앞으로의 정본이다. 새 스펙·계획·ADR은 여기에 쓴다.

## 6. 범위 밖

- 원본 저장소(`team-yg-pesonal-agent`)의 모든 파일
- `parfait/pm/`, `parfait/blog/`, `parfait/bot/`
- `team-yg-antigravity-playground` 저장소
- 위키 concepts 추가 ingest (2.3에서 별도 작업으로 분리)
- Gradle 빌드·CI 설정. 이관분은 파일 추가일 뿐 빌드에 닿지 않는다

## 7. 검증

1. `python3 docs/script/check_links.py docs` — exit 0
2. `python3 docs/script/test_search.py` (또는 pytest) — 개작된 수집기가 네 디렉토리에서
   문서를 찾고, `id`·`title`·`tags`로 순위를 매기는지
3. `python3 docs/script/test_check_links.py`
4. 이관 파일 수가 원본과 일치하는지 (2.1에서 제외한 4개 제외)
5. `./gradlew build`가 이관 전과 동일하게 통과 — 문서 추가가 빌드에 영향이 없음을 확인
