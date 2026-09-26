---
id: c103-error-use-original
title: C-103-Error 실패 화면 통합과 「편집 없이 사용」
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-06
related_code:
  - SegmentationViewModel.kt#SegmentationState
  - SegmentationViewModel.kt#SegmentationIntent
  - SegmentationViewModel.kt#SegmentationEffect
  - SegmentationViewModel.kt#loadCandidates
  - SegmentationRoute.kt#SegmentationRoute
  - SegmentationErrorScreen.kt#SegmentationErrorScreen
  - SegmentationScreen.kt
  - SegmentationConfirmViewModel.kt#SegmentationConfirmState
  - SaveBitmapUseCase.kt
  - ImageSegmentationRepository.kt#saveBitmap
  - YGButtonType.kt#Medium
  - strings.xml
related_adr:
  - 0012-mlkit-subject-segmentation.md
related_spec:
  - 2026-09-02-segmentation-module-install.md
  - 2026-08-23-c103-multi-subject-selection.md
related_architecture:
  - state-management.md
  - navigation-flow.md
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, c103, error]
---

# Spec: C-103-Error 실패 화면 통합과 「편집 없이 사용」

> 🔁 **「편집 없이 사용」은 2026-09-10에 「직접 편집」으로 바뀌었다**(커밋 `3217e62f7`, PR #487 `95b7fc4d5`로
> develop 머지). 원본을 한 번 저장하는 것까지는 같지만, 초안을 바로 기록하지 않고 사진 전체가 남은 마스크로
> C-104를 연다. 편집 결과를 받은 뒤에 초안을 기록하고 C-103-confirm으로 간다. 심볼은 `UseOriginal`이
> `EditManually`·`OnEditResult`로, `useOriginal()`이 `editManually()`·`recordEditResult()`로, `onClickUseOriginal`이
> `onClickEditManually`로 바뀌었다. 문구는 버튼 `segmentation_error_edit_manually`(「직접 편집」)와 설명 「다시
> 시도하거나 직접 편집할 수 있어요」다. 이 문서의 버튼 동작·문구 서술은 그 시점까지의 기록이다.
> ⚠️ 디자인 확정본과 다른 이름이 된 근거가 문서에 없다(OQ-P-401). 편집 화면으로 가는 갈래는
> [navigation-flow](../../../architecture/navigation-flow.md)에 있다.

