package com.teamyg.parfait.feature.intro.impl.termagree.model

data class TermContent(
    val isRequired: Boolean,
    val title: String,
    val landingUrl: String,
) {
    val visibleText: String = if (isRequired) "(필수) " else "" + title
}

internal val TERM_CONTENT_LIST = listOf(
    TermContent(
        isRequired = true,
        title = "서비스 이용약관",
        landingUrl = "",
    ),
    TermContent(
        isRequired = true,
        title = "개인정보 처리방침",
        landingUrl = "",
    ),
)
