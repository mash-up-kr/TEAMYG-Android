# parfait 구현 문서 이관 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `team-yg-pesonal-agent` 저장소의 `parfait/android`·`parfait/api`·`parfait/script`를 이 저장소의 `docs/` 아래로 복사하고, 상대 링크 3,200건 이상을 새 경로로 재작성한다.

**Architecture:** 복사는 `shutil`, 링크 재작성은 일회성 파이썬 도구가 한다. 도구는 링크를 텍스트로 치환하지 않는다 — 원본 위치 기준으로 target을 repo-relative 절대 경로로 resolve한 뒤, `PATH_MAP`으로 목적지 경로를 찾고, 새 파일 위치 기준 상대 경로를 다시 계산한다. 깊이 변화가 디렉토리마다 다르기 때문에 문자열 치환으로는 맞출 수 없다. 검증은 `check_links.py`가 exit 0을 내는지로 한다.

**Tech Stack:** Python 3 stdlib 전용 (`pathlib`·`re`·`shutil`·`os.path.relpath`·`unittest`). 빌드 도구 변경 없음.

**Spec:** [`docs/superpowers/specs/2026-09-22-parfait-docs-migration-design.md`](../specs/2026-09-22-parfait-docs-migration-design.md)

## Global Constraints

- **원본 저장소는 읽기 전용이다.** `/Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent` 아래 파일을 생성·수정·삭제하지 않는다. 그 저장소에서 `git` 쓰기 명령을 실행하지 않는다.
- 작업 저장소는 `/Users/jeonheehoon/Documents/work_station/mashup/github/TJYG-Android`, 브랜치는 `feature/#525-ai-docs-set-up`.
- 파이썬 스크립트는 **stdlib 전용**. pip 의존성을 추가하지 않는다.
- 스크립트의 `REPO_ROOT`는 `Path(__file__).resolve().parents[2]` 규약을 유지한다. `docs/script/` 배치에서 이 값이 저장소 루트로 맞는다.
- 문서 본문은 **한국어**, 코드·식별자·파일명·커밋 메시지는 **영어** (저장소 `CLAUDE.md`).
- Gradle 빌드·CI 설정 파일을 건드리지 않는다. 이관은 파일 추가일 뿐이다.
- 커밋 메시지 끝에 `Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>`를 붙인다.
- 일회성 도구를 두는 `<scratchpad>`는 `/private/tmp/claude-501/-Users-jeonheehoon-Documents-work-station-mashup-github-TJYG-Android/8cea55e0-e639-4adf-a604-a42f683d3a53/scratchpad` 다. 세션이 바뀌면 그 세션의 스크래치패드를 쓴다.

## Review Focus

스펙이 요구하지만 어떤 태스크의 테스트도 직접 건드리지 않는 것들. 각 줄의 검사는 지정된 태스크 안에 스텝으로 들어가 있다.

1. **코드 펜스 안의 예시 링크가 재작성되면 문서가 거짓이 된다** — 펜스 안 `](...)`는 링크가 아니라 예시다. Task 2에서 펜스 제외를 테스트한다.
2. **앵커가 유실되면 링크가 문서 첫 줄로 간다** — `](../specs/X.md#절이름)`의 `#절이름`이 보존돼야 한다. Task 2에서 테스트한다.
3. **매핑되지 않은 target이 조용히 통과하면 깨진 링크가 남는다** — `PATH_MAP`에도 `UNLINK_PREFIXES`에도 없는 target은 예외를 던져야 한다. Task 2에서 테스트한다.
4. **파일명의 유니코드 정규화 차이로 복사본이 원본과 달라질 수 있다** — 한글 파일명이 많다(`누끼-따기.md` 등). Task 3에서 파일 수 대조로 잡는다.
5. **`search.py`가 frontmatter 없는 문서에서 죽으면 검색이 통째로 실패한다** — `README.md`·`template.md`에는 frontmatter가 없다. Task 6에서 테스트한다.

---

## File Structure

| 파일 | 책임 |
|---|---|
| `docs/script/check_links.py` | 상대 링크 resolve 전수 검사. 이관의 게이트 |
| `docs/script/search.py` | 문서 자연어 검색. 수집기를 문서용으로 개작 |
| `docs/script/test_check_links.py` · `test_search.py` | 위 둘의 단위 테스트 |
| `docs/script/_script-template.py` · `README.md` · `.gitignore` | 스크립트 규약 |
| `<scratchpad>/migrate_links.py` | 일회성 이관 도구. 저장소에 커밋하지 않는다 |
| `<scratchpad>/test_migrate_links.py` | 위 도구의 테스트 |
| `docs/{superpowers/specs,superpowers/plans,adr,architecture,synthesis,api}/` | 이관된 문서 |
| `docs/code-conventions.md` | KDoc 규약 (원본 `parfait/android/CLAUDE.md`) |
| `docs/index.md` | 진입 허브 (원본 `parfait/index.md`, pm 절 제거) |
| `docs/doc-baseline.md` | 문서 베이스라인 |
| `CLAUDE.md` | 루트. `docs/code-conventions.md` 링크 한 줄 추가 |

`<scratchpad>`는 이 세션의 스크래치패드 디렉토리다. 일회성 도구는 저장소에 남기지 않는다 — 한 번 돌고 나면 쓸 일이 없고, 원본 저장소 절대 경로를 하드코딩하고 있다.

---

## Task 1: `docs/script/` 개설과 `check_links.py` 도킹

링크 재작성의 게이트를 먼저 세운다. 이게 없으면 Task 3의 성공 여부를 판정할 수 없다.

**Files:**
- Create: `docs/script/check_links.py`
- Create: `docs/script/test_check_links.py`
- Create: `docs/script/_script-template.py`
- Create: `docs/script/README.md`
- Create: `docs/script/.gitignore`

