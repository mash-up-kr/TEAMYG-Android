---
id: canvas-save-preview-capture-holder
title: 캔버스 저장 미리보기 캡처 전달 (Capture Holder)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-07
related_code:
  - CanvasCaptureHolder.kt#CanvasCaptureHolder
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasImageSaveRoute.kt#CanvasImageSaveRoute
  - CanvasImageSaveScreen.kt#CanvasImageSaveScreen
  - CanvasCaptureCache.kt#writeToCanvasCaptureCache
  - CanvasCaptureCache.kt#readCanvasCaptureCache
  - NavKeyCanvasImageSave.kt#NavKeyCanvasImageSave
  - NavKeyCanvasImageSave.kt#CanvasImageSaveResult
  - CanvasMainViewModel.kt#CanvasMainEffect.RequestCanvasCaptureForPreview
  - CanvasMainViewModel.kt#handleSaveCapturedCanvas
  - CanvasMainViewModel.kt#observeDayBoundary
  - Navigator.kt#Navigator
  - MainRoute.kt#MainRoute
  - ParfaitImageLoader.kt#newParfaitImageLoader
  - ToppingAlphaMaskCache.kt#loadToppingAlphaMask
related_adr:
related_spec: c001-canvas-gallery-save
related_architecture:
  - navigation-flow.md
  - state-management.md
tags: [spec, parfait, canvas, gallery, c-001, save-preview]
---

# Spec: 캔버스 저장 미리보기 캡처 전달

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

