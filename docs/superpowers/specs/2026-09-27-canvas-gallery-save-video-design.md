---
id: canvas-gallery-save-video
title: 캔버스 갤러리 동영상 저장 (Canvas Gallery Video Save)
status: draft
category: behavior-spec
platforms: android
verified: 2026-09-27
related_code:
  - YGCanvas.kt#YGCanvas
  - CanvasImageSaveScreen.kt#CanvasImageSaveScreen
  - CanvasImageSaveRoute.kt#CanvasImageSaveRoute
  - CanvasCaptureHolder.kt#CanvasCaptureHolder
  - CanvasMainRoute.kt#CanvasMainRoute
  - CanvasMainViewModel.kt#handleClickSaveToGallery
  - CanvasMainViewModel.kt#handleSaveCapturedCanvas
  - SaveCanvasToGalleryUseCase.kt#SaveCanvasToGalleryUseCase
  - GalleryRepositoryImpl.kt#saveImageToGallery
  - GalleryMediaProvider.kt#insertPendingImage
  - GalleryMediaProvider.kt#openOutputStream
  - GalleryMediaProvider.kt#finalizePendingImage
  - GalleryMediaProvider.kt#deleteImage
  - ToppingTransform.kt#ToppingTransform
  - CanvasToppingVO.kt#CanvasToppingVO
  - CanvasVideoSourceHolder.kt#CanvasVideoSourceHolder
  - CanvasVideoFrameRenderer.kt#CanvasVideoFrameRenderer
  - CanvasVideoEncoder.kt#CanvasVideoEncoder
  - SaveCanvasVideoToGalleryUseCase.kt#SaveCanvasVideoToGalleryUseCase
related_adr:
related_spec: canvas-save-preview-capture-holder, c001-canvas-gallery-save
related_architecture: data-layer, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, gallery, video, c-001]
---

# Spec: 캔버스 갤러리 동영상 저장

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

## 목표

C-001 캔버스 저장 미리보기(`CanvasImageSaveScreen`)에서 정지 이미지 저장과 별개로, 토핑이
서버 겹침 순서(`positionZ`) 대로 하나씩 나타나는 짧은 동영상을 만들어 기기 갤러리에 저장한다.
토핑이 실제로 드래그된 궤적을 재현하지 않는다 — 각 토핑은 최종 배치 좌표·스케일·회전 그대로,
등장 순서만 애니메이션(페이드인+슬라이드인)으로 표현한다.

## 범위

- **포함**: 토핑 정렬 기준 확정(`positionZ` 단일 기준) · 프레임 합성(배경+토핑 Matrix 렌더링) ·
  media3 Transformer 기반 mp4 인코딩 · `GalleryMediaProvider`의 MediaStore Video 저장 확장 ·
  미리보기 화면의 "동영상으로 저장" 버튼(토핑 개수 무관 항상 노출) · 인코딩 중 로딩 상태 ·
  성공/실패 결과 통지.
- **제외**: 토핑 드래그 궤적 재현 · 오디오 트랙 · 전환 방식 커스터마이징(페이드인+슬라이드인
  고정) · 영상 해상도·화질 프리셋 선택 UI · API 28 이하 기기의 저장 위치 차이 해소(기존
  [c001-canvas-gallery-save](archive/2026-08-23-c001-canvas-gallery-save.md)의 OQ-P-274를
  그대로 물려받는다) · 서버 스키마 변경(기존 응답 필드만 사용).

## 순서 기준

서버가 부여하는 `positionZ`(Int, 겹침 순서)를 **등장 순서와 최종 스택 깊이 양쪽에 동일하게**
쓴다. `createdAt`(생성 타임스탬프)은 쓰지 않는다 — 두 기준이 갈리면 영상 재생 중 이미 그려진
토핑이 뒤늦게 스택 순서가 바뀌는 것처럼 보이는 "튐" 현상이 생긴다. 동시 배치 시 `positionZ`
산정이 흔들릴 수 있는 기존 결함(OQ-P-322, `docs/status.md` "토핑 생성·배치" 절)은 이 기능에도
동일하게 적용되는 것으로 인지하고 별도 방어를 새로 만들지 않는다 — 정지 이미지 저장이 이미 같은
전제(현재 캔버스 상태 그대로 캡처) 위에 서 있다.

## API / 인터페이스

