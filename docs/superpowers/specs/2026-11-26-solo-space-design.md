---
id: solo-space-design
title: 혼자 공간 — 서버 없이 기기에만 두는 캔버스 하나 (Solo Space)
status: draft
category: behavior-spec
platforms: android
verified: 2026-11-26
related_code: Navigator.kt#popUpTo, ToppingDraft.kt#ToppingDraft, ToppingDraftEntity.kt#ToppingDraftEntity, StartToppingDraftUseCase.kt#StartToppingDraftUseCase, CanvasMainScreen.kt#CanvasMainScreen, ToppingEditRoute.kt#ToppingEditRoute, CustomGalleryPickerRoute.kt#CustomGalleryPickerRoute, OnboardingPager.kt#OnboardingPager, ProfileCard.kt#ProfileCard, NavKeyAnalyticsScreen.kt#toAnalyticsScreenOrNull, BootstrapSessionUseCase.kt#BootstrapSessionUseCase
related_adr: ADR-0026, ADR-0029, ADR-0031
related_spec:
related_architecture: data-layer, navigation-flow, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, canvas, solo-space]
---

# Spec: 혼자 공간 (Solo Space)

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처다. 본문은 설계 내용에 집중한다.

## 목표

**왜**: 운영이 끝나 서버가 닫힌 뒤에도 앱으로 캔버스를 꾸밀 수 있게 남긴다. 서버가 없으면 로그인도,
그룹 목록도, 그룹 캔버스도 열리지 않는다. 혼자 공간은 그 상태에서 유일하게 살아 있는 기능이다.

