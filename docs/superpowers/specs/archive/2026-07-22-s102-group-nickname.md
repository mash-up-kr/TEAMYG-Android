---
id: s102-group-nickname
title: S-102 그룹 내 닉네임 입력 화면 (GroupNickName)
status: implemented
category: ui-spec
platforms: android
verified: 2026-09-07
related_code:
  - NavKeyGroupNickName
  - GroupNickNameRoute.kt#GroupNickNameRoute
  - GroupNickNameScreen.kt#GroupNickNameScreen
  - GroupNickNameViewModel.kt#GroupNickNameViewModel
  - GroupNickNameViewModelTest
  - GroupNickNameError.kt#GroupNickNameError
  - CheckNameValidUseCase.kt#CheckNameValidUseCase
  - ChangeGroupNicknameUseCase.kt#ChangeGroupNicknameUseCase
  - JoinGroupUseCase.kt#JoinGroupUseCase
  - ParfaitGroupRepository.kt#changeMyNickname
  - ParfaitGroupRepository.kt#joinGroup
  - InviteCode.kt#InviteCode
  - YGModalPopup.kt#YGModalPopup
  - ServerErrorCode.kt#ParfaitGroup
  - Navigator.kt#goToSingleClearTop
  - NameValidResult.kt#NameValidResult
  - GroupCreateConfig.kt#GroupCreateConfig
  - core/ui/res/values/strings.xml
  - EntryBuilder.kt#featureGroupNickNameEntryBuilder
  - feature/groups/enter/impl/res/values/strings.xml
related_adr: ADR-0005, ADR-0006, ADR-0009, ADR-0016
related_spec:
related_architecture: state-management, navigation-flow
related_spec: s002-account-info
supersedes:
superseded_by:
tags: [spec, parfait, groups, nickname, s102]
---

