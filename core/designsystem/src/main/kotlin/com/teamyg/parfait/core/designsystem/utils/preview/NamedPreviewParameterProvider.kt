package com.teamyg.parfait.core.designsystem.utils.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * 값마다 이름을 붙여 스크린샷 파일명이 순서 번호 대신 이름으로 남게 한다.
 * 순서 번호로 남으면 값 하나를 빼거나 끼울 때 뒤 값들이 다른 값의 기준 이미지와 비교된다.
 * 규칙은 ADR-0037.
 */
abstract class NamedPreviewParameterProvider<T> : PreviewParameterProvider<T> {
    /** 이름은 파일명이 된다. 바꾸면 기준 이미지와 이어지지 않으므로 값이 보여 주는 상태로 짓는다. */
    protected abstract val entries: List<Pair<String, T>>

    override val values: Sequence<T>
        get() = entries.asSequence().map { it.second }

    override fun getDisplayName(index: Int): String = entries[index].first
}
