---
id: topping-edit-empty-subject-guard
title: 토핑 편집 빈 알맹이 차단 (Topping edit empty subject guard)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-09
related_code:
  - ToppingEditViewModel#completeEdit
  - ToppingEditEffect
  - ToppingEditRoute
  - ToppingEditMask#buildCutoutBitmap
  - ToppingEditMask#measureSubject
  - ToppingEditMask#trimTo
  - ToppingEditMask#SubjectMeasure
  - SubjectCoverage#isLargeEnough
  - SegmentationCandidateFilter#filterCandidates
  - SubjectCoverage#floorPixels
  - ImageSegmentationRepositoryImpl#postProcess
  - SegmentationConfirmViewModel
  - ImageUploadRepositoryImpl#upload
related_adr:
related_spec: segmentation-mask-postprocessing
related_architecture:
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, topping, validation]
---

# Spec: 토핑 편집 빈 알맹이 차단

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

토핑 편집에서 영역을 전부 지운 뒤 완료를 눌러도 업로드가 그대로 성립하는 구멍을 막는다.
자동 누끼 경로에는 이미 커버리지 하한이 있으나 수동 편집이 그 하한을 우회한다.

## 배경 — 관찰된 사실

편집 화면에서 지우기로 영역을 전부 지우고 완료를 누르면, 완전히 투명한 이미지가 서버까지
올라간다. 경로 어디에도 "알맹이가 남았는가"를 보는 관문이 없다.

`trimTransparentBounds`는 알파가 있는 픽셀이 하나도 없으면 자를 기준이 없어 **원본을 그대로
돌려준다**(주석에 적힌 의도된 동작이다). 그래서 전부 지운 결과는 0×0이 아니라 원본 해상도
크기의 완전 투명 비트맵이 된다.

그 뒤로는 아무도 걸러내지 않는다.

| 지점 | 지금 보는 것 | 빈 알맹이를 막는가 |
|---|---|---|
| `ToppingEditViewModel#completeEdit` | 파일 저장 성공 여부 | 아니오 |
| `SegmentationConfirmViewModel` | 초안의 `subjectImagePath`가 null인지 | 아니오 |
| `SegmentationConfirmRoute` 다음 버튼 | `isDraftReady` 하나 | 아니오 |
| `ImageUploadRepositoryImpl#upload` | `File.isFile` | 아니오 |

투명 PNG도 정상 파일이므로 발급·PUT·확정이 모두 성공한다. 결과로 캔버스에는 보이지 않는
토핑이 놓이고, 탭 판정 영역은 살아 있어 다른 사람이 그 자리를 누르면 Spotlight와 작성자
Toast가 뜬다. 되돌릴 방법은 삭제뿐이다.

**자동 경로에는 이미 하한이 있다.** `SegmentationCandidateFilter`가 커버리지 하한
(캔버스 면적의 5/10000, 최소 2,500px)으로 너무 작은 후보를 버린다. 판정 지표와 그 값의
근거는 [`archive/2026-08-24-segmentation-mask-postprocessing.md`](2026-08-24-segmentation-mask-postprocessing.md)
「필터 판정」에 있다. 즉 새 기준을 만들 필요가 없고, **있는 기준이 수동 편집에 닿지 않는 것이
문제다.**

## 범위

**포함**

- 편집 화면 완료 시점의 하한 판정과 차단
- 하한 상수·판정 함수를 `data` 내부에서 `domain`으로 옮겨 두 경로가 같은 것을 보게 함
- 차단 사실을 알리는 Toast

**제외**

- 확인 화면 다음 버튼의 보조 방어 — 그 경로로 들어오는 알맹이는 이미 검증을 통과한 것이라
  실익이 적고, 확인 화면이 매번 파일을 디코딩해야 한다.
- 업로드 계층의 최후 방어선 — 업로드 직전에 원본 해상도 비트맵을 다시 열어 픽셀을 훑어야
  해서 비용과 OOM 위험이 크고, 실패 시점이 사용자에게서 멀다.
- 이미 서버에 올라간 빈 토핑의 정리.

## 설계

### 판정 기준 — 자동 경로의 하한을 재사용한다

지표도 하한도 자동 경로와 같게 둔다. 지표는 알파 합(`coverageAlphaSum`, 255로 나누면 실제로
칠해진 픽셀 수)이고, 하한은 `max(2500, 캔버스 면적 × 5 / 10000)` 픽셀이다.

