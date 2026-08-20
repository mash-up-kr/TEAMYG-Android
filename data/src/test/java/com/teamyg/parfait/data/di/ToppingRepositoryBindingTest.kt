package com.teamyg.parfait.data.di

import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import kotlin.test.Test
import kotlin.test.assertTrue

class ToppingRepositoryBindingTest {
    @Test
    fun repositoryModule_bindsToppingRepository() {
        // Given·When 모듈이 선언한 바인딩의 반환 타입을 본다
        val boundTypes = RepositoryModule::class.java.methods.map { method -> method.returnType }

        // Then 배치 Repository 가 그중에 있다. 소비자가 0 이라 Dagger 가 이 그래프를 검증하지
        // 않으므로 이 단언이 누락을 잡는 유일한 선이다
        assertTrue(ToppingRepository::class.java in boundTypes)
    }
}
