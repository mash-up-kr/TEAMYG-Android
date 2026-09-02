package com.teamyg.parfait.data.repository.image

import android.content.Context
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.teamyg.parfait.data.utils.repositoryLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 모듈 식별자는 세그멘터 옵션과 무관하다 — `SubjectSegmenter` 구현이 옵션이 뭐든 세그멘테이션
 * feature 하나만 내놓는다. 그래서 판정용 세그멘터를 기본 옵션으로 따로 열어도 결과가 같다.
 */
class PlayServicesModuleInstallGateway
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : ModuleInstallGateway {
    private val client = ModuleInstall.getClient(context)

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            probeSegmenter().use { Tasks.await(client.areModulesAvailable(it)).areModulesAvailable() }
        }.getOrElse { throwable ->
            repositoryLogger.w(throwable) { "[MLKIT-MODULE] 가용 여부 확인이 실패했다 — 없는 것으로 본다" }
            false
        }
    }

    override fun install(onSignal: (ModuleInstallSignal) -> Unit) {
        val segmenter = probeSegmenter()

        lateinit var listener: InstallStatusListener
        listener = InstallStatusListener { update ->
            repositoryLogger.i {
                "[MLKIT-MODULE] 설치 상태 ${update.installState}, 오류 코드 ${update.errorCode}, " +
                    "세션 ${update.sessionId}"
            }

            // STATE_COMPLETED 는 같은 패키지 SegmentationModuleInstaller.kt 의 상수다
            // (GMS 상수와 값이 같고, 설치기가 재확인 실패를 표시할 때도 쓴다)
            when (update.installState) {
                STATE_COMPLETED -> onSignal(ModuleInstallSignal.Completed)
                STATE_FAILED, STATE_CANCELED ->
                    onSignal(ModuleInstallSignal.Failed(update.installState, update.errorCode))
                else -> return@InstallStatusListener
            }

            client.unregisterListener(listener)
            segmenter.close()
        }

        val request = ModuleInstallRequest
            .newBuilder()
            .addApi(segmenter)
            .setListener(listener)
            .build()

        client
            .installModules(request)
            .addOnSuccessListener { response ->
                if (response.areModulesAlreadyInstalled()) {
                    client.unregisterListener(listener)
                    segmenter.close()
                    onSignal(ModuleInstallSignal.AlreadyInstalled)
                }
            }.addOnFailureListener { throwable ->
                repositoryLogger.w(throwable) { "[MLKIT-MODULE] 설치 요청 자체가 실패했다" }
                client.unregisterListener(listener)
                segmenter.close()
                onSignal(ModuleInstallSignal.Failed(installState = STATE_FAILED, errorCode = 0))
            }
    }

    /** 모듈 판정에만 쓰고 process 에 넘기지 않으므로 이 게이트웨이가 열고 닫는다 */
    private fun probeSegmenter() = SubjectSegmentation.getClient(SubjectSegmenterOptions.Builder().build())
}
