---
id: ADR-0012
title: 이미지 세그멘테이션 — ML Kit Subject Segmentation 온디바이스 채택
status: accepted
date: 2026-07-12
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec: c103-segmentation-topping-edit, c103-multi-subject-selection, segmentation-pipeline-hardening, segmentation-mask-postprocessing, segmentation-alpha-refinement, alpha-kernel-suspend-cancellation, segmentation-retry-recovery
related_architecture: data-layer
platforms: android
tags: [adr, parfait]
---
# ADR-0012: 이미지 세그멘테이션 — ML Kit Subject Segmentation 온디바이스 채택

## 맥락
이미지에서 주요 피사체(전경)를 분리하는 [[누끼-따기]] 기능이 필요하다. MVP 미결 항목이던 "누끼 온디바이스 vs 서버"(→ [open-questions 2026-07-06 MVP 미결 정책](../../wiki/open-questions.md)) 중 처리 위치와 라이브러리를 정해야 했다.

## 결정
Google **ML Kit Subject Segmentation**(`play-services-mlkit-subject-segmentation`, GMS/Play services 기반)을 **온디바이스**로 채택한다.

- 버전은 `gradle/libs.versions.toml`의 `mlkitSubjectSegmentation`(현재 **beta**), 별칭 `google-mlkit-subject-segmentation`.
- `feature:segmentation:impl`의 `AndroidManifest`에 `com.google.mlkit.vision.DEPENDENCIES = subject_segment` meta-data를 두어 install-time에 모델을 다운로드.
- 실행은 `data`의 `ImageSegmentationRepositoryImpl`: `InputImage.fromBitmap` → `SubjectSegmenter`(`enableForegroundConfidenceMask`) → `foregroundConfidenceMask`(임계 0.5f)로 overlay/subject 비트맵 생성. subject 이미지는 `cacheDir`에 PNG로 저장하고 경로(`SegmentationResult.subjectImagePath`)를 반환.
- 블로킹 `Tasks.await(segmenter.process(...))`를 `Dispatchers.IO`로 감싸 suspend화, 마스크→픽셀 루프는 `Dispatchers.Default`.
- 비트맵은 [[0011-cross-module-bitmap-abstraction|BitmapWrapper]]로 도메인에 전달.

## 대안
- **서버 세그멘테이션** — 단말 성능 무관, 모델 교체 용이. 그러나 네트워크 왕복·서버 비용, 오프라인 불가.
  **→ 기각:** 원격 연동 자체가 후속 과제([[data-layer]]). 온디바이스가 오프라인·프라이버시 유리.
- **TFLite 커스텀 모델 직접 통합** — 모델·임계 완전 제어.
  **→ 기각:** 모델·전후처리 직접 관리 부담. ML Kit이 즉시 사용 가능.
- **ML Kit Selfie/일반 Segmentation** — 유사 온디바이스.
  **→ 기각:** 임의 피사체 대상엔 Subject Segmentation이 적합.

## 영향

**긍정**
- 온디바이스라 오프라인 동작·이미지 외부 미전송(프라이버시).
- ML Kit 통합이 단순, 별도 모델 관리 불필요.

**트레이드오프**
- **beta 라이브러리 의존** — API·동작 변동 가능.
- **GMS(Play services) 의존** — GMS 없는 기기 미지원. install-time 모델 다운로드라 첫 사용 지연·실패 가능.
- 결과 전달이 메모리 비트맵(overlay) + 파일경로(subject PNG)로 이원 — 캐시 파일 정리 정책 필요.

**위험·방어**
- 실패는 `Result<SegmentationResult>` + sealed `SegmentationException`(`ClientInit`·`ImageNotFound`)로 표현, ViewModel이 effect로 받아 Toast + back 처리.
- beta 승급·API 변동 추적 필요 → [open-questions 2026-07-12 ML Kit beta](../../wiki/open-questions.md).

## As-built 갱신 (2026-08-14, PR #221)

결정 자체는 유지되고 실행 세부가 바뀌었다. 위 "결정" 절과 갈리는 지점만 적는다.

