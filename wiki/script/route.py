#!/usr/bin/env python3
"""라우팅 조회·확장 — 요청에 필요한 문서를 좁힌다.

사용: python3 wiki/script/route.py --intent <의도> [--seed <경로> ...]
종료 코드: 0 정상 / 2 저장소 루트 아님 / 3 unclear(정지) /
          4 라우팅 입력이 잘못됨 — 알 수 없는 의도, 수집된 페이지가 아닌 --seed,
            routing.json이 실재하지 않는 경로나 불완전한 expansion 블록을 담음,
            또는 routing.json 자체가 없거나 JSON으로 파싱되지 않음.
            --seed 해석 실패는 intent가 unclear(3)여도 4로 끝난다 — 정지보다 우선한다.

자연어 분류는 하지 않는다. LLM이 의도를 판정해 인자로 넘기고, 이 스크립트는
결정적인 조회와 확장만 한다(스펙 4.1). 분류를 여기 넣으면 결정적이지 않은 결과가 나온다.
"""
from __future__ import annotations

import argparse
import json
import math
import pathlib
import sys
from dataclasses import dataclass, field

import wikilib
from wikilib import Finding

ROUTING_REL = "wiki/routing.json"


class RoutingError(Exception):
    """라우팅 입력이 잘못됐다 — 알 수 없는 의도, 또는 해석되지 않는 seed."""


@dataclass(frozen=True)
class Route:
    intent: str
    required: list[str] = field(default_factory=list)
    reference: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)
    stop: bool = False
    stop_message: str | None = None


def load_routing(repo_root: pathlib.Path) -> dict:
    return json.loads((repo_root / ROUTING_REL).read_text(encoding="utf-8"))


EXPANSION_WEIGHTS = ("source_overlap", "direct_link", "shared_neighbor", "same_type")


def _validate_expansion(routing: dict) -> list[Finding]:
    """expansion 블록이 온전한지.

    expand()는 이 값들을 직접 인덱싱한다. 검사하지 않으면 KeyError가 스택 트레이스와
    함께 종료 코드 1로 새어 나가는데, 1은 이 스크립트가 문서화한 코드가 아니다.
    맵이 잘못된 다른 모든 경우와 같이 4로 끝나야 호출자가 구분할 수 있다.
    """
    out: list[Finding] = []
    exp = routing.get("expansion")
    if not isinstance(exp, dict):
        return [Finding("violation", "라우팅경로", ROUTING_REL,
                        "expansion 블록이 없다 — weights(4종)·min_score·top_k가 필요하다")]
    weights = exp.get("weights")
    if not isinstance(weights, dict):
        out.append(Finding("violation", "라우팅경로", ROUTING_REL,
                           "expansion.weights 가 없다 — "
                           f"{', '.join(EXPANSION_WEIGHTS)} 가 필요하다"))
    else:
        for name in EXPANSION_WEIGHTS:
            val = weights.get(name)
            if isinstance(val, bool) or not isinstance(val, (int, float)):
                out.append(Finding("violation", "라우팅경로", ROUTING_REL,
                                   f"expansion.weights.{name} 가 없거나 수가 아니다"))
    for key in ("min_score", "top_k"):
        val = exp.get(key)
        if isinstance(val, bool) or not isinstance(val, (int, float)):
            out.append(Finding("violation", "라우팅경로", ROUTING_REL,
                               f"expansion.{key} 가 없거나 수가 아니다"))
    return out


def validate_routing(repo_root: pathlib.Path, routing: dict) -> list[Finding]:
    """맵이 가리키는 경로가 실재하는지 + expansion 블록이 온전한지.

    경로 검사는 glob(`*`)이 든 항목을 건너뛴다.
    """
    out: list[Finding] = _validate_expansion(routing)
    seen: set[str] = set()
    buckets = [(f"intents.{n}", s) for n, s in routing.get("intents", {}).items()]
    for where, spec in buckets:
        for key in ("required", "reference"):
            for rel in spec.get(key, []):
                if "*" in rel or rel in seen:
                    continue
                seen.add(rel)
                if not (repo_root / rel).exists():
                    out.append(Finding(
                        "violation", "라우팅경로", ROUTING_REL,
                        f"{where}.{key}가 가리키는 {rel} 이(가) 없다"))
    return out


def _dedup(seq):
    out, seen = [], set()
    for x in seq:
        if x not in seen:
            seen.add(x)
            out.append(x)
    return out


def lookup(repo_root: pathlib.Path, intent: str,
           routing: dict | None = None) -> Route:
    routing = routing if routing is not None else load_routing(repo_root)
    intents = routing.get("intents", {})
    if intent not in intents:
        raise RoutingError(
            f"알 수 없는 의도 '{intent}'. 허용: {', '.join(sorted(intents))}")
    spec = intents[intent]
    if spec.get("action") == "stop":
        return Route(intent=intent, stop=True,
                     stop_message=spec.get("message", "사용자에게 확인한다"))

    required = _dedup(list(spec.get("required", [])))
    reference = _dedup(list(spec.get("reference", [])))
    reference = [r for r in reference if r not in set(required)]
    notes: list[str] = []
    if len(required) > 7:
        notes.append(
            f"required {len(required)}개 — 7개 경고선을 넘었다. 라우팅이 과한지 검토하고 "
            f"wiki/routing-misses.md에 기록한다")
    return Route(intent=intent, required=required,
                 reference=reference, notes=notes)


