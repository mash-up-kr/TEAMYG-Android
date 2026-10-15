import hashlib
import json
import os
import time

import pytest
from conftest import page

import graph_signals
import wikilib


def graph(nodes, edges):
    """graphify graph.json 최소 형태."""
    return json.dumps({"nodes": nodes, "links": edges}, ensure_ascii=False)


def node(nid, src, community=0):
    return {"id": nid, "label": nid, "file_type": "concept",
            "source_file": src, "community": community}


def edge(a, b):
    return {"source": a, "target": b, "relation": "references",
            "confidence": "EXTRACTED", "confidence_score": 1.0}


def write_manifest(repo, keys=None):
    """wiki/graphify-out/manifest.json을 실제 파일 내용의 md5로 채운다.

    graphify가 추출 직후 남기는 것과 같은 형태다(`{스캔루트 상대경로:
    {mtime, ast_hash, semantic_hash}}`, 해시는 파일 바이트의 md5 — 실측 확인).
    keys를 주지 않으면 `wiki/pages/` 아래 마크다운 전부를 기록한다.
    """
    scan = repo / "wiki/pages"
    if keys is None:
        keys = sorted(p.relative_to(scan).as_posix()
                      for p in scan.rglob("*.md")) if scan.is_dir() else []
    out = {}
    for k in keys:
        p = scan / k
        h = hashlib.md5(p.read_bytes()).hexdigest()
        out[k] = {"mtime": p.stat().st_mtime, "ast_hash": h, "semantic_hash": h}
    d = repo / "wiki/graphify-out"
    d.mkdir(exist_ok=True, parents=True)
    (d / "manifest.json").write_text(json.dumps(out, ensure_ascii=False),
                                      encoding="utf-8")
    return out


def write_graph(repo, nodes, edges, manifest=True):
    """wiki/graphify-out/을 만들고 graph.json과, 짝이 되는 manifest.json을 쓴다.

    stale 판정은 mtime이 아니라 manifest.json의 내용 해시로 한다(스펙 §5.3)
    — 그래서 "그래프가 최신"인 상태를 만들려면 graph.json만으로는 부족하고
    매니페스트가 현재 페이지 내용과 일치해야 한다.
    """
    d = repo / "wiki/graphify-out"
    d.mkdir(exist_ok=True, parents=True)
    (d / "graph.json").write_text(graph(nodes, edges), encoding="utf-8")
    if manifest:
        write_manifest(repo)


def _backdate(repo, seconds=3600):
    """`wiki/pages/` 아래 모든 파일의 mtime을 과거로 민다.

    mtime 비교로는 잡히지 않는 상태를 만들어, 판정이 오로지 manifest.json의
    내용 해시에서 나온다는 것을 테스트가 실제로 강제하게 한다.
    """
    past = time.time() - seconds
    for f in (repo / "wiki/pages").rglob("*"):
        if f.is_file():
            os.utime(f, (past, past))


@pytest.fixture
def repo(make_repo):
    """페이지 4개. 브리지 검사가 4번째 커뮤니티 상대(넷)를 요구한다.

    브리지 그래프 신호(§Task 3)는 그래프에 나오는 개념의 source_file이 실제로
    로드된 페이지에 대응해야 판정이 나온다(어긋나면 그래프stale로 보고하는 것이
    이 작업의 핵심 안전장치다) — 그래서 브리지 테스트가 참조하는 4번째 페이지도
    여기 미리 만들어 둔다.
    """
    return make_repo({
        "wiki/conventions.md": "계약",
        "wiki/pages/concepts/하나.md": page(sources="[]"),
        "wiki/pages/concepts/둘.md": page(sources="[]"),
        "wiki/pages/concepts/셋.md": page(sources="[]"),
        "wiki/pages/concepts/넷.md": page(sources="[]"),
    })


# --- 브리프가 제공한 case들. 스캔 루트가 wiki/pages로 평탄화되면서 source_file
# 값은 도메인 세그먼트 없이 스캔 루트 기준 상대경로("concepts/하나.md" 등)
# 그대로 쓴다 — 원본(huihun-private-wiki)은 wiki/domains/<도메인>/concepts/...
# 배치라 도메인 세그먼트("a/")를 덧붙여야 했지만, 이 저장소는 도메인 계층이
# 없으므로 그 보정이 필요 없다.

