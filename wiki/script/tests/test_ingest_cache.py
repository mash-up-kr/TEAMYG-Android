import hashlib
import json
import unicodedata as ud

import ingest_cache


def test_pending_lists_everything_when_no_manifest(make_repo):
    root = make_repo({"wiki/raw/정책.md": "원본", "wiki/raw/검색.md": "결과"})
    assert sorted(ingest_cache.pending(root)) == ["wiki/raw/검색.md", "wiki/raw/정책.md"]


def test_record_then_pending_is_empty(make_repo):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    ingest_cache.record(root, "wiki/raw/정책.md")
    assert ingest_cache.pending(root) == []


def test_changed_file_becomes_pending_again(make_repo):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    ingest_cache.record(root, "wiki/raw/정책.md")
    (root / "wiki/raw/정책.md").write_text("고쳐진 원본", encoding="utf-8")
    assert ingest_cache.pending(root) == ["wiki/raw/정책.md"]


def test_record_writes_real_sha256(make_repo):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    ingest_cache.record(root, "wiki/raw/정책.md")
    m = ingest_cache.load_manifest(root)
    assert m["wiki/raw/정책.md"] == hashlib.sha256("원본".encode()).hexdigest()


def test_record_is_per_source_not_batch(make_repo):
    """한 건 기록해도 나머지는 pending으로 남는다 — 중간 실패가 소스를 삼키지 않게."""
    root = make_repo({"wiki/raw/하나.md": "1", "wiki/raw/둘.md": "2", "wiki/raw/셋.md": "3"})
    ingest_cache.record(root, "wiki/raw/하나.md")
    assert sorted(ingest_cache.pending(root)) == ["wiki/raw/둘.md", "wiki/raw/셋.md"]


def test_prune_removes_vanished_entries(make_repo):
    root = make_repo({
        "wiki/raw/있음.md": "1",
        "wiki/raw/.manifest.json": json.dumps({"wiki/raw/사라짐.md": "deadbeef"}),
    })
    removed = ingest_cache.prune(root)
    assert removed == ["wiki/raw/사라짐.md"]
    assert "wiki/raw/사라짐.md" not in ingest_cache.load_manifest(root)


def test_manifest_is_sorted_and_newline_terminated(make_repo):
    root = make_repo({"wiki/raw/나중.md": "1", "wiki/raw/먼저.md": "2"})
    ingest_cache.record(root, "wiki/raw/나중.md")
    ingest_cache.record(root, "wiki/raw/먼저.md")
    text = (root / "wiki/raw/.manifest.json").read_text(encoding="utf-8")
    assert text.endswith("\n")
    assert list(json.loads(text)) == sorted(json.loads(text))


def test_manifest_keeps_lint_happy(make_repo):
    """lint.check_manifest는 매니페스트 키 집합과 wiki/raw/의 실제 md가 같기를 요구한다."""
    import lint
    root = make_repo({"wiki/raw/정책.md": "원본"})
    ingest_cache.record(root, "wiki/raw/정책.md")
    assert lint.check_manifest(root) == []


def test_main_list_and_record(make_repo, monkeypatch, capsys):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    monkeypatch.chdir(root)
    assert ingest_cache.main(["--list"]) == 0
    assert "wiki/raw/정책.md" in capsys.readouterr().out
    assert ingest_cache.main(["--record", "wiki/raw/정책.md"]) == 0
    assert ingest_cache.main(["--list"]) == 0
    assert "처리 대상 0건" in capsys.readouterr().out


def test_main_record_missing_file_exits_1(make_repo, monkeypatch):
    root = make_repo({"wiki/raw/정책.md": "원본"})
    monkeypatch.chdir(root)
    assert ingest_cache.main(["--record", "wiki/raw/없는파일.md"]) == 1


def test_main_wrong_root_exits_2(make_repo, monkeypatch):
    root = make_repo({"wiki/conventions.md": "계약", "wiki/raw/정책.md": "원본"})
    monkeypatch.chdir(root / "wiki" / "raw")
    assert ingest_cache.main(["--list"]) == 2


def test_nfd_filename_is_recorded_as_nfc(make_repo):
    """macOS가 만든 NFD 파일명도 매니페스트에는 NFC 키로 들어간다.

    정규화하지 않으면 record 직후에도 pending에 남는다 (conventions.md §4).
    """
    nfd = "wiki/raw/" + ud.normalize("NFD", "캔버스") + ".md"
    root = make_repo({nfd: "원본"})
    ingest_cache.record(root, nfd)
    assert list(ingest_cache.load_manifest(root)) == ["wiki/raw/캔버스.md"]
    assert ingest_cache.pending(root) == []


def test_nfd_file_with_nfc_manifest_is_not_pending(make_repo):
    """이미 NFC로 기록된 매니페스트는 NFD 파일을 다시 대상으로 내놓지 않는다."""
    nfd = "wiki/raw/" + ud.normalize("NFD", "캔버스") + ".md"
    root = make_repo({nfd: "원본"})
    (root / "wiki/raw/.manifest.json").write_text(
        json.dumps({"wiki/raw/캔버스.md": hashlib.sha256("원본".encode()).hexdigest()}),
        encoding="utf-8")
    assert ingest_cache.pending(root) == []


def test_prune_keeps_nfd_file_recorded_as_nfc(make_repo):
    """NFC로 기록된 항목을 NFD 파일이 실재한다는 이유로 지우지 않는다."""
    nfd = "wiki/raw/" + ud.normalize("NFD", "캔버스") + ".md"
    root = make_repo({nfd: "원본"})
    (root / "wiki/raw/.manifest.json").write_text(
        json.dumps({"wiki/raw/캔버스.md": "deadbeef"}), encoding="utf-8")
    assert ingest_cache.prune(root) == []
    assert list(ingest_cache.load_manifest(root)) == ["wiki/raw/캔버스.md"]
