#!/usr/bin/env python3
"""그래프 신호 — graphify 산출물(wiki/graphify-out/graph.json)을 lint 경고로 투영한다.

graphify의 그래프는 **개념 단위**다: 노드가 개념이고 `source_file`이 출처
페이지를 가리킨다. lint의 다른 검사는 전부 **페이지 단위**라 그 신호를 그대로
못 쓴다 — 여기서 개념 그래프를 페이지 단위로 롤업한 뒤 판정한다(스펙 §5.5).

전부 경고다. 위반이 아니다. 고립·희소 커뮤니티·브리지는 "고칠 것"이 아니라
"볼 것"이다 — 사람이 판단하고 Deep Research(Task 5)의 입력이 된다. 위반으로
만들면 그래프가 조금 흔들릴 때마다(재생성·재분류) 커밋이 막힌다.

이 모듈은 **graphify를 import하지 않는다.** wiki/graphify-out/graph.json이라는
산출물만 읽는다 — graphify가 설치돼 있지 않은 환경에서도 lint가 죽지 않아야
하기 때문이다. wikilib에서 Finding과 nfc만 가져온다.
"""
from __future__ import annotations

import hashlib
import json
import pathlib
from collections import Counter, defaultdict

from wikilib import Finding, nfc

# graphify가 노드를 스캔하는 루트. graph.json의 source_file은 이 루트 기준
# 상대경로다 — 저장소 상대경로인 Page.rel과 형태가 다르므로 그대로 비교하면 안 된다.
#
# 같은 값이 wiki/graphify-out/.graphify_root에도 (절대경로로) 기록돼 있다. 두
# 사본이 어긋나면 이 모듈의 판정이 통째로 빗나가므로 check_scan_root가 매번
# 대조한다 — 상수는 그 파일이 없을 때의 fallback이다.
SCAN_ROOT = "wiki/pages"

SPARSE_MIN_PAGES = 3
SPARSE_DENSITY_THRESHOLD = 0.15
BRIDGE_MIN_COMMUNITIES = 3


def _graph_path(repo_root: pathlib.Path) -> pathlib.Path:
    return repo_root / "wiki" / "graphify-out" / "graph.json"


def _manifest_path(repo_root: pathlib.Path) -> pathlib.Path:
    return repo_root / "wiki" / "graphify-out" / "manifest.json"


def _root_path(repo_root: pathlib.Path) -> pathlib.Path:
    return repo_root / "wiki" / "graphify-out" / ".graphify_root"


def load_graph(repo_root: pathlib.Path) -> dict | None:
    """wiki/graphify-out/graph.json을 읽는다. 파일이 없으면 None.

    파싱 실패(손상된 JSON)는 여기서 조용히 삼키지 않는다 — 예외를 그대로
    올려서 check_graph_signals가 그래프stale 경고로 번역하게 한다. 손상된
    그래프를 None(= "그래프 없음")으로 뭉개면, 그래프를 아예 만든 적 없는
    저장소(정상)와 그래프가 깨진 저장소(비정상)가 똑같이 "신호 없음"이 되어
    구분이 안 된다.
    """
    path = _graph_path(repo_root)
    if not path.exists():
        return None
    return json.loads(path.read_text(encoding="utf-8"))


def to_repo_rel(source_file: str) -> str:
    """graphify 스캔 루트(wiki/pages) 기준 상대경로 → 저장소 상대경로.

    이 변환은 이 함수 하나에만 있고, 그래프 신호 판정은 전부 이 함수를
    거친 값으로만 페이지를 찾는다. 다른 곳에서 다시 문자열을 이어붙이면 이
    함수와 미묘하게 어긋날 수 있는데, 그 결과는 예외가 아니라 "일치하는
    페이지가 없다"는 조용한 빈 신호 집합이다 — 건강한 위키와 구별되지 않는
    실패라 한 곳에 모아둔다.
    """
    return nfc(f"{SCAN_ROOT}/{source_file}")


def _md5(path: pathlib.Path) -> str:
    """graphify가 manifest에 기록하는 것과 같은 해시(파일 바이트의 md5).

    graphify의 `detect._md5_file`이 쓰는 값이다. 텍스트로 읽어 다시 인코딩하면
    개행 변환·인코딩 추정이 끼어들어 같은 파일에서도 다른 값이 나올 수 있으므로
    바이트를 그대로 읽는다.
    """
    return hashlib.md5(path.read_bytes(), usedforsecurity=False).hexdigest()


