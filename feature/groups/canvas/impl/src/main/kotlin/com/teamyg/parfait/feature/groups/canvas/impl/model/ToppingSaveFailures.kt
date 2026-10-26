package com.teamyg.parfait.feature.groups.canvas.impl.model

internal data class ToppingSaveFailures(
    val toppingIds: Set<Long> = emptySet(),
    val textIds: Set<Long> = emptySet(),
) {
    fun isEmpty(): Boolean = toppingIds.isEmpty() && textIds.isEmpty()
}
