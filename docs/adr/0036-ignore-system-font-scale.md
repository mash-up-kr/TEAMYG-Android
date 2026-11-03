---
id: ADR-0036
title: 앱 텍스트는 시스템 글자 크기를 무시하고 피그마 수치로 그린다
status: proposed
date: 2026-11-03
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec: fixed-font-scale-text
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

위키 기획에는 글자 크기·접근성 정책이 없다. 이 결정이 처음 정하는 것이다.

## 결정

앱의 모든 Compose 텍스트는 `fontScale`과 무관하게 피그마 수치(dp)로 그린다. 상한(clamp)도 두지 않는다.

- 정책은 `core:util:android`의 `FixedFontScale.kt#toFixedFontScale` 한 곳에만 있다. `sp` 값을 dp로 보고 `Dp.toSp()`로 되돌린다. Android 14 비선형 스케일링도 같은 변환기로 거꾸로 적용된다.
- 텍스트는 `core:designsystem`의 래퍼 `YGText`·`YGBasicText`·`YGBasicTextField`로 그린다. 래퍼가 그 자리의 `LocalDensity`로 변환한다.
- 컴포지션 밖 측정(`TextMeasurer`, `android.graphics.Paint`)은 호출부가 직접 변환한다.
- 캔버스의 `LocalDensity` 덮어쓰기는 걷어내고 같은 방식으로 통일한다.
- 약관 웹뷰는 `textZoom = 100`으로 같은 정책을 따른다.
- 강제 수단(린트·CI)은 두지 않는다. [code-conventions](../code-conventions.md#compose-텍스트) 규약과 리뷰로 막는다.

## 대안

- **테마에서 `LocalDensity`를 `fontScale = 1`로 덮어쓰기** — 한 줄로 끝난다. 그러나 `Dialog`·`Popup`처럼
  새 window를 만드는 곳에서 시스템 density로 돌아갈 수 있고, 특정 화면만 OS 설정을 따르게 뺄 방법이 없다.
  **→ 기각:** 적용 범위를 보장할 수 없고 선택 해제가 안 된다.
- **타이포 토큰마다 `dp.toSp()`** — 토큰을 쓰는 곳은 자동으로 고정된다. 그러나 `object` 상수를
  컴포저블 접근자로 바꿔야 하고, 토큰 밖에서 만든 스타일은 빠진다.
  **→ 기각:** 호출부 전반이 바뀌는데 범위는 오히려 좁다.
- **상한만 두기**(예: 1.3배까지만 키움) — 접근성을 일부 남긴다. 그러나 레이아웃이 피그마와 어긋나는
  문제가 그 범위 안에서는 그대로 남는다.
  **→ 기각:** 지금 요구는 "항상 피그마 크기"다. 필요해지면 `toFixedFontScale` 한 곳만 바꾼다.

## 영향

**긍정**

- 어느 기기 설정에서든 화면이 피그마와 같다. 고정 폭 안의 잘림·줄바꿈 지점도 기기마다 같다.
- 정책을 끄거나 상한을 두는 변경이 함수 하나로 끝난다.

**트레이드오프**

- 시스템 글자 크기를 키운 사용자도 앱 글자가 커지지 않는다. 접근성 대가는 확정이다.
- 변환이 멱등이 아니라 래퍼에 변환한 스타일을 넘기면 글자가 두 번 줄어든다.
- 원본 `Text`를 쓰는 회귀를 컴파일러가 막지 않는다.

**위험·방어**

- 변환 불변식은 `FixedFontScaleTest`(JVM, 비선형 구간 포함)가 고정한다.
- 래퍼의 실제 렌더 크기·줄 간격은 `YGTextTest`·`YGBasicTextFieldTest`(계측)가 본다.
- 캔버스 측정과 렌더링의 일치는 `TextToppingAutoFitTest`가 `fontScale` 1.0과 1.15의 결과를 비교해 지킨다.
- `ParagraphStyle`의 lineHeight·`textIndent`·`inlineContent` 크기는 변환하지 않는다. 각 함수 KDoc에 적어 두었다.
