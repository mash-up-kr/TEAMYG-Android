package com.teamyg.parfait.data.source.image.mapper

import com.teamyg.parfait.data.service.model.response.image.ConfirmImageUploadResponse
import com.teamyg.parfait.data.service.model.response.image.IssueImageUploadUrlResponse
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.image.ImageStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * 매퍼 테스트는 결정이 있는 곳만 다룬다. 필드를 그대로 옮기기만 하는 매퍼는 컴파일러가
 * 막아주니 테스트하지 않는다.
 *
 * 이 매퍼가 내리는 결정은 둘이다. ① status 문자열을 ImageStatus 로 옮기는 규칙 —
 * 모르는 값이 오면 예외를 던지지 않고 UNKNOWN 으로 떨어뜨린다. 누가 enumValueOf 로 바꾸면
 * 서버가 상태 하나 추가하는 순간 크래시가 난다. ② expiresIn 이 초 단위 Long 인데 VO 는
 * Duration 이다. 단위를 잘못 읽으면 밀리초로 해석돼 만료 판정이 1000배 어긋난다.
 *
 * 필드 배선도 컴파일러에 다 맡길 수 없다. uploadUrl 과 imageUrl 이 둘 다 String 이라
 * 뒤바꿔도 통과하는데, 이 둘은 의미가 정반대다(1회용 서명 URL vs 장기 공개 주소).
 */
class ImageVOMapperTest {
    private fun issueResponse(
        imageId: Long = 1L,
        uploadUrl: String = "https://bucket.s3.region.amazonaws.com/nukki/user1/key.png?sig",
        imageUrl: String = "https://bucket.s3.region.amazonaws.com/nukki/user1/key.png",
        expiresIn: Long = 900L,
    ) = IssueImageUploadUrlResponse(
        imageId = imageId,
        uploadUrl = uploadUrl,
        imageUrl = imageUrl,
        expiresIn = expiresIn,
    )

    private fun confirmResponse(
        imageId: Long = 1L,
        imageUrl: String = "https://bucket.s3.region.amazonaws.com/nukki/user1/key.png",
        status: String = "COMPLETED",
    ) = ConfirmImageUploadResponse(
        imageId = imageId,
        imageUrl = imageUrl,
        status = status,
    )

    @Test
    fun toImageUploadUrlVO_mapsEveryField() {
        // Given 서명 URL 과 공개 URL 이 서로 다른 발급 응답
        val response = issueResponse(
            imageId = 7L,
            uploadUrl = "https://example.com/upload",
            imageUrl = "https://example.com/image",
        )

        // When VO 로 변환
        val vo = response.toImageUploadUrlVO()

        // Then 두 URL 이 뒤바뀌지 않고 제자리에 들어간다 (둘 다 String 이라 컴파일러가 못 막는다)
        assertEquals(ImageId(7L), vo.imageId)
        assertEquals("https://example.com/upload", vo.uploadUrl)
        assertEquals("https://example.com/image", vo.imageUrl)
    }

    @Test
    fun toImageUploadUrlVO_expiresInIsReadAsSeconds() {
        // Given 서버가 초 단위로 준 만료 시간
        val response = issueResponse(expiresIn = 900L)

        // When VO 로 변환
        val vo = response.toImageUploadUrlVO()

        // Then 초로 해석된다 (밀리초로 읽으면 900밀리초가 돼 1000배 어긋난다)
        assertEquals(900.seconds, vo.expiresIn)
    }

    @Test
    fun toConfirmedImageVO_mapsKnownStatus() {
        // Given 서버가 아는 상태 문자열을 준다
        val completed = confirmResponse(status = "COMPLETED")
        val pending = confirmResponse(status = "PENDING")

        // When VO 로 변환
        // Then 각각 대응 enum 으로 떨어진다
        assertEquals(ImageStatus.COMPLETED, completed.toConfirmedImageVO().status)
        assertEquals(ImageStatus.PENDING, pending.toConfirmedImageVO().status)
    }

    @Test
    fun toConfirmedImageVO_unknownStatus_fallsBackToUnknown() {
        // Given 클라이언트가 모르는 상태 문자열
        val response = confirmResponse(status = "FAILED")

        // When VO 로 변환
        val vo = response.toConfirmedImageVO()

        // Then 예외를 던지지 않고 UNKNOWN 으로 떨어진다
        assertEquals(ImageStatus.UNKNOWN, vo.status)
    }

    @Test
    fun toConfirmedImageVO_statusMatchIsCaseSensitive() {
        // Given 값은 맞지만 대소문자가 다른 상태
        val response = confirmResponse(status = "completed")

        // When VO 로 변환
        val vo = response.toConfirmedImageVO()

        // Then enum 이름과 정확히 같아야 매칭되므로 UNKNOWN 이다
        assertEquals(ImageStatus.UNKNOWN, vo.status)
    }

    @Test
    fun toConfirmedImageVO_mapsIdAndUrl() {
        // Given 확인 응답
        val response = confirmResponse(imageId = 42L, imageUrl = "https://example.com/image")

        // When VO 로 변환
        val vo = response.toConfirmedImageVO()

        // Then id 는 value class 로 감싸이고 URL 은 그대로다
        assertEquals(ImageId(42L), vo.imageId)
        assertEquals("https://example.com/image", vo.imageUrl)
    }
}
