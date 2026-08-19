package com.teamyg.parfait.data.repository.image

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SegmentationCacheDirTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun clearFiles_directoryHasFiles_removesThemAndKeepsTheDirectory() {
        // Given 파일 셋이 든 디렉토리
        val directory = temporaryFolder.newFolder("segmentation")
        repeat(3) { index -> File(directory, "parfait_$index.png").writeText("x") }

        // When 비운다
        directory.clearFiles()

        // Then 파일만 사라지고 디렉토리는 남는다 — 다음 저장이 mkdirs 없이도 쓸 수 있어야 한다
        assertTrue(directory.exists())
        assertEquals(0, directory.listFiles()?.size)
    }

    @Test
    fun clearFiles_directoryDoesNotExist_doesNothing() {
        // Given 아직 만들어진 적 없는 디렉토리
        val directory = File(temporaryFolder.root, "segmentation")

        // When 비운다 — 첫 진입에서 실제로 일어나는 상황이다
        directory.clearFiles()

        // Then 예외 없이 지나가고 디렉토리를 만들지도 않는다
        assertTrue(directory.exists().not())
    }
}
