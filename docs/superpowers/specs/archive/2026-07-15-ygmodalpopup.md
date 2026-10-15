---
id: ygmodalpopup
title: 모달 팝업 (YGModalPopup)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-18
related_code: YGModalPopup.kt#YGModalPopup, YGButton.kt#YGButton, YGButtonType.kt#Medium.Secondary, YGButtonType.kt#Medium.Primary
related_adr:
related_spec:
related_architecture: design-system
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, modal, dialog]
---

# Spec: 모달 팝업 (YGModalPopup)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 목표
아이콘 + 제목 + 본문 + 2버튼(Secondary 좌 / Primary 우)으로 구성된 `core:designsystem` 중앙 모달 팝업.
Compose `Dialog` 위에 파르페 디자인 토큰으로 렌더한다. destructive 확인("그룹에서 나갈까요?" 등)이 대표 유스케이스.
컴포넌트는 **버튼의 confirm/cancel 의미를 규정하지 않는다** — 버튼 타입(Secondary/Primary)만 노출하고 어느 쪽을 확인/취소로 쓸지는 호출자가 결정한다.

## 범위
- 포함: 아이콘·제목·본문·2버튼 배치, 디자인 토큰 매핑, `Dialog` 래핑, dismiss 제어 노출, 프리뷰.
- 제외:
  - 본문·버튼 문구 생성 — 호출자(feature) 소유. 컴포넌트는 완성된 문자열만 받는다.
  - 버튼 액션 실제 동작(그룹 탈퇴 등) — `onSecondaryClick`/`onPrimaryClick` 콜백만 노출.
  - 버튼 의미(confirm/cancel) 규정 — 호출자가 각 타입에 의미 부여.
  - 표시 여부 상태 관리 — 호출자가 조건부로 컴포저블을 부르거나 안 부른다(내부 visible 플래그 없음).
  - 버튼 개수 가변 — **항상 2버튼 고정**(1버튼·N버튼 미지원). 필요 시 후속 확장.
  - width 고정 제어 — 플랫폼 기본 다이얼로그 폭에 맡긴다(`usePlatformDefaultWidth` 미변경).

## API / 인터페이스
```kotlin
@Composable
fun YGModalPopup(
    title: String,
    body: String,
    @DrawableRes iconRes: Int,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabledButton: Boolean = true,
    iconTint: Color = YGAtomicColors.Cherry.Cherry600,
    properties: DialogProperties = DialogProperties(),
)
```
- `title`: 제목 문구. 호출자 주입.
- `body`: 본문 문구(여러 줄 가능). 호출자가 완성해 주입.
- `iconRes`: 상단 아이콘 리소스(`@DrawableRes`). 대표값 `R.drawable.ic_warning_round`, 호출자 주입.
- `secondaryText` / `onSecondaryClick`: **Secondary 스타일 버튼(좌측, 회색)** 텍스트·클릭. 의미(확인/취소)는 호출자 결정.
- `primaryText` / `onPrimaryClick`: **Primary 스타일 버튼(우측, 검정)** 텍스트·클릭. 의미는 호출자 결정.
- `onDismissRequest`: `Dialog` dismiss 요청(뒤로가기·바깥 탭) 콜백. 호출자 소유.
- `modifier`: 루트 컨테이너 배치용. 기본 `Modifier`.
- `isEnabledButton`: **두 버튼 공통** 활성 여부(단일 플래그). 기본 `true`. 양 `YGButton.isEnabled`에 동일 전달(비활성 시 색·클릭 차단은 `YGButtonType` 소관).
- `iconTint`: 아이콘 틴트. 기본 `YGAtomicColors.Cherry.Cherry600`(Figma warning red). 호출자 override 가능.
- `properties`: `Dialog` 동작 제어(뒤로가기·바깥 탭 dismiss 등). 기본 `DialogProperties()`.

## 동작 / 상태
- Stateless presentational. 내부 상태 없음(표시 여부 분기 없음).
- dismiss(뒤로가기·바깥 탭)는 `properties`로 제어, 발생 시 `onDismissRequest` 발화.
- 버튼 활성은 **단일 `isEnabledButton`**(기본 `true`)이 양 버튼에 공통 적용. 개별 분리 제어 없음. `YGButton.isEnabled`로 전달.

### 버튼 타입 ↔ 배치 매핑
| 버튼 타입 | 파라미터 | 위치 |
|-----------|----------|------|
| `YGButtonType.Medium.Secondary` | `secondaryText`/`onSecondaryClick` | 좌 |
| `YGButtonType.Medium.Primary` | `primaryText`/`onPrimaryClick` | 우 |

- confirm/cancel 등 **의미 매핑은 호출자 소관**(컴포넌트는 타입만 노출). 대표 유스케이스(그룹 나가기)에선 Secondary="나가기", Primary="취소".

## 레이아웃 / 토큰 매핑 (심볼명)
| 요소 | 토큰 / 값 |
|------|-----------|
| 루트 배경 | `YGAtomicColors.Gray.White` (Figma Base/White) |
| 루트 radius | `YGTheme.shapes.radius.medium1` |
| 루트 padding | top `YGTheme.layout.padding.padding5`, 좌/우/하 `YGTheme.layout.padding.padding6` |
| 루트 세로 간격(Contents↔Action) | `YGTheme.layout.gap.gap5` |
| 루트 정렬 | `Alignment.CenterHorizontally`, 폭은 `fillMaxWidth` |
| Contents 세로 간격(아이콘↔텍스트) | `YGTheme.layout.gap.gap2`(`spacedBy`) |
| 아이콘 박스 크기 | `SizeTokens.Size48` |
| 아이콘 이미지 크기 | `SizeTokens.Size32`(박스 안 중앙) |
| 아이콘 틴트 | `iconTint`(기본 `Cherry.Cherry600`) |
| Title 타이포 | `YGTheme.typography.title.t03SB`, center |
| Title 색 | `YGAtomicColors.Gray.Gray900` |
| Body 타이포 | `YGTheme.typography.body.b02R`, center |
| Body 색 | `YGAtomicColors.Gray.Gray500` |
| Title↔Body 세로 간격 | `YGTheme.layout.gap.gap2` |
| Action Area 가로 간격 | `YGTheme.layout.gap.gap3` |
| Action Area 폭 | `fillMaxWidth`, 각 버튼 `Modifier.weight(1f)` |

