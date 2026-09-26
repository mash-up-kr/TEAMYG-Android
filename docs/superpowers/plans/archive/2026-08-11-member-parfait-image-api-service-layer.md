# member·parfait-image API Service·DataSource 레이어 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 서버 member 2 + parfait-image 2 엔드포인트를 TJYG-Android `:data`의 Retrofit Service와 remote DataSource로 배선하고 대응 domain VO를 만든다. 같은 라운드에서 매퍼 단독 테스트 2건을 케이스 이관 후 삭제해 새 테스트 규약을 브랜치에 안착시킨다.

**Architecture:** `Service`(Retrofit·wire DTO) → `RemoteDataSource`(`ApiCaller` + mapper) → `domain VO`. 앞선 라운드들([2026-08-03-data-api-service-layer](../../specs/archive/2026-08-03-data-api-service-layer.md)·[2026-08-10-image-api-service-layer](../../specs/archive/2026-08-10-image-api-service-layer.md))이 세운 관용구의 증분이다. 관용구가 답하지 않는 지점은 하나 — 배치 요청 필드 3개가 서로 얽혀 있어(`borderType=SOLID`면 색·두께 필수) domain 쪽만 sealed로 묶는다.

**Tech Stack:** Kotlin 2.4.0 · Retrofit2 3.0.0 · kotlinx-serialization 1.11.0 · Hilt · MockK 1.14.11 · kotlinx-coroutines-test · kotlin.test

**작업 저장소:** TJYG-Android (`mash-up-kr/TJYG-Android`). 로컬 절대경로는 private submodule `wiki/personal-private/project-paths.md` 참고.

**브랜치:** `feature/sync-backend-api-260810`(PR #229) **위에** 새 브랜치를 판다. 배치 요청이 그 PR의 `ImageId`를 받고 `ServiceModule`·`RemoteDataSourceModule`·`http/` 파일이 겹치기 때문이다.

**스펙:** [specs/2026-08-11-member-parfait-image-api-service-layer.md](../../specs/archive/2026-08-11-member-parfait-image-api-service-layer.md)

**계약 정본:** [api/member.md](../../../api/member.md) · [api/parfait-image.md](../../../api/parfait-image.md) (서버 기준선 `2c5499a`)

> **완료 — PR #230 `develop` 머지(2026-08-12).** Task 1~5 전량 반영. 계획이 "PR #229 위에 판다"고 적은
> 대로 브랜치가 쌓였고, 결말은 **#229가 따로 머지되지 않고 두 라운드가 `feature/sync-backend-api-260810`
> 하나로 나간 것**이다. 머지본 재대조에서 **계획과 갈린 곳 0건** — Service 4·DTO 6·domain 11·DataSource
> 2쌍·DI 4줄·`http/` 2파일이 File Structure 표 그대로이고, 최종 검증의
> `find … -name "*MapperTest.kt"` 기대(출력 없음)도 develop에서 성립한다.
> **산출물 계약의 정본은 스펙**이다 — [specs/archive](../../specs/archive/2026-08-11-member-parfait-image-api-service-layer.md).
> 계획 본문 체크박스는 실행 기록을 이 블록에 모으는 관례대로 미체크로 둔다.
>
> **Task 5의 `http/README.md` 정정 ①이 develop 안에서 자기모순을 만들었다** — README는 이제 판별자 키를
> `isNewUser`로 적는데, **같은 디렉토리의 `http/auth.http` 주석은 여전히 `newUser`가 맞다고 가르치고**
> 응답 핸들러도 `response.body.data.newUser`를 읽는다. 앱 `KakaoLoginResponse`의 `@SerialName("newUser")`도
> 그대로다(계획 범위 밖이라 정상이지만, 이제 셋 중 하나만 고쳐진 상태다)
> → [open-questions](../../../synthesis/open-questions.md).

## Global Constraints

- **커밋은 Task 경계마다 한다.** TJYG-Android 저장소에 **로컬 커밋만** 하고 **push·PR은 하지 않는다** — 사용자 승인 게이트다.
- **ktlint**: `max_line_length = 120`, `ktlint_code_style = android_studio`. 각 Task의 마지막 검증에 `./gradlew ktlintCheck`가 포함된다.
- **테스트 함수명에 백틱을 쓰지 않는다.** 형식은 `메서드명_조건_기대결과()`. 저장소 관용구다(`PolicyRemoteDataSourceImplTest`·`ImageRemoteDataSourceImplTest`).
- **Given/When/Then 주석은 한국어**로 단다(같은 관용구).
- **매퍼 단독 테스트 파일을 만들지 않는다.** 판단이 든 변환은 그 매퍼를 통과시키는 DataSource 테스트의 케이스로 잠근다 — [unit-test-infrastructure](../../specs/archive/2026-08-06-unit-test-infrastructure.md) "테스트 규약" 11(2026-08-11 개정)·[architecture/data-layer](../../../architecture/data-layer.md) "응답 매핑".
- **DTO에 value class·enum·sealed를 넣지 않는다.** wire 형태는 raw 타입(`Long`·`String`·`Double`·`Int`)만 쓰고, 감싸고 벗기는 일은 mapper가 한다.
- **`@SerialName`을 전 필드에 명시한다.** 이름이 그대로여도 붙이는 것이 기존 관용구다.
- **`ToppingTransform` 생성은 항상 named argument로 한다.** `Double` 넷이 연속이라 위치 인자로 만들면 순서를 뒤바꿔도 컴파일이 통과한다 — 이 타입을 도입한 이유가 사라진다.
- **suspend 함수는 `coEvery`/`coVerify`로 stub·verify한다.** `every`/`verify`를 쓰면 `Continuation` 인자가 매칭되지 않아 `MockKException`이 난다.
- **`@NoAuth`를 붙이지 않는다.** 이번 4 엔드포인트 전부 서버 화이트리스트 밖이라 access token이 필요하다.
- **범위 밖**: Repository·`domain/repository` 인터페이스, UseCase, 화면 결선, 서버 에러 코드의 도메인 예외 번역, 요청 전 클라이언트 유효성 검사, 애플 로그인(Android가 쓰지 않기로 결정).

> **TDD 순서에 대한 메모(리뷰어용).** Task 2·3에서 domain 타입·wire DTO·mapper·인터페이스가 테스트보다 **앞선다**. 이것들은 로직이 아니라 **테스트가 컴파일되기 위한 선언**이고(데이터 홀더·`@Serializable` DTO·`when` 한 줄짜리 파싱), 실제 검증 대상인 `RemoteDataSourceImpl`은 테스트 → 실패 확인 → 구현 → 통과 확인 순서를 지킨다. 선언을 하나씩 테스트로 몰아가면 "필드가 있는지" 같은 컴파일러가 이미 답하는 것을 단언하게 되는데, 그건 이번 라운드가 지운 매퍼 테스트와 같은 종류다.

---

## File Structure

| 파일 | 책임 | Task |
|---|---|---|
| `data/src/test/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSourceImplTest.kt` | 매퍼 판단 케이스 흡수(타입 폴백·대소문자·필드 배선) | 1 |
| `data/src/test/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImplTest.kt` | 매퍼 판단 케이스 흡수(상태 폴백·대소문자) | 1 |
| ~~`data/src/test/.../policy/mapper/PolicyVOMapperTest.kt`~~ | 삭제 | 1 |
| ~~`data/src/test/.../image/mapper/ImageVOMapperTest.kt`~~ | 삭제 | 1 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/member/GlobalNickname.kt` | 전역 닉네임 value class | 2 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/member/LoginProvider.kt` | 로그인 수단 enum(`UNKNOWN` 폴백) | 2 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/member/MyAccountVO.kt` | 내 계정 조회 결과 VO | 2 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/request/member/ChangeGlobalNicknameRequest.kt` | 닉네임 변경 요청 wire DTO | 2 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/member/MyAccountResponse.kt` | 계정 조회 응답 wire DTO | 2 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/member/ChangeGlobalNicknameResponse.kt` | 닉네임 변경 응답 wire DTO | 2 |
| `data/src/main/java/com/teamyg/parfait/data/source/member/mapper/VOMapper.kt` | DTO → VO 변환(결정: provider 파싱) | 2 |
| `data/src/main/java/com/teamyg/parfait/data/service/MemberService.kt` | Retrofit 인터페이스 | 2 |
| `data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSource.kt` | 의미 기반 인터페이스 | 2 |
| `data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImpl.kt` | `ApiCaller` 경유 구현 | 2 |
| `data/src/test/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImplTest.kt` | 성공·실패 경로 + provider 판단 + 요청 바디 | 2 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/id/ParfaitId.kt` | 파르페 식별자 value class | 3 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/id/ParfaitImageId.kt` | 배치 식별자 value class | 3 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/id/GroupMemberId.kt` | 그룹 멤버십 식별자 value class | 3 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingTransform.kt` | 위치·크기·각도 묶음 | 3 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingBorder.kt` | 테두리 sealed(불가능한 상태 제거) | 3 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/topping/PlacedToppingVO.kt` | 배치 확정 결과 VO(+`ToppingPlacerVO`) | 3 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/PlaceParfaitImageRequest.kt` | 배치 요청 wire DTO(평면 9필드) | 3 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/PlaceParfaitImageResponse.kt` | 배치 응답 wire DTO(+`PlacedByResponse`) | 3 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt` | sealed → 평면 3필드, 응답 → VO | 3, 4 |
| `data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt` | Retrofit 인터페이스 | 3, 4 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt` | 의미 기반 인터페이스 | 3, 4 |
| `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImpl.kt` | `ApiCaller` 경유 구현 | 3, 4 |
| `data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt` | border 변환·transform 배선·중첩 매핑·실패 경로 | 3, 4 |
| `domain/src/main/java/com/teamyg/parfait/domain/model/topping/UpdatedToppingVO.kt` | 수정 결과 VO | 4 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageRequest.kt` | 수정 요청 wire DTO(5필드 nullable) | 4 |
| `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImageResponse.kt` | 수정 응답 wire DTO | 4 |
| `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt` | `provideMemberService`(2) · `provideParfaitImageService`(3) | 2, 3 |
| `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt` | `bindMemberRemoteDataSource`(2) · `bindParfaitImageRemoteDataSource`(3) | 2, 3 |
| `http/users.http` | member 계약 확인 요청 모음 | 5 |
| `http/parfait-image.http` | parfait-image 계약 확인 요청 모음 | 5 |
| `http/README.md` | 파일 목록 갱신 | 5 |

**Task 경계 근거.** Task 1은 **규약**이 산출물이라 리뷰어가 "이관이 충분한가"만 보고 판정할 수 있다. Task 2·3·4는 각각 하나의 **호출 경로**가 산출물이고 서로 독립적으로 반려 가능하다 — 3(POST, sealed→평면 변환)과 4(PATCH, 부분 수정 null 의미)는 잠그는 규칙이 다르다. Task 5는 코드가 아니라 수동 확인 수단이라 따로 뗀다.

---

## Task 1: 매퍼 테스트 케이스 이관 후 삭제

매퍼 단독 테스트 금지 규약(2026-08-11 개정)을 이 브랜치에 안착시킨다. **삭제 전에 이관이 먼저다** — 규약이 "검증을 줄이자"가 아니라 "한 곳에서 하자"이므로 옮기지 않고 지우면 규약 위반이다.

**Files:**
- Modify: `data/src/test/java/com/teamyg/parfait/data/source/policy/remote/PolicyRemoteDataSourceImplTest.kt`
- Modify: `data/src/test/java/com/teamyg/parfait/data/source/image/remote/ImageRemoteDataSourceImplTest.kt`
- Delete: `data/src/test/java/com/teamyg/parfait/data/source/policy/mapper/PolicyVOMapperTest.kt`
- Delete: `data/src/test/java/com/teamyg/parfait/data/source/image/mapper/ImageVOMapperTest.kt`

**Interfaces:**
- Consumes: 없음(기존 코드만 만진다)
- Produces: 없음. 이후 Task들은 이 Task가 세운 **규약**만 물려받는다 — 새 매퍼 테스트 파일을 만들지 않는다.

**이관 목록(정확히 이것만 옮긴다).** `ImageVOMapperTest`의 `expiresIn` 초 해석과 두 URL 배선은 **이미 `ImageRemoteDataSourceImplTest.issueUploadUrl_serviceReturnsSuccess_returnsMappedVo`가 단언**하고 있어 새로 옮길 것이 없다. 실제로 비는 것은 아래 넷이다.

| 원본 | 옮길 곳 | 내용 |
|---|---|---|
| `PolicyVOMapperTest.toPolicyVO_unknownType_fallsBackToUnknown` | `PolicyRemoteDataSourceImplTest` | 미지 타입 → `UNKNOWN` |
| `PolicyVOMapperTest.toPolicyVO_typeMatchIsCaseSensitive` | `PolicyRemoteDataSourceImplTest` | 대소문자 민감성 |
| `PolicyVOMapperTest.toPolicyVO_mapsEveryField` | `PolicyRemoteDataSourceImplTest` | `title`/`url` 배선(둘 다 `String`) |
| `ImageVOMapperTest.toConfirmedImageVO_unknownStatus_fallsBackToUnknown` + `_statusMatchIsCaseSensitive` + `_mapsKnownStatus`의 `PENDING` | `ImageRemoteDataSourceImplTest` | 상태 폴백·대소문자·`PENDING` 매핑 |

- [ ] **Step 1: `PolicyRemoteDataSourceImplTest`에 이관 케이스 3건을 추가한다(아직 실패한다)**

파일 맨 아래 `getPolicies_unexpectedException_returnsUnknownException` 뒤, 클래스 닫는 중괄호 앞에 추가한다.

```kotlin
    @Test
    fun getPolicies_unknownType_fallsBackToUnknown() = runTest {
        // Given 클라이언트가 모르는 타입 문자열
        coEvery { policyService.getPolicies() } returns successResponse("MARKETING_CONSENT")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(PolicyType.UNKNOWN, result.getOrThrow().single().type)
    }

    @Test
    fun getPolicies_typeMatchIsCaseSensitive() = runTest {
        // Given 값은 맞지만 대소문자가 다른 타입
        coEvery { policyService.getPolicies() } returns successResponse("terms_of_service")

        // When 정책 조회
        val result = dataSource.getPolicies()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(PolicyType.UNKNOWN, result.getOrThrow().single().type)
    }

    @Test
    fun getPolicies_mapsEveryFieldOfEachItem() = runTest {
        // Given title 과 url 이 서로 다른 값인 약관 한 건
        coEvery { policyService.getPolicies() } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = PolicyResponse(
                policies = listOf(
                    PolicyItemResponse(
                        termsId = 7L,
                        type = "PRIVACY_POLICY",
                        title = "개인정보 처리방침",
                        url = "https://example.com/privacy",
                        required = false,
                    ),
                ),
            ),
        )

        // When 정책 조회
        val vo = dataSource.getPolicies().getOrThrow().single()

        // Then 모든 필드가 제자리에 들어간다 (title 과 url 은 둘 다 String 이라 뒤바뀌어도 컴파일된다)
        assertEquals(TermsId(7L), vo.termsId)
        assertEquals(PolicyType.PRIVACY_POLICY, vo.type)
        assertEquals("개인정보 처리방침", vo.title)
        assertEquals("https://example.com/privacy", vo.url)
        assertEquals(false, vo.required)
    }
