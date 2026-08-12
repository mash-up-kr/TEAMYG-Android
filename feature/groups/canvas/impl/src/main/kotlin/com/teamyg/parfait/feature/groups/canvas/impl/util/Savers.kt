package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.runtime.saveable.Saver
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource

internal val PictureConfirmSourceSaver = Saver<PictureConfirmSource?, String>(
    save = { it?.name.orEmpty() },
    restore = { if (it.isEmpty()) null else PictureConfirmSource.valueOf(it) },
)
