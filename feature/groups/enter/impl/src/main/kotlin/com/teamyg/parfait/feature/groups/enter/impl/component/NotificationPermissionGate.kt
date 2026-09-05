package com.teamyg.parfait.feature.groups.enter.impl.component

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.buildAppSettingsIntent
import com.teamyg.parfait.core.util.android.permission.NotificationPermissionManager
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 정책: A-004(그룹 참여)·A-005(그룹 생성) 완료 직후, 캔버스 진입 전 보여준다.
 * 이미 허용돼 있으면 안내 없이 곧장 [onFinished] 로 넘어간다.
 *
 * **기기 토큰 등록을 여기에 매달지 마라** — 토큰은 알림 권한과 무관하게 발급되고 등록은
 * 세션 축이 맡는다([RegisterCurrentDeviceTokenUseCase][com.teamyg.parfait.domain.usecase.notification.RegisterCurrentDeviceTokenUseCase]).
 *
 * [onFinished] 는 허용·거부·"나중에" 세 갈래 모두에서 호출된다.
 */
@Composable
internal fun NotificationPermissionGate(onFinished: () -> Unit) {
    val activity: Activity? = LocalActivity.current
    val context: Context = activity ?: LocalContext.current
    val hasPermission = remember { NotificationPermissionManager.hasPermission(context) }

    // 아래 조기 return 밑으로 내리면 hasPermission 이 관찰 가능한 상태로 바뀌는 날
    // 런처 등록이 조용히 사라진다.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        // 눌러도 아무 일 없는 버튼으로 끝나지 않도록 설정으로 보낸다.
        val isPermanentlyDenied = activity?.let(NotificationPermissionManager::isPermanentlyDenied) == true

        if (!isGranted && isPermanentlyDenied) {
            context.startActivity(context.buildAppSettingsIntent())
        }

        onFinished()
    }

    if (hasPermission) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    NotificationPermissionModal(
        onGrantClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onLaterClick = onFinished,
        onDismissRequest = onFinished,
    )
}

@Composable
private fun NotificationPermissionModal(
    onGrantClick: () -> Unit,
    onLaterClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    YGModalPopup(
        title = stringResource(R.string.notification_permission_rationale_title),
        body = stringResource(R.string.notification_permission_rationale_body),
        iconRes = DesignSystemR.drawable.ic_info_round,
        secondaryText = stringResource(R.string.notification_permission_rationale_later),
        onSecondaryClick = onLaterClick,
        primaryText = stringResource(R.string.notification_permission_rationale_grant),
        onPrimaryClick = onGrantClick,
        onDismissRequest = onDismissRequest,
    )
}

@YGPreview
@Composable
private fun NotificationPermissionModalPreview() = PreviewBox {
    NotificationPermissionModal(
        onGrantClick = {},
        onLaterClick = {},
        onDismissRequest = {},
    )
}
