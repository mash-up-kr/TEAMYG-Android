---
id: ADR-NNNN
title: [Decision Title]
status: proposed             # proposed | accepted | superseded | deprecated
date: YYYY-MM-DD
deciders: Parfait 팀          # 팀/역할 (실명·개인정보 금지 — public repo)
supersedes:                   # 이 ADR이 대체하는 ADR-NNNN (없으면 비움)
superseded_by:                # 이 ADR을 대체한 ADR-NNNN (없으면 비움)
related_adr:
related_spec:
related_architecture:
platforms: android            # 이 repo는 TJYG-Android(Kotlin/Compose) 전용
tags: [adr, parfait]
---

# ADR-NNNN: 결정 제목

> 상태·날짜·결정자·대체 관계는 위 frontmatter가 단일 출처. 본문은 결정 내용에 집중.

## 맥락
무엇이 문제이고 왜 이 결정이 필요한가. (배경이 자명하면 생략 가능)

## 결정
무엇을 바꾸는가 / 했는가. 핵심을 먼저 한 문장, 세부는 불릿으로. (택한 근거·받아들인 트레이드오프는 결정 본문과 아래 "대안"에서 함께 다룬다.)

## 대안
- **대안 A** — 장점 산문. 그러나 단점 산문.
  **→ 기각:** 기각 사유.
- **대안 B** — 장점 산문. 그러나 단점 산문.
  **→ 기각:** 기각 사유.

## 영향

**긍정**

- …

**트레이드오프**

- …

**위험·방어**

- 어떻게 검증/완화하는가 (테스트·가드 등).

---
작성 규칙
- 본문은 **현재 결정의 최종상태**만 담는다. 번복·보강 시 본문을 갱신하고 "이전엔/번복" 식 이력은 누적하지 않는다. 한 결정이 다른 ADR을 대체하면 본문을 병합하고, frontmatter에 구 문서 `status: superseded` + `superseded_by`, 신 문서 `supersedes` 지정.
- 근거는 **파일명 + 심볼명**으로. 라인번호(`파일.kt:NN`)·변동 수치(파일 수·진행률·사용 횟수) 금지 — 자세한 규칙은 `README.md`.
- 파일명 `NNNN-kebab-case-title.md`, README 인덱스 테이블에 같은 커밋으로 한 줄 등록.
