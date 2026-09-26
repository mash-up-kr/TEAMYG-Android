---
id: segmentation-module-install
title: 세그멘테이션 optional module 설치 대기·실패 처리
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-03
related_code:
  - SegmentationModuleInstaller.kt#ensureInstalled
  - ModuleInstallGateway.kt
  - PlayServicesModuleInstallGateway.kt
  - ModuleInstallModule.kt
  - PrepareSegmentationModuleUseCase.kt
  - PictureConfirmViewModel.kt
  - ImageSegmentationRepositoryImpl.kt#runSegmenter
  - ImageSegmentationRepositoryImpl.kt#toSegmentationException
  - SegmentationException.kt#ModuleNotReady
  - SegmentationViewModel.kt#SegmentationState
  - SegmentationRoute.kt#SegmentationRoute
  - SegmentationErrorScreen.kt#SegmentationErrorScreen
  - AndroidManifest.xml
related_adr:
  - 0012-mlkit-subject-segmentation.md
related_spec:
  - 2026-08-23-c103-multi-subject-selection.md
related_architecture:
  - data-layer.md
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, segmentation, mlkit, module-install]
---

# Spec: 세그멘테이션 optional module 설치 대기·실패 처리

> ✅ **develop 머지(2026-09-03, PR #438 `24679f8c`)** — 설계대로 들어갔다. 설치기 테스트 7건과
> ViewModel 테스트 3건이 늘었고(유닛 1015 → 1029 중 10건), 계측 테스트는 그대로다. 이 스펙이 「파일
> 구성」에 미리 적어 둔 as-built 경로(`data/installer/image/`·`data/utils/image/`)가 머지된 코드와
> 같다. ⚠️ **모듈이 없는 상태의 화면 경로**(모듈 실패 문구·재시도 버튼)는 실기기에 모듈이 도착해
> 재현 수단이 사라진 채로 머지됐다 — 그 경로를 사람이 눈으로 본 적은 없다.

## 목표

세그멘테이션 모델이 아직 없는 기기에서 사진 편집이 조용히 실패하는 것을 없앤다. 설치가
끝났는지를 실제로 알아내고, 못 받았으면 사용자에게 맞는 안내를 준다.

## 왜 지금인가

2026-09-02, Galaxy Z Flip 3(SM-F711N, Android 15)에서 사진 편집이 매번 실패한다는 제보를 받고
실기기 logcat으로 원인을 둘로 갈랐다. 같은 앱이 Galaxy A35에서는 정상 동작한다.

### 원인 ① — `installModules`의 반환을 설치 완료로 오해한다

`ensureModuleInstalled`는 `Tasks.await(installModules(...))`가 돌아온 직후에
`areModulesAvailable`을 다시 물어 그 답을 결과로 삼는다. 실측은 이렇다.

```
[MLKIT-MODULE] 설치 요청 전 가용 여부 false
[MLKIT-MODULE] installModules 반환 37ms, 이미 설치됨 false      ← Task 는 성공으로 완료
[MLKIT-MODULE] 설치 상태 5, 오류 코드 8, 세션 16                ← 진짜 결과는 25ms 뒤 리스너로 도착
```

Play 서비스 모듈 설치 가이드가 이 계약을 문장으로 못 박아 두었다.

> The install request has been sent successfully. This does not mean the installation is completed.

설치의 진행과 종료는 `InstallStatusListener`로만 통지되고, 종료 상태는 `STATE_COMPLETED`·
`STATE_FAILED`·`STATE_CANCELED` 셋이다. 지금 코드에는 리스너가 없어서 **실패했다는 사실 자체를
알 수 없다.** 모듈이 없는 기기의 첫 사용자는 예외 없이 이 경로로 실패한다. A35에서 문제가 안 난
것은 그 기기에 모듈이 이미 있어 첫 확인에서 곧장 통과했기 때문이다.

### 원인 ② — 이 기기의 GMS가 설치를 못 잇는다

`설치 상태 5`는 `STATE_FAILED`, `오류 코드 8`은 `CommonStatusCodes.INTERNAL_ERROR`다.
`ModuleInstallStatusCodes`가 따로 정의한 `MODULE_NOT_FOUND`·`NOT_ALLOWED_MODULE`·
`INSUFFICIENT_STORAGE`가 아니므로 **기기에 모듈이 제공되지 않는 것도, 저장공간·네트워크 문제도
아니다.** Play 서버는 정상 응답한다.

```
I/Finsky  Module info request for [{MlkitSubjectSegmentation.optional:...}] ... hasAccount:true
D/Volley  https://play-fe.googleapis.com/fdfe/moduleDelivery [rc=200], [size=1202]
```

재부팅해도, 30초를 더 기다려도 결과가 같다. 앱이 고칠 수 있는 범위 밖이라 이 스펙은 원인 ②를
고치지 않는다. **정확히 알아채고 정직하게 알리는 데까지가 범위다.**

📌 **후속 관측(같은 날 14시)** — 그 기기에 **모듈이 결국 도착했다.** 오전 내내 `INTERNAL_ERROR`로
실패하던 설치가 몇 시간 뒤 성공했고, `DynamiteModule`이 원격 버전 `263234001`을 잡아 세그멘테이션이
정상 동작했다. 원인 ②는 **영구 실패가 아니라 아주 늦은 배달**일 수 있다. 이 스펙의 설계는 그대로
유효하다 — 어느 쪽이든 앱은 기다리고 알려야 한다. 열린 질문은
[open-questions](../../../synthesis/open-questions.md) OQ-P-344가 잇는다.

⚠️ **`splits` 목록으로 모듈 유무를 판정하지 말 것.** optional module은 APK split이 아니라 Chimera
dynamite 모듈로 배달된다 — 모듈이 도착해 실제로 동작하는 시점에도 GMS 패키지의 `splits` 목록에는
그 이름이 없었고, `DynamiteModule` 로그와 `areModulesAvailable`만이 사실을 말했다.

## 범위

- **포함**
  - `InstallStatusListener`로 설치 종료 상태까지 기다린다.
  - 모듈 준비를 카메라 화면 진입 시점에 **미리** 건다.
  - 진행 중인 설치를 여러 호출자가 공유한다(요청 중복 제거).
  - 실패 원인을 화면 문구로 가른다 — 대상 못 찾음과 모듈 준비 실패는 다른 문구를 쓴다.
  - **실패 화면에 재시도 버튼을 임시로 둔다**(아래 「재시도」). ⚠️ 디자인 검토 대기 시안이다.
  - 설치 상태·실패 코드 로그를 최종 코드에 남긴다.
- **제외**
  - **다운로드 진행률 표시** — 기존 로딩 오버레이를 그대로 쓴다.
  - **실패 코드별 문구 분기** — 문구는 모듈 실패 한 벌이다.
  - **앱 시작 시 `deferredInstall`** — 설치 경로를 둘로 늘리지 않는다.
  - **모듈을 끝내 못 받는 기기의 대체 경로** — open-questions로 넘긴다.

## API / 인터페이스

### data — `SegmentationModuleInstaller`

모듈 준비의 단일 소유자. `@Singleton`이지만 **코루틴 스코프도 `StateFlow`도 들지 않는다.**
스코프를 소유한 클래스는 취소·에러·재시작을 스스로 증명해야 하는데 이 클래스는 못 한다.

```kotlin
internal class SegmentationModuleInstaller(private val gateway: ModuleInstallGateway) {
    suspend fun ensureInstalled(): ModuleInstallOutcome
}

internal sealed interface ModuleInstallOutcome {
    data object Ready : ModuleInstallOutcome
    data class Failed(val errorCode: Int) : ModuleInstallOutcome
    data object TimedOut : ModuleInstallOutcome
}
```

### data — `ModuleInstallGateway`

GMS 타입이 닿는 자리를 이 이음매로 좁힌다. JVM 테스트가 설치 신호를 원하는 순서로 흘리기
위해서이고, 리포지토리에서 GMS 관심사를 덜어 내기 위해서이기도 하다.

```kotlin
internal interface ModuleInstallGateway {
    suspend fun isAvailable(): Boolean
    fun install(onSignal: (ModuleInstallSignal) -> Unit)
}

internal sealed interface ModuleInstallSignal {
    data object AlreadyInstalled : ModuleInstallSignal
    data object Completed : ModuleInstallSignal
    /** [installState] 를 함께 싣는다 — 취소는 `errorCode` 가 0 이라 코드만으로는 실패와 안 갈린다 */
    data class Failed(val installState: Int, val errorCode: Int) : ModuleInstallSignal
}
```

**두 함수에 인자가 없는 이유**는 게이트웨이가 `OptionalModuleApi`를 스스로 만들기 때문이다. GMS의
`areModulesAvailable`·`ModuleInstallRequest.Builder.addApi`가 그 타입을 요구한다.

⚠️ **그 `OptionalModuleApi`로 `SubjectSegmenter`를 쓰면 안 된다.** `SubjectSegmentation.getClient()`는
그 자체로 ML Kit의 **네이티브 그래프와 EGL 컨텍스트를 띄운다.** 판정용으로 하나 더 열면 실제
세그멘테이션용 클라이언트와 겹쳐 죽는다 — 2026-09-02 Galaxy Z Flip 3에서 확인했다.

```
graph.cc:502 Start running the graph, waiting for inputs.   (스레드 A)
graph.cc:502 Start running the graph, waiting for inputs.   (스레드 B, 43ms 뒤)
libc: Fatal signal 7 (SIGBUS), code 2 (BUS_ADRERR) in tid ... (drishti/...)
```

`OptionalModuleApi`는 `Feature` 배열만 돌려주면 되는 인터페이스다. 게이트웨이는 세그멘터 없이
feature 하나만 든 구현을 만든다.

```kotlin
private val segmentationModule = OptionalModuleApi {
    arrayOf(Feature("mlkit.segmentation.subject", 1L))
}
```

feature 이름과 버전의 근거는 같은 기기의 실측 로그다 — `ChimeraConfigurator: Starting update,
reason: 4 urgentFeatures: mlkit.segmentation.subject:1`. ⚠️ **ML Kit 내부 값이라 바뀔 수 있다.**
바뀌면 가용 판정이 조용히 false로 굳고 설치 요청만 반복된다(크래시는 아니다).

`onSignal` 콜백은 **정지 함수가 아니다.** GMS 리스너가 자기 Executor 스레드에서 부른다. 게이트웨이는
종료 신호를 흘린 직후 `unregisterListener`로 리스너를 걷는다. 상한 초과로 호출자가 떠나도 리스너는
남아 있다가 종료 신호에서 스스로 걷힌다.

GMS 구현이 `ModuleInstall.getClient`·`areModulesAvailable`·`installModules`·
`InstallStatusListener`·`unregisterListener`를 전부 감춘다.

⚠️ **가시성**: `ImageSegmentationRepositoryImpl`은 public 클래스이고 생성자도 public이라, `internal`
타입을 주입 파라미터로 받으면 `EXPOSED_PARAMETER_TYPE`으로 컴파일이 깨진다. 설치기와 게이트웨이를
public으로 두거나 리포지토리 impl을 `internal`로 내린다. 후자가 낫다 — `RepositoryModule`이 같은
모듈이라 결선이 안 깨지고, `:data` 밖에서 impl 타입을 부르는 곳이 없다.

### domain — `PrepareSegmentationModuleUseCase`

카메라 화면이 데이터 계층을 직접 부르지 않게 하는 통로다. 결과를 쓰지 않고 준비만 시킨다.
`ImageSegmentationRepository`에 대응 함수를 하나 추가한다.

### feature/camera — 사진 확인 화면

`PictureConfirmRoute`가 `viewModelScope.launch { prepareSegmentationModule() }`로 건다. UI 진입을
정지 함수로 옮기는 상태 보유자 경계라 허용되는 모양이다.

⚠️ **카메라 화면이 아니라 사진 확인 화면인 이유**는 진입 경로가 둘이기 때문이다. 세그멘테이션으로
가는 길은 `PictureConfirmRoute` 하나인데, 거기 도달하는 길은 `CustomCameraRoute`와
`CustomGalleryPickerRoute` 둘이다. 카메라에만 걸면 **갤러리로 사진을 고른 사용자는 사전 설치를 한
번도 안 탄다.** 확인 화면은 두 경로의 유일한 합류점이고 사용자가 사진을 확인하는 체류 시간도 있다.

`returnResultOnly = true`(배경 편집에서 온 경로)는 세그멘테이션으로 가지 않으므로 걸지 않는다.

⚠️ `PictureConfirmRoute`는 지금 ViewModel이 없는 스테이트리스 컴포저블이다. 이 훅을 위해 상태
보유자를 하나 만든다 — 상태는 없고 진입 시 준비만 거는 최소 형태다.

## 동작 / 상태

### 공유 대기

```kotlin
private val mutex = Mutex()
private var inFlight: CompletableDeferred<ModuleInstallSignal>? = null

suspend fun ensureInstalled(): ModuleInstallOutcome {
    if (gateway.isAvailable()) return ModuleInstallOutcome.Ready

    val pending = mutex.withLock {
        // 끝난 대기는 재사용하지 않는다 — 이 자리가 없으면 한 번 실패한 뒤 영영 그 실패만 돌려준다
        inFlight?.takeIf { !it.isCompleted } ?: startInstall().also { inFlight = it }
    }

    val signal = withTimeoutOrNull(INSTALL_TIMEOUT) { pending.await() }
        ?: return ModuleInstallOutcome.TimedOut.also { forget(pending) }

    return signal.toOutcome()
}

private suspend fun forget(pending: CompletableDeferred<ModuleInstallSignal>) {
    mutex.withLock { if (inFlight === pending) inFlight = null }
}
```

`startInstall`은 정지 함수가 아니다. 리스너를 등록하고 설치를 요청하고 `Deferred`를 돌려주기만
한다. 완료는 리스너 콜백이 `complete`로 채운다. 그래서 이 클래스에 스코프가 필요 없다.

⚠️ **`Deferred`가 나르는 것은 최종 결과가 아니라 게이트웨이의 신호다.** 완료 신호를 받은 뒤
가용 여부를 다시 확인하는 일이 정지 함수라 콜백 안에서 못 하기 때문이다. 판정은 신호를 받아 든
`ensureInstalled`가 한다.

⚠️ **끝난 대기를 걷는 일도 콜백이 아니라 `ensureInstalled`가 한다.** 콜백은 정지 함수가 아니라
`Mutex`를 잡을 수 없고, 이 클래스는 스코프를 안 들기로 했으니 콜백에서 코루틴을 띄울 수도 없다.
그래서 다음 호출자가 락 안에서 `isCompleted`를 보고 걷는다. 콜백이 하는 일은 `complete` 하나다.
같은 신호가 두 번 와도 두 번째 `complete`는 `false`를 돌려줄 뿐 던지지 않는다.

⚠️ **`CompletableDeferred`가 호출자 코루틴에 매달리지 않는 것이 이 구조의 핵심이다.** 카메라
화면을 벗어나 `viewModelScope`가 취소되면 카메라의 `await`만 끊기고 설치는 계속 간다. 이어서
편집 화면이 `ensureInstalled()`를 부르면 같은 `Deferred`에 붙는다. 요청이 두 번 나가지 않는다.

`inFlight`는 다음 호출자가 `isCompleted`로 걷고, **상한 초과에서도 비운다**(`forget`). 후자는 방어다 —
리스너가 종료 신호를 끝내 주지 않으면 그 `Deferred`가 영영 안 채워지고, 이후 모든 호출자가 죽은
대기에 붙어 프로세스가 사는 동안 재시도가 불가능해진다. 재촬영 동선이 재시도를 대신한다는 아래
설계가 그 자리에서 무너진다. **중복 요청 한 번이 영구 정지보다 싸다** — 오늘 실측에서 같은 요청을
여러 번 보내도 해가 없었다.

`forget`은 동일성 검사로 지운다. 자기가 기다리던 `Deferred`가 아직 `inFlight`일 때만 비워서,
그 사이 새로 시작된 설치를 지우지 않는다.

### 종료 판정

| 관찰한 것 | 결과 |
|---|---|
| `isAvailable()` 이 true | `Ready` — 설치를 요청하지 않는다 |
| 설치 응답의 `areModulesAlreadyInstalled` 가 true | `Ready` |
| `STATE_COMPLETED` | 가용 여부를 다시 확인해 true 면 `Ready` |
| `STATE_COMPLETED` 인데 재확인이 false | `Failed(STATE_COMPLETED, 0)` — 아래 방어 참고 |
| `STATE_FAILED` · `STATE_CANCELED` | `Failed(installState, errorCode)` |
| 설치 요청 Task 자체가 실패 | `Failed(statusCode)` |
| 상한 초과 | `TimedOut` |

`STATE_COMPLETED`에서 가용 여부를 다시 확인하는 것은 방어다. ML Kit 이슈 829가 "모듈은 있는데
ML Kit가 못 읽는" 상태를 보고했고, 우리는 그 상태를 겪은 적이 없으므로 단정하지 않는다. 그 상태를
만나면 성공으로 접지 않고 `Failed`로 떨어뜨린다 — **성공으로 접으면 곧바로 `process`가 "다운로드를
기다리는 중" 예외로 죽고, 로그에는 설치가 성공했다고 남아 원인이 가려진다.**

`STATE_UNKNOWN`(0)·`STATE_DOWNLOAD_PAUSED`(7)는 종료가 아니면서 오래 머무를 수 있다. 그 둘을
흡수하는 장치가 상한뿐이다.

`INSTALL_TIMEOUT`은 **20초**다. 재부팅 뒤 실측에서 Play 왕복만 5.4초 걸렸고 여기에 실제 다운로드가
얹힌다. 로딩 오버레이를 보는 사용자가 견딜 만하면서 정상 회선의 다운로드를 자르지 않는 값으로
잡았다. 실사용 데이터가 쌓이면 조정한다.

### 예외 매핑

`Failed`·`TimedOut`은 기존 `SegmentationException.ModuleNotReady`로 접되 실패 코드를 로그에
남긴다. `process` 단계의 `MlKitException.UNAVAILABLE` 매핑은 그대로 둔다 — 그 경로도 여전히
살아 있다.

### 화면 상태

> 📌 **이 절의 결정이 뒤집혔다(2026-09-05)** — 디자인 `C-103-Error` 확정본이 실패 문구를 한 벌로 요구해 `SegmentationErrorKind` 분기와 모듈 실패 문구 2건을 걷는다. 아래 설계는 그 시점까지의 기록이다 → [c103-error-use-original](2026-09-05-c103-error-use-original.md).

`SegmentationState.isError: Boolean`을 `errorKind: SegmentationErrorKind?`로 바꾼다.

- `SubjectNotFound` — 후보도 폴백도 못 얻은 기존 실패. 문구를 유지한다.
- `ModuleNotReady` — 모듈 준비 실패. 문구를 새로 넣는다.

`SegmentationErrorScreen`은 제목·설명을 파라미터로 받고, Route가 `errorKind`로 문자열을 고른다.
레이아웃·아이콘·색은 건드리지 않으므로 디자인 변경이 아니다.

지금 문구는 모듈 실패 상황에서 **틀린 지시**다. 사진을 바꿔도 결과가 같기 때문이다.

```xml
<!-- 기존: SubjectNotFound 에만 쓴다 -->
<string name="segmentation_error_title">사진 편집에 실패했어요</string>
<string name="segmentation_error_description">다른 사진을 선택하거나 다시 시도해 주세요</string>

<!-- 신설: ModuleNotReady -->
<string name="segmentation_module_error_title">사진 편집 기능을 준비하지 못했어요</string>
<string name="segmentation_module_error_description">네트워크 상태를 확인하고 잠시 후 다시 시도해 주세요</string>

<!-- 신설: 두 실패가 함께 쓰는 버튼 -->
<string name="segmentation_error_retry">다시 시도</string>
```

`SegmentationViewModel`이 실패 원인을 로그 없이 삼키던 것도 함께 고친다.

### 재시도

> 📌 **검토가 끝났고 버튼이 확정됐다(2026-09-05)** — 디자인 `C-103-Error`에 「다시 시도」와 「편집 없이 사용」 두 버튼이 들어왔다. 아래의 시안 경고는 더 이상 유효하지 않다 → [c103-error-use-original](2026-09-05-c103-error-use-original.md).

⚠️ **이 버튼은 디자인 검토를 받으려고 먼저 놓는 시안이다.**
[c103-multi-subject-selection](2026-08-23-c103-multi-subject-selection.md)이 실패 화면의
재시도 버튼을 "디자인에 없다"는 이유로 제외했고 OQ-P-153 ④가 그것을 추적 중이다. 검토 결과에
따라 모양도 자리도 바뀔 수 있다. **새 컴포넌트를 만들지 않고 디자인 시스템의 `YGButton`을 그대로
놓는 것**이 그래서다 — 버릴 때 버리기 쉽고, 검토가 볼 것은 컴포넌트가 아니라 배치다.

버튼은 **두 실패 모두에** 둔다. 문구는 실패마다 다르지만 버튼은 하나다.
`YGButton(text, buttonType = YGButtonType.Medium.Primary, isEnabled = true, onClick)`으로 놓는다.
타입 선택에 근거가 있는 것은 아니다 — 검토가 판정할 대상이라 하나를 골라 둔 것이다.

누르면 `SegmentationIntent.Retry`가 **화면 진입과 같은 절차를 처음부터 다시 태운다.** 지금
`SegmentationViewModel`의 `init` 블록에 있는 흐름을 이름 있는 함수로 빼고 진입과 재시도가 함께
쓴다. 디코드를 다시 하는 비용이 들지만, 중간 상태를 따로 들고 있다가 재사용하는 것보다 경로가
하나로 유지된다.

⚠️ **추출한 함수는 진입부에서 상태를 되돌려야 한다** — `isLoading = true`, `errorKind = null`,
`candidates = emptyList()`. 지금 코드는 실패 플래그를 **켜기만 하고 끄는 곳이 한 군데도 없다**.
그대로 추출하면 재시도가 성공해도 `errorKind`가 남아 Route의 분기가 계속 에러 화면을 고른다.

중복 실행은 `BaseViewModel`의 키 기반 `launch`로 막는다 — 후보 선택이 `SELECT_CANDIDATE_KEY`로
이미 쓰는 방식이고, 진행 중이면 새 요청을 **버린다**(취소 후 재시작이 아니다). ⚠️ **진입도 같은
키를 써야 한다.** 진입만 `viewModelScope.launch`로 두면 진입 흐름이 도는 중에 누른 재시도를
막지 못한다.

모듈 실패에서 이 버튼이 실제로 뜻을 가지는 이유는 「공유 대기」 절에 있다. 종료 상태에 도달한
설치는 `inFlight`를 비우므로 재시도가 **새 설치 요청을 낸다.**

닫기로 캔버스에 돌아가 카메라로 다시 들어가는 기존 동선도 재시도로 작동한다. 사전 설치를 카메라
진입에 걸었기 때문이다. 버튼이 걷히더라도 이 경로는 남는다.

## 파일 구성

> 📌 **as-built(2026-09-02)** — 구현 뒤 패키지를 정리해 아래 경로가 초안과 다르다. 모듈 설치 3종은
> `data/installer/image/`에, 세그멘테이션 순수 커널은 `data/utils/image/`에 있다. 둘 다
> `data/repository/image/`에 있었는데 **아무 리포지토리도 구현하지 않아** 옮겼다. 커널은 플랫폼
> import가 0개인 배열 연산이고(ADR-0012가 "모델을 갈아도 남는 순수 커널"이라 부른다), 설치 3종은
> 상태와 수명을 가진 협력자와 GMS 경계다. `source/`에 넣지 않은 이유는 이 저장소에서 `source`가
> 로컬·원격 데이터 접근과 매퍼 쌍을 뜻하기 때문이다.

| 파일 | 변경 |
|---|---|
| `installer/image/SegmentationModuleInstaller.kt` | 신설 — 공유 대기·종료 판정 |
| `installer/image/ModuleInstallGateway.kt` | 신설 — GMS 이음매 |
| `installer/image/ModuleInstallSignal.kt` · `ModuleInstallOutcome.kt` | 신설 — 게이트웨이가 흘리는 신호와 설치기가 돌려주는 결과. 둘은 `Failed` 모양이 같아 파일을 갈랐다 |
| `installer/image/PlayServicesModuleInstallGateway.kt` | 신설 — GMS 구현 |
| `di/ModuleInstallModule.kt` | 신설 — 게이트웨이 결선. `RepositoryModule`에 두지 않는다(리포지토리 결선이 아니다) |
| `SegmentationViewModelTest.kt` | `isError` 단언 3건이 `errorKind`로 바뀐다 |
| `SegmentationModuleInstallerTest.kt` | 신설 |
| `ImageSegmentationRepositoryImpl.kt` | `ensureModuleInstalled` 제거, 설치기 주입, 준비 함수 추가 |
| `ImageSegmentationRepository.kt` | 준비 함수 선언 추가 |
| `PrepareSegmentationModuleUseCase.kt` | 신설 |
| `PictureConfirmRoute.kt` + 신설 ViewModel | 진입 시 준비 호출(`returnResultOnly`면 안 건다) |
| `SegmentationViewModel.kt` | `isError` → `errorKind`, 실패 원인 로깅, 진입 흐름 함수 추출 + `Retry` 인텐트 |
| `SegmentationRoute.kt` · `SegmentationErrorScreen.kt` | 문구·재시도 콜백을 파라미터로 받고 `YGButton`을 놓는다 |
| `strings.xml` | 모듈 실패 문구 2개 + 재시도 버튼 라벨 추가 |

## 테스트

JVM 단위 테스트를 `runTest` 가상 시간으로 돌린다. 가짜 `ModuleInstallGateway`가 신호를 흘린다.

1. 이미 가용하면 설치를 요청하지 않는다 — `install` 호출 0회.
2. 동시 호출 둘이 요청을 한 번만 낸다. 사전 설치를 카메라에 건 대가로 생긴 위험이라 가장 중요하다.
3. 먼저 들어온 호출자를 취소해도 설치가 살아 있고, 두 번째 호출자가 같은 대기에 붙어 `Ready`를 받는다.
4. `STATE_FAILED`가 실패 코드를 실어 나온다.
5. 상한을 넘기면 `TimedOut`이고 `inFlight`가 걷혀, 그 뒤 호출자는 **새 설치를 시작한다**.
6. 종료 상태로 끝난 대기를 재사용하지 않는다 — `Failed` 뒤의 호출이 새 요청을 낸다. 재시도가
   실제로 뜻을 가지는지가 여기 걸려 있다.

📌 **as-built** — 설치기 테스트는 7건이다. 스펙의 6건에 "완료 신호를 받았는데 재확인이 실패한다"가
더해졌다. 그 분기를 실기기에서 재현할 수단이 없어 가짜 게이트웨이로만 잠근다.

ViewModel은 둘을 본다. ① `errorKind` 매핑 — 모듈 실패가 `ModuleNotReady`로, 후보 0건이
`SubjectNotFound`로 들어간다. ② `Retry` 인텐트가 진입과 같은 흐름을 다시 태우고, 연달아 눌러도
한 번만 돈다.

**테스트하지 않는 것**: 실제 GMS 설치는 단위 테스트로 재현할 수 없다. 실기기와 logcat이
유일한 확인 수단이라, **진단 로그를 최종 코드에 남긴다**(진단용 30초 폴링만 걷어낸다).

## 주의 / 열린 질문

- ⚠️ **원인 ②는 이 스펙이 못 고친다.** Z Flip 3처럼 GMS가 `INTERNAL_ERROR`로 설치를 못 잇는
  기기에서는 재촬영을 몇 번 돌아도 결과가 같다. 문구가 정직해지고 로그로 원인을 잡을 수 있게 될
  뿐이다. 그런 기기의 대체 경로는 open-questions로 올린다.
- 매니페스트 `com.google.mlkit.vision.DEPENDENCIES=subject_segment`는 그대로 둔다. ML Kit 문서가
  "Play 스토어 설치 후 자동 다운로드"로 설명하는 힌트이고, 카메라 진입 설치와 겹쳐도 해가 없다.
- `INSTALL_TIMEOUT` 20초는 실측 하나에 기댄 값이다.
