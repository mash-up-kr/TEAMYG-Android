package com.teamyg.parfait.core.designsystem.utils

import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ColorImage
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import kotlinx.coroutines.CompletableDeferred
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
 * 곧바로 성공하는 로더. 메모리 캐시 출처로 돌려주면 크로스페이드가 생략돼
 * 테스트가 그 경로를 지나지 않으므로 `DataSource.NETWORK` 로 둔다.
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

/**
 * 곧바로 실패하는 로더. 깨진 이미지 한 장이 화면 전체를 붙잡지 않는지 검사한다.
 */
fun instantlyFailingImageLoader(): ImageLoader {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return ImageLoader
        .Builder(context)
        .components {
            add(
                Interceptor { chain ->
                    ErrorResult(
                        image = null,
                        request = chain.request,
                        throwable = IllegalStateException("테스트용 실패"),
                    )
                },
            )
        }.build()
}

/**
 * 성공 시점을 테스트가 정하는 로더. 로딩 화면을 확인한 뒤 [succeed] 로 풀어,
 * 로딩에서 성공으로 넘어가는 전이를 한 테스트 안에서 볼 수 있게 한다.
 */
class ControllableImageLoader {
    private val gate = CompletableDeferred<Unit>()

    val imageLoader: ImageLoader = run {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ImageLoader
            .Builder(context)
            .components {
                add(
                    Interceptor { chain ->
                        gate.await()
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

    fun succeed() {
        gate.complete(Unit)
    }
}