**Interfaces:**
- Consumes: 없음 (첫 태스크)
- Produces: `python3 docs/script/check_links.py <경로...>` — 깨진 링크가 있으면 stdout에 `파일:줄: 깨진 링크 '...' → /절대/경로` 를 출력하고 exit 1, 없으면 exit 0. 이후 모든 태스크가 이 명령을 게이트로 쓴다.

- [ ] **Step 1: 원본 스크립트를 복사한다**

```bash
SRC=/Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent/parfait/script
mkdir -p docs/script
cp "$SRC/check_links.py" "$SRC/test_check_links.py" "$SRC/_script-template.py" \
   "$SRC/README.md" "$SRC/.gitignore" docs/script/
```

`oq_sync.py`·`vendor.py`·`test_oq_sync.py`·`test_vendor.py`·`SKILL.template.md`·`search.py`는 복사하지 않는다. `search.py`는 Task 6이 개작본으로 만든다.

- [ ] **Step 2: 복사된 파일 목록을 확인한다**

```bash
ls docs/script/
```

Expected: `.gitignore  README.md  _script-template.py  check_links.py  test_check_links.py` 다섯 개. `__pycache__`가 딸려 왔으면 지운다 (`rm -rf docs/script/__pycache__`).

- [ ] **Step 3: 테스트를 돌려 통과를 확인한다**

```bash
cd docs/script && python3 -m unittest test_check_links -v; cd -
```

Expected: PASS. `REPO_ROOT = parents[2]`가 `docs/script/check_links.py` 기준으로도 저장소 루트라서 수정 없이 돈다.

- [ ] **Step 4: docstring의 용법 경로를 고친다**

`docs/script/check_links.py` 상단 docstring에서 `parfait/script/check_links.py`를 `docs/script/check_links.py`로 바꾼다. 세 줄이다.

```python
용법:
    python3 docs/script/check_links.py                 # repo 전체
    python3 docs/script/check_links.py docs            # 하위 경로만
    python3 docs/script/check_links.py --quiet         # 깨진 것만 출력
```

같은 docstring의 마지막 문단에 있는 `wiki/script/lint.py` 언급은 이 저장소에도 그 파일이 있으므로 그대로 둔다.

- [ ] **Step 5: `docs/script/README.md`에서 빠진 스크립트 절을 지운다**

`oq_sync`·`vendor`·`search`를 설명하는 절을 삭제한다. `search`는 Task 6에서 개작본 설명으로 되살린다. 남는 것은 `check_links`와 스크립트 작성 규약뿐이다. 용법 예시의 `parfait/script/` 경로도 `docs/script/`로 고친다.

- [ ] **Step 6: 현재 저장소에서 게이트가 초록인지 확인한다**

```bash
python3 docs/script/check_links.py docs
```

Expected: exit 0. 이 시점의 `docs/`에는 이관분이 아직 없고 기존 문서만 있다. 여기서 이미 깨진 링크가 나오면 **이관과 무관한 기존 문제**다 — 보고하고, 고치지 말고 다음 스텝으로 간다(범위 밖).

- [ ] **Step 7: 커밋**

```bash
git add docs/script
git commit -m "chore(docs): dock the link checker under docs/script

Copy check_links.py and its test from the parfait docs repo so the
migration has a gate before any document moves. REPO_ROOT stays
parents[2] because docs/script sits at the same depth as the original
parfait/script.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 링크 재작성 도구

복사보다 먼저 만든다. 도구가 없으면 복사본은 깨진 링크 3,200건짜리 트리다.

**Files:**
- Create: `<scratchpad>/migrate_links.py`
- Create: `<scratchpad>/test_migrate_links.py`

**Interfaces:**
- Consumes: Task 1의 `docs/script/check_links.py` (게이트로만 쓴다)
- Produces:
  - `PATH_MAP: dict[str, str]` — 원본 repo-relative 경로 → 목적지 repo-relative 경로
  - `UNLINK_PREFIXES: tuple[str, ...]` — 대응 대상이 없어 링크를 해제할 target 접두사
  - `map_target(old_target: str) -> str | None` — 매핑된 목적지 경로. `UNLINK_PREFIXES`에 걸리면 `None`. 어디에도 없으면 `KeyError`
  - `rewrite(text: str, old_file: str, new_file: str) -> tuple[str, int]` — 재작성된 본문과 링크 해제 건수. `old_file`·`new_file`은 각 저장소 루트 기준 상대 경로 문자열

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`<scratchpad>/test_migrate_links.py`:

```python
import unittest

import migrate_links as m


class MapTargetTest(unittest.TestCase):
    def test_maps_spec_dir(self):
        self.assertEqual(
            m.map_target("parfait/android/specs/2026-08-28-login-debug-mode.md"),
            "docs/superpowers/specs/2026-08-28-login-debug-mode.md",
        )

    def test_maps_adr_dir(self):
        self.assertEqual(
            m.map_target("parfait/android/adr/0031-analytics-central-screen-mapping.md"),
            "docs/adr/0031-analytics-central-screen-mapping.md",
        )

    def test_maps_renamed_file(self):
        self.assertEqual(m.map_target("parfait/android/CLAUDE.md"), "docs/code-conventions.md")

    def test_maps_scope_map_onto_index(self):
        self.assertEqual(m.map_target("parfait/CLAUDE.md"), "docs/index.md")

    def test_maps_wiki_open_questions(self):
        self.assertEqual(
            m.map_target("wiki/synthesis/open-questions.md"), "wiki/open-questions.md"
        )

    def test_unlinks_wiki_concept(self):
        self.assertIsNone(m.map_target("wiki/concepts/누끼-따기.md"))

    def test_unknown_target_raises(self):
        with self.assertRaises(KeyError):
            m.map_target("parfait/pm/README.md")


