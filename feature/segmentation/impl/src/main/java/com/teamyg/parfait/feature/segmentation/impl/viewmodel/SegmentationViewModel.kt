package com.teamyg.parfait.feature.segmentation.impl.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.feature.segmentation.impl.screen.decodeUriToBitmap
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SegmentationState(
    val originBitmap: Bitmap? = null,
    val overlayBitmap: Bitmap? = null,
    val subjectImagePath: String? = null,
) : UiState

sealed interface SegmentationIntent : UiIntent {
    data class LoadImage(val imageUri: String?) : SegmentationIntent

    object ChangeImage : SegmentationIntent
}

sealed interface SegmentationEffect : UiSideEffect

@HiltViewModel
class SegmentationViewModel
@Inject constructor(
    @ApplicationContext private val context: Context,
) : BaseViewModel<SegmentationState, SegmentationIntent, SegmentationEffect>(
    initialState = SegmentationState(),
) {
    private var inputImage: InputImage? = null // MLKit용 이미지

    override fun processIntent(intent: SegmentationIntent) {
        when (intent) {
            is SegmentationIntent.LoadImage -> {
                viewModelScope.launch {
                    val bitmap = withContext(Dispatchers.IO) {
                        decodeUriToBitmap(context.contentResolver, Uri.parse(intent.imageUri))
                    }
                    updateState { copy(originBitmap = bitmap) }
                    inputImage = InputImage.fromBitmap(bitmap, 0)
                    processIntent(SegmentationIntent.ChangeImage)
                }
            }

            is SegmentationIntent.ChangeImage -> {
                viewModelScope.launch {
                    val image = inputImage ?: return@launch

                    // 전경 신뢰도 마스크
                    val options = SubjectSegmenterOptions
                        .Builder()
                        .enableForegroundConfidenceMask()
                        .build()
                    // 주체 세그테이터 만들기
                    val segmenter = SubjectSegmentation.getClient(options)

                    val result = withContext(Dispatchers.IO) {
                        Tasks.await(segmenter.process(image))
                    }
                    withContext(Dispatchers.Default) {
                        // 분할 결과 가져오기
                        val foregroundMask = result.foregroundConfidenceMask!!
                        val overlayColors = IntArray(image.width * image.height)
                        val subjectColors = IntArray(image.width * image.height)

                        for (i in 0 until image.width * image.height) {
                            if (foregroundMask[i] > 0.5f) {
                                overlayColors[i] = Color.argb(128, 255, 0, 255)
                                subjectColors[i] =
                                    state.value.originBitmap!!.getPixel(i % image.width, i / image.width)
                            }
                        }
                        updateState {
                            copy(
                                overlayBitmap = Bitmap.createBitmap(
                                    overlayColors,
                                    image.width,
                                    image.height,
                                    Bitmap.Config.ARGB_8888,
                                ),
                            )
                        }
                        val subjectBitmap = Bitmap.createBitmap(
                            subjectColors,
                            image.width,
                            image.height,
                            Bitmap.Config.ARGB_8888,
                        )

                        val file = File(context.cacheDir, "subject.png")
                        withContext(Dispatchers.IO) {
                            file.outputStream().use { subjectBitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        }
                        updateState { copy(subjectImagePath = file.absolutePath) }
                    }
                }
            }
        }
    }
}
