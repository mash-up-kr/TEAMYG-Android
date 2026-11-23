---
id: c001-empty-intro-animation
title: 캔버스 메인 빈 캔버스 안내 애니메이션 (C-001-Empty intro animation)
status: draft
category: behavior-spec
platforms: android
verified: 2026-11-23
related_code:
  - feature/groups/canvas/impl/.../viewmodel/CanvasMainViewModel.kt#CanvasMainViewModel
  - feature/groups/canvas/impl/.../screen/CanvasMainScreen.kt#CanvasMainScreen
  - core/designsystem/.../component/ygcanvas/YGCanvas.kt#YGCanvas
  - data/.../source/parfait/local/PastCanvasAlertLocalDataSourceImpl.kt#PastCanvasAlertLocalDataSourceImpl
  - feature/groups/list/impl/.../model/GroupListEmptyIntroState.kt#GroupListEmptyIntroState
related_adr: ADR-0008, ADR-0019, ADR-0036
related_spec: g001-empty-animation
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, C-001, animation]
---

# Spec: 캔버스 메인 빈 캔버스 안내 애니메이션

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

토핑이 0개인 오늘 캔버스에 들어왔을 때, 더미 이미지 3장이 차례로 내려앉고 안내 문구의 "함께 남기고
싶은 순간"이 글자별로 강조됐다 돌아오는 연출을 그룹당 하루 1회 보여 준다. 정책·디자인 원본은 이슈
#726이다. 같은 구조의 선례는 [g001-empty-animation](archive/2026-10-03-g001-empty-animation-design.md)이다.

## 범위

- 포함
  - 더미 이미지 3장의 등장·종료 애니메이션, 안내 문구의 글자별 강조·종료
  - 연출 단계(phase) 상태기계와 그 전이
  - 재생 여부 기록의 저장(계정별 별칭 키, DataStore)
  - 빈 캔버스 안내 문구 조건을 `isEmpty && background == null`에서 `isEmpty`로 변경하고, 문구를
    "…캔버스에 쌓아 주세요"로 교체
- 제외
  - 더미 이미지의 **회전각**. 나중에 받는 값으로 상수만 바꾼다. 구현 시점에는 0도로 둔다
  - 위키 반영

## 결정 요약

| 항목 | 결정 |
|---|---|
| 배경을 고른 빈 캔버스 | 안내 문구와 재생을 **모두 보인다.** 배경 위 텍스트 대비는 디자인 확인 항목으로 남긴다 |
| 재생 기록 키 | 계정 별칭(그룹)을 키로, 값은 마지막으로 재생한 **캔버스 날짜** |
| 대상 날짜 | **오늘 캔버스만.** 지난 날 빈 캔버스는 재생 없이 강조 없는 텍스트만 보인다 |
| 단계 소유 | `phase`는 `CanvasMainViewModel`, `Animatable` 값은 컴포지션 상태 홀더 |

## 동작 / 상태

### 노출 조건

아래 셋이 모두 참이면 진입 시 1회 재생한다. 어떤 경로(탭 전환·그룹 목록 등)로 들어와도 같다.

1. 오늘 캔버스를 보고 있다 (지난 날이 아니다)
2. `isCanvasEmpty`다 (토핑·텍스트 0개)
3. 기록의 날짜가 오늘 캔버스 날짜와 다르거나 기록이 없다

재생은 **시작하는 순간 기록한다.** 03:00(KST)에 새 캔버스가 생기면 날짜가 달라져 같은 그룹도 다시
1회 재생한다. 앱을 재설치하거나 다른 기기·다른 계정이면 기록이 없어 다시 재생한다.

### 단계

| 단계 | 의미 | 화면 |
|---|---|---|
| `Idle` | 판정 전 | 안내 문구만 (더미 없음) |
| `Entering` | 재생 중 (0 ~ 2,500ms) | 더미 3장 등장 + 글자별 강조 |
| `Shown` | 재생 완료, 탭 대기 | 더미 3장이 최종 위치·불투명도 100%, 강조 없음 |
| `Dismissing` | 닫는 중 (300ms) | 더미와 강조가 디졸브로 사라짐 |
| `Dismissed` | 종료 | 강조 없는 텍스트만 |

