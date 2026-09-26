---
id: s002-account-info
title: S-002 계정 정보 화면 (AccountInfo)
status: implemented
category: ui-spec
platforms: android
verified: 2026-08-04
related_code:
  - NavKeyAccountInfo
  - AccountInfoRoute.kt#AccountInfoRoute
  - AccountInfoScreen.kt#AccountInfoScreen
  - AccountInfoViewModel.kt#AccountInfoViewModel
  - AccountInfoViewModel.kt#AccountInfoUiState
  - CheckNameValidUseCase.kt#CheckNameValidUseCase
  - NameValidResult.kt#NameValidResult
  - GroupCreateConfig.kt#GroupCreateConfig
  - YGTopBarDetail.kt#YGTopBarDetail
  - YGLabel.kt#YGLabel
  - YGTextFormField.kt#YGTextFormField
  - ClearFocusOnTap.kt#clearFocusOnTap
  - feature/app/setting/impl/res/values/strings.xml
  - core/ui/res/values/strings.xml
  - EntryBuilder.kt#featureAppSettingEntryBuilder
related_adr: ADR-0005, ADR-0006, ADR-0009, ADR-0016
related_spec: s102-group-nickname, app-setting-s001, clearfocusontap-modifier, ygtext-date-label
related_architecture: state-management, navigation-flow
supersedes:
superseded_by:
tags: [spec, parfait, setting, account, nickname, s002]
---

# Spec: S-002 계정 정보 화면 (AccountInfo)

