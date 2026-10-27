package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.teamyg.parfait.feature.groups.canvas.impl.model.textToppingScriptRuns

/** 읽기 전용 렌더와 측정에 쓴다. 편집 필드는 [textToppingScriptOutputTransformation]이 같은 구간을 입힌다. */
internal fun textToppingAnnotatedString(text: String): AnnotatedString = buildAnnotatedString {
    append(text)
    textToppingScriptRuns(text).forEach { addStyle(it.script.spanStyle, it.start, it.end) }
}

/** 표시용 스타일만 입힌다 — 입력 텍스트와 조합 상태는 건드리지 않는다. */
internal val textToppingScriptOutputTransformation: OutputTransformation = OutputTransformation {
    textToppingScriptRuns(asCharSequence()).forEach { addStyle(it.script.spanStyle, it.start, it.end) }
}
