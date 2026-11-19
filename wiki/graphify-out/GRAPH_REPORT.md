# Graph Report - wiki  (2026-11-13)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 241 nodes · 439 edges · 19 communities (9 shown, 10 thin omitted)
- Extraction: 98% EXTRACTED · 1% INFERRED · 1% AMBIGUOUS · INFERRED: 6 edges (avg confidence: 0.79)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `92ef9e8f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- 캔버스 (canvas)
- parfait (제품)
- 무한 파르페 토핑 배치·간격 정책 v0.2 (G-001)
- 푸시알림 인앱 알림 제어 정책 v0.3 (S-105)
- 8/27 전체회의 — 2차 UT 설계·인터뷰
- 텍스트 토핑
- 캔버스 정책 v0.1 (C-001)
- 닉네임 입력 유효성 규칙 열네 줄 (1~15자, 공백 규칙)
- 공통 로딩 정책 v0.1
- G-001 무한파르페 정책설계 v0.2
- G-001 무한파르페 간격 정책 v0.1
- G-001 무한파르페 간격 정책 v0.2
- G-001 무한파르페 간격 정책 v0.3
- S-101 프로필 닉네임 컬러 규칙 v0.1
- S-101 프로필 닉네임 컬러 규칙 v0.3
- 기능정의서 v2 (2026-05-30)
- 기능정의서 v3 (2026-06-07)
- 기능정의서 v4 (2026-06-14)
- 푸시 알림

## God Nodes (most connected - your core abstractions)
1. `parfait (제품)` - 20 edges
2. `텍스트 토핑` - 19 edges
3. `토핑 배치 파이프라인` - 19 edges
4. `Toast` - 15 edges
5. `무한 파르페 (G-001)` - 15 edges
6. `푸시알림 인앱 알림 제어 정책 v0.3 (S-105)` - 14 edges
7. `푸시알림 정책 v0.4` - 12 edges
8. `화면 ID 체계` - 12 edges
9. `기능정의서_MVP v7 (2026-09-21, 현행 정본)` - 11 edges
10. `캔버스 (canvas)` - 11 edges

## Surprising Connections (you probably didn't know these)
- `초기 닉네임 자동 생성 (100×30=3000)` --conceptually_related_to--> `parfait (제품)`  [INFERRED]
  concepts/account.md → entities/parfait.md
- `공지사항` --references--> `S-106 공지사항 운영 정책 v0.3`  [INFERRED]
  concepts/notice-board.md → sources/src-S-106-공지사항-운영-정책-v0.3.md
- `초대 코드 → 딥링크 → 시스템 공유` --cites--> `9/21 우선순위 정의 회의`  [EXTRACTED]
  concepts/group.md → sources/src-회의록-2026-09-21-우선-순위-정의-회의.md
- `한 줄 정의와 Is / Is Not 표` --cites--> `5/24 플랫폼장회의 — 인터뷰 인사이트·Is/Is Not`  [EXTRACTED]
  concepts/product-origin.md → sources/src-회의록-2026-05-24-회의.md
- `Pre-Kickoff 회의록 (2026-04-01)` --references--> `parfait (제품)`  [EXTRACTED]
  sources/src-회의록-2026-04-01-Pre-Kickoff.md → entities/parfait.md

## Hyperedges (group relationships)
- **런칭 뒤 범위 확장 (밀었다가 되돌린 것)** — concepts_mvp_scope_limits_reverted_items, concepts_push_notification_s105_settings, concepts_canvas_gallery_window_release, concepts_canvas_external_share, concepts_group_invite_flow [EXTRACTED 0.90]
- **첫 업로드 부재 대책과 판정 축** — concepts_first_upload_problem_definition, concepts_first_upload_problem_empty_dummy_bg, concepts_first_upload_problem_ios_widget, concepts_first_upload_problem_no_push_policy, concepts_canvas_gallery_window_release [EXTRACTED 0.90]
- **탈퇴·나가기 시 데이터 처리 (익명화·Default 칩·토큰 삭제)** — concepts_account_soft_delete_withdrawal, concepts_group_anonymize_on_leave, concepts_account_nametag_chip, sources_src_common_service_rules [INFERRED 0.80]
- **닉네임 정책 계열 (S-002/S-102 판본과 공통 규칙)** — sources_src_s_002_app_nickname_generation_policy_v0_1, sources_src_s_002_app_nickname_policy_2026_09_20, sources_src_s_002_app_nickname_policy_v0_2, sources_src_s_102_group_nickname_generation_policy_v0_1, sources_src_s_102_group_nickname_policy_2026_09_20, sources_src_s_102_nickname_policy_v0_2, sources_src_common_service_rules_2026_09_20 [EXTRACTED 0.95]
- **UX_1 캔버스 편집 흐름 (배치·편집·삭제·텍스트)** — concept_ux_1_flow_redesign, sources_src_photo_edit_flow_v0_1_doc, sources_src_photo_delete_flow_v0_1, sources_src_bl_1_text_upload_policy_v0_1 [EXTRACTED 0.90]
- **브랜드 동사 규칙과 어긋나는 카피들** — concept_brand_verb_rule, sources_src_common_service_rules_2026_09_20, sources_src_c_001_canvas_policy_v0_3, sources_src_figma_annotation_group_side_menu_s_10n [EXTRACTED 0.85]
- **푸시 알림 정책 계열 (v0.1 → v0.4 + S-105 인앱 제어)** — sources_src_push_policy_v0_1_doc, sources_src_push_inapp_control_policy_v0_3_s_105, sources_src_push_policy_v0_4_p_06_bundle [EXTRACTED 0.95]
- **UX_1 토핑 추가·편집 플로우 개편 (배치+테두리 통합)** — sources_src_topping_stack_flow_improvement_2_2026_09_22_doc, sources_src_photo_edit_flow_v0_1_doc, sources_src_topping_center_align_angle_guide_snap_v0_1_doc, sources_src_photo_edit_flow_v0_1_c_105_arrange, sources_src_meeting_2026_09_29_doc [INFERRED 0.85]
- **제품 출발점 회의 (아이디어 → 아파트 → 누끼 캔버스 → 공유 0순위)** — sources_src_meeting_2026_04_01_doc, sources_src_meeting_2026_04_16_doc, sources_src_meeting_2026_04_23_doc, sources_src_meeting_2026_04_30_doc, sources_src_meeting_2026_05_09_doc, sources_src_meeting_2026_05_24_doc [INFERRED 0.80]
- **G-001 간격 정책 판본 사슬 (v0.1 → v0.2 → v0.3)** — sources_src_g_001_무한파르페_간격_정책_v0_1_gap_policy_v01, sources_src_g_001_무한파르페_간격_정책_v0_2_gap_policy_v02, sources_src_g_001_무한파르페_간격_정책_v0_3_gap_policy_v03 [EXTRACTED 0.95]

## Communities (19 total, 10 thin omitted)

### Community 0 - "캔버스 (canvas)"
Cohesion: 0.09
Nodes (28): 계정 (account), Nametag-Chip 12종 + Default, 초기 닉네임 자동 생성 (100×30=3000), 탈퇴 soft delete / 푸시 토큰 하드 삭제, 닉네임 두 층 (앱 / 그룹 내), 브랜드 동사 '쌓기' (올리다 금지), 캔버스 (canvas), 본인 토핑만 편집 (락 대신) (+20 more)

### Community 1 - "parfait (제품)"
Cohesion: 0.09
Nodes (41): 브랜드 동사 규칙 (쌓다 확정, 올리다 금지), 디저트 은유 어휘, Dot Grid 배경 (20px vs 22px), 무한 파르페 폐기 검토와 유지 결정, 초기 닉네임 자동 생성 (형용사 100 × 재료 30), 그룹 내 닉네임 초기값 공유 여부 상충, 탈퇴·나가기 시 토핑 익명화 보존, 계정 (+33 more)

### Community 2 - "무한 파르페 토핑 배치·간격 정책 v0.2 (G-001)"
Cohesion: 0.09
Nodes (35): 무한 파르페 간격 정책 v0.1 (G-001), 2-1-2-1 스태거 배치 패턴, 타입 체계 Type 1 / 2-Left / 2-Right, 무한 파르페 토핑 배치·간격 정책 v0.2 (G-001), 6타입 체계 Left-1~3 / Right-1~3, 지그재그 배치 패턴 (index % 2), 무한 파르페 간격 정책 v0.3 (G-001), 좌/우 인셋 4 (전체 템플릿 기준) (+27 more)

### Community 3 - "푸시알림 인앱 알림 제어 정책 v0.3 (S-105)"
Cohesion: 0.11
Nodes (31): 공지사항 NEW 칩 (14일 소멸, 기기 저장), 텍스트 푸시 P-04, account (계정), mvp-scope-limits (MVP 범위 제한), 공지사항, push-notification (푸시 알림), screen-id-scheme (화면 ID 체계), topping-placement-pipeline (토핑 배치 파이프라인) (+23 more)

### Community 4 - "8/27 전체회의 — 2차 UT 설계·인터뷰"
Cohesion: 0.13
Nodes (19): canvas (캔버스), 활동순 정렬 + 생성일시 타이브레이크, infinite-parfait (무한 파르페), 은유 미전달에도 정체성으로 유지, Pre-Kickoff 회의록 (2026-04-01), 팀장 회의록 (2026-04-02) 로드맵, 4/9 회의록, 4/16 회의록 — 친구 기반 일상 기록 앱 (+11 more)

### Community 5 - "텍스트 토핑"
Cohesion: 0.14
Nodes (29): 가이드와 자석 스냅 (6pt / ±3°), [캔버스에 쌓기] 전까지 로컬 유지 규칙, UX_1의 C-104~C-106·C-301 ID 재사용, 텍스트 업로드 후 수정 가능 여부 상충, UX_1 토핑 추가·편집 흐름 개편 (10/15 배포), UX_1 토핑 추가 플로우 개선 (10/15 배포), group (그룹), 화면 ID 체계 (+21 more)

### Community 6 - "캔버스 정책 v0.1 (C-001)"
Cohesion: 0.11
Nodes (19): 그룹명 정책 v0.1 (A-005), 캔버스 정책 v0.1 (C-001), 카메라 화면 정책 v0.1 (C-101), 이미지 렌더링 정책 v0.1 (공용), 누끼 이미지 정책 v0.1 (C-103-Selected), 누끼 편집 정책 v0.1 (C-104), 토핑 배치 정책 v0.1 (C-106), 캘린더 컴포넌트 정의 v0.1 (C-201) (+11 more)

### Community 7 - "닉네임 입력 유효성 규칙 열네 줄 (1~15자, 공백 규칙)"
Cohesion: 0.67
Nodes (3): 앱 닉네임 정책 v0.1 (S-002), 닉네임 입력 유효성 규칙 열네 줄 (1~15자, 공백 규칙), 닉네임 정책 v0.1 (S-102)

### Community 18 - "푸시 알림"
Cohesion: 0.11
Nodes (25): 하루 경계 03:00 KST (마감·생성), 외부 공유 (영상 저장·인스타 스토리), 갤러리 03시 창 해제 실험, 매일 빈 캔버스 → 첫 타자 부담 → 아무도 안 올림, C-001 Empty 더미 배경 (B안), 첫 업로드 부재, 홈 화면 위젯 (iOS만, C안), 푸시 지양 방침 (+17 more)

## Ambiguous Edges - Review These
- `토핑 편집자 확인 규칙 v0.1 (C-202)` → `Toast 공통 정책`  [AMBIGUOUS]
  sources/src-C-202-토핑-편집자-확인-규칙-v0.1.md · relation: references
- `S-105 알림 설정 (리마인드 끄기·모아 받기)` → `푸시 지양 방침`  [AMBIGUOUS]
  concepts/first-upload-problem.md · relation: conceptually_related_to
- `앱 닉네임 생성 정책 v0.1 (S-002)` → `앱 닉네임 정책 v0.2 (S-002)`  [AMBIGUOUS]
  sources/src-S-002-앱닉네임-정책-v0.2.md · relation: references
- `8/27 전체회의 — 2차 UT 설계·인터뷰` → `canvas (캔버스)`  [AMBIGUOUS]
  sources/src-회의록-2026-08-27-회의.md · relation: conceptually_related_to

## Knowledge Gaps
- **41 isolated node(s):** `캘린더 컴포넌트 정의 v0.1 (C-201)`, `G-001 무한파르페 정책설계 v0.2`, `G-001 무한파르페 간격 정책 v0.1`, `G-001 무한파르페 간격 정책 v0.2`, `G-001 무한파르페 간격 정책 v0.3` (+36 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **10 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `토핑 편집자 확인 규칙 v0.1 (C-202)` and `Toast 공통 정책`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `S-105 알림 설정 (리마인드 끄기·모아 받기)` and `푸시 지양 방침`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `앱 닉네임 생성 정책 v0.1 (S-002)` and `앱 닉네임 정책 v0.2 (S-002)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `8/27 전체회의 — 2차 UT 설계·인터뷰` and `canvas (캔버스)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **Why does `캔버스 정책 v0.2 (C-001)` connect `parfait (제품)` to `캔버스 정책 v0.1 (C-001)`?**
  _High betweenness centrality (0.330) - this node is a cross-community bridge._
- **Why does `캔버스` connect `parfait (제품)` to `텍스트 토핑`?**
  _High betweenness centrality (0.304) - this node is a cross-community bridge._
- **What connects `캘린더 컴포넌트 정의 v0.1 (C-201)`, `G-001 무한파르페 정책설계 v0.2`, `G-001 무한파르페 간격 정책 v0.1` to the rest of the system?**
  _41 weakly-connected nodes found - possible documentation gaps or missing edges._