| 현재 | 사건 | 다음 |
|---|---|---|
| `Idle` | 노출 조건 충족 | `Entering` (기록 쓰기) |
| `Idle` | 기록이 이미 있음 / 지난 날 | `Dismissed` |
| `Entering` | 재생 끝 | `Shown` |
| `Entering` | 뒤로가기·햄버거·그룹원 칩·토핑 추가 | `Shown` (버튼은 정상 동작) |
| `Entering` | 앱 백그라운드 | `Shown` |
| `Entering` | 빈 영역 탭 | `Dismissing` (그 시점부터 바로 디졸브) |
| `Shown` | 빈 영역 탭 | `Dismissing` |
| `Shown` | 위 버튼 | `Dismissing` (버튼은 정상 동작, 동시에 디졸브) |
| `Dismissing` | 디졸브 끝 | `Dismissed` |
| `Dismissing` | 앱 백그라운드 | `Dismissed` |

- 한 번 `Dismissing`·`Dismissed`가 되면 같은 화면 진입 동안 다시 재생하지 않는다.
- 토핑 추가 화면에서 돌아와도 같은 ViewModel이므로 단계가 유지된다(`Shown` 그대로). 화면에서 나갔다가
  새로 진입하면 새 ViewModel이라 `Idle`에서 기록을 조회해 `Dismissed`가 된다.
- 재생 도중 토핑이 생겨 `isCanvasEmpty`가 거짓이 되면 연출 전체를 `Dismissed`로 끝낸다.
- "빈 영역 탭"은 G-001처럼 `PointerEventPass.Final`에서 **소비되지 않은** 다운만 본다. 버튼이 받은 터치는
  닫기로 세지 않고, 버튼 쪽에서 상호작용을 따로 알린다. 터치는 소비하지 않는다.

### 더미 이미지 타임라인

- 자산: `Group 1/2/3.png`를 `drawable-nodpi`에 `canvas_empty_intro_1~3`으로 넣는다.
  크기는 PNG 픽셀 수를 그대로 dp로 쓴다 — 134×134, 180×193, 248×254.
- 시작 상태: 최종 위치에서 Y `-60dp`, 불투명도 0. 종료 상태: 최종 위치, 불투명도 1.
- 이동과 불투명도를 1,000ms 동안 함께 적용하고 이징은 `CubicBezierEasing(0f, 0f, 0f, 1f)`.
- 시작은 진입 기준 500 / 1,000 / 1,500ms. 마지막 이미지는 2,500ms에 끝난다.
- 이동은 바깥 `graphicsLayer`의 `translationY`로, 회전은 안쪽 레이어로 둬서 이미지가 기울어져 있어도
  이동 방향이 화면 기준 수직이 되게 한다.
- 최종 배치는 캔버스 크기에 대한 비율이다. 이미지의 왼쪽 위 모서리를 캔버스 왼쪽 위 기준
  `(x% × 캔버스 너비, y% × 캔버스 높이)`에 둔다(가정, "주의 / 열린 질문" 참고).

  | 이미지 | x | y |
  |---|---|---|
  | `canvas_empty_intro_1` (모자) | 31.64% | 36.58% |
  | `canvas_empty_intro_2` (신발) | 78.34% | 60.15% |
  | `canvas_empty_intro_3` (인물) | 31.49% | 69.04% |

- 배치 비율과 회전은 `CanvasEmptyIntroImage(res, size, xFraction, yFraction, rotation)` 목록 한 곳에
  모아 둔다.
- 접근성 트리에서 제외한다.

### 텍스트 강조

- 문구: "아직 캔버스가 비어 있어요\n친구들과 함께 남기고 싶은 순간을 캔버스에 쌓아 주세요"
- 대상: "함께 남기고 싶은 순간" 중 공백을 뺀 9글자. 나머지는 처음부터 끝까지 gray-500이다.
- 글자색만 바뀐다. 굵기·크기는 그대로다. `AnnotatedString`의 글자별 색 스팬으로 구현한다.
- 글자별 `gray-500 → Black` 150ms(이징 `(0, 0, 0.58, 1)`), 이어서 `Black → gray-500` 300ms
  (이징 `(0.42, 0, 0.58, 1)`).
- 시작은 진입 기준 500ms + 글자 번호 × 150ms. 마지막 글자는 2,150ms에 gray-500으로 돌아온다.
- 타이포: SUIT Medium 12sp, 줄 높이 150%(18sp), 자간 -1.1%(-0.132sp), 가운데 정렬. 시스템 글자
  크기는 무시한다(ADR-0036). 같은 디자인 시스템 토큰이 있는지는 계획 단계에서 확인한다.

