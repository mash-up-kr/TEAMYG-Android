package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * S3 presigned URL 로 파일 바이트를 보내는 표면.
 *
 * 자격증명을 붙이지 않고, 재발급 표면과 `Dispatcher` 를 공유하지 않으며, 본문을 로깅하지 않는다.
 *
 * 리텐션이 `RUNTIME` 인 이유: `dagger.fullBindingGraphValidation` 을 켜지 않아 Dagger 는 소비자
 * 없는 바인딩을 검증하지 않는다. 주입 자리에서 이 한정자가 빠진 것을 잡는 건 리플렉션 테스트뿐이다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UploadClient
