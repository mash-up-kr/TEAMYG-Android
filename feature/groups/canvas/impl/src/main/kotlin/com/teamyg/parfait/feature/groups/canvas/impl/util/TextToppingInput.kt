package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.teamyg.parfait.core.util.jvm.extension.lastGraphemeCluster
import com.teamyg.parfait.core.util.jvm.extension.truncateToGraphemeClusters
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_MAX_LENGTH

@Composable
internal fun rememberMaxLengthInputTransformation(): InputTransformation = remember {
    InputTransformation {
        val text = asCharSequence().toString()
        val truncated = text.truncateToGraphemeClusters(TEXT_TOPPING_MAX_LENGTH)
        if (truncated.length < text.length) {
            delete(truncated.length, text.length)
            placeCursorAtEnd()
        }
    }
}

/**
 * 한글 입력기가 줄바꿈 커밋 시 조합 중이던 글자를 중복으로 다시 보내는 경우를 보정한다. 줄바꿈
 * 하나당 딱 한 번만 지운다 — 그 뒤에 같은 글자가 또 오면 의도한 입력으로 보고 통과시킨다.
 */
@Composable
internal fun rememberDedupeHangulEnterTransformation(): InputTransformation = remember {
    var correctedThisNewline = false
    InputTransformation {
        val old = originalText.toString()
        val new = asCharSequence().toString()
        if (!new.startsWith(old)) return@InputTransformation
        val added = new.substring(old.length)
        if (added == "\n") {
            // 새 줄바꿈이 커밋됨 — 이 줄에 대한 보정 기회를 새로 연다
            correctedThisNewline = false
            return@InputTransformation
        }
        if (correctedThisNewline) return@InputTransformation
        if (old.isEmpty() || old.last() != '\n') return@InputTransformation
        val beforeNewline = old.dropLast(1).lastGraphemeCluster() ?: return@InputTransformation

        if (beforeNewline != "\n" && added == beforeNewline) {
            delete(old.length, new.length)
            placeCursorAtEnd()
            correctedThisNewline = true
        }
    }
}
