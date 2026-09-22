---
id: upload-image-downscale
title: 업로드 이미지 다운스케일·배경 JPEG 고정 (Upload image downscale)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-09
related_code:
  - ImageUploadRepositoryImpl#upload
  - UploadImageFormat
  - ImageFileLocalDataSourceImpl#copyToCache
  - ImageSegmentationRepositoryImpl#saveToCacheAsPng
  - UploadImageUseCase
  - AddToppingUseCase
  - CanvasBGEditViewModel#uploadBackgroundImage
  - CanvasToppingPlaceViewModel#maxScaleToOverflowCanvas
  - ToppingEditViewModel
  - ToppingOutlineCache
  - PresignedUploadDataSourceImpl#put
  - UploadImagePreprocessor
  - UploadImagePreprocessorImpl
  - UploadImagePlan#of
  - PreparedUploadImage
  - UploadImageSize
  - UtilsModule#bindUploadImagePreprocessor
  - File#readExifDegrees
  - ExifOrientation#exifOrientationToDegrees
related_adr: ADR-0017
related_spec: segmentation-preprocessing
related_architecture:
  - data-layer.md
supersedes:
superseded_by:
tags: [spec, parfait, image, upload]
---

# Spec: 업로드 이미지 다운스케일·배경 JPEG 고정

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

## 목표

서버로 나가는 이미지의 픽셀 수와 바이트를 줄인다. 지금은 업로드 경로 어디에도 축소가 없어서
카메라 원본 해상도가 그대로 S3에 올라가고, 소비 측은 그 해상도를 전혀 쓰지 않는다.

## 배경 — 관찰된 사실

서버에 올라간 파일을 실측한 결과가 출발점이다(팀 공유, 2026-09-08). 파일 6건의 픽셀 치수와
알파 bbox를 비교했더니 4건이 **완전히 일치**했고 2건은 2~5px 차이였다.

| 원본 크기 | 알파 bbox |
|---|---|
| 2600x3832 | 2598x3832 |
| 1662x1469 | 1662x1464 |
| 2297x2533 | 2297x2533 |
| 1098x1013 | 1098x1013 |
| 3000x1202 | 3000x1202 |
| 1000x1938 | 1000x1938 |

여기서 두 가지가 따라 나온다.

**첫째, 더 잘라서 줄일 여지가 없다.** `ImageSegmentationRepositoryImpl#postProcess`가 후처리
알파의 bbox로 비트맵을 잘라 저장하고 직후 `require`로 치수 일치를 강제한다. 업로드 파일이
알파 bbox와 같다는 것은 코드가 보장하는 등식이지 우연이 아니다.

**둘째, 문제는 절대 해상도다.** 2600x3832 ARGB_8888은 힙에서 약 40MB이고, PNG는 무손실이라
사진 콘텐츠에서 거의 줄지 않는다(`saveToCacheAsPng`가 넘기는 quality 100은 PNG에서 무시되는
인자다). 반면 소비 측이 요구하는 해상도의 상계는 캔버스 긴 변이다 —
`CanvasToppingPlaceViewModel#maxScaleToOverflowCanvas`가 토핑 배율 상한을 캔버스 긴 변에서
유도하고, 배경은 캔버스를 채우며, 테두리용 거리장은 `ToppingOutlineCache`가 256px로 줄여
계산한다.

> ℹ️ 완전 일치 4건이 `originalCandidate` 경로(누끼 없이 원본 판을 그대로 후보로 내주는 자리)를
> 탄 것인지 여부는 이 스펙이 판정하지 않는다. 별도 미결로 남긴다(아래 주의 절).

## 범위

**포함**

- 업로드 직전 긴 변 상한 축소. 두 업로드 경로(`ImageType.NUKKI`·`ImageType.BACKGROUND`) 모두.
- 배경 업로드의 출력 포맷을 JPEG로 고정.
- 판정 로직을 순수 함수로 빼고 JVM 유닛으로 덮는다.
- 축소 전후 치수·바이트 로깅.

**제외**

- **WebP 전환** — 서버가 `image/png`·`image/jpeg` 2종만 받는다(`ImageKeyGenerator`가 그 외를
  `INVALID_CONTENT_TYPE`으로 던진다). 서버·iOS와 함께 움직여야 하므로 후속 라운드로 분리한다.
