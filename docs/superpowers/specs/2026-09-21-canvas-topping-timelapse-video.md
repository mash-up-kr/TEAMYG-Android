---
id: canvas-topping-timelapse-video
title: 캔버스 토핑 타임랩스 동영상 저장 (Canvas Topping Timelapse Video)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-09-27
related_code:
  - CanvasImageSaveScreen.kt#CanvasImageSaveScreen
  - CanvasImageSaveRoute.kt#CanvasImageSaveRoute
  - NavKeyCanvasImageSave.kt#CanvasImageSaveResult
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasMainViewModel.kt#handleSaveCapturedCanvas
  - CanvasToppingLayer.kt#CanvasToppingLayer
  - YGCanvas.kt#CANVAS_AREA_ASPECT_RATIO
  - ToppingGeometry.kt#TOPPING_BASE_LONG_SIDE_RATIO
  - YGToppingCutoutImage.kt#YGToppingCutoutImage
  - YGLoadingOverlay.kt#YGLoadingOverlay
  - GalleryMediaProvider.kt#insertPendingImage
  - GalleryRepositoryImpl.kt#saveImageToGallery
  - SaveCanvasToGalleryUseCase.kt#SaveCanvasToGalleryUseCase
  - GalleryWritePermissionManager.kt#GalleryWritePermissionManager
  - ToppingVideoTimeline.kt#toppingVideoFrames
  - VideoTimelineOptions.kt#VideoTimelineOptions
  - ToppingVideoFrame.kt#ToppingVideoFrame
  - Mp4VideoEncoder.kt#Mp4VideoEncoder
  - BitmapSurfaceWriter.kt#BitmapSurfaceWriter
  - GalleryMediaProvider.kt#insertPendingVideo
  - GalleryRepositoryImpl.kt#saveVideoToGallery
  - SaveCanvasVideoToGalleryUseCase.kt#SaveCanvasVideoToGalleryUseCase
related_adr: ADR-0034
related_spec:
related_architecture: module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, gallery, video]
---

# Spec: 캔버스 토핑 타임랩스 동영상 저장

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 구현 상태

화면 아래 조각만 들어와 있다. 사용자가 누를 버튼이 없어 **지금은 어디서도 호출되지 않는다** —
결함이 아니라 화면 결선 전 단계다(OQ-P-411).

| 조각 | 상태 |
|------|------|
| `ToppingVideoTimeline` · `VideoTimelineOptions` · `ToppingVideoFrame` | 구현됨 |
| `Mp4VideoEncoder` · `BitmapSurfaceWriter` | 구현됨. 계측 테스트는 CI가 컴파일만 한다(OQ-P-102) |
| 갤러리 영상 저장(`data`·`domain`) | 구현됨 |
| `CanvasToppingLayer` 등장 진행도 · 녹화 레이어 · 프레임 루프 · 결과 타입 sealed · 미리보기 버튼 · ViewModel 경로 | 미착수 |

## 목표

캔버스에 토핑이 **쌓인 순서대로 하나씩 등장하는 동영상**을 만들어 기기 갤러리에 저장한다.
지금은 캔버스를 한 장의 PNG로만 남길 수 있어서, 여러 사람이 하루 동안 번갈아 토핑을 얹은
과정 자체가 결과물에 남지 않는다. 마지막 프레임은 기존 이미지 저장물과 같은 장면이므로,
동영상은 그 이미지에 과정을 덧붙인 확장이다.

## 범위

- **포함**
  - 저장 미리보기 화면(`CanvasImageSaveScreen`)에 동영상 저장 버튼 추가.
  - 오늘 캔버스와 지난(마감된) 캔버스 모두 대상.
  - 토핑 등장 순서는 `ToppingTransform.positionZ` 오름차순.
  - 토핑당 고정 시간 · 팝인 등장 연출 · 무음 mp4(H.264).
  - `Movies/Parfait`에 저장, API 28 이하 권한 요청 경로 포함.
  - 생성 중 `YGLoadingOverlay`로 화면 차단, 결과는 기존 갤러리 저장 토스트로 알림.
- **제외**
  - 배치 당시의 위치·크기 재현. 서버는 `transform` 현재 값만 주므로 **각 토핑은 최종 위치에
    등장**한다. 편집 모드에서 옮긴 이력은 재현할 수 없다.
  - 실제 배치 시각 간격 반영(`createdAt` 기반 가변 속도).
  - 오디오 트랙 · 워터마크 · 그룹명·날짜 오버레이 · 자체 재생 화면 · 외부 공유 시트.
  - 진행률 표시와 생성 취소. 로딩 오버레이 하나로 마감한다.
  - 영상 길이·해상도의 사용자 선택.

