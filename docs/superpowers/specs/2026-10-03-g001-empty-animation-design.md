---
id: g001-empty-animation
title: 그룹 목록 Empty 안내 애니메이션 (G-001-Empty intro animation)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-10-03
related_code:
  - feature/groups/list/impl/.../route/GroupListScreen.kt#GroupListScreen
  - feature/groups/list/impl/.../route/GroupListViewModel.kt#GroupListUiState
  - feature/groups/list/impl/.../route/component/GroupListTopBar.kt#GroupListTopBar
  - feature/groups/list/impl/.../route/component/GroupListTooltip.kt#GroupListTooltip
  - feature/groups/list/impl/.../route/component/ToppingLayout.kt#ToppingLayout
  - core/designsystem/.../component/ygtoppinggroup/YGToppingImage.kt#YGToppingImage
  - core/designsystem/.../component/ygtopbar/YGTopBar.kt#YGTopBarEmpty
related_adr:
related_spec: g001-group-list
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, G-001, animation]
---

# Spec: 그룹 목록 Empty 안내 애니메이션

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

그룹이 0건인 사용자가 G-001에 설 때, 빈 파르페 위에 더미 그룹 3개와 안내 툴팁을 순서대로
등장시켜 "그룹을 만들면 이렇게 쌓인다"를 보여 준다. 화면을 탭하면 함께 사라진다.

이슈 #579. 디자인 원본은 Figma `G-001-Empty` 프레임 둘이다 — 등장 `5417:6332`, 탭 종료
`5685:18397`.

## 범위

- 포함
  - 더미 그룹 3개의 등장·종료 애니메이션과 더미 이미지 에셋
  - 툴팁의 등장·종료 애니메이션, 문구·색 교체, 팝업에서 일반 컴포저블로의 전환
  - 상단 바 표기를 날짜·요일에서 "내 그룹 N"으로 교체
- 제외
  - 크림·컵을 윤곽선으로 그리는 빈 파르페 그래픽(Figma `ParfaitTemplate`). 기존 채워진
    이미지를 그대로 쓴다
  - 위키 반영. 이 정책 원본은 `wiki/raw`에 아직 없다

## 동작 / 상태

### 재생 조건

- 그룹 목록이 **0건으로 확정**되면(`groupList`가 `null`이 아닌 빈 목록) 타임라인을 시작한다.
  아직 한 번도 받지 못한 동안에는 시작하지 않는다.
- 화면에 **진입할 때마다 1회** 재생한다. 최초 실행뿐 아니라 다른 화면에서 돌아온 경우를
  포함한다. 최초 1회 플래그는 두지 않는다.
- 당겨서 새로고침은 타임라인을 다시 시작하지 않는다.

### 단계

| 단계 | 조건 | 화면 |
|---|---|---|
| `Entering` | 타임라인 시작 ~ 3,000ms | 더미·툴팁이 차례로 등장. 탭으로 종료할 수 없다 |
| `Shown` | 3,000ms 이후 | 전부 드러난 채 탭을 기다린다 |
| `Dismissing` | `Shown`에서 탭 | 더미·툴팁이 함께 사라지는 중 |
| `Dismissed` | 종료 애니메이션 끝 | 빈 파르페만 남는다. 다시 진입하기 전까지 유지 |

### 등장 타임라인

| 요소 | Figma 노드 | 구간 | 동작 |
|---|---|---|---|
| 초기 대기 | — | 0 ~ 500ms | 빈 파르페만 보인다 |
| 왼쪽 상단 그룹 | `5417:6414` | 500 ~ 1,500ms | 아래로 이동하며 페이드 인 |
| 오른쪽 중단 그룹 | `5417:6428` | 1,000 ~ 2,000ms | 아래로 이동하며 페이드 인 |
| 왼쪽 하단 그룹 | `5417:6421` | 1,500 ~ 2,500ms | 아래로 이동하며 페이드 인 |
| 툴팁 | `5417:6432` | 2,500 ~ 3,000ms | 제자리에서 페이드 인 |

