#!/usr/bin/env python3
"""위키 공용 유틸 — 페이지 수집·frontmatter 파싱·링크 추출.

검사 로직은 여기 두지 않는다. lint.py / check_status.py가 소비한다.
"""
from __future__ import annotations

import pathlib
import re
import unicodedata
from dataclasses import dataclass, field

LINK = re.compile(r"\[\[([^\]|#]+)(?:[|#][^\]]*)?\]\]")
COMMENT = re.compile(r"<!--.*?-->", re.S)
FENCE = re.compile(r"```.*?```", re.S)

# 링크 대상이 아니라 수집도 하지 않는 경로
# wiki/raw는 미통합 원본이다. check_raw_sync·check_manifest·_raw_texts가
# 파일시스템을 직접 훑어 짝을 맞추므로 아무도 이걸 Page로 소비하지 않는다.
# 수집하면 [[링크]] 대상이 아닌 원본이 인바운드 0건으로 고아 위반을 만든다 —
# raw/가 wiki/ 밖에 있던 원본 저장소에는 없던 문제다. 파일명 유일성 검사는
# all_markdown을 통해 그대로 받는다 (raw와 sources/src- 접두사 충돌은
# check_unique_names가 계속 잡아야 한다).
UNCOLLECTED_DIRS = ("wiki/templates", "wiki/script", "wiki/references", "wiki/raw")
UNCOLLECTED_FILES = (
    "wiki/CLAUDE.md",
    "wiki/conventions.md",
    "wiki/routing-misses.md",
)
# graphify 산출물. 수집도 안 하고 파일명 유일성 검사에도 넣지 않는다 — graphify가
# 쓰는 GRAPH_REPORT.md가 위키 페이지와 이름이 겹치는 것은 위키의 문제가 아니라
# 생성물의 문제이고, 여기서 위반으로 띄우면 사람이 고칠 수 없는 경고가 남는다.
# 스캔 루트(wiki/pages/) 밖에 두는 것과 별개로 이 마개가 필요하다 — 산출물이
# wiki/ 안에 있기 때문이다.
GENERATED_DIRS = ("wiki/graphify-out",)
# vault 스캔에서 제외할 디렉토리.
# .pytest_cache·.superpowers는 추적되지 않는 작업 부산물이다. 포함하면 lint 결과가
# 로컬 작업 사본과 새 클론에서 달라진다 — 검사는 커밋된 트리만 봐야 한다.
SKIP_DIRS = {
    ".git", "node_modules", ".pytest_cache", ".superpowers", ".claude",
}

# 블록 형식 YAML 리스트 항목: "  - 값"
BLOCK_ITEM = re.compile(r"^\s*-\s+(.*)$")


def nfc(s) -> str:
    return unicodedata.normalize("NFC", str(s))


def strip_noise(text: str) -> str:
    """주석 블록과 코드 펜스를 제거한다. 링크 검사는 항상 이걸 쓴다."""
    return FENCE.sub("", COMMENT.sub("", text))


def split_frontmatter(text: str) -> tuple[str | None, str]:
    """(frontmatter 본문, 나머지 본문). frontmatter가 없거나 닫히지 않으면 (None, 원문)."""
    lines = text.split("\n")
    if not lines or lines[0].strip() != "---":
        return None, text
    end = next((i for i, ln in enumerate(lines[1:], 1) if ln.strip() == "---"), None)
    if end is None:
        return None, text
    return "\n".join(lines[1:end]), "\n".join(lines[end + 1:])


def parse_fm(fm_text: str | None) -> dict:
    """frontmatter를 dict로. 리스트는 인라인(`[a, b]`)과 블록(`-` 항목) 둘 다 받는다.

    블록 형식은 Obsidian 속성 편집기가 GUI에서 속성을 건드리는 순간 내보내는 형식이다.
    저장소 루트가 vault이므로 사람이 편집하면 반드시 이 형식이 들어온다.
    두 형식은 같은 리스트를 만든다.
    """
    out: dict = {}
    lines = (fm_text or "").split("\n")
    i = 0
    while i < len(lines):
        m = re.match(r"^(\w+):\s*(.*)$", lines[i])
        if not m:
            i += 1
            continue
        key, val = m.group(1), m.group(2).strip()
        i += 1
        if val.startswith("[") and val.endswith("]"):
            out[key] = [nfc(x.strip()) for x in val[1:-1].split(",") if x.strip()]
            continue
        if val:
            out[key] = nfc(val)
            continue
        # 값이 비었다. 뒤따르는 "- 항목" 줄이 있으면 블록 리스트다.
        items = []
        while i < len(lines) and (bm := BLOCK_ITEM.match(lines[i])):
            item = bm.group(1).strip()
            if item:
                items.append(nfc(item))
            i += 1
        out[key] = items if items else ""
    return out


