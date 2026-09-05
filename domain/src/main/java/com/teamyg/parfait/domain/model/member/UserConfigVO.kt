package com.teamyg.parfait.domain.model.member

/**
 * 서버가 아니라 기기에만 남는 사용자 설정.
 *
 * @param seenTutorials 끝까지 본 튜토리얼. 목록에 없으면 아직 보여줘야 한다 — 화면이 늘 때마다
 *   설정에 boolean 을 하나씩 붙이지 않으려고 목록 하나로 둔다
 */
data class UserConfigVO(
    val seenTutorials: Set<TutorialKind> = emptySet(),
) {
    fun isTutorialVisible(tutorial: TutorialKind): Boolean = tutorial !in seenTutorials
}
