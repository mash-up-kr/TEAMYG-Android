# 기능별 현재 상태

> 현재형으로 쓰고, 바뀌면 덮어쓴다. 이력은 [`log.md`](log.md), 결정 이유는 `adr/`, 설계는 스펙.
> 영역 형식과 규칙: [기록 구조 설계](superpowers/specs/2026-09-26-docs-record-structure-design.md) §3.1.
> 코드 대조 기준: 작업 브랜치 HEAD `4fd12b91d`.

## 앱 진입·인증 — 스플래시·로그인·약관 (A-001·A-002)
- 상태: 앱은 스플래시에서 시작해 `BootstrapSessionUseCase`의 세션 검증 결과(저장 토큰 + `users/me` 조회)와 로띠 재생 종료가 둘 다 모여야 그룹 목록 또는 로그인으로 `replaceAll`하며, 세션은 401·`MEMBER_NOT_FOUND`일 때만 지우고 나머지 실패는 토큰을 남긴 채 로그인으로 보낸다. 로그인은 카카오 SDK ID 토큰을 서버에 보내 기존 회원은 그룹 목록으로, 신규 회원은 등록 토큰을 들고 약관 동의로 가고, 약관 동의는 서버 약관 목록으로 필수 동의를 가려 가입·세션 저장 뒤 그룹 목록으로 가며 약관 전문은 서버가 준 `url`을 공용 웹뷰로 연다.
- 앵커: `SplashViewModel`, `BootstrapSessionUseCase`, `LoginViewModel`, `TermAgreeViewModel`, `NotionWebView`, `feature/intro/impl`
- ⚠️ 진입 시간이 로띠 재생 길이에 묶여 있다 — 서버가 빨라도 애니메이션 끝까지 기다리고, 대기 상한이 없으며 느린 파싱은 그냥 대기다(실기기 측정 0건) (OQ-P-229)
- ⚠️ 오프라인으로 앱을 켜면 토큰은 남지만 로그인 화면으로 가서, 이미 로그인한 사용자가 처음 쓰는 사람과 같은 화면을 본다 — 설계 시점에 알고 남긴 선택이다 (OQ-P-195)
- ⚠️ 약관 목록 응답 `url`은 서버 `tos.content`를 그대로 실어 링크인지 전문인지 보장되지 않는데, 웹뷰가 판별·폴백 없이 그대로 로드한다 — 전문이면 일시 장애와 같은 재시도 화면이 뜬다 (OQ-P-068)
- ⚠️ 웹뷰 목적지가 임의 `title`·`url`을 받아 JavaScript를 켠 채 `loadUrl`하고 스킴·호스트 검증이나 리다이렉트 제한이 없다 — 지금 호출자는 서버 응답만 싣는다 (OQ-P-232)
- ⚠️ 스플래시는 공용 `YGLoadingLottie` 대신 `LottieAnimation`을 직접 부르고 모듈이 로띠 의존을 따로 단다 — 종료·파싱 실패 신호가 공용 표면에 없어서다 (OQ-P-230)
- 설계: [user-info-ssot](superpowers/specs/archive/2026-08-15-user-info-ssot.md), [a002-login-onboarding](superpowers/specs/archive/2026-08-11-a002-login-onboarding.md), [a002-kakao-login-api](superpowers/specs/archive/2026-08-13-a002-kakao-login-api.md), [intro-term-agree](superpowers/specs/archive/2026-07-22-intro-term-agree.md), [s004-terms-privacy-webview](superpowers/specs/archive/2026-07-20-s004-terms-privacy-webview.md), [ADR-0021](adr/0021-token-refresh-forced-logout.md), [ADR-0022](adr/0022-user-info-local-ssot.md)

## 그룹 목록 (G-001)
- 상태: `GroupListViewModel`이 그룹 인메모리 캐시(`GetMyGroupsFlowUseCase`)와 계정 SSoT(`GetMyAccountFlowUseCase`)를 구독해 그리고, 화면에 설 때마다 `RefreshMyGroupsUseCase`로 다시 조회한다. 첫 조회만 공통 로딩 덮개를 씌우고, 재진입 조회가 실패하면 보던 목록을 남기며, 당겨서 새로고침이 실패할 때만 에러 화면으로 간다. 토핑은 `ToppingLayout` 지그재그 위에 400ms 간격으로 쌓이고, 오늘 캔버스 토핑이 있으면 테두리까지 그린다. 없으면 `groupId`로 고른 템플릿을 그린다. 토핑을 누르면 C-001로, 그룹 추가 오버레이를 누르면 A-005·A-004로 간다.
- 앵커: `GroupListViewModel`, `GetMyGroupsFlowUseCase`, `ToppingLayout`, `GroupListParfaitLayout`, `feature/groups/list/impl/src/main/kotlin/com/teamyg/parfait/feature/groups/list/impl/util/ToppingImage.kt`
- ⚠️ 서버 `recentImageUrl`은 오늘 캔버스 토핑만 준다. 그래서 어제까지 활동한 그룹도 템플릿으로 그려지는데, 같은 카드의 경과 시간은 어제 토핑 시각이라 두 표시가 서로 맞지 않는다 (OQ-P-336)
- ⚠️ 토핑이 한 건도 없는 그룹은 서버 폴백 때문에 그룹 생성 시각과 생성자 칩을 마지막 활동처럼 보여 준다 (OQ-P-235)
- ⚠️ 계정 스트림이 첫 값을 내기 전에 「그룹 만들기」를 누르면 로그만 남고 아무 반응이 없다. 같은 오버레이의 「초대 코드로 참여」는 빈 닉네임으로 그냥 진행해 두 버튼의 반응이 반대다 (OQ-P-253, OQ-P-377)
- ⚠️ 경과 시간에 7일 이상 「오래 전」 갈래가 없어 계속 "N일전"으로 표시되고, 목록 정렬은 서버 응답 순서를 그대로 따른다 (OQ-P-170)
- ⚠️ 첫 조회 덮개를 켜는 판정이 `GroupListViewModel`과 `CanvasMainViewModel`에 따로 들어 있다. G-001 로딩은 위키가 요구한 전용 그래픽이 아니라 공통 오버레이를 쓴다. 전용 로띠는 C-001에만 있다 (OQ-P-330, OQ-P-113)
- ⚠️ 그룹 하나의 `recentImageUploadedAt` 파싱이 실패하면 카드 하나가 아니라 목록 전체가 실패한다. 현재 서버 계약으로는 이 경우가 생기지 않는다 (OQ-P-237)
- ⚠️ 파르페가 쌓이는 연출의 간격·생략 조건과 크림 하한에 정책 근거가 없다 — 그룹이 적은 사용자에게 보이는 모양이 이 상수로 정해진다 (OQ-P-357, OQ-P-375)
- 설계: [g001-group-list](superpowers/specs/archive/2026-08-01-g001-group-list.md), [group-ssot](superpowers/specs/archive/2026-08-17-group-ssot.md), [screen-resume-refetch](superpowers/specs/archive/2026-08-17-screen-resume-refetch.md), [g001-group-list-topping-border](superpowers/specs/archive/2026-09-11-g001-group-list-topping-border.md), [ADR-0023](adr/0023-group-in-memory-ssot.md)

