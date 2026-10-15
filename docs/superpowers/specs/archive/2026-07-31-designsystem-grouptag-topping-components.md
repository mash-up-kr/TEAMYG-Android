---
id: designsystem-grouptag-topping-components
title: 디자인시스템 Grouptag-Chip·Topping-Group 컴포넌트 신설 (Grouptag & Topping Group Components)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-01
related_code:
  - YGGrouptagChip.kt#YGGrouptagChip
  - YGGrouptagChipType.kt#YGGrouptagChipType
  - YGToppingGroup.kt#YGToppingGroup
  - YGToppingGroupType.kt#YGToppingGroupType
  - YGToppingImage.kt#YGToppingImage
  - YGToppingTemplate.kt#YGToppingTemplate
  - SizeTokens.kt#SizeTokens
  - ComposeConfig.kt#ComposeConfig
  - ComponentCatalog.kt#componentCatalog
  - ComponentEntryBuilders.kt#componentEntryBuilders
related_adr:
related_spec:
  - designsystem-canvas-components
  - designsystem-button-missing-components
  - app-preview-component-gallery
related_architecture:
  - design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, figma-sync, g-001, topping]
---

# Spec: 디자인시스템 Grouptag-Chip·Topping-Group 컴포넌트 신설

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.
>
> **구현 상태 — ✅ develop 머지 완료(PR #186, 2026-08-01)**. 2종 + 모델 3종 + 에셋 7종(6 density) +
> `coil-network-okhttp` + `SizeTokens.Size96`·`Size160` + 갤러리 등록 전량 반영.
> 2026-08-01 기준선 점검에서 머지 코드와 대조해 **드리프트 0건** 확인 — `YGGrouptagChipType` 6종 색 매핑,
> 이름 `widthIn(Size80)`+`Ellipsis`, 타임스탬프 `maxLines`/`softWrap` 가드, 배치 7변형의 회전·소수 오프셋,
> 칩 `wrapContentWidth(unbounded = true)`, `imageModifier` 체인 끝의 `clip(RectangleShape)`,
> `AsyncImage`의 `error` 폴백이 전부 최종 결정대로다.
>
> **설계대로 확인된 것** — 회전·오프셋 7변형이 육안으로 구분되고, 칩이 160dp 프레임을 넘어가도
> 잘리지 않으며, 템플릿 6종 에셋이 위키 [[G-001-그룹-토핑-템플릿-정책-v0.2]] 시트(별×2·음표×2·
> 소용돌이×2)와 일치한다. `AsyncImage`의 `error` 폴백도 의도대로 동작해 로드 실패가 물음표로 떨어진다.
>
> **Coil 네트워크 페처 도입 효과 검증됨** — `Remote` 성공 항목에 실제 원격 이미지가 렌더된다.
> 캔버스 라운드에서 미검증으로 남았던 `YGCanvasBackground.Image`의 원인도 이로써 해소됐다(다만
> 그쪽 화면 자체의 검증은 이번 범위가 아니다).
>
> **육안 검증에서 드러난 결함 2건(수정 완료)** — 둘 다 `:app-preview` 갤러리 화면 한정이고
> `:core:designsystem` 컴포넌트에는 결함이 없었다.
> - **샘플 URL이 죽어 있었다.** 계획서가 지정한 coil 저장소의 샘플 jpg 주소가 404였다. 그래서 "정상
>   로딩" 항목이 error 폴백으로 떨어져 물음표가 떴고, **이 라운드의 핵심 검증 지점이 무력화된
>   상태**였다. live 확인한 주소로 교체해 해결했다. 컴포넌트가 아니라 검증 픽스처의 결함이다.
> - **배치 7변형 라벨을 읽을 수 없었다.** 갤러리가 `type.name`을 칩 이름으로 넘겨 80dp 말줄임에
>   걸렸고, 화면에 `TYPE_1_…`·`TYPE_3_…`으로 잘려 어느 변형인지 구분할 수 없었다. 말줄임 자체는
>   정상 동작이므로 갤러리 쪽 라벨을 짧게(`1-L`·`1-R`·`2-L`·`2-R`·`3-L`·`3-R`·`TPL`) 바꿨다.
>
> **최종 전체 브랜치 리뷰에서 드러난 결함 2건(수정 완료)** — Task 단위 리뷰 5회가 전부 통과한 뒤
> 나온 것들이다. 둘 다 **Task 하나만 보면 보이지 않는 결함**이라는 공통점이 있다.
> - **칩이 프레임의 측정 제약에 갇혀 있었다.** `Box(Modifier.size(160dp))`는 자식에게
>   `maxWidth = 160dp`를 **제약으로** 내리는데, `Modifier.offset`은 배치 위치만 옮길 뿐 제약을 풀지
>   못한다. 그래서 이름이 80dp 캡에 닿으면 타임스탬프에 46.75dp만 남아 `23시간 전` 같은 **실제
>   프로덕션 문자열이 2줄로 래핑**됐다. 설계가 다룬 "클리핑 없음"은 *그리기* 문제이고 이것은 *측정*
>   문제라 별개다 — 스펙이 이 구분을 놓쳤다. 위키 라벨 정책의 "칩 폭은 내용 기준(토핑 폭 비종속)"·
>   "상대시간 항상 전체 노출" 위반. 칩에 `wrapContentWidth(unbounded = true)`를 걸어 제약을 풀고,
>   타임스탬프에 `maxLines = 1` + `softWrap = false` 가드를 더해 해결했다.
>   Task 2는 폭 무제한 부모에서만, Task 4는 짧은 이름으로만 칩을 렌더해서 이 조합이 어느 갤러리에도
>   없었던 것이 놓친 경위다.
> - **`Remote` 이미지가 96dp를 넘쳐 나왔다.** `ContentScale.Crop`인데 클리핑이 없어 **비정사각**
>   원격 사진이 프레임과 인접 셀을 덮는다. 첫 검증 URL이 정사각이라 이 경로가 한 번도 실행되지
>   않았고, URL 교체 때 하필 또 정사각을 골라 두 번 가려졌다. `imageModifier` 체인 **맨 끝**
>   (= `rotate` 뒤, 콘텐츠에 더 가까운 안쪽)에 `.clip(RectangleShape)`를 넣어 해결했다. 순서가
>   중요하다 — `clip`이 `rotate`보다 바깥이면 회전 오버행까지 잘려 요구사항을 위반한다.
>
> 재발 방지로 갤러리에 **경계 케이스 섹션**(긴 이름 + `23시간 전` / 비정사각 원격 이미지)을 추가했고,
> 실기기에서 칩이 한 줄로 유지되며 프레임 밖으로 확장되는 것과 비정사각 사진이 96dp로 잘리는 것을
> 확인했다.
>
> 함께 반영한 것: `YGToppingImage`에 `@Immutable`(선례 `YGCanvasBackground`와 동일 방식 —
> 소비처가 G-001 스크롤 그리드라 skippable 유지가 셀마다 값어치가 있다).
>
> **반영하지 않은 리뷰 지적 1건** — `Remote`의 error 폴백이 `Crop`, `YGToppingImage.Error` 분기가
> `Fit`으로 같은 물음표를 다르게 그린다는 지적. 정사각 에셋을 정사각 박스에 넣으면 두 스케일의 배율이
> 같아 **렌더 결과가 동일**하고, 이를 맞추려면 순수 렌더 컴포넌트에 `mutableStateOf` + `onError`
> 콜백이라는 상태와 재컴포지션 경로가 생긴다. 비용이 이득을 넘어 원래 형태로 되돌렸다.
>
> 🔁 **그 불일치가 2026-08-27(PR #396)에 저절로 사라졌다** — `Remote`가 `Crop`에서 `Fit`으로 바뀌면서
> error 폴백도 같은 스케일을 쓰게 됐다. 계기는 이 지적이 아니라 **누끼가 잘리는 버그**다: 원격
> 이미지는 배경을 지운 그림이라 짧은 변을 96dp에 맞추며 긴 변을 잘라 내면 피사체가 사라진다.
> `clip(RectangleShape)`은 그대로 남되 역할이 바뀌었다 — 비정사각을 프레임에 가두는 수단이 아니라,
> 이미지가 인접 셀을 덮는 것을 막는 방어선이다(`rotate` 안쪽이어야 하는 조건은 그대로다).
>
> **미검증**: pressed 상태 — 이 두 컴포넌트는 상호작용이 없어 애초에 해당 없음.

## 목표

Figma "칩"·"기타" 영역 컴포넌트 2종(`Grouptag-Chip`·`Topping-Group`)을 `:core:designsystem`에
신설한다. 두 종 모두 대응 구현체가 없다. G-001 무한 파르페 그리드(그룹 목록) 화면 구현의 선행 작업이다.

## 범위

- **포함**
  - `YGGrouptagChip`(Figma `Grouptag-Chip`) — 이름 + 구분점 + 상대시간 pill, 타입 6종
  - `YGGrouptagChipType` — 타임스탬프 텍스트 컬러 매핑 enum 6종
  - `YGToppingGroup`(Figma `Topping-Group`) — 160dp 프레임에 토핑 이미지 + 그룹 칩 합성
  - `YGToppingGroupType` — 배치 변형 7종(회전·오프셋 상수)
  - `YGToppingImage` — 토핑 콘텐츠 3상태 sealed
  - `YGToppingTemplate` — 이미지 없음 템플릿 6종 enum
  - 토핑 템플릿 에셋 7종(6종 + Error) 6개 density 버킷 반입
  - `SizeTokens`에 `Size96`·`Size160` 추가
  - Coil 3 네트워크 페처(`coil-network-okhttp`) 도입
  - 2종을 `:app-preview` 컴포넌트 갤러리에 등록
- **제외**
  - Figma `Chip-Indicator`·`List-Date` — **다른 브랜치에서 작업 중**(작업자 확인)
  - G-001 그룹 목록 화면(`feature/groups/list`) 실구현 — 이 컴포넌트를 쓰는 쪽
  - 템플릿 6종 랜덤 부여·영속 로직 — feature/domain 책임(아래 [계층 분할](#계층-분할) 참고)
  - `YGColorChipType`(Nametag-Chip) 14종 ↔ 정책 12종 드리프트 정리 — 기존 이월 미결

## 계층 분할

위키 [[토핑]] 대체 그래픽 조항은 "6종 중 1종 랜덤 최초 부여 → 첫 토핑 등록 전까지 고정, 새로고침·재접속·타
그룹 갱신에도 불변"을 요구한다. 이 정책을 디자인시스템에 넣지 않는다. 근거 3가지:

1. **"첫 토핑 등록 전까지"는 서버 데이터에 걸린 조건.** 그룹에 토핑이 생겼는지를 UI 컴포넌트는 모른다.
2. **"조회 실패"와 "이미지 없음"은 원인이 다른 별개 상태.** URL이 null인지로 추론하면 두 상태가 뭉개진다 —
   API가 실패한 것인지, 성공했는데 토핑이 없는 것인지 구분할 수 없다.
3. 위키는 **플랫폼 비종속 정책 SoT**다. iOS도 같은 규칙을 쓰므로 규칙은 도메인 쪽에 있어야 한다.

| 책임 | 주체 |
|---|---|
| 에셋 7종 보유, 3상태 렌더, Coil 로드 실패 시 Error 그래픽 fallback | `:core:designsystem` (이번 범위) |
| 어느 상태인지 판정, 6종 중 랜덤 부여, 첫 토핑 전까지 고정·영속 | feature/domain (범위 밖) |

컴포넌트는 `YGToppingImage` 3상태를 **명시적으로 주입받는다.** 상태 추론을 하지 않는다.

## `YGGrouptagChip`

G-001에서 그룹 대표 토핑에 붙는 라벨. 이름과 상대시간을 한 줄로 보여준다.

```kotlin
@Composable
fun YGGrouptagChip(
    name: String,
    timestamp: String,
    type: YGGrouptagChipType,
    modifier: Modifier = Modifier,
)
```

구조 — `Row`, 3요소 고정(슬롯 없음):

| 요소 | 규격 |
|---|---|
| 컨테이너 | 배경 `Transparency.Black75`, `shapes.radius.round`, 가로 `padding.padding5`·세로 `padding.padding2`, `gap.gap2` |
| 이름 | `typography.body.b02SB`, `Gray.White` |
| 구분점 | 1.25dp 정사각 원(`radius.round`), `Transparency.White50` |
| 타임스탬프 | `typography.caption.c01R`, 타입별 색(아래 표) |

**이름 말줄임** — 위키 [[무한-파르페-그리드]] 라벨 정책 "이름 80px 초과 시 초과분부터 `…`"(픽셀 기준, 문자수
기준은 폐기)를 따른다. 이름 텍스트에 `widthIn(max = Size80)` + `maxLines = 1` +
`TextOverflow.Ellipsis`. 타임스탬프는 "항상 전체 노출"이므로 잘리지 않는다.

### 타입 매핑

```kotlin
enum class YGGrouptagChipType(val timestampColor: Color)
```

기준 = 해당 그룹에서 **마지막으로 변화를 가한 유저의 타입**(위키 [[nametag-chip]] ② 표).
Nametag-Chip 원형칩(`YGColorChipType`)과는 **매핑이 별개**이므로 타입도 별개로 둔다 —
정책 문서가 이미 두 표로 분리돼 있고, 기존 `YGColorChipType`은 13종 + Plus로 정책 12종과 어긋나 있다.

| enum | 정책 컬러명 | 토큰 |
|---|---|---|
| `TYPE_1_2` | 연핑크 | `Cherry.Cherry100` |
| `TYPE_3_4` | 진핑크 | `Cherry.Cherry200` |
| `TYPE_5_6` | 체리 | `Cherry.Cherry300` |
| `TYPE_7_8` | 그레이 | `Gray.Gray200` |
| `TYPE_9_10` | 멜론 | `Melon.Melon500` |
| `TYPE_11_12` | 푸딩 | `Pudding.Pudding500` |

> ⚠️ 그레이(Type 7/8)는 위키 ② 표가 `White`인데 Figma는 `Gray-200`이다. Figma를 따라 구현하고
> 위키 쪽 확인 대상으로 남긴다(아래 [열린 질문](#열린-질문)).

## `YGToppingGroup`

G-001 그리드의 셀 1개. 토핑 이미지를 살짝 기울여 얹고 우측 하단에 그룹 칩을 겹친다.

```kotlin
@Composable
fun YGToppingGroup(
    image: YGToppingImage,
    name: String,
    timestamp: String,
    chipType: YGGrouptagChipType,
    type: YGToppingGroupType,
    modifier: Modifier = Modifier,
)
```

- 프레임 **160dp 정사각**(`Size160`), 이미지 **96dp**(`Size96`)
- **클리핑 없음.** 회전·오프셋으로 프레임을 넘어가는 픽셀과 칩을 자르지 않는다(위키 G-001 오버플로우 조항).
  C-106 캔버스 클리핑은 경계가 다른 별개 규칙이라 여기 적용 대상이 아니다.
- `onClick` **없음.** C-001 이동은 호출자가 `clickableYG`로 감싼다 — 그리드 셀의 터치 범위를 셀 쪽이
  결정해야 하고, 컴포넌트가 프레임 밖 칩까지 터치 범위에 넣을지 판단할 근거가 없다.
- 이미지·칩 모두 프레임 **중심 기준 오프셋**으로 절대 배치한다(Figma가 center 기준으로 좌표를 준다).

### 콘텐츠 3상태

```kotlin
sealed interface YGToppingImage {
    data class Remote(val url: String) : YGToppingImage
    data class Template(val type: YGToppingTemplate) : YGToppingImage
    data object Error : YGToppingImage
}
```

| 상태 | 렌더 | 대응 정책 |
|---|---|---|
| `Remote` | `AsyncImage(url)`, `error` 파라미터에 Error 드로어블 지정 | 정상 대표 토핑. **Coil 로드 실패는 자동으로 Error 그래픽으로 떨어진다** |
| `Template` | 템플릿 6종 중 지정된 1종 | 그룹에 아직 첫 토핑 없음 |
| `Error` | 물음표 그래픽 1종 | 특정 토핑 조회 실패 |

세 상태 모두 **그룹 칩은 정상 노출**된다(위키 조항).

### 배치 변형

```kotlin
enum class YGToppingGroupType(
    val rotation: Float,
    val imageOffset: DpOffset,
    val chipOffset: DpOffset,
)
```

프레임 중심(0, 0) 기준. 회전은 시계방향 양수(Compose `Modifier.rotate` 규약).

| enum | 회전 | 이미지 오프셋 | 칩 오프셋 |
|---|---|---|---|
| `TYPE_1_LEFT` | −6° | (−1.25, −11.25) | (−0.5, +49.23) |
| `TYPE_1_RIGHT` | +6° | (−1.25, −11.25) | (−0.5, +49.23) |
| `TYPE_2_LEFT` | −12° | (+1.06, −12.07) | (+0.13, +54.69) |
| `TYPE_2_RIGHT` | +16° | (+1.5, −12.63) | (+0.13, +58.13) |
| `TYPE_3_LEFT` | +8° | (−0.79, −10.79) | (−0.5, +49.5) |
| `TYPE_3_RIGHT` | +8° | (−0.79, −10.79) | (−0.5, +49.5) |
| `TEMPLATE` | 0° | (−0.79, −10.79) | (−0.5, +49.5) |

- **Figma 소수값을 반올림하지 않고 그대로 쓴다.** Compose는 소수 dp를 그대로 다루므로 정밀도를 버릴
  이유가 없고, 반올림하면 나중에 Figma와 대조할 때 "의도적 차이"인지 "드리프트"인지 구분이 안 된다.
- Left/Right 선택과 변형 번호(1/2/3) 랜덤 재부여는 **호출자 책임**이다(위키: index%2로 side 결정,
  번호는 목록 조회 응답 1회 재추첨·리렌더 시 재추첨 금지).
- `TEMPLATE`은 회전 0°인 **배치 변형**일 뿐이고, 안에 무엇을 그릴지는 `image` 파라미터가 정한다.
  `TEMPLATE` + `Remote` 조합도 컴파일된다 — 모순 조합 방지 책임은 호출자에게 있다(캔버스 라운드에서
  불리언 플래그로 통일하며 받아들인 것과 같은 트레이드오프).

> ⚠️ `TYPE_3_LEFT`와 `TYPE_3_RIGHT`가 Figma에서 회전·오프셋이 **완전히 동일**하다. 다른 Left 변형은
> 전부 음수 회전인데 3번만 Left도 양수(+8°)다. 디자인 의도인지 Figma 누락인지 불명 — Figma 그대로
> 구현하고 확인 대상으로 남긴다(아래 [열린 질문](#열린-질문)).

## 에셋

원본: 작업자 제공 `Topping-Template/Android/{ldpi,mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`
(`Template01~06.png` + `Template-Error.png`).

- 반입 위치: `core/designsystem/src/main/res/drawable-{density}/` 6버킷
- 파일명은 안드로이드 리소스 규칙(소문자·스네이크)으로 변환: `img_topping_template_01.png` …
  `img_topping_template_06.png`, `img_topping_template_error.png`
- 에셋 6종의 그림 내용은 위키 [[G-001-그룹-토핑-템플릿-정책-v0.2]](별×2·음표×2·소용돌이×2)와 일치.
  구 v0.1의 음식 이름 6종(딸기·키위·…) 서술은 이미 위키에서 상충으로 등록된 항목이라 여기서 다루지 않는다.

## 빌드 변경

**Coil 3 네트워크 페처 도입** — 현재 `coil-compose`만 있고 네트워크 페처가 없어 원격 URL이 로드되지
않는다. 캔버스 라운드에서 `YGCanvasBackground.Image`가 미검증으로 남은 원인이 이것이다.
`YGToppingGroup.Remote`가 같은 문제를 그대로 물려받으므로 이번 라운드에서 해소한다.

- `gradle/libs.versions.toml` — `coil-network-okhttp` 라이브러리 항목 추가(`coil` 버전 참조 재사용)
- `build-logic` `ComposeConfig.kt` — `implementation(libs.coil.network.okhttp)` 추가.
  기존 `coil-compose`가 여기 있으므로 같은 자리에 둔다.

부수 효과로 `YGCanvasBackground.Image`의 원격 로딩도 함께 살아난다.

## 크기 토큰

`SizeTokens`에 `Size96`·`Size160` 추가. 둘 다 **Figma가 고정한 치수**다(토핑 이미지 96, 프레임 160).
버튼 라운드에서 세운 원칙 — "패딩으로 도출되는 치수는 하드코딩하지 않고, Figma가 고정한 곳만 토큰으로
못박는다" — 에 해당한다.

## 검증

기존 디자인시스템 라운드와 동일하게 **테스트 없이 프리뷰 + 실기기 갤러리 육안 검증**으로 간다.
컴포넌트가 상태 없는 순수 렌더라 단위 테스트가 잡아낼 회귀가 거의 없고, 실제 결함(색 대비·잘림·오프셋)은
육안 검증에서만 드러난다는 선행 라운드 판단을 유지한다.

- `@YGPreview` + `PreviewBox` 프리뷰: `YGGrouptagChip` 6타입 + 긴 이름 말줄임 케이스,
  `YGToppingGroup` 배치 7변형 × 콘텐츠 3상태
- `:app-preview` 갤러리에 2종 등록(카테고리는 기존 그룹 체계에 맞춤)
- `:core:designsystem`·`:app-preview` `assembleDebug` + repo 전체 `ktlintCheck`
- 실기기에서 `Remote` 성공 상태 로딩 확인(네트워크 페처 도입 효과 검증 포함)
- **TJYG-Android 커밋은 하지 않는다**(작업자 지시). 작업 트리 변경만 남기고 보고한다.

## 열린 질문

아래 1~3은 [parfait open-questions](../../../synthesis/open-questions.md)에 [2026-07-31] 항목으로 등록했다.
1번은 정책 문서 자체의 문제이므로 위키 open-questions에도 등록 대상이다(정책 SoT는 위키).

1. **그레이 타입 타임스탬프 색** — 위키 [[nametag-chip]] ② 표는 Type 7/8 = `White`, Figma는
   `Gray-200`. Figma 우선 구현했으나 어느 쪽이 정본인지 확인 필요. 위키 정정 대상일 수 있다.
2. **`TYPE_3_LEFT`/`TYPE_3_RIGHT` 동일** — 회전·오프셋이 같아 두 변형이 시각적으로 구분되지 않는다.
   Left가 음수 회전이어야 하는데 Figma가 양수(+8°)인 것도 함께 확인 필요.
3. **템플릿 부여 주체** — 서버가 `templateType`을 내려주면 재접속·기기변경에도 불변이고 iOS와 같은
   그림이 나온다. 클라이언트 랜덤 + 로컬 영속은 기기변경에서 깨진다. API 미확정.
4. **`YGColorChipType` 13종 + Plus ↔ 정책 12종 불일치** — 기존 이월 미결. 이번 신설한
   `YGGrouptagChipType`은 정책 6타입(=12종 쌍)과 정확히 일치하므로 두 enum의 타입 수가 어긋난 상태로
   공존한다. Nametag 쪽 정리 시 함께 봐야 한다.
