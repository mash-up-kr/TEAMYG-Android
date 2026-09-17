# research 절차 (Deep Research)

lint와 그래프 신호는 무엇이 비어 있는지 찾아낼 뿐이다. 이 문서 없이 끝나면 그
탐지는 보고서 한 줄로 죽는다. 이 절차는 공백 → 검색 → 원본 저장 → 일반
ingest → 미결 갱신까지 이어서, 탐지가 실제로 위키에 되먹여지게 한다.

절차가 막아야 하는 실패는 둘이다.

- **출처 없는 위키 페이지.** 검색 결과가 `wiki/raw/`에 내려앉기 전에 위키 페이지로
  바로 변하면 `sources[]`가 가리키는 원본이 없고, `check_raw_sync`가 깨지고,
  나중에 어떤 주장이 무엇에 근거했는지 아무도 확인할 수 없다. 그래서 5·6단계가
  먼저 오고, 7단계는 페이지를 직접 쓰지 않고 일반 ingest 경로로 넘긴다.
- **확인 없는 검색.** 검색 주제를 LLM이 스스로 짓고 스스로 실행하면 이 모델이
  흥미로워하는 방향으로 위키가 편향된다. 3단계는 하드 게이트다 — 주제와
  검색어를 보여주고, 편집을 받고, 확인 전에는 검색하지 않는다.

## 1. 입력

아래 세 신호를 모은다. 하나만 보고 시작하지 않는다.

- `python3 wiki/script/lint.py` 출력의 "데이터 공백" 판단. lint.py 자신이
  밝히듯 데이터 공백은 기계 검사 밖이라 코드로 나오지 않는다 — `wiki/pages/index.md`·
  `wiki/pages/overview.md`를 `wiki/pages/purpose.md`의 핵심 질문과 대조해서
  사람/LLM이 읽어야 한다.
- `wiki/script/graph_signals.py`가 내는 `그래프고립`·`희소커뮤니티` 경고. 코드
  판정 기준은 `wiki/references/lint-rules.md`를 본다.
- `wiki/open-questions.md`에서 `action: research`로 표시된 항목.
  `action` 허용값의 정의는 `wiki/conventions.md` §3이 정본이다.

## 2. 주제 생성

`wiki/pages/purpose.md`·`wiki/pages/overview.md`를 읽는다. 여기서 뽑는 검색
주제는 **공백 특화**여야 한다 — 일반 키워드가 아니라 1단계에서 모은 공백이
`purpose`의 핵심 질문 중 무엇에 답하지 못하고 있는지에 맞춘 주제다. "이
개념에 대해 검색" 같은 넓은 주제는 반려하고 다시 좁힌다.

## 3. 사용자 확인 게이트

2단계에서 뽑은 주제와, 각 주제에 실제로 넣을 검색어를 사용자에게 그대로
보여준다. 사용자가 항목을 지우거나 검색어를 고칠 수 있어야 한다. **확인을
받기 전에는 4단계로 넘어가지 않는다.** 침묵이나 다음 지시를 확인으로 해석하지
않는다.

## 4. 검색 수행

3단계에서 확정된 검색어로만 검색한다. 확정되지 않은 주제, 검색 도중 새로
떠오른 주제는 이번 회차에서 실행하지 않는다 — 필요하면 다음 회차의 1단계
입력으로 남긴다.

## 5. 원본 저장

검색 결과를 위키 페이지로 바로 옮기지 않는다. 저장 위치와 파일 머리부에 무엇을
적는지는 `wiki/conventions.md` §10이 정본이며 여기서 되풀이하지 않는다 — 이
절차에서 지켜야 할 것은 순서뿐이다: 페이지를 만들기 전에 반드시 먼저 원본을
저장한다.

## 6. 매니페스트 등록

```bash
python3 wiki/script/ingest_cache.py --record <wiki/raw 상대경로>
```

5단계에서 저장한 파일마다 실행한다. 등록하지 않으면 `ingest_cache.py --list`가
이 파일을 계속 미처리로 잡거나, 7단계 ingest가 끝난 뒤 `매니페스트` 위반으로
남는다.

## 7. 일반 ingest로 처리

저장한 원본을 다른 `wiki/raw/` 소스와 동일하게 취급한다. research 전용 페이지
생성 경로를 따로 두지 않는다. 절차는 `wiki/references/ingest-checklist.md`가
정본이며 여기서 되풀이하지 않는다 — 소스 하나씩 분석 단계부터 시작해서 그
문서를 그대로 따른다.

## 8. 미결 갱신

7단계 ingest로 메운 공백이 `wiki/open-questions.md`의 기존 항목에 해당하면
그 항목의 `상태`를 갱신한다. 형식 규약은 `wiki/conventions.md` §3이 정본이며
여기서 되풀이하지 않는다.

## 멈출 조건

검색 결과가 기존 위키 서술과 정면으로 상충하는데, 그 결과만으로는 어느 쪽이
옳은지 판정할 데이터가 없을 때는 **페이지를 고치지 않는다.** 대신
`wiki/open-questions.md`에 새 항목을 만들어 두 주장을 나란히 적고
`action: research`로 남긴다. 같은 판단은 7단계 ingest 안에서 소스 단위로도
반복된다 — `ingest-checklist.md`의 멈출 조건이 그 지점을 담당한다.

## 연관

- `wiki/conventions.md` §3(미결 항목), §10(raw/ 규칙)
- `wiki/references/ingest-checklist.md`, `wiki/references/lint-rules.md`
- `wiki/pages/purpose.md`, `wiki/pages/overview.md`, `wiki/open-questions.md`
- `wiki/script/lint.py`, `wiki/script/graph_signals.py`,
  `wiki/script/ingest_cache.py`
- `wiki/graphify-out/GRAPH_REPORT.md`
