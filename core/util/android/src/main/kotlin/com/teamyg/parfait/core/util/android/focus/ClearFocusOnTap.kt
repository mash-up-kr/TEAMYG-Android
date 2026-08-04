package com.teamyg.parfait.core.util.android.focus

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * 빈 영역을 탭하면 현재 포커스를 해제한다
 */
@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current

    return pointerInput(focusManager) {
        detectTapGestures(
            onTap = { focusManager.clearFocus() },
        )
    }
}
