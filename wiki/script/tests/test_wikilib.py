import pathlib

from conftest import page

import wikilib


def test_split_frontmatter_returns_none_when_absent():
    fm, body = wikilib.split_frontmatter("# 제목\n본문")
    assert fm is None
    assert body == "# 제목\n본문"


def test_split_frontmatter_extracts_block():
    fm, body = wikilib.split_frontmatter("---\ntags: [a]\n---\n\n본문")
    assert fm == "tags: [a]"
    assert body.strip() == "본문"


def test_split_frontmatter_unterminated_is_none():
    fm, _ = wikilib.split_frontmatter("---\ntags: [a]\n본문")
    assert fm is None


def test_parse_fm_reads_scalar_and_list():
    fm = wikilib.parse_fm("tags: [연구, 개인]\nupdated: 2026-08-10\nstatus: current")
    assert fm["tags"] == ["연구", "개인"]
    assert fm["updated"] == "2026-08-10"
    assert fm["status"] == "current"


def test_strip_noise_removes_comments_and_fences():
    t = "앞 <!-- [[숨은링크]] --> 뒤\n```\n[[펜스링크]]\n```\n끝"
    out = wikilib.strip_noise(t)
    assert "숨은링크" not in out
    assert "펜스링크" not in out
    assert "앞" in out and "끝" in out


def test_find_links_handles_alias_and_anchor():
    links = wikilib.find_links("[[개념]] [[개념|별칭]] [[개념#절]]")
    assert links == ["개념", "개념", "개념"]


def test_load_pages_skips_uncollected(make_repo):
    root = make_repo({
        "wiki/pages/index.md": page(body="[[a-index]]"),
        "wiki/CLAUDE.md": "지시문",
        "wiki/conventions.md": "계약",
        "wiki/templates/concept.md": "템플릿",
        "wiki/script/lint.py": "코드",
        "wiki/references/lint-rules.md": "참고",
        "wiki/routing-misses.md": "기록",
        "wiki/pages/concepts/x.md": page(),
        "docs/llm-wiki.md": "원리",
    })
    rels = {str(p.path) for p in wikilib.load_pages(root)}
    assert rels == {"wiki/pages/index.md", "wiki/pages/concepts/x.md"}


def test_load_pages_marks_missing_frontmatter(make_repo):
    root = make_repo({"wiki/pages/concepts/x.md": "frontmatter 없음"})
    pg = wikilib.load_pages(root)[0]
    assert pg.has_fm is False
    assert pg.fm == {}


def test_build_name_index_groups_duplicates(make_repo):
    root = make_repo({
        "wiki/pages/concepts/토핑.md": page(),
        "wiki/pages/entities/토핑.md": page(),
    })
    idx = wikilib.build_name_index(wikilib.load_pages(root))
    assert len(idx["토핑"]) == 2


def test_nfc_normalizes_korean(make_repo):
    decomposed = "가"  # ㄱ + ㅏ
    assert wikilib.nfc(decomposed) == "가"


BLOCK_FM = "tags:\n  - t\nupdated: 2026-08-10\nsources:\n  - src-정책"


def test_parse_fm_reads_block_list():
    fm = wikilib.parse_fm(BLOCK_FM)
    assert fm["tags"] == ["t"]
    assert fm["sources"] == ["src-정책"]
    assert fm["updated"] == "2026-08-10"


def test_parse_fm_block_and_inline_agree():
    block = wikilib.parse_fm("tags:\n  - 연구\n  - 개인")
    inline = wikilib.parse_fm("tags: [연구, 개인]")
    assert block == inline == {"tags": ["연구", "개인"]}


def test_parse_fm_mixed_block_and_inline_keys():
    fm = wikilib.parse_fm(
        "tags:\n  - 연구\nsources: [src-가, src-나]\nstatus: current")
    assert fm == {"tags": ["연구"], "sources": ["src-가", "src-나"],
                  "status": "current"}


def test_parse_fm_empty_value_stays_empty_string():
    fm = wikilib.parse_fm("tags:\nupdated: 2026-08-10")
    assert fm["tags"] == ""
    assert fm["updated"] == "2026-08-10"


