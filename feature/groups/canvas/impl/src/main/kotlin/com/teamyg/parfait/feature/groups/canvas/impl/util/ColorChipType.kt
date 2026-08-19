package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.domain.model.group.NametagChipType

/**
 * 서버가 배정한 칩을 화면 색으로 옮긴다.
 *
 * 값이 없거나 반납된 자리는 [YGColorChipType.Default] 다 — 색이 "그룹 안의 이 사람"을 가리키는
 * 신호라, 가리킬 사람이 없을 때 아무 색이나 돌리면 신호가 거짓이 된다.
 *
 * **같은 규칙의 변환이 저장소에 셋이다** — S-101 그룹 설정(12종 1:1, 이 파일과 글자까지 같다)과
 * G-001 목록(12종을 6종으로 짝지어 접는다). 공용화하지 않은 이유는 자리가 없어서가 아니라
 * (`core:ui` 가 `:domain` 을 이미 보고 `:core:designsystem` 을 더하면 된다) 그 모듈의
 * `implementation`/`api` 가시성이 팀 결정 대상으로 열려 있어서다.
 *
 * ⚠️ **컴파일러가 잡아 주는 것은 앱이 [NametagChipType] 에 상수를 더할 때의 arm 누락뿐이다.**
 * 서버에 새 타입이 생기면 매퍼가 모르는 문자열을 `null` 로 접으므로 컴파일은 안 깨지고,
 * 세 변환 중 하나에서 색만 바꾸는 것도 못 잡는다. 색을 고칠 때는 셋을 함께 본다.
 */
internal fun NametagChipType?.toColorChipType(): YGColorChipType = when (this) {
    NametagChipType.TYPE1 -> YGColorChipType.NametagChip1
    NametagChipType.TYPE2 -> YGColorChipType.NametagChip2
    NametagChipType.TYPE3 -> YGColorChipType.NametagChip3
    NametagChipType.TYPE4 -> YGColorChipType.NametagChip4
    NametagChipType.TYPE5 -> YGColorChipType.NametagChip5
    NametagChipType.TYPE6 -> YGColorChipType.NametagChip6
    NametagChipType.TYPE7 -> YGColorChipType.NametagChip7
    NametagChipType.TYPE8 -> YGColorChipType.NametagChip8
    NametagChipType.TYPE9 -> YGColorChipType.NametagChip9
    NametagChipType.TYPE10 -> YGColorChipType.NametagChip10
    NametagChipType.TYPE11 -> YGColorChipType.NametagChip11
    NametagChipType.TYPE12 -> YGColorChipType.NametagChip12
    NametagChipType.DEFAULT, null -> YGColorChipType.Default
}