- 그룹: 최종 위치 기준 Y `-60dp → 0`과 불투명도 `0 → 1`을 1,000ms 동안 함께 적용한다.
  이징은 `CubicBezierEasing(0f, 0f, 0f, 1f)`.
- 툴팁: 위치는 고정, 불투명도만 `0 → 1`, 500ms. 원본 이징은 스프링(mass 1, stiffness 80,
  damping 20)인데, Figma는 이 스프링을 500ms 길이에 맞춘 샘플 곡선으로 내보낸다. 그 샘플
  곡선을 `Easing`으로 옮겨 `tween`에 쓴다. Compose `spring`을 그대로 쓰지 않는 이유는 같은
  계수로는 500ms 안에 끝나지 않아서다.

### 종료

- `Shown`에서 화면 안 아무 곳이나 한 번 누르면 더미 3개와 툴팁이 **동시에** 불투명도
  `1 → 0`으로 사라진다. 300ms, `CubicBezierEasing(0f, 0f, 0.58f, 1f)`.
- `Entering` 동안의 터치는 종료를 일으키지 않는다.
- 터치는 **소비하지 않는다.** 상단 바의 메뉴·그룹 추가 버튼은 어느 단계에서든 그대로
  동작하고, `Shown`에서 버튼을 누르면 종료와 버튼 동작이 함께 일어난다.
- 종료 판정은 터치 다운이다. 스크롤이나 당겨서 새로고침을 시작하는 터치도 종료로 친다.

### 백그라운드 복귀

- `Entering` 도중 앱이 백그라운드로 내려가면 `Shown`으로 건너뛴다. 돌아오면 전부 드러난
  상태이고 바로 탭해 닫을 수 있다.
- `Dismissing` 도중이면 `Dismissed`로 건너뛴다.
- `Shown`·`Dismissed`는 그대로 유지한다. 포그라운드 복귀만으로는 다시 재생하지 않는다.

### 더미 그룹

| 자리 | 그룹명 | 시간 | 칩 색 | 배치 변형 | 이미지 |
|---|---|---|---|---|---|
| 왼쪽 상단 | 예카수집가 | 1분전 | `YGGrouptagChipType.TYPE_5_6` | `YGToppingGroupType.TYPE_2_LEFT` | 말차 라떼 |
| 오른쪽 중단 | 파르페 | 2분전 | `YGGrouptagChipType.TYPE_1_2` | `YGToppingGroupType.TYPE_1_RIGHT` | 모자 |
| 왼쪽 하단 | 일상 | 3분전 | `YGGrouptagChipType.TYPE_3_4` | `YGToppingGroupType.TYPE_2_LEFT` | 카메라 |

- 누를 수 없다. 접근성 트리에도 올리지 않는다 — 실제 그룹이 아니어서 읽어 줄 대상이 아니다.
- 기존 `ToppingLayout`에 넣어 실제 그룹이 놓일 자리에 그대로 둔다.
- `Dismissed` 뒤에도 불투명도 0으로 **레이아웃에 남긴다.** 빼면 `GroupListParfaitLayout`이
  크림 개수를 줄이면서 파르페 높이가 움직인다.

## 표시·제어 규칙

### 툴팁

- Material `TooltipBox` 팝업을 걷어 내고, 상단 바 바로 아래에 겹쳐 그리는 일반 컴포저블로
  바꾼다. 팝업은 불투명도 애니메이션과 "화면 아무 곳 탭"을 함께 다루기 어렵다.
- 문구(두 줄, 가운데 정렬):
  - `새 그룹`을 만들거나 `그룹에 참여`하면
  - 내 그룹 목록을 `파르페`로 쌓을 수 있어요.