> 상태·날짜·대상·관련은 frontmatter가 단일 출처. 본문은 설계에 집중.
>
> ⚠️ **선행 리팩터가 먼저 머지됨(2026-07-29, PR #179)** — 이 스펙이 Task 1로 예정했던 domain 리팩터(빈 값 규칙 추가 +
> 의미 sealed 반환)를 **다른 작업(A-005 그룹 생성)이 먼저 구현해 develop에 넣었다.** 형태가 이 스펙·[ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md) 원안과 다르다:
> `NicknameResult` → **`NameValidResult`**(그룹명 공용), `Error.Empty` → **`Error.EmptyString`**(enum 순서상 마지막이나 결과 동일),
> UseCase 패키지에서 `group` 제거·인자명 `name`, 길이 상한은 `domain` **`GroupCreateConfig`**,
> **`core:ui` `toStringResource()` 확장은 머지되지 않았고** 표시 매핑은 각 feature ViewModel이 `@StringRes` ID로 수행한다.
> 🔁 **이 갈림은 2026-08-13(PR #223)에 원안으로 수렴하며 닫혔다** — 아래 as-built 갱신 블록 참고.
> 에러 문자열 리소스는 `core:ui` `strings.xml`에 이미 존재(닉네임용 4종). 아래 본문은 **as-built 계약 기준으로 갱신**했고,
> 매핑 위치를 원안(core:ui 확장)으로 되돌릴지는 미결 → [open-questions](../../../synthesis/open-questions.md) [2026-07-29].
> **남은 구현 범위는 `feature/app/setting/impl`의 AccountInfo 3종(Route/Screen/ViewModel)뿐이다.**
>
> 🔁 **2026-08-03 정정 — Danger Zone은 이 화면이 아니라 S-001 소속** (Figma 실물 대조).
> 초안은 로그아웃·서비스 탈퇴 진입점을 S-002에 뒀으나, Figma `S-002`(node `220:2192`)에는
> **닉네임 Input-Field 하나뿐이고 Danger Zone이 없다.** `Danger-Zone`(node `2019:6010`)은
> Figma `S-001`(node `220:2176`) Contents 최하단에 있다. → 로그아웃/탈퇴 Intent·핸들러·문자열을
> **전부 S-001([S-001 앱 설정](2026-07-19-app-setting-s001.md))로 이관**하고 이 스펙의 해당 범위를 삭제했다.
> 같은 정정으로 상단바가 `YGTopBarBack` → **`YGTopBarDetail(title)`**(Figma Top Bar에 타이틀
> "계정 정보" 존재), 섹션 라벨이 인라인 `Text` → **`YGLabel`**, 화면 최외곽에
> **`Modifier.clearFocusOnTap()`** 적용으로 확정됐다.
>
> ✅ **2026-08-04 develop 머지(PR #192, 브랜치 `feature/#86-app-setting-account-info-screen`)** — 위 정정을 포함해
> **코드=설계 일치**. 대조에서 확정한 as-built 3건(설계가 안 적었던 자리):
> ① Screen 상태 파라미터명이 `state`(플랜 초안 `uiState` 아님, `AppSettingScreen` 미러),
> ② Contents `Column`이 `Arrangement.spacedBy(gap.gap8)`을 유지한 채 자식이 라벨+필드 묶음 하나뿐이라 실효 간격 없음
> (Danger Zone 이관으로 두 번째 자식이 사라진 흔적),
> ③ 프리뷰 에러 케이스가 `core:ui`의 `error_empty_space_nickname`·`error_space_at_edge_nickname` 리소스 ID를 직접 주입.
> 같은 PR이 `clickableYGNoRipple`(사용처 0)·`YGTextFieldImpl`의 clear 노출 게이팅도 함께 develop에 넣었다 —
> [clearfocusontap 스펙](2026-08-03-clearfocusontap-modifier.md)·[YGTextField 스펙](2026-07-10-ygtextfield.md) 참고.

- **화면 ID**: S-002 (앱 설정 → 계정 정보)
- **Figma**: `S-002`(node `220:2192`) — 파르페 v0.1
- **대상 모듈**: `feature/app/setting/impl`(`route/`·`screen/`·`viewmodel/`) + `domain`(공유 UseCase 확장)
- **진입**: S-001 앱 설정([S-001 앱 설정](2026-07-19-app-setting-s001.md))의 "계정 정보" 항목 → `NavKeyAccountInfo`. Route stub·entry·NavKey는 S-001에서 이미 생성됨 → 이 스펙은 stub 본문을 채운다.
- **입력 유효성**: 위키 [[S-002-앱닉네임-정책-v0.1]]·[[이름-입력-규칙]]. S-102와 규칙 동일(앱/그룹 공통 15자).

## 목표

계정 레벨(앱) 닉네임을 조회·수정하는 화면.
닉네임은 입력 즉시 유효성 검사를 돌려 위반 시 인라인 에러를 노출한다.

## 범위

- 포함: 닉네임 입력 폼(최대 15자)·**입력 시 실시간** 유효성 검사·에러 메시지 인라인 노출·클리어(X)·빈 영역 탭 시 포커스 해제·뒤로가기.
- 제외(구현 TODO):
  - **닉네임 저장 영속화** — 프로필 API 미연동. `nickname` 초기값은 placeholder, 저장 usecase 없음(로컬 상태+검증까지만).
- 제외(타 화면 소속):
  - **로그아웃 / 서비스 탈퇴** — Figma상 Danger Zone은 S-001 소속 → [S-001 앱 설정](2026-07-19-app-setting-s001.md)로 이관(위 2026-08-03 정정).

## 화면 구성 (전부 기존 DS 컴포넌트 재사용)

- 최외곽 `YGScreen(modifier = modifier.clearFocusOnTap())` + `OnBack { onClickBack() }`.
  포커스 해제는 `YGScreen` 기본 동작이 아니라 **입력이 있는 화면이 명시적으로 붙이는 opt-in**이다 → [clearFocusOnTap 스펙](2026-08-03-clearfocusontap-modifier.md).
- 상단 `YGTopBarDetail(title = "계정 정보", onIconClick)` → 뒤로. (Figma Top Bar에 caret-left + 타이틀 both)
- 섹션 라벨 `YGLabel("닉네임")` — `component/ygtext/`의 타이포+색 프리셋(`body.b02R` + `Gray400`). Figma Label(14px gray-400)과 일치.
- `YGTextFormField(value, onValueChange, maxLength = GroupCreateConfig.NICKNAME_MAX_LENGTH, isError, errorDescription)` — 카운터(N/15)·클리어(X)·포커스/에러 테두리 내장.
- 라벨↔필드 간격 `gap.gap4`(12dp), Contents 좌우 패딩 `padding.padding7`(Figma Input 폭 335 = 375−40),
  **Contents 상단 패딩 없음**(0).
  > 🔁 **2026-08-04 정정 (Figma `220:2192` 실측 + 코드리뷰 P2 2건)** — 초안은 라벨↔필드 `gap.gap2`(4dp)·
  > 상단 `padding.padding8`(24dp)이었으나 Figma는 Input-Field 자식 간격 12dp이고, Top Bar 하단
  > (y=48+8+44+8=108)과 Contents(`top-[108px]`)가 맞닿아 상단 여백이 0이다.

## API / 인터페이스

```kotlin
// api — 이미 존재(S-001)
@Serializable data object NavKeyAccountInfo : NavKey

// domain — 의미 sealed 결과(ADR-0016 방향). 표시 문자열 미보유. **as-built, #179로 이미 머지됨(무변경)**
sealed interface NameValidResult {
    data object Success : NameValidResult
    sealed interface Error : NameValidResult {
        data object EmptyString : Error
        data object SpaceAtEdge : Error
        data object DuplicatedSpace : Error
        data object InvalidCharacter : Error
    }
}
class CheckNameValidUseCase @Inject constructor() {   // ADR-0009, 패키지 domain.usecase
    operator fun invoke(name: String): NameValidResult
}

// core:ui — 에러 문자열 리소스(닉네임용 4종). **as-built, #179로 이미 머지됨(무변경)**
//   2026-07-22 시점: 원안의 toStringResource() 확장이 미머지라 VM이 리소스 ID로 매핑했다.
//   2026-08-13(PR #223): core:ui text/NameValidResultUiText.kt로 이관 — 아래 갱신 블록.

// impl — MVI (기존 AppSetting 패턴 미러 + GroupNickNameViewModel as-built 미러)
data class AccountInfoUiState(
    val nickname: String = /* placeholder */ "대충지은랜덤닉네임",
    val errorMessageResId: Int? = null,   // 2026-07-22 as-built. #223에서 nicknameError: NameValidResult.Error? 로 교체
) : UiState

sealed interface AccountInfoIntent : UiIntent {
    data class InputWord(val nickName: String) : AccountInfoIntent
    data object ClickBack : AccountInfoIntent
}
sealed interface AccountInfoSideEffect : UiSideEffect {
    data object NavigateBack : AccountInfoSideEffect
}
```

> **필드명은 `GroupNickNameViewModel` as-built를 그대로 미러한다** — `errorMessageResId`(초안 `nickNameErrorResId` 아님).
> 화면 상태가 닉네임 하나뿐이라 필드명에 `nickName` 접두를 두지 않는다.

## 동작 / 상태

- **입력**(`InputWord`): `nickname` 갱신 + **즉시** `CheckNameValidUseCase(nickName)` 실행 →
  결과를 `when`으로 분기해 `errorMessageResId`에 `core:ui` 문자열 리소스 ID를 담는다(`Success`면 `null`).
  확인 버튼이 없으므로 검증은 실시간. Screen은 `stringResource(id)`로 렌더한다(as-built 관용구, `GroupNickNameViewModel` 미러).
  > 🔁 **#223(2026-08-13 머지) 이후**: `checkNameValid(nickName) as? NameValidResult.Error`가 `nicknameError`에 그대로 들어가고(`when` 소멸), Screen이 `nicknameError?.toStringResource(NameFieldType.NICKNAME)`로 렌더한다.
- **길이 상한**: `domain` `GroupCreateConfig.NICKNAME_MAX_LENGTH` → `YGTextFormField(maxLength = …)`가
  16번째 글자 입력을 하드 차단. UseCase는 길이 미검사(입력 단계에서 강제). 위키 "1~15자"와 일치.
  (#179에서 상수 소유처가 Screen 지역 상수 → domain 설정 객체로 이동됨 — 화면별 상수 재정의 금지.)
- **뒤로가기**(`ClickBack`) → `NavigateBack` → `navigator.onBack()`. 시스템 back은 `YGScreenScope.OnBack`이 같은 콜백으로 가로챈다.
- **포커스 해제**: 화면 빈 영역 탭 → `clearFocusOnTap()`이 `LocalFocusManager.clearFocus()` 호출(IME 닫힘).
  `YGTextFormField` 자체 탭은 필드가 먼저 소비하므로 영향 없다.

### 유효성 규칙 (`CheckNameValidUseCase`, 순차 검사 — 첫 실패 반환)

**이 절은 #179로 이미 머지 완료 — S-002 구현 시 domain 변경은 없다.** 4케이스 전부 공용 UseCase가 단일 소유하고,
각 규칙은 `NameValidResult.Error` 변형을 반환한다. 표시 문자열은 `core:ui` `strings.xml` 소유(ADR-0016 방향). 위키 정책 이미지 매핑:

| 순서(as-built) | 규칙(enum) | 조건 | 반환 Error | 표시 문자열(core:ui) |
|---|---|---|---|---|
| 1 | `CheckSpaceStartOrEnd` | 처음/끝 공백 불가 | `Error.SpaceAtEdge` | "닉네임의 처음과 끝에는 공백을 사용할 수 없어요" |
| 2 | `CheckDuplicatedSpace` | 연속 공백(`"  "`) 불가 | `Error.DuplicatedSpace` | "공백은 글자 사이에 1칸만 사용할 수 있어요" |
| 3 | `CheckValidCharacter` | 한글/영문/숫자/공백만 | `Error.InvalidCharacter` | "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요" |
| 4 | `CheckEmptyString` | 빈 값 불가 | `Error.EmptyString` | "닉네임은 비워둘 수 없어요" |

> 원안은 빈 값 규칙을 **선두**에 두려 했으나 as-built는 마지막이다. 빈 문자열은 앞 3규칙을 모두 공백 통과하므로 결과는 같다.

- **에러 문자열 출처**: domain은 `Error` 변형만 반환, 문자열은 `core:ui` `strings.xml`이 소유(다국어 통합). setting·groups 공용.
  ~~단 **매핑 코드는 각 feature ViewModel**에 있다~~ — **매핑도 `core:ui`가 단일 소유한다**(#223 develop 머지, 2026-08-13). ADR-0016 원안 수렴.
- **S-102·A-005 동반**: 같은 UseCase를 그룹 내 닉네임(S-102)·그룹명(A-005)이 공유한다. 그룹명 화면은 `SpaceAtEdge`·`EmptyString`만 그룹명용 문자열로 분기.
  `EmptyString`은 S-102·S-002 모두 입력 비어있을 때만 도달.

## 파일 구성

- `feature/app/setting/api/NavKeyAccountInfo.kt` — 목적지 키(**기존, 무변경**).
- `feature/app/setting/impl/navigation/EntryBuilder.kt` — `entry<NavKeyAccountInfo>` 이미 `AccountInfoRoute` 연결(**무변경**).
- `feature/app/setting/impl/route/AccountInfoRoute.kt` — VM 배선(`hiltViewModel`·`collectAsStateWithLifecycle`·`LaunchedEffect` effect 수집), back→onBack. **stub 본문 채움**.
- `feature/app/setting/impl/screen/AccountInfoScreen.kt` — stateless UI(길이 상한은 `GroupCreateConfig` 참조, 라벨은 `YGLabel`, 최외곽 `clearFocusOnTap()`). `@YGPreview`(기본/에러 상태 PreviewParameter).
- `feature/app/setting/impl/viewmodel/AccountInfoViewModel.kt` — MVI, `CheckNameValidUseCase` 주입. 검증 결과를 **도메인 의미 그대로** State에 담는다(#223 이후 `nicknameError: NameValidResult.Error?`, 그 전에는 `@StringRes` ID).
- `feature/app/setting/impl/res/values/strings.xml` — `account_info_title`·`account_info_nickname_label`
  (닉네임 에러 문자열은 core:ui 소유라 제외. `setting_logout`·`setting_withdraw`는 S-001 소속으로 이관 — 초안의 `account_info_logout`/`account_info_withdraw` 키는 폐기).
- ~~`domain` 모델·UseCase 변경~~ — **#179로 머지 완료(무변경)**: `NameValidResult` sealed, `CheckNameValidUseCase`(`domain.usecase`) 4규칙, `GroupCreateConfig` 길이 상한.
- ~~`core/ui` 매핑 확장·strings 신설~~ — 에러 문자열 4종은 **#179로 `core:ui` `strings.xml`에 이미 존재**. 매핑 확장(`toStringResource`)은 이 화면 구현 시점엔 미머지였다가 **#223(2026-08-13)에 `core/ui/.../text/NameValidResultUiText.kt`로 신설**됐다.

## 🔁 as-built 갱신 (2026-08-13, PR #223 — 유효성 표시 매핑 이관)

이 화면은 [S-101 라운드](2026-08-07-s101-group-side-menu.md)가 ADR-0016 원안으로 수렴시킨 4개 화면 중 하나다. 화면 UI·동작·문자열은 변하지 않았고 **에러 표현 위치만** 옮겼다.

| 항목 | 이 스펙 구현 시점(#192) | develop 현재(#223) |
|---|---|---|
| State 필드 | `errorMessageResId: Int?` | **`nicknameError: NameValidResult.Error?`** |
| 매핑 주체 | `AccountInfoViewModel`의 `when` 5분기 | **`core:ui` `NameValidResult.Error.toStringResource(NameFieldType.NICKNAME)`** |
| 변환 시점 | VM(상태 산출 시) | **화면(렌더 시)** |
| `CoreR` 참조 | VM이 직접 | 저장소에서 `NameValidResultUiText.kt` 한 곳만 |

Compose stability는 이 화면을 실측 대상으로 삼아 검증했다 — `AccountInfoUiState`가 `runtime class`/`Uncertain(Error)`로 뒤집혀도 `AccountInfoScreen`은 `restartable skippable`을 유지한다(`data object` 싱글턴이라 런타임 동일성 비교가 성립). 상세는 [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md) 수렴본 표.

## 검증

프로젝트에 테스트 인프라 없음(S-001·S-102 등 선례 동일) → 유닛테스트 미작성. 다음으로 검증:

- **컴파일**: `:feature:app:setting:impl`·`:domain` 컴파일 통과.
- **ktlint**: `ktlintCheck` 통과.
- **`@YGPreview`**: 기본(placeholder 닉네임)·에러(각 케이스) 상태 프리뷰 렌더 확인. as-built 프리뷰는 4변형
  (정상 2 + 빈 값 에러 + 앞 공백 에러)을 `PreviewParameterProvider`로 낸다.
- **유효성 수동 확인**: 빈 값→에러, 나머지 3케이스 정책 이미지와 메시지 일치.
- **미검증(머지 시점)**: 실기기/에뮬 동작 확인 기록 없음 — 저장 경로가 없어 화면이 로컬 상태만 다루는 상태로 머지됐다.

## 주의 / 열린 질문

- **닉네임 저장 미구현** — 프로필 조회/수정 API 연동 시 초기값 로드 + 저장 트리거(포커스 해제/IME 완료 등) 확정 필요.
  `clearFocusOnTap()`이 이미 포커스 해제 지점을 만들어 뒀으므로, "포커스 해제 시 저장"을 택하면 여기가 훅 지점이 된다.
- **로그아웃/탈퇴는 S-001 소속** — auth·회원 API 연동 시 확인 모달(YGModalPopup) + 실제 처리 결선 필요. 추적은 [S-001 앱 설정](2026-07-19-app-setting-s001.md).
- 유효성: 문자·공백 규칙은 UseCase, 길이(15)는 `domain` `GroupCreateConfig`로 검사 위치 이원화(S-102와 동일 구조) — 단일 소유처 아님(향후 통합 여지).
