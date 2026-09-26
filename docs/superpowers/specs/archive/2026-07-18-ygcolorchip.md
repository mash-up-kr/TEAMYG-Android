---
id: ygcolorchip
title: 네임태그 칩 계열 (YGNametagChip·YGUserChip·YGChipColorIndicator)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-31
related_code: YGNametagChip.kt#YGNametagChip, YGNametagChip.kt#YGNametagChipStyle, YGColorChipType.kt#YGColorChipType, YGNametagChipPreviewData.kt#YGNametagChipPreviewParameterProvider, YGUserChip.kt#YGUserChip, YGUserChip.kt#YGUserNameStyle, YGChipColorIndicator.kt#YGChipColorIndicator
related_adr: ADR-0010
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem]
---

# Spec: 네임태그 칩 계열 (YGNametagChip·YGUserChip·YGChipColorIndicator)

- 대상: `core:designsystem` — `component/ygcolorchip/`
- 관련: [ADR-0010](../../../adr/0010-custom-compositionlocal-theme.md)(자체 테마) · [design-system](../../../architecture/design-system.md) · PR #150(`feature/design-system-component-colorchip`, 최초) · PR #165(개명 + 컴포넌트 2종 추가) · 위키 정책 [[nametag-chip]]

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 목표
닉네임 첫 글자를 담는 원형 컬러칩(네임태그)과 그것을 조합한 표시 컴포넌트 묶음. 타입별로 채움/테두리/글자 색이 정해지고, 두 크기 스타일을 지원한다. 그룹 멤버 표시·프로필 원형 등이 유스케이스(위키 [[nametag-chip]] 정책의 구현체).

