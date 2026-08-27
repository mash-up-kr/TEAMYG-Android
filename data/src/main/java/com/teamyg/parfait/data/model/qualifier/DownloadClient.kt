package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * 서버가 준 공개 이미지 URL에서 바이트를 그대로 받아오는 표면.
 *
 * 자격증명을 붙이지 않는다 — 이 URL 들은 애초에 인증 없이 접근 가능한 공개 주소다.
 * [UnauthenticatedClient] 를 재사용하지 않고 따로 두는 이유는 타임아웃이 다르기 때문이다:
 * 재발급은 짧고 예측 가능한 요청이지만 이미지 다운로드는 파일 크기·네트워크 상태에 따라
 * 오래 걸릴 수 있어 더 넉넉한 [READ][okhttp3.OkHttpClient.Builder.readTimeout]·
 * [CALL][okhttp3.OkHttpClient.Builder.callTimeout] 타임아웃이 필요하다.
 *
 * 이웃 [UploadClient] 와 같은 이유로 리텐션이 `RUNTIME` 이다 — 이 저장소는
 * `dagger.fullBindingGraphValidation` 을 설정하지 않아, 이 한정자가 주입 자리에서
 * 빠져도(예: 실수로 지워도) 소비자가 없어지는 순간 Dagger 가 조용히 넘어간다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DownloadClient
