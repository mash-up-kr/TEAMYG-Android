# 작업 로그

append-only다. 앞 항목을 고치지 않는다.

## [2026-09-17] setup | 위키 체계 이식

- `huihun-private-wiki`의 위키 체계를 단일 도메인 평탄 구조로 옮겼다. 설계는
  `docs/superpowers/specs/2026-09-17-wiki-port-design.md`에 있다
- 도메인 차원 제거 — 콘텐츠는 `wiki/pages/` 아래, 구조 파일은 `purpose`·`overview`·
  `index` 한 벌
- 스크립트 6개 이식: `route` `lint` `check_status` `ingest_cache` `wikilib`
  `graph_signals`. `sync_issues.py`는 가져오지 않았다 — 팀 공용 저장소라 이슈를
  자동 생성하면 안 된다. 딸려서 `lint`의 `check_issue_field`와 계약의 `issue`
  필드도 뺐다
- Obsidian 제외. `[[이름]]` 링크 규약과 세 검사는 유지하되 근거를 `route.py` seed
  확장의 결정성으로 다시 썼다
- graphify 산출물은 `wiki/graphify-out/`, 스캔 루트는 `wiki/pages/`
- 콘텐츠는 아직 0건이다. 첫 ingest는 별도 작업
- 검증: `pytest` 전건 통과, `lint.py` 위반 0건, `check_status.py` 위반 0건,
  라우팅 6개 의도 0 / `unclear` 3 / 삭제된 `add-domain` 4
