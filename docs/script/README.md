# docs/script

이 repo의 **파이썬 툴링 스크립트 홈**. 스킬(`.claude/skills/*`)이 호출하는 로직·일회성 유틸을 모은다.

## 규약
- **stdlib 전용** — pip 의존성 0. `python3 docs/script/<name>.py`로 실행.
- 파일명은 기능 기반 snake/kebab(날짜 접두사 없음).
- 각 스크립트 상단은 [`_script-template.py`](_script-template.py) 헤더 규약(용법 docstring)을 따른다.
- **경로**: repo 루트 = `Path(__file__).resolve().parents[2]`(= `docs/script/x.py` → 루트) 기준 상대. repo 이동에 무관.
- 스킬이 호출하는 스크립트는 SKILL.md에서 `python3 docs/script/<name>.py`로 참조(cwd = repo 루트).
- 테스트는 같은 디렉토리에 `test_<name>.py`(`python3 -m unittest`, stdlib `unittest`).

## 인덱스
| 스크립트 | 용도 | 호출 스킬 |
|---|---|---|
| `check_links.py` | 마크다운 상대 링크 전수 resolve 검사(디렉토리 이동·아카이브 이동 후 `../` 깊이 확인) | _(없음 — 손으로 실행)_ |
| `search.py` | 자연어 쿼리로 구현 문서(스펙·계획·ADR·아키텍처·API 계약·synthesis) 검색 | _(없음 — 손으로 실행)_ |

## search

`docs/superpowers/specs`·`docs/superpowers/plans`·`docs/adr`·`docs/architecture`·
`docs/api`·`docs/synthesis` 여섯 디렉토리를 재귀로 훑어 각 마크다운의 frontmatter(`id`·`title`·`tags`·
`related_code`·`related_spec`·`related_adr`)와 헤딩을 색인한다. `archive/`
하위 문서도 검색 대상에 포함하고, 결과에 `[archived]`로 표시한다 — 아카이브가
과거 판단의 근거를 담은 문서 대다수라서 제외하면 검색의 요점을 잃는다.
frontmatter가 없는 파일(`README.md`, `template.md` 등)은 죽지 않고 파일명
stem을 `id`로, 빈 문자열을 `title`로 삼는다.

graphify는 `wiki/pages/`만 스캔하므로 `docs/` 아래 문서는 그 검색망 밖에
있다. `search.py`가 그 구멍을 메운다.

```bash
python3 docs/script/search.py "<자연어 쿼리>" [--top N]
```

점수 내림차순으로 `<id>  (score N) [archived] — <title>`과 상대 경로를 출력한다.

## 템플릿
- [`_script-template.py`](_script-template.py) — 파이썬 스크립트 헤더/경로 규약.
