---
id: c301-canvas-background-edit
title: C-301 캔버스 배경 편집 화면 (배경/토핑 탭 + 색 팔레트 + 카메라·갤러리 배경 선택)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-26
related_code: CanvasBGEditRoute, CanvasBGEditScreen, CanvasBGEditViewModel, CanvasBGEditUiState, CanvasBGEditError, CanvasEditTab, CanvasBackgroundPaletteColors, NavKeyCanvasBGEdit, PictureConfirmResult, NavKeyCameraCustom, NavKeyCustomGalleryPicker, NavKeyPictureConfirm, CANVAS_ASPECT_RATIO, YGFloatingBarEditTab, YGModalPopup, YGCanvasBackground, YGScaffoldV2, ChangeCanvasBackgroundUseCase, UploadImageUseCase, ImageFileRepository, ImageFileLocalDataSource, UploadImageFormat, UnsupportedImageException, AppError.UnsupportedImage, Color.kt#toRgbHex, CanvasBGEditViewModel.kt#handleOnClickConfirm, CanvasBGEditViewModel.kt#saveBackground, CanvasBGEditViewModelTest
related_adr: ADR-0002, ADR-0005, ADR-0006, ADR-0007
related_spec: c001-canvas-main, c101-camera-picture-confirm, c102-custom-gallery-picker, c301-topping-edit-tab, designsystem-canvas-components, designsystem-bar-listdate-components
related_architecture: navigation-flow, design-system, state-management, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, c301]
---

