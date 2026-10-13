---
id: canvas-topping-timelapse-video
title: 캔버스 토핑 타임랩스 동영상 저장 (Canvas Topping Timelapse Video)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-10-13
related_code:
  - CanvasImageSaveScreen.kt#CanvasImageSaveScreen
  - CanvasImageSaveRoute.kt#CanvasImageSaveRoute
  - NavKeyCanvasImageSave.kt#CanvasSaveResult
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasMainViewModel.kt#handleSaveRecordedVideo
  - CanvasToppingLayer.kt#CanvasToppingLayer
  - CanvasVideoRecordLayer.kt#CanvasVideoRecordLayer
  - CanvasVideoRecorder.kt#recordCanvasVideo
  - CanvasVideoSource.kt#CanvasVideoSource
  - CanvasVideoSourceHolder.kt#CanvasVideoSourceHolder
  - ToppingVideoTimeline.kt#toppingPopProgresses
  - VideoTimelineOptions.kt#VideoTimelineOptions
  - YGCanvas.kt#CANVAS_AREA_ASPECT_RATIO
  - ToppingGeometry.kt#TOPPING_BASE_LONG_SIDE_RATIO
  - YGToppingCutoutImage.kt#YGToppingCutoutImage
  - Mp4VideoEncoder.kt#Mp4VideoEncoder
  - BitmapSurfaceWriter.kt#BitmapSurfaceWriter
  - GalleryMediaProvider.kt#insertPendingVideo
  - GalleryRepositoryImpl.kt#saveVideoToGallery
  - SaveCanvasVideoToGalleryUseCase.kt#SaveCanvasVideoToGalleryUseCase
  - GalleryWritePermissionManager.kt#GalleryWritePermissionManager
related_adr: ADR-0035
related_spec:
related_architecture: module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, gallery, video]
---

# Spec: 캔버스 토핑 타임랩스 동영상 저장

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 구현 상태

타임라인·인코더·갤러리 영상 저장·녹화 레이어·미리보기 버튼까지 결선돼 있다. 유닛 테스트는
통과하지만 **실기기에서 영상을 뽑아 본 적이 없다**(OQ-P-429).

| 조각 | 상태 |
|------|------|
| `ToppingVideoTimeline` · `VideoTimelineOptions` | 구현됨 |
| `Mp4VideoEncoder` · `BitmapSurfaceWriter` | 구현됨. 계측 테스트는 CI가 컴파일만 한다(OQ-P-102) |
| 갤러리 영상 저장(`data`·`domain`) | 구현됨 |
| `CanvasToppingLayer` 등장 진행도 · `CanvasVideoRecordLayer` · 프레임 루프 · `CanvasSaveResult` · 미리보기 버튼 · ViewModel 경로 | 구현됨. 실기기 미확인 |

## 목표

캔버스에 토핑이 **쌓인 순서대로 차례로 튀어나오는 동영상**을 만들어 기기 갤러리에 저장한다.
캔버스를 한 장의 PNG로만 남기면 여러 사람이 하루 동안 번갈아 토핑을 얹은 과정이 결과물에
남지 않는다. 마지막 프레임은 이미지 저장물과 같은 장면이므로, 동영상은 그 이미지에 과정을
덧붙인 확장이다.

## 수치의 기준: iOS 저장본

타임라인·팝인 곡선·해상도·버튼 문구·결과 토스트는 iOS 구현(`CanvasImageExporter`·
`CanvasSavePreviewStore`)과 같은 값을 쓴다. 이 기능은 기획 원본(`wiki/raw/`)에 없어 정책
근거가 따로 없고, 두 플랫폼의 산출물이 같아야 한다는 것이 유일한 기준이다(OQ-P-429).

iOS와 다르게 남겨 둔 것은 하나다. 토핑 0건에서 영상 버튼을 막는다.

## 범위

- **포함**
  - 저장 미리보기 화면(`CanvasImageSaveScreen`)의 「영상으로 저장」 버튼.
  - 오늘 캔버스와 지난(마감된) 캔버스 모두 대상.
  - 토핑 등장 순서는 `ToppingTransform.positionZ` 오름차순.
  - 2초 고정 길이 · 팝인 등장 연출 · 무음 mp4(H.264).
  - `Movies/Parfait`에 저장, API 28 이하 권한 요청 경로 포함.
  - 생성 중 미리보기의 두 버튼·닫기·뒤로가기 차단, 결과는 이미지 저장과 같은 토스트로 알림.
