package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.core.content.FileProvider

/**
 * 캔버스 캐시(캡처 PNG·타임랩스 mp4)를 인스타그램에 content uri 로 넘기기 위한 FileProvider.
 *
 * 하위 클래스를 따로 두는 이유: data 모듈이 이미 FileProvider 를 두 번 선언했고(authority 만 다름),
 * androidx 기본 클래스를 또 선언하면 매니페스트 병합에서 provider 가 충돌한다 — 클래스 이름이 달라야 한다.
 */
class CanvasShareFileProvider : FileProvider()
