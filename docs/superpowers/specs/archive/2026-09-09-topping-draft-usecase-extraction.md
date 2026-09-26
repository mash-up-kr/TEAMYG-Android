---
id: topping-draft-usecase-extraction
title: 토핑 초안 접근을 UseCase 다섯으로 가른다
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-10
related_code:
  - ToppingDraftRepository.kt#ToppingDraftRepository
  - CanvasMainViewModel.kt#CanvasMainViewModel
  - CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceViewModel
  - SegmentationViewModel.kt#SegmentationViewModel
  - SegmentationConfirmViewModel.kt#SegmentationConfirmViewModel
related_adr: ADR-0026
related_spec: c106-topping-place-api
related_architecture: state-management, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, topping, usecase, refactoring]
---

# Spec: 토핑 초안 접근을 UseCase 다섯으로 가른다

## 목표

`feature/*/impl`의 ViewModel이 `ToppingDraftRepository`를 직접 주입받아 부르는 자리를 없앤다.
그 사이에 UseCase를 놓아, 화면이 도메인 계층을 볼 때 언제나 UseCase만 보게 만든다.

## 배경

`feature/*/impl` 아래 ViewModel 21개를 전수조사한 결과, Repository를 직접 주입받는 것은 넷이고
전부 같은 타입 `ToppingDraftRepository` 하나였다. DataSource를 직접 받는 ViewModel은 없다.

- `CanvasMainViewModel` — 흐름을 연다.
- `CanvasToppingPlaceViewModel` — 초안을 구독하고, 배치에 성공하면 비운다.
- `SegmentationViewModel` — 후보를 고르거나 원본을 그대로 쓸 때 적는다.
- `SegmentationConfirmViewModel` — 진입 판정으로 읽고 적으며, 화면 상태로 구독한다.

`ToppingDraftRepository`가 `:domain`에 선언되어 있어 모듈 의존 방향이 뒤집힌 것은 아니다.
어긋난 것은 계층이다. 나머지 ViewModel 17개는 예외 없이 UseCase만 받는다.

토핑 초안이 화면 다섯을 가로지르는 흐름 상태라서([ADR-0026](../../../adr/0026-topping-draft-datastore-ssot.md))
UseCase로 감쌀 단위를 잡기 애매했던 것이 원인으로 보인다. 다만 실제로 쓰이는 Repository 표면은
`draft`·`start`·`clear`·`record` 넷뿐이고 경계도 뚜렷하다.

## 범위

- 포함
  - `:domain`의 `usecase/topping/`에 UseCase 5종 신설.
  - ViewModel 4개의 생성자 의존성과 호출부를 그 UseCase로 교체.
  - `SegmentationConfirmViewModel`의 재사용 진입 판정을 UseCase로 이관.
  - 기존 ViewModel 테스트 4파일의 테스트 더블 교체, 판정 UseCase의 신규 유닛 테스트.
- 제외
  - `ToppingDraftRepository` 인터페이스의 시그니처·KDoc 변경. 계약은 그대로 둔다.
  - `:data`의 `ToppingDraftRepositoryImpl`과 그 테스트.
  - 다른 Repository·다른 ViewModel. 조사에서 위반이 나오지 않았다.
  - Repository를 feature 모듈에서 아예 못 보게 막는 정적 검사 도입. 이번 라운드는 호출부를
    옮기는 데까지다.

## 설계

### UseCase 5종

전부 `com.teamyg.parfait.domain.usecase.topping` 아래에 두고 `@Inject constructor`로
`ToppingDraftRepository`를 받는다. 호출은 저장소 관례대로 `operator fun invoke`다.

| UseCase | 시그니처 | 대체하는 호출부 |
|---|---|---|
| `GetToppingDraftFlowUseCase` | `operator fun invoke(): Flow<ToppingDraft?>` | `CanvasToppingPlaceViewModel#observeDraft`, `SegmentationConfirmViewModel#collectDraft` |
| `StartToppingDraftUseCase` | `suspend operator fun invoke(groupId: GroupId, parfaitId: ParfaitId, nextPositionZ: Int)` | `CanvasMainViewModel#startToppingFlow` |
| `ClearToppingDraftUseCase` | `suspend operator fun invoke()` | `CanvasToppingPlaceViewModel#handleOnClickConfirm` |
| `RecordToppingDraftUseCase` | `suspend operator fun invoke(subjectImagePath: String, cutoutImagePath: String?, borderColorArgb: Int?, borderWidthDp: Float?): Boolean` | `SegmentationViewModel#selectCandidate`·`#useOriginal`, `SegmentationConfirmViewModel#record` |
| `EnsureDraftSubjectRecordedUseCase` | `suspend operator fun invoke(subjectImagePath: String): Boolean` | `SegmentationConfirmViewModel`의 `init` 판정 |

이름은 `GetMyAccountFlowUseCase`를 따른다. 그 KDoc이 `Get…FlowUseCase`를 고른 이유를 적어
두었다 — 호출 자체는 구독하지 않고 `Flow`만 넘기기 때문이다. 초안 구독도 성질이 같다.
(`Observe…UseCase`가 통과 흐름에 쓰인 자리도 있어 이름만으로는 두 계열이 갈리지 않는다.)

