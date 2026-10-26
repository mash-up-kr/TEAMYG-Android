package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.TextEditResult
import com.teamyg.parfait.feature.groups.canvas.impl.model.TextEditSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 토핑 정리 화면 ↔ 텍스트 입력 화면 사이의 텍스트 재편집 왕복을 잇는다. 편집 대상과 결과가
 * NavKey 로 오가기엔 크고, 결과는 되돌아간 화면이 받아야 해서 둘 다 여기에 둔다.
 *
 * ⚠️ 메모리에만 있다 — 프로세스가 죽었다 살아나면 비어 있다.
 */
@Singleton
class TextEditSessionHolder @Inject constructor() {
    @Volatile
    private var session: TextEditSession? = null

    /** [result] 를 남긴 왕복. [clear] 가 남의 결과를 지우지 않게 가른다 */
    @Volatile
    private var resultSession: TextEditSession? = null

    private val _result = MutableStateFlow<Pair<Long, TextEditResult>?>(null)

    /** 편집한 텍스트의 parfaitTextId 와 결과. 받은 쪽이 [consume] 할 때까지 남는다 */
    val result: StateFlow<Pair<Long, TextEditResult>?> = _result.asStateFlow()

    /** 지난 왕복에서 받지 않은 결과가 새 편집에 섞이지 않게 함께 비운다 */
    fun start(session: TextEditSession) {
        _result.value = null
        resultSession = null
        this.session = session
    }

    fun session(): TextEditSession? = session

    fun finish(result: TextEditResult) {
        val current = session ?: return
        session = null
        resultSession = current
        _result.value = current.original.parfaitTextId to result
    }

    fun consume() {
        _result.value = null
        resultSession = null
    }

    /** [session] 으로 시작한 왕복이 남긴 것만 비운다 — 그새 시작된 다른 편집은 건드리지 않는다 */
    fun clear(session: TextEditSession) {
        if (this.session === session) this.session = null
        if (resultSession === session) consume()
    }

    fun clear() {
        session = null
        consume()
    }
}
