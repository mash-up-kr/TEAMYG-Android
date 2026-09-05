package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 갤러리에 무엇이 저장될지 먼저 보여 주는 미리보기 화면.
 *
 * 캡처한 비트맵을 키에 실을 수 없어(NavKey 는 직렬화돼 오간다) 캔버스 메인이 캐시에 PNG 로
 * 굽고 그 자리만 넘긴다.
 *
 * @param imagePath 캡처한 캔버스 PNG 의 경로. 미리보기가 닫히면 쓸모가 없어지는 캐시 파일이다
 * @param date 어느 날의 캔버스인지. `LocalDate.toString()`(ISO-8601) 형태 — 이 모듈은
 *  kotlinx-datetime 을 쓰지 않아 문자열로 나르고, 받는 쪽이 `LocalDate.parse` 로 되돌린다
 */
@Serializable
data class NavKeyCanvasImageSave(
    val imagePath: String,
    val date: String,
) : NavKey

/**
 * 미리보기에서 저장을 확정했다는 결과.
 *
 * 저장 자체를 여기서 하지 않는 이유: 결과 토스트가 뜨는 자리는 캔버스 메인이다. 미리보기가
 * 저장까지 하고 나면 알림만 남기고 사라지는 화면이 되어, 실패했을 때 알릴 곳이 없다.
 *
 * @param imagePath 저장할 이미지의 경로. 넘겨받은 [NavKeyCanvasImageSave.imagePath] 를 그대로 돌려준다
 */
data class CanvasImageSaveResult(val imagePath: String)

/** [CanvasImageSaveResult] 를 주고받는 결과 키. [NavKeyCanvasImageSave] 로 들어온 쪽이 이 키로 받는다 */
const val CANVAS_IMAGE_SAVE_RESULT_KEY = "canvas_image_save_result"
