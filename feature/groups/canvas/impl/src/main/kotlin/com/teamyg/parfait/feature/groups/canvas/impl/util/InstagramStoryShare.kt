package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.teamyg.parfait.core.ui.screenLogger
import com.teamyg.parfait.feature.groups.canvas.impl.BuildConfig
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasShareMedia
import java.io.File

private object InstagramStoryShareContract {
    const val PACKAGE = "com.instagram.android"
    const val ACTION_ADD_TO_STORY = "com.instagram.share.ADD_TO_STORY"

    /** 스토리 귀속([BuildConfig.FACEBOOK_APP_ID])을 싣는 extra 키 */
    const val EXTRA_SOURCE_APPLICATION = "source_application"
}

/**
 * 저장 전 캐시에 있는 미디어를 인스타그램 스토리 편집기의 **배경**으로 띄운다.
 *
 * 파일을 직접 넘기지 않고 [CanvasShareFileProvider] content uri 로 바꿔 읽기 권한만 한시로 준다.
 * 인스타그램이 깔려 있지 않거나 실행에 실패하면 `false` 로 접는다.
 */
internal fun shareCanvasToInstagramStory(
    context: Context,
    file: File,
    media: CanvasShareMedia,
): Boolean {
    val uri = runCatching {
        FileProvider.getUriForFile(context, CanvasShareFileProvider.authority(context), file)
    }.getOrElse { throwable ->
        screenLogger.e(throwable) { "shareCanvasToInstagramStory - content uri 생성 실패: ${file.path}" }
        return false
    }

    val intent = Intent(InstagramStoryShareContract.ACTION_ADD_TO_STORY).apply {
        setPackage(InstagramStoryShareContract.PACKAGE)
        setDataAndType(uri, media.mimeType)
        if (BuildConfig.FACEBOOK_APP_ID.isNotEmpty()) {
            putExtra(InstagramStoryShareContract.EXTRA_SOURCE_APPLICATION, BuildConfig.FACEBOOK_APP_ID)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // 인스타그램 미설치면 받을 액티비티가 없다. queries 로 패키지를 보이게 해 둬야 resolve 가 된다
    if (intent.resolveActivity(context.packageManager) == null) return false

    return runCatching { context.startActivity(intent) }
        .onFailure { throwable -> screenLogger.e(throwable) { "shareCanvasToInstagramStory - 인스타그램 실행 실패" } }
        .isSuccess
}
