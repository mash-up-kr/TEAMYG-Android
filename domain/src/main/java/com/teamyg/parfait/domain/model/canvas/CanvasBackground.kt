package com.teamyg.parfait.domain.model.canvas

/**
 * 캔버스 배경.
 *
 * 서버는 { type, value } 평면인데 value 의 뜻이 type 에 따라 갈린다(색 문자열 vs 이미지 URL).
 * sealed 로 가르면 "색인 줄 알고 URL 을 넣는" 실수가 컴파일에서 막힌다.
 *
 * 미지 type 은 null 로 접는다 — 그려달라는 뜻을 모르는 것과 미설정은 화면에서 같은 처리다.
 *
 * 배경을 **바꿀 때는** 이 타입이 아니라 [CanvasBackgroundEdit] 를 쓴다 — 이미지 배경은
 * 읽을 때 URL, 쓸 때 imageId 라서 서버 계약이 비대칭이다(`api/parfait.md`).
 */
sealed interface CanvasBackground {
    @JvmInline
    value class Color(val value: String) : CanvasBackground

    @JvmInline
    value class Image(val url: String) : CanvasBackground
}
