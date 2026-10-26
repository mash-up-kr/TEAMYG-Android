package com.teamyg.parfait.data.di

import com.teamyg.parfait.domain.repository.image.ImageUploadRepository
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class ToppingRepositoryBindingTest {
    @Test
    fun repositoryModule_bindsToppingRepositoryAndImageUploadRepository() {
        // Given·When 모듈이 선언한 바인딩의 반환 타입을 본다
        val boundTypes = RepositoryModule::class.java.methods.map { method -> method.returnType }

        // Then 이 모듈의 UseCase 가 둘 다 주입받는다. 바인딩 누락은 컴파일이 잡지 못할 수 있어
        // 여기서 단언한다 (근거: specs/2026-08-20-c106-topping-place-api.md)
        assertTrue(ToppingRepository::class.java in boundTypes)
        assertTrue(ImageUploadRepository::class.java in boundTypes)
    }

    @Test
    fun repositoryModule_bindsTextToppingRepository() {
        // Given·When 모듈이 선언한 바인딩의 반환 타입을 본다
        val boundTypes = RepositoryModule::class.java.methods.map { method -> method.returnType }

        // Then 텍스트 유스케이스 셋이 주입받는 저장소가 묶여 있다
        assertTrue(TextToppingRepository::class.java in boundTypes)
    }
}
