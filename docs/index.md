# Parfait 구현 문서 — 에이전트 진입 허브

> 이 문서 트리는 2026-09-22에 `team-yg-pesonal-agent`(`parfait/`)에서 복사됐다.
> 기준 커밋 `de9f5f5`. 원본은 그 저장소에 그대로 남아 있고 자동 동기화는 없다 —
> 이후의 정본은 이쪽이다.

> 세션 시작·작업 전 **이 파일부터** 읽어라. 여기서 "무엇을 찾으면 어디를 보라"로 라우팅한 뒤, 필요한 문서만 펼친다 (전체를 읽지 말 것).

## 지금 상태
Android 단일 플랫폼, Jetpack Compose + Navigation3. 다중 모듈(core/data/domain/feature)·컨벤션
플러그인·Hilt·자체 MVI 기반이고, 서버 연동은 `api/` 계약을 따른다.
기능별 현재 상태와 알려진 결함은 [`status.md`](status.md)에 있다.

## 무엇을 찾는가 → 어디를 보라
| 알고 싶은 것 | 권위 문서 |
|---|---|
| 기능별 현재 상태·알려진 결함 | [status.md](status.md) |
| 언제 무엇을 점검했나 | [log.md](log.md) |
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
| Crashlytics·Analytics·Firebase 설정·푸시(FCM) | [ADR-0013](adr/0013-firebase-fcm-crashlytics.md) + [ADR-0031](adr/0031-analytics-central-screen-mapping.md) |
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
- **[`synthesis/`](synthesis)** — 미결 추적(open-questions). wiki `synthesis/`와 동형.
  - **[`synthesis/open-questions.md`](synthesis/open-questions.md)** — 구현 미결·열린 결정·코드/문서 정합 이슈 추적. 정책·기획 미결은 위키 [[open-questions]].
- **[`code-conventions.md`](code-conventions.md)** — 코드 주석·KDoc 규약. 루트 `CLAUDE.md`가 이 파일을 링크한다.
- **[`doc-baseline.md`](doc-baseline.md)** — 문서를 마지막으로 검증한 `develop` 커밋과 점검 절차.
- **[`status.md`](status.md)** — 기능별 현재 상태. 덮어쓰기만 한다.
- **[`log.md`](log.md)** — 점검·lint·구조 변경의 시간순 기록. append-only.

### 플랫폼 공용
- **[`api/`](api/README.md)** — 서버(`mash-up-kr/TEAMYG-SERVER`) API 계약 스냅샷 + 플랫폼별 적용 상태.
  정본은 서버 코드이고 이 디렉토리는 미러다. 추적 브랜치는 서버 **`main`**(TJYG-Android의 `develop`과 다름).
  계약 절은 플랫폼과 무관하고 Android가 그것을 어떻게 받는지는 같은 문서의 「Android 매핑」 절에 적는다.
  기준선·갱신 절차는 [api/server-baseline.md](api/server-baseline.md) — 이 저장소에는 반복 워크플로 스킬이 없어 수동으로 갱신한다.
- **[`script/`](script/README.md)** — 파이썬 툴링 홈(스킬 호출 로직·유틸, stdlib 전용). 템플릿: `_script-template.py`.
  링크 깊이 검사는 `python3 docs/script/check_links.py docs`.

## 규율 (상세는 각 문서)
- **SoT 우선순위**(모순 시): 코드 > wiki > CLAUDE.md
- **라인번호·변동수치 금지** — 근거·규칙은 [adr/README.md](adr/README.md)
- **코드 주석·KDoc** — [code-conventions.md](code-conventions.md). 코드가 이미 말하는 것은 안 쓰고, 고정 틀을 쓰지 않으며, **다른 곳의 현재 상태는 낡으니 단정하지 않는다**. 아키텍처 결정은 코드가 아니라 `architecture/`·`adr/`에.
- 새 아키텍처 결정 = 새 ADR([adr/template.md](adr/template.md)), 코드와 같은 커밋. 구조 변경 시 같은 PR에서 wiki 갱신(drift 금지).
- 새 기능·컴포넌트 = 구현 전 [specs/](superpowers/specs/README.md)에 설계 스펙 확정([specs/template.md](superpowers/specs/template.md)) 후 코드 작성.
