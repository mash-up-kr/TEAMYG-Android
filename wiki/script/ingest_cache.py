#!/usr/bin/env python3
"""ingest 증분 게이트 — raw/ 원본의 SHA256을 기록해 재처리를 막는다.

사용:
  python3 wiki/script/ingest_cache.py --list                 처리 대상 나열
  python3 wiki/script/ingest_cache.py --record <저장소상대경로>  한 건 기록
  python3 wiki/script/ingest_cache.py --prune                실재하지 않는 항목 제거
종료 코드: 0 정상 / 1 대상 파일 없음 / 2 저장소 루트 아님

매니페스트는 **소스 하나의 ingest가 성공한 직후** 갱신한다. 배치 시작 시 일괄 기록하면
중간에 실패한 소스가 영구히 건너뛰어진다(스펙 8).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import sys

MANIFEST_REL = "wiki/raw/.manifest.json"


def sha256_of(path: pathlib.Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for block in iter(lambda: fh.read(65536), b""):
            h.update(block)
    return h.hexdigest()


def load_manifest(repo_root: pathlib.Path) -> dict[str, str]:
    p = repo_root / MANIFEST_REL
    if not p.exists():
        return {}
    return json.loads(p.read_text(encoding="utf-8"))


def save_manifest(repo_root: pathlib.Path, manifest: dict[str, str]) -> None:
    p = repo_root / MANIFEST_REL
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(dict(sorted(manifest.items())),
                            ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def raw_markdown(repo_root: pathlib.Path) -> list[pathlib.Path]:
    raw = repo_root / "wiki" / "raw"
    return sorted(raw.rglob("*.md")) if raw.is_dir() else []


def pending(repo_root: pathlib.Path, manifest: dict[str, str] | None = None) -> list[str]:
    manifest = load_manifest(repo_root) if manifest is None else manifest
    out = []
    for p in raw_markdown(repo_root):
        rel = p.relative_to(repo_root).as_posix()
        if manifest.get(rel) != sha256_of(p):
            out.append(rel)
    return out


def record(repo_root: pathlib.Path, rel: str) -> None:
    p = repo_root / rel
    if not p.is_file():
        raise FileNotFoundError(rel)
    manifest = load_manifest(repo_root)
    manifest[rel] = sha256_of(p)
    save_manifest(repo_root, manifest)


def prune(repo_root: pathlib.Path) -> list[str]:
    manifest = load_manifest(repo_root)
    actual = {p.relative_to(repo_root).as_posix() for p in raw_markdown(repo_root)}
    removed = sorted(set(manifest) - actual)
    if removed:
        for k in removed:
            del manifest[k]
        save_manifest(repo_root, manifest)
    return removed


def repo_root_ok(repo_root: pathlib.Path) -> bool:
    """lint.py·route.py와 달리 wiki/conventions.md까지는 요구하지 않는다.

    저 둘은 검사 대상이 0건이 되는 것을 막아야 하므로 계약 파일을 함께 요구한다.
    이 스크립트는 검사하지 않고 wiki/raw/의 해시만 기록하며, 매니페스트는 위키
    트리가 채워지기 *전에* 만들어질 수 있어야 한다.
    """
    return (repo_root / "wiki").is_dir()


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(add_help=True)
    g = ap.add_mutually_exclusive_group(required=True)
    g.add_argument("--list", action="store_true")
    g.add_argument("--record", metavar="REL")
    g.add_argument("--prune", action="store_true")
    args = ap.parse_args(argv)

    repo_root = pathlib.Path.cwd()
    if not repo_root_ok(repo_root):
        print(f"저장소 루트에서 실행한다. 현재 위치({repo_root})에 wiki/ 가 없다.",
              file=sys.stderr)
        return 2

    if args.list:
        todo = pending(repo_root)
        print(f"처리 대상 {len(todo)}건")
        for rel in todo:
            print(f"- {rel}")
        return 0
    if args.record:
        try:
            record(repo_root, args.record)
        except FileNotFoundError:
            print(f"{args.record} 가 없다.", file=sys.stderr)
            return 1
        print(f"기록: {args.record}")
        return 0
    removed = prune(repo_root)
    print(f"제거 {len(removed)}건" + ("".join(f"\n- {r}" for r in removed) if removed else ""))
    return 0


if __name__ == "__main__":
    sys.exit(main())
