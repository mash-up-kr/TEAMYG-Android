---
id: segmentation-preprocessing
title: 세그멘테이션 입력 전처리 — 촬영 손실·해상도 하한·회전 정합 (Subject Segmentation input preprocessing)
status: in-progress
category: behavior-spec
platforms: android
verified: 2026-08-25
related_code: ImageSegmentationRepositoryImpl#decodeImage, ContentResolver.kt#decodeUriToBitmap, CameraPreviewComponent.kt, CameraCrop.kt#saveViewfinderCapture, FileCameraCacheLocalDataSourceImpl#createFile, CreateCameraCacheFileUseCase, CreateCameraCacheUriUseCase, RecentImageRepositoryImpl#extensionOf, ExifOrientation.kt#exifOrientationToDegrees, ContentResolver.kt#rotatedToUpright, ImageFileLocalDataSourceImpl, DecodeImageUseCase, SegmentationViewModel, ToppingEditViewModel, ToppingEditMask.kt#buildCutoutBitmap, NavKeyToppingEdit
related_adr: ADR-0012
related_spec: c103-multi-subject-selection, segmentation-pipeline-hardening, c106-topping-place-api
related_architecture: data-layer
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 세그멘테이션 입력 전처리

## 목표

> ✅ **as-built 1단계(2026-08-25, PR #349 `a5e8a760` develop 머지)** — **근거가 확정돼 있던 항목만
> 들어갔다.** 촬영 품질 상향(`CAPTURE_MODE_MAXIMIZE_QUALITY` + `setJpegQuality(100)`), API 28 미만
> 갈래의 EXIF 회전 보정(`rotatedToUpright`·`readExifDegrees`·`exifOrientationToDegrees` + 모듈 로거
> `coreUtilAndroidLogger` 신설), `androidx.exifinterface` 의존 추가, 그리고 최근 이미지 `SOURCE`
> 확장자의 바이트 판정(`extensionOf`)이다. **마지막 항목은 아래 표가 "PNG 채택 시"로 분류했으나
> 계획이 1단계로 앞당겼다** — PNG 없이도 갤러리에서 고른 PNG 원본이 `.jpg` 이름으로 앉는 것이
> 지금도 참인 결함이기 때문이다(그 감수는 OQ-P-283).
>
> **조건부 항목 넷은 아직 하나도 안 들어갔다** — 짧은 변 512 확대, PNG 저장 전환, API 28 이상 회전
> 보정, 그리고 그 판정을 내리는 고정 사진 세트 측정이 전부 미착수다. 아래 본문에서 현재 코드를
> 단정하는 문장은 **1단계가 이미 고친 자리에 한해** 낡았다(촬영 빌더·`fileExtension`). 근거로서의
> 서술은 그대로 두고 바뀐 자리에만 표시를 달았다.
>
> 📌 **512 확대가 재시도 경로에 한해 들어왔다**(2026-09-10, PR #487). `decodeImage` 전역 확대는 여전히
> 미착수다. 후보 0건 뒤 재시도 사다리만 검출 입력을 짧은 변 512·긴 변 2048로 맞추고, 후보 픽셀은 원본에서
> 오려내므로 OQ-P-282의 크기 전파가 없다. 이 스펙이 예약한 `SegmentationInputNormalizer.kt` 이름은 쓰지 않았다
> (`SegmentationRecoveryNormalizer.kt`) → [segmentation-retry-recovery](archive/2026-09-10-segmentation-retry-recovery.md).

누끼의 **정확도**를 올린다. 대상을 더 잘 얻는 것이 목표이고 지연·메모리는 목표가 아니다.
세그멘테이션이 보는 픽셀이 만들어지는 자리를 손봐서, 모델에게 원본에 가장 가까운 입력을 준다.

## 근거 등급 (이 스펙의 성격)

**관찰된 실패 사례가 없는 상태에서 쓰는 스펙이다.** 사용자가 느린 것도 실패하는 것도 본 적 없고,
"누끼가 더 잘 나왔으면 좋겠다"가 출발점이다. 그래서 항목마다 근거의 등급과 **철회 조건**을 적는다.
등급이 낮은 항목은 검증 절의 사진 세트가 판정한 뒤에 남길지 정한다. **이 표가 이 스펙의 계약이다.**

| 항목 | 근거 | 판정 |
|---|---|---|
| `ImageCapture` 촬영 품질 상향 | 손실이 실제로 처음 생기는 자리다. 비용이 설정 한 줄이다 | 무조건 넣는다 → ✅ 머지(#349) |
| API 26·27 EXIF 회전 보정 | 그 갈래가 EXIF를 안 먹인다는 것이 코드로 확정된다 | 무조건 넣는다 → ✅ 머지(#349) |
| API 28 이상 추가 회전 보정 | `ImageDecoder`의 EXIF 적용 여부를 확인하지 못했다 | 조건부(OQ-P-280) → 미착수 |
| 짧은 변 512 하한 확대 | 문서가 하한을 명시하나 확대로 회복된다고는 말하지 않는다 | 조건부(OQ-P-278) → 전역 미착수(재시도 경로에만 PR #487) |
| 촬영 저장을 PNG로 | 손실의 두 번째 세대만 없앤다. 첫 세대가 더 크다 | 조건부(아래 경고) → 미착수 |

> ⚠️ **PNG 전환은 처음 생각보다 근거가 약하다.** `CameraPreviewComponent.kt`의
> `ImageCapture.Builder().build()`가 출력 포맷·품질·capture mode를 아무것도 지정하지 않아
> **기본값인 JPEG로 촬영된다.** `ImageProxy.toBitmap()`이 디코드하는 것은 이미 압축된 프레임이다.
> 즉 우리가 저장하며 만드는 손실은 **두 번째 세대**이고, 경계 아티팩트의 큰 몫은 첫 세대에서
> 결정된다. PNG 전환의 비용(파일 크기·인코딩 시간·업로드 용량·캐시 누적)은 이 라운드에서 가장
> 크므로, **격리 비교에서 마스크 차이가 눈에 안 보이면 JPEG로 되돌린다.**

> ⚠️ **512 하한 항목의 근거도 절반이다.** ML Kit Android 가이드 "Tips to improve performance" 절이
> "For ML Kit to get an accurate segmentation result, the image should be at least 512x512 pixels."
> 라고 적는다. 이것은 **하한 미만이면 정확하지 않다**는 말이지 **작은 이미지를 확대하면 회복된다**는
> 말이 아니다. 확대는 정보를 늘리지 않는다. 사진 세트에서 차이가 안 보이면 구현하지 않는다.

문서가 말하지 **않는** 것도 적어 둔다. 내부 다운스케일 여부, 모델의 실제 입력 해상도, 상한 치수는
공개 문서에 없다. 이 스펙은 그 셋을 전제로 삼지 않는다.

## 범위

**포함**

- `ImageCapture` 촬영 품질을 올려 첫 세대 손실을 줄인다.
- 디코드 지점에 세그멘테이션 입력 정규화 자리를 만든다. 그 자리에 무엇이 들어가는지는 위 근거
  등급 표를 따른다.
- 조건부로, `CustomCameraRoute`의 **토핑 만들기 경로**만 저장 포맷을 무손실로 바꾼다.
- 판정을 순수 함수로 빼고 JVM 유닛으로 덮는다.
- 고정 사진 세트와 전후 비교 절차를 정한다.

**제외**

- **배경 촬영 경로**(`NavKeyCameraCustom(returnResultOnly = true)`) — 커스텀 카메라의 소비자가 둘이다.
  토핑 만들기와 C-301 배경 촬영이다. 뒤엣것은 누끼를 안 타고 곧장 서버로 올라가므로 무손실 전환의
  이득이 0이고 업로드 용량만 커진다. **포맷 선택은 진입 인자로 가른다.**
- **원본 다운샘플**(OQ-P-228 잔존) — 목표가 자원이 아니다. 마스크가 입력 크기로 나오므로
  축소한 마스크를 되올리는 보간 손실이 새로 생긴다. 정확도를 목표로 두는 한 방향이 반대다.
- **뷰파인더 크롭이 정확도에 주는 영향** — 카메라 경로에서 모델이 보는 것은 센서 프레임이 아니라
  뷰파인더 사각형으로 잘린 조각이다. 피사체가 변에 걸치면 후보로 안 잡히거나 면적 필터에 걸려
  사라질 수 있다. 이 라운드는 `saveViewfinderCapture`를 열지만 크롭 정책은 손대지 않는다.
  사진 세트에 걸친 사진을 한 장 넣어 **기준선만 기록**한다.
- **색공간·`Bitmap.Config` 정규화** — `ImageDecoder`가 소스에 따라 `RGBA_F16`이나 Display P3를 낼 수
  있고 `InputImage.fromBitmap`이 요구하는 것은 ARGB_8888이다. 형식 정합 문제이므로 대비 정규화와는
  근거의 성격이 다르나, 검증 수단이 달라 별도 라운드로 민다(OQ-P-281).
- **대비·감마 정규화** — 문서 근거가 없다. 사진 세트에 저대비 사진을 넣어 기준선만 기록한다.
- **후처리 전반** — 최대 연결 요소만 남기기, 모폴로지 open·close, 알파 램프, 경계 색 오염 제거,
  후보 면적을 bounding box 대신 불투명 픽셀 수로 재기. 별도 라운드다.
- **EXIF 미러링**(`ORIENTATION_FLIP_*`) — 좌우 반전은 정확도와 무관하고, 잘못 적용하면 뒤집힌
  누끼가 나온다. 회전 3종만 처리한다.
- **갤러리 경로의 원본 손실** — 사용자가 고른 파일이라 우리가 고칠 수 있는 것이 아니다.
- **`SystemCameraRoute` 저장 포맷** — OEM 카메라 앱이 쓴다. 우리 코드가 아니다.

## 설계

### 1. 손실이 처음 생기는 자리를 먼저 고친다

`ImageCapture.Builder().build()`가 아무 설정도 하지 않아 기본값으로 동작한다. 출력은 JPEG이고
capture mode는 지연 최소화 쪽이다. (🔁 **이 서술은 #349 이전의 코드다** — 지금 빌더는
`CAPTURE_MODE_MAXIMIZE_QUALITY`와 `setJpegQuality(100)`을 명시한다. 출력이 JPEG인 것은 그대로다.) **`saveViewfinderCapture`가 다루는 비트맵은 이미 그 압축을
한 번 거친 픽셀이다.**

그래서 순서를 뒤집는다. **`ImageCapture` 쪽 품질을 먼저 올린다**(`setJpegQuality`를 상향하거나
`CAPTURE_MODE_MAXIMIZE_QUALITY`를 쓴다. 어느 쪽이 값을 하는지는 사진 세트가 가른다). 비용은
셔터 지연이 조금 느는 것뿐이고 파일 크기·업로드·캐시에는 영향이 없다.

**저장 포맷 PNG 전환은 그 위에서 추가 이득이 보일 때만 넣는다.** 넣기로 하면 이렇게 한다.

확장자가 갈리는 자리는 이미 준비돼 있다. 커스텀 카메라는 `CreateCameraCacheFileUseCase`를 직접
호출해 파일을 만들고, 시스템 카메라는 인자 없는 `CreateCameraCacheUriUseCase`를 호출한다.
**파일 use case에 포맷 인자를 주고 기본값을 JPEG로 두면** OEM 카메라 앱이 쓰는 경로는 그대로 있다.
포맷은 `CustomCameraViewModel`이 고정하지 않고 **`returnResultOnly`로 가른다** — 토핑 만들기만
PNG를 받고 배경 촬영은 JPEG를 유지한다.

WebP 무손실은 쓰지 않는다. `WEBP_LOSSLESS`의 API 하한을 문서로 확인하지 못했다. 확인하지 못한 것을
설계에 넣지 않는다.

**PNG를 넣으면 함께 넣어야 하는 것이 둘이다.** 이 둘은 PNG 없이는 필요 없고 PNG와 함께면 필수다.

- **촬영 중 상태와 셔터 비활성.** 화면 이동은 저장이 끝난 뒤에 일어난다. PNG 인코딩이 끼면 셔터에서
  확인 화면까지의 공백이 눈에 띄게 길어지는데 지금은 진행 표시도 연타 가드도 없다. 파일명이 초
  단위라 같은 초의 두 촬영은 서로를 덮는다.
- **`RecentImageRepositoryImpl#fileExtension`의 확장자 판정**(🔁 **#349에서 `extensionOf`로 바뀌며
  먼저 들어갔다** — PNG 채택을 기다리지 않았다). 당시에는 `SOURCE`를 `"jpg"`로 못박고
  있고 바로 위 KDoc이 "이름이 거짓이면 업로드가 content type을 잘못 정한다"고 경고한다.
  PNG 촬영본이 그 이름으로 앉으면, C-301 배경 편집이 최근 목록에서 그것을 골랐을 때
  `ImageFileLocalDataSourceImpl`이 확장자 기반 MIME을 먼저 믿어 **PNG 바이트가 `image/jpeg`로**
  올라간다. 확장자를 이름이 아니라 **바이트로** 정하도록 바꾼다.

### 2. 디코드 지점에서 입력을 정규화한다

정규화를 `segmentImage`가 아니라 `decodeImage`에 넣는다.

> 📌 **초판의 근거는 틀렸다.** 초판은 "편집 화면이 원본과 잘라낸 결과의 픽셀 치수 일치에 기대므로
> `segmentImage`에서만 고치면 마스크가 어긋난다"고 적었다. 코드가 그렇지 않다.
> `ToppingEditMask.kt#buildCutoutBitmap`이 세그멘테이션 판을 원본 사각형에 **늘려 그리고**,
> 그 KDoc이 "크기가 달라도 늘려서 맞춘다"를 계약으로 적어 두었다. 정렬을 좌우하는 것은 치수가
> 아니라 종횡비이고, 짧은 변 기준 확대는 종횡비를 보존한다.

**남는 진짜 근거는 일관성이다.** `SegmentationViewModel`과 `ToppingEditViewModel`이 같은
`DecodeImageUseCase`를 타므로, 디코드 지점에 두면 "정규화된 비트맵이 이 흐름의 원본"이라는 정의가
한 자리에서 유지된다. `segmentImage`에만 두면 같은 URI가 두 곳에서 다른 픽셀로 열리고, 지금은
`buildCutoutBitmap`의 늘려 그리기가 그것을 덮어 주지만 **그 방어에 기대는 설계가 된다.**
90도 회전처럼 종횡비가 뒤집히는 변환이 들어오면 그 방어로도 안 덮인다.

그 결과 `decodeImage`의 계약이 넓어진다. 다만 **"정규화"라는 말이 실제보다 넓게 읽히지 않도록**
KDoc에 무엇을 하고 무엇을 안 하는지 함께 적는다(색공간과 `Bitmap.Config`는 안 건드린다).

**해상도 하한과 상한.** 짧은 변이 512 미만이면 짧은 변이 512가 되도록 비율을 지켜 확대한다.

⚠️ **확대에는 반드시 총 픽셀 상한이 있어야 한다.** 짧은 변만 보고 키우면 긴 변이 자유롭다.
극단 종횡비 이미지(파노라마 조각 등)에서 확대 후 픽셀 수가 폭증하고, 그 뒤 `segmentImage`가
원본·subject 비트맵·(폴백이면) 전체 배열과 버퍼를 함께 들고 `persistSubject`가 같은 크기 투명
캔버스를 하나 더 만든다. 앱은 `largeHeap`을 선언하지 않고 OQ-P-228이 이미 12MP 사진에서 큰 피크를
쟀다. **확대 후 총 픽셀 수가 상한을 넘으면 확대하지 않는다**(확대 없음으로 정의한다). 그 편이
확대해서 죽는 것보다 낫다.

**확대는 편집 화면 밖으로도 전파된다.** 알맹이 PNG의 인트린식 픽셀 치수가 배치 화면의 초기 토핑
크기가 되고 그 값이 서버 `scale`로 굳는다. 짧은 변 512 미만 사진에서 만든 토핑이 이번 변경 뒤에
더 크게 캔버스에 박히는 무증상 회귀가 가능하다. 사진 세트가 배치까지 끝내서 확인한다(OQ-P-282).

### 3. 회전 정합

`InputImage.fromBitmap(bitmap, 0)`이 회전 0을 단정한다. 이 단정이 참인지는 디코드 경로에 달렸다.

`decodeUriToBitmap`은 API 28 이상에서 `ImageDecoder`를, 미만에서 `MediaStore.Images.Media.getBitmap`을
쓴다. 후자는 EXIF orientation을 적용하지 않는다. `minSdk`가 26이므로 **API 26·27에서 누운 사진이
그대로 모델에 들어가는 갈래가 살아 있다.** 이 갈래의 보정은 코드만 읽어도 확정되므로 무조건 넣는다.

⚠️ **API 28 이상은 기본적으로 손대지 않는다.** `ImageDecoder`가 EXIF를 자동 적용한다는 문장을 공식
문서에서 찾지 못했다(OQ-P-280). 이미 적용된 이미지에 또 회전을 걸면 두 번 돌아가므로, **판정하지
못한 상태의 기본값은 "보정하지 않음"이다.** 사진 세트에서 API 28 이상도 누워 나오는 것이 확인되면
그때 전 버전 보정으로 넓힌다. 판정 못 한 것을 근거로 위험한 쪽으로 기울지 않는다.

보정 자체는 `decodeUriToBitmap` 안에 둔다. 이 확장 함수의 호출부가 `ImageSegmentationRepositoryImpl`
하나뿐이라 계약을 넓혀도 파급이 없고, API 버전 분기가 이미 그 안에 있어 갈라진 동작을 한자리에서 메운다.
`androidx.exifinterface` 의존성을 새로 넣어야 한다.

⚠️ **이 전제가 develop에서 깨졌다**(2026-08-27, PR #369) — `decodeImage`가 스킴으로 갈라져,
`https://`면 `RemoteImageDownloadDataSource`가 받은 바이트를 `BitmapFactory.decodeByteArray`로
디코드하고 **`decodeUriToBitmap`을 아예 타지 않는다.** 그래서 보정을 그 확장 함수 안에만 두면
**서버 토핑을 다시 편집하는 경로에는 방향 보정도 하한 확대도 적용되지 않는다.** 구현할 때 정규화의
자리를 `decodeImage` 본문(두 갈래가 합류한 뒤)으로 올릴지, 갈래마다 따로 부를지를 먼저 정한다
→ [open-questions](../../synthesis/open-questions.md) OQ-P-326 ④.

**EXIF를 어디서 읽는지를 계약에 적는다.** `ImageDecoder.createSource`가 스트림을 소비하므로 같은
URI를 한 번 더 열어야 한다. 두 번째 열기는 실패할 수 있다(일회성 provider). **못 읽으면 회전 0으로
진행한다** — 태그가 깨진 것과 이미지를 못 연 것은 다른 사건이다. 다만 재개방 실패가 상시 참이 되면
보정이 조용히 무효가 되고 사진 세트로도 안 드러나므로, **재개방 실패는 로그로 남긴다.**

⚠️ **회전 복사본은 전체 해상도에 걸린다.** 확대와 달리 작은 이미지 한정이 아니다. 12MP 사진이면
회전 전후 두 판이 순간 함께 산다. 회전 전 판을 반드시 회수한다(아래 에러 처리).

## API / 인터페이스

```kotlin
// domain — 포맷은 도메인 어휘로 두고 확장자 매핑은 data 가 한다 (PNG 를 넣기로 한 경우에만)
enum class CameraCacheFormat { JPEG, PNG }

class CreateCameraCacheFileUseCase {
    // 기본값이 JPEG 라 CreateCameraCacheUriUseCase(시스템 카메라)는 손대지 않아도 된다
    operator fun invoke(format: CameraCacheFormat = CameraCacheFormat.JPEG): File
}

// domain — 계약이 넓어지는 자리. 넓어진 만큼 경계도 함께 적는다
interface ImageSegmentationRepository {
    /**
     * 세그멘테이션 입력 규격으로 정규화해 디코드한다 — 방향을 세우고 짧은 변 하한을 맞춘다.
     * 색공간과 [android.graphics.Bitmap.Config] 는 건드리지 않는다.
     */
    suspend fun decodeImage(uri: String): BitmapWrapper
}

// data — 판단만 떼어 기기 없이 검증한다(SegmentationMask.kt 와 같은 이유)
internal data class ScaledSize(val width: Int, val height: Int)

/**
 * 확대가 필요 없거나 확대하면 총 픽셀이 상한을 넘으면 null.
 * 상한을 두는 이유는 짧은 변만 보고 키우면 극단 종횡비에서 긴 변이 폭증해서다.
 */
internal fun computeUpscaleTarget(width: Int, height: Int): ScaledSize?

// core:util:android — ExifInterface 상수를 각도로. 미러링 4종은 0 이다(처리하지 않는다)
internal fun exifOrientationToDegrees(orientation: Int): Int
```

## 파일 구성

| 파일 | 역할 | 조건 |
|---|---|---|
| `CameraPreviewComponent.kt` | `ImageCapture` 품질 설정 | 무조건 → ✅ #349 |
| `core/util/android/.../ContentResolver.kt` | EXIF 각도 보정. 회전 전 판 회수 | 무조건(API 26·27) → ✅ #349 |
| `gradle/libs.versions.toml` | `androidx.exifinterface` 추가 | 무조건 → ✅ #349 (`core/util/android/build.gradle.kts` 결선과 모듈 로거 `Logger.kt` 신설이 함께 들어갔다) |
| `data/.../image/SegmentationInputNormalizer.kt` | 신설. `computeUpscaleTarget`·하한·픽셀 상한 | 512 항목 채택 시 |
| `ImageSegmentationRepositoryImpl#decodeImage` | 확대 적용. 중간 비트맵 회수. KDoc 경계 명시 | 512 항목 채택 시 |
| `domain/.../camera/CameraCacheFormat.kt` | 신설. 포맷 어휘 | PNG 채택 시 |
| `CreateCameraCacheFileUseCase` | 포맷 인자 추가(기본 JPEG) | PNG 채택 시 |
| `FileCameraCacheLocalDataSource(+Impl)` | `createFile`이 포맷을 받아 확장자를 고른다 | PNG 채택 시 |
| `CameraCrop.kt#saveViewfinderCapture` | 포맷을 받아 압축. `JPEG_QUALITY` 상수 정리 | PNG 채택 시 |
| `CustomCameraViewModel` | `returnResultOnly`로 포맷을 가른다. 촬영 중 상태·셔터 비활성 | PNG 채택 시 |
| `RecentImageRepositoryImpl#extensionOf` | `SOURCE` 확장자를 이름이 아니라 바이트로 판정 | ~~PNG 채택 시~~ → ✅ #349 (계획이 1단계로 앞당겼다) |

## 에러 처리

새 실패 표현을 만들지 않는다. 기존 경로로 접힌다.

- **PNG 저장 실패** — `CustomCameraRoute`가 이미 `runCatching`으로 감싸 촬영 실패 토스트로 받는다.
  다만 쓰다 끊기면 잘린 파일이 캐시에 남으므로 **실패 시 방금 만든 파일을 지운다.**
  ⚠️ **카메라 캐시에는 정리 경로가 없다**(`FileCameraCacheLocalDataSourceImpl`). 파일이 커지는
  변경이 그 미결의 부담을 키운다(OQ-P-279).
- **확대·회전 중 메모리 부족** — `DecodeImageUseCase`가 `runSuspendCatching`으로 한 번만 감싸고
  취소는 재던지므로 `SegmentationViewModel`의 실패 상태로 간다.
  **확대는 픽셀 상한이 막고, 회전은 전체 해상도에 걸리므로 중간 판 회수가 필수다.**
- **EXIF 읽기·URI 재개방 실패** — 회전 0으로 진행하고 로그를 남긴다.

회전과 확대가 겹치면 중간 비트맵이 생긴다. `toForegroundCandidate`(2026-09-10 PR #487에서 `SegmentationCandidateHarvest.kt#harvestForeground`로 옮겨 갔다)가 `trimmed !== masked`로 하는 것과
같은 관용구로, 입력과 다른 인스턴스일 때만 중간 판을 회수한다.

## 테스트

판단을 순수 함수로 빼서 JVM에서 검증한다. `SegmentationMask.kt`·`SegmentationCandidateFilter.kt`가
선 패턴이다. (`CameraCrop.kt#computeCropRect`도 순수 함수로 빠져 있으나 `feature/camera/impl`에
테스트 소스셋이 없어 **덮여 있지는 않다.** 순수 함수 분리의 선례일 뿐 JVM 검증의 선례는 아니다.)

- **`computeUpscaleTarget`** — 경계값(511·512·513), 비율 보존, 이미 충분하면 확대 없음, 0 이하 방어,
  그리고 **극단 종횡비에서 픽셀 상한에 걸려 확대 없음이 되는 것**. 마지막 항목은 기대값을 명시한다.
  기대값 없이 케이스만 두면 구현이 무엇을 하든 그것이 정답으로 굳는다.
- **`exifOrientationToDegrees`** — 회전 3종의 각도, `ORIENTATION_NORMAL`·`UNDEFINED`는 0,
  **미러링 4종도 0**(처리하지 않기로 한 결정을 테스트가 지킨다).

`Bitmap` 생성·`ImageDecoder`·`ExifInterface` 파일 읽기·촬영 품질·저장 포맷은 JVM 유닛으로 덮지 않는다.
아래 사진 세트가 담당한다.

## 검증 — 고정 사진 세트

사진마다 판정할 것을 하나씩 물린다.

| 사진 | 판정 대상 |
|---|---|
| EXIF 회전 태그가 **실제로 붙어 있음을 먼저 확인한** 세로 사진 | 회전 정합. API 27 에뮬레이터와 API 28 이상에서 각각 본다 |
| 짧은 변 512 미만 이미지 | 확대 가드의 효과. **배치까지 끝내서** 토핑 크기 회귀도 함께 본다 |
| 머리카락·털·잎사귀처럼 경계가 복잡한 피사체 | 촬영 품질과 저장 포맷. 아티팩트가 가장 크게 드러나는 조건 |
| 피사체가 뷰파인더 변에 걸친 촬영 | 이번엔 안 고친다. 크롭이 정확도에 주는 영향의 기준선 |
| 저대비·역광 | 이번엔 안 고친다. 다음 라운드 판단용 기준선 |
| 다중 피사체 | 기존 다중 후보 동작 회귀 확인 |

**회전 사진은 태그를 먼저 확인한다.** 많은 카메라 앱이 픽셀에 회전을 굽고 태그는 정상으로 쓴다.
그런 사진으로 시험하면 두 API 대역 모두 문제없다고 나오고 아무것도 배우지 못한다.
API 26·27 실기기는 구하기 어려우므로 **API 27 에뮬레이터를 판정 수단으로 쓴다.**

**손실 항목은 세 벌로 가른다.** 촬영 품질을 올린 것과 저장 포맷을 바꾼 것이 각각 얼마를 기여하는지
분리해야 PNG를 넣을지 정할 수 있다.

1. 현행 촬영 품질 + JPEG 저장 (기준선)
2. 상향 촬영 품질 + JPEG 저장
3. 상향 촬영 품질 + PNG 저장

2번과 3번의 차이가 눈에 안 보이면 PNG를 넣지 않는다. 함께 잰다 — PNG 저장 시간, 파일 크기,
셔터에서 확인 화면까지의 공백.

절차는 변경 전후 빌드에 같은 사진을 같은 경로로 통과시키고 C-103 후보 화면과 최종 누끼 PNG를
나란히 두는 것이다. 갤러리 경로만 쓰는 항목은 카메라 없이 격리 검증된다.

사진 자체는 개인 사진일 수 있으므로 **이 public repo에 커밋하지 않는다.** 조건 목록만 문서에 남기고
파일은 기기에 둔다.

## 주의 / 열린 질문

- **OQ-P-278** — 확대 가드의 효과가 미검증이다. 문서는 하한만 말하고 확대로 회복된다고는 말하지 않는다.
- **OQ-P-279** — PNG 전환이 온디스크 누적을 키운다. 카메라 캐시는 정리 경로가 없고, 최근 이미지는
  `filesDir`이라 OS 회수 대상도 아니다.
- **OQ-P-280** — `ImageDecoder`의 EXIF 자동 적용 여부를 문서로 확인하지 못했다. 이 스펙은 확인 전까지
  API 28 이상을 보정하지 않는 쪽을 기본값으로 둔다.
- **OQ-P-281** — 색공간·`Bitmap.Config`·HEIC를 `decodeImage`가 다루지 않는다.
- **OQ-P-282** — 배치 화면의 초기 토핑 크기가 알맹이의 절대 픽셀에서 나와 서버 `scale`로 굳는다.
- **OQ-P-228 잔존** — 원본 다운샘플은 여전히 없다.
- 후처리 항목들(최대 연결 요소·모폴로지·알파 램프·경계 색 오염)은 기전이 분명하고 눈에 보이는
  개선이 예상되나 이번 범위 밖이다. 이 스펙의 사진 세트가 그대로 다음 라운드의 기준선이 된다.

## 검수 이력

**2026-08-23, 서브에이전트 검수 2회(사실 대조·설계 공격).** 초판을 두 축에서 뒤집었다.

1. **`ImageCapture` 기본값이 JPEG라 손실이 이미 한 세대 있었다.** 초판은 무손실 저장을 유일한
   무조건 항목으로 두었는데, 근거가 가장 약한 항목이 가장 비싼 항목이었다. 순서를 뒤집고 철회
   조건을 붙였다.
2. **좌표계 근거가 거짓이었다.** `buildCutoutBitmap`이 늘려 그린다. 결론은 유지하되 근거를
   일관성으로 다시 세웠다.

그 밖에 확대 픽셀 상한 부재, `RecentImageRepositoryImpl`의 확장자 하드코딩, 배경 촬영 경로 오염,
회전 복사본의 메모리, 셔터 연타 가드 부재, 색공간·`Bitmap.Config` 누락, 배치 `scale` 전파를 반영했다.
