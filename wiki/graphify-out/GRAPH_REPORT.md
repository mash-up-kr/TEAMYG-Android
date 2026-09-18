# Graph Report - wiki  (2026-09-18)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 108 nodes · 248 edges · 9 communities
- Extraction: 98% EXTRACTED · 1% INFERRED · 2% AMBIGUOUS · INFERRED: 2 edges (avg confidence: 0.78)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `ac0018ca`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- 개요 (overview)
- 계정 (account)
- Toast 공통 정책
- 닉네임 생성 및 검증 정책
- 무한 파르페 (infinite-parfait, G-001)
- 기능정의서 판본 이력 (합성)
- 토핑 배치 간격 정책
- 로딩 및 토핑 변형 정책
- 그룹 정렬 및 인셋 규칙

## God Nodes (most connected - your core abstractions)
1. `기능정의서 판본 이력 (합성)` - 16 edges
2. `계정 (account)` - 15 edges
3. `개요 (overview)` - 14 edges
4. `캔버스 (canvas)` - 13 edges
5. `그룹 (group)` - 13 edges
6. `페이지 카탈로그 (index)` - 12 edges
7. `무한 파르페 (infinite-parfait, G-001)` - 11 edges
8. `기능정의서 v6 (2026-07-23)` - 11 edges
9. `parfait (제품)` - 10 edges
10. `Toast 공통 정책` - 10 edges

## Surprising Connections (you probably didn't know these)
- `무한 파르페 (infinite-parfait, G-001)` --cites--> `G-001 토핑 정책 v0.1`  [EXTRACTED]
  concepts/infinite-parfait.md → sources/src-G-001-토핑-정책-v0.1.md
- `기능정의서 판본 이력 (합성)` --cites--> `[K2] 생성 주체 미정 — 서버 생성 권장 (어뷰징·치우침 방지)`  [EXTRACTED]
  synthesis/spec-version-history.md → sources/src-S-002-앱-닉네임-생성-정책-v0.1.md
- `기능정의서 판본 이력 (합성)` --cites--> `중복이 의도 — 두 화면 분기 대비 계열 유지`  [EXTRACTED]
  synthesis/spec-version-history.md → sources/src-S-002-앱닉네임-정책-v0.2.md
- `기능정의서 판본 이력 (합성)` --cites--> `Nametag-Chip Default 상태 (정상 결정 불가 전부)`  [EXTRACTED]
  synthesis/spec-version-history.md → sources/src-S-101-프로필-닉네임-컬러-규칙-v0.4.md
- `기능정의서 판본 이력 (합성)` --cites--> `Toast 스택 — 나중 것이 위 (인스타 DM 레퍼런스)`  [EXTRACTED]
  synthesis/spec-version-history.md → sources/src-Toast-공통-정책.md

## Hyperedges (group relationships)
- **동일 입력 유효성 규칙을 공유하는 닉네임·그룹명 정책 계열** — sources_src_s_002_app_nickname_policy_v0_1, sources_src_s_002_app_nickname_policy_v0_2, sources_src_s_102_nickname_policy_v0_1, sources_src_s_102_nickname_policy_v0_2, sources_src_s_002_app_nickname_generation_policy_v0_1, sources_src_a_005_group_name_policy_v0_1, concepts_account [EXTRACTED 0.95]
- **G-001 무한파르페 정책 계보(정책설계·간격·토핑·툴팁·템플릿)** — sources_src_g_001_infinite_parfait_design, sources_src_g_001_infinite_parfait_design_v0_2, sources_src_g_001_infinite_parfait_design_v0_3, sources_src_g_001_spacing_policy_v0_3, sources_src_g_001_topping_policy_v0_1, sources_src_g_001_empty_tooltip_policy_v0_1, sources_src_g_001_group_topping_template_policy_v0_2, concepts_infinite_parfait [EXTRACTED 0.90]
- **사진→토핑 파이프라인 단계별 정책(C-101~C-106)** — sources_src_c_101_camera_policy_v0_1, sources_src_c_103_image_rendering_policy_v0_1, sources_src_c_103_selected_nukki_policy_v0_1, sources_src_c_104_nukki_edit_policy_v0_1, sources_src_c_106_topping_placement_policy_v0_1, concepts_topping_placement_pipeline [EXTRACTED 0.90]
- **G-001 간격 정책 판본 사슬 (v0.1 → v0.2 → v0.3)** — sources_src_g_001_무한파르페_간격_정책_v0_1_gap_policy_v01, sources_src_g_001_무한파르페_간격_정책_v0_2_gap_policy_v02, sources_src_g_001_무한파르페_간격_정책_v0_3_gap_policy_v03 [EXTRACTED 0.95]
- **동일 유효성 규칙 열네 줄을 든 문서 여섯** — sources_src_s_002_앱닉네임_정책_v0_1_app_nickname_v01, sources_src_s_102_닉네임_정책_v0_1_s102_nickname_v01, sources_src_s_002_앱_닉네임_생성_정책_v0_1_nickname_gen_policy, sources_src_s_102_그룹_닉네임_생성_정책_v0_1_group_nickname_gen, sources_src_s_002_앱닉네임_정책_v0_2_app_nickname_v02, sources_src_s_102_닉네임_정책_v0_2_s102_nickname_v02 [EXTRACTED 0.90]
- **Grouptag-Chip Timestamp 컬러 참조가 다섯 번 빗나간 사슬** — sources_src_g_001_토핑_정책_v0_1_topping_policy_v01, sources_src_g_001_무한파르페_간격_정책_v0_2_gap_policy_v02, sources_src_g_001_무한파르페_간격_정책_v0_3_gap_policy_v03, sources_src_g_001_무한파르페_정책설계_v0_2_design_policy_v02, sources_src_g_001_무한파르페_정책설계_v0_3_design_policy_v03, sources_src_s_101_프로필_이미지_및_그룹칩_내_타임스탬프_컬러_규칙_v0_1_timestamp_color_rule [EXTRACTED 0.90]

