package com.teamyg.parfait.data.model.error

import com.teamyg.parfait.data.model.exception.ApiException
import com.teamyg.parfait.domain.model.error.AppError
import kotlinx.coroutines.CancellationException

/**
 * 데이터 레이어 예외를 도메인 에러로 바꾼다. **Repository 경계에서만** 호출한다 —
 * 이 변환이 있어야 feature 모듈이 `:data` 를 보지 않는다.
 *
 * [CancellationException] 은 변환하지 않고 재던진다. 취소를 에러로 오분류하면
 * 화면을 벗어날 때마다 에러가 발행된다.
 */
internal fun Throwable.toAppError(): AppError = when (this) {
    is CancellationException -> throw this

    is ApiException.Business -> AppError.Server(
        code = code,
        statusCode = statusCode,
        serverMessage = serverMessage,
    )

    is ApiException.Network -> AppError.Network(cause)

    is ApiException.Http -> AppError.Unexpected(this)

    is ApiException.EmptyBody -> AppError.Unexpected(this)

    is ApiException.Unknown -> AppError.Unexpected(this)

    else -> AppError.Unexpected(this)
}

/** 실패 원인만 [AppError] 로 갈아끼운다. 성공 값은 그대로 통과한다 */
internal fun <T> Result<T>.mapErrorToAppError(): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(it.toAppError()) },
)
