---
id: text-topping-script-span
title: 텍스트 토핑 글꼴·자간 구간 분기 (Per-run script SpanStyle)
status: draft
category: behavior-spec
platforms: android
verified: 2026-10-27
related_code:
  - TextToppingScript.kt#TextToppingScript
  - TextToppingScriptRuns.kt#textToppingScriptRuns
  - TextToppingHanCoverage.kt#TextToppingHanCoverage
  - TextToppingScriptSpans.kt#textToppingAnnotatedString
  - TextToppingContent.kt#textToppingBaseStyle
  - TextToppingContent.kt#TextToppingContent
  - TextToppingLayoutModifiers.kt#fitTextToppingToHeight
  - CanvasTextInputScreen.kt
related_adr:
related_spec: text-topping
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 텍스트 토핑 글꼴·자간 구간 분기

## 목표

텍스트 토핑의 글꼴과 자간을 **텍스트 전체가 아니라 글자 구간마다** 고른다. 기획 스펙 「텍스트 스타일」은
언어별 글꼴을 정해 두었다 — 한글/영어/숫자 SUIT·자간 -1.1%, 일본어 Line Seed JP·-1.1%, 중국어 Line Seed
TW·0%(괄호의 -0.264px 는 오기, 0% 가 맞다). 그런데 지금 구현은 `detectTextToppingScript`가 내용의 주된
스크립트 하나로 통째 고른다. 그래서 가나가 하나라도 섞이면 한글까지 Line Seed JP 로 그려지고(한글 글리프가
없어 시스템 폰트로 빠진다), 일·중이 섞이면 한 글꼴로만 그려진다.

## 범위

- 포함: 구간 분할 규칙, 한자 커버리지 테이블과 생성 스크립트, 입력 필드·캔버스 렌더·크기 맞춤 측정의 적용
- 제외
  - iOS. 이번 변경은 Android 만 다룬다
  - 두 폰트 어디에도 없는 한자(간체 `们这难忆` 등)의 대체. 시스템 폰트 폴백을 그대로 둔다
  - 폰트 서브셋·APK 크기
- 안티골: 편집 중(C-106 입력)과 배치 후(캔버스 렌더)의 줄바꿈·크기가 달라지는 것. 세 경로가 같은 구간 결과를
  써야 한다

## Approaches

구간마다 `SpanStyle(fontFamily, letterSpacing)`을 붙이는 것은 갈래가 하나다. 폰트 폴백 체인
(`Typeface.CustomFallbackBuilder`)은 API 29+ 라 minSdk 26 에 못 쓰고, 무엇보다 글자마다 자간을 다르게 줄 수
없어 제외했다. 갈래는 "이 폰트에 이 한자가 있는가"를 어디서 아느냐였다.

| 갈래 | 내용 | 대가 |
|---|---|---|
| **A. 생성 테이블 (채택)** | 폰트 cmap 에서 뽑은 한자 범위를 Kotlin 소스로 생성해 커밋, 생성 스크립트도 저장소에 둔다 | 폰트 교체 시 재생성 필요 — 폰트 해시 테스트로 막는다 |
| B. 런타임 cmap 파싱 | 첫 사용 때 TTF 를 열어 cmap 을 읽는다 | `Context`·수 MB I/O·자작 파서, JVM 테스트 어려움 |
| C. `Paint.hasGlyph` | 기기에 묻는다 | 시스템 폴백까지 `true` 일 수 있어 판정을 못 믿는다 |

A 를 골랐다. 결정적이고 JVM 단위 테스트가 되며 API 26 에서 그대로 돈다. 폰트는 자주 안 바뀌어 동기화 부담은
테스트 하나면 된다.

## 동작 / 상태

### 구간 분할 규칙

코드포인트 단위로 스크립트를 정하고, 같은 스크립트가 이어지면 한 구간으로 합친다.

| 글자 | 스크립트 |
|---|---|
| 가나 (U+3040–30FF 히라가나·가타카나, U+31F0–31FF 가타카나 확장, U+FF66–FF9F 반각 가타카나) | `JAPANESE` — TW 에도 가나 글리프가 있으므로 명시적으로 JP 로 보낸다 |
| 한자 (U+3005–3007 `々〆〇`, U+303B `〻`, 통합·확장 A·호환·보조 평면) | 아래 순서에서 글리프가 있는 첫 폰트. 둘 다 없으면 1순위 |
| 한글·라틴 등 그 밖의 글자, 숫자 | `DEFAULT` (SUIT) |
| 공백·문장부호·이모지, 전각 숫자·라틴(U+FF10–FF19, U+FF21–FF3A, U+FF41–FF5A) 등 중립 문자 | 앞 구간을 따른다. 맨 앞의 중립 문자는 뒤에 오는 첫 구간을 따른다 (내 판단 — `「` 같은 여는 괄호가 뒤 글자와 같은 폰트로 그려지게) |

