package com.teamyg.parfait.domain.model.topping

/** 갈래는 다음 시도가 무엇을 다시 보내야 하는가로 나눈다 */
sealed interface TextToppingRecreateResult {
    data class Recreated(
        val placed: PlacedTextToppingVO,
    ) : TextToppingRecreateResult

    /**
     * 새 텍스트는 쌓였고 옛 것만 남았다. 다음 시도는 옛 것을 지우기만 해야 한다 — 쌓기는 멱등이
     * 아니라 다시 부르면 같은 글이 두 번 놓인다.
     */
    data class OldNotDeleted(
        val placed: PlacedTextToppingVO,
        val cause: Throwable,
    ) : TextToppingRecreateResult

    /** 서버에 아무것도 바뀌지 않았다 — 통째로 다시 시도해도 된다 */
    data class NotPlaced(
        val cause: Throwable,
    ) : TextToppingRecreateResult
}
