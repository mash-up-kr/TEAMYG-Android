---
id: user-info-ssot
title: S-001 유저 정보 — users/me 로컬 SSoT · 자동로그인 부트스트랩 (User Info SSoT)
status: implemented
category: behavior-spec
platforms: android
verified: 2026-08-16
related_code: MemberRepository, MemberRepositoryImpl, UserInfoLocalDataSource, EncryptedPreferences, UserInfoEntity, MemberRemoteDataSource, MyAccountVO, GlobalNickname, LoginProvider, CryptoManager, GetMyAccountFlowUseCase, RefreshMyAccountUseCase, ChangeGlobalNicknameUseCase, BootstrapSessionUseCase, SessionBootstrap, ServerErrorCode, GlobalNicknameError, LoginProviderUiText, SplashViewModel, AppSettingViewModel, AccountInfoViewModel, LogoutUseCase, TokenAuthenticator, EncryptedTokenStore
related_adr: ADR-0022, ADR-0019, ADR-0021, ADR-0008, ADR-0009
related_spec: session-token-refresh-infra, s002-account-info, app-setting-s001
related_architecture: data-layer, state-management
supersedes:
superseded_by:
tags: [spec, parfait, member, session, datastore]
---

# Spec: 유저 정보 로컬 SSoT · 자동로그인 부트스트랩

> 상태·날짜·대상·관련은 위 frontmatter가 단일 출처. 본문은 설계 내용에 집중.

