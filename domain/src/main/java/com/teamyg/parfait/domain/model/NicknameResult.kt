package com.teamyg.parfait.domain.model

sealed interface NicknameResult {
    data object Success : NicknameResult

    sealed interface Error : NicknameResult {
        data object Empty : Error

        data object SpaceAtEdge : Error

        data object DuplicatedSpace : Error

        data object InvalidCharacter : Error
    }
}
