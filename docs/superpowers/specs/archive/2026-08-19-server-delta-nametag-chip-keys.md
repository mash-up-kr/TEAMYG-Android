---
id: server-delta-nametag-chip-keys
title: 서버 delta 57529ec 반영 — 칩 JSON 키 정정·C-001 상단 칩 결선·업로드 시각 파싱 복구
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-20
related_code: MyParfaitGroupResponse, ParfaitGroupMemberResponse, CreateParfaitGroupResponse, GetTodayParfaitResponse, GroupMemberResponse, PlacedByResponse, PlaceParfaitImageResponse, NametagChipType, CanvasMemberVO, MyParfaitGroupVO, ParfaitGroupMemberVO, CanvasMainViewModel, ColorChipType, GrouptagChipType, PARFAIT_TIME_ZONE, MyParfaitGroupVOMapperTest, ParfaitGroupRemoteDataSourceImplTest, ParfaitRemoteDataSourceImplTest, ParfaitService
related_adr: ADR-0017, ADR-0023, ADR-0024
related_spec: server-delta-nametag-chip-day-boundary, group-ssot, s101-group-setting-api, c001-canvas-today-detail
related_architecture: data-layer, design-system, module-structure
supersedes:
superseded_by:
tags: [spec, parfait, group, canvas, server-contract, design-system]
---

