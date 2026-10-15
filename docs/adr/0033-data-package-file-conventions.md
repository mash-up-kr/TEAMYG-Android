---
id: ADR-0033
title: data·domain 패키지는 역할별로 가르고 파일당 타입 하나를 둔다
status: accepted
date: 2026-09-27
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
  - ADR-0001
  - ADR-0017
related_spec:
related_architecture:
  - data-layer.md
  - module-structure.md
platforms: android
tags: [adr, parfait, data, domain, convention]
---

# ADR-0033: data·domain 패키지는 역할별로 가르고 파일당 타입 하나를 둔다

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락
중첩 응답 DTO를 상위 응답 파일에 함께 두는 예외가 있었다. 서버가 한 파일에 담은 것을 앱도 한 파일에
담아야 계약과 눈으로 대조된다는 근거였다. 같은 느슨함이 다른 자리로 번졌다. 모델 파일에 매핑 확장이
붙고, `repository/`에 리포지토리가 아닌 인터페이스가 들어가고, `source/` 아래 이름이 제각각이 됐다.

그 결과 한 파일 안에 **데이터 모양을 담는 클래스와 동작을 담는 클래스가 섞였다.** 세 가지가 나빠졌다.
- **유지보수** — 한 파일을 고치면 무관한 타입까지 diff에 걸리고, 무엇이 어디에 기대는지 파일 경계로 드러나지 않는다.
- **가독성** — 파일을 열어야 그 안에 무엇이 있는지 안다.
- **탐색** — 타입 이름으로 파일을 찾을 수 없다. 중첩 타입은 부모 이름을 알아야 찾는다.

## 결정
`:data`·`:domain`의 패키지는 **역할 하나에 이름 규칙 하나**로 가르고, 파일에는 **공개 top-level 타입 하나만** 둔다.

- **파일당 타입 하나** — private이 아닌 top-level `class`·`interface`·`object`는 파일마다 하나이고 파일명은
  그 이름이다. 중첩 응답·요청 DTO도 제 파일을 갖는다. 예외는 sealed 하위 타입, companion, 그 파일 안에서만
  쓰는 `private` 헬퍼뿐이다.
- **`repository/`에는 Repository만** — `:domain`은 `~Repository` 인터페이스, `:data`는 `~RepositoryImpl` 구현만 둔다.
  리포지토리가 기대는 다른 인터페이스는 역할 패키지로 보낸다. 구현이 `:app` 같은 바깥 모듈에 있는 공급자는
  `domain/provider`(`DeviceTokenProvider`), 데이터 접근은 `data/source`다.
- **`source/<도메인>/{local,remote}` 이름 규칙** — `local`에는 `~LocalDataSource`·`~LocalDataSourceImpl`,
  `remote`에는 `~RemoteDataSource`·`~RemoteDataSourceImpl`만 둔다(`TokenLocalDataSource`,
  `PresignedUploadRemoteDataSource`). 서버 응답을 VO로 바꾸는 매핑은 `source/<도메인>/mapper/`로 뺀다.
  여러 도메인이 쓰면 `source/common/mapper/`에 둔다.
- **저장소 모델과 매핑 분리** — 저장소 모델은 `data/model/entity/`에 둔다. Entity↔VO 매핑 확장은
  `data/model/mapper/entity/<타입>Mapper.kt`, 예외 매핑은 `data/model/mapper/exception/`에 둔다.
  모델 파일에는 매핑을 두지 않는다.
- **이름은 서버를 따른다** — 파일을 나눠도 wire DTO 이름은 서버 이름이다. 서버가 가른 두 배치자 타입은
  앱에서도 `PlacedByResponse`·`PlaceParfaitImagePlacedByResponse`로 갈린다.

규칙의 에이전트용 요약은 `.claude/rules/`(`one-type-per-file.md`·`repository-package.md`·`source-package.md`)에 있다.

## 대안
- **중첩 DTO를 상위 응답 파일에 유지** — 서버 파일과 한눈에 대조된다. 그러나 대조는 `docs/api/` 계약 문서와
  DTO KDoc이 이미 맡고, 한 파일에 여러 타입을 두는 대가는 매일 파일을 찾고 고치는 쪽이 치른다.
  **→ 기각:** 드물게 하는 대조보다 늘 하는 탐색·수정을 우선한다.
- **도메인별 묶음 파일**(`AuthResponses.kt` 식) — 파일 수가 줄어든다. 그러나 ktlint `standard:filename`은
  top-level 선언이 하나인 파일에만 걸려서, 묶는 순간 파일명 검사를 피해 간다.
  **→ 기각:** 이름으로 찾을 수 없고 lint도 지켜 주지 못한다.
- **이름 규칙 없이 패키지만 나눔** — 규칙이 적다. 그러나 `TokenStore`·`PresignedUploadDataSource`처럼 같은 역할이
  다른 이름을 갖게 되고, 이름만 보고 로컬·원격·저장소를 가를 수 없다.
  **→ 기각:** 접미사가 곧 역할 표시다.

## 영향

**긍정**

- 타입 이름이 곧 파일명이라 이름 검색 한 번으로 파일이 나온다.
- 파일 경계가 역할 경계라 diff와 리뷰 범위가 좁아진다.
- 모든 파일이 단일 선언이 되어 ktlint 파일명 검사가 빠짐없이 걸린다.

**트레이드오프**

- 파일 수가 늘고, 작은 중첩 DTO도 파일을 하나씩 차지한다.
- 서버 파일 하나와 앱 파일 여럿을 대조하려면 `docs/api/` 계약 문서를 거쳐야 한다.
- 역할 접미사 때문에 이름이 길어진다(`ImageDownloadRemoteDataSourceImpl`).

**위험·방어**

- 파일명 외의 규칙(패키지별 허용 타입, 접미사, 매퍼 위치)은 **자동 검사가 없다.** 지금은 `.claude/rules/`와
  코드 리뷰에만 기댄다. 어긋남이 반복되면 Konsist 같은 아키텍처 테스트로 옮긴다.
