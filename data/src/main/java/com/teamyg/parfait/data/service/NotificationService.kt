package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.service.model.request.notification.RegisterDeviceTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 서버 화이트리스트 밖이라 access token 이 필요하다. @NoAuth 를 붙이지 않는다.
 *
 * 성공이 204 본문 없음이라 반환 타입에 envelope 를 두지 않는다.
 *
 * 등록 해제 엔드포인트가 없는 것은 누락이 아니다 — 서버가 로그아웃·탈퇴에서 대신 지운다
 * (`api/notification.md`).
 */
interface NotificationService {
    @POST("api/v1/notifications/devices")
    suspend fun postNotificationsDevices(@Body request: RegisterDeviceTokenRequest)
}
