---
tags: [lint, maintenance, parfait]
updated: 2026-07-22
---

# Lint 보고서 2026-07-22 — parfait 문서 정합성

기준선 동기화(`526f4c9`→`23ef432`, delta #153·#154·#161·#162) 커밋 직후 parfait 서브트리 **문서 내부 정합** 점검.
코드 대조는 방금 [doc-baseline](../doc-baseline.md) 점검이 delta를 커버 → 이번 lint는 링크·상태표·규율·민감데이터 중심.

- 대상: `parfait/**/*.md` (ADR 15 + architecture 5 + specs/plans + synthesis + index/doc-baseline)
- 방법: 상대 md-link resolve, wikilink resolve(wiki+parfait), 상태표↔frontmatter, 규율(hex·라인번호), 민감데이터, 고아.

## 링크 무결성
- **상대 md-link**: 깨진 링크 **0건**. (아카이브 이동분 `../`→`../../` 보정 검증 포함.)
- **wikilink**: 미해결 3건 발견 → **전부 수정 완료**(아래 조치).

## 상태표 · 개수 정합 ✅
- **specs/plans**: active 디렉토리에 README/template만 잔존(전 스펙·플랜 archive). archive frontmatter `status` 전건 `implemented`/`superseded`/`done` — README 표와 일치.
- **ADR**: 파일 15 = `adr/README.md` 행 15. 정합.
- **규율(라인번호·변동수치)**: `related_code` 심볼명 기반, 위반 0건.

## 민감 데이터 ⚠️
- **(없음)**. 이메일·실명·절대경로 노출 0. (grep 오탐: Kotlin 한정 `this@ClickableYGNode`·모듈경로 `feature/login/impl` — PII 아님.)

## 발견 🟡 (낮음 — 조치)

### 1. wikilink 미해결 3건 (컨벤션 이탈) — ✅ 수정 완료
parfait 내부 참조는 상대 md-link, 개념 참조만 `[[…]]`(open-questions 링크 규약). 아래는 broken wikilink였음:
- `adr/0015` `[[0002]]` → `[[0002-feature-api-impl-split]]`(전체 slug).
- `plans/archive/2026-07-21-feature-common-terms-module` `[[ADR-0002]]`·`[[ADR-0006]]` → `[[0002-feature-api-impl-split]]`·`[[0006-navigation3-custom-navigator]]`.
- `specs/archive/2026-07-21-feature-common-terms-module` `[[s004-terms-privacy-webview]]`(날짜 접두사 파일이라 미해결) → 상대 md-link `(2026-07-20-s004-terms-privacy-webview.md)`.

## 발견 🟡 (낮음 — 미조치, 정보성)

### 2. archive 문서 hex 색값 3건 (규율 "hex 금지" 이전 작성)
- `specs/archive/2026-07-15-ygmodalpopup.md`(Figma `#333333` 매칭 논의), `specs/archive/2026-07-19-ygdangerzone-dashed.md`(`#ECECEE`), `plans/archive/2026-07-15-ygmodalpopup.md`(`#FAFAFA`).
- 전부 **Figma 원본 색 서술**(디자인 소스 기록 목적) + archive. 신규 위반 아님 → 역사 스냅샷으로 보존. 규율 강제 정리 시 함께 제거 후보.

### 3. (wiki-side) `wiki/index.md` parfait 라인 ADR 개수 stale
- `wiki/index.md`가 parfait를 "ADR 14건"으로 기술 → 현재 **15건**(#161로 ADR-0015 feature/common 추가). architecture도 "5건" 유지 확인.
- **범위 외**(wiki 스키마 소관) → 이번 parfait lint에선 미수정. 위키 갱신 시 정정 권고.

## 조치 요약
| 발견 | 심각도 | 조치 |
|---|---|---|
| wikilink 미해결 3건 | 🟡 | ✅ 수정 완료(정확 slug / 상대 md-link) |
| archive hex 3건 | 🟡 | 미조치(Figma 원본·archive 보존) |
| wiki/index ADR 개수 stale | 🟡 | 미조치(wiki 소관, 갱신 시 정정) |
| 상대링크·상태표·개수·규율·민감데이터·고아 | ✅ | 문제 없음 |
