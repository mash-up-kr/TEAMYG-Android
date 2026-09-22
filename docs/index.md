# Parfait 구현 문서 — 에이전트 진입 허브

> 이 문서 트리는 2026-09-22에 `team-yg-pesonal-agent`(`parfait/`)에서 복사됐다.
> 기준 커밋 `de9f5f5`. 원본은 그 저장소에 그대로 남아 있고 자동 동기화는 없다 —
> 이후의 정본은 이쪽이다.

> 세션 시작·작업 전 **이 파일부터** 읽어라. 여기서 "무엇을 찾으면 어디를 보라"로 라우팅한 뒤, 필요한 문서만 펼친다 (전체를 읽지 말 것).

## 지금 상태 (1줄)
Android 단일 플랫폼, Jetpack Compose + Navigation3. 다중 모듈(core/data/domain/feature)·컨벤션 플러그인·Hilt·자체 MVI 기반. 원격 네트워크 기초 구조(컨벤션 플러그인·NetworkModule·ApiResponse/safeApiCall·remote 예시)가 **develop 머지**됨(#174), 실제 API 연동은 후속(ADR-0017). 화면은 G-001 목록(#222로 실패 화면·pull-to-refresh·A-005 이동까지)·C-101 카메라 플로우·C-001 캔버스 메인(#199 — 반응형 배치·Dot Grid 배경·토핑 추가 메뉴, ~~진입 경로 0건~~ → **#268로 G-001에서 진입**)까지 들어왔고 전부 **데이터·후속 화면 미결선** 상태다. **토핑 생성 경로는 이어졌다**(#221) — C-101-confirm "다음"이 C-103 누끼 추출로 결선되고 확인(C-103)·수동 편집(C-104)·테두리 편집(C-105)이 한 라운드에 들어와 캔버스 배치(C-106) 직전까지 닿는다. **C-106 화면도 들어왔고**(#290) 그 확인 버튼이 **서버까지 이어졌다** — 스택 PR 여섯 중 1·2가 #322로, 3~6이 #334로 들어와 **토핑이 실제로 올라간다**([스펙](superpowers/specs/archive/2026-08-20-c106-topping-place-api.md)). 확인을 누르면 발급 → **S3 PUT** → confirm → 배치 네 단계가 돌고, 테두리는 굽지 않고 서버 필드로 가며(ADR-0025), 흐름 상태는 DataStore 초안 한 벌이 나른다(ADR-0026). 배치에 성공한 알맹이는 갤러리 "최근"에 남아 다시 쓰인다. **캔버스 토핑에 첫 상호작용이 생겼다**(#298) — 타인 토핑을 탭하면 Spotlight + 작성자 토스트다. **캔버스가 밖으로 나가고 안의 것이 지워지기 시작했다**(#324·#335) — "갤러리에 저장"이 `GraphicsLayer` 캡처 → `MediaStore` 쓰기로 실물화됐고(#413·#414로 그 진입점이 지난 캔버스 메뉴에서 **날짜바 아이콘**이 되어 오늘 캔버스에서도 저장하고, #445로 저장 전에 **무엇이 저장될지 보여 주는 미리보기 화면**을 거친다)([스펙](superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)), C-301 편집 탭의 삭제 확인 모달이 **앱이 서버 데이터를 지우는 첫 경로**가 됐다. ⚠️ 삭제만 즉시 영구인데 실패는 로그 한 줄이다(OQ-P-270). ~~다만 네 화면의 닫기가 전부 빈 람다라 **플로우를 나갈 출구가 없다**.~~ → ✅ **출구가 생겼다**(#309) — 닫기가 `Navigator.popUpTo<T>()`로 캔버스까지 되감고, 배경 편집에서 들어온 경로만 부른 화면으로 돌아간다. **캔버스 편집 갈래도 열렸다**(#231) — C-001 편집 버튼이 C-301 배경 편집으로 이어지고 카메라·갤러리·확인 세 화면을 토핑 생성 플로우와 공유하지만(NavKey `returnResultOnly` 인자로 분기), ~~고른 배경이 저장·반영되지 않는다~~
→ ✅ **#329로 저장된다**(색은 HEX, 사진은 업로드 후 `imageId`로 PATCH하고 돌아간 캔버스가 재조회로 그린다)(~~C-001 진입 경로 0건이라 이 갈래 전체가 도달 불가~~ → #268로 진입이 열려 함께 도달 가능해졌다). 앱 진입 체인은 Splash→Login→TermAgree→GroupList로 이어졌고(#220), 그 첫 화면 A-002 로그인이 온보딩 일러스트 3장으로 실물화됐고(#218) **인증까지 결선됐다**(#241). 그룹 생성(A-005)·참여(A-004→S-102) 두 갈래도 확인 모달을 거쳐 닫혔고(#224), **#411로 종착지가 목록에서 C-001 캔버스가 되어 위키 정본과 맞았다**(`replaceAll(목록)` + `goTo(캔버스)`, 환영 배너 1회). 디자인시스템은 Figma 바 3종(Top Bar Canvas·List-Date·Floating Bar)과 배경 블러(Haze, ADR-0018)까지 머지됐다(#188).
서버 계약은 `api/`에 스냅샷돼 있다(도메인 8건·**엔드포인트 30개 + 테스트 전용 1**, 서버 `82e6edc`).
**2026-09-08 — 엔드포인트는 그대로인데 계약이 둘 늘었다**: 서버→앱 푸시가 **1종 → 3종**(데일리 리마인드
`REMIND_AM`·`REMIND_PM`, 10:00·20:00 KST)이 됐고, 캔버스 오늘·상세 응답에 **`groupName`(비널)**이 붙었다
(앱은 아직 안 읽는다 — OQ-P-383).
**2026-09-10 — 엔드포인트도 DTO도 그대로인데 권한 경계가 움직였다**: 멤버십 판정에 `leftAt IS NULL`이
들어가 **그룹 재참여가 열리고**(기존 멤버십 행을 되살리므로 과거 토핑의 귀속·소유권도 함께 돌아온다)
**탈퇴한 회원은 캔버스 다섯 경로에서 403**이 된다(OQ-P-398).
⚠️ **2026-09-02 — 여덟 번째 도메인 `notification`이 서버에만 열렸다**(기기 FCM 토큰 등록 1건).
**2026-08-22에 FCM 축을 걷어낸 근거가 이걸로 사라졌다** — 그때 이유가 "토큰을 보낼 서버가 없다"였고
(PR #325, ADR-0013 철회 정정) 이제 보낼 자리가 생겼다.
⚠️ **2026-09-04 — 서버가 실제로 푸시를 보내기 시작했고, 받을 앱이 없다**(서버 `aa9cc9b`).
**엔드포인트는 하나도 늘지 않았는데 앱이 맞춰야 할 계약은 이번 회차에 가장 크게 늘었다** — 신규 토핑이
배치되면 그룹의 나머지 구성원에게 FCM 단건 발송이 나가고, 문구·`data` 키 4종(`type`·`route`·`groupId`·
`date`)·TTL·**Android 채널 id `parfait_default`**·최소 한 번 보장(중복 수신 가능)이 전부 계약이다.
지금은 등록된 토큰이 0건이라 발송이 전부 취소돼 **동작 영향이 0**이지만, 앱이 등록을 시작하는 순간
구속력을 갖는다. 특히 **채널 id를 안 맞추면 서버는 성공인데 사용자는 아무것도 못 본다**
(OQ-P-341·343·351·352·353 → [api/notification.md](api/notification.md)).
✅ **2026-09-05 — 받을 앱이 생겼다**(#446 딥링크 · #447 FCM 수신부). 2026-08-22에 걷어낸 FCM 축이
돌아왔고 채널 id는 서버가 못 박은 `parfait_default`를 앱이 따랐다([ADR-0013](adr/0013-firebase-fcm-crashlytics.md)
되살림 정정). 딥링크는 세션 종료 이동 구조를 그대로 복제했다(`PushDeepLinkEventBus` + `MainRoute` 단일
수집 → [navigation-flow](architecture/navigation-flow.md) "푸시 딥링크 이동").
✅ **같은 날 #450이 가운데 한 줄을 이었다 — 푸시 축이 닫혔다.** 등록을 **세션 축 넷**(로그인·가입·앱
진입의 성공 분기 + `onNewToken`)에서 부르고, 알림 권한은 **A-004·A-005 완료 직후** 묻는다. 핵심 결정은
**등록과 권한이 별개 축**이라는 것이다 — 토큰은 권한과 무관하게 발급되므로 등록을 권한에 매달면
재로그인·기기교체·재설치 사용자가 등록 경로에 닿지 못한다(OQ-P-341·358 해소,
[스펙](superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md)).
**API 33 미만은 허용으로 본다** — 그 아래에서 `checkSelfPermission`이 항상 거부를 답하는데 알림은
기본으로 켜져 있어, 판정이 사실과 정반대인 채 **그 기기군의 포그라운드 알림을 통째로 버리고 있었다.**
⚠️ **남은 것은 계약 두 건과 실행 확인이다** — `api/conventions.md` "Android 불일치" 3건 중 둘이 푸시
쪽이고(`date`를 안 읽어 항상 최신 캔버스로 열고, 중복 수신이 알림 두 개로 쌓인다 — OQ-P-359),
**실서버·실기기 확인은 0회**다.
⚠️ **2026-09-01 — 그룹 목록 `recentImageUrl`이 "오늘 캔버스의 토핑"으로 좁혀져 `api/conventions.md`의
"Android 불일치"가 0건에서 1건이 됐다**(엔드포인트 증감 없음, 앱 코드도 그대로인데 뜻만 바뀐 자리 —
OQ-P-336).
✅ **2026-09-06 — 누끼가 실패해도 토핑을 만들 길이 생겼고, 푸시 딥링크가 엣지 케이스 둘을 닫았다**
(#457·#456·#455·#458, 앱 버전 **1.1.0**(코드 7)). C-103-Error가 한 벌 문구와 버튼 둘을 갖고,
「편집 없이 사용」이 원본 사진을 그대로 토핑 재료로 보낸다 — **새 경로를 만들지 않고** 후보 선택 경로를
재사용하되 원본은 잘린 판과 캔버스 판이 같은 그림이라 `saveBitmap` 한 번으로 떨군 경로를 두 자리에
싣는다(`SaveEditedImageUseCase` → `SaveBitmapUseCase` 개명, OQ-P-153 ④ 종결 · OQ-P-344 완화).
디코드 실패는 실패 화면이 아니라 **뒤로 가기**라 실패 화면은 원본이 사는 상태에서만 뜬다.
푸시 쪽은 **되살린 태스크가 같은 딥링크를 다시 발행하던 경로**를 `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`로
막고, 스플래시 이탈 대기·세션 확인 두 게이트가 develop에 섰다(OQ-P-360 ①③ 해소, ② 잔존).
⚠️ 알림 아이콘의 여백 보정이 임시고(OQ-P-374), 크림 하한 3칸의 근거가 커밋 제목뿐이다(OQ-P-375).

**2026-09-04 — 원격 이미지를 기다리는 방식이 두 화면에서 각각 정해졌고, 그 방식이 반대다**(#440).
C-001 캔버스는 **다 모일 때까지 아무것도 안 낸다**(`rememberBatchRevealState` — 성공·실패를 모두 결말로
세고 **빈 목록은 완료가 아니다**), G-001 목록은 **아예 안 기다린다** — 자리가 먼저 열리고 그림이 뒤따르며
대신 400ms 간격으로 아래에서 위로 쌓인다. 둘을 잇는 것은 `core:ui` `reveal/`의 인터페이스 하나와
`Modifier.revealed`(알파 0 + 시맨틱 제거, 측정·배치는 살려 둔다)뿐이다. 실패도 반대다 — 캔버스는
**한 장만 실패해도 전체를 막고**(다시 시도 덮개), 목록은 실패한 것만 폴백으로 남긴다(OQ-P-355).
`YGScaffoldV2`의 덮개가 **슬롯**이 되며 터치 삼킴이 슬롯 몫으로 내려가 `YGDimOverlay`가 분리됐고,
접근성 차단이 `hideFromAccessibility()` → `clearAndSetSemantics { }`로 **수단 정정**됐다(앞의 것은 노드
하나만 감춰 덮개 아래 버튼이 그대로 읽혔다). G-001 실패의 갈림은 **"당겼는가"**로 세 번째 뒤집혀
`ShowRefreshError` 토스트가 걷혔고, 새로고침 인디케이터가 로띠 + 안내 문구 2줄이 되어 플랫폼 기본을
벗어났다(OQ-P-113 ① 해소). ⚠️ 전용 로딩 그래픽은 위키가 요구한 G-001이 아니라 **C-001에 붙었다**.
**2026-09-01 — 만든 그룹의 캔버스로 바로 들어가게 됐고, 갤러리 저장이 날짜바로 올라갔다**
(#411·#413·#414·#412·#434). 그룹 생성·참여의 종착지가 G-001 목록에서 **C-001 캔버스**로 옮겨 위키
[[기능정의서-v6]] 배선과 처음으로 맞았고(OQ-P-135 해소), 복귀 관용구가 `replaceAll(목록)` +
`goTo(캔버스)` 두 줄이 되며 `goToSingleClearTop` 은 소비처가 0건이 됐다. 캔버스는 `NavKeyCanvasMain`
인자로 **진입 사유**를 받아 환영 배너를 1회 띄운다(`YGAlert` 첫 프로덕션 소비처). 갤러리 저장은
날짜바 `ic_save` 아이콘이 되어 오늘 캔버스에서도 가능하고, 지난 캔버스 메뉴는 액션 하나만 남았다.
토핑 편집 핸들은 우상단 회전·우하단 크기조절로 맞바뀌었고 앱 버전이 **1.0.0**(코드 6)이 됐다.
⚠️ 배너 문구·저장 노출 조건에는 정책 근거가 없다(OQ-P-339·340 신설).
**2026-09-01 — 토핑 변형 저장이 일괄 PATCH 한 번으로 접혔고, 되살린 알맹이도 테두리는 고칠 수 있게
됐다**(#428·#425). C-301 확인 버튼이 단건 PATCH 를 토핑마다 부르던 자리를 `updateAll` 한 번이 대신하고
단건 경로는 앱에서 걷혔다 — 저장을 가르는 축이 **토핑 단위에서 축 단위**(변형 일괄 1회 + 테두리 N회)로
바뀌었고, 대가로 **부분 성공이 사라졌다**(OQ-P-334 ⑤). 최근 알맹이 재사용 진입의 "사진 편집"은
`borderOnly` 편집으로 열려 `NavKeyToppingEdit` 의 그 플래그가 **"되살릴 원본이 없는 진입"**을 뜻하게
됐다(OQ-P-337·OQ-P-338 신설).
**2026-08-31 — 오늘 캔버스가 화면 셋의 소유물에서 저장소 한 벌로 내려갔다**(#404·#408).
`ParfaitRepository.getTodayCanvas` 하나가 **구독·갱신 둘·정리·실패 축** 다섯으로 갈리고
`GetTodayParfaitUseCase`가 사라졌다. `:data`의 `CanvasLocalDataSource`(인메모리 둘째)가 값을 들고
`CanvasPoller`(그룹별 참조 계수 + 5초 루프)가 서버 재조회를 소유하며, 수명은 `:core:ui`에 새로 선
`BaseViewModel.launchWhileSubscribed`(구독 수 기반)에 매단다 — **화면은 폴러의 존재를 모른다**
([ADR-0029](adr/0029-canvas-today-ssot-polling.md), [스펙](superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)).
서버 계약은 한 줄도 안 바뀌었고 바뀐 것은 **누가 언제 부르는가**다. 겸해 배경 편집의 배경 탭에서도
토핑이 반투명·비상호작용으로 그려진다. **#408**은 최근 목록 정원을 종류별로 갈라(원본·알맹이 각자)
OQ-P-258을 닫고, 최근 줄에 무엇을 싣는지를 `returnResultOnly`에서 신설 `RecentImagePick`으로 옮겼다.
⚠️ 폴링 주기 5초와 정지 유예 5초는 실측 전 값이고(OQ-P-320), 경계 직후 오늘 조회가 한 그룹에서
동시에 나갈 수 있으며(OQ-P-323), 최근 목록 총 상한이 두 배가 됐는데 잰 사람이 없다(OQ-P-332).
유닛 949 → **996건**, 계측 17 유지.
**2026-08-27 — 본인 토핑 탭이 갈 곳을 얻었고, 계약의 마지막 미소비 갈래가 닫혔다**(#369·#398·#400).
C-001에서 **본인 토핑을 탭하면** `NavKeyCanvasBGEdit(initialToppingId)`로 이어져 **C-301 편집 화면의
토핑 탭**이 그 토핑을 선택한 채 열린다(OQ-P-250 ③ 해소). ⚠️ **정책이 말하는 C-305라는 새 화면이
생긴 것이 아니고**, `isViewingToday` 가드 때문에 **지난 캔버스에서는 여전히 무반응**이다(OQ-P-326 ③).
같은 라운드가 **확인 버튼에 테두리 PATCH를 걸어** `parfait-image.md`를 `done`으로 만들었다(4/4 소비,
소비처를 얻은 엔드포인트 27건, OQ-P-276 ①③ 해소). 서버 토핑을 다시 편집하지 못하게 막던
`TODO(#274)`도 함께 닫혔다 — `decodeImage`가 스킴을 갈라 `https://`면 새 `RemoteImageDownloadDataSource`
(전용 `@DownloadClient`, Retrofit 밖 raw OkHttp 둘째 자리)로 받아 디코드한다.
⚠️ **경고가 셋이다** — 테두리를 **그리는 겹(첫 겹)과 보내는 겹(마지막 겹)이 다르고**(OQ-P-324),
배율 하한이 0.5 → **0.05**로 내려가 48dp에서 역산하는 배치 화면과 갈렸으며(OQ-P-325), 선작성된
선작성 문서 셋이 이 델타를 몰라 **그대로 구현하면 테두리 저장이 되돌아가고**(OQ-P-326 ①), 서버 토핑
재편집만 입력 정규화 밖에 남는다(④).
유닛 926 → **931건**, 계측 14 유지.
**2026-08-26 delta는 엔드포인트를 안 늘리고 응답 필드 하나를 더했다**(여섯 라운드 연속 증감 0) —
오늘·상세 캔버스 응답의 `images[].placedBy`에 `ownerType`(`ME`·`OTHER`)이 붙어 **"이 토핑이 내 것인가"가
계약 안으로 들어왔다**. 서버가 요청자와 배치자의 계정 id를 견주어 채우므로 앱이 자기 식별자를 구해 올
필요가 없고, C-202의 비어 있던 본인 갈래(상수 `false`)와 C-301 편집 탭의 축이 다른 비교가 함께 닫힌다
(OQ-P-250 ① 해소). ⚠️ **요청자마다 값이 달라지는 첫 응답**이라 사용자 사이에 공유·캐시할 수 없다.
✅ **앱이 같은 날 따라왔다(#376)** — 매퍼가 `"ME"`만 참으로 접어 `CanvasToppingVO.isMine`을 만들고
상수 `false` 함수와 `GetMyAccountFlowUseCase` 의존이 함께 사라졌다. **계약 delta와 앱 반영이 같은
날 붙은 첫 사례**다(OQ-P-250 ② 해소). ⚠️ 본인 토핑 탭은 이제 잘못된 갈래로 가지 않는 대신
**아무 일도 안 한다** — C-305 화면이 없어서다(③ 잔존). → ✅ **하루 뒤 목적지가 생겼다**(#400,
아래 2026-08-27 항목).
**2026-08-26 — 토스트 자리가 되돌려졌고, 그 결정을 적어 둔 문서 한 줄이 여섯 날 동안 거짓이었다**(#371).
카메라·갤러리가 `YGToastHost`를 **다시 자기 프레임 안**(뷰파인더 자리·그리드 자리)으로 가져갔다 —
#309가 걷어 스캐폴드로 올렸던 형태로 복귀했고, 이유는 스캐폴드 호스트가 상태바 아래에 떠서
**헤더 행(날짜·닫기)을 덮었기** 때문이다. ⚠️ 그래서 c102 스펙의 "보이는 위치는 사실상 그대로다"와
c101 스펙의 "위키 공통 정책에는 이쪽이 맞는다"가 둘 다 틀렸다(두 "상단"이 다른 상자였고, 위키
[[toast]]는 노출 방향만 정하지 기준 프레임을 정하지 않는다). 처방은 브랜치 안에서 한 번 갈렸다 —
`YGScaffoldV2`에 여백 파라미터를 더해 Route가 헤더 높이를 dp로 복제하던 첫 커밋을 두 번째 커밋이
**통째로 되돌렸다**(스캐폴드는 결국 무변경). ⚠️ 토스트 관용구가 셋이 됐고 규칙이 없다(OQ-P-312).
**2026-08-26 — 릴리즈 산출물이 처음으로 만들어졌고 그 계보가 develop 밖에 있다**(#372·#374).
릴리즈 빌드 타입에 `isShrinkResources`가 붙어 **리소스 축소가 처음 켜졌고**(그전까지는 minify만),
`firebase-crashlytics-ndk`가 CameraX·DataStore의 네이티브 사망을 줍기 시작했으며(⚠️ 심볼 업로드는
꺼져 있어 주소만 남는다), 앱 버전이 **0.0.1/1 → 0.0.3/3**이 됐다. ⚠️ **경고가 셋이다** — 축소된
APK를 설치해 본 사람이 없고(OQ-P-308), 리포트가 콘솔에 닿는지 확인되지 않았으며(OQ-P-309),
**태그 `0.0.3`이 가리키는 `release/version-0.0.3-3` 브랜치가 develop에 없는 45커밋을 담고 있다** —
그중 넷이 이 문서가 "미머지"로 세 라운드째 세어 온 세그멘테이션 브랜치들이다(OQ-P-311).
**2026-08-25 delta도 계약 파일을 한 개도 안 바꿨다**(다섯 라운드 연속 증감 0) — 바뀐 것은 계약이
아니라 **계약이 실려 오는 길**이다. 앞단 리버스 프록시가 TLS를 종단해 서버가 HTTPS 도메인을 얻었고,
애플리케이션은 여전히 평문으로 듣되 `forward-headers-strategy`로 원본 스킴을 인식한다.
⚠️ 런북이 검증 뒤 **평문 포트를 닫는 단계**를 두는데, 닫히는 순간 기존 `YG_BASE_URL`로 빌드된 앱은
전부 연결에 실패한다. 그 시점은 1회성 인프라 조작이라 서버 커밋에서 읽을 수 없다 — "코드로 읽을 수
없는 전환 시점"이 이로써 **둘**이다(OQ-P-302·OQ-P-284). 같은 라운드가 계약 문서의 열흘 묵은 오기도
정정했다 — 앱에 `usesCleartextTraffic`이 **없다**고 적어 왔으나 2026-08-15(PR #241)부터 있었다
(`api/conventions.md` "전송", OQ-P-076).
✅ **앱이 하루 뒤 따라왔다(#358)** — 그 플래그가 빠지고 `network_security_config.xml`이 들어와
**릴리즈는 HTTPS를 강제하고 디버그만 평문으로 붙는다.** OQ-P-076이 닫혔고, 문서가 못 박아 둔
순서(base URL 교체가 먼저)를 뒤집고도 앱이 안 끊긴 이유는 `debug-overrides`가 디버그 빌드를 예전
그대로 두기 때문이다. ⚠️ 좁히기는 권고보다 넓다 — 개발 서버 도메인 한정이 아니라 디버그 빌드의
모든 호스트이고 사용자 설치 인증서까지 신뢰한다.
**2026-08-24 delta는 계약 파일을 한 개도 안 바꿨다**(네 라운드 연속 증감 0) — 대신 이 체계가 몰랐던
근거 축을 드러냈다. Flyway가 꺼져 있던 기간에 `ddl-auto: update`가 운영 스키마를 대신 관리해
**코드 근거로 정확히 적은 서술 둘이 운영에서는 거짓이었다**(그룹 내 닉네임 중복 허용이 실제로는 500,
토핑 배치 POST가 500). `ddl-auto: validate` + V16으로 소유권이 Flyway로 돌아갔고, 앞으로는 어긋나면
기동 시점에 실패한다. ⚠️ 다만 그 전환에는 **사람이 1회성으로 실행하는 baseline 절차**가 선행하고
그 시점을 앱이 알 수 없다(`api/conventions.md` "스키마 소유권", OQ-P-284·285).
**2026-08-20 delta는 엔드포인트도 필드도 안 늘리고 실패 경로만 늘렸다**(세 라운드 연속 증감 0) —
쓰기 다섯 경로(배경 변경 + 토핑 배치·수정·테두리·삭제)가 마감된 캔버스를 409
`PARFAIT_ALREADY_CLOSED`로 거부한다. "마감 후 편집을 누가 막나"라는 두 라운드 묵은 물음에 **서버가
답했고**, 그 대신 "서버는 막지 않는다"고 단정한 앱 주석 일곱 곳이 하루 만에 낡았다
(`api/parfait.md`·`api/parfait-image.md`). ✅ **그 일곱은 같은 날 앱이 고쳤다**(PR #318) —
지우지 않고 409를 사실로 적는 문장으로 바꿨고 `ServerErrorCode.Parfait`도 함께 생겼다.
**2026-08-18 delta는 엔드포인트를 안 늘리고 응답을 넓혔다** — Nametag-Chip 부여 주체가 서버가 됐고
(그룹 상세·목록·캔버스 `placedBy`에 필드 셋), 그룹 상세가 `groupName`·`memberLimit`을 싣는다.
같은 라운드가 **하루 경계를 자정에서 03시로 옮겨** 앱과 어긋났다 — ✅ **앱도 03시로 옮겨 닫혔다**
(2026-08-20 PR #308, `api/parfait.md` "하루 경계").
**2026-08-19 delta도 엔드포인트 증감 0**(두 라운드 연속) — 칩이 캔버스 `groupMembers`·토핑 배치
`placedBy`까지 실려 C-001 상단 멤버 칩이 계약 안으로 들어왔고, 목록의 시각·칩이 비널이 됐으며,
과거 목록 `to` 기본값도 03시로 통일돼 **서버 안의 두 기준이 하나가 됐다**. 전역 405도 생겼다.
⚠️ **같은 delta가 응답 JSON 키를 `nameTagChip` 계열로 바꿔** 그 필드를 옛 키로 읽던 코드가 조용히
어긋났고, 목록 시각의 비널화로 기존 파싱 불일치가 "그룹이 하나라도 있으면"으로 커졌다 —
✅ **둘 다 2026-08-20에 닫혔다**(PR #310). 이로써 `api/conventions.md`의 "Android 불일치"가
**2건에서 0건**이 됐다(이 저장소에서 처음).
⚠️ **2026-09-01에 다시 1건이 됐다** — 그룹 목록 `recentImageUrl`의 뜻이 "오늘 캔버스의 토핑"으로
좁혀졌는데 앱이 옛 뜻으로 읽는다(위 "지금 상태" 참고).
2026-08-15 2차 서버 delta는 엔드포인트 증감 없이 규칙만 바꿨고 — 초대코드 6자·닉네임 자모 허용·
그룹 내 닉네임 중복 허용(`GROUP_NICKNAME_ALREADY_USED` 삭제) — **2026-08-16 delta가 파르페 상세 조회·
배경 변경 2건을 더했다.** 그중 배경 변경이 C-301이 고른 배경을 버리던 이유의 서버 절반을 닫는다.
**Android 표면은 27/27, 공백 0이다** — 그 신규 2건을 **같은 날 #266이 닫았다**(2026-08-15 PR #250이
파르페 오늘·과거 조회, 토핑 테두리 수정·삭제, 회원 탈퇴 다섯을 닫은 데 이어. 애플 로그인 1건과
테스트 전용 회전 1건은 분모에서 뺀다). 배경 변경은 이 도메인 **첫 쓰기 경로**여서 첫 요청 DTO와
쓰기 전용 `CanvasBackgroundEdit`가 함께 들어왔고(읽기 모델은 이미지 배경을 URL로 들어 되돌려 보낼 수
없다), `TodayCanvasVO`는 상세 조회와 응답을 공유하며 **`CanvasVO`로 개명**됐다. **다만 `http/` 요청
모음은 25/27로 남아 표면과 처음으로 갈렸다.**
같은 PR이 2차 서버 delta도 반영했다 — `CheckNameValidUseCase`가 자모를 허용하고
`GROUP_NICKNAME_ALREADY_USED` 분기는 걷혔다. **다만 그 다섯 표면은 Repository조차 없다.**
**2026-08-15 — 하루 만에 8 엔드포인트가 화면까지 결선됐다**(#241·#242·#243·#244·#248).
A-002 카카오 로그인(#241)에 이어 온보딩 약관이 목록 조회·회원가입·세션 저장까지 가고(#242),
그룹 생성(#243)·참여 미리보기/참여/닉네임 변경(#244)·목록 조회(#248)가 붙어 **mock UseCase 3종,
mock 그룹 4건, `TERM_CONTENT_LIST`가 전부 삭제**됐다. 선반영이던 `ParfaitGroupRepository` 5메서드와
`ServerErrorCode` 12종도 이 라운드에서 모두 소비된다. 그 아래는 MVI 공통 에러 인프라
(`AppError`·`Channel` 이펙트·`launch(key, onError)`)다([ADR-0020](adr/0020-mvi-error-effect-infrastructure.md)).
~~남은 mock은 G-001의 `nickName` 하나인데, 그 값이 그룹 생성 요청으로 서버에 나간다.~~ → ✅ **닫혔다**
(2026-08-20 #312) — G-001이 계정 SSoT를 구독해 **전역 닉네임**을 A-005로 넘긴다. ~~남은 화면 mock은
C-301 편집 탭(`CanvasBGEditViewModel`) 하나다.~~ → ✅ **그것도 닫혔다**(2026-08-22 #329) — 편집 탭이
서버 캔버스를 그린다. **화면 mock 0건.**
~~파르페·이미지·회원·토핑 4 도메인은 표면은 전량 있는데 여전히 Repository조차 없다~~ → **회원(#263)·
파르페(#268)가 Repository를 얻어 이미지·토핑 둘만 남았다.**
~~⚠️ 그룹 목록은 코드 대조만으로 실패가 예상된다 — 업로드 시각을 오프셋 필수 파서로 읽는데 서버는
오프셋을 안 싣는다~~ → **매퍼가 `LocalDateTime` + 고정 KST로 읽어 닫혔다**(2026-08-20 PR #310,
[OQ-P-165](synthesis/open-questions.md)). 다만 **어느 경로도 실서버 요청을 해 본 적이 없다**
([OQ-P-146](synthesis/open-questions.md)) — 계약 정합이 0건이 됐어도 그것을 확인한 것은 코드 대조뿐이다.
**2026-08-15 — 만료를 다루는 주체가 생겼다**(#260, [ADR-0021](adr/0021-token-refresh-forced-logout.md)).
`TokenAuthenticator`가 401을 가로채 재발급하고 원요청을 잇는다(화면은 못 본다). 재발급이 **서버에
거절당할 때만** 세션을 버리고 `SessionEvent.ForcedLogout`을 앱 루트 한 곳이 받아 로그인으로 보낸다 —
네트워크 실패·5xx는 토큰을 유지한다. 재발급은 자격증명을 안 붙이는 전용 클라이언트로 나간다(디스패처
고갈 방지). 로그아웃도 결선돼 **auth 도메인이 `android_status: done`**이 됐고, 같은 라운드가
`Navigator.clearBackStack()`을 제거하고 `replaceAll()`로 합쳤다.
**2026-08-16 — C-201 캘린더가 붙었다**(#259). C-001의 날짜 버튼이 `YGCanvas.calendarContent` 슬롯을
처음 채워 연·월 드롭다운 + 날짜 그리드가 열린다. 다만 **두 조회 UseCase가 mock**이고(표면은 있는데
우회한다) 고른 날짜가 캔버스·라벨을 바꾸지 않는다([c201 스펙](superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md)).
런처 아이콘도 교체됐다(#262 — 적응형 3종 + monochrome, 스플래시 테마 속성 제거).
**2026-08-17 — 캔버스가 서버를 본다**(#268). `ParfaitRepository`(파르페 도메인 첫 Repository, 오늘·목록·상세
셋만) → UseCase 둘 → C-001이 **배경·토핑·멤버를 실데이터로** 그리고, 날짜 선택이 그날 캔버스를 불러온다.
진입도 열렸다 — `NavKeyCanvasMain(groupId)` + G-001 토핑 클릭. ~~Repository가 0건인 도메인은 **둘**
(image·parfait-image)이다~~ → **0건이 됐다**(2026-08-20 #322 — `ImageUploadRepository`·`ToppingRepository`).
⚠️ **화면에서는 여전히 읽기만이다** — 그 둘을 부르는 화면이 아직 없어 토핑을 새로 얹지 못하고,
조회 실패는 로그만이라 빈 캔버스와 구분되지 않는다. ~~C-301 편집 탭은 여전히 mock이다~~
→ ✅ **#329로 그 탭도 서버 캔버스를 그린다**(2026-08-22)
([c001-canvas-today-detail 스펙](superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md)).
**2026-08-17 — 클릭이 한 유틸로 모였다**(#284, PR #292 develop 머지). 프로덕션 Foundation
`Modifier.clickable` **28곳을 전량 `clickableYGNoRipple`로 이관**했다(남은 `clickable`은 `androidTest`
픽스처 2건뿐). 컴포넌트 대부분이 `collectIsPressedAsState()`로 눌림을 직접 그려
리플이 필요 없는데 호출 지점마다 `indication = null` 유무가 갈려 의도가 코드로 구분되지 않았다 —
**무리플을 기본에 두고 리플이 필요한 곳을 나중에 `clickableYG`로 올리는** 방향이다. 그 과정에서
`clickableYGNoRipple`에 `interactionSource` 파라미터가 붙었고, 300ms 스로틀이 화면 클릭 전반에
적용됐다(게이트는 Modifier 노드마다 하나라 같은 요소 연타만 막는다). 미결 3건 해소(`YGDateButton`
규약 이탈·`clickableYGNoRipple` 사용처 0·갤러리 그리드 셀), 신규 1건(리플이 유일한 피드백이던 6곳)
→ [design-system](architecture/design-system.md) clickable 절.
**2026-08-17 — C-001 화면 이름이 역할을 따라갔다**(#278, PR #291 develop 머지). 화면 계열이
`CanvasImageAdd*` → **`CanvasMain*`**로 개명됐다(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/
`UiState`/`Intent`/`Effect`, `strings.xml` 키 `canvas_main_*`). 머지본 대조 결과 **이름 치환 외 변경 0건**
(시그니처·동작 불변). 현행 문서(index·architecture·api·open-questions 미결)는 새 이름을 쓰고,
아카이브 스펙 본문과 `doc-baseline`은 당시 이름을 유지한 채 각주로 표기했다.
**2026-08-17 — 달력도 서버를 본다**(#279). 두 UseCase가 mock을 버리고 `getYears`가 올라와
`ParfaitRepository`가 **다섯 갈래 중 넷**을 연다(남은 하나는 배경 변경). `ParfaitHistory`는 삭제되고
달력이 계약 VO `PastCanvasVO`를 그대로 쓰며, 날짜 선택은 캐시에서 `parfaitId`를 꺼내 **상세만** 부른다.
상태가 `todayCanvas`/`viewedCanvas`로 갈려 편집 대상이 언제나 오늘이고, 지난 캔버스에서는 메뉴가
**갤러리에 저장·오늘의 파르페 가기**로 바뀐다(저장은 아직 로그 한 줄). ⚠️ 상세 조회에 붙은
`launch(key)` 가드가 **직전 라운드의 "마지막 선택이 이긴다"를 뒤집어**, 연속 선택 시 머리말과 그림이
어긋난 채 남는다([c201-canvas-calendar-server 스펙](superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)).
**2026-08-17 — 그룹 설정이 mock을 다 버렸고 나가는 문도 열렸다**(#285·#287). 상세 조회로 화면이
채워지고 닉네임 변경·나가기·신고가 실제 요청을 보낸다. `ParfaitGroupRepository`가 **8/8**이 되며
parfait-group이 **`android_status: done`**(8 엔드포인트 전부 호출부). **진입도 이때 열렸다** —
`NavKeyGroupSetting(groupId)` + C-001 상단 메뉴로 4일간의 도달 불가가 닫혔고, 컨테이너도
`YGScaffoldV2`(Route)로 이관돼 V1 잔여가 7파일로 줄었다. 상세 응답에 그룹명이 없어 **UseCase가
목록을 한 번 더 부르고**(그 실패는 삼킨다), 나가기·신고 성공은 `replaceAll(NavKeyGroupList)`다
(백스택이 전부 떠난 그룹 것이라 되돌아가면 403). ⚠️ 남은 것은 계약 공백과 임시 상수다 —
`remainingCount` mock 1(정원이 생성 응답에만 있다)·컬러칩 인덱스 순환·**신고 사유 하드코딩 하나**,
그리고 403/404가 일시 장애와 같은 문구다. ~~회원 탈퇴는 그대로 stub~~ → **#306으로 닫혔다**
([s101-group-setting-api 스펙](superpowers/specs/archive/2026-08-17-s101-group-setting-api.md)).
**2026-08-17 — 화면이 앞에 설 때마다 다시 묻는다**(#288, PR #297 develop 머지). G-001 목록·C-001 캔버스에
`Enter` 인텐트가 생기고 Route의 `LifecycleResumeEffect`가 그것을 보낸다 — `init` 조회는 ViewModel 수명에
걸린 것이라 백스택 아래에서 살아남아 낡았고, **생성·참여 후 목록이 갱신되지 않던 문제가 닫혔다**
(OQ-P-169). 복귀 관용구는 손대지 않았다(재조회가 필요한 이유는 복귀가 아니라 **남이 바꾸기 때문**).
같이 뒤집힌 것이 실패 규칙이다 — 목록이 남아 있으면 화면을 유지하고 **당긴 새로고침 실패만 토스트**로
알린다(목록이 비면 종전대로 에러 화면). 토스트 호스트 때문에 G-001이 `YGScaffoldV2`로 이관돼
**V1 잔여 6파일**. C-001은 오늘을 볼 때만 오늘 캔버스·올해 달력 기록을 다시 받고, 화면을 열어 둔 채
자정을 넘긴 경우는 `syncToday()`가 맡는다. ⚠️ 관용구일 뿐 규약이 아니고(OQ-P-221) C-001 조회 실패는
여전히 로그뿐인데 **실패할 기회만 늘었다**
([screen-resume-refetch 스펙](superpowers/specs/archive/2026-08-17-screen-resume-refetch.md)).
**2026-08-19 — 되돌릴 수 없는 문 셋이 다 열렸다**(#306). S-001 앱 설정의 회원 탈퇴가 마지막 stub이었고
`WithdrawUseCase`로 결선되며 **member 도메인이 `android_status: done`**(3 엔드포인트 전부 호출부)이 됐다.
UseCase가 얹는 규칙은 **순서**다 — 서버가 받아 준 뒤에만 기기를 정리하고, 거절당하면 아무것도 지우지
않는다(로그아웃과 반대인데, 서버가 거절했는데 로컬만 지우면 계정이 살아 있는 채로 사용자만 탈퇴했다고
믿는다). 형태는 S-101 나가기·신고가 확정한 것을 그대로 따랐다 — 팝업을 먼저 닫고 로딩 오버레이가 덮으며
실패는 토스트다. 성공은 `replaceAll(NavKeyLogin)`. ⚠️ **끝난 뒤가 깨끗하지 않다** — 정리를 위임받은
`LogoutUseCase`가 죽은 토큰으로 서버 로그아웃을 부르고, 그 401이 재발급을 깨워 `ForcedLogout`까지
발행돼 **이동을 두 곳이 일으킨다**(OQ-P-242). 실기기 확인은 없다.
**2026-08-20 — 온보딩 약관이 실패를 말하기 시작했고, 스캐폴드 잔여가 2파일이 됐다**(#315).
가입 실패가 로그뿐이던 것이 `TermAgreeError` 2갈래 + 공통 토스트로 나가고(A-002·S-101과 같은 형태),
약관 조회 실패는 반대로 **공용 에러화면으로 안 가기로** 정해져 목록 자리에 남는다 — 가른 기준은
**재시도 동선이 화면 안에 있는가**이고, 이것이 OQ-P-167이 묻던 "공통 에러화면을 세울지"의 답이다.
컨테이너도 Route의 `YGScaffoldV2`로 옮겨 **이관 17화면·V1 잔여 2파일**(둘 다 EntryBuilder)이 됐다.
**2026-08-20 — 하루 만에 낡았던 주석 일곱이 정리됐다**(#318). 서버 409 가드를 사실로 적는 문장으로
바뀌었고(지우지 않은 근거는 [`code-conventions.md`](code-conventions.md) "기준 2와 3이 겹칠 때는 남긴다"),
`ServerErrorCode.Parfait.PARFAIT_ALREADY_CLOSED`가 **소비처 0건인 채로** 신설됐다 — 처분이 이미
정해진 코드는 미리 둔다는 예외를 그 파일에 함께 적었다. ⚠️ 다섯 경로 모두 **권한 검사가 마감 검사보다
앞**이라 마감된 캔버스라도 남의 토핑·비멤버면 403이 먼저 온다(상수 KDoc의 경고).
**2026-08-20 — 캔버스 토핑에 첫 상호작용이 생겼다**(#298, [c202 스펙](superpowers/specs/archive/2026-08-20-c202-canvas-spotlight.md)).
토핑을 탭하면 그 하나만 남기고 나머지를 `Black50`으로 덮고 작성자 토스트를 1회 띄운다(C-202).
`YGCanvas`에 다섯 번째 슬롯 `overlayContent`가 뚫려 **토스트가 스캐폴드가 아니라 캔버스 프레임
상단에 선다** — 그 결정이 이 화면의 다른 토스트 자리까지 정했다(OQ-P-167). `YGToastType.Record`는
닉네임 색을 호출자에게 받고, 상대 시각은 `core:util:jvm`의 `ElapsedTimeBucket`이 갈래로 나눈다.
⚠️ ~~**정책의 본인 갈래가 통째로 비어 있다** — 내 멤버십 행 id를 알 길이 없어 `isMine()`이 상수
`false`라 본인 토핑도 Spotlight로 들어가고~~ → ✅ **판정이 붙었다**(#376, 서버 `ownerType`) —
본인 토핑은 Spotlight에서 빠진다. ~~다만 **C-305 진입은 여전히 `TODO`**라 그 탭은 무반응이다~~ → ✅ **진입도 붙었다**(#400 — 목적지는 새 화면이 아니라 **C-301 편집 화면의 토핑 탭**이고, 오늘 캔버스에서만 열린다). 작성자 칩도 서버 필드가
아니라 화면 목록 조인이다(OQ-P-251). **이 라운드는 신규 유닛 0건**이다(OQ-P-252).
**2026-08-20 — 업로드·배치 계층이 develop에 들어왔다**(#322). C-106 결선 스택의 1/5·2/5가 한 PR로
올라와 `@UploadClient`(S3 presigned PUT 전용, 인터셉터 0개)·`PresignedUploadDataSource`(**Retrofit 밖
raw OkHttp를 쓰는 유일한 자리**)·`ImageUploadRepository`·`ToppingRepository`·`AddToppingUseCase`가
생겼다. **부르는 화면이 0건이라 보이는 동작은 그대로**이고, Dagger가 도달할 수 없는 바인딩이라
리플렉션 테스트가 유일한 감지선이다. 유닛 561 → **602건**.
**2026-08-20 — G-001의 마지막 mock이 사라지고 잔버그 둘이 닫혔다**(#312·#320·#319).
목록이 계정 SSoT를 구독해 **전역 닉네임**을 A-005로 넘기고(OQ-P-197 해소, 이펙트가 값을 싣는다),
그룹 추가 오버레이가 나가는 길에 접히며(돌아왔을 때 누른 적 없는 오버레이가 떠 있던 것),
달력 항목 사이 여백을 눌러도 달력이 닫히지 않는다(`YGCanvas`가 탭을 소비). ⚠️ 닉네임을 아직 못
받았으면 그룹 만들기가 **조용히 안 열린다**(OQ-P-253).

## 무엇을 찾는가 → 어디를 보라
| 알고 싶은 것 | 권위 문서 |
|---|---|
| 모듈 구조·의존 방향 | [ADR-0001](adr/0001-layered-multi-module.md) + [module-structure](architecture/module-structure.md) |
| feature :api/:impl 분리 이유 | [ADR-0002](adr/0002-feature-api-impl-split.md) |
| 빌드 세팅(컨벤션 플러그인·버전 카탈로그) | [ADR-0003](adr/0003-convention-plugins-version-catalog.md) |
| DI·Hilt·스코프 | [ADR-0004](adr/0004-hilt-ksp-di.md) + [data-layer](architecture/data-layer.md) |
| 화면 상태관리(MVI)·신규 화면 추가 | [ADR-0005](adr/0005-custom-mvi-baseviewmodel.md) + [state-management](architecture/state-management.md) |
| 공통 에러 처리·이펙트 전달·중복 실행 방어 | [ADR-0020](adr/0020-mvi-error-effect-infrastructure.md) + [mvi-error-infrastructure 스펙](superpowers/specs/archive/2026-08-13-mvi-error-infrastructure.md) |
| 화면 컨테이너·공통 로딩 오버레이·실패 토스트 배선 | [design-system](architecture/design-system.md) "화면 컨테이너" + [ygscaffold-v2 스펙](superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md) |
| 내비게이션·신규 목적지 등록 | [ADR-0006](adr/0006-navigation3-custom-navigator.md) + [navigation-flow](architecture/navigation-flow.md) |
| UI·Compose·디자인 토큰·테마·컴포넌트 작성 | [ADR-0010](adr/0010-custom-compositionlocal-theme.md) + [design-system](architecture/design-system.md) (전신 [ADR-0007](adr/0007-compose-material3-design-tokens.md), superseded) |
| 로컬 영속화(DataStore) | [ADR-0008](adr/0008-datastore-local-persistence.md) + [data-layer](architecture/data-layer.md) |
| UseCase 패턴 | [ADR-0009](adr/0009-usecase-injectable-invoke.md) |
| 신규 데이터(Repo/DataSource) 추가 | [data-layer](architecture/data-layer.md) 체크리스트 |
| 원격 네트워크(Retrofit·OkHttp)·인증 헤더·응답 계약 | [ADR-0017](adr/0017-remote-network-datasource.md) + [data-layer](architecture/data-layer.md) |
| 서버 API 계약·엔드포인트·요청/응답 필드 | [api/README.md](api/README.md) + [api/conventions.md](api/conventions.md) |
| 도메인에서 비트맵 다루기(크로스모듈 추상) | [ADR-0011](adr/0011-cross-module-bitmap-abstraction.md) + [module-structure](architecture/module-structure.md) |
| 이미지 세그멘테이션(누끼)·ML Kit | [ADR-0012](adr/0012-mlkit-subject-segmentation.md) + [data-layer](architecture/data-layer.md) |
| 이미지 업로드(presigned 발급·S3 PUT·확인) | [api/image.md](api/image.md) + [c106-topping-place-api 스펙](superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) "업로드 전송" |
| 토핑 테두리를 굽지 않고 서버 필드로 | [ADR-0025](adr/0025-topping-border-as-server-field.md) |
| 토핑 만들기 흐름 상태(초안 SSOT) | [ADR-0026](adr/0026-topping-draft-datastore-ssot.md) |
| 화면 방향(세로 고정)·대화면 예외 | [ADR-0027](adr/0027-portrait-orientation-lock.md) |
| 시스템바 아이콘 색·다크모드 미지원 | [ADR-0028](adr/0028-system-bar-light-fixed.md) |
| Crashlytics·Analytics·Firebase 설정 + **푸시(FCM)** (2026-08-22 철회 → **2026-09-05 되살림**(#446·#447)에 이어 **#450이 기기 토큰 등록과 알림 권한 안내까지 채웠다** — 등록은 세션 축 넷, 권한은 A-004·A-005 완료 직후, API 33 미만은 허용. OQ-P-341·358 해소. ⚠️ 쓰고 있는 FCM API 셋이 deprecated이나 서버가 선행 조건이라 등록 토큰 축에 남는다 — OQ-P-362) | [ADR-0013](adr/0013-firebase-fcm-crashlytics.md) |
| 로깅·Logger 추상화(Kermit) | [ADR-0014](adr/0014-logging-abstraction-kermit.md) |
| 유효성 결과·에러 문자열 다국어 매핑(domain 의미↔표시 분리) | [ADR-0016](adr/0016-domain-result-presentation-string-mapping.md) + [state-management](architecture/state-management.md) |
| 구현 직전 기능·컴포넌트 설계 스펙 | [specs/README.md](superpowers/specs/README.md) |
| 작업 계획·진행 중/완료 작업 | [plans/README.md](superpowers/plans/README.md) |
| 구현 미결·열린 결정·코드/문서 정합 이슈 | [open-questions.md](synthesis/open-questions.md) |

## 문서 지도

이 문서 트리는 **플랫폼 축으로 갈린다** — Android 전용 문서는 `adr/`·`architecture/`·
`superpowers/specs/`·`superpowers/plans/`·`synthesis/` 다섯 갈래에 있고 나머지는 플랫폼
공용이다. 새 문서를 어디에 둘지는 이 허브의 분류와 루트 [`CLAUDE.md`](../CLAUDE.md)의 「문서 (`docs/`)」 절이 기준이다.

### Android 전용
- **[`adr/`](adr/README.md)** — "왜"(결정·대안·트레이드오프). 인덱스: [adr/README.md](adr/README.md)
- **[`architecture/`](architecture/README.md)** — "어떻게/어디"(상시 구현 가이드). 인덱스: [architecture/README.md](architecture/README.md)
- **[`superpowers/specs/`](superpowers/specs/README.md)** — "무엇을 만드나"(구현 직전 확정 설계, `YYYY-MM-DD-kebab-topic.md`). 완료분은 `specs/archive/`. 인덱스: [specs/README.md](superpowers/specs/README.md)
- **[`superpowers/plans/`](superpowers/plans/README.md)** — 작업 계획(`YYYY-MM-DD-kebab-topic.md`). 완료분은 `plans/archive/`
- **[`synthesis/`](synthesis)** — 분석·점검 산출물(open-questions·lint). wiki `synthesis/`와 동형.
  - **[`synthesis/open-questions.md`](synthesis/open-questions.md)** — 구현 미결·열린 결정·코드/문서 정합 이슈 추적. 정책·기획 미결은 위키 [[open-questions]].
  - **[`synthesis/lint-2026-07-22-parfait.md`](synthesis/lint-2026-07-22-parfait.md)** — 문서 내부 정합(링크·상태표·규율·민감데이터) 점검 보고서(2026-07-22, wikilink 3건 수정).
  - **[`synthesis/lint-2026-07-06-parfait.md`](synthesis/lint-2026-07-06-parfait.md)** — 문서 vs 실제 코드 정합성 점검 보고서(2026-07-06, 조치 완료 이력).
- **[`code-conventions.md`](code-conventions.md)** — 코드 주석·KDoc 규약. 루트 `CLAUDE.md`가 이 파일을 링크한다.
- **[`doc-baseline.md`](doc-baseline.md)** — 문서를 마지막으로 검증한 `develop` 커밋 해시(SoT) + "develop 기준 문서 점검" 절차. 현재 기준선 `143cda87b`(2026-09-21 검증, #511까지 — **Android 소스가 한 줄도 안 움직인 첫 라운드이고, 들어온 것은 이 저장소 밖에서 운영하던 정책 위키의 분기본이다**. delta 1건, **158파일 27237/0 — 삭제 0줄**, 커밋 67개, 머지 트리 = 브랜치 팁. `.kt`·`.kts`·gradle·xml·toml 변경 **0건**이라 유닛 **1298**·계측 **46**은 셈할 것도 없이 유지. 아카이브 이동 **0건**, **parfait 문서 드리프트 0건**(spec·plan·architecture·ADR·`api/` 다섯 표면 전부 불변), 신설 1건(OQ-P-405, `oq-next` 406). #511 이 `wiki/` 아래로 스크립트 다섯·pytest 열다섯·`routing.json`·`references/`·원본 45건·콘텐츠 `pages/`·graphify 산출물을 들여왔고, 루트 `CLAUDE.md`에 「위키」 절이 붙었다(Gradle·CI 미변경). ⚠️ **같은 정책 위키가 두 벌이 됐다** — 이 저장소의 `wiki/`와 저쪽이 같은 원본을 각각 ingest했고, 소스 39건은 파일명까지 같은데 **개념 층이 갈라졌다**(이쪽 concepts 19건 한글 세분 / 저쪽 8건 영문 묶음). **어느 쪽이 정본인지 정한 문서가 없다**(OQ-P-405). ⚠️ 이식 설계가 「콘텐츠는 옮기지 않는다·원본이 계속 정본」이라 못 박았는데 머지된 트리에는 같은 원본 39건이 그대로 있다. 미머지 하나(`feature/debug-mode`) 유지 — 원격 브랜치는 일곱에서 다섯으로 줄었다. 직전 회차(`e10ead2ca`, #513까지) 요약은 doc-baseline 본문에 있다).

### 플랫폼 공용
- **[`api/`](api/README.md)** — 서버(`mash-up-kr/TEAMYG-SERVER`) API 계약 스냅샷 + 플랫폼별 적용 상태.
  정본은 서버 코드이고 이 디렉토리는 미러다. 추적 브랜치는 서버 **`main`**(TJYG-Android의 `develop`과 다름).
  계약 절은 플랫폼과 무관하고 Android가 그것을 어떻게 받는지는 같은 문서의 「Android 매핑」 절에 적는다.
  기준선·갱신 절차는 [api/server-baseline.md](api/server-baseline.md), 반복 워크플로는 스킬 `sync-teamyg-server-api`.
- **[`script/`](script/README.md)** — 파이썬 툴링 홈(스킬 호출 로직·유틸, stdlib 전용). 템플릿: `_script-template.py`.
  링크 깊이 검사는 `python3 docs/script/check_links.py docs`.

## 규율 (상세는 각 문서)
- **SoT 우선순위**(모순 시): 코드 > wiki > CLAUDE.md
- **라인번호·변동수치 금지** — 근거·규칙은 [adr/README.md](adr/README.md)
- **코드 주석·KDoc** — [code-conventions.md](code-conventions.md). 코드가 이미 말하는 것은 안 쓰고, 고정 틀을 쓰지 않으며, **다른 곳의 현재 상태는 낡으니 단정하지 않는다**. 아키텍처 결정은 코드가 아니라 `architecture/`·`adr/`에.
- 새 아키텍처 결정 = 새 ADR([adr/template.md](adr/template.md)), 코드와 같은 커밋. 구조 변경 시 같은 PR에서 wiki 갱신(drift 금지).
- 새 기능·컴포넌트 = 구현 전 [specs/](superpowers/specs/README.md)에 설계 스펙 확정([specs/template.md](superpowers/specs/template.md)) 후 코드 작성.
