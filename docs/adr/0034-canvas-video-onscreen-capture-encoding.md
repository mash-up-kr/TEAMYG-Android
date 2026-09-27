---
id: ADR-0034
title: 캔버스 동영상은 컴포지션을 프레임마다 캡처해 MediaCodec으로 인코딩한다
status: proposed
date: 2026-09-27
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0025, ADR-0030
related_spec: canvas-topping-timelapse-video
related_architecture: module-structure.md
platforms: android
tags: [adr, parfait, canvas, video]
---

# ADR-0034: 캔버스 동영상은 컴포지션을 프레임마다 캡처해 MediaCodec으로 인코딩한다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

캔버스에 토핑이 쌓인 순서대로 등장하는 동영상을 만들어 갤러리에 저장해야 한다
([스펙 `canvas-topping-timelapse-video`](../superpowers/specs/2026-09-21-canvas-topping-timelapse-video.md)). 두 가지를 정해야 한다. **프레임을 무엇으로 그리는가**와
**무엇으로 인코딩하는가**다.

토핑 렌더는 옮겨 심기 어렵다. 테두리가 사각형이 아니라 실루엣 거리장에서 만든 띠이고
(ADR-0030), 그 띠는 컴포저블 상자 밖으로 굵기만큼 나간다(`YGToppingCutoutImage`). 굵기는 화면
기준 dp로 고정되어 있고(ADR-0025), 크기는 `TOPPING_BASE_LONG_SIDE_RATIO`와 `ContentScale.Fit`
규칙에 걸려 있다. 캔버스 밖으로 나간 배치를 잘라 내는 클리핑 규약도 있다.

인코딩 쪽은 저장소에 선례가 없다. `MediaCodec`·`MediaMuxer`·Media3·ffmpeg 어느 것도 쓰이지
않는다. CameraX는 있지만 `camera-video` 모듈은 붙어 있지 않다.

## 결정

**기존 `CanvasToppingLayer`를 녹화용으로 한 벌 더 띄워 프레임마다 캡처하고, `MediaCodec` +
EGL 입력 표면 + `MediaMuxer`로 직접 mp4를 쓴다. 외부 의존성은 더하지 않는다.**

- 녹화 레이어는 **화면에 그리지 않는다.** `YGCanvas`가 이미 쓰는
  `graphicsLayer.record { drawContent() }` 패턴에서 `drawLayer` 호출만 뺀다. 레이어에는
  기록되고 화면에는 나타나지 않는다. 사용자에게 보이는 캔버스는 건드리지 않는다.
- 녹화 레이어의 **레이아웃 dp 폭은 화면 Canvas-Area와 같게** 두고, `LocalDensity`만
  `목표 픽셀 폭 / 레이아웃 dp 폭`으로 덮어써서 출력 해상도를 고정한다. 테두리 굵기가 dp 고정이라
  dp 폭이 달라지면 캔버스 폭에 대한 굵기 비율이 달라져 영상이 이미지와 어긋난다.
- 프레임 진행은 벽시계가 아니라 **프레임 인덱스**로 한다. 상태를 한 칸 전진시키고
  `withFrameNanos`로 그려질 때까지 기다린 뒤 `GraphicsLayer.toImageBitmap`으로 읽는다. 기기
  성능이 결과를 바꾸지 않는다.
- 인코더(`Mp4VideoEncoder`)와 EGL 통로(`BitmapSurfaceWriter`)는 `core/util/android/video/`에 둔다. 도메인과 화면을 모르는 조각이고, 그 모듈에
  계측 테스트 소스셋이 이미 있다.
- 녹화·인코딩은 Route 계층이, 갤러리 저장은 ViewModel이 맡는다.
  `RequestCanvasCaptureForPreview`가 이미 같은 분담이다.

## 대안

- **CPU `android.graphics.Canvas`로 프레임을 직접 합성** — 화면 없이 돌고, 출력 해상도를 기기와
  무관하게 고정하기 쉽고, 합성 로직을 순수 함수로 테스트할 수 있다. 거리장 유틸(`ToppingOutline`,
  `toBorderAlphaBitmap`)이 Compose 밖에서도 호출되므로 테두리 재사용 자체는 가능하다.
  그러나 배치 수식·`ContentScale.Fit` 반올림·테두리 outset·클리핑 규약을 두 번째로 구현하게 되고,
  그 사본이 화면 렌더와 어긋나는 순간이 곧 버그다. 렌더 규칙이 앞으로 바뀔 때마다 두 곳을 같이
  고쳐야 한다.
  **→ 기각:** 마지막 프레임이 이미지 저장물과 같아야 한다는 요구를 구조로 보장하지 못한다.
