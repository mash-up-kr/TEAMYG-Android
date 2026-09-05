package com.teamyg.parfait.feature.gallery.impl.route

import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtutorial.YGTutorialBoxPlacement
import com.teamyg.parfait.core.designsystem.component.ygtutorial.YGTutorialOverlay
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.buildAppSettingsIntent
import com.teamyg.parfait.core.util.android.permission.GalleryPermissionManager
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.feature.gallery.impl.R
import com.teamyg.parfait.feature.gallery.impl.screen.CustomGalleryPickerScreen
import com.teamyg.parfait.feature.gallery.impl.viewmodel.CustomGalleryPickerEffect
import com.teamyg.parfait.feature.gallery.impl.viewmodel.CustomGalleryPickerIntent
import com.teamyg.parfait.feature.gallery.impl.viewmodel.CustomGalleryPickerViewModel
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.core.ui.R as CoreUiR

@Composable
internal fun CustomGalleryPickerRoute(
    navigator: Navigator,
    recentImagePick: RecentImagePick,
    modifier: Modifier = Modifier,
    showGuideToast: Boolean = true,
    returnResultOnly: Boolean = false,
) {
    val activity: Activity? = LocalActivity.current
    val context: Context = activity ?: LocalContext.current

    val viewModel = hiltViewModel<CustomGalleryPickerViewModel, CustomGalleryPickerViewModel.Factory>(
        creationCallback = { factory -> factory.create(returnResultOnly, recentImagePick) },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val access = GalleryPermissionManager.resolveAccessLevelAfterRequest(context, activity)
        viewModel.processIntent(CustomGalleryPickerIntent.OnPermissionResult(access))
    }

    val toastPolicy = rememberYGToastPolicy()
    val guideToastMessage = stringResource(R.string.gallery_custom_guide_toast)
    var hasShownGuideToast by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.isLoading, state.isEmpty, state.access) {
        if (showGuideToast && !hasShownGuideToast && state.access.hasPermission && !state.isLoading && !state.isEmpty) {
            toastPolicy.show(YGToastType.Edit(guideToastMessage))
            hasShownGuideToast = true
        }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CustomGalleryPickerEffect.RequestPermission -> {
                    permissionLauncher.launch(GalleryPermissionManager.requiredPermissions)
                }

                is CustomGalleryPickerEffect.OpenAppSettings -> {
                    context.startActivity(context.buildAppSettingsIntent())
                }

                is CustomGalleryPickerEffect.NavigateToConfirm -> {
                    navigator.goTo(
                        NavKeyPictureConfirm(
                            uri = effect.uri,
                            source = PictureConfirmSource.GALLERY,
                            returnResultOnly = returnResultOnly,
                        ),
                    )
                }

                is CustomGalleryPickerEffect.NavigateToSegmentationConfirm -> {
                    navigator.goTo(
                        NavKeySegmentationConfirm(
                            sourceImageUri = null,
                            subjectImagePath = null,
                            trimmedSubjectImagePath = effect.trimmedSubjectImagePath,
                        ),
                    )
                }

                is CustomGalleryPickerEffect.NavigateToBack -> navigator.onBack()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.processIntent(
                    CustomGalleryPickerIntent.OnPermissionResult(
                        access = GalleryPermissionManager.resolveAccessLevelOnEnter(context),
                    ),
                )
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 튜토리얼은 스캐폴드 **밖**에 겹친다 — 안에 넣으면 컨텐츠 인셋을 받아 딤이 상태바
    // 밑에서 끊기고, 시스템바만 안 덮인 화면이 된다
    Box(modifier = Modifier.fillMaxSize()) {
        // 프레임 Box 안에 YGToastHost 를 직접 얹어서, 위치 계산 없이 항상 프레임 윗변에 뜨게 한다
        YGScaffoldV2 { innerPadding ->
            CustomGalleryPickerScreen(
                state = state,
                toastPolicy = toastPolicy,
                onClickGrantPermission = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestPermission) },
                onClickOpenSettings = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestOpenSettings) },
                onClickManageMedia = { viewModel.processIntent(CustomGalleryPickerIntent.OnRequestManageMedia) },
                onClickImage = { uri ->
                    viewModel.processIntent(CustomGalleryPickerIntent.OnClickImage(uri = uri))
                },
                onClickCutoutImage = { recentImage ->
                    viewModel.processIntent(CustomGalleryPickerIntent.OnClickCutoutImage(recentImage))
                },
                onClickCancel = { viewModel.processIntent(CustomGalleryPickerIntent.OnCancel) },
                modifier = modifier.padding(innerPadding),
            )
        }

        if (state.isTutorialVisible) {
            // 강조 대상이 화면 위쪽(오늘 찍은 사진 목록)이라 카드는 아래에 붙인다
            YGTutorialOverlay(
                imageResource = R.drawable.img_upload_tutorial,
                buttonText = stringResource(CoreUiR.string.next),
                title = stringResource(R.string.gallery_upload_tutorial_title),
                description = stringResource(R.string.gallery_upload_tutorial_description),
                onClickButton = { viewModel.processIntent(CustomGalleryPickerIntent.OnConfirmTutorial) },
                placement = YGTutorialBoxPlacement.Bottom,
            )
        }
    }
}
