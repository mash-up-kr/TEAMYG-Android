package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.placeCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.teamyg.parfait.core.util.jvm.extension.graphemeClusterCount
import com.teamyg.parfait.core.util.jvm.extension.lastGraphemeCluster

/** 그래핌 클러스터 기준(`String#graphemeClusterCount`) 글자 수 상한 */
const val TEXT_TOPPING_MAX_LENGTH = 100

/** 자동 크기의 최댓값(상한). Text-Area 에 안 들어가면 이 값부터 연속 축소한다 */
internal const val TEXT_TOPPING_FONT_SIZE_SP = 24f

/** 배치 모드 핀치 축소 하한. 사진 토핑의 `TOPPING_MIN_SCALE`(0.05, 상한 없음)과 같은 값으로 맞춘다. */
const val TEXT_TOPPING_MIN_SCALE = 0.05f

/** 그래핌 수가 [TEXT_TOPPING_MAX_LENGTH] 를 넘는 변경을 되돌린다 */
@Composable
internal fun rememberMaxLengthInputTransformation(): InputTransformation = remember {
    InputTransformation {
        if (asCharSequence().toString().graphemeClusterCount() > TEXT_TOPPING_MAX_LENGTH) {
            revertAllChanges()
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
