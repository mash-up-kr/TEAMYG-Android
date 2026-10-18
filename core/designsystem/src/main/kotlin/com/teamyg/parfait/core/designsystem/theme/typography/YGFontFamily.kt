package com.teamyg.parfait.core.designsystem.theme.typography

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.teamyg.parfait.core.designsystem.R

/**
 * 앱 기본 글꼴은 [suitFontFamily](한/영/숫자). 텍스트 토핑은 입력 언어에 따라 일본어는
 * [lineSeedJpFontFamily], 중국어는 [lineSeedTwFontFamily] 로 바꿔 쓴다(LINE Seed, 공식 배포본 —
 * 라이선스는 `core/designsystem/licenses/`). 폰트 패밀리는 디자인시스템 밖(feature)에서도 써야 해
 * 공개한다.
 */
object YGFontFamily {
    val suitFontFamily: FontFamily = FontFamily(
        Font(R.font.suit_regular, FontWeight.Normal),
        Font(R.font.suit_medium, FontWeight.Medium),
        Font(R.font.suit_semi_bold, FontWeight.SemiBold),
        Font(R.font.suit_bold, FontWeight.Bold),
    )

    /** 일본어 — Line Seed JP Regular(400). 스펙상 Regular 1종만 쓴다 */
    val lineSeedJpFontFamily: FontFamily = FontFamily(
        Font(R.font.line_seed_jp_regular, FontWeight.Normal),
    )

    /** 중국어(번체) — Line Seed TW Regular(400). 스펙상 Regular 1종만 쓴다 */
    val lineSeedTwFontFamily: FontFamily = FontFamily(
        Font(R.font.line_seed_tw_regular, FontWeight.Normal),
    )
}