class RewriteTest(unittest.TestCase):
    def test_depth_decrease(self):
        # adr 은 3단계에서 2단계로 내려간다. 같은 디렉토리 형제는 그대로.
        text = "[ADR-0004](0004-hilt-ksp-di.md)"
        out, unlinked = m.rewrite(
            text, "parfait/android/adr/0031-x.md", "docs/adr/0031-x.md"
        )
        self.assertEqual(out, "[ADR-0004](0004-hilt-ksp-di.md)")
        self.assertEqual(unlinked, 0)

    def test_cross_directory_gets_new_middle_segment(self):
        # adr → spec: 원본 ../specs/, 목적지 ../superpowers/specs/
        text = "[스펙](../specs/2026-08-28-login-debug-mode.md)"
        out, _ = m.rewrite(text, "parfait/android/adr/0031-x.md", "docs/adr/0031-x.md")
        self.assertEqual(
            out, "[스펙](../superpowers/specs/2026-08-28-login-debug-mode.md)"
        )

    def test_api_to_spec(self):
        # api 는 깊이가 그대로지만 중간 경로가 바뀐다
        text = "[스펙](../android/specs/2026-08-28-login-debug-mode.md)"
        out, _ = m.rewrite(text, "parfait/api/auth.md", "docs/api/auth.md")
        self.assertEqual(
            out, "[스펙](../superpowers/specs/2026-08-28-login-debug-mode.md)"
        )

    def test_preserves_anchor(self):
        text = "[타임존](../../api/parfait-group.md#타임존)"
        out, _ = m.rewrite(
            text,
            "parfait/android/specs/archive/2026-08-01-x.md",
            "docs/superpowers/specs/archive/2026-08-01-x.md",
        )
        self.assertEqual(out, "[타임존](../../api/parfait-group.md#타임존)")

    def test_unlinks_missing_wiki_concept(self):
        text = "정책은 [누끼 따기](../../wiki/concepts/누끼-따기.md)를 따른다."
        out, unlinked = m.rewrite(
            text, "parfait/android/specs/README.md", "docs/superpowers/specs/README.md"
        )
        self.assertEqual(out, "정책은 누끼 따기를 따른다.")
        self.assertEqual(unlinked, 1)

    def test_skips_code_fence(self):
        text = "```\n[예시](../specs/X.md)\n```\n"
        out, _ = m.rewrite(text, "parfait/android/adr/0031-x.md", "docs/adr/0031-x.md")
        self.assertEqual(out, text)

    def test_leaves_external_and_anchor_only(self):
        text = "[깃허브](https://github.com/x) 와 [절](#어떤-절)"
        out, _ = m.rewrite(text, "parfait/api/auth.md", "docs/api/auth.md")
        self.assertEqual(out, text)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd <scratchpad> && python3 -m unittest test_migrate_links -v
```

Expected: FAIL — `ModuleNotFoundError: No module named 'migrate_links'`

- [ ] **Step 3: 도구를 구현한다**

`<scratchpad>/migrate_links.py`:

```python
#!/usr/bin/env python3
"""parfait 문서를 TJYG-Android 의 docs/ 로 복사하며 상대 링크를 재작성한다.

일회성 도구다. 원본 저장소 절대 경로를 하드코딩하므로 저장소에 커밋하지 않는다.
원본 저장소는 읽기만 한다.
"""
import os
import posixpath
import re
import shutil
import sys
from pathlib import Path

SRC_REPO = Path(
    "/Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent"
)
DST_REPO = Path("/Users/jeonheehoon/Documents/work_station/mashup/github/TJYG-Android")

# 원본 repo-relative 경로(디렉토리 또는 파일) → 목적지 repo-relative 경로.
# 긴 접두사가 먼저 매칭돼야 하므로 조회할 때 길이 역순으로 훑는다.
PATH_MAP = {
    "parfait/android/specs": "docs/superpowers/specs",
    "parfait/android/plans": "docs/superpowers/plans",
    "parfait/android/adr": "docs/adr",
    "parfait/android/architecture": "docs/architecture",
    "parfait/android/synthesis": "docs/synthesis",
    "parfait/android/doc-baseline.md": "docs/doc-baseline.md",
    "parfait/android/CLAUDE.md": "docs/code-conventions.md",
    "parfait/api": "docs/api",
    "parfait/index.md": "docs/index.md",
    # 범위 지도 역할은 docs/index.md 가 이어받는다
    "parfait/CLAUDE.md": "docs/index.md",
    "wiki/synthesis/open-questions.md": "wiki/open-questions.md",
    "CLAUDE.md": "CLAUDE.md",
}

# 이 저장소에 대응 페이지가 없는 target. 링크를 해제하고 텍스트만 남긴다.
UNLINK_PREFIXES = ("wiki/concepts/",)

EXTERNAL = ("http://", "https://", "mailto:", "tel:", "data:")

FENCE = re.compile(r"```.*?```", re.S)
# [텍스트](경로) 전체를 잡는다. 텍스트 안의 중첩 대괄호는 다루지 않는다.
LINK = re.compile(r"\[([^\]]*)\]\(\s*([^)\s]+?)\s*(?:\s+\"[^\"]*\")?\)")


def map_target(old_target: str) -> str | None:
    """원본 repo-relative target → 목적지 repo-relative target.

    UNLINK_PREFIXES 에 걸리면 None. 어디에도 없으면 KeyError.
    """
    for prefix in UNLINK_PREFIXES:
        if old_target.startswith(prefix):
            return None
    for src in sorted(PATH_MAP, key=len, reverse=True):
        if old_target == src:
            return PATH_MAP[src]
        if old_target.startswith(src + "/"):
            return PATH_MAP[src] + old_target[len(src):]
    raise KeyError(old_target)


def _is_relative(target: str) -> bool:
    if not target or target.startswith(("#", "/")):
        return False
    if target.startswith(EXTERNAL):
        return False
    if target.startswith("<") or "{" in target or target.startswith("[["):
        return False
    return True


