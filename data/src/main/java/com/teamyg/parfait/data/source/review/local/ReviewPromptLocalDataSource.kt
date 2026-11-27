package com.teamyg.parfait.data.source.review.local

interface ReviewPromptLocalDataSource {
    suspend fun incrementUploadCount()

    suspend fun checkTrigger(): Boolean
}
