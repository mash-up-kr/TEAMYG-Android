package com.teamyg.parfait.data.util.image

import kotlinx.coroutines.InternalForInheritanceCoroutinesApi
import kotlinx.coroutines.Job
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * `isActive` 조회 수를 세는 [Job]. 커널이 행 경계마다 취소를 확인하는지 검증하는 데 쓴다.
 *
 * ⚠️ `Job by delegate` 는 [CoroutineContext] 의 `get`·`fold`·`minusKey`·`plus` 까지 위임한다.
 * 넷을 다 오버라이드하지 않으면 컨텍스트 조회가 위임 대상을 돌려주어 [isActive] 가 영영 안 불리고,
 * 증상은 "테스트가 조용히 통과한다"다.
 *
 * ⚠️ 코루틴이 [Job] 상속에 opt-in 을 요구하고 장래 에러 승격을 예고했다. 버전을 올릴 때 이 위임이
 * 막힐 수 있다.
 */
@OptIn(InternalForInheritanceCoroutinesApi::class)
internal class CountingJob(
    private val delegate: Job = Job(),
) : Job by delegate {
    var calls: Int = 0
        private set

    /** 이 횟수를 넘긴 조회부터 취소된 것으로 답한다 */
    var cancelAfter: Int = Int.MAX_VALUE

    override val isActive: Boolean
        get() {
            calls++
            // 위임을 먼저 취소해야 한다 — getCancellationException() 은 아직 활성인 Job 에서
            // 부르면 CancellationException 이 아니라 IllegalStateException 을 던진다
            if (calls > cancelAfter && delegate.isActive) delegate.cancel()
            return delegate.isActive
        }

    @Suppress("UNCHECKED_CAST")
    override fun <E : CoroutineContext.Element> get(key: CoroutineContext.Key<E>): E? =
        if (key === Job) this as E else null

    override fun <R> fold(
        initial: R,
        operation: (R, CoroutineContext.Element) -> R,
    ): R = operation(initial, this)

    override fun minusKey(key: CoroutineContext.Key<*>): CoroutineContext =
        if (key === Job) EmptyCoroutineContext else this

    // 위임된 plus 는 왼쪽 피연산자를 위임 Job 으로 바꿔 더블을 지운다. 인터페이스 기본 구현을
    // 부르면 this 가 왼쪽에 남는다
    override fun plus(context: CoroutineContext): CoroutineContext = super.plus(context)
}

/**
 * [block] 을 [job] 만 담긴 컨텍스트에서 돌린다.
 *
 * ⚠️ `withContext(job)` 을 쓰면 안 된다. 새 `ScopeCoroutine` 이 만들어져 컨텍스트의 [Job] 자리를
 * 차지하므로 커널이 [job] 을 못 본다.
 *
 * ⚠️ 컨텍스트에 `ContinuationInterceptor`(디스패처)를 넣으면 안 된다. 본문이 비동기로 제출되고
 * 이 함수가 즉시 반환해, 확인 호출 수가 0 인 채로 단언이 통과한다. 아래 완료 단언이 그것을 잡는다.
 *
 * 이 하니스는 커널에 중단 지점이 없다는 것을 전제한다.
 */
internal fun <T> runKernelCounting(
    job: CountingJob,
    block: suspend () -> T,
): T {
    var outcome: Result<T>? = null
    block.startCoroutine(
        object : Continuation<T> {
            override val context: CoroutineContext = job

            override fun resumeWith(result: Result<T>) {
                outcome = result
            }
        },
    )

    return checkNotNull(outcome) {
        "커널이 중단했다 — 이 하니스는 중단 지점이 없는 커널만 검증한다"
    }.getOrThrow()
}
