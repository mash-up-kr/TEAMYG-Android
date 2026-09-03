package com.teamyg.parfait.feature.groups.enter.impl.component

import androidx.lifecycle.ViewModel
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.qualifier.ApplicationScope
import com.teamyg.parfait.domain.usecase.notification.RegisterCurrentDeviceTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 권한을 막 허용한 뒤 지금 기기의 토큰을 등록만 걸어 두는 자리 — 결과를 화면에
 * 보여줄 일이 없어 상태·이펙트 없이 실행만 한다([PictureConfirmViewModel] 과 같은 결).
 *
 * `viewModelScope` 대신 [ApplicationScope] 를 쓴다 — 권한을 허용한 바로 그 콜백이
 * [onNotificationPermissionGranted] 를 부른 뒤 곧장 `navigator.replaceAll(...)` 로
 * 이 화면의 NavEntry(=이 ViewModel 의 store)를 걷어낸다. `viewModelScope` 였다면
 * 토큰 조회·서버 등록(네트워크 왕복)이 끝나기 전에 `onCleared()` 가 먼저 불려 등록이
 * 조용히 취소된다.
 */
@HiltViewModel
class NotificationPermissionViewModel @Inject constructor(
    private val registerCurrentDeviceTokenUseCase: RegisterCurrentDeviceTokenUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    fun onNotificationPermissionGranted() {
        applicationScope.launch {
            registerCurrentDeviceTokenUseCase()
                .onFailure { viewModelLogger.w(it) { "권한 허용 직후 토큰 등록이 실패했다" } }
        }
    }
}
