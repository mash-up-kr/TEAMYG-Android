package com.teamyg.parfait.preview.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtext.YGLabel
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.preview.model.ComponentCategory
import com.teamyg.parfait.preview.model.componentCatalog

@Composable
internal fun MainScreen(
    onComponentClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = "Component Preview",
                style = YGTheme.typography.title.t03SB,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        ComponentCategory.entries.forEach { category ->
            val entries = componentCatalog.filter { it.category == category }
            if (entries.isNotEmpty()) {
                item(key = "header_${category.name}") {
                    YGLabel(
                        text = category.label,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(
                    items = entries,
                    key = { it.label },
                ) { entry ->
                    YGButton(
                        text = entry.label,
                        buttonType = YGButtonType.Medium.Secondary,
                        isEnabled = true,
                        onClick = { onComponentClick(entry.navKey) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewMainScreen() = PreviewBox {
    MainScreen(
        onComponentClick = {},
    )
}
