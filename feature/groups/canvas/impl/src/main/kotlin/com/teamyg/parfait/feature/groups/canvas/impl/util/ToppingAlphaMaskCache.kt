package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/** 마스크 한 장의 긴 변. 올리면 판정이 외형에 가까워지고 디코딩·메모리가 는다. */
private const val MASK_LONG_SIDE = 256

/** 캔버스 하나에 올라가는 토핑 수를 넉넉히 덮는 상한. */
private const val MASK_CACHE_ENTRIES = 64

private const val LOAD_FACTOR = 0.75f

/**
 * 접근 순서 갱신이 곧 쓰기라 잠금 없이 건드리면 상태가 깨진다 — 디코딩은 백그라운드에서 끝나고
 * 읽기는 메인 스레드다. 모든 접근을 [maskCache] 자신에 대해 동기화한다.
 */
private val maskCache = object : LinkedHashMap<String, ToppingAlphaMask>(
    MASK_CACHE_ENTRIES,
    LOAD_FACTOR,
    true,
) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ToppingAlphaMask>): Boolean = size > MASK_CACHE_ENTRIES
}

/**
 * 같은 모델을 아직 뜨는 중이면 그 로드에 합류시킨다. 캐시 조회부터 쓰기까지가 잠금 밖이라,
 * 두 화면이 같은 토핑을 거의 동시에 요청하면 둘 다 캐시 미스로 갈라져 같은 이미지를 각자
 * 디코딩·주사한다.
 */
private val inFlightMasks = mutableMapOf<String, Deferred<ToppingAlphaMask?>>()

/**
 * 로드를 시작한 컴포지션이 먼저 사라져도 합류한 쪽이 같이 죽으면 안 되므로, 실제 로드는 호출자
 * 스코프가 아니라 여기서 돈다. 뜬 마스크는 캐시에 남고 컨텍스트를 붙잡지 않으니 아무도 안 기다리는
 * 로드가 끝까지 가더라도 다음 판정을 미리 데워 두는 셈이다.
 */
private val maskLoadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * [model] 은 그 화면이 **실제로 그리는** 대상이어야 한다. 배경 편집은 편집본 로컬 경로를 그리는데,
 * 그 파일은 투명 여백이 잘려 있어 원본과 비율이 다르다.
 */
suspend fun loadToppingAlphaMask(
    context: Context,
    model: String,
): ToppingAlphaMask? {
    synchronized(maskCache) { maskCache[model] }?.let { return it }

    // 로드가 호출자보다 오래 살 수 있어 화면 컨텍스트를 넘기면 그동안 붙잡힌다
    val appContext = context.applicationContext

    return synchronized(inFlightMasks) {
        inFlightMasks.getOrPut(model) {
            maskLoadScope.async { decodeToppingAlphaMask(appContext, model) }.also { started ->
                started.invokeOnCompletion {
                    synchronized(inFlightMasks) {
                        if (inFlightMasks[model] === started) inFlightMasks.remove(model)
                    }
                }
            }
        }
    }.await()
}

private suspend fun decodeToppingAlphaMask(
    context: Context,
    model: String,
): ToppingAlphaMask? {
    val request = ImageRequest
        .Builder(context)
        .data(model)
        .size(MASK_LONG_SIDE)
        .allowHardware(false)
        .build()

    val image = (context.imageLoader.execute(request) as? SuccessResult)?.image ?: return null
    val bitmap = image.toBitmap()

    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    val mask = ToppingAlphaMask.of(width = bitmap.width, height = bitmap.height) { x, y ->
        pixels[y * bitmap.width + x] ushr 24
    }

    synchronized(maskCache) { maskCache.put(model, mask) }
    return mask
}

/** 메모리 압박이나 테스트에서 캐시를 비우는 수단. */
fun clearToppingAlphaMasks() {
    synchronized(maskCache) { maskCache.clear() }
}

/** [models] 가 비면 아무것도 로드하지 않는다 — 판정을 쓰지 않는 화면이 로딩을 끄는 방법이다. */
@Composable
fun rememberToppingAlphaMasks(models: List<String>): Map<String, ToppingAlphaMask> {
    val context = LocalContext.current
    val loaded = remember { mutableStateMapOf<String, ToppingAlphaMask>() }

    LaunchedEffect(models) {
        models
            .distinct()
            .filterNot { loaded.containsKey(it) }
            .forEach { model -> loadToppingAlphaMask(context, model)?.let { loaded[model] = it } }
    }

    return loaded
}
