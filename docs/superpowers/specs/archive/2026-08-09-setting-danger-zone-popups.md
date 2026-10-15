---
id: setting-danger-zone-popups
title: 설정 Danger Zone 확인 팝업 (App/Group Setting confirmation dialogs)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-13
related_code: AppSettingScreen.kt#AppSettingScreen, AppSettingRoute.kt#AppSettingRoute, AppSettingViewModel.kt#AppSettingState, AppSettingViewModel.kt#AppSettingIntent, GroupSettingScreen.kt#GroupSettingScreen, GroupSettingRoute.kt#GroupSettingRoute, GroupSettingViewModel.kt#GroupSettingUiState, GroupSettingViewModel.kt#GroupSettingIntent, GroupSettingViewModelTest.kt, YGModalPopup.kt#YGModalPopup, YGButtonType.kt#Medium
related_adr: ADR-0005, ADR-0010
related_spec: ygmodalpopup, s101-group-side-menu, app-setting-s001, s002-account-info, unit-test-infrastructure
related_architecture: state-management, design-system
supersedes:
superseded_by:
tags: [spec, parfait, feature, setting, modal]
---

# Spec: 설정 Danger Zone 확인 팝업

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처(source of truth). 본문은 설계 내용에 집중.

> ✅ **2026-08-13 develop 머지(PR #225).** 머지 시점 재대조에서 **드리프트 0건** — 아래 본문의
> State 필드·Intent 8종·멱등 가드·문자열 19키·좌우 배치가 코드와 문자 그대로 일치한다.
> 확정된 수치 2건: `GroupSettingViewModelTest`가 16개 → **24개**, `AppSettingViewModelTest`가
> **5개**(상태 전이 3 + 멱등·교차 2). 앞선 PR #223(S-101 화면)과 **같은 날 연이어** 머지돼
> 그룹 설정 화면은 첫 머지 시점부터 팝업을 갖고 있다.
>
> 남은 것은 본문이 이미 적은 그대로다 — 확인 버튼은 팝업만 닫고 TODO이고(탈퇴·나가기·신고 **동작
> 미구현**), 실기기 육안 항목은 여전히 미수행이며, `GroupSettingScreen` 자체가 `goTo` 호출자
> 0건이라 **팝업까지 통째로 도달 불가**다(→ [s101 스펙](2026-08-07-s101-group-side-menu.md)).
>
> ✅ **셋 중 둘이 결선됐다(2026-08-17, PR #285·#287)** — 그룹 나가기·신고 확인이 실제 요청을 보내고
> 성공하면 `replaceAll(NavKeyGroupList)`로 나간다. 화면도 도달 가능해졌다. **회원 탈퇴는 그대로
> stub**이다(서버 엔드포인트·앱 표면은 있고 확인 핸들러만 로그 한 줄).
>
> 본문 "API 연동" 절이 필요하다고 적은 셋 중 **①(in-flight 필드)만 채웠다** — `isSubmittingDialogAction`
> 신설. ②(순서를 "요청 → 결과 → 닫기"로 뒤집기)는 **뒤집지 않고** 팝업을 먼저 닫은 뒤
> `YGScaffoldV2` 로딩 오버레이가 덮게 했고(팝업을 띄운 채 두면 그 덮개 아래 가려진다),
> ③(`isEnabledButton` 좌우 분리)은 그래서 필요 없어져 손대지 않았다. 실패는 공통 토스트다
> → [s101-group-setting-api 스펙](2026-08-17-s101-group-setting-api.md).

> **2026-08-09 구현 완료(미머지)** — 브랜치 `feature/group-and-app-setting-pop-up`에 커밋 5개
> (`08b97005`·`4ea6b210`·`8d5f477c`·`5b3290e8` + 리뷰 반영 `fff6159e`). **설계에서 뒤집힌 결정
> 0건** — 아래 본문이 그대로 as-built다. 검증: 유닛 테스트 전량 통과·`ktlintCheck`·
> `:app:assembleDebug`, 경고 0.
>
> **2026-08-10 독립 리뷰가 Important 3건을 추가로 잡았다.** ① 세 확인 핸들러에 멱등 가드가 없어
> 동시 멀티터치 시 확인과 취소가 둘 다 발화했다 — API가 붙으면 "취소를 눌렀는데 탈퇴"가 되는
> 경로라 가드 3개 + 교차 방어 테스트를 넣어 해소(`fff6159e`). ② "API 연동은 본문 채우기로 끝난다"는
> 목표 절의 주장이 사실이 아니었다 — 정정하고 열린 질문으로 승격. ③ 좌우 배치 회귀를 잠그는 자동
> 검증이 없다 — 이월(열린 질문).
>
> **육안 확인은 기기가 잠겨 8항목 전부 미수행이다.** 그중 문구·버튼 좌우·닫기 경로·문자열 일치는
> 코드 대조로 판정했지만(리뷰 3회가 `YGModalPopup`의 `Row` 자식 순서와 `YGButtonType` 색까지
> 확인), **실기기 항목이 닫힌 것은 아니다.** 남은 것: 닉네임 편집 중 팝업 왕복 시 입력값 생존
> (**1순위** — 열린 질문 참고)·본문 soft-wrap 실제 위치·IME 복귀·시스템 바 아티팩트
> (`enableEdgeToEdge` vs `decorFitsSystemWindows`)·200% 글꼴 세로 오버플로·아이콘 틴트 대조·
> TalkBack 다이얼로그 인식.

## 목표

앱 설정(`AppSettingScreen`)의 **서비스 탈퇴하기**와 그룹 설정(`GroupSettingScreen`)의 **그룹
나가기**·**그룹 신고하기**는 지금 Danger Zone 항목을 눌러도 ViewModel에서 로그만 찍고 끝난다.
피그마가 정의한 확인 모달 3종을 붙여, 되돌릴 수 없는 액션 앞에 사용자 확인 단계를 세운다.

기능(탈퇴·나가기·신고 실제 수행)은 이번 범위가 아니다 — 확인 버튼은 팝업만 닫고 TODO로 남는다.
서버 계약이 아직 Android에 연결되지 않았기 때문이다([api/README](../../../api/README.md) 기준 Android
대응 심볼 0건이고, **회원 탈퇴는 서버에 엔드포인트 자체가 없다**).

> **정정(2026-08-10)**: 초안은 여기에 "API가 붙을 때 확인 핸들러 본문만 채우면 된다"고 적었는데
> 사실이 아니다. 로딩·실패·중복요청을 담을 자리가 세 군데 다 비어 있다 → [주의 / 열린
> 질문](#주의--열린-질문)의 해당 항목. UI 확정을 먼저 하는 것 자체는 여전히 이득이지만,
> 연동이 "본문 채우기"로 끝나지는 않는다.

## 범위

- **포함**
  - `AppSettingScreen` — 서비스 탈퇴 확인 팝업 노출·닫기.
  - `GroupSettingScreen` — 그룹 나가기 / 그룹 신고 확인 팝업 노출·닫기.
  - 두 ViewModel의 팝업 표시 상태 + Intent 확장.
  - 팝업 문구 문자열 리소스 신설(모듈별 `strings.xml`).
  - ViewModel 유닛 테스트(상태 전이). `feature/app/setting/impl`에 유닛 테스트 컨벤션 플러그인 도입.
- **제외**
  - 탈퇴·나가기·신고의 실제 수행(API·Repository·UseCase). 확인 핸들러는 TODO 주석 + 로그.
  - **로그아웃 확인 팝업** — 요청 범위 밖. `AppSettingIntent.ClickLogout`은 기존 stub 그대로 둔다.
  - 신규 디자인시스템 컴포넌트·에셋·토큰. `YGModalPopup`을 그대로 쓰고 DS 파일은 건드리지 않는다.
  - 팝업 이후 화면 전환(탈퇴 후 로그인 화면 복귀 등).

## 사용 컴포넌트 — 신규 제작 0건

피그마 3개 프레임의 `Popup` 노드는 모두 같은 컴포넌트이고, `core:designsystem`의
`YGModalPopup`(`component/modal/`)이 이미 그것의 1:1 구현이다. 실제로 그 파일의 프리뷰 문구가
"그룹에서 나갈까요?"로, 이 스펙의 팝업 중 하나를 그대로 담고 있다.

| 피그마 요소 | 코드 |
|---|---|
| 딤 배경 + 중앙 정렬 | `Dialog`(Compose `androidx.compose.ui.window`) |
| 경고 아이콘 | `R.drawable.ic_warning_round`, 틴트 `YGAtomicColors.Cherry.Cherry600` |
| 제목 | `YGTheme.typography.title.t03SB` / `Gray.Gray900` |
| 본문 | `YGTheme.typography.body.b02R` / `Gray.Gray500` |
| 좌측 버튼(파괴적 액션) | `YGButton` + `YGButtonType.Medium.Secondary` — `secondaryText`/`onSecondaryClick` |
| 우측 버튼(취소) | `YGButton` + `YGButtonType.Medium.Primary` — `primaryText`/`onPrimaryClick` |

**버튼 좌우 배치 주의**: 피그마에서 파괴적 액션("탈퇴하기"/"나가기"/"신고하기")이 **왼쪽 secondary**,
취소("그만두기")가 **오른쪽 primary**다. 시각적으로 더 강조된 오른쪽 버튼이 "하지 않기"다.
`YGModalPopup`의 파라미터 순서(`secondaryText` → `primaryText`)가 이 배치와 일치하므로
`secondaryText`에 파괴적 액션, `primaryText`에 "그만두기"를 넘긴다. 뒤집으면 사용자가 취소하려다
탈퇴한다.

`DialogProperties`는 기본값을 쓴다 — 바깥 영역 탭·시스템 뒤로가기로 닫힌다. 피그마에 다른 지시가
없고, 파괴적 액션이 기본 동작이 아니라 명시적 좌측 버튼이라 쉬운 이탈이 안전한 쪽이다.

> ✅ **두 배치가 develop에 공존 확정(2026-08-13, PR #225)** — 이 스펙이 머지되면서 `YGModalPopup`
> 호출자가 6곳이 됐고 좌우 의미가 정확히 반으로 갈렸다: #224의 3화면은 **실행이 우 Primary**,
> 이 스펙의 3팝업은 **취소가 우 Primary**. 어느 쪽도 틀린 게 아니라 규약이 없는 것이다
> → [open-questions](../../../synthesis/open-questions.md) [2026-08-12] ②.
>
> 📌 **develop이 반대 배치를 먼저 머지했다(2026-08-12, PR #224)** — A-005 그룹 생성·A-004 초대코드의
> 확인 모달이 **취소=좌 Secondary / 실행(만들기·참여하기)=우 Primary**다. 그쪽은 파괴적 액션이 아니라
> 이 스펙의 근거("강조된 오른쪽이 하지 않기")와 곧장 충돌하지는 않지만, 같은 컴포넌트를 쓰는 화면들의
> 좌우 의미가 화면 성격에 따라 갈리게 됐다. 이 스펙 머지 시 규약으로 세울지 판단 필요 →
> [open-questions](../../../synthesis/open-questions.md) [2026-08-12]. 또 위 "API 연동" 절이 지적한
> `isEnabledButton` 단일 플래그 제약은 #224의 `isCreating` 처리로 **develop에 실사례가 생겼다**
> (생성 중 취소 버튼까지 함께 비활성).

## 팝업 문구 (피그마 확정본)

| 팝업 | 제목 | 본문 | 좌(파괴적) | 우(취소) |
|---|---|---|---|---|
| 서비스 탈퇴 | 파르페에서 탈퇴하시겠어요? | 지금까지 올린 사진은 익명으로 표시되며,<br>삭제되지 않아요. | 탈퇴하기 | 그만두기 |
| 그룹 나가기 | 그룹에서 나갈까요? | 그룹에서 나가도<br>그룹에 올렸던 사진은 지워지지 않아요 | 나가기 | 그만두기 |
| 그룹 신고 | 그룹을 신고할까요? | 신고 후에는 그룹에서 자동으로 나가지며,<br>그룹은 운영 정책에 따라 처리 돼요. | 신고하기 | 그만두기 |

본문 줄바꿈은 문자열 리소스의 `\n`으로 고정한다(피그마가 2줄로 끊어 쓴 그대로). 마침표 유무도
피그마 그대로 — 나가기 본문만 마침표가 없다.

문자열 키:

| 모듈 | 키 |
|---|---|
| `feature/app/setting/impl` | `setting_withdraw_dialog_title`, `setting_withdraw_dialog_body`, `setting_withdraw_dialog_confirm`, `setting_dialog_cancel` |
| `feature/groups/setting/impl` | `group_setting_leave_dialog_title`, `group_setting_leave_dialog_body`, `group_setting_leave_dialog_confirm`, `group_setting_report_dialog_title`, `group_setting_report_dialog_body`, `group_setting_report_dialog_confirm`, `group_setting_dialog_cancel` |

"그만두기"는 모듈 안에서 두 팝업이 공유하므로 그룹 쪽은 키 하나(`group_setting_dialog_cancel`)로
둔다. 모듈 간에는 공유하지 않는다 — feature `:impl` 모듈끼리 리소스를 끌어쓰지 않는 기존 구성을
따른다.

## 동작 / 상태

### AppSetting — Boolean 1개

팝업이 하나뿐이라 열거형을 만들 이유가 없다.

```kotlin
data class AppSettingState(
    // 기존 필드 …
    val isWithdrawDialogVisible: Boolean = false,
) : UiState

sealed interface AppSettingIntent : UiIntent {
    // 기존 …
    data object ClickWithdraw : AppSettingIntent          // 팝업 열기
    data object ConfirmWithdraw : AppSettingIntent        // 팝업 닫기 + TODO
    data object DismissWithdrawDialog : AppSettingIntent  // 팝업 닫기
}
```

- `ClickWithdraw` — 기존 stub 로그를 대체해 `isWithdrawDialogVisible = true`.
- `ConfirmWithdraw` — **팝업이 떠 있을 때만** `false`로 되돌리고 `// TODO 회원 탈퇴 API 연동` +
  `viewModelLogger`. 첫 줄이 `if (!state.value.isWithdrawDialogVisible) return`이다(아래 [멱등
  가드](#확인-핸들러의-멱등-가드)).
- `DismissWithdrawDialog` — `false`. "그만두기" 버튼과 `onDismissRequest`(바깥 탭·뒤로가기)가
  같은 Intent를 쓴다. **dismiss에는 가드를 두지 않는다** — 이미 닫힌 것을 또 닫는 것은 무해하고,
  가드는 되돌릴 수 없는 쪽에만 필요하다.

`AppSettingSideEffect`는 변경 없다 — 팝업은 화면 안에서 끝나고 내비게이션을 유발하지 않는다.

### GroupSetting — 단일 nullable enum

팝업이 둘이라 Boolean 두 개면 "둘 다 켜짐"이라는 표현 불가능해야 할 상태가 타입에 남는다.

```kotlin
enum class GroupSettingDialog { Leave, Report }

data class GroupSettingUiState(
    // 기존 필드 …
    val visibleDialog: GroupSettingDialog? = null,
) : UiState

sealed interface GroupSettingIntent : UiIntent {
    // 기존 …
    data object ClickLeaveGroup : GroupSettingIntent     // visibleDialog = Leave
    data object ClickReportGroup : GroupSettingIntent    // visibleDialog = Report
    data object ConfirmLeaveGroup : GroupSettingIntent   // 가드 통과 시 null + TODO
    data object ConfirmReportGroup : GroupSettingIntent  // 가드 통과 시 null + TODO
    data object DismissDialog : GroupSettingIntent       // null
}
```

`GroupSettingDialog`는 `GroupSettingViewModel.kt`에 `GroupSettingUiState`와 나란히 둔다 — 이
파일이 이미 state/intent/side-effect를 함께 담는 구성이고, 이 열거형은 화면 상태의 일부지 별도
UI 모델이 아니다(`model/GroupMemberUiModel.kt`와 성격이 다름).

`ConfirmLeaveGroup`/`ConfirmReportGroup`은 둘 다 `visibleDialog = null`로 같지만 TODO가 가리키는
API가 다르므로(`DELETE …/members/me` vs `POST …/reports`) Intent를 합치지 않는다. 기존 VM의
`handleClickLeaveGroup`·`handleClickReportGroup` TODO 주석이 그대로 확인 핸들러로 옮겨간다.

`DismissDialog`는 두 팝업 공용이다 — 무엇을 닫는지는 `visibleDialog`가 이미 안다.

### 확인 핸들러의 멱등 가드

세 확인 핸들러는 **자기 팝업이 실제로 떠 있을 때만** 진행한다. 첫 줄이 조기 반환이고, 상태 갱신·
TODO·로그는 그 뒤에 온다.

```kotlin
private fun handleConfirmLeaveGroup() {
    if (state.value.visibleDialog != GroupSettingDialog.Leave) return
    …
}
```

없으면 무엇이 깨지나: Compose `Modifier.clickable`은 형제 컴포저블에 대한 동시 멀티터치를 분리
전달한다. 두 손가락으로 "나가기"와 "그만두기"를 동시에 누르면 확인과 dismiss가 **둘 다** 발화하고,
TODO 자리에 API가 들어간 뒤라면 취소를 누른 사용자가 그룹에서 나가진다. 확인 버튼 연타로 요청이
N번 나가는 것도 같은 뿌리다.

`Leave`/`Report`를 각각 비교하므로 **교차 확인도 막힌다** — 신고 팝업이 떠 있는데 나가기 확인이
들어오면 아무 일도 일어나지 않는다. 이것이 가드의 핵심 가치이고, 유닛 테스트가 잠그는 것도 그
시나리오다.

같은 파일의 `handleConfirmNickname`이 이미 `if (!state.value.isConfirmEnabled) return`으로
시작한다 — 새 핸들러가 그 규약에 합류한 것이지 새 패턴이 아니다.

### 닉네임 편집 상태와의 관계

`GroupSettingScreen`은 닉네임 인라인 편집(`isEditing`)을 갖고 있다. 팝업 Intent는 `isEditing`을
건드리지 않는다 — 팝업을 닫으면 편집 상태가 그대로 남아야 한다. 시스템 뒤로가기는 팝업이 떠 있는
동안 `Dialog`의 자체 window가 먼저 받으므로, 화면의 `OnBack`(편집 중이면 포커스 해제, 아니면
뒤로)과 충돌하지 않는다.

## 표시·제어 규칙

- 팝업은 화면 상태에서 파생될 뿐 조건이 따로 없다 — Danger Zone 항목을 누르면 항상 뜬다.
- 확인 버튼은 항상 활성(`YGModalPopup`의 `isEnabledButton` 기본 `true`).
- 팝업이 뜬 동안 뒤 화면은 상호작용 불가(`Dialog` 기본 동작).

## 파일 구성

**변경**

| 파일 | 역할 |
|---|---|
| `AppSettingViewModel.kt` | state 필드·Intent 3종·핸들러 추가 |
| `AppSettingScreen.kt` | 콜백 2개 추가(`onConfirmWithdraw`·`onDismissWithdrawDialog`), `if (state.isWithdrawDialogVisible)` 분기로 `YGModalPopup` 호출 |
| `AppSettingRoute.kt` | 새 콜백 → Intent 배선 |
| `feature/app/setting/impl/src/main/res/values/strings.xml` | 문자열 4건 |
| `feature/app/setting/impl/build.gradle.kts` | `alias(libs.plugins.parfait.test.unit)` 추가 |
| `GroupSettingViewModel.kt` | `GroupSettingDialog` 열거형, state 필드, Intent 5종, 핸들러 |
| `GroupSettingScreen.kt` | 콜백 3개 추가, `when (state.visibleDialog)` 분기로 `YGModalPopup` 호출 |
| `GroupSettingRoute.kt` | 새 콜백 → Intent 배선 |
| `feature/groups/setting/impl/src/main/res/values/strings.xml` | 문자열 7건 |

**신설**

| 파일 | 역할 |
|---|---|
| `feature/app/setting/impl/src/test/.../AppSettingViewModelTest.kt` | 탈퇴 팝업 상태 전이 |

**변경 없음**: `core:designsystem`(`YGModalPopup` 포함), `app-preview`, `EntryBuilder`,
`NavigationModule`, api 모듈.

## 검증

**유닛 테스트가 잠그는 것** — ViewModel 상태 전이만:

- `AppSettingViewModelTest`: `ClickWithdraw` → `isWithdrawDialogVisible == true` /
  `ConfirmWithdraw` → `false` / `DismissWithdrawDialog` → `false`.
- `GroupSettingViewModelTest`(기존 파일에 추가): `ClickLeaveGroup` → `Leave` /
  `ClickReportGroup` → `Report` / `ConfirmLeaveGroup`·`ConfirmReportGroup`·`DismissDialog` → `null`.
  팝업 Intent가 `isEditing`·`nicknameInput`을 바꾸지 않는 것도 확인한다.

as-built 개수(2026-08-13 머지 기준): `AppSettingViewModelTest` **5개**(신설 파일), `GroupSettingViewModelTest` 16개 → **24개**.

`AppSettingViewModel`은 생성자 의존성이 없어 테스트가 직접 생성한다. 다만 해당 모듈에 유닛 테스트
컨벤션 플러그인(`parfait.test.unit`)이 아직 없어 `build.gradle.kts`에 추가해야 한다
(`feature/groups/setting/impl`이 이미 쓰는 방식 그대로).

**테스트가 잠그지 못하는 것** — 육안 확인이 유일한 그물:

- 팝업 문구·줄바꿈·버튼 좌우 배치. 특히 파괴적 액션이 왼쪽인지.
- Compose `Dialog`는 별도 window라 **`@Preview`에 렌더되지 않는다.** 화면 프리뷰에 팝업 상태를
  넣어도 보이지 않으므로 프리뷰 파라미터를 늘리지 않는다. 팝업 컴포넌트 자체의 모양은
  `app-preview`의 `YGModalPopupPreviewScreen`이 이미 커버하고, 실제 문구가 들어간 모습은
  에뮬레이터에서 확인한다.

## 주의 / 열린 질문

- **피그마 프레임 위치와 코드 진입점 불일치** — 서비스 탈퇴 팝업이 얹힌 피그마 프레임은
  `S-003`(계정 정보 화면)인데, 코드에서 "서비스 탈퇴하기" 항목은 `AppSettingScreen`의 Danger
  Zone에 있다(`AccountInfoScreen`이 아니라). 디자이너가 모달을 어느 프레임 위에 얹었는지는
  진입점 정의가 아니라고 보고 코드 현행(`AppSettingScreen.onClickWithdraw`)을 따른다. 탈퇴
  진입점이 계정 정보 화면으로 옮겨져야 하는지는 확인 대상.
- **신고 팝업 본문 폰트 드리프트** — 피그마에서 신고 팝업 본문만 `SUIT:Bold`로 찍혀 있고 나머지
  둘은 `Regular`다. 부모 노드 폰트 크기가 `0px`로 남아 있는 등 정리 안 된 흔적이 함께 보여
  디자이너 실수로 판단하고, 3개 모두 DS의 `body.b02R`로 통일한다.
- **로그아웃 확인 팝업 부재** — Danger Zone 상단 항목인 로그아웃에는 확인 팝업이 없다. 피그마에
  해당 모달이 없어 이번 범위에서 뺐지만, 탈퇴만 확인받고 로그아웃은 즉시 실행되는 게 의도인지는
  확인이 필요하다.
- **확인 후 화면 전환 미정** — 탈퇴·나가기·신고가 실제로 수행되면 어디로 가는지(로그인 화면,
  그룹 목록 등)가 정해지지 않았다. API 연동 시 SideEffect 신설이 필요하다.
- **팝업 폭 미제어(기존 미결 상속)** — [ygmodalpopup](2026-07-15-ygmodalpopup.md) 스펙이
  `usePlatformDefaultWidth`를 건드리지 않기로 해 팝업 폭은 플랫폼 기본값이다. 피그마는 375 프레임
  안에서 좌우 10 여백을 준 폭으로 그려져 있어 실제 렌더 폭이 더 좁을 수 있고, 그러면 본문 2줄
  줄바꿈이 피그마와 달라진다. 육안 확인 대상 — 어긋나면 DS 쪽 미결이지 이 스펙의 결정이 아니다.
  **같은 뿌리의 미결 1건 추가(2026-08-10 리뷰)**: `YGModalPopup`의 최외곽 `Column`에
  `verticalScroll`이 없어, 좁은 폭 + `\n` 강제 개행 + 200% 글꼴 배율이 겹치면 하단 버튼이 잘릴 수
  있다. 갇히지는 않는다(바깥 탭·뒤로가기 이탈 경로 생존).
- **API 연동이 "본문 채우기"로 끝나지 않는다(2026-08-10 리뷰)** — 세 확인 핸들러의 TODO 자리에
  실제 네트워크 호출이 들어가려면 지금 없는 것 셋이 필요하다. ① 두 `UiState` 어디에도 in-flight·
  error 필드가 없다. ② 확인 핸들러가 **팝업을 먼저 닫는다** — 진행 표시나 실패 재시도를 팝업 안에
  둘 자리가 없어지므로 "닫고 나서 요청" 순서를 뒤집어야 한다. ③ `YGModalPopup.isEnabledButton`이
  좌우 버튼 **공용 단일 플래그**라 "요청 중엔 확인만 비활성, 취소는 살림"이 표현 불가능하다(DS
  무수정 방침이 만든 제약). 탈퇴·나가기·신고는 실패 시 알려야 하는 되돌릴 수 없는 작업인데 현재
  구조의 기본값은 "성공한 것처럼 팝업만 닫힘"이다.
- **좌우 배치 회귀를 잠그는 자동 검증이 없다(2026-08-10 리뷰)** — `YGModalPopup`의 네 인자
  (`secondaryText`·`onSecondaryClick`·`primaryText`·`onPrimaryClick`)가 전부 같은 타입이라
  뒤바꿔 써도 컴파일·유닛 테스트·ktlint가 전부 통과한다. `Dialog`는 `@Preview`에 안 뜨므로
  프리뷰도 그물이 아니다. 현재 배치가 맞다는 것은 리뷰 3회가 `YGModalPopup`의 `Row` 자식 순서와
  `YGButtonType` 색까지 대조해 확인했지만, 다음 변경을 막는 장치는 없다. `parfait.test.compose`가
  build-logic에 이미 있어 계측 테스트를 붙일 수 있으나 feature 모듈 선례가 0건이고 Robolectric이
  없어 실행에 기기가 필요하다. **확인 버튼이 실제로 파괴적이 되는 시점(API 연동)에는 필요하다.**
- **닉네임 편집 중 팝업이 입력값을 지울 수 있다(2026-08-10 리뷰, 실기기 확인 1순위)** —
  `GroupNicknameField`의 `onFocusChanged`가 `ChangeNicknameFocus(false)`로 이어지고 그것이
  `cancelEditing()`을 불러 **`nicknameInput`을 확정값으로 되돌린다.** 팝업이 부모 창의 Compose
  포커스를 떨어뜨리면 입력하던 닉네임이 조용히 사라진다. 코드상으로는 `Dialog`가 윈도우 포커스만
  가져가고 Compose 포커스는 유지할 것으로 보이나 단정할 수 없다. 유닛 테스트
  `dialogIntents_whileEditing_keepEditingState`는 Intent를 직접 주입할 뿐 **`onFocusChanged`
  경로를 지나지 않아** 이 실패 모드를 방어하지 못한다.
