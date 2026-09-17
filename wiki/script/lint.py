#!/usr/bin/env python3
"""위키 lint — 결정적 기계 검사.

사용: python3 wiki/script/lint.py    (저장소 루트에서)
종료 코드: 위반 0건이면 0, 있으면 1. 경고는 종료 코드에 영향을 주지 않는다.

모순 감지·해소분 하류 반영·데이터 공백·stale 서술은 기계 검사 밖이다.
"""
from __future__ import annotations

import json
import pathlib
import re
import sys

import wikilib
from wikilib import Finding, Page, nfc

# 인바운드 링크가 없어도 정상인 페이지 (스펙 3.2, 3.4)
ORPHAN_EXEMPT_PATHS = {
    "wiki/log.md",
    "wiki/open-questions.md",
    "wiki/pages/index.md",
    "wiki/pages/purpose.md",
    "wiki/pages/overview.md",
}
ORPHAN_EXEMPT_DIRS = {"queries"}
# wiki/pages/ 루트에 올 수 있는 구조 파일. 한 곳에서만 정의한다 — 네 번째 구조
# 파일이 생겼을 때 배치 검사와 frontmatter 검사가 어긋나지 않게 하기 위해서다.
STRUCTURE_FILES = ("purpose.md", "index.md", "overview.md")


def check_unique_names(repo_root: pathlib.Path) -> list[Finding]:
    """vault 전역 파일명 유일성. Obsidian이 [[파일명]]을 유일하게 resolve하려면 필수다.

    UNCOLLECTED_FILES는 **서로 간에만** stem을 공유할 수 있다 (루트 CLAUDE.md와
    wiki/CLAUDE.md). 런타임이 이름을 정하는 파일이라 바꿀 수 없고, 수집되지 않아
    name index에 없으므로 둘만 겹치는 한 [[ ]] 해석이 모호해질 일이 없다.

    면제 파일을 그룹에서 빼 버리면 안 된다. 그러면 CLAUDE·conventions 같은 stem을
    아무도 못 쓰게 되는 것이 아니라, 반대로 일반 콘텐츠 페이지가 그 이름을 가져가도
    조용히 통과한다 — 그 페이지는 name index에 있으니 [[conventions]]가 실재하는
    페이지로 resolve되고, 깨진링크로도 링크모호로도 잡히지 않는다. 그래서 면제는
    "충돌한 경로가 전부 면제 대상일 때"만 적용한다.

    UNCOLLECTED_DIRS(templates/·script/·references/)는 제외하지 않는다. 템플릿
    concept.md와 실제 concept 페이지가 겹치면 Obsidian이 잘못 고를 수 있고, 그건
    이 검사만이 잡을 수 있는 진짜 모호성이다.
    """
    groups: dict[str, list[str]] = {}
    for p in wikilib.all_markdown(repo_root):
        rel = p.relative_to(repo_root).as_posix()
        groups.setdefault(nfc(p.stem), []).append(rel)
    out = []
    for stem, paths in sorted(groups.items()):
        if len(paths) < 2:
            continue
        if all(rel in wikilib.UNCOLLECTED_FILES for rel in paths):
            continue
        out.append(Finding(
            "violation", "파일명중복", paths[0],
            f"'{stem}' 이름이 {len(paths)}곳에 있다: {', '.join(sorted(paths))}. "
            f"[[{stem}]] 링크가 어느 파일을 가리키는지 결정되지 않는다"))
    return out


def check_links(pages: list[Page], index: dict[str, list[Page]]
                ) -> tuple[list[Finding], dict[str, set[str]]]:
    out: list[Finding] = []
    inbound: dict[str, set[str]] = {pg.stem: set() for pg in pages}
    for pg in pages:
        for target in wikilib.find_links(pg.body):
            if "/" in target:
                out.append(Finding(
                    "violation", "경로링크", pg.rel,
                    f"[[{target}]] — 경로 접두사 금지. 파일명만 쓴다"))
                continue
            hits = index.get(target, [])
            if not hits:
                out.append(Finding(
                    "violation", "깨진링크", pg.rel, f"[[{target}]] 대상 없음"))
            elif len(hits) > 1:
                out.append(Finding(
                    "violation", "링크모호", pg.rel,
                    f"[[{target}]] 후보 {len(hits)}개: "
                    f"{', '.join(h.rel for h in hits)}"))
            else:
                inbound[target].add(pg.stem)
    return out, inbound


def _orphan_exempt(pg: Page) -> bool:
    return pg.rel in ORPHAN_EXEMPT_PATHS or pg.parent_name in ORPHAN_EXEMPT_DIRS


