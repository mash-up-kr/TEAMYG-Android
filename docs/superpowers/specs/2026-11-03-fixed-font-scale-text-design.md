---
id: fixed-font-scale-text
title: 시스템 글자 크기와 무관한 텍스트 (Fixed font scale text)
status: in-progress
category: ui-spec
platforms: android
verified: 2026-11-03
related_code:
  - FixedFontScale.kt#toFixedFontScale
  - YGTypographyDefaults.kt#YGTypographyDefaults
  - YGTextFieldImpl.kt#YGTextFieldImpl
  - TextToppingContent.kt#TextToppingContent
  - CanvasTextInputScreen.kt#CanvasTextInputScreen
  - TextToppingLayoutModifiers.kt#withTextToppingSizing
  - YGToast.kt#YGToast
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, designsystem, typography]
---

# Spec: 시스템 글자 크기와 무관한 텍스트

## 목표

OS 글자 크기 설정(`fontScale`)과 관계없이 앱의 모든 텍스트가 **피그마 수치 그대로** 그려지게 한다.

실기기(`font_scale` = `1.15`)에서 그룹 상세 화면을 피그마와 나란히 놓으면 레이아웃(dp)은 같은데
글자만 크다. `YGTypographyDefaults`의 `fontSize`가 `sp`라서 `14.sp`가 16.1dp로 그려지기 때문이다.

정책을 나중에 바꾸게 될 수 있다(상한 도입, 해제 등). 그래서 변환을 **한 곳**에 모아, 그때는
그 한 곳만 고치면 되게 만드는 것이 이 설계의 핵심 목표다.

## 범위

- 포함
  - 공유 변환 함수 하나 — `fontScale`을 무시하고 피그마 수치를 dp로 해석해 `Dp.toSp()`로 바꾼다
  - 래퍼 컴포저블 — 원본 오버로드마다 하나씩
    - `YGText`: `material3.Text`의 String·AnnotatedString 오버로드
    - `YGBasicText`: `foundation.text.BasicText`의 AnnotatedString 오버로드(현재 쓰이는 것만)
    - `YGBasicTextField`: `BasicTextField`의 String·TextFieldValue·TextFieldState 오버로드
  - 저장소 전체의 `material3.Text` / `BasicText` / `BasicTextField` 호출을 래퍼로 교체(약 60개 파일)
  - 캔버스 텍스트 토핑의 기존 `LocalDensity` 덮어쓰기(`fontScale = 1f`)를 걷어내고 새 방식으로 통일
  - `docs/code-conventions.md`에 "텍스트는 기본으로 YG 래퍼를 쓴다" 규칙 추가
- 제외
  - 금지 규칙의 자동 강제(ktlint/Android Lint 커스텀 룰, CI 스크립트) — 문서 규약으로만 둔다(확정)
  - `fontScale` 상한(clamp) — "항상 피그마 크기"로 확정. 필요해지면 변환 함수만 바꾼다
  - 앱 전역 `LocalDensity` 덮어쓰기(테마 레벨) — 아래 접근 비교에서 기각
  - View 기반 텍스트(`Toast`, WebView 내용, 시스템 UI)
  - 텍스트가 아닌 dp 레이아웃 값

## 접근 비교

| 접근 | 내용 | 판단 |
|---|---|---|
| A. 테마에서 `LocalDensity` 덮어쓰기 | `YGCustomTheme`의 `CompositionLocalProvider`에서 `fontScale = 1f` | 기각. 한 줄이지만 `Dialog`/`Popup`처럼 새 window를 만드는 곳에서 시스템 density로 돌아갈 수 있고, 선택적으로 빼는 방법이 없다 |
| B. 타이포 토큰마다 `dp.toSp()` | `TextStyle` 상수를 컴포저블 접근자로 바꿔 변환 | 기각. `object` 상수를 유지할 수 없고 호출부 전반이 바뀐다 |
| **C. 래퍼 컴포저블 + 공유 변환 함수** | 텍스트를 그리는 지점에서 그 자리의 density로 변환 | **채택**(사용자 선택) |

C를 고른 이유:

- **되돌리기와 정책 변경이 한 곳.** 변환 함수가 입력을 그대로 돌려주게 바꾸면 정책이 꺼진다. 호출부는 건드리지 않는다.
- **선택 적용.** OS 설정을 따라야 하는 곳은 원본을 쓰면 된다.
- **window에 무관.** 변환이 그 자리의 `LocalDensity`로 계산되므로 `Dialog`/`Popup` 안에서도 결과가 같다.
- BOM `2026.08.00`의 `Dp.toSp()`는 Android 14 비선형 폰트 스케일링 변환기까지 거꾸로 적용하므로, 변환 결과가 정확히 피그마 dp가 된다.

