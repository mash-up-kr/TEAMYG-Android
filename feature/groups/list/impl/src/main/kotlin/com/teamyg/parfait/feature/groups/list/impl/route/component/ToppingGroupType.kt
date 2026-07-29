package com.teamyg.parfait.feature.groups.list.impl.route.component

enum class ToppingGroupType(
    val translationX: Int,
    val translationY: Int,
    val imageRotate: Int,
    val chipOffsetY: Int,
) {
    Left1(
        translationX = 26,
        translationY = 16,
        imageRotate = 6,
        chipOffsetY = -7,
    ),
    Left2(
        translationX = 24,
        translationY = 11,
        imageRotate = 12,
        chipOffsetY = -5,
    ),
    Left3(
        translationX = 25,
        translationY = 16,
        imageRotate = -8,
        chipOffsetY = -8,
    ),
    Right1(
        translationX = 26,
        translationY = 16,
        imageRotate = -6,
        chipOffsetY = -6,
    ),
    Right2(
        translationX = 22,
        translationY = 8,
        imageRotate = -16,
        chipOffsetY = -3,
    ),
    Right3(
        translationX = 25,
        translationY = 16,
        imageRotate = -8,
        chipOffsetY = -8,
    ),
}
