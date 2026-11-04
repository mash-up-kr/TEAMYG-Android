---
id: screenshot-comparison-ci
title: PR 스크린샷 비교 리포트 CI (Roborazzi screenshot comparison)
status: draft
category: behavior-spec
platforms: android
verified: 2026-11-04
related_code:
  - .github/PULL_REQUEST_TEMPLATE.md
  - .github/workflows/test.yml
  - build-logic/convention/build.gradle.kts
related_adr:
related_spec:
related_architecture:
supersedes:
superseded_by:
tags: [spec, parfait, ci, screenshot]
---

# Spec: PR 스크린샷 비교 리포트 CI

## 목표

UI를 바꾼 PR에서 리뷰어가 바뀐 화면을 **작성자가 아무것도 하지 않아도** 볼 수 있게 한다.

지금은 PR 템플릿의 "UI 변경 시 스크린샷 첨부" 체크와 Before / After 표에 기대는데, 작성자가
직접 찍어 붙여야 해서 잘 지켜지지 않았다. CI가 PR의 `@Preview` 렌더를 base와 비교해 달라진
화면과 새로 생긴 화면을 PR 코멘트에 이미지 표로 단다.

## 성공 기준

- UI를 바꾼 PR에 몇 분 안에 봇 코멘트가 붙고, 표 안 이미지가 GitHub에서 바로 보인다.
- 리포트가 PR이 바꾼 화면만 보여준다. 다른 PR이 develop에 넣은 변경이나 렌더 흔들림이 섞이지 않는다.
- push가 짧은 간격으로 여러 번 와도 코멘트는 항상 PR의 최신 head 기준이다.

## 범위

포함:

- `@Preview`가 있는 모든 모듈: `core:designsystem`, `core:ui`, `feature:*:impl`
- 바뀐 화면(Before / Diff / After), 새로 생긴 프리뷰(After만), 사라진 프리뷰(이름만)
- PR 템플릿 문구 수정: 리포트가 주 수단, 수동 첨부는 보조
- 닫힌 지 30일 지난 PR의 이미지 브랜치 정리

제외(non-goal):

- 머지 차단. 잡은 diff가 있어도 성공으로 끝나는 **리포트**다. 필수 체크로 묶지 않는다.
- 애니메이션·화면 전환·제스처 같은 동작. 이건 작성자 수동 첨부(GIF·영상)로 남는다.
- 프리뷰가 없는 화면. 프리뷰 작성 의무나 누락 검사는 만들지 않는다.
- 계측(에뮬레이터) 스크린샷.
- 골든 이미지를 레포에 커밋하는 방식(Roborazzi `verify`).

anti-goal(돌아가도 실패로 보는 것):

- 렌더가 흔들려 UI를 건드리지 않은 PR에도 diff가 뜨는 것. 리뷰어가 리포트를 무시하게 되면
  없느니만 못하다.
- 작성자에게 새 할 일(골든 재기록, 명령 실행)을 만드는 것.
- 오래된 실행의 결과가 최신 코멘트를 덮어쓰는 것.

## 접근 (Approaches)

| 안 | 내용 | 판단 |
|---|---|---|
| A. develop 골든 아티팩트 | develop push마다 골든을 렌더해 아티팩트로 올리고 PR이 내려받아 비교 | 기각. develop이 움직인 직후 골든 기록이 끝나기 전에 push하면 이전 골든과 비교해 오탐이 난다. base가 develop이 아닌 stacked PR(최근 머지 20건 중 5건)은 골든이 없어 리포트가 안 나온다 |
| **B. PR 잡 안에서 base도 렌더** | 한 잡에서 `base.sha`를 checkout해 record → PR merge ref를 checkout해 compare | **채택.** 기준과 대상이 같은 잡·같은 머신에서 나와 경합이 원천적으로 없다. stacked PR은 부모 브랜치를 base로 자연스럽게 비교한다. 골든 잡·아티팩트 보관이 필요 없다. 대가는 PR 잡 시간(렌더 2회). public 레포라 비용은 없다 |
| C. 골든 레포 커밋 | Roborazzi `verify` + 골든 커밋 | 기각. 작성자가 매번 재기록·커밋해야 하고, merge/rebase 혼재 환경에서 바이너리 충돌이 난다 |

