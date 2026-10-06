package com.teamyg.parfait.feature.camera.impl.viewmodel

import app.cash.turbine.test
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomCameraViewModelTest {
    private fun createViewModel() = CustomCameraViewModel(
        createCameraCacheFileUseCase = mockk(),
        createCameraCacheUriUseCase = mockk(),
    )

    @Test
    fun permission_whenDeniedOnEntry_requestsSystemDialog() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = false))

            assertEquals(CustomCameraEffect.RequestPermission, awaitItem())
        }
    }

    @Test
    fun permission_whenStillDeniedAfterRequest_doesNotRequestAgain() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = false))
            assertEquals(CustomCameraEffect.RequestPermission, awaitItem())

            // 다이얼로그가 닫힐 때마다 재개 확인이 들어오므로, 여기서 또 띄우면 끝없이 돈다
            viewModel.processIntent(
                CustomCameraIntent.OnPermissionRequestResult(granted = false, shouldShowRationale = false),
            )
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = false))

            expectNoEvents()
        }
    }

    @Test
    fun permission_whenGrantedOnEntry_doesNotRequest() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = true))

            expectNoEvents()
        }
    }
}
