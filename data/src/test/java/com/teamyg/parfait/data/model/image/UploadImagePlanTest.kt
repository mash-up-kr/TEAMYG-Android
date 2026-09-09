package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.image.ImageType
import com.teamyg.parfait.domain.model.image.SourceLongSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UploadImagePlanTest {
    @Test
    fun of_nukkiPngUnderLimit_passesThrough() {
        // Given 누끼 PNG 가 방어선 이하다
        val fileSize = UploadImageSize(width = 800, height = 1200)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 원본을 그대로 올린다 - 확대도 재인코딩도 하지 않는다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun of_nukkiPngOverLimit_scalesKeepingRatio() {
        // Given 긴 변이 누끼 방어선 1280 을 넘는데 원본 긴 변은 모른다
        val fileSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 긴 변이 방어선이 되고 짧은 변은 비율을 지킨다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1280, reencode.targetSize.height)
        assertEquals(868, reencode.targetSize.width)
        assertEquals(UploadImageFormat.PNG, reencode.format)
    }

    @Test
    fun of_limitDiffersByImageType() {
        // Given 두 상한 사이에 놓인 크기다 - 누끼 1500 초과, 배경 2048 이하
        val sourceSize = UploadImageSize(width = 1600, height = 1200)

        // When 같은 크기를 두 용도로 계획한다
        val nukkiPlan = UploadImagePlan.of(sourceSize, ImageType.NUKKI, UploadImageFormat.JPEG, null)
        val backgroundPlan = UploadImagePlan.of(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG, null)

        // Then 용도마다 상한이 달라 결과가 갈린다
        assertIs<UploadImagePlan.Reencode>(nukkiPlan)
        assertEquals(UploadImagePlan.Passthrough, backgroundPlan)
    }

    @Test
    fun of_backgroundPngUnderLimit_reencodesToJpeg() {
        // Given 상한 이하인 PNG 스크린샷을 배경으로 고른다
        val sourceSize = UploadImageSize(width = 1080, height = 1920)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(sourceSize, ImageType.BACKGROUND, UploadImageFormat.PNG, null)

        // Then 치수는 그대로지만 포맷 때문에 다시 굽는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(sourceSize, reencode.targetSize)
        assertEquals(UploadImageFormat.JPEG, reencode.format)
    }

    @Test
    fun of_backgroundJpegAtExactLimit_passesThrough() {
        // Given 긴 변이 배경 상한과 정확히 같다
        val sourceSize = UploadImageSize(width = 2048, height = 1000)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG, null)

        // Then 경계값은 축소 대상이 아니다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun of_backgroundJpegOneOverLimit_scales() {
        // Given 긴 변이 상한보다 1px 크다
        val sourceSize = UploadImageSize(width = 2049, height = 1000)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG, null)

        // Then 축소한다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2048, reencode.targetSize.width)
    }

    @Test
    fun of_extremeAspectRatio_keepsShortSideAtLeastOne() {
        // Given 짧은 변이 비율대로 줄이면 0 이 되는 극단 종횡비다
        val fileSize = UploadImageSize(width = 6000, height = 2)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 0 픽셀 비트맵은 만들 수 없으므로 1 로 바닥을 친다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1280, reencode.targetSize.width)
        assertEquals(1, reencode.targetSize.height)
    }

    @Test
    fun of_sampleSizeNeverUndershootsTarget() {
        // Given 큰 사진이다
        val fileSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, null)

        // Then 사전 축소판이 목표보다 작아지면 안 된다 - 그러면 확대해서 맞추게 된다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2, reencode.sampleSize)
        assertEquals(true, fileSize.width / reencode.sampleSize >= reencode.targetSize.width)
        assertEquals(true, fileSize.height / reencode.sampleSize >= reencode.targetSize.height)
    }

    @Test
    fun of_nukkiScalesBySourceRatio_notByOwnLongSide() {
        // Given 알맹이는 방어선 아래지만 원본 사진이 크다
        val fileSize = UploadImageSize(width = 925, height = 450)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 알맹이 자신의 긴 변이 아니라 원본 배율(1280/4032)로 줄어든다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(294, reencode.targetSize.width)
        assertEquals(143, reencode.targetSize.height)
    }

    @Test
    fun of_nukkiSourceUnderThreshold_passesThrough() {
        // Given 원본 사진의 긴 변이 이미 1280 이하다
        val fileSize = UploadImageSize(width = 900, height = 700)
        val sourceLongSide = SourceLongSide(1200)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 확대는 어떤 경우에도 하지 않는다 - 재업로드가 누적되지 않는 근거다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun of_nukkiScaledBelowMinimum_isPulledBackToMinimum() {
        // Given 배율대로면 결과가 하한보다 잘아진다
        val fileSize = UploadImageSize(width = 640, height = 400)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 하한까지 되돌린다 - 그 아래로는 앱이 실루엣조차 구분하지 못한다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(256, reencode.targetSize.width)
        assertEquals(160, reencode.targetSize.height)
    }

    @Test
    fun of_nukkiMinimumIsMonotonic_noCliffAtTheBoundary() {
        // Given 1px 만 다른 두 알맹이를 같은 원본에서 오려냈다
        val sourceLongSide = SourceLongSide(4032)

        // When 각각 계획을 세운다
        val smaller = UploadImagePlan.of(
            UploadImageSize(640, 400),
            ImageType.NUKKI,
            UploadImageFormat.PNG,
            sourceLongSide,
        )
        val larger = UploadImagePlan.of(
            UploadImageSize(641, 400),
            ImageType.NUKKI,
            UploadImageFormat.PNG,
            sourceLongSide,
        )

        // Then 큰 알맹이가 작은 알맹이보다 작게 올라가지 않는다 - 하한을 입력에 걸면 깨지던 성질이다
        val smallerTarget = assertIs<UploadImagePlan.Reencode>(smaller).targetSize
        val largerTarget = assertIs<UploadImagePlan.Reencode>(larger).targetSize
        assertEquals(256, smallerTarget.width)
        assertEquals(256, largerTarget.width)
    }

    @Test
    fun of_nukkiMinimumNeverEnlarges() {
        // Given 알맹이 자체가 하한보다 작다
        val fileSize = UploadImageSize(width = 100, height = 80)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 하한까지 키우지 않는다 - 되돌림의 상한이 알맹이 자신이다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun of_nukkiWellAboveMinimum_scalesByRatio() {
        // Given 배율대로 줄여도 결과가 하한보다 크다
        val fileSize = UploadImageSize(width = 641, height = 400)
        val sourceLongSide = SourceLongSide(2560)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 하한이 개입하지 않고 배율만 먹는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(321, reencode.targetSize.width)
        assertEquals(200, reencode.targetSize.height)
    }

    @Test
    fun of_nukkiCorruptSourceSmallerThanFile_neverEnlarges() {
        // Given 원본 긴 변이 알맹이보다 작다고 주장하는 망가진 입력이다
        val fileSize = UploadImageSize(width = 2000, height = 1000)
        val sourceLongSide = SourceLongSide(500)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.NUKKI, UploadImageFormat.PNG, sourceLongSide)

        // Then 배율 갈래를 건너뛰고 방어선만 걸린다 - 확대는 만들지 않는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1280, reencode.targetSize.width)
        assertEquals(640, reencode.targetSize.height)
    }

    @Test
    fun of_backgroundIgnoresSourceLongSide() {
        // Given 배경인데 원본 긴 변이 실려 왔다
        val fileSize = UploadImageSize(width = 1600, height = 1200)
        val sourceLongSide = SourceLongSide(4032)

        // When 계획을 세운다
        val plan = UploadImagePlan.of(fileSize, ImageType.BACKGROUND, UploadImageFormat.JPEG, sourceLongSide)

        // Then 배경은 이 값을 보지 않는다 - 2048 이하라 그대로 통과한다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }
}
