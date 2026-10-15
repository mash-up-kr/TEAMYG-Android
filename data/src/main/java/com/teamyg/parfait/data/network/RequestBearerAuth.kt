package com.teamyg.parfait.data.network

import com.teamyg.parfait.data.network.NetworkConstValue.AUTHORIZATION_HEADER
import com.teamyg.parfait.data.network.NetworkConstValue.BEARER_PREFIX
import okhttp3.Request

/** `Authorization` 헤더를 이 토큰 하나로 교체한다 */
internal fun Request.Builder.bearerAuth(token: String): Request.Builder =
    header(AUTHORIZATION_HEADER, "$BEARER_PREFIX$token")