def rewrite(text: str, old_file: str, new_file: str) -> tuple[str, int]:
    """본문의 상대 링크를 재작성한다. (새 본문, 링크 해제 건수)를 돌려준다."""
    old_dir = posixpath.dirname(old_file)
    new_dir = posixpath.dirname(new_file)
    unlinked = 0

    def fix_segment(segment: str) -> str:
        nonlocal unlinked

        def one(match: re.Match) -> str:
            label, target = match.group(1), match.group(2)
            if not _is_relative(target):
                return match.group(0)
            bare, _, anchor = target.partition("#")
            if not bare:
                return match.group(0)
            old_target = posixpath.normpath(posixpath.join(old_dir, bare))
            new_target = map_target(old_target)
            if new_target is None:
                unlinked += 1
                return label
            rel = posixpath.relpath(new_target, new_dir or ".")
            return f"[{label}]({rel}#{anchor})" if anchor else f"[{label}]({rel})"

        return LINK.sub(one, segment)

    # 코드 펜스 바깥만 손댄다. 펜스 안의 `](...)` 는 예시이지 링크가 아니다.
    out, last = [], 0
    for m in FENCE.finditer(text):
        out.append(fix_segment(text[last:m.start()]))
        out.append(m.group(0))
        last = m.end()
    out.append(fix_segment(text[last:]))
    return "".join(out), unlinked


# --- 복사 -------------------------------------------------------------------

COPY_TREES = [
    ("parfait/android/specs", "docs/superpowers/specs"),
    ("parfait/android/plans", "docs/superpowers/plans"),
    ("parfait/android/adr", "docs/adr"),
    ("parfait/android/architecture", "docs/architecture"),
    ("parfait/android/synthesis", "docs/synthesis"),
    ("parfait/api", "docs/api"),
]
COPY_FILES = [
    ("parfait/android/doc-baseline.md", "docs/doc-baseline.md"),
    ("parfait/android/CLAUDE.md", "docs/code-conventions.md"),
    ("parfait/index.md", "docs/index.md"),
]


def copy_all() -> list[tuple[str, str]]:
    """(원본 repo-relative, 목적지 repo-relative) 쌍 목록을 돌려준다."""
    pairs = []
    for src_rel, dst_rel in COPY_TREES:
        src, dst = SRC_REPO / src_rel, DST_REPO / dst_rel
        shutil.copytree(
            src, dst, dirs_exist_ok=True,
            ignore=shutil.ignore_patterns("__pycache__", ".DS_Store"),
        )
        for p in sorted(dst.rglob("*.md")):
            rel = p.relative_to(DST_REPO).as_posix()
            pairs.append((src_rel + rel[len(dst_rel):], rel))
    for src_rel, dst_rel in COPY_FILES:
        shutil.copy2(SRC_REPO / src_rel, DST_REPO / dst_rel)
        pairs.append((src_rel, dst_rel))
    return pairs


def main() -> int:
    pairs = copy_all()
    total_unlinked = 0
    for old_rel, new_rel in pairs:
        path = DST_REPO / new_rel
        text = path.read_text(encoding="utf-8")
        new_text, unlinked = rewrite(text, old_rel, new_rel)
        total_unlinked += unlinked
        if new_text != text:
            path.write_text(new_text, encoding="utf-8")
    print(f"파일 {len(pairs)}개 재작성 · 링크 해제 {total_unlinked}건")
    return 0


if __name__ == "__main__":
    sys.exit(main())
```

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd <scratchpad> && python3 -m unittest test_migrate_links -v
```

Expected: PASS, 14개 테스트 전부.

`test_unknown_target_raises`가 실패하면 `PATH_MAP`에 `parfait/pm`이 잘못 들어간 것이다. `test_skips_code_fence`가 실패하면 `FENCE` 분할이 깨진 것이다.

- [ ] **Step 5: 커밋 없음**

스크래치패드 파일이라 저장소에 들어가지 않는다. 다음 태스크로 간다.

---

## Task 3: 문서 트리 복사와 링크 재작성

**Files:**
- Create: `docs/superpowers/specs/` (110 md), `docs/superpowers/plans/` (81 md), `docs/adr/` (34 md), `docs/architecture/` (7 md), `docs/synthesis/` (3 md), `docs/api/` (17 md)
- Create: `docs/doc-baseline.md`, `docs/code-conventions.md`, `docs/index.md`

**Interfaces:**
- Consumes: Task 2의 `migrate_links.main()`, Task 1의 `check_links.py`
- Produces: 이관된 문서 트리. 이후 태스크가 이 경로를 수정한다.

- [ ] **Step 1: 이관 전 상태를 기록한다**

```bash
git -C /Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent rev-parse --short HEAD
```

이 SHA를 Task 5에서 `docs/index.md` 머리에 적는다. **출력값을 적어 둔다.**

- [ ] **Step 2: 도구를 실행한다**

```bash
cd <scratchpad> && python3 migrate_links.py; cd -
```

Expected: `파일 255개 재작성 · 링크 해제 7건`

255의 내역: `COPY_TREES` 252(specs 110 · plans 81 · adr 34 · architecture 7 · synthesis 3 · api 17) + `COPY_FILES` 3(`doc-baseline.md` · `code-conventions.md` · `index.md`).

파일 수가 255가 아니면 복사가 누락된 것이다. 해제 건수가 7이 아니면 `UNLINK_PREFIXES` 판정과 실제가 어긋난 것이다 — 둘 다 멈추고 원인을 본다. `KeyError`가 나면 `PATH_MAP`에 없는 target이 있다는 뜻이고, 예외 메시지가 그 경로를 알려준다.

- [ ] **Step 3: 파일 수를 원본과 대조한다**

`docs/superpowers/`에는 이관분이 아닌 문서가 이미 4개 있다 — `2026-09-17-wiki-port` 스펙·계획과 이 이관의 스펙·계획이다. 집계에서 뺀다.

```bash
SRC=/Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent
find "$SRC/parfait/android" "$SRC/parfait/api" -name '*.md' | wc -l
find docs/superpowers/specs docs/superpowers/plans docs/adr docs/architecture \
     docs/synthesis docs/api -name '*.md' \
  | grep -v '2026-09-17-wiki-port' | grep -v '2026-09-22-parfait-docs-migration' | wc -l
```