## API / 인터페이스

### 공유 변환 함수

```kotlin
// 비컴포저블 — TextMeasurer 등 컴포지션 밖에서도 쓴다
fun TextStyle.toFixedFontScale(density: Density): TextStyle
fun AnnotatedString.toFixedFontScale(density: Density): AnnotatedString
fun TextUnit.toFixedFontScale(density: Density): TextUnit

// 컴포저블 편의 함수 — LocalDensity.current를 쓴다
@Composable fun TextStyle.toFixedFontScale(): TextStyle
```

- 이름·시그니처 세부는 빌더 재량. 단 **정책은 이 함수들 안에만** 있어야 한다.
- 변환 대상: `sp` 단위인 `fontSize`, `lineHeight`, `letterSpacing`
  - `em`이나 `Unspecified`면 그대로 둔다. `em`은 fontSize 기준 상대값이라 fontSize만 바꾸면 따라온다.
- 변환식: `sp` 값을 피그마 dp로 보고 `with(density) { value.dp.toSp() }`
  - 예외는 `lineHeight`다. 렌더러(`resolveLineHeightInPx`)가 `fontScale` > 1.05에서 sp lineHeight를 글자 크기 대비 비율로 그린다. 그래서 `fontSize`·`lineHeight`가 둘 다 sp이면 비율을 유지해 `lineHeight × 변환된 fontSize / fontSize`로 바꾼다.
  - sp `fontSize`를 모르는 lineHeight(`fontSize`가 `Unspecified`인 `TextStyle`, `AnnotatedString` 안의 `ParagraphStyle`)는 위 절대값 변환으로 떨어진다. 이 경우 비선형 구간에서 줄 간격이 어긋날 수 있다.
- `AnnotatedString`은 `SpanStyle`의 `fontSize`/`letterSpacing`도 변환한다. `ParagraphStyle`의 `lineHeight`도 마찬가지다.
  - 근거: `YGToast`가 `TextStyle.toSpanStyle()`로 sp `fontSize`를 span에 싣는다. `core/util/android`의 `BuildAnnotatedString.kt`도 같다.
- 모듈: feature에서도 써야 하므로 public이다. `core:designsystem`의 `component/ygtext/` 패키지에 둔다(기존 `YGDate`·`YGLabel`과 같은 곳).

### 래퍼 컴포저블

- 원본 오버로드 하나당 래퍼 하나. **파라미터는 원본과 같은 이름·순서·기본값으로 전부 미러링**한다.
  - 그래야 호출부 교체가 이름 바꾸기만으로 끝나고, 되돌릴 때도 그렇다.
- 래퍼 안에서 `style`/`textStyle`을 변환한 뒤 원본을 호출한다. `YGText`는 직접 받는 `fontSize`·`lineHeight`·`letterSpacing`을 먼저 `style`에 합친 뒤 한 번만 변환한다(lineHeight 비율 변환에 fontSize가 필요하다).
  - 직접 받는 파라미터 예: `CameraZoomIndicatorComponent`의 `fontSize = 13.sp`, `ZoomLevelRow`의 `14.sp`/`13.sp`.
- `YGText(AnnotatedString)`과 `YGBasicText`는 `text`의 span도 변환한다.
- `BasicText`/`BasicTextField`의 `autoSize`(`TextAutoSize`)는 현재 쓰는 곳이 없다. 파라미터는 미러링하되 변환하지 않는다는 점을 KDoc 한 줄로 남긴다.

## 동작 / 상태

- 결과 불변식: `fontScale`이 얼마든 래퍼로 그린 텍스트의 픽셀 크기는 `fontScale = 1.0`일 때 원본으로 그린 것과 같다.
- `fontScale = 1.0` 기기에서는 교체 전후 화면이 바뀌지 않는다.

⚠️ **변환은 멱등이 아니다.** `fontScale` 1.15에서 14sp는 약 12.2sp가 되고, 한 번 더 변환하면 약 10.6sp가 된다.
- 규칙: **래퍼에는 원본(피그마 수치) 스타일을 넘기고, 변환은 래퍼 안에서만 한다.**
- 컴포지션 밖의 측정·그리기(`TextMeasurer`, `android.graphics.Paint`)에만 호출부가 직접 변환한 스타일을 쓴다.
- 같은 스타일 변수를 측정과 렌더링에 함께 쓰는 곳은 변환 전 값과 변환 후 값을 별도 변수로 나눈다.
  - 대표 위험 지점: `CanvasTextInputScreen`의 `hugRenderStyle`과 `withTextToppingSizing` 체인

### 래퍼 밖 density 소비자 (캔버스 외)

