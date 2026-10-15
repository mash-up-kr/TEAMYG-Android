---
id: s101-group-side-menu
title: 그룹 사이드 메뉴 화면 (S-101 GroupSetting) + S-102 닉네임 편집 모드
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-13
related_code: NavKeyGroupSetting.kt, GroupSettingRoute.kt#GroupSettingRoute, GroupSettingScreen.kt#GroupSettingScreen, GroupSettingViewModel.kt#GroupSettingViewModel, GroupSettingViewModel.kt#GroupSettingUiState, GroupMemberUiModel.kt#GroupMemberUiModel, GroupSettingViewModelTest.kt, GroupNicknameField.kt#GroupNicknameField, GroupMemberList.kt#GroupMemberList, EntryBuilder.kt#featureGroupSettingEntryBuilder, YGColorChipType.kt, YGNametagChipPreviewData.kt, YGTextFieldImpl.kt, YGTextFormField.kt#YGTextFormField, NameValidResultUiText.kt#toStringResource, NameValidResultUiText.kt#NameFieldType, YGTopBar.kt#YGTopBarDetail, YGUserChip.kt#YGUserChip, YGInviteCard.kt#YGInviteCard, YGDangerZone.kt#YGDangerZone, YGActionItem.kt#YGActionItem, CheckNameValidUseCase.kt
related_adr: ADR-0005, ADR-0006, ADR-0010, ADR-0016
related_spec: app-setting-s001, s002-account-info, s102-group-nickname, designsystem-grouptag-topping-components, clearfocusontap-modifier
related_architecture: design-system, navigation-flow, state-management, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, feature, groups, setting, navigation]
---

# Spec: 그룹 사이드 메뉴 화면 (S-101) + S-102 닉네임 편집 모드