Expected: 첫 명령 254, 둘째 명령 252. 차이 2는 `doc-baseline.md`와 `CLAUDE.md`(→`code-conventions.md`)가 `docs/` 바로 아래로 갔기 때문이다. 이 둘의 존재를 따로 확인한다:

```bash
ls docs/doc-baseline.md docs/code-conventions.md docs/index.md
```

- [ ] **Step 4: 원본에서 이미 깨져 있던 링크 11건을 고친다**

이 11건은 이관이 만든 것이 아니다. 원본 저장소에서도 깨져 있다 — 두 문서가 다른 디렉토리에서 작성된 뒤 옮겨지면서 상대 깊이가 따라가지 않았다. 원본은 읽기 전용이므로 사본에서만 고친다.

`docs/superpowers/plans/2026-08-05-orchestration-session-pipeline.md` — 스펙을 `parfait/specs/`로 가리킨다. 같은 계층의 `specs/`가 맞다.

```bash
sed -i '' 's|](parfait/specs/2026-08-05-orchestration-session-pipeline.md)|](../specs/2026-08-05-orchestration-session-pipeline.md)|g' \
  docs/superpowers/plans/2026-08-05-orchestration-session-pipeline.md
```

`docs/superpowers/plans/archive/2026-08-01-parfait-api-contract-docs.md` — API 계약 문서를 같은 디렉토리 형제(`conventions.md`)나 `api/` 하위로 가리킨다. `docs/superpowers/plans/archive/`에서 `docs/api/`까지는 `../../../api/`다.

```bash
F=docs/superpowers/plans/archive/2026-08-01-parfait-api-contract-docs.md
sed -i '' -E 's|\]\(api/([a-z-]+\.md)\)|](../../../api/\1)|g' "$F"
sed -i '' -E 's|\]\((conventions|auth|parfait|parfait-group|server-baseline|template|README)\.md\)|](../../../api/\1.md)|g' "$F"
```

- [ ] **Step 5: 고친 링크가 실제로 resolve 되는지 본다**

```bash
python3 docs/script/check_links.py docs/superpowers/plans
```

Expected: exit 0. 여기서 남는 것이 있으면 `sed` 패턴이 일부를 놓친 것이다 — 출력의 target을 보고 패턴을 넓힌다.

- [ ] **Step 6: 링크 게이트를 돌린다**

```bash
python3 docs/script/check_links.py docs
```

Expected: exit 0, `깨진 링크 0건`.

깨진 링크가 나오면 출력이 파일·줄·target을 준다. `PATH_MAP`에 빠진 경로인지 확인하고, 도구를 고친 뒤 **복사본을 지우고 Step 2부터 다시 돌린다**(`git clean -fd docs/` 후 재실행). 손으로 개별 링크를 고치지 않는다 — 3,200건 중 하나를 손으로 고치면 나머지도 손으로 고치게 된다.

- [ ] **Step 7: 링크 해제된 7곳을 미결로 등록한다**

`docs/synthesis/open-questions.md` 끝에 항목을 추가한다. 이 파일의 기존 항목 형식을 먼저 읽고 그 형식에 맞춘다.

내용: 이 저장소 위키에 아직 없는 개념 페이지 3건(`누끼-따기` 5곳, `토핑` 1곳, `캘린더-컴포넌트` 1곳) 때문에 링크를 해제했고, 같은 `raw/`에서 추가 ingest가 끝나면 링크를 되살린다는 것. 해당 문서 경로를 함께 적는다:

- `docs/superpowers/specs/README.md`
- `docs/superpowers/specs/archive/2026-08-23-c103-multi-subject-selection.md` (3곳)
- `docs/superpowers/specs/archive/2026-08-01-designsystem-bar-listdate-components.md`
- `docs/superpowers/specs/archive/2026-09-11-g001-group-list-topping-border.md`
- `docs/architecture/design-system.md`

- [ ] **Step 8: 게이트를 다시 돌린다**

```bash
python3 docs/script/check_links.py docs
```

Expected: exit 0. Step 7에서 새 링크를 넣었다면 그것도 검사된다.

- [ ] **Step 9: 커밋**

```bash
git add docs/
git commit -m "docs: migrate the parfait implementation docs into this repo

Copy specs, plans, ADRs, architecture, synthesis and the API contract
docs from the parfait docs repo, rewriting every relative link for the
new layout. Seven links to wiki concept pages that do not exist here
are unlinked and tracked in docs/synthesis/open-questions.md. Eleven
links that were already broken in the source repo are repaired here,
since the copy has to pass the link gate.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: KDoc 규약을 루트에서 링크한다

**Files:**
- Modify: `docs/code-conventions.md`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: Task 3이 만든 `docs/code-conventions.md`
- Produces: 루트 `CLAUDE.md`에서 도달 가능한 코드 규약. 구현 서브에이전트가 이 경로를 읽는다.

- [ ] **Step 1: 제목과 첫 문단을 이 저장소 기준으로 다시 쓴다**

원본은 이렇게 시작한다(경로는 Task 3의 재작성을 이미 거친 상태다):

```markdown
# parfait/android 범위 규약

TJYG-Android 구현 작업에 적용되는 규약. 이 디렉토리(`adr`·`architecture`·`specs`·`plans`·
`synthesis`·`doc-baseline.md`)가 적용 범위다. 플랫폼 공용인 `parfait/api/`·`parfait/pm/`에는
적용하지 않는다 — 그쪽 경계는 [`parfait/CLAUDE.md`](index.md)가 적는다.

작업 유형 라우팅·문서 위치는 저장소 루트 [`CLAUDE.md`](../CLAUDE.md)가 정본이고
여기 중복하지 않는다.
```

이것으로 바꾼다:

```markdown
# 코드 주석·KDoc 규약