**무엇**: TEAMYG-iOS `da3d27d`(혼자만들기 백도어 구현 #164)와 같은 혼자 공간이다.
- 기기에 하나뿐인 캔버스다. 로그인과 무관하고, 서버를 한 번도 부르지 않는다.
- 날짜별 캔버스가 아니라 캔버스 하나를 계속 쓴다. 처음 열 때 빈 캔버스로 만들어진다.
- 그룹 캔버스와 같은 화면(composable)을 쓰고, 혼자 쓰는 공간에 없는 요소만 걷어낸다.
- 숨은 진입점(5번 탭)으로만 들어간다.

**성공 기준**:
- 우리 서버에 닿지 못하는 상태에서 앱을 처음부터 켜서, 로그인 화면 → 혼자 공간 → 사진·배경·텍스트 편집 →
  이미지 저장까지 오류 없이 끝난다. *(내 판단)* 기준은 "우리 서버 없음"이지 "인터넷 없음"이 아니다. 종료 뒤에도
  기기 인터넷은 살아 있다. 누끼 모델(`play-services-mlkit-subject-segmentation`)은 처음 쓸 때 Play 서비스가
  내려받으므로(`SegmentationModuleInstaller`) 비행기 모드로 시험할 때는 모듈이 이미 설치된 기기를 쓴다.
- 「실기기 확인」의 iOS 대조 체크리스트를 통과한다.

## 범위

포함:
- 로그인 화면 온보딩 마지막 이미지의 체리를 5번 탭하면 혼자 공간으로 간다.
- 설정 화면의 내 프로필 카드를 5번 탭하면 혼자 공간으로 간다.
- 혼자 공간 캔버스와 그 하위 화면: 배경 편집, 토핑 배치, 토핑 재편집(이동·회전·테두리·삭제), 텍스트,
  이미지 저장·영상 저장·인스타그램 공유.
- 카메라·갤러리·누끼 흐름은 그룹과 같은 화면을 지나 혼자 공간 배치 화면으로 돌아온다.
- 햄버거 버튼은 비우기·삭제하기 메뉴를 연다. 각 항목은 확인 팝업을 거친다.

제외:
- 그룹 캔버스 데이터를 혼자 공간으로 옮기거나 미리 내려받기(사용자 확정: 빈 캔버스로 시작).
- 서버 동기화, 기기 간 공유, 계정별 분리.
- 날짜별 캔버스, 캘린더, 과거 캔버스, 마감, 멤버, 초대, 그룹 설정, 푸시.
- 진입점의 시각적 단서(탭 횟수 표시, 토스트). iOS와 같이 아무 반응 없이 5번째에 이동한다.
- 서버 종료 뒤 스플래시·로그인 화면 동작 변경(「서버 종료 뒤 경로」는 확인만 한다).

반목표 — 기술적으로 돌아가도 실패로 본다:
- 혼자 공간 경로가 서버를 한 번이라도 부른다. 운영 중에는 조용히 4xx가 나고, 종료 뒤에는 그 화면이
  로딩에 갇히거나 네트워크 오류 문구를 띄운다.
- 혼자 공간 작업이 운영 중인 그룹 캔버스 동작을 바꾼다.
- 저장 실패나 앱 종료로 기존 캔버스가 빈 캔버스로 덮인다.

## 접근

| 방식 | 내용 | 판단 |
|---|---|---|
| A. 특수 `GroupId` + `:data` 분기 | `GroupId(-1)`을 두고 기존 저장소 바인딩을 분기 구현으로 바꾼다 | 탈락. 분기가 빠진 경로가 가짜 id로 서버를 부르고, 컴파일러가 잡지 못한다. `todayCanvasRefreshFailures`·`getYears`·`getPastCanvases` 같은 날짜 전제 계약을 가짜로 채워야 한다 |
| **B. 혼자 공간 전용 ViewModel** | 화면 composable·UiState는 함께 쓰고, ViewModel과 저장소는 혼자 공간용을 따로 둔다 | **채택**(사용자 결정) |
| C. Hilt로 화면마다 구현 바꿔 끼우기 | ViewModel → UseCase → 저장소 사슬에 qualifier 판을 둔다 | 탈락. A와 같은 가짜 계약 문제가 남고 보일러플레이트가 B보다 많다 |

B를 고른 이유(구현 난도가 아니라 정확성·안정성·확장성 기준):
- **정확성**: 혼자 공간 ViewModel은 로컬 저장소만 의존한다. 서버를 부를 경로가 구조상 없다.
- **안정성**: 그룹 ViewModel에 UseCase를 더하거나 고쳐도 혼자 공간이 깨지지 않는다. 반대로 혼자 공간 작업이
  운영 중인 그룹 경로의 UseCase 계약을 바꾸지 않는다.
- **확장성**: 서버가 닫히면 그룹 캔버스는 더 고칠 일이 없다. 종료 뒤 기능을 더할 곳은 혼자 공간뿐이고,
  날짜·그룹 계약에 묶이지 않아 자유롭게 키울 수 있다.

B의 대가와 줄이는 방법:
- 배치·정렬·텍스트·배경 편집 ViewModel 로직이 두 벌이 된다. 화면 composable과 UiState는 그대로 함께 쓰고,
  ViewModel 안의 순수 계산은 `impl/util`의 internal 함수로 빼서 두 ViewModel이 부른다(「화면」).
- 혼자 공간 nav key를 따로 두면 토핑 흐름의 `popUpTo<NavKeyCanvasMain>()`이 혼자 공간에서 조용히 실패한다.
  표식 인터페이스로 푼다(「내비게이션」).

## 도메인

### 모델

```kotlin
/** 혼자 공간 캔버스. 원소 타입은 그룹 캔버스와 같아 화면 매퍼(`EditableToppingMapper`)를 그대로 쓴다 */
data class SoloCanvasVO(
    val background: CanvasBackground?,
    val toppings: List<CanvasToppingVO>,
    val texts: List<CanvasTextToppingVO>,
)
```

- 모든 토핑·텍스트는 `isMine = true`다. `placedBy`는 닉네임 "나" 하나로 고정한다.
- `createdAt`은 실제 배치 시각이다. 영상 저장(타임랩스)이 이 순서로 그린다.

### 저장소

```kotlin
interface SoloSpaceRepository {
    /** 처음 구독하면 파일을 읽는다. 파일이 없으면 빈 캔버스다. 읽기·파싱 실패는 실패로 흘린다(덮어쓰지 않는다) */
    val canvas: Flow<Result<SoloCanvasVO>>

    suspend fun changeBackground(edit: SoloBackgroundEdit): Result<Unit>
    /** [filePath]의 파일을 혼자 공간으로 복사해 배치한다. 올리기와 배치가 한 번에 끝난다 */
    suspend fun placeTopping(filePath: String, transform: ToppingTransform, border: ToppingBorder): Result<Unit>
    suspend fun updateTransforms(updates: List<ToppingTransformUpdate>, textUpdates: List<TextToppingTransformUpdate>): Result<Unit>
    suspend fun updateBorder(id: ParfaitImageId, border: ToppingBorder): Result<Unit>
    suspend fun deleteTopping(id: ParfaitImageId): Result<Unit>
    suspend fun placeText(text: String, transform: ToppingTransform, style: TextToppingStyle): Result<Unit>
    suspend fun deleteText(id: ParfaitTextId): Result<Unit>

    /** 배경·토핑·텍스트를 전부 비운다. 손상된 파일도 읽지 않고 빈 기록으로 덮는다 */
    suspend fun reset(): Result<Unit>
    /** 통째로 지운다. 다음에 열면 빈 캔버스다 */
    suspend fun delete(): Result<Unit>
}

sealed interface SoloBackgroundEdit {
    data class Color(val hex: String) : SoloBackgroundEdit
    data class Image(val filePath: String) : SoloBackgroundEdit
}
```

- 쓰기 결과는 `Unit`이다. 화면은 `canvas` 하나만 보고 그린다. 값을 얻는 길을 하나로 두는 것은
  [ADR-0029](../../adr/0029-canvas-today-ssot-polling.md)와 같은 원칙이다.
- 서버 계약과 달리 올리기·배치를 한 호출로 묶는다. 그래서 iOS의 "올렸지만 배치하지 않은 이미지(`uploads`)"
  정리가 필요 없다.
- 텍스트 내용 고치기(`updateText`)는 두지 않는다. 그룹 화면에도 그 UI가 없다(`TextToppingUpdate`를 쓰는
  feature 코드가 없다). 텍스트 이동·회전은 `updateTransforms`로 한다.
- id 타입은 그룹과 같은 `ParfaitImageId`·`ParfaitTextId`를 쓴다. 화면 모델(`EditableTopping`·`EditableText`)이
  이 타입을 들고 있기 때문이다.

### use case

`domain/usecase/solo/`에 저장소 메서드마다 하나씩 둔다: `GetSoloCanvasFlowUseCase`,
`ChangeSoloBackgroundUseCase`, `PlaceSoloToppingUseCase`, `UpdateSoloTransformsUseCase`,
`UpdateSoloToppingBorderUseCase`, `DeleteSoloToppingUseCase`, `PlaceSoloTextUseCase`,
`DeleteSoloTextUseCase`, `ResetSoloSpaceUseCase`, `DeleteSoloSpaceUseCase`.

### 토핑 초안 (ADR-0026)

카메라·갤러리·누끼 화면은 어느 공간에서 시작했는지 모른다. 그 흐름의 상태를 나르는 초안이 공간을 싣는다.

```kotlin
sealed interface ToppingDraftTarget {
    data class Group(val groupId: GroupId, val parfaitId: ParfaitId) : ToppingDraftTarget
    data object Solo : ToppingDraftTarget
}

data class ToppingDraft(
    val target: ToppingDraftTarget,   // groupId·parfaitId 를 대신한다
    val nextPositionZ: Int,
    ...
)
```

- `StartToppingDraftUseCase`는 `target`을 받는다. 그룹 호출부는 `ToppingDraftTarget.Group(groupId, parfaitId)`를 넘긴다.
- 저장 형태 `ToppingDraftEntity`는 `isSolo: Boolean = false`를 더하고 `groupId`·`parfaitId`를 nullable로 한다.
  업데이트 전에 저장된 초안은 그룹 초안으로 읽힌다.
- 초안을 읽는 그룹 ViewModel(`CanvasToppingPlaceViewModel`, `CanvasTextInputViewModel`)은 `Group` 초안만,
  혼자 공간 판(`SoloToppingPlaceViewModel`, `SoloTextInputViewModel`)은 `Solo` 초안만 받는다. 다른 쪽 초안을
  받으면 배치하지 않고 캔버스로 돌아간다. 그룹 텍스트 입력은 초안의 `groupId`를 애널리틱스 `group_id`와
  `refreshTodayParfaitDetail`에 쓰므로 `Group`에서 꺼내 쓰게 바꾼다.
- 초안을 시작하는 곳은 `CanvasMainViewModel`(그룹)과 `SoloCanvasViewModel`(혼자 공간)이다.

## 데이터

### 위치와 형식

- 경로: `filesDir/solo_space/canvas.json`과 `filesDir/solo_space/images/`. 계정 구분이 없다.
  로그아웃·탈퇴는 이 디렉터리를 건드리지 않는다(iOS와 같다).
- `canvas.json`은 `@LocalJson`으로 직렬화한다. 필드: `nextId`, `background`, `toppings`, `texts`.
- 이미지는 파일명만 저장한다. 읽을 때 디렉터리를 붙여 `file://` 절대 URI로 `CanvasToppingVO.imageUrl`·
  `CanvasBackground.Image.url`을 채운다.
- 이미지 파일명은 `<UUID>.png`(토핑) / `<UUID>.jpg`(배경)다. 서버 업로드 전처리(`UploadImagePreprocessor`)는
  거치지 않는다.

### 구현

- `SoloSpaceLocalDataSource`(`@Singleton`)와 `SoloSpaceRepositoryImpl`. remote 데이터소스에 의존하지 않는다.
- **메모리 SSoT**: 처음 구독할 때 파일을 한 번 읽어 `MutableStateFlow`에 둔다. 이후 읽기는 메모리에서 한다.
- **직렬화**: 모든 쓰기를 `Mutex` 하나로 줄 세운다.
- **원자적 저장**: 임시 파일에 쓰고 rename한다. 저장 중 앱이 죽어도 이전 기록이 남는다.
- **저장 실패**: 메모리 상태를 되돌리고 `Result.failure`를 낸다.
- **순서**:
  - 새 이미지는 먼저 `images/`에 복사하고, 기록 저장이 실패하면 그 사본을 지운다.
  - 버려진 이미지(교체된 배경, 지운 토핑)는 기록을 저장한 **뒤에** 지운다.
  - 비우기는 빈 기록을 먼저 저장한 뒤 `images/`를 비운다.
- **읽기 실패**: 파일이 없을 때만 빈 캔버스로 시작한다. 그 밖의 읽기·파싱 실패는 `canvas`에 실패로 흘리고
  파일을 덮지 않는다. 복구는 사용자가 메뉴의 "비우기"로 한다.
- **id**: `nextId`로 발급한다. 비운 뒤에도 이어 쓴다(화면이 들고 있던 옛 id와 겹치지 않게). 손상돼 읽을 수 없으면 1부터.
- 파일 작업은 `Dispatchers.IO`에서 한다.
- 로컬 I/O 실패는 네트워크 계열이 아닌 `AppError`로 바꿔 낸다. 어느 종류에 매핑할지는 구현자가 정한다.

### 백업

- `solo_space/`를 `data_extraction_rules.xml`의 **`cloud-backup`에서는 빼고 `device-transfer`에는 남긴다.**
  `backup_rules.xml`(API 30 이하 자동 백업)에서도 뺀다. *(내 판단)*
- 이유: 서버가 닫힌 뒤 기기를 바꾸는 사용자에게 혼자 공간을 넘겨줄 길은 기기 간 이전뿐이다. 클라우드 자동
  백업은 앱 전체 25MB 한도가 있고, 넘으면 그 앱의 백업 전체가 멈춘다. 사진 PNG가 쌓이면 쉽게 넘는다.

## 화면

### 모듈과 패키지

- 혼자 공간 화면은 `feature/groups/canvas/impl`의 `solo` 하위 패키지에 둔다. 화면 composable과 UiState가
  internal이라 같은 모듈이어야 다시 쓸 수 있다.
- 혼자 공간 ViewModel: `SoloCanvasViewModel`, `SoloBGEditViewModel`, `SoloToppingPlaceViewModel`,
  `SoloToppingArrangeViewModel`, `SoloTextInputViewModel`.
  - 의존해도 되는 것: `domain/usecase/solo/*`, 토핑 초안 use case, 기기 로컬 use case(최근 이미지,
    갤러리·영상 저장, 이미지 디코드 등).
  - 의존하면 안 되는 것: 서버를 부르는 저장소에 닿는 모든 use case, `AnalyticsLogger`.
- 이미지 저장 미리보기는 `NavKeyCanvasImageSave`를 그대로 쓴다. 그 ViewModel은 서버 의존이 없다.
- 그룹 ViewModel 안의 순수 계산(초기 배치 크기·위치, 다음 z 순서, 변형 스냅 처리 등)을 `impl/util`의
  internal 함수로 빼서 두 쪽이 함께 부른다. 그룹 ViewModel의 동작은 바뀌지 않아야 하고, 기존 테스트가 그대로
  통과해야 한다. 무엇을 뺄지는 구현자가 정한다.

### 혼자 공간 캔버스 (C-001과 같은 화면)

`CanvasMainScreen`을 쓴다. 그룹 전용 요소는 UiState 플래그나 nullable 콜백으로 숨긴다. 그룹 화면의 모습은
바뀌지 않는다.

`CanvasMainUiState`는 `todayCanvas: CanvasVO`를 들고, 화면은 `parfaitId`를 리빌 키로 쓴다. *(내 판단)*
UiState를 공간 중립으로 고치지 않고, `SoloCanvasViewModel`이 `SoloCanvasVO`를 `CanvasVO`로 바꾸는 매퍼 한 곳에서
고정값을 채운다: `parfaitId = ParfaitId(0)`, `groupName` = "내 공간", `date` = 오늘(`parfaitToday`),
`status = ACTIVE`, `lastClosedDate = null`, `members` 비움. 이유:
- 이 고정값은 화면 표시용으로만 쓰이고 저장소·use case 계약에 들어가지 않는다. 그 값으로 서버를 부를 경로가
  없으므로 접근 A를 떨어뜨린 문제(가짜 id가 서버로 나감)와 다르다.
- UiState를 바꾸면 그룹 ViewModel·화면·기존 테스트를 크게 건드려 반목표(그룹 동작 변경) 위험이 커진다.
- 서버가 닫힌 뒤 UiState를 혼자 공간에 맞게 정리하는 것은 그때 해도 늦지 않다.

- **제목**: "내 공간"(문자열 리소스).
- **상단 바**: 멤버 칩 없음.
- **날짜 헤더**: 오늘 날짜(`parfaitToday`, 새벽 3시 경계)를 글자로만 보인다. 캘린더 아이콘이 없고 누를 수 없으며
  접근성에서 버튼으로 읽히지 않는다. 날짜 경계를 넘기면 날짜만 바뀌고 캔버스는 그대로다.
- **없는 것**: 캘린더와 과거 캔버스, 과거 캔버스 알림과 넛지, 환영 배너, 마감, 폴링, 그룹 설정.
- **있는 것**: 카메라·갤러리 토핑 추가, 텍스트, 배경 편집, 재편집, 이미지 저장, 영상 저장, 인스타그램 공유.
- **햄버거**: 비우기·삭제하기 메뉴를 연다(Figma 시안 없음).
  - 모양은 그룹 목록의 "그룹 추가하기" 드롭다운과 같다. 흰 라운드 카드에 `YGActionItem` 두 개, 바깥은 딤,
    햄버거 아래로 펼친다. 순서는 "비우기", "삭제하기"이고 구분선으로 나눈다. 아이콘은 없다.
  - 딤이나 시스템 뒤로가기는 메뉴만 닫는다.
- **확인 팝업**(`YGModalPopup`, 아이콘 `ic_warning_round`, 왼쪽 그만두기 / 오른쪽 실행, 뒤로가기는 팝업만 닫음):
  - 비우기: 제목 "캔버스를 비울까요?", 본문 "올린 사진·글·배경이 모두 지워지고\n되돌릴 수 없어요",
    버튼 "그만두기" / "비우기".
  - 삭제하기: 제목 "내 공간을 삭제할까요?", 본문 "캔버스와 올린 사진이 모두 사라지고\n되돌릴 수 없어요",
    버튼 "그만두기" / "삭제하기".
- 메뉴·비우기 확인·삭제 확인은 한 번에 하나만 뜬다. 상태는 `SoloDialog { Menu, ResetConfirm, DeleteConfirm }?`
  하나로 둔다(`impl/model`).
- **실행 중**: 실행 버튼을 누르면 팝업을 바로 닫는다. 작업이 끝날 때까지 햄버거를 눌러도 메뉴를 열지 않고,
  연타해도 `reset()`·`delete()`는 한 번만 불린다.
- **결과**:
  - 비우기 성공 → 캔버스가 빈 상태로 다시 그려진다. 실패 → 에러 토스트
    "캔버스를 비우지 못했어요, 잠시 후 다시 시도해 주세요".
  - 삭제 성공 → 화면을 한 번만 닫고 들어온 화면(로그인 또는 설정)으로 돌아간다. 실패 → 에러 토스트
    "내 공간을 삭제하지 못했어요, 잠시 후 다시 시도해 주세요".
  - 캔버스 읽기 실패 → 로딩을 내리고 C-001-Error 토스트를 띄운다. 뒤로가기로 나갈 수 있고, "비우기"로 복구한다.
- 하위 화면(배경·배치·텍스트·재편집)에서 로컬 저장이 실패하면, 네트워크 문구("네트워크 연결을 확인하고…",
  "연결이 불안정해요…")가 아닌 문구를 띄운다. 문구는 구현자가 정한다.

## 내비게이션

### 키

`feature/groups/canvas/api`에 혼자 공간 키를 둔다: `NavKeySoloCanvas`, `NavKeySoloBGEdit`,
`NavKeySoloToppingPlace`, `NavKeySoloToppingArrange`, `NavKeySoloTextInput`.

### 돌아가기 — 표식 인터페이스

- 카메라·갤러리·누끼·배치 화면 아홉 곳이 `navigator.popUpTo<NavKeyCanvasMain>()` 또는
  `popUpTo<NavKeyCanvasBGEdit>()`로 돌아간다. `Navigator.popUpTo`는 클래스가 정확히 같아야 찾는다
  (`it::class == type`). 혼자 공간 키를 따로 두면 이 호출들이 아무것도 하지 않고 `false`를 돌려준다.
- 그래서:
  - canvas api에 표식 인터페이스 `CanvasHomeNavKey`(`NavKeyCanvasMain`, `NavKeySoloCanvas`가 구현)와
    `CanvasBGEditNavKey`(`NavKeyCanvasBGEdit`, `NavKeySoloBGEdit`가 구현)를 둔다.
  - `Navigator.popUpTo(type)`의 비교를 `type.isInstance(it)`로 바꾼다. 지금 키는 모두 final 클래스라
    기존 동작은 같다.
  - 위 호출부를 표식 인터페이스로 바꾼다. 백스택에서 가장 위의 것을 찾으므로 그룹·혼자 공간이 한 스택에
    함께 있어도 시작한 쪽으로 돌아간다.

### 배치 화면 고르기

- 누끼 편집(`ToppingEditRoute`)과 갤러리(`CustomGalleryPickerRoute`)는 지금 `NavKeyCanvasToppingPlace`로 간다.
  둘 다 초안의 `target`을 보고 `NavKeyCanvasToppingPlace` 또는 `NavKeySoloToppingPlace`로 간다.
  ViewModel이 초안을 읽어 이동 Effect에 실을지, Route가 고를지는 구현자가 정한다.

### 진입점

- **로그인**: `OnboardingPager`의 마지막 페이지(`image_onboarding_3`) 체리 위에 투명한 탭 영역을 둔다.
  - 영역은 그려진 이미지 경계 대비 비율로 잡는다: 너비 0.24, 높이 0.28, 중심 (0.5, 0.19). iOS와 같은 값이고
    Android 이미지에서도 체리(꼭지 포함)를 덮는다.
  - 5번째 탭에 `NavKeySoloCanvas`로 이동하고 횟수를 0으로 되돌린다. 횟수는 화면 상태에만 두고 저장하지 않는다.
  - 로그인 버튼과 페이저 스와이프를 막지 않아야 한다.
  - 로그인 디버그 모드(`feature/debug-mode`, develop 밖)가 먼저 merge되면 그 제스처와 겹치지 않는지 확인한다.
- **설정**: `ProfileCard` 5번 탭 → `NavKeySoloCanvas`. 같은 규칙이다.

## 애널리틱스

- 혼자 공간 ViewModel은 `AnalyticsLogger`를 의존하지 않는다.
- 화면 조회 매핑(`NavKeyAnalyticsScreen`, [ADR-0031](../../adr/0031-analytics-central-screen-mapping.md))은 혼자 공간
  키 다섯을 `null`로 둔다.
- 카메라·갤러리·누끼 화면처럼 그룹과 함께 쓰는 화면은 지금처럼 이벤트를 보낸다. *(내 판단: 이 화면들은 공간을
  모르고, 막으려면 그룹 경로 화면을 고쳐야 한다. 서버가 닫힌 뒤에는 대시보드를 볼 사람도 없다.)*

## 서버 종료 뒤 경로 (확인만 한다)

- 스플래시: `BootstrapSessionUseCase`는 연결 실패를 받으면 세션을 지우지 않고 `ToLogin`으로 보낸다.
  기다림은 최대 `CONNECT_TIMEOUT_SECONDS`(10초)다. 도메인이 풀리지 않으면 바로 실패한다.
- 강제 업데이트(`feature/force-update`, 아직 develop 밖): 버전 확인이 실패하거나 시간을 넘기면
  `NotRequired`로 통과한다(`CheckAppUpdateUseCase`, `SplashViewModel`).
- 그래서 서버가 닫혀도 로그인 화면까지 온다. 로그인 화면 체리가 종료 뒤 유일한 진입점이다. 설정 진입점은 운영 중에만
  닿는다.

## 파일 구성

- domain: `model/solo/SoloCanvasVO.kt`, `SoloBackgroundEdit.kt`, `model/topping/ToppingDraftTarget.kt`,
  `repository/solo/SoloSpaceRepository.kt`, `usecase/solo/*`, `ToppingDraft`·`StartToppingDraftUseCase` 변경
- data: `source/solo/local/SoloSpaceLocalDataSource(+Impl)`, `repository/solo/SoloSpaceRepositoryImpl`,
  `model/entity/SoloSpaceRecordEntity`와 매퍼, `ToppingDraftEntity` 확장, DI 바인딩
- core:navigation: `Navigator.popUpTo` 비교 변경
- feature/groups/canvas: api 키 다섯과 표식 인터페이스 둘, impl `solo` 패키지(Route·ViewModel·UiState 연결),
  `SoloDialog` 모델, 솔로 메뉴 컴포넌트와 프리뷰, 그룹 ViewModel 순수 계산 추출, 화면의 그룹 전용 요소 플래그
- feature/segmentation, feature/gallery, feature/camera: 표식 인터페이스로 `popUpTo`, 배치 화면 고르기
- feature/login: 체리 탭 영역 / feature/app/setting: 프로필 카드 탭
- app: `NavKeyAnalyticsScreen`, `data_extraction_rules.xml`, `backup_rules.xml`, 엔트리 등록
- docs: `architecture/navigation-flow.md`(진입), `architecture/data-layer.md`(로컬 저장소), `status.md`,
  B 선택을 남기는 ADR 하나

## 테스트

### JVM 유닛 — 저장소

임시 디렉터리와 고정 `Clock`을 쓴다.
- 파일 없음 → 빈 캔버스, 이후 쓰기 → 파일 생성
- 배치 후 이미지 파일 존재, 배경 교체·토핑 삭제 시 옛 파일이 기록 저장 **뒤에** 지워짐
- 저장 실패 시 메모리 롤백, 새로 복사한 이미지 사본 삭제
- 비우기: 빈 기록 저장 → `images/` 비움 순서, `nextId` 유지
- 삭제 후 다시 구독하면 빈 캔버스
- 손상된 `canvas.json` → 실패 방출, 파일 그대로, 비우기로 복구
- 동시 쓰기 여러 개가 겹치지 않음(Mutex)

### JVM 유닛 — 구조 보장

- 혼자 공간 ViewModel 다섯의 생성자 의존이 허용 목록(「모듈과 패키지」) 안에 있는지 확인하는 테스트를 둔다.
  누군가 서버 use case를 넣으면 테스트가 깨진다. 방식(리플렉션, Konsist 등)은 구현자가 정한다.
- `Navigator.popUpTo`: 표식 인터페이스로 찾기, 그룹·혼자 공간이 섞인 스택에서 가장 위를 찾기, 없으면 `false`.
- `ToppingDraftEntity`: `isSolo` 없는 기존 JSON이 그룹으로 읽힘, Solo 왕복.

### ViewModel

- `SoloCanvasViewModel`: 햄버거 → 메뉴, 확인 → reset/delete, 실패 토스트 Effect, 삭제 성공 시 닫기 Effect 1회,
  연타 시 1회 호출, 실행 중 햄버거 무시, 읽기 실패 → 로딩 해제와 오류 Effect.
- 나머지 혼자 공간 ViewModel: 그룹 판과 같은 시나리오를 가짜 저장소로.
- 그룹 ViewModel: 기존 테스트 전부 통과(순수 계산 추출의 회귀 확인).

### app 유닛

- `NavKeyAnalyticsScreenTest`: 혼자 공간 키 다섯은 화면 조회 없음.

### 실기기 확인 (iOS 대조 체크리스트)

- 비행기 모드로 앱 시작 → 로그인 화면 → 체리 5번 → 혼자 공간
- 설정 프로필 카드 5번 → 혼자 공간
- 사진(카메라·갤러리)·배경(색·사진)·텍스트 넣고 이동·회전·테두리·삭제
- 각 토핑 경로(누끼, 누끼 편집, 카메라, 사진 확인, 갤러리)의 취소·완료가 혼자 공간으로 돌아옴
- 그룹 캔버스의 같은 경로가 그대로 그룹 캔버스로 돌아옴
- `file://` 이미지가 캔버스, 이미지 저장, 영상 저장, 인스타그램 공유에서 보임
- 비우기, 삭제 후 다시 들어가면 빈 캔버스
- 앱 강제 종료 후 다시 열어도 그대로
- TalkBack: 날짜 헤더가 버튼으로 읽히지 않음, 메뉴·팝업 포커스

## 구현자에게 맡김

- 그룹 ViewModel에서 어떤 순수 계산을 뺄지.
- 배치 화면 고르기를 ViewModel Effect와 Route 중 어디서 할지.
- 로컬 I/O 실패의 `AppError` 매핑과 하위 화면 실패 문구.
- 구조 보장 테스트의 방식.
- 화면의 그룹 전용 요소를 UiState 플래그로 숨길지 nullable 콜백으로 숨길지.
