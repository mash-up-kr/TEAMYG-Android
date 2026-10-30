package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 이미지와 텍스트는 다른 테이블이라 id 가 겹칠 수 있어, 종류를 함께 들고 다닌다 */
sealed interface CanvasToppingKey {
    data class Image(val id: Long) : CanvasToppingKey

    data class Text(val id: Long) : CanvasToppingKey
}
