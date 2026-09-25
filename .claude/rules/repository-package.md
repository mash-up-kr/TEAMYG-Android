---
paths:
  - "domain/src/main/**/repository/**/*.kt"
  - "data/src/main/**/repository/**/*.kt"
---

# repository 패키지에는 Repository 만

- `domain` 의 `repository` 하위에는 이름이 `Repository` 로 끝나는 인터페이스만 둔다.
- `data` 의 `repository` 하위에는 이름이 `RepositoryImpl` 로 끝나는 구현체만 둔다.
- Repository 가 의존하는 다른 인터페이스를 같은 패키지에 두지 않는다. 역할에 맞는 패키지로 보낸다.
  - 플랫폼·외부 SDK 가 값을 대 주는 인터페이스(구현이 `app` 등 바깥 모듈) → `domain/provider`. 예: `DeviceTokenProvider`.
  - 데이터 소스 → `data/source/<도메인>/{local,remote}`.