- **누끼 출력 포맷 변경** — 알파가 필요해 PNG를 유지한다.
- **`cacheDir/upload` 누적 정리** — 이 스펙이 만드는 임시 파일은 지우지만, `copyToCache`가 남기는
  기존 복사본은 건드리지 않는다.
- **세그멘테이션 입력 해상도** — [segmentation-preprocessing](../2026-08-23-segmentation-preprocessing.md)이
  다루는 영역이고 목표 방향이 반대다(그쪽은 모델 입력의 정확도, 이쪽은 서버로 나가는 결과물).
- 캔버스 캡처(`writeToCanvasCaptureCache`) — 갤러리 저장·미리보기 전용이라 업로드와 무관하다.

## 설계

### 배치 — 업로드 경계 한 자리

`data` 레이어에 전처리 단위를 두고 `ImageUploadRepositoryImpl#upload`가 발급 직전에 부른다.

```kotlin
interface UploadImagePreprocessor {
    suspend fun prepare(file: File, imageType: ImageType): Result<PreparedUploadImage>
}

data class PreparedUploadImage(
    val file: File,
    val format: UploadImageFormat,
    val isTemporary: Boolean,
)
```

**소스가 아니라 업로드 경계에 두는 이유**는 로컬 사본의 용도가 업로드 하나가 아니기 때문이다.
누끼 파일(`SegmentationResult`의 잘린 판)은 `ToppingEditViewModel`의 수동 편집 입력이기도 하다.
그 화면은 확대해서 브러시로 다듬는 UX라 원본 해상도가 실제로 쓰인다. 소스에서 줄이면 편집
품질이 함께 내려간다. 경계에서 줄이면 **로컬은 원본을 유지하고 서버로 나가는 것만 줄어든다.**

배치의 부수 효과로 배경의 두 입력원(갤러리 선택, `returnResultOnly` 커스텀 카메라)이 같은
자리를 지나므로 규칙이 한 번만 적힌다.

`upload`는 현재 `UploadImageFormat.ofExtension`으로 contentType을 정한 뒤 발급과 PUT에 같은 값을
넘긴다. 전처리기가 파일과 포맷을 **쌍으로** 돌려주므로 그 성질이 유지된다. 둘이 갈라지면 S3가
서명 불일치로 거절하고 그 실패는 서버 로그에 남지 않는다.

### 결정 표

> 🔁 **누끼 행은 [topping-upload-source-scaled](2026-09-09-topping-upload-source-scaled.md)로 대체됐다.**
> 상한을 잘린 판에 거는 방식으로는 피사체가 프레임 일부만 차지할 때 원본이 아무리 커도 상한에 닿지
> 않는다. 새 스펙은 기준을 원본 사진으로 옮긴다. **배경 행은 이 문서가 그대로 정본이다.**
>
> 🔁 **아래의 iOS 값 인용 둘은 2026-09-09에 사실이 아니게 됐다.** iOS가 같은 날 누끼 상한을 1200으로,
> 배경 JPEG 품질을 0.7로 내렸다. 품질은 Android도 70으로 맞췄고(코드 반영 완료) 아래 본문을 정정했다.
> 누끼 상한은 쫓지 않는다 — 근거는 ADR-0032.

긴 변 상한은 **imageType마다 다르고, 값의 근거는 iOS다.** `TEAMYG-iOS`가 같은 서버에 같은
기능으로 올리고 있고 이미 상한을 두고 있다 — `ToppingImageEncoder.maximumLongEdge`가 1500,
`BackgroundImageLoader.maximumLongEdge`가 2048이며 배경은 `jpegCompressionQuality` 0.9로 굽는다.
플랫폼마다 상한이 다르면 같은 캔버스를 두 기기에서 볼 때 화질이 갈리므로 그 값을 그대로 쓴다.

| imageType | 긴 변 상한 |
|---|---|
| NUKKI | 1500 |
| BACKGROUND | 2048 |

