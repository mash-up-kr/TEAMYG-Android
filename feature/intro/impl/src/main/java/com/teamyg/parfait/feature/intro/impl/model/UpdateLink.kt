package com.teamyg.parfait.feature.intro.impl.model

/**
 * 업데이트 버튼이 열어 볼 링크 하나.
 *
 * @property uri 연 URI. 먼저 [PLAY_STORE_PACKAGE] 로만 열어 본다
 * @property opensInBrowser Play 가 받지 못했을 때 외부 브라우저로 다시 열어도 되는지. `https` 만 그렇다
 */
internal data class UpdateLink(
    val uri: String,
    val opensInBrowser: Boolean,
)
