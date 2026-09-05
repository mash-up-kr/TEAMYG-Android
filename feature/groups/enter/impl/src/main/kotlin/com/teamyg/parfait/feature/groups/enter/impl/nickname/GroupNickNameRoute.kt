package com.teamyg.parfait.feature.groups.enter.impl.nickname

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.navigationBarsAndImePadding
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.enter.impl.component.NotificationPermissionGate
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList

/**
 * [GroupNickNameSideEffect.NavigateToNext] 는 Parcelable/Serializable 이 아니라 `rememberSaveable`
 * 이 기본으로 못 담는다 — 필드가 전부 기본 타입이라 리스트로 풀어 담는다.
 *
 * 왜 저장해야 하는가: 알림 권한 안내가 끝나기 전까지 이 값을 들고 대기하는데, 그 사이
 * 다크모드·폰트 크기 변경 같은 구성 변경이 Activity 를 다시 세우면 `remember` 는 유실된다.
 * 이펙트는 `Channel` 이라 다시 오지 않으므로, 그룹은 이미 서버에서 참여 처리됐는데 사용자는
 * 이 화면에 갇힌다.
 */
private val NavigateToNextSaver = listSaver<GroupNickNameSideEffect.NavigateToNext?, Any>(
    save = { value -> if (value == null) emptyList() else listOf(value.groupId, value.groupName) },
    restore = { saved ->
        if (saved.isEmpty()) {
            null
        } else {
            GroupNickNameSideEffect.NavigateToNext(
                groupId = saved[0] as Long,
                groupName = saved[1] as String,
            )
        }
    },
)

@Composable
fun GroupNickNameRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: GroupNickNameViewModel,
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()

    // 이펙트는 코루틴에서 수집돼 그 안에서 `stringResource` 를 부를 수 없다 — 문구를 미리 뽑아 둔다
    val errorMessages = GroupNickNameError.entries.associateWith { it.toStringResource() }

    // 그룹 참여 직후 알림 권한 안내를 한 번 거쳐야 캔버스로 넘어간다 —
    // 안내가 끝나기 전까지는 목적지 정보만 들고 대기한다. rememberSaveable 인 이유는
    // NavigateToNextSaver 의 KDoc 참고
    var pendingNavigation by rememberSaveable(stateSaver = NavigateToNextSaver) {
        mutableStateOf<GroupNickNameSideEffect.NavigateToNext?>(null)
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                GroupNickNameSideEffect.NavigateToBack -> {
                    navigator.onBack()
                }

                is GroupNickNameSideEffect.NavigateToNext -> {
                    pendingNavigation = effect
                }

                is GroupNickNameSideEffect.ShowError -> {
                    toastPolicy.showError(errorMessages.getValue(effect.error))
                }
            }
        }
    }

    pendingNavigation?.let { effect ->
        NotificationPermissionGate(
            onFinished = {
                pendingNavigation = null
                // 여기까지 쌓인 화면(초대코드, 닉네임)은 전부 이번 참여 흐름의 것이라
                // 되돌아갈 곳이 없다 — 백스택을 그룹 목록까지 비운 뒤 참여한 그룹의 캔버스를
                // 쌓는다. 캔버스만 남기면 백버튼이 앱 종료로 새 나간다
                navigator.replaceAll(destination = NavKeyGroupList)
                navigator.goTo(
                    destination = NavKeyCanvasMain(
                        groupId = effect.groupId,
                        welcomeGroupName = effect.groupName,
                    ),
                )
            },
        )
    }

    YGScaffoldV2(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        isLoading = uiState.isEntering,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        GroupNickNameScreen(
            uiState = uiState,
            onValueChanged = { word -> viewModel.processIntent(GroupNickNameIntent.InputWord(word)) },
            onClickNextButton = { viewModel.processIntent(GroupNickNameIntent.ClickNextButton) },
            onClickBackButton = { viewModel.processIntent(GroupNickNameIntent.ClickBackButton) },
            onClickConfirmPopupEnter = { viewModel.processIntent(GroupNickNameIntent.ClickConfirmPopupEnter) },
            onDismissConfirmPopup = { viewModel.processIntent(GroupNickNameIntent.DismissConfirmPopup) },
            modifier = Modifier
                .fillMaxSize()
                .background(YGAtomicColors.Gray.White)
                .padding(innerPadding)
                .statusBarsPadding()
                .navigationBarsAndImePadding(),
        )
    }
}
