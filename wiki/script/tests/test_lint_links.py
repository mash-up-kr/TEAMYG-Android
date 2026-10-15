from conftest import page

import lint
import wikilib


def _run(root):
    pages = wikilib.load_pages(root)
    index = wikilib.build_name_index(pages)
    return lint.check_links(pages, index)


def test_broken_link_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/x.md": page(sources="[]", body="[[없는페이지]]"),
    })
    found, _ = _run(root)
    assert [f.code for f in found] == ["깨진링크"]
    assert found[0].level == "violation"


def test_ambiguous_link_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/토핑.md": page(sources="[]"),
        "wiki/pages/entities/토핑.md": page(sources="[]"),
        "wiki/pages/index.md": page(body="[[토핑]]"),
    })
    found, _ = _run(root)
    assert "링크모호" in [f.code for f in found]


def test_path_prefixed_link_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/토핑.md": page(sources="[]"),
        "wiki/pages/index.md": page(body="[[concepts/토핑]]"),
    })
    found, _ = _run(root)
    assert "경로링크" in [f.code for f in found]


def test_inbound_is_counted_per_target(make_repo):
    root = make_repo({
        "wiki/pages/concepts/개념.md": page(sources="[]"),
        "wiki/pages/index.md": page(body="[[개념]]"),
    })
    _, inbound = _run(root)
    assert inbound["개념"] == {"index"}


def test_orphan_page_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/외톨이.md": page(sources="[]"),
    })
    pages = wikilib.load_pages(root)
    _, inbound = _run(root)
    found = lint.check_orphans(pages, inbound)
    assert [f.code for f in found] == ["고아"]


def test_hub_files_and_queries_are_orphan_exempt(make_repo):
    root = make_repo({
        "wiki/pages/index.md": page(),
        "wiki/log.md": "# 로그",
        "wiki/pages/purpose.md": page(),
        "wiki/pages/overview.md": page(),
        "wiki/pages/queries/질의-2026-08-10.md": page(sources="[]"),
    })
    pages = wikilib.load_pages(root)
    _, inbound = _run(root)
    assert lint.check_orphans(pages, inbound) == []


def test_single_inbound_is_warning_not_violation(make_repo):
    root = make_repo({
        "wiki/pages/concepts/약한개념.md": page(sources="[]"),
        "wiki/pages/index.md": page(body="[[약한개념]]"),
    })
    pages = wikilib.load_pages(root)
    _, inbound = _run(root)
    found = lint.check_orphans(pages, inbound)
    weak = [f for f in found if f.code == "약연결"]
    assert len(weak) == 1
    assert weak[0].level == "warning"


def test_structure_files_are_exempt_from_weak_link_warning(make_repo):
    root = make_repo({
        "wiki/pages/index.md": page(body="[[purpose]] [[overview]]"),
        "wiki/pages/purpose.md": page(),
        "wiki/pages/overview.md": page(),
    })
    pages = wikilib.load_pages(root)
    _, inbound = _run(root)
    assert lint.check_orphans(pages, inbound) == []
