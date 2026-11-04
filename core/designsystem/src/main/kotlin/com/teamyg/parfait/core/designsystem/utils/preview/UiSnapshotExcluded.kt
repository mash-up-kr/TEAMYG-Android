package com.teamyg.parfait.core.designsystem.utils.preview

/**
 * 프리뷰 스크린샷 비교에서 뺀다. 두 번 렌더해 픽셀이 같지 않은 프리뷰(네트워크·WebView·시간 의존)에만 붙인다.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class UiSnapshotExcluded