def as_list(value) -> list[str]:
    """frontmatter 값을 리스트로. 빈 값은 빈 리스트다.

    빈 문자열을 `[""]`로 만들면 "값이 있다"는 판정이 조용히 참이 되어,
    대상 없는 참조가 검사를 통과한 뒤 빈 메시지로 새어 나온다.
    """
    if value is None:
        return []
    if isinstance(value, list):
        return [v for v in value if v]
    return [value] if value else []


@dataclass(frozen=True)
class Finding:
    level: str  # "violation" | "warning"
    code: str
    path: str
    message: str

    def __str__(self) -> str:
        mark = "위반" if self.level == "violation" else "경고"
        return f"[{mark}/{self.code}] {self.path} — {self.message}"


@dataclass(frozen=True)
class Page:
    path: pathlib.PurePosixPath
    stem: str
    text: str
    has_fm: bool
    fm: dict = field(default_factory=dict)
    body: str = ""

    @property
    def rel(self) -> str:
        return str(self.path)

    @property
    def parent_name(self) -> str:
        return self.path.parent.name


def is_collected(rel: str) -> bool:
    if rel in UNCOLLECTED_FILES:
        return False
    skip = UNCOLLECTED_DIRS + GENERATED_DIRS
    return not any(rel.startswith(d + "/") for d in skip)


def all_markdown(repo_root: pathlib.Path) -> list[pathlib.Path]:
    """위키에 속하는 모든 마크다운. 파일명 유일성 검사의 대상이다.

    범위가 저장소 전체가 아니라 wiki/인 이유: [[이름]] 링크는 위키 안에서만
    쓰이고, 이 저장소의 안드로이드 쪽 마크다운(README.md가 세 곳에 있다)은
    링크 대상이 아니다. 전체를 보면 고칠 수 없는 파일명중복이 영구히 뜬다.
    """
    out = []
    wiki_root = repo_root / "wiki"
    if not wiki_root.exists():
        return out
    for p in wiki_root.rglob("*.md"):
        rel = p.relative_to(repo_root).as_posix()
        if SKIP_DIRS & set(pathlib.PurePosixPath(rel).parts):
            continue
        if any(rel.startswith(d + "/") for d in GENERATED_DIRS):
            continue
        out.append(p)
    return sorted(out)


def load_pages(repo_root: pathlib.Path) -> list[Page]:
    """wiki/ 아래의 수집 대상 페이지."""
    pages: list[Page] = []
    wiki_root = repo_root / "wiki"
    if not wiki_root.exists():
        return pages
    for p in sorted(wiki_root.rglob("*.md")):
        rel = p.relative_to(repo_root).as_posix()
        if SKIP_DIRS & set(pathlib.PurePosixPath(rel).parts):
            continue
        if not is_collected(rel):
            continue
        raw = p.read_text(encoding="utf-8")
        fm_text, rest = split_frontmatter(raw)
        pages.append(
            Page(
                path=pathlib.PurePosixPath(rel),
                stem=nfc(p.stem),
                text=raw,
                has_fm=fm_text is not None,
                fm=parse_fm(fm_text),
                body=strip_noise(rest),
            )
        )
    return pages


def find_links(text: str) -> list[str]:
    return [nfc(m.group(1).strip()) for m in LINK.finditer(text)]


def build_name_index(pages: list[Page]) -> dict[str, list[Page]]:
    idx: dict[str, list[Page]] = {}
    for pg in pages:
        idx.setdefault(pg.stem, []).append(pg)
    return idx
