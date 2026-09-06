package com.teamyg.parfait.core.designsystem.component.ygtutorial

/**
 * 여러 장짜리 튜토리얼에서 지금 몇 번째 장인가. 한 장뿐인 튜토리얼은 이것을 넘기지 않는다.
 *
 * 진행 표시(`2/3`)와 버튼 라벨은 같은 사실 하나에서 나온다 — 마지막 장인가. 화면마다 그 판단을
 * 다시 쓰면 진행 표시는 `3/3` 인데 버튼은 "다음"인 조합이 언제든 만들어진다.
 *
 * @param step 1 부터 센다
 */
data class YGTutorialProgress(
    val step: Int,
    val total: Int,
) {
    val isLast: Boolean
        get() = step >= total
}
