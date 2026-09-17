import json

from conftest import page

import lint
import wikilib


def test_raw_without_source_page_is_violation(make_repo):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    found = lint.check_raw_sync(root, wikilib.load_pages(root))
    assert [f.code for f in found] == ["raw정합"]
    assert "ingest 안 됨" in found[0].message


def test_source_page_without_raw_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-없는원본.md": page(sources="[없는원본.md]"),
    })
    found = lint.check_raw_sync(root, wikilib.load_pages(root))
    assert [f.code for f in found] == ["raw정합"]
    assert "대응 raw 원본 없음" in found[0].message


def test_matched_pair_passes(make_repo):
    root = make_repo({
        "wiki/raw/정책.md": "원본",
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
    })
    assert lint.check_raw_sync(root, wikilib.load_pages(root)) == []


def test_research_subfolder_is_included(make_repo):
    root = make_repo({
        "wiki/raw/research/검색결과.md": "원본",
        "wiki/pages/sources/src-검색결과.md": page(sources="[검색결과.md]"),
    })
    assert lint.check_raw_sync(root, wikilib.load_pages(root)) == []


def test_manifest_missing_file_is_skipped(make_repo):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    assert lint.check_manifest(root) == []


def test_manifest_entry_missing_is_violation(make_repo):
    root = make_repo({
        "wiki/raw/정책.md": "원본",
        "wiki/raw/.manifest.json": json.dumps({}),
    })
    found = lint.check_manifest(root)
    assert [f.code for f in found] == ["매니페스트"]
    assert "등록 안 됨" in found[0].message


def test_manifest_stale_entry_is_violation(make_repo):
    root = make_repo({
        "wiki/raw/.manifest.json": json.dumps({"wiki/raw/사라진.md": "deadbeef"}),
    })
    found = lint.check_manifest(root)
    assert [f.code for f in found] == ["매니페스트"]
    assert "파일 없음" in found[0].message