기준을 하나로 두는 이유는 우회를 막기 위해서다. 자동으로는 버려질 크기를 수동 편집으로
만들어 낼 수 있으면, 하한이 있다는 사실 자체가 의미를 잃는다.

캔버스 면적은 알맹이의 원본, 즉 `buildCutoutBitmap`이 돌려주는 비트맵의 면적이다. 이 비트맵은
언제나 원본 크기이므로 자동 경로가 쓰는 면적과 같은 좌표계다.

### 정책의 자리 — `domain`

하한 상수와 판정 함수는 지금 `data`에 `internal`로 있어 `feature/segmentation/impl`에서 볼 수
없다. 이것을 `domain`의 정책 하나로 올린다. 자리는 `SegmentationCandidate`·`SegmentationBounds`가
있는 `domain/model` 아래다 — 같은 세그멘테이션 좌표계를 말하는 것끼리 모은다.

```kotlin
object SubjectCoverage {
    fun floorPixels(canvasArea: Long): Long
    fun isLargeEnough(alphaSum: Long, canvasArea: Long): Boolean
}
```

알파 합 비교까지 정책 안에 넣어, 호출부가 255를 각자 곱하지 않게 한다. 그 곱을 밖에 두면
지표의 단위를 호출부마다 다시 이해해야 하고, 한쪽만 고쳐 기준이 갈라질 자리가 생긴다.

`data` 쪽 호출부는 둘이다 — `SegmentationCandidateFilter`의 후보 필터와
`ImageSegmentationRepositoryImpl#postProcess`의 알파 정제 하한. 둘 다 새 정책을 부른다.
값의 근거를 가리키는 주석은 따라 옮긴다.

테두리 굵기 범위를 `domain` 한 곳으로 모은 선례(`ToppingBorder.WIDTH_RANGE_DP`)와 같은
모양이다. 플랫폼이 늘어도 정책은 한 자리에 남는다.

### 측정 — 한 번의 스캔으로 경계와 알파 합을 함께 낸다

`trimTransparentBounds`는 이미 전 픽셀을 훑는다. 여기에 측정을 얹어 스캔을 한 번으로 유지한다.
원본 해상도는 수천만 픽셀에 이르므로 두 번 훑는 선택은 그만큼을 그냥 버린다.

`ToppingEditMask`에 순수 함수를 둔다. `android.graphics`에 의존하지 않고 `IntArray`만 받으므로
Robolectric 없이 JVM 테스트로 덮인다.

```kotlin
internal data class SubjectMeasure(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val alphaSum: Long,
) {
    val isEmpty: Boolean get() = right < left || bottom < top
}

internal fun measureSubject(pixels: IntArray, width: Int, height: Int): SubjectMeasure
```

알파 합은 `Long`이다. 12MP 불투명 이미지의 합이 `Int` 범위를 넘는다.

`trimTransparentBounds`는 이 함수를 부르는 얇은 껍데기가 된다. 알파가 하나도 없을 때 원본을
그대로 돌려주는 기존 성질은 유지한다 — 아래 차단이 그 경우를 앞에서 잡지만, 이 함수 홀로도
안전해야 한다.

### 차단 시점 — 파일을 쓰기 전에

`completeEdit`은 `buildCutoutBitmap` 직후에 측정한다. 하한 미달이면 저장으로 넘어가지 않고
그 자리에서 되돌린다.

1. 비트맵을 회수한다.
2. `isSaving`을 `false`로 되돌린다.
3. 차단 effect를 던진다.
4. 함수를 빠져나온다.

**테두리만 고치는 진입(`borderOnly`)은 판정에서 뺀다.** 이 문이 막으려는 것은 사용자가 방금
비운 알맹이인데, 그 진입에는 영역 탭이 없어 알맹이를 비울 수단이 없다. 반대로 판정을 걸면
탈출구가 사라진다 — 캔버스에 놓인 토핑을 여는 경로라 로컬 알맹이가 없으면 서버에서 받은
그림이 원본 자리에 들어오고, 그 커버리지가 하한에 못 미치면 되돌리기로 늘릴 수 없어 두른
테두리를 잃고 화면을 벗어나는 것 말고 방법이 없다. 하한이 없는 플랫폼이 올렸거나 하한 도입
이전에 저장된 토핑이 그 경우에 든다.

