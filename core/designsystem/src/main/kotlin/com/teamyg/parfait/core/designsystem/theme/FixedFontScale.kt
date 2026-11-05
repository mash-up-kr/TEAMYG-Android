package com.teamyg.parfait.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * 하위 텍스트가 OS 글자 크기를 무시하게 한다.
 * `Dialog`·`Popup`은 새 창에서 기기 값으로 되돌리므로 그 content 안에서 다시 감싼다.
 */
@Composable
fun ProvideFixedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    val fixedDensity = remember(density) { Density(density = density, fontScale = 1f) }

    CompositionLocalProvider(LocalDensity provides fixedDensity, content = content)
}