def _entry_hash(entry) -> str:
    """manifest 항목에서 내용 해시를 꺼낸다. 없으면 빈 문자열.

    graphify는 항목 형태를 세 가지로 쓴다(실측 + graphify.detect의 정규화 코드):
    `{mtime, ast_hash, semantic_hash}`(현행), `{mtime, hash}`(구형),
    그리고 mtime 하나만 담은 숫자(더 구형). 여기서 빈 문자열이 나오면 해시를
    비교할 근거가 없다는 뜻이고, graphify 자신도 그 상태를 "재추출 필요"로
    읽으므로 호출자는 stale로 취급한다.
    """
    if isinstance(entry, dict):
        return str(entry.get("ast_hash") or entry.get("hash") or "")
    return ""


def check_scan_root(repo_root: pathlib.Path) -> str | None:
    """`.graphify_root`가 기록한 스캔 루트가 SCAN_ROOT와 같은지 대조한다.

    파일이 없으면 None(= 상수를 그대로 믿는다). 기록된 값은 그래프를 만든
    머신의 절대경로라 저장소 루트와 직접 이어붙일 수 없으므로, 끝쪽 경로
    세그먼트가 SCAN_ROOT와 일치하는지만 본다 — 클론 위치나 머신이 달라져도
    루트 자체가 같으면 통과하고, `wiki/`나 저장소 루트처럼 다른 루트에서
    다시 만든 경우만 걸린다.
    """
    path = _root_path(repo_root)
    if not path.exists():
        return None
    try:
        recorded = path.read_text(encoding="utf-8").strip()
    except OSError:
        return None
    if not recorded:
        return None
    parts = [nfc(p) for p in
             pathlib.PurePosixPath(recorded.replace("\\", "/")).parts
             if p not in ("/", "")]
    expected = [nfc(p) for p in SCAN_ROOT.split("/")]
    if parts[-len(expected):] == expected:
        return None
    return (f"스캔 루트 불일치 — wiki/graphify-out/.graphify_root는 '{recorded}'를 "
            f"기록했는데 graph_signals는 '{SCAN_ROOT}'를 가정한다. 그래프가 "
            f"다른 루트에서 만들어졌다면 source_file 형태가 달라 판정이 전부 "
            f"빗나간다. 판정 생략")


def graph_age(repo_root: pathlib.Path, pages) -> str | None:
    """그래프가 stale인 이유를 반환한다. 최신이면 None.

    판정 기준은 `wiki/graphify-out/manifest.json`이 기록한 파일별 내용 해시와
    실제 `wiki/pages/` 파일의 해시 비교다(스펙 §5.3). mtime은 쓰지 않는다 — git
    체크아웃이 파일 시각을 기록 순서대로 새로 찍기 때문에, 산출물이 먼저
    쓰이는 새 클론에서는 페이지가 항상 그래프보다 "새로워" 영구 stale이 된다.
    해시는 체크아웃 시각에 영향받지 않고, mtime이 잡을 수 없는 삭제·추가까지
    같은 비교 하나로 잡는다.

    stale로 보는 경우: 매니페스트 없음 / 기록과 내용이 다른 페이지 / 매니페스트에
    없는 새 페이지 / 매니페스트에는 있는데 사라진 페이지.
    """
    graph_path = _graph_path(repo_root)
    if not graph_path.exists():
        return "wiki/graphify-out/graph.json 없음"

    manifest_path = _manifest_path(repo_root)
    if not manifest_path.exists():
        return ("wiki/graphify-out/manifest.json 없음 — 그래프를 만든 기록이 없어 "
                "무엇이 바뀌었는지 대조할 수 없다. graphify 추출을 1회 돌린다")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError, UnicodeDecodeError) as e:
        return f"manifest.json 파싱 실패 — 판정 생략: {e}"
    if not isinstance(manifest, dict):
        return "manifest.json 형태가 예상과 다름(최상위가 매핑이 아니다) — 판정 생략"

    scan_dir = repo_root / SCAN_ROOT
    # 매니페스트 키(스캔 루트 상대)와 실제 파일을 NFC 키로 맞춰 대조한다.
    # 파일명이 한글이라 디스크가 NFD로 돌려주는 환경이 있어 양쪽 다 정규화한다.
    disk: dict[str, pathlib.Path] = {}
    if scan_dir.is_dir():
        for p in scan_dir.rglob("*"):
            if p.is_file():
                disk[nfc(p.relative_to(scan_dir).as_posix())] = p

    changed: list[str] = []
    missing: list[str] = []
    for key, entry in manifest.items():
        k = nfc(key)
        p = disk.get(k)
        if p is None:
            missing.append(f"{SCAN_ROOT}/{k}")
            continue
        recorded_hash = _entry_hash(entry)
        try:
            actual_hash = _md5(p)
        except OSError:
            missing.append(f"{SCAN_ROOT}/{k}")
            continue
        if recorded_hash != actual_hash:
            changed.append(f"{SCAN_ROOT}/{k}")

    known = {nfc(k) for k in manifest}
    added: list[str] = []
    for pg in pages:
        if pg.path.parts[:2] != ("wiki", "pages"):
            continue
        key = nfc(pathlib.PurePosixPath(*pg.path.parts[2:]).as_posix())
        if key not in known:
            added.append(nfc(pg.rel))

    parts = []
    for label, items in (("내용이 매니페스트 기록과 다름", changed),
                         ("매니페스트에 없는 새 페이지", added),
                         ("매니페스트에 있는데 사라진 페이지", missing)):
        if items:
            parts.append(f"{label} {len(items)}건: {_sample(items)}")
    if not parts:
        return None
    return "manifest.json과 어긋남 — " + "; ".join(parts)


