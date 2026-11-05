---
paths:
  - "**/*.kt"
---

# 함수 안에 함수를 선언하지 않는다

- 함수·컴포저블·람다 본문 안에 `fun` 을 선언하지 않는다(Kotlin local function). 파일 레벨 `private fun` 으로 빼고,
  안에서 캡처하던 값은 파라미터로 넘긴다. 특정 타입을 리시버로 쓰면 그 타입의 `private` 확장 함수로 둔다.
- 예: `CameraPreviewComponent` 의 바인딩 리스너 안 `fun bind(selector)` →
  파일 아래 `private fun ProcessCameraProvider.bindUseCases(lifecycleOwner, selector, preview, imageCapture): Camera?`.
- 이유: 리뷰에서 두 번 이상 지적됐다(PR #649 `CanvasToppingLayer.kt`, PR #670 `CameraPreviewComponent.kt`).
  긴 본문 속에 숨어 읽기 어렵고, 무엇을 캡처하는지가 시그니처에 드러나지 않는다.
- 람다 값(`val onClick = { ... }`)은 이 규칙의 대상이 아니다.