## Communities (9 total, 0 thin omitted)

### Community 0 - "개요 (overview)"
Cohesion: 0.26
Nodes (22): 캔버스 (canvas), 그룹 (group), MVP 범위 제약 (mvp-scope-limits), 화면 ID 체계 (screen-id-scheme), 토핑 배치 파이프라인, parfait (제품), 페이지 카탈로그 (index), 개요 (overview) (+14 more)

### Community 1 - "계정 (account)"
Cohesion: 0.17
Nodes (18): 계정 (account), Toast 공통 컴포넌트, 그룹명 정책 v0.1 (A-005), 토핑 편집자 확인 규칙 v0.1 (C-202), S-002 앱 닉네임 생성 정책 v0.1, S-002 앱닉네임 정책 v0.1, S-002 앱닉네임 정책 v0.2, S-101 프로필 닉네임 컬러 규칙 v0.1 (+10 more)

### Community 2 - "Toast 공통 정책"
Cohesion: 0.20
Nodes (14): 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.1, Nametag-Chip 12종 (6컬러 × 2타입), 토스트 닉네임 컬러 매핑 (Cherry-200/300/400, White, Melon, Pudding), 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.2, 타입 고정 정책 — 계정 생성 시 1회 배정, 불변, 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.3, 프로필 이미지 및 토스트 닉네임 텍스트 컬러 규칙 v0.4, Nametag-Chip Default 상태 (정상 결정 불가 전부) (+6 more)

### Community 3 - "닉네임 생성 및 검증 정책"
Cohesion: 0.29
Nodes (11): [K2] 생성 주체 미정 — 서버 생성 권장 (어뷰징·치우침 방지), 앱 닉네임 생성 정책 v0.1 (S-002), 형용사 100 × 파르페 재료 30 = 3000 조합, 앱 닉네임 정책 v0.1 (S-002), 닉네임 입력 유효성 규칙 열네 줄 (1~15자, 공백 규칙), 앱 닉네임 정책 v0.2 (S-002), 중복이 의도 — 두 화면 분기 대비 계열 유지, 그룹 닉네임 생성 정책 v0.1 (S-102) (+3 more)

### Community 4 - "무한 파르페 (infinite-parfait, G-001)"
Cohesion: 0.33
Nodes (11): 무한 파르페 (infinite-parfait, G-001), G-001-Empty 툴팁 노출 조건 정책 v0.1, 그룹 토핑 템플릿 정책 v0.1 (G-001), 그룹 토핑 템플릿 정책 v0.2 (G-001), G-001 무한파르페 정책설계 (06-13), G-001 무한파르페 정책설계 v0.2, G-001 무한파르페 정책설계 v0.3, G-001 무한파르페 간격 정책 v0.1 (+3 more)

### Community 5 - "기능정의서 판본 이력 (합성)"
Cohesion: 0.39
Nodes (9): 기능정의서_MVP v2 (2026-05-30), 기능정의서_MVP v3 (2026-06-07), 기능정의서_MVP v4 (2026-06-14), 기능정의서_MVP v5 (2026-06-25), 기능정의서_MVP v6 (2026-07-23), "어제의 파르페" UX 3안 + UT 판단 기준, 서술이 답 없이 사라진다, 기능정의서 판본 이력 (합성) (+1 more)

