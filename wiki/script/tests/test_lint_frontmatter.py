from conftest import page

import lint
import wikilib


def test_missing_frontmatter_is_violation(make_repo):
    root = make_repo({"wiki/pages/concepts/x.md": "frontmatter 없음"})
    found = lint.check_frontmatter(wikilib.load_pages(root))
    assert [f.code for f in found] == ["frontmatter"]
    assert "없음" in found[0].message


def test_missing_tags_and_updated_are_reported(make_repo):
    root = make_repo({"wiki/pages/concepts/x.md": "---\nsources: []\n---\n본문"})
    found = lint.check_frontmatter(wikilib.load_pages(root))
    assert "tags" in found[0].message and "updated" in found[0].message


def test_sources_required_for_concepts(make_repo):
    root = make_repo({"wiki/pages/concepts/x.md": page()})
    found = lint.check_frontmatter(wikilib.load_pages(root))
    assert "sources" in found[0].message


def test_sources_not_required_for_queries(make_repo):
    root = make_repo({"wiki/pages/queries/q.md": page()})
    assert lint.check_frontmatter(wikilib.load_pages(root)) == []


def test_log_is_exempt(make_repo):
    root = make_repo({"wiki/log.md": "## [2026-08-10] ingest | 제목"})
    assert lint.check_frontmatter(wikilib.load_pages(root)) == []


def test_structure_files_need_frontmatter(make_repo):
    root = make_repo({"wiki/pages/purpose.md": "목적만 적음"})
    found = lint.check_frontmatter(wikilib.load_pages(root))
    assert [f.code for f in found] == ["frontmatter"]


def test_provenance_violation_when_cited_source_not_declared(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
        "wiki/pages/concepts/개념.md": page(sources="[]", body="[[src-정책]] 참고"),
    })
    found = lint.check_provenance(wikilib.load_pages(root))
    assert [f.code for f in found] == ["출처추적"]
    assert "src-정책" in found[0].message


def test_provenance_passes_when_declared(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
        "wiki/pages/concepts/개념.md": page(sources="[src-정책]", body="[[src-정책]]"),
    })
    assert lint.check_provenance(wikilib.load_pages(root)) == []


def test_provenance_ignores_links_in_fences(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
        "wiki/pages/concepts/개념.md": page(
            sources="[]", body="```\n[[src-정책]]\n```"),
    })
    assert lint.check_provenance(wikilib.load_pages(root)) == []


BLOCK_PAGE = ("---\ntags:\n  - 정책\nupdated: 2026-08-10\n"
              "sources:\n  - src-정책\n---\n\n본문")


def test_block_form_frontmatter_satisfies_required_fields(make_repo):
    """Obsidian 속성 편집기가 내보내는 블록 형식도 요구를 충족한다."""
    root = make_repo({"wiki/pages/concepts/개념.md": BLOCK_PAGE})
    assert lint.check_frontmatter(wikilib.load_pages(root)) == []


def test_key_present_but_empty_is_violation(make_repo):
    """키만 있고 값이 비면 요구를 충족한 것이 아니다."""
    root = make_repo({
        "wiki/pages/concepts/개념.md":
            "---\ntags:\nupdated: 2026-08-10\nsources:\n---\n본문",
    })
    found = lint.check_frontmatter(wikilib.load_pages(root))
    assert [f.code for f in found] == ["frontmatter"]
    assert "tags" in found[0].message and "sources" in found[0].message
    assert "updated" not in found[0].message


def test_empty_inline_list_is_violation_for_concepts(make_repo):
    root = make_repo({"wiki/pages/concepts/개념.md": page(sources="[]")})
    found = lint.check_frontmatter(wikilib.load_pages(root))
    assert "sources" in found[0].message


def test_declared_source_must_exist(make_repo):
    root = make_repo({
        "wiki/pages/concepts/개념.md": page(sources="[src-존재하지않음, 아무말]"),
    })
    found = lint.check_provenance(wikilib.load_pages(root))
    assert [f.code for f in found] == ["출처추적", "출처추적"]
    assert all(f.level == "violation" for f in found)
    assert "대상 없음" in found[0].message


def test_source_page_sources_field_is_not_checked_for_page_existence(make_repo):
    """sources/ 페이지의 sources는 raw 원본 파일명이다. check_raw_sync가 본다."""
    root = make_repo({
        "wiki/raw/정책.md": "원본",
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
    })
    assert lint.check_provenance(wikilib.load_pages(root)) == []
