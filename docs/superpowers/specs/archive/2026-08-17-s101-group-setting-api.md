---
id: s101-group-setting-api
title: S-101 그룹 설정 결선 (상세 조회 · 닉네임 변경 · 나가기 · 신고 + C-001 진입)
status: implemented
category: feature-spec
platforms: android
verified: 2026-08-17
related_code: ParfaitGroupRepository, ParfaitGroupRepositoryImpl, GetGroupDetailUseCase, LeaveGroupUseCase, ReportGroupUseCase, GroupDetailVO, GroupSettingViewModel, GroupSettingUiState, GroupSettingIntent, GroupSettingSideEffect, GroupSettingError, GroupSettingRoute, EntryBuilder, NavKeyGroupSetting, CanvasMainViewModel, CanvasMainRoute, GetMyAccountFlowUseCase, ChangeGroupNicknameUseCase, YGScaffoldV2
related_adr: ADR-0005, ADR-0009, ADR-0016, ADR-0017, ADR-0020, ADR-0022
related_spec: s101-group-side-menu, setting-danger-zone-popups, data-api-service-layer, ygscaffold-v2-common-loading-error, user-info-ssot, c001-canvas-today-detail
related_architecture: data-layer, navigation-flow, state-management, design-system
supersedes:
superseded_by:
tags: [spec, parfait, feature, groups, setting, api-consumer]
---

# Spec: S-101 그룹 설정 결선

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.
>
> ⚠️ **사후 스펙(as-built)** — 선작성 스펙 없이 PR #285(`feature/#275-group-setting-api`)·
> PR #287(`feature/#277-group-leave-report-api`)이 develop에 머지됐다(2026-08-17).
> 화면 맥락은 [s101 스펙](2026-08-07-s101-group-side-menu.md), 팝업 맥락은
> [Danger Zone 팝업 스펙](2026-08-09-setting-danger-zone-popups.md).

## 목표

S-101 그룹 설정 화면이 **mock을 전부 버리고 서버를 본다.** 상세 조회로 화면을 채우고, 닉네임
변경·그룹 나가기·그룹 신고가 실제 요청을 보낸다. 그리고 C-001 캔버스에서 **그 화면으로 들어갈 수
있게** 한다 — 지금까지 화면은 있는데 여는 방법이 없었다.

## 범위

- `ParfaitGroupRepository`에 남아 있던 세 갈래(`getGroupDetail`·`leaveGroup`·`reportGroup`)를 도메인에
  올린다 → `ParfaitGroupRemoteDataSource` 8함수가 **전량 Repository를 얻는다.**
- UseCase 셋: `GetGroupDetailUseCase`(신설, 조합) · `LeaveGroupUseCase` · `ReportGroupUseCase`(위임).
- `GroupSettingViewModel`을 Assisted 주입(`groupId`)으로 바꾸고 mock 기본값 5종을 제거한다.
- 진입: `NavKeyGroupSetting`을 `data object` → `data class(groupId)`, C-001 상단 메뉴가 호출자가 된다.
- 화면 컨테이너를 `YGScaffold`(엔트리) → **`YGScaffoldV2`(Route)** 로 이관한다.

범위 밖: 회원 탈퇴(S-003 Danger Zone — 서버 엔드포인트는 있으나 화면은 여전히 stub), 신고 사유
선택 UI, 그룹 정원 표시, 컬러칩 배정 규칙.

## 동작 / 구조

### 층

```
GroupSettingViewModel ─ GetGroupDetailUseCase ─┬─ ParfaitGroupRepository.getGroupDetail
                     │                         └─ ParfaitGroupRepository.getMyGroups   (이름만)
                     ├─ ChangeGroupNicknameUseCase ─ changeMyNickname
                     ├─ LeaveGroupUseCase          ─ leaveGroup
                     ├─ ReportGroupUseCase         ─ reportGroup
                     └─ GetMyAccountFlowUseCase    ─ MemberRepository (isMe 판별)
```

### 상세 조회는 두 번 부른다 — 계약에 그룹명이 없다

`GET /api/parfait-groups/{groupId}`는 `groupId`·`groupNickname`·`inviteCode`·`members`만 준다
([api/parfait-group.md](../../../api/parfait-group.md)). 상단바 제목이 될 **그룹명이 없어서**
`GetGroupDetailUseCase`가 `getMyGroups()`를 한 번 더 불러 같은 `groupId`의 이름만 집어 붙이고,
그 조합 결과를 `GroupDetailVO`(서버 응답 하나에 대응하지 않는 도메인 VO)로 되돌린다.

**이름 조회 실패는 실패로 치지 않는다** — 빈 `GroupName`으로 두고 나머지를 띄운다. 제목 한 줄
때문에 멤버·초대코드까지 못 보여 주는 편이 나쁘다는 판단이다. 상세 조회 실패만 실패로 전파된다.
Repository·UseCase KDoc 양쪽에 "서버가 상세에 `groupName`을 실어 주면 이 두 번째 호출을 걷어낸다"는
`TODO(서버 응답 확장 대기)`가 붙어 있다.