def test_no_graph_returns_no_signals(repo):
    pages = wikilib.load_pages(repo)
    assert graph_signals.check_graph_signals(repo, pages) == []


def test_stale_graph_reports_and_skips(repo):
    """페이지 내용이 매니페스트 기록과 달라지면 stale이고 나머지 판정은 생략한다."""
    write_graph(repo, [node("n1", "concepts/하나.md")], [])
    target = repo / "wiki/pages/concepts/하나.md"
    target.write_text(page(sources="[]", body="추가된 본문"), encoding="utf-8")
    # mtime은 과거로 되돌린다 — 판정 근거가 시각이 아니라 내용 해시임을 고정한다.
    _backdate(repo)
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]
    assert found[0].level == "warning"
    assert "하나.md" in found[0].message


def test_fresh_clone_mtimes_do_not_make_the_graph_stale(repo):
    """새 클론 시뮬레이션 — 모든 페이지가 산출물보다 새 mtime이어도 stale이 아니다.

    git 체크아웃은 파일을 기록 순서대로 찍고 `wiki/` 아래에서 `graphify-out/`이
    `pages/`보다 먼저 정렬되므로, 새 클론에서는 페이지 mtime이 항상
    graph.json보다 뒤다. mtime으로 판정하면 모든 클론이 영구 stale이 되어
    그래프 신호 전체가 죽는다(B2). 내용은 그대로이므로 해시 비교는 통과해야 한다.
    """
    nodes = [node("h", "concepts/하나.md", 0), node("a", "concepts/둘.md", 0),
             node("b", "concepts/셋.md", 0)]
    write_graph(repo, nodes, [edge("h", "a"), edge("a", "b"), edge("b", "h")])
    future = time.time() + 3600
    for p in (repo / "wiki/pages").rglob("*.md"):
        os.utime(p, (future, future))
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == [], f"새 클론에서 stale이 뜨면 안 된다: {found}"


def test_added_page_absent_from_manifest_is_stale(repo):
    """그래프가 본 적 없는 새 페이지가 생기면 stale이다."""
    write_graph(repo, [node("n1", "concepts/하나.md")], [])
    (repo / "wiki/pages/concepts/다섯.md").write_text(
        page(sources="[]"), encoding="utf-8")
    # 새 페이지 mtime을 과거로 찍어 mtime 비교로는 절대 잡히지 않게 만든다.
    _backdate(repo)
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]
    assert "다섯.md" in found[0].message


def test_deleted_page_still_in_manifest_is_stale(repo):
    """매니페스트에는 있는데 파일이 사라졌으면 stale이다.

    mtime 비교로는 원리적으로 잡을 수 없던 경우다 — 없는 파일에는 mtime이
    없어서 순회 자체에서 빠졌다.
    """
    write_graph(repo, [node("n1", "concepts/하나.md")], [])
    (repo / "wiki/pages/concepts/넷.md").unlink()
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]
    assert "넷.md" in found[0].message


def test_missing_manifest_is_stale(repo):
    """graph.json은 있는데 manifest.json이 없으면 대조 근거가 없어 stale이다."""
    write_graph(repo, [node("n1", "concepts/하나.md")], [], manifest=False)
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]
    assert "manifest.json 없음" in found[0].message


def test_legacy_manifest_entry_without_hash_is_stale(repo):
    """해시가 비어 있는 항목은 대조할 근거가 없으므로 stale로 본다."""
    write_graph(repo, [node("n1", "concepts/하나.md")], [])
    mp = repo / "wiki/graphify-out/manifest.json"
    m = json.loads(mp.read_text(encoding="utf-8"))
    m["concepts/하나.md"] = {"mtime": 0, "ast_hash": "", "semantic_hash": ""}
    mp.write_text(json.dumps(m, ensure_ascii=False), encoding="utf-8")
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]


