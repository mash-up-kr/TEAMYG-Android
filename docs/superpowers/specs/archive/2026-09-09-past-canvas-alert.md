---
id: past-canvas-alert
title: 지난 캔버스 알럿 — 마감된 캔버스를 처음 확인하는 순간 1회 (Past canvas alert)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-09-10
related_code:
  - PastCanvasAlertRepository
  - PastCanvasAlertRepositoryImpl
  - PastCanvasAlertLocalDataSource
  - PastCanvasAlertLocalDataSourceImpl
  - CanvasMainViewModel#checkShowPastCanvasAlert
  - CanvasMainViewModel#handleClickPastCanvasAlertDate
  - CanvasMainViewModel#findParfaitId
  - CanvasMainEffect.ShowPastCanvasAlert
  - CanvasMainIntent.ClickPastCanvasAlertDate
  - CanvasMainRoute
  - CanvasVO#lastClosedDate
related_adr: ADR-0029
related_spec: c201-canvas-calendar-server, canvas-today-ssot-polling
related_architecture:
  - data-layer.md
  - state-management.md
supersedes:
superseded_by:
tags: [spec, parfait, canvas, alert]
---

# Spec: 지난 캔버스 알럿

> **사후 스펙(as-built)이다.** 이 기능은 선작성 설계 없이 구현이 먼저 들어왔고
> (2026-09-09, PR #477 `8d662fd60`), 이 문서는 머지된 코드를 읽어 정본으로 세운 기록이다.
> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처.

## 목표

03시 회전으로 캔버스가 마감되면, 그 마감을 **처음 확인하는 순간** 캔버스 화면에서 한 번
알린다. 어제 함께 만든 파르페가 완성됐다는 것을 알리고 그 날짜로 바로 보낸다.

## 범위

- 포함
  - 그룹별로 "마지막으로 확인한 마감일"을 기억하는 로컬 저장소.
  - 오늘 캔버스를 받을 때마다 도는 알림 판정과 그 1회 노출.
  - 알럿의 "보러가기"에서 그 날짜의 지난 캔버스로 이동.
- 제외
  - 서버 알림·푸시. 이 알럿은 순수하게 앱이 캔버스 응답을 보고 판정한다.
  - 알럿을 닫고 나중에 다시 보는 수단. 한 마감일에 한 번이다.
  - 캔버스 밖(그룹 목록 등)에서의 노출.

## 동작 / 상태

### 판정 재료는 `CanvasVO.lastClosedDate` 하나다

새 상태를 만들지 않는다. 오늘 캔버스 응답이 이미 나르는 `lastClosedDate`가 **03시 회전으로
마감될 때만 새 값이 되기 때문에**, 그 값의 변화가 곧 "새 마감이 생겼다"이다.

`CanvasMainViewModel`은 오늘 캔버스를 받는 자리에서 `checkShowPastCanvasAlert(canvas)`를
부른다. 폴링이 같은 값을 5초·10초마다 다시 실어 와도 저장된 값과 같으므로 두 번째부터는
조용하다.

### 세 갈래

| 저장된 마지막 확인 마감일 | 응답의 `lastClosedDate` | 동작 |
|---|---|---|
| 같음 | | 아무 일도 하지 않는다 |
| **`null`**(이 기기·이 그룹 조합이 처음) | 있음 | **기준선만 세운다** — `markSeen`만 부르고 알리지 않는다 |
| 다름 | 있음 | 인원 수를 받아 온 뒤 `markSeen` + 알럿 1회 |

**처음 확인을 조용히 넘기는 이유**는 배포 시점 때문이다. 이 기능이 없던 앱을 쓰던 사용자는
이미 지난 마감을 알고 있는데, 기준선이 없으면 업데이트 직후 그 마감이 알럿으로 뜬다.

**마감일이 어제가 아니어도 알린다.** 며칠 앱을 열지 않아 마감일이 여러 날 전으로 갱신됐어도
"그 마감을 처음 확인하는 순간"이면 보여준다.

### `markSeen`을 부르는 시점

알럿을 띄우기로 확정된 뒤(또는 기준선만 세우는 경우)에만 부른다. **인원 수 조회가 실패해
알럿을 못 띄운 경우까지 "봤다"로 남기면 안 된다** — 다음 판정에서 저장값이 이미 같은 값이라
재시도할 계기가 없어져 그 마감을 영영 못 본다.

### 인원 수

문구의 인원 수는 오늘 캔버스의 멤버가 아니라 **그 마감 당시의 참여자**다. `findParfaitId`로
그 날짜의 `parfaitId`를 찾아 `GetParfaitDetailUseCase`로 상세를 받고 그 응답의 `members`
크기를 쓴다. 조회가 실패하면 알럿을 띄우지 않는다.

### "보러가기" 이동

달력 탭(`ClickDate`)과 **다른 경로**를 쓴다. 달력은 이미 그 해를 펼쳐 기록을 받아 둔 상태에서만
눌리므로 `parfaitHistories`(현재 표시 중인 달 기준 연도)로 찾으면 되지만, 이 알럿은 달력과
무관하게 아무 때나 뜬다 — **새해 첫날의 마감일은 작년 12월 31일**일 수 있어 그 해 기록이 아직
없을 수 있다.

그래서 `findParfaitId(date)`가 `date`의 해를 직접 기준으로 삼는다. 이미 받아 둔 해면
`parfaitHistoriesByYear` 캐시에서, 아니면 그 해만 따로 받아 캐시에 남긴 뒤 찾는다. 기록에 없는
날이거나 조회가 실패하면 이동하지 않는다.

이미 그 날짜를 보고 있으면(`date == selectedDate`) 아무 일도 하지 않는다.

## API / 인터페이스

```kotlin
interface PastCanvasAlertRepository {
    suspend fun lastSeenClosedDate(groupId: GroupId): LocalDate?
    suspend fun markSeen(groupId: GroupId, lastClosedDate: LocalDate)
}
```

**같음 비교를 저장소가 대신하지 않는다.** "처음 확인"과 "마감일이 바뀜"을 호출부가 서로 다르게
다뤄야 하기 때문이다(앞은 기준선만 세우고 알리지 않는다). 저장소는 값을 주고 판단은 화면이 한다.

영속은 기존 `DataStore<Preferences>` 한 벌 위에 **그룹마다 별도 키**(`past_canvas_alert_seen_group_`
접두사 + 그룹 id)로 얹는다. 여러 그룹을 오가도 서로의 확인 여부를 덮지 않는다. 값은 `LocalDate`를
문자열로 적고, 읽을 때 파싱 실패는 `null`로 접는다(못 읽는 값은 "확인한 적 없음"과 같게 다룬다).

## 표시·제어 규칙

`YGAlertPolicy` 한 자리를 **환영 배너와 함께 쓴다.** 겹치지 않는다 — 지난 캔버스 알럿은 이
기기·이 그룹 조합을 처음 확인할 때는 절대 뜨지 않고, 환영 배너는 정확히 그 "처음 확인하는
순간"에만 뜬다.

문구는 `feature/groups/canvas/impl`의 `strings.xml`이 든다.

| 자리 | 문구 |
|---|---|
| 제목 | `{M}월 {D}일의 파르페 완성` |
| 부제 | `{N}명의 친구들과 함께했어요` |
| 버튼 | `보러가기` |

## 테스트

`CanvasMainViewModelTest`에 여섯 건이 붙었다 — 첫 확인이 기준선만 세우는 것, 마감일이 바뀌면
알리고 `markSeen`하는 것, 상세 조회 실패가 알림도 `markSeen`도 하지 않는 것, 어제보다 오래된
마감일도 알리는 것, 이미 본 마감일은 다시 알리지 않는 것, 그리고 "보러가기"의 두 갈래(해가 이미
캐시됐을 때·아닐 때). `PastCanvasAlertLocalDataSourceImplTest`가 저장소 왕복과 그룹별 분리를 덮는다.

## 주의 / 열린 질문

- ⚠️ **정책 소스가 없다.** 위키에 이 알럿을 정한 문서가 없어 노출 조건·문구·인원 수의 기준이
  전부 코드에서 확정됐다 → [open-questions](../../../synthesis/open-questions.md) OQ-P-392.
- ⚠️ **판정이 오늘 캔버스를 받을 때마다 돈다.** 상세 조회가 계속 실패하는 상황에서는 폴링
  회차마다 그 조회가 한 번씩 더 나간다. `launch(key = PAST_CANVAS_ALERT_KEY)` 가드가 동시
  중복만 막고 재시도 자체는 막지 않는다 → OQ-P-393.
- ⚠️ **알럿을 놓치면 다시 볼 길이 없다.** 띄우기로 확정한 순간 `markSeen`이 나가므로, 사용자가
  보기 전에 화면이 사라지면 그 마감은 다시 알리지 않는다.