merge/rebase 혼재는 B에서 영향이 없다. 비교 대상은 GitHub이 만든 `refs/pull/N/merge`(head + base tip)이고
기준은 같은 시점의 `base.sha`라, 작성자가 브랜치를 어떻게 맞췄는지와 무관하게 PR이 바꾼 것만 잡힌다.

## 설계

### 트리거

`on.pull_request.paths`로 건다. 필수 체크가 아니므로 `test.yml`의 스텝 건너뛰기 우회는 필요 없다.

```
feature/**
core/designsystem/**
core/ui/**
gradle/libs.versions.toml      # Compose BOM·Coil 버전이 렌더를 바꾼다
build-logic/**                 # Compose 컴파일러 설정, 스크린샷 convention plugin
gradle/wrapper/**
.github/workflows/<비교 워크플로>.yml
```

`core/analytics` `navigation` `util` `testing`은 UI 리소스·프리뷰가 없어 뺀다. 애매하면 넣는 쪽으로
판단한다 — 넓어서 생기는 손해는 잡 시간, 좁아서 생기는 손해는 리뷰어가 못 보는 것이다.

### 워크플로 구성 (3개)

1. **비교** (`pull_request`) — PR 코드를 빌드한다. 권한은 `contents: read`뿐, 시크릿 없음.
2. **코멘트** (`workflow_run`, 비교 완료 시) — 빌드하지 않고 산출물만 읽는다. 이미지 브랜치 push와
   코멘트 쓰기 권한을 여기에만 준다. 쓰기 토큰과 PR 코드가 같은 잡에 놓이지 않게 하려는 분리다.
3. **정리** (`schedule`, 하루 1회) — 닫힌 지 30일이 지난 PR의 `ui-snapshots/pr-<N>` 브랜치를 지운다.
   PR 코드를 checkout하지 않는다. 닫히자마자 지우지 않는 이유: 코멘트 이미지는 커밋 SHA를 가리키는데
   ref가 사라지면 GitHub GC 후 깨진다. 머지 직후 "그때 무엇을 리뷰했는지" 다시 볼 기간을 남긴다.
   30일 뒤 머지된 PR의 코멘트 이미지가 깨지는 것은 받아들인다.

### 비교 잡 흐름

1. `base.sha` checkout → 더미 `local.properties` → `recordRoborazziDebug --continue`
   → 렌더 결과를 잡 밖 임시 디렉터리로 옮긴다(다음 checkout이 지우지 않게).
2. PR merge ref checkout → 1의 결과를 각 모듈 `build/outputs/roborazzi`에 복원
   → `compareRoborazziDebug --continue`.
3. 비교 결과, 메타데이터(PR 번호, `head.sha`, `base.sha`, 상태)를 아티팩트로 올린다.

상태값은 최소 `ok` · `merge-conflict`(merge ref가 없음) · `no-base-setup`(base 커밋에 스크린샷
구성이 없음) · `failed`(렌더 자체가 깨짐)를 구분한다. "diff 없음"과 "비교 못 함"을 같은 코멘트로
보고하지 않는다.

- `no-base-setup`: 이 CI를 도입하는 PR 자체, 도입 전에 갈라진 브랜치와 그 위의 stacked PR은 base에
  `recordRoborazziDebug`가 없다. head의 구성을 base에 이식해 렌더하지 않고, "base에 스크린샷 구성이
  없어 비교하지 못했다"는 코멘트를 남긴다. (설계자 판단 — 이식은 잡 흐름을 크게 복잡하게 만들고,
  도입 직후 과도기에만 해당한다)
