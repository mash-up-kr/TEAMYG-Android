from conftest import page

import lint


def test_duplicate_stem_across_content_dirs_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/토핑.md": page(),
        "wiki/pages/entities/토핑.md": page(),
    })
    found = lint.check_unique_names(root)
    assert [f.code for f in found] == ["파일명중복"]
    assert "토핑" in found[0].message
    assert found[0].level == "violation"


def test_raw_and_source_page_do_not_collide_with_src_prefix(make_repo):
    root = make_repo({
        "wiki/raw/정책-v1.md": "원본",
        "wiki/pages/sources/src-정책-v1.md": page(sources="[정책-v1.md]"),
    })
    assert lint.check_unique_names(root) == []


def test_raw_and_source_page_collide_without_prefix(make_repo):
    root = make_repo({
        "wiki/raw/정책-v1.md": "원본",
        "wiki/pages/sources/정책-v1.md": page(sources="[정책-v1.md]"),
    })
    assert [f.code for f in lint.check_unique_names(root)] == ["파일명중복"]


def test_skipped_dirs_do_not_participate_in_uniqueness(make_repo):
    """추적되지 않는 작업 부산물이 파일명 중복을 만들면 안 된다."""
    root = make_repo({
        "wiki/pages/concepts/토핑.md": page(),
        ".pytest_cache/토핑.md": "부산물",
        ".superpowers/sdd/토핑.md": "부산물",
    })
    assert lint.check_unique_names(root) == []


def test_runtime_bridge_files_may_share_a_stem(make_repo):
    root = make_repo({"CLAUDE.md": "브리지", "wiki/CLAUDE.md": "진입점"})
    assert lint.check_unique_names(root) == []


def test_readme_pair_may_share_a_stem(make_repo):
    """루트 README와 script/README는 GitHub이 이름을 정하는 진입 문서다."""
    root = make_repo({"README.md": "저장소 소개", "script/README.md": "툴링 홈"})
    assert lint.check_unique_names(root) == []


def test_template_still_collides_with_a_real_page(make_repo):
    root = make_repo({
        "wiki/templates/concept.md": "템플릿",
        "wiki/pages/concepts/concept.md": page(sources="[]"),
    })
    assert [f.code for f in lint.check_unique_names(root)] == ["파일명중복"]


def test_ordinary_page_taking_an_exempt_stem_collides(make_repo):
    """면제 파일의 stem을 일반 콘텐츠 페이지가 가져가면 그대로 위반이다.

    그 페이지는 수집되므로 name index에 있고, [[conventions]]는 깨지지 않고
    조용히 그 페이지로 resolve된다 — 이 검사가 아니면 아무도 못 잡는다.
    """
    root = make_repo({
        "wiki/conventions.md": "계약",
        "wiki/pages/concepts/conventions.md": page(sources="[]"),
    })
    found = lint.check_unique_names(root)
    assert [f.code for f in found] == ["파일명중복"]
    assert "wiki/pages/concepts/conventions.md" in found[0].message
