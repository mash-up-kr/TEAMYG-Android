package com.teamyg.parfait.feature.groups.canvas.impl.model

import android.graphics.Bitmap

/** 갤러리 쓰기 권한 승인을 기다리는 저장 대상 */
internal sealed interface PendingGallerySave {
    data class Image(val bitmap: Bitmap) : PendingGallerySave

    /** @param filePath 미리보기가 캐시에 만든 mp4. 저장하지 않게 되면 `DiscardRecordedVideo` 로 지운다 */
    data class Video(val filePath: String) : PendingGallerySave
}