이 저장소의 Kotlin 코드에 적용되는 규약이다. 문서 트리의 범위 지도는
[`docs/index.md`](index.md)가 적고, 작업 유형 라우팅은 저장소 루트
[`CLAUDE.md`](../CLAUDE.md)가 정본이라 여기 중복하지 않는다.
```

플랫폼 축(`parfait/android` vs `parfait/api`·`parfait/pm`)은 이 저장소에 없다. 여기서는 문서가 전부 이 앱 것이다.

- [ ] **Step 2: 「서브에이전트에게 실어 나른다」 절을 줄인다**

문서 끝의 그 절에서 ⚠️ 경고와 근거 설명을 지운다. 근거는 그 절 자신의 문장이다:

> ⚠️ **이 파일은 TJYG-Android에서 일하는 서브에이전트에게 자동으로 닿지 않는다.** CLAUDE.md는 그 디렉토리 파일을 열 때 로드되는데 구현 서브에이전트는 다른 저장소에서 브리프만 읽는다.

이 저장소로 들어왔으므로 더는 참이 아니다. 절 제목과 아래 문장만 남긴다:

```markdown
### 서브에이전트에게 실어 나른다

구현·리뷰를 서브에이전트에 디스패치할 때 이 문서를 브리프에 링크한다.
`writing-plans`로 계획을 쓸 때는 계획의 **Global Constraints**에도 넣는다.
최소한 이 셋은 요지로 실어 나른다:

- 코드가 이미 말하는 것은 쓰지 않는다
- `@return`·`@param`은 타입·이름이 말하지 못할 때만
- 다른 컴포넌트의 현재 상태를 단정하지 않는다(낡는다)
```

- [ ] **Step 3: 루트 `CLAUDE.md`에 절을 추가한다**

`## 위키 (`wiki/`)` 절 **앞**에 넣는다. 본문을 옮기지 않고 링크만 건다 — 루트가 비대해지면 매 세션 비용이 된다.

```markdown
## 문서 (`docs/`)

`docs/`는 이 앱의 **구현 문서**다. 무엇을 어떻게 만들었고 왜 그렇게 했는지를 남긴다.
제품 스펙·기획 자체는 `wiki/`가 적는다 — 의존 방향은 구현 → 위키 단방향이다.

| 경로 | 내용 |
|---|---|
| `docs/index.md` | 진입 허브. 라우팅은 여기서 본다 |
| `docs/superpowers/specs/` | 설계 스펙. 구현 완료분은 `archive/` |
| `docs/superpowers/plans/` | 구현 계획. 구현 완료분은 `archive/` |
| `docs/adr/` | 아키텍처 결정 기록 |
| `docs/architecture/` | 레이어별 구조 문서 |
| `docs/api/` | TEAMYG-SERVER 계약. 서버가 정본이고 Android 매핑은 각 문서의 절로 붙는다 |
| `docs/synthesis/` | 미결 항목·린트 로그 |
| `docs/script/` | `check_links` `search`. 전부 저장소 루트에서 실행 |

**Kotlin 코드 주석·KDoc 규약은 [`docs/code-conventions.md`](docs/code-conventions.md)에
있다.** 구현·리뷰를 서브에이전트에 디스패치할 때 그 문서를 브리프에 링크하고,
`writing-plans`로 계획을 쓸 때는 Global Constraints에도 넣는다.

문서를 옮기거나 `archive/`로 내린 뒤에는
`python3 docs/script/check_links.py docs`로 상대 링크를 전수 확인한다(깨지면 exit 1).
```

- [ ] **Step 4: 링크 게이트를 돌린다**

```bash
python3 docs/script/check_links.py docs CLAUDE.md
```

Expected: exit 0. 새로 넣은 링크 `docs/code-conventions.md`·`docs/index.md`가 실제로 존재하는지 여기서 걸린다.

- [ ] **Step 5: 커밋**

```bash
git add CLAUDE.md docs/code-conventions.md
git commit -m "docs: link the Kotlin comment conventions from the root CLAUDE.md

The conventions file used to warn that it never reaches subagents
working in this repo. It lives here now, so the warning is gone and the
root links to it instead of inlining it.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: `docs/index.md`에서 pm 절을 걷어낸다

**Files:**
- Modify: `docs/index.md`

**Interfaces:**
- Consumes: Task 3이 만든 `docs/index.md`, Task 3 Step 1이 기록한 원본 SHA
- Produces: pm 참조가 없는 진입 허브

- [ ] **Step 1: pm 참조 위치를 찾는다**

```bash
grep -n "pm/" docs/index.md | head -40
grep -c "pm/" docs/index.md
```

`parfait/pm/`은 이관하지 않았으므로 이 참조들은 전부 죽은 링크다.

- [ ] **Step 2: pm 절과 pm 링크를 지운다**

절 전체가 pm이면 절을 지운다. 표의 행이면 행을 지운다. 문장 안의 링크면 문장을 다시 쓴다 — 링크만 떼면 "제품 문서는 …에 있다"가 목적어를 잃는다.

`blog/`·`bot/`·`raw/` 참조도 같은 이유로 죽은 링크다. 같이 처리한다.

- [ ] **Step 3: 이관 기준점을 머리에 적는다**

`docs/index.md` 제목 바로 아래에 넣는다. Task 3 Step 1에서 적어 둔 SHA를 쓴다.

```markdown
> 이 문서 트리는 2026-09-22에 `team-yg-pesonal-agent`(`parfait/`)에서 복사됐다.
> 기준 커밋 `<SHA>`. 원본은 그 저장소에 그대로 남아 있고 자동 동기화는 없다 —
> 이후의 정본은 이쪽이다.
```

- [ ] **Step 4: 게이트를 돌린다**

```bash
python3 docs/script/check_links.py docs
grep -c "pm/\|blog/\|bot/" docs/index.md
```

Expected: 첫 명령 exit 0. 둘째 명령 0.

- [ ] **Step 5: 커밋**

```bash
git add docs/index.md
git commit -m "docs: drop the routes this repo did not receive from the index hub

