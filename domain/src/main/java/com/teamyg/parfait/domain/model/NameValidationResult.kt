package com.teamyg.parfait.domain.model

sealed interface NameValidResult {
    data object Success : NameValidResult

    sealed interface Error : NameValidResult {
        data object SpaceAtEdge : Error

        data object DuplicatedSpace : Error

        data object InvalidCharacter : Error
    }
}