def build_graph(pages) -> tuple[dict[str, set[str]], dict[str, set[str]]]:
    """(링크 인접 — 무방향, 페이지별 sources 집합). 키는 저장소 상대 경로."""
    by_stem = {}
    for pg in pages:
        by_stem.setdefault(pg.stem, []).append(pg.rel)
    adj: dict[str, set[str]] = {pg.rel: set() for pg in pages}
    srcs: dict[str, set[str]] = {}
    for pg in pages:
        srcs[pg.rel] = {wikilib.nfc(x).removesuffix(".md")
                        for x in wikilib.as_list(pg.fm.get("sources"))}
        for target in wikilib.find_links(pg.body):
            hits = by_stem.get(target, [])
            if len(hits) != 1:      # 없거나 모호하면 링크로 치지 않는다 (lint가 위반으로 잡는다)
                continue
            other = hits[0]
            if other == pg.rel:
                continue
            adj[pg.rel].add(other)
            adj[other].add(pg.rel)
    return adj, srcs


def resolve_seeds(pages, seeds: list[str]) -> list[str]:
    """seed를 수집된 페이지의 저장소 상대 경로로 확정한다. 못 찾으면 RoutingError.

    해석되지 않는 seed를 조용히 건너뛰면 "확장 후보 없음"이 출력된다 — 확장이 돌았고
    아무것도 못 찾았다는 뜻인데, 실제로는 돌지도 않았다. 3단계는 graphify 노드명을
    `--seed`로 넘기므로(노드명은 경로가 아니다) 이 침묵은 반드시 밟게 된다.
    """
    canon = {wikilib.nfc(pg.rel): pg.rel for pg in pages}
    out = []
    for seed in seeds:
        hit = canon.get(wikilib.nfc(seed))
        if hit is None:
            raise RoutingError(
                f"--seed '{seed}' 가 수집된 페이지가 아니다. 저장소 상대 경로로 쓴다 "
                f"(예: wiki/pages/concepts/<페이지>.md). graphify 노드명은 "
                f"경로가 아니므로 그대로 넘기지 않는다")
        out.append(hit)
    return out


def _source_overlap(seed: str, cand: str, srcs: dict[str, set[str]],
                    stems: dict[str, str]) -> bool:
    """두 페이지가 같은 원본에 매여 있는가.

    `sources[]`는 디렉토리마다 뜻이 다르다(conventions.md §6). `sources/` 페이지는
    raw 원본 파일명을, `concepts/`·`entities/` 페이지는 `src-` 페이지 stem을 담는다.
    두 집합을 그대로 교차하면 서로 다른 이름공간이라 영영 겹치지 않는다 — ingest가
    `sources/src-X.md`를 seed로 쓰는 경우(스펙 4.4)에 가중치 4가 통째로 죽는다.
    그래서 "한쪽의 sources[]가 상대 페이지의 stem을 담고 있다"를 함께 본다. 이 절은
    대칭이라 소스 페이지에서 seed해도, 개념 페이지에서 seed해도 같은 판정이 나온다.

    선언(`sources[]`)만 본다. 본문이 [[src-X]]를 인용만 하고 선언하지 않은 경우는
    check_provenance가 위반으로 잡는 상태다. 여기서 점수를 주면 그 위반을 덮는다.
    """
    a, b = srcs.get(seed, set()), srcs.get(cand, set())
    if a & b:
        return True
    return stems.get(cand, "") in a or stems.get(seed, "") in b


def score_candidates(pages, seeds: list[str], weights: dict) -> dict[str, float]:
    """seed 각각에 대한 점수를 합산한다. 1홉만 본다.

    seeds는 이미 resolve_seeds()로 해석된 저장소 상대 경로여야 한다 — 이 함수는
    재해석하지 않는다. 호출자(expand())가 한 번 해석한 결과를 그대로 넘긴다;
    여기서 다시 부르면 같은 seed를 매 확장마다 두 번 해석하는 낭비가 된다.
    """
    adj, srcs = build_graph(pages)
    types = {pg.rel: pg.parent_name for pg in pages}
    stems = {pg.rel: pg.stem for pg in pages}
    seedset = set(seeds)
    scores: dict[str, float] = {}
    for seed in seeds:
        for pg in pages:
            cand = pg.rel
            if cand == seed or cand in seedset:
                continue
            s = 0.0
            if _source_overlap(seed, cand, srcs, stems):
                s += weights["source_overlap"]
            if cand in adj[seed]:
                s += weights["direct_link"]
            for shared in adj[seed] & adj.get(cand, set()):
                deg = len(adj.get(shared, ()))
                if deg >= 2:        # 공통 이웃은 정의상 차수 2 이상 — log(1+deg) > 0
                    s += weights["shared_neighbor"] / math.log(1 + deg)
            # 타입 일치는 이 seed가 다른 신호를 하나라도 낸 뒤에만 더한다. 관계 요건이
            # 없는 +1을 seed마다 쌓으면 seed 4개만으로 무관한 동일 타입 페이지가
            # min_score 4를 넘는다 — 디렉토리 이름만으로 라우팅되는 셈이다.
            # 이 신호는 관련 페이지들 사이의 tie-breaker지 그 자체로 관계가 아니다.
            if s and types.get(seed) and types.get(seed) == types.get(cand):
                s += weights["same_type"]
            if s:
                scores[cand] = scores.get(cand, 0.0) + s
    return scores


