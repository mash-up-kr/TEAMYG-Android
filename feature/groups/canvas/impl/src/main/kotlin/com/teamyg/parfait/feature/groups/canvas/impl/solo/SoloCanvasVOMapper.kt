package com.teamyg.parfait.feature.groups.canvas.impl.solo

import com.teamyg.parfait.domain.model.canvas.CanvasStatus
import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import kotlinx.datetime.LocalDate

/**
 * 그룹 캔버스 화면(`CanvasMainScreen`)을 그대로 쓰려고 혼자 공간 캔버스를 [CanvasVO] 모양에 싣는다.
 *
 * ⚠️ 여기 채우는 id·상태는 화면 표시용 고정값이다. 저장소·use case 로 되돌려 보내면 안 된다 —
 * 그 값으로 서버를 부를 길이 생기는 순간 가짜 id 가 서버로 나간다(스펙 「혼자 공간 캔버스」).
 */
internal fun SoloCanvasVO.toCanvasVO(
    today: LocalDate,
    title: String,
): CanvasVO = CanvasVO(
    parfaitId = SOLO_DISPLAY_PARFAIT_ID,
    groupName = GroupName(title),
    date = today,
    status = CanvasStatus.ACTIVE,
    lastClosedDate = null,
    members = emptyList(),
    background = background,
    toppings = toppings,
    texts = texts,
)

private val SOLO_DISPLAY_PARFAIT_ID = ParfaitId(0)
