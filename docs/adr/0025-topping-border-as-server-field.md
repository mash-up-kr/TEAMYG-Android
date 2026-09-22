---
id: ADR-0025
title: 토핑 테두리를 픽셀에 굽지 않고 서버 필드로 보낸다
status: accepted
date: 2026-08-20
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0005, ADR-0006, ADR-0030
related_spec: c106-topping-place-api, c103-segmentation-topping-edit, c106-topping-place
related_architecture: data-layer, design-system
platforms: android
tags: [adr, parfait, topping, border, server-contract]
---

# ADR-0025: 토핑 테두리를 픽셀에 굽지 않고 서버 필드로 보낸다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

토핑 테두리를 표현하는 길이 지금 앱 안에 **둘** 있고 서로를 모른다.

쓰기 쪽은 편집 화면이 테두리를 **이미지 픽셀에 구워** 최종 PNG를 만든다
(`ToppingEditViewModel#completeEdit`). 읽기 쪽은 캔버스가 서버의 `borderType`·`borderColor`·
`borderWidth`를 받아 같은 그림을 테두리 색으로 물들여 **여덟 방향으로 밀어 찍는다**
(`CanvasToppingLayer`).

C-106 결선에서 구운 PNG를 그대로 올리고 `borderType=NONE`을 보내면 화면은 맞게 보인다 —
테두리가 픽셀에 있기 때문이다. 그러나 그렇게 하면 서버의 테두리 필드 셋과
`updateToppingBorder` 엔드포인트, 그리고 이미 구현된 읽기 쪽 렌더러가 **전부 죽은 표면**이 된다.
C-301 테두리 재편집도 필드 수정으로는 성립하지 않고 이미지를 다시 구워 재업로드해야 한다.

전환을 막는 것으로 보였던 사유 하나는 사실이 아니었다. 편집 화면의 `borderLayers`는
`ToppingEditViewModel`이 *"겹칠 수 없으니 마지막에 고른 하나뿐"*으로 정의해 **항상 0개 아니면
1개**다. 서버의 단일 `SOLID(color, width)`와 정확히 1:1이라 표현력 손실이 없다.

## 결정

**테두리는 이미지에 굽지 않는다. 알맹이만 업로드하고 색·굵기는 서버 필드로 보낸다.**

- 업로드 대상은 **테두리 없는 알맹이의 트리밍본**이다. 현재 `cutoutImagePath`는 재편집 좌표계를
  지키려고 여백을 걷지 않으므로, `ToppingEditMask` 의 트리밍(현행 `measureSubject` + `trimTo`, 옛 `trimTransparentBounds`)을 태운 판을 따로 만든다.
  여백이 붙은 채 올리면 초기 배치(긴 변 40%·짧은 변 48dp 하한) 계산과 좌표가 어긋난다.
  **편집이 캐시에 남기는 파일은 여전히 둘이다** — 굽기 전에도 `cutout`과 트리밍본을 저장했고,
  바뀌는 것은 두 번째 파일의 내용뿐이다(테두리를 구운 판 → 테두리 없는 알맹이).
- 테두리 색·굵기는 토핑 초안([ADR-0026](0026-topping-draft-datastore-ssot.md))에 값으로 싣고
  배치 확정 때 `ToppingBorder.Solid`로 보낸다.
- **테두리를 그리는 화면 셋이 같은 렌더러를 쓴다.** 알맹이 위에 8방향 스탬프를 얹어 그리고,
  `CanvasToppingLayer`가 쥐고 있던 스탬프를 **`:core:designsystem`의 `YGToppingCutoutImage`로 올려**
  공유한다. feature 모듈의 `component/`가 아닌 이유는 나눠 쓰는 화면이 모듈 둘에 걸치기 때문이다
  (`:feature:segmentation:impl`·`:feature:groups:canvas:impl`) —
  [module-structure](../architecture/module-structure.md)의 컴포저블 소유 규칙을 따른다.
  대상은 배치 화면과 캔버스만이 아니라
  그 사이의 **누끼 확인 화면**도 포함한다 — 그 화면을 빼면 사용자가 한 흐름 안에서 같은 테두리를
  편집·확인(원본 픽셀에 구운 굵기)과 배치·캔버스(화면 dp 고정)의 **두 가지 굵기로** 보게 된다. 캔버스 토핑의 긴 변이 Canvas-Area 너비의 일부에 불과해 차이가 작지 않다.