def expand(repo_root: pathlib.Path, route: Route, seeds: list[str],
           routing: dict | None = None, pages=None) -> Route:
    """4-신호 1홉 확장으로 reference를 넓힌 새 Route를 만든다."""
    routing = routing if routing is not None else load_routing(repo_root)
    exp = routing.get("expansion", {})
    notes = list(route.notes)

    if not seeds:
        notes.append("seed가 없어 출처 중복 확장을 건너뛰었다 (스펙 4.4)")
        return Route(route.intent, route.required, route.reference,
                     notes, route.stop, route.stop_message)

    pages = wikilib.load_pages(repo_root) if pages is None else pages
    seeds = resolve_seeds(pages, seeds)
    scores = score_candidates(pages, seeds, exp["weights"])
    already = set(route.required) | set(route.reference) | set(seeds)
    ranked = sorted(((s, p) for p, s in scores.items()
                     if s >= exp["min_score"] and p not in already),
                    key=lambda t: (-t[0], t[1]))[:exp["top_k"]]

    if ranked:
        notes.append("확장으로 " + str(len(ranked)) + "건 추가: "
                     + ", ".join(f"{p}({s:.1f})" for s, p in ranked))
    else:
        notes.append(f"확장 후보 없음 (min_score {exp['min_score']} 미만)")
    return Route(route.intent, route.required,
                 route.reference + [p for _, p in ranked],
                 notes, route.stop, route.stop_message)


def as_dict(route: Route) -> dict:
    return {"intent": route.intent,
            "required": route.required, "reference": route.reference,
            "notes": route.notes, "stop": route.stop,
            "stop_message": route.stop_message}


def render(route: Route) -> str:
    if route.stop:
        return f"STOP — {route.stop_message}\n읽을 문서를 산출하지 않는다."
    lines = [f"의도: {route.intent}", "",
             f"## required ({len(route.required)}) — 작업 전 반드시 읽는다"]
    lines += [f"- {r}" for r in route.required] or ["- (없음)"]
    lines += ["", f"## reference ({len(route.reference)}) — 필요할 때만 연다"]
    lines += [f"- {r}" for r in route.reference] or ["- (없음)"]
    if route.notes:
        lines += ["", "## 참고"] + [f"- {n}" for n in route.notes]
    return "\n".join(lines)


def repo_root_ok(repo_root: pathlib.Path) -> bool:
    return (repo_root / "wiki").is_dir() and (repo_root / "wiki/conventions.md").is_file()


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(add_help=True)
    ap.add_argument("--intent", required=True)
    ap.add_argument("--seed", action="append", default=[])
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args(argv)

    repo_root = pathlib.Path.cwd()
    if not repo_root_ok(repo_root):
        print(f"저장소 루트에서 실행한다. 현재 위치({repo_root})에 wiki/ 또는 "
              f"wiki/conventions.md 가 없다.", file=sys.stderr)
        return 2

    try:
        routing = load_routing(repo_root)
    except (OSError, json.JSONDecodeError) as e:
        print(f"routing.json을 읽을 수 없다 ({ROUTING_REL}): {e}\n"
              f"경로가 실재하는지, JSON 문법이 온전한지 확인한다.", file=sys.stderr)
        return 4

    bad = validate_routing(repo_root, routing)
    if bad:
        for f in bad:
            print(f"- {f}", file=sys.stderr)
        print("routing.json이 실재하지 않는 경로를 가리키거나 expansion 블록이 불완전하다.",
              file=sys.stderr)
        return 4

    try:
        route = lookup(repo_root, args.intent, routing)
        if route.stop:
            if args.seed:
                # stop 경로에서는 expand()가 아예 돌지 않는다 — 여기서 미리
                # 해석하지 않으면 잘못된 seed가 침묵 속에 3으로 끝난다(R3).
                # stop이 아닌 경로는 아래에서 expand()가 이미 한 번 해석하므로
                # 여기서 또 부르면 R4가 없앤 중복이 main·expand 사이에서
                # 되살아난다 — 그래서 이 분기는 stop일 때만 탄다.
                pages = wikilib.load_pages(repo_root)
                resolve_seeds(pages, args.seed)
        else:
            route = expand(repo_root, route, args.seed, routing)
    except RoutingError as e:
        print(str(e), file=sys.stderr)
        return 4

    if args.json:
        print(json.dumps(as_dict(route), ensure_ascii=False, indent=2))
    else:
        print(render(route))
    return 3 if route.stop else 0


if __name__ == "__main__":
    sys.exit(main())
