package com.teamyg.parfait.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.teamyg.parfait.domain.model.id.MemberId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountKeyAliasTest {
    private val accountKeyAlias = AccountKeyAlias()

    @Test
    fun alias_sameInput_isStable() {
        val first = accountKeyAlias.alias("notice-read", MemberId(1))
        val second = accountKeyAlias.alias("notice-read", MemberId(1))

        assertEquals(first, second)
    }

    @Test
    fun alias_differentMemberId_differs() {
        assertNotEquals(
            accountKeyAlias.alias("notice-read", MemberId(1)),
            accountKeyAlias.alias("notice-read", MemberId(2)),
        )
    }

    @Test
    fun alias_differentScope_differs() {
        assertNotEquals(
            accountKeyAlias.alias("notice-read", MemberId(1)),
            accountKeyAlias.alias("other", MemberId(1)),
        )
    }

    @Test
    fun alias_hidesRawId_andHas16LowerHexChars() {
        val alias = accountKeyAlias.alias("notice-read", MemberId(123456))

        assertTrue(Regex("[0-9a-f]{16}").matches(alias))
        assertFalse(alias.contains("123456"))
    }
}