### 로딩은 셋을 합친 파생이다

`GroupSettingUiState`가 in-flight 필드 셋(`isLoadingDetail`·`isSubmittingNickname`·
`isSubmittingDialogAction`)을 따로 들고 `isLoading`이 그 OR다. 셋을 나눈 이유가 각각 다르다 —
첫 조회는 **보여 줄 값이 아직 없어서**, 닉네임 변경은 **왕복 중에 입력 필드를 더 고칠 수 있어서**,
나가기·신고는 **끝나면 화면을 떠나서** 덮는다. 세 갈래 전부 `finally`에서 내려 예외·취소 어느
경로로 빠져나가도 로딩이 걸린 채 남지 않는다.

`isConfirmEnabled`에 `!isSubmittingNickname`이 더해져 왕복 중 확인 버튼 재입력이 막힌다.

### 닉네임은 서버가 받아 준 값만 화면에 남는다

낙관 갱신(먼저 반영하고 실패 시 되돌리기)을 쓰지 않는다 — 되돌아간 이름을 사용자가 "저장됐다"고
읽은 뒤라 더 헷갈린다는 근거다. 성공 응답의 닉네임으로 `myNickname`·`nicknameInput`·**멤버 목록의
내 항목**을 함께 갱신하고 편집 모드를 닫는다.

조회가 늦게 끝나도 **편집 중이면 입력값을 건드리지 않는다**(`withDetail`의 `isEditing` 분기).

### 팝업은 먼저 닫고 요청한다

OQ-P-141이 "닫고 나서 요청" 순서를 뒤집어야 한다고 적었는데, 이번 구현은 **순서를 유지하고 덮개를
바꿨다.** 팝업을 닫은 뒤 `isSubmittingDialogAction`이 켜져 `YGScaffoldV2` 로딩 오버레이가 화면을
덮고, 실패는 공통 토스트가 말한다. 팝업을 띄운 채로 두면 그 덮개 아래 가려 아무것도 알리지 못한다.
따라서 `YGModalPopup.isEnabledButton`의 좌우 공용 플래그 문제(OQ-P-141 ②)도 이번엔 건드리지 않는다.

나가기와 신고는 **결과가 같다** — 둘 다 이 그룹에서 빠져나온다(서버가 신고를 같은 트랜잭션에서
탈퇴로 잇는다). 다른 것은 부르는 API와 실패 로그의 이름뿐이라 `submitDialogAction(key, action,
request)` 한 자리에 모았고, 멱등 가드(`visibleDialog`가 그 팝업일 때만 진행)는 그대로다.

### 성공 후 목적지는 백스택 교체다

`GroupSettingSideEffect.NavigateToGroupList` → `navigator.replaceAll(NavKeyGroupList)`.
`onBack()`이 아닌 이유는 **여기 오기까지 쌓인 화면이 전부 방금 떠난 그룹의 것**이라, 돌아가면
상세·닉네임 변경·신고가 모두 403 `GROUP_NOT_JOINED`로 떨어지기 때문이다. 목록도 새 엔트리로
열려 다시 조회한다.

### 나는 `memberId`로 찾는다

`GetMyAccountFlowUseCase().first()`의 `memberId`와 비교해 `isMe`를 정한다. 그룹 닉네임은 중복이
허용되므로(2026-08-15 서버 변경) **이름으로 나를 찾으면 남을 나로 표시**할 수 있다. 계정 정보를
모르면 아무도 나로 표시되지 않는다 — 틀린 사람을 강조하는 것보다 낫다는 선택이다.

### 실패 표현은 공통 토스트

`GroupSettingError` 3갈래(`INVALID_NICKNAME`·`NETWORK`·`UNKNOWN`) + `@Composable toStringResource()`
(ADR-0016). 입력 형식 오류는 고칠 곳이 눈앞이라 입력칸 아래 인라인으로 남고, **서버가 되돌린 사유는
`ShowError` 이펙트 → `toastPolicy.showError`** 로 나간다. 이펙트 수집은 코루틴이라 `stringResource`를
부를 수 없어, Route가 `GroupSettingError.entries`의 문구를 미리 뽑아 두고 이펙트는 고르기만 한다.

### 진입

```
NavKeyCanvasMain(groupId) ─(상단 메뉴 버튼)─▶ NavKeyGroupSetting(groupId)
```

`CanvasMainIntent.OnClickGroupSetting` → `CanvasMainEffect.NavigateToGroupSetting(groupId)` →
`goTo(NavKeyGroupSetting(groupId = effect.groupId.value))`. C-001이 이미 `groupId`를 Assisted로 들고
있어 넘길 값의 출처가 화면 상태다. `feature/groups/canvas/impl` → `feature/groups/setting/api`
의존 1건 추가(규약대로 `:api`만).

## 드리프트 / 잔존

