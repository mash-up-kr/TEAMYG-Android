import check_status
import wikilib


def src(status, superseded_by=None, supersedes=None, scope=None):
    lines = ["---", "tags: [test]", "updated: 2026-08-10",
             "sources: [원본.md]", f"status: {status}"]
    if superseded_by:
        lines.append(f"superseded_by: [{superseded_by}]")
    if supersedes:
        lines.append(f"supersedes: [{supersedes}]")
    if scope:
        lines.append(f"scope: {scope}")
    lines += ["---", "", "본문"]
    return "\n".join(lines)


def page_src(status, superseded_by=None, supersedes=None, scope=None):
    lines = ["---", "tags: [source]", "updated: 2026-09-17",
             "sources: [원본.md]", f"status: {status}"]
    if superseded_by is not None:
        lines.append(f"superseded_by: {superseded_by}")
    if supersedes is not None:
        lines.append(f"supersedes: {supersedes}")
    if scope is not None:
        lines.append(f"scope: {scope}")
    lines += ["---", "", "본문"]
    return "\n".join(lines)


def idx(*entries):
    body = "\n".join(entries)
    return f"---\ntags: [index]\nupdated: 2026-08-10\n---\n\n# 카탈로그\n{body}\n"


def test_missing_status_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-x.md":
            "---\ntags: [t]\nupdated: 2026-08-10\nsources: [x.md]\n---\n본문",
    })
    found = check_status.check_fields(check_status.collect_sources(root))
    assert "status 누락" in found[0].message


def test_invalid_status_value_is_violation(make_repo):
    root = make_repo({"wiki/pages/sources/src-x.md": src("최신")})
    found = check_status.check_fields(check_status.collect_sources(root))
    assert "값 오류" in found[0].message


def test_superseded_without_target_is_violation(make_repo):
    root = make_repo({"wiki/pages/sources/src-x.md": src("superseded")})
    found = check_status.check_fields(check_status.collect_sources(root))
    assert "superseded_by 없음" in found[0].message


def test_partial_without_scope_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-x.md": src("partial", superseded_by="src-y"),
        "wiki/pages/sources/src-y.md": src("current"),
    })
    found = check_status.check_fields(check_status.collect_sources(root))
    assert "scope 없음" in found[0].message


def test_current_with_superseded_by_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-x.md": src("current", superseded_by="src-y"),
        "wiki/pages/sources/src-y.md": src("current"),
    })
    found = check_status.check_fields(check_status.collect_sources(root))
    assert "current인데 superseded_by 있음" in found[0].message


def test_broken_reference_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-x.md": src("superseded", superseded_by="src-없음"),
    })
    found = check_status.check_refs(check_status.collect_sources(root))
    assert "대상 없음" in found[0].message


def test_chain_reaches_current(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("superseded", superseded_by="src-v2"),
        "wiki/pages/sources/src-v2.md": src("current", supersedes="src-v1"),
    })
    assert check_status.check_chain(check_status.collect_sources(root)) == []


def test_chain_dangling_end_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("superseded", superseded_by="src-없음"),
    })
    found = check_status.check_chain(check_status.collect_sources(root))
    assert any("current에 도달 못함" in f.message for f in found)


def test_chain_cycle_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("superseded", superseded_by="src-v2"),
        "wiki/pages/sources/src-v2.md": src("superseded", superseded_by="src-v1"),
    })
    found = check_status.check_chain(check_status.collect_sources(root))
    assert any("사이클" in f.message for f in found)


def test_reverse_consistency_violation(make_repo):
    """superseded_by만 있고 반대편 supersedes가 없는 일방적 판본 주장."""
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("superseded", superseded_by="src-v2"),
        "wiki/pages/sources/src-v2.md": src("current"),
    })
    found = check_status.check_reverse(check_status.collect_sources(root))
    assert len(found) == 1
    assert "supersedes" in found[0].message
    assert found[0].level == "violation"


def test_reverse_missing_superseded_by_is_violation(make_repo):
    """반대 방향 — supersedes만 있고 대상의 superseded_by가 없다."""
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("current"),
        "wiki/pages/sources/src-v2.md": src("current", supersedes="src-v1"),
    })
    found = check_status.check_reverse(check_status.collect_sources(root))
    assert len(found) == 1
    assert "superseded_by" in found[0].message
    assert found[0].level == "violation"


def test_reverse_bidirectional_pair_passes(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("superseded", superseded_by="src-v2"),
        "wiki/pages/sources/src-v2.md": src("current", supersedes="src-v1"),
    })
    assert check_status.check_reverse(check_status.collect_sources(root)) == []


def test_index_projection_current_needs_token(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("current"),
        "wiki/pages/index.md": idx("- [[src-v1]] — 요약"),
    })
    found = check_status.check_index_projection(root, check_status.collect_sources(root))
    assert "현행 정본" in found[0].message


def test_index_projection_passes(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("current"),
        "wiki/pages/index.md": idx("- [[src-v1]] — 요약, **현행 정본**"),
    })
    assert check_status.check_index_projection(
        root, check_status.collect_sources(root)) == []


def test_index_line_missing_is_violation(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src("current"),
        "wiki/pages/index.md": idx("- [[다른것]] — 요약"),
    })
    found = check_status.check_index_projection(root, check_status.collect_sources(root))
    assert "줄 없음" in found[0].message


def test_run_all_on_empty_repo(make_repo):
    root = make_repo({"wiki/index.md": "---\ntags: [i]\nupdated: 2026-08-10\n---\n"})
    assert check_status.run_all(root) == []


