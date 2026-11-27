package com.teamyg.parfait.data.repository.solo

import com.teamyg.parfait.data.model.entity.SoloBackgroundEntity
import com.teamyg.parfait.data.model.entity.SoloSpaceRecordEntity
import com.teamyg.parfait.data.model.entity.SoloTextEntity
import com.teamyg.parfait.data.model.entity.SoloToppingEntity
import com.teamyg.parfait.data.model.mapper.entity.soloTextEntity
import com.teamyg.parfait.data.model.mapper.entity.soloToppingEntity
import com.teamyg.parfait.data.model.mapper.entity.toVO
import com.teamyg.parfait.data.model.mapper.entity.withBorder
import com.teamyg.parfait.data.source.solo.local.SoloSpaceLocalDataSource
import com.teamyg.parfait.data.utils.repositoryLogger
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.solo.SoloBackgroundEdit
import com.teamyg.parfait.domain.model.solo.SoloCanvasVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.repository.solo.SoloSpaceRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock

/**
 * 혼자 공간 저장소. 규칙(메모리 SSoT, 쓰기 순서, 읽기 실패 시 덮지 않기)은
 * `docs/superpowers/specs/2026-11-26-solo-space-design.md` 「데이터 › 구현」이 정본이다.
 *
 * 메모리 상태는 기록 저장이 성공한 뒤에만 바꾼다. 그래서 저장 실패의 "롤백"은 바꾸지 않은 상태를 그대로 두는 것이다.
 */
