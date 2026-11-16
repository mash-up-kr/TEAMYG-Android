---
id: notice-read-per-account
title: 공지 읽음 기록의 계정별 저장 (Notice Read State per Account)
status: in-progress
category: data-flow
platforms: android
verified: 2026-11-16
related_code:
  - NoticeReadLocalDataSourceImpl.kt#NoticeReadLocalDataSourceImpl
  - NoticeRepositoryImpl.kt#NoticeRepositoryImpl
  - WithdrawUseCase.kt#WithdrawUseCase
  - LogoutUseCase.kt#LogoutUseCase
  - CryptoManager.kt#CryptoManager
related_adr:
related_spec: notice-server-integration
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait]
---

# Spec: 공지 읽음 기록의 계정별 저장

> 이슈 #714. 상태·날짜는 위 frontmatter가 단일 출처. [공지사항 서버 연동 스펙](archive/2026-11-11-notice-server-integration-design.md)의 "읽음은 기기 단위로 유지한다"와 제외 항목 "계정별 읽음 분리"를 뒤집는다 — 그 문서의 해당 문장은 구현 때 함께 고친다.

## 목표

공지 읽음 기록(NEW 칩 판정의 근거)을 **계정별로** 저장한다. 같은 기기에서 같은 계정으로 다시 로그인하면 기록이 이어지고, 다른 계정으로 로그인하면 0부터 시작한다. 그리고 저장된 Preferences 파일만 열어서는 `memberId`를 알 수 없고 유추할 수도 없게 한다.

## 범위

- 포함: 읽음 id 저장 키를 계정 별칭으로 나누기, 별칭 생성기(Keystore HMAC), `myAccount`에 묶인 `readNoticeIds`/`markRead`, 탈퇴 시 해당 계정 기록 삭제, 문서 갱신.
- 제외: `PastCanvasAlert`(OQ-P-397) — 별도 이슈. 별칭 생성기를 재사용할 수 있게만 만든다. 옛 키 `notice_read_ids`의 마이그레이션·삭제 — 이 키가 들어간 커밋이 아직 어떤 릴리스 태그에도 없어(최신 `1.3.0` 기준) 실사용자 기기에 없다. 읽음 상태를 서버에 두는 방안. 루팅 기기·런타임 후킹에 대한 방어(보호 범위는 **저장된 파일**이다).

## 배경

- 지금은 키 하나 `notice_read_ids`(stringSet)에 읽은 공지 id가 전부 들어 있고, 로그아웃·세션 만료·탈퇴 모두 이 키를 지우지 않는다. 그래서 다른 계정으로 로그인하면 이전 계정의 읽음 상태를 물려받는다.
- 로그아웃 때 지우면 같은 계정으로 다시 들어와도 NEW가 다시 뜬다. 그래서 지우지 않고 **계정마다 나눠 두는** 쪽을 택한다.
- 계정 식별자는 이미 있다. `MyAccountVO.memberId`(`MemberId`)가 로그인·가입·부트스트랩·닉네임 변경에서 `RefreshMyAccountUseCase`로 채워진다. 갱신이 실패하면 `null`일 수 있다.
- `memberId`는 작은 정수라 단순 해시는 1부터 대입해 역산된다. 기기 밖으로 나가지 않는 비밀 키가 섞여야 한다.

## API / 인터페이스

```kotlin
// data/security — 신규. CryptoManager 와 같은 규약(@Singleton @Inject 구체 클래스)
class AccountKeyAlias @Inject constructor() {
    /** 같은 (scope, memberId) 이면 이 기기에서 항상 같은 값, 다르면 다른 값. 원본 id 는 복원할 수 없다 */
    fun alias(scope: String, memberId: MemberId): String
}

// data/source/notice/local — 시그니처 변경
interface NoticeReadLocalDataSource {
    fun readNoticeIds(memberId: MemberId): Flow<Set<Long>>
    suspend fun markRead(memberId: MemberId, id: Long)
    suspend fun clear(memberId: MemberId)
}

// domain — 추가. readNoticeIds / markRead(id) 는 그대로
interface NoticeRepository {
    suspend fun clearReadNotices()
}
```

- 별칭: Keystore에 둔 non-exportable HMAC-SHA256 키 `parfait_alias_key`(`PURPOSE_SIGN`)로 `"$scope:${memberId.value}"`를 서명하고 앞 16자 hex를 쓴다. 공지의 scope는 `"notice-read"`다. scope를 섞는 이유는 같은 계정의 다른 용도(예: `PastCanvasAlert`)와 별칭이 겹치지 않게 하려는 것이다.
- 저장 키: `stringSetPreferencesKey("notice_read_$alias")`.
- 도메인 인터페이스의 `readNoticeIds`, `markRead(id)`는 바뀌지 않는다 — UseCase·ViewModel·화면은 손대지 않는다.

## 동작 / 상태

