package com.teamyg.parfait.data.datastore

interface RecentImageEditor {
    fun get(): String?

    fun set(value: String)
}