@Singleton
class SoloSpaceRepositoryImpl internal constructor(
    private val localDataSource: SoloSpaceLocalDataSource,
    private val clock: Clock,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SoloSpaceRepository {
    @Inject
    constructor(
        localDataSource: SoloSpaceLocalDataSource,
        clock: Clock,
    ) : this(localDataSource, clock, Dispatchers.IO)

    private val state = MutableStateFlow<SoloSpaceState>(SoloSpaceState.Unloaded)

    // 읽기도 이 잠금 안에서 한다 — 처음 읽기와 쓰기가 겹치면 읽은 옛 기록이 방금 저장한 기록을 덮는다
    private val mutex = Mutex()

    override val canvas: Flow<Result<SoloCanvasVO>> = flow {
        withContext(ioDispatcher) { mutex.withLock { loadIfNeededLocked() } }
        emitAll(state.mapNotNull { current -> current.toCanvasResult() })
    }

    override suspend fun changeBackground(edit: SoloBackgroundEdit): Result<Unit> = when (edit) {
        is SoloBackgroundEdit.Color -> edit { record ->
            record.copy(background = SoloBackgroundEntity.Color(edit.hex))
        }

        is SoloBackgroundEdit.Image -> {
            val fileName = newFileName(BACKGROUND_EXTENSION)
            edit(newImage = NewImage(sourcePath = edit.filePath, fileName = fileName)) { record ->
                record.copy(background = SoloBackgroundEntity.Image(fileName))
            }
        }
    }

    override suspend fun placeTopping(
        filePath: String,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<Unit> {
        val fileName = newFileName(TOPPING_EXTENSION)
        return edit(newImage = NewImage(sourcePath = filePath, fileName = fileName)) { record ->
            val topping = soloToppingEntity(
                id = record.nextId,
                fileName = fileName,
                transform = transform,
                border = border,
                createdAt = clock.now(),
            )
            record.copy(nextId = record.nextId + 1, toppings = record.toppings + topping)
        }
    }

    override suspend fun updateTransforms(
        updates: List<ToppingTransformUpdate>,
        textUpdates: List<TextToppingTransformUpdate>,
    ): Result<Unit> = edit { record ->
        val toppingUpdates = updates.associateBy { update -> update.parfaitImageId.value }
        val textUpdatesById = textUpdates.associateBy { update -> update.parfaitTextId.value }
        record.copy(
            toppings = record.toppings.map { topping -> toppingUpdates[topping.id]?.let(topping::applied) ?: topping },
            texts = record.texts.map { text -> textUpdatesById[text.id]?.let(text::applied) ?: text },
        )
    }

    override suspend fun updateBorder(
        id: ParfaitImageId,
        border: ToppingBorder,
    ): Result<Unit> = edit { record ->
        record.copy(
            toppings = record.toppings.map { topping ->
                if (topping.id == id.value) topping.withBorder(border) else topping
            },
        )
    }

    override suspend fun deleteTopping(id: ParfaitImageId): Result<Unit> = edit { record ->
        record.copy(toppings = record.toppings.filterNot { topping -> topping.id == id.value })
    }

    override suspend fun placeText(
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): Result<Unit> = edit { record ->
        val entity = soloTextEntity(
            id = record.nextId,
            text = text,
            transform = transform,
            style = style,
            createdAt = clock.now(),
        )
        record.copy(nextId = record.nextId + 1, texts = record.texts + entity)
    }

    override suspend fun deleteText(id: ParfaitTextId): Result<Unit> = edit { record ->
        record.copy(texts = record.texts.filterNot { text -> text.id == id.value })
    }

    override suspend fun reset(): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            // 손상된 기록은 nextId 도 믿을 수 없어 1부터 다시 쓴다
            val emptyRecord = SoloSpaceRecordEntity(nextId = state.value.loadedRecordOrNull()?.nextId ?: 1)
            try {
                localDataSource.writeRecord(emptyRecord)
            } catch (e: Exception) {
                return@withLock failure(e, "비우기 저장 실패")
            }
            state.value = SoloSpaceState.Loaded(emptyRecord)
            // 기록이 먼저 비었으므로 여기서 실패해도 캔버스는 비어 있다. 남는 것은 가리키는 곳 없는 파일뿐이다
            runCatchingQuietly("비우기 뒤 images 정리 실패") { localDataSource.clearImages() }
            Result.success(Unit)
        }
    }

    override suspend fun delete(): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock {
            try {
                localDataSource.deleteRecord()
            } catch (e: Exception) {
                return@withLock failure(e, "기록 삭제 실패")
            }
            // 파일은 없지만 이 프로세스가 사는 동안은 id 를 이어 쓴다 — 아직 옛 id 를 든 화면이 있을 수 있다
            state.value = SoloSpaceState.Loaded(
                SoloSpaceRecordEntity(nextId = state.value.loadedRecordOrNull()?.nextId ?: 1),
            )
            runCatchingQuietly("삭제 뒤 images 정리 실패") { localDataSource.clearImages() }
            Result.success(Unit)
        }
    }

    private suspend fun edit(
        newImage: NewImage? = null,
        transform: (SoloSpaceRecordEntity) -> SoloSpaceRecordEntity,
    ): Result<Unit> = withContext(ioDispatcher) {
        mutex.withLock { editLocked(newImage, transform) }
    }

    private fun editLocked(
        newImage: NewImage?,
        transform: (SoloSpaceRecordEntity) -> SoloSpaceRecordEntity,
    ): Result<Unit> {
        loadIfNeededLocked()
        val current = when (val loaded = state.value) {
            is SoloSpaceState.Loaded -> loaded.record

            // 읽지 못한 기록 위에 쓰면 사용자의 캔버스가 이 변경 하나로 덮인다. 복구는 비우기만 한다
            is SoloSpaceState.Corrupted -> return Result.failure(loaded.error)

            SoloSpaceState.Unloaded -> error("loadIfNeededLocked 뒤에는 Unloaded 일 수 없다")
        }

        if (newImage != null) {
            try {
                localDataSource.copyImage(newImage.sourcePath, newImage.fileName)
            } catch (e: Exception) {
                return failure(e, "이미지 복사 실패")
            }
        }

        val updated = transform(current)
        if (updated == current) {
            return Result.success(Unit)
        }

        try {
            localDataSource.writeRecord(updated)
        } catch (e: Exception) {
            if (newImage != null) {
                runCatchingQuietly("저장 실패 뒤 새 사본 정리 실패") { localDataSource.deleteImage(newImage.fileName) }
            }
            return failure(e, "기록 저장 실패")
        }

        state.value = SoloSpaceState.Loaded(updated)
        // 기록이 더는 가리키지 않게 된 뒤에 지운다. 순서가 바뀌면 저장 실패 시 기록이 없는 파일을 가리킨다
        (current.imageFileNames() - updated.imageFileNames()).forEach { fileName ->
            runCatchingQuietly("버려진 이미지 정리 실패") { localDataSource.deleteImage(fileName) }
        }
        return Result.success(Unit)
    }

    /**
     * 파일이 없을 때만 빈 캔버스로 시작한다. 그 밖의 실패는 [SoloSpaceState.Corrupted] 로 두고 파일을 건드리지 않는다.
     *
     * 실패 상태에서 다시 불리면 한 번 더 읽어 본다 — 일시적 I/O 실패를 사용자가 비우기로만 풀게 두면 멀쩡한
     * 캔버스를 지운다. 다시 실패하면 기존 실패 값을 그대로 둬 이미 구독 중인 화면에 실패가 또 흐르지 않게 한다.
     */
    private fun loadIfNeededLocked() {
        val current = state.value
        if (current is SoloSpaceState.Loaded) {
            return
        }
        try {
            state.value = SoloSpaceState.Loaded(localDataSource.readRecord() ?: SoloSpaceRecordEntity())
        } catch (e: Exception) {
            repositoryLogger.e(e) { "혼자 공간 기록을 읽지 못했다" }
            if (current is SoloSpaceState.Unloaded) {
                state.value = SoloSpaceState.Corrupted(AppError.Unexpected(e))
            }
        }
    }

    private fun SoloSpaceState.toCanvasResult(): Result<SoloCanvasVO>? = when (this) {
        SoloSpaceState.Unloaded -> null
        is SoloSpaceState.Loaded -> Result.success(record.toVO(localDataSource::imageUri))
        is SoloSpaceState.Corrupted -> Result.failure(error)
    }

    private companion object {
        const val TOPPING_EXTENSION = "png"
        const val BACKGROUND_EXTENSION = "jpg"
    }
}