앞의 넷은 Repository로 그대로 넘기는 위임이다. **위임이라는 사실 자체가 이 스펙의 결정**이다 —
호출부의 의미가 각각 달라 보여도 도메인 규칙이 붙지 않은 자리에 규칙을 지어내지 않는다.

### 판정 UseCase

다섯 번째만 조합이 있다. `SegmentationConfirmViewModel`은 지금 초안을 `first()`로 한 번 읽어
그것이 현재 알맹이를 가리키는지 보고, 아니면 `record`로 적는다. 판정 기준이 "초안이 비었는가"가
아니라 "이 알맹이를 가리키는가"라는 것은
[c106-topping-place-api](2026-08-20-c106-topping-place-api.md)가 정한 도메인 규칙이므로,
화면이 아니라 UseCase가 든다.

반환은 **"초안이 이 알맹이를 가리키게 되었는가"** 하나다. 이미 가리키고 있었을 때와 새로 적어
성공했을 때가 모두 `true`이고, 적으려다 실패했을 때만 `false`다. 호출부가 두 경우를 갈라 볼 일이
없어서 결과를 하나로 좁힌다.

이 UseCase는 `ToppingDraftRepository`를 직접 받는다. 같은 저장소 표면 둘(`draft`·`record`)을
한 판정 안에서 함께 쓰므로, 위임 UseCase를 거치면 테스트 더블만 두 겹이 되고 얻는 것이 없다.
저장소에 UseCase가 다른 UseCase를 받는 선례(`WithdrawUseCase`·`BootstrapSessionUseCase`)가
있지만 그것은 계층이 다른 부수효과를 재사용하는 경우다.

진입 종류 판정(`cutoutImagePath == null`)과 `hasRecordedEntrySubject` 플래그는 ViewModel에
남긴다. 앞의 것은 화면이 어떤 인자로 열렸는지의 문제이고, 뒤의 것은 `SavedStateHandle`에 얹혀
프로세스 사망 복원까지 살아남는 것이 존재 이유라 domain으로 옮기면 그 보장을 잃는다.
UseCase는 "초안을 이 알맹이에 맞춘다" 하나만 하고, 부를지 말지는 화면이 정한다.

### 반환 규약

`record`의 `Boolean` 반환(흐름이 열려 있지 않으면 `false`)을 `Result`로 바꾸지 않는다.
세 갈래가 이미 그 `Boolean`으로 분기하고 있고 내는 것도 서로 다르다 —
`SegmentationConfirmViewModel`이 `DraftWriteFailed`와 `DraftMissing`을,
`SegmentationViewModel`은 `SegmentationEffect.ShowError`를 낸다. 특히 뒤엣것은 **화면 이동
자체를 가른다**(`recorded`면 `GoToConfirm`, 아니면 `ShowError`). 반환 타입을 바꾸면 계층을
가르는 이 작업이 오류 처리 설계까지 함께 건드리게 된다. 두 변경을 한 번에 섞지 않는다.

## 오류 처리

지금 동작을 그대로 유지한다.

- `EnsureDraftSubjectRecordedUseCase`가 `false`를 내면 ViewModel이 `reportMissingDraft()`를
  부른다. 표시를 남기지 않아 복원된 화면이 다시 적어 보는 성질도 그대로다.
- `RecordToppingDraftUseCase`가 `false`를 내면 `SegmentationConfirmEffect.DraftWriteFailed`.
- `SegmentationViewModel`의 `record` 두 호출을 감싼 `runSuspendCatching { … }.getOrDefault(false)`
  **래퍼도 호출부에 그대로 둔다.** 이것을 UseCase 안으로 옮기거나 걷어 내면 실패가
  `ShowError`가 아니라 `launch(onError = …)` 경로로 빠져 화면 동작이 달라진다.
- UseCase는 예외를 삼키지 않는다. `launch(onError = …)` 가드도 호출부에 그대로 둔다.

## 테스트

- 신규는 `EnsureDraftSubjectRecordedUseCaseTest` 하나다. 초안이 같은 알맹이를 가리킬 때(적지
  않고 `true`), 다른 것을 가리켜 새로 적을 때(`record` 호출 후 `true`), `record`가 `false`를 낼
  때 세 갈래를 덮는다. `domain/src/test`가 이미 있어 새 하니스를 들이지 않는다.
- 위임만 하는 UseCase 4종은 단독 테스트를 만들지 않는다. 검증할 판단이 없고, 기존 ViewModel
  테스트가 그 경로를 그대로 덮는다.