def test_scan_root_stamp_mismatch_is_reported_with_both_roots(repo):
    """`.graphify_root`가 다른 루트를 기록했으면 두 값을 다 밝히고 판정을 멈춘다."""
    write_graph(repo, [node("n1", "concepts/하나.md")], [])
    (repo / "wiki/graphify-out/.graphify_root").write_text(
        "/somewhere/else/porgy/wiki\n", encoding="utf-8")
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]
    assert "/somewhere/else/porgy/wiki" in found[0].message
    assert graph_signals.SCAN_ROOT in found[0].message


def test_scan_root_stamp_matching_tail_passes_on_any_machine(repo):
    """기록된 절대경로의 끝이 SCAN_ROOT와 같으면 통과한다(클론 위치 무관)."""
    write_graph(repo, [node("n1", "concepts/하나.md"),
                       node("n2", "concepts/둘.md")], [edge("n1", "n2")])
    (repo / "wiki/graphify-out/.graphify_root").write_text(
        "/some/other/machine/porgy/wiki/pages\n", encoding="utf-8")
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert "그래프stale" not in {f.code for f in found}


def test_scan_root_falls_back_to_constant_when_stamp_absent(repo):
    """`.graphify_root`가 없으면 상수를 그대로 쓰고 아무 경고도 내지 않는다."""
    write_graph(repo, [node("n1", "concepts/하나.md"),
                       node("n2", "concepts/둘.md")], [edge("n1", "n2")])
    assert not (repo / "wiki/graphify-out/.graphify_root").exists()
    assert graph_signals.check_scan_root(repo) is None
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert "그래프stale" not in {f.code for f in found}


def test_bridge_check_survives_nodes_without_community():
    """community가 없는 노드(None)가 섞여도 브리지 검사가 죽지 않는다.

    rollup은 `n.get("community")`를 그대로 담으므로 커뮤니티 미배정 노드가
    None으로 들어온다. 정렬 키가 int와 None으로 섞이면 TypeError가 나고
    lint 전체가 트레이스백으로 종료됐다(F4).
    """
    adj = {"p": {"a", "b", "c"}}
    comm = {"p": 0, "a": None, "b": 1, "c": 2}
    assert graph_signals._check_bridges(adj, comm) == []


def test_bridge_still_fires_when_three_assigned_communities_remain():
    """미배정 이웃을 걸러도 배정된 커뮤니티가 3개면 브리지는 그대로 뜬다."""
    adj = {"p": {"a", "b", "c", "d"}}
    comm = {"p": 0, "a": None, "b": 1, "c": 2, "d": 3}
    found = graph_signals._check_bridges(adj, comm)
    assert [f.code for f in found] == ["브리지"]


def test_rollup_counts_cross_page_edges_only(repo):
    g = json.loads(graph(
        [node("n1", "concepts/하나.md"), node("n2", "concepts/하나.md"),
         node("n3", "concepts/둘.md")],
        [edge("n1", "n2"), edge("n1", "n3")]))
    deg, adj, comm = graph_signals.rollup(g)
    # 하나.md 내부 엣지(n1-n2)는 세지 않는다
    assert deg["concepts/하나.md"] == 1
    assert adj["concepts/하나.md"] == {"concepts/둘.md"}


def test_isolated_page_is_a_warning(repo):
    write_graph(repo, [node("n1", "concepts/하나.md"), node("n2", "concepts/둘.md")], [])
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    codes = {f.code for f in found}
    assert codes == {"그래프고립"}
    assert all(f.level == "warning" for f in found)


def test_bridge_page_is_a_warning(repo):
    nodes = [node("h", "concepts/하나.md", 0),
             node("a", "concepts/둘.md", 1),
             node("b", "concepts/셋.md", 2),
             node("c", "concepts/넷.md", 3)]
    edges = [edge("h", "a"), edge("h", "b"), edge("h", "c")]
    write_graph(repo, nodes, edges)
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert any(f.code == "브리지" and "하나" in f.path for f in found)


