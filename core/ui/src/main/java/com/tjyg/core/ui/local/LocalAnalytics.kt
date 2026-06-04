package com.tjyg.core.ui.local

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.teamyg.analytics.AnalyticsHelper

val LocalAnalyticsHelper: ProvidableCompositionLocal<AnalyticsHelper> =
    staticCompositionLocalOf {
        error("Not inject value yet")
    }
