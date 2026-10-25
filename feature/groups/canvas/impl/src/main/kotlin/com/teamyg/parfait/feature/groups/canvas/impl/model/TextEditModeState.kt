package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** C-106 편집(키보드 ↑)·배치(키보드 ↓) 모드와 그 전환이 거느리는 포커스·키보드 입출력 */
internal class TextEditModeState(
    val isEditing: Boolean,
    val focusRequester: FocusRequester,
    val endEditing: () -> Unit,
    val resumeEditing: () -> Unit,
    val onFocusChanged: (isFocused: Boolean) -> Unit,
)

/**
 * [isContentBlank] 인 채로 편집이 끝나면([TextEditModeState.endEditing]) [onConfirmEmpty] 로 생성을
 * 취소한다 — ✓·빈 곳 탭·안드로이드 백 세 경로가 모두 그 하나로 모인다.
 */
@Composable
internal fun rememberTextEditModeState(
    isContentBlank: Boolean,
    onConfirmEmpty: () -> Unit,
): TextEditModeState {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isEditing by rememberSaveable { mutableStateOf(true) }
    var everFocused by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!everFocused) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val endEditing: () -> Unit = {
        if (isContentBlank) {
            onConfirmEmpty()
        } else {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // 키보드가 내려갔는지 감지용
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isImeVisible = imeBottomPx > 0
    var keyboardWasShown by remember { mutableStateOf(false) }
    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            keyboardWasShown = true
        } else if (keyboardWasShown && isEditing) {
            endEditing()
        }
    }

    // 키보드가 떠 있는 동안의 백이 앱까지 닿는 드문 경우(일부 기기·외장 키보드)를 위한 명시적 경로.
    // 보통은 IME 가 백을 먼저 먹어 위 관측이 처리한다 — 둘은 서로 다른 경우를 덮는다
    BackHandler(enabled = isEditing) { endEditing() }

    return TextEditModeState(
        isEditing = isEditing,
        focusRequester = focusRequester,
        endEditing = endEditing,
        resumeEditing = {
            focusRequester.requestFocus()
            keyboardController?.show()
        },
        onFocusChanged = { isFocused ->
            if (isFocused) {
                everFocused = true
                isEditing = true
            } else if (everFocused) {
                isEditing = false
            }
        },
    )
}
