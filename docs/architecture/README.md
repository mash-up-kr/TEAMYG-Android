# Architecture

Parfait 프로젝트의 구조·설계 명세 문서를 모읍니다. (모듈 구조, 상태관리 패턴, DI, 데이터 흐름, UI 흐름 등)

> **참고** — 모든 주장은 **파일명 + 심볼명(클래스/함수/프로퍼티)** 으로 근거를 표시하고, 추정은 **Assumption** 라벨을 붙입니다.
>
> **⚠ 라인번호와 수치(파일 수·진행률·사용 횟수)는 적지 않습니다** — 커밋마다 바뀌어 금방 거짓이 됩니다. 자세한 규칙은 [`../adr/README.md`](../adr/README.md) 참조.
>
> `architecture/`는 "어떻게/어디"(구현 가이드)를, `adr/`는 "왜"(결정·대안)를 다룹니다. 상호 보완입니다.

| 문서 | 내용 |
|------|------|
| [module-structure.md](module-structure.md) | 전체 모듈 목적·의존 방향·레이어 그룹. 관련 ADR-0001·0002·0003 |
| [state-management.md](state-management.md) | MVI 단방향 흐름·3분할 계약·신규 화면 체크리스트·안티패턴. 관련 ADR-0005·0009 |
| [navigation-flow.md](navigation-flow.md) | Navigation3·Navigator·엔트리 빌더·신규 목적지 등록 체크리스트. 관련 ADR-0006·0002 |
| [data-layer.md](data-layer.md) | Repository·DataSource·DI 모듈·로컬 영속화·신규 데이터 체크리스트. 관련 ADR-0001·0004·0008 |
| [design-system.md](design-system.md) | 테마 홀더(`YGTheme.*`)·토큰 계층·컴포넌트 작성 규약(YGButton 레퍼런스). 관련 ADR-0010·0007 |

## Frontmatter (필수)
모든 architecture 문서는 YAML frontmatter를 단다(형식 권위: [`template.md`](template.md)). 필드: `id` · `title` · `category`(=architecture) · `status`(**living / superseded / deprecated**) · `platforms`(=android) · `verified`(코드 대조일) · `related_spec` · `related_adr` · `related_architecture` · `related_code`(심볼명, 라인번호 금지) · `tags`.
