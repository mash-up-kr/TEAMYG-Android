package com.teamyg.parfait.preview.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

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
    }
}

@YGPreview
@Composable
private fun PreviewMainScreen() = PreviewBox {
    MainScreen(
        toast = rememberYGToastPolicy(),
        modifier = Modifier.fillMaxSize(),
    )
}