- **제외**
  - 배치 당시의 위치·크기 재현. 서버는 `transform` 현재 값만 주므로 **각 토핑은 최종 위치에
    등장**한다. 편집 모드에서 옮긴 이력은 재현할 수 없다.
  - 실제 배치 시각 간격 반영(`createdAt` 기반 가변 속도).
  - 오디오 트랙 · 워터마크 · 그룹명·날짜 오버레이 · 자체 재생 화면 · 외부 공유 시트.
  - 진행률 표시 · 로딩 오버레이 · 생성 취소.
  - 영상 길이·해상도의 사용자 선택.

## 순서의 근거: positionZ를 쓴다

`CanvasToppingVO`에는 `createdAt`(서버가 주는 배치 시각)과 `transform.positionZ`가 둘 다 있다.
동영상은 `positionZ`를 쓴다.

- `positionZ`는 배치 때 기존 최대값 + 1로 부여된다(`CanvasMainViewModel`·`CanvasToppingPlaceViewModel`의
  `nextPositionZ`). 단조 증가이므로 배치 순서와 같다.
- 캔버스 렌더가 `positionZ` 오름차순이다(`CanvasMainUiState.toppings`). 같은 기준을 쓰면
  **영상의 마지막 프레임이 이미지 저장물과 같은 장면**이 된다. `createdAt`으로 정렬하면
  두 순서가 어긋나는 경우 마지막 장면이 미리보기와 달라진다.
- `createdAt`은 타임존이 없는 KST 벽시계 값이라 정렬 기준으로 삼기에 근거가 더 약하다.

## 타임라인

30fps, **2초 고정**(`VideoTimelineOptions.FRAME_COUNT` = 60). 길이는 토핑 수와 무관하다. 프레임
인덱스로 진행하므로 기기 성능과 무관하게 같은 결과가 나온다.

- 토핑 N개의 **시작 시각**을 1.5초 구간에 고르게 나눈다. `order`번째 토핑은 `1.5 × order / N`초에
  시작한다. 첫 토핑은 0초에 시작한다 — 배경만 보이는 도입 구간이 없다.
- 토핑 하나의 팝인은 0.25초다. N이 7 이상이면 시작 간격이 0.25초보다 짧아 **앞뒤 토핑의 팝인이
  겹친다.** 그래서 프레임 상태는 "보이는 개수"가 아니라 토핑별 진행도 목록이다
  (`toppingPopProgresses`).
- 마지막 토핑이 자리 잡은 뒤 남는 시간(0.25초 이상 0.5초 이하)은 완성본을 보여 준다.

팝인은 진행도(0~1)를 배율과 알파로 옮긴다.

- `scale`: 0.6에서 시작해 1을 살짝 넘겼다 1로 돌아온다. `0.6 + 0.4 × easeOutBack(progress)`,
  계수 1.70158.
- `alpha`: 진행도 그대로. 팝인 전체에 걸쳐 선형으로 오른다.

수치는 `model/VideoTimelineOptions`에 모여 있다.

## 인코딩 규격

| 항목 | 값 |
|------|-----|
| 컨테이너 | mp4 (`MediaMuxer`) |
| 코덱 | `video/avc` (H.264) |
| 해상도 | 1080 × 1920 (세로 9:16) |
| 프레임레이트 | 30fps |
| 비트레이트 | 6 Mbps |
| 키프레임 간격 | 1초 |
| 오디오 | 없음 |
| 파일명 | `parfait_<epochMillis>.mp4` |

해상도는 캔버스 영역 비율(`CANVAS_AREA_ASPECT_RATIO` = 9/16)을 따른다. 짝수 치수라 H.264
요구를 만족한다. 비트레이트와 키프레임 간격은 iOS가 값을 지정하지 않아(`AVAssetWriter` 기본값)
맞출 대상이 없다.

**인코더는 전용 단일 스레드를 소유한다.** EGL 컨텍스트가 생성 스레드에 묶이고 다른 스레드에서
그리면 예외 없이 조용히 실패하기 때문이다(ADR-0035). 호출부는 어느 스레드에서 불러도 되지만
**그 스레드가 블록된다** — 프레임 루프가 `withFrameNanos` 를 쓰므로 실제 호출부는 UI 스레드이고,
그래서 코덱 대기에는 상한이 있어야 한다. 상한이 없으면 코덱이 멈출 때 실패가 아니라 ANR 이 된다.

## 프레임 합성: 컴포지션을 프레임마다 캡처한다

