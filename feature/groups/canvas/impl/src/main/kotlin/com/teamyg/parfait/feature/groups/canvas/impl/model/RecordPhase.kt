package com.teamyg.parfait.feature.groups.canvas.impl.model

/** 저장 미리보기가 mp4 를 미리 굽는 진행 단계. 실패로 끝나도 [FINISHED] 다 */
internal enum class RecordPhase {
    IDLE,
    RECORDING,
    FINISHED,
}
