import json
import math

import pytest
from conftest import page

import route
import wikilib

WEIGHTS = {"source_overlap": 4, "direct_link": 3, "shared_neighbor": 1.5, "same_type": 1}


@pytest.fixture
def repo(make_repo):
    routing = {
        "version": 1,
        "intents": {"query": {"required": ["wiki/pages/index.md"], "reference": []}},
        "expansion": {"weights": {"source_overlap": 4, "direct_link": 3,
                                  "shared_neighbor": 1.5, "same_type": 1},
                      "min_score": 4, "top_k": 5, "hops": 1},
    }
    return make_repo({
        "wiki/conventions.md": "계약",
        "wiki/routing.json": json.dumps(routing, ensure_ascii=False),
        "wiki/pages/index.md": page(),
        # seed
        "wiki/pages/concepts/씨앗.md": page(sources="[src-정책]", body="[[이웃]]"),
        # 출처 중복만 (링크 없음) — 4점
        "wiki/pages/concepts/같은출처.md": page(sources="[src-정책]"),
        # 직접 링크 + 타입 일치 — 3 + 1 = 4점
        "wiki/pages/concepts/이웃.md": page(sources="[src-다른것]"),
        # 아무 관계 없음 — 0점 + 타입 1점
        "wiki/pages/concepts/무관.md": page(sources="[src-무관]"),
        "wiki/pages/sources/src-정책.md": page(sources="[정책.md]"),
        "wiki/pages/sources/src-다른것.md": page(sources="[다른것.md]"),
        "wiki/pages/sources/src-무관.md": page(sources="[무관.md]"),
    })


def test_no_seed_skips_expansion(repo):
    r = route.lookup(repo, "query")
    out = route.expand(repo, r, seeds=[])
    assert out.reference == r.reference
    assert any("seed" in n for n in out.notes)


def test_source_overlap_scores_four(repo):
    pages = wikilib.load_pages(repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/concepts/씨앗.md"],
        {"source_overlap": 4, "direct_link": 3, "shared_neighbor": 1.5, "same_type": 1})
    assert scores["wiki/pages/concepts/같은출처.md"] >= 4


def test_direct_link_and_type_reach_cutoff(repo):
    pages = wikilib.load_pages(repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/concepts/씨앗.md"],
        {"source_overlap": 4, "direct_link": 3, "shared_neighbor": 1.5, "same_type": 1})
    assert scores["wiki/pages/concepts/이웃.md"] >= 4


def test_unrelated_page_below_cutoff(repo):
    pages = wikilib.load_pages(repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/concepts/씨앗.md"],
        {"source_overlap": 4, "direct_link": 3, "shared_neighbor": 1.5, "same_type": 1})
    assert scores.get("wiki/pages/concepts/무관.md", 0) < 4


def test_expand_adds_to_reference_only(repo):
    r = route.lookup(repo, "query")
    out = route.expand(repo, r, seeds=["wiki/pages/concepts/씨앗.md"])
    assert out.required == r.required
    assert "wiki/pages/concepts/같은출처.md" in out.reference
    assert "wiki/pages/concepts/무관.md" not in out.reference


def test_seed_itself_is_never_a_candidate(repo):
    r = route.lookup(repo, "query")
    seed = "wiki/pages/concepts/씨앗.md"
    out = route.expand(repo, r, seeds=[seed])
    assert seed not in out.reference


def test_already_required_page_is_not_re_added(repo):
    r = route.lookup(repo, "query")
    out = route.expand(repo, r, seeds=["wiki/pages/concepts/씨앗.md"])
    assert "wiki/pages/index.md" not in out.reference


def test_top_k_caps_the_result(repo):
    r = route.lookup(repo, "query")
    routing = route.load_routing(repo)
    routing["expansion"]["top_k"] = 1
    out = route.expand(repo, r, seeds=["wiki/pages/concepts/씨앗.md"], routing=routing)
    added = [x for x in out.reference if x not in r.reference]
    assert len(added) == 1


