package com.teamyg.parfait.feature.groups.canvas.impl

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

internal fun hasTestTagPrefix(prefix: String) = SemanticsMatcher("TestTag startsWith '$prefix'") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
}
