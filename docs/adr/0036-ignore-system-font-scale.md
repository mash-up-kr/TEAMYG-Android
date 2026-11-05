---
id: ADR-0036
title: 앱 텍스트는 시스템 글자 크기를 무시하고 피그마 수치로 그린다
status: proposed
date: 2026-11-05
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec:
related_architecture: design-system.md
platforms: android
tags: [adr, parfait, designsystem, typography, accessibility]
---

# ADR-0036: 앱 텍스트는 시스템 글자 크기를 무시하고 피그마 수치로 그린다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

타이포 토큰(`YGTypographyDefaults`)은 피그마 수치를 `sp`로 옮겨 적었다. `sp`는 OS 글자 크기
설정(`fontScale`)을 곱하므로, 설정을 키운 기기에서는 레이아웃(dp)은 피그마와 같은데 글자만 커진다.
라벨이 입력칸을 밀어내고 고정 폭 칩의 잘림 지점이 기기마다 달라진다.

캔버스 텍스트 토핑은 이미 화면 단위로 `LocalDensity`를 `fontScale = 1`로 덮어써 막고 있었다.
토핑은 다른 사용자 기기에도 같은 모습으로 보이고 서버에 글자 크기로 저장되기 때문이다.
나머지 화면에는 대응이 없었다.

위키 기획에는 글자 크기·접근성 정책이 없다. 이 결정이 처음 정한다.

## 결정

앱의 모든 Compose 텍스트는 `fontScale`과 무관하게 피그마 수치(dp)로 그린다. 상한(clamp)도 두지 않는다.

- `core:designsystem` `theme/FixedFontScale.kt#ProvideFixedFontScale`이 `LocalDensity`를
  `Density(density, fontScale = 1f)`로 덮어쓴다. `YGCustomTheme`이 content 전체를 이것으로 감싸므로
  `:app`·`:app-preview`·테마를 쓰는 테스트가 모두 같은 값을 받는다.
- `LocalDensity`를 읽는 컴포지션 안 측정(`rememberTextMeasurer`, `with(density) { sp.toPx() }`로 만든
  `android.graphics.Paint`)도 같이 고정된다. 호출부 변환이 따로 없다.
- ⚠️ `Dialog`·`Popup`은 새 창의 `ComposeView`가 기기 설정으로 `LocalDensity`를 다시 내려, 테마 값이 끊긴다.
  그 content를 `ProvideFixedFontScale`로 한 번 더 감싼다. 지금 대상은 `YGModalPopup`의 `Dialog`와
  `CustomCalendar`의 `Popup`이다. `ModalBottomSheet`·`DropdownMenu`·`TooltipBox`처럼 내부에서 창을 여는
  material 컴포넌트도 같은 처지다.
- 캔버스(`TextToppingContent`, `CanvasTextInputScreen`)의 화면 단위 덮어쓰기는 **남긴다.** 테마 쪽 고정을
  나중에 풀어도 토핑은 고정돼야 하기 때문이다.
- 약관 웹뷰(`NotionWebView`)는 `textZoom = 100`으로 같은 정책을 따른다.
- 강제 수단(린트·CI)은 두지 않는다. [code-conventions](../code-conventions.md#compose-텍스트-글자-크기)
  규약과 리뷰로 막는다.

## 대안

- **텍스트 래퍼(`YGText` 등)가 스타일의 sp를 직접 변환** — `Dialog`·`Popup` 안에서도 래퍼만 쓰면 새 창
  여부와 상관없이 고정된다. 그러나 텍스트를 그리는 원본 컴포저블마다 래퍼를 만들어야 하고, 처음 쓰는
  공통·material 컴포넌트가 원본 `Text`를 쓰는지 매번 확인해야 하는 부담은 창 쪽 확인과 크게 다르지 않다.
  나중에 접근성을 위해 고정을 풀 때는 캔버스처럼 꼭 고정해야 하는 영역만 남기게 되는데, 그 선택은
  `LocalDensity` 범위로 하는 편이 단순하다. 변환이 멱등이 아니라 두 번 줄어드는 함정도 생긴다.
  **→ 기각:** 같은 확인 부담에 비해 코드와 함정이 많고, 범위 선택이 덜 유연하다.
- **Activity `Configuration.fontScale`을 1로 덮어쓰기**(`attachBaseContext`) — 새 창까지 한 번에 따른다.
  그러나 화면·영역 단위로 풀 수 없고 Compose 밖 리소스 해석까지 바뀐다.
  **→ 기각:** 범위를 고를 수 없다.
- **상한만 두기**(예: 1.3배까지만 키움) — 접근성을 일부 남긴다. 그러나 레이아웃이 피그마와 어긋나는
  문제가 그 범위 안에서는 그대로 남는다.
  **→ 기각:** 지금 요구는 "항상 피그마 크기"다. 필요해지면 `ProvideFixedFontScale` 한 곳만 바꾼다.

## 영향

**긍정**

- 어느 기기 설정에서든 화면이 피그마와 같다. 고정 폭 안의 잘림·줄바꿈 지점도 기기마다 같다.
- 텍스트 호출부는 원본 `Text`·`BasicText`·`BasicTextField`를 그대로 쓴다.
- 고정을 풀 때 `YGCustomTheme`에서 한 줄을 빼면 되고, 캔버스 고정은 그대로 남는다.

**트레이드오프**

- 시스템 글자 크기를 키운 사용자도 앱 글자가 커지지 않는다. 접근성 대가는 확정이다.
- 새 창을 여는 컴포넌트를 쓸 때 content를 감쌌는지 사람이 확인해야 한다. 컴파일러가 막지 않는다.

**위험·방어**

- `FixedFontScaleTest`(계측)가 테마의 `fontScale = 1`과, `Dialog`가 기기 값으로 돌아가며 다시 감싸야
  1이 된다는 전제를 고정한다. 이 전제가 Compose 버전업으로 바뀌면 이 테스트가 먼저 알려 준다.