토핑 렌더는 직접 다시 그리기 어렵다. 테두리가 사각 테두리가 아니라 **실루엣 거리장에서 만든
띠**이고(ADR-0030, `ToppingOutline`), 그 띠는 컴포저블 상자 밖으로 굵기만큼 나간다
(`YGToppingCutoutImage`의 경고). 크기는 `TOPPING_BASE_LONG_SIDE_RATIO`와 `ContentScale.Fit`에
걸려 있다. 이 규칙들을 `android.graphics.Canvas`에 다시 구현하면 렌더 경로가 둘로 갈려 영상과
이미지가 어긋날 여지가 생긴다.

그래서 **`CanvasToppingLayer`를 녹화용으로 한 벌 띄우고**(`CanvasVideoRecordLayer`) 프레임마다
읽는다.

- **화면에는 그리지 않는다.** `YGCanvas`가 쓰는 `graphicsLayer.record { drawContent() }`
  패턴에서 `drawLayer` 호출만 빼면, 레이어에는 기록되고 화면에는 나타나지 않는다.
- 배경을 토핑 아래에 직접 그린다. 빼면 빈 자리가 알파 0으로 기록돼 영상이 검정으로 나온다.
- `hitTestEnabled = false`, `revealTogether = false`로 판정과 일괄 드러내기를 끈다.

### ⚠️ 녹화 레이어는 저장 미리보기에 붙는다

NavDisplay는 백스택 아래에 깔린 엔트리를 컴포지션에서 뺀다. 미리보기가 떠 있는 동안 캔버스
메인은 컴포지션에 없어 그쪽 캔버스를 읽을 수 없다. 그래서 녹화 레이어는 미리보기 Route가
녹화하는 동안에만 붙이고, 그릴 재료는 캔버스 메인이 미리보기를 열 때 `CanvasVideoSourceHolder`에
넣어 둔다.

### ⚠️ dp 폭은 캔버스 메인과 같게, 픽셀만 밀도로 고정한다

토핑 테두리 굵기는 **화면 기준 dp 고정**이다(ADR-0025). 녹화 레이어의 dp 폭을 캔버스 메인의
Canvas-Area와 다르게 잡으면, 캔버스 폭에 대한 테두리 굵기의 비율이 달라져 **영상의 테두리가
이미지보다 두껍거나 얇게** 나온다.

- **레이아웃 dp 폭**: 캔버스 메인에서 측정한 Canvas-Area의 dp 크기를 `CanvasVideoSource`에 실어
  그대로 쓴다.
- **밀도**: `LocalDensity`를 `목표 픽셀 폭 / 레이아웃 dp 폭`으로 덮어써서 출력 픽셀 폭을
  1080에 고정한다.

## 구성 요소

| 조각 | 위치 | 책임 |
|------|------|------|
| `ToppingVideoTimeline` | `feature/groups/canvas/impl/util/` | 토핑 개수·프레임 인덱스 → 토핑별 진행도, 진행도 → 배율·알파. 순수 Kotlin. 수치는 `model/VideoTimelineOptions` |
| `CanvasVideoSource` · `CanvasVideoSourceHolder` | `impl/model/` · `impl/util/` | 미리보기를 연 순간의 토핑·배경·Canvas-Area dp·`retryKey`. 캔버스 메인이 넣고 미리보기가 읽는다 |
| `CanvasVideoRecordLayer` | `impl/component/` | 배경과 `CanvasToppingLayer`를 고정 픽셀 폭으로 띄우고 레이어에만 기록 |
| `recordCanvasVideo` | `impl/util/` | 레이어를 읽어 인코더에 넘긴다. **프레임 루프의 소유자는 Route다** — 상태 전진과 `withFrameNanos` 대기는 Route가 하고 이 함수가 `advanceFrame` 으로 위임받는다 |
| `Mp4VideoEncoder` | `core/util/android/video/` | `MediaCodec` + `MediaMuxer`. 도메인·화면 무지 |
| `BitmapSurfaceWriter` | `core/util/android/video/` | 비트맵을 GL 텍스처로 올려 코덱 입력 표면에 그리는 EGL 통로 |
| 갤러리 저장 | `data/`·`domain/` | `insertPendingVideo` · `saveVideoToGallery` · `SaveCanvasVideoToGalleryUseCase` |

`Mp4VideoEncoder`만 기기 의존이라 계측 테스트 대상이다. 나머지 경계는 JVM에서 검증된다.

## 데이터 흐름

### 결과 타입

미리보기는 영상을 **만들기까지** 하고, 갤러리에 넣는 일과 결과 알림은 캔버스 메인에 넘긴다.

```kotlin
sealed interface CanvasSaveResult {
    data class Image(val imagePath: String) : CanvasSaveResult
    data class Video(val videoPath: String) : CanvasSaveResult
    data object VideoFailed : CanvasSaveResult
}
```

