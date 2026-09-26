---
id: ADR-0011
title: 크로스모듈 비트맵 추상화 (BitmapWrapper / AndroidBitmap)
status: accepted
date: 2026-07-12
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec: c103-segmentation-topping-edit, c103-multi-subject-selection
related_architecture: module-structure, data-layer
platforms: android
tags: [adr, parfait]
---
# ADR-0011: 크로스모듈 비트맵 추상화 (BitmapWrapper / AndroidBitmap)

## 맥락
`domain`은 순수 Kotlin(kotlin-jvm) 모듈로 Android 의존을 금지한다([[0001-layered-multi-module]]·[[module-structure]]). 그런데 이미지 세그멘테이션 기능은 도메인 인터페이스(`ImageSegmentationRepository`)와 모델(`SegmentationResult`)이 **비트맵**을 다뤄야 한다. `android.graphics.Bitmap`은 Android 타입이라 domain에서 직접 참조할 수 없다.

## 결정
비트맵을 플랫폼 무관 추상 타입으로 감싸 도메인은 추상만 참조하고, 실제 `Bitmap`은 data 레이어에서만 다룬다.

- `core:util:jvm`에 순수 Kotlin `interface BitmapWrapper`(현재 멤버 없음 — TODO).
- `core:util:android`에 `@JvmInline value class AndroidBitmap(delegate: Bitmap) : BitmapWrapper` + 확장 `Bitmap.toAndroidBitmap()`. 실제 비트맵은 `getRawData()`로 노출.
- `domain`(`ImageSegmentationRepository`, `SegmentationResult.bitmap`)은 `BitmapWrapper`만 참조.
- `data` 구현(`ImageSegmentationRepositoryImpl`)이 `as? AndroidBitmap` 다운캐스트로 실제 `Bitmap` 복원. 실패 시 `SegmentationException.ImageNotFound`로 방어.
- 의존: `core:util:android` → `core:util:jvm`.

## 대안
- **domain을 android-library로 전환해 `Bitmap` 직접 사용** — 추상화 불필요, 코드 단순.
  **→ 기각:** domain 순수성([[0001-layered-multi-module]]) 파괴. 도메인 단위 테스트에 Robolectric/기기 필요해짐.
- **비트맵을 `ByteArray`·파일경로 등 원시 타입으로만 도메인에 전달** — 별도 타입 없이 통과.
  **→ 기각:** 타입 안전성·의미 상실, 매번 인코딩/디코딩 비용. (단, subject 결과 이미지는 `SegmentationResult.subjectImagePath` 파일경로로 별도 전달 — 메모리 비트맵과 역할 분리.)

## 영향

**긍정**
- domain이 순수 Kotlin 유지. 비트맵 표현을 플랫폼별로 교체 가능(테스트·향후 멀티플랫폼 여지).
- Android 타입 노출이 `core:util:android`·`data`로 국한.

**트레이드오프**
- data에서 `as? AndroidBitmap` 다운캐스트가 필요 — 인터페이스가 계약을 강제하지 못해 런타임 방어(`ImageNotFound`)에 의존.
- `BitmapWrapper`가 현재 **stub**(멤버 없음, `AndroidBitmap`도 `// TODO delegate 사용하도록 수정`). 실질 안전성이 다운캐스트에 달려 있는 과도기.

**위험·방어**
- 다운캐스트 실패는 `Result.failure(SegmentationException.ImageNotFound)`로 처리.
- 필요한 비트맵 연산이 정해지면 `BitmapWrapper`에 메서드를 정의해 다운캐스트 의존을 줄인다 → [open-questions 2026-07-12 BitmapWrapper stub](../../wiki/open-questions.md).

## As-built 갱신 (2026-08-14, PR #221)

결정은 유지되고 **적용 범위가 줄었다**.

- **`SegmentationResult`가 `BitmapWrapper`를 더 이상 담지 않는다** — 결과가 `subjectImagePath` +
  `subjectBounds`로 재편되며 도메인 **모델**에서 비트맵 추상이 빠졌다. 남은 사용처는 Repository
  **시그니처** 3개다: `decodeImage(uri): BitmapWrapper` · `segmentImage(bitmapWrapper)` ·
  `saveEditedImage(bitmapWrapper)`(신설).
- `saveEditedImage`도 같은 `as? AndroidBitmap` 다운캐스트 + `ImageNotFound` 방어를 반복한다 —
  다운캐스트 지점이 하나 늘었고, `BitmapWrapper`는 여전히 멤버 0인 stub이다
  → [open-questions](../synthesis/open-questions.md) [2026-07-12].
> 📌 **`saveEditedImage`는 2026-09-06 PR #457로 `saveBitmap`이 됐다.** 시그니처·다운캐스트 구조는
> 그대로이고 이름만 바뀌었다 — 아래 기록은 당시 이름을 유지한다.

- ⚠️ **화면 경계에서는 추상이 벗겨진다** — `SegmentationViewModel`·`ToppingEditViewModel`이
  `(wrapper as? AndroidBitmap)?.getRawData()`로 raw `android.graphics.Bitmap`을 꺼내 **UiState에
  직접 담는다**. 이 ADR이 규정하는 것은 domain 경계뿐이라 규약 위반은 아니지만, 다운캐스트가
  data 레이어 밖으로 나온 첫 사례다 →
  [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md) 드리프트 9.

## As-built 갱신 (2026-08-24, PR #342)

**위 "적용 범위가 줄었다"가 되돌아왔다.** 도메인 **모델**이 `BitmapWrapper`를 다시 문다 —
신설된 `SegmentationCandidate.bitmap`이 그것이고, 화면이 후보를 골라 탭할 때까지 파일을 만들지
않으므로 **경로가 아직 없는 구간을 비트맵으로 나를 수밖에 없다**
([c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)
Repository 계약 절). 대신 `SegmentationResult`는 반대로 더 가벼워져 경로 두 값만 남았다
(`subjectBounds` 제거).

- **Repository 시그니처는 넷이 됐다** — `decodeImage(uri): BitmapWrapper` ·
  `segmentImage(bitmapWrapper): Result<List<SegmentationCandidate>>` ·
  `persistSubject(candidate)`(신설, `candidate.bitmap`을 다운캐스트한다) ·
  `saveEditedImage(bitmapWrapper)`. 다운캐스트 지점이 셋이다.
- ⚠️ **화면이 이제 `BitmapWrapper`를 목록으로 들고 있다** — `SegmentationState.candidates`가
  최대 5개를 상태에 담고 각각이 자기 비트맵을 문다. 위 문단이 적은 "추상이 화면 경계에서
  벗겨진다"에 **수명 문제가 더해진 셈**이고, 명시적 해제가 없는 것은 미결로 남았다
  ([open-questions](../synthesis/open-questions.md) OQ-P-266·OQ-P-269).
