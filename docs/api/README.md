# API 계약 문서

서버(`mash-up-kr/TEAMYG-SERVER`)가 제공하는 API 계약의 **스냅샷**과 TJYG-Android의 **적용 상태**를 함께 둡니다.

> **정본은 서버 코드**입니다. 이 디렉토리는 미러이고, 어긋나면 서버가 옳습니다
> (파르페 SoT 우선순위 "코드 > wiki > CLAUDE.md"와 동형).
>
> 추적 브랜치는 서버 **`main`** — 기준 커밋과 갱신 절차는 [server-baseline.md](server-baseline.md).

## 전역 계약
- [conventions.md](conventions.md) — 응답 envelope(**204 예외 3건**)·성공/에러 코드 체계(전역 405 포함)·인증·URL 규약·직렬화 규약·**전송**(2026-08-25 — 서버가 HTTPS 도메인으로 옮겨 가고 평문 포트는 닫힌다)·**Android 불일치**(2026-09-08 기준 **4건** — 그룹 목록 `recentImageUrl`이 오늘 캔버스로 좁혀졌는데 앱은 "토핑 0건"으로 읽는다 · 푸시 `date` 미소비 · 푸시 알림 id가 재시도마다 갈린다 · 토핑 `borderWidth` 를 앱이 2~30dp 로 가둔다)

## 팀 명세 원문
- [spec/](spec/README.md) — 서버팀이 작성한 **API 명세**를 텍스트로 옮긴 것. 이 디렉토리의 도메인 문서가
  **코드의 미러**라면 `spec/`은 **팀이 합의한 의도**입니다. 코드에서 읽을 수 없는 계약(클라이언트 측 책임,
  값의 생성 주체), 명세에만 있는 미구현 항목, 값의 의미가 여기 있습니다. 각 명세 문서는 `## 코드 대조`
  절에서 **일치 / 코드에만 / 명세에만**을 갈라 적습니다.
  - [spec/auth-kakao-login.md](spec/auth-kakao-login.md) — 카카오 로그인/회원가입
  - [spec/auth-signup.md](spec/auth-signup.md) — 회원가입 완료(약관동의)
  - [spec/auth-reissue.md](spec/auth-reissue.md) — 토큰 재발급
  - [spec/auth-logout.md](spec/auth-logout.md) — 로그아웃