pm, blog and bot stayed in the source repo, so their rows were dead
links here. Records the source commit the tree was copied from.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 6: `search.py` 수집기를 문서용으로 개작한다

graphify의 스캔 루트는 `wiki/pages/`라서 `docs/` 아래 문서는 검색 수단이 없다. 이 태스크가 그것을 만든다.

**Files:**
- Create: `docs/script/search.py`
- Create: `docs/script/test_search.py`
- Modify: `docs/script/README.md`

**Interfaces:**
- Consumes: Task 3이 만든 `docs/superpowers/specs`·`docs/superpowers/plans`·`docs/adr`·`docs/architecture`
- Produces: `python3 docs/script/search.py "<자연어 쿼리>" [--top N]` — 점수 내림차순으로 `<id>  (score N) [archived] — <title>` 출력

- [ ] **Step 1: 실패하는 테스트를 쓴다**

`docs/script/test_search.py` (원본 테스트를 대체한다):

```python
import tempfile
import unittest
from pathlib import Path

import search


def _mk(path, **fm):
    path.parent.mkdir(parents=True, exist_ok=True)
    body = "\n".join(f"{k}: {v}" for k, v in fm.items())
    path.write_text(f"---\n{body}\n---\n# 본문\n", encoding="utf-8")


class ScoreTest(unittest.TestCase):
    def test_title_weight_beats_tags(self):
        by_title = {"id": "x", "title": "토핑 border 렌더링", "tags": "", "code": "", "headings": ""}
        by_tags = {"id": "y", "title": "무관", "tags": "border", "code": "", "headings": ""}
        self.assertGreater(search.score("border", by_title), search.score("border", by_tags))

    def test_no_match_zero(self):
        doc = {"id": "a", "title": "b", "tags": "", "code": "", "headings": ""}
        self.assertEqual(search.score("xyz", doc), 0.0)

    def test_related_code_is_searchable(self):
        doc = {"id": "a", "title": "b", "tags": "", "code": "LoginViewModel", "headings": ""}
        self.assertGreater(search.score("LoginViewModel", doc), 0.0)


class CollectTest(unittest.TestCase):
    def test_reads_frontmatter_id_and_title(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "2026-08-28-login-debug-mode.md"
            _mk(p, id="login-debug-mode", title="로그인 디버그 모드", tags="[plan, login]")
            doc = search.parse_doc(p, archived=False)
            self.assertEqual(doc["id"], "login-debug-mode")
            self.assertEqual(doc["title"], "로그인 디버그 모드")

    def test_falls_back_to_filename_without_frontmatter(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "README.md"
            p.write_text("# 안내\n본문\n", encoding="utf-8")
            doc = search.parse_doc(p, archived=False)
            self.assertEqual(doc["id"], "README")
            self.assertEqual(doc["title"], "")

    def test_marks_archived(self):
        with tempfile.TemporaryDirectory() as t:
            p = Path(t) / "archive" / "old.md"
            _mk(p, id="old", title="옛 스펙")
            self.assertTrue(search.parse_doc(p, archived=True)["archived"])


class SearchTest(unittest.TestCase):
    def test_ranks_relevant_first(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            _mk(root / "specs" / "a.md", id="topping-border", title="토핑 border 렌더링")
            _mk(root / "specs" / "b.md", id="login-debug", title="로그인 디버그 모드")
            search.DOC_ROOTS = [root / "specs"]
            res = search.search("토핑 border", top=5)
            self.assertEqual(res[0][1]["id"], "topping-border")

    def test_includes_archive(self):
        with tempfile.TemporaryDirectory() as t:
            root = Path(t)
            _mk(root / "specs" / "archive" / "old.md", id="old-spec", title="옛 토핑 스펙")
            search.DOC_ROOTS = [root / "specs"]
            res = search.search("토핑", top=5)
            self.assertEqual(len(res), 1)
            self.assertTrue(res[0][1]["archived"])


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: 테스트가 실패하는지 확인한다**

```bash
cd docs/script && python3 -m unittest test_search -v; cd -
```

Expected: FAIL — `ModuleNotFoundError: No module named 'search'`

- [ ] **Step 3: 구현한다**

`docs/script/search.py`:

```python
#!/usr/bin/env python3
"""자연어 쿼리로 구현 문서를 찾는다 — 스펙·계획·ADR·아키텍처.

용법:
    python3 docs/script/search.py "<자연어 쿼리>" [--top N]

graphify 의 스캔 루트는 `wiki/pages/` 라서 `docs/` 아래 문서는 그 바깥이다.
이 스크립트가 그 구멍을 메운다.

규약: stdlib 전용. repo 루트 = Path(__file__).resolve().parents[2].
"""
import argparse
import re
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

DOC_ROOTS = [
    REPO_ROOT / "docs" / "superpowers" / "specs",
    REPO_ROOT / "docs" / "superpowers" / "plans",
    REPO_ROOT / "docs" / "adr",
    REPO_ROOT / "docs" / "architecture",
]

FRONTMATTER = re.compile(r"^---\n(.*?)\n---", re.S)


def tokenize(s):
    return re.findall(r"[a-z0-9가-힣]+", s.lower())


def _field(block, key):
    m = re.search(rf"^{key}:\s*(.+)$", block, re.M)
    return m.group(1).strip().strip("\"'") if m else ""


def parse_doc(md_path, archived):
    text = md_path.read_text(encoding="utf-8", errors="ignore")
    m = FRONTMATTER.search(text)
    block = m.group(1) if m else ""
    doc_id = _field(block, "id") or md_path.stem
    related = " ".join(
        _field(block, k) for k in ("related_code", "related_spec", "related_adr")
    )
    return {
        "id": doc_id,
        "title": _field(block, "title"),
        "tags": _field(block, "tags"),
        "code": related,
        "headings": " ".join(re.findall(r"^#{1,3}\s+(.+)$", text, re.M)),
        "archived": archived,
        "path": str(md_path),
    }


