package com.teamyg.parfait.core.designsystem.utils.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

/**
 * 값마다 이름을 붙여 스크린샷 파일명이 순서 번호 대신 이름으로 남게 한다.
 * 순서 번호로 남으면 값 하나를 빼거나 끼울 때 뒤 값들이 다른 값의 기준 이미지와 비교된다.
 *
 * `core:testing:android` 의 `UiSnapshotPreviewTester` 가 이름 없는 파라미터 프리뷰를 실패시킨다.
 */
abstract class NamedPreviewParameterProvider<T> : PreviewParameterProvider<T> {
    /** 이름은 파일명에 들어가므로 provider 안에서 겹치지 않게 짓는다. */
    protected abstract val entries: List<Pair<String, T>>

    override val values: Sequence<T>
        get() = entries.asSequence().map { it.second }

    override fun getDisplayName(index: Int): String = entries[index].first
}