## 대안

- **구운 PNG + `borderType=NONE`** — 이번 라운드가 가장 작아지고 화면도 맞게 보인다.
  그러나 서버 필드 셋·`updateToppingBorder`·읽기 렌더러가 전부 쓰이지 않는 채 남고, C-301
  재편집이 "필드 수정"이 아니라 "이미지 재생성"이 된다.
  **→ 기각:** 그 사이 올라간 토핑이 전부 `NONE`으로 쌓여, 나중에 전환하면 과거 데이터를 손봐야 한다.
  전환 비용이 시간에 비례해 커지는 종류의 미룸이다.
- **구운 PNG + 필드도 채우기** — 서버가 값을 알게 되고 이번 라운드도 작다.
  그러나 읽기 쪽이 그 필드로 한 겹을 더 그려 **테두리가 이중으로 보인다.**
  **→ 기각:** 읽기 렌더러를 동시에 고치지 않는 한 화면이 틀린다.

## 영향

**긍정**

- 서버 계약과 읽기 렌더러가 처음으로 실제 쓰인다. `updateToppingBorder`는 **쓰일 수 있게 된다** —
  실제 소비는 C-301 라운드다.
- C-301 테두리 재편집이 필드 수정 한 번으로 끝난다(이미지 재업로드 불필요). 다만 **서버 쪽 이야기이고
  그 화면이 값을 그리는 것은 별개**다 → [OQ-P-254](../synthesis/open-questions.md).
  > ✅ **그 화면도 그리기 시작했다**(2026-08-27 develop 머지, PR #388) — 배경 편집이 맨 `Image`에서
  > `YGToppingCutoutImage`로 갈아타 **테두리를 그리는 화면이 넷**이 됐다. 계기는 이 ADR이 아니라
  > [토핑 알파 판정](../superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md)이다 — 터치 판정을 보이는
  > 실루엣에 맞추려면 두 캔버스 화면이 같은 그림을 그려야 해서, 렌더링 통일이 그 스펙의 전제가 됐다.
  > 같은 라운드에서 8방향 스탬프의 방향 수가 `TOPPING_OUTLINE_STAMP_COUNT`로 공개됐다 — 판정이
  > 같은 방향으로 되민 점을 읽으므로 **그리는 쪽이 정본을 갖는다.** 저장 경로(`borderLayers`가
  > PATCH에 안 실린다, OQ-P-276)는 그대로다.
- 같은 알맹이를 다른 캔버스에서 다른 테두리로 재사용할 여지가 생긴다(서버 `referenceCount`의
  전제와도 맞는다). 다만 **같은 파르페 안에서는 안 된다** — 배치가 `(parfaitId, imageId)` upsert라
  같은 이미지를 두 번 놓을 수 없다.

**트레이드오프**

- **굵기 거동이 달라진다.** 구운 테두리는 토핑을 키우면 함께 굵어졌지만, 서버 `borderWidth`는
  화면 dp 고정이라 토핑을 키워도 굵기가 그대로다. 어느 쪽이 정책인지 위키에 근거가 없다.
- 편집 화면은 `originPxPerDp`로 dp를 원본 픽셀 좌표계에 환산해 그리므로, 편집에서 본 굵기와
  캔버스에서 보이는 굵기가 어긋날 수 있다.

**위험·방어**

- 시각 회귀가 유일한 실질 위험이라 이 전환을 **PR 하나로 격리**하고 실기기 확인을 거기에 붙인다
  ([스펙의 PR 분할](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md#pr-분할-스택) 4번).
- 미리보기와 캔버스가 같은 스탬프 컴포저블을 공유하게 해, 두 그림이 갈라질 구조적 여지를 없앤다.