| 원안 서술 | develop 현재 |
|---|---|
| 매니페스트 meta-data로 install-time 모델 다운로드 | meta-data는 **힌트일 뿐 보장이 없어서**, 사용 직전에 `ModuleInstall.areModulesAvailable` → 없으면 `installModules` → 재확인한다 |
| `foregroundConfidenceMask`(임계 0.5f)로 **overlay/subject 비트맵** 생성 | overlay 비트맵이 없다. 같은 루프에서 subject 픽셀과 **바운딩 박스**를 함께 모은다 |
| `SegmentationResult.bitmap`(`BitmapWrapper`) + `subjectImagePath` | `bitmap` 제거 — `subjectImagePath` + `subjectBounds: SegmentationBounds?` 2필드([[0011-cross-module-bitmap-abstraction]] 영향 절 참고) |
| sealed `SegmentationException`(`ClientInit`·`ImageNotFound`) | `ModuleNotReady`·`Process` 2종 추가. `Tasks.await`의 `ExecutionException`을 한 겹 벗겨 `MlKitException.UNAVAILABLE`이면 `ModuleNotReady` |
| (없음) | `saveEditedImage(BitmapWrapper): Result<String>` 신설 — 손편집 결과를 `cacheDir` PNG로 떨구고 경로 반환 |

> 📌 **`saveEditedImage`는 2026-09-06 PR #457로 `saveBitmap`이 됐다**(`SaveEditedImageUseCase` → `SaveBitmapUseCase`).
> 「편집 없이 사용」이 원본을 같은 자리로 보내면서 이름이 역할보다 좁아졌기 때문이고, 시그니처와 동작은
> 그대로다. 아래 기록은 당시 이름을 유지한다 → [c103-error-use-original](../superpowers/specs/archive/2026-09-05-c103-error-use-original.md).
> 🔁 그 버튼은 2026-09-10 PR #487에서 「직접 편집」이 됐다. 원본을 `saveBitmap`으로 한 번 떨구는 것은 같고,
> 떨군 원본을 C-104의 마스크로 넘긴다.

- "결과 전달이 메모리 비트맵 + 파일경로로 **이원**"이라던 트레이드오프는 **경로 단일로 정리됐다**.
  대신 화면이 경로를 다시 디코드하므로 디코드 비용이 화면 쪽으로 옮겨졌다.
- **캐시 파일 정리 정책은 더 급해졌다** — 추출 1장에 더해 편집을 마칠 때마다 최대 2장이 늘고
  삭제 경로가 없다 → [open-questions](../synthesis/open-questions.md) [2026-07-12].
- 실패 표현은 여전히 완전하지 않다 — `foregroundConfidenceMask == null`은 `Result`를 타지 않고
  raw `error()`로 던진다(같은 항목).
- 소비 화면은 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md).

## As-built 갱신 (2026-08-20, PR #309 develop 머지)