```kotlin
// domain
interface GalleryRepository {
    suspend fun saveImageToGallery(bitmap: BitmapWrapper, fileName: String): Result<Unit>
    suspend fun saveVideoToGallery(videoFile: File, fileName: String): Result<Unit>
}

class SaveCanvasVideoToGalleryUseCase(
    private val galleryRepository: GalleryRepository,
) {
    suspend operator fun invoke(videoFile: File, date: LocalDate): Result<Unit>
}

// data
class CanvasVideoFrameRenderer {
    fun renderFrames(
        background: Bitmap?,
        toppings: List<ToppingRenderSnapshot>, // positionZ 오름차순 정렬 전제
        outputDir: File,
    ): List<File> // 순서대로 저장된 프레임 PNG 경로
}

data class ToppingRenderSnapshot(
    val bitmap: Bitmap,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
)

class CanvasVideoEncoder {
    suspend fun encode(frames: List<File>, frameDurationMs: Long, outputFile: File): Result<Unit>
}
```

- `frameDurationMs`는 렌더러가 만든 개별 프레임 각각의 노출 시간이다(토핑당 애니메이션 총
  시간이 아니라, 그 애니메이션을 구성하는 프레임 1장의 길이).
- `saveVideoToGallery`는 기존 `saveImageToGallery`와 나란한 계약이고, 압축 포맷만 다르다
  (`GalleryMediaProvider`가 내부에서 `MediaStore.Images` 대신 `MediaStore.Video` 컬렉션을 쓴다).

## 동작 / 흐름

```
[CanvasImageSaveScreen] 동영상으로 저장 클릭
        │
        ▼
CanvasVideoSourceHolder.peek() ── 배경 + positionZ 정렬된 토핑 스냅샷 리스트
        │
        ▼
CanvasVideoFrameRenderer.renderFrames()
   - 배경을 첫 프레임에 그린다
   - 토핑을 positionZ 순서대로 순회, 각 토핑마다 고정 시간만큼
     페이드인(alpha 0→1)+슬라이드인(offset→최종 좌표) 프레임 생성
   - 마지막 토핑 등장 후 정지 프레임 유지(홀드)
        │
        ▼ (임시 캐시 디렉토리에 프레임 PNG 순차 저장)
CanvasVideoEncoder.encode() ── media3 Transformer: 프레임 시퀀스 → H264 mp4
        │
        ▼
SaveCanvasVideoToGalleryUseCase → GalleryRepository.saveVideoToGallery
        → GalleryMediaProvider (MediaStore.Video, IS_PENDING 왕복)
        │
        ▼
임시 프레임/mp4 캐시 정리 → 결과 이벤트 → 캔버스 메인 토스트
```

### 토핑 데이터 확보 — `CanvasVideoSourceHolder`

`CanvasCaptureHolder`(정지 이미지 캡처 홀더)는 합성 완료된 비트맵 1장만 들고 있어 토핑별
개별 정보가 없다. 동영상 생성을 위해 같은 패턴의 새 전역 홀더 `CanvasVideoSourceHolder`를
둔다 — `CanvasMainRoute`가 저장 미리보기로 진입하는 시점에 배경 비트맵과, 화면에 이미 렌더링
중인 토핑 리스트(`positionZ` 오름차순 정렬, 각 토핑의 이미지는 Coil 캐시에서 동기 디코딩)를
채워 넣는다.

⚠️ 정지 이미지 캡처와 동일한 전제를 물려받는다 — 토핑 이미지가 비동기 로딩 중이면 그 상태
그대로 프레임에 담긴다(OQ-P-272와 동일 계열 결함, 별도로 새로 막지 않는다). 다만 프레임
렌더링 시작 전 **모든 토핑 Bitmap의 디코딩 완료를 기다리는 것**은 새로 추가한다 — 정지
이미지와 달리 여러 프레임에 걸쳐 그리므로, 중간에 이미지가 늦게 도착하면 일부 프레임만
비어 보이는 것을 막기 위함이다. 대기가 일정 시간을 넘으면 실패로 처리한다(§ 에러 처리).

### 프레임 렌더링

`android.graphics.Canvas` + `Matrix`로 그린다(Compose 오프스크린 재구성 없이). 좌표계·변환
순서는 `YGCanvas`가 화면에 토핑을 배치하는 수식을 그대로 이식한다 — 두 경로가 다른 결과를
내면 정지 이미지와 동영상 마지막 프레임의 토핑 위치가 어긋난다.

### 인코딩

