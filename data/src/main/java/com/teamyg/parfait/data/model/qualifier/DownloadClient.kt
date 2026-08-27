package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * 서버가 준 공개 이미지 URL에서 바이트를 그대로 받아오는 표면.
 *
 * 자격증명을 붙이지 않는다 — 이 URL 들은 애초에 인증 없이 접근 가능한 공개 주소다.
 * 타임아웃 프로필은 메인 클라이언트와 같다(더 늘려야 할 근거가 없다) — 이 한정자가
 * 따로 있는 이유는 순수하게 커넥션 풀·`Dispatcher` 를 메인/업로드/재발급 트래픽과
 * 나누기 위해서다.
 *
 * 이웃 [UploadClient] 와 같은 이유로 리텐션이 `RUNTIME` 이다 — 이 저장소는
 * `dagger.fullBindingGraphValidation` 을 설정하지 않아, 이 한정자가 주입 자리에서
 * 빠져도(예: 실수로 지워도) 소비자가 없어지는 순간 Dagger 가 조용히 넘어간다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DownloadClient