- 기존 ViewModel 테스트 4파일(`CanvasMainViewModelTest`·`CanvasToppingPlaceViewModelTest`·
  `SegmentationViewModelTest`·`SegmentationConfirmViewModelTest`)은 `ToppingDraftRepository`
  더블을 각 UseCase 더블로 바꾼다. 더블은 MockK을 쓴다. ViewModel 테스트가 이미 UseCase를
  `mockk()`으로 세우고 있고(`GetTutorialVisibleFlowUseCase` 등), 새 UseCase 테스트도 같은 패키지
  선례(`AddToppingUseCaseTest`)를 따른다. domain 유닛 테스트에 Fake를 쓴 파일도 있지만 이번
  변경만 그쪽으로 갈아타면 관례가 갈린다.
- ⚠️ **`SegmentationConfirmViewModelTest`는 단언 대상이 바뀐다.** 지금 그 파일의 여러 테스트가
  화면 상태가 아니라 `record` 상호작용 자체를 단언한다 — "이미 가리키면 안 적는다"와 "다르면 새로
  적는다"가 대표다. 판정이 UseCase로 올라가면 그 둘은 `ensureDraftSubjectRecorded` 호출 하나로
  붕괴해 서로를 가르지 못한다. **그 커버리지는 `EnsureDraftSubjectRecordedUseCaseTest`로
  이관하고**, ViewModel 쪽에는 "판정을 부르는가"와 그 결과에 따른 effect만 남긴다.
  나머지 세 파일은 더블만 갈아끼우면 되고 단언은 그대로다.

## 주의 / 열린 질문

- ⚠️ **UseCase를 지나가는지 컴파일러가 검사하지 않는다.** `feature/*/impl`은 `:domain` 전체를
  보므로 새 ViewModel이 Repository를 다시 직접 주입해도 빌드가 통과한다. 정적 검사로 막는 것은
  이 스펙의 범위 밖이고, 막을지 여부는 후속 판단이다.
- 위임만 하는 UseCase 4종은 계층을 지키는 값 말고는 하는 일이 없다. 도메인 규칙이 나중에 붙는
  자리가 여기라는 것이 이 배치의 전제다.

## as-built

설계대로 들어왔다. UseCase 다섯의 이름·시그니처·패키지가 스펙 표와 같고, `feature/` 아래
`ToppingDraftRepository` 참조는 0건이다. `ToppingDraftRepository` 인터페이스와 `:data` 구현은
손대지 않았다. Gradle 스크립트 변경도 없다.

구현하며 설계와 달라진 것은 테스트 둘이다.

- **판정 실패 테스트의 초안값을 정정했다.** 스펙이 예고한 대로 `EnsureDraftSubjectRecorded`가
  `false`를 낼 때 `DraftMissing`이 나가는지 보는 테스트를 새로 넣었는데, 초안을
  `subjectImagePath = null`로 세우면 `collectDraft()`가 같은 effect를 내서 **판정 갈래를
  지워도 테스트가 통과한다.** 초안을 정상값으로 세워 `DraftMissing`의 출처가 판정 실패
  하나만 남게 고쳤다. 갈래를 잠시 지웠을 때 그 테스트만 실패하는 것을 확인했다.
- **`onEnter_writesNothing`의 단언을 하나 되살렸다.** 판정이 UseCase로 올라가며 단언 대상이
  `ensureDraftSubjectRecorded` 미호출로 바뀌었는데, 그것만으로는 `init`에서
  `recordToppingDraft`를 직접 부르는 회귀를 못 잡는다. `record` 미호출 단언을 함께 둔다.

`SegmentationConfirmViewModelTest`는 17건으로 개수가 같다. 판정 갈래를 가르던 둘이 domain
테스트로 가고, 그 자리에 판정 호출·순서·실패 effect를 보는 셋이 들어왔다. 순서 보장은
"완결되지 않는 `Flow`에 기대는 간접 증명"에서 `coVerifyOrder` 직접 단언으로 바뀌었다.

⚠️ **`CanvasToppingPlaceViewModelTest`에 죽은 스텁이 남았다.** `clearToppingDraft`를
`relaxed = true`로 세워서 `coEvery { clearToppingDraft() } returns Unit` 여덟 자리가 이제
무의미하다. 동작 불변 리팩터의 diff에 무관한 정리를 섞지 않으려고 남겼다.

📌 **머지 뒤 같은 라운드에서 시그니처 둘이 넓어졌다**(2026-09-09, PR #480 `93cb002b5`) —
[topping-upload-source-scaled](2026-09-09-topping-upload-source-scaled.md)가 원본 긴 변을 업로드
경계까지 나르면서 `RecordToppingDraftUseCase#invoke`에 `sourceLongSide: SourceLongSide?`가 붙었고,
`EnsureDraftSubjectRecordedUseCase`는 `record`에 그 자리를 `null`로 적는다(최근 목록에서 되살린
알맹이는 오려낸 사진의 치수를 알 방법이 없다). 위 표의 시그니처는 이 스펙이 머지된 시점의 기록이다.
**이 스펙이 만든 UseCase 층이 그 인자가 얹힌 자리**이기도 하다 — 스펙 「주의」가 예고한 "그 PR이
먼저 머지되면 인자 추가 지점이 한 겹 늘어난다"가 실제로 그렇게 됐다.