| imageType | 입력 포맷 | 긴 변 | 동작 |
|---|---|---|---|
| NUKKI | PNG | > 1500 | 축소 후 PNG 재인코딩(알파 유지) |
| NUKKI | PNG | ≤ 1500 | 그대로 통과 |
| NUKKI | JPEG | > 1500 | 축소 후 JPEG 재인코딩 |
| NUKKI | JPEG | ≤ 1500 | 그대로 통과 |
| BACKGROUND | JPEG | > 2048 | 축소 후 JPEG |
| BACKGROUND | JPEG | ≤ 2048 | **그대로 통과** |
| BACKGROUND | PNG | 무관 | JPEG 재인코딩, 투명 영역은 흰색 합성 |

누끼의 JPEG 두 행은 방어적이다. `saveToCacheAsPng`가 항상 PNG로 굽기 때문에 지금은 닿지 않는
갈래이고, 전처리기가 imageType이 아니라 **입력 포맷**을 보고 판단한다는 것을 못박기 위해 적는다.
누끼의 출력 포맷은 입력을 따라가고 바꾸지 않는다.

**확대는 어떤 경우에도 하지 않는다.** 이 성질이 누적을 막는다. 토핑 재편집은 업로드본을 다시
내려받아(`ImageSegmentationRepositoryImpl#decodeImage`의 원격 갈래) 재분할하므로 같은 이미지가
전처리를 여러 번 지날 수 있는데, 두 번째 통과는 무동작이다.

**이미 JPEG이고 상한 이하인 배경을 다시 굽지 않는 이유**는 재인코딩이 손실만 더하고 이득이
없기 때문이다. 반대로 PNG 배경은 크기와 무관하게 굽는다 — 스크린샷을 배경으로 고르는 경우가
용량 기여가 가장 크고, 배경은 캔버스를 덮는 불투명 이미지라 알파를 버려도 잃는 것이 없다.

JPEG quality는 **70**으로 둔다. iOS의 `jpegCompressionQuality` 0.7과 같은 값이다.

### EXIF 회전

재인코딩 경로는 **원본의 EXIF orientation을 픽셀에 굽고, 출력에는 EXIF를 쓰지 않는다.**

이 조항이 없으면 조용한 회귀가 난다. 업로드는 지금까지 `copyToCache`가 원본 바이트를 복사해
태그가 살아 있었고 뷰어(Coil·iOS)가 그 태그로 사진을 세웠다. 그런데 `BitmapFactory.decodeFile`은
EXIF를 결과에 남기지도, 픽셀에 적용하지도 않는다. 그대로 두면 상한을 넘는 갤러리 JPEG —
세로로 찍은 폰 사진 대부분 — 이 눕고, **통과 갈래는 태그를 보존하므로 같은 사진이 크기에 따라
회전이 갈린다.**

픽셀에 굽는 방식은 이 저장소의 기존 선례를 따른다(`core/util/android`의 `rotatedToUpright`).
각도 판독도 그 모듈의 `exifOrientationToDegrees`를 재사용하므로 **미러링(`FLIP_*`·`TRANSPOSE`·
`TRANSVERSE`)을 0도로 두는 규약**이 그대로 적용된다.

📌 **as-built** — 재사용의 실제 모양은 `core:util:android` 에 새로 선 확장
`File#readExifDegrees` 다. 그 함수가 `ExifInterface` 판독을 감싸고 `exifOrientationToDegrees` 로
각도를 내며, 태그를 못 읽으면 경고 로그를 남기고 0을 돌려준다 — 태그가 깨진 것과 파일을 못 여는
것은 다른 사건이라 호출부의 디코드까지 막지 않는다. 회전을 픽셀에 굽는 일 자체는
`rotatedToUpright` 를 부르지 않고 전처리기 안의 사설 함수가 한다. 두 자리가 같은 규약을 쓰되
코드를 공유하지는 않는다.

**회전은 축소 뒤에 적용한다.** 순서가 반대면 원본 해상도 판 둘이 동시에 살아난다 —
`Bitmap.createBitmap(bitmap, ..., matrix, true)`가 회전본을 다 할당한 뒤에야 원본을 놓기 때문이다.
축소본을 돌리면 90·270도의 뒤집힌 치수가 그냥 나오므로 치수를 따로 맞바꿀 필요도 없다.
90도 배수 회전은 픽셀 치환이라 화질 손실이 없다.

⚠️ **ICC 프로파일은 재인코딩에서 소실된다.** Display P3 사진 배경이 sRGB로 앉으면서 채도가 변할
수 있다. 이 라운드는 그 영향을 측정하지 않았다(아래 주의 절).

