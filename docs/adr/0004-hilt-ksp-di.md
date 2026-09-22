---
id: ADR-0004
title: Hilt + KSP 의존성 주입, 스코프 분리
status: accepted
date: 2026-05-14
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr: ADR-0002, ADR-0017
related_spec:
related_architecture: data-layer
platforms: android
tags: [adr, parfait]
---
# ADR-0004: Hilt + KSP 의존성 주입, 스코프 분리

## 맥락
다중 모듈·레이어 구조에서 Repository·DataSource·Navigator·ViewModel의 수명과 조립을 일관되게 관리할 DI가 필요하다. 수동 DI는 모듈 경계를 넘는 그래프에서 보일러플레이트가 폭증한다.

## 결정
**Hilt(Dagger)** 를 KSP로 채택한다. 컨벤션 플러그인(`DaggerHiltCoreConventionPlugin`, `DaggerHiltComposeConventionPlugin`)으로 모듈 타입별 세팅을 자동 적용한다.

- 진입: `BaseApplication`에 `@HiltAndroidApp`, `MainActivity`에 `@AndroidEntryPoint`.
- ViewModel: `@HiltViewModel` + `@Inject constructor`.
- 스코프 사용 규칙:
  - **SingletonComponent** — Repository·DataSource·DataStore·네트워크 등 앱 수명 서비스. DI 모듈은 모두 `data` 레이어 `di/`에 **평면 배치, 역할당 파일 1개**로 둔다(`RepositoryModule`·`LocalDataSourceModule`·`RemoteDataSourceModule`·`ServiceModule`·`NetworkModule`·`JsonModule`·`DataStoreModule`·`SingletonInjectModule`) — 배치 규칙은 [[0017-remote-network-datasource]]·[[data-layer]].
  - **ActivityRetainedComponent / ActivityRetainedScoped** — `Navigator`와 feature 엔트리 빌더(`NavigationModule`). 설정 변경을 넘어 백스택 유지.
    > 📌 **평면 배치 규칙에 예외가 하나 생겼다(2026-09-05, PR #450)** — `DeviceTokenModule`이
    > **`:app`의 `push/di/`**에 있다. 바인딩 대상 `FirebaseDeviceTokenProvider`가 Firebase SDK를 쓰는데
    > [[0013-firebase-fcm-crashlytics]]가 그 의존을 `:app`에 가두므로 `:data`로 내릴 수 없다.
    > **`:app`이 Hilt 모듈을 갖는 첫 자리**이고, 같은 축의 다른 바인딩(`DeviceTokenRegistrarModule`)은
    > 규칙대로 `:data` `di/`에 있다 — 즉 갈린 기준은 도메인이 아니라 **SDK 의존이 어느 모듈에 갇혀
    > 있는가**다. 다른 Firebase 표면이 늘면 같은 이유로 `:app` 모듈이 더 생긴다.

Repository·DataSource 인터페이스↔구현 바인딩은 `@Binds`로.

## 대안
- **Koin** — 런타임 DI, 학습 곡선 낮음. 그러나 컴파일 타임 검증 없음.
  **→ 기각:** 다중 모듈 그래프 안정성 위해 컴파일 검증 선호.
- **수동 DI(팩토리·서비스 로케이터)** — 의존 적으면 가능.
  **→ 기각:** 모듈 경계·스코프 관리 비용 과다.

## 영향

**긍정**
- 컴파일 타임 그래프 검증. 스코프로 수명 명확(앱 전역 vs 액티비티 유지).
- feature 엔트리 빌더를 `Set<...>` 멀티바인딩으로 주입 → app이 impl을 직접 참조 안 함([[0002-feature-api-impl-split]]).

**트레이드오프**
- KSP 코드 생성으로 클린 빌드 시간 증가.

**위험·방어**
- 잘못된 스코프(예: Navigator를 Singleton으로) 사용 시 백스택 수명 버그 → 스코프 규칙을 이 ADR과 [[navigation-flow]]에 고정.
