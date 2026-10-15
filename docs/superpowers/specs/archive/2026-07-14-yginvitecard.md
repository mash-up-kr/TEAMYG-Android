---
id: yginvitecard
title: 그룹 초대 카드 (YGInviteCard)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-18
related_code: YGInviteCard.kt#YGInviteCard, YGInviteCardStatus.kt#YGInviteCardStatus, YGButton.kt#YGButton, YGButtonType.kt#SmallSquare
related_adr:
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, card]
---

# Spec: 그룹 초대 카드 (YGInviteCard)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
> 🔁 [2026-07-22] 이 스펙은 초기 baseline. 테두리 `shape`·`.clip` 모두 `medium1→none`(카드 각짐)·`InviteCodeBox` clip `small→none` 변경이 PR #159로 **develop 머지 완료** → [2026-07-19-designsystem-radius-none-sync](2026-07-19-designsystem-radius-none-sync.md)(현행 값의 단일 출처).

## 목표
그룹 초대 코드를 노출하고 복사 버튼을 제공하는 `core:designsystem` 카드 컴포넌트.
정원 여유(Active)·정원 초과(Invalid) 두 상태를 색과 버튼 활성으로 구분한다.

## 범위
- 포함: 라벨 + subText + 초대 코드 박스 + 복사 버튼(`YGButton` SmallSquare 재사용) 배치, Active/Invalid 상태별 토큰 분기, Active/Invalid 프리뷰.
- 제외:
  - subText 문구 생성("N명 남음"/"최대 인원 도달") — 호출자(feature) 소유. 컴포넌트는 완성된 문자열만 받는다.
  - 클립보드 복사·토스트 — `onCopyClick` 콜백만 노출, 실제 복사는 feature 레이어.
  - 복사 버튼 색 정의 — `YGButtonType.SmallSquare` 소관(fix/ygbutton #140 develop 머지됨). 본 카드는 상태로 `isEnabled`만 제어.
  - 카드 너비 고정 — `modifier`로 호출자가 결정(Figma의 335 고정폭은 프레임 예시).

## API / 인터페이스
```kotlin
enum class YGInviteCardStatus { Active, Invalid }

@Composable
fun YGInviteCard(
    label: String,
    inviteCode: String,
    subText: String,
    status: YGInviteCardStatus,
    copyButtonText: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes endIconResource: Int? = R.drawable.ic_copy,
)
```
- `label`: 라벨 문구("그룹 초대 코드" 등). 호출자 주입.
- `inviteCode`: 코드 박스에 표시할 초대 코드 문자열.
- `subText`: 라벨 우측 보조 문구(호출자가 완성해 주입). 색만 상태 파생.
- `status`: `Active`(정원 여유) / `Invalid`(정원 초과). 색·버튼 활성 분기의 단일 소스.
- `copyButtonText`: 복사 버튼 텍스트("복사" 등). 호출자 주입.
- `onCopyClick`: 복사 버튼 클릭 콜백. `Invalid`에서는 버튼 비활성이라 미발화.
- `modifier`: 기본 `Modifier`. 너비 등 배치는 호출자 결정. 카드 내부 자식은 가용 폭을 채운다.
- `endIconResource`: 복사 버튼 끝 아이콘. 기본 `R.drawable.ic_copy`, 주입으로 override. `null`이면 아이콘 없음.

## 동작 / 상태
- 상태는 런타임 파생이 아니라 **prop(`status`)** 으로 주입. 컴포넌트는 stateless presentational.
- 복사 버튼: `YGButton(text = copyButtonText, buttonType = YGButtonType.SmallSquare, isEnabled = status == Active, onClick = onCopyClick, endIconResource = endIconResource)`.

### 상태 → 토큰 매핑 (심볼명)
| 요소 | Active | Invalid |
|------|--------|---------|
| 카드 border 색 | `YGAtomicColors.Cherry.Cherry100` | `YGAtomicColors.Gray.Gray100` |
| subText 색 | `YGAtomicColors.Gray.Gray600` | `YGAtomicColors.Cherry.Cherry600` |
| 코드 박스 배경 | `YGAtomicColors.Cherry.Cherry100` | `YGAtomicColors.Gray.Gray200` |
| 코드 텍스트 색 | `YGAtomicColors.Gray.Gray900` | `YGAtomicColors.Gray.Gray500` |
| 복사 버튼 `isEnabled` | `true` | `false` |

### 상태 무관 고정 토큰
| 요소 | 토큰 |
|------|------|
| 카드 배경 | `YGAtomicColors.Gray.White` |
| 카드 radius | `YGTheme.shapes.radius.medium1` |
| 카드 border 두께 | `SizeTokens.Size1` |
| 카드 padding | 세로 `YGTheme.layout.padding.padding5`, 가로 `YGTheme.layout.padding.padding6` |
| 카드 세로 간격 | `YGTheme.layout.gap.gap3` |
| 라벨 텍스트 | `label` 파라미터, `YGTheme.typography.body.b02R`, `YGAtomicColors.Gray.Gray400` |
| subText 타이포 | `YGTheme.typography.body.b02R`, 우측 정렬 |
| 라벨 Row 간격 | `YGTheme.layout.gap.gap3` |
| 코드 박스 컨테이너 Row 간격 | `YGTheme.layout.gap.gap4` |
| 코드 박스 radius | `YGTheme.shapes.radius.small` |
| 코드 박스 padding | 세로 `YGTheme.layout.padding.padding3`, 가로 `YGTheme.layout.padding.padding8` |
| 코드 텍스트 타이포 | `YGTheme.typography.body.b01SB` |

## 표시·제어 규칙
- 라벨 Row: 라벨(시작) + subText(끝), subText는 남는 폭을 차지하고 텍스트 우측 정렬.
- 코드 박스 Row: 코드 박스(가용 폭 채움) + 복사 버튼(Hug). 세로 중앙 정렬.
- 코드 텍스트: 박스 내 가운데 정렬(Figma가 수평 정렬 미명시 → 시각 기준 center). start 정렬 요구 시 조정.
- 복사 버튼은 Invalid에서도 렌더(비활성 표시), 숨기지 않는다.

## 파일 구성
- `core/designsystem/.../component/card/YGInviteCard.kt` — 기존 stub 채움. public `YGInviteCard` + 필요 시 private 헬퍼(코드 박스). `YGInviteCardStatus` enum 동거.
- 프리뷰: `@YGPreview` + `PreviewBox`(모듈 관례). Active/Invalid 2종.

## 주의 / 열린 질문
- 코드 텍스트 수평 정렬(center vs start): Figma 미명시. 현재 center 가정. 디자이너 확인 후 확정.
- 코드 박스 padding은 gap 토큰(`layout.gap.gap7`/`gap3`)에서 **padding 토큰(`layout.padding.padding8`/`padding3`)으로 교정**됨(refactor `YGInviteCard gap 호출 수정`, #136 브랜치 — 패딩에는 padding 토큰 사용). 라벨 Row 간격·border 두께(`SizeTokens.Size1`) 치환은 유지. 토큰 실값이 Figma(8/1px 등)와 일치하는지는 프리뷰 육안 확인 대상.
