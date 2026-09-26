---
id: c103-multi-subject-selection
title: C-103 다중 피사체 후보 선택 (ML Kit enableMultipleSubjects)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-24
related_code:
  - ImageSegmentationRepositoryImpl.kt#segmentImage
  - ImageSegmentationRepositoryImpl.kt#persistSubject
  - ImageSegmentationRepository
  - SegmentationCandidate
  - SegmentationResult
  - SegmentationBounds
  - SegmentationMask.kt#maskSubjectPixels
  - SegmentationCandidateFilter.kt#filterCandidates
  - SegmentImageUseCase
  - PersistSubjectUseCase
  - ToppingDraftRepository
  - SegmentationViewModel.kt#SegmentationViewModel
  - SegmentationViewModel.kt#SegmentationState
  - SegmentationViewModel.kt#SegmentationIntent
  - SegmentationViewModel.kt#SegmentationEffect
  - SegmentationConfirmViewModel.kt#collectDraft
  - SegmentationScreen.kt#SegmentationScreen
  - SegmentationSubjectHighlight.kt#SegmentationSubjectHighlight
  - SegmentationHighlightGeometry.kt#ScaledRect
  - SegmentationHighlightGeometry.kt#scaledRectOrNull
  - SegmentationHighlightGeometry.kt#pickCandidateIndex
  - SegmentationRoute.kt#SegmentationRoute
  - SegmentationErrorScreen.kt#SegmentationErrorScreen
  - ImageSegmentationRepositoryImpl.kt#runSegmenter
  - ImageSegmentationRepositoryImpl.kt#fallbackCandidates
  - NavKeySegmentationConfirm
related_adr: ADR-0026
related_spec: c103-segmentation-topping-edit, segmentation-pipeline-hardening, c106-topping-place-api
related_architecture: module-structure, data-layer, navigation-flow
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, topping, c103]
---

# Spec: C-103 다중 피사체 후보 선택

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

- **화면 ID**: C-103-select
- **대상 모듈**: `feature/segmentation/impl` + `domain`(모델·Repository·UseCase) +
  `data`(`ImageSegmentationRepositoryImpl`)