- 강조 구절(`새 그룹`, `그룹에 참여`, `파르페`)과 테두리는 `YGAtomicColors.Soda.Soda500`.
- 화살표는 그룹 추가 칩을 가리킨다.
- 노출은 인트로 단계가 정한다. `GroupListUiState.isTooltipVisible`은 없앤다.

### 상단 바

- G-001 상단 바는 날짜·요일 대신 **"내 그룹" + 개수**를 보여 준다. 0건이면 `내 그룹 0`.
- 개수를 모를 때(아직 한 번도 받지 못했거나, 에러 화면에서 목록이 없을 때)는 "내 그룹"만
  보여 준다.
- `GroupListUiState.dateString`·`dayOfWeekString`과 그 값을 채우던 로직은 없앤다.

## API / 인터페이스

```kotlin
// core:designsystem — 로컬 리소스로 그리는 토핑 이미지
sealed interface YGToppingImage {
    data class Local(@DrawableRes val drawableRes: Int) : YGToppingImage
}

// core:designsystem — 날짜·요일 대신 제목·개수
@Composable
fun YGTopBarEmpty(
    title: String,
    count: String?,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    windowInsets: WindowInsets = YGTopBarDefaults.windowInsets,
    rightContent: @Composable () -> Unit = {},
)

// feature:groups:list:impl — 컴포지션이 드는 인트로 상태
internal enum class GroupListEmptyIntroPhase {
    Entering, Shown, Dismissing, Dismissed;

    fun onTouchDown(): GroupListEmptyIntroPhase
    fun onStop(): GroupListEmptyIntroPhase
    fun enterValue(animated: Float): Float
    fun exitValue(animated: Float): Float
}

@Stable
internal class GroupListEmptyIntroState {
    val phase: GroupListEmptyIntroPhase
    fun dummyProgress(index: Int): Float   // 0f..1f, 위치·불투명도 공용
    val tooltipAlpha: Float
    val exitAlpha: Float                   // 더미·툴팁 공용 종료 배율

    suspend fun play()
    suspend fun dismiss()
    fun onTouchDown()
    fun onStop()
}

@Composable
internal fun rememberGroupListEmptyIntroState(enabled: Boolean): GroupListEmptyIntroState
```

- `YGToppingImage.Local` — `Template`은 디자인 시스템이 가진 기본 템플릿 열거형에 묶여
  있어, feature 모듈이 가진 이미지를 넘길 길이 없다.
- `YGTopBarEmpty.count` — `null`이면 개수 자리를 그리지 않는다.
- `rememberGroupListEmptyIntroState(enabled)` — `enabled`가 참이 되는 순간 한 번 `play()`를
  돌린다. 상태는 `remember`로 들어, 화면을 벗어났다 돌아오면 컴포지션과 함께 새로 만들어져
  다시 재생된다.
- 단계 전이(`onTouchDown`, `onStop`)는 애니메이션 값과 분리된 enum 멤버 함수로 두어 단위
  테스트한다.

## 파일 구성

