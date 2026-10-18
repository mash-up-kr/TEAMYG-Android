package com.teamyg.parfait.core.designsystem.theme.colors

import androidx.compose.ui.graphics.Color

object YGAtomicColors {
    object Gray {
        val White: Color = Color(0xFFFAFAFA)
        val Black: Color = Color(0xFF0E0E0E)
        val Transparent: Color = Color.Transparent

        val Gray50: Color = Color(0xFFF6F6F9)
        val Gray100: Color = Color(0xFFECECEE)
        val Gray200: Color = Color(0xFFDDDEE0)
        val Gray300: Color = Color(0xFFAAACB2)
        val Gray400: Color = Color(0xFF95969B)
        val Gray500: Color = Color(0xFF7A7D82)
        val Gray600: Color = Color(0xFF6F7176)
        val Gray700: Color = Color(0xFF57585C)
        val Gray800: Color = Color(0xFF434448)
        val Gray850: Color = Color(0xFF333537)
        val Gray900: Color = Color(0xFF29292C)
        val Gray950: Color = Color(0xFF1B1B1B)
    }

    object Cherry {
        val Cherry: Color = Color(0xFFF40B31)

        val Cherry50: Color = Color(0xFFFFF6F8)
        val Cherry100: Color = Color(0xFFFEE6EA)
        val Cherry200: Color = Color(0xFFFCC2CC)
        val Cherry300: Color = Color(0xFFFA91A2)
        val Cherry400: Color = Color(0xFFF86078)
        val Cherry500: Color = Color(0xFFF63050)
        val Cherry600: Color = Color(0xFFDC0A2C)
        val Cherry700: Color = Color(0xFFCF092A)
    }

    object Melon {
        val Melon500: Color = Color(0xFF2FFFC1)
        val Melon600: Color = Color(0xFF2AE6AE)
    }

    object Pudding {
        val Pudding500: Color = Color(0xFFFFFF93)
        val Pudding600: Color = Color(0xFFFFFF32)
    }

    object Soda {
        val Soda500: Color = Color(0xFF2B9BE7)
    }

    /**
     * 텍스트 토핑 색 팔레트. [Stroke] 는 `Function/Text/Stroke` 계열(진한 톤 — 테두리·Default
     * 타입 글자색), [Fill] 은 `Function/Text/Fill` 계열(옅은 톤 — Fill·Fill+Stroke 타입의
     * 네모배경색)이다.
     *
     * Black/White 만 예외로 자기 색의 옅은 버전을 쓰지 않고 서로 교차한다 — White 텍스트의 Fill
     * 배경은 [Gray.Black], Black 텍스트의 Fill 배경은 [Gray.White]. 둘 다 이미 [Gray] 에 있어
     * 여기 새 값을 두지 않는다.
     */
    object Pink {
        val Stroke: Color = Color(0xFFFA91A2)
        val Fill: Color = Color(0xFFFFF9FA)
    }

    object Orange {
        val Stroke: Color = Color(0xFFFFBB42)
        val Fill: Color = Color(0xFFFFFBF3)
    }

    object Yellow {
        val Stroke: Color = Color(0xFFFFD21F)
        val Fill: Color = Color(0xFFFEFEF6)
    }

    object Green {
        val Stroke: Color = Color(0xFF64DB88)
        val Fill: Color = Color(0xFFF4FCF7)
    }

    object Blue {
        val Stroke: Color = Color(0xFF6DBBF2)
        val Fill: Color = Color(0xFFF6FBFE)
    }

    object Violet {
        val Stroke: Color = Color(0xFFCEA8FC)
        val Fill: Color = Color(0xFFFAF4FF)
    }

    object Transparency {
        private val GeneralWhite = Color(0xFFFFFFFF)

        val White25 = GeneralWhite.copy(alpha = 0.25f)
        val White50 = GeneralWhite.copy(alpha = 0.5f)
        val White75 = GeneralWhite.copy(alpha = 0.75f)

        val Black5 = Gray.Black.copy(alpha = 0.05f)
        val Black25 = Gray.Black.copy(alpha = 0.25f)
        val Black50 = Gray.Black.copy(alpha = 0.5f)
        val Black75 = Gray.Black.copy(alpha = 0.75f)
    }
}