# Spec: S-102 그룹 내 닉네임 입력 화면 (GroupNickName)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> **사후 기록(post-hoc)**: 타 작업자 구현이 선작성 스펙 없이 develop 머지(#154, 2026-07-22)됨.
> as-built 역기록. 코드가 SoT. 입력 유효성은 위키 정책 [[S-102-닉네임-정책-v0.1]]·[[이름-입력-규칙]]과 대조 완료(일치).
>
> **as-built 갱신(2026-07-26, #166)**: 화면 정적 문자열이 하드코딩 → `strings.xml` + `stringResource`로 이동. 문구 자체는 불변.
>
> **as-built 갱신(2026-07-29, #179)**: 유효성 결과가 [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md) 방향으로 리팩터됐으나
> **ADR 설계와 형태가 다르다**(아래 "유효성 규칙" 참고). `NickNameResult(isSuccess, errorMessage: String?)` →
> `NameValidResult` sealed(`Success`/`Error.{EmptyString, SpaceAtEdge, DuplicatedSpace, InvalidCharacter}`),
> UseCase 패키지 `domain.usecase.group` → `domain.usecase`, 길이 상한이 Screen 상수 → `domain` `GroupCreateConfig.NICKNAME_MAX_LENGTH`,
> UiState `errorMessage: String?` → `errorMessageResId: Int?`(ViewModel이 `core:ui` `strings.xml` 리소스 ID로 매핑).
> ADR-0016이 설계한 `core:ui` `toStringResource()` 확장은 **머지되지 않았다** → [open-questions](../../../synthesis/open-questions.md) [2026-07-29].
>
> **as-built 갱신(2026-08-13, #223)**: 위 갈림이 **원안으로 수렴하며 닫혔다**. `errorMessageResId: Int?` →
> **`nicknameError: NameValidResult.Error?`**, VM의 `when` 5분기 → `is NameValidResult.Error` 2분기,
> 표시 변환은 화면이 `nicknameError?.toStringResource(NameFieldType.NICKNAME)`로 한다. 화면 문구·동작 불변
> ([S-101 라운드](2026-08-07-s101-group-side-menu.md)가 4개 화면을 동시에 전환).
>
> **as-built 갱신(2026-08-12, #224)**: 확인이 `EnterGroupUseCase`(mock)를 거쳐 **G-001 그룹 목록으로 복귀**하는
> 데까지 결선됐다. 스펙이 유일한 미구현으로 뒀던 "다음 화면 네비게이션"이 닫혔다 — 다만 목적지는
> 당시 후보였던 A-005(그룹 생성)가 아니라 그룹 목록이다. **그룹 참여 API는 여전히 미연동.**
>
> ⚠️ **as-built 갱신(2026-08-15, #244 develop 머지)**: **화면의 역할이 바뀌었다.** 합류가 앞 화면(A-004)의
> `POST join`으로 옮겨가면서, 여기는 **이미 들어간 그룹의 닉네임을 바꾸는 화면**이 됐다 —
> mock `EnterGroupUseCase`가 삭제되고 `ChangeGroupNicknameUseCase`(PATCH `/{groupId}/nickname`)가 들어왔다.
> 그래서 `NavKeyGroupNickName`이 **`data object` → `data class(groupId: Long)`**로 바뀌고 VM은 Assisted 주입이 된다.
> 서버 실패 사유는 `GroupNickNameError` enum + 화면 매핑으로 입력 자리 아래에 나간다.
>
> ⚠️ **as-built 갱신(2026-08-16, #261 develop 머지)**: **역할이 한 번 더 바뀌어 이 화면이 참여를 끝낸다.**
> A-004의 확인 모달이 통째로 여기로 옮겨왔고, 모달의 "참여하기"가 `POST join` → `PATCH nickname`을
> **순서대로** 부른다. 그래서 앞 화면에서 받는 것이 `groupId`가 아니라 **참여에 쓸 초대코드와 모달에 띄울
> 그룹명**이고(`NavKeyGroupNickName(inviteCode, groupName)`), 화면을 이탈하면 참여 자체가 일어나지 않는다
> (OQ-P-166 해소). **닉네임 적용 실패는 참여를 되돌리지 않는다** — 로그만 남기고 전역 닉네임을 쓴 채
> 다음 화면으로 간다(코드에 안내 토스트 `TODO`). 반대로 참여 실패는 닉네임을 보내지 않고 모달을 닫은 뒤
> 사유를 입력 자리에 붙인다.

> ⚠️ **as-built 갱신(2026-09-07, #461 develop 머지)**: **입력칸이 앱 닉네임으로 채워진 채 선다.**
> `NavKeyGroupNickName`에 세 번째 인자 `nickName`이 붙고 A-004가 계정 SSoT 구독값을 실어 보내며,
> 이 화면은 그것을 `@Assisted`로 받아 초기 상태에 넣는다. 그전까지 이 화면만 빈 칸으로 시작해
> **위키 [[S-102-그룹-닉네임-생성-정책-v0.1]]의 "계정 공통 1개 값 재사용"과 어긋나 있었다**(생성
> 갈래 A-005는 #312부터 채워져 있었다). 사용자가 지우고 다시 쓰는 것은 그대로다 — 초기값일 뿐
> 읽기 전용이 아니다. 앞 화면이 값을 구하지 못했으면 빈 문자열이 오고 화면은 예전처럼 빈 칸으로
> 선다 → OQ-P-377.

- **화면 ID**: S-102 (그룹 참여 시 그룹 내 닉네임)
- **대상 모듈**: `feature/groups/enter/impl`(`nickname/`) + `feature/groups/enter/api`(NavKey) + `domain`(UseCase/model) + `core:designsystem`(`YGModalPopup`, #261에서 이관)

## 목표

그룹 참여 플로우에서 "그룹이름에서만 공유되는" 닉네임을 입력받는 화면. 확인 시 유효성 검사를 돌려
통과해야 다음 단계로 진행한다. #244에서는 **참여를 마친 그룹의 닉네임을 적용**하는 단계였고,
🔁 **#261부터는 참여 확인 모달을 띄우고 참여와 닉네임 적용을 함께 끝내는 화면**이다.

## 범위

- 포함: 닉네임 입력 폼(최대 15자)·확인 시 유효성 검사·에러 메시지 인라인 노출·입력 시 에러 초기화·진입 시 자동 포커스·뒤로가기.
  **#224 추가**: 유효성 통과 후 호출·진행 중 재진입 가드·완료 후 그룹 목록 복귀.
  **#244 추가**: 그룹 닉네임 변경 API 호출·서버 실패 사유 인라인 노출·진행 중 확인 버튼 비활성.
  **#261 추가**: 참여 확인 모달(A-004에서 이관)·참여 요청·참여 실패 사유 인라인 노출·진행 중 모달 dismiss 차단.
- 제외(구현 TODO):
  - ~~**그룹 참여 API 연동**~~ — ✅ **해소(#244·#261)**. #244에선 닉네임 변경(PATCH)만 불렀고,
    🔁 **#261부터 참여(POST join)도 이 화면이 부른다.**
  - ~~다음 화면 네비게이션~~ — #224에서 `goToSingleClearTop(NavKeyGroupList)`로 결선.
  - ~~**건너뛰기·이탈 경로 없음**~~ — 🔁 **#261에서 성격이 바뀌었다**. 이탈해도 참여가 일어나지 않으므로
    "닉네임 없는 참여"가 남지 않는다. 대신 **닉네임 입력이 참여의 필수 관문**이 됐다(건너뛰기는 여전히 없다).
  - **닉네임 적용 실패 안내 없음** — 참여만 되고 닉네임 PATCH가 실패하면 로그만 남고 전역 닉네임이 쓰인다.
    코드에 안내 토스트 `TODO`가 있다.

## API / 인터페이스

```kotlin
// api — 🔁 #244: data object → 인자 있는 NavKey / 🔁 #261: 참여 결과가 아니라 참여에 쓸 재료를 받는다
@Serializable data class NavKeyGroupNickName(val inviteCode: String, val groupName: String) : NavKey

// domain — UseCase(ADR-0009: @Inject + operator invoke). 패키지 domain.usecase (#179에서 .group 제거)
class CheckNameValidUseCase @Inject constructor() {
    operator fun invoke(name: String): NameValidResult
}
// 🔁 as-built(#179): NameValidResult sealed, 문자열 미보유. 표시 문자열은 ViewModel이
//    core:ui strings.xml 리소스 ID로 매핑.
// 🔁 as-built(#223, 2026-08-13): 매핑이 core:ui text/NameValidResultUiText.kt의
//    NameValidResult.Error.toStringResource(NameFieldType)로 이관 — State는 Error?를 그대로 든다.
sealed interface NameValidResult {
    data object Success : NameValidResult
    sealed interface Error : NameValidResult { /* EmptyString, SpaceAtEdge, DuplicatedSpace, InvalidCharacter */ }
}

// domain — 🔁 #244: mock EnterGroupUseCase 삭제, 그룹 닉네임 변경으로 교체 / 🔁 #261: JoinGroupUseCase 합류
class JoinGroupUseCase @Inject constructor(private val parfaitGroupRepository: ParfaitGroupRepository) {
    suspend operator fun invoke(inviteCode: InviteCode): Result<JoinedGroupVO>   // POST join (#261 이관)
}
class ChangeGroupNicknameUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(groupId: GroupId, groupNickname: GroupNickname): Result<GroupNicknameVO>
}

// impl — MVI (🔁 #179 errorMessageResId, #223 nicknameError, #224 isEntering, #244 submitError, #261 모달)
data class GroupNickNameUiState(
    val groupName: String = "",                        // #261 — NavKey 인자, 모달 제목에만 쓴다
    val nickName: String = "",                         // 🔁 #461 — NavKey 인자(앱 닉네임)로 채워진다
    val nicknameError: NameValidResult.Error? = null,   // 입력 형식(로컬 검증)
    val submitError: GroupNickNameError? = null,        // 서버만 알 수 있는 사유(#244)
    val isConfirmPopupVisible: Boolean = false,         // #261 — A-004에서 이관
    val isEntering: Boolean = false,
) : UiState

// 서버 실패 사유 — feature 로컬 enum + 화면 매핑(A-004의 InviteCodeError와 같은 형태)
// 🔁 #261: 닉네임 400 갈래(INVALID)가 빠지고 참여 실패 3종이 들어왔다 — 이제 이 화면의 실패는 참여 실패다
enum class GroupNickNameError { INVALID_INVITE_CODE, ALREADY_JOINED, MEMBER_LIMIT_REACHED, NETWORK, UNKNOWN }
@Composable internal fun GroupNickNameError.toStringResource(): String

sealed interface GroupNickNameIntent {
    data object ClickNextButton; data object ClickBackButton
    data class InputWord(val nickName: String)
    data object ClickConfirmPopupEnter; data object DismissConfirmPopup   // #261 — A-004에서 이관
}
sealed interface GroupNickNameSideEffect { data object NavigateToBack; data object NavigateToNext }

// ViewModel — NavKey 인자를 받으므로 Assisted 주입(#244 / 🔁 #261 인자 교체·이름 있는 @Assisted / 🔁 #461 셋으로 늘었다)
@HiltViewModel(assistedFactory = GroupNickNameViewModel.Factory::class)
class GroupNickNameViewModel @AssistedInject constructor(
    @Assisted(ASSISTED_INVITE_CODE) inviteCodeValue: String,
    @Assisted(ASSISTED_GROUP_NAME) groupName: String,
    @Assisted(ASSISTED_NICK_NAME) nickName: String,   // #461 — 입력칸 초기값(앱 닉네임)
    private val checkNickNameValid: CheckNameValidUseCase,
    private val joinGroup: JoinGroupUseCase,              // #261
    private val changeGroupNickname: ChangeGroupNicknameUseCase,
) : BaseViewModel<…>
```

- 엔트리 빌더가 `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(navKey.inviteCode, navKey.groupName) })`로
  VM을 만들어 Route에 **파라미터로 넘긴다**(#244, 🔁 #261 인자 둘) — Route의 기본값 `hiltViewModel()`이
  제거돼 이제 VM 없이 호출할 수 없다. 인자가 둘 다 `String`이라 이름 있는 `@Assisted` 한정자를 쓴다.

## 동작 / 상태

- **입력**(`InputWord`): `nickName` 갱신 + `nicknameError`·`submitError` 모두 `null`(입력 시 에러 즉시 해제).
- **확인**(`ClickNextButton`, 🔁 #261): 먼저 `CheckNameValidUseCase(nickName)`로 형식을 보고, `Error`면 그대로
  표시하고 **모달까지 가지 않는다**. 통과하면 `isConfirmPopupVisible = true` — 여기서는 아직 서버를 부르지 않는다
  (#244까지는 이 시점이 곧 PATCH였다).
- **모달 참여**(`ClickConfirmPopupEnter`, #261): `launch(key = KEY_ENTER_GROUP)`에서 `submitError`를 지우고
  `isEntering = true` → `JoinGroupUseCase(inviteCode)` → 성공한 `groupId`로
  `ChangeGroupNicknameUseCase(groupId, GroupNickname(nickName))` → 모달을 닫고 `NavigateToNext`.
  해제는 `finally`(버튼이 영구 비활성으로 남지 않는다), 중복 요청은 job 키 가드가 막는다.
  - **닉네임 적용 실패는 흐름을 멈추지 않는다** — 참여가 이미 끝났으므로 로그만 남기고 전역 닉네임을 쓴 채
    다음 화면으로 간다(코드에 안내 토스트 `TODO`).
  - **참여 실패는 닉네임을 보내지 않는다** — 모달을 닫고 사유만 붙인다.
- **참여 실패 매핑**(🔁 #261 교체): `AppError.Network` → `NETWORK` / `AppError.Server`의
  `INVALID_INVITE_CODE`·`GROUP_ALREADY_JOINED`·`GROUP_MEMBER_LIMIT_REACHED` → 각 사유 / 그 외 `UNKNOWN`.
  결과는 `submitError`에 담기고 입력 필드 아래 한 줄로 나간다. **문구는 A-004의 `invite_code_error_*`를
  그대로 재사용**하고(`group_nickname_error_invalid`는 삭제), 남은 자기 문구는 `NETWORK`·`UNKNOWN` 둘뿐이다.
  갈래가 이렇게 생긴 이유는 **앞 화면 미리보기를 통과한 뒤에야 도달**하기 때문이다 — 미리보기와 참여 사이에
  그룹 상태가 바뀐 경우만 남는다(정원이 차거나 그룹이 사라지거나 이미 참여됨).

> 🔁 **위 세 절은 2026-08-27(PR #394) 이전의 기록이다.** 실패를 어디에 보여 주는지가 바뀌었다.
> - **`submitError`가 State에서 빠졌다** — 서버 사유는 `GroupNickNameSideEffect.ShowError(error)`로
>   나가고 공통 토스트가 받는다. 입력칸 아래(`errorDescription`)는 **형식 오류 전용**이 됐으므로,
>   위 "형식 오류가 우선하고 없으면 서버 사유"라는 규칙도 함께 사라졌다. 갈래 매핑 자체는 그대로다.
> - **팝업을 요청 직전에 닫는다** — "모달을 닫고 사유만 붙인다"가 아니라, 부르기 전에 이미 닫혀 있다.
>   진행 중임은 `isEntering` 하나가 말하고 `YGScaffoldV2` 로딩 오버레이가 그린다. 그래서
>   `isEntering` 중 dismiss를 막던 가드도 걷혔다 — 막을 팝업이 없다.
> - **엔트리가 아니라 Route가 스캐폴드를 쥔다** — 토스트 호스트가 필요해 `YGScaffold`에서
>   `YGScaffoldV2`로 옮겼다(OQ-P-204).
> - **예외로 튄 경로도 알린다** — `launch(key = KEY_ENTER_GROUP, onError = …)`가 붙어,
>   `Result.failure`가 아니라 던져진 예외로 끝나던 경로가 `UNKNOWN` 토스트를 낸다.
> - **닉네임 적용 실패 `TODO`가 닫혔다** — `NICKNAME_NOT_APPLIED` 갈래가 생겼다. 흐름을 멈추지
>   않는다는 규칙은 그대로이고, 다만 **안내가 사라질 때까지 기다렸다가** 이동한다(토스트 호스트가
>   이 화면에 매여 있다). 기다리는 시간이 토스트 정책과 별개 상수라는 점은 OQ-P-328이 쥔다.
> - 텍스트 필드 밖을 탭하면 포커스가 풀린다(`clearFocusOnTap`).
  🔁 **#244·#250의 닉네임 400 갈래(`INVALID`)는 사라졌다** — 닉네임 실패가 더는 화면에 표시되지 않으므로
  `ServerErrorCode.INVALID_GROUP_NICKNAME`을 보는 분기가 이 화면에서 없어졌다(상수는 A-005가 계속 쓴다).
- **모달 취소·바깥 탭**(`DismissConfirmPopup`, #261): 진행 중(`isEntering`)이면 무시, 아니면 모달만 닫는다
  (A-005 `isCreating`·구 A-004 `isSubmitting` 가드와 같은 형태).
- **다음 화면**(`NavigateToNext`, #224): ~~`navigator.goToSingleClearTop(NavKeyGroupList)`~~
  → **as-built(#411 develop 머지, 2026-09-01)**: `replaceAll(NavKeyGroupList)` 뒤에
  `goTo(NavKeyCanvasMain(groupId, welcomeGroupName))`다(참여는 초대코드를 싣지 않아 배너 갈래가 갈린다).
  이펙트도 **`data class NavigateToNext(groupId, groupName)`**가 되어 참여 응답의 두 값을 나른다
  → [navigation-flow](../../../architecture/navigation-flow.md).
  의존은 규약대로 `:api`만(`feature/groups/enter/impl` → `feature/groups/list/api`, #224에서 추가.
  **#411에서 `feature/groups/canvas/api`가 하나 더 붙었다**).
- **뒤로가기**(`ClickBackButton`) → `NavigateToBack` → `navigator.onBack()`.
- **자동 포커스**: 화면 진입 시 `FocusRequester.requestFocus()`(`LaunchedEffect(Unit)`).
- **확인 버튼 활성**: `nickName.isNotEmpty() && isEntering.not()`(🔁 #244 — 진행 중 비활성이 붙었다).
  빈 값만 막고 상세 규칙은 클릭 시 UseCase가 검사한다.
  🔁 **#461부터 초기값이 채워져 있으면 진입 즉시 활성이다** — 앞 화면이 앱 닉네임을 실어 보내므로
  사용자가 아무것도 치지 않고 확인을 누를 수 있다. 초기값은 검사 대상이 아니다(위키
  [[이름-입력-규칙]]도 같은 방향이고, 자동 생성 닉네임은 조합 최대 11자라 상한 15를 넘지 않는다).
- **모달 표시**(#261): `uiState.isConfirmPopupVisible`일 때만 `YGModalPopup` 호출(표시 여부는 호출자 소관 —
  [ygmodalpopup 스펙](2026-07-15-ygmodalpopup.md)). 제목은 `%1$s`에 `groupName`을 끼운 `group_enter_confirm_title`,
  아이콘 `ic_warning_round`, 좌 Secondary "취소" / 우 Primary "참여하기" — **A-004에서 쓰던 문구·배치 그대로**
  옮겨왔다. `isEnabledButton`은 주지 않는다(기본 `true`).

### 유효성 규칙 (`CheckNameValidUseCase`, 순차 검사 — 첫 실패 반환)

> 🔁 **as-built(#179)**: 각 규칙은 문자열 대신 `NameValidResult.Error` 변형을 반환하고, 표시 문자열은 `core:ui` `strings.xml`이 소유한다
> (에러 문자열 리소스는 닉네임용·그룹명용이 별도 항목으로 공존). 매핑 주체는 #179 시점엔 **ViewModel**이었고 **#223(2026-08-13)에 `core:ui` 확장으로 이관**됐다.
> 빈 값 규칙 `CheckEmptyString`(→`Error.EmptyString`)이 추가됐으나 **enum 순서상 마지막**이다 — S-002 스펙은 선두를 전제했으나,
> 빈 문자열은 앞 3규칙을 공백으로 통과하므로 결과는 동일하다. S-102는 확인 버튼 `isNotEmpty()` 비활성으로 런타임 미도달.

| 규칙(enum) | 조건 | 반환 Error / 표시 문자열(닉네임 화면) |
|---|---|---|
| `CheckSpaceStartOrEnd` | 처음/끝 공백 불가 | `Error.SpaceAtEdge` — "닉네임의 처음과 끝에는 공백을 사용할 수 없어요" |
| `CheckDuplicatedSpace` | 연속 공백(`"  "`) 불가 | `Error.DuplicatedSpace` — "공백은 글자 사이에 1칸만 사용할 수 있어요" |
| `CheckValidCharacter` | 완성형 한글/영문/숫자/스페이스만(🔁 #244) | `Error.InvalidCharacter` — "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요" |
| `CheckEmptyString` | 빈 값 불가 | `Error.EmptyString` — "닉네임은 비워둘 수 없어요" |

- **길이 상한 15자**: `domain` `GroupCreateConfig.NICKNAME_MAX_LENGTH` → `YGTextFormField(maxLength = …)`로 입력 단계에서 강제(UseCase는 길이 미검사). 위키 [[S-102-닉네임-정책-v0.1]] "1~15자"와 일치. #179에서 Screen 지역 상수 → domain 설정 객체로 이동(A-005 그룹 생성 화면과 공용).
- 🔁 **문자 집합이 서버 정규식에 맞춰 좁혀졌다(#244 라운드의 #243 커밋)** — 구 검사는 `isLetter`·`isDigit`·
  `isWhitespace` + `Char.isKorean()`이라 자모·타 언어·non-breaking space까지 통과했고 서버에서만 400이 났다.
  지금은 `' '`·`가..힣`·`A..Z`·`a..z`·`0..9`뿐이며 `Char.isKorean()`은 **삭제**됐다(`core:util:jvm`).
  🔁 **#250에서 자모 범위가 다시 들어왔다** — 서버 정규식이 `ㄱ-ㅎ`·`ㅏ-ㅣ`를 얻어 **이번엔 앱이 더 좁아졌기**
  때문이다. 허용 집합은 `' '`·`가..힣`·**`ㄱ..ㅎ`·`ㅏ..ㅣ`**·`A..Z`·`a..z`·`0..9`이고, KDoc이 "서버보다
  느슨해도 좁아도 안 된다"로 양방향 기준을 명시한다. 상세·정책 공백은
  [a005 스펙](2026-07-29-a005-group-create.md) 유효성 절.
- 🔁 **중복 닉네임은 이제 막지 않는다**(#250) — 서버가 `existsByGroupIdAndNickname` 검사와
  `GROUP_NICKNAME_ALREADY_USED`를 포트·어댑터·에러 코드까지 통째로 삭제해 **같은 그룹 안 닉네임 중복이
  허용**된다(사유: "정책상 허용"). #244 시점의 "서버 계약이 먼저 답을 냈다(불가·409)"는 **뒤집혔고**,
  위키 [[이름-입력-규칙]]의 "그룹 내 닉네임 중복 처리" 미결은 근거가 서버 커밋 메시지뿐인 채로 남는다.
  같은 그룹에 같은 표시 이름이 여럿일 때의 구분 수단도 정해지지 않았다
  → [open-questions](../../../synthesis/open-questions.md).

## 표시·제어 규칙

- 상단 `YGTopBarDetail(title=R.string.group_enter, "그룹 참여하기")`, 제목/부제 텍스트, `YGTextFormField`(placeholder·isError·errorDescription·maxLength), 하단 `YGButton` `Large`.
- 에러 상태는 `uiState.nicknameError != null || uiState.submitError != null` → `isError` + 하단 `errorDescription`
  (🔁 #244. 형식 오류가 우선하고 없으면 서버 사유를 띄운다). #223 as-built 기준으로 형식 오류는
  `NameValidResult.Error`, #179~#223 사이엔 `errorMessageResId`, 그 전엔 `errorMessage: String?`였다.
- **정적 UI 라벨은 `feature/groups/enter/impl` `res/values/strings.xml` + `stringResource(R.string.*)`**(상단 타이틀·제목·부제·placeholder·확인 버튼, #166). 같은 모듈의 A-004 초대코드 화면([a004 스펙](2026-08-12-a004-group-invite-code.md))과 문자열 파일 공용(`submit`·`group_enter` 공유). 에러 문자열은 별개 경로 — `core:ui` `toStringResource` 매핑(ADR-0016).

## 파일 구성

- `api/NavKeyGroupNickName.kt` — 목적지 키.
- `domain/usecase/CheckNameValidUseCase.kt` + `domain/model/NameValidResult.kt` — 유효성 도메인 로직(🔁 #179: sealed·문자열 미보유, 패키지에서 `group` 제거).
- `domain/model/GroupCreateConfig.kt` — 이름 길이 상한 등 그룹 생성/참여 공용 상수(🔁 #179 신규).
- `core/ui/res/values/strings.xml` — 유효성 에러 문자열(닉네임/그룹명 각각). ViewModel이 리소스 ID로 참조(🔁 #179 신규, S-002·A-005와 공용).
- ~~`core/util/jvm/extension/CharExtension.kt#isKorean`~~ — **#244 라운드에서 삭제**(테스트 포함).
- `impl/nickname/GroupNickNameError.kt` — 서버 실패 사유 enum + `toStringResource()`(#244 신설, 🔁 #261 갈래 교체).
- `domain/usecase/group/ChangeGroupNicknameUseCase.kt`(#244 신설) · `JoinGroupUseCase.kt`(#261에 소비처 이관).
  삭제: `EnterGroupUseCase.kt`.
- 테스트(#244 / 🔁 #261): `GroupNickNameViewModelTest` — 모달 게이트·참여 성공·참여 실패 매핑·닉네임 실패에도
  진행·연타 가드·진행 중 dismiss 차단까지 늘었다(A-004에서 넘어온 참여 케이스 포함).
- `impl/nickname/GroupNickNameScreen.kt` — stateless UI + 확인 모달(#261 이관, 길이 상한은 `GroupCreateConfig` 참조).
- `impl/res/values/strings.xml` — 그룹 참여 플로우(S-102 + A-004 초대코드) 공용 정적 라벨. #166 신설, #224에서 확인 모달 문구 추가.
- `impl/nickname/GroupNickNameRoute.kt` — VM 배선, back→onBack, next stub.
- `impl/nickname/GroupNickNameViewModel.kt` — MVI, `CheckNameValidUseCase` 주입.
- `impl/navigation/EntryBuilder.kt#featureGroupNickNameEntryBuilder` — `entry<NavKeyGroupNickName> { YGScaffold(contentWindowInsets = WindowInsets(0.dp)) { GroupNickNameRoute(...) } }`(ime 패딩 직접 처리).

## 주의 / 열린 질문

- ~~**다음 화면 네비게이션 미구현**~~ — #224에서 결선됐고, 목적지는 A-005가 **아니라** G-001 그룹 목록이었다
  (#411부터는 참여한 그룹의 C-001 캔버스다).
  즉 "참여 다음이 생성"이라는 당시 후보 흐름은 성립하지 않고, A-005 진입은 목록 오버레이 하나뿐이다
  → [open-questions](../../../synthesis/open-questions.md) [2026-07-29].
- ~~**참여가 mock**~~ — ✅ **해소(#244·#261)**. #261부터 이 화면이 `POST join`을 직접 부른다.
- ~~⚠️ **합류와 닉네임이 두 요청으로 갈렸다**(#244)~~ — ✅ **해소(#261)**. 두 요청은 그대로지만 **한 확인 뒤에
  연달아** 나가고, 이탈하면 참여 자체가 없다 → [open-questions](../../../synthesis/open-questions.md) [2026-08-15] OQ-P-166.
  **남은 틈은 반대쪽이다** — 참여는 됐는데 닉네임 PATCH만 실패하면 사용자에게 아무 표시 없이 전역 닉네임으로
  들어간다(코드 `TODO`). 두 요청이 원자적이지 않다는 사실 자체는 그대로다.
- ~~**참여 중 표시가 없다**~~ — **부분 해소(#244)**. 확인 버튼이 진행 중 비활성이 됐고 #261에서 모달 dismiss도
  잠긴다. 진행 표시(스피너 등)는 여전히 없다 — 참여+닉네임 두 왕복이라 대기 구간이 더 길어졌다.
- **참여 실패 문구가 닉네임 입력 자리에 붙는다**(#261) — "이미 참여하고 있는 그룹이에요" 같은 초대코드 사유가
  닉네임 필드 아래에 뜨고 필드도 에러 상태가 된다. 사유의 원인(초대코드)과 표시 위치(닉네임)가 어긋난다.
- ~~**복귀 목적지가 위키 정본과 다름**~~ — ✅ **해소(#411, 2026-09-01)**. [[기능정의서-v6]]이 적어 둔
  **C-001(메인 캔버스) 직접 진입**으로 옮겨 갔다(OQ-P-135). 참여 직후 배너는 그룹명만 실린 갈래다.
  문구에 정책 소스가 없다는 점은 [open-questions](../../../synthesis/open-questions.md) [2026-09-01]가 쥔다.
- 유효성 규칙(공백·문자 종류)은 UseCase, 길이(15)는 `GroupCreateConfig` 상수로 **검사 위치 이원화** — 정책 [[이름-입력-규칙]] 상한과 정합하고 #179로 상수 소유처는 domain 단일화됐으나 검사 자체는 여전히 입력 컴포넌트 소관.