## 그룹 생성·참여 (A-004·A-005·S-102)
- 상태: 생성은 A-005(`GroupCreateViewModel`)에서 그룹명·닉네임·인원(1~12)을 받아 `CreateGroupUseCase`를 호출한다. 참여는 A-004(`GroupInviteCodeViewModel`)가 6자 코드로 `GetGroupJoinPreviewUseCase` 미리보기를 받고, S-102(`GroupNickNameViewModel`)가 `JoinGroupUseCase`를 호출한 뒤 그룹 닉네임 `PATCH`를 잇는다. 닉네임 초기값은 계정 공통 앱 닉네임을 NavKey 인자로 넘겨받는다. 성공하면 이동 이펙트를 `rememberSaveable`에 잡아 두고 `NotificationPermissionGate`를 거친 다음(생성은 정원이 2명 이상일 때만), 백스택을 그룹 목록으로 되돌리고 새 캔버스를 환영 인자와 함께 쌓는다.
- 앵커: `GroupCreateViewModel`, `GroupInviteCodeViewModel`, `GroupNickNameViewModel`, `NotificationPermissionGate`, `NavigateToNextSaver`, `feature/groups/enter/impl`
- ⚠️ 이전 세션에서 알림 권한을 이미 두 번 거부한 사용자는 영구 거부로 판정되지 않는다. 이 사용자가 「알림 받기」를 누르면 시스템 다이얼로그도 설정 이동도 일어나지 않는다 (OQ-P-371)
- ⚠️ 알림 권한 안내는 거부나 「나중에」를 저장하지 않는다. 그래서 권한을 허용하기 전까지 그룹을 만들거나 참여할 때마다 다시 뜨고, 노출 횟수를 정한 정책도 없다 (OQ-P-370)
- ⚠️ 앱 닉네임이 아직 없을 때 두 갈래의 처리가 반대다. 생성은 화면을 열지 않고, 참여는 빈 입력칸으로 진행한다 (OQ-P-377)
- ⚠️ 이동 이펙트를 잡아 두는 대기 배선과 `NavigateToNextSaver`가 `GroupCreateRoute`·`GroupNickNameRoute`에 똑같이 두 벌 있다. 프로세스가 죽으면 대기 중이던 목적지를 잃어, 그룹은 만들어졌는데 사용자는 첫 화면에 서게 된다 (OQ-P-372)
- ⚠️ 참여는 됐는데 그룹 닉네임 `PATCH`만 실패하면 `NICKNAME_NOT_APPLIED` 토스트를 띄우고 전역 닉네임으로 들어간다. 이때 이동을 늦추는 `NICKNAME_NOTICE_DURATION`은 `YGToastPolicy`와 따로 선언된 상수라 두 값이 어긋날 수 있다 (OQ-P-328)
- ⚠️ 서버가 그룹 안 닉네임 중복을 허용하는데 정책 문서에 근거가 없다. 같은 이름이 여럿이면 그룹원 목록과 토핑 작성자를 구분할 수단이 없다 (OQ-P-179)
- 설계: [a004-group-invite-code](superpowers/specs/archive/2026-08-12-a004-group-invite-code.md), [a005-group-create](superpowers/specs/archive/2026-07-29-a005-group-create.md), [s102-group-nickname](superpowers/specs/archive/2026-07-22-s102-group-nickname.md), [push-notification-permission-and-device-token](superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md), [ADR-0023](adr/0023-group-in-memory-ssot.md)

## 그룹 설정 (S-101) — 닉네임·나가기·신고
- 상태: 그룹 설정은 인메모리 그룹 SSoT의 상세 캐시를 구독해 그룹명·내 닉네임·멤버(서버가 배정한 네임태그 칩 색)·초대 코드·남은 정원(`memberLimit` − 멤버 수)을 그리고, 진입 시 상세를 한 번 새로 받는다. 닉네임 변경은 성공하면 저장소가 상세를 다시 받아 캐시로 화면을 갱신하고, 나가기·신고는 팝업을 닫은 뒤 로딩 오버레이를 덮고 성공하면 캐시에서 그룹을 지우고 `replaceAll(NavKeyGroupList)`, 실패하면 공통 토스트를 띄운다.
- 앵커: `GroupSettingViewModel`, `ParfaitGroupRepositoryImpl`, `GroupLocalDataSourceImpl`, `toGroupSettingError`, `GROUP_REPORT_REASON`, `feature/groups/setting/impl`
- ⚠️ 닉네임 변경은 성공했는데 뒤이은 상세 재조회가 실패하면 캐시가 옛 이름에 머물러, 편집이 닫힌 채 확인 버튼이 다시 활성이 된다 — 상시 재현 가능하다 (OQ-P-220)
- ⚠️ 캐시가 빈 채로 상세 조회가 실패하면 `remainingCount` 기본값 0 때문에 "정원이 찼어요"와 죽은 초대 코드 복사 버튼이 뜬다 (OQ-P-225)
- ⚠️ 403(이미 나간 그룹)·404가 전부 `UNKNOWN`으로 접혀 "잠시 후 다시 시도" 문구로 나간다 — 다시 해도 같은 실패가 일시 장애처럼 안내된다 (OQ-P-218)
- ⚠️ 사유 선택 UI가 없어 모든 신고가 상수 `GROUP_REPORT_REASON` 하나로 저장된다 — 신고는 탈퇴를 동반해 되돌릴 수 없다 (OQ-P-217)
- ⚠️ 상세 응답의 `groupName`·`memberLimit`이 기본값 없는 비널이라, 그 필드가 없는 구버전 서버를 만나면 상세 조회가 통째로 실패한다 (OQ-P-227)
- 설계: [s101-group-setting-api](superpowers/specs/archive/2026-08-17-s101-group-setting-api.md), [group-ssot](superpowers/specs/archive/2026-08-17-group-ssot.md), [s101-group-side-menu](superpowers/specs/archive/2026-08-07-s101-group-side-menu.md), [setting-danger-zone-popups](superpowers/specs/archive/2026-08-09-setting-danger-zone-popups.md), [ADR-0023](adr/0023-group-in-memory-ssot.md)

