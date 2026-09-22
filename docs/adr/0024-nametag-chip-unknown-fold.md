---
id: ADR-0024
title: 모르는 Nametag 칩 값을 UNKNOWN 센티널 없이 DEFAULT 로 접는다
status: accepted
date: 2026-08-20
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0023
related_spec: server-delta-nametag-chip-keys, group-ssot
related_architecture: data-layer
platforms: android
tags: [adr, parfait, group, mapper, contract]
---

# ADR-0024: 모르는 Nametag 칩 값을 UNKNOWN 센티널 없이 DEFAULT 로 접는다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락

`NametagChipType` 은 서버가 그룹 안에서 배정하는 칩이다. 12종에 더해 그룹을 나간 사람이 반납한
자리를 뜻하는 `DEFAULT` 가 있고, 이 값만 유일성 제약이 없어 한 그룹에 여럿이 가질 수 있다.

서버가 주는 열린 입력이라 앱이 모르는 문자열이 올 수 있고, 그때 조회가 통째로 실패하면 안 된다.
그래서 `toNametagChipType` 은 모르는 값을 `null` 로 접고 그 `null` 이 도메인 VO 셋
(`MyParfaitGroupVO`·`ParfaitGroupMemberVO`·`CanvasMemberVO`)과 화면 색 변환까지 흘렀다.

그런데 서버 계약이 이 필드를 비널로 좁히면서(server-baseline `57529ec`) **VO 에서 `null` 이
뜻하는 바가 "앱이 모르는 값" 하나만 남았다.** 목록 응답의 `lastPlacedByNameTagChip` 은
`COALESCE` 로 채워져 토핑이 0건이어도 생성자 칩이 온다. 즉 "값이 아직 없다"는 상태가 계약에서
사라졌고, 널 허용은 방어 목적으로만 남아 있었다.

## 결정

**모르는 문자열도 값이 없는 경우도 `NametagChipType.DEFAULT` 로 접고, 이 축에서 널 허용을
없앤다.** 매퍼가 non-null 을 돌려주고 VO 셋과 색 변환 셋이 모두 non-null 을 받는다.

- `toNametagChipType(): NametagChipType` — `entries.firstOrNull { it.name == this } ?: DEFAULT`
- VO 셋의 칩 필드에서 `?` 를 걷는다.
- `toColorChipType`·`toGrouptagChipType` 의 수신자를 non-null 로 바꾸고 `DEFAULT, null ->`
  분기를 `DEFAULT ->` 로 좁힌다.

매퍼에서 멈추지 않고 VO 와 화면 변환까지 민 이유는, 데이터가 `null` 을 만들지 못하는데 타입만
널 허용으로 두면 **도달할 수 없는 분기가 남기** 때문이다. 그 분기를 읽는 사람은 오지 않는 경우를
계속 고려하게 된다.

부수 효과로 세 `when` 이 `else` 없이 exhaustive 해졌다. 서버가 13번째 타입을 추가해 enum 에
상수를 더하는 날 컴파일 에러가 그 세 곳에서 난다 — 널 분기가 있던 때는 새 상수가 조용히 중립
색으로 떨어질 수 있었다.

## 대안

- **대안 A: 매퍼만 `DEFAULT` 로 접고 VO 는 널 허용 유지** — 변경 범위가 한 파일로 작다. 그러나
  타입이 허용하는 `null` 을 데이터가 결코 만들지 않는 상태가 되어, 화면 변환의 `null` 가지가
  영원히 죽은 채 남는다. 리뷰어와 다음 구현자가 그 가지를 볼 때마다 "언제 오는가"를 되묻게 된다.
  **→ 기각:** 널을 없애자는 결정의 이유가 정확히 그 되물음을 없애는 것인데 절반만 이룬다.

- **대안 B: `NametagChipType.UNKNOWN` 센티널을 신설해 모르는 값을 그리로 보낸다** — 이 저장소가
  서버 유래 enum 에 이미 쓰는 패턴이다(`LoginProvider`·`ImageStatus`·`CanvasStatus`·`PolicyType`
  모두 `UNKNOWN` 을 두고, `ImageType` 은 "앱이 만드는 값이라 폴백을 두지 않는다"고 예외임을
  KDoc 에 명시한다). 널 제거·비널 VO·화면 동작 동일을 그대로 달성하면서 **"서버가 늘린 새 타입"과
  "반납된 자리"가 구분된 채로 남는** 것이 장점이다. 비용은 상수 하나와 `when` 가지 셋이다.
  **→ 기각:** 팀이 "앱이 모르는 문자열이어도 기본 사양으로 `DEFAULT`"로 합의했고, 지금 두 상태를
  가르는 코드가 하나도 없다(소비처 셋 전부 색 변환만 한다). 다만 이 기각은 **현재 소비 방식에
  기댄 것**이라 아래 "위험·방어"에 재검토 트리거를 남긴다.

## 영향

**긍정**

- 이 축에서 `null` 개념이 사라져 도달 불가 분기가 남지 않는다.
- 세 색 변환이 exhaustive 해져 enum 확장이 컴파일 타임에 잡힌다.
- 화면이 이미 두 값을 같은 중립 색으로 그리므로 사용자에게 보이는 동작 변화가 없다.

**트레이드오프**

- **서버가 타입을 늘리면 그 값이 "반납된 자리"와 구분되지 않는다.** 데이터 레이어에서 두 상태를
  합쳤으므로 위 계층에서 되돌릴 방법이 없다.
- 이 저장소의 다른 서버 유래 enum 이 쓰는 `UNKNOWN` 센티널 패턴에서 이 타입만 벗어난다. 나중에
  누가 패턴을 맞추려 할 때 이 문서를 먼저 읽어야 한다.

**위험·방어**

- 폴백 동작은 `ParfaitGroupRemoteDataSourceImplTest` 가 목록 경로에서 알려진 값 · `"DEFAULT"`
  문자열 · 모르는 문자열 · 필드 없음 네 갈래로, 상세 경로에서 모르는 문자열 한 갈래로 잠근다
  (이 저장소는 매퍼 단독 테스트를 두지 않고 DataSource 테스트로 덮는다).
- **재검토 트리거** — `YGColorChipType.Default` 가 "반납된 자리"와 "모르는 타입"에 다른 표현을
  요구하게 되거나, 서버가 13번째 칩 타입을 실제로 추가하면 이 결정을 다시 본다. 그때의 이행
  경로는 대안 B(센티널 신설)이고, 바뀌는 곳은 매퍼 하나와 색 변환 셋이다.