### 메모리

⚠️ **비교 기준을 바로잡는다 — 변경 전 `upload`는 디코드를 아예 하지 않았다.** 원본 바이트를 그대로
PUT했다. 그러므로 이 스펙이 넣는 디코드는 "덜 쓰는 경로"가 아니라 **없던 메모리 부담을 새로
만드는 것**이다. 그 부담을 감수하는 것이 아니라 없애야 한다.

`inJustDecodeBounds`로 치수를 먼저 읽고, `inSampleSize`(2의 거듭제곱)에 더해
`inDensity`/`inTargetDensity`/`inScaled`로 **디코드 단계에서 목표 치수까지 내려받는다.** 전 해상도
판이 아예 생기지 않는다. `createScaledBitmap`은 반올림 오차를 정확히 맞추는 역할로만 남는다.

`inSampleSize`만으로는 부족하다는 것이 이 설계의 근거다. 2의 거듭제곱은 목표보다 작아지면 안
되므로, 4032x3024 배경(상한 2048)은 `4032/2 = 2016 < 2048`이라 **sampleSize가 1에 걸려 48.7MB를
통째로 올린다.** 노출 구간은 `BACKGROUND` 긴 변 2049~4095이고, 하필 카메라 원본이 그 안에 있다.
`NUKKI`는 상한이 1500이라 항상 sampleSize 2 이상이 걸려 이 문제가 없다.

밀도 스케일링은 반올림이 끼므로 **결과가 목표보다 작아지지 않는 것을 보장해야 한다.** 작아지면
뒤에서 확대하게 되고, 그것은 이 스펙의 핵심 불변식 위반이다. 중간 비트맵은 `recycle`한다.

### 임시 파일

전처리가 새 파일을 만들었으면 업로드의 성공·실패와 무관하게 지운다(`PreparedUploadImage`의
`isTemporary`가 그 책임을 나른다). 원본을 그대로 통과시킨 경우에는 아무것도 지우지 않는다 —
그 파일의 수명은 부른 쪽이 쥐고 있다.

**축소본은 언제나 `cacheDir/upload`에 쓴다.** 입력 파일의 디렉터리에 나란히 두면 안 된다 —
최근 사용 알맹이 재사용 경로의 누끼 파일은 `filesDir/recent_images`에 있어서 고아가 **캐시가
아니라 영구 내부저장소**에 남고, 세그멘테이션 캐시에 두면 `clearSegmentationCache()`가 전송 도중
그 파일을 지울 수 있다.

### 실패 처리

전처리가 실패하면 **업로드 전체를 실패시킨다.** 원본으로 폴백하지 않는다. 배경 JPEG 고정은
정책이라 조용히 어기면 안 되고, 실패가 드러나지 않으면 고칠 수도 없다.

⚠️ 초판은 여기에 "축소 경로가 원본 디코드보다 메모리를 적게 쓰므로 폴백이 더 위험하다"고 적었다.
**그 비교는 틀렸다** — 변경 전 경로에는 디코드가 없었다. 「메모리」 절이 그 사실 위에 다시 섰다.
폴백하지 않는 결정 자체는 유지하되 근거는 위 두 줄이다.

⚠️ **as-built 이탈** — 머지된 `ImageUploadRepositoryImpl` 의 전처리 호출부 주석이 **철회된 그 비교를
그대로 들고 있다**("축소본이 원본보다 메모리를 덜 쓰므로 … 폴백하지 않는다"). 코드가 하는 일은
스펙과 같고 갈린 것은 근거뿐이라 동작에는 영향이 없다. 정정 대상으로
[`../../synthesis/open-questions.md`](../../../synthesis/open-questions.md)에 남긴다.

### 로깅

축소 전후의 치수와 바이트 수를 남긴다. 이 스펙의 효과는 "서버에 올라간 파일이 작아졌는가"로만
판정되는데, 그 값을 앱 쪽에서 확인할 수단이 지금 없다.

## 검증

