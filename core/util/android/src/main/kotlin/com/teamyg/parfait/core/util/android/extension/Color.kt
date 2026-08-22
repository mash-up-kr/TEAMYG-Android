package com.teamyg.parfait.core.util.android.extension

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.util.Locale

private const val RGB_MASK = 0xFFFFFF

private const val RGB_HEX_FORMAT = "#%06X"

/**
 * 색을 `#RRGGBB` 여섯 자리로 적는다. [String.toColorOrNull] 의 반대 방향이다.
 *
 * 알파를 버리는 이유: 서버 계약이 여섯 자리만 받는다(3자리 축약·8자리 알파는 거부).
 *
 * 로케일을 고정하는 이유: 기본 로케일이 아라비아·데바나가리 숫자를 쓰는 기기에서는
 * `%X` 가 그 숫자로 적혀 서버가 못 읽는 문자열이 된다.
 */
fun Color.toRgbHex(): String = String.format(Locale.US, RGB_HEX_FORMAT, toArgb() and RGB_MASK)
