package com.teamyg.parfait.core.ui.reveal

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * 그리기를 멈추지 않고 가린다 — 감춘 자리도 측정·배치가 살아 있어야 이미지 요청이 이어진다.
 *
 * 시맨틱을 비우는 쪽에 `hideFromAccessibility` 를 쓰면 이 노드만 감춰지고 자식이 붙인
 * 시맨틱은 트리에 남는다. 가려야 할 것은 대개 자식 쪽 문구다.
 */
fun Modifier.revealed(revealed: Boolean): Modifier = alpha(if (revealed) 1f else 0f)
    .then(if (revealed) Modifier else SemanticsCleared)

private val SemanticsCleared = Modifier.clearAndSetSemantics { }
