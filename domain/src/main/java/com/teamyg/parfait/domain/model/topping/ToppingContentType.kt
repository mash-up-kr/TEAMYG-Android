package com.teamyg.parfait.domain.model.topping

/** [UNKNOWN] 은 앱이 모르는 종류라 이미지처럼 그린다. */
enum class ToppingContentType {
    IMAGE,
    TEXT,
    UNKNOWN,
    ;

    fun isText(): Boolean = this == TEXT
}
