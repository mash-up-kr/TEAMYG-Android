package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.core.content.FileProvider

/**
 * 캔버스 캐시(캡처 PNG·타임랩스 mp4)를 인스타그램에 content uri 로 넘기기 위한 FileProvider.
 *
 * 하위 클래스를 따로 두는 이유: androidx 기본 클래스를 앱의 다른 provider 와 함께 또 선언하면
 * 매니페스트 병합에서 충돌한다 — 클래스 이름이 달라야 한다.
 */
internal class CanvasShareFileProvider : FileProvider() {
    companion object {
        // AndroidManifest.xml 의 android:authorities 와 짝이다. 한쪽만 바꾸면 uri 생성이 실패한다
        private const val AUTHORITY_SUFFIX = ".canvas.fileprovider"

        fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX
    }
}