## 앱 설정 (S-001) — 로그아웃·회원 탈퇴
- 상태: 앱 설정은 계정 SSoT를 구독만 해 프로필(닉네임·로그인 수단)을 그리고, 약관 두 줄은 고정 문구로 두고 누를 때 미리 받아 둔 약관 목록에서 제목·주소를 꺼내 공용 웹뷰로 연다. 로그아웃은 `LogoutUseCase`가 서버 실패와 무관하게 토큰·그룹 캐시·오늘 캔버스·계정 정보를 지운 뒤 로그인으로 `replaceAll`하고, 탈퇴는 `WithdrawUseCase`가 서버 승인 뒤에만 같은 정리를 불러 로딩 오버레이 + 실패 토스트 형태로 로그인으로 보낸다.
- 앵커: `AppSettingViewModel`, `LogoutUseCase`, `WithdrawUseCase`, `AccountInfoViewModel`, `feature/app/setting/impl`
- ⚠️ 탈퇴 뒤 정리를 맡은 `LogoutUseCase`가 지워진 계정으로 서버 로그아웃을 부르고, 그 401이 재발급·`ForcedLogout`까지 깨워 로그인 이동을 두 곳이 일으킨다(실기기·실서버 확인 없음) (OQ-P-242)
- ⚠️ 약관 목록 조회가 실패했거나 그 종류가 없으면 약관 줄을 눌러도 로그만 남고 아무 일이 없다 — 온보딩 약관은 재시도 문구가 있어 같은 API의 실패 표현이 화면마다 갈린다 (OQ-P-231)
- ⚠️ 로그아웃·탈퇴가 사용자 설정(`UserConfigRepository`)을 지우지 않아 같은 기기에서 계정을 바꾸면 앞사람의 튜토리얼 확인을 물려받는다 — `clearConfig` 호출부가 0건이다 (OQ-P-366)
- ⚠️ 로그아웃·탈퇴가 그룹별 지난 캔버스 알럿 확인 기록을 지우지 않아, 계정을 바꿔 같은 그룹에 들어가면 알럿을 놓친다 (OQ-P-397)
- ⚠️ 로그아웃 요청 중 항목은 `enabled`만 꺼지고 색이 그대로라 사용자는 눌러도 반응이 없는 이유를 모른다 — 디자인시스템에 비활성 색이 없다 (OQ-P-186)
- 설계: [app-setting-s001](superpowers/specs/archive/2026-07-19-app-setting-s001.md), [s002-account-info](superpowers/specs/archive/2026-07-22-s002-account-info.md), [setting-danger-zone-popups](superpowers/specs/archive/2026-08-09-setting-danger-zone-popups.md), [session-token-refresh-infra](superpowers/specs/archive/2026-08-15-session-token-refresh-infra.md), [ADR-0021](adr/0021-token-refresh-forced-logout.md), [ADR-0022](adr/0022-user-info-local-ssot.md)

## 캔버스 메인 (C-001)
- 상태: `CanvasMainViewModel`은 저장소의 오늘 캔버스 흐름을 화면이 보이는 동안만 구독하고(갱신은 폴러 몫, 지난 날을 보는 동안은 구독을 끊는다) 첫 조회 전에만 덮개를 띄우며 첫 조회 실패만 토스트로 알린다. 상단 그룹명은 그룹 목록 캐시가 정본이고 캔버스 응답의 이름은 캐시가 빌 때만 쓰며, 남의 토핑 탭은 Spotlight와 작성자 토스트로, 본인 토핑 탭은 오늘 캔버스에서만 C-301 토핑 탭으로 이어지고, 배경·토핑 이미지 결말은 `canvasLoadState`로 접혀 첫 페인트 전까지만 로딩·재시도 덮개가 된다.
- 앵커: `CanvasMainViewModel`, `CanvasMainUiState`, `CanvasMainRoute`, `canvasLoadState`, `pickToppingHit`, `GetTodayParfaitFlowUseCase`
- ⚠️ 토핑 한 장만 실패해도 첫 페인트 전이면 캔버스 전체가 다시 시도 덮개로 막히는데 그 규칙의 근거가 코드에만 있고, 첫 페인트 판정 상태가 Route 컴포지션에 있어 덮개 회귀를 잡는 테스트가 없다 (OQ-P-355, OQ-P-395)
- ⚠️ 배경을 고른 빈 캔버스에는 빈 안내판이 뜨지 않는다 — 안내판 조건이 `isEmpty && background == null`이다 (OQ-P-304)
- ⚠️ 그룹 생성·참여 직후 환영 배너는 문구·갈래에 정책 소스가 없고, 초대코드를 한 번 복사하면 재발행된 배너에 버튼 동작이 없어 다시 복사할 수 없다 (OQ-P-339)
- ⚠️ Spotlight 작성자 이름 색은 서버 값이 아니라 `memberChips` 조인이라 탈퇴·이탈 멤버는 기본색이 되고, 토핑 탭 판정의 알파 문턱·해상도는 실측 전 값이다 (OQ-P-251, OQ-P-313)
- ⚠️ 캔버스 두 조회의 404 `GROUP_NOT_FOUND`를 다른 실패와 가르지 않고, 목록에서 사라진 그룹이면 상단 바가 옛 이름을 그대로 든다 (OQ-P-383, OQ-P-407)
- ⚠️ 적응형 폴링의 단계·상한과 구독 정지 유예는 실측 없이 정한 값이고, 하루 경계 직후 같은 그룹의 오늘 조회가 동시에 나가 서버가 캔버스를 중복 생성하는지 확인되지 않았다 (OQ-P-320, OQ-P-323)
- 설계: [c001-canvas-main](superpowers/specs/archive/2026-08-12-c001-canvas-main.md), [c001-canvas-today-detail](superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md), [c202-canvas-spotlight](superpowers/specs/archive/2026-08-20-c202-canvas-spotlight.md), [canvas-today-ssot-polling](superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md), [canvas-adaptive-polling](superpowers/specs/archive/2026-09-10-canvas-adaptive-polling.md), [canvas-feedback-fixes](superpowers/specs/archive/2026-09-10-canvas-feedback-fixes.md), [past-canvas-alert](superpowers/specs/archive/2026-09-09-past-canvas-alert.md), [ADR-0023](adr/0023-group-in-memory-ssot.md), [ADR-0029](adr/0029-canvas-today-ssot-polling.md), [ADR-0030](adr/0030-topping-outline-distance-field.md)

