# 데이터 계약

이 문서가 정본이다. `wiki/CLAUDE.md`는 이 계약을 참조하며 중복 서술하지 않는다.
검사 스크립트(`wiki/script/lint.py`, `wiki/script/check_status.py`)가 이 계약을
기계적으로 강제한다. 두 스크립트는 위반이 있으면 종료 코드 1을 반환한다.
저장소 루트가 아닌 곳에서 실행하면 검사 대상이 0건이 되어 위반을 통과로 오인하므로,
`wiki/`와 `wiki/conventions.md`가 보이지 않으면 검사하지 않고 2를 반환한다.

## 1. 근거 우선순위

`wiki/pages/index.md` → `concepts/`·`entities/` → `wiki/open-questions.md`(필수)
→ 필요 시 `sources/`.

근거를 `sources/`로 한정하거나 `sources/`를 `concepts/`보다 우선하지 않는다. 컴파일
결과를 버리고 원본 조각에서 매번 재유도하는 것이며, 이 위키가 피하려는 방식이다.

## 2. 판본 상태 (`status`)

`sources/` 페이지에 필수. 값은 `current`·`superseded`·`partial` 셋뿐이다.

- `superseded`·`partial`은 `superseded_by` 필수, `partial`은 `scope` 추가 필수.
- `superseded_by`를 따라가면 반드시 `status: current`에 도달해야 한다.
- `supersedes` ↔ `superseded_by`는 양방향이 일치해야 한다.
- `wiki/pages/index.md`의 표기는 이 필드의 투영이다. 어긋나면 frontmatter가 정답이다.
  - `current` → `**현행 정본**`
  - `superseded` → `🔁`. `현행 정본`을 함께 붙이면 위반이다.
  - `partial` → `**현행 정본**`과 `🔁`를 **둘 다** 적는다. "일부는 현행, 일부는 대체"라는
    뜻이라 한쪽만으로는 상태를 표현할 수 없다.

## 3. 미결 항목

`wiki/open-questions.md`가 단일 추적 장소다. 항목 형식:

    ### [YYYY-MM-DD] 주제 요약
    - **출처 A**: [[페이지A]] — 주장 요약
    - **출처 B**: [[페이지B]] — 상충 주장 요약
    - **상태**: 미해결
    - **action**: research
    - **해소 메모**: (해소된 경우 어느 쪽이 옳고 왜인지)

`상태`는 자유 서술이다. `해소됨`이 아닌 모든 값은 미결로 본다.
`action`은 `create-page`·`research`·`skip` 셋 중 하나여야 한다. 열거형으로 제한하는
이유는 매번 다른 후속 작업이 지어내지는 것을 막기 위해서다.

## 4. 파일명과 링크

- 위키 내부 링크는 `[[파일명]]` 형식만 쓴다. 경로 접두사는 금지다.
- 파일명은 `wiki/` 안에서 유일해야 한다. `route.py`의 seed 확장과 `lint.py`의
  링크 검사가 `[[이름]]`을 파일 하나로 확정할 수 있어야 하기 때문이다. 이름이
  겹치면 링크가 깨지지 않고 아무 파일로나 해석된다 — 조용히 틀리는 것이
  깨지는 것보다 나쁘다.
  - 예외: 런타임이 이름을 정하는 진입 파일(`wiki/CLAUDE.md`,
    `wiki/conventions.md`, `wiki/routing-misses.md`)은 **서로 간에만** 같은
    stem을 허용한다. 이들은 수집 대상이 아니라 경로로만 참조된다.
  - 검사 범위는 `wiki/` 안으로 한정한다. 저장소의 안드로이드 쪽 마크다운
    (`README.md`가 `/`·`http/`·`tools/build-cache-bench/` 세 곳에 있다)은
    `[[이름]]` 링크 대상이 아니다.
