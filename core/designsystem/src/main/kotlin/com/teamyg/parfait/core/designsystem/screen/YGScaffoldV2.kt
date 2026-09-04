package com.teamyg.parfait.core.designsystem.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingArt
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingOverlay
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

/**
 * [YGScaffold] 에 공통 로딩 오버레이와 공통 에러 토스트 자리를 더한 신판.
 *
 * 상태를 여기서 만들지 않는다 — 로딩 여부도 토스트 큐도 호출부가 넘긴다. `toastPolicy` 를
 * 넘기지 않으면 [rememberYGToastPolicy] 가 기본값으로 상태를 하나 만들어 쥐지만, 그 상태의
 * 주인은 여전히 호출부다. 스캐폴드는 **어디에 무엇을 겹칠지**만 안다. 화면이 실패를 어떤
 * 문구로 말할지는 화면의 어휘라 여기서 정하지 않는다.
 *
 * 세 층을 이 순서로 겹친다.
 * 1. [content] — [Scaffold] 가 계산한 인셋 패딩을 그대로 받는다
 * 2. 로딩 오버레이 — 인셋을 받지 않는다. Dim 이 시스템바 밑에서 끊기면 어설프다
 * 3. 토스트 호스트 — 상태바 인셋만 받는다. Toast 정책이 위에서 아래로 내려오는 노출이다
 *
 * 토스트가 로딩보다 위인 이유: 로딩 중에 일어난 실패도 보여야 한다.
 *
 * 이 컴포저블은 반드시 `YGCustomTheme` 하위에서 써야 한다 — 토스트 호스트가 내부적으로
 * `YGTheme.layout` 을 읽는데, 테마 밖에서는 실제로 토스트를 띄우는 순간에야
 * `IllegalStateException("Not Init Layout")` 이 터진다. 컴포지션 시점엔 조용해서 놓치기 쉽다.
 *
 * @param isLoading `true` 면 [content] 위에 [YGLoadingOverlay] 를 덮어 터치를 삼키고,
 *   [content] 서브트리를 접근성 트리에서 숨긴다(터치만 막고 TalkBack 은 통과시키는
 *   비대칭을 막기 위해서다)
 * @param toastPolicy 토스트 큐. 화면이 실패를 알리려면 이 정책을 직접 만들어 넘기고
 *   `showError` 로 띄운다. 넘기지 않으면 스캐폴드가 자기 것을 만들어 쓴다
 */
@Composable
fun YGScaffoldV2(
    modifier: Modifier = Modifier,
    containerColor: Color = YGAtomicColors.Gray.White,
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    isLoading: Boolean = false,
    loadingArt: YGLoadingArt = YGLoadingArt.Light,
    toastPolicy: YGToastPolicy = rememberYGToastPolicy(),
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = if (isLoading) {
                    Modifier.fillMaxSize().semantics { hideFromAccessibility() }
                } else {
                    Modifier.fillMaxSize()
                },
            ) {
                content(innerPadding)
            }

            if (isLoading) {
                YGLoadingOverlay(art = loadingArt)
            }

            YGToastHost(
                policy = toastPolicy,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fillMaxWidth(),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGScaffoldV2LoadingPreview() = PreviewBox {
    YGScaffoldV2(isLoading = true) { innerPadding ->
        Text(
            text = "컨텐츠",
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Gray900,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
