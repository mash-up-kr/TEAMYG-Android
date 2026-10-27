package com.teamyg.parfait.feature.groups.canvas.impl.model

import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextToppingHanCoverageTest {

    @Test
    fun tw_containsCommonTraditionalHan() {
        assertTrue('們'.code in LINE_SEED_TW_HAN)
        assertTrue('這'.code in LINE_SEED_TW_HAN)
    }

    @Test
    fun jpOnlyShinjitai_absentFromTw() {
        listOf('学', '会').forEach {
            assertFalse(it.code in LINE_SEED_TW_HAN)
            assertTrue(it.code in LINE_SEED_JP_HAN)
        }
    }

    @Test
    fun simplifiedHan_absentFromBoth() {
        listOf('们', '这').forEach {
            assertFalse(it.code in LINE_SEED_TW_HAN)
            assertFalse(it.code in LINE_SEED_JP_HAN)
        }
    }

    @Test
    fun supplementaryPlane_jpHasExtB() {
        assertTrue(0x21A1A in LINE_SEED_JP_HAN)
        assertFalse(0x21A1A in LINE_SEED_TW_HAN)
    }

    @Test
    fun nonHan_absentFromBoth() {
        listOf('A', '가', 'ア').forEach {
            assertFalse(it.code in LINE_SEED_TW_HAN)
            assertFalse(it.code in LINE_SEED_JP_HAN)
        }
    }

    @Test
    fun coverageData_matchesBundledFontHashes() {
        // 단위 테스트 작업 디렉터리는 모듈 루트다
        assertEquals(LINE_SEED_TW_SHA256, sha256("src/main/res/font/line_seed_tw_regular.ttf"), REGENERATE_MESSAGE)
        assertEquals(LINE_SEED_JP_SHA256, sha256("src/main/res/font/line_seed_jp_regular.ttf"), REGENERATE_MESSAGE)
    }

    private fun sha256(path: String): String =
        MessageDigest.getInstance("SHA-256").digest(File(path).readBytes())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val REGENERATE_MESSAGE =
            "폰트가 바뀌었다 — python3 tools/text-topping-han-coverage/generate.py 로 테이블을 다시 만든다"
    }
}
