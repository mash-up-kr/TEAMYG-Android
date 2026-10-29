package com.teamyg.parfait.feature.groups.canvas.impl.model

import java.io.File
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextToppingChineseCharacterCoverageTest {
    @Test
    fun tw_containsCommonTraditionalChineseCharacter() {
        assertTrue('們'.code in LINE_SEED_TW_CHINESE_CHARACTERS)
        assertTrue('這'.code in LINE_SEED_TW_CHINESE_CHARACTERS)
    }

    @Test
    fun jpOnlyShinjitai_absentFromTw() {
        listOf('学', '会').forEach {
            assertFalse(it.code in LINE_SEED_TW_CHINESE_CHARACTERS)
            assertTrue(it.code in LINE_SEED_JP_CHINESE_CHARACTERS)
        }
    }

    @Test
    fun simplifiedChineseCharacter_absentFromBoth() {
        listOf('们', '这').forEach {
            assertFalse(it.code in LINE_SEED_TW_CHINESE_CHARACTERS)
            assertFalse(it.code in LINE_SEED_JP_CHINESE_CHARACTERS)
        }
    }

    @Test
    fun supplementaryPlane_jpHasExtB() {
        assertTrue(0x21A1A in LINE_SEED_JP_CHINESE_CHARACTERS)
        assertFalse(0x21A1A in LINE_SEED_TW_CHINESE_CHARACTERS)
    }

    @Test
    fun nonChineseCharacter_absentFromBoth() {
        listOf('A', '가', 'ア').forEach {
            assertFalse(it.code in LINE_SEED_TW_CHINESE_CHARACTERS)
            assertFalse(it.code in LINE_SEED_JP_CHINESE_CHARACTERS)
        }
    }

    @Test
    fun syntheticTable_bmpEdges() {
        val table = TextToppingChineseCharacterCoverage("\u4E00\u4E05\u4E10\u4E10\u4E20\u4E22", intArrayOf())
        listOf(0x4E00, 0x4E05, 0x4E10, 0x4E20, 0x4E22).forEach { assertTrue(it in table, it.toString(16)) }
        listOf(0x4DFF, 0x4E06, 0x4E0F, 0x4E11, 0x4E1F, 0x4E23).forEach { assertFalse(it in table, it.toString(16)) }
    }

    @Test
    fun syntheticTable_supplementaryEdges() {
        val table = TextToppingChineseCharacterCoverage("", intArrayOf(0x20000, 0x20005, 0x30000, 0x30000))
        listOf(0x20000, 0x20005, 0x30000).forEach { assertTrue(it in table, it.toString(16)) }
        listOf(0x1FFFF, 0x20006, 0x2FFFF, 0x30001).forEach { assertFalse(it in table, it.toString(16)) }
    }

    @Test
    fun syntheticTable_empty() {
        val table = TextToppingChineseCharacterCoverage("", intArrayOf())
        assertFalse(0x4E00 in table)
        assertFalse(0x20000 in table)
    }

    @Test
    fun tw_containsIterationMarks() {
        listOf(0x3005, 0x3006, 0x3007).forEach {
            assertTrue(it in LINE_SEED_TW_CHINESE_CHARACTERS)
            assertTrue(it in LINE_SEED_JP_CHINESE_CHARACTERS)
        }
    }

    @Test
    fun coverageData_matchesBundledFontHashes() {
        // 단위 테스트 작업 디렉터리는 모듈 루트다
        assertEquals(LINE_SEED_TW_SHA256, sha256("src/main/res/font/line_seed_tw_regular.ttf"), REGENERATE_MESSAGE)
        assertEquals(LINE_SEED_JP_SHA256, sha256("src/main/res/font/line_seed_jp_regular.ttf"), REGENERATE_MESSAGE)
    }

    private fun sha256(path: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(File(path).readBytes())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val REGENERATE_MESSAGE =
            "폰트가 바뀌었다 — python3 tools/text-topping-chinese-character-coverage/generate.py 로 테이블을 다시 만든다"
    }
}
