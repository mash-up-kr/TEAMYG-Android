package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 이 Saver 가 어긋나면 구성 변경 뒤 목적지가 바뀌거나 사라진다. */
class GroupNickNameNavigateToNextSaverTest {
    private val saverScope = SaverScope { true }

    private fun save(value: GroupNickNameSideEffect.NavigateToNext?): Any? = with(NavigateToNextSaver) {
        with(saverScope) { save(value) }
    }

    @Test
    fun saveAndRestore_value_keepsEveryField() {
        val value = GroupNickNameSideEffect.NavigateToNext(groupId = 7L, groupName = "라마바")

        val restored = NavigateToNextSaver.restore(save(value)!!)

        // Then 필드 순서가 어긋나면 여기서 걸린다
        assertEquals(value, restored)
    }

    @Test
    fun save_null_savesNothing() {
        val saved = save(null)

        // Then listSaver 가 빈 리스트를 "저장 안 함"으로 접는다 — 복원은 아예 일어나지 않고
        // rememberSaveable 의 초기값 null 이 그대로 쓰인다
        assertNull(saved)
    }
}
