package com.teamyg.parfait.domain.model.solo

sealed interface SoloBackgroundEdit {
    data class Color(val hex: String) : SoloBackgroundEdit

    /** [filePath]는 이미 앱 캐시로 떨군 사본의 절대경로다 */
    data class Image(val filePath: String) : SoloBackgroundEdit
}
