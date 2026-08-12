package com.teamyg.parfait.domain.model.id

/**
 * 캔버스 위 배치 행의 식별자. 이미지 자체를 가리키는 ImageId 와 다른 키다 —
 * 서버 경로에도 imageId 와 parfaitImageId 가 따로 있다.
 */
@JvmInline
value class ParfaitImageId(val value: Long)
