package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals

/** 이 Saver 가 어긋나면 구성 변경 뒤 목적지가 바뀌거나 사라진다. */
class GroupNickNameNavigateToNextSaverTest {
    // listSaver 가 항목마다 canBeSaved 를 부른다. true 로 스텁하면 이 Saver 의 존재 이유인
    // "Bundle 에 담기는가" 검사가 통째로 무력해진다.
    private val saverScope = SaverScope { it is Long || it is String || it is Int || it is Boolean }

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
}