# Spec: C-301 캔버스 배경 편집 화면

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> 📌 **심볼 리네임(2026-08-17, #278)** — 아래 본문의 `CanvasImageAdd*`는 **당시 이름**이다. 현재 코드는 **`CanvasMain*`**(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`, `strings.xml` 키 `canvas_main_*`). 이름만 바뀌고 시그니처·동작은 불변이라 본문은 기록대로 둔다.

> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #231(`feature/background-edit-screen`)이
> develop에 머지됐다(2026-08-15). 아래는 머지 코드를 역기록한 것이며, 설계 대조가 아니라
> **규약(parfait)·정책(위키) 대조**로 드리프트를 표기한다.

> **화면 ID 판정** — 위키 [[기능정의서-v3]]이 C-301을 "캔버스 배경 변경"에서
> **"파르페 편집 모드 진입"(배경 변경 + 토핑 편집 통합 진입점)**으로 개편했고, 이 화면은
> 배경/토핑 두 탭을 가진 편집 모드 진입 화면이라 C-301에 해당한다. 코드 심볼은
> `CanvasBGEdit*`(배경만 가리키는 이름)이다. 위키에는 [[기능정의서-v6]]의 "에딧 모드 삭제"
> 비고와 C-301~C-306 표 잔존이 **미결로 남아 있다** — 코드가 C-301을 실물로 만든 것은
> 그 미결의 판단 재료다(위키 [[open-questions]] 소관).

## 목표
C-001 캔버스 메인에서 캔버스 편집 버튼으로 들어와, 캔버스 배경을 **팔레트 색 8종** 또는
**카메라·갤러리 이미지**로 고르고 확인/그만두기로 빠져나온다.

## 범위
- **포함**
  - `NavKeyCanvasBGEdit`(`data object`) 신설 + `featureCanvasEntryBuilder`에 entry 추가(규약 기본형
    `YGScaffold { innerPadding -> … }`).
  - `CanvasBGEditRoute`·`CanvasBGEditScreen`·`CanvasBGEditViewModel` 신설(MVI 3분할).
  - 배경 미리보기(9:16) + 팔레트 행(갤러리·카메라 원 2종 + 색 원 8종, 가로 스크롤) +
    `YGFloatingBarEditTab`(배경/토핑 탭 + 닫기 + 확인).
  - 그만두기 확인 모달(`YGModalPopup`) — 닫기 버튼이 곧바로 나가지 않고 모달을 거친다.
  - **재사용 진입 플래그** — `NavKeyCameraCustom`·`NavKeyCustomGalleryPicker`가 `data object`에서
    `data class(showGuideToast, returnResultOnly)`로 바뀌고, `NavKeyPictureConfirm`에
    `returnResultOnly`가 붙었다. C-101-confirm이 이 값에 따라 **세그멘테이션으로 전진**하는 대신
    `PictureConfirmResult`(신설, `feature/camera/api`)를 `ResultEventBus`로 돌려준다.
  - `feature/groups/canvas/impl` `strings.xml`에 탭 라벨 2 + 모달 문구 4 추가.
  - C-001의 캔버스 편집 콜백 결선(`OnClickCanvasEdit` → `NavigateToCanvasBGEdit` → `goTo`).
  - `domain`에 `CANVAS_ASPECT_RATIO` 상수 신설(`model/CanvasConst.kt`).
- **제외**(이번 라운드에서 안 함)
  - **선택한 배경의 저장·반영** — 확인 이펙트가 `// TODO` 주석과 함께 `onBack()`만 한다.
    브랜치 중간에 "메인 캔버스에 반영"이 들어왔다가 되돌려졌다(커밋 `10e70809`).
  - **토핑 탭의 내용** — 탭 전환은 상태만 바뀌고 본문은 배경 편집 그대로다
    (→ 다음 라운드 PR #264에서 채워졌다, [c301-topping-edit-tab](2026-08-16-c301-topping-edit-tab.md)).
  - 저장된 기존 배경 불러오기(코드 TODO), 서버 연동, 유닛 테스트.

## 동작 / 구조

### 화면 구성
- 세로 `Column(fillMaxSize)`: 배경 미리보기 → 팔레트 행 → `YGFloatingBarEditTab`. 모달은
  `Column` 밖에서 `showQuitDialog`로 띄운다.
- 미리보기는 `aspectRatio(CANVAS_ASPECT_RATIO)` `Box`에 상하 `padding4`·**좌우 21dp 리터럴**
  (코드 주석 "21.dp 공통에 없음")·`Gray500` 테두리 1dp다. 이미지가 선택돼 있으면 `ContentScale.Crop`
  으로 채우고, 없으면 선택 색으로 배경을 칠한다.
- 팔레트 행은 `horizontalScroll` + `spacedBy(gap3)`. 원은 전부 36dp이고 안쪽 아이콘 24dp,
  테두리는 `Transparency.Black5`다.
  - 갤러리·카메라 원: 선택된 이미지의 **출처가 자기 쪽일 때만** 썸네일을 원 안에 깔고 아이콘 색을
    `White`로 바꾼다(아니면 `Gray100` 배경 + `Gray500` 아이콘).
  - 색 원: 선택 상태면 `Transparency.Black25` 오버레이 + `ic_check`. 선택 판정은
    `color == selectedColor && selectedImageUri == null`이라 **이미지를 고르면 색 선택 표시가 꺼진다.**

### 상태(MVI)
`CanvasBGEditUiState`: `selectedTab`(`CanvasEditTab` BACKGROUND/TOPPING) · `selectedColor`(Compose
`Color`, 기본값 = 팔레트 첫 색) · `selectedImageUri` · `selectedImageSource`(`PictureConfirmSource`) ·
`showQuitDialog`.

- 색 선택은 이미지 선택을 **비운다**(`selectedImageUri`·`selectedImageSource` → null). 반대로 이미지
  결과가 오면 색은 남겨 두고 이미지가 우선한다.
- 확인(`OnClickConfirm`)은 `selectedImageUri`가 있으면 `YGCanvasBackground.Image(url)`, 없으면
  `YGCanvasBackground.Solid(color)`를 만들어 `ConfirmBackground` 이펙트에 싣는다. **Route가 그 값을
  쓰지 않는다**(TODO).
- 닫기(`OnClickCloseButton`)는 `showQuitDialog = true`만 세우고, 모달 확인이 `NavigateBack`,
  취소·바깥 탭이 `OnQuitDialogCancel`이다.
- ViewModel은 UseCase·Repository를 갖지 않는다(`@Inject constructor()`).

### 배경 이미지 선택 왕복 (재사용 진입)
```
NavKeyCanvasBGEdit ─┬─▶ NavKeyCameraCustom(showGuideToast=false, returnResultOnly=true) ─┐
                    └─▶ NavKeyCustomGalleryPicker(동일 인자) ───────────────────────────┤
                                                                                         ▼
                                       NavKeyPictureConfirm(uri, source, returnResultOnly=true)
                                                    │ sendResult(PictureConfirmResult) + onBack ×2
                                                    ▼
                                             NavKeyCanvasBGEdit (ResultEffect<PictureConfirmResult>)
```
- `returnResultOnly = false`(기본)면 확인 화면은 종전대로 `goToAndPopCurrent(NavKeySegmentation)`으로
  **토핑 생성 플로우**로 간다. 같은 세 화면이 인자 하나로 두 플로우를 겸한다.
- `showGuideToast = false`는 카메라·갤러리 진입 시 가이드 토스트를 끈다 — 토핑 생성 경로에서만 띄운다.
- 복귀는 `sendResult` 직후 **`navigator.onBack()`을 두 번** 부른다(확인 화면 → 카메라/갤러리).
  각 호출에 주석으로 어느 화면을 걷는지 적혀 있다.

### 진입
- C-001 `YGCanvas`의 `editAction` → `CanvasImageAddIntent.OnClickCanvasEdit` →
  `CanvasImageAddEffect.NavigateToCanvasBGEdit` → `goTo(NavKeyCanvasBGEdit)`. C-001 스펙이 지적하던
  "캔버스 편집 콜백 빈 람다"가 닫혔다.
- 다만 **C-001 자체의 진입 경로가 여전히 0건**이라(`NavKeyCanvasImageAdd`를 `goTo`하는 호출자 없음)
  이 화면도 실행 중 도달할 수 없다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-12].

## 드리프트 / 잔존

1. ~~**편집 결과가 아무 데도 남지 않는다**~~ — ✅ **해소(PR #329, 2026-08-22)**. 확인이 서버 저장을
   마친 뒤에만 화면을 넘기고, 돌아간 캔버스 메인이 재조회로 그린다. 저장 경로와 그 라운드가 정한
   것은 아래 [as-built 재정정](#as-built-재정정-2026-08-22-pr-329-develop-머지) 절이 갖는다
   → [open-questions](../../../synthesis/open-questions.md) OQ-P-173.
2. **미리보기가 `YGCanvas`를 재사용하지 않는다** — 화면이 `Box` + `aspectRatio` + `border`로 직접
   그린다. 그래서 좌상단 컷 도형·Dot Grid·메뉴가 없고, 좌우 여백이 C-001의 `padding7`(20)이 아니라
   **21dp 리터럴**이다(코드 주석이 토큰 부재를 자인). 편집 중 보는 캔버스와 실제 캔버스가 다르다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
3. ~~**"토핑" 탭이 아무 일도 안 한다**~~ — ✅ **해소(PR #264, 2026-08-16)**. 탭이 선택·이동·크기·
   회전·삭제와 테두리 재편집 왕복으로 채워졌다. 그 라운드의 설계·드리프트는
   [c301-topping-edit-tab 스펙](2026-08-16-c301-topping-edit-tab.md)이 갖는다. 심볼 이름이
   여전히 `CanvasBGEdit*`인 것과 탭 라벨·enum 소유는 그대로다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
4. **State·Effect가 UI 타입을 든다** — `selectedColor`가 Compose `Color`이고 `ConfirmBackground`가
   디자인시스템 타입 `YGCanvasBackground`를 싣는다. 팔레트 `CanvasBackgroundPaletteColors`도
   ViewModel 파일의 public 상수이고 8종 중 5종이 **hex 리터럴**(토큰·`YGAtomicColors` 밖)이다
   → [state-management](../../../architecture/state-management.md) ·
   [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
5. **캔버스 비율 상수가 둘로 갈렸다** — `domain`에 `CANVAS_ASPECT_RATIO`가 신설됐는데
   `core:designsystem` `YGCanvas`에는 같은 값의 private `CANVAS_AREA_ASPECT_RATIO`가 이미 있다.
   화면 비율은 도메인 규칙이 아니라 표시 규격이다
   → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
6. **NavKey 인자가 데이터가 아니라 동작 플래그다** — `showGuideToast`·`returnResultOnly`는 화면이
   그릴 값이 아니라 **호출자가 고르는 분기**이고, `@Serializable` 백스택 키에 실린다.
   ~~복귀도 `onBack()` 2회로 스택 깊이를 가정한다~~ → ✅ **#309(2026-08-20)로 `popUpTo<NavKeyCanvasBGEdit>()`가
   됐다**(확인·닫기 둘 다). 깊이를 가정하지 않고, 목적지를 타입으로 특정할 수 있는 근거는
   `returnResultOnly = true`를 주는 곳이 이 화면 하나라는 것이다. 카메라 실패·취소도 같은 라운드에
   `sendResult(uri: String?)`를 그만뒀지만(인자 없는 `Cancel`), **이 화면이 실패를 아는 수단은 여전히
   없다** — 이제는 아무 결과도 오지 않는다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-178.
7. **클릭 규약 이탈** — 팔레트 원 2종·색 원이 `Modifier.clickable`을 직접 쓴다(`clickableYG`
   미사용, 갤러리 그리드 셀과 같은 부류) → [open-questions](../../../synthesis/open-questions.md) [2026-08-04].
8. **치수 리터럴** — 원 36dp·아이콘 24dp·테두리 1dp·미리보기 좌우 21dp가 토큰 밖이다.
   `SizeTokens`에 있는 값(`Size24`·`Size1`)도 리터럴로 적었고, 36·21은 스케일 자체에 없다
   (A-002가 남긴 "스케일 공백" 지적과 같은 부류).

## 정책 대조 (위키)

| 정책 항목 | 코드 | 판정 |
|---|---|---|
| C-301 = 파르페 편집 모드 진입(배경 변경 + 토핑 편집 통합, [[기능정의서-v3]]) | 배경/토핑 탭 화면 | 방향 일치, 토핑 탭은 #264로 채워짐 |
| 캔버스 Area 비율 9:16([[캔버스-반응형-레이아웃]]) | `aspectRatio(CANVAS_ASPECT_RATIO)` | 일치 |
| 캔버스 좌우 여백 20([[캔버스-반응형-레이아웃]]) | 미리보기 좌우 21dp 리터럴 | **불일치**(드리프트 2) |
| 캔버스 좌상단 컷 도형 + 날짜 라벨 | 미리보기에 없음 | **불일치**(미리보기가 캔버스 재현 아님) |
| 배경 팔레트 색 목록·개수 | 위키에 대응 소스 없음 | **대조 대상 부재** — 8종·색값이 코드로 확정 |
| 편집 그만두기 확인 문구 | 위키에 대응 소스 없음 | **대조 대상 부재** |
| 배경 이미지의 크롭·비율 규칙([[이미지-렌더링-정책]]은 단독 노출용) | 미리보기 `ContentScale.Crop` | 대조 대상 부재(정책은 확인 화면 대상) |

## 규약 대조 (parfait)
- **화면 컨테이너**: entry가 규약 기본형(`YGScaffold { innerPadding -> …padding(innerPadding) }`)이다.
  화면 최외곽 `YGScreen`은 쓰지 않는다(C-001·G-001·A-002와 같은 이탈).
- **문자열**: 탭 라벨·모달 문구가 전부 `feature/groups/canvas/impl` `strings.xml`이다
  ([2026-07-26 항목](../../../synthesis/open-questions.md) 규약 준수).
- **디자인시스템 재사용**: `YGFloatingBarEditTab`(C-104/C-105 편집 화면에 이은 **두 번째 실화면
  소비처**)·`YGModalPopup`(**7번째 소비처**, 파괴적 액션=좌 Secondary 배치 진영)·`YGAtomicColors`.
  팔레트 원은 화면 로컬 private 컴포저블이고 대응 컴포넌트가 없다.
- **MVI**: 3분할 계약을 지키고 이펙트 수집은 Route 한 곳이다. Intent 6종이 `class`(무인자)라
  기존 화면들과 같은 형태다.

## 파일 구성

```
feature/groups/canvas/api/
  NavKeyCanvasBGEdit.kt                신설(data object)
feature/groups/canvas/impl/
  route/CanvasBGEditRoute.kt           신설 — ResultEffect + 이펙트 수집
  screen/CanvasBGEditScreen.kt         신설 — 미리보기·팔레트·플로팅바·모달 + 프리뷰
  viewmodel/CanvasBGEditViewModel.kt   신설 — UiState/Intent/Effect + 팔레트 상수 + CanvasEditTab
  navigation/EntryBuilder.kt           entry 추가
  route/CanvasImageAddRoute.kt         편집 버튼 결선 + NavKey 생성자 호출로 변경
  viewmodel/CanvasImageAddViewModel.kt Intent/Effect 1종씩 추가
  res/values/strings.xml               6줄 추가
feature/camera/api/
  NavKeyCameraCustom.kt                data object → data class(showGuideToast, returnResultOnly)
  NavKeyPictureConfirm.kt              returnResultOnly 추가
  PictureConfirmResult.kt              신설(uri, source)
feature/camera/impl/
  navigation/EntryBuilder.kt           navKey 인자 전달
  route/CustomCameraRoute.kt           토스트 게이팅 + returnResultOnly 전달
  route/PictureConfirmRoute.kt         확인 분기(결과 반환 vs 세그멘테이션 전진)
feature/gallery/api/
  NavKeyCustomGalleryPicker.kt         data object → data class(동일 인자)
feature/gallery/impl/
  navigation/EntryBuilder.kt           navKey 인자 전달
  route/CustomGalleryPickerRoute.kt    토스트 게이팅 + returnResultOnly 전달
domain/
  model/CanvasConst.kt                 신설(CANVAS_ASPECT_RATIO)
```

## as-built 재정정 (2026-08-22, PR #329 develop 머지)

> **드리프트 1이 닫혔다** — 고른 배경이 서버에 저장된다. 아래는 머지 코드를 역기록한 것이고,
> 이 절의 서술이 위 본문과 어긋나면 이 절이 현행이다.

### 저장 경로

확인 버튼이 세 갈래로 갈린다.

| 고른 것 | 하는 일 |
|---|---|
| 팔레트 색 | `Color.toRgbHex()`로 `#RRGGBB`를 만들어 `CanvasBackgroundEdit.Color`로 PATCH |
| 기기에서 고른 사진 | `UploadImageUseCase`(캐시 복사 → 발급 → S3 PUT → confirm)로 `imageId`를 얻어 `CanvasBackgroundEdit.Image`로 PATCH |
| 서버에 이미 있던 배경 | **요청 0건** — 이펙트만 쏘고 끝난다 |

세 번째 갈래를 가르는 것은 `selectedImageSource`가 null인지다. 서버 배경의 URL은 기기가 읽을 수
없어 다시 올릴 수도 없고, 바뀐 것이 없으니 보낼 것도 없다.

**저장이 끝나야 화면을 넘긴다.** `ConfirmBackground`를 먼저 쏘면 캔버스 메인이 저장되지 않은
배경을 그린 채 서 있다가 다음 조회에서 슬그머니 되돌아간다.

### 이 라운드가 정한 것

- **성공인데 그릴 수 없는 응답**(앱이 모르는 `type` → `null`)은 **실패로 다루지 않는다.** 저장은
  끝났으므로 화면을 막을 이유가 없고, 고른 값으로 그린다 → OQ-P-193 ①.
- **Route는 이펙트에 실린 배경을 쓰지 않는다.** 되돌아간 캔버스 메인이 다시 조회해 그리므로
  값을 실어 나를 이유가 없다 → OQ-P-194 ③(재조회 쪽으로 결정).
- **화면 타입은 남았다.** `YGCanvasBackground`를 이펙트가 계속 싣고, 도메인 타입을 화면까지
  쓰지 않는다 → OQ-P-194 ①은 그대로 열려 있다.
- **저장된 배경을 팔레트 시작점으로 읽는다** — 위 [범위](#범위)의 제외 항목 "저장된 기존 배경
  불러오기"가 함께 닫혔다. 색을 못 읽으면 기본 색으로 두는데, 그 상태로 확인을 누르면 배경이
  팔레트 첫 색으로 **바뀐다**(못 읽는 값을 되돌려 보내지 않는다).

### 실패 표현

`CanvasBGEditError` enum 3종(`NETWORK`·`UNSUPPORTED_IMAGE`·`UNKNOWN`) + `strings.xml` 3줄이
신설되고 `YGScaffoldV2`의 토스트로 나간다. 갈래를 셋으로 둔 근거는 사용자가 다르게 굴 수 있는
경우가 셋뿐이라는 것이다 — 다시 시도하면 될 일인가, 이 사진 자체가 안 되는 것인가, 그 밖인가.

⚠️ **서버 409 `PARFAIT_ALREADY_CLOSED`는 `UNKNOWN`으로 접힌다.** 마감된 캔버스에 배경을 보내면
영원히 실패하는데 문구는 "잠시 후 다시 시도해 주세요"다. C-106 배치가 같은 코드를 되감기 판정에
쓰는 것과 처분이 갈렸다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-261.

### 화면 컨테이너·진입 인자

- `NavKeyCanvasBGEdit`가 `data object` → **`data class(groupId, parfaitId)`**가 됐다. 편집 대상은
  언제나 오늘의 캔버스인데 그 id는 오늘 조회를 이미 마친 캔버스 메인만 안다 — 편집 화면이 스스로
  오늘 조회를 부르면 캔버스가 없는 날에는 서버가 캔버스를 새로 만든다. ViewModel은
  `@AssistedInject` + `hiltViewModel(creationCallback = …)`으로 두 값을 받는다.
- C-001은 **오늘 캔버스를 못 받았으면 편집을 열지 않는다**(`todayCanvas` null이면 로그만 남기고
  버튼이 조용히 안 먹는다). 위 [규약 대조](#규약-대조-parfait)의 "entry가 규약 기본형"은
  **더 이상 사실이 아니다** — entry에서 `YGScaffold` 껍질을 걷고 Route가 `YGScaffoldV2`를 직접
  든다(실패를 토스트로 알려야 하고, 두 겹이면 인셋 패딩이 두 번 먹는다).
- 이펙트 수집은 코루틴이라 그 안에서 `stringResource`를 부를 수 없어, Route가 **문구 사전**
  (`CanvasBGEditError.entries.associateWith { it.toStringResource() }`)을 컴포지션에서 미리 뽑아
  둔다. 실패 문구가 여러 갈래인 화면의 첫 사례다.

### 다시 여는 사이 날이 바뀌면

ViewModel이 진입 시 오늘 조회를 한 번 더 부르고(편집을 여는 사이 다른 멤버가 올린 토핑까지
그려야 한다), 응답의 `parfaitId`가 진입 인자와 다르면 **조회 쪽으로 저장 대상을 옮긴다.**
화면에 그려진 토핑과 저장 대상이 갈라지는 편이 더 나쁘다는 판단이다.

### 유닛 테스트

이 화면은 테스트가 0건이었고 `CanvasBGEditViewModelTest` **15건**이 붙었다 — 저장 세 갈래,
업로드·저장 각각의 실패, 못 읽는 사진, 저장된 배경으로 열기(색·이미지), 토핑 비율 배치·정렬·
소유 판정·테두리 펴기다. 캔버스 메인 쪽도 편집 진입 두 건이 늘었다.

### 잔존

- 드리프트 2(미리보기가 `YGCanvas` 재사용 아님)·4(State·Effect가 UI 타입)·7(클릭 규약)·
  8(치수 리터럴)은 그대로다.
- **업로드 캐시가 쌓이기만 한다** — `ImageFileLocalDataSourceImpl`이 고른 사진을
  `cacheDir/upload`에 UUID 이름으로 떨구는데 지우는 코드가 없다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-262.

## as-built 재정정 (2026-08-23, PR #336 develop 머지)

> **확인 버튼이 더는 배경만 다루지 않는다.** 배경 축의 세 갈래는 그대로이고, 그 앞에 토핑 저장이
> 붙었다. 토핑 쪽 설계는
> [c301 토핑 탭 스펙](2026-08-16-c301-topping-edit-tab.md#as-built-재정정-2026-08-23-pr-336-develop-머지)에 있다.

- 위 [저장 경로](#저장-경로)의 표는 `saveBackground()`로 이름을 얻어 **그대로 남았다.** 달라진 것은
  그 앞이다 — `handleOnClickConfirm`이 바뀐 토핑을 먼저 전부 PATCH 하고 나서 이 흐름을 태운다.
  코루틴 키도 `SAVE_BACKGROUND_KEY` → **`CONFIRM_KEY`**로 바뀌어 확인 버튼 전체가 한 단위다.
- 그래서 **"요청 0건"이던 세 번째 갈래가 더는 요청 0건이 아니다** — 서버 배경을 그대로 둔 채
  토핑만 옮기고 확인하면 배경 요청은 여전히 없지만 토핑 PATCH는 나간다.
- ⚠️ **두 축의 실패 처분이 갈렸다.** 배경 실패는 위 [실패 표현](#실패-표현)대로 토스트로 나가고
  화면이 남는데, 토핑 실패는 로그 한 줄이고 배경이 성공하면 화면이 넘어간다
  → [open-questions](../../../synthesis/open-questions.md) OQ-P-275.


## as-built 재정정 (2026-08-26, PR #376 develop 머지)

> **이 화면의 의존 하나가 줄었다.** `CanvasBGEditViewModel`이 `GetMyAccountFlowUseCase`를 더는 받지
> 않고, 진입 시 계정 SSoT를 `first()`로 기다리던 줄도 없어졌다. 토핑 소유 판정이 서버가 준 값
> (`placedBy.ownerType` → `CanvasToppingVO.isMine`)으로 바뀌어 계정 id가 필요 없어졌기 때문이다.
> 설계 서술은 [c301 토핑 탭 스펙](2026-08-16-c301-topping-edit-tab.md#as-built-재정정-2026-08-26-pr-376-develop-머지)에 있다.

- 위 [유닛 테스트](#유닛-테스트) 절이 세는 **15건과 그 구성은 그대로**다(현재 이 클래스는 22건).
  달라진 것은 픽스처다 — `MyAccountVO` 스텁이 사라지고 토핑 픽스처가 `isMine`을 직접 받는다.
- 배경 축(저장 세 갈래·실패 표현·`CONFIRM_KEY`)은 이 delta에서 한 줄도 안 바뀌었다.
