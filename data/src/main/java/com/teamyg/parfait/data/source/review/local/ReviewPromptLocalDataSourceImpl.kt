package com.teamyg.parfait.data.source.review.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val UPLOAD_COUNT_THRESHOLD = 3
private const val RETENTION_DAYS = 3

// 4개월을 30일*4로 근사한다
private const val RETRIGGER_SNOOZE_DAYS = 120

private val FIRST_SEEN_KEY = stringPreferencesKey("review_prompt_first_seen")
private val UPLOAD_COUNT_KEY = stringPreferencesKey("review_prompt_upload_count")
private val LAST_TRIGGERED_KEY = stringPreferencesKey("review_prompt_last_triggered")

@Singleton
class ReviewPromptLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock = Clock.System,
) : ReviewPromptLocalDataSource {
    override suspend fun markFirstSeenIfAbsent() {
        dataStore.edit { prefs ->
            if (prefs[FIRST_SEEN_KEY] == null) prefs[FIRST_SEEN_KEY] = clock.now().toEpochMilliseconds().toString()
        }
    }

    override suspend fun incrementUploadCount() {
        dataStore.edit { prefs ->
            val current = prefs[UPLOAD_COUNT_KEY]?.toIntOrNull() ?: 0
            prefs[UPLOAD_COUNT_KEY] = (current + 1).toString()
        }
    }

    /**
     * 조건을 보고, 맞으면 같은 트랜잭션 안에서 바로 [LAST_TRIGGERED_KEY] 를 적는다 — 확인과
     * 기록 사이에 다른 업로드가 끼어들 여지를 없앤다.
     */
    override suspend fun checkAndMarkTriggered(): Boolean {
        var triggered = false
        dataStore.edit { prefs ->
            val uploadCount = prefs[UPLOAD_COUNT_KEY]?.toIntOrNull() ?: 0
            if (uploadCount < UPLOAD_COUNT_THRESHOLD) return@edit

            val firstSeenAt = prefs[FIRST_SEEN_KEY]?.let(::parseInstant) ?: return@edit
            val now = clock.now()
            if (now - firstSeenAt < RETENTION_DAYS.days) return@edit

            val lastTriggeredAt = prefs[LAST_TRIGGERED_KEY]?.let(::parseInstant)
            if (lastTriggeredAt != null && now - lastTriggeredAt < RETRIGGER_SNOOZE_DAYS.days) return@edit

            prefs[LAST_TRIGGERED_KEY] = now.toEpochMilliseconds().toString()
            triggered = true
        }
        return triggered
    }

    private fun parseInstant(raw: String): Instant? = raw.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
}