결과 키(`CANVAS_IMAGE_SAVE_RESULT_KEY`)는 하나다. 같은 화면에서 나온 같은 "저장 확정" 사건이고
종류만 다르다. 갤러리 저장을 미리보기에서 하지 않는 이유는 결과 토스트가 뜨는 자리가 캔버스
메인이라서다 — 미리보기가 저장까지 하고 사라지면 실패를 알릴 곳이 없다.

### 녹화는 미리보기 Route, 저장은 캔버스 메인

1. 캔버스 메인이 미리보기를 열 때 캡처 PNG와 함께 `CanvasVideoSource`를 홀더에 넣는다. 캡처와
   같은 시점의 상태를 한 번 읽어 둘 다 만든다.
2. 미리보기에서 「영상으로 저장」을 누르면 Route가 `isRecordingVideo`를 세운다. 그 동안 두
   버튼이 비활성이고 닫기와 뒤로가기가 막히며, 녹화 레이어가 컴포지션에 들어간다.
3. Route가 **배경과 토핑 이미지 전부의 로드 완료를 기다린다.** 녹화 레이어가 둘을 접어 보고하는
   `onLoadStateChange` 하나만 본다. 첫 프레임을 이미지 없이 찍는 사고를 여기서 막는다.
4. 60프레임을 돌아 캐시에 mp4를 완성하고 `CanvasSaveResult.Video`를 보낸 뒤 닫힌다. 재료가
   없거나 로드·녹화가 실패하면 `VideoFailed`를 보내고 닫힌다.
5. 캔버스 메인의 `ResultEffect`가 `Video`를 권한 흐름에 태워 `CanvasMainIntent.SaveRecordedVideo`로
   넘기고, ViewModel이 `SaveCanvasVideoToGalleryUseCase`를 부른 뒤 `ShowGallerySaveResult`로 알린다.
   `VideoFailed`는 실패 토스트만 띄운다.

미리보기에 ViewModel을 두지 않는다. 녹화는 컴포지션 캡처라 화면 계층 책임이고, 그 밖에 들 상태가
`isRecordingVideo` 하나뿐이다.

### 갤러리 저장

`GalleryMediaProvider.insertPendingVideo`의 MIME은 `video/mp4`, `RELATIVE_PATH`는 `Movies/Parfait`다.
`IS_PENDING` 2단 커밋과 실패 시 등록 되돌리기는 이미지 경로와 같은 구조다. 영상은 이미 파일로
존재하므로 `saveVideoToGallery`는 캐시 파일 경로를 받아 스트림을 복사한다.

### 권한 흐름

`CanvasMainRoute`는 승인 대기 중인 값을 sealed `PendingGallerySave`(`Image`는 비트맵, `Video`는
파일 경로)로 들고 있다. `GalleryWritePermissionManager`는 이미지와 같이 쓴다. API 29 이상은
애초에 걸리지 않는다.

## 표시·제어 규칙

- 하단에 버튼 둘을 같은 폭으로 나란히 둔다. 왼쪽이 「영상으로 저장」(Secondary), 오른쪽이
  「갤러리에 저장」(Primary)이다.
- 영상 버튼은 **토핑이 0건이면 비활성**이다. 미리보기는 평면 PNG 경로와 날짜만 받아 토핑을 셀
  수 없어 `NavKeyCanvasImageSave.toppingCount`로 실어 나른다.
- 생성 중에는 화면을 덮지 않는다. 두 버튼을 비활성으로 두고 닫기 버튼과 시스템 뒤로가기를
  막는다 — 만드는 중에 나가면 결과를 알릴 길이 없다.
- 결과 토스트는 이미지 저장과 **같은 문구**다(`canvas_main_gallery_save_success`·
  `canvas_main_gallery_save_failure`). `ShowGallerySaveResult`는 종류를 싣지 않는다.
- **토핑이나 배경 이미지 로드가 하나라도 실패하면 녹화를 시작하지 않고** 실패로 닫는다. 녹화
  레이어는 화면과 **다른 픽셀 크기**로 이미지를 다시 요청하므로 화면이 성공한 것을 녹화가 실패할
  수 있고, 그때 "빠진 그림으로 녹화 강행"은 "실패로 접기"보다 나쁘다. 갤러리에 남는 산출물은
  되돌릴 수 없다.

## 실패 처리

