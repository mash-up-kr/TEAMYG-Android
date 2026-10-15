# Graph Report - wiki  (2026-09-21)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 114 nodes · 225 edges · 22 communities (10 shown, 12 thin omitted)
- Extraction: 97% EXTRACTED · 1% INFERRED · 2% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.75)
- Token cost: 36,147 input · 393 output

## Graph Freshness
- Built from commit: `143cda87`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- 파르페 앱 핵심 개념
- 화면별 정책 정의
- 닉네임 생성·검증 정책
- 로딩·토핑 템플릿 정책
- 무한 파르페 토핑 간격
- 이미지 누끼·토핑 배치 정책
- Nametag 컬러·토스트 규칙
- 스플래시·공통 로딩 정책
- 배치 패턴·기능정의서 이력
- 무한 파르페 정책 v0.2
- 무한 파르페 간격 v0.1
- 무한 파르페 간격 v0.2
- 무한 파르페 간격 v0.3
- 앱 닉네임 생성 정책
- 프로필 닉네임 컬러 v0.1
- 프로필 닉네임 컬러 v0.3
- 그룹 닉네임 생성 정책
- 그룹 닉네임 정책 v0.2
- 기능정의서 v2
- 기능정의서 v3
- 기능정의서 v4
- 그룹 정렬·타임스탬프 컬러

## God Nodes (most connected - your core abstractions)
1. `캔버스 (canvas)` - 18 edges
2. `기능정의서_MVP v7 (2026-09-21, 현행 정본)` - 17 edges
3. `페이지 카탈로그 (index)` - 14 edges
4. `개요 (overview)` - 14 edges
5. `계정 (account)` - 14 edges
6. `그룹 (group)` - 14 edges
7. `무한 파르페 (infinite-parfait)` - 14 edges
8. `토핑 배치 파이프라인 (topping-placement-pipeline)` - 14 edges
9. `푸시 알림 (push-notification)` - 13 edges
10. `화면 ID 체계 (screen-id-scheme)` - 13 edges

## Surprising Connections (you probably didn't know these)
- `목적 (purpose)` --references--> `화면 ID 체계 (screen-id-scheme)`  [EXTRACTED]
  purpose.md → concepts/screen-id-scheme.md
- `토핑 편집자 확인 규칙 v0.1 (C-202)` --references--> `Toast 공통 정책`  [AMBIGUOUS]
  sources/src-C-202-토핑-편집자-확인-규칙-v0.1.md → sources/src-Toast-공통-정책.md
- `그룹 토핑 템플릿 정책 v0.2 (G-001)` --semantically_similar_to--> `G-001 무한파르페 정책설계 v0.3`  [AMBIGUOUS] [semantically similar]
  sources/src-G-001-그룹-토핑-템플릿-정책-v0.2.md → sources/src-G-001-무한파르페-정책설계-v0.3.md
- `앱 닉네임 정책 v0.2 (S-002)` --references--> `형용사 100 × 파르페 재료 30 = 3000 조합`  [AMBIGUOUS]
  sources/src-S-002-앱닉네임-정책-v0.2.md → sources/src-S-002-앱-닉네임-생성-정책-v0.1.md
- `지그재그 배치 (index % 2)` --references--> `기능정의서_MVP v7 (2026-09-21, 현행 정본)`  [EXTRACTED]
  concepts/infinite-parfait.md → sources/src-기능정의서-v7.md

## Hyperedges (group relationships)
- **제품을 지탱하는 세 단위 (계정·그룹·캔버스)** — entities_parfait, concepts_account, concepts_group, concepts_canvas [EXTRACTED 0.95]
- **v7이 정책 설계서 본문을 `기타` 열로 흡수** — sources_src_gineungjeongeuiseo_v7, sources_src_gineungjeongeuiseo_v6, synthesis_spec_version_history, concepts_canvas, concepts_infinite_parfait [EXTRACTED 0.90]
- **초기 닉네임 1회 생성값을 앱·그룹 두 층이 공유** — concepts_account, concepts_account_nickname_two_layers, concepts_group, sources_src_gineungjeongeuiseo_v7 [EXTRACTED 0.85]
- **G-001 간격 정책 판본 사슬 (v0.1 → v0.2 → v0.3)** — sources_src_g_001_무한파르페_간격_정책_v0_1_gap_policy_v01, sources_src_g_001_무한파르페_간격_정책_v0_2_gap_policy_v02, sources_src_g_001_무한파르페_간격_정책_v0_3_gap_policy_v03 [EXTRACTED 0.95]
- **동일 유효성 규칙 열네 줄을 든 문서 여섯** — sources_src_s_002_앱닉네임_정책_v0_1_app_nickname_v01, sources_src_s_102_닉네임_정책_v0_1_s102_nickname_v01, sources_src_s_002_앱_닉네임_생성_정책_v0_1_nickname_gen_policy, sources_src_s_102_그룹_닉네임_생성_정책_v0_1_group_nickname_gen, sources_src_s_002_앱닉네임_정책_v0_2_app_nickname_v02, sources_src_s_102_닉네임_정책_v0_2_s102_nickname_v02 [EXTRACTED 0.90]

