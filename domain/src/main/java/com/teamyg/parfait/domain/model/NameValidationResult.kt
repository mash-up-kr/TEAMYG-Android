package com.teamyg.parfait.domain.model

data class NameValidationResult(
    val isSuccess: Boolean,
    val errorMessage: String?,
)