- **읽기**: `NoticeRepositoryImpl.readNoticeIds`는 `UserInfoLocalDataSource.myAccount`(이미 data에 있는 소스. Repository 사이 의존을 피한다)를 `map { it?.memberId }.distinctUntilChanged().flatMapLatest`로 받아, 계정이 있으면 그 계정의 키를, 없으면 빈 집합을 낸다.
- **쓰기**: `markRead(id)`는 현재 `memberId`를 읽어 소스에 넘긴다. 계정이 `null`이면 쓰지 않고 조용히 건너뛴다 — 잘못된 계정에 귀속시키는 것보다 NEW가 한 번 더 보이는 쪽이 낫다.
- **계정 전환·재로그인**: `myAccount`가 바뀌면 같은 흐름이 다른 키를 읽는다. 로그인·로그아웃·세션 만료·웹 로그인에 따로 지우는 분기는 없다.
- **탈퇴**: `WithdrawUseCase`가 서버 탈퇴가 성공한 뒤 `logout()` **앞에서** `clearReadNotices()`를 부른다. `logout()`이 `myAccount`를 비우므로 순서가 바뀌면 지울 키를 알 수 없다. 이 삭제가 실패해도 로그아웃은 진행한다. 서버 탈퇴가 실패하면 아무것도 지우지 않는다(기존 계약).
- **오류**: 별칭 생성(Keystore) 실패 시 읽기는 빈 집합, 쓰기는 조용히 무시한다(`data`에는 로깅 도구가 없다). 읽음 기록은 편의 기능이라 화면 진행을 막지 않는다.
- **`myAccount`가 로그인 중에 `null`인 경우**(계정 갱신 실패): NEW 칩이 일시적으로 많이 보일 수 있다. 감수한다.

## 표시·제어 규칙

화면 변경 없음. NEW 판정(`GetNoticeItemsUseCase`: 게시 14일 미만 & 미열람)은 그대로다.

## 파일 구성

- `data/.../security/AccountKeyAlias.kt` — 신규. 별칭 생성.
- `data/.../source/notice/local/NoticeReadLocalDataSource(.Impl).kt` — 계정 인자 추가, `clear` 추가.
- `data/.../repository/notice/NoticeRepositoryImpl.kt` — `myAccount` 구독, `clearReadNotices`.
- `domain/.../repository/notice/NoticeRepository.kt` — `clearReadNotices` 추가, KDoc 갱신.
- `domain/.../usecase/member/WithdrawUseCase.kt`(+ `LogoutUseCase.kt` KDoc) — 탈퇴만 읽음 기록을 지운다는 점.
- DI: `AccountKeyAlias`는 `@Inject` 생성자라 모듈 변경 없음. `LocalDataSourceModule`의 바인딩은 그대로.

## 테스트

- `NoticeReadLocalDataSourceImpl`(FakePreferencesDataStore, `AccountKeyAlias` mock): 같은 계정이면 유지, 다른 계정이면 격리, `clear`가 해당 계정만 지움, **저장된 키 이름에 원본 `memberId` 숫자가 들어가지 않음**.
- `NoticeRepositoryImpl`: 계정 전환 시 다른 집합, 같은 계정 재로그인 시 유지, `myAccount == null`이면 빈 집합·`markRead` 건너뜀, `clearReadNotices`가 현재 계정만 지움.
- `WithdrawUseCase`: 성공 시 `clearReadNotices` → `logout` 순서, 삭제 실패에도 `logout` 진행, 서버 실패 시 둘 다 부르지 않음.
- `AccountKeyAlias`: 계측 테스트(API 28 에뮬레이터). 같은 입력이면 같은 별칭, 다른 id·scope면 다른 별칭, 출력에 원본 id가 들어 있지 않음. JVM에는 AndroidKeyStore가 없어 단위 테스트에서는 `CryptoManager`처럼 mock으로 쓴다.

## 문서 갱신

- [공지사항 서버 연동 스펙](archive/2026-11-11-notice-server-integration-design.md): 읽음 저장소 설명("기기 단위")·제외 항목("계정별 읽음 분리")·결정 문장("읽음은 기기 단위로 유지한다")을 계정 단위로 고친다.
- `docs/status.md` 공지 절의 "읽은 id는 지우지 않는다"를 새 동작으로 바꾼다.
- `docs/log.md`에 한 줄 추가한다.
- `docs/synthesis/open-questions.md` OQ-P-397에 "`AccountKeyAlias`로 해소할 수 있다"는 포인터를 단다.
- `docs/superpowers/specs/README.md` 인덱스에 이 스펙을 올린다.

## 주의 / 열린 질문

- 보호 범위는 **저장된 파일만 열었을 때**다. 키 개수로 이 기기에서 쓴 계정 수는 대략 드러난다.
- 백업·복원: `allowBackup=true`라 평문 값이 복원될 수 있지만, Keystore 키는 복원되지 않아 새 기기에서는 별칭이 달라진다 — 복원된 기록은 어느 계정과도 맞지 않고 사실상 초기화된다. 의도한 동작이다.
- 같은 문제를 가진 `PastCanvasAlert`(OQ-P-397)는 별도 이슈로 이어서 `AccountKeyAlias(scope = ...)`로 푼다.
