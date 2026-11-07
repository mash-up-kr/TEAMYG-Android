package com.teamyg.parfait.core.testing.android

/**
 * 파라미터 프리뷰 값 하나.
 *
 * @param variant 같은 함수의 `@Preview` 변형을 가른다. 변형이 다르면 이름이 같아도 파일이 겹치지 않는다.
 */
internal data class PreviewParameterName(
    val previewId: String,
    val variant: Any?,
    val name: String?,
) {
    /** 스캐너가 만들 파일명을 대소문자 구분 없이 비교하는 키. */
    val fileNameKey: String
        get() = name.orEmpty().replace(' ', '_').lowercase()

    /** 이름이 파일명에 안전한 문자(영문·숫자·`_`·공백)로만 이뤄졌는지. */
    val hasWellFormedName: Boolean
        get() = name.orEmpty().matches(PARAMETER_NAME)

    private companion object {
        val PARAMETER_NAME = Regex("[A-Za-z0-9_ ]+")
    }
}

/**
 * 파라미터 프리뷰는 이름이 스냅샷 파일명이 된다. 이름이 없으면 순서 번호로 돌아가 값이 빠질 때 기준 이미지와
 * 어긋난 비교가 나오므로 막는다. 근거는 ADR-0037.
 *
 * 스캐너가 공백을 `_` 로 바꾸고, 대소문자를 가리지 않는 파일 시스템에서는 대소문자만 다른 이름이 한 파일이 되므로
 * 그렇게 바꾼 뒤의 이름으로 중복을 본다.
 */
internal fun checkPreviewParameterNames(parameters: List<PreviewParameterName>) {
    val unnamed = parameters.filter { it.name.isNullOrBlank() }
    check(unnamed.isEmpty()) {
        "Preview parameters need names (NamedPreviewParameterProvider): " + unnamed.map { it.previewId }.distinct()
    }

    val malformed = parameters.filterNot { it.hasWellFormedName }
    check(malformed.isEmpty()) {
        "Preview parameter names allow only letters, digits, '_' and ' ': " +
            malformed.map { "${it.previewId}[${it.name}]" }
    }

    val duplicated = parameters
        .groupBy { it.previewId to it.variant }
        .filterValues { group -> group.map { it.fileNameKey }.toSet().size != group.size }
        .keys
        .map { it.first }
    check(duplicated.isEmpty()) {
        "Preview parameter names must be unique per preview (case-insensitive): $duplicated"
    }
}
