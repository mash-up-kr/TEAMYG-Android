import json

import pytest

import route


@pytest.fixture
def repo(make_repo):
    """routing.json과 그것이 가리키는 파일이 모두 실재하는 최소 저장소."""
    from conftest import page
    routing = {
        "version": 1,
        "intents": {
            "ingest": {"required": ["wiki/conventions.md"],
                       "reference": ["wiki/references/ingest-checklist.md"]},
            "query": {"required": ["wiki/open-questions.md"], "reference": []},
            "unclear": {"action": "stop", "message": "의도를 사용자에게 확인한다"},
        },
        "expansion": {"weights": {"source_overlap": 4, "direct_link": 3,
                                  "shared_neighbor": 1.5, "same_type": 1},
                      "min_score": 4, "top_k": 5, "hops": 1},
    }
    return make_repo({
        "wiki/conventions.md": "계약",
        "wiki/routing.json": json.dumps(routing, ensure_ascii=False),
        "wiki/references/ingest-checklist.md": "체크리스트",
        "wiki/open-questions.md": page(),
        "wiki/pages/purpose.md": page(),
        "wiki/pages/index.md": page(),
        "wiki/pages/overview.md": page(),
    })


def test_lookup_returns_intent_required(repo):
    r = route.lookup(repo, "query")
    assert r.required == ["wiki/open-questions.md"]
    assert r.reference == []


def test_lookup_deduplicates_preserving_order(repo):
    d = json.loads((repo / "wiki" / "routing.json").read_text())
    d["intents"]["ingest"]["required"] = [
        "wiki/conventions.md", "wiki/references/ingest-checklist.md",
        "wiki/conventions.md"]
    (repo / "wiki" / "routing.json").write_text(json.dumps(d, ensure_ascii=False))
    r = route.lookup(repo, "ingest")
    assert r.required.count("wiki/conventions.md") == 1
    assert r.required == ["wiki/conventions.md", "wiki/references/ingest-checklist.md"]


def test_unclear_stops(repo):
    r = route.lookup(repo, "unclear")
    assert r.stop is True
    assert r.required == [] and r.reference == []
    assert "확인" in r.stop_message


def test_validate_routing_flags_missing_path(repo):
    d = json.loads((repo / "wiki" / "routing.json").read_text())
    d["intents"]["query"]["required"].append("wiki/없는파일.md")
    (repo / "wiki" / "routing.json").write_text(json.dumps(d, ensure_ascii=False))
    found = route.validate_routing(repo, route.load_routing(repo))
    assert [f.code for f in found] == ["라우팅경로"]
    assert "wiki/없는파일.md" in found[0].message


def test_validate_routing_clean_repo(repo):
    assert route.validate_routing(repo, route.load_routing(repo)) == []


def test_main_exit_codes(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    assert route.main(["--intent", "ingest"]) == 0
    assert "wiki/conventions.md" in capsys.readouterr().out
    assert route.main(["--intent", "unclear"]) == 3
    assert route.main(["--intent", "없는의도"]) == 4


def test_main_wrong_root_exits_2(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo / "wiki" / "pages")
    assert route.main(["--intent", "query"]) == 2
    assert "저장소 루트" in capsys.readouterr().err


def test_main_json_output(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    route.main(["--intent", "query", "--json"])
    d = json.loads(capsys.readouterr().out)
    assert d["intent"] == "query"
    assert d["required"] == ["wiki/open-questions.md"]
    assert d["stop"] is False


# ── R1: 깨진 routing.json은 트레이스백이 아니라 exit 4다 ─────────────────────

def test_main_exits_4_on_broken_json(repo, monkeypatch, capsys):
    (repo / "wiki" / "routing.json").write_text("{이것은 json이 아니다", encoding="utf-8")
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "query"])
    err = capsys.readouterr().err
    assert code == 4
    assert err.strip()  # 트레이스백이 아니라 사람이 읽을 메시지


def test_main_exits_4_on_missing_routing_file(repo, monkeypatch, capsys):
    (repo / "wiki" / "routing.json").unlink()
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "query"])
    err = capsys.readouterr().err
    assert code == 4
    assert err.strip()


# ── R2: min_score: true는 bool이라 int 서브클래스 검사를 통과해선 안 된다 ────

def test_validate_expansion_rejects_bool_min_score(repo):
    d = json.loads((repo / "wiki" / "routing.json").read_text(encoding="utf-8"))
    d["expansion"]["min_score"] = True
    found = route._validate_expansion(d)
    assert found and all(f.code == "라우팅경로" for f in found)


def test_main_exits_4_on_bool_min_score(repo, monkeypatch, capsys):
    d = json.loads((repo / "wiki" / "routing.json").read_text(encoding="utf-8"))
    d["expansion"]["min_score"] = True
    (repo / "wiki" / "routing.json").write_text(json.dumps(d, ensure_ascii=False),
                                                encoding="utf-8")
    monkeypatch.chdir(repo)
    assert route.main(["--intent", "query"]) == 4


# ── R3: unclear도 --seed를 검증한다. 정지보다 잘못된 seed가 우선이다 ────────

def test_main_unclear_with_bad_seed_exits_4_not_3(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "unclear", "--seed", "없는페이지"])
    err = capsys.readouterr().err
    assert code == 4
    assert "없는페이지" in err


def test_main_normal_intent_with_bad_seed_exits_4(repo, monkeypatch, capsys):
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "query", "--seed", "없는페이지"])
    err = capsys.readouterr().err
    assert code == 4
    assert "없는페이지" in err


def test_main_resolves_valid_seed_only_once_on_normal_path(repo, monkeypatch, capsys):
    """정상 intent + 유효한 seed에서 resolve_seeds가 main·expand 사이에서 또
    중복되면 안 된다 — R4가 없앤 중복을 R3가 다른 경로로 되살리지 않는지 고정한다.
    """
    calls = []
    orig = route.resolve_seeds

    def spy(pages, seeds):
        calls.append(list(seeds))
        return orig(pages, seeds)

    monkeypatch.setattr(route, "resolve_seeds", spy)
    monkeypatch.chdir(repo)
    code = route.main(["--intent", "query", "--seed", "wiki/pages/purpose.md"])
    assert code == 0
    assert len(calls) == 1


def test_unknown_intent_raises(make_repo):
    root = make_repo({"wiki/conventions.md": "# contract"})
    routing = {"intents": {"lint": {"required": ["wiki/conventions.md"]}}}
    with pytest.raises(route.RoutingError) as e:
        route.lookup(root, "add-domain", routing)
    assert "알 수 없는 의도" in str(e.value)


def test_route_has_no_domain_field():
    r = route.Route(intent="lint")
    assert not hasattr(r, "domain")
    assert "domain" not in route.as_dict(r)
