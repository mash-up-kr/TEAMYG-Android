---
id: open-questions
title: Open Questions — 구현 미결·열린 결정
category: meta
status: living
platforms: android
verified: 2026-09-21
related_spec: topping-edit-empty-subject-guard, upload-image-downscale, push-notification-permission-and-device-token, canvas-today-ssot-polling, topping-alpha-hit-test, segmentation-mask-postprocessing, segmentation-alpha-refinement, alpha-kernel-suspend-cancellation, segmentation-preprocessing, c001-canvas-gallery-save, c301-topping-edit-tab, c106-topping-place-api, c106-topping-place, user-info-ssot, app-setting-s001, s004-terms-privacy-webview, canvas-detail-background-api-service-layer, c201-canvas-calendar, c201-canvas-calendar-server, c001-canvas-today-detail, session-token-refresh-infra, c301-canvas-background-edit, c103-segmentation-topping-edit, intro-term-agree, designsystem-bar-listdate-components, designsystem-text-component-sync, a005-group-create, s002-account-info, data-network-setup, network-envelope-token-storage, designsystem-grouptag-topping-components, designsystem-button-component-sync, designsystem-button-missing-components, designsystem-canvas-components, g001-group-list, c101-camera-picture-confirm, c102-custom-gallery-picker, parfait-api-contract-docs, data-api-service-layer, unit-test-infrastructure, ci-gradle-cache-seeding, a002-login-onboarding, c001-canvas-main, image-api-service-layer, member-parfait-image-api-service-layer, a004-group-invite-code, s102-group-nickname, mvi-error-infrastructure, a002-kakao-login-api, ygscaffold-v2-common-loading-error, s101-group-setting-api, screen-resume-refetch, canvas-save-preview-capture-holder, topping-border-distance-field, g001-group-list-topping-border, build-cache-measurement-harness
related_adr: ADR-0004, ADR-0010, ADR-0011, ADR-0012, ADR-0013, ADR-0014, ADR-0016, ADR-0017, ADR-0018, ADR-0019, ADR-0020, ADR-0021, ADR-0022, ADR-0025, ADR-0026, ADR-0029, ADR-0030
related_architecture: design-system, data-layer, navigation-flow, module-structure, state-management
related_code:
tags: [meta, parfait]
---
# Open Questions — 구현 미결·열린 결정

TJYG-Android 구현에서 발견된 미결 결정·계약 공백·코드/문서 정합 이슈를 추적한다.
정책 기획 쪽 미결은 위키 [[open-questions]]에 있다. 여기는 **코드·ADR·architecture 소관**만 둔다.
해소된 항목은 상태를 "해소됨"으로 바꾸고 관련 ADR/architecture 문서에 반영한다.

> **읽는 법 — 부정 매칭.** 상태는 열거형이 아니라 자유 서술이다(`미해결 (코드 수정 대상)`,
> `부분 해소 (② 해소, ① 잔존)`, `보류 (온디바이스로 잠정 채택, beta 추적 중)` 등).
> **`해소됨`으로 시작하지 않는 모든 항목은 미결**로 취급한다 — `미해결` 문자열 검색은
> `부분 해소 (…잔존)` 항목을 통째로 놓친다. `해소됨`이어도 메모에 잔존 조건이 있으면 살아 있다.
> 괄호 안까지 그대로 읽을 것.

> 링크 규약: parfait 내부 문서는 상대 md 링크(`../adr/…`, `../architecture/…`). 위키 개념 참조만 `[[…]]`.

---

### [2026-07-10] YGButton 디자인 토큰 규칙 미확정
- **ID**: OQ-P-001
- **출처**: `component/ygbutton/YGButtonType.kt` — 각 변형 `colors`가 시맨틱(`YGTheme.colorScheme`) 대신 `YGAtomicColors`를 직접 참조, 값 잠정(mock). 코드 주석 "Design Token 규칙이 조금 이상… 컴포넌트 완성 시점에 문의 예정".
- **항목**: ① 컴포넌트가 원자 색을 직접 읽는 것을 시맨틱 계층으로 정리할지, ② XSmall/Small/… 변형별 패딩·radius·textStyle 토큰 매핑 확정.
- **상태**: 미해결
- **해소 메모**: 컴포넌트 완성·디자인 토큰 규칙 확정 시 [design-system](../architecture/design-system.md) 규약과 [ADR-0010](../adr/0010-custom-compositionlocal-theme.md) 원칙(시맨틱 우선)에 맞춰 정리.

### [2026-07-12] BitmapWrapper stub — 계약 없는 추상
- **ID**: OQ-P-002
- **출처**: `core/util/jvm`의 `BitmapWrapper`(멤버 없음, `// TODO 차후 비트맵 사용에 필요한 함수 구현`), `core/util/android`의 `AndroidBitmap`(`// TODO delegate 사용하도록 수정`).
- **항목**: ① 도메인이 비트맵에 필요한 연산을 `BitmapWrapper` 계약으로 정의할지(현재는 `data`에서 `as? AndroidBitmap` 다운캐스트에 의존), ② `getRawData()` 직접 노출을 유지할지.
- **상태**: 미해결
- **해소 메모**: 필요한 연산 확정 시 [ADR-0011](../adr/0011-cross-module-bitmap-abstraction.md) 본문·`BitmapWrapper`에 반영해 다운캐스트 의존을 줄인다.

### [2026-07-12] ML Kit Subject Segmentation beta 의존
- **ID**: OQ-P-003
- **출처**: `gradle/libs.versions.toml`의 `mlkitSubjectSegmentation`(beta), `feature/segmentation/impl`의 `AndroidManifest` install-time 모델. [ADR-0012](../adr/0012-mlkit-subject-segmentation.md).
- **항목**: ① beta 승급·API 변동 추적, ② GMS 미탑재 기기 대응, ③ subject PNG 캐시 파일(`cacheDir`) 정리 정책, ④ [[누끼-따기]] "온디바이스 vs 서버" 미결의 온디바이스 잠정 확정 여부.
- **상태**: 부분 해소 (③ 캐시 정리 정책 — **PR #309 develop 머지, 2026-08-20.**
  ①은 잔존, ②는 방어만 붙은 채 미해결, ④는 온디바이스 잠정 채택 유지로 별개)
  > 📌 **②는 방어가 붙고 ③은 커졌다(2026-08-14, PR #221)** — 매니페스트 meta-data가 보장이 아니라는 전제 아래
  > `ModuleInstall.areModulesAvailable`/`installModules`로 사용 직전 확인·설치를 넣고, 실패를
  > `SegmentationException.ModuleNotReady`(일시적)와 `Process`로 갈랐다. **다만 앱에는 재시도 경로가 없다** —
  > 에러 화면이 닫기(빈 람다)뿐이라 "잠시 후 다시"라고 안내만 하고 다시 시도할 수단을 주지 않는다.
  > ③은 반대로 악화됐다 — 편집을 마칠 때마다 `parfait_<timestamp>.png`가 최대 2장 더 늘고(알맹이 + 최종본)
  > 삭제 경로는 여전히 없다.
  > ✅ **③이 닫혔다(2026-08-18, `refactor/segmentation-logic`)** — 저장 위치를 `cacheDir` 하위
  > **세그멘테이션 전용 디렉토리**(`SegmentationCacheDir.kt`)로 옮기고, **세그멘테이션 진입 시**(디코드보다
  > 먼저) 그 디렉토리를 통째로 비운다(`ClearSegmentationCacheUseCase`). 새 흐름은 캔버스에서만 시작하고
  > 그러려면 이전 흐름 화면들이 이미 백스택에서 걷혀 있어 진행 중인 흐름을 지울 위험이 없다 — 누적 상한이
  > 직전 흐름 1회분이 된다. 리뷰가 정리 호출 자체가 `init` 코루틴의 무방비 첫 문장이던 것을 찾아
  > `runCatching`으로 감쌌다(best-effort, 정리 실패가 세그멘테이션을 막지 않는다).
  > ⚠️ **위 안전 근거는 같은 라운드 막바지까지 거짓이었다** — `CanvasToppingPlaceRoute`의 배치 완료
  > 이펙트가 `goTo(NavKeyCanvasMain(groupId = 0L))`로 캔버스를 **새로 쌓아** 방금 끝난 흐름의 화면들을
  > 백스택에 남기고 있었다. 그 상태에서 다음 흐름이 캐시를 비우면 남아 있던 화면들이 가리키던 PNG가
  > 지워져 뒤로 가면 빈 이미지·먹통 버튼이 됐다. 최종 리뷰가 `navigator.popUpTo<NavKeyCanvasMain>()`으로
  > 바꿔(이전 흐름을 걷어내는 쪽으로) 안전 근거를 참으로 만들었다. **①(재시도 경로 없음)은 그대로
  > 잔존한다** — 이 라운드는 캐시만 다뤘다. **세그멘테이션 캐시만 닫혔다는 점도 분명히 한다** — 카메라
  > 캐시(`FileCameraCacheLocalDataSourceImpl`)는 여전히 정리 경로가 없고 초 단위 파일명이라 충돌도
  > 남는다. 원본 다운샘플 부재로 인한 메모리 위험은 OQ-P-228(신규 항목, 아래)로 갈랐다 →
  > [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md).
  > 🔧 **정정(2026-08-19)** — 위 ⚠️ 문단은 develop에 미머지인 PR #290(`feature/topping-add-screen`)을
  > 로컬 머지해 얹은 `refactor/segmentation-logic` 브랜치 기준이다. `CanvasToppingPlaceRoute`는
  > **#290이 들여오는 화면**이라 plain develop에는 애초에 없다. 같은 작업을 develop 위
  > `refactor/segmentation-develop`(develop `86f0f6b0` + 커밋 11개)로 다시 만들면서 이 수정
  > (`ea278cce`)도 #290과 함께 빠졌다 — **뺀 것이 맞다.** develop 기준 흐름은
  > `SegmentationConfirmRoute`(`onClickNext`) → `NavKeyCanvasMove`(`CanvasMoveScreen`, 완료
  > 이벤트가 없는 스텁)에서 끝나고, 캔버스로 돌아가는 유일한 수단이 `onBack`이라 뒤로 갈 때마다
  > 백스택이 하나씩 걷힌다. 즉 **캔버스를 새로 쌓아 이전 흐름 화면을 살려 두는 코드가 develop에
  > 없어**, 원 스펙의 안전 근거는 **아무 수정 없이 develop에서 참이다.** 위험이 사라진 게 아니라
  > **#290이 머지되는 순간 되돌아온다** — `CanvasToppingPlaceRoute`의 배치 완료 이펙트가
  > `popUpTo<NavKeyCanvasMain>()`(`Navigator.kt#popUpTo`, 이 라운드가 신설)을 쓰도록 바꾸는 것이
  > 처방이고, 이건 이 라운드가 아니라 #290 쪽이 짊어질 몫이다. 옛 브랜치는
  > `backup/segmentation-on-290`으로 로컬 보존돼 있다.
  > 🔧 **재정정(2026-08-20)** — #290이 머지돼(develop `f12870a8`) 위 위험이 예고대로 되돌아왔고,
  > **결국 이 라운드가 짊어졌다.** `refactor/segmentation-develop`을 develop `750cc2dd` 위로
  > 리베이스하면서 `1181eedf`가 그 처방을 그대로 적용했다(`goTo(NavKeyCanvasMain(groupId = 0L))` →
  > `popUpTo<NavKeyCanvasMain>()`, OQ-P-238 ① 해소). "#290 쪽 몫"이라던 판단을 뒤집은 이유는
  > **위험이 이 브랜치의 캐시 정리에서 나오기 때문**이다 — 고칠 도구(`popUpTo`)도 이 브랜치에만 있다.
  > 즉 ③의 안전 근거는 **`refactor/segmentation-develop` 머지 시점부터 다시 참이고**, 그전까지
  > develop 단독으로는 거짓이다.
  > ✅ **그 시점이 왔다(2026-08-20, PR #309 — develop `cf357937`)** — 브랜치 팁이 충돌 해소 편집
  > 없이 그대로 머지돼 캐시 정리(`SegmentationCacheDir.kt`·`ClearSegmentationCacheUseCase`)와
  > 그 안전 근거를 참으로 만드는 되감기(`CanvasToppingPlaceRoute`의 `popUpTo<NavKeyCanvasMain>()`)가
  > **한 커밋 안에서 함께 develop 사실이 됐다.** ③은 이제 브랜치 조건 없이 닫힌다. ①(재시도 동선)과
  > 카메라 캐시(`FileCameraCacheLocalDataSourceImpl`, 정리 경로 없음·초 단위 파일명 충돌)는 그대로다.
  > 📌 **①이 열려 있는 자리가 바뀌었다(2026-08-22, PR #311)** — 실패를 담던 **에러 화면 자체가
  > 없어졌다**. `SegmentationErrorScreen`·`SegmentationLoadingScreen`이 지워지고 `SegmentationState.isError`가
  > `SegmentationEffect.ShowError` 1회성 이펙트 + 공통 에러 토스트로 바뀌었으며, 로딩은
  > `YGScaffoldV2(isLoading = …)` 오버레이가 받는다. **코드가 든 근거가 정확히 ①이다** — 재시도 동선이
  > 없는 실패를 상태로 붙들어 두면 영영 걷히지 않는 화면이 되니 한 번 알리고 끝낸다는 것이고,
  > 실패해도 원본 사진이 그대로 남아 사용자가 뒤로 가 다른 사진을 고를 수 있다. 즉 **재시도가 없다는
  > 사실은 그대로이되, 없는 재시도를 기다리는 화면이 사라졌다.** 재시도 버튼이 생기면 이 결정을
  > 되돌려야 한다 — `ModuleNotReady`는 여전히 "잠시 후 다시"라고 안내하면서 다시 시도할 수단이 없다.
- **해소 메모**: 정식(GA) 승급 시 버전 고정·문서 갱신. ③ 캐시 정리는 위에서 해소됨 —
  [ADR-0012](../adr/0012-mlkit-subject-segmentation.md) As-built 절에 반영 완료. ①(재시도 동선)은
  실행을 `init`에서 꺼내는 구조 변경과 재시도 버튼 디자인이 확정돼야 다룰 수 있고, 이제는
  **실패 표현을 이펙트에서 상태로 되돌리는 일**까지 함께 걸린다(위 2026-08-22 정정).

### [2026-07-12] 세그멘테이션 예외 처리 불일치
- **ID**: OQ-P-004
- **출처**: `data`의 `ImageSegmentationRepositoryImpl.segmentImage` — `Result<SegmentationResult>`/`SegmentationException` 패턴을 쓰면서도 `foregroundConfidenceMask`가 null이면 `error("...")`(raw `IllegalStateException`)로 throw. Result로 감싸지 않아 호출부(effect→Toast)가 못 잡을 수 있음.
- **항목**: ① `Tasks.await` 예외를 `SegmentationException`으로 통합할지, ② null 마스크를 `Result.failure`로 바꿀지.
- **상태**: 해소됨 (① **PR #221 develop 머지, 2026-08-14** — `toSegmentationException()`이 `ExecutionException`을 한 겹 벗겨 `MlKitException.UNAVAILABLE`이면 `ModuleNotReady`, 그 외는 `Process`로 매핑하고 `Result.failure`로 반환한다. / ② **PR #309 develop 머지, 2026-08-20** — `foregroundConfidenceMask == null`이 더는 `error("…")` raw throw가 아니라 `Result.failure(SegmentationException.Process)`를 탄다)
- **해소 메모**: ②는 `segmentImage`가 픽셀 마스킹을 `SegmentationMask.kt` 의 순수 함수(당시 `maskSubjectPixels`, 현재 `maskSubjectAlpha`)로 뽑아내며 함께 정리됐다 — [ADR-0012](../adr/0012-mlkit-subject-segmentation.md) As-built 절과 [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)에 반영 완료.
  **정확히는 같은 라운드 안에서 두 단계였다** — 위 커밋은 null 마스크·마스크 크기 불일치 두 경로만
  `Result`로 접었고, 같은 `withContext` 블록의 세 번째 경로(`saveToCacheAsPng`의 `IOException`)는
  여전히 새어나가고 있었다. 그 경로는 라운드 막바지 리뷰가 별도로 잡아 `try`로 마저 감쌌다 —
  블록 전체가 방어된 것은 그 시점부터다.
  > 🔧 **정정(2026-08-19)** — `refactor/segmentation-logic`(PR #290을 로컬 머지해 얹은 브랜치)은
  > 그 자체로 머지될 수 없어 plain develop 위 `refactor/segmentation-develop`로 다시 만들어졌다.
  > 위에서 닫힌 것으로 적은 동작(null 마스크·크기 불일치 `Result.failure`, 저장 경로 `try` 방어)은
  > 새 브랜치의 `ImageSegmentationRepositoryImpl.kt#segmentImage`에서도 그대로 확인된다 — **해소
  > 판정 자체는 안 바뀐다.** 브랜치명만 정정한다.
  > ✅ **develop 머지됨(2026-08-20, PR #309)** — 위 동작이 이제 브랜치가 아니라 develop 코드다.
  > 같은 머지가 `DecodeImageUseCase`도 `Result`로 넓혔다 — 스펙이 기각했던 대안인데, 두 호출부가
  > 쓰던 stdlib `runCatching`이 `CancellationException`까지 삼켜 **떠난 화면이 자기를 "디코드 실패"로
  > 보고하던 것**이 뒤집은 근거다. 리포지토리 계약(`decodeImage`는 던진다)은 그대로이고 감싸는 자리가
  > UseCase 하나로 모였다.

### [2026-07-12] 디자인시스템 컴포넌트 컨벤션 분기
- **ID**: OQ-P-005
- **출처 A**: `component/ygbutton`·`ygiconbutton`·`ygactionitem` — 컴포넌트별 폴더 + `@Preview`/`YGCustomTheme`(+`PreviewParameterProvider`) 프리뷰.
- **출처 B**: `component/textfield`·`etc` — 그룹 폴더 + `@YGPreview`/`PreviewBox` 프리뷰.
- **항목**: ① 패키지 네이밍(컴포넌트별 vs 그룹 폴더) 표준, ② 프리뷰 방식(`@YGPreview`/`PreviewBox` vs `@Preview`/`PreviewParameterProvider`) 표준.
- **상태**: 부분 해소 (② 프리뷰 방식 — **#158 develop 머지(2026-07-19)로 해소**. ① 패키지 네이밍은 잔존 미해결.)
- **해소 메모**: ② 프리뷰 방식 — 리팩터([designsystem-preview-migration 스펙](../superpowers/specs/archive/2026-07-18-designsystem-preview-migration.md)/[plan](../superpowers/plans/archive/2026-07-18-designsystem-preview-migration.md))로 컴포넌트 프리뷰 전부 `@YGPreview`+`PreviewBox` 전환, **PR #158 develop 머지 완료**(`ce4e9b8`). [design-system](../architecture/design-system.md) 프리뷰 노트 "표준 통일 완료"로 갱신함. ① 패키지 네이밍 표준 확정 시 "컴포넌트 작성 규약"에 반영하고 기존 컴포넌트 정리(YGColorChip 패키지 불일치 포함).

### [2026-07-13] design-system.md가 develop 미머지 브랜치 작업을 구현됨으로 기술
- **ID**: OQ-P-006
- **출처**: 문서가 일부 심볼을 구현됨으로 기술하나 `origin/develop`에 부재. `YGListItem`·`YGHorizontalDivider`(`component/etc/`, design-system.md 인벤토리)는 브랜치 `feature/#136-etc-component`에만 존재. (`YGModalPopup`은 `feature/#135-modal-component`에만 — 아직 인벤토리 미기재.)
- **항목**: ① 문서 기준선을 develop로 볼지(파르페 규율 "코드>문서, drift 금지"), ② 미머지 항목을 "머지 예정/브랜치" 마커로 남길지 인벤토리에서 잠정 뺄지.
- **상태**: 해소됨
- **해소 메모**: clickable 유틸(`clickableYG`·`ygDimRipple`·`ygScaleRipple`)은 **#94 develop 머지(#143)로 해소**(2026-07-15). **#136(etc: YGListItem·YGHorizontalDivider·YGActionItem·YGDangerZone·YGInviteCard)은 PR #148, #135(modal: YGModalPopup)은 PR #151로 2026-07-18 기준선 점검 시 develop 머지 확인** → 잔여 해소. design-system.md 인벤토리에 전 컴포넌트 등록·"미머지" 마커 제거 완료.

### [2026-07-14] clickable 유틸이 `core:util:android`로 이동 — ripple 색 테마 비의존
- **ID**: OQ-P-007
- **출처**: `core:util:android clickable/`(#94에서 `core:designsystem`→이동). `YGDimRipple`의 기본색이 `YGAtomicColors.Gray.Gray900`(테마)에서 리터럴 `YGDimRippleColor = Color(0xFF29292C)`로 바뀜 — util:android가 `core:designsystem` 비의존이라 테마 색을 못 읽음.
- **항목**: ① ripple 색 시맨틱 토큰화를 어떻게 할지(호출측 designsystem 컴포넌트가 `color` 주입 vs util 잔류 리터럴), ② `core:util:android`가 Compose UI(`parfait.jetpack.compose` 플러그인 + material-ripple/animation)를 갖게 된 레이어 성격 변화 — util 모듈에 UI clickable/ripple을 두는 게 맞는지(대안: 별도 `core:ui`/designsystem 잔류). 결정되면 ADR 검토.
- **상태**: 미해결 (이동·#94 develop 머지(#143, 2026-07-15) 완료, 레이어·토큰 방침 미확정)
- **해소 메모**: 색 토큰 규칙 확정 시 [[design-system]] 규약과 정합. 레이어 방침 확정 시 module-structure/ADR 반영.

### [2026-07-16] YGToggleButton 규약 이탈 — Colors 미분리·색 하드결선·하드코딩 치수
- **ID**: OQ-P-008
- **출처**: `component/ygtogglebutton/YGToggleButton.kt`(PR #142 develop 머지) — 다른 상호작용 컴포넌트(YGButton·YGChipButton)와 달리 Colors data class를 분리하지 않고 `YGAtomicColors.{Gray.White,Gray.Gray900,Transparency.Black50}`를 컴포저블 본문에서 `isSelected` 인라인 조건 분기(색 커스터마이즈 불가). 아이콘 크기 `24.dp` 리터럴(`SizeTokens` 미사용). 상호작용은 `clickable`+pressed 대신 `selectable`(selected 시맨틱).
- **항목**: ① 색을 `YGToggleButtonColors`(+Defaults) 패턴으로 분리할지(YGChipButton 선례), ② `24.dp`를 `SizeTokens`로 토큰화할지, ③ `selectable` 관용구를 선택형 컴포넌트 표준으로 채택할지.
- **상태**: 해소됨 (**PR #183 develop 머지, 2026-08-01** — 컴포넌트 삭제로 ①~③ 대상 코드가 사라짐)
- **해소 메모**: [미구현 컴포넌트 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md)이 대응 Figma 원본 없음·실화면 미사용을 근거로 삭제를 정했고(대체물 `YGEditButton` 신설), `component/ygtogglebutton/` 2파일 + `:app-preview` 잔재 4곳이 #183로 develop에서 제거됐다. [design-system](../architecture/design-system.md) 인벤토리·원자색 목록·pressed 관용구 예외에서도 걷어냈다. 단 "Colors 분리 조건" 자체는 [2026-07-30 신규 버튼군 항목](#2026-07-30-신규-버튼군이-colors-data-class를-분리하지-않음--규약-적용-조건-미정)으로 이어진다.

### [2026-07-18] YGColorChip 패키지↔폴더 불일치
- **ID**: OQ-P-009
- **출처**: `component/ygcolorchip/` — `YGColorChip.kt`·`YGColorChipPreviewData.kt`는 `package …component.ygchip` 선언, `YGColorChipType.kt`만 `package …component.ygcolorchip`. 폴더는 `ygcolorchip/`인데 패키지가 둘로 갈림.
- **항목**: 패키지를 폴더명(`ygcolorchip`)으로 통일할지(권장), 폴더를 패키지명(`ygchip`)에 맞출지.
- **상태**: 해소됨 (**PR #165 develop 머지, 2026-07-31** — 권장안대로 폴더명 `ygcolorchip`으로 통일)
- **해소 메모**: #165(개명 `YGColorChip`→`YGNametagChip` + `YGUserChip`·`YGChipColorIndicator` 신설)에서 패키지 선언이 전 파일 `…component.ygcolorchip`으로 정리됨. [design-system](../architecture/design-system.md) 인벤토리·과도기 마커에서 "패키지 불일치" 제거함. [2026-07-12 컨벤션 분기](#2026-07-12-디자인시스템-컴포넌트-컨벤션-분기) ①(컴포넌트별 vs 그룹 폴더 혼재)은 별개로 잔존.

### [2026-07-18] 네임태그 컬러칩 타입 개수 — 코드 14종 vs 정책 12종
- **ID**: OQ-P-010
- **출처**: `component/ygcolorchip/YGColorChipType.kt` — `NametagChip1`~`NametagChip13` + `NametagChipPlus` = **14종**(숫자 13 + Plus). 위키 정책 [[nametag-chip]]([[S-101-프로필-닉네임-컬러-규칙-v0.3]])은 **Nametag-Chip 12종**으로 기술. **#165(2026-07-31 머지)에서 `NametagChipPlus`의 용도가 코드 주석으로 확정**됐다(멤버 5명 이상일 때의 "+" 칩 = 색 타입이 아니라 접기 표시) — 즉 정책 대응 색 타입은 13종이고 정책은 12종이라 **숫자 타입 1종 초과가 실질 쟁점**으로 좁혀졌다.
- **항목**: ① 실제 색 매핑이 12종인지 13종인지 확정(`Plus`는 집계 표시용으로 제외), ② 코드↔정책 중 어느 쪽이 SoT인지(원칙: 코드>정책, 단 색 규칙은 디자인 정책 소관). 위키 정책 재확인 필요.
- **상태**: 해소됨 (2026-08-13, PR #223 develop 머지 — 코드가 12종+Plus)
- **해소 메모**: **Figma 컴포넌트셋 `144:5415`가 정본이었고, 코드 쪽 결함 2건이 원인이었다.** ① `NametagChip11`이 `NametagChip3`과 fill/stroke/text 3색이 전부 같은 **완전 중복**이라 뒤 항목이 한 칸씩 밀려 있었다(코드 12 = Figma 11, 코드 13 = Figma 12). ② `NametagChip9`의 `textColor`가 테두리색과 같은 `Cherry50`이었다(Figma는 `Pudding500`) — 민트 배경에 글자가 묻힌다. S-101 라운드에서 중복을 삭제하고 재번호해 **`NametagChip1`~`NametagChip12` + `NametagChipPlus`** 로 정렬했고 9번 글자색을 정정했다. 즉 정책 12종이 맞고 코드가 13종이었던 것이며, 위키 [[nametag-chip]]·[[S-101-프로필-닉네임-컬러-규칙-v0.3]]은 **수정 불필요**다. 사용처는 프리뷰 데이터와 `app-preview` 갤러리 1곳뿐이었고, 갤러리는 같은 색을 계속 가리키도록 `NametagChip12` → `NametagChip11`로 함께 옮겨 렌더가 변하지 않는다. 상세는 [s101 스펙](../superpowers/specs/archive/2026-08-07-s101-group-side-menu.md) "`YGColorChipType` 드리프트 수정" 절.
  > ✅ **머지됨(2026-08-13, PR #223).** [ygcolorchip 스펙](../superpowers/specs/archive/2026-07-18-ygcolorchip.md) 타입 표·주의 절과 [design-system](../architecture/design-system.md) 인벤토리를 12종+Plus로 정리했다. 다만 **스펙의 영향 범위 서술이 틀렸던 것도 이때 드러났다** — 사용처가 "프리뷰 데이터뿐"이 아니라 `app-preview` `YGTopBarPreviewScreen.MemberListSample`도 포함이었고, 재번호와 함께 `NametagChip12` → `NametagChip11`로 옮겨 렌더는 불변이다. 같은 PR이 **첫 화면 소비처**(S-101 `GroupMemberList`)도 열었는데 타입 배정이 목록 인덱스 순환 mock이라 위키 [[nametag-chip]]의 "타입은 유저별 고정"은 여전히 미구현이다.

### [2026-07-18] YGDateButton clickableYG 미사용 — 스로틀 규약 이탈
- **ID**: OQ-P-011
- **출처**: `component/ygdatebutton/YGDateButton.kt` — 클릭을 표준 `Modifier.clickable(indication = null)` + `semantics { role = Role.Button }`로 직접 구현. 다른 상호작용형 컴포넌트(YGButton·YGIconButton·YGActionItem·YGChipButton)가 쓰는 `core:util:android`의 중복 클릭 leading-throttle 유틸(`clickableYG`)을 안 씀 → 빠른 연타 방어 부재.
- **항목**: `YGDateButton`을 `clickableYG`(또는 변형)로 전환할지, 캘린더 셀은 스로틀 예외로 둘지.
- **상태**: **해소됨** (2026-08-17, 리네임/이관 #284)
- **해소 메모**: `clickableYGNoRipple`로 이관돼 스로틀을 탄다. **캘린더 셀을 예외로 두지 않는 쪽**으로 정해졌다 — 날짜 셀은 단일 선택이라 같은 날짜를 연타해도 멱등이고, 스로틀 게이트는 Modifier 노드마다 하나여서 다른 날짜로 옮겨 누르는 것은 막지 않는다. 규약은 [design-system](../architecture/design-system.md) clickable 절 "무리플이 기본이다"로 옮겨 적었다.

### [2026-07-18] FCM 토큰 서버 전송 미구현
- **ID**: OQ-P-012
- **출처**: `app/fcm/YGFirebaseMessagingService.kt` — `onNewToken`이 `TODO("서버에 FCM 토큰 전송")`. [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md).
- **항목**: 토큰 갱신 시 서버 등록 흐름(원격 API·재시도·로그인 연계). 원격 네트워킹 자체가 후속 과제([data-layer](../architecture/data-layer.md)).
- **상태**: 해소됨 (**PR #325 develop 머지, 2026-08-22** — 구현이 아니라 **대상 소멸**로 닫혔다)
- **해소 메모**: FCM 자체가 걷혔다 — `YGFirebaseMessagingService`·`app/Logger.kt`·`MainActivity`의
  토큰 조회와 알림 권한·채널·`firebase-messaging` 의존이 전부 사라져 `onNewToken`이라는 자리가 없다
  ([ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 철회 정정). **원격 연동은 그 사이 준비됐는데도
  이 항목이 그쪽으로 닫히지 않았다는 점이 요지다** — 토큰을 보낼 곳이 생긴 뒤에도 아무도 결선하지
  않았고, 결선 없는 껍데기가 첫 실행마다 알림 권한을 묻는 대가만 물리고 있었다. 푸시를 실제로 붙일
  때 토큰 라이프사이클은 **새 결정**으로 다시 연다(권한을 언제 묻는가가 함께 걸린다).

### [2026-07-18] `analytics` 패키지가 순수 로깅만 — 이름/기능 범위 불일치
- **ID**: OQ-P-013
- **출처**: `core:util:jvm`의 `analytics` 패키지에 `Logger`/`Loggers`/`KermitLoggerImpl`/`LoggerInitializer`가 있으나 실제 애널리틱스(이벤트 전송·Firebase Analytics 연동)는 없음. [ADR-0014](../adr/0014-logging-abstraction-kermit.md).
- **항목**: ① 릴리즈 로그 라이터 정책(로그 억제·Crashlytics 연동)을 `LoggerInitializer`에 둘지, ② `analytics` 패키지에 실제 이벤트 트래킹을 붙일지 패키지명을 `logging`으로 좁힐지.
- **상태**: 미해결
- **해소 메모**: 방침 확정 시 [ADR-0014](../adr/0014-logging-abstraction-kermit.md) 본문·`LoggerInitializer` 갱신.

### [2026-07-18] YGAtomicColors public 전환 — 시맨틱 우선 원칙 실질 이탈
- **ID**: OQ-P-014
- **출처**: `theme/colors/YGAtomicColors.kt` — `internal object YGAtomicColors` → `object YGAtomicColors`(public) 변경. **PR #158(`refactor/design-system-preview`) develop 머지 완료**(2026-07-19, `ce4e9b8`).
- **배경**: 디자인이 GUI에서 시맨틱(`YGColorScheme`) 개념을 쓰지 않고 원자 색을 그대로 끌고 가 사용 → 컴포넌트·피처가 원자 색 직접 참조하는 게 현실. `internal` 유지가 외부 모듈 사용을 막아 불가피하게 public 전환.
- **항목**: ① [ADR-0010](../adr/0010-custom-compositionlocal-theme.md) "컴포넌트는 시맨틱을 읽는다" 원칙을 폐기/완화할지(원자 색이 실질 SoT), ② [design-system](../architecture/design-system.md) "원자 색 직접 참조 금지 원칙" 서술 개정, ③ 시맨틱 레이어(`YGColorScheme`/`YGSemanticColorDefaults`)를 유지할지 걷어낼지, ④ 방향 전환을 신규 ADR로 남길지 ADR-0010 갱신할지.
- **상태**: 미해결 — **코드는 머지됨(public 확정)**, 그러나 원칙 문서화(①~④) 미결. design-system·ADR-0010에는 "머지됨+원칙 이탈" 마커 반영했으나 **방향 전환 ADR 미작성**.
- **해소 메모**: 원칙 결정 시 신규 ADR로 "원자 색 직접 노출 채택" 기록 또는 ADR-0010 개정. 기존 [2026-07-10 YGButton 디자인 토큰](#2026-07-10-ygbutton-디자인-토큰-규칙-미확정) "시맨틱 정리" 방향과 상반 — 함께 재정리.

### [2026-07-20] ProfileCard 각짐 — `radius.none` 토큰 부재로 `RectangleShape` 직접 참조
- **ID**: OQ-P-015
- **출처**: `feature/app/setting/impl/component/ProfileCard.kt`(PR #160 develop 머지) — 배경·보더 both `shape = RectangleShape`(직접 참조). 설계([app-setting-s001 스펙](../superpowers/specs/archive/2026-07-19-app-setting-s001.md))는 `YGTheme.shapes.radius.none`을 전제했으나, 초기엔 해당 토큰이 develop 미머지라 우회.
- **항목**: ProfileCard의 `RectangleShape` 직접 참조를 `YGTheme.shapes.radius.none`으로 승격(그 스펙의 "각짐도 테마 경유" 원칙 정합).
- **상태**: 미해결 (코드 수정 대상 — **종속 해소**: `radius.none` 토큰이 [designsystem-radius-none-sync](../superpowers/specs/archive/2026-07-19-designsystem-radius-none-sync.md) PR #159로 2026-07-22 develop 머지됨. ProfileCard 코드 교체만 잔존, 이제 unblocked)
- **해소 메모**: ProfileCard `shape`를 `YGTheme.shapes.radius.none`으로 교체하고 이 항목 해소. [design-system](../architecture/design-system.md) radius 마커와 함께 정리.

### [2026-07-20] 화면 컨테이너(YGScreen/YGScaffold) 컨벤션 — ADR 미작성
- **ID**: OQ-P-016
- **출처**: `core:designsystem` `screen/`(`YGScreen`·`YGScaffold`·`YGScreenScope.OnBack`). 설계 [designsystem-ygscreen-scaffold 스펙](../superpowers/specs/archive/2026-07-20-designsystem-ygscreen-scaffold.md), architecture [design-system](../architecture/design-system.md) "화면 컨테이너"·[navigation-flow](../architecture/navigation-flow.md) 체크리스트에 컨벤션(YGScaffold=nav, YGScreen=화면 최외곽) 반영. **초안 `OnBackResult` 반환 강제 → `OnBack` @Composable(내부 BackHandler emit)로 전환**한 결정 근거는 스펙에만 있고 ADR 미작성.
- **항목**: ① 화면 컨테이너를 DS 레벨에서 제공하고 뒤로가기를 `YGScreenScope`로 노출하는 결정을 ADR로 남길지, ② `YGScreen`↔`YGScaffold` 미통합(`YGScaffold`는 `YGScreenScope`/OnBack 없음)을 통합할지 별도 유지할지 — 이 통합 방향이 정해져야 ADR 내용이 확정됨.
- **상태**: 보류 (**코드 develop 머지 확정 — PR #162, 2026-07-22 기준선 점검**. 스펙 `implemented`·archive 이동. 통합 방향 + ADR 작성만 잔존)
- **해소 메모**: 코드가 develop 머지되고 ①②가 정해지면 신규 ADR 작성(화면 컨테이너·뒤로가기 스코프 채택 근거) 후 [design-system](../architecture/design-system.md)·[navigation-flow](../architecture/navigation-flow.md)의 `related_adr`에 연결하고 이 항목 해소.

### [2026-07-22] YGDangerZone 피그마 델타 — 좌우 gap-5·고정폭 미반영
- **ID**: OQ-P-017
- **출처**: `component/ygdangerzone/YGDangerZone.kt`(PR #159 develop 머지) — 루트 modifier가 `dashedBorder().padding(vertical = padding2)` + `width(IntrinsicSize.Max)`. 피그마는 상하 padding-2 **+ 좌우 gap-5**, 폭 `Fixed 335`. 좌우 패딩·고정 폭 미반영 상태로 머지.
- **항목**: ① 좌우 패딩(gap-5)을 추가할지, ② 폭을 고정(335)으로 둘지 `IntrinsicSize.Max`(Hug) 유지할지 — 디자인 확인 필요.
- **상태**: 미해결 (코드 수정 대상 — 디자인 확정 대기)
- **해소 메모**: 디자인 확정 시 코드 반영 후 [ygdangerzone-dashed 스펙](../superpowers/specs/archive/2026-07-19-ygdangerzone-dashed.md) "주의/열린 질문" 정리.

### [2026-07-23] 프리뷰 관용구 부분 회귀 — 신규 컴포넌트가 @YGPreview 표준 이탈
- **ID**: OQ-P-018
- **출처**: `component/ygalert/YGAlert.kt`·`component/ygtoast/YGToast.kt`(PR #149 develop 머지)·`component/ygcolorchip/YGUserChip.kt`(PR #165 develop 머지, 2026-07-31) — 프리뷰가 `@Preview` + `YGCustomTheme`. `component/ygtext/YGDate.kt`는 `@YGPreview`이나 `PreviewBox` 대신 `YGCustomTheme` 직접 래핑. #158로 "전 컴포넌트 `@YGPreview`+`PreviewBox` 통일"([2026-07-12 컨벤션 분기](#2026-07-12-디자인시스템-컴포넌트-컨벤션-분기) ② 해소)한 뒤 신규 컴포넌트에서 표준이 다시 갈라짐. 같은 #165의 `YGChipColorIndicator`·`YGNametagChip`은 표준을 따르므로 **같은 PR 안에서도 갈린다**.
- **항목**: 신규 컴포넌트 프리뷰를 `@YGPreview`+`PreviewBox`로 정렬할지(권장), 프리뷰 표준을 강제할 방법(리뷰 체크리스트·lint)이 필요한지.
- **상태**: 미해결 (코드 수정 대상)
- **해소 메모**: 정렬 시 [design-system](../architecture/design-system.md) "프리뷰 방식" 마커를 "통일"로 되돌리고 이 항목 해소. [2026-07-12 컨벤션 분기](#2026-07-12-디자인시스템-컴포넌트-컨벤션-분기) ②와 함께 관리.

### [2026-07-26] 문자열 리소스화 부분 적용 — 잔존 하드코딩·domain 표시문자열
- **ID**: OQ-P-019
- **출처**: PR #166(`feature/intro/impl`·`feature/groups/enter/impl` `strings.xml` 신설)로 TermAgree·GroupNickName·GroupInviteCode 화면 정적 라벨은 리소스화됐으나, ① `feature/intro/impl`의 `TermContent.kt#TERM_CONTENT_LIST` 약관 항목 title이 코틀린 리터럴로 잔존, ② `domain`의 `InviteCodeResult`가 `errorMessage: String?`로 **표시 문자열을 도메인이 보유** — [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)이 `NicknameResult`에서 걷어낸 패턴과 동일, ③ `feature/groups/canvas/impl`의 `CanvasImageAddScreen` 등 미착수 화면은 리터럴 그대로.
- **항목**: ① 정적 라벨 = `strings.xml` 관용구를 전 feature 모듈 규약으로 문서화할지(현재는 각 plan에만 기술, architecture 미기재), ② `InviteCodeResult`를 sealed + `core:ui` 매핑(ADR-0016 패턴)으로 정렬할지, ③ 약관 항목 title 리소스화 여부(랜딩 URL TODO와 함께 처리 후보).
- **상태**: 해소됨 (2026-08-15 — ①규약 문서화(2026-07-29) · ②`InviteCodeResult` 삭제(PR #244) · ③약관 title·미착수 화면 리터럴(PR #199·#242) 전부. 아래 마커는 이력)
  > ✅ **카메라·갤러리는 규약을 따름(2026-08-01, PR #182)** — `feature/camera/impl`·`feature/gallery/impl`에 `strings.xml`이 신설되고 권한·확인 화면 라벨이 전부 `stringResource`로 갔다. **예외 1건**: `CustomGalleryPickerScreen`의 빈 상태 문구가 코틀린 리터럴로 남았다(같은 화면의 다른 문구는 리소스) → 아래 [갤러리 빈 상태 항목](#2026-08-01-갤러리-빈-상태-그래픽이-상시-노출되고-문구가-리터럴)에서 함께 추적.
  > 📌 **신규 화면이 규약을 안 따름(2026-08-01, PR #173)** — G-001 `GroupListScreen`·`GroupListAddGroupScreen`의 라벨 3종("그룹 추가하기"·"그룹 만들기"·"그룹 들어가기")이 코틀린 리터럴이고, 코드 주석은 `Todo : core:ui 에 string resource 로 분리`라고 적는다. 화면 전용 정적 라벨은 **feature `strings.xml`**이 규약(공유 문구만 `core:ui`)이라 주석의 목적지부터 규약과 어긋난다. 규약이 문서에만 있고 코드 리뷰에서 안 걸린다는 신호다.
  > ✅ **그 3종은 해소됨(2026-08-04, PR #189 chore)** — `feature/groups/list/impl` `strings.xml`이 신설되고 `group_add`·`group_create`·`group_enter`로 옮겨졌다. 주석이 가리키던 `core:ui`가 아니라 **규약대로 feature 모듈**에 들어갔다. 잔존은 여전히 ②`InviteCodeResult`·③ 약관 title·미착수 화면(캔버스 등) 리터럴이다.
  > ✅ **A-002도 규약을 따름(2026-08-11, PR #218)** — `feature/login/impl` `strings.xml`이 신설되고 온보딩 설명 3종·카카오 버튼 라벨·`contentDescription`이 전부 `stringResource`로 갔다. 화면 소유 모듈에 둔 배치도 규약대로다. 남은 리터럴은 여전히 ②`InviteCodeResult`·③ 약관 title·미착수 화면(캔버스 등)이다.
  > ✅ **캔버스 화면도 규약을 따름(2026-08-11, PR #199)** — ③이 대표 사례로 들던 `feature/groups/canvas/impl`의 `CanvasImageAddScreen` 리터럴이 사라졌다. 같은 모듈에 `strings.xml`이 신설되고 빈 캔버스 안내·메뉴 라벨 4종·`+N` 포맷이 전부 `stringResource`다. **남은 리터럴은 ②`InviteCodeResult`·③ 약관 title**이고, 미착수 화면 목록에서 캔버스는 빠진다. 다만 같은 화면의 **mock 그룹명**은 ViewModel 코틀린 리터럴로 남았다(문자열 리소스화 대상이 아니라 데이터 미결선 — [2026-08-12] mock 항목).
  > ✅ **갤러리 예외 1건도 해소됨(2026-08-04, PR #191)** — 빈 상태 문구가 `feature/gallery/impl` `strings.xml`로 갔고, 같은 PR이 추가한 헤더·재선택 버튼·가이드 토스트 문구도 전부 리소스다. 다만 **가이드 토스트 문구가 카메라 것과 문자 그대로 같은데 두 모듈에 각각 정의**돼 아래 [중복 정의 항목](#2026-08-04-가이드-토스트-문구가-카메라갤러리-두-모듈에-중복-정의)으로 갈라졌다.
  > ✅ **S-101·설정 팝업도 규약을 따름(2026-08-13, PR #223·#225)** — `feature/groups/setting/impl` `strings.xml`이 신설되고 라벨·초대 문구 템플릿·팝업 문구가 전부 `stringResource`이며, `feature/app/setting/impl` `strings.xml`에 탈퇴 팝업 문구 4종이 추가됐다. 유효성 에러 문구는 공유 문구라 `core:ui` 소유 그대로다 — **①의 "화면 전용은 feature / 공유는 core:ui" 규약이 문서와 코드 양쪽에서 지켜진 라운드**다. 남은 리터럴은 여전히 ②`InviteCodeResult`·③ 약관 title.
  > 📌 **②가 오히려 커졌다(2026-08-12, PR #224)** — `InviteCodeResult`에 `groupName: String`이 추가됐고 그 값도 `CheckInviteCodeValidUseCase` 안 **한국어 리터럴 mock**이다(확인 모달 제목에 들어간다). 즉 domain이 들고 있는 표시성 문자열이 하나 더 늘었다. 화면 정적 라벨은 모달 문구까지 전부 `feature/groups/enter/impl` `strings.xml`이라 규약을 지키는데, domain 경유 문자열만 예외로 남는다 → [a004 스펙](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md).
  > ✅ **②③ 둘 다 해소됨(2026-08-15, PR #242·#244)** — ②는 실서버 결선이 **`InviteCodeResult` 자체를 삭제**하며 닫혔다(실패 사유는 feature 로컬 `InviteCodeError` enum + 화면 `toStringResource()`). ③은 약관 목록이 서버에서 오면서 `TermContent.kt#TERM_CONTENT_LIST`가 통째로 삭제돼 title·랜딩 URL 리터럴이 함께 사라졌다. **이로써 이 항목의 세 갈래가 전부 닫힌다.** 다만 실패 문구 enum이 A-004·S-102 두 모듈에 각각 생겨 문구가 겹치기 시작했다 → OQ-P-167·[ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md) as-built.
- **해소 메모**: ① 화면 전용 라벨=feature `strings.xml` / 공유 문구=`core:ui` `strings.xml` / domain 문자열 미보유 규약을 module-structure에 명시(#179가 `NickNameResult`의 domain 문자열을 걷어내 선례 확정). ②는 A-004 실연동(PR #244)이 모델째 걷어내며 닫혔고, ③은 약관 서버 결선(PR #242)이 닫았다. 반영처: [a004 스펙](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md)·[intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md)·[api/policy.md](../api/policy.md).

### [2026-07-27] Toast·Alert 호스트 노출 애니메이션이 동작하지 않음
- **ID**: OQ-P-020
- **출처**: `component/ygtoast/YGToastPolicy.kt#YGToastHost`·`component/ygalert/YGAlertPolicy.kt#YGAlertHost` — `AnimatedVisibility`가 `visible = true`인 상태로 최초 컴포즈돼 입장 transition이 돌지 않고(`updateTransition`의 `currentState == targetState`), 퇴장은 `setVisible(false)` 직후 같은 프레임에 목록에서 제거된다(Alert은 `clearAlert()`로 즉시 해체). 결과적으로 `YGToastItem.visible`·`YGAlertItem.visible`·`setVisible()`·양쪽 `exit =` 인자가 모두 死코드. [텍스트 영역 sync 스펙](../superpowers/specs/archive/2026-07-27-designsystem-text-component-sync.md)의 갤러리 화면이 두 호스트를 처음 실행시키면서 최종 리뷰에서 드러남.
- **항목**: ① 입장은 `MutableTransitionState(false).apply { targetState = true }`로, 퇴장은 제거 전 `delay(ANIMATION_DURATION)`로 살릴지, ② 아니면 애니메이션 의도를 접고 `visible`·`setVisible`·`exit` 死코드를 걷어낼지.
- **상태**: 미해결
  > 📌 **실사용처 생김(2026-08-01, PR #182)** — C-101 카메라 진입 시 촬영 가이드 토스트가 `rememberYGToastPolicy()`+`YGToastHost`로 뜬다(갤러리 showcase 밖 첫 실사용). 즉 이 결함이 이제 사용자 화면에서 재현된다.
- **해소 메모**: 위키 [[Toast-공통-정책]]은 노출 방식만 규정하고 애니메이션은 규정하지 않는다 — 디자인 의도 확인 후 ①/② 택일. 처리 시 sync 스펙의 "일치 확인" 정정 노트도 갱신.

### [2026-07-27] YGToastHost 다중 스택이 겹쳐 그려짐
- **ID**: OQ-P-021
- **출처**: `component/ygtoast/YGToastPolicy.kt#YGToastHost` — 컨테이너가 `Box`라 동시 노출된 토스트가 같은 원점에 겹쳐 그려진다. `Black75` 배경이 중첩돼 어두워지고 텍스트가 포개진다. `YGToastPolicy.show`가 `add(0, …)`로 앞에 넣으므로 최신 토스트가 오히려 아래 깔린다. 위키 [[Toast-공통-정책]]의 "나중 것을 이전 것 위에 노출(쌓임)"과 어긋난다.
- **항목**: `Box` → `Column(verticalArrangement = Arrangement.spacedBy(...))`로 바꿀지(1줄), 바꾼다면 최신 것이 위로 오도록 삽입 순서(`add(0, …)`)와 배치 방향이 맞는지 함께 확인.
- **상태**: 미해결
- **해소 메모**: 위 애니메이션 항목과 같은 파일이라 한 라운드에서 함께 처리하는 편이 낫다. 처리 시 sync 스펙 정정 노트 갱신.

### [2026-07-27] YGChipButton 세로 패딩 Figma 불일치
- **ID**: OQ-P-022
- **출처**: `component/ygchipbutton/YGChipButton.kt#YGChipButton` — 상/하 패딩이 `padding.padding3`. Figma `Button-Chip-Right`/`Button-Chip-Left` 변형은 세로 `padding-2`로, 칩 높이가 코드 39 vs 디자인 29로 어긋난다. [텍스트 영역 sync 스펙](../superpowers/specs/archive/2026-07-27-designsystem-text-component-sync.md) 대조 중 `YGAlert` 칩에서 발견.
- **항목**: ① 세로 패딩을 `padding2`로 내릴지, ② 내릴 경우 `YGAlert`·`YGTopBar` 등 공통 사용처의 높이 변화를 함께 검수할지.
- **상태**: 해소됨 (**PR #183 develop 머지, 2026-08-01** — 세로 패딩 `padding2` 반영)
- **해소 메모**: [버튼 영역 sync 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-component-sync.md) 드리프트 V2로 처리. `padding2`로 내리고 `YGAlert`·`YGTopBar` 높이 변화를 실기기 갤러리에서 확인했다. [design-system](../architecture/design-system.md) `YGChipButton` 노트에 반영.

### [2026-07-27] YGToast.Record 표시 문자열 하드코딩
- **ID**: OQ-P-023
- **출처**: `component/ygtoast/YGToast.kt#YGToast` — `Record` 분기가 `"님이 … 전에 쌓았어요"` 한국어 문구를 `core:designsystem` 안에 리터럴로 보유. 같은 sealed의 `InviteCode`·`Edit`·`Fail`은 완성 문장을 호출자가 주입받는 것과 규약이 어긋난다.
- **항목**: ① 조사·문구를 `strings.xml`(표현 계층)로 옮겨 [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md) 방향에 맞출지, ② 아니면 `Record`도 완성 문장 주입형으로 통일해 designsystem에서 문자열을 걷어낼지.
- **상태**: 미해결
- **해소 메모**: Toast 실사용처(캔버스 토핑 추가 알림) 구현 시점에 정리. 확정 시 [design-system](../architecture/design-system.md)에 "designsystem 컴포넌트는 표시 문자열을 보유하지 않는다" 규약으로 반영 검토.

### [2026-07-29] 유효성 결과 매핑 as-built가 ADR-0016 원안과 다름
- **ID**: OQ-P-024
- **출처**: `domain/model/NameValidResult.kt`·`domain/usecase/CheckNameValidUseCase.kt`·`feature/groups/enter/impl` `GroupNickNameViewModel`·`GroupCreateViewModel`·`core/ui/res/values/strings.xml`(PR #179 develop 머지). [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)은 `NicknameResult` sealed + `core:ui` `NicknameResult.Error.toStringResource()` 확장 + `core:ui`→`:domain` 의존을 결정했으나, 머지된 코드는 타입명이 `NameValidResult`(그룹명 공용)이고 **표시 매핑이 각 feature ViewModel의 `when`**(리소스 ID 산출)이며 `toStringResource` 확장·`core:ui`→`:domain` 의존은 없다. 에러 문자열 자체는 `core:ui` `strings.xml` 공용.
- **항목**: ① 매핑을 ADR 원안대로 `core:ui` 확장으로 끌어올려 VM 중복을 없앨지, ② as-built(VM이 `@StringRes` 산출)를 정본으로 ADR-0016을 개정할지. ②를 택하면 "UI State가 리소스 ID를 보유"가 규약이 되므로 [state-management](../architecture/state-management.md)에도 한 줄 필요.
- **상태**: 해소됨 (2026-08-13, PR #223 develop 머지 — ①로 결정·구현·머지 완료)
- **해소 메모**: **①(원안 수렴)을 택했다.** S-101 라운드(브랜치 `feature/#211-S-101-group-side-menu`)가 4번째 복제를 만들 자리에서 방향을 뒤집어 **4개 화면을 동시에 전환**했다. `core/ui/.../text/NameValidResultUiText.kt`에 `NameFieldType` enum + `@Composable NameValidResult.Error.toStringResource(fieldType)`를 신설하고 `core:ui` → `:domain` 의존을 추가했으며, UI State 4곳이 `NameValidResult.Error?`(도메인 의미)를 보유하고 화면이 렌더 시점에 변환한다. 원안과 갈리는 것은 타입명(`NameValidResult`, #179 as-built 유지)과 `fieldType` 파라미터 두 가지뿐 — 후자는 `SpaceAtEdge`·`EmptyString` 문구가 닉네임용/그룹명용으로 갈려서 필요하다. 부수 결과로 클릭 시점 검증 2곳의 `when`이 5분기 → 2분기로 줄고 `CoreR` 참조가 저장소에서 한 곳(`NameValidResultUiText.kt`)만 남았다. Compose stability 회귀 우려는 compose compiler report 실측으로 반증(`Uncertain(Error)` + `restartable skippable` 유지). [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)의 as-built 표를 "역사"로 정리하고 수렴본 표를 추가함. 상세는 [s101 스펙](../superpowers/specs/archive/2026-08-07-s101-group-side-menu.md) "유효성 표시 매핑" 절.
  > ✅ **머지됨(2026-08-13, PR #223).** [s002-account-info](../superpowers/specs/archive/2026-07-22-s002-account-info.md)·[s102](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md)·[a005](../superpowers/specs/archive/2026-07-29-a005-group-create.md) 세 스펙의 "VM에서 매핑" 서술을 as-built로 고쳤고, [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)·[state-management](../architecture/state-management.md)·[module-structure](../architecture/module-structure.md)에 머지 표기를 넣었다. State 필드도 함께 개명됐다(`errorMessageResId`/`groupNameErrorTextResId` → `nicknameError`/`groupNameError`). **남은 것 하나** — `core:ui`가 `:domain`을 `implementation`으로 갖는 탓에 public 확장의 리시버 타입이 숨은 의존이 됐다 → [2026-08-13] 항목.
  > 📌 **as-built 쪽으로 한 표 더 쌓임(2026-08-03)** — S-002 브랜치(`feature/#86-app-setting-account-info-screen`)가 원안대로 `NicknameResult` + `core:ui` `text/NickNameResultUiText.kt#toStringResource` 확장을 실제로 구현해 갖고 있었으나, develop rebase에서 **폐기하고 VM `when` 매핑으로 수렴**시켰다(develop이 이미 `NameValidResult`로 머지돼 타입·패키지가 충돌). 이로써 `toStringResource` 확장은 코드베이스 어디에도 남지 않고, VM 매핑 사례가 `GroupNickNameViewModel`·`GroupCreateViewModel`·`AccountInfoViewModel` **3건**이 됐다. 원안(①)으로 되돌리려면 이제 3곳을 동시에 고쳐야 한다 — 결정을 미룰수록 ① 비용이 오른다.
  > 📌 **2026-08-04 (PR #192) 머지 확정** — 위 3번째 사례(`AccountInfoViewModel`의 `NameValidResult` → `core:ui` `@StringRes` `when` 매핑)가 develop에 들어왔다. as-built 3건이 이제 전부 develop 코드다.

### [2026-07-29] A-005 그룹 생성 화면 진입 경로 부재
- **ID**: OQ-P-025
- **출처**: `feature/groups/enter/api/NavKeyGroupCreate.kt`·`feature/groups/enter/impl/navigation/EntryBuilder.kt#featureGroupCreateEntryBuilder`(PR #179 develop 머지) — entry·DI는 등록됐으나 `NavKeyGroupCreate`로 `goTo` 하는 호출자가 코드 전체에 없다. 직전 단계 후보인 `GroupNickNameRoute`의 `NavigateToNext`는 여전히 stub이고, A-005는 `nickName` 인자를 요구한다.
- **항목**: ① 그룹 참여(S-102)와 그룹 생성(A-005)의 진입 관계를 확정할지(기획상 참여 플로우 다음이 맞는지), ② 확정 시 `GroupNickNameRoute`에서 `navigator.goTo(NavKeyGroupCreate(nickName))` 결선.
- **상태**: **부분 해소** (② 결선됨 — 화면은 도달 가능해졌다. ① 진입 관계 확정과 인자 값의 출처는 잔존)
- **해소 메모**: 결선 후 [a005 스펙](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·[s102 스펙](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md)의 "다음 네비게이션 미구현" 항목을 함께 정리. 위키 [[기능정의서-v6]] 화면 흐름과 대조 필요.
  > 📌 **진입점 UI는 생겼고 결선만 남음(2026-08-01, PR #173)** — G-001 그룹 추가 오버레이의 "그룹 만들기"가 `GroupListSideEffect.NavigateToCreateGroup`을 발신하지만 Route 소비부가 `// Todo : navigator.goTo(NavKeyGroupCreate)` 주석이다. 같은 오버레이의 "그룹 들어가기"는 `NavKeyGroupInviteCode`로 실제 결선됐다. 남은 것은 `goTo` 한 줄과 `nickName` 인자 출처(A-005가 인자 있는 NavKey)다.
  > ✅ **② 결선됨(2026-08-10, PR #222)** — `GroupListRoute`가 `navigator.goTo(NavKeyGroupCreate(nickName = uiState.nickName))`를 부른다. 다만 **① 진입 관계는 그대로 미결**이고(기획상 참여 플로우 다음이 맞는지), 후보였던 `GroupNickNameRoute.NavigateToNext`는 여전히 stub이라 **A-005 진입점이 G-001 오버레이 하나뿐**이다. 또 넘기는 `nickName`이 `GroupListUiState` 기본값 mock 리터럴이라 **값의 출처는 안 정해졌다**([2026-08-07] mock 항목과 같은 뿌리).
  > ⚠️ **mock 인자가 서버로 나가기 시작했다(2026-08-15, PR #243)** — `CreateGroupUseCase`가 실서버를 타면서
  > 이 `nickName`이 `POST /api/parfait-groups`의 `groupNickname`으로 **실제 전송**된다. 표시용 mock이던
  > 값이 이제 서버에 저장되는 데이터다. 내 계정 조회(`member` 도메인, 표면만 있고 소비처 0)가 붙어야 닫힌다.
  > 📌 **①의 후보 흐름이 부정됐다(2026-08-12, PR #224)** — `GroupNickNameRoute.NavigateToNext`가 결선됐는데 목적지가 A-005가 **아니라 그룹 목록**이다. 즉 "참여(S-102) 다음이 생성(A-005)"이라는 당시 가정은 코드로 기각됐고, A-005 진입점은 앞으로도 G-001 오버레이 하나다. 남은 미결은 인자 `nickName`의 출처(여전히 mock)뿐이다. 대신 **복귀 목적지 자체가 위키 정본과 어긋나는** 새 쟁점이 생겼다 → [2026-08-12] 복귀 목적지 항목.

### [2026-07-29] `GroupCreateConfig`가 표시 관심사를 domain에 보유
- **ID**: OQ-P-026
- **출처**: `domain/model/GroupCreateConfig.kt`(PR #179 develop 머지) — 이름 길이 상한(정책값)과 함께 `GROUP_COLUMN_COUNT`(인원 선택 그리드 열 수)를 같은 객체에 둔다. 열 수는 화면 레이아웃 값이라 `domain`이 UI 결정을 들고 있는 형태다. `GROUP_COUNT_LIST`(1~12)는 정책값이라 domain이 맞다.
- **항목**: ① `GROUP_COLUMN_COUNT`를 화면(`GroupCreateScreen`)이나 `core:ui`로 내릴지, ② 객체명 `GroupCreateConfig`가 닉네임 상한(S-102·S-002 공용)까지 담는 게 맞는지 — 이름이 그룹 생성 전용처럼 읽힌다.
- **상태**: 미해결 (코드 수정 대상)
- **해소 메모**: 정리 시 [module-structure](../architecture/module-structure.md) domain 순수성 규칙과 정합 확인. 상한 상수의 단일 소유 자체는 유지(중복 정의 회귀 방지).

### [2026-07-29] `core:ui` 공용 UI 컴포넌트의 프리뷰·규약 범위 미정
- **ID**: OQ-P-027
- **출처**: `core/ui/VerticalGridLayout.kt`(PR #179 develop 머지) — 프리뷰가 `@Preview` + **public** 함수 + `Random` 색이고, `core:designsystem`의 `@YGPreview`+`PreviewBox`(private) 규약을 따르지 않는다. `core:ui`는 그동안 MVI 베이스만 있어 규약 대상이 아니었으나 공용 Compose 레이아웃이 들어오면서 경계가 모호해졌다.
- **항목**: ① 공용 UI 컴포넌트를 `core:ui`에 둘지 `core:designsystem`으로 옮길지, ② `core:ui`에도 프리뷰 규약(`@YGPreview`+`PreviewBox`, 프리뷰 함수 private)을 적용할지 — `core:ui`가 `core:designsystem`에 의존하는지부터 확인 필요.
- **상태**: 미해결
- **해소 메모**: 방침 확정 시 [module-structure](../architecture/module-structure.md) `core:ui` 행과 [design-system](../architecture/design-system.md) 프리뷰 규약 범위를 함께 갱신. [2026-07-23 프리뷰 관용구 부분 회귀](#2026-07-23-프리뷰-관용구-부분-회귀--신규-컴포넌트가-ygpreview-표준-이탈)와 함께 관리.

### [2026-07-30] 도메인 모델 `VO` 접미사 규약이 기존 명명과 갈림
- **ID**: OQ-P-028
- **출처**: `domain/model/TempVO.kt`·`data/source/temp/mapper/VOMapper.kt`·`data/source/temp/remote/TempRemoteDataSource.kt`(PR #174 develop 머지, 2026-08-01) — 원격 예시 세트가 도메인 모델을 `TempVO`로, 매퍼 파일을 `VOMapper.kt`로 명명한다. 기존 `domain.model`은 전부 무접미사(`SegmentationResult`·`GalleryImageGroup`·`InviteCodeResult`·`NameValidResult`·`DayWindow`)라 같은 패키지 안에서 규약이 둘이 된다.
- **항목**: ① 원격 유래 모델만 `…VO`를 쓸지(=출처를 이름에 남길지), ② 전부 무접미사로 통일할지, ③ 통일한다면 매퍼 파일명(`VOMapper.kt`)도 `<도메인>Mapper.kt` 등으로 맞출지.
- **상태**: 미해결 (**개명 비용이 커졌다** — 아래 참고)
  > 📌 **2026-08-06 (PR #197 머지) 갱신** — 예시 세트 `Temp*`는 삭제됐지만 규약을 정하기 전에 `VO` 접미사가 실제 도메인으로 퍼졌다(`KakaoLoginVO`·`AuthSessionVO`·`PolicyVO`·group 7종, mapper 파일도 도메인마다 `VOMapper.kt`). 반면 값 하나짜리 래퍼는 접미사 없는 value class(`GroupId`·`AccessToken`·`GroupName` 등 10종)라 **두 관례가 같은 패키지 트리에 공존**한다. "확정 전에 정하면 개명 비용 없음"이라는 전제는 지나갔다.
- **해소 메모**: 결정 후 [ADR-0017](../adr/0017-remote-network-datasource.md) "응답 → 도메인 매핑 위치" 조항과 [data-layer](../architecture/data-layer.md) "레이어 배치"·"응답 매핑", [data-network-setup 스펙](../superpowers/specs/archive/2026-07-26-data-network-setup.md)의 심볼명을 함께 맞춘다.

### [2026-07-30] 원격 DataSource가 도메인 모델을 직접 반환 — Repository 매핑 여지 없음
- **ID**: OQ-P-029
- **출처**: `data/source/temp/remote/TempRemoteDataSource.kt`(`Result<TempVO>` 반환)·`data/source/temp/mapper/VOMapper.kt`(PR #174 develop 머지, 2026-08-01) — [ADR-0017](../adr/0017-remote-network-datasource.md)이 data 전용 중간 모델을 기각하면서 변환이 DataSource 경계 1회로 고정됐다. `:data`→`:domain` 의존이라 레이어 역전은 아니나([ADR-0001](../adr/0001-layered-multi-module.md)), 로컬(DataStore·파일) DataSource들은 아직 이 규약의 적용 대상인지 명시되지 않았다.
- **항목**: ① 로컬 DataSource(`RecentImageLocalDataSource`·`FileRecentImageLocalDataSource` 등)도 "DataSource는 도메인 모델 반환" 규약에 편입할지, 아니면 원격에만 적용할지. ② 원격+로컬을 합성하는 Repository가 생길 때 변환 책임이 어디로 가는지(현재는 변환할 것이 남지 않음).
- **상태**: 미해결 (**쟁점이 실물이 됐다** — 아래 참고)
  > 📌 **2026-08-06 (PR #197 머지) 갱신** — 14 엔드포인트 원격 DataSource 전량이 도메인 모델을 반환하며 develop에 들어왔다(`AuthRemoteDataSource#loginWithKakao: Result<KakaoLoginVO>` 등). 즉 이 위에 Repository를 얹을 때 **변환할 것이 남지 않은 상태**로 결선 라운드를 맞는다 — Repository를 둘지 UseCase가 DataSource를 직접 쓸지가 아래 "API 표면 14 엔드포인트가 소비처 0건" 항목과 같은 결정이다.
- **해소 메모**: 확정 시 [data-layer](../architecture/data-layer.md) "신규 데이터 추가 체크리스트"에 DataSource 반환 타입 규칙으로 한 줄 고정.

### [2026-07-30] 사진 업로드 경로의 타임아웃 정책 미정
- **ID**: OQ-P-030
- **출처**: `data/di/NetworkModule.kt#provideOkHttpClient`(PR #174 develop 머지, 2026-08-01) — 단일 `OkHttpClient`가 connect/read/write 타임아웃을 모든 호출에 공통 적용하고 `callTimeout`은 설정하지 않는다(=전체 소요 무제한). 코드리뷰에서 30초가 과하다는 지적을 받아 값을 낮췄으나, 토핑 사진 업로드(누끼 PNG) API는 아직 없어 실제 전송·서버 처리 시간을 모른 채 정한 값이다. OkHttp의 read/write는 전체 전송 시간이 아니라 바이트 간 유휴 상한이라, 업로드가 느린 것 자체는 이 값으로 잡히지 않는다.
- **항목**: ① 업로드 API 확정 후 전체 소요 상한(`callTimeout`)을 둘지 — 두면 스피너·취소 UX와 값이 묶인다. ② 업로드 전용 `OkHttpClient`(`@Qualifier`)를 분리해 read/write만 늘릴지, 아니면 단일 클라이언트 값을 상향할지. ③ 실패 시 재시도(멱등성 확인 필요)를 어디에 둘지 — 인터셉터 vs 호출부.
- **상태**: 해소됨 (2026-08-20 develop 머지, PR #322)
  > ✅ **셋 다 정해졌다.** ① `callTimeout`을 **업로드 표면에만** 둔다 — `writeTimeout`은 바이트 사이
  > 유휴 상한이라 전송 전체가 느린 것을 못 잡는다. 메인·재발급 클라이언트는 종전대로 3종만 쓴다.
  > ② 업로드 전용 `OkHttpClient`(`@UploadClient`)를 **분리했다.** 예상대로 선택이 아니라 전제였다.
  > ③ 재시도는 인터셉터가 아니라 **호출부**다 — 실패하면 발급부터 전량 재시도한다
  > ([스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) "결정된 것"). 만료 판정을 따로 하지 않으므로
  > `expiresIn` 선행 조건도 함께 사라졌다. 대가는 고아 S3 객체이고 그 정리 경로 부재는 별개 미결이다.
  > 수치는 규율대로 문서에 적지 않는다 — 구조만 기록한다.
  > 📌 **전제가 사라졌다(2026-08-10, 서버 `5bb2a3a`)** — 업로드 API가 [api/image.md](../api/image.md)로 들어왔고, 형태가 예상과 다르다. **바이트가 서버를 지나지 않는다**(S3 presigned PUT 직접 업로드). 그래서 타임아웃 결정 대상이 `YG_BASE_URL` 호출이 아니라 **S3로 나가는 PUT**이다 — 이 요청은 Retrofit 서비스가 아니므로 ②의 "업로드 전용 `OkHttpClient` 분리"는 선택이 아니라 사실상 전제가 되고, ③ 재시도는 `expiresIn` 만료 시 URL 재발급이 선행돼야 한다(만료된 presigned URL은 재시도해도 실패한다). ①의 `callTimeout`도 서버 API가 아니라 이 PUT에 걸 값이다.
  > 📌 **클라이언트 분리의 진짜 강제 사유는 타임아웃이 아니다(2026-08-10 최종 코드리뷰).** `AuthInterceptor`가 `request.tag(Invocation::class.java)?.method()`로 **Retrofit 메서드 애노테이션**을 읽어 `@NoAuth`를 판정한다. 발급받은 `uploadUrl`로 raw OkHttp `Request`를 만들어 쏘면 `Invocation` 태그가 없어 `skipAuth = false`가 되고, 토큰이 있는 한 `Authorization: Bearer …`가 **무조건 붙는다.** presigned URL 요청에 이 헤더가 실리면 S3가 인증 수단 중복으로 거절한다. 즉 공유 `OkHttpClient`를 그대로 쓰면 업로드가 **아예 동작하지 않는다** — 분리는 성능 선택이 아니라 기능 전제다. `writeTimeout`이 이미지 업로드에 짧다는 것도 같은 결정에 묶인다.
- **해소 메모**: 업로드 엔드포인트 붙일 때 실측 후 결정하고 [ADR-0017](../adr/0017-remote-network-datasource.md) "로깅"·타임아웃 서술과 [data-layer](../architecture/data-layer.md) 네트워킹 섹션에 반영. 파르페 규율상 문서에 수치는 적지 않고 구조(클라이언트 분리 여부·callTimeout 유무)만 기록한다.

### [2026-07-30] Figma가 아이콘 tint 색을 노출하지 않아 대조 불가 — Button-Icon·Action-Item
- **ID**: OQ-P-031
- **출처**: `component/ygiconbutton/YGIconButton.kt#YGIconButton`·`component/ygactionitem/YGActionItem.kt#YGActionItem` — [버튼 영역 sync 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-component-sync.md) 대조 중 발견. Figma `Button-Icon`·`Action-Item`의 아이콘이 색을 포함한 래스터 에셋으로 내보내져 `get_design_context` 응답에 tint 값이 없다. 컨테이너·아이콘 프레임 크기는 대조됐으나 색 3상태(`YGIconButton`: 기본/pressed/disabled)는 코드 현행값을 근거 없이 유지한 상태다.
- **항목**: ① `YGIconButton` tint 3상태가 디자인 의도와 맞는지 디자이너 확인, ② `YGActionItem` 신설 아이콘의 tint를 텍스트 색과 함께 움직이게 한 이번 결정(pressed 시 함께 진해짐)이 맞는지 확인, ③ 디자인 쪽에서 아이콘을 벡터+변수 바인딩으로 바꿀 수 있는지(이후 sync 라운드의 대조 가능성 문제).
- **상태**: 미해결 (디자이너 확인 필요 — **관련 코드는 PR #183로 develop 머지, 2026-08-01**. 값은 현행 유지)
- **해소 메모**: 확인 후 값이 다르면 해당 컴포넌트 색 매핑을 고치고 위 스펙의 "일치 확인" 표를 갱신한다.
  > 📌 **미확인 잔존(2026-07-30)** — Figma `Button-Edit-Action`은 `Disabled`에서만 다른 아이콘 에셋을 쓴다(= 아이콘 색이 다를 가능성). 색을 읽을 수 없어 `YGEditActionButton`은 3상태 모두 `Gray.White` 고정으로 구현했다. 배경만 상태별로 바뀐다.
  > ⚠️ **실제 결함으로 드러남(2026-07-30)** — 신설 `YGCircleButton`을 "리소스 색 그대로" 방침으로 만들었더니 `Type=Secondary`(어두운 원)에서 아이콘이 배경에 묻혔다. 저장소 아이콘 드로어블이 **전부 `#000000`**이기 때문이다. Figma 스크린샷으로 Secondary 아이콘이 흰색임을 확인해 `YGCircleButtonType.iconTint`를 신설했다(머지 코드 기준 `Default`·`Small` = `Gray.Gray850`, `Secondary` = `Gray.White`). 즉 **어두운 배경 변형에는 tint 지정이 필수**다. `Default`·`Small`의 정확한 톤은 여전히 미확인(팔레트 값으로 근사).

### [2026-07-30] Button-Medium Transparency pressed 배경이 디자인 변수에 미바인딩
- **ID**: OQ-P-032
- **출처**: `component/ygbutton/YGButtonType.kt#YGButtonType.Medium.Transparency` — [버튼 영역 sync 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-component-sync.md) 드리프트 V4 처리 중 발견. default·disabled는 Figma가 `transparency/white-50` 변수를 쓰지만 pressed만 변수 없는 리터럴 색이다. 코드 쪽도 `YGAtomicColors.Transparency`에 대응 단계가 없어 `Gray.White.copy(alpha = …)`로 유지한다 — 즉 이 한 상태만 원자 팔레트 밖 값이다.
- **항목**: ① 디자인에서 pressed 값을 `transparency/*` 변수로 승격 요청할지, ② 승격 시 `YGAtomicColors.Transparency`에 대응 단계를 추가하고 `copy(alpha = …)` 리터럴을 걷어낼지.
- **상태**: 미해결 (디자인 토큰 쪽 선행 필요 — 대상 코드는 PR #183로 develop 머지, 2026-08-01)
- **해소 메모**: 토큰 확정 시 `YGAtomicColors.Transparency` 단계 추가 + `Medium.Transparency` 색 매핑 교체, [design-system](../architecture/design-system.md) 토큰 계층 표 갱신.

### [2026-07-30] 카메라 컨트롤 임시 구현체 잔존 — 셔터 구현이 두 곳에 공존
- **ID**: OQ-P-033
- **출처**: `feature/camera/impl` `component/controls/ShutterButton.kt`·`FlipCameraButton.kt`·`component/CameraControlComponent.kt` vs 신설 예정 `core/designsystem` `component/ygcamerashutter/YGCameraShutter.kt`([미구현 컴포넌트 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md)) — feature 쪽 셔터는 디자인 정본보다 큰 고정 크기 + `Color.Gray` 리터럴 테두리 + pressed 없음, flip 버튼은 이모지 문자 + `Color` 리터럴 배경, 취소는 맨 Material3 `TextButton`이다. 컴포넌트 스펙은 Figma 정본이 있는 `Camera-Shutter`만 designsystem에 만들고 화면 치환은 하지 않기로 정했다(작업자 결정) — 즉 셔터가 두 구현으로 공존한다.
- **항목**: ① 카메라 화면(C-101) 라운드에서 `ShutterButton`을 `YGCameraShutter`로 치환하고 feature 쪽을 지울지, ② flip 버튼이 Figma `Button-Circle` `Type=Small`(`ic_rotate`)에 대응하는지 화면 노드로 확인할지 — 컴포넌트 시트만으로는 단정할 수 없다, ③ 취소·줌 컨트롤의 Figma 대응을 찾을지.
- **상태**: 해소됨 (**PR #182 develop 머지, 2026-08-01** — ①②는 치환으로 닫힘, ③ 줌은 컨트롤 자체가 화면에서 빠져 [2026-08-01 줌 死코드 항목](#2026-08-01-카메라-줌-ui가-死코드로-남음)으로 넘어갔다)
- **해소 메모**: `component/controls/ShutterButton.kt`·`FlipCameraButton.kt`가 삭제되고 `CameraControlComponent`가 `YGCameraShutter` + `YGCircleButton`(플래시·전환) 조합으로 바뀌었다. 취소는 맨 `TextButton` 대신 상단 `YGCircleButton`(`ic_close`)이다. flip 아이콘은 `ic_reverse`·`YGCircleButtonType.Default`로 구현했다(Figma `Type=Small` 여부는 대조하지 않았고, 화면이 정본이 된 상태). [design-system](../architecture/design-system.md) 인벤토리에 화면 적용 줄 추가, 상세는 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md).

### [2026-07-30] Button-Edit-Action이 정수 토큰 재조립으로 2dp 커짐 + Small 테두리 소수 잔존
- **ID**: OQ-P-034
- **출처**: [미구현 컴포넌트 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) "치수 도출 원칙" — Figma `Button-Edit-Action`은 아이콘 프레임이 22이고 `SizeTokens`에 대응 스케일이 없다. 스펙은 `Size22`를 만들지 않고 `Size24`로 옮기기로 정했고, 그 결과 내부 원과 바깥 프레임이 각각 2dp 커진다. 또 `Button-Circle` `Type=Small`의 테두리는 재조회 후에도 소수(0.636)로 남아 1dp로 정규화한다.
  > ✅ **부분 해소(2026-07-30 재조회)** — `Button-Circle` `Type=Small`이 Figma에서 **정수 치수로 정리**됐다(내부 원 28 명시·아이콘 18·글리프 12·바깥 폭 44 명시, 구조도 "패딩 도출"에서 "지름 고정 + 중앙 아이콘"으로 바뀜). `SizeTokens.Size18` 추가를 합의해 Circle 3변형의 치수 오차는 없어졌다. 남은 것은 Edit-Action 2dp와 Small 테두리 두께다.
- **항목**: ① Edit-Action의 2dp 차이를 디자이너가 수용하는지, 아니면 Figma를 정수 치수(아이콘 24 또는 원 40)로 정리해줄 수 있는지, ② 수용도 정리도 안 되면 `Size22`를 스케일에 넣을지 해당 컴포넌트만 리터럴 dp를 허용할지, ③ Small 테두리 0.636을 1로 정리해줄 수 있는지.
- **상태**: 미해결 (의도된 절충 — 구현 완료, 정수 토큰으로 반영. PR #183 develop 머지, 2026-08-01)
- **해소 메모**: `YGEditActionButton`이 내부 `padding3` + 아이콘 `SizeTokens.Size24`, 바깥 `padding1` 래핑으로 구현됐다(2026-07-30). 확인 후 값이 바뀌면 해당 컴포넌트 치수와 스펙 "치수 도출 원칙" 표를 함께 고친다.

### [2026-07-30] Camera-Shutter에 바인딩된 Transparency.Black5의 용도 불명
- **ID**: OQ-P-035
- **출처**: Figma `Camera-Shutter` 노드의 디자인 변수 목록에 `Transparency/Black-5`가 잡히지만, 셔터는 흰 외곽 원 + 어두운 내부 원 두 도형으로만 보인다([미구현 컴포넌트 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md)). 두 원은 래스터 에셋으로 내보내져 코드 응답에 색·효과가 드러나지 않는다. 외곽 테두리나 그림자일 가능성이 있다.
- **항목**: ① `Black-5`가 외곽 원 테두리인지 그림자인지 디자이너 확인, ② 그림자라면 Compose에서 `shadow`로 재현할지(현재 designsystem에 그림자 관용구가 없다).
- **상태**: 미해결 (구현 완료 — 두 원만 그렸다. PR #183 develop 머지, 2026-08-01)
- **해소 메모**: `YGCameraShutter`가 흰 외곽 원 + `padding2` + 내부 `Size48` 원 2도형으로 구현됐다(2026-07-30). 확인 후 필요하면 테두리/그림자를 더하고 스펙 상태 표를 갱신.

### [2026-07-30] 신규 버튼군이 Colors data class를 분리하지 않음 — 규약 적용 조건 미정
- **ID**: OQ-P-036
- **출처**: [미구현 컴포넌트 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-missing-components.md) "Colors 분리 판단" — 신설 5종(`YGEditTabButton`·`YGEditButton`·`YGCircleButton`·`YGEditActionButton`·`YGCameraShutter`)은 색 주입 data class를 만들지 않고 변형 타입(`YGCircleButtonType`) 또는 컴포저블 본문에서 상태 분기한다. Figma가 색을 고정하고 주입 사용처가 없다는 판단이다. [design-system](../architecture/design-system.md) 컴포넌트 작성 규약은 `YGButton` 기준으로 "Colors data class 분리"를 적어 두었으므로 이 판단은 규약과 갈린다. 같은 이탈로 등록된 [2026-07-16 YGToggleButton 항목](#2026-07-16-ygtogglebutton-규약-이탈--colors-미분리색-하드결선하드코딩-치수)은 그 컴포넌트 삭제로 없어질 예정이다.
- **항목**: ① 규약을 "색 주입 요구가 있을 때만 Colors를 분리한다"로 다듬을지, ② 아니면 신규 5종도 일괄 분리해 규약을 그대로 지킬지(사용처가 없는 API가 5개 늘어난다).
- **상태**: 미해결 (구현 완료 — 5종 모두 미분리로 반영. PR #183 develop 머지, 2026-08-01)
- **해소 메모**: `YGCircleButton`만 변형 타입(`YGCircleButtonType`)이 색·아이콘 크기·tint·`paintsOuterCircle`을 들고 있고 나머지 4종은 컴포저블 본문 상태 분기다. 방침 확정 시 [design-system](../architecture/design-system.md) "컴포넌트 작성 규약"에 분리 조건을 한 줄로 고정한다.
  > 📌 **함께 정할 것(2026-08-01 머지 코드 확인)** — `YGCircleButtonType`은 `YGButtonType`과 달리 `@get:Composable`이 아니라 `@Immutable` + 평범한 `val`이다(테마 미경유·상수 직접 대입). 변형 타입이 토큰을 노출하는 방식이 두 가지로 갈렸으므로 Colors 분리 조건과 같은 줄에서 "변형 타입은 `@get:Composable`로 테마를 읽는다 / 상수 대입도 허용한다"를 함께 못박아야 한다.

### [2026-07-31] Grouptag-Chip 그레이 타입 타임스탬프 색 — 정책 문서 `White` vs Figma `Gray-200`
- **ID**: OQ-P-037
- **출처**: [grouptag-topping 스펙](../superpowers/specs/archive/2026-07-31-designsystem-grouptag-topping-components.md) 타입 매핑 표 — `YGGrouptagChipType.TYPE_7_8`(그레이)의 타임스탬프 색을 Figma 컴포넌트가 `Gray-200`으로 주는데, 정책 문서(S-101에서 분리된 그룹칩 Timestamp 컬러 규칙)의 표는 같은 자리를 `White`로 적는다. 나머지 5종(Cherry-100/200/300·Melon·Pudding)은 양쪽이 일치한다.
- **항목**: 어느 쪽이 정본인지. Figma가 맞으면 정책 문서를 정정해야 하고, 정책이 맞으면 코드와 Figma를 함께 고쳐야 한다.
- **상태**: 미해결 (구현은 Figma를 따라 `Gray.Gray200`으로 반영. PR #186 develop 머지, 2026-08-01)
- **해소 메모**: 정책 SoT는 위키이므로 위키 open-questions에도 같은 항목을 등록해야 한다(디자인 파일 ↔ 정책 문서 불일치라 구현 밖에서 결론이 나야 한다). 위키 등록은 develop 머지 후로 미뤘던 것이고, **#186 머지에 따라 2026-08-01 기준선 점검에서 위키에 등록 완료**했다(`wiki/synthesis/open-questions.md` 항목 + `wiki/concepts/nametag-chip.md` ② 표 ⚠️ 마커).

### [2026-07-31] `YGToppingGroupType`의 `TYPE_3_LEFT`·`TYPE_3_RIGHT`가 완전히 동일
- **ID**: OQ-P-038
- **출처**: [grouptag-topping 스펙](../superpowers/specs/archive/2026-07-31-designsystem-grouptag-topping-components.md) 배치 변형 표 — Figma `Topping-Group`의 `Type=3, Direction=Left`와 `Type=3, Direction=Right`가 회전(+8°)·이미지 오프셋·칩 오프셋이 모두 같다. 다른 Left 변형(`Type=1`·`2`)은 전부 음수 회전인데 3번만 Left도 양수다.
- **항목**: 디자인 의도인지 Figma 변형 작성 누락인지. 누락이면 Left 회전각의 부호가 바뀌어야 한다.
- **상태**: 미해결 (Figma 원본대로 구현, 실기기에서 두 변형이 시각적으로 구분되지 않음을 확인. PR #186 develop 머지, 2026-08-01)
- **해소 메모**: 확정 시 `YGToppingGroupType`의 해당 두 엔트리와 코드 주석을 함께 고친다.

### [2026-07-31] `YGChipColorIndicator`의 정책 근거·용도 불명
- **ID**: OQ-P-039
- **출처**: `component/ygcolorchip/YGChipColorIndicator.kt#YGChipColorIndicator`(PR #165 develop 머지) — `isChecked`로 Cherry ↔ 투명을 분기하는 작은 원. 대응 위키 정책 문서가 없다(위키 Chip-Indicator는 [[캘린더-컴포넌트]] C-201 소관으로 이 컴포넌트와 별개). 사용처도 0건이고 `:app-preview` 갤러리에도 미등록이라, 어느 화면의 어떤 선택 상태를 표시하는지 문서만으로는 확정할 수 없다.
- **항목**: ① 이 인디케이터가 붙는 화면·요소가 무엇인지(프로필 색 선택? 멤버 선택?), ② 그 정책이 위키에 있어야 하는지(있어야 하면 소스 수집 대상), ③ 이름이 `Chip`을 달고 있는데 실제로는 칩 외부에서 쓰이는지.
- **상태**: 부분 해소 (①②는 2026-08-04 확정, ③ 이름 잔존)
  > ✅ **첫 사용처 확정(2026-08-04, PR #188)** — `component/yglistdate/YGListDate.kt`가 이 인디케이터를 **C-201 캘린더 날짜 셀의 업로드 여부 점**으로 소비한다. 즉 "프로필 색 선택/멤버 선택"이 아니라 위키 [[캘린더-컴포넌트]] Chip-Indicator였고, **별개라고 본 위 판단이 틀렸다.** 정책 근거도 그 문서이며 "Button-Date가 Disabled면 항상 False" 예외까지 `YGListDate`가 강제한다.
  > 잔존 ③ — 이름은 `Chip`을 달고 있으나 실제 소비처는 칩이 아니라 날짜 셀이고, 패키지도 `ygcolorchip/`이라 소유 폴더가 용도와 어긋난다.
- **해소 메모**: [ygcolorchip 스펙](../superpowers/specs/archive/2026-07-18-ygcolorchip.md)에 유스케이스(= `YGListDate`)를 적고, 개명·이동 여부는 다음 `ygcolorchip` 라운드에서 판단한다. 설계 상세는 [bar-listdate 스펙](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md).

### [2026-07-31] `YGUserChip`·`YGChipColorIndicator`가 갤러리 미등록
- **ID**: OQ-P-040
- **출처**: `component/ygcolorchip/YGUserChip.kt`·`YGChipColorIndicator.kt`(PR #165 develop 머지) — `:app-preview` 컴포넌트 갤러리(카탈로그 + showcase + `@IntoSet` 배선)에 두 신규 컴포넌트가 등록되지 않았다. `ygcolorchip` 계열은 원래부터 갤러리에 없어(`YGNametagChip`도 미등록) 이번 PR만의 누락은 아니다.
- **항목**: ① 갤러리 등록을 신규 컴포넌트 완료 조건(DoD)으로 규약화할지, ② `ygcolorchip` 계열 3종을 묶어 showcase를 추가할지.
- **상태**: 미해결 (**후속 4개 PR은 전부 등록함** — #183 버튼 5종·#185 캔버스 5종·#186 칩/토핑 2종·#188 `YGListDate`/`YGFloatingBar`가 카탈로그·showcase·`@IntoSet`까지 배선됐다. 2026-08-04 기준 갤러리 누락은 `ygcolorchip` 계열 3종뿐이고, 그중 `YGChipColorIndicator`는 `YGListDate` 갤러리 안에서 간접 노출된다)
- **해소 메모**: 규약화하면 [design-system](../architecture/design-system.md) "컴포넌트 작성 규약"에 한 줄 고정하고, 등록 시 갤러리 카탈로그 카테고리를 함께 정한다. 이후 라운드가 이미 관행으로 지키고 있으므로 문서화만 남은 셈이다.

### [2026-07-31] 토핑 템플릿 6종 부여 주체 미정 — 서버 필드 부재
- **ID**: OQ-P-041
- **출처**: [grouptag-topping 스펙](../superpowers/specs/archive/2026-07-31-designsystem-grouptag-topping-components.md) "계층 분할" — 제품 정책은 "6종 중 1종 랜덤 최초 부여 → 첫 토핑 등록 전까지 고정, 새로고침·재접속·타 그룹 갱신에도 불변"인데, 클라이언트가 랜덤을 뽑아 로컬에 영속하면 기기 변경에서 깨진다. 디자인시스템은 `YGToppingImage.Template(type)`으로 결정된 값을 주입받기만 한다.
- **항목**: 서버가 그룹 조회 응답에 템플릿 종류 필드를 내려줄지, 아니면 클라이언트가 뽑아 저장할지. 서버가 내려주면 기기 변경·플랫폼 간(iOS) 일관성이 확보된다.
- **상태**: 미해결 (G-001 목록 API 미확정 — 컴포넌트는 PR #186로 develop 머지, 2026-08-01)
- **해소 메모**: 결정 시 [data-layer](../architecture/data-layer.md) DTO와 G-001 화면 스펙에 반영한다.

### [2026-08-01] 캔버스 화면(C-001) 임시 구현체 잔존 — 메뉴 구현이 두 곳에 공존
- **ID**: OQ-P-042
- **출처**: `feature/groups/canvas/impl`의 `CanvasImageAddScreen`("카메라로 촬영"·"갤러리에서 선택"을 맨 Material3 `Button`+`Text`로 그림) vs 신설 `core/designsystem` `component/ygcanvas/`·`ygcanvasmenu/`·`ygmenuitem/`(PR #185 develop 머지). [캔버스 컴포넌트 스펙](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md)이 화면 치환을 범위 밖으로 두어 셔터([2026-07-30 카메라 항목](#2026-07-30-카메라-컨트롤-임시-구현체-잔존--셔터-구현이-두-곳에-공존))와 같은 공존 상태가 하나 더 생겼다.
- **항목**: ① C-001 화면 라운드에서 임시 버튼을 `YGCanvas`+`YGCanvasMenu`로 치환하고 feature 쪽 임시 컴포저블을 지울지, ② 캔버스 화면이 `YGCanvas`의 직교 플래그를 어떤 UI 상태에 매핑할지(플래그 조합 모순 방지 책임이 호출자에 있다).
- **상태**: **해소됨** (2026-08-11, PR #199)
- **해소 메모**: ① 임시 `Text` + Material3 `Button` 2개가 걷혔고 화면이 `YGTopBarCanvas` + `YGCanvas`(+`YGCanvasMenu`)만 쓴다 — 메뉴 구현 공존 종료. ② 매핑은 **화면 로컬 `remember` 플래그 하나**(`isMenuExpanded`)가 `isDimmed`·`isMenuExpanded`를 함께 켜는 형태로 확정됐고(Figma `Status=Expanded` 조합), `isEmpty`는 상수 `true`, `isCalendarVisible`는 미사용이다. 상세 [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md), 인벤토리 서술은 [design-system](../architecture/design-system.md) "캔버스 5종" 절에 반영했다. 남은 미결(mock 데이터·`isEmpty` 상수·도달 불가)은 [2026-08-12] 항목들로 갈라 뒀다.

### [2026-08-01] 컷 도형 다리 길이·Empty 배경색의 근거가 Figma 벡터뿐
- **ID**: OQ-P-043
- **출처**: `shape/CanvasCutCornerShape.kt#canvasCutCornerShape`·`component/ygcanvas/YGCanvas.kt`(PR #185 develop 머지) — ① 좌상단 컷의 다리 길이가 기본값 리터럴이고 근거가 Figma 벡터 path뿐이다. 위키 [[캔버스-반응형-레이아웃]]은 "좌상단이 비스듬히 잘린 컷"만 서술하고 수치가 없어, 디자이너가 값을 바꾸면 추적할 문서 근거가 없다. ② Figma `Status=Empty`의 배경이 회색인데 이것이 "배경 미지정 기본값"인지 "비어 있을 때만 회색"인지 원본에서 갈리지 않는다 — 구현은 전자로 보고 `background` 기본값에 뒀다(Empty여도 지정 배경이 있으면 그대로 그린다).
- **항목**: ① 컷 다리 길이를 정책 문서(위키)나 디자인 토큰에 올릴지, ② Empty 배경의 의미를 디자이너에게 확정받을지.
- **상태**: 미해결 (구현 완료 — Figma 실측대로 반영)
- **해소 메모**: ①이 정해지면 위키 [[캔버스-반응형-레이아웃]] 갱신 요청 후 컷 값을 그 문서 근거로 바꾼다. ②는 C-001 화면 라운드에서 실제 빈 캔버스를 그릴 때 확인.

### [2026-08-01] YGCanvasDateSelectButton의 클릭 영역·접근성 이름이 아이콘에만 걸림
- **ID**: OQ-P-044
- **출처**: `component/ygcanvasdateselect/YGCanvasDateSelectButton.kt#YGCanvasDateSelectButton`(PR #185 develop 머지) — 바 전체가 컷 배경·테두리를 공유해 하나의 버튼처럼 보이고 이름도 `Button`인데, 실제 클릭 대상은 우측 `YGIconButton`(`SIZE_44`) 하나다. 날짜 텍스트를 눌러도 아무 일도 일어나지 않는다. 같은 아이콘의 `contentDescription`도 `null`이라 유일한 상호작용 요소에 접근성 이름이 없다.
- **항목**: ① 바 전체를 클릭 영역으로 올릴지(이름대로) 아이콘만 유지할지, ② `contentDescription`을 필수 인자로 노출할지 — `YGIconButton`·`YGCircleButton` 선례는 필수다.
- **상태**: 미해결 (구현 라운드에서 **현행 유지로 판정**(2026-07-31), 화면 라운드·접근성 라운드로 이월)
- **해소 메모**: C-201 캘린더/C-001 화면 라운드에서 실제 터치 기대치를 확인한 뒤 정한다. ②는 전원 접근성 라운드와 묶어 처리.

### [2026-08-01] YGCanvas의 Dim 탭 닫기 미규정 + 캘린더 슬롯 미충전
- **ID**: OQ-P-045
- **출처**: `component/ygcanvas/YGCanvas.kt#YGCanvas`(PR #185 develop 머지) — ① Dim은 소비 전용 `pointerInput`으로 아래 레이어 터치를 막지만 **탭했을 때의 동작이 없다**(`onDimClick` 미노출). `Expanded`·`Calendar`를 Dim 탭으로 닫을지가 규정되지 않았고 Figma도 다루지 않는다. 부작용으로 드래그도 막히는데 스크림으로선 의도된 동작이다. ② `calendarContent` 슬롯을 채울 컴포넌트가 없어 `Status=Calendar`를 실물로 대조하지 못했다 — 패널·`List-Date`·`Chip-Indicator`는 C-201 라운드 몫이다. 같은 이유로 `YGCanvasBackground.Image` 화면의 실제 렌더도 여전히 미검증이다(막고 있던 Coil 네트워크 페처 부재는 #186로 해소).
- **항목**: ① Dim 탭 닫기를 컴포넌트 API로 열지(=`onDimClick`) 화면이 바깥에서 처리할지, ② 캘린더 패널이 붙은 뒤 `Status=Calendar`·`Image` 배경을 재대조할지.
- **상태**: **부분 해소** (① 해소 — 2026-08-11 PR #199 / ② **캘린더 슬롯은 2026-08-16 PR #259로 충전**, `YGCanvasBackground.Image` 렌더 미검증만 잔존)
- **해소 메모**: **①**: 컴포넌트 API를 여는 쪽으로 결론났다 — `YGCanvas(onDimClick: () -> Unit = {})` 신설, 구현이 소비 전용 `pointerInput`에서 `clickable(interactionSource = null, indication = null)`로 바뀌어 터치 소비는 유지된다. C-001이 이걸로 확장 메뉴를 닫는다. **②**: **C-201 캘린더 라운드(2026-08-16, PR #259)가 슬롯을 채웠다** — C-001이 `isCalendarVisible`을 켜고 화면 로컬 `CustomCalendar`를 넣으면서 `Status=Calendar` 조합이 실물로 그려지고, Dim이 메뉴와 캘린더 양쪽의 스크림을 겸한다(`onDimClick`이 둘 다 닫는다). 이로써 `YGCanvas`의 직교 플래그 넷이 전부 실사용된다 → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md). **남은 것은 `YGCanvasBackground.Image` 렌더 미검증뿐**이다 — C-001이 여전히 배경 기본값 `Solid(Gray100)`으로만 그린다. [캔버스 컴포넌트 스펙](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md)의 "주의 / 열린 질문"은 그 한 줄만 남기고 정리한다.

### [2026-08-01] G-001 목록 화면이 화면 컨테이너 규약을 벗어남
- **ID**: OQ-P-046
- **출처**: `feature/groups/list/impl/navigation/EntryBuilder.kt#featureGroupListEntryBuilder`·`route/GroupListRoute.kt`(PR #173 develop 머지) — 엔트리 컨테이너가 `YGScaffold`가 아니라 `Box`(전면 배경 이미지 `group_list_background`)이고 `YGScaffold(containerColor = Gray.Transparent)`는 Route 안으로 내려갔다. 그룹 추가 오버레이는 **두 번째 `YGScaffold`**(`Transparency.Black25`)를 겹쳐 그린다. 화면 최외곽 `YGScreen`도 쓰지 않아 `YGScreenScope.OnBack` 경로가 없다. [navigation-flow](../architecture/navigation-flow.md) 체크리스트 2번·[design-system](../architecture/design-system.md) "화면 컨테이너" 역할 분리와 어긋난다.
- **항목**: ① 전면 배경 이미지가 있는 화면의 관용구를 정할지(`YGScaffold`에 배경 슬롯을 열지 / nav 레벨 `Box` 래핑을 허용할지), ② 오버레이·Dim을 `YGScaffold` 중첩으로 그리는 것이 맞는지(`Dialog`·`Popup`·단일 `Box` 대안), ③ 화면 최외곽 `YGScreen` 사용을 규약으로 강제할지 — 강제하면 이 화면이 위반이고, 안 하면 [2026-07-20 화면 컨테이너 ADR](#2026-07-20-화면-컨테이너ygscreenygscaffold-컨벤션--adr-미작성) 내용이 달라진다.
- **상태**: 미해결 (코드 머지됨 — 규약 쪽 결정 필요)
  > 📌 **스캐폴드만 V2가 됐다(2026-08-17, PR #297)** — 두 자리 다 `YGScaffoldV2`로 바뀌었지만 이 항목이
  > 묻는 것(엔트리 `Box` + Route 스캐폴드, 오버레이를 **스캐폴드 중첩**으로 그리기, `YGScreen` 미사용)은
  > 그대로다. 오버레이 쪽에는 `toastPolicy`를 주지 않아 **토스트 호스트는 아래 스캐폴드 하나**다.
- **해소 메모**: 결정 시 [navigation-flow](../architecture/navigation-flow.md) 체크리스트와 [design-system](../architecture/design-system.md) "화면 컨테이너"를 함께 고치고, [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)의 이탈 표기를 정리한다. [2026-07-20 항목](#2026-07-20-화면-컨테이너ygscreenygscaffold-컨벤션--adr-미작성)의 ADR 내용에 직접 걸린다.

### [2026-08-01] G-001 파르페·툴팁이 위키 정책과 미결선 — 화면 골격만 머지됨
- **ID**: OQ-P-047
- **출처**: `feature/groups/list/impl/route/GroupListParfaitLayout.kt`·`GroupListScreen.kt`·`GroupListViewModel.kt`·`route/component/GroupListTooltip.kt`(PR #173·#176·#180 develop 머지) — ① `GroupListUiState.groupList`가 `List<String>` placeholder고 렌더에 쓰이지 않아 [[무한-파르페-그리드]]의 지그재그 배치·인셋·y좌표·6타입 변형·활동순 정렬·상대시간이 전부 미구현이다. ② 크림 반복이 정책의 "토핑 0~3 → 3개, 4개부터 1:1" 규칙이 아니라 `content` 높이를 덮을 때까지의 올림 나눗셈이다. ③ 툴팁이 `LaunchedEffect(Unit) { show() }`로 **진입 시 무조건** 뜬다 — [[g-001-empty-툴팁]]의 노출 조건은 "그룹 0건". ④ `GroupListUiState.isTooltipVisible`은 死필드이고 실제 노출은 화면 로컬 `rememberTooltipState`가 쥔다. ⑤ 툴팁 문구·앵커가 코드에 리터럴로 확정됐는데 위키 정책은 문구·앵커를 미정으로 둔다(정책 소스 미수집).
- **항목**: ① 목록 조회 API가 붙는 라운드에서 파르페 좌표 정책을 어디까지 컴포넌트(`YGToppingGroup`)로 흡수할지, ② 크림 개수 규칙을 정책대로 되돌릴지 레이아웃 파생값으로 유지할지, ③ 툴팁 노출 조건을 `groupList.isEmpty()`로 결선하고 상태 소유를 VM/화면 중 어디로 정할지(`isTooltipVisible` 존폐), ④ 툴팁 문구·앵커를 위키 정책으로 역수집할지.
- **상태**: 미해결 — **①은 부분 해소(2026-08-07, PR #194)**, **③은 해소(2026-08-25, PR #352)**, ②④ 잔존
- **해소 메모**: **①**: 토핑 배치가 화면의 `ToppingLayout`(지그재그 커스텀 `Layout`)으로 들어왔고 좌·우 인셋 4·같은 side 갭 -12·`Right = Left + 86`·저개수 N≤3 +12가 전부 [[G-001-무한파르페-간격-정책-v0.3]]과 **일치**한다. 소유 갈림은 "컴포넌트(`YGToppingGroup`)는 한 토핑의 회전·오프셋만, 화면은 열·간격"으로 결론났다. 다만 **변형 타입은 정책의 랜덤 재부여가 아니라 index 순환**이라 별도 항목으로 뗀다([2026-08-07](#2026-08-07-토핑-변형-타입이-index-순환으로-부여됨--정책은-랜덤-재부여)). **②**: 크림 반복 기준이 접시 제외로 좁혀졌을 뿐 여전히 높이 파생이다. **③**: 조회가 아직 mock이라 그대로고, mock 기본값이 4건이라 **0건 아닌 상태에서도 툴팁이 뜬다**. **④**는 위키 소관이라 정책 소스 수집 요청이 먼저다([[open-questions]]의 툴팁 문구·앵커 미정 항목). 표 갱신은 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) "정책 대조"에 반영했다.
  **③**: ✅ **해소(2026-08-25, PR #352)** — 조건과 상태 소유가 한꺼번에 정해졌다. `observeGroups`가
  캐시를 구독하면서 `isTooltipVisible = groups?.isEmpty() == true`를 같은 자리에서 세우므로
  **死필드였던 `isTooltipVisible`이 실제 노출을 결정하는 값이 됐고**(출처 ④도 함께 닫힌다),
  `GroupListTopBar`는 그 값과 "그룹 추가 칩이 보이는가"를 **둘 다** 만족할 때만 툴팁을 띄운다
  (에러 화면 제외는 종전대로다). 미조회(`null`)에서 안 띄우는 것이 이 결선의 핵심이다 — 0건인지
  모르는 채로 띄우면 그룹이 있는 사용자에게도 한 번 스친다(ADR-0023이 `null`과 `emptyList()`를
  가른 이유가 여기서 실제로 쓰였다). 마지막 그룹을 나가면 다시 뜨고 첫 그룹을 만들면 재조회 없이
  그 자리에서 접힌다. 유닛 4건이 네 갈래를 잠근다. **닫힘 비영속**은 상태를 저장하지 않으므로
  종전대로이고, 화면을 떠났다 돌아오면 컴포지션이 새로 서면서 다시 뜬다.

### [2026-08-01] 테마 비의존 그리기 확장의 소유 모듈이 갈림
- **ID**: OQ-P-048
- **출처**: `core/util/android/extension/Modifier.kt#drawTooltipCornerTop`(PR #176 develop 머지) vs `core:designsystem`의 `border/DashedBorder.kt#dashedBorder`·`shape/CanvasCutCornerShape.kt#canvasCutCornerShape` — 셋 다 테마를 읽지 않고 색·치수를 인자로 받는 순수 그리기 확장인데, 툴팁 꼬리만 `core:util:android`에 있다. 같은 갈림이 clickable 유틸에서 이미 [2026-07-14 항목](#2026-07-14-clickable-유틸이-coreutilandroid로-이동--ripple-색-테마-비의존)으로 등록돼 있다.
- **항목**: ① 그리기 프리미티브의 기본 소유를 `core:designsystem`(`border/`·`shape/`)으로 못박을지, ② `core:util:android`를 "Compose 확장 잡동사니" 자리로 인정하고 기준을 문서화할지 — 후자면 두 모듈의 경계 서술이 필요하다.
- **상태**: 미해결
  > 📌 **네 번째 자리가 생김(2026-08-11, PR #199)** — 배경 점 격자 `Modifier.ygBackgroundDotGrid()`가 `core:designsystem`에 들어가되 `border/`·`shape/`가 아니라 **`component/ygbackgrounddotgrid/`**다. 컴포저블이 아닌 `Modifier` 확장인데 "컴포넌트당 폴더" 규약을 따랐고, 기본 인자로 `YGAtomicColors`·`SizeTokens`를 읽어 **테마 비의존도 아니다**(위 세 사례와 성격이 갈린다). ①을 정할 때 "프리미티브가 토큰 기본값을 가져도 되는가"까지 함께 정해야 한다.
- **해소 메모**: 결정 시 [module-structure](../architecture/module-structure.md) `core:util:android` 행과 [design-system](../architecture/design-system.md) 과도기 마커를 함께 정리한다. [2026-07-14 항목](#2026-07-14-clickable-유틸이-coreutilandroid로-이동--ripple-색-테마-비의존) ②와 같은 결정에 묶인다.

### [2026-08-01] 화면 PR이 `YGButtonType.radius`를 삭제 — 각짐 토큰 경유 회귀
- **ID**: OQ-P-049
- **출처**: `component/ygbutton/YGButtonType.kt`·`YGButton.kt`(PR #182 develop 머지, 카메라 화면 PR) — 변형 공통 속성 `radius`가 제거되고 `YGButton`의 `background`·`border` `shape` 인자와 `clip`도 빠졌다. 현재 전 변형이 `radius.none`이라 렌더는 동일하지만, [버튼 sync 스펙](../superpowers/specs/archive/2026-07-30-designsystem-button-component-sync.md)·[radius-none-sync 스펙](../superpowers/specs/archive/2026-07-19-designsystem-radius-none-sync.md)이 세운 "각짐도 테마 토큰 경유"가 코드에서 사라졌다. 카메라 화면 작업 PR이 디자인시스템 규약을 되돌린 형태다.
- **항목**: ① `radius` 속성을 되살릴지(변형별 곡률이 다시 생기면 필요), 아니면 "전 변형 각짐"을 확정으로 보고 스펙·[design-system](../architecture/design-system.md) 규약에서 radius 조항을 걷어낼지. ② 화면 PR이 `core:designsystem`을 고칠 때의 게이트(디자인시스템 소유자 리뷰·sync 스펙 대조)가 필요한지 — 같은 PR에서 `YGDate`도 함께 회귀했다.
- **상태**: 미해결 (코드/규약 정합)
- **해소 메모**: ①이 정해지면 두 sync 스펙의 각짐 조항과 design-system "컴포넌트 작성 규약"을 함께 맞춘다. ②는 [2026-08-01 YGDate 항목](#2026-08-01-ygdate의-background가-테두리를-덮음)과 한 결정으로 묶인다.

### [2026-08-01] `YGDate`의 background가 테두리를 덮음
- **ID**: OQ-P-050
- **출처**: `component/ygtext/YGDate.kt#YGDate`(PR #182 develop 머지) — modifier 체인이 `background(White)` → `border(Gray800)` → **`background(White)`** 순이라, 나중 배경이 앞서 그린 테두리 위에 칠해진다. #149 sync가 확정한 테두리가 화면에서 안 보일 수 있다. 첫 실사용처가 이 PR의 C-101 상단 날짜 라벨이다.
- **항목**: 중복 `background`를 지울지, 아니면 의도가 "테두리 안쪽만 채우기"였다면 `border`를 뒤로 보내거나 `padding`을 사이에 넣을지.
- **상태**: 미해결 (코드 수정 대상 — 실기기 육안 확인 필요)
- **해소 메모**: 고친 뒤 [ygtext-date-label 스펙](../superpowers/specs/archive/2026-07-18-ygtext-date-label.md) as-built와 [design-system](../architecture/design-system.md) `YGDate` 줄의 ⚠️를 함께 정리한다.

### [2026-08-01] C-101 뷰파인더 상·하 간격이 정책과 어긋남
- **ID**: OQ-P-051
- **출처**: `feature/camera/impl/.../screen/CustomCameraScreen.kt#CameraContent`(PR #182 develop 머지) — 상단 날짜 행과 뷰파인더 사이가 `Spacer(10.dp)` **리터럴**(코드 주석 "10.dp가 없어서 넣었습니다")이고, 뷰파인더와 컨트롤 사이가 `Spacer(gap3)`다. 위키 [[카메라-뷰파인더]] 정책은 상단 8·하단 10을 고정으로 규정하므로 두 값이 뒤바뀐 셈이다. 좌우 여백(`padding7`)·블러 스펙은 정책과 일치한다.
- **항목**: ① 상·하 간격을 정책값에 맞춰 토큰(`gap3`/`padding4`)으로 교체할지, ② 주석이 말하는 "10.dp가 없다"는 인식(실제로는 `padding4`가 10)을 어디서 바로잡을지 — 토큰 탐색이 안 되는 게 반복 원인이면 규약 쪽 문제다.
- **상태**: 미해결 (코드 수정 대상)
- **해소 메모**: 교체 후 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md) "정책 대조" 표를 갱신한다.

### [2026-08-01] 카메라 줌 UI가 死코드로 남음
- **ID**: OQ-P-052
- **출처**: `feature/camera/impl`의 `component/CameraZoomIndicatorComponent.kt`·`component/controls/ZoomLevelRow.kt`(참조 0건) + `CameraControlComponent`(`zoomRatio`·`zoomRange`·`onClickZoomLevel`을 받기만 하고 렌더에 안 씀) + `CustomCameraViewModel`(`OnZoomRangeReady`·`OnClickZoomLevel` 인텐트의 발신처 없음, `zoomRatio`는 `LaunchedEffect`로 카메라에 계속 반영). PR #182가 컨트롤 행을 셔터·플래시·전환 3종으로 재구성하면서 줌 UI만 빠졌다.
- **항목**: ① 줌을 다시 노출할지(Figma 대응 확인 필요 — [2026-07-30 카메라 항목](#2026-07-30-카메라-컨트롤-임시-구현체-잔존--셔터-구현이-두-곳에-공존) ③에서 넘어온 질문), ② 안 쓸 거면 컴포넌트 2개와 상태·인텐트를 걷어낼지.
- **상태**: 부분 해소 (②의 절반 — 파라미터·인텐트는 걷혔고 컴포넌트 2개는 잔존, 2026-09-20 PR #514 / ① 잔존)
  > 📌 **파라미터와 인텐트만 걷혔다(2026-09-20, PR #514)** — `CameraControlComponent`가 받기만 하던
  > `zoomRatio`·`zoomRange`·`onClickZoomLevel`이 시그니처에서 빠지고, `CustomCameraIntent.OnClickZoomLevel`과
  > 그 핸들러도 사라졌다. **남은 것 셋**: `CameraZoomIndicatorComponent`·`ZoomLevelRow` 두 컴포넌트가
  > 여전히 참조 0건이고, `CustomCameraState.zoomRatio`·`zoomRange`와 `OnZoomRangeReady` 인텐트는
  > **살아 있다** — 死코드가 아니다. `CustomCameraRoute`가 `state.zoomRatio`를
  > `CameraPreviewComponent`에 넘겨 `cameraControl.setZoomRatio`로 계속 반영하기 때문이고, 값이
  > 초기값 1f에서 움직일 경로만 없다. 즉 ②는 **사용자가 줌을 바꿀 수단이 없다**는 쪽만 남았고,
  > 그 결정은 ①에 달렸다.
- **해소 메모**: 정하면 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md) 범위 표를 갱신한다.

### [2026-08-01] 카메라·갤러리 권한 요청 경로가 UI에 없음
- **ID**: OQ-P-053
- **출처**: `feature/camera/impl/.../component/CameraPermissionRequestComponent.kt`·`feature/gallery/impl/.../component/GalleryPermissionRequestComponent.kt`(PR #182 develop 머지) — 두 컴포넌트 모두 `onClickGrantPermission`·`permanentlyDenied`를 파라미터로 받지만 본문에서 쓰지 않고 "설정으로 이동" 버튼 하나만 그린다. Route의 `permissionLauncher`와 VM의 `OnRequestPermission`은 살아 있으나 **발신처가 없어** 시스템 권한 다이얼로그가 뜨는 경로가 없다(갤러리는 부분 접근 배너의 `onClickManageMedia`만 launcher를 탄다).
- **항목**: ① 최초 진입 시 자동 요청 또는 "권한 허용" 버튼을 둘지, ② 최초 거부와 영구 거부 화면을 나눌지(`permanentlyDenied` 분기 부활), ③ 안 쓸 파라미터면 시그니처에서 뺄지.
- **상태**: 해소됨 (①② 2026-09-11 PR #489, ③ 2026-09-20 PR #514)
  > 📌 **갤러리 쪽 launcher 경로만 실물이 됐다(2026-08-04, PR #191)** — 死코드였던 부분 접근 배너 대신 화면 하단 "사진 재선택" `YGButton`이 PARTIAL일 때 노출돼 `OnRequestManageMedia` → `RequestPermission` → launcher를 탄다. 즉 **부분 접근 상태에서만** 시스템 다이얼로그가 뜨고, 미허용(DENIED/PERMANENTLY_DENIED) 상태의 `onClickGrantPermission`은 여전히 권한 화면에서 호출되지 않는다.
  > 📌 **두 컴포넌트를 다시 짜고도 그대로다(2026-08-25, PR #350)** — 인셋 수정 라운드가 카메라·갤러리 권한 화면의 레이아웃을 통째로 고쳐 놓으면서 `onClickGrantPermission`·`permanentlyDenied`는 손대지 않았다. 두 파라미터는 여전히 받기만 하고 본문에서 쓰이지 않는다. **화면을 여는 사람이 이 자리를 지나갔는데도 안 열렸다**는 뜻이라, 이 항목은 "잊혀서 남아 있는 것"이 아니라 **결정이 없어서 남아 있는 것**이다.
  > 📌 **①② 결정·구현(2026-09-10 결정, 2026-09-11 PR #489 develop 머지 `be537ea93` — 브랜치 `bugfix/permission-not-required`)** ① **진입 시 자동 요청**으로 정했다. 두 VM이 첫 권한 없음 확인에서 `RequestPermission`을 한 번 발행하고(`requestPermissionOnce`), 다이얼로그가 닫혀 재개 확인이 다시 와도 재요청하지 않는다. 물을 수 없는 상태면 시스템이 다이얼로그 없이 거부로 답하므로 설정 이동 화면이 남는다. 갤러리 PARTIAL은 묻지 않는다. ② **거부 화면을 나누지 않는다.** 다이얼로그가 떠 있는 동안에도 뒤에는 설정 이동 화면을 그대로 둔다(작업자 결정). ③ 카메라 컴포넌트의 `onClickGrantPermission`·`permanentlyDenied`와 갤러리 컴포넌트의 `onClickGrantPermission`은 여전히 쓰이지 않는다(갤러리 컴포넌트에는 `permanentlyDenied` 파라미터가 처음부터 없고 `isDeniedPermission`을 받는다). 같은 브랜치가 카메라에서 권한 전에 CameraX를 바인딩해 허용 뒤에도 프리뷰가 뜨지 않던 결함을 함께 고쳤다(`98286f73e`, 권한 다이얼로그는 Activity를 pause만 시켜 CameraX가 다시 열지 않았다) → [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)·[c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) 「권한 요청 as-built 갱신」.
  > ✅ **③이 닫혔다(2026-09-20, PR #514)** — 쓰이지 않던 파라미터를 시그니처에서 뺐다.
  > `CameraPermissionRequestComponent`에서 `permanentlyDenied`·`onClickGrantPermission`이,
  > `GalleryPermissionRequestComponent`에서 `onClickGrantPermission`이 빠졌고, 두 ViewModel의
  > `OnRequestPermission` 인텐트와 핸들러도 함께 사라졌다(갤러리는 `RequestPermission` **이펙트**를
  > 쏘는 다른 경로 — 진입 시 1회 자동 요청·`OnRequestManageMedia` — 가 살아 있어 launcher는 그대로
  > 돈다). 카메라 권한 화면 프리뷰도 영구 거부 갈래가 사라져 하나로 줄었다.
- **해소 메모**: 위 📌와 두 스펙, [specs/README](../superpowers/specs/README.md) c101·c102 행의 미머지 표기는 78회차 기준선 점검(2026-09-11, develop `c37dc2b4c`)에서 걷었다.

### [2026-08-01] 갤러리 빈 상태 그래픽이 상시 노출되고 문구가 리터럴
- **ID**: OQ-P-054
- **출처**: `feature/gallery/impl/.../screen/CustomGalleryPickerScreen.kt#GalleryContent`(PR #182 develop 머지) — 빈 상태 이미지(`image_gallery_empty`)가 `when(isLoading/isEmpty/else)` 분기 **밖**에 있어 사진 목록이 있어도 함께 그려진다. 로딩 인디케이터는 흰 배경 위 `Color.White`이고, 빈 상태 문구는 `strings.xml`이 아니라 코틀린 리터럴이다(같은 화면의 권한 문구는 리소스화됨 → [2026-07-26 항목](#2026-07-26-문자열-리소스화-부분-적용--잔존-하드코딩domain-표시문자열)).
- **항목**: ① 그래픽을 `isEmpty` 분기 안으로 넣을지(디자인상 상시 노출 의도인지 확인), ② 인디케이터 색을 대비 있는 값으로 바꿀지, ③ 문구를 갤러리 `strings.xml`로 옮길지.
- **상태**: 부분 해소 (①③ **PR #191 develop 머지, 2026-08-04** — 그래픽이 `isEmpty` 분기 안으로 들어가 그래픽+문구가 한 `Column`으로 묶였고, 문구는 갤러리 `strings.xml`로 이동. 그래픽 자체도 벡터 → 밀도별 PNG로 교체. / ② **잔존** — 인디케이터는 여전히 흰 배경 위 `Color.White`다. 같은 PR이 그리드 배경까지 흰색으로 확정해 로딩 중에는 빈 화면으로 보인다.)
- **해소 메모**: ②만 고치면 닫힌다(색 하나). 처리 시 [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) "주의 / 열린 질문"과 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)의 갤러리 항목을 함께 정리한다.

### [2026-08-01] C-101-confirm 이후 경로 미결선 — 확인 화면에서 앞으로 못 감
- **ID**: OQ-P-055
- **출처**: `feature/camera/impl/.../route/PictureConfirmRoute.kt`(PR #182 develop 머지) — "다음"이 `onClickConfirm = { }`(TODO "c103-로딩페이지로 넘어가야함"), 닫기가 `onClickClose = {}`(TODO "c001-캔버스메인으로 넘어가야함")다. 뒤로(다시 찍기)만 동작한다. [navigation-flow](../architecture/navigation-flow.md) 체크리스트 6번(진입 경로를 같은 PR에)의 반대편 사례 — 나가는 경로가 없다.
- **항목**: ① C-103(누끼 로딩) 진입 NavKey·인자 계약 확정, ② 닫기가 캔버스(C-001)로 가는지 촬영 호출자에게 결과를 돌려주는지(`LocalResultEventBus` 경로가 이미 있다) 확정.
- **상태**: 해소됨 (① **PR #221 develop 머지, 2026-08-14** — `navigator.goToAndPopCurrent(NavKeySegmentation(sourceImageUri = uri))`. 인자는 원본 uri 하나이고, `goTo`가 아니라 **치환**이라 확인 화면은 백스택에서 걷힌다. `feature/camera/impl` → `feature/segmentation/api` 의존 추가. / ② **PR #309 develop 머지, 2026-08-20**)
  > 📌 **영향 확대(2026-08-04, PR #191)** — 갤러리 선택도 이 화면으로 합류한다(`PictureConfirmSource.GALLERY`). 갤러리는 결과 반환까지 없애서 **두 진입점 모두 이 화면이 유일한 출구**인데 그 출구가 TODO다. ②의 "결과 반환" 선택지는 갤러리 쪽에서 이미 폐기된 셈이라 결정이 한쪽으로 기울었다.
  > ✅ **②가 닫혔다(2026-08-18, `refactor/segmentation-logic`)** — `Navigator`에 타입 기준 pop
  > `popUpTo<T>()`(`Navigator.kt#popUpTo`)가 신설됐다. 기존 `goToSingleClearTop`은 키 동등성 비교라
  > `NavKeyCanvasMain`의 `groupId`를 알아야 하는데 카메라·세그멘테이션 NavKey는 그 값을 안 들고 다녀서
  > 못 썼다. `PictureConfirmRoute`(`returnResultOnly = false`)·`SegmentationRoute`·
  > `SegmentationConfirmRoute`의 닫기가 전부 `popUpTo<NavKeyCanvasMain>()`으로 캔버스까지 돌아간다.
  > **배경 편집 경로(C-301, `returnResultOnly = true`)는 갈린다** — 여기서 캔버스까지 튀면 편집 중이던
  > 배경이 날아가므로 `onClickClose`가 `popUpTo` 대신 `onBack` 2회(확인 버튼과 같은 백 처리)다.
  > 세그멘테이션 화면들은 배경 편집 경로를 타지 않아 이 분기가 없다. `ToppingEditRoute`는 닫기 버튼
  > 자체가 없어(뒤로만) 대상이 아니다 → [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md).
  > 🔧 **정정(2026-08-19)** — `refactor/segmentation-logic`(PR #290 로컬 머지 브랜치)은 develop
  > 위 `refactor/segmentation-develop`로 다시 만들어졌다(사유는 OQ-P-003 ③ 정정 참고). `popUpTo`
  > 신설과 `PictureConfirmRoute`·`SegmentationRoute`·`SegmentationConfirmRoute`의 닫기 결선은
  > 새 브랜치에서도 코드로 확인된다(`Navigator.kt#popUpTo`, 각 Route의 `onClickClose`) — **해소
  > 판정은 안 바뀐다.** 브랜치명만 정정한다.
  > ✅ **develop 머지됨(2026-08-20, PR #309)** — 위 결선이 develop 코드다. **다만 배경 편집 경로의
  > 처리가 위 서술과 갈린다** — PR 코드리뷰가 `onBack` 2회를 짚어 `popUpTo<NavKeyCanvasBGEdit>()`로
  > 바뀌었다(확인·닫기 두 콜백 모두). 흐름 깊이를 가정하지 않으므로 사이에 화면이 하나 껴도 어긋나지
  > 않고, 목적지를 타입으로 특정할 수 있는 근거는 `returnResultOnly = true` 호출자가 `CanvasBGEditRoute`
  > 하나뿐이라는 것이다. 대가는 카메라가 자기를 부른 화면을 이름으로 안다는 결합이다.
- **해소 메모**: [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)과
  [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)에
  반영 완료.

### [2026-08-01] 블러 구현 관용구가 둘로 갈림 — Haze vs 자체 GraphicsLayer
- **ID**: OQ-P-056
- **출처**: `feature/camera/impl/.../component/CameraFeedLayer.kt`(PR #182 develop 머지, `rememberGraphicsLayer` 2장 + `BlurEffect`) vs [ADR-0018](../adr/0018-backdrop-blur-haze.md)(Top Bar 배경 블러는 Haze, 자체 `GraphicsLayer` 기각). ADR-0018은 "C-101도 같은 구조이니 그 라운드 시작 시 Haze 재사용을 검토하라"고 남겼는데, 검토 기록 없이 자체 구현으로 머지됐다. 두 경우는 대상이 다르다 — C-101은 **자기 자식(카메라 피드)**을 흐리고, ADR-0018이 실패한 것은 **자기 밖 배경**을 레이어로 옮겨 담는 경로다.
  > 📌 **Haze 쪽도 머지됨(2026-08-04, PR #188)** — `libs.versions.toml`·`ComposeConfig`·`YGTopBarEmpty(hazeState)`·`YGTopBarDefaults.BackdropBlurRadius`가 develop에 들어와, 두 관용구가 이제 **둘 다 코드에 있다**(그전까지 Haze 쪽은 브랜치 미머지 상태였다).
- **항목**: ① 이 구분(자기 콘텐츠 블러=자체 구현 / 배경 블러=Haze)을 규약으로 못박을지, 아니면 C-101도 Haze로 통일할지. ② C-101 블러가 실제로 걸리는지 **극단값 대조**로 확인했는지 — ADR-0018이 "틴트만으로도 흐린 것처럼 보여 미동작이 육안 검증을 통과한다"고 경고한 바로 그 구조다(이 라운드에 검증 기록 없음). ③ API 31 미만 폴백(`isBlurSupported`)이 스크림만 남기는 것으로 충분한지.
- **상태**: 미해결 (①은 규약, ②는 검증 미수행)
- **해소 메모**: ② 확인이 먼저다(반경 극단값으로 대조). 결과에 따라 ①을 [design-system](../architecture/design-system.md)이나 ADR-0018 개정으로 남기고 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)의 블러 절을 갱신한다.

### [2026-08-02] 서버 응답 envelope와 Android ApiResponse 불일치
- **ID**: OQ-P-057
- **출처**: 서버 `parfait.common.response.ApiResponse`([api/conventions.md](../api/conventions.md) "응답 envelope") — `success`·`errorDetail` 필드를 Android `ApiResponse`가 갖고 있지 않다. Android `data/service/model/response/ApiResponse.kt`.
- **항목**: Android `ApiResponse`에 `success`·`errorDetail` 필드를 추가할지, 추가 시 파싱·기본값 처리를 어떻게 할지.
- **상태**: 해소됨 (2026-08-04, PR #190 develop 머지)
- **해소 메모**: Android `ApiResponse`가 `success`·`code`·`message`·`data`·`errorDetail` 5필드로 서버 envelope와 일치한다(`errorDetail` 기본값 `null`). [ADR-0017](../adr/0017-remote-network-datasource.md) as-built 절·[data-layer](../architecture/data-layer.md)에 반영했고 [api/conventions.md](../api/conventions.md) "Android 불일치" 표에서 제거했다. **다만 실서버 요청은 여전히 0건**이라 파싱 실동작은 아래 "`data-api-service-layer` 전체가 런타임 미검증" 항목이 계속 안고 간다.

### [2026-08-02] Android 성공 코드 판정이 서버와 어긋남
- **ID**: OQ-P-058
- **출처**: Android `ApiResponse.SUCCESS_CODE`(TODO 상수, `isSuccess`가 `code == "SUCCESS"` 단일 비교) vs 서버 `ApiResponse.ok`/`ApiResponse.created`(`code`=`"OK"`/`"CREATED"` 2종, [api/conventions.md](../api/conventions.md) "응답 envelope").
- **항목**: `isSuccess` 판정을 `"OK"`·`"CREATED"` 2종 비교로 바꿀지 여부와 시점.
- **상태**: 해소됨 (2026-08-04, PR #190 develop 머지)
- **해소 메모**: 성공 판정이 `success` 필드 직독으로 바뀌고 `isSuccess` 프로퍼티·`SUCCESS_CODE` 상수가 삭제됐다 — 서버가 성공 코드를 늘려도 깨지지 않는다. `code`는 분기용으로만 남는다. [ADR-0017](../adr/0017-remote-network-datasource.md) as-built 절 갱신 + [api/conventions.md](../api/conventions.md) "Android 불일치" 표에서 제거.

### [2026-08-02] TokenProvider 실구현 부재
- **ID**: OQ-P-059
- **출처**: Android `EmptyTokenProvider`(항상 null 반환) vs 서버 `SecurityConfig` 화이트리스트(`/actuator/health`·`/swagger-ui.html`·`/swagger-ui/**`·`/favicon.ico`·`/v3/api-docs/**`·`/api/v1/auth/kakao`·`/api/v1/auth/signup`·`/api/v1/auth/reissue`, [api/conventions.md](../api/conventions.md) "인증").
- **항목**: 실 `TokenProvider` 구현 시점·토큰 저장 방식(DataStore 등) 확정.
- **상태**: 해소됨 (2026-08-04, PR #190 develop 머지) — 단 **동작 확인은 아님**
- **해소 메모**: `EmptyTokenProvider`가 삭제되고 `TokenStoreTokenProvider`(→`TokenStore`→`EncryptedTokenStore`, [ADR-0019](../adr/0019-encrypted-token-storage.md))가 들어왔다. [ADR-0017](../adr/0017-remote-network-datasource.md)·[data-layer](../architecture/data-layer.md) 갱신 + [api/conventions.md](../api/conventions.md) 표에서 제거. **저장소가 실제로 토큰을 돌려주는지는 미확인**이다 — `TokenStore.save()` 호출부가 develop에 0건이라 저장된 토큰 자체가 없다(아래 "실기기 암복호화 왕복 검증이 수행 불가" 항목).

### [2026-08-02] 서버 URL 규약 3형태 혼재
- **ID**: OQ-P-060
- **출처**: 서버 `KakaoLoginController`·`SignupController`·`ReissueController`·`LogoutController`(`/api/v1/auth/**`) · `ParfaitController`(`/api/v1/groups/{groupId}/parfaits/**`) · `ParfaitGroupController`(`/api/parfait-groups`, 버전 프리픽스 없음) — [api/conventions.md](../api/conventions.md) "URL 규약".
- **항목**: 버전 프리픽스(`/api/v1/`) 유무와 그룹 경로(`groups` vs `parfait-groups`) 통일 여부 확정.
- **상태**: 미해결 (서버팀 확인 필요 — 서버에 URL 규약 문서 없음)
  > 📌 **2026-08-11 delta로 형태가 5종이 됐다** — `/api/v1/users/me/<하위>`가 더해졌고, 그룹 하위 경로가 `/api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}`까지 깊어졌다. **`images` 세그먼트가 두 도메인(최상위 업로드 / 그룹 하위 배치)에 동시에 존재**한다. 통일을 미룰수록 클라이언트 경로 상수 설계가 굳는다.
- **해소 메모**: 서버가 정리하면 [api/conventions.md](../api/conventions.md) "URL 규약" 표와 각 도메인 문서(`auth.md`·`parfait-group.md`·`parfait.md`·`member.md`·`parfait-image.md`)의 엔드포인트 표 경로를 함께 갱신한다.

### [2026-08-02] 파르페 연도 조회 경로 `year`(단수) vs 응답 필드 `years`(복수) 불일치
- **ID**: OQ-P-061
- **출처**: `GET /api/v1/groups/{groupId}/parfaits/year` — 경로 세그먼트는 단수 `year`인데 응답 `ParfaitYearsResponse.years`는 복수 목록이다. [api/parfait.md](../api/parfait.md) "미결" — 서버 코드만으로는 의도된 설계인지 실수인지 확인할 수 없다(근거 자료인 PR 설명·이슈 조사는 문서화 범위 밖).
- **항목**: 서버팀에 의도 확인(경로를 `years`로 바꿀지, 필드명을 유지할지).
- **상태**: 미해결 (서버팀 확인 필요)
- **해소 메모**: 확인 후 [api/parfait.md](../api/parfait.md) 엔드포인트 표·경로 서술을 갱신한다.

### [2026-08-02] 회원 전역 닉네임과 그룹 닉네임 유효성 규칙 동일성 미대조
- **ID**: OQ-P-062
- **출처**: `ParfaitGroupService.validateJoin`의 `requireMemberNickname`이 반환한 **회원 전역 닉네임**에 `GroupNickname.of`를 그대로 적용한다(join-preview·join의 `INVALID_GROUP_NICKNAME`). [api/parfait-group.md](../api/parfait-group.md) "미결" — 전역 닉네임을 검증하는 `core/member` 값 객체는 이번 서버 계약 조사 범위(컨트롤러·DTO·`ParfaitGroupError`·`GroupName`/`GroupNickname`/`GroupMemberLimit`) 밖이라 확인하지 못했다.
- **항목**: 전역 닉네임 규칙(길이·문자 패턴)이 `GroupNickname`(1~15자, `^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$`)과 같은지 서버 `core/member` 코드를 추가로 읽어 확정한다 — 다르면 회원이 그룹 참여를 시도할 때 본인이 입력한 값과 무관하게 `INVALID_GROUP_NICKNAME`을 받을 수 있다.
- **상태**: 해소됨 (2026-08-11, 서버 `2c5499a` — 두 규칙이 문자 그대로 같다)
- **해소 메모**: `[Feat/#66] 전역 닉네임 변경 API (#77)`가 `core/member/domain/GlobalNickname`을 신설하면서 전역 닉네임 검증 지점이 코드로 드러났다. `MAX_LENGTH = 15`·패턴·길이 검사가 `GroupNickname`과 동일하고, 다른 것은 던지는 코드(`INVALID_NICKNAME` vs `INVALID_GROUP_NICKNAME`)와 `GroupNickname.unknown()` 센티널의 존재뿐이다 — 정상 경로에서 우려한 오탐은 발생하지 않는다. [api/parfait-group.md](../api/parfait-group.md) "미결"을 해소 서술로 바꾸고 [api/member.md](../api/member.md)에 대조 결과를 적었다. 잔여: `GroupNickname.unknown()`이 만드는 `(알수없음)`은 자기 패턴을 통과하지 못하는 값인데, 이 값이 전역 닉네임 자리로 흘러드는 경로는 현재 코드에 없다.

### [2026-08-02] `errorDetail`이 계약에만 있고 값이 채워지지 않는 상태가 의도인지 미확정
- **ID**: OQ-P-063
- **출처**: [api/conventions.md](../api/conventions.md) "응답 envelope" — `GlobalExceptionHandler`의 네 핸들러(`BusinessException`·`ParfaitGroupException`·bad-request 4종·`Exception`)가 모두 `errorDetail` 인자 없이 `ApiResponse.error(errorCode)`를 호출해 필드가 계약에는 있으나 값은 항상 `null`이다. 검증 실패(`MethodArgumentNotValidException`)도 필드별 상세 없이 `CommonErrorCode.INVALID_REQUEST` 하나로 뭉개진다.
- **항목**: 필드별 검증 메시지(`errorDetail`)를 채울 계획이 있는지 서버팀 확인. 채워지면 Android가 폼 필드 단위 에러 표시를 구현할 근거가 생긴다 — 현재는 채워지지 않는다는 전제로만 설계할 수 있다.
- **상태**: 미해결 (서버팀 확인 필요)
- **해소 메모**: 확인 후 [api/conventions.md](../api/conventions.md) "응답 envelope"의 "현재 항상 null" 서술을 갱신한다.

### [2026-08-02] `GET /health`가 인증 대상인 것이 의도인지 미확정
- **ID**: OQ-P-064
- **출처**: [api/conventions.md](../api/conventions.md) "인증" — `HealthController`가 매핑한 `GET /health`(`http/global/health`)는 `SecurityConfig` 화이트리스트의 `/actuator/health`와 경로가 달라 **인증 대상**이다. 화이트리스트는 `/actuator/health`만 허용하고 `/health`는 포함하지 않는다.
- **항목**: `GET /health`가 의도적으로 인증 대상인지(운영용이라 무관), 아니면 화이트리스트 누락인지 서버팀 확인.
- **상태**: 미해결 (서버팀 확인 필요)
- **해소 메모**: 확인 후 [api/conventions.md](../api/conventions.md) "인증"의 관측 사실 서술을 정리한다.

### [2026-08-02] 카카오 로그인 429 요청 한도 초과가 명세에만 있고 서버에 없음
- **ID**: OQ-P-065
- **출처**: 팀 명세([api/spec/auth-kakao-login.md](../api/spec/auth-kakao-login.md))가 `POST /api/v1/auth/kakao`의 상태 코드로 **429 요청 한도 초과**를 열거하나(code 미지정), 서버 `AuthErrorCode` 12종에 대응 코드가 없고 rate limit 구현 흔적(`429`·`TOO_MANY`·`RateLimit`·`Bucket`)도 코드에서 발견되지 않는다.
- **항목**: 429가 **미구현**인지, 인프라 계층(게이트웨이·WAF)에서 처리돼 애플리케이션 코드에 없는 것인지 서버팀 확인. 후자라면 envelope 없이 원시 429가 올 수 있어 Android가 `ApiException.Http`로 받게 되므로 소비 코드에 영향이 있다.
- **상태**: 미해결 (서버팀 확인 필요)
- **해소 메모**: 확인 후 [api/spec/auth-kakao-login.md](../api/spec/auth-kakao-login.md) "코드 대조"와 [api/auth.md](../api/auth.md) "명세 델타"를 갱신한다.

### [2026-08-02] 토큰 재발급 403 정지·탈퇴 회원이 명세에만 있고 서버에 없음
- **ID**: OQ-P-066
- **출처**: 팀 명세([api/spec/auth-reissue.md](../api/spec/auth-reissue.md))가 `POST /api/v1/auth/reissue`의 상태 코드로 **403 정지·탈퇴 회원**을 열거하나, `AuthErrorCode`에 정지·탈퇴에 해당하는 코드가 없고 `ReissueService`에 회원 상태 검사 자체가 없다. 회원 부재는 `MemberQueryPort.existsById` 실패로 **401 `MEMBER_NOT_FOUND`**가 나간다 — HTTP 코드(403 vs 401)와 code 문자열이 모두 다르다.
- **항목**: 회원 정지·탈퇴 상태 개념이 서버에 있는지, 403이 미구현인지 서버팀 확인. 탈퇴 기능은 앱 화면(S-002 계정 정보)에 예정돼 있어 계약이 필요해진다.
- **상태**: 미해결 (서버팀 확인 필요)
- **해소 메모**: 확인 후 [api/spec/auth-reissue.md](../api/spec/auth-reissue.md) "코드 대조"와 [api/auth.md](../api/auth.md) reissue 절을 갱신한다.

### [2026-08-02] 약관 목록 조회 API 부재 — 앱이 termsId를 얻을 경로가 없음
- **ID**: OQ-P-067
- **출처**: 팀 명세([api/spec/auth-signup.md](../api/spec/auth-signup.md))의 `POST /api/v1/auth/signup`이 `agreements[].termsId`를 필수로 요구하고, 서버 `SignupService.validateAgreements`가 `TosQueryPort.findCurrentTerms`(타입별 최신 버전)와 대조해 어긋나면 `TERMS_NOT_FOUND` 400을 던진다. 그런데 **현재 유효한 약관 목록(id·필수 여부·본문·랜딩 URL)을 조회하는 엔드포인트가 서버 계약에 없다**([api/README.md](../api/README.md) 도메인 3건 어디에도 없음).
- **항목**: 약관 목록 조회 API를 서버가 제공할 것인지 확인. 없으면 앱이 `termsId`를 하드코딩해야 하는데, 약관이 개정돼 최신 버전 id가 바뀌면 전 신규 가입이 `TERMS_NOT_FOUND` 400으로 막힌다. 온보딩 약관 동의 화면(TermAgree)이 이미 구현돼 있어(랜딩 URL·저장 TODO 잔존) 연동 시점에 걸린다.
- **상태**: 해소됨 (2026-08-03)
- **해소 메모**: 서버가 `[Feat/#64] 약관 목록 조회 API 구현 (#65)`(`69654bc`)로 **`GET /api/v1/policies`**를 신설했다 — `termsId`·`type`·`title`·`url`·`required`를 내려주고 signup과 **같은 포트**(`TosQueryPort.findCurrentTerms`)를 쓰므로 목록이 준 id는 같은 시점의 signup에서 유효하다. 계약은 [api/policy.md](../api/policy.md), 명세 대조는 [api/spec/auth-signup.md](../api/spec/auth-signup.md) "코드에만 있음"에 반영. 앱 측 연동(`TermContent.kt#TERM_CONTENT_LIST` 리터럴·랜딩 URL TODO 제거)은 미착수라 아래 신규 항목으로 승계한다.

### [2026-08-03] 약관 목록 응답 `url`이 링크인지 전문인지 스키마로 보장되지 않음
- **ID**: OQ-P-068
- **출처**: `GET /api/v1/policies`([api/policy.md](../api/policy.md))의 `policies[].url`을 `TosAdapter`가 `url = it.content`로 채운다. `Tos.content`는 `@Lob` `LONGTEXT` 컬럼이라 약관 **전문**이 들어갈 수 있는 자리이며, URL 전용 컬럼은 추가되지 않았다(서버 커밋 메시지도 "별도 컬럼을 추가하지 않고 기존 `tos.content` 값을 그대로 재사용"이라고 명시).
- **항목**: 운영 DB의 `tos.content`에 무엇을 넣기로 했는지(랜딩 URL / 약관 전문) 서버팀 확인. 전문이 들어가면 앱이 `url`을 브라우저·WebView로 열 수 없고, 컬럼 의미가 signup 흐름과 목록 조회 흐름에서 갈린다.
- **상태**: 미해결 (서버팀 확인 필요 — **2026-08-18부터 앱이 이 값을 실제로 연다**)
- **해소 메모**: 확인 후 [api/policy.md](../api/policy.md) 응답 필드 표와 "미결"을 갱신한다.
  > ⚠️ **위험이 가정에서 화면으로 내려왔다(2026-08-18, PR #296)** — 이 값이 `NavKeyWebView(title, url)`의
  > 인자가 돼 `NotionWebView`가 그대로 로드한다. 링크가 아니면(전문이면) 로드에 실패해 재시도 화면이
  > 뜨고, 사용자에게는 일시 장애와 구분되지 않는다. `NavKeyWebView` KDoc이 이 함정을 적어 두지만
  > 코드에는 판별·폴백이 없다(예: 전문이면 웹뷰 대신 텍스트로 그리기).

### [2026-08-03] 온보딩 약관 화면이 서버 약관 목록을 쓰지 않음(리터럴 잔존)
- **ID**: OQ-P-069
- **출처**: `feature/intro/impl`의 `TermContent.kt#TERM_CONTENT_LIST`가 약관 항목 title을 코틀린 리터럴로 갖고 랜딩 URL은 TODO다. 서버는 `GET /api/v1/policies`로 `termsId`·`title`·`url`·`required`를 내려준다([api/policy.md](../api/policy.md)) — 앱 `:data`에 대응 Service·Response·DataSource가 아직 없다.
- **항목**: 약관 동의 화면을 서버 목록 기반으로 전환. 응답이 **빈 배열일 수 있다**는 점(200 정상)과 배열 순서(`TERMS_OF_SERVICE` → `PRIVACY_POLICY` 서버 고정)를 화면 계약에 반영해야 한다. 리터럴 title 리소스화 건([2026-07-29] 다국어 항목 ③)도 이 전환에 흡수된다.
- **상태**: 해소됨 (2026-08-15, PR #242 — 화면이 서버 목록을 쓴다)
  > 📌 **2026-08-06 (PR #197 머지) 갱신** — `PolicyService#getPolicies`·`PolicyRemoteDataSource#getPolicies`·`PolicyVO`가 develop에 들어와 **"`:data`에 대응 표면이 없다"는 전제는 해소**됐다. 남은 것은 Repository·UseCase·화면 결선이고, `TERM_CONTENT_LIST` 리터럴도 그대로다.
- **해소 메모**: `PolicyRepository`·`GetPoliciesUseCase`가 붙고 `TermAgreeViewModel`이 진입 시 조회한다.
  `TermContent`·`TERM_CONTENT_LIST`는 삭제됐다. 화면 계약 두 가지도 반영됐다 — **배열 순서를 그대로 화면
  순서로 쓰고**(앱이 재정렬하지 않음), **빈 배열은 실패가 아니라 빈 목록**으로 처리한다(확인 버튼 비활성이라
  signup까지 가지 않는다. 다만 조회 실패와 화면 표현이 달라 사용자는 구분이 어렵다 → OQ-P-167).
  반영처: [api/policy.md](../api/policy.md) "Android 매핑"(+`android_status: done`)·[intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md).

### [2026-08-02] 키 유실(Keystore 무효화) 경로 미검증
- **ID**: OQ-P-070
- **출처**: [ADR-0019](../adr/0019-encrypted-token-storage.md) "키 유실 시 정책" — 기기 복원·잠금 화면 자격증명 변경 등으로 Keystore 키가 무효화되면 `CryptoManager.decrypt`가 예외를 던지고 `EncryptedTokenStore.read()`가 이를 잡아 `clear()` 후 `null`을 반환하도록 설계됐다. 코드베이스에 `test`/`androidTest`가 없고 Android Keystore는 JVM 유닛 테스트에서 동작하지 않아 이 경로를 재현·검증하지 못했다.
- **항목**: 키 유실을 실기기에서 재현(기기 복원 또는 잠금 자격증명 변경)해 `clear()` 분기가 실제로 타는지, 앱이 정상적으로 "토큰 없음" 상태로 전환되는지 확인.
- **상태**: 미해결 (재현 수단 없음)
  > 📌 **as-built 범위 확대(2026-08-04, PR #190 머지본)** — `read()`의 `runCatching`이 복호화뿐 아니라 **DataStore 읽기까지** 감싼다. 즉 일시적 저장소 I/O 실패도 같은 경로로 떨어져 토큰이 삭제된다 — 재현해야 할 경우의 수가 하나 늘었다.
  > 📌 **2026-08-09 갱신(PR #219)** — "코드베이스에 `test`/`androidTest`가 없다"는 전제는 해소됐다. `parfait-test-unit`·`parfait-test-android` 배선과 `src/androidTest/` 소스셋이 들어왔다([spec](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md)). 그래도 **재현 수단 없음은 그대로다.** Android Keystore는 여전히 JVM 유닛 테스트에서 동작하지 않고(Robolectric 제외), 계측 테스트는 CI에서 `assembleDebugAndroidTest` 컴파일까지만 검증해 실행되지 않는다. 막고 있는 것이 둘로 명확해졌다 — CI에 기기·에뮬레이터가 없다는 점, 그리고 키 무효화 자체가 기기 복원·잠금 자격증명 변경이라 프로그램으로 유발할 수 없다는 점.
- **해소 메모**: 확인 후 [ADR-0019](../adr/0019-encrypted-token-storage.md) "키 유실 시 정책"과 [specs/archive/2026-08-02-network-envelope-token-storage.md](../superpowers/specs/archive/2026-08-02-network-envelope-token-storage.md) "검증" 절에 결과를 반영한다.

### [2026-08-02] 인터셉터 `runBlocking`이 코드리뷰를 통과할지 미확정
- **ID**: OQ-P-071
- **출처**: `AuthInterceptor` → `TokenStoreTokenProvider.getToken()`이 `runBlocking { tokenStore.getAccessToken() }`으로 suspend 경계를 넘는다([ADR-0019](../adr/0019-encrypted-token-storage.md) "결정", [specs/2026-08-02-network-envelope-token-storage.md](../superpowers/specs/archive/2026-08-02-network-envelope-token-storage.md) "`runBlocking` 사용 근거"). OkHttp dispatcher 스레드에서 실행돼 메인 스레드는 막지 않는다는 근거로 채택했으나, 코루틴 규율(구조화된 동시성) 이탈이라는 지적이 나올 수 있다.
- **항목**: 코드리뷰에서 `runBlocking` 사용이 반려될지 확정. 반려되면 메모리 캐시(StateFlow) + 동기 읽기 방식으로 전환하고, 앱 시작 직후 캐시가 비어 있는 창(window)에서 첫 요청이 토큰 없이 나가는 타이밍 문제를 별도로 설계해야 한다.
- **상태**: 해소됨 (2026-08-04, PR #190 develop 머지 — 반려되지 않음)
- **해소 메모**: 리뷰 반영 커밋은 `AuthInterceptor`의 early return만 걷어냈고 `TokenStoreTokenProvider`의 `runBlocking`은 무수정으로 머지됐다. 메모리 캐시 전환은 불필요해졌다. 단 **런타임에 이 경로가 돌아간 적은 없다**(토큰 저장분 0건) — 실제 지연·ANR 관측은 로그인 연동 라운드 몫이다.

### [2026-08-02] 실기기 암복호화 왕복 검증이 수행 불가
- **ID**: OQ-P-072
- **출처**: [specs/2026-08-02-network-envelope-token-storage.md](../superpowers/specs/archive/2026-08-02-network-envelope-token-storage.md) "검증" — 저장 → 앱 완전 종료 → 재시작 → 읽기를 사람이 육안 확인하면 된다고 봤으나, `TokenStore.save()` 호출부가 코드베이스에 **0건**이라 저장을 트리거할 방법 자체가 없다(auth 도메인 Service·RemoteDataSource·Repository 구현이 이 라운드 범위 밖).
- **항목**: 로그인이 실제로 붙어 `TokenStoreTokenProvider`/`EncryptedTokenStore.save()`가 호출되는 다음 라운드에서 저장 → 종료 → 재시작 → 읽기 왕복을 실기기로 확인한다(DataStore 파일에 평문이 없는지 포함).
- **상태**: 미해결 (로그인 연동 라운드로 이월)
  > 📌 **코드가 develop에 머지됐어도 상태 불변(2026-08-04, PR #190)** — 저장 경로 전체가 develop에 들어왔지만 `TokenStore.save()` 호출부는 여전히 0건이다. 머지가 검증을 대신하지 않는다.
  > 📌 **로그인 화면이 다음 단계로 이어져도 상태 불변(2026-08-09, PR #220)** — 카카오 토큰은 `LoginState`에만 담기고 저장 호출은 여전히 0건이다 → [2026-08-10] 온보딩 체인 항목.
  > 📌 **저장 호출부는 생겼는데 검증은 그대로다(2026-08-16, PR #263)** — 로그인 결선(#241) 이후 저장이 실제로 일어나고, 같은 프록시(`EncryptedPreferences`)로 **계정 정보까지 암호화 저장**돼 확인 대상이 둘이 됐다(DataStore 파일에 닉네임·`memberId` 평문이 없는지 포함). 그런데 자동로그인 라운드의 **수동 확인 7항목이 미수행**이라 저장 → 종료 → 재시작 왕복은 여전히 사람이 본 적이 없다.
- **해소 메모**: 로그인 연동 라운드에서 확인 후 [ADR-0019](../adr/0019-encrypted-token-storage.md)와 [specs/archive/2026-08-02-network-envelope-token-storage.md](../superpowers/specs/archive/2026-08-02-network-envelope-token-storage.md) "검증" 절을 갱신한다.

### [2026-08-02] debug 빌드 `Level.BODY` 로깅이 `reissue`/`logout` 요청 바디의 refresh token을 평문 노출
- **ID**: OQ-P-073
- **출처**: `NetworkModule.provideOkHttpClient`의 `HttpLoggingInterceptor`가 `redactHeader("Authorization")`로 헤더는 가렸으나 `Level.BODY`는 유지했다. `/api/v1/auth/reissue`·`/api/v1/auth/logout` 요청 바디에 실린 `refreshToken`은 헤더가 아니라 바디 필드라 redact 대상이 아니고, debug logcat에 평문으로 남는다.
- **항목**: 바디 필드 단위 redact(예: 커스텀 인터셉터로 JSON 필드 마스킹) 또는 auth 관련 경로만 `Level.NONE`/`Level.HEADERS`로 낮추는 방안 중 선택.
- **상태**: 미해결 (**이론적 노출이 실제 노출이 됐다**)
  > ⚠️ **두 요청 모두 호출부를 얻었다(2026-08-15, PR #260)** — `reissue`는 `TokenAuthenticator`가 401마다
  > 부르고 `logout`은 S-001 앱 설정이 부른다. 게다가 재발급 전용 클라이언트도 같은 `loggingInterceptor()`
  > 설정을 공유하므로 **재발급 요청 바디의 refresh token이 debug logcat에 그대로 남는다.**
  > 실기기 검증(OQ-P-146)에서 로그를 캡처할 때 이 값이 함께 찍힌다는 뜻이다.
- **해소 메모**: 반영 시 [ADR-0017](../adr/0017-remote-network-datasource.md) "로깅" 절과 `NetworkModule.provideOkHttpClient`를 갱신한다.

### [2026-08-02] `@NoAuth` 판정이 Retrofit `Invocation` 태그에 의존 — OkHttp 직접 요청·R8 release 미검증
- **ID**: OQ-P-074
- **출처**: `AuthInterceptor`가 `chain.request().tag(Invocation::class.java)?.method()?.isAnnotationPresent(NoAuth::class.java)`로 스킵 여부를 판정한다(`network/NoAuth.kt`, [ADR-0017](../adr/0017-remote-network-datasource.md) "인증"). `Invocation` 태그는 **Retrofit이 만든 요청에만 자동으로 붙는다** — OkHttp를 직접 쓰는 요청(예: Coil 이미지 로딩이 같은 `OkHttpClient`를 공유하게 되는 경우)에는 태그가 없어 `skipAuth`가 `false`로 떨어져 헤더가 붙는다. 현재 그런 경로는 없다.
  **② R8 release 미검증 항목은 2026-08-03 `data-api-service-layer` 라운드 최종 리뷰에서 해소됐다 — 답은 부정이었다.** keep 규칙(`-keep @interface com.teamyg.parfait.data.network.NoAuth`)이 `data/proguard-rules.pro`에 있었는데, `:data`는 **Android 라이브러리 모듈**이라 `proguardFiles`는 그 모듈 자체의 R8 실행에만 쓰이고 앱(`:app`)의 R8 실행에는 `consumerProguardFiles`로 명시한 규칙만 전달된다. 컨벤션 플러그인(`setConfigAndroidLibrary`)이 `consumerProguardFiles`를 등록하지 않아 이 keep 규칙이 앱에 전혀 전달되지 않고 있었다 — release 빌드였다면 `@NoAuth` 어노테이션이 R8에 제거되고, 화이트리스트 4곳(`postAuthKakao`·`postAuthSignup`·`postAuthReissue`·`getPolicies`) 전부에 `Authorization` 헤더가 붙어 **토큰 재발급이 가장 필요한 순간(만료·미보유 상태)에 막혔을 것**이다. 조치: keep 규칙을 `data/consumer-rules.pro`로 옮기고 `setConfigAndroidLibrary`가 `consumerProguardFiles("consumer-rules.pro")`를 등록하도록 수정.
- **항목**: ① Coil 등 OkHttp를 직접 공유하는 신규 경로가 생기면 `Invocation` 태그 부재로 인증 헤더가 붙는지 확인하고 필요 시 별도 처리(잔존). ~~② `:app:assembleRelease`로 실제 release 빌드를 만들어 화이트리스트 엔드포인트가 여전히 헤더 없이 나가는지 확인~~(해소 — 위 참고, `consumerProguardFiles` 등록 후 `:app:assembleDebug`로 Hilt 그래프까지 재확인).
  > ⚠️ **②가 develop에서 되살아났다(2026-08-04, PR #190)** — 위 수정(`consumer-rules.pro` 이관 + 컨벤션 플러그인 `consumerProguardFiles` 등록)은 `feature/sync-api-service` 브랜치 산출물이라 **develop 미머지**였다. 반면 먼저 머지된 PR #190은 keep 규칙을 **`data/proguard-rules.pro`**에 넣었고, 당시 develop의 `AndroidConfig.kt#setConfigAndroidLibrary`는 `consumerProguardFiles`도 `proguardFiles`도 등록하지 않았다 — 즉 keep 규칙이 어디에도 전달되지 않는 상태였다.
  > 📌 **②가 다시 닫혔다(2026-08-06, PR #197 머지)** — 그 브랜치가 머지되며 keep 규칙이 `data/consumer-rules.pro`로 옮겨지고 `setConfigAndroidLibrary`가 `consumerProguardFiles("consumer-rules.pro")`를 등록했다. develop의 라이브러리 모듈 전부가 이미 `consumer-rules.pro` 파일을 갖고 있어 이 등록이 다른 모듈을 깨지 않는다. 같은 PR로 `@NoAuth` 사용처도 0곳→4곳이 됐다. **다만 `:app:assembleRelease`로 실제 R8 결과를 확인한 기록은 여전히 없다** — 배치가 옳다는 것과 release에서 검증했다는 것은 다르다.
- **상태**: 부분 해소 (② 해소 — 배치 정상화 확인, release 빌드 실행 검증은 미수행 / ① 잔존 — 다만 전제가 바뀌었다, 아래 참고)
  > 📌 **release 빌드가 실제로 돌아간 첫 흔적이 남았다(2026-08-26, PR #372)** — 브랜치 이름이
  > `feature/#283-check-release-build`이고, 커밋 하나가 **릴리즈에서만 터지는 lint 실패**
  > (`Instantiatable` — 매니페스트가 직접 선언한 Kakao `AuthCodeHandlerActivity`의 상속 체인을
  > 컴파일 클래스패스에 `appcompat`이 없어 풀지 못했다)를 고친다. 그 실패를 만나려면 릴리즈
  > 조립을 실행해야 하므로 **"아무도 release를 만들어 본 적 없다"는 더는 참이 아니다.**
  > ⚠️ **그래도 ②가 묻는 것은 안 확인됐다** — R8이 돌아간 산출물에서 `@NoAuth` keep이 살아
  > 화이트리스트 4곳이 헤더 없이 나가는지는 **요청을 보내야 알 수 있고**, 실기기·실서버 확인은
  > 여전히 0회다(OQ-P-146). 빌드가 성공했다는 것과 R8 결과가 옳다는 것은 다르다.
  > ⚠️ **①의 "그런 경로는 없다"가 곧 깨진다(2026-08-12, PR #230)** — `ImageService`가 develop에 들어오면서 **S3 presigned PUT이 예정된 경로가 됐다**(아직 앱 코드는 없다). 그 요청은 Retrofit이 아니라 raw OkHttp로 나가므로 `Invocation` 태그가 없어 `skipAuth = false`가 되고, `Authorization`이 실린 presigned URL을 **S3가 서명 불일치로 거절한다.** 즉 ①은 "언젠가 생길 수도 있는 경로"가 아니라 **업로드 라운드의 선행 조건**이고, 업로드 전용 `OkHttpClient` 분리는 성능 선택이 아니라 기능 전제다 → [image-api-service-layer 스펙](../superpowers/specs/archive/2026-08-10-image-api-service-layer.md), [api/image.md](../api/image.md) "Android 매핑".
- **해소 메모**: ② 반영처: [ADR-0017](../adr/0017-remote-network-datasource.md) "인증" R8 절·[data-layer](../architecture/data-layer.md) "인증"·[specs/archive/2026-08-03-data-api-service-layer.md](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md) "미결" 절을 머지 확정으로 갱신했다. ① 잔존 — 신규 OkHttp 직접 경로가 생기면 이 항목을 다시 연다.

### [2026-08-02] 카카오 로그인 판별자 JSON 키가 `newUser` — Android 응답 타입에 `@SerialName` 필요
- **ID**: OQ-P-075
- **출처**: 서버 `KakaoLoginResponse`는 Kotlin `val isNewUser: Boolean`이지만, 서버가 발행한 OpenAPI 스키마의 `KakaoLoginResponse`는 필드를 **`newUser`**로 적는다. Jackson이 getter(`isNewUser()`) 이름에서 `is` 접두사를 떼고 직렬화하기 때문이다 → [api/conventions.md](../api/conventions.md) "직렬화 규약", [api/auth.md](../api/auth.md), [api/spec/auth-kakao-login.md](../api/spec/auth-kakao-login.md).
- **항목**: Android가 이 응답 타입을 만들 때 `@SerialName("newUser")`를 반드시 붙인다. 붙이지 않으면 kotlinx-serialization이 기본값으로 조용히 떨어져 **신규 유저가 기존 회원으로 분기**되고 존재하지 않는 `accessToken`을 꺼낸다 — 예외가 나지 않아 발견이 늦다. 서버팀에 `@get:JsonProperty("isNewUser")`로 키를 고정할 의향이 있는지도 함께 확인한다(고정되면 클라이언트 쪽 어노테이션이 불필요해진다).
- **상태**: ⚠️ **재개(2026-08-11) — 전제가 틀렸다.** 실제 응답 키는 `isNewUser`이고, 위 "해소"로 붙인 `@SerialName("newUser")`가 **지금은 그 자체로 불일치**다 → 후속 항목 [2026-08-11] 판별자 키 정정.
- **해소 메모**: `data/service/model/response/auth/KakaoLoginResponse.kt`가 판별자 프로퍼티를 `isNewUser`로 두고 `@SerialName("newUser")`를 붙였다. 같은 라운드가 **DTO 전 프로퍼티에 `@SerialName` 명시**를 규약으로 굳혀(키가 프로퍼티명과 같아도 붙인다) 같은 종류의 사고를 구조적으로 막는다 → [data-layer](../architecture/data-layer.md) "패키지 배치". **다만 이 규약은 키 값이 옳을 때만 방어가 된다** — 이번엔 계약 문서가 틀린 값을 줬고 규약이 그 값을 성실히 고정했다. 이 항목은 후속 항목으로 이어진다.

### [2026-08-02] 개발 서버가 평문 HTTP — 앱에서 전 요청이 cleartext 차단된다
- **ID**: OQ-P-076
- **출처**: 개발 서버 base URL이 `https`가 아니라 평문 `http`다(주소는 private submodule `project-paths.md` 참고). TJYG-Android는 `targetSdk = 36`이고 `AndroidManifest.xml`에 `usesCleartextTraffic`·`networkSecurityConfig`가 **둘 다 없다** → [api/conventions.md](../api/conventions.md) "직렬화 규약".
- **항목**: Android 9(API 28)부터 평문 HTTP는 기본 차단이라, 실제 연동을 시작하면 **모든 요청이 `CLEARTEXT communication not permitted`로 실패**한다. 서버에 HTTPS를 적용할지(권장), 아니면 debug 빌드 한정으로 `network_security_config.xml`에 해당 호스트만 허용할지 결정한다. 후자는 release 빌드가 HTTPS 전환 전까지 동작하지 않는다는 뜻이므로 서버 일정과 묶인다.
- **상태**: **해소됨** (2026-08-25, PR #358 — 서버가 ①을 채택하고 앱 매니페스트 조치가 같은 날 들어왔다)
  > 📌 **2026-08-14** — A-002 로그인 실기기 검증을 막고 있어 `app/src/main/AndroidManifest.xml`에
  > `android:usesCleartextTraffic="true"`를 넣었다(**PR #241로 2026-08-15 develop 머지**).
  > **main 매니페스트라 릴리즈 빌드까지 따라간다** — 앱
  > 전체의 HTTPS 강제가 꺼진 상태로 배포될 수 있다. 사용자가 이 자리를 알고 선택했다.
  > 남은 결정은 그대로다: ① 서버 HTTPS 전환(권장, 그러면 이 줄을 지운다) ② 그 전에 릴리즈가
  > 나가야 하면 `app/src/debug/AndroidManifest.xml` 또는 `network_security_config.xml`로
  > **개발 서버 도메인만 debug 한정** 허용으로 좁힌다.
  > ✅ **①이 서버에서 채택됐다(2026-08-25, 서버 #112·#113)** — 앞단 리버스 프록시가 TLS를 종단하고
  > 도메인에 인증서가 붙었다 → [api/conventions.md](../api/conventions.md) "전송". **그러나 이 줄을
  > 아직 지우지 않는다**: 앱이 새 HTTPS 주소로 옮기기 전까지 평문 주소가 유일한 경로이고,
  > `usesCleartextTraffic="true"` 제거는 그 이전이 아니라 **이후**여야 한다(순서를 뒤집으면 앱이
  > 즉시 끊긴다). 전환 시점 자체는 별도 항목으로 뗐다 — OQ-P-302.
- **항목 갱신(2026-08-25)**: 남은 것은 둘이다. ① 앱 `YG_BASE_URL`을 새 HTTPS 주소로 바꾸는 것,
  ② 그 뒤 `app/src/main/AndroidManifest.xml`의 `usesCleartextTraffic="true"`를 제거하는 것.
  ②를 미루면 릴리즈 빌드가 **평문 다운그레이드를 계속 허용**한다 — 서버가 HTTPS를 갖춘 뒤로는
  이 플래그에 남는 효용이 없다.
- **해소 메모**: ②가 같은 날 들어왔다(**PR #358**, 2026-08-25). `usesCleartextTraffic="true"`가
  사라지고 `app/src/main/res/xml/network_security_config.xml`이 그 자리를 대신한다 —
  `base-config`는 `cleartextTrafficPermitted="false"`에 시스템 인증서만 신뢰하고,
  `debug-overrides`가 평문을 허용하면서 사용자 설치 인증서까지 신뢰한다.
  **이 문서가 못 박아 둔 순서(① 먼저, ② 나중)를 코드는 뒤집었는데, 그 위험을 `debug-overrides`가
  흡수했다** — 디버그 빌드는 평문 주소로 계속 붙고 릴리즈 빌드만 HTTPS를 강제하므로, ①이 아직
  안 끝났어도 지금 붙어 있는 개발 경로가 끊기지 않는다. CI도 `YG_BASE_URL`을 주입하지 않아
  `BASE_URL_FALLBACK`(이미 `https`)로 조립된다.
  ⚠️ **다만 좁히기가 이 문서가 적어 둔 것보다 넓다** — 제안은 "개발 서버 도메인만 debug 한정"
  이었으나 실제 설정은 **디버그 빌드의 모든 호스트**에 평문을 연다. 사용자 인증서 신뢰도
  함께 붙어 디버그 빌드는 중간자 프록시로 트래픽을 볼 수 있는 상태다(디버깅 편의와 맞바꾼 자리).
  ① `YG_BASE_URL` 교체는 `local.properties`라 커밋 delta로 확인할 수 없어 **OQ-P-302가 계속 쥔다**.

### [2026-08-03] `clickableYGNoRipple` 사용처 0 — 존치 여부
- **ID**: OQ-P-077
- **출처**: `core/util/android/clickable/YGClickable.kt#clickableYGNoRipple` — `YGScreen` 배경 탭 포커스 해제를 위해 신설됐으나, 그 결선이 접근성 사유로 철회되고 [clearfocusontap-modifier](../superpowers/specs/archive/2026-08-03-clearfocusontap-modifier.md)(`pointerInput` 기반)로 대체되면서 **호출자가 코드 전체에 없다**(정의만 잔존). 함께 들어온 `clickableYGThrottle`의 `indications: List<Indication>?` nullable 일반화도 이 API 전용이다.
  > 📌 **2026-08-04 (PR #192 머지) 갱신** — 이 API는 **이번 머지로 develop에 처음 들어왔다.** 즉 "결선을 위해 만들었다가 결선이 없어진 API"가 아니라, **결선이 develop에 한 번도 도달하지 않은 채 잔여물만 머지된** 상태다. 되돌리기 비용이 가장 싼 시점이 지금이라는 뜻이기도 하다(호출부 0, 되돌릴 시그니처 1개).
- **항목**: ① 존치 — `clickableYG`/`DimRipple`/`ScaleRipple`/`MergeRipple` 4종과 세트를 이루는 공용 API라 "리플 없는 클릭"이 앞으로 쓰일 수 있다, ② 제거(YAGNI) — 제거 시 `clickableYGThrottle`의 nullable 일반화도 함께 되돌려 시그니처를 원복해야 한다.
- **상태**: **해소됨** (2026-08-17, 이관 #284 — ① 존치)
- **해소 메모**: 죽은 API가 아니라 **프로젝트 표준 클릭 유틸**이 됐다. 프로덕션 `Modifier.clickable` 28곳이 전부 이 함수로 이관되면서 사용처 0이 28이 됐고, 되돌리기 후보였던 `clickableYGThrottle`의 `indications` nullable 일반화도 존치 근거를 얻었다. 같은 라운드에서 `interactionSource` 파라미터가 추가됐다(hoisted `MutableInteractionSource`를 넘겨야 pressed 표현이 살아 있는 컴포넌트 9종 때문). 규약은 [design-system](../architecture/design-system.md) clickable 절.

### [2026-08-03] 배경 탭 포커스 해제가 입력 화면 3종에 미적용
- **ID**: OQ-P-078
- **출처**: `feature/groups/enter/impl` `GroupNickNameScreen`·`GroupCreateScreen`·`invitecode/component/InviteCodeInputFieldElement` — 텍스트 입력이 있으나 `YGScreen`을 쓰지 않아(각각 `Column`·`YGScaffold` 기반) 빈 영역 탭 포커스 해제가 없다. S-002만 [clearFocusOnTap](../superpowers/specs/archive/2026-08-03-clearfocusontap-modifier.md)을 적용했다. (📌 2026-08-04 PR #192로 Modifier·S-002 적용분 develop 머지 — 나머지 3종은 그대로 미적용.)
- **항목**: ① 입력이 있는 화면 전부에 `Modifier.clearFocusOnTap()`을 붙여 UX를 통일할지, ② 통일한다면 "텍스트 입력이 있는 화면은 화면 최외곽에 `clearFocusOnTap()`을 붙인다"를 [design-system](../architecture/design-system.md) 또는 [navigation-flow](../architecture/navigation-flow.md) 체크리스트 규약으로 명문화할지.
- **상태**: 미해결 (회귀는 아님 — 이 화면들은 이전에도 없었다)
- **해소 메모**: 적용 시 [clearfocusontap-modifier 스펙](../superpowers/specs/archive/2026-08-03-clearfocusontap-modifier.md)의 "미적용 입력 화면 3종" 항목을 정리한다.

### [2026-08-03] `data-api-service-layer` 전체가 런타임 미검증 — 요청을 한 번도 보내지 못했다
- **ID**: OQ-P-079
- **출처**: [specs/archive/2026-08-03-data-api-service-layer.md](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md) "검증" — 14 엔드포인트 Service·remote DataSource·domain VO가 전부 들어갔고 컴파일·ktlint·`:app:assembleDebug`(Hilt 그래프 resolve)는 통과했지만, **실제 서버로 나간 요청이 0건이다.** 개발 서버 base URL이 평문 `http`인데 `AndroidManifest.xml`에 `usesCleartextTraffic`·`networkSecurityConfig`가 둘 다 없고(위 "개발 서버가 평문 HTTP" 항목과 같은 근거), `local.properties`에 `YG_BASE_URL` 값 자체가 비어 있다. 검증 수단은 컴파일 + `http/` 요청 파일 육안 대조뿐이었다.
- **항목**: 이 레이어가 짊어진 위험은 **컴파일·lint·Hilt 그래프 어디에도 걸리지 않는 종류의 결함**이 그대로 묻혀 들어갔다는 것이다. 대표 사례가 `KakaoLoginResponse`의 `@SerialName("newUser")`(auth.md 참고) — 이 애노테이션이 실수로 빠지거나 잘못된 키 문자열로 붙어도 컴파일은 통과하고 ktlint도 통과하고 Hilt 그래프도 정상 resolve되며, **실제 로그인 응답을 역직렬화하는 순간까지 아무 신호도 나지 않는다.** 실연동(로그인 붙이기) 라운드에서 반드시 실기기 또는 서버 목(mock)으로 14개 엔드포인트를 최소 1회씩 왕복시켜 확인해야 한다.
- **상태**: 미해결 (실연동 라운드로 이월)
  > 📌 **2026-08-06 (PR #197 머지) 갱신** — 이 표면이 develop에 들어왔다. 즉 미검증 코드가 브랜치가 아니라 **기본 브랜치에 있다**. 검증 조건(cleartext·`YG_BASE_URL`)은 하나도 바뀌지 않았고, 소비처가 0건이라 지금은 실행조차 되지 않는다.
- **해소 메모**: 서버 HTTPS 전환 또는 `network_security_config.xml` 화이트리스트 결정(위 "개발 서버가 평문 HTTP" 항목)이 먼저 풀려야 이 항목도 풀린다. 확인 후 [specs/archive/2026-08-03-data-api-service-layer.md](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md) "검증" 절과 `parfait/api/` 4개 계약 문서의 "Android 매핑" 절(`android_status`를 `partial`→`done`으로)을 갱신한다.
  > 📌 **전송 갈래는 닫혔다(2026-08-25, PR #358)** — 디버그 빌드는 `debug-overrides`로 평문이
  > 열려 있고 서버도 HTTPS를 갖췄으므로 **`CLEARTEXT communication not permitted`가 더는 이
  > 항목을 막지 않는다.** 남은 전제는 `YG_BASE_URL` 하나이고, 그것이 채워지면 실요청 왕복을
  > 실제로 해 볼 수 있다. 이 항목이 미해결인 근거는 이제 "막혀 있다"가 아니라
  > **"아무도 아직 해 보지 않았다"**로 바뀐다.

### [2026-08-04] Top Bar의 두 우측 슬롯이 측정 의미가 다름 — `rightContent` vs `trailingContent`
- **ID**: OQ-P-080
- **출처**: `component/ygtopbar/YGTopBar.kt#YGTopBarContent`(PR #188 develop 머지) — `YGTopBarEmpty`가 받는 `rightContent`는 **안쪽 `weight(1f)` `Row` 안**의 형제이고, 같은 PR이 추가한 `trailingContent`(`YGTopBarCanvas`가 씀)는 **그 `Row` 바깥**의 형제다. 즉 앞의 것은 잔여 폭을 제목·날짜와 나눠 갖고, 뒤의 것은 나눔 밖에서 자기 폭을 먼저 확보한다. 이름만 보고는 구분되지 않는다.
- **항목**: ① 다음 Top Bar 라운드에서 두 슬롯을 하나로 통합할지(통합하려면 `Empty`의 로고→날짜 `Row` 구성을 다시 짜야 해서 #188은 범위 밖으로 뒀다), ② 통합 안 하면 이름·KDoc으로 측정 위치를 드러낼지.
- **상태**: 미해결 (이월 관찰 — 현재 렌더 결과는 정상)
- **해소 메모**: 정리 시 [design-system](../architecture/design-system.md) `YGTopBar` 항목과 [bar-listdate 스펙](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md) `YGTopBarContent` 절을 함께 고친다.

### [2026-08-04] `YGFloatingBar` 4변형 사용처 0건 — 화면 배치 책임·중앙 문구 출처 미정
- **ID**: OQ-P-081
- **출처**: `component/ygfloatingbar/YGFloatingBar.kt`(PR #188 develop 머지) — 4변형이 전부 갤러리에서만 렌더되고 feature 참조가 0건이다. 컴포넌트는 폭을 정하지 않고(`modifier` 몫) 상단 패딩만 갖는데, Figma도 화면 어디에 떠 있는지(상단 고정/하단/오버레이)를 주지 않았다. `YGFloatingBarEdit`의 중앙 문구도 Figma가 `Text` placeholder만 둬서 편집 대상 이름인지 모드 라벨인지 미확정이다.
- **항목**: ① 캔버스·편집 화면 라운드에서 배치(위치·폭·safe area)를 어떻게 정할지, ② `Edit`의 중앙 문구가 무엇인지, ③ `EditTab`의 탭 문자열("영역"/"테두리")이 화면 소유인지 컴포넌트 기본값이어야 하는지.
- **상태**: 부분 해소 (①③ **PR #221 develop 머지, 2026-08-14** / ② **잔존** — `Edit` 변형만 여전히 사용처 0건)
- **해소 메모**: ① 배치가 확정됐다 — C-103~C-105 4화면이 전부 세로 `Column`의 맨 위/맨 아래에 `fillMaxWidth()`로 붙이는 형태이고 오버레이가 아니다(`BackClose` 추출·확인, `Close` 로딩·에러, `EditTab` 편집). safe area는 엔트리 `YGScaffold` 기본 `innerPadding`이 처리한다. ③ 탭 문자열은 **화면 소유**로 확정 — `ToppingEditTab` enum이 `@StringRes label`을 들고 화면이 `stringResource`로 풀어 넘긴다(feature `strings.xml`). ②는 `Edit` 변형에 첫 소비처가 생길 때 닫는다. [design-system](../architecture/design-system.md) 인벤토리 노트와 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md)에 반영했고, [bar-listdate 스펙](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md) 열린 질문 3은 닫힌다.
  > 📌 **변형이 5종이 되고 배치가 한 갈래 늘었다(2026-08-30, PR #406 develop 머지)** — Figma에
  > `Status=Title`이 추가돼 `YGFloatingBarTitle`이 신설되고 C-102 갤러리 두 화면이 손으로 조립하던
  > `Row` + `YGCircleButton`을 그것으로 바꿨다. ①의 답은 여기서도 같다 — 세로 `Column` 맨 위에
  > `fillMaxWidth()`로 붙이고 오버레이가 아니다. **②(`Edit`의 중앙 문구)는 그대로 잔존**이고,
  > `Title`의 중앙 문구는 화면 `strings.xml`이 갖는다(③이 탭 문자열에서 고른 것과 같은 쪽).
  > 다만 **빈 상태에는 제목을 두지 않는데 그 근거가 작업자 지시뿐**이다 → OQ-P-331.

### [2026-08-04] Top Bar 날짜 표기가 영문 고정 — 로케일·포맷 규칙 미정
- **ID**: OQ-P-082
- **출처**: `component/ygtopbar/YGTopBar.kt#YGTopBarEmpty`(PR #188) + `feature/groups/list/impl` `GroupListViewModel`·`core:util:jvm` `model/DateFormat` — 상단 바가 완성된 문자열 2개(`date`·`day`)를 받기만 하고, 실제 값은 VM이 `DateFormat.FullMonthWithDay`·`AbbreviatedDayOfWeek`로 만든다. 두 포맷 모두 **영문 표기**(Figma `December 31 (Wed)`)인데 앱 UI는 한국어다. 같은 화면의 `YGDate`도 같은 값을 쓴다.
- **항목**: ① 날짜·요일 표기를 한국어로 갈지 Figma대로 영문을 유지할지(제품 결정), ② 포맷 소유를 `core:util:jvm` 상수로 둘지 로케일 기반 포맷터로 바꿀지, ③ 정책 소스가 위키에 없다 — 수집 대상인지.
- **상태**: 미해결 (컴포넌트는 무관 — 호출 화면·정책 소관)
  > 📌 **세 번째 소비처(2026-08-11, PR #199)** — C-001 캔버스 날짜 라벨이 같은 `DateTextFormat` 2종을 쓴다(`"May 20"` + `"(Wed)"`, 괄호는 화면이 문자열 결합으로 붙인다). 화면 3곳으로 퍼졌다.
  > 📌 **두 번째 소비처(2026-08-04, PR #191)** — C-102 갤러리 목록의 날짜 헤더가 `core:util:jvm` `DateTextFormat`(`monthDayFormat`·`weekdayFormat`, 둘 다 영문 약어)을 쓴다. 즉 영문 표기가 상단 바 한 곳이 아니라 **화면 2곳·포맷 객체 2개**(`DateFormat`·`DateTextFormat`)로 퍼졌고, ②의 "포맷 소유" 질문에는 **같은 성격의 객체가 둘로 나뉜 것**도 포함된다.
- **해소 메모**: ①이 정해지면 `DateFormat`·`DateTextFormat`과 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)·[c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)을 함께 고친다. 위키 정책이 필요하면 소스 수집을 요청한다.

### [2026-08-04] 배경 블러가 실화면에 미배선 + API 31 미만 폴백 수용 여부
- **ID**: OQ-P-083
- **출처**: `component/ygtopbar/YGTopBar.kt#ygTopBarBackdrop`([ADR-0018](../adr/0018-backdrop-blur-haze.md), PR #188 develop 머지) vs `feature/groups/list/impl` `GroupListScreen` — Top Bar는 `hazeState`를 받을 수 있지만 유일한 소비 화면 G-001이 넘기지 않고 배경에 `Modifier.hazeSource`도 걸지 않는다. 앱에서는 `White75` 틴트만 보이고 블러는 `:app-preview` 갤러리 데모에서만 산다. 또 `RenderEffect`가 API 31+이라 `minSdk` 26~30 기기에서는 배선해도 틴트만 남는데, 검증 기기가 API 36이라 그 경로는 한 번도 실행되지 않았다.
- **항목**: ① G-001(및 이후 소비 화면)에 `hazeSource`/`hazeState`를 배선할지 — 배선하면 스크롤 콘텐츠가 바 뒤로 지나갈 때만 의미가 있다, ② 26~30에서 "블러 없음"을 디자인이 수용하는지(플랫폼 제약이라 대안이 없다), ③ API 31 스크롤 중 블러 미갱신 upstream 이슈가 이 앱에 영향을 주는지.
- **상태**: 미해결 (컴포넌트는 준비 완료 — 화면 배선·수용 판단 대기)
- **해소 메모**: ①은 G-001 데이터 결선 라운드에서 함께 처리하고 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)에 반영한다. ②는 디자인 확인 후 [ADR-0018](../adr/0018-backdrop-blur-haze.md) "위험·방어"에 결론을 적는다.

### [2026-08-04] `YGListDate` 업로드 인디케이터가 접근성 트리에 없음
- **ID**: OQ-P-084
- **출처**: `component/yglistdate/YGListDate.kt`(PR #188 develop 머지) — 업로드 여부를 색 있는 점 하나로만 표시하고 `contentDescription`·`semantics`가 없어 TalkBack에는 날짜 버튼만 읽힌다. 색맹 사용자에게도 단서가 색뿐이다. 다만 이 모듈에는 **상태 표시 요소의 접근성 기준 자체가 문서화된 적 없어** 이 컴포넌트만의 문제가 아니다(`YGChipColorIndicator`·`YGGrouptagChip` 타임스탬프 색 등 같은 부류).
- **항목**: ① 상태를 색·도형으로만 표시하는 요소의 접근성 규약(합성 `semantics`·`stateDescription`)을 정할지, ② 정한다면 `YGListDate`처럼 합성 컴포넌트가 부품의 semantics를 병합(`mergeDescendants`)하는 관용구를 함께 못박을지.
- **상태**: 미해결 (이월 관찰 — 모듈 전체 기준 부재)
- **해소 메모**: 규약을 세우면 [design-system](../architecture/design-system.md) "컴포넌트 작성 규약"에 한 줄 고정하고 대상 컴포넌트를 일괄 점검한다.

### [2026-08-04] clear 버튼 노출 게이팅 변경이 기존 입력 화면 2곳에서 미검증
- **ID**: OQ-P-085
- **출처**: `component/textfield/YGTextFieldImpl.kt#showClear`(PR #192 develop 머지) — 조건에 `(isFocused || isError)`가 추가돼 **비포커스·정상 상태에서는 clear(X)가 사라진다.** `YGTextFieldImpl`은 `YGTextField`·`YGTextFormField` 공용이라 변경이 `AccountInfoScreen`뿐 아니라 `GroupCreateScreen`·`GroupNickNameScreen`에도 동시에 적용된다. 이 두 화면은 이번 PR의 범위가 아니었고 회귀 확인 기록이 없다.
- **항목**: ① 두 화면에서 "값이 있는데 clear가 안 보이는" 상태가 UX상 의도인지 디자인 확인(그룹 생성·닉네임 입력은 진입 직후 포커스가 없을 수 있다), ② 의도라면 [YGTextField 스펙](../superpowers/specs/archive/2026-07-10-ygtextfield.md) 표시 규칙이 이미 정본이므로 화면별 예외 없이 확정, 아니면 게이팅에 화면별 opt-out을 둘지.
- **상태**: 미해결 (회귀 확인 필요 — 컴포넌트 변경이 비참여 화면에 전파된 케이스)
- **해소 메모**: 확인 후 [YGTextField 스펙](../superpowers/specs/archive/2026-07-10-ygtextfield.md) "표시·제어 규칙"에 결과를 적고, 예외가 필요하면 시그니처 변경 여부를 함께 결정한다.

### [2026-08-04] S-002가 저장 경로 없이 머지됨 — 닉네임이 화면 로컬 상태에서만 산다
- **ID**: OQ-P-086
- **출처**: `feature/app/setting/impl` `AccountInfoViewModel#AccountInfoUiState`(PR #192 develop 머지) — `nickname` 초기값이 하드코딩 placeholder고, 입력은 유효성 검사만 거쳐 `updateState`로 끝난다. 저장 UseCase·Repository 호출이 없어 **뒤로가기만 해도 입력이 사라진다.** `AppSettingState.nickname`(S-001 프로필 카드)도 같은 성격의 별도 placeholder라 두 화면의 닉네임이 서로 무관하다.
- **항목**: ① 프로필 조회/수정 API 연동 시 저장 트리거를 무엇으로 할지(포커스 해제 — `clearFocusOnTap()`이 이미 그 지점을 만들어 뒀다 / IME 완료 / 상단바 확인 버튼 신설), ② 두 화면이 같은 닉네임을 보도록 소유처를 어디에 둘지(공유 상태 vs 각자 조회), ③ 위키 [[닉네임-자동-생성]]의 "계정 생성 시 1회 부여·DB 저장 후 불변" 규칙과 이 화면의 수정 허용이 어떻게 맞물리는지(초기값 출처가 서버여야 한다).
- **상태**: 미해결 (API 연동 라운드로 이월 — 현재 develop 화면은 동작하지 않는 폼)
  > 📌 **저장 경로가 표면으로는 생겼다(2026-08-12, PR #230)** — `MemberRemoteDataSource.changeGlobalNickname(GlobalNickname)`·`getMyAccount()`가 develop에 있다. ③의 "초기값 출처가 서버여야 한다"도 `MyAccountVO.nickname`으로 조달 가능해졌다. **화면과의 결선은 그대로 0건**이라 이 항목은 닫히지 않는다 — 표면 부재라는 사유만 사라졌다 → [api/member.md](../api/member.md) "Android 매핑".
- **해소 메모**: 연동 시 [s002 스펙](../superpowers/specs/archive/2026-07-22-s002-account-info.md) "주의 / 열린 질문"과 [app-setting-s001 스펙](../superpowers/specs/archive/2026-07-19-app-setting-s001.md) placeholder 항목을 함께 닫는다. 로그아웃·탈퇴 stub(같은 PR로 UI만 노출됨)도 같은 라운드 대상이다.

### [2026-08-04] 커스텀 갤러리가 결과 반환을 끊었는데 호출 화면의 `ResultEffect`가 남음
- **ID**: OQ-P-087
- **출처**: `feature/gallery/impl` `CustomGalleryPickerViewModel`(`ReturnResult` → `NavigateToConfirm`으로 교체)·`route/CustomGalleryPickerRoute.kt`(`LocalResultEventBus` 사용 제거) vs `feature/groups/canvas/impl/route/CanvasMainRoute.kt`(PR #191 develop 머지) — 호출 화면은 여전히 `ResultEffect<String> { CacheImage(imageUri) }`로 URI 반환을 기다리지만, 커스텀 갤러리는 이제 아무것도 보내지 않고 확인 화면으로 `goTo` 한다. 같은 화면에서 가는 다른 목적지(`NavKeyCameraCustom`)도 성공 시엔 확인 화면으로 전진하고 `ReturnResult`는 실패·취소(null)에만 쓴다. 즉 **이 `ResultEffect`가 캐시 저장을 트리거하는 경로가 사실상 없다.**
- **항목**: ① 사진 선택 후 캐시 저장(`CanvasMainIntent.CacheImage`)을 어디서 할지 — 확인 화면 "다음"이 결선되면 그쪽 책임인지, ② 결선 후에도 `ResultEffect`를 남길지 걷어낼지, ③ 커스텀/시스템 피커가 반환 방식이 갈린 것(시스템 쪽은 `LocalResultEventBus` 유지)이 의도인지.
- **상태**: 해소됨 (**PR #309 develop 머지, 2026-08-20** — ②로 결론이 났다: 수신부를 걷어냈다)
- **해소 메모**: `CanvasMainRoute`의 `ResultEffect<String>`과 짝인 `CanvasMainIntent.CacheImage`·
  `handleCacheImage`·`CanvasMainEffect.NavigateToSegmentation`·`AddRecentImageUseCase` 주입이 전부
  삭제됐다. **죽어 있던 것은 기능뿐이고 크래시는 살아 있었다** — 카메라 취소가 흘린 `null`이 결과
  버스에 남아 있다가 캔버스로 돌아오는 순간 그 인텐트로 들어갔다(결과 키가 타입 이름이라 nullable
  여부로 갈리지 않는다). 같은 라운드가 `CustomCameraEffect.ReturnResult(uri: String?)`를 인자 없는
  `Cancel`로 좁혀 `null`을 흘리는 자리 자체를 없앴고, 촬영 실패는 `CaptureFailed`로 갈라 나왔다.
  ①은 별개 자리에서 답이 나왔다 — 최근 이미지 공급자는 `SegmentationViewModel`이 됐다(기록 대상이
  결과물이 아니라 **사용자가 고른 원본 uri**다). ③(커스텀/시스템 피커의 반환 방식 차이)은 시스템
  피커가 도달 불가라 물음이 실물로 서지 않는다 →
  [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md).

  > 📌 **다른 死 `ResultEffect` 하나는 걷혔다(2026-08-09, PR #220)** — 로그인 화면의 `ResultEffect<String>` Toast가 짝(`GroupHomeRoute`)과 함께 삭제됐다. 여기 남은 `CanvasMainRoute` 건은 그대로다 → [2026-08-10] 데코레이터 존치 항목.

  > 📌 **같은 화면 트리에 두 번째 수신부가 생겼다(2026-08-15, PR #231)** — C-301 배경 편집이 `ResultEffect<PictureConfirmResult>`로 확인 화면 결과를 받는다(이쪽은 실제로 발동한다). `CanvasMainRoute`의 `ResultEffect<String>`는 여전히 死경로이고, 카메라 실패·취소가 보내는 `String?` 결과는 **새 수신부도 타입이 달라 못 받는다** → [2026-08-15] 재사용 진입 플래그 항목.

### [2026-08-04] 갤러리 그리드 셀이 `clickableYG` 대신 표준 `clickable` 사용
- **ID**: OQ-P-088
- **출처**: `feature/gallery/impl/.../component/GalleryImageGridComponent.kt#GalleryImageCell`(PR #191 develop 머지) — 셀 클릭이 `Modifier.clickable`이라 `core:util:android`의 leading-throttle(`clickableYG`)을 타지 않는다. 이 클릭은 `navigator.goTo`로 이어지므로 연타 시 확인 화면이 백스택에 중복으로 쌓일 수 있다. 같은 규약 이탈이 [2026-07-18 `YGDateButton` 항목](#2026-07-18-ygdatebutton-clickableyg-미사용--스로틀-규약-이탈)으로 이미 등록돼 있다.
- **항목**: ① 화면(feature) 쪽 클릭에도 `clickableYG`를 규약으로 적용할지 — 지금까지 이 규약은 디자인시스템 컴포넌트 기준으로만 서술됐다, ② 적용한다면 리플 변형(그리드 셀은 이미지 위라 dim/scale 중 무엇인지) 선택.
- **상태**: **해소됨** (2026-08-17, 이관 #284 — ①은 적용, ②는 무리플로 확정)
- **해소 메모**: ① 규약이 **"디자인시스템 컴포넌트"에서 feature 화면 클릭까지 넓어졌다** — 프로덕션 `Modifier.clickable` 28곳 전량 이관. ② 리플 변형을 고르는 대신 **`clickableYGNoRipple`(무리플)을 기본**으로 놓고 리플이 필요한 지점을 나중에 올리는 방향으로 갔다. 그리드 셀은 선택 상태 표현이 없어 리플이 유일한 피드백이었으므로 **`clickableYG` 승격 후보 목록**에 올라간다 → [2026-08-17] 승격 후보 항목. 규약 서술은 [design-system](../architecture/design-system.md) clickable 절.

  > 📌 **사례 추가(2026-08-15, PR #231)** — C-301 배경 편집의 팔레트 원 3종(갤러리·카메라·색)이 전부 `Modifier.clickable`이다. 갤러리·카메라 원은 `goTo`로 이어져 그리드 셀과 같은 중복 진입 위험이 있다.
  > ✅ **이 사례도 함께 닫혔다(2026-08-17, #284)** — 팔레트 원 3종이 `clickableYGNoRipple`로 이관돼 `goTo` 중복 진입이 스로틀에 막힌다.

### [2026-08-04] 갤러리 死코드 2건 — 부분 접근 배너·전체 조회 UseCase
- **ID**: OQ-P-089
- **출처**: `feature/gallery/impl/.../component/GalleryPartialAccessBanner.kt`(참조 0건 — 하단 "사진 재선택" `YGButton`으로 대체됐으나 파일이 남았고, 배경·문구가 `Color` 리터럴 + 코틀린 리터럴이라 문자열 리소스 규약에도 어긋난다) · `domain/.../usecase/gallery/LoadAllGalleryImageGroupsUseCase.kt`(참조 0건 — 화면이 03시 창 필터본 `LoadFilterYGGalleryImageGroupsUseCase`만 쓴다). 둘 다 PR #191 이후 상태.
- **항목**: ① 배너를 지울지(대체 완료) 다른 접근 수준 안내로 되살릴지, ② 전체 조회 UseCase가 앞으로 쓰일 화면이 있는지(있으면 유지, 없으면 Repository의 `loadAllGalleryImages`까지 함께 정리).
- **상태**: 부분 해소 (② 해소 — 정리 쪽으로 닫힘, 2026-09-20 PR #514 / ① 잔존)
  > ✅ **②가 "없다"로 닫혔다(2026-09-20, PR #514)** — `LoadAllGalleryImageGroupsUseCase`가 삭제되고
  > `GalleryRepository.loadAllGalleryImages`와 `GalleryRepositoryImpl`의 구현까지 함께 걷혔다.
  > 해소 메모가 "없으면 Repository까지 함께 정리"라고 적어 둔 그대로다. 갤러리 화면은 03시 창
  > 필터본 `loadFilterYGGalleryImages` 하나만 쓴다.
  > ⚠️ **①은 그대로다** — `GalleryPartialAccessBanner.kt`가 참조 0건으로 남아 있다. 같은 청소
  > 라운드가 바로 옆 死코드는 지우면서 이 파일은 건드리지 않았다.
- **해소 메모**: ①만 남았다. 정리 시 [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) 파일 구성·주의 절과 [data-layer](../architecture/data-layer.md) 레이어 배치의 `GalleryRepository` 서술을 맞춘다.

### [2026-08-04] 가이드 토스트 문구가 카메라·갤러리 두 모듈에 중복 정의
- **ID**: OQ-P-090
- **출처**: `feature/camera/impl` `strings.xml`의 `camera_custom_guide_toast` · `feature/gallery/impl` `strings.xml`의 `gallery_custom_guide_toast`(PR #191 develop 머지) — 문자열 값이 문자 그대로 같다(누끼 대상 선택 가이드). [module-structure](../architecture/module-structure.md) 규약은 "여러 feature가 공유하는 문구는 `core:ui`"라고 정하는데, 지금은 같은 문구가 두 feature에 복제됐다.
- **항목**: ① `core:ui` `strings.xml`로 올릴지(규약대로), ② 아니면 두 화면의 문구가 앞으로 갈릴 예정이라 복제를 의도로 볼지 — 갈릴 예정이면 각 문구가 화면별로 달라져야 한다.
- **상태**: 미해결 (문구가 개정되면 한쪽만 고쳐질 위험)
- **해소 메모**: 결정 후 [module-structure](../architecture/module-structure.md) "규칙"의 공유 문구 조항에 사례를 붙이고 [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) 규약 대조 절을 정리한다.

### [2026-08-04] `AuthInterceptor`의 `@NoAuth` 스킵 방식이 브랜치별로 갈렸다 — 토큰 조회 생략 여부
- **ID**: OQ-P-091
- **출처**: `data/.../network/AuthInterceptor.kt`. 두 형태가 공존한다. ① **`origin/feature/sync-api-service`·`origin/feature/set-up-backend-api` 커밋본** — `skipAuth`면 `chain.proceed(originalRequest)`로 **early return**해 `tokenProvider.getToken()` 호출 자체를 하지 않는다. ② **`feature/set-up-backend-api` 로컬 작업 트리(미커밋)** — early return을 없애고 헤더 부착 조건만 `token != null && skipAuth.not()`으로 바꿔, `skipAuth`여도 `getToken()`을 **항상 호출**한다. **헤더 부착 결과는 네 경우 모두 동일**하다(토큰 있음+`skipAuth`에도 헤더가 붙지 않는다) — 갈리는 것은 비용뿐이다. ②는 화이트리스트 경로(`postAuthKakao`·`postAuthSignup`·`postAuthReissue`·`getPolicies`) 요청마다 `TokenStoreTokenProvider`의 `runBlocking` + DataStore 읽기 + Keystore 복호화를 유발한다.
- **항목**: ①②를 확정한다. ②를 택하면 [ADR-0017](../adr/0017-remote-network-datasource.md) "인증"·[data-layer](../architecture/data-layer.md) "인증"·[network-envelope-token-storage 스펙](../superpowers/specs/archive/2026-08-02-network-envelope-token-storage.md) 세 곳의 "스킵 대상이면 토큰 조회 자체를 생략한다"를 as-built로 정정해야 한다. `skipAuth` 판정 후 `val token = if (skipAuth) null else tokenProvider.getToken()`로 두면 early return 없이도 ① 의미를 지킬 수 있다.
- **상태**: 해소됨 (2026-08-04, PR #190 develop 머지 — **②로 확정**)
- **해소 메모**: PR #190의 마지막 커밋(`refactor: 코드 리뷰 반영`)이 early return을 걷어낸 단일 변경이다 — 즉 ②는 미커밋 실험이 아니라 **리뷰 결론**이었다. [ADR-0017](../adr/0017-remote-network-datasource.md) "인증"·[data-layer](../architecture/data-layer.md) "인증"·[network-envelope-token-storage 스펙](../superpowers/specs/archive/2026-08-02-network-envelope-token-storage.md) 세 곳의 "토큰 조회 자체를 생략한다"를 as-built로 정정하고, 절약 근거 문장을 비용 감수 서술로 바꿨다. 비용이 실제로 드는 시점은 `@NoAuth`를 붙인 서비스 메서드가 develop에 들어올 때다(현재 0건).

### [2026-08-04] `http/` 요청 모음과 `parfait/api/` 계약 문서가 같은 계약을 이중 관리

- **ID**: OQ-P-092
- **출처**: TJYG-Android 루트 `http/`(PR #190 develop 머지) — `auth.http`·`parfait-group.http`·`parfait.http`·`health.http`·`_reset.http` + `README.md`가 엔드포인트 경로·요청 바디·응답 형태·함정(예: `reissue`에 `Authorization`을 붙이면 막힘, `logout` 204라 본문 없음)을 서술한다. 같은 내용이 [api/](../api/README.md)의 도메인 문서 4건 + [api/conventions.md](../api/conventions.md)에도 있다. 두 표면 다 근거는 서버 코드지만 **갱신 절차가 다르다** — `api/`는 스킬 `sync-teamyg-server-api`가 서버 기준선 delta로 갱신하고, `http/`는 사람이 손으로 고친다.
- **항목**: ① 서버 계약이 바뀔 때 `http/`도 함께 갱신하는 것을 `sync-teamyg-server-api` 절차에 넣을지(넣으면 이 위키 저장소의 스킬이 코드 저장소 파일을 고치게 된다), ② 아니면 `http/README.md`를 계약 서술 없이 "실행 방법"으로만 깎고 계약 근거는 `api/`로 단일화할지. 현재는 `http/README.md`가 envelope 5필드·204 예외·`errorDetail` 항상 null까지 자체 서술하고 있어 서버가 바뀌면 조용히 갈린다.
- **상태**: 미해결 (**2026-08-10 실제로 갈렸다** — 아래 참고)
  > 📌 **표면이 더 커졌다(2026-08-06, PR #197)** — `policy.http`가 추가돼 요청 모음이 **14 엔드포인트 전량**을 덮고, `README.md`도 약관 `termsId` 출처·`url` 전문 가능성·성공 코드 2종 같은 계약 서술을 더 얹었다. 이중 관리 면적이 늘었다는 뜻이라 결정을 미룰수록 비싸진다.
  > 📌 **갈라짐 발생(2026-08-10, 서버 `5bb2a3a`)** — image 도메인 2건이 들어와 서버는 16 엔드포인트인데 `http/`는 14에 멈춰 있다. `api/`만 스킬로 갱신되고 `http/`는 손이 닿지 않은 첫 사례다 → 후속 항목 [2026-08-10] `http/` 요청 모음 공백.
  > 📌 **복제면이 넷째로 늘었다(2026-09-04, PR #451 `2b1dce3a`)** — `http/fcm-test.http`가 **서버가 보내는 푸시 페이로드**(문구·`data` 키 4종·채널 id `parfait_default`·TTL·APNs 헤더)를 상수로 옮겨 적었다. 지금까지 복제된 것은 **HTTP 왕복의 계약**이었고 그것은 엔드포인트 커버 셈(`N/29`)이 갈라짐을 잡아 줬는데, **서버→앱 단방향 푸시에는 세는 축이 없다** — 서버가 문구나 채널 id를 바꿔도 어느 셈에도 안 잡히고 앱 저장소의 상수만 조용히 낡는다 → 후속 항목 [2026-09-04] 서버 발송 페이로드 복제(OQ-P-354).
  > 📌 **이중 관리 비용이 처음 실물로 드러났다(2026-08-12, PR #230)** — 공백은 사람이 손으로 메워 20/20이 회복됐지만, 같은 PR이 `http/README.md`만 고치고 `http/auth.http`를 안 고쳐 **`http/` 내부에서도 두 파일이 갈렸다.** 즉 갈라짐은 이제 `api/` ↔ `http/` 사이만이 아니라 `http/` 안에서도 난다 — 계약 서술이 세 곳(도메인 문서·README·요청 파일 주석)에 복제돼 있어서다. ②(README를 실행 방법으로만 축소)의 근거가 이 사례로 한 칸 세졌다.
- **해소 메모**: 결정 후 [api/README.md](../api/README.md) "계약을 실제로 확인하는 법" 절과 `sync-teamyg-server-api` 스킬 절차에 반영한다.

### [2026-08-04] `@NoAuth`를 붙일 서비스 메서드가 develop에 0건 — 인증 스킵 경로가 통째로 死코드

- **ID**: OQ-P-093
- **출처**: `data/.../network/NoAuth.kt`·`AuthInterceptor.kt`(PR #190 develop 머지) — 어노테이션과 판정 로직(Retrofit `Invocation` 태그 조회), R8 keep 규칙까지 들어왔지만 **`@NoAuth`를 실제로 붙인 곳은 develop에 없었다.** 당시 develop의 원격 서비스는 `TempService` 하나뿐이고, 화이트리스트 대상(`postAuthKakao`·`postAuthSignup`·`postAuthReissue`·`getPolicies`)은 전부 `data-api-service-layer` 브랜치에 있었다.
- **항목**: 이 구조가 처음 실행되는 시점이 **auth Service 머지 시점**이라는 뜻이다. 그때 함께 확인할 것 ① `Invocation` 태그가 실제로 붙어 `skipAuth`가 `true`로 판정되는지, ② keep 규칙이 유효한 자리로 옮겨졌는지(위 R8 항목), ③ 스킵 경로에서도 `getToken()`이 호출되는 비용이 실제로 문제인지.
- **상태**: 해소됨 (2026-08-06, PR #197 develop 머지 — 사용처 4곳 확보·keep 규칙 이관 동반)
- **해소 메모**: `AuthService`의 `postAuthKakao`·`postAuthSignup`·`postAuthReissue`와 `PolicyService.getPolicies`에 `@NoAuth`가 붙었다(서버 화이트리스트와 일치, `postAuthLogout`은 화이트리스트 밖이라 미부착). ②는 위 R8 항목에서 함께 닫혔다. **①③은 여전히 실행으로 확인되지 않았다** — 요청이 0건이라 판정 코드가 `true`를 돌려준 적이 없고 스킵 경로의 `getToken()` 비용도 측정된 적이 없다. 그 확인은 "런타임 미검증" 항목이 안고 간다. 반영처: [ADR-0017](../adr/0017-remote-network-datasource.md) "인증"·[data-layer](../architecture/data-layer.md) "인증".

### [2026-08-06] API 표면 14 엔드포인트가 소비처 0건으로 머지됨

- **ID**: OQ-P-094
- **출처**: `data/service/`(`AuthService`·`PolicyService`·`ParfaitGroupService`·`ParfaitService`)·`data/source/{auth,policy,group,parfait}/`·`domain/model/{auth,group,id,policy}/`(PR #197 develop 머지) — Service 4·DataSource 4쌍·DTO 21·VO/value class 21이 들어왔지만 **이들을 호출하는 Repository·UseCase·화면이 하나도 없다.** `domain/repository`에 대응 인터페이스도 없다(스펙이 명시적으로 범위 밖에 뒀다). 화면 쪽 placeholder는 그대로다 — S-002 닉네임·S-001 프로필·G-001 그룹 목록·온보딩 약관 전부 로컬 상태나 리터럴로 산다.
- **항목**: ① 결선 순서를 무엇으로 잡을지(로그인 → 약관 → 그룹 목록이 의존 순서상 자연스럽다), ② DataSource와 화면 사이에 Repository를 둘지 UseCase가 DataSource를 직접 쓸지 — 현재 원격 DataSource가 **이미 도메인 모델을 반환**해서 Repository가 할 변환이 없다([2026-07-30] "원격 DataSource가 도메인 모델을 직접 반환" 항목과 같은 쟁점), ③ 결선 전까지 이 표면이 컴파일만 통과한 채 남는 기간을 얼마로 볼지.
- **상태**: 부분 해소 (2026-08-15 — auth 2·policy 1·parfait-group 5, **8 엔드포인트가 화면까지** 결선됐다. 나머지 4 도메인은 그대로)
  > ✅ **②가 확정됐다 — Repository를 둔다.** 원격 DataSource가 이미 도메인 모델을 반환해도 Repository가
  > **`ApiException` → `AppError` 변환 경계**를 맡으므로 위임만 하는 층이 아니다([data-layer](../architecture/data-layer.md)
  > "실패는 Repository 경계에서 도메인 타입이 된다"). ①의 순서도 "로그인 → 약관 → 그룹"으로 실제로 그렇게 갔다.
  > ③(표면이 노는 기간)은 parfait·image·member·parfait-image 넷에 대해 여전히 열려 있다 — 그중 캔버스 조회는
  > C-001 결선의 선행이다(OQ-P-158).
- **해소 메모**: 첫 결선 라운드에서 ②를 확정하고 [ADR-0017](../adr/0017-remote-network-datasource.md) 대안 D·[data-layer](../architecture/data-layer.md) "신규 데이터 추가 체크리스트"에 반영한다. 각 도메인이 결선되면 [api/](../api/README.md) 문서의 `android_status`를 `partial`→`done`으로 올린다.

  > 📌 **①의 화면 순서만 먼저 코드로 굳었다(2026-08-09, PR #220)** — `Splash → Login → TermAgree → GroupList` 전이가 결선됐다. 데이터 쪽은 **한 줄도 붙지 않았다**(Service 호출 0건 유지) → [2026-08-10] 온보딩 체인 항목.
  > 📌 **첫 소비처 자리가 mock으로 채워졌다(2026-08-12, PR #224)** — 그룹 생성·참여가 화면에서 결선되면서 `CreateGroupUseCase`·`EnterGroupUseCase`가 생겼는데, `parfait-group` 계열 remote DataSource를 쓰지 않고 **고정 지연 후 성공만 반환하는 stub**이다. 표면이 놀고 있는 채로 그 위에 대체 구현이 하나 더 얹혔다 → [2026-08-12] mock UseCase 항목.
  > 📌 **표면이 20에서 25로 늘었고 그 다섯도 소비처 0이다(2026-08-15, PR #250)** — 파르페 오늘·과거 조회,
  > 토핑 테두리 수정·삭제, 회원 탈퇴가 Service·DataSource·domain VO까지 들어왔다(OQ-P-158 해소). **표면 쪽
  > 공백은 이제 없다** — Android가 쓰기로 한 25 엔드포인트 전량에 심볼이 있다. 그래서 ③("이 상태로 남는
  > 기간")이 네 도메인(parfait·image·member·parfait-image)에 대해 **유일하게 남은 질문**이 됐고, 그중
  > 캔버스 조회는 C-001 결선의 선행이라 순서상 가장 앞이다.
  > ✅ **auth 도메인이 닫혔다(2026-08-15, PR #260)** — `reissue`(`TokenAuthenticator`)·`logout`
  > (`AuthRepository.logout` → `LogoutUseCase` → S-001)이 소비처를 얻어 애플을 뺀 4 엔드포인트 전부가
  > 호출부를 가진다. `api/auth.md`가 `android_status: done`이 된 두 번째 도메인이다.
  > 📌 **표면이 25에서 27로 늘었고 그 둘도 소비처 0이다(2026-08-16, PR #266)** — 캔버스 상세 조회·배경
  > 변경이 Service·DataSource·domain 모델까지 들어왔다. ③("표면이 노는 기간")의 대상이 다시 넓어졌고,
  > 이번 둘 중 **배경 변경은 소비처가 이미 화면으로 존재한다**는 점이 앞선 공백들과 다르다 — C-301
  > 배경 편집은 develop에 있으면서 고른 값을 버리고 있다(OQ-P-173). 즉 "표면이 노는" 것이 아니라
  > **화면과 표면이 서로를 모르는** 상태다.
  > ⚠️ **parfait 도메인에는 표면을 우회하는 소비자가 생겼다(2026-08-16, PR #259)** — C-201 캘린더의
  > UseCase 둘이 조회 두 엔드포인트를 KDoc으로 가리키면서 remote DataSource를 안 쓰고 mock을 만든다.
  > ③("표면이 노는 기간")의 성격이 바뀐 셈이다 — 이제는 **놀고 있는 표면 위에 mock 소비자가 얹힌**
  > 상태이고, 이것은 2026-08-12 그룹 라운드에서 한 번 겪고 걷어낸 형태다(OQ-P-134) → OQ-P-183.
  > 📌 **표면이 14에서 20으로 늘었고 소비처는 그대로 0이다(2026-08-12, PR #230)** — Service 7·remote DataSource 7쌍·DTO 30·domain 37이 됐다. **"덮을 게 남아서 소비를 미룬다"는 사유가 이번 라운드로 사라졌다**([2026-08-11] 7 엔드포인트 공백 항목 해소) — 표면을 더 만들 것이 없으므로 ③("이 상태로 남는 기간을 얼마로 볼지")이 이제 실질적인 질문이다. ②(Repository를 둘지)도 여전히 미정이고, 그 사이 표면만 계속 컴파일된다.

### [2026-08-06] 스펙이 지시한 근거 주석·KDoc 2건이 코드에 없다

- **ID**: OQ-P-095
- **출처**: ① `data/source/group/mapper/VOMapper.kt`의 `recentImageUploadedAt` 변환 — 스펙([data-api-service-layer](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md) "계약이 던지는 함정" 6번)이 "mapper에 타임존 근거를 주석으로 남긴다"고 정했으나 주석이 없다. 서버가 오프셋 없는 문자열을 주고 실제 기준은 Asia/Seoul 벽시계라, 코드만 봐서는 UTC로 오인할 수 있다. ② `domain/model/auth/KakaoLoginVO.kt`·`domain/model/KakaoLoginResult.kt` — 스펙이 "양쪽 KDoc에 서로를 가리키는 한 줄"을 정했으나 두 파일 다 KDoc이 없다. 앞은 서버 응답, 뒤는 카카오 **SDK** 결과다.
- **항목**: ① 주석·KDoc을 채울지, ② 아니면 계약 근거를 코드 주석에 두지 않고 `parfait/api/` 문서로만 유지하는 것을 규약으로 정할지 — 지금은 스펙이 "주석을 남긴다"고 적고 코드가 안 남긴 상태라 어느 쪽도 규약이 아니다. `PolicyItemResponse`에는 KDoc이 있어 관행도 갈린다.
- **상태**: 미해결 (코드 수정 대상 — 동작 영향 없음, 오독 위험만)
- **해소 메모**: 채우면 [data-api-service-layer 스펙](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md) "As-built 이탈" 4·5번과 [api/parfait-group.md](../api/parfait-group.md)·[api/auth.md](../api/auth.md) Android 매핑 절의 지적을 정리한다.

### [2026-08-06] `domain/model/`이 루트 평면과 도메인 하위 패키지로 갈림

- **ID**: OQ-P-096
- **출처**: `domain/model/`(PR #197 머지 후) — 이번 라운드가 추가한 21선언은 `auth/`·`group/`·`id/`·`policy/` 하위 패키지로 들어갔고, 기존 8선언(`DayWindow`·`GalleryImageGroup`·`GroupCreateConfig`·`InviteCodeResult`·`KakaoLoginResult`·`Logger`·`NameValidResult`·`SegmentationResult`)은 루트에 남았다. 스펙은 "평면 10개에 9개 이상이 더 붙으면 하위 패키지로 나눈다"를 전제했는데 실제로는 신규분만 나뉘었다. `KakaoLoginResult`(루트)와 `KakaoLoginVO`(`auth/`)가 이름은 닮았는데 위치가 다른 것이 대표 사례다.
- **항목**: ① 기존 8선언도 도메인 하위 패키지로 옮길지(`Logger`처럼 도메인이 애매한 것의 소속을 정해야 한다), ② 아니면 "원격 계약에서 온 모델만 하위 패키지"를 규칙으로 명문화할지. ②를 택하면 근거를 [data-layer](../architecture/data-layer.md) "레이어 배치"에 적는다.
- **상태**: 미해결 (배치 규약 미정 — 새 모델을 어디 둘지 매번 판단해야 한다)
  > 📌 **여덟 번째 하위 패키지(2026-08-15, PR #250)** — `canvas/`가 여섯 선언(`TodayCanvasVO`·`PastCanvasVO`·
  > `CanvasStatus`·`CanvasBackground`·`CanvasMemberVO`·`CanvasToppingVO`)과 함께 들어왔고 루트 평면 8선언은
  > 이번에도 그대로다. **신규분만 계속 나뉜다는 ②의 패턴이 네 라운드째 반복됐다** — 규약 없이 관행만 굳는
  > 상태다. (2026-08-16 PR #266이 `TodayCanvasVO`를 **`CanvasVO`로 개명**하고 쓰기 전용
  > `CanvasBackgroundEdit`를 더해 **일곱 선언**이 됐다 — 하위 패키지 개수는 그대로다.)
  > 📌 **혼재가 더 깊어졌다(2026-08-12, PR #230)** — 하위 패키지가 `image/`·`member/`·`topping/` 셋 늘어 **넷에서 일곱**이 됐고, 루트 평면 8선언은 하나도 안 옮겨졌다. `GalleryImageGroup`(루트, 기기 갤러리)과 `image/`(서버 업로드)가 이제 나란히 있어 `KakaoLoginResult`/`KakaoLoginVO` 사례가 하나 더 늘었다 — 이름이 닮았는데 위치가 다르고, **가리키는 대상도 다르다**([2026-08-10] `image` 이름 선점 항목과 같은 뿌리). 규약 없이 라운드가 반복되면 신규분만 계속 나뉜다.
- **해소 메모**: 결정 후 [data-layer](../architecture/data-layer.md) "레이어 배치"의 `domain/model/` 서술과 [data-api-service-layer 스펙](../superpowers/specs/archive/2026-08-03-data-api-service-layer.md) "As-built 이탈" 6번을 정리한다.

### [2026-08-07] 토핑 변형 타입이 index 순환으로 부여됨 — 정책은 랜덤 재부여

- **ID**: OQ-P-097
- **출처**: `feature/groups/list/impl/route/GroupListScreen.kt#TOPPING_PLACEMENT_TYPES`(PR #194 develop 머지) — 6타입(`YGToppingGroupType`의 `TYPE_1~3_LEFT/RIGHT`)을 목록 index로 나눈 나머지로 고른다. 코드에 `// Todo : 로직 추후 변경하기`가 붙어 있다. 위키 [[G-001-무한파르페-정책설계-v0.3]]은 **변형 번호를 랜덤 재부여**하고 재추첨 시점을 "목록 조회 응답 1회"로 못박았다(리렌더·셀 재사용 시 재추첨 금지 = 회전각 튐 방지). index 순환이면 같은 자리의 토핑은 항상 같은 회전각이고, 앞에 하나만 추가돼도 뒤 전부의 회전각이 결정적으로 밀린다.
- **항목**: ① 랜덤 부여를 어디서 할지 — 조회 응답을 받는 VM(정책의 "응답 1회"와 맞음) vs 화면(`remember(groupList)`), ② 랜덤 시드 없이 `Random`을 쓰면 프로세스 재생성·상태 복원에서 각이 바뀌는데 그걸 허용할지, ③ 좌/우(`index % 2`)는 정책상 결정적이므로 랜덤 대상은 번호(1/2/3)뿐임을 코드에 못박을지.
- **상태**: 미해결 (조회가 붙었는데도 index 순환 그대로 — **같은 성격의 결정적 파생이 둘 늘었다**)
  > 📌 **2026-08-15(PR #248)** — 조회 결선 라운드가 변형 타입을 건드리지 않았고, 대신 같은 방식이 둘 더 생겼다:
  > 그룹칩 타입이 **목록 index 순환**(정책은 마지막으로 그룹을 바꾼 유저의 Nametag-Chip 타입)이고,
  > 토핑 템플릿이 **`groupId` 파생**(정책은 그룹 생성 시 6종 중 랜덤 부여 후 고정)이다. 둘 다 코드에 TODO가
  > 붙어 있고 **서버가 값을 내려주면 걷는다**는 전제라, 변형 번호와 달리 앱 단독으로 못 닫는다.
- **해소 메모**: 결정 시 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)의 "토핑 배치"·"정책 대조"를 갱신한다. 칩 타입·템플릿은 서버 응답 필드가 선행이므로 [api/parfait-group.md](../api/parfait-group.md) 목록 응답 필드와 함께 본다. [2026-08-01 파르페·툴팁 항목](#2026-08-01-g-001-파르페툴팁이-위키-정책과-미결선--화면-골격만-머지됨) ①에서 떨어져 나온 항목이다.

### [2026-08-07] G-001이 mock 그룹 4건을 UiState 기본값으로 들고 머지됨

- **ID**: OQ-P-098
- **출처**: `feature/groups/list/impl/route/GroupListViewModel.kt#MockToppingGroup`·`GroupListUiState`(PR #194 develop 머지) — `groupList`의 **기본값 자체가** 이름·원격 이미지 URL·상대시간 문자열을 박은 4건이다(`chipType`도 전 항목 동일 값 고정). 조회 경로가 없으므로 develop 빌드는 항상 이 4건을 그린다. URL 중 1건은 스킴이 두 번 붙어 로드에 실패하고, `AsyncImage(error = …)` 폴백 덕에 조회 실패 그래픽으로 그려진다 — 즉 **실패 경로가 의도 없이 상시 노출**된다. 부작용으로 0건 상태를 실행 중에 볼 수 없어 [[g-001-empty-툴팁]] 조건 위반이 가려진다.
- **항목**: ① mock을 VM 기본값이 아니라 프리뷰 파라미터·`@VisibleForTesting`으로 옮길지, ② 조회가 붙기 전까지 develop에 mock 데이터를 두는 것을 허용할지(같은 판단이 앞으로 반복된다), ③ 상대시간을 문자열로 들고 있는 임시 모델을 도메인 모델 결선 시 어떻게 걷어낼지.
- **상태**: 부분 해소 (2026-08-15, PR #248 — `MockToppingGroup`·mock 4건·mock 새로고침 전부 삭제. **`nickName` 하나 잔존**)
- **해소 메모**: ①②③이 조회 결선으로 함께 닫혔다 — 기본값이 빈 목록이고, 상대시간은 문자열이 아니라 `Instant`에서
  화면이 계산하며(`GroupTimestamp`), 새로고침은 실제 재조회다. [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)
  "API / 인터페이스"의 mock 블록도 실제 모델로 교체했다.
  **남은 것은 `GroupListUiState.nickName`** — 여전히 리터럴이고 #243 이후로는 그 값이 **실서버 그룹 생성 요청의
  `groupNickname`으로 나간다**(내 계정 조회가 붙어야 닫힌다 — `member` 도메인 표면은 소비처 0, OQ-P-094).
  > 📌 **mock 필드 추가(2026-08-10, PR #222)** — `GroupListUiState.nickName`이 같은 방식으로 기본값 리터럴이 됐고, 그 값이 **화면 밖으로 나간다** — `goTo(NavKeyGroupCreate(nickName))`로 A-005가 받는다([2026-07-29] 항목). 새로고침(`Refresh`)도 조회 없이 고정 지연만 두는 mock이다. 즉 mock이 표시용을 넘어 **다른 화면의 입력**이 됐다.
  > 📌 **mock이 왕복을 닫았다(2026-08-12, PR #224)** — 생성·참여가 끝나면 `goToSingleClearTop(NavKeyGroupList)`로 이 화면에 **돌아온다**. 엔트리 재사용이라 같은 ViewModel이 살아나고 조회 경로도 없으므로 새 그룹은 절대 나타나지 않는다 — mock 4건 고정이 "방금 만든 그룹이 없다"는 형태로 사용자에게 보이게 됐다. ③(임시 모델 걷어내기)에 **재조회 트리거를 누가 쥐는가**가 추가된다 → [2026-08-12] mock UseCase 항목 ③.

### [2026-08-07] 토핑이 그려지는데 클릭 경로가 없다 — `ClickTopping` 死경로

- **ID**: OQ-P-099
- **출처**: `GroupListScreen.kt#GroupListContent`·`YGToppingGroup.kt#YGToppingGroup`(PR #194 develop 머지) — 화면이 `onClickTopping` 콜백을 파라미터로 받고 VM에 `GroupListIntent.ClickTopping`·`GroupListSideEffect.NavigateToCanvas`가 있는데, 토핑을 그리는 자리에서 아무것도 연결하지 않는다. `YGToppingGroup`은 설계상 `onClick`을 갖지 않고 [design-system](../architecture/design-system.md)이 "터치 범위는 호출자가 `clickableYG`로 감쌈"이라 적어 뒀는데, **첫 호출자가 감싸지 않았다**. 위키 [[무한-파르페-그리드]]의 "대표 토핑 클릭 → C-001"이 실행 불가다.
- **항목**: ① 터치 범위를 어디로 잡을지 — 160dp 프레임 전체 vs 96dp 이미지 + 칩(회전·오프셋 때문에 프레임과 시각 영역이 어긋난다), ② 그래도 호출자 책임으로 둘지 아니면 `YGToppingGroup`에 `onClick`을 열지(열면 컴포넌트가 상호작용을 갖게 되어 현재 "상호작용 없음" 분류가 바뀐다), ③ `NavigateToCanvas` 대상 `NavKey`가 아직 없어 C-001 결선과 함께 처리해야 하는지.
- **상태**: 미해결
- **해소 메모**: 결정 시 [design-system](../architecture/design-system.md)의 `YGToppingGroup` 서술과 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) "동작 / 상태"를 함께 고친다.

### [2026-08-07] 상단 인셋 관용구가 3형태로 갈림 — 탑바 흡수 vs 화면 처리 vs Scaffold 기본

- **ID**: OQ-P-100
- **출처**: `component/ygtopbar/YGTopBar.kt#YGTopBarEmpty`·`YGTopBarDefaults.kt#windowInsets`·`GroupListRoute.kt`(PR #194 develop 머지) — `YGTopBarEmpty`가 `windowInsets`(기본 `WindowInsets.statusBars`)를 자기 패딩으로 흡수하고 호출 화면은 `YGScaffold(contentWindowInsets = systemBars.only(Horizontal + Bottom))`로 상단을 뺀다. develop에는 이미 ① 엔트리 `YGScaffold` 기본 인셋(대부분 화면), ② 화면이 직접 `windowInsetsPadding`(C-101 카메라, 의도적 예외)이 있어 세 번째 형태가 추가됐다. 게다가 이 파라미터는 **`Empty` 변형에만** 있어 `Back`·`Detail`·`Canvas`를 쓰는 화면은 같은 관용구를 못 쓴다.
- **항목**: ① 인셋 소유의 기본을 어디로 정할지(엔트리 컨테이너 / 화면 / 컴포넌트), ② 컴포넌트 흡수를 채택하면 나머지 탑바 3변형에도 `windowInsets`를 열지, ③ 흡수형을 쓰는 화면이 `YGScaffold` 상단을 빼는 것을 규약으로 강제할지(빼먹으면 이중 적용인데 컴파일로 못 막는다).
- **상태**: 미해결 (코드 머지됨 — 규약 쪽 결정 필요)
  > 📌 **형태 선택이 화면 정책을 깎았다(2026-08-11, PR #199)** — C-001은 ①(엔트리 `YGScaffold` 기본)을 골랐고, 그 결과 배경 점 격자가 `innerPadding` 안쪽에만 그려져 위키 정책의 "화면 전체 뒤"를 못 지킨다(아래 [2026-08-12] Dot Grid 항목). `YGTopBarCanvas`에 `windowInsets`가 없어 G-001 관용구를 쓸 수도 없으니, ②는 이제 취향 문제가 아니라 **정책 이행을 막는 제약**이다.
  > ⚠️ **③이 실제 결함으로 나타났다(2026-08-25, PR #350)** — 갤러리 권한 거부 화면이 Route의
  > `YGScaffoldV2`가 준 `innerPadding` 위에 `windowInsetsPadding(systemBars)`을 한 번 더 걸어 닫기
  > 버튼이 상태바 높이만큼 내려앉아 있었고, 이슈 #345로 사용자 눈에 먼저 띄었다. **같은 화면의
  > 목록 갈래는 멀쩡했다** — 인셋 소유가 화면 단위가 아니라 **갈래(권한 / 콘텐츠) 단위로 갈릴 수
  > 있다**는 것이 이 항목에 없던 결이다. 게다가 [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)이
  > 2026-08-04에 "인셋 이중 적용이 사라졌다"고 적은 뒤로 안 걷힌 갈래가 세 주를 살아남았다.
  > 같은 라운드가 카메라 권한 화면에서는 **무는 자리**(닫기 `Row` → 바깥 `Box`)를 옮겼다 — ①의
  > "화면이 직접 무는" 형태는 무는 주체까지 정해야 재현 가능한 규약이 된다.
  > 📌 **4형태·5형태가 더 붙었다(2026-08-13, PR #223)** — S-101 그룹 설정 entry가 ①에 **`consumeWindowInsets(innerPadding)`**을 얹는 형태를 도입했다(하위 `imePadding()`이 인셋을 두 번 세지 않게). 소비를 빼면 확인 버튼이 내비게이션 바 높이만큼 떠오르는데 컴파일로 못 막는다 — ③과 같은 성질의 암묵 규약이 하나 더 생긴 셈이다. 대조적으로 `feature/groups/enter/impl`의 세 entry는 `contentWindowInsets = WindowInsets(0.dp)`로 인셋을 끄고 `statusBarsPadding()` + `navigationBarsAndImePadding()`을 직접 붙인다. 즉 develop의 인셋 관용구는 이제 **다섯 형태**다.
- **해소 메모**: 결정 시 [navigation-flow](../architecture/navigation-flow.md) 체크리스트 2번과 [design-system](../architecture/design-system.md) `YGTopBar`·"화면 컨테이너" 절, [ygtopbar 스펙](../superpowers/specs/archive/2026-07-18-ygtopbar.md)을 함께 정리한다. [2026-08-01 화면 컨테이너 규약 이탈 항목](#2026-08-01-g-001-목록-화면이-화면-컨테이너-규약을-벗어남)과 같은 화면에 걸린다.

### [2026-08-07] `animateToppingPlacement`가 사용처 0건으로 머지됨

- **ID**: OQ-P-101
- **출처**: `feature/groups/list/impl/route/component/ToppingLayout.kt#animateToppingPlacement`·`ToppingLayoutDefaults`(PR #194 develop 머지) — 목록 중간 삽입·삭제로 자리가 밀릴 때 순간이동 대신 애니메이션시키는 `Modifier` 확장인데 호출부가 없다. KDoc이 "호출부에서 각 항목을 안정적인 key로 감싸야 항목 이동으로 인식된다"고 전제를 다는데 `ToppingLayout` 호출부는 `fastForEachIndexed`로 key 없이 그린다. 같은 파일의 `ToppingLayout`·`ToppingLayoutDefaults`가 feature `impl` 내부 전용인데도 `public`이라(같은 폴더 `GroupListParfaitLayout`은 `internal`) 가시성도 갈린다.
- **항목**: ① 조회 결선 라운드에서 실제로 붙일지, 아니면 걷어낼지(사용처 0 공개 API를 남긴 선례가 이미 있다 — [2026-08-03 `clickableYGNoRipple` 항목](#2026-08-03-clickableygnoripple-사용처-0--존치-여부). **그 선례는 2026-08-17 #284로 존치 쪽으로 닫혔다** — 사용처 0이던 API가 표준 유틸이 됐다), ② feature `impl` 내부 심볼의 기본 가시성을 `internal`로 못박을지.
- **상태**: 부분 해소 (① 걷어내는 쪽으로 닫힘, 2026-09-20 PR #514 / ② 잔존)
  > ✅ **①이 걷어내는 쪽으로 닫혔다(2026-09-20, PR #514)** — `animateToppingPlacement`와
  > `ToppingLayoutDefaults`가 `ToppingLayout.kt`에서 삭제됐다. `clickableYGNoRipple` 선례와 반대
  > 결말이고, 갈린 이유도 그 선례가 적어 둔 그대로다 — 저쪽은 소비처가 실제로 생겼고 이쪽은
  > 붙일 라운드가 끝내 오지 않았다. 목록 항목을 안정적인 key로 감싸는 호출부 변경도 함께 필요했는데
  > 그 전제가 채워진 적이 없다.
  > ⚠️ **②는 그대로다** — 같은 파일에 남은 `ToppingLayout`이 여전히 `public`이고, 같은 폴더
  > `GroupListParfaitLayout`은 `internal`이다. 이 라운드가 가시성은 손대지 않았다.
- **해소 메모**: ②만 남았다. 결정 시 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) "토핑 배치" 절을 정리한다.

### [2026-08-09] 테스트 기반 구조에 검증되지 않은 표면 3건

- **ID**: OQ-P-102
- **출처**: PR #219 develop 머지([unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md)). 유닛 테스트는 배선·통과했지만 세 표면이 실제로 동작하는지는 증명되지 않았다. ① `MainDispatcherRule`은 사용처가 0건이다 — 배선(`@get:Rule` + `runTest(rule.dispatcher)` 컴파일·통과)까지만 확인했고 `Dispatchers.setMain` 적용·복원과 스케줄러 공유가 무엇을 막아주는지는 미검증이다. 이번 범위(`domain`·`data`·`core:util:*`)에 ViewModel이 없어서다. ② 계측 테스트 2건(`YGThemeSmokeTest`·`ContextExtensionTest`)은 CI에서 `assembleDebugAndroidTest` 컴파일까지만 검증돼 런타임 오류가 드러나지 않는다(**2026-08-25 기준 파일 5개·`@Test` 14건으로 늘었고 조건은 그대로다** — 아래 참고). ③ `core:util:android`는 `parfait-test-unit`이 적용됐지만 unit 테스트가 0개다(내용물이 Compose Modifier·Context/Bitmap 확장이라 대상 없음).
- **항목**: ① 첫 ViewModel 테스트를 쓸 때 룰 자체를 검증하는 테스트를 함께 추가할지 — 계측 소스셋에서 코루틴을 다루려면 `bundles.test-android`에 `kotlinx-coroutines-test`를 넣어야 하고(현재 없고 `:core:testing`도 계측에 미배선), `runTest`를 인자 없이 부르면 스케줄러가 갈려 `advanceUntilIdle()`이 Main 큐를 비우지 못한다. ② CI에 기기·에뮬레이터를 붙일 시점. ③ Android 비의존 로직이 `core:util:android`에 생기는 시점에 채운다.
- **상태**: 미해결 (셋 다 트리거 대기 — ViewModel 등장 / CI 기기 도입 / 대상 로직 추가)
  > 📌 **②의 규모가 일곱 배가 됐는데 실행은 여전히 0회다(2026-08-25, PR #351)** — `YGCanvasTest`
  > 2건이 붙어 계측 소스셋이 **파일 5개·`@Test` 14건**이 됐다(`YGThemeSmokeTest`·`ContextExtensionTest`
  > ·`YGLoadingOverlayTest`·`YGScaffoldV2Test`·`YGCanvasTest`). CI `test.yml`은 그대로
  > `:core:util:android:assembleDebugAndroidTest`·`:core:designsystem:assembleDebugAndroidTest`
  > 두 줄이라 **컴파일만 되고 단언은 한 번도 실행되지 않는다.** 이번에 들어온 둘은 `YGCanvas`의
  > 새 규칙(배경 미설정일 때만 빈 안내판)을 잠그려고 쓴 것이라, 잠갔다고 적기 어려운 상태가
  > 새 규칙 하나를 더 덮는다. ②(CI 기기 도입)의 값어치가 라운드마다 커진다.
  > 📌 **②의 규모가 또 두 배가 됐다(2026-09-04, PR #440 develop 머지)** — 계측이 파일 6·`@Test` 17건에서
  > **파일 11·`@Test` 35건**이 됐다(디자인시스템에 `YGLoadingLottieTest`·`ParfaitImageLoaderTest` 신설 +
  > `YGCanvasTest` 2 → 8 · `YGScaffoldV2Test` 5 → 7 · `YGLoadingOverlayTest` 2 → 3, `core:ui`에
  > `reveal/` 계측 3파일 6건 신설). **`core:ui`가 계측 소스셋을 처음 갖게 됐고**
  > (`parfait.test.android`·`parfait.test.compose`), CI `test.yml`은 여전히 두 모듈의
  > `assembleDebugAndroidTest` 두 줄이라 **새 모듈의 계측은 컴파일조차 안 된다.** 이번에 들어온 것들이
  > 잠그려는 규칙(덮개 아래 접근성 차단·드러나기 전 클릭 차단·재시도 시 캐시 우회)은 전부 **눈으로는
  > 확인이 안 되는 종류**라 ②의 값어치가 또 커졌다.
  > 📌 **처음으로 계측이 "컴파일은 되는" 자리에 들어왔다(2026-09-08, PR #465 develop 머지)** —
  > 계측이 파일 11·`@Test` 35건에서 **파일 12·`@Test` 37건**이 됐다(`core:util:android` 에
  > `ModifierCenteredAtTest` 2건 신설, 이 모듈에 `parfait.test.compose` 가 붙었다). 이 모듈은
  > CI `test.yml` 의 `assembleDebugAndroidTest` 두 줄 중 하나라 **컴파일은 된다** — 그래도 단언은
  > 여전히 실행되지 않는다. 이번에 들어온 계약(부모보다 큰 자식을 `centeredAt` 으로 놓으면 중심이
  > 맞고 `offset` 으로 놓으면 넘친 양의 절반만큼 밀린다)은 **눈으로 보기 전에는 드러나지 않는
  > 배치 규칙**이라 ②의 값어치가 또 커졌다.
  > 📌 **①이 한 라운드 더 버텼다(2026-08-12, PR #230)** — `data` 유닛 테스트가 3건 늘었는데(`ImageRemoteDataSourceImplTest`·`MemberRemoteDataSourceImplTest`·`ParfaitImageRemoteDataSourceImplTest`) **`MainDispatcherRule` 사용처는 여전히 0건**이다. 셋 다 `runTest`만 쓰고 `Dispatchers.Main`을 건드리지 않는다 — 원인은 그대로 "테스트 대상에 ViewModel이 없다"이고 그 조건은 소비처 결선 라운드까지 안 바뀐다.
- **해소 메모**: 해소 시 [unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md) "주의 / 열린 질문" 절의 대응 항목을 지운다.

### [2026-08-09] Repository Fake를 어디에 둘지 미결

- **ID**: OQ-P-103
- **출처**: PR #219 develop 머지 — [unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md)은 공용 Fake를 `:core:testing`에 두기로 설계했으나, 실제로 Fake 2종을 만들어보고(2026-08-06, 커밋 후 되돌림) 대가가 드러나 이번 머지에서 빼기로 했다. `:core:testing`은 `setConfigTestUnit()`을 통해 **모든 대상 모듈의 테스트 classpath**에 걸려 있어 한 Fake를 고치면 무관한 모듈의 테스트까지 재컴파일된다. `domain` repository의 Fake를 `core:testing`이 소유하는 것도 소유권상 어긋난다. 현재 `:core:testing`에는 `MainDispatcherRule`만 있고 `domain` 의존도 없다.
- **항목**: 선택지 셋 중 하나를 고른다. (1) 모듈별 `src/testFixtures` — 소유권 명확·재컴파일 범위 최소, 대신 소스셋이 모듈마다 늘고 AGP `testFixtures` 활성화 필요 / (2) `:core:testing`은 모듈 비종속 유틸만 두고 도메인 Fake는 `testFixtures`로 / (3) 각 모듈 `src/test`에 두고 공유하지 않음 — 소비자가 하나뿐인 Fake가 많다면 중복이 오히려 싸다.
- **상태**: 미해결 (첫 Fake가 실제로 필요해지는 시점에 정한다 — 현재 상태는 어느 쪽도 강제하지 않는다)
- **해소 메모**: 정하면 [module-structure](../architecture/module-structure.md)의 `:core:testing` 행과 스펙 "`:core:testing` 모듈" 절에 반영한다. (2)를 고르면 `domain` 의존이 `api`로 되살아난다.

### [2026-08-10] 온보딩 체인이 화면 전이만 결선 — 인증·동의 저장이 통째로 빠져 있다

- **ID**: OQ-P-104
- **출처**: PR #220 develop 머지 — `feature/login/impl` `LoginRoute.kt`·`LoginViewModel.kt`, `feature/intro/impl` `termagree/TermAgreeRoute.kt`. `Splash → Login → TermAgree → GroupList`가 이어졌지만 세 구멍이 그대로다. ① 카카오 로그인 성공 토큰은 `LoginState.token`에만 담기고 서버 `POST /api/v1/auth/login`·`/auth/signup` 호출도, `TokenStore` 저장도 없다 — [ADR-0019](../adr/0019-encrypted-token-storage.md)의 저장 경로는 여전히 호출자 0건이다. ② 서버 로그인 응답이 신규/기존 회원을 가르는데(`KakaoLoginResponse`의 `newUser` 판별자, [api/auth.md](../api/auth.md)) 화면은 분기 없이 **누구나 매번 약관 화면**을 지난다. ③ `TermAgreeViewModel`의 동의 저장은 여전히 `// Todo`라 `signup`이 필수로 받는 `agreements[].termsId`를 만들 자리가 없다(약관 목록도 `TERM_CONTENT_LIST` 리터럴).
- **항목**: ① 서버 인증을 어느 단계에 넣을지 — 카카오 토큰 획득 직후 `login` 호출 후 `newUser`로 약관/그룹목록을 가를지, 아니면 약관 동의까지 받고 `signup` 한 번으로 끝낼지. ② ①이 정해져야 `clearBackStack()` 리셋 지점(현재 약관 → 그룹목록)이 맞는지도 확정된다 — 기존 회원이 약관을 건너뛰면 리셋 지점이 로그인 쪽으로 올라간다. ③ `termsId` 출처를 `GET /api/v1/policies` 연동으로 세우는 건([2026-08-03] 항목)이 이 체인의 선행 조건인지.
- **상태**: 해소됨 (2026-08-15, PR #241·#242 — 세 구멍이 모두 닫혔다)
  > ✅ ① 서버 인증이 카카오 토큰 획득 직후 `POST /auth/kakao`로 들어갔고(#241), ② `isNewUser` 분기로
  > 기존 회원은 목록·신규는 약관으로 갈리며, ③ 약관 화면이 `GET /api/v1/policies`로 목록을 받아
  > `POST /auth/signup`에 `agreements[].termsId`를 실어 보내고 세션까지 저장한다(#242).
  > **①의 답은 "둘 다"였다** — 로그인에서 한 번, 약관 동의 후 가입에서 한 번. ②(리셋 지점)도
  > 그에 따라 둘로 갈렸다(로그인·약관 각각) → [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인".
  > 남은 것은 **실서버로 한 번도 안 돌았다는 것**(OQ-P-146)과 실패 표현(OQ-P-167)이다.
  > 📌 **같은 패턴이 그룹 플로우에서 반복됐다(2026-08-12, PR #224)** — 그룹 생성·참여도 화면 전이만 결선되고 서버 호출은 mock UseCase로 대체됐다. 온보딩 체인과 이 플로우는 **같은 결선 라운드를 기다린다** → [2026-08-12] mock UseCase 항목.
  > 📌 **화면만 더 채워졌다(2026-08-11, PR #218)** — A-002가 일러스트·문구를 실물로 얻었지만 ①②③ 구멍은 그대로다. 게다가 카카오 로그인 실패·취소는 **로그만 남기고 화면 표현이 0**이라, 실연동이 붙으면 에러 표현부터 새로 설계해야 한다 → [a002-login-onboarding 스펙](../superpowers/specs/archive/2026-08-11-a002-login-onboarding.md).
- **해소 메모**: 해소 시 [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인"·[intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md)·[ADR-0019](../adr/0019-encrypted-token-storage.md) 검증 절을 함께 갱신한다.

### [2026-08-10] `ResultEventBus` 왕복을 검증하던 유일한 화면이 사라졌다

- **ID**: OQ-P-105
- **출처**: PR #220 develop 머지 — `feature/groups/home/{api,impl}` 모듈 삭제(`NavKeyGroupHome`·`GroupHomeRoute`·`EntryBuilder`·`NavigationModule`)와 `LoginRoute`의 `ResultEffect<String>` Toast 제거. 둘은 짝이었다(홈이 `sendResult` + `onBack`, 로그인이 `ResultEffect`로 수신). `MainRoute`의 `rememberResultEventBusNavEntryDecorator`는 그대로 남고, 남은 실사용은 카메라·시스템 갤러리의 `sendResult` 3곳 + `CanvasMainRoute`의 `ResultEffect` 1곳인데 그 수신부는 이미 死경로로 등록돼 있다([2026-08-04] 항목).
- **항목**: ① 결과 반환 관용구를 계속 쓸지 — 커스텀 갤러리·카메라는 이미 `goTo` 전진으로 갈아탔고 남은 소비처가 死경로뿐이라, 데코레이터째 걷어낼지 아니면 재사용처를 확정할지. ② 걷어낸다면 `Navigator.onBack()`의 `size <= 1` 가드 주석("`ResultEffect` 발동 상황에서 사이즈가 1인 경우 크래시")이 가리키는 전제도 같이 정리한다.
- **상태**: 부분 해소 (① **PR #221 develop 머지, 2026-08-14** — 실사용 왕복이 하나 되살아났다. 토핑 편집 화면이 `sendResult(TOPPING_EDIT_RESULT_KEY, ToppingEditResult)` + `onBack()`으로 결과를 돌려주고 `SegmentationConfirmRoute`가 `ResultEffect<ToppingEditResult>`로 받는다. **NavKey가 담지 못하는 "나올 때의 값"이라 이 관용구를 다시 고른 것**이므로 데코레이터째 걷어내는 선택지는 사실상 닫혔다. / ② 가드는 여전히 필요하다 — 그룹 목록에서 백스택이 1개다. / [2026-08-04]의 死 `ResultEffect` 1건은 그대로다)
- **해소 메모**: ①이 닫혔으니 남은 것은 死 수신부 정리([2026-08-04] 항목)뿐이다. [navigation-flow](../architecture/navigation-flow.md) 체크리스트 5번에 "되살아난 사례" 마커를 넣었고 "토핑 생성 플로우" 절에 왕복 경로를 적었다.

  > 📌 **두 번째 실사용 왕복(2026-08-15, PR #231)** — C-101-confirm이 `returnResultOnly`일 때 `PictureConfirmResult`를 돌려주고 C-301이 받는다. 같은 화면이 전진(`goToAndPopCurrent`)과 반환을 **인자 하나로 겸하는** 형태라, 데코레이터 존치는 확정으로 봐도 된다.

### [2026-08-10] 이미지 업로드 확인 API에 소유자 검증이 없다

- **ID**: OQ-P-106
- **출처**: TEAMYG-SERVER `main` `5bb2a3a`([api/image.md](../api/image.md)) — `POST /api/v1/images/{imageId}/confirm`. `ConfirmImageUploadController.confirm`이 `@PathVariable imageId`만 받고 `Authentication`을 받지 않는다. `ConfirmImageUploadCommand`에도 memberId가 없고 `ConfirmImageUploadService`는 `imageMetaQueryPort.findById` 결과의 `uploadedByMemberId`를 대조하지 않는다. 화이트리스트 밖이라 토큰은 필요하지만, **유효한 토큰을 가진 아무 회원이나 남의 `imageId`를 `COMPLETED`로 올릴 수 있다.** id가 auto-increment Long이라 값 추측도 쉽다. 발급 API(`POST /api/v1/images`)는 반대로 memberId를 확실히 쓴다(S3 키에 `user<memberId>`가 들어간다).
- **항목**: ① 서버 이슈로 올려 confirm에도 소유자 대조를 넣을지 — 넣으면 남의 이미지 확정 시 새 에러 코드(403 계열)가 생기고 앱의 에러 매핑이 늘어난다. ② 앱이 붙기 전에 서버가 고칠 것이라 보고 [api/image.md](../api/image.md)에 관측 사실로만 남길지. ③ confirm 재시도로 받는 `IMAGE_ALREADY_CONFIRMED`(409)를 앱이 성공으로 간주할지 — 소유자 검증이 없는 현재 상태에서는 **409가 "내가 이미 했다"인지 "남이 했다"인지 구분되지 않는다**. 검증이 들어오면 이 모호함도 같이 사라진다.
- **상태**: 미해결 (서버 소관 — 앱 연동 착수 전에 확인 필요)
  > 📌 **앱 표면이 ③을 코드로 고정했다(2026-08-12, PR #230)** — `ImageRemoteDataSource.confirmUpload`가 `IMAGE_ALREADY_CONFIRMED`를 성공으로 번역하지 **않고** `ApiException.Business`로 흘리며, 그 판단을 테스트가 잠근다. 사유는 이 항목 ③ 그대로다 — 소유자 검증이 없어 409의 뜻이 갈리지 않는다. ①②는 서버 소관이라 잔존.
- **해소 메모**: 서버가 고치면 [api/image.md](../api/image.md) confirm 절의 ⚠️ 두 개(소유자 미검증·409 해석)와 [api/conventions.md](../api/conventions.md) "인증" 절 말미를 함께 지운다.

### [2026-08-10] 이미지 업로드 계약의 미사용 필드·정리 경로 부재

- **ID**: OQ-P-107
- **출처**: TEAMYG-SERVER `main` `5bb2a3a`([api/image.md](../api/image.md)) — 두 건이다. ① `IssueImageUploadUrlRequest.fileName`이 `@NotBlank` 필수인데 `toCommand`가 싣지 않아 서버 로직에 닿지 않는다. S3 키는 `ImageKeyGenerator`가 UUID로 만들고 확장자는 `contentType`에서 유도하므로 원본 파일명은 어디에도 저장되지 않는다 — 앱은 **쓰이지 않을 값을 반드시 채워야** 400을 면한다. ② 발급 시점에 `ImageMeta.createPending`이 행을 먼저 만드는데, 앱이 S3 PUT을 하지 않거나 confirm을 부르지 않으면 `PENDING` 행과 S3 키가 그대로 남는다. 서버에 `@Scheduled`가 0건이고 `ImageMetaRepository`는 `JpaRepository` 기본 메서드뿐이라 **정리 경로가 없다**. `referenceCount` 컬럼도 도메인·엔티티에만 있고 증감 코드가 없다.
- **항목**: ① `fileName`을 계약에서 뺄지, 아니면 메타 컬럼·S3 키에 반영할지(서버 결정). 앱은 그때까지 더미가 아닌 실제 파일명을 보내 둔다 — 나중에 서버가 쓰기 시작해도 값이 맞는다. ② 고아 `PENDING` 정리를 서버 배치로 둘지, 앱이 실패 시 취소 API를 부르게 할지(현재 취소 API 없음). ③ `referenceCount`를 누가 증감할지 — 토핑·캔버스가 이미지를 참조하기 시작하는 시점의 계약이다.
- **상태**: **부분 해소** (③ 해소 — ①② 잔존, 둘 다 서버 소관이나 ①은 앱 요청 바디 구성에 즉시 영향)
  > 📌 **③ 해소(2026-08-15, 서버 `36ecd1c`)** — `referenceCount` 증감 주체가 정해졌다. **토핑 배치(POST)가 +1,
  > 토핑 삭제(DELETE)가 -1**이고 **0이 되면 S3 객체를 지운다**(`ImageDeleteAdapter`) →
  > [api/parfait-image.md](../api/parfait-image.md). 대신 **새 공백이 생겼다** — 카운트가 0이어도
  > `image_meta` 행은 `COMPLETED`로 남아, S3 객체 없는 그 `imageId`를 다시 배치하면 상태 검사를 통과해
  > 깨진 이미지가 걸린다. ②(고아 `PENDING` 정리)는 그대로다 — 같은 delta에 서버 첫 스케줄러(캔버스 회전)가
  > 들어왔지만 이미지 정리는 그 대상이 아니다.
- **해소 메모**: 해소 시 [api/image.md](../api/image.md) 요청 필드 표의 ⚠️와 "미결" 절을 갱신한다.
  ③은 [api/image.md](../api/image.md) confirm 절과 [api/parfait-image.md](../api/parfait-image.md)에 반영했다.

### [2026-08-10] `http/` 요청 모음이 서버 신규 엔드포인트 2건을 덮지 못한다

- **ID**: OQ-P-108
- **출처**: 서버 delta `5bb2a3a`로 엔드포인트가 16개가 됐는데 TJYG-Android 루트 `http/`에는 `images.http`가 없었다(당시 `auth`·`policy`·`parfait-group`·`parfait`·`health` 5개 파일 = 14 엔드포인트). PR #197 시점의 "전량 커버"가 깨졌다. 이는 [2026-08-04] `http/`↔`api/` 이중 관리 항목이 예고한 갈라짐이 **처음 실제로 발생한 사례**다.
- **항목**: [2026-08-04] 항목의 선택지 ①(스킬이 `http/`도 갱신)·②(`http/`를 실행 방법으로만 축소) 중 무엇을 고를지. 갱신 경로가 둘이라는 구조 자체는 그대로다.
- **상태**: 미해결 (**2026-08-16 서버 delta로 다시 25/27** — 다섯 번째 왕복, 구조 결정은 그대로.
  이번엔 `:data` 표면만 닫히고 `http/`는 안 닫혀 **두 표면이 처음으로 갈렸다**)
  > ✅ **일곱 번째 왕복이 닫혔다 — 26/29(2026-09-04, PR #451 `2b1dce3a`)** — `notifications.http`가 신설돼 기기 토큰 등록을 덮고 `http-client.env.json`·`_reset.http`·`http/README.md`도 같은 PR에서 함께 갱신됐다. **앞선 여섯 번과 방향이 반대다** — 지금까지는 요청 모음이 코드 표면보다 뒤처졌는데, 이번엔 **요청 모음이 앞서 나갔다**(등록을 부를 수단이 develop에 0건이라 앱은 못 부르지만 이 파일은 손으로 돌릴 수 있다, OQ-P-341). **일곱 번째도 사람이 손으로 메운 것**이라 [2026-08-04]의 "갱신 경로가 둘"은 그대로다.
  > ⚠️ **왕복이 반만 닫혔다(2026-08-16, PR #266)** — 같은 두 엔드포인트에 Service·DataSource가 붙었는데
  > `http/parfait.http`는 그대로다. **앞선 네 번은 표면과 요청 모음이 한 라운드에서 함께 메워졌다** —
  > "손으로 메운다"가 유지되던 근거(같은 사람이 같은 라운드에 둘 다 만진다)가 이번에 깨졌다.
  > 요청 모음이 코드 표면보다 뒤처지는 형태는 [2026-08-04] "갱신 경로가 둘" 항목이 예고한 그대로다.
  > 📌 **재발(2026-08-16, 서버 `22717fe`)** — `parfait.http`에 **상세 조회·배경 변경** 두 요청이 없다.
  > 배경 변경은 특히 손으로 쏴 볼 값이 많다(HEX 형식·조건부 필수·업로드 확인 상태) — 요청 모음이
  > 없으면 계약을 확인할 수단이 스웨거뿐이다.
  > ✅ **공백 해소(2026-08-15, PR #250)** — 같은 라운드가 다섯 요청을 채웠다: `parfait.http`에 오늘의 캔버스·
  > 과거 목록, `parfait-image.http`에 테두리 수정·삭제, `users.http`에 탈퇴. `http-client.env.json`·
  > `_reset.http`에 `parfait_id`가 등재돼 **`parfait-image.http`가 `parfaitId` 리터럴을 손으로 바꾸던 단계가
  > 사라졌고**(오늘 조회 응답 핸들러가 채운다) 선행이 셋에서 넷이 됐다. `README.md`에는 두 DELETE의 성공
  > 표현이 다르다는 함정과 **파일을 통째로 순서 실행하면 계정·데이터가 지워진다**는 경고가 붙었다.
  > **다만 이번에도 사람이 손으로 메웠다** — 벌어졌다 닫히는 왕복이 네 번째이고 [2026-08-04] "갱신 경로가
  > 둘" 항목은 그대로 열려 있다.
  > 📌 **재발(2026-08-15, 서버 `36ecd1c`)** — 파르페 오늘 조회·과거 목록 · 토핑 테두리 수정·삭제 · 회원 탈퇴 5건에 요청이 없다. 테스트 전용 회전 1건은 앱 대상이 아니라 세지 않는다. **이 반복 자체가 [2026-08-04] "갱신 경로가 둘" 항목의 결정 근거다** — 세 번 연속 사람 손으로 메웠고 세 번 다 다음 delta에서 깨졌다.
  > 📌 **2026-08-11 서버 delta로 7건 공백**(21 엔드포인트 중 14만 덮임) — 애플 로그인 1 · member 2 · parfait-image 2가 새로 비었고, image 2는 `images.http`가 **아직 미머지 브랜치에만** 있다. 손으로 메우는 방식이 서버 delta 한 번에 다시 무너졌다 — [2026-08-04] 항목의 구조 결정을 더 미룰 근거가 없다.
  > 📌 **공백 해소(2026-08-12, PR #230)** — `images.http`가 develop에 들어오고 `users.http`·`parfait-image.http`가 신설됐다. 애플 로그인 1건은 Android 미사용 결정이라 대상이 아니므로 **요청 모음이 Android가 쓰는 20 엔드포인트 전량을 덮는다.** `http-client.env.json`에 `image_id`·`image_upload_url`·`parfait_image_id` 3변수, `_reset.http`에 도메인별 비우기 항목 2개(`0-1-2`·`0-1-3`)가 짝으로 붙었다.
  > ⚠️ **대신 같은 PR이 `http/` 안에 모순을 하나 만들었다** — `http/README.md`의 판별자 키 서술만 `isNewUser`로 정정되고 `http/auth.http` 주석·응답 핸들러는 `newUser` 그대로다 → [2026-08-11] 판별자 키 항목 ④.
- **해소 메모**: `image-api-service-layer` 라운드가 `images.http`를 신설해 **16/16 커버를 회복**했고(브랜치 `feature/sync-backend-api-260810` — **2026-08-12 PR #230으로 머지**, 같은 PR이 `users.http`·`parfait-image.http`까지 얹어 20/20이 됐다) `http/README.md` 5곳·`http-client.env.json`·`_reset.http`도 함께 갱신했다. S3 PUT 요청도 같은 파일에 뒀다 — 서버 계약이 아니라 AWS 계약이지만, `Content-Type` 불일치로 S3가 거절하는 실패는 **서버 로그에 남지 않아** 이 파일이 재현할 유일한 자리라서다. 다만 이번엔 **사람이 손으로 메운 것**이고 [2026-08-04]의 "갱신 경로가 둘"은 그대로라, 다음 서버 delta에서 같은 일이 반복된다. 결정 후 [api/README.md](../api/README.md) "계약을 실제로 확인하는 법" 절의 ⚠️와 [2026-08-04] 항목을 함께 닫는다.

### [2026-08-10] presigned `uploadUrl`이 debug 로그로 새어나간다

- **ID**: OQ-P-109
- **출처**: `data/di/NetworkModule.kt#provideOkHttpClient` × `data/service/ImageService.kt#postImages`(**2026-08-12 PR #230으로 develop 머지**) — 로깅 인터셉터가 `BuildConfig.DEBUG`에서 `Level.BODY`이고 `redactHeader("Authorization")`은 **헤더만** 가린다. 발급 응답 본문의 `uploadUrl`은 `X-Amz-Signature`를 포함한 **그 자체가 자격증명**이다(만료 전까지 누구나 그 버킷 키에 PUT할 수 있다). 응답 바디 전량이 logcat에 찍힌다. 기존 14 엔드포인트에는 본문에 자격증명을 싣는 응답이 없어 **이번에 처음 생긴 성질**이다.
- **항목**: ① debug 로그 레벨을 `BODY`로 유지할지, 아니면 이미지 도메인만 응답 본문을 가릴지(OkHttp 로깅 인터셉터에는 바디 redact 기능이 없어 커스텀 인터셉터가 필요하다). ② 아니면 debug 빌드 한정 + `expiresIn` 만료라는 이중 제한으로 충분하다고 볼지.
- **상태**: 해소됨(PR5)
  > 📌 **마감이 정해졌다(2026-08-20, PR1 최종 리뷰)** — "실연동 라운드"가 가리키는 라운드는
  > [c106-topping-place-api](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) 스택의 **PR5**(화면 결선)다.
  > PR1(업로드 전송 계층)은 `ImageUploadRepository`를 만들었지만 그것을 부르는 코드가 0건이라
  > **런타임에 presigned URL을 실제로 받아 오는 첫 시점이 PR5**다. 그때까지 logcat에 찍히는 것은
  > 지금과 같이 아무것도 없다. 커스텀 인터셉터(OkHttp 로깅 인터셉터에 바디 redact가 없다)는
  > PR1 계획 범위 밖이라 미뤘고, **PR5 계획이 이 항목을 선행 조건으로 실어야 한다.**
  > 참고: 같은 라운드가 **업로드 전용 클라이언트**에서는 로깅 인터셉터를 아예 제거했다
  > ([ADR-0017](../adr/0017-remote-network-datasource.md) "로깅" 절과 별개 표면).
- **해소 메모**: `@NoBodyLog` + `SelectiveLoggingInterceptor`로 발급 엔드포인트만 `Level.HEADERS`로
  낮췄다. 전체 레벨을 낮추지 않은 이유는 본문 로깅이 다른 엔드포인트에서는 값이 크기 때문이고,
  `BASIC`이 아니라 `HEADERS`인 이유는 새는 값이 **응답 본문에만** 있어 헤더까지 버릴 이유가 없기
  때문이다. 결정 시 [ADR-0017](../adr/0017-remote-network-datasource.md) "로깅" 절에 반영한다.

### [2026-08-10] `image`라는 이름이 domain에서 기기 이미지 뜻으로 선점돼 있다

- **ID**: OQ-P-110
- **출처**: `image-api-service-layer` 라운드 최종 코드리뷰. `data/source/image/`는 `remote`/`local` 하위 구분이 출처를 갈라 문제가 없지만(저장소 규칙이 "폴더=도메인, 하위=출처"), **`domain/`에는 출처 축이 없다.** 그리고 거기서 `image`는 이미 기기 이미지 뜻이다 — `domain/repository/image/`에 `RecentImageRepository`(기기 캐시)·`ImageSegmentationRepository`(누끼 분할), `domain/usecase/image/`에 `DecodeImageUseCase`·`SegmentImageUseCase`·`AddRecentImageUseCase`·`GetRecentCacheImagesUseCase`가 있고 넷 다 기기 측이다. 이번에 `domain/model/image/`(서버 업로드)가 그 옆에 들어왔다. [image-api-service-layer 스펙](../superpowers/specs/archive/2026-08-10-image-api-service-layer.md)은 `GalleryImageGroup`만 검토했고 이 두 패키지는 짚지 못했다.
- **항목**: 다음 라운드가 `ImageRepository`·`UploadImageUseCase`를 만들면 **기기 이미지 심볼들과 같은 패키지에 앉는다.** ① 서버 업로드 쪽을 다른 이름으로 가를지(`imageupload`·`upload`), ② 기기 쪽을 `gallery`·`localimage`로 개명할지(카메라·갤러리 feature가 소비 중이라 파급이 크다), ③ 그대로 두고 클래스명으로만 구분할지. **다음 라운드가 이름을 정하기 전에 결정돼야 한다** — 나중에 바꾸면 소비자가 늘어난 뒤다.
- **상태**: 해소됨 (2026-08-20 develop 머지, PR #322)
  > ✅ **③으로 정해졌다** — 폴더를 가르지 않고 클래스명으로 구분한다. 서버 업로드 Repository는
  > `domain/repository/image/ImageUploadRepository`로, 기기 쪽 심볼들과 같은 패키지에 앉는다.
  > 근거: `domain/model/image/`가 이미 서버 업로드 모델을 담고 있어 그쪽과 이름이 맞고
  > `data/source/image/`와도 대칭이다. ①(`imageupload` 별도 폴더)은 같은 것을 부르는 이름이
  > 계층마다 갈리고, ②(기기 쪽 개명)는 카메라·갤러리 feature 소비처가 많아 이 라운드 밖이다.
  > **요청의 `ImageType`이 `NUKKI`·`BACKGROUND` 둘이라 이름에 `Topping`을 붙이지 않았다** —
  > C-301 배경 업로드가 같은 Repository를 쓴다.
- **해소 메모**: 기존 `RecentImageLocalDataSource` 이름이 부정확하다는 지적(같은 라운드 스펙 미결)이 이 항목의 부분집합이다. 결정 시 [module-structure](../architecture/module-structure.md)와 [data-layer](../architecture/data-layer.md)에 반영한다.

### [2026-08-10] `ImageUploadUrlVO.expiresIn`이 상대값이라 만료 판정에 쓸 수 없다

- **ID**: OQ-P-111
- **출처**: `domain/model/image/ImageUploadUrlVO.kt`(**2026-08-12 PR #230으로 develop 머지**) — 서버가 주는 초 단위 `Long`을 `Duration`으로 바꿔 담는다(`AuthSessionVO.expiresIn` 선례). 그런데 **발급 시각이 어디에도 기록되지 않는다** — 매퍼도 응답 도착 시각을 남기지 않는다. "이 `uploadUrl`이 아직 유효한가"를 물으려면 호출부가 별도로 시각을 잡아야 한다.
- **항목**: ① VO에 발급 시각(또는 만료 시각 절대값)을 실을지. ② 아니면 재발급을 **만료 판정 없이 실패 시 재시도**로 처리할지(S3가 만료된 URL을 거절하면 발급부터 다시). ②가 단순하지만 실패 한 번을 반드시 치른다. 관련: `contentType`이 발급 요청과 PUT 헤더 **두 곳에 각각 전달**되는데 서명 대상이라 어긋나면 서버 로그에 안 남는 실패가 난다 — 지금 타입으로는 아무것도 강제되지 않는다([스펙 미결](../superpowers/specs/archive/2026-08-10-image-api-service-layer.md)의 `contentType` 열거형화가 같은 라운드다).
- **상태**: 미해결 (소비자 0건이라 현재 무해 — 재발급 흐름 설계 시점에 정한다)
- **해소 메모**: 결정 시 VO 시그니처가 바뀌므로 [api/image.md](../api/image.md) Android 매핑 절도 함께 갱신한다.

### [2026-08-11] G-001 실패 화면이 도달 불가 — `isError`를 세우는 코드가 없다

- **ID**: OQ-P-112
- **출처**: `feature/groups/list/impl/route/GroupListErrorScreen.kt`·`GroupListRoute.kt`·`GroupListViewModel.kt`(PR #222 develop 머지) — 화면·상태 필드(`GroupListUiState.isError`)·Route 분기가 모두 있는데 `isError = true`가 나타나는 자리는 **`GroupListErrorScreen` 프리뷰 하나뿐**이다. 조회가 없으니 실패도 없어서 실기기에서는 이 화면이 뜨지 않는다. 같은 라운드의 `Refresh` 인텐트도 조회 없이 고정 지연 후 `isRefreshing`을 되돌리는 mock이라, 문구가 안내하는 "아래로 당겨 다시 시도"가 실제로 재시도하는 것이 없다. 앞선 死경로 선례(`ClickTopping`·`animateToppingPlacement`, [2026-08-07])와 같은 형태다.
- **항목**: ① 조회가 붙기 전에 UI만 먼저 머지하는 것을 계속 허용할지 — 허용한다면 도달 불가 상태를 어디에 기록할지(현재는 스펙·이 문서뿐이고 코드에는 표시가 없다). ② 실패 판정의 소유 — 조회 결선 시 `isError`를 VM이 예외에서 파생할지, 로딩·성공·실패·0건을 **sealed 상태 하나**로 접을지(지금은 `isError`·`isRefreshing`·`groupList` 세 필드가 독립이라 "로딩 중 실패" 같은 조합이 표현되지 않는다). ③ 재시도 수단을 pull-to-refresh 하나로 둘지(문구가 그것만 안내한다).
- **상태**: 해소됨 (2026-08-15, PR #248 — 조회 실패가 `isError`를 세운다. ②의 상태 모델링 선택만 메모로 남긴다)
- **해소 메모**: ①은 이번 라운드로 닫혔고(도달 불가 기간은 약 5일이었다), ③은 pull-to-refresh 하나로 확정됐다.
  ②는 **세 필드 독립 유지**로 결론 났다 — `isError`·`isRefreshing`·`groupList`가 그대로이고 VM이 실패 시
  `isError = true`만 세운다. 그래서 **성공한 목록이 남아 있어도 재조회가 실패하면 전면 에러 화면으로 바뀐다**
  (코드 주석이 "실패를 알릴 다른 자리가 없다"고 근거를 적는다) — 부분 실패 표현은 OQ-P-167로 옮겼다.
  반영처: [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) "에러·새로고침"·정책 대조 표.
  > 🔁 **그 규칙은 뒤집혔다(2026-08-17, PR #297)** — 재진입마다 조회가 나가게 되면서 "실패 = 전면 교체"가
  > **뒤로 온 것만으로 목록이 사라진다**가 됐다. 이제 목록이 남아 있으면 화면을 유지하고
  > `isError`는 `groupList.isEmpty()`일 때만 선다. "실패를 알릴 다른 자리"는 토스트로 생겼고
  > (`ShowRefreshError`, 당긴 새로고침에만) 세 필드 독립은 그대로다
  > → [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md).
  > 🔁 **한 번 더 뒤집혔다(2026-09-04, PR #440 develop 머지)** — 갈림의 기준이 "목록이 남아 있는가"에서
  > **"사용자가 당겼는가"**로 옮겨 간다. 당긴 새로고침 실패는 목록이 있어도 전면 에러 화면이고
  > (당기는 동안 화면이 이미 목록을 비우므로 실패를 받아 줄 자리가 거기뿐이다), 재진입 조회 실패만
  > 목록을 남긴다. `ShowRefreshError` 토스트는 도달 불가가 되어 걷혔다. 세 필드 독립은 여전히
  > 그대로이나 `isError`에 규칙이 하나 붙었다 — **실패는 켜기만 하고, 끄는 것은 성공한 조회뿐**이다
  > (끄면 실패한 재진입 조회가 앞선 실패를 덮어 낡은 목록이 표시 없이 돌아온다) → OQ-P-348.
  > 즉 ②가 물었던 "세 필드 독립을 유지할 것인가"는 **세 번 뒤집히는 동안 한 번도 안 바뀌었다** —
  > 바뀐 것은 매번 `isError`를 세우는 조건뿐이다.

### [2026-08-11] 로딩 표현이 위키 정책의 "자체 로딩 그래픽"과 다르다

- **ID**: OQ-P-113
- **출처**: `feature/groups/list/impl/route/component/GroupListPullToRefreshBox.kt`(PR #222 develop 머지) — Material3 `PullToRefreshBox` 기본 인디케이터를 그대로 쓴다(래퍼가 더한 것은 `graphicsLayer`로 콘텐츠를 함께 내리는 동작뿐). 위키 [[무한-파르페-그리드]] 상태 규칙은 **초기 로딩을 "인디케이터·스켈레톤 대신 제작한 자체 로딩 그래픽"**으로 못박았다. 두 가지가 갈린다: ① 초기 로딩 상태가 코드에 아예 없고, ② 새로고침 표현이 플랫폼 기본이다. 함께 머지된 에러 문구(`strings.xml` `group_list_error`)도 위키에 대응 정책 소스가 없어 **코드가 먼저 확정**한 상태다(툴팁 문구와 같은 성격).
- **항목**: ① 자체 로딩 그래픽의 적용 범위 — 초기 로딩만인지 pull-to-refresh 인디케이터까지인지(후자면 `PullToRefreshBox`의 `indicator` 슬롯 교체가 필요하다). ② 파르페 메타포 로딩 에셋을 디자인에서 받아야 한다. ③ 에러 문구·컵 그래픽 구성을 정책 소스로 역수집할지, 코드를 정본으로 인정할지.
- **상태**: 미해결 (① ②는 디자인 입력 대기, ③은 위키 ingest 판단)
  > 📌 **조회가 붙어도 ①은 그대로다(2026-08-15, PR #248)** — 진입 시 실제 조회가 도는데 **초기 로딩 상태
  > 필드가 없다**(`isRefreshing`은 당김 전용). 첫 조회 동안 화면은 빈 파르페를 그리다 목록으로 바뀐다.
  > 즉 "자체 로딩 그래픽"을 붙일 자리조차 아직 없다.
  > 📌 **①의 전제가 미머지 브랜치에서 깨졌다(2026-09-02)** — `feature/image-loading-placeholder`가
  > 초기 로딩과 이미지 로딩을 `YGScaffoldV2`의 덮개 하나로 잇는다. 붙일 자리가 생겼다는 뜻이지
  > 위키가 말한 "자체 로딩 그래픽"이 붙었다는 뜻은 아니다 — 붙은 것은 공통 로딩 로띠다.
  > 머지 시 OQ-P-346과 함께 본다.
  > 📌 **①과 ③이 답을 얻었다(2026-09-03에 브랜치로 확인, 2026-09-04 PR #440 develop 머지)** — 디자인이 두 상태를 각각
  > 그려 둔 것을 확인했다(Figma 파일 `QPoxqbNMNktsi8ktua3gMN`, 두 프레임 모두 이름이 `G-001-Error`다:
  > 노드 `2019:10223`이 새로고침 중, `1972:4873`이 실패). **①은 pull-to-refresh까지다** — 다크 로띠가
  > `PullToRefreshBox`의 `indicator` 슬롯에 들어가고 그 아래 24px 자리에 안내 문구 2줄이 붙는다.
  > **③은 디자인이 정본**이라는 쪽으로 기운다 — 실패 프레임이 `group_list_error`와 같은 문구를 담고
  > 있어 코드가 먼저 확정한 것이 아니었다. 다만 **새로고침 문구는 아직 어느 정책 소스에도 없다**.
  > 남는 것은 ②(파르페 메타포 에셋)뿐이고, 붙은 것은 여전히 공통 로딩 로띠다. 자세한 것은 OQ-P-348.
  > 📌 **머지된 뒤에도 ②는 그대로다(2026-09-04, PR #440)** — G-001이 쓰는 것은 초기 덮개도
  > 새로고침 인디케이터도 **공통 애셋**(`YGLoadingArt.Light`/`Dark`)이다. 같은 라운드에 **화면 전용
  > 로띠가 처음 생겼는데 그것은 C-001 몫이다**(`YGLoadingArt.Topping`, `res/raw/loading_topping`) —
  > 위키가 전용 그래픽을 요구한 화면은 G-001인데 받은 화면은 캔버스라 **어긋난 방향으로 채워졌다.**
  > ①은 닫혔다(pull-to-refresh까지 자체 표현이 됐다). ③은 새로고침 문구만 남는다 — 실패 문구는
  > 디자인이 정본이라는 것이 확인됐고, 새로고침 문구(`group_list_refreshing`)는 여전히 Figma에만 있다.
- **해소 메모**: 확정 시 [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 정책 대조 표의 로딩 행을 갱신하고, 문구 정책이 수집되면 위키 쪽 미결과 함께 닫는다.

### [2026-08-11] CI 빌드 성능 후속 2축 — `org.gradle.parallel` 재도입과 configuration cache

- **ID**: OQ-P-114
- **출처**: PR #227 develop 머지([ci-gradle-cache-seeding 스펙](../superpowers/specs/archive/2026-08-10-ci-gradle-cache-seeding.md)) — 캐시 시딩은 들어갔고 효과도 확인됐다(PR `unit-test` 6m16s → 2m45s). 초안에 있던 `org.gradle.parallel`·힙 상향·`kotlin.daemon.jvmargs`는 **의도적으로 되돌렸고**, configuration cache는 처음부터 범위 밖이었다. 둘 다 "안 하기로" 한 것이 아니라 **별건으로 미룬 것**이다.
- **항목**: ① `parallel` 재도입 여부 — 다시 켠다면 검증을 `test` 그래프 하나로 끝내지 말고 `assembleRelease`·`lint`까지 돌려야 한다(미선언 모듈 간 의존은 태스크 그래프마다 다르게 나타나고 릴리스 간헐 실패는 "플래키"로 오진되기 쉽다). 힙과 `kotlin.daemon.jvmargs`가 함께 가야 한다 — 적지 않으면 Kotlin 데몬이 Gradle 데몬 `-Xmx`를 상속해 조용히 2배 예약이 된다. ② configuration cache를 CI에서 살릴지 — `setup-gradle`의 `cache-encryption-key` 입력 + repo secret 생성이 필요하고 Crashlytics·google-services 플러그인 호환을 따로 검증해야 한다. 지금 상태로는 매 런 새 러너인 CI에서 이득이 0이다.
- **상태**: 미해결 (효과 측정이 끝난 뒤 별건으로 — 캐시 변경과 섞으면 원인을 못 가른다)
- **해소 메모**: 착수 시 [ci-gradle-cache-seeding 스펙](../superpowers/specs/archive/2026-08-10-ci-gradle-cache-seeding.md) "검토했다가 뺀 것"·"범위" 절을 근거로 삼고, 결과를 새 스펙으로 분리한다.

### [2026-08-11] GitHub Actions Node 20 deprecation 경고

- **ID**: OQ-P-115
- **출처**: PR CI 런 로그(진단 시점 `31323149207`·`31323027198`, 머지 후에도 동일) — `actions/checkout@v4`·`actions/setup-java@v4`·`actions/upload-artifact@v4`·`dorny/test-reporter@v2`·`gradle/actions/setup-gradle@v4`에 대해 Node 20 deprecation 경고가 런마다 붙고, `setup-java@v4`는 별도 deprecation 경고까지 낸다. 워크플로 4종 + 합성 액션 2종이 전부 해당한다.
- **항목**: ① 일괄 메이저 버전 올림을 언제 할지 — 액션 하나씩 올리면 런마다 경고가 남고, 한꺼번에 올리면 캐시 키(`setup-gradle` 버전 포함)가 바뀌어 첫 런이 콜드로 돈다. ② 캐시 시딩 직후라 **효과 측정 기간과 겹치지 않게** 시점을 잡을 것.
- **상태**: 미해결 (경고일 뿐 실패는 아님 — 강제 종료일 전에 처리)
- **해소 메모**: 올리면 `.github/workflows/*.yml`과 `.github/actions/{setup-android-build,restore-app-secrets}/action.yml`을 함께 본다.

### [2026-08-11] 로그인 판별자 키를 계약 문서가 틀리게 기술했고 Android가 그대로 구현했다

- **ID**: OQ-P-116
- **출처**: 서버 `KakaoLoginResponse`·`AppleLoginResponse`의 `val isNewUser: Boolean`. 서버 `http` 모듈이 `tools.jackson.module:jackson-module-kotlin`을 의존해 **`is` 접두사가 JSON 키에 남고**, `KakaoLoginControllerTest`·`AppleLoginControllerTest`가 실제 응답 본문에 `jsonPath("$.data.isNewUser")`를 단언한다. 팀 명세도 `isNewUser`로 적었다. **OpenAPI 스키마만 `newUser`**인데, springdoc이 swagger-core 자체 ObjectMapper(Kotlin 모듈 없음)로 모델을 유도하기 때문이다. Android `data/service/model/response/auth/KakaoLoginResponse.kt`는 `@SerialName("newUser")`를 붙이고 있다 → [api/auth.md](../api/auth.md) "판별자 키", [api/conventions.md](../api/conventions.md) "직렬화 규약".
- **항목**: ① Android의 `@SerialName("newUser")`를 제거하거나 `isNewUser`로 고친다 — 현재 상태로는 키를 못 찾아 `MissingFieldException`이 나고 **카카오 로그인 호출이 통째로 실패**한다(기본값이 없는 필드다). ② 실서버 응답으로 키를 한 번 확인한다 — 근거 3축(서버 코드·컨트롤러 테스트·팀 명세)이 한 방향이고 스키마만 반대지만, 실물 응답을 본 적은 없다. `http/auth.http`로 찍는 것이 가장 싸다. ③ 서버팀에 `@JsonProperty`로 키를 명시 고정할 의향을 확인한다 — Jackson 모듈 구성이 바뀌면 키가 조용히 뒤집힌다. ④ **`http/auth.http`를 README와 맞춘다**(아래 참고) — 지금은 같은 폴더의 두 파일이 반대를 가르친다.
- **상태**: 미해결 (①은 앱 코드 수정 대상, ②는 실연동 라운드에서, ④는 2026-08-12 신설)
  > ⚠️ **정정이 세 자리 중 한 자리에만 닿았다(2026-08-12, PR #230)** — 그 PR이 `http/README.md`의 판별자 서술을 `isNewUser`로 고쳤는데, **`http/auth.http`의 주석은 여전히 "판별자 필드명은 `newUser`다. `isNewUser`가 아니다"라고 가르치고** 응답 핸들러도 `response.body.data.newUser`를 읽어 분기한다. 앱 `KakaoLoginResponse`의 `@SerialName("newUser")`도 그대로다. `http/`를 처음 쓰는 사람은 같은 디렉토리에서 서로 반대되는 두 문장을 읽고, ②(실서버로 키 확인)를 하려는 사람이 쓰는 도구가 **바로 그 틀렸을 가능성이 있는 키로 분기한다** — 응답이 `isNewUser`로 오면 `auth.http` 1번이 조용히 "기존 회원" 경로를 타므로, 확인하려던 것을 확인하지 못한 채 넘어간다. 세 자리(앱 DTO·`auth.http`·README)를 한 번에 고치는 것이 맞다.
- **해소 메모**: 이 불일치는 **계약 문서가 만들었다** — 2026-08-02 판본이 스키마 하나를 근거로 키를 `newUser`로 적었고 앱이 성실히 따랐다. 재발 방지로 [api/conventions.md](../api/conventions.md) "OpenAPI"에 "스키마가 틀리는 것: 직렬화 키와 `required`"를 명시하고, 직렬화 키의 근거를 **컨트롤러 테스트의 응답 본문 단언**으로 바꿨다. ①이 끝나면 [api/README.md](../api/README.md) 도메인 표의 `⚠️불일치`와 [api/conventions.md](../api/conventions.md) "Android 불일치" 표를 함께 지운다. 선행 항목은 OQ-P-075.

### [2026-08-11] 서버가 앱보다 7 엔드포인트 앞섰다 — 애플 로그인·회원·토핑 배치가 통째로 공백

- **ID**: OQ-P-117
- **출처**: 서버 `2c5499a` 기준 21 엔드포인트. TJYG-Android develop의 원격 표면은 `AuthService`·`ParfaitGroupService`·`ParfaitService`·`PolicyService` 4개(= 14 엔드포인트)로 그대로다. 공백은 image 2(미머지 브랜치 `feature/sync-backend-api-260810`에 `ImageService`가 있다) · 애플 로그인 1 · member 2 · parfait-image 2 → [api/README.md](../api/README.md), [api/conventions.md](../api/conventions.md) "Android 불일치".
- **항목**: ① 앱이 어느 순서로 따라갈지 — 화면 결선 순서(온보딩 → 캔버스)와 서버 순서가 다르다. ~~② **애플 로그인은 iOS만의 요구가 아니다** — Android가 붙일지, 붙는다면 `identityToken`·`authorizationCode`를 어디서 얻을지(애플 로그인 SDK가 Android에 없어 웹 플로가 필요하다) 결정한다.~~(해소 — 아래) ③ 토핑 배치(`parfait-image`)는 **목록 조회 API가 없어** 배치만 되고 다시 그릴 수 없다 — 앱 결선은 그 API를 기다려야 한다.
  > 📌 **② 해소(2026-08-11)** — **Android는 애플 로그인을 쓰지 않는다.** 서버 계약은 그대로 두되 앱 대응 심볼을 만들지 않고 `http/auth.http`에도 요청을 넣지 않는다. [api/README.md](../api/README.md) Android 열에 `해당 없음` 값을 신설해 `미구현`(아직 없음)과 구분했고 — 표면 개수를 셀 때 분모에서 뺀다 — [api/auth.md](../api/auth.md) 엔드포인트 표·Android 매핑 절에 반영했다. 근거는 [member·parfait-image 서비스 레이어 스펙](../superpowers/specs/archive/2026-08-11-member-parfait-image-api-service-layer.md) "범위". iOS가 붙으면 계약은 그대로 유효하다.
  > 📌 **공백이 곧 0이 된다(진행 중)** — 분모가 21에서 **20**으로 줄고(애플 1 제외), develop 14 + PR #229의 image 2 + 위 스펙의 member 2·parfait-image 2 = **20**이다. 즉 표면 공백 자체는 이 라운드로 닫히고 **①이 말하는 "순서" 문제는 표면이 아니라 소비처 쪽으로 옮겨간다.**
- **상태**: **해소됨** (2026-08-12, PR #230 develop 머지 — 공백 0)
  > ⚠️ **② 결정과 어긋난 잔여물이 develop에 있다(2026-08-11, PR #218)** — 같은 날 머지된 로그인 PR이 애플 버튼을 넣었다 지우면서 심볼 3종을 남겼다 → 아래 [애플 잔여 심볼 항목](#2026-08-11-애플-로그인-잔여-심볼-3종이-사용처-0으로-develop에-남았다). 결정 자체는 유효하고, 코드에서 걷어내는 것이 남았다.
- **해소 메모**: 공백 6건(image 2·member 2·parfait-image 2)이 PR #230으로 한 번에 닫혔다 — `ImageService`·`MemberService`·`ParfaitImageService` + remote DataSource 3쌍 + DTO 9 + domain 16. 애플 1건은 ② 결정으로 분모에서 빠져 **20/20**이다. 반영처: [api/README.md](../api/README.md) 도메인 표 Android 열 3행·표면 개수, 세 도메인 문서의 `android_status`·엔드포인트 표·"Android 매핑" 절, [api/conventions.md](../api/conventions.md) 개수 문단, [data-layer](../architecture/data-layer.md) "네트워킹" 반영 범위.
  **①이 말하던 "따라가는 순서"는 이 항목에서 끝난다** — 표면이 없어서 못 쓰던 상태가 사라졌으므로 남은 것은 소비처 결선 순서이고 그건 [2026-08-06] 소비처 0건 항목이 안고 간다. ③(배치 목록 조회 API 부재로 캔버스를 다시 그릴 수 없다)은 **서버 소관이라 여전히 열려 있고** OQ-P-119가 추적한다.

### [2026-08-11] 토핑 배치 POST가 남의 배치를 덮어쓰고 소유자를 가져간다

- **ID**: OQ-P-118
- **출처**: 서버 `PlaceParfaitImageService.place` — `parfaitImageQueryPort.findByParfaitIdAndImageMetaId(parfaitId, imageId)`로 기존 배치를 찾아 있으면 `ParfaitImage.reposition(placedByGroupMemberId = 호출자, …)`을 부른다. **배치자 대조가 없다.** 반면 `UpdateParfaitImageService`는 `groupMember.id != parfaitImage.placedByGroupMemberId`면 `PARFAIT_IMAGE_NOT_OWNED`(403)로 막는다 → [api/parfait-image.md](../api/parfait-image.md).
- **항목**: ① 같은 그룹 멤버가 남의 토핑을 옮기는 것이 의도인지(협업 캔버스라 허용일 수도 있다). ② 허용이라면 **PATCH의 소유자 검사와 모순**이므로 한쪽을 맞춘다 — 지금은 "PATCH로는 못 옮기지만 POST로는 옮기고 소유권까지 가져간다". ③ POST의 upsert 동작(`imageId` 재사용 시 새 행이 안 생김) 자체를 앱이 알아야 한다 — 같은 이미지를 두 번 배치할 수 없다는 뜻이다.
- **상태**: 미해결 (서버팀 확인 필요 — 권한 모델 결정)
  > 📌 **앱은 방어하지 않고 사실만 문서화했다(2026-08-12, PR #230)** — `ParfaitImageRemoteDataSource.placeTopping` KDoc이 upsert·소유자 이전을 적어 두는 데 그친다. 클라이언트가 막을 수 있는 종류가 아니라서다(같은 판단이 image confirm 소유자 미검증에도 적용됐다). ③(같은 이미지를 두 번 배치할 수 없다)은 화면 결선 시 제약으로 나타난다.
- **해소 메모**: 확인 후 [api/parfait-image.md](../api/parfait-image.md) POST 절의 ⚠️와 "미결"을 갱신한다. 정책 근거는 위키 [[토핑]]·[[토핑-spotlight]](C-202는 타인 토핑 탭을 조회로만 규정한다) 쪽도 함께 본다.

### [2026-08-11] 토핑 배치 계약의 공백 — 목록 조회·삭제 부재, 테두리 수정 경로 없음, 좌표 검증 없음

- **ID**: OQ-P-119
- **출처**: 서버 `http/parfaitimage` 전수 — 컨트롤러는 `PlaceParfaitImageController`(POST)·`UpdateParfaitImageController`(PATCH) 둘뿐이다. `PlaceParfaitImageRequest`·`UpdateParfaitImageRequest`에 검증 애노테이션이 없고 컨트롤러도 `@Valid`를 붙이지 않는다. `UpdateParfaitImageRequest`에 테두리 필드가 없고 두 응답 DTO도 테두리를 돌려주지 않는다 → [api/parfait-image.md](../api/parfait-image.md).
- **항목**: ① **배치 목록 조회 API**가 언제 나오는지 — 없으면 앱이 캔버스를 다시 그릴 수 없다(현재는 자기가 방금 보낸 값만 안다). ② **배치 삭제 API** 부재 — 토핑을 지울 수 없다. ③ 배치 후 **테두리 변경 경로**를 PATCH에 넣을지, 아니면 같은 `imageId` 재-POST로 가게 둘지(후자는 위 소유권 문제와 얽힌다). ④ `positionX/Y/Z`·`scale`·`rotation` 범위를 서버가 강제할지 앱 책임으로 둘지 — 현재 음수 `scale`도 저장된다.
- **상태**: **부분 해소** (①②③ 해소 — ④ 잔존)
  > ✅ **①②③ 해소(2026-08-15, 서버 `36ecd1c`)** — ② **배치 삭제**(`DELETE .../images/{parfaitImageId}`)와
  > ③ **테두리 수정**(`PATCH .../images/{parfaitImageId}/border`)이 신설됐고, ①은 이 도메인이 아니라
  > **[api/parfait.md](../api/parfait.md)의 `GET .../parfaits/today`**가 닫았다 — 오늘 캔버스 응답의
  > `images[]`가 배치 전량(좌표·테두리·배치자·생성시각)을 내려준다. **"캔버스를 다시 그릴 수 없다"는
  > 앱 결선의 선행 조건이 사라졌다.** ④(좌표·`scale`·`rotation` 범위 검증)는 그대로이고 신규
  > `borderWidth`도 같은 상태다 — 새 엔드포인트에도 Bean Validation·`@Valid`가 없다.
  > 대신 **새 미결 둘**이 붙었다: 삭제가 S3만 지우고 `COMPLETED` 메타를 남기는 것, 그 S3 호출이 트랜잭션
  > 안에 있는 것 → [api/parfait-image.md](../api/parfait-image.md).
  > 📌 **앱 표면이 ③의 부재를 모양으로 드러낸다(2026-08-12, PR #230)** — `PlacedToppingVO`·`UpdatedToppingVO`에 테두리 필드가 없다(서버가 안 돌려줘서 지어내지 않았다). ①(목록 조회 부재)은 `ParfaitImageRemoteDataSource`가 배치·수정만 갖는 이유이고, 이 라운드가 Repository까지 가지 않은 사유이기도 하다 → [api/parfait-image.md](../api/parfait-image.md) "Android 매핑".
- **해소 메모**: 서버가 채우면 [api/parfait-image.md](../api/parfait-image.md)에 엔드포인트를 추가하고 [api/README.md](../api/README.md) 도메인 표의 개수를 갱신한다.

### [2026-08-11] 전역 닉네임을 바꿔도 기존 그룹 닉네임이 그대로다

- **ID**: OQ-P-120
- **출처**: 서버 `MemberService.change` — `Member.globalNickname` 한 컬럼만 갱신한다(`MemberAdapter.updateGlobalNickname`). 그룹 참여 시점에 복사된 `groupNickname`은 별도 컬럼이고 그룹별 변경 API가 따로 있다([api/parfait-group.md](../api/parfait-group.md)) → [api/member.md](../api/member.md).
- **항목**: ① 의도된 설계인지 확인 — 그룹마다 다른 이름을 쓰는 것이 기능이라면 맞고, 아니면 전파가 빠진 것이다. ② 앱이 두 값을 어느 화면에서 어떻게 보여줄지 — S-002(앱 닉네임)와 S-101(그룹 프로필)이 서로 다른 값을 보이게 된다. 위키 [[닉네임-자동-생성]]은 "앱↔그룹 값 공유"를 초기값 한정으로 적고 있어 **변경 이후의 동기화는 정책에도 없다**.
- **상태**: 미해결 (①은 서버팀·기획 확인, ②는 그 결과에 종속)
  > 📌 **앱 표면이 두 값을 타입으로 갈라 놨다(2026-08-12, PR #230)** — `GlobalNickname`과 `GroupNickname`을 유효성 규칙이 같은데도 합치지 않아, ②(두 화면이 다른 값을 보인다)를 결선할 때 서로 뒤바꾸는 실수가 컴파일에서 막힌다. ①(전파가 빠진 것인지)은 서버·기획 확인이라 그대로다.
- **해소 메모**: 확인 후 [api/member.md](../api/member.md) "미결"과 위키 [[닉네임-자동-생성]] 쪽 대응 서술을 갱신한다.

### [2026-08-11] 영속 `LoginProvider.GOOGLE`이 core enum에 없어 계정 조회가 500이 된다

- **ID**: OQ-P-121
- **출처**: 서버 `MemberAdapter.toCoreProvider` — 영속 `LoginProvider`는 `KAKAO`·`APPLE`·`GOOGLE` 3종인데 core `LoginProvider`는 2종이라, `GOOGLE` 행에서 `error("GOOGLE login provider is not supported yet")`로 `IllegalStateException`을 던진다. `GlobalExceptionHandler`의 `Exception` 핸들러가 **500 `INTERNAL_SERVER_ERROR`**로 바꾼다 → [api/member.md](../api/member.md) `GET /api/v1/users/me`.
- **항목**: 구글 로그인을 계약에서 뺄지(영속 enum에서도 제거), 아니면 core에 넣고 로그인 엔드포인트를 만들지 결정한다. 지금은 **구글 회원 행이 하나라도 생기면 그 회원의 계정 조회가 깨진다** — 현재는 구글 로그인 경로 자체가 없어 도달하지 않는다.
- **상태**: 미해결 (서버팀 확인 필요 — 도달 불가라 급하지 않음)
  > 📌 **앱은 이 값에서 크래시하지 않는다(2026-08-12, PR #230)** — `LoginProvider`에 `UNKNOWN`을 두고 매퍼가 `enumValueOf`가 아니라 `when` 분기라, 서버가 `GOOGLE`을 core enum에 넣더라도 앱은 조용히 `UNKNOWN`으로 떨어진다. **다만 서버가 500을 던지는 경로는 그대로**라 그 회원은 계정 조회 자체를 못 한다 — 앱 방어는 이 항목을 닫지 않는다 → [api/member.md](../api/member.md) "Android 매핑".
- **해소 메모**: 확인 후 [api/member.md](../api/member.md) 응답 필드 표의 ⚠️와 "미결"을 갱신한다.

### [2026-08-11] parfait-image 팀 명세 원문이 `api/spec/`에 없다 — 코드에서 못 읽는 클라이언트 책임이 거기 있다

- **ID**: OQ-P-122
- **출처**: 팀 명세에 `PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/images/{parfaitImageId}` 페이지가 있고 **관련 화면을 C-305로, 요구를 "자신이 올린 토핑의 위치·크기·각도 수정, 캔버스 영역 이탈 시 자동 보정"으로** 적었다. `api/spec/`에는 auth 4건뿐이라 이 원문이 저장소에 없다. 서버 코드에는 좌표 검증이 **0건**이므로(→ [api/parfait-image.md](../api/parfait-image.md)) "이탈 보정을 누가 하는가"는 코드가 답하지 못한다 — 명세만 답한다.
- **항목**: ① `spec/parfait-image-place.md`·`spec/parfait-image-update.md`를 수집하고 `## 코드 대조` 절을 돌린다. ② 그 결과로 OQ-P-119 ④(좌표·`scale`·`rotation` 범위를 서버가 강제할지 앱 책임으로 둘지)가 닫히는지 확인한다 — 명세가 앱 책임으로 읽히지만 근거가 화면 설명 한 줄이라 서버팀 확인이 함께 필요하다. ③ 같은 방식으로 member 도메인 명세도 있는지 확인한다.
- **상태**: 미해결 (수집은 사용자만 할 수 있다 — 명세 도구 접근 필요)
- **해소 메모**: 수집 후 [api/spec/README.md](../api/spec/README.md) 목록과 [api/README.md](../api/README.md) "팀 명세 원문" 절에 등록하고, [api/parfait-image.md](../api/parfait-image.md)에 **명세 델타** 문단을 추가한다(auth 도메인 문서들과 같은 형식). 이번 서비스 레이어 라운드는 이 수집을 기다리지 않는다 — 자동 보정은 화면 계층 일이라 `:data` 범위 밖이다.

### [2026-08-11] 애플 로그인 잔여 심볼 3종이 사용처 0으로 develop에 남았다

- **ID**: OQ-P-123
- **출처**: PR #218 develop 머지 — 브랜치가 애플 로그인 버튼을 넣었다가 같은 브랜치에서 지웠는데(`chore: 애플 로그인 관련 코드 삭제`) 부속물이 남았다. `core:designsystem` `theme/colors/AppleDesignGuideColors.kt`(신규 파일), `feature/login/impl` `res/drawable/icon_logo_apple.xml`, `feature/login/impl` `strings.xml`의 애플 버튼 라벨·`contentDescription` 2건. develop 전수 검색에서 **참조가 0건**이다. 바로 전날 **Android는 애플 로그인을 쓰지 않기로 확정**했으므로([2026-08-11] 서버 delta 항목의 ② 해소) 이 심볼들은 앞으로도 소비처가 생기지 않는다.
- **항목**: ① 지금 걷어낼지, 아니면 "언젠가 붙을 수도"로 두고 死코드 목록에 올려둘지 — 사용처 0 공개 심볼을 남긴 선례가 이미 둘 있다(`clickableYGNoRipple` [2026-08-03] — **2026-08-17 #284로 존치 쪽 결말**, `animateToppingPlacement` [2026-08-07] — 미결). 다만 선례가 존치로 닫혔다고 이쪽까지 존치가 되는 건 아니다: 저쪽은 소비처가 실제로 생겼고 애플 로그인은 **Android가 안 쓰기로 확정**돼 소비처가 생길 길이 없다. ② 걷어낸다면 `AppleDesignGuideColors`는 `core:designsystem` 소관이라 로그인 PR과 별개 정리 대상이다. ③ 브랜치 안에서 되돌린 기능의 부속 리소스를 리뷰가 못 잡는다는 신호 — 체크 지점을 어디에 둘지(R8은 리소스 축소를 하지만 소스 심볼은 남는다).
- **상태**: 부분 해소 (①② 중 `AppleDesignGuideColors`만 정리, 2026-09-20 PR #514 / drawable·문자열·③ 잔존)
  > ✅ **②가 실행됐다(2026-09-20, PR #514)** — `core:designsystem`의 `AppleDesignGuideColors.kt`가
  > 삭제됐다. ①이 묻던 "지금 걷어낼지"에 **걷어내는 쪽**으로 답한 셈이고, 선례 둘 중
  > `animateToppingPlacement`(같은 라운드에 삭제)와 같은 결말이다.
  > ⚠️ **소스 심볼만 갔고 리소스는 남았다** — `feature/login/impl`의 `icon_logo_apple.xml`,
  > `core:designsystem`의 `ic_social_apple.xml`, `feature/login/impl` `strings.xml`의 애플 버튼
  > 라벨·`contentDescription` 2건, `core:ui` `strings.xml`의 `login_provider_apple`이 그대로다.
  > ③(브랜치 안에서 되돌린 기능의 부속 리소스를 무엇이 잡는가)이 여전히 열려 있다는 증거이기도 하다 —
  > 이번에도 걷힌 것은 코틀린 심볼뿐이다.
  > 📌 **③의 괄호가 이제야 참이 됐다(2026-08-26, PR #372)** — "R8은 리소스 축소를 하지만"이라고
  > 적었으나 그때 릴리즈는 `isMinifyEnabled = true`만 켜져 있었고 `isShrinkResources`는 기본값
  > `false`였다. 즉 **리소스는 축소된 적이 없었다.** 이번에 그 스위치가 켜져 미사용 drawable·
  > string이 실제로 릴리즈 산출물에서 빠지기 시작한다(소스 심볼이 남는다는 뒷문장은 그대로다).
  > 그래도 이 항목의 처분은 안 바뀐다 — 축소는 **산출물에서만** 빼므로 저장소의 死심볼 셋은 그대로다.
- **해소 메모**: 걷어내면 [design-system](../architecture/design-system.md) 색 트리의 `AppleDesignGuideColors` 줄과 [a002-login-onboarding 스펙](../superpowers/specs/archive/2026-08-11-a002-login-onboarding.md) "드리프트" 1번을 함께 지운다. 서버 계약 쪽 애플 엔드포인트는 그대로 둔다(iOS 소관).

### [2026-08-11] A-002 치수 리터럴 4종 — 토큰 스케일에 없는 값을 코드가 자인한다

- **ID**: OQ-P-124
- **출처**: `feature/login/impl` `screen/LoginScreen.kt`·`component/OnboardingPager.kt`(PR #218 develop 머지) — 상단 여백, 페이저 좌/우 여백(둘이 1 차이로 갈린다), 일러스트↔설명·페이저↔버튼 간격 두 곳이 `dp` 리터럴이다. 간격 두 곳에는 코드 주석이 **"gap 없음"**이라고 붙어 있어, 쓰는 사람이 스케일 공백을 인지한 채 리터럴을 남겼다. `YGLayoutGap`·`YGLayoutPadding`은 짝수 스케일이라 이 값들이 실제로 없다. 같은 PR이 카카오 버튼 패딩은 토큰으로 옮겼으므로 **부분 토큰화**다.
- **항목**: ① 스케일에 값을 추가할지(홀더 + `*Defaults` 동시 수정이 강제되므로 비용이 명확하다), ② 화면 고유 여백은 리터럴을 허용하고 규약에 예외로 적을지(`YGInputNumber`의 "디자인가이드 고정 크기" 선례가 있다), ③ 좌/우가 1 차이로 갈린 것이 Figma 실측인지 오차인지 확인.
- **상태**: 미해결 (③이 먼저 — 실측이면 ②, 오차면 대칭으로 고치고 ①)
- **해소 메모**: 결정 시 [design-system](../architecture/design-system.md) "토큰 계층"·"신규 토큰 값 추가 체크리스트"에 예외 규약을 적고 [a002-login-onboarding 스펙](../superpowers/specs/archive/2026-08-11-a002-login-onboarding.md) "드리프트" 2번을 갱신한다.

### [2026-08-11] 화면 에셋 소유가 `core:designsystem`과 feature로 갈린다

- **ID**: OQ-P-125
- **출처**: PR #218 develop 머지 — A-002 온보딩 일러스트(`image_onboarding_1`~`_3`)는 `core:designsystem` `res/drawable*`에 들어가 feature가 `DesignSystemR`로 참조하는데, **같은 화면의** 카카오·애플 로고 벡터는 `feature/login/impl` `res/drawable/`에 있다. 문자열은 "화면 전용 = feature / 공유 = `core:ui`" 규약이 [module-structure](../architecture/module-structure.md)에 있지만 **이미지에는 대응 규약이 없다**. 밀도 버킷도 이미지마다 다르게 채워졌다 — `image_onboarding_1`만 xhdpi 버킷이 없고 대신 밀도 없는 기본 `drawable/`에 하나 더 있으며, `_3`은 ldpi가 없다. 렌더는 되지만(가장 가까운 버킷을 스케일) 세 장이 같은 조건으로 그려지지 않는다.
- **항목**: ① 이미지 에셋 소유 기준을 문자열과 같은 축(화면 전용 = feature / 여러 화면 공용 = `core:designsystem`)으로 못박을지, 아니면 "디자인이 준 것은 전부 DS"로 갈지. ② 버킷 세트를 채울지 — 세 장 다 같은 세트를 갖게 할지, 아니면 벡터·WebP로 갈지. ③ 기존 화면 에셋(갤러리 빈 상태 PNG 등)도 그 기준으로 재배치할지.
- **상태**: 미해결 (①이 정해져야 ③의 범위가 정해진다)
- **해소 메모**: 정하면 [module-structure](../architecture/module-structure.md) "규칙"에 이미지 축을 한 줄 추가하고 [design-system](../architecture/design-system.md) `res/drawable*` 노트를 갱신한다.
  > 📌 **같은 갈림이 로띠에서 되풀이됐다(2026-08-18, PR #305)** — 로딩 로띠 2종은 `core:designsystem`
  > `res/raw/`(모듈 최초의 raw 리소스)에 들어가 `YGLoadingLottie`가 감싸는데, 스플래시 로띠는
  > `feature/intro/impl` `res/raw/`에 있고 화면이 `LottieAnimation`을 직접 부른다. 판단 자체는
  > 설명 가능하다(하나는 여러 화면 공용, 하나는 그 화면 전용) — **그러나 기준이 문서에 없어 이번에도
  > 사례로만 갈렸다.** ①의 범위에 raw 애니메이션이 포함되는지도 정해진 바 없다.

### [2026-08-11] 온보딩 3장의 문구·구성이 정책 소스 없이 코드로 확정됐다

- **ID**: OQ-P-126
- **출처**: PR #218 develop 머지 — `feature/login/impl` `strings.xml`의 온보딩 설명 3종과 일러스트 3장 구성. 위키에 A-002 슬라이드 정책이 없다(수집된 것은 [[기능정의서-v5]]의 "프로필 이미지 안 넣음" 한 줄과 [[화면-ID-체계]]의 `A-002 로그인` 뿐). G-001 에러 문구·툴팁 문구와 같은 성격으로 **코드가 정본이 된 문구**가 하나 더 늘었다. 겸해서 `OnboardingPagesPreviewParameterProvider`가 세 번째 페이지에 `image_onboarding_3`이 아니라 `_1`을 넣고 설명 줄바꿈도 `strings.xml`과 달라, 프리뷰가 실화면과 갈린다.
- **항목**: ① 온보딩 문구·일러스트 구성을 정책 소스로 역수집할지, 코드를 정본으로 인정할지([2026-08-11] 로딩 그래픽 항목의 ③과 같은 판단이다). ② 프리뷰 파라미터를 실화면과 일치시킬지 — 프리뷰가 실기기 없이 볼 수 있는 유일한 그물인데 세 번째 장이 다르다. ③ 프리뷰용 데이터와 Route의 `remember` 목록이 두 벌로 존재하는 구조를 유지할지(둘이 어긋나도 컴파일은 통과한다).
- **상태**: 미해결 (②는 즉시 고칠 수 있는 코드 수정, ①은 위키 ingest 판단)
- **해소 메모**: ①이 정해지면 위키 쪽 미결과 함께 닫고, ②③ 처리 시 [a002-login-onboarding 스펙](../superpowers/specs/archive/2026-08-11-a002-login-onboarding.md) "드리프트" 4번을 지운다.

### [2026-08-12] 캔버스 날짜가 03시 경계를 안 쓴다 — 같은 저장소에 `DayWindow`가 있는데도

- **ID**: OQ-P-127
- **출처**: `feature/groups/canvas/impl` `viewmodel/CanvasMainViewModel.kt#loadCanvasMainInfo`(PR #199 develop 머지) — 캔버스 날짜 라벨을 `Clock.System.todayIn(TimeZone.currentSystemDefault())`로 만든다. 위키 [[캔버스-마감-스케줄]]은 하루 경계가 **03:00 KST 고정**이고(서버 기준 KST), `domain`의 `DayWindow.current(timeZone, clock)`가 그 경계를 이미 구현해 C-102 갤러리가 쓰고 있다. 지금 구현은 경계가 00:00이고 시간대도 기기 설정을 따른다 — 00:00~02:59 사이에는 화면이 **캔버스의 실제 날짜보다 하루 뒤 날짜**를 보여준다.
- **항목**: ① 화면 날짜를 `DayWindow` 기준으로 옮길지(경계·시간대 둘 다), ② 시간대를 KST로 고정할지 기기 시간대를 인정할지 — 서버가 KST로 캔버스를 마감하므로 해외 사용자는 어느 쪽이든 정책 결정이 필요하다. ③ 날짜가 화면에서 계산되는 구조 자체를 유지할지(서버가 캔버스 날짜를 내려주면 표시만 남는다).
- **상태**: 미해결 (그룹·캔버스 데이터 미결선이라 지금은 표시만 틀린다)
  > ⚠️ **범위가 커졌다(2026-08-16, PR #259)** — 같은 `today` 값이 이제 **캘린더의 미래 날짜 잠금과 오늘
  > 강조**까지 결정한다. 00:00~02:59에는 캔버스의 실제 날짜가 아직 어제인데 달력이 오늘을 다음 날로
  > 표시하고 그 날을 이미 선택 가능하게 연다. 게다가 `today`가 **UiState 기본값과 로드 함수에서 각각**
  > `Clock`을 읽어 자정을 사이에 두면 두 값이 갈릴 수 있다
  > → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md).
  > ✅ **②는 답이 나왔고 ①은 절반 남았다(2026-08-17, PR #268)** — 시간대는 **KST 고정**으로 확정됐다
  > (`PARFAIT_TIME_ZONE`·`parfaitToday()`). 정한 이유가 표시 정합이 아니라 **동작**이라는 점이 중요하다:
  > 캔버스 행이 KST 날짜를 키로 저장돼, 기기 시간대로 오늘을 세면 오늘 조회의 자정 경계 재시도가
  > 하루 한 번이 아니라 **로드마다** 돌고 달력이 지금 보는 날을 미래로 보고 잠근다. **경계 00:00은
  > 그대로**라 ①(`DayWindow` 03:00으로 옮길지)은 열려 있고, `DayWindow`는 여전히 C-102 갤러리만 쓴다.
  > **`today` 이중 계산은 해소됐다** — 로드 함수가 날짜를 만들지 않게 되어 UiState 기본값 한 자리다
  > → [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md).
  > ⚠️ **경계 00:00을 읽는 자리가 늘었다(2026-08-17, PR #297)** — 재진입마다 도는 `syncToday()`가
  > `parfaitToday()`로 오늘을 다시 세고, 값이 달라지면 **보고 있던 캔버스를 비우고 날짜를 옮긴다.**
  > 즉 03:00 미적용이 이제 표시·달력 잠금뿐 아니라 **화면 상태 리셋 시점**까지 정한다 — 00:00~02:59에
  > 재진입하면 서버가 아직 어제로 치는 날에 앱이 새 날 캔버스를 요청한다. G-001도 같은 시점에 날짜
  > 헤더를 다시 센다(이쪽은 기기 시간대 그대로)
  > → [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md).
- **해소 메모**: ①②가 정해지면 [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) "정책 대조" 표와 드리프트 1번을 고치고, `DayWindow`를 화면 계층에서도 쓰는 관용구를 [module-structure](../architecture/module-structure.md)에 한 줄 남긴다. [2026-08-04] 날짜 영문 표기 항목과 같은 화면·같은 값에 걸린다.

### [2026-08-12] Dot Grid가 시스템 바 영역을 못 덮는다 — 정책은 "화면 전체 뒤"

- **ID**: OQ-P-128
- **출처**: `feature/groups/canvas/impl` `screen/CanvasMainScreen.kt` + `core:designsystem` `component/ygbackgrounddotgrid/YGBackgroundDotGrid.kt#ygBackgroundDotGrid`(PR #199 develop 머지) — 격자 Modifier가 엔트리 `YGScaffold`의 `innerPadding` **안쪽** `Column`에 붙는다. 위키 [[캔버스-반응형-레이아웃]]은 "화면 전체(상단바·하단바·캔버스 포함) 뒤에 동일하게 깔리고, 앵커는 화면 좌상단 (0,0)"이라고 못박는다. 지금은 상태바·내비게이션 바 영역이 `YGScaffold`의 흰 `containerColor`만 남고, 격자 원점도 상단 인셋만큼 밀려 **점 위치가 정책 좌표와 어긋난다**. 점 스펙(지름 2·`Gray100`·간격 20)은 정책과 일치한다.
- **항목**: ① 격자를 엔트리 컨테이너 쪽으로 올릴지(`YGScaffold` 배경 슬롯 신설 / entry에서 `Box`로 감싸기 — 후자는 [2026-08-01] G-001 컨테이너 이탈과 같은 형태다), ② 탑바가 인셋을 흡수하는 G-001 관용구로 갈지(그러려면 `YGTopBarCanvas`에 `windowInsets`를 열어야 한다 — [2026-08-07] 항목 ②), ③ 정책의 "앵커 (0,0)"을 화면 원점으로 볼지 콘텐츠 원점으로 볼지 디자이너에게 확인할지.
- **상태**: 미해결 (렌더는 되지만 정책 불일치)
- **해소 메모**: ①②는 인셋 관용구 결정([2026-08-07])과 한 몸이다. 정하면 [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) "정책 대조" 표와 [navigation-flow](../architecture/navigation-flow.md) 인셋 사례 노트를 함께 고친다.

### [2026-08-12] C-001이 도달 불가로 머지됐고 화면 안 콜백 3종이 빈 람다

- **ID**: OQ-P-129
- **출처**: `feature/groups/canvas/impl` `route/CanvasMainRoute.kt` + `feature/groups/list/impl` `route/GroupListRoute.kt`(PR #199 develop 머지) — ① `NavKeyCanvasMain`를 `goTo` 하는 호출자가 develop에 **0건**이다(entry 등록만 있다). 유일한 후보인 G-001의 `GroupListSideEffect.NavigateToCanvas` 분기는 여전히 `// Todo : canvas page 이동`이고, 애초에 그 이펙트를 쏘는 토핑 클릭 경로도 없다([2026-08-07] 死경로 항목). ② 화면 안에서도 `onClickDateSelect`·`onClickMenu`·`onClickEditCanvasBG`가 TODO 주석 달린 빈 람다다 — 날짜 선택(캘린더)·상단 메뉴·캔버스 편집이 전부 미결선이고, `NavKeyCanvasEdit`·`NavKeyCanvasImageSelect` entry는 등록돼 있는데 이 화면에서 가지 않는다. ③ 이름과 목적지도 어긋난다 — `CanvasMainIntent.OnClickCanvas`·`CanvasMainEffect.NavigateToCanvas`가 실제로는 갤러리(`NavKeyCustomGalleryPicker`)로 간다.
- **항목**: ① G-001 토핑 클릭 → C-001 진입을 어느 라운드에서 결선할지(그룹 데이터 결선과 묶일 수밖에 없다 — 캔버스는 `groupId`가 필요한데 `NavKeyCanvasMain`는 `data object`다), ② 날짜 선택·상단 메뉴·캔버스 편집의 목적지를 확정할지, ③ `OnClickCanvas`/`NavigateToCanvas` 이름을 목적지에 맞게 고칠지(화면 이름이 `CanvasImageAdd*`인데 C-001 캔버스 메인을 가리키는 것도 같은 성격이다 — 이쪽은 2026-08-17 리네임으로 닫혔다).
- **상태**: 미해결
- **해소 메모**: ①을 열 때 `NavKeyCanvasMain`에 `groupId` 인자를 붙일지 함께 정한다([navigation-flow](../architecture/navigation-flow.md) "인자 있는 목적지"). 체크리스트 6번 사례 목록도 그때 정리한다. 상세는 [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) 드리프트 3·6번.

  > 📌 **②의 셋 중 캔버스 편집만 결선됐다(2026-08-15, PR #231)** — `onClickEditCanvasBG`가 `NavKeyCanvasBGEdit`(C-301 배경 편집)으로 간다. 날짜 선택·상단 메뉴는 그대로 빈 람다다. **①은 그대로라 새로 생긴 화면까지 도달 불가 범위에 들어왔다** → [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md).
  > 📌 **날짜 선택도 결선됐다(2026-08-16, PR #259)** — `onClickDateSelect`가 캘린더 오버레이를 연다.
  > **②에 남은 것은 상단 메뉴 하나**다. ①(진입 경로 0건)은 그대로이고, ③(`OnClickCanvas`/`NavigateToCanvas`
  > 이름 불일치)도 그대로다 → [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md).
  > ✅ **①이 해소됐다(2026-08-17, PR #268)** — G-001 토핑 클릭이 `goTo(NavKeyCanvasMain(groupId))`로
  > 이어져 진입 경로가 열렸고, 그 선행으로 NavKey가 `data object` → **`data class(groupId)`**가 됐다
  > (해소 메모가 "함께 정한다"고 적어 둔 그것이다). 인텐트·이펙트도 `GroupId`를 싣는다 — 첫 그룹으로
  > 고정하면 두 번째 그룹의 캔버스에 들어갈 방법이 없기 때문이고, `GroupListViewModelTest`가 잠근다.
  > **남은 것은 ②의 상단 메뉴 하나와 ③(이름 불일치)이고, ③은 이번에 `NavigateToCanvas`가 G-001 쪽에서
  > 실제로 캔버스로 가게 되면서 두 모듈의 같은 이름이 서로 다른 뜻이 됐다** — C-001의
  > `CanvasMainEffect.NavigateToCanvas`는 여전히 갤러리로 간다
  > → [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md).
  > 📌 **③의 절반이 닫혔다(2026-08-17, 리네임 #278)** — 화면 계열이 `CanvasImageAdd*` → **`CanvasMain*`**로
  > 바뀌어(`NavKeyCanvasMain`·`CanvasMainRoute`/`Screen`/`ViewModel`/`UiState`/`Intent`/`Effect`,
  > `strings.xml` 키 `canvas_main_*`) 이름이 C-001 캔버스 메인이라는 실제 역할과 맞는다. **남은 것은
  > `OnClickCanvas`/`NavigateToCanvas`가 갤러리로 가는 이름 불일치**이고, ②의 상단 메뉴도 그대로다.
  > 📌 **②가 겨누던 두 목적지가 목적지째 사라졌다(2026-09-20, PR #514)** — 출처 문단이 "entry는
  > 등록돼 있는데 이 화면에서 가지 않는다"고 적던 `NavKeyCanvasEdit`·`NavKeyCanvasImageSelect`가
  > 화면·Route·엔트리 등록까지 삭제됐다(OQ-P-239). **②에 남은 상단 메뉴 하나는 그대로**이고,
  > 이제 그 목적지는 새로 만들어야 한다 — 골라 쓸 수 있던 미결선 엔트리가 없어졌다. ③도 그대로다.

### [2026-08-12] C-001이 mock을 ViewModel 로직에 박고 `isEmpty`를 상수로 넘긴다

- **ID**: OQ-P-130
- **출처**: `feature/groups/canvas/impl` `viewmodel/CanvasMainViewModel.kt#loadCanvasMainInfo`·`screen/CanvasMainScreen.kt`(PR #199 develop 머지) — ① `init`이 부르는 로드 함수가 그룹명 문자열과 멤버 7명을 TODO 주석과 함께 채운다(G-001이 mock을 UiState **기본값**에 둔 것과 또 다른 형태다 — [2026-08-07] 항목). ② 프리뷰 `CanvasMainScreenPreviewParameterProvider`가 같은 7명을 **다른 이름·다른 칩 타입**으로 따로 들고 있어 두 벌이 어긋나는데 컴파일은 통과한다(A-002 프리뷰 드리프트와 같은 구조 — [2026-08-11] 항목). ③ `isEmpty = true`가 화면에 상수로 박혀 있고 `content` 토핑 슬롯을 아예 안 넘긴다 — 토핑이 하나라도 생기면 빈 상태 문구가 그 위에 계속 그려진다.
- **항목**: ① mock 소유를 어디로 통일할지(UiState 기본값 / 로드 함수 / 프리뷰 전용) — 지금 develop에 세 형태가 있다. ② 프리뷰 데이터와 실행 데이터가 갈리는 것을 막을 방법(공유 provider·`@PreviewParameter` 재사용)을 규약으로 둘지. ③ `isEmpty`를 UiState의 토핑 목록 유무에서 파생시킬지 — 캔버스 데이터가 붙는 라운드의 선행 결정이다.
- **상태**: 미해결 (③ 해소·① 대부분 해소, ② 잔존)
  > ✅ **캔버스 데이터가 붙으며 ③이 닫히고 ①이 대부분 걷혔다(2026-08-17, PR #268)** — `isEmpty`는
  > **토핑 목록 파생**(`isCanvasEmpty`)이 됐고 `content` 슬롯에 `CanvasToppingLayer`가 들어간다.
  > 멤버 7명 mock도 캔버스 응답의 `groupMembers`로 바뀌었다. **남은 mock은 그룹명 하나**다 —
  > 캔버스 응답에 그룹명이 없어 로드 함수가 문자열을 그대로 들고 있고, 그래서 **한 상단 바 안에서
  > 실데이터(멤버 칩)와 mock(그룹명)이 나란히 그려진다.** 칩 **색**도 서버가 안 줘서 목록 인덱스
  > 순환이다(OQ-P-210). **②(프리뷰 두 벌)는 그대로다** — 프리뷰 provider가 여전히 다른 이름·칩 타입을
  > 들고 있고, 이번에 `canvasDate`·`canvasDay`가 `selectedDate` 파생이 되며 프리뷰도 `selectedDate`를
  > 넣도록 바뀌었을 뿐이다.
- **해소 메모**: ①의 잔여(그룹명)는 그룹 상세 조회를 붙이는 라운드에서 닫고
  [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) 드리프트 4번을 마저 지운다.
  ②는 [2026-08-11] A-002 프리뷰 항목과 같은 결정이다.

### [2026-08-12] `MAX_VISIBLE_MEMBER_CHIPS`가 死상수 — 임계값이 리터럴로 두 번 적혔다

- **ID**: OQ-P-131
- **출처**: `feature/groups/canvas/impl` `screen/CanvasMainScreen.kt`(PR #199 develop 머지) — 상단 바 멤버 칩 개수 상한을 `private const val MAX_VISIBLE_MEMBER_CHIPS`로 선언해 두고, 정작 `take(…)`와 초과분 계산은 **리터럴 숫자**를 쓴다. 상수를 고쳐도 동작이 안 바뀌고 리터럴 둘 중 하나만 고치면 칩 개수와 `+N`이 어긋난다. 칩 겹침 간격도 `dp` 리터럴이라 토큰 스케일 밖이다([2026-08-11] A-002 치수 리터럴 항목과 같은 성격).
- **항목**: ① 상수를 실제로 쓰게 고칠지(즉시 가능한 코드 수정), ② 겹침 간격을 토큰으로 올릴지 화면 고유 값으로 인정할지, ③ `+N` 임계값 자체의 근거 — Figma 주석("캔버스 전용 `+` 칩")뿐이고 위키에 그룹원 표시 정책이 없다. 그룹 정원은 [[그룹]]이 12명으로 못박는데 상단 바가 몇 명까지 보여야 하는지는 정책 소스가 없다.
- **상태**: 미해결 (①은 즉시 고칠 수 있는 코드 수정, ③은 정책 수집 판단)
- **해소 메모**: ①② 처리 시 [c001-canvas-main 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) 드리프트 5번을 지운다. ③은 위키 소관이라 정책 소스 수집 요청이 먼저다. ②는 [2026-08-11] 치수 리터럴 항목과 같은 결정에 묶인다.

### [2026-08-12] `ApiCaller.safeApiCallWithoutData`가 표면 완성과 함께 死코드로 확정됐다

- **ID**: OQ-P-132
- **출처**: `data/network/ApiCaller.kt`(PR #230 develop 머지 시점 재확인) — 진입점 넷 중 `safeApiCallWithoutData`만 **프로덕션 호출부가 0건**이다. 선언과 `ApiCallerTest`의 자기 테스트뿐이고, 20 엔드포인트 전부가 `safeApiCall(block)`·`safeApiCall(block, transform)`·`safeApiCallNoContent`(`logout` 하나) 셋으로 갈렸다. 2026-08-03 라운드가 "14 전량 구현으로 사실상 死코드"라며 이월했던 minor인데, **표면이 20/20으로 닫히며 "아직 안 쓰였을 뿐"이라는 해석이 없어졌다** — Android가 붙일 엔드포인트가 더 없다.
- **항목**: ① 지울지 — 지우면 `ApiResponse<Unit>`인데 payload를 안 보는 형태가 서버에 다시 생길 때 되살려야 한다. ② 남긴다면 KDoc에 "현재 사용처 없음, 서버가 `data`를 무의미하게 채우는 응답을 낼 때 쓴다"를 적어 다음 사람이 셋 중 무엇을 고를지 헷갈리지 않게 할지. ③ 테스트도 함께 처리 — 소비자 없는 메서드를 잠그는 테스트가 남는다.
- **상태**: **해소됨** (2026-08-15, PR #250 — 첫 프로덕션 소비처가 생겼다)
- **해소 메모**: **"Android가 붙일 엔드포인트가 더 없다"는 전제가 서버 delta로 뒤집혔다.** 토핑 삭제
  (`DELETE .../images/{parfaitImageId}`)가 **200 + `data: null`**이라 envelope는 오는데 payload가 의미
  없어 `safeApiCallWithoutData`가 정확히 그 자리다 — `ParfaitImageRemoteDataSourceImpl#deleteTopping`이
  첫 호출부다. 같은 delta의 회원 탈퇴는 204·본문 없음이라 `safeApiCallNoContent`로 갈렸다(`logout`에 이어
  두 번째). **이로써 진입점 넷이 전부 프로덕션 소비처를 갖는다.** 지우자는 선택지 ①은 소멸했고 ②(KDoc에
  "사용처 없음" 명시)·③(테스트 처리)도 대상이 없어졌다. [data-layer](../architecture/data-layer.md)
  진입점 표에 두 소비처를 명시했다.

### [2026-08-12] `ApiException.Business.statusCode`를 KDoc이 약속하는데 테스트는 그 경로를 타지 않는다

- **ID**: OQ-P-133
- **출처**: `data/source/member/remote/MemberRemoteDataSource.kt` KDoc × `data/source/*/remote/*ImplTest.kt`(PR #230 develop 머지) — KDoc이 "`MEMBER_NOT_FOUND`가 401과 404 둘 다로 오니 소비 측은 `ApiException.Business.statusCode`로 구분하라"고 적었다. 그런데 DataSource 테스트 4건의 실패 케이스는 전부 **서비스가 `ApiResponse(success = false)`를 반환하도록** mocking해 `ApiCaller`의 `success == false` 분기를 타고, 그 분기에서 `statusCode`는 **항상 `null`**이다. 실제 401·404·409는 Retrofit이 `HttpException`을 던져 `toApiException`의 `statusCode = e.code()` 경로로 가는데 그 경로를 잠그는 것은 `ApiCallerTest`뿐이고 도메인 DataSource 테스트에는 없다. 계획서가 "이 라운드가 검증하지 못하는 것"으로 예고했던 축이 그대로 develop에 들어왔다.
- **항목**: ① 도메인 DataSource 테스트에 `HttpException` 경로 케이스를 더할지 — 더하면 에러 바디 envelope를 실제로 파싱시켜 `statusCode`·`code` 조합을 잠글 수 있다. ② 아니면 그 검증을 `ApiCallerTest` 소관으로 명시하고 DataSource 테스트는 매핑·배선만 본다고 규약에 적을지. ③ 어느 쪽이든 **KDoc이 약속한 구분 방식이 실제로 성립하는지**는 에러 코드를 도메인 예외로 번역하는 라운드가 오기 전에 한 번 확인돼야 한다 — 지금은 "그렇게 하면 된다"고 적혀만 있다.
- **상태**: 미해결 (동작 결함은 아님 — 검증 공백과 문서의 약속이 어긋난 상태)
- **해소 메모**: 정하면 [data-layer](../architecture/data-layer.md) "응답 매핑"의 테스트 규약 문단에 어느 계층이 무엇을 잠그는지 적는다. ①을 고르면 [unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md) "테스트 규약"에도 한 줄이 붙는다.

### [2026-08-12] 그룹 생성·참여가 mock UseCase로 결선됐다 — 서버도, 실패 경로도, 목록 갱신도 없다

- **ID**: OQ-P-134
- **출처**: `domain/usecase/group/CreateGroupUseCase.kt`·`EnterGroupUseCase.kt`·`CheckInviteCodeValidUseCase.kt`(PR #224 develop 머지) — 셋 다 인자를 받고도 **고정 지연 후 성공만 반환**한다(`Todo : 서버 작업이 연결되면…`). 초대코드 검증은 인자조차 받지 않아 **사용자가 입력한 코드가 어디에도 쓰이지 않고**, 모달에 띄우는 그룹명은 UseCase 안 리터럴이다. 호출부(`GroupCreateViewModel`·`GroupNickNameViewModel`)의 실패 분기는 `if (result.isSuccess)` 하나뿐이라 **실패면 모달이 열린 채, 또는 화면에 머문 채 아무 일도 일어나지 않는다**. 20/20으로 닫힌 API 표면([2026-08-06] 항목) 중 `parfait-group` 계열은 여전히 호출되지 않는다.
- **항목**: ① 실연동 라운드에서 이 세 UseCase를 remote DataSource에 붙일 때 Repository를 둘지([2026-08-06] ②와 같은 결정), ② 실패 표현을 무엇으로 할지 — 코드 무효·인원 초과(12명)·이미 가입은 위키 [[그룹]]이 요구하는 케이스인데 화면에 표현 자리가 `errorText`(A-004)뿐이고 A-005·S-102에는 없다, ③ 생성·참여 성공 후 목록이 갱신될 경로 — 복귀가 `goToSingleClearTop`이라 목록 엔트리·ViewModel이 그대로 살아나므로, 조회가 붙어도 **재조회를 누가 트리거할지**를 함께 정해야 한다.
- **상태**: 해소됨 (2026-08-15, PR #243·#244·#248 — mock 3종 전부 삭제. **③은 OQ-P-169로 승계**)
- **해소 메모**: ①은 **Repository를 둔다**로 확정됐다 — 세 화면 다 `ParfaitGroupRepository`를 거치는 UseCase를 쓴다(선반영된 경계를 그대로 소비, OQ-P-157). ②는 화면마다 자리가 생겼다 — A-004·S-102는 입력 자리 인라인(`InviteCodeError`·`GroupNickNameError`), A-005는 **여전히 로그뿐**이라 표현 문제만 OQ-P-167로 옮겼다. ③(목록 갱신 트리거)은 조회가 붙으며 실제 문제로 드러나 **OQ-P-169**가 됐다. 반영처: [a005](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·[a004](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md)·[s102](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md)·[g001](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 스펙, [api/parfait-group.md](../api/parfait-group.md) Android 매핑, [data-layer](../architecture/data-layer.md) Repository 인벤토리.

### [2026-08-12] 그룹 생성·참여 완료 후 복귀 목적지가 위키 정본과 다르다

- **ID**: OQ-P-135
- **출처**: `GroupCreateRoute.kt`·`GroupNickNameRoute.kt`의 `goToSingleClearTop(NavKeyGroupList)`(PR #224 develop 머지) — 코드는 생성·참여를 마치면 **G-001 그룹 목록**으로 돌아온다. 위키 정본 [[기능정의서-v6]]은 중간 화면 G-002(그룹 진입)를 삭제하면서 A-004(참여)·A-005(생성)의 다음 단계를 **C-001(메인 캔버스) 직접 진입**으로 재배선했고, 위키 [[그룹]]도 같은 서술이다. 게다가 C-001은 지금 `goTo` 호출자가 0건이라([2026-08-12] C-001 항목) 정본대로였다면 이 라운드가 그 진입도 함께 열었어야 한다.
- **항목**: ① 코드가 맞다면 위키 흐름을 바꿔야 하는 사안이므로 기획 확인이 필요하다(만든 그룹으로 바로 들어갈지, 목록에서 고르게 할지), ② 정본이 맞다면 `NavKeyCanvasMain`가 그룹 식별자를 인자로 받아야 하는데 현재 `data object`다 — 인자 있는 NavKey 전환이 선행 조건, ③ 어느 쪽이든 새 그룹을 목록에서 어떻게 보여줄지([2026-08-12] mock 항목 ③)와 함께 정해진다.
- **상태**: **해소됨** (2026-09-01, PR #411 — 코드가 위키 정본 쪽으로 옮겨 갔다)
  > ✅ **①이 "정본이 맞다"로 답났다** — 생성·참여를 마치면 **만든/참여한 그룹의 C-001로 바로 들어간다**.
  > ②의 선행 조건(인자 있는 `NavKeyCanvasMain`)은 이미 #268에서 끝나 있었고, 이번에는 그 키에
  > `welcomeGroupName`·`welcomeInviteCode`가 더 붙어 캔버스가 **진입 사유까지** 받는다.
  > 정본과 다른 점 하나가 남는데 결함이 아니라 구현 사정이다 — 목록을 `replaceAll`로 먼저 깔고 그
  > 위에 캔버스를 쌓는다(캔버스만 남기면 뒤로가기가 앱 종료가 된다). ③(새 그룹을 목록에 어떻게
  > 보이게 할지)은 #297의 `Enter` 인텐트로 이미 닫혀 있었고, 이제는 목록 엔트리 자체가 새것이다.
  > **남은 것은 배너 쪽**이다 — 문구·복사 동작·1회 보장에 정책 근거가 없다(OQ-P-339).
- **해소 메모**: 반영처 — [navigation-flow](../architecture/navigation-flow.md) "그룹 생성·참여 플로우" ·
  [a005 스펙](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·[s102 스펙](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md)의
  "다음 화면"·"복귀 목적지" 서술. 위키 쪽은 정본과 맞았으므로 등록할 것이 없다.

### [2026-08-12] 백스택 리셋 관용구가 둘로 갈렸다 — `clearBackStack()`+`goTo` vs `goToSingleClearTop()`

- **ID**: OQ-P-136
- **출처**: `core/navigation/Navigator.kt#goToSingleClearTop`(PR #224 신설) × `SplashRoute.kt`·`TermAgreeRoute.kt`의 `clearBackStack()` + `goTo` — 둘 다 "되돌아가면 안 되는 경계"를 표현하는데 전제가 다르다. 앞은 스택을 비우고 새로 쌓아 **대상 화면이 새로 만들어지고**, 뒤는 스택에 이미 있는 엔트리 위만 잘라내 **상태·ViewModel이 살아난다**. 고르는 기준이 문서에도 코드 주석에도 없고, `clearBackStack()`은 단독으로 부르면 백스택이 비어 크래시 위험이 있어 항상 `goTo`가 따라와야 한다는 암묵 규약까지 얹혀 있다.
- **항목**: ① 두 관용구의 선택 기준을 명문화할지(대상이 스택에 있으면 SingleClearTop, 진입 체인 리셋은 clearBackStack), ② `clearBackStack()`을 목적지 인자를 받는 형태로 합쳐 "비우고 안 쌓는" 상태를 타입에서 지울지, ③ `goToSingleClearTop`이 **엔트리를 재사용**한다는 성질이 목록 재조회 미발생과 직결되므로([2026-08-12] mock 항목) 화면 갱신 규약과 함께 볼지.
- **상태**: 부분 해소 (② 해소 — 2026-08-15 PR #260로 `clearBackStack()` 제거 / ①③ 잔존 — 선택 기준 부재)
- **해소 메모**: 정하면 [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인"·"그룹 생성·참여 플로우" 두 절과 신규 목적지 체크리스트에 반영한다.

  > 📌 **세 번째 형태(2026-08-15, PR #231)** — C-301 배경 편집으로 돌아오는 경로는 `navigator.onBack()`을 **두 번 연달아** 부른다(확인 화면·카메라/갤러리를 각각 걷는다). 명시적 관용구가 아니라 **스택 깊이 가정**이라, 중간에 화면이 하나 끼면 조용히 어긋난다.
  > ✅ **②가 해소됐다(2026-08-15, PR #260)** — `clearBackStack()`이 **제거**되고 목적지를 받는
  > `replaceAll(destination)`으로 합쳐졌다. "비우고 안 쌓은" 중간 상태를 API에서 지운 것이고, 호출부
  > 3곳(`Splash`·`TermAgree`·`Login`)이 함께 옮겨졌으며 강제 로그아웃이 네 번째 소비처다.
  > `NavigatorTest`가 "항상 비지 않는다"를 잠근다. **①③은 그대로다** — 선택 기준(`replaceAll` vs
  > `goToSingleClearTop` vs `onBack()` 2회)은 여전히 문서·주석 어디에도 없다.

  > 📌 **네 번째 형태가 생기고, 갈래 하나는 소비처를 잃었다(2026-09-01, PR #411)** — 그룹 생성·참여의
  > 복귀가 `goToSingleClearTop(NavKeyGroupList)`에서 **`replaceAll(NavKeyGroupList)` + `goTo(NavKeyCanvasMain(…))`
  > 두 줄**로 바뀌었다. 흐름 화면을 걷어내되 **머무는 곳이 목록이 아니라 그 위 화면**이라 기존 셋 중
  > 어느 것도 맞지 않아 조합이 됐고, 목록을 깔아 두는 이유(뒤로가기가 앱을 끄지 않게)를 두 Route가
  > 같은 주석으로 적는다 — **선택 기준이 문서가 아니라 주석 복제로 유지되는 형태가 반복됐다.**
  > 이 이동으로 `goToSingleClearTop`은 **develop에서 소비처가 0건**이 됐다 — 함수는 `Navigator`에
  > 남아 있고 `NavigatorTest`가 잠그는 것은 `replaceAll`·`popUpTo`·`onBack`뿐이라 **이 갈래를 확인하는
  > 자리는 코드에도 테스트에도 없다.** 즉 ①은 갈래가 줄어 쉬워진 것이 아니라, 쓰이지 않는 갈래 하나와
  > 문서 없는 조합 하나를 동시에 안게 됐다.

### [2026-08-12] 확인 모달의 문구·좌우 배치가 정책 소스 없이 코드로 확정됐다

- **ID**: OQ-P-137
- **출처**: `feature/groups/enter/impl` `strings.xml`(`group_create_confirm_*`·`group_enter_confirm_*`)·`GroupCreateScreen.kt`·`GroupInviteCodeScreen.kt`(PR #224 develop 머지) — ① 위키에 그룹 생성·참여 확인 모달의 **존재도 문구도 정책 문서가 없다**([[기능정의서-v6]]에 팝업 언급 없음). A-002 온보딩 문구와 같은 방식으로 코드가 먼저 확정했다. ② 두 화면은 **취소=좌 Secondary / 실행=우 Primary**인데, 미머지 [Danger Zone 팝업 스펙](../superpowers/specs/archive/2026-08-09-setting-danger-zone-popups.md)은 피그마 근거로 **파괴적 액션=좌 Secondary / 취소=우 Primary**다 — 같은 컴포넌트에서 "오른쪽이 무엇인가"가 화면마다 뒤집힌다. ③ 참여 모달의 Primary는 "참여하기"인데 실제로는 닉네임 입력 화면으로 **이동만** 하고 합류는 그다음 화면 몫이다(문구가 약속하는 시점과 코드의 시점이 어긋난다). ④ dismiss 가드도 비대칭이다 — A-005는 `isCreating` 중 닫기를 막고 `isEnabledButton`으로 두 버튼을 함께 비활성하지만, A-004는 가드가 없다.
- **항목**: ① 모달 문구를 디자인·기획 소스로 확정받을지(피그마 프레임 존재 여부부터), ② `YGModalPopup` 좌우 배치 규약을 세울지 — 파괴/비파괴로 가를지, 아니면 "확인은 항상 오른쪽"으로 통일할지(뒤집으면 Danger Zone 스펙이 함께 바뀐다), ③ "참여하기" 문구를 이동 의미로 바꿀지 실제 합류를 이 시점으로 옮길지, ④ 진행 중 dismiss 가드를 두 화면에 통일할지.
- **상태**: 부분 해소 (③④ 해소 — 2026-08-16 PR #261, ④의 잔여 비대칭도 2026-08-27 PR #393·#394로 소멸 / ①② 잔존: 문구·배치 근거 부재)
- **해소 메모**: ②를 정하면 [design-system](../architecture/design-system.md) `YGModalPopup` 노트와 [ygmodalpopup 스펙](../superpowers/specs/archive/2026-07-15-ygmodalpopup.md)의 "버튼 의미는 호출자 소관" 서술에 규약 한 줄을 얹는다(현재는 컴포넌트가 의미를 규정하지 않는다는 것만 적혀 있다).

  > ✅ **③④가 해소됐다(2026-08-16, PR #261)** — 참여 확인 모달이 A-004에서 **S-102로 내려갔다**.
  > ③ 모달의 "참여하기"가 이제 실제로 `POST join`을 부르므로 문구와 코드의 시점이 맞고, 그 자리에서
  > 닉네임 `PATCH`까지 이어진다. ④ dismiss 가드도 생겼다 — S-102는 `isEntering` 중 닫기를 막고, A-004는
  > 모달 자체가 없어져 비대칭이 사라졌다(`isEnabledButton` 미지정은 A-005와 여전히 다르지만, 진행 중
  > 닫기가 막히므로 두 버튼이 눌릴 창이 없다). **①②는 그대로** — 문구·좌우 배치가 그대로 옮겨왔을 뿐이라
  > `YGModalPopup` 호출자 7곳의 좌우 진영 분포도 변하지 않았다.

  > 📌 **7번째 소비처가 파괴적=좌 쪽에 붙었다(2026-08-15, PR #231)** — C-301 배경 편집의 그만두기 확인이 `그만두기`=좌 Secondary / `계속 편집하기`=우 Primary다. 여섯 곳이 반으로 갈려 있던 상태에서 파괴적=좌 진영이 하나 앞섰고, 문구도 같은 모듈 탈퇴 확인과 `그만두기`를 **반대 의미로** 쓴다(거기서는 닫기).
  > 📌 **②가 가정에서 사실이 됐다(2026-08-13, PR #225)** — Danger Zone 확인 팝업 3종(서비스 탈퇴·그룹 나가기·그룹 신고)이 **파괴적 액션=좌 Secondary / 취소=우 Primary**로 머지돼, `YGModalPopup` 호출자 6곳의 좌우 의미가 정확히 반으로 갈렸다(#224 3화면=실행이 우 / #225 3팝업=취소가 우). 어느 쪽도 코드 결함이 아니라 규약이 없는 것이며, 네 인자가 전부 같은 타입이고 `Dialog`가 프리뷰에 안 떠서 **뒤바꿈을 잡는 자동 검증은 여전히 0건**이다.

  > ✅ **④의 마지막 비대칭이 사라졌다(2026-08-27, PR #393·#394)** — A-005·S-102가 둘 다 **요청 직전에
  > 팝업을 닫는** 쪽으로 옮기면서, A-005의 `isEnabledButton = isCreating.not()` 전달과 두 화면의
  > 진행 중 dismiss 가드가 함께 걷혔다. 팝업이 이미 없으니 막을 것도 비활성할 것도 없다.
  > 진행은 `YGScaffoldV2` 로딩 오버레이가, 실패는 토스트가 말한다 — Danger Zone 3종이 PR #287에서
  > 고른 것과 같은 관용구이고, 이로써 `YGModalPopup`을 쓰는 화면의 **진행 중 처리**는 갈리지 않는다.
  > **①②는 그대로다** — 문구도 좌우 배치도 손대지 않았다.


### [2026-08-13] S-101 그룹 설정 화면이 도달 불가로 머지됐다

- **ID**: OQ-P-138
- **출처**: `feature/groups/setting/api/NavKeyGroupSetting.kt`·`feature/groups/setting/impl/navigation/EntryBuilder.kt`(PR #223·#225 develop 머지) — develop 전체에서 `NavKeyGroupSetting` 참조는 **선언과 entry 등록 두 곳뿐**이고 `goTo` 호출자가 없다. 화면 본문(닉네임 인라인 편집·그룹원 목록·초대 코드 복사·Danger Zone)과 확인 팝업 2종이 전부 들어왔는데 앱에서 열 방법이 없다. 진입 후보는 G-001 그룹 목록 또는 C-001 캔버스이나 어느 쪽에도 진입점 UI가 없다. C-001(도달 불가)·A-005(약 2주 뒤 호출자 확보)에 이어 같은 패턴이 세 번째다.
- **항목**: ① 진입점을 어느 화면에 둘지 확정(위키 [[화면-ID-체계]]상 `S-` = Sidebar라 상단바 메뉴가 자연스러우나 문서 근거가 없다), ② 진입 시 `groupId`를 넘겨야 하는데 `NavKeyGroupSetting`이 `data object`라 인자 추가가 함께 필요하다, ③ 화면을 도달 가능하게 만들기 전까지 실기기 육안 확인 항목(S-101 9건 + 팝업 8건)이 통째로 막혀 있다는 점을 어떻게 다룰지.
- **상태**: **해소됨(2026-08-17, PR #285)**
- **해소 메모**: ①은 **C-001 캔버스 상단 메뉴**로 확정됐다(위키 [[화면-ID-체계]]의 `S-` = Sidebar와 어긋나지 않는다 — 캔버스에서 여는 사이드 메뉴다). ② `NavKeyGroupSetting`이 `data class(groupId)`가 되고 C-001이 자기 `groupId`를 그대로 넘긴다. ③ 실기기 육안 확인 항목(S-101 9건 + 팝업 8건)은 **막혀 있지 않게 됐을 뿐 수행되지는 않았다** — 실기기 미검증은 [OQ-P-146] 축으로 남는다. 도달 불가 기간은 약 4일. 반영: [s101-group-setting-api 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md) · [navigation-flow](../architecture/navigation-flow.md) "그룹 설정 진입·이탈".

### [2026-08-13] S-101 데이터가 전량 mock이고 서버 계약에 필요한 필드가 없다

- **ID**: OQ-P-139
- **출처**: `GroupSettingViewModel.kt`의 `MOCK_GROUP_NAME`·`MOCK_MY_NICKNAME`·`MOCK_INVITE_CODE`·`MOCK_REMAINING_COUNT`·`MOCK_MEMBER_NICKNAMES`(전부 `GroupSettingUiState` 기본값) × [api/parfait-group.md](../api/parfait-group.md) — 화면이 쓰는 값 중 **서버 `GET /api/parfait-groups/{groupId}` 응답에 없는 것이 둘**이다: 상단바 제목이 되는 `groupName`, `N명 남음` 계산에 필요한 `memberLimit`. 응답은 `groupId`·`groupNickname`·`inviteCode`·`members`만 준다. 닉네임 변경·그룹 나가기·신고도 엔드포인트는 있으나 호출하는 코드가 없다(확인 핸들러가 TODO). G-001·C-001과 같은 뿌리의 mock이지만, 이 화면은 **계약 자체가 화면을 못 채운다**는 점이 다르다.
- **항목**: ① `groupName`을 그룹 목록 API에서 받아 NavKey로 넘길지 서버에 필드 추가를 요청할지, ② `memberLimit`(위키 [[그룹]] 최대 12명)을 서버가 줄지 클라이언트 상수로 둘지 — 상수로 두면 정책 변경 시 앱 배포가 필요하다, ③ 컬러칩 타입도 응답에 없다(아래 항목).
- **상태**: 해소됨 (2026-08-18 서버 delta — 계약 공백 둘이 채워졌다. 앱 반영은 OQ-P-216·OQ-P-224)
- **해소 메모**: 서버 소관 결정이면 [api/parfait-group.md](../api/parfait-group.md) Android 매핑 절에, 클라이언트 소관이면 [s101 스펙](../superpowers/specs/archive/2026-08-07-s101-group-side-menu.md)에 반영한다.
  > 📌 **①은 앱이 임시로 답했고 ②는 그대로다(2026-08-17, PR #285)** — mock 5종이 전부 걷히고 화면이
  > 서버를 본다. 그룹명은 **NavKey가 아니라 `GetGroupDetailUseCase`가 `getMyGroups()`를 한 번 더 불러**
  > 붙인다(이름 조회 실패는 실패로 치지 않고 빈 제목). 두 코드가 `TODO(서버 응답 확장 대기)`로
  > "서버가 상세에 `groupName`을 실으면 걷어낸다"를 명시하므로 **①은 임시 답이지 확정이 아니다**
  > → [2026-08-17] 상세 조회 2회 항목.
  > ⚠️ **②는 오히려 눈에 띄게 됐다** — `remainingCount`만 mock 1로 남아, 나머지가 전부 실데이터인
  > 화면에서 **"1명 남음"이 그럴듯하게 틀린 값**으로 보인다. 전에는 화면 전체가 mock이라 오해할
  > 여지가 없었다. ③은 [OQ-P-140] 그대로다.
  > ✅ **①②③이 전부 서버에서 닫혔다(2026-08-18 서버 delta `08df1bf`)** — 그룹 상세 응답이
  > `groupName`·`memberLimit`을 싣고 `members[].nametagChip`까지 준다
  > ([api/parfait-group.md](../api/parfait-group.md)). **계약 공백이라는 이 항목의 근거가 사라졌다** —
  > 남은 것은 앱이 그 필드를 읽는 작업이고(그러면 목록 조합·`GroupDetailVO`·mock 1·인덱스 순환이 함께
  > 걷힌다) 그것은 OQ-P-216·OQ-P-224의 항목이다. ③의 "부여 주체" 결정 자체는 OQ-P-223으로 옮겨간다.

### [2026-08-13] 네임태그 컬러칩 배정 주체가 여전히 미정 — 첫 소비처가 인덱스 순환으로 열렸다

- **ID**: OQ-P-140
- **출처**: `GroupSettingViewModel.kt`의 `NAMETAG_CHIP_TYPES` + `MOCK_MEMBERS`(PR #223 develop 머지) — 위키 [[nametag-chip]]은 "타입은 유저별로 **고정**"이라고 정하지만 **부여 주체를 적지 않았고**, 서버 `ParfaitGroupMemberResponse`도 `memberId`·`groupNickname` 2필드뿐이라 타입 정보가 없다. `YGColorChipType`이 12종+Plus로 정렬되며 개수 쟁점([2026-07-18])은 닫혔는데, 그 타입들의 **첫 화면 소비처**가 목록 인덱스 `% 12` 순환으로 열렸다 — 멤버가 나가고 들어오면 남은 사람의 색이 바뀐다. 정책이 요구하는 고정성과 정반대다.
- **항목**: ① 타입 부여 주체를 서버로 할지(응답에 필드 추가) 클라이언트가 `memberId` 해시로 유도할지, ② 후자면 그룹 간 같은 유저가 같은 색인지(앱 닉네임처럼 계정 공통인지) 정해야 한다 — 위키 [[nametag-chip]]에 그 범위가 없다, ③ G-001의 `YGGrouptagChipType`도 전 항목 동일 값 고정이라 같은 결정에 걸린다([2026-08-07] 토핑 항목).
- **상태**: 미해결 (① 서버로 확정·②③ 계약이 답함 — 2026-08-18. 남은 것은 정책 문서 공백(OQ-P-223)과 앱 미반영(OQ-P-224))
- **해소 메모**: 정하면 위키 [[nametag-chip]]에 부여 주체·범위를 추가하고(정책 소관), 구현 쪽은 [s101 스펙](../superpowers/specs/archive/2026-08-07-s101-group-side-menu.md) "컬러칩 배정 규칙"과 [ygcolorchip 스펙](../superpowers/specs/archive/2026-07-18-ygcolorchip.md)에 반영한다.
  > ⚠️ **가정이 실재가 됐다(2026-08-17, PR #285)** — `NAMETAG_CHIP_TYPES[index % 12]`는 그대로인데
  > 목록이 mock이 아니라 **서버 멤버**다. 멤버가 나가고 들어오면(그리고 이제 나가기가 실제로
  > 동작한다) 남은 사람의 색이 실제로 바뀐다. 코드의 TODO도 "서버가 타입을 주면 교체"로 남았다.
  > C-001 캔버스 멤버 칩(OQ-P-210)과 **같은 결정에 걸린 자리가 둘**이 됐다.
  > ✅ **①이 서버로 확정됐다(2026-08-18 서버 delta `08df1bf`)** — 그룹 상세 `members[].nametagChip`이
  > 생겼고 코드 TODO가 기다리던 조건이 충족됐다. **②의 답도 함께 나왔다: 계정 공통이 아니라 그룹별**이다
  > (`assignRandom`이 그 그룹의 활동 멤버가 안 쓰는 값 중에서 뽑는다) → [api/parfait-group.md](../api/parfait-group.md)
  > "Nametag-Chip 배정 규칙". ③도 계약이 답했다 — G-001 목록에 `lastPlacedByNametagChip`이 실린다.
  > **남은 것은 둘**이다: 정책 문서에 규칙이 없다는 것(OQ-P-223)과 앱이 아직 필드를 안 읽는다는 것
  > (OQ-P-224). 즉 이 항목의 "부여 주체 미정"은 끝났고 자리가 그 둘로 갈라진다.

### [2026-08-13] Danger Zone 확인 3종이 되돌릴 수 없는 동작을 담을 자리 없이 머지됐다

- **ID**: OQ-P-141
- **출처**: `AppSettingViewModel.kt#handleConfirmWithdraw`·`GroupSettingViewModel.kt#handleConfirmLeaveGroup`·`#handleConfirmReportGroup`(PR #225 develop 머지) — 세 핸들러 모두 멱등 가드를 통과하면 **팝업을 먼저 닫고** TODO 로그만 남긴다. 실제 네트워크 호출을 넣으려면 지금 없는 것이 셋이다: ① 두 `UiState` 어디에도 in-flight·error 필드가 없고, ② 팝업을 먼저 닫아 진행 표시·실패 재시도를 얹을 자리가 사라지므로 "닫고 나서 요청" 순서를 뒤집어야 하며, ③ `YGModalPopup.isEnabledButton`이 좌우 공용 단일 플래그라 "요청 중엔 확인만 비활성, 취소는 살림"이 표현 불가능하다. 게다가 **회원 탈퇴는 서버에 엔드포인트 자체가 없다**. 현재 구조의 기본값은 실패해도 "성공한 것처럼 팝업만 닫힘"이다.
- **항목**: ① 확인 핸들러의 순서를 "요청 → 결과 → 닫기"로 뒤집을지, ② `isEnabledButton`을 좌우 개별 플래그로 재분리할지(`YGModalPopup` 변경 — [ygmodalpopup 스펙](../superpowers/specs/archive/2026-07-15-ygmodalpopup.md)이 이미 "개별 비활성 불가"를 미결로 안고 있다), ③ 회원 탈퇴 엔드포인트를 서버에 요청할지, ④ 탈퇴·나가기 성공 후 이동할 화면(로그인 / 그룹 목록)을 정할지 — SideEffect 신설이 필요하다.
- **상태**: 해소됨 (2026-08-19 PR #306 — 셋 다 결선. ②·③은 채택이 아니라 **불필요해져서** 닫혔다)
- **해소 메모**: [Danger Zone 팝업 스펙](../superpowers/specs/archive/2026-08-09-setting-danger-zone-popups.md) "API 연동" 열린 질문의 develop 확정판이다. 연동 시 [state-management](../architecture/state-management.md)의 로딩·에러 표현 규약과 함께 본다.
  > ✅ **그룹 나가기·신고가 결선됐다(2026-08-17, PR #287)** — 세 구멍 중 **①만 채우고 나머지 둘은
  > 필요 없게 만들었다.** ① `isSubmittingDialogAction` 신설(+ 첫 조회·닉네임 왕복과 따로 들고
  > `isLoading`이 OR). ② 순서는 **뒤집지 않았다** — 팝업을 먼저 닫고 `YGScaffoldV2` 로딩 오버레이가
  > 화면을 덮는다. 근거는 "팝업을 띄운 채 두면 그 덮개 아래 가려 아무것도 알리지 못한다"이고,
  > 실패는 공통 토스트가 말한다. ③ 그래서 `isEnabledButton` 좌우 분리도 손대지 않았다
  > ([ygmodalpopup 스펙](../superpowers/specs/archive/2026-07-15-ygmodalpopup.md)의 미결은 그대로 남는다).
  > ④ 목적지는 **그룹 목록**으로 확정(`replaceAll(NavKeyGroupList)` — 백스택이 전부 떠난 그룹 것이라
  > 돌아가면 403뿐이다). 연타 방어는 `launch(key)` + 멱등 가드이고 테스트가 "두 번 눌러도 API 1회"를
  > 잠근다 → [s101-group-setting-api 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md).
  > ⚠️ **회원 탈퇴(S-003)만 그대로 stub이다** — 서버 엔드포인트(`DELETE /api/v1/users/me`)도 앱 표면도
  > 있는데 확인 핸들러가 로그 한 줄이다. 같은 화면의 로그아웃은 결선돼 있어 **한 Danger Zone 안에서
  > 하나는 동작하고 하나는 안 한다**(OQ-P-186).
  > ✅ **마지막 하나도 결선됐다(2026-08-19, PR #306)** — S-001 탈퇴 확인이 `WithdrawUseCase`를 부르고
  > **S-101이 확정한 형태를 그대로 따랐다**: 팝업을 먼저 닫고 `YGScaffoldV2` 로딩 오버레이가 덮으며,
  > 실패는 공통 토스트가 말한다. ①은 `isWithdrawing` 신설(`isLoading`이 `isLoggingOut`과의 OR),
  > ②·③은 이번에도 손대지 않았다. ④ 목적지는 **로그인**으로 확정(`replaceAll(NavKeyLogin)` — 탈퇴는
  > 세션 자체가 끝나므로 그룹 목록으로 갈 자리가 없다). 연타 방어는 `launch(key)`이고 테스트가
  > "요청 중 다시 눌러도 API 1회"를 잠근다 — **되돌릴 수 없는 요청이라 나가기·신고보다 이 잠금이
  > 무겁다**. 남은 물음은 이 항목이 아니라 **성공 뒤 정리 경로**로 옮겨 간다(OQ-P-242).

### [2026-08-13] `core:ui`의 표시 매핑 확장이 숨은 `:domain` 의존 위에 서 있다

- **ID**: OQ-P-142
- **출처**: `core/ui/build.gradle.kts`(`implementation(projects.domain)`)·`core/ui/.../text/NameValidResultUiText.kt#toStringResource`(PR #223 develop 머지) — 확장 함수가 `public`이고 리시버가 `NameValidResult.Error`(domain 타입)라 **public API 시그니처에 domain이 노출되는데 의존은 `implementation`이라 소비자에게 전파되지 않는다.** 지금 컴파일되는 것은 소비 feature 4곳이 컨벤션 플러그인으로 `:domain`을 직접 갖고 있기 때문이고, 그 컨벤션에서 `:domain`이 빠지면 원인 불명으로 깨진다.
- **항목**: ① `api(projects.domain)`으로 승격할지 — 저장소에 `api(...)` 선언이 **0건**이고 컨벤션 플러그인 `DependencyHandler`에 `api` 확장 함수 자체가 없어 build-logic 변경이 선행한다(`0fbddfb1`·`09f49a92`가 과거에 `api`를 되돌린 이력도 있다), ② 아니면 확장을 `internal`로 낮추고 `core:ui`가 표시용 래퍼를 노출할지, ③ 그대로 두고 컨벤션 플러그인이 `:domain`을 항상 준다는 것을 규약으로 명문화할지.
- **상태**: 미해결 (현재 컴파일·동작 정상 — 잠재 취약)
- **해소 메모**: 정하면 [module-structure](../architecture/module-structure.md) `core:ui` 행과 [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)에 반영한다.

### [2026-08-13] `GroupInviteCodeRoute`가 IME 인셋을 두 번 적용한다

- **ID**: OQ-P-143
- **출처**: `feature/groups/enter/impl/navigation/EntryBuilder.kt#featureGroupInviteCodeEntryBuilder`(`navigationBarsAndImePadding()`) × `invitecode/GroupInviteCodeRoute.kt`(`modifier.imePadding()`) — entry가 이미 IME 인셋을 붙이는데 Route가 같은 인셋을 한 번 더 얹는다. S-101 라운드가 자기 화면의 같은 증상(확인 버튼이 내비게이션 바 높이만큼 떠오름)을 `consumeWindowInsets`로 고치며 지목한 항목인데, **기전은 다르다** — S-101은 `YGScaffold` `innerPadding` 미소비였고 이쪽은 `contentWindowInsets = WindowInsets(0.dp)`라 `innerPadding`이 0이고 수동 인셋끼리 겹친다. 같은 파일의 `GroupNickName`·`GroupCreate` entry는 Route가 `imePadding()`을 안 써서 무사하다.
- **항목**: ① Route의 `imePadding()`을 제거할지 entry의 `navigationBarsAndImePadding()`을 제거할지, ② 세 entry가 공유하는 인셋 관용구(`contentWindowInsets = WindowInsets(0.dp)` + 수동 패딩)를 유지할지 S-101 형태로 통일할지([2026-08-07] 인셋 관용구 항목과 같은 결정), ③ 실기기에서 실제로 키보드 위 여백이 두 배인지 확인 — 코드 대조만 했다.
- **상태**: 해소됨 (① **PR #237 develop 머지, 2026-08-14** — Route의 `imePadding()` 제거, entry 단독. ②는 [2026-08-07] 관용구 항목으로 넘어가고 ③은 대상 소멸)
- **해소 메모**: 같은 PR이 매니페스트에 `android:windowSoftInputMode="adjustResize"`를 붙였다 — `MainActivity` 단일 액티비티라 **앱 전 화면에 걸리는 변경**인데 실기기 확인 기록이 없다(다른 입력 화면의 인셋 체감이 함께 달라질 수 있다). [navigation-flow](../architecture/navigation-flow.md) 인셋 사례 블록과 [a004 스펙](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md)에 반영했다. 관용구 통일(②) 자체는 여전히 [2026-08-07] 항목 소관이다.

### [2026-08-13] MVI 베이스 확장이 과도기를 만든다 — 새 API를 쓰는 화면과 안 쓰는 화면 공존

- **ID**: OQ-P-144
- **출처**: [ADR-0020](../adr/0020-mvi-error-effect-infrastructure.md) · [mvi-error-infrastructure 스펙](../superpowers/specs/archive/2026-08-13-mvi-error-infrastructure.md) — `BaseViewModel`에 `launch`·`postError`·`error`를 더하되 기존 19개 ViewModel은 손대지 않기로 했다(하위호환 유지·점진 이관). `Channel` 전환만 호출부 수정 없이 전 화면에 적용된다.
- **항목**: ① 이관을 각 화면의 API 결선 라운드에 묶는 방식이 유지되는지, 아니면 어느 시점에 일괄 정리 라운드를 두는지. ② 이관 전 화면이 `viewModelScope.launch`를 직접 쓰는 동안 예외 가드가 없는 상태가 남는데 이를 허용 범위로 볼지. ③ `isLoading` 필드명 규약이 인터페이스 강제 없이 실제로 지켜지는지 — 첫 두세 화면 결선 후 재점검.
- **상태**: 미해결 (구현 완료 후에도 유효)
  > 📌 **2026-08-15 develop 머지**(PR #241) — 19개 ViewModel diff 0줄로 들어갔다. ②의 "이관 전 화면은
  > 예외 가드가 없다"는 그대로다. ③ `isLoading` 규약은 A-002 한 화면만 따랐다(첫 사례).
  > `error` 채널이 철회돼(ADR-0020 번복) **이관해야 할 API 표면 자체가 줄었다** — 남은 것은
  > `launch(key, onError)` 하나다.
  > 📌 **①의 방식이 실제로 작동했다(2026-08-15, PR #242·#243·#244·#248)** — 하루에 다섯 화면
  > (약관·A-005·A-004·S-102·G-001)이 **API 결선 라운드에 묶어** `viewModelScope.launch` → `launch(key = …)`로
  > 옮겼다. **관용구도 함께 굳었다**: job 키 상수를 `private companion object`에 두고, 진행 플래그는
  > `launch` 밖에서 켜고 `finally`에서 끈다(가드에 막혀도 화면은 진행 중으로 보여야 하므로).
  > ②는 아직 남은 화면들에 유효하고, ③ `isLoading` 규약은 **지켜지지 않았다** — 다섯 화면이
  > `isLoading`·`isRefreshing`·`isSigningUp`·`isCreating`·`isSubmitting`·`isEntering`으로 제각각이다.
- **해소 메모**: 이관이 끝나면 [state-management](../architecture/state-management.md) 체크리스트를 새 API 기준으로 고치고 이 항목을 닫는다.

### [2026-08-13] 이펙트 2중 수집은 어느 primitive로도 조용히 오동작한다

- **ID**: OQ-P-145
- **출처**: [ADR-0020](../adr/0020-mvi-error-effect-infrastructure.md) "영향 → 위험·방어" — `Channel`은 이벤트를 한 수집자에게만 주고 `SharedFlow`는 모두에게 줘서 내비게이션을 두 번 실행한다. 둘 다 틀린 결과이므로 규약("이펙트 수집은 Route 한 곳")을 동시 구독자 카운트 로그로 감시하기로 했다. 현재 `effect` 수집 지점은 화면당 정확히 하나이고 ViewModel을 자식 컴포저블로 내려주는 곳은 없다.
- **항목**: ① 로그로 충분한지, 디버그 빌드에서 예외로 올릴지. ② 앱 전역 알림(세션 만료 등) 같은 진짜 멀티캐스트 요구가 생기면 별도 `SharedFlow`를 노출하기로 했는데, 그 자리를 `core:ui`가 공통으로 제공할지 각 ViewModel이 알아서 둘지.
- **상태**: 부분 해소 (2026-08-14)
  > ✅ **②는 결정됐다** — 공용 `error` 채널이 철회되면서(ADR-0020 번복) `core:ui`가 공통 스트림을
  > 제공하지 않기로 했다. 세션 만료 같은 앱 전역 관심사는 별도 앱 스코프 버스 소관이고 아직 없다.
  > ①(로그로 충분한가 / debug 예외로 올릴까)은 그대로 미결이다. 동시 구독자 카운트 로그는 들어갔다.
- **해소 메모**: 실제 위반 사례가 나오면 [state-management](../architecture/state-management.md) 안티패턴 절에 추가한다.

### [2026-08-14] A-002 로그인 실기기 검증 9항목 미수행 — 앱 최초 실서버 호출이 한 번도 안 돌았다

- **ID**: OQ-P-146
- **출처**: [a002-kakao-login-api 스펙](../superpowers/specs/archive/2026-08-13-a002-kakao-login-api.md) "실기기 검증" — 구현·유닛 테스트·리뷰는 끝났고 **PR #241로 2026-08-15 develop에 머지됐으나** 실물 기기·실제 카카오 계정·개발 서버가 필요한 항목은 하나도 돌지 않았다. 즉 **검증 안 된 로그인 경로가 develop에 있다.** 컴파일·ktlint·Hilt 어디에도 안 걸리는 종류의 결함이 여기서 처음 드러난다.
- **항목**: ① 개발 서버에 요청이 나가는가(평문 HTTP 차단이면 즉시 중단 → OQ-P-076) ② 신규 계정 → 약관 화면, 응답 판별자 키가 실제로 `isNewUser`인가(`MissingFieldException`이면 [api/auth.md](../api/auth.md)가 틀린 것) ③ 기존 계정 → 그룹 목록, 백스택 비움 ④ 로그인 → 앱 종료 → 재시작 → 토큰 읽힘, DataStore 파일에 평문 없음(ADR-0019 검증) ⑤ 카카오 창 취소 → 로딩 풀림 ⑥ 버튼 연타 → 카카오 창 1회 ⑦ 비행기 모드 → 로딩 풀림 + `AppError.Network` 로그 ⑧ `TokenStoreTokenProvider`의 `runBlocking` 체감 지연 ⑨ **카카오 창 떠 있는 동안 화면 회전 → 로딩 풀림**(이번 라운드 fix 대상, 유닛 테스트로 못 덮는다)
- **상태**: 미해결 (실기기 대기 — **검증 안 된 실서버 경로가 하루 만에 8 엔드포인트로 늘었다**)
  > ⚠️ **2026-08-15 같은 날 네 라운드가 더 머지됐다**(PR #242·#243·#244·#248) — 약관 조회·회원가입·
  > 그룹 목록·생성·참여 미리보기·참여·닉네임 변경이 전부 실서버를 타는데 **어느 것도 실기기로 안 돌았다**.
  > 검증 항목도 그만큼 늘어난다: 신규 가입 끝까지(약관 조회 → signup → 세션 저장 → 목록 조회),
  > 그룹 생성 후 목록 반영, 초대코드 실패 3갈래(없는 코드·이미 참여·정원 초과) 문구, 닉네임 중복 409.
  > 특히 **그룹 목록은 코드 대조만으로 이미 실패가 예상된다**(업로드 시각 파싱, OQ-P-165) — 실기기 1회로
  > 바로 드러날 종류다.
  > ⚠️ **검증 대상이 또 늘었다(2026-08-15, PR #260)** — 세션 인프라의 수동 확인 4항목이 그대로 미수행이다:
  > ① 만료된 access token으로 조회 시 **화면에 에러 없이** 목록이 그려지는가(재발급 투명성) ② refresh
  > token까지 무효화한 뒤 아무 API 호출 → 로그인 화면 이동 + 뒤로가기 불가 ③ **비행기 모드에서
  > 로그아웃되지 않는가**(이 갈래가 무너지면 지하철 진입이 곧 로그아웃이다) ④ 설정에서 로그아웃 →
  > 재로그인. 셋 다 유닛 테스트가 잠근 분기지만 실기기에서만 드러나는 실패 양상이 따로 있다
  > (`runBlocking` 체감 지연·디스패처 고갈).
  > ⚠️ **재진입·시간 경계 항목이 붙었다(2026-08-17, PR #297)** — 유닛 테스트가 인텐트까지만 잠그므로
  > 실기기에서만 보이는 것이 넷이다: ① 다른 화면에 갔다 오면 목록·캔버스가 실제로 갱신되는가
  > (`LifecycleResumeEffect`가 기대한 시점에 도는가 — 대화상자·권한 요청처럼 **부분 정지**하는 경로에서
  > 몇 번 도는지 포함) ② 새로고침 실패 토스트가 실제로 뜨는가(비행기 모드로 당기기) ③ 재진입 왕복이
  > 잦을 때 `/parfaits/today` 호출이 눈에 띄는 지연·중복을 만드는가 ④ 화면을 열어 둔 채 자정을 넘겼을 때
  > `syncToday()`가 날짜와 캔버스를 갈아 끼우는가.
  > ⚠️ **처음으로 쓰기 경로가 develop에 들어왔는데 그것도 안 돌았다(2026-08-22, PR #334)** —
  > C-106 결선 스택 넷이 한 머지로 들어오면서 발급 → S3 PUT → confirm → 배치 **네 단계가 사용자
  > 조작에 걸렸다.** 그전까지 미검증 경로는 전부 읽기였고, 이번 것은 실패해도 서버에 흔적이 남는다
  > (고아 `PENDING` 이미지·S3 객체). 스택이 남긴 실기기 항목은 이월 13항목 + 신규 9항목이고 **하나도
  > 수행되지 않았다.** 특히 실기기 1회로만 갈릴 것 셋이다: ① presigned PUT에 `Authorization`이
  > 안 붙는가(붙으면 업로드가 아예 안 된다 — 유닛이 잠근 것은 인터셉터 판정뿐이다) ② 배치 화면에서
  > 본 위치·크기·각도가 캔버스에 같게 그려지는가(좌표 왕복은 순수 함수 테스트가 잠갔지만 두 화면의
  > 클립 모양이 다르다) ③ 테두리 색 `#RRGGBB` 왕복이 실제 응답에서도 성립하는가.
  > ⚠️ **두 번째 쓰기 경로가 하루 만에 붙었고 그것도 안 돌았다(2026-08-22, PR #329)** — C-301 배경
  > 저장이 업로드 세 단계 + PATCH를 확인 버튼 하나에 건다. 앞 라운드와 겹치지 않는 항목이 넷이다:
  > ① **소유 판정**(`isMine`)이 실제 응답에서 어느 쪽으로 갈리는가 — 계정 id와 그룹 멤버십 행 id를
  > 견주므로 실기기 1회로 남의 토핑이 만져지는지 내 토핑이 안 만져지는지가 바로 드러난다(OQ-P-250).
  > ② **마감된 캔버스**(03시를 걸쳐 화면을 열어 둔 상태)에서 확인을 누르면 "잠시 후 다시"가 뜨고
  > 다시 눌러도 영원히 실패하는가(OQ-P-261). ③ 갤러리에서 고른 **HEIC·WebP 등 계약 밖 형식**이
  > "다른 사진을 골라 주세요"로 걸러지는가(형식 판정이 시스템 MIME → 바이트 순으로 두 번 돈다).
  > ④ 저장한 배경이 캔버스 메인 재조회에서 실제로 그려지는가(이미지 배경은 URL을 응답으로만 아는데,
  > 지금 그 응답을 쓰지 않고 재조회에 맡긴다).
  > 📌 **항목 ⑨(카카오 창 회전)는 경로가 닫혔다(2026-08-22, PR #339)** — 앱이 세로로 고정되고
  > 카카오 리다이렉트 액티비티까지 함께 고정돼([ADR-0027](../adr/0027-portrait-orientation-lock.md))
  > 기기를 돌려도 그 구간이 재생성되지 않는다. **회전 말고 다른 구성 변경**(글꼴 크기·다크 모드
  > 전환·멀티윈도우)은 그대로이므로 확인 자체를 지우지는 않고, 재현 방법을 그쪽으로 바꾼다.
  > 나머지 항목은 전부 그대로다.
- **해소 메모**: ⑥은 버튼이 비활성이어도 시각적으로 동일하므로("눌리는가"로 확인, "비활성으로 보이는가"가 아니다) 주의한다. ⚠️ 디버그 빌드는 `HttpLoggingInterceptor.Level.BODY`라 logcat에 ID 토큰·nonce·발급 토큰이 찍힌다 — 그 로그를 PR·이슈에 붙이지 않는다. 결과에 따라 [api/auth.md](../api/auth.md) 판별자 키 항목과 [ADR-0019](../adr/0019-encrypted-token-storage.md) 검증 절을 갱신한다.

### [2026-08-14] 신규 가입자가 세션 없이 그룹 목록에 도달한다 — signup 라운드까지의 과도기

- **ID**: OQ-P-147
- **출처**: `feature/intro/impl/termagree/TermAgreeRoute.kt` — `NavKeyTermAgree(registrationToken)`로 토큰은 도착하지만 화면이 **받아 들고만 있고** `POST /api/v1/auth/signup`을 부르지 않는다. "다음"은 여전히 `clearBackStack()` + `goTo(NavKeyGroupList)`다. 즉 신규 계정은 access token 없이 목록 화면에 도달하고 첫 인증 호출이 401이 난다.
- **항목**: 의도된 과도기이므로 결정할 것은 하나다 — signup 라운드까지 **이 상태를 그대로 두는가**, 아니면 임시로 약관 화면 "다음"을 막아 잘못된 상태 도달 자체를 차단하는가. 후자면 실기기 검증에서 신규 계정 경로를 끝까지 볼 수 없다.
- **상태**: 해소됨 (2026-08-15, PR #242 — 같은 날 signup이 붙어 과도기가 하루 만에 닫혔다)
- **해소 메모**: `TermAgreeViewModel`이 `SignUpUseCase`를 호출하고 성공 시 UseCase가 세션을 저장한 뒤에야
  `NavigateToNext`가 나간다 — 즉 목록 화면에 도달할 때는 access token이 저장돼 있다.
  반영처: [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md)·[api/auth.md](../api/auth.md) signup 절.
  [a002-kakao-login-api 스펙](../superpowers/specs/archive/2026-08-13-a002-kakao-login-api.md) "주의"의 과도기 문단도 이 항목과 함께 유효기간이 끝났다.

### [2026-08-14] MVI 인프라 이월 minor 묶음 — 테스트 커버리지·관용구

- **ID**: OQ-P-148
- **출처**: `feature/mvi-error-infra-a002-login` 두 라운드의 task 리뷰·최종 리뷰가 Minor 로 분류해 이월한 것들. 개별로는 항목을 만들 값이 아니라 하나로 묶는다.
- **항목**: ① `BaseViewModel`의 `invokeOnCompletion` 정체성 검사(`=== job`)를 실제로 걸어보는 테스트 없음(단일 스레드 `TestDispatcher`로 구성 난이도 높음) ② 이펙트 실패모드 로그 2종(멀티 구독자 경고·버퍼 초과 드롭) 테스트 없음 ③ `toAppError`의 `else` 분기(비 `ApiException` `Throwable`) 미테스트 ④ `AppError` 서브클래스가 매 생성마다 스택트레이스를 채운다 — `AppError.Server`는 401 같은 *예상된* 결과인데 비용을 낸다(`Exception(message, cause, false, false)` 검토) ⑤ `catch (Throwable)`가 `OutOfMemoryError`까지 `AppError.Unexpected`로 바꾼다 ⑥ `core:ui`가 `:domain`을 `implementation`으로 두고 `AppError`를 공개 API에 노출한다 — 지금은 모든 소비자가 `:domain`을 직접 갖고 있어 컴파일되지만 `app-preview`가 `BaseViewModel`을 건드리면 깨진다(`api`로 전환 필요) ⑦ `AuthRepositoryImpl`의 `loginWithKakao` 성공 경로 테스트가 `NewUser`만 덮는다 ⑧ `toKakaoLoginVO`의 분기(`AuthRemoteDataSourceImplTest` 부재)가 어느 테스트에도 안 걸린다 — 다른 6개 도메인은 전부 DataSource 테스트가 있다
- **상태**: 미해결 (전부 비차단)
- **해소 메모**: ⑥은 `core/ui/build.gradle.kts` 한 줄이고 `app-preview`가 깨지는 날 강제된다. ⑧은 `signup`·`reissue`·`logout` 결선 라운드에서 `AuthRemoteDataSourceImplTest`를 만들며 함께 닫는 것이 자연스럽다.

### [2026-08-14] `CustomCameraRoute`의 `runCatching`이 취소를 저장 실패로 오분류한다

- **ID**: OQ-P-149
- **출처**: `feature/camera/impl/route/CustomCameraRoute.kt` — `runCatching { withContext(Dispatchers.IO) { saveViewfinderCapture(...) } }`. `withContext`가 suspend라 촬영 저장 중 화면을 벗어나면 취소가 `Result.failure`가 되고 "저장 실패" 경로로 분기한다. [data-layer](../architecture/data-layer.md) "suspend 를 감싸는 runCatching" 참고.
- **항목**: `core:util:jvm`의 `runSuspendCatching`으로 교체한다. 같은 부류였던 `EncryptedTokenStore.read`·`AddRecentImageUseCase`는 **PR #241로 develop에 고쳐져 들어갔고**(2026-08-15) 이 건만 남았다 — 카메라 화면이 그 브랜치 범위 밖이라 미뤘다.
- **상태**: 미해결 (교체 대상 확정, 라운드만 대기)
- **해소 메모**: 고칠 때 취소가 실패로 오지 않는 회귀 테스트를 함께 붙인다(`EncryptedTokenStoreTest`의 취소 케이스가 본보기다).

### [2026-08-15] 누끼 캔버스 Safe Margin +20%가 미이행 — 원본 전체 크기가 끝까지 실려 간다

- **ID**: OQ-P-150
- **출처**: `feature/segmentation/impl` `editor/ToppingEditMask.kt#buildCutoutBitmap`(결과가 언제나 `originBitmap` 크기) · `data` `ImageSegmentationRepositoryImpl.segmentImage`(subject 비트맵도 `image.width`×`image.height`)(PR #221 develop 머지) — 위키 [[누끼-따기]]의 C-103-Selected 규격은 **객체 바운딩 박스 + 상하좌우 20% Safe Margin**으로 잘라낸 캔버스를 만들어 C-103~C-105에 같은 스케일로 넘기라고 한다(여백만큼 사진이 없으면 투명 픽셀로 강제 확장). 코드는 `subjectBounds`를 계산해 두고도 **하이라이트 표시에만** 쓰고 크롭에는 쓰지 않아, 원본 해상도 그대로가 편집·저장·캔버스 배치까지 실려 간다.
- **항목**: ① 정책대로 Safe Margin 캔버스를 만들지 — 만들면 편집·테두리 좌표계가 전부 그 캔버스 기준으로 바뀌고 원본 밖으로 번지는 테두리가 잘리지 않게 된다(현재는 원본 경계에서 잘린다), ② 안 만들 거면 정책을 개정할지 — 원본 유지는 "지운 자리를 원본에서 되살리는" 편집 방식과 잘 맞는 선택이기도 하다, ③ 캔버스 배치(C-106)가 받는 이미지 크기 계약을 어디에 적을지.
- **상태**: 미해결 (정책 vs 코드 — 어느 쪽이 옳은지부터 결정 필요)

  📌 **같은 20%가 다른 자리에서 먼저 쓰였다(2026-09-10, PR #487)**. 재시도 회복 2단계의 힌트 크롭 여유가
  각 변 20%이고, 그 근거로 이 정책을 인용했다. 누끼 캔버스 Safe Margin을 이행한 것은 아니므로 이 항목은 그대로다.
- **해소 메모**: 결정 후 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md) "정책 대조" 표와 위키 [[누끼-따기]] C-103-Selected 절을 한쪽으로 맞춘다. 메모리도 함께 본다 — 원본 해상도 비트맵 2장이 `UiState`에 상주하고 저장 시 2장을 더 만든다.

### [2026-08-15] 브러시·테두리 굵기 단위가 정책 px와 코드 dp로 갈린다

- **ID**: OQ-P-151
- **출처**: `feature/segmentation/impl` `viewmodel/ToppingEditViewModel.kt`(`MIN_BRUSH_WIDTH_DP`·`MAX_BRUSH_WIDTH_DP`·`MIN_BORDER_WIDTH_DP`·`MAX_BORDER_WIDTH_DP`)(PR #221 develop 머지) — 위키 [[누끼-편집]]은 브러시·테두리 모두 **2~50px**인데 코드는 **2~50dp**다. 값은 같고 단위만 다르다. 코드 주석이 근거를 남겼다 — "사진 해상도나 기기 밀도가 달라도 체감 굵기가 같도록". 원본 좌표 환산은 `originPxPerDp`(미리보기 배율의 역수)로 저장 시점에 한다.
- **항목**: ① 정책의 "px"가 화면 px인지 원본 이미지 px인지 확정 — 원본 px이면 같은 굵기가 사진 해상도에 따라 전혀 다르게 보인다, ② 코드 판단(dp)을 정책으로 승격할지, ③ 상한 3배 확대(`MAX_ZOOM`)처럼 정책 문서에 없는 값이 함께 확정된 것도 같이 적을지.
- **상태**: 미해결 (코드가 근거를 적고 먼저 확정한 사례 — 정책 추인 대기)
- **해소 메모**: 정하면 위키 [[누끼-편집]] "브러시 / 테두리 크기" 절과 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md) 정책 대조 표를 맞춘다.

### [2026-08-15] 토핑 편집 플로우에 출구가 없다 — 닫기 4곳이 전부 빈 람다

- **ID**: OQ-P-152
- **출처**: `feature/segmentation/impl` `route/SegmentationRoute.kt`·`route/SegmentationConfirmRoute.kt`(둘 다 `onClickClose = { }` + TODO "편집 플로우 종료 후 이동할 화면 연결 필요") · `feature/camera/impl` `route/PictureConfirmRoute.kt`(`onClickClose = {}` TODO "c001-캔버스메인으로")(PR #221 develop 머지) — 화면마다 `YGFloatingBar`의 닫기 버튼이 그려지는데 눌러도 아무 일이 없다. 편집 화면(`ToppingEditRoute`)만 닫기가 `onBack()`이다. 즉 촬영·갤러리에서 들어오면 **뒤로가기로 한 칸씩 물러나는 것 말고는 나갈 길이 없다.**
- **항목**: ① 종료 목적지를 C-001 캔버스 메인으로 확정할지 — C-001 자체가 아직 도달 불가 화면이라([2026-08-12] 항목) 목적지를 정해도 진입 경로가 없다, ② 종료가 `goToSingleClearTop`인지 `clearBackStack()`+`goTo`인지([2026-08-12] 백스택 관용구 항목과 같은 결정), ③ 종료 시 편집 중이던 결과·캐시 파일을 어떻게 할지.
- **상태**: 해소됨 (**PR #309 develop 머지, 2026-08-20**)
- **해소 메모**: 세 닫기가 전부 결선됐다 — `SegmentationRoute`·`SegmentationConfirmRoute`·
  `PictureConfirmRoute`(`returnResultOnly = false`)가 `popUpTo<NavKeyCanvasMain>()`이고, 배경 편집에서
  들어온 경로만 `popUpTo<NavKeyCanvasBGEdit>()`로 갈린다(캔버스까지 튀면 편집 중이던 배경이 날아간다).
  ①은 C-001로 확정됐고 그 화면은 그 사이 도달 가능해졌다(PR #268). ②의 답은 둘 중 어느 쪽도 아닌
  **타입 기준 되감기**다 — `goToSingleClearTop`은 키 동등성 비교라 `NavKeyCanvasMain`의 `groupId`를
  알아야 하는데 카메라·세그멘테이션 NavKey가 그 값을 안 들고 다녀서 새 API(`Navigator.kt#popUpTo`)가
  생겼다. ③(종료 시 캐시 파일)은 **다음 진입이 통째로 비우는 쪽**으로 정해졌다(OQ-P-003 ③) —
  나가는 시점에 지우지 않는 이유는 되돌아가는 화면이 아직 그 파일을 보고 있을 수 있어서다 →
  [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md).
  `SegmentationLoadingScreen`·`SegmentationErrorScreen`·`SegmentationScreen`이 콜백 하나를 공유해
  한 자리를 채우자 셋이 함께 출구를 얻었다. `ToppingEditRoute`는 닫기 버튼 자체가 없어 대상이 아니다.

### [2026-08-15] C-103 다중 검출 선택과 실패 재시도가 통째로 빠졌다

- **ID**: OQ-P-153
- **출처**: `feature/segmentation/impl` `screen/SegmentationScreen.kt`(로딩/에러/본문 3분기, 본문은 `subjectBounds` 하나만 하이라이트) · `screen/SegmentationErrorScreen.kt`(닫기 버튼 하나)(PR #221 develop 머지) — 위키 [[누끼-따기]]는 [[기능정의서-v5]] 기준으로 **C-103-loading이 다중 검출 시 C-103-select로 분기**하고 **실패 시 재시도 또는 원본 사용 옵션**을 주라고 한다. 코드는 ML Kit `foregroundConfidenceMask`(단일 전경 마스크)만 쓰므로 대상이 애초에 하나이고, 에러 화면에는 재시도·원본 사용이 없다(닫기마저 빈 람다 → [2026-08-15] 출구 항목). `ModuleNotReady`는 코드가 "잠시 후 재시도하면 해결"이라 적어 둔 **일시적** 실패인데도 재시도할 수단이 없다.
- **항목**: ① 다중 피사체 선택을 지원할지 — 지원하려면 `SubjectSegmenter`의 `subjects`(개별 피사체 목록)로 갈아타야 하고 결과 모델이 `SegmentationBounds` 단수에서 복수로 바뀐다, ② 안 할 거면 위키의 C-103-select를 폐기 표기할지, ③ 에러 화면에 재시도 버튼을 둘지(최소한 `ModuleNotReady`에는 필요), ④ "원본 사용" 옵션을 살릴지.
- **상태**: **해소됨**(2026-09-06, PR #457 `5f860cb9c` — ①②③에 이어 ④가 닫혔다)
  > ✅ **①②③이 닫혔다(2026-08-23)** — ① 다중 피사체 선택을 **지원한다.** `subjects`로 갈아탔고
  > 결과 모델이 `SegmentationCandidate` 목록이 됐다. 실기기에서 점선 박스가 둘 이상 뜨는 것까지
  > 확인했다 → [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md).
  > ② 폐기 표기는 필요 없어졌다(지원하기로 했으므로). ③ **실패 표현이 다시 화면이 됐다** —
  > 디자인 `C-103-Error`가 나와 `SegmentationErrorScreen`이 되살아났고, PR #311의 판단이 뒤집혔다.
  > **셋 다 develop 코드다(2026-08-24, PR #342 = `34bf1939`).**
  >
  > ⚠️ **④는 남았고, 오히려 나빠졌다.** 디자인에 재시도·원본 사용 버튼이 **하나도 없다** — 문구로만
  > "다른 사진을 선택하거나 다시 시도해 주세요"라고 안내한다. 그리고 실패가 화면 전체를 덮으면서
  > 아래 문단이 적어 둔 "④의 절반이 뜻밖에 채워졌다"(원본 사진이 하이라이트만 빠진 채 남는다)가
  > **사실이 아니게 됐다.** 이제 실패하면 원본조차 보이지 않는다. 위키 [[누끼-따기]]의 "재시도 또는
  > 원본 사용 옵션"과 디자인이 정면으로 갈리는 자리이고, 어느 쪽이 정본인지 판단이 필요하다.
  > ③의 근거였던 `ModuleNotReady`("잠시 후 재시도하면 해결")도 여전히 재시도할 수단이 없다.
  > 📌 **③④의 대상이 사라졌다(2026-08-22, PR #311)** — 재시도·원본 사용을 얹을 자리로 지목했던
  > `SegmentationErrorScreen`이 삭제되고 실패가 **1회성 토스트**가 됐다(OQ-P-003 ① 정정 참고).
  > 그래서 ③④는 "있는 화면에 버튼을 더할지"가 아니라 **"실패 표현을 다시 화면으로 되돌릴지"**라는
  > 더 큰 물음이 됐다. 반대로 ④(원본 사용)는 뜻밖에 절반이 채워졌다 — 실패해도 화면이 걷히지 않고
  > **원본 사진이 하이라이트만 빠진 채 남는다**. 다만 그 상태에서 "이 원본을 그대로 토핑으로 쓴다"로
  > 넘어갈 길은 없어 여전히 뒤로 가는 것이 유일한 출구다. ①②(다중 검출)는 이 라운드와 무관하게 그대로다.
  > 📌 **재시도 버튼이 들어왔다(2026-09-03, PR #438 `24679f8c`)** — `SegmentationErrorScreen`이
  > `YGButton`(`Medium.Primary`)을 받고, 누르면 진입과 같은 흐름을 처음부터 다시 태운다
  > (`SegmentationIntent.Retry`). **③의 근거였던 "`ModuleNotReady`인데 재시도할 수단이 없다"가
  > 닫혔고**, 실패 문구도 원인별로 갈렸다(`SegmentationErrorKind`). ⚠️ **다만 이 버튼은 디자인
  > 검토를 받으려고 먼저 놓은 시안이라 모양도 자리도 바뀔 수 있다** — 디자인 `C-103-Error`에는
  > 여전히 버튼이 없다 → [segmentation-module-install 스펙](../superpowers/specs/archive/2026-09-02-segmentation-module-install.md) 「재시도」.
  > **④(원본 사용)는 그대로 남는다.**
  > ✅ **④가 코드로 닫혔다(2026-09-06, PR #457 `5f860cb9c`)** — 디자인 `C-103-Error` 확정본의
  > 「편집 없이 사용」 버튼이 들어왔다. 누르면 원본 사진을 그대로 토핑 재료로 삼아 `C-103-confirm`으로
  > 가고, 그 화면에서 손으로 다듬을 수도 있다(`sourceImageUri`가 널이 아니라 `isBorderOnlyEdit`가
  > 거짓이다). 같은 PR이 실패 문구를 한 벌로 합쳐 ③에 붙어 있던 "디자인에는 버튼이 없다"는 단서도
  > 걷었다 → [c103-error-use-original 스펙](../superpowers/specs/archive/2026-09-05-c103-error-use-original.md).
  > ⚠️ **위키 [[토핑]]과의 간격은 남는다** — 이 경로가 만드는 토핑은 배경이 남은 불투명한 사각형이라
  > "누끼 사진 객체"라는 정의에 들어맞지 않는다. 캔버스에서 다르게 취급할지는 정하지 않았다.
  > 🔁 **그 버튼이 「직접 편집」이 됐다(2026-09-10, PR #487 `95b7fc4d5`)**. ④는 닫힌 채 경로만 달라졌다. 원본을
  > 한 번 저장한 뒤 사진 전체가 남은 마스크로 C-104를 열고, 편집 결과를 받아 `C-103-confirm`으로 간다.
  > 위 ⚠️의 간격도 그대로다. C-104의 완료 판정은 `SubjectCoverage` 하한 하나라서, 배경을 지우지 않고 완료하면
  > 여전히 배경이 남은 사각형이 토핑이 된다.
- **해소 메모**: ①②는 위키 [[누끼-따기]] "버전별 보강" 절과 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md) 화면 ID 대응 표를 함께 정리한다. ③④는 [ADR-0012](../adr/0012-mlkit-subject-segmentation.md) As-built 절의 실패 처리 서술과 정합을 본다.

### [2026-08-15] C-105 테두리 색 팔레트 9종이 정책 소스 없이 코드로 확정됐다

- **ID**: OQ-P-154
- **출처**: `feature/segmentation/impl` `editor/ToppingBorderColors.kt#TOPPING_BORDER_COLORS`(PR #221 develop 머지) — 9종 중 4종만 `YGAtomicColors`(투명·`Gray.White`·`Gray.Black`·`Cherry.Cherry200`)이고 나머지 5종은 `Color(0xFF……)` 리터럴이다. 위키에 C-105 테두리 색 정책 문서가 없어 대조 대상 자체가 없다. 맨 앞 투명 칩은 색이 아니라 "두르지 않음"이라는 **의미**를 갖는데 그 규약도 코드에만 있다.
- **항목**: ① 팔레트가 Figma에 있는지 확인하고 있으면 원자 색 토큰으로 승격할지, ② 없으면 디자인에 요청할지, ③ "투명 = 두르지 않음"을 정책으로 못박을지 — 되돌리기 스택에는 이력이 남고 두른 겹만 비는 동작까지 포함해서.
- **상태**: 미해결 (정책 소스 부재 — A-002 온보딩 문구·확인 모달 문구와 같은 유형)
- **해소 메모**: 색이 토큰으로 올라가면 [design-system](../architecture/design-system.md) 원자 색 목록에 반영하고, 정책이 생기면 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md) 정책 대조 표의 "정책 문서 없음" 행을 채운다.

### [2026-08-15] 편집 로직이 유닛 테스트 없이 머지됐다 — 모듈에 테스트 플러그인이 없다

- **ID**: OQ-P-155
- **출처**: `feature/segmentation/impl/build.gradle.kts`(`parfait.test.unit` 미적용) vs 같은 PR이 추가한 `core/util/jvm` 테스트 2파일(`ArgbExtensionTest`·`FloatArrayExtensionTest`)(PR #221 develop 머지) — 픽셀 유틸은 `core:util:jvm`으로 승격되며 테스트가 붙었는데, **정작 판단이 몰려 있는 곳**(마스크 합성 `buildCutoutBitmap`의 3단계 알파 규칙, 거리장 밴드 환산 `toBorderBands`, `UndoRedoStack`의 undo/redo/`replaceLast`, `BitmapViewMapping.fitCenter`·`clampPan`)은 검증이 없다. `UndoRedoStack`·`BitmapViewMapping`은 Android 타입에 의존하지 않아 **지금도 JVM 테스트가 가능하다**(비트맵 합성 2건만 계측 또는 Robolectric이 필요하다).
- **항목**: ① `feature/segmentation/impl`에 `parfait.test.unit`을 붙이고 순수 로직 2종부터 덮을지, ② 비트맵 합성 검증을 어디서 돌릴지(계측 / Robolectric / 안 함), ③ 편집 화면이 들고 있는 상태(그리는 도중 획·zoom/pan)가 화면 로컬이라 ViewModel 테스트로 안 잡히는 것을 감수할지.
- **상태**: 부분 해소 (**PR #309 develop 머지, 2026-08-20** — `parfait.test.unit`이 붙고 모듈이 첫
  `src/test` 소스셋을 얻었다. **다만 ①이 겨눈 순수 로직 2종은 여전히 안 덮였다** — 붙은 테스트는
  `SegmentationViewModelTest` 하나이고 `UndoRedoStack`·`BitmapViewMapping`은 그대로 검증이 없다.
  ②③은 미해결)
- **해소 메모**: 플러그인이 이미 있으므로 남은 것은 테스트를 쓰는 일뿐이다 — 판단이 몰린
  `buildCutoutBitmap`(3단계 알파 규칙)·`toBorderBands`·`UndoRedoStack`·`BitmapViewMapping`이 대상이고,
  앞의 하나만 비트맵 합성이라 계측 또는 Robolectric이 필요하다. [2026-08-09] "검증 안 된 표면" 항목의
  `MainDispatcherRule` 사용처와도 겹친다. 플러그인 적용 목록의 SoT는 코드이고
  ([unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md) "적용 대상" 표는
  스펙 시점 기준이다), 그 스펙에는 이번 적용 사례만 각주로 남겼다.

### [2026-08-15] 세그멘테이션 화면이 raw `Bitmap`을 UiState에 담고 死코드 2건이 함께 머지됐다

- **ID**: OQ-P-156
- **출처**: `feature/segmentation/impl` `viewmodel/SegmentationViewModel.kt#SegmentationState.originBitmap` · `viewmodel/ToppingEditViewModel.kt#ToppingEditState`(`originBitmap`·`segmentationBitmap`) · `screen/BitmapUtils.kt#mapViewToBitmap`·`#mapBitmapToViewFloat`(둘 다 참조 0건)(PR #221 develop 머지) — ViewModel이 `(wrapper as? AndroidBitmap)?.getRawData()`로 `BitmapWrapper` 추상을 벗겨 `android.graphics.Bitmap`을 상태에 직접 담는다. [ADR-0011](../adr/0011-cross-module-bitmap-abstraction.md)이 규정하는 것은 domain 경계뿐이라 규약 위반은 아니지만 다운캐스트가 data 레이어 밖으로 나온 첫 사례이고, **원본 해상도 비트맵이 상태 수명 동안 상주**한다(`ToppingEditState`는 2장). 스냅샷 상태에 담긴 비트맵이라 Compose stability 관점에서도 unstable 파라미터다.
- **항목**: ① 비트맵을 상태가 아니라 `remember`/`produceState`로 화면이 들지, ② `BitmapWrapper`에 필요한 연산을 정의해 다운캐스트를 data로 되돌릴지([2026-07-12] BitmapWrapper stub 항목과 같은 결정), ③ 死코드 2건(`mapViewToBitmap`·`mapBitmapToViewFloat`)을 걷어낼지 — 좌표 변환 4종 중 2종만 쓰인다.
- **상태**: 부분 해소 (③ 해소, 2026-09-20 PR #514 / ①② 잔존)
  > ✅ **③이 닫혔다(2026-09-20, PR #514)** — `BitmapUtils.kt`의 `mapViewToBitmap`·`mapBitmapToViewFloat`
  > 둘이 삭제됐다. 함께 `BitmapViewMapping.fitCenter`의 `IntSize` 오버로드도 참조 0건이라 걷혀,
  > 좌표 변환은 `Size`를 받는 `fitCenter`와 `mapViewToBitmapFloat` 둘만 남았다.
  > ⚠️ **①②는 그대로다** — ViewModel이 여전히 `(wrapper as? AndroidBitmap)?.getRawData()`로 추상을
  > 벗겨 raw `Bitmap`을 UiState에 담는다.
- **해소 메모**: ②가 정해지면 [ADR-0011](../adr/0011-cross-module-bitmap-abstraction.md) As-built 절과 [module-structure](../architecture/module-structure.md) 규칙 서술을 함께 손본다. ①은 실기기에서 큰 사진으로 OOM 여부를 본 뒤 판단한다.

### [2026-08-15] 그룹 Repository 경계와 에러 코드 9종이 소비처 없이 먼저 머지됐다

- **ID**: OQ-P-157
- **출처**: `domain/repository/group/ParfaitGroupRepository.kt` · `data/repository/group/ParfaitGroupRepositoryImpl.kt` · `domain/model/error/ServerErrorCode.kt`(`ParfaitGroup` 8종·`Common` 1종)(PR #241 develop 머지) — 로그인 라운드의 마지막 커밋이 A-002와 무관한 그룹 경계를 함께 넣었다. 이유는 커밋 메시지에 있다: 화면 브랜치 셋(#233·#239·#240)이 각자 같은 4파일을 만들고 있어 두 번째가 머지되는 순간 충돌한다. 결과적으로 **UseCase·ViewModel 없이 Repository·DI·테스트만 develop에 있다** — [data-layer](../architecture/data-layer.md) "원격 Repository 인벤토리".
- **항목**: ① 인터페이스가 DataSource 8개 중 5개만 올린 상태로 남는 기간이 얼마나 되는지 — 그룹 상세·탈퇴·신고는 "화면이 요구할 때" 올리기로 했는데 그 시점의 판단 주체가 정해져 있지 않다(브랜치마다 각자 추가하면 다시 같은 충돌이 난다). ② `ServerErrorCode.ParfaitGroup` 8종·`Common` 1종은 **분기에 쓰는 코드만 둔다**는 자기 KDoc 규칙을 지금은 어기고 있다 — 소비처가 생길 때까지 방치되면 계약 변경을 아무도 못 잡는다. ③ 이 선반영 방식(충돌 회피용 경계 선행)을 관례로 삼을지, 이번만의 예외로 둘지.
- **상태**: 부분 해소 (2026-08-15 같은 날 PR #243·#244·#248이 **5 메서드·에러 코드 9종을 전부 소비**했다. ③ 관례화 여부만 잔존)
- **해소 메모**: ①②는 자연 해소됐다 — 선반영이 하루를 못 넘겼고 `ServerErrorCode`는 "분기에 쓰는 코드만 둔다"는 자기 규칙으로 돌아왔다. 인벤토리 표의 "소비: 없음"은 지웠다([data-layer](../architecture/data-layer.md)). ③은 남는다 — **충돌 회피용 경계 선행을 관례로 삼을지**는 이번이 성공 사례가 됐다는 것만 확인됐고, 그렇게 정하면 [data-layer](../architecture/data-layer.md) "신규 데이터 추가 체크리스트"에 적는다. 반대로 이번뿐이라면 다음번에 같은 상황(화면 브랜치 셋이 같은 파일을 만드는)이 오면 다시 판단해야 한다.

### [2026-08-15] 서버가 다시 5 엔드포인트 앞섰다 — 캔버스 조회·토핑 삭제/테두리·탈퇴가 통째로 공백

- **ID**: OQ-P-158
- **출처**: 서버 `36ecd1c` 기준 26 엔드포인트(+테스트 전용 1). TJYG-Android develop의 원격 표면은 PR #230 이후 그대로 20이다(`ParfaitService`는 `@GET .../parfaits/year` 하나, `MemberService`는 `@GET`·`@PATCH` 둘, `ParfaitImageService`는 `@POST`·`@PATCH` 둘) → [api/README.md](../api/README.md), [api/conventions.md](../api/conventions.md) "Android 불일치".
- **항목**: ① 어느 것부터 붙일지 — **`GET .../parfaits/today`가 C-001 캔버스 결선의 선행**이라 우선순위가 가장 높다(배치 목록 부재라는 오래된 장애물이 이것으로 사라졌다, OQ-P-119). ② **회원 탈퇴는 응답이 본문 없는 204**라 앱 `ApiCaller`의 envelope 전제와 맞지 않는다 — `Response<Unit>` 계열 진입점을 새로 둘지, 서버에 envelope 통일을 요청할지 정해야 표면을 붙일 수 있다(OQ-P-162). ③ 토핑 삭제·테두리 수정은 C-105·C-106 편집 플로우와 짝이라 그 화면 결선 라운드에 함께 간다.
- **상태**: **해소됨** (2026-08-15, PR #250 — 다섯 표면이 한 라운드에 들어왔다)
- **해소 메모**: 선작성 스펙·플랜 한 쌍이 그대로 이행됐다
  ([spec](../superpowers/specs/archive/2026-08-15-parfait-canvas-topping-member-api-service-layer.md)) — 설계와 갈린 곳
  0건이고 **DI 바인딩은 한 줄도 늘지 않았다**(세 Service·세 DataSource가 이미 등록돼 있었다).
  ①의 우선순위 판단(`today`가 C-001 결선의 선행)은 유효한 채로 다음 라운드에 넘어간다 — 표면은 생겼고
  Repository·UseCase·화면이 남았다(OQ-P-094 ③). ②의 204 문제는 **새 진입점 없이** 기존
  `safeApiCallNoContent`로 풀렸고, 짝인 토핑 삭제가 `safeApiCallWithoutData`를 살려 OQ-P-132까지 닫았다.
  ③(토핑 삭제·테두리는 C-105·C-106 결선 라운드와 함께)은 표면만 앞서 간 상태다.
  [api/README.md](../api/README.md) 도메인 표·개수, 세 도메인 문서의 "Android 매핑",
  [api/conventions.md](../api/conventions.md) 개수 문단, [data-layer](../architecture/data-layer.md)
  "네트워킹" 반영 범위를 함께 갱신했다.

### [2026-08-15] 테스트 전용 캔버스 회전 엔드포인트가 인증 없이 전 그룹 캔버스를 마감한다

- **ID**: OQ-P-159
- **출처**: 서버 `ParfaitCanvasRotationTestController`(`POST /api/v1/test/parfait-canvas/rotate`) + `SecurityConfig.WHITELIST_PATHS` — 화이트리스트에 올라 **토큰 없이 호출되고**, 대상이 특정 그룹이 아니라 `ParfaitGroupQueryPort.findAllIds()`가 주는 **전체 그룹**이다. 호출 한 번이 모든 그룹의 `ACTIVE` 캔버스를 즉시 `CLOSED`/`EMPTY`로 바꾸고 다음 날 캔버스를 만든다. 컨트롤러와 화이트리스트 양쪽에 "프로덕션 오픈 전 함께 제거" TODO가 달려 있다 → [api/parfait.md](../api/parfait.md), [api/conventions.md](../api/conventions.md) "인증".
- **항목**: ① 제거 시점을 서버팀과 확인한다 — dev 환경에만 열려 있는지, prod 배포에도 함께 나가는지 코드로는 갈리지 않는다(프로파일 조건이 없다). ② 앱은 이 경로를 쓰지 않는다(`해당 없음`) — QA가 03시를 기다리지 않고 캔버스 전환을 재현하려면 이 엔드포인트가 유일한 수단이라, 제거되면 **QA 재현 수단도 같이 사라진다.** 대체 수단(관리자 인증 + 그룹 단위 회전)이 필요한지 판단한다.
- **상태**: 미해결 (서버 소관 — 프로덕션 오픈 전 반드시 닫혀야 함)
- **해소 메모**: 제거되면 [api/parfait.md](../api/parfait.md)의 해당 절·엔드포인트 표 행과 [api/conventions.md](../api/conventions.md) 화이트리스트 항목, [api/README.md](../api/README.md)의 "+테스트 전용 1" 표기를 함께 지운다.

### [2026-08-15] `GET .../parfaits/today`가 조회인데 캔버스를 만든다

- **ID**: OQ-P-160
- **출처**: 서버 `GetTodayParfaitService.get` → `EnsureActiveCanvasUseCase.ensure`(`ParfaitService.ensure`, `@Transactional`) — 오늘 날짜 파르페가 없으면 `Parfait.createToday`로 **새로 만들어 저장한다.** GET 한 번이 행을 만들고, 그 행은 연도 목록·과거 목록에도 즉시 나타난다. 또 `ensure`는 상태가 아니라 **날짜로** 찾으므로(`findByGroupIdAndDate`), 오늘 날짜 캔버스가 이미 `CLOSED`·`EMPTY`면 **마감된 캔버스를 "오늘"로 돌려준다** → [api/parfait.md](../api/parfait.md).
- **항목**: ① 부작용 있는 GET이 의도인지 — 회전 배치가 이미 다음 날 캔버스를 만들어 두므로 이 생성은 **배치가 돌지 않았을 때의 폴백**으로 읽힌다. 앱이 캔버스 목록을 미리 당겨오는 화면(캘린더)에서 GET을 부르면 **빈 캔버스 행이 양산**될 수 있다. ② 마감된 오늘 캔버스를 받았을 때 앱이 무엇을 보여줄지 — **서버는 막지 않는다.** 토핑 배치·수정·테두리·삭제 네 엔드포인트 어디도 `parfait.status`를 보지 않아(`PlaceParfaitImageService`는 `existsByIdAndGroupId`만 본다) **마감된 캔버스에도 토핑이 올라간다.** 마감 이후 편집을 막는 것이 지금은 앱 책임이라는 뜻이고, 그 규칙이 어디에도 적혀 있지 않다 → [api/parfait-image.md](../api/parfait-image.md). ③ 03시 경계에서 앱이 열려 있으면 같은 화면이 어제 캔버스를 들고 있게 된다 — 재조회 트리거를 앱이 어떻게 잡을지.
- **상태**: 부분 해소 (**② 편집 무가드는 서버가 닫았다 — 2026-08-20 서버 `efbf98f`**, ①③ 잔존)
  > ✅ **②에 서버가 답했다(2026-08-20, `fix: 마감된 파르페에 대한 편집 요청 거부`)** — 토핑 배치·수정·
  > 테두리·삭제 네 엔드포인트가 대상 파르페의 `status != ACTIVE`면 **409 `PARFAIT_ALREADY_CLOSED`**로
  > 거부한다(배경 변경도 같이 — OQ-P-189). 배치는 존재 확인을 `existsByIdAndGroupId`에서
  > `findByIdAndGroupId`로 바꿔 같은 조회로 상태를 읽고, 나머지 셋은 파르페 조회 자체가 새로 생겼다
  > → [api/parfait-image.md](../api/parfait-image.md). **"마감 이후 편집을 막는 것이 앱 책임"이라는
  > 전제가 사라졌다** — 앱 방어(지난 캔버스의 편집 진입을 치우는 것)는 남아도 되지만 이제 유일한
  > 방어가 아니고, 그렇게 적어 둔 주석 일곱 곳은 거짓이 됐다(OQ-P-244).
  > **①③은 그대로다** — 부작용 있는 GET도, 03시 경계에서 화면이 어제 캔버스를 든 채 남는 것도
  > 이 delta가 건드리지 않았다.
  > 📌 **앱 표면이 생겼는데 방어는 KDoc뿐이다(2026-08-15, PR #250)** — `ParfaitRemoteDataSource.getTodayCanvas`가
  > 들어오면서 경고("반복 호출하면 빈 캔버스가 양산된다", "잠그는 것은 화면 책임")가 Service·DataSource
  > 양쪽 KDoc에 적혔지만 **호출을 억제하거나 `status`로 편집을 잠그는 코드는 한 줄도 없다.** 소비처가
  > 0건이라 지금은 영향이 없고, ②③은 C-001 결선 라운드에서 처음 실물로 결정된다 —
  > `CanvasStatus`(`ACTIVE`·`CLOSED`·`EMPTY`·`UNKNOWN`)라는 판단 재료는 domain에 준비됐다.
- **해소 메모**: 확인 후 [api/parfait.md](../api/parfait.md) `today` 절의 ⚠️ 둘과 "미결"을 갱신한다. ②는 위키 [[캔버스-마감-스케줄]] 정책과도 대조가 필요하다.

### [2026-08-15] 과거 파르페 목록의 `thumbnailUrl`이 항상 null이고 페이지네이션이 없다

- **ID**: OQ-P-161
- **출처**: 서버 `GetPastParfaitsService.getPastParfaits` — `PastParfaitResult(thumbnailUrl = null, …)`을 리터럴로 넣는다. 필드만 있고 채우는 코드가 없다. 같은 응답의 `imageCount`는 집계로 실제 값이 온다. 범위 기본값은 `to`=오늘·`from`=`to - 30일`이고 **상한도 페이지 파라미터도 없다** → [api/parfait.md](../api/parfait.md).
- **항목**: ① 썸네일을 서버가 채울지(그룹 목록 API는 이미 `recentImageUrl`을 네이티브 쿼리로 뽑고 있어 같은 방식이 가능하다), 아니면 앱이 `today`/배치 조회로 대신할지. ② 범위 상한 — 앱이 캘린더에서 1년치를 요청하면 그대로 전량이 내려온다. ③ 응답 0건 표현이 `today`(널)와 목록(빈 배열)에서 서로 다르다 — 소비 측 분기 규칙을 한쪽으로 통일할지.
- **상태**: 미해결 (①②는 서버 소관, ③은 계약 관측 사실)
- **해소 메모**: 채워지면 [api/parfait.md](../api/parfait.md) 과거 목록 절의 ⚠️와 "미결"을 갱신한다.

### [2026-08-15] 탈퇴 응답만 envelope가 없다 — 성공 표현이 서버 안에서 셋으로 갈렸다

- **ID**: OQ-P-162
- **출처**: 서버 `MemberController.withdraw`(`@ResponseStatus(NO_CONTENT)` + `Unit` → **204, 본문 없음**) vs `DeleteParfaitImageController.delete`(**200 + `data: null`**) vs 나머지 전부(`ApiResponse` envelope). 같은 delta에 들어온 두 DELETE가 서로 다르다 → [api/member.md](../api/member.md), [api/parfait-image.md](../api/parfait-image.md), [api/conventions.md](../api/conventions.md).
- **항목**: ① 서버가 통일할지(`logout`도 같은 204라 선례가 둘이다) — 통일한다면 어느 쪽으로인지. ② 앱 `ApiCaller`는 envelope 파싱을 전제하므로 **204를 받는 진입점이 따로 필요하다**(`safeApiCallWithoutData`가 死코드로 남아 있다는 지적이 OQ-P-132에 있다 — 이 엔드포인트가 그 자리의 첫 소비처가 될 수 있다). ③ 탈퇴는 **회원이 없어도 204**(멱등)이고 도메인 에러가 없다 — 앱이 "이미 탈퇴됨"을 구분할 방법이 없다는 뜻인데, 구분이 필요한지 판단한다.
- **상태**: 부분 해소 (②·③은 닫혔다 — ①만 서버팀 몫으로 남는다)
  > ✅ **②가 새 진입점 없이 풀렸다(2026-08-15, PR #250)** — `MemberService.deleteUsersMe`가 `ApiResponse`가
  > 아니라 `Unit`을 반환하고 DataSource가 기존 `safeApiCallNoContent`로 호출한다(`logout` 선례). 짝인
  > 토핑 삭제는 200 + `data: null`이라 `safeApiCallWithoutData`로 갈렸고, 그 진입점이 死코드에서 벗어났다
  > (OQ-P-132 해소). **서버 안에서 성공 표현이 셋으로 갈린 것 자체(①)는 그대로**이고, 앱은 그 갈림을
  > 진입점 두 개로 그대로 받아냈다.
  > 📌 **③은 소비처가 없어 아직 물음이 서지 않는다** — 탈퇴를 부르는 화면이 0건이라 "이미 탈퇴됨"을
  > 구분할 필요가 생기는 자리가 없다.
  > 📌 **소비처가 생겼는데 ③은 물음이 서지 않은 채로 닫혔다(2026-08-19, PR #306)** — S-001이 탈퇴를
  > 부르지만 **204를 성공 하나로 받는다**. 이미 탈퇴된 계정이어도 화면이 할 일이 같기 때문이다
  > (토큰·계정 정보를 지우고 로그인으로 보낸다). 구분이 필요해지는 자리는 "탈퇴 완료" 안내처럼
  > **결과를 문구로 말하는 화면**이 생길 때이고 지금은 없다. ①(서버 안에서 성공 표현이 셋으로 갈린 것)만
  > 서버팀 몫으로 남는다.
- **해소 메모**: 결정 시 [api/conventions.md](../api/conventions.md) "envelope를 쓰지 않는 응답" 절과 [api/member.md](../api/member.md) 미결을 갱신한다.

### [2026-08-15] 탈퇴 회원이 남긴 토핑이 `(알수없음)`으로 캔버스에 계속 보인다

- **ID**: OQ-P-163
- **출처**: 서버 `MemberService.withdraw`가 회원 행을 하드 삭제하고 그룹 멤버십을 `ParfaitGroupMember.leave()`로 바꾼다(`groupNickname` → `GroupNickname.unknown()` = `(알수없음)`, `leftAt` 기록). 그런데 그 멤버가 배치한 `parfait_image` 행은 **삭제되지 않고**, `GET .../parfaits/today`의 `placedBy` 조회는 `findAllByIdIn`이라 **`leftAt` 필터가 없다.** 결과적으로 `images[].placedBy.groupMemberId`가 `groupMembers` 목록에 **없을 수 있고** 닉네임은 `(알수없음)`이다 → [api/parfait.md](../api/parfait.md), [api/member.md](../api/member.md). 관련: `GroupNickname.of`가 이 센티널만 검증을 건너뛰도록 특례가 붙었는데(`fix: 탈퇴 멤버 닉네임 재구성 시 GroupNickname 검증 실패 수정`) **`of`는 사용자 입력에도 쓰여** 누구나 `(알수없음)`을 자기 그룹 닉네임으로 넣을 수 있다 → [api/parfait-group.md](../api/parfait-group.md).
- **항목**: ① 탈퇴자 토핑을 남기는 것이 의도인지(협업 캔버스라 남기는 쪽이 자연스러울 수 있다) — 남긴다면 앱이 `placedBy`를 어떻게 표시할지 정책이 필요하다(위키 [[nametag-chip]]은 닉네임 첫 글자로 색을 정하는데 `(`가 첫 글자가 된다). ② `groupMembers`에 없는 `groupMemberId`를 앱이 어떻게 처리할지 — 지금 계약으로는 조인이 깨진다. ③ 사용자가 `(알수없음)`을 직접 입력하는 우회를 서버가 막을지.
- **상태**: 미해결 (①③은 서버·기획, ②는 C-001 결선 시 앱 결정)
- **해소 메모**: 정해지면 [api/parfait.md](../api/parfait.md) `today` 절과 [api/parfait-group.md](../api/parfait-group.md) 닉네임 절, 위키 쪽 표시 정책을 함께 갱신한다.

### [2026-08-15] 애플 연동 해제(revoke) 수단이 서버에서 사라졌다 — 탈퇴 API가 생긴 그 delta에서

- **ID**: OQ-P-164
- **출처**: 서버 `refactor: 애플 로그인 authorizationCode 교환 로직 제거 (#89)` — 요청 필드 `authorizationCode`·`AppleAuthorizationCodeExchangeAdapter`·`AppleClientSecretGenerator`·`Member.appleRefreshToken`(마이그레이션 `V10__drop_apple_refresh_token_from_member.sql`)이 전부 제거됐다. 커밋 메시지가 사유를 밝힌다 — 교환이 `invalid_client`로 계속 실패해 로그인 자체가 막혀 있었고, 그 토큰의 유일한 용도가 **탈퇴 시 애플 revoke**였으며 당시 탈퇴 기능이 없었다. **그런데 같은 delta의 다른 PR(#90)이 탈퇴 API를 넣었다** — `MemberService.withdraw`에 애플 호출은 없다 → [api/auth.md](../api/auth.md), [api/member.md](../api/member.md).
- **항목**: ① App Store 심사 요건(애플 로그인 제공 앱은 계정 삭제 시 연동 해제를 요구받는다)에 걸리는지 확인한다 — 커밋 메시지는 "필수 요건은 아니다"라고 적었으나 근거가 함께 있지 않다. ② 필요하다면 `client_secret` 생성 문제(`invalid_client`)를 먼저 풀어야 한다 — 되돌리는 것이 아니라 재구현이다. ③ **Android는 애플 로그인을 쓰지 않기로 했으므로**(OQ-P-117 ②) 이 항목은 iOS가 붙는 시점의 서버 선행 조건이다.
- **상태**: 미해결 (서버·iOS 소관 — Android 영향 없음)
- **해소 메모**: 정해지면 [api/auth.md](../api/auth.md) 애플 로그인 절과 [api/member.md](../api/member.md) 탈퇴 절의 ⚠️를 갱신한다.

### [2026-08-15] 그룹 목록 업로드 시각을 오프셋 필수 파서로 읽는데 서버는 오프셋을 안 싣는다

- **ID**: OQ-P-165
- **출처**: `data/source/group/mapper/VOMapper.kt#toMyParfaitGroupVO`(PR #248 develop 머지) — `recentImageUploadedAt`을 `kotlin.time.Instant::parse`로 읽고 주석이 "오프셋(`Z`)째로 읽는다"고 적는다. `MyParfaitGroupVOMapperTest`도 `…Z`·`+09:00` 문자열만 검증한다. 그런데 서버(`TEAMYG-SERVER` `main` `36ecd1c`)의 `MyParfaitGroupResponse.recentImageUploadedAt`은 `LocalDateTime`이고 `ParfaitGroupControllerTest`가 응답 문자열 `2026-08-01T12:00:00`(오프셋 없음)을 직접 검증한다 → [api/parfait-group.md](../api/parfait-group.md) 직렬화 포맷. **오프셋 없는 문자열은 `Instant.parse`가 받지 못하므로, 최근 이미지가 있는 그룹이 하나라도 있으면 매퍼가 던지고 G-001 목록 조회가 통째로 실패한다**(예외는 `ApiCaller`의 transform 가드에 잡혀 `AppError.Unexpected` → 에러 화면).
- **항목**: ① 어느 쪽을 고칠지 — 서버가 오프셋 포함 포맷(`Instant`/`OffsetDateTime`)으로 바꾸거나, 앱이 `LocalDateTime` + 고정 타임존(Asia/Seoul)으로 읽는다. 앱 변경 의도("벽시계가 아니라 절대 시점")는 타당하므로 서버 쪽이 자연스럽다. ② 개발 서버 실응답이 실제로 어떤 포맷인지 먼저 확인한다 — 테스트 값이 실측처럼 보이지만(`2026-08-15T05:17:10.240Z`) `main` 코드로는 설명되지 않는다. ③ 고친 뒤 `MyParfaitGroupVOMapperTest`의 케이스를 DataSource 테스트로 옮긴다(OQ-P-168).
- **상태**: 해소됨 (앱이 `LocalDateTime` + 고정 KST로 읽는다 — PR #310 develop 머지 2026-08-20)
- **해소 메모**: 확정되면 [api/parfait-group.md](../api/parfait-group.md) 엔드포인트 표의 Android 열(`⚠️불일치`)과 각주, [api/conventions.md](../api/conventions.md) "Android 불일치" 표, [data-layer](../architecture/data-layer.md) 시각 노트를 함께 정리한다.
  > ⚠️ **악화(2026-08-19, 서버 `57529ec`)** — 서버가 이 필드를 `COALESCE`로 **비널화**했다(토핑 0건 그룹은
  > 그룹 생성 시각). 앱 매퍼는 `recentImageUploadedAt?.let(Instant::parse)`라 **값이 널일 때만** 파싱을
  > 건너뛰었는데 그 우회로가 사라졌다 — 즉 이제 "최근 이미지가 있는 그룹이 있으면"이 아니라
  > **"그룹이 하나라도 있으면"** G-001 목록이 통째로 실패한다. 토핑을 한 번도 안 올린 계정이 우연히
  > 살아 있던 마지막 안전지대가 없어졌다. **우선순위를 올려야 한다.** 필드가 두 뜻을 겸하게 된 것은
  > 별개 항목(OQ-P-235)이다.
  > ✅ **해소(2026-08-20, PR #310 develop 머지)** — ①의 두 선택지 중 **앱 쪽**을 골랐다.
  > 매퍼가 `LocalDateTime::parse` → `toInstant(PARFAIT_TIME_ZONE)`로 읽어 벽시계에 KST를 붙인다.
  > "서버 쪽이 자연스럽다"고 적었던 판단을 뒤집은 근거는 **계약 사실**이다 — 서버 DB 커넥션 세 환경이
  > `serverTimezone=Asia/Seoul`이라 그 벽시계의 시간대가 KST로 확정돼 있고, 서버 포맷 변경을 기다리는
  > 동안 G-001 목록이 계속 비어 있을 이유가 없었다. VO 타입(`Instant`)과 화면 계산은 그대로다.
  > ②(개발 서버 실응답 포맷)는 **끝까지 확인하지 못했다** — 실서버 요청 검증은 여전히 0건이고, 판단
  > 근거는 서버 코드·컨트롤러 테스트 대조뿐이다. ③은 OQ-P-168에서 함께 닫혔다.
  > 남은 위험은 "예상 밖 모양이 오면 목록 전체가 빈다"는 실패 단위 문제이고 그것이 OQ-P-237이다.

### [2026-08-15] 그룹 참여가 두 요청으로 갈렸다 — 중간에 이탈하면 닉네임 없는 참여가 남는다

- **ID**: OQ-P-166
- **출처**: `GroupInviteCodeViewModel`(`JoinGroupUseCase` → `NavigateToNext(groupId)`)·`GroupNickNameViewModel`(`ChangeGroupNicknameUseCase`)(PR #244 develop 머지) — A-004 확인 모달이 `POST /api/parfait-groups/join`으로 **합류를 끝내고**, S-102는 그 `groupId`로 닉네임을 PATCH 한다. 그래서 **S-102에서 뒤로 가거나 앱을 닫아도 그룹에는 이미 들어가 있고** 닉네임은 서버 초기값으로 남는다. 화면에 건너뛰기·취소 개념이 없고 재진입 경로는 S-101 그룹 설정뿐이다.
- **항목**: ① 이 상태를 정상으로 볼지 — 정상이면 S-102를 "선택 입력"으로 문구·버튼을 바꾸는 것이 정직하다. ② 아니면 참여를 닉네임 확정까지 미루거나(A-004 모달을 이동으로 되돌리고 S-102가 join+PATCH를 순서대로), 이탈 시 되돌릴 경로를 준다. ③ 서버 초기 닉네임 규칙(위키 [[닉네임-자동-생성]])이 그룹 닉네임에도 적용되는지 확인이 선행이다.
- **상태**: 해소됨 (2026-08-16, PR #261 — 항목 ②의 앞쪽 안을 코드가 골랐다)
- **해소 메모**: **A-004 확인 모달이 S-102로 내려가고, 그 모달의 "참여하기"가 `POST join` → `PATCH nickname`을 순서대로 부른다.** A-004는 미리보기(`GET join-preview`)까지만 하고 `NavKeyGroupNickName(inviteCode, groupName)`으로 넘기므로, 닉네임 화면에서 이탈하면 참여 자체가 일어나지 않는다. 화면 인자도 참여 결과(`groupId`)에서 참여 재료(초대코드·그룹명)로 바뀌었다. 두 스펙([a004](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md)·[s102](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md))과 [navigation-flow](../architecture/navigation-flow.md)·[api/parfait-group](../api/parfait-group.md)에 반영했다. **남은 틈은 반대 방향**이다 — 참여는 됐는데 닉네임 PATCH만 실패하면 표시 없이 전역 닉네임으로 들어간다(코드 `TODO`, 실패 표현은 OQ-P-167). 항목 ③(서버 초기 그룹 닉네임 규칙)은 이 항목과 별개로 확인된 바 없다.

### [2026-08-15] 서버 실패를 화면이 표현하는 방식이 넷으로 갈렸다

- **ID**: OQ-P-167
- **출처**: 2026-08-15 결선 라운드 4건 — ① **입력 자리 인라인 한 줄**(A-004 `InviteCodeError`·S-102 `GroupNickNameError`), ② **전면 에러 화면**(G-001 `GroupListErrorScreen`, 성공하던 목록도 통째로 대체), ③ **목록 자리 임시 문구 + "다시 시도"**(온보딩 약관, 코드에 `TODO(공통 에러화면)`), ④ **표현 없음 — 로그만**(A-005 그룹 생성, 실패 토스트가 같은 PR에서 "문구 정책이 없다"는 이유로 걷혔다 / 약관 화면의 가입 실패, 각 갈래에 `TODO(에러 UX 미정)`).
- **항목**: ① 공통 에러화면·공통 에러 표시 규약을 세울지(약관 화면 코드가 그 존재를 전제하고 있다). ② 실패 문구를 누가 확정할지 — 지금은 A-004·S-102만 `strings.xml`에 문구가 있고 나머지는 문구 자체가 없다. ③ 재시도 수단의 최소선(G-001은 당김, 약관은 텍스트 탭, A-005는 없음).
- **상태**: 미해결 (**출처 네 형태 중 ③④는 닫혔다** — 남은 것은 ②의 문구 복제와 화면별 재시도 최소선)
  > 📌 **부분 진전(2026-08-16, develop 머지 PR #267 `955c4636`)** — ①에 답이 생겼다.
  > [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)이 **"알리고 끝나는 실패는
  > 공통 토스트(`YGToastType.Fail`)"**로 확정하고 `YGScaffoldV2`가 그 자리를 제공한다. 다만 **차단성 에러
  > (재시도 동선이 필요한 실패)는 여전히 화면 소관**이라 위 ②(전면 에러 화면)·③은 갈래로 남는다.
  > ②(문구를 누가 확정하나)도 절반만 답했다 — 계약이 `String`이라 **문구는 화면 소유**이고,
  > A-002 로그인이 `LoginError` 4갈래로 첫 사례를 만들었다(`login_error_*`). 화면 고유 문구가 없는
  > 실패(`AppError.Unexpected` 등)를 위한 **공통 매핑은 여전히 없다** — 화면 수만큼 "알 수 없는 오류"가
  > 복제될 자리다. ③(재시도 최소선)은 손대지 않았다.
  > **현황(2026-08-16 develop 기준, 코드 대조 확인)**: 이관 3화면(A-002 로그인 · S-003 앱 설정 ·
  > S-002 계정 정보), V1 잔여 8파일·호출 22곳(camera·gallery·canvas·groups enter/list/setting·intro·
  > segmentation). 잔여 이관과 V1 삭제 시점은 OQ-P-204.
  > 📌 **④에 사례가 둘 더 늘었다(2026-08-17, PR #268)** — C-001의 **오늘 캔버스 조회 실패**와 **날짜별
  > 캔버스 조회 실패**가 둘 다 로그만 남긴다. 코드가 적는 근거는 "배경과 토핑이 안 그려질 뿐 토핑을
  > 올리는 것은 그대로 할 수 있어 화면을 막을 이유가 없다"인데, **그 '올리는 것'이 아직 없다**(배치
  > 경로 미결선, OQ-P-209). 결과적으로 사용자에게는 **빈 캔버스와 조회 실패가 같은 화면**이다.
  > 이 화면은 `YGScaffoldV2` 이관 3화면에 들어 있지 않다(OQ-P-204).
  > 📌 **④(표현 없음 — 로그만)에 사례가 하나 늘었다(2026-08-16, PR #261)** — S-102가 참여 뒤 부르는
  > 닉네임 `PATCH`가 실패하면 **참여는 유지한 채 로그만 남기고 다음 화면으로 간다**. 코드에
  > `TODO(닉네임 적용 실패 안내)`가 있고 "닉네임은 나중에 바꿀 수 있어요" 수준의 토스트 자리를 지목한다 —
  > ygscaffold-v2가 확정한 "알리고 끝나는 실패는 공통 토스트"에 정확히 해당하는 갈래다. **그 스펙은
  > 2026-08-16 머지됐으므로(PR #267) 자리는 이미 있고, S-102는 아직 이관되지 않아 `TODO`가 그대로다** —
  > 첫 소비 후보.
  > 다만 **실패 표현을 실제로 바꾼 건 로그인 하나**다 — ④(표현 없음)에서 토스트로 옮겼다. S-002 는
  > 이미 ①(입력 자리 인라인)이라 그대로 두는 것이 맞고, S-003 은 표현할 실패가 없다(로그아웃은
  > 서버 실패해도 로컬 정리 후 진행). **즉 이관과 실패 표현 통일은 별개 축이다.**
  > 📌 **①의 전제가 두 화면에서 깨졌다(2026-08-26, PR #371)** — "`YGScaffoldV2`가 그 자리를
  > 제공한다"고 적어 두었으나, 카메라·갤러리는 스캐폴드 호스트가 자기 헤더 행을 덮어 **호스트를
  > 다시 화면 안으로 가져갔다.** 이 항목이 정한 "알리고 끝나는 실패는 공통 토스트"는 그대로
  > 유효하다 — 바뀐 것은 **그 토스트가 어느 상자를 기준으로 뜨는가**이고, 그 축에는 규칙이 없다
  > → OQ-P-312. 카메라의 촬영 실패 `showError`도 이제 그 화면 안 호스트로 나간다.
  > 📌 **두 번째 화면이 토스트로 옮겼다(2026-08-17, PR #285·#287)** — S-101 그룹 설정이 `GroupSettingError`
  > 3갈래(`INVALID_NICKNAME`·`NETWORK`·`UNKNOWN`)로 A-002와 같은 형태를 만들었고, **한 화면이 ①과
  > 공통 토스트를 함께 쓰는 첫 사례**다(입력 형식 오류는 고칠 곳이 눈앞이라 인라인, 서버가 되돌린
  > 사유는 토스트). 다만 **②가 경고한 "화면마다 복제되는 알 수 없는 오류"가 실제로 늘었다** —
  > `group_setting_nickname_error_unknown`("잠시 후 다시 시도해 주세요")이 상세 조회·나가기·신고 실패까지
  > 받아, `GROUP_NOT_FOUND`(404)·`GROUP_NOT_JOINED`(403)와 일시 장애가 같은 문구다. ③(재시도 최소선)은
  > 여전히 손대지 않았다 — 이 화면의 조회 실패에는 재시도 동선이 없다.
  > 🔁 **그 세 번째 화면이 도로 없어졌다(2026-09-04, PR #440 develop 머지)** — G-001의 토스트가
  > 걷혔다. 당기는 동안 목록을 비우게 되면서 당긴 새로고침 실패가 **전면 에러 화면**으로 가고, 남은
  > 갈래(재진입 조회 실패)는 다시 ④(표현 없음)다. 그래서 ②는 아래에서 "보여 줄 것이 없을 때만 전면"으로
  > 좁혔던 것이 **"사용자가 시켰거나 보여 줄 것이 없을 때 전면"으로 도로 넓어졌고**, 이 화면이 문구
  > 갈래를 안 세는 유일한 토스트 사례였던 것도 사라졌다. ③(재시도 최소선)은 그대로 당김 하나다.
  > 📌 **세 번째 화면 + ②(전면 에러 화면)의 성격 변화(2026-08-17, PR #297)** — G-001이 토스트를 얻었는데
  > 앞의 둘과 결이 다르다: 문구 갈래를 세는 enum이 아니라 **문구 하나**이고(사유는 로그로만 갈린다),
  > 조건이 화면 상태가 아니라 **"사용자가 시켰는가"**다(당긴 새로고침만 알리고 재진입 조회 실패는 조용).
  > 이로써 ②는 "성공하던 목록을 통째로 대체한다"에서 **"보여 줄 것이 없을 때만 전면"**으로 좁혀졌다.
  > 대신 **④(로그만)의 무게가 C-001로 옮겨 갔다** — 같은 라운드가 그 화면의 조회 빈도를 재진입마다로
  > 늘렸는데 표현은 그대로다 → OQ-P-221.
  > ✅ **출처의 ③④가 닫혔다 — 둘 다 온보딩 약관이었다(2026-08-20, PR #315 develop 머지)**.
  > ④(가입 실패가 로그뿐)는 `TermAgreeError` 2갈래 + 공통 토스트로 옮겼고, ③(조회 실패가 목록 자리
  > 임시 문구)은 **공용 에러화면으로 바꾸지 않기로** 정해져 `TODO(공통 에러화면)` 둘이 근거 문장이
  > 됐다. 그래서 이 항목이 세던 네 형태 중 **한 화면이 둘을 동시에 확정한 첫 사례**이고, 가른 기준이
  > ①이 묻던 "공통 에러화면을 세울지"에 대한 답이기도 하다 — **재시도 동선이 화면 안에 있으면
  > 화면에 남기고, 없으면 토스트로 알린다.** 전면 에러 화면(G-001)은 그 위에 "보여 줄 것이 아무것도
  > 없을 때"라는 조건이 하나 더 붙은 형태로 남는다.
  > ⚠️ **②는 반대로 한 칸 더 나빠졌다** — `TermAgreeError.UNKNOWN`이 이 화면의 "알 수 없는 오류"
  > 문구를 새로 만들어, 같은 뜻의 문자열을 든 화면이 셋이 됐다(S-101·A-002·약관 동의).
  > ③(재시도 최소선)은 이 화면에 한해 답이 있다 — 조회는 "다시 시도" 탭, 가입은 화면이 남아 확인
  > 버튼이 그 자리에 있다.
  > 📌 **C-001이 공통 토스트를 쓰되 `YGScaffoldV2`의 자리는 쓰지 않는다**(C-106 결선 PR3, 2026-08-22
  > develop 머지 PR #334) — ④가 지목하던 C-001 오늘 캔버스 조회 실패가
  > 드디어 표현을 얻었다(보여 줄 캔버스가 없을 때만, G-001이 정한 "사용자가 시켰는가"의 C-001 대응물).
  > 그런데 **토스트가 뜨는 자리가 스캐폴드 최상단이 아니라 캔버스 프레임 상단**이다. 같은 화면의
  > Spotlight 작성자 토스트(C-202)가 `YGCanvas`의 `overlayContent`에 먼저 못 박혀 있어서다
  > (develop `9b112e86`, "pin the spotlight toast to the canvas frame's top edge"). 큐를 둘로 나누면
  > 같은 화면에서 토스트 자리가 갈리고 [[toast]] 공통 정책의 스택(나중 것이 위로)이 큐마다 따로 놀아,
  > **정책 하나를 화면 쪽 호스트에 넘기고 스캐폴드에는 넘기지 않는 형태**로 정했다. 스캐폴드는 이
  > 화면에서 로딩 오버레이 자리로만 남는다(PR5가 쓴다).
  > **그래서 "공통 실패는 `YGScaffoldV2`가 자리를 준다"가 전 화면 규칙이 아니게 됐다** — 화면이 자기
  > 토스트 호스트를 이미 갖고 있으면 그쪽이 이긴다. 남은 질문은 그 예외를 규약으로 적을지, 아니면
  > `YGScaffoldV2`가 호스트 위치를 슬롯으로 열어 하나로 되돌릴지다.
  > 📌 **캔버스 쪽 토스트 호스트가 필요한 사례가 둘로 늘었다 — 둘 다 배치 화면(같은 Route)이고 둘 다
  > "되감으면 안내가 죽는다"는 같은 함정에서 나왔다.** 첫 번째는 PR4(2026-08-21, 브랜치
  > `feature/#270-topping-border-contract`)의 초안 결손(`DraftMissing`) 안내 — 되감기가 토스트 호스트를
  > 같은 프레임에 폐기해 알림이 잔상으로 끝나므로 되감기를 걷고 알린 뒤 화면에 남기는 것으로 정했다.
  > 두 번째는 PR5(2026-08-21, 브랜치 `feature/#270-topping-place-wiring`) 최종 브랜치 리뷰가 잡은
  > **영구 실패(다섯 코드) 안내** — `CanvasToppingPlaceRoute`의 `toastPolicy`가
  > `rememberYGToastPolicy()`로 Route 컴포지션에 매달려 있어 `popUpTo`가 같은 프레임에 안내까지
  > 폐기하는 것을 최종 리뷰가 Critical로 잡았고, 처방은 PR4와 같다(되감기를 걷고 알린 뒤 화면에
  > 남긴다). **진짜 처방(안내를 캔버스 쪽 토스트 호스트로 보내는 것)은 두 사례 모두 미뤘다** — 자리가
  > 아직 없다. 이 화면 하나에서 같은 함정에 두 번 걸린 것이 그 자리의 필요를 뒷받침한다
  > → [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) 실패 처리 절.
  > ✅ **위 세 사례가 develop 사실이 됐다(2026-08-22, PR #334)** — 스택 넷이 한 머지로 들어왔고,
  > 그래서 **캔버스 쪽 토스트 호스트가 없다는 것도 이제 develop의 사실**이다. 남은 질문(예외를 규약으로
  > 적을지, `YGScaffoldV2`가 호스트 위치를 슬롯으로 열지)은 그대로 열려 있다.
  > 📌 **출처 ②(전면 에러 화면)에서 처음으로 하나가 빠져나왔다(2026-08-22, PR #311 develop 머지)** —
  > 세그멘테이션 실패가 `SegmentationErrorScreen`(전면)에서 **공통 토스트**로 옮겼다. 위 #315가 세운
  > 기준("재시도 동선이 화면 안에 있으면 화면에 남기고, 없으면 토스트")을 **그 기준이 만들어진 뒤 처음
  > 적용한 사례**이고, 판정이 갈린 자리도 정확히 그 지점이다 — 이 실패에는 재시도가 없으니 토스트다.
  > 다만 G-001 전면 에러에 붙어 있던 조건("보여 줄 것이 아무것도 없을 때")과 갈리는 이유가 하나 더
  > 있다: 세그멘테이션은 **실패해도 보여 줄 것이 남는다**(원본 사진). ②로 남는 것은 G-001 하나다.
  > ⚠️ **②가 경고한 문구 복제는 여기선 안 늘었다** — 문구 갈래를 세는 enum 없이 `segmentation_error_message`
  > 하나이고(실패 사유 셋이 같은 문장을 쓴다), 화면 어휘라 공통 매핑 부재와도 무관하다. 즉 이 라운드는
  > ①의 기준을 적용만 했고 ②(공통 매핑 부재)·③(재시도 최소선)은 손대지 않았다.
  > ✅ **출처 ④(표현 없음 — 로그만)가 그룹 진입 흐름에서 비었다(2026-08-27, PR #393·#394)** — 이
  > 항목이 ④의 첫 사례로 적었던 **A-005 그룹 생성**이 `GroupCreateError` 2종 + 토스트를 얻었고,
  > 나중에 ④에 더해진 **S-102의 닉네임 `PATCH` 실패**도 `NICKNAME_NOT_APPLIED`로 닫혔다
  > (위에서 "`YGScaffoldV2` 머지로 자리는 이미 있고 S-102가 첫 소비 후보"라고 적어 둔 그대로다).
  > 같은 라운드에서 S-102는 서버 실패 표현도 ①(입력 자리 인라인)에서 토스트로 옮겨,
  > 입력칸 아래는 형식 오류 전용이 됐다 — S-101이 만든 "인라인 + 토스트 병용"과 같은 모양이다.
  > **②는 오히려 한 뼘 늘었다** — `group_create_error_unknown`·`group_nickname_error_unknown`이
  > 각각 "알 수 없는 오류" 자리를 또 하나씩 만들었다(공통 매핑은 여전히 없다). ③도 그대로다 —
  > 두 화면 어디에도 재시도 동선은 없고, 사용자가 버튼을 다시 누르는 것이 유일한 경로다.
  > ④에 남은 것은 C-001의 캔버스 조회 실패 둘이다.
  > 🔁 **G-001이 얻었던 토스트를 도로 내놨다(2026-09-03, 미머지 브랜치)** — 이 항목이 "세 번째 화면"으로
  > 세던 사례가 사라지고, G-001의 실패 표현은 다시 ②(전면 에러 화면) 하나가 된다. 다만 ②에 붙어 있던
  > 조건이 바뀐다 — **"보여 줄 것이 아무것도 없을 때"에서 "당겨서 새로고침했을 때도"로 넓어진다**.
  > #315가 세운 기준("재시도 동선이 화면 안에 있으면 화면에 남긴다")과는 어긋나지 않는다. 그 화면이
  > 당겨서 새로고침을 갖고 있고, 애초에 사용자가 당긴 동작의 결과이기 때문이다.
  > **이로써 ②의 조건은 화면 상태가 아니라 "사용자가 시켰는가"로 정리된다** — #297이 토스트에 붙였던
  > 바로 그 기준이 이번에는 전면 화면 쪽으로 옮겨 갔다 → OQ-P-348.
- **해소 메모**: 정해지면 네 스펙(a005·a004·s102·intro-term-agree·g001)의 실패 절과 [design-system](../architecture/design-system.md)에 공통 규약을 적는다.

### [2026-08-15] 매퍼 단독 테스트가 규약을 어기고 다시 생겼다

- **ID**: OQ-P-168
- **출처**: `data/src/test/.../group/mapper/MyParfaitGroupVOMapperTest.kt`(PR #248 develop 머지) — `*VOMapperTest`는 만들지 않고 케이스를 DataSource 테스트로 옮긴다는 규약([unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md))에 따라 `PolicyVOMapperTest`·`ImageVOMapperTest`가 삭제돼 develop `*VOMapperTest`가 0건이었는데, 이번 라운드가 새로 하나를 넣었다. 대응 `ParfaitGroupRemoteDataSourceImplTest`는 이미 있다.
- **항목**: ① 케이스(오프셋 표기 2종·null)를 DataSource 테스트로 옮기고 파일을 지울지, ② 아니면 규약을 "판단이 있는 변환은 매퍼 테스트 허용"으로 개정할지 — 이번 건은 시각 파싱이라 판단이 있는 쪽에 가깝다(그리고 그 판단이 OQ-P-165로 틀렸을 가능성이 있다).
- **상태**: 해소됨 (①을 이행 — PR #308이 옮길 파일을 만들고 #310이 매퍼 테스트를 지웠다, develop 머지 2026-08-20)
  > ⚠️ **출처 서술 정정(2026-08-15)** — 위의 "대응 `ParfaitGroupRemoteDataSourceImplTest`는 이미 있다"는
  > **사실이 아니다.** develop의 `XxxRemoteDataSourceImplTest`는 image·member·parfait·parfaitimage·policy
  > 다섯이고 **group은 없다** — 그 도메인만 `ParfaitGroupRepositoryImplTest`로 대신하고 있다. 즉 ①(케이스를
  > DataSource 테스트로 옮기고 파일을 지운다)을 고르면 **옮길 파일부터 새로 만들어야 한다**.
  > 📌 **규약이 지켜진 대조 사례(2026-08-15, PR #250)** — 캔버스 조회 라운드는 판단이 든 변환 넷
  > (`images` null → 빈 목록 · 미지 배경 type → null · 미지 status → `UNKNOWN` · `SOLID` 불완전 → `None`)을
  > 전부 `ParfaitRemoteDataSourceImplTest`로 잠갔고 `*VOMapperTest`를 하나도 만들지 않았다. 규약 자체는
  > 작동하고, 어긋난 것은 group 도메인 하나다.
  > ✅ **해소(2026-08-20, PR #308·#310 develop 머지)** — ①(케이스를 DataSource 테스트로 옮기고 파일을
  > 지운다)을 골랐고, 없던 이행 대상 파일은 PR #308이 `ParfaitGroupRemoteDataSourceImplTest`로 신설했다.
  > 그래서 develop의 `XxxRemoteDataSourceImplTest`는 **여섯**(image·member·parfait·parfaitgroup·
  > parfaitimage·policy)이고 `*VOMapperTest`는 다시 **0건**이다.
  > **규약 개정(②)은 하지 않았다** — 오히려 이 파일이 규약을 어긴 값을 보여 줬다. "판단이 든 변환"이라며
  > 남겨 둔 그 테스트는 오프셋 붙은 입력을 **스스로 지어 넣어** OQ-P-165의 파싱 버그를 초록으로 지켜
  > 왔다(Given 주석이 매퍼 주석과 같은 허구였다). 매퍼를 단독으로 두면 입력의 현실성을 아무도 검사하지
  > 않는다는 것이 규약의 근거이고, 이 건이 그 사례다.
- **해소 메모**: 어느 쪽이든 [unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md)의 매퍼 항목 서술을 정본으로 맞춘다.

### [2026-08-15] 생성·참여 후 그룹 목록이 스스로 갱신되지 않는다

- **ID**: OQ-P-169
- **출처**: `GroupListViewModel`(조회는 `init` + `Refresh`뿐, PR #248) × `goToSingleClearTop(NavKeyGroupList)` 복귀(PR #224) — 복귀가 엔트리를 재사용하므로 ViewModel이 살아 있고 `init`이 다시 돌지 않는다. **그룹을 만들거나 참여하고 목록으로 돌아와도 새 그룹이 바로 보이지 않고, 당겨야 나타난다.** OQ-P-134 ③에서 승계된 항목이다(그때는 조회 자체가 없어 관측되지 않았다).
- **항목**: ① 복귀 시 재조회를 무엇으로 트리거할지 — `ResultEventBus` 결과 반환, 목록 화면의 `ON_RESUME` 관측, 복귀 관용구를 `clearBackStack()`+`goTo`로 바꿔 엔트리를 새로 만들기 중 하나. ②는 위키가 요구하는 "재진입 시 자동 재조회"와도 직결된다([[무한-파르페-그리드]]). ③ 백스택 리셋 관용구 선택 기준(OQ-P-136)과 같이 정해야 한다.
- **상태**: **해소됨** (2026-08-17, PR #297)
  > ✅ **①은 `ON_RESUME` 관측으로 결론났다** — `GroupListIntent.Enter`가 신설되고 Route의
  > `LifecycleResumeEffect`가 화면이 앞에 설 때마다 그것을 보낸다. VM `init`은 통째로 사라져 첫 조회도
  > 이 경로다. 복귀 관용구(`goToSingleClearTop`)는 **바꾸지 않았다** — 엔트리를 새로 만드는 쪽은 백스택
  > 정책을 재조회 사정으로 흔드는 것이고, 재조회가 필요한 진짜 이유는 복귀가 아니라 **남이 바꾸기
  > 때문**(다른 멤버가 올린 최근 사진)이라 화면이 스스로 묻는 편이 근거와 맞는다. 그래서 ③(백스택
  > 리셋 관용구 선택 기준, OQ-P-136)과도 **떨어졌다** — 그쪽은 여전히 열려 있다.
  > ②(위키 [[무한-파르페-그리드]]의 "재진입 시 자동 재조회") 요구는 충족됐다.
  > 같은 관용구를 C-001도 함께 받았다(오늘 캔버스·올해 달력 기록).
  > **딸려 온 것 둘** — 재조회가 잦아지면서 조회 실패 규칙이 "목록이 남아 있으면 화면 유지 + 당김 실패만
  > 토스트"로 뒤집혔고, 토스트 호스트 때문에 G-001 Route가 `YGScaffoldV2`로 이관됐다(OQ-P-204).
  > 남은 축은 **관용구가 규약이 아니라는 것**(OQ-P-221)이다.
- **해소 메모**: 반영처 — [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md)(신설) ·
  [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 조회·실패 절 ·
  [navigation-flow](../architecture/navigation-flow.md) `goToSingleClearTop`·"그룹 생성·참여 플로우" ·
  [state-management](../architecture/state-management.md) 재진입 재조회 규약.

### [2026-08-15] G-001 상대시간이 위키 표기와 갈린다 — 7일 이상 갈래 없음, 정렬은 서버 위임

- **ID**: OQ-P-170
- **출처**: `feature/groups/list/impl/route/GroupTimestamp.kt` + `strings.xml`(PR #248) — 갈래가 `JustNow`·`Minutes`·`Hours`·`Days`뿐이라 **7일이 지나도 "N일전"**이 계속 나간다. 위키 [[무한-파르페-그리드]] 라벨 표는 **7일 이상을 "오래 전"**으로 못박는다. 문구 표기도 띄어쓰기가 없다("방금전"·"3분전" vs 정책 "방금 전"·"N분 전"). 또 앱은 응답 순서를 그대로 그리는데(정렬 코드 없음) 서버 계약 문서에는 이 엔드포인트의 **정렬 보장 서술이 없다** — 위키는 활동순 + 동률 시 생성일시 최신순을 요구한다.
- **항목**: ① "오래 전" 갈래를 추가할지(정책대로) 아니면 정책을 일수 표기로 바꿀지. ② 문구 띄어쓰기를 어느 쪽으로 통일할지. ③ 정렬 책임을 서버로 확정하고 계약 문서에 명시할지, 앱이 정렬할지.
- **상태**: 미해결
- **해소 메모**: ③은 서버 확인이 필요하므로 [api/parfait-group.md](../api/parfait-group.md) 목록 절에 정렬 서술을 받아 적는 것이 먼저다. ①②는 [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 정책 대조 표를 갱신한다.

### [2026-08-15] 이름 유효성이 서버 집합으로 좁혀지며 자모 단독 입력이 막혔는데 정책에 근거가 없다

- **ID**: OQ-P-171
- **출처**: `domain/usecase/CheckNameValidUseCase.kt`(PR #243) — 허용 문자가 `' '`·`가..힣`·`A..Z`·`a..z`·`0..9`로 좁혀지고 `Char.isKorean()`(자모 포함)이 삭제됐다. 근거는 서버 정규식 `^[가-힣A-Za-z0-9]+(?: [가-힣A-Za-z0-9]+)*$`이고, 좁히지 않으면 앱을 통과한 이름이 서버에서만 400으로 튕긴다는 것이라 방향은 타당하다. 다만 위키 [[이름-입력-규칙]]은 허용 문자를 "한글·영문·숫자·공백"이라고만 적어 **자모 단독(`ㅋㅋ`·`ㅠㅠ`)의 허용 여부가 정책에 없다** — 사용자 체감으로는 되던 입력이 안 되게 바뀐 변경이다.
- **항목**: ① 정책에 자모 허용 여부를 명시할지(허용이면 서버 정규식부터 바꿔야 한다). ② 위키 [[이름-입력-규칙]]의 "한글" 정의를 완성형으로 못박을지 — 정책 문서 개정은 위키 쪽 소관이라 여기서는 구현 상태만 추적한다.
- **상태**: 부분 해소 — **구현 불일치는 닫혔고 정책 공백만 남았다**(2026-08-15, PR #250)
  > ✅ **앱이 서버 집합으로 다시 넓혀졌다(PR #250)** — `CheckNameValidUseCase.CheckValidCharacter`에
  > `'ㄱ'..'ㅎ'`·`'ㅏ'..'ㅣ'`가 더해져 `ㅋㅋ`·`ㅠㅠ`·`파르페ㅎㅎ`가 통과한다. KDoc이 **"서버보다 느슨하면
  > 안 되고 좁아도 안 된다"**로 양방향 기준을 명시했고, `CheckNameValidUseCaseTest`의 자모 케이스가
  > `Success` 기대로 뒤집혔다. `ServerErrorCode.ParfaitGroup.INVALID_GROUP_NAME` KDoc의 정규식도
  > 자모 포함본으로 정정됐다.
  > ⚠️ **①②(정책 명시)는 그대로 열려 있다** — 위키 [[이름-입력-규칙]]은 여전히 "한글"의 범위를 정하지
  > 않고, 지금 근거는 **서버 커밋 메시지 하나**뿐이다. 정책 개정은 위키 소관이라 여기서는 구현 상태만
  > 추적한다. [a005 스펙](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·
  > [s102 스펙](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md) 유효성 절과
  > [api/member.md](../api/member.md)·[api/parfait-group.md](../api/parfait-group.md)는 갱신했다.
  >
  > 🔁 **2026-09-11 정정 — 그룹명은 그날까지 서버가 자모를 안 받았다.** 서버 2026-08-15 커밋(`e4ff23f`)이
  > 자모를 넣은 것은 `GroupNickname`·`GlobalNickname` 둘뿐이었고, `GroupName`은 2026-09-11 `21d8bd1`
  > (PR #137)에야 같은 범위를 얻었다. 그런데 계약 문서는 그룹명도 08-15부터 받는다고 적었고, PR #250은
  > 그룹명·닉네임 공용인 `CheckNameValidUseCase`를 넓혔다. 그래서 **08-15~09-11 사이 A-005에서는 앱이 서버보다
  > 넓었다** — 자모가 든 그룹명이 앱을 통과해 서버에서만 400 `INVALID_GROUP_NAME`이 났다. 서버가 따라와
  > 앱 코드 변경 없이 구현 불일치가 완전히 닫혔고, 정책 공백(①②)은 그대로다.
- **해소 메모**: 서버가 먼저 움직였다. `fix: 그룹/전역 닉네임 자음 모음 단독 입력 허용`이 `GroupNickname`·`GlobalNickname` 정규식에 자모 범위(`ㄱ-ㅎ`·`ㅏ-ㅣ`)를 넣어 **서버는 이제 자모 단독을 받는다**(사유는 iOS 클라이언트가 통과시키던 값이 서버에서만 400이던 것). 따라서 ①의 "허용이면 서버 정규식부터 바꿔야 한다"는 이미 이뤄졌고, 지금은 **앱이 서버보다 좁다** — `CheckNameValidUseCase`가 완성형만 통과시켜 서버가 받는 입력을 앱이 먼저 막는다. 남은 결정은 앱을 서버 집합으로 다시 넓힐지와 위키 [[이름-입력-규칙]]에 자모 허용을 명시할지다. 넓히면 [a005 스펙](../superpowers/specs/archive/2026-07-29-a005-group-create.md)·[s102 스펙](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md) 유효성 절과 [api/member.md](../api/member.md)·[api/parfait-group.md](../api/parfait-group.md) 정규식 서술을 함께 맞춘다.

### [2026-08-15] 닉네임 편집을 버리는 뒤로가기 동작이 S-002와 S-102로 갈렸다

- **ID**: OQ-P-172
- **출처**: `feature/app/setting/impl` `AccountInfoScreen`·`AccountInfoViewModel`(S-002)과 `feature/groups/setting/impl` `GroupSettingScreen`(S-102) — 디자인은 두 화면 모두 편집 중 뒤로가기에 `닉네임 수정을 취소할까요?` 확인을 두는데, S-002만 이번에 확인 모달을 붙였다(`isDiscardDialogVisible`, 서버 값과 다를 때만). S-102는 `handleBack`이 `isEditing`이면 포커스만 내리고 두 번째 뒤로가기에 그대로 나가 **고치던 값이 조용히 사라진다.** 두 화면이 같은 컴포넌트(`YGModalPopup`)를 쓸 수 있는데도 동작이 다르다.
- **항목**: ① S-102도 같은 확인 모달을 붙일지(붙이면 저장값·입력 버퍼 분리가 S-102에도 필요하다 — S-102는 `myNickname`·`nicknameInput`으로 이미 갖고 있어 비용이 작다). ② `그만두기`=나가기 / `취소하기`=닫기 매핑을 두 화면이 공유할지 — 같은 모듈의 탈퇴 확인에서는 `그만두기`가 반대로 닫기를 뜻해 단어가 화면마다 다른 일을 한다.
- **상태**: 미해결 (S-002만 적용 — 2026-08-16 PR #263으로 develop 머지됐고 S-102는 그대로다)
- **해소 메모**: S-102에 붙이면 [user-info-ssot 스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md)의 「S-002 편집 세션」 절을 공용 규칙으로 올리고 이 항목을 닫는다.

### [2026-08-15] C-301 배경 편집 결과가 아무 데도 반영되지 않는다

- **ID**: OQ-P-173
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditViewModel#handleOnClickConfirm`·`CanvasBGEditRoute`(PR #231) — 확인이 `YGCanvasBackground`(`Image`/`Solid`)를 만들어 `ConfirmBackground` 이펙트에 싣지만 Route가 그 값을 쓰지 않고 `// TODO: 선택한 배경을 서버에 업로드/저장하는 연동 필요` 주석과 함께 `onBack()`만 한다. C-001은 `YGCanvas`에 `background`를 넘기지 않아 기본값 `Solid(Gray100)` 그대로다. 브랜치 안에 "이미지 선택 완료 후 메인 캔버스에 반영" 커밋이 있었다가 되돌려졌다(커밋 `10e70809`). 저장 경로가 없으니 재진입하면 기본값부터 다시 고른다.
- **항목**: ① 배경을 어디에 저장할지 — 서버 캔버스 계약에 배경 필드가 있는지부터 확인해야 한다([api/parfait-group.md](../api/parfait-group.md)·[api/README.md](../api/README.md)에 캔버스 조회 계약 자체가 아직 공백). ② 서버 전까지 C-001로 값을 되돌릴지(`ResultEventBus` 왕복 vs 공유 상태), ③ 되돌린 배경을 C-001이 `YGCanvas.background`로 그릴지.
- **상태**: **해소됨** (2026-08-22, PR #329 develop 머지 — ①이 결선됐고 ②는 만들 이유가 사라졌으며
  ③은 "C-001이 재조회로 그린다"로 정해졌다)
  > ✅ **고른 배경이 서버에 남는다** — 확인이 색이면 `#RRGGBB`로, 기기에서 고른 사진이면
  > 업로드(`UploadImageUseCase`)로 `imageId`를 얻어 `changeCanvasBackground`를 부르고, **저장이 끝난
  > 뒤에만** 화면을 넘긴다(먼저 넘기면 캔버스 메인이 저장 안 된 배경을 그린 채 서 있다가 다음 조회에서
  > 되돌아간다). 서버에 이미 있던 배경을 그대로 두고 확인만 누르면 **요청이 0건**이다 — https 주소는
  > 기기가 읽을 수 없어 다시 올릴 수도 없고 바뀐 것도 없다.
  > ③은 **되돌려 그리지 않는 쪽**으로 닫혔다: Route가 이펙트에 실린 배경을 쓰지 않고 돌아간 캔버스
  > 메인이 다시 조회한다. 저장된 배경을 **팔레트 시작점으로 읽는 것**도 같은 라운드에 들어와, 재진입
  > 시 기본값부터 다시 고르던 것이 끝났다 →
  > [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md#as-built-재정정-2026-08-22-pr-329-develop-머지).
  > 남은 것은 이 항목이 아니라 옆 항목들이다 — 화면 타입 보유(OQ-P-194 ①)·마감 409 처분(OQ-P-261)·
  > 배경 이미지 참조 카운트(OQ-P-190).
  > ✅ **앱 표면도 붙었다(2026-08-16, PR #266)** — `ParfaitRemoteDataSource.changeCanvasBackground(groupId,
  > parfaitId, background)`와 쓰기 전용 `CanvasBackgroundEdit`(`Color(hex)`/`Image(imageId)`)이 develop에
  > 있다([spec](../superpowers/specs/archive/2026-08-16-canvas-detail-background-api-service-layer.md)). ①의 남은 내용은
  > **Repository·UseCase·화면 결선**뿐이고, ②(임시로 C-001에 되돌리기)는 이제 만들 이유가 사라졌다 —
  > 정식 경로가 서버·앱 양쪽에 있다. 결선에서 함께 정해야 할 것은 **화면이 어느 배경 표현을 들지**
  > (OQ-P-194)와 **저장 성공인데 그릴 수 없는 응답을 어떻게 다룰지**(OQ-P-193)다.
  > ⚠️ 표면이 붙은 지금도 **C-301은 여전히 고른 값을 버린다** — 화면과 표면이 서로를 모르는 상태다.
  > ✅ **전제 반전(2026-08-16, 서버 `22717fe`)** — **배경 저장 API가 생겼다**:
  > `PATCH /api/v1/groups/{groupId}/parfaits/{parfaitId}/background`(PR #103, `type` = `COLOR`(HEX) 또는
  > `IMAGE`(업로드 확인 완료 `imageId`)). 아래 2026-08-15 메모의 "쓰는 API가 서버 어디에도 없다"는
  > **더 이상 사실이 아니다.** ①의 답이 "서버에 요청한다"에서 **"앱이 연동한다"로 바뀌었고**,
  > ②(서버 전까지 C-001로 값을 되돌릴지)는 **유일한 선택지가 아니라 임시 수단으로 되돌아갔다** —
  > 정식 경로가 열렸으니 임시 왕복을 만들 이유가 줄었다. 앱 쪽 대응 심볼은 아직 0건이다
  > ([api/parfait.md](../api/parfait.md) Android 매핑). 새로 딸려오는 결정은 OQ-P-189(마감 캔버스
  > 배경 변경)·OQ-P-190(참조 카운트)·OQ-P-191(id vs URL·조건부 필수)이다.
  > ⚠️ **①이 서버 쪽에서 막혀 있음이 확인됐다(2026-08-15)** — 캔버스 조회 계약이 공백이라던 전제가
  > [api/parfait.md](../api/parfait.md)로 채워졌고, 거기서 **읽기 필드(`background.type`·`value`)는 있는데
  > 쓰는 API가 서버 어디에도 없다**는 것이 드러났다(`parfait` 테이블 컬럼과 응답 필드만 있고 채우는 코드가
  > 없어 현재는 항상 `null`이다). 앱 쪽 `CanvasBackground` 도메인 모델과 조회 표면은 PR #250으로 들어왔으니
  > **읽는 절반은 준비됐고 쓰는 절반이 서버에 없다.** 즉 ②(서버 전까지 C-001로 값을 되돌릴지)가 임시가
  > 아니라 **당분간 유일한 선택지**다 — 서버에 배경 설정 API를 요청하는 것이 ①의 실질 내용이 됐다.
- **해소 메모**: ①이 서버 계약 대기라면 ②를 임시로라도 열어야 화면이 의미를 갖는다. 결정되면 [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) 드리프트 1·[c001 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md)을 함께 갱신한다.

### [2026-08-15] 배경 편집 미리보기가 `YGCanvas`를 재사용하지 않는다 — 편집 화면과 실제 캔버스가 다르다

- **ID**: OQ-P-174
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditScreen`(PR #231) — 미리보기가 `Box` + `aspectRatio(CANVAS_ASPECT_RATIO)` + `border`로 직접 그려진다. 그래서 좌상단 컷 도형(`canvasCutCornerShape`)·날짜 라벨·Dot Grid·메뉴가 없고, 좌우 여백이 C-001의 `padding7`(20)이 아니라 **21dp 리터럴**이다(코드 주석 "21.dp 공통에 없음"이 토큰 부재를 자인한다). 위키 [[캔버스-반응형-레이아웃]]의 좌우 20·컷 도형 규정과 어긋나는 화면이 하나 더 생긴 셈이다.
- **항목**: ① 미리보기를 `YGCanvas`(또는 그 축소 변형)로 바꿀지 — 지금 `YGCanvas`는 `fillMaxSize` 전제로 자기 배치를 계산해서 그대로 끼우면 어긋난다([c001 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md) 드리프트 7과 같은 뿌리). ② 아니면 편집 미리보기는 "실물 축소"가 아니라는 것을 정책으로 확정할지. ③ 21dp를 토큰으로 올릴지 20으로 맞출지.
- **상태**: 미해결
- **해소 메모**: ①을 고르면 `YGCanvas`의 배치 계산을 파라미터로 분리하는 일이 선행한다. 정해지면 [design-system](../architecture/design-system.md) 캔버스 절과 [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) 드리프트 2를 정리한다.

### [2026-08-15] C-301의 "토핑" 탭이 비어 있다 — 편집 모드의 절반이 미구현

- **ID**: OQ-P-175
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditScreen`·`CanvasEditTab`(PR #231) — 탭이 배경/토핑 2종인데 `selectedTab` 상태만 바뀌고 본문·팔레트는 그대로다. 위키 [[기능정의서-v3]]은 C-301을 "파르페 편집 모드 진입"(배경 변경 + 누끼 사진 편집 통합 진입점)으로 정의하므로, 통합 진입점의 한쪽이 빈 채로 머지됐다. 화면·심볼 이름도 배경만 가리킨다(`CanvasBGEdit*`·`NavKeyCanvasBGEdit`).
- **항목**: ① 토핑 탭에서 무엇을 편집할지 확정(위키 표의 C-305 토핑 편집·C-306 테두리 편집과의 관계 — 이미 `feature/segmentation`에 `NavKeyToppingEdit` 두 탭이 있다), ② 탭 선택이 화면 안 전환인지 다른 목적지로의 이동인지, ③ 심볼 이름을 C-301 전체를 가리키게 고칠지.
- **상태**: 부분 해소 — **②는 답이 나왔고 ①③은 열려 있다**(2026-08-16, PR #264)
  > ✅ **탭이 채워졌고 전환은 화면 안 분기로 확정됐다** — 토핑 탭이 선택·이동·크기·회전·삭제와
  > 테두리 재편집 왕복을 갖췄다. **②**: 탭 선택은 다른 목적지로의 이동이 아니라 같은 화면 안에서
  > 본문(팔레트 vs 토핑 스택)을 가르는 분기다. 위키 표의 C-305(토핑 편집)는 **선택 + 모서리 버튼**으로,
  > C-306(테두리 편집)은 **`NavKeyToppingEdit(borderOnly = true)`**로 기존 C-104/C-105 화면을 재사용해
  > 성립했다 — 새 목적지를 만들지 않았다.
  > **①③은 그대로다**: 편집 대상이 아직 mock이고(OQ-P-199), 심볼·NavKey 이름은 여전히
  > `CanvasBGEdit*`라 화면 전체를 가리키지 않는다. 새 라운드의 설계·드리프트는
  > [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)이 갖는다.
- **해소 메모**: ①은 위키의 "에딧 모드 삭제 비고 vs C-301~C-306 잔존" 미결과 맞물린다(정책 소관은 위키 [[open-questions]]). 정해지면 [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) 범위·정책 대조 표를 갱신한다.

### [2026-08-15] C-301의 State·Effect가 UI 타입을 들고 팔레트 색이 코드 hex로 확정됐다

- **ID**: OQ-P-176
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditViewModel`(PR #231) — `CanvasBGEditUiState.selectedColor`가 Compose `Color`, `CanvasBGEditEffect.ConfirmBackground`가 디자인시스템 `YGCanvasBackground`다. 팔레트 `CanvasBackgroundPaletteColors`는 ViewModel 파일의 public 상수이고 8종 중 3종만 `YGAtomicColors`(White·Black·Cherry200), **나머지 5종은 hex 리터럴**이다. [state-management](../architecture/state-management.md)는 "State는 도메인 의미를 들고 표시 변환은 화면이 한다"고 적는다.
- **항목**: ① 배경 선택값을 도메인 표현(예: 팔레트 인덱스·색 코드 문자열)으로 바꿀지 — 서버 저장 계약이 정해지면 그쪽이 답을 준다([2026-08-15] 반영 항목과 묶인다). ② 팔레트 색 5종을 디자인 토큰(`YGAtomicColors`)으로 승격할지 — 승격하려면 디자인 소스에서 이름을 받아야 한다. ③ 팔레트 목록의 소유를 ViewModel 파일이 아닌 곳으로 옮길지.
- **상태**: 미해결
- **해소 메모**: ②는 [design-system](../architecture/design-system.md) 원자 색 확산 논의와 같은 자리다. 정해지면 [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) 드리프트 4를 정리한다.

### [2026-08-15] 캔버스 비율 상수가 `domain`과 `core:designsystem`에 이중으로 존재한다

- **ID**: OQ-P-177
- **출처**: `domain/model/CanvasConst.kt#CANVAS_ASPECT_RATIO`(PR #231 신설) × `core/designsystem/.../ygcanvas/YGCanvas.kt#CANVAS_AREA_ASPECT_RATIO`(private) — 값이 같고 뜻도 같은데 모듈이 다르다. 캔버스 비율은 도메인 규칙이 아니라 표시 규격이라 `domain` 배치가 [module-structure](../architecture/module-structure.md) 레이어 의도와 어긋난다(Android 의존이 없어 "순수 Kotlin" 규칙 자체를 어기지는 않는다). 한쪽만 바뀌면 편집 미리보기와 실제 캔버스의 비율이 조용히 갈린다.
- **항목**: ① 소유를 `core:designsystem`으로 모으고 public으로 올릴지, ② 아니면 `domain` 상수를 정본으로 삼고 `YGCanvas`가 참조할지(디자인시스템 → domain 의존이 생긴다), ③ 화면 규격 상수 전반의 소유 규칙을 세울지.
- **상태**: 부분 해소 (**①은 develop 머지로 닫혔다**(2026-08-22, PR #334). ③은 그대로 열려 있다)
- **해소 메모**: ①이 의존 방향상 자연스럽다. 정하면 [module-structure](../architecture/module-structure.md) 규칙 항목과 [design-system](../architecture/design-system.md) 캔버스 절을 정리한다.
  > ✅ **①로 닫혔다**(2026-08-21 브랜치 작업 → 2026-08-22 develop 머지, PR #334) —
  > `domain/model/CanvasConst.kt`를 지우고 `YGCanvas`의 `CANVAS_AREA_ASPECT_RATIO`를 public으로 올려
  > 정본을 하나로 뒀다. ②를 고르면 `core:designsystem` → `:domain` 간선이 새로 생기는데, 캔버스 비율은
  > 도메인 규칙이 아니라 표시 규격이라 소유가 이쪽이다. **상수가 하나뿐이라 갈라짐을 막을 단언이
  > 필요 없어졌다** — [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md)이
  > 계획하던 "통일이 되돌려지면 깨지는 테스트"를 그래서 만들지 않았고, 그 자리는 컴파일이 지킨다.
  > ③(화면 규격 상수 **전반**의 소유 규칙)은 이 한 건으로 서지 않아 그대로 남는다.
  > **브랜치가 develop에 들어오면 상태를 `해소됨`으로 올린다.**

### [2026-08-15] 재사용 진입을 NavKey 동작 플래그로 가르고 복귀는 `onBack()` 2회에 기댄다

- **ID**: OQ-P-178
- **출처**: `feature/camera/api` `NavKeyCameraCustom`·`NavKeyPictureConfirm`, `feature/gallery/api` `NavKeyCustomGalleryPicker`, `feature/camera/impl` `PictureConfirmRoute`(PR #231) — `showGuideToast`·`returnResultOnly`는 화면이 그릴 데이터가 아니라 **호출자가 고르는 동작 분기**인데 `@Serializable` 백스택 키에 실린다. 확인 화면은 `returnResultOnly`면 `sendResult(PictureConfirmResult)` 후 `navigator.onBack()`을 두 번 부른다(어느 화면을 걷는지 주석으로만 표시). 또 카메라 실패·취소는 여전히 `sendResult(uri: String?)`라 **반환 타입이 한 플로우에 둘**이고, `ResultEffect<PictureConfirmResult>`만 구독하는 C-301은 실패를 못 받아 아무 표시 없이 돌아온다.
- **항목**: ① 재사용 화면의 동작 차이를 NavKey 인자로 싣는 것을 관용구로 확정할지, 아니면 목적지를 나눌지(`NavKeyPictureConfirmForBackground` 등). ② 복귀를 `onBack()` 2회 대신 명시적 수단(`goToSingleClearTop`·`popUpTo` 계열)으로 바꿀지. ③ 실패 반환 타입을 결과 타입 하나로 합칠지(`PictureConfirmResult`에 실패 표현 추가).
- **상태**: 부분 해소 (**②는 PR #309 develop 머지로 닫혔다, 2026-08-20.** ①③은 미해결)
  > ✅ **복귀가 깊이 대신 타입이 됐다(PR #309)** — `PictureConfirmRoute`의 `returnResultOnly = true`
  > 경로가 확인·닫기 두 콜백 모두 `popUpTo<NavKeyCanvasBGEdit>()`다. `onBack()` 2회는 흐름 깊이가
  > 정확히 2라고 가정했고 **그 가정을 주석이 설명하고 있었다는 것 자체가 신호**였다 — 사이에 화면이
  > 하나 끼는 날 조용히 어긋난다. 목적지를 타입으로 특정할 수 있는 근거는 `returnResultOnly = true`를
  > 주는 곳이 `CanvasBGEditRoute` 하나뿐이고 `NavKeyCanvasBGEdit`이 `data object`라 백스택에 최대
  > 한 장이라는 것이다. **대가는 카메라가 자기를 부른 화면을 이름으로 안다는 결합**이고, 둘째 호출자가
  > 생기면 이 분기를 고쳐야 한다. ③도 절반은 사라졌다 — 카메라 실패·취소가 `sendResult(uri: String?)`를
  > 하지 않게 되면서(`CustomCameraEffect.Cancel`은 인자가 없다) **한 플로우에 반환 타입이 둘인 상태는
  > 없어졌다.** 다만 C-301이 실패를 아는 수단은 여전히 없다(이제는 아무 결과도 오지 않는다).
- **해소 메모**: ①은 아직 관용구로 확정되지 않았다 — 같은 라운드가 `returnResultOnly` 분기를 하나 더
  늘렸다(닫기). 정해지면 [navigation-flow](../architecture/navigation-flow.md) "캔버스 배경 편집 플로우" 절과
  [c301](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)·[c101](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md) 스펙을 갱신한다.

### [2026-08-15] 그룹 닉네임 중복 409가 서버에서 사라져 앱 분기·테스트가 死코드가 됐다

- **ID**: OQ-P-179
- **출처**: 서버 `e4ff23f`(`fix: 그룹 내 닉네임 중복 검사 제거`) × `feature/groups/enter/impl` `GroupNickNameViewModel`·`GroupNickNameViewModelTest`·`domain/model/error/ServerErrorCode.kt`·`data/repository/group/ParfaitGroupRepositoryImplTest` — 서버가 참여·닉네임 변경 양쪽의 `existsByGroupIdAndNickname` 검사와 `GROUP_NICKNAME_ALREADY_USED`를 **포트·어댑터·리포지토리 메서드·에러 코드까지 통째로 삭제**했다(사유: "정책상 같은 그룹 안 닉네임 중복 허용"). 앱에는 그 코드를 `GroupNickNameError.ALREADY_USED`로 매핑하는 분기, 그 분기를 검증하는 유닛 테스트 2건, `ServerErrorCode.ParfaitGroup` 상수가 그대로 남아 **어느 것도 도달하지 않는다**. S-102는 이제 중복 닉네임을 조용히 성공시킨다.
- **항목**: ① 앱에서 분기·상수·테스트를 걷어낼지(서버 결정을 확정으로 볼 때). ② 그룹 안 닉네임 중복 허용을 정책 문서에 명시할지 — 지금 근거는 서버 커밋 메시지뿐이고 위키에는 항목이 없다. ③ 같은 그룹에 같은 표시 이름이 여럿일 때 G-001·S-101 그룹원 목록·토핑 작성자 표시를 무엇으로 구분할지(현재 구분자는 닉네임뿐이다).
- **상태**: 부분 해소 — **①은 처리됐고 ②③은 열려 있다**(2026-08-15, PR #250)
  > ✅ **앱에서 死코드가 걷혔다(PR #250)** — `ServerErrorCode.ParfaitGroup.GROUP_NICKNAME_ALREADY_USED`,
  > `GroupNickNameError.ALREADY_USED`(+`toStringResource` 분기), `strings.xml` 문구 1건,
  > `GroupNickNameViewModel`의 매핑 분기가 함께 사라졌다. 그 코드를 검증하던 두 테스트
  > (`GroupNickNameViewModelTest`·`ParfaitGroupRepositoryImplTest`)와 화면 프리뷰 에러 케이스는 삭제가
  > 아니라 **`INVALID_GROUP_NICKNAME`(400)으로 바꿔 살렸다** — 서버 실패를 화면 문구로 잇는 경로 자체는
  > 계속 잠긴다. `ServerErrorCode.ParfaitGroup`은 8종 → 7종이 됐고 "분기에 쓰는 코드만 둔다"는 자기
  > KDoc 규칙이 유지된다.
  > ⚠️ **②③은 그대로다** — 그룹 안 닉네임 중복 허용의 근거가 여전히 서버 커밋 메시지뿐이고, 같은 그룹에
  > 같은 표시 이름이 여럿일 때 G-001·S-101 그룹원 목록·토핑 작성자를 무엇으로 구분할지는 미정이다
  > (현재 구분자는 닉네임뿐인데 이제 유일하지 않다).
- **해소 메모**: ①은 [s102 스펙](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md) 에러 매핑 절과 함께 정리한다. ③은 표시 정책이라 위키 [[nametag-chip]]·[[그룹]] 쪽 결정이 필요하다. 계약 쪽 기술은 [api/parfait-group.md](../api/parfait-group.md)에 이미 반영했다.

### [2026-08-15] 초대코드 자릿수가 서버 8 → 6으로 내려와 앱과 맞았다 — 정책 문서는 여전히 없다

- **ID**: OQ-P-180
- **출처**: 서버 `e4ff23f`(`refactor: 그룹 참여 코드 자릿수 8자에서 6자로 변경`, `InviteCode.LENGTH`·JPA 컬럼·마이그레이션 V13) × `domain/model/group/InviteCode.kt`(`LENGTH = 6`)·A-004 입력 칸 6개 — 앱은 처음부터 6이었고 서버는 8을 검증했다. 즉 **앱이 보낸 코드는 서버 형식 검증(`InviteCode.of`)을 통과할 수 없었다.** 실서버 요청이 0건이라(OQ-P-146) 드러나지 않았고, 계약 문서에 서버 자릿수가 적혀 있지 않아 대조에서도 걸리지 않았다. 이제 양쪽이 6으로 맞다.
- **항목**: ① 자릿수 6과 문자 집합을 정책 문서에 올릴지 — 서버 생성 알파벳은 혼동 문자(`I`·`O`·`0`·`1`)를 뺀 32자인데 검증은 영숫자 전부를 받고, 앱은 대문자 정규화를 하지 않는다(서버가 `uppercase()`). 세 규칙 중 어느 것도 위키에 없다. ② 계약 문서에 값 형식(길이·문자 집합)을 적는 것을 규약으로 올릴지 — 이번 불일치가 **형식을 안 적어서** 숨었다.
- **상태**: 부분 해소 (자릿수는 맞음, 정책·규약 공백 잔존)
- **해소 메모**: ②는 [api/template.md](../api/template.md)에 "요청 필드 표에 값 형식을 적는다"를 넣는 것이 최소 조치다. ①은 [a004 스펙](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md)이 이미 "코드 자릿수 6은 정책 문서 없이 코드가 확정"이라 적어 둔 것과 같은 자리다.

### [2026-08-15] 캔버스 매퍼의 조용한 폴백 3종에 관측 수단이 없다

- **ID**: OQ-P-181
- **출처**: `data/source/parfait/mapper/VOMapper.kt`(PR #250) — 서버 값이 앱이 모르는 것일 때 세 자리가
  **조용히 접힌다**: `status`가 미지값이면 `CanvasStatus.UNKNOWN`, `background.type`이 미지값이면
  **`null`**(= 미설정과 같은 값), 토핑이 `borderType = SOLID`인데 색·두께가 비면 `ToppingBorder.None`.
  셋 다 크래시를 막는다는 근거가 분명하고 KDoc에도 적혀 있다. 다만 **어디에도 로그가 없다** — 앱에
  `Logger` 추상화가 있는데([ADR-0014](../adr/0014-logging-abstraction-kermit.md)) 매퍼는 쓰지 않는다.
  특히 배경의 미지 type은 "미설정"과 **같은 값으로 접혀** 두 경우를 구분할 근거가 결과에 남지 않는다.
- **항목**: ① 폴백이 발동한 사실을 로그로 남길지 — 남긴다면 매퍼가 `Logger`를 갖는 첫 사례가 되고,
  `internal` 확장 함수 모음이라 주입 경로부터 정해야 한다(DataSource가 로그를 찍는 편이 구조상 쉽다).
  ② 아니면 서버 enum 확장은 계약 변경이므로 계약 대조(이 문서·`api/`)로만 잡는다고 확정할지.
  ③ 배경의 미지 type과 미설정을 화면이 구분해야 할 일이 생기는지 — 지금 결론("화면 처리가 같다")은
  배경 편집(C-301)이 결선되기 전의 판단이다.
- **상태**: 미해결 (**소비처가 생겼다** — 2026-08-17)
  > 📌 **폴백이 화면에도 생겼고 자리가 여섯이 됐다(2026-08-17, PR #268)** — C-001이 캔버스 응답을
  > 그리면서 **색 문자열 파싱 실패** 두 자리가 늘었다: 토핑 `borderColor`를 못 읽으면 **테두리를
  > 안 그리고**, `background.value`를 못 읽으면 **기본 배경**(`Solid(Gray100)`)으로 간다. 근거는
  > 매퍼와 같다("임의의 색을 골라 칠하는 것보다 덜 틀리다") — 그리고 **로그도 매퍼와 같이 없다.**
  > ③의 판정이 실제로 필요해진 것도 여기다: 화면은 **미설정·미지 type·파싱 실패 셋을 같은 기본
  > 배경으로** 떨어뜨려 세 경우가 사용자에게 완전히 같아졌다. 매퍼가 접은 것(미지 type → `null`)과
  > 화면이 접은 것(파싱 실패 → 기본값)이 **다른 층에서 두 번** 일어난다는 점도 새로 드러났다.
  > 📌 **화면이 접던 자리가 컴포넌트로 내려갔다(2026-08-25, PR #351)** — `CanvasMainScreen`의
  > `DEFAULT_CANVAS_BACKGROUND`가 사라지고 파싱 실패가 **미설정과 같은 `null`로** 떨어진다.
  > `YGCanvas`의 `background`가 nullable이 되어 `null`이면 흰 바탕을 깐다. **접히는 값이
  > `Solid(Gray100)`에서 흰 바탕으로 바뀌었을 뿐 세 경우가 같아지는 성질은 그대로**이고,
  > 화면에 복제돼 있던 기본값이 없어진 만큼 접는 층은 둘에서 **하나**로 줄었다.
  > ③(미지 type과 미설정을 구분해야 하는가)은 값이 옮겨 갔을 뿐 그대로 열려 있다.
  > 📌 **③의 걱정 한 갈래는 구조로 닫혔고 다른 갈래가 늘었다(2026-08-16, PR #266)** — 앱이 배경을
  > 되돌려 보낼 때 **읽기 모델을 재사용하지 않는다**(쓰기 전용 `CanvasBackgroundEdit`). 그래서 "미지
  > type을 `null`로 접었다가 다음 저장 요청에서 조용히 사라진다"는 경로는 생기지 않는다. 대신
  > **배경 변경 응답에도 같은 폴백이 걸렸다** — 저장은 성공인데 결과가 `null`이면 그릴 값이 없다
  > (OQ-P-193). 폴백 자리가 셋에서 **넷**이 된 셈이고 ①(로그를 남길지)의 대상도 그만큼 늘었다.
  > 📌 **③의 전제가 바뀌었다(2026-08-16)** — 배경 쓰기 API가 생겨(OQ-P-173) 배경 편집 결선이 실제
  > 일정에 들어왔다. "미지 type과 미설정을 화면이 구분할 일이 없다"는 판단은 **읽기 전용이던 시절의
  > 것**이고, 이제 앱이 배경을 **되돌려 보내는** 경로가 생긴다 — 미지 type을 `null`로 접으면 다음 저장
  > 요청에서 그 값이 조용히 사라진다.
- **해소 메모**: ①을 택하면 [data-layer](../architecture/data-layer.md) "응답 매핑" 절에 규약으로 적는다.
  ③은 OQ-P-173(배경 연동)·OQ-P-191(id vs URL)과 같은 라운드에서 답이 정해진다.

### [2026-08-15] `http/`의 파괴적 요청이 파일 순서 실행 한 번에 계정·데이터를 지운다

- **ID**: OQ-P-182
- **출처**: `http/users.http`(마지막 요청이 회원 탈퇴)·`http/parfait-image.http`(마지막 요청이 토핑 삭제,
  PR #250) — 두 파일 모두 "위에서부터 순서대로 실행"이 권장 사용법인데 그 끝에 되돌릴 수 없는 요청이
  붙었다. 탈퇴는 회원 행을 지우고 모든 그룹 멤버십을 정리하며, 토핑 삭제는 참조 카운트가 0이 되면
  **S3 객체까지** 지운다. 현재 방어는 `http/README.md`의 ⚠️ 문장 하나뿐이다 — IntelliJ HTTP Client에는
  요청 단위 확인 절차가 없다.
- **항목**: ① 파괴적 요청을 별도 파일(예: `_danger.http`)로 분리할지 — 분리하면 "도메인별 한 파일"
  규칙이 깨진다. ② 요청 본문을 주석 처리해 두고 쓸 때만 풀게 할지. ③ 아니면 문서 경고로 충분하다고
  볼지(개발 서버 전용이고 계정을 다시 만들 수 있다는 전제).
- **상태**: 미해결 (실서버 요청이 0건이라 아직 사고는 없었다)
- **해소 메모**: 정하면 [api/README.md](../api/README.md) "계약을 실제로 확인하는 법" 절과
  `http/README.md`를 함께 맞춘다. [2026-08-04] `http/`↔`api/` 이중 관리 항목과 같은 자리에서 본다.

### [2026-08-16] 캘린더가 mock UseCase로 결선됐고 mock 생성 로직이 `domain`에 산다

- **ID**: OQ-P-183
- **출처**: `domain/usecase/parfait/GetParfaitHistoriesUseCase.kt`·`GetParfaitYearsUseCase.kt`(PR #259) —
  둘 다 고정 지연 후 성공만 반환하고, `ParfaitRemoteDataSource`·`ParfaitService`(표면은 PR #250으로
  이미 있다)를 호출하지 않는다. **이번 것은 한 걸음 더 갔다** — 반환값이 리터럴 하나가 아니라
  달마다 정해진 날짜 목록·`dayOfYear % 9`로 흩뜨린 이미지 수·epoch day를 쓴 가짜 `ParfaitId`를
  만드는 **생성 로직이 프로덕션 `domain` 코드에 있다**. 그 형태가 2026-08-12 그룹 라운드에서 한 번
  들어왔다가 #243·#244·#248로 전부 걷힌 것(OQ-P-134 해소)과 같은데 하루 만에 되돌아왔다.
  `groupId`는 인자에서 아예 빠져 있다 — 화면 `NavKeyCanvasImageAdd`가 `data object`라 그룹 식별자를
  들고 있지 않기 때문이고, 그래서 실연동 시 **NavKey 인자 추가가 선행**이다(OQ-P-129 ①과 같은 자리).
- **항목**: ① mock을 UseCase 본문에 두는 것을 관례로 볼지 — 이번 라운드까지 develop에 mock 소유
  형태가 넷이다(UiState 기본값 / 로드 함수 / UseCase 반환 / **UseCase 안 생성 로직**). ② 실연동
  라운드에서 `ParfaitRepository`를 둘지(OQ-P-094 ②의 결론대로면 둔다). ③ `groupId` 전달을 NavKey
  인자로 열지, 그룹 컨텍스트를 다른 방식으로 들고 다닐지.
- **상태**: **해소됨** (2026-08-17, PR #279 — 실연동 완료. ①의 판단만 사례로 남는다)
  > ✅ **②③이 답을 얻었는데 mock은 그대로다(2026-08-17, PR #268)** — ②는 **`ParfaitRepository`를 둔다**로
  > 확정됐고(오늘·목록·상세 셋을 열었다), ③은 **NavKey 인자**로 열렸다(`NavKeyCanvasImageAdd(groupId)`).
  > 즉 "화면이 그룹 식별자를 안 갖고 있어 UseCase 인자에서 뺐다"는 근거가 사라졌다. **그런데 이 두
  > UseCase는 손대지 않아**, 지금 같은 ViewModel 안에서 **캔버스 조회는 Repository를 타고 달력 조회는
  > mock을 만든다.** ①(mock을 UseCase 본문에 두는 것을 관례로 볼지)은 그대로이고, 이제 **같은 파일
  > 안의 대조군**이 생겨 판단 근거가 더 분명해졌다.
- **해소 메모**: **PR #279가 다음 날 실연동을 끝냈다.** 예고한 첫 걸음(연도 조회를 `ParfaitRepository`에
  올리는 것) 그대로 `getYears`가 올라왔고 두 UseCase가 Repository를 주입받아 mock 생성 로직이 전부
  사라졌다. 덤으로 `ParfaitHistory`가 **삭제**되고 달력이 계약 VO `PastCanvasVO`를 그대로 쓰게 돼
  "응답의 어느 필드에 대응하는지 코드에 없다"는 미검증도 닫혔다.
  ①(mock을 UseCase 본문에 두는 것을 관례로 볼지)은 **결정이 아니라 사례로 남는다** — 이번 것도 하루를
  못 넘겼다(OQ-P-134와 같은 결말). 반영처: [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md)
  "데이터" 표·드리프트 1 · [api/parfait.md](../api/parfait.md) Android 매핑 ·
  [data-layer](../architecture/data-layer.md) Repository 인벤토리 ·
  [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md).
  **실연동이 새로 연 미결 넷은 OQ-P-211~214다.**

### [2026-08-16] 고른 날짜가 아무것도 바꾸지 않는다 — 캘린더의 출력이 셀 강조뿐이다

- **ID**: OQ-P-184
- **출처**: `feature/groups/canvas/impl` `viewmodel/CanvasImageAddViewModel.kt`(`ClickDate` 처리)·
  `component/CustomCalendar.kt`(PR #259) — 날짜를 누르면 `selectedDate`만 갱신된다. 달력은 닫히지
  않고, 상단 날짜 라벨(`canvasDate`·`canvasDay`)은 오늘 고정이며, 캔버스 내용도 그대로다(토핑 슬롯
  자체가 미사용, OQ-P-130 ③). 즉 **날짜 선택이 캔버스 전환으로 이어지는 경로가 없다** — 캘린더가
  하는 일은 셀 강조까지다.
- **항목**: ① 날짜를 고르면 무엇이 일어나야 하는가 — 그 날 캔버스를 열지(그러면 화면이 "오늘"이
  아닌 상태를 가져야 한다), 아니면 목록·상세로 넘어갈지. 위키 [[캘린더-컴포넌트]]는 컴포넌트 정의만
  담고 선택 후 동작을 적지 않는다. ② 선택 즉시 달력을 닫을지(지금은 다시 눌러야 닫힌다).
  ③ `selectedDate` 기본값이 오늘인데 그 상태와 "아무것도 안 골랐다"가 구분되지 않는 것을 유지할지.
- **상태**: **해소됨** (2026-08-17, PR #268 — ①② 답 나옴, ③만 별도로 이월)
- **해소 메모**: **①은 "그 날 캔버스를 연다"로 정해졌다.** 화면은 오늘이 아닌 상태를 갖게 됐고
  (`selectedDate`가 배경·토핑·상단 라벨을 모두 지배한다), 조회는 `/parfaits/today`가 아니라
  **목록→상세 2단**(`GetCanvasByDateUseCase`)이다 — `today`가 조회인데 캔버스를 만들어서, 달력을 훑는
  것만으로 빈 캔버스가 쌓이면 안 되기 때문이다. **②도 즉시 닫기로 정해졌다.** 새 캔버스가 오기 전에
  이전 날 그림을 비우고(머리말과 그림이 어긋난 상태를 보이느니 잠깐 비는 편이 덜 틀리다), 응답 경합은
  중복 실행 가드가 아니라 **반영 직전 `selectedDate` 재확인**으로 막는다(날짜 선택은 마지막에 고른
  것이 이겨야 하는데 `launch` key 가드는 앞선 조회를 살린다). **③은 그대로 남는다** — 기본값이 오늘이라
  "아무것도 안 골랐다"와 구분되지 않고, 지금은 같은 날 재선택을 "닫기만"으로 처리해 드러나지 않는다.
  반영처: [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md) 드리프트 2 ·
  [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md).
  > ⚠️ **②의 두 근거가 하루 만에 뒤집혔다(2026-08-17, PR #279)** — 이전 날 그림을 **비우지 않게** 됐고
  > (달력이 기록 있는 날만 열어 주므로 "잠깐 비어 보임"이 항상 거짓말이라는 새 근거) 상세 조회에
  > **`launch(key)` 가드가 붙었다**(= 앞선 조회가 이긴다). 두 번째 변경은 근거가 코드에 없고,
  > 위 문장이 적어 둔 이유와 정면으로 어긋난다 → OQ-P-212.

### [2026-08-16] 세션 인프라에 남은 구멍 넷 — 유실 창·단일 수집·한정자 그물·재발급 쿨다운

- **ID**: OQ-P-185
- **출처**: `data/session/SessionEventBus.kt`·`data/network/TokenAuthenticator.kt`·`app/MainRoute.kt`
  (PR #260, [ADR-0021](../adr/0021-token-refresh-forced-logout.md) "트레이드오프"·"위험·방어") —
  ① **`Channel(CONFLATED)`에 `onUndeliveredElement`가 없다.** 값을 꺼낸 직후 수집 코루틴이 취소되면
  (Activity 재생성 창) 이벤트가 조용히 사라지고, 토큰은 이미 지워진 뒤라 이후 401은 "refresh token
  부재" 경로로 조용히 끝나 **두 번째 이벤트가 오지 않는다** — 로그인 화면으로 못 가고 실패만 계속 본다.
  ② **수집 지점이 앱 루트 한 곳이라는 것은 규약일 뿐 기계 검사가 없다**(`BaseViewModel.effect`가
  구독자 수를 세어 로그를 남기는 것과 대비된다). ③ **`TokenAuthenticator`가 `@UnauthenticatedClient`
  `AuthService`를 받는다는 사실에 그물이 없다** — 생성자에서 한정자만 지우면 모든 테스트가 통과하면서
  디스패처 데드락이 되살아난다(`NetworkModuleTest`는 클라이언트 쪽 성질만 잠근다). ④ **재발급 실패에
  쿨다운이 없다** — 오프라인에서 401 N건이 각자 최대 15초(read timeout)씩 직렬로 재발급을 시도한다.
- **항목**: ① 유실 창을 닫을지(`onUndeliveredElement`로 재발행 / 이벤트 대신 "세션 없음" 상태를
  구독) ② 수집 지점 단일성에 방어를 둘지 ③ 한정자 누락을 컴파일·테스트로 잡을 방법이 있는지
  ④ 실패 결과를 짧게 공유하는 쿨다운을 넣을지.
- **상태**: 미해결 (전부 설계 시점에 알고 남긴 것 — 실기기 검증 전이라 체감 여부 미확인)
- **해소 메모**: ①④는 [ADR-0021](../adr/0021-token-refresh-forced-logout.md) "영향"에, ②는
  [navigation-flow](../architecture/navigation-flow.md) "세션 종료 이동"에 반영한다.
  > 📌 **①의 피해 범위만 줄었다(2026-08-16, PR #263)** — 자동로그인 라운드가 머지되며 앱 진입마다
  > `BootstrapSessionUseCase`가 토큰 유무를 보고 없으면 로그인으로 보낸다. 이벤트가 유실돼도
  > **다음 앱 실행에서는 로그인 화면으로 간다**. 다만 유실된 그 실행 안에서는 여전히 갇힌 채
  > 실패만 보고, ②③④는 손대지 않았다. "세션 없음을 상태로 들인다"는 방향은 채택되지 않았다 —
  > 부트스트랩은 진입 시점에 한 번 판정할 뿐 상시 구독이 아니다.

### [2026-08-16] 로그아웃 비활성이 사용자에게 안 보이고, 같은 화면의 탈퇴는 여전히 stub이다

- **ID**: OQ-P-186
- **출처**: `core:designsystem` `component/ygactionitem/YGActionItem.kt`(`enabled` 신설, PR #260)·
  `feature/app/setting/impl` `screen/AppSettingScreen.kt`·`viewmodel/AppSettingViewModel.kt` —
  ① 요청 중 로그아웃 항목이 `enabled = !isLoggingOut`으로 클릭만 막히고 **색은 그대로다**. 비활성 색이
  디자인시스템에 없어 컴포넌트가 임의로 정하지 않았고 KDoc에도 그렇게 적혀 있다 — 사용자는 눌러도
  아무 일이 없는 이유를 알 수 없다. 로딩 표시도 없다. ② 같은 Danger Zone의 **회원 탈퇴는 stub**인데
  서버 계약(`DELETE /api/v1/users/me`)도 앱 표면(`MemberRemoteDataSource#withdraw`)도 이미 있다
  ([api/member.md](../api/member.md)) — 한 화면에 결선된 항목과 안 된 항목이 나란히 있다.
- **항목**: ① `YGActionItem` 비활성 색을 디자인에서 받을지, 아니면 진행 표시(스피너·문구)로 대신할지.
  ② 탈퇴 결선을 어느 라운드에 둘지 — 탈퇴는 로그아웃과 달리 **되돌릴 수 없어** 실패 표현이 필요하고,
  애플 연동 해제 수단 부재(OQ-P-164)와도 얽힌다.
- **상태**: 부분 해소 (②는 닫혔다 — ①의 비활성 색은 그대로 미결)
- **해소 메모**: ①은 [design-system](../architecture/design-system.md) `YGActionItem` 항목과
  [ygactionitem 스펙](../superpowers/specs/archive/2026-07-12-ygactionitem.md)에 반영한다. ②는 결선 시
  [api/member.md](../api/member.md) Android 매핑을 함께 갱신한다.
  > 📌 **옆 화면이 먼저 답을 만들었다(2026-08-17, PR #287)** — S-101 그룹 설정의 나가기·신고가
  > 같은 성질의 되돌릴 수 없는 동작을 결선하며 **덮개는 `YGScaffoldV2` 로딩 오버레이, 실패는 공통
  > 토스트**라는 형태를 확정했다(①이 물은 "비활성 색이냐 진행 표시냐"에 진행 표시 쪽 사례다).
  > ②의 탈퇴는 그대로 stub이라, 이제 develop에서 **되돌릴 수 없는 확인 셋 중 둘만 동작한다**.
  > ✅ **②가 닫혔다(2026-08-19, PR #306)** — 옆 화면이 만든 답을 그대로 가져와, 탈퇴도 로딩 오버레이 +
  > 실패 토스트로 결선됐다. **한 Danger Zone 안에서 하나만 동작하던 상태가 끝났다.** 되돌릴 수 없다는
  > 성질에 대한 답은 **실패 표현(토스트)과 연타 잠금**이었고, 애플 연동 해제(OQ-P-164)는 이번에도
  > 다루지 않았다 — 탈퇴는 서버 계약대로만 나가고 앱은 애플 쪽을 모른다.
  > ⚠️ **①은 그대로다** — 로그아웃 항목은 여전히 `enabled`만 꺼지고 색이 안 바뀐다. 오히려 이번에
  > **같은 화면에 진행 표시 두 갈래가 공존**하게 됐다: 탈퇴는 화면 전체를 덮고, 로그아웃은 그 오버레이가
  > 뜨는 동안에도 항목 비활성이라는 별도 신호를 함께 낸다(`isLoggingOut`이 `YGActionItem`에 남아 있다).

### [2026-08-16] 런처 아이콘 교체가 스플래시 테마 속성을 함께 지웠다 — 구버전 콜드 스타트 미검증

- **ID**: OQ-P-187
- **출처**: `app/src/main/res/values/themes.xml`·`drawable/splash_icon*.xml`·`mipmap-*`(PR #262,
  `app`·`app-preview` 동일 변경) — 적응형 아이콘 3종(전경·배경·**monochrome**)과 밀도별 모눈 배경
  PNG로 교체하면서 `Theme.TeamYg`의 `android:windowSplashScreenAnimatedIcon`·`android:windowBackground`를
  **제거**했다. 주석은 "비워 두면 Android 12+ 시스템 스플래시가 런처 아이콘을 그대로 쓴다"는 근거를
  적는데, `minSdk`는 26이다 — **Android 12 미만에서는 시스템 스플래시가 없고 `windowBackground`도
  사라져** 콜드 스타트 첫 프레임이 플랫폼 기본 테마 배경이 된다. 앱이 직접 그리는
  `feature:intro`의 `SplashScreen`은 그 뒤에 온다. 확인 기록은 없다.
- **항목**: ① Android 12 미만에서 콜드 스타트 첫 프레임이 무엇으로 보이는지 실기기 확인(흰 배경이면
  문제 없고, 검정·깜빡임이면 `windowBackground`를 되살려야 한다). ② `core-splashscreen`
  호환 라이브러리를 쓸지 아니면 12 미만을 그대로 둘지. ③ 아이콘 에셋이 `app`·`app-preview` 두 곳에
  **복제**돼 있는데(파일 내용 동일) 공유할지 — 지금은 한쪽만 고치면 조용히 갈린다.
- **상태**: 미해결 (렌더 확인 0건)
  > 📌 **고아 드로어블이 정리됐다(2026-09-17, PR #504)** — 위 출처가 `app/` 경로로 적은
  > `splash_icon.xml`·`splash_icon_background.xml` 은 실제로는 **`core/ui/src/main/res/drawable/`** 에
  > 있었고, 청소 라운드가 "쓰지 않는 리소스"로 둘 다 지웠다. 테마 속성이 빠진 뒤 아무도 참조하지
  > 않는 상태였다는 것이 이로써 확인됐다. **항목 ①②③은 그대로다** — 콜드 스타트 첫 프레임을
  > 실기기에서 본 기록이 여전히 없고, `core-splashscreen` 도입 여부도 정해지지 않았으며,
  > 아이콘 에셋의 `app`·`app-preview` 복제도 남아 있다(같은 라운드가 두 모듈의 `colors.xml` 에서
  > 템플릿 색 일곱을 지웠지만 아이콘 에셋은 건드리지 않았다).
- **해소 메모**: ①은 OQ-P-146 실기기 항목과 같은 회차에 본다.
  parfait에 앱 리소스·테마 인벤토리 문서가 없어 반영처는 결정 시 함께 정한다.

### [2026-08-16] 캘린더 라운드가 규약 이탈 셋을 함께 들였다 — 그리기 확장 소유·State 계산 프로퍼티·치수 리터럴

- **ID**: OQ-P-188
- **출처**: `core:util:android` `extension/Modifier.kt#verticalScrollbar` ·
  `feature/groups/canvas/impl` `component/CustomCalendar.kt#sideBorder`(파일 안 private) ·
  `viewmodel/CanvasMainViewModel.kt`(`CanvasMainUiState.selectableMonths`) ·
  `component/CalendarDropdown.kt`(폭·최대 높이 `dp` 리터럴)(PR #259) —
  ① 테마 비의존 그리기 확장의 소유가 이제 **네 곳**이다(`core:designsystem` `border/`·`shape/` /
  `core:designsystem` `component/ygbackgrounddotgrid/` / `core:util:android` `extension/` / feature
  파일 안 private). ② [state-management](../architecture/state-management.md)는 "State가 계산 프로퍼티로
  들 이유가 없다"고 적는데 `selectableMonths`가 State 안에 있다 — 다만 화면만이 아니라 ViewModel의
  연도 이동 계산도 읽어서 화면 헬퍼로 내리면 로직이 갈린다. ③ 드롭다운 폭·최대 높이·스크롤바
  기본값이 토큰 스케일 밖 리터럴이다(A-002·C-001 치수 리터럴 항목과 같은 성격).
- **항목**: ① 그리기 확장 소유 규칙을 세울지(①은 [2026-08-01] 프리미티브 소유 항목의 확장이다).
  ② State 계산 프로퍼티를 허용 사례로 규약에 적을지, 아니면 ViewModel private 헬퍼로 옮길지.
  ③ 화면 고유 치수를 토큰으로 올릴지 인정할지.
- **상태**: 미해결 (동작 결함 아님 — 규약 정합)
- **해소 메모**: ①은 [design-system](../architecture/design-system.md) "과도기" 절, ②는
  [state-management](../architecture/state-management.md) "UI State가 담는 것", ③은 [2026-08-11] 치수
  리터럴 항목과 같은 결정에 묶인다.

### [2026-08-16] 배경 변경이 캔버스 마감 상태를 보지 않는다 — 지난 캔버스도 계속 고쳐진다

- **ID**: OQ-P-189
- **출처**: 서버 `core/parfait/service/ChangeParfaitBackgroundService`(PR #103) — 그룹 멤버십과 파르페
  존재만 확인하고 `Parfait.status`를 읽지 않는다. `CLOSED`·`EMPTY` 파르페에도 배경이 저장된다.
  같은 도메인에 `PARFAIT_ALREADY_CLOSED`(409)가 있는데 그 코드에 닿는 공개 경로는 여전히 없다
  ([api/parfait.md](../api/parfait.md)). **토핑 네 엔드포인트도 마찬가지로 상태를 안 본다**(OQ-P-160 ②) —
  즉 이 delta는 새 결함을 만든 게 아니라 **"마감 후 편집을 아무도 막지 않는다"는 공백을 하나 더 늘렸다.**
- **항목**: ① 마감된 캔버스의 배경을 바꿀 수 있는 것이 의도인지(캔버스는 "그날의 기록"인데 배경만
  사후 수정 가능하다) — 서버팀 확인 대상이다. ② 의도가 아니라면 서버가 막을지, 앱이 C-301 진입을
  막을지. ③ 막는다면 어느 코드로 낼지(`PARFAIT_ALREADY_CLOSED`가 이미 있다).
- **상태**: 해소됨 (**2026-08-20 서버 `efbf98f`** — ① 의도가 아니었고, ②를 **서버가** 맡았고,
  ③은 `PARFAIT_ALREADY_CLOSED`로 정해졌다. 남은 것은 앱이 그 코드를 어떻게 보여줄지 → OQ-P-244)
  > ✅ **`ChangeParfaitBackgroundService`가 파르페를 찾은 직후 `status != ACTIVE`를 검사해 409로 끊는다**
  > (`fix: 마감된 파르페에 대한 편집 요청 거부`). 배경 값 해석보다 앞이라 `CLOSED` 캔버스에 잘못된 HEX를
  > 함께 보내도 `INVALID_BACKGROUND`가 아니라 이 코드가 온다. 토핑 네 엔드포인트도 같은 라운드에서
  > 같은 가드를 얻어(OQ-P-160 ②) **"마감 후 편집을 아무도 막지 않는다"는 공백이 통째로 닫혔다.**
  > 서버 커밋 메시지가 증상을 적는다 — 03시 회전 직후 쓰기가 200으로 성공하고도 뒤이은 `today` 조회가
  > 새 캔버스를 줘서 사용자에게는 편집이 사라진 것처럼 보였다 → [api/parfait.md](../api/parfait.md).
  > **앱이 택한 "길 치우기"는 그대로 두어도 된다** — 이제 서버 가드와 겹치는 이중 방어이고, 다른 진입
  > 경로가 생겨도 서버가 막는다.
  > 📌 **앱 표면이 붙으면서 "화면 책임"이 문서에서 코드 주석으로 내려왔다(2026-08-16, PR #266)** —
  > `ParfaitService`·`ParfaitRemoteDataSource`의 함수 KDoc이 "마감 캔버스도 바뀐다 · 막는 것은 화면
  > 책임"을 ⚠️로 달았다. **가드 코드는 없다** — 표면 라운드가 의도적으로 범위 밖에 뒀다
  > ([spec](../superpowers/specs/archive/2026-08-16-canvas-detail-background-api-service-layer.md) 결정 ⑤). 즉 ②가
  > 앱으로 정해지면 **C-301 진입 조건이 그 유일한 방어**가 되고, 지금은 경고만 있고 아무도 안 막는다.
  > 📌 **②에 앱 쪽 첫 답이 나왔다(2026-08-17, PR #279)** — C-001이 지난 캔버스를 볼 때 메뉴 액션
  > 두 개(토핑 추가·캔버스 편집)를 **갤러리에 저장·오늘의 파르페 가기로 갈아 끼워 진입점 자체를
  > 치운다.** UiState도 `todayCanvas`/`viewedCanvas`로 갈라 편집 대상이 언제나 오늘이 되게 했다.
  > **가드가 아니라 길 치우기**라는 점이 남는다 — 다른 진입 경로(딥링크·C-301 직접 진입)가 생기면
  > 다시 뚫리고, 서버는 여전히 아무것도 안 막는다.
- **해소 메모**: ②가 앱 쪽으로 정해지면 [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)에
  진입 조건으로 적는다. OQ-P-173과 같은 라운드에서 본다. 화면이 실제로 택한 방식은
  [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)에 있다.

### [2026-08-16] 배경 이미지가 참조 카운트를 올리지 않아 토핑을 지우면 배경이 깨진다

- **ID**: OQ-P-190
- **출처**: 서버 `ChangeParfaitBackgroundService`가 `ImageMetaQueryPort.findById`로 **읽기만** 하고
  `ImageMeta.increaseReferenceCount`를 부르지 않는다(PR #103). `image_meta.reference_count`의 증감
  경로는 토핑 배치(+1)·토핑 삭제(−1)뿐이고, `DeleteParfaitImageService`는 카운트가 0이 되면
  **S3 객체까지 지운다**([api/parfait-image.md](../api/parfait-image.md)). 같은 이미지를 배경으로 쓰고
  토핑으로도 올렸다가 그 토핑을 지우면 배경 URL이 죽는다. 배경만 설정한 이미지는 카운트가 0인 채로
  남아 **고아 정리 정책이 생기는 순간 지워질 후보**가 된다(OQ-P-107 계열).
- **항목**: ① 배경도 참조로 세는 것이 맞는지 — 맞다면 서버가 설정 시 +1, 교체·마감 시 −1을 해야 하고
  교체 시점 감소 지점이 지금 없다. ② 아니라면 배경 이미지를 별도 수명 규칙으로 둘지. ③ 앱이 배경과
  토핑에 같은 이미지를 쓰지 못하게 막을지(현재 계약상 막을 근거가 없다).
- **상태**: 미해결 (서버 소관 — **실사례가 생겼다**)
  > ⚠️ **배경 이미지가 실제로 올라가기 시작했다(2026-08-22, PR #329)** — C-301 확인이 고른 사진을
  > `ImageType.BACKGROUND`로 업로드하고 그 `imageId`로 배경을 바꾼다. 아래 메모가 "결선되면 그때 처음
  > 실제 사례가 생긴다"고 적어 둔 시점이 왔다. 지금 배경으로만 쓰인 이미지는 **`reference_count`가 0인
  > 채로 남고**, 배경을 다른 것으로 바꿔도 이전 이미지의 카운트를 내리는 경로가 없다. 아직 증상이
  > 없는 이유는 고아 정리 정책이 서버에 없기 때문이지, 앱이 막고 있어서가 아니다.
  > ③(같은 이미지를 배경·토핑에 쓰지 못하게 막을지)도 그대로 열려 있다 — 앱은 두 경로에서 **같은
  > 갤러리 사진을 고를 수 있고** 막을 근거가 계약에 없다.
  > 📌 **앱 표면에는 경고만 실렸다(2026-08-16, PR #266)** — DataSource·Service KDoc이 참조 카운트 함정을
  > ⚠️로 옮겨 적었다.
- **해소 메모**: 정해지면 [api/image.md](../api/image.md)·[api/parfait.md](../api/parfait.md)의
  수명 서술을 함께 맞춘다.

### [2026-08-16] 배경 이미지를 id로 보내고 URL로 돌려받는다 — 앱이 현재 배경의 이미지를 되짚을 수 없다

- **ID**: OQ-P-191
- **출처**: 서버 `ChangeParfaitBackgroundRequest`(`imageId`)와 `BackgroundResponse`(`type`·`value`) —
  `type = IMAGE`일 때 저장·응답되는 `value`는 **`ImageMeta.url`**이라 요청 단위(id)와 응답 단위(URL)가
  다르다(PR #103). 조회 응답(`GetTodayParfaitResponse.background`)도 같은 타입이라 마찬가지다. 앱
  `CanvasBackground`는 URL만 받게 되고, "지금 배경인 이미지를 그대로 다시 보내기"를 하려면 id를
  따로 들고 있어야 한다. 덧붙여 이 요청은 **조건부 필수의 첫 사례**다 — `type`에 따라 `value` 또는
  `imageId`가 필수인데 둘 다 널 허용이라 타입·OpenAPI 스키마 어디에도 안 드러난다
  ([api/conventions.md](../api/conventions.md)).
- **항목**: ① 응답에 `imageId`를 함께 실을지(서버 변경) — 아니면 앱이 편집 세션 안에서만 id를 들고
  가는 것으로 충분한지. ② 앱 요청 모델을 sealed로 만들어 조건부 필수를 타입으로 세울지, 서버 형태
  그대로 널 허용 세 필드로 둘지([data-layer](../architecture/data-layer.md)의 "서버 형태를 따른다"와
  충돌한다).
- **상태**: **부분 해소** (2026-08-16, PR #266 — **② 결정됨**, ①은 서버 소관으로 잔존)
  > ✅ **②는 sealed로 갔다** — wire DTO(`ChangeParfaitBackgroundRequest`)는 서버 형태 그대로 평면·널 허용을
  > 유지하고, `:domain`의 쓰기 전용 sealed `CanvasBackgroundEdit`(`Color(hex)`/`Image(imageId)`)이 조건부
  > 필수를 **컴파일에서** 막는다. 펴는 일은 매퍼(`toRequest()`)가 한다. "서버 형태를 따른다"와 충돌하지
  > 않는 이유는 그 규약이 **DTO에 대한 것**이고 domain은 좁게 잡는다는 규약이 이미 짝으로 있기 때문이다
  > (선례 `ToppingTransform.toPlaceRequest`·`ToppingBorder.toUpdateBorderRequest`) →
  > [data-layer](../architecture/data-layer.md) "요청 방향 변환"·[api/conventions.md](../api/conventions.md).
  > ①(응답에 `imageId`를 실을지)은 서버 변경이라 그대로 열려 있다. 다만 앱이 **읽기 모델을 되돌려 보내지
  > 않는 구조**가 돼서, 편집 세션 안에서 방금 고른 `imageId`를 들고 가면 왕복은 성립한다 — 다시 켰을 때
  > "지금 배경이 어느 이미지인지"를 되짚는 문제만 남는다.
- **해소 메모**: ②는 이 도메인의 **첫 요청 DTO** 라운드에서 위와 같이 정해졌다
  ([spec](../superpowers/specs/archive/2026-08-16-canvas-detail-background-api-service-layer.md) 결정 ①②).
  ①이 정해지면 [api/parfait.md](../api/parfait.md) 응답 필드 표를 함께 고친다.

### [2026-08-16] 파르페 상세 조회가 상태를 거르지 않아 오늘 캔버스를 얻는 경로가 둘이 됐다

- **ID**: OQ-P-192
- **출처**: 서버 `GetParfaitDetailService`(PR #96) — id로 찾고 `status`를 보지 않아 `ACTIVE`인 오늘
  캔버스도 그대로 조회된다. 커밋 메시지는 "이전 파르페 상세"라고 적지만 계약은 과거 전용이 아니다.
  응답 타입도 `today`와 **같은 클래스**다. 차이는 부작용뿐이다 — `today`는
  `EnsureActiveCanvasUseCase`로 **행을 만들고**(OQ-P-160), 상세는 `readOnly`다
  ([api/parfait.md](../api/parfait.md)).
- **항목**: ① 앱이 C-001을 그릴 때 어느 경로를 쓸지 — `today`는 캔버스를 보장하지만 부작용이 있고,
  상세는 부작용이 없지만 `parfaitId`를 먼저 알아야 한다(과거 목록 또는 이전 `today` 응답에서 온다).
  ② 캘린더에서 "오늘"을 고른 경우를 상세로 보낼지 `today`로 보낼지 — 같은 화면이 두 경로를 타면
  부작용 유무가 사용자 조작에 따라 갈린다.
- **상태**: 미해결 (앱 소비처 0건 — C-001·C-201 결선 라운드에서 판정)
  > 📌 **두 경로 다 앱 표면을 얻었고, 그 과정에서 경고의 소유가 옮겨졌다(2026-08-16, PR #266)** —
  > 상세 조회 응답이 오늘 조회와 같은 클래스라 **`TodayCanvasVO`가 `CanvasVO`로 개명**됐다(한 타입이
  > 두 경로를 담는다). 그 결과 "이 값을 얻는 조회는 서버에 캔버스 행을 만든다"는 경고가 **타입 KDoc에서
  > 함수 KDoc으로** 내려갔다 — 오늘 조회만 만들고 상세는 안 만들기 때문이다. **호출부는 반환 타입만
  > 봐서는 부작용 유무를 알 수 없다**는 뜻이라, ①②의 선택이 코드에 남을 자리가 함수 이름뿐이다.
- **해소 메모**: OQ-P-160(부작용 있는 GET)과 한 결정이다. 정해지면
  [c001 스펙](../superpowers/specs/archive/2026-08-12-c001-canvas-main.md)·[c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md)에
  조회 경로를 명시한다.

### [2026-08-16] 배경 변경 성공이 `null`로 접힐 수 있다 — "저장됐는데 그릴 수 없다"가 성공과 구분되지 않는다

- **ID**: OQ-P-193
- **출처**: `data/source/parfait/remote/ParfaitRemoteDataSource#changeCanvasBackground`가
  `Result<CanvasBackground?>`를 반환한다(PR #266) — 서버 응답의 `background`는 비널인데, 매퍼
  (`ChangeParfaitBackgroundResponse.toCanvasBackground()`)가 **미지 `type`을 `null`로 접는 조회 규칙을
  그대로 재사용**하기 때문이다. 그래서 호출부가 받는 성공값 `null`의 뜻은 "미설정"이 아니라
  **"저장은 됐는데 앱이 그릴 수 없다"**이다. 조회 쪽 `null`(미설정)과 **같은 값이 다른 뜻**을 갖는다.
- **항목**: ① 화면이 이 경우를 실패처럼 다뤄야 하는지(저장은 성공했으므로 되돌리면 안 되고, 그렇다고
  그릴 값도 없다). ② 아니면 반환을 비널로 두고 미지 type을 실패(`ApiException`)로 올릴지 —
  조회와 규칙이 갈리지만 뜻은 정확해진다. ③ 폴백 발동을 로그로 남길지(OQ-P-181 ①과 같은 결정).
- **상태**: **부분 해소** (2026-08-22, PR #329 — **①② 결정됨**, ③(폴백 관측)만 잔존)
  > ✅ **①은 "실패로 다루지 않는다"**로 닫혔다 — 저장은 성공했으므로 화면을 막을 이유가 없고,
  > 그릴 값이 없으면 **사용자가 고른 값**으로 그린다(`fallbackBackground`). 되돌리지도, 실패 토스트를
  > 띄우지도 않는다.
  > ✅ **②(반환을 비널로 두고 미지 type을 실패로 올리기)는 기각**됐다 — 조회와 규칙을 갈라 놓는 값이
  > 저장 성공을 실패로 보이게 하는 대가보다 작다는 판단이다.
  > ⚠️ **다만 이 결정은 아직 아무 화면 결과도 바꾸지 않는다** — 확인 이펙트를 받은 Route가 실린 배경을
  > 버리고 캔버스 메인이 재조회로 그리기 때문이다(OQ-P-194 ③). 즉 널 폴백은 **선반영된 방어**이고,
  > 이펙트 값을 실제로 쓰는 화면이 생기는 날 처음 동작한다.
  > ③(폴백 발동을 로그로 남길지)은 그대로다 — 이 자리도 조용히 접힌다(OQ-P-181).
- **해소 메모**: 정해지면 [api/parfait.md](../api/parfait.md) Android 매핑의 반환 타입 설명과
  [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)에 함께 적는다.
  OQ-P-173(배경 연동)·OQ-P-181(폴백 관측)과 한 라운드에서 본다.

### [2026-08-16] 배경 표현이 셋으로 갈렸다 — 읽기 VO·쓰기 VO·화면 타입 사이 변환 주체가 없다

- **ID**: OQ-P-194
- **출처**: 배경을 가리키는 타입이 develop에 셋이다 — `domain/model/canvas/CanvasBackground`(읽기,
  `Color(value)`/`Image(url)`) · `CanvasBackgroundEdit`(쓰기, `Color(hex)`/`Image(imageId)`, PR #266 신설) ·
  `feature/groups/canvas/impl`의 화면 타입 `YGCanvasBackground`(C-301 이펙트가 싣는 값, PR #231).
  **읽기/쓰기가 갈린 것은 서버 계약이 비대칭이라 근거가 분명하다**(쓸 때 `imageId`, 읽을 때 URL).
  문제는 셋 사이를 잇는 코드가 아직 하나도 없다는 것이다 — C-301이 고른 값은 화면 타입에 머물고,
  그 값에서 `CanvasBackgroundEdit`를 만들려면 이미지 배경의 경우 **업로드·확인을 거쳐 `imageId`를
  얻는 단계**가 먼저 필요한데 그 경로도 아직 없다([api/image.md](../api/image.md) 2단계 업로드).
- **항목**: ① 화면 타입을 없애고 도메인 타입을 화면까지 쓸지, 아니면 매핑 계층을 둘지
  (C-301 State가 Compose `Color`를 들고 있는 것과 같은 쟁점이다 — OQ-P-176). ② 이미지 배경 저장이
  "갤러리·카메라 선택 → 업로드 → confirm → `changeCanvasBackground`" 네 단계인데 그 조율을 UseCase가
  할지 화면이 할지. ③ 저장 후 C-001이 그릴 값을 응답 echo에서 받을지 재조회할지.
- **상태**: **부분 해소** (2026-08-22, PR #329 — **②③ 결정됨**, ①(화면 타입 존폐)만 잔존)
  > ✅ **②(네 단계의 조율 주체)는 UseCase 둘로 갈라졌다** — `UploadImageUseCase(uri, imageType)`가
  > "캐시 복사 → 발급 → S3 PUT → confirm"을 닫아 `imageId`를 주고, `ChangeCanvasBackgroundUseCase`가
  > 그 값으로 PATCH를 부른다. 화면은 **순서를 알지 않고** 둘을 이어 부르기만 한다. 업로드 안에서
  > 캐시 복사 단계가 따로 생긴 이유는 단위 차이다 — 화면이 쥔 것은 `content://`인데 업로드는 파일
  > 절대경로만 받는다(`ImageFileRepository`).
  > ✅ **③(저장 후 C-001이 그릴 값)은 재조회**로 정해졌다 — 응답 echo를 실어 나르지 않는다.
  > ⚠️ **①은 그대로다** — `CanvasBGEditUiState`는 여전히 Compose `Color`를, `ConfirmBackground`는
  > `YGCanvasBackground`를 든다. 도메인 값은 경계에서만 만들어진다(`Color.toRgbHex()`). 배경에
  > 계약이 생겼으므로 "도메인 의미가 없어서"라는 원래 근거는 사라졌고, 남은 근거는 **화면이 도메인
  > 타입을 들 이유가 아직 없다**는 것뿐이다 → [state-management](../architecture/state-management.md).
- **해소 메모**: 배경 편집 결선 라운드에서 ①②③이 한 번에 정해진다 — 정하면
  [c301 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)·[state-management](../architecture/state-management.md)에
  반영한다. OQ-P-173·OQ-P-193과 같은 자리다.

### [2026-08-16] 오프라인으로 앱을 켜면 자동로그인을 포기한다 — 토큰은 남고 라우팅만 로그인으로 간다

- **ID**: OQ-P-195
- **출처**: `domain/usecase/session/BootstrapSessionUseCase`(PR #263) — 부트스트랩은 목적지를 실패
  종류와 무관하게 `SessionBootstrap.ToLogin` 하나로 내고, 갈리는 것은 정리 범위뿐이다(인증 거절만
  파기). 즉 **네트워크 실패에도 토큰은 남지만 화면은 로그인으로 간다.** 같은 라운드의
  `TokenAuthenticator`가 "네트워크 실패에는 토큰을 유지한다"고 결정한 것과 처분은 같은데
  **사용자가 보는 결과는 다르다** — 이미 로그인한 사용자가 지하철에서 앱을 켜면 로그인 화면을 본다.
  그렇게 만든 이유는 그룹 목록을 캐시로 그릴 수단이 없어서다(G-001은 매 진입 서버 조회).
- **항목**: ① "토큰이 있고 네트워크만 실패한 경우"를 `ToGroupList`로 돌릴지 — 돌리려면 목록이 빈
  화면이 아니라 무언가를 그릴 수 있어야 한다. ② 아니면 로그인 화면이 "연결을 확인해 주세요"를
  구분해 보여줄지(지금은 처음 쓰는 사람과 오프라인 복귀 사용자가 같은 화면을 본다).
- **상태**: 미해결 (설계 시점에 알고 남긴 선택)
- **해소 메모**: 그룹 목록 캐시가 생기는 라운드에서 재검토한다 — 정하면
  [user-info-ssot 스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md) 부트스트랩 절과
  [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인"에 반영한다.

### [2026-08-16] 계정 정보가 낡거나 비어 있어도 사용자에게 알릴 수단이 없다

- **ID**: OQ-P-196
- **출처**: `MemberRepositoryImpl`·`AppSettingViewModel`·`AccountInfoViewModel`(PR #263) —
  ① `refreshMyAccount()`가 실패하면 로컬을 **유지**한다(낡은 값이라도 지우지 않는다는 결정). 그런데
  화면에는 "이 값이 낡았다"를 말할 자리가 없어 사용자는 다른 기기에서 바꾼 닉네임이 안 보이는 이유를
  알 수 없다. ② 반대로 SSoT가 비어 있으면(최초 로그인 전·복호화 실패 후) 화면은 `null`을 로딩으로
  다뤄 S-002 입력 필드를 비활성으로 둔다 — **채워질 계기가 그 화면에 없어서**(구독만 한다) 갱신이
  실패한 상태로 들어오면 비활성이 그대로 남는다. 재시도 수단도 표시도 없다.
- **항목**: ① 낡음 표시를 둘지(마지막 갱신 시각·조용한 재시도 중 무엇으로). ② 비어 있는 상태에
  화면이 재시도 진입점을 가질지, 아니면 부트스트랩·로그인 두 시점에만 채워지는 지금을 유지할지.
- **상태**: 미해결
- **해소 메모**: 정하면 [ADR-0022](../adr/0022-user-info-local-ssot.md) "영향"과
  [state-management](../architecture/state-management.md) SSoT 구독 절에 반영한다. 공통 로딩·에러 표현
  라운드(`ygscaffold-v2-common-loading-error` 스펙)와 같은 자리에서 볼 수 있다.

### [2026-08-16] SSoT가 생겼는데 G-001 목록 닉네임만 여전히 mock이다

- **ID**: OQ-P-197
- **출처**: `feature/groups/list/impl` `GroupListViewModel`의 `GroupListUiState.nickName` 기본값이
  하드코딩 문자열 그대로다(PR #263 범위 밖으로 의도적으로 남겼다). S-001·S-002는 mock을 걷고
  `GetMyAccountFlowUseCase`를 구독하는데 **한 화면만 다른 이름을 보여줄 수 있는 상태**다 — 사용자가
  S-002에서 닉네임을 바꾸면 설정 화면은 즉시 바뀌고 목록만 옛 문자열을 계속 그린다.
- **항목**: 이 화면을 SSoT에 붙일 라운드를 정한다. 붙이는 비용은 UseCase 구독 한 줄이라 작지만,
  G-001이 그리는 값이 **전역 닉네임인지 그룹 닉네임인지**를 먼저 확정해야 한다 — 서버는 둘을 다른
  컬럼으로 들고 전역 닉네임 변경이 그룹 닉네임을 바꾸지 않는다([api/member.md](../api/member.md)).
- **상태**: 해소됨 (PR #312 develop 머지 2026-08-20)
- **해소 메모**: 붙일 때 [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)과
  [api/member.md](../api/member.md) Android 매핑을 함께 갱신한다.
  > ✅ **전역 닉네임으로 확정됐다(2026-08-20, PR #312)** — `GroupListViewModel`이 생성 시점에
  > `GetMyAccountFlowUseCase`를 구독하고 mock 리터럴이 사라졌다. 구독을 `Enter`가 아니라 `init`에
  > 둔 근거는 **목록과 성격이 다르다**는 것이다 — 계정 SSoT가 밀어 주는 값이라 화면에 설 때마다
  > 끌어올 이유가 없고, 갱신은 로그인·가입·스플래시·닉네임 변경 쪽이 이미 맡는다.
  > 이 값은 **화면에 그려지지 않는다** — A-005로 넘기는 인자로만 쓰이므로 "한 화면만 다른 이름을
  > 보여준다"는 증상은 애초에 눈에 보이는 것이 아니었고, 실제 위험이던 "mock 이름이 그룹 생성
  > 요청으로 서버에 나간다"가 닫혔다.
  > 같은 라운드가 타입을 `String`에서 `String?`으로 넓혀 **아직 못 받은 상태를 값으로 표현**하고,
  > 이펙트 `NavigateToCreateGroup`이 닉네임을 실어 보낸다(Route가 `uiState`를 다시 읽지 않는다).
  > ⚠️ 그 대가가 새 미결이다 — 닉네임이 아직 없으면 그룹 만들기가 **조용히 안 열린다**(OQ-P-253).

### [2026-08-16] 같은 `MEMBER_NOT_FOUND`에 두 소비자의 처분이 갈린다 — 화면은 표시만 하고 세션은 그대로다

- **ID**: OQ-P-198
- **출처**: `BootstrapSessionUseCase`는 `ServerErrorCode.Member.MEMBER_NOT_FOUND`를 **세션 사망**으로
  보고 `LogoutUseCase`로 토큰·계정 정보를 지우는데, 같은 코드를 S-002
  (`AccountInfoViewModel`·`GlobalNicknameError.ACCOUNT_GONE`)는 **문구로 표시만** 한다(PR #263).
  의도된 갈림이다 — 강제 로그아웃 발신 주체를 `TokenAuthenticator` 하나로 두려고 화면이 세션을
  파괴하는 경로를 새로 열지 않았고, 죽은 세션은 다음 앱 진입의 부트스트랩이 걷어낸다. 대가는
  **탈퇴된 계정으로 앱을 계속 쓰는 창**이다: 그 화면에서 나가도 로그인 상태가 유지되고, 이후 호출은
  각자 실패하며, 사용자는 이유를 모른다.
- **항목**: ① 화면이 세션 사망을 감지했을 때 무엇을 할지 — 아무것도 안 함(현재) / 강제 로그아웃
  이벤트를 쏠 수 있는 단일 창구를 도메인에 둘지. ② 둘 이상의 화면이 같은 판단을 하게 되면 그 규칙을
  어디에 둘지(지금은 부트스트랩 안의 `private` 판정이다).
- **상태**: 미해결 (설계 시점에 알고 남긴 선택)
- **해소 메모**: 정하면 [ADR-0021](../adr/0021-token-refresh-forced-logout.md) 세션 종료 경로와
  [ADR-0022](../adr/0022-user-info-local-ssot.md) 세션 정리 절에 함께 반영한다. OQ-P-185(유실 창)와
  같은 자리다.

### [2026-08-16] C-301 토핑 탭이 mock 목록을 고치고, 고친 결과는 화면을 나가면 사라진다

- **ID**: OQ-P-199
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditViewModel#loadMockToppings`·`CanvasBGEditIntent`
  (PR #264) — 토핑 4개가 디자인시스템 템플릿 이미지와 하드코딩 좌표로 만들어지고(`TODO` 주석 있음),
  이동·크기·회전·삭제·테두리 재편집이 전부 `UiState` 안에서만 일어난다. 확인 버튼은 여전히
  `ConfirmBackground`(배경)만 싣는다. 서버에는 이미 파르페 상세 조회(`GET .../parfaits/{parfaitId}`)·
  토핑 테두리 수정·토핑 삭제 표면이 `:data`까지 들어와 있고 **소비처가 0건**이다
  ([api/parfait-image.md](../api/parfait-image.md)·[api/parfait.md](../api/parfait.md)).
  배경이 겪은 것(OQ-P-174)과 같은 미완이 토핑에서 반복됐다.
- **항목**: ① 토핑 목록 조회를 어느 시점에 붙일지(C-001 캔버스와 C-301 편집이 같은 조회를 쓰는지).
  ② 편집 결과의 저장 단위 — 확인 한 번에 위치·크기·회전을 **일괄 전송**할지, 조작마다 보낼지.
  서버 토핑 수정 표면은 **테두리만** 받고 좌표·배율·회전 갱신 경로가 계약에 없다. ③ 삭제를 확인
  모달 시점에 즉시 반영할지, 확인 버튼까지 미룰지(지금은 즉시 목록에서 뺀다).
- **상태**: **해소됨** (2026-08-22 PR #329로 ①, 2026-08-23 PR #335로 ③, 같은 날 PR #336으로 **②**)
  > ✅ **②가 닫혔다(2026-08-23, PR #336) — 확인 한 번에 일괄이고, 바뀐 것만 보낸다.**
  > ViewModel이 조회 응답 스냅샷(`confirmedToppings`)을 따로 들고 확인 시점에 `state.toppings`와
  > 대조해, 위치·배율·각도 중 하나라도 달라진 토핑만 `UpdateToppingUseCase`로 PATCH 한다. 토핑들
  > 끼리는 `async` + `awaitAll`로 **병렬**이고(순차면 확인 버튼이 바뀐 토핑 수만큼 느려진다) 전부
  > 끝난 뒤에야 배경 저장이 이어진다 — 둘을 얽으면 한쪽만 실패한 경우를 갈라 다뤄야 해서다.
  > `positionZ`는 안 보낸다(부분 병합이라 서버 겹침 순서가 유지되고, 앱에 z 조작 경로가 없다).
  > ⚠️ **닫히면서 남긴 것이 셋이다** — 실패가 화면에 안 닿는데 확인은 성공한다(OQ-P-275),
  > 테두리 재편집만 여전히 안 나간다(OQ-P-276), 범위 검증 없는 두 축이 그대로 요청 값이 된다
  > (OQ-P-271·OQ-P-241).
  > ✅ **①이 닫혔다 — 편집 탭도 같은 서버 조회를 쓴다.** `loadMockToppings()`가 사라지고
  > `GetTodayParfaitUseCase` 응답을 그린다(진입 시 한 번 더 부르는 이유는 편집을 여는 사이 다른
  > 멤버가 올린 토핑까지 그려야 해서다). 좌표계도 함께 맞춰졌다 — `CanvasToppingItem`이 Dp 오프셋을
  > 버리고 **Canvas-Area 대비 0~1 중심점**을 들고, 배치 규칙 셋이
  > `component/CanvasToppingLayer.kt`에서 `util/ToppingGeometry.kt`로 올라가 **캔버스 메인·편집 탭·배치
  > 화면이 같은 값을 본다.** 같은 캔버스가 두 화면에서 다르게 보이던 상태가 끝났다.
  > ⚠️ **②③은 그대로다** — 이동·크기·회전은 여전히 `UiState`에서 끝나고 확인 버튼은 배경만 저장한다.
  > 삭제는 `TODO(#271 대기)`로 화면에서만 사라지고, 토핑 편집 진입은 `TODO(#274 대기)`다(서버 토핑은
  > https 주소라 편집 화면이 `ContentResolver`로 열지 못한다). 서버 토핑 수정 표면이 **테두리만** 받고
  > 좌표·배율·회전 갱신 경로가 계약에 없다는 사실도 그대로다.
  > ⚠️ **새로 생긴 위험은 소유 판정이다** — `isMine`이 상수를 벗어나 계정 id와 그룹 멤버십 행 id를
  > 견주는데 두 값은 축이 다르다(OQ-P-250). 캔버스 메인에서는 Spotlight 갈래가 갈릴 뿐이지만
  > **이 화면에서는 그 판정이 곧 편집 게이트**다.
  > ⚠️ **①의 답이 절반만 나오면서 출처가 둘로 갈렸다(2026-08-17, PR #268)** — C-001 캔버스는 이제
  > 서버 조회(`GetTodayParfaitUseCase`·`GetParfaitDetailUseCase`)로 토핑을 그리는데, **C-301 편집 탭은
  > 여전히 `loadMockToppings()`를 고친다.** 즉 같은 앱 안에서 같은 캔버스의 토핑이 **한쪽은 서버,
  > 한쪽은 mock**이다. ①의 원래 물음("C-001과 C-301이 같은 조회를 쓰는지")은 이제 **"C-301을 그
  > 조회로 옮기는 일"**로 좁혀졌고, 옮기면 ②(저장 단위)가 곧바로 걸린다 — 서버 토핑 수정 표면은
  > **테두리만** 받고 좌표·배율·회전 갱신 경로가 계약에 없다는 사실이 그대로다.
  > ✅ **③이 닫혔다(2026-08-23, PR #335)** — **확인 모달 시점에 즉시**다. "삭제하기"가 곧
  > `DELETE .../images/{parfaitImageId}`이고, **성공해야** 화면 목록에서 뺀다(서버에 반영되지 않은
  > 것을 화면에서 지우지 않는다). 부작용은 이 화면의 두 파괴적 조작이 갈린 것이다 — **"그만두기"로
  > 나가도 지운 토핑은 돌아오지 않는데** 이동·크기·회전은 여전히 나가면 사라진다. 실패 처분은 함께
  > 정해지지 않아 로그 한 줄로 남았다(OQ-P-270).
  > 📌 **②의 전제 하나가 낡아 있었다** — "서버 토핑 수정 표면은 **테두리만** 받고 좌표·배율·회전
  > 갱신 경로가 계약에 없다"는 서술은 2026-08-15 PR #250 이후로 사실이 아니다.
  > `PATCH .../images/{parfaitImageId}`가 `positionX`·`positionY`·`positionZ`·`scale`·`rotation`을
  > 부분 병합으로 받고 앱 `:data`에도 `updateTopping`으로 들어와 있다
  > ([api/parfait-image.md](../api/parfait-image.md)). 그래서 ②에 남은 물음은 "계약이 있는가"가
  > 아니라 **"확인 한 번에 일괄 전송할지, 조작마다 보낼지"** 하나다.
- **해소 메모**: ②는 서버 계약 확장이 필요하면 [api/parfait-image.md](../api/parfait-image.md)에
  먼저 반영한다. 정해지면 [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)
  드리프트 1·2와 [c301 배경 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md) 드리프트 1,
  그리고 [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md) 드리프트 2를
  함께 정리한다.

### [2026-08-16] C-106 배치 규격이 코드 어디에도 없다 — 토핑을 새로 얹는 경로 자체가 없다

- **ID**: OQ-P-200
- **출처**: 위키 [[C-106-토핑-배치-정책-v0.1]]([[토핑]] "캔버스 배치 규격")은 초기 크기(더 긴 쪽 =
  캔버스 가로 40%)·초기 위치(정중앙)·최소 터치 방어(짧은 쪽 48px 하한)를 확정 규칙으로 적는데,
  `CanvasBGEditScreen#rememberToppingBaseSize`는 이미지 `intrinsicSize`를 그대로 dp로 읽고 배율 1로
  그린다(PR #264). 이탈 허용 + 클리핑만 `clipToBounds()`로 일치한다. C-103 편집을 마치고 오는
  `NavKeyCanvasMove(imageUri)` 목적지는 있으나 **캔버스에 얹는 경로가 결선되지 않아** 당장 어긋나는
  화면은 없다.
- **항목**: ① 초기 배치 계산을 어디에 둘지 — 화면·`domain`·`core:designsystem` 중 어디가 소유하는가
  (캔버스 폭이 필요하므로 측정 결과에 붙는다). ② 최소 터치 방어의 단위(정책은 px, 코드 좌표계는 dp)를
  확정. ③ 이 규격을 새 배치에만 적용할지, 서버에서 받은 기존 토핑에도 적용할지.
- **상태**: 해소됨 (**규격 넷 전부 코드에 들어왔다** — 2026-08-19, PR #290. 단 이 배치가 저장으로
  이어지지 않는 것은 OQ-P-209·OQ-P-238이 이어받는다)
  > ✅ **남은 셋이 한꺼번에 들어왔다(2026-08-19, PR #290)** — C-106 배치 화면이 생기면서
  > `CanvasToppingPlaceViewModel#applyInitialPlacementIfNeeded`가 **정중앙**(`(canvas - base) / 2`)과
  > **48 하한**(`MIN_TOPPING_SHORT_SIDE`로 역산한 배율과 40% 배율 중 큰 쪽)을 계산하고, 좌표를
  > clamp하지 않아 이탈 허용도 그대로다. 항목별 답:
  > - ① **초기 배치 계산은 ViewModel이 소유**한다 — 캔버스 실측과 토핑 원본 크기가 서로 다른 시점에
  >   비동기로 오므로 화면이 인텐트 둘(`OnCanvasMeasured`·`OnToppingBaseSizeMeasured`)로 올려 주고,
  >   어느 쪽이 먼저 오든 다시 시도하되 **사용자가 한 번 손대면 멈춘다**(`hasUserAdjustedPlacement`).
  >   `domain`·`core:designsystem` 어느 쪽도 아니다.
  > - ② **단위는 dp로 굳었다.** 정책 문서는 여전히 px이고, C-104 브러시 굵기가 이미 같은 방식으로
  >   갈렸다(값은 같고 단위만 다르다).
  > - ③ **양쪽 다에 적용된다.** 40% 상수를 `internal`로 열어 읽기(`CanvasToppingLayer`)와
  >   쓰기(신규 배치)가 **같은 값을 공유**한다.
  >
  > 📌 **직전 기록 — 규격 셋 중 하나가 먼저 들어왔다(2026-08-17, PR #268)** — `CanvasToppingLayer`의
  > `TOPPING_BASE_LONG_SIDE_RATIO`가 **`scale = 1.0`의 뜻을 "긴 변이 Canvas-Area 너비의 40%"로** 못
  > 박았다(정사각 박스 + `ContentScale.Fit`이라 원본 크기를 몰라도 성립한다). 이탈 허용 + 클리핑도
  > 그대로 일치한다. **다만 이것은 서버에서 받은 배치를 그리는 규칙**이고, **초기 위치(정중앙)·최소
  > 터치 방어(48px)는 여전히 코드에 없다** — 새로 얹는 경로 자체가 없기 때문이다(OQ-P-209).
  > ③(새 배치에만 적용할지, 받은 토핑에도 적용할지)은 **받은 토핑에 적용하는 쪽으로 먼저 굳었다**:
  > 서버가 `scale`의 단위를 말하지 않아 그 해석이 곧 40% 규칙이 됐다(OQ-P-207).
- **해소 메모**: 규격 자체는 [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md)
  정책 대조 표가 정본이다. **② 단위(px↔dp)는 정책 문서 쪽이 아직 안 맞았고**, 정책이 다루지 않는
  회전·리사이즈 한계를 코드가 새로 정한 것은 OQ-P-241로 갈렸다.

### [2026-08-16] 편집하러 간 토핑이 누구인지를 Route의 `rememberSaveable`이 기억한다

- **ID**: OQ-P-201
- **출처**: `CanvasBGEditRoute#editingToppingId`(PR #264) — 테두리 재편집 왕복은
  `ResultEventBus`로 결과만 돌려주므로 "어느 토핑이었나"를 받는 쪽이 따로 들고 있어야 하는데,
  그 상태가 ViewModel이 아니라 Route의 `rememberSaveable`에 산다. ViewModel은 자기가 무엇을 편집하러
  보냈는지 모르고, 결과가 왔을 때만 인텐트 인자로 전해 듣는다. `NavKeyToppingEdit`가 왕복 대상 id를
  싣지 않는 것도 같은 원인이다(그 키는 C-103 확인 화면이 먼저 쓰던 계약이다).
- **항목**: ① 왕복 대상 식별을 어디에 둘지 — ViewModel State(`editingToppingId`)로 올릴지,
  `ToppingEditResult`에 요청 식별자를 실어 되돌려받을지, NavKey에 태울지. ② 같은 패턴이 다른 화면에도
  생기면(결과 왕복이 목록의 한 항목을 겨냥할 때) 관용구로 굳힐지.
- **상태**: 미해결
- **해소 메모**: [state-management](../architecture/state-management.md) "화면 상태는 ViewModel이
  소유" 항목과 [navigation-flow](../architecture/navigation-flow.md) 결과 왕복 절에 함께 반영한다.

### [2026-08-16] 편집 모드의 남의 토핑 취급이 C-202 정책과 다르고, 드래그 핸들에 대체 수단이 없다

- **ID**: OQ-P-202
- **출처**: `CanvasBGEditScreen`(PR #264) — 토핑 탭은 남의 토핑을 딤 **아래**에 그리고 그 위 딤이
  탭을 받아 **선택 해제**만 한다. 위키 [[C-202-토핑-편집자-확인-규칙-v0.1]]([[토핑-spotlight]])은
  타인 토핑 탭에 Spotlight 강조 + 작성자 Toast를, 본인 토핑 탭에 C-305 편집 진입을 규정한다.
  코드는 본인 토핑 탭이 곧 편집이 아니라 **선택**이고 편집은 모서리 버튼이다. 또 크기조절·회전 핸들이
  `YGCircleButton(onClick = {})` + `Modifier.dragBy`라 접근성 서비스에는 눌리는 버튼으로 노출되지만
  눌러도 아무 일이 없고 드래그의 대체 조작이 없다.
- **항목**: ① C-202가 캔버스 상세(C-001) 전용인지, 편집 모드에도 적용되는지 — 적용된다면 편집 중
  타인 토핑 탭의 규칙을 정책에 명시해야 한다(위키 소관). ② 본인 토핑의 "탭 = 편집 진입" vs
  "탭 = 선택 후 버튼"을 정책 쪽에 맞출지 코드 쪽으로 정책을 고칠지. ③ 드래그 조작의 접근성 대체 수단
  (증분 버튼·semantics 커스텀 액션)을 둘지.
- **상태**: 미해결 (**③은 화면 둘로 번졌다** — 2026-08-19)
  > 📌 **핸들이 공용 컴포저블이 되면서 문제도 같이 옮겨 갔다(2026-08-19, PR #290)** — 같은 조합을 감싼
  > `ToppingDragHandleButton`이 `groups/canvas/impl`의 `component/`로 올라가 C-301 편집 탭과 C-106 배치
  > 화면이 공유한다. **공용화는 접근성을 고치지 않았다** — 여전히 `onClick = {}` + `Modifier.dragBy`라
  > 스크린리더에는 눌리는 버튼인데 눌러도 아무 일이 없고, 이제 그런 자리가 두 화면에 있다.
  > 고칠 자리가 한 곳으로 모인 것은 이득이다.
- **해소 메모**: ①②는 위키 [[open-questions]]의 "에딧 모드 삭제 비고 vs C-301~C-306 잔존" 미결과
  같은 자리다. 정하면
  [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) 정책 대조 표를 갱신한다.

### [2026-08-16] 토핑 라운드가 규약 이탈 셋을 함께 들였다 — 점선 직접 그리기·클릭 규약·치수 리터럴

- **ID**: OQ-P-203
- **출처**: `CanvasBGEditScreen`·`util/ToppingGeometry.kt`(PR #264) — ① 선택 스트로크를
  `core:designsystem`의 `dashedBorder()`가 아니라 화면이 `drawBehind` + `dashPathEffect`로 직접 그린다
  (회전을 얹어야 해 형태가 달랐다). ② 토핑·딤이 `clickable(indication = null)`을 직접 쓴다
  (`clickableYG` 미사용, 갤러리 그리드·배경 팔레트와 같은 부류). ③ 탭별 상하 패딩·스트로크 굵기·점선
  간격·버튼 시각 반지름·모서리 간격이 토큰 밖 리터럴이고, 코드 주석이 "공통에 없음"이라고 자인한다.
  같은 라운드가 배치·제스처 확장 2종은 규약대로 `core:util:android` `extension/`으로 올렸다.
- **항목**: ① 회전 가능한 점선 테두리를 `dashedBorder()` 확장으로 흡수할지, 편집 전용 그리기로 둘지.
  ② ~~인터랙션이 "탭 아님"(드래그·선택 토글)일 때도 `clickableYG` 관용구를 강제할지.~~
  → ✅ **해소(2026-08-17, 이관 #284)**. 토핑·딤이 `clickableYGNoRipple`로 이관돼 관용구를 탄다 —
  드래그·선택 토글도 예외로 두지 않는다(무리플이라 시각은 그대로, 스로틀만 얹힌다).
  ③ 편집 화면용 치수(60·14·2·7.5·9·7dp)를 토큰 스케일에 올릴지 — A-002·C-201이 남긴 "스케일 공백"
  지적과 같은 자리다.
- **상태**: 부분 해소 (② 해소, ①③ 잔존 — **③은 두 번째 화면으로 복제됐다**)
  > 📌 **②의 대상이 아예 사라졌다(2026-08-27, PR #390)** — 토핑·딤의 클릭 모디파이어가 판정
  > 오버레이의 `pointerInput` 하나로 합쳐졌다. 관용구를 강제할지 묻던 자리가 없어진 것이지
  > 규약이 달라진 것은 아니다.
  > 📌 **③이 복사됐다(2026-08-19, PR #290)** — C-106 배치 화면이 캔버스 영역에 **같은 값**
  > (상단 60dp·하단 14dp)을 리터럴로 다시 적고 **"공통에 없음" 주석까지 그대로 옮겼다.** 리터럴이 두
  > 화면에 흩어졌으므로 토큰으로 올릴 때 고칠 자리도 둘이다. ①(회전 가능한 점선 테두리)은 이번에
  > `dashedBorder()`로 흡수되는 대신 **화면 밖 `component/` 패키지로 올라가 두 화면이 공유**하는
  > 형태가 됐다 — 디자인시스템으로 가지 않았으니 물음 자체는 남는다.
- **해소 메모**: ①은 [design-system](../architecture/design-system.md) "그리기 프리미티브 소유"
  항목(현재 네 곳 + 이번 예외)과 함께 정한다. ②③은 [2026-08-04]·[2026-08-16] 규약 이탈 항목과 묶인다.

### [2026-08-17] 스캐폴드가 둘로 갈린 채 남았다 — 잔여 이관 8파일과 V1 삭제 시점이 미정이다

- **ID**: OQ-P-204
- **출처**: `YGScaffold.kt`(`@Deprecated(WARNING)` 부착)·`YGScaffoldV2.kt`(PR #267 develop 머지) —
  V2가 정본이 되고 V1에 경고가 붙었지만, 이관은 3화면(A-002·S-003·S-002)에서 멈췄고 **V1 호출이
  8파일 22곳 남았다**(camera·gallery·groups canvas/enter/list/setting·intro·segmentation). 스펙이
  `ERROR` 승급 기준을 "호출처 0"이 아니라 **"각 화면이 Route에서 스캐폴드를 소유하고 로딩·실패를
  배선함"**으로 정정했으므로 IDE 일괄 치환으로는 기준을 채울 수 없고, 화면별 결선 라운드가 필요하다.
  그 라운드가 언제 도는지는 정해진 바 없다.
- **항목**: ① 잔여 8파일을 화면별 API 결선 라운드에 붙일지, 이관만 하는 라운드를 따로 돌릴지.
  ② `ERROR` 승급·V1 파일 삭제 시점. ③ 공존 기간 동안 새로 생기는 화면이 규약(Route 소유)을 지키는지
  기계로 확인할 수단이 없다 — 지금은 리뷰가 유일한 관문이다.
- **상태**: 부분 해소 (① 해소 — 이관 완료, 2026-09-20 PR #513·#514 / ②③ 잔존.
  **8파일 → 7 → 6 → 3 → 2 → 1파일 1호출 → 0**)
- **해소 메모**: 이관이 끝나면 [design-system](../architecture/design-system.md) "화면 컨테이너"의
  V1 항목과 [navigation-flow](../architecture/navigation-flow.md) 체크리스트 2번의 "(구 형태)" 서술을
  함께 지운다. OQ-P-167(실패 표현 갈래)과는 별개 축이다 — 이관해도 실패 표현이 통일되지는 않는다.
  > 📌 **①에 사례로 답이 나왔다(2026-08-17, PR #285)** — S-101 그룹 설정이 **API 결선 라운드에 이관을
  > 딸려서** 옮겼다(엔트리의 `YGScaffold` → Route의 `YGScaffoldV2`). 이관만 하는 라운드가 아니라
  > 결선 라운드에 붙인 것이고, 그 편이 자연스러운 이유는 **채울 것(로딩·실패)이 그때 생기기**
  > 때문이다 — 스펙이 정정한 `ERROR` 승급 기준과 같은 논리다. 남은 것은 **7파일**
  > (camera·gallery·groups canvas/enter/list·intro·segmentation)이고, 그중 groups canvas·list는
  > 이미 서버를 보는 화면이라 **결선 라운드에 딸려 갈 기회가 지나간 자리**다.
  > 📌 **①의 답이 넓어졌다(2026-08-17, PR #297)** — G-001 그룹 목록이 **API 결선이 아닌 재조회
  > 라운드**에 이관됐다. 새로고침 실패를 토스트로 알리기로 하면서 호스트가 필요해진 것이고, 즉 이관을
  > 끌어오는 것은 결선 자체가 아니라 **채울 것이 생기는 시점**이다("기회가 지나갔다"고 적은 자리가
  > 다른 이유로 다시 왔다). 남은 것은 **6파일**(camera·gallery·groups canvas/enter·intro·segmentation).
  > 이 화면은 `isLoading`은 여전히 안 넘기고 오버레이용 두 번째 스캐폴드도 V2로 겹쳐 그린다(OQ-P-046)
  > → [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md).
  > 📌 **①의 답이 한 번 더 넓어졌다 — 이번엔 채울 것이 없는데도 옮겼다(2026-08-18, PR #296·#305)**.
  > 약관 웹뷰는 그전까지 **머티리얼 `Scaffold`를 엔트리에서 직접** 부르던 자리(V1·V2 어느 쪽도 아닌
  > 규약 이탈)라 화면을 고치는 김에 Route + V2로 갔고, 스플래시는 **로띠를 시스템바 밑까지 그리려고**
  > `contentWindowInsets = WindowInsets(0)`을 주려다 V2를 쥐게 됐다. 둘 다 `isLoading`·`toastPolicy`를
  > 안 넘긴다 — 즉 이관의 계기는 "로딩·실패를 채울 때"뿐 아니라 **그 파일을 어차피 여는 라운드**이기도
  > 하다(같은 논리가 `refactor/segmentation-logic` 8엔트리 일괄 이관이다). **V1 잔여 파일 수는 6으로
  > 그대로다** — 스플래시가 빠져도 `feature/intro/impl` EntryBuilder에 약관 동의 엔트리가 남는다.
  > 이관 화면은 7개(A-002·S-003·S-002·S-101·G-001·스플래시·약관 웹뷰).
  > 📌 **③이 처음으로 시험대에 올랐고 통과했다(2026-08-19, PR #290)** — C-106 토핑 배치가 공존 기간에
  > **새로 생긴 화면**인데 이관 대상이 아니라 처음부터 Route에서 V2를 소유했다. 다만 통과 여부를 가른
  > 것은 리뷰이고, ③이 묻는 **기계 확인 수단은 여전히 없다.** 이 화면은 부를 API가 없어
  > `isLoading`·`toastPolicy`를 안 넘긴다. **V1 잔여는 6파일 그대로**이고(같은 모듈 EntryBuilder의 옛
  > 엔트리들이 남는다) 이관 화면은 **8개**가 됐다 →
  > [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md).
  > 📌 **①의 다른 답이 한 라운드에 8엔트리를 옮겼다(2026-08-20, PR #309)** — `camera` 3
  > (`NavKeyCameraCustom`·`NavKeyCameraSystem`·`NavKeyPictureConfirm`) · `gallery` 2
  > (`NavKeyCustomGalleryPicker`·`NavKeySystemGalleryPicker`) · `segmentation` 3
  > (`NavKeySegmentation`·`NavKeySegmentationConfirm`·`NavKeyToppingEdit`)이 한꺼번에 갔다. 계기는
  > 로딩·실패가 아니라 **세 모듈 파일을 어차피 다 여는 라운드**였다는 것이고(위 #296·#305와 같은
  > 갈래), 실제로 여덟 엔트리 중 `isLoading`을 넘기는 곳이 하나도 없다 — 세 모듈의 로딩이 전부 화면
  > 고유 표현이라 V2가 흡수하지 않는 갈래다. 실익은 토스트 배선 쪽이었다(`CustomCameraScreen`·
  > `CustomGalleryPickerScreen`이 직접 꽂던 `YGToastHost`·`toastPolicy` 파라미터가 걷혔고, 조용히
  > 뒤로 가던 카메라 촬영 실패에 `showError`가 붙었다). **V1 잔여가 처음으로 줄어 3파일이 됐다** —
  > 전부 EntryBuilder(`feature/intro/impl`·`feature/groups/enter/impl`·`feature/groups/canvas/impl`)이고
  > ②(`ERROR` 승급·V1 삭제)를 정할 자리가 그만큼 가까워졌다. ③의 기계 확인 수단은 여전히 없다 →
  > [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md).
  > 📌 **①이 묻던 다른 쪽 — 이관만 하는 라운드가 처음 돌았다(2026-08-20, PR #315)** — 온보딩 약관
  > 동의가 옮겨 **V1 잔여 2파일**(`feature/groups/enter/impl` 3곳·`feature/groups/canvas/impl` 5곳)이
  > 됐고 이관 화면은 **17개**다. 다만 "이관만"이라는 이름과 달리 **결선을 데려왔다** — 이 화면은
  > 서버 조회·가입이 진작 붙어 있고 실패 표현만 `TODO`로 비어 있어서, 컨테이너를 옮기는 김에
  > `TermAgreeError` 2갈래 + 공통 토스트가 함께 들어왔다(OQ-P-167 ③④ 해소). 즉 앞선 사례들이
  > "채울 것이 생기는 시점"에 이관이 딸려 온 것이라면, 여기서는 **채울 것이 이미 밀려 있던 자리**가
  > 이관을 계기로 채워졌다. 남은 둘은 EntryBuilder 안에 옛 엔트리가 뭉쳐 있는 자리라 화면별 결선
  > 없이는 안 줄어든다. ③의 기계 확인 수단은 이번에도 없다 →
  > [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md).
  > 📌 **결선 라운드가 다시 한 화면을 데려왔다(2026-08-22, PR #334)** — C-001 캔버스 메인이
  > `feature/groups/canvas/impl` EntryBuilder의 `YGScaffold`를 벗고 Route에서 `YGScaffoldV2`를
  > 소유한다. 계기는 앞선 사례들과 같은 "채울 것이 생기는 시점"이다 — 오늘 캔버스 조회 실패 표현과
  > 배치 로딩 오버레이가 이 라운드에 생겼다. **V1 잔여는 2파일 그대로**이고(같은 EntryBuilder에 옛
  > 엔트리 넷이 남는다: 배경 편집·캔버스 편집·이미지 선택·`NavKeyCanvasMove`) 호출 곳 수가
  > 5 → 4로 줄었다. 이관 화면은 **18개**다. ③의 기계 확인 수단은 이번에도 없다 →
  > [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md).
  > 📌 **버그픽스 라운드가 두 화면을 데려왔고, V1 호출이 하나만 남았다(2026-08-27, PR #393·#394)** —
  > A-005 그룹 생성과 S-102 그룹 참여가 `feature/groups/enter/impl` EntryBuilder의 `YGScaffold`를
  > 벗고 Route에서 `YGScaffoldV2`를 소유한다. 계기는 이번에도 "채울 것이 생기는 시점"이다 —
  > 두 화면의 실패 토스트와 진행 오버레이가 이 라운드에 생겼다. **V1 잔여는 1파일 1호출**
  > (같은 EntryBuilder의 A-004 초대 코드 엔트리)이고 이관 화면은 **20개**다. 그래서 ②(`ERROR` 승급·
  > V1 삭제)가 처음으로 손에 잡히는 거리에 왔다 — 남은 하나는 A-004뿐이다. ③의 기계 확인 수단은
  > 이번에도 없다.
  > ⚠️ **한 모듈 안에서 두 관용구가 나란히 놓였다** — 같은 EntryBuilder에서 A-004는 엔트리가
  > 스캐폴드를 씌우고, 형제 둘은 Route가 쥔다. A-005 쪽 엔트리에는 그 이유를 적은 주석이 붙었다
  > (로딩·토스트가 화면 상태를 봐야 한다).
  > ✅ **①이 닫혔다 — V1 호출이 0이 됐다(2026-09-20, PR #513·#514)** — 마지막 하나였던 A-004
  > 초대 코드가 Route에서 `YGScaffoldV2`를 쥔다(#513). 이관 전용 라운드였고, **옮기면서 채울 것도
  > 같이 왔다** — `isLoading = uiState.isSubmitting`이 붙어 코드 제출 중 공통 덮개가 처음 뜬다
  > (전에는 다음 버튼 비활성만 있었다). 인셋 관용구는 그대로 옮겨 왔다: `contentWindowInsets =
  > WindowInsets(0.dp)`를 스캐폴드가 받고 `statusBarsPadding()` + `navigationBarsAndImePadding()`은
  > Screen `modifier`가 계속 직접 문다. 위 ⚠️가 적은 "한 모듈 두 관용구"도 이로써 사라졌다.
  > 같은 날 #514가 캔버스 EntryBuilder의 구 형태 엔트리 셋을 **화면째 삭제**해(OQ-P-239) 잔여가
  > 완전히 비었다 — 마지막 잔여가 이관이 아니라 **삭제**로 걷힌 셈이다.
  > **②(`ERROR` 승급·V1 파일 삭제)는 그대로 미결**이고, 스펙이 정한 승급 기준("각 화면이 Route에서
  > 스캐폴드를 소유하고 로딩·실패를 배선함")은 이제 충족됐다. ③(기계 확인 수단)도 그대로 없다 —
  > 다만 호출부가 0이 된 지금은 `@Deprecated(ERROR)` 승급 자체가 그 수단이 된다.

### [2026-08-17] 공통 로딩 오버레이가 임시 구현이고, 적용 기준도 사례에서 귀납한 것뿐이다

- **ID**: OQ-P-205
- **출처**: `YGLoadingOverlay.kt`(PR #267 develop 머지) — KDoc이 스스로 "임시 구현"이라고 적고
  Dim 농도·인디케이터 모양·문구 유무가 전부 디자인 미확정이다. `SegmentationLoadingScreen`의
  "로띠 넣을 예정" `TODO`와 같은 운명이고, 그 화면은 여전히 자기 로딩 UI를 따로 그린다. **언제 켜는가**도
  규칙이 아니라 세 화면에서 귀납한 기준("네트워크 왕복인가")이다 — S-002는 처음엔 안 걸었다가
  `isSubmitting`으로 정정했다(`8e0662b5`).
- **항목**: ① 로딩 UI 디자인 확정 시 이 파일과 `SegmentationLoadingScreen`을 함께 정리한다.
  ② 오버레이를 켜는 기준을 규약으로 승격할지 — 지금은 화면마다 "이건 왕복인가"를 다시 판단한다.
  ③ 화면 고유 로딩 화면(문구·닫기 버튼을 가진 것)을 V2가 흡수할 갈래인지 계속 별개로 둘지.
  ④ **`YGLoadingTone`을 고르는 기준을 규약으로 둘지**(#305 신설) — 색을 화면 테마가 아니라 "얹히는
  바탕"으로 고르게 해 놓아 호출자가 매번 판단한다. 지금 두 사례는 갈린다(Dim 위 `Light` / 흰 목록 `Dark`).
- **상태**: 부분 해소 (**①은 완전 해소, ③도 답이 나왔다** — ②④ 잔존)
- **해소 메모**: 정해지면 [design-system](../architecture/design-system.md) "화면 컨테이너"에 적고
  [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)의 "임시" 표기를 걷는다.
  > ✅ **인디케이터가 확정됐다(2026-08-18, PR #305)** — `CircularProgressIndicator` + `Cherry100`이
  > 디자인 로띠(`YGLoadingLottie`, 애셋 원본 치수 `Size44`)로 교체되고 Dim이 `Black25` → **`Black75`**로
  > 짙어졌다. 교체가 `YGLoadingOverlay.kt` 한 파일에서 끝난 것은 스펙이 파일을 나눈 이유가 그대로
  > 맞았다는 뜻이다. **①의 절반은 여전히 열려 있다** — `SegmentationLoadingScreen`의
  > `// TODO: 로띠 넣을 예정`은 그대로라, 로띠가 프로젝트에 들어온 뒤에도 그 화면만 자기 로딩 UI를
  > 그린다(③과 같은 자리). Dim 농도·문구 유무의 근거도 여전히 없다(짙어진 것은 로띠 가독성 때문이고
  > 디자인 수치가 아니다).
  > ✅ **①의 나머지 절반이 닫혔고, 닫힌 방식이 ③의 답이다(2026-08-22, PR #311 develop 머지)** —
  > `SegmentationLoadingScreen`은 로띠를 얻은 것이 아니라 **삭제됐다**. 세그멘테이션 Route가
  > `YGScaffoldV2(isLoading = state.isLoading)`으로 공통 오버레이를 쓰고, 함께 지워진
  > `SegmentationErrorScreen`의 실패는 공통 토스트로 갔다(OQ-P-167 정정). 그래서 ③("화면 고유 로딩
  > 화면을 V2가 흡수할 갈래인지 계속 별개로 둘지")은 **흡수하는 쪽**으로 답이 났다 — 이 저장소에서
  > 화면 고유 로딩 화면은 이제 0개다.
  > **뒤집힌 것은 이 항목이 아니라 [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)의
  > 제외 목록**이다("문구·닫기 버튼을 가진 로딩 화면은 V2가 다루는 갈래가 아니다"). 그 제외를 세웠던
  > 근거 둘이 다시 세어 보니 값이 없었다 — 문구는 `CircularProgressIndicator` 옆의 안내문 두 줄이었고,
  > 닫기 버튼은 **오버레이가 어차피 터치를 삼키므로 로딩 중에는 눌리지도 않던 것**이다(#309로 세
  > 모듈이 이미 V2를 쥔 뒤라 자리는 이미 있었다). 대가는 명시한다: **로딩 중 닫기가 도달 불가**가 됐고
  > (시스템 뒤로가기는 그대로 동작한다) 로딩 안내 문구 두 줄이 사라졌다.
  > **②④(오버레이를 켜는 기준·`YGLoadingTone` 기준)는 그대로 열려 있다.** 이 라운드는 오히려 ②의
  > 귀납 기준("네트워크 왕복인가")에 **왕복이 아닌 첫 사례**를 더했다 — 세그멘테이션은 온디바이스
  > ML Kit 추론이라 서버에 나가지 않는데 오버레이를 켠다. 기준을 규약으로 올린다면 "왕복"이 아니라
  > **"사용자가 기다려야 하는 비동기 작업인가"**에 가깝다. Dim 농도·문구 유무의 근거는 여전히 없다.
  > 📌 **②의 사례가 "누른 작업" 밖으로 나갔다(2026-08-30, PR #407 develop 머지)** — G-001 그룹 목록과
  > C-001 오늘 캔버스가 **화면에 들어오자마자 나가는 첫 조회**에 오버레이를 켠다. 그때까지 기준을
  > 세우던 사례는 전부 사용자가 버튼을 눌러 시작한 작업이었다. 여기서 기준이 하나 더 갈라진다 —
  > **"조회 중인가"가 아니라 "아직 한 번도 못 받은 조회인가"**다(재진입마다 조회가 나가는 두
  > 화면이라 조건이 없으면 덮개가 번쩍인다). 그 판정이 지금 두 ViewModel에 각각 적혀 있다
  > → OQ-P-330. ②를 규약으로 올릴 때 **"무엇을 덮는가"와 함께 "언제가 처음인가"도 함께 정해야
  > 한다**는 것이 이 라운드가 더한 것이다.

### [2026-08-17] 토스트가 떠 있는 2초 동안 상단 띠의 탭이 삼켜지는 것이 전 화면 공통이 됐다

- **ID**: OQ-P-206
- **출처**: `YGToastHost`(기존 동작)·`YGScaffoldV2.kt`(PR #267 develop 머지) — 호스트가 `Box` 최상단
  자식이고 전폭이라, 토스트가 사는 동안 그 띠 아래의 상단바 뒤로가기 같은 버튼이 히트테스트에서
  가려질 수 있다. **이번 변경이 만든 동작은 아니지만**, 지금까지 camera·gallery가 손으로 붙였던 것을
  V2가 모든 화면의 기본으로 승격시켰다.
- **항목**: ① 호스트에 `pointerInput`을 통과시키는 처리(예: 토스트 영역만 히트, 나머지는 관통)를
  넣을지. ② 아니면 토스트를 화면 최상단이 아닌 다른 위치로 옮길지 — 위→아래 노출은 Toast 공통 정책이라
  정책 변경이 선행이다.
- **상태**: 미해결 (실기기로 재현·측정된 바 없다 — 코드 구조에서만 드러났다)
- **해소 메모**: 고치면 `YGToastPolicy`·`YGToastHost` 쪽이 소관이고
  [design-system](../architecture/design-system.md) 토스트 항목에 적는다.

### [2026-08-17] 토핑 배치 값의 단위·기준을 계약이 말하지 않아 앱이 정했다

- **ID**: OQ-P-207
- **출처**: `feature/groups/canvas/impl` `component/CanvasToppingLayer.kt`(PR #268) ·
  [api/parfait.md](../api/parfait.md) — 서버 응답의 `positionX`·`positionY`·`scale`·`rotation`·
  `borderWidth`는 타입만 있고 **무엇에 대한 값인지가 계약에 없다.** 화면이 넷을 정했다:
  좌표는 **Canvas-Area 대비 0~1 정규화 중심점**(절대 px이면 기기마다 캔버스 폭이 달라 같은 배치가
  다른 자리에 뜬다), `scale` 1.0은 **긴 변이 그 너비의 40%**(위키 [[C-106-토핑-배치-정책-v0.1]] 초기
  크기를 끌어다 썼다), `borderWidth` 1.0은 **화면 기준 1dp**(토핑을 키워도 굵기 불변 — 처음엔 배율을
  곱했다가 리뷰에서 정정), `borderColor`는 **`#RRGGBB` 6자리**(계약 타입이 String이라 알파 8자리도
  읽어 둔다). 넷 다 서버 코드·문서에 근거가 없고 **앱 주석이 유일한 기록**이다.
- **항목**: ① 이 해석을 서버 계약 문서(OpenAPI 설명 또는 `api/parfait.md`)에 못 박을지 — 지금은
  iOS가 다르게 해석해도 계약으로 잡히지 않고, **쓰기 경로가 붙는 순간 두 앱이 서로의 배치를 깨뜨린다.**
  ② 40%를 서버가 아는 값으로 볼지(그러면 `scale`은 그 배수라는 뜻이 계약에 들어간다) 앱 표현 규칙으로
  볼지. ③ `rotation`의 단위(도 vs 라디안)·기준축은 아직 아무도 적지 않았다 — 코드는 `rotationZ`에
  그대로 넣어 **도**로 쓴다.
- **상태**: 미해결 (읽기만 있어 아직 충돌하지 않는다 — 그러나 **쓰는 쪽 값이 화면에서 만들어지기
  시작했다**, 2026-08-19)
  > 📌 **앱이 정한 해석이 이제 쓰기 값을 만든다(2026-08-19, PR #290)** — C-106 배치 화면이
  > `offsetX`·`offsetY`(dp)·`scale`·`rotationDegrees`(도)를 만들어 이펙트로 내보낸다. 아직 서버로
  > 가지 않아 충돌은 안 났지만, **①이 말한 "쓰기 경로가 붙는 순간"이 한 칸 가까워졌다.** 특히
  > `scale`은 읽기 쪽 40% 규칙과 **같은 상수를 공유**하게 됐고(`TOPPING_BASE_LONG_SIDE_RATIO`가
  > `internal`로 열렸다), `rotation`은 여전히 도 단위로만 쓰인다(③ 그대로). 좌표는 아직
  > **정규화되지 않은 dp**라 그대로 보내면 읽기 쪽 해석(0~1 정규화)과 어긋난다 — 저장 결선 때 변환이
  > 필요한 자리다.
- **해소 메모**: 정하면 [api/parfait.md](../api/parfait.md) 응답 필드 표에 단위 열을 더하고
  [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md) 드리프트 1을 지운다.
  서버 변경이 필요하면 `sync-teamyg-server-api` 쪽 요청이 선행이다.

### [2026-08-17] 누끼 테두리가 토핑 하나당 이미지 아홉 장을 그린다

- **ID**: OQ-P-208
- **출처**: `CanvasToppingLayer.kt#ToppingOutline`(PR #268) — 토핑이 누끼라 사각 테두리를 두르면 잘라 낸
  배경이 다시 드러난다. 그래서 같은 그림을 테두리 색으로 물들여 **여덟 방향으로 밀어 찍고** 그 위에
  원본을 얹어 실루엣을 딴다. `borderType = SOLID`인 토핑 하나가 `AsyncImagePainter` 그리기 **9회**다.
  캔버스에 올라갈 토핑 수의 상한이 계약·정책 어디에도 없다.
- **항목**: ① 실기기에서 측정할지 — 지금 근거는 "이음매가 안 보인다"는 시각 판단뿐이고 프레임 비용은
  잰 적이 없다. ② 더 싼 방법(알파 마스크 기반 외곽선, `RenderEffect`)으로 갈지. ③ **테두리 표현 자체에
  정책 소스가 없다** — 위키에 토핑 테두리 규정이 없고 C-104 누끼 편집의 "테두리 2~50px"는 편집 화면
  기준이라 캔버스 렌더 규칙과 다른 자리다. 코드가 먼저 확정한 셋째 사례다.
- **상태**: 부분 해소 (② **해소**(PR #464 develop 머지,
  [ADR-0030](../adr/0030-topping-outline-distance-field.md)) — 알파 마스크 기반 거리장으로 확정돼
  여덟 방향 스탬프를 대체한다. **①③은 그대로 잔존한다** — ①은 토핑당 프레임 그리기가 9회에서
  2회로 준 것이 코드에서 읽히는 사실일 뿐 프레임을 잰 적이 없고, ③은 굵기에 정책 소스가
  없다는 사실을 이번 라운드도 건드리지 않았다 — **범위만 2~50dp 에서 2~30dp 로 바뀌었고**
  그 값도 정책이 아니라 눈으로 본 결과다, OQ-P-381)
- **해소 메모**: ①은 실기기 프레임 측정이 선행이다([topping-border-distance-field
  스펙](../superpowers/specs/archive/2026-09-07-topping-border-distance-field.md) 「육안 확인」 4번은 버벅임 유무만
  보고 수치는 재지 않는다). ③은 위키 정책 수집 요청이 선행이다. 둘 다 처리되면 이 항목과
  [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md) 드리프트 5를 지운다.

### [2026-08-17] 캔버스가 도달 가능해졌는데 토핑을 얹는 경로는 여전히 없다

- **ID**: OQ-P-209
- **출처**: `CanvasMainViewModel`(PR #268) · [api/parfait-image.md](../api/parfait-image.md) —
  진입이 열리고 서버 배치가 그려지면서 **읽기만 붙었다는 사실이 사용자 경로 위로 올라왔다.** 토핑 배치
  확정(POST)·좌표 수정 표면은 `:data`까지 있는데 소비처가 0건이고, C-102 갤러리는 결과 반환이 끊겨 있어
  (OQ-P-087) 카메라·갤러리로 들어가도 캔버스로 돌아오지 못한다. 즉 화면이 **남이 올린 토핑을 보는
  용도로만** 동작한다. `today` 조회를 쓰는 근거("토핑을 얹으려면 `parfaitId`가 필요하다")도 아직
  실현되지 않았다 — `parfaitId`는 받아 놓고 쓰는 데가 없어 상태에서 **제거**됐다.
- **항목**: ① ~~배치 결선 라운드에서 좌표·배율·회전을 어떻게 만들지~~ → ✅ **해소(2026-08-19, PR #290)**.
  C-106 배치 화면이 생겨 사용자가 드래그·리사이즈·회전으로 만들고, 초기값은 정중앙·40%·48dp 하한이다
  (OQ-P-200 종결). ② 서버 수정 표면이 **테두리만** 받으므로 이동·크기·
  회전을 저장할 계약 확장이 선행이다(OQ-P-199 ②). ③ 얹은 직후 화면 갱신을 재조회로 할지 로컬 반영으로
  할지 — 재조회면 부작용 있는 `today`를 다시 부르게 된다.
- **상태**: 부분 해소 (① 해소, ②③ 잔존 — **값은 만들어졌는데 보낼 곳이 없다**)
  > 📌 **경로가 화면까지 왔고 거기서 멈췄다(2026-08-19, PR #290)** — 확인 버튼이
  > `CanvasToppingPlaceEffect.ToppingPlaced(imageUri, offsetX, offsetY, scale, rotationDegrees)`를
  > 쏘고, Route는 `// TODO` 한 줄 뒤에 캔버스로 이동한다. **네 값이 다 만들어졌는데 실어 보낼 계약이
  > 없어 그대로 버려진다** — ②가 이제 사용자 경로 위에 올라왔다는 뜻이다. ③은 손도 못 댔다(보낸 것이
  > 없으니 갱신할 것도 없다). 이동 방식 자체의 문제는 OQ-P-238로 갈렸다.
- **해소 메모**: OQ-P-199 ②와 한 라운드다. 정해지면
  [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md) 드리프트 2와
  [api/parfait-image.md](../api/parfait-image.md) Android 매핑을 함께 고친다.

### [2026-08-17] 캔버스 멤버 칩 색이 목록 순서로 돌아가 사람에 붙지 않는다

- **ID**: OQ-P-210
- **출처**: `CanvasMainViewModel#toMemberChips`(PR #268) — 서버 `groupMembers`에 칩 색·타입 필드가
  없어 **목록 인덱스로 팔레트 7종을 돌려 쓴다.** 보장되는 것은 "같은 그룹을 다시 열면 같은 사람에게 같은
  색"까지이고, **멤버가 빠지면 뒤가 한 칸씩 밀려 색이 바뀐다.** 위키 [[nametag-chip]]은 "타입은 유저별
  고정"이라고 적는다. S-101 그룹원 목록이 같은 형태로 먼저 어긴 자리(2026-08-13)의 두 번째 사례이고,
  **두 화면이 같은 사람에게 서로 다른 색을 줄 수 있다**(팔레트 목록도 서로 다르다).
- **항목**: ① 타입 부여 주체를 서버로 올릴지(응답 필드 추가) 앱이 결정론적으로 파생할지
  (예: `groupMemberId` 해시) — 후자면 두 화면이 같은 규칙을 공유해야 한다. ② 캔버스가 쓰는 팔레트가
  7종인 근거가 없다(`YGColorChipType`은 12종 + Plus). ③ 칩 글자가 닉네임 `take(1)`인데 정책에는
  "첫 글자"의 정의(공백·이모지·조합 문자)가 없다.
- **상태**: 미해결 (①은 서버가 절반만 답했다 — 2026-08-18)
- **해소 메모**: ①이 서버 변경이면 `sync-teamyg-server-api` 요청이 선행이다. 정하면
  [design-system](../architecture/design-system.md) `YGNametagChip` 항목의 두 사례와
  [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md) 정책 대조 표를 함께 고친다.
  > ⚠️ **①의 답이 이 화면만 비껴갔다(2026-08-18 서버 delta `08df1bf`)** — 서버가 칩 배정을 가져갔지만
  > 필드는 **`placedBy`에만** 붙었고 상단 칩이 읽는 `groupMembers`에는 없다. 그룹 설정 화면
  > (OQ-P-140)은 `members[].nametagChip`으로 닫을 수 있는데 **여기만 계약 밖에 남았다**
  > → OQ-P-224 ①. ②(팔레트 7종의 근거 없음)는 그대로이고, 서버가 12종을 주는 지금은
  > **7종으로 접으면 계약이 주는 구분을 앱이 도로 뭉갠다**는 문제로 성격이 바뀌었다.
  > ③(칩 글자 `take(1)`의 정의)은 변동 없다.

### [2026-08-17] 지난 캔버스의 "갤러리에 저장"이 로그 한 줄이다

- **ID**: OQ-P-211
- **출처**: `CanvasMainViewModel#handleClickSaveToGallery`(PR #279) — 지난 캔버스를 볼 때 메뉴
  맨 위에 오는 액션인데 핸들러가 `TODO` 주석과 정보 로그 하나다. 버튼·문구(`canvas_main_save_to_gallery`)·
  아이콘·인텐트는 다 있어서 **사용자에게는 동작하는 기능처럼 보인다.** 같은 라운드가 지난 캔버스에서
  편집 진입점을 치운 자리를 이 액션이 대신 차지했으므로, 지금 지난 캔버스에서 할 수 있는 일은
  **오늘로 돌아가기 하나뿐**이다.
- **항목**: ① 캔버스를 이미지로 만드는 주체 — Compose `GraphicsLayer` 캡처인지 서버 렌더인지
  (전자면 화면 밖 영역·비동기 이미지 로딩 완료를 어떻게 다룰지가 따라온다). ② 저장 권한·경로
  (`MediaStore` 진입점이 앱에 아직 없다). ③ 결과 표현 — 성공·실패 토스트가 필요하면 `YGScaffoldV2`
  이관과 같은 자리다(OQ-P-204). ④ 구현 전까지 버튼을 비활성으로 둘지.
- **상태**: **해소됨** (2026-08-23, PR #335과 같은 라운드의 PR #324 — 네 항목 전부 답이 나왔다)
- **해소 메모**: 정하면 [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)
  드리프트 1을 지운다. ①이 캡처면 `core:util:android` 소유 판단이 붙는다(OQ-P-186과 같은 성격).
  > ✅ **넷 다 답이 나왔다**(2026-08-23, PR #324) — 설계는
  > [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)이 갖는다.
  > **①** Compose `GraphicsLayer` 캡처다. 다만 예상했던 후속("화면 밖 영역·비동기 이미지 로딩 완료를
  > 어떻게 다룰지")은 **다뤄지지 않은 채 남았다** — 캡처는 지금 화면에 그려진 것을 그대로 복사한다
  > (OQ-P-272). 소유 판단은 갈렸다: 권한 판정만 `core:util:android`(`GalleryWritePermissionManager`)
  > 로 가고, 캡처는 화면(`CanvasMainRoute`)에 남았다.
  > **②** `MediaStore` 진입점이 기존 `GalleryMediaProvider`(읽기 전용이던 자리)에 붙었고, 권한은
  > API 29 미만에서만 필요해 매니페스트도 `maxSdkVersion="28"`로 좁혔다. 저장 위치는
  > `Pictures/Parfait`인데 **그 경로도 API 29부터다**(OQ-P-274).
  > **③** 성공·실패 모두 토스트이고 자리는 `YGScaffoldV2`가 아니라 **`YGCanvas.overlayContent`의
  > 호스트**다 — 이 화면의 토스트 자리를 정한 OQ-P-167의 결정을 그대로 따랐다.
  > **④** 비활성으로 두는 갈래는 쓰이지 않았다(구현이 같은 라운드에 들어와 물음이 사라졌다).

### [2026-08-17] 날짜 연속 선택의 승자가 근거 없이 뒤집혔다 — 머리말과 그림이 어긋난 채 남는다

- **ID**: OQ-P-212
- **출처**: `CanvasMainViewModel#loadCanvasDetail`·`handleClickDate`(PR #279) — 상세 조회에
  `launch(key = LOAD_CANVAS_DETAIL_KEY)` 가드가 붙었다. `BaseViewModel.launch`의 key 가드는 **같은 key가
  돌고 있으면 새 작업을 시작하지 않는다**(앞선 것이 이긴다). 같은 라운드가 **이전 날 그림을 비우지 않도록**
  바꿨으므로, A → B를 빠르게 고르면 ⓐ B 요청이 버려지고 ⓑ A 응답이 와도 `selectedDate`가 B라 반영되지
  않아 **B 머리말 + A 토핑**이 다음 조작까지 남는다. 직전 라운드(PR #268)는 정확히 이 이유로 가드를
  **일부러 걸지 않았다**고 코드 주석에 적었는데(OQ-P-184 ②), 그 주석과 함께 근거가 사라졌다.
- **항목**: ① 마지막 선택이 이겨야 하는가(그러면 가드를 빼거나 앞선 job을 취소하는 형태로 바꾼다) —
  `launch`에 "새 것이 이긴다" 변형이 없어 **베이스 확장 여부**가 함께 걸린다. ② 아니면 조회 중 달력·
  날짜 탭을 막을지. ③ 어느 쪽이든 이전 날 그림을 남기는 선택과 짝이라 함께 정해야 한다.
- **상태**: 미해결 (실기기 확인 없음 — 재현은 연속 탭 타이밍에 달렸다)
- **해소 메모**: 정하면 [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)
  드리프트 2와 [state-management](../architecture/state-management.md) `launch` 가드 서술을 함께 고친다.
  ①을 고르면 [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md)의
  "반영 직전 재확인" 서술이 정본이 된다.

### [2026-08-17] 달력이 기록 없는 과거 날짜를 잠근다 — 위키 정책의 Disabled 정의를 넘어섰다

- **ID**: OQ-P-213
- **출처**: `CustomCalendar`(PR #279) — 셀 활성 조건이 `date <= today`에서
  `오늘 || uploadedDates 포함`으로 바뀌었다. 위키 [[캘린더-컴포넌트]]는 Disabled를 **"캔버스를 볼 수
  없는 미래 날짜"**로 정의하는데, 이제 **파르페가 없는 과거 날짜와 토핑이 0건인 날**까지 잠긴다
  (`uploadedDates`는 `toppingCount > 0`만 담는다). 이유는 타당하다 — 눌러도 열 캔버스가 없으면 그 탭은
  아무 일도 하지 않는다. 다만 **정책 문서에 없는 조건이고**, "캔버스는 열렸는데 토핑이 없는 날"은
  서버에 행이 있어 열 수 있는데도 닫힌다.
- **항목**: ① 정책을 코드에 맞춰 넓힐지(위키 수집 요청) 코드를 정책에 맞춰 되돌릴지.
  ② 토핑 0건인 날을 "열 수 있는 날"로 볼지 — `isEmpty`는 원래 **점을 찍지 않는 기준**이었는데 이번에
  **열람 가능 기준**까지 겸하게 됐다(한 값이 두 뜻을 진다). ③ 앞뒤 달 날짜를 잠그는 기존 확대
  (2026-08-16 항목)와 함께 정리할지.
- **상태**: 미해결 (정책 소스 부재 — 위키 소관 판단이 먼저)
- **해소 메모**: 정하면 [c201 스펙](../superpowers/specs/archive/2026-08-16-c201-canvas-calendar.md) 정책 대조 표와
  [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)
  정책 대조 표를 함께 고친다. ①이 위키 쪽이면 정책 소스 수집 요청이 선행이다.

### [2026-08-17] 연도별 캐시에 무효화 경로가 없고, 파생이 캐시가 가른 둘을 다시 뭉갠다

- **ID**: OQ-P-214
- **출처**: `CanvasMainUiState.parfaitHistoriesByYear`·`parfaitHistories`(PR #279) — 한 번 받은 해는
  화면이 사는 동안 다시 받지 않는다. ⓐ **무효화 경로가 없어** 오늘 캔버스에 토핑을 얹어도 달력 점이
  안 바뀐다(지금은 얹는 경로가 없어 안 드러난다, OQ-P-209). ⓑ 캐시를 `Map`으로 둔 근거는 **"받아 봤는데
  비어 있는 해"와 "아직 안 받은 해"를 구분**하는 것인데, 정작 화면이 읽는 파생 `parfaitHistories`는
  `orEmpty()`라 **둘을 다시 같은 빈 목록으로** 준다. ⓒ 그 해 조회가 실패해도 캐시에 안 들어가므로
  같은 해로 돌아올 때마다 다시 부른다 — 재시도로는 맞고, 실패를 화면에 알리지 않으므로 사용자에겐
  "점이 없는 해"와 같아 보인다.
- **항목**: ① 토핑 배치 결선 때 무효화를 어떻게 걸지 — 해당 연도만 버릴지, `PastCanvasVO`를 로컬에서
  갱신할지. ② 파생이 상태 셋(미조회·조회됨·실패)을 표현해야 하는지 — 그러면 UiState가 `Map<Int, X>`의
  값 타입을 갖게 된다. ③ 캐시 상한이 없다(연도를 오갈수록 쌓인다) — 화면 수명 안이라 문제가 아니라고
  볼지.
- **상태**: 미해결
- **해소 메모**: ①은 OQ-P-209(토핑 얹는 경로)와 한 라운드다. 정하면
  [c201-canvas-calendar-server 스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)
  드리프트 3·4를 지우고 [state-management](../architecture/state-management.md)의 "UI State가 담는 것"에
  캐시 형태를 한 줄 적는다.

### [2026-08-17] 무리플 전면 이관으로 피드백이 사라진 클릭 6곳 — `clickableYG` 승격 후보

- **ID**: OQ-P-215
- **출처**: 이관 #284 — 프로덕션 `Modifier.clickable` 28곳이 `clickableYGNoRipple`로 옮겨졌다. 그중
  **원래 Material 기본 리플이 돌던 자리**는 무리플이 되면서 시각 피드백이 실제로 줄었다. 컴포넌트가
  `collectIsPressedAsState()`로 눌림을 그리거나 선택 상태가 남는 곳은 손실이 없지만, 아래 여섯은
  **리플이 유일한 피드백**이었다 — `NotionWebView` 재시도 텍스트, `TermAgreeScreen` 재시도 텍스트와
  약관 링크 caret, `InviteCodePasteBar`, `GalleryImageGridComponent` 셀,
  `CanvasImageSelectScreen` 이미지. 뒤 둘은 탭 즉시 `goTo`로 전진해 화면이 바뀌므로 손실이 가볍고,
  앞 넷은 제자리에 남아 "눌렸는지" 단서가 없다.
  > 📌 **후보가 다섯으로 줄었다(2026-09-20, PR #514)** — `CanvasImageSelectScreen`이 도달 불가
  > 화면으로 삭제됐다(OQ-P-239). 손실이 가볍다고 적었던 둘 중 하나가 판정 없이 사라진 것이라
  > 결정 대상에서 빠진다.
- **항목**: ① 여섯을 `clickableYG`(Dim 리플)로 올릴지, 텍스트 링크류는 색 변화 같은 다른 표현으로 갈지.
  ② 승격 판정 기준을 규약으로 적을지 — "자체 눌림/선택 표현이 없고 제자리에 남는 클릭은 리플을 준다"
  같은 문장이 있어야 다음 화면에서 같은 판단을 반복하지 않는다.
- **상태**: 미해결 (실기기 확인 없음 — 체감 손실 크기를 눈으로 못 봤다)
- **해소 메모**: 정하면 [design-system](../architecture/design-system.md) clickable 절의 "무리플이 기본이다"
  아래 승격 기준을 한 줄 추가하고 후보 목록을 지운다. 이 항목은 [2026-08-04] 갤러리 그리드 셀
  항목의 ②(리플 변형 선택)를 이어받은 자리다.

### [2026-08-17] 그룹 상세 한 화면에 요청이 둘이고, 둘째 실패는 조용히 삼켜진다

- **ID**: OQ-P-216
- **출처**: `GetGroupDetailUseCase`·`ParfaitGroupRepository#getGroupDetail` KDoc(PR #285) ×
  [api/parfait-group.md](../api/parfait-group.md) — `GET /api/parfait-groups/{groupId}` 응답에
  **그룹명이 없어** UseCase가 `getMyGroups()`를 한 번 더 불러 같은 `groupId`의 이름만 집어 붙인다
  (조합 결과가 `GroupDetailVO`이고, 서버 응답에 1:1로 대응하지 않는 유일한 그룹 VO다). 이름 조회
  실패는 실패로 치지 않고 **빈 `GroupName`** 으로 둔다 — 상단바 제목만 빈 채 나머지가 뜬다.
  양쪽 KDoc에 `TODO(서버 응답 확장 대기)`가 붙어 있어 **임시임을 코드가 스스로 적는다.**
- **항목**: ① 서버에 상세 응답 `groupName` 추가를 요청할지(그러면 두 번째 호출과 조합 VO가 함께
  사라진다), 아니면 진입 화면이 이름을 NavKey로 넘기는 형태로 갈지 — 후자는 C-001이 이미 그룹명을
  갖고 있지 않아 그쪽에도 조회가 필요하다. ② 빈 제목이 사용자에게 어떻게 보이는지 정하지 않았다
  (지금은 상단바가 그냥 빈칸이고, 이름만 실패했다는 표시가 없다). ③ 화면 진입마다 목록 전체를 다시
  받는 비용을 감수할지 — **③은 그룹 SSoT 라운드가 닫았다**: 목록·상세가 인메모리 SSoT 한 벌에 있고
  `GetGroupDetailUseCase`는 두 캐시를 `combine`할 뿐이라 이름 때문에 나가는 HTTP 호출이 사라졌다
  ([ADR-0023](../adr/0023-group-in-memory-ssot.md)). ①②는 그대로 남는다 — 서버 응답에 여전히
  그룹명이 없고, 빈 제목의 표현도 정하지 않았다.
- **상태**: 해소됨 (③은 그룹 SSoT가, ①은 서버 delta + PR #308이, ②는 조합 소멸로 — develop 머지 2026-08-20)
- **해소 메모**: 서버가 필드를 실으면 [api/parfait-group.md](../api/parfait-group.md) Android 매핑과
  [data-layer](../architecture/data-layer.md)의 "UseCase가 Repository를 두 번 부르는 첫 사례" 서술을
  함께 걷는다. 같은 응답의 `memberLimit` 공백은 [2026-08-13] S-101 데이터 항목(OQ-P-139)이다.
  > ✅ **서버가 실었다(2026-08-18 서버 delta `08df1bf`)** — 상세 응답에 `groupName`이 있고 같은 라운드가
  > `memberLimit`도 넣어 OQ-P-139까지 함께 닫혔다. 두 KDoc의 `TODO(서버 응답 확장 대기)`가 기다리던
  > 조건이 충족됐으므로, **`GroupDetailVO` 조합과 `GetGroupDetailUseCase`의 `combine`을 걷어내는 것은
  > 이제 앱 쪽 작업**이다(OQ-P-224 ②). ②(이름만 실패했을 때의 빈 제목 표현)는 조합이 사라지면 문제
  > 자체가 없어진다 — 상세 한 번으로 이름이 온다.
  > ✅ **앱 쪽도 닫혔다(2026-08-20, PR #308 develop 머지)** — `GroupDetailVO`와 `combine`, 두
  > `TODO(서버 응답 확장 대기)`가 함께 삭제됐다. ②는 예고대로 **문제 자체가 사라졌다** — 이름이 상세
  > 응답에 실려 오므로 "이름만 실패한 상태"가 성립하지 않는다.
  > [data-layer](../architecture/data-layer.md)의 "UseCase가 Repository를 두 번 부르는 첫 사례" 서술도
  > 이 라운드에 고쳤다 — 지우지 않고 **소멸한 사례로 남겼다**(합성 자리를 `:data`가 아니라 UseCase에 둔
  > 판단이 그 소멸을 한 줄 삭제로 끝나게 했다는 것이 남길 값이다).

### [2026-08-17] 모든 그룹 신고가 같은 사유 문자열로 저장된다

- **ID**: OQ-P-217
- **출처**: `GroupSettingViewModel`의 `GROUP_REPORT_REASON`(PR #287) ×
  [api/parfait-group.md](../api/parfait-group.md) — 서버는 신고 사유를 필수로 받고 공백이면 400
  `INVALID_GROUP_REPORT_REASON`인데 **사유를 고르는 UI가 없다.** 화면이 상수 문자열 하나를 대신
  채우고, 코드 TODO가 "사유 선택이 생기면 이 상수는 사라진다"고 적는다. 신고는 성공 시 **탈퇴를
  동반**하므로(서버가 같은 트랜잭션에서 처리) 되돌릴 수 없는 동작인데, 운영 쪽에는 모든 건이
  구분되지 않는 같은 사유로 쌓인다.
- **항목**: ① 사유 선택 UI를 넣을지(디자인·문구 미정), 아니면 자유 입력으로 받을지. ② 사유 목록을
  누가 정의할지 — 서버가 enum으로 받을지 지금처럼 자유 문자열로 둘지. ③ 그 전까지 상수 문구를
  운영이 식별 가능한 값(예: 진입 화면 표시)으로 둘지.
- **상태**: 미해결
- **해소 메모**: 정해지면 [s101-group-setting-api 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md)
  드리프트 절과 [api/parfait-group.md](../api/parfait-group.md) 신고 절에 반영한다.

### [2026-08-17] 이미 나간 그룹을 다시 여는 것과 일시 장애가 같은 문구다

- **ID**: OQ-P-218
- **출처**: `GroupSettingError`·`GroupSettingViewModel#toGroupSettingError`(PR #285·#287) —
  갈래가 셋(`INVALID_NICKNAME`·`NETWORK`·`UNKNOWN`)인데 소비 경로는 넷(상세 조회·닉네임 변경·
  나가기·신고)이다. `GROUP_NOT_FOUND`(404)·`GROUP_NOT_JOINED`(403)가 전부 `UNKNOWN`("잠시 후 다시
  시도해 주세요")으로 접혀, **다시 시도해도 결과가 같은 실패**가 일시 장애처럼 안내된다. enum KDoc도
  "닉네임 변경이 서버에서 되돌아온 사유"라고 적어 지금 쓰임과 어긋난다.
- **항목**: ① 403/404에 별도 문구를 줄지 — 준다면 "이미 나간 그룹"은 문구보다 **목록으로 돌려보내는
  것**이 맞을 수 있다(나가기 성공 경로가 이미 `replaceAll`을 한다). ② 화면 고유 문구가 없는 실패의
  공통 매핑(OQ-P-167 ②)이 생기면 이 enum이 그 위에 얹힐지. ③ enum KDoc·이름을 네 경로 공용으로 정정할지.
- **상태**: 미해결 (실패해도 화면은 남으므로 갇히지는 않는다)
- **해소 메모**: OQ-P-167(실패 표현 갈래)의 하위 사례다. 정하면
  [s101-group-setting-api 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md)과
  [api/parfait-group.md](../api/parfait-group.md) Android 매핑에 반영한다.

### [2026-08-17] 목록 캐시를 통째로 덮는 갱신이, 폴링이 붙는 순간 나간 그룹을 되살린다

- **ID**: OQ-P-219
- **출처**: `GroupLocalDataSourceImpl#saveMyGroups`·`#removeGroup`(그룹 SSoT 라운드) ×
  [group-ssot 스펙](../superpowers/specs/archive/2026-08-17-group-ssot.md) "갱신·무효화 규칙" — 목록 갱신은
  받은 응답으로 **통째 대입**이고 나가기·신고는 캐시에서 그 그룹을 지운다. 두 쓰기 사이에 순서 보장이
  없어서, `removeGroup` **뒤에** 도착하는 *그 이전에 출발한* 목록 응답이 나간 그룹을 되살린다.
  지금 트리거 배치(나가기 성공 → 목록 복귀 → `Enter` 재조회)로는 창이 사실상 닫혀 있지만, 같은 스펙이
  자리를 남겨 둔 **폴링이 들어오면 바로 열린다**(주기 조회가 사용자 조작과 겹친다).
- **항목**: ① 폴링 도입 시 세대 카운터(요청 시각·시퀀스)로 낡은 응답을 버릴지, tombstone(나간 그룹 id를
  잠시 기억)으로 막을지. ② 목록 갱신을 통째 대입이 아니라 병합으로 바꿀지 — 그러면 서버에서 사라진
  그룹을 앱이 스스로 못 지운다. ③ 나가기·신고 직후 진행 중인 목록 조회를 취소할지.
- **상태**: 미해결 (폴링 전까지는 잠재 — 코드는 2026-08-20 PR #307로 develop에 있다)
- **해소 메모**: 폴링 설계에서 정하고 [ADR-0023](../adr/0023-group-in-memory-ssot.md) "갱신 시점"과
  스펙의 갱신 규칙 표에 함께 반영한다. 같은 라운드에서 생성·참여 직후 목록 조회가 두 번 나가는 것
  (저장소 재조회 + G-001 `Enter` 재조회)도 그때 같이 볼 자리다.
  **2026-08-27 — 캔버스 형태가 열렸다.** [캔버스 오늘 SSoT·폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  이 항목 ②(통째 대입을 병합으로)를 캔버스 **화면 상태 층**에서 답했다(dirty 집합 + 삭제 툼스톤).
  항목 ①·③은 저장소 캐시 층의 문제라 그대로 열려 있고, 캔버스에서 나타난 형태를 OQ-P-321로
  따로 등록했다. 두 항목은 같은 결정으로 함께 닫는 것이 맞다.

### [2026-08-17] 그룹 닉네임은 바뀌었는데 화면이 옛 이름을 든 채 확인 버튼이 되살아난다

- **ID**: OQ-P-220
- **출처**: `ParfaitGroupRepositoryImpl#changeMyNickname`·`GroupSettingViewModel`(그룹 SSoT 라운드) ×
  [group-ssot 스펙](../superpowers/specs/archive/2026-08-17-group-ssot.md) — 닉네임 변경 성공 뒤 저장소가
  상세를 다시 받아 캐시에 넣고, 화면은 그 방출로 새 값을 얻는다(화면이 자기 상태를 손으로 고치지 않는
  것이 이 설계의 요점이다). 그런데 **그 후속 재조회가 실패하면** 변경 자체는 `success`로 남으므로
  화면은 편집을 닫지만 캐시는 옛 이름 그대로다. 결과로 `nicknameInput`(새 값)과 `myNickname`(옛 값)이
  갈라져 **확인 버튼이 다시 활성**이 된다(`isConfirmEnabled`) — 사용자는 방금 성공한 변경을 다시
  누르게 된다.
- **항목**: ① 재조회 실패를 사용자에게 알릴지(스펙의 열린 질문 "낡은 값을 알릴 수단이 없다"와 같은
  뿌리다). ② 알리지 않는다면 재조회 실패 시 편집 상태를 어떻게 정리할지 — 입력을 옛 값으로 되돌릴지,
  확인 버튼만 비활성으로 둘지. ③ 응답이 주는 새 닉네임으로 캐시를 낙관적으로 갱신할지(그러면 재조회
  자체가 필요 없어지지만, 멤버 목록의 내 항목을 짚으려면 계정 id가 필요해 저장소가 계정 저장소를
  알게 된다 — 이 라운드가 피한 결합이다).
- **상태**: 미해결 (코드는 2026-08-20 PR #307로 develop에 있다 — 잠재가 아니라 상시 재현 가능한 자리다)
- **해소 메모**: 정하면 [ADR-0023](../adr/0023-group-in-memory-ssot.md) "트레이드오프"와
  [group-ssot 스펙](../superpowers/specs/archive/2026-08-17-group-ssot.md) 열린 질문 절에 반영한다.

### [2026-08-18] 재진입 재조회가 관용구일 뿐 규약이 아니고, 실패 표현이 화면마다 갈린다

- **ID**: OQ-P-221
- **출처**: `GroupListRoute`·`CanvasMainRoute`의 `LifecycleResumeEffect` + 두 ViewModel의 `Enter`
  (PR #297 develop 머지) — 같은 형태가 두 화면에 생겼지만 **어디에도 규약으로 적혀 있지 않다.**
  ① 새로 생기는 화면이 이것을 따르는지 확인할 수단이 없다(리뷰가 유일한 관문이고, `init` 조회로
  머지되면 그 화면만 조용히 낡는다). ② key 관용구도 갈렸다 — G-001은 `LifecycleResumeEffect(Unit)`,
  C-001은 `LifecycleResumeEffect(viewModel)`. ③ **실패 표현이 같이 안 왔다** — G-001은 당긴 새로고침
  실패에 토스트 자리가 생겼지만 재진입 조회 실패는 조용하고, C-001은 오늘 캔버스·달력 기록 조회 실패가
  전부 로그뿐인데 **재조회 빈도만 늘었다**. 즉 재진입마다 실패할 기회가 늘어난 화면에서 사용자는
  낡은 값을 낡은 줄 모르고 본다.
- **항목**: ① 재진입 재조회를 규약으로 올릴지(올린다면 "무엇을 다시 묻고 무엇은 두는가"의 기준도 함께 —
  C-001은 오늘·올해만 다시 받고 연도 목록·지난 캔버스는 두는데 그 판단이 화면마다 재발명된다).
  ② key 관용구 통일. ③ 재진입 조회 실패를 알릴지 — 알린다면 사용자가 시키지 않은 조회의 실패를
  어떤 강도로 말할지(토스트는 스스로 뜬 실패라 성격이 다르다), 안 알린다면 그 결정을 어디에 적을지.
- **상태**: 미해결
- **해소 메모**: ①②는 [state-management](../architecture/state-management.md) 재진입 재조회 절을
  규약으로 승격하고 [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md)을
  정본으로 가리킨다. ③은 실패 표현 갈래(OQ-P-167)·V2 이관(OQ-P-204)과 같은 자리에서 정한다.

### [2026-08-18] 하루의 경계가 서버는 03시, 앱은 자정 — 그 세 시간 동안 오늘 조회가 두 번 돈다

- **ID**: OQ-P-222
- **출처**: 서버 `ParfaitDay`(`core/parfait/domain`, 2026-08-18 delta `08df1bf`) ×
  앱 `domain/model/ParfaitDay.kt`의 `parfaitToday()` × `GetTodayParfaitUseCase` ×
  [api/parfait.md](../api/parfait.md) "하루 경계" — 서버가 파르페의 하루를 **03:00에 넘긴다**(회전 배치
  실행 시각과 같은 값이고 위키 [[캔버스-마감-스케줄]]의 03시와도 같다). 오늘 조회·그룹 생성·회전 가드가
  전부 이 기준이다. **앱은 KST 자정 기준 그대로**라, 00:00~03:00 KST에는 서버가 준 정상 응답(D−1 날짜의
  `ACTIVE` 캔버스)을 `GetTodayParfaitUseCase`가 "자정을 걸친 요청"으로 오인해 **한 번 더 부른다** —
  두 번째도 같은 값이라 결국 그것을 쓰지만, **부작용 있는 GET이 그 구간 내내 두 배로 돈다.** 표시도
  어긋난다: `CanvasMainUiState.today`는 D인데 그 아래 그려지는 캔버스는 D−1이고, `syncToday()`가 자정에
  화면을 비우고 다시 불러도 03시까지 계속 D−1이 온다. 달력이 고르는 "오늘"과 활성 캔버스 날짜도 그
  구간에는 다르다.
- **항목**: ① 앱 `parfaitToday()`를 03시 경계로 맞출지 — 맞추면 재시도 조건·달력의 오늘·`syncToday()`
  트리거 시각이 전부 03시로 옮겨간다(자정에 도는 지금의 재계산은 의미가 없어진다). ② 경계 값(03시)을
  앱이 상수로 복제할지 계약에서 읽을지 — 지금 계약에 그 값을 내려주는 필드가 없고, 복제하면 서버가
  배치 시각을 바꿀 때 조용히 갈린다. ③ **서버 안에서도 두 기준이 공존한다** — 과거 목록의 `to`
  기본값만 `LocalDate.now()`(자정)라, 그 구간에 목록 기본 상한이 활성 캔버스 날짜보다 하루 앞선다.
  서버에 정렬을 요청할지, 앱이 항상 범위를 명시해 회피할지.
- **상태**: 해소됨 (앱이 03시로 옮김 — PR #308 develop 머지 2026-08-20 / ③은 2026-08-19 서버 delta가 닫음)
- **해소 메모**: 정책상 옳은 쪽은 서버다(03시는 위키 [[캔버스-마감-스케줄]]에 이미 있다). 고치면
  [api/parfait.md](../api/parfait.md) 엔드포인트 표의 `⚠️불일치` 각주와
  [api/conventions.md](../api/conventions.md) "Android 불일치" 2행을 함께 걷는다. ③은 서버 소관이라
  다음 `sync-teamyg-server-api` 라운드에서 확인한다.
  > ✅ **①② 해소, ③ 잔존(2026-08-18 구현, PR #308로 2026-08-20 develop 머지)** — `parfaitToday()`가
  > `DayWindow.DAY_BOUNDARY_HOUR`를 써서 03시에 넘어간다 — ②의 "복제할지"는 **복제하되 상수를 공유하고,
  > 시각만 공유하고 시간대는 공유하지 않는다는 것을 KDoc에 박는다**로 답했다. 같은 라운드가
  > `GetTodayParfaitUseCase.invoke`에 `clock` 기본 파라미터를 열어 경계 동작을 테스트로 잠갔다 —
  > 그전에는 테스트가 검증 대상과 같은 함수로 기대값을 만들어 경계를 되돌려도 전부 통과했다.
  > ✅ **③도 해소(2026-08-19, 서버 `57529ec`)** — `GetPastParfaitsService`의 `to` 기본값이
  > `LocalDate.now()`에서 `ParfaitDay.current()`로 바뀌어 **서버 안의 두 기준이 하나가 됐다**
  > (`fix: 이전 파르페 목록 조회가 자정이 아닌 새벽 3시 기준으로 오늘을 판단하도록 통일`).
  > 이 항목은 세 갈래 모두 닫혔고 **앱 쪽도 2026-08-20에 develop에 들어갔다**(`412991ea`) —
  > [api/parfait.md](../api/parfait.md)의 `⚠️불일치` 각주와 [api/conventions.md](../api/conventions.md)
  > "Android 불일치" 2행을 이 라운드에 함께 걷었다(그 표는 이제 0건이다).
  > ⚠️ **다시 갈릴 조건은 남는다** — 앱은 계약이 내려주지 않는 03시를 상수로 복제했으므로 서버가 배치
  > 시각을 바꾸면 조용히 어긋난다. 그 조건이 `DayWindow.DAY_BOUNDARY_HOUR` KDoc에 적혀 있다.
  > ⚠️ 이 라운드가 회귀 하나를 만들었다 다시 닫았다 — `parfaitToday()`를 **부르지 않던** G-001 헤더와
  > 카메라 날짜 라벨이 기기 자정을 써서 00:00~03:00에 목록 D / 캔버스 D−1이 됐다. 최종 리뷰가 잡았다.

### [2026-08-18] Nametag-Chip 부여 주체가 서버로 정해졌는데 정책 문서에 그 규칙이 없다

- **ID**: OQ-P-223
- **출처**: 서버 `NameTagChipType`·`ParfaitGroupService#assignNametagChip`·`ParfaitGroupMember#leave`
  (2026-08-18 delta) × 위키 [[nametag-chip]] × [api/parfait-group.md](../api/parfait-group.md)
  "Nametag-Chip 배정 규칙" — 오래 열려 있던 "부여 주체" 질문(OQ-P-140 ①·OQ-P-210 ①)에 **서버가 코드로
  답했다.** 규칙은 이렇다: 참여·생성 시 **그 그룹의 활동 멤버가 안 쓰는 타입 중 무작위**, 탈퇴 시
  `RELEASED`로 반납, 닉네임 변경은 칩을 안 바꾸고 **재배정 경로는 없다.** 유일성은 **그룹 안**에서만
  성립해 같은 회원이 그룹마다 다른 색이다 — 위키가 "타입은 유저별 고정"이라고만 적고 **범위를 정하지
  않은 자리**를 코드가 그룹 단위로 메운 것이라, 정책 문서에는 여전히 근거가 없다. `RELEASED`라는
  13번째 값도 정책 밖이다.
- **항목**: ① 위키 [[nametag-chip]]에 부여 주체(서버)·유일성 범위(그룹 내, 계정 공통 아님)·반납을
  올릴지 — 올리면 "유저별 고정"의 뜻이 "그룹 안에서 고정"으로 좁혀진다. ② `RELEASED`의 표시 규칙이
  없다 — 앱 `YGColorChipType`은 12종 + `NametagChipPlus`뿐이고 대응 값이 없는데, 그룹 목록
  `lastPlacedByNametagChip`에는 **마지막 토퍼가 탈퇴하면 `RELEASED`가 온다**(그룹 상세 `members`에는
  안 온다 — 탈퇴자를 거른다). 캔버스 `placedBy`도 같다. ③ 배정 가능 타입 12종과 그룹 정원 12가 같아야
  한다는 전제가 서버 주석에만 있다 — 정원 정책이 바뀌면 후보 고갈로 **도메인 에러가 아니라 500**이 된다.
- **상태**: 미해결 (서버는 확정, 정책·앱이 안 따라옴)
- **해소 메모**: ①은 위키 소관(정책 SoT). ②③을 정하면
  [api/parfait-group.md](../api/parfait-group.md) "Nametag-Chip 배정 규칙"과
  [design-system](../architecture/design-system.md) `YGNametagChip` 항목에 반영한다. 앱이 필드를 읽는
  작업은 OQ-P-140·OQ-P-210의 잔여 항목이다.
  > 🔧 **전제 정정(2026-08-19, 서버 `57529ec`)** — 반납 값의 이름이 **`RELEASED` → `DEFAULT`**로 바뀌었다
  > (마이그레이션 V15가 기존 행 갱신 + 컬럼을 `NOT NULL DEFAULT 'DEFAULT'`로). 위 ②의 문자열을 그대로
  > 읽지 말 것. 성질도 하나 드러났다 — **`DEFAULT`는 `TYPE1`~`TYPE12`와 달리 유일성 제약이 없어 한 그룹
  > 안에서 여러 명이 동시에 가질 수 있다**(서버 enum 주석이 명시). 값이 실리는 자리도 늘어
  > 그룹 **생성** 응답과 캔버스 `groupMembers`까지 넷이 됐다. ②의 "표시 규칙" 질문은 앱에 `Default` 칩이
  > 생기면서(OQ-P-226) 색·대비 쟁점으로 옮겨 갔고, 여기서는 **정책 문서 공백**만 남는다.

### [2026-08-18] 칩 필드가 `placedBy`에만 붙어 캔버스 상단 멤버 칩은 여전히 계약 밖이다

- **ID**: OQ-P-224
- **출처**: 서버 `GetTodayParfaitUseCase`의 `GroupMemberResult`·`PlacedByResult`(2026-08-18 delta) ×
  `CanvasMainViewModel#toMemberChips` × [api/parfait.md](../api/parfait.md) — 같은 응답 안에서
  **토핑 작성자(`placedBy`)는 `nametagChip`을 받고 멤버 목록(`groupMembers`)은 못 받는다.** 그런데 C-001이
  상단에 그리는 칩은 `groupMembers`에서 오고, 그것이 7종 팔레트를 인덱스로 도는 자리다(OQ-P-210).
  즉 **서버가 칩을 주기 시작했는데 정작 이 화면의 문제는 안 닫혔다.** 반대로 `placedBy.nametagChip`은
  받을 수 있게 됐지만 앱 DTO에 필드가 없어 아직 아무도 안 읽는다. 그룹 상세(`members[].nametagChip`)와
  그룹 목록(`lastPlacedByNametagChip`)도 같은 상태다.
- **항목**: ① 서버에 `groupMembers[].nametagChip` 추가를 요청할지 — 상세/목록·`placedBy`가 이미 주므로
  **한 응답 안에서 두 목록의 표현이 갈린 것**이고, 안 주면 C-001만 영원히 자체 규칙을 쓴다.
  ② 앱이 필드 넷(`members[].nametagChip`·`lastPlacedByNametagChip`·`placedBy.nametagChip` +
  그룹 상세 `groupName`·`memberLimit`)을 읽는 라운드를 언제 잡을지 — 지금은 `ignoreUnknownKeys = true`
  덕에 깨지지 않고 **조용히 안 쓰이는 중**이다. ③ 서버 enum 문자열(`TYPE1`~`TYPE12`·`RELEASED`)을
  앱 `YGColorChipType`에 매핑하는 자리를 어디로 둘지(디자인시스템 타입은 `:core:designsystem`이고
  응답 매핑은 `:data`다 — 지금 둘 사이에 다리가 없다).
- **상태**: 해소됨 (서버가 ①을 닫고 앱이 ②③을 읽는다 — PR #308·#310 develop 머지 2026-08-20)
- **해소 메모**: ①은 다음 `sync-teamyg-server-api` 라운드 전에 서버팀에 요청해야 확인된다.
  ②③을 하면 OQ-P-140·OQ-P-210의 잔여 항목이 함께 닫히고
  [api/parfait.md](../api/parfait.md)·[api/parfait-group.md](../api/parfait-group.md) Android 매핑 절을 고친다.
  > ✅ **②③ 해소, ① 잔존(2026-08-18 구현, PR #308로 머지)** — 앱이 그룹 상세·목록의
  > 칩과 `groupName`·`memberLimit`을 읽는다. ③(매핑 자리)의 답은 **feature impl의 확장 함수 둘**이고,
  > 두 화면의 규칙이 달라(S-101 12→12 · G-001 12→6) 공용화하지 않았다.
  > **①은 그대로다** — 서버가 `groupMembers`에 칩을 안 줘서 C-001 상단 멤버 칩만 아직 인덱스 순환이고,
  > 그 결과 **같은 사람이 S-101과 C-001에서 다른 색으로 보인다**(전에는 양쪽 다 지어낸 값이라 모순이
  > 안 보였는데, 한쪽이 정본이 되면서 드러났다). 서버 요청이 선행이다.
  > `placedBy.nametagChip`은 DTO까지만 받아 둔 상태 그대로다(읽는 화면 0건).
  > ✅ **① 해소(2026-08-19, 서버 `57529ec`)** — 캔버스 조회 응답의 `groupMembers[]`에 `nameTagChip`이
  > 붙었고([api/parfait.md](../api/parfait.md)), 같은 delta가 **토핑 배치 응답 `placedBy`에도** 칩을 실었다
  > ([api/parfait-image.md](../api/parfait-image.md)). **"한 응답 안에서 두 목록의 표현이 갈렸다"는 조건이
  > 사라져** C-001 상단 칩을 계약으로 정할 수 있다 — 남은 것은 앱이 읽는 일이다.
  > ⚠️ **다만 그 delta가 JSON 키를 함께 바꿔(`nametagChip` → `nameTagChip`) ②에서 해소했다고 적은
  > 그 브랜치의 `@SerialName`이 전부 옛 키였다** → OQ-P-234(다음 라운드가 머지 전에 고쳤다).
  >
  > ✅ **①②③ 전부 해소 — 이 항목은 닫힌다(2026-08-19 구현, PR #310로 2026-08-20 develop 머지).**
  > 앱이 `CanvasMemberVO.nametagChip`을 읽고 `CanvasMainViewModel.toMemberChips`가 서버 값으로 색을
  > 정한다. **`NAMETAG_CHIP_PALETTE`가 삭제돼 팔레트 인덱스 순환이라는 개념 자체가 사라졌고, 그래서
  > OQ-P-210 ②(팔레트가 12종 중 7종만 도는 근거 없음)도 함께 소멸**했다. 최종 리뷰가 서버 코드로
  > 확인한 바, S-101과 C-001이 같은 `parfait_group_member.nametag_chip` 행에서 값을 받으므로
  > **같은 사람이 두 화면에서 같은 색**이 된다 — 이 항목이 열린 이유였던 모순이 사라졌다.
  > 옛 키 문제도 같은 라운드가 닫았다(OQ-P-234 ①).
  > ✅ **develop 확인(2026-08-20, `750cc2dd`)** — `git grep NAMETAG_CHIP_PALETTE origin/develop`이 0건이고
  > 세 모듈의 `util/` 변환이 `NametagChipType`을 받는다. **`placedBy.nameTagChip`만 DTO에서 멈춰 있다** —
  > 읽는 화면이 0건이라 남긴 것이고, C-202 Spotlight는 이 필드가 아니라 `groupMembers` 조인을 쓰므로
  > 그 화면이 붙어도 자동으로 필요해지지는 않는다.
  > 📌 **그 예측대로 붙었다(2026-08-20, PR #298)** — Spotlight 토스트가 작성자 칩을 얻는 방법이
  > `memberChips`에서 `groupMemberId`로 찾는 조인이고, 코드에 "서버 값이 도메인까지 오면 바꾼다"는
  > `TODO`가 붙어 있다. 그래서 `placedBy.nameTagChip`은 **읽는 화면이 생긴 뒤에도 여전히 DTO에서
  > 멈춰 있다** — 다만 조인은 목록에 없는 사람(탈퇴·이탈)을 `Default`로 떨어뜨려 정책과 갈린다
  > → OQ-P-251.

### [2026-08-18] 상세 조회가 빈 캐시로 실패하면 S-101이 "정원이 찼어요"로 거짓말한다

- **ID**: OQ-P-225
- **출처**: `GroupSettingUiState.remainingCount`(기본값 `0`) × `GroupSettingScreen`의 `<= 0` 분기 ×
  `YGInviteCard`(`status != Active`면 복사 버튼 비활성) — 서버 delta 반영 라운드에서 정원이 실데이터가
  되면서 mock 상수 `1`이 사라졌고 기본값이 `0`이 됐다. `loadGroupDetail`의 `finally`가 실패 경로에서도
  `isLoadingDetail`을 내리므로, **캐시가 빈 채로 상세 조회가 실패하면** 로딩 덮개가 걷히고 화면이
  "정원이 찼어요" + 죽은 초대코드 복사 버튼을 띄운다. 전에는 mock `1` 덕에 우연히 Active로 보였다.
  화면 나머지도 비어 있어 노출은 작지만, **비어 있는 것과 "가득 찼다"고 말하는 것은 다르다.**
- **항목**: ① `remainingCount`를 널 허용으로 바꿔 "모른다"를 표현할지, 아니면 상세가 도착하기 전까지
  초대 카드를 아예 안 그릴지. ② 상세 조회 실패가 지금 토스트 하나로 끝나는데(화면은 그대로 서 있다)
  실패한 화면을 어떤 상태로 둘지 — 재시도 동선이 없다는 기존 미결(OQ-P-218)과 같은 자리다.
  ③ 정원과 멤버 수가 어긋나 음수가 나오는 경우는 `coerceAtLeast(0)`로 접었는데, 그 값도 "가득 찼다"와
  같은 표현을 탄다(0이 두 가지를 뜻한다).
- **상태**: 미해결
- **해소 메모**: 최종 브랜치 리뷰가 Minor로 짚었고 컨트롤러가 park했다 — 실패 표현은 이 라운드 스펙의
  범위 밖이다. 정하면 [api/parfait-group.md](../api/parfait-group.md) Android 매핑과
  [s101 스펙](../superpowers/specs/archive/2026-08-17-s101-group-setting-api.md) 드리프트 표에 반영한다.

### [2026-08-18] Default 칩이 기존 타입과 잘 구분되지 않고, 글자 대비가 접근성 기준에 못 미친다

- **ID**: OQ-P-226
- **출처**: `YGColorChipType.Default`·`YGGrouptagChipType.DEFAULT`(Figma Nametag-Chip `144:5415` ·
  Grouptag-Chip `3733:9410` 토큰을 그대로 옮긴 것) × 독립 코드 리뷰(2026-08-18) — 폴백 칩이 생기면서
  두 가지가 드러났다. ① **Grouptag `DEFAULT`(Gray300)와 기존 `TYPE_7_8`(Gray200)의 타임스탬프 색이
  목록에서 사실상 같은 회색**이라, "마지막 토퍼가 나간 그룹"과 "TYPE7 사용자가 올린 그룹"이 구분되지
  않는다 — 이 라운드가 방금 의미를 부여한 신호가 폴백 자리에서 도로 흐려진다. ② **Nametag `Default`의
  글자색(Gray300) on 배경(White) 대비가 WCAG AA에 못 미친다**(`YGUserChip`이 닉네임 첫 글자를 그 색으로
  그린다). `NametagChipPlus`와 fill·stroke가 같고 글자색만 달라 둘의 구분도 글자에만 걸려 있다.
- **항목**: ① 폴백 색을 기존 타입과 겹치지 않는 값으로 바꿀지, 아니면 색 말고 다른 수단(모양·아이콘)으로
  "가리킬 사람이 없음"을 표현할지. ② `Default` 글자 대비를 올릴지 — 올리면 Figma 토큰과 갈리므로 디자인
  소관이다. ③ `Default`와 `NametagChipPlus`가 같은 컨테이너를 쓰는 것이 의도인지(Figma는 두 변형을 한
  컨테이너 스타일로 묶어 놨다).
- **상태**: 미해결
- **해소 메모**: 코드 결함이 아니라 디자인에 되물을 항목이다 — 토큰을 그대로 옮긴 결과다. 같은 타입을
  추가하는 PR #298과 함께 정해야 한다(정의가 글자까지 같다). 정하면
  [design-system](../architecture/design-system.md) `YGNametagChip` 항목에 반영한다.
  ⚠️ **2026-08-20부터 이 항목에 걸린 것이 하나 늘었다** — [ADR-0024](../adr/0024-nametag-chip-unknown-fold.md)가
  앱이 모르는 칩 문자열까지 `DEFAULT`로 접으면서, 그 폴백 색이 이제 **"반납된 자리"·"서버가 늘린 새 타입"·
  "구버전 서버"** 셋을 한꺼번에 표현한다. 위 ①에서 색을 갈라 "가리킬 사람이 없음"을 따로 표현하기로
  정하는 순간 ADR-0024의 재검토 트리거가 걸린다(데이터 레이어가 이미 셋을 합쳤으므로 화면에서
  되돌릴 수 없고, 매퍼와 색 변환 셋을 함께 고쳐야 한다).

### [2026-08-18] 신규 응답 필드 둘이 비널이라 구버전 서버를 만나면 화면이 통째로 실패한다

- **ID**: OQ-P-227
- **출처**: `MyParfaitGroupDetailResponse.groupName`·`.memberLimit`(기본값 없는 non-null) ×
  독립 코드 리뷰(2026-08-18) — 서버 계약이 두 필드를 비널로 주므로 DTO도 비널이고, 이는 "DTO는 서버의
  거울"이라는 규약대로다. 다만 그 결과 **`08df1bf` 이전 서버**(스테이징·롤백·구버전 배포)를 만나면
  `kotlinx.serialization`이 `MissingFieldException`을 던져 S-101 그룹 설정 조회가 통째로 실패한다.
  기본값을 주면 계약 위반이 조용히 지나가므로 **큰 소리로 깨지는 편이 옳다**고 보고 그대로 뒀지만,
  그 판단은 "앱과 서버가 항상 같이 배포된다"를 전제한다 — 그 전제는 어디에도 안 적혀 있다.
- **항목**: ① 앱↔서버 배포 순서를 규약으로 적을지(적으면 이 항목은 닫힌다). ② 적지 않는다면 신규
  비널 필드에 대한 방침을 정해야 한다 — 기본값을 줄지, 아니면 실패를 잡아 화면 단위로 알릴지.
  ③ 이 위험은 이 라운드 두 필드만의 것이 아니다. 앞으로 서버가 비널 필드를 더할 때마다 같은 자리가 생긴다.
- **상태**: 미해결
- **해소 메모**: 실서버 요청 검증이 0건이라 아직 드러난 적이 없다. 정하면
  [api/conventions.md](../api/conventions.md) 직렬화 규약 절과
  [data-layer](../architecture/data-layer.md)에 반영한다.

### [2026-08-18] 원본 다운샘플 미적용 — 세그멘테이션 실측 메모리 피크가 largeHeap 없이 위험 구간
- **ID**: OQ-P-228
- **출처**: `ImageSegmentationRepositoryImpl.kt#segmentImage`(원본 해상도 그대로 처리, 다운샘플
  없음) × 독립 코드 리뷰(2026-08-18, `refactor/segmentation-logic` 최종 리뷰) — ~~12MP 사진 기준
  실측: `segmentImage` 내부에 살아 있는 비트맵 총량이 피크에서 약 244MB, 토핑 편집 화면까지
  이어지면(스택 아래 `SegmentationState.originBitmap`이 겹쳐 살아 있는 채) 약 390MB.~~ `app` 모듈
  매니페스트는 `largeHeap`을 선언하지 않는다.
  [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)이
  "고화질 보존을 택했다"며 다운샘플을 의도적으로 범위 밖에 뒀는데, 리뷰는 그 판단이 틀렸다고 본다.
  같은 저장소의 `ToppingBorderOutline.kt`는 이미 자기 작업 치수를 캡핑하고 있어(원본 그대로 돌리지
  않는다), 다운샘플 여부 판단이 코드베이스 안에서 화면마다 갈린다.
  > 🔧 **수치 정정(2026-08-19)** — 위 취소선 두 수치는 develop에 미머지인 PR #290을 로컬 머지해
  > 얹은 트리에서 잰 값이라 지금은 틀렸다. 같은 작업을 plain develop 위
  > `refactor/segmentation-develop`로 다시 만들면서 재도출했다.
  > - **`segmentImage`(`ImageSegmentationRepositoryImpl.kt#segmentImage`) — 약 195MB로 정정.**
  >   #290이 만들던 trimmed 비트맵(`trimTransparentBounds`)이 develop에는 없어, 동시에 살아 있는
  >   전체 해상도 버퍼가 다섯이 아니라 **넷**이다 — 원본 비트맵, ML Kit
  >   `foregroundConfidenceMask`(픽셀당 4바이트 `FloatBuffer`), 픽셀 `IntArray`, subject 비트맵.
  >   넷 다 4바이트/픽셀이라 12MP(예: 4032×3024 = 12,192,768픽셀) 기준 16바이트 × 12,192,768 ≈
  >   195MB. 옛 244MB는 다섯 번째 trimmed 버퍼(#290 전용)를 더한 값이었다.
  > - **토핑 편집 화면(`ToppingEditViewModel.completeEdit`) 값은 재도출하지 않는다.** 옛 390MB는
  >   #290의 `trimTransparentBounds` 경로를 포함해 잰 값이라 그대로 못 쓰고, 이 화면은
  >   `originBitmap`·`segmentationBitmap`에 `buildCutoutBitmap`의 `cutout`·`withBorders`의
  >   `edited`까지 겹치는 구간이 있어 정확한 피크를 내려면 `ToppingBorderOutline`의 치수 캡핑까지
  >   따라가야 한다 — 이번 정정에서는 그 도출을 하지 않았다. **현재 트리 기준 토핑 편집 피크는
  >   미측정으로 남는다.** 재측정 없이 옛 390MB를 새 수치인 것처럼 쓰지 않는다.
  > - 위험 자체(다운샘플 상한 없음, `largeHeap` 미선언)는 안 바뀐다. 항목 ①~③과 상태는 그대로
  >   둔다.
  >
  > 🔧 **위 정정의 전제가 하루 만에 사라졌다(2026-08-19, PR #290 머지)** — 정정이 "develop에는 없다"고
  > 적은 trimmed 비트맵(`trimTransparentBounds`·`:data`의 bounding box 절단)이 **develop에 들어왔다.**
  > 즉 `segmentImage`의 동시 생존 전체 해상도 버퍼는 넷이 아니라 **다섯**이고, 위 195MB 도출은 지금
  > 트리에 맞지 않는다. 옛 244MB 쪽이 다시 맞는 모양이지만 **그 값도 다른 트리에서 잰 것이라 그대로
  > 되살리지 않는다** — 현재 develop 기준 피크는 세그멘테이션·토핑 편집 **둘 다 미측정으로 남긴다.**
  > 토핑 편집은 부담이 한 겹 더 늘었다: `ToppingEditViewModel.completeEdit`이 `cutout`·`edited`에
  > 더해 `trimmedEdited`까지 만들고, **"테두리가 없으면 같은 파일을 두 번 떨구지 않는다" 최적화가
  > 사라져** 파일 저장도 항상 두 번이다.
  >
  > 📌 **세그멘테이션 라운드가 머지됐다(2026-08-20, PR #309)** — 이 항목이 겨눈 `segmentImage`는
  > 이제 develop에서 `getPixels` 1회 + 배열 내 마스킹으로 돌고 저장 구간이 `try`/`finally`로 감싸여
  > `subjectBitmap`·trimmed 비트맵의 회수가 실패 경로에서도 보장된다. **동시 생존 버퍼가 준 것은
  > 아니다** — 배열 하나를 아끼는 재작성이었고 다운샘플 상한은 여전히 없으며 `largeHeap`도 그대로
  > 미선언이다. 위 두 실측치는 다른 트리에서 잰 값이라 되살리지 않는다 — **현재 develop 기준 피크는
  > 세그멘테이션·토핑 편집 둘 다 미측정으로 남는다.** 항목 ①~③과 상태는 그대로다.
- **항목**: ① 원본 디코드에 다운샘플 상한을 둘지, 둔다면 어느 화면(세그멘테이션만 vs 디코드 공통
  경로)·어느 치수부터 적용할지. ② 다운샘플 대신 `largeHeap` 선언으로 버틸지(저사양 기기에서 여전히
  위험할 수 있다). ③ `ToppingBorderOutline`이 쓰는 치수 캡 관용구를 세그멘테이션 파이프라인 전체의
  표준으로 승격할지.
- **상태**: 미해결
- **해소 메모**: 다운샘플 도입 또는 `largeHeap` 채택 시
  [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)
  "주의 / 열린 질문"과 [ADR-0012](../adr/0012-mlkit-subject-segmentation.md)를 함께 갱신한다.

### [2026-08-18] 앱 진입이 로띠 재생 길이에 묶였다 — 상한도 하한도 없다

- **ID**: OQ-P-229
- **출처**: `feature/intro/impl` `splash/SplashViewModel.kt`(`SplashState.isAnimationFinished`·
  `navigateIfReady`)·`splash/SplashScreen.kt`(PR #305 develop 머지) — 부트스트랩 응답과 로띠 재생
  종료가 **둘 다** 끝나야 이동한다. 서버가 빨라도 애니메이션이 끝날 때까지 기다리고, 반대로 느린
  기기에서 재생이 늘어지면 진입도 그만큼 늦는다. [user-info-ssot 스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md)이
  `SplashInitialUseCase`를 지우며 "최소 노출 시간 요구가 없다"고 적은 자리에 **다른 이유로 최소 노출이
  되돌아온 것**이다. 재생 시간·기기별 프레임 드롭은 측정된 바 없고 실기기 확인도 없다.

  > 📌 **로띠가 통째로 갈렸는데 대기 길이는 그대로다(2026-09-03, PR #444)** — 스플래시 애셋이 새 로고
  > 애니메이션으로 교체됐다. dotLottie 를 풀어 보면 프레임률·재생 길이·레이어 구성이 교체 전과 같아
  > (60fps·3.5초, 위키 [[스플래시-애니메이션]] A-001 정본과 일치) **이 항목이 묻는 대기 길이는 한
  > 프레임도 안 움직였다.** 바뀐 것은 각 레이어의 패스·키프레임뿐이다. ①②③ 전부 그대로 열려 있고,
  > 실기기 측정도 여전히 0건이다.
- **항목**: ① 대기 상한을 둘지(예: 응답이 끝났고 N초가 지나면 재생을 끊고 넘어간다). ② 반대로 최소
  노출을 의도로 인정하고 그 값을 디자인·기획이 정할지 — 지금은 애셋 길이가 곧 정책이다.
  ③ 저사양 기기에서 첫 프레임까지 로띠 파싱이 얼마나 걸리는지 실측 — 파싱 실패는 '끝'으로 넘기지만
  **느린 파싱은 그냥 대기**다.
- **상태**: 미해결 (실기기 측정 0건)
- **해소 메모**: 정하면 [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인"과
  [user-info-ssot 스펙](../superpowers/specs/archive/2026-08-15-user-info-ssot.md)의 스플래시 항목을 함께 고친다.
  OQ-P-187(구버전 콜드 스타트 첫 프레임)과 같은 회차에 실기기로 보는 것이 낫다.

### [2026-08-18] 로띠 재생 관용구가 둘로 갈렸다 — 디자인시스템 표면 vs 화면 직접 호출

- **ID**: OQ-P-230
- **출처**: `core:designsystem` `component/ygloading/YGLoadingLottie.kt`(공용 표면, `progress` 유무로
  반복·추종을 가른다) × `feature/intro/impl` `splash/SplashScreen.kt`(`rememberLottieComposition` +
  `LottieAnimation`을 직접 호출, 모듈 `build.gradle.kts`에 `libs.lottie.compose`를 따로 단다) —
  같은 PR(#305)이 공용 표면을 만들면서 스플래시는 그것을 쓰지 않았다. 이유는 설명 가능하다(스플래시는
  **재생 종료 시점**과 **파싱 실패**를 알아야 하는데 `YGLoadingLottie`는 그 둘을 밖으로 내지 않는다).
  결과적으로 로띠 의존이 두 모듈에 각각 선언돼 있고 `build-logic` `ComposeConfig`에는 없다
  (haze·coil과 다른 배선 형태다).
- **항목**: ① `YGLoadingLottie`에 종료·실패 콜백을 더해 스플래시도 흡수할지, 아니면 "로딩 표시"와
  "연출 애니메이션"을 다른 갈래로 못박을지. ② 로띠 의존을 `ComposeConfig`로 올릴지, 쓰는 모듈이
  각자 달게 둘지 — 지금은 후자이고 세 번째 화면이 생기면 같은 줄이 또 늘어난다.
- **상태**: 미해결
- **해소 메모**: 정하면 [design-system](../architecture/design-system.md) "로띠 의존" 절과
  [ygscaffold-v2 스펙](../superpowers/specs/archive/2026-08-16-ygscaffold-v2-common-loading-error.md)에 적는다.
  OQ-P-125(에셋 소유)와 같은 축이다 — 그쪽이 파일 위치, 이쪽이 호출 표면이다.

### [2026-08-18] 설정 화면의 약관 탭이 조용히 아무 일도 하지 않는 경로를 갖는다

- **ID**: OQ-P-231
- **출처**: `feature/app/setting/impl` `viewmodel/AppSettingViewModel.kt`(`loadPolicies`·
  `handleClickPolicy`, PR #296 develop 머지) — 약관 두 줄은 `strings.xml`로 고정돼 있어 조회가
  실패해도 사라지지 않는데, 목록이 비었거나 그 종류가 없으면 **탭해도 로그만 남고 화면이 그대로다.**
  재조회하지 않는 것은 의도이고 근거도 코드에 적혀 있다(같은 요청은 같은 응답이라 탭마다 요청만 는다).
  문제는 **사용자에게 "실패"와 "반응 없음"이 구분되지 않는다**는 것이다 — 같은 화면의 로그아웃은
  실패를 토스트로 알리는데(`YGScaffoldV2`가 이미 붙어 있다) 약관 쪽은 그 자리를 안 쓴다. 온보딩
  약관 화면은 조회 실패에 재시도 문구가 있어 **같은 API의 실패 표현이 화면마다 갈린다**.
- **항목**: ① 실패·누락 시 토스트를 띄울지(호스트는 이미 있다). ② 아니면 조회가 실패한 동안 두 줄을
  비활성으로 보일지 — 문구가 고정이라 "누를 수 있어 보이는데 안 눌린다"가 현재 상태다.
  ③ 같은 API 실패 표현을 두 화면에서 맞출지(OQ-P-167 실패 표현 갈래와 같은 축).
- **상태**: 미해결
- **해소 메모**: 고치면 [app-setting-s001 스펙](../superpowers/specs/archive/2026-07-19-app-setting-s001.md)
  as-built와 [api/policy.md](../api/policy.md) "앱 동작 메모"를 함께 갱신한다.

### [2026-08-18] 웹뷰 목적지가 임의 `title`·`url`을 받는 범용 화면이 됐는데 출처 검증이 없다

- **ID**: OQ-P-232
- **출처**: `feature/common/terms/api` `NavKeyWebView.kt`·`impl` `WebViewRoute.kt`·
  `component/NotionWebView.kt`(PR #296 develop 머지) — 목적지가 문자열 둘을 받아 그대로
  `WebView.loadUrl`에 넣는다. `NotionWebView`는 JavaScript·DOM storage를 켜 두고(노션 렌더 때문),
  스킴·호스트 화이트리스트나 리다이렉트 제한은 없다. 지금 호출자는 둘 다 서버 응답 값을 싣지만
  (`GET /api/v1/policies`), 신뢰 경계는 **서버가 주는 문자열**까지 넓어졌고 NavKey는 `@Serializable`이라
  그 값이 백스택 상태로 직렬화된다. 이름도 옛 전제를 이고 있다 — 여는 주소가 노션이라는 보장은 없다.
- **항목**: ① 허용 스킴(`https`만)·호스트를 Route나 컴포넌트에서 검증할지. ② 리다이렉트를
  `shouldOverrideUrlLoading`으로 가둘지. ③ `NotionWebView`를 이름·역할대로 일반 웹뷰로 정리할지
  (그러면 JS 활성이 기본이어야 하는지도 다시 판단해야 한다).
- **상태**: 미해결 (지금 실제 위험은 낮다 — 호출자 둘 다 서버 응답만 싣는다)
- **해소 메모**: 정하면 [s004-terms-privacy-webview 스펙](../superpowers/specs/archive/2026-07-20-s004-terms-privacy-webview.md)
  as-built와 [module-structure](../architecture/module-structure.md) terms 모듈 행에 적는다.
  OQ-P-068(응답 `url`이 링크가 아닐 수 있다)과 같은 파일에서 만난다.

### [2026-08-18] 앱 버전이 `:app` 밖에서 카탈로그를 다시 읽는다 — 접미사·플레이버가 생기면 조용히 갈린다

- **ID**: OQ-P-233
- **출처**: `core/util/android/build.gradle.kts`(`buildFeatures.buildConfig = true` +
  `buildConfigField("String", "APP_VERSION_NAME", libs.versions.appVersionName)`)·
  `core/util/android/.../AppInfo.kt`(PR #295 develop 머지) — `versionName`은 애플리케이션 모듈
  속성이라 라이브러리 `BuildConfig`에 없어서, `:app`이 쓰는 것과 **같은 카탈로그 항목을 이 모듈
  빌드에도 심는다.** 출처가 하나라 지금은 어긋나지 않지만 `:app`이 `versionNameSuffix`나 플레이버로
  버전을 갈아 끼우면 **설정 화면만 옛 값을 보여 준다.** KDoc이 이 함정과 대안(`PackageManager`로
  설치된 패키지를 읽기)을 적어 두었으나 코드는 그대로다. 겸해서 `core:util:android`가 저장소에서
  `buildConfig`를 켜는 첫 라이브러리 모듈이 됐다.
- **항목**: ① 접미사·플레이버가 실제로 생길 때 `PackageManager` 읽기로 옮길지, 아니면 그런 빌드를
  만들지 않기로 못박을지. ② 버전 노출이 `core:util:android`에 있을 일인지(`:app`이 DI로 내려주는
  형태를 브랜치 안에서 한 번 거쳤다가 상수로 되돌아왔다 — 그 판단의 근거가 코드에만 있다).
- **상태**: 미해결 (현재 값은 정확하다 — 갈릴 조건이 아직 없다)
- **해소 메모**: 정하면 [module-structure](../architecture/module-structure.md) `core:util:android` 행과
  [app-setting-s001 스펙](../superpowers/specs/archive/2026-07-19-app-setting-s001.md) 버전 항목을 함께 고친다.

### [2026-08-19] 서버가 칩 필드의 JSON 키를 바꿨는데 그 필드를 읽는 브랜치는 옛 키를 들고 있다

- **ID**: OQ-P-234
- **출처**: 서버 `ParfaitGroupResponse`·`GetTodayParfaitResponse`·`PlaceParfaitImageResponse`
  (2026-08-19 delta `57529ec`, `fix: placedBy 스키마 이름 충돌 해소 및 nameTagChip 필드명을 스펙에 맞게 통일`)
  × 앱 브랜치 `feature/#294-group-ssot`의 `MyParfaitGroupResponse`·`ParfaitGroupMemberResponse`·
  `GetTodayParfaitResponse` — 응답 JSON 키가 **`nametagChip` → `nameTagChip`**,
  **`lastPlacedByNametagChip` → `lastPlacedByNameTagChip`**으로 바뀌었다(서버 코어·도메인·영속성의
  내부 프로퍼티명은 그대로이고 HTTP DTO 경계에서만 바뀌었다 — 그래서 서버 코드를 훑으면 옛 이름이 계속
  보인다). develop에는 이 필드를 읽는 코드가 아직 없어 무해하지만, **OQ-P-224 ②를 해소한 그 브랜치가
  옛 키로 `@SerialName`을 달아 뒀다.** 셋 다 `String? = null`이라 `MissingFieldException`도 안 나고
  **값이 조용히 `null`로 떨어져 칩이 전부 폴백으로 그려진다.** 같은 브랜치의 `NametagChipType.RELEASED`와
  그것을 설명하는 KDoc도 서버가 더는 보내지 않는 문자열이다(지금은 `DEFAULT`, → OQ-P-223).
- **항목**: ① 머지 전에 키 셋과 enum 값을 서버에 맞출지(가장 단순한 답이고 지금 유일하게 옳은 답이다).
  ② **널 기본값이 이 부류의 사고를 숨긴다** — OQ-P-227이 "기본값을 주면 계약 위반이 조용히 지나가니
  큰 소리로 깨지는 편이 옳다"고 정한 방침과 이 세 필드가 반대다. 칩 필드에도 그 방침을 적용할지,
  아니면 "표시용 부가 필드는 널 허용"이라는 예외를 명시할지. ③ 키 이름 변경을 **계약 문서 대조 말고
  다른 수단으로 잡을 방법**이 있는지 — 이번에도 잡은 것은 서버 delta 감사이고, 앱 테스트는 자기 DTO를
  자기가 만들어 넣으므로 절대 못 잡는다(와이어 계약 테스트가 있는 곳은 `isNewUser` 하나뿐이다).
- **상태**: ①② 해소(PR #310 develop 머지 2026-08-20) / **③ 미해결**
- **해소 메모**: 고치면 [api/parfait-group.md](../api/parfait-group.md)·[api/parfait.md](../api/parfait.md)
  Android 매핑의 경고를 걷고 OQ-P-224를 닫는다. ②는 [api/conventions.md](../api/conventions.md)
  직렬화 규약과 [data-layer](../architecture/data-layer.md)에 적는다(OQ-P-227과 한 자리).
  > ✅ **①② 해소(2026-08-19 구현, PR #310로 2026-08-20 develop 머지)** — 세 DTO의 키를
  > `nameTagChip` 계열로 맞추고 `NametagChipType.RELEASED`를 `DEFAULT`로 바꿨다
  > ([plan](../superpowers/plans/archive/2026-08-19-server-delta-nametag-chip-keys.md) Task 2·3). ②(널 기본값 방침)는
  > **널 허용을 유지하기로 답했다** — 구버전 서버·롤백을 만나도 화면이 통째로 실패하지 않는 쪽을 골랐고,
  > 그 결정을 지키는 테스트(`getMyGroups_missingUploadedAt_isNull`)를 같은 라운드가 붙였다.
  > ⚠️ **③은 그대로다.** 이번에도 키 어긋남을 잡은 것은 `sync-teamyg-server-api` 감사였다.
  > 최종 리뷰가 "비용 대비 이득이 심하게 기울어 있다 — 파일 하나에 세 DTO의 JSON 디코드 단언이면
  > 끝이고 선례(`KakaoLoginResponseSerializationTest`)까지 있다"며 **다음 라운드 최우선**으로 권했다.
  > ⚠️ **머지로 ③의 성격이 바뀌었다(2026-08-20)** — 그전까지 이 키들을 읽는 코드는 브랜치에만 있었고
  > develop은 필드를 안 읽어 무해했다. 지금은 **develop이 네 자리에서 이 키로 색을 정한다**(S-101 멤버
  > 칩 · G-001 그룹 칩 · C-001 상단 칩 · 목록 시각). 다음 키 변경은 브랜치가 아니라 **출시 가능한
  > develop을 조용히 폴백 색으로 만든다.**

### [2026-08-19] `recentImageUploadedAt`이 비널이 되면서 두 가지를 뜻하게 됐다

- **ID**: OQ-P-235
- **출처**: 서버 `ParfaitGroupMemberRepository.findMyGroupSummaries`(두 서브쿼리에 `COALESCE`) ·
  `ParfaitGroupService.create`(`savedGroup.updatedAt`) × [api/parfait-group.md](../api/parfait-group.md) —
  토핑이 0건인 그룹도 이제 값을 받는다: 시각은 **그룹 생성 시각**, 칩은 **생성자의 칩**이다. 필드 이름은
  그대로 `recentImageUploadedAt`이라 **"마지막 토핑 시각"과 "그룹 생성 시각"이 한 필드에 섞였고**,
  소비 측이 가르는 수단은 `recentImageUrl`이 `null`인지뿐이다(그 필드만 널을 유지했다). G-001 목록이
  이 값으로 경과 시간을 그리므로 **활동이 0건인 그룹도 활동이 있었던 것처럼 보인다.** 겸해서 같은 필드의
  출처가 엔드포인트마다 다르다 — 목록은 `parfait_group.created_at`, 생성 응답은 `updatedAt`이다.
- **항목**: ① 앱이 `recentImageUrl == null`을 "활동 없음"으로 읽어 표시를 가를지, 아니면 서버에
  구분 필드를 요청할지. ② 필드 이름과 뜻이 어긋난 것을 계약 문서 비고로만 둘지 서버에 되물을지.
  ③ 생성·목록의 출처 불일치(`updatedAt` vs `created_at`)가 의도인지 — 생성 직후에는 같지만 그룹 행이
  갱신되면 갈린다.
- **상태**: 미해결
- **해소 메모**: 정하면 [api/parfait-group.md](../api/parfait-group.md) 목록·생성 응답 절과 G-001
  화면 쪽 표기 규칙에 반영한다. **이 변경이 기존 파싱 불일치를 상시화한 것은 별개 항목이었고 닫혔다** —
  앱 매퍼의 `?.let`이 널일 때만 파싱을 건너뛰었는데 그 우회로가 사라져 그룹이 하나라도 있으면 G-001
  목록이 실패하던 것을 PR #310이 고쳤다([2026-08-15] 항목 · [api/conventions.md](../api/conventions.md)
  "Android 불일치"는 이제 0건).
  ⚠️ **이 항목 자체는 그대로 열려 있다** — 파싱이 고쳐졌다고 "한 필드가 두 뜻을 겸한다"가 해소되는
  것은 아니다. **오히려 develop이 그 값을 실제로 그리기 시작해 사정거리가 넓어졌다**(2026-08-20,
  PR #308·#310) — G-001이 이제 서버 시각과 서버 칩을 함께 쓰므로, 토핑 0건 그룹은 **생성 시각을 활동
  시각처럼**, **생성자 칩을 마지막 토퍼 칩처럼** 보여 준다. 코드 변경 없이 화면 값이 바뀐 자리다.
  🔁 **2026-08-31 서버 delta(`02e11be`)가 ①의 선택지를 없앴다** — `recentImageUrl`이 **오늘 캔버스**의
  토핑으로 좁혀지면서, 그 필드가 `null`인 것은 "토핑 0건"이 아니라 "오늘 토핑 없음"을 뜻하게 됐다.
  **응답에서 두 뜻을 가르는 수단이 사라졌다.** 같은 delta가 시각 쪽은 고쳤다 — 빈 오늘 캔버스가
  `COALESCE`로 그룹 생성 시각을 새게 하던 자리가 막혀, 토핑이 한 건이라도 있는 그룹은 이제 **정말
  마지막 토핑 시각**을 받는다. 그래서 이 항목의 사정거리는 **토핑이 한 건도 없는 그룹**으로 좁아졌고,
  대신 앱이 옛 판별법을 그대로 쓰는 새 자리가 생겼다 → OQ-P-336.

### [2026-08-19] 서버가 `PlacedByResponse`를 개명해 앱 DTO 이름의 근거가 사라졌다

- **ID**: OQ-P-236
- **출처**: 서버 `PlaceParfaitImagePlacedByResult`/`PlaceParfaitImagePlacedByResponse`(2026-08-19 개명) ×
  앱 `data/service/model/response/parfait/PlacedByResponse`·`response/parfaitimage/PlacedByResponse` ×
  [data-layer](../architecture/data-layer.md) — 앱이 같은 이름을 두 패키지에 둔 근거는 **"서버가 그렇다"**
  였다(중첩 응답을 상위 응답 파일에 함께 두는 예외도 같은 근거다). 서버는 springdoc이 두 스키마를 같은
  것으로 취급해 캔버스 쪽에 추가한 칩 필드가 스웨거에 안 나오는 문제 때문에 **토핑 배치 쪽만 개명**했고,
  그래서 지금 앱 DTO는 서버의 거울이 아니다. 겸해서 그 응답에 생긴 `placedBy.nameTagChip`을 앱
  DTO·`PlacedToppingVO`가 안 읽는다.
- **항목**: ① 앱도 개명해 거울을 유지할지, 아니면 "패키지가 다르면 같은 이름을 허용한다"를 규약으로
  못박을지(후자면 `data-layer`의 근거 문장을 바꿔야 한다). ② 배치 응답의 칩을 읽을지 — 방금 배치한
  사람의 칩을 재조회 없이 쓸 수 있게 하려는 것이 서버의 의도인데, 앱은 배치 후 캔버스를 다시 그리는
  경로가 아직 없다(토핑 배치 소비처 0건). ③ 이런 "이름만 바뀐 서버 변경"을 어느 계층까지 따라갈지 —
  wire DTO는 따라가야 계약 대조가 되지만 VO·도메인은 제품 언어라 따라가지 않는다.
- **상태**: ① 해소(PR #310 develop 머지 2026-08-20) / **②③ 미해결**
- **해소 메모**: 정하면 [api/parfait-image.md](../api/parfait-image.md)·[api/parfait.md](../api/parfait.md)
  응답 DTO 절과 [data-layer](../architecture/data-layer.md) "선언당 파일 하나" 예외 문단을 함께 고친다.
  > ✅ **① 해소(2026-08-20, PR #310 develop 머지)** — 앱도 개명해 **거울을 유지하는 쪽**을 골랐다
  > (`response/parfaitimage`의 것만 `PlaceParfaitImagePlacedByResponse`로, 캔버스 쪽은 그대로). 그래서
  > `data-layer`의 근거 문장은 고칠 것이 없다. 이름이 길어진 대가로 두 패키지에 같은 이름을 두던 모양이
  > 사라졌고, "통일해야 하나" 오해를 막으려 **양쪽 KDoc이 서로를 가리킨다**(그 두 줄은 주석 정리
  > 커밋이 한 번 지웠다가 리뷰가 되살린 것이다).
  > **②는 그대로다** — 칩 필드를 DTO까지만 받고 `PlacedToppingVO`에는 안 올렸다(읽는 화면 0건).
  > ③(이름만 바뀐 서버 변경을 어느 계층까지 따라가는가)도 답이 안 적혔다 — 이번 사례가 "wire DTO는
  > 따라간다"의 두 번째 선례가 됐을 뿐이다.

<!--
항목 추가 형식:

### [YYYY-MM-DD] [주제 요약]
- **출처**: `경로/파일` — 근거 (라인번호·변동수치 금지, 파일명+심볼명)
- **항목**: 결정해야 할 것
- **상태**: 미해결 | 해소됨 | 보류
- **해소 메모**: 해소 시 어느 ADR/architecture에 반영했는지
-->

### [2026-08-19] 시각 하나가 예상 밖 모양이면 G-001 목록이 통째로 빈다

- **ID**: OQ-P-237
- **출처**: `data/source/group/mapper/VOMapper.kt#toMyParfaitGroupVO`(`LocalDateTime::parse`가 던진다) ×
  `data/network/ApiCaller.kt#runCatchingApi`(`transform`을 `try` 블록 **안에서** 부르고 마지막
  `catch (e: Exception)`이 `ApiException.Unknown`으로 접는다) × 최종 리뷰(2026-08-19) — 그룹 열둘 중
  **하나**의 `recentImageUploadedAt`만 예상 밖 모양이어도 목록 전체가 `AppError.Unexpected`가 돼
  화면이 빈다. 카드 한 장이 아니라 목록이 실패 단위다.
  같은 라운드가 **같은 응답의 다른 필드에는 정반대 태도**를 취했다 — DTO 널 허용을 유지한 이유가
  "구버전 서버를 만나도 화면이 통째로 실패하지 않게"였는데(OQ-P-234 ②), 포맷에는 그 방어가 없다.
- **항목**: ① 매퍼가 파싱 실패를 `null`로 접을지 — 접으면 "시각을 못 읽었다"와 "시각이 없다"가
  뭉개지고(그 둘을 가르려 널 허용을 유지한 것과 충돌), 안 접으면 필드 하나가 화면을 지운다.
  ② 아니면 실패 단위를 목록에서 **카드로** 낮출지(`ApiCaller`가 아니라 매퍼가 원소별로 방어).
  ③ 이 결정을 시각 필드 하나가 아니라 **매핑 실패 일반의 방침**으로 세울지 — 지금은 매퍼마다 다르다.
- **상태**: 미해결 (현재 서버 계약으로는 발화하지 않는다 — 아래)
- **해소 메모**: 지금 안 터지는 근거는 최종 리뷰가 kotlinx-datetime 0.8.0 소스까지 열어 확인했다 —
  `LocalDateTime.Formats.ISO`가 초를 선택으로 두어 Jackson이 초 0일 때 줄여 쓰는 `...T12:00` 형태도
  받는다. 롤백도 안전하다(구버전 서버는 `null`을 주고 `?.let`이 건너뛴다). 정하면
  [api/parfait-group.md](../api/parfait-group.md) Android 매핑과 [data-layer](../architecture/data-layer.md)에
  반영한다. OQ-P-165(시각 파싱 불일치)의 후속이고 OQ-P-234 ②와 한 축이다.

### [2026-08-19] 배치를 끝낸 뒤 돌아가는 곳이 `groupId = 0L`이고, 흐름 화면이 백스택에 남는다

- **ID**: OQ-P-238
- **출처**: `CanvasToppingPlaceRoute.kt#CanvasToppingPlaceRoute`(PR #290) — 배치 확정 이펙트를 받으면
  `navigator.goTo(NavKeyCanvasMain(groupId = 0L))`이다. 그룹 id가 하드코딩인 이유는
  `NavKeyCanvasToppingPlace`가 `imageUri`만 싣기 때문이고, 촬영·갤러리·세그멘테이션·편집 NavKey도
  같은 이유로 그 값을 안 들고 다닌다(`refactor/segmentation-develop` 스펙이 "다섯 NavKey에 groupId를
  싣는 대안은 배경 편집처럼 무의미한 경로까지 오염된다"고 이미 기각했다). 게다가 `goTo`라
  **흐름 화면 전부가 백스택에 남은 채** 캔버스가 한 장 더 쌓인다.
- **항목**: ~~① 그룹 id를 어떻게 전할지~~ — **해소.** ② 배치 확정을 서버에 보내기 전까지 이 이동을
  어떻게 둘지 — 지금은 **저장 없이 화면만 바뀐다.** ③ 흐름을 중간에 닫는 경로(각 화면
  `onClickClose`)와 같은 자리에서 정할지 — 세그멘테이션 쪽 셋은
  `refactor/segmentation-develop`이 이미 결선했고, `CanvasToppingPlaceScreen`의 닫기는 아직 남았다.
- **상태**: ① 해소됨(2026-08-20) · ② 해소됨(2026-08-22) · ③ 미해결
- **해소 메모**: ①은 `refactor/segmentation-develop`의 `1181eedf`가
  `navigator.goTo(NavKeyCanvasMain(groupId = 0L))`을 `navigator.popUpTo<NavKeyCanvasMain>()`으로
  바꿔 닫았다. 되감기는 이미 백스택에 있는 엔트리를 찾으므로 **그룹 id를 실어 나를 필요 자체가
  없어졌고** 하드코딩 `0L`도 함께 사라졌다. 같은 커밋이
  [segmentation-pipeline-hardening 스펙](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)의
  캐시 정리 안전 근거("새 흐름은 캔버스에서만 시작하고 그러려면 이전 화면이 이미 백스택에서 걷혀
  있다")를 **참으로 만든다** — 그 브랜치가 스스로 들여온 결함이라 그 브랜치에서 닫는 것이 맞다고 봤다.
  ✅ **develop에 반영됐다(2026-08-20, PR #309 — develop `cf357937`)** — `CanvasToppingPlaceRoute`의
  배치 완료 이펙트는 이제 `popUpTo<NavKeyCanvasMain>()`이고 `groupId = 0L` 하드코딩은 코드에 없다.
  ✅ **②가 닫혔다(2026-08-22, PR #334)** — 확인 버튼이 발급 → S3 PUT → confirm → 배치 네 단계를
  태우고, **성공했을 때만** 되감는다. "저장 없이 화면만 바뀐다"는 상태가 이로써 사라졌고, 이 항목이
  물음을 열어 둔 채 기다리던 **되감기 시점의 근거도 함께 정해졌다**(성공 이펙트가 유일한 되감기
  경로다). 실패는 되감지 않고 알린 뒤 화면에 남는다 — 되감으면 Route 컴포지션에 매달린 토스트까지
  같은 프레임에 폐기돼 사용자가 실패 사실을 못 듣기 때문이다(OQ-P-167).
  ⚠️ **다만 실서버 요청은 아직 0건이다**(실기기 미수행) — 네 단계가 실제로 통과하는 것을 본 적이 없다.
  ③은 그대로 열려 있다 — `CanvasToppingPlaceScreen`의 닫기는 `onBack()` 한 칸이라, 흐름 전체를
  되감는 다른 세 화면(`popUpTo<NavKeyCanvasMain>()`)과 여전히 형태가 다르다.
  ③이 정해지면 [navigation-flow](../architecture/navigation-flow.md) 토핑 생성 플로우 절과
  [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md) 드리프트 ①②를 함께 본다.
  OQ-P-209 ②③과 한 라운드에 정해지는 것이 자연스럽다.

### [2026-08-19] `NavKeyCanvasMove` 계열이 호출자를 잃고 도달 불가로 남았다

- **ID**: OQ-P-239
- **출처**: `NavKeyCanvasMove.kt` · `CanvasMoveRoute.kt` · `CanvasMoveScreen.kt` ·
  `feature/groups/canvas/impl/navigation/EntryBuilder.kt`(PR #290) — 유일한 호출자였던
  `SegmentationConfirmRoute.onClickNext`가 `NavKeyCanvasToppingPlace`로 갈아탔는데 목적지·Route·Screen과
  엔트리 등록은 그대로다. `CanvasMoveRoute`는 `navigator`를 받아 놓고 쓰지도 않는다(자리채움이었다).
  미머지 브랜치가 이 셋을 지우는 커밋(`f5ed87d5`)을 만들었다가 **develop에서는 아직 호출자가 있다는
  이유로 되돌렸는데**, 그 이유가 #290 머지로 사라졌다.
- **항목**: ① 셋을 지울지 — 지우면 도달 불가 화면 목록에서 하나가 빠진다. ② 같은 부류
  (`NavKeyCameraSystem`·`NavKeySystemGalleryPicker`)와 묶어 한 번에 정리할지.
- **상태**: 해소됨 (① 지우는 쪽으로 실행, 2026-09-20 PR #514. ②는 별개로 남는다)
  > 📌 **2026-08-20 — `refactor/segmentation-develop`이 되살리지 않기로 했다.** 그 브랜치가 두 번째
  > 리베이스에서 같이 처리할 후보였으나(아래 해소 메모의 옛 권고), **이 잔해는 그 라운드가 만든 것이
  > 아니라 #290이 남긴 것**이라 리뷰 대상 diff를 넓힐 값이 없다고 판단했다
  > ([재정정 절](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md#드롭됐던-것-둘의-처리--하나만-되살렸다)).
  > 코드 현황은 그대로다 — `goTo` 호출부 0건, `feature/groups/canvas/impl`의 엔트리 등록만 잔존.
  > 📌 **그 라운드가 머지된 뒤에도 그대로다(2026-08-20, PR #309)** — develop에서 `NavKeyCanvasMove`·
  > `CanvasMoveRoute`·`CanvasMoveScreen`과 엔트리 등록이 확인되고 `goTo` 호출부는 여전히 0건이다.
  > **이 잔해를 지울 라운드가 정해지지 않았다는 것만 남았다.**
  > ⚠️ **전용 청소 라운드가 지나갔는데도 그대로다(2026-09-17, PR #504)** — 이 델타는 목적 자체가
  > 정리였고(`Delete : unused resource`·`Reafctor : Redundant constructs exclude unused symbol`),
  > 쓰지 않는 드로어블 둘과 템플릿 색 일곱, 안 쓰는 import 여럿을 실제로 걷었다. 그런데
  > `NavKeyCanvasMove` 계열 셋과 엔트리 등록은 **한 줄도 건드리지 않았다** — develop에서 `goTo`
  > 호출부는 여전히 0건이다. **"지울 라운드가 정해지지 않았다"는 위 문장이 청소 라운드가 와도
  > 참이었다**는 것이 이번에 드러났다. 자동 검사가 못 잡는 종류이기 때문이다: 엔트리 등록이 참조라
  > 미사용 심볼로 보이지 않는다. 지우려면 사람이 따로 결정해야 한다.
  > ✅ **그 다음 청소 라운드가 사람 손으로 지웠다(2026-09-20, PR #514)** — `NavKeyCanvasMove`·
  > `CanvasMoveRoute`·`CanvasMoveScreen`과 엔트리 등록이 삭제됐다. **같은 커밋이 범위를 넓혀
  > `NavKeyCanvasEdit`·`NavKeyCanvasImageSelect` 계열도 함께 걷었다**(`CanvasEditRoute`/`Screen`·
  > `CanvasImageSelectRoute`/`Screen`·엔트리 둘). 커밋 제목이 `refactor: remove the three unreachable
  > canvas entries`라 **바로 위 ⚠️가 적은 "사람이 따로 결정해야 한다"가 그대로 실행된 모양**이다.
  > 딸려 걷힌 것 셋: ① `NavTransition.Fade.metadata` 예외(OQ-P-260 ③), ② `LocalSharedTransitionScope`와
  > 두 루트의 `SharedTransitionLayout` 껍질, ③ `NavKeyAnalyticsScreen`의 화면 ID 셋
  > (`C-001-edit`·`C-001-image-select`·`C-001-move`)과 그 테스트 기대값.
  > ⚠️ **②(`NavKeyCameraSystem`·`NavKeySystemGalleryPicker`)는 그대로다** — 이 라운드가 캔버스 셋만
  > 보고 그 둘은 건드리지 않았다. 도달 불가 화면이 0이 된 것은 **캔버스 쪽만**이다.
- **해소 메모**: [navigation-flow](../architecture/navigation-flow.md) "인자 있는 목적지" 목록과 토핑
  생성 플로우 절의 도달 불가 표기는 이 회차(2026-09-21) 점검에서 걷었다. ②는 별개 항목으로 남는다 —
  같은 부류 둘의 존폐는 아직 아무도 결정하지 않았다.

### [2026-08-19] 배치 화면이 보여 주는 캔버스가 실제 캔버스가 아니다

- **ID**: OQ-P-240
- **출처**: `CanvasToppingPlaceViewModel.kt#CanvasToppingPlaceUiState`(PR #290) — 배경은
  `YGAtomicColors.Gray.White` 기본값 고정에 `TODO`가 붙어 있고, **이미 올라간 토핑도 그리지 않는다.**
  C-001 캔버스는 서버에서 배경색·기존 토핑을 받아 그리는데(`GetTodayParfaitUseCase`) 배치 화면은
  그 조회를 안 한다. 사용자는 **흰 바탕에 자기 토핑만 놓고** 자리를 고른 뒤 실제 캔버스로 돌아간다.
- **항목**: ① 배경·기존 토핑을 어디서 받을지 — 캔버스 상세를 다시 부를지, C-001이 이미 들고 있는
  것을 NavKey나 공유 캐시로 넘길지(전자면 부작용 있는 `today`를 또 부르게 될 수 있다).
  ② 기존 토핑을 그린다면 배치 중인 토핑과 시각적으로 어떻게 가를지(지금은 배경 전체에 `Black25` 딤을
  깔아 배치 대상만 도드라지게 한다 — 남의 토핑도 그 딤 아래로 갈지).
  ③ 겹침 규칙(z 순서)이 정책에도 코드에도 없다.
- **상태**: **해소됨** (2026-08-25, PR #357 — ①은 재조회, ②는 딤 아래, ③은 `positionZ` 오름차순)
- **해소 메모**: 셋 다 코드가 답을 골랐다. **①** `CanvasToppingPlaceViewModel`이 초안에서 읽은
  `groupId`로 `GetTodayParfaitUseCase`를 **다시 부른다**(NavKey·공유 캐시로 넘기는 안은 안 골랐다).
  `groupId` 하나당 한 번만 부르도록 필드 하나로 가드하고, 조회 실패는 로그만 남긴 채 기본 배경·빈
  토핑 목록으로 배치를 계속하게 둔다. 우려하던 **부작용 있는 `today` 재호출**은 KDoc이 "이 흐름에
  들어왔다는 것 자체가 오늘 캔버스가 있다는 뜻"이라고 근거를 적어 두었다 — 발동할 일이 없다는 뜻이지
  호출이 안전하다는 보장은 아니라는 단서까지 함께 적었다(OQ-P-129의 부작용 있는 GET은 그대로 열려
  있다). **②** 남의 토핑도 딤 **아래**다 — 겹 순서가 배경 → 기존 토핑 → `Black25` 딤 → 배치 중인
  토핑이라 지금 옮기는 것만 딤 위에 뜬다. **③** 그리는 순서를 `transform.positionZ` 오름차순으로
  정렬해 `CanvasToppingLayer`의 계약(뒤에 오는 것이 위)에 맞춘다 — C-001과 같은 규칙이다.
  배경 이미지도 C-001과 같은 `ContentScale.Crop`이라 두 화면의 그림이 갈리지 않는다.
  ⚠️ **남는 것 셋**: 배치 화면의 캔버스 상자는 여전히 `YGCanvas`가 아니라 화면 자작 `Box`라
  **좌상단 컷 도형이 없다**(OQ-P-174와 같은 자리) · 겹침 z 규칙은 **정책 소스가 아니라 서버 필드
  해석**이라 위키에는 여전히 근거가 없다 · 이 라운드가 연 조회·매핑 경로에 **테스트가 0건**이다
  (OQ-P-303). [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md) 드리프트 ④를
  갱신했다.

### [2026-08-19] 회전과 리사이즈 한계를 정책이 말한 적이 없고 코드가 정했다

- **ID**: OQ-P-241
- **출처**: `CanvasToppingPlaceViewModel.kt`(`TOPPING_DRAG_PX_PER_SCALE`·`TOPPING_DRAG_DEGREES_PER_PX`·
  `TOPPING_MAX_OVERFLOW_RATIO`·`minScaleForTouchTarget`·`maxScaleToOverflowCanvas`, PR #290) ×
  위키 [[C-106-토핑-배치-정책-v0.1]] — 정책은 **초기 배치와 이탈 허용만** 규정하고 조작 이후를 다루지
  않는다. 그래서 앱이 넷을 정했다: 리사이즈 하한은 **48dp 최소 터치 영역에서 역산**(고정 배율이면 큰
  원본 사진이 처음 크기로 못 돌아온다), 상한은 **캔버스 긴 변의 1.5배**를 역산(고정 배율이면 큰 원본이
  캔버스를 못 벗어난다), 회전은 **무제한**, 드래그 감도 둘은 리터럴 상수다. 위키 [[누끼-편집]]이
  C-104 확대 배율(1~3배)을 규정한 것과 대비된다 — 같은 종류의 규칙이 한 화면엔 있고 한 화면엔 없다.
- **항목**: ① 이 넷을 정책으로 끌어올릴지(위키 소관, 수집 요청 선행) 앱 표현 규칙으로 둘지.
  ② 최소 터치 방어를 **초기 배치에만** 적용할지 리사이즈 하한으로도 쓸지 — 코드는 후자를 골랐는데,
  정책 문구("스케일링된 토핑의 짧은 쪽이 48px 미만이면")는 초기 렌더링 절에 있다.
  ③ 회전 스냅(0·90·180·270 근처 흡착)이나 상한이 필요한지.
- **상태**: 부분 해소 (**감도 둘은 2026-08-27 PR #397로 소멸** — 앱이 정한 것은 하한·상한 둘로 줄었다 / 나머지 잔존)
  > ⚠️ **회전 무제한이 화면 안 문제가 아니게 됐다(2026-08-23, PR #336)** — C-301 편집 탭의 확인
  > 버튼이 `rotationDegrees`를 그대로 `rotation`에 실어 PATCH 한다. 몇 바퀴를 돌리든 렌더는 같지만
  > **저장되는 수치는 계속 커지고**, 서버도 범위를 검증하지 않는다
  > ([api/parfait-image.md](../api/parfait-image.md) 미결). ③(스냅·상한)이 이제 저장 값의 문제이기도
  > 하다. 같은 라운드에서 배율 쪽은 상한 자체가 사라졌다(OQ-P-271).
  > ✅ **감도 상수 둘이 사라졌다(2026-08-27, PR #397)** — 이 항목이 "앱이 정한 넷"으로 셌던 것 중
  > `TOPPING_DRAG_PX_PER_SCALE`·`TOPPING_DRAG_DEGREES_PER_PX`가 없어지고, 감도가 리터럴이 아니라
  > **기하로 결정된다**. 크기조절은 핸들이 중심에서 멀어진 비율을 배율에 곱하고, 회전은 드래그를
  > 핸들 벡터의 접선에 투영해 각도로 환산한다(`ToppingGeometry`의 `resizeScaleFactor`·
  > `rotationDeltaDegrees`). 그래서 정책으로 끌어올릴지 물어야 할 것은 **하한·상한 둘**로 줄었다.
  > ⚠️ **고친 계기는 이 미결이 아니라 결함이었다** — 회전 핸들은 우측 하단에 있는데 각도가 드래그의
  > 가로 성분만 받아, 화면 좌표계에서 **시계방향으로 끌면 반시계로 돌았다**. 크기조절은 배율에
  > 고정량을 더해서, 초기 배율이 작아지는 큰 원본 사진일수록 같은 손동작이 훨씬 크게 먹었다.
  > ③(스냅·상한)은 손대지 않았고, 실기기로 감도를 확인한 적이 없다는 점도 그대로다 —
  > 다만 이제 확인할 대상이 상수가 아니라 기하 자체다.
  > 📌 **환산 자리가 ViewModel에서 화면으로 옮겨 왔다** — 핸들 위치를 아는 쪽이 화면이라,
  > 인텐트가 픽셀이 아니라 **배율에 곱할 값**(`OnToppingResize`)과 **각도**(`OnToppingRotate`)를 싣고
  > ViewModel은 누적만 한다(`OnToppingMoveDrag`가 비율을 받는 것과 같은 결이다). 핸들 벡터는
  > `rememberUpdatedState`로 읽어야 한다 — `dragBy`의 제스처 블록은 키가 그대로면 시작 시점 람다를
  > 계속 쓰므로, 값으로 캡처하면 처음 위치의 접선에 갇혀 한 바퀴를 돌지 못한다.
- **해소 메모**: ①은 위키 정책 수집 요청이 선행이다(코드가 먼저 확정한 넷째 사례 — 앞의 셋은
  C-104 확대 상한·C-105 테두리 색 팔레트·토핑 테두리 렌더 규칙). 정하면
  [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md) 조작 절과 드리프트 ⑤를
  갱신한다.

### [2026-08-20] 탈퇴가 끝난 계정으로 로그아웃이 한 번 더 나가고, 그 401이 재발급까지 깨운다

- **ID**: OQ-P-242
- **출처**: `WithdrawUseCase`(PR #306) → `LogoutUseCase` → `AuthRepository.logout()` — 서버가 탈퇴를
  받아 준 **뒤에** 로컬을 정리하는데, 그 정리를 맡은 `LogoutUseCase`가 서버 로그아웃부터 부른다. 그
  계정은 방금 지워졌으므로 서버는 401 `MEMBER_NOT_FOUND`를 준다([api/member.md](../api/member.md)).
  `logout`은 화이트리스트 밖이라 그 401이 `TokenAuthenticator`를 깨우고, 재발급에 쓸 refresh token은
  **탈퇴가 서버에서 이미 지운 것**이라 재발급도 거절된다. 거절은 세션 사망 경로라
  `SessionEvent.ForcedLogout`이 발행되고 `MainRoute`가 `replaceAll(NavKeyLogin)` 한다 — 같은 시각
  ViewModel도 `NavigateToLogin`을 쏘므로 **이동을 두 곳이 일으킨다**. 종착지가 같아 사용자 눈에는
  드러나지 않지만, 되돌릴 수 없는 동작 뒤에 **반드시 실패할 왕복이 둘 붙고** 로그에는 "재발급 거절 —
  세션 종료"가 결함처럼 남는다.
- **항목**: ① `WithdrawUseCase`가 `LogoutUseCase` 대신 **로컬 정리만** 부르게 할지 — 그러면 "무엇을
  지우는가"의 단일 자리(`LogoutUseCase`)가 깨지므로, 정리 부분만 따로 뽑아 둘이 공유하는 형태가
  필요하다. ② 아니면 서버 로그아웃 호출은 그대로 두고 **탈퇴 직후임을 아는 경로**에서 재발급을
  건너뛸지(`TokenAuthenticator`가 볼 수 있는 신호가 지금은 없다). ③ 이동 주체를 하나로 좁힐지 —
  `ForcedLogout`과 화면 이펙트가 같은 목적지로 겹치는 첫 사례다.
- **상태**: 미해결 (실기기·실서버 확인 없음 — 위 연쇄는 계약과 코드 대조로 얻은 것이고 관측한 것이
  아니다. 오프라인이면 첫 401 자체가 안 나므로 연쇄도 없다)
- **해소 메모**: 정하면 [data-layer](../architecture/data-layer.md) UseCase 절과
  [api/member.md](../api/member.md) Android 매핑, [ADR-0021](../adr/0021-token-refresh-forced-logout.md)
  세션 종료 경로를 함께 갱신한다. OQ-P-198(같은 코드에 처분이 갈린다)과 같은 자리다 — 그쪽은 화면이
  세션 사망을 **안 알리는** 쪽이고, 이쪽은 앱이 스스로 죽인 세션을 **다시 확인받는** 쪽이다.

### [2026-08-20] 하루 경계 상수 하나가 뜻이 다른 두 하루를 동시에 정한다

- **ID**: OQ-P-243
- **출처**: `domain/model/DayWindow.kt`의 `DAY_BOUNDARY_HOUR` × `domain/model/ParfaitDay.kt`의
  `parfaitToday()` × `GetRecentCacheImagesUseCase`·`GalleryRepositoryImpl` — PR #308이 앱의 하루 경계를
  03시로 옮길 때 **경계 값을 두 곳에 적지 않으려고** 기존 `DayWindow.DAY_BOUNDARY_HOUR`를 재사용했다.
  그 자체는 옳은 판단이지만, 지금 그 상수는 **성격이 다른 두 하루**를 정한다 — `parfaitToday()`의 하루는
  **서버 마감 배치 시각의 거울**(고정 KST, 계약이 값을 내려주지 않아 앱이 복제한 것)이고,
  `DayWindow.current()`의 하루는 **기기 기준 최근 사진 윈도우**(기기 시간대)다. KDoc은 "시각만 공유하고
  시간대는 공유하지 않는다"까지 적었으나, **서버가 배치 시각을 바꿀 때 무엇을 해야 하는지**는 적히지
  않았다. 그 경우 상수를 고치면 갤러리 윈도우가 함께 움직이고, 갈라 두면 "두 곳에 적으면 한쪽만
  고쳐진다"던 원래 문제로 돌아간다.
- **항목**: ① 서버 배치 시각이 바뀔 때 상수를 갈라 각자 갖게 할지, 아니면 갤러리 윈도우도 같이 움직이는
  것이 의도라고 못박을지. ② 계약에 하루 경계를 내려주는 필드를 서버에 요청할지 — 있으면 복제가 사라져
  이 결합 자체가 없어진다. ③ 갤러리 윈도우의 경계가 03시여야 하는 근거가 정책에 있는지(파르페 마감과
  같은 값을 쓰는 것이 우연인지 의도인지 어디에도 안 적혀 있다).
- **상태**: 미해결 (지금은 두 값이 같아야 맞는 상태라 발화하지 않는다)
- **해소 메모**: 정하면 [api/parfait.md](../api/parfait.md) "하루 경계"와
  [data-layer](../architecture/data-layer.md) 시각 노트에 반영한다. OQ-P-222 ②(경계 값을 복제할지)의
  후속이고, 그 항목이 "복제하되 상수를 공유한다"로 답한 대가가 이것이다.

### [2026-08-20] 서버가 마감 캔버스를 막자 "안 막는다"고 적어 둔 앱 주석 일곱 곳이 하루 만에 거짓이 됐다

- **ID**: OQ-P-244
- **출처**: 서버 `efbf98f`(`fix: 마감된 파르페에 대한 편집 요청 거부`)가 쓰기 다섯 경로에 `status != ACTIVE`
  가드를 넣어 409 `PARFAIT_ALREADY_CLOSED`를 내기 시작했다(OQ-P-160 ②·OQ-P-189 해소). 그 전제를 **단정문으로**
  들고 있는 앱 자리가 일곱이다 — `data`의 `ParfaitService`(배경 변경 KDoc)·`ParfaitRemoteDataSource`(오늘
  조회·배경 변경 둘), `domain`의 `ParfaitRepository`·`CanvasStatus`, `feature`의 `CanvasMainViewModel`·
  `CanvasMainScreen`. 문구는 "서버가 캔버스 상태를 보지 않아 마감된 캔버스도 편집된다 · 막는 것은 화면
  책임"류다. **동작은 안 깨진다** — 화면 방어가 서버 가드와 겹칠 뿐이고, 쓰기 다섯 경로는 소비처가 0건이라
  409를 받을 코드가 아직 없다. 그래서 계약 표의 `⚠️불일치`도 아니다(요청·응답 형태 불변).
  ⚠️ 다만 앱 `domain/model/error/ServerErrorCode.kt`에 **`PARFAIT_ALREADY_CLOSED` 상수가 없다** —
  결선하면 일반 오류로 뭉개진다.
- **항목**: ① 일곱 자리를 지울지 포인터로 바꿀지 — [parfait/CLAUDE.md](../code-conventions.md) "기준 2와 3이 겹칠 때는
  남긴다"에 따르면 `CanvasStatus`·`CanvasMainScreen`처럼 **오해를 미리 막는 성격**이 섞인 자리는 지우기보다
  근거 문서를 가리키게 바꾸는 쪽이다. ② C-106 배치가 결선될 때 409를 어떤 문구로 보여줄지 — "마감된
  캔버스"는 03시 회전 직후 화면이 오래 열려 있으면 실제로 나온다(재조회로 복구되는 부류라 일반 오류
  문구와 처분이 다르다). ③ 이 부류(다른 컴포넌트의 현재 상태를 단정한 주석)를 계약 문서 감사 말고
  잡을 수단이 있는지 — 2026-08-19 라운드에 이어 **두 번째로 하루 만에 낡았고**, 두 번 다 이 감사가 찾았다.
- **상태**: 미해결 (**①② 해소, ③만 남음** — 잡을 수단이 문서 감사뿐인 것은 그대로다)
- **해소 메모**: ①을 정리하면 [api/parfait.md](../api/parfait.md)·
  [api/parfait-image.md](../api/parfait-image.md) Android 매핑의 ⚠️도 함께 걷는다. ②는 C-106 결선 스펙에
  적는다. ③은 OQ-P-234 ③(와이어 계약 테스트 부재)과 같은 성격이지만 대상이 **주석**이라 테스트로는
  안 잡힌다 — 문서 감사 주기가 유일한 수단이라는 사실 자체를 기록해 둔다.
  > 📌 **②는 답이 났다(2026-08-20, [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md))** —
  > `PARFAIT_ALREADY_CLOSED`만 토스트 후 캔버스로 되감고 나머지 실패는 화면에 머문다. 마감된 캔버스에는
  > 다시 눌러도 영원히 실패하므로 잔류시키면 사용자가 할 수 있는 일이 실패 반복뿐이기 때문이다.
  > 상수 부재도 그 라운드의 선행 커밋이 닫았다. ①③은 그대로 열려 있다.
  > ✅ **①도 닫혔다(2026-08-20, PR #318 develop 머지)** — 일곱 자리가 전부 **409를 사실로 적는 문장**이
  > 됐고, 지우는 대신 고친 것은 [parfait/CLAUDE.md](../code-conventions.md) "기준 2와 3이 겹칠 때는 남긴다"를
  > 따른 결과다(새 문장은 단정 대신 `api/parfait.md`를 가리킨다). 상수도 같은 PR로 develop에 들어와
  > 브랜치와 **바이트까지 같다** — `ServerErrorCode.kt`가 그 파일의 "쓰는 코드만 둔다" 규약에
  > **처분이 정해진 코드는 미리 둔다**는 예외를 함께 신설했다.
  > 정리 라운드가 발견한 것도 있다: 다섯 경로 전부 **권한 검사가 마감 검사보다 앞**이라 마감된
  > 캔버스라도 남의 토핑·비멤버면 403이 먼저 온다 — "마감을 유일한 실패로 두고 분기하면 놓친다"가
  > 상수 KDoc의 경고로 남았다. **③은 그대로다** — 이번에도 찾은 것은 이 감사다.

### [2026-08-20] 테두리 굵기 거동이 편집 화면과 캔버스에서 다르다

- **ID**: OQ-P-245
- **출처**: [ADR-0025](../adr/0025-topping-border-as-server-field.md)가 테두리를 픽셀에 굽지 않고 서버
  필드로 보내기로 하면서 드러난 차이다. 구운 테두리는 이미지의 일부라 **토핑을 키우면 함께 굵어졌다.**
  서버 `borderWidth`를 받아 그리는 `CanvasToppingLayer`는 그 값을 **화면 dp로 고정**해 8방향 스탬프를
  찍으므로 토핑을 키워도 굵기가 그대로다. 편집 화면(`ToppingEditViewModel`)은 또 다르게 — `originPxPerDp`로
  dp를 원본 픽셀 좌표계에 환산해 굽는다. 즉 **같은 "굵기 N dp"가 세 자리에서 서로 다른 그림**이 될 수 있다.
  위키 정책([[토핑]]·C-104 편집 정책)은 브러시·테두리 범위만 정하고 배율에 따른 거동을 다루지 않는다.
- **항목**: ① 토핑을 키울 때 테두리가 함께 굵어져야 하는가(정책 확정 필요 — 지금은 서버 계약이 dp 고정
  쪽으로 사실상 정해 버린 상태다). ② 편집 화면 미리보기를 캔버스와 같은 거동으로 맞출지, 아니면 편집은
  원본 좌표계 그대로 두고 차이를 받아들일지. ③ 굵기 값의 단위를 계약 문서에 명시할지 —
  [api/parfait-image.md](../api/parfait-image.md)는 타입만 적고 단위를 말하지 않아 앱이 dp로 정했다.
  ④ **기기 폭 축**(2026-08-21, PR5 추가) — 토핑 크기는 캔버스 폭 대비 비율로 정규화되는데
  `borderWidth`만 절대 dp라, 폭이 다른 기기에서 상대 굵기가 달라진다. ①②③이 다루던 것은
  "편집 화면 굵기와 캔버스 굵기의 어긋남"(배율 축)뿐이었다.
- **상태**: 미해결 (정책 근거 없음 — 코드가 먼저 정했다)
- **해소 메모**: 정하면 [ADR-0025](../adr/0025-topping-border-as-server-field.md) "트레이드오프"와
  [design-system](../architecture/design-system.md) 토핑 절에 반영한다. C-301 테두리 재편집 라운드가
  같은 값을 다시 만지므로 그 전에 정하는 편이 싸다.
  > 📌 **이 차이가 사용자 화면에 실현됐다**(2026-08-21 브랜치 작업 → 2026-08-22 develop 머지,
  > PR #334) — 편집을 마치면 누끼 확인·배치·캔버스가 전부 서버 계약과 같은 방식으로
  > (화면 dp 고정 8방향 스탬프) 그리므로, **편집 화면에서 본 굵기보다 그다음 화면들이 가늘어 보인다.**
  > 그때까지는 굽기 덕에 편집에서 본 그림이 확인 화면까지 그대로 따라와 차이가 한 흐름 안에서
  > 드러나지 않았다. 라운드는 이것을 회귀가 아니라 **의도된 변화**로 두고 실기기 확인 항목에 적어
  > 두었다 — 어느 쪽이 정책인지는 여전히 이 항목이 쥔다. ①이 사실상 dp 고정으로 굳는 압력이 한 단계
  > 더 세졌다.

### [2026-08-20] S3 업로드가 코루틴 취소를 따라가지 않는다

- **ID**: OQ-P-246
- **출처**: `data/source/image/remote/PresignedUploadDataSourceImpl#put`(PR1 `feature/#270-image-upload-transport`) — `withContext(Dispatchers.IO)` 안에서 OkHttp `Call.execute()`를 블로킹으로 부르고 `Call.cancel()`을 코루틴 취소에 잇지 않는다. 그 블록에 중단점이 없어 **호출 코루틴이 취소돼도 업로드는 `callTimeout`까지 계속 돈다.** 브랜치 최종 리뷰가 잡았고 "되돌리는 비용은 지금이 가장 싸다"고 평가했다.
- **항목**: ① `suspendCancellableCoroutine` + `enqueue` + `invokeOnCancellation { call.cancel() }`로 바꿀지, 아니면 ② 지금 형태를 두고 화면이 취소를 안 하도록 설계할지. ①이면 취소를 실제로 관측하는 테스트 설계가 따로 필요하다(느린 응답 + 취소).
- **상태**: 해소됨(PR5)
- **해소 메모**: PR1에서 미룬 이유는 전송 메서드의 모양을 바꾸는 변경이라 단일 fix 웨이브에 태우면 클린한 브랜치를 늦게 흔들 위험이 이득보다 컸다는 것이다. PR5는 로딩 오버레이·실패 시 `popUpTo` 되감기가 있어 `viewModelScope` 취소가 흔한 화면이므로 그 라운드가 판정했다. `execute()` → `enqueue` + `suspendCancellableCoroutine`·`invokeOnCancellation { call.cancel() }`로 바꿨다. `onFailure`가 취소를 실패 `Result`로 둔갑시키지 않도록 `continuation.isActive` 가드를 뒀다.

### [2026-08-20] 서버가 200에 실패 봉투를 실으면 `AppError.Server.statusCode`가 null이다

- **ID**: OQ-P-247
- **출처**: `data/network/ApiCaller#runCatchingApi` — HTTP 200 + `success=false` 봉투는 `ApiException.Business(statusCode = null)`을 만들고(`ApiCaller.kt:60`), `HttpException` 경로만 `statusCode = e.code()`를 채운다(`:84`). 그 값이 `AppError.Server`까지 그대로 흘러간다. PR2 브랜치 최종 리뷰가 "이 브랜치가 지금 잠그지 못하는 것" 중 하나로 짚었다.
- **항목**: [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md)의 실패 처리 절은 되감기 판정을 **`code`와 `statusCode`를 함께 보고** 하라고 정한다 — 코드 문자열이 도메인 간 유일하지 않다는 `ServerErrorCode`의 전제 때문이다. 그런데 `statusCode`가 null로 오는 갈래에서 그 판정이 어떻게 되는지가 정해져 있지 않다. ① `code`만으로 판정하고 `statusCode`는 확증용으로만 쓸지, ② null을 "판정 불가"로 보고 잔류시킬지, ③ 서버가 실제로 200에 실패 봉투를 싣는 경로가 있는지부터 확인할지.
- **상태**: 해소됨(PR5, ①안)
- **해소 메모**: PR2는 `AppError`로 바꿔 올리기만 하고 코드를 보고 분기하는 자리가 없어 이 null이 문제가 되지 않는다. ③을 먼저 확인한 결과 `parfait/api/`의 실패 표에 200 + `success=false` 사례가 없고, 그와 별개로 세 코드에 status로 갈려야 하는 동명 코드가 없어 `code` 단독 판정으로 닫았다.

### [2026-08-20] 업로드 확정과 배치 사이에서 취소되면 고아 이미지가 남고 예외가 `Result` 밖으로 나간다

- **ID**: OQ-P-248
- **출처**: `domain/usecase/topping/AddToppingUseCase`(PR2 `feature/#270-topping-place-domain`) — 업로드가 `COMPLETED`까지 간 뒤 배치 전에 호출 코루틴이 취소되면 서버에는 확정된 이미지만 남는다. 게다가 `mapErrorToAppError`가 `CancellationException`을 **변환하지 않고 재던지므로**(`AppErrorMapper.kt`) 그 취소는 실패 `Result`가 아니라 예외로 호출부까지 올라간다. PR2 브랜치 최종 리뷰가 짚었다.
- **항목**: ① 취소를 화면이 어떻게 받을지(예외로 올라오는 것이 정상 계약임을 호출부가 알고 있어야 한다). ② 고아 이미지를 감수할지 — 스펙의 재시도 결정이 이미 고아 S3 객체를 감수하기로 했으므로 같은 처분이면 문서에 그렇게 적으면 된다. ③ `positionZ`를 이미 소진한 흐름을 다시 타면 z가 겹치는지(서버가 유일성을 요구하지 않아 거부되지는 않는다).
- **상태**: 해소됨(PR5, 감수)
- **해소 메모**: OQ-P-246과 뿌리는 같지만 결과가 다르다 — 246은 취소돼도 전송이 계속 도는 것이고, 이쪽은 취소 시점이 두 단계 **사이**일 때 서버에 남는 것이다. PR5가 판정했다 — ① 취소는 예외로 올라오는 것이 정상 계약이고 그 시점엔 화면이 없다. ② 고아 이미지는 재시도 결정과 같은 처분으로 감수한다. ③ z 겹침은 서버가 유일성을 요구하지 않아 거부되지 않는다. 코드 변경 없이 `AddToppingUseCase` KDoc 한 줄로 닫았다.

### [2026-08-20] 가입 중 플래그가 "버튼을 잠근다"고 적혀 있는데 그 버튼이 없다

- **ID**: OQ-P-249
- **출처**: `TermAgreeViewModel#requestSignUp` KDoc이 `isSigningUp`을 **"버튼을 잠그는 표시일 뿐"**이라고
  적는데, 확인 버튼의 활성 조건 `TermAgreeState.isAvailable`은 그 값을 보지 않는다(목록 비었는지와 필수
  동의만 본다). 같은 라운드(PR #315)의 `TermAgreeRoute` 주석은 반대로 **"다음 버튼은 `isSigningUp`을
  보지 않아 응답을 기다리는 동안에도 눌린다"**고 적고 로딩 오버레이로 덮는다 — 두 주석이 같은 플래그에
  서로 다른 역할을 적었고, 코드와 맞는 쪽은 Route다.
- **항목**: ① 주석을 코드에 맞출지(`isSigningUp`은 오버레이 전용), 코드를 주석에 맞출지
  (`isAvailable`에 `isSigningUp` 부정을 넣어 버튼도 잠근다). ② 이 화면은 오버레이만으로 막는데
  S-001 로그아웃은 `isLoggingOut`으로 **항목 자체를 비활성**한다(덮개가 아니라 줄 하나만 가려야 해서다)
  — 요청 중 표시의 기본 관용구가 둘 중 무엇인지 정한 적이 없다 →
  [state-management](../architecture/state-management.md) "요청 중 플래그는 `finally`로 내린다".
- **상태**: 미해결 (**동작 영향 0** — 오버레이가 입력을 삼키고 중복 요청은 `launch(key = KEY_SIGN_UP)`이 막는다)
- **해소 메모**: 위험은 지금이 아니라 나중이다 — 오버레이 조건(`isLoading || isSigningUp`)을 좁히거나
  V2의 로딩 표현이 바뀌는 순간 **주석이 약속한 방어가 어디에도 없다**는 것이 드러난다. 정하면
  [intro-term-agree 스펙](../superpowers/specs/archive/2026-07-22-intro-term-agree.md) "실패 표현" 절의 같은 서술도
  함께 맞춘다. ②는 화면마다 답이 다를 수 있다(덮을 것이 화면 전체인지 줄 하나인지) — 그렇다면
  "무엇을 가리는가로 고른다"를 규약으로 적는 편이 낫다.

### [2026-08-20] 본인 토핑을 가려낼 방법이 없어 C-202의 본인 갈래가 통째로 비어 있다

- **ID**: OQ-P-250
- **출처**: `CanvasMainViewModel#handleOnClickTopping`의 `CanvasToppingVO.isMine()`이 **상수 `false`**다
  (PR #298 develop 머지). 위키 [[C-202-토핑-편집자-확인-규칙-v0.1]]은 본인 토핑 탭을 Spotlight 대상에서
  빼고 C-305 편집으로 보내라고 정하는데, 지금은 **본인 토핑도 Spotlight로 들어가고 자기 닉네임이
  적힌 토스트가 뜬다.** 판정이 안 되는 이유는 캔버스 조회 응답이 "내 `groupMemberId`"를 알려 주지
  않아서다 — 토핑의 `placedBy.groupMemberId`와 비교할 상대가 앱에 없다.
- **항목**: ① 내 멤버십 행 id를 어디서 얻을지 — 서버에 캔버스 응답의 `groupMembers[]`에 "나" 표시를
  요청할지, 아니면 그룹 상세(`ParfaitGroupDetailVO`)가 이미 아는 값을 캔버스가 가져다 쓸지
  (S-101은 `memberId`로 `isMe`를 판별한다). ② 판정이 생길 때까지 본인 토핑을 Spotlight에 넣어 둘지,
  아니면 탭을 무시할지 — 지금은 넣는 쪽이고 정책과 다르다. ③ C-305 목적지 자체가 없어 ①이 풀려도
  갈 곳이 없다(그 라운드가 오면 함께 닫힌다).
- **상태**: **해소됨** (**① 서버 `ownerType`(2026-08-26) · ② 앱이 같은 날 읽음(PR #376) · ③ 목적지 확보(2026-08-27, PR #400)** — 다만 목적지가 새 C-305 화면이 아니고 조건이 하나 붙었다. 아래 참고)
  > ⚠️ **C-301 편집 탭이 판정을 시작했다(2026-08-22, PR #329)** — `CanvasBGEditViewModel`이
  > `MyAccountVO.memberId`(계정 id)와 `placedBy.groupMemberId`(그룹 멤버십 행 id)를 견주고, 코드
  > KDoc이 **두 값이 서로 다른 축이라는 사실을 스스로 적어 두었다**(`TODO(서버 응답 확장 대기)`).
  > 캔버스 메인의 상수 `false`보다 나쁜 점이 있다 — 상수는 항상 같은 쪽으로 틀리지만 이 비교는
  > **서버가 두 id를 어떻게 발급하느냐에 따라 우연히 맞기도 하고 틀리기도 한다.** 그리고 이 화면에서
  > 판정은 표현이 아니라 **게이트**다: 참으로 새면 남의 토핑을 선택·이동·삭제할 수 있고, 거짓으로
  > 접히면 내 토핑도 못 만진다(탭 자체를 무시한다).
  > ①의 무게가 그만큼 늘었다 — 실기기 1회로 어느 쪽인지 바로 드러난다.
  > ✅ **①이 서버에서 닫혔다(2026-08-26 서버 delta, PR #115)** — 오늘·상세 캔버스 응답의
  > `images[].placedBy`에 `ownerType`(`ME`·`OTHER`)이 붙었다. 서버가 **요청자의 계정 id와 배치자의
  > 계정 id를** 견주어 채우므로, 앱이 내 멤버십 행 id를 어디서 얻을지 정할 필요 자체가 사라졌다.
  > 예고된 두 선택지(서버에 "나" 표시 요청 / 그룹 상세 값 차용) 중 **서버가 첫째를 골랐고, 표시가
  > 아니라 판정 결과를 준다.** 그래서 C-301 편집 탭의 축이 다른 비교도 함께 걷어낼 수 있다
  > → [api/parfait.md](../api/parfait.md). **남은 것은 ②③이다** — ②는 앱이 이 값을 읽는 순간
  > 자동으로 정해지고, ③(C-305 목적지 부재)은 그대로다.
  > ✅ **②가 같은 날 닫혔다(2026-08-26, PR #376 develop 머지)** — `PlacedByResponse.ownerType`이
  > 생기고 `:data` 매퍼가 `"ME"` 여부를 `CanvasToppingVO.isMine`으로 접는다. 예고한 대로 ②는
  > 선택이 아니라 **읽는 순간 정해졌다**: 본인 토핑은 Spotlight에서 빠지고, C-301 편집 탭은
  > `GetMyAccountFlowUseCase` 의존과 축이 다른 비교를 통째로 버렸다. **판정이 게이트였던 화면에서
  > 남의 토핑을 만질 수 있었던 가능성이 사라진 것이 이 라운드의 실질**이다.
  > ⚠️ **③은 그대로이고, 증상만 바뀌었다** — 본인 토핑 탭이 "Spotlight로 잘못 들어간다"에서
  > **"아무 일도 안 한다"**가 됐다. `TODO: C-305 토핑 편집 화면으로 이동` 뒤에 그냥 `return`한다.
  > 정책([[C-202-토핑-편집자-확인-규칙-v0.1]])이 요구하는 편집 진입은 여전히 미구현이다.
  > ✅ **③이 닫혔다(2026-08-27, PR #400 develop 머지)** — `handleOnClickMyTopping`이
  > `NavKeyCanvasBGEdit`에 탭한 토핑 id를 실어 보내고, 편집 화면이 토핑 탭을 편 채 그 토핑을
  > 선택한 상태로 열린다. ⚠️ **예고했던 "C-305 화면 라운드"는 오지 않았다** — 정책이 별도 화면으로
  > 적은 목적지를 **기존 C-301 편집 화면의 토핑 탭**이 받았다. 위키 [[화면-ID-체계]]에서 C-305가
  > 독립 화면인 것과 구현이 갈린 자리이고, 이 미결이 닫히면서 그 어긋남이 자리를 물려받았다
  > → OQ-P-326 ③.
  > ⚠️ **조건이 하나 붙었다** — `isViewingToday`가 거짓이면(지난 캔버스를 보는 중) 탭이 여전히
  > 아무 일도 안 한다. 편집 대상이 언제나 오늘 캔버스라는 `NavKeyCanvasBGEdit`의 기존 계약을 따른
  > 결과인데, 정책([[C-202-토핑-편집자-확인-규칙-v0.1]])은 그런 조건을 적지 않는다.
- **해소 메모**: ①②는 2026-08-26 라운드에서, ③은 2026-08-27 라운드에서 닫혔다. 반영처는
  [c202-canvas-spotlight 스펙](../superpowers/specs/archive/2026-08-20-c202-canvas-spotlight.md) 정책 대조 표와
  as-built 재정정 두 절 · [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) ·
  [api/parfait.md](../api/parfait.md) Android 매핑 ·
  [architecture/navigation-flow](../architecture/navigation-flow.md) 캔버스 배경 편집 플로우다.
  **남은 두 가닥은 이 미결이 아니라 OQ-P-326이 잇는다** — 지난 캔버스 무반응과 화면 ID 어긋남이다.

### [2026-08-20] Spotlight 작성자 정보가 서버 값이 아니라 화면 목록 조인이다

- **ID**: OQ-P-251
- **출처**: `CanvasMainViewModel#handleOnClickTopping`(PR #298 develop 머지) — 토스트의 닉네임 색은
  `memberChips`에서 같은 `groupMemberId`를 찾아 얻는다. 서버는 `placedBy.nameTagChip`을 이미 주지만
  앱 DTO에서 멈춰 있어(OQ-P-224 잔여) 도메인까지 오지 않아서다. 그 조인을 위해 `GroupMemberChip`에
  `groupMemberId`가 붙었고 코드에 임시임을 밝히는 `TODO`가 있다. **목록에 없는 사람은 `Default`로
  떨어지는데 서버도 그 경우 `DEFAULT`를 준다**(`groupMembers`는 탈퇴자를 거르고 `placedBy`는 안
  거른다 — [api/parfait.md](../api/parfait.md)). 즉 두 경로의 결과가 **지금은 우연히 같아** 임시라는
  사실이 화면에 안 드러난다.
- **항목**: ① `placedBy.nameTagChip`을 VO까지 올려 조인을 걷을지 — 읽는 화면이 생겼으므로
  "소비자 0이라 도메인 모양을 굳히지 않는다"던 보류 사유는 사라졌다. **결과가 같은 지금이 옮기기
  좋은 시점이고, 서버가 두 목록의 배정 규칙을 갈라 놓으면 그때는 조용히 틀린 색이 된다.**
  ② 탈퇴·이탈 토스트 문안 — 서버가 닉네임을 `(알수없음)`으로 바꿔 주므로 뜻은 맞지만 문장이
  `(알수없음)님이 …`가 되어 정책 예시(`알 수 없는 사용자가 …`)와 형태가 다르다. 문안을 화면이
  다시 쓸지, 정책 쪽을 서버 문자열에 맞출지. ③ 상대 시각 갈래 경계(1분·1시간·1일·7일)와 `오래전`
  문구는 정책에 근거가 없다 — 코드가 정한 값을 정책으로 승격할지.
- **상태**: 미해결 (**①은 지금 증상이 없다** — 위 우연한 일치 때문이다)
- **해소 메모**: ①은 [api/parfait.md](../api/parfait.md)·[api/parfait-image.md](../api/parfait-image.md)의
  Android 매핑과 함께 갱신한다. ②③은 기획 확인이 선행이고, 정해지면 위키 쪽 미결로도 올린다.

### [2026-08-20] Spotlight 라운드가 판단이 몰린 순수 함수 둘을 테스트 없이 들여왔다

- **ID**: OQ-P-252
- **출처**: PR #298은 **신규 유닛 테스트가 0건**이다(머지 전후 develop 568건 그대로). 그런데 그
  라운드가 들여온 `Instant.toElapsedTimeBucket`(경계 넷 + 미래 시각 클램프)과
  `YGColorChipType.toSpotlightToastNameColor`(12타입 → 6색 접기)는 **판단이 몰린 순수 함수**이고,
  이 저장소는 그런 자리를 유닛으로 잠가 왔다(`ToppingGeometryTest`·`SegmentationMaskTest` 선례,
  [unit-test-infrastructure 스펙](../superpowers/specs/archive/2026-08-06-unit-test-infrastructure.md)).
  전자는 `core:util:jvm`에 있어 JVM 유닛으로 바로 덮이고, 후자는 `:feature:groups:canvas:impl`의
  `internal` 확장이라 그 모듈 테스트 소스셋이 필요하다.
- **항목**: ① 두 함수를 뒤늦게 덮을지, 아니면 다음에 이 자리를 고치는 라운드에 묶을지.
  ② 경계값을 테스트가 잠그면 OQ-P-251 ③(갈래 경계에 정책 근거가 없다)의 값이 **코드가 정본**임을
  명시적으로 굳히는 셈이라, 그 판단을 먼저 할지.
- **상태**: 미해결
- **해소 메모**: `toSpotlightToastNameColor`는 S-101·G-001·C-001의 칩 변환 셋과 같은 계열이라
  (그쪽은 `util/ColorChipType.kt`) 함께 볼 자리다 — 공용화 미결은 OQ-P-234 ④와 같은 뿌리다.

### [2026-08-20] 닉네임이 아직 없으면 그룹 만들기가 조용히 안 열린다

- **ID**: OQ-P-253
- **출처**: `GroupListViewModel#handleClickCreateNewGroup`(PR #312 develop 머지) — 계정 스트림이 첫
  값을 내놓기 전에 "그룹 만들기"를 누르면 `viewModelLogger.w`만 남기고 **아무 일도 일어나지 않는다.**
  근거는 "DataStore를 한 번 읽는 사이라 따로 알리지 않는다, 다시 누르면 열린다"이고, 화면에는
  로딩도 토스트도 없다. 같은 오버레이의 "초대 코드로 참여"는 닉네임을 안 받아 이 가드가 없다 —
  **같은 오버레이의 두 버튼이 서로 다르게 반응한다.**
- **항목**: ① 이 구간을 사용자에게 알릴지(버튼 비활성 · 로딩 · 토스트) 아니면 지금처럼 둘지.
  ② 알린다면 관용구를 무엇으로 할지 — 이 저장소의 "요청 중 표시"는 오버레이(약관 동의)와 항목
  비활성(S-001 로그아웃)으로 이미 갈려 있다(OQ-P-249 ②). ③ 계정 정보가 **영영 없는** 경우
  (부트스트랩 실패 후 로컬이 빈 상태)와 "아직 못 읽은" 경우를 구분하지 않는다 — 전자면 다시 눌러도
  영원히 안 열린다.
- **상태**: 미해결 (**발생 창이 좁다** — 로컬 DataStore 1회 읽기 구간이고, 그 사이 목록도 아직 비어 있다)
- **해소 메모**: ③이 실제로 가능한지는 [ADR-0022](../adr/0022-user-info-local-ssot.md)의 부트스트랩
  실패 처분과 함께 봐야 한다 — 인증 거절일 때만 세션을 파기하므로 "토큰은 있는데 계정 정보가 없는"
  상태가 남을 수 있다. **OQ-P-196 ②가 S-002에서 지적한 것과 같은 뿌리**이고, 이 화면은 비활성
  대신 무반응이라는 점만 다르다.
  > ⚠️ **가드는 그대로인데 그것을 세우던 근거가 흔들렸다(2026-08-27, PR #393)** — 그때까지 A-005의
  > 닉네임 필드는 `enabled = false`에 no-op `onValueChange`라, 넘겨받은 닉네임이 **고칠 수 없는
  > 표시값**이었다. 그래서 값이 없으면 화면을 열 이유도 없었다. 이제 그 필드가 열려 사용자가
  > 직접 입력하고 그룹명과 같은 검사를 받으므로, 넘어오는 값은 **초기값**이다 — 빈 채로 열어도
  > 사용자가 채울 수 있다. ①(알릴지 말지)에 "가드를 없애고 그냥 연다"는 선택지가 하나 늘었고,
  > ③(계정 정보가 영영 없는 경우)의 무게도 그만큼 줄었다. `GroupListViewModel#handleClickCreateNewGroup`은
  > 바뀌지 않았다.

### [2026-08-21] C-301 배경 편집에서 테두리를 다시 편집해도 화면이 그대로다

- **ID**: OQ-P-254
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditScreen`·`CanvasBGEditViewModel#CanvasToppingItem`
  (#334 develop 머지, 2026-08-22) — 이 화면은 이미 놓인 토핑을
  `borderOnly`로 다시 편집하는 경로를 갖고 있고([ADR-0026](../adr/0026-topping-draft-datastore-ssot.md)이
  `TOPPING_EDIT_RESULT_KEY`를 걷지 않은 이유가 이 경로다), 편집 결과의 테두리 값을 받아
  `CanvasToppingItem.borderLayers`에 담아 둔다. 그런데 토핑을 그리는 자리는 편집 결과 **이미지 하나만**
  읽고 그 값을 보지 않는다. 테두리를 두르든 벗든 화면이 그대로다.
- **항목**: ① 이 화면을 `:core:designsystem`의 `YGToppingCutoutImage`로 갈아태워 `borderLayers`를
  그릴지 — 값도 컴포저블도 이미 있어 붙이는 일만 남는다. ② 아니면 C-301 라운드가 이 화면의 미리보기를
  다시 설계할 때 함께 볼지(지금 미리보기는 컷 도형·Dot Grid·날짜 라벨이 없는 목업이고 그 어긋남은
  OQ-P-174가 쥐고 있다).
- **상태**: 해소됨 (2026-08-27, PR #388 — ①로 닫혔다. **저장 쪽 OQ-P-276은 잔존**)
  > ✅ **①이 그대로 일어났다(2026-08-27, PR #388)** — `CanvasToppingImage`가 맨 `Image` 대신
  > `YGToppingCutoutImage`를 쓰고 `borderLayers` 첫 겹의 색·두께를 넘긴다. 계기는 이 항목이 아니라
  > [토핑 알파 판정](../superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md)이었다 — 판정 모양을 외형과
  > 맞추려면 두 화면이 같은 그림을 그려야 해서, 렌더링 수정이 그 스펙의 범위로 들어왔다. 색을 못 읽거나
  > painter가 성공 상태가 아니면 안 그리는 조건이 캔버스 메인과 같다. **받아 두고 안 보내는 쪽(OQ-P-276)은
  > 그대로다.** 새로 보이게 된 테두리와 코너 스트로크·버튼의 관계는 OQ-P-315가 쥔다.
  > ⚠️ **그 화면을 다시 만진 라운드가 이것을 건너뛰었다(2026-08-22, PR #329)** — 토핑 그리는 자리가
  > 통째로 재작성돼(mock → 서버 URL, 오프셋 → 비율, 새 크기 계산) `rememberToppingPainter`가 생겼는데도
  > 여전히 **편집 결과 이미지 하나만 읽는다.** ①(`YGToppingCutoutImage`로 갈아태우기)이 붙을 자리가
  > 그때 손댄 그 함수라, 다음 라운드에는 "값도 컴포저블도 이미 있다"에 **"그리는 함수도 이미 새로
  > 썼다"**가 더해진다.
  > ⚠️ **같은 값이 저장 쪽에서도 빠졌다(2026-08-23, PR #336)** — 확인 버튼이 이동·크기·회전을
  > PATCH 하면서 `borderLayers`는 비교에도 요청에도 넣지 않았다. 이제 이 값은 **받아 두고 안 그리고,
  > 받아 두고 안 보낸다** → OQ-P-276. 둘은 한 라운드에서 함께 닫는 편이 낫다.
- **해소 메모**: 붙일 때 함께 걷기로 했던 `CanvasToppingItem`의 KDoc도 같은 라운드에서
  사실에 맞게 고쳐졌다 — `editedImagePath`를 "테두리를 새로 구운 이미지"라고 적던 문장이
  "테두리는 픽셀에 굽지 않고 `borderLayers`로 따로 나른다"로 바뀌었다 — ADR-0025 전환(PR #334)
  이후로 거짓이던 문장이다. 아래는 닫히기 전의 기록이다.
  이 화면이 테두리를 **그린 적은 없다.** 그전에는 편집이 돌려주던 파일에 테두리가 이미
  구워져 있어 화면이 아무것도 하지 않아도 반영된 것처럼 보였고,
  [ADR-0025](../adr/0025-topping-border-as-server-field.md) 전환이 굽기를 멈추면서 **표시 경로가 없다는
  사실이 드러났다.** 그래서 이 라운드는 필드 이름만 맞추고(그 자리가 받는 것이 구운 판에서 알맹이로
  바뀌었다) 표시는 손대지 않았다. 붙일 때 `CanvasToppingItem`이 지금 들고 있는 경고 KDoc도 함께 걷는다.

### [2026-08-21] 누끼 알맹이를 최근 이미지로 재사용하려면 선행 결함 넷을 먼저 닫아야 한다

- **ID**: OQ-P-255
- **출처**: [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) PR6(누끼 알맹이
  재사용) 계획 전 실사 — 배치 성공 시 테두리 없는 알맹이를 최근 이미지에 저장하고 최근 목록에서 골라
  누끼 확인 화면으로 직행시키려면, 기존 최근 이미지 경로가 다음 넷을 견디지 못한다.
- **항목**:
  1. `FileRecentImageLocalDataSourceImpl#readBytes`가 `contentResolver.openInputStream(uri)` 전용이라
     스킴 없는 절대경로를 못 읽는다. `AddRecentImageUseCase`가 `runSuspendCatching`으로 감싸므로
     **아무 일도 안 일어난 채 성공처럼 지나간다.**
  2. 같은 파일이 확장자를 `.jpg`로 하드코딩한다(`FILE_EXTENSION`). 알맹이는 투명 PNG라 이름이
     거짓이 되고, `ImageUploadRepositoryImpl#contentTypeOf`의 확장자 판정과 부딪힌다.
  3. 최근 목록이 `List<String>`이라 종류 축이 없다. 스키마를 넓히면
     `RecentImageLocalDataSourceImpl#decode`의 `runCatching { … }.getOrDefault(emptyList())`가 구
     스키마 디코드 실패를 삼켜 **기존 목록이 통째로 날아가고 파일은 고아로 남는다**
     (`GetRecentCacheImagesUseCase#clearOutsideDayWindow`가 목록 기준이라 못 지운다).
  4. `NavKeySegmentationConfirm`이 인자 셋을 요구하고 그 화면의 `onClickEditPhoto`가
     `sourceImageUri`·`cutoutImagePath`를 둘 다 쓴다 — 알맹이만 복원하면 **"사진 편집" 버튼이 죽는다.**
     셋을 다 저장하면 내부 저장소 사용량이 3배가 되면서 `MAX_SIZE = 9`와 부딪힌다.
- **상태**: 해소됨 (2026-08-21, PR6 설계·구현 — 넷 다 처방이 정해졌고
  [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md)의 "누끼 알맹이 재사용
  (PR6)" 절이 정본이다. 구현은 2026-08-22 develop 머지됐다 — PR #334)
- **해소 메모**: ①은 절대경로 전용 `readFileBytes(path)`를 따로 두고 종류가 읽기 경로를 가르는 것으로,
  ②는 `getTargetFile(bytes, extension)`으로 확장자를 인자화해 알맹이가 `.png`를 받는 것으로 닫았다.
  ③은 **2단 폴백 디코드**다 — 신 스키마가 실패하면 구 `List<String>`으로 한 번 더 시도해 종류를
  `SOURCE`로 올려받으므로 기존 목록이 사라지지 않는다(목록을 둘로 가르는 안은 상한 9가 목록마다 따로
  걸려 최대 18장이 되고 시간순 병합이 이중으로 생겨 기각했다). ④는 **저장 대상을 알맹이 1장으로
  좁혀** 풀었다 — `NavKeySegmentationConfirm`의 원본·마스크 인자를 nullable로 넓히고 재사용 항목에서는
  "사진 편집"을 잠갔다(🔁 **2026-09-01 develop 머지로 뒤집혔다, 이슈 #424 · PR #425** — 잠그는 대신
  테두리 편집만 여는 것으로 바뀌었다. 저장 대상이 알맹이 1장뿐인 것은 그대로이고, 영역 편집이 여전히 불가능하다는 사실도 같다). 셋을 다 저장하면 용량이 3배가 되면서 `MAX_SIZE = 9`와 정면으로 부딪히고,
  다시 편집하는 길은 갤러리의 원본에서 새로 시작하는 형태로 이미 있다.
  설계 중 **선행 결함 하나가 더 드러났다** — 확인 화면은 초안에 알맹이가 적혀 있어야 다음 버튼이
  열리는데(`isDraftReady`) 재사용 진입은 그것을 적는 두 경로(세그멘테이션·편집 결과)를 모두 타지
  않는다. 확인 화면이 스스로 `record`를 먼저 마친 뒤 구독을 여는 것으로 처방했고, 순서를 뒤집으면
  첫 방출의 `null`이 `DraftMissing` 토스트를 쏜다.

### [2026-08-21] `Int.toRgbHexString()`의 불투명 가드가 `require()` — 반투명 색이 팔레트에 들어오면 크래시다

- **ID**: OQ-P-256
- **출처**: PR5 최종 브랜치 리뷰 — `core/util/android`의 `String.kt#toRgbHexString`이 알파가
  불투명(`0xFF`)이 아니면 `require()`로 던진다. 호출부
  `CanvasToppingPlaceViewModel#toToppingBorder`는 `handleOnClickConfirm`에서 `launch { }` **앞에**
  동기로 불린다 — `BaseViewModel.launch`의 `try`(성공·실패·예외·취소 네 경로를 한 곳에서 덮는 그
  블록) 밖이라, 여기서 `require()`가 던지면 어디에도 안 걸리고 그대로 크래시한다.
- **항목**: ① 지금은 이 화면의 `borderColorArgb`가 팔레트가 주는 불투명 색뿐이라 도달 불가능하지만,
  그 전제가 코드 어디에도 강제돼 있지 않다 — 팔레트에 반투명 색이 하나라도 들어오면 사용자 손에서
  터진다. `require()`를 `runCatching`으로 감싸 `ToppingBorder.None`으로 폴백할지, 팔레트 타입을 좁혀
  반투명을 아예 표현 불가능하게 만들지, 아니면 이 상태를 계속 감수할지. ② 감수한다면 그 전제(팔레트가
  불투명만 준다)를 어디에 못박아 다음 사람이 같은 가정을 다시 확인하지 않게 할지.
- **상태**: 부분 해소 (**크래시 경로는 닫혔고 ①의 "표현 불가능하게 만들기"와 ②만 남았다** —
  2026-08-21, PR5 브랜치 코드리뷰 대응)
  > ✅ **크래시가 정상 실패 경로가 됐다** — `toToppingBorder` 호출을 `launch { }` **안**,
  > `addToppingUseCase` 호출 **앞**으로 옮겼다. 던지면 `BaseViewModel.launch`의
  > `catch (e: Throwable)` → `onError` → `PlaceFailed`로 흡수되고, `finally`가 `isLoading`을
  > 내린다. 업로드보다 앞이라 **고아 이미지도 안 남는다.**
  > `CanvasToppingPlaceViewModelTest#onClickConfirm_nonOpaqueBorderColor_failsInsteadOfCrashing`이
  > 그 경로를 잠근다(호출을 다시 `launch` 밖으로 빼면 `processIntent`가 그 자리에서 던져 실패한다).
  >
  > **`require()`는 그대로 뒀다** — `toRgbHexString`은 `core:util:android`의 public 확장이라
  > 누구든 부를 수 있고, public API 경계의 인자 검증은 그 자리가 맞다. 문제는 검사가 아니라
  > **호출부가 그것을 안 받는 것**이었다.
  >
  > **`ToppingBorder.None` 폴백은 기각했다** — 사용자가 고른 테두리가 말없이 사라진 채 서버에
  > 저장된다. 이 라운드가 6자리 전환으로 내내 피하려던 무증상 실패와 같은 부류다. 읽기 쪽
  > `toColorOrNull` → null → 안 그리기는 **이미 저장된 잘못된 값을 표시하는** 처리라 성격이 다르다.
  > ⚠️ **같은 계약을 향하는 두 번째 함수가 생겼다(2026-08-22, PR #329)** — `Color.toRgbHex()`는
  > 알파를 **조용히 버리고**(테스트가 그 동작을 잠근다) 로케일을 `Locale.US`로 고정한다. 즉 반투명
  > 색을 만났을 때 한 경로는 던지고 다른 경로는 통과하며, **로케일 결함은 기존 함수에만 남았다**
  > → OQ-P-263.
- **해소 메모**: 남은 것은 **타입으로 불변식을 옮기는 것**이다 — 불투명 색 전용 타입을
  팔레트 → 편집 화면 → 초안 → ViewModel까지 관통시키면 반투명 색이 표현 불가능해지고 `require()`도
  필요 없어진다. 커스텀 컬러피커처럼 진입점이 실제로 늘어나는 라운드에서 함께 한다.
  ②(전제를 어디에 못박을지)는 이 항목과 `toRgbHexString` KDoc이 지금 그 역할을 한다.

### [2026-08-21] 갤러리 "최근"에서 누끼 알맹이 셀을 무엇으로 구별할지 시안이 없다

- **ID**: OQ-P-257
- **출처**: [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md) PR6 설계 —
  최근 목록에 종류 축이 생기면서 원본 사진과 누끼 알맹이가 같은 "최근 업로드" 섹션에 섞인다. 알맹이는
  투명 PNG이고 지금 셀은 `ContentScale.Crop`이라, 여백을 걷어낸 객체가 잘린다.
- **항목**: ① 종류를 알리는 시각 장치를 둘지 — 뱃지·셀 배경 구분·섹션 분리 셋이 후보다. ② 흰 배경
  위의 투명 알맹이가 실제로 알아볼 만한지는 실기기에서만 판정된다.
- **상태**: 미해결 (**디자인 시안 대기** — PR6는 시각 장치를 두지 않고 `ContentScale.Fit`만 적용했다)
- **해소 메모**: PR6가 `Fit`을 고른 것은 잘림을 막는 최소 조치이지 표시 정책의 확정이 아니다. 위키에
  갤러리 최근 섹션의 정책 근거가 없어 이 항목이 그 자리를 대신 지킨다. 흰 배경 위의 투명 알맹이가
  실제로 알아볼 만한지는 이 브랜치의 실기기 확인 2항이 판정한다.

### [2026-08-21] 최근 목록 상한 9를 토핑 흐름 하나가 두 칸씩 먹는다

- **ID**: OQ-P-258
- **출처**: PR6 최종 브랜치 리뷰 — 토핑 만들기에 진입하면 고른 원본 사진이 한 칸을 먹고, 배치에
  성공하면 알맹이가 또 한 칸을 먹는다. `MAX_SIZE = 9`는 그대로라 **최근 사진이 전보다 두 배 속도로
  밀려난다.**
- **항목**: ① 상한을 올릴지(내부 저장소 사용량이 함께 는다) ② 종류별로 상한을 나눌지(목록이 둘로
  갈려 시간순 정렬과 데이 윈도우 정리가 이중이 된다 — PR6가 그 이유로 단일 목록을 골랐다)
  ③ 지금대로 감수할지.
- **상태**: 해소됨 (**②로 닫혔다** — 2026-08-31, PR #408 develop 머지)
- **해소 메모**: 데이 윈도우 정리(03:00 밖이면 삭제)가 이미 목록을 매일 비우므로 누적되지는
  않는다. 문제가 되는 것은 같은 날 토핑을 여러 개 만들 때다.

  > ✅ **②를 골랐고, PR6가 그것을 기각한 근거는 실제로 발생하지 않았다(2026-08-31, PR #408)** —
  > `MAX_SIZE` 가 `MAX_SIZE_PER_KIND` 가 되어 원본과 알맹이가 각자 정원을 든다. PR6는 ②를
  > "목록이 둘로 갈려 시간순 정렬과 데이 윈도우 정리가 이중이 된다"는 이유로 물렀는데,
  > **저장 목록을 가르지 않고 자르는 판정만 종류별로 두면** 둘 다 이중이 되지 않는다 —
  > 자른 결과가 아니라 덧붙인 목록을 살아남은 URI 집합으로 다시 걸러 시간순을 지킨다.
  > 데이 윈도우 정리는 목록 하나를 그대로 보므로 손대지 않았다.
  > 겸해 **최근 줄에 무엇을 싣는지가 `returnResultOnly` 에서 신설 `RecentImagePick` 으로 갈렸다** —
  > 두 플래그가 우연히 같은 방향을 가리키던 것을 부르는 쪽이 직접 고르게 바꾼 것이다
  > ([navigation-flow](../architecture/navigation-flow.md) 「인자 있는 목적지」).
  > ⚠️ **①이 걱정한 저장소 사용량은 그대로 남는다** — 총 상한이 두 배가 됐고 잰 사람이 없다 →
  > OQ-P-332.

### [2026-08-22] 앱 전역 화면 전환이 루트 두 곳에 복제됐다

- **ID**: OQ-P-259
- **출처**: `app/MainRoute.kt` · `app-preview/route/RootRoute.kt`(PR #326 develop 머지) — 두 파일이
  `NavDisplay`의 `transitionSpec`·`popTransitionSpec`·`predictivePopTransitionSpec`에
  `NavTransition.Default`를 물리는 **같은 세 줄**을 각각 들고 있다. `NavTransition`은 프리셋을
  공유하지만 **그것을 `NavDisplay`에 붙이는 일**은 공유되지 않는다. 앱 기본을 바꿀 때 한쪽만 고치면
  컴포넌트 갤러리(app-preview)와 본 앱의 전환이 갈리고, 그것을 잡을 기계 검사는 없다.
- **항목**: ① 붙이는 쪽을 하나로 뺄지 — `NavTransition` 쪽에 `NavDisplay` 인자 묶음을 주는 확장이나
  두 루트가 함께 쓰는 컴포저블이 후보다. ② 아니면 app-preview는 본 앱 전환을 따를 이유가 없다고 보고
  복제를 의도로 확정할지(그렇다면 두 파일에 그 뜻을 적어야 한다).
- **상태**: 미해결
  > 📌 **복제가 줄지 않고 얇아지기만 했다(2026-09-20, PR #514)** — 두 파일 모두 `NavDisplay`를 감싸던
  > `SharedTransitionLayout` + `CompositionLocalProvider(LocalSharedTransitionScope …)` 껍질을 벗고
  > `NavDisplay`를 바로 부른다(`modifier`가 껍질 대신 `NavDisplay`로 내려갔다). 공유 요소를 쓰는
  > 화면이 하나도 안 남아서다. **복제된 줄 수는 줄었지만 복제 자체는 그대로다** — 데코레이터 셋과
  > 전환 세 줄은 여전히 두 파일에 나란히 있고, 이번에도 **같은 편집을 양쪽에 두 번** 했다. ①②는
  > 그대로 열려 있다.
- **해소 메모**: 같은 부류가 이미 하나 있다 — 데코레이터 세 개(`SaveableStateHolder`·`ViewModelStore`·
  `ResultEventBus`)도 두 루트에 나란히 복제돼 있다. 즉 이 항목은 전환만의 문제가 아니라 **두 루트가
  `NavDisplay` 설정을 통째로 복제하는 형태**를 어떻게 둘지의 문제다 →
  [navigation-flow](../architecture/navigation-flow.md) 「화면 전환」.

### [2026-08-22] 새 전환을 아무도 본 적이 없고, 예외를 붙인 화면은 도달 불가다

- **ID**: OQ-P-260
- **출처**: `core/navigation/NavTransition.kt` · `feature/groups/canvas/impl/navigation/EntryBuilder.kt`
  (PR #326 develop 머지) — ① `NavTransitionTest`가 잠그는 것은 **세 슬롯이 비지 않았다**는 것뿐이라
  전환의 모양·시간·방향은 실기기에서만 판정된다. ② `predictivePop`은 시스템 predictive back 제스처가
  실제로 `swipeEdge`를 물어다 줘야 도는데 그 경로를 밟아 본 기록이 없다(`targetSdk` 36이라 opt-out을
  하지 않는 한 켜져 있다는 것이 근거의 전부다). ③ 유일한 예외인 `NavTransition.Fade`는
  `NavKeyCanvasEdit`에 붙었는데 그 화면과 짝인 `NavKeyCanvasImageSelect`는 **`goTo` 호출부가 0건**이라
  (OQ-P-129 ②) 공유 요소 전환도 그 예외도 실행되지 않는다. ④ `Default.push`는 나가는 화면을
  `ExitTransition.KeepUntilTransitionsFinished`로 붙들어 두므로, 무거운 화면이 전환 동안 한 프레임에
  둘 다 그려진다 — 체감 비용도 미측정이다.
- **항목**: ① 실기기 확인 항목으로 옮길지(쌓기·뒤로·가장자리 제스처 좌우 각 1회). ② `Fade` 예외의
  값은 그 짝이 도달 가능해질 때까지 판정을 미룰 수밖에 없는데, 그때 이 예외가 아직 맞는지 다시 볼지.
- **상태**: 부분 해소 (③ 소멸 — 예외가 화면째 삭제, 2026-09-20 PR #514 / ①②④ 잔존)
  > ✅ **③이 판정 없이 사라졌다(2026-09-20, PR #514)** — `NavKeyCanvasEdit`과 그 짝
  > `NavKeyCanvasImageSelect`가 삭제되면서 `NavTransition.Fade.metadata`를 다는 엔트리도 사라졌다.
  > 지금 develop의 모든 화면이 `Default`를 쓴다. **②는 답 없이 소멸했다** — "그 짝이 도달
  > 가능해질 때 다시 본다"던 조건이 성립할 수 없게 됐다.
  > ⚠️ 그래서 **`Fade` 프리셋 자체가 사용처 0으로 남았다** → OQ-P-404. ①④(실기기 미확인)는 그대로다.
- **해소 메모**: ①·④는 실기기 이월 목록에 붙는다 —
  [doc-baseline](../doc-baseline.md) 「현재 기준선」의 실기기 항목과 같은 줄에서 관리한다.

### [2026-08-22] 마감된 캔버스의 409를 배경 저장이 일반 오류로 접는다 — 같은 코드에 두 처분이 생겼다

- **ID**: OQ-P-261
- **출처**: `feature/groups/canvas/impl/viewmodel/CanvasBGEditError.kt`·`CanvasBGEditViewModel#toCanvasBGEditError`
  (PR #329 develop 머지) — 사유 enum이 `NETWORK`·`UNSUPPORTED_IMAGE`·`UNKNOWN` 셋이고 `AppError.Server`는
  전부 `UNKNOWN`으로 접힌다. 그런데 서버는 마감된 파르페의 배경 변경을 409 `PARFAIT_ALREADY_CLOSED`로
  거절하고([api/parfait.md](../api/parfait.md)), 그 상황은 **03시 회전을 걸쳐 화면이 오래 열려 있으면
  실제로 나온다.** 사용자가 보는 문구는 "잠시 후 다시 시도해 주세요"인데 **다시 눌러도 영원히 실패한다.**
  같은 상수를 C-106 배치는 `isPermanentPlaceFailure()`로 읽어 **토스트 후 캔버스로 되감는다**(OQ-P-244 ②)
  — 한 서버 코드에 두 화면의 처분이 갈렸다.
- **항목**: ① 배경 저장도 배치와 같은 처분(알리고 되감기)을 쓸지 — 되감으면 캔버스 메인이 재조회로 새
  캔버스를 받으므로 사용자가 다시 시작할 수 있다. ② 그 판정을 화면마다 다시 쓰지 않게 공용화할지
  (`isPermanentPlaceFailure`가 이미 `feature/groups/canvas/impl/util/`에 있다). ③ 되감기가 토스트를 같은
  프레임에 폐기하는 함정(OQ-P-167)이 여기도 그대로 걸리는지 — 이 화면의 `toastPolicy`도 Route 컴포지션에
  매달려 있다.
- **상태**: 미해결
  > ⚠️ **한 번의 확인이 같은 409를 두 처분으로 낸다(2026-08-23, PR #336)** — 마감된 캔버스에서
  > 확인을 누르면 토핑 위치 PATCH도 배경 PATCH도 409인데, **토핑 쪽은 로그 한 줄이고 배경 쪽만
  > 토스트**다. 그전까지 이 항목이 "화면마다 처분이 갈렸다"였다면 이제 **한 화면·한 버튼 안에서**
  > 갈린다. 이로써 같은 서버 코드의 처분은 넷이다 — 되감기(C-106 배치, 나중에 뒤집힘) · 알리고
  > 남기(C-106 최종) · 일반 오류 토스트(배경 저장) · 무반응(토핑 삭제·위치 수정).
  > ②(판정 공용화)의 값이 그만큼 커졌다 → OQ-P-275.
  > 🔁 **처분이 넷에서 셋으로 줄었다(2026-08-28, 브랜치 `feature/canvas-polling` — develop 미머지)** —
  > 무반응 갈래(토핑 삭제·위치 수정)가 사라지고 **일반 오류 토스트 + 화면 잔류**로 합쳐졌다
  > (OQ-P-270 ① · OQ-P-275 ③). 남은 셋은 되감기(C-106 배치, 나중에 뒤집힘) · 알리고 남기(C-106
  > 최종) · 일반 오류 토스트(배경 저장·토핑 저장·토핑 삭제)다. **①(409를 "마감된 캔버스"로 갈라
  > 말할지)은 그대로 열려 있다** — 이번 라운드는 셋을 한 문구로 접는 쪽을 택했고, 그 선택이
  > 409를 다른 서버 코드와 같은 자리에 두었다.
  > ✅ **셋으로 줄어든 처분이 실제로 코드에 있다(2026-08-31 확인, 브랜치
  > `feature/#427-sync-backend-api-260831`)** — `CanvasBGEditViewModel.handleOnClickConfirm`·
  > `failToDeleteTopping`·`failToSave`를 직접 읽어 셋 다 토스트 + 화면 잔류인 것을 확인했다.
  > **다만 "완전히 같다"는 아니다** — ⓐ 배경 실패는 `toCanvasBGEditError`로 원인별 코드
  > (`NETWORK`·`UNSUPPORTED_IMAGE`·`BACKGROUND_SAVE_UNKNOWN`)를 가르는데 토핑 저장 실패는 원인을
  > 안 가리고 늘 `TOPPING_SAVE_UNKNOWN`이다. ⓑ **같은 확인에서 배경과 토핑이 함께 실패하면 배경
  > 쪽 토스트만 뜬다** — `handleOnClickConfirm`의 `when`이 `savedBackground == null`을 토핑 실패
  > 분기보다 먼저 매칭해서다(토핑 실패는 `dirtyToppingIds`에는 남아 재시도되지만 그 사실을 이
  > 확인에서는 안 알린다). ①은 여전히 열려 있다 → [api/parfait-image.md](../api/parfait-image.md)
  > Android 매핑.
- **해소 메모**: ①을 정하면 [c301 배경 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)의
  as-built 절 "실패 표현"과 [api/parfait.md](../api/parfait.md) Android 매핑을 함께 고친다. ③은 OQ-P-167이
  쥔 "안내를 캔버스 쪽 토스트 호스트로 보내기"와 같은 자리다.

### [2026-08-22] 업로드용 캐시 복사본이 쌓이기만 하고 지우는 코드가 없다

- **ID**: OQ-P-262
- **출처**: `data/source/image/local/ImageFileLocalDataSourceImpl#copyToCache`(PR #329 develop 머지) —
  고른 사진을 `cacheDir/upload`에 `UUID` 이름으로 떨구는데 **읽는 쪽도 지우는 쪽도 없다.** 업로드가
  성공하든 실패하든 남고, 같은 사진을 여러 번 고르면 이름이 달라 매번 새 파일이 된다. 이름을 UUID로
  둔 것 자체는 근거가 있다(갤러리 `content://`의 마지막 조각은 확장자 없는 숫자 id라 같은 사진을 두 번
  고르면 앞 파일을 덮어쓴다). 비교 대상이 있다 — 세그멘테이션 캐시는 `SegmentationCacheDir` +
  `ClearSegmentationCacheUseCase`로 진입 시 비우고, 최근 이미지는 `DayWindow`로 축출한다. **이 디렉토리만
  정책이 없다.**
- **항목**: ① 언제 지울지 — 업로드 성공 직후(그 파일을 다시 쓸 일이 없다)·화면 진입 시 통째로·날짜
  윈도 중 어느 쪽인지. ② 실패한 업로드의 복사본을 재시도용으로 남길 이유가 있는지(지금은 재시도가
  uri부터 다시 복사한다 — 남겨도 안 쓴다). ③ 정리 주체를 DataSource에 둘지 UseCase로 올릴지(세그멘테이션
  선례는 UseCase다).
- **상태**: 미해결
- **해소 메모**: 캐시 디렉토리는 OS가 압박 시 회수하므로 용량이 무한히 늘지는 않지만, **회수 시점이
  앱 통제 밖**이라 그때 사라지는 파일이 무엇인지는 정해 두는 편이 낫다(토핑 초안이 캐시 파일 경로를
  가리키는 것과 같은 부류의 함정이다 → [ADR-0026](../adr/0026-topping-draft-datastore-ssot.md)).
  정하면 [data-layer](../architecture/data-layer.md) "DataSource 종류"의 파일 기반 항목에 적는다.

  📌 **같은 디렉토리에 정책이 있는 파일이 생겼다**(2026-09-09, PR #473) — 업로드 전처리가 만드는
  축소본도 `cacheDir/upload` 에 UUID 이름으로 앉는데, 이쪽은 업로드의 성공·실패와 무관하게
  `finally` 에서 지운다(`PreparedUploadImage.isTemporary` 가 그 책임을 나른다). 이 항목이 묻는 ①의
  답을 한 종류에 대해서는 이미 낸 셈이다 — **업로드가 끝나면 즉시**. 남은 것은 `copyToCache` 가
  떨구는 복사본이고, 이제 **같은 디렉토리에 수명 정책이 있는 파일과 없는 파일이 섞여 있다.**

### [2026-08-22] 색을 `#RRGGBB`로 적는 함수가 둘이고, 알파 처리가 서로 정반대다

- **ID**: OQ-P-263
- **출처**: `core/util/android`의 `String.kt#Int.toRgbHexString()`(C-106 라운드)와
  `Color.kt#Color.toRgbHex()`(PR #329 신설) — **같은 서버 계약(6자리 HEX)을 향하는 변환이 둘**인데
  거동이 갈린다. 앞의 것은 알파가 불투명이 아니면 `require()`로 던지고(OQ-P-256이 그 크래시 경로를
  다뤘다), 뒤의 것은 **알파를 조용히 버린다** — 그 동작이 `ColorTest.toRgbHex_translucentColor_dropsTheAlpha`로
  잠겨 있어 실수가 아니라 의도다. 로케일 처리도 다르다: 새 함수만 `Locale.US`를 고정하고, 기존 함수의
  `"#%06X".format(...)`은 **기본 로케일을 탄다** — 아라비아·데바나가리 숫자를 쓰는 기기에서 서버가 못
  읽는 문자열이 된다. 즉 **새로 쓴 쪽이 옳고 먼저 있던 쪽에 결함이 남아 있다.**
- **항목**: ① 하나로 합칠지 — 합친다면 알파를 버릴지 던질지 정해야 하고, 둘은 호출 맥락이 다르다
  (테두리 색은 사용자가 고른 값이라 조용히 바꾸면 안 되고, 배경색은 팔레트라 알파가 애초에 없다).
  ② 합치지 않는다면 두 함수의 KDoc이 서로를 가리켜 다음 사람이 아무거나 고르지 않게 할지.
  ③ 기존 함수의 로케일 결함은 어느 쪽으로 가든 지금 고쳐야 한다.
- **상태**: 미해결 (③은 **실기기 로케일 1회로 갈리는 항목**이다 — 한국어 기기에서는 드러나지 않는다)
- **해소 메모**: OQ-P-256이 남긴 "타입으로 불변식을 옮긴다"(불투명 색 전용 타입)가 ①의 답이 되면
  두 함수가 함께 정리된다. 그때까지는 ②③만으로도 값이 있다.

### [2026-08-22] 세로 고정을 떠받치는 opt-out이 `targetSdk 37`에 사라지는데 대화면 방침이 없다

- **ID**: OQ-P-264
- **출처**: `app/src/main/AndroidManifest.xml`·`app-preview/src/main/AndroidManifest.xml`의
  `PROPERTY_COMPAT_ALLOW_RESTRICTED_ORIENTATION_AND_ASPECT_RATIO_OPT_OUT`(PR #339 develop 머지,
  [ADR-0027](../adr/0027-portrait-orientation-lock.md)) — `targetSdk 36`은 smallest width 600dp
  이상에서 `screenOrientation`을 무시하고, 이 속성이 그 예외를 되돌린다. **속성 자체가
  `targetSdk 37`부터 제거돼 무력화된다**는 사실은 지금 **매니페스트 주석의 TODO 한 줄에만** 있다.
  그날 태블릿·폴더블은 눕는데, 가로 규격은 위키 정책에도 구현 문서에도 없다(목록 지그재그 좌표·
  C-101 뷰파인더 여백·달력 그리드가 전부 세로 폭 실측이다).
- **항목**: ① 대화면에서 무엇을 보일지 — 세로 레이아웃을 가운데 두고 여백을 주는 letterbox인지,
  가로 규격을 새로 만드는지. ②는 ①이 정해질 때까지의 방어다 — `targetSdk` 상향 PR이 이 항목을
  밟게 할 자리가 매니페스트 주석뿐인지, 아니면 문서·체크리스트에 걸지. ③ 대화면 opt-out이 지금
  실제로 먹는지 자체가 미확인이다(폰에서는 드러나지 않는다).
- **상태**: 미해결
- **해소 메모**: ①을 정하면 [ADR-0027](../adr/0027-portrait-orientation-lock.md) "영향"의 시한부
  항목과 매니페스트 TODO를 함께 닫는다. `targetSdk` 상향은 [ADR-0003](../adr/0003-convention-plugins-version-catalog.md)
  컨벤션 플러그인 한 곳에서 갈리므로 그 자리가 트리거로 쓸 만하다.

### [2026-08-22] 세로 고정 뒤 카메라 촬영이 기기를 든 방향을 따라가지 않는다

- **ID**: OQ-P-265
- **출처**: `feature/camera/impl/.../route/CustomCameraRoute.kt`(`ImageProxy#imageInfo.rotationDegrees`를
  `saveViewfinderCapture`에 넘긴다)와 PR #339의 `screenOrientation="portrait"` — 이 앱은
  `ImageCapture#setTargetRotation`을 부르지 않고 `OrientationEventListener`도 두지 않는다.
  즉 보정 기준이 **표시 방향**인데 그것이 이제 항상 세로로 고정된다. 고정 전에는 액티비티가 돌면서
  기준도 함께 돌았으므로, **가로로 들고 찍었을 때의 결과가 이번 라운드에서 바뀌었다.**
  촬영 결과는 누끼·배치·캔버스까지 그대로 흘러가므로 한 번 누우면 흐름 끝까지 눕는다.
- **항목**: ① 가로로 든 촬영을 어떻게 다룰지 — 센서 방향을 읽어 보정할지(`OrientationEventListener` +
  `targetRotation`), 세로로 들도록 화면이 안내할지, 지금 거동을 그대로 둘지. ② 정책 근거가 없다 —
  위키 [[카메라-뷰파인더]]는 여백·블러만 말하고 방향을 말한 적이 없다. ③ 갤러리에서 고른 사진에는
  해당하지 않는다(EXIF를 디코더가 본다) — 두 입구의 결과가 갈리는지 확인이 필요하다.
- **상태**: 미해결 (**실기기 1회로 갈린다** — 에뮬레이터 회전은 표시 방향만 돌려서 재현이 다르다)
- **해소 메모**: ①이 "보정한다"로 가면 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)
  촬영 절과 [ADR-0027](../adr/0027-portrait-orientation-lock.md) 트레이드오프 항목을 함께 고친다.

### [2026-08-23] 다중 후보를 들고 있는 동안 비트맵을 해제하지 않는다

- **ID**: OQ-P-266
- **출처**: [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)
  — 후보 선택 화면이 `SegmentationCandidate` 목록을 상태에 들고 있고, 각 후보가 자기 bounds
  크기의 비트맵을 물고 있다. `SegmentationState.originBitmap`과 **겹쳐 살아 있다.**
  기존에도 `originBitmap`을 명시적으로 해제하지 않아 관례를 따랐으나, 이 라운드가 그 위에
  후보 최대 5개를 얹는다. ⚠️ **관례가 한쪽으로 갈려 있지 않다** — 같은
  `ImageSegmentationRepositoryImpl`은 `subjectBitmap`과 트리밍 비트맵을 `finally`로 명시
  회수한다(하드닝 라운드가 일부러 넣었다). 해제하지 않는 것은 화면이 들고 있는 비트맵 쪽 관례다.
- **항목**: ① ML Kit가 돌려준 비트맵의 소유권이 문서에 없다 — `recycle()`을 부르면 내부에서
  재사용 중일 때 그리기가 깨진다. 소유권을 확인할 방법이 필요하다. ② 필터에서 탈락한 후보도
  같은 이유로 참조만 버리고 회수는 GC에 맡긴다. ③ OQ-P-228(원본 다운샘플 부재)이 열려 있는
  동안 이 압력이 더해진다 — 다운샘플이 들어가면 후보 비트맵도 함께 작아져 이 항목의 무게가
  줄어든다.
- **상태**: 미해결 (**실측 없음** — 후보가 실제로 몇 개, 어떤 크기로 잡히는지 아직 안 봤다.
  OQ-P-267과 같은 실기기 1회로 함께 갈린다)
- **해소 메모**: 면적 필터와 개수 상한이 총량을 누르는 것이 지금의 유일한 방어다.
  > 📌 **브랜치가 아니라 develop 이야기가 됐다(2026-08-24, PR #342 = `34bf1939`)** — 이 스펙이
  > 낳은 미결 넷(OQ-P-266·267·269·277)의 출처는 이제 전부 develop 코드다. 넷 다 실기기 확인
  > 항목이라 머지 자체로 닫히는 것은 없다.

### [2026-08-23] 후보 필터 상수 둘의 근거가 실측이 아니다

- **ID**: OQ-P-267
- **출처**: [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)
  후보 필터 절 — `MIN_SUBJECT_AREA_RATIO`(원본 면적 대비 1%)와 `MAX_SUBJECT_COUNT`(5)를
  실기기 분포를 보지 않고 정했다. ML Kit `SubjectSegmenter`에는 신뢰도 점수 필드도, 개수
  상한 옵션도 없어서 앱이 직접 자를 수밖에 없다.
- **항목**: ① 임계가 너무 높으면 사용자가 고르려던 작은 물체가 후보에서 사라진다(화면에는
  아무 단서도 남지 않는다). ② 너무 낮으면 파편이 점선 박스로 화면을 덮는다. ③ 상한 5가
  실제 분포에서 절단을 일으키는지 — 절단되면 큰 것부터 남으므로 작은 대상이 먼저 잘린다.
- **상태**: 미해결 (**실기기 1회로 갈린다** — 배경이 복잡한 사진 몇 장이면 분포가 보인다)
- **해소 메모**: 값만 고치면 되도록 상수로 두고 순수 함수(`SegmentationCandidateFilter`)로
  덮는다. 재조정은 테스트 기댓값 수정으로 끝난다.

### [2026-08-23] 다중 모드에서 전경 마스크가 채워지는지 확인하지 못했다

- **ID**: OQ-P-268
- **출처**: [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)
  후보 0건 폴백 절 — 아티팩트(`play-services-mlkit-subject-segmentation` 16.0.0-beta1)를 열어
  확인한 것은 옵션 플래그 다섯이 서로 배타적이지 않고 결과 조립도 독립적이라는 것까지다.
  실제로 값을 채우는 것은 Play 서비스가 내려주는 dynamite 모듈이라 아티팩트에 없다.
- **항목**: ① 다중 모드에서 `getForegroundConfidenceMask()`가 non-null인가. ② **후보가 0건인
  사진에서 전경 마스크는 차 있는가** — 두 출력이 같은 head에서 나온다면 폴백은 영영 안 타는 죽은
  코드가 된다. ③ 그래도 옵션을 켜 두는 비용이 남는다 — 정상 경로에서도 원본 해상도
  `FloatBuffer`(픽셀당 4바이트)를 매번 할당하고, 클라이언트가 `float[]`를 버퍼로 복사하는
  구간에서 순간 두 벌이 된다. OQ-P-266의 메모리 계산은 이 몫을 따로 세지 않았다.
- **상태**: **해소됨(2026-08-23, 실기기)** — 답이 ①②③ 어느 갈래도 아니었다. **두 옵션을 함께 켜면
  ML Kit 다이나마이트 모듈이 `SIGSEGV`로 죽는다.** 값이 차는지 안 차는지를 물을 수 없었다.
- **해소 메모**: Galaxy A35(Android 16)에서 세그멘테이션 진입 때마다 앱이 죽었고, 크래시가
  `drishti_gl_runn`(MediaPipe GL 러너)과 Binder `onTransact` 양쪽에서 났다. 스택 프레임이 전부
  `dl-MlkitSubjectSegmentation.optional_*.apk` 안이라 **JVM 예외 핸들러를 타지 않아 로그가 남지
  않았다** — `try/catch`도 Crashlytics의 Java 핸들러도 잡지 못하고 `logcat -b crash`에만 남는다.
  `enableForegroundConfidenceMask()` 한 줄을 빼자 같은 동선에서 크래시가 사라져 원인이 갈렸다.
  공식 문서의 설정 예시 다섯 가지도 foreground 계열과 multi-subject 계열을 **한 번도 함께 쓰지
  않는다**(명시적 금지는 없다).
  처방은 해소 메모가 예비로 적어 둔 쪽이다 — **후보가 0건일 때만 전경 마스크 옵션으로 2차
  세그멘테이션을 돌린다.** ③의 비용 문제도 함께 사라졌다. 정상 경로는 이제 `FloatBuffer`를 아예
  받지 않아 **전보다 가벼워졌다.** ②(후보 0건인 사진에서 전경 마스크가 차는가)는 2차 실행이
  별개 요청이라 성립할 여지가 커졌지만, 그 사진을 아직 못 만나 여전히 미확인이다.

### [2026-08-23] segmenter를 닫은 뒤 ML Kit가 준 비트맵의 수명이 문서에 없다

- **ID**: OQ-P-269
- **출처**: [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)
  — `ImageSegmentationRepositoryImpl#segmentImage`은 `segmenter.use { }`로 process 직후 닫는다.
  지금은 결과에서 뽑은 마스크만 쓰고 그 자리에서 비트맵을 새로 만들어 문제가 없지만, 이 설계는
  **닫힌 뒤에도 ML Kit가 준 `Subject.getBitmap()`을 화면 수명 내내 들고 그린다.**
- **항목**: ① 닫은 뒤 그 비트맵이 유효한가. 비트맵은 AIDL 경계를 넘어오며 수신 프로세스에
  매핑되므로 실무적으로 안전할 공산이 크지만 **문서로 보장된 바가 없다.** ② 위험하다면 선택지는
  둘이다 — 후보를 만들 때 우리 비트맵으로 복사하거나(메모리를 한 번 더 쓴다), segmenter를 화면
  수명 동안 열어 두거나(자원 점유가 길어진다).
- **상태**: 미해결 (**①은 한 번 통과했다** — 아래 참고. 확인 화면까지 다녀오는 동선이 남았다)
- **해소 메모**: 같은 API를 쓰는 프로덕션 사례(Telegram `StickerMakerView`)는 segmenter를 아예
  닫지 않는다. 그것이 우연인지 필요 때문인지가 이 항목의 답이다.
  > ✅ **닫은 뒤에도 그려진다(2026-08-23, Galaxy A35)** — 후보 하이라이트가 뜬다는 것이 곧
  > `segmenter.use { }`를 빠져나온 뒤의 비트맵이 유효하다는 뜻이다. ①의 답이 실무적으로는
  > "유효하다" 쪽이다. 다만 **문서 보장이 없다는 사실은 그대로**이고, 화면 수명 내내 들고 있다가
  > 확인 화면을 다녀와 되돌아온 뒤에도 유효한지는 아직 안 봤다 — 그 동선에서 깨지면 예외가 아니라
  > **빈 자리**로 드러날 공산이 크다. ②(복사할지 segmenter를 열어 둘지)는 손대지 않았다.

### [2026-08-23] 토핑 삭제만 즉시 영구인데, 실패는 화면에 닿지 않는다

- **ID**: OQ-P-270
- **출처**: `feature/groups/canvas/impl` `CanvasBGEditViewModel#handleOnDeleteToppingDialogConfirm`
  (PR #335) — 확인 모달의 "삭제하기"가 곧 `DeleteToppingUseCase` 호출이고, 실패 갈래는
  `viewModelLogger.e` 한 줄이다. 같은 파일에 `CanvasBGEditEffect.ShowError`가 있고 Route가 그것을
  토스트로 받는데(배경 저장 실패가 그 길로 나간다) **삭제만 그 길을 쓰지 않는다.**
  계약상 실패는 셋이다 — 403 `PARFAIT_IMAGE_NOT_OWNED`(남의 배치·그룹 미참여) · 409
  `PARFAIT_ALREADY_CLOSED`(마감된 캔버스) · 404 `PARFAIT_IMAGE_NOT_FOUND`(두 번째 삭제)
  ([api/parfait-image.md](../api/parfait-image.md)). 지금은 셋 다 **모달이 닫히고 아무 일도 안
  일어나는** 하나의 모습으로 접힌다.
- **항목**: ① 삭제 실패를 무엇으로 알릴지 — 셋을 한 문구로 접을지, 409만 갈라 "마감된 캔버스"라고
  말할지. 409는 **같은 서버 코드에 세 번째 처분**을 더한 자리다(C-106 배치는 알린 뒤 남기고, C-301
  배경 저장은 일반 오류로 접고, 삭제는 아무것도 안 한다 — OQ-P-261). ② **같은 화면 안에서 되돌림
  가능성이 갈린 것**을 그대로 둘지 — 삭제는 "그만두기"로 나가도 돌아오지 않는데 이동·크기·회전은
  나가면 사라진다. 화면은 그 차이를 말하지 않는다. ③ 소유 판정이 새면 삭제가 남의 토핑을 향할 수
  있는데(OQ-P-250) 그때 서버가 주는 403이 지금은 무반응이라 **잘못된 게이트가 조용히 덮인다.**
- **상태**: 미해결 (**①③은 브랜치에서 답이 나왔고 develop 머지 대기, ② 잔존**)
  > 🔁 **②의 전제가 절반 바뀌고, ①의 범위가 넓어졌다(2026-08-23, PR #336)** — 확인 버튼이 이동·
  > 크기·회전을 PATCH 하기 시작해 **"나가면 사라진다"가 더는 사실이 아니다.** 대신 갈린 축이
  > **시점**으로 옮겨 갔다: 삭제는 모달 확인 시점에, 나머지 셋은 확인 버튼 시점에 영구가 되고
  > "그만두기"로 나가면 셋은 여전히 사라진다. ①은 삭제만의 문제가 아니게 됐다 — 위치 PATCH 실패도
  > 같은 방식으로 접히는데, **그쪽은 화면이 성공한 것처럼 넘어가기까지 한다**(OQ-P-275).
  > 문구를 정할 때 두 갈래를 함께 봐야 한다.
  > ✅ **①③이 닫혔다(2026-08-28, 브랜치 `feature/canvas-polling` — develop 미머지)** — **셋을 한
  > 문구로 접었다.** 409를 갈라 "마감된 캔버스"라고 말하지 않는다. `CanvasBGEditError`에
  > `TOPPING_DELETE_UNKNOWN`이 생기고 `AppError.Network`만 공용 `NETWORK`로 갈린다. 403도 이제
  > 토스트로 보이므로 ③의 "잘못된 게이트가 조용히 덮인다"도 사라졌다. 같은 라운드에서 **삭제가
  > 성공했을 때만 화면을 닫고**(그전에는 성공해도 안 닫혔다) 진행 중에는 `YGScaffoldV2` 로딩
  > 오버레이가 덮는다 — OQ-P-275와 함께 한 라운드에서 정하라던 메모대로 갔다.
  > ⚠️ **②는 그대로다** — 삭제는 모달 확인 시점, 나머지는 확인 버튼 시점이고 화면은 그 차이를
  > 여전히 말하지 않는다. 오히려 **삭제만 성공 시 화면을 닫게 되면서 시점 차이가 더 눈에 띈다.**
  > 409 처분 갈래는 OQ-P-261이 계속 쥔다(무반응 하나가 일반 오류 토스트로 옮겨 갔다).
- **해소 메모**: ②는 [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)
  as-built 절의 비대칭 서술과 함께 정리한다. ①③의 as-built는 develop 머지 시점에 그 스펙과
  specs README로 옮긴다 — 지금은 [폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  「배경 편집 화면의 진행·실패 표현」에 있다.

### [2026-08-23] 토핑 크기 상한이 근거 없이 사라졌다 — 이제 막는 자리가 앱에도 서버에도 없다

- **ID**: OQ-P-271
- **출처**: `CanvasBGEditViewModel`(PR #335) — 같은 커밋이 `TOPPING_MAX_SCALE = 2.5f`를 지우고
  크기조절 핸들의 `coerceIn(MIN, MAX)`를 `coerceAtLeast(MIN)`으로 바꿨다. 커밋 메시지는 토핑 삭제
  연동과 mock 제거를 말할 뿐 이 변경을 설명하지 않고, 코드에도 근거 주석이 없다. 서버 쪽도
  `scale`에 검증이 없다([api/parfait-image.md](../api/parfait-image.md) 미결). **하한만 남아 축이
  비대칭**이 됐고, 회전과 함께 **상·하한이 없는 축이 둘**이 됐다.
- **항목**: ① 상한을 되살릴지, 없는 것이 결정인지. ② 없는 것이 결정이라면 캔버스 밖으로 얼마든지
  커진 배치가 저장될 때 무엇이 막는지 — 지금은 앱도 서버도 안 막고, 그린 결과는 클리핑으로 잘릴 뿐
  값은 그대로 남는다. ③ 되살린다면 그 값의 근거를 어디서 받을지(2.5도 실측이 아니었다).
- **상태**: 미해결 (**하루 만에 화면 밖 문제가 됐다** — 아래 참고)
  > ⚠️ **"그때까지는 화면 안에서만"이 하루 만에 끝났다(2026-08-23, PR #336)** — 같은 화면의 확인
  > 버튼이 `scale`을 그대로 PATCH 본문에 싣는다. 서버도 범위를 검증하지 않으므로 **무한히 커진
  > 배율이 저장되고 다음 조회에서 그대로 돌아온다.** ②("무엇이 막는지")가 이제 실제 데이터의
  > 문제다.
  > ⚠️ **더 나쁜 것은 이 변경이 회귀 테스트로 굳었다는 점이다** — 같은 PR이
  > `CanvasBGEditViewModelTest`에 "예전 상한 2.5에 걸리지 않고 그대로 커진다"를 단언하는 케이스를
  > 넣었다. 근거 없이 지운 상한을 **되살리려면 이제 그 테스트를 함께 지워야 한다.** ①의 답이
  > "되살린다"라면 그 테스트가 왜 있었는지부터 설명해야 한다.
- **해소 메모**: ②를 앱 책임으로 정하면
  [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) 드리프트 4를
  "두 축 모두 상한 없음"으로 다시 적는다. 회전 쪽 같은 물음은 OQ-P-241 ③이 쥐고 있다.

### [2026-08-23] 캔버스 캡처가 "지금 그려진 것"을 복사한다 — 배경이 늦으면 그대로 담긴다

- **ID**: OQ-P-272
- **출처**: `CanvasMainRoute`(PR #324)가 `RequestCanvasCapture`를 받는 즉시
  `graphicsLayer.toImageBitmap()`을 부른다. 캡처 대상인 `YGCanvas`의 배경은 Coil `AsyncImage`라
  비동기로 온다 — **아직 안 온 상태에서 저장을 누르면 배경 없는 그림이 저장된다.** 기다리거나 다시
  시도하는 코드는 없다. 같은 이유로 결과물의 픽셀 크기가 **기기 화면 폭에 종속**된다(캡처는 화면에
  그려진 크기 그대로다).
- **항목**: ① 배경 로딩 완료를 기다릴지 — 기다린다면 무엇으로 판정할지(`AsyncImage` 상태를 화면이
  들고 있지 않다). ② 저장 이미지의 해상도를 규격으로 정할지, 화면 크기를 그대로 둘지. ③ 저장되는
  그림이 **프레임 없는 배경+토핑**이라는 것이 의도인지 — 정책 소스가 없어 코드가 확정했다.
- **상태**: 미해결 (**실기기 1회로 ①②가 함께 드러난다**)
- **해소 메모**: ①은 저장을 누른 시점에 배경 준비 여부를 상태로 들고 있으면 풀리는데, 그 상태를
  누가 소유할지가 `YGCanvas`(컴포넌트)와 화면 사이에서 갈린다 — **그 갈림은 2026-08-25 PR #351로
  컴포넌트 쪽으로 정해졌다**(배경 미설정을 컴포넌트가 흰 바탕으로 받는다,
  [architecture/design-system](../architecture/design-system.md) 캔버스 절). 다만 그것은 **무엇을
  그릴지**를 컴포넌트가 정한 것이고, **다 그려졌는지**를 아는 상태는 여전히 아무도 안 갖고 있다.
  이 경로는 유닛이 ViewModel 층에서 멈춰 **캡처·권한·`MediaStore` 쓰기가 한 줄도 안 잠겨 있다**는
  것도 함께 본다 →
  [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md).

### [2026-08-23] 전일 캔버스 알림 얼럿이 문자열과 호스트만 있고 띄우는 코드가 없다

- **ID**: OQ-P-273
- **출처**: `CanvasMainScreen`(PR #324) — `YGCanvas.overlayContent`에 `YGToastHost` 아래
  `YGAlertHost`가 병치되고 `alertPolicy` 파라미터가 생겼는데, `show`를 부르는 프로덕션 코드가
  **0건**이다(프리뷰만 정책 객체를 직접 만들어 보여 준다). 문자열 셋
  (`canvas_main_closed_canvas_alert_title`·`_sub`·`_button`)도 같은 라운드에 들어와 **프리뷰에서만**
  쓰인다. 겹침 처리도 코드 주석의 TODO다 — "토스트·얼럿이 같은 타이밍에 겹쳐 뜨지 않게 같이
  처리한다. 지금은 그냥 세로로 쌓아 둘 다 보일 수 있다."
- **항목**: ① 이 얼럿을 무엇이 띄우는지 — 전일 캔버스 마감을 앱이 어떻게 아는가(조회 응답의
  `lastClosedDate`인지, 별도 트리거인지). FCM은 2026-08-22 PR #325로 걷혔으므로 푸시는 후보가
  아니다. ② 마감 알림을 **몇 번** 띄울지(캔버스 재진입마다면 매번 뜬다). ③ 토스트와 겹칠 때의 규칙.
- **상태**: 미해결 (**호스트는 쓰이기 시작했고 이 얼럿만 여전히 아무도 본 적이 없다**)
- **해소 메모**: ①이 정해지기 전에는 문자열 셋과 호스트가 낡을 위험만 쌓는다. 정하면
  [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md) 제외 항목과
  [architecture/design-system](../architecture/design-system.md)의 `overlayContent` 호스트 둘 서술을
  함께 갱신한다.

  > 📌 **호스트에 첫 소비처가 생겼다(2026-09-01, PR #411)** — C-001이 그룹 생성·참여 직후 진입에서
  > **환영 배너**를 `alertPolicy.show`로 띄운다. 즉 "얼럿을 띄우는 코드가 0건"은 더 이상 참이 아니다.
  > **다만 이 항목이 묻던 전일 캔버스 마감 알림은 그대로 미구현**이고, `canvas_main_closed_canvas_alert_*`
  > 문자열 셋과 겹침 처리 TODO 주석도 남았다(①②는 손대지 않았다). ③(토스트와 겹칠 때의 규칙)도
  > **여전히 검증되지 않았다** — 환영 배너가 뜨는 진입에서는 같이 뜰 토스트가 없어 겹침이 발생하지
  > 않는다. 새로 생긴 축은 **한 슬롯을 두 사유가 나눠 쓴다**는 것이다: 얼럿 슬롯은 하나이고 새
  > `show`가 이전 것을 대체하므로, 마감 알림이 붙는 날 두 배너의 우선순위를 정해야 한다.

### [2026-08-23] 갤러리 저장이 API 29를 경계로 위치도 보호도 갈린다

- **ID**: OQ-P-274
- **출처**: `GalleryMediaProvider#insertPendingImage`(PR #324) — `RELATIVE_PATH`
  (`Pictures/Parfait`)와 `IS_PENDING`을 **API 29 이상에서만** 넣는다. `minSdk`가 26이라 26~28
  기기에서는 ① 저장 위치가 앱 전용 하위 폴더가 아니고 ② 쓰다 만 파일이 갤러리에 잠깐 온전한
  것처럼 보이며(그 보호가 `IS_PENDING`이다) ③ `WRITE_EXTERNAL_STORAGE` 권한 다이얼로그가 추가로
  뜬다. 세 갈래 전부 실기기 확인이 0회다.
- **항목**: ① 26~28에서 `Pictures/Parfait`에 넣을지 — 넣으려면 `MediaColumns.DATA`로 경로를 직접
  적어야 하고 그것은 Q부터 무시되는 필드다. ② 그 구간을 그냥 기본 위치로 둘지(그러면 저장 위치가
  기기에 따라 달라진다는 것을 어디에도 안 적은 상태다). ③ `minSdk`를 올릴 계획이 있는지 — 있다면
  이 분기와 권한 선언이 통째로 사라진다.
- **상태**: 미해결
- **해소 메모**: 정하면 [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)
  「권한」·「쓰기」 절과 [architecture/data-layer](../architecture/data-layer.md) 시스템 미디어 항목을
  함께 고친다. ③은 [ADR-0027](../adr/0027-portrait-orientation-lock.md)이 `targetSdk`로 겪는 것과
  같은 부류의 질문이다(OQ-P-264).

### [2026-08-23] 토핑 저장이 실패해도 확인은 성공하고 화면이 넘어간다

- **ID**: OQ-P-275
- **출처**: `CanvasBGEditViewModel#handleOnClickConfirm`·`#updateToppingIfChanged`(PR #336) — 확인
  버튼이 바뀐 토핑을 PATCH 하고 그 실패를 `viewModelLogger.e` 한 줄로 삼킨 뒤 `saveBackground()`를
  이어 태운다. 배경이 성공하면 `CanvasBGEditEffect.ConfirmBackground`가 나가 **화면이 넘어간다.**
  사용자가 보는 것은 완전한 성공이지만, 되돌아간 캔버스 메인은 재조회로 **옛 좌표**를 그린다 —
  옮긴 자리가 조용히 되감긴 것처럼 보인다. 계약상 실패는 셋이다(403 `PARFAIT_IMAGE_NOT_OWNED` ·
  409 `PARFAIT_ALREADY_CLOSED` · 404 `PARFAIT_IMAGE_NOT_FOUND`,
  [api/parfait-image.md](../api/parfait-image.md)).
- **항목**: ① 하나라도 실패하면 화면을 붙잡을지, 넘기되 알릴지. ② **부분 실패**를 무엇으로 보여
  줄지 — 셋 중 하나만 실패한 경우가 이 화면의 기본형이다(토핑들이 병렬로 나간다). ③ 같은 버튼
  안에서 **배경은 토스트 + 잔류, 토핑은 무반응 + 이동**으로 갈린 것을 그대로 둘지. ④ 소유 판정이
  새면(OQ-P-250) 남의 토핑에 PATCH가 나가고 서버 403이 오는데, 지금은 그 403이 무반응이라
  **잘못된 게이트가 또 한 번 조용히 덮인다**(삭제와 같은 구조 — OQ-P-270 ③).
- **상태**: 미해결 (**네 항목 전부 브랜치에서 답이 나왔고 develop 머지 대기** — 남은 것은
  아래 ⚠️ 하나다)
  > ✅ **넷 다 답이 나왔다(2026-08-28, 브랜치 `feature/canvas-polling` — develop 미머지)** —
  > ① **붙잡는다.** 토핑 PATCH와 배경 저장을 모두 시도하되 하나라도 실패하면 `ConfirmBackground`를
  > 쏘지 않는다. ② **부분 실패는 한 문구로 접는다** — 어느 토핑이 실패했는지 화면에 말하지 않고
  > `TOPPING_SAVE_UNKNOWN` 토스트 하나가 나간다. 대신 **실패한 토핑은 `dirtyToppingIds`에 남아**
  > 다시 누른 확인이 그것만 재시도한다(폴링 스펙이 "성공·실패를 가리지 않고 비운다"고 적어 둔
  > 규칙이 이것으로 뒤집혔다). ③ **갈린 처분이 합쳐졌다** — 배경도 토핑도 토스트 + 화면 잔류다.
  > ④ 403이 토스트로 보이므로 잘못된 게이트가 조용히 덮이지 않는다(OQ-P-270 ③과 같은 자리).
  > 진행 중에는 `YGScaffoldV2` 로딩 오버레이가 덮는다.
  > ⚠️ **배경과 토핑이 함께 실패하면 배경 쪽 토스트만 나간다** — 둘을 겹쳐 띄우지 않기로 한
  > 결과다. ②를 "한 문구로 접는다"로 답한 것의 연장이고, 토핑 실패를 알릴 필요가 있다고 판단되면
  > 이 자리부터 다시 연다.
- **해소 메모**: as-built는 develop 머지 시점에
  [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) as-built 절의
  비대칭 서술과 [c301 배경 스펙](../superpowers/specs/archive/2026-08-15-c301-canvas-background-edit.md)
  「실패 표현」으로 옮긴다 — 지금은 [폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  「배경 편집 화면의 진행·실패 표현」에 있다. ③이 맞물린 OQ-P-261 ②(마감 409 판정 공용화)는
  그대로 열려 있다.

### [2026-08-23] 편집 결과 다섯 중 테두리만 혼자 서버로 안 나간다

- **ID**: OQ-P-276
- **출처**: `CanvasBGEditViewModel#updateToppingIfChanged`(PR #336) — 변경 판정이 넷
  (`positionX`·`positionY`·`scale`·`rotationDegrees`)만 비교하고 `borderLayers`·`editedImagePath`는
  대상이 아니다. 요청에도 없다. 테두리 PATCH(`.../images/{parfaitImageId}/border`)는
  `ParfaitImageRemoteDataSource.updateToppingBorder`까지 와 있지만 **소비처가 0건**이고
  ([api/parfait-image.md](../api/parfait-image.md)) Repository 갈래도 안 열렸다. 그래서 테두리만
  다시 편집하고 확인을 누르면 **요청이 한 건도 나가지 않고 결과가 사라진다.** 다른 넷이 저장되기
  시작한 지금, 사용자 기준으로는 **"확인했는데 이것만 사라지는" 하나**가 됐다.
- **항목**: ① 무엇을 보낼지 — 앱은 테두리를 **겹 목록**(`ToppingBorderLayer`)으로 들고 서버는
  단일 3필드(`borderType`·`borderColor`·`borderWidth`)다. 모양이 맞지 않는 자리를 어디서 접을지.
  ② `editedImagePath`(테두리를 구워 넣은 새 이미지)를 새로 업로드해 배치를 갈아 끼워야 하는지 —
  [ADR-0025](../adr/0025-topping-border-as-server-field.md)가 굽기를 멈추고 테두리를 서버 필드로
  옮겼으므로 원칙적으로는 필드만 보내면 되는데, 그 결정 이후 이 화면이 실제로 무엇을 들고 오는지가
  확인된 적이 없다. ③ 보내지 않기로 한다면 이 화면의 편집 버튼을 왜 여는지.
- **상태**: 부분 해소 (**①③ 해소 — 2026-08-27, PR #369 develop 머지. ② 잔존**)
  > ✅ **①이 닫혔다 — 접는 자리는 `CanvasBGEditViewModel.toToppingBorder`다.** 겹 목록의
  > **마지막 겹**(가장 바깥)만 `ToppingBorder.Solid`로 보내고, 비면 `ToppingBorder.None`을 보낸다.
  > 되읽는 방향(`toBorderLayers`)은 반대로 한 겹짜리 목록으로 편다.
  > ✅ **③은 질문 자체가 소멸했다** — 편집 버튼이 여는 것이 실제로 서버까지 간다.
  > `updateToppingIfChanged`가 하나였던 변경 판정을 **둘로 갈라** 위치·배율·각도(`update`)와
  > 테두리(`updateBorder`)를 독립적으로 비교하고 독립적으로 보낸다. 서버 API가 두 엔드포인트로
  > 갈라져 있는 것을 그대로 미러링한 결과다. 이로써 [api/parfait-image.md](../api/parfait-image.md)의
  > `android_status`가 **`done`**이 됐다(4/4 소비).
  > ⚠️ **②는 그대로다** — `editedImagePath`(테두리를 구워 넣은 새 이미지)와 `cutoutImagePath`는
  > `handleOnToppingEditResult`가 상태에 적어 두기만 하고 어디로도 안 나간다.
  > [ADR-0025](../adr/0025-topping-border-as-server-field.md)가 굽기를 멈춘 뒤 이 화면이 실제로
  > 무엇을 들고 오는지는 여전히 확인된 적이 없다.
  > ⚠️ **①의 답이 표시 쪽과 어긋난다** — 같은 화면이 그릴 때는 **첫 겹**을 쓴다(OQ-P-254를 닫은
  > PR #388). 겹이 둘 이상이면 보이는 테두리와 저장되는 테두리가 갈린다 → OQ-P-324.
- **해소 메모**: 남은 것은 ② 하나다. `editedImagePath`를 새로 업로드해 배치를 갈아 끼울지 정하면
  [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) as-built 재정정
  절과 [api/parfait-image.md](../api/parfait-image.md) Android 매핑을 함께 고친다.

### [2026-08-23] 후보를 다시 고르면 초안에 적힌 편집 결과가 조용히 덮인다

- **ID**: OQ-P-277
- **출처**: [c103-multi-subject-selection 스펙](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)
  선택 시점 절 × PR2 최종 리뷰(2026-08-23) — 저장과 초안 기록이 화면 진입에서 **탭 시점으로**
  옮겨 오면서 생긴 새 동작이다. 이전에는 재탭이 이동만 했다.
- **항목**: ① 확인·편집 화면에서 테두리를 두른 뒤 뒤로 와 **같은 후보를 다시 탭**하면
  `SegmentationViewModel#selectCandidate`의 `record(borderColorArgb = null, borderWidthDp = null)`가
  초안을 새 경로로 갈아 끼운다. 사용자가 두른 테두리가 말없이 사라진다. ② 그것이 의도인지
  판단이 필요하다 — "다시 고르면 처음부터"가 자연스러울 수도 있고, 같은 후보를 다시 고른
  경우만 갈라 초안을 건드리지 않는 선택지도 있다. ③ **다른 후보**를 고르는 경우는 알맹이가
  바뀌므로 테두리를 지우는 것이 맞다(`ToppingDraftRepository.record` KDoc이 그 규칙을 적어 두었다).
- **상태**: 미해결 (스펙이 "선택 취소·다시 고르기 동선"을 범위 밖으로 두었으므로 이번 라운드의
  결함으로 세지 않는다. **실기기 확인 때 함께 본다**)
- **해소 메모**: ①이 문제로 판명되면 같은 후보 재선택을 걸러 내는 것이 가장 작은 처방이다.

### [2026-08-23] 짧은 이미지를 512로 확대하면 세그멘테이션이 실제로 나아지는가

- **ID**: OQ-P-278
- **출처**: [segmentation-preprocessing 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)
  근거 등급 표 — ML Kit Android 가이드 "Tips to improve performance" 절이 "For ML Kit to get an
  accurate segmentation result, the image should be at least 512x512 pixels."라고 적는다.
- **항목**: ① 그 문장은 **하한 미만이면 정확하지 않다**는 말이고, **확대하면 회복된다**는 말이
  아니다. 확대는 정보를 늘리지 않으므로 모델이 내부에서 어차피 같은 처리를 한다면 이득이 0일 수
  있다. ② 모델의 실제 입력 해상도와 내부 다운스케일 여부는 공개 문서에 없어 추론으로 메울 수 없다.
  ③ 그래서 이 항목은 스펙에서 **조건부 구현**으로 표시했다 — 사진 세트에서 차이가 안 보이면 넣지 않는다.
- **상태**: 미해결 (구현 전 판정 대상. 짧은 변 512 미만 사진 1장으로 갈린다)

  📌 **재시도 경로에 한해 확대가 들어왔다(2026-09-10, PR #487)**. 후보 0건 뒤 재시도 사다리가 검출 입력을
  짧은 변 512로 맞춘다(긴 변 2048 상한과 충돌하면 상한이 이긴다). `decodeImage` 전역 확대는 여전히 없고,
  **이 질문의 답도 아직 없다.** 판정 수단이 사진 세트에서 단계 로그로 바뀌었는데, 그 로그가 logcat 밖으로
  나가지 않는다(OQ-P-399).
- **해소 메모**: 효과가 없으면 스펙의 해당 절과 `computeUpscaleTarget`을 함께 걷는다. 반대로
  효과가 크면 하한을 512보다 올릴 여지도 같은 사진으로 본다.

### [2026-08-23] PNG 전환이 온디스크 누적을 키우는데 지우는 코드가 없는 자리가 둘이다

- **ID**: OQ-P-279
- **출처**: [segmentation-preprocessing 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)
  에러 처리 절 × `FileCameraCacheLocalDataSourceImpl` × `RecentImageRepositoryImpl` —
  세그멘테이션 캐시는
  [pipeline-hardening 라운드](../superpowers/specs/archive/2026-08-18-segmentation-pipeline-hardening.md)가
  전용 디렉토리 + 진입 시 통째 비우기로 정리했으나 **나머지 둘은 그 라운드가 안 건드렸다.**
- **항목**: ① **카메라 캐시**(`cacheDir/camera`)를 지우는 코드가 없고 파일명이 초 단위라 충돌도
  남는다. 촬영 결과가 PNG가 되면 더 큰 파일이 그 자리에 쌓인다. ② **최근 이미지**
  (`filesDir/recent_images`, 상한 9장)는 `cacheDir`가 아니라 `filesDir`이라 **OS가 저장 공간
  압박에서 회수하지도 않는다.** `SegmentationViewModel`이 진입마다 원본을 `SOURCE`로 복사하므로
  PNG 촬영본이 그대로 아홉 칸을 채운다. ③ 실제 증가 폭을 재지 않았다 — 스펙의 사진 세트가 함께 잰다.
  ④ 최근 이미지 저장이 파일 전체를 `ByteArray`로 읽는 것도 같은 자리에서 커진다.
- **상태**: 미해결 (기존 결함이나 PNG를 채택하면 부담이 커진다. 크기 측정이 선행)
- **해소 메모**: 스펙이 PNG를 조건부로 내렸으므로 이 항목의 급함도 그 판정에 달렸다. ①은
  세그멘테이션 캐시와 같은 처방(전용 디렉토리 + 진입 시 정리 + `File.createTempFile`)을 쓸 수
  있는지 보면 되고, ②는 상한을 장수가 아니라 바이트로 두는 편이 맞을 수 있다.

### [2026-08-23] ImageDecoder가 EXIF orientation을 자동 적용하는지 문서로 확인하지 못했다

- **ID**: OQ-P-280
- **출처**: [segmentation-preprocessing 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)
  설계 3절 × `ContentResolver.kt#decodeUriToBitmap` — API 28 이상은 `ImageDecoder`, 미만은
  `MediaStore.Images.Media.getBitmap`을 탄다. 후자가 EXIF를 적용하지 않는다는 것은 널리 알려져
  있고 `minSdk`가 26이라 그 갈래는 살아 있다. **전자가 적용한다는 문장은 공식 문서에서 찾지 못했다.**
- **항목**: ① 확인 못 한 것을 전제로 삼으면 `InputImage.fromBitmap(bitmap, 0)`의 회전 0 단정이
  API 28 이상에서 참인지 알 수 없다. ② 보정 범위가 갈린다 — API 28 미만만 보정할지, 버전 분기 없이
  항상 보정할지. ③ 항상 보정하는 쪽은 이미 정립된 이미지를 또 돌릴 위험이 있어 EXIF 값을 그대로
  믿어야 한다.
- **상태**: 미해결 (실기기·에뮬레이터 확인으로 갈린다. 스펙이 사진 세트에 그 사진을 넣어 두었다)

  🔁 **2026-08-25 갱신(PR #349 develop 머지)** — **기본값 쪽이 실제 코드가 됐다.** API 28 미만
  갈래에만 `ContentResolver.kt#rotatedToUpright`가 붙었고 `ImageDecoder` 갈래는 그대로다.
  질문 자체는 그대로 열려 있다 — 판정은 여전히 사진 세트가 한다. 확인 뒤 "무조건 보정"으로
  넓히는 자리는 [전처리 계획](../superpowers/plans/2026-08-23-segmentation-preprocessing.md) Task 8이다.
- **해소 메모**: EXIF 회전이 붙은 세로 사진 1장을 두 API 대역에서 각각 통과시키면 답이 나온다.
  API 28 이상에서 이미 정립돼 나오면 분기 보정, 아니면 무조건 보정이다. **스펙은 판정 전까지
  API 28 이상을 보정하지 않는 쪽을 기본값으로 둔다** — 확인 못 한 것을 근거로 이중 회전 위험이
  있는 쪽으로 기울지 않는다.

### [2026-08-23] decodeImage 가 색공간·Bitmap.Config·HEIC 를 다루지 않는데 계약은 "정규화"라고 말한다

- **ID**: OQ-P-281
- **출처**: [segmentation-preprocessing 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)
  범위 제외 절 × `ContentResolver.kt#decodeUriToBitmap` ×
  `ImageSegmentationRepositoryImpl#segmentImage` — 스펙이 `decodeImage`의 계약을 "세그멘테이션
  입력 규격으로 정규화"로 넓히면서 내용은 회전과 해상도 하한 둘만 담았다.
- **항목**: ① **`Bitmap.Config`.** `InputImage.fromBitmap`이 요구하는 것은 ARGB_8888인데
  `ImageDecoder`는 소스에 따라 `RGBA_F16`(10-bit HEIC·AVIF)을 낼 수 있다. 정상 경로에 가드가 없다.
  폴백 경로만 `Bitmap.createBitmap(pixels, …, ARGB_8888)`이라 우연히 보증된다.
  ② **색공간.** `ImageDecoder`는 Display P3 같은 소스 색공간을 보존한다. 모델이 sRGB 전제라면 같은
  피사체가 색공간에 따라 다른 신뢰도를 받는다. 이것은 대비 조정이 아니라 **형식 정합**이라
  "대비 정규화는 근거가 없어 제외"와 같은 처분을 받을 항목이 아니다.
  ③ **HEIC.** 최근 기기 갤러리 기본 포맷인데 `MediaStore.Images.Media.getBitmap`(API 26·27 갈래)이
  열지 못한다. 이 스펙이 손대는 바로 그 갈래다.
- **상태**: 미해결 (스펙이 명시적으로 범위 밖에 두었다. 검증 수단이 사진 세트와 달라 분리했다)
- **해소 메모**: 가장 작은 처방은 정규화 첫 단계에 `config != ARGB_8888`이면 `copy(ARGB_8888, true)`를
  넣는 것이다. ③은 API 26·27 갈래를 `ImageDecoder` 이전 세대의 다른 API로 바꾸는 것이 근본 처방이나
  그쪽은 회전 처리와 함께 봐야 한다.

### [2026-08-23] 배치 화면의 초기 토핑 크기가 알맹이의 절대 픽셀에서 나와 서버 scale 로 굳는다

- **ID**: OQ-P-282
- **출처**: [segmentation-preprocessing 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)
  설계 2절 × `ToppingHandleComponents.kt#rememberToppingBaseSize` × `ToppingPlacement.kt` —
  `rememberToppingBaseSize`가 알맹이 PNG의 인트린식 픽셀 치수를 그대로 dp로 환산하고, 그 값이
  배치 화면의 초기 크기이자 `toToppingTransform`이 계산하는 `scale`의 분자가 된다. 그 `scale`이
  서버에 저장된다([c106-topping-place-api](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md)).
- **항목**: ① 세그멘테이션 입력을 확대하면 알맹이의 절대 픽셀이 커지고, 사용자가 배치 화면에서
  손대지 않아도 **같은 사진에서 만든 토핑이 전보다 크게 캔버스에 박힌다.** 크래시가 아니라 무증상
  회귀다. ② 확대와 무관하게도, 토핑의 캔버스 내 크기가 소스 이미지 해상도에 좌우되는 것이
  의도인지 확인이 필요하다. 저해상도 사진에서 만든 토핑이 항상 작게 들어간다는 뜻이다.
  ③ 처방 후보는 배치 기본 크기를 픽셀이 아니라 정규화 기준으로 옮기는 것이다.
- **상태**: 미해결 (②는 확대를 안 넣어도 이미 참인 동작이다. ①은 확대 채택 시 새로 생긴다)

  📌 **재시도 경로는 ①을 피한다(2026-09-10, PR #487)**. 확대한 판은 검출에만 쓰고 후보 픽셀은 원본에서
  오려내므로 알맹이의 절대 픽셀이 커지지 않는다. 전역 확대가 들어오면 ①은 그대로 생긴다. ②와는 무관하다.
- **해소 메모**: 스펙의 사진 세트가 "짧은 변 512 미만 사진으로 배치까지 끝내 전후 비교"를 넣어
  ①을 판정한다. ②는 그 결과와 무관하게 정책 확인이 필요한 별개 질문이다.

### [2026-08-23] 확장자 판정을 고쳐도 이미 .jpg 로 앉은 PNG 는 그대로 남는다

- **ID**: OQ-P-283
- **출처**: [segmentation-preprocessing 계획](../superpowers/plans/2026-08-23-segmentation-preprocessing.md) Task 4 ×
  `FileRecentImageLocalDataSourceImpl#getTargetFile` × `RecentImageRepositoryImpl` — 최근 이미지의
  파일명이 `sha256 + "." + extension` 이라 **확장자가 바뀌면 같은 바이트가 다른 파일명**이 된다.
  그리고 목록의 중복 판정 키는 파일명이 아니라 uri 다.
- **항목**: ① Task 4 이전에 저장된 PNG 원본은 `<sha>.jpg` 로 앉아 있고, 같은 사진을 다시 고르면
  `<sha>.png` 가 새로 만들어져 **최근 목록에서 두 칸을 먹는다.** 상한이 9칸인데 토핑 흐름 하나가
  이미 두 칸씩 쓴다(OQ-P-258). ② 옛 `.jpg` 항목을 배경으로 고르면 **Task 4 가 고치려던 결함이
  그대로 재현된다** — `ImageFileLocalDataSourceImpl#formatOf` 가 확장자에서 유도된 MIME 을 바이트
  스니핑보다 먼저 믿는다. ③ 마이그레이션(기존 파일 재판정·개명)을 할지, 상한에 밀려 사라지기를
  기다릴지.
- **상태**: 미해결 (계획이 **마이그레이션 안 함**으로 감수했다. 목록 상한이 9칸이라 곧 밀려난다는 판단)

  ⚠️ **2026-08-25 갱신(PR #349 develop 머지)** — **감수가 가정에서 사실이 됐다.** Task 4가
  `RecentImageRepositoryImpl#extensionOf`로 들어가면서 이 항목이 말하는 두 이름 공존이 지금
  develop에서 실제로 일어난다. PNG 채택 여부와 무관하게 이미 참이므로, 이 항목의 급함은
  더 이상 PNG 판정에 매여 있지 않다.
- **해소 메모**: ③을 하기로 하면 저장 디렉토리를 훑어 바이트로 재판정하고 개명하는 1회성 경로가
  필요하고, 목록의 uri 도 함께 갱신해야 한다. 그보다 싼 처방은 중복 판정 키를 uri 가 아니라
  **바이트 해시**로 옮기는 것이다 — 그러면 확장자가 달라도 같은 사진으로 본다. ①과 ②가 함께 닫힌다.

### [2026-08-24] 계약 문서의 서술이 운영에서 언제부터 참인지 확정되지 않는다

- **ID**: OQ-P-284
- **출처**: 서버 `#110`(`a404ac2`) × `bootstrap/src/main/resources/application.yaml` ×
  `V16__reconcile_ddl_auto_schema_drift.sql` × 서버 `docs/operations/flyway-cutover.md` —
  Flyway가 꺼져 있던 기간에 `ddl-auto: update`가 운영 스키마를 대신 관리했고, `update`는 추가만 하고
  삭제하지 않으므로 DROP·제약·기본값·데이터 이관을 담은 마이그레이션이 반영되지 않았다.
  이 라운드가 `ddl-auto: validate` + V16으로 소유권을 Flyway에 되돌렸다
  ([api/conventions.md 스키마 소유권](../api/conventions.md#스키마-소유권--코드가-정본이어도-운영-응답은-다를-수-있다)).
- **항목**: ① **전환이 배포만으로 끝나지 않는다** — 운영 히스토리가 V1~V4뿐이라 Flyway가 V5부터
  재실행하려 하고 `ddl-auto`가 만들어 둔 컬럼을 다시 ADD 하다 죽는다. V5~V15를 적용된 것으로 기록하는
  baseline SQL을 **사람이 1회성으로** 실행해야 한다. ② 그 절차가 언제 돌았는지(혹은 돌았는지) 앱 쪽에서
  알 방법이 없다 — 계약 문서가 "서버가 이렇게 한다"고 적어도 **그 시점 이전 운영에서는 거짓**일 수 있다.
  실제로 최소 둘이 그랬다([parfait-group.md](../api/parfait-group.md) 닉네임 중복 허용 ·
  [parfait-image.md](../api/parfait-image.md) 토핑 배치 500). ③ 전환 전에 앱이 붙어 실패를 만나면
  원인이 **계약이 아니라 운영 스키마**인데, 앱 쪽 증상(500)은 그 둘을 구분하지 못한다.
- **상태**: 미해결 (서버 소관. 앱은 baseline 실행 완료 여부를 확인받아야 한다)
- **해소 메모**: 서버팀에 baseline 절차 실행 시점을 확인해 기록하면 ①②가 닫힌다. ③은 앱이 할 일이
  없다 — `validate` 전환 뒤로는 스키마가 어긋나면 **서버가 기동 자체를 못 하므로** 500이 아니라 연결
  실패로 드러난다.

### [2026-08-24] 이 감사가 코드만 보고 운영 스키마를 안 본다

- **ID**: OQ-P-285
- **출처**: [server-baseline.md](../api/server-baseline.md) 점검 절차 × 서버 `#110` —
  절차가 대조하는 것은 컨트롤러·DTO·에러 코드 enum·`SecurityConfig`·`ApiResponse`·
  `GlobalExceptionHandler`, 전부 **코드**다. 마이그레이션 SQL과 운영 스키마 상태는 대조 대상이 아니다.
- **항목**: ① 2026-08-15 라운드는 "그룹 내 닉네임 중복이 허용된다"를 코드 근거로 정확히 적었는데
  **운영에는 유니크 인덱스가 남아 500이었다.** 아홉 라운드 동안 이 감사가 그것을 못 잡았다.
  ② OQ-P-180("계약 문서에 서버 초대코드 자릿수를 안 적어 대조로도 못 잡았다")과 같은 계열이지만
  이쪽은 **문서에 적어도 못 잡는다** — 근거 축 자체가 없었다. ③ 마이그레이션 파일을 감사 대상에
  넣을지, 넣는다면 어디까지인지(전체 스키마 미러 vs 계약에 닿는 제약만).
- **상태**: 부분 해소 (①② 절차에 축 추가, ③ 범위는 최소선으로 잡았고 실효는 다음 라운드에 드러난다)
- **해소 메모**: `validate` 전환이 이 결함의 재발을 대부분 막는다 — 코드와 운영이 어긋나면 서버가
  기동에 실패하므로 **드리프트가 조용히 살아 있을 수 없다.** 그래서 처방은 스키마 전체를 미러하는
  것이 아니라, delta에 `resources/db/migration/` 변경이 있으면 **그것이 계약 서술을 뒤집는지만**
  보는 것이다. 그 한 줄을 [server-baseline.md](../api/server-baseline.md) 점검 절차 3에 넣었다.
  남은 것은 그 최소선이 충분한지다 — 제약·기본값·컬럼 삭제 밖의 변경(인덱스·타입 확장)이 계약을
  건드리는 사례가 나오면 범위를 다시 본다.

### [2026-08-24] 누끼 마스크에 내부 구멍이 실물에서 얼마나 생기는지 모른다

- **ID**: OQ-P-286
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  근거 등급 표 — 이진 컷이 내부 구멍을 남기는 것은 원리상 맞으나 관찰된 사례가 없다.
- **항목**: ① 구멍 메우기(테두리 배경 flood fill)를 넣을 값어치가 있는가. ② 축소 판정이라
  블록 폭 미만 구멍은 애초에 안 보이는데, 실물 구멍이 그보다 큰가. ③ 초판이 실루엣을 덮어 칠하지
  않으려 넣은 filled-mask 침식이 **작은 구멍을 아예 못 메우고 중간 구멍에는 반투명 링을 남겼다.**
  그 링은 다음 단계 침식의 표적이라 구멍이 오히려 커진다. ④ 반대로 **큰 구멍은 그대로 메워진다** —
  손을 허리에 얹은 사람의 팔과 몸통 사이처럼 테두리에 닿지 않는 정당한 배경까지 칠해진다.
  침식이 유용한 쪽만 죽이고 해로운 쪽은 못 막는다.
- **상태**: 미해결 (**이번 라운드에서 제외**. 철회 조건을 사진 세트 판정 전에 선행 적용했다)
- **해소 메모**: 실물에서 구멍이 눈에 보이면 다시 연다. 그때는 침식 대신 **축소판으로 구멍 위치만
  찾고 각 구멍 bbox 안에서 원본 해상도 flood를 다시 돌려 확정**하는 형태여야 ③이 안 생긴다.
  ④는 별개 가드가 필요하다 — 구멍 면적이 감싸는 성분 면적의 일정 비율을 넘으면 메우지 않는다.
  제외 결정으로 filled-mask와 적용 규칙의 세 값이 함께 사라져 커널이 단순해졌다.

### [2026-08-24] 폴백 알파 램프가 만드는 부분 알파 띠의 실제 폭을 모른다

- **ID**: OQ-P-287
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「폴백 경로 배선」 — 신뢰도 0.35~0.65를 알파 0~255로 사상하면 경계에 띠가 생기는데, 그 폭은
  신뢰도 기울기에 달렸고 측정한 적이 없다.
- **항목**: ① 띠가 1픽셀 이하면 램프가 하드컷과 사실상 같아 값어치가 없다. ② 띠가 넓으면
  `ToppingEditMask#trimTransparentBounds`가 그 띠까지 포함해 잘라 편집·저장 경로의 판이 종전보다
  커진다. ③ keep-mask 한 블록 팽창이 띠를 살리는데, 팽창 폭(원본 4픽셀)이 띠보다 좁으면 여전히
  잘린다.
- **상태**: 미해결 (사진 세트에서 띠 폭을 잰다)
- **해소 메모**: 1픽셀 이하로 나오면 램프를 빼고 하드컷으로 되돌린다. 넓으면 램프 구간을
  0.4~0.6으로 좁히거나 팽창 폭을 맞춘다.

### [2026-08-24] 알파 침식은 흰 테의 원인인 RGB 오염을 못 고친다

- **ID**: OQ-P-288
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  근거 등급 표 경고 — 원 제안의 "1픽셀 erode로 색 오염 제거"가 실제로는 알파만 건드린다.
- **항목**: ① 흰 테의 정체는 알파가 낮은 픽셀이 배경색에 오염된 RGB를 갖고 있는 것이라, 바깥 한
  겹을 지워도 안쪽 부분 알파 픽셀의 색은 그대로다. ② 진짜 처방은 색 디컨태미네이션(부분 알파
  픽셀의 RGB를 이웃 불투명 픽셀 색으로 대체)인데 이번 라운드에 없다. ③ Android 비트맵이
  premultiplied라 `getPixels` 왕복에서 저알파 픽셀의 색 정밀도가 떨어지는데, 램프가 저알파 픽셀을
  늘리므로 그 손실이 눈에 보일 수 있다.
- **상태**: 미해결 (이번 라운드는 목표를 "경계 한 겹 침식"으로 줄여 적었다)
- **해소 메모**: 사진 세트의 "밝은 배경의 밝은 물체"에서 알파 침식만으로 흰 테가 사라지는지 본다.
  안 사라지면 색 디컨태미네이션을 다음 라운드로 올린다.
  ⚠️ **판정 조건이 초판에서 바뀌었다.** 초판은 침식 대상을 "알파 1~254"로 잡았는데, 그러면 흰 테가
  가장 심한 조건(밝은 배경 + 하드 매트)에서 단계가 통째로 no-op이라 이 미결을 판정할 수 없었다.
  능선 보호 조건으로 바꿔 하드 매트 경계도 침식되므로 이제 판정이 성립한다. 판정 근거는 관측
  3번(부분 알파 픽셀 비율)이다 — 비율이 0에 가까운데 흰 테가 남으면 원인은 RGB 오염이 맞다.

### [2026-08-24] 사각형 IoU가 교차하는 얇은 피사체를 같은 후보로 오판한다

- **ID**: OQ-P-289
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「필터 판정」 — `SegmentationCandidateFilter#filterCandidates`의 중복 판정을 사각형 IoU로 바꾼다.
- **항목**: ① 서로 교차하는 대각선 가닥 두 개는 bounds가 같아 IoU가 1이지만 실제 마스크 교집합은
  거의 없다. 별개 피사체가 병합되어 사라진다. ② 마스크 IoU로 바꾸면 해결되고 비용도 작으나(두
  후보의 bounds 교집합 영역만 훑으면 된다) 코드가 늘어난다. ③ 임계 0.9가 "거의 같은 박스"만 잡는
  보수적 값이라, ML Kit이 실제로 내놓는 중복이 그 위에 있는지 아래에 있는지 모른다.
- **상태**: 미해결 (사각형 IoU 0.9로 시작한다)
- **해소 메모**: 사진 세트에서 ①이 보이거나 중복이 안 잡히면 마스크 IoU로 승격한다.

### [2026-08-24] bounds 축소 후 얇은 피사체의 탭 타깃이 너무 작아진다

- **ID**: OQ-P-290
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「화면 쪽 파급」 × `SegmentationHighlightGeometry#pickCandidateIndex` —
  후처리가 bounds를 실제 객체에 붙이면 얇은 피사체의 탭 사각형이 몇 dp가 된다.
- **항목**: ① 판정용 사각형만 최소 크기로 넓히고 그리기는 tight로 두는 방법이 있다. ② 그러면
  겹친 후보의 우선순위가 흔들린다 — `pickCandidateIndex`가 bbox 면적 최소로 승자를 고르는데,
  넓힌 사각형끼리 겹치면 어느 쪽이 이길지 규칙이 새로 필요하다. ③ 승자 선택 기준을 면적에서
  커버리지로 옮길지도 함께 봐야 한다.
- **상태**: 미해결 (이번 라운드 범위 밖)
- **해소 메모**: 후처리를 넣은 뒤 실기기에서 얇은 피사체를 실제로 못 누르는지 먼저 확인한다.
  증상이 없으면 열어 두지 않는다.

### [2026-08-24] 후보 선택 화면에 semantics가 없어 스크린리더로 후보를 고를 수 없다

- **ID**: OQ-P-291
- **출처**: `SegmentationSubjectHighlight` × `SegmentationScreen` — `Canvas`와
  `detectTapGestures`만 쓰고 semantics가 전혀 없다. 이미지도 `contentDescription`이 null이다.
- **항목**: ① 스크린리더 사용자는 후보를 인지할 수도 고를 수도 없다. ② bounds 축소와 무관한
  **기존 결함**이라 마스크 후처리 라운드에서 고치지 않았다. ③ 후보마다 semantics 노드를 얹으려면
  현재의 단일 `Canvas` 구조를 바꿔야 하는지, `semantics { }` 블록으로 충분한지 확인이 필요하다.
- **상태**: 미해결 (기존 결함. 안 넣기로 한 기록)
- **해소 메모**: C-103 후보 선택 UI를 다시 손대는 라운드에 함께 본다.

### [2026-08-24] 후보 커버리지 임계의 근거가 측정이 아니다

- **ID**: OQ-P-292
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「필터 판정」 × `SegmentationCandidateFilter.kt#filterCandidates` — 면적 판정을 bounds 사각형에서
  커버리지(알파 총합 ÷ 255)로 바꾸면서 임계를 새로 정했다.
- **항목**: ① 초판은 "채움비가 대략 절반이니 종전의 절반"이라는 계산으로 값을 정했다가 철회했다.
  실제 채움비는 솔리드 제품과 비스듬히 놓인 가늘고 긴 물체 사이에서 자릿수로 벌어진다.
  ② **그 자리를 채운 값도 측정이 아니라 같은 종류의 추정이다.** ③ 커버리지는 채움비를 곱한 값이라
  같은 임계가 **가늘고 긴 물체에는 엄격해지고 솔리드 물체에는 느슨해진다.** 지금 값은 얇은 정당
  피사체를 살리는 쪽으로 골랐고, 그 대가로 작은 솔리드 물체가 종전보다 훨씬 많이 후보로 올라온다.
  ④ OQ-P-267이 "후보 필터 상수의 근거가 실측이 아니다"로 이미 열려 있고 이 라운드가 그 상수를
  갈아치운다 — 같은 계열이다.
- **상태**: 미해결 (값을 계약으로 박되 근거 등급은 조건부로 두었다)
- **해소 메모**: 사진 세트에서 두 방향을 각각 본다 — 비스듬히 놓인 가늘고 긴 물체가 후보로 남는가,
  그리고 잡스러운 작은 솔리드 물체가 상한을 잡아먹지 않는가. `MAX_SUBJECT_COUNT` 상한이 후자를
  받아 내므로 실질 위험은 전자 쪽이다. 관측 1번(후처리 전후 후보 수)이 판정 자료가 된다.

### [2026-08-24] 관측 로그가 릴리즈 빌드에서 남는지 확인이 안 됐다

- **ID**: OQ-P-293
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「관측」 × `Logger.kt#repositoryLogger` × [ADR-0017](../adr/0017-remote-network-datasource.md)
- **항목**: ① 이 라운드가 심는 로그 세 줄의 목적은 **필드에서 인식 실패의 원인이 ML Kit인지 우리
  후처리인지 가르는 것**이다. 디버그 빌드에서만 남으면 목적을 달성하지 못한다. ② 저장소에 빌드
  타입별 로거 게이팅 관행이 있는지, 있다면 이 세 줄을 예외로 둘지 판단이 필요하다. ③ 예외로 둔다면
  개인정보가 섞이지 않는지도 함께 봐야 한다(후보 수·픽셀 비율뿐이라 문제없어 보이나 확인 대상이다).
- **상태**: 미해결 (구현 전 확인)
- **해소 메모**: `Loggers.create` 구현과 릴리즈 빌드 설정을 확인하면 ①②가 닫힌다. 안 남는다면
  이 라운드의 관측 항목은 사진 세트 검증용으로만 쓰이고 필드 진단에는 못 쓴다는 사실을 스펙에
  적어야 한다.

### [2026-08-24] 판정 버퍼 축소를 적용하지 않는 크기 하한을 실측으로 못 정했다

- **ID**: OQ-P-294
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「처리 해상도」 × 앞 라운드 [segmentation-preprocessing](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)
  「해상도 하한」(OQ-P-278)
- **항목**: ① 축소의 근거는 런 개수 폭증과 그로 인한 OOM인데, **작은 판에서는 그 위험이 없고 판정
  해상도만 잃는다.** 즉 작은 이미지에서 축소는 순손실이다. ② 그래서 크기 하한을 두었으나 그 값은
  추정이고, 런 개수가 실제로 위험해지는 지점을 재 본 적이 없다. ③ 앞 라운드가 조건부로 두고 있는
  "짧은 변 512 확대"가 채택되면 긴 변이 짧은 사진에서 판정 버퍼가 세 자릿수로 내려간다. 하한이
  그 조합을 자동으로 막지만, **두 라운드의 상수가 서로를 전제하는 관계**가 되므로 어느 한쪽이
  바뀌면 다른 쪽을 함께 봐야 한다.
- **상태**: 미해결 (하한값을 계약으로 박되 근거 등급은 조건부로 두었다)
- **해소 메모**: 사진 세트의 "짧은 변 512 미만"과 "잡티가 많은 텍스처 배경"이 각각 ②③을 판정한다.
  후자에서 런 개수를 한 번 재면 하한값의 근거가 추정에서 실측으로 바뀐다.

### [2026-08-24] area opening 은 크고 떨어진 덩어리 둘을 못 가른다

- **ID**: OQ-P-295
- **출처**: [segmentation-mask-postprocessing.md](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)
  「범위 - 제외」 — 앞 라운드가 후처리로 미룬 "최대 연결 요소만 남기기"를 채택하지 않았다.
- **항목**: ① area opening 은 임계 **미만** 성분만 버리고 임계 이상은 모두 남긴다. 그래서 한 후보
  안에 크고 떨어진 덩어리가 둘 있으면 **bounds 가 여전히 둘을 함께 감싼다.** 이 라운드가 고치려는
  증상("구석 한 점이 bounds 를 넓힌다")의 큰 판본이 그대로 남는 셈이다. ② 최대 연결 요소만 남기면
  그것이 닫히지만 **분리된 가는 구조와 정당한 다중 성분**(사람 둘, 사람과 든 물건)을 잃는다.
  이 라운드는 후자를 택했다. ③ **OR 풀링에 따르는 별개 한계도 있다** — 실루엣에서 배율의 두 배
  이내에 있는 잡티는 본체와 같은 성분으로 묶여 구조적으로 제거할 수 없다.
- **상태**: 미해결 (교환을 의식적으로 택했고 잔여를 추적한다)
- **해소 메모**: 사진 세트에서 ①이 실제로 보이면 "가장 큰 성분 대비 일정 비율 미만인 성분을
  버린다"처럼 절대 임계가 아니라 상대 임계를 쓰는 절충을 본다. 최대 연결 요소로 되돌리는 것은
  마지막 수단이다 — ②의 손실이 이 기능의 목적(사용자가 어느 피사체를 오릴지 고른다)과 정면으로
  부딪친다.

### [2026-08-25] 휘도 1채널 안내가 흰 물체·흰 배경에서 충분한가

- **ID**: OQ-P-296
- **출처**: [segmentation-alpha-refinement.md](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)
  「범위 - 제외」 — 컬러 3채널 안내를 채택하지 않고 휘도로 시작한다.
- **항목**: ① 가이드 필터는 안내자에 경계가 있는 자리에서만 알파를 옮긴다. **휘도가 같고 색만
  다른 경계는 1채널 안내로 안 잡힌다.** ② 관측된 결함(파란 매트 위 흰 물체)은 휘도차가 있어
  잡히지만, 같은 사진의 **흰 체온계와 흰 리모컨·흰 상자가 맞붙는 자리**는 휘도차가 거의 없다.
  ③ 컬러 3채널로 올리면 픽셀마다 3×3 공분산 역행렬이 필요해 구현과 실패 모드가 함께 는다.
- **상태**: 미해결 (휘도로 시작하고 사진 세트가 판정한다)
- **해소 메모**: 흰 물체 경계가 흰 배경으로 새어 나가는 사례가 보이면 컬러로 올린다. 올릴 때
  바꾸는 것은 안내자 추출 함수 하나이고 계수·적용 단계는 그대로다.

### [2026-08-25] 정련의 입력을 하드 매트가 아니라 신뢰도 마스크로 바꿀 것인가

- **ID**: OQ-P-297
- **출처**: [segmentation-alpha-refinement.md](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)
  「설계 - 정련 알고리즘」 — 입력 `p` 를 하드 매트로 고정했다.
- **항목**: ① 탐침에서 ML Kit 후보별 신뢰도 마스크가 **연속값을 갖는다**는 것을 확인했다(0과 1
  사이가 각각 약 26%·41%). 하드 매트를 쓰는 동안 그 정보를 버린다. ② 그런데 그 연속값의 대부분이
  **경계 전이가 아니라 내부 업샘플 격자 잡음**이라, 그대로 정련 입력으로 쓰면 물체 내부가 얼룩덜룩
  반투명해진다. ③ 내부를 1로 되채우는 처리를 앞에 붙이면 쓸 수 있으나, 그 처리가 다시 구멍
  메우기와 같은 부류의 문제를 부른다(OQ-P-286).
- **상태**: 미해결 (이번 라운드는 하드 매트로 간다)
- **해소 메모**: 정련 결과가 경계에서 부족할 때 본다. 신뢰도를 **안내자 보조**로 쓰는(입력이 아니라)
  선택지도 있다 — 색이 비슷한 경계에서 모델의 판단을 더하는 방향이다.

### [2026-08-25] 정련 반경과 정칙화 기본값의 근거가 없다

- **ID**: OQ-P-298
- **출처**: [segmentation-alpha-refinement.md](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)
  「근거 등급」 — 조건부 항목.
- **항목**: ① 반경이 너무 작으면 경계가 안 옮겨지고, 너무 크면 얇은 구조가 뭉개진다. ② 정칙화가
  너무 작으면 잡음까지 따라가고, 너무 크면 평균 필터로 퇴화해 경계가 흐려지기만 한다. ③ 두 값 다
  측정 없이 정한다. **모델 마스크의 업샘플 격자 주기가 반경의 자연스러운 하한**이라는 단서는 있으나
  그 주기를 잰 적이 없다.
- **상태**: 미해결
- **해소 메모**: 사진 세트에서 두 값을 몇 단계로 바꿔 눈으로 고른다. 판정 기준은 "파란 띠가
  줄었는가"와 "면봉 막대·케이블이 살아남았는가" 둘이다.

### [2026-08-25] 원본 해상도 정련의 체감 지연을 재지 않았다

- **ID**: OQ-P-299
- **출처**: [segmentation-alpha-refinement.md](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)
  「설계 - 처리 해상도」 — 계수는 축소판, 적용은 원본 해상도.
- **항목**: ① 계수 산출은 축소판이라 싸지만 **적용은 원본 픽셀 전부를 훑는다.** 후보가 여럿이면
  그만큼 곱해진다. ② 촬영 후 후보 화면까지의 체감 지연이 이 라운드에서 늘어나는데 그 증가폭을
  모른다. ③ 앞 라운드가 세운 취소 규약이 있어 사용자가 뒤로 나가면 빠져나오지만, **붙들고 있는
  시간 자체는 줄지 않는다.**
- **상태**: 미해결 (소요 시간 로그를 심고 사진 세트가 판정한다)
- **해소 메모**: 감당이 안 되면 정련을 **경계 밴드로 한정**한다 — 마스크 경계에서 안팎 몇십 픽셀
  띠에서만 돌리는 방식이고, 비용이 면적이 아니라 둘레에 비례한다. 내부는 어차피 불투명이라
  정련해도 바뀌는 것이 없다.

### [2026-08-25] 정련의 피크 메모리를 재지 않았다

- **ID**: OQ-P-300
- **출처**: [segmentation-alpha-refinement.md](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)
  「설계 - 안내자 공급」 — 계획 검수가 초안의 메모리 계약이 사실이 아님을 밝혔다.
- **항목**: ① 정련은 bounds 크기 안내자 `IntArray`(12MP 전면이면 48MB)와 패치 `ByteArray`(12MB),
  축소판 실수 배열 여섯(배율 4에서 약 19MB)을 **동시에** 든다. ② 초안 계획은 여기에 원본 해상도
  휘도·알파 실수 배열 둘을 더 실체화해 **약 175MB**가 될 뻔했고, 검수가 그것을 잡아 즉석 계산으로
  바꿨다. ③ 그래도 약 79MB가 기존 피크 위에 얹히는데 `largeHeap` 이 없다. **실측한 적이 없다.**
- **상태**: 미해결 (즉석 계산으로 줄였고 잔여를 추적한다)
- **해소 메모**: OOM 이 실제로 나면 되돌리기가 받아 크래시는 아니지만 기능이 통째로 죽는다.
  그때는 정련을 경계 밴드로 한정하는 안(OQ-P-299의 대안)이 메모리도 같이 줄인다.

### [2026-08-25] 권한 화면 인셋 수정을 실기기에서 확인하지 않았다

- **ID**: OQ-P-301
- **출처**: `feature/gallery/impl/.../component/GalleryPermissionRequestComponent.kt` ·
  `feature/camera/impl/.../component/CameraPermissionRequestComponent.kt`(PR #350 develop 머지) —
  이슈 #345가 지목한 인셋 이중 적용을 걷어냈고 카메라 쪽은 무는 자리를 바깥 `Box`로 옮겼다.
- **항목**: ① **고친 결과를 실기기에서 본 사람이 없다.** PR 본문이 스스로 밝히듯 권한 거부 상태를
  로컬에서 재현하지 못해 비교 이미지의 "After"는 실측이 아니라 이슈에 붙은 디자인 이미지다.
  ② **테스트로도 안 잠긴다.** 인셋은 기기의 실제 시스템 바 높이에 붙는 값이라 유닛 테스트 대상이
  아니고, 스크린샷 테스트도 없다. 즉 이 화면의 세로 정렬은 지금 **사람 눈 말고 검증 수단이 없다**.
  ③ **카메라 쪽 안내 블록의 하단 `padding3` 보정에는 대응 디자인이 없다.** 이슈 이미지는 갤러리
  화면 것이고, 코드 주석의 논증(닫기 줄을 쌓으면 블록이 줄 높이의 절반만큼 내려 보인다)이 유일한
  근거다. 겹쳐 놓기로 바꾼 뒤에도 보정이 남아 있어야 하는지가 이미지로 확정되지 않았다.
- **상태**: 미해결 (실기기 확인 필요 — 권한 거부 상태를 만들어 두 화면을 각각 본다)
- **해소 메모**: 확인되면 [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md)
  「as-built 재정정」과 [c101 스펙](../superpowers/specs/archive/2026-08-01-c101-camera-picture-confirm.md)
  「권한 화면 as-built 갱신」의 실기기 미확인 문장을 지운다. 어긋나 있으면 ③이 먼저 답을 받아야
  한다 — 보정을 뺄지 값을 바꿀지는 디자인이 정한다.

### [2026-08-25] 서버 평문 포트가 닫히는 시점을 앱이 알 수 없다

- **ID**: OQ-P-302
- **출처**: 서버 `deploy/caddy/Caddyfile` · `bootstrap/src/main/resources/application.yaml`
  (`server.forward-headers-strategy`) · 서버 런북 `docs/operations/https-setup.md`(서버 #112·#113
  `main` 머지) — TLS 종단이 앞단 리버스 프록시로 가고, 런북이 검증 뒤 **평문 포트를 차단하는
  단계**를 절차에 둔다.
- **항목**: ① **차단은 배포가 아니라 사람이 하는 1회성 인프라 조작이라 서버 커밋에 남지 않는다.**
  이 감사는 커밋 delta를 보는 체계라 "언제부터 평문이 죽는가"를 코드에서 읽을 수 없다
  (OQ-P-284와 같은 부류이고, 이번이 **두 번째**다). ② 차단되는 순간 **기존 `YG_BASE_URL`로 빌드된
  앱은 전부 연결에 실패한다** — 서버 에러가 아니라 연결 실패라 `safeApiCall`의 네트워크 갈래로
  떨어진다. ③ 차단 전까지는 애플리케이션이 `X-Forwarded-*`를 무조건 신뢰하므로 프록시를 우회한
  평문 경로로 **스킴·클라이언트 IP를 위조**할 수 있다(런북이 지적한 위험이고, 차단이 그 답이다).
- **상태**: 미해결 (서버팀에 전환 시점을 물어야 한다 — 앱 base URL 교체가 그 앞에 와야 한다)
- **해소 메모**: 시점이 정해지면 앱 `local.properties`/CI의 `YG_BASE_URL`을 옮긴다.
  > 📌 **순서가 뒤집혔고 그래도 앱이 안 끊겼다(2026-08-25, PR #358)** — 원래 이 메모는 "base URL을
  > 먼저 옮기고 그다음 `usesCleartextTraffic`을 지운다"였는데 앱은 반대로 갔다. 지워도 끊기지 않은
  > 이유는 `network_security_config.xml`의 `debug-overrides`가 **디버그 빌드에는 평문을 계속 열어
  > 두기** 때문이다(OQ-P-076 해소 메모). 그래서 이 항목이 쥔 위험은 **릴리즈 빌드 하나로 좁아졌다** —
  > 평문 주소로 조립된 릴리즈는 이제 서버 차단을 기다릴 것도 없이 **앱 자신이 먼저 막는다.**
  > 차단 시점이 여전히 코드에 안 남는다는 사실은 그대로다.
  [api/conventions.md](../api/conventions.md) "전송" 절의 ⚠️ 문장도 그때 확정형으로 고친다.
  > 📌 **저장소가 가르치는 주소는 옮겨졌다(2026-08-26, PR #376)** — `http/README.md`의 `base_url`
  > 예시가 평문 IP·포트에서 **HTTPS 도메인**으로 바뀌고, "앱에서는 아직 이 서버를 호출할 수 없다"던
  > 절이 "`YG_BASE_URL`은 HTTPS 주소를 넣는다 / 프록시를 우회하는 평문 포트 주소를 넣지 말 것"으로
  > 교체됐다. **그래도 이 항목은 안 닫힌다** — 실제 빌드가 어떤 주소를 쓰는지는 `local.properties`에
  > 있어 커밋 delta로 안 보이고, ①(차단 시점이 코드에 안 남는다)은 성질상 그대로다. 바뀐 것은
  > **새로 클론한 사람이 평문 주소를 집어넣을 확률**이다.

### [2026-08-26] 배치 화면이 새로 연 캔버스 조회·매핑 경로에 테스트가 0건

- **ID**: OQ-P-303
- **출처**: `CanvasToppingPlaceViewModel.kt#loadCanvasIfNeeded`·`#withCanvas`(PR #357) —
  초안의 `groupId`로 `GetTodayParfaitUseCase`를 부르고 응답을 배경색·배경 이미지 URL·기존 토핑
  목록으로 옮기는 경로가 통째로 들어왔는데, 같은 라운드가 `CanvasToppingPlaceViewModelTest`에
  더한 것은 **새 의존을 생성자에 끼우고 조회를 조용히 실패시키는 스텁뿐**이다
  (`Result.failure(...)` 고정). 즉 기존 테스트가 계속 초록인 이유는 **새 경로가 한 번도
  성공하지 않기 때문**이다.
- **항목**: ① 잠글 것이 넷 있다 — 배경이 `Color`면 색으로, `Image`면 URL로 갈리는 매핑 /
  색 문자열을 못 읽으면 기본 배경을 유지하는 폴백 / 토핑을 `positionZ` 오름차순으로 정렬하는 것 /
  `groupId`가 같으면 두 번 조회하지 않는 가드. ② 그중 마지막은 필드 하나(`canvasLoadedForGroupId`)로
  구현돼 있어 **초안 흐름이 여러 번 방출되면 무엇이 보장되는지가 테스트 없이는 읽히지 않는다.**
  ③ 조회 실패가 배치를 막지 않는다는 계약도 지금은 주석뿐이다.
- **상태**: 미해결 (테스트를 안 쓴 이유가 기록돼 있지 않다)
- **해소 메모**: 채우면 [c106-topping-place 스펙](../superpowers/specs/archive/2026-08-19-c106-topping-place.md)
  드리프트 ④의 "테스트 0건" 문장을 지운다. OQ-P-240이 이 항목을 남기고 닫혔다.

### [2026-08-26] 빈 캔버스 안내판의 노출 조건에 "배경 미설정"이 들어갔는데 정책 근거가 없다

- **ID**: OQ-P-304
- **출처**: `YGCanvas.kt#CanvasArea`(PR #351) — 빈 안내판 조건이 `isEmpty`에서
  **`isEmpty && background == null`**이 됐다. 배경을 고른 캔버스는 토핑이 0개여도 안내 문구가
  뜨지 않고 고른 배경만 보인다. 같은 라운드가 안내판을 `Gray100` **판**으로 만들어 배경 자리를
  통째로 덮게 했고(그전에는 문구만 얹혔다), 배경 미설정의 기본 그림은 **흰 바탕**으로 갈렸다.
- **항목**: ① 배경을 고른 빈 캔버스에 안내를 아예 안 보여 주는 것이 의도인지 — 첫 토핑을 올리라는
  안내가 필요한 시점은 오히려 배경만 고르고 비어 있을 때다. ② 안내판 바탕(`Gray100`)과 미설정
  기본 바탕(흰색)이 갈린 것이 의도인지 — 사용자에게는 "빈 캔버스"가 두 가지 색으로 보인다.
  ③ 안내 문구 자체가 위키에 정책 소스가 없다(코드가 먼저 확정한 문구 목록에 하나 더 붙는다).
- **상태**: 미해결 (정책 소스 수집이 선행 — 위키 [[캔버스-반응형-레이아웃]]에 빈 상태 조항이 없다)
- **해소 메모**: 정해지면
  [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md)의
  `isEmpty` 파생 서술과 [architecture/design-system](../architecture/design-system.md) 캔버스 절을
  함께 고친다.

### [2026-08-26] 서명 키 부재 안내가 태스크 이름 일치에 걸려 있고 실제로 발화하는지 확인되지 않았다

- **ID**: OQ-P-305
- **출처**: `buildlogic/AndroidConfig.kt#failWhenStoreFileMissing`(PR #354) — 키가 없거나 가리키는
  파일이 없을 때 **설정 단계에서 터뜨리지 않고** `validateSigningRelease`·`validateSigningDebug`에
  `doFirst`를 얹어 그 태스크가 돌 때만 실패시킨다. 근거는 분명하다(설정 단계에서 막으면 키를 못 받은
  사람과 CI가 ktlint·테스트조차 못 돌린다). 다만 `tasks.matching { it.name == … }.configureEach`는
  **그 이름의 태스크가 없으면 아무 일도 하지 않는다** — 안내가 조용히 사라지는 형태다.
- **항목**: ① 키 없이 `:app:assembleRelease`를 돌려 그 메시지가 실제로 나오는지 확인한 사람이 없다.
  ② 서명 없이도 끝까지 가는 조립 경로(태스크 이름이 다르거나 그 태스크를 안 타는 경로)가 있는지.
  ③ AGP는 debug 키가 비면 자기 기본 keystore로 떨어지는데, 이 안내는 그 경우에도 실패를 만든다 —
  "기본 키로 조용히 디버그 빌드가 되는 것"과 "이름 있는 실패" 중 무엇을 원하는지가 안 적혀 있다.
- **상태**: 미해결 (키 없는 릴리즈 조립을 한 번도 안 돌려 봤다)
- **해소 메모**: 확인되면 [ADR-0003](../adr/0003-convention-plugins-version-catalog.md)의
  as-built 문단에 결과를 적는다. `parfait-release.keystore`가 `.gitignore`에 올라간 것(PR #368)까지
  같은 자리다 — 키를 어디서 받는지가 저장소 어디에도 안 적혀 있다.

### [2026-08-26] 번들 폰트가 수정본이 됐는데 라이선스 사본이 사용자에게 안 닿는다

- **ID**: OQ-P-306
- **출처**: `core/designsystem/OFL.txt`(#366 신설) · `core/designsystem/src/main/res/font/suit_*.ttf` ·
  `feature/app/setting/impl` `AppSettingScreen` — 번들 SUIT가 cmap을 손본 **수정본**으로 바뀌면서
  OFL 1.1의 "수정본 배포 시 라이선스 사본 동봉" 조건이 걸렸다. 사본은 **모듈 루트 파일**이라
  저장소에는 있으나 **APK에 실리지 않고**, S-001 설정 화면 항목은 계정·서비스 이용약관·개인정보
  처리 방침·버전 넷이라 **오픈소스 고지 자리가 없다**. `oss-licenses` 계열 플러그인도 안 쓴다.
- **항목**: ① 고지를 어디에 둘지 — S-001에 "오픈소스 라이선스" 항목을 더할지(S-004 웹뷰 패턴을
  재사용할 수 있다), 아니면 라이선스 수집 플러그인을 붙여 목록을 자동 생성할지. ② 자동 생성을
  고르면 **번들 폰트는 Gradle 의존이 아니라 리소스 파일이라 그 목록에 안 잡힌다** — 폰트 몫은
  따로 실어야 한다. ③ 사본을 `assets/`나 `res/raw/`로 옮겨 APK에 넣을지, 화면 문구로만 담을지.
- **상태**: 미해결 (배포 전에 답이 필요한 자리다 — 스토어 배포가 조건을 실제로 발동시킨다)
- **해소 메모**: 정하면 [design-system](../architecture/design-system.md) 폰트 절의 ②와
  [s004 스펙](../superpowers/specs/archive/2026-07-20-s004-terms-privacy-webview.md)·
  [s001 스펙](../superpowers/specs/archive/2026-07-19-app-setting-s001.md) 항목 목록을 함께 고친다.
  `.gitignore`에 올라간 릴리즈 keystore(OQ-P-305)와 같이 **배포가 가까워지며 드러나는 자리**다.

### [2026-08-26] 폰트 수정본에 회귀 감지선이 없고 fallback 결과를 본 사람도 없다

- **ID**: OQ-P-307
- **출처**: `core/designsystem/src/main/res/font/suit_*.ttf`(#366) — 교체 대상이 **바이너리 넷**이라
  diff로 무엇이 바뀌었는지 읽을 수 없고, 파일명·버전·아웃라인이 그대로라 **원본을 다시 넣어도
  아무것도 빨갛게 되지 않는다.** 무엇을 고쳤는지는 `OFL.txt`의 고지 문단과 커밋 메시지에만 있다.
- **항목**: ① 다음에 SUIT를 올릴 때 **원본으로 되돌아가는 회귀**를 무엇이 잡는지 — 지금은 아무것도
  없다. cmap에 빈 글리프 매핑이 없음을 확인하는 검사를 빌드나 CI에 둘지. ② fallback으로 넘어간
  문자는 **기기 시스템 폰트로 그려져 화면에 두 글자체가 섞인다** — 어느 문자가 그 대상인지,
  섞인 결과가 디자인상 받아들일 만한지 **아무도 실기기로 보지 않았다**(#365가 보고한 것은
  투명하게 찍히는 쪽이다). ③ 계측 테스트가 있어도 CI가 컴파일만 하므로(OQ-P-102 ②) 렌더 결과를
  잠글 자리는 지금 구조에 없다.
- **상태**: 미해결 (**실기기 1회로 ②가 갈린다** — 대상 문자를 모아 한 화면에 찍어 보면 된다)
- **해소 메모**: ①을 붙이면 [design-system](../architecture/design-system.md) 폰트 절에 검사 위치를
  적는다. ②가 확인되면 같은 절의 "기기마다 글자체가 섞인다" 문장을 관측 결과로 바꾼다.

### [2026-08-26] 리소스 축소를 처음 켰는데 축소된 산출물을 실행해 본 사람이 없다

- **ID**: OQ-P-308
- **출처**: `build-logic/convention/.../buildlogic/AndroidConfig.kt#setConfigAndroidApplication`(#372) —
  release 빌드 타입에 **`isShrinkResources = true`**가 신설됐다. 그전까지는 `isMinifyEnabled`만
  켜져 있어 **코드만 줄고 리소스는 그대로 실렸다.** 이 컨벤션 플러그인은 `app`과 `app-preview`
  두 애플리케이션 모듈에 함께 걸리므로 스위치도 둘 다에 걸린다.
- **항목**: ① **리소스 축소의 실패는 빌드가 아니라 실행에서 드러난다** — 잘못 걷힌 리소스는
  조립을 통과하고 그 화면에 들어갈 때 터진다. 릴리즈 APK를 설치해 화면을 돌아본 기록이 0건이다
  (OQ-P-146과 같은 뿌리). ② **`keep.xml`이 없다.** 지금 develop에 `Resources.getIdentifier`
  사용처가 0건이라 이름으로 찾는 리소스가 없어 당장은 안전하지만, 그런 코드가 들어오는 순간
  방어가 아무 데도 없다 — 규약을 어디에 적을지. ③ 같은 커밋이 **debug 블록에도 `proguardFiles`를
  넣었는데** `isMinifyEnabled = false`라 **아무 일도 안 한다**. 지울지, 나중에 켤 자리 표시로
  남길지 정한다(남긴다면 그 뜻을 주석으로 적어야 다음 사람이 켜져 있다고 오해하지 않는다).
- **상태**: 미해결 (**동작 영향은 릴리즈 산출물에만 있다** — 디버그 빌드는 축소를 안 한다)
- **해소 메모**: ①이 확인되면 [ADR-0003](../adr/0003-convention-plugins-version-catalog.md)
  as-built 절에 관측 결과를 적는다. ②를 정하면 같은 절에 `keep.xml` 위치를 함께 적는다.

### [2026-08-26] NDK 크래시 수집기를 붙였는데 심볼이 없어 주소만 남는다

- **ID**: OQ-P-309
- **출처**: `app/build.gradle.kts`(#372) — `firebase-crashlytics-ndk`가 붙었다. 근거는 AAR로 들어오는
  CameraX·DataStore 네이티브 라이브러리가 **시그널로 죽으면 JVM 예외가 없어 기본 수집기로는
  안 잡힌다**는 것이다. 같은 주석이 `nativeSymbolUploadEnabled`를 **켜지 않는 이유**도 적었다 —
  자체 네이티브 빌드가 없어 올릴 언스트립 심볼이 없고, 서드파티 `.so`는 주소만 남는다.
- **항목**: ① **그러면 이 수집기가 무엇을 주는가** — 심볼 없는 주소 스택으로 CameraX·DataStore
  네이티브 크래시의 원인을 어디까지 좁힐 수 있는지 아무도 확인하지 않았다(라이브러리 이름과
  오프셋까지는 남는다). ② **리포트가 콘솔에 실제로 도착하는지 본 기록이 0건**이다 — 릴리즈를
  설치해 크래시를 내 본 적이 없다. ③ NDK 수집기는 APK에 자기 `.so`를 더한다 — 그 크기 증가를
  재지 않았고, 같은 라운드가 켠 리소스 축소(OQ-P-308)와 방향이 반대다.
- **상태**: 미해결 (**수집 자체는 손해가 없다** — 안 붙였으면 그 크래시는 통째로 안 보였다)
- **해소 메모**: ①②가 확인되면 [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) as-built 절에
  적는다. 자체 네이티브 코드가 생기면 그때 `nativeSymbolUploadEnabled`를 다시 본다.

### [2026-08-26] 앱 버전이 1에서 3으로 뛰었다 — 결번 하나가 저장소 밖 산출물을 가리킨다

- **ID**: OQ-P-310
- **출처**: `gradle/libs.versions.toml`(#374) — `appVersionCode` **1 → 3**, `appVersionName`
  **0.0.1 → 0.0.3**. 이 값을 바꾼 커밋은 저장소 전체에서 **도입 커밋과 이번 것(같은 내용이 브랜치와
  머지 양쪽에 하나씩) 둘뿐**이라 **2는 어느 브랜치에도 존재한 적이 없다.** 태그는 하나 있다 —
  경량 태그 `0.0.3`이고 develop이 아니라 **`release/version-0.0.3-3` 브랜치의 머지 커밋**을 가리킨다
  (OQ-P-311).
- **항목**: ① 2가 어디서 쓰였는가 — 저장소 밖에서 조립해 올린 산출물이 있다면 **어떤 코드가 어떤
  버전으로 나갔는지가 저장소가 아니라 사람의 기억에 걸려 있다**는 뜻이다. 크래시 리포트는 버전으로
  묶이므로(OQ-P-309) 이 축이 흐리면 리포트를 커밋에 되짚을 수 없다. ② **다음 올림을 무엇이
  강제하는가** — 손으로 고치는 값이라 잊으면 스토어가 같은 `versionCode`를 거절한다. 지금 규율은
  태그 하나뿐이고 그 태그도 **경량**이라 누가 언제 붙였는지 남지 않는다. ③ `previewVersionCode`는
  1 그대로다 — `app-preview`는 `applicationId`가 달라 셈이 따로 간다.
- **상태**: 미해결 (**빌드 영향 0** — 값이 무엇이든 조립은 된다. 배포 추적의 문제다)
- **해소 메모**: ②를 정하면 [ADR-0003](../adr/0003-convention-plugins-version-catalog.md)
  버전 카탈로그 절에 규칙을 적는다(태그를 쓸지, CI가 올릴지). ①은 사람에게 물어야 답이 나온다.
  > 📌 **두 번 더 올랐고 이번엔 develop 에도 들어왔다(2026-08-30, PR #409)** — 한 PR 이 커밋 둘로
  > `3 → 4 → 5`(`0.0.3 → 0.1.0 → 0.1.1`)를 연달아 올렸고, `develop` 과 최신 release 브랜치가
  > **같은 값 5/0.1.1** 을 든다. 즉 develop 이 이 축에서 처음으로 배포본과 맞았다.
  > ⚠️ **②는 그대로다** — 여전히 손으로 고치는 값이고, 이번에도 결번은 아니지만 **한 PR 안에서 두
  > 칸을 뛴 이유가 어디에도 안 적혀 있다**(브랜치 이름은 `build/bump-version-0.1.0-4` 인데 담긴 것은
  > 5/0.1.1 까지다). 태그도 `0.1.0`·`0.1.1` 이 새로 붙었는데 **셋 다 경량이고 develop 이 아니라
  > release 브랜치 쪽 커밋을 가리킨다**(OQ-P-311).

  > 📌 **1.0.0 이 됐다(2026-09-01, PR #434)** — `5 → 6`·`0.1.1 → 1.0.0`이고, 이번에는 커밋 하나가 한
  > 칸만 올려 결번도 건너뜀도 없다. **정식 판 번호로 올린 근거는 어디에도 없다** — PR 제목과 브랜치
  > 이름(`chore/version-1.0.0-6`)이 값을 되풀이할 뿐이고, 이 커밋에 딸린 기능 변경도 없다(버전
  > 카탈로그 두 줄이 전부다). ①(2가 어디서 쓰였는가)과 ②(다음 올림을 무엇이 강제하는가)는 그대로다 —
  > **0.x 에서 1.0.0 으로 넘어가는 판단조차 손으로 고친 두 줄에만 남는다**는 것이 이번에 드러난
  > 형태다. `previewVersionCode`는 여전히 1이다.

  > 📌 **이름은 두고 코드만 올랐다 — 1.1.3 이 두 트리다(2026-09-11, PR #488·#490)** — #488 이 `9 → 10`·
  > `1.1.2 → 1.1.3` 을 올렸고, 한 시간 반 뒤 권한 요청 수정(#489)이 머지된 다음 #490 이 **코드만** `10 → 11` 로
  > 올렸다. 버전 이력에서 이름을 그대로 두고 코드만 올린 커밋은 이것이 처음이다. 그래서 `1.1.3` 이라는 이름
  > 아래 **코드 10(수정 전 트리)과 코드 11(수정 후 트리)** 이 있다. `origin/release/version-1.1.3-10` 은 #488 머지
  > 커밋이고 `-11` 은 #490 머지 커밋이다. **경량 태그 `1.1.3` 은 11 쪽만** 가리키므로 **10 을 되짚는 표식은
  > 브랜치 이름 하나뿐이다.** 코드 10 이 실제로 나갔는지, 다시 뗀 이유가 #489 인지는 PR 본문 셋 어디에도 적혀
  > 있지 않다. ①이 결번 대신 **같은 이름의 앞선 코드**에 걸리는 형태로 되풀이되고, 이름으로만 묶으면 두 트리의
  > 리포트가 섞인다(OQ-P-309). ②는 그대로다.

  > 📌 **코드 10 을 가리키는 ref 가 0개가 됐다(2026-09-11 확인)** — 위 📌가 "표식은 브랜치 이름 하나뿐"이라 적은
  > `release/version-1.1.3-10` 은 그 문장을 커밋한 시각(00:28 KST)보다 먼저인 00:19 KST 에 원격에서 지워져
  > 있었다(GitHub 이벤트 `DeleteEvent`). `-11` 도 지금 원격에 없다. 경량 태그 `1.1.3` 은 00:15 KST 에 한 번
  > 지워졌다가 지금 `c37dc2b4c`(코드 11)를 가리킨다 — 지우기 전에 어느 커밋을 가리켰는지는 이벤트에 남지
  > 않는다. 그래서 코드 10 을 되짚을 수단은 **develop 이력 속 #488 머지 커밋(`544ce434a`)과 그 커밋의 버전
  > 카탈로그 두 줄**뿐이다. ①이 한 단계 더 흐려졌고 ②는 그대로다.

### [2026-08-26] 릴리즈 계보가 develop 밖에 있다 — 배포된 0.0.3에 develop에 없는 45커밋이 들어 있다

- **ID**: OQ-P-311
- **출처**: `origin/release/version-0.0.3-3`(경량 태그 `0.0.3`이 이 브랜치의 머지 커밋을 가리킨다) —
  이 브랜치는 `origin/develop`에 **없는 커밋 45개**를 담고 있고, 그중 머지 여덟이 feature 브랜치를
  직접 받았다. 반대 방향(develop에만 있는 커밋)은 여섯이다. 받은 브랜치 중 **넷이 이 문서가
  "미머지 추적 항목"으로 세 라운드째 세어 온 그 넷**이다 — `segmentation-candidate-coverage` ·
  `segmentation-alpha-kernel` · `segmentation-postprocess-wiring` · `segmentation-alpha-refinement`.
  다섯째 `feature/toast-position-fix`는 이 문서에 이름조차 없다.
- **항목**: ① **이 감사 체계의 전제가 흔들린다** — [doc-baseline](../doc-baseline.md)은 `develop`을
  기준선으로 잡고 그 delta만 본다. 사용자에게 실제로 나간 산출물이 develop이 아니라 release
  브랜치에서 조립됐다면, **"문서가 검증한 코드"와 "배포된 코드"가 다른 트리**다. ② 저 넷을
  develop에 되돌려 머지할 것인가, 아니면 release가 별도 계보로 계속 갈 것인가 — 전자면 이 항목은
  다음 라운드에 저절로 닫히고, 후자면 기준선을 **둘로** 두거나 감사 대상을 바꿔야 한다.
  ③ 선작성 스펙·계획 넷([전처리](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)·
  [후처리](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)·
  [알파 정련](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)과 그 계획들)이 지금 `active`인데,
  **구현이 release에는 있고 develop에는 없다** — 아카이브 판정 기준(`develop` 머지)을 그대로 둘지
  정해야 한다.
- **상태**: 미해결 (**문서 신뢰도에 직접 걸리는 자리다** — 코드가 아니라 감사 범위의 문제)
- **해소 메모**: ②가 정해지면 [doc-baseline](../doc-baseline.md) "현재 기준선" 절에 감사 대상 브랜치를
  명시하고, `sync-tjyg-develop-baseline` 스킬 문서의 범위 문장도 함께 고친다. **이번 라운드는
  release 브랜치를 감사하지 않았다** — 기준선 규율이 develop만 보도록 돼 있어서다.
  > 📌 **다섯째가 develop으로도 들어왔다(2026-08-26, PR #371)** — 이 문서에 이름조차 없던
  > `feature/toast-position-fix`가 develop에 머지돼 **두 계보 모두에 있다.** 그래서 갈라진 폭은
  > 45커밋에서 **43커밋**으로 줄었다(develop만 가진 것은 여덟이다). ⚠️ **세그멘테이션 넷은 그대로
  > release에만 있고**, 이 항목이 묻는 ②③은 아무것도 안 바뀌었다. 이번 사례가 보여 주는 것은
  > **한 브랜치가 두 계보에 각각 머지될 수 있다**는 것이고, 그러면 "어느 트리가 배포됐나"는 더
  > 흐려진다 — 같은 변경이 양쪽에 있어도 **주변 커밋이 달라 결과 트리가 같다는 보장은 없다.**
  > 📌 **세그멘테이션 넷이 develop 으로도 들어왔다(2026-08-27, PR #363)** — ③은 닫혔다. 선작성
  > 스펙·계획 중 셋([후처리](../superpowers/specs/archive/2026-08-24-segmentation-mask-postprocessing.md)·
  > [알파 정련](../superpowers/specs/archive/2026-08-25-segmentation-alpha-refinement.md)·
  > [커널 취소 확인 전환](../superpowers/specs/archive/2026-08-27-alpha-kernel-suspend-cancellation.md))이
  > **`develop` 머지라는 기존 기준 그대로** 아카이브로 갔고, 기준을 바꿀 필요가 없어졌다.
  > ⚠️ **①②는 아무것도 안 바뀌었고, 오히려 한 겹 나빠졌다.** develop 이 받은 것은 release 가 받은
  > 그 커밋들이 **아니라 rebase 된 다른 커밋들**이라 release-only 커밋 수는 **43 그대로**다.
  > 게다가 rebase 과정에서 커널 전체가 `suspend` + `ensureActive()` 로 바뀌었으므로, 두 계보는
  > 이제 **SHA 만이 아니라 내용이 다르다** — 배포된 `0.0.3` 은 콜백 방식 커널을 담고 있고 정련
  > 라운드의 리뷰 반영 커밋 일부도 없다. 즉 "같은 기능이 양쪽에 있다"는 말이 이 넷에는 성립하지
  > 않는다. 반대 방향(develop 만 가진 것)은 **85커밋**으로 벌어졌다.
  > ⚠️ **③이 다시 열렸고, 이번엔 계보가 둘 더 생겼다(2026-08-30)** — `release/version-0.1.0-4` 와
  > `release/version-0.1.1-5` 가 새로 생겨 release 계보가 셋이 됐다. 최신 것을 기준으로 세면
  > **release 만 가진 커밋 50개 · develop 만 가진 커밋 8개**다(그 여덟은 이번 회차의 delta 그 자체다).
  > 즉 갈라진 방향이 **뒤집혔다** — 지금까지는 develop 이 앞서고 release 가 뒤처졌는데, 이제
  > **배포 계보가 develop 이 아직 못 받은 것을 담고 있다.**
  > release 는 이번 회차의 세 feature 브랜치(#391 스포트라이트 토스트 · #381 갤러리 상단바 ·
  > init-loading)를 **develop 과 별개로 직접 받았고**, 그 위에 develop 에 **없는** 브랜치 셋을 더
  > 받았다 — `feature/debug-mode` · `feature/cache-image` · `feature/canvas-polling`(PR2
  > `feature/canvas-today-ssot` 를 품은 스택).
  > **그래서 ③이 같은 형태로 되살아난다** — 선작성 문서 넷이 지금 `draft` 인데 구현은 release 에만
  > 있다: [로그인 디버그 모드 스펙](../superpowers/specs/2026-08-28-login-debug-mode.md) ·
  > [그 계획](../superpowers/plans/2026-08-28-login-debug-mode.md) · [PR2 계획](../superpowers/plans/archive/2026-08-27-canvas-today-ssot.md) ·
  > [PR3 계획](../superpowers/plans/archive/2026-08-27-canvas-polling.md). 아카이브 판정을 `develop` 머지로 두는 한
  > **구현이 끝난 문서가 계속 `draft` 로 남는다.**
  > 태그도 둘 늘어(`0.1.0`·`0.1.1`) 셋이 됐고 **여전히 전부 경량**이며 셋 다 release 쪽 커밋을
  > 가리킨다(OQ-P-310).

  > 📌 **③이 다시 좁아졌다 — 넷 중 셋이 develop 으로 들어왔다(2026-08-31, PR #404·#408)**
  > `feature/canvas-polling` 스택(PR2 를 품는다)과 `feature/cache-image` 가 develop 에 머지돼
  > 선작성 문서 셋([스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md) · [PR2 계획](../superpowers/plans/archive/2026-08-27-canvas-today-ssot.md) ·
  > [PR3 계획](../superpowers/plans/archive/2026-08-27-canvas-polling.md), PR1 계획까지 넷)이 `develop` 머지라는
  > **기존 판정 기준 그대로** 아카이브로 갔다. 세그멘테이션 넷 때(2026-08-27)와 같은 결말이다.
  > **남은 것은 `feature/debug-mode` 하나** — `develop` 에 `DebugMode*` 심볼이 0건이라
  > [로그인 디버그 모드 스펙](../superpowers/specs/2026-08-28-login-debug-mode.md)과 [그 계획](../superpowers/plans/2026-08-28-login-debug-mode.md)은
  > 구현이 끝난 채 `draft` 로 남는다.
  > ⚠️ **①②는 이번에도 안 바뀌었고 갈림은 오히려 커졌다.** 최신 release(`0.1.1-5`) 기준으로
  > **release 만 50커밋 · develop 만 43커밋**이다 — release-only 가 50 에서 줄지 않았다는 것은
  > develop 이 받은 것이 release 가 받은 그 커밋이 아니라 **리베이스된 다른 커밋**이라는 뜻이다
  > (세그멘테이션 넷 때와 같은 형태). 직전 회차의 "develop 만 8"이 43 으로 벌어진 것은 이번
  > delta 그 자체다.

  > 📌 **①이 최신 계보에서는 성립하지 않는다(2026-09-10, PR #483)** — `origin/release/version-1.1.2-9`
  > 가 `origin/develop` HEAD **그 커밋**이라 양방향 0커밋이고, 직전 `release/version-1.1.1-8` 도
  > 그랬다. 즉 **문서가 검증한 트리와 배포용으로 뗀 트리가 같다.** 관행이 뒤집힌 자리는 방향이다 —
  > release 가 feature 브랜치를 직접 받아 앞서가던 형태가 아니라, **develop 에서 릴리즈 브랜치를 떼고
  > 버전을 올린 그 커밋을 develop 이 PR 로 되받는다.** 그래서 그 회차의 delta 가 곧 릴리즈 브랜치의
  > 내용이 되고, 기준선을 develop 하나로 두는 이 감사 체계의 전제가 최신 구간에서는 다시 참이 된다.
  > 경량 태그 `1.1.2` 도 이번엔 develop 커밋을 가리킨다(태그가 경량이라는 성질은 OQ-P-310 ② 그대로).
  > ⚠️ **②는 답이 아니라 관행으로만 닫혔다** — 계보를 하나로 둔다는 규칙이 어디에도 안 적혀 있고,
  > 옛 release 브랜치 넷은 그대로 남아 있으며 **`feature/debug-mode` 는 여전히 release 쪽에만 있다**
  > ([로그인 디버그 모드 스펙](../superpowers/specs/2026-08-28-login-debug-mode.md)과
  > [그 계획](../superpowers/plans/2026-08-28-login-debug-mode.md)이 `draft` 로 남는 이유다).

  > 📌 **①이 이번에도 성립하지 않는다 — 한 이름에 릴리즈 브랜치가 둘이다(2026-09-11, PR #488·#490)** —
  > `origin/release/version-1.1.3-10` 은 #488 머지 커밋(`544ce434a`)이고 `origin/release/version-1.1.3-11` 은
  > `origin/develop` HEAD(`c37dc2b4c`) 그 커밋이다. 앞의 것은 develop 의 조상이라(develop 이 5커밋 앞선다)
  > **release 만 가진 커밋은 둘 다 0** 이다. 직전 회차가 "로컬에만 있고 푸시 전"이라 범위 밖에 둔 `-10` 이
  > 원격에 올라온 것이다. 뗀 트리가 develop 선 위에 있어도, 같은 이름의 판을 두 번 떼면 **어느 브랜치가
  > 배포됐는가**를 이름이 아니라 코드 번호로 가려야 한다(OQ-P-310). `feature/debug-mode` 는 여전히 develop 밖이다.

  > 📌 **원격 release 브랜치가 0개가 됐다(2026-09-11 확인)** — 옛 여섯(`0.0.3-3`·`0.1.1-5`·`1.0.0-6`·`1.1.0-7`·
  > `1.1.1-8`·`1.1.2-9`)은 2026-09-10 12:32~12:34 KST 에 지워졌고, 위 📌가 적은 `1.1.3-10`·`-11` 도 지금 원격에
  > 없다(`-10` 은 2026-09-11 00:19 KST 삭제, `-11` 의 삭제 이벤트는 조회한 이벤트 목록에 없다). 그래서 이 항목이
  > 말하는 "release 쪽"은 **이제 브랜치가 아니라 경량 태그로만 남는다.** `feature/debug-mode` 팁(`ed8e1ec8a`)은
  > 태그 `0.1.0`·`0.1.1`·`1.0.0` 에 들어 있고 develop 과 `1.1.x` 태그에는 없다 — "release 쪽에만 있다"는 사실이
  > 브랜치를 지운 뒤에도 태그를 통해 참이다. ①은 최신 구간에서 여전히 성립하지 않고, ②(계보 규칙)는 여전히 글로
  > 안 적혔다 — 브랜치를 한꺼번에 지운 일도 규칙이 아니라 정리 작업으로만 남았다.

### [2026-08-26] 토스트가 어느 프레임 위에 뜨는지를 정하는 규칙이 없다

- **ID**: OQ-P-312
- **출처**: PR #371 develop 머지 — `CustomCameraScreen`·`CustomGalleryPickerScreen`이 `toastPolicy`를
  **필수 파라미터로** 받아 자기 프레임 `Box` 안에 `YGToastHost`를 직접 심는다. 2026-08-20 PR #309가
  걷어 스캐폴드로 옮겼던 바로 그 형태로 되돌아온 것이다. 되돌린 이유는 스캐폴드 호스트가 상태바
  인셋 바로 아래에 떠서 두 화면의 헤더 행(날짜·닫기)을 **덮었기** 때문이다. 위키 [[toast]] 공통
  정책은 노출 **방향**(위→아래)만 정하고 **어느 상자의 위인지**는 정하지 않는다.
- **항목**: ① **관용구가 셋이 됐다** — (a) `YGScaffoldV2` 기본 호스트(대다수 화면), (b) `YGCanvas`의
  `overlayContent` 슬롯(C-001, OQ-P-167이 지목한 자리), (c) Screen이 정책을 받아 직접 심기(카메라·
  갤러리). 무엇을 언제 쓰는지 규칙이 없고, 세 방식의 기준선이 각각 다르다(상태바 아래 / 캔버스 위
  여백 / 화면 자기 프레임 윗변). ② **발행할 수 없는 호스트가 화면마다 하나씩 생긴다** — Route가
  `toastPolicy`를 안 넘기면 `YGScaffoldV2`가 자기 것을 만들어 호스트를 그리는데, 그 정책 객체는
  밖에서 잡을 수 없어 **아무도 발행할 수 없다.** 스캐폴드에 호스트를 끄는 파라미터가 없다.
  ③ 그래서 **"이 화면의 토스트는 어디에 뜨는가"를 Route만 보고는 알 수 없다** — Screen까지 열어야
  한다. ④ **호스트가 콘텐츠 갈래 안에만 있다** — 카메라는 `CameraContent`, 갤러리는 `GalleryContent`
  안이라 **권한 거부 갈래에는 호스트가 없다.** 지금은 두 화면 다 발행 조건이 권한 허용을 요구해
  증상이 없지만, **그 결합은 코드 어디에도 적혀 있지 않아** 발행 조건을 넓히는 순간 조용히 안 뜬다.
- **상태**: 미해결 (**동작 영향 0** — ④의 결합이 지금은 성립한다. 규칙과 방어의 부재가 문제다)
- **해소 메모**: ①을 정하면 [design-system](../architecture/design-system.md) `YGScaffoldV2` 절에
  "토스트 기준 프레임은 무엇이 정하는가"를 적고 세 관용구 중 무엇이 기본인지 명시한다. ②는
  스캐폴드에 호스트를 끄는 수단(`toastPolicy: YGToastPolicy?` 같은 형태)을 열면 함께 닫힌다.
  ④는 발행 조건과 호스트 위치의 결합을 코드 주석으로 적거나 호스트를 갈래 밖으로 올린다.
  ⚠️ **이번 라운드는 신규 테스트가 0건이고 확인 수단이 실기기 눈뿐이다** — 배치는 유닛으로 못 덮고
  CI는 계측을 컴파일만 한다(OQ-P-102 ②). 첫 커밋이 시도한 dp 복제 계산을 버린 이유도 같다.

### [2026-08-27] 마스크 해상도와 알파 임계값이 측정 없이 굳었다

- **ID**: OQ-P-313
- **출처**: PR #388 develop 머지 — `ToppingAlphaMaskCache.kt#MASK_LONG_SIDE`(256)와
  `ToppingAlphaMask.ALPHA_THRESHOLD`(128). 두 값 다 [토핑 알파 판정 스펙](../superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md)이
  "실기기 확인 뒤 조정한다"고 적은 채로 들어왔다.
- **항목**: ① 해상도 256px에서 판정과 외형의 어긋남이 손가락으로 느껴지는지 — 오차 상한이
  `그림 긴 변 ÷ 마스크 긴 변`이라 큰 토핑일수록 커진다. ② 임계값 절반이 부드럽게 깎인 누끼의
  가장자리 한 겹을 못 누르게 만드는지. ③ 얇거나 작은 토핑이 여유분 없는 실루엣 판정에서
  실제로 눌리는지 — 스펙이 "여유분 없음"을 결정 사항으로 두고 최소 터치 크기 보장을 뺐다.
- **상태**: 미해결 (**확인 수단이 실기기 눈뿐이다** — 유닛 테스트는 손으로 만든 마스크를 덮을 뿐
  실제 누끼의 알파 분포를 모른다)
- **해소 메모**: 두 값은 상수 하나씩이라 판정 뒤 숫자만 고치면 된다. ③이 문제로 드러나면
  여유분이나 최소 터치 크기를 별도 라운드로 다룬다(스펙 리스크 표 첫 행).

### [2026-08-27] 토핑과 스포트라이트 딤을 스크린리더가 뭐라 읽을지 정해지지 않았다

- **ID**: OQ-P-314
- **출처**: PR #389 develop 머지 — `feature/groups/canvas/impl` `strings.xml`의
  `canvas_topping_content_description`("토핑", `TODO(접근성)` 주석 동반)·`canvas_spotlight_dismiss`
  ("토핑 강조 닫기"). 판정이 레이어로 올라가면서 토핑별 `clickable`이 사라져, 그것이 붙여 주던
  클릭 시맨틱스를 `semantics(mergeDescendants = true)`로 다시 만들어야 했고 그때 읽을 문구가
  필요해졌다.
- **항목**: ① 토핑을 무엇으로 읽어 줄지 — 지금은 열두 개가 전부 "토핑"이라 스크린리더 사용자가
  서로를 구분할 수 없다. 작성자 닉네임이 후보이지만 캔버스 메인은 이미 `placedBy` 조인이 임시다
  (OQ-P-224 잔여). ② 딤 해제 액션 문구. ③ 배경 편집의 토핑도 같은 문자열을 공유하는데 그 화면의
  탭은 선택이지 강조가 아니라, 같은 문구가 맞는지 확인되지 않았다.
- **상태**: 미해결 (임시 문자열로 머지됨 — 시맨틱스 자체는 있으므로 포커스는 잡힌다)
- **해소 메모**: 문구가 정해지면 `strings.xml` 두 항목과 `TODO(접근성)` 주석을 함께 지운다.

### [2026-08-27] 배경 편집에 테두리가 생겼는데 코너 스트로크·버튼과의 관계를 확인하지 않았다

- **ID**: OQ-P-315
- **출처**: PR #388 develop 머지 — `CanvasBGEditScreen.kt#CanvasToppingImage`가 맨 `Image`에서
  `YGToppingCutoutImage`로 바뀌어 테두리를 그린다. 선택 스트로크와 네 버튼 좌표는 여전히
  `computeToppingStrokeCorners`·`computeToppingButtonPoints`가 **그림 사각형**에서 계산한다.
- **항목**: ① 스트로크·버튼이 테두리 바깥이 아니라 그림 가장자리에 붙어 테두리를 파고드는 모양이
  되는데 그대로 둘지. ② 토핑 박스가 그림에 딱 맞아 **테두리 스탬프가 박스를 넘어가므로** 잘리는지.
  ③ 테두리가 새로 보이면서 배경 편집의 토핑이 캔버스 메인과 같아졌지만, 사용자에게는 편집 화면의
  토핑이 갑자기 커진 것으로 보인다.
- **상태**: 미해결 (**의도된 렌더링 변경이고 판정과 외형을 맞추기 위한 전제다** — 확인이 안 됐을 뿐)
- **해소 메모**: ①②는 실기기 눈으로 판정한다. 여백을 주기로 하면 `ToppingGeometry`의
  `STROKE_MARGIN_*`이 테두리 두께를 함께 받아야 한다.

### [2026-08-27] 그룹 목록 토핑만 누끼 판정·테두리 밖에 남았다

- **ID**: OQ-P-316
- **출처**: PR #389 develop 머지 — 알파 판정과 테두리 렌더가 캔버스 메인·배경 편집 두 화면에만
  적용됐다. G-001은 `GET /api/parfait-groups`의 `MyParfaitGroupResponse`에 테두리 필드가 없어
  앱이 만들어 낼 수 없고, `YGToppingGroup`이 `ContentScale.Crop`으로 그려 실루엣이 사각으로 잘린다.
- **항목**: ① 서버가 목록 응답에 테두리 필드를 줄지. ② 주더라도 `Crop`을 `Fit`으로 바꿔야
  실루엣이 살아나는데 그러면 지금 배치가 달라진다. ③ 템플릿 6종과 조회 실패 그래픽에도 테두리를
  두를지는 정책이 비어 있다. ④ 판정까지 옮길지 — 목록 토핑은 클릭 경로 자체가 아직 없다(OQ-P-099).
- **상태**: 부분 해소 (**②가 ① 없이 먼저 일어났다** — 2026-08-27 PR #396 / **①은 서버 쪽만 닫혔다** —
  2026-09-11 서버 `82e6edc` / **앱 데이터 계층이 받는다** — 2026-09-11 PR #496 / **렌더가 닫혔다** —
  2026-09-16 PR #497 / **③④ 잔존**)
- **해소 메모**: ①이 먼저다. 서버 계약이 바뀌면 [api/parfait.md](../api/parfait.md)와
  [design-system](../architecture/design-system.md)의 `YGToppingGroup` 서술을 함께 고친다.
  > ✅ **②만 떼어서 먼저 갔다(2026-08-27, PR #396)** — `YGToppingGroup`의 `Remote` 갈래가
  > `ContentScale.Crop`에서 `Fit`으로 바뀌어 누끼 실루엣이 살아났다. 이 항목은 ②를 "①을 받고 나서
  > 할 일"로 묶어 두었는데, **테두리와 무관하게 그 자체로 버그였다** — `Crop`이 짧은 변을 96dp
  > 프레임에 맞추느라 긴 변을 잘라 비정사각 누끼의 피사체가 사라지고 있었다.
  > ②가 걱정한 "그러면 지금 배치가 달라진다"는 실제로는 문제가 되지 않았다 — 프레임 크기와
  > 회전·오프셋은 그대로이고, `clip(RectangleShape)`은 남아 이미지가 인접 셀을 덮는 것을 계속 막는다
  > (역할이 "비정사각을 프레임에 가둔다"에서 "넘치는 픽셀을 막는 방어선"으로 바뀌었을 뿐이다).
  > **①③④는 그대로다** — 목록 응답에는 여전히 테두리 필드가 없고, 템플릿·조회 실패 그래픽의
  > 테두리 정책도 비어 있으며, 목록 토핑에는 알파 판정도 클릭 경로도 없다.
  >
  > ✅ **①이 서버에서 답을 얻었다(2026-09-11, 서버 `82e6edc` PR #139)** — `GET /api/parfait-groups`
  > 응답에 `recentImageBorderType`·`recentImageBorderColor`·`recentImageBorderWidth`(모두 널 허용)가
  > 붙었다. 세 필드는 `recentImageUrl`과 **같은 오늘 캔버스 토핑**을 가리키므로 오늘 캔버스가 비면 함께
  > `null`이고, 색·두께는 `SOLID`일 때만 값이 보장된다([api/parfait-group.md](../api/parfait-group.md)).
  > **남은 것은 앱이 읽는 일이다** — `MyParfaitGroupResponse`·`MyParfaitGroupVO`에 필드가 없고
  > `YGToppingGroup`의 `Remote` 갈래는 테두리 없이 그린다. 읽을 때는 캔버스 매퍼
  > (`data/source/parfait/mapper/VOMapper.kt`의 `toToppingBorder`)와 같은 규칙으로 `ToppingBorder`에 접고
  > 두께를 `solidClamped`로 가두는 것이 자연스럽다. ③④는 그대로다.
  >
  > ✅ **앱 쪽 절반 중 데이터 계층이 develop에 들어왔다(2026-09-11, PR #496 `1b21725ba`)** —
  > `MyParfaitGroupVO.recentImageBorder`가 생겼고, 캔버스·토핑 **두** 매퍼에 똑같이 있던 접는 규칙이 공용
  > `ToppingBorderMapper.kt#toToppingBorder` 하나로 모였으며 그룹 매퍼가 세 번째 호출부가 됐다. 그래서 두께
  > 클램프도 목록까지 따라왔다(OQ-P-381). 🔧 로컬 커밋 단계에 적은 이 자리의 📌는 "세 매퍼에 똑같이 있던"이라
  > 했는데 사본은 둘이었다(커밋 `76eef8a40` 제목이 "두 벌"이다) — 79회차 기준선 점검에서 머지 코드와 대조해 고쳤다.
  > **`YGToppingGroup` 렌더는 별도 티켓이 맡는다** — 이 항목은 그 티켓이 들어올 때까지 열어 둔다.
  >
  > ✅ **렌더가 들어왔다(2026-09-16, PR #497 `a1fc2377f`, 티켓 #494)** — `YGToppingImage.Remote`가
  > `border: YGToppingBorder?`를 싣고 `Remote` 갈래가 `YGToppingCutoutImage`로 그린다. 테두리까지 96dp
  > 프레임에 넣으려고 **알맹이를 굵기만큼 줄이고**, 거리판은 feature(`GroupListContent`)가
  > `rememberToppingOutlines`로 불러 넘긴다([design-system](../architecture/design-system.md) ·
  > [g001-group-list-topping-border 스펙](../superpowers/specs/archive/2026-09-11-g001-group-list-topping-border.md)).
  > **③④는 그대로 열려 있다** — 템플릿 6종·조회 실패 그래픽의 테두리 정책은 여전히 비어 있고, 목록
  > 토핑의 알파 판정·토핑 단위 클릭도 없다(카드 전체가 클릭 범위다). 이 항목은 ③④ 때문에 열어 둔다.

### [2026-08-27] 알파 마스크 캐시가 프로세스 전역인데 비우는 호출부가 0건이다

- **ID**: OQ-P-317
- **출처**: PR #388 develop 머지 — `ToppingAlphaMaskCache.kt`의 `maskCache`(최상위 `private val`
  `LinkedHashMap`)·`inFlightMasks`·`maskLoadScope`(`SupervisorJob` + `Dispatchers.Default`)가 모두
  파일 최상위 상태다. `clearToppingAlphaMasks()`는 열려 있지만 부르는 곳이 없고, 코드 주석이
  "항목 수에 상한이 있어 누수가 아니라서 호출부 신설을 미뤘다"고 적는다.
- **항목**: ① 화면이 죽어도 로드가 끝까지 가는 것은 **의도**(합류한 쪽이 같이 죽지 않게)인데,
  아무도 안 기다리는 로드를 취소할 수단이 없다. ② 캐시가 전역이라 유닛·계측 테스트 사이에
  상태가 넘어간다 — 지금 판정 테스트는 손으로 만든 마스크만 써서 닿지 않지만, 캐시를 태우는
  테스트를 쓰는 순간 격리가 깨진다. ③ 메모리 압박 신호(`onTrimMemory`)에 붙일지.
- **상태**: 미해결 (**동작 영향 0** — 상한 64로 메모리는 묶여 있다. 수명 주체가 없는 것이 문제다)
- **해소 메모**: ②가 먼저 걸린다. 테스트가 붙는 시점에 `@After`에서 `clearToppingAlphaMasks()`를
  부르거나, 캐시를 최상위 상태가 아니라 주입되는 홀더로 바꾼다.
- ⚠️ **항목당 크기가 커진다(PR #464 develop 머지,
  [ADR-0030](../adr/0030-topping-outline-distance-field.md))** — 캐시가 `core:ui`(`ToppingOutlineCache`,
  전역 상태 셋을 `object` 로 묶었다)로 옮겨 가며 비트셋 대신 거리 배열(`ShortArray`, 1/8 필드픽셀 양자화)을
  담는다. 항목당 크기가 약 8KB에서 약 128KB로 커진다. **상한 64칸은 그대로**라 누수 성질은
  바뀌지 않지만, 수명 주체가 없는 채로 총량 상한만 8배가 되므로 압박 상황의 체감이 달라질 수 있다.
  ⚠️ **같은 성질의 캐시가 하나 더 생겼다(2026-09-07, 같은 브랜치)** — 띠 판이 컴포저블 `remember`
  수명이라 화면 전환·Spotlight 전환마다 사라져 테두리가 깜빡였고, 그래서
  `core:designsystem` 의 `ToppingBorderPlateCache`(LRU 32칸)가 판을 컴포지션 밖에 남긴다. 항목이
  알맹이 + 사방 굵기라 **굵기에 상한이 없는 한 총량에도 상한이 없고**, 비우는 `clear()` 를 부르는
  곳도 없다. ②(테스트 격리)가 걸리는 표면이 둘로 늘었다.
  ⚠️ **그 캐시의 총 장수 상한이 세 배가 됐다(2026-09-16, PR #497)** — `ToppingBorderPlateCache` 가 열쇠당
  판 한 장이 아니라 **크기별로 최대 3장**을 두는 선반이 됐다(G-001 목록과 캔버스가 같은 토핑을 크게 다른
  크기로 그려 서로의 판을 덮어썼다 — [ADR-0030](../adr/0030-topping-outline-distance-field.md) 「위험·방어」).
  열쇠 상한 32칸은 그대로라 누수 성질은 바뀌지 않지만, 수명 주체가 없는 채로 총량 상한만 다시 커진다.
  ✅ **②에 첫 대응이 들어왔다(같은 라운드)** — 계측 테스트 `ToppingBorderPlateCacheTest` 가 `@Before` 에서
  `clear()` 를 부른다. 프로세스 전역 캐시를 태우는 첫 테스트라 격리를 손으로 세운 것이고, `ToppingOutlineCache`
  쪽에는 아직 그런 호출부가 없다. ①③과 수명 주체 부재는 그대로다.

### [2026-08-27] 알파 커널에 확인 없이 오래 도는 루프가 남아 있다

- **ID**: OQ-P-318
- **출처**: [커널 취소 확인 전환 스펙](../superpowers/specs/archive/2026-08-27-alpha-kernel-suspend-cancellation.md)
  「미결」 — 그 스펙이 확인 지점·빈도 변경을 범위 밖에 두고 사실만 남겼는데, 스펙이 아카이브로
  가면서 추적 주체가 사라진다.
- **항목**: ① `AlphaComponents.kt#applyAreaOpening` 은 `countRuns` 와 `fillRuns` 로 마스크 전체를
  **두 번** 훑은 뒤에야 첫 확인(`unionAdjacentRows`)에 닿는다. ② 같은 함수의 union 이후 성분
  집계·마스크 소거 루프에도 확인이 없다. ③ `AlphaRefine.kt` 의 `downscale` 마지막 나눗셈 루프와
  `guidedCoefficients` 의 배열 생성 람다도 같다. 큰 판에서는 사용자가 뒤로 나간 뒤에도 전체 두
  패스가 그대로 지나간다.
- **상태**: 미해결 (**전환 자체는 이 성질을 바꾸지 않았다** — 콜백 시절에도 같은 자리가 비어 있었다)
- **해소 메모**: 붙들리는 시간이 실제로 문제가 되는지는 정련 소요 시간 로그(OQ-P-299)와 같은
  근거로 판정된다. 넣는다면 `suspend` 전염이 이미 끝나 있어 추가 배관 없이 `job.ensureActive()`
  한 줄씩이다.

### [2026-08-27] 취소 확인 방식의 성능 차이를 잴 하니스가 없다

- **ID**: OQ-P-319
- **출처**: [커널 취소 확인 전환 스펙](../superpowers/specs/archive/2026-08-27-alpha-kernel-suspend-cancellation.md)
  「미결」·「배제한 대안」 — `yield()` 를 배제한 근거가 측정인데, 그 측정이 통상적인 프로파일링으로
  재현되지 않는다.
- **항목**: ① 콜백과 `suspend` + `ensureActive()` 의 차이가 프로파일러 해상도 아래라 실기기에서
  확인할 방법이 지금 없다. ② 재려면 마이크로벤치마크 하니스를 세워야 하고, 이 크기의 차이를 위해
  그 비용을 치를지는 정하지 않았다. ③ 그래서 `yield()` 배제 결정은 **다시 검증할 수단이 없는
  상태로** 굳었다.
- **상태**: 미해결 (**동작 영향 0** — 판단 근거의 재현성 문제다)
- **해소 메모**: 하니스를 세운다면 그 자체가 별도 승인 사항이다(테스트 스캐폴딩 신설). 세우지
  않기로 하면 이 항목을 닫고 스펙의 결정을 그대로 둔다.

### [2026-08-27] 캔버스 폴링 단계·상한이 실측 전 값이고, 조정 트리거·실측 주체가 없다

- **ID**: OQ-P-320
- **출처**: [캔버스 오늘 SSoT·폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md) 「폴링을
  어디에 두는가」·「주의」 × [canvas-adaptive-polling 스펙](../superpowers/specs/archive/2026-09-10-canvas-adaptive-polling.md) ×
  [ADR-0029](../adr/0029-canvas-today-ssot-polling.md) — **값을 정하는 방식은 이제 정해졌다**(조회
  결과가 캐시와 같으면 한 칸씩 성기게, 다르면 가장 촘촘한 단계로). 다만 그 방식이 쓰는 단계 값과
  상한 자체는 어떤 지표를 보고 정한 것이 아니다.
- **항목**: ① 그 방식이 고른 단계·상한의 근거가 실측이 아니라 응답 크기·체감 지연 추정이다 — 한
  그룹에 몰리는 요청은 그때 캔버스를 보는 사람 수(정원 상한 12명)와 현재 단계에 반비례하는데,
  실사용에서 동시 접속자 수와 조회 결과가 캐시와 같게 나오는 빈도가 얼마나 되는지 모른다. ②
  조정 근거가 될 지표(서버 응답 시간·에러율·모바일 데이터 사용량)를 누가 어디서 보는지 정해지지
  않았다. ③ 화면별로 주기를 달리할지(배경 편집은 편집 중이라 더 길어도 되는지)도 열려 있다 —
  지금 단계는 화면이 아니라 그룹 단위로만 갈린다.
- **항목 추가**: ④ `BaseViewModel`의 구독 정지 유예(`SUBSCRIPTION_STOP_TIMEOUT`)는 여전히 5초
  고정이다. 폴링 주기가 단계형으로 바뀌면서 "폴링 주기와 같은 값"이라는 이전의 우연은 깨졌지만,
  유예 값 자체의 근거는 여전히 없다.
- **상태**: 미해결 (**값을 정하는 방식은 구현됨** — 2026-09-10, `CanvasPollInterval`이 그룹별
  단계를 들고 `CanvasPoller`가 대기 직전마다 그것에 묻는 방식으로 상수 하나를 대체했다. 다만
  그 단계 값·상한과, 조정 근거로 쓸 실사용 지표·주체는 여전히 없다)
- **해소 메모**: 실서버 부하와 캐시-일치 빈도를 실측해 단계·상한을 다시 잰다. 바꾸면
  [canvas-adaptive-polling 스펙](../superpowers/specs/archive/2026-09-10-canvas-adaptive-polling.md)과 ADR-0029의
  「주기를 되돌리는 계기」 표를 함께 고친다. 코드 쪽 상수는 `CanvasPollInterval`과
  `BaseViewModel`의 `SUBSCRIPTION_STOP_TIMEOUT` 두 곳이다.

### [2026-08-27] 캔버스 캐시에서도 낡은 응답이 뒤늦게 도착해 과거로 되돌릴 수 있다

- **ID**: OQ-P-321
- **출처**: [캔버스 오늘 SSoT·폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  「폴링을 어디에 두는가」·「주의」 — 폴러는 진행 중인 갱신이 있으면 그 주기를 건너뛰지만, 그 가드는
  **폴러를 통과하는 요청끼리만** 막는다. OQ-P-219가 그룹 목록에서 지적한 것과 같은 기전이 캔버스
  캐시에서 나타난 형태다.
- **항목**: ① 폴러 밖에서 나가는 경로와 겹치는 창을 어떻게 닫을지. ② 세대 카운터로 낡은 응답의
  쓰기를 버릴지. ③ 배경 편집의 dirty 집합·툼스톤은 **화면 상태 층**의 방어라 저장소 캐시가 과거로
  돌아가는 것 자체는 못 막는다 — 캐시 층 방어를 따로 둘지.
- **상태**: 부분 해소 (**①②는 구현이 답했다**, ③ 잔존 — 2026-08-31 PR #404 develop 머지)
- **해소 메모**: OQ-P-219와 같은 결정으로 함께 닫는 것이 맞다. 정하면 ADR-0023·ADR-0029의 "갱신
  시점"과 두 스펙의 갱신 규칙 표에 함께 반영한다.

  > ✅ **①②가 닫혔다(2026-08-31, PR #404)** — ①이 걱정한 "폴러 밖에서 나가는 경로"가 아예 없다.
  > 캐시에 쓰는 곳은 `CanvasPoller.refresh` 하나뿐이고 `ParfaitRepositoryImpl` 의 갱신 두 표면도
  > 폴러를 지난다. ②는 세대 카운터로 들어왔다 — `stopAll()` 이 세대를 올려 **그 전에 출발한 응답은
  > 캐시에 싣지 않는다.** 겸해 그룹별 "진행 중" 표를 두어 갱신이 겹쳐 나가지 않으므로, 한 그룹
  > 안에서 응답이 뒤엉킬 창 자체가 생기지 않는다.
  > ⚠️ **③은 그대로다** — 세대 카운터가 막는 것은 **세션 종료를 걸친 응답**뿐이라, 세션 안에서
  > 캐시가 과거로 돌아가는 일반적인 경우를 위한 캐시 층 방어는 여전히 없다.

### [2026-08-27] positionZ 를 앱이 정하는 한 동시 배치의 깊이 겹침은 닫히지 않는다

- **ID**: OQ-P-322
- **출처**: [캔버스 오늘 SSoT·폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  「토핑 배치 화면의 positionZ」 × [ADR-0026](../adr/0026-topping-draft-datastore-ssot.md) —
  확정 시점 재계산은 **완화이지 해결이 아니다**. 재계산이 읽는 값은 최대 폴링 주기만큼 낡아, 두
  사람이 그 안에 확인을 누르면 같은 최대 깊이를 읽는다.
- **항목**: ① 서버가 `positionZ` 를 배정하게 할지(서버 계약 변경). ② 겹쳤을 때의 정렬 동률
  타이브레이크를 앱이 정할지(생성 시각? 이미지 id?). ③ 하루 경계에서 구독 캔버스와 초안
  `parfaitId` 가 갈리면 초안 값으로 물러서는데, 그때 실리는 z 가 그 캔버스 기준으로 낡았을 수 있다.
- **상태**: 미해결 (**구현됨** — 2026-08-31 PR #404 develop 머지로 확정 시점 재계산이 들어왔다.
  겹쳐도 거절되지는 않고 그리는 순서만 흔들리는 것은 그대로다)
- **해소 메모**: ①은 서버 작업이라 [api/parfait-image.md](../api/parfait-image.md) 계약 변경과 함께
  간다. 그 전까지는 완화 상태로 두고 스펙의 서술을 그대로 유지한다.

### [2026-08-27] 하루 경계 직후 오늘 조회가 한 그룹에서 동시에 나가 캔버스를 중복 생성할 수 있다

- **ID**: OQ-P-323
- **출처**: [캔버스 오늘 SSoT·폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)
  「폴링을 어디에 두는가」·「하루 경계」 × [api/parfait.md](../api/parfait.md) — 오늘 조회는 해당
  날짜 파르페가 없으면 만들어 저장한다. 경계 티커가 발화하면 그 그룹에서 캔버스를 보고 있는
  클라이언트가 각자 한 번씩 오늘 조회를 태운다.
- **항목**: ① 서버가 그룹·날짜 조합에 유니크 제약을 두어 중복 생성을 막는지 확인하지 못했다.
  막지 않으면 중복 행이고, 막으면 일부 클라이언트가 오류를 받는다. ② 앱 쪽에서 경계 전환 조회에
  무작위 지연을 줄지. ③ 폴링이 백그라운드에서 멎으므로 동시 클라이언트 수는 "그 순간 실제로
  보고 있는 사람"으로 줄지만 0은 아니다.
- **상태**: 미해결 (**구현됨** — 2026-08-31 PR #404 develop 머지로 경계 티커
  (`ObserveParfaitDayBoundaryUseCase`)가 들어왔다. 서버 확인은 여전히 선행 과제다)
- **해소 메모**: 서버 구현을 확인해 ①을 먼저 닫는다. 막고 있으면 앱은 그 오류를 조용히 넘기고
  다음 주기 상세 조회로 회복하면 된다. 앱 쪽 방어는 `CanvasPoller` 가 캐시의 날짜를 보고
  **오늘 조회와 상세 조회를 가르는 것** 하나뿐이라, 경계 직후 첫 요청이 오늘 조회인 것은 그대로다.

### [2026-08-28] 토핑 테두리를 그리는 겹과 서버로 보내는 겹이 서로 다르다

- **ID**: OQ-P-324
- **출처**: `CanvasBGEditScreen`(그리기)과 `CanvasBGEditViewModel.toToppingBorder`(저장), 둘 다
  develop. 앱은 테두리를 **겹 목록**(`ToppingBorderLayer`)으로 들고 서버는 **한 겹**만 받는데,
  접는 규칙이 두 자리에서 갈렸다 — 그리는 쪽은 `borderLayers.firstOrNull()`, 보내는 쪽은
  `lastOrNull()`이다. `ToppingEditViewModel`의 `borderHistory`가 `UndoRedoStack<ToppingBorderLayer>`
  라 겹은 실제로 둘 이상 쌓일 수 있다(테두리 편집 화면은 그 겹을 겹겹이 그린다).
- **항목**: ① 어느 쪽이 정본인지 — 테두리 편집 화면의 렌더링(`ToppingBorderEditScreen`)과
  `SegmentationConfirmViewModel`은 **마지막 겹을 바깥으로** 보는 쪽이라 저장 규칙이 그것과 맞고,
  캔버스 편집 화면의 렌더링만 첫 겹이다. ② 겹을 여러 장 유지하는 것 자체가 필요한지 — 서버가
  한 겹만 받으므로 되읽으면 언제나 한 겹이 되고, 여러 겹은 **한 세션 안에서만 존재한다.**
  ③ 정하면 감지선이 필요하다 — 지금 붙은 테스트가 전부 한 겹짜리 목록만 쓴다.
- **상태**: 미해결 (**증상은 세션 안에서만 보인다** — 테두리를 두 번 이상 겹쳐 두른 뒤 확인을
  누르면, 화면에 보이던 겹과 다른 겹이 저장되고 재조회 뒤 그것이 드러난다)
- **해소 메모**: ①을 "마지막 겹이 바깥"으로 통일하면 고칠 자리는 `CanvasBGEditScreen` 한 곳이다.
  반영처는 [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md)
  as-built 재정정 절과 [api/parfait-image.md](../api/parfait-image.md) Android 매핑이다.

### [2026-08-28] 토핑 배율 하한이 두 화면에서 갈렸고 편집 쪽 근거가 없다

- **ID**: OQ-P-325
- **출처**: PR #398(`[FIX] 배율 하한 수정`) — `CanvasBGEditViewModel`의 `TOPPING_MIN_SCALE`이
  0.5에서 **0.05**가 됐다. 같은 저장소의 배치 화면은 상수가 아니라 **짧은 변 48dp**
  (`CanvasToppingPlaceViewModel`의 `MIN_TOPPING_SHORT_SIDE`)에서 하한을 역산하고, 실측을 못 얻은
  경우에만 `TOPPING_MIN_SCALE_FALLBACK = 0.5`로 물러난다. 위키
  [[C-106-토핑-배치-정책-v0.1]]의 48px 최소 터치 방어가 그 역산의 근거다.
- **항목**: ① 편집 탭의 새 하한에 근거가 없다 — 커밋 메시지와 KDoc이 "배율 하한 수정"이라고만
  적는다. 무엇이 문제였는지(초기 배율이 커서 줄일 여지가 없었는지, 특정 이미지에서 걸렸는지)가
  남아 있지 않다. ② 48dp 방어를 편집 탭에도 둘지 — 지금은 토핑을 최소 터치 크기 아래로 줄일 수
  있고, 줄인 값이 그대로 PATCH로 나가 **다시 잡을 수 없는 토핑이 서버에 남는다.**
  ③ 두 화면이 같은 규칙을 봐야 하는지 — 배치 규칙 셋은 이미 `ToppingGeometry`로 올라가 공유
  중인데 하한만 갈라져 있다.
- **상태**: 미해결 (**동작 영향 있음** — 편집 탭에서만 재현되고, 실기기 확인 0회)
- **해소 메모**: ②가 참이면 처방은 `MIN_TOPPING_SHORT_SIDE` 역산을 편집 탭으로 옮기는 것이고,
  그때 `ToppingGeometry`가 그 계산을 함께 든다. 반영처는
  [c301-topping-edit-tab 스펙](../superpowers/specs/archive/2026-08-16-c301-topping-edit-tab.md) 드리프트 4다.
  > 📌 **③의 대비가 한 뼘 더 벌어졌다(2026-08-27, PR #397)** — 크기조절 환산 자체가
  > `ToppingGeometry#resizeScaleFactor`로 올라가 두 화면이 **같은 함수**를 부르게 됐는데, 그 함수는
  > 배율에 곱할 값만 돌려주고 클램프는 여전히 호출부 몫이다(편집 탭은 상수 하한, 배치 화면은 48dp
  > 역산). 공유되는 것이 넓어질수록 하한만 갈라져 있는 것이 눈에 띈다.
  > ⚠️ 그 함수는 **중심 너머까지 한 번에 끌면 0에서 멈춘다** — 뒤집힌 토핑을 만들지 않으려는
  > 방어이고, 실제 하한은 그다음 호출부의 클램프가 정한다. 즉 편집 탭의 0.05는 이제 두 겹 중
  > 바깥쪽이다.

### [2026-08-28] 선작성 문서 셋이 이번 델타를 모른다

- **ID**: OQ-P-326
- **출처**: [캔버스 오늘 SSoT·폴링 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md)·
  [PR3 계획](../superpowers/plans/archive/2026-08-27-canvas-polling.md)·
  [세그멘테이션 입력 전처리 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)은 develop 머지
  **전에** 쓰였고, 하루 뒤 PR #369·#400이 그 문서들이 전제한 코드를 바꿨다. 앞 둘은 아직
  `status: draft`, 셋째는 `in-progress`다.
- **항목**: ① **테두리 PATCH가 계획에서 사라진다** — 계획의 `updateDirtyToppings`가
  `updateToppingUseCase` 하나만 부른다. 그대로 구현하면 PR #369가 붙인 테두리 저장이 **되돌아간다.**
  스펙·계획 두 곳의 "테두리 PATCH는 아직 소비처가 없지만"이라는 서술도 이제 거짓이다.
  ② **초기 선택 시딩이 매 방출마다 되돌아간다** — `withCanvas`가 `selectedTab`·`selectedToppingId`를
  `initialToppingId`로 채우는데, 계획이 이 자리를 구독으로 바꾸면서 "최초 방출에만 시딩한다"고
  적은 목록에 이 두 필드가 없다. 사용자가 탭을 옮기거나 선택을 풀어도 다음 주기에 되돌아간다.
  ③ **정책의 C-305가 별도 화면인지 다시 물어야 한다** — OQ-P-250 ③이 "기존 화면이 받는다"로
  닫혔으므로, 위키 [[화면-ID-체계]]·[[C-202-토핑-편집자-확인-규칙-v0.1]]과 구현의 화면 경계가
  갈린 채로 남는다. 지난 캔버스에서 본인 토핑 탭이 무반응인 것도 이 갈림의 결과다.
  ④ **[세그멘테이션 입력 전처리 스펙](../superpowers/specs/2026-08-23-segmentation-preprocessing.md)의 전제도
  깨졌다** — 그 스펙은 방향 보정과 하한 확대를 `decodeUriToBitmap` 안에 두면 된다고 적는데,
  PR #369가 `decodeImage`를 스킴으로 갈라 원격 경로가 그 확장 함수를 타지 않게 됐다. 그대로
  구현하면 **서버 토핑 재편집 경로만 정규화 밖에 남는다.** 그 스펙은 아직 `in-progress`다.
  ⑤ **크기조절·회전 인텐트가 개명되고 시그니처가 바뀌었다**(2026-08-27, PR #397) —
  `OnToppingResizeDrag(Offset)`·`OnToppingRotateDrag(Offset)`가 `OnToppingResize(scaleFactor)`·
  `OnToppingRotate(deltaDegrees)`가 됐다. 픽셀 환산이 화면으로 올라간 결과이고, 스펙이
  `dirtyToppingIds`에 id를 넣는 자리로 세어 둔 그 인텐트들이다. **스펙 본문은 이번 회차에
  현행 이름으로 정정했다**(2026-08-28). 계획 쪽은 `OnToppingMoveDrag`만 쓰므로 영향이 없다.
- **상태**: 부분 해소 (**①②⑤는 2026-08-28, ⑥은 2026-08-30 스택 리베이스에서 닫혔고 넷 다
  2026-08-31 PR #404 로 develop 에 들어왔다** — 코드가 문서를 앞질러 풀었다 / ③④ 잔존)
- **해소 메모**: ①②는 계획을 고치는 일이라 다음 캔버스 폴링 라운드의 첫 작업이고, ④는 전처리
  라운드의 첫 작업이다. ③은 위키
  정책과의 대조라 [[화면-ID-체계]] 쪽 판단이 선행한다 — 구현이 옳으면 정책 문서가 따라오고,
  정책이 옳으면 화면을 가르는 라운드가 따로 필요하다.

  > ✅ **①②⑤가 닫혔다(2026-08-28, 스택 PR 3단을 `627e1867` 위로 리베이스)** — 예상했던 "다음
  > 라운드의 첫 작업"이 아니라 **리베이스 충돌을 푸는 자리에서** 닫혔다. 세 브랜치가 develop 과
  > 같은 파일을 건드려 충돌이 났고, 그것을 푸는 유일한 방법이 두 의도를 합치는 것이었다.
  > **①** `updateDirtyToppings` 가 테두리 PATCH 를 함께 낸다. 다만 계획이 적어 둔 "스냅샷 대조가
  > 사라지므로 테두리가 바뀌었는가를 따로 볼 필요가 없다"는 **틀린 예측이었다** — 축별 판정을 빼면
  > 위치만 옮긴 토핑에도 테두리 PATCH 가 따라 나가고, develop 의
  > `onClickConfirm_toppingBorderEdited_savesOnlyTheBorder` 가 그 반대 방향을 잠그고 있어 실제로
  > 깨졌다. 그래서 스냅샷은 **이름을 바꿔 남았다**(`confirmedToppings` → `serverToppings`)
  > — 목록 전체가 아니라 **집합이 이미 고른 토핑 안에서만** 축을 가린다.
  > **②** `withCanvas` 의 시딩 가드 안으로 `selectedTab`·`selectedToppingId` 가 들어갔다.
  > **⑤** 인텐트 개명(`OnToppingResize`·`OnToppingRotate`)이 스펙 본문에 반영됐고, PR #397 이
  > 전면 재작성한 배치 화면 테스트 다섯은 develop 쪽을 채택했다 — 옛
  > `OnToppingResizeDrag` 기반 테스트는 `resizeOutwardDirection` 이 사라져 컴파일되지 않는다.
  > **검증** — 세 스택 지점 각각에서 `:domain`·`:data`·`:core:ui`·`:feature:groups:canvas:impl`
  > 유닛 테스트가 통과한다. **③④는 그대로다** — ③은 위키 판단이 선행이고, ④는 전처리 스펙이
  > 아직 `in-progress` 다.
  > ⚠️ **세 브랜치는 아직 develop 에 머지되지 않았다** — 리베이스로 갱신된 것은 로컬 브랜치이고,
  > 원격에는 올라가 있지 않다.
  > ⚠️ **⑥ 첫 조회 덮개가 계획의 코드블록에서 사라진다(2026-08-30, PR #407 develop 머지)** —
  > [PR2 계획](../superpowers/plans/archive/2026-08-27-canvas-today-ssot.md)이 `loadTodayCanvas()` 를 **통째로 갈아 끼우는**
  > 코드블록을 싣는데, 그 블록에는 이번에 붙은 `isInitialLoading` 이 없다. 적힌 그대로 구현하면
  > **오늘 캔버스 첫 조회의 화면 덮개가 조용히 사라진다.** [PR3 계획](../superpowers/plans/archive/2026-08-27-canvas-polling.md)은
  > 한 걸음 더 가서 그 함수를 지우자고 적는다(구독이 대신한다). release 계보는 이 충돌을 이미
  > 한 번 풀었고 **답이 계획과 다르다** — 폴링을 받은 뒤의 release 쪽 `CanvasMainViewModel` 은
  > 덮개를 `try`/`finally` 가 아니라 **구독 안에서 파생**시킨다. 계획을 고칠 때 베낄 자리가 이미
  > 있다는 뜻이다.
  > 📌 **PR2·PR3 브랜치가 원격에 올라왔다(2026-08-30)** — `origin/feature/canvas-today-ssot` ·
  > `origin/feature/canvas-polling`(전자를 품은 스택). 다만 들어간 곳은 develop 이 아니라
  > **release 계보**다 → OQ-P-311.

  > ✅ **⑥이 닫혔다(2026-08-30, 스택 3단을 `27e85d0d` 위로 다시 리베이스)** — `feature/#392-canvas-topping`
  > · `feature/canvas-today-ssot` · `feature/canvas-polling` 을 `--update-refs` 로 함께 옮기면서
  > PR #407 의 덮개와 부딪히는 자리를 셋 다 풀었다. **답은 예상보다 한 겹 두껍다.**
  > 예상대로 덮개는 구독 안에서 파생한다 — 구독이 열릴 때 `todayCanvas` 가 `null` 이면 켜고
  > (이미 받아 둔 캔버스가 있으면 켜지 않는다 — 화면에 다시 붙을 때마다 번쩍이지 않도록),
  > 구독이 캔버스를 실어 오면 내린다. **그러나 그것만으로는 갱신 실패에서 덮개가 풀리지 않는다**
  > — 실패하면 캐시가 아무것도 방출하지 않아 화면이 로딩에 갇힌다. `try`/`finally` 가 늘 내려
  > 주던 자리를 구독은 대신하지 못한다.
  > 그래서 **폴러가 실패를 내보내는 축을 새로 냈다**: `CanvasPoller.refreshFailures` →
  > `ParfaitRepository.todayCanvasRefreshFailures` → `ObserveTodayParfaitRefreshFailureUseCase`.
  > 값은 싣지 않아 ADR-0023 의 "값을 얻는 길은 하나"는 그대로다. `stopAll()` 로 세대가 바뀐 뒤
  > 도착한 실패는 내보내지 않는다.
  > **PR3 계획이 "트리거를 잃었다"며 지우자고 적은 `ShowTodayCanvasError` 는 되살렸다** — 이펙트
  > 선언·화면 처리·문자열 리소스 셋 다. 조건만 "보여 줄 캔버스가 없을 때"에서 "덮개가 걸려
  > 있을 때"로 좁혔다. 폴링이 5초마다 도는 자리라 가드가 없으면 실패가 이어지는 동안 토스트가
  > 쌓인다.
  > **검증** — 세 브랜치 각각에서 clean 뒤 `./gradlew test ktlintCheck` 가 통과한다.
  > 반영처는 [이 라운드의 스펙](../superpowers/specs/archive/2026-08-27-canvas-today-ssot-polling.md) 머리말·「Repository」
  > as-built·「실패 표현」 as-built·「삭제」 as-built · [ADR-0029](../adr/0029-canvas-today-ssot-polling.md)
  > 「결정」·「영향」 · [PR2 계획](../superpowers/plans/archive/2026-08-27-canvas-today-ssot.md) ·
  > [PR3 계획](../superpowers/plans/archive/2026-08-27-canvas-polling.md) 머리말이다.

  > ✅ **①②⑤⑥이 develop 에서 사실이 됐다(2026-08-31, PR #404 `2c7bb31b`)** — 리베이스분이 원격에
  > 올라가지 않았다던 상태가 끝났다. 스택 3단이 한 머지로 들어왔고 **머지 커밋의 트리가 브랜치 팁
  > `6bd21fb7` 과 같아** 위 리베이스 서술을 재측정 없이 develop 사실로 읽어도 된다. 스펙은
  > `implemented`, 계획 셋은 `done` 으로 아카이브됐고 ADR-0029 는 `accepted` 가 됐다.
  > **③④는 그대로다** — ③은 위키 판단이 선행이고, ④는 전처리 스펙이 아직 `in-progress` 다.

### [2026-08-28] 원격 이미지 다운로드가 응답 본문을 통째로 힙에 올린다

- **ID**: OQ-P-327
- **출처**: `RemoteImageDownloadDataSourceImpl.download`(PR #369) — `response.body?.bytes()`로
  전체를 읽어 `ByteArray`로 돌려주고, `ImageSegmentationRepositoryImpl.decodeImage`가 그것을
  `BitmapFactory.decodeByteArray`에 넘긴다. 같은 저장소의 이웃인 `PresignedUploadDataSource`는
  반대로 **스트리밍 `RequestBody`**를 써서 바이트를 힙에 통째로 올리지 않는 것이 명시된 결정이다.
- **항목**: ① 상한이 없다 — 서버가 주는 토핑 이미지 크기의 실측이 0건이고,
  `Content-Length` 확인도 없다. ② 디코드 직후 원본 `ByteArray`와 비트맵이 **동시에** 살아 있다
  (누끼 파이프라인의 피크 메모리를 재지 않은 것은 OQ-P-320과 같은 계열이다).
  ③ 파일로 떨군 뒤 경로로 넘기는 방식(`ImageFileLocalDataSource`가 이미 하는 일)과 어느 쪽이
  맞는지 정해진 적이 없다.
- **상태**: 미해결 (**실물 크기를 모른다** — 서버 이미지가 작으면 무해하고, 크면 이 경로가 먼저
  터진다)
- **해소 메모**: ①의 실측이 먼저다. 정하면 반영처는
  [architecture/data-layer](../architecture/data-layer.md) "원격(raw HTTP)" 절이다.

### [2026-08-28] 안내 토스트를 기다리는 시간이 토스트 정책과 별개 상수로 굳었다

- **ID**: OQ-P-328
- **출처**: `GroupNickNameViewModel#noticeNicknameNotApplied`(PR #394) — 참여는 끝났고 닉네임
  `PATCH`만 실패한 경우를 `ShowError(NICKNAME_NOT_APPLIED)`로 알린 뒤, `NICKNAME_NOTICE_DURATION`
  만큼 **이동을 미룬다.** 토스트 호스트가 이 화면의 `YGScaffoldV2`에 매여 있어 곧바로 넘기면 안내가
  뜨자마자 함께 사라지기 때문이다. 그런데 그 상수는 `YGToastPolicy`가 정하는 노출 시간과 **별개로
  선언된 값**이고, KDoc이 "맞춘다"고 적는 것으로 동기화를 대신한다.
- **항목**: ① 대기 시간을 토스트 정책에서 읽어 오게 할지 — 그러면 ViewModel이 디자인시스템 정책
  타입을 알게 되고, 지금까지 State가 표시 타입을 안 드는 쪽으로 지켜 온 경계와 부딪힌다.
  ② 아니면 이동을 늦추는 대신 토스트가 화면 전환을 넘어 살아남게 할지 — 위키 [[toast]] 공통 정책은
  노출·닫기·스택만 규정하고 **화면이 바뀔 때 어떻게 되는지는 말하지 않는다.** ③ 기다리는 동안
  `isEntering`을 켠 채 두는 것이 맞는지 — 참여는 이미 끝났으므로 진행 표시가 사실과 다르다
  (코드가 적은 근거는 "여기서 다시 누르면 이미 참여한 그룹이라는 실패만 돌아온다"이고,
  그것은 재진입 방지이지 진행 표시가 아니다).
- **상태**: 미해결 (**동작은 의도대로** — 두 상수가 갈리는 날에만 어긋난다)
- **해소 메모**: ①을 고르면 반영처는 [state-management](../architecture/state-management.md)
  "서버 실패 갈래" 절이고, ②는 [design-system](../architecture/design-system.md)의 `YGToast`·
  `YGScaffoldV2` 서술과 함께 본다. 토스트 호스트가 화면마다 갈리는 문제는 OQ-P-312가 따로 쥔다.

### [2026-08-30] 토스트 교체 태그가 화면 소유 문자열이라 서로를 지울 수 있다

- **ID**: OQ-P-329
- **출처**: `YGToastPolicy#show(type, replaceTag)`·`CanvasMainRoute#SPOTLIGHT_TOAST_TAG`(PR #405) —
  같은 태그를 단 토스트를 걷어내고 새것만 남기는 갈래가 생겼다. 태그는 **호출 화면이 정하는 평문
  문자열**이고 정책 홀더는 그 값의 의미를 모른다. 지금 태그를 주는 곳은 캔버스 Spotlight 하나뿐이라
  충돌이 없다.
- **항목**: ① **같은 문자열을 고른 두 발행자는 서로의 토스트를 지운다** — 정책 객체가 화면 단위로
  살아 있어 지금은 한 화면 안의 문제지만, 스캐폴드가 기본 정책을 만들어 주는 화면이 늘면 그 경계가
  화면과 꼭 같지 않다(OQ-P-312 ②). 태그를 상수 한곳에 모을지, 타입 있는 키로 올릴지.
  ② **"쌓인다"와 "교체된다"를 무엇으로 가르는지가 코드에만 있다** — 위키 [[Toast-공통-정책]]은
  스택만 규정하고 교체를 말하지 않는다. Spotlight 가 교체여야 하는 근거는 "같은 자리에 포개져
  못 읽는다"는 구현 사실이지 정책 문안이 아니다. ③ **감지선이 계측에만 있다** —
  `YGToastHostTest` 3건이 이 규칙을 잠그는데 **CI 는 계측을 컴파일만 한다**(OQ-P-102 ②).
- **상태**: 미해결 (**동작 영향 0** — 태그를 주는 호출자가 하나뿐이다. 두 번째가 생기는 날의 문제다)
- **해소 메모**: ①을 정하면 [design-system](../architecture/design-system.md) `YGAlert`/`YGToast` 절과
  [ygtoast 스펙](../superpowers/specs/archive/2026-07-23-ygtoast.md) 노출 정책 절에 태그의 소유자를 적는다.
  ②는 위키 [[Toast-공통-정책]] 갱신이 선행이다 — 정책이 교체를 인정하면 그때 컴포넌트 계약으로 내린다.

### [2026-08-30] 첫 조회 덮개를 켜는 판정이 화면마다 복제됐고 정책 근거가 없다

- **ID**: OQ-P-330
- **출처**: `GroupListViewModel#isInitialLoad`·`CanvasMainViewModel#loadTodayCanvas`(PR #407) —
  두 화면이 각자 `isInitialLoading` 필드를 두고 각자 조건을 적는다(`groupList == null` + 당김 제외 /
  `todayCanvas == null`). 켜고 내리는 자리를 `launch` 블록 안으로 넣어야 한다는 것도 두 곳에 각각
  주석으로만 적혀 있다 — 코루틴 키 가드에 막히면 블록이 안 돌아 `finally` 가 따라오지 않기 때문이다.
- **항목**: ① 같은 판정을 세 번째 화면이 또 적을 것인가 — `BaseViewModel` 이나 스캐폴드 쪽으로
  올릴 자리가 있는지(올리면 "무엇이 첫 조회인가"를 공통이 알아야 한다). ② **당겨서 새로고침을 빼는
  규칙이 그룹 목록에만 있다** — 캔버스에는 당김이 없어 지금은 갈리지 않지만, 생기는 순간 같은 판단을
  다시 해야 한다. ③ **정책 근거가 없다** — 위키 [[무한-파르페-그리드]]는 초기 로딩을 "인디케이터·
  스켈레톤 대신 제작한 자체 그래픽"으로 적는데, 들어온 것은 **전 화면 공통 오버레이**(딤 +
  `YGLoadingLottie`)다. 캔버스 쪽은 위키에 초기 로딩 조항 자체가 없다.
- **상태**: 미해결 (**동작은 의도대로** — 복제와 근거 부재의 문제다)
  > 📌 **①에 자리가 하나 열렸다(2026-09-04, PR #440 develop 머지)** — `YGScaffoldV2`에
  > `loadingOverlay` 슬롯이 붙어 **무엇을 덮을지는 화면이 정할 수 있게 됐다.** C-001이 그 슬롯으로
  > 로딩 덮개와 실패 덮개를 갈아 끼운다. 다만 ①이 물은 것은 **"무엇이 첫 조회인가"의 판정을 어디로
  > 올리는가**이고 **그 답은 아직 없다** — 판정은 여전히 화면마다 적히고, 캔버스 쪽은 오히려 축이
  > 하나 늘었다(`isInitialLoading`에 더해 이미지 결말을 접은 `CanvasLoadState`가 붙어
  > `isLoading = isInitialLoading || loadState != Loaded`가 됐다).
  > ③도 갈래가 하나 늘었다 — **화면 전용 로띠가 처음 생겼는데 G-001이 아니라 C-001에 붙었다**
  > (`YGLoadingArt.Topping`). 위키가 전용 그래픽을 요구한 쪽은 여전히 공통 애셋을 쓴다(OQ-P-113 ②).
  > ②(당김을 빼는 규칙이 그룹 목록에만 있다)는 그대로다 — 캔버스에는 아직 당김이 없다.
- **해소 메모**: ①②는 [state-management](../architecture/state-management.md) 에 "첫 조회 표현"
  절을 두는 일이고 OQ-P-205 ②(오버레이를 켜는 기준의 규약 승격)와 같은 자리에서 정한다.
  ③은 위키 판단이 선행이다 — 공통 오버레이로 갈음할 것이면 [[무한-파르페-그리드]] 문안을 고쳐야 하고,
  전용 그래픽을 유지할 것이면 G-001 만 다른 표현을 갖는다.

### [2026-08-30] 갤러리 빈 상태에만 제목이 없는 근거가 작업자 지시뿐이다

- **ID**: OQ-P-331
- **출처**: `CustomGalleryPickerScreen#GalleryContent`(PR #406) — 사진이 있으면
  `YGFloatingBarTitle`("오늘 찍은 사진"), `isEmpty` 면 제목 없는 `YGFloatingBarClose` 다.
  Figma 는 `Floating Bar` 에 `Status=Title` 을 추가했을 뿐 **어느 상태에 무엇을 쓰는지**를 주지 않았고,
  스펙이 이 갈래의 출처로 적은 것은 작업자 지시 한 줄이다. 권한 미허용 갈래도 제목이 없다.
- **항목**: ① 빈 상태에 제목이 없어야 하는가 — 같은 화면이 상태에 따라 머리글을 잃는 것이라
  스크린리더에는 화면 이름이 사라지는 것과 같다. ② 문구 "오늘 찍은 사진"이 화면 이름인지 목록
  머리글인지 — 위키에 C-102 문구 정책이 없다(빈 상태 안내문은 있고 제목은 없다).
- **상태**: 미해결 (**동작 영향 0** — 육안 대조도 아직 없다. 이 라운드 검증은 기계 검사뿐이었다)
- **해소 메모**: 정해지면 [c102 스펙](../superpowers/specs/archive/2026-08-04-c102-custom-gallery-picker.md) 상단바
  개정 블록의 표에 근거를 적고, 문구가 정책이 되면 위키 수집 대상으로 올린다.

### [2026-08-31] 최근 목록 정원을 종류별로 가르며 총 상한이 두 배가 됐는데 잰 사람이 없다

- **ID**: OQ-P-332
- **출처**: `RecentImageRepositoryImpl`(PR #408 develop 머지) — `MAX_SIZE` 가
  `MAX_SIZE_PER_KIND` 가 됐다. 값은 그대로지만 **적용 단위가 목록 전체에서 종류별로 바뀌어**
  원본과 알맹이가 각자 그만큼을 든다. OQ-P-258 ①이 "상한을 올리면 내부 저장소 사용량이 함께
  는다"며 물렀던 그 비용이, ②를 고르면서 같은 크기로 따라왔다.
- **항목**: ① 같은 날 토핑을 여러 개 만드는 실사용에서 내부 저장소가 실제로 얼마나 차는지
  재지 않았다 — 알맹이는 PNG 이고(OQ-P-003 ③) 원본은 카메라 캡처 원본이라 한 장의 무게가 다르다.
  ② 축출은 여전히 개수 기준이라 **용량 기준 상한이 없다.** ③ 데이 윈도우 정리(03:00 밖이면 삭제)가
  매일 비우므로 누적은 안 되지만, 하루 안 최대치는 두 배가 된 채다.
- **상태**: 미해결 (**동작은 의도대로** — 재지 않은 비용의 문제다)
- **해소 메모**: ①을 재고 나서야 정원 값을 다시 정할 수 있다. 바꾸면
  [data-layer](../architecture/data-layer.md) 「예: 최근 이미지」와 OQ-P-258 해소 메모를 함께 고친다.

### [2026-08-31] 과거 캔버스 목록의 `status`를 앱이 받지 않고 빈 날을 따로 추론한다

- **ID**: OQ-P-333
- **출처**: 서버 `00622cb`(`feat: 캔버스 리스트 조회 응답에 status 추가`)가 `PastParfaitResponse`에
  `status`(`ACTIVE`·`CLOSED`·`EMPTY`)를 붙였는데, 앱 `PastParfaitResponse`(`data/service/model/response/parfait/`)에
  대응 필드가 없다. `ignoreUnknownKeys = true`라 파싱은 안 깨진다. 한편 `PastCanvasVO.isEmpty`가
  `toppingCount == 0`으로 "빈 날"을 스스로 판정하고, C-201 캘린더가 점을 찍는 기준이 그 값이다.
- **항목**: ① 두 판정이 같은 것을 뜻하지 않는다 — 서버 `EMPTY`는 **토핑 0건으로 마감된 날**이고,
  앱 판정에는 **진행 중인 오늘 캔버스**도 걸린다(`ACTIVE` + `imageCount == 0`). ② 앱이 `status`를
  받아 `PastCanvasVO`에 올릴지, 지금처럼 개수로만 판정할지. ③ 올린다면 `today`·상세가 이미 쓰는
  `CanvasStatus`(미지 값 폴백 `UNKNOWN`)를 그대로 재사용할지.
- **상태**: 부분 해소 (②③ 해소, ① 결정으로 종결 — 아래 참고)
  > ✅ **②③ 해소, ①은 결정됐다(2026-08-31 브랜치 작업 → 2026-09-01 develop 머지, PR #428)** —
  > `status`를 `PastParfaitResponse`·`PastCanvasVO`까지 올렸다. 매퍼는 `today`·상세가 이미 쓰는
  > `toCanvasStatus()`를 그대로 재사용해 미지 값 폴백(`CanvasStatus.UNKNOWN`)도 같이 따라왔다
  > (②③). **달력 점 기준은 개수로 유지한다** — 위키 [[C-201-캘린더-정책-v0.1]]이 "토핑 1개 이상 =
  > True"로 규정하고 지금 `PastCanvasVO.isEmpty`(토핑 개수)가 그 정본과 일치한다. 서버 `EMPTY`
  > (0건으로 마감된 날)로 옮기면 진행 중인 오늘의 빈 캔버스에 점이 찍힌다. **남는 것은 `status`를
  > 읽는 화면이 아직 0건이라는 사실뿐이다** → [api/parfait.md](../api/parfait.md) Android 매핑.
- **해소 메모**: 화면 소비처가 생기면 그때 [c201-canvas-calendar-server
  스펙](../superpowers/specs/archive/2026-08-17-c201-canvas-calendar-server.md)과 [api/parfait.md](../api/parfait.md)를
  다시 본다.

### [2026-08-31] 토핑 일괄 수정 API가 생겼는데 앱은 단건을 병렬로 N번 부른다

- **ID**: OQ-P-334
- **출처**: 서버 `79a6d35`(`feat: 토핑 여러 개를 한 번에 수정하는 배치 API 추가`, PR #119) —
  `UpdateParfaitImagesController`가 컬렉션 경로에 `@PatchMapping`으로 붙었다. 커밋 본문이 밝힌 동기가
  "클라이언트가 단건 수정 API를 개수만큼 반복 호출해야 했다"이고, 앱 `CanvasBGEditViewModel`이 실제로
  `async` + `awaitAll`로 그렇게 부른다(PR #336). 앱에 이 엔드포인트의 표면은 0건이다.
- **항목**: ① 옮겨 탈지 — 옮기면 **부분 성공이 사라진다**(서버가 `@Transactional` 하나로 묶어 항목
  하나가 걸리면 전부 롤백). 지금은 토핑별로 성공·실패가 갈린다. ② 실패 응답에 **어느 항목이 걸렸는지가
  없다** — 코드만 오고 `parfaitImageId`는 안 온다. 화면이 "무엇을 되돌릴지" 알 수 없다.
  ③ 단건과 일괄의 **검사 순서가 반대**라 마감된 캔버스의 남의 토핑에 단건은 403
  `PARFAIT_IMAGE_NOT_OWNED`, 일괄은 409 `PARFAIT_ALREADY_CLOSED`를 낸다 — 처분을 코드로 가르는 화면은
  옮겨 타는 순간 갈래가 바뀐다(OQ-P-261과 같은 자리). ④ `items` 개수 상한이 서버에 없다.
  ⑤ **되풀이되는 실패가 다른 토핑까지 막을 수 있다** — 일괄이 한 요청으로 묶이므로, 한 토핑이 계속
  걸리면 같은 확인에 함께 실린 나머지 dirty 토핑도 매번 같이 롤백된다(그 토핑만 골라 빼는 폴백은
  없다). 폴백 없이 간 근거는 선택 자체가 `isMine`으로 막혀 있어(`CanvasBGEditViewModel#handleOnClickTopping`)
  남의 토핑이 섞여 들어올 조건이 좁다는 것 — 막힌 것이 아니라 발생 표면이 좁다는 판단이다.
- **상태**: 부분 해소 (① 해소 — 일괄로 옮겨 탔다 / ②③④⑤ 잔존)
  > ✅ **①이 해소됐다(2026-08-31 브랜치 작업 → 2026-09-01 develop 머지, PR #428)** — 확인 버튼이
  > `UpdateToppingsUseCase` → `ToppingRepository.updateAll`로 변형을 일괄 1회에 접는다. **지금
  > 처분은 로그 한 줄이 아니다** — 실패하면 `CanvasBGEditViewModel.handleOnClickConfirm`이 보낸
  > 토핑 전부의 id를 `dirtyToppingIds`에 남겨 다음 확인이 그것만 재시도하고,
  > `CanvasBGEditError.TOPPING_SAVE_UNKNOWN` 토스트를 낸다(이 항목의 초판 해소 메모가 "지금 그
  > 실패는 로그 한 줄로 접히고 있다"고 적은 것은 그 시점에도 이미 낡은 전제였다). ②③④는 서버
  > 계약 그대로라 잔존이고, ⑤가 이 결정의 새 대가로 남는다.
- **해소 메모**: ②는 서버가 응답에 실패 항목을 실어야 풀린다(앱 단독으로는 못 고친다). ③은
  `toCanvasBGEditError`가 403·409를 둘 다 `unknown`으로 접어 문구엔 안 드러나지만 계약 문서의
  사실로는 남는다. ④·⑤는 서버 상한과 화면 폴백을 각각 다시 논의할 사안이다 —
  결정 후 [api/parfait-image.md](../api/parfait-image.md) Android 매핑과 `http/parfait-image.http`
  요청 모음을 함께 채운다.

### [2026-08-31] 단건 위치 PATCH가 서버에 살아 있는데 앱 표면이 사라졌다

- **ID**: OQ-P-335
- **출처**: PR #428(2026-09-01 develop 머지) — 토핑 일괄 수정으로 옮겨 타면서
  `ParfaitImageService.patchGroupsByGroupIdParfaitsByParfaitIdImagesByParfaitImageId`·
  `ParfaitImageRemoteDataSource.updateTopping`·`ToppingRepository.update`·`UpdateToppingUseCase`·
  wire DTO `UpdateParfaitImageRequest`를 함께 걷었다. 서버 `PATCH .../images/{parfaitImageId}`
  엔드포인트 자체는 그대로 있다([api/parfait-image.md](../api/parfait-image.md)) — 앱만 그
  표면을 접었다.
- **항목**: ① 소비처가 다시 생기면(예: 토핑 하나만 옮기는 세밀한 상호작용) wire 계약·매퍼·테스트를
  처음부터 다시 만들어야 한다. ② 되살릴 때 단건과 일괄의 **검사 순서 차이**(마감된 캔버스의 남의
  토핑에 단건은 403 `PARFAIT_IMAGE_NOT_OWNED`, 일괄은 409 `PARFAIT_ALREADY_CLOSED`)를 다시 봐야
  한다 — 두 표면이 같은 화면에 공존하면 같은 상황에 다른 코드가 뜬다.
- **상태**: 미해결 (**동작 영향 0** — 지금은 어느 화면도 단건 PATCH를 부르지 않는다)
- **해소 메모**: 단건 소비처가 다시 필요해지면 [api/parfait-image.md](../api/parfait-image.md)
  엔드포인트 표의 Android 열을 되돌리고 `:data`(Service·DataSource·wire DTO)부터 다시 만든다.
  검사 순서 차이는 OQ-P-334 ③과 같은 자리다.

### [2026-09-01] `recentImageUrl`이 "오늘 캔버스"로 좁혀졌는데 앱은 "토핑 0건"으로 읽는다

- **ID**: OQ-P-336
- **출처**: 서버 `ParfaitGroupMemberRepository.findMyGroupSummaries`(2026-08-31, `02e11be` —
  `recentImageUrl` 서브쿼리에만 `parfait_date = :today`가 붙었고 `ParfaitGroupAdapter.findAllByMemberId`가
  `ParfaitDay.current()`를 넘긴다) × 앱 `domain/model/group/MyParfaitGroupVO`(KDoc) ·
  `feature/groups/list/impl/util/ToppingImage.kt#toToppingImage` ×
  [api/parfait-group.md](../api/parfait-group.md) — 앱은 `recentImageUrl == null`을 "아직 토핑이 없는
  그룹"으로 읽어 템플릿 그래픽을 띄운다. 계약의 뜻은 이제 **"오늘 캔버스에 토핑이 없다"**라, 어제까지
  활동한 그룹도 오늘 캔버스가 비면 같은 그림을 받는다. 같은 줄의 경과 시간은 어제 토핑 시각이라
  **한 카드 안에서 두 표시가 서로를 반박한다.**
- **항목**: ① G-001이 "오늘 토핑 없음"을 템플릿으로 그리는 것이 의도인지 — 위키 [[토핑]]의 대체 그래픽
  정책은 템플릿을 **"첫 토핑 등록 전까지"**로 적는다(그 뜻이면 어제 활동한 그룹은 템플릿이 아니어야
  한다). ② 의도가 아니라면 날짜 제한 없는 최근 이미지를 서버에 되물을지, 앱이 다른 신호로 가를지.
  ③ 어느 쪽으로 정하든 앱 KDoc 두 곳이 옛 뜻을 가르치는 것은 고쳐야 한다.
- **상태**: 부분 해소 (③ 해소 — KDoc 두 곳이 바뀐 뜻을 적는다 / ①② 잔존)
  > ✅ **③이 닫혔다(2026-09-01 develop 머지, PR #428 문서 커밋 `a8d2efd5`)** — `MyParfaitGroupVO`의
  > `recentImageUrl`·`recentImageUploadedAt` KDoc과 `ToppingImage.kt#toToppingImage` KDoc이 "오늘
  > 캔버스 것만 온다"와 "어제까지 활동한 그룹도 템플릿에 걸린다"를 적는다. **화면 동작은 그대로다** —
  > 어느 것을 그릴지(①)와 무엇으로 가를지(②)는 여전히 결정 전이다.
- **해소 메모**: 정하면 [api/parfait-group.md](../api/parfait-group.md) 목록 응답 절과
  [api/conventions.md](../api/conventions.md) "Android 불일치" 행에 반영한다. ①은 정책 소관이라 위키
  [[open-questions]]와 갈리는 자리다 — 여기는 앱이 무엇을 그리는지만 추적한다.

### [2026-09-01] 테두리 미리보기 여백이 굵기 상한과 따로 굳었고, 세 화면의 테두리 렌더 규칙이 갈렸다

- **ID**: OQ-P-337
- **출처**: `ToppingBorderEditScreen`의 `MAX_BORDER_PADDING_DP`(PR #425 develop 머지) ×
  `ToppingEditViewModel`의 `MAX_BORDER_WIDTH_DP` — 미리보기가 알맹이 사방에 여백을 두고 그 판 위에
  테두리를 그려 가장자리에서 깎이던 것을 고쳤다. 여백 값과 굵기 상한은 **같은 값인데 서로를 모르는
  별개 상수**이고 파일도 갈라져 있다.
- **항목**: ① 굵기 상한이 올라가면 미리보기가 **말없이 다시 깎인다** — 두 상수를 잇거나 여백을
  굵기에서 파생시킬지. ② 여백을 둔 화면은 편집 미리보기 하나뿐이다 — 누끼 확인 화면과 캔버스는
  `YGToppingCutoutImage`(여덟 방향 스탬프, [ADR-0025](../adr/0025-topping-border-as-server-field.md))로
  그리므로 **한 흐름 안에서 테두리를 그리는 규칙이 둘**이고, 같은 굵기가 화면마다 다르게 잘릴 수
  있다. ③ 실기기에서 두 화면을 나란히 본 사람이 없다 — 어긋남의 크기를 잰 적이 없다.
- **상태**: 부분 해소 (① **해소**(PR #464 develop 머지,
  [ADR-0030](../adr/0030-topping-outline-distance-field.md)) — `ToppingBorderEditScreen`의
  `MAX_BORDER_PADDING_DP`가 `MAX_BORDER_WIDTH_DP`에서 파생되는 한 줄로 바뀌어 두 상수가 서로를 안다.
  ② **코드는 하나가 됐다** — `YGToppingCutoutImage`가 여덟 방향 스탬프 대신 편집 화면과 같은 거리장을
  그린다. 다만 **③이 아직 닫히지 않아 이번 라운드도 실기기 대조가 없다** — 코드가 통일된 사실과
  네 화면이 실제로 같아 보이는지는 별개다)
- **해소 메모**: ③은 실기기 육안 확인이 선행이다([topping-border-distance-field
  스펙](../superpowers/specs/archive/2026-09-07-topping-border-distance-field.md) 「육안 확인」 1번). 그것이 끝나면
  이 항목 전체가 닫히고 [design-system](../architecture/design-system.md) `YGToppingCutoutImage`
  항목과 [c103-segmentation-topping-edit
  스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md) 테두리 절에 함께 적는다.

### [2026-09-01] 되살린 알맹이 편집은 원본 자리에도 알맹이를 넣어, 재편집 좌표계 전제가 진입마다 다르다

- **ID**: OQ-P-338
- **출처**: `SegmentationConfirmRoute#onClickEditPhoto`·`SegmentationConfirmState.editImagePath`
  (PR #425 develop 머지) — 최근 목록에서 되살린 알맹이는 원본도 재편집 마스크도 없어,
  `NavKeyToppingEdit`의 `sourceImageUri`·`segmentationImageUri`에 **같은 알맹이 경로**가 들어간다.
  `borderOnly = true`라 영역 탭이 안 열리므로 지금은 스트로크가 0건이고 `buildCutoutBitmap` 결과가
  알맹이 그대로다.
- **항목**: ① 그 편집이 돌려주는 `cutoutImagePath`는 **트리밍된 알맹이 기준**이라, "재편집 좌표계를
  지키려 원본 크기를 유지한다"는 `completeEdit`의 전제가 이 진입에서만 다른 뜻이 된다(두 번 되살리면
  기준이 또 한 번 좁아진다). ② `borderOnly` 가드가 느슨해지는 순간 사용자가 **알맹이를 원본으로 알고**
  지운 영역을 되살리려 하게 된다 — 가드가 유일한 방어인데 그 사실이 코드 한 곳에만 있다.
- **상태**: 미해결 (**동작 영향 0** — 가드가 서 있는 동안은 결과가 알맹이 그대로다)
- **해소 메모**: 정하면 [navigation-flow](../architecture/navigation-flow.md)의 재사용 진입 항목과
  [c106-topping-place-api 스펙](../superpowers/specs/archive/2026-08-20-c106-topping-place-api.md)
  「누끼 알맹이 재사용 (PR6)」 절에 적는다.

### [2026-09-01] 환영 배너가 정책 소스 없이 코드로 확정됐고, 1회 노출이 ViewModel 수명에 걸려 있다

- **ID**: OQ-P-339
- **출처**: `CanvasMainViewModel` init(`welcomeGroupName`·`welcomeInviteCode` assisted 인자)·
  `CanvasMainRoute#CanvasMainEffect.ShowWelcome`·`feature/groups/canvas/impl` `strings.xml`
  (`canvas_welcome_joined_title`·`_sub`·`canvas_welcome_created_title`·`_sub`·`canvas_welcome_invite_copy`·
  `_copied`, PR #411 develop 머지) — 그룹을 만들거나 참여한 직후 캔버스에 들어오면 배너가 1회 뜨고,
  생성 갈래에는 초대코드 복사 버튼이 붙는다. ① **위키에 이 배너의 정책 문서가 없다** — 문구 6종과
  갈래 둘(참여/생성)을 코드가 확정했다. ② **1회 보장의 근거가 상태가 아니라 ViewModel 수명**이다 —
  "백스택 재진입에서는 새로 만들어지지 않는다"는 주석이 방어의 전부라, 프로세스 사망 후 복원처럼
  키가 다시 살아나는 경로에서는 배너가 다시 뜬다(인자는 `NavKey`에 그대로 남는다). ③ **복사 완료
  표시가 같은 배너의 재발행**이다 — `복사완료`로 문구만 바꿔 `show`를 다시 부르므로 자동 소멸
  타이머가 처음부터 다시 돌고, 두 번째 배너에는 `onButtonClick`이 없어 **한 번 복사하면 그 배너에서
  다시 복사할 수 없다**. ④ 복사 성공·실패를 사용자에게 알리는 것도 그 문구 교체뿐이다
  (`clipboard.setClipEntry`가 실패해도 문구는 바뀐다).
- **항목**: ① 배너 문구·노출 갈래를 기획·디자인 소스로 확정받을지. ② 1회를 무엇으로 보장할지 —
  소비 후 키에서 지우기(백스택 엔트리 교체), 화면 상태 플래그, 아니면 지금처럼 수명에 맡기기.
  ③ 복사 후 다시 복사할 수 있어야 하는지 — 그렇다면 배너를 재발행하는 대신 버튼 상태를 갈아야 한다.
  ④ 복사 실패 표현을 둘지(같은 화면에 토스트 큐가 이미 있다).
- **상태**: 미해결 (**동작은 한다** — 정책 근거와 재현 조건의 문제다)
- **해소 메모**: 정하면 [navigation-flow](../architecture/navigation-flow.md) "그룹 생성·참여 플로우"의
  진입 사유 인자 항목과 [ygalert 스펙](../superpowers/specs/archive/2026-07-23-ygalert.md) 노출 정책 절에 적는다.
  문구가 정책으로 확정되면 위키 쪽에 별도 등록한다(여기는 구현 소관만 둔다).

### [2026-09-01] 갤러리 저장이 오늘 캔버스로 넓어졌는데, 보일지를 정하는 규칙이 빈 안내판 조건의 부정이다

- **ID**: OQ-P-340
- **출처**: `CanvasMainUiState#isCanvasSaveVisible`·`CanvasMainScreen`(PR #413)·`YGCanvasMenu` 호출부
  (PR #414) — 저장이 지난 캔버스 전용 메뉴 액션에서 **날짜바 오른쪽 아이콘**으로 옮겨 가면서
  ① **오늘 캔버스에서도 저장할 수 있게 됐다.** 지난 캔버스에서만 저장하게 두었던 이전 라운드의 근거
  (편집이 막힌 자리를 저장이 대신한다)가 사라진 셈인데, 넓힌 근거는 커밋에 없다. ② 노출 조건은
  `(isCanvasEmpty && canvasBackground == null).not()`이라 **빈 안내판 조건(OQ-P-304)의 정확한 부정**을
  두 번째 자리에서 다시 적는다 — 안내판 조건이 바뀌면 저장 아이콘이 말없이 따라 바뀌고, 그 결합은
  주석 한 줄로만 유지된다. ③ 저장 아이콘의 유무가 날짜바의 **끝 패딩 분기**까지 정한다(없으면
  텍스트가 잘린 모서리에 붙지 않게 패딩을 대신 넣는다).
- **항목**: ① 오늘 캔버스 저장이 의도인지 기획 확인. ② 안내판·저장의 공통 조건을 이름 있는 하나로
  묶을지(둘 다 "그릴 것이 없다"를 말한다). ③ 저장할 것이 없을 때 아이콘을 감추는 대신 비활성으로
  둘지 — 같은 라운드에 `YGMenuItem`이 비활성 표현을 얻었으므로 수단은 생겼다.
- **상태**: 미해결 (**동작 영향 있음** — 오늘 캔버스 저장은 새 기능이고 검증 기록이 없다)
- **해소 메모**: 정하면 [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)
  목표·범위 절과 [designsystem-canvas-components 스펙](../superpowers/specs/archive/2026-07-31-designsystem-canvas-components.md)
  `YGCanvasDateSelectButton` 치수표에 적고, ②는 OQ-P-304와 함께 닫는다.

### [2026-09-02] FCM을 걷어낸 근거가 서버 delta로 사라졌다 — 되살릴지 정해야 한다

- **ID**: OQ-P-341
- **출처**: 서버 `DeviceTokenController`(`POST /api/v1/notifications/devices`, `0c59af9`) ×
  [api/notification.md](../api/notification.md) ×
  [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 철회 정정(2026-08-22 PR #325) — 앱은 FCM 수신
  서비스·토큰 조회·알림 채널·`POST_NOTIFICATIONS` 권한 요청과 `firebase-messaging` 의존까지
  **갖고 있다가 걷어냈다.** **철회 근거는 "쓰이지 않았다" 하나**였고, 쓰이지 않은 이유가
  `onNewToken`이 `TODO("서버에 FCM 토큰 전송")`인 채였다는 것 — **보낼 서버가 없었다.**
  이번 delta가 정확히 그 자리를 열었으므로 **철회를 떠받치던 전제가 없어졌다.** develop
  `0173e454`에는 여전히 관련 심볼이 0건이다.
- **항목**: ① 되살릴지, 알림 기획(OQ-P-343)이 확정된 뒤로 미룰지 — ADR-0013은 "결선 없는 껍데기를
  출시 경로에서 뺀 것"이라 명시했으므로 **결선이 가능해진 지금은 판단 근거가 다르다.**
  ② 되살린다면 등록 호출 시점 — 서버는 **앱 시작·`onNewToken`·권한 허용마다 재호출**을 전제로 upsert를
  설계했다(반복 호출이 정상이라는 뜻이다). ③ 알림 권한을 언제 묻는지 — ADR-0013이 "되살릴 때 다시
  정하라"고 남긴 물음이고, 걷어낸 형태(`MainActivity.onCreate`에서 무조건)가 문제였다.
  ④ 권한 거부 상태에서도 토큰을 등록할지.
- **상태**: **해소됨** (①은 2026-09-05 PR #446·#447, ②③④는 같은 날 PR #450 develop 머지.
  ⚠️ 실서버·실기기 확인은 0회라 등록이 실제로 통하는지는 아무도 보지 않았다)
  > 📌 **`:data` 표면이 생겼다(2026-09-03, PR #437 `7019a550`)** — `NotificationService` ·
  > `NotificationRemoteDataSource`(+`Impl`) · `DeviceToken`. **미결은 그대로다** — ①이 묻는 것은
  > 표면의 유무가 아니라 **FCM 축을 되살릴지**이고, 토큰을 얻는 심볼(`FirebaseMessaging`·
  > `firebase-messaging` 의존)은 develop에 여전히 0건이라 **이 표면을 부를 수단 자체가 없다.**
  > ②(등록 호출 시점)는 이제 "부를 자리가 없다"가 아니라 "어디서 부를지 안 정했다"가 됐다 —
  > `registerDeviceToken`의 호출부는 0건이다. `platform`을 `"ANDROID"`로 고정한 것은 앱이 정했다
  > (호출자가 고르지 않는다) → [api/notification.md](../api/notification.md) Android 매핑.
  > 📌 **부를 수단이 미머지 브랜치에 있다(2026-09-03 확인, `feature/push-fcm-service`)** — `firebase-messaging`
  > 의존과 `ParfaitFirebaseMessagingService`(수신·채널·`POST_NOTIFICATIONS` 확인)·푸시 딥링크
  > 버스가 develop 밖에서 되살아나 있고, **`onNewToken`도 함께 돌아왔다.** 그런데 그 자리는
  > 아직 등록을 부르지 않는다 — 주석이 "등록 API 스펙이 아직 배포되지 않았다"고 적는데,
  > **그 전제는 #437이 이미 무너뜨렸다**(develop 에 `NotificationRemoteDataSource` 가 있다).
  > 즉 이 미결의 ①은 브랜치에서 사실상 "되살린다"로 답해졌고, 남은 것은 ②(등록 호출 시점)다.
  > 머지 전에는 문서를 고치지 않는다([2026-07-13] 규율) — 머지 회차에 이 줄부터 본다.
  > ⚠️ **미룰 때의 대가가 2026-09-04 서버 delta(`aa9cc9b`)로 달라졌다** — 서버가 **실제로 푸시를 보내기
  > 시작했다**(신규 토핑 배치 시 그룹의 나머지 구성원에게). 지금까지 이 미결은 "앱이 안 보내면 아무 일도
  > 안 일어난다"였는데, 이제는 **보내는 쪽이 돌고 있고 받을 앱이 없다.** 등록된 토큰이 0건이라 발송이
  > 전부 `NO_DEVICE_TOKEN`으로 취소되므로 **동작 영향은 여전히 0**이지만, ①을 "되살린다"로 답하는
  > 순간 채널 id·`data` 스키마·중복 수신 내성이 곧바로 구속력을 갖는다(OQ-P-351·352)
  > → [api/notification.md](../api/notification.md) "이 계약이 앱에 요구하는 것".
  > 📌 **손으로 돌려 볼 자리가 develop에 생겼다(2026-09-04, PR #451 `2b1dce3a`)** — `http/notifications.http`(등록)와 `http/fcm-test.http`(FCM 직접 발송)다. **앱 코드는 여전히 한 줄도 안 바뀌었고** 이 미결의 ①~④ 어느 것도 답해지지 않았다. 값은 두 가지다 — ① 등록 계약(204·upsert·400 4종)을 **앱 없이 지금 확인할 수 있게** 됐고, ② 수신부가 붙은 뒤 채널 id·`data` 파싱·딥링크를 **토핑을 실제로 올리지 않고** 재현할 수 있다. ⚠️ **아직 아무도 안 돌렸다** — `fcm_access_token`(1시간 만료)과 Firebase 서비스 계정 키가 필요하다.
  > 📌 **미머지 브랜치 `feature/push-notification-permission` 이 이 미결의 ②③④를 한꺼번에 건드린다(2026-09-04 확인)** — 커밋 제목이 `기기 토큰 등록의 도메인 계약을 만든다` · `NotificationRepository를 이미 있는 기기 토큰 등록 API에 연결한다` · `FCM 토큰 발급 시(onNewToken) 서버에 등록한다` · `알림 권한을 허용한 직후 지금 토큰을 바로 등록한다` · `그룹 생성·참여 직후 알림 권한 안내를 추가한다`로, ②(등록 호출 시점)는 **`onNewToken` + 권한 허용 직후**, ③(권한을 언제 묻는지)은 **그룹 생성·참여 직후**로 답해져 있다. 직전 회차가 `feature/push-fcm-service` 에서 본 "등록을 안 부르는 자리"도 이 브랜치가 잇는다. 머지 전에는 문서를 고치지 않는다([2026-07-13] 규율) — 머지 회차에 ②③④와 ADR-0013 되살림 정정을 함께 본다.
  > 📌 **미머지 브랜치가 다섯에서 일곱이 됐다(2026-09-04 확인)** — 위 권한 브랜치와 `feature/#420-canvas-tutorial`(C-001 최초 진입 튜토리얼 오버레이·표시 여부를 사용자 설정에 저장)이 더해졌다.
  > ✅ **①이 답해졌다 — 되살렸다(2026-09-05, PR #447 `feature/push-fcm-service` · #446 `feature/push-notification-deeplink` develop 머지).**
  > `firebase-messaging` 의존 · `app` `push/ParfaitFirebaseMessagingService`(수신·표시) ·
  > `BaseApplication`의 채널 생성 · 매니페스트의 서비스 등록과 `POST_NOTIFICATIONS` 선언이 돌아왔고,
  > 예고에 없던 딥링크 축이 함께 들어왔다 → [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정.
  > **결정의 형태가 걷어낼 때와 다르다** — 채널 id를 앱이 정하지 않고 **서버가 못 박은 `parfait_default`를
  > 따랐고**(OQ-P-352 ①), 채널 생성 자리가 `MainActivity`에서 `BaseApplication`으로 옮겼다.
  > ⚠️ **②는 그대로 미해결이고 성격만 바뀌었다** — `onNewToken`이 돌아왔는데 **등록을 부르지 않는다.**
  > 그 자리의 `TODO` 주석은 "등록 API 스펙이 아직 배포되지 않았다"고 적지만 **그 전제는 PR #437이 이미
  > 무너뜨렸다**(`NotificationRemoteDataSource`가 develop에 있다). 직전 회차가 브랜치를 보고 예고한
  > 그대로다 — 판정은 "부를 수단이 없다"에서 **"부를 수 있는데 안 부른다"**로 옮겼다.
  > ⚠️ **③은 답이 아니라 공백으로 돌아왔다** — 권한을 묻는 코드가 develop에 0건이고, 그래서 Android 13+
  > 에서는 수신부와 채널이 다 있어도 표시가 막힌다 → **OQ-P-358 신설**. ④(권한 거부 상태에서도 등록할지)는
  > 등록 호출부가 0건이라 여전히 도달하지 않는다.
  > 📌 **미머지 브랜치는 일곱에서 넷이 됐다**(`feature/debug-mode` · `feature/#420-canvas-tutorial` ·
  > `feature/#423-canvas-save-preview` · `feature/push-notification-permission`). 남은 푸시 브랜치가
  > ②③④를 한꺼번에 답하는 그 브랜치다.
  > 📌 **넷에서 둘이 됐다(2026-09-05 확인)** — `feature/#420-canvas-tutorial`·`feature/#423-canvas-save-preview`가
  > develop에 들어갔고(둘 다 알림과 무관하다), 남은 것은 `feature/debug-mode`와
  > `feature/push-notification-permission` 둘이다. **이 미결의 ②③④를 답하는 브랜치는 아직 밖에 있다.**
  > 📌 **그 브랜치의 답이 2026-09-05에 바뀌었다 — 리뷰가 ②를 뒤집었다(미푸시, develop 밖).**
  > 직전 회차가 커밋 제목에서 읽은 답(②는 `onNewToken` + 권한 허용 직후)이 **결함이었고, 같은
  > 브랜치 안에서 되돌려졌다**(`7e984099` → `a99a899e`). develop은 애초에 등록을 부르지 않았으므로
  > **출시된 적 없는 결함이다.** FCM 토큰은 알림 권한과 무관하게 SDK가 설치 시점에 발급하는데, 등록을
  > 권한에 매달면 **재로그인·기기교체·재설치 사용자가 등록 경로에 닿지 못한다**(서버가 로그아웃에서
  > 매핑을 지우는데 기기 토큰은 그대로라 `onNewToken`이 안 불리고, 권한은 이미 허용이라 안내가
  > 건너뛴다). 브랜치가 등록 시점을 **세션 축**
  > (`LoginWithKakaoUseCase`·`SignUpUseCase`의 `saveSession` 직후 · `BootstrapSessionUseCase`의 성공
  > 분기)과 `onNewToken` 넷으로 다시 세웠다 — 서버가 전제한 "앱 시작·`onNewToken`·권한 허용마다
  > 재호출"에 맞는 형태다. ④는 **거부 상태에서도 등록한다**로 답해졌다(서버가 권한 상태를 모르니
  > 발송이 나가고 OS가 버리지만, 사용자가 나중에 설정에서 켜면 앱이 아무것도 안 해도 동작한다).
  > ③은 **A-004·A-005 완료 직후**로 유지하되 영구 거부 시 앱 설정으로 보내는 갈래가 붙었다.
  > ⚠️ **브랜치가 미결 하나를 새로 드러냈다** — `POST_NOTIFICATIONS`가 API 33 신설이라 `minSdk` 26의
  > API 26~32에서는 `checkSelfPermission`이 항상 거부를 답하는데 알림은 기본으로 켜져 있다. develop의
  > `ParfaitFirebaseMessagingService.showNotification`이 정확히 그 함정을 밟아 **그 기기군의 포그라운드
  > 알림을 전부 버리고 있다.** 브랜치가 판정을 `NotificationPermissionManager`로 빼서 두 자리를 함께
  > 고쳤다. 설계 정본은
  > [specs/2026-09-05-push-notification-permission-and-device-token](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md)이다.
  > 📌 **브랜치가 알림과 무관한 축 하나를 더 싣고 있다** — 이벤트 버스 재배치다. `domain/event`·
  > `data/event`를 신설해 세션 종료·푸시 딥링크 버스를 `repository/`·`data/{session,push}`에서 옮기고,
  > `SessionEventSource`를 `SessionEventBus`로, `:data` 구현을 `SessionEventBusImpl`로 개명했다(둘 다
  > Repository가 아니고, `Source`는 이 저장소에서 DataSource 계열 이름이라 오독을 부른다).
  > **동작 변경은 없다**(Hilt 그래프 동일, 12파일 rename). `SessionEventBus`가 구독만 내놓고 발행은
  > `:data` 구현이 갖는 비대칭은 그대로다 — [data-layer](../architecture/data-layer.md)가 그 차이를
  > 의도된 설계로 적어 두었으므로 머지 회차에 그 문단의 심볼명을 갱신해야 한다.
  > 머지 전에는 문서를 고치지 않는다([2026-07-13] 규율) — 머지 회차에 ②③④와 ADR-0013 되살림 정정,
  > **ADR-0004의 평면 배치 규칙 예외**(`DeviceTokenModule`이 `:app` 최초의 Hilt 모듈이다.
  > ADR-0013이 Firebase 의존을 `:app`에 가둬 `:data`로 못 내린다), 그리고 위 이벤트 버스 개명에 따른
  > `data-layer.md`·`module-structure.md` 갱신을 함께 본다.
  > ✅ **②③④가 모두 답해졌다 — 이 미결은 닫힌다(2026-09-05, PR #450 `489b14cc` develop 머지).**
  > 위에 예고한 그대로 들어왔고 어긋난 자리가 없다.
  > - **②(등록 호출 시점)** — 세션 축 넷이다: `LoginWithKakaoUseCase`·`SignUpUseCase`의
  >   `refreshMyAccount` 뒤, `BootstrapSessionUseCase`의 성공 분기, `onNewToken`. 네 자리 모두
  >   **`suspend`가 아닌 `DeviceTokenRegistrar.register()`** 하나를 부르고 실행은 `:data` 구현이
  >   `@ApplicationScope`에서 한다. 재시도 3회(3초·6초)와 `Mutex`가 그 안에 있다.
  > - **③(권한을 언제 묻는지)** — A-004·A-005 완료 직후다(OQ-P-358 해소).
  > - **④(거부 상태에서도 등록할지)** — **등록한다.** 서버가 권한 상태를 모르므로 발송이 나가고 OS가
  >   버리지만, 사용자가 나중에 설정에서 켜면 앱이 아무것도 안 해도 동작한다.
  > 예고했던 문서 일도 함께 끝났다 — [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정에
  > 등록·권한 결선을 더했고, [ADR-0004](../adr/0004-hilt-ksp-di.md)에 `:app` Hilt 모듈 예외를,
  > [data-layer](../architecture/data-layer.md)·[module-structure](../architecture/module-structure.md)·
  > [ADR-0021](../adr/0021-token-refresh-forced-logout.md)에 이벤트 버스 개명을 적었다.
  > ⚠️ **남은 것은 실행 확인이다** — 실서버·실기기 확인이 0회라 등록이 실제로 204를 받는지, 발송이
  > `NO_DEVICE_TOKEN`을 벗어나는지 아무도 보지 않았다.
- **해소 메모**: 정하면 [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md)에 **되살림 정정**을 더하고
  (철회 정정을 지우지 않는다 — 두 결정이 다 이력이다), [api/notification.md](../api/notification.md)
  Android 매핑 절과 [data-layer](../architecture/data-layer.md)에 적는다. ①이 "미룬다"로 정해져도
  **미룬다는 결정 자체를 적어야** 다음 라운드가 같은 물음을 다시 연다.

### [2026-09-02] 세션 없이 등록된 기기 토큰은 로그아웃이 지우지 못한다

- **ID**: OQ-P-342
- **출처**: 서버 `LogoutService`(`DeviceTokenDeletePort.delete(memberId, sessionId)`) ·
  `DeviceTokenAdapter.delete`(`deleteByMemberIdAndSessionId`) · `V17__create_device_token.sql`
  (`session_id` 널 허용) × [api/notification.md](../api/notification.md) — `sessionId`는
  access token 클레임에서 오는데 **이 변경 전에 발급된 토큰에는 그 클레임이 없어 널로 등록된다.**
  삭제 조건이 `memberId`와 `sessionId`를 함께 보므로 **널인 행은 로그아웃으로 지워지지 않는다.**
  걷어 내는 것은 탈퇴(회원 단위 전량)와 같은 토큰의 재등록(upsert)뿐이다.
- **항목**: ① 과도기 창이 닫힌 뒤(구 access token이 전부 만료된 뒤) 서버가 널 행을 정리할지.
  ② 앱이 붙을 때 로그인 직후 재등록으로 널 행을 덮게 할지 — 재등록은 같은 `token`이면 upsert라
  세션이 채워진다. ③ 로그아웃한 기기로 알림이 계속 갈 수 있다는 뜻인데, 그 위험을 누가 막을지.
- **상태**: 미해결, **사각이 좁아졌다** (앱 동작 영향은 여전히 0 — 앱이 아직 등록하지 않아 널 행이
  생길 경로가 없다. 2026-09-03 PR #437로 `:data` 표면은 생겼으나 호출부가 0건이라 이 전제는 그대로다)
  > 📌 **2026-09-04 서버 delta(`aa9cc9b`)로 걷어 가는 경로가 하나 늘었다** — 발송이 죽은 토큰
  > (`UNREGISTERED`·`INVALID_ARGUMENT`·`SENDER_ID_MISMATCH`)을 만나면 `deleteByToken`으로 그 행을
  > 회수한다. **세션을 안 보고 토큰만으로 지우므로 널 세션 행도 걷힌다.** 다만 그것은 **그 기기가
  > 실제로 무효가 된 뒤**의 정리라, ③(로그아웃한 기기로 알림이 계속 가는 위험)은 그대로 남는다 —
  > 로그아웃해도 토큰 자체는 유효하기 때문이다.
- **해소 메모**: 서버가 닫으면 [api/notification.md](../api/notification.md)
  "기기 토큰이 지워지는 세 경로"의 경고를 지운다. 앱이 ②로 닫으면 등록 호출 시점(OQ-P-341 ②)과
  같은 자리에서 정해진다.

### [2026-09-02] 알림을 무엇을 언제 보내는지가 서버 계약에도 정책에도 없다

- **ID**: OQ-P-343
- **출처**: 서버 커밋 `[Feat/#125]` 메시지가 **"발송 인프라와 알림 트리거는 이 변경 범위 밖"**이라고
  명시 × [api/notification.md](../api/notification.md) — 저장소(`device_token`)와 등록 표면만 열렸고
  발송 조건·문구·딥링크 목적지·수신 설정이 전부 비어 있다. 위키 정책 소스에도 알림 항목이 없다.
- **항목**: ① 알림 종류(토핑 등록·캔버스 마감·그룹 초대 등)와 트리거 조건. ② 문구·딥링크가 화면
  어디로 떨어지는지. ③ 사용자 수신 설정(전역·그룹별)을 둘지 — 두면 S-001 앱 설정에 자리가 필요하다.
  ④ 권한 요청을 언제 띄울지(Android 13+ `POST_NOTIFICATIONS`).
- **상태**: **부분 해소** (①의 일부와 ②가 서버 코드로 답해졌고, ③④는 그대로 미해결)
  > 📌 **2026-09-04 서버 delta(`aa9cc9b`)가 보내는 쪽을 붙였다** — `[Feat/#127]`이 FCM 발송 인프라와
  > **토핑 등록 알림**을 연결했다. ①의 답은 **1종뿐**이다: 신규 토핑 배치(재배치 제외)에 대해
  > 그룹 구성원 중 나간 사람과 작성자 본인을 뺀 나머지에게 나간다. ②도 서버가 정했다 —
  > 문구와 `data` 키(`type`·`route=canvas`·`groupId`·`date`)가 `NotificationMessageFactory` 한 곳에
  > 있다. **다만 그것을 승인한 정책 문서는 여전히 없고**, 딥링크가 앱의 어느 목적지로 떨어지는지도
  > 안 정해졌다 → OQ-P-351로 분리했다.
  > ⚠️ **커밋 제목은 "3종 알림 트리거 연결"이라고 적지만 코드에 연결된 트리거는 1종이다** —
  > `NotificationMessageFactory`에 문구 조립 함수가 하나뿐이고 `ToppingPlacedNotifier` 호출부도
  > 한 곳이다. **나머지 둘이 무엇이었는지 서버 코드에서 읽을 수 없다** — 캔버스 마감·그룹 초대 같은
  > 후보의 발송 코드가 없다. 이것이 ①에 남은 물음이다.
  > ③④는 서버가 손대지 않았다. 수신 설정 축이 없어 **끄는 방법이 OS 설정뿐**이고, 권한 요청 시점은
  > 여전히 앱 몫이다(OQ-P-341 ③과 같은 자리).
  > 📌 **2026-09-08 갱신 — ①이 답해졌다**(서버 `4789557`, `[Feat/#128]`). 알림 종류가 **3종**이 됐다:
  > 토핑 등록 · **오전 리마인드 `REMIND_AM`(10:00 KST)** · **저녁 리마인드 `REMIND_PM`(20:00 KST)**.
  > 커밋 제목의 "3종"과 코드가 이제 맞고, **나머지 둘이 무엇이었는지도 드러났다** — 캔버스 마감·그룹
  > 초대가 아니라 데일리 리마인드였다. 캔버스 마감 알림은 **여전히 발송 코드가 없다**(채널 설명 문구가
  > 그것을 약속하는 것도 그대로다).
  > ⚠️ **③이 더 급해졌다.** 리마인드는 하루 두 번 **전 이용자**에게 나가고, 대상 조회
  > (`ReminderTargetQueryPort`)가 **토큰 보유 여부만 본다** — 끄겠다는 의사를 담을 자리가 서버 스키마에
  > 없다. 게다가 그 KDoc이 전제하는 "권한 거부 이용자는 토큰이 없다"가 앱 동작과 어긋난다 → OQ-P-384.
  > 📌 **앱이 ①의 나머지 둘을 먼저 구현했다(2026-09-05, PR #446)** — `PushNotificationType`이
  > `TOPPING`·**`REMIND_AM`·`REMIND_PM`** 셋이고 `route=group`이 그룹 목록으로 떨어진다. 즉 앱은
  > **P-01/P-02/P-03 세 알림을 전제로 서 있는데 서버가 보내는 것은 하나뿐**이다. 근거로 코드가
  > 가리키는 것은 "FCM 페이로드 스펙 v1"인데 **그 문서가 어느 저장소에도 없다** → **OQ-P-361 신설**.
  > ⚠️ **④는 더 나빠졌다** — 서버가 안 손댄 것은 그대로이고, 앱은 **권한을 묻는 코드 없이** 수신부만
  > 붙였다(OQ-P-358). ③(수신 설정)도 그대로라 끄는 방법은 여전히 OS 설정뿐이다.
- **해소 메모**: ①②③은 **정책 소관이라 위키 [[open-questions]]와 갈리는 자리**다 — 여기는 앱이
  무엇을 구현해야 하는지만 추적한다. 정해지면 스펙을 `specs/`에 세우고
  [api/notification.md](../api/notification.md) 미결 절을 지운다.

### [2026-09-02] 세그멘테이션 모듈을 끝내 못 받는 기기에서 사진 편집을 어떻게 할지 없다

- **ID**: OQ-P-344
- **출처**: [segmentation-module-install.md](../superpowers/specs/archive/2026-09-02-segmentation-module-install.md)
  「왜 지금인가 — 원인 ②」 × [ADR-0012 실측 정정](../adr/0012-mlkit-subject-segmentation.md) —
  실기기(Galaxy Z Flip 3, SM-F711N)에서 GMS가 `STATE_FAILED` / `CommonStatusCodes.INTERNAL_ERROR`로
  optional module 설치를 못 잇는다. 재부팅해도 같고, `MODULE_NOT_FOUND`도 `INSUFFICIENT_STORAGE`도
  아니며 Play 응답은 정상(`moduleDelivery` rc=200)이다.
- **항목**: ① 모듈을 못 받는 기기에서 토핑 만들기를 아예 막을지, 누끼 없이 원본으로 진행하게 할지,
  수동 편집(C-104)으로 우회하게 할지 — **정책 결정이 필요하다.** ② 실패가 얼마나 흔한지 모른다.
  실기기 한 대의 관찰이고 모수가 없다. ③ 프로덕션에서 이 실패를 셀 수단이 없다.
- **상태**: 미해결, **전제 축소 2회**. ✅ **①에 우회로가 생겼다(2026-09-06, PR #457)** — 실패 화면의
  「편집 없이 사용」이 누끼 없이 원본을 토핑 재료로 보내므로, 모듈이 안 와도 **토핑을 만들 경로가 있다.**
  ①은 "막을지"가 아니라 "우회로를 이대로 둘지"로 좁아졌고 ②가 남는 본체다.
  🔁 **우회로의 모양이 바뀌었다(2026-09-10, PR #487)**. 그 버튼이 「직접 편집」이 되어 원본을 C-104 수동
  편집으로 보낸다. C-104는 세그멘테이션 모듈을 부르지 않으므로, 모듈 없이 토핑을 만드는 경로는 그대로 있다.
  ①의 선택지 가운데 "수동 편집(C-104)으로 우회"를 코드가 먼저 골랐고, 정책 근거는 여전히 없다(OQ-P-401).
  아래는 1차 축소(같은 날 후속 관측) — 그 기기에 **모듈이 결국 도착했다.**
  오전 내내 `INTERNAL_ERROR`로 실패하던 설치가 몇 시간 뒤 성공했고 세그멘테이션이 정상 동작했다.
  "끝내 못 받는 기기"가 아니라 **아주 늦게 받는 기기**였을 수 있다. ①의 무게는 그만큼 줄었고,
  ②(빈도를 셀 수단이 없다)가 남는 본체다.
- **해소 메모**: ②가 먼저다. 실패 코드가 로그에만 남으면 빈도를 못 세므로, 원격 로깅에 실을지를
  정하는 것이 ①의 전제다. ①은 위키 정책 소관과 갈리는 자리다 —
  기능정의서에 "누끼 실패 시 무엇을 보여주는가"가 없다.
  ⚠️ **판정 수단 주의**: optional module은 APK split이 아니라 Chimera dynamite 모듈로 배달된다.
  GMS 패키지의 `splits` 목록에 없어도 깔려 있을 수 있다 — `DynamiteModule` 로그와
  `areModulesAvailable`만 믿는다.

### [2026-09-02] 모듈 판정용 feature 이름이 ML Kit 내부 값이라 바뀌면 조용히 깨진다

- **ID**: OQ-P-345
- **출처**: [segmentation-module-install.md](../superpowers/specs/archive/2026-09-02-segmentation-module-install.md)
  「API / 인터페이스」 — 판정에 `SubjectSegmenter`를 쓰면 네이티브 그래프가 하나 더 떠서 실제
  세그멘테이션과 겹쳐 SIGBUS로 죽는 것을 실기기(Galaxy Z Flip 3)에서 확인했다. 그래서 `Feature`
  하나만 든 `OptionalModuleApi`로 바꿨고, 이름 `mlkit.segmentation.subject`와 버전 `1`은 같은
  기기의 `ChimeraConfigurator` 로그에서 읽은 값이다.
- **항목**: ① ML Kit이 그 이름·버전을 바꾸면 `areModulesAvailable`이 늘 false를 돌려주고, 앱은
  설치 요청만 반복하다 실패 화면을 띄운다 — **크래시가 아니라 조용한 기능 정지**라 알아채기 어렵다.
  ② 회귀를 잡을 수단이 없다. JVM 테스트는 가짜 게이트웨이를 쓰므로 이 값이 틀려도 통과한다.
  ③ ML Kit이 공개 상수를 제공하는지 확인하지 못했다(`OptionalModuleUtils`는 내부 SDK 클래스다).
- **상태**: 미해결 (**동작 중** — 현재 값으로 실기기에서 정상 동작 확인)
- **해소 메모**: ②를 먼저 좁히는 편이 싸다. 모듈이 없는 기기에서 설치 요청이 실제로 나가는지를
  실기기 로그로 확인하는 절차를 회귀 점검에 넣는 것이 최소한이다. ML Kit 버전을 올릴 때 이 값을
  같이 확인한다.

### [2026-09-02] 원격 이미지 묶음 노출 — 미머지 브랜치가 로딩 표현을 바꾼다

- **ID**: OQ-P-346
- **출처**: TJYG-Android 미머지 브랜치 `feature/image-loading-placeholder`(develop 기준선 위) —
  원격 이미지가 로딩 중 아무것도 그리지 않다가 성공하는 순간 불투명으로 튀던 것을 고치면서,
  캔버스 토핑과 G-001 그룹 목록을 각각 **한 묶음으로 모아 한 번에 드러내는** 규칙이 생겼다.
  `core:ui`에 `reveal/` 패키지(`rememberBatchReveal`·`rememberStaggeredReveal`과 그 순수 함수)가
  신설되고 두 화면이 그것을 공유한다. `core:designsystem`에는 `YGSkeleton`이 신설되고,
  `YGToppingGroup`이 `onImageSettled`를, `CanvasToppingLayer`가 `revealTogether`·`revealResetKey`를
  받는다. 전역 `ImageLoader`(`newParfaitImageLoader`)를 두 `Application`이 `SingletonImageLoader.Factory`로
  물어 주고 크로스페이드를 켠다.
- **항목**: 머지되면 delta 감사에서 갱신할 것 — ① [design-system](../architecture/design-system.md)
  컴포넌트 인벤토리에 `YGSkeleton` 등록, `YGCanvas` 배경이 로딩 상태를 밖으로 들어 올려
  스켈레톤을 캡처 대상 밖에 그리는 것과 `YGToppingGroup`의 결말 콜백 반영.
  ② [module-structure](../architecture/module-structure.md)에 `core:ui` `reveal/` 추가.
  ③ ADR을 쓰지 않기로 했으므로 이 결정의 근거는 커밋 메시지와 `reveal/`의 순수 함수 KDoc에만
  있다 — 인벤토리에서 그 자리를 가리킬지 정한다.
- **상태**: 해소됨 (2026-09-04, PR #440 develop 머지 — 다만 **머지본이 브랜치와 셋 다르다**, 아래)
- **해소 메모**: 문서 조치는 예고한 ①②를 그대로 했다 —
  [design-system](../architecture/design-system.md)에 `YGSkeleton`·`YGDimOverlay`·`YGLoadingArt`·
  이미지 로더 절을 세우고, [module-structure](../architecture/module-structure.md)에 `core:ui` `reveal/`
  항목을 뒀다. ③(ADR을 안 쓰기로 한 결정의 근거를 어디서 가리킬지)은 **인벤토리 쪽으로 정했다** —
  module-structure의 `reveal/` 항목이 "왜 `core:designsystem`이 아닌가"를 적고, 컴포넌트 표는 두 신설
  컴포넌트의 스펙 칸에 "스펙 없음"을 명시해 찾는 사람이 헤매지 않게 했다.
  **브랜치를 보고 적어 둔 것 중 셋이 머지본과 다르다.**
  ① `YGToppingGroup`은 **한 줄도 안 바뀌었다** — 결말 콜백(`onImageSettled`)이 들어오지 않았다.
  G-001은 이미지를 기다리지 않는 쪽으로 바뀌었기 때문이다: 자리가 먼저 열리고 그림이 뒤따라 채워진다.
  ② 그래서 두 화면이 공유하는 것은 `reveal/` 패키지이되 **구현이 갈린다** — 캔버스는
  `rememberBatchRevealState`(결말을 세어 한 번에), G-001은 `rememberStaggeredRevealState`(시간 간격으로
  하나씩)다. ③ `YGSkeleton`의 소비처는 **`YGCanvas` 배경 하나**다 — G-001 토핑 자리에는 안 들어갔다.
  함께 예고에 없던 것이 둘 붙었다: `YGDimOverlay` 분리와 전역 `ImageLoader`의 **다시 받기**
  (`rememberReloadableImageRequest`) — 뒤엣것이 C-001 재시도 버튼의 전제다.

### [2026-09-03] 누끼 영역 편집의 빨간 틴트가 정책 소스 없이 코드로 확정됐다

- **ID**: OQ-P-347
- **출처**: `feature/segmentation/impl` `screen/ToppingEditScreen.kt`(`MASK_TINT_ALPHA`, 마스크 합성
  레이어 안에서 `BlendMode.SrcAtop`으로 `Cherry500` 한 겹)(PR #442 develop 머지) × 위키 [[누끼-편집]] —
  정책은 **지워진 배경을 불투명도 50%로 노출**하고 **채운 자리를 100%로 복구**하는 것까지만 적는다.
  **남는 영역을 색으로 물들인다는 조항이 없다.** 머지 커밋 제목이 "누끼 원본 영역 빨간색으로 변경"이라
  요청이 있었던 것은 분명하나, 근거는 커밋 메시지에만 있다.
- **항목**: ① 이 틴트가 정책인지 임시 보조선인지 — 정책이면 위키 [[누끼-편집]]에 조항이 필요하고,
  임시면 걷는 조건이 필요하다. ② 색과 알파의 근거 — `Cherry500` 50%는 같은 화면 붓 미리보기와 같은
  색·같은 알파다. 편집 대상과 미리보기가 같은 색으로 보이는 것이 의도인지 확인 안 됐다.
  ③ 테두리 편집 탭·확인 화면·배치 화면은 틴트 없이 그린다 — **같은 알맹이가 화면마다 다른 색으로
  보인다.** 어디까지가 편집 중 표시인지 규칙이 없다.
- **상태**: 미해결 (**동작 중** — develop 코드이고 영역 탭에서만 그린다)
- **해소 메모**: ①이 먼저다. 정책으로 굳으면 [c103 스펙](../superpowers/specs/archive/2026-08-15-c103-segmentation-topping-edit.md)
  「정책 대조」 표에 행을 더하고 위키 쪽에 조항을 요청한다. 임시로 남기면 걷는 조건을 그 스펙에 적는다.
  ③은 [2026-09-01] "세 화면의 테두리 렌더 규칙이 갈렸다" 항목과 같은 축이다 — 함께 본다.

### [2026-09-03] G-001 새로고침 — 미머지 브랜치가 로딩 문구와 실패 라우팅을 바꾼다

- **ID**: OQ-P-348
- **출처**: TJYG-Android 미머지 브랜치 `feature/image-loading-placeholder`(OQ-P-346과 같은 브랜치,
  커밋 `163cd194`·`1d68f167`) — 디자인 두 프레임을 근거로 G-001 당겨서 새로고침을 다시 짰다
  (Figma 파일 `QPoxqbNMNktsi8ktua3gMN`, 두 프레임 모두 이름이 `G-001-Error`다: 노드 `2019:10223`이
  새로고침 중, `1972:4873`이 실패). 셋이 바뀐다. ① `GroupListPullToRefreshBox`의 인디케이터가
  로띠 하나에서 **로띠 + 안내 문구 2줄**(`group_list_refreshing`, 신설)이 됐다. ② 당기는 동안
  `GroupListScreen`이 **토핑을 비운다** — 디자인의 새로고침 프레임이 빈 컵이다. ③ 당긴 새로고침이
  실패하면 목록이 남아 있어도 `GroupListErrorScreen`으로 간다. 그래서 `ShowRefreshError` 토스트와
  `group_list_refresh_error`가 도달 불가가 되어 걷혔다.
- **항목**: 머지되면 delta 감사에서 볼 것 — ① [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md)
  「에러·새로고침」 절과 정책 대조 표의 로딩·실패 행. `ShowRefreshError`가 사라지고 실패 갈림의
  기준이 "목록이 남아 있는가"에서 **"사용자가 당겼는가"**로 바뀐 것이 핵심이다.
  ② **디자인의 세로 위치와 구현이 다르다** — 디자인은 로띠·문구 블록을 화면 중앙 근처에 두고
  콘텐츠를 210px 내리는데, 구현은 임계값이 여는 틈 안에 넣어 실측 블록 높이 + 20dp(로띠가
  임계값 틈에서 남기던 여백)만 연다. 이 차이를 디자이너와 맞출지 정한다.
  ③ **새로고침 문구의 정책 소스가 없다** — 실패 문구는 디자인 프레임에 있으나 새로고침 문구는
  Figma에만 있고 위키에 대응 조항이 없다(OQ-P-113 ③과 같은 성격).
- **상태**: 부분 해소 (2026-09-04, PR #440 develop 머지 — ①③은 코드로 확정됐고 **②는 그대로 남는다**)
- **해소 메모**: ①과 ③은 예고한 대로 들어왔다. 반영처는
  [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 「에러·새로고침」·정책 대조 표,
  [screen-resume-refetch 스펙](../superpowers/specs/archive/2026-08-17-screen-resume-refetch.md)의 실패 규칙 표,
  [state-management](../architecture/state-management.md) "재조회 빈도가 실패 표현을 바꾼다"다.
  머지본에서 **더 드러난 것 둘**: 당기는 동안 목록을 비우는 자리가 캐시나 `UiState`가 아니라
  **화면**이라는 것(캐시는 `StateFlow`라 같은 목록을 다시 받으면 재방출하지 않아 비운 값이 굳는다),
  그리고 `isError`가 **켜기만 하고 끄는 것은 성공한 조회뿐**이라는 규칙이다(끄면 실패한 재진입 조회가
  앞선 실패를 덮는다). 초기 덮개를 내리는 시점도 조회 반환에서 **캐시가 목록을 낸 시점**으로 옮겼다.
  ⚠️ **②(디자인의 세로 위치와 구현의 차이)는 손대지 않은 채 머지됐다** — 구현은 임계값이 여는 틈에
  블록을 넣고, 문구가 붙는 만큼만 그 틈을 더 벌린다. 디자이너와 맞출지는 그대로 열려 있다.
  ③(새로고침 문구의 정책 소스 없음)도 그대로다 — 문구 `group_list_refreshing`은 Figma에만 있다.
  **함께 발견한 결함은 OQ-P-349로 갈랐다** — 이 라운드가 G-001의 토스트 자체를 걷어 우회를 굳혔을 뿐
  원인은 남아 있다. 등장 연출(400ms 스태거·재진입 생략)의 근거 부재는 OQ-P-357로 갈랐다.

### [2026-09-03] `YGScaffoldV2` 기본 토스트 자리가 상단 바와 같은 띠를 쓴다

- **ID**: OQ-P-349
- **출처**: `core/designsystem/screen/YGScaffoldV2.kt` — 토스트 호스트를 `Alignment.TopCenter` +
  `windowInsetsPadding(WindowInsets.statusBars)`로 놓아 상태바 바로 아래에 붙인다. 그런데 상단 바를
  가진 화면은 그 바도 같은 자리에서 시작한다(G-001의 `YGTopBarEmpty`는 상단 인셋을 자기가 흡수한다).
  둘 다 반투명이라 겹치면 서로 비쳐 보인다 — 토스트가 `Transparency.Black75`, 상단 바가
  `White75` + 블러다. **G-001에서 실기기로 재현했고**(새로고침 실패 토스트가 날짜·그룹 추가 칩 위에
  겹쳤다), 그 화면은 OQ-P-348이 토스트 자체를 걷어 우회했을 뿐 원인은 그대로다.
- **항목**: ① 스캐폴드 기본 자리를 쓰면서 상단 바를 가진 화면이 어디까지인지 — 후보는
  `GroupSettingRoute`·`GroupCreateRoute`·`GroupNickNameRoute`·`AppSettingRoute` 등
  `YGScaffoldV2(toastPolicy = …)`를 그대로 넘기는 화면 전부다. ② 고치는 자리 — 화면마다
  `YGToastHost`를 자기 상단 바 아래에 직접 얹을지(C-001 캔버스·C-101 카메라·C-102 갤러리가 이미
  그렇게 한다), 아니면 `YGScaffoldV2`가 호스트 위치를 슬롯으로 열지. **②는 OQ-P-167이 이미
  던져 둔 질문과 같다** — 거기서는 "화면이 자기 호스트를 가지면 그쪽이 이긴다"는 예외를 규약으로
  적을지가 쟁점이었는데, 이 결함은 그 예외가 **선택이 아니라 상단 바가 있으면 필수**임을 보여 준다.
- **상태**: 미해결 (G-001은 우회됐고 나머지 화면은 미확인 — 재현 여부부터 확인해야 한다)
  > 📌 **우회가 굳었다(2026-09-04, PR #440 develop 머지)** — G-001의 토스트·이펙트·문구가 삭제되면서
  > Route가 `toastPolicy`를 아예 안 넘긴다. **재현하던 유일한 화면이 재현 수단을 잃었다**는 뜻이라,
  > ①(어느 화면들이 같은 상태인가)을 확인할 자리가 그만큼 줄었다 — 남은 후보는 상단 바를 가진 채
  > 스캐폴드 기본 자리를 쓰는 설정·생성·닉네임·앱 설정 쪽이고 전부 미확인이다.
  > ②(고치는 자리)에는 재료가 하나 생겼다 — 같은 라운드가 `YGScaffoldV2`에 **덮개 슬롯**을 열었다.
  > 토스트 호스트가 아니라 로딩 덮개 쪽이지만, "스캐폴드가 자리만 정하고 무엇을 그릴지는 화면이
  > 정한다"는 형태의 선례라 호스트 위치를 슬롯으로 여는 안의 비용을 낮춘다.
- **해소 메모**: 위키 [[toast]] 공통 정책은 "위에서 아래로 내려와 노출"만 정하고 기준선을 말하지
  않는다. 화면 상단인지 상단 바 아래인지는 구현 규약 몫이므로, 정해지면
  [design-system](../architecture/design-system.md)의 `YGScaffoldV2` 절에 적는다.

### [2026-09-03] 스플래시 로고 애니메이션이 통째로 갈렸는데 본 사람도, 되돌아가는 것을 잡을 것도 없다

- **ID**: OQ-P-350
- **출처**: `feature/intro/impl` `res/raw/splash.lottie`(PR #444 develop 머지) — 앱을 여는 첫 화면의
  애니메이션이 새 로고 판으로 통째로 교체됐다. 코드는 한 줄도 안 바뀌었다(`R.raw.splash`·`SplashScreen`
  무변경). dotLottie 를 풀면 manifest 판본·애니메이션 id·프레임률·재생 길이·레이어 구성이 교체 전과
  같고 다른 것은 각 레이어의 패스와 키프레임 값이다 — 즉 **바뀐 것 전부가 눈으로만 보이는 결과**다.
  그런데 그 결과를 실기기로도 프리뷰로도 본 기록이 0건이고, 저장소 안에서 새 판을 설명하는 글은
  커밋 메시지뿐이다(디자인 출처·Figma 노드 기재 없음).
- **항목**: ① 새 판이 위키 [[스플래시-애니메이션]] A-001 정본의 연출(글자 일곱 개 순차 팝인 → 하단 획
  드로잉 → 홀드)을 그대로 그리는지 — 레이어 이름은 그 구조와 맞지만 **그림 자체는 대조되지 않았다.**
  ② 되돌아가는 회귀를 무엇이 잡는지 — 파일명도 구조값도 그대로라 옛 판을 다시 넣어도 아무것도
  빨갛게 되지 않는다. 구조값(프레임률·재생 길이·레이어 구성)만이라도 검사에 걸어 둘지.
  ③ 디자인 출처를 어디에 적을지 — 지금은 애셋 파일 하나가 근거의 전부다.
- **상태**: 미해결 (**실기기 1회로 ①이 갈린다** — 앱을 열기만 하면 된다)
- **해소 메모**: ①이 확인되면 [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인"의
  스플래시 절에 관측 결과를 적는다. ②는 OQ-P-307(폰트 바이너리 교체의 회귀 감지)과 같은 축이되
  **로띠는 열어서 읽을 수 있다**는 점이 다르다 — 폰트가 못 하는 구조 대조를 로띠는 할 수 있으므로,
  검사를 붙인다면 이쪽이 먼저다. 대기 길이 쪽은 OQ-P-229가 따로 본다.

### [2026-09-04] 푸시 문구·data 스키마·채널 id·딥링크가 서버 코드에만 있고 정책 근거가 없다

- **ID**: OQ-P-351
- **출처**: 서버 `NotificationMessageFactory.toppingPlaced` · `FcmNotificationSender`(`aa9cc9b`) ×
  [api/notification.md](../api/notification.md) "서버가 보내는 푸시" — 제목(`{그룹명} 파르페에 체리 얹을
  타이밍!`)·본문(`{닉네임}님이 새 토핑을 쌓았어요` / 작성자 탈퇴 시 `누군가 새 토핑을 쌓았어요`)과
  `data` 키 네 개(`type`·`route`·`groupId`·`date`)가 **서버 코드 한 곳에서 정해졌다.** 위키 정책 소스에
  알림 항목 자체가 없고(OQ-P-343), parfait 쪽에도 이 문구를 승인한 문서가 없다.
- **항목**: ① 문구가 기획 승인을 받은 것인지, 서버 구현이 임시로 정한 것인지 — 승인본이면 위키에
  정책 페이지가 서야 하고, 임시면 앱이 그 문구를 전제로 화면을 만들면 안 된다.
  ② `route=canvas` + `groupId` + `date` 조합이 앱 내비게이션의 **어느 목적지**로 떨어지는지 —
  `date`가 오늘이 아닐 수 있으므로 오늘 캔버스와 지난 캔버스 중 어디로 가는지가 갈린다.
  ③ `data` 키 이름·값(특히 `type`의 `TOPPING`)을 앞으로 늘릴 때 누가 관리하는지 — 지금은 서버 코드가
  유일한 정본이라 앱이 오타를 내도 아무 데서도 안 걸린다.
- **상태**: **부분 해소** (②가 코드로 답해졌다 — 앱이 목적지를 정했다. ①③ 잔존.
  ⚠️ **2026-09-06 정정: "등록 호출부가 0건이라 동작 영향 0"은 낡았다** — PR #450이 등록을 세션 축
  넷에 걸었으므로 **이 문구는 실제로 사용자에게 닿는다**)
  > ✅ **②를 앱이 정했다(2026-09-05, PR #446)** — `route=canvas` → `NavKeyCanvasMain(groupId)`,
  > `route=group` → `NavKeyGroupList`이고 **`date`는 안 읽는다.** `PushDeepLink.AddTopping`의 KDoc이
  > "알림이 가리키던 날짜가 아니라 항상 그 그룹의 최신 캔버스"라고 못 박으므로 **오늘/지난 캔버스의
  > 갈림은 "항상 오늘"로 닫혔다.** ⚠️ 그런데 그 선택의 근거도 코드 한 줄이고, 계약 표는 여전히
  > `date`로 도달할 것을 요구한다 → **OQ-P-359 신설**.
  > 📌 **③의 대가가 코드로 옮겨왔다** — `type`·`route`·`groupId` 세 키 이름과 `canvas`·`group`,
  > `TOPPING`·`REMIND_AM`·`REMIND_PM` 값이 이제 **앱 프로덕션 코드에도 문자열 상수로** 있다
  > (`PushDeepLinkIntent.kt`·`PushDeepLinkParser`). 서버가 값을 바꾸면 조용히 `null`이 되어
  > **딥링크가 평범한 실행과 구분되지 않는다**(OQ-P-354와 같은 축이고 복제면이 넷째로 늘었다).
  > ①(문구 승인 여부)은 그대로다 — 앱은 서버 문구를 그대로 표시하므로 승인 없이 사용자에게 닿는다.
- **해소 메모**: ①은 정책 소관이라 위키 [[open-questions]]와 갈리는 자리다. ②③이 정해지면
  [api/notification.md](../api/notification.md) "이 계약이 앱에 요구하는 것" 표와
  [navigation-flow](../architecture/navigation-flow.md)에 적는다.

### [2026-09-04] 서버가 지정한 알림 채널 id를 앱이 만들지 않으면 조용히 안 보인다

- **ID**: OQ-P-352
- **출처**: 서버 `FcmNotificationSender`의 `ANDROID_CHANNEL_ID`(`parfait_default`, `aa9cc9b`) ×
  [api/notification.md](../api/notification.md) — 서버가 `AndroidNotification.channelId`를 못 박아
  보내므로, Android 8 이상에서 **같은 id의 채널이 앱에 없으면 알림이 표시되지 않는다.** 그런데
  발송 쪽에서는 그것이 성공(`SENT`)으로 찍힌다. develop 에는 채널 생성 심볼이 0건이다
  (2026-08-22 PR #325가 FCM 축을 걷어냈다, OQ-P-341).
- **항목**: ① 앱이 이 id로 채널을 만들지, 서버가 앱이 정한 id로 바꿀지 — **한쪽이 상수를 고치면
  다른 쪽이 조용히 깨지는 자리**라 어느 쪽이 정본인지 정해야 한다. ② 채널의 사용자 표시 이름·중요도를
  누가 정하는지(서버는 id만 보내고 나머지는 앱 몫이다). ③ **이 어긋남을 무엇이 잡는지** — 서버 테스트도
  앱 테스트도 상대편 상수를 모르므로 실기기로 받아 보기 전에는 드러나지 않는다.
- **상태**: **부분 해소** (① 앱이 서버 id를 따랐다 — 2026-09-05 PR #447 / ②는 앱이 정했고 근거가 없다 /
  ③ 잔존. ⚠️ **2026-09-06 정정: "등록된 토큰이 0건이라 도달 불가"는 낡았다** — PR #450으로 등록이
  붙어 발송이 실제로 나간다)
  > 📌 **③에 재현 수단이 생겼다(2026-09-04, PR #451 `2b1dce3a`)** — `http/fcm-test.http` 3번이
  > **일부러 다른 채널 id로 쏘는 대조군**이고, 응답이 200인데 알림이 안 뜨는 상황을 그대로 만든다.
  > 다만 ③이 물은 것은 "무엇이 자동으로 잡는가"이고 **그 답은 여전히 없다** — 이 요청도 사람이
  > 손으로 쏴야 하고, 앱에 수신부·채널이 없어 지금은 절반만 돌아간다(발송만 되고 받을 쪽이 없다).
  > 두 상수를 대조하는 검사는 서버에도 앱에도 없다.
  > ✅ **①이 코드로 답해졌다 — 앱이 따라갔다(2026-09-05, PR #447)** — `PUSH_NOTIFICATION_CHANNEL_ID`가
  > `parfait_default`이고 `BaseApplication.createPushNotificationChannel`이 앱 시작에 만든다.
  > **정본은 서버라는 뜻이지만 그 사실을 적어 둔 곳이 양쪽 코드의 상수뿐**이라 ③(무엇이 어긋남을
  > 잡는가)은 그대로다 — 오히려 같은 문자열을 가진 자리가 셋(서버·`fcm-test.http`·앱)이 됐다.
  > 📌 **②도 앱이 정했다** — 중요도 `IMPORTANCE_HIGH`, 표시 이름 `파르페 알림`, 설명
  > `새 토핑, 캔버스 마감 안내 알림`(`strings.xml`). ⚠️ **설명이 서버가 안 보내는 알림을 약속한다** —
  > 캔버스 마감 알림은 발송 코드가 없다(OQ-P-343 ①). 이 문구를 승인한 정책 문서도 없다(OQ-P-351 ①).
  > ⚠️ **채널이 있어도 표시되지 않는 구간이 새로 드러났다** — 권한을 묻는 코드가 없어 Android 13+
  > 에서는 채널과 무관하게 막힌다(OQ-P-358).
- **해소 메모**: 정해지면 [api/notification.md](../api/notification.md) "이 계약이 앱에 요구하는 것"에
  정본 표기를 남기고, 앱이 채널을 만들 때 그 상수 옆에 이 문서 포인터를 둔다. ①은 OQ-P-341(FCM 축을
  되살릴지)이 먼저 답해져야 실행에 옮길 수 있다.

### [2026-09-04] 푸시 TTL 6시간이 재시도 백오프 일정과 겹친다

- **ID**: OQ-P-353
- **출처**: 서버 `NotificationMessageFactory`(TTL 6시간) × `OutboxBackoff`(1분 → 5분 → 15분 → 1시간 →
  6시간, 최대 5회, `aa9cc9b`) — 발송이 뒤쪽 단계까지 밀리면 재시도 간격 자체가 TTL과 같은 크기가 된다.
  FCM 은 TTL 이 지난 메시지를 버리므로 **재시도가 성공해도 사용자에게 도달하지 않는 구간**이 생긴다.
  서버 코드는 두 값을 서로 참조하지 않는다.
- **항목**: ① 늦게 도착하는 토핑 알림에 값이 있는지 — 없다면 재시도 상한을 TTL 안으로 줄이는 편이
  맞다. ② 있다면 TTL 을 늘릴지, 만료된 행을 재시도에서 빼고 취소로 처리할지.
  ③ 지금은 만료로 버려진 것과 발송 실패가 구분되지 않는다 — `last_error` 만으로 사후 판별이 되는지.
- **상태**: 미해결 (**관측 수단 없음** — 앱 수신부가 없어 도달·미도달을 확인할 방법이 지금은 없다)
- **해소 메모**: 서버 쪽 튜닝값 문제라 앱이 고칠 것은 없다. 다만 "보냈는데 안 왔다"의 원인 후보가
  하나 늘었으므로, 앱에 수신부가 붙은 뒤 재현되면 이 항목부터 본다.
  [api/notification.md](../api/notification.md) "몇 번, 얼마나 늦게 오는가"에 함께 적혀 있다.

### [2026-09-04] 서버 발송 페이로드가 앱 저장소에 복제됐는데 갈라짐을 세는 축이 없다

- **ID**: OQ-P-354
- **출처**: `http/fcm-test.http`(TJYG-Android, PR #451 `2b1dce3a`) × [api/notification.md](../api/notification.md)
  "어떤 페이로드가 가는가" × 서버 `NotificationMessageFactory`·`FcmNotificationSender`(`aa9cc9b`) —
  같은 값(문구 2종·`data` 키 4종·채널 id `parfait_default`·TTL 6시간·APNs 헤더)이 **세 곳**에 있다.
  파일 머리말이 "서버 코드를 정본으로 삼는다"고 적지만, 그 대조를 무엇이 언제 하는지는 정해지지 않았다.
- **항목**: ① 서버 계약이 바뀔 때 이 파일을 누가 고치는지 — 엔드포인트 복제는 [2026-08-04] `http/`↔`api/`
  이중 관리 항목(OQ-P-092)이 안고 있고 커버 셈(`N/29`)이 갈라짐을 드러내 줬는데,
  **푸시는 셀 엔드포인트가 없어 같은 방식이 안 통한다.**
  ② 이 파일을 계약 서술 없이 "쏘는 방법"으로만 깎고 값은 `api/notification.md`를 보게 할지.
  ③ 대조군 요청(3~6번)은 **일부러 계약과 다르게** 쏘는 것이라, 나중에 읽는 사람이 정본과 구분할 수
  있는 표식이 파일 주석뿐인 것이 충분한지.
- **상태**: 미해결, **복제면이 넷이 됐다** (2026-09-05 PR #446·#447로 **앱 프로덕션 코드**가
  같은 값들을 갖게 됐다 — 채널 id `parfait_default`(`PUSH_NOTIFICATION_CHANNEL_ID`)와
  `data` 키·값 문자열(`PushDeepLinkIntent.kt`·`PushDeepLinkParser`). 이제 어긋나면 무해하지 않다 —
  키가 안 맞으면 딥링크가 **평범한 실행처럼 조용히 무시되고**, 채널 id가 안 맞으면 알림이 안 뜬다)
- **해소 메모**: OQ-P-092와 같은 자리에서 함께 정한다 — 둘 다 "계약 서술을 어디에 두는가"이고,
  이 항목은 **셈으로 안 잡히는 복제**라는 점만 다르다. 정하면
  [api/README.md](../api/README.md) "계약을 실제로 확인하는 법"에 반영한다.

### [2026-09-04] 캔버스 토핑은 한 장만 실패해도 전부 막는데 그 규칙의 근거가 코드에만 있다

- **ID**: OQ-P-355
- **출처**: `feature/groups/canvas/impl/util/CanvasLoadState.kt#canvasLoadState`(PR #440 develop 머지) ×
  위키 [[토핑]] — 접기 규칙이 **한 장이라도 실패하면 전체 실패**이고, 그때 캔버스 전체가 다시 시도
  덮개로 덮여 아무것도 못 본다. KDoc은 "토핑을 하나라도 못 보면 캔버스를 쓸 이유가 없다는 기획
  판단"이라 적지만 그 판단을 담은 문서가 없다. 위키가 부분 실패를 정한 자리는 **G-001뿐**이고
  (조회 실패 → 물음표 1종), C-001 캔버스 토핑의 실패 표현 조항은 아예 없다. 같은 라운드의 G-001은
  반대로 간다 — 이미지를 기다리지 않고 실패한 것만 폴백으로 남긴다.
- **항목**: ① 전면 차단이 맞는지 — 맞다면 위키에 C-001 조항이 서야 하고, 아니면 G-001처럼 실패한
  토핑만 대체 그래픽으로 두는 편이 같은 앱 안에서 일관된다. ② **문구 넷이 코드에서 먼저 확정됐다**
  (`canvas_main_loading_title`·`_description`·`canvas_main_load_error_title`·`_description`, 버튼 문구
  포함 다섯) — 디자인 프레임 이름(`C-001-Loading`/`C-001-Error`)이 주석으로 있으나 위키에 대응 정책
  소스가 없다(G-001 에러 문구와 같은 성격). ③ **3상태 축이 둘로 복제됐다** —
  `YGCanvasBackgroundState`(디자인시스템 public)와 `CanvasLoadState`(feature internal)가 값이 같고
  화면이 둘 사이를 변환한다. 한쪽에 상태가 하나 늘면 다른 쪽과 변환 함수를 함께 고쳐야 한다.
- **상태**: 미해결 (**동작 중** — develop 코드이고 실기기 확인은 0회다)
- **해소 메모**: ①이 먼저다. 정해지면 [c001-canvas-today-detail 스펙](../superpowers/specs/archive/2026-08-17-c001-canvas-today-detail.md)
  「토핑 렌더」의 갱신 블록에 근거를 적고 위키 쪽 조항을 요청한다. ②는 OQ-P-113 ③과 같은 축이다 —
  코드가 먼저 확정한 문구를 정책으로 역수집할지 디자인을 정본으로 인정할지. ③은 축을 하나로 모을지
  변환을 남길지의 문제이고, 모은다면 방향은 디자인시스템 쪽이다(표시 규격의 소유는 그쪽이라는 것이
  [module-structure](../architecture/module-structure.md)가 이미 두 번 고른 기준이다).

### [2026-09-04] 캔버스 다시 시도가 그림은 되살리는데 판정 마스크는 안 되살린다

- **ID**: OQ-P-356
- **출처**: `CanvasToppingLayer#rememberToppingHitEntries`(`retryKey`) × `ToppingAlphaMaskCache.kt#rememberToppingAlphaMasks`
  (PR #440 develop 머지) — 재시도는 `retryKey`를 올려 표시용 요청의 캐시를 건너뛰지만, 알파 마스크는
  **url 목록만 보는 `LaunchedEffect`**라 목록이 그대로면 다시 요청되지 않는다. 먼저 실패한 마스크는
  그 화면에 머무는 동안 비어 있고, 마스크가 없으면 판정이 **사각형으로 떨어진다**
  (`ToppingHitTarget`의 설계된 폴백이라 못 누르게 되지는 않는다). 코드 KDoc이 "알파 마스크는 여기
  딸려 오지 않는다"고 사실만 적는다.
- **항목**: ① 재시도가 마스크까지 데려갈지 — 데려가려면 마스크 쪽에도 재시도 키가 필요하고, 실패한
  것만 다시 부르는 것과 전부 다시 부르는 것이 갈린다(마스크는 프로세스 전역 캐시라 성공한 것을 다시
  부르면 낭비다). ② 아니면 사각형 폴백을 허용 범위로 인정할지 — 그러면 **재시도로 그림이 돌아온
  화면에서 투명한 자리가 눌린다**는 것을 알고 두는 것이다. ③ 판정이 사각형으로 떨어졌다는 것을
  아무 데서도 셀 수 없다(로그도 없다).
- **상태**: 해소됨 (①로 결정·구현됨 — PR #464 develop 머지,
  [ADR-0030](../adr/0030-topping-outline-distance-field.md))
- **해소 메모**: 마스크 캐시가 `core:ui`의 거리판 캐시(`ToppingOutlineCache.kt`)로 옮겨 가며 캐시 키가
  `"$retryKey|$model"`로 바뀌었다 — `retryKey`가 올라가면 같은 모델이라도 새 캐시 항목을 만들어
  재시도 뒤 그림과 함께 테두리·판정이 되살아난다. ②·③이 다루던 "사각형 폴백을 허용할지" 논의는
  ①로 닫히며 대상을 잃는다.

### [2026-09-04] G-001 파르페가 쌓이는 연출의 간격·생략 조건에 정책 근거가 없다

- **ID**: OQ-P-357
- **출처**: `GroupListScreen.kt#STAGGER_STEP_MILLIS`·`rememberStaggeredRevealState`(PR #440 develop 머지) —
  그룹 토핑이 400ms 간격으로 아래에서 위로 하나씩 나타나고 크림이 그 높이를 따라 붙는다. 간격 값의
  근거로 적힌 것은 코드 주석 한 줄("크림이 내려오는 시간과 맞춘다")이고, **재진입에는 연출을 생략**하는
  조건(`groupList == null`)도 코드가 정했다. 위키 [[무한-파르페-그리드]]에는 등장 연출 조항이 없다 —
  배치 좌표와 상태 규칙만 있다.
- **항목**: ① 이 연출이 정책인지 구현 재량인지 — 정책이면 위키에 조항이 필요하고, 그때 간격·순서
  (아래에서 위로)·크림 동반이 함께 정해져야 한다. ② 그룹이 많을 때의 총 대기 — 12명 상한은 그룹 수
  상한이 아니라 항목이 늘수록 마지막 토핑까지의 시간이 선형으로 는다. 상한을 둘지 정해지지 않았다.
  ③ 생략 조건이 "덮개를 켜는 조건"과 같은 값을 두 곳에서 각각 판정한다(OQ-P-330 ①과 같은 자리).
- **상태**: 미해결 (**동작 중** — 실기기 확인 0회이고, 육안으로 어색한지 잰 사람이 없다)
- **해소 메모**: ①이 정해지면 [g001 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 「토핑 배치」의
  갱신 블록과 정책 대조 표에 행을 더한다. ②는 실기기 관측이 선행이다.

### [2026-09-05] 알림 권한을 묻는 코드가 없어 Android 13+에서는 푸시가 표시되지 않는다

- **ID**: OQ-P-358
- **출처**: `app/src/main/AndroidManifest.xml`(`POST_NOTIFICATIONS` **선언**) ×
  `push/ParfaitFirebaseMessagingService#showNotification`(`ContextCompat.checkSelfPermission` 실패 시
  조용히 `return`) × develop 전체 검색 — `ActivityResultContracts.RequestPermission`으로 이 권한을
  요청하는 자리가 **한 곳도 없다**(카메라·갤러리 권한에는 있다). 2026-08-22 PR #325가 걷어낸 것 중
  권한 요청만 돌아오지 않았다([ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정).
- **항목**: ① 언제 묻는지 — 미머지 브랜치 `feature/push-notification-permission`은 **그룹 생성·참여
  직후**로 답해 두었고(OQ-P-341 ③), 그 브랜치가 들어오기 전까지 develop은 묻지 않는다.
  ② 거부·미허용 상태를 사용자에게 알릴지 — 지금은 표시 실패가 **로그에도 안 남는다**.
  ③ Android 12 이하에서는 권한이 필요 없어 같은 코드가 다르게 동작하는데, 그 차이를 확인한 사람이 없다.
- **상태**: **해소됨** (2026-09-05, PR #450 develop 머지)
  > ✅ **①이 답해졌다** — `NotificationPermissionGate`가 **A-004·A-005 완료 직후, 캔버스 진입 전**에
  > 묻는다. 이미 허용돼 있으면 안내 없이 통과하고, 영구 거부면 앱 설정으로 보낸다.
  > ✅ **③도 함께 닫혔다** — API 33 미만은 권한이 정의돼 있지 않아 `checkSelfPermission`이 **항상 거부를
  > 답하는데** 알림은 기본으로 켜져 있다. `NotificationPermissionManager`가 버전으로 먼저 갈라
  > **허용으로 본다.** 위 출처가 지목한 `showNotification`이 정확히 그 함정을 밟아 **그 기기군의
  > 포그라운드 알림을 전부 버리고 있었고**, 같은 판정으로 교체되며 고쳐졌다.
  > ⚠️ **②(거부·미허용을 사용자에게 알릴지)는 답해지지 않았다** — 표시 실패는 여전히 조용하다.
  > 다만 이제 안내 자체가 있으므로 **성격이 "알릴 수단이 없다"에서 "안내를 지나친 뒤를 안 알린다"로
  > 바뀌었고**, 노출 횟수 정책 미정(OQ-P-370)과 같은 자리에서 본다.
- **해소 메모**: [api/notification.md](../api/notification.md) "이 계약이 앱에 요구하는 것"의 마지막
  행과 미결 절, [ADR-0013](../adr/0013-firebase-fcm-crashlytics.md) 되살림 정정을 함께 고쳤다.
  설계 정본은 [push-notification-permission-and-device-token 스펙](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md) 결정 3·4.

### [2026-09-05] 앱 수신부가 계약의 `date`를 버리고 중복 수신을 알림 두 개로 쌓는다

- **ID**: OQ-P-359
- **출처**: `push/PushDeepLinkIntent.kt`(extras 키 `type`·`route`·`groupId` — `date` 없음) ·
  `domain.model.push.PushDeepLink.AddTopping` KDoc("알림이 가리키던 날짜가 아니라 항상 그 그룹의 최신
  캔버스로 연다") · `ParfaitFirebaseMessagingService#showNotification`
  (`notificationId = message.messageId?.hashCode()`) × [api/notification.md](../api/notification.md)
  "이 계약이 앱에 요구하는 것" — 계약은 `date`로 캔버스에 도달할 것과 **같은 알림을 두 번 받아도 부작용이
  없을 것**을 요구한다.
- **항목**: ① 알림이 가리킨 날짜의 캔버스로 갈지, 늘 오늘로 갈지 — 재시도가 최대 6시간이고 캔버스 마감이
  03시라 **두 값이 갈리는 구간이 실제로 있다**(위키 [[캔버스-마감-스케줄]]). "항상 오늘"이 맞다면
  서버가 `date`를 보내는 이유가 무엇인지 정해야 한다. ② 중복 알림을 접을지 — `dedup_key`는 서버 생산
  쪽 멱등일 뿐이라 재시도로 온 같은 알림은 `messageId`가 달라 **알림 두 개로 쌓인다.** 접으려면
  알림 id를 `data`에서 끌어와야 한다(예: 그룹·날짜 조합). ③ 어느 쪽도 지금은 확인할 수단이 없다 —
  `http/fcm-test.http`를 두 번 쏘면 재현되지만 실행 기록이 0건이다.
- **상태**: 미해결 (⚠️ **2026-09-06 정정: 더 이상 도달 불가가 아니다** — PR #450으로 등록 호출부가
  넷 생겨 알림이 실제로 온다. ①②가 곧바로 사용자에게 보이는 상태다)
- **해소 메모**: ①이 정해지면 [api/notification.md](../api/notification.md) "이 계약이 앱에 요구하는 것"
  표와 [api/conventions.md](../api/conventions.md) "Android 불일치"의 두 행을 함께 닫는다. ②는 앱만
  고치면 되므로 ①과 독립이다.

### [2026-09-05] 콜드 스타트 딥링크가 스플래시 부트스트랩의 백스택 리셋과 겹친다

- **ID**: OQ-P-360
- **출처**: `MainActivity#onCreate`(`consumePushDeepLink(intent)`를 `setContent` 앞에서 부른다) ·
  `MainRoute`의 `LaunchedEffect(Unit)` 수집 · `PushDeepLinkEventBusImpl`(`Channel(CONFLATED)`,
  구독 전에 발행한 것이 남는다 — `PushDeepLinkEventBusImplTest`가 잠근다) ×
  [navigation-flow](../architecture/navigation-flow.md) "앱 진입 체인" — 부트스트랩이 끝나면
  `SplashRoute`가 `replaceAll(...)`로 백스택을 갈아 끼운다. **코드 주석은 딥링크 수집이 "로그인·부트스트랩이
  끝난 뒤"에 시작한다고 적지만, `LaunchedEffect(Unit)`은 앱 루트의 첫 컴포지션에 곧바로 시작한다** —
  그 시점의 백스택은 `NavKeySplash` 하나다.
- **항목**: ① 딥링크가 먼저 쌓이고 리셋이 뒤따르면 **딥링크 목적지가 백스택째 걷힌다** — 실제로 그
  순서가 나는지, 아니면 부트스트랩이 늘 먼저 끝나는지 아무도 확인하지 않았다. ② 이동 수단이 `goTo`라
  딥링크로 연 화면 아래에 **그때까지의 백스택이 남는다**(다른 경계는 `replaceAll` 관용구를 쓴다) —
  로그인 화면 위에 캔버스가 얹히는 상태가 허용 범위인지. ③ 로그인 안 된 상태로 탭했을 때 무엇을 보여줄지
  — 지금은 목적지로 그냥 간다.
- **상태**: **부분 해소** (①③ 해소, ② 잔존). ✅ **develop 머지**(2026-09-06, PR #456 `618ead927`)
  > ✅ **①은 순서를 갈라 보는 대신 구간 자체를 없앴다** — 수집이
  > `snapshotFlow { navigator.backStack.lastOrNull() }.first { it != NavKeySplash }`로 스플래시 이탈을
  > 기다린 뒤 소비하므로, `replaceAll`이 딥링크를 걷어낼 창이 남지 않는다. ⚠️ **수정 전에 그 순서가
  > 실제로 났는지는 관측하지 않았다** — 수정 뒤 콜드 스타트 딥링크가 캔버스에 도착하는 것만 확인했다.
  > ✅ **③은 "버린다"로 정해졌다** — `AddTopping`·`GroupList` 두 목적지 모두 로그인이 필요하므로,
  > 대기 뒤에 `HasActiveSessionUseCase`가 false면 딥링크를 버린다. 판정 근거는
  > `BootstrapSessionUseCase`와 같은 `AuthRepository.hasSession`이다 — 근거가 갈리면 부트스트랩은
  > 로그인으로 보냈는데 이쪽은 통과시키는 어긋남이 생긴다. **로그인을 마쳐도 원래 목적지로 이어가지
  > 않는다.** 판정이 대기 뒤에 있는 이유는 부트스트랩이 인증 거절을 받으면 토큰을 지우기 때문이다.
  > 📌 **②는 그대로다** — 이동 수단은 여전히 `goTo`라 딥링크로 연 캔버스 아래에 백스택이 남는다.
  > 다만 ③이 닫히면서 **"로그인 화면 위에 캔버스가 얹힌다"는 갈래는 사라졌다.**
  > ⚠️ **이 항목이 근거로 삼았던 "등록 호출부가 0건"은 틀렸다** — PR #450(`da26d084a`)이
  > `BootstrapSessionUseCase`·`LoginWithKakaoUseCase`·`SignUpUseCase`·`onNewToken` 넷에
  > `DeviceTokenRegistrar.register()`를 붙였고, 권한 요청도 `NotificationPermissionGate`가
  > `GroupCreateRoute`·`GroupNickNameRoute` 두 자리에서 한다. **같은 문구가 OQ-P-351·OQ-P-352·
  > OQ-P-359의 상태에도 남아 있었고 2026-09-06 감사가 셋 다 걷었다.**
  > 📌 **같은 PR이 딥링크 파싱 축을 enum으로 옮겼다** — `route`는 `PushNotificationRouteType`,
  > `type`은 `PushNotificationType`이 각자 `key`로 물고, `groupId`를 양수로 읽는 규칙은
  > `PushDeepLink.AddTopping.parse`가 든다. **문자열 복제면이 줄어든 것이지 사라진 것은 아니다**
  > (서버 값이 바뀌면 여전히 조용히 `null`이 된다 — OQ-P-351 ③).
- **해소 메모**: 검증은 콜드/웜 × 로그인/비로그인 네 시나리오의 실기기 수동 확인이다. 자동 테스트는
  없다 — `app` 모듈에 `androidTest` 소스셋이 없고, 이 로직은 `LaunchedEffect` 안의 UI 배선이라
  JVM 단위 테스트로 잡히지 않는다. ②를 "리셋한다"로 정하면 그 자리의 관용구가 `replaceAll`로 바뀐다.

### [2026-09-05] 앱이 "FCM 페이로드 스펙 v1"을 근거로 서버에 없는 알림 둘을 먼저 구현했다

- **ID**: OQ-P-361
- **출처**: `push/PushDeepLinkParser` KDoc("FCM 페이로드 스펙 v1 §3") ·
  `ParfaitFirebaseMessagingService` KDoc("FCM 페이로드 스펙 v1은 세 알림(P-01/P-02/P-03) 모두
  `notification` 블록을 항상 함께 보낸다") · `domain.model.push.PushNotificationType`
  (`TOPPING`·`REMIND_AM`·`REMIND_PM`) × 서버 `NotificationMessageFactory`(문구 조립 함수 1개) ×
  [api/notification.md](../api/notification.md) — **그 스펙 문서가 parfait에도 위키에도 서버 저장소에도
  없다.** 앱은 `route=group`과 리마인드 2종을 파싱·라우팅까지 갖췄는데 **서버에는 그것을 보내는 코드가
  없다**(OQ-P-343 ①의 "나머지 둘").
  > 📌 **2026-09-08 — 서버가 따라붙었다**(`4789557`). `ReminderType(MORNING→"REMIND_AM",
  > EVENING→"REMIND_PM")`과 `dailyReminder`의 `data = mapOf("type" to type.dataType, "route" to "group")`이
  > **앱이 먼저 만들어 둔 문자열과 정확히 같다.** 그러므로 ③("아무도 안 보내므로 테스트도 실기기도
  > 지나가지 않는다")은 해소됐다. **①과 ④는 그대로다** — 서버 `ReminderType` KDoc까지
  > "페이로드 스펙 v1 §3.1"을 인용하는데 **그 문서는 여전히 어느 저장소에도 없다.**
- **항목**: ① 그 스펙이 실재하는지 — 노션 등 저장소 밖에 있다면 어디인지 적어야 하고, 구두 합의였다면
  근거가 코드 주석뿐이다. ② P-02/P-03의 트리거·문구·발송 시각을 누가 정하는지 — 채널 설명 문구
  (`새 토핑, 캔버스 마감 안내 알림`)는 이미 마감 알림을 약속하고 있다. ③ 앱이 서버보다 앞서 구현한
  경로를 무엇이 지키는지 — `route=group`은 **아무도 안 보내므로 테스트도 실기기도 지나가지 않는다.**
  ④ 이 저장소의 `parfait/api/notification.md`가 그 스펙의 자리를 대신할지.
- **상태**: **부분 해소** (2026-09-08 — 서버가 두 알림을 실제로 보내기 시작해 ③은 닫혔다.
  ①·④는 미해결: **스펙 문서 실물이 없다**)
- **해소 메모**: ①이 먼저다 — 문서가 있으면 `raw/`로 들여 위키가 흡수하고(정책 소관), 없으면
  [api/notification.md](../api/notification.md)가 정본 자리를 맡는다는 것을 명시한다. ②는 정책 소관이라
  위키 [[open-questions]]와 갈리는 자리이고 OQ-P-343 ①과 같은 물음이다.
  ⚠️ 함께 발견: `ParfaitFirebaseMessagingService` KDoc의 `toPushDeepLinkOrNull` 링크가 옛 패키지
  (`com.teamyg.parfait.pushdeeplink`)를 가리킨다 — 같은 PR이 `push`로 줄이면서 남은 자국이다.

### [2026-09-05] FCM이 등록 토큰을 FID로 갈아타는데 앱·서버가 함께 움직여야 한다

- **ID**: OQ-P-362
- **출처**: `firebase-messaging` 25.1.2 소스 원문 — `FirebaseMessaging.getToken()`·`deleteToken()` 과
  `FirebaseMessagingService.onNewToken()` 이 **전부 `@Deprecated`** 이고 대체가 FID 기반
  `register()`·`onRegistered(installationId)` 다(25.1.0 부터). **develop 이 쓰는 API 가 정확히 그
  셋이다**(`FirebaseDeviceTokenProvider`·`ParfaitFirebaseMessagingService`, 2026-09-05 PR #450 머지 —
  둘 다 `@Suppress("DEPRECATION")` 과 전환 근거 주석을 달고 있다).
- **항목**: ① 언제 옮길지 — **선행 조건은 서버다**(아래 상태 참고). ② 서버가 Admin SDK 를
  9.10.0 이상으로 올리고 `setFid` 로 바꾸는 회차를 언제 잡을지.
- **상태**: 미해결 (**급하지 않다** — Firebase 문서가 두 방식을 "fully co-supported" 로 적고 제거
  일정이 없다. 기존 등록 토큰은 계속 동작한다)
  > ⚠️ **앱이 절반만 옮길 수 없다.** 두 API 는 매니페스트 플래그
  > `firebase_messaging_installation_id_enabled` 로 갈리고 **서로 배타적**이다 — 플래그가 없으면
  > `register()` 가 `IllegalStateException` 을 던지고, 있으면 반대로 `getToken()` 이 던진다.
  > 그래서 이 미결은 앱 단독으로 닫을 수 없다.
  > ✅ **②가 답해졌다(2026-09-05, jar 대조).** `firebase-admin` **9.9.0 의 `Message.Builder` 에는
  > `setFid` 가 없고, 9.10.0 에는 있다.** 서버는 9.9.0 이므로 **앱만 옮기면 FID 가 `setToken`
  > 자리에 들어가 모든 발송이 실패한다.** 순서는 하나뿐이다 — ① 서버가 9.10.0 으로 올려 `setFid`
  > 로 바꾸고 ② 그 뒤 앱이 플래그를 켠다. 앱 플래그를 먼저 켜면 `getToken()` 이 던져 되돌아갈
  > 중간 상태가 없다.
  > 📌 **앱 쪽 전환 비용은 생각보다 작다** — `FirebaseInstallations.getId()` 로 FID 를 당겨올 수
  > 있어 지금의 pull 모델이 그대로 성립한다. `DeviceTokenRegistrar` 와 세션 트리거 넷은 남고
  > `FirebaseDeviceTokenProvider` 구현·`onNewToken`→`onRegistered`·매니페스트 플래그만 바뀐다.
  > (초판이 "값을 당겨오는 자리가 없어져 구조가 통째로 바뀐다"고 적었으나 사실이 아니다.)
  > 📌 브랜치는 등록 토큰 축에 남기로 하고 근거·전환 조건을
  > [specs/2026-09-05-push-notification-permission-and-device-token](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md)
  > 결정 6에 적었다. 같은 라운드에 **토큰 계약을 비널로 좁혔다** — `getToken()` 은 `Task<String>` 이고
  > 미발급을 `Task` 실패로 주므로 `currentToken(): DeviceToken?` 의 `null` 분기가 도달하지 않는
  > 경로였다(그것을 검증하던 테스트도 걷었다).
- **해소 메모**: 전환을 결정하면 **서버 delta 와 같은 회차에** 본다 — 앱의 매니페스트 플래그와
  서버의 발송 필드가 동시에 바뀌어야 한다. ADR-0013 에 정정으로 적고
  [api/notification.md](../api/notification.md) 등록 절의 `token` 필드 설명도 함께 고친다.
  다시 볼 조건 셋: ① 서버의 `fid` 지원 확인 ② Firebase 의 등록 토큰 제거 일정 발표
  ③ deprecated API 의 실제 동작 중단.
  > 📌 **브랜치가 develop 이 됐다(2026-09-05, PR #450)** — 이 미결이 가리키던 코드는 이제 미머지가
  > 아니라 **출시 경로에 있다.** 판단(등록 토큰 축에 남는다)과 선행 조건(서버 Admin SDK 9.10.0)은
  > 그대로이고, 바뀐 것은 **어긋났을 때의 대가**다: 전에는 브랜치 하나가 못 나가는 것이었고 지금은
  > 발송이 전부 실패하는 것이다. 서버 delta 회차에 `FcmNotificationSender` 와 Admin SDK 판을 함께 본다.

### [2026-09-05] 튜토리얼이 실제 화면 대신 목업 스크린샷을 덮는다 — 어긋날 길과 정책 근거

- **ID**: OQ-P-363
- **출처**: `YGTutorialOverlay`·`CanvasTutorialStep`(PR #449 develop 머지) — `imageResource`가
  **딤까지 구워진 알파 없는 풀스크린 목업 PNG**이고 그 한 장이 실제 화면을 통째로 덮는다
  (`img_canvas_tutorial_1~3`·`img_upload_tutorial`·`img_segmentation_tutorial`, `drawable-xxhdpi` 단일 밀도).
  강조할 자리만 뚫은 오버레이가 아니다.
- **항목**: ① **화면이 바뀌어도 그림은 안 따라온다** — 안내가 가리키는 버튼이 옮겨 가거나 문구가
  바뀌면 목업만 낡는데, 어긋남을 잡는 수단이 없다(계측 테스트도 없다). ② 통짜 스크린샷이라
  **기기 폭·글자 크기·다크 테마**에 맞춰 다시 그려지지 않는다 — `ContentScale.Crop`이라 폭이 다른
  기기에서는 잘린다. ③ **정책 근거가 없다** — 위키에 튜토리얼 조항 자체가 없고, 노출 조건(설치 후
  화면별 첫 진입 1회)·문구·장 수·카드 위치가 전부 코드에만 있다. ④ 에셋이 큰 편인데(업로드
  튜토리얼 한 장이 이 저장소 최대 크기 드로어블이다) 밀도별 세트가 아니라 xxhdpi 하나다.
- **상태**: 미해결 (**동작은 의도대로** — 어긋남과 근거 부재의 문제다)
- **해소 메모**: ③은 위키 판단이 선행이다 — 정책으로 확정되면 위키에 조항을 만들고 여기는 구현
  소관만 남긴다. ①②는 [design-system](../architecture/design-system.md) 「튜토리얼 4종」 항목에
  대응 규칙을 적는다(뚫는 방식으로 바꿀지, 목업을 유지하고 갱신 책임을 명시할지).

### [2026-09-05] 미리보기 NavKey가 캐시 파일 경로를 나른다 — 값이 썩을 수 있다

- **ID**: OQ-P-364
- **출처**: `NavKeyCanvasImageSave(imagePath, date)`(PR #445 develop 머지) — 미리보기가 그릴 비트맵을
  키에 실을 수 없어(NavKey는 `@Serializable`) 캔버스 메인이 캡처를 캐시에 굽고 **절대경로만** 넘긴다.
  `date`도 도메인 타입이 아니라 `LocalDate.toString()` 문자열이고 받는 쪽이 `LocalDate.parse`로 되돌린다
  (canvas `:api`가 kotlinx-datetime을 쓰지 않는다).
- **항목**: ① **키가 복원되는 시점에 그 파일이 있으리라는 보장이 없다** — 프로세스 사망 뒤 복원이나
  OS의 캐시 정리를 지나면 경로만 살아 돌아온다. 지금은 미리보기가 `AsyncImage`로 그리다 조용히 비고,
  확정하면 읽기 실패 토스트로 끝난다(크래시는 아니다). ② `date`를 문자열로 나르는 것이 관용구인지 —
  `:api` 모듈에 날짜 의존을 들이지 않으려는 판단이지만, 파싱 실패는 `LocalDate.parse`가 던지고
  아무도 잡지 않는다. ③ 같은 형태의 화면이 또 생기면 "캐시 경로를 키에 싣는다"를 규약으로 올릴지.
- **상태**: 미해결 (**동작 영향 낮음** — 복원 경로에서만 드러난다)
- **해소 메모**: ①③은 [navigation-flow](../architecture/navigation-flow.md) 「인자 있는 목적지」에
  적고, 대안(결과 버스로 비트맵을 나르거나 미리보기가 자기 소유 캐시를 갖는 형태)을 그 자리에서 견준다.
  > 🔁 **①의 "프로세스 사망 뒤 복원" 전제가 사실이 아니다(2026-09-07, PR #463 develop 머지).**
  > 이 앱은 **백스택을 저장하지 않는다** —
  > `Navigator`가 백스택을 `mutableStateListOf`로 들고 `@ActivityRetainedScoped`로 살 뿐이고,
  > `MainRoute`는 그것을 그대로 `NavDisplay`에 넘기며 `rememberNavBackStack`도 `SavedStateHandle`도
  > 쓰지 않고, `MainActivity.onCreate`도 `savedInstanceState`를 읽지 않는다. 프로세스가 죽고 돌아오면
  > 백스택은 `NavigatorConst.INITIAL_NAVIGATION_KEY`(=`NavKeySplash`) 하나로 리셋되므로 **미리보기
  > 화면 자체가 복원되지 않고, 그러니 경로만 살아 돌아오는 일도 없다.** 남는 갈래는 프로세스가 살아
  > 있는 동안의 OS 캐시 정리 하나뿐이라 상태 줄의 "복원 경로에서만 드러난다"도 그만큼 좁아진다.
  > 같은 전제가 `NavKeyCanvasImageSave`의 KDoc("NavKey 는 직렬화돼 오간다")에도 남아 있어 함께 정정할
  > 자리다. 같은 라운드에서 `CanvasCaptureHolder`가 들어와 **정상 경로는 미리보기가 그 파일을 아예 읽지
  > 않게 됐다** — 파일이 없어 드러나는 자리는 저장 확정의 `readCanvasCaptureCache` 하나로 좁아졌고,
  > ①이 물은 "키에 캐시 경로를 싣는 것" 자체는 그대로 남는다
  > → [navigation-flow](../architecture/navigation-flow.md) 「캔버스 저장 미리보기 왕복」.

### [2026-09-05] 캡처 캐시 파일을 지우는 자리가 없고 저장 왕복에 테스트가 없다

- **ID**: OQ-P-365
- **출처**: `CanvasCaptureCache.kt#writeToCanvasCaptureCache`·`readCanvasCaptureCache`·
  `CanvasImageSaveRoute`(PR #445) — 캡처는 `cacheDir/canvas_capture/canvas_preview.png`라는
  **고정 이름**으로 쓰인다(쌓이지 않게 하려는 선택이다). 지우는 호출은 어디에도 없고, 같은 경로를
  반복해 그리므로 미리보기가 Coil 요청에 `addLastModifiedToFileCacheKey`를 걸어 이전 캡처가 다시
  뜨는 것을 막는다.
- **항목**: ① 저장을 그만둔 캡처가 캐시에 남는다 — OS가 캐시를 정리할 때까지 마지막 캔버스 한 장이
  기기에 남고, 그것이 남아도 되는지 정한 적이 없다. ② **이름이 고정이라 동시성 전제가 생겼다** —
  캔버스를 연달아 캡처하면 같은 파일을 덮어쓰고, 앞선 미리보기가 살아 있으면 다른 그림을 보게 된다.
  ③ **테스트가 없다** — 붙은 유닛은 저장 클릭이 `RequestCanvasCaptureForPreview`를 보내는지 하나뿐이고
  쓰기·읽기·결과 왕복·미리보기 화면은 한 줄도 잠기지 않았다.
- **상태**: 미해결 (**동작 영향 낮음** — ②는 한 화면에서 연달아 캡처해야 재현된다)
- **해소 메모**: 정하면 [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)
  「드리프트 / 잔존」에 적는다. ①은 미리보기를 떠날 때 지우는 것이 가장 싸고, 그러면 ②도 함께 좁아진다.
  > 🔁 **②가 홀더에도 그대로 옮겨 갔고, 재현 조건은 "한 화면에서 연달아"보다 넓다(2026-09-07,
  > PR #463 develop 머지).** 캡처 비트맵을 `CanvasCaptureHolder`가
  > 나르게 되면서 **고정 파일과 홀더가 같은 자리에서 함께 덮인다** — 홀더도 한 장만 들고 `put`이 이전
  > 것을 밀어낸다. 게다가 `MainRoute`가 푸시 딥링크를 앱 루트 한 곳에서 수집해 **지금 어느 화면에
  > 있는지 따지지 않고** `goTo(NavKeyCanvasMain(groupId))`를 부르므로, 미리보기를 열어 둔 채 알림을
  > 탭해 다른 그룹 캔버스로 가서 저장하면 파일과 홀더가 모두 그 그룹의 캡처로 바뀐다. 아래로 내려갔던
  > 미리보기로 돌아오면 `remember`가 다시 계산되어 **다른 그룹의 그림을 원래 날짜 라벨과 함께 보게
  > 된다**(날짜는 계속 `NavKeyCanvasImageSave.date`가 나른다). 이번 라운드가 만든 결함은 아니고 닫지도
  > 않는다 — 홀더가 파일과 같은 성질을 갖는다는 사실만 싣는다. ③은 **한 칸 좁아졌다** — 새로 붙은
  > 유닛 여섯 중 `CanvasCaptureHolderTest` 넷은 홀더 계약만 잠그지만, `CanvasCaptureCacheTest` 둘이
  > **쓰기의 압축 실패·성공 갈래**를 잠근다(`Context`를 `mockk`로 세우고 `cacheDir`에
  > `TemporaryFolder`를 물려 실제 파일 IO가 도는 채로 `compress`만 스텁한다 — 계획이 "유닛으로 감싸기
  > 어렵다"고 본 판단이 실행 중에 뒤집혔다). 읽기·결과 왕복·미리보기 화면은 여전히 한 줄도 잠기지
  > 않는다 → [캔버스 저장 미리보기 캡처 전달 스펙](../superpowers/specs/archive/2026-09-07-canvas-save-preview-capture-holder.md).

### [2026-09-05] 사용자 설정을 지우는 계약만 있고 부르는 자리가 없다

- **ID**: OQ-P-366
- **출처**: `UserConfigRepository.clearConfig`·`UserConfigRepositoryImpl.clearConfig`·
  `UserConfigLocalDataSourceImpl.clear`(PR #449) — 계약·구현·데이터소스까지 다 있는데 **호출부가 0건**이다.
  로그아웃(`LogoutUseCase`)·탈퇴(`WithdrawUseCase`)는 계정 정보와 토큰만 지운다.
- **항목**: ① 로그아웃·탈퇴가 이 설정을 지워야 하는가 — 지우면 같은 기기에서 계정을 바꾼 사람이
  튜토리얼을 다시 보고, 안 지우면 **다른 계정이 앞사람의 "봤다"를 물려받는다.** 둘 다 근거가 없다.
  ② 지운다면 자리가 어디인가 — 계정 정보를 지우는 자리와 같은 곳인지, 설정이 계정에 매이는 것이
  맞는지(지금 이 저장소는 계정 축이 아니라 **기기 축**이다). ③ 쓰이지 않는 계약을 남겨 둘지.
- **상태**: 미해결 (**동작 영향 있음** — 계정 전환 시 튜토리얼이 안 뜬다)
- **해소 메모**: 정하면 [data-layer](../architecture/data-layer.md) DataStore 항목에 소유 축(계정 vs 기기)을
  적는다. 계정 축이면 [ADR-0022](../adr/0022-user-info-local-ssot.md)의 정리 경로에 합류시킨다.

### [2026-09-05] 평문·암호화 DataStore 프록시가 서로의 복제다

- **ID**: OQ-P-367
- **출처**: `DataStorePreferences`(PR #449) · `EncryptedPreferences`(PR #263) — `observe`·`read`·
  `write`(단건·다건)·`remove`·`decodeOrDiscard`가 **KDoc까지 같고**, 다른 것은 쓰기의
  `cryptoManager.encrypt`와 읽기의 `decrypt` 두 줄뿐이다.
- **항목**: ① 한쪽이 다른 쪽을 감싸거나 공통 부분을 뽑을지 — 지금은 폐기 규칙(못 읽는 저장분을 버린다)
  같은 미묘한 결정이 **두 벌로 존재**해서, 한쪽만 고치면 조용히 갈린다. ② `DataStorePreferences.read`는
  **호출부가 0건**이다(쓰는 곳이 `observe`·`write`·`remove`만 쓴다) — 대칭을 위해 남길지.
  ③ 평문·암호문이 **같은 `DataStore<Preferences>` 하나**를 공유하므로 키가 섞인다 — 지금은 문제가
  없지만 어느 키가 어느 형태인지는 코드로만 안다.
- **상태**: 미해결 (**동작 영향 0** — 복제와 미사용 표면의 문제다)
- **해소 메모**: ①을 정하면 [data-layer](../architecture/data-layer.md) 「평문 DataStore 프록시」 항목에
  적고, 뽑아낸다면 [ADR-0019](../adr/0019-encrypted-token-storage.md)의 결정 범위를 건드리는지 함께 본다.

### [2026-09-05] `launchWhileSubscribed`를 고르는 기준과 실제 쓰임이 갈렸다

- **ID**: OQ-P-368
- **출처**: `CanvasMainViewModel#observeCanvasTutorial`·`CustomGalleryPickerViewModel#observeTutorial`·
  `SegmentationConfirmViewModel`(PR #449) — 셋 다 `launchWhileSubscribed`로 튜토리얼 노출 여부를 구독한다.
  [state-management](../architecture/state-management.md)가 적어 둔 선택 기준은 **"이 구독이 서버를 계속
  부르는가"**([ADR-0029](../adr/0029-canvas-today-ssot-polling.md))인데, 이 구독은 DataStore 한 키를 읽는다.
- **항목**: ① 기준을 넓힐지(로컬 구독에도 기본으로 쓴다), 아니면 이 셋을 `launch`로 되돌릴지 —
  지금은 문서의 기준과 코드의 관행이 어긋난 채 **쓰는 곳이 셋에서 여섯**이 됐다. ② 딸려 오는 비용이
  하나 있다 — 이 구독은 화면이 `state`를 보는 동안에만 열려서 **ViewModel 테스트가 `backgroundScope`에서
  `state`를 수집해야** 하고, 세 테스트가 각자 `shownViewModel()` 헬퍼로 같은 준비를 적는다.
- **상태**: 미해결 (**동작은 의도대로** — 기준 문서와 관행의 어긋남이다)
- **해소 메모**: 정하면 [state-management](../architecture/state-management.md) 해당 절의 기준 문장을
  고친다. ②는 테스트 헬퍼를 `core:testing`으로 올릴지와 같은 자리에서 본다.

### [2026-09-05] 같은 튜토리얼 상태가 화면마다 두 형태다 — 한쪽은 리소스를 State에 싣는다

- **ID**: OQ-P-369
- **출처**: `CanvasMainUiState.tutorialStep: CanvasTutorialStep?` vs
  `CustomGalleryPickerState.isTutorialVisible: Boolean`·`SegmentationConfirmState`(PR #449) —
  여러 장짜리인 캔버스만 enum을 담는데, 그 `CanvasTutorialStep`이 `@DrawableRes`·`@StringRes` 상수를
  프로퍼티로 든다. State가 `Int`를 직접 담지는 않아도 **표시 리소스가 State를 타고 흐른다.**
- **항목**: ① 리소스를 든 enum이 State에 있어도 되는가 — [state-management](../architecture/state-management.md)의
  "표시 문자열·리소스 ID를 State에 담지 않는다"와 [ADR-0016](../adr/0016-domain-result-presentation-string-mapping.md)에
  걸린다(도메인 의미만 담고 화면이 리소스를 고르는 형태로 하려면 `CanvasTutorialStep`에서 리소스를
  떼어 화면 매핑으로 내리면 된다). ② 한 장짜리·여러 장짜리가 두 형태로 갈린 것을 유지할지 —
  네 번째 화면이 붙을 때 어느 쪽을 따를지 정해진 것이 없다. ③ 완료를 남기는 시점도 갈린다 —
  캔버스는 **마지막 장을 닫을 때만** 남기고(중간에 접으면 다음 진입에서 처음부터 다시 본다),
  한 장짜리 둘은 누르는 즉시 남긴다. 앞의 것은 의도가 KDoc에 적혀 있으나 규칙으로 올라간 적은 없다.
- **상태**: 미해결 (**동작은 의도대로** — 형태 분기와 규약 이탈이다)
- **해소 메모**: ①②를 정하면 [state-management](../architecture/state-management.md) 「UI State가 담는 것」에
  적고, ③은 [design-system](../architecture/design-system.md) 「튜토리얼 4종」에 노출·완료 규칙으로 적는다.

### [2026-09-05] 알림 권한 안내를 몇 번 보여줄지 정한 적이 없다

- **ID**: OQ-P-370
- **출처**: `NotificationPermissionGate`(PR #450) — 거부·"나중에"를 **영속하지 않는다.** 노출 조건은
  "지금 권한이 없다" 하나이므로, 허용하기 전까지 **그룹 생성·참여 흐름을 탈 때마다 다시 뜬다.**
  스펙이 이 사실을 적고 미결로 남겼다
  ([결정 4](../superpowers/specs/archive/2026-09-05-push-notification-permission-and-device-token.md)).
- **항목**: ① 최초 1회로 줄일지 — 줄이려면 "안내를 보여줬다"를 로컬에 영속해야 하고, 자리는 이미 있다
  (`UserConfigRepository`, PR #449). ② 반대로 매번 뜨는 것이 의도인지 — 그룹을 새로 만들 때마다 묻는
  것은 맥락이 달라졌다고 볼 수도 있다. **정책 근거가 없다** — 위키에 알림 권한 조항 자체가 없다.
  ③ **안내를 지나친 뒤를 알리지 않는다**(OQ-P-358 ②에서 옮겨 온 항목) — 거부한 사용자는 알림이 왜
  안 오는지 앱 어디서도 알 수 없고, 표시 실패는 로그에도 안 남는다.
- **상태**: 미해결 (**동작은 의도대로** — 근거 부재와 반복 노출이다)
- **해소 메모**: ①②는 위키 판단이 선행이다. 1회로 정하면 `TutorialKind`와 같은 자리
  (`UserConfigVO`)에 항목을 더하는 것이 가장 싸고, 그때 [data-layer](../architecture/data-layer.md)
  DataStore 항목과 위 스펙 결정 4를 함께 고친다.

### [2026-09-05] 이전 세션에서 이미 두 번 거부한 사용자는 설정으로 보내지 못한다

- **ID**: OQ-P-371
- **출처**: `NotificationPermissionGate`(PR #450) — 영구 거부 판정이 `shouldShowRequestPermissionRationale`
  을 **요청 직전과 콜백에서 두 번 읽어 `true` → `false` 로 떨어진 경우**만 인정한다. 한 번만 읽으면
  "아직 안 물어봤다"와 "두 번 거부됐다"가 같은 `false` 라 **처음 온 사용자까지 설정으로 보내기** 때문이다.
- **항목**: ① 그 대가로 **이전 세션에서 이미 두 번 거부해 둔 사용자**는 요청 직전 값이 `false` 라
  판정에 걸리지 않는다 — 「알림 받기」를 눌러도 시스템 다이얼로그가 안 뜨고 설정으로도 안 가서
  **버튼이 무반응으로 남는다.** ② 닫으려면 "한 번은 요청했다"를 로컬에 영속해야 하고, 그러면
  OQ-P-370 ①과 같은 저장소를 쓰게 된다 — 두 물음을 함께 정하는 편이 낫다. ③ 이 갈래를 확인한 사람이
  없다 — 재현하려면 두 번 거부한 상태로 앱을 다시 켜야 하는데 실기기 확인이 0회다.
- **상태**: 미해결 (**동작 영향 있음** — 해당 사용자에게는 버튼이 아무 일도 하지 않는다)
- **해소 메모**: ②를 택하면 위 스펙 결정 4의 ⚠️ 두 문단과
  `NotificationPermissionManager.shouldShowRationale` KDoc을 함께 고친다.

### [2026-09-05] 이펙트를 붙잡아 두는 게이트 배선이 두 Route에 복제됐다

- **ID**: OQ-P-372
- **출처**: `GroupCreateRoute`·`GroupNickNameRoute`(PR #450) — 둘 다 `NavigateToNext` 이펙트를 곧바로
  실행하지 않고 `rememberSaveable`에 담아 두었다가 게이트가 끝나면 잇는다. **같은 구조가 두 벌이고**
  `NavigateToNextSaver`도 각자 있다(이펙트 타입이 달라 공용화하려면 제네릭이나 공통 인터페이스가 필요하다).
- **항목**: ① 세 번째 화면이 같은 게이트를 필요로 할 때 또 복제할지 — 지금은 "이득이 얇다"가 근거이고
  스펙이 그렇게 적었다. ② **이펙트를 화면 상태로 붙잡아 두는 것 자체가 새 관용구**인데 규약이 없다 —
  이 저장소의 이펙트는 지금까지 전부 즉시 소비였다. ③ `rememberSaveable`은 구성 변경만 막고
  **프로세스 사망은 막지 못한다** — `Navigator` 백스택이 `@ActivityRetainedScoped`의 순수
  `mutableStateListOf`라 스플래시로 초기화되므로, 그 경우 사용자는 그룹이 만들어진 채 처음 화면에 선다.
- **상태**: 미해결 (**동작은 의도대로** — 복제와 규약 부재다. ③은 이 구조가 못 막는 범위의 문제다)
- **해소 메모**: ②를 정하면 [state-management](../architecture/state-management.md)에 "이펙트를 지연
  소비하는 자리" 절을 두고, ①은 그때 함께 본다. ③은 백스택 복원(OQ-P-339와 같은 자리)이 선행이다.

### [2026-09-05] 알림 권한 게이트에 자기 테스트가 없다

- **ID**: OQ-P-373
- **출처**: `feature/groups/enter/impl` — 이 모듈에 **`androidTest` 소스셋이 없다.** PR #450이 JVM으로
  덮은 것은 `NotificationPermissionManagerTest`(API 33 미만에서 `Context`를 묻지 않고 `true`를 준다)와
  두 `NavigateToNextSaver` 왕복뿐이다.
- **항목**: ① **33 이상 분기와 `shouldShowRationale` 두 번 읽기 비교가 한 줄도 안 잠겼다** — 정확히
  OQ-P-371이 가리키는 갈래이고, 잘못 고치면 처음 온 사용자를 설정으로 보내는 회귀가 조용히 들어온다.
  ② 하니스를 신설할지 — `parfait.test.compose`를 이 모듈에 붙이면 되지만, **CI가 계측을 컴파일만 하는
  문제**가 그대로라 붙여도 돌지 않는다(OQ-P-102 ②). ③ 게이트가 `onFinished`를 세 갈래 모두에서
  부른다는 계약도 테스트가 아니라 KDoc으로만 유지된다.
- **상태**: 미해결 (**동작은 의도대로** — 잠금 부재다)
- **해소 메모**: ②는 OQ-P-102와 같은 자리에서 정한다 — CI가 계측을 실행하지 않는 한 하니스 신설의
  값이 얇다. ①만이라도 잠그려면 판정을 Composable 밖 순수 함수로 한 번 더 빼는 길이 있다.

### [2026-09-06] 알림 아이콘의 여백 보정이 벡터 안의 임시 변환으로 들어갔다

- **ID**: OQ-P-374
- **출처**: `app/src/main/res/drawable/ic_notification.xml`(PR #456 신설) ·
  `ParfaitFirebaseMessagingService#showNotification`(`setSmallIcon`) — 런처 아이콘
  `ic_launcher_monochrome`은 어댑티브 세이프존 여백을 안고 있어 상태바 24dp 규격에서 실루엣이 작게
  찍힌다. 전용 에셋을 새로 두면서 **원본 SVG의 여백을 `group`의 `scale`·`translate`로 줄여** 라이브
  영역을 채웠고, 그 사실을 파일 주석이 적는다.
- **항목**: ① 디자인이 여백을 조인 알림 아이콘을 정식으로 줄지 — 주면 `group` 변환을 걷어낸다.
  ② 이 실루엣이 승인받은 모양인지 근거가 없다. 알림 아이콘 규격을 적은 정책 문서가 위키에도 parfait에도
  없다. ③ 확인 수단이 실기기 눈뿐이다 — 상태바 아이콘은 시스템이 알파만 쓰고 색을 덮으므로 프리뷰로
  최종 모습을 볼 수 없다.
- **상태**: 미해결 (**동작은 의도대로** — 임시 보정과 정책 근거 부재다)
- **해소 메모**: ①이 오면 파일 주석의 지시대로 `group`을 걷고 이 항목을 닫는다. ②는 위키
  [[open-questions]]와 갈리는 자리다 — 알림 항목 자체가 정책 소스에 없다(OQ-P-343).

### [2026-09-06] 파르페 크림 하한이 3칸이 됐지만 근거가 커밋 제목뿐이다

- **ID**: OQ-P-375
- **출처**: `GroupListParfaitLayout.kt#MIN_MIDDLE_COUNT`(PR #455에서 1 → 2) × 위키
  [[무한-파르페-그리드]] — 크림은 `topSection`(`parfait_cream_top`) 한 칸에 `middleSection`이 쌓이는
  구조라 하한이 2칸에서 **3칸**이 됐고, 이는 위키가 요구한 "토핑 0~3개 → 크림 3개"와 **처음으로
  맞는다.** 그런데 그 의도를 적은 것은 커밋 제목 한 줄뿐이고 코드에는 정책 참조가 없다.
- **항목**: ① 상수가 위키 규칙을 구현한 것이 맞는지 — 맞다면 이름이나 KDoc이 그 사실을 말해야 한다.
  지금은 왜 2인지 코드만 봐서는 알 수 없다. ② **KDoc이 코드와 어긋난다** — 함수 주석이 여전히
  "기본적으로 middleSection 을 1개만 배치하지만"이라고 적는다. ③ **증가 규칙은 여전히 다르다** —
  위키는 "4개부터 토핑 1개당 크림 1칸"인데 코드는 `content` 높이를 덮을 때까지 반복이라 토핑 수가
  아니라 높이가 개수를 정한다. 하한만 맞고 기울기는 안 맞는 상태다.
- **상태**: 미해결 (**동작 변화 있음** — 그룹이 적은 사용자에게 크림 한 칸이 더 보인다)
- **해소 메모**: ②는 한 줄 수정이라 다음 이 파일을 여는 작업에 얹는다. ③은
  [g001-group-list 스펙](../superpowers/specs/archive/2026-08-01-g001-group-list.md) 정책 대조 표의 "크림 개수 규칙"
  행과 같은 자리이고, 높이 기반을 유지할지 토핑 수 기반으로 갈지는 디자인 확인이 먼저다.

### [2026-09-07] 저장 아이콘 연타를 막는 자리가 없다

- **ID**: OQ-P-376
- **출처**: `CanvasMainViewModel#handleClickSaveToGallery`(PR #445 이후 그대로) — 저장 클릭을 받아
  `postSideEffect(CanvasMainEffect.RequestCanvasCaptureForPreview)` 한 줄만 하고, 같은 ViewModel의 다른
  작업들이 쓰는 `BaseViewModel#launch(key = ...)` 중복 가드도 걸지 않는다. 이펙트 통로인
  `BaseViewModel#effect`는 `Channel(Channel.BUFFERED)`이라 **구독자가 없는 동안 발행해도 버퍼에 남았다가
  전달된다** — 화면 재진입·Activity 재생성에 이동이 저절로 다시 도는 것을 막으려고 고른 성질이다
  ([ADR-0020](../adr/0020-mvi-error-effect-infrastructure.md)). 수집은 `CanvasMainRoute`의
  `LaunchedEffect(viewModel)`이라 미리보기가 위에 쌓여 캔버스 메인의 컴포지션이 사라지면 끊긴다.
- **항목**: ① 저장 아이콘을 연타하면 두 번째 이펙트가 버퍼에 남았다가 **미리보기에서 돌아온 뒤**
  전달되어 곧바로 다시 미리보기로 들어간다 — 사용자가 방금 닫은 화면이 저절로 다시 열린다.
  ② 막는 자리가 어디인가 — ViewModel이 `launch(key)`로 접을지, 캡처가 끝날 때까지 아이콘을 잠글지,
  통로를 `Channel(CONFLATED)`로 바꿀지. 마지막 것은 이 화면만의 결정이 아니다(`BaseViewModel`이
  전 화면 공용이고 캔버스 메인의 다른 이펙트까지 함께 접힌다). ③ 같은 성질을 가진 다른 이동 이펙트를
  세어 본 적이 없다 — 통로가 공용이라 이 화면만의 문제가 아닐 수 있다.
- **상태**: 미해결 (**동작 영향 낮음** — 연타해야 재현되고 크래시는 아니다. 캡처 홀더 라운드와 독립인
  기존 결함이다)
- **해소 메모**: ①②는 [c001-canvas-gallery-save 스펙](../superpowers/specs/archive/2026-08-23-c001-canvas-gallery-save.md)
  「드리프트 / 잔존」에 적고, 저장 왕복을 다시 여는 라운드에 얹는다(캡처 홀더 라운드가 범위 밖으로
  둔 항목이다 → [캔버스 저장 미리보기 캡처 전달 스펙](../superpowers/specs/archive/2026-09-07-canvas-save-preview-capture-holder.md)).
  ③은 이펙트 통로 자체의 물음이라 [state-management](../architecture/state-management.md)에서
  ADR-0020과 함께 본다.

### [2026-09-07] 앱 닉네임이 없을 때 생성·참여 두 갈래가 반대로 답한다

- **ID**: OQ-P-377
- **출처**: `GroupListViewModel#handleClickCreateNewGroup`과 `GroupInviteCodeViewModel`의 확인 갈래
  (후자는 PR #461 develop 머지) — 그룹 내 닉네임의 초기값은 계정 공통 앱 닉네임을 재사용하고, 두 갈래
  모두 `GetMyAccountFlowUseCase`를 `init`에서 구독해 그 값을 다음 화면의 NavKey 인자로 실어 보낸다.
  **값이 아직 없을 때의 답이 서로 반대다** — 생성 갈래는 이동 자체를 접고 로그만 남기며(다시 누르면
  열린다), 참여 갈래는 막지 않고 빈 문자열로 넘어간다.
- **항목**: ① **사용자에게 보이는 결과가 반대다** — 생성은 버튼을 눌러도 아무 일이 없고(참여 갈래
  주석이 명시적으로 피하려 한 바로 그 모양이다), 참여는 빈 입력칸으로 선다. 어느 쪽이 이 앱의 답인지
  정한 적이 없다. ② **도달 조건을 세어 본 적이 없다** — 계정 SSoT는 부트스트랩이 채우므로 두 화면에서
  값이 비는 창이 실제로 열리는지, 열린다면 얼마나 긴지 아무도 확인하지 않았다. 로그만 남으므로
  재현되어도 드러나지 않는다. ③ **정책에 없다** — 위키 [[S-102-그룹-닉네임-생성-정책-v0.1]]은 "계정
  공통 1개 값 재사용"까지만 정하고 **그 값이 없을 때**를 말하지 않는다.
- **상태**: 미해결 (**동작 영향 낮음** — 두 갈래 다 사용자가 직접 입력하면 진행되고, 확인 버튼이
  빈 값을 막으므로 닉네임 없는 그룹이 만들어지지는 않는다)
- **해소 메모**: ①은 [navigation-flow](../architecture/navigation-flow.md) 「그룹 생성·참여 플로우」의
  같은 자리에 두 답을 나란히 적었고, 하나로 모으기로 정해지면 그 절과
  [a004 스펙](../superpowers/specs/archive/2026-08-12-a004-group-invite-code.md)·
  [s102 스펙](../superpowers/specs/archive/2026-07-22-s102-group-nickname.md)을 함께 고친다. ③은 위키 판단이
  선행이다 — 정책으로 확정되면 위키에 조항을 만들고 여기는 구현 소관만 남긴다.

### [2026-09-07] 토핑 알파 판정이 그림 사각형 밖으로 넓어졌다

- **ID**: OQ-P-378
- **출처**: `ToppingHitTarget.kt`(PR #464 develop 머지,
  [ADR-0030](../adr/0030-topping-outline-distance-field.md)) — 옛 `ToppingAlphaMask`는 판 범위 밖을
  투명으로 답해, 실루엣이 그림 왼쪽·위쪽 변에 닿아 있어도 그 바깥은 눌리지 않았다. 새 거리판은 판
  밖 좌표에서도 거리를 재므로(`ToppingOutline.distanceAt` — 벗어난 만큼을 거리에 더해 하한을 낸다)
  **테두리를 그린 자리까지 판정이 눌린다.** 회귀 테스트
  `ToppingHitTestTest#containsPoint_leftOfImageRect_isHitWhenTheBorderReachesThere`·
  `containsPoint_aboveImageRect_isHitWhenTheBorderReachesThere`의 기대값이 이 변화 때문에 뒤집혔다.
- **항목**: ① 그리는 모양과 판정 모양을 정의상 일치시키는 것이 이번 라운드의 목적이므로 새 동작이
  맞다고 본다 — 다만 이것은 **설계 판단이지 사용자 확인이 아니다.** ② 사용자가 체감할 변화인데
  실기기에서 확인된 적이 없다 — 테두리가 그림 변 밖으로 나가 있는 토핑(캔버스 진입점에 흔한 배치)을
  눌러 보면 옛 판정이면 안 눌렸을 자리가 눌린다. ③ 위키에 이 판정 범위를 규정한 조항이 없다 —
  코드가 먼저 정한 사례다.
- **상태**: 미해결 (**도달 가능하나 미관측** — 유닛 테스트는 새 규칙으로 고정됐고, 2026-09-07
  실기기 확인에서 테두리가 가장자리에 잘리지 않는 것은 봤지만 **판정이 넓어진 자리를 눌러 본
  기록은 없다**)
- **해소 메모**: 그림 상자 밖으로 테두리가 나간 토핑을 눌러 옛 판정이면 안 눌렸을 자리가 눌리는지
  확인하면 닫는다. 닫을 때 [토핑 알파 판정 스펙](../superpowers/specs/archive/2026-08-26-topping-alpha-hit-test.md)의
  판정 규칙 절에 반영한다.

### [2026-09-07] 편집 화면과 나머지 셋의 거리판 해상도가 다르다

- **ID**: OQ-P-379
- **출처**: `ToppingBorderEditScreen.kt`의 `PREVIEW_FIELD_LONG_SIDE = 1440` ×
  `ToppingOutlineCache.kt`의 `OUTLINE_LONG_SIDE = 256`(PR #464 develop 머지, [ADR-0030](../adr/0030-topping-outline-distance-field.md)) — 편집 화면은 거리판을
  화면에 나올 크기 그대로(사실상 화면 크기, 상한 1440) 재고, 누끼 확인·배치·캔버스 셋은 캐시가 만든
  긴 변 256 거리판을 공유한다. 띠의 바깥 곡선은 등거리 곡선이라 굵을 때는 격자 차이가 안 보인다.
- **항목**: ① **굵기가 얇을수록 256 격자가 실루엣 잔주름을 뭉갠다** — 굵기 최소(2dp)에 토핑이 화면을
  거의 채우는 자리(누끼 확인 화면 등)에서 편집 화면과 나머지 셋의 모양이 갈릴 수 있다. ②
  [topping-border-distance-field 스펙](../superpowers/specs/archive/2026-09-07-topping-border-distance-field.md) 목표인
  "네 화면 모양 일치"와 정면으로 부딪히는 자리인데 이번 라운드는 256으로 간 채 실기기 확인에
  넘긴다. ③ 갈리면 `OUTLINE_LONG_SIDE`를 512로 올리는 대응이 이미 스펙에 있지만(항목당 메모리가
  네 배가 되므로 캐시 칸 수를 함께 줄인다), 그 판단 자체가 아직 실기기 확인 전이다.
- **상태**: 미해결 (**한 번 관측했으나 조건이 불완전** — 2026-09-07 실기기 확인에서 네 화면 모양이
  같았고 그래서 `OUTLINE_LONG_SIDE` 를 256으로 유지했다. 다만 **굵기 최소 2dp 조건까지 대조했는지
  기록이 없다** — 차이가 드러나는 조건이 바로 그 자리다)
- **해소 메모**: 굵기 2dp에 토핑이 화면을 거의 채우는 자리에서 편집 화면과 나머지 셋을 한 번 더
  대조하면 닫는다. 갈리면 스펙의 "열린 질문" 절에 512로 올린 근거를 적고 캐시 칸 수를 함께 줄인다.

### [2026-09-07] 확인 화면과 배치 화면이 같은 그림을 다른 열쇠로 잡는다

- **ID**: OQ-P-380
- **출처**: `SegmentationConfirmScreen.kt` 는 `subjectImagePath` 원문(절대경로)을 모델로 쓰고,
  `CanvasToppingPlaceScreen.kt` 는 같은 경로를 `File(path).toUri().toString()`(`file://` 스킴)으로
  바꿔 쓴다(PR #464 develop 머지). 거리판 캐시의
  열쇠가 `"$retryKey|$model"` 이라 **두 화면이 같은 그림을 서로 다른 항목으로 잡는다.**
- **항목**: ① 누끼 확인 → 배치로 넘어갈 때 캐시가 미스라 같은 그림을 두 번 디코딩하고 거리판도 두
  장 만든다. ② 그래서 배치 화면 진입 시 테두리가 한 박자 늦는다 — 깜빡임을 없애려고 넣은 동기
  조회(`ToppingOutlineCache.peek`)가 이 경로에서는 무력하다. ③ Coil 의 이미지 캐시도 같은 이유로 갈린다.
- **상태**: 미해결 (**동작은 맞고 비용만 든다** — 두 판의 내용은 같다)
- **해소 메모**: 두 화면이 같은 문자열을 쓰도록 맞추면 닫힌다. 어느 쪽으로 맞출지는 Coil 이 두
  형식을 다 받으므로 취향이 아니라 **다른 호출부**를 보고 정해야 한다.

### [2026-09-08] 테두리 굵기 범위를 코드가 정하고 두 플랫폼이 서로 다른 값을 본다

- **ID**: OQ-P-381
- **출처**: `domain` 의 `ToppingBorder.WIDTH_RANGE_DP`(2.0..30.0) · `ToppingBorder.solidClamped` ·
  `ToppingEditState` 의 굵기 게터(PR #464 develop 머지).
  상한 50dp 에서는 테두리가 알맹이를 덮어 버려 30 으로 내렸다. 근거는 정책이 아니라 눈으로 본
  결과다(OQ-P-208 ③ 의 구체 사례).
- **항목**: ① **iOS 는 20 에서 자른다** — 같은 캔버스가 기기마다 다르게 보인다. 두 플랫폼이 같은
  상한을 봐야 하는데 지금은 갈려 있다. ② **매핑의 클램프는 임시 코드다** — 상한을 내리기 전에 50
  으로 저장된 행이 이미 있어 슬라이더만 좁히면 그 행이 계속 굵게 그려진다. 서버가 범위를 강제하거나
  기존 행이 정리되면 걷을 자리다([api/parfait-image.md](../api/parfait-image.md) 미결). ③ 위키에
  대응 정책이 없어 값이 바뀔 때마다 코드가 먼저 정한다.
- **상태**: 미해결 (**동작은 의도대로다** — 슬라이더와 매핑이 같은 범위를 본다)
- **해소 메모**: 위키에 굵기 정책이 들어오면(raw 수집이 선행) 그 값으로 `WIDTH_RANGE_DP` 를 맞추고
  iOS 와 대조해 닫는다. 그때 ②의 클램프를 계속 둘지도 함께 정한다 — 서버가 범위를 강제하면 걷는다.

### [2026-09-08] 띠 한 장 만드는 비용이 다섯 배가 됐고 리사이즈 중에는 계속 다시 만든다

- **ID**: OQ-P-382
- **출처**: `ToppingOutline.distanceAt` 의 판 밖 씨앗 탐색 · `YGToppingCutoutImage` 의
  `snapshotFlow { boxSize }` + `conflate`(PR #464 develop 머지). 판 밖 거리를 씨앗까지 실제로 재면서 판 한 장이 **6.9ms 에서 36ms** 가 됐다
  (거리판 긴 변 256·알맹이 400px·굵기 150px 기준, JVM 측정).
- **항목**: ① 리사이즈 핸들을 끄는 동안 크기가 바뀔 때마다 판을 다시 만든다 — 위치 드래그는
  상자 크기를 안 바꿔 해당 없다. ② **손이 멎은 뒤로 미루는 안은 되돌렸다**(2026-09-08) — 그동안
  옛 판을 늘려 그리면 굵기가 배율만큼 틀어져 보이다가 손을 떼는 순간 제 굵기로 스냅해 룩이 튄다.
  굵기 dp 고정이 눈에 보이는 성질이라 늘려 그리는 것 자체가 안 된다. ③ 아직 재지 않은 것은
  실기기 프레임이다(OQ-P-208 ① 과 같은 자리).
- **상태**: 미해결 (**실기기에서 버벅임을 관측하지는 않았다** — JVM 측정과 코드에서 읽히는 사실뿐)
- **해소 메모**: 굵기를 건드리지 않는 축으로 줄여야 한다 — 리사이즈 중에만 판 해상도를 낮추거나
  (`buildBorderPlate` 의 `plateLongSide`), 거리판을 여백까지 포함해 미리 계산해 판 만들 때는 읽기만
  하게 바꾸는 두 갈래가 있다. 후자는 거리판 한 장의 메모리가 는다(OQ-P-317 과 같은 자리).

### [2026-09-08] 캔버스 응답이 그룹명을 주기 시작했는데 앱이 그것을 버린다

- **ID**: OQ-P-383
- **출처**: 서버 `9c13852`(`feat: 캔버스 조회 응답에 그룹이름 추가`) — `GetTodayParfaitResult`·
  `GetTodayParfaitResponse` 에 `groupName` 이 **비널 문자열**로 붙었고, 오늘·상세 두 조회가 모두 채운다.
  앱 쪽은 `GetTodayParfaitResponse`(`:data`)·`CanvasVO`(`:domain`) 어디에도 자리가 없다.
- **항목**: ① 서버 커밋 메시지가 목적을 적는다 — **그룹 id 만 쥔 채(푸시 알림) 캔버스로 바로 들어오면
  상단에 그릴 그룹명을 얻을 길이 없었다.** 즉 이 필드는 딥링크 진입을 받치려고 생겼다.
  ② 앱 `Json` 이 `ignoreUnknownKeys = true` 라 파싱은 안 깨지고 **값만 조용히 버려진다.**
  ③ 그래서 서버가 없애 준 왕복(그룹 상세 조회)이 앱에서는 그대로 남는다.
  ④ 그룹 조회가 하나 늘면서 **두 조회에 404 `GROUP_NOT_FOUND` 경로가 생긴 것**도 함께 미반영이다.
- **상태**: 부분 해소 (**①②③ 닫힘** — 2026-09-08, PR #469 develop 머지 / **④ 잔존**)
- **해소 메모**: ✅ **하루 만에 반영됐다.** `GetTodayParfaitResponse`(`:data`)·`CanvasVO`(`:domain`)에
  `groupName` 이 서고 `VOMapper.toCanvasVO` 가 `GroupName` 으로 감싸 나른다 — 오늘·상세가 같은 매퍼를
  타므로 두 조회 모두 값을 얻는다. **다만 ③의 왕복은 실제로는 안 없앴다** — C-001 상단 바의 정본을
  그룹 목록 캐시([ADR-0023](../adr/0023-group-in-memory-ssot.md))로 두고, 캔버스가 준 이름은 그 캐시가
  **아직 비어 있을 때만** 채우기로 했다. 캔버스 SSoT([ADR-0029](../adr/0029-canvas-today-ssot-polling.md))가
  그룹의 값을 자기 정본으로 삼으면 같은 값의 출처가 둘이 되기 때문이다. 그래서 이 필드가 실제로 값을
  내는 자리는 **목록 캐시가 빈 진입**(푸시 딥링크·프로세스 재시작 복귀)이고, 그것이 서버가 이 필드를
  만든 이유(①)와 정확히 겹친다. 같은 라운드가 **목록에 없는 그룹이면 이름을 지우던 자리**도 고쳤다
  (`orEmpty()` → `return@collect`) — 안 고치면 부트스트랩한 이름이 목록 방출 때마다 지워진다.
  ⚠️ 뒤집으면 목록에서 사라진 그룹의 옛 이름이 남지만, 그 경로는 화면을 떠나는 흐름이다.
  **④는 그대로다** — 앱 코드에 `GROUP_NOT_FOUND` 상수가 없어 두 조회의 404 를 다른 실패와 구별하지
  않는다. [api/parfait.md](../api/parfait.md) "Android 매핑" ·
  [api/conventions.md](../api/conventions.md) "Android 불일치"(이 항목은 표에서 걷었다).

### [2026-09-08] 리마인드 발송 대상이 알림 권한을 보지 않는다

- **ID**: OQ-P-384
- **출처**: 서버 `4789557` — `ReminderTargetQueryPort` KDoc 이 **"권한 거부 이용자는 토큰이 없어 결과에
  미포함(정책 E-02)"** 이라고 적는다. 구현(`DeviceTokenRepository.findActiveGroupMemberTokens`)은
  `device_token` 에 `parfait_group_member` 를 `EXISTS` 세미조인할 뿐 권한을 보지 않는다.
  앱 쪽 근거는 PR #450 — 등록 호출을 **권한 허용 직후에서 세션 축 넷으로 옮겼고**, 그 이유가
  "토큰이 권한과 무관하게 발급되기 때문" 이다.
- **항목**: ① 그러므로 **권한을 거부한 기기의 토큰도 대상에 든다** — 서버 주석의 전제가 앱 동작과
  어긋난다. ② 발송은 성공으로 집계되고 사용자에게는 아무것도 보이지 않으므로 **틀렸다는 신호가
  어디에도 안 남는다.** ③ 더 근본적으로 **"알림을 끄겠다"는 의사를 담는 자리가 서버·앱 어디에도 없다**
  (OQ-P-343 의 남은 갈래와 같은 자리다). ④ 하루 두 번 전원 발송이라 규모가 곧바로 붙는다.
- **상태**: 미해결
- **해소 메모**: 갈래는 둘이다 — 앱이 권한 없을 때 토큰을 지우거나(등록 축을 되돌리는 셈이라 PR #450
  의 근거와 충돌한다), 서버에 수신 설정 필드를 두고 대상 조회가 그것을 보게 한다. 후자가 수신 설정
  요구(OQ-P-343)와 한 벌이다. [api/notification.md](../api/notification.md) "데일리 리마인드".

### [2026-09-08] P-03 의 21시 하드컷이 배치 시작 시각만 본다

- **ID**: OQ-P-385
- **출처**: 서버 `4789557` — `DailyReminderSender.send` 가 `now.toLocalTime()` 이 `EVENING_CUTOFF`(21:00)
  이상이면 **통째로 건너뛴다**(정책 E-09 "21:00 이후 도착 금지"). `now` 는 배치 시작 시각이고,
  발송은 그 뒤 `CHUNK_SIZE = 500` 씩 청킹해 `CHUNK_DELAY_MS = 200` 지연을 끼고 돈다.
- **항목**: ① 판정이 **시작 시각 한 번뿐**이라, 잡이 길어져 21:00 을 넘겨 나가는 **뒤쪽 청크는 막지
  못한다.** ② 20:00 시작이면 여유가 한 시간이라 지금 규모에서는 넘길 일이 없다 — 그러나 그 여유가
  이용자 수·FCM 응답 지연에 달려 있고 **어디에도 상한이 적혀 있지 않다.** ③ 잡이 실패해 늦게 재시작되면
  시작 시각 판정이 오히려 잘 듣는다(그때는 통째로 스킵된다) — 위험한 쪽은 **정시에 시작해 길게 끄는**
  경우다.
- **상태**: 미해결 (**실측하지 않았다** — 코드에서 읽히는 사실과 상수뿐이고, 운영 이용자 수를 모른다)
- **해소 메모**: 청크 루프 안에서 컷오프를 다시 보고 남은 청크를 버리면 정확해진다. 그러려면 "몇 명이
  못 받았다" 를 남길 자리가 필요한데 리마인드는 Outbox 를 안 써서 **로그뿐이다**([api/notification.md](../api/notification.md)).

### [2026-09-08] 빈 알맹이 차단의 하한·문구가 기획에 없다

- **ID**: OQ-P-386
- **출처**: `specs/archive/2026-09-08-topping-edit-empty-subject-guard.md`. 토핑 편집에서 영역을 전부 지우고
  완료해도 완전히 투명한 이미지가 서버까지 올라가는 구멍을 막으면서, 판정 하한과 안내 문구를
  **구현이 정했다**. 하한은 자동 누끼 경로의 커버리지 하한(`SubjectCoverage`, 캔버스 면적의 5/10000·
  최소 2,500px)을 그대로 재사용했고, 문구는 `topping_edit_subject_too_small` 이다.
- **항목**: ① "빈 토핑은 올릴 수 없다"는 규칙 자체가 기획 산출물 어디에도 없다. ② 하한을 자동 경로와
  같게 둘지, 수동 편집만 더 느슨하게 할지가 정해진 바 없다 — 지금은 티끌만 남긴 의도적 편집도 함께
  막힌다. ③ 문구가 디자인·기획 검수를 거치지 않았다.
- **상태**: 미해결 (구현은 위 스펙대로 선행한다)
- **해소 메모**: 같은 서버에 같은 기능으로 올리므로 **iOS와 하한이 갈리면 한쪽에서만 올라가는 토핑이
  생긴다.** 기획이 확정할 때 플랫폼 공통값으로 정하고, 정책이 서면 위키 쪽 정책 문서에도 자리를 만든다.

### [2026-09-09] 업로드 상한 1500·2048 의 근거가 iOS 정합뿐이다

- **ID**: OQ-P-387
- **출처**: `specs/archive/2026-09-08-upload-image-downscale.md`. 업로드 직전 긴 변 상한을
  `UploadImagePlan` 이 imageType 마다 다르게 들고 있다(누끼·배경). 그 두 값은 `TEAMYG-iOS` 의
  `ToppingImageEncoder.maximumLongEdge`·`BackgroundImageLoader.maximumLongEdge` 를 그대로 옮긴 것이다.
- **항목**: ① iOS 가 그 값을 어떻게 골랐는지는 그쪽 코드에 적혀 있지 않아 근거를 되짚을 데가 없다.
  ② 소비 측 상계는 캔버스 긴 변이라 배경 상한은 실제 필요보다 크다 — 더 내릴 여지가 있는데 그
  협의를 아직 하지 않았다. ③ 축소 후 실제로 바이트가 얼마나 주는지 측정치가 없다.
- **상태**: 미해결 (값은 iOS 와 같게 두고 진행한다)
- **해소 메모**: 상한은 **두 플랫폼이 같이 움직여야 하는 값**이다. 한쪽만 내리면 같은 캔버스를 두
  기기에서 볼 때 화질이 갈린다. 기대만큼 안 줄면 다음 레버는 값이 아니라 포맷(WebP)이고, 그것은
  서버가 `image/png`·`image/jpeg` 둘만 받으므로 서버까지 함께 움직이는 별건이다.

### [2026-09-09] 저장소 주석이 스펙이 철회한 메모리 비교를 들고 있다

- **ID**: OQ-P-388
- **출처**: `ImageUploadRepositoryImpl#upload` 의 전처리 호출부 주석과
  `specs/archive/2026-09-08-upload-image-downscale.md` 「실패 처리」.
- **항목**: 주석은 "축소본이 원본보다 메모리를 덜 쓰므로 폴백이 더 큰 메모리를 요구한다"고 적었는데,
  스펙은 그 비교를 **명시적으로 철회했다** — 변경 전 `upload` 에는 디코드가 아예 없었으므로 비교
  대상 자체가 성립하지 않는다. 폴백하지 않는 결정은 양쪽이 같고, 갈린 것은 근거뿐이다.
- **상태**: 미해결 (동작 영향 없음, 주석 정정 대상)
- **해소 메모**: 주석을 스펙의 현재 근거 두 줄로 갈아 끼운다 — 배경 JPEG 고정은 정책이라 조용히
  어기면 안 되고, 실패가 드러나지 않으면 고칠 수도 없다.

### [2026-09-09] 코드 주석 셋이 아카이브로 옮겨진 스펙 경로를 가리킨다

- **ID**: OQ-P-389
- **출처**: `UploadImagePlan`(1곳)·`UploadImagePreprocessorImpl`(2곳)의 KDoc.
- **항목**: 셋 다 `specs/2026-09-08-upload-image-downscale.md` 를 근거로 적었는데, 이 회차가 그 스펙을
  `specs/archive/` 로 옮겼다. 경로가 한 단계 어긋나 근거를 따라가지 못한다.
- **상태**: 해소됨 (2026-09-09, PR #480 `93cb002b5` — 셋 다 `specs/archive/…` 로 고쳐졌다)
- **해소 메모**: 같은 파일들을 여는 라운드가 곧바로 왔고 함께 고쳐졌다. **다만 같은 유형이 즉시
  재발했다** — 이 회차가 스펙 넷을 새로 아카이브로 옮겨, 그 넷을 가리키던 코드 주석이 다시 한 단계
  어긋났다(OQ-P-396). 스펙을 옮길 때 코드 주석을 함께 보는 절차가 없다는 것이 반복되는 원인이다.

### [2026-09-09] 재인코딩이 만드는 세 갈래를 실기기가 아직 판정하지 않았다

- **ID**: OQ-P-390
- **출처**: `specs/archive/2026-09-08-upload-image-downscale.md` 「검증」·「주의」. 계획의 수동 검증
  Task 가 미체크로 남아 있다.
- **항목**: ① **없던 디코드가 생겼다** — 변경 전 업로드는 바이트 복사뿐이었다. 저사양 기기에서 배경
  긴 변 2049~4095 구간이 견디는지는 실기기가 처음 판정한다. ② **ICC 프로파일이 재인코딩에서
  소실된다** — Display P3 사진 배경이 sRGB 로 앉으며 채도가 변할 수 있는데 그 영향을 측정하지 않았다.
  ③ **미러링 EXIF 는 보정하지 않는다** — `exifOrientationToDegrees` 가 `TRANSPOSE`·`TRANSVERSE` 를 0 도로
  두므로 그 사진만 재인코딩 갈래와 통과 갈래의 방향이 갈린다.
- **상태**: 미해결 (자동 테스트로는 닿지 않는 갈래들)
- **해소 메모**: ③ 의 0 도 매핑은 `segmentation-preprocessing` 이 정한 저장소 규약이라 이 라운드가
  뒤집지 않았다. ①②는 눈에 띄는 회귀가 나오면 각각 별건으로 다룬다.

### [2026-09-09] 업로드본이 작아지면서 알맹이 커버리지 하한의 기준면이 흔들린다

- **ID**: OQ-P-391
- **출처**: `specs/archive/2026-09-08-topping-edit-empty-subject-guard.md` 「주의」와
  `specs/archive/2026-09-08-upload-image-downscale.md` 「결정 표」. 두 스펙이 같은 날 나란히 머지됐다.
- **항목**: 빈 알맹이 차단은 **편집 화면의 원본 해상도 비트맵**에서 알파 합을 재는데, 그 알맹이가
  서버로 나갈 때는 긴 변 상한까지 줄어든다. 하한을 통과한 알맹이가 축소 뒤에는 같은 하한에 못
  미치는 구간이 생긴다. 특히 `borderOnly` 진입은 판정에서 빠져 있어, 서버에서 받아 여는 토핑의
  커버리지가 이미 줄어든 판이다.
- **상태**: 미해결 (지금까지 드러난 오작동은 없음)
- **해소 메모**: 하한을 어느 좌표계에서 재는지가 정해진 적이 없다. 기획이 하한을 확정할 때
  **업로드본 기준인지 편집본 기준인지**를 함께 정하고, iOS 와 값·기준면을 맞춘다(OQ-P-386 과 한 묶음).

### [2026-09-10] 지난 캔버스 알럿의 정책 소스가 없다

- **ID**: OQ-P-392
- **출처**: `specs/archive/2026-09-09-past-canvas-alert.md`(사후 스펙), `CanvasMainViewModel#checkShowPastCanvasAlert`,
  `feature/groups/canvas/impl` 의 `canvas_main_closed_canvas_alert_*` 문자열.
- **항목**: 위키에 이 알럿을 정한 기획 문서가 없다. 코드가 확정한 것이 넷이다 — ① 노출 조건("그
  마감을 처음 확인하는 순간", 며칠 전 마감도 포함) ② 이 기기·이 그룹이 처음이면 조용히 기준선만
  세운다 ③ 인원 수를 **그 마감 당시 참여자**로 센다(오늘 멤버가 아니다) ④ 문구 3종.
- **상태**: 미해결 (동작함, 근거가 코드뿐)
- **해소 메모**: 기획이 이 알럿을 문서로 확정하면 위키에 소스를 넣고 사후 스펙을 그 근거로 다시
  묶는다. 특히 ②는 배포 시점 방어라 정책이 다르게 정해지면 첫 사용자 경험이 달라진다.

### [2026-09-10] 알럿 판정이 폴링 회차마다 돌아 실패가 반복 조회를 부른다

- **ID**: OQ-P-393
- **출처**: `CanvasMainViewModel#checkShowPastCanvasAlert`·`#closedCanvasMemberCount`.
- **항목**: 판정은 오늘 캔버스를 받을 때마다 돌고, 인원 수를 얻는 지난 캔버스 상세 조회가 실패하면
  `markSeen` 을 부르지 않는다(그 마감을 영영 못 보는 것을 막는 의도적 설계다). 그래서 그 조회가
  계속 실패하는 상황에서는 **폴링 회차마다 상세 조회가 한 번씩 더 나간다.**
  `launch(key = PAST_CANVAS_ALERT_KEY)` 가드는 동시 중복만 막고 재시도 자체는 막지 않는다.
- **상태**: 미해결 (적응형 주기로 최악의 빈도는 20초에 한 번까지 성기어졌다)
- **해소 메모**: 재시도 상한이나 백오프를 둘지는 알럿 정책이 확정된 뒤에 정한다(OQ-P-392와 한 묶음).
  실패 원인이 서버 쪽이면 적응형 주기의 실패-무시 규칙과 같은 축에서 다루는 편이 낫다.

### [2026-09-10] 같은 최소 노출 500ms 를 상수 둘이 각자 든다

- **ID**: OQ-P-394
- **출처**: `YGScaffoldV2` 의 `YG_LOADING_MINIMUM_VISIBLE_MILLIS`,
  `GroupListViewModel.REFRESH_MINIMUM_VISIBLE_MILLIS`.
- **항목**: 로딩 덮개와 당겨서 새로고침 인디케이터가 같은 문제("깜빡이기만 하면 무엇을 기다렸는지
  알 수 없다")를 같은 값으로 푸는데, 그리는 자리가 달라(스캐폴드 / `isRefreshing` 상태) 상수가 둘로
  갈렸다. 한쪽만 바뀌면 두 표시의 체감 길이가 어긋난다.
- **상태**: 미해결 (동작 영향 없음)
- **해소 메모**: 값을 디자인시스템 한 곳으로 모을지, 아니면 "덮개"와 "인디케이터"를 서로 다른
  기준으로 볼지 정한다. 후자면 두 값이 다른 것이 정상이므로 그 근거를 문서에 적는다.

### [2026-09-10] 캔버스 첫 페인트 판정에 자기 테스트가 없다

- **ID**: OQ-P-395
- **출처**: `CanvasMainRoute` 의 `paintedCanvasIds`·`sawLoading`·`observedCanvasId` 상태 셋과 그
  `LaunchedEffect`, `specs/archive/2026-09-10-canvas-feedback-fixes.md` 「테스트」.
- **항목**: 이 판정은 구현 중에 두 번 무너졌다가 잡혔다(첫 컴포지션의 `Loaded` 오인, 토핑도 배경도
  없는 캔버스에서 판정이 굳는 것). 그런데 상태가 전부 Route 컴포지션에 있어 ViewModel 테스트가 닿지
  않고, 계측 테스트도 없다. **덮개가 다시 뜨는 회귀도, 아예 안 뜨는 회귀도 자동으로는 안 잡힌다.**
  `paintedCanvasIds` 가 화면 수명 동안 단조 증가하는 것도 같은 이유로 관측되지 않는다.
- **상태**: 미해결
- **해소 메모**: 판정을 순수 함수나 상태 홀더로 떼면 JVM 유닛으로 덮인다. 떼는 김에 리셋 조건을
  한 자리에 모으면 위 두 회귀가 같은 테스트로 잠긴다.

### [2026-09-10] 스펙을 아카이브로 옮기는 절차가 코드 주석을 보지 않는다

- **ID**: OQ-P-396
- **출처**: OQ-P-389의 재발. 이 회차가 스펙 넷을 `specs/archive/` 로 옮겼고, 그중 셋을 코드 주석
  **여덟 자리**가 옛 경로로 가리킨다 — `UploadImagePlan`(세 자리)·`UploadImageUseCase`·
  `SourceLongSide`(topping-upload-source-scaled), `CanvasPollInterval`·
  `ParfaitFirebaseMessagingService`(canvas-adaptive-polling), `NavKeyAnalyticsScreen`
  (release-analytics-screen-tracking). topping-draft-usecase-extraction 만 가리키는 주석이 없다.
- **항목**: 스펙 이동은 문서 저장소에서 일어나고 주석은 코드 저장소에 있어, 옮기는 라운드가 그
  주석을 건드릴 계기가 없다. 같은 일이 직전 회차에도 있었다(OQ-P-389).
- **상태**: 미해결 (경로 문자열 정정, 동작 영향 없음)
- **해소 메모**: 둘 중 하나다 — ① 코드 주석이 파일 경로 대신 스펙 `id` 를 가리키게 바꾼다(이동에
  견딘다) ② `sync-tjyg-develop-baseline` 이 아카이브 이동 시 그 스펙을 가리키는 코드 주석을 함께
  세는 단계를 갖는다. ①이 반복을 끝낸다.

### [2026-09-10] 로그아웃이 지난 캔버스 알럿 기록을 지우지 않는다

- **ID**: OQ-P-397
- **출처**: `LogoutUseCase`(KDoc 이 "무엇을 지우는가의 단일 자리"라고 못 박는다),
  `PastCanvasAlertLocalDataSourceImpl`.
- **항목**: 새로 생긴 그룹별 확인 기록은 `DataStore` 에 남는데 로그아웃·탈퇴가 그것을 지우지 않는다.
  다른 계정으로 로그인해 같은 그룹에 들어가면 **이미 본 것으로 취급돼 알럿을 놓친다.**
- **상태**: 미해결 (기기를 공유해 계정을 바꾸는 경우에만 드러난다)
- **해소 메모**: `LogoutUseCase` 에 정리 한 줄을 더하거나, 키를 그룹 id 가 아니라 계정 id + 그룹 id
  로 묶는다. 앞엣것이 그 KDoc 의 규약과 맞다.

### [2026-09-10] 재참여 규정이 정책 문서에 없고 과거 토핑이 되살아난다

- **ID**: OQ-P-398
- **출처**: 서버 `d76b27a`(`fix: 그룹 탈퇴 후 재참여가 불가능하던 문제 해결`),
  `ParfaitGroupService.join` · `ParfaitGroupMember.rejoin` · `DeleteParfaitImageService`,
  [api/parfait-group.md](../api/parfait-group.md) · [api/parfait-image.md](../api/parfait-image.md).
- **항목**: 서버가 그룹 재참여를 열면서 세 가지를 한꺼번에 정했는데 정책 문서에 대응 조항이 없다.
  ① 재참여하면 **그룹 닉네임이 전역 닉네임으로 초기화**되고 탈퇴 전에 바꿔 둔 값은 복원되지 않는다.
  ② **Nametag-Chip을 다시 뽑는다**(위키 [[nametag-chip]]은 "타입 고정"만 적는다).
  ③ **멤버십 행 id가 유지된다** — 그래서 탈퇴 중 `(알수없음)`·`DEFAULT`로 보이던 그 사람의 과거 토핑이
  재참여 후 **새 닉네임·새 칩으로 되살아나고**, 아직 `ACTIVE`인 캔버스라면 **편집·삭제 소유권까지 돌아온다.**
  ③이 의도인지, 아니면 재참여를 새 사람으로 볼 것인지가 기획에 없다.
- **상태**: 미해결 (서버 동작은 확정, 정책 근거만 없음. 앱 쪽 표면 영향은 별도 확인 대상)
- **해소 메모**: 기획에 "탈퇴 후 재참여" 조항을 세우고 ①~③ 각각의 기대를 적는다. 과거 토핑을 다시
  자기 것으로 볼 이유가 없다면 소유권 판정을 멤버십 id가 아니라 참여 회차로 가르는 편이 맞다.

### [2026-09-10] 재시도 회복의 철회 근거인 단계 로그가 운영에서 모이지 않는다

- **ID**: OQ-P-399
- **출처**: [segmentation-retry-recovery 스펙](../superpowers/specs/archive/2026-09-10-segmentation-retry-recovery.md)
  「근거 등급」 × `data/utils/Logger.kt#repositoryLogger` × `core:util:jvm` `LoggerInitializer.kt`(PR #487 develop
  머지). 스펙은 판정 수단을 "순수 함수 유닛 테스트와 실사용 로그"로 두고, 조건부 세 항목(2단계 힌트 크롭 ·
  1단계 대비 스트레치 · 짧은 변 하한 확대)의 철회 조건을 단계 로그로 정했다. 그런데 `repositoryLogger`는
  `Loggers.create`가 만든 Kermit 로거이고, 출력처는 `Logger.setLogWriters(platformLogWriter())` 하나뿐이다.
- **항목**: ① 단계 로그가 **logcat에만 남는다.** 개발자 기기 밖에서 난 실패는 기록이 모이지 않는다.
  ② 회복 경로를 강제로 태우는 수단도 넣지 않았다(스펙 「제외」). 그래서 **철회 조건을 채울 데이터가 들어올 길이
  둘 다 막혀 있고**, 잠정값 표(스펙 4-1)는 판정을 거치지 않은 채 확정값처럼 굳는다.
  ③ 같은 뿌리가 OQ-P-344 ③(모듈 설치 실패 빈도를 셀 수단이 없다)에도 있다. 원격으로 나가는 통로로는
  #478이 붙인 Firebase Analytics가 이미 있다.
- **상태**: 미해결 (코드 사실은 확정. 원격 수집 여부는 결정 전)
- **해소 메모**: 둘 중 하나로 닫는다. 단계 결과의 요약(돈 단계·걸린 가드·최종 후보 수)만 원격으로 올리거나,
  스펙의 판정 수단을 "실패 사진을 확보한 뒤 로컬에서 재현"으로 고쳐 적는다. 앞쪽을 고르면 OQ-P-344 ③도 같은
  통로로 닫힌다.

### [2026-09-10] 재시도 회복 as-built가 미룬 항목과 실기기 확인이 추적처 없이 남았다

- **ID**: OQ-P-400
- **출처**: [segmentation-retry-recovery 스펙](../superpowers/specs/archive/2026-09-10-segmentation-retry-recovery.md) as-built 배너의
  「기록해 둔 결정」·「다음 라운드로 미룬 항목」 × [계획](../superpowers/plans/archive/2026-09-10-segmentation-retry-recovery.md)
  Task 8 Step 2(PR #487 develop 머지).
- **항목**:
  ① **30초 상한을 넘기면 빈 성공으로 접히고 끈적한 플래그가 선다.** 그 사진은 사다리를 다시 돌지 않는다. 그런데
  스펙 §1이 플래그를 세우는 근거는 "입력이 같으면 결과도 같다"이고, 타임아웃은 입력이 아니라 기기 부하나 모듈
  설치 대기(`INSTALL_TIMEOUT_MS` 20초가 같은 예산 안에서 소비된다) 같은 자원 조건이다. 근거가 닿지 않는 경우까지
  같은 플래그로 접었다.
  ② 단계 로그가 2단계 크롭의 소스 치수를 "원본"으로 표기한다(크롭 퍼센트 로그로 복구는 된다).
  ③ `focusStage`의 2048x1536/69% 경계 픽스처가 없다.
  ④ 예외 경로에서 소유한 중간 비트맵을 회수하지 않는다(GC가 회수하므로 OOM 경로의 피크에만 영향이 있다).
  ⑤ `SegmentationMask.kt#maskSubjectAlpha`를 부르는 곳이 테스트뿐이다. 위임 껍데기로 남겼지만 실사용 경로가 없다.
  ⑥ **실기기 확인이 0회다.** 회복 경로는 강제 수단이 없어 돌지 않았고, 1차 경로 회귀 확인(계획 Task 8 Step 2:
  평범한 사진·다중 피사체·색 보존·로그 꼬리)도 기록이 없다. Task 4·5가 1차 경로의 수확을 옮기고 일반화했으며,
  캔버스 밖 후보를 버리는 동작 변경도 1차 경로에 걸렸다.
- **상태**: 미해결 (②~⑤는 정리 대상, ①은 결정 대상, ⑥은 사람이 할 확인)
- **해소 메모**: ⑥의 1차 경로 확인이 가장 먼저다. 매 촬영이 지나는 경로이기 때문이다. ①을 고치려면 타임아웃과
  끝까지 돈 경우를 ViewModel이 구별해야 하므로 `recoverCandidates`의 결과 표현을 함께 봐야 한다. ⑤는 위임 껍데기를
  지우거나, 테스트가 두 함수를 직접 부르게 옮기면 닫힌다.

### [2026-09-10] C-103-Error 두 번째 버튼이 디자인 확정본과 다른 이름·동작이 됐는데 근거가 문서에 없다

- **ID**: OQ-P-401
- **출처**: 커밋 `3217e62f7`(`feat: 실패 화면의 편집 없이 사용을 직접 편집으로 바꿔 C-104 로 바로 보낸다`, PR #487
  develop 머지) × `feature/segmentation/impl` `strings.xml`(`segmentation_error_edit_manually`) ×
  [c103-error-use-original 스펙](../superpowers/specs/archive/2026-09-05-c103-error-use-original.md). 그 스펙은 「편집 없이 사용」을
  **디자인 `C-103-Error` 확정본**의 버튼으로 적었다.
- **항목**: ① 버튼 라벨이 「직접 편집」, 설명이 「다시 시도하거나 직접 편집할 수 있어요」로 바뀌었다. 디자인이
  먼저 바뀌었는지 코드가 먼저 바꿨는지를 이 저장소에서 확인할 수 없고, 커밋 메시지도 동작만 적는다.
  ② 동작도 달라졌다. 원본을 곧장 토핑 재료로 쓰던 버튼이 C-104 수동 편집을 거치게 됐다. 사용자는 한 단계를 더
  거치는 대신 배경을 지울 기회를 얻는다. ③ 위키에는 C-103-Error 버튼 조항이 없어 정책 쪽 기준도 없다.
  OQ-P-344 ①이 묻는 "우회로를 어떻게 둘지"와 같은 자리다.
- **상태**: 미해결 (디자인 확인 대기)
- **해소 메모**: 디자인 `C-103-Error` 최신본을 확인한다. 라벨·설명이 맞으면 c103-error-use-original 스펙의 🔁 배너에
  근거를 적고 닫는다. 다르면 문구를 디자인에 맞춘다.

### [2026-09-16] 리모트 빌드 캐시 도입 문턱값이 없고, 하니스는 머지됐지만 측정이 안 돌았다

- **ID**: OQ-P-402
- **출처**: PR #499 develop 머지([build-cache-measurement-harness 스펙](../superpowers/specs/archive/2026-09-14-build-cache-measurement-harness.md)
  「주의 / 열린 질문」) — 그 스펙이 문턱값과 커밋 쌍 선정을 열린 채로 두고 아카이브로 갔다. 추적 주체가
  사라지므로 여기로 옮긴다.
- **항목**: ① `T_local − T_remote`가 얼마 이상이면 리모트 캐시를 세울 값어치가 있다고 볼 것인지 합의된
  선이 없다. **측정 후에 정하면 결과에 맞춰 기준이 움직이므로 돌리기 전에 정해야 한다.** ② 커밋 쌍
  `P1`(일상)·`P2`(코어 ABI 변경)을 무엇으로 고정할지 정해지지 않았다 — 러너는 두 SHA가 같거나 트리가
  같은 쌍만 거부하고, **대상 그래프의 입력이 실제로 바뀌는지는 보지 않는다.** 무신호 쌍을 고르면
  `S3`·`S4`가 둘 다 `UP_TO_DATE`로 끝나 핵심 값이 0에 수렴하고도 에러 없이 통과한다. ③ 하니스가
  머지됐을 뿐 **측정을 아직 아무도 돌리지 않았다.** 게이트 G0(`check-relocatability.sh`)도 마찬가지다 —
  거기서 깨지면 핵심 값이 얼마든 도입 가치는 0이다.
- **상태**: 미해결 (**동작 영향 0** — `tools/`와 `.gitignore`뿐이라 앱 빌드에 닿지 않는다. 판단 근거가
  아직 만들어지지 않은 것이 문제다)
- **해소 메모**: 순서가 있다. ①을 먼저 정하고, G0를 돌려 통과하면 ②로 쌍을 고정한 뒤 측정한다.
  측정치는 머신마다 달라 커밋하지 않으므로(`runs/`는 `.gitignore`), 판단에 실제로 쓴 수치만 후속 스펙으로
  옮긴다. `T_remote`가 전송 시간을 0으로 놓는 **낙관적 상한**이고 `S4` 쪽이 사전 빌드를 더 돌아 한 방향으로
  부풀려진다는 점을 문턱값과 함께 읽어야 한다. 빌드 성능의 다른 두 축(`org.gradle.parallel`·configuration
  cache)은 [2026-08-11] 항목이 따로 추적한다.

### [2026-09-17] `Navigator` 만 명시적 backing field 로 옮겨 상태 노출 관용구가 두 갈래가 됐다

- **ID**: OQ-P-403
- **출처**: `core/navigation/.../Navigator.kt`(PR #504 develop 머지) × `core/ui/.../BaseViewModel.kt`.
  청소 라운드가 `Navigator` 의 `private val _backStack` + `val backStack: List<NavKey> get() = _backStack`
  쌍을 **Kotlin 명시적 backing field** 한 줄(`val backStack: List<NavKey>` 아래 `field: SnapshotStateList<NavKey> = ...`)로
  바꿨다. develop 전체에서 이 형태를 쓰는 곳은 **`Navigator` 하나뿐**이고, 같은 일을 하는
  `private val _x` + 공개 접근자 쌍은 넷이 남아 있다 — `BaseViewModel`(`_state`/`state`,
  MVI 베이스라 모든 ViewModel 이 상속한다) · `BaseViewModel._effect` · `GroupLocalDataSourceImpl._myGroups` ·
  `CanvasPoller._refreshFailures`. Kotlin 은 `2.4.10` 이고 `build-logic`·`gradle.properties` 어디에도
  이 기능을 켜는 컴파일러 인자가 없다(추가 설정 없이 컴파일된다).
- **항목**: ① 두 관용구 중 무엇을 프로젝트 관용구로 삼을지. ② 삼는다면 `BaseViewModel` 까지 옮길지 —
  `state` 는 `_state.asStateFlow()` 라 `field` 로 옮기면 래퍼 호출이 사라지고 공개 타입만 `StateFlow<S>` 로
  남는다. 베이스 클래스라 한 번 바꾸면 모든 ViewModel 의 상태 노출 형태가 같이 정해진다.
  ③ 옮기지 않기로 하면 `Navigator` 를 되돌릴지, 예외로 둘지.
- **상태**: 미해결 (**동작 영향 0** — 공개 시그니처도 Compose 의 스냅샷 읽기도 그대로다. 관용구가
  둘로 갈린 것만 문제다)
- **해소 메모**: 정하면 [state-management](../architecture/state-management.md) 에 관용구를 한 줄 박고,
  그 문서가 지금 `BaseViewModel` 의 상태 노출 형태를 **적지 않는다**는 공백도 함께 닫는다. ②를
  고르면 ADR 을 새로 열 것 없이 그 문서 갱신으로 충분하다 — 결정이 모듈 경계나 의존 방향을 바꾸지 않는다.

### [2026-09-21] 도달 불가 화면을 지우자 `NavTransition.Fade` 프리셋이 사용처 0으로 남았다

- **ID**: OQ-P-404
- **출처**: `core/navigation/.../NavTransition.kt#Fade`(PR #514 develop 머지) — 유일한 소비처였던
  `NavKeyCanvasEdit` 엔트리(`metadata = NavTransition.Fade.metadata`)가 화면째 삭제되면서
  (OQ-P-239) 프리셋을 다는 엔트리가 **0건**이 됐다. develop 전체에서 `NavTransition.` 을 부르는
  자리는 `MainRoute`·`RootRoute` 의 `Default` 여섯 줄뿐이다. **`NavTransitionTest` 는 이것을
  잡지 못한다** — 잠그는 것이 "프리셋마다 세 키가 다 있는가" 하나라, 프리셋을 아무도 안 써도
  테스트는 그대로 통과한다. 컴파일도 `object` 상수라 경고를 내지 않는다.
- **항목**: ① 남길지 — 남긴다면 근거는 "다음 공유 요소 전환에서 쓴다"인데, 그 화면이 언제 올지
  정해진 바 없다. ② 걷어낼지 — 걷으면 `NavTransitionTest` 의 `Fade` 케이스와
  [navigation-flow](../architecture/navigation-flow.md) 「`Fade` 를 고르는 기준」 문단도 함께 간다.
  ③ 기준 문단만 남기고 프리셋을 지울지 — 판단 근거는 문서에 남기고 코드만 비우는 형태다.
- **상태**: 미해결 (**동작 영향 0** — 아무도 안 쓰므로 무엇을 골라도 화면은 안 바뀐다)
- **해소 메모**: 같은 부류가 셋 있다 — `clickableYGNoRipple`(존치로 닫힘, 소비처가 실제로 생겼다) ·
  `animateToppingPlacement`(삭제로 닫힘) · `AppleDesignGuideColors`(삭제로 닫힘). 앞의 둘이 갈린
  기준은 "소비처가 생길 길이 있는가"였고, `Fade` 는 그 판정이 **공유 요소를 쓰는 화면을 다시
  만들 계획이 있는가**에 달렸다. 정하면 [navigation-flow](../architecture/navigation-flow.md)
  「화면 전환」 절의 🔁 문단을 함께 정리한다.

### [2026-09-21] 같은 정책 위키가 두 저장소에 각각 존재하는데 어느 쪽이 정본인지 정한 문서가 없다

- **ID**: OQ-P-405
- **출처**: TJYG-Android `wiki/`(PR #511 develop 머지)와 이 저장소의 `wiki/`. 둘이 **같은 원본을
  각각 ingest한 결과**다. 소스 층은 사실상 같다 — 39건이 파일명까지 일치하고 차이는 한글 자모
  정규화(NFC/NFD)뿐이다. 갈라진 것은 그 위의 개념 층이다. 이쪽 `concepts/`는 19건이고 화면 단위로
  잘게 쪼갠 한글 이름(`카메라-뷰파인더`·`무한-파르페-그리드`·`nametag-chip`)인데, 저쪽은 8건이고
  굵게 묶은 영문 이름(`account`·`canvas`·`topping-placement-pipeline`·`mvp-scope-limits`)이다.
  `synthesis/`도 다르다 — 이쪽은 `open-questions`와 lint 보고서 아홉, 저쪽은 `spec-version-history`
  하나다. 뼈대도 다르다 — 저쪽은 `pages/`로 한 겹 내린 구조에 `routing.json`·`references/`·graphify가
  있고, 이쪽은 평탄 구조에 `personal-private` 서브모듈이 붙는다.
  ⚠️ 이식 설계 문서(`docs/superpowers/specs/2026-09-17-wiki-port-design.md`)는 결정 표에
  **"콘텐츠는 옮기지 않는다 · 원본 위키가 계속 정본 · 여기는 빈 스캐폴드로 시작해 새로 ingest한다"**를
  적었는데, 머지된 트리에는 같은 원본 39건이 그대로 있다. 결정 표의 문장과 실제 트리가 어긋난다.
- **항목**: ① **정책 정본을 어디로 둘 것인가** — parfait 문서(`specs/`·`architecture/`·이 문서)는
  화면 정책을 근거로 인용할 때 위키를 가리킨다. 그 대상이 지금 둘이다. ② **갈라진 개념 층을
  어떻게 할 것인가** — 같은 소스에서 나온 두 벌의 개념 분해라 한쪽이 갱신돼도 다른 쪽은 모른다.
  합칠지, 한쪽을 파생으로 선언할지, 둘 다 살릴지. ③ **플랫폼 비종속 전제와 충돌하는가** —
  [`parfait/CLAUDE.md`](../index.md)는 "위키는 iOS가 붙어도 그대로 재사용되도록 플랫폼
  비종속으로 쓴다"를 근거로 축을 갈랐는데, 이번에 위키 한 벌이 **Android 전용 저장소 안**으로
  들어갔다. 그 전제를 유지할지 고쳐 적을지.
- **상태**: 미해결 (**구현 영향 0** — 코드가 한 줄도 안 바뀌었다. 문서 근거의 문제다)
- **해소 메모**: ①이 정해지면 [`parfait/CLAUDE.md`](../index.md) 「정책·기획 자체는 여기가
  아니라 위키다」 문단과 [`doc-baseline`](../doc-baseline.md)의 근거 서술이 같은 대상을 가리키도록
  고친다. ②는 사람에게 물어야 답이 나온다 — 두 개념 분해 중 어느 것이 쓰기 편한지는 이 문서가
  판정할 수 없다. ③은 iOS 문서가 실제로 생기기 전에 정하는 편이 싸다.

### [2026-09-22] parfait 문서 이관에서 위키 개념 3건이 아직 없어 7곳의 링크를 해제했다

- **ID**: OQ-P-406
- **출처**: parfait 구현 문서 트리를 이 저장소 `docs/`로 이관하면서(Task 3, `migrate_links.py`)
  이 저장소 위키에 아직 ingest되지 않은 개념 페이지 3건 — [[누끼-따기]]·[[토핑]]·[[캘린더-컴포넌트]] —
  을 가리키던 마크다운 링크 7곳을 해제했다. 원본에서는 `[[개념]] ([link](.../wiki/concepts/개념.md))`
  형태로 위키 본문 링크가 따로 있었는데, 이관 도구의 `PATH_MAP`에 `wiki/concepts/`는 대응 목적지가
  없어 링크만 걷고 라벨 텍스트를 남겼다.
- **항목**: ① [[누끼-따기]] 5곳 — [specs/README.md](../superpowers/specs/README.md),
  [specs/archive/2026-08-23-c103-multi-subject-selection.md](../superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md)(3곳),
  [architecture/design-system.md](../architecture/design-system.md). ② [[토핑]] 1곳 —
  [specs/archive/2026-09-11-g001-group-list-topping-border.md](../superpowers/specs/archive/2026-09-11-g001-group-list-topping-border.md).
  ③ [[캘린더-컴포넌트]] 1곳 —
  [specs/archive/2026-08-01-designsystem-bar-listdate-components.md](../superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md).
- **상태**: 미해결
- **해소 메모**: 같은 `raw/`에서 [[누끼-따기]]·[[토핑]]·[[캘린더-컴포넌트]] 세 개념이 실제 페이지로
  ingest되면, 위 7곳의 링크를 되살리고 이 항목을 해소됨으로 바꾼다.

<!-- oq-next: 407 -->
