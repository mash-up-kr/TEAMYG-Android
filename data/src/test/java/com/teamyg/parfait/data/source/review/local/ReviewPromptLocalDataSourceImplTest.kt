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
    fun checkTrigger_fewerThanThreeUploads_isFalse() = runTest {
        repeat(2) { dataSource.incrementUploadCount() }
        now += 4.days

        assertFalse(dataSource.checkTrigger())
    }

    @Test
    fun checkTrigger_threeUploadsButRetentionNotYetThreeDays_isFalse() = runTest {
        // 최초 감지 기준선은 checkTrigger 가 처음 불릴 때 스스로 세운다
        assertFalse(dataSource.checkTrigger())
        repeat(3) { dataSource.incrementUploadCount() }
        now += 2.days

        assertFalse(dataSource.checkTrigger())
    }

    @Test
    fun checkTrigger_threeUploadsAndThreeDaysRetention_isTrue() = runTest {
        assertFalse(dataSource.checkTrigger())
        repeat(3) { dataSource.incrementUploadCount() }
        now += 3.days

        assertTrue(dataSource.checkTrigger())
    }

    @Test
    fun checkTrigger_firstCall_marksFirstSeenWithoutTriggering() = runTest {
        // 기준선이 막 세워진 순간이라 "유지 3일"을 만족할 수 없다
        repeat(3) { dataSource.incrementUploadCount() }

        assertFalse(dataSource.checkTrigger())
    }

    @Test
    fun checkTrigger_recentlyTriggered_isFalseUntilFourMonthsPass() = runTest {
        assertFalse(dataSource.checkTrigger())
        repeat(3) { dataSource.incrementUploadCount() }
        now += 3.days

        // 첫 트리거 — 이 호출이 같은 자리에서 lastTriggered 도 함께 적는다
        assertTrue(dataSource.checkTrigger())

        // 다음날 조건이 다시 충족돼도(추가 업로드 등) 4개월 전에는 재시도하지 않는다
        now += 1.days
        assertFalse(dataSource.checkTrigger())

        // 호출로부터 119일째는 아직 막혀 있다
        now += 118.days
        assertFalse(dataSource.checkTrigger())

        // 정확히 120일(4개월)이 지나면 다시 트리거된다
        now += 1.days
        assertTrue(dataSource.checkTrigger())
    }

    @Test
    fun checkTrigger_calledRepeatedlyBeforeThreshold_keepsTheOriginalFirstSeenTimestamp() = runTest {
        // 기준선을 세운 첫 호출
        assertFalse(dataSource.checkTrigger())
        now += 3.days

        // 기준선이 이미 있으니 다시 불러도 바뀌지 않는다 — 업로드 3회를 채우면 그대로 트리거된다
        assertFalse(dataSource.checkTrigger())
        repeat(3) { dataSource.incrementUploadCount() }

        assertTrue(dataSource.checkTrigger())
    }

    @Test
    fun incrementUploadCount_accumulatesAcrossCalls() = runTest {
        assertFalse(dataSource.checkTrigger())
        now += 3.days

        dataSource.incrementUploadCount()
        assertFalse(dataSource.checkTrigger())
        dataSource.incrementUploadCount()
        assertFalse(dataSource.checkTrigger())
        dataSource.incrementUploadCount()
        assertTrue(dataSource.checkTrigger())
    }
}