- 일부 모듈만 렌더에 실패한 경우(`--continue`): 모듈 단위로 실패를 감지해 **그 모듈의 행은 표에서
  뺀다**. 한쪽에서만 깨진 모듈의 프리뷰가 전부 "신규"나 "삭제"로 뜨는 오탐을 막기 위해서다. 실패한
  모듈 이름은 코멘트 맨 위에 따로 적는다. 비교 잡은 이 경우에도 성공으로 끝낸다 — 코멘트 워크플로가
  성공한 실행만 받기 때문이고, 컴파일 실패는 `test.yml`이 이미 빨간 체크로 알린다. (설계자 판단)

시크릿은 넘기지 않는다. `pull_request`는 PR이 올린 Gradle 코드를 그대로 돌리므로 실제 키를 주면
빼낼 수 있다. `app/build.gradle.kts`가 `local.properties`를 무조건 읽으므로 더미 파일은 필요하다.

### 렌더 결정성

B는 같은 머신에서 두 번 렌더하므로 흔들림이 그대로 오탐이 된다. 그래서:

- `@YGPreview`는 `phone` 하나만 남기고 나머지 5변형은 지우지 않고 주석 처리한다(사용자 결정). 원래 6변형(phone·foldable·foldable-fold × light·night)이 사용처 106곳에 붙어
  PR마다 600장 넘게 두 번 렌더하게 되고, `YGDarkColorScheme`이 `YGLightColorScheme`과 같아 night 변형은 같은 그림이다.
- 모든 프리뷰는 같은 코드를 두 번 렌더하면 픽셀이 같아야 한다. 도입 시 base와 head를 같은 커밋으로
  돌려 diff 0건을 확인한다.
- 네트워크·비동기 리소스를 쓰는 프리뷰 파일 5개는 고치거나 제외 어노테이션을 단다:
  `TermAgreeScreen` `CanvasToppingPlaceScreen` `CanvasImageSaveScreen` `GroupListScreen` `WebViewScreen`.
  Coil은 프리뷰용 이미지 핸들러, Lottie는 커스텀 tester에서 `LottieTask.EXECUTOR`를 동기 executor로 교체,
  WebView는 제외가 기본 방향이다.
- 글자 크기 고정은 PR #669(ADR-0036, 미머지)에서 다룬다. Robolectric 기본 `fontScale`이 1.0이라 그 PR과 무관하게 결정성은 유지된다. 이번 작업의 ADR 번호는 작성 시점의 다음 빈 번호를 쓴다.

### 순서 보장 (짧은 간격의 연속 push)

오래된 실행의 결과가 최신 코멘트·이미지를 덮어쓰지 않게 세 겹으로 막는다.

1. **비교 워크플로 concurrency**: 그룹 키는 PR 번호, `cancel-in-progress: true`. 새 push가 오면
   이전 비교는 취소된다.
2. **코멘트 워크플로 진입 조건과 concurrency**: 비교 `conclusion == 'success'`일 때만 돈다(취소된
   실행은 코멘트를 남기지 않는다). 그룹 키는 PR을 가리키는 고정 값(`workflow_run`의
   head repository + head branch)으로 두고 `cancel-in-progress: true`.
3. **stale 검사**: 코멘트 잡이 쓰기 직전에 API로 PR의 현재 `head.sha`를 읽어 아티팩트의 `head.sha`와
   다르면 아무것도 쓰지 않고 끝낸다. 1·2를 지나 지연 도착한 `workflow_run`도 여기서 걸러진다.
   이미지 push 직전과 코멘트 쓰기 직전 두 번 검사한다.

또한 코멘트의 이미지 URL은 브랜치 이름이 아니라 **이미지 커밋 SHA**로 건다
(`blob/<commit-sha>/<path>?raw=true`). 이미지 브랜치가 나중에 덮어써져도 각 코멘트는 자기가 만든
이미지만 가리킨다.

코멘트 본문에 비교한 `base.sha`와 `head.sha`(짧은 형태)를 적는다. 다음 경우 리포트가 낡을 수 있고,
리뷰어가 SHA로 알아볼 수 있게 하려는 것이다:

- develop이 움직였지만 PR에 push가 없을 때
- 이후 push가 `paths`에 걸리지 않아 비교가 돌지 않았을 때

### 코멘트

