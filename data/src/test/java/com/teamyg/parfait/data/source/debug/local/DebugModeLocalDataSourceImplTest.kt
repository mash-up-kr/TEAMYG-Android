package com.teamyg.parfait.data.source.debug.local

import app.cash.turbine.test
import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSourceImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugModeLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    private val dataSource = DebugModeLocalDataSourceImpl(dataStore = dataStore)

    @Test
    fun isEnabled_neverStored_isFalse() = runTest {
        // Given 한 번도 저장한 적이 없다

        // When 값을 읽는다
        // Then 켜지지 않은 것으로 본다 — 없는 값이 디버그 모드를 열면 안 된다
        assertFalse(dataSource.isEnabled.first())
    }

    @Test
    fun setEnabled_true_thenRead_isTrue() = runTest {
        // Given 디버그 모드를 켠다
        dataSource.setEnabled(true)

        // When 다시 읽는다
        // Then 켜진 채로 남는다
        assertTrue(dataSource.isEnabled.first())
    }

    @Test
    fun setEnabled_false_afterTrue_isFalse() = runTest {
        // Given 켜 둔 상태
        dataSource.setEnabled(true)

        // When 끈다
        dataSource.setEnabled(false)

        // Then 꺼진 채로 남는다 — 배지 탭이 유일한 회복 경로라 이 왕복이 끊기면 안 된다
        assertFalse(dataSource.isEnabled.first())
    }

    @Test
    fun isEnabled_otherKeyChanges_doesNotReemit() = runTest {
        // Given 디버그 플래그를 구독하고 있다
        dataSource.isEnabled.test {
            assertFalse(awaitItem())

            // When 같은 DataStore 파일의 다른 키가 바뀐다
            dataStore.putRaw(key = ToppingDraftLocalDataSourceImpl.TOPPING_DRAFT_KEY_NAME, value = "{}")
            // `expectNoEvents` 는 그 시점 채널만 본다 — 수집 코루틴을 먼저 재개시키지 않으면
            // `distinctUntilChanged` 가 없어도 통과해 이 테스트가 아무것도 지키지 못한다
            runCurrent()

            // Then 디버그 플래그 구독자는 흔들리지 않는다
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