private sealed interface SoloSpaceState {
    data object Unloaded : SoloSpaceState

    data class Loaded(val record: SoloSpaceRecordEntity) : SoloSpaceState

    data class Corrupted(val error: AppError) : SoloSpaceState
}

private data class NewImage(
    val sourcePath: String,
    val fileName: String,
)

private fun SoloSpaceState.loadedRecordOrNull(): SoloSpaceRecordEntity? = (this as? SoloSpaceState.Loaded)?.record

private fun newFileName(extension: String): String = "${UUID.randomUUID()}.$extension"

private fun SoloSpaceRecordEntity.imageFileNames(): Set<String> = buildSet {
    (background as? SoloBackgroundEntity.Image)?.let { image -> add(image.fileName) }
    toppings.forEach { topping -> add(topping.fileName) }
}

private fun SoloToppingEntity.applied(update: ToppingTransformUpdate): SoloToppingEntity = copy(
    positionX = update.positionX ?: positionX,
    positionY = update.positionY ?: positionY,
    positionZ = update.positionZ ?: positionZ,
    scale = update.scale ?: scale,
    rotation = update.rotation ?: rotation,
)

private fun SoloTextEntity.applied(update: TextToppingTransformUpdate): SoloTextEntity = copy(
    positionX = update.positionX ?: positionX,
    positionY = update.positionY ?: positionY,
    positionZ = update.positionZ ?: positionZ,
    scale = update.scale ?: scale,
    rotation = update.rotation ?: rotation,
)

private fun failure(
    cause: Exception,
    message: String,
): Result<Unit> {
    repositoryLogger.e(cause) { "혼자 공간 $message" }
    return Result.failure(AppError.Unexpected(cause))
}

private inline fun runCatchingQuietly(
    message: String,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: Exception) {
        repositoryLogger.w(e) { "혼자 공간 $message" }
    }
}