## 도메인 계약
| 문서 | 서버 위치 | 엔드포인트 | Android |
|---|---|---|---|
| [auth.md](auth.md) | `http/auth` | 5 (카카오 로그인 · **애플 로그인** · 회원가입 완료 · 토큰 재발급 · 로그아웃) | **결선됨**(애플 해당 없음, 나머지 4 전부 호출부 있음) |
| [policy.md](policy.md) | `http/auth` | 1 (현재 유효 약관 목록) | 구현됨 |
| [parfait-group.md](parfait-group.md) | `http/parfaitgroup` | 8 (목록 · 상세 · 참여 미리보기 · 참여 · 생성 · 닉네임 변경 · 탈퇴 · 신고) | **결선됨**(8 전부 호출부 있음, **불일치 1건** — 목록 `recentImageUrl`의 뜻) |
| [parfait.md](parfait.md) | `http/parfait` | 5 + 테스트 전용 1 (연도 리스트 · 오늘의 캔버스 · 과거 목록 · **상세 조회** · **배경 변경** / 테스트 회전) | **결선됨**(회전 해당 없음, 5 전부 호출부 있음, 불일치 0건 — 2026-09-07 신설 `groupName`을 앱이 이튿날 읽기 시작했다) |
| [image.md](image.md) | `http/image` | 2 (업로드 URL 발급 · 업로드 확인) | **결선됨**(2 전부 호출부 있음) |
| [member.md](member.md) | `http/member` | 3 (내 계정 조회 · 전역 닉네임 변경 · **탈퇴**) | **결선됨**(3 전부 호출부 있음) |
| [parfait-image.md](parfait-image.md) | `http/parfaitimage` | 5 (토핑 배치 확정 · 위치/크기/각도 수정 · **일괄 수정** · **테두리 수정** · **삭제**) | 구현됨(**네 갈래 결선됨 그대로** — 배치 확정·삭제·**일괄 수정**·테두리 수정 / **위치/크기/각도 단건 수정은 표면 없음**(2026-08-31 걷어냄), **불일치 1건** — 앱이 읽는 쪽에서 `borderWidth` 를 2~30dp 로 가둔다) |
| [notification.md](notification.md) | `http/notification` | 1 (**기기 FCM 토큰 등록**) + **HTTP 밖 푸시 발송 3종**(토핑 등록 · **오전 리마인드** · **저녁 리마인드**) | **구현됨·결선됨**(PR #450 — 등록을 **세션 축 넷**에서 부른다. 수신·채널·딥링크는 PR #446·#447. ✅ 리마인드 2종은 앱이 먼저 만들어 둔 `REMIND_AM`·`REMIND_PM`·`route=group`과 **문자열이 맞는다**. ⚠️ 푸시 `data`의 `date` 미사용·중복 알림 두 건은 잔존) |

**총 30 엔드포인트 + 테스트 전용 1**(2026-09-11, 서버 `82e6edc` — 여덟 번째 도메인 `notification`이
2026-09-02에 들어와 29 → 30이 됐고, **이번 회차의 엔드포인트 증감도 0이다**).

⚠️ **2026-09-11 — 엔드포인트·에러 코드는 그대로이고, 값 규칙 하나와 응답 필드 셋이 바뀌었다.**
① `fix: 그룹명 검증에 한글 자음/모음 단독 입력 허용`(PR #137)이 `GroupName`에 자모 범위를 넣었다. 닉네임
둘은 2026-08-15에 이미 받았는데 그룹명만 빠져 있었고, **이 디렉토리는 그동안 그룹명도 받는다고 잘못 적어
왔다** — 그 기간에 앱은 자모 그룹명을 통과시켜 서버에서만 400 `INVALID_GROUP_NAME`이 났다. 이제 서버 값
객체 셋과 앱이 같은 집합이다. ② `fix: 그룹 목록 응답에 최신 사진 테두리 정보 추가`(PR #139)가 목록 응답에
`recentImageBorderType`·`recentImageBorderColor`·`recentImageBorderWidth`(모두 널 허용)를 더했다.
세 필드는 `recentImageUrl`과 같은 오늘 캔버스 토핑을 가리킨다. 앱은 아직 안 읽지만 `ignoreUnknownKeys`라
불일치는 아니고, OQ-P-316 ①이 서버 쪽에서 닫혔다 → [parfait-group.md](parfait-group.md). 신규 OQ 0건.
📌 **같은 날 앱 데이터 계층이 읽기 시작했다**(PR #496 develop 머지) — `MyParfaitGroupVO.recentImageBorder`로
접는다. G-001 렌더는 별도 티켓이라 아래 도메인 표의 Android 열은 그대로다(목록의 `⚠️불일치`는
`recentImageUrl`의 뜻 때문이다).

⚠️ **2026-09-10 — 엔드포인트도 DTO도 에러 코드도 안 바뀌었는데 권한 경계가 움직였다.**
`fix: 그룹 탈퇴 후 재참여가 불가능하던 문제 해결`이 멤버십 판정에 `leftAt IS NULL`을 넣었다. 그 결과
① **탈퇴 이력만 있는 회원의 재참여가 열렸다**(직전까지 409 `GROUP_ALREADY_JOINED`로 막혀 있었다) —
참여는 새 행을 만들지 않고 **기존 멤버십 행을 `rejoin`으로 재활성화**하며 그룹 닉네임을 전역 닉네임으로
초기화하고 칩을 다시 뽑는다(→ [parfait-group.md](parfait-group.md)). ② **탈퇴한 회원이 캔버스 다섯 경로**
(연도·오늘·과거·상세 조회와 배경 변경)**에서 403 `GROUP_NOT_JOINED`로 막힌다** — 직전까지는 탈퇴 행이
남아 있는 것만으로 통과했다(→ [parfait.md](parfait.md)). ③ **멤버십 id가 유지되므로 과거 토핑의 귀속과
소유권이 재참여로 되살아난다**(→ [parfait-image.md](parfait-image.md)). 신규 OQ 1건(OQ-P-398).

⚠️ **2026-09-08 — 엔드포인트는 그대로인데 두 축이 함께 움직였다.** ① `[Feat/#128]`이 **데일리 리마인드
2종**을 붙여 서버→앱 단방향 푸시가 **1종 → 3종**이 됐다. 시각 트리거(10:00·20:00 KST)에 전 이용자
대상이고, Outbox를 쓰지 않아 **재시도도 중복도 없는 대신 실패가 로그로만 남는다.** 토핑 알림 제목도
`체리 얹을 타이밍!` → `체리 하나 톡!`으로 바뀌었다 → [notification.md](notification.md).
② `9c13852`가 캔버스 오늘·상세 응답에 **`groupName`(비널)**을 더했다 — 푸시로 그룹 id만 쥐고 들어와도
상단에 그릴 이름을 얻으라는 것이 근거다. ✅ **앱이 이튿날 그 필드를 받았다**(2026-09-08, PR #469) —
다만 **그룹명의 정본은 그룹 목록 캐시로 남았고**, 캔버스가 준 이름은 그 캐시가 빌 때만 상단 바를
채운다. 서버가 없애 주려던 왕복은 그래서 그대로 남는다(OQ-P-383 ④는 404 경로만 잔존)
→ [parfait.md](parfait.md).

⚠️ **2026-09-04 — 엔드포인트를 하나도 안 늘리고 계약이 가장 크게 늘어난 회차다.** `[Feat/#127]`이
**FCM 발송 인프라와 토핑 등록 알림을 붙여 서버가 실제로 푸시를 보내기 시작했다.** HTTP 표면(컨트롤러·
DTO·에러 코드·`SecurityConfig`·envelope)은 한 글자도 안 바뀌었지만, **서버→앱 단방향 계약이 새로
생겼다** — 문구·`data` 키 4종·Android 채널 id·TTL·재시도·중복 수신 가능성이 전부 앱이 맞춰야 할
항목이다. **이 표의 엔드포인트 개수로는 보이지 않는 변화**라 → [notification.md](notification.md).
**Android 표면은 28/29, 공백 1이다**(2026-09-03 갱신) —
분모에서 애플 로그인 1(`해당 없음`)을 뺀 값이 29이고, 테스트 전용 회전 1은 총계에서 이미 분리했다.
✅ **기기 FCM 토큰 등록에 표면이 생겼다**(PR #437 `7019a550`) — **서버 표면이 먼저 열린 첫 도메인**이었는데
결국 여기서도 앱이 하루 만에 따라붙었다. 다만 **표면뿐이고 부를 수단이 없다** — FCM 토큰을 얻는 심볼이
develop에 0건이라(2026-08-22 PR #325가 걷어냈다) 되살릴지가 그대로 미결이다
→ [open-questions](../synthesis/open-questions.md) OQ-P-341.
⚠️ **2026-09-04부터 그 공백의 값이 달라졌다** — 서버가 **실제로 푸시를 보내고 있고 받을 앱이 없다.**
등록된 토큰이 0건이라 발송은 전부 취소되지만, 앱이 등록을 시작하는 순간 문구·`data` 키·채널 id가
곧바로 구속력을 갖는다 → [notification.md](notification.md).
✅ **받을 앱이 2026-09-05에 생겼다**(PR #446 딥링크 · #447 FCM 수신부) — 채널 id는 서버가 못 박은
`parfait_default`를 앱이 그대로 따랐다. ~~다만 등록 호출부는 여전히 0건~~ → ✅ **같은 날 PR #450이
그 한 줄을 이었다** — 등록을 **세션 축 넷**(로그인·가입·앱 진입의 성공 분기 + `onNewToken`)에서 부르고,
알림 권한 안내도 A-004·A-005 완료 직후에 붙었다. 즉 이 도메인은 **양끝과 가운데가 다 서 있고**,
남은 것은 실행 확인이다 — 실서버·실기기 확인이 0회라 발송이 `NO_DEVICE_TOKEN`을 벗어났는지 확인된 바
없다 → [notification.md](notification.md) "기기 토큰 등록 결선"·"푸시 수신·딥링크".
**남은 공백 1은 위치/크기/각도 단건 수정 PATCH**로, 표면이 있었다가 소비처를 잃어 2026-08-31에 걷어낸
**닫힌 결정에 가까운 공백**이다(소비처가 일괄 PATCH로 옮겨 탔다).

> **`구현됨`은 `:data`에 Service·DataSource 표면이 있고 계약과 일치한다는 뜻**이다(2026-08-06, PR #197
> develop 머지).
>
> ✅ **2026-08-15 — 소비처가 생겼다.** 다섯 라운드(PR #241·#242·#243·#244·#248)가 **8 엔드포인트를
> 화면까지** 이었다 — 카카오 로그인·회원가입(auth), 약관 목록(policy), 그룹 목록·생성·참여 미리보기·참여·
> 닉네임 변경(parfait-group). `policy.md`는 유일한 엔드포인트가 전부 소비돼 **`android_status: done`**이고
(2026-08-18 PR #296으로 소비 화면이 온보딩·설정 둘이 됐다 — 응답의 `title`·`url`이 웹뷰 목적지 인자로 나간다),
> `parfait-group.md`(상세·탈퇴·신고 미소비)는 `partial`이다. 나머지 넷
> (parfait·image·member·parfait-image)은 여전히 표면만 있고 소비처가 0이다(**member는 2026-08-16에 닫혔다** — 아래).
>
> ✅ **2026-08-15 — `auth.md`가 `done`이 됐다**(PR #260). `reissue`는 `TokenAuthenticator`가, `logout`은
> `AuthRepository.logout()` → `LogoutUseCase` → S-001 앱 설정이 소비한다. 애플을 뺀 4 엔드포인트 전부가
> 호출부를 가지므로 **소비처를 얻은 엔드포인트는 10건**이 됐다
> ([스펙](../superpowers/specs/archive/2026-08-15-session-token-refresh-infra.md)).
>
> ✅ **2026-08-16 — `member.md`에 첫 소비처가 생겼다**(PR #263). `GET /api/v1/users/me`와
> `PATCH /api/v1/users/me/nickname`이 `MemberRepository` → UseCase 3종 → S-001·S-002·스플래시
> 부트스트랩까지 이어졌다. **소비처를 얻은 엔드포인트는 12건**이고, 표면만 있고 소비처가 0인 도메인은
> **셋**(parfait·image·parfait-image)으로 줄었다. `member.md`는 **탈퇴만 미소비**라 `partial` 그대로다
> ([스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md)).
>
> ⚠️ **2026-08-16 — `parfait.md`에 표면을 우회하는 소비자가 생겼다**(PR #259). C-201 캘린더의 UseCase
> 둘이 파르페 조회 두 엔드포인트를 KDoc으로 가리키면서 remote DataSource를 안 쓰고 mock을 만든다.
> `android_status`는 `partial` 그대로다(소비처가 계약을 타지 않는다) → [parfait.md](parfait.md) Android 매핑.
> **실서버 요청 검증은 아직 0건**(실기기 미수행) → [open-questions](../synthesis/open-questions.md).
>
> `image.md`(2026-08-10 신설) · `member.md`·`parfait-image.md`(2026-08-11 신설)도 **2026-08-12 PR #230
> 머지로 표면을 얻어** `android_status: partial`이 됐다 — 앞의 넷과 같은 뜻이다(표면은 있고 소비처는 없다).
>
> ✅ **카카오 로그인 판별자 키 불일치는 해소됐다**(2026-08-15, PR #241) — `@SerialName("isNewUser")` 정정 +
> 와이어 계약 테스트([auth.md](auth.md) "판별자 키"). 같은 라운드가 이 엔드포인트를 **소비처까지** 이었다.
>
> ✅ **2026-08-15(PR #250) — 표면 공백이 다시 0이 됐다.** 서버 delta가 벌린 신규 5건(파르페 오늘·과거,
> 토핑 테두리·삭제, 회원 탈퇴)이 Service·remote DataSource·domain VO까지 한 라운드에 들어왔다
> ([spec](../superpowers/specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md)).
> `parfait`·`member`·`parfait-image` 세 도메인의 Android 열이 전부 `구현됨`이 됐고, `android_status`는
> 셋 다 `partial` 그대로다 — **소비처가 0건**이기 때문이다(`done`은 화면까지 이어졌을 때 쓴다).
>
> ⚠️ **새 불일치 1건**(2026-08-15) — 그룹 목록의 `recentImageUploadedAt`을 앱이 오프셋 필수 파서로 읽는데
> 서버는 오프셋 없이 내려준다. 대응 심볼이 있는데 계약과 어긋나므로 `parfait-group.md`의 GET
> `/api/parfait-groups` 행이 **`⚠️불일치`**였다([conventions.md](conventions.md) "Android 불일치").
> ✅ **2026-08-20에 닫혔다**(PR #310) — 매퍼가 `LocalDateTime::parse` + `toInstant(PARFAIT_TIME_ZONE)`로
> 바뀌어 그 행은 `구현됨·결선됨`이 됐다.
>
> 🔁 **2026-08-15 2차 서버 delta(`e4ff23f`) — 엔드포인트 증감 0, 규칙 변경 3건.** 초대코드 자릿수 8 → 6
> (앱은 처음부터 6이라 **드러나지 않던 불일치가 서버 쪽에서 닫혔다**), 닉네임 정규식에 자모 허용 추가,
> 그룹 내 닉네임 중복 검사 제거로 `GROUP_NICKNAME_ALREADY_USED` 삭제. 셋 다 요청/응답 형태는 그대로라
> Android 열 값은 바뀌지 않는다.
> ✅ **뒤의 둘은 같은 날 PR #250이 앱에 반영했다** — `CheckNameValidUseCase`가 자모 범위를 얻어 집합이
> 다시 같아졌고, `ALREADY_USED` 계열(상수·enum·문구·분기)은 걷혔다. **남은 것은 정책 문서 공백**이다
> → [open-questions](../synthesis/open-questions.md).
>
> 🔁 **2026-08-16 서버 delta(`22717fe`) — 파르페 상세 조회·배경 변경 2건이 들어와 공백이 다시 벌어졌다**
> (26 → 28, 표면 25/27). **가장 큰 의미는 배경 쓰기 경로다** — C-301 배경 편집이 고른 값을 버리던 이유의
> 서버 절반(`background_type`·`background_value`에 쓰는 API 부재)이 닫혔다. 상세 조회는 응답이
> `GetTodayParfaitResponse` **재사용**이라 앱 DTO·VO·매퍼가 이미 있고 Service·DataSource 함수만 붙이면
> 된다 → [parfait.md](parfait.md).
>
> ✅ **2026-08-16 — 그 공백이 같은 날 닫혔다**(PR #266). 서버 delta와 앱 대응이 하루 안에 붙은 첫 사례라
> **표면 왕복이 가장 짧게 끝났다**(직전 라운드는 서버 `36ecd1c` → PR #250까지 벌어져 있었다). 배경 변경은
> 이 도메인 첫 쓰기 경로여서 **첫 요청 DTO**와 쓰기 전용 도메인 모델 `CanvasBackgroundEdit`이 함께 들어왔다 —
> 이미지 배경이 **쓸 때 `imageId`·읽을 때 URL**이라 읽기 모델을 되돌려 보낼 수 없기 때문이다.
> `android_status`는 `partial` 그대로다(**소비처가 여전히 0건**) → [parfait.md](parfait.md) Android 매핑.
>
> ✅ **2026-08-17 — `parfait.md`에 첫 소비처가 생겼다**(PR #268). `ParfaitRepository` → UseCase 둘 →
> C-001 캔버스 메인이 **오늘 조회·과거 목록·상세** 셋을 소비한다(연도 조회·배경 변경은 미소비).
> **소비처를 얻은 엔드포인트는 15건**이고, 표면만 있고 소비처가 0인 도메인은 **둘**(image·parfait-image)로
> 줄었다. `android_status`는 **`partial` 그대로**다. 같은 도메인의 **표면 우회 소비자(캘린더 mock
> UseCase 둘)는 그대로**라, 이제 한 화면 안에서 캔버스 조회는 계약을 타고 달력 조회는 안 탄다
> → [parfait.md](parfait.md) Android 매핑 ·
> [스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md).
>
> ✅ **2026-08-17 — 표면 우회 소비자가 사라졌다**(PR #279). C-201 캘린더의 UseCase 둘이 mock을 버리고
> `ParfaitRepository`를 타면서 **연도 조회까지 소비처를 얻었다** — 이 도메인에서 미소비로 남은 것은
> **배경 변경 하나**다. **소비처를 얻은 엔드포인트는 16건**이고 `android_status`는 `partial` 그대로다.
> 2026-08-16에 열렸던 "소비자가 표면을 우회한다"는 상태가 **하루 만에 닫혔다**
> → [parfait.md](parfait.md) Android 매핑 ·
> [스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md).
>
> ✅ **2026-08-17 — `parfait-group.md`가 `done`이 됐다**(PR #285·#287). S-101 그룹 설정이 상세 조회·
> 나가기·신고를 소비해 **8 엔드포인트 전부 호출부를 얻었다**(닉네임 변경은 S-102와 공용). Repository에
> 남겨 뒀던 세 갈래가 "화면이 요구할 때 올린다"는 방침대로 이때 올라왔다. **소비처를 얻은 엔드포인트는
> 19건**이다. `done`은 소비 여부만 뜻한다 — 목록의 `recentImageUploadedAt` 파싱 불일치는 **그대로**다
> ([conventions.md](conventions.md) "Android 불일치") → [parfait-group.md](parfait-group.md) Android 매핑 ·
> [스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md).
>
> 🔁 **2026-08-18 서버 delta(`08df1bf`) — 엔드포인트 증감 0인데 응답이 넓어지고 "오늘"이 바뀌었다.**
> ① **Nametag-Chip 부여 주체가 서버가 됐다** — 그룹 참여·생성 시 그룹 안에서 겹치지 않는 타입을 뽑고
> 탈퇴 시 `RELEASED`로 반납한다. 응답 필드 셋이 늘었다(그룹 상세 `members[].nametagChip`, 그룹 목록
> `lastPlacedByNametagChip`, 캔버스 `placedBy.nametagChip`).
> 🔁 **이 문단의 필드 키와 반납 값 이름은 2026-08-19에 바뀌었다 — 아래 항목이 정본이다.**
> **앱은 세 화면이 각자 인덱스로 색을
> 돌리고 있어** 그 규칙을 버릴 수 있게 됐다 → [parfait-group.md](parfait-group.md) "Nametag-Chip 배정 규칙".
> ② **그룹 상세가 `groupName`·`memberLimit`을 싣는다** — 앱이 목록을 한 번 더 읽어 이름을 붙이던 조합과
> "N명 남음" mock 1이 **둘 다 서버에서 닫혔다**(OQ-P-139·OQ-P-216).
> ③ ⚠️ **"오늘"이 자정이 아니라 03시 경계가 됐다**(`ParfaitDay`). 앱은 자정 기준이라 00:00~03:00 KST에
> 오늘 조회가 두 번 돌고 화면이 D−1 캔버스를 D 아래 그린다 — **불일치 2건째**
> → [parfait.md](parfait.md) "하루 경계".
> 새로 읽어야 할 필드가 넷이지만 **`http/` 요청 모음의 커버는 그대로 25/27**이다(엔드포인트가 안 늘었다).
>
> 🔁 **2026-08-19 서버 delta(`57529ec`) — 직전 라운드의 뒷정리 둘이 들어왔고, 그 과정에서 JSON 키가 바뀌었다.**
> ① **Nametag-Chip 정합성** — `groupMembers[].nameTagChip`(C-001 상단 멤버 칩이 계약 안으로)·토핑 배치 응답
> `placedBy.nameTagChip`이 더해지고, 목록의 `recentImageUploadedAt`·`lastPlacedByNameTagChip`이
> `COALESCE`로 **비널**이 됐다(토핑 0건 그룹은 생성 시각·생성자 칩). 반납 값 이름이 `RELEASED` → `DEFAULT`.
> 그룹 **생성** 응답도 목록의 세 필드를 얻었다.
> ⚠️ **응답 JSON 키가 `nametagChip` → `nameTagChip`, `lastPlacedByNametagChip` → `lastPlacedByNameTagChip`으로
> 바뀌었다**(서버 코어 프로퍼티명은 그대로, HTTP DTO 경계에서만) — develop은 이 필드를 안 읽어 무해하지만
> 그 필드를 옛 키로 읽던 코드는 값이 조용히 `null`이 됐다(✅ 2026-08-20 PR #310 머지로 정정).
> ② **하루 경계가 서버 안에서 통일됐다** — 과거 목록의 `to` 기본값도 `ParfaitDay.current()`가 됐다.
> ✅ 앱과의 불일치도 2026-08-20에 닫혔다(PR #308이 `parfaitToday()`를 03시로 옮겼다).
> ③ **전역 405가 생겼다**(`CommonErrorCode.METHOD_NOT_ALLOWED`) — 그전에는 메서드 불일치가 500이었다.
> ④ **탈퇴 후 재가입 500 수정**(`provider_user_id` tombstone rename이 flush를 못 타던 버그,
> [member.md](member.md)). 엔드포인트·화이트리스트·`ApiResponse`는 불변이고 **표면 셈도 27/27·25/27 그대로**다.
>
> ✅ **2026-08-19 — `member.md`가 `done`이 됐다**(PR #306). S-001 앱 설정의 탈퇴 확인이
> `WithdrawUseCase`를 불러 **3 엔드포인트 전부 호출부를 얻었다**. **소비처를 얻은 엔드포인트는 20건**
> 이고, 표면만 있고 소비처가 0인 도메인은 여전히 **둘**(image·parfait-image)이다. 이 도메인에 남는
> 물음은 소비 여부가 아니라 **성공 뒤 정리 경로**다 — 탈퇴 직후의 로그아웃 요청이 죽은 토큰으로 나가
> 재발급·강제 로그아웃까지 깨운다 → [member.md](member.md) Android 매핑 ·
> [open-questions](../synthesis/open-questions.md) OQ-P-242.

> 🔁 **2026-08-20 서버 delta(`efbf98f`) — 엔드포인트도 필드도 안 늘고 실패 경로만 늘었다.**
> 쓰기 다섯 경로(배경 변경 + 토핑 배치·수정·테두리·삭제)가 대상 캔버스의 `status`를 읽어 `ACTIVE`가
> 아니면 **409 `PARFAIT_ALREADY_CLOSED`**로 거부한다. 그전에는 03시 회전 직후 마감된 캔버스에 쓰기가
> **200으로 성공하고도** 뒤이은 `today` 조회가 새 캔버스를 줘서 편집이 사라진 것처럼 보였다.
> **"마감 후 편집을 서버가 막을지 앱 책임으로 둘지"라는 두 라운드 묵은 물음에 서버가 답한 것**이고,
> 그 김에 토핑 수정·테두리·삭제 셋이 파르페 존재·그룹 소속 검사(404 `PARFAIT_NOT_FOUND`)를 처음 얻었다
> → [parfait.md](parfait.md) · [parfait-image.md](parfait-image.md).
> ⚠️ **앱은 이 코드를 모른다** — `ServerErrorCode`에 상수가 없고, 다섯 경로 전부 소비처가 0건이라
> 지금은 도달하지 않는다. 대신 **"서버가 마감 캔버스를 막지 않는다"고 단정한 앱 주석 일곱 곳이
> 거짓이 됐다**(요청·응답 형태는 그대로여서 `⚠️불일치`는 아니다)
> → [open-questions](../synthesis/open-questions.md).
>
> ✅ **2026-08-20 — 계약 delta 없이 Android 쪽만 크게 움직였다**(PR #307·#308·#310 develop 머지).
> 서버 기준선은 `57529ec` 그대로이고 **엔드포인트·화이트리스트·envelope 모두 불변**이다. 바뀐 것은
> 앱이 그 계약을 얼마나 읽는가다 — ① **"Android 불일치"가 2건에서 0건이 됐다**(`recentImageUploadedAt`
> 파싱 · 하루 경계 03시), ② 2026-08-18~19 delta가 더한 필드 대부분이 화면까지 닿았다
> (`groupName`·`memberLimit`·칩 세 자리 중 둘), ③ 그룹 목록·상세 **읽기가 `Flow` 구독으로 바뀌었다**
> (엔드포인트는 그대로, 응답을 두는 자리만 `:data` 캐시로 이동 — [ADR-0023](../adr/0023-group-in-memory-ssot.md)).
> **`android_status`는 어느 도메인도 바뀌지 않았다** — 소비처 셈이 그대로이기 때문이다
> (`parfait-group` `done` · `parfait`·`parfait-image` `partial`). 남은 미소비 필드는 둘이고 성격이 다르다:
> `placedBy.nameTagChip`은 **읽는 화면이 없어 DTO에서 멈춰 세운 것**, 그룹 생성 응답 3필드는
> **DTO 거울만 두고 VO를 안 늘린 것**이다.
>
> ✅ **2026-08-20 — 두 도메인이 처음으로 DataSource 위층을 얻었다**(PR #322 develop 머지, 계약 delta
> 없음). `image`는 `ImageUploadRepository`(발급 → **S3 PUT** → 확인 3단계를 하나로), `parfait-image`는
> `ToppingRepository.place` + `AddToppingUseCase`다. **`android_status`는 둘 다 `partial` 그대로다** —
> 이 저장소의 셈은 "화면이 부르는가"이고 그 위가 아직 없다(결선은 C-106 PR5).
> S3 PUT은 우리 서버 계약이 아니라 발급 응답이 준 URL로 나가므로 엔드포인트 셈(27/27)에 안 들어간다.
> ⚠️ 그 경로가 **Retrofit 밖 raw OkHttp라 `@NoAuth` 판정을 못 받는다** — 전용 `@UploadClient`가
> 성능이 아니라 **기능 전제**인 이유이고, 같은 클라이언트가 로깅 인터셉터를 아예 달지 않는다
> (presigned URL은 쿼리 스트링이 곧 자격증명이다) → [image.md](image.md).
> `placedBy.nameTagChip`은 **읽는 화면이 생긴 뒤에도 여전히 DTO에서 멈춰 있다** — C-202 Spotlight
> (PR #298)가 그 필드 대신 `groupMembers` 조인으로 색을 정해서다
> → [open-questions](../synthesis/open-questions.md) OQ-P-251.
>
> ✅ **2026-08-26 — 캔버스 응답이 소유 판정을 싣는다**(서버 PR #115, 엔드포인트 증감 0). 오늘·상세
> 두 조회의 `images[].placedBy`에 `ownerType`(`ME`·`OTHER`)이 붙어 **"이 토핑이 내 것인가"가 계약
> 안으로 들어왔다**. 그전까지 앱에는 견줄 상대가 없어 C-202의 본인 갈래가 비어 있었다
> (OQ-P-250 ① 해소). ⚠️ **요청자마다 값이 달라지는 첫 필드**라 이 응답은 사용자 사이에 공유·캐시할
> 수 없다. ✅ **develop이 같은 날 읽기 시작했다**(PR #376) — 매퍼가 `"ME"`만 참으로 접어
> `CanvasToppingVO.isMine`을 만들고, C-202의 상수 `false`와 C-301 편집 탭의 축이 다른 비교가 함께
> 사라졌다. **계약 delta와 앱 반영이 같은 날 붙은 첫 사례**다(그전까지는 며칠씩 벌어졌다)
> → [parfait.md](parfait.md).
>
> ✅ **2026-08-22 — 앱이 처음으로 서버에 무언가를 만든다**(PR #334 develop 머지, 계약 delta 없음).
> C-106 결선 스택 넷이 한 머지로 들어오면서 발급 → **S3 PUT** → confirm → 배치 네 단계가 확인 버튼
> 하나에 걸렸다. **`image.md`가 `done`이 됐다**(2/2 소비) — 표면만 있고 소비처가 0인 도메인은 이로써
> **0개**가 됐고, `parfait-image.md`는 배치 하나만 소비돼 `partial` 그대로다(나머지 셋은 C-301 라운드).
> **소비처를 얻은 엔드포인트는 23건**이다. 그전까지 소비되던 것이 전부 읽기였으므로 **쓰기 경로가
> 사용자 조작에 걸린 것도 이번이 처음**이다 — S3 PUT은 발급 응답이 준 URL로 나가므로 엔드포인트
> 셈(27/27)에는 들어가지 않는다.
> ⚠️ **실서버 요청 검증은 여전히 0건**(실기기 미수행)이고, 실패하면 서버에 흔적이 남는다(고아
> `PENDING` 이미지·S3 객체) → [open-questions](../synthesis/open-questions.md) OQ-P-146.
> 발급 응답 본문에 실려 오던 presigned URL은 `@NoBodyLog` + `SelectiveLoggingInterceptor`로 로그에서
> 뺐다(그 URL은 쿼리 스트링이 곧 자격증명이다) → [image.md](image.md).
>
> ✅ **2026-08-22 — `parfait.md`가 `done`이 됐다**(PR #329 develop 머지, 계약 delta 없음).
> C-301 배경 편집의 확인 버튼이 **배경 변경 PATCH**를 부르면서 이 도메인의 마지막 미소비 갈래가
> 닫혔다(회전 제외 5/5). **소비처를 얻은 엔드포인트는 24건**이고, `partial`로 남은 도메인은
> **둘**(`parfait-group.md`·`parfait-image.md`)이다. 같은 라운드가 `image.md`의 `imageType`에
> **두 번째 값**을 실었다 — 배경 이미지가 `BACKGROUND`로 올라간다(그전까지는 `NUKKI` 하나뿐).
> ⚠️ 앱이 서버에 쓰는 두 번째 경로인데 **실기기·실서버 확인은 여전히 0회**이고, 마감된 캔버스가
> 돌려주는 409는 화면에서 일반 오류로 접힌다
> → [open-questions](../synthesis/open-questions.md) OQ-P-146·OQ-P-261.
>
> ✅ **2026-08-23 — 앱이 서버 데이터를 지우는 첫 경로가 생겼다**(PR #335 develop 머지, 계약 delta
> 없음). C-301 편집 탭의 삭제 확인 모달이 **토핑 삭제 DELETE**를 부르면서 `parfait-image.md`의
> 미소비 셋이 **둘**(위치·테두리 수정)로 줄었다. **소비처를 얻은 엔드포인트는 25건**이고 `partial`
> 도메인은 여전히 **둘**이다(`parfait-group.md`·`parfait-image.md`). ⚠️ **실패가 화면에 닿지
> 않는다** — 403·409·404가 전부 로그 한 줄로 접혀, 같은 화면의 배경 저장과 처분이 갈렸다
> → [parfait-image.md](parfait-image.md) Android 매핑 ·
> [open-questions](../synthesis/open-questions.md) OQ-P-270.
>
> ✅ **2026-08-23 — 편집 결과가 서버에 남기 시작했다**(PR #336 develop 머지, 계약 delta 없음).
> C-301 편집 탭의 **확인 버튼**이 바뀐 토핑만 골라 **위치 PATCH**를 부르면서 `parfait-image.md`의
> 미소비가 **하나**(테두리 수정)로 줄었다. **소비처를 얻은 엔드포인트는 26건**이고 `partial`
> 도메인은 여전히 **둘**이다(`parfait-group.md`·`parfait-image.md`). 부분 병합 계약을 앱이 실제로
> 활용한 첫 사례다 — `positionZ`를 널로 두어 겹침 순서를 서버 값으로 남긴다.
> ⚠️ **실패 처분이 같은 버튼 안에서 갈렸다** — 배경 실패는 토스트 + 화면 잔류, 토핑 실패는 로그
> 한 줄 + 화면 이동이라 **사용자가 성공했다고 믿는다**
> → [parfait-image.md](parfait-image.md) Android 매핑 ·
> [open-questions](../synthesis/open-questions.md) OQ-P-275.

> ⚠️ **2026-08-25 — 계약은 그대로인데 붙는 주소가 바뀐다**(서버 #112·#113 `main` 머지, 계약 파일
> 변경 0건). 앞단 리버스 프록시가 TLS를 종단해 서버가 **HTTPS 도메인**을 얻었고, 검증 뒤 **평문
> 포트를 닫는 단계**가 런북 절차에 있다. 엔드포인트·필드·에러 코드는 한 글자도 안 바뀌었지만
> **모든 도메인이 같은 전제 위에 있다** — 차단되는 순간 기존 `YG_BASE_URL`로 빌드된 앱은 전부
> 연결에 실패한다. 그 시점은 1회성 인프라 조작이라 서버 커밋에서 읽을 수 없다
> → [conventions.md](conventions.md) "전송" ·
> [open-questions](../synthesis/open-questions.md) OQ-P-302·OQ-P-076.

> ✅ **2026-08-27 — 마지막 미소비 엔드포인트가 닫혔다**(PR #369 develop 머지, 계약 delta 없음).
> C-301 편집 탭의 확인 버튼이 **테두리 PATCH**까지 부르면서 `parfait-image.md`가 **`done`**이 됐다
> (4/4 소비). **소비처를 얻은 엔드포인트는 27건**이고, `partial`로 남은 도메인은
> **하나**(`parfait-group.md`)다. 앱이 테두리를 **겹 목록**으로 들고 서버가 **한 겹**을 받는
> 모양 차이는 `CanvasBGEditViewModel.toToppingBorder`가 접는데, **마지막 겹**을 보내는 그 규칙이
> 같은 화면의 **첫 겹**을 그리는 렌더링과 어긋난다
> → [parfait-image.md](parfait-image.md) Android 매핑 ·
> [open-questions](../synthesis/open-questions.md) OQ-P-324.
> ⚠️ **실서버 확인은 여전히 0회**이고, 테두리 저장 실패도 앞선 두 갈래와 같이 로그 한 줄로 접힌다
> (OQ-P-146·OQ-P-275).

> 🔁 **2026-09-02 서버 delta(`0c59af9`) — 여덟 번째 도메인이 생겼고, 앱이 처음으로 뒤처진 채 시작한다.**
> `[Feat/#125]`가 **기기(FCM) 토큰 등록 `POST /api/v1/notifications/devices`** 하나를 더해 29 → 30이 됐다
> ([notification.md](notification.md)). ① **등록은 `token`을 유일 키로 하는 upsert**라, 같은 기기에서
> 계정을 바꿔 로그인하면 이전 매핑이 별도 호출 없이 끊긴다 — 그래서 **삭제 엔드포인트가 없다.**
> ② 삭제는 다른 두 도메인의 부수 효과로만 일어난다 — **로그아웃은 그 세션이 등록한 행**,
> **탈퇴는 그 회원의 전 행**이다([auth.md](auth.md)·[member.md](member.md)).
> ③ 그 세션 단위 삭제를 위해 **access token이 `sessionId` 클레임을 싣기 시작했다** — `validateAccessToken`의
> 반환이 `AccessTokenClaims`로 바뀌었지만 **와이어 계약은 한 글자도 안 바뀌었다**(토큰 문자열 안의 내용만
> 늘었다) → [conventions.md](conventions.md) "인증".
> ④ **204·envelope 없음이 셋이 됐다**(로그아웃·탈퇴·기기 토큰 등록). ⑤ 신설 에러 코드는 **0종**이다 —
> 실패가 전부 전역 계약(400 `INVALID_REQUEST`·401 4종)이다. `SecurityConfig` 화이트리스트는 불변이라
> 신규 1건도 인증 대상이다.
> ⚠️ **앱에는 대응 심볼이 하나도 없는데, 그 이유가 이번엔 다르다** — 앱은 FCM 수신 서비스와 토큰 조회를
> 갖고 있다가 **2026-08-22 PR #325로 걷어냈고**, 걷어낸 근거가 정확히 **"보낼 서버가 없다"**였다
> (`onNewToken`이 `TODO`인 채여서 결선된 적 없는 기능 때문에 첫 실행마다 알림 권한을 물었다 —
> [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 철회 정정). **이번 delta가 그 전제를 뒤집는다.**
> 다만 서버도 **발송 인프라·알림 트리거를 범위 밖으로 명시**해 무엇을 언제 보내는지는 여전히 계약에
> 없다 → [open-questions](../synthesis/open-questions.md).
> 🔁 **이 마지막 줄은 다음 회차(`aa9cc9b`)에 뒤집혔다** — 아래 2026-09-04 문단을 본다.
> ⚠️ **`sessionId`가 널인 채 등록된 행은 로그아웃이 못 지운다**(삭제 조건이 `memberId` + `sessionId`다).
> 클레임 과도기를 위해 컬럼을 널 허용으로 둔 대가다 → [notification.md](notification.md).

> 🔁 **2026-09-04 서버 delta(`aa9cc9b`) — 엔드포인트는 그대로인데 서버가 푸시를 보내기 시작했다.**
> `[Feat/#127]`이 **FCM 발송 인프라 + 토핑 등록 알림**을 붙였다. **HTTP 표면 변화는 0**이다 —
> 컨트롤러·요청 DTO·에러 코드·`SecurityConfig`·`ApiResponse`·`GlobalExceptionHandler` 전부 불변이고
> 총계도 30 그대로다. **그런데 이 디렉토리가 지금까지 다룬 적 없는 종류의 계약이 생겼다** —
> HTTP 왕복이 아니라 **서버가 앱으로 밀어 넣는 단방향 메시지**다 → [notification.md](notification.md).
> ① **트리거는 신규 토핑 배치 하나뿐**이다(`PlaceParfaitImageService.place()`의 `existing == null` 분기).
> **재배치는 알림이 나가지 않고**, 수신자는 그룹 구성원에서 **나간 사람과 작성자 본인을 뺀** 나머지다.
> ② **페이로드가 계약이다** — 제목·본문 문구, `data` 키 **`type`·`route`·`groupId`·`date`** 네 개(값은
> 전부 문자열), TTL 6시간, **Android 채널 id `parfait_default`**, APNs `apns-priority`·sound.
> ⚠️ **채널 id는 앱이 만들어야 하는 쪽**이라, 안 맞추면 **서버는 발송 성공인데 사용자는 아무것도 못 본다.**
> ③ **최소 한 번(at-least-once) 보장이라 중복 수신이 가능하다** — 앱이 견뎌야 한다. 지연은 폴링(기본 2초)
> + 커밋 직후 깨우기라 정상 경로는 거의 즉시고, 실패는 1분→5분→15분→1시간→6시간 최대 5회 재시도 뒤
> **조용히 버려진다**(사용자·클라이언트 통지 없음).
> ④ **기기 토큰이 지워지는 경로가 셋이 됐다** — 로그아웃·탈퇴에 더해 **발송이 죽은 토큰을 발견하면
> 그 행을 회수한다**(`UNREGISTERED` 등).
> ⑤ 발송 직전에 조건을 **다시 검사**한다 — 그룹 삭제·수신자 탈퇴·수신자 토큰 0건이면 취소하고,
> **작성자가 나갔으면 익명 문구로 치환**한다. 즉 알림은 올린 순간이 아니라 **보내는 순간의 상태**를 담는다.
> ⑥ 새 테이블 `notification_outbox`(`V18`) — FK 없는 큐, `dedup_key` 유니크, 종료 행은 매일 04:00 KST에
> 7일 지난 것부터 삭제.
> ⚠️ **커밋 제목은 "3종 알림 트리거"라고 적지만 코드에 연결된 트리거는 1종**이다 — 캔버스 마감·초대 등의
> 발송 코드가 없다(OQ-P-343).
> ⚠️ **지금은 앱이 토큰을 등록하지 않아 전부 `NO_DEVICE_TOKEN`으로 취소되므로 동작 영향이 0**이지만,
> 앱이 등록을 시작하는 순간부터 위 ②③이 곧바로 구속력을 갖는다 → OQ-P-351·352·353 신설.

테스트 전용 회전 엔드포인트(`POST /api/v1/test/parfait-canvas/rotate`)는 인증 없이 전 그룹 캔버스를
마감·재생성하며 서버가 프로덕션 오픈 전 제거를 예고했다 — 문서상 위치는 [parfait.md](parfait.md)지만
**총계에서 분리해 센다**(앱이 붙을 대상이 아니다).

`auth.md`와 `policy.md`는 서버 모듈이 같고(`http/auth`, OpenAPI 태그도 둘 다 `Auth`) URL 세그먼트가
다르다(`/api/v1/auth/*` vs `/api/v1/policies`). 파일명 규약이 서버 패키지가 아니라 경로 기준이라
문서를 나눴다 — 아래 [규약](#규약) 참고. 반대로 `member.md`·`parfait-image.md`는 경로(`users`,
그룹 하위 `images`)가 아니라 **서버 도메인 이름**을 따랐다(사유는 [conventions.md](conventions.md) "URL 규약").

## 규약
- **파일명에 날짜 접두사를 붙이지 않습니다.** `specs/`·`plans/`와 달리 API 계약은 `architecture/`와 같은
  **살아있는 문서**입니다 — 서버가 바뀌면 같은 파일을 갱신하고, 판본은 frontmatter `server_commit`·`verified`가 기록합니다.
- 도메인 파일명은 **서버 URL 세그먼트** 기준입니다(`/api/parfait-groups` → `parfait-group.md`).
  소비자는 서버 패키지가 아니라 경로로 API를 찾기 때문입니다.
- 형식 권위 출처는 [template.md](template.md). 새 도메인 문서는 위 인덱스 표에 한 줄 등록합니다.
- 엔드포인트 표의 **Android 열**은 네 값입니다.
  - `미구현` — 대응 심볼이 없다(**아직** 없는 것 — 붙일 예정이다)
  - `구현됨` — 대응 심볼이 있고 계약과 일치한다
  - `⚠️불일치` — 대응 심볼이 **있는데** 계약과 어긋난다(사유 각주 필수)
  - `해당 없음` — 서버에 있으나 **Android가 쓰지 않기로 결정**한 엔드포인트(결정 근거 필수).
    `미구현`과 구분합니다 — 전자는 공백이고 이쪽은 닫힌 결정이라, 표면 개수를 셀 때 분모에서 뺍니다.
- 파르페 공통 규율: **라인번호·변동 수치·색 hex 금지**. 근거는 파일명 + 심볼명.
- **팀 명세를 도메인 문서에 직접 섞지 않습니다.** 명세 원문은 [spec/](spec/README.md)에 두고, 도메인
  문서에는 코드로 확인되는 사실 + **명세 델타** 한 줄만 둡니다. 섞으면 어느 근거로 적힌 문장인지
  구분이 사라집니다. 명세 원문에는 **개인 식별 정보(작성자·코멘트)를 옮기지 않습니다** — public repo입니다.

## 갱신
- **서버가 바뀌었을 때** → 스킬 `sync-teamyg-server-api`(계약 절 갱신 + 기준선 갱신)
- **Android가 바뀌었을 때** → 스킬 `sync-tjyg-develop-baseline`(`android_status`·Android 매핑 절 갱신)

## 계약을 실제로 확인하는 법

TJYG-Android 저장소의 **`http/` 디렉토리**에 IntelliJ HTTP Client 요청 모음이 있다 — develop 기준
`auth.http`·`policy.http`·`parfait-group.http`·`parfait.http`·`images.http`·`users.http`·
`parfait-image.http`·`notifications.http`·`health.http`·`_reset.http` + `http-client.env.json` +
사용법 `README.md`다. 여기 문서에 적힌 계약을 서버에 직접 쏴서 확인할 수 있다.

**같은 디렉토리의 `fcm-test.http`는 성격이 다르다** — 우리 서버가 아니라 **FCM v1 API로 직접** 나가고,
서버가 만드는 것과 같은 모양의 푸시를 손으로 쏴서 앱 수신을 확인하는 자리다. 서버 엔드포인트가 아니라
아래 커버 셈의 분자·분모 어디에도 들어가지 않는다.

> ✅ **커버가 다시 전량이다(2026-08-15, PR #250).** 서버 delta로 20/25가 됐던 것이 같은 라운드의 `http/`
> 보강으로 **25/25**가 됐다 — `parfait.http`에 오늘·과거 조회, `parfait-image.http`에 테두리 수정·삭제,
> `users.http`에 탈퇴가 붙었고 `http-client.env.json`·`_reset.http`에 `parfait_id`가 등재됐다.
> **손으로 메우는 방식이 서버 delta마다 무너졌다 복구되는 것이 네 번째**다 — 갱신 경로가 둘이라는 구조는
> 그대로다 → [open-questions](../synthesis/open-questions.md).
>
> 📌 **2026-08-16 서버 delta로 다시 25/27이 됐다** — `parfait.http`에 상세 조회·배경 변경 요청이 없다.
> **다섯 번째 왕복**이다.
>
> 📌 **2026-08-31 서버 delta로 25/28이 됐다** — `parfait-image.http`에 **토핑 일괄 수정 PATCH** 요청이
> 없다. **여섯 번째 왕복**이고, 이번에는 `:data` 표면도 함께 비어 있다(요청 모음만 뒤처진 2026-08-16과
> 다르다).
>
> ✅ **`:data` 표면은 같은 날 닫혔지만 `http/`는 그대로 25/28로 남았다**(2026-08-31 두 번째 라운드,
> 브랜치 `feature/#427-sync-backend-api-260831` → **2026-09-01 develop 머지, PR #428 `e870fb87`**). 일괄 PATCH가 Service·DataSource·Repository·UseCase까지
> 올라와 위 도메인 표의 공백은 닫혔다 — **두 표면이 처음으로 같이 뒤처졌다가 한쪽만 먼저 닫힌
> 사례**다. `http/parfait-image.http`에 일괄 요청을 넣지 않은 것은 이 라운드의 결정이다 — 그
> 모음은 손으로 쏴서 계약을 확인하는 자리인데 이번 라운드에 실서버 요청 계획 자체가 없어 채워도
> 아무도 돌리지 않는다. 채우는 것은 실서버 검증을 하는 라운드의 일이다.
> ⚠️ **그래서 결과가 뒤집혔다** — 이 파일의 PATCH 항목(3·4·7·8번)은 전부 앱이 걷어낸 **단건
> 경로**를 친다. 요청 모음은 이제 앱에 표면이 없는 엔드포인트만 붙들고 있고, 앱이 실제로 쓰는
> 변형 저장 경로(일괄 PATCH)는 실서버 검증이 **0건**이다.
>
> 📌 **2026-09-02 서버 delta로 25/29가 됐다** — `notifications.http`가 아예 없다(기기 토큰 등록).
> **일곱 번째 왕복**이고, 이번 공백은 앞의 여섯과 성격이 다르다 — 앞선 왕복은 전부 **앱이 곧 쓸**
> 엔드포인트가 요청 모음에서 빠진 것이었는데, 이번 것은 **앱이 2026-08-22에 걷어낸 축**의 엔드포인트다
> (PR #325). 다만 이 모음은 손으로 쏴서 계약을 확인하는 자리이므로, **앱 표면과 무관하게 지금도
> 검증할 수 있는 대상**이다 — 등록이 반복 호출을 전제로 설계돼 있어 여러 번 쏴도 upsert로 수렴하고,
> 지우려면 로그아웃을 부르면 된다(`auth.http`에 이미 있다).
>
> ✅ **일곱 번째 왕복이 닫혔다 — 26/29(2026-09-04, PR #451 `2b1dce3a`).** `notifications.http`가
> 신설돼 기기 토큰 등록을 덮고, `http-client.env.json`·`_reset.http`·`README.md`도 같은 PR에서
> 함께 갱신됐다(`fcm_project_id`·`fcm_access_token`·`fcm_device_token` 세 변수는 응답에서 뽑는 값이
> 아니라 **손으로 채우는 값**이라 `_reset.http`의 비우기 목록에는 넣지 않는다). ⚠️ **이번에도 사람이
> 손으로 메웠고**, 앞선 여섯 번과 달리 **요청 모음이 앱 코드보다 앞서 나갔다** — 등록 엔드포인트를
> 부를 수단(FCM 토큰 취득)이 develop에 여전히 0건이므로 이 파일은 앱 없이 손으로만 돌릴 수 있다
> → [open-questions](../synthesis/open-questions.md) OQ-P-108 · OQ-P-341.
>
> ⚠️ **같은 PR이 서버 계약의 복제면을 하나 더 늘렸다.** `fcm-test.http`가 서버 발송 페이로드
> (문구·`data` 키 4종·채널 id·TTL·APNs 헤더)를 **상수로 옮겨 적었다.** 엔드포인트 커버와 달리
> **이 복제는 세는 축이 없어** 서버가 값을 바꿔도 아무 셈에도 안 잡힌다
> → [open-questions](../synthesis/open-questions.md) OQ-P-354.
>
> ⚠️ **이번엔 왕복이 반만 닫혔다(2026-08-16, PR #266)** — 같은 두 엔드포인트에 **`:data` 표면은 붙었는데
> `http/`는 그대로 25/27이다. 앞선 네 번은 표면과 요청 모음이 한 라운드에서 함께 메워졌다** — 두 표면이
> 갈린 첫 사례다. 배경 변경은 손으로 쏴 볼 값이 특히 많다(HEX 형식·조건부 필수·업로드 확인 상태)
> → [open-questions](../synthesis/open-questions.md).
>
> ⚠️ **파괴적 요청이 두 파일의 마지막에 있다** — `users.http`의 탈퇴, `parfait-image.http`의 토핑 삭제.
> 파일을 위에서부터 통째로 돌리면 계정·데이터가 지워진다(`http/README.md`가 이 경고를 담는다).
>
> ⚠️ **`http/auth.http`가 아직 `newUser`로 분기한다.** 앱 DTO와 `http/README.md`는 `isNewUser`로
> 정정됐는데(PR #241·#230) 이 파일만 남았다 → [open-questions](../synthesis/open-questions.md).

- 로그인 응답에서 토큰을 자동 추출해 다음 요청이 그대로 쓴다 — 스웨거에서 복붙할 필요가 없다
- 각 요청 주석에 이 문서들의 함정을 옮겨 뒀다(`reissue`에 `Authorization`을 붙이면 재발급이 막히는 건은
  주석 처리된 헤더 줄을 풀어 **직접 재현**할 수 있다)
- 서버 주소·토큰은 gitignore된 `http-client.private.env.json`에만 둔다(커밋되는 `http-client.env.json`은 빈 값 골격)
- 런타임 global 변수가 env 파일보다 **우선**한다 — 토큰을 손으로 넣을 때는 `_reset.http`로 먼저 비운다

> 이 모음과 `api/`의 도메인 문서는 **같은 서버 코드를 근거로 하는 두 표면**이다. 서버가 바뀌면 양쪽이
> 같이 갱신돼야 하고, 한쪽만 고치면 조용히 갈린다 → [open-questions](../synthesis/open-questions.md).

문서와 서버가 어긋나는 것 같으면 여기서 먼저 쏴 보는 게 빠르다.