| 실패 지점 | 처리 |
|-----------|------|
| 홀더가 비어 있음(프로세스 재시작) · Canvas-Area 미측정 | 녹화 시작 안 함. `VideoFailed` |
| 토핑·배경 이미지 로드 실패 | 녹화 시작 안 함. `VideoFailed` |
| 프레임 캡처·인코딩 실패 | 캐시 mp4 삭제. `VideoFailed` |
| 갤러리 등록 후 쓰기 실패 | `IS_PENDING` 되돌리기가 빈 항목 제거. 실패 토스트 |
| 권한 거부 (API 28 이하) | 실패 토스트 |

화면을 떠나 녹화 코루틴이 취소된 것은 실패로 접지 않는다(`recordCanvasVideo`가
`CancellationException`을 다시 던진다).

## 테스트

- `ToppingVideoTimeline` — 프레임 수, 시작 시각 분배, 팝인 겹침, 마지막 프레임, 배율·알파 경계값.
  JVM 단위 테스트.
- `CanvasMainViewModel` — `SaveRecordedVideo` 성공·실패 side effect.
- `Mp4VideoEncoder` · `BitmapSurfaceWriter` — `core/util/android/src/androidTest` 계측 테스트.
  mp4가 열리는지와 프레임 수·길이를 `MediaMetadataRetriever`로 확인한다.
- 미리보기 Route의 녹화 흐름에는 테스트가 없다.

## 주의 / 열린 질문

- ⚠️ **`GLUtils.texImage2D` 는 HARDWARE config 비트맵을 거부한다**(`IllegalArgumentException:
  invalid Bitmap format`). 그런데 Compose 의 `GraphicsLayer.toImageBitmap()` 은 **API 28 이상에서
  `Bitmap.createBitmap(Picture)` 를 타 HARDWARE 비트맵을 돌려준다.** 그래서 `BitmapSurfaceWriter.draw`
  가 HARDWARE 일 때만 `ARGB_8888` 사본을 만들어 업로드한다. **이 변환을 지우면 사실상 모든 기기에서
  녹화가 항상 실패한다** — 낭비처럼 보이지만 아니다. 프레임마다 전체 해상도 사본이 한 번 생기는
  비용은 피할 수 없다(hwui 가 어차피 되복사한다). 계측 테스트가 HARDWARE 비트맵을 직접 먹여 이
  경로를 잠근다.
- ⚠️ **팝인 중에는 토핑 테두리 띠가 잘린다.** 띠가 컴포저블 상자 밖으로 나가는데 `graphicsLayer`의
  알파가 1 미만이면 상자 경계에서 잘린다. 알파가 팝인 전체(0.25초)에 걸쳐 오르므로 그 동안
  테두리가 덜 보이다가 팝인이 끝나는 프레임에 온전해진다. 실기기에서 눈에 띄는지 본 적이 없다
  (OQ-P-429).
- ⚠️ **스포트라이트를 켠 채 저장하면** 이미지에는 `Black50` 딤과 맨 위로 옮겨진 토핑이 구워지는데
  녹화 레이어에는 딤이 없다. 그래서 저장 요청 시 스포트라이트를 먼저 해제한다.
- ⚠️ **토핑 테두리 판 캐시(`ToppingBorderPlateCache`)가 녹화 밀도에서 미스된다.** 판 열쇠가
  `outsetPx`(밀도 의존)를 포함하므로 녹화용 판은 새로 만들어진다. 판 재생성 자체는 옳지만
  (판이 출력 해상도에 맞아야 한다) 녹화 게이트가 판 생성을 기다리지 않아 **이른 프레임의 토핑이
  테두리 없이 등장할 수 있고**, 캐시 용량(32칸)을 넘기면 화면용 판을 축출해 녹화 후 화면 테두리가
  다시 만들어진다. 처방이 공용 캐시를 건드려야 하고 기기 없이 측정할 수 없어 **실기기 관찰 후
  고치기로 유보했다**(OQ-P-433).
- ⚠️ **홀더는 프로세스 사망을 넘지 못한다.** 미리보기가 복원되면 PNG는 캐시 파일에서 다시 읽지만
  영상 재료는 없어 「영상으로 저장」이 실패 토스트로 끝난다.
- **1080 × 1920 인코딩의 생성 시간을 재지 않았다.** 프레임마다 전체 해상도 GPU 읽기와 사본이
  한 번씩 일어난다. 그 동안 진행 표시 없이 버튼만 잠겨 있다(OQ-P-429).
- **`GalleryMediaProvider`는 API 28 이하에서 `RELATIVE_PATH`를 넣지 않는다.** `minSdk`가 26이라
  그 구간에서는 `Movies/Parfait` 하위 디렉토리가 적용되지 않고 기본 위치에 떨어진다. 이미지
  저장도 같은 문제를 갖고 있다. 이 스펙에서 고치지 않고 별건으로 남긴다(OQ-P-274).
