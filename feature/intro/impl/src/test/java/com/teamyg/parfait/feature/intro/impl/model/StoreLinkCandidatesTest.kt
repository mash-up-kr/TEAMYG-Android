package com.teamyg.parfait.feature.intro.impl.model

import kotlin.test.Test
import kotlin.test.assertEquals

class StoreLinkCandidatesTest {
    private val packageName = "com.teamyg.parfait"
    private val market = "market://details?id=com.teamyg.parfait"
    private val web = "https://play.google.com/store/apps/details?id=com.teamyg.parfait"

    @Test
    fun storeLinkCandidates_nullStoreUrl_returnsMarketThenWeb() {
        assertEquals(listOf(market, web), storeLinkCandidates(null, packageName))
    }

    @Test
    fun storeLinkCandidates_marketStoreUrl_comesFirst() {
        val storeUrl = "market://details?id=other"

        assertEquals(listOf(storeUrl, market, web), storeLinkCandidates(storeUrl, packageName))
    }

    @Test
    fun storeLinkCandidates_playHttpsStoreUrl_comesFirst() {
        val storeUrl = "https://play.google.com/store/apps/details?id=other"

        assertEquals(listOf(storeUrl, market, web), storeLinkCandidates(storeUrl, packageName))
    }

    @Test
    fun storeLinkCandidates_nonPlayHttps_isIgnored() {
        assertEquals(listOf(market, web), storeLinkCandidates("https://example.com/app.apk", packageName))
    }

    @Test
    fun storeLinkCandidates_httpPlayHost_isIgnored() {
        val storeUrl = "http://play.google.com/store/apps/details?id=other"

        assertEquals(listOf(market, web), storeLinkCandidates(storeUrl, packageName))
    }

    @Test
    fun storeLinkCandidates_malformed_isIgnored() {
        assertEquals(listOf(market, web), storeLinkCandidates("not a url", packageName))
        assertEquals(listOf(market, web), storeLinkCandidates("", packageName))
    }

    @Test
    fun storeLinkCandidates_storeUrlEqualsDefault_notDuplicated() {
        assertEquals(listOf(market, web), storeLinkCandidates(market, packageName))
    }
}
