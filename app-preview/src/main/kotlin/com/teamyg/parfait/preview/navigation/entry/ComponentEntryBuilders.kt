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
import com.teamyg.parfait.preview.navigation.key.NavKeyYGAlert
import com.teamyg.parfait.preview.navigation.key.NavKeyYGButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGCameraShutter
import com.teamyg.parfait.preview.navigation.key.NavKeyYGCanvas
import com.teamyg.parfait.preview.navigation.key.NavKeyYGCanvasDateSelectButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGCanvasMenu
import com.teamyg.parfait.preview.navigation.key.NavKeyYGChipButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGCircleButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDangerZone
import com.teamyg.parfait.preview.navigation.key.NavKeyYGEditActionButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGEditButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGEditTabButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDate
import com.teamyg.parfait.preview.navigation.key.NavKeyYGDateButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGGrouptagChip
import com.teamyg.parfait.preview.navigation.key.NavKeyYGHorizontalDivider
import com.teamyg.parfait.preview.navigation.key.NavKeyYGIconButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInputNumber
import com.teamyg.parfait.preview.navigation.key.NavKeyYGInviteCard
import com.teamyg.parfait.preview.navigation.key.NavKeyYGLabel
import com.teamyg.parfait.preview.navigation.key.NavKeyYGListItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGMenuItem
import com.teamyg.parfait.preview.navigation.key.NavKeyYGModalPopup
import com.teamyg.parfait.preview.navigation.key.NavKeyYGStrokeButton
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTextFormField
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToast
import com.teamyg.parfait.preview.navigation.key.NavKeyYGTopBar
import com.teamyg.parfait.preview.navigation.key.NavKeyYGToppingGroup
import com.teamyg.parfait.preview.screen.component.YGActionItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGAlertPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGCameraShutterPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGCanvasDateSelectButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGCanvasMenuPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGCanvasPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGChipButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGCircleButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDangerZonePreviewScreen
import com.teamyg.parfait.preview.screen.component.YGEditActionButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGEditButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGEditTabButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDatePreviewScreen
import com.teamyg.parfait.preview.screen.component.YGDateButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGGrouptagChipPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGHorizontalDividerPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGIconButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGInputNumberPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGInviteCardPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGLabelPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGListItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGMenuItemPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGModalPopupPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGStrokeButtonPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTextFieldPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTextFormFieldPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGToastPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGTopBarPreviewScreen
import com.teamyg.parfait.preview.screen.component.YGToppingGroupPreviewScreen

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
    entry<NavKeyYGCircleButton> {
        ScreenScaffold { modifier ->
            YGCircleButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGEditActionButton> {
        ScreenScaffold { modifier ->
            YGEditActionButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCameraShutter> {
        ScreenScaffold { modifier ->
            YGCameraShutterPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGEditButton> {
        ScreenScaffold { modifier ->
            YGEditButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGEditTabButton> {
        ScreenScaffold { modifier ->
            YGEditTabButtonPreviewScreen(
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
    entry<NavKeyYGToast> {
        ScreenScaffold { modifier ->
            YGToastPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGAlert> {
        ScreenScaffold { modifier ->
            YGAlertPreviewScreen(
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
    entry<NavKeyYGStrokeButton> {
        ScreenScaffold { modifier ->
            YGStrokeButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGMenuItem> {
        ScreenScaffold { modifier ->
            YGMenuItemPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCanvasMenu> {
        ScreenScaffold { modifier ->
            YGCanvasMenuPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCanvasDateSelectButton> {
        ScreenScaffold { modifier ->
            YGCanvasDateSelectButtonPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGCanvas> {
        ScreenScaffold { modifier ->
            YGCanvasPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGGrouptagChip> {
        ScreenScaffold { modifier ->
            YGGrouptagChipPreviewScreen(
                onBack = navigator::onBack,
                modifier = modifier,
            )
        }
    }
    entry<NavKeyYGToppingGroup> {
        ScreenScaffold { modifier ->
            YGToppingGroupPreviewScreen(
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
