package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * S3 presigned URL 로 파일 바이트를 보내는 표면.
 *
 * 자격증명을 붙이지 않고, 재발급 표면과 `Dispatcher` 를 공유하지 않으며, 본문을 로깅하지 않는다.
 * 이름은 사용처가 아니라 이 표면의 성질을 가리킨다.
 *
 * 이웃 [UnauthenticatedClient] 와 달리 리텐션이 `RUNTIME` 이다 — 이 한정자가 주입 자리에서
 * 빠지는 것이 이 라운드의 핵심 실패 모드인데, 이 저장소는 `dagger.fullBindingGraphValidation`
 * 을 설정하지 않아 Dagger 가 엔트리포인트에서 도달 가능한 바인딩만 검증하고, 소비자가 없는
 * 바인딩은 그 대상에서 빠진다. 리플렉션 테스트가 유일한 감지선이라 런타임까지 남겨 둔다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UploadClient
