package com.teamyg.parfait.domain.repository.pushdeeplink

import com.teamyg.parfait.domain.model.pushdeeplink.PushDeepLink
import kotlinx.coroutines.flow.Flow

/**
 * 푸시 딥링크 구독구. 앱 루트 한 곳에서만 수집한다
 */
interface PushDeepLinkSource {
    val deepLinks: Flow<PushDeepLink>
}