def score(query, doc):
    q = set(tokenize(query))
    if not q:
        return 0.0
    id_t = tokenize(doc["id"])
    title_t = tokenize(doc["title"])
    tag_t = tokenize(doc["tags"])
    code_t = tokenize(doc["code"])
    head_t = tokenize(doc.get("headings", ""))
    total = 0.0
    for t in q:
        total += (
            4 * id_t.count(t)
            + 4 * title_t.count(t)
            + 2 * tag_t.count(t)
            + 2 * code_t.count(t)
            + 1 * head_t.count(t)
        )
        if any(t in n for n in id_t):
            total += 1
    return total


def collect():
    docs = []
    for root in DOC_ROOTS:
        if not root.is_dir():
            continue
        for md in sorted(root.rglob("*.md")):
            docs.append(parse_doc(md, archived="archive" in md.parts))
    return docs


def search(query, top=8):
    ranked = sorted(((score(query, d), d) for d in collect()), key=lambda x: -x[0])
    return [(sc, d) for sc, d in ranked if sc > 0][:top]


def main():
    ap = argparse.ArgumentParser(description="구현 문서 검색")
    ap.add_argument("query", nargs="+")
    ap.add_argument("--top", type=int, default=8)
    a = ap.parse_args()
    q = " ".join(a.query)
    res = search(q, a.top)
    if not res:
        print(f"매칭 문서 없음: {q}")
        return
    for sc, d in res:
        mark = " [archived]" if d["archived"] else ""
        rel = Path(d["path"]).relative_to(REPO_ROOT)
        print(f"{d['id']}  (score {sc:.0f}){mark} — {d['title'][:80]}\n    {rel}")


if __name__ == "__main__":
    main()
```

`tokenize`가 원본과 다르다 — 한글을 문자 클래스에 넣었다. 원본은 `[a-z0-9]+`라서 한국어 제목이 통째로 버려진다. 대상 문서의 `title`이 대부분 한국어이므로 이 변경이 없으면 검색이 사실상 영문 `id`만 본다.

- [ ] **Step 4: 테스트가 통과하는지 확인한다**

```bash
cd docs/script && python3 -m unittest test_search -v; cd -
```

Expected: PASS, 8개 전부.

- [ ] **Step 5: 실제 문서에 대고 돌려본다**

```bash
python3 docs/script/search.py "토핑 border" --top 5
python3 docs/script/search.py "login debug" --top 5
```

Expected: 두 쿼리 모두 1건 이상. 0건이면 `DOC_ROOTS` 경로가 틀렸거나 frontmatter 파싱이 실패한 것이다. 예외 없이 끝나는지도 본다 — `README.md`·`template.md`처럼 frontmatter 없는 파일에서 죽으면 안 된다.

- [ ] **Step 6: `docs/script/README.md`에 `search` 절을 되살린다**

Task 1 Step 5에서 지운 `search` 절 자리에, 개작된 동작을 적는다 — 대상 네 디렉토리, `archive/` 포함, frontmatter `id`·`title`·`tags`·`related_*`를 본다는 것.

- [ ] **Step 7: 커밋**

```bash
git add docs/script/search.py docs/script/test_search.py docs/script/README.md
git commit -m "feat(docs): search implementation docs by natural language query

Rewrite the collector that used to walk .claude/skills so it reads the
frontmatter of specs, plans, ADRs and architecture docs instead.
graphify only scans wiki/pages, so docs/ had no search until now.
Tokenizer now keeps Hangul, since most titles here are Korean.

Co-Authored-By: Claude Opus 5 (1M context) <noreply@anthropic.com>"
```

---

## Task 7: 전체 검증

**Files:** 없음 (검증 전용)

**Interfaces:**
- Consumes: Task 1~6의 산출물 전부
- Produces: 완료 판정 근거

- [ ] **Step 1: 링크 전수 검사**

```bash
python3 docs/script/check_links.py
```

Expected: exit 0. 저장소 전체를 검사한다 — `docs/` 밖(루트 `CLAUDE.md`, `README.md`, `wiki/`)에서 새로 깨진 것이 없는지 본다.

- [ ] **Step 2: 스크립트 테스트 전부**

```bash
cd docs/script && python3 -m unittest discover -p 'test_*.py' -v; cd -
```

Expected: PASS. `test_check_links`·`test_search` 양쪽.

- [ ] **Step 3: 파일 수 최종 대조**

```bash
SRC=/Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent
echo "원본: $(find "$SRC/parfait/android" "$SRC/parfait/api" -name '*.md' | wc -l)"
echo "사본: $(find docs -name '*.md' \
  | grep -v '2026-09-17-wiki-port' | grep -v '2026-09-22-parfait-docs-migration' | wc -l)"
```

Expected: 원본 254, 사본 255. 차이 1은 `docs/index.md`(원본 `parfait/index.md`는 `parfait/android`·`parfait/api` 바깥이라 원본 집계에 없다)다.

- [ ] **Step 4: 원본 저장소가 불변인지 확인한다**

```bash
git -C /Users/jeonheehoon/Documents/work_station/mashup/team-yg-pesonal-agent status --short
```

Expected: 출력 없음. 한 줄이라도 나오면 Global Constraints 위반이다 — 무엇이 바뀌었는지 보고하고 되돌린다.

- [ ] **Step 5: 빌드가 영향받지 않았는지 확인한다**

```bash
./gradlew build
```

Expected: `BUILD SUCCESSFUL`. 이관은 파일 추가일 뿐이므로 이관 전과 같아야 한다. 실패하면 이관과 무관한 기존 실패인지 먼저 확인한다 — `git stash` 후 재실행으로 가른다.

- [ ] **Step 6: 커밋할 것이 남았으면 커밋한다**

```bash
git status --short
```

Expected: 깨끗함. Task 1~6이 각자 커밋했으므로 남은 것이 없어야 한다.
