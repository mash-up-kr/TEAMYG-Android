package com.teamyg.parfait.core.ui.outline

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.teamyg.parfait.core.util.android.outline.toToppingOutline
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/** 거리판 한 장의 긴 변. 올리면 테두리가 실루엣에 가까워지고 디코딩·메모리가 는다 */
private const val OUTLINE_LONG_SIDE = 256

/** 캔버스 하나에 올라가는 토핑 수를 넉넉히 덮는 상한 */
private const val OUTLINE_CACHE_ENTRIES = 64

private const val LOAD_FACTOR = 0.75f

/**
 * 접근 순서 갱신이 곧 쓰기라, 여럿이 잠금 없이 건드리면 상태가 깨진다. 로딩을 어느 컨텍스트에서
 * 부르는지 이 파일이 정하지 않으므로 모든 접근을 [outlineCache] 자신에 대해 동기화한다.
 */
private val outlineCache = object : LinkedHashMap<String, ToppingOutline>(
    OUTLINE_CACHE_ENTRIES,
    LOAD_FACTOR,
    true,
) {
    override fun removeEldestEntry(eldest: Map.Entry<String, ToppingOutline>): Boolean = size > OUTLINE_CACHE_ENTRIES
}

/**
 * 같은 모델을 아직 뜨는 중이면 그 로드에 합류시킨다. 캐시 조회부터 쓰기까지가 잠금 밖이라,
 * 두 화면이 같은 토핑을 거의 동시에 요청하면 둘 다 캐시 미스로 갈라져 같은 이미지를 각자 디코딩한다.
 */
private val inFlightOutlines = mutableMapOf<String, Deferred<ToppingOutline?>>()

/**
 * 로드를 시작한 컴포지션이 먼저 사라져도 합류한 쪽이 같이 죽으면 안 되므로, 실제 로드는 호출자
 * 스코프가 아니라 여기서 돈다.
 */
private val outlineLoadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * 이미 받아 둔 거리판을 기다리지 않고 꺼낸다. [loadToppingOutline] 은 `suspend` 라 캐시가 적중해도
 * 값이 첫 컴포지션 뒤에나 오고, 그 사이 테두리가 빠진 채로 한 프레임이 그려진다.
 */
fun peekToppingOutline(
    model: String,
    retryKey: Int,
): ToppingOutline? = synchronized(outlineCache) { outlineCache["$retryKey|$model"] }

/**
 * [model] 은 그 화면이 **실제로 그리는** 대상이어야 한다. 편집본을 그리는 화면에 원본 주소를
 * 넘기면 투명 여백이 잘려 비율이 달라 실루엣이 통째로 어긋난다.
 *
 * @param retryKey 올리면 이 모델의 캐시를 건너뛰고 다시 받는다 — 안 그러면 재시도로 그림이 돌아와도
 *   테두리와 판정이 옛 실패에 묶인다
 */
suspend fun loadToppingOutline(
    context: Context,
    model: String,
    retryKey: Int,
): ToppingOutline? {
    val key = "$retryKey|$model"
    synchronized(outlineCache) { outlineCache[key] }?.let { return it }

    // 로드가 호출자보다 오래 살 수 있어 화면 컨텍스트를 넘기면 그동안 붙잡힌다
    val appContext = context.applicationContext

    return synchronized(inFlightOutlines) {
        inFlightOutlines.getOrPut(key) {
            outlineLoadScope.async { decodeToppingOutline(appContext, model, key) }.also { started ->
                started.invokeOnCompletion {
                    synchronized(inFlightOutlines) {
                        if (inFlightOutlines[key] === started) inFlightOutlines.remove(key)
                    }
                }
            }
        }
    }.await()
}

private suspend fun decodeToppingOutline(
    context: Context,
    model: String,
    key: String,
): ToppingOutline? {
    val request = ImageRequest
        .Builder(context)
        .data(model)
        .size(OUTLINE_LONG_SIDE)
        .allowHardware(false)
        // 거리판으로 접고 나면 버릴 비트맵이다. 표시용과 크기가 달라 키도 다르니, 얹어 두면
        // 토핑 수만큼 쓸모없는 항목이 표시용 비트맵을 밀어낸다
        .memoryCachePolicy(CachePolicy.DISABLED)
        .build()

    val image = (context.imageLoader.execute(request) as? SuccessResult)?.image ?: return null

    // execute 는 자기 디스패처에서 돌지만 그 뒤는 부르는 쪽 컨텍스트다. 전 픽셀 순회를
    // 그대로 두면 호출부가 메인 스레드일 때 토핑 수만큼 메인이 잡힌다
    val outline = withContext(Dispatchers.Default) {
        image.toBitmap().toToppingOutline(fieldLongSide = OUTLINE_LONG_SIDE)
    }

    synchronized(outlineCache) { outlineCache.put(key, outline) }
    return outline
}

/**
 * 메모리 압박이나 테스트에서 캐시를 비우는 수단. 아직 부르는 곳을 두지 않았다 — 항목 수에
 * 상한이 있어 누수가 아니라서 호출부 신설을 미뤘다.
 */
fun clearToppingOutlines() {
    synchronized(outlineCache) { outlineCache.clear() }
}

/** [models] 가 비면 아무것도 로드하지 않는다 */
@Composable
fun rememberToppingOutlines(
    models: List<String>,
    retryKey: Int,
): Map<String, ToppingOutline> {
    val context = LocalContext.current

    // 캐시에 있는 것부터 채우고 들어간다 — 비운 채로 시작하면 화면을 다시 그릴 때마다 테두리가
    // 한 프레임 늦게 붙어 깜빡인다.
    // 키를 안 주는 것은 의도다. [retryKey] 가 오르면 새 열쇠로는 캐시가 반드시 미스라, 맵을 다시
    // 만들면 재시도하는 동안 이미 받아 둔 거리판까지 사라진다
    val loaded = remember {
        mutableStateMapOf<String, ToppingOutline>().apply {
            models.distinct().forEach { model ->
                peekToppingOutline(model, retryKey)?.let { outline -> put(model, outline) }
            }
        }
    }

    LaunchedEffect(models, retryKey) {
        models
            .distinct()
            .forEach { model ->
                loadToppingOutline(context, model, retryKey)?.let { loaded[model] = it }
            }
    }

    return loaded
}