```

import에 `com.teamyg.parfait.domain.model.id.TermsId`를 추가한다(나머지 `ApiResponse`·`PolicyResponse`·`PolicyItemResponse`·`PolicyType`은 이미 import돼 있다).

- [ ] **Step 2: 추가한 3건이 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*PolicyRemoteDataSourceImplTest*"`
Expected: PASS. **여기서 실패하면 안 된다** — 매퍼는 이미 구현돼 있고 이 Step은 "DataSource 경로로도 같은 판단이 잡히는가"를 확인하는 것이다. 실패하면 매퍼 동작이 매퍼 테스트가 기술한 것과 다르다는 뜻이므로 멈추고 보고한다.

- [ ] **Step 3: `ImageRemoteDataSourceImplTest`에 이관 케이스 3건을 추가한다**

파일 맨 아래 `confirmUpload_successButNullData_returnsEmptyBodyException` 뒤, 클래스 닫는 중괄호 앞에 추가한다.

```kotlin
    @Test
    fun confirmUpload_pendingStatus_mapsToPending() = runTest {
        // Given 서버가 PENDING 상태를 준다
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(status = "PENDING")

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then PENDING enum 으로 떨어진다
        assertEquals(ImageStatus.PENDING, result.getOrThrow().status)
    }

    @Test
    fun confirmUpload_unknownStatus_fallsBackToUnknown() = runTest {
        // Given 클라이언트가 모르는 상태 문자열
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(status = "FAILED")

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(ImageStatus.UNKNOWN, result.getOrThrow().status)
    }

    @Test
    fun confirmUpload_statusMatchIsCaseSensitive() = runTest {
        // Given 값은 맞지만 대소문자가 다른 상태
        coEvery { imageService.postImagesByImageIdConfirm(any()) } returns confirmSuccess(status = "completed")

        // When 업로드 확인
        val result = dataSource.confirmUpload(ImageId(7L))

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(ImageStatus.UNKNOWN, result.getOrThrow().status)
    }
```

import 추가 없음(`ImageStatus`·`ImageId`는 이미 있다).

- [ ] **Step 4: 추가한 3건이 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ImageRemoteDataSourceImplTest*"`
Expected: PASS (Step 2와 같은 이유).

- [ ] **Step 5: 매퍼 테스트 파일 2개를 삭제한다**

```bash
rm data/src/test/java/com/teamyg/parfait/data/source/policy/mapper/PolicyVOMapperTest.kt
rm data/src/test/java/com/teamyg/parfait/data/source/image/mapper/ImageVOMapperTest.kt
```

- [ ] **Step 6: 매퍼 테스트가 0건인지 확인한다**

Run: `find . -path ./build -prune -o -name "*MapperTest.kt" -print`
Expected: 출력 없음.

- [ ] **Step 7: 전체 유닛 테스트와 ktlint를 돌린다**

Run: `./gradlew test ktlintCheck`
Expected: PASS. 삭제한 두 파일이 유일한 사용처였던 import·헬퍼가 남아 컴파일이 깨지지 않는지 여기서 잡힌다.

- [ ] **Step 8: 커밋**

```bash
git add -A data/src/test
git commit -m "test: 매퍼 단독 테스트를 DataSource 테스트로 이관 후 삭제

매퍼의 유일한 호출자가 DataSource라 두 파일이 같은 변환을 두 번 검증했다.
판단이 든 케이스(타입·상태 폴백, 대소문자 민감성, 같은 타입 필드 배선)를
각 DataSource 테스트로 옮긴 뒤 PolicyVOMapperTest·ImageVOMapperTest를 지운다."
```

---

## Task 2: member 슬라이스

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/member/GlobalNickname.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/member/LoginProvider.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/member/MyAccountVO.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/member/ChangeGlobalNicknameRequest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/member/MyAccountResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/member/ChangeGlobalNicknameResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/member/mapper/VOMapper.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/MemberService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `ApiCaller#safeApiCall(block, transform)` · `ApiResponse<T>(success, code, message, data, errorDetail)` · `ApiException.Business(code, serverMessage, statusCode, errorDetail)` · `MemberId(value: Long)`(`domain/model/id`, 기존)
- Produces:
  - `GlobalNickname(val value: String)` — value class
  - `LoginProvider { KAKAO, APPLE, UNKNOWN }`
  - `MyAccountVO(memberId: MemberId, provider: LoginProvider, nickname: GlobalNickname)`
  - `MemberRemoteDataSource#getMyAccount(): Result<MyAccountVO>`
  - `MemberRemoteDataSource#changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>`

- [ ] **Step 1: domain 타입 3개를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/member/GlobalNickname.kt`:
```kotlin
package com.teamyg.parfait.domain.model.member

/**
 * 계정 하나당 하나인 전역 닉네임. 그룹 안에서 쓰는 이름은 GroupNickname 으로 별개다.
 *
 * 서버 유효성 규칙은 GroupNickname 과 문자 그대로 같지만(1~15자, 한글·영문·숫자,
 * 단어 사이 한 칸 공백) 타입을 합치지 않는다. 합치면 전역 닉네임을 그룹 API 에
 * 그대로 넘기는 실수가 컴파일을 통과한다. 검증은 서버가 하며 이 타입은 감싸기만 한다.
 */
