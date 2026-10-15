package com.teamyg.parfait.feature.segmentation.impl.editor

/**
 * 되돌리기/다시 실행 스택.
 *
 * 무엇을 쌓는지는 알지 않으므로, 되돌리기가 서로 섞이면 안 되는 편집끼리 하나씩 나눠 들면 된다.
 *
 * @param done 확정된 편집. 뒤쪽이 가장 최근이다
 * @param undone 되돌려서 물러난 편집. 뒤쪽이 가장 먼저 다시 실행될 것이다
 */
data class UndoRedoStack<T>(
    val done: List<T> = emptyList(),
    val undone: List<T> = emptyList(),
) {
    /** 가장 마지막에 확정된 편집. 되돌리기가 먼저 벗겨낼 것이기도 하다 */
    val latest: T? get() = done.lastOrNull()

    val canUndo: Boolean get() = latest != null

    val canRedo: Boolean get() = undone.isNotEmpty()

    /** 새로 편집하면 다시 실행할 것이 없어진다 */
    fun push(item: T): UndoRedoStack<T> = UndoRedoStack(done = done + item, undone = emptyList())

    fun undo(): UndoRedoStack<T> {
        val last = latest ?: return this
        return UndoRedoStack(done = done.dropLast(1), undone = undone + last)
    }

    fun redo(): UndoRedoStack<T> {
        val last = undone.lastOrNull() ?: return this
        return UndoRedoStack(done = done + last, undone = undone.dropLast(1))
    }
}
