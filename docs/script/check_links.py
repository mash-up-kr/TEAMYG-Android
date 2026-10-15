#!/usr/bin/env python3
"""마크다운 상대 링크가 실제 파일로 resolve 되는지 전수 검사한다.

용법:
    python3 docs/script/check_links.py                 # repo 전체
    python3 docs/script/check_links.py docs            # 하위 경로만
    python3 docs/script/check_links.py --quiet         # 깨진 것만 출력

규약:
- stdlib 전용(pip 의존성 0).
- repo 루트 = Path(__file__).resolve().parents[2] 기준 상대 경로.
- 깨진 링크가 하나라도 있으면 exit 1.

검사 대상은 `](경로)` 꼴의 **상대 링크**와 raw HTML의 `<img src="…">`·`<a href="…">`다.
URL(http/https/mailto)·앵커 전용(`#x`)·절대 경로는 건너뛴다. `[[wikilink]]`는
Obsidian이 파일명으로 resolve 하므로 대상이 아니다(위키 쪽 검사는 `wiki/script/lint.py`가 한다).

디렉토리를 옮긴 뒤 `../` 깊이가 맞는지 확인하는 것이 주 용도다. 아카이브 이동
(`specs/` → `specs/archive/`)도 같은 종류의 작업이라 같은 검사가 쓰인다.
"""
import argparse
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

SKIP_DIRS = {".git", ".superpowers", "node_modules", "__pycache__", ".venv", "venv"}

# ```...``` 펜스와 `인라인 코드` 안의 예시는 실제 링크가 아니다
INLINE_CODE = re.compile(r"`[^`\n]*`")
# 펜스 여는/닫는 줄: 들여쓰기 0~3칸 + 백틱 3개 이상 또는 물결 3개 이상
FENCE_LINE = re.compile(r"^\s{0,3}(`{3,}|~{3,})")
# ](...) 꼴. 링크 텍스트의 중첩 괄호는 다루지 않는다 — 경로에 ')' 가 없다는 가정.
# 앞이 ']' 이면 `[[wikilink]](설명)` 이라 링크가 아니다
LINK = re.compile(r"(?<!\])\]\(\s*([^)\s]+?)\s*(?:\s+\"[^\"]*\")?\)")
# 마크다운 링크 문법을 안 쓰는 raw HTML의 <img src="…">·<a href="…">
HTML_ATTR = re.compile(r"""<[a-zA-Z][^>]*?\s(?:src|href)\s*=\s*["']([^"']+)["']""")

EXTERNAL = ("http://", "https://", "mailto:", "tel:", "data:")


def is_checkable(target: str) -> bool:
    """검사 대상인 상대 링크인지."""
    if not target or target.startswith(("#", "/")):
        return False
    if target.startswith(EXTERNAL):
        return False
    if target.startswith("<") or "{" in target:  # 템플릿 플레이스홀더
        return False
    if target.startswith("[["):  # 마크다운 링크 안에 넣은 Obsidian 위키링크
        return False
    return True


def fence_spans(lines):
    """닫힌 펜스의 (시작, 끝) 줄 인덱스. 안 닫힌 여는 마커는 펜스로 치지 않는다.

    게이트는 가리는 쪽이 아니라 검사하는 쪽으로 실패해야 한다 — 떠돌이 백틱
    하나가 뒤따르는 진짜 링크를 통째로 감추는 것이 이전 구현의 결함이었다.
    """
    spans = []
    open_i = None
    marker = None
    for i, line in enumerate(lines):
        m = FENCE_LINE.match(line)
        if not m:
            continue
        tok = m.group(1)
        if open_i is None:
            open_i, marker = i, tok
        elif tok[0] == marker[0] and len(tok) >= len(marker):
            spans.append((open_i, i))
            open_i = marker = None
    return spans


def blank_out(text: str) -> str:
    """코드 펜스·인라인 코드를 줄 수를 지킨 채 지운다 — 줄 번호가 어긋나지 않게."""
    lines = text.split("\n")
    for start, end in fence_spans(lines):
        for i in range(start, end + 1):
            lines[i] = ""
    return INLINE_CODE.sub("", "\n".join(lines))


def iter_markdown(roots: list[Path]):
    for root in roots:
        if root.is_file():
            yield root
            continue
        for path in sorted(root.rglob("*.md")):
            if any(part in SKIP_DIRS for part in path.parts):
                continue
            yield path


def broken_links(path: Path) -> list[tuple[int, str, Path]]:
    """(줄번호, 원본 링크, resolve 된 경로) 목록."""
    try:
        text = path.read_text(encoding="utf-8")
    except (UnicodeDecodeError, OSError):
        return []

    found = []
    for lineno, line in enumerate(blank_out(text).splitlines(), start=1):
        targets = LINK.findall(line) + HTML_ATTR.findall(line)
        for target in targets:
            if not is_checkable(target):
                continue
            # 앵커·쿼리를 떼고 퍼센트 인코딩된 공백만 되돌린다
            bare = target.split("#", 1)[0].split("?", 1)[0].replace("%20", " ")
            if not bare:  # 같은 문서 앵커
                continue
            resolved = (path.parent / bare).resolve()
            if not resolved.exists():
                found.append((lineno, target, resolved))
    return found


def main() -> int:
    ap = argparse.ArgumentParser(description="마크다운 상대 링크 전수 resolve 검사")
    ap.add_argument("roots", nargs="*", default=[], help="검사할 경로(기본: repo 전체)")
    ap.add_argument("--quiet", action="store_true", help="요약 없이 깨진 링크만 출력")
    args = ap.parse_args()

    roots = [Path(r) if Path(r).is_absolute() else REPO_ROOT / r for r in args.roots] or [REPO_ROOT]

    scanned = 0
    total_broken = 0
    for path in iter_markdown(roots):
        scanned += 1
        for lineno, target, resolved in broken_links(path):
            total_broken += 1
            rel = path.relative_to(REPO_ROOT)
            print(f"{rel}:{lineno}: 깨진 링크 {target!r} → {resolved}")

    if not args.quiet:
        print(f"\n파일 {scanned}개 검사 · 깨진 링크 {total_broken}건")

    return 1 if total_broken else 0


if __name__ == "__main__":
    sys.exit(main())
