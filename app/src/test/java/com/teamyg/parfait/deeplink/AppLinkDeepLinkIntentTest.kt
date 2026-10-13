package com.teamyg.parfait.deeplink

import android.content.Intent
import android.net.Uri
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinkDeepLinkIntentTest {
    @Test
    fun toAppLinkDeepLinkOrNull_viewActionWithValidInviteCode_returnsOpenGroupInvite() {
        // Given, When App Link 클릭으로 열린 VIEW 인텐트
        val result = appLinkIntent(inviteCode = "ABC123").toAppLinkDeepLinkOrNull()

        // Then
        assertEquals(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"), result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_missingInviteCode_returnsNull() {
        // Given, When inviteCode 쿼리 파라미터 자체가 없음
        val result = appLinkIntent(inviteCode = null).toAppLinkDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_malformedInviteCode_returnsNull() {
        // Given, When 초대코드 규칙(6자 영숫자)에 맞지 않는 inviteCode
        val result = appLinkIntent(inviteCode = "abc").toAppLinkDeepLinkOrNull()

        // Then 잘못된 코드로 초대코드 화면을 열지 않는다
        assertNull(result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_notViewAction_returnsNull() {
        // Given, When VIEW 가 아닌 액션(예: MAIN, 평범한 콜드 스타트)
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_MAIN
        }

        // Then
        assertNull(intent.toAppLinkDeepLinkOrNull())
    }

    @Test
    fun toAppLinkDeepLinkOrNull_noData_returnsNull() {
        // Given, When VIEW 이지만 data URI 가 없음
        val intent = mockk<Intent> {
            every { action } returns Intent.ACTION_VIEW
            every { data } returns null
        }

        // Then
        assertNull(intent.toAppLinkDeepLinkOrNull())
    }

    @Test
    fun toAppLinkDeepLinkOrNull_trailingSlashPath_returnsOpenGroupInvite() {
        // Given, When 끝 슬래시가 붙은 /link/ 에 inviteCode 가 붙어 열림
        val result = appLinkIntent(inviteCode = "ABC123", path = "/link/").toAppLinkDeepLinkOrNull()

        // Then
        assertEquals(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"), result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_subPath_returnsNull() {
        // Given, When 코드를 경로로 실은 /link/ABC123
        val result = appLinkIntent(inviteCode = "ABC123", path = "/link/ABC123").toAppLinkDeepLinkOrNull()

        // Then 링크 양식은 쿼리 파라미터뿐이다
        assertNull(result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_rootPath_returnsNull() {
        // Given, When 랜딩 페이지(루트)에 inviteCode 가 붙어 열림
        val result = appLinkIntent(inviteCode = "ABC123", path = "").toAppLinkDeepLinkOrNull()

        // Then 랜딩 페이지는 앱 링크가 아니다
        assertNull(result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_otherPath_returnsNull() {
        // Given, When /link 가 아닌 경로
        val result = appLinkIntent(inviteCode = "ABC123", path = "/other").toAppLinkDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun toAppLinkDeepLinkOrNull_similarPrefixPath_returnsNull() {
        // Given, When /link 로 시작하지만 다른 경로(/linkage)
        val result = appLinkIntent(inviteCode = "ABC123", path = "/linkage").toAppLinkDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    private fun appLinkIntent(
        inviteCode: String?,
        path: String = "/link",
    ): Intent {
        val uri = mockk<Uri> {
            every { this@mockk.path } returns path
            every { getQueryParameter("inviteCode") } returns inviteCode
        }
        return mockk {
            every { action } returns Intent.ACTION_VIEW
            every { data } returns uri
        }
    }
}
