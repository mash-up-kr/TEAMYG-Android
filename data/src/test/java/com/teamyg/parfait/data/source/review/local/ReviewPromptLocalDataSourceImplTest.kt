package com.teamyg.parfait.data.source.review.local

import com.teamyg.parfait.data.datastore.FakePreferencesDataStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class ReviewPromptLocalDataSourceImplTest {
    private val dataStore = FakePreferencesDataStore()

    // 고정된 "지금"을 자유롭게 밀어 "며칠이 지났다"를 흉내 낸다
    private var now = Instant.parse("2026-01-01T00:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    private val dataSource = ReviewPromptLocalDataSourceImpl(dataStore, clock)

    @Test
    fun checkAndMarkTriggered_fewerThanThreeUploads_isFalse() = runTest {
        dataSource.markFirstSeenIfAbsent()
        repeat(2) { dataSource.incrementUploadCount() }
        now += 4.days

        assertFalse(dataSource.checkAndMarkTriggered())
    }

    @Test
    fun checkAndMarkTriggered_threeUploadsButRetentionNotYetThreeDays_isFalse() = runTest {
        dataSource.markFirstSeenIfAbsent()
        repeat(3) { dataSource.incrementUploadCount() }
        now += 2.days

        assertFalse(dataSource.checkAndMarkTriggered())
    }

    @Test
    fun checkAndMarkTriggered_threeUploadsAndThreeDaysRetention_isTrue() = runTest {
        dataSource.markFirstSeenIfAbsent()
        repeat(3) { dataSource.incrementUploadCount() }
        now += 3.days

        assertTrue(dataSource.checkAndMarkTriggered())
    }

    @Test
    fun checkAndMarkTriggered_neverMarkedFirstSeen_isFalse() = runTest {
        // 기준선이 없으니 "유지"를 판단할 수 없다
        repeat(3) { dataSource.incrementUploadCount() }

        assertFalse(dataSource.checkAndMarkTriggered())
    }

    @Test
    fun checkAndMarkTriggered_recentlyTriggered_isFalseUntilFourMonthsPass() = runTest {
        dataSource.markFirstSeenIfAbsent()
        repeat(3) { dataSource.incrementUploadCount() }
        now += 3.days

        // 첫 트리거 — 이 호출이 같은 자리에서 lastTriggered 도 함께 적는다
        assertTrue(dataSource.checkAndMarkTriggered())

        // 다음날 조건이 다시 충족돼도(추가 업로드 등) 4개월 전에는 재시도하지 않는다
        now += 1.days
        assertFalse(dataSource.checkAndMarkTriggered())

        // 호출로부터 119일째는 아직 막혀 있다
        now += 118.days
        assertFalse(dataSource.checkAndMarkTriggered())

        // 정확히 120일(4개월)이 지나면 다시 트리거된다
        now += 1.days
        assertTrue(dataSource.checkAndMarkTriggered())
    }

    @Test
    fun markFirstSeenIfAbsent_calledTwice_keepsTheOriginalTimestamp() = runTest {
        dataSource.markFirstSeenIfAbsent()
        repeat(3) { dataSource.incrementUploadCount() }
        now += 3.days

        dataSource.markFirstSeenIfAbsent()

        assertTrue(dataSource.checkAndMarkTriggered())
    }

    @Test
    fun incrementUploadCount_accumulatesAcrossCalls() = runTest {
        dataSource.markFirstSeenIfAbsent()
        now += 3.days

        dataSource.incrementUploadCount()
        assertFalse(dataSource.checkAndMarkTriggered())
        dataSource.incrementUploadCount()
        assertFalse(dataSource.checkAndMarkTriggered())
        dataSource.incrementUploadCount()
        assertTrue(dataSource.checkAndMarkTriggered())
    }
}
