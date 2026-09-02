package com.teamyg.parfait.feature.camera.impl.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.usecase.image.PrepareSegmentationModuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 사진 확인 화면이 세그멘테이션 모델을 미리 받아 두는 자리.
 *
 * ⚠️ **카메라 화면이 아니라 여기인 이유**는 진입 경로가 둘이기 때문이다 — 촬영과 갤러리 선택이
 * 이 화면에서 합쳐지고, 세그멘테이션으로 가는 길은 여기 하나다. 근거는
 * `parfait/specs/2026-09-02-segmentation-module-install.md`.
 */
@HiltViewModel
class PictureConfirmViewModel
@Inject
constructor(
    private val prepareSegmentationModuleUseCase: PrepareSegmentationModuleUseCase,
) : ViewModel() {
    /**
     * 결과를 안 본다. 실패는 실제로 세그멘테이션을 시도하는 화면이 받고, 이 화면은 사용자를
     * 붙잡지 않는다.
     */
    fun prepareSegmentationModule() {
        viewModelScope.launch {
            runSuspendCatching { prepareSegmentationModuleUseCase() }
                .onFailure { viewModelLogger.w(it) { "세그멘테이션 모듈 사전 준비가 실패했다" } }
        }
    }
}