## 표시·제어 규칙
- 루트: `Dialog(onDismissRequest, properties)` 내부 `Column`(`fillMaxWidth` + 배경·radius·clip·padding).
- Contents `Column`: 아이콘(`SizeTokens.Size48` 박스 안 `SizeTokens.Size32` 이미지, `iconRes` 틴트 `iconTint`) + 텍스트 `Column`(Title/Body, center).
- Action Area `Row`: Secondary 버튼(좌, `weight(1f)`) → Primary 버튼(우, `weight(1f)`).
- 두 버튼 폭은 `weight(1f)`로 균등 분할.

## 파일 구성
- `core/designsystem/.../component/modal/YGModalPopup.kt` — public `YGModalPopup`(Dialog 래핑) + private `YGModalPopupContent`(레이아웃).
- 프리뷰: `@YGPreview` + `PreviewBox`(모듈 관례). Figma 예시("그룹에서 나갈까요?" + `ic_warning_round` + Secondary "나가기"/Primary "취소").

## 주의 / 열린 질문

- **첫 실화면 소비처(2026-08-12, PR #224)** — A-005 그룹 생성·A-004 초대코드가 확인 모달로 쓴다
  ([a005](2026-07-29-a005-group-create.md)·[a004](2026-08-12-a004-group-invite-code.md) 스펙).
  ① "표시 여부는 호출자" 규약은 그대로 지켜졌고(각 UiState `isConfirmPopupVisible`),
  ② 두 화면 다 **취소=좌 Secondary / 실행=우 Primary**라 [Danger Zone 팝업 스펙](2026-08-09-setting-danger-zone-popups.md)의
  반대 배치와 갈렸으며, ③ 위 "버튼 개별 비활성 불가"가 실제로 걸렸다 — A-005가 생성 중
  `isEnabledButton = isCreating.not()`을 주는 탓에 취소 버튼까지 함께 잠긴다
  → [open-questions](../../../synthesis/open-questions.md) [2026-08-12].
- **반대 배치가 develop에 공존 확정(2026-08-13, PR #225)** — Danger Zone 확인 팝업 3종(서비스 탈퇴 ·
  그룹 나가기 · 그룹 신고)이 머지되면서 **파괴적 액션=좌 Secondary / 취소("그만두기")=우 Primary**가
  develop 코드가 됐다. 같은 컴포넌트에서 "오른쪽 Primary가 무엇인가"가 화면 성격에 따라 갈리는 것이
  이제 가정이 아니라 사실이다(#224 3화면 = 실행이 우 / #225 3팝업 = 취소가 우). 네 인자가 전부 같은
  타입이고 `Dialog`가 프리뷰에 안 떠서 **뒤바꿈을 잡는 자동 검증이 여전히 0건**이다.
- **API 재설계(#135 브랜치 refactor)**: 초기 `confirmText`/`onConfirm`/`cancelText`/`onCancel` + `confirmEnabled`/`cancelEnabled` 시맨틱 API에서, **버튼 타입 기준**(`secondaryText`/`onSecondaryClick`·`primaryText`/`onPrimaryClick`) + **단일 `isEnabledButton`**으로 변경. 컴포넌트가 confirm/cancel 의미를 규정하지 않게 되고, 버튼 활성 개별 제어가 사라짐(양 버튼 공통).
- **버튼 개별 비활성 불가**: `isEnabledButton` 단일 → 한쪽만 비활성(예: 확인만 disable) 요구 시 개별 플래그 재분리 필요. 현재 미지원.
- **Title 색**: Figma `#333333`. 정확 매칭 아토믹 토큰 없음(`Gray.Gray850`=#333537 근사, `Gray.Gray900`=#29292C). **해소** — 구현(#135 브랜치 atomic color refactor)에서 `YGAtomicColors.Gray.Gray900` 채택(하드코딩 리터럴 폐기, 아토믹 토큰 사용). Figma #333333과 미세 차이(#29292C)는 디자인 토큰 우선 방침에 따라 수용. 육안 확인 대상.
- **아이콘 에셋 스케일**: Figma `Ic_Warning_Round` 48×48(내부 Union 원 ~25px). 리소스 `ic_warning_round.xml`은 24dp viewport. 구현은 `SizeTokens.Size48` 박스 안에 `SizeTokens.Size32` 이미지로 그림(24dp 에셋 → ~1.33배 스케일) → 링 두께·비율 미세 차이 가능. 프리뷰 육안 확인 대상, 필요 시 전용 에셋.
- **width**: 고정 제어 안 함(플랫폼 기본 폭). Figma Hug(312)·Contents 206·Action 280 프레임 수치는 예시로 간주. 본문 줄바꿈은 실제 다이얼로그 폭 파생.
- **iconRes 필수 여부**: 현재 non-null 필수. 아이콘 없는 팝업 요구 시 nullable 확장.
