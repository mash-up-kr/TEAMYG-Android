import pathlib
import sys

import pytest

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[1]))


@pytest.fixture
def make_repo(tmp_path):
    """가짜 저장소를 만든다. files는 {저장소상대경로: 내용} 매핑."""

    def _make(files: dict[str, str]) -> pathlib.Path:
        for rel, content in files.items():
            p = tmp_path / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(content, encoding="utf-8")
        return tmp_path

    return _make


def page(tags="[test]", updated="2026-08-10", sources=None, body=""):
    """frontmatter가 붙은 페이지 본문을 만든다."""
    lines = ["---", f"tags: {tags}", f"updated: {updated}"]
    if sources is not None:
        lines.append(f"sources: {sources}")
    lines += ["---", "", body]
    return "\n".join(lines)
