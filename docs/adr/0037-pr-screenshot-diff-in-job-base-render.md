---
id: ADR-0037
title: PR 스크린샷 비교는 PR 잡 안에서 base를 함께 렌더한다
status: proposed
date: 2026-11-04
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec: screenshot-comparison-ci
related_architecture:
platforms: android
tags: [adr, parfait, ci, screenshot]
---

# ADR-0037: PR 스크린샷 비교는 PR 잡 안에서 base를 함께 렌더한다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락
UI를 바꾼 PR의 Before / After는 작성자가 직접 찍어 붙여야 해서 잘 지켜지지 않았다. CI가 `@Preview`
렌더를 base와 비교해 PR 코멘트로 달아야 하는데, 비교 기준(base 렌더)을 어디서 얻는지가 정확도와
작성자 부담을 가른다. 설계는 [스펙](../superpowers/specs/2026-11-04-screenshot-comparison-ci-design.md).

## 결정
**한 PR 잡 안에서 PR merge ref 커밋의 첫 부모(base)를 checkout해 Roborazzi로 렌더하고, merge ref를
checkout해 비교한다.**

- base는 이벤트의 `base.sha`가 아니라 merge ref의 첫 부모다. develop이 움직이면 이벤트 값이 merge ref보다
  늦을 수 있는데, base와 head 렌더는 같은 merge에서 나와야 한다.

- 기준과 대상이 같은 잡·같은 머신에서 나와 경합이 없다. stacked PR은 부모 브랜치를 base로 비교한다.
- 코멘트 쓰기 권한과 이미지 브랜치 push는 `workflow_run` 코멘트 워크플로에만 둔다. PR 코드를 빌드하는
  비교 잡은 `contents: read`뿐이고 시크릿이 없다. 쓰기 토큰과 PR 코드가 같은 잡에 놓이지 않게 하려는
  분리다.
- 이미지는 orphan 브랜치 `ui-snapshots/pr-<N>`에 두고 닫힌 지 **30일** 뒤 정리한다. 코멘트의 이미지
  URL이 커밋 SHA를 가리키므로 ref가 사라지면 깨진다. 닫히자마자 지우지 않는 것은 머지 직후 그때의 리뷰를
  다시 볼 기간을 남기기 위해서다.
- 바뀐 화면이 **0건이면 새 코멘트를 달지 않는다**(이미 코멘트가 있는 PR만 갱신). 트리거가 넓어 UI와
  무관한 PR도 대부분 잡이 도는데 그 모두에 봇 코멘트를 붙이지 않기 위해서다. "비교 못 함" 중 merge
  충돌·렌더 실패는 0건이 아니므로 코멘트를 남긴다. base에 구성 없음(`no-base-setup`)은 기존 코멘트만
  갱신한다 — develop에 구성이 없는 동안 모든 PR이 이 상태라 새로 달면 0건 규칙과 같은 소음이 된다.
- 머지는 막지 않는 리포트다. 필수 체크로 묶지 않는다.
- 한 프리뷰 함수에 `@Preview`가 여럿이면 `group = UI_SNAPSHOT_PREVIEW_GROUP`을 단 변형만 찍는다. `@YGPreview`는
  phone 변형에 이 group을 달아 6변형 중 하나만 비교한다 — 전부 찍으면 렌더가 6배가 된다. 이름이나 선언 순서로
  고르지 않는 것은 스캐너가 선언 순서를 넘기지 않기 때문이다(근거는 [스펙](../superpowers/specs/2026-11-04-screenshot-comparison-ci-design.md) 렌더 결정성).

## 대안
- **A. develop 골든 아티팩트** — develop push마다 골든을 렌더해 올리고 PR이 내려받아 비교한다. PR 잡이
  짧다. 그러나 develop이 움직인 직후 골든 기록이 끝나기 전에 push하면 이전 골든과 비교해 오탐이 나고,
  base가 develop이 아닌 stacked PR은 골든이 없어 리포트가 나오지 않는다.
  **→ 기각:** 경합과 stacked PR 공백.
- **C. 골든 이미지를 레포에 커밋** — Roborazzi `verify`. 판정이 결정적이다. 그러나 작성자가 매번 재기록·
  커밋해야 하고, merge와 rebase가 섞인 환경에서 바이너리 충돌이 난다.
  **→ 기각:** 작성자에게 새 할 일을 만들고 충돌이 난다.

## 영향

**긍정**

- 작성자는 아무것도 하지 않아도 바뀐 화면이 코멘트에 보인다. merge/rebase 혼재와 무관하게 PR이 바꾼
  것만 잡힌다.
- 골든 잡·아티팩트 보관이 필요 없다.

**트레이드오프**

- PR 잡이 렌더를 2회 하므로 길어진다(public 레포라 비용은 없다).
- 30일 뒤 머지된 PR의 코멘트 이미지는 깨진다.
- 이 CI를 도입하기 전 커밋이 base인 PR은 base에 스크린샷 구성이 없어 비교하지 못한다(`no-base-setup`).

**위험·방어**

- 같은 머신에서 두 번 렌더하므로 흔들리는 프리뷰는 그대로 오탐이 된다. 프리뷰는 같은 코드를 두 번
  렌더해 픽셀이 같아야 한다. 현재 시각을 읽어 그리는 화면은 `clock`을 시각을 읽는 안쪽 Content
  컴포저블에만 받고, 프리뷰가 거기에 고정 시계를 넘긴다.
- 오래된 실행이 최신 코멘트를 덮는 것은 PR별 concurrency, 코멘트 직전 head SHA stale 검사, 이미지
  커밋 SHA 고정 URL로 막는다.

**개발자 안내: `@UiSnapshotExcluded`**

두 번 렌더했을 때 픽셀이 같지 않을 프리뷰(네트워크·WebView·시간·난수에 의존)에만 붙여 비교에서 뺀다.
그 밖의 프리뷰는 붙이지 않고 결정적으로 렌더되게 고친다. 규칙 본문은 `UiSnapshotExcluded`의 KDoc에
있다.
