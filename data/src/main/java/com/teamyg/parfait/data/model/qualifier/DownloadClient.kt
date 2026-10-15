package com.teamyg.parfait.data.model.qualifier

import javax.inject.Qualifier

/**
 * 서버가 준 공개 이미지 URL에서 바이트를 받아오는 표면. 공개 주소라 자격증명을 붙이지 않는다.
 *
 * 타임아웃은 메인 클라이언트와 같다. 한정자를 따로 둔 이유는 커넥션 풀·`Dispatcher` 를
 * 메인/업로드/재발급 트래픽과 나누기 위해서다. 리텐션이 `RUNTIME` 인 이유는 [UploadClient] 와 같다.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DownloadClient
