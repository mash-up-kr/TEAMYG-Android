package com.teamyg.parfait.core.designsystem.utils

import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ColorImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import kotlinx.coroutines.awaitCancellation

private const val FAKE_IMAGE_SIDE_PX = 120

/**
 * 요청이 끝나지 않는 로더. 로딩 상태를 붙잡아 두고 그 동안의 화면을 검사한다.
 */
fun neverFinishingImageLoader(): ImageLoader {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return ImageLoader
        .Builder(context)
        .components { add(Interceptor { awaitCancellation() }) }
        .build()
}

/**
 * 네트워크를 타지 않고 곧바로 성공하는 로더. 성공 이후의 화면을 검사한다.
 *
 * `DataSource.NETWORK` 로 돌려주는 이유: 메모리 캐시 출처면 Coil 이 크로스페이드를
 * 건너뛰어, 페이드를 켠 뒤에도 이 테스트가 그 경로를 지나지 않게 된다.
 */
fun instantlySucceedingImageLoader(): ImageLoader {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return ImageLoader
        .Builder(context)
        .components {
            add(
                Interceptor { chain ->
                    SuccessResult(
                        image = ColorImage(
                            color = Color.RED,
                            width = FAKE_IMAGE_SIDE_PX,
                            height = FAKE_IMAGE_SIDE_PX,
                        ),
                        request = chain.request,
                        dataSource = DataSource.NETWORK,
                    )
                },
            )
        }.build()
}
