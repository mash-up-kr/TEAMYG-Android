package com.teamyg.parfait.feature.groups.enter.impl.component

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.feature.groups.enter.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 정책: A-004(그룹 참여)·A-005(그룹 생성) 완료 직후, 캔버스 진입 전 1회 보여준다.
 * 이미 허용돼 있으면 안내 없이 곧장 [onFinished] 로 넘어간다.
 *
 * [onFinished] 는 허용·거부·"나중에" 세 갈래 모두에서 호출된다.
 */
@Composable
internal fun NotificationPermissionGate(
    onFinished: () -> Unit,
    viewModel: NotificationPermissionViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val alreadyGranted = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    if (alreadyGranted) {
        LaunchedEffect(Unit) { onFinished() }
        return
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            viewModel.onNotificationPermissionGranted()
        }
        onFinished()
    }

    YGModalPopup(
        title = stringResource(R.string.notification_permission_rationale_title),
        body = stringResource(R.string.notification_permission_rationale_body),
        iconRes = DesignSystemR.drawable.ic_info_round,
        secondaryText = stringResource(R.string.notification_permission_rationale_later),
        onSecondaryClick = onFinished,
        primaryText = stringResource(R.string.notification_permission_rationale_grant),
        onPrimaryClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        onDismissRequest = onFinished,
    )
}