> 📌 **정정(2026-08-19)** — 이 브랜치는 develop에 미머지인 PR #290(`feature/topping-add-screen`)을
> 로컬 머지해 얹은 것이라 그 자체로 리뷰·머지될 수 없었다. 같은 작업이 plain develop 위
> **`refactor/segmentation-develop`**로 다시 만들어졌다(브랜치명만 달라졌을 뿐, 아래 서술하는
> `segmentImage` 재작성·캐시 정리·예외 처리 내용은 새 브랜치에서도 동일하게 확인됨). 상세는
> [segmentation-pipeline-hardening 스펙의 "as-built 정정" 절](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md#as-built-정정-2026-08-19-리베이스).
>
> ✅ **머지됨(2026-08-20, PR #309 — develop `cf357937`)** — 아래 서술은 이제 브랜치가 아니라
> develop 코드다. 브랜치 팁이 충돌 해소 편집 없이 그대로 들어갔다.
>
> 📌 **재정정(2026-08-20)** — 그 브랜치를 develop `750cc2dd` 위로 한 번 더 리베이스했다.
> **아래 세 항목의 결론은 그대로 유효하다.** 다만 `segmentImage`는 리베이스에서 develop과 충돌해
> 이 ADR이 다루는 저장 구간의 모양이 조금 달라졌다 — 이 라운드가 넣은 `try`/`finally` 안으로
> develop의 trimmed 비트맵 생성이 들어와, `finally`의 `recycle()`이 두 비트맵의 수명을 함께 닫는다.
> 상세는 [스펙의 "as-built 재정정" 절](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md#as-built-재정정-2026-08-20-두-번째-리베이스).

[segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md) 구현.
위 두 절이 남긴 것 셋을 여기서 닫는다.

- **캐시 정리 정책이 정해졌다** — `cacheDir` 전용 하위 디렉토리(`SegmentationCacheDir.kt`)를 두고
  `SegmentationViewModel`이 세그멘테이션 진입 시(디코드보다 먼저) 통째로 비운다.
  `ClearSegmentationCacheUseCase`가 도메인 쪽 진입점이다. 누적 상한은 직전 흐름 1회분으로
  줄었다. **"캐시 파일 정리 정책 필요"라던 트레이드오프와 "정리 정책은 더 급해졌다"는 경고를
  여기서 닫는다.**
- **`foregroundConfidenceMask == null`이 `Result.failure`를 타게 됐다** — 더는 raw `error()`가
  아니다. **"실패 표현은 여전히 완전하지 않다"는 문장을 지운다.**
- **마스크 루프가 `getPixels` 1회 + 배열 내 마스킹으로 바뀌었다** — 픽셀당 `Bitmap.getPixel` JNI
  왕복이 사라지고, 같은 배열을 훑으며 객체가 아닌 자리를 지우고 bounding box를 넓힌다
  (`SegmentationMask.kt#maskSubjectPixels` — 2026-08-27 PR #363 이 `maskSubjectAlpha` 로 개명하고 반환을 `MaskedAlpha` 로 바꿨다. `Bitmap` 비의존 순수 함수라는 성질은 그대로다). 마스크 버퍼 용량이
  `width * height`와 다르면 실패로 방어하는 것도 이때 붙었다 — 지금까지는 어긋나도 조용히
  잘못 읽었다. 그 가드는 라운드 안에서 한 번 더 손을 탔다(아래).
- **`segmentImage`의 저장 구간까지 방어가 마저 닫혔다** — 위 null 마스크·크기 불일치 두 경로를
  닫은 뒤에도 같은 `withContext(Dispatchers.Default)` 블록 안, `saveToCacheAsPng`가 던지는
  `IOException`은 여전히 `Result` 밖으로 새어나가 호출부(`SegmentationViewModel`의 `init`
  코루틴)를 죽였고 `subjectBitmap.recycle()`도 건너뛰었다. 저장 구간을 `try`로 마저 감싸고
  `recycle()`을 `finally`로 옮겨 실패해도 항상 돌게 했다 — **이 블록이 완전히 방어되는 것은 이
  시점부터다.** 같은 김에 마스크 크기 가드를 `capacity()`(버퍼 전체 용량)에서 `remaining()`
  (`get(index)`가 실제로 경계로 삼는 `limit` 기준 값)으로 정정했다. 그 방어가 `Exception`을
  통째로 잡던 탓에 `CancellationException`까지 삼켜 취소된 흐름을 실패로 보고하던 것도, 재던지는
  분기를 앞세워 갈랐다(`BaseViewModel.launch`와 같은 관용구).

## As-built 갱신 (2026-08-24, PR #342 develop 머지)

**이 ADR이 결정 절에 적어 둔 옵션이 바뀌었다.** `SubjectSegmenter`를 여는 옵션이
`enableForegroundConfidenceMask()` 하나에서
`enableMultipleSubjects(SubjectResultOptions.Builder().enableSubjectBitmap().build())`로 옮겨 갔다.
라이브러리·좌표·온디바이스 채택이라는 **결정 자체는 그대로**이고, 바뀐 것은 같은 라이브러리에서
무엇을 받아 오는가다 — 전경 전체를 합친 마스크 1장이 아니라 **피사체별 subject 목록**이고,
`enableSubjectBitmap()`을 켜면 `Subject.getBitmap()`이 이미 bounds 크기로 잘린 판을 준다.
소비 화면과 설계 근거는
[c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md).

⚠️ **두 옵션 계열을 한 요청에 함께 켜면 안 된다.** ML Kit 다이나마이트 모듈이 `SIGSEGV`로 죽는다
(2026-08-23 실기기 확인, Galaxy A35 / Android 16). 크래시가 `drishti_gl_runn`과 Binder
`onTransact` 양쪽에서 나고 스택이 전부 모듈 네이티브라 **JVM 예외 핸들러를 타지 않는다** —
`try/catch`로도 Crashlytics의 Java 핸들러로도 못 잡고 `logcat -b crash`에만 남는다. 공식 문서의
설정 예시 다섯 가지도 두 계열을 한 번도 함께 쓰지 않는다(명시적 금지 문구는 없다).
그래서 전경 마스크 경로는 지우지 않고 **후보가 0건일 때의 2차 요청**으로만 남겼다 —
`SegmentationMask.kt#maskSubjectAlpha`(당시 이름 `maskSubjectPixels`)와 위 절이 닫아 둔 방어가 그 경로에서 계속 쓰인다.
그 대가로 **세그멘터를 한 흐름에서 두 번 열 수 있다**(개폐·optional module 확인·예외 변환은
`runSegmenter`가 공유한다). 정상 경로는 오히려 가벼워졌다 — 쓰지도 않는 원본 해상도
`FloatBuffer`를 매번 받던 것이 사라졌다([open-questions](../synthesis/open-questions.md) OQ-P-268 해소).

⚠️ **저장이 이 계약에서 빠졌다** — `segmentImage`는 더 이상 PNG를 떨구지 않고, 위 절들이 서술한
저장 구간 방어는 신설 `persistSubject`로 옮겨 갔다(같은 `try`/`finally` 모양을 유지한다).
`SegmentationResult.subjectImagePath`를 만드는 코드가 어디인지가 바뀐 것이지 결과의 의미는 같다
→ [data-layer](../architecture/data-layer.md).

⚠️ **`segmenter.close()` 이후 ML Kit가 준 비트맵의 수명이 문서에 없다**(OQ-P-269). 이 라운드부터
그 비트맵을 화면 수명 내내 들고 그린다.

⚠️ **모델이 준 마스크를 그대로 쓰지 않게 됐다**(2026-08-27, PR #363) — 이 결정이 고른 것은
"전경을 누가 분리하는가"이고, 그 산출물의 **정확도를 어디까지 앱이 책임지는가**는 열려 있었다.
이 라운드가 그 자리를 채웠다: ML Kit이 준 알파를 `:data` 의 순수 커널에 태워 잡티 성분을 지우고,
원본 휘도를 안내자로 경계를 정련하고, 한 겹 침식한 뒤 tight bounds 와 커버리지를 다시 잰다.
**ML Kit 교체 가능성이라는 이 ADR 의 전제는 오히려 강해졌다** — 커널이 `Bitmap` 도 ML Kit 타입도
모르는 배열 연산이라 모델을 갈아도 그대로 남는다. 대신 **"온디바이스 모델 하나로 충분한가"라는
질문의 답이 '아니다'로 확정**됐다는 사실은 기록해 둔다. 후처리의 임계·반경·정칙화 값은 아직
실기기 사진 세트가 판정하지 않았다([open-questions](../synthesis/open-questions.md) OQ-P-287~300)
→ [data-layer](../architecture/data-layer.md).

## 실측 정정 (2026-09-02, Galaxy Z Flip 3)

위 「As-built 갱신 (2026-08-14)」 표의 **"사용 직전에 `areModulesAvailable` → 없으면 `installModules`
→ 재확인한다"가 성립하지 않는다.** 그 재확인은 설치 완료를 못 본다.

`installModules`가 돌려주는 Task는 **요청 접수**에서 완료된다. Play 서비스 모듈 설치 가이드가
"The install request has been sent successfully. This does not mean the installation is completed."
라고 명시하고, 설치의 진행과 종료는 `InstallStatusListener`로만 통지된다. 실기기에서 Task가
37ms 만에 성공으로 반환되고 25ms 뒤 리스너가 `STATE_FAILED`를 알리는 것을 확인했다.

따라서 **모듈이 없는 기기의 첫 사용자는 예외 없이 실패한다.** A35에서 안 드러난 것은 그 기기에
모듈이 이미 있어 첫 확인에서 통과했기 때문이다.

이 결정 자체는 유지된다 — 온디바이스 ML Kit Subject Segmentation은 그대로다. 바뀌는 것은 **모듈
준비를 누가 어떻게 기다리는가**이고, 대기·실패 판정·사전 설치 설계는
[segmentation-module-install 스펙](../superpowers/specs/archive/2026-09-02-segmentation-module-install.md)이 정본이다.
✅ **그 설계가 develop에 들어갔다**(2026-09-03, PR #438 `24679f8c`) — `SegmentationModuleInstaller`가
종료 신호까지 기다리고, 준비는 사진 확인 화면 진입에 미리 건다.

⚠️ **기기에 따라 이 모듈을 끝내 못 받는다.** 같은 기기에서 GMS가 `STATE_FAILED` /
`CommonStatusCodes.INTERNAL_ERROR`로 설치를 못 잇고, 재부팅해도 같다. `MODULE_NOT_FOUND`도
`INSUFFICIENT_STORAGE`도 아니고 Play 응답은 정상이라 앱이 고칠 수 있는 범위 밖이다. "온디바이스
모델을 Play 서비스가 내려준다"는 이 결정의 전제에 **배달 실패라는 잔여 위험**이 있다는 뜻이고,
그런 기기의 대체 경로는 열려 있다([open-questions](../synthesis/open-questions.md)).

### 후속 정정 (2026-09-02 오후, 같은 기기)

위 절의 두 서술을 실측이 뒤집었다.

⚠️ **"끝내 못 받는다"는 확정이 아니다.** 같은 기기에 **모듈이 몇 시간 뒤 도착했다**(`DynamiteModule`
원격 버전 `263234001`). 오전의 `INTERNAL_ERROR` 반복은 영구 실패가 아니라 아주 늦은 배달이었을 수
있다. 배달 실패라는 잔여 위험은 남지만 그 성격이 "불가"에서 "지연"으로 약해진다
([open-questions](../synthesis/open-questions.md) OQ-P-344).

⚠️ **`splits` 목록으로 모듈 유무를 판정하지 말 것.** optional module은 APK split이 아니라 Chimera
dynamite 모듈로 배달된다. 모듈이 도착해 실제로 동작하는 시점에도 GMS 패키지의 `splits` 목록에는
그 이름이 없었다.

그리고 이 결정이 지금까지 적지 않았던 제약 하나가 드러났다. **`SubjectSegmentation.getClient()`는
그 자체로 네이티브 그래프와 EGL 컨텍스트를 띄운다.** 그래서 세그멘터를 "모듈이 있는지 묻는
용도"로 하나 더 여는 것이 안전하지 않다 — 판정용 그래프와 실제 세그멘테이션용 그래프가 겹쳐
`SIGBUS`(`drishti` 스레드)로 죽는 것을 실기기에서 확인했다. 이 ADR이 이미 적어 둔 "옵션 조합으로
`SIGSEGV`" 기록과 같은 계열이고, **세그멘터 인스턴스의 동시 존재가 위험하다**는 것이 그 둘을
관통하는 사실이다. 모듈 판정은 `Feature`만 든 `OptionalModuleApi`로 한다
→ [segmentation-module-install 스펙](../superpowers/specs/archive/2026-09-02-segmentation-module-install.md).

## As-built 갱신 (2026-09-10, PR #487 develop 머지)

**입력도 손보기 시작했다. 다만 재시도에서만이다.** 후보 0건으로 실패한 사진에서 「다시 시도」를 누르면
`recoverCandidates`가 전처리 사다리를 한 번 돈다. 1단계는 대비 LUT와 검출 해상도 맞춤(짧은 변 512, 긴 변 2048,
둘이 충돌하면 상한이 이긴다)이고, 2단계는 1단계가 남긴 힌트로 크롭한다. 1차 경로의 입력은 그대로다.

이 결정의 전제와 닿는 자리는 둘이다.

- **세그멘터를 여는 횟수가 늘었다.** 1차 경로는 여전히 최대 두 번(다중 subject 다음 전경 폴백)이다. 회복 경로는
  단계마다 같은 두 겹을 돌기 때문에 **재시도 한 번에 최대 네 번** 연다. 두 옵션 계열을 한 요청에 싣지 않는다는
  위 제약은 그대로 지킨다. 사다리에 30초 상한이 있지만, `Tasks.await` 블로킹 추론 하나가 끝난 뒤에야 걸린다.
- **모델을 바꿀 수 있다는 전제는 그대로다.** 손본 판은 알파를 얻는 데만 쓰고 후보 픽셀은 원본에서 오려낸다.
  `PlateSource.OriginRegion`에 픽셀 인자가 없어서 이 규칙이 구조적으로 강제된다. 좌표 변환·가드·LUT는 `Bitmap`도
  ML Kit 타입도 모르는 순수 함수다.

⚠️ **회복 경로는 실기기에서 한 번도 돌지 않았다.** 조건부 항목(힌트 크롭·대비·512 확대)을 철회할 근거로 삼은
단계 로그도 logcat 밖으로 나가지 않는다(OQ-P-399)
→ [segmentation-retry-recovery 스펙](../superpowers/specs/archive/2026-09-10-segmentation-retry-recovery.md).