### Community 6 - "토핑 배치 간격 정책"
Cohesion: 0.36
Nodes (9): 무한 파르페 간격 정책 v0.1 (G-001), 2-1-2-1 스태거 배치 패턴, 타입 체계 Type 1 / 2-Left / 2-Right, 무한 파르페 토핑 배치·간격 정책 v0.2 (G-001), 6타입 체계 Left-1~3 / Right-1~3, 지그재그 배치 패턴 (index % 2), 무한 파르페 정책 설계 (G-001, 06-13), 토핑 프레임 180×180 / Img 144×144 (+1 more)

### Community 7 - "로딩 및 토핑 변형 정책"
Cohesion: 0.29
Nodes (8): 공통 로딩 정책 v0.1, Lottie_Loading_Light/Dark.lottie 44x44 60fps 2초 loop, 무한 파르페 정책 설계 v0.2 (G-001), 변형 시드 = 그룹 ID 고정 (폐기됨), 무한 파르페 정책 설계 v0.3 (G-001), 빈 그룹 토핑 템플릿 6종 (디저트), 변형 타입 랜덤 재부여 (조회 응답 1회, 회전각 튐 방지), 오버플로우 Clip Contents OFF — 삐짐 노출 (폐기됨)

### Community 8 - "그룹 정렬 및 인셋 규칙"
Cohesion: 0.33
Nodes (6): 무한 파르페 간격 정책 v0.3 (G-001), 좌/우 인셋 4 (전체 템플릿 기준), 정렬 타이브레이크 — 그룹 생성일시 최신순, 정렬 = 활동순 (9인 투표, AOS-A 근거), 좌/우 인셋 28, 활동 정의 넷 — 토핑 추가·편집, 배경 추가·변경

## Ambiguous Edges - Review These
- `토핑 배치 정책 v0.1 (C-106)` → `기능정의서 v6 (2026-07-23)`  [AMBIGUOUS]
  sources/src-C-106-토핑-배치-정책-v0.1.md · relation: semantically_similar_to
- `토핑 편집자 확인 규칙 v0.1 (C-202)` → `Toast 공통 정책`  [AMBIGUOUS]
  sources/src-C-202-토핑-편집자-확인-규칙-v0.1.md · relation: references
- `그룹 토핑 템플릿 정책 v0.2 (G-001)` → `G-001 무한파르페 정책설계 v0.3`  [AMBIGUOUS]
  sources/src-G-001-그룹-토핑-템플릿-정책-v0.2.md · relation: semantically_similar_to
- `형용사 100 × 파르페 재료 30 = 3000 조합` → `앱 닉네임 정책 v0.2 (S-002)`  [AMBIGUOUS]
  sources/src-S-002-앱닉네임-정책-v0.2.md · relation: references

## Knowledge Gaps
- **8 isolated node(s):** `G-001 무한파르페 간격 정책 v0.1`, `S-101 프로필 닉네임 컬러 규칙 v0.1`, `기능정의서 v2 (2026-05-30)`, `빈 그룹 토핑 템플릿 6종 (디저트)`, `Nametag-Chip 12종 (6컬러 × 2타입)` (+3 more)
  These have ≤1 connection - possible missing edges or undocumented components.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `토핑 배치 정책 v0.1 (C-106)` and `기능정의서 v6 (2026-07-23)`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `토핑 편집자 확인 규칙 v0.1 (C-202)` and `Toast 공통 정책`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `그룹 토핑 템플릿 정책 v0.2 (G-001)` and `G-001 무한파르페 정책설계 v0.3`?**
  _Edge tagged AMBIGUOUS (relation: semantically_similar_to) - confidence is low._
- **What is the exact relationship between `형용사 100 × 파르페 재료 30 = 3000 조합` and `앱 닉네임 정책 v0.2 (S-002)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **Why does `Toast 공통 정책` connect `Toast 공통 정책` to `개요 (overview)`, `계정 (account)`?**
  _High betweenness centrality (0.514) - this node is a cross-community bridge._
- **Why does `기능정의서 판본 이력 (합성)` connect `기능정의서 판본 이력 (합성)` to `그룹 정렬 및 인셋 규칙`, `Toast 공통 정책`, `닉네임 생성 및 검증 정책`, `로딩 및 토핑 변형 정책`?**
  _High betweenness centrality (0.273) - this node is a cross-community bridge._
- **Why does `그룹 (group)` connect `개요 (overview)` to `계정 (account)`, `Toast 공통 정책`, `무한 파르페 (infinite-parfait, G-001)`?**
  _High betweenness centrality (0.212) - this node is a cross-community bridge._