## 토핑 생성·배치 (C-101·C-102·C-105·C-106)
- 상태: 캔버스 메인이 오늘 캔버스 id와 다음 깊이를 DataStore 초안에 못 박으면서 흐름이 시작되고, 촬영(C-101)이나 커스텀 갤러리(C-102 — 최근 줄은 부른 쪽에 따라 원본 또는 배치에 성공한 알맹이)로 고른 사진이 누끼·편집을 거쳐 초안에 알맹이와 테두리 한 겹을 남긴다. 배치 화면(C-106)은 폴러가 주는 오늘 캔버스 위에 정중앙·캔버스 폭 40%·짧은 변 48dp 하한으로 자동 배치하고, 확인하면 알맹이를 원본 긴 변 기준으로 축소·재인코딩해 올린 뒤 서버 좌표로 배치하며 성공했을 때만 초안을 비우고 캔버스로 되감는다.
- 앵커: `CanvasToppingPlaceViewModel`, `AddToppingUseCase`, `ToppingDraftRepositoryImpl`, `UploadImagePreprocessorImpl`, `RecentImagePick`, `CustomCameraViewModel`
- ⚠️ 커스텀 카메라는 `targetRotation` 없이 표시 방향 기준 회전값으로 보정해, 세로 고정 화면에서 가로로 들고 찍은 사진이 누운 채 누끼·배치·캔버스까지 흘러간다 (OQ-P-265)
- ⚠️ 테두리는 편집 세션 안에서만 여러 겹이고 초안·서버에는 마지막 한 겹이 저장되는데 C-301 편집 화면만 첫 겹을 그린다. 굵기는 토핑 배율·기기 폭과 무관한 절대 dp이고 그 정책 근거가 없다 (OQ-P-324, OQ-P-245)
- ⚠️ 토핑 업로드는 알맹이를 디코드해 다시 인코딩하므로 디코드 실패·ICC 프로파일 소실·미러 EXIF 미보정 갈래가 생기고, 빈 알맹이 하한은 축소 전 편집본 해상도에서 재며 `borderOnly` 진입은 판정에서 빠진다 (OQ-P-390, OQ-P-391)
- ⚠️ 갤러리 최근 줄의 알맹이로 들어온 재편집(`borderOnly`)은 원본 자리에도 알맹이를 넣어 재편집 좌표계 전제가 진입마다 다르고, 영역 탭을 막는 가드 하나가 유일한 방어다 (OQ-P-338)
- ⚠️ 새 토핑의 `positionZ`는 앱이 확정 시점에 구독 캔버스로 다시 세는 완화뿐이라, 폴링 주기 안에 두 사람이 확인을 누르면 깊이가 겹쳐 그리는 순서가 흔들린다 (OQ-P-322)
- ⚠️ 배치·편집 화면이 공유하는 `ToppingDragHandleButton`은 `onClick`이 빈 람다라 스크린리더에는 눌러도 반응 없는 버튼이고 드래그의 대체 수단이 없다 (OQ-P-202)
- 설계: [c101-camera-picture-confirm](superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md), [c102-custom-gallery-picker](superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md), [c106-topping-place](superpowers/specs/archive/2026-08-19-c106-topping-place.md), [c106-topping-place-api](superpowers/specs/archive/2026-08-20-c106-topping-place-api.md), [topping-border-distance-field](superpowers/specs/archive/2026-09-07-topping-border-distance-field.md), [topping-upload-source-scaled](superpowers/specs/archive/2026-09-09-topping-upload-source-scaled.md), [topping-draft-usecase-extraction](superpowers/specs/archive/2026-09-09-topping-draft-usecase-extraction.md), [ADR-0025](adr/0025-topping-border-as-server-field.md), [ADR-0026](adr/0026-topping-draft-datastore-ssot.md), [ADR-0030](adr/0030-topping-outline-distance-field.md), [ADR-0032](adr/0032-android-own-topping-upload-scale.md)

