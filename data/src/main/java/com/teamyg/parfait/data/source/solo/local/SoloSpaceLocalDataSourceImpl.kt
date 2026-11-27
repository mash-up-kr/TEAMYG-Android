package com.teamyg.parfait.data.source.solo.local

import android.content.Context
import com.teamyg.parfait.data.model.entity.SoloSpaceRecordEntity
import com.teamyg.parfait.data.model.qualifier.LocalJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton

/** [root] 를 받는 생성자는 테스트가 임시 디렉터리를 넘기려고 둔다. 실제 루트는 `filesDir/solo_space` 다 */
@Singleton
class SoloSpaceLocalDataSourceImpl internal constructor(
    private val root: File,
    private val json: Json,
) : SoloSpaceLocalDataSource {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        @LocalJson json: Json,
    ) : this(File(context.filesDir, DIR_NAME), json)

    private val recordFile: File get() = File(root, RECORD_FILE_NAME)
    private val tempRecordFile: File get() = File(root, TEMP_RECORD_FILE_NAME)
    private val imagesDir: File get() = File(root, IMAGES_DIR_NAME)

    override fun readRecord(): SoloSpaceRecordEntity? {
        val file = recordFile
        if (!file.exists()) {
            return null
        }
        return json.decodeFromString<SoloSpaceRecordEntity>(file.readText())
    }

    override fun writeRecord(record: SoloSpaceRecordEntity) {
        root.ensureDirectory()
        val temp = tempRecordFile
        temp.writeSynced(json.encodeToString(record).toByteArray())
        // 같은 디렉터리 안 rename 이라 원자적으로 바뀐다
        Files.move(
            temp.toPath(),
            recordFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    override fun deleteRecord() {
        tempRecordFile.delete()
        val file = recordFile
        if (file.exists() && !file.delete()) {
            throw IOException("기록 파일을 지우지 못했다: $file")
        }
    }

    override fun copyImage(
        sourcePath: String,
        fileName: String,
    ) {
        imagesDir.ensureDirectory()
        val target = File(imagesDir, fileName)
        try {
            target.writeSynced(File(sourcePath).readBytes())
        } catch (e: IOException) {
            target.delete()
            throw e
        }
    }

    override fun deleteImage(fileName: String) {
        val file = File(imagesDir, fileName)
        if (file.exists() && !file.delete()) {
            throw IOException("이미지를 지우지 못했다: $file")
        }
    }

    override fun clearImages() {
        imagesDir.listFiles()?.forEach { file ->
            if (!file.deleteRecursively()) {
                throw IOException("이미지를 지우지 못했다: $file")
            }
        }
    }

    override fun imageUri(fileName: String): String = File(imagesDir, fileName).toPath().toUri().toString()

    private companion object {
        const val DIR_NAME = "solo_space"
        const val RECORD_FILE_NAME = "canvas.json"
        const val TEMP_RECORD_FILE_NAME = "canvas.json.tmp"
        const val IMAGES_DIR_NAME = "images"
    }
}

private fun File.ensureDirectory() {
    if (!isDirectory && !mkdirs()) {
        throw IOException("디렉터리를 만들지 못했다: $this")
    }
}

// rename 이나 기록 저장보다 내용이 늦게 디스크에 닿으면, 전원이 꺼졌을 때 기록은 있는데 내용이 빈 파일이 남는다
private fun File.writeSynced(bytes: ByteArray) {
    FileOutputStream(this).use { output ->
        output.write(bytes)
        output.fd.sync()
    }
}
