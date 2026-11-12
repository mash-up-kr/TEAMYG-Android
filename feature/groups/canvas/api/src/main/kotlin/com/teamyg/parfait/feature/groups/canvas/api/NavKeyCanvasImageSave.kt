package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 캔버스 메인에서 저장 타입을 고른 뒤 무엇이 저장될지 먼저 보여 주는 미리보기 화면.
 *
 * 한 화면이 [CanvasSaveType] 로 사진·영상 미리보기를 겸한다
 *
 * 캡처한 비트맵을 키에 실을 수 없어(NavKey 는 직렬화돼 오간다) 캔버스 메인이 캐시에 PNG 로
 * 굽고 그 자리만 넘긴다.
 *
 * @param saveType 사진·영상 중 어느 미리보기인지. 캔버스 메인의 저장 메뉴가 정한다
 * @param imagePath 캡처한 캔버스 PNG 의 경로. 미리보기가 닫히면 쓸모가 없어지는 캐시 파일이다
 * @param date 어느 날의 캔버스인지. `LocalDate.toString()`(ISO-8601) 형태 — 이 모듈은
 *  kotlinx-datetime 을 쓰지 않아 문자열로 나르고, 받는 쪽이 `LocalDate.parse` 로 되돌린다
 */
@Serializable
data class NavKeyCanvasImageSave(
    val saveType: CanvasSaveType,
    val imagePath: String,
    val date: String,
) : NavKey

/** 저장 미리보기가 사진·영상 중 무엇을 보여 주고 확정하는지. 영상은 토핑이 있어야 성립한다 */
enum class CanvasSaveType {
    IMAGE,
    VIDEO,
}

/**
 * 미리보기가 닫히며 캔버스 메인에 남기는 결과 — 저장할 것, 또는 알릴 실패.
 *
 * 공유 실패([ShareFailed])도 여기 싣는다. 공유는 성공을 알리지 않고 실패만 캔버스 메인 토스트로 알리므로
 * 결과 키를 따로 두지 않았다. 공유 결과가 더 늘면 이름을 미리보기 결과로 넓힌다.
 *
 * 갤러리에 넣는 일을 미리보기에서 하지 않는 이유: 결과 토스트가 뜨는 자리는 캔버스 메인이다. 미리보기가
 * 저장까지 하고 나면 알림만 남기고 사라지는 화면이 되어, 실패했을 때 알릴 곳이 없다.
 */
sealed interface CanvasSaveResult {
    /** @param imagePath 저장할 이미지의 경로. 넘겨받은 [NavKeyCanvasImageSave.imagePath] 를 그대로 돌려준다 */
    data class Image(val imagePath: String) : CanvasSaveResult

    /** @param videoPath 미리보기가 캐시에 만들어 둔 mp4 의 경로 */
    data class Video(val videoPath: String) : CanvasSaveResult

    /** 미리보기가 영상을 만들지 못했다. 저장할 파일이 없고 실패만 알리면 된다 */
    data object VideoFailed : CanvasSaveResult

    /** 인스타그램 스토리 공유를 띄우지 못했다 — 미설치·실행 실패·영상을 못 만듦. 실패만 알리면 된다 */
    data object ShareFailed : CanvasSaveResult
}

/** [CanvasSaveResult] 를 주고받는 결과 키. [NavKeyCanvasImageSave] 로 들어온 쪽이 이 키로 받는다 */
const val CANVAS_SAVE_RESULT_KEY = "canvas_save_result"