- **JVM 유닛** — 목표 치수 산출, `inSampleSize` 계산, 결정 표의 재인코딩 필요 판정을 순수 함수로
  빼서 덮는다. 경계값(상한 정확히, 상한+1, 정사각형, 극단 종횡비)과 **imageType마다 상한이
  다르다는 것**(같은 1600px 이미지가 NUKKI에서는 줄고 BACKGROUND에서는 안 준다)을 포함한다.
- **JVM 유닛** — `ImageUploadRepositoryImpl`은 전처리기를 대역으로 두고, 전처리 결과의 파일과
  포맷이 발급 요청과 PUT에 **같은 값으로** 실리는지 검증한다. 기존 `ImageUploadRepositoryImplTest`의
  구성(mockk + `kotlin.test`)을 그대로 쓴다.
- **수동** — 실제 디코드·인코딩은 실기기에서 눈으로 확인한다. `data` 모듈에 계측 테스트 소스셋이
  없고 프로젝트에 Robolectric도 없어, 새 하니스를 들이지 않기로 확정했다.
  확인 항목: 큰 사진 누끼 업로드 · 상한 이하 누끼(무동작) · JPEG 배경 · PNG 스크린샷 배경 ·
  투명 PNG 배경(흰색 합성) · 업로드본 재편집(2회차 무동작) · 캔버스에서 토핑 최대 확대 시 화질 ·
  **EXIF 90/270 세로 사진 배경**(상한 초과본과 이하본을 둘 다 올려 두 결과의 방향이 같은지 대조 —
  갈리면 회전 방향이 반대다) · **저사양 기기에서 긴 변 2049~4095 배경**(없던 디코드가 생긴 구간) ·
  **광색역(Display P3) 사진 배경**(ICC 소실로 채도가 변하는지) · **서버가 안 받는 확장자**
  (`.gif`·`.webp`를 골랐을 때 서버를 부르기 전에 끊기는지 — 판정 자리가 전처리기로 옮겨져
  자동 테스트가 없다).

## 주의 / 열린 질문

- **완전 일치 4건의 정체가 미확인이다.** 알파가 전 픽셀 불투명하다는 뜻이고, 그렇다면
  `originalCandidate` 경로(누끼 없이 원본 판)를 탄 것이 된다. 사용자가 원본 후보를 고른 정상
  동선인지, 후처리가 알파를 전부 지워 되돌아간 것인지에 따라 대응이 달라진다. 판정 수단은
  `ImageSegmentationRepositoryImpl`이 이미 남기는 되돌림 로그다. **이 스펙과 독립이다** — 어느
  쪽이든 축소의 필요성은 바뀌지 않는다.
- **상한값의 근거는 iOS와의 정합이지 측정이 아니다.** iOS가 1500·2048을 어떻게 골랐는지는
  그쪽 코드에 적혀 있지 않다. 축소 후 실제 바이트가 얼마나 주는지는 수동 확인에서 처음
  나오고(검증 절), 기대에 못 미치면 값이 아니라 포맷(WebP)이 다음 레버다.
- **두 플랫폼이 같이 낮추는 것은 별건이다.** 소비 측 상계는 캔버스 긴 변이라 2048은 그보다
  크다. 값을 낮추려면 iOS와 함께 움직여야 하고, 이 스펙은 그 협의를 하지 않는다.
- **없던 디코드가 생겼다.** 변경 전 업로드는 바이트 복사뿐이었다. 「메모리」 절이 전 해상도 판을
  없앴지만, 그래도 이 경로는 이제 비트맵을 만든다. 저사양 기기에서 긴 변 2049~4095 배경이
  실제로 견디는지는 수동 확인이 처음 판정한다.
- **ICC 프로파일 소실을 측정하지 않았다.** 광색역 사진 배경이 눈에 띄게 변하면 별건으로 다룬다.
- **미러링 EXIF는 보정하지 않는다.** `TRANSPOSE`·`TRANSVERSE`는 90도 성분을 품는데
  `exifOrientationToDegrees`가 0으로 매핑하므로 그 사진만 재인코딩 갈래와 통과 갈래의 방향이
  갈린다. 미러링을 0도로 두는 것은 [segmentation-preprocessing](../2026-08-23-segmentation-preprocessing.md)이
  정한 저장소 규약이라 이 스펙이 뒤집지 않는다.
- **기존 업로드본은 그대로다.** 이미 올라간 큰 파일을 줄이는 마이그레이션은 없다.
