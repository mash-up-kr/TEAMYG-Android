---
id: segmentation-retry-recovery
title: 세그멘테이션 재시도 회복 — 입력 전처리 사다리와 좌표 역변환 (Segmentation retry recovery)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-10
related_code:
  - ImageSegmentationRepositoryImpl.kt#segmentImage
  - ImageSegmentationRepositoryImpl.kt#segmentForeground
  - ImageSegmentationRepositoryImpl.kt#runSegmenter
  - ImageSegmentationRepositoryImpl.kt#recoverCandidates
  - ImageSegmentationRepositoryImpl.kt#runRecoveryLadder
  - ImageSegmentationRepositoryImpl.kt#runStage
  - SegmentationCandidateHarvest.kt#harvestSubjects
  - SegmentationCandidateHarvest.kt#harvestForeground
  - SegmentationCandidateHarvest.kt#PlateSource
  - SegmentationRecoveryNormalizer.kt#normalizeForDetection
  - RecoverCandidatesUseCase.kt#RecoverCandidatesUseCase
  - SegmentationViewModel.kt#recover
  - ImageSegmentationRepository.kt#segmentImage
  - SegmentationMask.kt#maskSubjectAlpha
  - SegmentationMask.kt#confidenceToAlpha
  - AlphaPostProcessor.kt#postProcessAlpha
  - SegmentationCandidateFilter.kt#filterCandidates
  - SubjectCoverage.kt#floorPixels
  - SegmentationException.kt#ModuleNotReady
  - SegmentationModuleInstaller.kt#ensureInstalled
  - SegmentationViewModel.kt#loadCandidates
  - SegmentationViewModel.kt#SegmentationIntent
  - SegmentationErrorScreen.kt#SegmentationErrorScreen
  - UploadImagePlan.kt#of
  - AlphaPostProcessor.kt#AlphaPostProcessOptions
  - AlphaComponents.kt#applyAreaOpening
  - UploadImagePreprocessorImpl.kt#prepare
  - ImageSegmentationRepositoryImpl.kt#persistSubject
  - SegmentationViewModel.kt#editManually
  - BaseViewModel.kt#launch
  - SegmentationBounds.kt#SegmentationBounds
  - SegmentationRecoveryPlan.kt#isLongSideCapped
  - SegmentationMask.kt#projectAlpha
related_adr:
  - 0012-mlkit-subject-segmentation.md
related_spec:
  - 2026-08-23-segmentation-preprocessing.md
  - 2026-09-05-c103-error-use-original.md
  - 2026-09-02-segmentation-module-install.md
  - 2026-08-23-c103-multi-subject-selection.md
related_architecture:
  - data-layer.md
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, c103, retry]
---

# Spec: 세그멘테이션 재시도 회복