## 순서의 근거: positionZ를 쓴다

`CanvasToppingVO`에는 `createdAt`(서버가 주는 배치 시각)과 `transform.positionZ`가 둘 다 있다.
동영상은 `positionZ`를 쓴다.

- `positionZ`는 배치 때 기존 최대값 + 1로 부여된다(`CanvasMainViewModel`·`CanvasToppingPlaceViewModel`의
  `nextPositionZ`). 단조 증가이므로 배치 순서와 같다.
- 현재 캔버스 렌더가 `positionZ` 오름차순이다(`CanvasMainUiState.toppings`). 같은 기준을 쓰면
  **영상의 마지막 프레임이 이미지 저장물과 정확히 같은 장면**이 된다. `createdAt`으로 정렬하면
  두 순서가 어긋나는 경우 마지막 장면이 미리보기와 달라진다.
- `createdAt`은 타임존이 없는 KST 벽시계 값이라 정렬 기준으로 삼기에 근거가 더 약하다.

## 타임라인

30fps 고정. 프레임 인덱스로 진행하므로 기기 성능과 무관하게 같은 결과가 나온다.

| 구간 | 프레임 | 내용 |
|------|--------|------|
| 도입 | 15 (0.5초) | 배경만. 토핑 0개 |
| 토핑 1개당 | 12 (0.4초) | 팝인 8프레임 + 유지 4프레임 |
| 마무리 | 30 (1.0초) | 전체 토핑이 놓인 최종 장면 유지 |

총 프레임은 `15 + 12N + 30`이다. 토핑 10개면 약 5.5초, 30개면 약 13.5초다.

팝인은 등장하는 토핑 하나에만 적용한다. 이미 놓인 토핑은 최종 상태 그대로다.

- `scale`: 0 → 1.08 → 1.0 (오버슈트)
- `alpha`: 0 → 1 (팝인 구간 앞 절반에서 완료)

이징 곡선과 오버슈트 값은 `ToppingVideoTimeline`에 상수로 둔다.

## 인코딩 규격

| 항목 | 값 |
|------|-----|
| 컨테이너 | mp4 (`MediaMuxer`) |
| 코덱 | `video/avc` (H.264) |
| 해상도 | 720 × 1280 (세로 9:16) |
| 프레임레이트 | 30fps |
| 비트레이트 | 6 Mbps |
| 키프레임 간격 | 1초 |
| 오디오 | 없음 |
| 파일명 | `parfait_<epochMillis>.mp4` |

해상도는 캔버스 영역 비율(`CANVAS_AREA_ASPECT_RATIO` = 9/16)을 따른다. 짝수 치수라 H.264
요구를 만족한다.

**인코더는 전용 단일 스레드를 소유한다.** EGL 컨텍스트가 생성 스레드에 묶이고 다른 스레드에서
그리면 예외 없이 조용히 실패하기 때문이다(ADR-0034). 호출부는 어느 스레드에서 불러도 되지만
**그 스레드가 블록된다** — 프레임 루프가 `withFrameNanos` 를 쓰므로 실제 호출부는 UI 스레드이고,
그래서 코덱 대기에는 상한이 있어야 한다. 상한이 없으면 코덱이 멈출 때 실패가 아니라 ANR 이 된다.

## 프레임 합성: 온스크린 컴포지션을 프레임마다 캡처한다

토핑 렌더는 직접 다시 그리기 어렵다. 테두리가 사각 테두리가 아니라 **실루엣 거리장에서 만든
띠**이고(ADR-0030, `ToppingOutline`), 그 띠는 컴포저블 상자 밖으로 굵기만큼 나간다
(`YGToppingCutoutImage`의 경고). 크기는 `TOPPING_BASE_LONG_SIDE_RATIO`와 `ContentScale.Fit`에
걸려 있다. 이 규칙들을 `android.graphics.Canvas`에 다시 구현하면 렌더 경로가 둘로 갈려 영상과
이미지가 어긋날 여지가 생긴다.

그래서 **기존 `CanvasToppingLayer`를 녹화용으로 한 벌 더 띄우고** 프레임마다 읽는다.

- **화면에는 그리지 않는다.** `YGCanvas`가 이미 쓰는 `graphicsLayer.record { drawContent() }`
  패턴에서 `drawLayer` 호출만 빼면, 레이어에는 기록되고 화면에는 나타나지 않는다.
- 사용자에게 보이는 캔버스는 건드리지 않는다. 녹화 중 캔버스가 깜빡이지 않는다.
- `hitTestEnabled = false`로 판정을 끈다. 이미 파라미터로 있다.

