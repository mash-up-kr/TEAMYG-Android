---
id: ADR-0016
title: 유효성 결과 — domain 의미 sealed 반환 + 표시 문자열 프레젠테이션 매핑
status: accepted
date: 2026-07-23
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0001, ADR-0005, ADR-0009
related_spec: s002-account-info, s102-group-nickname, a005-group-create
related_architecture: state-management
platforms: android
tags: [adr, parfait, i18n, domain, presentation]
---

# ADR-0016: 유효성 결과 — domain 의미 sealed 반환 + 표시 문자열 프레젠테이션 매핑

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

> ⚠️ **as-built 이탈(2026-07-29, PR #179 develop 머지)** — 결정의 **핵심 방향(domain은 의미만, 표시 문자열은 프레젠테이션 소관)은
> 코드로 실현됐으나 형태가 다르다.** 아래 "결정"은 원안이고, 실제 머지된 코드는 다음과 같다.
>
> | 항목 | 원안(이 ADR) | as-built(#179) |
> |---|---|---|
> | 결과 타입 | `NicknameResult` sealed(`Error.Empty` 등) | `NameValidResult` sealed(`Error.EmptyString` 등), 그룹명에도 공용 |
> | UseCase 위치·인자 | `domain.usecase.group`, `nickName` | `domain.usecase`, `name` |
> | 표시 매핑 소유 | `core:ui` `NicknameResult.Error.toStringResource()`(@Composable) | **각 feature ViewModel의 `when`**이 `@StringRes` ID 산출 |
> | UI State 보유 | `NicknameResult.Error?`(도메인 의미) | `errorMessageResId: Int?`(리소스 ID) |
> | 문자열 소유 | `core:ui` `strings.xml` | 동일 — `core:ui` `strings.xml`(닉네임용/그룹명용 항목 분리) |
> | `core:ui` → `:domain` 의존 | 추가 | **추가되지 않음**(매핑이 feature에 있어 불필요) |
>
> 즉 **문자열 리소스는 공용화됐지만 매핑 로직은 feature마다 중복**된다 — 이 ADR이 "기각"한 대안(feature마다 매핑 보유)에 가깝다.
> 원안으로 수렴할지 as-built를 정본으로 개정할지 미결 → [open-questions](../synthesis/open-questions.md) [2026-07-29].
>
> 📌 **2026-08-04 (PR #192) 갱신** — S-002(`AccountInfoViewModel`)가 as-built 형태로 머지되면서 VM `when` 매핑 사례가
> `GroupNickNameViewModel`·`GroupCreateViewModel`·`AccountInfoViewModel` **3건**이 됐다. S-002 브랜치는 원안대로
> `toStringResource()` 확장을 구현해 갖고 있었으나 develop rebase에서 폐기했다 — 원안으로 되돌리려면 이제 3곳을 동시에 고쳐야 한다.
>
> ✅ **2026-08-09 — 원안으로 수렴함**(S-101 라운드, 브랜치 `feature/#211-S-101-group-side-menu`).
> **2026-08-13 PR #223로 develop 머지 완료** — 아래 수렴본이 develop 코드이고, 이 ADR의 "결정"이 다시 정본이다.
> 전환된 4개 화면의 State 필드도 함께 개명됐다: `AccountInfoUiState.errorMessageResId` → `nicknameError`,
> `GroupNickNameUiState.errorMessageResId` → `nicknameError`, `GroupCreateUiState.groupNameErrorTextResId` → `groupNameError`,
> 신규 `GroupSettingUiState.nicknameError`.
> S-101이 4번째 복제를 만들 자리에서 방향을 뒤집어 **4개 화면을 동시에 전환**했다. as-built 표는 이제 역사이고
> 아래 "결정"이 다시 정본이다. 원안과 갈리는 부분은 다음 두 가지뿐이다.
>
> | 항목 | 이 ADR 원안 | 2026-08-09 수렴본 |
> |---|---|---|
> | 결과 타입 | `NicknameResult` | **`NameValidResult`**(그룹명 공용, #179 as-built 유지) |
> | 확장 시그니처 | `NicknameResult.Error.toStringResource()` | **`NameValidResult.Error.toStringResource(fieldType: NameFieldType)`** |
> | 매핑 소유 | `core:ui` | 동일 — `core/ui/.../text/NameValidResultUiText.kt` |
> | UI State | 도메인 의미 보유 | 동일 — `NameValidResult.Error?` |
> | `core:ui` → `:domain` | 추가 | 동일 — `implementation(projects.domain)` |
>
> **`fieldType` 파라미터가 원안에 없던 이유**: 원안은 닉네임 전용을 전제했다. 실제로는 `SpaceAtEdge`·`EmptyString`의
> 문구가 닉네임용/그룹명용으로 갈리므로(`error_space_at_edge_nickname` / `_groupname`) 확장 하나로 덮으려면 대상 구분이
> 필요하다. `NameFieldType`은 부가 데이터 없는 순수 판별자라 저장소 관례대로 `enum class`다.
>
> 부수 결과: 클릭 시점 검증 2곳(`GroupNickName`·`GroupCreate`)의 `when`이 **5분기 → 2분기**(`Success`/`is Error`)로 줄었고,
> ViewModel·테스트에서 `core:ui`의 `R` 참조가 사라져 저장소에서 `CoreR`를 쓰는 곳은 `NameValidResultUiText.kt` 하나가 됐다.
>
> **Compose stability 실측** — State가 도메인 sealed를 들면 `domain`이 Compose Compiler를 안 거쳐 unstable로 뒤집힐 수
> 있다는 우려가 리뷰에서 나왔으나, compose compiler report로 확인한 결과 `AccountInfoUiState`는 `runtime class` /
> `<runtime stability> = Uncertain(Error)`이고 `AccountInfoScreen`은 **`restartable skippable`을 유지**한다.
> `data object` 싱글턴이라 런타임 동일성 비교도 성립한다 — skip 회귀 없음, stability 화이트리스트 불필요.
>
> 상세는 [s101-group-side-menu 스펙](../superpowers/specs/archive/2026-08-07-s101-group-side-menu.md)의 "유효성 표시 매핑" 절.

> 📌 **as-built 확장(2026-08-15, PR #244) — 같은 형태가 서버 실패 사유로 번졌다.** 그룹 결선 라운드가
> **domain의 마지막 표시 문자열 보유자였던 `InviteCodeResult.errorMessage`를 삭제**했다(모델 자체가 사라짐).
> 그 자리를 대신하는 것은 feature 로컬 enum + `@Composable toStringResource()`다 —
> `InviteCodeError`(A-004)·`GroupNickNameError`(S-102). ViewModel은 `AppError.Server.code`를 enum으로 접고
> 문구는 화면이 붙인다.
>
> - **원안과 어긋나지 않는다** — "domain은 의미만, 표시는 프레젠테이션 소관"이 그대로다. 다만 **매핑 소유가
>   `core:ui`가 아니라 각 feature**다. 근거는 소비처가 하나라는 것(공유 규칙엔 공유 매핑 — 여기는 공유가 아니다).
> - ⚠️ **문구는 이미 겹친다** — 두 enum의 `NETWORK`·`UNKNOWN` 문구가 같고 리소스만 모듈별로 따로 있다.
>   세 번째 화면이 같은 갈래를 복제하면 #179~#223과 같은 경로를 밟는다 → [open-questions](../synthesis/open-questions.md) [2026-08-15].
> - 이 매핑은 `NameValidResult`(입력 형식)와 **별개 축**이다 — S-102는 둘을 동시에 들고
>   형식 오류를 우선 표시한다(`nicknameError ?: submitError`).
>   🔁 **그 병기는 2026-08-27(PR #394)에 끝났다** — S-102의 `submitError`가 State에서 빠지고
>   서버 사유는 `ShowError` 이펙트 + 토스트로 나간다. 입력칸 아래는 형식 오류 전용이다.
>   위 문장이 그리는 "둘을 동시에 들고 `?:`로 고른다"는 형태는 이제 **S-002 계정 정보에만** 남는다
>   (`AccountInfoUiState.submitError`). 별개 축이라는 원칙 자체는 그대로이고, 바뀐 것은
>   두 축을 **같은 자리에 겹쳐 보일지 나눠 보일지**다.

## 맥락

`CheckNameValidUseCase`(domain)가 반환하던 `NicknameResult`는 실패 사유를 **한국어 문자열**로
담고 있었다(`data class NicknameResult(isSuccess, errorMessage: String?)`). 이 구조는 두 문제가 있다.

- **다국어 불가**: 표시 문자열이 domain에 하드코딩되어 Android 리소스(`strings.xml`) 다국어 체계로 통합할 수 없다.
- **레이어 역전**: "무엇이 틀렸나"(도메인 규칙)와 "사용자에게 어떻게 보이나"(표시)가 domain에 섞였다.

닉네임 유효성은 S-102(그룹 내 닉네임)·S-002(앱 닉네임)가 **공유**하므로, 매핑을 특정 feature에 두면 중복된다.

## 결정

**domain은 의미(성공/실패 종류)만 반환하고, 표시 문자열 매핑은 프레젠테이션 공유 레이어(`core:ui`)가 소유한다.**

- `NicknameResult`를 **sealed interface**로 전환: `Success` + `Error.{Empty, SpaceAtEdge, DuplicatedSpace, InvalidCharacter}`. 문자열 필드 제거.
- `CheckNameValidUseCase`는 규칙 위반 시 대응 `NicknameResult.Error` 변형을 반환(순차 검사·첫 실패).
- `core:ui`에 `NicknameResult.Error.toStringResource()`(@Composable, `stringResource` 위임) + 에러 문자열 `strings.xml`. 두 feature가 공용.
- UI State는 표시 문자열이 아닌 `NicknameResult.Error?`(도메인 의미)를 보유. Screen이 렌더 시점에 문자열로 매핑.
- `core:ui`는 이 매핑을 위해 `:domain`에 의존(ui→domain, ADR-0001 단방향과 정합 — 상위 UI가 하위 domain 참조).

## 대안

- **feature마다 매핑 보유** — "feature에서 extension"이라는 직관에 맞고 core 무변경. 그러나 setting·groups가 동일
  에러 문자열 4종·매핑을 각각 복제(DRY 위반). 닉네임 규칙이 이미 공유 domain인데 표시만 분산됨.
  **→ 기각:** 공유 규칙엔 공유 매핑. `core:ui` 단일 소유가 정합.
- **domain이 @StringRes Int 반환** — domain이 Android 리소스 ID를 들고 있어 다국어는 되나 domain이 Android/R에 오염.
  **→ 기각:** domain 순수성 위반(ADR-0001·0011의 domain 비-Android 원칙).
- **매핑을 `messageRes(): @StringRes Int`(비-Composable)로** — 테스트·재사용 유연. 그러나 호출부마다 `stringResource` 필요.
  as-built는 `toStringResource(): @Composable String`으로 호출부(`error?.toStringResource()`) 간결화 선택.
  **→ 보류(대안):** 컴포지션 밖 사용처가 생기면 @StringRes 반환형으로 재검토.

## 영향

**긍정**

- 에러 문자열이 `strings.xml`로 통합 → 다국어 대응 가능.
- domain은 표시 무관·순수 유지. 규칙과 표현 분리.
- 매핑 단일 소유(`core:ui`) → S-102·S-002 및 향후 닉네임 화면 공용, 중복 0.

**트레이드오프**

- `core:ui`가 `:domain`에 의존 추가(허용 방향이나 결합 1건 증가).
- `toStringResource()`가 @Composable이라 컴포지션 밖(예: ViewModel 로그) 사용 불가.

**위험·방어**

- 새 `Error` 변형 추가 시 `when` 매핑이 exhaustive라 컴파일 단계에서 누락 감지(sealed 이점).
- 기존 S-102 소비처 동반 리팩터 완료(GroupNickName VM/Screen), 컴파일·ktlint·assemble 검증.
