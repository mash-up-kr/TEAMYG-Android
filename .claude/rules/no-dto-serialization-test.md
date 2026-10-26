---
paths:
  - "data/src/test/**/*.kt"
  - "data/src/main/**/service/model/**/*.kt"
---

# DTO 직렬화·매핑 테스트를 만들지 않는다

- `data/src/test/**/service/model/` 아래에 테스트를 두지 않는다. 요청·응답 DTO(`*Request`·`*Response`)를
  `Json`으로 인코딩·디코딩해 JSON 키, `@SerialName`, 기본값, null 처리를 단언하는 테스트는 만들지 않는다.
- 이유: 단언하는 키를 `@SerialName`을 쓴 사람이 같은 가정으로 적는다. 서버 키와 어긋나면 코드와 테스트가
  함께 틀리고 테스트는 초록이다. 서버와의 대조는 `sync-server-api-baseline` 감사가 맡는다.
- 응답 → VO 매핑에 판단이 든 경우(enum 폴백, nullable 접기, 단위 변환)는 매퍼 단독 테스트가 아니라
  `XxxRemoteDataSourceImplTest`의 케이스로 잠근다(`docs/architecture/data-layer.md` 매퍼 테스트 규약).
