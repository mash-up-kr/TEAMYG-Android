package com.teamyg.parfait.feature.intro.impl.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 버전 체크를 기다리는 상한. 넘기면 강제 업데이트가 아닌 것으로 보고 통과한다(fail-open). */
internal val VERSION_CHECK_TIMEOUT: Duration = 3.seconds