한자 순서는 **문장(텍스트 전체)에 가나가 있으면 JP → TW, 없으면 TW → JP** 다. 상용 한자는 두 폰트에 다 있어서
커버리지만으로는 일·중을 가를 수 없고, 먼저 오는 폰트가 상대 언어 한자를 가져가기 때문이다(번체 예문 32자가
JP 에 전부 있다). 결과적으로

- `最高デザイナー ソフィ` → 전부 JP, `頂級設計師徐輝` → 전부 TW (기획 예시와 일치)
- 가나 없는 문장의 일본 신자체(`学` `会` 등, JP 에만 1,180자)는 JP 로 빠진다 — `学校`처럼 한 단어 안에서 JP·TW 가
  섞일 수 있다. 규칙의 결과로 받아들인다
- 한글과 한자가 섞이면 한자는 TW → JP 순을 탄다. 지금은 SUIT(시스템 폴백)로 그린다

### 스타일

- 구간 `SpanStyle` = `TextToppingScript`의 `fontFamily` + `letterSpacingEm` (SUIT·JP -1.1%, TW 0% — 지금 값 유지)
- 베이스 `TextStyle`은 `b01R` + 크기·행간(`withTextToppingSizing`) + 색·정렬. 글꼴·자간은 구간이 덮는다

### 적용 경로

세 경로가 같은 함수의 결과를 쓴다.

1. 입력 필드 (`CanvasTextInputScreen`의 `BasicTextField`) — `OutputTransformation`에서
   `TextFieldBuffer.addStyle`. 표시만 바꾸고 상태 텍스트(서버로 가는 `text`)는 그대로다
2. 캔버스 렌더 (`TextToppingContent`의 `BasicText`) — `AnnotatedString`. C-001·C-305·이미지 저장·타임랩스가
   모두 이 컴포저블을 거친다
3. 크기 맞춤·hug 폭 측정 (`fitTextToppingToHeight`, `CanvasTextInputScreen`의 hug 측정) — 같은 `AnnotatedString`

placeholder(빈 입력 안내 문구)는 고정 한국어라 지금처럼 베이스 스타일(SUIT)로 둔다.

`detectTextToppingScript`는 구간 분할 함수로 대체되어 없어진다.

## 파일 구성

- `model/TextToppingScript.kt` — enum(글꼴·자간·`spanStyle`)만 둔다. 구간 타입은 `model/TextToppingScriptRun.kt`
- `util/TextToppingScriptRuns.kt` — 구간 분할 `textToppingScriptRuns`
- `model/TextToppingHanCoverage.kt` — 커버리지 이진 탐색. `intArrayOf`로 넣으면 JVM 64KB 메서드 한도를 넘어
  BMP 범위는 문자 쌍 문자열, 보조 평면은 정수 배열로 인코딩한다
- `model/TextToppingHanCoverageData.kt` (생성물) — TW·JP 한자 커버리지, 한자 판정 범위 `TEXT_TOPPING_HAN_RANGES`,
  생성 근거 폰트의 SHA-256. 한자 범위의 정본은 생성 스크립트 하나다
- `tools/text-topping-han-coverage/generate.py` — Python + fonttools 생성 스크립트
- 단위 테스트
  - 구간 분할: 기획 예시 두 개, 한글+한자, 숫자, 앞·뒤 중립 문자, 보조 평면 한자, 빈 문자열
  - 커버리지 동기화: `res/font/line_seed_{tw,jp}_regular.ttf` 의 해시가 생성물의 해시와 같다

## 검증 근거 (스파이크, SM-A356N debug 빌드)

- 키 하나당 `OutputTransformation` 중앙값 약 0.2–0.3ms, 크기 맞춤 측정 +약 0.3–0.4ms. 프레임 p50/p90 은 기준선과
  같은 수준이고 40자 연타에서 누락·밀림이 없었다
- 일·번체·간체·한글+한자 네 경우 모두 위 규칙대로 구간이 나뉘고 그려졌다

## 주의 / 열린 질문

- 한글 IME 조합 중 연타(조합 밑줄·커서)는 `adb input text`가 ASCII 만 보내 스파이크에서 못 쟀다. 실기기에서
  손으로 확인한다
- iOS 규칙 일치는 OQ-P-456, CJK 문장부호의 시스템 폰트 폴백은 OQ-P-457, 구간 경계 자간 확인은 OQ-P-458 에서
  추적한다