> **개명(#165, 2026-07-31 develop 머지)** — `YGColorChip`→`YGNametagChip`, `YGColorChipStyle`→`YGNametagChipStyle`, 파라미터 `text`→`userFirstName`, `YGColorChipPreviewData.kt`→`YGNametagChipPreviewData.kt`(provider도 동일 개명). 색 타입 `YGColorChipType`은 이름·변형 그대로다. 같은 PR에서 `YGUserChip`·`YGChipColorIndicator` 2종이 같은 패키지에 신설됐고, **패키지↔폴더 불일치가 해소**됐다(전 파일 `…component.ygcolorchip`).

## 범위
- 포함: 원형 칩 렌더(채움+테두리+중앙 텍스트), 타입별 색 매핑(`YGColorChipType`), 크기 스타일(`YGNametagChipStyle`), 칩+이름 가로 조합(`YGUserChip`), 선택 표시 점(`YGChipColorIndicator`), 타입 프리뷰.
- 제외:
  - 표시 문자 생성(닉네임 첫 글자 추출 등) — 호출자 소유. `userFirstName` 완성 문자열만 받는다.
  - 타입↔멤버 매핑 규칙 — 위키 정책([[nametag-chip]]/[[S-101-프로필-닉네임-컬러-규칙-v0.3]]) 소관. 컴포넌트는 `YGColorChipType`만 받는다.
  - "5명 이상 시 +N" 집계 — `NametagChipPlus` 타입만 제공하고, 몇 명부터 접을지·표시 문자는 호출자 몫.

## API / 인터페이스
```kotlin
sealed interface YGNametagChipStyle {
    val colorChipSize: Dp
    val colorChipWidth: Dp          // 테두리 두께
    val textStyle: TextStyle @Composable get
    data object Style28 : YGNametagChipStyle   // caption.c01R
    data object Style40 : YGNametagChipStyle   // body.b01R
}

sealed interface YGColorChipType {
    val fillColor: Color
    val strokeColor: Color
    val textColor: Color
    // NametagChip1 ~ NametagChip12, NametagChipPlus  (#223에서 13종 → 12종 + Plus로 정정)
}

@Composable
fun YGNametagChip(
    colorChipType: YGColorChipType,
    userFirstName: String,
    chip: YGNametagChipStyle,
    modifier: Modifier = Modifier,
)

sealed interface YGUserNameStyle {
    val textStyle: TextStyle @Composable get
    val textColor: Color
    data object StyleMedium : YGUserNameStyle   // body.b02R  + Gray.Gray800
    data object StyleBold : YGUserNameStyle     // body.b02SB + Gray.Gray950
}

@Composable
fun YGUserChip(
    colorChipType: YGColorChipType,
    userFirstName: String,
    chip: YGNametagChipStyle,
    userName: String,
    userStyle: YGUserNameStyle,
    modifier: Modifier = Modifier,
)

@Composable
fun YGChipColorIndicator(
    modifier: Modifier = Modifier,
    isChecked: Boolean,
)
```
- `colorChipType`: 채움/테두리/글자 색 묶음(`YGColorChipType` 변형). 호출자 주입.
- `userFirstName`: 칩 중앙 표시 문자. 호출자 주입.
- `chip`: 크기 스타일(`Style28`/`Style40`) — 지름·테두리 두께·텍스트 스타일 결정.
- `userName`(`YGUserChip`): 칩 오른쪽 이름 텍스트. 말줄임·최대 폭 제한 없음(호출자가 제약).
- `userStyle`(`YGUserChip`): 이름 텍스트의 타이포+색 프리셋.
- `isChecked`(`YGChipColorIndicator`): 켜짐이면 Cherry 점, 꺼짐이면 투명(자리 유지).

## 동작 / 상태
- 세 컴포넌트 모두 stateless presentational. 상호작용(클릭·pressed) 없음 — 순수 표시.
- `YGNametagChip`: 원형 = `clip(CircleShape)` + `background(fillColor)` + `border(colorChipWidth, strokeColor, CircleShape)`, 중앙 정렬 `Text(userFirstName, textColor, textStyle)`.
- `YGUserChip`: `Row`(수직 중앙 정렬, `YGTheme.layout.gap.gap3` 간격) = `YGNametagChip` + 이름 `Text`. 칩 크기는 `YGNametagChipStyle`, 이름은 `YGUserNameStyle`로 각각 독립 지정(조합 제약 없음).
- `YGChipColorIndicator`: 고정 지름 원(`clip(CircleShape)` + `background`). `isChecked`로 `YGAtomicColors.Cherry.Cherry` ↔ `Color.Transparent` 분기 — 꺼짐 상태에서도 크기를 차지해 레이아웃이 흔들리지 않는다.

### 스타일 매핑
| 스타일 | 지름 | 테두리 두께 | 텍스트 스타일 |
|--------|------|-------------|----------------|
| `Style28` | `colorChipSize`(28dp급) | `colorChipWidth`(가는) | `YGTheme.typography.caption.c01R` |
| `Style40` | `colorChipSize`(40dp급) | `colorChipWidth` | `YGTheme.typography.body.b01R` |

| 이름 스타일 | 타이포 | 색 |
|---|---|---|
| `StyleMedium` | `body.b02R` | `YGAtomicColors.Gray.Gray800` |
| `StyleBold` | `body.b02SB` | `YGAtomicColors.Gray.Gray950` |

### 타입 매핑
- `NametagChip1`~`NametagChip12` + `NametagChipPlus` = **13종**(색 타입 12 + 집계 표시용 Plus 1). 각 변형이 `fillColor`/`strokeColor`/`textColor`를 `YGAtomicColors`(Cherry/Melon/Pudding/Gray 계열)로 고정. 실색 값은 코드(`YGColorChipType.kt`)에서 확인.
  > 🔁 **개수 정정(#223 develop 머지, 2026-08-13)** — 구 `NametagChip1~13` + Plus = 14종에서 정렬됐다. 원인 2건: ① `NametagChip11`이 `NametagChip3`과 fill/stroke/text 3색 전부 동일한 **완전 중복**이라 뒤 항목이 한 칸씩 밀려 있었고(코드 12 = Figma 11, 코드 13 = Figma 12), ② `NametagChip9`의 `textColor`가 테두리색과 같은 `Cherry50`이라 Melon 배경에 글자가 묻혔다(Figma는 `Pudding500`). 중복 삭제 + 재번호 + 9번 글자색 정정으로 Figma 컴포넌트셋과 위키 정책 12종에 맞췄다 → [s101 스펙](2026-08-07-s101-group-side-menu.md).
- `NametagChipPlus` 용도는 #165에서 코드 주석으로 명시됐다 — **멤버 5명 이상일 때의 "+" 칩**(흰 배경 + Gray 테두리/글자).

## 파일 구성
- `component/ygcolorchip/YGNametagChip.kt` — public `YGNametagChip` + `YGNametagChipStyle`.
- `component/ygcolorchip/YGColorChipType.kt` — `YGColorChipType` 색 매핑.
- `component/ygcolorchip/YGNametagChipPreviewData.kt` — 프리뷰 데이터(`YGChipPreviewData`) + `YGNametagChipPreviewParameterProvider`(타입 전수).
- `component/ygcolorchip/YGUserChip.kt` — public `YGUserChip` + `YGUserNameStyle`.
- `component/ygcolorchip/YGChipColorIndicator.kt` — public `YGChipColorIndicator`.
- 패키지 선언은 전 파일 `…component.ygcolorchip`(폴더명 일치, #165에서 정리됨).
- 프리뷰: `YGNametagChip`·`YGChipColorIndicator`는 `@YGPreview`+`PreviewBox`(표준), `YGUserChip`은 `@Preview`+`YGCustomTheme`(표준 이탈).

## 주의 / 열린 질문
- ~~**⚠️ 패키지↔폴더 불일치(코드 결함)**~~ — **해소(#165 develop 머지, 2026-07-31)**. 세 파일이 `package …component.ygchip`으로 갈려 있던 문제가 정리돼 폴더·패키지가 `ygcolorchip`으로 일치한다.
- ~~**⚠️ 타입 개수 정책 드리프트**~~ — **해소(#223 develop 머지, 2026-08-13)**. 코드가 `NametagChip1~12` + `Plus`로 정렬돼 위키 정책 [[nametag-chip]]([[S-101-프로필-닉네임-컬러-규칙-v0.3]])의 **12종**과 일치한다. Figma 컴포넌트셋이 정본이었고 위키는 수정 불필요였다 → [open-questions](../../../synthesis/open-questions.md) [2026-07-18].
- **사용처는 프리뷰 3곳뿐(#223 시점)**: `YGNametagChipPreviewData`(타입 전수) · `core:designsystem` `YGTopBar` 프리뷰 · `app-preview` `YGTopBarPreviewScreen.MemberListSample`. **첫 화면 소비처는 S-101**이 `GroupMemberList`에서 목록 인덱스 순환으로 배정하며 열었다 — 타입 부여 주체가 미정이라 mock이다(서버 `ParfaitGroupMemberResponse`에 타입 필드 없음).
- **⚠️ 프리뷰 관용구 회귀**: `YGUserChip`이 `@YGPreview`+`PreviewBox` 표준 대신 `@Preview`+`YGCustomTheme`. #149(YGAlert·YGToast)와 같은 이탈 → [open-questions](../../../synthesis/open-questions.md) [2026-07-23].
- **`YGChipColorIndicator`·`YGUserChip` 사용처·정책 근거 부재**: 두 컴포넌트 모두 `:app-preview` 갤러리 미등록이고 feature 참조 0건. 특히 인디케이터(선택 표시 점)는 대응 정책 문서가 위키에 없다(위키 Chip-Indicator는 C-201 캘린더 소관으로 별개) → [open-questions](../../../synthesis/open-questions.md).
- **`YGChipColorIndicator` 파라미터 순서**: `modifier`가 첫 인자이고 필수 `isChecked`가 뒤 — Compose 관용구(필수 → `modifier` → 선택)와 어긋나 호출 시 named argument가 사실상 강제된다.
- **원자 색 직접 참조(과도기)**: 타입 색이 시맨틱(`YGTheme.colorScheme`) 대신 `YGAtomicColors` 직접 참조. 설계 전반 과도기 패턴 → [design-system](../../../architecture/design-system.md).