@pytest.fixture
def degree_repo(make_repo):
    """공통 이웃 하나만으로 연결된 두 후보. 이웃의 차수만 다르다."""
    routing = {
        "version": 1, "intents": {"query": {"required": [], "reference": []}},
        "expansion": {"weights": WEIGHTS, "min_score": 4, "top_k": 5, "hops": 1},
    }
    files = {
        "wiki/conventions.md": "계약",
        "wiki/routing.json": json.dumps(routing, ensure_ascii=False),
        # seed는 두 이웃을 모두 링크한다
        "wiki/pages/concepts/씨앗.md": page(sources="[src-씨앗]",
                                          body="[[저차수이웃]] [[고차수이웃]]"),
        # 저차수이웃: 씨앗과 저차수경유만 인접 — 차수 2
        "wiki/pages/concepts/저차수이웃.md": page(sources="[src-저차수이웃]"),
        "wiki/pages/concepts/저차수경유.md": page(sources="[src-저차수경유]",
                                              body="[[저차수이웃]]"),
        # 고차수이웃: 씨앗·고차수경유 + 군더더기 8개 — 차수 10
        "wiki/pages/concepts/고차수이웃.md": page(sources="[src-고차수이웃]"),
        "wiki/pages/concepts/고차수경유.md": page(sources="[src-고차수경유]",
                                              body="[[고차수이웃]]"),
    }
    for i in range(8):
        files[f"wiki/pages/concepts/군더더기{i}.md"] = page(
            sources=f"[src-군더더기{i}]", body="[[고차수이웃]]")
    return make_repo(files)


def test_shared_neighbor_decays_with_degree(degree_repo):
    """차수 높은 이웃을 공유한 후보가 더 낮은 점수를 받는다: 1.5 / log(1 + deg).

    수식을 그대로 다시 쓰지 않는다 — 그러면 score_candidates에 공통 이웃 항이
    아예 없어도 통과한다. 실제 출력 두 개를 비교한다.
    """
    pages = wikilib.load_pages(degree_repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/concepts/씨앗.md"], WEIGHTS)
    low = scores["wiki/pages/concepts/저차수경유.md"]
    high = scores["wiki/pages/concepts/고차수경유.md"]
    assert low > high > 0
    # 둘의 차이는 공통 이웃 항에서만 온다 (둘 다 타입 일치 1점, 직접 링크·출처 중복 없음)
    assert low - high == pytest.approx(
        WEIGHTS["shared_neighbor"] * (1 / math.log(1 + 2) - 1 / math.log(1 + 10)))


def test_expand_notes_report_what_was_added(repo):
    r = route.lookup(repo, "query")
    out = route.expand(repo, r, seeds=["wiki/pages/concepts/씨앗.md"])
    assert any("확장" in n for n in out.notes)


def test_main_with_seed_runs_expansion(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "query",
                       "--seed", "wiki/pages/concepts/씨앗.md", "--json"])
    d = json.loads(capsys.readouterr().out)
    assert code == 0
    assert "wiki/pages/concepts/같은출처.md" in d["reference"]


# ── F1: 타입 일치는 관계가 아니라 tie-breaker다 ──────────────────────────────

@pytest.fixture
def same_type_repo(make_repo):
    """같은 타입 디렉토리에 seed 여섯 개. 후보는 그중 어느 것과도 관계가 없다."""
    routing = {
        "version": 1, "intents": {"query": {"required": [], "reference": []}},
        "expansion": {"weights": WEIGHTS, "min_score": 4, "top_k": 5, "hops": 1},
    }
    files = {
        "wiki/conventions.md": "계약",
        "wiki/routing.json": json.dumps(routing, ensure_ascii=False),
        # 관계없는 후보 — 링크도 공유 출처도 없고 디렉토리 이름만 같다
        "wiki/pages/concepts/남.md": page(sources="[src-남]"),
        "wiki/pages/sources/src-남.md": page(sources="[남.md]"),
    }
    for i in range(6):
        files[f"wiki/pages/concepts/씨앗{i}.md"] = page(sources=f"[src-씨앗{i}]")
        files[f"wiki/pages/sources/src-씨앗{i}.md"] = page(sources=f"[씨앗{i}.md]")
    return make_repo(files)


@pytest.mark.parametrize("n", [1, 2, 4, 6])
def test_same_type_alone_never_reaches_cutoff(same_type_repo, n):
    """seed를 몇 개 쌓아도 타입만 같은 페이지는 절단선을 넘지 못한다.

    합산이 seed마다 이뤄지므로 관계 요건이 없으면 N개 seed가 N점을 만든다.
    """
    pages = wikilib.load_pages(same_type_repo)
    seeds = [f"wiki/pages/concepts/씨앗{i}.md" for i in range(n)]
    scores = route.score_candidates(pages, seeds, WEIGHTS)
    assert scores.get("wiki/pages/concepts/남.md", 0) < 4


