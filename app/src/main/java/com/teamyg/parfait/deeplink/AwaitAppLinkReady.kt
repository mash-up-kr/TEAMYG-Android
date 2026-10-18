package com.teamyg.parfait.deeplink

import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.intro.api.NavKeySplash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * App Links 로 쌓을 화면을 지금 올려도 되는 때까지 기다린다.
 *
 * 세션이 없으면 링크를 버리지 않고 이어간다 — 초대 링크는 계정이 없는 신규 유저가 가장 먼저
 * 누르는 경로다. 로그인(기존 유저)과 약관 동의(신규 유저) 둘 다 `replaceAll(NavKeyGroupList)`
 * 로 끝나므로 [NavKeyGroupList] 가 맨 위에 올라온 뒤에 쌓아야 그 `replaceAll` 에 지워지지 않고,
 * 뒤로 가면 그룹 목록이 남는다. 계정이 생긴 시점은 약관 동의 화면이 아직 맨 위인 때가 있어 기준이 못 된다.
 */
internal suspend fun awaitAppLinkReady(
    currentNavKey: Flow<NavKey?>,
    hasActiveSession: suspend () -> Boolean,
) {
    // 스플래시가 replaceAll 로 목적지를 확정하기 전에 쌓으면 그 replaceAll 이 링크를 지운다.
    currentNavKey.first { it != NavKeySplash }

    // 판정이 스플래시 대기 뒤에 있는 이유는 부트스트랩이 인증 거절을 받으면 토큰을 지우기 때문이다.
    if (!hasActiveSession()) {
        currentNavKey.first { it == NavKeyGroupList }
    }
}