저장 뒤에 판정하면 쓸모없는 파일 두 개가 캐시에 남고, 지우는 코드를 따로 들여야 한다.

`isSaving` 복구가 빠지면 완료 버튼이 영구히 잠긴 화면이 된다. 이 상태는 사용자가 뒤로
나가기 전에는 풀 방법이 없다.

### 사용자 피드백

`ToppingEditEffect`에 `SubjectTooSmall`을 더한다. `SaveFailed`와 합치지 않는 이유는 두 가지가
다른 사건이기 때문이다 — 하나는 저장이 실패한 것이고 다른 하나는 입력이 규칙에 어긋난 것이며,
사용자가 할 일도 다르다(재시도 대 되돌리기).

`ToppingEditRoute`는 Toast만 띄우고 **화면을 떠나지 않는다.** `LoadFailed`와 달리
`navigator.onBack()`을 부르지 않는다. 편집 화면에 머물러야 되돌리기로 방금 지운 획을 되살릴 수
있다.

문구는 `topping_edit_subject_too_small` = `남은 영역이 너무 작아 저장할 수 없습니다`.
완전히 지운 경우와 아주 조금만 남긴 경우를 함께 설명하며, 판정이 면적 기준이라는 사실과
어긋나지 않는다.

## 검증

**자동 테스트**

| 대상 | 무엇을 고정하는가 |
|---|---|
| `domain` · `SubjectCoverageTest` | 하한과 정확히 같으면 통과, 미만이면 차단, 큰 캔버스에서 비율 하한이 절대 하한을 넘어서는 경계, 알파 합 0 |
| `feature/segmentation/impl` · `ToppingEditMaskTest` | `measureSubject`가 한 번의 스캔에서 경계와 알파 합을 함께 내는지, 전부 투명일 때 `isEmpty`가 참인지, 반투명 픽셀의 알파가 합에 그대로 들어가는지 |
| `data` · `SegmentationCandidateFilterTest` | 정책을 옮긴 뒤에도 후보 판정 결과가 그대로인지(기존 단언 유지, import만 변경) |

**수동 검증** — `ToppingEditViewModel`은 `android.graphics.Bitmap`에 묶여 있고 이 저장소에는
Robolectric이 없다. 아래는 실기기로 확인한다.

1. 영역을 전부 지우고 완료 → Toast가 뜨고 화면에 남는다.
2. 그 상태에서 되돌리기 → 획이 살아나고 완료가 정상 동작한다.
3. 아주 작은 조각만 남기고 완료 → 하한 미만이면 같은 Toast가 뜬다.
4. 정상 편집 후 완료 → 확인 화면으로 넘어가고 업로드까지 성립한다.
5. 테두리만 고치는 진입(`borderOnly`) → 판정을 건너뛰므로 차단이 걸리지 않는다. **로컬 알맹이가
   없는 토핑**(서버에서 받아 여는 경우)으로 확인해야 의미가 있다 — 방금 편집한 토핑은 어차피
   하한을 넘으므로 이 갈래가 드러나지 않는다.

## 주의 / 열린 질문

- **기획 문서에 이 규칙이 없다.** "빈 토핑을 올릴 수 없다"는 기획 산출물 어디에도 적혀 있지
  않고, 하한과 문구도 기획이 정한 값이 아니다. 구현 결정으로 먼저 넣고
  [`../synthesis/open-questions.md`](../../../synthesis/open-questions.md)에 확인 항목으로 남긴다.
- **iOS 정합.** 같은 서버에 같은 기능으로 올리므로 상한이 아니라 하한도 플랫폼마다 다르면
  한쪽에서만 올라가는 토핑이 생긴다. 기획이 확정할 때 함께 정한다.
- **ViewModel 글루의 테스트 공백.** 차단 분기·`isSaving` 복구·effect는 자동 테스트가 없다.
  Robolectric 도입은 이 스펙의 범위 밖이고, 필요해지면 별도로 판단한다.
- **업로드 다운스케일과의 상호작용.** 업로드 경로에 축소가 들어가면 서버에 저장되는 알맹이의
  커버리지가 그만큼 줄어든다. `borderOnly`를 판정에서 뺀 결정이 그 영향을 받는 자리다.
