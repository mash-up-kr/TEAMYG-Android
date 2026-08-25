package com.teamyg.parfait.data.source.image.remote

interface RemoteImageDownloadDataSource {
    /**
     * [url] 의 응답 바이트 전체를 그대로 받는다.
     *
     * **실패하면 던진다.** [PresignedUploadDataSource]와 달리 이 값을 곧장 비트맵으로 디코드하는
     * `ImageSegmentationRepository.decodeImage` 쪽 계약이 이미 "실패하면 던진다"라서, 여기서
     * `Result`로 한 번 더 감싸면 호출부가 두 겹을 벗겨야 한다.
     */
    suspend fun download(url: String): ByteArray
}
