package com.teamyg.parfait.domain.repository.image

interface ImageFileRepository {
    /**
     * 기기의 이미지를 앱이 읽을 수 있는 파일로 떨구고 그 절대경로를 돌려준다.
     *
     * [ImageUploadRepository.upload] 가 파일 절대경로만 받는데 화면이 쥔 것은 `content://`
     * 처럼 경로가 아닌 uri 라, 그 사이를 메우는 자리다.
     *
     * @param uri `content://` · `file://` 무엇이든 시스템이 열 수 있어야 한다.
     */
    suspend fun copyToCache(uri: String): Result<String>
}