- **Media3 Transformer 도입** — 구글이 관리하는 공식 경로이고 인코더 파편화를 라이브러리가
  흡수한다. 그러나 이미지 입력 단위가 "정지 이미지 N초"라, 팝인 보간 프레임을 넣으려면 프레임마다
  파일을 굽고 `EditedMediaItem`을 수백 개 이어 붙여야 한다. 토핑 20개 기준 240장이다.
  **→ 기각:** 팝인 연출과 궁합이 나쁘고, 그 대가로 의존성 세 개가 늘어난다.
- **`MediaCodec` ByteBuffer 모드 + 직접 YUV 변환** — EGL 보일러플레이트가 사라지고 색변환을
  순수 함수로 테스트할 수 있다. 그러나 지원 컬러 포맷이 기기마다 갈려
  (`COLOR_FormatYUV420Planar`·`SemiPlanar`·`Flexible`) 조회·분기 코드가 필요하고, `minSdk` 26
  범위에서 이 파편화가 실제 버그로 드러난다. 색변환도 CPU라 프레임이 수백 장이면 느리다.
  **→ 기각:** EGL 코드를 아끼는 대가로 기기 파편화를 직접 떠안는다.

## 영향

**긍정**

- 렌더 경로가 하나다. 영상과 이미지의 픽셀 불일치가 원천적으로 생기지 않는다. 토핑 렌더 규칙이
  바뀌어도 영상이 저절로 따라온다.
- 외부 의존성이 늘지 않는다. APK 크기와 빌드 시간에 영향이 없다.
- 하드웨어 인코더를 쓰고, 컬러 포맷 변환을 GPU가 처리한다.
- 출력 해상도가 기기에 무관하게 같다. 밀도 덮어쓰기로 dp 기하와 픽셀 크기를 분리했다.
- 순서·타이밍 계산이 순수 Kotlin으로 떨어져 JVM에서 검증된다. 기기 테스트 대상은 인코더 하나다.

**트레이드오프**

- 녹화가 컴포지션 생존에 묶인다. 화면이 떠 있어야 하고, 그 동안 로딩 오버레이로 사용자 조작을
  막아야 한다. 백그라운드 생성은 불가능하다.
- EGL 초기화 보일러플레이트를 직접 들고 있어야 한다. 대신 `core/util/android`에 한 벌만 둔다.
- `GraphicsLayer.toImageBitmap` GPU 읽기가 프레임마다 일어난다. 프레임 수가 곧 비용이다.
- `CanvasToppingLayer`에 등장 진행도를 주입할 파라미터가 필요하다. 이미 파라미터가 많은
  컴포저블이 더 넓어진다.

**위험·방어**

- **인코더가 없거나 표면 입력을 못 받는 기기** — `MediaCodec` 생성 실패를 `Result` 실패로
  접어 실패 토스트로 마감한다. 캐시 mp4와 갤러리 등록을 함께 되돌린다.
- **첫 프레임이 이미지 없이 찍히는 사고** — `CanvasToppingLayer`의 `onLoadStateChange`로 토핑
  이미지 로드 완료를 기다린 뒤에 루프를 시작한다.
- **테두리 비율 어긋남** — 같은 캔버스를 이미지와 영상 마지막 프레임으로 각각 뽑아 대조한다.
  어긋나면 dp 폭 전달 경로부터 본다. 녹화 레이어가 아직 없어 대조한 적이 없다(OQ-P-411).
- **mp4가 재생되지 않는 산출물** — 계측 테스트가 `MediaMetadataRetriever`로 길이를,
  `MediaExtractor`로 샘플 수를 확인한다.
- **HARDWARE 비트맵 거부** — `GLUtils.texImage2D`가 `Config.HARDWARE`를 거부하는데
  `GraphicsLayer.toImageBitmap()`은 API 28 이상에서 그것을 돌려준다. `BitmapSurfaceWriter.draw`가
  업로드 전에 `ARGB_8888`로 복사하고, 계측 테스트가 HARDWARE 비트맵을 직접 넣어 이 경로를 잠근다.
