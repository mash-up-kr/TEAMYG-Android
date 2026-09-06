package com.teamyg.parfait.data.utils.image

import java.io.File

/** `cacheDir` 하위의 세그멘테이션 전용 디렉토리 이름. 다른 캐시 소비자와 섞이지 않게 가른다 */
internal const val SEGMENTATION_CACHE_DIR_NAME = "segmentation"

/**
 * 디렉토리 안의 파일을 지운다. **디렉토리 자체는 남긴다.**
 *
 * 없는 디렉토리를 비우는 것은 오류가 아니다 — 앱 설치 후 첫 진입이 그 상태다.
 */
internal fun File.clearFiles() {
    listFiles()?.forEach { it.delete() }
}
