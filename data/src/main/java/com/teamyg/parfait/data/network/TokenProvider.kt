package com.teamyg.parfait.data.network

interface TokenProvider {
    fun getToken(): String?
}
