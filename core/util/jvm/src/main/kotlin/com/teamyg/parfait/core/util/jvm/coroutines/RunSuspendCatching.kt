package com.teamyg.parfait.core.util.jvm.coroutines

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.cancellation.CancellationException

/**
 * suspend 호출을 감싸는 [runCatching].
 *
 * stdlib [runCatching] 은 [CancellationException] 까지 잡아 `Result.failure` 로 만든다.
 * 블록 안에 suspend 호출이 있으면 그 순간 **취소가 실패로 둔갑한다** — 부모는 자식이
 * 정상 종료했다고 믿고, 화면을 벗어났을 뿐인데 호출부는 "작업이 실패했다"로 분기한다.
 * 이 함수는 취소만 걸러 재던지고 나머지만 `Result.failure` 로 만든다.
 *
 * **블록에 suspend 호출이 없으면 stdlib [runCatching] 을 쓴다.** 이걸 쓰면 "여기 취소
 * 위험이 있다"는 신호를 거짓으로 남기게 된다.
 *
 * 원본 관용구와 달리 `runCatching(block)` 에 람다를 넘기지 않고 직접 `try` 로 부른다.
 * 다른 함수로 넘기면 컴파일러가 exactly-once 를 증명하지 못해 `callsInPlace` 계약이
 * 거짓이 되고 `LEAKED_IN_PLACE_LAMBDA`·`WRONG_INVOCATION_KIND` 억제가 필요해진다.
 */
@OptIn(ExperimentalContracts::class)
inline fun <T> runSuspendCatching(block: () -> T): Result<T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
