package com.teamyg.parfait.data.model.image

import com.teamyg.parfait.domain.model.image.ImageType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UploadImageScaleTest {
    @Test
    fun planUploadImage_nukkiPngUnderLimit_passesThrough() {
        // Given 누끼 PNG 가 상한 이하다
        val sourceSize = UploadImageSize(width = 1000, height = 1500)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 원본을 그대로 올린다 - 확대도 재인코딩도 하지 않는다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun planUploadImage_nukkiPngOverLimit_scalesKeepingRatio() {
        // Given 긴 변이 누끼 상한 1500 을 넘는다
        val sourceSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 긴 변이 상한이 되고 짧은 변은 비율을 지킨다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1500, reencode.targetSize.height)
        assertEquals(1018, reencode.targetSize.width)
        assertEquals(UploadImageFormat.PNG, reencode.format)
    }

    @Test
    fun planUploadImage_limitDiffersByImageType() {
        // Given 두 상한 사이에 놓인 크기다 - 누끼 1500 초과, 배경 2048 이하
        val sourceSize = UploadImageSize(width = 1600, height = 1200)

        // When 같은 크기를 두 용도로 계획한다
        val nukkiPlan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.JPEG)
        val backgroundPlan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG)

        // Then 용도마다 상한이 달라 결과가 갈린다
        assertIs<UploadImagePlan.Reencode>(nukkiPlan)
        assertEquals(UploadImagePlan.Passthrough, backgroundPlan)
    }

    @Test
    fun planUploadImage_backgroundPngUnderLimit_reencodesToJpeg() {
        // Given 상한 이하인 PNG 스크린샷을 배경으로 고른다
        val sourceSize = UploadImageSize(width = 1080, height = 1920)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.PNG)

        // Then 치수는 그대로지만 포맷 때문에 다시 굽는다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(sourceSize, reencode.targetSize)
        assertEquals(UploadImageFormat.JPEG, reencode.format)
    }

    @Test
    fun planUploadImage_backgroundJpegAtExactLimit_passesThrough() {
        // Given 긴 변이 배경 상한과 정확히 같다
        val sourceSize = UploadImageSize(width = 2048, height = 1000)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG)

        // Then 경계값은 축소 대상이 아니다
        assertEquals(UploadImagePlan.Passthrough, plan)
    }

    @Test
    fun planUploadImage_backgroundJpegOneOverLimit_scales() {
        // Given 긴 변이 상한보다 1px 크다
        val sourceSize = UploadImageSize(width = 2049, height = 1000)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.BACKGROUND, UploadImageFormat.JPEG)

        // Then 축소한다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2048, reencode.targetSize.width)
    }

    @Test
    fun planUploadImage_extremeAspectRatio_keepsShortSideAtLeastOne() {
        // Given 짧은 변이 비율대로 줄이면 0 이 되는 극단 종횡비다
        val sourceSize = UploadImageSize(width = 6000, height = 2)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 0 픽셀 비트맵은 만들 수 없으므로 1 로 바닥을 친다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(1500, reencode.targetSize.width)
        assertEquals(1, reencode.targetSize.height)
    }

    @Test
    fun planUploadImage_sampleSizeNeverUndershootsTarget() {
        // Given 큰 사진이다
        val sourceSize = UploadImageSize(width = 2600, height = 3832)

        // When 계획을 세운다
        val plan = planUploadImage(sourceSize, ImageType.NUKKI, UploadImageFormat.PNG)

        // Then 사전 축소판이 목표보다 작아지면 안 된다 - 그러면 확대해서 맞추게 된다
        val reencode = assertIs<UploadImagePlan.Reencode>(plan)
        assertEquals(2, reencode.sampleSize)
        assertEquals(true, sourceSize.width / reencode.sampleSize >= reencode.targetSize.width)
        assertEquals(true, sourceSize.height / reencode.sampleSize >= reencode.targetSize.height)
    }
}
