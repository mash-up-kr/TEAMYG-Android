package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.view.PreviewView
import androidx.compose.runtime.State
import com.teamyg.parfait.feature.camera.impl.model.CameraTransition

internal class CameraPreviewHandle(
    val previewView: PreviewView,
    val camera: State<Camera?>,
    val transition: State<CameraTransition?>,
)