> ✅ **develop 머지(2026-08-16, PR #263 `2143c229`)** — 설계 범위 전량이 들어왔다.
> **as-built 차이 5건**을 아래 본문에 반영했다.
> ① UseCase 이름이 **`ObserveMyAccountUseCase` → `GetMyAccountFlowUseCase`**다. 호출 자체는 구독하지
> 않고 `Flow`만 넘기므로(구독 시점은 수집 측이 정한다) `Flow`를 돌려주는 선례
> (`GetRecentCacheImagesUseCase`)와 접두사를 맞췄고, 일회성 갱신인 `RefreshMyAccountUseCase`와
> 구분하려고 이름에 `Flow`를 남겼다.
> ② **암호화 DataStore 접근이 `EncryptedPreferences`(`data/datastore/`)로 뽑혔다.** 이 스펙이 그리던
> "저장소가 직접 `CryptoManager`를 감싼다"는 형태가 아니라, 쓰기의 암호화·읽기의 복호화·**못 읽는
> 저장분 폐기**를 프록시가 갖고 저장소는 *무엇을 지울지*와 *어떻게 해석할지*만 넘긴다
> (`EncryptedTokenStore`도 같은 프록시로 옮겨 탔다). 프록시가 **암호문 상태에서 `distinctUntilChanged`**
> 하는 것이 계정 정보 쪽에 필요했다 — 이 DataStore를 토큰과 공유해서, 재발급 저장이 무관한 구독자
> (편집 중인 입력 필드)를 재방출로 흔든다.
> ③ **닉네임 변경 성공 시 로컬이 비어 있으면 재조회로 채운다.** 아래 "갱신 시점"이 "재조회 안 함"으로
> 못 박았으나, 로컬이 `null`이면 `memberId`·provider를 몰라 닉네임만으로 VO를 만들 수 없다. 폴백
> 결과는 무시한다 — 변경 자체는 이미 성공했다.
> ④ **부트스트랩이 두 저장소를 직접 지우지 않고 `LogoutUseCase`에 위임한다.** 지울 대상이 사용자
> 로그아웃과 같아서, "무엇을 지우는가"가 두 곳에 적히면 한쪽만 늘어난다는 것이 근거다.
> ⑤ **`SplashInitialUseCase`가 삭제됐고**(최소 노출 시간 요구가 없어 자리채움 mock을 남길 이유가 없다)
> 부트스트랩 진입은 `SplashIntent.Init` 하나다. 화면 쪽에는 계획에 없던 것이 둘 더 붙었다 —
> 서버 실패 갈래를 담는 feature 로컬 `GlobalNicknameError`(4종)와, `ServerErrorCode.Member`
> (`INVALID_NICKNAME`·`MEMBER_NOT_FOUND`) 신설이다.
>
> **미검증**: 아래 계획의 수동 확인 7항목(자동로그인 왕복·첫 프레임·계정 전환·오프라인 진입 등)은
> 실기기로 돌리지 않았다.

## 목표

`GET /api/v1/users/me`가 주는 계정 정보를 **로컬에 한 벌만 두고** 여러 화면이 그것을 구독한다.
닉네임을 바꾸면 그 자리에서 모든 화면이 갱신되고, 앱을 껐다 켜도 서버 응답을 기다리지 않고
첫 프레임부터 값이 보인다.

지금은 화면마다 mock 문자열이 박혀 있다 — `AppSettingState.nickname = "아니야나그런데기니야"`,
`AccountInfoUiState.nickname = "대충지은랜덤닉네임"`. `MemberRemoteDataSource.getMyAccount()`는
구현돼 있으나 호출부가 0건이다.

같이 스플래시가 세션을 복원한다. 지금은 `SplashInitialUseCase`가 `delay(1000)` mock이고
`SplashRoute`가 **무조건 로그인 화면으로 보낸다** — 토큰이 있어도 매번 다시 로그인해야 한다.

## 범위

- **포함**
  - `UserInfoLocalDataSource` — DataStore 영속, 암호화, `Flow` 노출
  - `MemberRepository` + UseCase 3종(관찰·갱신·닉네임 변경)
  - 로그인·회원가입 성공 직후 1회 갱신
  - 스플래시 부트스트랩 + 자동로그인 라우팅
  - S-001 앱 설정·S-002 계정 정보 결선(mock 제거)
  - 로그아웃·강제 로그아웃 시 userInfo clear
- **제외**
  - **G-001 그룹 목록·A-005 그룹 생성** — `GroupListUiState.nickName` mock은 그대로 둔다.
    이번 라운드 밖이고, SSoT가 생기면 언제든 붙일 수 있다
  - 회원 탈퇴 — 서버 계약 없음
  - 프로필 이미지 — 서버 응답에 없다

## 선행

**[session-token-refresh-infra](2026-08-15-session-token-refresh-infra.md)가 먼저 머지돼야 한다.**
이 스펙은 그 라운드가 만든 것 셋에 의존한다.

- `LogoutUseCase` — 여기에 `clearMyAccount()`를 더한다
- `TokenAuthenticator` — 만료된 access token을 알아서 재발급하므로 부트스트랩이 만료를 다루지 않는다
- `SessionEventBus` / `ForcedLogout` — 세션이 끝나는 두 번째 경로

## API / 인터페이스

```kotlin
// data/source/member/local/UserInfoLocalDataSource.kt
interface UserInfoLocalDataSource {
    /** 저장된 계정 정보. 없거나 복호화에 실패하면 `null` */
    val myAccount: Flow<MyAccountVO?>

    suspend fun save(account: MyAccountVO)

    suspend fun clear()
}
```

```kotlin
// domain/repository/member/MemberRepository.kt
interface MemberRepository {
    /** 로컬 SSoT 읽기. 화면은 이것만 구독한다 */
    val myAccount: Flow<MyAccountVO?>

    /** 서버에서 당겨 로컬을 덮어쓴다 */
    suspend fun refreshMyAccount(): Result<MyAccountVO>

    /** 성공하면 로컬 닉네임도 함께 갱신한다 */
    suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>

    suspend fun clearMyAccount()
}
```

```kotlin
// domain/usecase/member/
class GetMyAccountFlowUseCase   { operator fun invoke(): Flow<MyAccountVO?> }
class RefreshMyAccountUseCase   { suspend operator fun invoke(): Result<MyAccountVO> }
class ChangeGlobalNicknameUseCase { suspend operator fun invoke(nickname: GlobalNickname): Result<GlobalNickname> }
```

```kotlin
// domain/model/session/SessionBootstrap.kt
sealed interface SessionBootstrap {
    data object ToGroupList : SessionBootstrap
    data object ToLogin : SessionBootstrap
}

// domain/usecase/session/BootstrapSessionUseCase.kt
class BootstrapSessionUseCase { suspend operator fun invoke(): SessionBootstrap }
```

부트스트랩이 목적지를 **도메인 타입으로** 돌려주는 이유 — 스플래시 화면이 "토큰이 있나",
"조회가 됐나"를 알 필요가 없다. 판단은 도메인이 하고 화면은 결과를 내비게이션으로 옮기기만 한다.

### 저장 형태

`MyAccountVO`는 값 클래스 둘(`MemberId`·`GlobalNickname`)과 enum 하나(`LoginProvider`)를 품어
그대로 직렬화할 수 없다. `:data`에 저장 전용 `@Serializable` 모델(`UserInfoEntity`)과 매퍼를 두고,
직렬화한 문자열 전체를 암호화해 단일 DataStore에 키 하나로 넣는다.
`RecentImageLocalDataSourceImpl`(`@LocalJson Json` + DataStore)과 `EncryptedTokenStore`의 선례를 각각 따른다.

> **as-built** — 암호화·복호화·손상분 폐기는 저장소가 직접 하지 않고 `EncryptedPreferences`
> (`data/datastore/`)가 한다. `UserInfoLocalDataSourceImpl`이 정하는 것은 저장 형태(JSON)와 실패 시
> 지울 범위(자기 키 하나)뿐이고, `EncryptedTokenStore`는 같은 프록시에 **한 짝 전체**(access+refresh)를
> 지우도록 넘긴다 — 두 토큰이 같은 키로 암호화돼 하나를 못 읽으면 다른 하나도 못 읽기 때문이다.
> 프록시는 **복호화 전, 암호문 상태에서 중복 방출을 끊는다**(`distinctUntilChanged`). DataStore를
> 저장소들이 공유해서 토큰 재발급 저장 같은 무관한 키 변경에도 `data`가 재방출하는데, 복호화 뒤에
> 끊으면 이미 매번 Keystore를 두드린 다음이라 비용을 못 던다. 이 성질이 S-002의 입력 버퍼가
> 편집 중에 되돌려지지 않는 근거이기도 하다.

**닉네임과 `memberId`는 식별 가능한 개인정보**라 평문으로 두지 않는다. 복호화에 실패하면
(키 회전·백업 복원) 저장분을 버리고 `null`을 돌려 다음 갱신이 채우게 한다 —
`EncryptedTokenStore.read()`와 같은 처리다.

`LoginProvider`는 `UNKNOWN` 폴백이 있는 enum이라, 저장분을 읽을 때 알 수 없는 값이면
`UNKNOWN`으로 떨어진다. 서버가 provider를 늘려도 저장분 때문에 크래시하지 않는다.

## 동작 / 상태

### 갱신 시점

| 시점 | 동작 |
|---|---|
| 로그인·회원가입 성공 직후 | `refreshMyAccount()` 1회. 실패해도 로그인은 진행한다 |
| 앱 진입(스플래시) | 토큰이 있으면 `refreshMyAccount()` 1회 |
| 닉네임 변경 성공 | 응답 값으로 로컬 갱신. **로컬이 비어 있으면 재조회로 채운다**(as-built) — `memberId`·provider를 몰라 닉네임만으로 VO를 세울 수 없다. 폴백 실패는 무시한다 |
| 그 외 화면 진입 | **없음.** 화면은 구독만 한다 |

로그인 직후 실패를 무시하는 이유 — 그 시점에 화면을 되돌릴 곳이 없다. 사용자는 이미 인증됐고,
값은 다음 진입에서 채워진다. 실패는 로그로 남긴다.

### 부트스트랩

```
토큰 없음                         → ToLogin (서버 호출 없음)
토큰 있음 + users/me 성공          → ToGroupList   (SSoT 채워진 상태로 도착)
토큰 있음 + 인증 거절              → 토큰·userInfo clear → ToLogin
토큰 있음 + 그 외 실패             → 아무것도 지우지 않고 ToLogin
```

**목적지는 실패 종류와 무관하게 `ToLogin` 하나다. 갈리는 것은 정리 범위뿐이다.**
세션을 파기하는 것은 **세션이 죽었다고 서버가 말한 경우**뿐이다 — HTTP 401 또는
`MEMBER_NOT_FOUND`. 그 외(네트워크 실패·5xx·로컬 저장 실패를 포함한 예상 밖 실패)는
토큰도 userInfo도 건드리지 않는다.

만료된 access token은 `TokenAuthenticator`가 재발급하므로 부트스트랩이 401을 직접 다루지 않는다.
재발급까지 실패하면 그쪽이 이미 토큰을 지우고 `ForcedLogout`을 쏜다.

> ⚠️ **네트워크 실패도 `ToLogin`으로 보낸다.** 오프라인에서 앱을 켜면 로그인 화면으로 간다는
> 뜻이다. `TokenAuthenticator`가 네트워크 실패에 토큰을 유지하기로 한 결정과 방향이 어긋나
> 보이지만, 여기서는 **토큰을 지우지 않고** 라우팅만 로그인으로 보낸다 — 연결이 돌아온 뒤
> 다시 켜면 자동로그인이 성립한다. 오프라인 진입에 그룹 목록을 캐시로 그릴 수단이 아직
> 없어서 내린 선택이고, 캐시가 생기면 재검토한다 → [미결](#주의--열린-질문)

> **정리 범위를 인증 거절로 좁힌 근거**(2026-08-15, 구현 중 확정). 초안은 "실패 = 토큰·userInfo
> clear"였으나 두 경로가 깨진다. (1) 서버 5xx는 `AppError.Unexpected`로 떨어져 **서버 배포·장애
> 중에 앱을 켠 모든 복귀 사용자가 로그아웃**된다. (2) 서버가 200을 준 뒤 로컬 저장이 실패해도
> 같은 실패 채널로 나와 **멀쩡한 세션이 파기된다.** 둘 다 세션 사망의 증거가 아닌데 처분만
> 가혹하다. 목적지는 어차피 `ToLogin`으로 같으므로 사용자가 보는 화면은 바뀌지 않고,
> 잘못된 파기만 사라진다.

### 세션 정리

userInfo는 토큰과 **같은 수명**이다. 지우는 자리가 둘이다.

| 경로 | 지우는 주체 |
|---|---|
| 사용자 로그아웃 | `LogoutUseCase`가 `authRepository.logout()` + `memberRepository.clearMyAccount()` |
| 강제 로그아웃 | `TokenAuthenticator`가 토큰을 지우는 그 자리에서 userInfo도 지운다 |
| 부트스트랩 인증 거절 | **`LogoutUseCase`에 위임한다**(as-built) — 지울 대상이 사용자 로그아웃과 같다 |

as-built로 정리 주체가 **둘**(`LogoutUseCase`·`TokenAuthenticator`)이 됐다. 부트스트랩이 두 저장소를
직접 부르지 않는 이유는 "무엇을 지우는가"가 두 곳에 적히면 한쪽만 늘어나기 때문이고, 반환값도
보지 않는다 — 라우팅은 이미 `ToLogin`으로 정해져 있다. `clearMyAccount()`의 IO 실패는 `LogoutUseCase`가
`runSuspendCatching`으로 삼켜 로그아웃 자체를 실패로 만들지 않는다.

강제 로그아웃 쪽을 `:data` 안에서 끝내는 이유 — 앱 루트가 이벤트를 받아 정리하게 하면, 그
이벤트가 유실되는 순간(재생성 창, `session-token-refresh-infra`의 열린 질문) **토큰은 지워졌는데
userInfo는 남는** 상태가 생긴다. 지우는 주체를 하나로 두면 둘이 갈라지지 않는다.

`TokenAuthenticator`는 **`ForcedLogout`을 먼저 쏘고 그다음에 두 저장소를 지운다.** 이벤트는
"세션이 죽었다"는 통지이지 "정리가 끝났다"는 통지가 아니고, `clear()`가 던지면(DataStore IO
실패) 이벤트가 영영 안 나가 토큰만 지워진 채 앱이 그 사실을 모르게 된다.

### 화면 결선

| 화면 | 지금 | 바뀐 뒤 |
|---|---|---|
| S-001 앱 설정 | `nickname`·`loginProvider` mock 문자열 | `GetMyAccountFlowUseCase` 구독 |
| S-002 계정 정보 | `nickname` mock 문자열 | 구독 + 변경은 `ChangeGlobalNicknameUseCase` |

**서버 실패 갈래는 feature 로컬 `GlobalNicknameError` 4종**(`INVALID`·`ACCOUNT_GONE`·`NETWORK`·`UNKNOWN`)으로
받는다(as-built). `GroupNickNameError`(`feature/groups/enter/impl`)와 형태는 같지만 재사용하지 않는다 —
feature `impl`은 서로를 의존하지 않는 leaf 모듈이고, 전역 닉네임에는 중복(`ALREADY_USED`) 개념이 없다.
`ACCOUNT_GONE`(404 `MEMBER_NOT_FOUND`)을 `UNKNOWN`과 나눠 두는 이유는 **재시도가 절대 성공하지 못하는
실패**여서다. ⚠️ 그 코드를 `BootstrapSessionUseCase`는 세션 사망으로 판정하는데 **이 화면은 표시만 하고
세션을 정리하지 않는다** — 죽은 세션은 다음 앱 진입의 부트스트랩이 걷어낸다(강제 로그아웃 발신 주체는
`TokenAuthenticator` 하나라는 결정을 유지한다).

`LoginProvider`는 enum이라 표시 문구 매핑이 필요하다(`KAKAO` → "Kakao"). ADR-0016 결정대로
**domain은 의미만 돌려주고 표시 매핑은 프레젠테이션이 소유한다** — `core:ui`에
`LoginProvider.toStringResource()`(@Composable)를 두고 문자열은 `core:ui`의 `strings.xml`에 넣는다.
`UNKNOWN`도 문구를 가져야 한다(빈 화면보다 낫다).

S-002의 닉네임 변경은 **낙관적 갱신을 하지 않는다.** 서버 응답을 받고 로컬을 갱신한다 —
실패했는데 다른 화면에 새 닉네임이 보이는 것이 되돌리는 것보다 나쁘다.

## 표시·제어 규칙

- SSoT가 비어 있는 동안(최초 로그인 전 또는 복호화 실패 후) 화면은 **빈 문자열이 아니라 로딩
  상태**를 보여야 한다. mock을 지우면 기본값이 없어지므로 각 화면이 `null`을 다룬다.
  S-002는 레이아웃을 그대로 두고 **입력 필드를 비활성**으로만 둔다 — 자리를 다른 것으로
  바꾸면 값이 도착할 때 화면이 튄다
- 닉네임 변경 요청 중에는 확인 버튼 비활성
- 스플래시는 부트스트랩이 끝날 때까지 현재 로딩 화면을 유지한다
  > 🔁 **as-built 정정(2026-08-18, #305)** — **부트스트랩이 끝나도 못 떠난다.** 스플래시에 로띠가
  > 들어오며 조건이 둘이 됐고(`SplashState.destination`·`isAnimationFinished`), 나중에 끝난 쪽이
  > 이동을 일으킨다. 위 ⑤가 "최소 노출 시간 요구가 없어 `SplashInitialUseCase`를 지웠다"고 적은
  > 자리에 **다른 이유(애니메이션)로 최소 노출이 되돌아온 것**이다 — 다만 `delay` 상수가 아니라
  > 재생 종료 신호이고, 그 신호는 화면이 인텐트로 올린다(도메인은 여전히 목적지만 안다).
  > 상한은 없다 → [open-questions](../../../synthesis/open-questions.md).

### S-002 편집 세션

계정 정보는 **읽는 화면이 기본이고 편집은 세션**이다. 규칙 셋이 거기서 나온다.

| 상황 | 동작 |
|---|---|
| 입력 필드에 포커스 없음 | 확인 버튼을 **보이지 않는다**(기본 상태엔 버튼이 없다) |
| 포커스 있음 | 확인 버튼이 키보드 위 화면 하단에 뜬다. 활성 조건은 **서버 값과 다르고** 형식이 유효하고 요청 중이 아닐 것 |
| 뒤로가기 — 서버 값과 다름 | `닉네임 수정을 취소할까요?` 확인. `그만두기`=버리고 나가기 / `취소하기`=닫고 계속 편집 |
| 뒤로가기 — 바꾼 것 없고 편집 중 | 키보드만 내린다 |
| 뒤로가기 — 바꾼 것 없음 | 바로 나간다. 잃을 것이 없는데 묻지 않는다 |

"서버 값과 다른가"를 알려면 **저장된 값과 입력 버퍼를 따로 들고 있어야 한다.** 화면 상태가
`savedNickname`(SSoT가 준 값)과 `nickname`(입력)을 나눠 갖는 이유다.

> 같은 확인 모달이 S-102(그룹 닉네임)에도 있으나 현재 구현은 뒤로가기에 포커스만 내린다 →
> [미결](#주의--열린-질문)

## 파일 구성

| 파일 | 역할 |
|---|---|
| `data/source/member/local/UserInfoLocalDataSource(.kt/Impl.kt)` | DataStore 영속·암호화·`Flow`. 신규 |
| `data/model/local/UserInfoEntity.kt` | `@Serializable` 저장 모델 + 매퍼. 신규 |
| `data/repository/member/MemberRepositoryImpl.kt` | remote↔local 조율. 신규 |
| `domain/repository/member/MemberRepository.kt` | 신규 |
| `domain/usecase/member/*.kt` | UseCase 3종. 신규 |
| `domain/model/session/SessionBootstrap.kt` | 신규 |
| `domain/usecase/session/BootstrapSessionUseCase.kt` | 신규 |
| `domain/repository/auth/AuthRepository.kt` | `hasSession()` 추가(토큰 존재 여부) |
| `domain/usecase/auth/LogoutUseCase.kt` | `clearMyAccount()` 추가 |
| `data/network/TokenAuthenticator.kt` | 세션 폐기 시 userInfo도 clear |
| `domain/usecase/auth/LoginWithKakaoUseCase.kt`·`SignUpUseCase.kt` | 성공 직후 refresh |
| `core/ui/.../text/LoginProviderUiText.kt` | `toStringResource()`. 신규 |
| `feature/intro/impl/.../splash/SplashViewModel.kt`·`SplashRoute.kt` | 부트스트랩 분기 |
| `feature/app/setting/impl/.../AppSettingViewModel.kt` | mock 제거·구독 |
| `feature/app/setting/impl/.../AccountInfoViewModel.kt` | mock 제거·구독·변경 결선 |

## 테스트

**`UserInfoLocalDataSourceImpl`** (`data/src/test/`)
- 저장 후 읽으면 같은 값이 나온다(값 클래스·enum 왕복)
- 저장분이 없으면 `null`
- 복호화 실패 → `null` + 저장분 폐기
- 저장분의 provider가 알 수 없는 값 → `UNKNOWN`

**`MemberRepositoryImpl`** (`data/src/test/`)
- `refreshMyAccount` 성공 → local 저장 + 반환값 일치
- `refreshMyAccount` 실패 → **local 유지**(낡은 값이라도 지우지 않는다)
- `changeGlobalNickname` 성공 → local 닉네임만 갱신, `memberId`·provider 불변
- `changeGlobalNickname` 실패 → local 불변

**`BootstrapSessionUseCase`** (`domain/src/test/`)
- 토큰 없음 → `ToLogin`, 서버 호출 0건
- 토큰 있음 + 성공 → `ToGroupList`
- 토큰 있음 + 인증 거절(401 / `MEMBER_NOT_FOUND`) → `ToLogin` + 토큰·userInfo clear 각 1회
- 토큰 있음 + 네트워크 실패 → `ToLogin` + clear 0건
- 토큰 있음 + 5xx·예상 밖 실패 → `ToLogin` + clear 0건

**`LogoutUseCase`** — 토큰 clear와 userInfo clear가 **둘 다** 불린다

**`AppSettingViewModel`·`AccountInfoViewModel`**
- SSoT가 값을 내보내면 상태에 반영된다
- SSoT가 `null`이면 로딩 상태
- 닉네임 변경 성공 → SSoT 갱신이 상태로 되돌아온다
- 변경 요청 중 버튼 비활성, 연타 1회
- 바꾼 것 없이 뒤로가기 → 묻지 않고 나간다
- 고친 뒤 뒤로가기 → 확인을 묻고 아직 나가지 않는다
- `그만두기` → 입력이 서버 값으로 돌아가고 나간다 / `취소하기` → 닫히고 입력이 남는다
- 확인 버튼 활성 조건이 서버 값과 다를 때만 성립한다

## 주의 / 열린 질문

- **오프라인 진입이 로그인 화면으로 간다.** 위 부트스트랩 표의 ⚠️ 참고. 그룹 목록을 캐시로
  그릴 수단이 생기면 "토큰이 있고 네트워크만 실패한 경우"를 `ToGroupList`로 돌리는 것을 재검토
- **`GroupListUiState.nickName` mock은 남는다.** 이번 범위 밖이라 의도적이다 — SSoT가 생긴 뒤에도
  한 화면이 계속 mock을 들고 있다는 사실이 다음 라운드까지 눈에 보여야 한다
- **`refreshMyAccount` 실패 시 낡은 값을 계속 보여준다.** 사용자에게 "이 값이 낡았다"를 알릴
  수단이 없다. 화면에 표시할지 미정
- 서버 `LoginProvider`에 `GOOGLE`이 있는데 core enum에 없다(`api/member.md`). 구글 로그인 경로가
  생기면 `UNKNOWN`으로 떨어진다 — 표시 문구가 그 상황에 적절한지 확인 필요
- **S-102 그룹 닉네임에도 같은 수정 취소 확인 모달이 디자인돼 있으나 구현은 뒤로가기에
  포커스만 내린다.** S-002만 이번에 맞췄다 — 두 화면의 뒤로가기 동작이 갈라져 있다
