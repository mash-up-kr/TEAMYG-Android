package com.teamyg.parfait.preview.navigation.entry

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.navigation.key.NavKeyYGActionItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGChipButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDangerZone
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDate
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDateButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGHorizontalDivider
import com.teamyg.parfait.preview.navigation.key.NavKeyYGIconButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInputNumber
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInviteCard
import com.teamyg.parfait.preview.navigation.key.NavKeyYGLabel
import com.teamyg.parfait.preview.navigation.key.NavKeyYGListItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGModalPopup
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextFormField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToggleButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTopBar
import com.teamyg.parfait.preview.screen.component.YGActionItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGChipButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDangerZonePreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDatePreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDateButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGHorizontalDividerPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGIconButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGInputNumberPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGInviteCardPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGLabelPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGListItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGModalPopupPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTextFieldPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTextFormFieldPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGToggleButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTopBarPreviewScreen

internal fun EntryProviderScope<NavKey>.componentEntryBuilders(navigator: Navigator) {
    entry<NavKeyYGButton> {
        ScreenScaffold { modifier ->
            YGButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGChipButton> {
        ScreenScaffold { modifier ->
            YGChipButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGToggleButton> {
        ScreenScaffold { modifier ->
            YGToggleButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGIconButton> {
        ScreenScaffold { modifier ->
            YGIconButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGDateButton> {
        ScreenScaffold { modifier ->
            YGDateButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGInputNumber> {
        ScreenScaffold { modifier ->
            YGInputNumberPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGTextField> {
        ScreenScaffold { modifier ->
            YGTextFieldPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGTextFormField> {
        ScreenScaffold { modifier ->
            YGTextFormFieldPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGLabel> {
        ScreenScaffold { modifier ->
            YGLabelPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGDate> {
        ScreenScaffold { modifier ->
            YGDatePreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGActionItem> {
        ScreenScaffold { modifier ->
            YGActionItemPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGModalPopup> {
        ScreenScaffold { modifier ->
            YGModalPopupPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGInviteCard> {
        ScreenScaffold { modifier ->
            YGInviteCardPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGDangerZone> {
        ScreenScaffold { modifier ->
            YGDangerZonePreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGListItem> {
        ScreenScaffold { modifier ->
            YGListItemPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGHorizontalDivider> {
        ScreenScaffold { modifier ->
            YGHorizontalDividerPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGTopBar> {
        ScreenScaffold { modifier ->
            YGTopBarPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun ScreenScaffold(content: @Composable (Modifier) -> Unit) {
    Scaffold { innerPadding ->
        content(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
