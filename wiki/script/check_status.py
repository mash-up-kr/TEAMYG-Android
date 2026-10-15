#!/usr/bin/env python3
"""판본 상태(status) 무결성 검사.

사용: python3 wiki/script/check_status.py    (저장소 루트에서)
종료 코드: 위반 0건이면 0, 있으면 1.
"""
from __future__ import annotations

import pathlib
import sys

import wikilib
from wikilib import Finding

VALID = {"current", "superseded", "partial"}


def repo_root_ok(repo_root: pathlib.Path) -> bool:
    """저장소 루트인지. 2단계 훅은 아무도 고르지 않은 cwd에서 실행된다."""
    return ((repo_root / "wiki").is_dir()
            and (repo_root / "wiki" / "conventions.md").is_file())


def wrong_root_message(repo_root: pathlib.Path) -> str:
    return (f"저장소 루트에서 실행해야 한다. 현재 위치: {repo_root}\n"
            f"wiki/ 와 wiki/conventions.md 가 보이지 않는다 — "
            f"검사 대상이 0건이 되어 위반을 통과로 오인한다.")


def collect_sources(repo_root: pathlib.Path, pages: list | None = None) -> dict[str, dict]:
    """{stem: frontmatter}. pages를 주면 재수집하지 않는다."""
    out = {}
    for pg in (wikilib.load_pages(repo_root) if pages is None else pages):
        if pg.parent_name != "sources":
            continue
        out[pg.stem] = {**pg.fm, "_rel": pg.rel, "_stem": pg.stem}
    return out


def _key(name: str) -> str:
    return wikilib.nfc(name)


def _as_list(value) -> list[str]:
    return wikilib.as_list(value)


def check_fields(sources: dict[str, dict]) -> list[Finding]:
    out = []
    for key, fm in sorted(sources.items()):
        rel = fm["_rel"]
        st = fm.get("status")
        if st is None:
            out.append(Finding("violation", "status", rel,
                               "status 누락 (판정 불가 = 인용 금지)"))
            continue
        if not isinstance(st, str) or st not in VALID:
            out.append(Finding("violation", "status", rel, f"status 값 오류 '{st}'"))
        if st in ("superseded", "partial") and not _as_list(fm.get("superseded_by")):
            out.append(Finding("violation", "status", rel,
                               f"{st}인데 superseded_by 없음"))
        if st == "partial" and not fm.get("scope"):
            out.append(Finding("violation", "status", rel, "partial인데 scope 없음"))
        if st == "current" and _as_list(fm.get("superseded_by")):
            out.append(Finding("violation", "status", rel,
                               "current인데 superseded_by 있음"))
    return out


def check_refs(sources: dict[str, dict]) -> list[Finding]:
    out = []
    for key, fm in sorted(sources.items()):
        for field in ("superseded_by", "supersedes"):
            for target in _as_list(fm.get(field)):
                if _key(target) not in sources:
                    out.append(Finding(
                        "violation", "status", fm["_rel"],
                        f"{field} 대상 없음 → {target}"))
    return out


def check_chain(sources: dict[str, dict]) -> list[Finding]:
    out = []
    for key, fm in sorted(sources.items()):
        seen, cur = [key], key
        while sources.get(cur, {}).get("status") in ("superseded", "partial"):
            nxt = _as_list(sources[cur].get("superseded_by"))
            if not nxt:
                break
            cur = _key(nxt[0])
            if cur in seen:
                out.append(Finding("violation", "status", fm["_rel"],
                                   f"superseded_by 사이클 {' → '.join(seen + [cur])}"))
                break
            seen.append(cur)
            # 여기서 break하지 않는다. cur가 없으면 다음 while 조건이 False가 되어
            # else로 떨어지고 "current에 도달 못함"이 보고된다. break하면 else를
            # 건너뛰어 끊긴 체인이 조용히 통과한다.
        else:
            if sources.get(cur, {}).get("status") != "current":
                out.append(Finding("violation", "status", fm["_rel"],
                                   f"체인이 current에 도달 못함 ({' → '.join(seen)})"))
    return out


def check_reverse(sources: dict[str, dict]) -> list[Finding]:
    """supersedes ↔ superseded_by 양방향 일치. 계약(§2)이 양방향을 요구하므로
    한쪽만 어긋나도 위반이다. 한 방향을 경고로 두면 일방적 판본 주장이 통과한다."""
    out = []
    for key, fm in sorted(sources.items()):
        stem = fm["_stem"]
        for target in _as_list(fm.get("supersedes")):
            tk = _key(target)
            if tk in sources and stem not in _as_list(sources[tk].get("superseded_by")):
                out.append(Finding(
                    "violation", "status", fm["_rel"],
                    f"supersedes={target} 인데 {target}.superseded_by에 {stem} 없음"))
        for target in _as_list(fm.get("superseded_by")):
            tk = _key(target)
            if tk in sources and stem not in _as_list(sources[tk].get("supersedes")):
                out.append(Finding(
                    "violation", "status", fm["_rel"],
                    f"superseded_by={target} 인데 {target}.supersedes에 {stem} 없음"))
    return out


def check_index_projection(repo_root: pathlib.Path,
                           sources: dict[str, dict]) -> list[Finding]:
    """wiki/pages/index.md의 표기가 frontmatter status와 맞는지. frontmatter가 정답이다."""
    idx = repo_root / "wiki" / "pages" / "index.md"
    lines = idx.read_text(encoding="utf-8").split("\n") if idx.exists() else []
    out = []
    for key, fm in sorted(sources.items()):
        stem, rel = fm["_stem"], fm["_rel"]
        line = next((l for l in lines if l.startswith(f"- [[{stem}]]")), None)
        if line is None:
            out.append(Finding("violation", "status", rel,
                               "wiki/pages/index.md에 줄 없음"))
            continue
        has_cur, has_sup = "현행 정본" in line, "🔁" in line
        st = fm.get("status")
        if st == "current" and not has_cur:
            out.append(Finding("violation", "status", rel,
                               "index 불일치 — status=current인데 '현행 정본' 표기 없음"))
        if st in ("superseded", "partial") and not has_sup:
            out.append(Finding("violation", "status", rel,
                               f"index 불일치 — status={st}인데 🔁 표기 없음"))
        if st == "superseded" and has_cur:
            out.append(Finding("violation", "status", rel,
                               "index 불일치 — 전면 폐기본인데 '현행 정본' 표기"))
        if st == "partial" and not (has_cur and has_sup):
            out.append(Finding(
                "violation", "status", rel,
                "index 불일치 — partial은 현행 범위와 🔁 대체 범위를 모두 표기해야 함"))
    return out


def run_all(repo_root: pathlib.Path, pages: list | None = None) -> list[Finding]:
    sources = collect_sources(repo_root, pages)
    return [
        *check_fields(sources),
        *check_refs(sources),
        *check_chain(sources),
        *check_reverse(sources),
        *check_index_projection(repo_root, sources),
    ]


def main() -> int:
    repo_root = pathlib.Path.cwd()
    if not repo_root_ok(repo_root):
        print(wrong_root_message(repo_root), file=sys.stderr)
        return 2
    pages = wikilib.load_pages(repo_root)
    sources = collect_sources(repo_root, pages)
    findings = run_all(repo_root, pages)
    violations = [f for f in findings if f.level == "violation"]
    warnings = [f for f in findings if f.level == "warning"]
    print(f"소스 {len(sources)}건 검사")
    for f in findings:
        print(f"- {f}")
    print(f"— 위반 {len(violations)}건 (경고 {len(warnings)}건)")
    return 1 if violations else 0


if __name__ == "__main__":
    sys.exit(main())
