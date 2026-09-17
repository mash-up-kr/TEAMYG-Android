# 위키 운영 진입점

**위키 작업(ingest/query/lint/research/delete-source 중 하나, 또는
`wiki/` 아래 파일 편집) 을 시작하기 전에는 반드시 `wiki/script/route.py`를
돌린다.** "자체 지식으로 처리할 수 있을 것 같다"는 판단으로 이 단계를 건너뛰지
않는다 — 그 판단 자체가 확률적이고, 이 문서의 목적은 그것을 확정적 의무로
바꾸는 데 있다. 데이터 계약은 여기서 되풀이하지 않는다. 정본은
`wiki/conventions.md`다.

## 라우팅 절차 — 6단계

1. 요청을 아래 **의도 7종** 중 하나로 분류한다. 확신이 서지 않으면
   `unclear`로 둔다.
2. 아래 명령으로 `required`·`reference` 후보를 받는다.

   ```
   python3 wiki/script/route.py --intent <의도> [--seed <저장소상대경로> ...] [--json]
   ```

3. graphify가 설치돼 있으면 `graphify query "<노드명>"`으로 관계를 보완한다.
   요청 표현을 그대로 검색어로 쓰지 않는다 — `graph.json`에 실재하는 노드명만
   골라 쓰고, 비슷한 말을 지어내지 않는다. 매칭되는 노드가 없으면 "관련
   어휘를 찾지 못했다"고 밝히고 그래프 검색을 멈춘 뒤 라우팅 맵 결과만으로
   진행한다. 매칭된 결과 페이지는 seed 후보에 더한다. **노드명은 경로가
   아니다** — `--seed`로 넘기기 전에 저장소 상대 경로로 바꾼다. 변환은
   출력 줄의 `src=` 값(스캔 루트 `wiki/pages` 기준 상대경로) 앞에
   `wiki/pages/`를 붙이는 것이다: `src=concepts/개념.md` →
   `wiki/pages/concepts/개념.md`. `src=`가 없으면 그 후보를 버린다. 경로가
   아닌 값을 넘기면 `route.py`가 종료 코드 4로 막는다.
4. seed(`required` 페이지, `--seed`로 넘긴 페이지, 3번 결과)가 정해지면
   `--seed <저장소상대경로>`를 붙여 같은 명령을 다시 돌려 출처 중복 확장을
   받는다.
5. `required`만 읽는다. `reference`는 필요할 때만 연다. 이번 작업에 적용할
   규약이 무엇인지 이 시점에 명시한다.
6. 작업이 끝나면 "작업 후" 절차를 따른다.

## 의도 7종 — 판정 기준

- **ingest** — `wiki/raw/`의 새 원본을 위키 페이지로 통합하는 요청.
- **query** — 기존 위키 내용을 조회·답하는 요청. 페이지를 만들거나 고치지
  않는다.
- **research** — 위키에 없는 지식을 웹 검색으로 메우려는 요청. 결과는
  `wiki/raw/`에 먼저 저장한 뒤 ingest로 이어진다.
- **delete-source** — 사용자가 `wiki/raw/`에서 원본을 이미 지웠고, 연결된
  위키 페이지를 정리해야 하는 요청.
- **lint** — 콘텐츠를 바꾸지 않고 위반 여부만 점검하는 요청.
- **schema-change** — `wiki/conventions.md`가 정의하는 데이터 계약 자체
  (필드, 값 집합, 디렉토리 배치 등)를 바꾸는 요청.
- **unclear** — 위 여섯 중 어디에도 확신이 서지 않는 요청.

## 멈출 조건

`route.py`의 종료 코드가 아래 두 코드(3, 4) 중 하나면 **진행하지 않고
사용자에게 확인한다.** 4 아래 나열된 항목들은 별개의 정지 조건이 아니라 4
하나가 갈리는 원인들이다.

- **3** — 의도가 `unclear`다. 의도를 짐작으로 채우지 않는다. 단, `--seed`를
  함께 넘겼는데 그 seed가 해석되지 않으면 3이 아니라 4가 뜬다 — seed 오류가
  의도 불명보다 먼저, 그리고 우선해서 걸린다(아래 2번째 항목).
- **4** — 라우팅 입력이 잘못됐다. 표준 출력이 아니라 표준 오류의 메시지를 읽고
  원인을 가린다.
  - 의도를 모른다 → 오타면 정정하고, 허용된 7종 중에서 다시 고른다.
  - `--seed`가 수집된 페이지가 아니다 → 저장소 상대 경로로 고친다. graphify
    노드명은 경로가 아니므로 그대로 넘기지 않는다(3단계). 의도가 `unclear`라도
    이 검사는 건너뛰지 않는다.
  - `routing.json`이 실재하지 않는 경로를 가리키거나 `expansion` 블록이
    불완전하다 → 맵을 고친다.
  - `routing.json` 자체가 없거나 JSON 문법이 깨졌다 → 파일 경로와 문법을
    확인한다. 이 경우도 트레이스백이 아니라 이 코드로 끝난다.

(참고: **0**은 정상, **2**는 저장소 루트가 아닌 곳에서 실행했다는 뜻이다 —
저장소 루트로 이동한 뒤 다시 돈다.)

## 읽기 폭

`route.py` 출력의 `required`가 7개를 넘으면 라우팅이 과하다는 신호다(스크립트가
직접 이 임계치로 경고를 낸다) — 그래도 읽기는 진행하되, 아래 "라우팅 실패 기록"
절차를 따른다.

## 작업 후

`python3 wiki/script/lint.py`로 위반 0건을 확인한다.

PR을 올리기 전 로컬에서 graphify를 1회 수동 실행하고 `wiki/graphify-out/`
산출물까지 함께 커밋한다.

    graphify extract ./wiki/pages

백엔드는 실행하는 환경에 맞춰 고른다. 셋 중 하나다 — 로컬 `ollama`
(`--backend ollama`), `OPENAI_BASE_URL`을 로컬 서버로 돌린 `openai` 백엔드,
또는 API 키를 쓰는 원격 백엔드. 앞의 둘은 API 키가 필요 없다.
`graphify update`와 `--code-only`는 코드 전용이라 이 위키에는 쓰지 않는다.

`graph.json`이 아직 없으면 `lint.py`의 그래프 신호 4종은 아예 뜨지 않는다.
백엔드가 없는 환경에서도 lint는 정상 동작한다.

## 라우팅 실패 기록

작업 중 라우팅 맵이 놓친 문서를 열었거나, 반대로 필요 없는 문서까지
`required`로 끌려왔다면 `wiki/routing-misses.md`에 적고
`wiki/routing.json`을 고친다. 기록하지 않으면 같은 실패가 반복된다.

## 더 읽을 곳

- **데이터 계약(필드, 값, 디렉토리 배치, frontmatter, 민감 데이터 등급)** —
  `wiki/conventions.md`가 정본이다. 이 문서와 어긋나면 `conventions.md`가
  옳다.
- **긴 절차** — `wiki/references/ingest-checklist.md`,
  `wiki/references/cascade-delete.md`, `wiki/references/lint-rules.md`,
  `wiki/references/research.md`. 해당 의도로 라우팅하면 `route.py`가 알맞은
  파일을 `reference`로 알려준다.
- **설계 원리** — `docs/superpowers/specs/2026-09-17-wiki-port-design.md`.
  스키마 자체를 바꿀 때(`schema-change`)가 아니면 열 필요 없다.