### ⚠️ dp 폭은 화면과 같게, 픽셀만 밀도로 고정한다

토핑 테두리 굵기는 **화면 기준 dp 고정**이다(ADR-0025). 녹화 레이어의 dp 폭을 화면의
Canvas-Area와 다르게 잡으면, 캔버스 폭에 대한 테두리 굵기의 비율이 달라져 **영상의 테두리가
이미지보다 두껍거나 얇게** 나온다.

따라서 녹화 레이어는 다음과 같이 잡는다.

- **레이아웃 dp 폭**: 화면에서 측정한 Canvas-Area의 dp 폭을 그대로 받아 쓴다.
- **밀도**: `LocalDensity`를 `목표 픽셀 폭 / 레이아웃 dp 폭`으로 덮어써서 출력 픽셀 크기를
  720 × 1280에 고정한다.

이렇게 하면 dp 기하와 테두리 비율은 화면과 같고, 출력 해상도는 기기에 무관하게 같다.

## 구성 요소

| 조각 | 위치 | 책임 |
|------|------|------|
| `ToppingVideoTimeline` | `feature/groups/canvas/impl/util/` | 토핑 개수 → 프레임 목록(`visibleCount`, `popProgress`). 순수 Kotlin. 수치는 `model/VideoTimelineOptions`, 프레임 타입은 `model/ToppingVideoFrame` |
| 녹화용 오프스크린 레이어 | `feature/groups/canvas/impl/component/` | `CanvasToppingLayer`를 고정 픽셀 크기로 한 벌 더 띄우고 레이어에만 기록 |
| `recordCanvasVideo` | `feature/groups/canvas/impl/util/` | 레이어를 읽어 인코더에 넘긴다. **프레임 루프의 소유자는 Route다** — 상태 전진과 `withFrameNanos` 대기는 Route가 하고 이 함수가 `advanceFrame` 으로 위임받는다 |
| `Mp4VideoEncoder` | `core/util/android/video/` | `MediaCodec` + `MediaMuxer`. 도메인·화면 무지 |
| `BitmapSurfaceWriter` | `core/util/android/video/` | 비트맵을 GL 텍스처로 올려 코덱 입력 표면에 그리는 EGL 통로 |
| 갤러리 저장 | `data/`·`domain/` | `insertPendingVideo` · `saveVideoToGallery` · `SaveCanvasVideoToGalleryUseCase` |

`Mp4VideoEncoder`만 기기 의존이라 계측 테스트 대상이다. 나머지 경계는 JVM에서 검증된다.

## 데이터 흐름

### 결과 타입을 sealed로 가른다

미리보기 화면은 저장 종류만 알려 주고 저장은 하지 않는다. 기존 `CanvasImageSaveResult`를
sealed로 바꾼다.

```kotlin
sealed interface CanvasSaveResult {
    data class Image(val imagePath: String) : CanvasSaveResult
    data object Video : CanvasSaveResult
}
```

같은 화면의 NavKey에는 `toppingCount`가 더해진다. 동영상 버튼의 활성 조건이 토핑 개수인데,
미리보기는 평면 PNG만 받아 그 수를 알 길이 없다.

결과 키(`CANVAS_IMAGE_SAVE_RESULT_KEY`)는 하나로 유지한다. 같은 화면에서 나온 같은 "저장 확정"
사건이고 종류만 다르다. 플래그 필드를 더하지 않는 이유는 영상 갈래가 `imagePath`를 쓰지 않아서다.
필드를 남기면 아무도 읽지 않는 값이 결과에 실린다. `CanvasBackground`가 같은 이유로 sealed다.

### 녹화·인코딩은 Route, 저장은 ViewModel

1. `CanvasMainRoute`의 `ResultEffect`가 `when`으로 갈린다. `Image`는 기존 경로 그대로,
   `Video`는 ViewModel에 녹화 시작을 알린다.
2. ViewModel은 `isRecordingVideo` 상태만 세운다. 그 상태가 참이면 녹화 레이어가 컴포지션에
   들어가고 `YGLoadingOverlay`가 화면을 덮는다.
3. Route가 **토핑 이미지 전부의 로드 완료를 기다린다.** `CanvasToppingLayer`의
   `onLoadStateChange`를 그대로 쓴다. 첫 프레임을 이미지 없이 찍는 사고를 여기서 막는다.
4. 프레임 루프를 돌아 캐시에 mp4를 완성하고, 경로를 `CanvasMainIntent.SaveRecordedVideo`로
   ViewModel에 넘긴다.
5. ViewModel이 `SaveCanvasVideoToGalleryUseCase`를 부르고 결과를 알린다.

