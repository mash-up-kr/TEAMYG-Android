package com.teamyg.parfait.domain.repository.push

import com.teamyg.parfait.domain.model.push.PushDeepLink
import kotlinx.coroutines.flow.Flow

/**
 * 푸시 딥링크 구독구. 앱 루트 한 곳에서만 수집한다
 */
interface PushDeepLinkSource {
    val deepLinks: Flow<PushDeepLink>
}