def _sample(items: list[str], n: int = 5) -> str:
    items = sorted(items)
    more = "" if len(items) <= n else f" 외 {len(items) - n}건"
    return ", ".join(items[:n]) + more


def rollup(graph: dict) -> tuple[dict[str, int], dict[str, set[str]], dict[str, int]]:
    """개념 그래프를 페이지 단위로 롤업한다(스펙 §5.5).

    - 페이지 P의 개념 집합 = source_file이 P인 노드
    - deg(P) = P의 개념에서 나가는 엣지 중 상대 개념의 source_file이 P가
      아닌 것의 수 (페이지 내부 엣지는 세지 않는다)
    - 페이지 간 인접 = 개념 엣지가 하나라도 있으면 두 페이지가 인접

    반환하는 키는 graphify가 기록한 그대로(스캔 루트 기준 상대경로)다.
    저장소 상대경로 변환과 "그 페이지가 실제로 로드됐는가" 확인은 여기서
    하지 않는다 — 이 함수는 graph.json만 보고 Page 목록을 모르므로, 그
    존재 확인은 호출자(check_graph_signals)의 몫이다.

    엣지 컨테이너 키는 `links`가 정본이다(graphify 산출물 실측). 혹시 다른
    이름(`edges`)으로 온 경우를 방어적으로 받아준다.
    """
    nodes = graph.get("nodes", [])
    links = graph.get("links", graph.get("edges", []))

    node_page: dict[str, str] = {}
    page_communities: dict[str, Counter] = defaultdict(Counter)
    for n in nodes:
        nid = n.get("id")
        src = n.get("source_file")
        if not nid or not src:
            continue
        src = nfc(src)
        node_page[nid] = src
        page_communities[src][n.get("community")] += 1

    deg: dict[str, int] = {src: 0 for src in page_communities}
    adj: dict[str, set[str]] = {src: set() for src in page_communities}

    for e in links:
        sp = node_page.get(e.get("source"))
        tp = node_page.get(e.get("target"))
        if sp is None or tp is None or sp == tp:
            continue
        deg[sp] += 1
        deg[tp] += 1
        adj[sp].add(tp)
        adj[tp].add(sp)

    comm = {src: counts.most_common(1)[0][0]
            for src, counts in page_communities.items()}
    return deg, adj, comm


def _resolve_pages(comm: dict[str, int], pages
                    ) -> tuple[dict[str, str], list[str]]:
    """롤업 키(스캔 루트 상대)를 저장소 상대경로로 바꾸고 로드된 페이지와 대조한다.

    반환: (원본 키 → 저장소 상대경로, 대응 페이지를 못 찾은 저장소 상대경로 목록)
    """
    page_index = {nfc(pg.rel) for pg in pages}
    resolved: dict[str, str] = {}
    unresolved: list[str] = []
    for src in comm:
        rel = to_repo_rel(src)
        if rel in page_index:
            resolved[src] = rel
        else:
            unresolved.append(rel)
    return resolved, unresolved


