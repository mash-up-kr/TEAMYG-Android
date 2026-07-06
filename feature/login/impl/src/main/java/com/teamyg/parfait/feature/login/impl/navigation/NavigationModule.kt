package com.teamyg.parfait.feature.login.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.login.impl.util.KakaoLoginHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
object NavigationModule {
    @IntoSet
    @Provides
    fun provideFeatureLoginEntryBuilder(
        kakaoLoginHelper: KakaoLoginHelper,
    ): EntryProviderScope<NavKey>.(Navigator) -> Unit = {
        featureLoginEntryBuilder(navigator = it, kakaoLoginHelper = kakaoLoginHelper)
    }
}
