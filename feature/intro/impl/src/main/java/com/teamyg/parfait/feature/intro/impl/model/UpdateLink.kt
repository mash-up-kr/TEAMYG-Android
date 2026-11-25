package com.teamyg.parfait.feature.intro.impl.model

/** @property opensInBrowser Play 가 받지 못했을 때 외부 브라우저로 다시 열어도 되는지 */
internal data class UpdateLink(
    val uri: String,
    val opensInBrowser: Boolean,
)