@JvmInline
value class GlobalNickname(val value: String)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/member/LoginProvider.kt`:
```kotlin
package com.teamyg.parfait.domain.model.member

/**
 * 서버가 주는 값이라 UNKNOWN 폴백을 둔다. enumValueOf 로 바꾸면 서버가 provider 를
 * 하나 늘리는 순간 크래시한다. 실제로 서버 영속 계층에는 GOOGLE 이 있는데 core enum 에는
 * 없는 상태다(`api/member.md`).
 */
enum class LoginProvider {
    KAKAO,
    APPLE,
    UNKNOWN,
}
```

`domain/src/main/java/com/teamyg/parfait/domain/model/member/MyAccountVO.kt`:
```kotlin
package com.teamyg.parfait.domain.model.member

import com.teamyg.parfait.domain.model.id.MemberId

data class MyAccountVO(
    val memberId: MemberId,
    val provider: LoginProvider,
    val nickname: GlobalNickname,
)
```

- [ ] **Step 2: wire DTO 3개를 만든다**

`data/src/main/java/com/teamyg/parfait/data/service/model/request/member/ChangeGlobalNicknameRequest.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.request.member

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param nickname 빈 문자열은 서버 @NotBlank 에 걸려 400 INVALID_REQUEST 이고,
 *   형식 위반(연속 공백·허용 밖 문자·16자 이상)은 400 INVALID_NICKNAME 이다.
 *   코드가 갈리므로 소비 측이 두 실패를 같은 것으로 뭉개지 않도록 주의한다.
 */
