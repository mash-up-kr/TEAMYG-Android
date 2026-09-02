package com.teamyg.parfait.data.source.file.local

import java.io.File

/** 안드로이드 uri 를 밖으로 내보내지 않는다 — 위 계층이 JVM 테스트로 잡히게 하려는 경계다 */
interface FileRecentImageLocalDataSource {
    fun mkdirs(): Boolean

    fun readBytes(sourceUri: String): ByteArray

    fun readFileBytes(filePath: String): ByteArray

    fun getTargetFile(
        bytes: ByteArray,
        extension: String,
    ): File

    /** uri 가 가리키는 파일 이름을 못 읽으면 `null` */
    fun getTargetFileFromUri(uri: String): File?

    fun getUriStringForFile(target: File): String
}
