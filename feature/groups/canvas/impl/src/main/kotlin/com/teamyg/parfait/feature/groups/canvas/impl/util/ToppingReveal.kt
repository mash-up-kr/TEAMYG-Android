package com.teamyg.parfait.feature.groups.canvas.impl.util

/**
 * 토핑을 한꺼번에 드러낼 때가 됐는가. [settled] 는 토핑마다 이미지가 결말났는지다.
 *
 * **빈 목록은 완료가 아니다.** 캔버스 조회가 오기 전에는 그릴 토핑이 없어 목록이 비는데,
 * 그 순간을 완료로 세면 빗장이 먼저 풀려 뒤늦게 도착한 토핑들이 하나씩 따로 뜬다.
 */
internal fun allToppingsSettled(settled: List<Boolean>): Boolean = settled.isNotEmpty() && settled.all { it }
