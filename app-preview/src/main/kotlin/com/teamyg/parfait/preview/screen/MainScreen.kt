package com.teamyg.parfait.preview.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
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
    toast: YGToastPolicy,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(color = Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Button(onClick = {
            toast.show(YGToastType.Edit("토스트 테스트"))
            toast.show(YGToastType.InviteCode("토스트 테스트22"))
        }) {
            Text("토스트 띄우기")
        }
        YGToastHost(
            policy = toast,
            modifier = Modifier.align(Alignment.TopCenter),
        )
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
        toast = rememberYGToastPolicy(),
        modifier = Modifier.fillMaxSize(),
        onComponentClick = {},
    )
}
