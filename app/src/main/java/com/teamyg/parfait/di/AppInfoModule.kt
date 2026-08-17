package com.teamyg.parfait.di

import com.teamyg.parfait.BuildConfig
import com.teamyg.parfait.core.util.jvm.model.AppVersionName
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 라이브러리 모듈은 자기 `BuildConfig` 에 앱 버전을 갖지 못한다 — `versionName` 은 애플리케이션
 * 모듈의 `defaultConfig` 에만 있다. 그래서 화면이 직접 읽는 대신 `:app` 이 여기서 꺼내 내려 준다.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppInfoModule {
    @Provides
    fun provideAppVersionName(): AppVersionName = AppVersionName(BuildConfig.VERSION_NAME)
}