## 누끼 추출 (C-103·C-104)
- 상태: 사진 확인 화면 진입에서 ML Kit optional module 설치를 미리 요청하고, 세그멘테이션 화면은 진입마다 전용 캐시 디렉토리를 비운 뒤 원본 해상도 그대로 다중 피사체 추론 → 마스크 후처리·가이드 필터 알파 정련 → 후보가 0건이면 전경 마스크 2차 요청 순으로 후보를 만든다. 실패는 `C-103-Error` 한 화면이 받아 「다시 시도」(0건이면 정규화·초점 크롭 회복 사다리를 사진당 한 번)와 「직접 편집」(원본을 C-104로)을 주고, C-104 영역 탭은 붓 획으로 마스크를 고친 결과가 빈 알맹이 하한을 넘겨야 초안에 적는다.
- 앵커: `SegmentationViewModel`, `ImageSegmentationRepositoryImpl`, `SegmentationModuleInstaller`, `harvestSubjects`, `refineAlpha`, `ToppingEditViewModel`
- ⚠️ 원본을 다운샘플 없이 디코드하고 다중 후보 비트맵도 선택 전까지 전부 들고 있어, 큰 사진에서 메모리 피크가 `largeHeap` 없이 위험 구간이다 — 현재 트리 기준 피크는 미측정 (OQ-P-228, OQ-P-266)
- ⚠️ 전경 마스크 옵션과 다중 후보 옵션을 한 요청에 함께 켜면 ML Kit 모듈이 네이티브 `SIGSEGV`로 죽는다. `try/catch`도 Crashlytics도 못 잡으므로 두 옵션은 반드시 별도 요청으로 둔다 (OQ-P-409)
- ⚠️ 세그멘테이션은 beta ML Kit에 기대고, 모듈 가용 판정이 ML Kit 내부 feature 이름 상수라 이름이 바뀌면 크래시 없이 설치 요청만 반복하다 실패 화면이 뜬다. 모듈을 끝내 못 받는 기기의 정책은 없고 「직접 편집」이 유일한 우회로다 (OQ-P-003, OQ-P-345, OQ-P-344)
- ⚠️ 회복 사다리의 잠정값을 철회할 근거인 단계 로그가 Kermit `platformLogWriter` 하나라 logcat 밖으로 나가지 않고, 회복 경로를 강제로 태울 수단도 없다 (OQ-P-399)
- ⚠️ `applyAreaOpening`의 `countRuns`·`fillRuns`와 알파 정련 일부 루프에 취소 확인이 없어, 큰 판에서는 화면을 떠난 뒤에도 전체 패스가 끝까지 돈다 (OQ-P-318)
- ⚠️ C-104 영역 탭의 빨간 틴트와 `C-103-Error` 「직접 편집」 버튼의 이름·동작은 위키·디자인 근거 없이 코드가 정했다 (OQ-P-347, OQ-P-401)
- 설계: [c103-segmentation-topping-edit](superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md), [c103-multi-subject-selection](superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md), [segmentation-alpha-refinement](superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md), [segmentation-module-install](superpowers/specs/archive/2026-09-02-segmentation-module-install.md), [c103-error-use-original](superpowers/specs/archive/2026-09-05-c103-error-use-original.md), [segmentation-retry-recovery](superpowers/specs/archive/2026-09-10-segmentation-retry-recovery.md), [segmentation-preprocessing](superpowers/specs/2026-08-23-segmentation-preprocessing.md), [ADR-0012](adr/0012-mlkit-subject-segmentation.md)

## 캔버스 편집 (C-301 배경·삭제)
- 상태: 편집 화면은 캔버스 메인이 넘긴 오늘 `parfaitId`로 열려 오늘 캔버스를 구독하고, 배경·탭·선택은 최초 방출에만 시딩하며 이후 방출은 dirty·툼스톤 집합을 지키며 토핑 목록만 병합한다. 확인 버튼은 dirty 토핑의 변형을 일괄 PATCH 한 번, 테두리를 토핑별 PATCH로 보낸 뒤 배경(색 또는 업로드한 이미지)을 저장해 모두 성공해야 화면을 닫고, 토핑 삭제는 확인 모달에서 곧바로 DELETE 해 성공할 때만 닫으며, 실패는 모두 `CanvasBGEditError` 토스트 + 화면 잔류다.
- 앵커: `CanvasBGEditViewModel`, `CanvasBGEditUiState`, `CanvasBGEditError`, `NavKeyCanvasBGEdit`, `UpdateToppingsUseCase`, `ChangeCanvasBackgroundUseCase`
- ⚠️ 삭제는 모달 확인 시점에 영구가 되고 이동·크기·회전·테두리는 확인 버튼 시점에야 저장돼, 그만두기로 나가면 삭제만 남는데 화면은 그 차이를 말하지 않는다 (OQ-P-270)
- ⚠️ 마감된 캔버스의 409를 배경·토핑 저장 모두 일반 오류 토스트로 접어 다시 눌러도 영원히 실패하고, 변형 일괄 PATCH는 부분 성공이 없어 한 토핑이 걸리면 보낸 토핑 전부가 dirty로 남는다 (OQ-P-261, OQ-P-334)
- ⚠️ 크기는 하한만 있고 회전과 함께 상한이 없어 캔버스 밖으로 커진 배율이 그대로 PATCH 된다 — 배치 화면에는 상한이 있다 (OQ-P-271)
- ⚠️ 테두리를 그릴 때는 첫 겹, 저장할 때는 마지막 겹을 써서 겹이 둘 이상이면 보이는 테두리와 저장되는 테두리가 갈리고, 편집 결과의 `editedImagePath`는 상태에만 남는다 (OQ-P-324, OQ-P-276)
- ⚠️ C-305(본인 토핑 편집)가 별도 화면이 아니라 이 화면의 토핑 탭이고, 캔버스 메인의 `isViewingToday` 가드 때문에 지난 캔버스에서 본인 토핑 탭은 무반응이다 (OQ-P-326)
- ⚠️ 배경 업로드용 `copyToCache` 복사본이 `cacheDir/upload`에 쌓이기만 하고, 배경색은 `toRgbHex`로·테두리색은 로케일을 고정하지 않는 `toRgbHexString`으로 적는다 (OQ-P-262, OQ-P-263)
- 설계: [c301-canvas-background-edit](superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md), [c301-topping-edit-tab](superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md), [topping-batch-update-and-past-canvas-status](superpowers/specs/archive/2026-08-31-topping-batch-update-and-past-canvas-status.md), [canvas-today-ssot-polling](superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md), [ADR-0025](adr/0025-topping-border-as-server-field.md), [ADR-0029](adr/0029-canvas-today-ssot-polling.md)