## Communities (22 total, 12 thin omitted)

### Community 0 - "파르페 앱 핵심 개념"
Cohesion: 0.36
Nodes (26): 계정 (account), Nametag-Chip 12종 + Default 상태, 닉네임 두 층 (앱 닉네임 / 그룹 내 닉네임), 캔버스 (canvas), Canvas-Menu 2뎁스, 하루 경계 03:00 KST, Spotlight 상태, 그룹 (group) (+18 more)

### Community 1 - "화면별 정책 정의"
Cohesion: 0.21
Nodes (13): 그룹명 정책 v0.1 (A-005), 캔버스 정책 v0.1 (C-001), 캔버스 정책 v0.2 (C-001), 카메라 화면 정책 v0.1 (C-101), 캘린더 컴포넌트 정의 v0.1 (C-201), G-001-Empty 툴팁 노출 조건 정책 v0.1, 그룹 토핑 템플릿 정책 v0.1 (G-001), 그룹 토핑 템플릿 정책 v0.2 (G-001) (+5 more)

### Community 3 - "닉네임 생성·검증 정책"
Cohesion: 0.29
Nodes (11): [K2] 생성 주체 미정 — 서버 생성 권장 (어뷰징·치우침 방지), 앱 닉네임 생성 정책 v0.1 (S-002), 형용사 100 × 파르페 재료 30 = 3000 조합, 앱 닉네임 정책 v0.1 (S-002), 닉네임 입력 유효성 규칙 열네 줄 (1~15자, 공백 규칙), 앱 닉네임 정책 v0.2 (S-002), 중복이 의도 — 두 화면 분기 대비 계열 유지, 그룹 닉네임 생성 정책 v0.1 (S-102) (+3 more)

### Community 4 - "로딩·토핑 템플릿 정책"
Cohesion: 0.22
Nodes (10): 공통 로딩 정책 v0.1, Lottie_Loading_Light/Dark.lottie 44x44 60fps 2초 loop, 좌/우 인셋 4 (전체 템플릿 기준), 무한 파르페 정책 설계 v0.2 (G-001), 좌/우 인셋 28, 변형 시드 = 그룹 ID 고정 (폐기됨), 무한 파르페 정책 설계 v0.3 (G-001), 빈 그룹 토핑 템플릿 6종 (디저트) (+2 more)

### Community 5 - "무한 파르페 토핑 간격"
Cohesion: 0.53
Nodes (6): 무한 파르페 간격 정책 v0.1 (G-001), 타입 체계 Type 1 / 2-Left / 2-Right, 무한 파르페 토핑 배치·간격 정책 v0.2 (G-001), 6타입 체계 Left-1~3 / Right-1~3, 토핑 프레임 180×180 / Img 144×144, 토핑 정책 v0.1 (G-001)

### Community 6 - "이미지 누끼·토핑 배치 정책"
Cohesion: 0.29
Nodes (8): 이미지 렌더링 정책 v0.1 (공용), 누끼 이미지 정책 v0.1 (C-103-Selected), 누끼 편집 정책 v0.1 (C-104), 토핑 배치 정책 v0.1 (C-106), 토핑 편집자 확인 규칙 v0.1 (C-202), G-001 토핑 정책 v0.1, S-101 프로필 닉네임 컬러 규칙 v0.2, 기능정의서 v5 (2026-06-25)

