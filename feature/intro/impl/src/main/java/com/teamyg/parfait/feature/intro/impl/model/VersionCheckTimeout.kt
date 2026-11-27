package com.teamyg.parfait.feature.intro.impl.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 넘기면 업데이트 없음으로 보고 통과한다(fail-open). */
internal val VERSION_CHECK_TIMEOUT: Duration = 3.seconds