## 달력·지난 캔버스 (C-201)
- 상태: 달력은 캔버스 메인 안에서 연도 목록과 해마다 한 번만 받는 기록 캐시로 그려지고, 보고 있는 달의 기록 있는 날과 오늘만 열린다. 지난 날을 고르면 상세 조회 결과를 오늘 구독과 분리된 칸에 두고 메뉴가 갤러리 저장·오늘로 가기로 바뀌며, 오늘 캔버스의 마지막 마감일을 이 기기·이 그룹에서 처음 보는 순간 그 마감 당시 인원 수로 지난 캔버스 알럿을 한 번 띄운다.
- 앵커: `CanvasMainUiState`, `CustomCalendar`, `GetParfaitHistoriesUseCase`, `PastCanvasVO`, `PastCanvasAlertRepositoryImpl`
- ⚠️ 상세 조회가 먼저 온 호출이 이기는 `launch(key)` 가드라, 날짜를 연달아 고르면 뒤 선택은 조회되지 않고 앞 응답은 날짜 재확인에 걸려 버려져 머리말과 그림이 어긋난 채 남는다 (OQ-P-212)
- ⚠️ 연도별 기록 캐시에는 무효화 경로가 없고, 파생 `parfaitHistories`가 "안 받은 해"와 "빈 해"를 같은 빈 목록으로 뭉갠다 (OQ-P-214)
- ⚠️ 기록 없는 지난 날과 토핑 0건인 날을 잠그는 규칙이 위키 Disabled 정의를 넘어서고, 점을 찍는 기준이 열람 가능 기준까지 겸한다 (OQ-P-213)
- ⚠️ 지난 캔버스 알럿의 노출 조건·첫 진입 기준선·인원 수 기준·문구는 정책 소스 없이 코드가 정했다 (OQ-P-392)
- ⚠️ 알럿 인원 수 조회가 실패하면 확인 기록을 남기지 않아 폴링 회차마다 상세 조회가 다시 나가고, 그룹별 확인 기록은 로그아웃·탈퇴에도 지워지지 않아 계정을 바꾸면 알럿을 놓친다 (OQ-P-393, OQ-P-397)
- 설계: [c201-canvas-calendar](superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md), [c201-canvas-calendar-server](superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md), [topping-batch-update-and-past-canvas-status](superpowers/specs/archive/2026-08-31-topping-batch-update-and-past-canvas-status.md), [past-canvas-alert](superpowers/specs/archive/2026-09-09-past-canvas-alert.md), [ADR-0029](adr/0029-canvas-today-ssot-polling.md)

## 캔버스 이미지 저장
- 상태: 날짜바의 저장 아이콘(오늘·지난 캔버스 모두, 빈 안내판이 없을 때만)을 누르면 `CanvasMainRoute`가 `YGCanvas` 프레임의 GraphicsLayer를 캡처해 고정 이름 캐시 PNG로 굽고 비트맵은 `CanvasCaptureHolder`에 실어 ViewModel 없는 미리보기로 보낸다. 미리보기에서 확정하면 결과 버스로 경로만 돌려주고, 캔버스 메인이 그 파일을 다시 읽어(API 28 이하는 쓰기 권한을 물은 뒤) `SaveCanvasToGalleryUseCase` → `GalleryMediaProvider`로 MediaStore에 PNG를 쓰고 결과를 캔버스 프레임 토스트로 알린다.
- 앵커: `CanvasCaptureHolder`, `writeToCanvasCaptureCache`, `NavKeyCanvasImageSave`, `SaveCanvasToGalleryUseCase`, `GalleryMediaProvider`, `GalleryWritePermissionManager`
- ⚠️ 캡처는 지금 그려진 것을 그대로 복사해 배경 이미지가 아직 안 왔으면 배경 없이 담기고, 해상도도 기기 화면 크기에 종속된다 (OQ-P-272)
- ⚠️ 캡처 파일 이름이 고정이고 지우는 호출이 없으며, 홀더도 한 장만 들어 연달아 캡처하면(푸시로 다른 그룹 캔버스에 갔다 오는 경우 포함) 미리보기가 다른 그림을 원래 날짜 라벨과 함께 보여 준다. 읽기·결과 왕복·미리보기 화면에 테스트가 없다 (OQ-P-365)
- ⚠️ 미리보기 키가 캐시 파일 절대경로와 날짜 문자열을 나른다 — OS가 캐시를 정리하면 확정 시 읽기 실패 토스트로 끝나고, `LocalDate.parse` 실패는 아무도 잡지 않는다 (OQ-P-364)
- ⚠️ API 29 미만에서는 `Pictures/Parfait` 하위 폴더도 `IS_PENDING` 보호도 없이 기본 위치에 쓰고 쓰기 권한 다이얼로그가 추가로 뜬다 (OQ-P-274)
- ⚠️ 오늘 캔버스에서도 저장되는 것에 기획 근거가 없고, 저장 아이콘 노출이 빈 안내판 조건의 부정으로 따로 적혀 있어 안내판 조건이 바뀌면 말없이 따라 바뀐다 (OQ-P-340)
- 설계: [c001-canvas-gallery-save](superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md), [canvas-save-preview-capture-holder](superpowers/specs/archive/2026-09-07-canvas-save-preview-capture-holder.md), [ADR-0011](adr/0011-cross-module-bitmap-abstraction.md)