- 구조 파일: `wiki/pages/purpose.md`, `wiki/pages/index.md`, `wiki/pages/overview.md` 셋뿐이다(§5 참조).
- 소스 요약 페이지: `src-<원본stem>.md`. 원본과 이름이 겹치지 않게 하는 접두사다.
- 파일명은 영문 kebab-case가 기본. 한국 인물·장소 등 고유명사는 한글 파일명을 허용한다.
- 한글 파일명 비교는 항상 NFC 정규화 후 한다.

## 5. 디렉토리 배치

최종 배치는 다음과 같다(단일 도메인 평탄 구조 — 도메인 디렉토리가 없다):

```
wiki/
  CLAUDE.md  conventions.md  routing.json  log.md
  open-questions.md  routing-misses.md
  references/  templates/  script/  raw/  graphify-out/
  pages/
    purpose.md  overview.md  index.md
    sources/  concepts/  entities/  queries/  synthesis/
```

`wiki/pages/` 아래에 올 수 있는 마크다운은 두 가지뿐이다.

- 루트: `purpose.md`, `index.md`, `overview.md` 셋만.
- `sources/`·`concepts/`·`entities/`·`queries/`·`synthesis/` **바로 아래**. 하위 폴더를
  더 파지 않는다. `sources/`에 두는 페이지는 stem이 `src-`로 시작해야 한다.

그 밖의 위치는 위반이다. 모든 콘텐츠 검사(frontmatter·raw 정합·판본 상태)가 부모
디렉토리 이름으로 대상을 고르기 때문이다. `concepts/기타/x.md`나 `wiki/pages/` 루트에
흘린 파일은 어느 검사에도 걸리지 않으면서 인바운드 링크 1건만으로 합법이 된다.
배치가 곧 검사 대상 선정이라, 배치를 강제하지 않으면 나머지 규칙 전체가 우회
가능해진다.

`wiki/graphify-out/`은 수집 대상이 아니고 파일명 유일성 검사에도 들어가지 않는다.
graphify가 쓰는 산출물(`graph.json`·`manifest.json`·`GRAPH_REPORT.md` 등)이 위키
페이지와 이름이 겹치는 것은 위키의 문제가 아니라 생성물의 문제이며, 여기서 위반으로
띄우면 사람이 고칠 수 없는 경고가 영구히 남는다. 같은 이유로 `wiki/graphify-out/`은
graphify의 스캔 루트(`graph_signals.SCAN_ROOT` = `wiki/pages`) **밖에** 있어야 한다 —
산출물이 스캔 루트 안에 있으면 그래프를 다시 만들 때마다 산출물 자신이 콘텐츠로
스캔되어 순환이 생긴다.

## 6. frontmatter 필수 필드

콘텐츠 디렉토리 다섯 곳과 구조 파일 셋(`wiki/pages/purpose.md`·`wiki/pages/index.md`·
`wiki/pages/overview.md`)에 frontmatter가 필수다. `wiki/log.md`는 append-only 로그라
예외다.

| 필드 | 필수 범위 | 값 |
|---|---|---|
| `tags` | 위 전부 | 리스트 |
| `updated` | 위 전부 | `YYYY-MM-DD` |
| `sources` | `sources/`·`concepts/`·`entities/` | 리스트 |
| `status` | `sources/` | §2 참조 |

`queries/`와 `synthesis/`는 `sources`가 면제다. 위키에서 파생된 산출물이라 원본
출처가 아니라 본문 링크가 근거이기 때문이다.

**`sources` 필드는 디렉토리마다 뜻이 다르다.** 같은 이름이지만 가리키는 대상이 다르다.

- `sources/` 페이지의 `sources`는 **raw 원본 파일명**이다 (`[정책.md]`,
  `wiki/templates/source.md` 참조). `wiki/raw/` 아래 실제 파일과 stem 하나로 1:1
  대응해야 한다(§10) — 도메인 구분은 없다.
- `concepts/`·`entities/` 페이지의 `sources`는 **`src-` 페이지 stem**이다
  (`[src-정책]`, `wiki/templates/concept.md` 참조). 선언한 stem은 **`wiki/pages/sources/`에
  실제로 존재해야** 한다.