- **`remainingCount`가 여전히 mock 1이다.** 서버 상세 응답에 `memberLimit`이 없고 그룹 생성 응답에만
  있어 셀 방법이 없다. 다른 값이 전부 실데이터가 된 지금은 **"1명 남음"이 그럴듯하게 틀린 값**으로
  보인다(전에는 화면 전체가 mock이라 오해할 여지가 없었다) → [open-questions](../../../synthesis/open-questions.md).
- **컬러칩은 계속 목록 인덱스 순환**(`NAMETAG_CHIP_TYPES[index % 12]`)이다. 이제 실제 멤버 목록이라
  멤버가 들고 나면 남은 사람의 색이 바뀐다 — 위키 [[nametag-chip]]의 "타입은 유저별 고정"과 정반대
  (OQ-P-140).
- **신고 사유가 하드코딩 상수 하나**다(`GROUP_REPORT_REASON`). 사유 선택 UI가 없는데 서버는 사유를
  필수로 받아(빈 값이면 400 `INVALID_GROUP_REPORT_REASON`) 화면이 대신 채운다 — 모든 신고가 같은
  문자열로 저장된다 → [open-questions](../../../synthesis/open-questions.md).
- **`GroupSettingError`가 네 경로 공용인데 갈래는 셋**이다. KDoc은 "닉네임 변경이 서버에서 되돌아온
  사유"라고 적지만 상세 조회·나가기·신고 실패도 같은 매핑을 탄다. `GROUP_NOT_FOUND`(404)·
  `GROUP_NOT_JOINED`(403)는 전부 `UNKNOWN`("잠시 후 다시 시도해 주세요")이라, **이미 나간 그룹을
  다시 여는 상황과 일시 장애가 같은 문구**다.
- **조회 실패해도 화면은 남는다** — 빈 값 + 토스트다. 재시도 동선은 없다(OQ-P-167 ③).
- **회원 탈퇴는 그대로 stub**이다. 서버 엔드포인트(`DELETE /api/v1/users/me`)와 앱 표면은 있는데
  S-003 확인 핸들러가 여전히 로그 한 줄이다.
- **실기기·실서버 확인 없음.** 화면이 도달 가능해졌으니 s101·팝업 스펙이 남긴 육안 확인 항목
  (S-101 9건 + 팝업 8건)이 이제 **막혀 있지 않다** — 수행되지 않았을 뿐이다.

## 정책 대조

- 그룹 나가기·신고 후 목적지를 그룹 목록으로 정한 것은 앱 판단이다 — 위키 정책에 근거 문서가 없다.
- 신고가 탈퇴를 동반한다는 것은 **서버 동작**이고, 팝업 문구가 그렇게 약속하고 있어 화면이 두 경로를
  같은 목적지로 보낸다.
- 그룹 닉네임 중복 허용(2026-08-15 서버 변경) 때문에 `isMe` 판별이 이름 비교일 수 없다 — 정책
  문서에는 여전히 중복 허용 항목이 없다(OQ-P-176).

## 테스트

유닛 436 → **456건**(+20).

- `GroupSettingViewModelTest` 24 → **35건**: 첫 조회 상태 반영·`myMemberId` 미상·조회 실패,
  닉네임 서버 거절/네트워크 실패, 로딩 3구간(첫 조회 전·닉네임 왕복 중), 나가기·신고 성공/실패,
  연타 시 API 1회, 팝업 미표시 시 무동작, 신고 사유가 공백이 아님.
- `GetGroupDetailUseCaseTest` **5건 신설**: 두 호출 성공·다른 그룹 섞인 목록에서 골라내기·목록 실패
  시 빈 이름·목록에 그룹이 없을 때 빈 이름·상세 실패 전파.
- `ParfaitGroupRepositoryImplTest` 11 → **15건**: 나가기·신고 각 성공/실패 변환.

`GroupSettingViewModel`은 Assisted 팩토리라 테스트가 생성자를 직접 부른다.

## 파일 구성

- `domain/repository/group/ParfaitGroupRepository.kt` — 3함수 추가(8/8)
- `domain/model/group/GroupDetailVO.kt` — 신설(조합 VO)
- `domain/usecase/group/GetGroupDetailUseCase.kt`·`LeaveGroupUseCase.kt`·`ReportGroupUseCase.kt` — 신설
- `data/repository/group/ParfaitGroupRepositoryImpl.kt` — 3함수 위임 + `mapErrorToAppError`
- `feature/groups/setting/api/NavKeyGroupSetting.kt` — `data object` → `data class(groupId)`
- `feature/groups/setting/impl` — `viewmodel/GroupSettingViewModel.kt`·`viewmodel/GroupSettingError.kt`(신설)·
  `route/GroupSettingRoute.kt`·`navigation/EntryBuilder.kt`·`res/values/strings.xml`(문구 3종)·
  `build.gradle.kts`(→ `feature/groups/list/api`)
- `feature/groups/canvas/impl` — `viewmodel/CanvasMainViewModel.kt`(Intent·Effect 각 1)·
  `route/CanvasMainRoute.kt`·`build.gradle.kts`(→ `feature/groups/setting/api`)
