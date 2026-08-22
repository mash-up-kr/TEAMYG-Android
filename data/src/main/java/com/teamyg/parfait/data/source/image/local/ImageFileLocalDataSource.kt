package com.teamyg.parfait.data.source.image.local

import java.io.File

interface ImageFileLocalDataSource {
    /**
     * 업로드가 파일 절대경로만 받는데 갤러리 `content://` 는 경로가 아니라, 한 번 떨궈야 한다.
     *
     * 확장자는 시스템 MIME 을 먼저 믿되 없거나 서버가 받지 않는 형식이면 바이트 앞머리로
     * 다시 본다 — 확장자와 실제 내용이 어긋난 파일이 드물지 않고, 업로드는 확장자로
     * contentType 을 정한다. 어느 쪽으로도 PNG·JPEG 이 아니면 여기서 던진다.
     */
    fun copyToCache(uri: String): File
}