`concepts/`·`entities/` 본문이 `[[src-...]]`를 인용했다면 그 stem이 `sources`에도
있어야 한다. 두 방향을 모두 보는 이유는, 한쪽만 보면 "출처 없이 인용"과 "존재하지 않는
출처 선언"이 각각 반대편 구멍으로 빠져나가기 때문이다.

**키만 있고 값이 비면 충족이 아니다.** `tags:` 뒤에 아무것도 없는 상태는 누락과 같게
취급한다. 리스트는 인라인(`tags: [a, b]`)과 블록 형식을 모두 받는다.

    tags:
      - a
      - b

블록 형식을 받는 이유는 사람이 frontmatter를 직접 편집할 때 에디터가 리스트를 이
형식으로 다시 쓰는 경우가 흔하기 때문이다. 사람이 편집한 페이지가 형식 때문에 빈
값으로 읽히면, 태그도 출처도 없는 페이지가 검사를 통과한다.

## 7. 인바운드 링크

- **인바운드 0건은 위반이다.** 어디서도 링크되지 않은 페이지는 검색으로만 닿는다.
  이 위키는 링크를 따라 읽히도록 만들어졌고, 고아 페이지는 없는 것과 같다.
- **인바운드 1건은 경고다.** 위반은 아니지만 개념망에 한 줄로만 매달려 있다는 뜻이라
  편입을 검토한다.

면제 대상은 다음과 같다(위반·경고 둘 다 면제).

- `queries/` 아래 전부 — 위키에서 파생된 질의 응답 산출물이라 인바운드가 없는 것이 정상이다.
- `wiki/log.md`, `wiki/open-questions.md`, `wiki/pages/index.md`, `wiki/pages/purpose.md`,
  `wiki/pages/overview.md` 다섯 경로.

## 8. 페이지 구성

- 본문 섹션 구성은 자유다. 해당 없는 섹션은 지운다.
- 말미 연결 섹션은 `## 연관`으로 고정한다. `## 관련`·`## 참고`를 쓰지 않는다.
- 미확정 항목 섹션은 `## 미결`로 고정한다. 반드시 `open-questions.md`에 등록하고 링크한다.
- 판본이 걸린 섹션 제목엔 `(vX 현행, [[소스]])`를 표기한다.
- 상충 인라인 마커: `> ⚠️ [YYYY-MM-DD] [[대상]] 와 상충 — 무엇이 어긋나는지 → [[open-questions]]`

## 9. 민감 데이터 등급

이 저장소는 private이다. 그래도 자격증명은 유출 시 피해가 저장소 밖으로 나간다.

| 패턴 | 등급 | 처리 |
|---|---|---|
| API 키·secret·password·token·JWT·private key | 위반 | 커밋 차단. 자동 수정 금지, 사용자에게 보고 |
| 이메일·전화번호·주민등록번호 | 경고 | 보고만 |
| 로컬 절대경로 | 경고 | 보고만 |

개인 식별 정보는 이 위키의 정당한 콘텐츠이므로 경고로 둔다.

검사 범위는 `wiki/` 페이지와 `wiki/raw/` 원본 **둘 다**이다. `wiki/raw/`는 손대지 않은
제3자 원본이 쌓이는 곳이라 붙여넣은 자격증명이 가장 먼저 닿는 자리다. 거기를 안 보면
"커밋 차단"이 표적을 비껴간다.

## 10. raw/ 규칙

- `wiki/raw/`의 **기존 파일은 불변**이다. 수정·삭제하지 않는다.
- **추가는 Deep Research 산출물에 한한다.** `wiki/raw/research/`에만 쓰고, 파일
  앞부분에 취득 일자·질의어·URL을 기록한다.
- LLM은 `wiki/raw/`의 어떤 파일도 삭제하지 않는다. 삭제는 사람이 한다.
- `wiki/raw/**/X.md`와 `wiki/pages/sources/src-X.md`는 stem 하나로 1:1 대응한다 —
  도메인 구분은 없다.