- PR당 하나. 숨은 마커(`<!-- parfait-ui-diff-report -->`)로 찾아 덮어쓴다.
- 바뀐 화면이 0건이면 코멘트를 **새로 만들지 않는다**. 이미 코멘트가 있는 PR만 "바뀐 화면 없음"으로
  갱신한다. 트리거 범위에 `feature/**`가 있어 UI와 무관한 PR도 대부분 잡이 도는데, 그 모두에 봇
  코멘트를 붙이지 않기 위해서다. `no-base-setup` · `merge-conflict` · `failed`는 0건이 아니라 "비교
  못 함"이므로 코멘트를 남긴다.
- 섹션: 변경(Before / Diff / After), 신규(After), 삭제(이름만). 행이 많으면 상한을 두고 나머지는
  이미지 브랜치 링크로 넘긴다(본문 65536자 제한).
- 아티팩트는 PR 코드가 만든 것이므로 신뢰하지 않는다. PR 번호·SHA는 형식 검증, 파일 경로는
  `build/outputs/roborazzi` 밖·`..`·심볼릭 링크·개행 포함을 거부, `git add`는
  `--literal-pathspecs` + NUL 구분 목록.
- 이미지는 orphan 브랜치 `ui-snapshots/pr-<N>`에 force push. develop 히스토리에 들어가지 않는다.

### Gradle 구성

- Roborazzi + Robolectric + ComposablePreviewScanner를 version catalog에 추가.
- 스크린샷 convention plugin을 새로 만들어 `@Preview`가 있는 모듈에 적용한다. 스캔 패키지는 모듈
  경로에서 유도, private 프리뷰 포함, 제외 어노테이션 필터.
- 커스텀 tester(Lottie 동기 executor 등)를 둘 **Android** 테스트 지원 모듈이 필요하다. `core:testing`은
  `parfait.kotlin.jvm`이라 쓸 수 없다.
- `robolectric.pixelCopyRenderMode=hardware`, `isIncludeAndroidResources = true`.

### PR 템플릿

체크 항목 "UI 변경 시 스크린샷 첨부"를 "UI 변경 시 스크린샷 비교 리포트 확인 (동작·애니메이션·프리뷰가
없는 화면은 직접 첨부)" 취지로 바꾸고, Screenshots 섹션 안내도 같은 취지로 고친다.

### 문서

B 채택 이유(A·C 기각)는 ADR로 남긴다. 렌더 결정성 규칙(프리뷰는 두 번 렌더해 같아야 한다, 제외
어노테이션을 언제 쓰는가)은 개발자가 프리뷰를 쓸 때 볼 자리에 둔다.

## 빌더에게 맡기는 것

- 워크플로·스크립트·모듈·플러그인·어노테이션 이름. 단 다른 프로젝트의 이름을 가져오지 않고 이 레포
  기준으로 새로 짓는다. 예외는 의미상 대안이 없는 것뿐이다 — 라이브러리가 정한 이름(Roborazzi의
  `recordRoborazziDebug`·`compareRoborazziDebug`, `build/outputs/roborazzi`), GitHub이 정한 이벤트·
  ref 이름(`refs/pull/N/merge`) 등. 이 문서에 적힌 이름(`ui-snapshots/pr-<N>`,
  `<!-- parfait-ui-diff-report -->`)은 이 기준으로 정한 것이다
- 새 프리뷰 감지 방식. Roborazzi compare가 기준 없는 프리뷰를 어떻게 내놓는지 확인하고, 안 되면 base
  record 목록과 head 결과 목록을 직접 대조한다
- 5개 불안정 프리뷰 각각을 고칠지 제외할지
- 잡 timeout, Gradle 캐시 설정(기존 `setup-android-build` 재사용 가능 여부)
- 코멘트 행 상한, 표 레이아웃 세부

## 주의 / 열린 질문

- PR 잡 시간: 렌더를 2회 하므로 길어진다. 첫 실측 후 timeout을 정한다.
- fork PR: 구조상 안전하지만(시크릿 없음, 쓰기 분리) 이 레포는 팀 내부 브랜치만 쓰므로 fork 전용
  다듬기는 하지 않는다.
