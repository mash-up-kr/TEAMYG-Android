package com.teamyg.parfait.core.util.jvm.extension

import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TOLERANCE = 1e-3f

/** 씨앗 자리를 그린 그림에서 판을 만든다. `#` 이 씨앗이다 */
private fun seedField(vararg rows: String): FloatArray {
    val width = rows.first().length
    return FloatArray(rows.size * width) { index ->
        if (rows[index / width][index % width] == '#') 0f else SQUARED_DISTANCE_UNSET
    }
}

/** 씨앗을 모두 훑어 가장 가까운 것을 찾는, 느리지만 틀릴 수 없는 방식 */
private fun bruteForce(
    field: FloatArray,
    width: Int,
    height: Int,
): FloatArray {
    val seeds = field.indices.filter { index -> field[index] == 0f }
    return FloatArray(width * height) { index ->
        seeds.minOfOrNull { seed ->
            val gapX = index % width - seed % width
            val gapY = index / width - seed / width
            (gapX * gapX + gapY * gapY).toFloat()
        } ?: SQUARED_DISTANCE_UNSET
    }
}

class FloatArrayExtensionTest {
    @Test
    fun fillWithSquaredDistance_singleSeed_measuresFromThatSeed() {
        // Given 한가운데 한 칸만 씨앗인 판
        val field = seedField(
            ".....",
            ".....",
            "..#..",
            ".....",
            ".....",
        )

        // When 거리를 재면
        field.fillWithSquaredDistance(width = 5, height = 5)

        // Then 씨앗 자리는 0 이고 대각선 한 칸은 제곱거리 2 다
        assertEquals(0f, field[2 * 5 + 2])
        assertEquals(1f, field[2 * 5 + 3])
        assertEquals(2f, field[1 * 5 + 1])
        assertEquals(8f, field[0])
    }

    @Test
    fun fillWithSquaredDistance_everySeed_leavesZero() {
        val field = seedField("###", "###")

        field.fillWithSquaredDistance(width = 3, height = 2)

        assertTrue(field.all { distance -> distance == 0f })
    }

    @Test
    fun fillWithSquaredDistance_noSeed_staysUnset() {
        // Given 씨앗이 하나도 없는 판
        val field = seedField("...", "...")

        // When 거리를 재면
        field.fillWithSquaredDistance(width = 3, height = 2)

        // Then 어디에서도 가까운 씨앗을 찾지 못해 재지 않은 값 언저리에 남는다
        assertTrue(field.all { distance -> distance >= SQUARED_DISTANCE_UNSET })
    }

    @Test
    fun fillWithSquaredDistance_singleRow_measuresAlongTheRow() {
        val field = seedField("#....")

        field.fillWithSquaredDistance(width = 5, height = 1)

        assertEquals(listOf(0f, 1f, 4f, 9f, 16f), field.toList())
    }

    @Test
    fun fillWithSquaredDistance_shapeWithHole_matchesBruteForce() {
        // Given 안이 빈 도넛 모양처럼 씨앗이 흩어진 판
        val field = seedField(
            "..###..",
            ".#...#.",
            "#.....#",
            ".#...#.",
            "..###..",
        )
        val expected = bruteForce(field.copyOf(), width = 7, height = 5)

        // When 거리를 재면
        field.fillWithSquaredDistance(width = 7, height = 5)

        // Then 모두 훑어 구한 값과 같다
        field.indices.forEach { index -> assertEquals(expected[index], field[index], TOLERANCE) }
    }

    @Test
    fun fillWithSquaredDistance_randomFields_matchBruteForce() {
        val random = Random(seed = 20260813)

        repeat(times = 50) {
            // Given 크기도 씨앗도 무작위인 판
            val width = random.nextInt(from = 1, until = 20)
            val height = random.nextInt(from = 1, until = 20)
            val field = FloatArray(width * height) {
                if (random.nextInt(from = 0, until = 5) == 0) 0f else SQUARED_DISTANCE_UNSET
            }
            // 씨앗이 하나도 없으면 잴 거리가 없어 비교할 것도 없다
            field[random.nextInt(from = 0, until = field.size)] = 0f
            val expected = bruteForce(field.copyOf(), width, height)

            // When 거리를 재면
            field.fillWithSquaredDistance(width, height)

            // Then 어느 판에서도 모두 훑어 구한 값과 같다
            field.indices.forEach { index ->
                val message = "width=$width height=$height index=$index"
                assertEquals(sqrt(expected[index]), sqrt(field[index]), TOLERANCE, message)
            }
        }
    }
}