## 푸시·딥링크
- 상태: FCM 수신은 `ParfaitFirebaseMessagingService`가 포그라운드 알림을 직접 띄우고 토핑 알림이면 오늘 캔버스 재조회를 요청하며, 탭한 알림의 `route`·`groupId`는 `PushDeepLinkEventBus`를 거쳐 `MainRoute`가 스플래시 이탈과 세션 확인을 기다린 뒤 캔버스(`canvas`)나 그룹 목록(`group`)으로 보낸다. 기기 토큰 등록은 알림 권한과 별개 축으로 부트스트랩·카카오 로그인·가입·`onNewToken` 네 자리가 부르고, 알림 권한은 그룹 생성·참여 완료 직후 `NotificationPermissionGate`가 묻는다(API 33 미만은 허용으로 본다).
- 앵커: `ParfaitFirebaseMessagingService`, `PushDeepLinkParser`, `PushDeepLinkEventBusImpl`, `DeviceTokenRegistrarImpl`, `NotificationPermissionGate`, `NotificationPermissionManager`
- ⚠️ 수신부가 계약의 `date`를 읽지 않아 토핑 알림은 늘 그 그룹의 최신 캔버스로 열리고, 알림 id가 `messageId` 해시라 재시도로 온 같은 알림이 두 개로 쌓인다 (OQ-P-359)
- ⚠️ 영구 거부 판정이 요청 직전·직후 rationale 비교라 이전 세션에서 이미 두 번 거부한 사용자에게는 "허용" 버튼이 설정으로도 안 보내고 아무 일도 하지 않는다. 이 갈래를 잠그는 테스트가 없다(모듈에 `androidTest` 소스셋 없음) (OQ-P-371, OQ-P-373)
- ⚠️ 거부·"나중에"를 영속하지 않아 허용 전까지 그룹 생성·참여를 마칠 때마다 안내가 다시 뜬다. 노출 횟수는 정한 적이 없다 (OQ-P-370)
- ⚠️ 딥링크 이동이 `goTo`라 알림으로 연 캔버스·목록 아래에 기존 백스택이 그대로 남는다(다른 진입 경계는 `replaceAll`을 쓴다) (OQ-P-360)
- ⚠️ 수신부 KDoc이 근거로 드는 "FCM 페이로드 스펙 v1"은 어느 저장소에도 없고, `data` 키·채널 id 문자열이 서버·`http/fcm-test.http`·앱 코드에 복제돼 있어 어긋나면 딥링크가 평범한 실행처럼 조용히 무시되거나 알림이 안 뜬다 (OQ-P-361, OQ-P-354)
- ⚠️ `getToken`·`onNewToken`은 deprecated지만 FID 전환은 서버가 Admin SDK를 올려 `setFid`로 바꾼 뒤에만 할 수 있다. 앱이 매니페스트 플래그를 먼저 켜면 모든 발송이 실패한다 (OQ-P-362)
- 설계: [push-notification-permission-and-device-token](superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md), [canvas-adaptive-polling](superpowers/specs/archive/2026-09-10-canvas-adaptive-polling.md), [ADR-0013](adr/0013-firebase-fcm-crashlytics.md), [navigation-flow](architecture/navigation-flow.md)

## 튜토리얼 (ygtutorial)
- 상태: 디자인시스템 `YGTutorialOverlay`는 딤까지 구워진 풀스크린 목업 PNG 한 장으로 실제 화면을 덮고 클릭을 삼킨다. 버튼 라벨(「다음」/「시작하기」)은 `YGTutorialProgress`가 정한다. 소비 화면은 C-001 캔버스(`CanvasTutorialStep` 3장), C-102 갤러리(1장), 누끼 확인(1장) 셋이다. 각 화면은 `GetTutorialVisibleFlowUseCase`로 `TutorialKind`별 노출 여부를 구독하고, `CompleteTutorialUseCase`로 평문 DataStore의 `UserConfigRepository`에 "봤다"를 남긴다. 캔버스는 마지막 장을 닫을 때, 한 장짜리 둘은 누르는 즉시 남긴다.
- 앵커: `YGTutorialOverlay`, `YGTutorialProgress`, `CanvasTutorialStep`, `GetTutorialVisibleFlowUseCase`, `UserConfigRepository`, `core/designsystem/src/main/kotlin/com/teamyg/parfait/core/designsystem/component/ygtutorial`
- ⚠️ `clearConfig`는 계약과 구현만 있고 부르는 곳이 없다. 로그아웃·탈퇴가 이 설정을 지우지 않아서, 같은 기기에서 계정을 바꾸면 앞사람의 "봤다" 기록을 물려받아 튜토리얼이 뜨지 않는다 (OQ-P-366)
- ⚠️ 목업 PNG는 실제 화면이 바뀌어도 따라 바뀌지 않는다. `ContentScale.Crop`이라 폭이 다른 기기에서는 잘리고, 위키에 튜토리얼 정책 조항이 없다 (OQ-P-363)
- ⚠️ 같은 튜토리얼 상태가 화면마다 두 형태다. 캔버스는 리소스 ID를 든 `CanvasTutorialStep`을 State에 싣고, 나머지 둘은 `Boolean`을 쓴다. 완료를 기록하는 시점도 둘로 갈린다 (OQ-P-369)
- ⚠️ 튜토리얼 구독 셋이 모두 `launchWhileSubscribed`를 쓰는데, 문서의 선택 기준은 "서버를 계속 부르는 구독"이다. 이 구독은 DataStore 키 하나만 읽는다 (OQ-P-368)
- ⚠️ 평문 `DataStorePreferences`와 암호화 프록시가 암호화 두 줄만 빼고 같은 코드다. 평문 쪽 `read`는 부르는 곳이 없다 (OQ-P-367)
- 설계: [design-system](architecture/design-system.md), [state-management](architecture/state-management.md), [data-layer](architecture/data-layer.md), [ADR-0008](adr/0008-datastore-local-persistence.md)

