package com.teamyg.parfait.data.source.review.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val UPLOAD_COUNT_THRESHOLD = 3
private const val RETENTION_DAYS = 3

// 4개월을 30일*4로 근사한다
private const val RETRIGGER_SNOOZE_DAYS = 120

private val FIRST_SEEN_KEY = longPreferencesKey("review_prompt_first_seen")
private val UPLOAD_COUNT_KEY = intPreferencesKey("review_prompt_upload_count")
private val LAST_TRIGGERED_KEY = longPreferencesKey("review_prompt_last_triggered")

/**
 * 임계값 비교(업로드 3회·유지 3일·재시도 120일)를 domain 이 아니라 여기서 한다 — "최초 감지
 * 기록"과 "조건 확인 + 트리거 기록"을 각각 하나의 [DataStore.edit] 트랜잭션 안에 묶어야
 * 두 단계 사이에 다른 업로드·리프레시가 끼어들 여지가 없다.
 */
@Singleton
class ReviewPromptLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.System,
) : ReviewPromptLocalDataSource {
    override suspend fun incrementUploadCount() {
        dataStore.edit { prefs ->
            val current = prefs[UPLOAD_COUNT_KEY] ?: 0
            prefs[UPLOAD_COUNT_KEY] = current + 1
        }
    }

    /**
     * "최초 감지"가 아직 없으면 이 호출로 세우고, 업로드 3회 이상 + 그 기준선으로부터 3일
     * 이상 + (미호출 또는 마지막 호출로부터 4개월 이상)이면 같은 트랜잭션 안에서 바로
     * [LAST_TRIGGERED_KEY] 를 적고 true 를 돌린다.
     */
    override suspend fun checkTrigger(): Boolean {
        var triggered = false
        dataStore.edit { prefs ->
            val now = clock.now()
            if (prefs[FIRST_SEEN_KEY] == null) prefs[FIRST_SEEN_KEY] = now.toEpochMilliseconds()

            val uploadCount = prefs[UPLOAD_COUNT_KEY] ?: 0
            if (uploadCount < UPLOAD_COUNT_THRESHOLD) return@edit

            val firstSeenAt = prefs[FIRST_SEEN_KEY]?.let(Instant::fromEpochMilliseconds) ?: return@edit
            if (now - firstSeenAt < RETENTION_DAYS.days) return@edit

            val lastTriggeredAt = prefs[LAST_TRIGGERED_KEY]?.let(Instant::fromEpochMilliseconds)
            if (lastTriggeredAt != null && now - lastTriggeredAt < RETRIGGER_SNOOZE_DAYS.days) return@edit

            prefs[LAST_TRIGGERED_KEY] = now.toEpochMilliseconds()
            triggered = true
        }
        return triggered
    }
}