# Spec: 서버 delta 57529ec 반영

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ✅ **develop 머지 완료(2026-08-20, PR #310 `feature/#300-sync-backend-api-250819` → develop `750cc2dd`)** —
> 스택 셋(#307 → #308 → #310)이 순서대로 들어가 develop HEAD가 이 브랜치 팁과 같아졌고 머지 커밋에
> 충돌 해소 편집이 0건이다. 유닛 테스트는 532 → 538건. **이 라운드로 `api/conventions.md`의
> "Android 불일치"가 2건에서 0건이 됐다** — `recentImageUploadedAt` 파싱은 이 브랜치가 고쳤고,
> 하루 경계는 선행 #308이 옮겼다.
>
> ⚙️ **구현 완료 시점 기록(2026-08-19, 브랜치 `feature/#300-sync-backend-api-250819` 위 커밋 8개)** —
> 계획 7 Task + 최종 리뷰 fix 1회가 전부 들어왔다. `testDebugUnitTest :domain:test ktlintCheck
> assembleDebug` 통과. **뒤집힌 결정 0건.** 최종 리뷰(opus)가 서버 원본과 DTO 10개를 키 단위로 대조해
> 전부 일치를 확인했고 "머지 전 필수 수정 없음"으로 판정했다.
> **결정 3·5가 닫은 것** — OQ-P-224(칩 필드 소비)가 세 갈래 모두 해소됐고, `NAMETAG_CHIP_PALETTE`가
> 사라지며 **OQ-P-210 ②(팔레트 7종 근거 없음)도 함께 소멸**했다. 같은 사람이 S-101과 C-001에서 같은
> 색이 된다(서버가 같은 행에서 두 값을 준다 — 최종 리뷰가 서버 코드로 확인).
> **감수한 것 두 가지가 그대로 남는다** — ① 와이어 계약 테스트를 안 붙였다(결정 1의 경고 그대로,
> 이번에도 키 어긋남을 잡은 것은 계약 문서 감사였다 → OQ-P-234 ③, 최종 리뷰가 다음 라운드 최우선으로
> 권했다). ② 파싱 실패가 목록 전체를 죽이는 반경은 손대지 않았다 — 스펙이 실패 처리를 정하지 않아
> 드라이브바이로 굳힐 수 없다고 판단했다 → **OQ-P-237 신규**.
> **실제 중복은 넷이었다**(결정 4는 셋을 셌다) — 색 변환 3벌 외에 `String? → NametagChipType` 매퍼가
> group·parfait 두 곳에 있었다. **넷째는 이후 리뷰 지적으로 걷었다** — `:data` 안에서 닫히는
> 변환이라 색 변환 3벌을 막던 가시성 미결이 여기엔 없었다
> ([architecture/module-structure](../../../architecture/module-structure.md) 참고).
> 드리프트는 이미 시작돼 있었다 — 두 사본의 KDoc이 갈라져 parfait 쪽이 `"DEFAULT"` 를 접지 않는다는
> 계약 한 줄을 처음부터 빠뜨린 채 태어났다.

[2026-08-18-server-delta-nametag-chip-day-boundary](2026-08-18-server-delta-nametag-chip-day-boundary.md)의
**직접 후속**이다. 그 라운드가 "서버 요청 대상"·"범위 밖"으로 미뤄 둔 둘을 이번 서버 delta가 닫아 주었고,
동시에 **그 라운드가 짠 코드를 조용히 무력화하는 변경**을 함께 들여왔다. 작업 대상 브랜치는
`feature/#300-sync-backend-api-250819`(PR #310)이고 선행 라운드 브랜치
`feature/#300-sync-backend-api-250818`(PR #308) 위에 얹혀 있다.

> 🔁 **"선행 라운드를 develop 위로 rebase한 것"이라 적었던 것은 더 이상 사실이 아니다.**
> 2026-08-20에 선행 라운드가 `refactor/#294-group-data-using-ssot`(PR #307) 위로 다시 얹히면서
> 이 브랜치의 base가 재작성됐고, 이 브랜치도 같은 날 새 #308 위로 따라 옮겼다. 그 뒤
> **PR #307·#308이 develop에 머지됐고**(develop `412991ea`) 이 브랜치의 base도 develop으로
> 돌아왔다. ✅ **그 하나도 2026-08-20에 머지됐다**(`750cc2dd`) — 스택 전체가 develop에 있다.
>
> ⚙️ **이후 추가된 것(2026-08-20)** — 코드리뷰 지적을 받아 **모르는 칩 문자열과 값 없음을 모두
> `NametagChipType.DEFAULT`로 접고 이 축의 널 허용을 없앴다**(매퍼 · VO 셋 · 색 변환 셋).
> 결정과 대가는 [ADR-0024](../../../adr/0024-nametag-chip-unknown-fold.md)에 있다 — 이 스펙이 잡아 둔
> "모르는 값은 `null`로 접는다"를 그것이 갱신한다. 서버가 타입을 늘리면 "반납된 자리"와
> 구분되지 않는 것이 그 대가이고, 재검토 트리거를 ADR에 명시했다.

## 서버가 바꾼 것

기준선 `08df1bf` → `57529ec`, 2커밋. **엔드포인트 증감 0**(28 + 테스트 전용 1). 계약 문서 갱신은
[api/server-baseline.md](../../../api/server-baseline.md) 10회차 행에 있고, 여기서는 앱이 반응해야 하는 것만 본다.

| # | 서버 변경 | 앱에 미치는 영향 |
|---|---|---|
| ① | 응답 JSON 키 `nametagChip` → `nameTagChip`, `lastPlacedByNametagChip` → `lastPlacedByNameTagChip` | **선행 라운드가 읽던 값이 전부 `null`이 된다.** 예외는 안 난다 |
| ② | 캔버스 `groupMembers[]`에 `nameTagChip` 신설 | C-001 상단 멤버 칩이 계약 안으로 들어온다 |
| ③ | 토핑 배치 응답 `placedBy`에 `nameTagChip` 신설 + 중첩 타입을 `PlaceParfaitImagePlacedByResponse`로 개명 | 선행 라운드가 "VO에 안 올린다"고 적은 **사유가 사라진다** |
| ④ | 목록·생성의 `recentImageUploadedAt`·`lastPlacedByNameTagChip`이 `COALESCE`로 비널 | **G-001 목록 조회가 상시 실패**한다 |
| ⑤ | 반납 값 `RELEASED` → `DEFAULT`(V15) | 도메인 enum 값·KDoc·두 util 분기가 없는 값을 가리킨다 |
| ⑥ | 그룹 생성 응답에 `recentImageUrl`·`recentImageUploadedAt`·`lastPlacedByNameTagChip` 신설 | 읽는 코드 없음 |
| ⑦ | 과거 목록 `to` 기본값도 `ParfaitDay.current()` | 앱은 항상 범위를 명시해 부르므로 안 물린다 |

**①과 ④가 이 스펙의 이유다.** 나머지는 그 김에 정리하는 것이다.

**앱 영향이 없음을 확인한 서버 변경 셋**(감사 흔적으로 남긴다): 전역 405 신설(`CommonErrorCode.METHOD_NOT_ALLOWED`) —
`AppErrorMapper`에 서버 에러코드 열거가 없어 앱 분기에 닿지 않는다 · 탈퇴 후 재가입 500 수정 — 계약이
아니라 서버 내부 flush 순서다 · Discord 알림 큐 필터 — 서버 운영. ⑦도 여기 속하지만 표에 남긴 이유는
앱 KDoc 하나가 그 기본값을 기술하기 때문이다(결정 7 아래 참고).

### ①이 왜 위험한가

세 DTO의 필드가 전부 `String? = null`이라 키가 어긋나도 `MissingFieldException`이 나지 않는다.
**칩이 전량 폴백 색으로 그려지고 아무 신호도 남지 않는다.** 선행 라운드가 S-101·G-001의 인덱스 순환을
걷어내고 서버 값으로 바꾼 작업이 통째로 무효가 되는데, 화면은 그럴듯하게 뜬다.

이 부류를 앱 테스트가 못 잡는다는 점이 중요하다 — 테스트가 자기 DTO 객체를 자기가 만들어 넣으므로
`@SerialName` 문자열은 어떤 단언도 통과하지 않는다. 이번에도 잡은 것은 계약 문서 감사였다
([open-questions](../../../synthesis/open-questions.md) OQ-P-234).

### ④가 왜 지금 터지나

앱 매퍼는 `recentImageUploadedAt?.let(Instant::parse)`다. 서버 값은 오프셋 없는 로컬 날짜시각이라
`kotlin.time.Instant.parse`가 받지 못한다 — **원래도 틀렸다.** 다만 토핑이 0건인 그룹은 서버가 `null`을
줘서 `?.let`이 파싱을 건너뛰었고, 그래서 "토핑을 한 번도 안 올린 계정"은 우연히 살아 있었다.
서버가 `COALESCE`로 비널화하면서 **그 마지막 안전지대가 사라졌다** — 이제 그룹이 하나라도 있으면
매퍼가 던지고, `ApiCaller`의 transform 가드가 `AppError.Unexpected`로 접어 G-001이 통째로 실패한다.

## 결정

### 1. 키 셋을 서버에 맞추고, DTO는 널 허용을 유지한다

`@SerialName`과 Kotlin 프로퍼티명을 **둘 다** 서버 이름으로 바꾼다. wire DTO는 서버의 거울이라는 규약
([data-layer](../../../architecture/data-layer.md))이 프로퍼티명에도 걸린다 — `@SerialName`만 고치면 코드를
읽는 사람에게 서버 이름이 안 보인다.

**고칠 자리는 정확히 셋이다** — `MyParfaitGroupResponse.lastPlacedByNametagChip`(그룹 목록) ·
`ParfaitGroupMemberResponse.nametagChip`(그룹 상세 멤버) · **`GetTodayParfaitResponse.kt`의
`PlacedByResponse.nametagChip`**(캔버스 토핑 작성자). 세 번째를 빠뜨리기 쉬우니 파일까지 적는다.
앱 `CreateParfaitGroupResponse`에는 아직 이 필드가 없어(결정 7이 새로 넣는다) 이 셋에 안 들어간다.

**개명은 `:data`의 wire DTO에서 멈춘다.** `:domain`은 `NametagChipType`·`MyParfaitGroupVO.lastPlacedByNametagChip`·
`ParfaitGroupMemberVO.nametagChip`의 표기를 **그대로 둔다** — 도메인은 거울이 아니라 제품 언어이고
(서버 `parfait`가 도메인에서 `Canvas`인 것과 같은 축), 매퍼가 그 번역 지점이다. 도메인까지 따라가면
타입 이름 `NametagChipType`도 함께 바꿔야 하는데 그것을 요구하는 계약 변화는 없다. 결과적으로
**`:data` 안에서만 두 표기가 만나고, 만나는 자리는 매퍼 한 곳**이다.

**널 허용(`String? = null`)은 그대로 둔다.** 비널로 좁히면 다음 키 변경이 `MissingFieldException`으로
즉시 드러나지만, 그 대가로 구버전 서버·롤백·스테이징을 만났을 때 화면이 통째로 실패한다
(OQ-P-227이 직전 라운드에서 이미 짚은 자리이고, 그 전제 — 앱과 서버가 항상 같이 배포된다 — 는 여전히
어디에도 적혀 있지 않다).

> ⚠️ **그 대가로 이 부류의 사고를 잡는 수단이 지금 없다.** 와이어 계약 테스트(실제 응답 모양의 JSON
> 문자열을 디코딩해 필드가 채워지는지 단언)를 붙이는 선택지가 있었고 저장소에 선례도 있다
> (`KakaoLoginResponseSerializationTest` — `isNewUser` 정정 때 만든 것). **이번 라운드는 붙이지 않기로
> 했다**(범위 결정). 그래서 다음 서버 키 변경도 `sync-teamyg-server-api` 감사로만 잡힌다 →
> OQ-P-234 ③에 남긴다.

### 2. 업로드 시각은 매퍼가 KST를 부여해 `Instant`로 만든다

VO 타입(`MyParfaitGroupVO.recentImageUploadedAt: Instant?`)과 화면의 경과시간 계산을 **그대로 두고
매퍼만 고친다.**

```
recentImageUploadedAt?.let(LocalDateTime::parse)?.toInstant(PARFAIT_TIME_ZONE)
```

- **왜 KST를 붙일 수 있나** — 서버 DB 커넥션이 dev·local·prod 세 환경 전부 `serverTimezone=Asia/Seoul`이고
  `hibernate.jdbc.time_zone`도 같다. 즉 이 문자열의 벽시계는 KST 기준이라는 것이 계약 사실이다
  ([api/parfait-group.md](../../../api/parfait-group.md) 타임존 절).
- **왜 VO를 `LocalDateTime`으로 안 바꾸나** — 화면이 하는 일이 경과시간 계산이라 절대 시점이 필요하다.
  벽시계를 그대로 들면 기준 시간대를 어딘가에서 다시 정해야 하고, 그 자리가 화면으로 내려가면 해외 기기에서
  숫자가 어긋난다. 기존 주석의 의도("벽시계가 아니라 절대 시점으로 든다")가 옳았고 **틀린 것은 변환 방법뿐**이다.
- **왜 오프셋 유무를 둘 다 받지 않나** — 서버가 오프셋을 안 싣는다는 것이 지금 확정 사실이다. 두 포맷을
  받게 하면 계약이 실제로 어느 쪽인지가 코드에서 흐려진다.
- `PARFAIT_TIME_ZONE`은 `:domain`에 이미 공개돼 있다(`ParfaitDay.kt`). **새 상수를 만들지 않는다** —
  선행 라운드가 `DAY_BOUNDARY_HOUR`에 대해 세운 규율과 같다.

주석은 "왜 KST인가"(위 계약 근거)로 갈아 끼운다. 지금 주석은 결론만 적고 근거가 없어 다음 사람이
같은 실수를 되풀이할 수 있다.

#### `MyParfaitGroupVOMapperTest`를 지운다

**이 파일이 버그를 초록으로 지켜 왔다.** 두 테스트(`uploadedAtWithZuluOffset_isParsedAsThatInstant`·
`uploadedAtWithNumericOffset_isTheSameInstantAsItsUtcForm`)가 **오프셋이 붙은 문자열**을 Given으로 넣고
`Instant` 동치를 단언한다. Given 주석("서버가 UTC 오프셋(`Z`)을 붙여 보낸다")이 매퍼 주석과 똑같은
허구이고, 테스트가 자기 입력을 자기가 만들어 넣으니 **서버가 실제로 무엇을 보내는지와 무관하게 통과한다.**
셋째 테스트(`uploadedAtMissing_isNull`)가 검증하는 `null` 입력은 이제 계약상 발생하지 않는다.

- 결정 2를 적용하면 앞의 둘은 **파싱 예외로 실패한다.** 고쳐서 살리는 선택지는 없다 — 살리려면 다시
  오프셋 있는 입력을 지어내야 하고 그것이 애초의 병이다.
- **파일을 지우고** 커버리지를 `ParfaitGroupRemoteDataSourceImplTest`로 옮긴다. 매퍼 단독 테스트를
  만들지 않는 규약과 일관되고, 이 파일은 그 규약의 마지막 예외였다.
- 선행 스펙이 이 파일을 "규약 예외로 남아 있는 파일이라 늘리지도 손대지도 않는다 · 신규 필드가 널 허용이라
  그대로 컴파일된다"로 park했다. **그 판단은 그 라운드에서는 옳았고 이번 라운드가 뒤집는다** — 그때는
  필드가 늘기만 했고 이번엔 변환 자체가 바뀐다.

### 3. C-001 상단 멤버 칩을 서버 값으로 바꾼다

선행 라운드가 **범위 밖**으로 둔 항목이고 사유는 "서버 `groupMembers`에 필드가 없다" 하나였다.
②가 그것을 닫았다.

- `GetTodayParfaitResponse.GroupMemberResponse`에 `nameTagChip` 신설 → `CanvasMemberVO.nametagChip`으로 올린다.
  **이쪽은 VO까지 올린다** — 읽는 화면(C-001)이 같은 라운드에 있다.
- `CanvasMainViewModel.toMemberChips`가 `member.nametagChip.toColorChipType()`을 쓰고,
  `NAMETAG_CHIP_PALETTE`와 "서버가 이 목록에는 값을 안 준다"는 KDoc을 함께 걷는다.
- **칩 안의 글자는 안 바꾼다** — `Default`가 떠도 `nickname.take(1)` 그대로다(S-101 `GroupMemberList`와
  같다). Figma의 `Default` 변형은 글자가 `-`지만, 여기서 `Default`가 뜨는 것은 **계약이 어긋났다는 뜻**이라
  첫 글자가 오히려 단서가 된다. 선행 라운드가 세운 논리를 그대로 잇는다.

**이것으로 닫히는 것이 둘 더 있다.**
- **같은 사람이 S-101과 C-001에서 다른 색**이던 문제(OQ-P-224 ①). 선행 라운드가 한쪽만 정본으로 만들면서
  드러난 모순이다.
- **팔레트 7종의 근거 없음**(OQ-P-210 ②). 12종 중 7종만 도는 목록이었고 근거가 어디에도 없었는데,
  서버가 타입을 주면 **팔레트라는 개념 자체가 사라진다.** 선행 라운드가 "상단 칩이 계약을 타게 될 때
  같이 정한다"고 미뤄 둔 그 시점이 지금이다.

### 4. 칩→색 변환은 canvas impl에 복제한다

C-001의 규칙은 S-101과 **같다**(12종 1:1, 없으면 `Default`). 선행 라운드는 "S-101(12→12)과 G-001(12→6)은
규칙이 달라 공용 변환을 만들지 않는다"고 적었는데, 이번에 **처음으로 규칙이 같은 두 화면**이 생긴다.

그래도 복제한다. **다만 흔히 드는 두 논거는 여기서 성립하지 않으므로 쓰지 않는다.**

- ❌ **"공용 자리가 없다"가 아니다.** `core:ui`의 `text/LoginProviderUiText.kt`·`text/NameValidResultUiText.kt`가
  "도메인 enum → UI 표현" 매핑의 선례이고, `core:ui → :domain` 간선은 **정확히 같은 이유로 이미 열려 있다**
  (#223, [ADR-0016](../../../adr/0016-domain-result-presentation-string-mapping.md)). `:core:designsystem`은
  `:core:ui`를 모르므로 순환도 없다. 즉 자리는 있고, 새 간선 한 줄이면 된다.
- ❌ **"컴파일러가 막아 준다"도 아니다.** 두 변환이 `else` 없는 exhaustive `when`인 것은 맞지만, 그것이
  잡는 것은 **arm 누락뿐**이고 그 조건은 **앱이 `NametagChipType`에 상수를 더할 때**다. 서버에 13번째
  타입이 생겨도 매퍼(`toNametagChipType`)가 `entries.firstOrNull`로 **모르는 문자열을 `null`로 접으므로
  컴파일은 안 깨진다.** 그리고 진짜 드리프트 위험은 arm 누락이 아니라 **arm 몸통이 갈리는 것**
  (한쪽에서 같은 타입을 다른 색으로 고침)인데 컴파일러는 그것을 전혀 못 잡는다.

**진짜 이유는 하나다 — 그 간선을 여는 것이 이 라운드에 끼울 결정이 아니다.** `core:ui → :domain`이
`implementation`이라 **public 시그니처에 도메인 타입이 노출되는데 의존은 숨어 있는** 상태이고
(소비 feature가 컨벤션 플러그인으로 `:domain`을 직접 갖고 있어 지금 컴파일될 뿐이다), `api` 승격은
저장소에 선언이 0건이고 컨벤션 플러그인에 확장 함수조차 없어 **팀 결정 대상으로 이미 추적 중**이다
([module-structure](../../../architecture/module-structure.md) · [open-questions](../../../synthesis/open-questions.md)
[2026-08-13]). 같은 형태의 매핑을 두 번째로 올리면서 그 미결을 조용히 굳힐 수 없다.

자리는 선행 라운드가 세운 규약대로 그 모듈의 `util` 패키지다 —
`feature/groups/canvas/impl/.../util/ColorChipType.kt`(S-101 것과 파일명·함수명·12갈래가 **글자까지 같다**).

KDoc에 셋을 박는다: ① 이 라운드로 칩 변환이 **세 벌**이 됐고 둘은 규칙이 같다, ② 컴파일러가 잡아 주는
것은 **앱이 enum 상수를 늘릴 때의 arm 누락뿐**이고 색을 한쪽만 바꾸는 것은 못 잡는다, ③ 세 파일이
서로를 가리키는 상호 참조. 기존 두 파일의 KDoc이 서로를 "짝이 되는 변환"(단수)으로 부르고 있으므로
그 문장도 함께 고친다. → OQ에 남긴다.

### 5. 토핑 배치 응답 칩은 DTO까지만 받는다

선행 라운드가 **"서버가 배치 확정 응답엔 칩을 안 준다"**를 사유로 VO 승격을 보류했다.
③이 그 사유를 없앴다 — 이제 두 응답이 모두 칩을 주므로 공유 VO `ToppingPlacerVO`를 채울 때
"없다/모른다"를 뭉갤 일이 없다.

**그럼에도 이번 라운드는 DTO까지만 받는다.** 남은 사유는 하나이고 그것은 여전히 유효하다 —
`placedBy`를 읽는 화면이 0건이다. 소비자 없이 도메인 모양을 굳히면 그 화면이 붙을 때 되돌려야 한다.

- `response/parfaitimage/PlaceParfaitImageResponse.kt`의 `PlacedByResponse`를 서버를 따라
  **`PlaceParfaitImagePlacedByResponse`로 개명**한다(서버·앱 모두 상위 응답과 한 파일에 있는 톱레벨
  클래스다 — 중첩 선언이 아니다). 앱이 두 패키지에 같은 이름을 둔 근거가 **"서버가 그렇다"**였는데
  (그 KDoc이 그렇게 적혀 있다) 서버가 한쪽을 개명해 그 근거가 사라졌다. 거울을 유지한다.
- ⚠️ **캔버스 쪽(`response/parfait/GetTodayParfaitResponse.kt`)의 `PlacedByResponse`는 `클래스 이름`만
  그대로 둔다**(서버가 안 바꿨다). **필드 키는 반드시 `nameTagChip`으로 바꾼다** — 결정 1의 세 자리 중
  하나가 바로 이 클래스다. 두 문장이 붙어 있으니 "이 파일은 손대지 않는다"로 읽지 말 것.

> **C-202 Spotlight(PR #298)가 이 결정에 물리지 않는다.** 선행 라운드가 확인한 대로 그쪽은 칩을
> `placedBy`가 아니라 `groupMembers`에서 `GroupMemberId`로 조인해 찾는데, **결정 3이 그 경로를
> 채워 준다.** 즉 이 보류가 C-202를 막지 않는다.

그래서 정확히 말하면 이 값은 "소비 화면이 생길 때 올린다"가 아니라 **"올릴 계획이 지금 없다"**이다 —
알려진 유일한 소비자가 다른 경로를 쓴다. 계약 대조용으로 DTO에만 두는 상태를 언제까지 둘지가
OQ-P-236 ②의 질문이고, 이 스펙은 그 질문을 닫지 않는다.

### 6. `RELEASED` → `DEFAULT`

도메인 enum 값·KDoc·두 util의 분기·테스트를 함께 바꾼다.

**지금 결과가 맞는 것은 우연이고, 그 우연이 두 겹이다.** ① 키가 어긋나 `"DEFAULT"`는 매퍼에 **도달조차
못 한다**(필드가 통째로 `null`이다). ② 결정 1로 키를 고치면 그제서야 도달하는데, 그때는 `"DEFAULT"`가
매퍼의 "모르는 문자열" 갈래로 빠져 다시 `null`이 된다. 두 경우 다 화면 표현이 반납 값과 같아서 안 드러난다.
우연이 근거가 되면 안 되고, enum·KDoc이 존재하지 않는 계약 값을 가리키는 상태를 남길 수 없다.

KDoc에 서버가 이번에 명시한 성질을 싣는다 — **`DEFAULT`는 `TYPE1`~`TYPE12`와 달리 유일성 제약이 없어
한 그룹 안에서 여럿이 동시에 가질 수 있다.** "값이 없다(`null`)와 뜻이 다르다"는 기존 문장은 그대로 살린다.

**함께 고칠 KDoc은 넷이다**(전부 이번 delta로 거짓이 된 서술이다).

| 자리 | 지금 적힌 것 | 왜 거짓인가 |
|---|---|---|
| `MyParfaitGroupVO.lastPlacedByNametagChip` | "토핑이 하나도 없으면 `null`" | 이제 **생성자의 칩**이 온다 |
| `MyParfaitGroupVO.recentImageUploadedAt` | "오프셋이 붙은 절대 시점" | 오프셋은 서버가 아니라 **앱이 부여**한다(결정 2) |
| `ParfaitGroupMemberVO.nametagChip` | "계약 타입이 널 허용이라" | 서버는 **비널로 좁혔고** 앱만 널 허용을 유지한다(결정 1) |
| 캔버스 `PlacedByResponse` | "서버가 그쪽엔 이 값을 주지 않아서다" | 이제 **준다**(결정 5가 보류 사유를 갈아 끼운다) |

### 7. 그룹 생성 응답 3필드는 DTO에만 받는다

`CreateParfaitGroupResponse`에 필드를 더하되 `CreatedGroupVO`는 건드리지 않는다 — 결정 5와 같은 이유다
(A-005는 생성 직후 목록으로 돌아가며 목록을 다시 읽으므로 소비할 값이 없다).

⚠️ 계약 문서에 적어 둔 함정 하나가 여기 걸린다 — **같은 필드의 출처가 엔드포인트마다 다르다**
(목록은 `parfait_group.created_at`, 생성은 `updatedAt`). 지금은 읽지 않으니 무해하지만, 읽게 되는 날
두 값이 같다고 가정하면 안 된다 → OQ-P-235 ③.

**⑦이 남기는 문서 드리프트 하나를 함께 고친다** — `ParfaitService.getGroupsByGroupIdParfaits`의 KDoc이
서버 기본값을 "`to` = 오늘, `from` = `to` − 30일"로 적는데, 그 "오늘"이 이제 자정이 아니라
`ParfaitDay.current()`(03시 경계)다. 동작은 안 바뀐다(유일한 프로덕션 호출부 `GetParfaitHistoriesUseCase`가
항상 범위를 명시한다) — **기술만 낡았다.** 선행 라운드가 03시 경계를 앱 코드에 새긴 직후라 더 눈에 띈다.

## 화면 반영

| 자리 | 지금(선행 라운드 결과) | 바꿈 |
|---|---|---|
| S-101 멤버 칩 | `member.nametagChip` → 12종 1:1 | **동작은 같고 키만 살아난다**(지금은 `null`이라 전부 `Default`) |
| G-001 그룹 칩 | `lastPlacedByNametagChip` → 짝 묶음 | 키가 살아나고, ⚠️ **토핑 0건 그룹의 색이 바뀐다**(아래) |
| G-001 경과시간 | 매퍼가 던져 **목록 전체 실패** | KST 부여로 복구 |
| C-001 상단 멤버 칩 | `NAMETAG_CHIP_PALETTE[index % 7]` | `member.nametagChip` → 12종 1:1, 없으면 `Default` |

S-101은 코드 변경 없이 키만 고치면 되는데, **G-001은 그렇지 않다.** 서버 `COALESCE`의 두 번째 폴백이
**그룹 생성자의 칩**이라, 토핑이 0건인 그룹의 칩이 중립 `DEFAULT`에서 **생성자 색**으로 바뀐다. 앱 코드는
안 바뀌지만 **화면에 보이는 값이 바뀐다** — "마지막으로 그룹을 바꾼 사람"이라는 칩의 의미가 그 그룹에서는
"만든 사람"이 되는 셈이고, 그것을 앱이 구분할 수단은 `recentImageUrl`이 `null`인지뿐이다(→ OQ-P-235).
이 라운드는 서버가 준 값을 그대로 그린다.

같은 이유로 "선행 라운드의 코드는 옳았고 키만 어긋나 있었다"는 **S-101과 C-001에만** 해당한다.

## 범위 밖

- **DTO 비널화** — 결정 1 참고. 서버가 좁혔지만 앱은 따라가지 않는다.
- **와이어 계약 테스트** — 결정 1의 경고 참고. 붙이지 않기로 한 범위 결정이다.
- **칩→색 변환 공용화 / 새 모듈 간선** — 결정 4 참고.
- **`ToppingPlacerVO` 칩 승격** — 결정 5 참고. 소비 화면이 생길 때.
- **`YGColorChipType.Default`의 색 구분·글자 대비** — OQ-P-226. 디자인 소관이고 PR #298과 같이 정한다.
- **`http/` 요청 모음** — 엔드포인트가 안 늘어 커버 25/27 그대로. 신규 필드는 전부 응답이다.
- **실기기·실서버 확인** — 이 저장소의 모든 계약 라운드와 같이 코드 대조까지다. ④는 실서버를 한 번만
  쏴 보면 즉시 드러났을 종류라는 점은 기록해 둔다.

## 테스트

TDD로 간다. 매퍼 단독 테스트는 만들지 않는다(규약) — 판단이 든 변환은 DataSource 테스트 케이스로.

커버 대상은 **메모리 상태(ViewModel)와 매핑·에러 경계**다. 프레임워크·라이브러리 동작
(`kotlinx.serialization`이 `@SerialName`을 존중하는지 따위)은 대상이 아니다.

| 대상 | 잠글 것 |
|---|---|
| `ParfaitGroupRemoteDataSourceImplTest` | 오프셋 없는 `"2026-08-01T12:00:00"`이 **KST 기준 `Instant`**가 된다 · `"DEFAULT"` · `null` · 미지 문자열 → `null`. **지우는 `MyParfaitGroupVOMapperTest`의 커버리지가 여기로 온다** |
| `ParfaitRemoteDataSourceImplTest` | `groupMembers[].nameTagChip`이 `CanvasMemberVO`로 온다 · 없으면 `null` |
| `ColorChipType`(canvas, 신설) | 12갈래 전부 · `DEFAULT` → `Default` · `null` → `Default` |
| 기존 두 util 테스트 | `RELEASED` 케이스를 `DEFAULT`로 |
| `CanvasMainViewModelTest` | 상단 칩이 **인덱스가 아니라 서버 값**에서 온다 · 칩이 없는 멤버는 `Default` · **멤버 하나가 빠져도 남은 사람 색이 안 밀린다**(선행 인덱스 규칙의 실패 모드를 직접 잠근다) |

시각 파싱은 이 라운드가 고치는 **버그의 본체**라 경계를 함께 잠근다 — 자정 직전·직후 값이 KST 기준
같은 날로 읽히는지. 시간대·자정 넘김은 표준 edge-case 목록에 있는 항목이고, 이번 사고가 정확히 그 부류다.

**널 허용 유지의 대가로 못 잡는 것을 여기 명시한다** — 위 어떤 테스트도 `@SerialName` 문자열이 서버와
같은지는 검증하지 않는다. 전부 DTO 객체를 직접 만들어 넣기 때문이다.

## 열린 질문

- **키 리네임을 계약 문서 감사 말고 잡을 수단이 없다** → OQ-P-234.
- **토핑 0건 그룹이 시각·칩 **둘 다** 거짓말한다** — 시각은 그룹 생성 시각, 칩은 생성자 색이 온다.
  G-001이 활동 0건 그룹에도 경과시간과 사람 색을 그린다. 가르려면 `recentImageUrl`이 `null`인지를 함께
  봐야 한다 → OQ-P-235.
- **같은 규칙의 칩 변환이 세 벌** → 결정 4. 공용화 자리는 있고(`core:ui`), 막는 것은
  `implementation`/`api` 가시성 미결이다.
- **`ToppingPlacerVO` 칩 승격 시점** → 결정 5 · OQ-P-236 ②. 서버 쪽 선행 조건은 이번에 사라졌는데
  **알려진 소비자가 다른 경로를 쓴다** — "언제 올리나"가 아니라 "올릴 일이 있나"가 질문이다.
- **칩 배정 규칙이 정책 문서에 없다** — `DEFAULT`도 정책 밖이다 → OQ-P-223.