### Community 7 - "Nametag 컬러·토스트 규칙"
Cohesion: 0.25
Nodes (11): 무한 파르페 간격 정책 v0.3 (G-001), 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.1, Nametag-Chip 12종 (6컬러 × 2타입), 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.2, 타입 고정 정책 — 계정 생성 시 1회 배정, 불변, 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.3, 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.4, Nametag-Chip Default 상태 (정상 결정 불가 전부) (+3 more)

### Community 8 - "스플래시·공통 로딩 정책"
Cohesion: 0.50
Nodes (4): 스플래시 정책 v0.1 (A-001), 공통 로딩 정책 v0.1 (common), S-002 앱닉네임 정책 v0.2, S-101 프로필 닉네임 컬러 규칙 v0.4

### Community 9 - "배치 패턴·기능정의서 이력"
Cohesion: 0.33
Nodes (7): 2-1-2-1 스태거 배치 패턴, 지그재그 배치 패턴 (index % 2), 무한 파르페 정책 설계 (G-001, 06-13), 기능정의서_MVP v2 (2026-05-30), 기능정의서_MVP v3 (2026-06-07), 기능정의서_MVP v4 (2026-06-14), 기능정의서_MVP v5 (2026-06-25)

### Community 22 - "그룹 정렬·타임스탬프 컬러"
Cohesion: 0.33
Nodes (6): 정렬 타이브레이크 — 그룹 생성일시 최신순, 정렬 = 활동순 (9인 투표, AOS-A 근거), 토스트 닉네임 컬러 매핑 (Cherry-200/300/400, White, Melon, Pudding), 활동 정의 넷 — 토핑 추가·편집, 배경 추가·변경, Grouptag-Chip Timestamp 별개 매핑 (Cherry-100/200/300), 프로필 이미지 및 그룹칩 내 타임스탬프 컬러 규칙 v0.1

## Ambiguous Edges - Review These
- `토핑 편집자 확인 규칙 v0.1 (C-202)` → `Toast 공통 정책`  [AMBIGUOUS]
  sources/src-C-202-토핑-편집자-확인-규칙-v0.1.md · relation: references
- `그룹 토핑 템플릿 정책 v0.2 (G-001)` → `G-001 무한파르페 정책설계 v0.3`  [AMBIGUOUS]
  sources/src-G-001-그룹-토핑-템플릿-정책-v0.2.md · relation: semantically_similar_to
- `형용사 100 × 파르페 재료 30 = 3000 조합` → `앱 닉네임 정책 v0.2 (S-002)`  [AMBIGUOUS]
  sources/src-S-002-앱닉네임-정책-v0.2.md · relation: references
- `P-01 토핑 등록 알림` → `Toast (toast)`  [AMBIGUOUS]
  concepts/push-notification.md · relation: conceptually_related_to

## Knowledge Gaps
- **27 isolated node(s):** `캘린더 컴포넌트 정의 v0.1 (C-201)`, `G-001 무한파르페 정책설계 v0.2`, `G-001 무한파르페 간격 정책 v0.1`, `G-001 무한파르페 간격 정책 v0.2`, `G-001 무한파르페 간격 정책 v0.3` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `토핑 편집자 확인 규칙 v0.1 (C-202)` and `Toast 공통 정책`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `그룹 토핑 템플릿 정책 v0.2 (G-001)` and `G-001 무한파르페 정책설계 v0.3`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `형용사 100 × 파르페 재료 30 = 3000 조합` and `앱 닉네임 정책 v0.2 (S-002)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `P-01 토핑 등록 알림` and `Toast (toast)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `Toast 공통 정책` connect `Nametag 컬러·토스트 규칙` to `이미지 누끼·토핑 배치 정책`, `그룹 정렬·타임스탬프 컬러`?**
  _High betweenness centrality (0.179) - this node is a cross-community bridge._
- **Why does `무한 파르페 정책 설계 v0.3 (G-001)` connect `로딩·토핑 템플릿 정책` to `그룹 정렬·타임스탬프 컬러`, `Nametag 컬러·토스트 규칙`?**
  _High betweenness centrality (0.136) - this node is a cross-community bridge._
- **Why does `토핑 편집자 확인 규칙 v0.1 (C-202)` connect `이미지 누끼·토핑 배치 정책` to `Nametag 컬러·토스트 규칙`?**
  _High betweenness centrality (0.124) - this node is a cross-community bridge._