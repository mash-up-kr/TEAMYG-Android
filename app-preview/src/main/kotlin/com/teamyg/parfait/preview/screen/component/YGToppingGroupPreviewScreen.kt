package com.teamyg.parfait.preview.screen.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingTemplate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarBack
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val SAMPLE_TOPPING_URL = "https://picsum.photos/400"

@Composable
internal fun YGToppingGroupPreviewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        YGTopBarBack(onIconClick = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PreviewSection("배치 7변형 (Template 그래픽 고정)") {
                    YGToppingGroupType.entries.forEach { type ->
                        YGToppingGroup(
                            image = YGToppingImage.Template(YGToppingTemplate.TEMPLATE_01),
                            name = type.shortLabel(),
                            timestamp = "3분전",
                            chipType = YGGrouptagChipType.TYPE_1_2,
                            type = type,
                        )
                    }
                }
            }
            item {
                PreviewSection("Template 6종") {
                    YGToppingTemplate.entries.forEach { template ->
                        YGToppingGroup(
                            image = YGToppingImage.Template(template),
                            name = "잠탈감금",
                            timestamp = "3분전",
                            chipType = YGGrouptagChipType.TYPE_9_10,
                            type = YGToppingGroupType.TEMPLATE,
                        )
                    }
                }
            }
            item {
                PreviewSection("Remote 성공 / Remote 실패 / Error") {
                    YGToppingGroup(
                        image = YGToppingImage.Remote(SAMPLE_TOPPING_URL),
                        name = "정상 로딩",
                        timestamp = "3분전",
                        chipType = YGGrouptagChipType.TYPE_3_4,
                        type = YGToppingGroupType.TYPE_1_LEFT,
                    )
                    YGToppingGroup(
                        image = YGToppingImage.Remote("https://invalid.example/none.png"),
                        name = "로드 실패",
                        timestamp = "3분전",
                        chipType = YGGrouptagChipType.TYPE_7_8,
                        type = YGToppingGroupType.TYPE_1_RIGHT,
                    )
                    YGToppingGroup(
                        image = YGToppingImage.Error,
                        name = "조회 실패",
                        timestamp = "오래 전",
                        chipType = YGGrouptagChipType.TYPE_11_12,
                        type = YGToppingGroupType.TYPE_2_LEFT,
                    )
                }
            }
            item {
                PreviewSection("경계 케이스 (긴 이름 + 긴 시간 / 비정사각 이미지)") {
                    // 칩이 160dp 프레임을 넘어 한 줄로 나오는지 확인용.
                    YGToppingGroup(
                        image = YGToppingImage.Template(YGToppingTemplate.TEMPLATE_01),
                        name = "팀장은 진짜 연경이야",
                        timestamp = "23시간 전",
                        chipType = YGGrouptagChipType.TYPE_3_4,
                        type = YGToppingGroupType.TYPE_2_RIGHT,
                    )
                    // 가로로 긴 비정사각 원격 이미지가 잘리지 않고 96dp 안에 다 들어오는지 확인용.
                    YGToppingGroup(
                        image = YGToppingImage.Remote("https://picsum.photos/800/400"),
                        name = "비정사각",
                        timestamp = "3분전",
                        chipType = YGGrouptagChipType.TYPE_9_10,
                        type = YGToppingGroupType.TYPE_1_LEFT,
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewYGToppingGroupPreviewScreen() = PreviewBox {
    YGToppingGroupPreviewScreen(
        onBack = {},
    )
}

/**
 * 갤러리 "배치 7변형" 섹션 전용 표시 라벨. Grouptag-Chip의 80dp 말줄임에 걸리지 않도록
 * [YGToppingGroupType.name]을 짧게 줄인다. 컴포넌트 계약과 무관한 갤러리 표시 전용 매핑이라
 * `core:designsystem`이 아닌 이 화면 파일 안에 둔다.
 */
private fun YGToppingGroupType.shortLabel(): String = when (this) {
    YGToppingGroupType.TYPE_1_LEFT -> "1-L"
    YGToppingGroupType.TYPE_1_RIGHT -> "1-R"
    YGToppingGroupType.TYPE_2_LEFT -> "2-L"
    YGToppingGroupType.TYPE_2_RIGHT -> "2-R"
    YGToppingGroupType.TYPE_3_LEFT -> "3-L"
    YGToppingGroupType.TYPE_3_RIGHT -> "3-R"
    YGToppingGroupType.TEMPLATE -> "TPL"
}
