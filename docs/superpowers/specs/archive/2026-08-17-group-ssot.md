---
id: group-ssot
title: 그룹 목록·상세 인메모리 SSoT — Flow 구독 + 명시적 갱신 (Group SSoT)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-20
related_code: ParfaitGroupRepository, ParfaitGroupRepositoryImpl, GroupLocalDataSource, GroupLocalDataSourceImpl, ParfaitGroupRemoteDataSource, MyParfaitGroupVO, ParfaitGroupDetailVO, GroupDetailVO, GetMyGroupsFlowUseCase, RefreshMyGroupsUseCase, GetGroupDetailUseCase, RefreshGroupDetailUseCase, ChangeGroupNicknameUseCase, JoinGroupUseCase, CreateGroupUseCase, LeaveGroupUseCase, ReportGroupUseCase, GroupListViewModel, CanvasMainViewModel, GroupSettingViewModel, LogoutUseCase, WithdrawUseCase, TokenAuthenticator
related_adr: ADR-0023, ADR-0022, ADR-0001, ADR-0009, ADR-0020
related_spec: user-info-ssot, s101-group-setting-api, c001-canvas-today-detail
related_architecture: data-layer, state-management
supersedes:
superseded_by:
tags: [spec, parfait, group, state, cache]
---

# Spec: 그룹 목록·상세 인메모리 SSoT

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ✅ **develop 머지 완료(2026-08-20, PR #307 `refactor/#294-group-data-using-ssot` → develop `8ca3329a`)** —
> 리베이스 뒤 브랜치 팁이 그대로 들어갔다(머지 커밋에 충돌 해소 편집 0건). 아래 as-built 서술은 그대로
> develop 사실이다. 유닛 테스트는 490 → 511건.
>
> ⚙️ **구현 완료 시점 기록(2026-08-17, 브랜치 `refactor/#294-group-data-using-ssot`, `develop` `c36cad49` 위 커밋 12개)** — 계획 7 Task가
> 전부 들어왔고 설계에서 뒤집힌 결정은 없다. **as-built 차이 둘**: ① 세션 정리에서 그룹 캐시 clear를
> 계정 정보 clear **앞에** 둔다(두 경로 모두) — 계정 정보 정리는 DataStore IO라 던질 수 있고, 뒤에
> 두면 그때 그룹 캐시가 지워지지 않아 이 스펙이 "실제 위험"이라 부른 상태가 된다. ② `GroupSettingViewModel`이
> 계정 id를 읽는 자리를 `BaseViewModel.launch` 가드 + `runCatching`으로 감싼다 — 구독을 별도 코루틴으로
> 옮기면서 이전에 있던 예외 가드를 잃어 DataStore 읽기 실패가 크래시가 됐다. develop 리베이스에서
> 새로고침 실패 토스트(`ShowRefreshError`, PR #297)와 합쳤다 — 토스트 규칙은 그대로 두고 판정 기준만
> 캐시 상태(`isNullOrEmpty()`)로 바꿨다.
> 2026-08-20 `c36cad49` 위로 다시 리베이스하면서 **세션 정리를 부르는 경로가 하나 늘었다** —
> develop이 #306으로 들여온 탈퇴(`WithdrawUseCase`)가 `LogoutUseCase`에 정리를 위임하므로
> 탈퇴도 그룹 캐시를 비운다. 설계가 바뀐 것은 아니고 "무엇을 지우는가"를 한 자리에 모아 둔
> 결정이 새 호출자를 공짜로 덮은 것이다(아래 "세션 종료 정리" 참고). 그 리베이스는 텍스트
> 충돌 없이 `:domain:compileTestKotlin`을 깨뜨렸다 — develop이 함께 들여온
> `WithdrawUseCaseTest`가 `LogoutUseCase`를 2-인자로 직접 생성하고 있었다.
> 실기기·실서버 확인은 하지 않았다(아래 "검증 못 한 것" 참고). 미결 2건은
> [open-questions](../../../synthesis/open-questions.md) OQ-P-219·OQ-P-220으로 등록했다.

> **기준선** — 이 스펙은 `Enter` 인텐트(`LifecycleResumeEffect`, 화면이 앞에 설 때마다 재조회)가
> 이미 있다는 전제로 쓴다. 그것은 선행 브랜치 `feature/#288-group-list-refresh`가 만들었고
> **2026-08-17 develop에 머지됐다**(PR #297). 구현 브랜치도 그 위로 리베이스했다.
> 머지 과정에서 develop이 한 가지를 더 얹었다 — **당겨서 새로고침이 실패했는데 보여 줄 목록이
> 남아 있으면 토스트로 알린다**(`GroupListSideEffect.ShowRefreshError`). 아래 "화면"의 실패 규칙은
> 그것까지 반영한 것이다.

## 목표

그룹 목록과 그룹 상세를 `:data`의 인메모리 저장소 한 벌에 두고, 화면은 그것을 `Flow`로
구독한다. 그룹을 만들거나 참여하거나 나가면 그 사실이 저장소에 반영되고, 목록·캔버스·설정
어느 화면이 열려 있든 같은 값을 본다.

지금은 그룹 정보를 화면마다 따로 조회하고, 조회 결과는 그 화면의 `UiState`에만 산다.
그래서 세 가지가 어긋나 있다.

- **같은 사실을 두 번 부른다.** `GetGroupDetailUseCase`는 그룹명을 얻으려고 상세 조회 뒤
  `getMyGroups()`를 한 번 더 부른다. 서버 상세 응답에 `groupName`이 없기 때문이다.
- **캔버스는 그룹명을 모른다.** `CanvasMainViewModel`이 하드코딩 문자열을 쓴다.
- **한 화면의 변경이 다른 화면에 전달되지 않는다.** 설정에서 내 그룹 닉네임을 바꿔도 그 값은
  `GroupSettingUiState`에만 남는다.

> **#288이 이미 닫은 것** — "생성·참여 직후 목록이 갱신되지 않는다"(`goToSingleClearTop`이 기존
> 백스택 엔트리를 재사용해 `init` 조회가 다시 돌지 않던 문제)는 재진입마다 재조회하는 `Enter`
> 인텐트가 해결했다. 이 스펙은 그 재조회의 **대상을 캐시로 바꿀 뿐**이다 — 재진입 재조회 자체는
> 계속 필요하다. 목록의 최근 사진은 다른 멤버가 올려도 바뀌므로 내 앱 안의 변경만 좇아서는
> 최신이 되지 않는다.

계정 정보는 이미 같은 문제를 로컬 SSoT로 풀었다([ADR-0022](../../../adr/0022-user-info-local-ssot.md)).
이 스펙은 그 형태를 그룹에 적용하되, **영속하지 않는다**는 점만 다르다(근거는
[ADR-0023](../../../adr/0023-group-in-memory-ssot.md)).

## 범위

**포함**

- `:data`에 `GroupLocalDataSource`(인메모리) 신설, `ParfaitGroupRepositoryImpl`이 원격·로컬을 조율
- 조회 UseCase를 구독용(`Flow`)과 갱신용(`suspend`)으로 분리
- `GroupListViewModel`·`CanvasMainViewModel`·`GroupSettingViewModel`을 구독 방식으로 이관
- 세션 종료 시 캐시 정리(`LogoutUseCase`·`TokenAuthenticator`)

**제외**

- **폴링**. 이 스펙은 폴링이 붙을 자리(주기적으로 `refreshMyGroups()`를 부르는 트리거)를 남길 뿐
  구현하지 않는다.
- **영속**. 앱을 껐다 켜면 캐시는 비어 있고 첫 조회를 기다린다.
- **멤버 VO 통합**. `ParfaitGroupMemberVO`(계정 id 축)와 `CanvasMemberVO`(멤버십 행 id 축)는
  서버가 같은 쿼리 결과를 서로 다른 id로 투영해서 생긴 둘이다. 합치려면 서버가 두 id를 함께
  실어야 하므로 이번 범위 밖이고, 그 전까지 캔버스 멤버 칩은 캔버스 응답을 계속 쓴다.
- **`remainingCount`·컬러칩 배정** 등 S-101이 남긴 mock. 서버 계약 공백이라 이 작업으로 닫히지 않는다.

## 저장소 구조

`GroupLocalDataSource`는 IO가 없으므로 모든 함수가 non-suspend다.

- `myGroups: StateFlow<List<MyParfaitGroupVO>?>` — **`null`은 "아직 한 번도 못 받음"**,
  `emptyList()`는 "받아 봤는데 그룹이 없음"이다. 이 둘이 같아지면 목록 화면이 빈 상태·0건 온보딩
  툴팁을 조회 전에 띄우고, 첫 조회 실패와 그룹 0건도 구분하지 못한다.
- `groupDetail(groupId): Flow<ParfaitGroupDetailVO?>` — 내부 `Map<GroupId, ParfaitGroupDetailVO>`를
  `map` + `distinctUntilChanged`로 좁힌다. 다른 그룹 상세가 갱신됐다고 이 구독자가 재방출되지 않는다.
- 쓰기 4종: `saveMyGroups`·`saveGroupDetail`·`removeGroup`·`clear`.
  상태 변경은 전부 `MutableStateFlow.update {}`로 한다(읽고 쓰는 사이에 다른 갱신이 끼면
  덮어써지므로).

`ParfaitGroupRepository`는 읽기 둘(`myGroups`·`groupDetail`)과 갱신 둘(`refreshMyGroups`·
`refreshGroupDetail`)을 노출하고, 기존 명령형 함수(생성·참여·닉네임 변경·나가기·신고)는
시그니처를 유지한 채 성공 직후 캐시를 갱신한다.

**갱신 함수는 데이터를 돌려주지 않는다**(`Result<Unit>`). 값을 얻는 길이 `Flow` 하나여야 화면이
반환값을 쓰기 시작하지 않는다 — 둘 다 주면 캐시는 곧 두 번째 출처가 된다.

**그룹명 합성은 `:data`가 아니라 `GetGroupDetailUseCase`가 한다.** 저장소는 "서버가 준 사실 하나 =
캐시 하나"만 지키고, 목록 캐시의 이름과 상세 캐시를 `combine`해 `GroupDetailVO`를 만드는 것은
domain 몫이다. 서버가 상세에 `groupName`을 실어 주면 `combine`만 걷어내면 된다.

## 갱신·무효화 규칙

캐시가 바뀌는 시점은 아래 일곱뿐이다.

| 시점 | 캐시 동작 | 실패 시 |
|---|---|---|
| `refreshMyGroups()` | 목록 통째 교체 | 캐시 불변, `Result.failure` |
| `refreshGroupDetail(groupId)` | 그 그룹 엔트리만 교체 | 캐시 불변, `Result.failure` |
| 그룹 생성 성공 | 이어서 `refreshMyGroups()` | 재조회 실패해도 생성은 `success` |
| 그룹 참여 성공 | 이어서 `refreshMyGroups()` | 재조회 실패해도 참여는 `success` |
| 내 닉네임 변경 성공 | 이어서 `refreshGroupDetail(groupId)` | 재조회 실패해도 변경은 `success` |
| 나가기·신고 성공 | `removeGroup(groupId)` — 목록에서 제거 + 상세 엔트리 폐기 | — |
| 로그아웃·강제 로그아웃 | `clear()` | — |

**생성·참여 후 재조회하는 이유**: 응답(`CreatedGroupVO`·`JoinedGroupVO`)에는 `recentImageUrl`과
`recentImageUploadedAt`이 없어 `MyParfaitGroupVO`를 세울 수 없다. 빈 값으로 껍데기를 끼워 넣으면
목록의 활동순 정렬이 어긋난다. 재조회 실패가 이미 성공한 조작을 실패로 되돌리지 않는 것은
ADR-0022의 닉네임 폴백과 같은 판단이다.

**닉네임 변경도 재조회하는 이유**: 응답은 `groupId`와 바뀐 닉네임뿐이라, 캐시의 `members`에서
"내" 항목을 짚으려면 계정 `memberId`가 필요하다. 저장소가 그것을 알려면 `MemberRepository`를
끌어와 저장소끼리 엮인다. 대신 서버를 한 번 더 부른다 — 닉네임 변경은 드물고, 왕복 동안 화면은
이미 로딩으로 덮여 있다.

**나가기·신고 후 재조회하지 않는 이유**: 그 그룹은 이후 모든 호출이 403 `GROUP_NOT_JOINED`다.
목록에서 지우는 것이 서버 상태와 일치하고 재조회는 낭비다.

**세션 종료 정리**: `LogoutUseCase`가 "무엇을 지우는가"의 단일 자리이므로(ADR-0022) 그룹 캐시
정리를 여기에 더한다. 강제 로그아웃도 `TokenAuthenticator`가 토큰을 지우는 그 자리에서 함께
지운다. 인메모리라 **프로세스가 살아 있는 채 계정이 바뀌면 이전 계정의 그룹이 남는 것**이 실제
위험이다.

그래서 `LogoutUseCase`를 부르는 쪽은 그룹 캐시를 따로 신경 쓰지 않는다. 탈퇴(`WithdrawUseCase`,
#306)가 그 증거다 — 이 스펙이 쓰인 뒤에 develop에 들어왔고 정리를 `LogoutUseCase`에 위임하므로
아무것도 더 붙이지 않고 그룹 캐시까지 비운다. **정리 대상이 또 늘면 고칠 자리는 여전히 하나다.**

## 화면

**UseCase** — 조회가 구독과 갱신으로 갈라진다.

- `GetMyGroupsUseCase`(suspend) → `GetMyGroupsFlowUseCase`(Flow) + `RefreshMyGroupsUseCase`
- `GetGroupDetailUseCase`는 `Flow<GroupDetailVO?>`를 돌려주고 이름 합성을 담당,
  `RefreshGroupDetailUseCase` 신설

이름은 계정 SSoT 선례(`GetMyAccountFlowUseCase`·`RefreshMyAccountUseCase`)를 따른다.

**G-001 그룹 목록**(`GroupListViewModel`)

- `init`에서 캐시를 구독한다. 서버 재조회는 **`Enter` 인텐트가 부른다**(#288이 만든 자리) —
  화면이 앞에 설 때마다 `RefreshMyGroupsUseCase`가 나가고, 표시는 캐시가 맡는다.
  `Refresh` 인텐트(pull-to-refresh)도 같은 갱신을 부른다.
- 재조회가 나가지 않는 경로에서도 목록이 맞다 — 나가기·신고가 캐시에서 그룹을 지우면 목록이
  그 자리에서 반영한다.
- 조회 실패 규칙은 **선행 브랜치가 정한 것을 승계한다**: 보여 줄 목록이 있으면 남기고, 없을 때만
  에러 화면으로 넘긴다. `groupList`가 nullable이 되므로 판정은 `isNullOrEmpty()`가 된다 — **미조회와
  0건을 여기서는 함께 "보여 줄 것이 없다"로 본다.** 사용자가 직접 당긴 새로고침이 실패했고 목록이
  남아 있을 때는 토스트로 알린다(`ShowRefreshError`) — 목록이 그대로인 것은 "새 소식이 없다"와
  구분되지 않기 때문이고, 에러 화면으로 넘어가는 경우는 그 화면이 이미 실패를 말하므로 겹치지 않는다.
- `UiState.groupList`를 nullable로 바꾼다. "미조회"와 "0건"이 갈려야 빈 상태 표현이 정확해진다.
  화면(`GroupListScreen`)은 당분간 `orEmpty()`로 받는다 — 0건 온보딩 툴팁이 결선될 때 이 구분이
  분기의 근거가 된다.

**C-001 캔버스**(`CanvasMainViewModel`)

- `loadCanvasMainInfo()`의 하드코딩 그룹명을 제거하고 목록 캐시에서 `groupId`로 찾아 구독한다.
- 캐시가 비어 있는 진입 경로(프로세스 재시작 후 캔버스 복귀)에서만 `RefreshMyGroupsUseCase`를
  1회 부른다. 실패해도 캔버스는 그대로 그린다 — 이름 한 줄 때문에 캔버스를 막지 않는다.
- `Enter`가 부르는 오늘 캔버스·달력 재조회는 그대로 둔다. 그룹명 구독은 그것과 별개 경로다.
- 멤버 칩은 캔버스 응답을 계속 쓴다(범위 제외 항목 참고).

📌 **as-built(2026-09-08, PR #469) — 목록 캐시가 정본이라는 결정은 그대로이고 두 가지가 붙었다.**
서버가 캔버스 응답에 `groupName`을 실으면서, 목록 캐시가 **아직 비어 있는 동안에는** 캔버스가 준
이름이 상단 바를 채운다(`groupName.ifEmpty { … }`). 위의 "1회 갱신"이 응답을 기다리는 사이가 그
자리다. 또 **목록에 그 그룹이 없을 때 이름을 빈 문자열로 지우던 것**을 지우지 않도록 고쳤다 —
그러지 않으면 방금 채운 이름이 다음 목록 방출에 곧바로 날아간다
→ [api/parfait.md](../../../api/parfait.md) "Android 매핑".

**S-101 그룹 설정**(`GroupSettingViewModel`)

- `init`에서 상세를 구독하고 갱신을 1회 부른다. 캐시가 있으면 첫 프레임부터 값이 있고
  `isLoadingDetail`은 캐시가 없을 때만 참이다.
- 닉네임 변경 성공 후의 수동 상태 갱신이 사라진다 — 재조회 결과가 `Flow`로 내려온다.
- 나가기·신고 성공 시 캐시 제거는 저장소가 한다. 화면은 지금처럼 목록으로 나간다.

**그룹 생성·참여**(`GroupCreateViewModel`·`GroupNickNameViewModel`) — 화면 코드는 바뀌지 않는다.

## 테스트

- **`GroupLocalDataSourceImpl`** — mock이 아니라 실물로 검증한다. 초기 `null`과
  `saveMyGroups(emptyList())` 이후 `emptyList()`가 구분되는지, `removeGroup`이 목록과 상세를 **둘 다**
  지우는지(한쪽만 지우면 나간 그룹의 상세가 남아 다음 진입에서 유령 화면이 뜬다), 다른 그룹 상세
  저장이 구독자를 재방출시키지 않는지, `clear()` 후 `null`로 되돌아가는지.
- **`ParfaitGroupRepositoryImpl`** — 위 갱신 표를 그대로 단언한다. 특히 갱신 실패가 캐시를
  건드리지 않는지, 생성·참여의 후속 재조회가 실패해도 결과가 `success`로 남는지.
- **`GetGroupDetailUseCase`** — 목록 캐시가 비어 있어도 상세가 나오는지(이름은 빈 값), 목록 캐시가
  늦게 채워지면 이름만 갱신돼 재방출되는지.
- **ViewModel** — 목록은 캐시 방출만으로 상태가 갱신되는지, `Enter`가 갱신을 부르는지, 조회 실패가
  캐시가 빈 경우에만 에러 화면으로 가는지(#288 규칙). 설정은 캐시가 있을 때 로딩이 처음부터
  거짓인지와 닉네임 변경 후 값이 재조회 방출로 바뀌는지. 캔버스는 캐시의 그룹명이 화면에 오는지와
  캐시가 비어 있을 때만 목록 갱신을 부르는지.
- **`LogoutUseCase`** — 토큰·계정 정보·그룹 캐시 셋을 모두 지우는지. 강제 로그아웃 경로도 같다.

`Flow` 단언은 Turbine을 쓴다. 매퍼 단독 테스트는 만들지 않고 DataSource 테스트 케이스로 덮는
기존 관례를 유지한다.

## 검증 못 한 것

유닛 테스트로 덮이지 않아 실기기 확인이 남았다.

- 그룹 생성·참여 후 목록에 새 그룹이 실제로 뜨는지(캐시 갱신 + `Enter` 재조회가 함께 도는 경로)
- 설정에서 닉네임을 바꾼 뒤 다른 화면으로 나갔을 때 값이 따라오는지
- 나가기·신고 후 목록에서 그 그룹이 사라지는지
- 계정 전환 시 이전 계정 그룹이 남지 않는지(로그아웃 → 다른 계정 로그인)
- 캔버스 그룹명이 실제 그룹 이름으로 뜨는지

## 열린 질문

- **멤버 응답 id 통일**은 서버 변경이 필요하다. 두 응답이 계정 id와 멤버십 행 id를 함께 실으면
  VO가 하나로 합쳐지고 캔버스 멤버 칩까지 상세 캐시가 흡수한다. 같은 변경이 `parfait/api/parfait.md`가
  적어 둔 미결(탈퇴 멤버 토핑의 `placedBy.groupMemberId`가 `groupMembers`에 없어 조인이 성립하지
  않는 문제)도 함께 정리한다.
- **낡은 값을 알릴 수단이 없다.** 목록은 갱신 실패를 에러 화면으로 알리지만, 캔버스가 캐시에서
  읽는 그룹명은 갱신 실패를 표현할 자리가 없다. ADR-0022가 계정 정보에서 남긴 것과 같은 공백이다.
- **폴링 주기와 트리거 위치**가 미정이다. 이 스펙은 캐시 구조만 세운다.
