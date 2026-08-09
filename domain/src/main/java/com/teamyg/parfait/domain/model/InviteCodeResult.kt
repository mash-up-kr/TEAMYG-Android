package com.teamyg.parfait.domain.model

data class InviteCodeResult(
    val isSuccess: Boolean,
    val errorMessage: String?,
    val groupName: String,
)
