import json
import pathlib

import pytest

import route

REPO = pathlib.Path(__file__).resolve().parents[3]
ROUTING = REPO / "wiki" / "routing.json"

INTENTS = {"ingest", "query", "research", "delete-source",
           "lint", "schema-change", "unclear"}


def load():
    return json.loads(ROUTING.read_text(encoding="utf-8"))


def test_routing_json_exists_and_parses():
    assert ROUTING.exists()
    load()


def test_version_and_top_level_keys():
    d = load()
    assert d["version"] == 1
    assert set(d) == {"version", "intents", "expansion"}


def test_all_intents_present():
    assert set(load()["intents"]) == INTENTS


def test_unclear_is_a_stop_not_a_document_list():
    unclear = load()["intents"]["unclear"]
    assert unclear["action"] == "stop"
    assert "required" not in unclear and "reference" not in unclear


def test_query_reference_is_empty_by_design():
    # 스펙 4.2: 도메인 전체 sources를 기본 reference로 올리면
    # 4.3의 실패 조건을 기본값이 스스로 만족한다.
    assert load()["intents"]["query"]["reference"] == []


def test_every_intent_except_unclear_has_both_lists():
    for name, spec in load()["intents"].items():
        if name == "unclear":
            continue
        assert isinstance(spec["required"], list), name
        assert isinstance(spec["reference"], list), name


def test_expansion_constants_match_spec():
    e = load()["expansion"]
    assert e["weights"] == {"source_overlap": 4, "direct_link": 3,
                            "shared_neighbor": 1.5, "same_type": 1}
    assert e["min_score"] == 4
    assert e["top_k"] == 5
    assert e["hops"] == 1


def test_research_routes_to_conventions():
    """research는 raw/에 쓰는 유일한 워크플로우다 — 그 규칙은 conventions.md에 있다.

    스펙 4.2 목록에는 없는 의도적 이탈이다(스펙 4.2의 '이탈(2단계)' 참조).
    """
    assert "wiki/conventions.md" in load()["intents"]["research"]["required"]


def test_shipped_routing_json_has_seven_intents():
    repo_root = pathlib.Path(__file__).resolve().parents[3]
    routing = json.loads((repo_root / "wiki" / "routing.json")
                         .read_text(encoding="utf-8"))
    assert "domains" not in routing
    assert set(routing["intents"]) == {
        "ingest", "query", "research", "delete-source",
        "lint", "schema-change", "unclear"}


@pytest.mark.xfail(
    reason="wiki/conventions.md, wiki/references/*.md, "
           "wiki/pages/{purpose,index,overview}.md, wiki/open-questions.md 는 "
           "각각 Task 8·Task 9에서 만들어진다 — 그 전까지 validate_routing이 "
           "존재하지 않는 경로를 라우팅경로 위반으로 잡는다",
    strict=False,
)
def test_shipped_routing_json_paths_exist():
    repo_root = pathlib.Path(__file__).resolve().parents[3]
    routing = json.loads((repo_root / "wiki" / "routing.json")
                         .read_text(encoding="utf-8"))
    assert route.validate_routing(repo_root, routing) == []
