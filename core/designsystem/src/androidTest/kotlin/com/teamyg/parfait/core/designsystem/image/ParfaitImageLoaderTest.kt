package com.teamyg.parfait.core.designsystem.image

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import coil3.request.ImageRequest
import coil3.request.crossfadeMillis
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ParfaitImageLoaderTest {
    @Test
    fun parfaitImageLoader_fadesLoadedImageIn() {
        // Given 앱이 쓰는 로더의 기본값을 그대로 물려받은 요청
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val request = ImageRequest
            .Builder(context)
            .defaults(newParfaitImageLoader(context).defaults)
            .build()

        // Then 이미지는 튀어나오지 않고 페이드로 들어온다
        assertTrue(request.crossfadeMillis > 0)
    }
}
