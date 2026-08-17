package com.teamyg.parfait.domain.model

data class SegmentationResult(
    /** 원본과 같은 캔버스 크기의 객체 이미지. 수동 편집 화면이 원본과 픽셀 단위로 맞춰 그리는 데 쓴다 */
    val subjectImagePath: String,
    /** 투명한 여백을 걷어내 객체 크기만 남긴 이미지. 미리보기·배치처럼 실제 보이는 크기가 필요할 때 쓴다 */
    val trimmedSubjectImagePath: String,
    val subjectBounds: SegmentationBounds?,
)