## 공통 기반
- 상태: 교차 관심사의 구조는 `architecture/`와 ADR이 정본이고 여기는 gotcha만 둔다. 세션은 `TokenAuthenticator`가 401마다 재발급하고 실패하면 강제 로그아웃으로 접히며, 릴리즈는 minify·리소스 축소를 켠 빌드를 develop 위의 경량 태그로 낸다.
- 앵커: `TokenAuthenticator`, `WithdrawUseCase`, `YGFontFamily`, `build-logic/convention/src/main/kotlin/com/teamyg/parfait/buildlogic/AndroidConfig.kt`, `.github/workflows/test.yml`, `app/src/main/AndroidManifest.xml`
- ⚠️ 릴리즈 빌드는 minify와 리소스 축소를 켜지만 축소된 산출물을 설치·실행한 기록이 없고 `keep.xml`도 없다. R8 산출물에서 `@NoAuth` keep 규칙이 살아 화이트리스트 요청이 인증 헤더 없이 나가는지도 확인되지 않았다 (OQ-P-308, OQ-P-074)
- ⚠️ 경량 태그 `1.0.0`에는 develop에 없는 커밋(`feature/debug-mode` 포함)이 들어 있고, 릴리즈 계보를 develop 하나로 둔다는 규칙은 글로 없다. 태그 `1.1.3`은 버전 코드 11만 가리켜 같은 이름의 코드 10 산출물과 이름으로 구별되지 않는다 (OQ-P-311, OQ-P-310)
- ⚠️ `WithdrawUseCase`가 `LogoutUseCase`를 재사용해 탈퇴가 끝난 계정으로 서버 로그아웃을 한 번 더 보내고, 그 401이 `TokenAuthenticator` 재발급까지 깨워 강제 로그아웃과 화면 이펙트가 같은 목적지로 겹친다 (OQ-P-242)
- ⚠️ 번들 SUIT 폰트가 수정본이라 OFL 1.1 사본 고지가 필요한데 `core/designsystem/OFL.txt`는 APK에 안 실리고 앱에 오픈소스 고지 화면이 없다. 파일명·버전·아웃라인이 원본과 같아 원본으로 되돌아가도 아무 검사도 실패하지 않는다 (OQ-P-306, OQ-P-307)
- ⚠️ 대화면 세로 고정이 매니페스트 opt-out 속성에 기대는데 그 속성은 `targetSdk 37`부터 무력화되고 대화면 방침이 없다. `compileSdk`는 이미 37이다 (OQ-P-264)
- ⚠️ CI는 `core:util:android`·`core:designsystem` 두 모듈의 계측 테스트를 컴파일만 하고 실행하지 않으며, `core:ui`의 계측 소스셋은 컴파일 대상에도 없다 (OQ-P-102)
- ⚠️ 원격 이미지 다운로드가 응답 본문을 상한 없이 통째로 힙에 올린다. 서버 토핑 이미지의 실물 크기를 잰 적이 없다 (OQ-P-327)
- ⚠️ 서버 실패를 화면이 표현하는 방식이 하나로 모이지 않았고, 실패 문구가 화면마다 복제돼 있다 (OQ-P-167)
- 설계: [session-token-refresh-infra](superpowers/specs/archive/2026-08-15-session-token-refresh-infra.md), [ADR-0021](adr/0021-token-refresh-forced-logout.md), [ADR-0003](adr/0003-convention-plugins-version-catalog.md), [ADR-0027](adr/0027-portrait-orientation-lock.md), [module-structure](architecture/module-structure.md), [design-system](architecture/design-system.md)

## 서버 계약
- 상태: 서버가 정본인 계약은 [api/README.md](api/README.md)에서, 앱과의 간극은 [api/conventions.md 「Android 불일치」](api/conventions.md#android-불일치)에서 본다.
- 앵커: `ServerErrorCode`, `JsonModule`, `data/src/main/java/com/teamyg/parfait/data/service`, `http/README.md`
- ⚠️ 응답 JSON 키를 단언하는 와이어 테스트는 `KakaoLoginResponseSerializationTest` 하나뿐이다. 원격 DataSource 테스트는 DTO를 직접 만들어 넣어 `@SerialName`을 안 보므로, 서버가 칩 키를 다시 바꾸면 널 기본값 때문에 멤버·그룹·캔버스 칩이 조용히 폴백 색으로 그려진다 (OQ-P-234)
- ⚠️ 서버는 그룹 재참여 때 멤버십 id를 유지해 탈퇴 전 토핑이 새 닉네임·칩으로 되살아나고 `ACTIVE` 캔버스면 편집·삭제 소유권까지 돌아오는데, 정책 근거가 없다 (OQ-P-398)
- 설계: [ADR-0017](adr/0017-remote-network-datasource.md)

## 실기기 미확인

코드로 판정할 수 없고 실기기·실서버에서 한 번도 확인되지 않은 항목.

- OQ-P-287 — 누끼 전처리의 임계·반경·정칙화·축소 하한이 측정 없이 정한 값이다(OQ-P-287~300, 판정 주체는 실기기 사진 세트)
- OQ-P-301 — 카메라 권한 거부 화면의 수정 결과를 실기기에서 본 사람이 없다
- OQ-P-337 — 토핑 테두리를 그리는 두 화면을 실기기에서 나란히 본 사람이 없어 어긋남의 크기를 잰 적이 없다(③)
- OQ-P-400 — 세그멘테이션 재시도 회복 경로와 1차 경로 회귀를 실기기로 본 적이 없다
- OQ-P-260 — 화면 전환 애니메이션의 모양·시간·방향을 실기기로 본 적이 없다
- OQ-P-146 — 로그인 실기기 검증 항목과 앱의 첫 실서버 호출 왕복이 한 번도 돌지 않았다
- OQ-P-350 — 교체된 스플래시 로고 애니메이션을 본 사람이 없고 되돌아가도 잡을 수단이 없다
- OQ-P-267 — 누끼 후보 필터 상수 둘이 실측 근거 없이 정해졌다
- OQ-P-280 — `ImageDecoder`의 EXIF 회전 자동 적용 여부를 사진 세트로 확인하지 않았다
- OQ-P-185 — 세션 인프라의 유실 창·재발급 쿨다운을 실제 네트워크 조건에서 본 적이 없다
- OQ-P-206 — 토스트가 떠 있는 동안 상단 띠의 탭이 삼켜지는 동작을 실기기로 확인하지 않았다
- OQ-P-215 — 리플을 걷어 피드백이 사라진 클릭 자리를 실기기 촉감으로 판정하지 않았다
- OQ-P-325 — 토핑 배율 하한이 두 화면에서 갈렸고, 편집 쪽 값이 손에 맞는지 본 적이 없다
- OQ-P-379 — 편집 화면과 나머지 화면의 테두리 거리판 해상도 차이를 눈으로 대조하지 않았다
- OQ-P-382 — 띠 한 장을 만드는 비용 증가와 리사이즈 중 재생성을 실기기 성능으로 잰 적이 없다
- OQ-P-038 — 토핑 그룹 두 변형이 코드상 같아 실제 화면에서 구분되는지 확인하지 않았다
- OQ-P-050 — 날짜 칩 배경이 테두리를 덮는지 실기기 렌더로 확인하지 않았다
- OQ-P-357 — 파르페 쌓임 연출의 간격을 실기기에서 본 적이 없다
- OQ-P-410 — 세션·푸시·캔버스·업로드·카메라·세그멘테이션 영역에서 이월된 나머지 실기기·실서버 항목
