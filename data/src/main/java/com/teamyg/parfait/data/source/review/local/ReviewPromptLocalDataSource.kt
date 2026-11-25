package com.teamyg.parfait.data.source.review.local

interface ReviewPromptLocalDataSource {
    suspend fun markFirstSeenIfAbsent()

    suspend fun incrementUploadCount()

    suspend fun checkAndMarkTriggered(): Boolean
}
