---
id: app-preview-component-gallery
title: 컴포넌트 프리뷰 갤러리 앱 (app-preview Component Gallery)
status: implemented
category: ui-spec
platforms: android
verified: 2026-07-22
related_code:
  - MainScreen.kt
  - MainEntryBuilder.kt
  - MainEntryModule.kt
  - NavKeyMain.kt#NavKeyMain
  - RootRoute.kt#RootRoute
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, app-preview, designsystem]
---

# Spec: 컴포넌트 프리뷰 갤러리 앱 (app-preview)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.
> ✅ **develop 머지 완료**(PR #163 `feature/design-system-preview-app-component`, 2026-07-22). 카탈로그(5카테고리)·NavKey 17·showcase 화면 17·`PreviewSection`·`ComponentEntryModule`(@IntoSet) 배선 모두 코드 반영. 구조=설계 일치.

## 목표

`:app-preview` 애플리케이션 모듈을, 실기기에서 `:core:designsystem`의 `YG*` 컴포넌트를 직접
눈으로 확인·조작하는 **컴포넌트 갤러리**로 확장한다. 메인 화면에 컴포넌트명 버튼을 카테고리로
묶어 나열하고, 버튼을 탭하면 해당 컴포넌트의 모든 변형·상태를 실렌더로 보여주는 프리뷰 화면으로
이동한다. Android Studio `@YGPreview`(정적, 모듈 내부)의 한계(실기기 미실행·외부 재사용 불가)를
보완한다.

## 범위

- **포함**: `:core:designsystem`의 재사용 `YG*` 컴포넌트 프리뷰 화면. 카테고리 그룹 메인 목록.
  컴포넌트별 NavKey·EntryBuilder·showcase 화면. 카탈로그 데이터 모델. 공용 showcase 헬퍼.
- **제외**:
  - feature 모듈 로컬 컴포넌트(ProfileCard, ShutterButton 등) — app-preview에 feature 의존 추가
    안 함(현 의존: `core:designsystem`/`navigation`/`ui`/`util`만).
  - `YGTextFieldImpl`(internal 공용 구현체) — 단독 화면 없음.
  - designsystem의 기존 `@YGPreview` 함수 재사용 — `private`/모듈 내부라 app-preview에서 호출 불가.
    showcase 화면은 신규 작성한다. 단, **각 showcase 화면·MainScreen·PreviewSection에는 자체
    `@YGPreview`+`PreviewBox` 프리뷰 함수를 신규로 붙인다**(Android Studio 정적 프리뷰로도 확인 가능).
  - 유닛/스크린샷 테스트 — app-preview는 실기기 육안 확인용 샌드박스라 자동 테스트 대상 아님.

## 컴포넌트 카탈로그 (5 카테고리 → 17 화면)

| 카테고리 | 컴포넌트(=화면) |
|---|---|
| Button | YGButton, YGChipButton, YGToggleButton, YGIconButton, YGDateButton, YGInputNumber |
| Input | YGTextField, YGTextFormField |
| Text | YGLabel, YGDate, YGActionItem |
| Container | YGModalPopup, YGInviteCard, YGDangerZone, YGListItem, YGHorizontalDivider |
| Bar | YGTopBar (Back/Detail/Empty/Default 4변형을 1화면에 묶음) |

- 총 17개 프리뷰 화면(YGTopBar 4변형은 한 화면에서 순차 렌더).

## API / 인터페이스

### 카탈로그 데이터 모델 (`model/ComponentCatalog.kt`)
```kotlin
enum class ComponentCategory(val label: String) {
    BUTTON("Button"), INPUT("Input"), TEXT("Text"), CONTAINER("Container"), BAR("Bar"),
}

data class ComponentEntry(
    val category: ComponentCategory,
    val label: String,      // 버튼에 표시할 컴포넌트명 (예: "YGButton")
    val navKey: NavKey,      // 탭 시 이동할 NavKey
)

val componentCatalog: List<ComponentEntry>   // 위 표 순서대로 나열
```

### NavKey (`navigation/key/NavKeyYG*.kt` — 파일당 1개, 17개)
```kotlin
@Serializable
data object NavKeyYGButton : NavKey
// NavKeyYGChipButton, ... NavKeyYGTopBar (카탈로그 항목별 1개씩, 각자 파일)
```
- 컴포넌트별 개별 NavKey → 백스택·시스템 뒤로가기·복원 정상 동작. 기존 `NavKeyMain.kt`와 동일하게
  **NavKey당 파일 1개**로 둔다(`NavKeyYGButton.kt` … `NavKeyYGTopBar.kt`).

### showcase 헬퍼 (`screen/component/PreviewSection.kt`)
```kotlin
@Composable
fun PreviewSection(title: String, content: @Composable () -> Unit)
```
- `title`: 변형 소제목(`YGLabel`로 렌더). `content`: 해당 변형 실렌더. 일관 여백 래퍼.

### 컴포넌트 프리뷰 화면 시그니처 (예)
```kotlin
@Composable
fun YGButtonPreviewScreen(onBack: () -> Unit)
```
- 모든 프리뷰 화면 공통: `onBack` 콜백 1개. 상단 `YGTopBarBack(onBackClick = onBack)`.

## 동작 / 상태

- **메인 화면**: `componentCatalog`를 `category`로 groupBy → 카테고리 헤더(`YGLabel`) + 각 항목
  `YGButton(Medium.Secondary, text = label)`. 버튼 탭 → `navigator.navigate(entry.navKey)`.
- **프리뷰 화면**: `LazyColumn`으로 변형 전부 세로 나열. 각 변형은 `PreviewSection(title=변형명){ 실렌더 }`.
  - onClick류 콜백은 no-op 람다.
  - 상태 보유 컴포넌트(YGToggleButton `isSelected`, YGTextField/YGTextFormField 텍스트,
    YGInputNumber 선택, YGModalPopup 표시 여부)는 `remember` 로컬 상태로 실제 인터랙션 가능하게 한다.
  - YGButton: buttonType 전종(SmallSquare / Medium.Primary·Secondary·Transparency / Large) ×
    enabled·disabled × 아이콘 유무 일부.
- **뒤로가기**: `navigator.back()` (Navigation3 백스택). 시스템 백버튼도 `NavDisplay` 기본으로 동일.

## 표시·제어 규칙

- 메인 목록은 데이터 주도: 컴포넌트 추가 = `componentCatalog`에 1줄 + NavKey/Entry/Screen 추가.
- 카테고리 순서·컴포넌트 순서는 카탈로그 선언 순서를 따른다.
- 각 프리뷰 화면은 스크롤 가능(`LazyColumn`) — 변형이 화면을 넘겨도 전부 접근 가능.

## 파일 구성

신규/개조 (`app-preview/src/main/kotlin/com/teamyg/parfait/preview/` 이하):

| 파일 | 신규/개조 | 역할 |
|---|---|---|
| `model/ComponentCatalog.kt` | 신규 | 카테고리 enum + ComponentEntry + `componentCatalog` 목록 |
| `navigation/key/NavKeyYG*.kt` (17개) | 신규 | 컴포넌트별 `@Serializable data object` NavKey, 파일당 1개 |
| `navigation/entry/ComponentEntryBuilders.kt` | 신규 | 컴포넌트별 `entry<NavKeyXxx>{ Scaffold { XxxPreviewScreen(onBack) } }` |
| `navigation/di/ComponentEntryModule.kt` | 신규 | `@Module @InstallIn(ActivityRetainedComponent)` + `@IntoSet @Provides` 등록 |
| `screen/MainScreen.kt` | 개조 | 플레이스홀더 → 카테고리 그룹 버튼 목록(navigator 이동) |
| `navigation/entry/MainEntryBuilder.kt` | 개조 | MainScreen에 navigator 이동 로직 전달 |
| `screen/component/PreviewSection.kt` | 신규 | 변형 소제목 + 여백 공용 래퍼 |
| `screen/component/*PreviewScreen.kt` | 신규 | 컴포넌트별 showcase 화면 17개 |

- 배선: `ComponentEntryModule`의 `@IntoSet @Provides`가 `RootRoute`의 entryBuilder set에 합류 →
  기존 `NavDisplay`가 자동 렌더(기존 Main 패턴 동일).

## 주의 / 열린 질문

- 컴포넌트별 변형 목록의 "완전성"은 각 컴포넌트 스펙(`parfait/specs/archive/`의 개별 컴포넌트 스펙)을
  근거로 채운다. 신규 변형 누락 가능 → 구현 시 각 컴포넌트 파라미터 실제 확인.
- 검증 = 컴파일 성공 + 실기기 빌드/설치 후 육안 확인. 카탈로그↔NavKey↔Entry 매핑 누락은 컴파일
  타임에 최대한 드러나게 한다(등록 누락 시 화면 미도달로만 드러나므로 구현 시 대조 체크리스트 유지).