녹화를 Route에 두는 이유는 컴포지션 캡처가 화면 계층 책임이라서다.
`RequestCanvasCaptureForPreview`가 이미 같은 구조다(캡처는 Route, 저장은 ViewModel).

### 갤러리 저장

`GalleryMediaProvider`에 `insertPendingVideo`를 더한다. MIME은 `video/mp4`, `RELATIVE_PATH`는
`Movies/Parfait`다. `IS_PENDING` 2단 커밋과 실패 시 등록 되돌리기는 기존 이미지 경로의 구조를
그대로 따른다.

기존 `saveImageToGallery`는 `BitmapWrapper`를 받아 압축하지만, 영상은 이미 파일로 존재한다.
`saveVideoToGallery`는 캐시 파일 경로를 받아 스트림을 복사한다.

### 권한 흐름을 일반화한다

`CanvasMainRoute`는 승인 대기 중인 값을 `pendingGalleryBitmap: Bitmap?`으로 들고 있다. 영상은
비트맵이 아니라 파일 경로라 이 자리를 sealed로 바꿔 두 갈래를 함께 나른다
(`pendingGallerySave`). `GalleryWritePermissionManager`는 그대로 쓴다. API 29 이상은 애초에
걸리지 않는다.

## 표시·제어 규칙

- 동영상 버튼은 **토핑이 0건이면 비활성**이다. 쌓일 것이 없으면 영상이 성립하지 않는다.
  ⚠️ 미리보기 화면은 평면 PNG 경로와 날짜만 받아 **토핑을 셀 수 없다.** 그래서
  `NavKeyCanvasImageSave`에 `toppingCount`를 더해 실어 나른다.
- 생성 중에는 `YGLoadingOverlay`가 화면을 덮고 클릭을 삼킨다. 이 컴포넌트가 이미 클릭을
  삼키므로 별도 차단 장치를 두지 않는다.
- 결과 토스트는 기존 `ShowGallerySaveResult`를 재사용하고 **종류만 실어 문구를 나눈다.** 별도
  effect를 만들지 않는다.
- **토핑이나 배경 이미지 로드가 하나라도 실패하면 녹화를 시작하지 않고** 실패 토스트를 띄운다.
  초판은 "실패한 토핑을 화면과 같은 실패 상태로 영상에 담고 전부 실패할 때만 중단"으로 적었으나
  구현·리뷰를 거쳐 뒤집었다. 근거는 두 가지다. 녹화 레이어는 화면과 **다른 픽셀 크기**로 이미지를
  다시 요청하므로 화면이 성공한 것을 녹화가 실패할 수 있고, 그때 남는 선택은 "실패로 접어 녹화를
  취소"와 "빠진 그림으로 녹화 강행" 둘인데 후자가 더 나쁘다. 갤러리에 남는 산출물은 되돌릴 수 없다.
  실무적으로도 화면 쪽 오류 오버레이가 먼저 막아 이 경로에 닿기 어렵다.

## 실패 처리

| 실패 지점 | 처리 |
|-----------|------|
| 토핑 이미지 로드 전부 실패 | 녹화 시작 안 함. 실패 토스트 |
| 프레임 캡처·인코딩 실패 | 캐시 mp4 삭제. 실패 토스트 |
| 갤러리 등록 후 쓰기 실패 | 기존 `IS_PENDING` 되돌리기가 빈 항목 제거. 실패 토스트 |
| 권한 거부 (API 28 이하) | 실패 토스트. 녹화 결과물은 캐시에서 정리 |

## 테스트

- `ToppingVideoTimeline` — 프레임 수 계산, 팝인 진행도 경계값, 토핑 0·1개. JVM 단위 테스트.
- `CanvasMainViewModel` — 동영상 저장 성공·실패 side effect. 기존 갤러리 저장 테스트와 같은 형태.
- `Mp4VideoEncoder` — `core/util/android/src/androidTest`에 계측 테스트. 이 소스셋이 이미 있어
  새 하니스를 만들지 않는다. mp4가 열리는지와 프레임 수·길이를 `MediaMetadataRetriever`로 확인한다.
- 매퍼 단독 테스트는 만들지 않는다(repo 관례).

## 파일 구성

