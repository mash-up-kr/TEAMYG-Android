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

## [2026-09-17] ingest | 기능정의서_MVP v2

- 원본: `wiki/raw/기능정의서-v2.md` (2026-05-30자, Notion 캡처 2장 전사, 원본
  페이지는 게스트 권한이라 접근 불가)
- 첫 콘텐츠 ingest다. 이전까지 위키 콘텐츠는 0건이었다
- 소스 1건: `src-기능정의서-v2` (`status: current`, 비교 대상 판본이 없어
  `supersedes`/`superseded_by` 없음)
- 엔티티 1건: `parfait`
- 개념 5건: `canvas` `group` `cutout-placement-pipeline` `screen-id-scheme`
  `mvp-scope-limits`
- synthesis는 만들지 않았다 — 소스가 하나라 가로지르는 비교 분석이 나오지 않는다
- 화면 34건 중 단발 언급 화면은 페이지로 만들지 않고 관련 개념 본문 서술로 남겼다
- `purpose.md`가 예고한 "화면 ID 체계 개념 페이지"를 만들고 거기서 링크를 걸었다
- open-questions 8건 등록(전부 `action: research`). 소스가 "정의 필요"로 남긴 정책
  16건을 주제별로 묶은 것과, 소스 자신이 밝힌 공백(`C-002` 미정의, 세부 열 공백
  4건), 판본 번호 불일치(제목 v2 / 내부 DB 제목 v3), 대상 사용자 정의 부재다
- 판본 번호 불일치는 대체 관계와 무관하다고 보고 진행했다 — 비교할 소스가 없어
  `status` 판정에는 영향이 없다

## [2026-09-17] ingest | 기능정의서_MVP v3

- 원본: `wiki/raw/기능정의서-v3.md` (2026-06-07자, Notion 캡처 2장 전사, 원본
  페이지는 게스트 권한이라 접근 불가)
- 판본 관계: v3가 v2를 **전면 대체**. v3가 MVP 화면 전체를 다시 정의하고 원본
  스스로 "v2 대비 주요 변경"을 밝혔으므로 `partial`이 아니라 `superseded`로 봤다
  - `src-기능정의서-v3`: `status: current`, `supersedes: [src-기능정의서-v2]`
  - `src-기능정의서-v2`: `status: superseded`, `superseded_by: [src-기능정의서-v3]`
- synthesis 1건 신설: `v2-to-v3-scope-shift`. 판본 비교가 처음 가능해진 시점이라
  [[purpose]] 핵심 질문 4에 답을 시도했다. `index.md`의 `## synthesis`에 등록하고
  `parfait`·`canvas`·`group`·`cutout-placement-pipeline`·`screen-id-scheme`·
  `mvp-scope-limits`에서 링크했다
- 개념 신설 없음. `C-301`(파르페 편집 모드)은 별도 페이지를 만들지 않고
  `cutout-placement-pipeline` 안 서술로 뒀다
- 기존 6개 페이지 전부 갱신 — v3 기준으로 다시 쓰고 v2에서 바뀐 지점만 판본을
  밝혔다
- 미결 2건 해소: `C-002` 미정의(v3가 명시적으로 제거), 판본 번호 불일치(별도 v3
  문서 실재로 v2가 진짜 2차 판본임이 확정). 둘 다 하류 반영 완료
- 미결 1건 신설: v3 **내부 모순** — 페이지 비고는 "그룹장 기능 삭제"인데 `A-005`
  시스템 동작은 "사용자 그룹장 등록"을, `S-103` 분기조건은 "그룹장 + 잔여 멤버"를
  남겼다. 어느 쪽이 옳은지 소스로 판정되지 않아 `group`에 상충 마커를 달고
  `open-questions`에 등록했다
- 8일 사이 화면 구조는 크게 움직였으나 미정 정책은 거의 그대로다. 삭제는 셋 늘고
  근거는 하나도 늘지 않았다
- 검증: `lint.py` 위반 0건