def test_signals_are_never_violations(make_repo):
    files = {"wiki/conventions.md": "계약"}
    for i in range(5):
        files[f"wiki/pages/concepts/p{i}.md"] = page(sources="[]")
    repo = make_repo(files)
    nodes = [node("n%d" % i, "concepts/p%d.md" % i, 0) for i in range(5)]
    write_graph(repo, nodes, [])
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert found, "신호가 하나는 나와야 의미 있는 검사다"
    assert all(f.level == "warning" for f in found)


# --- 브리프에 없는 case. 실제 그래프는 6노드·2커뮤니티로 희소 커뮤니티·브리지를
# 실제로 밟아볼 만큼 크지 않다(브리프도 이를 인정한다) — 그래서 합성 그래프로
# 아래 분기를 추가로 덮는다: source_file 불일치(그래프stale 재확인), 희소
# 커뮤니티 최소 페이지 수 미만, 조밀한(경고 없는) 커뮤니티, graphify를
# import하지 않는다는 제약 자체.

def test_unresolved_source_file_is_reported_as_stale(repo):
    """그래프가 가리키는 페이지가 로드된 페이지 목록에 없으면 stale로 보고한다.

    조용히 그 개념만 빼고 계속 진행하면 빈 신호 집합이 나오는데, 이는 브리프가
    명시적으로 경고한 실패 모드다 — 건강한 위키와 구별되지 않는다.
    """
    write_graph(repo, [
        node("n1", "concepts/하나.md"),
        node("ghost", "concepts/유령.md"),  # 저장소에 없는 페이지
    ], [edge("n1", "ghost")])
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert [f.code for f in found] == ["그래프stale"]
    assert found[0].level == "warning"


def test_sparse_community_requires_at_least_three_pages(make_repo):
    """페이지 2개짜리 커뮤니티는 밀도가 0이어도 희소 커뮤니티로 판정하지 않는다."""
    repo = make_repo({
        "wiki/conventions.md": "계약",
        "wiki/pages/concepts/하나.md": page(sources="[]"),
        "wiki/pages/concepts/둘.md": page(sources="[]"),
    })
    write_graph(repo, [
        node("n1", "concepts/하나.md", 0),
        node("n2", "concepts/둘.md", 0),
    ], [])
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    codes = {f.code for f in found}
    assert "희소커뮤니티" not in codes
    assert codes == {"그래프고립"}


def test_densely_connected_community_has_no_signals(repo):
    """3페이지가 완전 연결(삼각형)이면 고립도 희소 커뮤니티도 뜨지 않는다."""
    nodes = [node("h", "concepts/하나.md", 0),
             node("a", "concepts/둘.md", 0),
             node("b", "concepts/셋.md", 0)]
    edges = [edge("h", "a"), edge("a", "b"), edge("b", "h")]
    write_graph(repo, nodes, edges)
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert found == []


def test_sparse_community_is_a_warning(make_repo):
    """4페이지가 같은 커뮤니티인데 엣지가 없으면(밀도 0 < 0.15) 희소 커뮤니티다."""
    files = {"wiki/conventions.md": "계약"}
    for i in range(4):
        files[f"wiki/pages/concepts/p{i}.md"] = page(sources="[]")
    repo = make_repo(files)
    nodes = [node("n%d" % i, "concepts/p%d.md" % i, 7) for i in range(4)]
    write_graph(repo, nodes, [])
    pages = wikilib.load_pages(repo)
    found = graph_signals.check_graph_signals(repo, pages)
    assert any(f.code == "희소커뮤니티" for f in found)
    assert all(f.level == "warning" for f in found)


def test_graph_signals_does_not_import_graphify():
    """wiki/graphify-out/graph.json만 읽는다 — graphify 설치 여부와 무관하게 동작해야 한다."""
    import inspect
    src = inspect.getsource(graph_signals)
    assert "import graphify" not in src
    assert "from graphify" not in src


def test_load_graph_returns_none_when_missing(repo):
    assert graph_signals.load_graph(repo) is None


def test_missing_graph_is_silent(make_repo):
    """graph.json이 없는 저장소는 정상 상태다. 경고를 내면 첫 ingest 전까지 노이즈다."""
    root = make_repo({"wiki/pages/concepts/개념.md": page(sources="[]")})
    assert graph_signals.check_graph_signals(root, wikilib.load_pages(root)) == []
