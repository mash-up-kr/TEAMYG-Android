---
id: a005-group-create
title: A-005 그룹 생성 화면 (GroupCreate)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-01
related_code:
  - NavKeyGroupCreate
  - GroupCreateRoute.kt#GroupCreateRoute
  - GroupCreateScreen.kt#GroupCreateScreen
  - GroupCreateViewModel.kt#GroupCreateViewModel
  - GroupCreateViewModelTest
  - CheckNameValidUseCase.kt#CheckNameValidUseCase
  - CreateGroupUseCase.kt#CreateGroupUseCase
  - CreateGroupUseCaseTest
  - ParfaitGroupRepository.kt#createGroup
  - ServerErrorCode.kt#ParfaitGroup
  - YGModalPopup.kt#YGModalPopup
  - Navigator.kt#goToSingleClearTop
  - NameValidResult.kt#NameValidResult
  - GroupCreateConfig.kt#GroupCreateConfig
  - VerticalGridLayout.kt#VerticalGridLayout
  - EntryBuilder.kt#featureGroupCreateEntryBuilder
  - feature/groups/enter/impl/res/values/strings.xml
  - core/ui/res/values/strings.xml
related_adr: ADR-0005, ADR-0006, ADR-0009, ADR-0016
related_spec: s102-group-nickname, s002-account-info
related_architecture: state-management, navigation-flow, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, groups, group-create, a005]
---

