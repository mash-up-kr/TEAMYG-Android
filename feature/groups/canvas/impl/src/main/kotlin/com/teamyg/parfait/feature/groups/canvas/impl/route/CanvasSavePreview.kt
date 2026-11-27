package com.teamyg.parfait.feature.groups.canvas.impl.route

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.permission.GalleryWritePermissionManager
import com.teamyg.parfait.feature.groups.canvas.api.CANVAS_SAVE_RESULT_KEY
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveType
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasVideoSource
import com.teamyg.parfait.feature.groups.canvas.impl.model.PendingGallerySave
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.readCanvasCaptureCache
import com.teamyg.parfait.feature.groups.canvas.impl.util.writeToCanvasCaptureCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * 저장 미리보기(`NavKeyCanvasImageSave`)에서 돌아온 결과를 갤러리 저장으로 잇는다. 그룹·혼자 공간
 * 캔버스 홈이 함께 쓴다.
 *
 * 이미지는 보여 준 그림 그대로 남겨야 하므로 캔버스를 다시 캡처하지 않고 미리보기가 쓰던 파일을
 * 읽는다. 동영상은 미리보기가 이미 캐시에 만들어 왔고 여기서는 갤러리에 넣기만 한다.
 *
 * @param onSave 쓰기 권한이 확인된 뒤에만 불린다
 * @param onDiscardVideo 권한이 거부돼 만들어 온 mp4 를 저장하지 않게 됐다
 */
@Composable
internal fun CanvasSaveResultEffect(
    toastPolicy: YGToastPolicy,
    onSave: (PendingGallerySave) -> Unit,
    onDiscardVideo: (filePath: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gallerySaveFailureMessage = stringResource(R.string.canvas_main_gallery_save_failure)
    val shareFailedMessage = stringResource(R.string.canvas_share_failed)

    // WRITE_EXTERNAL_STORAGE 요청은 Activity 가 있어야만 가능해, 저장할 것을 여기서 들고 있다가
    // 승인이 오면 그때 넘긴다
    var pendingGallerySave by remember { mutableStateOf<PendingGallerySave?>(null) }
    val galleryWritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingGallerySave
        pendingGallerySave = null
        if (pending != null) {
            if (granted) {
                onSave(pending)
            } else {
                if (pending is PendingGallerySave.Video) {
                    onDiscardVideo(pending.filePath)
                }
                toastPolicy.showError(gallerySaveFailureMessage)
            }
        }
    }

    val saveWithPermission: (PendingGallerySave) -> Unit = { pending ->
        if (GalleryWritePermissionManager.hasPermission(context)) {
            onSave(pending)
        } else {
            pendingGallerySave = pending
            galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
        }
    }

    ResultEffect<CanvasSaveResult>(resultKey = CANVAS_SAVE_RESULT_KEY) { result ->
        when (result) {
            is CanvasSaveResult.Image -> scope.launch {
                withContext(Dispatchers.IO) { readCanvasCaptureCache(result.imagePath) }
                    .onSuccess { bitmap -> saveWithPermission(PendingGallerySave.Image(bitmap)) }
                    .onFailure { toastPolicy.showError(gallerySaveFailureMessage) }
            }

            is CanvasSaveResult.Video -> saveWithPermission(PendingGallerySave.Video(result.videoPath))

            is CanvasSaveResult.VideoFailed -> toastPolicy.showError(gallerySaveFailureMessage)

            is CanvasSaveResult.ShareFailed -> toastPolicy.showError(shareFailedMessage)
        }
    }
}

/**
 * 캔버스 프레임을 캡처해 캐시에 쓰고 저장 미리보기로 간다.
 *
 * [date]·[videoSource] 는 캡처 **직후** 읽는다 — 아래 정지 지점(파일 쓰기) 사이에 캔버스가 바뀌면,
 * 나중에 읽은 토핑이 이미 캡처된 그림과 어긋난다.
 */
internal suspend fun GraphicsLayer.openCanvasSavePreview(
    context: Context,
    navigator: Navigator,
    saveType: CanvasSaveType,
    date: () -> LocalDate,
    videoSource: () -> CanvasVideoSource,
    onFailure: () -> Unit,
) {
    val bitmap = toImageBitmap().asAndroidBitmap()
    val capturedDate = date()
    val capturedVideoSource = videoSource()

    withContext(Dispatchers.IO) { bitmap.writeToCanvasCaptureCache(context) }
        .onSuccess { file ->
            // ⚠️ 홀더에 건넸다고 파일 쓰기를 지우면 안 된다 — CanvasSaveResultEffect 가
            // 저장할 때 그 파일을 읽는다
            CanvasCaptureHolder.put(bitmap)
            CanvasVideoSourceHolder.put(capturedVideoSource)
            navigator.goTo(
                destination = NavKeyCanvasImageSave(
                    saveType = saveType,
                    imagePath = file.absolutePath,
                    date = capturedDate.toString(),
                ),
            )
        }.onFailure { onFailure() }
}
