package com.teamyg.parfait.data.repository.image

import android.content.Context
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiException as GmsApiException
import com.google.android.gms.common.api.OptionalModuleApi
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.google.android.gms.tasks.Tasks
import com.teamyg.parfait.data.utils.repositoryLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val SUBJECT_SEGMENTATION_FEATURE = "mlkit.segmentation.subject"
private const val SUBJECT_SEGMENTATION_FEATURE_VERSION = 1L

class PlayServicesModuleInstallGateway
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ModuleInstallGateway {
    private val client = ModuleInstall.getClient(context)

    /**
     * 판정에 `SubjectSegmenter`(`SubjectSegmentation.getClient`)를 쓰지 않는다 — 그건 그
     * 자체로 네이티브 그래프와 EGL 컨텍스트를 띄운다. 판정용 그래프가 닫히는 도중 실제
     * 세그멘테이션 클라이언트가 또 하나의 그래프를 띄우면서 겹쳐 Galaxy Z Flip 3(Android 15)
     * 실기기에서 SIGBUS로 죽는 것을 확인했다. `OptionalModuleApi`는 feature 배열만 돌려주면
     * 되므로 그래프를 띄우지 않고 판정만 한다.
     *
     * feature 이름·버전의 근거: 실기기 logcat과
     * `parfait/specs/2026-09-02-segmentation-module-install.md`.
     */
    private val segmentationModule = OptionalModuleApi {
        arrayOf(Feature(SUBJECT_SEGMENTATION_FEATURE, SUBJECT_SEGMENTATION_FEATURE_VERSION))
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Tasks.await(client.areModulesAvailable(segmentationModule)).areModulesAvailable()
        }.getOrElse { throwable ->
            repositoryLogger.w(throwable) { "[MLKIT-MODULE] 가용 여부 확인이 실패했다 — 없는 것으로 본다" }
            false
        }
    }

    override fun install(onSignal: (ModuleInstallSignal) -> Unit) {
        lateinit var listener: InstallStatusListener
        listener = InstallStatusListener { update ->
            repositoryLogger.i {
                "[MLKIT-MODULE] 설치 상태 ${update.installState}, 오류 코드 ${update.errorCode}, " +
                    "세션 ${update.sessionId}"
            }

            when (update.installState) {
                STATE_COMPLETED -> onSignal(ModuleInstallSignal.Completed)

                STATE_FAILED, STATE_CANCELED ->
                    onSignal(ModuleInstallSignal.Failed(update.installState, update.errorCode))

                else -> return@InstallStatusListener
            }

            client.unregisterListener(listener)
        }

        val request = ModuleInstallRequest
            .newBuilder()
            .addApi(segmentationModule)
            .setListener(listener)
            .build()

        client
            .installModules(request)
            .addOnSuccessListener { response ->
                if (response.areModulesAlreadyInstalled()) {
                    client.unregisterListener(listener)
                    onSignal(ModuleInstallSignal.AlreadyInstalled)
                }
            }.addOnFailureListener { throwable ->
                val statusCode = (throwable as? GmsApiException)?.statusCode ?: 0
                repositoryLogger.w(throwable) { "[MLKIT-MODULE] 설치 요청 자체가 실패했다, 상태 코드 $statusCode" }
                client.unregisterListener(listener)
                onSignal(ModuleInstallSignal.Failed(installState = STATE_FAILED, errorCode = statusCode))
            }
    }
}
