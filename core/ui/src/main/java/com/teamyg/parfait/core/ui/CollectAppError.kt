package com.teamyg.parfait.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.teamyg.parfait.domain.model.error.AppError

/**
 * ViewModel 의 공통 에러 통로를 수집한다. Route 에서 한 줄로 붙인다.
 *
 * 기본 동작이 로그뿐인 것은 **의도된 공백**이다 — 에러 UX 디자인이 아직 없다.
 * 문구·토스트가 정해지면 [defaultAppErrorHandler] 한 곳만 고치면 전 화면에 적용된다.
 */
@Composable
fun CollectAppError(
    viewModel: BaseViewModel<*, *, *>,
    onError: (AppError) -> Unit = defaultAppErrorHandler,
) {
    val currentOnError by rememberUpdatedState(onError)
    LaunchedEffect(viewModel) {
        viewModel.error.collect { currentOnError(it) }
    }
}

private val defaultAppErrorHandler: (AppError) -> Unit = { error ->
    // TODO(에러 UX 미정): 디자인 확정 후 YGToast 노출로 교체한다
    screenLogger.e { "처리되지 않은 AppError: $error" }
}