def check_orphans(pages: list[Page], inbound: dict[str, set[str]]) -> list[Finding]:
    out = []
    for pg in pages:
        if _orphan_exempt(pg):
            continue
        deg = len(inbound.get(pg.stem, set()) - {pg.stem})
        if deg == 0:
            out.append(Finding("violation", "고아", pg.rel, "인바운드 링크 0건"))
        elif deg == 1:
            src = next(iter(inbound[pg.stem] - {pg.stem}))
            out.append(Finding("warning", "약연결", pg.rel,
                               f"인바운드 1건({src}) — 개념망 편입 검토"))
    return out


CONTENT_DIRS = {"sources", "concepts", "entities", "queries", "synthesis"}
# frontmatter에 sources 필드가 추가로 필요한 디렉토리
SOURCED_DIRS = {"sources", "concepts", "entities"}
STRUCTURE_PATHS = {f"wiki/pages/{f}" for f in STRUCTURE_FILES}


def _needs_frontmatter(pg: Page) -> bool:
    # wiki/log.md와 wiki/open-questions.md는 여기서 명시적으로 빼지 않는다.
    # STRUCTURE_PATHS에 없고 부모가 CONTENT_DIRS도 아니라 False가 된다.
    return pg.rel in STRUCTURE_PATHS or pg.parent_name in CONTENT_DIRS


def _filled(fm: dict, key: str) -> bool:
    """키가 있고 값이 비어 있지 않은가. 빈 값은 요구를 충족한 것이 아니다."""
    return bool(fm.get(key))


def check_frontmatter(pages: list[Page]) -> list[Finding]:
    out = []
    for pg in pages:
        if not _needs_frontmatter(pg):
            continue
        if not pg.has_fm:
            out.append(Finding("violation", "frontmatter", pg.rel, "frontmatter 없음"))
            continue
        missing = [k for k in ("tags", "updated") if not _filled(pg.fm, k)]
        if pg.parent_name in SOURCED_DIRS and not _filled(pg.fm, "sources"):
            missing.append("sources")
        if missing:
            out.append(Finding("violation", "frontmatter", pg.rel,
                               f"{', '.join(missing)} 누락 또는 빈 값"))
    return out


def check_provenance(pages: list[Page]) -> list[Finding]:
    """concepts·entities의 sources 선언과 본문 인용이 맞아야 한다.

    두 방향을 본다.
    - 본문이 인용한 소스 요약 페이지가 frontmatter sources에도 있는가
    - frontmatter sources가 선언한 대상이 sources/에 실제로 있는가

    `sources/` 페이지의 sources는 raw 원본 파일명을 가리키며 check_raw_sync가
    그쪽을 검사한다. 여기서 중복 보고하지 않는다.
    """
    source_stems = {pg.stem for pg in pages if pg.parent_name == "sources"}
    out = []
    for pg in pages:
        if pg.parent_name not in ("concepts", "entities"):
            continue
        declared = {nfc(x).removesuffix(".md")
                    for x in wikilib.as_list(pg.fm.get("sources"))}
        cited = set(wikilib.find_links(pg.body)) & source_stems
        for miss in sorted(cited - declared):
            out.append(Finding("violation", "출처추적", pg.rel,
                               f"본문이 [[{miss}]]를 인용하나 frontmatter sources에 없음"))
        for dangling in sorted(declared - source_stems):
            out.append(Finding(
                "violation", "출처추적", pg.rel,
                f"frontmatter sources '{dangling}' 대상 없음 — "
                f"wiki/pages/sources/{dangling}.md 가 없다"))
    return out


def check_layout(pages: list[Page]) -> list[Finding]:
    """wiki/pages/ 아래의 배치 규칙. 규정 밖 위치는 콘텐츠 검사를 통째로 비껴간다.

    frontmatter·raw 정합·판본 검사가 모두 부모 디렉토리 이름으로 대상을 고른다.
    `concepts/sub/x.md`나 pages/ 루트의 임의 파일은 어느 검사에도 걸리지 않으면서
    인바운드 링크 1건만으로 합법이 된다. 그 구멍을 여기서 막는다.
    """
    expected = (f"{{{','.join(sorted(STRUCTURE_FILES))}}} "
                f"또는 {{{','.join(sorted(CONTENT_DIRS))}}}/ 바로 아래")
    allowed_root = {nfc(f) for f in STRUCTURE_FILES}
    out = []
    for pg in pages:
        parts = pg.path.parts
        if parts[:2] != ("wiki", "pages"):
            continue
        tail = parts[2:]
        if len(tail) == 1:
            if nfc(tail[0]) not in allowed_root:
                out.append(Finding(
                    "violation", "배치", pg.rel,
                    f"wiki/pages/ 루트에 올 수 없는 파일 — {expected}"))
            continue
        if len(tail) > 2:
            out.append(Finding(
                "violation", "배치", pg.rel,
                f"콘텐츠 디렉토리 하위 폴더는 허용되지 않음 — {expected}"))
            continue
        if tail[0] not in CONTENT_DIRS:
            out.append(Finding(
                "violation", "배치", pg.rel,
                f"'{tail[0]}/' 은 콘텐츠 디렉토리가 아님 — {expected}"))
            continue
        if tail[0] == "sources" and not pg.stem.startswith("src-"):
            out.append(Finding(
                "violation", "배치", pg.rel,
                "sources/ 페이지는 stem이 'src-'로 시작해야 함 "
                "(원본 파일명과 겹치지 않게 하는 접두사)"))
    return out


