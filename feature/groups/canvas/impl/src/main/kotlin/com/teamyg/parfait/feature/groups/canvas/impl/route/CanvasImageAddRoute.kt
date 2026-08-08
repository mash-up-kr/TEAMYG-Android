package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasImageAddScreen
import com.teamyg.parfait.feature.groups.canvas.impl.screen.GroupMemberChip
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageAddIntent
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
internal fun CanvasImageAddRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasImageAddViewModel = hiltViewModel(),
) {
    ResultEffect<String> { imageUri ->
        viewModel.processIntent(CanvasImageAddIntent.CacheImage(imageUri))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CanvasImageAddEffect.NavigateToCamera -> navigator.goTo(
                    destination = NavKeyCameraCustom,
                )

                is CanvasImageAddEffect.NavigateToCanvas -> navigator.goTo(
                    destination = NavKeyCustomGalleryPicker,
                )

                is CanvasImageAddEffect.NavigateToSegmentation -> navigator.goTo(
                    destination = NavKeySegmentation(
                        sourceImageUri = effect.uri,
                    ),
                )
            }
        }
    }

    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    CanvasImageAddScreen(
        groupName = "그룹이름은최대열글자", // TODO: 그룹 정보 연동 필요
        memberChips = listOf(
            GroupMemberChip("문", YGColorChipType.NametagChip1),
            GroupMemberChip("전", YGColorChipType.NametagChip8),
            GroupMemberChip("김", YGColorChipType.NametagChip5),
            GroupMemberChip("이", YGColorChipType.NametagChip3),
            GroupMemberChip("박", YGColorChipType.NametagChip11),
            GroupMemberChip("최", YGColorChipType.NametagChip6),
            GroupMemberChip("정", YGColorChipType.NametagChip2),
        ), // TODO: 그룹원 Nametag-Chip 정보 연동 필요
        canvasDate = today.format(DateTextFormat.monthDayFormat),
        canvasDay = today.format(DateTextFormat.weekdayFormat),
        onClickBack = { navigator.onBack() },
        onClickDateSelect = {}, // TODO: 날짜 선택 연동 필요
        onClickMenu = {}, // TODO: 메뉴 연동 필요
        onClickCamera = { viewModel.processIntent(CanvasImageAddIntent.OnClickCamera()) },
        onClickGallery = { viewModel.processIntent(CanvasImageAddIntent.OnClickCanvas()) },
        onClickEditCanvasBG = {}, // TODO: 캔버스 편집 화면 연동 필요
        modifier = modifier,
    )
}
