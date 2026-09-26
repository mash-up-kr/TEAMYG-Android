---
paths:
  - "data/src/main/**/*.kt"
  - "domain/src/main/**/*.kt"
---

# 파일당 top-level 타입 하나

- 파일 하나에 private 이 아닌 top-level `class`·`interface`·`object` 를 하나만 둔다. 파일 이름은 그 선언 이름과 맞춘다.
- 예외: 중첩 선언(sealed 하위 타입, companion object), 그 파일 안에서만 쓰는 `private` 헬퍼 클래스.
- 매핑 확장 함수는 모델 파일에 두지 않고 `data/model/mapper/<분류>/<타입>Mapper.kt` 로 분리한다. 예: `RecentImageKindEntity` 는 `model/entity/RecentImageKindEntity.kt`, `toVO()`·`toEntity()` 는 `model/mapper/entity/RecentImageKindEntityMapper.kt`.
- 응답·요청 DTO 도 마찬가지다. 중첩 응답 타입을 부모 응답 파일에 같이 넣지 않는다.
