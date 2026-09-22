---
id: ADR-0008
title: 로컬 영속화는 DataStore(Preferences) — Room 미채택
status: accepted
date: 2026-06-10
deciders: Parfait 팀
supersedes:
superseded_by:
related_adr:
related_spec:
related_architecture:
platforms: android
tags: [adr, parfait]
---
# ADR-0008: 로컬 영속화는 DataStore(Preferences) — Room 미채택

## 맥락
최근 이미지 URI 등 가벼운 메타데이터를 로컬에 저장해야 한다. 관계형 쿼리나 대량 엔티티가 필요한 요구는 아직 없다.

## 결정
로컬 영속화는 **DataStore(Preferences)** 로 한다(`androidx.datastore`). Room은 도입하지 않는다.

- `DataStoreModule`이 `DataStore<Preferences>` 싱글톤과 JSON 파서(`ignoreUnknownKeys`, `coerceInputValues`, `encodeDefaults`) 제공.
- `RecentImageEditor` 인터페이스가 DataStore 접근을 추상화.
- 이미지 자체는 파일 시스템(내부 저장소), 메타데이터는 DataStore로 이원 관리. `RecentImageRepositoryImpl`이 `RecentImageLocalDataSource`(DataStore)와 `FileRecentImageLocalDataSource`(파일)를 조합, 파일 last-modified 기반으로 캐시 축출.

## 대안
- **Room** — 관계형·쿼리·마이그레이션 강력. 그러나 현재 요구(키-값 메타) 대비 과함.
  **→ 기각:** 스키마·마이그레이션 비용 불필요.
- **SharedPreferences** — 가장 단순. 그러나 동기 API·flow 미지원.
  **→ 기각:** DataStore가 coroutine/flow 친화적 후속.

## 영향

**긍정**
- 가벼운 메타에 딱 맞는 비동기·flow 기반 저장. 스키마 관리 부담 없음.

**트레이드오프**
- 관계형 쿼리·조인이 필요해지면 재검토 필요(그때 새 ADR로 Room 도입 결정).

**위험·방어**
- 파일과 DataStore 상태 불일치 방지를 Repository 한 곳에서 조율([[data-layer]]).