@Serializable
data class ChangeGlobalNicknameRequest(
    @SerialName("nickname")
    val nickname: String,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/response/member/MyAccountResponse.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.response.member

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param provider LoginProvider 이름 문자열. 매퍼가 enum 으로 바꾸며 모르는 값은 UNKNOWN 이다.
 */
@Serializable
data class MyAccountResponse(
    @SerialName("memberId")
    val memberId: Long,
    @SerialName("provider")
    val provider: String,
    @SerialName("nickname")
    val nickname: String,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/response/member/ChangeGlobalNicknameResponse.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.response.member

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeGlobalNicknameResponse(
    @SerialName("nickname")
    val nickname: String,
)
```

- [ ] **Step 3: mapper를 만든다**

`data/src/main/java/com/teamyg/parfait/data/source/member/mapper/VOMapper.kt`:
```kotlin
package com.teamyg.parfait.data.source.member.mapper

import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO

internal fun MyAccountResponse.toMyAccountVO(): MyAccountVO = MyAccountVO(
    memberId = MemberId(memberId),
    provider = provider.toLoginProvider(),
    nickname = GlobalNickname(nickname),
)

internal fun ChangeGlobalNicknameResponse.toGlobalNickname(): GlobalNickname = GlobalNickname(nickname)

private fun String.toLoginProvider(): LoginProvider = when (this) {
    LoginProvider.KAKAO.name -> LoginProvider.KAKAO
    LoginProvider.APPLE.name -> LoginProvider.APPLE
    else -> LoginProvider.UNKNOWN
}
```

- [ ] **Step 4: Service와 DataSource 인터페이스를 만든다**

`data/src/main/java/com/teamyg/parfait/data/service/MemberService.kt`:
```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

/**
 * 두 엔드포인트 모두 서버 화이트리스트 밖이라 access token 이 필요하다. @NoAuth 를 붙이지 않는다.
 * 대상 회원은 요청이 아니라 토큰에서 정해지므로 경로 변수도 바디 필드도 없다.
 */
interface MemberService {
    @GET("api/v1/users/me")
    suspend fun getUsersMe(): ApiResponse<MyAccountResponse>

    @PATCH("api/v1/users/me/nickname")
    suspend fun patchUsersMeNickname(
        @Body request: ChangeGlobalNicknameRequest,
    ): ApiResponse<ChangeGlobalNicknameResponse>
}
```

`data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSource.kt`:
```kotlin
package com.teamyg.parfait.data.source.member.remote

import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO

interface MemberRemoteDataSource {
    /**
     * 토큰이 가리키는 회원의 계정 정보를 읽는다.
     *
     * MEMBER_NOT_FOUND 가 401(전역 인증 필터)과 404(서비스) 둘 다로 올 수 있다.
     * code 문자열만으로 분기하면 두 상황이 뭉개지므로, 이 계층은 번역하지 않고
     * ApiException.Business 로 그대로 흘린다.
     */
    suspend fun getMyAccount(): Result<MyAccountVO>

    /**
     * 전역 닉네임을 바꾼다. 이미 참여한 그룹의 그룹 닉네임은 바뀌지 않는다 — 별도 컬럼이고
     * 서버가 이 API 에서 건드리지 않는다(`api/member.md`).
     */
    suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname>
}
```

- [ ] **Step 5: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImplTest.kt`:
```kotlin
package com.teamyg.parfait.data.source.member.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.MemberService
import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemberRemoteDataSourceImplTest {
    private val memberService: MemberService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = MemberRemoteDataSourceImpl(
        memberService = memberService,
        apiCaller = apiCaller,
    )

    private fun accountSuccess(
        memberId: Long = 42L,
        provider: String = "KAKAO",
        nickname: String = "행복한 판다",
    ) = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = MyAccountResponse(memberId = memberId, provider = provider, nickname = nickname),
    )

    private fun nicknameSuccess(nickname: String = "부지런한 수달") = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = ChangeGlobalNicknameResponse(nickname = nickname),
    )

    @Test
    fun getMyAccount_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 계정 정보를 준다
        coEvery { memberService.getUsersMe() } returns
            accountSuccess(memberId = 42L, provider = "KAKAO", nickname = "행복한 판다")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then 모든 필드가 제자리에 들어간다
        assertEquals(MemberId(42L), vo.memberId)
        assertEquals(LoginProvider.KAKAO, vo.provider)
        assertEquals(GlobalNickname("행복한 판다"), vo.nickname)
    }

    @Test
    fun getMyAccount_appleProvider_mapsToApple() = runTest {
        // Given 서버가 애플 회원을 준다
        coEvery { memberService.getUsersMe() } returns accountSuccess(provider = "APPLE")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then APPLE enum 으로 떨어진다
        assertEquals(LoginProvider.APPLE, vo.provider)
    }

    @Test
    fun getMyAccount_unknownProvider_fallsBackToUnknown() = runTest {
        // Given 클라이언트가 모르는 provider 문자열 (서버 영속 계층에는 GOOGLE 이 있다)
        coEvery { memberService.getUsersMe() } returns accountSuccess(provider = "GOOGLE")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(LoginProvider.UNKNOWN, vo.provider)
    }

    @Test
    fun getMyAccount_providerMatchIsCaseSensitive() = runTest {
        // Given 값은 맞지만 대소문자가 다른 provider
        coEvery { memberService.getUsersMe() } returns accountSuccess(provider = "kakao")

        // When 계정 조회
        val vo = dataSource.getMyAccount().getOrThrow()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(LoginProvider.UNKNOWN, vo.provider)
    }

    @Test
    fun getMyAccount_memberNotFound_returnsBusinessException() = runTest {
        // Given envelope 의 success=false 응답
        coEvery { memberService.getUsersMe() } returns ApiResponse(
            success = false,
            code = "MEMBER_NOT_FOUND",
            message = "존재하지 않는 회원입니다",
            data = null,
        )

        // When 계정 조회
        val result = dataSource.getMyAccount()

        // Then Business 예외로 실패한다 (401 인지 404 인지는 이 계층이 판정하지 않는다)
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("MEMBER_NOT_FOUND", error.code)
    }

    @Test
    fun getMyAccount_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery { memberService.getUsersMe() } throws IOException("connection reset")

        // When 계정 조회
        val result = dataSource.getMyAccount()

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun changeGlobalNickname_serviceReturnsSuccess_returnsSavedNickname() = runTest {
        // Given 서비스가 저장된 닉네임을 준다
        coEvery { memberService.patchUsersMeNickname(any()) } returns nicknameSuccess("부지런한 수달")

        // When 닉네임 변경
        val result = dataSource.changeGlobalNickname(GlobalNickname("부지런한 수달"))

        // Then 저장된 값이 GlobalNickname 으로 돌아온다
        assertEquals(GlobalNickname("부지런한 수달"), result.getOrThrow())
    }

    @Test
    fun changeGlobalNickname_unwrapsValueClassForRequestBody() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<ChangeGlobalNicknameRequest>()
        coEvery { memberService.patchUsersMeNickname(capture(request)) } returns nicknameSuccess()

        // When value class 로 감싼 닉네임으로 변경 호출
        dataSource.changeGlobalNickname(GlobalNickname("부지런한 수달"))

        // Then 바디에는 raw String 이 들어간다 (Retrofit 경계에서 벗긴다)
        assertEquals("부지런한 수달", request.captured.nickname)
        coVerify(exactly = 1) { memberService.patchUsersMeNickname(any()) }
    }

    @Test
    fun changeGlobalNickname_invalidNickname_returnsBusinessException() = runTest {
        // Given 형식 위반 응답
        coEvery { memberService.patchUsersMeNickname(any()) } returns ApiResponse(
            success = false,
            code = "INVALID_NICKNAME",
            message = "닉네임 형식이 올바르지 않습니다",
            data = null,
        )

        // When 닉네임 변경
        val result = dataSource.changeGlobalNickname(GlobalNickname("연속  공백"))

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("INVALID_NICKNAME", error.code)
    }

    @Test
    fun changeGlobalNickname_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery { memberService.patchUsersMeNickname(any()) } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 닉네임 변경
        val result = dataSource.changeGlobalNickname(GlobalNickname("부지런한 수달"))

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.EmptyBody>(result.exceptionOrNull())
        assertEquals("SUCCESS", error.code)
    }
}
```

- [ ] **Step 6: 테스트가 컴파일에 실패하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*MemberRemoteDataSourceImplTest*"`
Expected: FAIL — `Unresolved reference: MemberRemoteDataSourceImpl`.

- [ ] **Step 7: 구현을 쓴다**

`data/src/main/java/com/teamyg/parfait/data/source/member/remote/MemberRemoteDataSourceImpl.kt`:
```kotlin
package com.teamyg.parfait.data.source.member.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.MemberService
import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.source.member.mapper.toGlobalNickname
import com.teamyg.parfait.data.source.member.mapper.toMyAccountVO
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO
import javax.inject.Inject

class MemberRemoteDataSourceImpl @Inject constructor(
    private val memberService: MemberService,
    private val apiCaller: ApiCaller,
) : MemberRemoteDataSource {
    override suspend fun getMyAccount(): Result<MyAccountVO> = apiCaller.safeApiCall(
        block = { memberService.getUsersMe() },
        transform = { it.toMyAccountVO() },
    )

    override suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname> =
        apiCaller.safeApiCall(
            block = {
                memberService.patchUsersMeNickname(
                    ChangeGlobalNicknameRequest(nickname = nickname.value),
                )
            },
            transform = { it.toGlobalNickname() },
        )
}
```

- [ ] **Step 8: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*MemberRemoteDataSourceImplTest*"`
Expected: PASS (10건).

- [ ] **Step 9: DI에 2줄을 추가한다**

`ServiceModule.kt` — import에 `com.teamyg.parfait.data.service.MemberService`를 더하고 object 안에 추가:
```kotlin
    @Provides
    @Singleton
    fun provideMemberService(retrofit: Retrofit): MemberService = retrofit.create(MemberService::class.java)
```

`RemoteDataSourceModule.kt` — import에 `com.teamyg.parfait.data.source.member.remote.MemberRemoteDataSource`와 `...MemberRemoteDataSourceImpl`을 더하고 interface 안에 추가:
```kotlin
    @Binds
    @Singleton
    fun bindMemberRemoteDataSource(memberRemoteDataSourceImpl: MemberRemoteDataSourceImpl): MemberRemoteDataSource
```

- [ ] **Step 10: 전체 검증**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS. `assembleDebug`가 Hilt 그래프를 만들므로 DI 2줄이 실제로 물리는지 여기서 확인된다.

- [ ] **Step 11: 커밋**

```bash
git add domain/src/main data/src/main data/src/test
git commit -m "feat(data): member API Service·RemoteDataSource 배선

GET /api/v1/users/me, PATCH /api/v1/users/me/nickname 두 엔드포인트를
Service·remote DataSource·domain VO 로 배선한다.

GlobalNickname 은 GroupNickname 과 서버 규칙이 같아도 타입을 합치지 않는다.
합치면 전역 닉네임을 그룹 API 에 넘기는 실수가 컴파일을 통과한다.
LoginProvider 는 서버가 주는 값이라 UNKNOWN 폴백을 둔다."
```

---

## Task 3: parfait-image 배치 확정(POST)

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/id/ParfaitId.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/id/ParfaitImageId.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/id/GroupMemberId.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingTransform.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingBorder.kt`
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/PlacedToppingVO.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/PlaceParfaitImageRequest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/PlaceParfaitImageResponse.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImpl.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/ServiceModule.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/di/RemoteDataSourceModule.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt`

**Interfaces:**
- Consumes: `ApiCaller#safeApiCall(block, transform)` · `GroupId(value: Long)`·`ImageId(value: Long)`(`domain/model/id`, `ImageId`는 PR #229 산출물) · `GroupNickname(value: String)`(`domain/model/group`)
- Produces:
  - `ParfaitId(val value: Long)` · `ParfaitImageId(val value: Long)` · `GroupMemberId(val value: Long)`
  - `ToppingTransform(positionX: Double, positionY: Double, positionZ: Int, scale: Double, rotation: Double)`
  - `ToppingBorder` sealed — `ToppingBorder.None`(data object) · `ToppingBorder.Solid(color: String, width: Double)`
  - `PlacedToppingVO(parfaitImageId, imageId, imageUrl, transform, placedBy)` · `ToppingPlacerVO(groupMemberId, nickname)`
  - `ParfaitImageRemoteDataSource#placeTopping(groupId, parfaitId, imageId, transform, border): Result<PlacedToppingVO>`
  - mapper 내부 함수 `ToppingTransform#toPlaceRequest(imageId, border)` — Task 4가 같은 파일에 함수를 더한다.

- [ ] **Step 1: id value class 3개를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/id/ParfaitId.kt`:
```kotlin
package com.teamyg.parfait.domain.model.id

@JvmInline
value class ParfaitId(val value: Long)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/id/ParfaitImageId.kt`:
```kotlin
package com.teamyg.parfait.domain.model.id

/**
 * 캔버스 위 배치 행의 식별자. 이미지 자체를 가리키는 ImageId 와 다른 키다 —
 * 서버 경로에도 imageId 와 parfaitImageId 가 따로 있다.
 */
@JvmInline
value class ParfaitImageId(val value: Long)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/id/GroupMemberId.kt`:
```kotlin
package com.teamyg.parfait.domain.model.id

/**
 * 그룹 멤버십 행의 식별자. 계정을 가리키는 MemberId 와 다른 키다.
 */
@JvmInline
value class GroupMemberId(val value: Long)
```

- [ ] **Step 2: topping domain 타입 3개를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingTransform.kt`:
```kotlin
package com.teamyg.parfait.domain.model.topping

/**
 * 캔버스 위 토핑의 위치·크기·각도.
 *
 * Double 이 넷 연속이라 평면 파라미터로 두면 호출부가 순서를 뒤바꿔도 컴파일이 통과한다.
 * 이 타입을 만드는 이유가 그것이므로 생성은 항상 named argument 로 한다.
 *
 * 서버에 범위 검증이 없다 — 음수 scale, 캔버스 밖 좌표가 그대로 저장된다.
 * 보정은 화면 계층 책임이다(`api/parfait-image.md`).
 */
data class ToppingTransform(
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
)
```

`domain/src/main/java/com/teamyg/parfait/domain/model/topping/ToppingBorder.kt`:
```kotlin
package com.teamyg.parfait.domain.model.topping

/**
 * 토핑 테두리.
 *
 * 서버는 borderType=SOLID 인데 색이나 두께가 없으면 400 INVALID_BORDER 를 던진다.
 * sealed 로 묶어 그 실패를 표현 불가능한 상태로 만든다 — Solid 를 만들려면 둘 다 있어야 한다.
 *
 * color 는 raw String 이고 앱이 형식을 규정하지 않는다. 서버 계약이 타입만 정하고
 * 형식을 말하지 않아 지금 좁힐 근거가 없다. 색을 실제로 만드는 화면 라운드가 정한다.
 */
sealed interface ToppingBorder {
    data object None : ToppingBorder

    data class Solid(val color: String, val width: Double) : ToppingBorder
}
```

`domain/src/main/java/com/teamyg/parfait/domain/model/topping/PlacedToppingVO.kt`:
```kotlin
package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 배치 확정 결과.
 *
 * 테두리 필드가 없다. 서버가 저장은 하는데 응답에 돌려주지 않기 때문이다 — 없는 것을
 * 지어내지 않는다. 앱이 테두리 상태를 알려면 자기가 보낸 값을 기억해야 한다.
 */
data class PlacedToppingVO(
    val parfaitImageId: ParfaitImageId,
    val imageId: ImageId,
    val imageUrl: String,
    val transform: ToppingTransform,
    val placedBy: ToppingPlacerVO,
)

/**
 * @param nickname 전역 닉네임이 아니라 그룹 안에서 쓰는 이름이다.
 */
data class ToppingPlacerVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
)
```

- [ ] **Step 3: wire DTO 2개를 만든다**

`data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/PlaceParfaitImageRequest.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 서버 계약을 그대로 미러링한 평면 DTO. sealed 는 domain 쪽에만 산다.
 *
 * @param imageId COMPLETED 상태여야 한다. PENDING 이면 409 IMAGE_NOT_CONFIRMED.
 * @param borderType NONE 또는 SOLID. enum 밖 값은 Jackson 역직렬화가 먼저 깨져
 *   400 INVALID_REQUEST 다(도메인 코드가 아니라 공통 코드).
 * @param borderColor borderType=SOLID 면 필수. NONE 이면 서버가 무시한다.
 * @param borderWidth borderType=SOLID 면 필수. NONE 이면 서버가 무시한다.
 */
@Serializable
data class PlaceParfaitImageRequest(
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("positionX")
    val positionX: Double,
    @SerialName("positionY")
    val positionY: Double,
    @SerialName("positionZ")
    val positionZ: Int,
    @SerialName("scale")
    val scale: Double,
    @SerialName("rotation")
    val rotation: Double,
    @SerialName("borderType")
    val borderType: String,
    @SerialName("borderColor")
    val borderColor: String? = null,
    @SerialName("borderWidth")
    val borderWidth: Double? = null,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/PlaceParfaitImageResponse.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 요청에 보낸 borderType·borderColor·borderWidth 가 응답에 없다. 서버가 저장만 하고
 * 돌려주지 않는다(`api/parfait-image.md`).
 *
 * @param imageId 요청에 넣은 image_meta id 그대로.
 * @param parfaitImageId 배치 행의 id. 이후 PATCH 가 쓰는 키다.
 */
@Serializable
data class PlaceParfaitImageResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("imageId")
    val imageId: Long,
    @SerialName("imageUrl")
    val imageUrl: String,
    @SerialName("positionX")
    val positionX: Double,
    @SerialName("positionY")
    val positionY: Double,
    @SerialName("positionZ")
    val positionZ: Int,
    @SerialName("scale")
    val scale: Double,
    @SerialName("rotation")
    val rotation: Double,
    @SerialName("placedBy")
    val placedBy: PlacedByResponse,
)

@Serializable
data class PlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
)
```

- [ ] **Step 4: mapper를 만든다**

`data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt`:
```kotlin
package com.teamyg.parfait.data.source.parfaitimage.mapper

import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlacedByResponse
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingPlacerVO
import com.teamyg.parfait.domain.model.topping.ToppingTransform

private const val BORDER_TYPE_NONE = "NONE"
private const val BORDER_TYPE_SOLID = "SOLID"

/**
 * sealed 테두리를 서버가 받는 평면 3필드로 편다. None 이면 색·두께를 보내지 않는다.
 */
internal fun ToppingTransform.toPlaceRequest(
    imageId: ImageId,
    border: ToppingBorder,
): PlaceParfaitImageRequest {
    val solid = border as? ToppingBorder.Solid
    return PlaceParfaitImageRequest(
        imageId = imageId.value,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
        borderType = when (border) {
            ToppingBorder.None -> BORDER_TYPE_NONE
            is ToppingBorder.Solid -> BORDER_TYPE_SOLID
        },
        borderColor = solid?.color,
        borderWidth = solid?.width,
    )
}

internal fun PlaceParfaitImageResponse.toPlacedToppingVO(): PlacedToppingVO = PlacedToppingVO(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    imageId = ImageId(imageId),
    imageUrl = imageUrl,
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
    placedBy = placedBy.toToppingPlacerVO(),
)

private fun PlacedByResponse.toToppingPlacerVO(): ToppingPlacerVO = ToppingPlacerVO(
    groupMemberId = GroupMemberId(groupMemberId),
    nickname = GroupNickname(nickname),
)
```

- [ ] **Step 5: Service와 DataSource 인터페이스를 만든다**

`data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt`:
```kotlin
package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * 서버 화이트리스트 밖이라 access token 이 필요하다. @NoAuth 를 붙이지 않는다.
 *
 * 경로의 images 세그먼트는 최상위 /api/v1/images(업로드)와 다른 도메인이다 —
 * 이쪽은 캔버스 배치다.
 */
interface ParfaitImageService {
    @POST("api/v1/groups/{groupId}/parfaits/{parfaitId}/images")
    suspend fun postGroupsByGroupIdParfaitsByParfaitIdImages(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Body request: PlaceParfaitImageRequest,
    ): ApiResponse<PlaceParfaitImageResponse>
}
```

`data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt`:
```kotlin
package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform

interface ParfaitImageRemoteDataSource {
    /**
     * 업로드가 확인된(COMPLETED) 이미지를 파르페 위 좌표에 배치한다.
     *
     * 같은 (parfaitId, imageId) 로 다시 부르면 새 배치가 생기지 않고 기존 배치가
     * 이동하며 소유자가 호출자로 바뀐다 — 서버가 upsert 로 구현돼 있고 배치자를
     * 대조하지 않는다(`api/parfait-image.md`). 같은 이미지를 두 번 배치할 수 없다.
     */
    suspend fun placeTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO>
}
```

- [ ] **Step 6: 실패하는 테스트를 쓴다**

`data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt`:
```kotlin
package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitImageService
import com.teamyg.parfait.data.service.model.request.parfaitimage.PlaceParfaitImageRequest
import com.teamyg.parfait.data.service.model.response.ApiResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlaceParfaitImageResponse
import com.teamyg.parfait.data.service.model.response.parfaitimage.PlacedByResponse
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.GroupMemberId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParfaitImageRemoteDataSourceImplTest {
    private val parfaitImageService: ParfaitImageService = mockk()
    private val apiCaller = ApiCaller(json = Json { ignoreUnknownKeys = true })
    private val dataSource = ParfaitImageRemoteDataSourceImpl(
        parfaitImageService = parfaitImageService,
        apiCaller = apiCaller,
    )

    private val transform = ToppingTransform(
        positionX = 120.5,
        positionY = 340.2,
        positionZ = 1,
        scale = 1.0,
        rotation = 0.0,
    )

    private fun placeSuccess() = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = PlaceParfaitImageResponse(
            parfaitImageId = 201L,
            imageId = 77L,
            imageUrl = "https://example.com/image",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = 1,
            scale = 1.0,
            rotation = 0.0,
            placedBy = PlacedByResponse(groupMemberId = 10L, nickname = "연경이"),
        ),
    )

    private suspend fun place(border: ToppingBorder = ToppingBorder.None) = dataSource.placeTopping(
        groupId = GroupId(1L),
        parfaitId = ParfaitId(5L),
        imageId = ImageId(77L),
        transform = transform,
        border = border,
    )

    @Test
    fun placeTopping_serviceReturnsSuccess_returnsMappedVo() = runTest {
        // Given 서비스가 배치 성공 응답을 준다
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns placeSuccess()

        // When 토핑 배치
        val vo = place().getOrThrow()

        // Then 식별자·URL·transform 이 제자리에 들어간다
        assertEquals(ParfaitImageId(201L), vo.parfaitImageId)
        assertEquals(ImageId(77L), vo.imageId)
        assertEquals("https://example.com/image", vo.imageUrl)
        assertEquals(transform, vo.transform)
    }

    @Test
    fun placeTopping_mapsNestedPlacedBy() = runTest {
        // Given 배치자 정보가 담긴 성공 응답
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns placeSuccess()

        // When 토핑 배치
        val vo = place().getOrThrow()

        // Then 중첩 객체가 VO 로 풀리고 닉네임은 그룹 닉네임 타입이다
        assertEquals(GroupMemberId(10L), vo.placedBy.groupMemberId)
        assertEquals(GroupNickname("연경이"), vo.placedBy.nickname)
    }

    @Test
    fun placeTopping_noneBorder_sendsNoColorAndWidth() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitImageRequest>()
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns placeSuccess()

        // When 테두리 없이 배치
        place(border = ToppingBorder.None)

        // Then borderType 만 NONE 이고 색·두께는 보내지 않는다
        assertEquals("NONE", request.captured.borderType)
        assertNull(request.captured.borderColor)
        assertNull(request.captured.borderWidth)
    }

    @Test
    fun placeTopping_solidBorder_sendsColorAndWidth() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitImageRequest>()
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns placeSuccess()

        // When SOLID 테두리로 배치
        place(border = ToppingBorder.Solid(color = "#FFFFFF", width = 4.0))

        // Then 색·두께가 함께 실린다 (서버가 SOLID 인데 둘 중 하나가 없으면 400 INVALID_BORDER)
        assertEquals("SOLID", request.captured.borderType)
        assertEquals("#FFFFFF", request.captured.borderColor)
        assertEquals(4.0, request.captured.borderWidth)
    }

    @Test
    fun placeTopping_buildsRequestBodyFromTransform() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<PlaceParfaitImageRequest>()
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), capture(request))
        } returns placeSuccess()

        // When 배치
        place()

        // Then transform 5필드와 imageId 가 그대로 실린다 (Double 이 넷이라 뒤바뀌어도 컴파일된다)
        assertEquals(77L, request.captured.imageId)
        assertEquals(120.5, request.captured.positionX)
        assertEquals(340.2, request.captured.positionY)
        assertEquals(1, request.captured.positionZ)
        assertEquals(1.0, request.captured.scale)
        assertEquals(0.0, request.captured.rotation)
    }

    @Test
    fun placeTopping_unwrapsIdsForPathVariables() = runTest {
        // Given 성공 응답
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns placeSuccess()

        // When value class 로 감싼 id 로 배치
        place()

        // Then 경로 변수에는 raw Long 이 들어간다 (Retrofit 경계에서 벗긴다)
        coVerify(exactly = 1) {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(1L, 5L, any())
        }
    }

    @Test
    fun placeTopping_groupNotJoined_returnsBusinessException() = runTest {
        // Given envelope 의 success=false 응답
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "GROUP_NOT_JOINED",
            message = "참여하지 않은 그룹입니다",
            data = null,
        )

        // When 토핑 배치
        val result = place()

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        val error = assertIs<ApiException.Business>(result.exceptionOrNull())
        assertEquals("GROUP_NOT_JOINED", error.code)
    }

    @Test
    fun placeTopping_imageNotConfirmed_returnsBusinessException() = runTest {
        // Given 업로드가 확인되지 않은 이미지
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = false,
            code = "IMAGE_NOT_CONFIRMED",
            message = "업로드가 확인되지 않은 이미지입니다",
            data = null,
        )

        // When 토핑 배치
        val result = place()

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        assertEquals("IMAGE_NOT_CONFIRMED", assertIs<ApiException.Business>(result.exceptionOrNull()).code)
    }

    @Test
    fun placeTopping_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } throws IOException("connection reset")

        // When 토핑 배치
        val result = place()

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }

    @Test
    fun placeTopping_successButNullData_returnsEmptyBodyException() = runTest {
        // Given success=true 인데 data 가 비었다
        coEvery {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(any(), any(), any())
        } returns ApiResponse(
            success = true,
            code = "SUCCESS",
            message = "성공",
            data = null,
        )

        // When 토핑 배치
        val result = place()

        // Then EmptyBody 예외
        assertTrue(result.isFailure)
        assertEquals("SUCCESS", assertIs<ApiException.EmptyBody>(result.exceptionOrNull()).code)
    }
}
```

- [ ] **Step 7: 테스트가 컴파일에 실패하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitImageRemoteDataSourceImplTest*"`
Expected: FAIL — `Unresolved reference: ParfaitImageRemoteDataSourceImpl`.

- [ ] **Step 8: 구현을 쓴다**

`data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImpl.kt`:
```kotlin
package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitImageService
import com.teamyg.parfait.data.source.parfaitimage.mapper.toPlaceRequest
import com.teamyg.parfait.data.source.parfaitimage.mapper.toPlacedToppingVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import javax.inject.Inject

class ParfaitImageRemoteDataSourceImpl @Inject constructor(
    private val parfaitImageService: ParfaitImageService,
    private val apiCaller: ApiCaller,
) : ParfaitImageRemoteDataSource {
    override suspend fun placeTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.postGroupsByGroupIdParfaitsByParfaitIdImages(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                request = transform.toPlaceRequest(imageId = imageId, border = border),
            )
        },
        transform = { it.toPlacedToppingVO() },
    )
}
```

- [ ] **Step 9: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitImageRemoteDataSourceImplTest*"`
Expected: PASS (10건).

- [ ] **Step 10: DI에 2줄을 추가한다**

`ServiceModule.kt` — import에 `com.teamyg.parfait.data.service.ParfaitImageService`를 더하고 object 안에 추가:
```kotlin
    @Provides
    @Singleton
    fun provideParfaitImageService(retrofit: Retrofit): ParfaitImageService =
        retrofit.create(ParfaitImageService::class.java)
```

`RemoteDataSourceModule.kt` — import에 `com.teamyg.parfait.data.source.parfaitimage.remote.ParfaitImageRemoteDataSource`와 `...ParfaitImageRemoteDataSourceImpl`을 더하고 interface 안에 추가:
```kotlin
    @Binds
    @Singleton
    fun bindParfaitImageRemoteDataSource(
        parfaitImageRemoteDataSourceImpl: ParfaitImageRemoteDataSourceImpl,
    ): ParfaitImageRemoteDataSource
```

- [ ] **Step 11: 전체 검증**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS.

- [ ] **Step 12: 커밋**

```bash
git add domain/src/main data/src/main data/src/test
git commit -m "feat(data): 토핑 배치 확정 API Service·RemoteDataSource 배선

POST /api/v1/groups/{groupId}/parfaits/{parfaitId}/images 를 배선한다.

서버는 borderType=SOLID 인데 색이나 두께가 없으면 400 INVALID_BORDER 를
던진다. domain 쪽 ToppingBorder 를 sealed 로 묶어 그 상태를 표현 불가능하게
만들고, mapper 가 평면 3필드로 편다. ToppingTransform 도 Double 넷이 연속인
자리를 이름 붙은 필드로 감싼다."
```

---

## Task 4: parfait-image 위치/크기/각도 수정(PATCH)

**Files:**
- Create: `domain/src/main/java/com/teamyg/parfait/domain/model/topping/UpdatedToppingVO.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageRequest.kt`
- Create: `data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImageResponse.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/service/ParfaitImageService.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSource.kt`
- Modify: `data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImpl.kt`
- Test: `data/src/test/java/com/teamyg/parfait/data/source/parfaitimage/remote/ParfaitImageRemoteDataSourceImplTest.kt` (추가)

**Interfaces:**
- Consumes: Task 3의 `ParfaitImageService` · `ParfaitImageRemoteDataSource(+Impl)` · `ToppingTransform` · `ParfaitImageId` · `ParfaitId` · mapper 파일
- Produces: `UpdatedToppingVO(parfaitImageId: ParfaitImageId, transform: ToppingTransform)` · `ParfaitImageRemoteDataSource#updateTopping(groupId, parfaitId, parfaitImageId, positionX, positionY, positionZ, scale, rotation): Result<UpdatedToppingVO>`

- [ ] **Step 1: domain VO와 wire DTO 2개를 만든다**

`domain/src/main/java/com/teamyg/parfait/domain/model/topping/UpdatedToppingVO.kt`:
```kotlin
package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.ParfaitImageId

/**
 * 수정 결과. 배치 응답(PlacedToppingVO)과 달리 imageId·imageUrl·placedBy 가 없다 —
 * 같은 리소스인데 서버의 두 응답 필드 집합이 다르다(`api/parfait-image.md`).
 */
data class UpdatedToppingVO(
    val parfaitImageId: ParfaitImageId,
    val transform: ToppingTransform,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/request/parfaitimage/UpdateParfaitImageRequest.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.request.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 부분 수정. null 인 필드는 서버가 기존 값을 유지한다(ParfaitImage.update 의 ?: 병합).
 *
 * @RemoteJson Json 이 explicitNulls 기본값을 쓰므로 안 바꾸는 필드도 "positionX": null 로
 * 실려 나간다. 서버에게 키 부재와 명시적 null 이 같은 뜻이라 동작은 정확하다.
 * 이 API 하나 때문에 전역 Json 설정을 바꾸지 않는다.
 *
 * 전 필드가 null 인 빈 패치도 서버가 받아들이며 updatedAt 만 올라간다(에러가 아니다).
 */
@Serializable
data class UpdateParfaitImageRequest(
    @SerialName("positionX")
    val positionX: Double? = null,
    @SerialName("positionY")
    val positionY: Double? = null,
    @SerialName("positionZ")
    val positionZ: Int? = null,
    @SerialName("scale")
    val scale: Double? = null,
    @SerialName("rotation")
    val rotation: Double? = null,
)
```

`data/src/main/java/com/teamyg/parfait/data/service/model/response/parfaitimage/UpdateParfaitImageResponse.kt`:
```kotlin
package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 병합 후 값이 전부 non-null 로 온다. 요청이 부분이어도 응답은 전체다.
 */
@Serializable
data class UpdateParfaitImageResponse(
    @SerialName("parfaitImageId")
    val parfaitImageId: Long,
    @SerialName("positionX")
    val positionX: Double,
    @SerialName("positionY")
    val positionY: Double,
    @SerialName("positionZ")
    val positionZ: Int,
    @SerialName("scale")
    val scale: Double,
    @SerialName("rotation")
    val rotation: Double,
)
```

- [ ] **Step 2: mapper에 응답 변환을 더한다**

`data/src/main/java/com/teamyg/parfait/data/source/parfaitimage/mapper/VOMapper.kt` 맨 아래(`toToppingPlacerVO` 뒤)에 추가하고, import에 `UpdateParfaitImageResponse`·`UpdatedToppingVO`를 더한다.

```kotlin
internal fun UpdateParfaitImageResponse.toUpdatedToppingVO(): UpdatedToppingVO = UpdatedToppingVO(
    parfaitImageId = ParfaitImageId(parfaitImageId),
    transform = ToppingTransform(
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    ),
)
```

- [ ] **Step 3: Service와 DataSource 인터페이스에 PATCH를 더한다**

`ParfaitImageService.kt` — import에 `retrofit2.http.PATCH`·`UpdateParfaitImageRequest`·`UpdateParfaitImageResponse`를 더하고 interface 안에 추가:
```kotlin
    @PATCH("api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}")
    suspend fun patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
        @Path("groupId") groupId: Long,
        @Path("parfaitId") parfaitId: Long,
        @Path("parfaitImageId") parfaitImageId: Long,
        @Body request: UpdateParfaitImageRequest,
    ): ApiResponse<UpdateParfaitImageResponse>
```

`ParfaitImageRemoteDataSource.kt` — import에 `ParfaitImageId`·`UpdatedToppingVO`를 더하고 interface 안에 추가:
```kotlin
    /**
     * 배치된 토핑의 위치·크기·각도를 부분 수정한다. 넘기지 않은 값은 서버가 유지한다.
     *
     * 테두리는 이 API 로 바꿀 수 없다 — 서버 요청에 필드가 없다. 바꾸려면 같은 imageId 로
     * placeTopping 을 다시 부르는 수밖에 없고, 그 경로는 소유자를 덮어쓴다.
     *
     * 그룹에 참여하지 않았을 때도 본인 배치가 아닐 때와 같은 코드(PARFAIT_IMAGE_NOT_OWNED,
     * 403)가 온다 — placeTopping 이 미참여를 GROUP_NOT_JOINED 로 구분하는 것과 다르다.
     */
    suspend fun updateTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        positionX: Double? = null,
        positionY: Double? = null,
        positionZ: Int? = null,
        scale: Double? = null,
        rotation: Double? = null,
    ): Result<UpdatedToppingVO>
```

- [ ] **Step 4: 실패하는 테스트를 쓴다**

`ParfaitImageRemoteDataSourceImplTest.kt`의 클래스 닫는 중괄호 앞에 추가한다. import에 `UpdateParfaitImageRequest`·`UpdateParfaitImageResponse`를 더한다.

```kotlin
    private fun updateSuccess() = ApiResponse(
        success = true,
        code = "SUCCESS",
        message = "성공",
        data = UpdateParfaitImageResponse(
            parfaitImageId = 201L,
            positionX = 200.0,
            positionY = 400.0,
            positionZ = 1,
            scale = 1.5,
            rotation = 45.0,
        ),
    )

    @Test
    fun updateTopping_serviceReturnsSuccess_returnsMergedTransform() = runTest {
        // Given 서비스가 병합된 값을 준다
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                any(), any(), any(), any(),
            )
        } returns updateSuccess()

        // When 위치와 크기만 수정
        val vo = dataSource.updateTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            parfaitImageId = ParfaitImageId(201L),
            positionX = 200.0,
            positionY = 400.0,
            scale = 1.5,
            rotation = 45.0,
        ).getOrThrow()

        // Then 응답은 부분이 아니라 전체 transform 이다
        assertEquals(ParfaitImageId(201L), vo.parfaitImageId)
        assertEquals(
            ToppingTransform(positionX = 200.0, positionY = 400.0, positionZ = 1, scale = 1.5, rotation = 45.0),
            vo.transform,
        )
    }

    @Test
    fun updateTopping_omittedFieldsAreSentAsNull() = runTest {
        // Given 요청 바디를 잡아둔다
        val request = slot<UpdateParfaitImageRequest>()
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                any(), any(), any(), capture(request),
            )
        } returns updateSuccess()

        // When z-order 만 바꾼다
        dataSource.updateTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            parfaitImageId = ParfaitImageId(201L),
            positionZ = 3,
        )

        // Then 지정한 필드만 값이 있고 나머지는 null 이다 (서버가 null 을 미변경으로 읽는다)
        assertEquals(3, request.captured.positionZ)
        assertNull(request.captured.positionX)
        assertNull(request.captured.positionY)
        assertNull(request.captured.scale)
        assertNull(request.captured.rotation)
    }

    @Test
    fun updateTopping_unwrapsIdsForPathVariables() = runTest {
        // Given 성공 응답
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                any(), any(), any(), any(),
            )
        } returns updateSuccess()

        // When value class 로 감싼 id 로 수정
        dataSource.updateTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            parfaitImageId = ParfaitImageId(201L),
            positionX = 200.0,
        )

        // Then 경로 변수 셋에 raw Long 이 들어간다
        coVerify(exactly = 1) {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                1L, 5L, 201L, any(),
            )
        }
    }

    @Test
    fun updateTopping_notOwned_returnsBusinessException() = runTest {
        // Given 본인이 배치한 토핑이 아니다 (그룹 미참여도 같은 코드로 온다)
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                any(), any(), any(), any(),
            )
        } returns ApiResponse(
            success = false,
            code = "PARFAIT_IMAGE_NOT_OWNED",
            message = "본인이 배치한 토핑이 아닙니다",
            data = null,
        )

        // When 수정
        val result = dataSource.updateTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            parfaitImageId = ParfaitImageId(201L),
            positionX = 200.0,
        )

        // Then Business 예외로 실패한다
        assertTrue(result.isFailure)
        assertEquals(
            "PARFAIT_IMAGE_NOT_OWNED",
            assertIs<ApiException.Business>(result.exceptionOrNull()).code,
        )
    }

    @Test
    fun updateTopping_ioException_returnsNetworkException() = runTest {
        // Given 네트워크 단절
        coEvery {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                any(), any(), any(), any(),
            )
        } throws IOException("connection reset")

        // When 수정
        val result = dataSource.updateTopping(
            groupId = GroupId(1L),
            parfaitId = ParfaitId(5L),
            parfaitImageId = ParfaitImageId(201L),
            positionX = 200.0,
        )

        // Then Network 예외로 감싸진다
        assertTrue(result.isFailure)
        assertIs<ApiException.Network>(result.exceptionOrNull())
    }
```

- [ ] **Step 5: 테스트가 컴파일에 실패하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitImageRemoteDataSourceImplTest*"`
Expected: FAIL — `Unresolved reference: updateTopping`.

- [ ] **Step 6: 구현을 쓴다**

`ParfaitImageRemoteDataSourceImpl.kt` — import에 `UpdateParfaitImageRequest`·`toUpdatedToppingVO`·`ParfaitImageId`·`UpdatedToppingVO`를 더하고 클래스 안에 추가:
```kotlin
    override suspend fun updateTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        positionX: Double?,
        positionY: Double?,
        positionZ: Int?,
        scale: Double?,
        rotation: Double?,
    ): Result<UpdatedToppingVO> = apiCaller.safeApiCall(
        block = {
            parfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                parfaitImageId = parfaitImageId.value,
                request = UpdateParfaitImageRequest(
                    positionX = positionX,
                    positionY = positionY,
                    positionZ = positionZ,
                    scale = scale,
                    rotation = rotation,
                ),
            )
        },
        transform = { it.toUpdatedToppingVO() },
    )
```

- [ ] **Step 7: 테스트가 통과하는지 확인한다**

Run: `./gradlew :data:testDebugUnitTest --tests "*ParfaitImageRemoteDataSourceImplTest*"`
Expected: PASS (15건 — Task 3의 10건 + 이번 5건).

- [ ] **Step 8: 전체 검증**

Run: `./gradlew test ktlintCheck :app:assembleDebug`
Expected: PASS.

- [ ] **Step 9: 커밋**

```bash
git add domain/src/main data/src/main data/src/test
git commit -m "feat(data): 토핑 위치/크기/각도 수정 API 배선

PATCH .../images/{parfaitImageId} 를 배선한다. 팀 명세가 partial update 로
명시했고 C-305 자동 보정·z-order 변경이 위치만 바꾸는 실사용례라 부분 수정을
그대로 노출한다 - nullable 5파라미터.

안 바꾸는 필드는 explicitNulls 기본값 때문에 null 로 실려 나가는데, 서버
ParfaitImage.update 가 ?: 로 병합하므로 키 부재와 같은 뜻이다."
```

---

## Task 5: `http/` 요청 모음

**Files:**
- Create: `http/users.http`
- Create: `http/parfait-image.http`
- Modify: `http/README.md`

**Interfaces:**
- Consumes: `http/http-client.env.json`의 `base_url`·`access_token`·`group_id`·`image_id` 변수(전부 기존)
- Produces: 없음(코드가 아니라 사람이 쏘는 확인 수단)

`http-client.env.json`에는 변수를 더하지 않는다 — `parfait_id`는 서버에 조회 API가 없어 값을 얻을 경로가 없으므로 요청 파일에 리터럴로 두고 그 사실을 주석에 적는다.

> 🔁 **2026-08-11 이 지시는 뒤집혔다(사용자 결정, 커밋 `70203174`).** 위 문장은 `parfait_id`(조회 경로 없음)에 대해서는 여전히 맞지만, **`parfait_image_id`까지 싸잡아 막은 것이 틀렸다.** 최종 브랜치 리뷰가 지적했듯 `terms_id_*`·`group_id`·`image_*`는 전부 스크립트가 `client.global.set`으로 채우는 값인데도 `http-client.env.json`에 선언돼 있고 `_reset.http`에 도메인별 비우기 항목이 있다 — 계획의 논거("런타임에 채우므로 선언 불필요")가 그 관행과 어긋났다. **다음 라운드는 새 전역 변수를 만들면 env 구조·리셋 목록에 함께 등재한다.**

- [ ] **Step 1: `http/users.http`를 만든다**

```
### 회원(내 계정 조회 / 전역 닉네임 변경) API
#
# 인증 필요. 먼저 auth.http 로 로그인해 access_token 을 채운다.
# 대상 회원은 요청이 아니라 토큰에서 정해진다 - 남의 계정을 지정할 경로가 없다.

### 내 계정 정보 조회
# 서버가 가입 시 자동 생성한 닉네임을 여기서 처음 볼 수 있다(가입 응답에는 없다).
#
# ⚠️ provider 에 서버 core enum 밖 값이 오면(영속 계층에는 GOOGLE 이 있다) 서버가
#    500 INTERNAL_SERVER_ERROR 를 낸다. 앱은 UNKNOWN 으로 떨어뜨리지만 그 전에 서버가 깨진다.
GET {{base_url}}/api/v1/users/me
Authorization: Bearer {{access_token}}

> {%
    client.test("200", function() {
        client.assert(response.status === 200, "status: " + response.status);
    });
    client.log("provider=" + response.body.data.provider + " nickname=" + response.body.data.nickname);
%}

### 전역 닉네임 변경
# 이미 참여한 그룹의 그룹 닉네임은 바뀌지 않는다 - 별도 컬럼이고 이 API 가 건드리지 않는다.
PATCH {{base_url}}/api/v1/users/me/nickname
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "nickname": "부지런한 수달"
}

> {%
    client.test("200", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.data.nickname === "부지런한 수달", "nickname: " + response.body.data.nickname);
    });
%}

### (대조용) 빈 닉네임 → 400 INVALID_REQUEST
# @NotBlank 에 걸린다. 아래 형식 위반과 코드가 다르다는 것이 요점이다.
PATCH {{base_url}}/api/v1/users/me/nickname
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "nickname": ""
}

> {%
    client.test("400 INVALID_REQUEST", function() {
        client.assert(response.status === 400, "status: " + response.status);
        client.assert(response.body.code === "INVALID_REQUEST", "code: " + response.body.code);
    });
%}

### (대조용) 연속 공백 → 400 INVALID_NICKNAME
# @NotBlank 는 통과하고 GlobalNickname.of 에서 걸린다. 위 요청과 코드가 갈린다.
PATCH {{base_url}}/api/v1/users/me/nickname
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "nickname": "연속  공백"
}

> {%
    client.test("400 INVALID_NICKNAME", function() {
        client.assert(response.status === 400, "status: " + response.status);
        client.assert(response.body.code === "INVALID_NICKNAME", "code: " + response.body.code);
    });
%}

### (대조용) 16자 → 400 INVALID_NICKNAME
# 상한은 15자다. 그룹 닉네임과 규칙이 문자 그대로 같다.
PATCH {{base_url}}/api/v1/users/me/nickname
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "nickname": "가나다라마바사아자차카타파하가나"
}

> {%
    client.test("400 INVALID_NICKNAME", function() {
        client.assert(response.status === 400, "status: " + response.status);
        client.assert(response.body.code === "INVALID_NICKNAME", "code: " + response.body.code);
    });
%}

### (대조용) 토큰 없이 호출 → 401 UNAUTHORIZED
GET {{base_url}}/api/v1/users/me

> {%
    client.test("401 UNAUTHORIZED", function() {
        client.assert(response.status === 401, "status: " + response.status);
        client.assert(response.body.code === "UNAUTHORIZED", "code: " + response.body.code);
    });
%}
```

- [ ] **Step 2: `http/parfait-image.http`를 만든다**

```
### 토핑 배치(배치 확정 / 위치·크기·각도 수정) API
#
# ⚠️ 선행 요청이 있다. 다른 파일과 다른 점이다.
#    1) auth.http 로 로그인해 access_token 을 채운다
#    2) parfait-group.http 로 group_id 를 채운다
#    3) images.http 로 발급 -> S3 PUT -> confirm 까지 돌려 image_id 를 COMPLETED 로 만든다
#       (PENDING 인 imageId 로 배치하면 409 IMAGE_NOT_CONFIRMED 다)
#
# ⚠️ parfaitId 를 얻을 조회 API 가 서버에 없다. 아래 요청은 리터럴 1 을 쓴다 -
#    실제 값은 DB 나 서버팀에서 받아 손으로 바꾼다.
#
# ⚠️ 경로의 images 세그먼트는 최상위 /api/v1/images(업로드)와 다른 도메인이다.
#    여기 imageId 와 parfaitImageId 는 서로 다른 키다.

### 1. 토핑 배치 확정 (테두리 없음)
# 생성 POST 라 201 CREATED 다. 같은 도메인의 이미지 발급이 200 인 것과 다르다.
POST {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "imageId": {{image_id}},
  "positionX": 120.5,
  "positionY": 340.2,
  "positionZ": 1,
  "scale": 1.0,
  "rotation": 0.0,
  "borderType": "NONE",
  "borderColor": null,
  "borderWidth": null
}

> {%
    client.test("201", function() {
        client.assert(response.status === 201, "status: " + response.status);
        client.assert(response.body.code === "CREATED", "code: " + response.body.code);
    });
    client.global.set("parfait_image_id", response.body.data.parfaitImageId);
    client.log("placedBy=" + JSON.stringify(response.body.data.placedBy));
%}

### 2. 같은 imageId 로 다시 배치 (upsert 확인)
# ⚠️ 새 배치가 생기지 않는다. 기존 배치가 이동하고 소유자가 호출자로 바뀐다.
#    1번 응답과 parfaitImageId 가 같으면 upsert 가 맞다.
POST {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "imageId": {{image_id}},
  "positionX": 999.0,
  "positionY": 999.0,
  "positionZ": 9,
  "scale": 2.0,
  "rotation": 90.0,
  "borderType": "NONE",
  "borderColor": null,
  "borderWidth": null
}

> {%
    client.test("201 이고 같은 parfaitImageId", function() {
        client.assert(response.status === 201, "status: " + response.status);
        client.assert(
            String(response.body.data.parfaitImageId) === String(client.global.get("parfait_image_id")),
            "새 행이 생겼다: " + response.body.data.parfaitImageId
        );
    });
%}

### 3. 위치·크기·각도 부분 수정 (z-order 만)
# 보내지 않은 필드는 서버가 유지한다. 응답은 부분이 아니라 전체 값이다.
PATCH {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images/{{parfait_image_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "positionZ": 3
}

> {%
    client.test("200 이고 positionZ 만 바뀐다", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.data.positionZ === 3, "positionZ: " + response.body.data.positionZ);
        client.assert(response.body.data.positionX === 999.0, "positionX 가 바뀌었다: " + response.body.data.positionX);
    });
%}

### 4. 빈 바디로 수정
# 전 필드 null 인 빈 패치도 에러가 아니다. updatedAt 만 올라간다.
PATCH {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images/{{parfait_image_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{}

> {%
    client.test("200", function() {
        client.assert(response.status === 200, "status: " + response.status);
    });
%}

### 5. (대조용) SOLID 인데 색·두께 없음 → 400 INVALID_BORDER
# 앱은 ToppingBorder sealed 로 보내므로 이 상태를 만들 수 없다. 서버 제약 확인용이다.
POST {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "imageId": {{image_id}},
  "positionX": 10.0,
  "positionY": 10.0,
  "positionZ": 1,
  "scale": 1.0,
  "rotation": 0.0,
  "borderType": "SOLID",
  "borderColor": null,
  "borderWidth": null
}

> {%
    client.test("400 INVALID_BORDER", function() {
        client.assert(response.status === 400, "status: " + response.status);
        client.assert(response.body.code === "INVALID_BORDER", "code: " + response.body.code);
    });
%}

### 6. (대조용) borderType 이 enum 밖 값 → 400 INVALID_REQUEST
# 도메인 코드가 아니라 공통 코드다. Jackson 역직렬화가 먼저 깨지기 때문이다.
POST {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "imageId": {{image_id}},
  "positionX": 10.0,
  "positionY": 10.0,
  "positionZ": 1,
  "scale": 1.0,
  "rotation": 0.0,
  "borderType": "DOTTED",
  "borderColor": null,
  "borderWidth": null
}

> {%
    client.test("400 INVALID_REQUEST", function() {
        client.assert(response.status === 400, "status: " + response.status);
        client.assert(response.body.code === "INVALID_REQUEST", "code: " + response.body.code);
    });
%}

### 7. (대조용) 서버에 좌표 범위 검증이 없다
# 음수 scale 과 캔버스 밖 좌표가 그대로 저장된다. 보정은 앱 책임이다(C-305).
# 이 요청이 200 이면 "서버가 막아주겠지"라는 가정이 틀렸다는 뜻이다.
PATCH {{base_url}}/api/v1/groups/{{group_id}}/parfaits/1/images/{{parfait_image_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "positionX": -99999.0,
  "scale": -1.0,
  "rotation": 720.0
}

> {%
    client.test("200 - 서버가 막지 않는다", function() {
        client.assert(response.status === 200, "status: " + response.status);
        client.assert(response.body.data.scale === -1.0, "scale: " + response.body.data.scale);
    });
%}

### 8. (대조용) 참여하지 않은 그룹으로 수정 → 403 PARFAIT_IMAGE_NOT_OWNED
# ⚠️ 배치(POST)는 미참여를 GROUP_NOT_JOINED 로 구분하는데 수정(PATCH)은 안 한다.
#    "그룹에서 나갔다"와 "남의 토핑이다"를 코드만으로 구분할 수 없다.
PATCH {{base_url}}/api/v1/groups/999999/parfaits/1/images/{{parfait_image_id}}
Authorization: Bearer {{access_token}}
Content-Type: application/json

{
  "positionX": 1.0
}

> {%
    client.test("403 PARFAIT_IMAGE_NOT_OWNED", function() {
        client.assert(response.status === 403, "status: " + response.status);
        client.assert(response.body.code === "PARFAIT_IMAGE_NOT_OWNED", "code: " + response.body.code);
    });
%}
```

- [ ] **Step 3: `http/README.md`를 갱신한다 — 파일 목록 2줄 + 사실 오류 2건**

**(a) 파일 목록 표**에 `images.http` 행 뒤로 두 줄을 더한다.

```markdown
| `users.http` | 내 계정 조회 · 전역 닉네임 변경(선행: `auth.http`만) |
| `parfait-image.http` | 토핑 배치 확정 · 위치/크기/각도 수정(**선행이 셋** — `auth.http` → `parfait-group.http` → `images.http`) |
```

**권장 순서** 문단 뒤에 한 줄을 더한다.

```markdown
`parfait-image.http`는 준비가 가장 길다 — `images.http`의 발급 → S3 PUT → confirm 까지 끝내 이미지를 `COMPLETED`로 만들어야 배치가 통과한다(`PENDING`이면 `409 IMAGE_NOT_CONFIRMED`). `parfaitId`는 조회 API가 서버에 없어 요청 파일의 리터럴을 손으로 바꿔야 한다.
```

**(b) 사실 오류 2건을 고친다.** 이 README는 어제 정정된 계약을 아직 반영하지 않았다.

`### \`isNewUser\`가 아니라 \`newUser\`다 ⚠️ 가장 중요` 절 — **제목과 본문이 통째로 틀렸다.** 서버가
`jackson-module-kotlin`을 쓰므로 `is` 접두사가 JSON 키에 남고, 실제 응답 키는 **`isNewUser`**다
(`api/auth.md` "판별자 키"). `newUser`는 springdoc이 자기 ObjectMapper로 유도한 **스키마 쪽 값**이라
런타임과 다르다. 절 제목을 아래로 바꾸고 본문을 이 사실로 다시 쓴다.

```markdown
### 판별자 키는 `isNewUser`다 — 스웨거의 `newUser`가 틀렸다 ⚠️ 가장 중요

서버 `KakaoLoginResponse`는 Kotlin `val isNewUser: Boolean`이고, 서버가 `jackson-module-kotlin`을 쓰므로 **JSON 키에 `is` 접두사가 그대로 남는다.** 컨트롤러 테스트가 실제 응답 본문에 `$.data.isNewUser`를 단언한다.

스웨거만 `newUser`로 적는데, springdoc이 Kotlin 모듈이 없는 자기 ObjectMapper로 모델을 유도하기 때문이다 — **런타임 직렬화 결과와 다르다.** 앱 `KakaoLoginResponse`에 붙은 `@SerialName("newUser")`는 **고쳐야 한다**(키를 못 찾아 `MissingFieldException`이 난다).
```

`### 스웨거에 없는 에러 코드가 많다` 절의 enum 열거를 갱신한다 — `AuthErrorCode`가 12종에서
**14종**(`APPLE_SERVER_ERROR`·`APPLE_SERVER_UNAVAILABLE` 신설)이 됐고 enum이 둘 늘었다.

```markdown
스웨거는 성공 응답만 열거한다. 실제 에러 코드는 `AuthErrorCode`(14종)·`ParfaitGroupApiErrorCode`(11종)·`ImageErrorCode`(4종)·`MemberErrorCode`(2종)·`ParfaitImageErrorCode`(5종)·`CommonErrorCode`(2종)에 있고, 각 `.http` 파일 주석에 엔드포인트별로 적어뒀다.
```

애플 로그인 요청 파일·항목은 넣지 않는다 — Android가 쓰지 않기로 한 엔드포인트다.

- [ ] **Step 4: 요청 파일이 문법적으로 열리는지 확인한다**

IntelliJ HTTP Client로 두 파일을 열어 요청 블록이 인식되는지 본다(빨간 밑줄·미인식 블록이 없어야 한다). **실제로 쏘지는 않는다** — 개발 서버 접근과 선행 요청 3단계가 필요하고, 이 라운드의 검증 범위 밖이다.

- [ ] **Step 5: 커밋**

```bash
git add http/
git commit -m "docs(http): member·parfait-image 요청 모음 추가, README 사실 오류 2건 정정

users.http 와 parfait-image.http 를 더해 이번 라운드 4 엔드포인트를 덮는다.

parfait-image.http 는 선행 요청이 셋이다(로그인 -> 그룹 -> 이미지 confirm).
parfaitId 는 조회 API 가 없어 리터럴을 손으로 바꿔야 하고, 그 사실을 주석에
적었다. upsert 동작·좌표 검증 부재·PATCH 의 403 비대칭처럼 코드로는 확인할
수 없는 계약을 대조용 요청으로 재현할 수 있게 했다.

README 정정 2건: (1) 판별자 JSON 키는 newUser 가 아니라 isNewUser 다.
서버가 jackson-module-kotlin 을 써서 is 접두사가 남고 컨트롤러 테스트가
실제 응답 본문을 단언한다 - newUser 는 springdoc 이 자기 ObjectMapper 로
유도한 스키마 값이라 런타임과 다르다. (2) AuthErrorCode 가 12종에서 14종이
됐고 MemberErrorCode·ParfaitImageErrorCode 가 늘었다.

애플 로그인은 Android 가 쓰지 않기로 해 넣지 않는다."
```

---

## 최종 검증

전체 라운드가 끝난 뒤 한 번 더 돌린다.

```bash
./gradlew test ktlintCheck :app:assembleDebug
find . -path ./build -prune -o -name "*MapperTest.kt" -print   # 출력 없어야 한다
```

기대: 유닛 테스트 전량 통과, ktlint 위반 0, debug 빌드 성공(Hilt 그래프 포함), 매퍼 단독 테스트 0건.

### 이 라운드가 검증하지 못하는 것

- **실서버 호출 0건.** 개발 서버가 평문 HTTP라 앱에서 차단되고(`api/conventions.md`), 소비처가 없어 요청을 만들 자리도 없다. `http/` 요청 모음이 계약 해석을 사람이 확인할 유일한 수단이다.
- **`parfait-image` 배치 결과를 다시 읽지 못한다.** 서버에 배치 목록 조회 API가 없어 `http/`로도 "배치가 실제로 남았는가"를 직접 조회할 수 없다 — 위 요청 2번의 `parfaitImageId` 비교가 간접 확인이다.
- **부분 수정의 `null` 전송이 실서버에서 의도대로 읽히는지.** 서버 코드(`ParfaitImage.update`의 `?:` 병합)로는 확정했지만 실제 요청으로 확인한 적이 없다. `parfait-image.http` 3번이 그 확인이다.
