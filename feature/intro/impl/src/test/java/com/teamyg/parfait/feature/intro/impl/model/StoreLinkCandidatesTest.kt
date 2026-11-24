package com.teamyg.parfait.feature.intro.impl.model

import kotlin.test.Test
import kotlin.test.assertEquals

class StoreLinkCandidatesTest {
    private val packageName = "com.teamyg.parfait"
    private val market = "market://details?id=com.teamyg.parfait"
    private val web = "https://play.google.com/store/apps/details?id=com.teamyg.parfait"

    private fun uris(storeUrl: String?) = storeLinkCandidates(storeUrl, packageName).map { it.uri }

    @Test
    fun storeLinkCandidates_nullStoreUrl_returnsMarketThenWeb() {
        assertEquals(listOf(market, web), uris(null))
    }

    @Test
    fun storeLinkCandidates_marketStoreUrl_comesFirst() {
        val storeUrl = "market://details?id=other"

        assertEquals(listOf(storeUrl, market, web), uris(storeUrl))
    }

    @Test
    fun storeLinkCandidates_playHttpsStoreUrl_comesFirst() {
        val storeUrl = "https://play.google.com/store/apps/details?id=other"

        assertEquals(listOf(storeUrl, market, web), uris(storeUrl))
    }

    @Test
    fun storeLinkCandidates_otherHttpsStoreUrl_comesFirst() {
        val storeUrl = "https://example.com/notice/1"

        assertEquals(listOf(storeUrl, market, web), uris(storeUrl))
    }

    @Test
    fun storeLinkCandidates_intentScheme_isIgnored() {
        val storeUrl = "intent://scan/#Intent;scheme=zxing;package=com.example;end"

        assertEquals(listOf(market, web), uris(storeUrl))
    }

    @Test
    fun storeLinkCandidates_httpPlayHost_isIgnored() {
        val storeUrl = "http://play.google.com/store/apps/details?id=other"

        assertEquals(listOf(market, web), uris(storeUrl))
    }

    @Test
    fun storeLinkCandidates_malformed_isIgnored() {
        assertEquals(listOf(market, web), uris("not a url"))
        assertEquals(listOf(market, web), uris(""))
    }

    @Test
    fun storeLinkCandidates_storeUrlEqualsDefault_notDuplicated() {
        assertEquals(listOf(market, web), uris(market))
    }

    @Test
    fun storeLinkCandidates_onlyHttpsOpensInBrowser() {
        val links = storeLinkCandidates("https://example.com/notice/1", packageName)

        assertEquals(listOf(true, false, true), links.map { it.opensInBrowser })
    }
}