def test_parse_fm_block_list_keeps_korean_items():
    fm = wikilib.parse_fm("sources:\n  - src-개인정보보호법\n  - src-정책-v2")
    assert fm["sources"] == ["src-개인정보보호법", "src-정책-v2"]
    assert all(x == wikilib.nfc(x) for x in fm["sources"])


def test_as_list_treats_empty_value_as_empty():
    assert wikilib.as_list("") == []
    assert wikilib.as_list(None) == []
    assert wikilib.as_list([""]) == []
    assert wikilib.as_list("src-가") == ["src-가"]
    assert wikilib.as_list(["src-가"]) == ["src-가"]


def test_all_markdown_skips_scratch_dirs(make_repo):
    root = make_repo({
        "wiki/pages/index.md": page(),
        "wiki/.pytest_cache/README.md": "부산물",
        "wiki/.superpowers/sdd/메모.md": "부산물",
    })
    rels = {p.relative_to(root).as_posix() for p in wikilib.all_markdown(root)}
    assert rels == {"wiki/pages/index.md"}


def test_finding_lives_in_wikilib():
    f = wikilib.Finding("violation", "코드", "wiki/x.md", "메시지")
    assert (f.level, f.code, f.path, f.message) == ("violation", "코드", "wiki/x.md", "메시지")
    assert "[위반/코드]" in str(f)
    assert "[경고/코드]" in str(wikilib.Finding("warning", "코드", "wiki/x.md", "메시지"))


def test_finding_is_frozen():
    import dataclasses
    f = wikilib.Finding("violation", "코드", "wiki/x.md", "메시지")
    try:
        f.level = "warning"
    except dataclasses.FrozenInstanceError:
        return
    raise AssertionError("Finding은 frozen이어야 한다")


def test_page_has_no_domain_attribute(make_repo):
    """도메인 차원은 제거됐다. 남아 있으면 소비자가 조용히 옛 구조를 가정한다."""
    root = make_repo({"wiki/pages/concepts/개념.md": page(sources="[]")})
    pg = wikilib.load_pages(root)[0]
    assert not hasattr(pg, "domain")


def test_all_markdown_is_scoped_to_wiki(make_repo):
    """저장소의 안드로이드 쪽 마크다운은 [[이름]] 링크 대상이 아니다."""
    root = make_repo({
        "README.md": "# root",
        "http/README.md": "# http",
        "wiki/pages/concepts/개념.md": page(sources="[]"),
    })
    rels = {p.relative_to(root).as_posix() for p in wikilib.all_markdown(root)}
    assert rels == {"wiki/pages/concepts/개념.md"}


def test_graphify_output_is_not_collected(make_repo):
    """생성물이 위키 페이지로 수집되면 파일명중복·고아 검사에 자기가 걸린다."""
    root = make_repo({
        "wiki/graphify-out/GRAPH_REPORT.md": "# report",
        "wiki/pages/concepts/개념.md": page(sources="[]"),
    })
    assert [pg.rel for pg in wikilib.load_pages(root)] == [
        "wiki/pages/concepts/개념.md"]
    assert wikilib.all_markdown(root) == [
        root / "wiki/pages/concepts/개념.md"]


def test_wiki_entry_files_are_not_collected(make_repo):
    root = make_repo({
        "wiki/CLAUDE.md": "# ops",
        "wiki/conventions.md": "# contract",
        "wiki/routing-misses.md": "# misses",
        "wiki/open-questions.md": page(),
    })
    assert [pg.rel for pg in wikilib.load_pages(root)] == ["wiki/open-questions.md"]


def test_raw_is_not_collected_but_participates_in_uniqueness(make_repo):
    """wiki/raw는 미통합 원본이다 — [[링크]] 대상이 아니므로 Page로 수집되면

    안 된다(고아 위반을 만든다). 하지만 파일명 유일성 검사(all_markdown)는
    받아야 한다 — sources/의 src- 접두사와의 충돌을 계속 잡아야 하기 때문이다.
    """
    root = make_repo({
        "wiki/raw/정책.md": "원본",
        "wiki/pages/concepts/개념.md": page(sources="[]"),
    })
    assert [pg.rel for pg in wikilib.load_pages(root)] == [
        "wiki/pages/concepts/개념.md"]
    rels = {p.relative_to(root).as_posix() for p in wikilib.all_markdown(root)}
    assert "wiki/raw/정책.md" in rels
