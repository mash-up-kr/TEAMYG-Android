# cascade-delete 절차

**전제**: 사용자가 `wiki/raw/`에서 파일을 직접 지운 뒤 이 절차를 호출한다. LLM은
`wiki/raw/`의 어떤 파일도 삭제하지 않는다(`conventions.md` §10) — 삭제를 제안할
수는 있지만 실행은 사람이 한다.

## 1. 연결된 위키 페이지 찾기

삭제된 원본의 stem을 `<stem>`이라 할 때, 연결된 페이지를 세 가지 방법으로
찾는다. 하나만 보면 놓치는 페이지가 생긴다.

- frontmatter `sources: [...]`에 `src-<stem>`이 들어 있는 `concepts/`·
  `entities/` 페이지
- 소스 요약 페이지 `sources/src-<stem>.md` 그 자체
- 본문에서 `[[src-<stem>]]`을 인용하는 페이지 전체(`wiki/` 전역 검색)

## 2. 소스 요약 페이지 삭제

`sources/src-<stem>.md`를 삭제한다.

## 3. 공유 페이지는 보존한다 — 사용자 확인 필요

1단계에서 찾은 `concepts/`·`entities/` 페이지 중 **여러 소스에 걸친 페이지는
삭제하지 않는다.** frontmatter `sources[]`에서 이번에 지워진 소스의 stem만
빼낸다.

- 뺀 결과 `sources[]`가 비게 되는 페이지만 삭제 후보다.
- 후보 목록과 각 페이지 내용을 사용자에게 제시하고, 삭제 여부를 확인받은
  뒤에만 실제로 지운다. 임의로 삭제하지 않는다.

## 4. 카탈로그에서 제거

3번에서 삭제하기로 확정된 페이지를 `wiki/pages/index.md`에서 제거한다.

## 5. 남은 wikilink 정리

삭제가 확정된 페이지를 가리키는 `[[wikilink]]`를 남은 모든 페이지 본문에서
제거하거나 대체 서술로 바꾼다. 방치하면 `깨진링크` 위반이 남는다.

## 6. 판본 체인 재연결 — `partial`이 끼면 사용자 확인 필요

삭제된 소스가 판본 체인(`supersedes`/`superseded_by`)에 있었다면 체인을
다시 잇는다. 양방향 일치와 `current` 도달 조건은 `conventions.md` §2 그대로
적용된다.

- 체인에서 삭제된 소스의 양옆 중 **하나라도 `status: partial`이면 자동으로
  다시 잇지 않는다.** `partial`의 `scope`는 자유 서술이라 기계적 병합 규칙이
  없다. 남은 두 판본의 `scope` 원문을 사용자에게 그대로 제시하고 새 관계를
  결정받는다.
- `partial`이 끼지 않은 단순 체인(예: A가 B를 대체하고 B가 C를 대체하는데
  B가 삭제됨)은 A와 C를 직접 잇는다.

## 7. 매니페스트 정리

`python3 wiki/script/ingest_cache.py --prune`을 실행해 `wiki/raw/.manifest.json`
에서 더 이상 존재하지 않는 원본 항목을 뺀다.

## 8. 미결 항목 인용 정리

삭제된 소스를 인용하던 `wiki/open-questions.md` 항목을 찾는다. 그 미결이
삭제로 무효가 되었으면 `상태`를 갱신하고, 아니면 인용을 고쳐 쓴다.

## 9. 기록과 검증

`wiki/log.md`에 삭제 내역(무엇을, 왜)을 기록한 뒤 아래 둘 다 종료 코드 0을
확인한다.

```bash
python3 wiki/script/lint.py
python3 wiki/script/check_status.py
```

둘 중 하나라도 위반이 남으면 절차가 끝난 게 아니다. 5번(잔여 링크)이나
6번(판본 체인)이 빠졌을 가능성이 가장 크다. 코드별 대응은
`wiki/references/lint-rules.md`를 본다.

그 다음 그래프를 1회 다시 만든다 — 시점과 명령은 `wiki/CLAUDE.md`의 "작업 후"
절이 정본이다. 삭제는 그래프를 반드시 어긋나게 만든다: 없어진 페이지가
매니페스트에 남아 `그래프stale`이 뜨고, 그 동안 나머지 세 그래프 신호는 판정
자체가 생략된다 — `research.md`가 Deep Research 입력으로 요구하는
`그래프고립`·`희소커뮤니티`가 영구히 빈 채로 남는다.

## 자동 처리 vs 사용자 확인

| 단계 | 처리 |
|---|---|
| 1, 2, 4, 5, 7, 8, 9 | 자동 |
| 3 (공유 페이지의 삭제 여부) | 사용자 확인 필요 |
| 6 (`partial`이 낀 판본 체인 재연결) | 사용자 확인 필요 |

## 연관

- `wiki/conventions.md` §2(판본 상태), §3(미결 항목), §10(raw/ 규칙)
- `wiki/script/ingest_cache.py --prune`
- `wiki/script/lint.py`, `wiki/script/check_status.py`
- `wiki/references/lint-rules.md`
