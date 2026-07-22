package com.teamyg.parfait.feature.intro.impl.termagree.model

data class TermContent(
    val isRequired: Boolean,
    val title: String,
    val landingUrl: String,
)

internal val TERM_CONTENT_LIST = listOf(
    TermContent(
        isRequired = true,
        title = "서비스 이용약관",
        landingUrl = "", // Todo : 노션 생성 후 landingUrl 추가
    ),
    TermContent(
        isRequired = true,
        title = "개인정보 처리방침",
        landingUrl = "", // Todo : 노션 생성 후 landingUrl 추가
    ),
)
