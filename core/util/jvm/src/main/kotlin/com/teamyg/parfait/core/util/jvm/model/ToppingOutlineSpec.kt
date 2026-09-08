package com.teamyg.parfait.core.util.jvm.model

/** 거리판을 담고 읽는 규격. 판을 만드는 쪽과 읽는 쪽이 같은 값을 봐야 거리가 어긋나지 않는다 */
object ToppingOutlineSpec {
    /** 실루엣 안으로 볼 알파 문턱. 이보다 옅은 자리는 실루엣 바깥으로 친다 */
    const val ALPHA_THRESHOLD = 128

    /** 1 필드픽셀을 이 수만큼 쪼개 담는다 */
    const val DISTANCE_STEPS_PER_PX = 8

    /** 담을 수 있는 가장 먼 거리(필드픽셀). 판의 대각선보다 한참 크다 */
    const val MAX_STORED_DISTANCE_PX = Short.MAX_VALUE / DISTANCE_STEPS_PER_PX

    /** 가장자리 한 겹을 반 픽셀씩 물려 칠해 계단이 지지 않게 한다 */
    const val EDGE_FEATHER_PX = 0.5f

    const val ALPHA_MAX = 255
}
