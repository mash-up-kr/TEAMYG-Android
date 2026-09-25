---
paths:
  - "data/src/main/**/source/**/*.kt"
---

# source 패키지 규칙

- `source/<도메인>/local` 에는 이름이 `LocalDataSource`(인터페이스)·`LocalDataSourceImpl`(구현)로 끝나는 타입만 둔다.
- `source/<도메인>/remote` 에는 이름이 `RemoteDataSource`·`RemoteDataSourceImpl` 로 끝나는 타입만 둔다.
- 서버 응답(`*Response`)을 VO 로 바꾸는 매핑은 DataSourceImpl 안에 쓰지 않고 `source/<도메인>/mapper/` 의
  매퍼 파일로 분리한다. 여러 도메인이 함께 쓰는 매핑은 `source/common/mapper/` 에 둔다.
- 저장소 모델(Entity)과 VO 사이 매핑은 이 규칙이 아니라 `data/model/mapper/` 규칙을 따른다.