def test_same_type_alone_is_not_even_scored(same_type_repo):
    pages = wikilib.load_pages(same_type_repo)
    seeds = [f"wiki/pages/concepts/씨앗{i}.md" for i in range(6)]
    scores = route.score_candidates(pages, seeds, WEIGHTS)
    assert "wiki/pages/concepts/남.md" not in scores


@pytest.fixture
def tiebreak_repo(make_repo):
    """seed가 직접 링크한 두 페이지. 하나는 타입이 같고 하나는 다르다."""
    routing = {
        "version": 1, "intents": {"query": {"required": [], "reference": []}},
        "expansion": {"weights": WEIGHTS, "min_score": 4, "top_k": 5, "hops": 1},
    }
    return make_repo({
        "wiki/conventions.md": "계약",
        "wiki/routing.json": json.dumps(routing, ensure_ascii=False),
        "wiki/pages/concepts/씨앗.md": page(sources="[src-씨앗]",
                                          body="[[같은타입]] [[다른타입]]"),
        "wiki/pages/concepts/같은타입.md": page(sources="[src-같은타입]"),
        "wiki/pages/entities/다른타입.md": page(sources="[src-다른타입]"),
    })


def test_same_type_still_breaks_ties_among_related_pages(tiebreak_repo):
    """실제 신호(직접 링크)가 있으면 타입 일치가 그 위에 얹힌다."""
    pages = wikilib.load_pages(tiebreak_repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/concepts/씨앗.md"], WEIGHTS)
    same = scores["wiki/pages/concepts/같은타입.md"]
    other = scores["wiki/pages/entities/다른타입.md"]
    assert same > other
    assert same - other == pytest.approx(WEIGHTS["same_type"])


# ── F2: 출처 중복은 두 이름공간을 잇는다 ─────────────────────────────────────

@pytest.fixture
def ingest_repo(make_repo):
    """스펙 4.4가 이름하는 ingest seed — 대상 소스의 요약 페이지."""
    routing = {
        "version": 1,
        "intents": {"ingest": {"required": [], "reference": []}},
        "expansion": {"weights": WEIGHTS, "min_score": 4, "top_k": 5, "hops": 1},
    }
    return make_repo({
        "wiki/conventions.md": "계약",
        "wiki/routing.json": json.dumps(routing, ensure_ascii=False),
        # sources/ 페이지의 sources는 raw 원본 파일명이다
        "wiki/pages/sources/src-p.md": page(sources="[p.md]"),
        "wiki/pages/sources/src-다른것.md": page(sources="[다른것.md]"),
        # concepts/·entities/의 sources는 src- 페이지 stem이다
        "wiki/pages/concepts/개념1.md": page(sources="[src-p]", body="[[src-p]]"),
        "wiki/pages/concepts/개념2.md": page(sources="[src-p]", body="[[src-p]]"),
        "wiki/pages/entities/실체1.md": page(sources="[src-p]", body="[[src-p]]"),
        # 선언 없이 인용만 한다 — lint의 '출처추적' 위반 상태다
        "wiki/pages/concepts/인용만.md": page(sources="[src-다른것]", body="[[src-p]]"),
    })


def test_source_page_seed_surfaces_its_derived_pages(ingest_repo):
    pages = wikilib.load_pages(ingest_repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/sources/src-p.md"], WEIGHTS)
    for rel in ("wiki/pages/concepts/개념1.md",
                "wiki/pages/concepts/개념2.md",
                "wiki/pages/entities/실체1.md"):
        assert scores[rel] >= 4, rel


def test_source_overlap_is_symmetric(ingest_repo):
    """개념 페이지에서 seed해도 소스 요약 페이지가 같은 가중치를 받는다."""
    pages = wikilib.load_pages(ingest_repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/concepts/개념1.md"], WEIGHTS)
    assert scores["wiki/pages/sources/src-p.md"] >= 4


def test_citation_without_declaration_gets_no_overlap_weight(ingest_repo):
    """본문 인용만으로는 출처 중복 가중치를 주지 않는다.

    그건 check_provenance가 '출처추적' 위반으로 잡는 상태다. 여기서 보상하면 덮인다.
    직접 링크 3점만 남아 절단선 아래에 머문다.
    """
    pages = wikilib.load_pages(ingest_repo)
    scores = route.score_candidates(
        pages, ["wiki/pages/sources/src-p.md"], WEIGHTS)
    assert scores["wiki/pages/concepts/인용만.md"] == pytest.approx(
        WEIGHTS["direct_link"])


def test_ingest_expansion_returns_candidates(ingest_repo):
    r = route.lookup(ingest_repo, "ingest")
    out = route.expand(ingest_repo, r, seeds=["wiki/pages/sources/src-p.md"])
    assert "wiki/pages/concepts/개념1.md" in out.reference
    assert "wiki/pages/concepts/인용만.md" not in out.reference
    assert not any("확장 후보 없음" in n for n in out.notes)


# ── F3: expansion 블록이 망가지면 4로 끝난다 ─────────────────────────────────

def test_validate_routing_flags_missing_expansion(repo):
    d = json.loads((repo / "wiki" / "routing.json").read_text(encoding="utf-8"))
    del d["expansion"]
    (repo / "wiki" / "routing.json").write_text(json.dumps(d, ensure_ascii=False),
                                                encoding="utf-8")
    found = route.validate_routing(repo, route.load_routing(repo))
    assert [f.code for f in found] == ["라우팅경로"]
    assert "expansion" in found[0].message


@pytest.mark.parametrize("mutate", [
    lambda d: d["expansion"].pop("min_score"),
    lambda d: d["expansion"].pop("top_k"),
    lambda d: d["expansion"].pop("weights"),
    lambda d: d["expansion"]["weights"].pop("source_overlap"),
])
def test_validate_routing_flags_incomplete_expansion(repo, mutate):
    d = json.loads((repo / "wiki" / "routing.json").read_text(encoding="utf-8"))
    mutate(d)
    (repo / "wiki" / "routing.json").write_text(json.dumps(d, ensure_ascii=False),
                                                encoding="utf-8")
    found = route.validate_routing(repo, route.load_routing(repo))
    assert found and all(f.code == "라우팅경로" for f in found)


def test_main_exits_4_on_malformed_expansion(repo, monkeypatch, capsys):
    """KeyError로 1을 뱉지 않는다 — 1은 이 스크립트가 문서화한 코드가 아니다."""
    d = json.loads((repo / "wiki" / "routing.json").read_text(encoding="utf-8"))
    del d["expansion"]
    (repo / "wiki" / "routing.json").write_text(json.dumps(d, ensure_ascii=False),
                                                encoding="utf-8")
    monkeypatch.chdir(repo)
    assert route.main(["--intent", "query"]) == 4
    assert "expansion" in capsys.readouterr().err


# ── F4: 해석되지 않는 seed는 침묵이 아니라 오류다 ────────────────────────────

def test_unknown_seed_raises(repo):
    """해석은 expand()가 맡는다 — score_candidates는 이미 해석된 경로만 받는다(R4)."""
    r = route.lookup(repo, "query")
    with pytest.raises(route.RoutingError) as e:
        route.expand(repo, r, seeds=["그래프노드이름"])
    assert "그래프노드이름" in str(e.value)


def test_main_exits_4_on_unknown_seed(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "query",
                       "--seed", "wiki/pages/concepts/없는페이지.md"])
    err = capsys.readouterr().err
    assert code == 4
    assert "wiki/pages/concepts/없는페이지.md" in err


def test_unknown_seed_does_not_claim_expansion_ran(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    route.main(["--intent", "query", "--seed", "오타"])
    out = capsys.readouterr()
    assert "확장 후보 없음" not in out.out


# ── R4: resolve_seeds는 expand당 한 번만 — score_candidates는 재해석하지 않는다 ──

def test_expand_resolves_seeds_only_once(repo, monkeypatch):
    """expand()가 이미 해석한 결과를 score_candidates에 넘긴다 — 두 번 부르지 않는다.

    되돌리면(score_candidates가 다시 resolve_seeds를 부르면) 호출이 2회로 늘어
    이 테스트가 실패한다.
    """
    calls = []
    orig = route.resolve_seeds

    def spy(pages, seeds):
        calls.append(list(seeds))
        return orig(pages, seeds)

    monkeypatch.setattr(route, "resolve_seeds", spy)
    r = route.lookup(repo, "query")
    route.expand(repo, r, seeds=["wiki/pages/concepts/씨앗.md"])
    assert len(calls) == 1


def test_score_candidates_does_not_resolve_seeds_itself(repo, monkeypatch):
    """score_candidates의 시그니처는 이미 해석된 경로를 받는다 — 내부에서 재해석하지 않는다."""
    pages = wikilib.load_pages(repo)
    calls = []
    orig = route.resolve_seeds

    def spy(p, s):
        calls.append(list(s))
        return orig(p, s)

    monkeypatch.setattr(route, "resolve_seeds", spy)
    route.score_candidates(pages, ["wiki/pages/concepts/씨앗.md"], WEIGHTS)
    assert calls == []