> ✅ **구현 완료·develop 머지(2026-09-07, PR #463 `2285d09da`).** 머지본을 줄 단위로 대조한 결과
> **설계와 어긋난 자리가 없다** — 홀더 세 함수와 `@Volatile` 필드, `onDispose`에서 `navKey !in
> navigator.backStack`일 때만 비우는 조건, `bitmap`/`fallbackImagePath` 두 인자로 갈린 화면,
> `check(compress(...))`, 유닛 6건(홀더 4 · 캐시 2)이 모두 이 문서대로다. 코드가 문서보다 더 적은
> 것도 없다.

> 📌 **초판(2026-09-07)의 전제 셋이 검수에서 뒤집혔다.** ① 폴백이 "프로세스 사망 복원"에서 발동한다고
> 적었으나 이 앱의 백스택은 저장되지 않아 그 상황에서 미리보기는 **복원 자체가 되지 않는다**.
> ② 꺼내면서 비우는 홀더가 안전하다고 보았으나, 그러면 Activity 재생성과 딥링크 복귀에서 그림을
> 잃는다. ③ 홀더가 계약을 강화한다고 적었으나 실제로는 **약화**한다. 아래 본문은 정정된 판이다.

## 목표

저장 미리보기에 진입했을 때 캡처 이미지가 곧바로 보이게 한다.

지금은 캔버스 메인이 캡처한 비트맵을 메모리에 들고 있으면서도 그것을 미리보기에 넘기지 못한다.
`NavKeyCanvasImageSave`가 `@Serializable`이라 비트맵을 실을 수 없어 캐시에 PNG로 굽고
**절대경로만** 넘기고, 미리보기는 그 경로를 `AsyncImage`로 다시 읽는다. 그래서 진입할 때마다
`파일 열기 → 전체 해상도 PNG 디코드 → 크로스페이드`를 거치고, 그동안 화면에는 테두리만 있는 빈
프레임이 보인다.

이 지연은 캐시 설정을 고쳐서 줄일 수 있는 종류가 아니다. 미리보기의 Coil 요청은
`addLastModifiedToFileCacheKey(true)`를 걸고 있는데, 이것은 `FileUriKeyer`가 메모리 캐시 키를
`"$uri-$lastModified"`로 만들게 한다. 캡처할 때마다 파일의 수정 시각이 바뀌므로 **이 화면은 원리적으로
메모리 캐시에 맞을 수 없다.** 고정 파일명 때문에 이전 캡처가 다시 뜨는 것을 막으려고 넣은 장치이고
그 목적대로 동작하므로, 이 조항을 되돌리는 것이 아니라 **정상 경로가 파일을 거치지 않게** 만든다.

## 범위

- **포함**:
  - 캡처 비트맵을 화면 사이로 나르는 홀더 신설.
  - 미리보기가 비트맵을 직접 그리는 경로와, 비트맵이 없을 때의 방어 분기.
  - 캔버스 메인이 캡처를 홀더에 담는 자리.
  - `writeToCanvasCaptureCache`가 `Bitmap.compress`의 반환값을 확인하게 하는 수정(아래 「압축 실패를
    성공으로 넘기지 않는다」).
  - 홀더 계약의 JVM 유닛 테스트.
  - `parfait/architecture/navigation-flow.md` 「캔버스 저장 미리보기 왕복」 절 갱신과
    `parfait/synthesis/open-questions.md`의 OQ-P-364·OQ-P-365 재판정.
- **제외**:
  - **저장 버튼을 누른 뒤 화면이 전환되기까지의 지연.** 전체 해상도 PNG 압축이 여전히 `goTo` 앞을
    막는다. 이 압축을 비동기로 내리면 캡처·캐시 쓰기 실패를 진입 전에 알리는 지금 동작
    (`canvas_main_capture_failure`)이 성립하지 않아, 이번 라운드에서는 손대지 않기로 확정했다.
  - **저장을 확정한 뒤의 파일 재디코드.** `readCanvasCaptureCache`가 그대로 남는다.
  - **`feature/groups/canvas/api`의 공개 계약.** `NavKeyCanvasImageSave`·`CanvasImageSaveResult`·
    `CANVAS_IMAGE_SAVE_RESULT_KEY` 셋 다 그대로다. 백스택 구성과 화면 전환 애니메이션도 바뀌지 않는다.
  - **갤러리 저장 권한 왕복.**
  - **고정 파일명이 만드는 덮어쓰기**(OQ-P-365). 캡처 파일을 지우는 자리도 만들지 않는다.
  - **저장 아이콘 연타 방어.** `handleClickSaveToGallery`에 스로틀이 없어 이펙트가 버퍼에 쌓일 수
    있다는 것을 검수가 발견했으나, 이번 변경과 독립인 기존 결함이라 미결로만 올린다.

## API / 인터페이스

### 신설: `CanvasCaptureHolder`

`feature/groups/canvas/impl/src/main/kotlin/.../impl/util/CanvasCaptureHolder.kt`

```kotlin
internal object CanvasCaptureHolder {
    fun put(bitmap: Bitmap)
    fun peek(): Bitmap?
    fun clear()
}
```

- `put` — 캔버스 메인이 캡처한 비트맵을 담는다. 이전 값이 있으면 덮어쓴다.
- `peek` — 담긴 비트맵을 돌려주되 **비우지 않는다.** 비어 있으면 `null`이다.
- `clear` — 비운다. 미리보기가 백스택에서 빠질 때 그 화면이 부르고(아래 「홀더 수명」), 테스트가
  전역 상태를 되돌릴 때도 쓴다.

같은 디렉토리의 `ToppingAlphaMaskCache`가 이미 파일 최상위 전역 캐시를 두는 선례다. 홀더를
Hilt로 주입하지 않는 이유는 미리보기 화면에 ViewModel이 없어(그릴 것이 인자뿐이라 만들지 않았다)
주입 지점이 없기 때문이다.

**꺼내면서 비우지 않는다.** 초판은 `take()` 하나로 두어 미리보기가 진입 직후 꺼내고 홀더를 비우게
했는데, 그러면 미리보기가 다시 컴포즈될 때마다 그림을 잃는다(아래 「홀더 수명」). 읽기 전용이 되면서
필드에는 `@Volatile`만 두면 충분해졌다 — 초판의 `take()`는 read-modify-write라 `@Volatile`이 약속을
지키지 못했다.

### 변경: `CanvasImageSaveScreen`

```kotlin
@Composable
internal fun CanvasImageSaveScreen(
    bitmap: ImageBitmap?,
    fallbackImagePath: String,
    date: LocalDate,
    onClickClose: () -> Unit,
    onClickSave: () -> Unit,
    modifier: Modifier = Modifier,
)
```

- `bitmap` — 캡처를 그대로 그릴 비트맵.
- `fallbackImagePath` — 기존 `imagePath`의 개명.

## 동작 / 상태

### 정상 경로

```
C-001 캔버스 메인
  RequestCanvasCaptureForPreview
    ─▶ graphicsLayer.toImageBitmap().asAndroidBitmap()
    ─▶ writeToCanvasCaptureCache(Dispatchers.IO)      // 저장 확정이 읽을 파일. 성공해야 진입
         성공 ─▶ CanvasCaptureHolder.put(bitmap)
                 goTo(NavKeyCanvasImageSave(imagePath, date))
         실패 ─▶ 토스트(canvas_main_capture_failure)
                        │
        C-001 저장 미리보기
          remember { CanvasCaptureHolder.peek()?.asImageBitmap() }
            비트맵 있음 ─▶ Image(bitmap, ContentScale.Fit)      // 디스크·디코드·페이드 없음
            비트맵 없음 ─▶ AsyncImage(fallbackImagePath)        // 아래 「비어 있을 때」
```

`asImageBitmap()`은 픽셀을 복사하지 않고 감싸기만 한다.

### 홀더 수명

홀더는 **언제나 최대 한 장**을 든다 — `put`이 이전 값을 덮어쓰기 때문이고, 누적되는 경로가 없다.
그 한 장은 미리보기가 백스택에서 빠질 때까지 산다.

**비우는 자리는 미리보기의 `onDispose` 하나다.** 거기서 자기 `NavKey`가 `Navigator.backStack`에 아직
있는지 보고, 없을 때만 비운다. 키가 남아 있다는 것은 화면이 잠깐 내려간 것이므로(아래 두 경로)
돌아왔을 때 그림이 있어야 하고, 빠졌다는 것은 이 화면이 끝났다는 뜻이다. 닫기·저장 확정·시스템
백이 전부 이 조건 하나로 모인다 — `MainRoute`가 `NavDisplay(onBack = navigator::onBack)`으로 시스템
백을 직접 받으므로 화면의 닫기 콜백만으로는 그 경로를 덮지 못한다. `BackHandler`를 따로 달지 않는
것은 이 화면이 predictive back 전환을 쓰기 때문이다.

초판은 미리보기가 꺼내면서 비우게 했다. 미리보기 컴포지션이 그림을 들고 화면이 사라질 때 함께
놓인다는 그림이었는데, **미리보기가 화면에서 사라지지 않고도 다시 컴포즈되는 경로가 둘 있다.**

- **Activity 재생성.** `MainActivity`에 `android:configChanges`가 없어 다크모드 전환·글꼴 크기 변경·
  로케일 변경이 Activity를 다시 만든다. 이때 `Navigator`는 `@ActivityRetainedScoped`라 살아남아
  백스택에 미리보기가 그대로 있고, `CanvasImageSaveRoute`만 새로 컴포즈된다.
- **백스택 하강 후 복귀.** `MainRoute`가 푸시 딥링크를 전역에서 수집해 조건 없이 `goTo`한다. 미리보기를
  열어 둔 채 알림을 탭하면 미리보기가 아래로 내려갔다가, 뒤로 오면 다시 컴포즈된다.

두 경우 모두 `remember`가 다시 계산되므로, 비우는 홀더였다면 이미 비어 있어 폴백으로 떨어진다.
사용자 눈에는 있던 그림이 사라졌다가 디스크 디코드와 페이드를 거쳐 돌아온다 — **이 스펙이 없애려던
그 깜빡임이다.** 비파괴로 두면 두 경로 모두 그림이 그대로 남는다.

`remember`의 계산 블록에 부작용을 두지 않게 되는 것도 함께 얻는다. Compose는 컴포지션을 시도했다
폐기할 수 있고 그 계산은 되돌려지지 않으므로, 그 자리에서 전역 상태를 비우는 것은 애초에 계약 위반이었다.

대가는 미리보기가 살아 있는 동안 캡처 한 장이 메모리에 남는 것이다. 그 비트맵의 실체는 API에 따라 갈린다
— Compose `ui-graphics`의 `GraphicsLayer.toImageBitmap()`(`LayerSnapshot.android.kt`)은 API 28 이상에서
`LayerSnapshotV28`(`Bitmap.createBitmap(Picture)`, 소스 주석이 명시하는 하드웨어 비트맵 생성 경로)을 타
`Bitmap.Config.HARDWARE` 비트맵을 돌려주고, API 26·27만 `LayerSnapshotV22`(`ImageReader`) 경로로
`ARGB_8888` 소프트웨어 비트맵을 만든다. 이 앱의 `minSdk`가 26이라 두 경로가 다 살아 있다. 그래서
대부분의 기기(API 28+)에서 상주 비용은 자바 힙이 아니라 그래픽 메모리이고, 캡처 PNG 한 장이 디스크
캐시에 남는 기존 결정(OQ-P-365 ①)과 "성격이 같다"는 비유는 그만큼 헐거워진다.

### 비어 있을 때

`peek()`이 `null`이면 `fallbackImagePath`를 지금과 똑같이 `AsyncImage`로 읽는다. 폴백 요청의
`addLastModifiedToFileCacheKey(true)`는 유지한다.

**지금 알려진 경로 중 이 분기에 닿는 것은 없다.** 홀더는 프로세스 전역이라 Activity 재생성을 넘고,
프로세스가 죽으면 미리보기 자체가 복원되지 않기 때문이다(아래 「백스택은 저장되지 않는다」). 그래도
분기를 남기는 이유는 둘이다. `NavKeyCanvasImageSave`가 어차피 그 경로를 나르고(저장 확정이 그 파일을
읽는다), 홀더가 어떤 이유로든 비었을 때 화면이 빈 채로 남는 것보다 그리려고 시도하는 편이 낫다.

### 백스택은 저장되지 않는다

`Navigator`는 백스택을 `mutableStateListOf`로 들고 `@ActivityRetainedScoped`로 산다. `MainRoute`가
그것을 그대로 `NavDisplay`에 넘기며 `rememberNavBackStack`도 `SavedStateHandle`도 쓰지 않고,
`MainActivity.onCreate`도 `savedInstanceState`를 읽지 않는다. 그래서 **프로세스가 죽고 돌아오면
백스택은 `NavKeySplash` 하나로 리셋된다.**

`NavKeyCanvasImageSave`가 `@Serializable`인 것은 Navigation3의 `NavKey` 관용구이고, 이 앱에서 그것을
실제로 직렬화해 저장하는 자리는 없다. "프로세스 사망 뒤 경로만 살아 돌아온다"는 서술은
`NavKeyCanvasImageSave`의 기존 KDoc과 OQ-P-364 ①에도 들어 있는데, 그 전제는 사실이 아니다.
이 스펙은 그 정정을 함께 싣는다.

### 압축 실패를 성공으로 넘기지 않는다

`writeToCanvasCaptureCache`는 `Bitmap.compress`의 `Boolean` 반환값을 버리고 `runCatching`으로 예외만
잡는다. 그래서 압축이 중간에 실패하면 **잘린 파일이 남았는데 `Result`는 성공**이다.

지금은 미리보기가 그 파일을 읽다 빈 프레임이 되어 사용자가 확정 전에 이상을 알아챈다. 홀더가 들어가면
미리보기는 멀쩡한 메모리 비트맵을 보여 주므로 **실패가 확정 이후로 밀린다** — 사용자가 안심하고
확정한 뒤에야 저장 실패 토스트가 뜨거나, 잘린 그림이 갤러리에 남는다.

그래서 이 라운드가 `compress`의 반환값을 확인해 거짓이면 실패로 돌린다. 이 수정 없이는 홀더가
**보고 확정한 그림과 갤러리에 남는 그림이 같아야 한다**는 계약을 약화시킨다.

### 계약은 강해지지 않는다

초판은 `asImageBitmap()`이 픽셀을 복사하지 않으므로 위 계약이 강해진다고 적었다. 틀렸다. 그 사실은
홀더와 미리보기 사이의 이야기이고, 미리보기와 갤러리 사이와는 무관하다.

변경 전에는 미리보기와 저장이 **같은 파일 하나**를 거쳐 계약이 구조적으로 보장됐다. 변경 후에는
미리보기가 메모리 객체를, 저장이 파일 디코드를 각각 본다 — **출처가 둘로 갈린다.** 위 압축 검사가
그 갈림에서 생기는 가장 큰 구멍을 막지만, 갈림 자체는 남는다.

### 오늘 캔버스와 지난 캔버스

저장 아이콘은 날짜바에 있고 노출 조건은 날짜가 아니라 `CanvasMainUiState.isCanvasSaveVisible`
(`(토핑 0 && 배경 없음)`의 부정)이다. 그래서 오늘 캔버스와 지난 캔버스가 모두 이 흐름에 들어온다.
`CanvasMainScreen`이 `captureGraphicsLayer`를 조건 없이 넘기고 `YGCanvas`도 날짜를 보지 않으므로
둘은 같은 경로를 타며 분기가 없다.

날짜는 계속 `NavKeyCanvasImageSave.date`가 나르고 **홀더는 비트맵만 담는다.** 표시할 날짜의 출처를
하나로 두려는 것이고, 그래서 미리보기의 날짜 라벨은 캡처 시점 값으로 고정된다.

저장 성공 토스트의 날짜는 다르다. `handleSaveCapturedCanvas`가 그 시점의 `state.value.selectedDate`를
읽는데, `selectedDate`는 컴포지션이 아니라 ViewModel에 있고 ViewModel은 미리보기가 떠 있는 동안에도
살아 있다. `observeDayBoundary`가 파르페 하루 경계에서 그 값을 오늘로 옮기므로, 경계 직전에 저장을
눌러 미리보기에 머무는 동안 경계를 넘기면 **토스트가 미리보기가 보여 준 날짜와 다른 날짜를 말할 수
있다.** 창이 매우 좁고 이번 변경과 무관한 기존 성질이라 여기서 다루지 않는다.

## 파일 구성

| 파일 | 처리 | 역할 |
|---|---|---|
| `impl/util/CanvasCaptureHolder.kt` | 신설 | 캡처 비트맵을 화면 사이로 전달 |
| `impl/util/CanvasCaptureCache.kt` | 변경 | `compress` 반환값 확인 |
| `impl/screen/CanvasImageSaveScreen.kt` | 변경 | 인자 2개로 갈리고 비트맵 분기 추가. `@Preview`도 함께 갱신 |
| `impl/route/CanvasImageSaveRoute.kt` | 변경 | `peek()`으로 비트맵 확보 후 Screen에 전달. 백스택에서 빠질 때 `clear()` |
| `impl/route/CanvasMainRoute.kt` | 변경 | `goTo` 직전에 `put(bitmap)` |
| `impl/src/test/.../util/CanvasCaptureHolderTest.kt` | 신설 | 홀더 계약 |
| `impl/src/test/.../util/CanvasCaptureCacheTest.kt` | 신설 | 압축 실패가 실패로 나가는가 |

`feature/groups/canvas/api`는 변경이 없다.

## 검증

**JVM 유닛** — `CanvasCaptureHolderTest`. `Bitmap`은 `mockk`로 세운다(같은 모듈의
`CanvasMainViewModelTest`가 이미 그렇게 한다). 모듈이 이미 `parfait.test.unit` 플러그인을 쓰므로
새 소스셋도 빌드 설정 변경도 없다.

- `peek`이 `put`한 비트맵을 돌려준다.
- `peek`을 두 번 불러도 같은 비트맵이 나온다(꺼내도 비우지 않는다).
- `put`이 이전 비트맵을 덮어쓴다.
- 비어 있을 때 `peek`은 `null`이다.

`CanvasCaptureCacheTest`가 압축 실패 분기를 덮는다. `Context`는 `mockk`로 세우고 `cacheDir`에
JUnit `TemporaryFolder`를 물리면 실제 파일 IO가 도는 채로 `compress`만 스텁할 수 있다.

- `compress`가 거짓이면 `Result`가 실패다.
- `compress`가 참이면 `Result`가 성공이다(`check`를 과하게 걸어 정상 경로까지 막는 회귀를 잡는다).

`check`를 지우는 뮤테이션을 걸어 첫 건이 실제로 깨지는 것을 확인했다.

**이 유닛들이 보증하는 범위는 홀더 계약과 압축 실패 분기까지다.** 누군가 `CanvasMainRoute`의 `put`이나
`CanvasImageSaveRoute`의 `peek`을 지워도 전부 통과하고, 앱은 크래시 없이 예전의 느린 경로로
조용히 돌아간다. **이 기능의 유일한 관측 가능한 증상이 "느리다"인데 그것을 잡는 자동 검증은 없다.**
계측 테스트 소스셋 신설을 범위 밖으로 둔 결과이고, 아래 수동 확인이 그 자리를 대신한다.

**수동 확인**

- 오늘 캔버스에서 저장 → 미리보기 진입 즉시 이미지가 보인다.
- 지난 캔버스에서 저장 → 날짜 라벨과 이미지가 그 날의 것이다.
- 미리보기에서 확정 → 갤러리 저장이 성공하고 토스트의 날짜가 맞다.
- 미리보기에서 취소 → 캔버스로 돌아오고, 다시 저장하면 새 캡처가 보인다.
- 미리보기에서 **시스템 백**으로 나간다 → 닫기 버튼과 같이 동작한다(그 경로도 `onDispose`를 지난다).
- **미리보기가 떠 있는 상태에서 다크모드를 토글한다** → 그림이 그대로 남아야 한다. 사라졌다가
  페이드로 돌아오면 홀더가 비파괴가 아니라는 뜻이다.

## 주의 / 열린 질문

- **전역 가변 상태가 하나 늘어난다.** 미리보기 화면에 ViewModel이 없다는 기존 결정의 대가다.
  미리보기가 언젠가 ViewModel을 갖게 되면 이 홀더는 그리로 흡수될 자리다.
- **홀더가 마지막 캡처 한 장을 붙든다.** 다음 캡처가 밀어낼 때까지 전체 해상도 비트맵이 상주한다.
  「홀더 수명」이 그 근거를 적었다.
- **미리보기와 저장의 출처가 갈렸다.** 「계약은 강해지지 않는다」 참고. 압축 검사가 가장 큰 구멍을
  막지만 구조적 보장은 사라졌다.
- **OQ-P-364 ①의 전제를 정정해야 한다** — "프로세스 사망 뒤 경로만 살아 돌아온다"는 이 앱에서
  일어나지 않는다. 백스택이 저장되지 않아 그 화면 자체가 복원되지 않기 때문이다.
- **OQ-P-365 ②의 판정을 되돌린다.** 초판은 "미리보기가 백스택에 하나뿐이라 실질적으로 발생하지
  않는다"고 닫았는데 근거가 틀렸다. 미리보기를 열어 둔 채 푸시 딥링크로 다른 그룹 캔버스에 가서
  저장하면 고정 파일명이 덮이고, **홀더도 같은 자리에서 덮인다.** 아래로 내려갔던 미리보기로 돌아오면
  다른 그룹의 캡처를 그 날짜 라벨과 함께 보게 된다. 이번 라운드가 만든 결함은 아니고 닫지도 않는다 —
  홀더가 파일과 같은 성질을 갖는다는 사실만 미결에 싣는다.
- **저장 아이콘 연타 방어가 없다**(신규 미결). `handleClickSaveToGallery`가 스로틀 없이
  `postSideEffect`하고 `_effect`가 `Channel(BUFFERED)`이라, 연타하면 두 번째 이펙트가 버퍼에 남았다가
  미리보기에서 돌아온 뒤 전달되어 곧바로 다시 미리보기로 들어간다. 이번 변경과 독립이다.
- **전역 크로스페이드는 그대로 둔다.** `newParfaitImageLoader`의 `crossfade(true)`는 원격 이미지가
  투명한 자리에서 튀지 않게 하려는 앱 전체의 결정이고, 이 화면은 정상 경로에서 Coil을 쓰지 않게
  되므로 손댈 이유가 없다.