def check_graph_signals(repo_root: pathlib.Path, pages) -> list[Finding]:
    try:
        graph = load_graph(repo_root)
    except (json.JSONDecodeError, OSError, UnicodeDecodeError) as e:
        return [Finding(
            "warning", "그래프stale", "wiki/graphify-out/graph.json",
            f"graph.json 파싱 실패 — 판정 생략: {e}")]
    if graph is None:
        return []

    reason = check_scan_root(repo_root)
    if reason:
        return [Finding("warning", "그래프stale", "wiki/graphify-out/.graphify_root", reason)]

    reason = graph_age(repo_root, pages)
    if reason:
        return [Finding("warning", "그래프stale", "wiki/graphify-out/graph.json", reason)]

    deg, adj, comm = rollup(graph)

    resolved, unresolved = _resolve_pages(comm, pages)
    if unresolved:
        unresolved = sorted(unresolved)
        sample = ", ".join(unresolved[:5])
        more = "" if len(unresolved) <= 5 else f" 외 {len(unresolved) - 5}건"
        return [Finding(
            "warning", "그래프stale", "wiki/graphify-out/graph.json",
            f"그래프가 가리키는 페이지 {len(unresolved)}건이 로드된 페이지와 "
            f"대응하지 않음: {sample}{more} — 그래프가 오래됐거나 스캔 루트 "
            f"정규화가 어긋났을 수 있다. 판정 생략")]

    deg_r = {resolved[src]: n for src, n in deg.items()}
    adj_r = {resolved[src]: {resolved[t] for t in neighbors}
             for src, neighbors in adj.items()}
    comm_r = {resolved[src]: c for src, c in comm.items()}

    out: list[Finding] = []
    out.extend(_check_isolated(deg_r))
    out.extend(_check_sparse_communities(comm_r, adj_r))
    out.extend(_check_bridges(adj_r, comm_r))
    return out


def _check_isolated(deg_r: dict[str, int]) -> list[Finding]:
    out = []
    for rel in sorted(deg_r):
        d = deg_r[rel]
        if d <= 1:
            out.append(Finding(
                "warning", "그래프고립", rel,
                f"개념 그래프 차수 {d} — 다른 페이지와 개념적으로 거의/전혀 "
                f"연결되지 않음(check_orphans의 링크 기반 '고아'와는 별개 신호: "
                f"이쪽은 개념 그래프 차수를 보는 경고, '고아'는 인바운드 "
                f"[[위키링크]] 0건을 보는 위반이다)"))
    return out


def _check_sparse_communities(comm_r: dict[str, int],
                               adj_r: dict[str, set[str]]) -> list[Finding]:
    communities: dict[int, set[str]] = defaultdict(set)
    for rel, c in comm_r.items():
        communities[c].add(rel)

    out = []
    for c in sorted(communities, key=lambda c: sorted(communities[c])[0]):
        members = communities[c]
        k = len(members)
        if k < SPARSE_MIN_PAGES:
            continue
        possible = k * (k - 1) / 2
        actual = sum(len(adj_r.get(m, set()) & members) for m in members) / 2
        density = actual / possible if possible else 0.0
        if density < SPARSE_DENSITY_THRESHOLD:
            member_list = sorted(members)
            out.append(Finding(
                "warning", "희소커뮤니티", member_list[0],
                f"커뮤니티 {c} 내부 페이지 간 밀도 {density:.2f} "
                f"(페이지 {k}개, 실제/가능 엣지 {actual:g}/{possible:g}) — "
                f"관련 페이지: {', '.join(member_list)}"))
    return out


def _check_bridges(adj_r: dict[str, set[str]],
                    comm_r: dict[str, int]) -> list[Finding]:
    out = []
    for rel in sorted(adj_r):
        # community가 없는 노드는 rollup이 None으로 담는다(graphify가 커뮤니티
        # 탐지를 돌리지 않았거나 그 노드만 배정에서 빠진 경우). 정렬 키가
        # int와 None으로 섞이면 TypeError로 lint 전체가 트레이스백과 함께
        # 죽는다 — 경고 하나 때문에 게이트를 내리지 않도록 미배정은 세지 않고
        # 건너뛴다. 판정 자체도 "서로 다른 커뮤니티 3개 이상"이므로 소속을
        # 모르는 이웃은 애초에 근거가 되지 못한다.
        neighbor_communities = {comm_r[n] for n in adj_r[rel]
                                if n in comm_r and comm_r[n] is not None}
        if len(neighbor_communities) >= BRIDGE_MIN_COMMUNITIES:
            out.append(Finding(
                "warning", "브리지", rel,
                f"{len(neighbor_communities)}개 서로 다른 커뮤니티에 걸쳐 연결됨 "
                f"({sorted(neighbor_communities)}) — 여러 지식 영역을 잇는 "
                f"페이지 후보"))
    return out