### 닫기

- 이미지 알파와 강조 색이 **동시에** 300ms, 이징 `(0, 0, 0.58, 1)`로 사라지고 강조 색은 gray-500으로
  수렴한다. 이후 강조 없는 텍스트만 남는다.
- 재생 도중 탭해도 그 시점의 값에서 바로 디졸브가 시작된다.

## 구조

### 데이터

기록은 `PastCanvasAlert`와 같은 모양이다. 그룹마다 마지막 날짜를 덮어써서 키가 날마다 쌓이지 않는다.

| 계층 | 추가 |
|---|---|
| domain | `CanvasIntroRepository`, `GetCanvasIntroPlayedDateUseCase`, `MarkCanvasIntroPlayedUseCase` |
| data | `CanvasIntroLocalDataSource` + Impl(DataStore, `AccountKeyAlias`), `CanvasIntroRepositoryImpl`, Hilt 모듈 바인딩 |

- 키는 `AccountKeyAlias`로 가린 계정(그룹)이고, 값은 `LocalDate` 문자열이다.
- 별칭 생성에 실패하면 읽기는 `null`, 쓰기는 건너뛴다. 연출이 화면을 막지 않기 위해서다.
- 읽기는 `data.first()`로 하고 `edit`은 쓸 때만 탄다.
- 로그아웃은 기록을 지우지 않고, 탈퇴(`WithdrawUseCase`)만 그 계정 기록을 지운다.

### 화면

- ViewModel은 `phase`와 전이만 들고 있다. 오늘 캔버스가 비었다고 확정되면 기록을 조회하고, 재생을
  시작하면 기록한다.
- 컴포지션 쪽 `CanvasEmptyIntroState`는 `Animatable`(더미 y·알파, 9글자 색, 닫기 알파)만 들고, 값
  getter가 `phase`를 거치게 해서 단계만 바뀌어도 완료·닫힘 상태가 보이게 한다.
- `YGCanvas`의 `emptyMessage: String`을 `emptyContent` 슬롯으로 바꿔 텍스트 스팬과 더미 레이어를
  캔버스 feature 쪽에서 그린다. 조건은 `isEmpty`다. 기존 사용처(프리뷰·테스트 포함)는 계획 단계에서
  전부 확인한다.

## 테스트

| 대상 | 종류 | 검증 |
|---|---|---|
| `CanvasEmptyIntroPhase` | JVM 단위 | 전이표 전부 |
| `CanvasMainViewModel` | JVM 단위 | 기록 없음 → `Entering`·기록 쓰기, 기록 있음·지난 날 → `Dismissed`, 토핑 생성 시 종료, 복귀 시 `Shown` 유지 |
| `CanvasIntroLocalDataSourceImpl` | JVM 단위 | 같은 날 "재생함", 다른 날 "아직 안 함", 별칭 실패, 계정 격리, 읽기에서 `edit` 미사용 |
| 타임라인 수치 | JVM 단위 | 이미지 500/1,000/1,500ms, 글자 500 + 150n ms, 마지막 글자 2,150ms, 전체 2,500ms |
| 화면 연출 | 수동 | 프리뷰·API 28 에뮬레이터에서 타이밍·디졸브 확인. 못 한 부분은 open-questions에 남긴다 |

## 문서

- `docs/status.md` 캔버스 메인(C-001) 절을 현재 상태로 덮어쓴다. 안내판 조건 변경과 OQ-P-304 해소를
  반영한다.
- `docs/log.md`에 한 줄을 추가한다. ADR은 새로 쓰지 않는다(ADR-0008, ADR-0019 범위).

## 주의 / 열린 질문

- 배경을 고른 빈 캔버스에서 회색 텍스트와 더미 이미지가 읽히는지는 디자인 확인이 필요하다.
- 더미 이미지의 PNG 픽셀 수 = dp 해석은 Figma 기준 확인 전 가정이다.
- 배치 퍼센트가 이미지의 왼쪽 위 모서리 기준인지 중심 기준인지는 확인 전이다. 왼쪽 위로 가정했고, 틀리면
  `CanvasEmptyIntroImage` 한 곳의 기준점만 바꾼다.
- 회전각은 아직 받지 못했다. 0도로 구현한다.