def _raw_stems(repo_root: pathlib.Path) -> set[str]:
    """wiki/raw/**/X.md → X. 도메인 차원이 없어 stem 하나로 짝을 맞춘다."""
    raw_root = repo_root / "wiki" / "raw"
    if not raw_root.exists():
        return set()
    return {nfc(p.stem) for p in raw_root.rglob("*.md")}


def check_raw_sync(repo_root: pathlib.Path, pages: list[Page]) -> list[Finding]:
    raw = _raw_stems(repo_root)
    src = {pg.stem.removeprefix("src-")
           for pg in pages if pg.parent_name == "sources"}
    out = []
    for stem in sorted(raw - src):
        out.append(Finding(
            "violation", "raw정합", f"wiki/raw/{stem}.md",
            f"ingest 안 됨 — wiki/pages/sources/src-{stem}.md 없음"))
    for stem in sorted(src - raw):
        out.append(Finding(
            "violation", "raw정합", f"wiki/pages/sources/src-{stem}.md",
            f"대응 raw 원본 없음 — wiki/raw/ 아래에 {stem}.md 가 없다"))
    return out


def check_manifest(repo_root: pathlib.Path) -> list[Finding]:
    raw_root = repo_root / "wiki" / "raw"
    mf = raw_root / ".manifest.json"
    rel = "wiki/raw/.manifest.json"
    if not mf.exists():
        return []
    try:
        entries = json.loads(mf.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return [Finding("violation", "매니페스트", rel, f"JSON 파싱 실패: {e}")]
    actual = {p.relative_to(repo_root).as_posix() for p in raw_root.rglob("*.md")}
    recorded = set(entries)
    out = []
    for miss in sorted(actual - recorded):
        out.append(Finding("violation", "매니페스트", miss, "매니페스트에 등록 안 됨"))
    for stale in sorted(recorded - actual):
        out.append(Finding("violation", "매니페스트", stale,
                           "매니페스트에 있으나 파일 없음"))
    return out


# (등급, 정규식). 스펙 9.2 — 자격증명은 위반, 개인 식별 정보는 경고.
SENSITIVE = {
    "credential": ("violation",
                   r"(?i)(api[_-]?key|secret|password|token)\s*[:=]\s*[\"']?[A-Za-z0-9_\-]{16,}"),
    "jwt": ("violation", r"eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),
    "private_key": ("violation", r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    "email": ("warning", r"[\w.+-]+@[\w-]+\.[\w.]+"),
    "phone": ("warning", r"01[016-9][-\s.]?\d{3,4}[-\s.]?\d{4}"),
    "resident_id": ("warning", r"\d{6}[-]\d{7}"),
    "abs_path": ("warning", r"/Users/[a-z]+"),
}

ACTION_VALUES = ("create-page", "research", "skip")
ITEM_HEAD = re.compile(r"^### \[")
ACTION_LINE = re.compile(r"^- \*\*action\*\*:\s*(.+?)\s*$")


def _raw_texts(repo_root: pathlib.Path | None) -> list[tuple[str, str]]:
    """raw/ 아래 마크다운의 (상대경로, 본문). 붙여넣은 자격증명이 가장 먼저 닿는 곳이다."""
    if repo_root is None:
        return []
    raw_root = repo_root / "wiki" / "raw"
    if not raw_root.exists():
        return []
    out = []
    for p in sorted(raw_root.rglob("*.md")):
        rel = p.relative_to(repo_root).as_posix()
        if wikilib.SKIP_DIRS & set(pathlib.PurePosixPath(rel).parts):
            continue
        out.append((rel, p.read_text(encoding="utf-8")))
    return out


def check_sensitive(pages: list[Page],
                    repo_root: pathlib.Path | None = None) -> list[Finding]:
    """wiki/ 페이지와 raw/ 원본을 함께 훑는다.

    raw/는 손대지 않은 제3자 원본이 쌓이는 곳이라 자격증명이 가장 들어오기 쉽다.
    여기를 안 보면 '커밋 차단' 규칙이 표적을 비껴간다.
    """
    targets = [(pg.rel, pg.text) for pg in pages] + _raw_texts(repo_root)
    out = []
    for rel, text in targets:
        for i, line in enumerate(text.split("\n"), 1):
            for kind, (level, pat) in SENSITIVE.items():
                if re.search(pat, line):
                    out.append(Finding(level, f"민감/{kind}", f"{rel}:{i}",
                                       line.strip()[:80]))
    return out


def check_action_enum(repo_root: pathlib.Path) -> list[Finding]:
    """open-questions.md의 각 항목이 허용된 action 값을 하나 갖는지 검사한다."""
    path = repo_root / "wiki" / "open-questions.md"
    if not path.exists():
        return []
    rel = "wiki/open-questions.md"
    out = []
    heading, action, start = None, None, 0
    lines = path.read_text(encoding="utf-8").split("\n")

    def close(head, act, lineno):
        if head is None:
            return
        if act is None:
            out.append(Finding("violation", "action", f"{rel}:{lineno}",
                               f"'{head}' 항목에 action 누락 "
                               f"(허용: {', '.join(ACTION_VALUES)})"))
        elif act not in ACTION_VALUES:
            out.append(Finding("violation", "action", f"{rel}:{lineno}",
                               f"'{head}' 항목의 action 값 '{act}' 은 허용되지 않음 "
                               f"(허용: {', '.join(ACTION_VALUES)})"))

    for i, line in enumerate(lines, 1):
        if ITEM_HEAD.match(line):
            close(heading, action, start)
            heading, action, start = line.strip()[4:], None, i
            continue
        m = ACTION_LINE.match(line)
        if m and heading is not None:
            action = m.group(1)
    close(heading, action, start)
    return out


def run_all(repo_root: pathlib.Path, pages: list[Page] | None = None) -> list[Finding]:
    if pages is None:
        pages = wikilib.load_pages(repo_root)
    index = wikilib.build_name_index(pages)
    link_findings, inbound = check_links(pages, index)
    # 함수 안에서 import한다 — graph_signals는 wiki/graphify-out/graph.json이라는
    # 산출물만 읽는 독립 모듈이라 항상 존재하지만, 모듈 로드를 호출 시점까지
    # 늦춰 두면 이 파일 상단의 import 목록이 "항상 필요한 것"과 "검사 하나가
    # 쓰는 것"으로 갈리지 않는다.
    import graph_signals
    return [
        *check_unique_names(repo_root),
        *link_findings,
        *check_orphans(pages, inbound),
        *check_layout(pages),
        *check_frontmatter(pages),
        *check_provenance(pages),
        *check_raw_sync(repo_root, pages),
        *check_manifest(repo_root),
        *check_sensitive(pages, repo_root),
        *check_action_enum(repo_root),
        *graph_signals.check_graph_signals(repo_root, pages),
    ]


def repo_root_ok(repo_root: pathlib.Path) -> bool:
    """저장소 루트인지. 2단계 훅은 아무도 고르지 않은 cwd에서 실행된다."""
    return ((repo_root / "wiki").is_dir()
            and (repo_root / "wiki" / "conventions.md").is_file())


def wrong_root_message(repo_root: pathlib.Path) -> str:
    return (f"저장소 루트에서 실행해야 한다. 현재 위치: {repo_root}\n"
            f"wiki/ 와 wiki/conventions.md 가 보이지 않는다 — "
            f"검사 대상이 0건이 되어 위반을 통과로 오인한다.")


def main() -> int:
    repo_root = pathlib.Path.cwd()
    if not repo_root_ok(repo_root):
        print(wrong_root_message(repo_root), file=sys.stderr)
        return 2

    import check_status

    pages = wikilib.load_pages(repo_root)
    findings = run_all(repo_root, pages) + check_status.run_all(repo_root, pages)
    violations = [f for f in findings if f.level == "violation"]
    warnings = [f for f in findings if f.level == "warning"]

    print(f"검사 대상 {len(pages)}건 (templates·script·references 제외)\n")
    print(f"## 위반 {len(violations)}건")
    for f in violations:
        print(f"- {f}")
    print(f"\n## 경고 {len(warnings)}건 (판단 필요, 종료 코드에 영향 없음)")
    for f in warnings:
        print(f"- {f}")
    print("\n> 모순 감지·해소분 하류 반영·데이터 공백·stale 서술은 기계 검사 밖 — "
          "사람/LLM이 읽어야 한다.")
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