> 📌 **develop 머지(2026-08-24, PR #342 = `34bf1939`)** — 스택 둘이 **한 PR로 합쳐져** 들어왔다.
> 브랜치 `feature/c103-multi-subject-ui`가 PR1 커밋 넷을 안은 채 develop을 대상으로 열렸고, 머지
> 트리가 브랜치 팁 `8fe67476`과 같아 충돌 해소 편집이 0건이다. 그래서 develop 이력에는 PR1이
> 별도 머지로 남지 않는다. 17파일 삽입 1074줄·삭제 211줄.
> 신규 유닛: 필터 7건 · 하이라이트 기하 8건 · `SegmentationViewModelTest` 22건(전환 전 13건) —
> 저장소 전체 유닛이 751 → **775건**, 테스트 클래스 87 → **89개**가 됐다.
>
> 브랜치에는 스펙이 적어 둔 `..2f57b54d` 뒤로 커밋 하나(`8fe67476`)가 더 붙었다 — 최종 리뷰가
> 실패 화면 분기 위치와 폴백 이름을 지적한 것을 반영한 것이고, 그 결과가 아래 「실패 화면」 절이
> 말하는 대로 **Route가 `state.isError`로 두 화면을 고르는** 지금의 형태다.
>
> ✅ **실기기에서 다중 후보가 확인됐다**(2026-08-23, Galaxy A35) — 점선 박스가 둘 이상 뜬다.
> 그 과정에서 **네이티브 크래시 하나가 드러나 고쳤고**(옵션 조합, 아래 「ML Kit 옵션」 절),
> 실패 화면 디자인이 나와 **`C-103-Error`를 함께 넣었다**. 계획 밖에서 들어온 이 둘과 주석 정리·
> 아이콘 tint 수정까지 PR2 브랜치에 커밋 넷이 더 얹혀 있다.
>
> 구현이 이 스펙과 갈린 자리 둘. ① `bounds`의 `right`·`bottom`을 ML Kit의 `subject.width`가 아니라
> **비트맵의 실제 치수**에서 뽑는다 — 「ML Kit 값을 후보로 옮기는 규칙」이 둘의 일치를 보장할 수
> 없다고 적어 두고 KDoc은 "반드시 bounds 크기"를 못 박아, 두 경로 중 폴백만 그 불변식을 강제하고
> 있었다(PR1 최종 리뷰가 잡았다). ② 좌표 계산이 `subjectRect`를 `internal`로 올리는 대신 신설
> 파일 `SegmentationHighlightGeometry.kt`로 옮겨 갔다(Compose 타입을 걷어 JVM 테스트가 닿게 했다).

## 목표

사진에 피사체가 여럿일 때 사용자가 그중 하나를 골라 토핑으로 쓰게 한다.

## 왜 지금 없는가

정책에는 처음부터 있었다. 위키 [[누끼-따기]] (link)가
기능정의서 v5를 근거로 "누끼가 다중으로 잡히면 C-103-select로, 단일이면 C-103으로" 갈리는 서브
화면을 정의한다. 구현이 그 갈래를 만들지 않았을 뿐이고,
[c103-segmentation-topping-edit 스펙](2026-08-15-c103-segmentation-topping-edit.md)이
제외 항목에 "다중 피사체 선택(C-103-select 본래 의미)"으로 적어 두었다.

구현이 후보를 하나로 접는 자리는 **ML Kit 옵션 한 곳**이다. `segmentImage`가
`enableForegroundConfidenceMask()`만 켜서 **전경 전체를 하나로 합친 마스크 1장**을 받고,
`maskSubjectPixels`가 그 마스크 전체를 훑어 bounding box를 **하나만** 누적한다. 그래서 떨어져
있는 두 물체가 잡혀도 박스가 둘이 되지 않고 **둘을 함께 감싸는 큰 박스 하나**가 된다.
`SegmentationResult`·`SegmentationState`·`SegmentationSubjectHighlight`가 모두 그 단수 전제 위에
서 있어, 이 라운드는 옵션 한 줄이 아니라 그 경계들을 함께 넓힌다.

## 화면 ID 대응

C-103-select를 **별도 목적지로 만들지 않는다.** `NavKeySegmentation` 하나가 후보 수에 따라 점선
박스를 1개 또는 N개 그린다. 후보가 1개면 지금 화면과 픽셀 단위로 같다.

정책이 화면 ID를 둘로 가른 것은 식별 체계상의 구분이고, 두 상태의 UI가 같은 형태라 목적지를
쪼개면 NavKey·Route·EntryBuilder·ViewModel이 한 벌 늘고 두 화면이 거의 같은 코드를 복제한다.
[c103-segmentation-topping-edit 스펙](2026-08-15-c103-segmentation-topping-edit.md)의
화면 ID 대응 표가 이미 `NavKeySegmentation` 하나에 C-103-loading과 C-103-select를 함께 매핑해
둔 것과 같은 판단이다.

**후보가 1개여도 탭을 요구한다.** 정책 문장("단일이면 C-103으로")은 선택 화면을 건너뛰고 확인
화면으로 직행하라는 뜻으로도 읽히지만, 그렇게 하면 저장 시간이 진입 로딩에 덮이고 **인식이
잘못됐을 때 사용자가 그것을 보기 전에 다음 화면으로 넘어간다.** 현행 동작이기도 하다.

## 파일 구성

| 파일 | 모듈 | 신설 여부 |
|---|---|---|
| `model/SegmentationCandidate.kt` | `domain` | 신설 |
| `usecase/image/PersistSubjectUseCase.kt` | `domain` | 신설 |
| `repository/image/SegmentationCandidateFilter.kt` | `data` | 신설(`internal`) |
| `repository/image/ImageSegmentationRepositoryImpl.kt` | `data` | 수정 |
| `repository/image/SegmentationMask.kt` | `data` | 유지(폴백이 쓴다) |
| `model/SegmentationResult.kt` · `repository/image/ImageSegmentationRepository.kt` · `usecase/image/SegmentImageUseCase.kt` | `domain` | 수정 |
| `viewmodel/SegmentationViewModel.kt` · `screen/SegmentationScreen.kt` · `component/SegmentationSubjectHighlight.kt` · `route/SegmentationRoute.kt` | `feature/segmentation/impl` | 수정 |
| `component/SegmentationHighlightGeometry.kt` | `feature/segmentation/impl` | 신설(`internal`) |
| `screen/SegmentationErrorScreen.kt` | `feature/segmentation/impl` | 신설 |

필터를 `:data`의 `internal`로 두는 것은 `SegmentationMask.kt`의 선례를 따른 것이다. ML Kit가 돌려준
값을 어디까지 신뢰할지는 그 라이브러리를 다루는 구현의 관심사이지 도메인 규칙이 아니다.

## 범위

- **포함**
  - ML Kit 다중 옵션 전환 — `enableMultipleSubjects` + `enableSubjectBitmap`.
  - `SegmentationCandidate` 신설 및 `segmentImage` 반환 타입 다중화.
  - `persistSubject` 신설 — 파일 저장을 **선택 시점으로 이동**.
  - 초안 기록(`ToppingDraftRepository.record`) 호출 시점을 화면 진입에서 **선택 시점으로 이동**.
  - 후보 필터 순수 함수 신설 — 면적 임계·개수 상한·결정적 정렬·동일 bounds 중복 제거.
  - 후보 0건 시 전경 마스크 폴백.
  - `SegmentationState` 재편(경로 3필드 제거, `candidates` 추가)과 `SegmentationIntent` 신설.
  - `SegmentationResult.subjectBounds` 제거.
  - 다중 하이라이트 렌더링과 탭 판정.
  - 선택 결과의 화면 이동을 Route 직접 호출에서 side effect 수신으로 이동.
  - **`C-103-Error` 실패 화면 신설** — 대상을 아예 못 얻은 실패를 토스트가 아니라 화면으로 받는다.
- **제외**(이번 라운드에서 안 함)
  - **Safe Margin +20% 캔버스** — 정책 대조 표 참고. OQ-P-150이 계속 열려 있다.
  - **실패 화면의 재시도·원본 사용 버튼** — 디자인에 없다. 위키 정책과 갈리며 OQ-P-153 ④가 추적한다.
  - **후보가 1개일 때 확인 화면 직행** — 위 "화면 ID 대응" 절의 근거.
  - **후보 비트맵의 명시적 해제**(OQ-P-266).
  - **원본 다운샘플**(OQ-P-228) — 이 라운드가 상주 메모리를 늘려 그 미결의 무게를 키운다.
  - 선택 취소·다시 고르기 동선 — 확인 화면에서 뒤로 가면 이 화면으로 돌아오는 기존 동선 그대로다.
  - 후보에 순번·라벨 표시 — 점선 박스만으로 구분한다.
  - 세그멘테이션 실패 후 재시도(OQ-P-003 ① 잔존).

## 동작 / 구조

### ML Kit 옵션

```kotlin
SubjectSegmenterOptions.Builder()
    .enableMultipleSubjects(
        SubjectSegmenterOptions.SubjectResultOptions.Builder()
            .enableSubjectBitmap()
            .build(),
    ).build()
```

`enableSubjectBitmap()`을 켜면 `Subject.getBitmap()`이 이미 bounds 크기로 잘린 판을 준다. 후보마다
전체 픽셀을 훑어 마스킹하고 다시 자르는 일이 사라진다. `Subject.getConfidenceMask()`는 쓰지 않는다 —
마스크를 받아 우리가 자르는 대안과 결과가 같은데 코드가 길다.

> ⚠️ **`enableForegroundConfidenceMask()`를 함께 켜면 안 된다.** 두 옵션을 한 요청에 실으면 ML Kit
> 다이나마이트 모듈이 **`SIGSEGV`로 죽는다**(2026-08-23 실기기 확인, Galaxy A35 / Android 16).
> 크래시가 `drishti_gl_runn`과 Binder `onTransact` 양쪽에서 나고 스택이 전부 모듈 네이티브라
> **JVM 예외 핸들러를 타지 않는다** — `try/catch`로도 못 잡고 로그도 `logcat -b crash`에만 남는다.
> 공식 문서의 설정 예시 다섯 가지도 두 계열을 한 번도 함께 쓰지 않는다(OQ-P-268).
>
> 그래서 전경 마스크는 **후보가 0건일 때 별도 요청으로** 받는다. 아래 폴백 절 참고.

### ML Kit 값을 후보로 옮기는 규칙

- `left`·`top`은 `startX`·`startY`를 그대로 쓰고, `right`·`bottom`은 **비트맵의 실제 치수**를 더한다
  (`startX + bitmap.width`). `SegmentationBounds`의 `right`·`bottom`은 **exclusive**이므로 `-1`을
  붙이지 않는다. ML Kit의 `getWidth()`가 아니라 비트맵에서 뽑는 이유는 아래 세 번째 불릿에 있다.
- `getBitmap()`은 `@Nullable`이다. 옵션을 켰다는 이유로 비널을 단정하지 않는다 — **null인 subject는
  후보에서 제외**하고, 그래서 후보가 0건이 되면 아래 폴백을 탄다.
- **위치는 `bounds`로 잡고 크기는 비트맵 그대로 쓴다.** `getWidth()`·`getHeight()`와
  `getBitmap()`의 실제 치수가 같다는 보장이 문서에 없으므로, 스케일하지 말고 `(left, top)`에
  무보정으로 배치한다. 어긋나더라도 그림이 찌그러지지는 않는다.

### 도메인 모델

```kotlin
data class SegmentationCandidate(
    /** 원본 좌표계. `right`·`bottom`은 exclusive다 */
    val bounds: SegmentationBounds,
    /** 반드시 [bounds] 크기로 잘린 판이어야 한다 — 저장이 이 판을 원본 캔버스의 (left, top)에 그대로 얹는다 */
    val bitmap: BitmapWrapper,
    val canvasWidth: Int,
    val canvasHeight: Int,
)
```

`canvasWidth`·`canvasHeight`는 `bounds`가 어느 좌표계의 값인지를 말한다. 한 번의 세그멘테이션에서
나온 후보들끼리 같은 값이 복제되지만, 후보 하나가 자기 좌표계를 온전히 설명하므로
`persistSubject(candidate)`에 다른 크기를 실어 보내 좌표가 어긋나는 조합이 성립하지 않는다.

`bitmap`이 bounds 크기여야 한다는 것은 **후보를 만드는 모든 경로가 지켜야 하는 불변식**이다.
KDoc에 못 박는 이유는 폴백 경로가 이것을 어기기 쉬워서다(아래 참고).

`SegmentationResult`는 경로 2개만 남기고 **`subjectBounds`를 걷는다.** 이 설계에서 그 필드를 읽는
곳이 0이 된다 — 화면은 후보의 bounds를 쓰고, 저장 결과에서 bounds를 다시 받을 이유가 없다.

### Repository 계약

한 메서드가 하던 일을 둘로 가른다.

```kotlin
suspend fun segmentImage(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>>
suspend fun persistSubject(candidate: SegmentationCandidate): Result<SegmentationResult>
```

`segmentImage`는 디스크를 건드리지 않는다. `persistSubject`가 두 장을 만든다 — 후보 비트맵을
`canvasWidth × canvasHeight` 투명 캔버스의 `(bounds.left, bounds.top)`에 그려 `subjectImagePath`를,
후보 비트맵 자체를 저장해 `trimmedSubjectImagePath`를 만든다. 두 경로의 의미는 지금과 같다(원본
좌표계 판은 수동 편집이 원본과 픽셀로 겹쳐 그리는 데, 트리밍 판은 미리보기·배치에 쓴다).

UseCase도 대칭으로 `SegmentImageUseCase`와 `PersistSubjectUseCase` 둘이 된다.

**`SegmentationResult` 타입 자체는 화면 경계를 넘지 않는다** — 확인 화면은
`NavKeySegmentationConfirm`의 String 인자 둘로 받고, 초안은 `record`의 개별 파라미터로 받는다.
타입을 유지하는 이유는 하류 결선 때문이 아니라, 두 경로를 함께 돌려주는 자리가 이미 있어 새로
만들 것이 없어서다.

### 후보 필터

판단이 드는 부분이라 기기 없이 검증되는 순수 함수로 뺀다. `SegmentationMask.kt`가 `Bitmap` 대신
`IntArray`·`FloatBuffer`를 받게 해 둔 것과 같은 이유다.

1. 면적이 원본의 `MIN_SUBJECT_AREA_RATIO`(1%) **미만**이면 버린다.
2. `bounds`가 완전히 같은 후보가 둘 이상이면 하나만 남긴다 — 탭 판정이 그중 하나를 영영 못 고른다.
3. 남은 것을 면적 내림차순으로 정렬해 `MAX_SUBJECT_COUNT`(5)개까지 취한다.
4. 면적 동률은 `top` → `left` 오름차순으로 가른다. ML Kit 반환 순서에 기대지 않아야 테스트가
   흔들리지 않는다.

면적은 마스크의 실제 객체 픽셀 수가 아니라 **bounds 면적**으로 잰다. 이 필터가 거르려는 것은 손톱만
한 파편이고, 그 판정에는 bounds로 충분하다. 계산은 `SegmentationBounds`의 `width`·`height`를
그대로 쓴다 — 이미 `right - left`라서 `+1`을 붙이면 틀린다.

⚠️ **두 상수는 실측이 아니다** — 실기기에서 ML Kit가 몇 개를, 어떤 크기로 돌려주는지 아직 보지
않았다(OQ-P-267). 값만 고치면 되도록 상수로 두고 순수 함수로 덮는다.

### 후보 0건 폴백

**필터를 통과한 후보가 0건이면** 폴백을 탄다. `getSubjects()`가 비었을 때뿐 아니라 **후보는 있었으나
전부 임계 미만이라 걸러진 경우도 포함**한다. 뒤엣것을 빼면 이 절이 막으려는 회귀가 그대로 열린다.

**폴백은 세그멘테이션을 한 번 더 돌린다.** 전경 마스크 옵션을 다중 후보 옵션과 함께 켤 수 없기
때문이다(위 옵션 절). 그래서 `enableForegroundConfidenceMask()`만 켠 새 요청을 보내고, 그 결과의
마스크로 `maskSubjectPixels` 경로를 태워 후보 1개를 만든다.

이 설계의 비용은 **후보가 0건인 사진에서만** 든다. 정상 경로는 오히려 가벼워졌다 — 쓰지도 않는
원본 해상도 `FloatBuffer`를 매번 받던 것이 사라졌다.

세그멘터를 열고 optional module을 확인하고 한 장을 처리한 뒤 닫는 부분은 `runSegmenter`로 뽑아
두 호출이 같은 방어를 공유한다.

⚠️ **`maskSubjectPixels`가 만드는 비트맵은 원본 전체 크기이므로 반드시 `bounds`로 크롭한 뒤**
후보에 싣는다. 자르지 않고 실으면 `persistSubject`가 그 판을 `(left, top)`만큼 밀어 그려 오른쪽과
아래가 잘린다 — 크래시가 아니라 조용한 파손이라 눈에 늦게 띈다.

2차 요청이 실패하면 값으로 접는다 — 1차는 이미 성공한 흐름이고, 화면에는 "대상 없음"과 같은
결과로 보이면 된다.

폴백이 없으면 **지금 잘 되던 사진이 다중 전환 이후 실패로 바뀌는 회귀**가 열린다. 대가는 코드
경로가 둘이 되는 것이고, 그 둘째 경로의 bounds 계산은 이미 테스트가 덮고 있다(크롭은 새로 붙는
부분이라 덮이지 않는다 — 아래 테스트 절 참고).

폴백까지 비면 아래 「실패 화면」 절로 간다.

⚠️ **폴백이 실제로 도달하는지는 아직 못 봤다** — 후보가 0건이 되는 사진을 만나지 못했다. 다만
2차가 별개 요청이라 전경 마스크가 채워질 여지는 커졌다(OQ-P-268).

### 상태·의도·효과

```kotlin
data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
) : UiState
```

`subjectImagePath`·`trimmedSubjectImagePath`·`subjectBounds` 세 필드는 상태에서 빠진다. 저장이 탭
시점으로 옮겨 가면 화면이 경로를 들고 있을 이유가 없다.

지금 비어 있는 `SegmentationIntent`가 실제로 쓰인다.

```kotlin
sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    data object ShowError : SegmentationEffect
    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
}
```

의도에는 인덱스만 싣는다 — 비트맵이 의도에 얹히지 않는다. 상태 교체와 탭이 경합할 수 있으므로
`candidates.getOrNull(index)`로 읽고, 없으면 아무것도 하지 않는다.

### 선택 시점에 일어나는 일의 순서

**이 순서가 곧 계약이다.** 지금은 초안 기록이 `init`, 화면 이동이 탭이라 순서가 저절로 보장됐지만,
둘 다 탭 시점으로 옮겨 오면서 순서를 지키지 않으면 다음 화면이 깨진다.

1. 이미 저장이 진행 중이면 **아무것도 하지 않는다**(중복 탭 방어).
2. `isLoading = true`.
3. `persistSubjectUseCase(candidate)`.
4. 성공하면 `toppingDraftRepository.record(subject = trimmed, cutout = full)`를 **끝까지 마친다.**
5. `isLoading = false`.
6. `GoToConfirm`을 post한다.

**4를 3과 병렬로 두거나 6보다 뒤로 미루면 안 된다.** `SegmentationConfirmViewModel`은 정상
진입(`cutoutImagePath != null`)에서 스스로 `record`하지 않고 `collectDraft`로 구독만 하므로,
초안이 비어 있으면 첫 방출에서 `DraftMissing` 토스트를 띄우고 "다음"을 잠근다. 이 흐름에서 초안의
**유일한 writer가 이 화면**이다.

`record`가 실패하거나 흐름 미개시를 알리면 이동하지 않고 `ShowError`로 접는다 — 초안 없이 확인
화면에 보내면 그 화면이 어차피 막힌다.

**`isLoading`은 성공·실패 어느 쪽에서도 반드시 내린다.** 이동이 `goTo`라 이 화면과 ViewModel이
백스택에 남고, 확인 화면에서 뒤로 오면 같은 상태가 되살아난다. 켜 둔 채 나가면 돌아왔을 때
오버레이에 갇혀 아무것도 못 한다.

`persistSubject`가 실패하면 후보 목록은 그대로 두고 토스트만 띄운다. 사용자는 같은 후보를 다시
탭하거나 다른 후보를 고를 수 있다.

**저장 중 뒤로 가기는 막지 않는다.** `YGScaffoldV2` 오버레이는 시스템 뒤로가기를 막지 않고, 막을
근거도 없다. 화면을 떠나면 `viewModelScope`가 취소되고 캐시에 반쪽 PNG가 남을 수 있는데, 캐시는
다음 세그멘테이션 진입에서 통째로 비워지므로(하드닝 라운드가 정한 규칙) 누적되지 않는다. 4단계가
시작되기 전에 취소되면 초안은 손대지 않은 상태로 남는다.

### 다중 하이라이트

`SegmentationSubjectHighlight`가 `bounds` 하나 대신 `List<SegmentationBounds>`를 받고,
`onClickSubject: () -> Unit`이 `onClickCandidate: (index: Int) -> Unit`이 된다.

딤은 후보 사각형을 모두 담은 `Path`를 만들어 `clipPath(path, ClipOp.Difference)` 한 번으로 뺀다.
`clipRect(ClipOp.Difference)`를 후보 수만큼 중첩해도 결과가 같지만(각각 빼는 것과 교집합이 같다),
재귀 없이 평평하게 쓰려면 `Path` 쪽이다. `Path`는 그리기 블록 안에서 매번 만든다 — 이 화면은
정적인 사진 위의 오버레이라 프레임이 계속 돌지 않는다. 애니메이션이 붙으면 그때 `remember`로
올린다. 테두리는 후보마다 지금과 같은 흰 dashed `Stroke`다.

원본 픽셀 좌표를 `ContentScale.Fit` 화면 좌표로 옮기는 계산은 `SegmentationSubjectHighlight` 안의
`private fun subjectRect`에서 **새 파일 `SegmentationHighlightGeometry.kt`의 `scaledRectOrNull`로
옮긴다.** 그리기와 탭 판정이 같은 계산을 공유하는 구조는 그대로다. 이미지 치수가 유효하지 않을 때
`null`을 돌려주는 가드도 유지한다 — `Path.addRect`는 좌표가 `NaN`이면 예외를 던진다.

Compose 타입(`Size`·`Rect`·`Offset`)을 쓰지 않고 `Float`와 자체 `ScaledRect`로 주고받는다. 그래야
JVM 단위 테스트가 이 계산에 닿는다.

**탭 판정은 맞는 후보 중 면적이 가장 작은 것을 고른다.** 목록이 면적 내림차순이라 "뒤에서부터
훑어 처음 맞는 것"과 결과가 같지만, 그렇게 쓰면 **컴포넌트의 올바름이 필터의 정렬 기준에 몰래
의존한다.** 면적 최소로 적으면 정렬이 바뀌어도 깨지지 않는다.

큰 후보 안에 작은 후보가 들어 있을 때 작은 쪽이 잡히고, 큰 쪽은 작은 박스 바깥을 탭해 고른다.
반대로 하면 안쪽 대상을 **아예 고를 수 없다.**

⚠️ **판정 기준이 마스크가 아니라 bounds 사각형이다** — 대각선으로 놓인 두 물체처럼 박스가 크게
겹치는 배치에서는 빈 배경을 탭했는데 후보가 잡히는 체감이 난다. 알려진 한계로 남긴다.

### 실패 화면 (`C-103-Error`)

**실패를 둘로 가른다.**

| 실패 | 표현 | 근거 |
|---|---|---|
| 대상을 아예 못 얻음(디코드 실패·세그멘테이션 실패·폴백까지 0건) | **화면 전체가 `C-103-Error`** | 화면에 할 수 있는 것이 없는 막다른 곳이다 |
| 고른 뒤의 저장 실패 | 토스트 | 후보 목록이 살아 있어 다른 대상을 고를 수 있다. 화면을 덮으면 그 길이 막힌다 |

앞엣것은 **효과가 아니라 상태**(`SegmentationState.isError`)다. 화면이 그 상태로 남아야 하는데
효과는 1회성이라 재구성에서 사라진다.

디자인은 Figma `C-103-Error`다. 상단 바에 닫기 하나, 중앙에 경고 아이콘과 문구 두 줄이 있다.

| 요소 | 디자인 | 구현 |
|---|---|---|
| 상단 바 | 닫기만 | `YGFloatingBarClose` |
| 아이콘 | `Ic_Warning_Round` 44 | `ic_warning_round` + `SizeTokens.Size44` + `ColorFilter.tint` |
| 제목 | `T03_SB` · `gray-900` | `typography.title.t03SB` · `Gray.Gray900` |
| 부제 | `B02_R` · `gray-500` | `typography.body.b02R` · `Gray.Gray500` |
| 배경 | `base/white` | `Gray.White` |
| 간격 | 아이콘↔문구 8, 문구 사이 2 | `layout.gap.gap3` · `gap1` |

문구는 "사진 편집에 실패했어요" + "다른 사진을 선택하거나 다시 시도해 주세요"다. 기존 토스트 문구
("…잠시 후 다시 시도해 주세요")와 안내하는 행동이 다르므로 문자열을 새로 둔다.

⚠️ **재시도·원본 사용 버튼은 없다.** 디자인에 없기 때문이고, 이것은 위키 [[누끼-따기]]
(link)의 "실패 시 재시도 또는 원본 사용 옵션"과 **정면으로
갈린다.** 어느 쪽이 정본인지는 OQ-P-153 ④가 추적한다. 이 라운드는 디자인을 따랐다.

이 화면은 **PR #311이 삭제했던 `SegmentationErrorScreen`을 되살리는 것**이다. 그때의 근거가 "그
화면이 위키가 정의한 실패 처리를 담은 적이 없다"였는데, 디자인이 나오면서 담을 것이 생겼다.

### 문구

`GuideBanner`의 "토핑으로 사용할 대상을 하나 선택해 주세요"는 손대지 않는다. 후보가 여럿인 상황에서
그대로 맞고, 후보가 1개일 때의 현행 문구이기도 하다.

## 메모리

저장 순간의 피크는 **줄고**, 화면이 살아 있는 동안의 상주분은 **는다.**

| | 현행 | 신설안 |
|---|---|---|
| 저장 순간 피크 | `originBitmap` + `pixels` `IntArray` + `subjectBitmap` ≈ 12wh | `originBitmap` + 후보 총합 + 투명 캔버스 ≈ 8wh + 후보 |
| 화면 상주 | `originBitmap` ≈ 4wh | `originBitmap` + 후보 총합(≤5개, 각 bounds 크기) |

투명 캔버스를 새로 만들어도 총량이 현행보다 작은 것은, 현행이 `IntArray`와 `Bitmap`을 동시에 들고
있기 때문이다. 늘어나는 것은 후보 상주분이고 그것이 OQ-P-266이다. 여기에 정상 경로에서도 지불하는
전경 마스크 `FloatBuffer`(순간 8wh, 복사 포함)가 더해진다(OQ-P-268).

⚠️ **표의 "화면 상주" 열은 PR2 이후를 말한다.** PR1만 머지된 상태에서는 후보 목록이 상태에 실리지
않아 `init`이 끝나면 도달 불가가 되고, 상주는 `originBitmap` 그대로다.

⚠️ **`MAX_SUBJECT_COUNT`는 피크 할당을 막지 못한다.** 그 상수가 자르는 것은 **우리가 들고 있는
수**이고, `enableSubjectBitmap()`을 켠 이상 필터가 돌기 전에 ML Kit가 **모든** subject의 비트맵을
이미 만들어 둔다. 세그멘테이션 직후의 순간 봉우리(원본 + 전체 subject 비트맵 + 전경 마스크
`FloatBuffer`)는 위 표에 없다 → OQ-P-268.

## 정책 대조

| 정책 조항 (위키 [[누끼-따기]] 캔버스 생성 규격) | 이번 설계 |
|---|---|
| 기준 영역 = 대상의 최소 바운딩 박스 | ✅ `bounds`가 그것이다 |
| Safe Margin 상하좌우 +20% | ⚠️ **미이행** — `subjectImagePath`는 원본 전체 크기(여백이 20%가 아니라 나머지 전부), `trimmedSubjectImagePath`는 여백 0% |
| 가장자리 부족분을 투명 픽셀로 강제 확장 | ⚠️ **미이행** — 대응 코드 없음 |

**새로 만드는 드리프트가 아니라 물려받는 것이다.** OQ-P-150이 같은 것을 이미 열어 두었고 선행
스펙의 정책 대조 표가 미이행으로 표기했다. 다만 정책 조항의 이름이 **"C-103-Selected"**이고
`persistSubject`가 그 캔버스를 만드는 바로 그 코드라, 가장 손대기 좋은 자리에서 손대지 않기로
정했다는 사실을 여기 적어 둔다.

## PR 분할

스택 둘로 나눈다. PR2의 베이스는 PR1 브랜치다.

| # | 범위 | 주요 파일 | 보이는 변화 |
|---|---|---|---|
| 1 | data·domain 다중화 | `ImageSegmentationRepositoryImpl` · `SegmentationCandidate` · `SegmentationCandidateFilter` · `ImageSegmentationRepository` · `SegmentationResult` · `SegmentImageUseCase` · `PersistSubjectUseCase` · **`SegmentationViewModel`** | 다중 피사체 사진에서 박스와 알맹이가 **가장 큰 후보 하나로 좁아진다.** 단일 피사체 사진은 변화 없음 |
| 2 | UI 다중 표시 | `SegmentationState`·`SegmentationViewModel` · `SegmentationScreen` · `SegmentationSubjectHighlight` · `SegmentationRoute` | 점선 박스가 후보 수만큼 |

**PR1에서도 `SegmentationViewModel`을 고친다.** `SegmentImageUseCase`의 반환 타입이 바뀌므로
그러지 않으면 컴파일되지 않는다. PR1의 ViewModel은 `init`에서 `candidates.firstOrNull()`을 골라
`persistSubject`와 `record`를 **지금처럼 즉시** 부르고, 경로 3필드를 그대로 채운다. 즉
**저장 시점이 탭으로 옮겨 가는 것은 PR2다.**

PR1의 "보이는 변화"가 완전한 무변화는 아니다. 옵션 전환 자체가 결과를 바꾸기 때문이다 — 이 스펙의
진단("후보를 하나로 접는 자리는 ML Kit 옵션 한 곳")이 그대로 여기에도 적용된다. 그래도 나누는
이유는 리뷰의 초점이 갈리기 때문이다. PR1은 ML Kit를 다루는 판단(옵션·매핑·필터·폴백)에, PR2는
그리기와 탭 판정과 순서 계약에 각각 집중된다.

계획을 쓸 때 **선행 스펙에 갱신 표기를 다는 태스크를 함께 넣는다.**
[c103-segmentation-topping-edit 스펙](2026-08-15-c103-segmentation-topping-edit.md)의
네 자리가 이 라운드로 거짓이 된다 — 범위 제외의 "다중 피사체 선택", 화면 ID 대응 표의 "다중 검출
분기 없음", 드리프트 3의 "C-103-select가 사실상 없다", 정책 대조 표의 "C-103-loading /
C-103-select 분리 → 부분 이행".

## 테스트

- `SegmentationCandidateFilterTest`(신규, JVM) — 면적 임계 경계값, **후보는 있으나 전부 임계 미만**,
  상한 초과 시 절단, 면적 동률 타이브레이크, 동일 bounds 중복 제거, 빈 입력.
- `SegmentationMaskTest`(기존) — bounds 계산은 그대로 유효하다. 폴백에 새로 붙는 **크롭은 덮이지
  않는다**(`Bitmap` 의존이라 JVM 대상이 아니다) — 실기기 확인 항목으로 넘긴다.
- `SegmentationViewModelTest` — **확장이 아니라 사실상 재작성이다.** 픽스처와 스텁 반환 타입이
  바뀌고, `state.subjectImagePath`를 단언하는 3건이 후보 기준으로 바뀌며, `subjectBounds == null`
  전제가 `candidates == emptyList()`로 바뀐다. 특히 `init`에서 초안을 적는지 보는 2건은 **PR2에서
  의미가 뒤집힌다**(더 이상 `init`이 적지 않는다) — 탭 시점 기준으로 다시 쓴다.
  신규 케이스: 저장 성공 시 `record` 후 `GoToConfirm`이 나가는 **순서**, 저장 실패 시 목록 유지와
  `ShowError`, 저장 후 `isLoading` 해제, 저장 중 중복 탭 무시, 범위 밖 인덱스 무시.
- `SegmentationHighlightGeometryTest`(신규, JVM) — 좌표 변환과 탭 대상 선택을 `internal` 순수
  함수로 빼내 덮는다. **탭 판정(면적 최소 선택)과 좌표 변환은 이 설계에서 가장 조용히 깨지는
  자리인데 지금 세그멘테이션 화면에는 UI 테스트가 한 건도 없다.**
- `ImageSegmentationRepositoryImpl`의 캔버스 합성 좌표는 `Bitmap`·`Canvas` 의존이라 JVM 유닛
  대상이 아니다. 순수 함수로 뺄 수 있는 부분이 없으므로 **실기기 확인으로 덮는다.**

## 실기기 확인 항목

기기 없이 판정할 수 없어 미검증으로 남는 것들이다.

- ✅ **다중 후보가 실제로 뜬다**(2026-08-23, Galaxy A35) — 점선 박스가 둘 이상 뜨는 것을 확인했다.
  이 라운드의 목적 자체가 여기서 처음 확인됐다.
- ✅ **크래시가 없다** — 옵션 조합을 가른 뒤 같은 동선에서 `logcat -b crash`가 0건이다.
- ✅ **`segmenter.close()` 이후에도 후보 비트맵을 그릴 수 있다**(OQ-P-269) — 위 확인이 곧 이것이다.
  하이라이트가 그려진다는 것은 닫힌 뒤의 비트맵이 유효하다는 뜻이다. 확인 화면까지 다녀오는
  동선은 아직 안 봤다.

남은 것:

1. 배경이 복잡한 사진에서 후보가 몇 개, 어떤 크기로 잡히는가(OQ-P-267의 두 상수를 여기서 조정).
2. 후보가 0건인 사진에서 2차 요청의 전경 마스크가 값을 주는가 — 폴백이 도달하는 경로인지가
   여기서 갈린다(OQ-P-268). 그런 사진을 아직 못 만났다.
3. 폴백을 탄 사진의 결과물이 어긋나지 않는가(크롭이 맞는지).
4. 겹친 후보에서 안쪽·바깥쪽을 모두 고를 수 있는가.
5. 확인 화면에서 뒤로 와 **같은 후보를 다시 고르면** 앞서 두른 테두리가 어떻게 되는가(OQ-P-277).
6. 후보 사각형의 정확한 경계에서 탭이 의도대로 잡히는가.
7. ~~`C-103-Error` 화면이 실제로 뜨는가~~ ✅ **확인**(2026-08-23) — 화면이 뜨고 경고 아이콘 색도
   맞다. 처음엔 검게 나왔는데 `ic_warning_round` 가 `fillColor="#000000"` 벡터라 쓰는 쪽이 tint 를
   걸어야 했다(같은 아이콘을 같은 색으로 쓰는 `CameraPermissionRequestComponent` 선례가 있었다).

## 미결 신규

- **OQ-P-266** — 후보 비트맵을 명시적으로 해제하지 않는다.
- **OQ-P-267** — 후보 필터 상수 둘의 근거가 실측이 아니다.
- **OQ-P-268** — 다중 모드에서 전경 마스크가 채워지는지 미검증이고, 정상 경로에서도 그 비용을
  지불한다.
- **OQ-P-269** — `segmenter.close()` 이후 ML Kit가 준 비트맵의 수명이 문서에 없다.
- **OQ-P-277** — 같은 후보를 다시 고르면 초안에 적힌 테두리가 조용히 덮인다. 저장이 탭 시점으로
  옮겨 오면서 생긴 새 동작이다.

## 연관

- 정책: 위키 [[누끼-따기]] (link) —
  C-103-loading / C-103-select 분기 정의(기능정의서 v5)
- 선행: [c103-segmentation-topping-edit](2026-08-15-c103-segmentation-topping-edit.md) —
  제외 항목으로 적힌 다중 선택을 이 라운드가 닫는다
- 선행: [segmentation-pipeline-hardening](2026-08-18-segmentation-pipeline-hardening.md) —
  `maskSubjectPixels` 순수 함수 분리·캐시 정리가 이 라운드의 전제다
- [ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md) — 초안 DataStore SSOT. 선택 시점 순서
  계약이 이 결정 위에 선다