> ✅ **구현 완료·develop 머지(2026-09-06, PR #457 `5f860cb9c`)** — 4 Task 전부 리뷰를 통과했고
> 머지본을 스펙과 줄 단위로 대조한 결과 **설계와 어긋난 자리가 없다.** 유닛 테스트가 25건에서
> 28건이 됐고, 스펙이 적어 둔 정리 검사 넷(`SegmentationErrorKind`·`errorKind`·
> `segmentation_module_error_*`·`saveEditedImage`)이 `develop`에서 모두 0건이다.
> ⚠️ **프리뷰 육안 대조는 여전히 하지 않았다** — 버튼 폭 161.5dp와 설명·버튼 사이 24dp는 코드로만
> 확인했고 테스트가 잡지 못하는 자리다.
> 📌 **본문 「편집 없이 사용」절의 코드 블록보다 머지본이 한 겹 두껍다** — 초안 기록을
> `runSuspendCatching { … }.getOrDefault(false)`로 감싸 실패하면 `ShowError`로 보낸다. 같은 절의
> 「실패 처리와 중복 실행」이 요구한 동작 그대로이고, 예시가 저장 실패만 보여 준 것이다.
>
> 디자인 `C-103-Error`가 확정되어 실패 화면이 한 벌 문구와 버튼 둘을 갖는다. 이 스펙은 그
> 확정본에 코드를 맞추고, 오래 열려 있던 「원본 사용」 선택지를 채운다.
> 대응 이슈는 [#348](https://github.com/mash-up-kr/TEAMYG-Android/issues/348)이다.

## 목표

누끼 실패 화면이 **말한 것과 할 수 있는 것을 일치시킨다.** 지금 화면은 "다른 사진을 선택하거나
다시 시도해 주세요"라고 안내하면서 사진을 다시 고를 길을 주지 않고, 실패 원인에 따라 문구를 둘로
가르지만 디자인은 한 벌만 요구한다. 그리고 세그멘테이션이 끝내 안 되는 기기에서 사용자가 토핑을
만들 방법이 아예 없다.

## 왜 지금인가

이슈 #348은 PR #342 리뷰에서 나왔다. `segmentation_error_description`이 재시도와 사진 재선택
두 가지 행동을 안내하는데 화면에는 닫기 버튼 하나뿐이었고, 닫기는
`navigator.popUpTo<NavKeyCanvasMain>()`이라 캔버스까지 나가 버려 안내한 두 행동 중 어느 것도
그 자리에서 할 수 없었다. 이후 PR #438이 재시도 버튼을 넣어 절반을 채웠지만, 그 버튼은
[segmentation-module-install](2026-09-02-segmentation-module-install.md) 「재시도」 절이
적어 둔 대로 **디자인 검토를 받으려고 먼저 놓은 시안**이었다.

검토 결과가 디자인 `C-103-Error`로 나왔다. 재시도 버튼은 확정됐고, 두 번째 버튼
「편집 없이 사용」이 함께 들어왔으며, 문구는 원인과 무관하게 한 벌이 됐다.

「편집 없이 사용」이 실질적으로 중요한 이유는 모듈 설치 실패 사례에 있다. 같은 스펙이 기록한
Galaxy Z Flip 3(SM-F711N)에서 GMS가 `INTERNAL_ERROR`로 모듈 설치를 몇 시간 동안 잇지 못했다.
그동안 그 기기의 사용자에게는 **토핑을 만들 경로가 하나도 없었다.** 원본을 그대로 쓰는 길이
있으면 모델이 오지 않아도 앱을 쓸 수 있다.

## 범위

- **포함**
  - 실패 문구를 한 벌로 통합하고 `SegmentationErrorKind` 분기를 걷는다.
  - 실패 화면에 「편집 없이 사용」 버튼을 더한다. 누르면 원본 사진을 토핑 재료로 삼아
    `C-103-confirm`으로 간다.
  - 원본 디코드 실패를 실패 화면이 아니라 **뒤로 가기**로 처리한다.
  - 재시도 버튼에 붙어 있던 「디자인 검토 대기 시안」 표기를 걷는다.
- **제외**
  - **실패 원인별 문구 분기** — 디자인이 한 벌이다. 원인은 로그로만 남긴다.
  - **원본 사용 전용 확인 화면** — 기존 `C-103-confirm`을 그대로 쓴다.
  - **모듈 설치 설계 변경** — [segmentation-module-install](2026-09-02-segmentation-module-install.md)의
    설치 대기·공유 대기·종료 판정은 그대로 유효하다.
  - **캔버스에서 원본 토핑을 다르게 취급하는 규칙** — 저장 이후 경로는 후보 선택과 같다.

## API / 인터페이스

### 상태·인텐트·이펙트

```kotlin
data class SegmentationState(
    val isLoading: Boolean = true,
    val originBitmap: Bitmap? = null,
    val candidates: List<SegmentationCandidate> = emptyList(),
    /** 참이면 화면 전체가 `C-103-Error` 로 바뀐다 */
    val isError: Boolean = false,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class ClickCandidate(val index: Int) : SegmentationIntent
    data object Retry : SegmentationIntent
    data object UseOriginal : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect {
    data object ShowError : SegmentationEffect
    /** 원본을 못 읽어 이 화면에서 할 수 있는 일이 없다 */
    data object GoBack : SegmentationEffect
    data class GoToConfirm(
        val subjectImagePath: String,
        val trimmedSubjectImagePath: String,
    ) : SegmentationEffect
}
```

`SegmentationErrorKind`를 지운다. 화면이 원인을 쓰지 않으므로 상태가 원인을 나를 이유가 없다.
`SegmentationRoute`의 `titleRes()`·`descriptionRes()` 매핑 함수 둘도 함께 사라진다.

⚠️ 원인 추적 수단은 그대로 남는다. `SegmentationViewModel`이 실패를 받을 때 남기는
`viewModelLogger.e`와 `ImageSegmentationRepositoryImpl`의 `toSegmentationException`이 예외 타입과
ML Kit 오류 코드를 기록한다. 화면에서 분기가 사라지는 것이지 진단이 사라지는 것이 아니다.

### 화면

```kotlin
@Composable
internal fun SegmentationErrorScreen(
    onClickRetry: () -> Unit,
    onClickUseOriginal: () -> Unit,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
)
```

제목·설명 파라미터를 걷는다. 문구가 한 벌이라 밖에서 실어 보낼 것이 없고, 파라미터로 남겨 두면
호출부마다 다른 문구를 넣을 수 있다는 뜻이 되어 통합 결정과 어긋난다.

## 동작 / 상태

### 실패 화면에 도달하는 경로

| 상황 | 원본 비트맵 | 결과 |
|---|---|---|
| `decodeImage` 실패 | 없음 | **`GoBack`** — 실패 화면을 띄우지 않는다 |
| 모듈 준비 실패(`ModuleNotReady`) | 있음 | `isError = true` |
| 세그멘테이션 처리 실패(`Process` 외) | 있음 | `isError = true` |
| 성공했으나 후보 0건 | 있음 | `isError = true` |

디코드 실패를 갈라 낸 것이 이 설계의 핵심 장치다. 그 덕분에 **실패 화면은 원본 비트맵이 반드시
살아 있는 상태에서만 뜬다.** 「편집 없이 사용」 버튼에 비활성 상태나 숨김 분기가 필요 없어지고,
"편집 없이 사용할 수 있어요"라는 설명이 언제나 참이 된다.

### 뒤로 가기

`GoBack`은 Route에서 `navigator.onBack()`으로 받는다. **토스트는 띄우지 않는다.**

⚠️ **돌아가는 곳은 사진 확인 화면이 아니라 카메라 화면 또는 갤러리 피커다.** `PictureConfirmRoute`가
세그멘테이션으로 갈 때 `goTo`가 아니라 `goToAndPopCurrent`를 써서 사진 확인 화면을 백스택에서
치환하기 때문이다. 그 아래에 남아 있는 것은 촬영 화면이거나 갤러리 피커다. 목적지가 예상과 달라도
설계의 목적은 그대로 선다 — 원본을 못 읽었으면 다른 원본을 고르는 것이 유일한 길이고, 두 화면 다
그 일을 할 수 있는 자리다.

⚠️ 이 결정은 기존 동작을 바꾼다. 지금은 디코드가 실패해도 실패 화면이 뜨고 사용자가 재시도를
누를 수 있었다. 만료된 URI나 깨진 파일은 재시도해도 결과가 같아서, 그 자리에 머무는 것이 사용자에게
줄 것이 없다.

### 「편집 없이 사용」

기존 후보 선택 경로를 그대로 재사용한다. 새 화면도 새 내비게이션 축도 만들지 않는다.

```
후보 선택     → persistSubject → 초안 기록 → GoToConfirm
편집 없이 사용 → saveBitmap     → 초안 기록 → GoToConfirm
```

다른 것은 저장 단계 하나다. 후보 선택은 `persistSubject`가 잘린 판과 원본 크기 캔버스 판
**두 장**을 떨구는데, 원본 사용에서는 그 두 판이 같은 그림이다. 잘라낼 여백도 얹을 자리도 없기
때문이다. 그래서 **한 번만 저장하고 같은 경로를 두 자리에 싣는다.**

```kotlin
val path = saveBitmapUseCase(originBitmapWrapper).getOrElse {
    releaseLoading()
    postSideEffect(SegmentationEffect.ShowError)
    return@launch
}

toppingDraftRepository.record(
    subjectImagePath = path,
    cutoutImagePath = path,
    borderColorArgb = null,
    borderWidthDp = null,
)

postSideEffect(
    SegmentationEffect.GoToConfirm(subjectImagePath = path, trimmedSubjectImagePath = path),
)
```

⚠️ **`persistSubject`에 원본 전체를 후보로 만들어 넘기지 않는다.** 그 경로는 원본 크기 빈
비트맵을 하나 더 만들어 그 위에 원본을 그리므로 순간 메모리가 두 배로 뛴다. 카메라 사진
4000×3000 ARGB_8888 한 장이면 48MB가 96MB가 되고, 결과로 남는 것은 내용이 같은 PNG 두 개다.
저장 한 번이 더 싸고 결과가 같다.

⚠️ **`AndroidBitmap`의 생성자가 `core:util:android` 모듈 내부로 막혀 있다.** feature 모듈에서
`state.originBitmap`을 다시 감쌀 수 없으므로, ViewModel이 디코드에서 받은 `BitmapWrapper`를
필드로 들고 있다가 그대로 넘긴다. 화면이 그리는 `state.originBitmap`은 지금처럼 유지한다.

📌 **이름을 바꿨다(2026-09-06, 코드 리뷰 반영).** 처음에는 KDoc만 넓히고 이름을 두려 했다. 근거는
"편집 저장 호출부까지 건드리면 범위를 넘는다"였는데, 실제로 세어 보니 순수 기계적 치환 6파일이었다.
이름과 역할이 어긋나는 것을 주석으로 변명하는 상태가 더 비싸다.

- `SaveEditedImageUseCase` → `SaveBitmapUseCase`
- `ImageSegmentationRepository.saveEditedImage()` → `saveBitmap()`

이름이 역할을 말하게 됐으므로 "편집 결과에 한정되지 않는다"고 적어 둔 KDoc 두 줄은 지운다.

### 도착한 확인 화면의 상태

`SegmentationConfirmRoute`가 인자를 옮겨 담는 규칙은 그대로다.

| `NavKeySegmentationConfirm` | `SegmentationConfirmViewModel` | 원본 사용일 때의 값 |
|---|---|---|
| `trimmedSubjectImagePath` | `subjectImagePath` | 원본 경로 |
| `subjectImagePath` | `cutoutImagePath` | 원본 경로(같은 값) |
| `sourceImageUri` | `sourceImageUri` | 진입 시 받은 원본 URI |

`cutoutImagePath`가 널이 아니므로 `isReuseEntry`가 거짓이 되어 확인 화면이 초안을 다시 적지
않는다. 초안은 이미 이 화면이 적었다. `editImagePath`는 `cutoutImagePath`를 골라 원본을 편집
재료로 삼는다.

`sourceImageUri`가 널이 아니므로 `isBorderOnlyEdit`도 거짓이다. **사용자는 「편집 없이 사용」으로
들어온 뒤에도 확인 화면에서 테두리와 영역을 손볼 수 있다.** 「편집 없이」가 막다른 길이 되지 않고,
자동 누끼가 실패한 사진을 손으로 다듬는 길이 열린다.

### 실패 처리와 중복 실행

저장이나 초안 기록이 실패하면 후보 선택과 같이 `SegmentationEffect.ShowError` 토스트를 띄우고
실패 화면에 그대로 머문다. 로딩은 성공·실패와 무관하게 걷는다.

중복 실행은 `BaseViewModel`의 키 기반 `launch`로 막는다. 진행 중이면 새 요청을 **버린다.**
키는 `USE_ORIGINAL_KEY`를 새로 둔다.

세 동작(`loadCandidates`·`selectCandidate`·`useOriginal`)은 UI에서 동시에 닿을 수 없다. 재시도가
도는 동안 `isError`가 걷히고 로딩 오버레이가 덮으므로 버튼이 화면에서 사라지고, 후보가 있는
화면과 실패 화면은 서로 배타적이다. 그래도 키를 나누는 이유는 **상수 이름이 하는 일과 어긋나지
않게 하기 위해서다.** 남의 키를 빌리면 읽는 사람이 두 동작 사이에 없는 관계를 읽게 된다.

## 표시·제어 규칙

### 문구

```xml
<string name="segmentation_error_title">사진 편집에 실패했어요</string>
<string name="segmentation_error_description">다시 시도하거나 편집 없이 사용할 수 있어요</string>
<string name="segmentation_error_retry">다시 시도</string>
<string name="segmentation_error_use_original">편집 없이 사용</string>

<!-- 삭제 -->
<!-- segmentation_module_error_title, segmentation_module_error_description -->
```

`segmentation_error_message`(고른 뒤 실패 토스트)는 그대로 둔다. 화면을 덮지 않는 실패에 쓰는
별개 문구다.

### 버튼

| 버튼 | 타입 | 색 |
|---|---|---|
| 다시 시도 | `YGButtonType.Medium.Primary` | Gray900 채움, White 글자 |
| 편집 없이 사용 | `YGButtonType.Medium.Secondary` | Gray100 채움, Gray500 테두리, Gray900 글자 |

디자인 시스템에 이미 있는 두 타입이 디자인 `C-103-Error`의 두 버튼과 그대로 맞는다.
**새 컴포넌트를 만들지 않는다.**

세로 간격은 아래와 같다. 아이콘·문구·버튼의 틈이 서로 달라 **바깥 `Column`에 균일 배치를 쓰지 않고
명시적 `Spacer`로 둔다.** 균일 배치를 남기면 스페이서 위아래로 값이 한 번 더 붙어 틈이 벌어진다.

| 자리 | 토큰 | 값 |
|---|---|---|
| 아이콘 → 문구 블록 | `gap3` | 8dp |
| 제목 → 설명 | `gap1` | 2dp |
| 설명 → 버튼 블록 | `gap7` | 24dp |
| 버튼 → 버튼 | `gap3` | 8dp |

⚠️ 네 값 모두 기존 토큰으로 떨어져 새 상수를 만들지 않는다.

**두 버튼의 폭은 디자인 실측값으로 같게 고정한다.** `YGButton`은 `modifier`를 주지 않으면 자기
텍스트 폭으로 감싸므로 글자 수가 다른 두 버튼이 계단처럼 어긋난다. 감싸는 `Column`에 폭을 한 번
주고 각 버튼이 `Modifier.fillMaxWidth()`로 그것을 채운다. 값을 한 곳에만 두어 문구가 바뀌어도
버튼 폭은 디자인을 따르게 한다.

아이콘·제목·설명의 배치와 닫기 버튼(`YGFloatingBarClose`)은 건드리지 않는다.

## 파일 구성

| 파일 | 변경 |
|---|---|
| `SegmentationViewModel.kt` | `SegmentationErrorKind` 삭제, `errorKind` → `isError`, 디코드 실패를 `GoBack`으로, `UseOriginal` 인텐트와 `useOriginal()` 추가, 원본 `BitmapWrapper` 보관 |
| `SegmentationRoute.kt` | `titleRes()`·`descriptionRes()` 삭제, `GoBack` 수신, `onClickUseOriginal` 결선 |
| `SegmentationScreen.kt` | KDoc이 `[SegmentationState.errorKind]`를 가리킨다. 링크만 고친다 |
| `SegmentationErrorScreen.kt` | 제목·설명 파라미터 제거, `Medium.Secondary` 버튼 추가, 시안 경고 KDoc 정리 |
| `strings.xml` | 문구 통합, 모듈 실패 문구 2건 삭제, 「편집 없이 사용」 라벨 추가 |
| `SaveBitmapUseCase.kt`(구 `SaveEditedImageUseCase.kt`) · `ImageSegmentationRepository.kt` · `ImageSegmentationRepositoryImpl.kt` · `ToppingEditViewModel.kt` | `saveBitmap` 으로 이름을 바꾼다 |
| `SegmentationViewModelTest.kt` | `errorKind` 를 읽는 6곳 전환 + 신규 3건 |

data 계층과 도메인 모델은 바뀌지 않는다. `persistSubject`·`SegmentationCandidate`·모듈 설치기는
그대로다.

## 테스트

`SegmentationViewModelTest`에서 `errorKind` 를 읽는 6곳을 `isError`로 바꾼다. 그중 둘은 단순 치환이 아니다.

| 기존 테스트 | 어떻게 바뀌는가 |
|---|---|
| `init_decodeFails_tellsTheUserWithoutSegmenting` | **의미가 뒤집힌다.** 실패 화면 단언과 `expectNoEvents()`를 걷고 `GoBack` 1건 · `isError == false` · `segmentImage` 호출 0회를 단언한다 |
| `init_moduleNotReady_marksModuleError` | 이름과 단언이 바뀐다. 모듈 실패도 대상 못 찾음과 **같은 `isError`로 들어간다**는 것을 잠근다. 지우지 않는 이유는 그 통합이 이 스펙의 결정이기 때문이다 |

신규 3건을 더한다.

1. `useOriginal_savesOnceAndGoesToConfirm` — `saveBitmap` 호출이 **1회**이고 `record`와
   `GoToConfirm`이 같은 경로를 두 자리에 싣는다. 저장을 두 번 하지 않는다는 것이 이 설계의
   핵심이라 여기서 잠근다.
2. `useOriginal_saveFails_showsToastAndStaysOnErrorScreen` — `ShowError`가 뜨고 `isError`가
   그대로 참이며 로딩이 걷힌다.
3. `useOriginal_pressedTwiceWhileRunning_runsOnce` — 키 가드가 두 번째 누름을 버린다.

계측 테스트는 건드리지 않는다.

**테스트하지 않는 것**: 원본 사용으로 만든 토핑이 캔버스에 실제로 어떻게 보이는지는 단위 테스트가
말할 수 없다. 저장 이후 경로가 후보 선택과 같으므로 기존 커버리지가 그대로 적용된다.

## 주의 / 열린 질문

- ⚠️ **원본 토핑은 배경이 남은 불투명한 직사각형이다.** 위키 [[토핑]]은 토핑을 "누끼 사진 객체"로
  정의하는데, 이 경로가 만드는 토핑은 그 정의에 들어맞지 않는다. 디자인이 명시적으로 요구한
  선택지라 구현은 따르되, 정책 문서와의 간격은 남는다. 캔버스에서 사각형 토핑을 다르게 취급할지는
  이 스펙의 범위 밖이다.
- ⚠️ **[segmentation-module-install](2026-09-02-segmentation-module-install.md)의 두 절이
  이 스펙으로 뒤집힌다.** 「화면 상태」 절의 원인별 문구 분기와 「재시도」 절의 시안 표기다.
  **전면 대체가 아니다** — 그 스펙의 설치 대기·공유 대기·종료 판정·예외 매핑은 그대로 살아 있다.
  구 스펙에는 이 스펙을 가리키는 📌 표기만 더하고 `superseded_by`는 비워 둔다.
- ⚠️ **모듈 실패 문구가 사라지면서 "네트워크 상태를 확인하라"는 안내도 사라진다.** 모듈을 못 받는
  원인이 네트워크인 경우 사용자는 그 사실을 화면에서 알 수 없다. 대신 「편집 없이 사용」이 있어
  막히지는 않는다. 디자인의 결정을 따른다.
- ⚠️ **확인 화면에서 뒤로 돌아와 재시도를 누르면 방금 저장한 원본 파일이 지워진다.**
  `loadCandidates()`가 첫 줄에서 `clearSegmentationCache()`를 부르기 때문이다. 후보 선택 경로에도
  이미 있는 성질이라 이 스펙이 만든 결함은 아니지만, 「편집 없이 사용」이 그 경로를 하나 늘린다.
  초안이 가리키는 파일이 사라지므로 확인 화면은 초안 없음으로 잠긴다. 이 스펙의 범위 밖이다.
- ✅ **OQ-P-153 ④(원본 사용 옵션을 살릴지)가 이 스펙으로 닫힌다.** ③에 붙어 있던 "디자인에는
  버튼이 없다"는 단서도 함께 걷힌다.
- ⚠️ **OQ-P-344(모듈이 끝내 안 오는 기기)의 성격이 바뀐다.** 여전히 앱이 고칠 수 없는 문제지만,
  그 기기의 사용자에게 토핑을 만들 경로가 생겼다. 미결로 남기되 심각도는 내려간다.