- 대상: `:feature:groups:setting:api` · `:feature:groups:setting:impl` (+ `core:designsystem` `ygcolorchip` 드리프트 수정)
- 이슈: `mash-up-kr/TEAMYG-Android` #211, 브랜치 `feature/#211-S-101-group-side-menu`
- Figma(파르페 v0.1): `S-101` 기본 `144:7450` · 복사됨 `430:977` · `S-102` 편집 유효 `220:2662` · 편집 오류 `144:8245` · `Nametag-Chip` 컴포넌트셋 `144:5415`
- 위키 화면 정의: `S-` = Sidebar, `S-101`(그룹 메뉴) — [[화면-ID-체계]] / 닉네임 유효성 [[S-102-닉네임-정책-v0.2]] / 컬러칩 [[nametag-chip]]

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> ✅ **결선됐다(2026-08-17, PR #285·#287)** — 이 스펙이 "이번 범위 밖"으로 미뤄 둔 것 대부분이 닫혔다.
> mock 기본값 5종이 사라지고 상세 조회·닉네임 변경·나가기·신고가 실제 요청을 보내며, C-001 상단
> 메뉴가 호출자가 돼 **도달 불가도 닫혔다**(`NavKeyGroupSetting`이 `data class(groupId)`). 화면
> 컨테이너도 `YGScaffold`(엔트리) → `YGScaffoldV2`(Route)로 이관됐다. 아래 본문의 mock 상수·
> `// TODO: API 연동` 서술은 **당시 기록**이다. 남은 것은 `remainingCount`(계약에 `memberLimit`
> 부재)와 컬러칩 인덱스 순환 둘 → [s101-group-setting-api 스펙](2026-08-17-s101-group-setting-api.md).
>
> ✅ **2026-08-13 develop 머지(PR #223).** 아래 2026-08-09 개정본이 머지된 코드와 **거의 그대로 일치**한다.
> 머지 시점 재대조에서 확정한 as-built 3건과 서술 오류 2건만 아래에 반영했다.
>
> | 항목 | 2026-08-09 개정본 | develop as-built |
> |---|---|---|
> | `GroupMemberUiModel` | `nickname`·`colorChipType`·`isMe` | **`id: Long` 선두 추가** — `GroupMemberList`가 `key(member.id)`로 감싼다 |
> | State 계산 프로퍼티 | `isConfirmEnabled` 하나 | **`isNicknameValid`**(= `nicknameError == null`) 추가 — 화면의 Done 게이트가 이 이름을 읽는다 |
> | 유닛 테스트 | 16개 | 16개로 머지 후 **PR #225가 8개 추가해 24개** |
> | `YGColorChipType` 사용처 | "프리뷰 데이터 + `YGTopBar` 프리뷰(`NametagChip5`)뿐" | **틀린 서술** — `app-preview` `YGTopBarPreviewScreen.MemberListSample`이 `NametagChip12`를 쓰고 있었고 재번호와 함께 `NametagChip11`로 옮겼다 |
> | `GroupInviteCodeRoute` 인셋 | "`padding(innerPadding)` + 소비 없는 `imePadding()`" | **기전이 다르다** — 그쪽 `YGScaffold`는 `contentWindowInsets = WindowInsets(0.dp)`라 `innerPadding`이 0이고, EntryBuilder의 `navigationBarsAndImePadding()`과 Route의 `imePadding()`이 **IME 인셋을 두 번** 적용한다 |
>
> 🔁 **2026-08-09 개정 — 구현 완료 시점의 as-built 반영.** 초안 이후 사양 추가와 구조 정정이 여러 건 들어와
> 본문을 그에 맞춰 고쳤다. 초안과 갈린 곳은 아래 절에 각각 근거와 함께 적었다. 요약:
>
> | 항목 | 초안 | as-built |
> |---|---|---|
> | `복사됨` 복귀 | 규칙 미기재 → 화면 이탈까지 유지로 가정 | **2초 뒤 자동 복귀**(사용자 사양) |
> | 클립보드 내용 | 초대 코드 6자리 | **초대 문구 템플릿 2줄**(코드 포함) |
> | State 타입 | 전부 원시 타입 | `groupName`·`myNickname`·`inviteCode`를 **도메인 VO**로, 에러는 **`NameValidResult.Error?`** |
> | 유효성 표시 매핑 | VM이 `@StringRes` 산출(S-002 as-built 답습) | **`core:ui` 확장으로 이관**(ADR-0016 원안 수렴, 4개 화면 동시) |
> | 확인 버튼 배치 | 스크롤 위 오버레이(`Box` 하단 정렬) | **스크롤 영역의 형제**(`weight(1f)`), 겹침 없음 |
> | `EntryBuilder` | 수정 불필요 | **`consumeWindowInsets` 추가**(인셋 이중 계산으로 버튼이 떠오름) |
> | 화면 모델 위치 | `viewmodel` 패키지 동거 | **`impl.model` 패키지 분리** |
> | 테스트 | 없음(테스트 기반 미머지) | **유닛 테스트 16개**(#219 머지로 기반 확보) |

## 목표

그룹 사이드 메뉴 화면 S-101을 구현한다. 그룹 속 내 닉네임(조회 + 인라인 편집) · 그룹원 목록 · 초대 코드 카드 · Danger Zone(그룹 나가기 / 그룹 신고하기)의 4블록으로 구성한다. 데이터는 이번 범위에서 **ViewModel Mock 상태**로 두고 실제 API는 결선하지 않는다(G-001 그룹 목록과 같은 단계).

Figma의 `S-102`는 별도 화면이 아니라 **이 화면의 닉네임 편집 상태**다. 편집 중에도 그룹원 목록·초대 코드·Danger Zone이 같은 자리에 그대로 남고 하단에 `확인` 버튼과 키보드만 얹힌다. 따라서 라우트를 분리하지 않고 단일 화면의 상태 분기로 구현한다.

## 범위

- **포함**:
  - `GroupSettingRoute`(현재 `// TODO impl` stub) 본문 구현 + `GroupSettingScreen`(stateless) + `GroupSettingViewModel`(MVI).
  - 닉네임 인라인 편집 모드: 포커스 진입 · 실시간 유효성 검사 · `확인` 버튼 활성/비활성 · 확정/취소.
  - 초대 문구 클립보드 복사 + 카드 우측 텍스트 `복사됨` 전환(2초 뒤 자동 복귀).
  - Danger Zone 2항목 UI + 클릭 stub.
  - 화면 로컬 컴포넌트 2종(`GroupNicknameField`·`GroupMemberList`) + 문자열 리소스.
  - `YGColorChipType` 드리프트 2건 수정(아래 별도 절).
  - `YGTextFormField`에 `keyboardOptions`·`keyboardActions` 파라미터 추가(기본값 있어 기존 호출부 무영향) — 키보드 엔터 확정을 위해 필요.
  - `YGTextFieldImpl` 최소 높이 48 고정(`defaultMinSize` + `SizeTokens.Size48`) — 클리어 버튼 등장·소멸마다 행 높이가 재계산돼 필드가 들썩였다.
  - **유효성 표시 매핑을 `core:ui`로 이관**(ADR-0016 원안 수렴) — 이 화면 포함 4개 화면 동시 전환. 아래 별도 절.
  - `GroupSettingViewModel` **JVM 유닛 테스트 16개**.
- **제외**:
  - 그룹 상세 조회·닉네임 변경·그룹 탈퇴·신고 **API 연동**. Repository·UseCase 신설 없음(`ParfaitGroupRemoteDataSource`는 이미 있으나 이번엔 쓰지 않는다).
  - 그룹 나가기·신고 **확인 모달** — Figma 미제공.
  - 상단바 `List-Member`(멤버 겹침 칩) — Figma에서 `opacity 0`이라 비노출이 정답.
  - `+N` 칩(`NametagChipPlus`) — Figma 주석상 캔버스 전용(그룹 멤버수 > 5일 때 남은 수), 이 화면 소관 아님.
  - 신규 디자인시스템 **컴포넌트**·에셋·토큰 **0건**(위 `YGTextFormField` 확장은 기존 컴포넌트 파라미터 추가).
  - Compose UI 계측 테스트 — feature 모듈에 계측 배선이 없고 값 대비 비용이 크다.

## 모듈 / 파일 구성

```
feature/groups/setting/api/.../
  NavKeyGroupSetting.kt              (기존 유지 — data object, Mock이라 groupId 인자 없음)
feature/groups/setting/impl/
  build.gradle.kts                   (수정: parfait.test.unit 플러그인 적용)
  src/main/.../route/GroupSettingRoute.kt         (구현: hiltViewModel + collectAsStateWithLifecycle + effect 수집)
  src/main/.../screen/GroupSettingScreen.kt       (신규: stateless UI)
  src/main/.../viewmodel/GroupSettingViewModel.kt (신규: State/Intent/SideEffect + VM 동거 — S-001 선례)
  src/main/.../model/GroupMemberUiModel.kt        (신규: 화면 모델 — component가 viewmodel을 참조하지 않게 분리)
  src/main/.../component/GroupNicknameField.kt    (신규: YGLabel + YGTextFormField)
  src/main/.../component/GroupMemberList.kt       (신규: YGLabel + YGUserChip 목록)
  src/main/.../navigation/EntryBuilder.kt         (수정: consumeWindowInsets — 아래 인셋 절)
  src/main/res/values/strings.xml                 (신규)
  src/test/.../viewmodel/GroupSettingViewModelTest.kt (신규: VM 상태 전이·유효성·타이머·SideEffect 16개)
core/designsystem/.../component/ygcolorchip/
  YGColorChipType.kt                 (수정: 드리프트 2건)
  YGNametagChipPreviewData.kt        (수정: 위 변경 반영)
core/designsystem/.../component/textfield/
  YGTextFieldImpl.kt                 (수정: keyboardOptions·keyboardActions 전달 + 최소 높이 48)
  YGTextFormField.kt                 (수정: 위 두 파라미터 노출)
core/ui/.../text/
  NameValidResultUiText.kt           (신규: NameFieldType + NameValidResult.Error.toStringResource)
core/ui/build.gradle.kts             (수정: implementation(projects.domain) 추가)
feature/{app/setting,groups/enter}/impl/...  (수정: VM 3곳이 위 확장으로 전환 — ADR-0016 절)
```

## 브랜치 기반

이 작업의 브랜치 `feature/#211-S-101-group-side-menu`는 처음에 `feature/#215-test-environment` 위에 올렸다 — ViewModel 유닛 테스트에 그 브랜치의 `parfait.test.unit` 컨벤션 플러그인과 `:core:testing`(`MainDispatcherRule`)이 필요했기 때문이다.

🔁 **2026-08-09** — 그 브랜치가 **PR #219로 `develop`에 머지**되고 리모트에서 삭제돼, 이 브랜치를 `origin/develop` 위로 rebase했다. 테스트 기반이 이제 develop에 있으므로 **PR은 `develop` 대상으로 바로 낸다**(#219 대기 불필요).

재사용 디자인시스템 심볼: `YGTopBarDetail` · `YGLabel` · `YGTextFormField` · `YGUserChip`/`YGNametagChip` · `YGInviteCard` · `YGDangerZone` + `YGActionItem` · `YGButton(YGButtonType.Large)` · `YGScreen`. 재사용 도메인 심볼: `CheckNameValidUseCase` · `NameValidResult` · `GroupCreateConfig`(닉네임 상한).

## State / ViewModel

한 파일(`GroupSettingViewModel.kt`)에 State + Intent/SideEffect + ViewModel 동거(MVI, `BaseViewModel` 상속). 내비게이션·클립보드는 Intent → SideEffect 경유.

```kotlin
// impl/model/GroupMemberUiModel.kt — component가 viewmodel 패키지를 참조하지 않도록 분리
data class GroupMemberUiModel(
    val id: Long,                                 // GroupMemberList의 key. Mock은 인덱스, 실연동은 서버 memberId
    val nickname: String,
    val colorChipType: YGColorChipType,
    val isMe: Boolean = false,
)

data class GroupSettingUiState(
    val groupName: GroupName,                     // Mock
    val myNickname: GroupNickname,                // 확정된 내 닉네임
    val nicknameInput: String,                    // 편집 중 입력값 — VO 아님(아래 근거)
    val isEditing: Boolean = false,
    val nicknameError: NameValidResult.Error? = null,  // 도메인 의미. 표시 변환은 화면 소관
    val members: List<GroupMemberUiModel>,        // Mock
    val inviteCode: InviteCode,                   // Mock
    val remainingCount: Int,                      // Mock
    val isCodeCopied: Boolean = false,
) : UiState

sealed interface GroupSettingIntent : UiIntent {
    data object ClickBack : GroupSettingIntent
    data class InputNickname(val nickname: String) : GroupSettingIntent
    data class ChangeNicknameFocus(val isFocused: Boolean) : GroupSettingIntent
    data object ConfirmNickname : GroupSettingIntent
    data object ClickCopyInviteCode : GroupSettingIntent
    data object ClickLeaveGroup : GroupSettingIntent
    data object ClickReportGroup : GroupSettingIntent
}

sealed interface GroupSettingSideEffect : UiSideEffect {
    data object NavigateBack : GroupSettingSideEffect
    data class CopyInviteCode(val inviteCode: String) : GroupSettingSideEffect
}
```

- `processIntent`는 intent별 private `handle*()`에 위임하고, 각 `handle*`이 `updateState`/`postSideEffect`를 호출한다(S-001 선례와 동일하게 `when` 분기가 직접 `postSideEffect`를 부르지 않는다).
- State의 계산 프로퍼티는 둘이다. `isNicknameValid` = `nicknameError == null`("닫아도 되는가"), `isConfirmEnabled` = `isNicknameValid && nicknameInput != myNickname.value`(유효성 통과 + 실제 변경이 있을 때만 확인 버튼 활성). 화면이 두 게이트를 각각 읽으므로 이름이 필요하다 — 아래 [편집 모드 동작](#편집-모드-동작) 참고.
- 초대 카드의 상태·문구 선택(`Active`/`Invalid`, `N명 남음`/`복사됨`/`최대 인원 도달`)은 **화면의 private 헬퍼**가 한다. 표시 규칙이라 State가 들 이유가 없다.
- Mock 데이터는 State 기본값으로 둔다(G-001 `GroupListUiState` 선례). 실연동 시 교체 지점을 한 곳으로 모으기 위해 파일 하단 private 상수 블록에 모으고 각 `// TODO: API 연동` 주석에 대응 엔드포인트를 적는다.

**도메인 VO 치환(🔁 2026-08-09)** — `groupName`·`myNickname`·`inviteCode`를 `domain.model.group`의 VO로 올렸다. 전부 검증 없는 얇은 `@JvmInline value class`라 실익은 타입 구분이지만, 실연동 시 매핑이 자연스러워지고 인자 바꿔 넣기 사고를 막는다.

- **`nicknameInput`은 `String`으로 남긴다.** 편집 중 값이라 빈 문자열·공백 시작·특수문자 같은 **유효하지 않은 중간 상태**를 담는다. `GroupNickname`으로 감싸면 "GroupNickname인데 유효하지 않다"는 모순이 되고, `CheckNameValidUseCase`도 `String`을 받는다. `remainingCount`는 대응 VO가 없다(서버 계약에 `memberLimit` 부재).
- Mock 상수는 `String`으로 두고 State 선언부에서 감싼다 — `MOCK_MY_NICKNAME`이 `myNickname`(VO)과 `nicknameInput`(String) 양쪽에 쓰여, 상수를 VO로 만들면 언랩이 되레 늘어난다.
- `CopyInviteCode` SideEffect는 `String`을 싣는다. Route는 클립보드 문자열만 조립하는 프레젠테이션 계층이라 도메인 VO를 알 필요가 없다.
- 이 저장소에서 **UI State가 도메인 VO를 보유하는 첫 사례**다(`AccountInfoUiState` 등은 원시 타입). 실연동 라운드에서 다른 화면도 따를지는 그때 판단한다.

## 편집 모드 동작

| 사건 | 처리 |
|---|---|
| 닉네임 필드 포커스 획득 | `ChangeNicknameFocus(true)` → `isEditing = true`. `확인` 버튼 노출 |
| 입력 | `InputNickname` → `nicknameInput` 갱신 + **매 입력마다** `CheckNameValidUseCase` 실행해 `nicknameError` 갱신(Success는 `null`로 접는다) |
| 유효성 실패 | 필드 `isError = true`(테두리·카운터 강조) + 하단 에러 문구 + `확인` 비활성. Figma `144:8245` 주석 "닉네임 유효성 검사 통과하지 못할 시 비활성화" |
| `확인` 클릭 **또는 키보드 완료(엔터)** | `ConfirmNickname` 하나로 모인다 — `myNickname = nicknameInput`, 그룹원 목록의 내 항목 동기화, `isEditing = false`, 포커스 해제 |
| 배경 탭 / 뒤로가기 / 포커스 상실 | 편집 취소 — `nicknameInput = myNickname.value`, `nicknameError = null`, `isEditing = false` |

- 길이 상한은 `YGTextFormField(maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH)`로 컴포넌트가 입력 자체를 막는다(15자). 카운터 `n/15`·클리어 버튼·포커스/에러 테두리는 `YGTextFieldImpl`에 이미 구현돼 있어 추가 작업이 없다.
- 엔터 확정을 받으려면 `ImeAction.Done` + `onDone` 콜백이 필요한데 `YGTextFormField`가 그 통로를 안 갖고 있었다 → `keyboardOptions`·`keyboardActions`를 기본값과 함께 뚫는다(위 범위 참고).
- 화면에서 확정 콜백은 **`onConfirmNickname()` 실행 뒤 `focusManager.clearFocus()`** 순서여야 한다. 뒤집으면 포커스 상실이 먼저 편집을 취소해 입력값이 되돌아가고, 뒤이은 확정이 조건 미달로 아무 일도 하지 않는다.
  > ⚠️ **테스트가 잠그는 범위에 주의.** `confirmNickname_thenLosesFocus_keepsConfirmedNickname`은 VM에 두 Intent를 그 순서로 직접 넣어 **VM이 그 순서를 견딘다**는 것만 확인한다. **화면이 그 순서로 호출하는지는 어떤 테스트도 잠그지 않는다** — 화면에서 순서를 뒤집어도 유닛 테스트 16개는 전부 통과하고 앱만 깨진다. 이 호출 순서는 육안 확인 항목이다.
- 화면의 확정 게이트는 **`isNicknameValid`**(= `nicknameError == null`)이고, ViewModel의 `handleConfirmNickname` 가드는 **`isConfirmEnabled`**다. 둘이 다른 것이 의도다 — 값을 안 바꾸고 엔터를 누르면 화면은 통과시키고 VM 가드가 확정만 무시해 **키보드만 닫힌다**. 화면 게이트를 `isConfirmEnabled`로 잠그면 키보드가 갇힌다. 버튼은 `isEnabled = isConfirmEnabled`라 변경이 없으면 눌리지 않는다.
- **뒤로가기는 화면이 포커스 해제로 번역한다** — `if (isEditing) focusManager.clearFocus() else onClickBack()`. 배경 탭·포커스 상실 경로는 "포커스 해제가 원인 → 편집 취소가 결과"인데 뒤로가기만 결과만 실행하면, `isEditing`은 꺼지는데 키보드·커서가 남아 편집 모드가 아닌 상태에서 엔터로 확정이 일어난다.
  - ViewModel의 `handleClickBack` 편집 분기는 **이중 방어**로 남긴다. 화면이 편집 중에는 `ClickBack` Intent를 아예 쏘지 않으므로 그 분기는 **프로덕션에서 도달하지 않는다**. 그것을 덮는 테스트(`clickBack_whileEditing_…`)도 마찬가지로 앱에서 발생하지 않는 경로를 잠근다 — 커버리지 착시에 주의.
  - **시스템 back 횟수는 미검증이다.** IME가 back을 먼저 소비할 때 키보드는 닫히지만 **포커스는 남으므로**, 두 번째 back에서 편집 취소, 세 번째에서 화면 종료가 될 수 있다. 실기기로 실측해 이 문장을 정정할 것 → 아래 [남은 실기기 확인](#남은-실기기-확인).
- `확인` 버튼은 `isEditing`일 때만 컴포지션한다. 포커스와 키보드가 같이 움직이므로 이것이 "키보드가 올라와 있을 때만 노출"이다.
- 검증 시점이 기존 `GroupNickNameViewModel`(클릭 시점)과 다르다. 이 화면은 버튼 활성 상태 자체가 검증 결과에 걸려 있어 **입력 시점 검증**이어야 한다 — `AccountInfoViewModel`(S-002)과 같은 방식이다.
- 배경 탭 취소는 `clearFocusOnTap()`(opt-in Modifier)을 화면 루트에 건다. 포커스 해제가 곧 편집 종료이므로 별도 취소 버튼을 두지 않는다.

## UI 매핑 (Figma → 심볼)

루트: `YGScreen(modifier.clearFocusOnTap())` 안에 **단일 `Column(fillMaxSize)`** — 상단바 / 스크롤 영역 / 확인 버튼 영역이 **형제**로 쌓인다.

| Figma | 구현 |
|---|---|
| Top Bar (Status=Detail) | `YGTopBarDetail(title = state.groupName.value, onIconClick = handleBack)` |
| Contents 컨테이너 | `Column(weight(1f) + verticalScroll)` · 좌우 `padding.padding7` · 하단 `padding.padding8` · 블록 간 `Arrangement.spacedBy(gap.gap8)`. **상단 패딩 없음**(Figma에서 Status Bar 48 + Top Bar 60 = 108이고 Contents top도 108) |
| Input-Field | `GroupNicknameField`: `YGLabel` + `YGTextFormField(value, onValueChange, maxLength, isError, errorDescription, keyboardOptions, keyboardActions)`, 라벨↔필드 `gap.gap4` |
| Member-List | `GroupMemberList`: `YGLabel` + `Column(spacedBy(gap.gap4), padding top gap.gap3)`. 라벨↔목록 총 간격이 `gap4 + gap3`인 것은 Figma 그대로다(Member-List gap 12 + List-Container padding-top 8) |
| └ User-Chip | `YGUserChip(colorChipType, userFirstName, chip = YGNametagChipStyle.Style40, userName, userStyle)` — 본인은 `YGUserNameStyle.StyleBold` + 이름 뒤 `(나)`, 타인은 `StyleMedium`. **클릭 없음**(Figma 주석 "내 프로필 눌러도 뎁스없음") |
| Invite-Card | `YGInviteCard(label, inviteCode, subText, status, copyButtonText, onCopyClick)` 그대로 사용(신규 없음) |
| Danger-Zone | `YGDangerZone(topZone = YGActionItem(그룹 나가기), bottomZone = YGActionItem(그룹 신고하기))`, `fillMaxWidth` |
| Button-Area (편집 모드) | 스크롤 영역의 **형제** `Box` + `Modifier.imePadding()`, 배경 `YGAtomicColors.Gray.White` + 사방 `padding.padding7`, `YGButton(text = 확인, buttonType = YGButtonType.Large, isEnabled = state.isConfirmEnabled)`. `isEditing`일 때만 컴포지션 |

**버튼을 오버레이가 아니라 형제로 두는 이유(🔁 2026-08-09)** — 초안은 `Box` 안에서 버튼을 하단 정렬 오버레이로 얹었는데, 실기기에서 **편집 중 끝까지 스크롤해도 초대 코드 카드와 Danger Zone이 버튼 뒤에 갇혔다**. 키보드가 뜨면 뷰포트가 줄어 스크롤이 생기고 버튼이 마지막 구간을 덮기 때문이다. Figma `Contents`가 `Height=Hug(659)`·`Scroll position=Fixed`라 스크롤이 없다고 읽었으나, 그건 375×812 + 키보드 없는 조건이다.

하단 여백을 버튼 높이만큼 더해 보정하는 방법(측정 상태 + `isEditing` 분기)을 거쳤다가 **겹침 자체를 없애는 쪽으로 정리했다.** 스크롤 영역에 `weight(1f)`를 주면 뷰포트가 버튼 위까지로 제한돼 마지막 블록이 항상 도달 가능하고, 높이 측정·패딩 분기가 전부 불필요해진다. 측정값 변화가 화면 전체 recomposition을 유발하던 문제와 편집 진입 첫 프레임에 여백이 부족하던 문제도 함께 사라졌다.

- 목록은 최대 12명(위키 [[그룹]])이라 `LazyColumn` 대신 `Column` + `verticalScroll`을 쓴다.
- 닉네임 첫 글자는 `nickname.take(1)`로 뽑는다(`first()`는 빈 문자열에서 예외).
- `MainActivity`가 `enableEdgeToEdge()`를 켜둬 IME 인셋이 전달된다. 인셋 이중 계산은 아래 절 참고.

## 창 인셋 처리

`EntryBuilder`가 `YGScaffold`의 `innerPadding`을 `padding`으로 적용하되 **`consumeWindowInsets(innerPadding)`으로 소비**한다.

소비하지 않으면 하위의 `imePadding()`이 창 바닥 기준 IME 인셋(키보드 + 내비게이션 바)을 통째로 다시 적용해, **확인 버튼이 키보드 위로 내비게이션 바 높이만큼 떠오른다**(실기기에서 확인). 초안은 "`EntryBuilder` 수정 불필요"였으나 그건 배경색만 본 판단이었다.

> ⚠️ `GroupInviteCodeRoute`에도 IME 인셋이 두 번 걸린다. **다만 기전은 이 화면과 다르다**(2026-08-13 재대조) — 그쪽 `EntryBuilder`는 `YGScaffold(contentWindowInsets = WindowInsets(0.dp))`라 `innerPadding`이 0이고 인셋을 직접 `statusBarsPadding()` + `navigationBarsAndImePadding()`으로 붙이는데, `GroupInviteCodeRoute`가 거기에 `modifier.imePadding()`을 또 얹는다. 이번 범위 밖이라 손대지 않았다 → 열린 질문.

## 초대 코드 복사

- `ClickCopyInviteCode` → `isCodeCopied = true` + `postSideEffect(CopyInviteCode(inviteCode.value))`.
- `GroupSettingRoute`가 effect를 받아 Compose 클립보드 API(`LocalClipboard` + `ClipEntry`)로 복사한다. 화면 밖 플랫폼 자원이라 VM이 직접 만지지 않는다.
- **클립보드에 들어가는 것은 코드가 아니라 초대 문구다**(사용자 사양):
  ```
  친구가 파르페에 초대했어요.
  체리 올리러 가볼까요? {코드}
  ```
  `strings.xml`에 `%1$s` 포맷으로 두고 **조립은 Route가** 한다(`stringResource`는 UI 계층 소관). `CopyInviteCode` SideEffect는 코드만 싣고 ViewModel은 문자열 리소스를 모른다. `stringResource`는 `LaunchedEffect` 밖에서 템플릿만 읽어두고 안에서 포맷한다(`LaunchedEffect` 블록은 `@Composable`이 아니다).
- 카드 우측 문구 우선순위: `remainingCount <= 0`이면 `최대 인원 도달`(`Invalid`) → `isCodeCopied`면 `복사됨` → 그 외 `N명 남음`.
- **`복사됨`은 2초 뒤 원래 문구로 돌아온다**(사용자 사양). ViewModel이 `viewModelScope`에서 타이머를 소유하고, **연타 시 이전 Job을 `cancel()`** 해 마지막 클릭 기준 2초가 되게 한다. 취소하지 않으면 먼저 걸린 타이머가 중간에 문구를 되돌린다. 지연값은 파일 하단 private 상수.
- 복사 토스트는 **띄우지 않는다**. 디자인시스템에 `YGToastType.InviteCode("초대 코드를 복사했어요")`가 있으나 이 화면 사양에 없고, 카드 문구 전환 자체가 피드백이다.

> 🔁 초안의 "복귀 규칙 디자인 미기재 → 화면 이탈까지 유지" 가정은 폐기됐다.

## `YGColorChipType` 드리프트 수정 (동반 변경)

Figma 컴포넌트셋 `144:5415`는 타입 `1~12` + `+` = 13종인데 코드는 `NametagChip1~13` + `NametagChipPlus` = 14종이다. 대조 결과 원인 2건:

1. **`NametagChip11`이 `NametagChip3`과 완전 중복**(Cherry400 / Cherry100 / Melon500). 이 중복 때문에 뒤 항목이 한 칸씩 밀려 코드 `12`가 Figma `11`, 코드 `13`이 Figma `12`에 대응한다.
2. **`NametagChip9`의 텍스트 색 드리프트** — Figma 9번은 Melon500 / Cherry50 / **Pudding500**인데 코드는 텍스트도 Cherry50이라 글자색이 테두리색과 같다.

조치: `NametagChip11`(중복)을 삭제하고 이후 항목을 재번호(`12`→`11`, `13`→`12`)해 **12종 + Plus**로 Figma와 정렬한다. `NametagChip9`의 `textColor`를 `Pudding.Pudding500`으로 정정한다. `YGNametagChipPreviewData`의 목록도 함께 맞춘다.

영향 범위 — 화면 코드 사용처는 0건이고, 사용처는 셋이다: `YGNametagChipPreviewData`(목록 동반 수정) · `core:designsystem` `YGTopBar` 프리뷰(`NametagChip5`, 재번호 대상 밖) · **`app-preview` `YGTopBarPreviewScreen.MemberListSample`(`NametagChip12`)**. 세 번째는 초안이 "없다"고 적었던 것이 틀린 것이고(계획서 Task 1의 grep 전제 오류, 구현자가 잡았다), 재번호 뒤에도 **같은 색을 계속 가리키도록** `NametagChip11`로 함께 옮겨 렌더가 변하지 않는다. 이 수정으로 위키 [[nametag-chip]] "12종"과 코드가 일치하며, [open-questions](../../../synthesis/open-questions.md)의 12종/14종 불일치 항목이 닫힌다.

## 컬러칩 배정 규칙

- 타입 배정 주체는 **미정**이다(위키 [[nametag-chip]]: 타입은 유저별로 고정, 부여 주체 미기재). 서버 `ParfaitGroupMemberResponse`도 `memberId`·`groupNickname` 2필드뿐이라 타입 정보가 없다.
- 이번 범위는 Mock이므로 **목록 인덱스 순환**으로 배정하고, 실연동 시 교체할 지점에 TODO를 남긴다. 12종을 넘는 인덱스는 나머지 연산으로 감싼다.

## 문자열 리소스 (`res/values/strings.xml`)

| key | 값 |
|---|---|
| `group_setting_nickname_label` | 그룹 속 내 닉네임 |
| `group_setting_member_label` | 그룹원 (%1$d) |
| `group_setting_invite_label` | 그룹 초대 코드 |
| `group_setting_invite_remaining` | %1$d명 남음 |
| `group_setting_invite_copied` | 복사됨 |
| `group_setting_invite_full` | 최대 인원 도달 |
| `group_setting_copy` | 복사 |
| `group_setting_leave` | 그룹 나가기 |
| `group_setting_report` | 그룹 신고하기 |
| `group_setting_confirm` | 확인 |
| `group_setting_member_name_me` | %1$s (나) |
| `group_setting_invite_message` | 친구가 파르페에 초대했어요.\n체리 올리러 가볼까요? %1$s |

정적 UI 라벨만 리소스화한다. 닉네임·그룹명·초대 코드처럼 **데이터인 값은 State 소유**다. 유효성 에러 문구는 `core:ui` 기존 리소스를 재사용한다.

## 유효성 표시 매핑 — ADR-0016 원안 수렴 (동반 변경)

이 화면을 만들며 VM `when`으로 `@StringRes`를 산출하는 기존 방식을 답습하면 **같은 매핑이 4번째로 복제**된다. [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md)이 결정한 원안은 `core:ui`가 매핑을 단일 소유하는 것이고, 그 ADR이 명시적으로 기각한 대안이 "feature마다 매핑 보유"다. 그래서 이번 라운드에 **4개 화면을 동시에 원안으로 수렴**시켰다.

신설: `core/ui/.../text/NameValidResultUiText.kt`
```kotlin
enum class NameFieldType { NICKNAME, GROUP_NAME }

@Composable
fun NameValidResult.Error.toStringResource(fieldType: NameFieldType): String
```
- `core:ui` → `:domain` 의존을 추가한다(ui → domain, 허용 방향).
- UI State는 `NameValidResult.Error?`(도메인 의미)를 보유하고 **화면이 렌더 시점에 변환**한다.
- `fieldType`이 필요한 이유: `SpaceAtEdge`·`EmptyString`은 닉네임용/그룹명용 문구가 갈리고(`error_space_at_edge_nickname` / `_groupname`) 나머지 2종은 공용이라, 확장 하나로 못 덮는다.

전환 대상 4곳 — `GroupSettingViewModel`·`AccountInfoViewModel`·`GroupNickNameViewModel`(전부 `nicknameError`, `NICKNAME`) / `GroupCreateViewModel`(`groupNameError`, `GROUP_NAME`).

부수 이득 두 가지:
- **클릭 시점 검증 2곳의 `when`이 5분기 → 2분기**로 줄었다(`Success` / `is Error`).
- ViewModel과 그 테스트에서 `core:ui`의 `R` 참조가 사라졌다. 저장소 전체에서 `CoreR`를 쓰는 곳은 이제 실제 리소스를 해석하는 `NameValidResultUiText.kt` 하나뿐이다.

**Compose stability 실측(2026-08-09)** — State가 도메인 sealed 타입을 들면 `domain`이 Compose Compiler를 안 거치므로 unstable로 뒤집힐 수 있다는 우려가 리뷰에서 나왔다. compose compiler report를 켜서 확인한 결과 `AccountInfoUiState`는 `runtime class` / `<runtime stability> = Uncertain(Error)`이고 `AccountInfoScreen`은 **`restartable skippable`을 유지**한다. `data object` 싱글턴이라 런타임 동일성 비교도 성립한다 — 전환으로 인한 **skip 회귀 없음**이라 stability 화이트리스트는 불필요하다.

> ⚠️ **측정 범위**: 실측 대상은 `AccountInfoUiState`(`String` + sealed 2필드)다. `GroupSettingUiState`는 `List<GroupMemberUiModel>` 때문에 **이 전환과 무관하게 이미 unstable**이라 위 결론이 그대로 적용되지 않는다. 다만 `List` unstable은 이 저장소 공통 관례이고(`GroupListUiState`·`TermAgreeUiState` 등 동일, `kotlinx.collections.immutable` 도입 이력 0건) 이번 변경이 만든 회귀가 아니다.

이 변경으로 [open-questions](../../../synthesis/open-questions.md) `[2026-07-29] 유효성 결과 매핑 as-built가 ADR-0016 원안과 다름`이 ①(원안 수렴)으로 닫힌다.

## `YGTextFieldImpl` 최소 높이 (동반 변경)

`showClear`(포커스 또는 에러 + 값 있음)일 때 상하 패딩이 `padding5`(12) → `padding1`(2)로 줄고 대신 44dp `YGIconButton`이 행에 들어오는 구조라, 클리어 버튼 등장·소멸마다 행 높이가 재계산돼 필드가 들썩였다.

`Modifier.defaultMinSize(minHeight = SizeTokens.Size48.getDp())`를 체인 맨 앞(배경·테두리보다 먼저)에 걸어 최소 높이를 고정한다. 고정 높이가 아니라 최소 높이라 콘텐츠가 커질 여지는 남는다.

> 토큰 산술로는 두 상태 모두 48이 나온다(12×2+24 = 2×2+44 = 48). 즉 명목상 no-op이고, 실제 들썩임은 폰트 렌더링이 명목 line-height와 어긋나 생긴 오차로 보인다 — `minHeight`가 그 오차를 흡수한다. 실기기에서 흔들림이 남으면 원인이 다른 곳(텍스트 실측 높이)이다.

## 네비게이션 배선

- `EntryBuilder`(`featureGroupSettingEntryBuilder`)는 entry 하나를 유지한다. `YGScaffold`의 `containerColor` 기본값이 이미 `YGAtomicColors.Gray.White`라 배경은 손댈 필요가 없고, 대신 위 [창 인셋 처리](#창-인셋-처리) 절의 `consumeWindowInsets`가 추가된다.
- `NavigateBack → navigator.onBack()`.
- 이 화면으로 들어오는 경로(G-001 또는 C-001)는 이번 범위 밖이다. 현재 `goTo(NavKeyGroupSetting)` 호출자는 없다.

## 검증

- **`GroupSettingViewModel` JVM 유닛 테스트 16개**(`src/test/`, 이 스펙 범위. 같은 파일이 [Danger Zone 팝업](2026-08-09-setting-danger-zone-popups.md) 라운드에서 8개 늘어 develop 현재 24개다) — `parfait.test.unit` + `:core:testing`(`MainDispatcherRule`) + Turbine. 잠그는 규칙: 유효성 5케이스 매핑(연속 공백 포함) · 확정 가드 · 편집 취소 시 입력 원복과 에러 초기화 · 포커스 전이 · 확정 시 그룹원 목록 동기화 · **확정 → 포커스 상실 순서 불변식** · `NavigateBack`/`CopyInviteCode` 방출 · **복사 2초 복귀와 연타 시 타이머 리셋**(가상 시간 제어).
  - **유닛 테스트가 못 잡는 것**: `imePadding()`·인셋 동작, 클립보드 실제 복사, 긴 문자열 레이아웃, 스크롤 도달성 → 육안이 유일한 그물이다. 실제로 이번 라운드의 결함 2건(버튼이 내비바만큼 떠오름, 편집 중 마지막 블록이 버튼 뒤에 갇힘)이 **실기기에서만** 드러났다.
  - Compose UI 계측 테스트는 이번 범위 밖(feature 모듈 계측 미배선).
- 검증선: `:feature:groups:setting:impl:testDebugUnitTest` + 관련 모듈 **컴파일** + **ktlint** + `:app:assembleDebug`(Hilt 그래프, `YGTextFormField` 기존 호출부 회귀, ADR-0016 전환으로 바뀐 테스트 없는 화면 3곳의 회귀).
- `@YGPreview` + `PreviewBox` 프리뷰 **13종**: 화면 6종(기본 / `복사됨` / `Invalid` / 긴 문자열 / 편집 유효 / 편집 오류) + `GroupNicknameField` 4종(기본 / 빈 값 / 에러 / 15자) + `GroupMemberList` 3종(본인만 / 여러 명 / 긴 닉네임 혼합).
- 실기기 육안: 아래 [남은 확인](#남은-실기기-확인) 절.
- **긴 문자열 케이스를 반드시 포함한다** — `bar-listdate` 라운드에서 짧은 샘플만 쓰다 폭 측정 결함을 놓친 전례가 있다. 그룹명 10자·닉네임 15자를 화면·컴포넌트 프리뷰 양쪽에 넣었다.

## 남은 실기기 확인

서브에이전트가 기기를 다룰 수 없어 사람이 해야 하는 항목이다. 굵은 것은 이미 한 번 결함이 나왔던 자리다.

1. **확인 버튼이 키보드 상단에 딱 붙는지** — 제스처 내비게이션과 3버튼 내비게이션 **양쪽**(인셋 높이가 다르다).
2. **편집 중 끝까지 스크롤했을 때 초대 코드 카드·Danger Zone이 다 보이는지.**
3. **편집 중 back을 몇 번 눌러야 화면이 닫히는지 실측** — IME가 back을 먼저 소비할 때 키보드는 닫히지만 포커스는 남으므로 3회일 수 있다. 각 단계에서 키보드·커서·확인 버튼 상태를 기록하고, 그 결과로 위 [편집 모드 동작](#편집-모드-동작)의 서술을 정정할 것.
4. **확인 버튼이 비활성일 때 버튼·주변 여백을 탭해도 입력이 유지되는지** — `clickable(enabled = false)`가 탭을 소비하지 않아 루트 `clearFocusOnTap`이 발화하던 결함을 고쳤다. Compose 버전에 따라 소비 동작이 다를 수 있어 실기기 확인이 확실하다.
5. 무효 입력에서 엔터 → 아무 일도 안 일어나고 키보드 유지. 값 미변경에서 엔터 → 키보드만 닫힘.
6. 복사 → 다른 앱에 붙여넣어 **초대 문구 2줄**이 나오는지, 카드 문구가 2초 뒤 돌아오는지, 연타 시 마지막 클릭 기준인지.
7. 15자 닉네임 + `(나)` 접미가 `YGUserChip`에서 줄바꿈되는지, 긴 그룹명이 상단바에서 2줄로 감기는지(→ 열린 질문의 `YGTopBarDetail`).
8. 닉네임 필드 높이가 클리어 버튼 등장·소멸에 흔들리지 않는지. `YGTextFormField` 기존 호출부 3곳(`AccountInfo`·`GroupCreate`·`GroupNickName`)의 높이도 변하지 않았는지 프리뷰로 대조 — 특히 `GroupCreateScreen`의 `enabled = false` 필드는 `showClear = false` 경로만 탄다.
9. `NametagChip9`의 글자색을 `Pudding500`으로 고친 뒤 `Melon500` 배경 위 대비가 실제로 읽히는지(Figma 정본 대조).

## 주의 / 열린 질문

- **`NavKeyGroupSetting`에 `groupId`가 없다** — Mock이라 지금은 무해하나 실연동 시 인자 추가가 필요하다.
- **`YGTopBarDetail`에 `maxLines`·`overflow`가 없다** — 같은 파일의 `YGTopBarCanvas`·`YGTopBarEmpty`는 `maxLines = 1`을 갖는데 `Detail`만 없어 긴 그룹명이 2줄로 감길 수 있다. 디자인시스템 소관이라 이번 범위 밖.
- **`GroupInviteCodeRoute`에 같은 인셋 이중 계산이 남아 있다** — `padding(innerPadding)` + 하위 `imePadding()`인데 소비가 없다. 이 화면과 같은 증상일 것.
- **`setClipEntry` 예외 처리 없음** — 실패해도 UI는 무손상이나, `LaunchedEffect` 수집 코루틴이 죽으면 이후 SideEffect가 멈추는 이론적 경로가 있다. 다만 이 화면의 SideEffect는 2종뿐이고 뒤로가기는 `OnBack`(BackHandler)이 별도 경로로 살아 있어 화면이 갇히지는 않는다.
- **`core:ui`가 `:domain`을 `implementation`으로 갖는다** — `NameValidResultUiText.kt`의 `toStringResource`는 public이고 리시버가 domain 타입이라 **public API 시그니처에 domain이 노출되는데 의존은 숨어 있다.** 지금은 소비자 4곳이 컨벤션 플러그인으로 `:domain`을 직접 갖고 있어 컴파일되지만, 그 컨벤션에서 `:domain`이 빠지면 원인 불명으로 깨진다.
  - `api` 승격이 의미상 맞으나 **저장소에 `api(...)` 선언이 0건**이고 컨벤션 플러그인 `DependencyHandler`에 `api` 확장 함수 자체가 없다. `0fbddfb1`·`09f49a92`가 `api`를 되돌린 이력도 있어 이번엔 손대지 않았다 — 팀 결정 대상.
- **확인 버튼 스트립의 탭 흡수 방식** — `pointerInput { detectTapGestures {} }`로 탭을 소비해 루트 `clearFocusOnTap`이 발화하지 않게 한다. `detectTapGestures`의 `awaitFirstDown(requireUnconsumed = true)` 동작에 기대는 방식이라 Compose 버전이 바뀌면 깨질 수 있다. 다만 `clearFocusOnTap` 자체가 같은 패턴이라 저장소 관례와는 일관된다.
- **서버 그룹 상세 응답에 `groupName`·`memberLimit`가 없다** — `GET /api/parfait-groups/{groupId}`는 `groupId`·`groupNickname`·`inviteCode`·`members`만 준다([api/parfait-group.md](../../../api/parfait-group.md)). 상단바 제목과 `N명 남음`의 출처가 계약상 없다. 그룹 목록 API에서 이름을 받아 NavKey로 넘기거나 서버에 필드 추가를 요청해야 한다.
  > 📌 **절반만 답했다(2026-08-17, PR #285)** — 그룹명은 NavKey가 아니라 **`GetGroupDetailUseCase`가 `getMyGroups()`를 한 번 더 불러** 붙인다(이름 조회 실패는 실패로 치지 않는다). `memberLimit`은 여전히 없어 `remainingCount`가 mock 1로 남았다 → [s101-group-setting-api 스펙](2026-08-17-s101-group-setting-api.md).
- **컬러칩 타입 부여 주체 미정** — 서버 응답에 타입이 없어 Mock 인덱스 순환으로 대체.
  > ⚠️ **실데이터가 되면서 증상이 실재가 됐다(2026-08-17, PR #285)** — 순환 배정은 그대로인데 목록이 실제 멤버라, 멤버가 들고 나면 남은 사람의 색이 바뀐다(OQ-P-140).
- **그룹 나가기·신고 확인 모달 미제공** — 클릭은 stub(로그 + TODO)이다. Danger Zone 동작 자체가 미구현이라는 뜻이다.
  > ✅ **모달은 붙었다(2026-08-13, PR #225)** — [Danger Zone 팝업 스펙](2026-08-09-setting-danger-zone-popups.md)이 `GroupSettingDialog?` + `YGModalPopup` 2종을 얹었다. 다만 확인 버튼은 여전히 팝업만 닫고 TODO라 **동작 미구현은 그대로**다.
  > ✅ **동작도 붙었다(2026-08-17, PR #287)** — 확인 버튼이 `DELETE …/members/me`·`POST …/reports`를 부르고 성공하면 `replaceAll(NavKeyGroupList)`로 나간다.
- **화면 진입 경로 없음** — `NavKeyGroupSetting`으로 `goTo` 하는 호출자가 아직 없어 이번 라운드에서는 프리뷰·수동 진입으로만 확인된다.
  > ⚠️ **머지 후에도 0건이다(2026-08-13 확인)** — develop 전체에서 `NavKeyGroupSetting` 참조는 선언과 `EntryBuilder` entry뿐이다. 화면 전체가 도달 불가 상태로 머지됐다(C-001 선례) → [open-questions](../../../synthesis/open-questions.md) [2026-08-13].
  > ✅ **닫혔다(2026-08-17, PR #285)** — C-001 상단 메뉴가 `goTo(NavKeyGroupSetting(groupId))`로 연다. 도달 불가 기간은 약 4일이었다. 실기기 육안 확인 항목(S-101 9건 + 팝업 8건)은 이제 막혀 있지 않으나 **수행되지 않았다**.
