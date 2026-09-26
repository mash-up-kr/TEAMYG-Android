---
id: ADR-0018
title: 배경 블러에 Haze 도입 (자체 GraphicsLayer 구현 기각)
status: accepted
date: 2026-08-01
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec:
  - designsystem-bar-listdate-components
  - c101-camera-picture-confirm
related_architecture:
  - design-system
platforms: android
tags: [adr, parfait, designsystem, blur, top-bar]
---

# ADR-0018: 배경 블러에 Haze 도입 (자체 GraphicsLayer 구현 기각)

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

> ✅ **develop 머지(2026-08-04, PR #188)** — 이 결정이 코드로 들어갔다. `libs.versions.toml` `haze`
> 항목 + `ComposeConfig` 배선, `YGTopBarEmpty(hazeState: HazeState? = null)`, 분기는 private
> `Modifier.ygTopBarBackdrop`(`null`이면 `drawBehind` 틴트, 아니면 `hazeEffect`),
> 반경 `YGTopBarDefaults.BackdropBlurRadius`. 결정과 구현의 갈림 없음.
> **다만 실화면 배선은 아직 없다** — G-001 `GroupListScreen`이 `hazeState`를 넘기지 않아 앱에서는
> 틴트만 보인다(갤러리 데모에서만 블러가 산다) → [open-questions](../synthesis/open-questions.md) [2026-08-04].

## 맥락

Figma `Top Bar`의 `Status=Default`·`Status=Empty`가 반투명 배경(`Transparency.White75`) 위에
**배경 블러 4**를 요구한다. 바 뒤로 콘텐츠가 스크롤해 지나가는 화면(G-001 그룹 목록)에서 바가
콘텐츠를 가리지 않으면서 텍스트 가독성을 유지하려는 장치다.

**Compose는 배경 블러를 기본 제공하지 않는다.** `Modifier.blur`와 `BlurEffect`는 *자기 자신과 자식*을
흐린다. 뒤에 있는 것을 흐리려면 배경을 별도 레이어로 캡처해 블러를 걸고 소비 표면 영역에 다시 그려야
하는데, 그 배선은 컴포넌트 단독으로 닫히지 않는다 — 무엇이 "뒤"인지는 호출 화면만 안다.

같은 요구가 C-101 카메라 뷰파인더(위키 [[카메라-뷰파인더]])에도 있어, 처음에는 그쪽이 세운
`GraphicsLayer` 2회 그리기 관용을 재사용하기로 했다.

## 결정

**배경 블러는 `dev.chrisbanes.haze`(1.7.2)로 구현한다. 자체 `GraphicsLayer` 구현은 기각한다.**

- `gradle/libs.versions.toml`에 `haze` 항목, Compose 컨벤션 플러그인(`ComposeConfig`)에 배선.
  `coil-compose`가 이미 그 자리에 있으므로 같은 곳에 둔다.
- 블러 소스(배경)는 `Modifier.hazeSource(state)`, 소비 표면(Top Bar)은 `Modifier.hazeEffect(state)`.
- **`HazeState`는 컴포넌트가 만들지 않고 호출 화면이 소유한다.** Top Bar는 `hazeState`를 파라미터로
  받는다. 무엇이 블러 소스인지는 화면의 판단이다.
- `hazeState`는 **nullable, 기본값 `null`**. `null`이면 블러 없이 `White75` 틴트만 그린다.
  갤러리·프리뷰처럼 뒤에 스크롤 콘텐츠가 없는 맥락에서 억지로 상태를 만들게 하지 않는다.
- 블러 반경은 `YGTopBarDefaults.BackdropBlurRadius`로 노출한다.
- 1.x 계열을 쓴다. 2.0은 alpha이고 `blurEffect {}` 래퍼·`haze-blur` 모듈 분리로 API가 갈린다.

## 대안

- **대안 A: 자체 `GraphicsLayer` + `BlurEffect`** — 의존성이 늘지 않고 C-101과 관용이 하나로 유지된다.
  초안은 이쪽으로 정했다. 그러나 **실기기에서 세 가지 형태 모두 블러가 걸리지 않았다**(아래 기각 근거).
  **→ 기각:** 동작하지 않는다.
- **대안 B: 블러를 포기하고 틴트만** — 가장 싸다. 그러나 Figma 정본과 어긋난 채로 남는다.
  **→ 기각:** 정본 불일치를 남기지 않기로 함(작업자 결정).
- **대안 C: 반투명 배경 + 스크림 강화로 흉내** — "뒤가 비쳐 보이되 흐리다"는 의도가 사라져 다른
  디자인이 된다.
  **→ 기각:** 요구를 충족하지 않는다.

### 대안 A를 기각한 실측 근거

40dp 같은 극단값으로 대조하며 세 형태를 시도했고 전부 블러가 나타나지 않았다.

| 시도 | 결과 |
|---|---|
| `blurLayer.record { drawLayer(backdropLayer) }` → `drawLayer(blurLayer)` | 블러 없음 |
| 위에서 `record`와 `renderEffect` 설정 순서 교체 | 블러 없음 |
| 배경 쪽이 레이어를 직접 `record`하고 `renderEffect`를 건 뒤 Top Bar가 그림 | 블러 없음 |
| **(대조)** `blurLayer.record { drawRect(위 절반만) }` | **경계가 크게 번짐** |

마지막 대조가 중요하다 — `record` 안에 **직접 그린** 도형에는 `renderEffect`가 정상으로 걸린다.
즉 `BlurEffect`도 `GraphicsLayer`도 고장 난 게 아니다. 배경 콘텐츠를 레이어로 옮겨 담는 경로에서만
효과가 사라진다. 정확한 기전은 규명하지 못했고, 규명 비용이 라이브러리 하나보다 크다고 판단했다.

**Haze는 같은 조건(실기기 API 36, 반경 4·40 대조)에서 즉시 동작했다.**

> ⚠️ **C-101 설계도 같은 구조다.** 카메라 뷰파인더의 확정 설계가
> `contentLayer.record{...}` → `blurLayer.record{drawLayer(contentLayer)}` → `drawLayer(blurLayer)`로
> 여기서 실패한 형태와 같다. 아직 구현 전(게이트 PoC 단계)이므로, 그 라운드를 시작할 때 이 결정을
> 먼저 보고 Haze 재사용을 검토할 것.
>
> 📌 **후속(2026-08-01, PR #182 develop 머지)** — C-101은 위 검토 없이 **자체 `GraphicsLayer`로
> 머지됐다**(`feature/camera/impl` `CameraFeedLayer`, 위와 정확히 같은 record→record→draw 형태 +
> `Black25` 스크림 + 뷰파인더 영역 `clipRect` 복원). 여기서 기각한 경로와 **대상이 다르다**는 것이
> 유일한 차이다 — C-101이 흐리는 것은 *자기 자식*(`AndroidView`로 붙인 카메라 피드)이고, 이 ADR이
> 막힌 것은 *자기 밖 배경*을 옮겨 담는 경로다. 그래서 이 결정은 **배경 블러에 한정**해 유효하고,
> 프로젝트에 블러 관용구가 둘 존재하게 됐다. C-101 블러의 실동작은 이 ADR이 요구한 **극단값 대조로
> 아직 확인되지 않았다** → [open-questions](../synthesis/open-questions.md) [2026-08-01] ·
> [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md).

## 영향

**긍정**

- Figma 정본과 일치하는 배경 블러를 실제로 얻는다(실기기 확인).
- `hazeState`를 호출자가 소유하므로 디자인시스템 컴포넌트가 화면 구조를 알 필요가 없다.
- C-101이 같은 도구로 풀릴 가능성이 열린다 — 자체 구현으로는 두 곳 모두 막혔을 것이다.

**트레이드오프**

- 외부 의존성 1개 추가. 렌더링처럼 교체 비용이 큰 층위라 가볍게 되돌리기 어렵다.
- Top Bar 공개 시그니처에 `hazeState` 파라미터가 붙는다(기본값 `null`이라 기존 호출부는 무변경).
- 블러 반경이 디자인시스템 밖으로 노출된다(`YGTopBarDefaults.BackdropBlurRadius`).

**위험·방어**

- ⚠️ **API 31 미만에서는 실제 블러가 없다.** 안드로이드 배경 블러는 `RenderEffect` 기반이고 그것이
  API 31부터다. 이 프로젝트 `minSdk`는 **26**이므로 26~30 기기에서는 틴트만 남는다. Haze의 한계가
  아니라 플랫폼 제약이고, 어떤 대안으로도 해결되지 않는다.
  → `hazeEffect`의 `tints`에 `White75`를 넣어 블러가 없어도 가독성이 유지되게 한다.
- ⚠️ API 31에 스크롤 중 블러가 갱신되지 않는 upstream 이슈가 보고돼 있다(#77).
  → 검증 기기가 API 36이라 이 경로는 실행되지 않았다. 구형 기기 확보 시 확인 대상.
- ⚠️ **블러는 어긋나도 눈에 잘 띄지 않는다.** 틴트만 걸려도 "뒤가 좀 연하네" 정도로 보여서, 이번에
  블러가 전혀 동작하지 않는 상태가 육안 검증을 그대로 통과했다.
  → 블러를 검증할 때는 **반드시 극단값(예: 40dp) 대조**를 한다. 사양값 한 번만 보고 판단하지 않는다.
- 검증은 이 저장소의 디자인시스템 관례대로 **프리뷰 + 실기기 육안**으로 한다.
