---
id: c102-custom-gallery-picker
title: C-102 커스텀 갤러리 선택 화면 (Custom Gallery Picker)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-21
related_code: CustomGalleryPickerScreen, CustomGalleryPickerViewModel, CustomGalleryPickerRoute, GalleryImageGridComponent, GalleryPermissionRequestComponent, GalleryPartialAccessBanner, GalleryPermissionManager, GalleryRepository, GalleryMediaProvider, LoadFilterYGGalleryImageGroupsUseCase, GetRecentCacheImagesUseCase, GalleryImageGroup, DayWindow, DateTextFormat, NavKeyCustomGalleryPicker, NavKeyPictureConfirm, PictureConfirmSource
related_adr: ADR-0002, ADR-0006, ADR-0016
related_spec: c101-camera-picture-confirm, c301-canvas-background-edit
related_architecture: navigation-flow, module-structure, data-layer
supersedes:
superseded_by:
tags: [spec, parfait, gallery, c102]
---

# Spec: C-102 커스텀 갤러리 선택 화면

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> 📌 **심볼 리네임(2026-08-17, #278)** — 아래 본문의 `CanvasImageAdd*`는 **당시 이름**이다. 현재 코드는 **`CanvasMain*`**(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`, `strings.xml` 키 `canvas_main_*`). 이름만 바뀌고 시그니처·동작은 불변이라 본문은 기록대로 둔다.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #191(`feature/#171-gallery-screen`)이 develop에
> 머지됐다(2026-08-04). 화면 골격 자체는 그 전부터 있었고(#182 라운드에서 권한 화면·`strings.xml`만
> 정리됨), 이번 PR이 **목록·헤더·빈 상태·부분 접근·선택 후 경로**를 실물로 채웠다. 아래는 머지 코드를
> 역기록한 것이며 설계 대조가 아니라 **정책(위키)·규약(parfait) 대조**로 드리프트를 표기한다.

> ➕ **상단바 개정(2026-08-28 작성 · PR #406 develop 머지, 2026-08-30, 이슈 #381)** —
> Figma C-102의 상단이 닫기 버튼만 있던 줄에서 **제목 "오늘 찍은 사진" + 우측 닫기**로 바뀌었다.
> 화면이 직접 조립하던 `Row` + `YGCircleButton`을 걷어내고 디자인시스템 `YGFloatingBar`를 쓴다.
>
> | 상태 | 상단바 |
> |---|---|
> | 목록(로딩 포함) | `YGFloatingBarTitle(title = gallery_today_photos_title)` |
> | 빈 상태(`isEmpty`) | `YGFloatingBarClose` — 제목 없음(작업자 지시) |
> | 권한 미허용 | `YGFloatingBarClose` — 시각적 변화 없이 중복 조립만 제거 |
>
> `Title` 변형 자체는 이 라운드에 신설했다 →
> [designsystem-bar-listdate-components 스펙](2026-08-01-designsystem-bar-listdate-components.md)의 같은 날짜 개정.
>
> **부수 효과**: 수제 닫기 버튼은 `contentDescription`이 `null`이었고 디자인시스템 버튼은 `"닫기"`를
> 갖고 있어, 두 화면의 닫기 버튼에 접근성 레이블이 생겼다.
>
> 상단바 아래 `Spacer(gap5)`와 인셋 처리는 건드리지 않았다.
>
> 📌 **머지 확인(2026-08-30)** — 머지 트리가 브랜치 팁과 같아 위 표가 그대로 as-built다.
> 빈 상태에 제목을 두지 않은 근거가 작업자 지시뿐인 것은 그대로 남는다 → OQ-P-331.
>
> 검증은 기계 검사뿐이다 —
> `compileDebugKotlin`·`ktlintMainSourceSetCheck`·`:feature:gallery:impl:testDebugUnitTest` 통과,
> **프리뷰·실기기 육안 대조는 하지 않았다.**

## 목표
캔버스에 올릴 사진을 **앱 자체 갤러리 화면**에서 고르게 한다. 시스템 피커(`SystemGalleryPickerScreen`)
대신 쓰는 커스텀 그리드이며, 고른 사진은 촬영 경로와 **같은 확인 화면**(C-101-confirm =
`PictureConfirmScreen`)으로 합류시킨다.

## 범위
- **포함**:
  - 권한 상태에 따른 화면 분기(`GalleryPermissionManager.GalleryAccessLevel` 5단계) —
    미허용은 `GalleryPermissionRequestComponent`, 허용(FULL·PARTIAL)은 목록.
  - 목록 구성 — "최근 업로드한 사진"(앱 캐시 이미지) 섹션 + 날짜 그룹 섹션, 3열 정사각 그리드.
  - 날짜 헤더 — `LocalDate`를 화면에서 `DateTextFormat.monthDayFormat`·`weekdayFormat`으로 포맷,
    날짜와 `(요일)`을 색이 다른 두 `Text`로 나눠 그린다.
  - 빈 상태 — 빈 그래픽(`image_gallery_empty`) + 안내 문구를 `isEmpty` 분기 **안에서** 그린다.
  - 부분 접근(PARTIAL) — 하단 "사진 재선택" `YGButton`으로 권한 재요청(선택 사진 관리) 진입.
  - 가이드 토스트 — 목록이 실제로 보이는 상태에서 1회만 `YGToastType.Edit`로 노출.
  - 선택 → `NavKeyPictureConfirm(uri, source = GALLERY)`로 확인 화면 이동.
  - `GalleryImageGroup.date` 타입 교체(문자열 → `LocalDate`)와 그에 따른 Repository·UseCase 시그니처
    변경, `GalleryMediaProvider`의 표시 포맷 삭제.
  - 빈 상태 그래픽을 벡터 드로어블에서 밀도별 PNG 세트로 교체.
- **제외**(이번 라운드에서 안 함):
  - 확인 화면 이후 경로 — "다음"(C-103 로딩)·닫기(C-001)는 여전히 TODO(카메라 경로와 공유).
  - ~~최초 권한 요청 UI — "설정으로 이동"만 있고 시스템 다이얼로그를 띄우는 경로는 PARTIAL 재선택뿐.~~
    → 2026-09-11 PR #489 develop 머지로 진입 시 자동 요청이 들어갔다(아래 「권한 요청 as-built 갱신」).
  - 다중 선택·정렬·앨범 전환.

## 동작 / 구조

### 권한 흐름
- Route가 `ON_RESUME`마다 `GalleryPermissionManager.resolveAccessLevelOnEnter`로 접근 수준을 다시 읽어
  `OnPermissionResult` 인텐트로 넘긴다. 허용이면 그 자리에서 목록을 다시 로드한다(확인 화면에서
  돌아올 때도 재로드된다).
- 요청 결과는 `resolveAccessLevelAfterRequest`로 해석한다. `RequestPermission` 효과만
  `permissionLauncher`를 태우고, 그 효과를 발신하는 것은 `OnRequestPermission`과
  `OnRequestManageMedia` 두 인텐트다 — 즉 **부분 접근 재선택과 최초 요청이 같은 launcher를 공유**한다.
  > 📌 **발신처가 하나 더 생겼다(2026-09-11, PR #489 develop 머지)**: VM이 첫 권한 없음 확인에서 직접 `RequestPermission`을
  > 발행한다(`requestPermissionOnce`). 위 두 인텐트 중 `OnRequestPermission`은 여전히 화면에서 부르는 곳이 없다
  > → 아래 「권한 요청 as-built 갱신」.

### 목록 로드
- `LoadFilterYGGalleryImageGroupsUseCase`만 쓴다 — `DayWindow.current`(하루 경계 03시)로 창을 잡아
  그 안의 사진만 조회하고, 타임스탬프에서 경계 시각을 뺀 뒤의 날짜를 그룹 키로 삼는다.
- "최근 업로드한 사진"은 갤러리가 아니라 `GetRecentCacheImagesUseCase`(앱 캐시)에서 오고, VM `init`에서
  Flow로 수집해 상태에 반영한다. 비어 있으면 헤더째 그리지 않는다.
- `isEmpty`는 상태의 파생 프로퍼티(모든 그룹이 비었고 최근 목록도 빈 경우)다.

### 그리드
- `LazyVerticalGrid` 고정 3열. 헤더는 `GridItemSpan(maxLineSpan)`으로 한 줄 전체를 차지한다.
- 셀은 `aspectRatio(1f)` + Coil `AsyncImage`(`ContentScale.Crop`), 셀 간격은 가로 `Arrangement.spacedBy`,
  세로는 셀 자신의 하단 패딩으로 준다(토큰 사용, 이전 라운드의 `dp` 리터럴 제거).
- 헤더 컴포저블이 **오버로드 2개**다 — 문자열 1개(최근 섹션)와 날짜+요일 2개(날짜 섹션).

### 선택 후 경로 (⚠️ 반환 → 이동으로 바뀜)
- 이전에는 `ReturnResult`가 `LocalResultEventBus`로 호출 화면에 URI를 **돌려줬다**. 이번 PR이
  `NavigateToConfirm`으로 바꿔 **확인 화면으로 전진**한다.
- 확인 화면은 카메라 것을 그대로 쓰고, 진입 출처를 `NavKeyPictureConfirm.source`
  (`PictureConfirmSource.CAMERA`/`GALLERY`)로 구분해 좌측 버튼 문구만 "다시 찍기"/"다시 선택"으로 가른다.
  이 때문에 `feature/gallery/impl` → `feature/camera/api` 의존이 새로 생겼다(`:api`만 참조하므로
  [module-structure](../../../architecture/module-structure.md) 규칙 준수).
- 부작용: 호출 화면 `CanvasImageAddRoute`의 `ResultEffect<String>`는 커스텀 갤러리로부터 결과를
  받지 못한다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-04].

> 📌 **두 번째 호출자와 반환 경로 부활(2026-08-15, PR #231)** — `NavKeyCustomGalleryPicker`가
> `data object` → `data class(showGuideToast, returnResultOnly)`로 승격됐다(기본값 유지 → 기존
> 동작 불변). C-301 배경 편집이 두 인자를 `false`/`true`로 주고 들어오면 **가이드 토스트가 꺼지고**,
> 확인 화면이 세그멘테이션으로 전진하는 대신 `PictureConfirmResult`를 `ResultEventBus`로 돌려준다
> (복귀는 확인 화면·갤러리를 걷는 `onBack()` 2회). 즉 이 화면이 결과 반환 플로우에 다시 참여하지만
> **결과를 만드는 주체는 확인 화면**이고 갤러리 자신의 `NavigateToConfirm` 경로는 그대로다
> → [c301 스펙](2026-08-15-c301-canvas-background-edit.md).

### 상태(MVI)
`CustomGalleryPickerState`: `isLoading` · `access` · `groups` · `recentImages`(+ 파생 `isEmpty`).
효과는 `RequestPermission` · `OpenAppSettings` · `NavigateToConfirm` · `NavigateToBack`.

## 정책 대조 (위키)

| 정책 항목 | 코드 | 판정 |
|---|---|---|
| C-102는 하루 경계 **03시** 창의 사진만 로드([[캔버스-마감-스케줄]]) | `DayWindow.DAY_BOUNDARY_HOUR` 기반 `loadFilterYGGalleryImages` | 일치 |
| 확인 화면 이미지 노출 = 비율 분기([[이미지-렌더링-정책]]) | `PictureConfirmScreen`의 `BoxWithConstraints` 분기 | 일치(아래 참고) |
| 갤러리 화면 자체의 레이아웃 정책 | 위키에 대응 소스 없음 | **대조 불가** — 그리드 열 수·간격·헤더 표기의 정책 근거가 없다 |

> ✅ **확인 화면이 [[이미지-렌더링-정책]]에 맞춰졌다(이번 PR)** — 이전 구현은 비율과 무관하게 이미지
> 영역에 항상 테두리를 둘렀다. 지금은 이미지 원본 비율과 영역 비율을 비교해 **가로가 먼저 차면
> 컨테이너·테두리 없이 단독 노출(Case A)**, **세로가 먼저 차면 흰 배경 + `Gray500` 테두리
> 컨테이너 안에 중앙 배치(Case B)**한다. 상세 as-built는
> [c101 스펙](2026-08-01-c101-camera-picture-confirm.md).
> 단 원본 크기를 아직 모르는 첫 프레임(비동기 로드 전)은 Case A로 떨어진다.

> ⚠️ **날짜·요일이 영문 약어**다(`DateTextFormat`이 `ENGLISH_ABBREVIATED`). 앱 UI는 한국어이고 정책
> 소스가 없다 — Top Bar 날짜와 **같은 미결**이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-04].

## 규약 대조 (parfait)
- **화면 컨테이너**: `featureCustomGalleryEntryBuilder`는 `YGScaffold { innerPadding }`을 정상 적용한다
  (카메라의 의도적 예외와 다름). 이번 PR이 화면 안의 `windowInsetsPadding(systemBars)`을 걷어내
  **목록 갈래에서는 인셋 이중 적용이 사라졌다**. 화면 최외곽 `YGScreen`은 쓰지 않는다(G-001·C-101과 같은 이탈).
  > ⚠️ **권한 갈래에는 이중 적용이 남아 있었다(2026-08-25, PR #350에서 해소)** — 위 문장이 세 주
  > 동안 화면 전체를 말하는 것처럼 읽혔으나, `GalleryPermissionRequestComponent`의 닫기 줄은
  > `windowInsetsPadding(WindowInsets.systemBars)`을 계속 걸고 있었다. 아래 「as-built 재정정」 참고.
  > 📌 **컨테이너가 Route로 내려갔다(2026-08-20, PR #309)** — 스캐폴드가 EntryBuilder의 `YGScaffold`에서
  > Route의 `YGScaffoldV2`가 됐고, `CustomGalleryPickerScreen`이 파라미터로 받아 직접 꽂던
  > `YGToastHost`·`toastPolicy`도 그때 걷혔다(정책 객체는 Route가 만들어 스캐폴드에 넘긴다).
  > ⚠️ **이 문단의 마지막 줄이 틀렸다.** 그 자리에 "갤러리 토스트는 이미 컨텐츠 영역 상단
  > 정렬이라 보이는 위치는 사실상 그대로다"라고 적어 왔는데, **실제로는 올라가서 닫기 버튼 행을
  > 덮었다.** 두 상단이 같지 않다 — 스캐폴드 호스트는 **상태바 인셋 바로 아래**이고 갤러리가
  > 말하던 "컨텐츠 영역 상단"은 **닫기 행 아래의 그리드 프레임 윗변**이다. 그 차이가 6일 뒤
  > #371로 되돌려졌다 → 아래
  > [as-built 재정정](#as-built-재정정-2026-08-26-pr-371-develop-머지).
- **문자열**: 목록 헤더·빈 상태·재선택 버튼·가이드 토스트가 모두 `feature/gallery/impl` `strings.xml`로
  갔다 — [2026-07-26 문자열 리소스화 항목](../../../synthesis/open-questions.md)이 지적한 갤러리 리터럴
  1건이 닫혔다. 단 **가이드 토스트 문구가 카메라 쪽과 문자 그대로 같은데 두 모듈에 각각 정의**된다.
- **클릭 규약 이탈**: 그리드 셀이 `Modifier.clickable`을 직접 쓴다. 다른 상호작용 요소가 쓰는
  `clickableYG`(leading-throttle)를 안 써서 연타 시 확인 화면이 중복으로 쌓일 수 있다
  (`YGDateButton` 선례와 같은 부류) → [open-questions](../../../synthesis/open-questions.md) [2026-08-04].
- **표시 포맷 위치**: `GalleryMediaProvider`가 갖고 있던 날짜 포맷이 사라지고 `data`는 `LocalDate`만
  넘긴다. 표시 포맷은 화면이 `core:util:jvm` `DateTextFormat`으로 만든다 —
  [data-layer](../../../architecture/data-layer.md) 레이어 배치와 정합하는 방향의 변경이다.
- **디자인시스템 재사용**: `YGCircleButton`(닫기)·`YGButton`(재선택)·`YGToastPolicy`/`YGToastHost`.
  그리드·헤더·빈 상태는 feature 로컬 컴포저블이고 대응 디자인시스템 컴포넌트가 없다.
  (닫기는 2026-08-28 개정으로 `YGFloatingBarTitle`·`YGFloatingBarClose`가 대신한다 → 위 개정 블록.)

## 파일 구성
| 파일 | 역할 |
|---|---|
| `feature/gallery/api/.../NavKeyCustomGalleryPicker.kt` | 목적지(인자 없음) |
| `feature/gallery/impl/.../route/CustomGalleryPickerRoute.kt` | 권한 재확인·효과 소비·토스트 1회 노출·확인 화면 이동 |
| `feature/gallery/impl/.../screen/CustomGalleryPickerScreen.kt` | 권한/목록 분기, 상단 바(2026-08-28 개정으로 `YGFloatingBar`), 로딩·빈 상태·그리드, 토스트 호스트, PARTIAL 하단 버튼 |
| `feature/gallery/impl/.../component/GalleryImageGridComponent.kt` | 3열 그리드 + 날짜/최근 헤더 오버로드 2종 |
| `feature/gallery/impl/.../component/GalleryPermissionRequestComponent.kt` | 권한 거부 화면(카메라와 공통 형태) |
| `feature/gallery/impl/.../viewmodel/CustomGalleryPickerViewModel.kt` | MVI 상태·인텐트·효과 |
| `feature/gallery/impl/src/main/res/values/strings.xml` | 헤더·빈 상태·재선택·가이드 토스트 문구 |
| `feature/camera/api/.../PictureConfirmSource.kt` | 확인 화면 진입 출처(CAMERA/GALLERY) |
| `domain/.../model/GalleryImageGroup.kt`·`repository/gallery/GalleryRepository.kt` | 날짜 키 `LocalDate`화 |
| `data/.../repository/gallery/GalleryRepositoryImpl.kt`·`utils/GalleryMediaProvider.kt` | 그룹핑 키 `LocalDate` 반환, 표시 포맷 제거 |
| `core/designsystem/res/drawable-*/image_gallery_empty.png` | 빈 상태 그래픽(벡터 → 밀도별 PNG 6종) |

## as-built 재정정 (2026-08-25, PR #350 develop 머지)
이슈 #345가 지목한 것은 **닫기 버튼이 설계보다 상태바 높이만큼 내려앉은 것**이었고, 원인은
`YGScaffoldV2`가 `innerPadding`을 넘겨줄 뿐 인셋을 소비하지 않는다는 성질이다 — Route가 그
`innerPadding`을 화면에 물린 위에서 권한 컴포넌트가 `windowInsetsPadding(systemBars)`을 한 번 더
걸면 상단은 상태바만큼 밀리고, 하단은 내비게이션 바 인셋이 닫기 줄 **아래**의 빈칸이 되어 가운데
안내 블록까지 밀려 내려간다.
- **중복 `windowInsetsPadding`이 제거됐다.** 이제 이 갈래의 인셋 소유자는 Route의 `YGScaffoldV2`
  하나이고, 권한이 있을 때 그려지는 `GalleryContent`와 같은 형태가 됐다. **카메라 권한 화면과는
  방향이 반대다** — 카메라는 Scaffold가 인셋을 안 주므로 컴포넌트가 계속 직접 문다
  → [c101 스펙](2026-08-01-c101-camera-picture-confirm.md). 「파일 구성」의 "카메라와 공통 형태"는
  **레이아웃 골격에 한한 말**이고 인셋 소유는 두 화면이 다르다.
- **최외곽이 `Column` → `Box`가 되고 닫기 줄이 겹쳐 놓인다.** 줄을 세로로 쌓으면 안내 블록이 줄
  아래 남은 공간의 가운데로 앉아 화면 기준으로는 줄 높이의 절반만큼 내려가 보이기 때문이고,
  안내 블록에는 하단 `padding3` 보정이 함께 붙었다.
- **실기기 확인 없이 머지됐다.** PR 본문이 스스로 밝힌다 — 권한 거부 상태를 로컬에서 재현하지
  못해 비교 이미지의 "After"가 실측이 아니라 이슈에 붙은 디자인 이미지다. 인셋은 기기의 실제
  시스템 바 높이에 붙는 값이라 단위 테스트로도 잠기지 않았다 →
  [open-questions](../../../synthesis/open-questions.md) [2026-08-25].
- 권한 흐름 자체(`onClickGrantPermission` 미호출)는 이 라운드에서 변하지 않았다.

## 주의 / 열린 질문
- **로딩 인디케이터가 흰 배경 위 흰색**이다 — 배경이 흰색으로 확정된 이번 PR 이후에도 그대로라
  로딩 중 화면이 비어 보인다([2026-08-01 갤러리 빈 상태 항목](../../../synthesis/open-questions.md) ② 잔존).
- **死코드 2건**: `GalleryPartialAccessBanner`(부분 접근 배너 — 하단 버튼으로 대체됐으나 파일 잔존,
  색·문구도 리터럴), `LoadAllGalleryImageGroupsUseCase`(전체 조회 UseCase — 호출자 0건).
- **최초 권한 요청 경로 부재**: `onClickGrantPermission`이 여전히 권한 화면에서 호출되지 않는다
  (카메라와 동일, [2026-08-01 항목](../../../synthesis/open-questions.md)). **#350이 이 컴포넌트를 다시
  짜면서도 건드리지 않았다.**
  > 📌 **진입 시 자동 요청이 들어갔다(2026-09-11, PR #489 develop 머지)**: 시스템 다이얼로그는 이제 뜬다.
  > `onClickGrantPermission` 미사용만 남는다(OQ-P-053 ③) → 아래 「권한 요청 as-built 갱신」.
- **확인 화면 이후 미결선**: 갤러리 경로도 같은 확인 화면으로 합류하므로 "다음"·닫기 TODO의 영향
  범위가 두 진입점으로 늘었다.
- 위 항목은 [open-questions](../../../synthesis/open-questions.md)에서 추적한다.

## as-built 재정정 (2026-08-26, PR #371 develop 머지)

> **토스트가 다시 그리드 프레임 안으로 들어갔다.** 권한 갈래·목록 갈래·PARTIAL 하단 버튼은 한 줄도
> 안 바뀌었다. 카메라와 **같은 커밋 묶음**이고 처방도 같다.
> 브랜치 `feature/toast-position-fix`, 머지 `bf06c830`.

- **증상**: 진입 안내 토스트가 상태바 인셋 바로 아래에 떠서 **닫기 버튼 행을 덮었다.** 위
  「규약 대조」의 "보이는 위치는 사실상 그대로다"가 이때 거짓이 됐고, 이 재정정이 그 문장을 고친다.
- **처방**: `CustomGalleryPickerScreen`이 `toastPolicy: YGToastPolicy`를 **필수 파라미터로 받고**,
  그리드 프레임 `Box`(닫기 행 + `gap5` 아래, `weight(1f)`) 안에 `YGToastHost`를 `TopCenter`로 심는다.
  Route는 스캐폴드에 정책을 넘기지 않는다. 로딩·빈 상태·그리드 세 갈래가 그 `Box`의 안쪽 형제라
  **셋 중 무엇이 그려져도 토스트 자리는 같다**.
- ⚠️ 스캐폴드가 자기 기본 정책으로 만든 **발행 불가능한 호스트**가 하나 더 있고, 호스트가
  **권한 허용 갈래 안에만** 있다(안내 토스트 조건이 이미 `access.hasPermission`을 요구해 지금은
  증상이 없다) → OQ-P-312 ②④.
- 이 화면은 `showError`를 쓰지 않는다 — 발행하는 것은 안내 토스트 하나뿐이다.

### 유닛 테스트

**0건이다**(저장소 전체 789·계측 14건 그대로). 배치 변경이라 유닛으로 덮을 수 없다.

## 권한 요청 as-built 갱신 (2026-09-11, PR #489 develop 머지)

> 브랜치 `bugfix/permission-not-required`, 커밋 `1d25a4bb9`. 2026-09-11 PR #489로 develop에 머지됐다
> (머지 `be537ea93`). 머지 전에 쓴 절이고, 머지 코드와 다시 대조해 어긋난 자리가 없었다. 카메라와 같은 커밋이고
> 처방도 같다. 결정의 근거와 CameraX 바인딩 결함은 [c101 스펙](2026-08-01-c101-camera-picture-confirm.md)의
> 같은 날짜 절에 있다.

- `CustomGalleryPickerViewModel#handleOnPermissionResult`의 권한 없음 갈래(DENIED·PERMANENTLY_DENIED)가
  `requestPermissionOnce()`를 부른다. `private var hasRequestedPermission`으로 진입당 한 번만 `RequestPermission`을
  발행하고, Route의 기존 효과 수집이 `permissionLauncher`를 띄운다.
- 갤러리는 요청 결과도 같은 `OnPermissionResult` 인텐트로 들어온다(`resolveAccessLevelAfterRequest`). 거부 콜백이
  권한 없음 갈래를 다시 타도 플래그가 서 있으므로 재요청하지 않는다.
- PARTIAL은 권한이 있는 갈래라 요청하지 않는다. 사진 재선택은 종전대로 하단 버튼의 `OnRequestManageMedia`가 맡는다.
- 다이얼로그 뒤에 설정 이동 화면이 보이는 것, 최초 거부와 영구 거부를 나누지 않은 것, `onClickGrantPermission`이
  쓰이지 않고 남은 것은 카메라와 같다(OQ-P-053 ③ 잔존).
  > ✅ **그 파라미터가 빠졌다(2026-09-20, PR #514)** — `GalleryPermissionRequestComponent`의
  > `onClickGrantPermission`과 `CustomGalleryPickerIntent.OnRequestPermission` 인텐트·핸들러가
  > 삭제됐다(OQ-P-053 ③ 해소). **launcher 경로는 그대로 돈다** — `RequestPermission` 이펙트를
  > 쏘는 자리가 진입 시 1회 자동 요청(`requestPermissionOnce`)과 `OnRequestManageMedia` 둘이라,
  > 사라진 것은 아무도 부르지 않던 세 번째 발신처뿐이다.
- 허용하면 launcher 콜백이 FULL 또는 PARTIAL을 넘기고 VM이 그 자리에서 목록을 로드한다. 카메라처럼 lifecycle
  재시작을 기다리는 바인딩이 없으므로 같은 결함은 코드상 생기지 않는다.

### 검증

`CustomGalleryPickerViewModelTest`에 3건을 더해 8건에서 11건이 됐다. 권한 없음이면 요청을 1회 발행하는지, 요청 뒤
거부가 거듭 들어와도 재요청하지 않는지, PARTIAL이면 요청하지 않는지를 본다. 앞의 두 건은 구현 전에 실패하는 것을
확인했고, PARTIAL 건은 구현 전에도 통과하는 가드다. 모듈 `ktlintCheck`도 통과했다. 갤러리 경로의 실기기 확인
기록은 없다.
