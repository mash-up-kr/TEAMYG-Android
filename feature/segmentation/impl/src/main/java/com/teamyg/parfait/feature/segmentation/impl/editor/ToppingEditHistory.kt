package com.teamyg.parfait.feature.segmentation.impl.editor

/**
 * 되돌리기/다시 실행 스택.
 *
 * 탭마다 만지는 대상이 달라 되돌릴 대상도 다르므로, 하나의 스택을 공유하지 않고
 * [ToppingEditTab] 별로 이 타입을 따로 하나씩 들고 간다.
 * 영역 탭에서 획을 지운 뒤 테두리 탭에서 되돌리기를 눌러도 획이 살아나지 않아야 한다.
 *
 * @param done 확정된 편집. 뒤쪽이 가장 최근이다
 * @param undone 되돌려서 물러난 편집. 뒤쪽이 가장 먼저 다시 실행될 것이다
 */
data class ToppingEditHistory<T>(
    val done: List<T> = emptyList(),
    val undone: List<T> = emptyList(),
) {
    val canUndo: Boolean get() = done.isNotEmpty()

    val canRedo: Boolean get() = undone.isNotEmpty()

    /** 새로 편집하면 다시 실행할 것이 없어진다 */
    fun push(item: T): ToppingEditHistory<T> = ToppingEditHistory(done = done + item, undone = emptyList())

    fun undo(): ToppingEditHistory<T> {
        val last = done.lastOrNull() ?: return this
        return ToppingEditHistory(done = done.dropLast(1), undone = undone + last)
    }

    fun redo(): ToppingEditHistory<T> {
        val last = undone.lastOrNull() ?: return this
        return ToppingEditHistory(done = done + last, undone = undone.dropLast(1))
    }

    /**
     * 가장 최근 편집을 그 자리에서 손본다. 되돌릴 칸을 새로 만들지 않고 마지막 칸의 내용만 바뀐다.
     *
     * 슬라이더처럼 값이 이어서 변하는 조작에 쓴다. 미는 동안 매번 칸을 쌓으면
     * 되돌리기 한 번에 한 칸씩만 물러나 쓸모가 없어진다.
     */
    fun replaceLast(transform: (T) -> T): ToppingEditHistory<T> {
        val last = done.lastOrNull() ?: return this
        return ToppingEditHistory(done = done.dropLast(1) + transform(last), undone = emptyList())
    }
}
