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

enum class RecentImageKind {
    /** 사용자가 고른 원본 사진 */
    SOURCE,

    /** 배치까지 마친 테두리 없는 트리밍 알맹이 */
    CUTOUT,
}
