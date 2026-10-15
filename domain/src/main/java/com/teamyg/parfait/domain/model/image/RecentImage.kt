package com.teamyg.parfait.domain.model.image

/**
 * 갤러리 "최근"에 남는 항목 한 개.
 *
 * @param uri 화면이 그릴 때 쓰는 FileProvider uri
 * @param filePath 같은 파일의 절대경로. 토핑 초안이 요구하는 형태다
 */
data class RecentImage(
    val uri: String,
    val filePath: String,
    val kind: RecentImageKind,
)
