package com.teamyg.parfait.feature.groups.canvas.impl.model

/** [script] 하나로 그리는 연속 구간. 인덱스는 UTF-16 이고 [end] 는 포함하지 않는다. */
data class TextToppingScriptRun(
    val script: TextToppingScript,
    val start: Int,
    val end: Int,
)
