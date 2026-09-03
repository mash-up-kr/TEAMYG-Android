package com.teamyg.parfait.domain.model.qualifier

import javax.inject.Qualifier

/** 프로세스와 수명을 같이 하는 스코프. 화면·ViewModel 보다 오래 살아야 하는 작업에만 쓴다 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