| 파일 | 역할 |
|---|---|
| `feature/groups/list/impl/.../model/GroupListEmptyIntroPhase.kt` (신규) | 단계 열거형과 단계 전이·값 해석(순수 로직) |
| `feature/groups/list/impl/.../model/GroupListEmptyIntroTimeline.kt` (신규) | 타임라인 상수·이징 |
| `feature/groups/list/impl/.../model/GroupListEmptyIntroState.kt` (신규) | 애니메이션 값을 드는 상태 홀더 |
| `feature/groups/list/impl/.../model/GroupListEmptyDummyGroup.kt` (신규) | 더미 3개 정의 |
| `feature/groups/list/impl/.../route/component/GroupListEmptyDummyGroups.kt` (신규) | 더미 그리기 |
| `feature/groups/list/impl/.../route/GroupListScreen.kt` | 0건일 때 더미·툴팁 배치, 터치 다운 관찰, 라이프사이클 연결 |
| `feature/groups/list/impl/.../route/component/GroupListTooltip.kt` | 문구·색 교체 |
| `feature/groups/list/impl/.../route/component/GroupListTopBar.kt` | `TooltipBox` 제거, 제목·개수 전달 |
| `feature/groups/list/impl/.../route/GroupListViewModel.kt` | 날짜·툴팁 상태 제거 |
| `feature/groups/list/impl/.../route/GroupListErrorScreen.kt` | 상단 바 인자 교체 |
| `feature/groups/list/impl/src/main/res/drawable*/` | 더미 이미지 PNG 3장 |
| `feature/groups/list/impl/src/main/res/values/strings.xml` | 더미 그룹명, "내 그룹". 툴팁 문구는 기존처럼 `GroupListTooltip` 안에 둔다 |
| `core/designsystem/.../ygtoppinggroup/YGToppingImage.kt`, `YGToppingGroup.kt` | `Local` 케이스 |
| `core/designsystem/.../ygtopbar/YGTopBar.kt` | `YGTopBarEmpty` 시그니처 |
| `app-preview/.../YGTopBarPreviewScreen.kt` | 바뀐 시그니처 반영 |

더미 이미지는 Figma MCP로 각 `Topping-Group`의 이미지 원본을 받아, 프레임(96dp 정사각)
안의 배치대로 잘라 PNG로 넣는다. 회전은 `YGToppingGroupType`이 맡으므로 이미지에 굽지 않는다.

### 검증

- 단위 테스트: 단계 전이 — `Entering`에서 터치 다운 무시, `Shown`에서 터치 다운 시
  `Dismissing`, `onStop`의 단계별 결과.
- `GroupListViewModelTest`: 날짜·툴팁 상태를 보던 케이스 정리.
- 프리뷰: 0건 화면의 `Shown`·`Dismissed`.
- 에뮬레이터: 등장 순서·간격, 3초 이전 탭 무시, 탭 종료, 버튼 동작, 재진입 재생, 등장 도중
  백그라운드 복귀.

## 주의 / 열린 질문

- **0건 파르페 높이가 바뀐다.** 더미가 레이아웃에 남으므로 0건일 때도 토핑 3개 분량의
  크림이 쌓인다. Figma 프레임과는 맞지만 기존 0건 화면보다 파르페가 길다.
- **오른쪽 그룹의 세로 위치가 Figma와 다르다.** Figma는 왼쪽 첫 그룹보다 68 아래,
  `ToppingLayout`은 `alternateOffsetY` 기본값만큼 아래다. 실제 그룹이 놓일 자리와 맞추는
  쪽을 택했다.
- **기존 위키 정책과 시점이 달라진다.** `G-001-Empty` 툴팁 정책은 "0건이면 항상 뜬다"인데,
  이 스펙에서는 등장 타임라인 끝에 뜨고 탭하면 재진입 전까지 안 뜬다. 위키 반영은 정책
  원본을 `wiki/raw`에 넣는 별도 작업이다.
- **앱 첫 실행에서는 초기 대기 500ms 동안 크림이 자란다.** 목록을 받기 전에는 더미 없이
  측정되고, 0건이 확정되는 순간 더미 자리가 생기면서 `GroupListParfaitLayout`이 크림을
  늘린다. 다른 화면에서 돌아올 때는 처음부터 더미가 있어 자라는 모션이 없다.
- **0건에서 당겨서 새로고침이 실패하면 다시 재생된다.** 에러 화면으로 바뀌며 인트로 상태가
  버려지기 때문이다. 재시도가 성공해 목록 화면으로 돌아오면 처음부터 돈다.
- **화면 회전 등 구성 변경에서는 다시 재생된다.** 상태를 `remember`로만 들기 때문이다.
- **백그라운드 복귀 동작은 정책에 없다.** "완료 상태로 표시"는 구현 쪽에서 정한 값이다.
