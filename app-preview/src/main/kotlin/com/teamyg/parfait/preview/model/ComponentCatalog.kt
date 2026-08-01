package com.teamyg.parfait.preview.model

import androidx.navigation3.runtime.NavKey
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

enum class ComponentCategory(val label: String) {
    BUTTON("Button"),
    INPUT("Input"),
    TEXT("Text"),
    CONTAINER("Container"),
    BAR("Bar"),
}

data class ComponentEntry(
    val category: ComponentCategory,
    val label: String,
    val navKey: NavKey,
)

val componentCatalog: List<ComponentEntry> = listOf(
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGButton",
        navKey = NavKeyYGButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGChipButton",
        navKey = NavKeyYGChipButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGCircleButton",
        navKey = NavKeyYGCircleButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGEditActionButton",
        navKey = NavKeyYGEditActionButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGCameraShutter",
        navKey = NavKeyYGCameraShutter,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGEditButton",
        navKey = NavKeyYGEditButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGEditTabButton",
        navKey = NavKeyYGEditTabButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGIconButton",
        navKey = NavKeyYGIconButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGDateButton",
        navKey = NavKeyYGDateButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGInputNumber",
        navKey = NavKeyYGInputNumber,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGStrokeButton",
        navKey = NavKeyYGStrokeButton,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGMenuItem",
        navKey = NavKeyYGMenuItem,
    ),
    ComponentEntry(
        category = ComponentCategory.BUTTON,
        label = "YGCanvasDateSelectButton",
        navKey = NavKeyYGCanvasDateSelectButton,
    ),
    ComponentEntry(
        category = ComponentCategory.INPUT,
        label = "YGTextField",
        navKey = NavKeyYGTextField,
    ),
    ComponentEntry(
        category = ComponentCategory.INPUT,
        label = "YGTextFormField",
        navKey = NavKeyYGTextFormField,
    ),
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGLabel",
        navKey = NavKeyYGLabel,
    ),
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGDate",
        navKey = NavKeyYGDate,
    ),
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGActionItem",
        navKey = NavKeyYGActionItem,
    ),
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGToast",
        navKey = NavKeyYGToast,
    ),
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGAlert",
        navKey = NavKeyYGAlert,
    ),
    ComponentEntry(
        category = ComponentCategory.TEXT,
        label = "YGGrouptagChip",
        navKey = NavKeyYGGrouptagChip,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGModalPopup",
        navKey = NavKeyYGModalPopup,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGInviteCard",
        navKey = NavKeyYGInviteCard,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGDangerZone",
        navKey = NavKeyYGDangerZone,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGListItem",
        navKey = NavKeyYGListItem,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGHorizontalDivider",
        navKey = NavKeyYGHorizontalDivider,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGCanvasMenu",
        navKey = NavKeyYGCanvasMenu,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGCanvas",
        navKey = NavKeyYGCanvas,
    ),
    ComponentEntry(
        category = ComponentCategory.CONTAINER,
        label = "YGToppingGroup",
        navKey = NavKeyYGToppingGroup,
    ),
    ComponentEntry(
        category = ComponentCategory.BAR,
        label = "YGTopBar",
        navKey = NavKeyYGTopBar,
    ),
)