`androidx.media3:media3-transformer`로 프레임 PNG 시퀀스를 이어 붙여 mp4를 만든다. 신규
의존성(`media3-transformer`, `media3-effect`, `media3-common`)을 추가한다 — 이 저장소에
동영상 인코딩 코드/라이브러리가 지금까지 전혀 없었다(전수 검색 결과 없음).

## 표시·제어 규칙

- "동영상으로 저장" 버튼은 토핑 개수(0개 포함)와 무관하게 항상 노출한다. 토핑 0~1개인
  캔버스에서는 사실상 정지 이미지와 큰 차이가 없는 짧은 영상이 만들어지지만, 별도 숨김
  조건을 두지 않는다.
- 인코딩은 프레임 렌더링+mp4 변환이 걸려 즉시 끝나지 않는다 — 버튼 클릭 시 로딩 상태를
  표시하고, 완료 전까지 중복 클릭을 막는다(기존 `SAVE_CANVAS_TO_GALLERY_KEY` 중복 실행
  가드와 같은 패턴).
- 저장 진행 중 사용자가 미리보기 화면을 벗어나도 저장 코루틴은 화면 생명주기와 분리해
  계속 진행하고, 완료 시 결과를 토스트로 알린다(정지 이미지 저장과 동일 패턴).

## 에러 처리

- 토핑 이미지 디코딩 대기 타임아웃: 저장 취소 + 실패 토스트.
- 프레임 렌더링/인코딩 중 예외(디스크 부족 등): 실패 토스트, `finally`에서 임시 프레임 파일과
  mp4 캐시를 정리한다.
- `GalleryMediaProvider` MediaStore 쓰기 실패: 기존 이미지 저장과 동일하게 `IS_PENDING` 항목을
  `deleteImage`로 롤백한다.

## 계층 배치

| 계층 | 심볼 | 하는 일 |
|---|---|---|
| `feature` | `CanvasVideoSourceHolder` | 배경+정렬된 토핑 스냅샷 보관(정지 이미지 홀더와 나란함) |
| `feature` | `CanvasImageSaveScreen` | "동영상으로 저장" 버튼, 로딩 상태 |
| `data` | `CanvasVideoFrameRenderer` | Canvas+Matrix 프레임 합성 |
| `data` | `CanvasVideoEncoder` | media3 Transformer로 mp4 인코딩 |
| `data` | `GalleryMediaProvider` | MediaStore Video 왕복(등록·쓰기·확정·삭제) 추가 |
| `data` | `GalleryRepositoryImpl` | `saveVideoToGallery` |
| `domain` | `GalleryRepository`·`SaveCanvasVideoToGalleryUseCase` | 저장 계약 |

## 파일 구성

| 파일 | 변경 |
|---|---|
| `feature/.../util/CanvasVideoSourceHolder.kt` | 신규 |
| `feature/.../screen/CanvasImageSaveScreen.kt` | 버튼·로딩 상태 추가 |
| `feature/.../route/CanvasImageSaveRoute.kt` | 저장 트리거 연결 |
| `data/.../video/CanvasVideoFrameRenderer.kt` | 신규 |
| `data/.../video/CanvasVideoEncoder.kt` | 신규(media3 Transformer 래핑) |
| `data/.../utils/GalleryMediaProvider.kt` | Video 컬렉션 등록·쓰기·확정·삭제 추가 |
| `data/.../repository/gallery/GalleryRepositoryImpl.kt` | `saveVideoToGallery` |
| `domain/.../repository/gallery/GalleryRepository.kt` | 계약 추가 |
| `domain/.../usecase/gallery/SaveCanvasVideoToGalleryUseCase.kt` | 신규 |
| `gradle/libs.versions.toml`, `data/build.gradle.kts` | `media3-transformer`/`-effect`/`-common` 의존성 추가 |

## 주의 / 열린 질문

- 프레임 합성 Matrix 계산은 순수 함수로 분리해 유닛 테스트 가능하나, media3 Transformer
  인코딩 자체는 에뮬레이터/기기 통합 테스트가 필요하다(유닛 테스트로 못 덮는 영역).
- `positionZ` 동시 배치 흔들림(OQ-P-322)이 동영상 등장 순서에도 그대로 전이된다 — 별도
  이슈로 새로 열지 않고 기존 미결에 편입한다.
- 토핑 이미지 비동기 로딩 결함(OQ-P-272)과 API 28 이하 저장 위치 차이(OQ-P-274)를 동영상
  저장 경로도 동일하게 물려받는다.