- `YGNametagChip` — `android.graphics.Paint.textSize`를 `textStyle.fontSize.toPx()`로 잡아 글리프 잉크 중심을 재고 `Text`를 그만큼 옮긴다.
  - `Text`가 `YGText`가 되면 Paint에도 `textStyle.toFixedFontScale(density)`를 써야 한다. 그러지 않으면 `fontScale` ≠ 1.0일 때 세로 오프셋이 어긋난다.
- 교체 중 같은 패턴(`fontSize.toPx()`, `TextMeasurer`, `drawText`)이 더 나오면 같은 규칙을 적용한다.

## 캔버스 통일

현재 `TextToppingContent`와 `CanvasTextInputScreen`이 각자 `LocalDensity`를 `fontScale = 1f`로 덮어써서
"모든 사용자에게 같은 크기·줄바꿈"을 보장한다. 이 덮어쓰기를 걷어내고 래퍼와 변환 함수로 바꾼다.

⚠️ 덮어쓰기는 텍스트 렌더링만이 아니라 **그 안의 모든 density 소비자**에 걸려 있다. 걷어낼 때 같이 바꿔야 하는 것:

- `TextMeasurer` 측정 — `TextToppingLayoutModifiers`의 크기 탐색, `CanvasTextInputScreen`의 `rememberTextMeasurer`
  - 측정에 넘기는 스타일에 비컴포저블 `toFixedFontScale(density)`를 적용해야 렌더링과 측정이 어긋나지 않는다.
- 덮어쓴 density로 sp↔dp/px를 환산하는 코드
  - 예: `CanvasTextInputScreen`의 `LocalDensity.current` 사용처
  - `TextToppingBackground`는 `Float` `fontSizeSp`를 곧바로 dp로 바꾸므로 동작은 그대로다. 덮어쓰기를 전제로 한 KDoc만 고친다.
- 타임랩스 `CanvasVideoRecordLayer`는 `Density(density = ...)`로 `fontScale` 기본값 1f를 쓴다. 그 안에서 변환은 아무 일도 하지 않으므로 손대지 않는다.

완료 기준: `font_scale` `1.15` 기기에서 텍스트 토핑의 크기·줄바꿈·배경 박스·저장 이미지·타임랩스가 교체 전과 같아야 한다.
- 텍스트 토핑은 다른 사용자의 기기에도 그대로 보이고 서버 `fontSize`로 저장되므로, 여기서 어긋나면 사용자 간 결과가 달라진다.

## 문서 규약

`docs/code-conventions.md`에 Compose 텍스트 절을 추가한다.

- 텍스트는 `YGText` / `YGBasicText` / `YGBasicTextField`를 기본으로 쓴다.
- 원본(`material3.Text` 등)은 OS 글자 크기를 **의도적으로** 따라야 할 때만 쓰고, 그 자리에 이유를 주석 한 줄로 남긴다.
- 컴포지션 밖에서 텍스트를 측정·그릴 때(`TextMeasurer`, `drawText`)는 스타일에 `toFixedFontScale(density)`를 적용한다.

자동 강제는 하지 않는다. 리뷰에서 이 문서를 근거로 잡는다.

## 파일 구성

- `core/designsystem/.../component/ygtext/` — 변환 함수 파일 1개, `YGText`·`YGBasicText`·`YGBasicTextField` 파일
- 교체 대상 — `import androidx.compose.material3.Text`, `foundation.text.BasicText`, `foundation.text.BasicTextField`를 쓰는 모든 파일
  - `app-preview`, `core:ui`, `core:designsystem`, `feature:*`
  - 디자인시스템 컴포넌트 내부(`YGButton`, `YGTextFieldImpl` 등)도 포함한다
- 캔버스 — `TextToppingContent`, `CanvasTextInputScreen`, `TextToppingLayoutModifiers`, `TextToppingBackground`
- `docs/code-conventions.md`

## 빌더 재량

- 변환 함수·파일의 정확한 이름과 분할
- 래퍼 KDoc 문구. [`code-conventions.md`](../../code-conventions.md) 규약을 따른다
- 교체 작업의 커밋·태스크 분할 순서
- 변환 함수의 JVM 유닛 테스트 범위
  - 최소: sp/em/Unspecified 각각, span 변환, `fontScale` 1.0·1.15·2.0(비선형 구간)

## 주의 / 열린 질문

- **접근성 트레이드오프(확정)**: 시스템 글자 크기를 키운 사용자도 앱 글자가 커지지 않는다. 사용자가 이를 알고 "항상 피그마 크기"를 골랐다.
- 강제 수단이 없으므로 새 코드가 원본 `Text`를 쓰는 회귀는 리뷰에 기댄다.