- [`adr/0034-canvas-video-onscreen-capture-encoding.md`](../../adr/0034-canvas-video-onscreen-capture-encoding.md) — 합성·인코딩 결정
- `feature/groups/canvas/api/` — `CanvasSaveResult` sealed 전환, `NavKeyCanvasImageSave`에 `toppingCount` 추가
- `feature/groups/canvas/impl/util/ToppingVideoTimeline.kt` — 프레임 계산
- `feature/groups/canvas/impl/model/VideoTimelineOptions.kt` · `ToppingVideoFrame.kt` — 타임라인 수치·프레임 타입
- `feature/groups/canvas/impl/component/` — 녹화용 오프스크린 레이어
- `feature/groups/canvas/impl/util/CanvasVideoRecorder.kt` — 프레임 루프
- `core/util/android/video/Mp4VideoEncoder.kt` · `BitmapSurfaceWriter.kt` — 인코더
- `data/`·`domain/` — 갤러리 영상 저장 슬라이스

## 주의 / 열린 질문

- ⚠️ **`GLUtils.texImage2D` 는 HARDWARE config 비트맵을 거부한다**(`IllegalArgumentException:
  invalid Bitmap format`). 그런데 Compose 의 `GraphicsLayer.toImageBitmap()` 은 **API 28 이상에서
  `Bitmap.createBitmap(Picture)` 를 타 HARDWARE 비트맵을 돌려준다.** 그래서 `BitmapSurfaceWriter.draw`
  가 HARDWARE 일 때만 `ARGB_8888` 사본을 만들어 업로드한다. **이 변환을 지우면 사실상 모든 기기에서
  녹화가 항상 실패한다** — 낭비처럼 보이지만 아니다. 프레임마다 전체 해상도 사본이 한 번 생기는
  비용은 피할 수 없다(hwui 가 어차피 되복사한다). 계측 테스트가 HARDWARE 비트맵을 직접 먹여 이
  경로를 잠근다.
- ⚠️ **영상은 녹화 시점의 토핑 목록에서 그려지고, 미리보기 이미지는 그보다 앞서 캡처된 것이다.**
  두 시점 사이에 폴링이 토핑을 더하거나 지우면 "영상 마지막 프레임 = 이미지 저장물"이 어긋난다.
  창은 좁다(미리보기를 열고 버튼을 누르는 사이). 설계의 성질이고 결함이 아니지만, 이 약속을
  인용할 때는 이 예외를 함께 읽어야 한다.
- ⚠️ **스포트라이트를 켠 채 저장하면** 이미지에는 `Black50` 딤과 맨 위로 옮겨진 토핑이 구워지는데
  녹화 레이어에는 딤이 없다. 그래서 저장 요청 시 스포트라이트를 먼저 해제한다.
- ⚠️ **토핑 테두리 판 캐시(`ToppingBorderPlateCache`)가 녹화 밀도에서 미스된다.** 판 열쇠가
  `outsetPx`(밀도 의존)를 포함하므로 녹화용 판은 새로 만들어진다. 판 재생성 자체는 옳지만
  (판이 출력 해상도에 맞아야 한다) 녹화 게이트가 판 생성을 기다리지 않아 **이른 프레임의 토핑이
  테두리 없이 등장할 수 있고**, 캐시 용량(32칸)을 넘기면 화면용 판을 축출해 녹화 후 화면 테두리가
  다시 만들어진다. 처방이 공용 캐시를 건드려야 하고 기기 없이 측정할 수 없어 **실기기 관찰 후
  고치기로 유보했다**(OQ-P-412).

- **버튼 레이블과 하단 버튼 영역 레이아웃이 미정이다.** 현재 하단에는 `YGButton` 하나만 있다.
  두 개를 어떻게 배치할지(나란히·위아래·주/부 강조)는 디자인 확인이 필요하다(OQ-P-411).
- **토스트 문구가 미정이다.** 영상 성공·실패 문구를 디자인에서 받아야 한다(OQ-P-411).
- **정책 원본이 없다.** 이 기능은 기획 문서(`raw/`)에 없는 신규 제안이다. 정책 확정이 붙으면
  타임라인 수치와 해상도를 그 문서에 맞춘다(OQ-P-411).
- **토핑이 매우 많을 때의 상한을 두지 않았다.** 프레임은 인코더로 흘려보내므로 메모리는
  화면 캔버스와 같은 수준이지만, 토핑 60개면 영상이 25초를 넘고 인코딩도 길어진다. 실측 후
  상한이나 속도 조정을 정한다(OQ-P-413).
- **`GalleryMediaProvider`는 API 28 이하에서 `RELATIVE_PATH`를 넣지 않는다.** `minSdk`가 26이라
  그 구간에서는 `Movies/Parfait` 하위 디렉토리가 적용되지 않고 기본 위치에 떨어진다. 기존
  이미지 저장도 같은 문제를 갖고 있다. 이 스펙에서 고치지 않고 별건으로 남긴다(OQ-P-274).
