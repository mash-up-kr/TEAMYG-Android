package com.teamyg.parfait.core.testing.android

import kotlin.test.Test
import kotlin.test.assertFailsWith

class PreviewParameterNamesTest {
    @Test
    fun `이름이 붙은 파라미터 프리뷰는 통과한다`() {
        checkPreviewParameterNames(
            listOf(
                param(name = "default"),
                param(name = "Medium Primary"),
            ),
        )
    }

    @Test
    fun `이름 없는 파라미터 프리뷰는 실패한다`() {
        assertFailsWith<IllegalStateException> {
            checkPreviewParameterNames(listOf(param(name = null)))
        }
    }

    @Test
    fun `파일명에 쓸 수 없는 문자가 든 이름은 실패한다`() {
        assertFailsWith<IllegalStateException> {
            checkPreviewParameterNames(listOf(param(name = "a/b")))
        }
    }

    @Test
    fun `한 프리뷰 안에서 공백과 대소문자만 다른 이름은 실패한다`() {
        assertFailsWith<IllegalStateException> {
            checkPreviewParameterNames(
                listOf(
                    param(name = "Medium Primary"),
                    param(name = "medium_primary"),
                ),
            )
        }
    }

    @Test
    fun `다른 프리뷰나 다른 변형의 같은 이름은 통과한다`() {
        checkPreviewParameterNames(
            listOf(
                param(method = "A", name = "default"),
                param(method = "B", name = "default"),
                param(method = "A", variant = "foldable", name = "default"),
            ),
        )
    }
}

private fun param(
    method: String = "Preview",
    variant: String = "phone",
    name: String?,
) = PreviewParameterName(previewId = "FakeKt.$method", variant = variant, name = name)