# Spec: A-005 그룹 생성 화면 (GroupCreate)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> **사후 기록(post-hoc)**: 타 작업자 구현이 선작성 스펙 없이 develop 머지(#179, 2026-07-27)됨.
> as-built 역기록. 코드가 SoT. 입력 유효성·인원 상한은 위키 정책 [[A-005-그룹명-정책-v0.1]]·[[이름-입력-규칙]]·[[그룹]]과 대조 완료(일치).
>
> **as-built 갱신(2026-08-12, #224)**: 확인 버튼이 곧바로 다음 단계로 가지 않고 **생성 확인 모달**을 띄우도록
> 바뀌었고, 모달 확인이 `CreateGroupUseCase`(mock)를 거쳐 **G-001 그룹 목록으로 복귀**하는 데까지 결선됐다.
> 스펙이 "제외(구현 TODO)"에 뒀던 다음 화면 네비게이션이 닫히고, 진입 경로는 #222에서 이미 뚫렸다
> (G-001 그룹 추가 오버레이). **그룹 생성 API는 여전히 미연동** — UseCase가 고정 지연 후 성공만 반환한다.
>
> ⚠️ **as-built 갱신(2026-08-15, #243 develop 머지)**: **그룹 생성이 실서버를 탄다.**
> `CreateGroupUseCase`가 `ParfaitGroupRepository.createGroup`을 호출하고(`POST /api/parfait-groups`),
> 인자가 `(groupName: String, groupNumber: Int)` → **`(GroupName, GroupNickname, memberLimit)`** 도메인
> 타입으로 바뀌었으며 응답은 `CreatedGroupVO`다. 실패 갈래는 `ServerErrorCode.ParfaitGroup` 코드별로
> 갈라 로그를 남기고 **팝업은 닫지 않는다**. 같은 라운드가 유효성 문자 집합을 **서버 정규식에 맞춰 좁혔다**
> (아래 "유효성 규칙" 절).

- **화면 ID**: A-005 (새 그룹 생성 — 그룹명 + 최대 인원수 입력)
- **대상 모듈**: `feature/groups/enter/impl`(`groupcreate/`) + `feature/groups/enter/api`(NavKey) + `domain`(공용 UseCase·설정) + `core:ui`(공용 레이아웃·에러 문자열)

## 목표

그룹명·그룹 인원(최대 인원수)을 입력받아 새 그룹을 만드는 화면. 직전 단계에서 입력한
그룹 내 닉네임을 인자로 받아 읽기 전용으로 함께 보여준다. 확인 시 그룹명 유효성 검사를 통과해야 다음 단계로 넘어간다.

## 범위

- 포함: 그룹명 입력(최대 10자)·닉네임 읽기 전용 표시·인원 선택 그리드(1~12)·확인 버튼 활성 조건·확인 시 그룹명 유효성 검사·에러 인라인 노출·입력 시 에러 초기화·뒤로가기.
  **#224 추가**: 생성 확인 모달·생성 중 재진입 가드·생성 후 그룹 목록 복귀.
- 제외(구현 TODO):
  - ~~**그룹 생성 API 연동**~~ — ✅ **해소(#243)**. `POST /api/parfait-groups` 호출 + 코드별 실패 분기.
    다만 **실패 표현은 여전히 로그뿐**이다(모달이 열린 채 남는다 — 안내 없이 닫으면 아무 일도 없던 것처럼
    보인다는 코드 주석). 실패 토스트가 한 번 들어왔다가 **정책이 없다는 이유로 같은 PR 안에서 걷혔다**.
  - ~~**다음 화면 네비게이션**~~ — #224에서 `goToSingleClearTop(NavKeyGroupList)`로 결선(아래 "동작 / 상태").
  - ~~**진입 경로**~~ — #222에서 G-001 그룹 추가 오버레이가 `goTo(NavKeyGroupCreate(nickName))` 호출자가 됐다.
    다만 넘기는 `nickName`이 mock이고 진입 관계 자체는 미결 → [open-questions](../../../synthesis/open-questions.md) [2026-07-29].

## API / 인터페이스

```kotlin
// api — 인자 있는 NavKey(선례: NavKeySegmentation·NavKeyCanvasEdit·NavKeyPictureConfirm)
@Serializable data class NavKeyGroupCreate(val nickName: String) : NavKey

// domain — 그룹 생성/참여 공용 상수
object GroupCreateConfig {
    const val GROUP_NAME_MAX_LENGTH   // 그룹명 상한(위키 정책 10자)
    const val NICKNAME_MAX_LENGTH     // 닉네임 상한(위키 정책 15자)
    const val GROUP_COLUMN_COUNT      // 인원 그리드 열 수
    val GROUP_COUNT_LIST              // 선택 가능한 인원 목록(1..12)
}

// impl — MVI. 표시 문자열은 리소스 ID로 보유(as-built, ADR-0016 방향)
data class GroupCreateUiState(
    val groupName: String = "",
    val nickName: String = "",
    val groupNumber: Int? = null,
    val groupNameErrorTextResId: Int? = null,   // #223(2026-08-13)에 groupNameError: NameValidResult.Error? 로 교체
    val isConfirmPopupVisible: Boolean = false,   // #224 신설
    val isCreating: Boolean = false,              // #224 신설
) : UiState {
    val isValid: Boolean   // groupName·nickName 비어있지 않고 groupNumber 선택됨
}

sealed interface GroupCreateIntent : UiIntent {
    data object ClickNextButton; data object ClickBackButton
    data class InputGroupName(val newGroupName: String)
    data class ClickGroupNumber(val newSelectedNumber: Int)
    data object ClickConfirmPopupCreate; data object DismissConfirmPopup   // #224 신설
}
sealed interface GroupCreateSideEffect : UiSideEffect { data object NavigateToBack; data object NavigateToNext }

// domain — 그룹 생성(#224 신설, 🔁 #243에서 실서버 연동)
class CreateGroupUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(
        groupName: GroupName,
        groupNickname: GroupNickname,
        memberLimit: Int,
    ): Result<CreatedGroupVO>
    // envelope 가 성공이어도 groupId <= 0 이면 계약 위반으로 보고 AppError.Unexpected 로 되돌린다 —
    // 화면마다 다시 검사하면 한 곳만 빠져도 0 인 ID 로 다음 요청이 나간다
}

// ViewModel — 닉네임을 NavKey 인자로 받으므로 Assisted 주입(선례: SegmentationViewModel)
@HiltViewModel(assistedFactory = GroupCreateViewModel.Factory::class)
class GroupCreateViewModel @AssistedInject constructor(
    @Assisted nickName: String,
    private val checkNameValid: CheckNameValidUseCase,
    private val createGroup: CreateGroupUseCase,   // #224 신설
) : BaseViewModel<…>(initialState = GroupCreateUiState(nickName = nickName))
```

## 동작 / 상태

- **그룹명 입력**(`InputGroupName`): `groupName` 갱신 + 에러 필드 `null`(입력 시 에러 즉시 해제).
  > 🔁 **as-built(#223, 2026-08-13)**: 필드가 `groupNameErrorTextResId: Int?` → **`groupNameError: NameValidResult.Error?`**로 바뀌고, VM의 `when` 5분기가 `is NameValidResult.Error` 2분기로 접혔다. 표시 변환은 화면이 `groupNameError?.toStringResource(NameFieldType.GROUP_NAME)`로 한다 — 이 화면이 유일한 `GROUP_NAME` 소비처다([S-101 라운드](2026-08-07-s101-group-side-menu.md)의 ADR-0016 원안 수렴).
- **인원 선택**(`ClickGroupNumber`): `groupNumber` 갱신(단일 선택, 토글 해제 없음).
- **확인**(`ClickNextButton`): `CheckNameValidUseCase(groupName)` 실행 → `Success`면 에러를 지우고
  **확인 모달을 연다**(🔁 #224 — 이전엔 곧바로 `NavigateToNext`였다). `Error` 변형이면 대응 `core:ui`
  문자열 리소스 ID를 state에 반영(화면 잔류).
- **모달 만들기**(`ClickConfirmPopupCreate`, #224 / 🔁 #243): `groupNumber`가 없거나 이미 `isCreating`이면 무시.
  `isCreating = true` → `CreateGroupUseCase(GroupName, GroupNickname, memberLimit)` → 해제는 `finally`
  (어느 경로로 빠져나가도 팝업 버튼이 영구 비활성으로 남지 않는다) → **성공이면** 모달을 닫고 `NavigateToNext`.
  실행은 `launch(key = KEY_CREATE_GROUP, onError = ::onCreateGroupFailed)` — `onError`는 `Result.failure`가
  아니라 **던져진 예외**를 받으므로 두 실패 경로가 한 핸들러로 모인다.
- **실패 처리**(#243): `AppError.Network` / `AppError.Server`(코드별) / 그 외로 갈라 **로그만** 남기고
  모달을 유지한다. 서버 코드 분기는 `INVALID_GROUP_NAME`·`INVALID_GROUP_NICKNAME`·`INVALID_GROUP_MEMBER_LIMIT`
  (셋 다 클라 검증·선택 UI가 이미 막으므로 여기 오면 규칙이 어긋난 것) · `MEMBER_NOT_FOUND`(재로그인 필요,
  동선 미확정 `TODO`) · `Common.INVALID_REQUEST`(앱 버그).
  **`groupNickname`은 NavKey로 받은 값 그대로** 나간다 — 그 출처가 G-001 `GroupListUiState`의 mock 닉네임이다
  (아래 "주의").
- **모달 취소·dismiss**(`DismissConfirmPopup`, #224): `isCreating` 중이면 무시(생성 중 닫기 차단), 아니면 모달만 닫는다.
- **다음 화면**(`NavigateToNext`, #224): ~~`navigator.goToSingleClearTop(NavKeyGroupList)`~~
  → **as-built(#411 develop 머지, 2026-09-01)**: `replaceAll(NavKeyGroupList)` 뒤에 곧바로
  `goTo(NavKeyCanvasMain(groupId, welcomeGroupName, welcomeInviteCode))`다 — 만든 그룹의 캔버스로 바로
  들어가고 목록은 백스택 아래에 깔린다(캔버스만 남기면 뒤로가기가 앱 종료가 된다).
  이펙트도 `data object`에서 **`data class NavigateToNext(groupId, groupName, inviteCode)`**가 되어
  생성 응답(`CreatedGroupVO`)의 세 값을 실어 나른다 → [navigation-flow](../../../architecture/navigation-flow.md).
- **뒤로가기**(`ClickBackButton`) → `NavigateToBack` → `navigator.onBack()`.
- **확인 버튼 활성**: `isValid` — 그룹명·닉네임 비어있지 않고 인원이 선택됨. 상세 규칙은 클릭 시 UseCase가 검사.

### 유효성 규칙 (`CheckNameValidUseCase` 공용 — S-102와 동일 규칙)

닉네임과 **같은 UseCase**를 그룹명에도 적용한다. 위키 [[이름-입력-규칙]]이 그룹명·닉네임 공통 규칙이므로 정합.
표시 문자열만 그룹명용 리소스로 분기한다(`core:ui` `strings.xml`에 닉네임용/그룹명용 항목이 별도로 존재).

> 🔁 **as-built(#243, 2026-08-15) — 허용 문자 집합이 서버 정규식에 맞춰 좁혀졌다.** 기존 검사는
> `isLetter()`·`isDigit()`·`isWhitespace()` + `Char.isKorean()` 조합이라 **유니코드 전체**를 받았다 —
> 자모(`ㄱ`·`ㅏ`)·일본어·아랍 숫자·non-breaking space가 통과한 뒤 서버에서만 400으로 튕겼다.
> 지금은 `' '` · `가..힣` · `A..Z` · `a..z` · `0..9`만 통과하며, 코드가 서버 정규식
> `^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$`와 같은 집합으로 유지한다고 KDoc에 적는다.
> 쓰이지 않게 된 `core:util:jvm`의 `Char.isKorean()`은 같은 PR에서 **삭제**됐다(테스트 포함).
>
> 🔁 **재개정(#250, 2026-08-15) — 자모 범위가 다시 들어왔다.** 서버가 같은 날 정규식에 자모를 넣어
> (`^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$`) **이번엔 앱이 더 좁아졌기** 때문이다.
> 허용 집합은 `' '` · `가..힣` · **`ㄱ..ㅎ` · `ㅏ..ㅣ`** · `A..Z` · `a..z` · `0..9`이고, KDoc이
> **"서버보다 느슨하면 안 되고 좁아도 안 된다"**로 양방향 기준을 명시한다(좁으면 서버가 받는 이름을 앱이
> 먼저 막는다). `Char.isKorean()`은 되살리지 않았다 — 여전히 일본어·아랍 숫자·non-breaking space까지
> 받기 때문이다. `CheckNameValidUseCaseTest`의 자모 케이스는 `Success` 기대로 뒤집혔다.
> **위키 [[이름-입력-규칙]]은 그대로 "한글·영문·숫자·공백"이라 자모 단독의 허용 여부가 문서에 없다** —
> 지금 근거는 서버 커밋 메시지뿐이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].

| 반환 Error | 표시 문자열(그룹명 화면) |
|---|---|
| `Error.SpaceAtEdge` | "그룹명의 처음과 끝에는 공백을 사용할 수 없어요" |
| `Error.DuplicatedSpace` | "공백은 글자 사이에 1칸만 사용할 수 있어요" |
| `Error.InvalidCharacter` | "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요" |
| `Error.EmptyString` | "그룹명은 비워둘 수 없어요" |

- **길이 상한 10자**: `GroupCreateConfig.GROUP_NAME_MAX_LENGTH` → `YGTextFormField(maxLength = …)`로 입력 단계 강제(UseCase는 길이 미검사). 위키 [[A-005-그룹명-정책-v0.1]] "1~10자"와 일치.
- **인원 상한 12**: `GroupCreateConfig.GROUP_COUNT_LIST` = 1~12. 위키 [[그룹]] "최대 12명"과 일치.

## 표시·제어 규칙

- 상단 `YGTopBarDetail(title = R.string.group_create)`, 본문은 `LazyColumn`(좌우 `padding7`, 상 `padding6`, 하 `padding10`), 하단 고정 `YGButton(Large)`.
- 섹션 3개 — 각각 `YGLabel` + `gap4` 간격:
  1. **그룹명** — `YGTextFormField`(placeholder·`isError`·`errorDescription`·`maxLength`).
  2. **그룹 속 내 닉네임** — `YGTextFormField(enabled = false, onValueChange = no-op)`로 읽기 전용 표시(NavKey 인자값).
  3. **그룹 인원** — `VerticalGridLayout`(`core:ui`) + `YGInputNumber` 셀, 선택 상태는 `groupNumber == 셀 값`. 하단에 `caption.c01R`/`Gray300` 안내 문구("그룹명과 인원수는 추후 변경할 수 없어요").
- 정적 라벨은 `feature/groups/enter/impl` `res/values/strings.xml`(같은 모듈의 S-102·A-004 초대코드 화면과 파일 공용), 에러 문자열은 `core:ui` `strings.xml`.
- **확인 모달(#224)**: `isConfirmPopupVisible`일 때만 `YGModalPopup` 호출(컴포넌트가 표시 여부를 갖지 않는
  규약대로 — [ygmodalpopup 스펙](2026-07-15-ygmodalpopup.md)). 제목은 `%1$s`에 `groupName`을 끼운 포맷 문자열,
  본문은 "추후 변경 불가" 재확인 문구(화면 하단 안내와 같은 취지), 아이콘 `ic_warning_round`,
  좌 Secondary "취소" / 우 Primary "만들기", `isEnabledButton = isCreating.not()`(**두 버튼 공통** 비활성).
  프리뷰 파라미터에 모달 노출 케이스가 추가됐으나 Compose `Dialog`는 별도 window라 `@Preview`에 뜨지 않는다.
- 엔트리는 `YGScaffold(containerColor = Gray.White, contentWindowInsets = WindowInsets(0.dp))` + `statusBarsPadding()`·`navigationBarsAndImePadding()`.

> 🔁 **닉네임 필드·팝업·실패 안내는 2026-08-27(PR #393)에 바뀌었다.** 위 서술은 그 이전의 기록이다.
> - **닉네임 필드가 열렸다** — `enabled = false` + no-op `onValueChange`가 아니라 `InputNickName`
>   인텐트를 받고, 그룹명과 같은 `CheckNameValidUseCase` 검사를 거쳐 `nickNameError`를 필드 아래에
>   띄운다. 상한은 그대로 그룹명과 다른 상수다(`NICKNAME_MAX_LENGTH`). NavKey로 받는 값은 읽기 전용
>   표시값이 아니라 **초기값**이 됐고, 그래서 위 "서버로 나가는 닉네임이 mock이다" 경고가 가리키던
>   자리도 성격이 달라졌다 — 무엇이 넘어오든 사용자가 그 화면에서 고쳐 보낸다.
> - **확인은 두 이름을 함께 본다** — 그룹명만 검사하고 통과시키면 팝업을 연 뒤에 닉네임 에러가
>   뒤늦게 떠 사용자가 두 번 걸린다. 둘 다 통과할 때만 `isConfirmPopupVisible`이 켜진다.
> - **`isEnabledButton` 전달이 사라졌다** — 요청 직전에 팝업을 닫으므로 비활성으로 둘 버튼이 없다.
>   `isCreating` 중 dismiss를 막던 가드도 같은 이유로 걷혔고, 진행은 `YGScaffoldV2` 로딩 오버레이가
>   그린다(OQ-P-137 ④).
> - **생성 실패가 로그에서 토스트로 나왔다** — `GroupCreateError` 2종(`NETWORK`·`UNKNOWN`) +
>   `ShowError` 이펙트다. 서버 400 세 갈래를 나누지 않은 근거는 "사용자가 손쓸 수 없어 문구를 나눠도
>   할 일이 달라지지 않는다"이고, 이름 유효성 에러는 토스트를 타지 않고 필드 아래에 남는다
>   (OQ-P-167 ④).
> - **엔트리가 스캐폴드를 벗었다** — 로딩·토스트가 화면 상태를 봐야 해서 Route가 `YGScaffoldV2`를
>   쥔다(OQ-P-204). 텍스트 필드가 둘이 되어 `clearFocusOnTap`도 붙었다.

## 파일 구성

- `api/NavKeyGroupCreate.kt` — 인자 있는 목적지 키.
- `impl/groupcreate/GroupCreateViewModel.kt` — MVI + Assisted 주입, `CheckNameValidUseCase` 사용. 검증 결과를 도메인 의미 그대로 State에 담는다(#223 이후 `NameValidResult.Error?`, 그 전에는 `@StringRes` ID).
- `impl/groupcreate/GroupCreateScreen.kt` — stateless UI + `PreviewParameterProvider`(빈/기본/입력 3상태) `@YGPreview`.
- `impl/groupcreate/GroupCreateRoute.kt` — VM 배선, back→`onBack`, next stub.
- `impl/navigation/EntryBuilder.kt#featureGroupCreateEntryBuilder` — `entry<NavKeyGroupCreate>`에서 `hiltViewModel(creationCallback = { factory.create(navKey.nickName) })`로 VM 생성 후 Route 호출.
- `impl/navigation/NavigationModule.kt` — 빌더 `@IntoSet` 제공.
- `core/ui/VerticalGridLayout.kt` — 열 수·행/열 간격을 받는 공용 그리드(Column+Row+`IntrinsicSize.Max`, 빈 칸은 `Spacer(weight)`). 스크롤 컨테이너 안에서 쓰려고 `LazyVerticalGrid` 대신 비지연 레이아웃 채택.
- `domain/model/GroupCreateConfig.kt`·`domain/model/NameValidResult.kt`·`domain/usecase/CheckNameValidUseCase.kt` — 공용 도메인(S-102와 공유).
- 테스트(#243): `GroupCreateViewModelTest`(생성 성공·실패 분기·중복 요청 가드) · `CreateGroupUseCaseTest`(`groupId` 유효성 가드 포함) · `CheckNameValidUseCaseTest`(좁힌 문자 집합 — #250에서 자모 허용으로 케이스 개정).

## 주의 / 열린 질문

- ~~**진입 경로 없음**~~ — #222(G-001 그룹 추가 오버레이)로 뚫렸다. 넘어오는 `nickName`이 mock인 것은 잔존 → [open-questions](../../../synthesis/open-questions.md) [2026-07-29]·[2026-08-07].
- ~~**그룹 생성이 mock**~~ — ✅ **해소(#243)**. 실서버를 타고, G-001도 같은 라운드(#248)에서 조회가 붙어
  **복귀한 목록에 새 그룹이 뜰 자리는 생겼다**. ~~다만 복귀가 `goToSingleClearTop`이라 재조회가 돌지 않는다~~
  → #297의 `Enter` 인텐트로 닫혔고(OQ-P-169), #411부터는 목록 엔트리 자체가 새것이다.
- ⚠️ **서버로 나가는 닉네임이 mock이다** — `groupNickname`은 NavKey 인자를 그대로 쓰고, 그 값은 G-001
  `GroupListUiState.nickName` 기본값 리터럴이다. 즉 **실제로 만들어지는 그룹의 내 닉네임이 실사용자 값이 아니다**
  → [open-questions](../../../synthesis/open-questions.md) [2026-07-29]·[2026-08-15].
- **실패가 로그뿐이다** — 갈래는 전부 열거됐지만 화면 표현이 없어 모달이 열린 채 멈춘다. 실패 토스트가
  같은 PR에서 들어왔다 걷힌 이유는 "문구 정책이 없다"이다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-15].
- **생성 중 표시가 없다** — `isCreating`은 모달 버튼 비활성에만 쓰이고 진행 표시(스피너 등)는 없다. 요청이 도는 동안 화면이 멈춘 것처럼 보인다.
- ~~**복귀 목적지가 위키 정본과 다름**~~ — ✅ **해소(#411, 2026-09-01)**. [[기능정의서-v6]]이 적어 둔
  **C-001(메인 캔버스) 직접 진입**으로 옮겨 갔다(OQ-P-135). 목록을 백스택 아래에 남기는 것만 정본에 없는
  구현 사정이다.
- ⚠️ **환영 배너의 문구·초대코드 복사 동작에 정책 소스가 없다** — 생성 직후 캔버스가 띄우는
  "%s 그룹을 만들었어요 / 초대코드는 %s이에요" 배너와 복사 버튼은 위키에 대응 문서가 없고 코드가 확정했다
  → [open-questions](../../../synthesis/open-questions.md) [2026-09-01].
- **`GroupCreateConfig`가 표시 관심사를 포함** — `GROUP_COLUMN_COUNT`(그리드 열 수)는 UI 레이아웃 값인데 `domain`에 있다. → [open-questions](../../../synthesis/open-questions.md) [2026-07-29].
- **`VerticalGridLayout` 프리뷰가 규약 이탈** — `@Preview` + public 프리뷰 함수 + 랜덤 색. `core:ui`는 디자인시스템 프리뷰 규약(`@YGPreview`+`PreviewBox`) 적용 대상이 아니었으나, 공용 UI 컴포넌트가 늘면 규약 범위를 정해야 한다. → [open-questions](../../../synthesis/open-questions.md) [2026-07-29].
- **읽기 전용 필드 관용구** — 닉네임 표시에 `YGTextFormField(enabled = false)` + no-op `onValueChange`를 쓴다. 표시 전용 컴포넌트가 없어 입력 컴포넌트를 비활성으로 전용한 형태.