> ✅ **as-built(2026-09-10)**: 브랜치 `feature/#486-segmentation-error-case`, 커밋 `d55880fb0`~`3217e62f7`(15개).
> 전체 검증 통과: `:domain:test` 133건, `:data:testDebugUnitTest` 534건, `:feature:segmentation:impl:testDebugUnitTest`
> 74건, 실패 0. `ktlintCheck`와 `:app:assembleDebug`도 통과했다. 신규 유닛은 52건이다. 이 검증 뒤에 붙은 커밋은 넷이다.
> `0735aebf6`·`c7d7e3863`은 선언 위치만 옮긴 정리이고 `9da7187dc`는 주석 정리라서 `:data:testDebugUnitTest`,
> `:data` ktlint, `:app:assembleDebug`만 다시 돌렸고 모두 통과했다. `3217e62f7`은 실패 화면의 「편집 없이 사용」을
> 「직접 편집」(C-104 직행)으로 바꾼 변경이며, `:feature:segmentation:impl:testDebugUnitTest` 75건(실패 0), 그 모듈의
> ktlint, `:app:assembleDebug`가 통과했다.
>
> ✅ **develop 머지(2026-09-10, PR #487 `95b7fc4d5`)**: 머지 트리가 브랜치 팁 `3217e62f7`과 같다(충돌 해소 편집 0건).
> develop에서 다시 센 유닛은 `:domain` 133건, `:data` 534건, `:feature:segmentation:impl` 75건이고, 앱 전체로는
> 1229건에서 1282건이 됐다(+53). 아래 API 절의 선언은 develop 코드와 맞는다. 다만 frontmatter `related_code`가
> 수확 코드를 옮기기 전의 이름(`toCandidatePairs`·`buildCandidatePair`·`postProcess`·`toForegroundCandidate`·
> `originalCandidate`)을 들고 있어서, 머지 점검에서 현행 이름으로 바꿨다.
>
> ⚠️ **철회 조건인 단계 로그가 운영에서 모이지 않는다.** `repositoryLogger`의 출력처가 Kermit `platformLogWriter`
> 하나뿐이라 로그가 logcat에만 남는다(OQ-P-399). 아래 「다음 라운드로 미룬 항목」과 실기기 확인 두 건은
> OQ-P-400이 추적한다.
>
> ⚠️ **회복 경로는 실기기에서 한 번도 돌지 않았다.** 강제 수단을 넣지 않기로 한 설계를 그대로 지켰다. 실패 사진이
> 생기면 아래 「주의 / 열린 질문」의 항목으로 확인한다.
>
> ⚠️ **1차 경로 실기기 회귀 확인(계획 Task 8 Step 2)도 사용자 확인 대기다.** 에이전트가 실행할 수 없는 단계이기
> 때문이다.
>
> **계획과 갈린 자리**: `harvestForeground`의 `confidenceToAlphaArray` 호출을 OOM 가드 안으로 옮겼다(계획
> 누락, 안 옮기면 1차 경로가 OOM에서 크래시한다). 다중 subject 단계 로그에도 소요를 넣었다. 1차 경로도 공용
> `multipleSubjectOptions()`/`foregroundOptions()`를 쓰도록 통합해 중복 인라인 빌더를 지웠다. 사다리 단계 이탈
> 경로(비-`ModuleNotReady` 실패, 모듈 중단, 전경 마스크 실패)에도 단계 로그를 남겼다. **최종 리뷰가 더한 수정**:
> `capped` 판정을 `targetSize.width < source.width`(계획 원안, 하한·상한이 충돌하면 틀렸다)에서 `isLongSideCapped`로
> 바꿨다. 목표·상한 로그를 판 생성 성공 이전으로 옮겨 실패해도 남게 했다. `runSegmenter`가 `CancellationException`을
> 다시 던지게 고쳤다. 재표본 후 자르기 중복을 `projectAlpha` 헬퍼로 합쳤다. 마스크 길이 불일치 경고 로그를
> 추가했다. **구현 뒤 정리**: 좌표·단계 타입 여섯과 `DetectionPlate`·`MaskedAlpha`를 `data/model/image/`에 파일
> 하나씩으로 옮겼고, 상수는 `SegmentationRecoverySpec`·`SegmentationContrastSpec`·`SegmentationMaskSpec` object로
> 묶었다. 한때 `SegmentationRecoveryNormalizer.kt`에 달았던 ktlint 파일명 억제는 `DetectionPlate`가 빠지면서 지웠다.
>
> **기록해 둔 결정**: (a) 사다리 30초 상한을 넘기면 빈 성공으로 접히고 ViewModel의 끈적한 플래그가 서므로 그
> 사진은 사다리를 다시 돌지 않는다. (b) `SegmentationModuleInstaller.INSTALL_TIMEOUT_MS`(20초)는 30초 사다리
> 예산 안에서 소비될 수 있다. 스펙 4-1이 이 값을 선례로 인용하지만 두 값은 다르다. (c) `capped` 정의 정정은
> 바로 위 항목과 같다.
>
> **다음 라운드로 미룬 항목**: 단계 로그가 2단계 크롭의 소스 치수를 "원본"으로 표기한다(크롭 퍼센트 로그로
> 복구는 가능하다). `focusStage`의 2048x1536/69% 경계 픽스처를 아직 안 넣었다. 예외 경로에서 소유 중간
> 비트맵을 회수하지 않는다(GC가 회수하며 OOM 경로 피크에만 영향). `maskSubjectAlpha`에 실사용 호출자가 없다.

> ⚠️ **이 스펙은 검수를 네 번 거쳤다.** 설계 검수 2회(사실 대조·설계 공격)가 초안의 치명 결함 여섯을
> 잡아 전면 개정했고, 구현 계획 검수 2회(실행 가능성·테스트 정합)가 설계 결함을 더 잡아 한 번 더
> 고쳤다. 무엇이 왜 뒤집혔는지는 맨 아래 「검수 이력」에 있다.

## 목표

`C-103-Error`의 「다시 시도」가 **실제로 다른 결과를 낼 수 있게** 만든다. 지금 그 버튼은
후보 0건으로 실패한 사진에서 같은 입력으로 같은 파이프라인을 다시 돌기 때문에 항상 같은
빈 목록을 돌려준다.

대응 이슈는 `#486`이다.

## 왜 지금인가

`SegmentationViewModel#loadCandidates`가 `isError`를 참으로 만드는 자리는 둘이다. 세그멘테이션이
`Result.failure`로 끝난 경우와, 성공했는데 후보 목록이 빈 경우다. 이 둘의 재시도 성질이 다르다.

- **`ModuleNotReady`** — `SegmentationModuleInstaller#ensureInstalled`가 매번 가용 여부를 새로
  묻고 끝난 대기를 재사용하지 않으므로, 설치가 끝나면 재시도가 성공한다.
  ⚠️ 설치 대기 상한을 넘긴 경우도 여기로 접힌다. `ModuleInstallOutcome.TimedOut`은 installer
  층의 값이고, `runSegmenter`가 `Ready`가 아닌 결과를 전부 `ModuleNotReady`로 바꿔 올린다.
  **ViewModel이 볼 수 있는 모듈 관련 예외는 `ModuleNotReady` 하나다.**
- **후보 0건** — `ImageSegmentationRepositoryImpl#segmentImage`가 다중 subject와 전경 마스크
  폴백을 모두 거친 뒤의 결과다. 우리 코드 경로에는 무작위 요소가 없다. **같은 사진으로 몇 번을
  눌러도 같은 빈 목록이다.**
  ⚠️ ML Kit 추론 자체의 결정성은 코드로 확인할 수 없다. 이 서술의 근거는 우리 코드까지다.

[segmentation-preprocessing](../2026-08-23-segmentation-preprocessing.md)이 촬영과 디코드 쪽 입력
품질을 다뤘고, 대비·감마 정규화와 후처리 전반을 "문서 근거가 없다"는 이유로 다음 라운드로 밀었다.
이 스펙이 그 다음 라운드에 해당하되, **적용 지점을 전역이 아니라 재시도 경로로 좁힌다.**

## 범위

**포함**

- 재시도를 직전 실패의 성격과 **이 사진으로 사다리를 이미 돌렸는지**로 가른다.
- 저장소에 회복 경로 `recoverCandidates`를 더한다. 전처리 두 단계를 순서대로 시도하고 첫 성공에서 멈춘다.
- 검출 공간과 원본 공간을 가르는 좌표 변환을 도입한다. **후보의 픽셀은 언제나 원본에서 오려낸다.**
- 후처리 유틸을 「신뢰도→알파」와 「알파 후처리」로 쪼갠다. 전자는 검출 공간, 후자는 원본 공간에서 돈다.
- 후보 수확 코드를 저장소 구현에서 별도 파일로 옮기고, **판의 출처를 타입으로 갈라** 1차 경로와 회복
  경로가 같은 subject 루프를 쓴다.
- 사다리 전체에 대기 상한을 걸고, 단계 사이와 픽셀 루프에서 취소를 확인한다.
- 단계별 결과를 로그로 남겨 조건부 항목의 철회 근거를 모은다. **「필터를 완화했다면 살았을 후보 수」도
  로그로만 남긴다.**

**제외**

- **실패 문구의 원인별 분기** — [c103-error-use-original](2026-09-05-c103-error-use-original.md)이
  `SegmentationErrorKind`를 걷어내고 한 벌로 통합했다. 이 스펙의 분기는 ViewModel 내부에만 있고
  화면 문구는 전과 똑같다.
- **필터 하한 완화를 판정에 적용하는 것** — 후보의 캔버스 치수가 언제나 원본이라 `SubjectCoverage` 하한도
  원본 면적 기준이고, 그래서 2단계가 크롭 안에서 찾은 작은 피사체가 걸러질 수 있다. 그러나
  `ToppingEditViewModel`이 저장할 때 **같은 엄격 하한**으로 판정하므로, 완화 하한으로 통과한 후보를 골라
  「사진 편집」에 들어가면 손대지 않고 저장해도 `SubjectTooSmall`로 막힌다. `SubjectCoverage`의 KDoc이
  바로 이 불일치를 금지한다. **이번에는 완화했다면 살았을 후보 수만 로그로 남기고, 판정은 1차와 같다.**
- **「편집 없이 사용」(2026-09-10부터 「직접 편집」) 진입 시 사다리 취소** — 그 버튼은 `SegmentationErrorScreen`에만 있다. 회복이 도는
  동안에는 `isError`가 거짓이라 에러 화면이 그려지지 않으므로 **UI에서 도달할 경로가 없다.** 넣으면 죽은
  코드이고, 경합으로 도달하면 에러도 후보도 없는 막다른 화면을 남긴다.
- **회복 경로를 강제로 태우는 디버그 수단** — 검출이 안 되는 실제 사진이 없어 실기기에서 회복 경로를
  태울 방법이 없다. 스캐폴딩을 넣지 않기로 했다. **그래서 병합 시점에 회복 경로는 실기기에서 한 번도
  돌지 않은 상태다.** 실패 사진이 생기면 그때 확인한다.
- **1차 경로의 필터 임계 변경** — 이 스펙과 무관한 회귀가 난다.
- **`decodeImage` 전역 정규화** — 확대판이 그대로 후보가 되면 알맹이 PNG의 인트린식 치수가 커지고
  그 값이 배치 초기 크기를 거쳐 서버 `scale`로 굳는다(OQ-P-282). 회복 경로는 원본에서 오려내므로
  그 전파가 아예 없다. [segmentation-preprocessing](../2026-08-23-segmentation-preprocessing.md)의
  「짧은 변 512 하한 확대」 항목은 **미착수로 남고, 이 스펙이 재시도 경로에 한해 그 자리를 대신한다.**
- **사다리 3단계 이상** — 진짜 탈출구는 실패 화면의 「직접 편집」이다. 2026-09-10에 「편집 없이 사용」을 대신해 C-104로 바로 간다.
- **단계별 진행 표시** — 문구를 한 벌로 통합한 결정과 같은 이유다.
- **고정 사진 세트** — 합성 사진으로 검증하면 변환 자체를 증명하는 순환 논증이 되기 쉽다.

## 설계

### 1. 재시도를 가른다

지금 `SegmentationIntent.Retry`는 `loadCandidates`를 통째로 다시 돈다. 여기에 사다리를 그냥 얹으면
1차 몫까지 겹쳐 낭비가 크다. 반대로 직전 실패만 보고 가르면 **회복까지 실패한 뒤 1차에서 다시 0건이
나올 때 「회복 전 0건」으로 덮여서, 사다리가 한 번 걸러 되풀이된다.**

그래서 ViewModel이 두 값을 든다. 직전 실패(`EXCEPTION` / `EMPTY`)와, **이 사진으로 사다리를 끝까지
돌렸는지를 뜻하는 끈적한 플래그**다. 플래그는 1차 결과로 되돌리지 않는다.

| 직전 실패 | 사다리를 돌렸나 | 재시도 동작 | 추론 |
|---|---|---|---|
| 예외 | 무관 | 현행 `loadCandidates` | 최대 2회 |
| 0건 | 아니오 | 회복 경로 | 최대 4회 |
| 0건 | 예 | 현행 `loadCandidates` | 최대 2회 |

같은 사진으로 사다리는 **한 번만** 돈다. 입력이 같으면 결과도 같기 때문이다. 사다리가 `ModuleNotReady`로
중간에 접힌 경우는 끝까지 돈 것이 아니므로 플래그를 세우지 않는다 — 모듈이 돌아오면 다시 돌 기회가 남는다.

세 번째 줄이 디코드부터 다시 도는 것은 낭비가 아니다. 디코드를 다시 하면 URI 만료나 손상 같은 다른
결과가 나올 여지가 있고, 버튼이 계속 살아 있는 편이 낫다.

원본 비트맵은 `SegmentationViewModel`이 `originBitmapWrapper`로 이미 들고 있다. 회복 경로는 그것을
넘겨받으므로 디코드를 다시 하지 않는다.

### 2. 사다리 두 단계

`recoverCandidates`가 원본을 받아 순서대로 시도하고, 후보가 하나라도 나오면 그 자리에서 멈춘다.

**1단계 「정규화」** — 대비 퍼센타일 스트레치와 검출 해상도 맞춤을 적용한다. 크롭이 없으므로 좌표
역변환은 스케일뿐이다.

**1단계 무동작 가드** — 목표 치수가 원본과 같고 대비도 안 거는 경우 1단계는 1차 경로의 재실행이 된다.
그런 경우 1단계를 건너뛴다. ⚠️ **대비를 켜 둔 동안에는 이 가드가 걸리지 않는다.** 대비 스트레치는
조건부 항목이라, 로그가 그것을 철회하면 그때부터 가드가 의미를 갖는다. 대비는 저장소의 상수 하나로
켜고 끈다.

**2단계 「집중」** — 1단계가 남긴 힌트 사각형으로 크롭하고 같은 정규화를 건다. 역변환은 오프셋과 스케일이다.

**수축 가드** — 크롭 면적이 원본의 70% 이상이면 2단계를 건너뛴다. 힌트 픽셀이 흩어져 있으면 여유를
붙인 크롭이 원본과 같아지고, 그러면 2단계는 1단계 재탕이다. **판정은 정수로 한다** — 부동소수 비율로
비교하면 정확히 70%인 경계가 반올림 오차로 흔들린다.

각 단계는 기존과 같은 두 겹으로 돈다. 다중 subject 옵션으로 한 번, 후보가 0건이면 전경 마스크
옵션으로 한 번이다. 두 옵션을 한 요청에 못 싣는 제약(`segmentForeground`의 KDoc이 적어 둔
`SIGSEGV`)이 그대로 적용된다.

### 3. 힌트

**힌트 하한은 폴백 이진화와 같은 축(`AlphaPostProcessOptions.binaryThreshold`)을 쓴다.** 더 높은 축으로
찾으면 1단계가 실패한 상황에서 힌트가 구조적으로 거의 항상 빈다. 힌트는 임계를 **초과**하는 픽셀을
감싸는 사각형이다.

**힌트는 단계 안에서 좌표 넷으로 뽑아 들고 나오고, 그 단계의 ML Kit 결과는 놓는다.** 결과를 2단계까지
붙들면 피크가 커지고, 네이티브 신뢰도 버퍼가 새 세그멘터를 연 뒤에도 유효한지에 기대게 된다.

힌트가 없으면 **짧은 변의 70%를 한 변으로 하는 중앙 정사각형**으로 크롭한다.

**1차 경로에서는 힌트를 계산하지 않는다.** 원본 전체를 한 번 더 훑는 비용을 1차에 얹지 않는다.

### 4. 검출 해상도

**검출 입력에 긴 변 상한을 건다.** 근거는 **자원**이다. 큰 판을 네 번 추론하는 대신 작은 판을 돌아
시간과 메모리 피크를 줄인다. 짧은 변 하한과 긴 변 상한이 한 배율 계산에 들어가므로 두 축은 언제나 같은
방향으로 움직이고, 극단 종횡비에서 픽셀이 폭증하는 문제도 상한 쪽에서 막힌다.

⚠️ **"되올리니까 검출률이 안 떨어진다"는 논증은 틀렸다.** 되올림이 회복하는 것은 마스크 정밀도이고,
축소가 없애는 것은 검출기가 보는 정보량이다. 상한이 실제로 걸렸는지를 로그로 남겨 회귀를 감시한다.

하한과 상한이 충돌하면(극단 종횡비) **상한이 이긴다.**

### 4-1. 잠정 초기값

**전부 잠정이다.** 로그가 판정하기 전까지의 출발점이다.

| 상수 | 잠정값 | 근거 |
|---|---|---|
| 검출 짧은 변 하한 | 512 | ML Kit Android 가이드가 "at least 512x512"를 적는다 |
| 검출 긴 변 상한 | 2048 | 하한의 네 배. 문서 근거는 없고 자원 쪽에서 정한다 |
| 대비 퍼센타일 | 하위 1% · 상위 99% | 이상치만 자르고 본체는 보존하는 통상값 |
| 대비 적용 | 켠다 | 조건부 항목이다. 상수 하나로 끈다 |
| 힌트 신뢰도 하한 | `AlphaPostProcessOptions.binaryThreshold` | 폴백 성분 판정과 같은 축을 써야 힌트가 안 빈다 |
| 힌트 크롭 여유 | 힌트 사각형 각 변의 20% | 누끼 Safe Margin 정책과 같은 비율. ⚠️ 그 정책은 아직 코드에 없다(OQ-P-150) |
| 수축 가드 | 크롭 면적이 원본의 70% 이상이면 2단계를 건너뛴다 | 크롭이 원본과 같아지는 경우를 막는다 |
| 중앙 폴백 크롭 | 짧은 변의 70%를 한 변으로 하는 정사각형 | 힌트가 없을 때만 쓴다 |
| 로그용 완화 배수 | 1차 하한의 1/4 | **판정에는 쓰지 않는다.** 완화했다면 살았을 후보 수를 세는 데만 쓴다 |
| 사다리 대기 상한 | 30초 | `SegmentationModuleInstaller.INSTALL_TIMEOUT_MS`가 선례다. 블로킹 추론 하나만큼 늦게 걸릴 수 있다 |
| 왕복 좌표 허용오차 | 각 축 1px | 테스트가 구현의 오차에 맞춰지지 않게 숫자로 고정한다 |

### 5. 좌표계와 픽셀

**손본 판은 마스크를 얻는 데만 쓰고, 알파는 후처리 전에 원본 공간으로 되올린다. 픽셀은 언제나 원본에서
읽는다.**

⚠️ **이 규칙을 어기면 결과 토핑의 색이 변한다.** 대비를 건 판의 픽셀이 후보가 되면 사용자가 고른 알맹이가
원본과 다른 색으로 저장된다.

**규칙을 타입으로 강제한다.** 수확 함수는 판의 출처를 `PlateSource`로 받는다.

- `MlKitPlate` — 1차 경로 전용. 검출 공간이 곧 원본 공간이라 ML Kit 판의 픽셀이 원본 색이다.
- `OriginRegion` — 회복 경로 전용. **픽셀 인자가 없다.** 알파만 검출 판에서 가져오고 픽셀은 수확 함수가
  원본에서 직접 읽는다. 대비 판의 픽셀이 들어올 입구가 구조적으로 없다.

어느 출처를 쓸지는 subject 루프가 **투영(`DetectionProjection`) 유무**로 정한다. 투영이 없으면 1차, 있으면
회복이다. 호출부가 출처를 고르지 않으므로 회복 경로가 `MlKitPlate`를 쓸 수 없다.

**되올림은 램프 뒤에 건다.** `confidenceToAlpha`는 램프에 버림이 붙은 비선형 변환이라 순서가 결과를 바꾼다.
신뢰도는 검출 공간에서 알파로 바꾸고, 그 알파를 원본 공간으로 되올린다. 원본 크기 `FloatBuffer`를 만드는
우회는 금지한다.

subject 하나를 원본으로 되돌리는 순서는 이렇다.

1. 검출 공간 사각형을 변환으로 원본 좌표에 옮긴다(**사상 사각형**).
2. 사상 사각형을 **크롭 사각형과 원본의 교집합**으로 자른다(**잘린 사각형**). 원본 경계만으로 자르면 2단계에서
   크롭 밖으로 새는 사각형이 생긴다. 교집합이 비면 그 subject를 버린다.
3. 검출 판의 알파를 **사상 사각형 크기로 먼저 재표본하고, 그다음 잘린 사각형으로 자른다.** 잘린 크기로
   바로 재표본하면 어긋난다.
4. 원본의 잘린 사각형에서 픽셀을 읽고 후처리한다. `guidance`도 원본의 그 영역을 읽는다.
5. 후처리 결과의 로컬 사각형에 잘린 사각형의 원점을 더해 원본 좌표 후보를 만든다.

전경 폴백도 같은 순서다. 마스크 전체를 검출 사각형으로 보고 1~5를 탄다. **5번의 오프셋을 빠뜨리면 1단계는
오프셋이 0이라 멀쩡하고 2단계만 엉뚱한 영역을 오린다.** 캔버스 검사도 통과하므로 따로 확인한다.

### 6. 되돌림 후보와 커버리지

`postProcessAlpha`는 **알파를 제자리에서 지운다**(그 함수의 KDoc). 그래서 회복 경로는 후처리 전에 알파
사본을 떠 둔다.

- **후처리가 성공하면** 지워진 알파로 합성한다. 1차와 같다.
- **후처리가 실패하거나 알파를 전멸시켜 되돌리면**, 사본 알파와 원본 픽셀을 합성해 **새 판**을 만들고
  커버리지를 사본 알파의 합으로 센다. 원본 픽셀의 알파는 JPEG에서 전부 255이므로, 그것으로 세거나 판을
  그대로 쓰면 **불투명 사각형 후보가 커버리지 만점으로 필터 정렬 1위에 오른다.**
- **알파가 안 바뀌어도** 회복 경로는 판을 새로 만든다. 1차는 ML Kit 판을 재사용해도 되지만, 회복 경로에서
  재사용할 판은 원본이다.

1차 경로의 되돌림은 지금처럼 ML Kit 판에서 커버리지를 센다. 그 판은 후처리가 지우지 않았다.

### 7. 계약 검사

기존 `require`들은 판 치수와 사각형 치수가 같은지만 보는데, 사각형을 판에서 만들기 때문에 사실상
항진명제다. **정작 깨질 수 있는 것은 사각형이 캔버스 안에 있는지이고**, 깨지면 예외가 아니라
`persistSubject`의 `Canvas.drawBitmap`이 조용히 자른다.

수확은 후보마다 사각형이 캔버스 안인지 본다. **어기면 그 후보만 버린다.** 흐름 전체를 실패시키지 않는다.

⚠️ 1차 경로에도 같은 검사가 걸린다. 지금까지 캔버스 밖으로 새던 ML Kit 후보는 저장 시 잘렸고, 이제는
후보에서 빠진다. 의도한 동작 변경이다.

### 8. 다중 후보와 필터

**1차 경로와 회복 경로가 같은 subject 루프를 쓴다.** 판 있음 → 원본 좌표 사각형 → bbox 사전 절단 →
면적 내림차순 정렬 → 후처리 상한 절단 → 수확 순서다. 사전 절단은 **원본 좌표 사각형 면적**을 원본 면적
기준 엄격 하한과 비교한다. 1차는 두 좌표계가 같으므로 지금과 똑같다.

회복 경로도 수확 뒤 `filterCandidates`를 **기본 하한으로** 건다. 폴백 후보는 1차와 같이 필터를 거치지 않는다.
[c103-multi-subject-selection](2026-08-23-c103-multi-subject-selection.md)이 정의한 선택 UX가
재시도 전후로 같다.

로그에는 사전 절단과 필터 각각에서 **엄격 하한 통과 수와 1/4 하한이었다면 통과했을 수**를 함께 남긴다.
뒤의 값은 근사다 — 사전 절단에서 떨어진 subject는 후처리를 안 거쳤으므로 필터 단계의 수에 들어가지 않는다.

## API / 인터페이스

```kotlin
// domain
interface ImageSegmentationRepository {
    /**
     * [segmentImage] 가 후보를 하나도 못 낸 뒤에만 부른다. 입력을 손봐 가며 다시 찾는다.
     * 후보의 픽셀은 언제나 [bitmapWrapper] 에서 오려낸다 — 손본 판은 검출에만 쓴다.
     */
    suspend fun recoverCandidates(bitmapWrapper: BitmapWrapper): Result<List<SegmentationCandidate>>
}

// data/model/image/ — 선언 하나에 파일 하나
internal data class DetectionBounds(val left: Int, val top: Int, val right: Int, val bottom: Int)
internal data class ScaledSize(val width: Int, val height: Int)
internal data class RecoveryTransform(val scaleX: Float, val scaleY: Float, val offsetX: Int, val offsetY: Int) {
    fun toOrigin(bounds: DetectionBounds): SegmentationBounds
}
internal data class RecoveryStage(
    val cropRect: SegmentationBounds?,
    val targetSize: ScaledSize,
    val applyContrast: Boolean,
    val transform: RecoveryTransform,
)
/** 검출 공간이 원본에 어떻게 놓이는가. 1차 경로에는 없다 */
internal data class DetectionProjection(val transform: RecoveryTransform, val clip: SegmentationBounds)
/** 재표본은 [mapped] 크기로, 그다음 [clipped] 로 자른다 */
internal data class ProjectedRegion(val mapped: SegmentationBounds, val clipped: SegmentationBounds)
/** [ownedByUs] 가 거짓이면 원본이다. 쓰지도 회수하지도 않는다 */
internal class DetectionPlate(val bitmap: Bitmap, val ownedByUs: Boolean)
internal class MaskedAlpha(val alpha: ByteArray, val result: AlphaPostProcessResult)
/** 4-1 잠정값. 짧은 변 하한·긴 변 상한·왕복 허용오차·힌트 여유·수축 가드·중앙 폴백 비율 */
internal object SegmentationRecoverySpec
/** 휘도 단계 수와 퍼센타일 절단점 */
internal object SegmentationContrastSpec
/** 신뢰도→알파 램프의 바닥·천장과 불투명 값 */
internal object SegmentationMaskSpec

// data/utils/image/SegmentationRecoveryPlan.kt — 전부 순수
internal fun resolveTargetSize(width: Int, height: Int): ScaledSize
internal fun normalizeStage(width: Int, height: Int, applyContrast: Boolean): RecoveryStage?
internal fun focusCrop(width: Int, height: Int, hint: DetectionBounds?, hintTransform: RecoveryTransform?): SegmentationBounds
internal fun cropAreaPercent(crop: SegmentationBounds, width: Int, height: Int): Int
internal fun focusStage(width: Int, height: Int, crop: SegmentationBounds, applyContrast: Boolean): RecoveryStage?
internal fun hintBounds(alpha: ByteArray, width: Int, height: Int, threshold: Int): DetectionBounds?
internal fun projectRegion(detection: DetectionBounds, projection: DetectionProjection, width: Int, height: Int): ProjectedRegion?
internal fun SegmentationBounds.offsetBy(dx: Int, dy: Int): SegmentationBounds
internal fun isInsideCanvas(bounds: SegmentationBounds, canvasWidth: Int, canvasHeight: Int): Boolean
/** 하한·상한이 충돌하는 극단 종횡비에서 [resolveTargetSize]가 상한에 걸렸는지. `capped` 로그가 이 함수를 쓴다 */
internal fun isLongSideCapped(width: Int, height: Int): Boolean

// data/utils/image/SegmentationContrast.kt — 순수
internal fun contrastLut(histogram: IntArray): IntArray

// data/utils/image/SegmentationMask.kt — 순수. maskSubjectAlpha 는 두 함수의 위임 껍데기로 남는다
internal fun confidenceToAlphaArray(mask: FloatBuffer, width: Int, height: Int): ByteArray
internal suspend fun postProcessMaskedAlpha(alpha: ByteArray, width: Int, height: Int, options: AlphaPostProcessOptions = AlphaPostProcessOptions(), guidance: GuidanceProvider? = null): MaskedAlpha?
internal fun resampleAlpha(alpha: ByteArray, width: Int, height: Int, targetWidth: Int, targetHeight: Int): ByteArray
internal fun cropAlpha(alpha: ByteArray, width: Int, height: Int, region: SegmentationBounds): ByteArray
internal fun alphaSum(alpha: ByteArray): Long
/** 사상 사각형 크기로 재표본한 뒤 잘린 사각형으로 자른다(§5 순서 3). originAlphaOf/placeForegroundAlpha가 공유하는 헬퍼다 */
internal fun projectAlpha(alpha: ByteArray, width: Int, height: Int, projected: ProjectedRegion): ByteArray

// data/utils/image/SegmentationCandidateHarvest.kt — Bitmap 실행
internal sealed interface PlateSource {
    class MlKitPlate(val plate: Bitmap, val region: SegmentationBounds) : PlateSource
    class OriginRegion(val detectionPlate: Bitmap, val projected: ProjectedRegion) : PlateSource
}
internal class HarvestedCandidate(val candidate: SegmentationCandidate, val reverted: Boolean)
internal class ForegroundHarvest(val candidates: List<SegmentationCandidate>, val hint: DetectionBounds?)

internal suspend fun harvestSubjects(subjects: List<Subject>, origin: Bitmap, projection: DetectionProjection?): List<HarvestedCandidate>
internal suspend fun harvestForeground(mask: FloatBuffer, maskWidth: Int, maskHeight: Int, origin: Bitmap, projection: DetectionProjection?, hintThreshold: Int?): ForegroundHarvest

// data/utils/image/SegmentationRecoveryNormalizer.kt — Bitmap 실행
internal suspend fun normalizeForDetection(origin: Bitmap, stage: RecoveryStage): DetectionPlate
```

## 동작 / 상태

`SegmentationState`에 새 필드를 넣지 않는다. 직전 실패와 사다리 플래그는 ViewModel의 `private var`로만 든다.

**회복 분기도 상태 시퀀스를 지킨다.** 시작할 때 `isLoading`을 켜고 `isError`를 끈다. `isError`를 안 끄면
Route가 `SegmentationErrorScreen`을 계속 그리고 `YGScaffoldV2`가 그 위에 로딩 덮개를 얹는다. 끝날 때는
성공·실패와 무관하게 `isLoading`을 끈다. **예상 못 한 예외(`launch`의 `onError`)에서도 `isError`를 되돌린다** —
안 그러면 에러도 후보도 없는 화면에 갇힌다.

사다리는 `LOAD_CANDIDATES_KEY`를 그대로 쓴다. 진입과 재시도가 같은 키를 쓰는 현재 구조가 연타를 막는다.

**취소** — `launch`는 `viewModelScope`라 ViewModel이 정리될 때 함께 취소된다. 사다리는 단계 사이와 픽셀
루프의 행 경계에서 취소를 확인한다. ⚠️ `runSegmenter`는 `Tasks.await` 블로킹 대기라 추론 도중에는 끊기지
않는다. 대기 상한과 취소는 **진행 중인 추론 하나가 끝난 뒤에** 걸린다.

**디스패처** — 픽셀을 만지는 작업은 `Dispatchers.Default`에서 돈다. 1차 경로가 수확을 감싸는 방식과 같다.
`recoverCandidates`는 `viewModelScope`(Main)에서 불리므로 감싸지 않으면 메인 스레드에서 돈다.

## 파일 구성

| 파일 | 역할 | 성격 |
|---|---|---|
| `data/model/image/*.kt` | 신설. 좌표·단계 타입 여섯, `DetectionPlate`, `MaskedAlpha`(이동), 상수 object 셋. 파일 하나에 선언 하나 | 순수 |
| `data/utils/image/SegmentationRecoveryPlan.kt` | 신설. 해상도 목표, 두 단계 계획과 가드, 힌트, 투영과 교집합, 캔버스 검사 | 순수 |
| `data/utils/image/SegmentationContrast.kt` | 신설. 퍼센타일 절단 LUT | 순수 |
| `data/utils/image/SegmentationMask.kt` | 램프와 후처리 분리, 재표본·자르기·알파 합 추가 | 순수(기존) |
| `data/utils/image/SegmentationCandidateHarvest.kt` | 신설. 수확 코드 이동, `PlateSource`로 출처 분리, 공통 subject 루프, 전경 수확 | `Bitmap` 실행 |
| `data/utils/image/SegmentationRecoveryNormalizer.kt` | 신설. 크롭·축소·대비 적용, 판 소유권 | `Bitmap` 실행 |
| `data/repository/image/ImageSegmentationRepositoryImpl.kt` | `recoverCandidates`와 사다리 추가, 수확 코드 제거, 폴백의 `ModuleNotReady` 승격 | 조합 |
| `domain/repository/image/ImageSegmentationRepository.kt` | `recoverCandidates` 선언 | 계약 |
| `domain/usecase/image/RecoverCandidatesUseCase.kt` | 신설 | 계약 |
| `feature/segmentation/impl/.../SegmentationViewModel.kt` | 직전 실패와 사다리 플래그, 재시도 분기, 상태 시퀀스 | 상태 |

## 에러 처리

새 실패 표현을 만들지 않는다. `recoverCandidates`도 `Result<List<SegmentationCandidate>>`를 돌려주고,
전 단계가 실패하면 `Result.success(emptyList())`다.

- **`ModuleNotReady`가 나오면 즉시 중단**하고 그 실패를 그대로 올린다. 다중 subject 갈래와 전경 폴백 갈래
  **양쪽 모두** 이 규칙을 따른다.
- **그 밖의 예외와 `OutOfMemoryError`는 그 단계만 포기**하고 다음 단계로 넘어간다.
- **후보 하나의 실패는 그 후보만 버린다.** 캔버스 검사 위반, 픽셀 할당 OOM, 되돌림 판 합성 실패가 여기에 든다.
- **`CancellationException`은 다시 던진다.**
- **픽셀 배열 할당은 OOM 가드 안에 둔다.** `buildCandidatePair`의 KDoc이 경고하듯 12MP 후보에서 OOM이 가장
  잘 나는 자리가 그 할당이다. 호출부로 빼면 `Exception`만 잡는 바깥에서 앱이 죽는다.

**1차 경로 폴백의 분류는 바꾸지 않는다.** 전경 폴백이 `Result`를 올리되 `ModuleNotReady`만 실패로 올리고, 그
밖의 추론 실패는 지금처럼 「인식된 대상 없음」(빈 목록)으로 접는다. 모두 실패로 올리면 1차의 `Process` 실패가
빈 목록에서 예외로 분류가 바뀌어 그 사진의 재시도가 회복 경로로 가지 못한다.

### 판 소유권

**원본에는 쓰지도 회수하지도 않는다.** 크롭도 축소도 필요 없는 사진이면 검출 판이 곧 원본 인스턴스이고,
원본은 `decodeUriToBitmap`이 `isMutableRequired = true`로 디코드해 가변이다. 거기에 대비를 적용하면 예외 없이
원본 사진이 조용히 바뀌고, 회수하면 ViewModel이 쥔 원본이 죽는다.

`normalizeForDetection`은 `DetectionPlate(bitmap, ownedByUs)`를 돌려준다. 대비를 걸어야 하는데 판이 원본이거나
불변이면 **먼저 복사한다.** 회수는 `ownedByUs`일 때만 한다.

회복 경로의 수확은 판을 항상 새로 만든다(§6).

### 메모리 피크

앱은 `largeHeap`을 선언하지 않는다.

| 사는 것 | 언제 놓는가 |
|---|---|
| 원본 비트맵 | 놓지 않는다. ViewModel과 화면이 붙들고 있다 |
| 단계별 검출 판 | 그 단계가 끝나면 `finally`에서. `ownedByUs`일 때만 |
| 단계별 ML Kit 결과 | 그 단계가 끝나면. 힌트 사각형 하나만 남긴다 |
| 알파 사본과 후보별 픽셀 배열 | 후보 하나를 만들 때마다 |
| `persistSubject`의 원본 크기 캔버스 | 그 함수가 `finally`로 이미 회수한다 |

히스토그램은 축소가 끝난 판에서 행 단위로 모은다. 대비 LUT 적용은 픽셀 루프다 — `minSdk`가 26이라
`RenderEffect`를 못 쓰고, 임의 LUT는 `ColorMatrixColorFilter`로 표현되지 않는다.

## 테스트

판단을 순수 함수로 빼서 JVM에서 덮는다. 각 테스트는 **대응하는 틀린 구현을 넣었을 때 실패해야 한다.**

- **`RecoveryTransform.toOrigin`** — 축마다 배율이 **다른** 변환. 같은 배율 픽스처만 있으면 `scaleY` 대신
  `scaleX`를 써도 통과한다. 왕복 항등성은 각 축 1px 허용오차로 따로 둔다.
- **`resolveTargetSize`** — 하한·상한 경계, 비율 보존, **충돌 시 상한이 이긴다.**
- **`normalizeStage`** — 무동작 가드, 대비가 켜지면 널이 아니다, 크롭과 오프셋이 없다.
- **`focusCrop`** — 힌트 주변 여유, 원본 경계 클램프, **비정사각 원본의 중앙 정사각형.**
- **`focusStage`** — **정확히 70%에서 널, 그보다 작으면 널이 아니다.** 경계 픽스처가 없으면 `>=`를 `>`로 바꿔도 통과한다.
- **`hintBounds`** — 한 점, **흩어진 두 점**, 임계와 같은 값은 제외, 없으면 널.
- **`projectRegion`** — 크롭과 원본의 교집합, 사상 사각형 보존, 교집합이 비면 널.
- **`offsetBy`와 `isInsideCanvas`** — 오프셋 적용, 네 변 각각의 위반(**위쪽 음수 포함**).
- **`contrastLut`** — **양 끝 이상치가 있는 분포**에서 퍼센타일 절단. 이상치가 없으면 최소·최대 확장으로 바꿔도 통과한다.
  분모 0이면 항등, 단조, 대역 밖 클램프.
- **`resampleAlpha`** — **확대 중간값**(2에서 3으로 늘릴 때 가운데), 축소 박스 평균, 같은 크기, 길이 불일치.
- **`cropAlpha`와 `alphaSum`** — 가운데 자르기, 범위 밖 예외, 부호 없는 합.
- **재시도 분기** — 0건 뒤 회복, 예외 뒤 1차, 회복 실패 뒤 1차, **네 번 눌러도 사다리는 한 번**, 모듈로 접힌 사다리는
  다시 돌 수 있다, 회복 중 로딩과 에러 해제, 예상 못 한 예외 뒤 에러 화면 복원.

ML Kit 호출과 `Bitmap` 생성은 유닛으로 덮지 않는다. 그 자리의 규칙 둘(픽셀은 원본에서, 원본에는 쓰지 않는다)은
`PlateSource.OriginRegion`에 픽셀 인자가 없다는 것과 `DetectionPlate.ownedByUs`로 **구조적으로** 막는다.

## 근거 등급 (이 스펙의 계약)

**관찰된 실패 사례가 없는 상태에서 쓰는 스펙이다.** 검출이 안 되는 실제 사진을 아직 모으지 못했고, 회복 경로를
강제로 태우는 수단도 넣지 않았다. **병합 시점에 회복 경로는 실기기에서 돌지 않았다.**

| 항목 | 근거 | 판정 |
|---|---|---|
| 좌표 역변환과 수확 분리 | 없으면 결과 색이 변한다. 코드로 확정된다 | 무조건 |
| 판 출처의 타입 분리와 원본 비변경 | 없으면 원본 사진이 조용히 바뀐다. 코드로 확정된다 | 무조건 |
| 되돌림 후보의 알파 사본 | 없으면 불투명 사각형 후보가 1위에 오른다. 코드로 확정된다 | 무조건 |
| 캔버스 검사와 후보 단위 버림 | 없으면 저장 시점에 조용히 잘린다. 코드로 확정된다 | 무조건 |
| 재시도 분기와 끈적한 플래그 | 없으면 사다리가 한 번 걸러 되풀이된다. 코드로 확정된다 | 무조건 |
| 대기 상한과 취소 확인 | 사다리가 길다. 블로킹 추론 하나만큼 늦는 한계가 있다 | 무조건 |
| 검출 입력 긴 변 상한 | **자원 근거로만 무조건이다.** 검출률은 로그로 감시한다 | 무조건 |
| 2단계 힌트 크롭 | 상대 크기가 지배 변수라는 것은 통념이나 이 앱에서 미측정 | 조건부 |
| 1단계 대비 스트레치 | 문서 근거가 없다. 기존 스펙이 보류한 항목이다 | 조건부 |
| 짧은 변 하한 확대 | 확대는 정보를 늘리지 않는다(OQ-P-278) | 조건부 |

**판정 수단은 순수 함수 유닛 테스트와 실사용 로그다.**

**철회 조건**은 단계별 로그다. 단계마다 아래를 남긴다.

- 어느 단계가 돌았고, 가드에 걸려 건너뛰었다면 어느 가드였는지
- 힌트가 나왔는지, 크롭이 원본의 몇 퍼센트인지
- 목표 치수와 긴 변 상한이 걸렸는지
- 사전 절단과 필터 각각의 엄격 통과 수와 1/4 하한이었다면 통과했을 수
- 최종 후보 수(폴백 포함)와 소요

## 주의 / 열린 질문

- **잠정값** — 위 4-1 표의 값은 전부 잠정이다.
- **회복 경로 실기기 미검증** — 실패 사진이 생기면 확인한다. 그때 필요한 항목은 단계 로그 순서, **타임아웃 시 로그
  순서**, 로딩 중 에러 화면 비겹침, **저장된 토핑의 색이 원본과 같은지**, 회복 실패 뒤 재시도가 사다리를 다시 안
  도는지다.
- **필터 완화와 수동 편집 하한** — 로그가 완화의 수익을 보여 주더라도 회복 판정에만 완화를 넣으면 편집 저장이 막힌다.
  넣으려면 초안에 후보 출처를 싣고 `ToppingEditViewModel`의 판정까지 함께 바꿔야 한다.
- **ML Kit 추론의 결정성** — "재시도는 항상 같은 결과"의 근거는 우리 코드까지다.
- **재표본의 방식** — 축소에 박스 평균을 쓰는 것은 에일리어싱 일반론이고 이 파이프라인에서 측정하지 않았다.
- **OQ-P-278 잔존** — 확대로 정확도가 회복된다는 근거는 여전히 없다.
- **OQ-P-282 회피** — 회복 경로가 원본에서 오려내므로 토핑 초기 크기와 서버 `scale` 전파가 없다.
- **OQ-P-150 잔존** — 힌트 크롭 여유의 근거로 삼은 누끼 Safe Margin 정책이 아직 코드에 없다.
- **선행 스펙과의 파일명** — `SegmentationInputNormalizer.kt`는 선행 스펙이 예약해 둔 이름이다. 이 스펙은
  `SegmentationRecoveryNormalizer.kt`를 써서 충돌을 피한다.

## 검수 이력

**2026-09-10, 설계 검수 2회(사실 대조·설계 공격).** 초안을 여섯 축에서 뒤집었다.

1. `maskSubjectAlpha`에 되올린 알파를 넣을 입구가 없었다 — 앞뒤로 쪼갰다.
2. `postProcess`가 final 클래스 `Subject`를 받아 원본 좌표를 못 담았다 — 좌표를 직접 받게 했다.
3. 힌트 하한을 폴백 이진화보다 높은 축에 잡아 힌트가 거의 항상 비었다 — 같은 축으로 내렸다.
4. 알파 되올림이 확대만 받는다는 전제가 512 하한과 모순이었다 — 재표본으로 바꿨다.
5. 회복 실패 뒤 재시도가 같은 사다리를 반복했다 — 실패 성격을 갈랐다.
6. 안전망으로 지목한 `require`가 항진명제였다 — 캔버스 검사를 더했다.

**2026-09-10, 구현 계획 검수 2회(실행 가능성·테스트 정합).** 설계를 한 번 더 고쳤다.

1. **대비 LUT가 원본 비트맵에 직접 썼다.** 크롭도 축소도 없으면 검출 판이 원본이고 원본은 가변이다 — 판 소유권을 두었다.
2. **알파가 제자리에서 지워져 되돌림 커버리지가 틀렸고, 원본 픽셀로 되돌리면 불투명 사각형이 됐다** — 알파 사본을 두었다.
3. **2단계 폴백에 크롭 오프셋이 빠졌다.** 1단계는 오프셋이 0이라 드러나지 않는다 — 순서를 못 박았다.
4. **실패 성격만 보는 분기가 사다리를 한 번 걸러 되풀이했다** — 끈적한 플래그를 더했다.
5. **픽셀 배열 할당이 OOM 가드 밖으로 나갔다** — 할당을 수확 안으로 되돌렸다.
6. **「편집 없이 사용」 취소는 UI에서 도달할 수 없었다** — 걷어냈다.
7. **필터 완화가 수동 편집의 엄격 하한과 충돌했다** — 로그로만 남긴다.
8. 회복 경로의 subject 루프를 따로 구현하게 되어 있었다 — 공통 루프로 합쳤다.
9. 무거운 작업의 디스패처와 취소 확인, 가드에 걸린 단계의 로그, 폴백의 `ModuleNotReady` 승격이 빠져 있었다.
10. 테스트 여섯이 대응 결함을 넣어도 통과했다 — 경계와 이상치 픽스처를 요구했다.
