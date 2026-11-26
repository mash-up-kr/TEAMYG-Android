package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.lifecycle.SavedStateHandle
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasToGalleryUseCase
import com.teamyg.parfait.domain.usecase.gallery.SaveCanvasVideoToGalleryUseCase
import com.teamyg.parfait.domain.usecase.image.AddRecentImageUseCase
import com.teamyg.parfait.domain.usecase.parfait.ObserveParfaitDayBoundaryUseCase
import com.teamyg.parfait.domain.usecase.topping.ClearToppingDraftUseCase
import com.teamyg.parfait.domain.usecase.topping.GetToppingDraftFlowUseCase
import com.teamyg.parfait.domain.usecase.topping.StartToppingDraftUseCase
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 혼자 공간은 서버·애널리틱스에 닿지 않는다. ViewModel 생성자에 허용 목록 밖의 타입이 들어오면
 * (`AnalyticsLogger` 포함) 여기서 막는다.
 */
class SoloViewModelDependencyTest {
    private val viewModels: List<KClass<*>> = listOf(
        SoloCanvasViewModel::class,
        SoloBGEditViewModel::class,
        SoloToppingPlaceViewModel::class,
        SoloTextInputViewModel::class,
        SoloToppingArrangeViewModel::class,
    )

    /** 혼자 공간 use case 패키지 밖에서 허용하는 기기 로컬 use case. 서버 저장소에 닿지 않는 것만 둔다 */
    private val allowedUseCases: Set<Class<*>> = setOf(
        StartToppingDraftUseCase::class.java,
        GetToppingDraftFlowUseCase::class.java,
        ClearToppingDraftUseCase::class.java,
        AddRecentImageUseCase::class.java,
        SaveCanvasToGalleryUseCase::class.java,
        SaveCanvasVideoToGalleryUseCase::class.java,
        ObserveParfaitDayBoundaryUseCase::class.java,
    )

    private fun isAllowed(type: Class<*>): Boolean = type.isPrimitive ||
        type == String::class.java ||
        type == SavedStateHandle::class.java ||
        type.name.startsWith(SOLO_USE_CASE_PACKAGE) ||
        type in allowedUseCases

    @Test
    fun soloViewModels_constructorParameters_stayInsideAllowList() {
        // Given 혼자 공간 ViewModel 다섯의 주입 생성자
        val violations = viewModels.flatMap { viewModel ->
            val constructors = viewModel.java.declaredConstructors.filter { constructor ->
                constructor.isAnnotationPresent(Inject::class.java) ||
                    constructor.annotations.any { it.annotationClass.simpleName == "AssistedInject" }
            }
            assertTrue(constructors.isNotEmpty(), "${viewModel.simpleName} 에 주입 생성자가 없다")

            // When 파라미터 타입을 허용 목록과 대조한다
            constructors
                .flatMap { it.parameterTypes.toList() }
                .filterNot(::isAllowed)
                .map { "${viewModel.simpleName} -> ${it.name}" }
        }

        // Then 서버·애널리틱스 의존은 하나도 없다
        assertTrue(violations.isEmpty(), "허용 목록 밖 의존: $violations")
    }

    private companion object {
        const val SOLO_USE_CASE_PACKAGE = "com.teamyg.parfait.domain.usecase.solo."
    }
}
