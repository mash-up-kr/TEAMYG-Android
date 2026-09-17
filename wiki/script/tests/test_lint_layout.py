from conftest import page

import lint
import wikilib


def _layout(root):
    return lint.check_layout(wikilib.load_pages(root))


def test_accepted_shapes_pass(make_repo):
    root = make_repo({
        "wiki/log.md": page(),
        "wiki/open-questions.md": page(),
        "wiki/pages/purpose.md": page(),
        "wiki/pages/index.md": page(),
        "wiki/pages/overview.md": page(),
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
        "wiki/pages/concepts/개념.md": page(sources="[]"),
        "wiki/pages/entities/인물.md": page(sources="[]"),
        "wiki/pages/queries/질의-2026-09-17.md": page(),
        "wiki/pages/synthesis/분석.md": page(),
    })
    assert _layout(root) == []


def test_subdirectory_of_content_dir_is_violation(make_repo):
    root = make_repo({"wiki/pages/concepts/sub/깊은개념.md": page(sources="[]")})
    found = _layout(root)
    assert [f.code for f in found] == ["배치"]
    assert found[0].level == "violation"
    assert found[0].path == "wiki/pages/concepts/sub/깊은개념.md"
    assert "하위 폴더" in found[0].message


def test_loose_file_at_pages_root_is_violation(make_repo):
    root = make_repo({"wiki/pages/아무거나.md": page()})
    found = _layout(root)
    assert [f.code for f in found] == ["배치"]
    assert found[0].level == "violation"
    assert "wiki/pages/ 루트에 올 수 없는 파일" in found[0].message


def test_unknown_content_dir_is_violation(make_repo):
    root = make_repo({"wiki/pages/notes/메모.md": page()})
    found = _layout(root)
    assert [f.code for f in found] == ["배치"]
    assert "콘텐츠 디렉토리가 아님" in found[0].message


def test_sources_page_without_src_prefix_is_violation(make_repo):
    root = make_repo({"wiki/pages/sources/정책.md": page(sources="[정책.md]")})
    found = _layout(root)
    assert [f.code for f in found] == ["배치"]
    assert "src-" in found[0].message


def test_files_outside_pages_are_ignored(make_repo):
    """wiki/ 바로 아래 운영 파일은 배치 검사 대상이 아니다."""
    root = make_repo({
        "wiki/log.md": page(),
        "wiki/open-questions.md": page(),
    })
    assert _layout(root) == []