def test_index_projection_partial_needs_both_tokens(make_repo):
    """partial은 현행 범위와 대체 범위를 둘 다 표기해야 한다. 🔁만으로는 부족하다."""
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src(
            "partial", superseded_by="src-v2", scope="3장만"),
        "wiki/pages/sources/src-v2.md": src("current", supersedes="src-v1"),
        "wiki/pages/index.md": idx(
            "- [[src-v1]] — 요약, 🔁 [[src-v2]]",
            "- [[src-v2]] — 요약, **현행 정본**"),
    })
    found = check_status.check_index_projection(root, check_status.collect_sources(root))
    assert [f.level for f in found] == ["violation"]
    assert "모두 표기" in found[0].message
    assert found[0].path == "wiki/pages/sources/src-v1.md"


def test_index_projection_partial_with_both_tokens_passes(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-v1.md": src(
            "partial", superseded_by="src-v2", scope="3장만"),
        "wiki/pages/sources/src-v2.md": src("current", supersedes="src-v1"),
        "wiki/pages/index.md": idx(
            "- [[src-v1]] — 1~2장은 **현행 정본**, 3장은 🔁 [[src-v2]]",
            "- [[src-v2]] — 요약, **현행 정본**"),
    })
    assert check_status.check_index_projection(
        root, check_status.collect_sources(root)) == []


def test_block_form_superseded_by_is_read_as_list(make_repo):
    """블록 형식 리스트도 판본 참조로 읽힌다."""
    root = make_repo({
        "wiki/pages/sources/src-v1.md":
            "---\ntags:\n  - t\nupdated: 2026-08-10\nsources:\n  - 원본.md\n"
            "status: superseded\nsuperseded_by:\n  - src-v2\n---\n본문",
        "wiki/pages/sources/src-v2.md": src("current", supersedes="src-v1"),
    })
    sources = check_status.collect_sources(root)
    assert sources["src-v1"]["superseded_by"] == ["src-v2"]
    assert check_status.check_fields(sources) == []
    assert check_status.check_refs(sources) == []
    assert check_status.check_chain(sources) == []


def test_empty_superseded_by_does_not_satisfy_requirement(make_repo):
    """값이 빈 키는 '있음'이 아니다 — 예전엔 [''] 로 읽혀 통과하고 빈 메시지를 뱉었다."""
    root = make_repo({
        "wiki/pages/sources/src-v1.md":
            "---\ntags: [t]\nupdated: 2026-08-10\nsources: [원본.md]\n"
            "status: superseded\nsuperseded_by:\n---\n본문",
    })
    sources = check_status.collect_sources(root)
    found = check_status.check_fields(sources)
    assert [f.level for f in found] == ["violation"]
    assert "superseded_by 없음" in found[0].message
    assert check_status.check_refs(sources) == []


def test_main_loads_pages_only_once(make_repo, monkeypatch):
    """main()이 collect_sources와 run_all에 각각 load_pages를 시키면 안 된다(R5).

    되돌리면(main이 pages를 한 번만 읽어 두 함수에 넘기지 않으면) load_pages 호출이
    2회로 늘어 이 테스트가 실패한다.
    """
    root = make_repo({
        "wiki/conventions.md": "계약",
        "wiki/index.md": "---\ntags: [i]\nupdated: 2026-08-10\n---\n",
    })
    monkeypatch.chdir(root)
    calls = []
    orig = wikilib.load_pages

    def spy(repo_root):
        calls.append(repo_root)
        return orig(repo_root)

    monkeypatch.setattr(wikilib, "load_pages", spy)
    assert check_status.main() == 0
    assert len(calls) == 1


def test_check_status_main_exits_2_outside_repo_root(make_repo, monkeypatch, capsys):
    root = make_repo({
        "wiki/conventions.md": "계약",
        "wiki/index.md": "---\ntags: [i]\nupdated: 2026-08-10\n---\n",
    })
    monkeypatch.chdir(root / "wiki")
    code = check_status.main()
    err = capsys.readouterr().err
    assert code == 2
    assert "저장소 루트" in err


def test_check_status_main_runs_at_repo_root(make_repo, monkeypatch):
    root = make_repo({
        "wiki/conventions.md": "계약",
        "wiki/index.md": "---\ntags: [i]\nupdated: 2026-08-10\n---\n",
    })
    monkeypatch.chdir(root)
    assert check_status.main() == 0


def test_check_status_does_not_import_lint():
    import check_status
    import inspect
    src = inspect.getsource(check_status)
    assert "from lint import" not in src, "check_status가 lint를 import하면 순환이 생긴다"
    assert "import lint" not in src


def test_block_form_status_is_value_error_not_crash(make_repo):
    root = make_repo({
        "wiki/pages/sources/src-x.md":
            "---\ntags: [t]\nupdated: 2026-08-10\nsources: [x.md]\n"
            "status:\n  - current\n---\n본문",
    })
    found = check_status.check_fields(check_status.collect_sources(root))
    assert [f.level for f in found] == ["violation"]
    assert "값 오류" in found[0].message


def test_sources_are_keyed_by_stem_alone(make_repo):
    """도메인 접두사가 사라졌다. 키가 옛 형태면 체인 추적이 통째로 빗나간다."""
    root = make_repo({
        "wiki/pages/sources/src-v1.md": page_src(status="current"),
    })
    sources = check_status.collect_sources(root)
    assert set(sources) == {"src-v1"}
    assert "_domain" not in sources["src-v1"]
